/*
 * $Id: Fat.java 4975 2009-02-02 08:30:52Z lsantha $
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
 
package org.jnode.fs.fat;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.util.Arrays;

import org.jnode.driver.block.BlockDevice;
import org.jnode.fs.FileSystemException;
import org.jnode.fs.FileSystemFullException;

/**
 * 
 *
 * @author Ewout Prangsma &lt; epr at jnode.org&gt;
 * @author Matthias Treydte
 */
public final class Fat {

    private final long[] entries;

    /** The type of FAT */
    private final FatType fatType;

    /** The number of sectors this fat takes */
    private final int nrSectors;
    
    /** The number of bytes/sector */
    private final int sectorSize;

    private final FatFileSystem fs;

    /**
     * If this fat needs to be written to disk.
     */
    private boolean dirty;

    /** entry index for find next free entry */
    private int lastFreeCluster = 2;

    /**
     * Create a new instance
     * 
     * @param fs the {@link FatFileSystem} this FAT is part of
     * @param mediumDescriptor
     * @param sectors
     * @param sectorSize
     */
    public Fat(FatFileSystem fs, int mediumDescriptor, int sectors, int sectorSize) {
        this.fatType = fs.getFatType();
        this.nrSectors = sectors;
        this.sectorSize = sectorSize;
        this.dirty = false;
        this.fs = fs;
        entries = new long[(int) ((sectors * sectorSize) /
                this.fatType.getEntrySize())];
        entries[0] = (mediumDescriptor & 0xFF) | 0xFFFFF00L;
        
    }

    Fat(FatType fatType, int mediumDescriptor, int sectors, int sectorSize) {
        this.fatType = fatType;
        this.nrSectors = sectors;
        this.sectorSize = sectorSize;
        this.dirty = false;
        this.fs = null;
        
        entries = new long[(int) ((sectors * sectorSize) /
                this.fatType.getEntrySize())];
        entries[0] = (mediumDescriptor & 0xFF) | 0xFFFFF00L;
    }

    /**
     * Read the contents of this FAT from the given device at the given offset.
     * 
     * @param device the device to read the FAT from
     * @param offset the byte offset where to read the FAZ from the device
     * @throws IOException on read error
     */
    public synchronized void read(BlockDevice device, long offset)
            throws IOException {
        
        final byte[] data = new byte[nrSectors * sectorSize];
        device.read(offset, ByteBuffer.wrap(data));

        for (int i = 0; i < entries.length; i++)
            entries[i] = fatType.readEntry(data, i);

        this.dirty = false;
    }
    
    /**
     * Write the contents of this FAT to the given device at the given offset.
     * 
     * @param device the device to write the FAT to
     * @param offset the byte offset where to write the FAT on the device
     * @throws IOException on write error
     */
    public synchronized void write(BlockDevice device, long offset)
            throws IOException {

        final byte[] data = new byte[nrSectors * sectorSize];
        
        for (int index = 0; index < entries.length; index++) {
            fatType.writeEntry(data, index, entries[index]);
        }
        
        device.write(offset, ByteBuffer.wrap(data));
        this.dirty = false;
    }

    /**
     * Gets the medium descriptor byte
     * 
     * @return int
     */
    public int getMediumDescriptor() {
        return (int) (entries[0] & 0xFF);
    }

    /**
     * Sets the medium descriptor byte.
     * 
     * @param descr the new medium descriptor
     */
    public void setMediumDescriptor(int descr) {
        entries[0] = 0xFFFFFF00 | (descr & 0xFF);
    }

    /**
     * Gets the number of entries of this fat
     * 
     * @return int the number of FAT entries
     */
    public int getNrEntries() {
        return entries.length;
    }

    /**
     * Gets the entry at a given offset
     * 
     * @param index
     * @return long
     */
    public long getEntry(int index) {
        return entries[index];
    }

    /**
     * Returns the last free cluster that was accessed in this FAT.
     *
     * @return the last seen free cluster
     */
    public int getLastFreeCluster() {
        return this.lastFreeCluster;
    }
    
    public synchronized long[] getChain(long startCluster) {
        testCluster(startCluster);
        // Count the chain first
        int count = 1;
        long cluster = startCluster;
        while (!isEofCluster(entries[(int) cluster])) {
            count++;
            cluster = entries[(int) cluster];
        }
        // Now create the chain
        long[] chain = new long[count];
        chain[0] = startCluster;
        cluster = startCluster;
        int i = 0;
        while (!isEofCluster(entries[(int) cluster])) {
            cluster = entries[(int) cluster];
            chain[++i] = cluster;
        }
        return chain;
    }

    /**
     * Gets the cluster after the given cluster
     * 
     * @param cluster
     * @return long The next cluster number or -1 which means eof.
     */
    public synchronized long getNextCluster(long cluster) {
        testCluster(cluster);
        long entry = entries[(int) cluster];
        if (isEofCluster(entry)) {
            return -1;
        } else {
            return entry;
        }
    }

    /**
     * Allocate a cluster for a new file
     * 
     * @return long the number of the newly allocated cluster
     * @throws FileSystemFullException if there are no free clusters
     */
    public long allocNew() throws FileSystemFullException {

        int i;
        int entryIndex = -1;

        for (i = lastFreeCluster; i < entries.length; i++) {
            if (isFreeCluster(entries[i])) {
                entryIndex = i;
                break;
            }
        }
        
        if (entryIndex < 0) {
            for (i = 2; i < lastFreeCluster; i++) {
                if (isFreeCluster(entries[i])) {
                    entryIndex = i;
                    break;
                }
            }
        }

        if (entryIndex < 0) {
            throw new FileSystemFullException(fs,
                    "FAT Full (" + entries.length + ", " + i + ")"); //NOI18N
        }

        entries[entryIndex] = fatType.getEofMarker();
        lastFreeCluster = (entryIndex + 1) % entries.length;
        this.dirty = true;
        
        return entryIndex;

    }

    /**
     * Allocate a series of clusters for a new file.
     * 
     * @param nrClusters when number of clusters to allocate
     * @return long
     * @throws FileSystemException on write error
     */
    public synchronized long[] allocNew(int nrClusters) throws FileSystemException {

        long rc[] = new long[nrClusters];

        rc[0] = allocNew();
        for (int i = 1; i < nrClusters; i++) {
            rc[i] = allocAppend(rc[i - 1]);
        }

        return rc;
    }

    /**
     * Allocate a cluster to append to a new file
     * 
     * @param cluster a cluster from a chain where the new cluster should be
     *      appended
     * @return long the newly allocated and appended cluster number
     * @throws FileSystemFullException if there are no free clusters
     */
    public synchronized long allocAppend(long cluster)
            throws FileSystemFullException {
        
        testCluster(cluster);
        
        while (!isEofCluster(entries[(int) cluster])) {
            cluster = entries[(int) cluster];
        }
        
        long newCluster = allocNew();
        entries[(int) cluster] = newCluster;

        return newCluster;
    }

    public synchronized void setEof(long cluster) {
        testCluster(cluster);
        entries[(int) cluster] = fatType.getEofMarker();
    }

    public synchronized void setFree(long cluster) {
        testCluster(cluster);
        entries[(int) cluster] = 0;
    }

    /**
     * Print the contents of this FAT to the given writer. Used for debugging
     * purposes.
     *
     * @param out
     */
    public void printTo(PrintWriter out) {
        int freeCount = 0;
        out.println("medium descriptor 0x" + Integer.toHexString(getMediumDescriptor()));
        for (int i = 2; i < entries.length; i++) {
            long v = entries[i];
            if (isFreeCluster(v)) {
                freeCount++;
            } else {
                out.print("0x" + Integer.toHexString(i) + " -> ");
                if (isEofCluster(v)) {
                    out.println("eof");
                } else if (isReservedCluster(v)) {
                    out.println("reserved");
                } else {
                    out.println("0x" + Long.toHexString(v));
                }
            }
        }
        out.println("Nr free entries " + freeCount);
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final Fat other = (Fat) obj;
        if (!Arrays.equals(this.entries, other.entries)) return false;
        if (this.fatType != other.fatType) return false;
        if (this.nrSectors != other.nrSectors) return false;
        if (this.sectorSize != other.sectorSize) return false;
        
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 23 * hash + Arrays.hashCode(this.entries);
        hash = 23 * hash + this.fatType.hashCode();
        hash = 23 * hash + this.nrSectors;
        hash = 23 * hash + this.sectorSize;
        return hash;
    }
    
    /**
     * Is the given entry a free cluster?
     *
     * @param entry
     * @return boolean
     */
    protected boolean isFreeCluster(long entry) {
        return (entry == 0);
    }

    /**
     * Is the given entry a reserved cluster?
     *
     * @param entry
     * @return boolean
     */
    protected boolean isReservedCluster(long entry) {
        return fatType.isReservedCluster(entry);
    }

    /**
     * Is the given entry an EOF marker
     *
     * @param entry
     * @return boolean
     */
    protected boolean isEofCluster(long entry) {
        return fatType.isEofCluster(entry);
    }

    protected void testCluster(long cluster) throws IllegalArgumentException {
        if ((cluster < 2) || (cluster >= entries.length)) {
            throw new IllegalArgumentException("Invalid cluster value");
        }
    }

    /**
     * Returns the dirty.
     *
     * @return boolean
     */
    public boolean isDirty() {
        return dirty;
    }

}
