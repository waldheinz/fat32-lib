/*
 * Copyright (C) 2009-2013 Matthias Treydte <mt@waldheinz.de>
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
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class FatFileTest {

    private Fat fat;
    private FatLfnDirectoryEntry entry;
    private FatFile ff;

    @Before
    public void setUp() throws IOException {
        final InputStream is = getClass().getResourceAsStream(
                "fat12-test.img.gz");

        final RamDisk rd = RamDisk.readGzipped(is);
        final FatFileSystem fatFs = new FatFileSystem(rd, false);

        this.entry = (FatLfnDirectoryEntry) fatFs.getRoot().getEntry("Readme.txt");
        this.fat = fatFs.getFat();
        this.ff = FatFile.get(fat, entry.realEntry);
    }
    
    @Test
    public void testGetLength() {
        System.out.println("getLength");

        assertEquals(27, ff.getLength());
    }
    
    @Test
    public void testSetLength() throws Exception {
        System.out.println("setLength");

        final long origModified = entry.getLastModified();
        final long origAccessed = entry.getLastAccessed();
        final long origCreated = entry.getCreated();
        
        ff.setLength(100);
        assertEquals(100, ff.getLength());
        assertTrue(ff.getChain().getLengthOnDisk() >= 100);
        assertTrue(entry.getLastAccessed() > origAccessed);
        assertTrue(entry.getLastModified() > origModified);
        assertEquals(origCreated, entry.getCreated());
    }
    
    @Test
    public void testReadOk() throws Exception {
        System.out.println("read (OK)");

        final long origModified = entry.getLastModified();
        final long origAccessed = entry.getLastAccessed();
        final long origCreated = entry.getCreated();

        final ByteBuffer data = ByteBuffer.allocate((int) ff.getLength());
        ff.read(0, data);
        
        assertTrue(entry.getLastAccessed() > origAccessed);
        assertEquals(entry.getLastModified(), origModified);
        assertEquals(origCreated, entry.getCreated());
    }

    @Test(expected=IOException.class)
    public void testReadTooLong() throws Exception {
        System.out.println("read (too long)");
        
        final ByteBuffer data = ByteBuffer.allocate((int) ff.getLength() + 1);
        ff.read(0, data);
    }
    
    @Test
    public void testWrite() throws Exception {
        System.out.println("write");
        
        final long origModified = entry.getLastModified();
        final long origAccessed = entry.getLastAccessed();
        final long origCreated = entry.getCreated();
        final int len = 100000;
        
        final ByteBuffer data = ByteBuffer.allocate(len);
        ff.write(0, data);
        
        assertTrue(entry.getLastAccessed() > origAccessed);
        assertTrue(entry.getLastModified() > origModified);
        assertEquals(origCreated, entry.getCreated());
        assertEquals(len, ff.getLength());
        assertEquals(len / fat.getBootSector().getBytesPerCluster() + 1,
                ff.getChain().getChainLength());
    }
    
}
