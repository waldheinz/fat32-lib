
package de.waldheinz.fs.exfat;

import de.waldheinz.fs.util.RamDisk;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class ExFatDirectoryEntryTest {

    private RamDisk rd;
    private ExFatSuperBlock sb;
    
    
    @Before
    public void setUp() throws IOException {
        final InputStream is =
                getClass().getResourceAsStream("exfat-test.dmg.gz");
        this.rd = RamDisk.readGzipped(is);
        is.close();
        
        this.sb = ExFatSuperBlock.read(rd, false);
    }
    
    @Test
    public void testCreateRoot() throws Exception {
        System.out.println("createRoot");
        
        Node re = Node.createRoot(sb);
        
        assertNotNull(re);
        assertEquals("root cluster count", 1, re.getClusterCount());
    }
    
}
