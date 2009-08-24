
package com.meetwise.fs.fat;

import com.meetwise.fs.RamDisk;
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
                "/data/fat16-test.img.gz");

        final RamDisk rd = RamDisk.readGzipped(is);
        assertEquals(512, rd.getSectorSize());
        assertEquals(10240000, rd.getLength());
    }
}
