
package de.waldheinz.fs.exfat;

import de.waldheinz.fs.FsDirectory;
import de.waldheinz.fs.FsDirectoryEntry;
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
    public void testGetSpace() throws IOException {
        System.out.println("get*Space");
        
        final ExFatFileSystem fs = ExFatFileSystem.read(bd, true);
        
        assertEquals(fs.getFreeSpace(), -1);
        assertEquals(fs.getTotalSpace(), -1);
        assertEquals(fs.getUsableSpace(), -1);
    }
    
    @Test
    public void testRead() throws Exception {
        System.out.println("read");

        final ExFatFileSystem fs = ExFatFileSystem.read(bd, true);
        
        assertNotNull(fs);
    }

    @Test
    public void testGetRoot() throws IOException {
        System.out.println("getRoot");

        final ExFatFileSystem fs = ExFatFileSystem.read(bd, true);
        final FsDirectory rootDir = fs.getRoot();

        for (FsDirectoryEntry e : rootDir) {
            System.out.println(e);
        }
        
    }

}
