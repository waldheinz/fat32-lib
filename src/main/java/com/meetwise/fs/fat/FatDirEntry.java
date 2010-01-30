/*
 * $Id: FatDirEntry.java 4975 2009-02-02 08:30:52Z lsantha $
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

import com.meetwise.fs.util.DosUtils;
import com.meetwise.fs.util.LittleEndian;

/**
 * 
 *
 * @author Ewout Prangsma &lt;epr at jnode.org&gt;
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class FatDirEntry {
    
    public final static int OFFSET_LENGTH = 0x1c;
    
    private final AbstractDirectoryEntry entry;
    
    /**
     * Create a new entry from a FAT directory image.
     * 
     * @param dir
     * @param src
     * @param offset
     */
    public FatDirEntry(AbstractDirectoryEntry entry) {
        this.entry = entry;
    }

    public AbstractDirectoryEntry getEntry() {
        return entry;
    }
    
    public long getCreated() {
        return DosUtils.decodeDateTime(
                LittleEndian.getUInt16(entry.getData(), 0x10),
                LittleEndian.getUInt16(entry.getData(), 0x0e));
    }
    
    public void setCreated(long created) {
        LittleEndian.setInt16(entry.getData(), 0x0e,
                DosUtils.encodeTime(created));
        LittleEndian.setInt16(entry.getData(), 0x10,
                DosUtils.encodeDate(created));

        entry.markDirty();
    }

    public long getLastModified() {
        return DosUtils.decodeDateTime(
                LittleEndian.getUInt16(entry.getData(), 0x18),
                LittleEndian.getUInt16(entry.getData(), 0x16));
    }

    public void setLastModified(long lastModified) {
        LittleEndian.setInt16(entry.getData(), 0x16,
                DosUtils.encodeTime(lastModified));
        LittleEndian.setInt16(entry.getData(), 0x18,
                DosUtils.encodeDate(lastModified));

        entry.markDirty();
    }

    public long getLastAccessed() {
        return DosUtils.decodeDateTime(
                LittleEndian.getUInt16(entry.getData(), 0x12),
                0); /* time is not recorded */
    }
    
    public void setLastAccessed(long lastAccessed) {
        LittleEndian.setInt16(entry.getData(), 0x12,
                DosUtils.encodeDate(lastAccessed));
        entry.markDirty();
    }
    
    /**
     * Returns the deleted.
     * 
     * @return boolean
     */
    public boolean isDeleted() {
        return  (LittleEndian.getUInt8(entry.getData(), 0) == 0xe5);
    }
    
    /**
     * Returns the length.
     * 
     * @return long
     */
    public long getLength() {
        return LittleEndian.getUInt32(entry.getData(), 0x1c);
    }
    
    public void setLength(long length) throws IllegalArgumentException {
        if (length > Integer.MAX_VALUE)
            throw new IllegalArgumentException("too big");
        
        LittleEndian.setInt32(entry.getData(), 0x1c, (int) length);
        entry.markDirty();
    }

    /**
     * Returns the name.
     * 
     * @return String
     */
    public ShortName getName() {
        return ShortName.parse(entry);
    }
    
    public void setName(ShortName sn) {
        sn.write(entry);
    }

    /**
     * Returns the startCluster.
     * 
     * @return int
     */
    public long getStartCluster() {
        return LittleEndian.getUInt16(entry.getData(), 0x1a);
    }
    
    /**
     * Sets the startCluster.
     *
     * @param startCluster The startCluster to set
     */
    void setStartCluster(long startCluster) {
        if (startCluster > Integer.MAX_VALUE) throw new AssertionError();

        LittleEndian.setInt16(entry.getData(), 0x1a, (int) startCluster);
        entry.markDirty();
    }
    
}
