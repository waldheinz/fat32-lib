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

import com.meetwise.fs.FSDirectory;
import com.meetwise.fs.FSDirectoryEntry;
import com.meetwise.fs.FileSystemException;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * 
 *
 * @author gbin
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class FatLfnDirectory implements FSDirectory {

    private final Map<ShortName, LfnEntry> shortNameIndex =
            new HashMap<ShortName, LfnEntry>();
            
    private final Map<String, LfnEntry> longNameIndex =
            new HashMap<String, LfnEntry>();

    private final Map<FatDirEntry, FatFile> files;
    private final Map<FatDirEntry, FatDirectory> directories;

    private final ShortNameGenerator sng;

    private FatDirEntry labelEntry;
    private final AbstractDirectory dir;
    private final Fat fat;
    
    /**
     * @param fs
     * @param chain
     * @throws FileSystemException
     */
    public FatLfnDirectory(AbstractDirectory dir, Fat fat) {
        if (dir == null) throw new NullPointerException();
        
        this.fat = fat;
        this.dir = dir;
        this.sng = new ShortNameGenerator(shortNameIndex.keySet());
        this.files = new HashMap<FatDirEntry, FatFile>();
        this.directories = new HashMap<FatDirEntry, FatDirectory>();
        
        parseLfn();
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
                    dir.isReadOnly());
            files.put(entry, file);
        }
        
        return file;
    }

    /**
     * TODO: get rid of this method
     * @return
     */
    public AbstractDirectory getStorageDirectory() {
        return this.dir;
    }

    public boolean isDirty() {
        return dir.isDirty();
    }

    String getLabel() {
        if (labelEntry != null) return labelEntry.getName();
        else return null;
    }

    private void findLabelEntry() {
        for (int i=0; i < dir.getEntryCount(); i++) {
            if (dir.getEntry(i) instanceof FatDirEntry) {
                FatDirEntry e = (FatDirEntry) dir.getEntry(i);
                if (e.isLabel()) {
                    labelEntry = e;
                    dir.setEntry(i, null);
                    break;
                }
            }
        }
    }
    
    void setLabel(String label) throws IOException {
        throw new UnsupportedOperationException();
    }

    ShortNameGenerator getShortNameGenerator() {
        return sng;
    }

    @Override
    public LfnEntry addFile(String name) throws FileSystemException {    
        name = name.trim();
        final ShortName shortName = sng.generateShortName(name);
        FatDirEntry realEntry = new FatDirEntry(dir, shortName);
        LfnEntry entry = new LfnEntry(this, realEntry, name);
        shortNameIndex.put(shortName, entry);
        longNameIndex.put(name, entry);
        dir.setDirty();
        return entry;
    }

    @Override
    public FSDirectoryEntry addDirectory(String name) throws IOException {
        name = name.trim();
        final ShortName sn = sng.generateShortName(name);
        final FatDirEntry realEntry = new FatDirEntry(dir, sn);
        realEntry.setFlags(FatConstants.F_DIRECTORY);
        final FatFile f = getFile(realEntry);
        final FatDirectory fatDir = FatDirectory.create(f, dir.getStorageCluster(), false);
        realEntry.setStartCluster(fatDir.getStorageCluster());
        
        final LfnEntry entry = new LfnEntry(this, realEntry, name);
        shortNameIndex.put(sn, entry);
        longNameIndex.put(name, entry);
        dir.setDirty();
        flush();
        return entry;
    }

    @Override
    public FSDirectoryEntry getEntry(String name) {
        name = name.trim();

        final FSDirectoryEntry entry = longNameIndex.get(name);
        
        if (entry == null)
            return shortNameIndex.get(new ShortName(name));
        else
            return entry;

    }
    
    private void parseLfn() {
        int i = 0;
        int size = dir.getEntryCount();

        while (i < size) {
            // jump over empty entries
            while (i < size && dir.getEntry(i) == null) {
                i++;
            }

            if (i >= size) {
                break;
            }

            int offset = i; // beginning of the entry
            // check when we reach a real entry
            while (dir.getEntry(i) instanceof FatLfnDirEntry) {
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
            
            LfnEntry current = new LfnEntry(this, dir.entries, offset, i - offset);
            
            if (!current.isDeleted() && current.isValid() && !current.isDotDir()) {
                shortNameIndex.put(current.getRealEntry().getShortName(), current);
                longNameIndex.put(current.getName(), current);
            }
        }

    }
    
    private void updateLFN() throws IOException {
        ArrayList<AbstractDirectoryEntry> destination = new ArrayList<AbstractDirectoryEntry>();

        if (labelEntry != null) destination.add(labelEntry);
        
        for (LfnEntry currentEntry : shortNameIndex.values()) {
            AbstractDirectoryEntry[] encoded = currentEntry.compactForm();
            for (int i = 0; i < encoded.length; i++) {
                destination.add(encoded[i]);
            }
        }

        final int size = destination.size();
        
        dir.changeSize(size);
        
        boolean useAdd = false;
        for (int i = 0; i < size; i++) {
            if (!useAdd) {
                try {
                    dir.setEntry(i, destination.get(i));
                } catch (ArrayIndexOutOfBoundsException aEx) {
                    useAdd = true;
                }
            }
            
            if (useAdd) {
                dir.entries.add(i, destination.get(i));
            }
        }

        final int entireSize = dir.getEntryCount();
        for (int i = size; i < entireSize; i++) {
            dir.setEntry(i, null); // remove stale entries
        }

    }

    @Override
    public void flush() throws FileSystemException, IOException {
        for (FatFile f : files.values()) {
            f.flush();
        }
        
        updateLFN();
        dir.flush();
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
        LfnEntry byLongName = longNameIndex.get(name);
        
        if (byLongName != null) {
            longNameIndex.remove(name);
            shortNameIndex.remove(byLongName.getRealEntry().getShortName());
            return;
        }
        
        LfnEntry byShortName = shortNameIndex.get(new ShortName(name));

        if (byShortName != null) {
            longNameIndex.remove(byShortName.getName());
            shortNameIndex.remove(new ShortName(name));
        }
        
        throw new FileNotFoundException(name);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                " [size="  + shortNameIndex.size() + "]";
    }
    
}
