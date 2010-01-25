
package com.meetwise.fs.fat;


import com.meetwise.fs.util.RamDisk;
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
        bs = new Fat16BootSector(rd);
        bs.init();
        bs.setSectorsPerFat(20);
        bs.setSectorsPerCluster(2);
        bs.write();
        
        fat = Fat.create(bs, 0);
        cc = new ClusterChain(fat, false);
    }

    @Test
    public void testWrite() throws IOException {
        System.out.println("write");

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
        
        cc.setSize(0);
        assertEquals(0, cc.getChainLength());
    }

    @Test
    public void testFirstClusterAlloc() throws IOException {
        System.out.println("firstClusterAlloc");

        cc.setSize(bs.getBytesPerCluster());

        assertEquals(1, cc.getChainLength());
        assertEquals(bs.getBytesPerCluster(), cc.getLengthOnDisk());
    }
}
