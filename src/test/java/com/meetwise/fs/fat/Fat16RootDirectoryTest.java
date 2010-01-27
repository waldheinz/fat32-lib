
package com.meetwise.fs.fat;

import com.meetwise.fs.BlockDevice;
import com.meetwise.fs.util.RamDisk;
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
        
        Fat16RootDirectory dir = Fat16RootDirectory.read(bs, true);
        assertEquals(bs.getRootDirEntryCount(), dir.getCapacity());
        assertEquals(0, dir.getEntryCount());
    }
    
    @Test
    public void testCreate() throws Exception {
        System.out.println("create");
        
        Fat16RootDirectory dir = Fat16RootDirectory.create(bs);
        assertEquals(bs.getRootDirEntryCount(), dir.getCapacity());
        assertEquals(0, dir.getEntryCount());
    }
    
    @Test
    public void testGetStorageCluster() throws IOException {
        System.out.println("getStorageCluster");

        Fat16RootDirectory dir = Fat16RootDirectory.create(bs);

        assertEquals(0, dir.getStorageCluster());
    }
    
    @Test
    public void testCanChangeSize() throws IOException {
        System.out.println("canChangeSize");

        Fat16RootDirectory dir = Fat16RootDirectory.create(bs);

        assertEquals(false, dir.canChangeSize(100));
    }
    
}
