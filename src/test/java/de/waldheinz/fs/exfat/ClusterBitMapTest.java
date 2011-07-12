
package de.waldheinz.fs.exfat;

import java.io.IOException;
import java.io.InputStream;
import de.waldheinz.fs.util.RamDisk;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class ClusterBitMapTest {
    
    private RamDisk bd;
    private ExFatSuperBlock sb;
    private ClusterBitMap cbm;
    
    @Before
    public void setUp() throws IOException {
        final InputStream is =
                getClass().getResourceAsStream("exfat-test.dmg.gz");
        bd = RamDisk.readGzipped(is);
        sb = ExFatSuperBlock.read(bd, false);
        cbm = ClusterBitMap.read(sb, 2, 1246);
        is.close();
    }
    
    @Test
    public void testGetUsedClusterCount() throws IOException {
        System.out.println("getUsedClusterCount");
        
        final long usedClusterCount = cbm.getUsedClusterCount();
        
        System.out.println(
                "bitmap says " + usedClusterCount + " clusters used");
        
        long total = sb.getClusterCount();
        
        for (long i=Cluster.FIRST_DATA_CLUSTER;
                i <= sb.getClusterCount() + 1; i++) {
            
            if (cbm.isClusterFree(i)) {
                total--;
            }
        }
        
        System.out.println("counting says " + total + " clusters used");
        
        assertThat(usedClusterCount, equalTo(total));
    }
    
    @Test
    public void testIsClusterFree() throws IOException {
        System.out.println("isClusterFree");
        
        for (long i=2; i < 512; i++) {
            cbm.isClusterFree(i);
        }
    }
    
    @Test(expected=IOException.class)
    public void testReadFail() throws IOException {
        System.out.println("read (fail)");

        ClusterBitMap.read(sb, 2, 12);
    }
    
}
