
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
public class FatDirectoryTest {

    private RamDisk rd;
    private Fat32BootSector bs;
    private Fat fat;
    private ClusterChain chain;
    private FatDirectory dir;

    @Before
    public void setUp() throws IOException {
        this.rd = new RamDisk(512 * 2048);
        this.bs = new Fat32BootSector(rd);
        this.bs.init();
        this.fat = Fat.create(bs, 0);
        this.chain = new ClusterChain(fat, false);
        this.dir = FatDirectory.create(chain, 0, true);
    }

    @Test
    public void testGetStorageCluster() {
        System.out.println("getStorageCluster");

        assertEquals(bs.getRootDirFirstCluster(), dir.getStorageCluster());
    }
}
