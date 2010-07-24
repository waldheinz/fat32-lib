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

import de.waldheinz.fs.BlockDevice;
import de.waldheinz.fs.util.RamDisk;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class FsInfoSectorTest {

    private BlockDevice dev;
    private Fat32BootSector bs;
    
    @Before
    public void setUp() throws IOException {
        this.dev = new RamDisk(1024);
        this.bs = new Fat32BootSector(dev);
        this.bs.init();
        this.bs.setFsInfoSectorNr(1);
    }

    @Test(expected=IOException.class)
    public void testCreateFail() throws Exception {
        System.out.println("create (fail)");

        bs.setFsInfoSectorNr(0);
        FsInfoSector.create(bs);
    }

    @Test(expected=IOException.class)
    public void testReadFail() throws Exception {
        System.out.println("read (fail)");
        
        FsInfoSector.read(bs);
    }
    
    @Test
    public void testRead() throws IOException {
        System.out.println("read");
        
        FsInfoSector.create(bs);
        FsInfoSector.read(bs);
    }

    @Test
    public void testCreate() throws Exception {
        System.out.println("create");

        FsInfoSector.create(bs);
    }
    
    @Test
    public void testSetFreeClusterCount() throws IOException {
        System.out.println("setFreeClusterCount");

        final FsInfoSector fsi = FsInfoSector.create(bs);
        fsi.setFreeClusterCount(100);
        assertEquals(100, fsi.getFreeClusterCount());
    }
    
    @Test
    public void testSetLastAllocatedCluster() throws IOException {
        System.out.println("setLastAllocatedCluster");

        final FsInfoSector fsi = FsInfoSector.create(bs);
        fsi.setLastAllocatedCluster(100);
        assertEquals(100, fsi.getLastAllocatedCluster());
    }
    
}
