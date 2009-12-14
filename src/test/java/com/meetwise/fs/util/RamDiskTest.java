
package com.meetwise.fs.util;

import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class RamDiskTest {
    
    @Test
    public void testReadGzipped() throws IOException {
        System.out.println("testReadGzipped");
        
        final InputStream is = getClass().getResourceAsStream(
                "/fat16-test.img.gz");

        final RamDisk rd = RamDisk.readGzipped(is);
        assertEquals(512, rd.getSectorSize());
        assertEquals(10240000, rd.getSize());
    }

    @Test(expected=IllegalStateException.class)
    public void testClose() throws IOException {
        System.out.println("close");
        
        final RamDisk d = new RamDisk(4096);
        d.close();
        d.flush();
    }
    
}
