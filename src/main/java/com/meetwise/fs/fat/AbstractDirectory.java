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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.NoSuchElementException;

import java.util.Vector;
import com.meetwise.fs.FSDirectory;
import com.meetwise.fs.FSDirectoryEntry;
import com.meetwise.fs.FileSystemException;
import com.meetwise.fs.ReadOnlyFileSystemException;

/**
 * @author Ewout Prangsma &lt; epr at jnode.org&gt;
 */
abstract class AbstractDirectory
        extends FatObject
        implements FSDirectory, Iterable<FSDirectoryEntry> {

    protected Vector<FatBasicDirEntry> entries =
            new Vector<FatBasicDirEntry>();
    
    private boolean dirty;
    protected FatFile file;

    // for root
    protected AbstractDirectory(FatFileSystem fs, int nrEntries) {
        super(fs);
        
        entries.setSize(nrEntries);
        dirty = false;
    }
    
    protected AbstractDirectory(FatFileSystem fs, int nrEntries, FatFile file) {
        this(fs, nrEntries);
        this.file = file;
    }
    
    protected AbstractDirectory(FatFileSystem fs, FatFile myFile) {
        this(fs, (int) myFile.getLength() / 32, myFile);
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
    
    /**
     * Add a directory entry.
     * 
     * @param nameExt
     * @return
     * @throws FileSystemException 
     */
    protected synchronized FatDirEntry addFatFile(String nameExt)
            throws FileSystemException {
        
        if (getFileSystem().isReadOnly()) {
            throw new ReadOnlyFileSystemException(this.getFileSystem(),
                    "addFile in readonly filesystem"); //NOI18N
        }

        if (getFatEntry(nameExt) != null) {
            throw new FileSystemException(this.getFileSystem(),
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
        
        throw new FileSystemException(this.getFileSystem(),
                "directory is full"); //NOI18N
    }

    /**
     * Add a new file with a given name to this directory.
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
    protected synchronized FatDirEntry addFatDirectory(
            String nameExt, long parentCluster) throws IOException {
        
        final FatDirEntry entry = addFatFile(nameExt);
        final int clusterSize = getFileSystem().getClusterSize();
        entry.setFlags(FatConstants.F_DIRECTORY);
        final FatFile f = entry.getFatFile();
        f.setLength(clusterSize);

        //TODO optimize it also to use ByteBuffer at lower level        
        //final byte[] buf = new byte[clusterSize];
        final ByteBuffer buf = ByteBuffer.allocate(clusterSize);

        // Clean the contents of this cluster to avoid reading strange data
        // in the directory.
        //file.write(0, buf, 0, buf.length);
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
        if (getFileSystem().isReadOnly()) throw new
                ReadOnlyFileSystemException(this.getFileSystem(),
                "readonly filesystem"); //NOI18N

        final long parentCluster;

        if (file == null) {
            parentCluster = 0;
        } else {
            parentCluster = file.getStartCluster();
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
     * Remove a file or directory with a given name
     * 
     * @param nameExt
     */
    @Override
    public synchronized void remove(String nameExt) throws IOException {
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

    /**
     * Print the contents of this directory to the given writer. Used for
     * debugging purposes.
     * 
     * @param out
     */
    public void printTo(PrintWriter out) {
        int freeCount = 0;

        for (int i = 0; i < entries.size(); i++) {
            FatBasicDirEntry entry = entries.get(i);
            
            if (entry != null) {
                out.println("0x" + Integer.toHexString(i) +
                        " " + entries.get(i));
            } else {
                freeCount++;
            }
        }
        
        out.println("Unused entries " + freeCount);
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
    protected final void resetDirty() {
        this.dirty = false;
    }

    /**
     * Can this directory change size of <code>newSize</code> directory
     * entries?
     * 
     * @param newSize
     * @return boolean
     */
    protected abstract boolean canChangeSize(int newSize);

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
     * Flush the contents of this directory to the persistent storage
     */
    @Override
    public abstract void flush() throws IOException;
    
    /**
     * Read the contents of this directory from the given byte array
     * 
     * @param src
     */
    protected synchronized void read(byte[] src) {
        
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

    /**
     * Write the contents of this directory to the given device at the given
     * offset.
     * 
     * @param dest
     */
    protected synchronized void write(byte[] dest) {
        byte[] empty = new byte[32];
        
        for (int i = 0; i < entries.size(); i++) {
            final FatBasicDirEntry entry = entries.get(i);
            
            if (entry != null) {
                entry.write(dest, i * 32);
            } else {
                System.arraycopy(empty, 0, dest, i * 32, 32);
            }
        }
    }

}
