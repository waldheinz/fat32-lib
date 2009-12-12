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

import com.meetwise.fs.BootSector;
import com.meetwise.fs.AbstractFileSystem;
import com.meetwise.fs.BlockDevice;
import java.io.IOException;
import java.util.HashMap;
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
    private final FsInfoSector fsInfo;
    private final BootSector bs;
    private final FatLfnDirectory rootDir;
    private final HashMap<FatDirEntry, FatFile> files =
            new HashMap<FatDirEntry, FatFile>();
    private final FatType fatType;

    public FatFileSystem(BlockDevice api, boolean readOnly)
            throws FileSystemException {

        this(api, readOnly, false);
    }
    
    /**
     * Constructor for FatFileSystem in specified readOnly mode
     * 
     * @param api the BlockDevice holding the file system
     * @param readOnly if this FS should be read-lonly
     * @param ignoreFatDifferences 
     * @throws FileSystemException on error
     */
    public FatFileSystem(BlockDevice api, boolean readOnly, boolean ignoreFatDifferences)
            throws FileSystemException {
        
        super(api, readOnly);

        try {
            bs = new BootSector(512);
            bs.read(getApi());

            //this.fsInfo = new FsInfoSector(bs, getApi());
            this.fsInfo = null;
            
            Fat[] fats = new Fat[bs.getNrFats()];
            fatType = bs.getFatType();
            
            for (int i = 0; i < fats.length; i++) {
                Fat tmpFat = new Fat(
                        this, bs.getMediumDescriptor(), bs.getSectorsPerFat(),
                        bs.getBytesPerSector());
                fats[i] = tmpFat;

                try {
                    tmpFat.read(getApi(), FatUtils.getFatOffset(bs, i));
                } catch (IOException ex) {
                    throw new FileSystemException(this, ex);
                }
            }


            if (!ignoreFatDifferences) for (int i = 1; i < fats.length; i++) {
                if (!fats[0].equals(fats[i])) {
                        throw new FileSystemException(this,
                            "FAT " + i + " differs from FAT 0");
                }
            }
            
            fat = fats[0];
            if (fatType == FatType.FAT32) {
                FatFile rootDirFile = new FatFile(this,
                        bs.getRootDirFirstCluster());
                rootDir = new FatLfnDirectory(this, rootDirFile);
            } else {
                rootDir = new FatLfnDirectory(this, bs.getNrRootDirEntries());
                rootDir.read(getApi(), FatUtils.getRootDirOffset(bs));
            }
            
        } catch (IOException ex) {
            throw new FileSystemException(this, ex);
        }
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

        final BlockDevice api = getApi();

        if (bs.isDirty()) {
            bs.write(api);
        }

        if (fsInfo != null && fsInfo.isDirty()) fsInfo.write();
        
        for (FatFile f : files.values()) {
            f.flush();
        }

        if (fat.isDirty()) {
            for (int i = 0; i < bs.getNrFats(); i++) {
                fat.write(api, FatUtils.getFatOffset(bs, i));
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
    
    /**
     * Gets the file for the given entry.
     * 
     * @param entry
     * @return 
     */
    FatFile getFile(FatDirEntry entry) {
        FatFile file = files.get(entry);
        
        if (file == null) {
            file = new FatFile(this, entry, entry.getStartCluster(),
                    entry.getLength(), entry.isDirectory());
            files.put(entry, file);
        }
        
        return file;
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
    
    public long getFreeSpace() {
        // TODO implement me
        return -1;
    }

    public long getTotalSpace() {
        // TODO implement me
        return -1;
    }

    public long getUsableSpace() {
        // TODO implement me
        return -1;
    }
}
