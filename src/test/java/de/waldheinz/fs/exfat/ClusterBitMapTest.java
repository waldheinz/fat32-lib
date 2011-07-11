
package de.waldheinz.fs.exfat;

import java.io.IOException;
import java.io.InputStream;
import de.waldheinz.fs.util.RamDisk;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class ClusterBitMapTest {

    private RamDisk bd;
    private ExFatSuperBlock sb;

    @Before
    public void setUp() throws IOException {
        final InputStream is =
                getClass().getResourceAsStream("exfat-test.dmg.gz");
        bd = RamDisk.readGzipped(is);
        sb = ExFatSuperBlock.read(bd, false);
        is.close();
    }
    
    @Test
    public void testRead() throws IOException {
        System.out.println("read");
        
        ClusterBitMap result = ClusterBitMap.read(sb, 2, 1246);
        assertNotNull(result);
    }
    
    @Test
    public void testIsClusterFree() throws IOException {
        System.out.println("isClusterFree");
        
        ClusterBitMap result = ClusterBitMap.read(sb, 2, 1246);
        
        for (long i=2; i < 512; i++) {
            result.isClusterFree(i);
        }
    }
    
    @Test(expected=IOException.class)
    public void testReadFail() throws IOException {
        System.out.println("read (fail)");

        ClusterBitMap.read(sb, 2, 12);
    }
    
}
