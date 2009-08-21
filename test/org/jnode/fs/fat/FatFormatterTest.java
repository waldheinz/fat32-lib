
package org.jnode.fs.fat;

import com.meetwise.fat32.Utils;
import java.nio.ByteBuffer;
import org.jnode.driver.block.BlockDevice;
import org.jnode.driver.block.RamDisk;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class FatFormatterTest {
    
    @Test
    public void testFat16Format() throws Exception {
        System.out.println("testFat16Format");
        
        BlockDevice d = new RamDisk(5120000);
        
        FatFormatter ff = FatFormatter.superFloppyFormatter(d);
        ff.format(d, null);

        FatFileSystem fs = new FatFileSystem(d, false);
        final BootSector bs = fs.getBootSector();

        Utils.hexDump(System.out, ByteBuffer.wrap(bs.data));
        
        assertEquals(2, bs.getNrFats());
        assertEquals(d.getSectorSize(), bs.getBytesPerSector());
        assertTrue(bs.getNrRootDirEntries() % d.getSectorSize() == 0);
        
    }
    
}