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
import java.nio.ByteBuffer;

/**
 * 
 *
 * @author Ewout Prangsma &lt;epr at jnode.org&gt;
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class FatDirectoryEntry extends AbstractFsObject {
    
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
    
    private FatDirectoryEntry() {
        this(new byte[SIZE], false);
        
    }
    
    /**
     * Reads a {@code FatDirectoryEntry} from the specified {@code ByteBuffer}.
     * The buffer must have at least {@link #SIZE} bytes remaining. The entry
     * is read from the buffer's current position, and if this method returns
     * non-null the position will have advanced by {@link #SIZE} bytes,
     * otherwise the position will remain unchanged.
     *
     * @param buff the buffer to read the entry from
     * @param readOnly if the resulting {@code FatDirecoryEntry} should be
     *      read-only
     * @return the directory entry that was read from the buffer or {@code null}
     *      if there was no entry to read from the specified position (first
     *      byte was 0)
     */
    public static FatDirectoryEntry read(ByteBuffer buff, boolean readOnly) {
        assert (buff.remaining() >= SIZE);

        /* peek into the buffer to see if we're done with reading */
        
        if (buff.get(buff.position()) == 0) return null;

        /* read the directory entry */

        final byte[] data = new byte[SIZE];
        buff.get(data);
        return new FatDirectoryEntry(data, readOnly);
    }
    
    /**
     * Decides if this entry is a "volume label" entry according to the FAT
     * specification.
     *
     * @return if this is a volume label entry
     */
    public final boolean isVolumeLabel() {
        if (isLfnEntry()) return false;
        else return ((getFlags() & (F_DIRECTORY | F_VOLUME_ID)) == F_VOLUME_ID);
    }
    
    public final boolean isSystem() {
        return ((getFlags() & F_SYSTEM) != 0);
    }

    public final boolean isHidden() {
        return ((getFlags() & F_HIDDEN) != 0);
    }
    
    public final boolean isLabel() {
        return ((getFlags() & F_VOLUME_ID) != 0);
    }
    
    public final boolean isLfnEntry() {
        return isReadonlyFlag() && isSystem() &&
                isHidden() && isLabel();
    }

    public final boolean isDirty() {
        return dirty;
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
    
    public final boolean isDirectory() {
        return ((getFlags() & (F_DIRECTORY | F_VOLUME_ID)) == F_DIRECTORY);
    }
    
    public static FatDirectoryEntry create(boolean directory) {
        final FatDirectoryEntry result = new FatDirectoryEntry();

        if (directory) {
            result.setFlags(F_DIRECTORY);
        }
        
        /* initialize date and time fields */

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
            throw new UnsupportedOperationException("not a volume label");
            
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

    public final long getCreated() {
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

    public final long getLastModified() {
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

    public final long getLastAccessed() {
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
     * Returns the {@code ShortName} that is stored in this directory entry or
     * {@code null} if this entry has not been initialized.
     * 
     * @return the {@code ShortName} stored in this entry or {@code null}
     */
    public ShortName getShortName() {
        if (this.data[0] == 0) {
            return null;
        } else {
            return ShortName.parse(this.data);
        }
    }
    
    /**
     * Does this entry refer to a file?
     *
     * @return
     * @see org.jnode.fs.FSDirectoryEntry#isFile()
     */
    public final boolean isFile() {
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

    /**
     * Writes this directory entry into the specified buffer.
     *
     * @param buff the buffer to write this entry to
     */
    void write(ByteBuffer buff) {
        buff.put(data);
        this.dirty = false;
    }
    
    public boolean isReadonlyFlag() {
        return ((getFlags() & F_READONLY) != 0);
    }
    
    final String getLfnPart() {
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
