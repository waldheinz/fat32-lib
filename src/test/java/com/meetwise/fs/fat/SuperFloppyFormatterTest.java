
package com.meetwise.fs.fat;

import com.meetwise.fs.util.RamDisk;
import java.io.IOException;
import org.junit.Test;

/**
 *
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
public class SuperFloppyFormatterTest {
    
    @Test
    public void testFormat() throws IOException {
        System.out.println("format");
        
        RamDisk rd = new RamDisk(1024 * 1024);
        SuperFloppyFormatter f = new SuperFloppyFormatter(rd);
        f.format();
    }
}
