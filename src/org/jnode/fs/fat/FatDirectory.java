/*
 * $Id: FatDirectory.java 4975 2009-02-02 08:30:52Z lsantha $
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
import java.nio.ByteBuffer;
import java.util.Iterator;

import org.jnode.driver.block.BlockDevice;
import org.jnode.fs.FSEntry;

/**
 * @author epr
 */
public class FatDirectory extends AbstractDirectory {

    private boolean root = false;
    private String label;

    /**
     * Constructor for Directory.
     * 
     * @param fs
     * @param file
     * @throws IOException on read error
     */
    public FatDirectory(FatFileSystem fs, FatFile file) throws IOException {
        super(fs, file);
        
        this.file = file;
    }

    // for root
    protected FatDirectory(FatFileSystem fs, int nrEntries) {
        super(fs, nrEntries, null);
        
        root = true;
    }

    public boolean isRoot() {
        return this.root;
    }

    /**
     * Read the contents of this directory from the persistent storage at the
     * given offset.
     * 
     * @throws IOException on read error
     */
    protected void read() throws IOException {
        entries.setSize((int) file.getLengthOnDisk() / 32);

        // TODO optimize it also to use ByteBuffer at lower level
        // final byte[] data = new byte[entries.size() * 32];
        final ByteBuffer data = ByteBuffer.allocate(entries.size() * 32);
        file.read(0, data);
        read(data.array());

        resetDirty();
    }

    /**
     * Write the contents of this directory to the given persistent storage at
     * the given offset.
     *
     * @throws IOException on write error
     */
    protected synchronized void write() throws IOException {
        if (label != null)
            applyLabel();
        // TODO optimize it also to use ByteBuffer at lower level
        // final byte[] data = new byte[entries.size() * 32];
        final ByteBuffer data = ByteBuffer.allocate(entries.size() * 32);

        if (canChangeSize(entries.size())) {
            file.setLength(data.capacity());
        }
        
        write(data.array());
        // file.write(0, data, 0, data.length);
        file.write(0, data);
        resetDirty();
    }

    public synchronized void read(BlockDevice device, long offset) throws IOException {
        ByteBuffer data = ByteBuffer.allocate(entries.size() * 32);
        device.read(offset, data);
        read(data.array());
        resetDirty();
    }

    public synchronized void write(BlockDevice device, long offset) throws IOException {
        if (label != null)
            applyLabel();
        final ByteBuffer data = ByteBuffer.allocate(entries.size() * 32);
        write(data.array());
        device.write(offset, data);
        resetDirty();
    }

    /**
     * Flush the contents of this directory to the persistent storage
     */
    public void flush() throws IOException {
        if (root) {
            final FatFileSystem fs = (FatFileSystem) getFileSystem();
            if (fs != null) {
                long offset = FatUtils.getRootDirOffset(fs.getBootSector());
                write(fs.getApi(), offset);
            }
        } else {
            write();
        }
    }

    /**
     * @see org.jnode.fs.fat.AbstractDirectory#canChangeSize(int)
     */
    protected boolean canChangeSize(int newSize) {
        return !root;
    }

    /**
     * Set the label
     * 
     * @param label
     * @throws IOException on write error
     */
    public void setLabel(String label) throws IOException {
        if (!root) {
            throw new IOException(
                    "volume name change on non-root directory"); //NOI18N
        }

        this.label = label;
    }

    private void applyLabel() throws IOException {
        FatDirEntry labelEntry = null;
        Iterator<FSEntry> i = iterator();
        FatDirEntry current;
        while (labelEntry == null && i.hasNext()) {
            current = (FatDirEntry) i.next();
            if (current.isLabel() &&
                    !(current.isHidden() && current.isReadonly() && current.isSystem())) {
                labelEntry = current;
            }
        }
        if (labelEntry == null) {
            labelEntry = addFatFile(label);
            labelEntry.setLabel();
        }
        labelEntry.setName(label);
        if (label.length() > 8) {
            labelEntry.setExt(label.substring(8));
        } else {
            labelEntry.setExt("");
        }
    }
}
