
package com.meetwise.fs.fat;

import com.meetwise.fs.util.RamDisk;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class BootSectorTest {

    @Test
    public void testGetDataClusterCount() throws IOException {
        System.out.println("getDataClusterCount");

        RamDisk rd = new RamDisk(1024 * 1024);
        BootSector bs = new Fat16BootSector(rd);
        bs.init();

        assertTrue(bs.getDataClusterCount() > 0);
    }

    @Test
    public void testNrFats() throws IOException {
        System.out.println("setNrFats");

        RamDisk rd = new RamDisk(512);
        BootSector bs = new Fat32BootSector(rd);
        bs.init();
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
