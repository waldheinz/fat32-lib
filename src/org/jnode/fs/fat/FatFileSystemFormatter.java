/*
 * $Id$
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

import org.jnode.driver.block.BlockDeviceAPI;
import org.jnode.fs.FileSystemException;


/**
 * @author epr
 */
public class FatFileSystemFormatter {
    private static final int NB_HEADS = 255;
    private static final int SECTOR_PER_TRACK = 63;

    private FatType fatSize;

    public FatFileSystemFormatter(FatType fatSize) {
        this.fatSize = fatSize;
    }

    /**
     * 
     * @param api 
     * @return
     * @throws FileSystemException
     */
    public FatFileSystem format(BlockDeviceAPI api) throws FileSystemException {
        try {
            long numberOfSectors;
            long offset;
            
            int sectorSize = api.getSectorSize();
            
            numberOfSectors = api.getLength() / sectorSize;
            offset = 0;

            FatFormatter ff =
                    FatFormatter.HDFormatter(sectorSize, (int) numberOfSectors, SECTOR_PER_TRACK,
                            NB_HEADS, fatSize, (int) offset, 1, FAT_STANDARD_BS);
                            
            ff.format(api, null);

            return new FatFileSystem(api, false); // not readOnly !
        } catch (IOException ioe) {
            throw new FileSystemException("Formating problem", ioe);
        }
    }

    public static final byte[] BOOT_BYTES = new byte[] {(byte) 0xEB, (byte) 0x48, (byte) 0x90, (byte) 0x4A,
                (byte) 0x4E, (byte) 0x6F, (byte) 0x64, (byte) 0x65, (byte) 0x31, (byte) 0x2E,
                (byte) 0x30, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0xED, (byte) 0x00,
                (byte) 0x02, (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x80, (byte) 0xF8,
                (byte) 0x81, (byte) 0x00, (byte) 0x20, (byte) 0x00, (byte) 0x40, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x03, (byte) 0x02,
                (byte) 0xFF, (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x01, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x08, (byte) 0xFA, (byte) 0xEA,
                (byte) 0x50, (byte) 0x7C, (byte) 0x00, (byte) 0x00, (byte) 0x31, (byte) 0xC0,
                (byte) 0x8E, (byte) 0xD8, (byte) 0x8E, (byte) 0xD0, (byte) 0xBC, (byte) 0x00,
                (byte) 0x20, (byte) 0xFB, (byte) 0xA0, (byte) 0x40, (byte) 0x7C, (byte) 0x3C,
                (byte) 0xFF, (byte) 0x74, (byte) 0x02, (byte) 0x88, (byte) 0xC2, (byte) 0x52,
                (byte) 0xBE, (byte) 0x76, (byte) 0x7D, (byte) 0xE8, (byte) 0x34, (byte) 0x01,
                (byte) 0xF6, (byte) 0xC2, (byte) 0x80, (byte) 0x74, (byte) 0x54, (byte) 0xB4,
                (byte) 0x41, (byte) 0xBB, (byte) 0xAA, (byte) 0x55, (byte) 0xCD, (byte) 0x13,
                (byte) 0x5A, (byte) 0x52, (byte) 0x72, (byte) 0x49, (byte) 0x81, (byte) 0xFB,
                (byte) 0x55, (byte) 0xAA, (byte) 0x75, (byte) 0x43, (byte) 0xA0, (byte) 0x41,
                (byte) 0x7C, (byte) 0x84, (byte) 0xC0, (byte) 0x75, (byte) 0x05, (byte) 0x83,
                (byte) 0xE1, (byte) 0x01, (byte) 0x74, (byte) 0x37, (byte) 0x66, (byte) 0x8B,
                (byte) 0x4C, (byte) 0x10, (byte) 0xBE, (byte) 0x05, (byte) 0x7C, (byte) 0xC6,
                (byte) 0x44, (byte) 0xFF, (byte) 0x01, (byte) 0x66, (byte) 0x8B, (byte) 0x1E,
                (byte) 0x44, (byte) 0x7C, (byte) 0xC7, (byte) 0x04, (byte) 0x10, (byte) 0x00,
                (byte) 0xC7, (byte) 0x44, (byte) 0x02, (byte) 0x01, (byte) 0x00, (byte) 0x66,
                (byte) 0x89, (byte) 0x5C, (byte) 0x08, (byte) 0xC7, (byte) 0x44, (byte) 0x06,
                (byte) 0x00, (byte) 0x70, (byte) 0x66, (byte) 0x31, (byte) 0xC0, (byte) 0x89,
                (byte) 0x44, (byte) 0x04, (byte) 0x66, (byte) 0x89, (byte) 0x44, (byte) 0x0C,
                (byte) 0xB4, (byte) 0x42, (byte) 0xCD, (byte) 0x13, (byte) 0x72, (byte) 0x05,
                (byte) 0xBB, (byte) 0x00, (byte) 0x70, (byte) 0xEB, (byte) 0x7D, (byte) 0xB4,
                (byte) 0x08, (byte) 0xCD, (byte) 0x13, (byte) 0x73, (byte) 0x0A, (byte) 0xF6,
                (byte) 0xC2, (byte) 0x80, (byte) 0x0F, (byte) 0x84, (byte) 0xF3, (byte) 0x00,
                (byte) 0xE9, (byte) 0x8D, (byte) 0x00, (byte) 0xBE, (byte) 0x05, (byte) 0x7C,
                (byte) 0xC6, (byte) 0x44, (byte) 0xFF, (byte) 0x00, (byte) 0x66, (byte) 0x31,
                (byte) 0xC0, (byte) 0x88, (byte) 0xF0, (byte) 0x40, (byte) 0x66, (byte) 0x89,
                (byte) 0x44, (byte) 0x04, (byte) 0x31, (byte) 0xD2, (byte) 0x88, (byte) 0xCA,
                (byte) 0xC1, (byte) 0xE2, (byte) 0x02, (byte) 0x88, (byte) 0xE8, (byte) 0x88,
                (byte) 0xF4, (byte) 0x40, (byte) 0x89, (byte) 0x44, (byte) 0x08, (byte) 0x31,
                (byte) 0xC0, (byte) 0x88, (byte) 0xD0, (byte) 0xC0, (byte) 0xE8, (byte) 0x02,
                (byte) 0x66, (byte) 0x89, (byte) 0x04, (byte) 0x66, (byte) 0xA1, (byte) 0x44,
                (byte) 0x7C, (byte) 0x66, (byte) 0x31, (byte) 0xD2, (byte) 0x66, (byte) 0xF7,
                (byte) 0x34, (byte) 0x88, (byte) 0x54, (byte) 0x0A, (byte) 0x66, (byte) 0x31,
                (byte) 0xD2, (byte) 0x66, (byte) 0xF7, (byte) 0x74, (byte) 0x04, (byte) 0x88,
                (byte) 0x54, (byte) 0x0B, (byte) 0x89, (byte) 0x44, (byte) 0x0C, (byte) 0x3B,
                (byte) 0x44, (byte) 0x08, (byte) 0x7D, (byte) 0x3C, (byte) 0x8A, (byte) 0x54,
                (byte) 0x0D, (byte) 0xC0, (byte) 0xE2, (byte) 0x06, (byte) 0x8A, (byte) 0x4C,
                (byte) 0x0A, (byte) 0xFE, (byte) 0xC1, (byte) 0x08, (byte) 0xD1, (byte) 0x8A,
                (byte) 0x6C, (byte) 0x0C, (byte) 0x5A, (byte) 0x8A, (byte) 0x74, (byte) 0x0B,
                (byte) 0xBB, (byte) 0x00, (byte) 0x70, (byte) 0x8E, (byte) 0xC3, (byte) 0x31,
                (byte) 0xDB, (byte) 0xB8, (byte) 0x01, (byte) 0x02, (byte) 0xCD, (byte) 0x13,
                (byte) 0x72, (byte) 0x2A, (byte) 0x8C, (byte) 0xC3, (byte) 0x8E, (byte) 0x06,
                (byte) 0x48, (byte) 0x7C, (byte) 0x60, (byte) 0x1E, (byte) 0xB9, (byte) 0x00,
                (byte) 0x01, (byte) 0x8E, (byte) 0xDB, (byte) 0x31, (byte) 0xF6, (byte) 0x31,
                (byte) 0xFF, (byte) 0xFC, (byte) 0xF3, (byte) 0xA5, (byte) 0x1F, (byte) 0x61,
                (byte) 0xFF, (byte) 0x26, (byte) 0x42, (byte) 0x7C, (byte) 0xBE, (byte) 0x7C,
                (byte) 0x7D, (byte) 0xE8, (byte) 0x40, (byte) 0x00, (byte) 0xEB, (byte) 0x0E,
                (byte) 0xBE, (byte) 0x81, (byte) 0x7D, (byte) 0xE8, (byte) 0x38, (byte) 0x00,
                (byte) 0xEB, (byte) 0x06, (byte) 0xBE, (byte) 0x8B, (byte) 0x7D, (byte) 0xE8,
                (byte) 0x30, (byte) 0x00, (byte) 0xBE, (byte) 0x90, (byte) 0x7D, (byte) 0xE8,
                (byte) 0x2A, (byte) 0x00, (byte) 0xEB, (byte) 0xFE, (byte) 0x47, (byte) 0x52,
                (byte) 0x55, (byte) 0x42, (byte) 0x20, (byte) 0x00, (byte) 0x47, (byte) 0x65,
                (byte) 0x6F, (byte) 0x6D, (byte) 0x00, (byte) 0x48, (byte) 0x61, (byte) 0x72,
                (byte) 0x64, (byte) 0x20, (byte) 0x44, (byte) 0x69, (byte) 0x73, (byte) 0x6B,
                (byte) 0x00, (byte) 0x52, (byte) 0x65, (byte) 0x61, (byte) 0x64, (byte) 0x00,
                (byte) 0x20, (byte) 0x45, (byte) 0x72, (byte) 0x72, (byte) 0x6F, (byte) 0x72,
                (byte) 0x00, (byte) 0xBB, (byte) 0x01, (byte) 0x00, (byte) 0xB4, (byte) 0x0E,
                (byte) 0xCD, (byte) 0x10, (byte) 0xAC, (byte) 0x3C, (byte) 0x00, (byte) 0x75,
                (byte) 0xF4, (byte) 0xC3, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x24, (byte) 0x12,
                (byte) 0x0F, (byte) 0x09, (byte) 0x00, (byte) 0xBE, (byte) 0xBD, (byte) 0x7D,
                (byte) 0x31, (byte) 0xC0, (byte) 0xCD, (byte) 0x13, (byte) 0x46, (byte) 0x8A,
                (byte) 0x0C, (byte) 0x80, (byte) 0xF9, (byte) 0x00, (byte) 0x75, (byte) 0x0F,
                (byte) 0xBE, (byte) 0xDA, (byte) 0x7D, (byte) 0xE8, (byte) 0xC6, (byte) 0xFF,
                (byte) 0xEB, (byte) 0x94, (byte) 0x46, (byte) 0x6C, (byte) 0x6F, (byte) 0x70,
                (byte) 0x70, (byte) 0x79, (byte) 0x00, (byte) 0xBB, (byte) 0x00, (byte) 0x70,
                (byte) 0xB8, (byte) 0x01, (byte) 0x02, (byte) 0xB5, (byte) 0x00, (byte) 0xB6,
                (byte) 0x00, (byte) 0xCD, (byte) 0x13, (byte) 0x72, (byte) 0xD7, (byte) 0xB6,
                (byte) 0x01, (byte) 0xB5, (byte) 0x4F, (byte) 0xE9, (byte) 0xDD, (byte) 0xFE,
                (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
                (byte) 0x00, (byte) 0x00, (byte) 0x55, (byte) 0xAA};


    private static final BootSector FAT_STANDARD_BS =
            new BootSector(BOOT_BYTES);

}
