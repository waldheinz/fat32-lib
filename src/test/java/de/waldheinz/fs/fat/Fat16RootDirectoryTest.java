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
public class Fat16RootDirectoryTest {

    private BlockDevice dev;
    private Fat16BootSector bs;
    
    @Before
    public void setUp() throws IOException {
        this.dev = new RamDisk(1024 * 1024);
        this.bs = new Fat16BootSector(dev);
        this.bs.init();
        this.bs.write();
    }
    
    @Test
    public void testRead() throws Exception {
        System.out.println("read");
        
        Fat16RootDirectory.create(bs);
        
        Fat16RootDirectory dir = Fat16RootDirectory.read(bs, true);
        assertEquals(bs.getRootDirEntryCount(), dir.getCapacity());
        assertEquals(0, dir.getEntryCount());
    }
    
    @Test
    public void testCreate() throws Exception {
        System.out.println("create");
        
        Fat16RootDirectory dir = Fat16RootDirectory.create(bs);
        assertEquals(bs.getRootDirEntryCount(), dir.getCapacity());
        assertEquals(Fat16BootSector.DEFAULT_ROOT_DIR_ENTRY_COUNT,
                dir.getCapacity());
        assertEquals(0, dir.getEntryCount());
    }
    
    @Test
    public void testGetStorageCluster() throws IOException {
        System.out.println("getStorageCluster");

        Fat16RootDirectory dir = Fat16RootDirectory.create(bs);

        assertEquals(0, dir.getStorageCluster());
    }
    
    @Test
    public void testCanChangeSizeOk() throws IOException {
        System.out.println("canChangeSize (OK)");

        Fat16RootDirectory dir = Fat16RootDirectory.create(bs);
        dir.changeSize(dir.getCapacity());
    }
    
    @Test(expected=DirectoryFullException.class)
    public void testCanChangeSizeBad() throws IOException {
        System.out.println("canChangeSize (bad)");

        Fat16RootDirectory dir = Fat16RootDirectory.create(bs);
        dir.changeSize(dir.getCapacity() + 1);
    }
}
