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
import java.nio.ByteBuffer;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class ClusterChainTest {

    private ClusterChain cc;
    private Fat fat;
    private BootSector bs;

    @Before
    public void setUp() throws IOException {
        final RamDisk rd = new RamDisk(512 * 2048);
        SuperFloppyFormatter.get(rd).format();
        
        bs = BootSector.read(rd);
        fat = Fat.read(bs, 0);
        cc = new ClusterChain(fat, false);
    }

    @Test
    public void testWriteBufferLimit() throws IOException {
        System.out.println("writeBufferLimit");

        ByteBuffer data = ByteBuffer.allocate(4096);
        
        for (int i=0, off=0; i < 4096; i += 13, off += 7) {
            data.limit(i);
            data.position(off);
            cc.writeData(off, data);

            assertEquals(i, data.limit());
            assertEquals(i, data.position());
        }
    }
    
    @Test
    public void testWriteData() throws IOException {
        System.out.println("writeData");
        final int chunkSize = 123;

        ByteBuffer data = ByteBuffer.allocate(chunkSize);
        for (int i=0; i < chunkSize; i++) {
            data.put(i, (byte)(i & 0xff));
        }
        
        final int writes = 10;

        for (int i=0; i < writes; i++) {
            cc.writeData(i * chunkSize, data);
            data.rewind();
        }

        ByteBuffer read = ByteBuffer.allocate(writes * chunkSize);
        cc.readData(0, read);

        byte expected = 0;
        for (int i=0; i < writes * chunkSize; i++) {
            assertEquals(expected & 0xff, read.get(i));
            
            expected = (byte)(++expected % chunkSize);
        }
    }

    @Test
    public void testClustersInSync() throws IOException {
        System.out.println("clustersInSync");

        final int clusters = fat.getFreeClusterCount();

        cc.setChainLength(1);
        assertEquals(clusters - 1, fat.getFreeClusterCount());

        cc.setChainLength(0);
        assertEquals(clusters, fat.getFreeClusterCount());
    }
    
    @Test
    public void testWrite() throws IOException {
        System.out.println("write (bytes per cluster=" +
                bs.getBytesPerCluster() + ")");
                
        final ByteBuffer buff = ByteBuffer.allocate(256);
        cc.writeData(0, buff);
        assertEquals(0, buff.remaining());
        
        buff.rewind();
        cc.writeData(buff.capacity(), buff);
        assertEquals(0, buff.remaining());
    }
    
    @Test
    public void testSetSize() throws IOException {
        System.out.println("setSize");
        
        cc.setSize(bs.getBytesPerCluster());
        assertEquals(1, cc.getChainLength());

        cc.setSize(bs.getBytesPerCluster() + 1);
        assertEquals(2, cc.getChainLength());

        cc.setSize(0);
        assertEquals(0, cc.getChainLength());

        cc.setSize(1);
        assertEquals(1, cc.getChainLength());
    }

    @Test
    public void testFirstClusterAlloc() throws IOException {
        System.out.println("firstClusterAlloc");

        cc.setSize(bs.getBytesPerCluster());

        assertEquals(1, cc.getChainLength());
        assertEquals(bs.getBytesPerCluster(), cc.getLengthOnDisk());
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testSetChainLengthNegative() throws IOException {
        System.out.println("setChainLength (negative)");

        cc.setChainLength(-1);
    }
}
