/*
 * $Id: BootSector.java 4975 2009-02-02 08:30:52Z lsantha $
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

import com.meetwise.fs.BlockDevice;
import com.meetwise.fs.Sector;
import com.meetwise.fs.util.LittleEndian;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The boot sector.
 *
 * @author Ewout Prangsma &lt;epr at jnode.org&gt;
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public abstract class BootSector extends Sector {

    /**
     * The size of a boot sector in bytes.
     */
    public final static int SIZE = 512;
    
    protected BootSector(byte[] src) {
        super(src);
        
        if (src.length != SIZE) throw new IllegalArgumentException(
                "boot sector must be " + SIZE + " bytes");
    }
    
    public static BootSector read(BlockDevice device) throws IOException {
        final byte[] bytes = Sector.readBytes(device, 0, SIZE);
        
        final int sectorsPerFat = LittleEndian.getUInt16(bytes, 0x16);
        if (sectorsPerFat == 0) return new Fat32BootSector(bytes);
        else return new Fat16BootSector(bytes);
    }

    public abstract FatType getFatType();
    
    
    /**
     * Gets the number of sectors/fat for FAT 12/16.
     * 
     * @return int
     */
    public abstract long getSectorsPerFat();
    
    /**
     * Sets the number of sectors/fat
     * 
     * @param v  the new number of sectors per fat
     */
    public abstract void setSectorsPerFat(long v);

    public abstract void setSectorCount(long count);

    public abstract int getRootDirEntryCount();
    
    public abstract long getSectorCount();

    public void init() {
        
    }

    /**
     * Write the contents of this bootsector to the given device.
     * 
     * @param device
     * @throws IOException on write error
     */
    public synchronized void write(BlockDevice device) throws IOException {
        device.write(0, ByteBuffer.wrap(data));
        dirty = false;
    }
    
    /**
     * Gets the OEM name
     * 
     * @return String
     */
    public String getOemName() {
        StringBuilder b = new StringBuilder(8);
        
        for (int i = 0; i < 8; i++) {
            int v = data[0x3 + i];
            if (v == 0) break;
            b.append((char) v);
        }
        
        return b.toString();
    }


    /**
     * Sets the OEM name, must be at most 8 characters long.
     *
     * @param name the new OEM name
     */
    public void setOemName(String name) {
        if (name.length() > 8) throw new IllegalArgumentException(
                "only 8 characters are allowed");

        for (int i = 0; i < 8; i++) {
            char ch;
            if (i < name.length()) {
                ch = name.charAt(i);
            } else {
                ch = (char) 0;
            }

            set8(0x3 + i, ch);
        }
    }

    /**
     * Sets the FAT type for this boot sector. This method updates the string
     * found at offset 0x36 in the boot sector.
     *
     * @param type the new FAT type
     */
    public void setFatType(FatType type) {
        
        for (int i = 0; i < 8; i++) {
            char ch;
            if (i < type.getLabel().length()) {
                ch = type.getLabel().charAt(i);
            } else {
                ch = (char) 0;
            }
            
            set8(0x36 + i, ch);
        }
    }

    /**
     * Gets the number of bytes/sector
     * 
     * @return int
     */
    public int getBytesPerSector() {
        return get16(0x0b);
    }

    /**
     * Sets the number of bytes/sector
     * 
     * @param v the new value for bytes per sector
     */
    public void setBytesPerSector(int v) {
        if (v == getBytesPerSector()) return;

        switch (v) {
            case 512: case 1024: case 2048: case 4096:
                set16(0x0b, v);
                break;
                
            default:
                throw new IllegalArgumentException();
        }
    }

    private static boolean isPowerOfTwo(int n) {
        return ((n!=0) && (n&(n-1))==0);
    }

    /**
     * Gets the number of sectors/cluster
     * 
     * @return int
     */
    public int getSectorsPerCluster() {
        return get8(0x0d);
    }

    /**
     * Sets the number of sectors/cluster
     *
     * @param v the new number of sectors per cluster
     */
    public void setSectorsPerCluster(int v) {
        if (v == getSectorsPerCluster()) return;
        if (!isPowerOfTwo(v)) throw new IllegalArgumentException(
                "value must be a power of two");
        
        set8(0x0d, v);
    }
    
    /**
     * Gets the number of reserved (for bootrecord) sectors
     * 
     * @return int
     */
    public int getNrReservedSectors() {
        return get16(0xe);
    }

    /**
     * Sets the number of reserved (for bootrecord) sectors
     * 
     * @param v the new number of reserved sectors
     */
    public void setNrReservedSectors(int v) {
        if (v == getNrReservedSectors()) return;
        if (v < 1) throw new IllegalArgumentException(
                "there must be >= 1 reserved sectors");
        set16(0xe, v);
    }

    /**
     * Gets the number of fats
     * 
     * @return int
     */
    public int getNrFats() {
        return get8(0x10);
    }

    /**
     * Sets the number of fats
     *
     * @param v the new number of fats
     */
    public void setNrFats(int v) {
        if (v == getNrFats()) return;
        
        set8(0x10, v);
    }


    public long getRootDirFirstCluster() {
        return get32(0x2c);
    }
    
    /**
     * Gets the number of logical sectors
     * 
     * @return int
     */
    protected int getNrLogicalSectors() {
        return get16(0x13);
    }
    
    /**
     * Sets the number of logical sectors
     * 
     * @param v the new number of logical sectors
     */
    protected void setNrLogicalSectors(int v) {
        if (v == getNrLogicalSectors()) return;
        
        set16(0x13, v);
    }
    
    protected void setNrTotalSectors(long v) {
        set32(0x20, v);
    }
    
    protected long getNrTotalSectors() {
        return get32(0x20);
    }

    /**
     * Gets the medium descriptor byte
     * 
     * @return int
     */
    public int getMediumDescriptor() {
        return get8(0x15);
    }

    /**
     * Sets the medium descriptor byte
     * 
     * @param v the new medium descriptor
     */
    public void setMediumDescriptor(int v) {
        set8(0x15, v);
    }
    
    /**
     * Gets the number of sectors/track
     * 
     * @return int
     */
    public int getSectorsPerTrack() {
        return get16(0x18);
    }

    /**
     * Sets the number of sectors/track
     *
     * @param v the new number of sectors per track
     */
    public void setSectorsPerTrack(int v) {
        if (v == getSectorsPerTrack()) return;
        
        set16(0x18, v);
    }

    /**
     * Gets the number of heads
     * 
     * @return int
     */
    public int getNrHeads() {
        return get16(0x1a);
    }

    /**
     * Sets the number of heads
     * 
     * @param v the new number of heads
     */
    public void setNrHeads(int v) {
        if (v == getNrHeads()) return;
        
        set16(0x1a, v);
    }

    /**
     * Gets the number of hidden sectors
     * 
     * @return int
     */
    public long getNrHiddenSectors() {
        return get32(0x1c);
    }

    /**
     * Sets the number of hidden sectors
     *
     * @param v the new number of hidden sectors
     */
    public void setNrHiddenSectors(long v) {
        if (v == getNrHiddenSectors()) return;
        
        set32(0x1c, v);
    }

    /**
     * Returns the dirty.
     * 
     * @return boolean
     */
    public boolean isDirty() {
        return dirty;
    }
    
    @Override
    public String toString() {
        StringBuilder res = new StringBuilder(1024);
        res.append("Bootsector :\n");
        res.append("oemName=");
        res.append(getOemName());
        res.append('\n');
        res.append("medium descriptor = ");
        res.append(getMediumDescriptor());
        res.append('\n');
        res.append("Nr heads = ");
        res.append(getNrHeads());
        res.append('\n');
        res.append("Sectors per track = ");
        res.append(getSectorsPerTrack());
        res.append('\n');
        res.append("Sector per cluster = ");
        res.append(getSectorsPerCluster());
        res.append('\n');
        res.append("byte per sector = ");
        res.append(getBytesPerSector());
        res.append('\n');
        res.append("Nr fats = ");
        res.append(getNrFats());
        res.append('\n');
        res.append("Nr hidden sectors = ");
        res.append(getNrHiddenSectors());
        res.append('\n');
        res.append("Nr logical sectors = ");
        res.append(getNrLogicalSectors());
        res.append('\n');
        res.append("Nr reserved sector = ");
        res.append(getNrReservedSectors());
        res.append('\n');
        
        return res.toString();
    }
}
