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
 * @since 0.6
 */
public final class FatLfnDirectory implements FsDirectory {
    final Map<ShortName, FatLfnDirectoryEntry> shortNameIndex;
    final Map<String, FatLfnDirectoryEntry> longNameIndex;
    final Map<FatDirectoryEntry, FatFile> files;
    private final Map<FatDirectoryEntry, FatLfnDirectory> directories;
    final ShortNameGenerator sng;
    final AbstractDirectory dir;
    final Fat fat;
    
    FatLfnDirectory(AbstractDirectory dir, Fat fat) {
        if ((dir == null) || (fat == null)) throw new NullPointerException();
        
        this.fat = fat;
        this.dir = dir;
        this.shortNameIndex =
                new LinkedHashMap<ShortName, FatLfnDirectoryEntry>();
        this.longNameIndex = new LinkedHashMap<String, FatLfnDirectoryEntry>();
        this.sng = new ShortNameGenerator(shortNameIndex.keySet());
        this.files = new LinkedHashMap<FatDirectoryEntry, FatFile>();
        this.directories = new LinkedHashMap<FatDirectoryEntry, FatLfnDirectory>();
        
        parseLfn();
    }
    
    void checkReadOnly() throws ReadOnlyException {
        if (dir.isReadOnly()) {
            throw new ReadOnlyException();
        }
    }
    
    FatFile getFile(FatDirectoryEntry entry) throws IOException {
        FatFile file = files.get(entry);

        if (file == null) {
            file = FatFile.get(fat, entry);
            files.put(entry, file);
        }
        
        return file;
    }

    FsDirectory getDirectory(FatDirectoryEntry entry) throws IOException {
        FatLfnDirectory result = directories.get(entry);

        if (result == null) {
            final ClusterChainDirectory storage = read(entry, fat);
            result = new FatLfnDirectory(storage, fat);
            directories.put(entry, result);
        }
        
        return result;
    }
    
    AbstractDirectory getStorageDirectory() {
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
    public FatLfnDirectoryEntry addFile(String name) throws IOException {
        checkReadOnly();
        
        name = name.trim();
        final ShortName shortName = makeShortName(name);
        final AbstractDirectoryEntry entryData =
                new AbstractDirectoryEntry();
        FatDirectoryEntry realEntry = FatDirectoryEntry.create(entryData);
        realEntry.setName(shortName);

        final FatLfnDirectoryEntry entry =
                new FatLfnDirectoryEntry(realEntry, name, this);

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
    public FatLfnDirectoryEntry addDirectory(String name) throws IOException {
        checkReadOnly();
        
        name = name.trim();
        
        final ShortName sn = makeShortName(name);
        final FatDirectoryEntry realEntry = createSub(dir, fat);
        
        realEntry.setName(sn);
        
        final FatLfnDirectoryEntry entry =
                new FatLfnDirectoryEntry(realEntry, name, this);
        
        try {
            dir.addEntries(entry.compactForm());
        } catch (IOException ex) {
            new ClusterChain(
                    fat, realEntry.getStartCluster(), false).setChainLength(0);
//            realEntry.getEntry().remove();
            dir.removeEntry(realEntry.getEntry());
            throw ex;
        }
        
        shortNameIndex.put(sn, entry);
        longNameIndex.put(name, entry);

        getDirectory(realEntry);
        
        flush();
        return entry;
    }
    
    private FatLfnDirectoryEntry getEntryImpl(String name) {
        name = name.trim();
        
        final FatLfnDirectoryEntry entry = longNameIndex.get(name);

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
            
            final FatLfnDirectoryEntry current =
                    new FatLfnDirectoryEntry(offset, ++i - offset, this);
            
            if (!current.isDeleted() && current.isValid()) {
                shortNameIndex.put(current.getRealEntry().getName(), current);
                longNameIndex.put(current.getName(), current);
            }
        }
    }

    void updateLFN() throws IOException {
        ArrayList<AbstractDirectoryEntry> destination =
                new ArrayList<AbstractDirectoryEntry>();

        for (FatLfnDirectoryEntry currentEntry : shortNameIndex.values()) {
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

            final Iterator<FatLfnDirectoryEntry> it =
                    shortNameIndex.values().iterator();

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
        
        final FatLfnDirectoryEntry entry = getEntryImpl(name);
        if (entry == null) return;
        removeImpl(entry);
    }

    private void removeImpl(FatLfnDirectoryEntry entry) throws IOException {
        final FatDirectoryEntry realEntry = entry.getRealEntry();
        
        if (realEntry.getName().equals(ShortName.DOT) ||
                realEntry.getName().equals(ShortName.DOT_DOT)) {
            throw new IllegalArgumentException(
                    "the dot entries can not be removed");
        }

        final ClusterChain cc = new ClusterChain(
                fat, realEntry.getStartCluster(), false);

        cc.setChainLength(0);
        longNameIndex.remove(entry.getName());
        shortNameIndex.remove(realEntry.getName());

        if (entry.isFile()) {
            files.remove(realEntry);
        } else {
            directories.remove(realEntry);
        }

        updateLFN();
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

    private static ClusterChainDirectory read(FatDirectoryEntry entry, Fat fat)
            throws IOException {

        if (!entry.getEntry().isDirectory()) throw
                new IllegalArgumentException(entry + " is no directory");

        final ClusterChain chain = new ClusterChain(
                fat, entry.getStartCluster(),
                entry.getEntry().isReadOnly());

        final ClusterChainDirectory result =
                new ClusterChainDirectory(chain, false);

        result.read();
        return result;
    }
    
    private static FatDirectoryEntry createSub(
            AbstractDirectory parent, Fat fat) throws IOException {

        final ClusterChain chain = new ClusterChain(fat, false);
        chain.setChainLength(1);

        final AbstractDirectoryEntry entryData =
                new AbstractDirectoryEntry();

        final FatDirectoryEntry realEntry = FatDirectoryEntry.create(entryData);
        realEntry.getEntry().setFlags(AbstractDirectoryEntry.F_DIRECTORY);
        realEntry.setStartCluster(chain.getStartCluster());

        final ClusterChainDirectory dir =
                new ClusterChainDirectory(chain, false);

        /* add "." entry */

        final AbstractDirectoryEntry dot = new AbstractDirectoryEntry();
        dot.setFlags(AbstractDirectoryEntry.F_DIRECTORY);
        final FatDirectoryEntry dotEntry = FatDirectoryEntry.create(dot);
        dotEntry.setName(ShortName.DOT);
        dotEntry.setStartCluster((int) dir.getStorageCluster());
        copyDateTimeFields(realEntry, dotEntry);
        dir.addEntry(dot);

        /* add ".." entry */

        final AbstractDirectoryEntry dotDot =
                new AbstractDirectoryEntry();
        dotDot.setFlags(AbstractDirectoryEntry.F_DIRECTORY);
        final FatDirectoryEntry dotDotEntry = FatDirectoryEntry.create(dotDot);
        dotDotEntry.setName(ShortName.DOT_DOT);
        dotDotEntry.setStartCluster((int) parent.getStorageCluster());
        copyDateTimeFields(realEntry, dotDotEntry);
        dir.addEntry(dotDot);

        dir.flush();

        return realEntry;
    }

    private static void copyDateTimeFields(
            FatDirectoryEntry src, FatDirectoryEntry dst) {
        
        dst.setCreated(src.getCreated());
        dst.setLastAccessed(src.getLastAccessed());
        dst.setLastModified(src.getLastModified());
    }
}
