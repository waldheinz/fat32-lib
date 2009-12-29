
package com.meetwise.fs.fat;

import com.meetwise.fs.util.RamDisk;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class BootSectorTest {
    
    @Test
    public void testNrFats() throws IOException {
        System.out.println("setNrFats");

        RamDisk rd = new RamDisk(512);
        BootSector bs = new Fat32BootSector(rd);
        bs.init();
        bs.setNrFats(2);
        bs.write();

        bs = BootSector.read(rd);
        assertTrue(bs instanceof Fat32BootSector);
        assertEquals(2, bs.getNrFats());
    }
}
