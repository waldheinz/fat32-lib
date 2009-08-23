/*
 * $Id: BlockDevice.java 4975 2009-02-02 08:30:52Z lsantha $
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
 
package org.jnode.driver.block;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * <description>
 *
 * @author Ewout Prangsma &lt; epr at jnode.org&gt;
 */
public interface BlockDevice {

    /**
     * Gets the total length in bytes
     *
     * @return long
     */
    public abstract long getLength();

    /**
     * Read a block of data
     *
     * @param devOffset
     * @param dest
     * @throws IOException
     */
    public abstract void read(long devOffset, ByteBuffer dest) throws IOException;

    /**
     * Write a block of data
     *
     * @param devOffset
     * @param src
     * @throws IOException
     */
    public abstract void write(long devOffset, ByteBuffer src) throws IOException;

    /**
     * flush data in caches to the block device
     *
     * @throws IOException
     */
    public abstract void flush() throws IOException;

    /**
     * Gets the sector size for this device.
     *
     * @return The sector size in bytes
     * @throws IOException
     */
    int getSectorSize() throws IOException;
}
