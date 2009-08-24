/*
 * $Id$
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

/**
 * 
 *
 * @author Ewout Prangsma &lt; epr at jnode.org&gt;
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public enum FatType {

    /**
     * For a 12-bit file allocation table.
     */
    FAT12(0xFFFL, 1.5f) {
        public long readEntry(byte[] data, int index) {
            final int idx = (int) (index * 1.5);
            final int b1 = data[idx] & 0xFF;
            final int b2 = data[idx + 1] & 0xFF;
            final int v = (b2 << 8) | b1;
            
            if ((index % 2) == 0) {
                return v & 0xFFF;
            } else {
                return v >> 4;
            }
        }

        public void writeEntry(byte[] data, int index, long entry) {
            final int idx = (int) (index * 1.5);
            
            if ((index % 2) == 0) {
                data[idx] = (byte) (entry & 0xFF);
                data[idx + 1] = (byte) ((entry >> 8) & 0x0F);
            } else {
                data[idx] |= (byte) ((entry & 0x0F) << 4);
                data[idx + 1] = (byte) ((entry >> 4) & 0xFF);
            }
        }
    },

    /**
     * For a 16-bit file allocation table.
     */
    FAT16(0xFFFFL, 2.0f) {
        public long readEntry(byte[] data, int index) {
            final int idx = index * 2;
            final int b1 = data[idx] & 0xFF;
            final int b2 = data[idx + 1] & 0xFF;
            return (b2 << 8) | b1;
        }

        public void writeEntry(byte[] data, int index, long entry) {
            final int idx = index << 1;
            data[idx] = (byte) (entry & 0xFF);
            data[idx + 1] = (byte) ((entry >> 8) & 0xFF);
        }
    },

    /**
     * For a 32-bit file allocation table.
     */
    FAT32(0xFFFFFFFFL, 4.0f) {
        public long readEntry(byte[] data, int index) {
            final int idx = index * 4;
            final long l1 = data[idx] & 0xFF;
            final long l2 = data[idx + 1] & 0xFF;
            final long l3 = data[idx + 2] & 0xFF;
            final long l4 = data[idx + 3] & 0xFF;
            return (l4 << 24) | (l3 << 16) | (l2 << 8) | l1;
        }

        public void writeEntry(byte[] data, int index, long entry) {
            final int idx = index << 2;
            data[idx] = (byte) (entry & 0xFF);
            data[idx + 1] = (byte) ((entry >> 8) & 0xFF);
            data[idx + 2] = (byte) ((entry >> 16) & 0xFF);
            data[idx + 3] = (byte) ((entry >> 24) & 0xFF);
        }
    };

    private final long minReservedEntry;
    private final long maxReservedEntry;
    private final long eofCluster;
    private final long eofMarker;
    private final float entrySize;

    private FatType(long bitMask, float entrySize) {
        this.minReservedEntry = (0xFFFFFF0L & bitMask);
        this.maxReservedEntry = (0xFFFFFF6L & bitMask);
        this.eofCluster = (0xFFFFFF8L & bitMask);
        this.eofMarker = (0xFFFFFFFL & bitMask);
        this.entrySize = entrySize;
    }

    abstract long readEntry(byte[] data, int index);

    abstract void writeEntry(byte[] data, int index, long entry);

    final boolean isReservedCluster(long entry) {
        return ((entry >= minReservedEntry) && (entry <= maxReservedEntry));
    }

    final boolean isEofCluster(long entry) {
        return (entry >= eofCluster);
    }

    final long getEofMarker() {
        return eofMarker;
    }

    final float getEntrySize() {
        return entrySize;
    }
}
