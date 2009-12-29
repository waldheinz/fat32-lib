/*
 * $Id: FatLfnDirectory.java 4975 2009-02-02 08:30:52Z lsantha $
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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import com.meetwise.fs.FSDirectoryEntry;
import com.meetwise.fs.FileSystemException;
import com.meetwise.fs.ReadOnlyFileSystemException;

/**
 * @author gbin
 */
class FatLfnDirectory extends FatDirectory {

    private final HashMap<String, LfnEntry> shortNameIndex =
            new HashMap<String, LfnEntry>();
    
    private final HashMap<String, LfnEntry> longFileNameIndex =
            new HashMap<String, LfnEntry>();

    private final ShortNameGenerator sng;

    /**
     * @param fs
     * @param file
     * @throws FileSystemException
     */
    public FatLfnDirectory(FatFileSystem fs, FatFile file) throws FileSystemException {
        super(fs, file);
        
        this.sng = new ShortNameGenerator(shortNameIndex.keySet());
        read();
    }

    /**
     * Constructor for the root directory.
     *
     * @param fs
     * @param nrEntries
     */
    public FatLfnDirectory(FatFileSystem fs, int nrEntries) {
        super(fs, nrEntries);
        
        this.sng = new ShortNameGenerator(shortNameIndex.keySet());
    }
    
    ShortNameGenerator getShortNameGenerator() {
        return sng;
    }

    @Override
    public LfnEntry addFile(String name) throws FileSystemException {
        if (getFileSystem().isReadOnly()) {
            throw new ReadOnlyFileSystemException(this.getFileSystem(),
                    "addFile in readonly filesystem");
        }

        name = name.trim();
        String shortName = sng.generateShortName(name);
        FatDirEntry realEntry = new FatDirEntry(this, splitName(shortName), splitExt(shortName));
        LfnEntry entry = new LfnEntry(this, realEntry, name);
        shortNameIndex.put(shortName, entry);
        longFileNameIndex.put(name, entry);
        setDirty();
        flush();
        return entry;
    }

    @Override
    public FSDirectoryEntry addDirectory(String name) throws IOException {
        if (getFileSystem().isReadOnly()) {
            throw new ReadOnlyFileSystemException(this.getFileSystem(),
                    "addDirectory in readonly filesystem"); //NOI18N
        }

        name = name.trim();
        String shortName = sng.generateShortName(name);
        FatDirEntry realEntry = new FatDirEntry(this, splitName(shortName), splitExt(shortName));

        final long parentCluster;
        if (file == null) {
            parentCluster = 0;
        } else {
            parentCluster = file.getStartCluster();
        }

        final int clusterSize = getFileSystem().getClusterSize();
        realEntry.setFlags(FatConstants.F_DIRECTORY);
        final FatFile f = realEntry.getFatFile();
        f.setLength(clusterSize);

        // TODO optimize it also to use ByteBuffer at lower level
        // final byte[] buf = new byte[clusterSize];
        final ByteBuffer buf = ByteBuffer.allocate(clusterSize);

        // Clean the contents of this cluster to avoid reading strange data
        // in the directory.
        // file.write(0, buf, 0, buf.length);
        f.write(0, buf);

        f.getDirectory().initialize(f.getStartCluster(), parentCluster);

        LfnEntry entry = new LfnEntry(this, realEntry, name);
        shortNameIndex.put(shortName, entry);
        longFileNameIndex.put(name, entry);
        setDirty();
        flush();
        return entry;
    }

    @Override
    public FSDirectoryEntry getEntry(String name) {
        name = name.trim();
        FSDirectoryEntry entry;
        
        // try first as a long file name
        entry = longFileNameIndex.get(name);
        if (entry == null)
            return shortNameIndex.get(name.toUpperCase());
        else
            return entry;

    }

    @Override
    protected void read(byte[] src) {
        super.read(src);
        readLFN();
    }

    private void readLFN() {
//        System.out.println("Read LFN");
        int i = 0;
        int size = entries.size();

        while (i < size) {
            // jump over empty entries
            while (i < size && entries.get(i) == null) {
                i++;
            }

            if (i >= size) {
                break;
            }

            int offset = i; // beginning of the entry
            // check when we reach a real entry
            while (entries.get(i) instanceof FatLfnDirEntry) {
                // System.out.println(" Jumped over : " + entries.get(i));
                i++;
                if (i >= size) {
                    // This is a cutted entry, forgive it
                    break;
                }
            }
            i++;
            if (i >= size) {
                // This is a cutted entry, forgive it
                break;
            }
            
            LfnEntry current = new LfnEntry(this, entries, offset, i - offset);

            if (!current.isDeleted() && current.isValid() && !current.isDotDir()) {
                shortNameIndex.put(current.getRealEntry().getName(), current);
                longFileNameIndex.put(current.getName(), current);
            }
        }

    }

    private void updateLFN() throws FileSystemException {
        ArrayList<FatBasicDirEntry> destination = new ArrayList<FatBasicDirEntry>();

        if (labelEntry != null) destination.add(labelEntry);

        for (LfnEntry currentEntry : shortNameIndex.values()) {
            FatBasicDirEntry[] encoded = currentEntry.compactForm();
            for (int i = 0; i < encoded.length; i++) {
                destination.add(encoded[i]);
            }
        }

        final int size = destination.size();
        if (entries.size() < size) {
            if (!canChangeSize(size)) {
                throw new RootDirectoryFullException(this.getFileSystem());
            }
        }

        boolean useAdd = false;
        for (int i = 0; i < size; i++) {
            if (!useAdd) {
                try {
                    entries.set(i, destination.get(i));
                } catch (ArrayIndexOutOfBoundsException aEx) {
                    useAdd = true;
                }
            }
            if (useAdd) {
                entries.add(i, destination.get(i));
            }
        }

        final int entireSize = entries.size();
        for (int i = size; i < entireSize; i++) {
            entries.set(i, null); // remove stale entries
        }

    }

    @Override
    public void flush() throws FileSystemException {
        updateLFN();
        
        super.flush();
    }

    @Override
    public Iterator<FSDirectoryEntry> iterator() {
        return new Iterator<FSDirectoryEntry>() {
            Iterator<LfnEntry> it = shortNameIndex.values().iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public FSDirectoryEntry next() {
                return it.next();
            }

            /**
             * @see java.util.Iterator#remove()
             */
            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }
    
    /**
     * Remove the entry with the given name from this directory.
     * 
     * @param name
     * @throws IOException
     */
    @Override
    public void remove(String name) throws IOException {
        name = name.trim();
        LfnEntry byLongName = longFileNameIndex.get(name);

        if (byLongName != null) {
            longFileNameIndex.remove(name);
            shortNameIndex.remove(byLongName.getRealEntry().getName());
            return;
        }

        String uppedName = name.toUpperCase();
        LfnEntry byShortName = shortNameIndex.get(uppedName);

        if (byShortName != null) {
            longFileNameIndex.remove(byShortName.getName());
            shortNameIndex.remove(uppedName);
        }
        
        throw new FileNotFoundException(name);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                " [size="  + shortNameIndex.size() + "]";
    }
    
}
