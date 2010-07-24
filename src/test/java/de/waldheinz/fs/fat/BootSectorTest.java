/*
 * Copyright (C) 2009,2010 Matthias Treydte <mt@waldheinz.de>
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

import de.waldheinz.fs.util.RamDisk;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class BootSectorTest {

    @Test
    public void testSectorsPerCluster() throws IOException {
        System.out.println("sectorsPerCluster");

        RamDisk rd = new RamDisk(1024 * 1024);
        BootSector bs = new Fat16BootSector(rd);
        bs.init();
        bs.setSectorsPerCluster(1);
        bs.write();

        bs = BootSector.read(rd);
        assertEquals(1, bs.getSectorsPerCluster());
    }

    @Test
    public void testGetDataClusterCount() throws IOException {
        System.out.println("getDataClusterCount");

        RamDisk rd = new RamDisk(1024 * 1024);
        BootSector bs = new Fat16BootSector(rd);
        bs.init();
        bs.setSectorsPerCluster(2);
        bs.setSectorCount(2048);

        assertTrue(bs.getDataClusterCount() > 0);
    }

    @Test
    public void testNrFats() throws IOException {
        System.out.println("setNrFats");

        RamDisk rd = new RamDisk(512);
        BootSector bs = new Fat32BootSector(rd);
        bs.init();
        bs.setSectorsPerCluster(2);
        bs.setNrFats(2);
        bs.write();

        bs = BootSector.read(rd);
        assertEquals(2, bs.getNrFats());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testSetSectorsPerClusterInvalid() throws IOException {
        System.out.println("setSectorsPerCluster (invalid)");

        RamDisk rd = new RamDisk(512);
        BootSector bs = new Fat32BootSector(rd);
        bs.init();
        bs.setSectorsPerCluster(3);
    }

    @Test
    public void testSetSectorsPerClusterValid() throws IOException {
        System.out.println("setSectorsPerCluster (valid)");

        RamDisk rd = new RamDisk(512);
        BootSector bs = new Fat32BootSector(rd);
        bs.init();
        bs.setSectorsPerCluster(4);
    }
}
