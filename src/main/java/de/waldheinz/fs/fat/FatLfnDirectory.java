/*
 * Copyright (C) 2003-2009 JNode.org
 *               2009,2010 Matthias Treydte <mt@waldheinz.de>
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

package de.waldheinz.fs.fat;

import de.waldheinz.fs.FsDirectory;
import de.waldheinz.fs.FsDirectoryEntry;
import de.waldheinz.fs.ReadOnlyException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class implements the "long file name" logic atop an
 * {@link AbstractDirectory} instance.
 *
 * @author gbin
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class FatLfnDirectory implements FsDirectory {
    private final Map<ShortName, LfnEntry> shortNameIndex;
    private final Map<String, LfnEntry> longNameIndex;
    private final Map<FatDirEntry, FatFile> files;
    private final Map<FatDirEntry, FatLfnDirectory> directories;
    private final ShortNameGenerator sng;
    private final AbstractDirectory dir;
    private final Fat fat;
    
    public FatLfnDirectory(AbstractDirectory dir, Fat fat) {
        if ((dir == null) || (fat == null)) throw new NullPointerException();
        
        this.fat = fat;
        this.dir = dir;
        this.shortNameIndex = new LinkedHashMap<ShortName, LfnEntry>();
        this.longNameIndex = new LinkedHashMap<String, LfnEntry>();
        this.sng = new ShortNameGenerator(shortNameIndex.keySet());
        this.files = new LinkedHashMap<FatDirEntry, FatFile>();
        this.directories = new LinkedHashMap<FatDirEntry, FatLfnDirectory>();
        
        parseLfn();
    }

    private void checkReadOnly() throws ReadOnlyException {
        if (dir.isReadOnly()) {
            throw new ReadOnlyException();
        }
    }
    
    private FatFile getFile(FatDirEntry entry) throws IOException {
        FatFile file = files.get(entry);

        if (file == null) {
            file = FatFile.get(fat, entry);
            files.put(entry, file);
        }
        
        return file;
    }

    private FsDirectory getDirectory(FatDirEntry entry) throws IOException {
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

    /**
     * <p>
     * {@inheritDoc}
     * </p><p>
     * According to the FAT file system specification, leading and trailing
     * spaces in the {@code name} are ignored by this method.
     * </p>
     * 
     * @param name {@inheritDoc}
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @Override
    public LfnEntry addFile(String name) throws IOException {
        checkReadOnly();
        
        name = name.trim();
        final ShortName shortName = makeShortName(name);
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

    private ShortName makeShortName(String name) throws IOException {
        try {
            return sng.generateShortName(name);
        } catch (IllegalArgumentException ex) {
            throw new IOException(
                    "could not generate short name for \"" + name + "\"", ex);
        }
    }
    
    /**
     * <p>
     * {@inheritDoc}
     * </p><p>
     * According to the FAT file system specification, leading and trailing
     * spaces in the {@code name} are ignored by this method.
     * </p>
     *
     * @param name {@inheritDoc}
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @Override
    public LfnEntry addDirectory(String name) throws IOException {
        checkReadOnly();
        
        name = name.trim();
        
        final ShortName sn = makeShortName(name);
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
            destination.addAll(Arrays.asList(encoded));
        }

        final int size = destination.size();

        dir.changeSize(size);
        dir.setEntries(destination);
    }

    @Override
    public void flush() throws IOException {
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
    public Iterator<FsDirectoryEntry> iterator() {
        return new Iterator<FsDirectoryEntry>() {

            Iterator<LfnEntry> it = shortNameIndex.values().iterator();

            @Override
            public boolean hasNext() {
                return it.hasNext();
            }

            @Override
            public FsDirectoryEntry next() {
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
     * @throws IllegalArgumentException on an attempt to remove the dot entries
     */
    @Override
    public void remove(String name)
            throws IOException, IllegalArgumentException {
        
        checkReadOnly();
        
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
    
    /**
     * <p>
     * {@inheritDoc}
     * </p><p>
     * According to the FAT file system specification, leading and trailing
     * spaces in the {@code name} are ignored by this method.
     * </p>
     * 
     * @param name {@inheritDoc}
     * @return {@inheritDoc}
     * @throws IOException {@inheritDoc}
     */
    @Override
    public FsDirectoryEntry getEntry(String name) throws IOException {
        return getEntryImpl(name);
    }
    
    class LfnEntry implements FsDirectoryEntry {
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
            if (realEntry.getName().equals(ShortName.DOT) ||
                    realEntry.getName().equals(ShortName.DOT_DOT)) {

                /* the dot entries must not have a LFN */
                return new AbstractDirectoryEntry[] { realEntry.getEntry() };
            }

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
        public FsDirectory getParent() {
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
            checkReadOnly();
            
            fileName = newName;
            realEntry.setName(sng.generateShortName(newName));
        }
        
        public void setCreated(long created) {
            checkReadOnly();

            realEntry.setCreated(created);
        }

        @Override
        public void setLastModified(long lastModified) {
            checkReadOnly();

            realEntry.setLastModified(lastModified);
        }

        public void setLastAccessed(long lastAccessed) {
            checkReadOnly();
            
            realEntry.setLastAccessed(lastAccessed);
        }

        @Override
        public FatFile getFile() throws IOException {
            return FatLfnDirectory.this.getFile(realEntry);
        }
        
        @Override
        public FsDirectory getDirectory() throws IOException {
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
            checkReadOnly();

            if (realEntry.getName().equals(ShortName.DOT) ||
                    realEntry.getName().equals(ShortName.DOT_DOT)) {

                throw new IllegalArgumentException(
                        "the dot entries can not be removed");
            }

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
