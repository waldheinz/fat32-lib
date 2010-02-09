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
import com.meetwise.fs.ReadOnlyFileSystemException;
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
    private final Map<ShortName, LfnEntry> shortNameIndex;
    private final Map<String, LfnEntry> longNameIndex;
    private final Map<FatDirEntry, FatFile> files;
    private final Map<FatDirEntry, FatLfnDirectory> directories;
    private final ShortNameGenerator sng;
    private final AbstractDirectory dir;
    private final Fat fat;

    /**
     * @param fs
     * @param chain
     * @throws FileSystemException
     */
    public FatLfnDirectory(AbstractDirectory dir, Fat fat) {
        if ((dir == null) || (fat == null)) throw new NullPointerException();

        this.fat = fat;
        this.dir = dir;
        this.shortNameIndex = new HashMap<ShortName, LfnEntry>();
        this.longNameIndex = new HashMap<String, LfnEntry>();
        this.sng = new ShortNameGenerator(shortNameIndex.keySet());
        this.files = new HashMap<FatDirEntry, FatFile>();
        this.directories = new HashMap<FatDirEntry, FatLfnDirectory>();

        parseLfn();
    }

    /**
     * Gets the file for the given entry.
     *
     * @param entry
     * @return
     */
    private FatFile getFile(FatDirEntry entry) throws IOException {
        FatFile file = files.get(entry);

        if (file == null) {
            file = FatFile.get(fat, entry);
            files.put(entry, file);
        }
        
        return file;
    }

    private FSDirectory getDirectory(FatDirEntry entry) throws IOException {
        FatLfnDirectory result = directories.get(entry);

        if (result == null) {
            final FatDirectory storage = FatDirectory.read(entry, fat);
            result = new FatLfnDirectory(storage, fat);
            directories.put(entry, result);
        }
        
        return result;
    }
    
    public AbstractDirectory getStorageDirectory() {
        return this.dir;
    }
    
    @Override
    public LfnEntry addFile(String name) throws IOException {
        name = name.trim();
        final ShortName shortName = sng.generateShortName(name);
        final AbstractDirectoryEntry entryData = new AbstractDirectoryEntry(dir);
        FatDirEntry realEntry = FatDirEntry.create(entryData);
        realEntry.setName(shortName);
        final LfnEntry entry = new LfnEntry(realEntry, name);

        dir.addEntries(entry.compactForm());
        
        shortNameIndex.put(shortName, entry);
        longNameIndex.put(name, entry);

        getFile(realEntry);
        
        dir.setDirty();
        return entry;
    }
    
    @Override
    public LfnEntry addDirectory(String name) throws IOException {
        name = name.trim();
        final ShortName sn = sng.generateShortName(name);
        final FatDirectory newDir = FatDirectory.createSub(dir, fat);
        final FatDirEntry realEntry = newDir.getEntry();
        
        realEntry.setName(sn);
        
        final LfnEntry entry = new LfnEntry(realEntry, name);
        
        try {
            dir.addEntries(entry.compactForm());
        } catch (IOException ex) {
            newDir.delete();
            throw ex;
        }
        
        shortNameIndex.put(sn, entry);
        longNameIndex.put(name, entry);

        getDirectory(realEntry);
        
        flush();
        return entry;
    }
    
    private LfnEntry getEntryImpl(String name) {
        name = name.trim();
        
        final LfnEntry entry = longNameIndex.get(name);

        if (entry == null) {
            if (!ShortName.canConvert(name)) return null;
            return shortNameIndex.get(ShortName.get(name));
        } else {
            return entry;
        }
    }
    
    private void parseLfn() {
        int i = 0;
        final int size = dir.getEntryCount();
        
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
            while (dir.getEntry(i).isLfnEntry()) {
                i++;
                if (i >= size) {
                    // This is a cutted entry, forgive it
                    break;
                }
            }
            
            if (i >= size) {
                // This is a cutted entry, forgive it
                break;
            }
            
            final LfnEntry current = new LfnEntry(offset, ++i - offset);
            
            if (!current.isDeleted() && current.isValid()) {
                shortNameIndex.put(current.getRealEntry().getName(), current);
                longNameIndex.put(current.getName(), current);
            }
        }
    }

    private void updateLFN() throws IOException {
        ArrayList<AbstractDirectoryEntry> destination =
                new ArrayList<AbstractDirectoryEntry>();

        for (LfnEntry currentEntry : shortNameIndex.values()) {
            AbstractDirectoryEntry[] encoded = currentEntry.compactForm();
            for (int i = 0; i < encoded.length; i++) {
                destination.add(encoded[i]);
            }
        }

        final int size = destination.size();

        dir.changeSize(size);
        dir.setEntries(destination);
    }

    @Override
    public void flush() throws FileSystemException, IOException {
        for (FatFile f : files.values()) {
            f.flush();
        }
        
        for (FatLfnDirectory d : directories.values()) {
            d.flush();
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
     * @param name the name of the entry to remove
     * @throws IOException on error removing the entry
     */
    @Override
    public void remove(String name) throws IOException {
        if (dir.isReadOnly()) throw new ReadOnlyFileSystemException(null);
        final LfnEntry entry = getEntryImpl(name);
        if (entry == null) return;
        entry.remove();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() +
                " [size=" + shortNameIndex.size() + //NOI18N
                ", dir=" + dir + "]"; //NOI18N
    }

    @Override
    public FSDirectoryEntry getEntry(String name) throws IOException {
        return getEntryImpl(name);
    }

    class LfnEntry implements FSDirectoryEntry {
        private String fileName;
        private final FatDirEntry realEntry;

        public LfnEntry(FatDirEntry realEntry, String name) {
            this.realEntry = realEntry;
            this.fileName = name;
        }

        public LfnEntry(int offset, int length) {
            /* this is just an old plain 8.3 entry */
            if (length == 1) {
                realEntry = FatDirEntry.read(dir.getEntry(offset));
                fileName = realEntry.getName().asSimpleString();
            } else {
                /* stored in reverse order */
                final StringBuilder name = new StringBuilder(13 * (length - 1));

                for (int i = length - 2; i >= 0; i--) {
                    AbstractDirectoryEntry entry = dir.getEntry(i + offset);
                    name.append(FatLfnDirEntry.getSubstring(entry));
                }
                
                fileName = name.toString().trim();
                realEntry = FatDirEntry.read(dir.getEntry(offset + length - 1));
            }
        }
        
        public int totalEntrySize() {
            int result = (fileName.length() / 13) + 1;
            if ((fileName.length() % 13) != 0) result++;
            return result;
        }

        public AbstractDirectoryEntry[] compactForm() {
            int totalEntrySize = totalEntrySize();
            
            final AbstractDirectoryEntry[] entries =
                    new AbstractDirectoryEntry[totalEntrySize];
            
            int j = 0;
            final byte checkSum = realEntry.getName().checkSum();
            
            for (int i = totalEntrySize - 2; i > 0; i--) {
                entries[i] = new AbstractDirectoryEntry(getStorageDirectory());
                
                FatLfnDirEntry.set(entries[i],
                        fileName.substring(j * 13, j * 13 + 13), j + 1,
                        checkSum, false);
                j++;
            }

            entries[0] = new AbstractDirectoryEntry(getStorageDirectory());
            FatLfnDirEntry.set(entries[0], fileName.substring(j * 13),
                    j + 1, checkSum, true);
            
            entries[totalEntrySize - 1] = realEntry.getEntry();
            
            return entries;
        }

        @Override
        public String getName() {
            return fileName;
        }

        @Override
        public FSDirectory getParent() {
            return FatLfnDirectory.this;
        }

        @Override
        public long getCreated() {
            return realEntry.getCreated();
        }

        @Override
        public long getLastModified() {
            return realEntry.getLastModified();
        }

        @Override
        public long getLastAccessed() {
            return realEntry.getLastAccessed();
        }

        @Override
        public boolean isFile() {
            return realEntry.getEntry().isFile();
        }

        @Override
        public boolean isDirectory() {
            return realEntry.getEntry().isDirectory();
        }
        
        @Override
        public void setName(String newName) {
            fileName = newName;
            realEntry.setName(sng.generateShortName(newName));
        }
        
        public void setCreated(long created) {
            realEntry.setCreated(created);
        }

        @Override
        public void setLastModified(long lastModified) {
            realEntry.setLastModified(lastModified);
        }

        public void setLastAccessed(long lastAccessed) {
            realEntry.setLastAccessed(lastAccessed);
        }

        @Override
        public FatFile getFile() throws IOException {
            return FatLfnDirectory.this.getFile(realEntry);
        }
        
        @Override
        public FSDirectory getDirectory() throws IOException {
            return FatLfnDirectory.this.getDirectory(realEntry);
        }

        @Override
        public boolean isValid() {
            return realEntry.getEntry().isValid();
        }

        public boolean isDeleted() {
            return realEntry.isDeleted();
        }

        @Override
        public String toString() {
            return "LFN = " + fileName + " / SFN = " + realEntry.getName();
        }

        /**
         * @return Returns the realEntry.
         */
        public FatDirEntry getRealEntry() {
            return realEntry;
        }

        /**
         * Indicate if the entry has been modified in memory (ie need to be saved)
         *
         * @return true if the entry need to be saved
         */
        @Override
        public boolean isDirty() {
            return true;
        }

        private void remove() throws IOException {
            final ClusterChain cc = new ClusterChain(
                    fat, realEntry.getStartCluster(), false);
            
            cc.setChainLength(0);
            
            longNameIndex.remove(this.getName());
            shortNameIndex.remove(realEntry.getName());
            
            if (isFile()) {
                files.remove(this.realEntry);
            } else {
                files.remove(this.realEntry);
            }
            
            realEntry.remove();
            updateLFN();
        }
    }
}
