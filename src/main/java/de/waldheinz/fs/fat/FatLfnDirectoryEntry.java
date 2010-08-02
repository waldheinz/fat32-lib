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
public final class FatLfnDirectoryEntry
        extends FatDirectoryEntry
        implements FsDirectoryEntry {
    
    private final FatLfnDirectory parent;

    private String fileName;

    FatLfnDirectoryEntry(String name, ShortName sn, FatLfnDirectory parent) {
        this.parent = parent;
        this.fileName = name;
        super.setShortName(sn);
    }
    
    private int totalEntrySize() {
        int result = (fileName.length() / 13) + 1;

        if ((fileName.length() % 13) != 0) {
            result++;
        }
        
        return result;
    }

    FatDirectoryEntry[] compactForm() {
        if (getShortName().equals(ShortName.DOT) ||
                getShortName().equals(ShortName.DOT_DOT)) {
            /* the dot entries must not have a LFN */
            return new FatDirectoryEntry[]{this};
        }
    
        int totalEntrySize = totalEntrySize();

        final FatDirectoryEntry[] entries =
                new FatDirectoryEntry[totalEntrySize];

        final byte checkSum = getShortName().checkSum();
        int j = 0;
        
        for (int i = totalEntrySize - 2; i > 0; i--) {
            entries[i] = createLfnPart(fileName.substring(j * 13, j * 13 + 13),
                    j + 1, checkSum, false);
            j++;
        }

        entries[0] = createLfnPart(fileName.substring(j * 13),
                j + 1, checkSum, true);
        
        entries[totalEntrySize - 1] = this;
        
        return entries;
    }

    @Override
    public String getName() {
        checkValid();
        
        return fileName;
    }

    @Override
    public FsDirectory getParent() {
        checkValid();
        
        return parent;
    }

    @Override
    public long getCreated() {
        return super.getCreated();
    }

    @Override
    public long getLastModified() {
        return super.getLastModified();
    }

    @Override
    public long getLastAccessed() {
        return super.getLastAccessed();
    }

    @Override
    public boolean isFile() {
        return super.isFile();
    }

    @Override
    public boolean isDirectory() {
        return super.isDirectory();
    }

    @Override
    public void setName(String newName) {
        checkWritable();
        
        fileName = newName;
        super.setShortName(parent.sng.generateShortName(newName));
    }

    @Override
    public void setCreated(long created) {
        parent.checkReadOnly();
        super.setCreated(created);
    }

    @Override
    public void setLastModified(long lastModified) {
        parent.checkReadOnly();
        super.setLastModified(lastModified);
    }

    @Override
    public void setLastAccessed(long lastAccessed) {
        parent.checkReadOnly();
        super.setLastAccessed(lastAccessed);
    }

    @Override
    public FatFile getFile() throws IOException {
        return parent.getFile(this);
    }

    @Override
    public FsDirectory getDirectory() throws IOException {
        return parent.getDirectory(this);
    }

    @Override
    public String toString() {
        return "LFN = " + fileName + " / SFN = " + super.getShortName();
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
    
    static FatDirectoryEntry createLfnPart(String subName,
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

        final byte[] rawData = new byte[FatDirectoryEntry.SIZE];

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

        return new FatDirectoryEntry(rawData, false);
    }

    String getLfnPart() {
        final char[] unicodechar = new char[13];

        unicodechar[0] = (char) LittleEndian.getUInt16(data, 1);
        unicodechar[1] = (char) LittleEndian.getUInt16(data, 3);
        unicodechar[2] = (char) LittleEndian.getUInt16(data, 5);
        unicodechar[3] = (char) LittleEndian.getUInt16(data, 7);
        unicodechar[4] = (char) LittleEndian.getUInt16(data, 9);
        unicodechar[5] = (char) LittleEndian.getUInt16(data, 14);
        unicodechar[6] = (char) LittleEndian.getUInt16(data, 16);
        unicodechar[7] = (char) LittleEndian.getUInt16(data, 18);
        unicodechar[8] = (char) LittleEndian.getUInt16(data, 20);
        unicodechar[9] = (char) LittleEndian.getUInt16(data, 22);
        unicodechar[10] = (char) LittleEndian.getUInt16(data, 24);
        unicodechar[11] = (char) LittleEndian.getUInt16(data, 28);
        unicodechar[12] = (char) LittleEndian.getUInt16(data, 30);

        int end = 0;

        while ((end < 13) && (unicodechar[end] != '\0')) {
            end++;
        }

        return new String(unicodechar).substring(0, end);
    }
}
