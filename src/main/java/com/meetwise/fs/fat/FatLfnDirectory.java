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
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;


/**
 * 
 *
 * @author gbin
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class FatLfnDirectory implements FSDirectory {

    private final HashMap<ShortName, LfnEntry> shortNameIndex =
            new HashMap<ShortName, LfnEntry>();
            
    private final HashMap<String, LfnEntry> longFileNameIndex =
            new HashMap<String, LfnEntry>();

    private final ShortNameGenerator sng;

    private FatDirEntry labelEntry;
    private final AbstractDirectory dir;
    
    /**
     * @param fs
     * @param chain
     * @throws FileSystemException
     */
    public FatLfnDirectory(AbstractDirectory dir) throws FileSystemException, IOException {
        this.dir = dir;
        this.sng = new ShortNameGenerator(shortNameIndex.keySet());
        
        parseLfn();
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
        if (!dir.isRoot()) {
            throw new IOException(
                    "volume name change on non-root directory"); //NOI18N
        }

        if (label != null) {
            Iterator<FSDirectoryEntry> i = iterator();
            FatDirEntry current;
            while (labelEntry == null && i.hasNext()) {
                current = (FatDirEntry) i.next();
                if (current.isLabel() &&
                        !(current.isHidden() && current.isReadonly() && current.isSystem())) {
                    labelEntry = current;
                }
            }

            if (labelEntry == null) {
                labelEntry = dir.addFatFile(label);
                labelEntry.setLabel();
            }

            labelEntry.setName(label);
        } else {
            labelEntry = null;
        }
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
        longFileNameIndex.put(name, entry);
        dir.setDirty();
        return entry;
    }

    @Override
    public FSDirectoryEntry addDirectory(String name) throws IOException {
        name = name.trim();
        final ShortName sn = sng.generateShortName(name);
        FatDirEntry realEntry = new FatDirEntry(dir, sn);
        
        final int clusterSize = dir.getClusterSize();
        realEntry.setFlags(FatConstants.F_DIRECTORY);
        final FatFile f = realEntry.getFatFile();
        f.setLength(clusterSize);
        
        final ByteBuffer buf = ByteBuffer.allocate(clusterSize);

        f.write(0, buf);
        f.getDirectory().initialize(f.getStartCluster(), dir.getStorageCluster());
        
        LfnEntry entry = new LfnEntry(this, realEntry, name);
        shortNameIndex.put(sn, entry);
        longFileNameIndex.put(name, entry);
        dir.setDirty();
        flush();
        return entry;
    }

    @Override
    public FSDirectoryEntry getEntry(String name) {
        name = name.trim();

        final FSDirectoryEntry entry = longFileNameIndex.get(name);
        
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
            
            LfnEntry current = new LfnEntry(this, entries, offset, i - offset);

            if (!current.isDeleted() && current.isValid() && !current.isDotDir()) {
                shortNameIndex.put(current.getRealEntry().getShortName(), current);
                longFileNameIndex.put(current.getName(), current);
            }
        }

    }
    
    private void updateLFN() throws IOException {
        ArrayList<FatBasicDirEntry> destination = new ArrayList<FatBasicDirEntry>();

        if (labelEntry != null) destination.add(labelEntry);

        for (LfnEntry currentEntry : shortNameIndex.values()) {
            FatBasicDirEntry[] encoded = currentEntry.compactForm();
            for (int i = 0; i < encoded.length; i++) {
                destination.add(encoded[i]);
            }
        }

        final int size = destination.size();
        if (dir.getCapacity() < size) {
            if (!dir.canChangeSize(size)) {
                throw new IOException("root directory is full");
            }
        }
        
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
                entries.add(i, destination.get(i));
            }
        }

        final int entireSize = dir.getEntryCount();
        for (int i = size; i < entireSize; i++) {
            dir.setEntry(i, null); // remove stale entries
        }

    }

    @Override
    public void flush() throws FileSystemException, IOException {
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
        LfnEntry byLongName = longFileNameIndex.get(name);
        
        if (byLongName != null) {
            longFileNameIndex.remove(name);
            shortNameIndex.remove(byLongName.getRealEntry().getShortName());
            return;
        }
        
        LfnEntry byShortName = shortNameIndex.get(new ShortName(name));

        if (byShortName != null) {
            longFileNameIndex.remove(byShortName.getName());
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
