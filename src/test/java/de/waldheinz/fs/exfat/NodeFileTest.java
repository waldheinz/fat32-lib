
package de.waldheinz.fs.exfat;

import org.junit.Ignore;
import de.waldheinz.fs.util.RamDisk;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class NodeFileTest {
    
    private RamDisk bd;
    private NodeDirectory dir;
    private NodeFile nf;
    
    @Before
    public void setUp() throws IOException {
        final InputStream is =
                getClass().getResourceAsStream("exfat-test.dmg.gz");
        bd = RamDisk.readGzipped(is);
        is.close();
        dir = (NodeDirectory) ExFatFileSystem.read(bd, false).getRoot();
        nf = (NodeFile) dir.getEntry("lorem ipsum.txt").getFile();
    }
    
    @Test
    public void testGetLength() {
        System.out.println("getLength");


        assertEquals(591, nf.getLength());
    }
    
    @Test
    @Ignore
    public void testSetLength() throws Exception {
        System.out.println("setLength");

    }
    
    @Test
    public void testReadLoremIpsum() throws Exception {
        System.out.println("read (Lorem Ipsum)");
        
        long longSize = nf.getLength();
        
        assertThat(longSize, allOf(greaterThan(0l), lessThan((long)Integer.MAX_VALUE)));
        
        if (longSize > Integer.MAX_VALUE) throw new AssertionError("too big");
        
        int size = (int) longSize;
        ByteBuffer buff = ByteBuffer.allocate(size);
        
        nf.read(0, buff);
        
        assertThat(buff.position(), is(buff.capacity()));
        
        System.out.println("The Lorem Imsum:");
        System.out.println(new String(buff.array()));
    }
    
    @Test
    @Ignore
    public void testWrite() throws Exception {
        System.out.println("write");
        
    }
    
    @Test
    @Ignore
    public void testFlush() throws Exception {
        System.out.println("flush");

        
    }

}
