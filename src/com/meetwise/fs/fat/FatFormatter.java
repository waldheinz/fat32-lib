/*
 * $Id: FatFormatter.java 4975 2009-02-02 08:30:52Z lsantha $
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

/**
 * 
 *
 * @author Ewout Prangsma &lt; epr at jnode.org&gt;
 * @author Matthias Treydte
 */
public final class FatFormatter {

    /**
     * 
     */
    public static final int MAX_DIRECTORY = 512;

    /**
     * 
     */
    public static final int HD_DESC = 0xf8;

    /**
     * 
     */
    public static final int FLOPPY_DESC = 0xf0;


    private final BootSector bs;
    private final Fat fat;
    private final FatDirectory rootDir;

    /**
     * 
     *
     * @param d
     * @return
     * @throws IOException
     */
    public static FatFormatter superFloppyFormatter(BlockDevice d) throws IOException {
        final BootSector bs = new BootSector(SF_BS);
        final int totalSectors = (int)(d.getLength() / d.getSectorSize());
        final FatType fatSize = defaultFatSize(d);
        final int spc = defaultSectorsPerCluster(
                d.getSectorSize(), totalSectors);

        bs.setNrFats(2);
        bs.setSectorsPerTrack(32);
        bs.setBytesPerSector(d.getSectorSize());
        bs.setMediumDescriptor(HD_DESC);
        bs.setNrHeads(64);
        bs.setOemName("fat32lib");
        bs.setSectorCount(totalSectors);
        bs.setSectorsPerCluster(spc);
        
        if (fatSize == FatType.FAT32) {
            bs.setSectorsPerFat32(1009);
            bs.setNrReservedSectors(32);
            
            final FsInfoSector fsInfo = new FsInfoSector(bs, d);
            fsInfo.init();
        } else {
            bs.setNrReservedSectors(1);
            bs.setSectorsPerFat((Math.round(totalSectors / (spc *
                (d.getSectorSize() / fatSize.getEntrySize()))) + 1));
            bs.setNrRootDirEntries(calculateDefaultRootDirectorySize(
                    d.getSectorSize(), totalSectors));
        }
        
        return new FatFormatter(FatType.FAT32, bs);
    }
    
    private static FatType defaultFatSize(BlockDevice d) {
        final long len = d.getLength();

        if (len < 1024 * 1024 * 1024) return FatType.FAT16;
        else return FatType.FAT32;
    }
    
    private FatFormatter(FatType fatSize, BootSector bs) throws IOException {
        this.bs = bs;
        fat = new Fat(fatSize, bs.getMediumDescriptor(),
                bs.getSectorsPerFat(), bs.getBytesPerSector());
        
        rootDir = new FatLfnDirectory(null, bs.getNrRootDirEntries());
    }

    private static int defaultSectorsPerCluster(int bps, int sectors) {
        // Apply the default cluster size from MS
        final long sizeInMB = (sectors * bps) / (1024 * 1024);
        int spc;

        if (sizeInMB < 32) {
            spc = 1;
        } else if (sizeInMB < 64) {
            spc = 2;
        } else if (sizeInMB < 128) {
            spc = 4;
        } else if (sizeInMB < 256) {
            spc = 8;
        } else if (sizeInMB < 1024) {
            spc = 32;
        } else if (sizeInMB < 2048) {
            spc = 64;
        } else if (sizeInMB < 4096) {
            spc = 128;
        } else if (sizeInMB < 8192) {
            spc = 256;
        } else if (sizeInMB < 16384) {
            spc = 512;
        } else
            throw new IllegalArgumentException(
                    "Disk too large to be formatted in FAT16");
        
        return spc;
    }

    private static int calculateDefaultRootDirectorySize(int bps, int nbTotalSectors) {
        int totalSize = bps * nbTotalSectors;
        // take a default 1/5 of the size for root max
        if (totalSize >= MAX_DIRECTORY * 5 * 32) { // ok take the max
            return MAX_DIRECTORY;
        } else {
            return totalSize / (5 * 32);
        }
    }
    
    /**
     * Format the given device, according to my settings
     * 
     * @param api
     * @param label 
     * @throws IOException
     */
    public void format(BlockDevice api, String label) throws IOException {
        bs.write(api);
        
        for (int i = 0; i < bs.getNrFats(); i++) {
            fat.write(api, FatUtils.getFatOffset(bs, i));
        }

        rootDir.write(api, FatUtils.getRootDirOffset(bs));
        api.flush();

        if (label != null) {
            FatFileSystem fs = new FatFileSystem(api, false);
            fs.setVolumeLabel(label);
            fs.flush();
            api.flush();
        }
    }
    
    /**
     * Returns the bs.
     *
     * @return BootSector
     */
    public BootSector getBootSector() {
        return bs;
    }

    public final static byte[] SF_BS = {
        (byte) 0xeb, (byte) 0x58, (byte) 0x90, (byte) 0x6d, (byte) 0x6b,
        (byte) 0x64, (byte) 0x6f, (byte) 0x73, (byte) 0x66, (byte) 0x73,
        (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x20,
        (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0xf8, (byte) 0x00, (byte) 0x00, (byte) 0x20,
        (byte) 0x00, (byte) 0x40, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x38, (byte) 0x01,
        (byte) 0x00, (byte) 0x68, (byte) 0x02, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x29, (byte) 0xcd, (byte) 0xa3, (byte) 0x9b,
        (byte) 0x5b, (byte) 0x66, (byte) 0x61, (byte) 0x74, (byte) 0x33,
        (byte) 0x32, (byte) 0x6c, (byte) 0x69, (byte) 0x62, (byte) 0x20,
        (byte) 0x20, (byte) 0x20, (byte) 0x46, (byte) 0x41, (byte) 0x54,
        (byte) 0x33, (byte) 0x32, (byte) 0x20, (byte) 0x20, (byte) 0x20,
        (byte) 0x0e, (byte) 0x1f, (byte) 0xbe, (byte) 0x77, (byte) 0x7c,
        (byte) 0xac, (byte) 0x22, (byte) 0xc0, (byte) 0x74, (byte) 0x0b,
        (byte) 0x56, (byte) 0xb4, (byte) 0x0e, (byte) 0xbb, (byte) 0x07,
        (byte) 0x00, (byte) 0xcd, (byte) 0x10, (byte) 0x5e, (byte) 0xeb,
        (byte) 0xf0, (byte) 0x32, (byte) 0xe4, (byte) 0xcd, (byte) 0x16,
        (byte) 0xcd, (byte) 0x19, (byte) 0xeb, (byte) 0xfe, (byte) 0x54,
        (byte) 0x68, (byte) 0x69, (byte) 0x73, (byte) 0x20, (byte) 0x69,
        (byte) 0x73, (byte) 0x20, (byte) 0x6e, (byte) 0x6f, (byte) 0x74,
        (byte) 0x20, (byte) 0x61, (byte) 0x20, (byte) 0x62, (byte) 0x6f,
        (byte) 0x6f, (byte) 0x74, (byte) 0x61, (byte) 0x62, (byte) 0x6c,
        (byte) 0x65, (byte) 0x20, (byte) 0x64, (byte) 0x69, (byte) 0x73,
        (byte) 0x6b, (byte) 0x2e, (byte) 0x20, (byte) 0x20, (byte) 0x50,
        (byte) 0x6c, (byte) 0x65, (byte) 0x61, (byte) 0x73, (byte) 0x65,
        (byte) 0x20, (byte) 0x69, (byte) 0x6e, (byte) 0x73, (byte) 0x65,
        (byte) 0x72, (byte) 0x74, (byte) 0x20, (byte) 0x61, (byte) 0x20,
        (byte) 0x62, (byte) 0x6f, (byte) 0x6f, (byte) 0x74, (byte) 0x61,
        (byte) 0x62, (byte) 0x6c, (byte) 0x65, (byte) 0x20, (byte) 0x66,
        (byte) 0x6c, (byte) 0x6f, (byte) 0x70, (byte) 0x70, (byte) 0x79,
        (byte) 0x20, (byte) 0x61, (byte) 0x6e, (byte) 0x64, (byte) 0x0d,
        (byte) 0x0a, (byte) 0x70, (byte) 0x72, (byte) 0x65, (byte) 0x73,
        (byte) 0x73, (byte) 0x20, (byte) 0x61, (byte) 0x6e, (byte) 0x79,
        (byte) 0x20, (byte) 0x6b, (byte) 0x65, (byte) 0x79, (byte) 0x20,
        (byte) 0x74, (byte) 0x6f, (byte) 0x20, (byte) 0x74, (byte) 0x72,
        (byte) 0x79, (byte) 0x20, (byte) 0x61, (byte) 0x67, (byte) 0x61,
        (byte) 0x69, (byte) 0x6e, (byte) 0x20, (byte) 0x2e, (byte) 0x2e,
        (byte) 0x2e, (byte) 0x20, (byte) 0x0d, (byte) 0x0a, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x55, (byte) 0xaa
    };

}
