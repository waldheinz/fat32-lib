/*
 * $Id: AbstractDirectory.java 4975 2009-02-02 08:30:52Z lsantha $
 *
 * Copyright (C) 2003-2009 JNode.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
 
package com.meetwise.fs.fat;

import com.meetwise.fs.BlockDevice;
import com.meetwise.fs.FSDirectory;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

import java.util.Vector;
import com.meetwise.fs.FSDirectoryEntry;
import com.meetwise.fs.FileSystemException;
import com.meetwise.fs.ReadOnlyFileSystemException;
import java.util.HashMap;
import java.util.Map;

/**
 * 
 *
 * @author Ewout Prangsma &lt;epr at jnode.org&gt;
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
abstract class AbstractDirectory
        implements Iterable<FSDirectoryEntry> , FSDirectory {

    protected final Vector<FatBasicDirEntry> entries;
    private final ClusterChain chain;
    private final BlockDevice device;
    private final long deviceOffset;
    private final Map<FatDirEntry, FatFile> files;
    private final Fat fat;
    private boolean dirty;
    private final int clusterSize;
    private final boolean readOnly;

    protected AbstractDirectory(Fat fat, BlockDevice device,
            long offset, int nrEntries, int clusterSize, boolean readOnly) throws IOException {
        
        this.entries = new Vector<FatBasicDirEntry>(nrEntries);
        this.files = new HashMap<FatDirEntry, FatFile>();
        this.entries.setSize(nrEntries);
        this.chain = null;
        this.fat = fat;
        this.device = device;
        this.deviceOffset = offset;
        this.clusterSize = clusterSize;
        this.readOnly = readOnly;
        
        read();
    }
    
    protected AbstractDirectory(ClusterChain chain)
            throws IOException {
        
        final long size = chain.getLengthOnDisk() / FatBasicDirEntry.SIZE;
        if (size > Integer.MAX_VALUE)
            throw new IOException("too many directory entries");

        this.entries = new Vector<FatBasicDirEntry>((int) size);
        this.files = new HashMap<FatDirEntry, FatFile>();
        this.entries.setSize((int) size);
        this.chain = chain;
        this.device = null;
        this.deviceOffset = -1;
        this.clusterSize = chain.getClusterSize();
        this.readOnly = chain.isReadOnly();
        this.fat = chain.getFat();
        
        read();
    }

    public final int getClusterSize() {
        return clusterSize;
    }
    
    /**
     * Returns the first cluster of this directory, if it is stored in a
     * cluster chain. This is true for all directories excpet the root
     * directories of FAT12/16 partitions, for which 0 is returned.
     *
     * @return the first cluster of the chain where the contents of this
     *      directory are stored, or 0 for FAT12/16 root directories
     */
    protected final long getStorageCluster() {
        if (this.chain != null) {
            assert (this.chain.getStartCluster() >= Fat.FIRST_CLUSTER);
            
            return this.chain.getStartCluster();
        } else {
            return 0;
        }
    }
    
    /**
     * Gets an iterator to iterate over all entries. The iterated objects are
     * all instance DirEntry.
     * 
     * @return Iterator
     */
    @Override
    public Iterator<FSDirectoryEntry> iterator() {
        return new DirIterator();
    }

    public boolean isReadOnly() {
        return readOnly;
    }
    
    /**
     * Add a directory entry.
     * 
     * @param nameExt
     * @return
     * @throws FileSystemException 
     */
    protected FatDirEntry addFatFile(String nameExt)
            throws FileSystemException {
        
        if (isReadOnly()) {
            throw new ReadOnlyFileSystemException(null);
        }

        if (getFatEntry(nameExt) != null) {
            throw new FileSystemException(null,
                    "file already exists " + nameExt); //NOI18N
        }
        
        final FatDirEntry newEntry =
                new FatDirEntry(this, splitName(nameExt), splitExt(nameExt));
        
        for (int i = 0; i < entries.size(); i++) {
            FatBasicDirEntry e = entries.get(i);
            if (e == null) {
                entries.set(i, newEntry);
                setDirty();
                return newEntry;
            }
        }

        final int newSize = entries.size() + 512 / 32;
        if (canChangeSize(newSize)) {
            entries.setSize(newSize);
            setDirty();
            return newEntry;
        }
        
        throw new FileSystemException(null,
                "directory is full"); //NOI18N
    }

    /**
     * Add a new chain with a given name to this directory.
     * 
     * @param name
     * @return 
     * @throws IOException
     */
    @Override
    public FSDirectoryEntry addFile(String name) throws IOException {
        return addFatFile(name);
    }

    /**
     * Add a directory entry of the type directory.
     * 
     * @param nameExt
     * @param parentCluster
     * @return 
     * @throws IOException
     */
    private FatDirEntry addFatDirectory(
            String nameExt, long parentCluster) throws IOException {
        
        final FatDirEntry entry = addFatFile(nameExt);
        entry.setFlags(FatConstants.F_DIRECTORY);
        final FatFile f = entry.getFatFile();
        f.setLength(clusterSize);
        
        final ByteBuffer buf = ByteBuffer.allocate(clusterSize);

        // Clean the contents of this cluster to avoid reading strange data
        // in the directory.
        //chain.write(0, buf, 0, buf.length);
        f.write(0, buf);

        f.getDirectory().initialize(f.getStartCluster(), parentCluster);
        flush();
        return entry;
    }

    /**
     * Add a new (sub-)directory with a given name to this directory.
     * 
     * @param name
     * @return 
     * @throws IOException
     */
    @Override
    public FSDirectoryEntry addDirectory(String name) throws IOException {
        if (isReadOnly()) throw new
                ReadOnlyFileSystemException(null);

        final long parentCluster;

        if (chain == null) {
            parentCluster = 0;
        } else {
            parentCluster = chain.getStartCluster();
        }
        
        return addFatDirectory(name, parentCluster);
    }

    /**
     * Gets the number of directory entries in this directory
     * 
     * @return int
     */
    public int getSize() {
        return entries.size();
    }

    /**
     * Search for an entry with a given name.ext
     * 
     * @param nameExt
     * @return FatDirEntry null == not found
     */
    protected FatDirEntry getFatEntry(String nameExt) {

        final String name = splitName(nameExt);
        final String ext = splitExt(nameExt);

        for (int i = 0; i < entries.size(); i++) {
            final FatBasicDirEntry entry = entries.get(i);

            if (entry != null && entry instanceof FatDirEntry) {
                FatDirEntry fde = (FatDirEntry) entry;

                if (name.equalsIgnoreCase(fde.getNameOnly()) && 
                        ext.equalsIgnoreCase(fde.getExt())) {
                    
                    return fde;
                }
            }
        }
        
        return null;
    }

    /**
     * Gets the entry with the given name.
     * 
     * @param name
     * @return 
     * @throws IOException
     */
    @Override
    public FSDirectoryEntry getEntry(String name) throws IOException {
        final FatDirEntry entry = getFatEntry(name);
        if (entry == null) {
            throw new FileNotFoundException(name);
        } else {
            return entry;
        }
    }

    /**
     * Remove a chain or directory with a given name
     * 
     * @param nameExt
     */
    @Override
    public void remove(String nameExt) throws IOException {
        final FatDirEntry entry = getFatEntry(nameExt);

        if (entry == null) throw new FileNotFoundException(nameExt);
        
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i) == entry) {
                entries.set(i, null);
                setDirty();
                flush();
                return;
            }
        }
    }
    
    private class DirIterator implements Iterator<FSDirectoryEntry> {

        private int offset = 0;

        /**
         * @see java.util.Iterator#hasNext()
         */
        @Override
        public boolean hasNext() {
            
            while (offset < entries.size()) {
                FatBasicDirEntry e = entries.get(offset);
                if ((e != null) && e instanceof FatDirEntry && 
                        !((FatDirEntry) e).isDeleted() &&
                        !((FatDirEntry) e).getName().equals(".") &&
                        !((FatDirEntry) e).getName().equals("..")) {

                    return true;
                } else {
                    offset++;
                }
            }
            
            return false;
        }

        /**
         * @see java.util.Iterator#next()
         */
        @Override
        public FSDirectoryEntry next() {
            
            while (offset < entries.size()) {
                FatBasicDirEntry e = entries.get(offset);
                if ((e != null) && (e instanceof FatDirEntry) &&
                        !((FatDirEntry) e).isDeleted() &&
                        !((FatDirEntry) e).getName().equals(".") &&
                        !((FatDirEntry) e).getName().equals("..")) {

                    offset++;
                    
                    return (FSDirectoryEntry) e;
                } else {
                    offset++;
                }
            }
            
            throw new NoSuchElementException();
        }

        /**
         * @see java.util.Iterator#remove()
         */
        @Override
        public void remove() {
            throw new UnsupportedOperationException();
        }
    }

    /**
     * Returns the dirty.
     * 
     * @return boolean
     */
    public boolean isDirty() {
        if (dirty)  return true;
        
        for (int i = 0; i < entries.size(); i++) {
            FatBasicDirEntry entry = entries.get(i);
            
            if ((entry != null) && (entry instanceof FatDirEntry)) {
                if (((FatDirEntry) entry).isDirty()) {
                    return true;
                }
            }
        }
        
        return false;
    }

    /**
     * Mark this directory as dirty.
     */
    protected final void setDirty() {
        this.dirty = true;
    }

    /**
     * Mark this directory as not dirty.
     */
    private final void resetDirty() {
        this.dirty = false;
    }

    /**
     * Can this directory change size of <code>newSize</code> directory
     * entries?
     * 
     * @param newSize
     * @return boolean
     */
    protected final boolean canChangeSize(int newSize) {
        return (chain != null);
    }

    protected String splitName(String nameExt) {
        int i = nameExt.indexOf('.');
        if (i < 0) {
            return nameExt;
        } else {
            return nameExt.substring(0, i);
        }
    }

    protected String splitExt(String nameExt) {
        int i = nameExt.indexOf('.');
        if (i < 0) {
            return "";
        } else {
            return nameExt.substring(i + 1);
        }
    }

    /**
     * Sets the first two entries '.' and '..' in the directory
     * 
     * @param myCluster 
     * @param parentCluster
     */
    protected void initialize(long myCluster, long parentCluster) {
        FatDirEntry e = new FatDirEntry(this, ".", "");
        entries.set(0, e);
        e.setFlags(FatConstants.F_DIRECTORY);
        e.setStartCluster((int) myCluster);
        e = new FatDirEntry(this, "..", "");
        entries.set(1, e);
        e.setFlags(FatConstants.F_DIRECTORY);
        e.setStartCluster((int) parentCluster);
    }

    
    /**
     * Gets the file for the given entry.
     * 
     * @param entry
     * @return 
     */
    FatFile getFile(FatDirEntry entry) {
        FatFile file = files.get(entry);
        
        if (file == null) {
            file = new FatFile(fat, entry, entry.getStartCluster(),
                    entry.getLength(), entry.isDirectory(),
                    isReadOnly());
            files.put(entry, file);
        }
        
        return file;
    }

    /**
     * Flush the contents of this directory to the persistent storage
     */
    @Override
    public void flush() throws IOException {
        final ByteBuffer data = ByteBuffer.allocate(
                entries.size() * FatBasicDirEntry.SIZE);

        final byte[] empty = new byte[32];


        for (FatFile f : files.values()) {
            f.flush();
        }

        for (int i = 0; i < entries.size(); i++) {
            final FatBasicDirEntry entry = entries.get(i);

            if (entry != null) {
                entry.write(data.array(), i * 32);
            } else {
                System.arraycopy(empty, 0, data.array(), i * 32, 32);
            }
        }
        
        if (chain != null) {
            final long trueSize = chain.setSize(data.capacity());
            chain.writeData(0, data);

            if (trueSize > data.capacity()) {
                final int rest = (int) (trueSize - data.capacity());
                final ByteBuffer fill = ByteBuffer.allocate(rest);
                chain.writeData(data.capacity(), fill);
            }
        } else {
            device.write(deviceOffset, data);
        }

        resetDirty();
    }
    
    private void read() throws IOException {
        final ByteBuffer data = ByteBuffer.allocate(
                entries.size() * FatBasicDirEntry.SIZE);

        if (this.chain != null) {
            chain.readData(0, data);
        } else {
            device.read(deviceOffset, data);
        }
        
        final byte[] src = data.array();

        for (int i = 0; i < entries.size(); i++) {
            int index = i * 32;
            if (src[index] == 0) {
                entries.set(i, null);
            } else {
                FatBasicDirEntry entry = FatDirEntry.create(this, src, index);
                entries.set(i, entry);
            }
        }
    }
}
