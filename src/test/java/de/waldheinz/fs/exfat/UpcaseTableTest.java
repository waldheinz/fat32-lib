
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
public class UpcaseTableTest {
    
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
    public void testRead() throws Exception {
        System.out.println("read");
        
        final UpcaseTable ut = UpcaseTable.read(
                sb, 3, 5836, 0xffffffffe619d30dl);
        
        assertNotNull(ut);
        assertEquals(2918, ut.getCharCount());
    }
    
    @Test
    public void testToUpperCase() throws IOException {
        System.out.println("toUpperCase");
        
        final UpcaseTable ut = UpcaseTable.read(
                sb, 3, 5836, 0xffffffffe619d30dl);
        
        assertEquals('A', ut.toUpperCase('a'));
        assertEquals('Z', ut.toUpperCase('z'));
        assertEquals('Ö', ut.toUpperCase('ö'));
        
        assertEquals('ß', ut.toUpperCase('ß'));
        assertEquals('A', ut.toUpperCase('A'));
        assertEquals('Z', ut.toUpperCase('Z'));
        assertEquals('Ö', ut.toUpperCase('Ö'));
    }
    
}
