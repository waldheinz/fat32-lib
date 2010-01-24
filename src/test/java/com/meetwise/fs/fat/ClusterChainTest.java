
package com.meetwise.fs.fat;


import com.meetwise.fs.util.RamDisk;
import java.io.IOException;
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

    @Before
    public void setUp() throws IOException {
        final RamDisk rd = new RamDisk(512 * 2048);
        final BootSector bs = new Fat16BootSector(rd);
        bs.init();
        bs.setSectorsPerFat(20);
        bs.write();
        
        fat = Fat.create(bs, 0);
        cc = new ClusterChain(fat, Fat.FIRST_CLUSTER, false);
    }

    @Test
    public void testSetSize() throws IOException {
        System.out.println("setLength");

        cc.setSize(4096);
        cc.setSize(0);
    }
}
