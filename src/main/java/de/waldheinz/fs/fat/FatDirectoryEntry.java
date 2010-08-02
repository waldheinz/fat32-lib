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

import java.nio.ByteBuffer;

/**
 * 
 *
 * @author Ewout Prangsma &lt;epr at jnode.org&gt;
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
class FatDirectoryEntry extends FatObject {
    
    /**
     * The size in bytes of an FAT directory entry.
     */
    public final static int SIZE = 32;

    /**
     * The offset to the flags byte in a directory entry.
     */
    public static final int FLAGS_OFFSET = 0x0b;

    public static final int F_READONLY = 0x01;
    public static final int F_HIDDEN = 0x02;
    public static final int F_SYSTEM = 0x04;
    public static final int F_VOLUME_ID = 0x08;
    public static final int F_DIRECTORY = 0x10;
    public static final int F_ARCHIVE = 0x20;
    
    protected final byte[] data;

    private boolean dirty;
    
    FatDirectoryEntry(byte[] data, boolean readOnly) {
        super(readOnly);
        
        this.data = data;
    }

    FatDirectoryEntry() {
        this(new byte[SIZE], false);
    }
    
    public static FatDirectoryEntry read(
            ByteBuffer buff, int offset, boolean readOnly) {
        
        final byte[] data = new byte[SIZE];
        buff.get(data, offset, SIZE);
        
        return new FatDirectoryEntry(data, readOnly);
    }
    
    /**
     * Decides if this entry is a "volume label" entry according to the FAT
     * specification.
     *
     * @return if this is a volume label entry
     */
    public boolean isVolumeLabel() {
        if (isLfnEntry()) return false;
        else return ((getFlags() & (F_DIRECTORY | F_VOLUME_ID)) == F_VOLUME_ID);
    }
    
    public boolean isSystem() {
        return ((getFlags() & F_SYSTEM) != 0);
    }

    public boolean isHidden() {
        return ((getFlags() & F_HIDDEN) != 0);
    }
    
    public boolean isLabel() {
        return ((getFlags() & F_VOLUME_ID) != 0);
    }
    
    public boolean isLfnEntry() {
        return isReadonlyFlag() && isSystem() &&
                isHidden() && isLabel();
    }

    /**
     * Returns the attribute.
     *
     * @return int
     */
    public int getFlags() {
        return LittleEndian.getUInt8(data, FLAGS_OFFSET);
    }
    
    /**
     * Sets the flags.
     *
     * @param flags
     */
    public void setFlags(int flags) {
        LittleEndian.setInt8(data, FLAGS_OFFSET, flags);
    }
    
    public boolean isDirectory() {
        return ((getFlags() & (F_DIRECTORY | F_VOLUME_ID)) == F_DIRECTORY);
    }
    
    public static FatDirectoryEntry create() {
        final FatDirectoryEntry result = new FatDirectoryEntry();

        final long now = System.currentTimeMillis();

        result.setCreated(now);
        result.setLastAccessed(now);
        result.setLastModified(now);

        return result;
    }
    
    public static FatDirectoryEntry createVolumeLabel(String volumeLabel) {
        final byte[] data = new byte[SIZE];
        
        System.arraycopy(
                    volumeLabel.getBytes(), 0,
                    data, 0,
                    volumeLabel.length());

        final FatDirectoryEntry result = new FatDirectoryEntry(data, false);
        result.setFlags(FatDirectoryEntry.F_VOLUME_ID);
        return result;
    }
    
    public String getVolumeLabel() {
        if (!isVolumeLabel())
            throw new IllegalStateException("not a volume label");
            
        final StringBuilder sb = new StringBuilder();
        
        for (int i=0; i < AbstractDirectory.MAX_LABEL_LENGTH; i++) {
            final byte b = this.data[i];
            
            if (b != 0) {
                sb.append((char) b);
            } else {
                break;
            }
        }
        
        return sb.toString();
    }

    public long getCreated() {
        return DosUtils.decodeDateTime(
                LittleEndian.getUInt16(data, 0x10),
                LittleEndian.getUInt16(data, 0x0e));
    }
    
    public void setCreated(long created) {
        LittleEndian.setInt16(data, 0x0e,
                DosUtils.encodeTime(created));
        LittleEndian.setInt16(data, 0x10,
                DosUtils.encodeDate(created));

        this.dirty = true;
    }

    public long getLastModified() {
        return DosUtils.decodeDateTime(
                LittleEndian.getUInt16(data, 0x18),
                LittleEndian.getUInt16(data, 0x16));
    }

    public void setLastModified(long lastModified) {
        LittleEndian.setInt16(data, 0x16,
                DosUtils.encodeTime(lastModified));
        LittleEndian.setInt16(data, 0x18,
                DosUtils.encodeDate(lastModified));

        this.dirty = true;
    }

    public long getLastAccessed() {
        return DosUtils.decodeDateTime(
                LittleEndian.getUInt16(data, 0x12),
                0); /* time is not recorded */
    }
    
    public void setLastAccessed(long lastAccessed) {
        LittleEndian.setInt16(data, 0x12,
                DosUtils.encodeDate(lastAccessed));

        this.dirty = true;
    }
    
    /**
     * Returns the deleted.
     * 
     * @return boolean
     */
    public boolean isDeleted() {
        return  (LittleEndian.getUInt8(data, 0) == 0xe5);
    }
    
    /**
     * Returns the length.
     * 
     * @return long
     */
    public long getLength() {
        return LittleEndian.getUInt32(data, 0x1c);
    }
    
    public void setLength(long length) throws IllegalArgumentException {
        if (length > Integer.MAX_VALUE)
            throw new IllegalArgumentException("too big");
        
        LittleEndian.setInt32(data, 0x1c, (int) length);
    }

    /**
     * Returns the name.
     * 
     * @return String
     */
    public ShortName getShortName() {
        return ShortName.parse(this.data);
    }
    
    /**
     * Does this entry refer to a file?
     *
     * @return
     * @see org.jnode.fs.FSDirectoryEntry#isFile()
     */
    public boolean isFile() {
        return ((getFlags() & (F_DIRECTORY | F_VOLUME_ID)) == 0);
    }
    
    public void setShortName(ShortName sn) {
        if (sn.equals(this.getShortName())) return;
        
        sn.write(this.data);
        this.dirty = true;
    }

    /**
     * Returns the startCluster.
     * 
     * @return int
     */
    public long getStartCluster() {
        return LittleEndian.getUInt16(data, 0x1a);
    }
    
    /**
     * Sets the startCluster.
     *
     * @param startCluster The startCluster to set
     */
    void setStartCluster(long startCluster) {
        if (startCluster > Integer.MAX_VALUE) throw new AssertionError();

        LittleEndian.setInt16(data, 0x1a, (int) startCluster);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() +
                " [name=" + getShortName() + "]"; //NOI18N
    }

    void write(ByteBuffer buff, int offset) {
        buff.put(data, offset, SIZE);
        this.dirty = false;
    }
    
    public boolean isReadonlyFlag() {
        return ((getFlags() & F_READONLY) != 0);
    }
    
}
