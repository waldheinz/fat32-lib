
package com.meetwise.fs.util;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
public class FileDiskTest {

    private final static int SIZE = 1024 * 1024;
    private FileDisk fd;
    private File f;
    
    @Before
    public void setUp() throws Exception {
        f = File.createTempFile("fileDiskTest", ".tmp");
        f.deleteOnExit();
        fd = FileDisk.create(f, SIZE);
    }

    @After
    public void tearDown() throws IOException {
        fd.close();
        f.delete();
    }
    
    @Test
    public void testIsReadOnly() {
        System.out.println("isReadOnly");

        assertFalse(fd.isReadOnly());
    }

    @Test(expected=IllegalStateException.class)
    public void testClose() throws IOException {
        System.out.println("close");
        
        fd.close();
        fd.getSize();
    }

    @Test(expected=IOException.class)
    public void testReadPastEnd() throws IOException {
        System.out.println("readPastEnd");

        fd.read(SIZE - 999, ByteBuffer.allocate(1000));
    }

    @Test(expected=IOException.class)
    public void testWritePastEnd() throws IOException {
        System.out.println("writePastEnd");
        
        fd.write(SIZE - 999, ByteBuffer.allocate(1000));
    }

    @Test
    public void testPersistence() throws IOException {
        System.out.println("persistence");

        /* write random data */

        final ByteBuffer reference = ByteBuffer.allocate(SIZE);
        final Random rnd = new Random(System.currentTimeMillis());
        rnd.nextBytes(reference.array());
        fd.write(0, reference);
        fd.close();

        /* check if we can read it back in */
        
        fd = new FileDisk(f, true);
        final ByteBuffer read = ByteBuffer.allocate(SIZE);
        fd.read(0, read);

        reference.rewind();
        read.rewind();
        
        assertEquals(reference, read);
    }
    
}
