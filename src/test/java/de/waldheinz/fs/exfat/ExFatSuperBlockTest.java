
package de.waldheinz.fs.exfat;

import java.io.IOException;
import java.io.InputStream;
import de.waldheinz.fs.BlockDevice;
import de.waldheinz.fs.util.RamDisk;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class ExFatSuperBlockTest {

    private BlockDevice bd;

    @Before
    public void setUp() throws IOException {
        final InputStream is =
                getClass().getResourceAsStream("exfat-test.dmg.gz");
        bd = RamDisk.readGzipped(is);
        is.close();
    }
    
    @Test
    public void testRead() throws IOException {
        System.out.println("read");
        
        final ExFatSuperBlock sb = ExFatSuperBlock.read(bd, false);
        
        assertEquals("block start", 0, sb.getBlockStart());
        assertEquals("block count", 80000, sb.getBlockCount());
        assertEquals("fat first block", 128, sb.getFatBlockStart());
        assertEquals("fat block count", 128, sb.getFatBlockCount());
        assertEquals("first cluster block", 256, sb.getClusterBlockStart());
        assertEquals("cluster count", 9968, sb.getClusterCount());
        assertEquals("root dir cluster", 5, sb.getRootDirCluster());
        assertEquals("volume serial", 0x4ce52a96, sb.getVolumeSerial());
        assertEquals("version major", 1, sb.getFsVersionMajor());
        assertEquals("version minor", 0, sb.getFsVersionMinor());
        assertEquals("volume state", 0x0000, sb.getVolumeState());
        assertEquals("block size", 512, sb.getBlockSize());
        assertEquals("cluster size", 8, sb.getBlocksPerCluster());
        
        System.out.println(sb.getPercentInUse());
    }
    
}
