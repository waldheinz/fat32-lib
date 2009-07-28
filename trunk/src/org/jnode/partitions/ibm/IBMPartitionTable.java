/*
 * $Id: IBMPartitionTable.java 4975 2009-02-02 08:30:52Z lsantha $
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
 
package org.jnode.partitions.ibm;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import java.util.logging.Logger;
import org.jnode.driver.Device;
import org.jnode.driver.block.BlockDeviceAPI;
import org.jnode.partitions.PartitionTable;
import org.jnode.partitions.PartitionTableType;

/**
 * @author epr
 */
public class IBMPartitionTable implements PartitionTable<IBMPartitionTableEntry> {
    private static final int TABLE_SIZE = 4;

    /** The type of partition table */
    private final IBMPartitionTableType tableType;
    
    /** The partition entries */
    private final IBMPartitionTableEntry[] partitions;

    /** The device */
    private final Device drivedDevice;

    /** Extended partition */
    private final ArrayList<IBMPartitionTableEntry> extendedPartitions =
            new ArrayList<IBMPartitionTableEntry>();

    /** My logger */
    private static final Logger log = Logger.getLogger(IBMPartitionTable.class.getName());

    /** The position of the extendedPartition in the table */
    private int extendedPartitionEntry = -1;
    private final static int SECTOR_SIZE = 512;

    /**
     * Create a new instance
     * 
     * @param bootSector
     */
    public IBMPartitionTable(IBMPartitionTableType tableType, byte[] bootSector, Device device) {
        // this.bootSector = bootSector;
        this.tableType = tableType;
        this.drivedDevice = device;
        if (containsPartitionTable(bootSector)) {
            this.partitions = new IBMPartitionTableEntry[TABLE_SIZE];
            for (int partNr = 0; partNr < partitions.length; partNr++) {
                log.finer("try part " + partNr);
                partitions[partNr] = new IBMPartitionTableEntry(this, bootSector, partNr);
                if (partitions[partNr].isExtended()) {
                    extendedPartitionEntry = partNr;
                    log.finer("Found Extended partitions");
                    handleExtended(partitions[partNr]);
                }
            }
        } else {
            partitions = null;
        }
    }

    /**
     * Fill the extended Table
     */
    private void handleExtended(IBMPartitionTableEntry current) {

        final long startLBA = current.getStartLba();
        final ByteBuffer sector = ByteBuffer.allocate(SECTOR_SIZE);
        try {
            log.finer("Try to read the Extended Partition Table");
            BlockDeviceAPI api = drivedDevice.getAPI(BlockDeviceAPI.class);
            api.read(startLBA * SECTOR_SIZE, sector);
        } catch (IOException e) {
            // I think we ca'nt get it
            log.severe("IOException");
        }

        IBMPartitionTableEntry entry = null;
        for (int i = 0; i < TABLE_SIZE; i++) {
            entry = new IBMPartitionTableEntry(this, sector.array(), i);
            if (entry.isValid() && !entry.isEmpty()) {
                // correct the offset
                if (entry.isExtended()) {
                    entry.setStartLba(entry.getStartLba() +
                            partitions[extendedPartitionEntry].getStartLba());
                    handleExtended(entry);
                } else {
                    entry.setStartLba(entry.getStartLba() + current.getStartLba());
                    extendedPartitions.add(entry);
                }
            } 
        }
    }

    public boolean hasExtended() {
        return !extendedPartitions.isEmpty();
    }

    /**
     * Does the given bootsector contain an IBM partition table?
     * 
     * @param bootSector
     */
    public static boolean containsPartitionTable(byte[] bootSector) {
        if ((bootSector[510] & 0xFF) != 0x55) {
            return false;
        }
        if ((bootSector[511] & 0xFF) != 0xAA) {
            return false;
        }
        return true;
    }

    public Iterator<IBMPartitionTableEntry> iterator() {
        return new Iterator<IBMPartitionTableEntry>() {
            private int index = 0;
            private final int last = (partitions == null) ? 0 : partitions.length - 1;

            public boolean hasNext() {
                return index < last;
            }

            public IBMPartitionTableEntry next() {
                return partitions[index++];
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * @return Returns the extendedPartitions.
     */
    public List<IBMPartitionTableEntry> getExtendedPartitions() {
        return extendedPartitions;
    }

    /**
     * @see org.jnode.partitions.PartitionTable#getType()
     */
    public PartitionTableType getType() {
        return tableType;
    }
}
