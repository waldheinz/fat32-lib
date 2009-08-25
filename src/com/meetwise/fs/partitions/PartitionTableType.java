/*
 * $Id: PartitionTableType.java 4975 2009-02-02 08:30:52Z lsantha $
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
 
package com.meetwise.fs.partitions;

import com.meetwise.fs.BlockDevice;

/**
 * @author Ewout Prangsma (epr@users.sourceforge.net)
 */
public interface PartitionTableType {

    /**
     * Gets the unique name of this partition table type.
     * 
     * @return this type's name
     */
    public String getName();

    /**
     * Can this partition table type be used on the given first sector of a
     * blockdevice?
     * 
     * @param firstSector the first sector
     * @param devApi the device
     * @return if this type is supported
     */
    public boolean supports(byte[] firstSector, BlockDevice devApi);

    /**
     * Create a partition table for a given device.
     * 
     * @param firstSector
     * @param device
     * @return the new partition table
     * @throws PartitionTableException
     */
    public PartitionTable<?> create(byte[] firstSector, BlockDevice device) throws PartitionTableException;

}
