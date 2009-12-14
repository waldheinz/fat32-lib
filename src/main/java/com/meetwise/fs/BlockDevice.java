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
 
package com.meetwise.fs;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * This is the abstraction used for a device that can hold a {@link FileSystem}.
 *
 * @author Ewout Prangsma &lt;epr at jnode.org&gt;
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public interface BlockDevice {

    /**
     * Gets the total length of this device in bytes.
     *
     * @return the total number of bytes on this device
     * @throws IOException on error getting the size of this device
     */
    public abstract long getSize() throws IOException;

    /**
     * Read a block of data from this device.
     *
     * @param devOffset the byte offset where to read the data from
     * @param dest the destination buffer where to store the data read
     * @throws IOException on read error
     */
    public abstract void read(long devOffset, ByteBuffer dest)
            throws IOException;

    /**
     * Writes a block of data to this device.
     *
     * @param devOffset the byte offset where to store the data
     * @param src the source {@code ByteBuffer} to write to the device
     * @throws ReadOnlyDeviceException if this {@code BlockDevice} is read-only
     * @throws IOException on write error
     * @see #isReadOnly() 
     */
    public abstract void write(long devOffset, ByteBuffer src)
            throws ReadOnlyDeviceException, IOException;

    /**
     * Flushes data in caches to the actual storage.
     *
     * @throws IOException on write error
     */
    public abstract void flush() throws IOException;

    /**
     * Returns the size of a sector on this device.
     *
     * @return the sector size in bytes
     * @throws IOException on error determining the sector size
     */
    public int getSectorSize() throws IOException;

    /**
     * Closes this {@code BlockDevice}. No methods of this device may be
     * accesses after this method was called.
     *
     * @throws IOException on error closing this device
     * @see #isClosed() 
     */
    public void close() throws IOException;

    /**
     * Checks if this device was already closed. No methods may be called
     * on a closed device (except this method).
     *
     * @return if this device is closed
     */
    public boolean isClosed();

    /**
     * Checks if this {@code BlockDevice} is read-only.
     *
     * @return if this {@code BlockDevice} is read-only
     */
    public boolean isReadOnly();
    
}
