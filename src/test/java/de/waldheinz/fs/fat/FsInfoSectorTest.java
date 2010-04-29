
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
