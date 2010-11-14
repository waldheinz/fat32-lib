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

import de.waldheinz.fs.AbstractFsObject;
import de.waldheinz.fs.FsDirectory;
import de.waldheinz.fs.FsDirectoryEntry;
import java.io.IOException;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 * @since 0.6
 */
public final class FatLfnDirectoryEntry
        extends AbstractFsObject
        implements FsDirectoryEntry {
    
    private final FatLfnDirectory parent;
    final FatDirectoryEntry realEntry;
    
    private String fileName;
    
    FatLfnDirectoryEntry(String name, ShortName sn,
            FatLfnDirectory parent, boolean directory) {
        
        super(false);
        
        this.parent = parent;
        this.fileName = name;
        
        final long now = System.currentTimeMillis();
        this.realEntry = FatDirectoryEntry.create(directory);
        this.realEntry.setShortName(sn);
        this.realEntry.setCreated(now);
        this.realEntry.setLastAccessed(now);
    }

    FatLfnDirectoryEntry(FatLfnDirectory parent,
            FatDirectoryEntry realEntry, String fileName) {
        
        super(parent.isReadOnly());
        
        this.parent = parent;
        this.realEntry = realEntry;
        this.fileName = fileName;
    }
    
    static FatLfnDirectoryEntry extract(
            FatLfnDirectory dir, int offset, int len) {
            
        final FatDirectoryEntry realEntry = dir.dir.getEntry(offset + len - 1);
        final String fileName;
        
        if (len == 1) {
            /* this is just an old plain 8.3 entry */
            fileName = realEntry.getShortName().asSimpleString();
        } else {
            /* stored in reverse order */
            final StringBuilder name = new StringBuilder(13 * (len - 1));
            
            for (int i = len - 2; i >= 0; i--) {
                FatDirectoryEntry entry = dir.dir.getEntry(i + offset);
                name.append(entry.getLfnPart());
            }
            
            fileName = name.toString().trim();
        }
        
        return new FatLfnDirectoryEntry(dir, realEntry, fileName);
    }

    /**
     * Returns if this directory entry has the FAT "hidden" flag set.
     *
     * @return if this is a hidden directory entry
     */
    public boolean isHidden() {
        return this.realEntry.isHiddenFlag();
    }

    /**
     * Sets the "hidden" flag on this {@code FatLfnDirectoryEntry} to the
     * specified value.
     *
     * @param hidden if this entry should have the hidden flag set
     */
    public void setHidden(boolean hidden) {
        this.realEntry.setHiddenFlag(hidden);
    }
    
    private int totalEntrySize() {
        int result = (fileName.length() / 13) + 1;

        if ((fileName.length() % 13) != 0) {
            result++;
        }
        
        return result;
    }

    FatDirectoryEntry[] compactForm() {
        if (this.realEntry.getShortName().equals(ShortName.DOT) ||
                this.realEntry.getShortName().equals(ShortName.DOT_DOT)) {
            /* the dot entries must not have a LFN */
            return new FatDirectoryEntry[]{this.realEntry};
        }
    
        final int totalEntrySize = totalEntrySize();

        final FatDirectoryEntry[] entries =
                new FatDirectoryEntry[totalEntrySize];

        final byte checkSum = this.realEntry.getShortName().checkSum();
        int j = 0;
        
        for (int i = totalEntrySize - 2; i > 0; i--) {
            entries[i] = createPart(fileName.substring(j * 13, j * 13 + 13),
                    j + 1, checkSum, false);
            j++;
        }

        entries[0] = createPart(fileName.substring(j * 13),
                j + 1, checkSum, true);
        
        entries[totalEntrySize - 1] = this.realEntry;
        
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
    public void setName(String newName) {
        checkWritable();
        
        fileName = newName;
        realEntry.setShortName(parent.sng.generateShortName(newName));
    }
    
    @Override
    public void setLastModified(long lastModified) {
        parent.checkReadOnly();
        realEntry.setLastModified(lastModified);
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
    public String toString() {
        return "LFN = " + fileName + " / SFN = " + realEntry.getShortName();
    }
    
    private static FatDirectoryEntry createPart(String subName,
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

    @Override
    public long getLastModified() throws IOException {
        return realEntry.getLastModified();
    }

    @Override
    public long getCreated() throws IOException {
        return realEntry.getCreated();
    }

    @Override
    public long getLastAccessed() throws IOException {
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
    public boolean isDirty() {
        return realEntry.isDirty();
    }
    
}
