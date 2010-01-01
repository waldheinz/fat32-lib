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
 
package com.meetwise.fs.fat;

import com.meetwise.fs.BlockDevice;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import com.meetwise.fs.FSDirectoryEntry;
import com.meetwise.fs.FileSystemException;

/**
 * @author Ewout Prangsma &lt; epr at jnode.org&gt;
 */
class FatDirectory extends AbstractDirectory {

    private final boolean root;

    protected FatDirEntry labelEntry;

    /**
     * Constructor for Directory.
     * 
     * @param fs
     * @param file
     * @throws FileSystemException 
     */
    public FatDirectory(FatFileSystem fs, FatFile file) throws FileSystemException{
        super(fs, file);

        this.root = 
                (fs.getFatType() == FatType.FAT32) ? 
                    (file.getStartCluster() == 
                    ((Fat32BootSector)fs.getBootSector()).getRootDirFirstCluster()) ?
                        true : false : false;
    }

    // for root
    protected FatDirectory(FatFileSystem fs, int nrEntries) {
        super(fs, nrEntries, null);
        
        root = true;
    }

    String getLabel() {
        if (labelEntry != null) return labelEntry.getName();
        else return null;
    }
    
    /**
     * Read the contents of this directory from the persistent storage at the
     * given offset.
     * 
     * @throws FileSystemException
     */
    protected void read() throws FileSystemException {
        entries.setSize((int) file.getLengthOnDisk() / 32);

        // TODO optimize it also to use ByteBuffer at lower level
        // final byte[] data = new byte[entries.size() * 32];
        final ByteBuffer data = ByteBuffer.allocate(entries.size() * 32);
        
        try {
            file.readData(0, data);
        } catch (IOException ex) {
            throw new FileSystemException(getFileSystem(), ex);
        }

        read(data.array());
        resetDirty();
    }

    @Override
    protected void read(byte[] src) {
        super.read(src);
        findLabelEntry();
    }
    
    private void findLabelEntry() {
        for (int i=0; i < entries.size(); i++) {
            if (entries.get(i) instanceof FatDirEntry) {
                FatDirEntry e = (FatDirEntry) entries.get(i);
                if (e.isLabel()) {
                    labelEntry = e;
                    entries.set(i, null);
                    break;
                }
            }
        }
    }

    /**
     * Write the contents of this directory to the given persistent storage at
     * the given offset.
     *
     * @throws FileSystemException
     */
    protected void write() throws FileSystemException {
        // TODO optimize it also to use ByteBuffer at lower level
        // final byte[] data = new byte[entries.size() * 32];
        final ByteBuffer data = ByteBuffer.allocate(entries.size() * 32);

        if (canChangeSize(entries.size())) {
            try {
                file.setSize(data.capacity());
            } catch (IOException ex) {
                throw new FileSystemException(getFileSystem(), ex);
            }
        }
        
        write(data.array());

        try {
            file.writeData(0, data);
        } catch (IOException ex) {
            throw new FileSystemException(getFileSystem(), ex);
        }

        resetDirty();
    }

    public void read(BlockDevice device, long offset) throws FileSystemException {
        ByteBuffer data = ByteBuffer.allocate(entries.size() * 32);

        try {
            device.read(offset, data);
        } catch (IOException ex) {
            throw new FileSystemException(this.getFileSystem(), ex);
        }
        
        read(data.array());
        resetDirty();
    }

    public void write(BlockDevice device, long offset)
            throws FileSystemException {
        
        final ByteBuffer data = ByteBuffer.allocate(entries.size() * 32);
        write(data.array());
        try {
            device.write(offset, data);
        } catch (IOException ex) {
            throw new FileSystemException(this.getFileSystem(), ex);
        }
        resetDirty();
    }

    /**
     * Flush the contents of this directory to the persistent storage
     * 
     * @throws FileSystemException 
     */
    @Override
    public void flush() throws FileSystemException {
        if (file == null) {
            final FatFileSystem fs = getFileSystem();
            long offset;
            
            try {
                offset = FatUtils.getRootDirOffset(fs.getBootSector());
            } catch (IOException ex) {
                throw new FileSystemException(fs, ex);
            }

            write(fs.getBlockDevice(), offset);
        } else {
            write();
        }
    }
    
    void setLabel(String label) throws IOException {
        if (!root) {
            throw new IOException(
                    "volume name change on non-root directory"); //NOI18N
        }

        if (label != null) {
            Iterator<FSDirectoryEntry> i = iterator();
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
        } else {
            labelEntry = null;
        }
    }
}
