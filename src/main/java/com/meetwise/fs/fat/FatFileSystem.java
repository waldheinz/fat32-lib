/*
 * $Id: FatFileSystem.java 4975 2009-02-02 08:30:52Z lsantha $
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

import com.meetwise.fs.AbstractFileSystem;
import com.meetwise.fs.BlockDevice;
import java.io.IOException;
import com.meetwise.fs.FSDirectory;
import com.meetwise.fs.FileSystemException;

/**
 * 
 *
 * @author Ewout Prangsma &lt;epr at jnode.org&gt;
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public final class FatFileSystem extends AbstractFileSystem {
    
    private final Fat fat;
    private final BootSector bs;
    private final FatLfnDirectory rootDir;
    private final FatType fatType;
    private final long filesOffset;

    public FatFileSystem(BlockDevice api, boolean readOnly)
            throws IOException {

        this(api, readOnly, false);
    }
    
    /**
     * Constructor for FatFileSystem in specified readOnly mode
     * 
     * @param api the BlockDevice holding the file system
     * @param readOnly if this FS should be read-lonly
     * @param ignoreFatDifferences
     * @throws IOException on read error
     */
    public FatFileSystem(BlockDevice api, boolean readOnly,
            boolean ignoreFatDifferences)
            throws IOException {
        
        super(api, readOnly);
        
        this.bs = BootSector.read(api);
        
        if (bs.getNrFats() <= 0) throw new IOException(
                "boot sector says there are no FATs");
        
        this.filesOffset = FatUtils.getFilesOffset(bs);
        this.fatType = bs.getFatType();
        this.fat = Fat.read(bs, 0);

        if (!ignoreFatDifferences) {
            for (int i=1; i < bs.getNrFats(); i++) {
                final Fat tmpFat = Fat.read(bs, i);
                if (!fat.equals(tmpFat)) {
                    throw new FileSystemException(this,
                            "FAT " + i + " differs from FAT 0");
                }
            }
        }

        if (fatType == FatType.FAT32) {
            final Fat32BootSector f32bs = (Fat32BootSector) bs;
            ClusterChain rootDirFile = new ClusterChain(fat, getClusterSize(),
                    getFilesOffset(), f32bs.getRootDirFirstCluster(), isReadOnly());
            rootDir = new FatLfnDirectory(getFilesOffset(), rootDirFile, true);
        } else {
            final Fat16BootSector f16bs = (Fat16BootSector) bs;
            rootDir = new FatLfnDirectory(getFilesOffset(), fat,
                    api, FatUtils.getFatOffset(bs, 0),
                    f16bs.getRootDirEntryCount(),
                    bs.getBytesPerSector() * bs.getSectorsPerCluster(),
                    isReadOnly());
        }
            
    }

    public long getFilesOffset() {
        return filesOffset;
    }
    
    public FatType getFatType() {
        return this.fatType;
    }

    /**
     * Returns the volume label of this file system.
     *
     * @return the volume label
     */
    public String getVolumeLabel() {
        return rootDir.getLabel();
    }

    /**
     * Sets the volume label for this file system.
     *
     * @param label the new volume label, may be {@code null}
     * @throws IOException on write error
     */
    public void setVolumeLabel(String label) throws IOException {
        rootDir.setLabel(label);
    }

    /**
     * Flush all changed structures to the device.
     * 
     * @throws IOException
     */
    @Override
    public void flush() throws IOException {
        if (bs.isDirty()) {
            bs.write();
        }
        
        if (fat.isDirty()) {
            for (int i = 0; i < bs.getNrFats(); i++) {
                fat.writeCopy(FatUtils.getFatOffset(bs, i));
            }
        }
        
        if (rootDir.isDirty()) {
            rootDir.flush();
        }
    }
    
    @Override
    public FSDirectory getRoot() {
        return rootDir;
    }
    
    public int getClusterSize() {
        return bs.getBytesPerSector() * bs.getSectorsPerCluster();
    }

    /**
     * Returns the fat.
     * 
     * @return Fat
     */
    Fat getFat() {
        return fat;
    }

    /**
     * Returns the bootsector.
     * 
     * @return BootSector
     */
    public BootSector getBootSector() {
        return bs;
    }
    
    @Override
    public long getFreeSpace() {
        // TODO implement me
        return -1;
    }

    @Override
    public long getTotalSpace() {
        // TODO implement me
        return -1;
    }

    @Override
    public long getUsableSpace() {
        // TODO implement me
        return -1;
    }
}
