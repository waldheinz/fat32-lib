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
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class ClusterChainDirectoryTest {

    private RamDisk rd;
    private Fat32BootSector bs;
    private Fat fat;
    private ClusterChain chain;
    private ClusterChainDirectory dir;

    @Before
    public void setUp() throws IOException {
        this.rd = new RamDisk(2048 * 2048);
        this.bs = new Fat32BootSector(rd);
        this.bs.init();
        
        this.bs.setSectorsPerCluster(2);
        this.bs.setSectorsPerFat(4096);
        
        this.fat = Fat.create(bs, 0);
        this.chain = new ClusterChain(fat, false);
        this.dir = ClusterChainDirectory.createRoot(chain);
    }

    @Test(expected=DirectoryFullException.class)
    public void testMaximumSize() throws IOException {
        System.out.println("maximumSize");
        
        while (true) {
            FatDirectoryEntry e = new FatDirectoryEntry();
            dir.addEntry(e);
            
            assertTrue(
                    chain.getLengthOnDisk() <= ClusterChainDirectory.MAX_SIZE);
        }
    }

    @Test
    public void testGetStorageCluster() {
        System.out.println("getStorageCluster");

        assertEquals(0, dir.getStorageCluster());
    }

    @Test
    public void testCreate() {
        System.out.println("create");

        assertEquals(
                chain.getLengthOnDisk() / FatDirectoryEntry.SIZE,
                dir.getCapacity());
    }
    
}
