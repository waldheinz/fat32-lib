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
import java.io.IOException;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 * @since 0.6
 */
public final class FatLfnDirectoryEntry implements FsDirectoryEntry {
    private final FatDirEntry realEntry;
    private final FatLfnDirectory parent;

    private String fileName;

    FatLfnDirectoryEntry(FatDirEntry realEntry, String name,
            FatLfnDirectory lfnDir) {
        
        this.parent = lfnDir;
        this.realEntry = realEntry;
        this.fileName = name;
    }

    FatLfnDirectoryEntry(
            int offset, int length, FatLfnDirectory parent) {
        
        this.parent = parent;
        
        /* this is just an old plain 8.3 entry */
        if (length == 1) {
            realEntry = FatDirEntry.read(parent.dir.getEntry(offset));
            fileName = realEntry.getName().asSimpleString();
        } else {
            /* stored in reverse order */
            final StringBuilder name = new StringBuilder(13 * (length - 1));
            for (int i = length - 2; i >= 0; i--) {
                AbstractDirectoryEntry entry = parent.dir.getEntry(i + offset);
                name.append(getSubstring(entry));
            }
            fileName = name.toString().trim();
            realEntry = FatDirEntry.read(
                    parent.dir.getEntry(offset + length - 1));
        }
    }
    
    private int totalEntrySize() {
        int result = (fileName.length() / 13) + 1;
        if ((fileName.length() % 13) != 0) {
            result++;
        }
        return result;
    }

    AbstractDirectoryEntry[] compactForm() {
        if (realEntry.getName().equals(ShortName.DOT) ||
                realEntry.getName().equals(ShortName.DOT_DOT)) {
            /* the dot entries must not have a LFN */
            return new AbstractDirectoryEntry[]{realEntry.getEntry()};
        }
    
        int totalEntrySize = totalEntrySize();
        final AbstractDirectoryEntry[] entries =
                new AbstractDirectoryEntry[totalEntrySize];

        final byte checkSum = realEntry.getName().checkSum();
        int j = 0;
        for (int i = totalEntrySize - 2; i > 0; i--) {
            entries[i] = new AbstractDirectoryEntry(
                    parent.getStorageDirectory());

            set(entries[i], fileName.substring(
                    j * 13, j * 13 + 13), j + 1, checkSum, false);
            j++;
        }

        entries[0] = new AbstractDirectoryEntry(parent.getStorageDirectory());
        set(entries[0],
                fileName.substring(j * 13), j + 1, checkSum, true);
        entries[totalEntrySize - 1] = realEntry.getEntry();
        return entries;
    }

    @Override
    public String getName() {
        return fileName;
    }

    @Override
    public FsDirectory getParent() {
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
        return realEntry.getEntry().isFile();
    }

    @Override
    public boolean isDirectory() {
        return realEntry.getEntry().isDirectory();
    }

    @Override
    public void setName(String newName) {
        parent.checkReadOnly();
        fileName = newName;
        realEntry.setName(parent.sng.generateShortName(newName));
    }

    public void setCreated(long created) {
        parent.checkReadOnly();
        realEntry.setCreated(created);
    }

    @Override
    public void setLastModified(long lastModified) {
        parent.checkReadOnly();
        realEntry.setLastModified(lastModified);
    }

    public void setLastAccessed(long lastAccessed) {
        parent.checkReadOnly();
        realEntry.setLastAccessed(lastAccessed);
    }

    @Override
    public FatFile getFile() throws IOException {
        return parent.getFile(realEntry);
    }

    @Override
    public FsDirectory getDirectory() throws IOException {
        return parent.getDirectory(realEntry);
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
    FatDirEntry getRealEntry() {
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

    void remove() throws IOException {
        parent.checkReadOnly();

        if (realEntry.getName().equals(ShortName.DOT) ||
                realEntry.getName().equals(ShortName.DOT_DOT)) {
            throw new IllegalArgumentException(
                    "the dot entries can not be removed");
        }

        final ClusterChain cc = new ClusterChain(
                parent.fat, realEntry.getStartCluster(), false);
        
        cc.setChainLength(0);
        parent.longNameIndex.remove(this.getName());
        parent.shortNameIndex.remove(realEntry.getName());
        if (isFile()) {
            parent.files.remove(this.realEntry);
        } else {
            parent.files.remove(this.realEntry);
        }
        realEntry.remove();
        parent.updateLFN();
    }


    static void set(AbstractDirectoryEntry entry, String subName,
            int ordinal, byte checkSum, boolean isLast) {

        final char[] unicodechar = new char[13];
        subName.getChars(0, subName.length(), unicodechar, 0);

        for (int i=subName.length(); i < 13; i++) {
            if (i==subName.length()) {
                unicodechar[i] = 0x0000;
            } else {
                unicodechar[i] = 0xffff;
            }
        }

        final byte[] rawData = entry.getData();

        if (isLast) {
            LittleEndian.setInt8(rawData, 0, ordinal + (1 << 6));
        } else {
            LittleEndian.setInt8(rawData, 0, ordinal);
        }

        LittleEndian.setInt16(rawData, 1, unicodechar[0]);
        LittleEndian.setInt16(rawData, 3, unicodechar[1]);
        LittleEndian.setInt16(rawData, 5, unicodechar[2]);
        LittleEndian.setInt16(rawData, 7, unicodechar[3]);
        LittleEndian.setInt16(rawData, 9, unicodechar[4]);
        LittleEndian.setInt8(rawData, 11, 0x0f); // this is the hidden
                                                    // attribute tag for
        // lfn
        LittleEndian.setInt8(rawData, 12, 0); // reserved
        LittleEndian.setInt8(rawData, 13, checkSum); // checksum
        LittleEndian.setInt16(rawData, 14, unicodechar[5]);
        LittleEndian.setInt16(rawData, 16, unicodechar[6]);
        LittleEndian.setInt16(rawData, 18, unicodechar[7]);
        LittleEndian.setInt16(rawData, 20, unicodechar[8]);
        LittleEndian.setInt16(rawData, 22, unicodechar[9]);
        LittleEndian.setInt16(rawData, 24, unicodechar[10]);
        LittleEndian.setInt16(rawData, 26, 0); // sector... unused
        LittleEndian.setInt16(rawData, 28, unicodechar[11]);
        LittleEndian.setInt16(rawData, 30, unicodechar[12]);

    }

    static String getSubstring(AbstractDirectoryEntry entry) {
        final byte[] rawData = entry.getData();

        final char[] unicodechar = new char[13];

        unicodechar[0] = (char) LittleEndian.getUInt16(rawData, 1);
        unicodechar[1] = (char) LittleEndian.getUInt16(rawData, 3);
        unicodechar[2] = (char) LittleEndian.getUInt16(rawData, 5);
        unicodechar[3] = (char) LittleEndian.getUInt16(rawData, 7);
        unicodechar[4] = (char) LittleEndian.getUInt16(rawData, 9);
        unicodechar[5] = (char) LittleEndian.getUInt16(rawData, 14);
        unicodechar[6] = (char) LittleEndian.getUInt16(rawData, 16);
        unicodechar[7] = (char) LittleEndian.getUInt16(rawData, 18);
        unicodechar[8] = (char) LittleEndian.getUInt16(rawData, 20);
        unicodechar[9] = (char) LittleEndian.getUInt16(rawData, 22);
        unicodechar[10] = (char) LittleEndian.getUInt16(rawData, 24);
        unicodechar[11] = (char) LittleEndian.getUInt16(rawData, 28);
        unicodechar[12] = (char) LittleEndian.getUInt16(rawData, 30);

        int end = 0;

        while ((end < 13) && (unicodechar[end] != '\0')) {
            end++;
        }

        return new String(unicodechar).substring(0, end);
    }
}
