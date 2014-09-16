/*
 * Copyright (C) 2009-2014 Andre Bierwolf <a.b.bierwolf@gmail.com>
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

package de.waldheinz.fs.fat;

import static org.junit.Assert.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import org.junit.Test;
import de.waldheinz.fs.util.RamDisk;

/**
 * 
 * @author Andr&eacute; Bierwolf &lt;a.b.bierwolf at gmail.com&gt;
 */
public class AbstractDirectoryTest {

    private static final int TEST_CAPACITY = 10;

    @Test
    public void testFlushWithMaxEntriesAndVolumeLabel() throws IOException {
        System.out.println("testFlushWithMaxEntriesAndVolumeLabel");

        RamDisk dev = new RamDisk(1024 * 1024);
        BootSector bs = new Fat16BootSector(dev);
        bs.init();
        bs.setNrFats(2);
        bs.setBytesPerSector(dev.getSectorSize());
        bs.setSectorCount(dev.getSize() / dev.getSectorSize());
        bs.setSectorsPerCluster(1);
        bs.setSectorsPerFat(130);
        bs.write();
        Fat fat = Fat.create(bs, 0);

        AbstractDirectory directory = new AbstractDirectory(FatType.FAT32, TEST_CAPACITY, false, true) {

            @Override
            protected void write(ByteBuffer data) throws IOException {
            }

            @Override
            protected void read(ByteBuffer data) throws IOException {
            }

            @Override
            protected long getStorageCluster() {
                return 0;
            }

            @Override
            protected void changeSize(int entryCount) throws DirectoryFullException, IOException {
            }
        };

        for (int i = 0; i < TEST_CAPACITY; i++) {
            FatDirectoryEntry subDir = directory.createSub(fat);
            subDir.setShortName(ShortName.get("dir" + i));
            directory.addEntry(subDir);
        }
        directory.flush();
        directory.setLabel("TEST");
        try {
            directory.flush();
        } catch (java.nio.BufferOverflowException e) {
            fail("Should not throw an exception here");
        }
    }
}
