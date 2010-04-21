
package de.waldheinz.fs.fat;

import de.waldheinz.fs.fat.Fat16BootSector;
import de.waldheinz.fs.fat.Fat;
import de.waldheinz.fs.fat.FatUtils;
import de.waldheinz.fs.fat.BootSector;
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
public class FatTest {

    private BlockDevice dev;
    private BootSector bs;
    private Fat fat;

    @Before
    public void setUp() throws IOException {
        this.dev = new RamDisk(1024 * 1024);
        this.bs = new Fat16BootSector(dev);
        this.bs.init();
        this.bs.setNrFats(2);
        this.bs.setBytesPerSector(dev.getSectorSize());
        this.bs.setSectorCount(dev.getSize() / dev.getSectorSize());
        this.bs.setSectorsPerCluster(1);
        this.bs.setSectorsPerFat(130);
        this.bs.write();
        this.fat = Fat.create(bs, 0);
    }
    
    @Test
    public void testRead() throws IOException {
        System.out.println("read");
        
        Fat result = Fat.read(bs, 0);
        assertEquals(fat, result);
    }
    
    @Test
    public void testCreate() {
        System.out.println("create");
        
        assertEquals(bs.getDataClusterCount(), fat.getFreeClusterCount());
    }
    
    @Test
    public void testGetFatType() {
        System.out.println("getFatType");
        
        assertEquals(bs.getFatType(), fat.getFatType());
    }
    
    @Test
    public void testGetBootSector() {
        System.out.println("getBootSector");
        
        assertEquals(bs, fat.getBootSector());
    }
    
    @Test
    public void testGetDevice() {
        System.out.println("getDevice");
        
        assertEquals(bs.getDevice(), fat.getDevice());
    }
    
    @Test
    public void testWrite() throws IOException {
        System.out.println("write");
        
        fat.write();
    }
    
    @Test
    public void testWriteCopy() throws Exception {
        System.out.println("writeCopy");
        
        fat.writeCopy(FatUtils.getFatOffset(bs, 1));
    }
    
    @Test
    public void testGetMediumDescriptor() {
        System.out.println("getMediumDescriptor");

        assertEquals(bs.getMediumDescriptor(), fat.getMediumDescriptor());
    }

    @Test
    public void testAllocNew() throws IOException {
        System.out.println("allocNew");
        
        long result = fat.allocNew();
        
        assertEquals(result, fat.getLastAllocatedCluster());
    }
    
    @Test
    public void testGetFreeClusterCount() throws IOException {
        System.out.println("getFreeClusterCount");

        final int max = fat.getFreeClusterCount();

        for (int i=0; i < max; i++) {
            assertEquals(max - i, fat.getFreeClusterCount());
            fat.allocNew();
        }

        assertEquals(0, fat.getFreeClusterCount());

        try {
            fat.allocNew();
            fail("allocated too many clusters");
        } catch (IOException ex) {
            /* fine */
        }
    }
    
}
