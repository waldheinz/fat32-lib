/*
 * $Id: AbstractDirectoryEntry.java 4975 2009-02-02 08:30:52Z lsantha $
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

import com.meetwise.fs.util.LittleEndian;

/**
 * 
 * @author gbin
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
class AbstractDirectoryEntry extends FatObject {

    public static final int F_READONLY = 0x01;
    public static final int F_HIDDEN = 0x02;
    public static final int F_SYSTEM = 0x04;
    public static final int F_LABEL = 0x08;
    public static final int F_DIRECTORY = 0x10;
    public static final int F_ARCHIVE = 0x20;

    /**
     * The offset to the flags byte in a directory entry.
     */
    public static final int FLAGS_OFFSET = 0x0b;

    /**
     * The size in bytes of an FAT directory entry.
     */
    public final static int SIZE = 32;
    
    protected final byte[] rawData = new byte[SIZE];
    private final AbstractDirectory dir;

    private int flags;
    private boolean dirty;

    protected AbstractDirectoryEntry(AbstractDirectory dir) {
        this.dir = dir;
    }

    protected AbstractDirectoryEntry(AbstractDirectory dir,
            byte[] src, int offset) {
        
        System.arraycopy(src, offset, rawData, 0, SIZE);
        this.flags = LittleEndian.getUInt8(src, offset + FLAGS_OFFSET);
        this.dir = dir;
        this.dirty = false;
    }
    
    public AbstractDirectory getDir() {
        return dir;
    }
    
    public void write(byte[] dest, int offset) {
        System.arraycopy(rawData, 0, dest, offset, SIZE);
        LittleEndian.setInt8(dest, offset + FLAGS_OFFSET, flags);
    }
    
    /**
     * Returns the attribute.
     *
     * @return int
     */
    public int getFlags() {
        return flags;
    }
    
    /**
     * Sets the flags.
     *
     * @param flags
     */
    public void setFlags(int flags) {
        this.flags = flags;
        markDirty();
    }

    public boolean isReadonly() {
        return ((flags & F_READONLY) != 0);
    }

    public void setReadonly() {
        setFlags(flags | F_READONLY);
    }

    public boolean isHidden() {
        return ((flags & F_HIDDEN) != 0);
    }

    public void setHidden() {
        setFlags(flags | F_HIDDEN);
    }

    public boolean isSystem() {
        return ((flags & F_SYSTEM) != 0);
    }

    public void setSystem() {
        setFlags(flags | F_SYSTEM);
    }

    public boolean isLabel() {
        return ((flags & F_LABEL) != 0);
    }

    public boolean isDirectory() {
        return ((flags & F_DIRECTORY) != 0);
    }

    public void setDirectory() {
        setFlags(F_DIRECTORY);
    }

    public boolean isArchive() {
        return ((flags & F_ARCHIVE) != 0);
    }

    public void setArchive() {
        setFlags(flags | F_ARCHIVE);
    }

    protected void markDirty() {
        this.dirty = true;
        this.dir.setDirty();
    }

    public final boolean isDirty() {
        return this.dirty;
    }
}
