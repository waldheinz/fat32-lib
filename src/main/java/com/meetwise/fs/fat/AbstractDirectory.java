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

    private final Vector<FatBasicDirEntry> entries;
    private final Map<FatDirEntry, FatFile> files;
    private final Fat fat;
    private boolean dirty;
    private final int clusterSize;
    private final boolean readOnly;
    private final boolean isRoot;
    
    protected AbstractDirectory(Fat fat, int entryCount, boolean readOnly, boolean isRoot) throws IOException {
        final Fat16BootSector bs = (Fat16BootSector) fat.getBootSector();
        
        this.entries = new Vector<FatBasicDirEntry>(entryCount);
        this.entries.setSize(entryCount);
        this.files = new HashMap<FatDirEntry, FatFile>();
        this.fat = fat;
        this.clusterSize = bs.getBytesPerCluster();
        this.readOnly = readOnly;
        this.isRoot = isRoot;
        
        read();
    }
    
    protected abstract void read(ByteBuffer data) throws IOException;
    
    protected abstract void write(ByteBuffer data) throws IOException;

    protected abstract long getStorageCluster();

    protected abstract boolean canChangeSize(int entryCount);

    public final void setEntry(int idx, FatBasicDirEntry entry) {
        this.entries.set(idx, entry);
    }

    public final FatBasicDirEntry getEntry(int idx) {
        return this.entries.get(idx);
    }

    public final int getCapacity() {
        return this.entries.capacity();
    }

    public final int getEntryCount() {
        return this.entries.size();
    }

    public final int getClusterSize() {
        return clusterSize;
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

    public final boolean isRoot() {
        return this.isRoot;
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
                new FatDirEntry(this, new ShortName(nameExt));
        
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
                
        return addFatDirectory(name, getStorageCluster());
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
        final ShortName toFind = new ShortName(nameExt);

        for (int i = 0; i < entries.size(); i++) {
            final FatBasicDirEntry entry = entries.get(i);

            if (entry != null && entry instanceof FatDirEntry) {
                final FatDirEntry fde = (FatDirEntry) entry;
                
                if (fde.getShortName().equals(toFind)) return fde;
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
     * Sets the first two entries '.' and '..' in the directory
     * 
     * @param myCluster 
     * @param parentCluster
     */
    protected void initialize(long myCluster, long parentCluster) {
        FatDirEntry e = new FatDirEntry(this, new ShortName(".", ""));
        entries.set(0, e);
        e.setFlags(FatConstants.F_DIRECTORY);
        e.setStartCluster((int) myCluster);
        e = new FatDirEntry(this, new ShortName("..", ""));
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

        write(data);

        resetDirty();
    }
    
    private void read() throws IOException {
        final ByteBuffer data = ByteBuffer.allocate(
                entries.size() * FatBasicDirEntry.SIZE);
                
        read(data);

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
