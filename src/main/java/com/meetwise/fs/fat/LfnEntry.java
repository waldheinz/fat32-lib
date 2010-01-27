/*
 * $Id: LfnEntry.java 4975 2009-02-02 08:30:52Z lsantha $
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

import java.io.IOException;
import java.util.List;

import com.meetwise.fs.FSDirectory;
import com.meetwise.fs.FSDirectoryEntry;
import com.meetwise.fs.FSFile;

/**
 * 
 * @author gbin
 */
class LfnEntry implements FSDirectoryEntry {
    private String fileName;
    private final FatLfnDirectory parent;
    private final FatDirEntry realEntry;
    
    public LfnEntry(FatLfnDirectory parent, FatDirEntry realEntry, String longName) {
        this.realEntry = realEntry;
        this.parent = parent;
        fileName = longName.trim();
    }

    public LfnEntry(FatLfnDirectory parent, List<?> entries, int offset, int length) {
        this.parent = parent;
        // this is just an old plain 8.3 entry, copy it;
        if (length == 1) {
            realEntry = (FatDirEntry) entries.get(offset);
            fileName = realEntry.getName();
            return;
        }
        // stored in reversed order
        StringBuilder name = new StringBuilder(13 * (length - 1));
        for (int i = length - 2; i >= 0; i--) {
            FatLfnDirEntry entry = (FatLfnDirEntry) entries.get(i + offset);
            name.append(entry.getSubstring());
        }
        fileName = name.toString().trim();
        realEntry = (FatDirEntry) entries.get(offset + length - 1);
    }

    public AbstractDirectoryEntry[] compactForm() {
        int totalEntrySize = (fileName.length() / 13) + 1; // + 1 for the real

        if ((fileName.length() % 13) != 0) // there is a remaining part
            totalEntrySize++;
            
        AbstractDirectoryEntry[] entries = new AbstractDirectoryEntry[totalEntrySize];
        int j = 0;
        int checkSum = calculateCheckSum();
        for (int i = totalEntrySize - 2; i > 0; i--) {
            entries[i] =
                    new FatLfnDirEntry(parent.getStorageDirectory(),
                    fileName.substring(j * 13, j * 13 + 13), j + 1,
                            (byte) checkSum, false);
            j++;
        }

        entries[0] =
                new FatLfnDirEntry(parent.getStorageDirectory(),
                fileName.substring(j * 13), j + 1, (byte) checkSum, true);
        entries[totalEntrySize - 1] = realEntry;
        return entries;

    }

    private byte calculateCheckSum() {

        char[] fullName = new char[] {' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' ', ' '};
        char[] name = realEntry.getShortName().getName().toCharArray();
        char[] ext = realEntry.getShortName().getExt().toCharArray();
        System.arraycopy(name, 0, fullName, 0, name.length);
        System.arraycopy(ext, 0, fullName, 8, ext.length);

        byte[] dest = new byte[11];
        for (int i = 0; i < 11; i++)
            dest[i] = (byte) fullName[i];

        int sum = dest[0];
        for (int i = 1; i < 11; i++) {
            sum = dest[i] + (((sum & 1) << 7) + ((sum & 0xfe) >> 1));
        }

        return (byte) (sum & 0xff);
    }

    @Override
    public String getName() {
        return fileName;
    }

    @Override
    public FSDirectory getParent() {
        return parent;
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
        return realEntry.isFile();
    }

    @Override
    public boolean isDirectory() {
        return realEntry.isDirectory();
    }

    @Override
    public void setName(String newName) {
        fileName = newName;
        realEntry.setName(parent.getShortNameGenerator().
                generateShortName(newName));
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
    public FSFile getFile() throws IOException {
        return parent.getFile(realEntry);
    }

    @Override
    public FSDirectory getDirectory() throws IOException {
        return parent.getFile(realEntry).getDirectory();
    }

    @Override
    public boolean isValid() {
        return realEntry.isValid();
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
    
    boolean isDotDir() {
        if (getName().equals(".")) return true;
        if (getName().equals("..")) return true;
        
        return false;
    }
}
