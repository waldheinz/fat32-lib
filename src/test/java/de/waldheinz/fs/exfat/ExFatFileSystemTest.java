
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
public class ExFatFileSystemTest {

    private RamDisk bd;
    
    @Before
    public void setUp() throws IOException {
        final InputStream is =
                getClass().getResourceAsStream("exfat-test.dmg.gz");
        bd = RamDisk.readGzipped(is);
        is.close();
    }
    
    @Test
    public void testRead() throws Exception {
        System.out.println("read");

        final ExFatFileSystem fs = ExFatFileSystem.read(bd, true);
        
        assertNotNull(fs);
    }

}
