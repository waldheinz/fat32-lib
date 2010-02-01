
package com.meetwise.fs.fat;

import com.meetwise.fs.FileSystemException;
import com.meetwise.fs.fat.FatLfnDirectory.LfnEntry;
import com.meetwise.fs.util.RamDisk;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class FatFileTest {

    private Fat fat;
    private LfnEntry entry;
    private FatDirEntry fatEntry;
    private FatFile ff;

    @Before
    public void setUp() throws IOException {
        final InputStream is = getClass().getResourceAsStream(
                "/fat12-test.img.gz");

        final RamDisk rd = RamDisk.readGzipped(is);
        final FatFileSystem fatFs = new FatFileSystem(rd, false);

        this.entry = (LfnEntry) fatFs.getRoot().getEntry("Readme.txt");
        this.fatEntry = entry.getRealEntry();
        this.fat = fatFs.getFat();
        this.ff = FatFile.get(fat, fatEntry);
    }
    
    @Test
    public void testGet() throws Exception {
        System.out.println("get");
        
        assertEquals(ff, FatFile.get(fat, fatEntry));
    }
    
    @Test
    public void testGetLength() {
        System.out.println("getLength");

        assertEquals(27, ff.getLength());
    }
    
    @Test
    public void testSetLength() throws Exception {
        System.out.println("setLength");

        final long origModified = entry.getLastModified();
        final long origAccessed = entry.getLastAccessed();
        final long origCreated = entry.getCreated();
        
        ff.setLength(100);
        assertEquals(100, ff.getLength());
        assertTrue(ff.getChain().getLengthOnDisk() >= 100);
        assertTrue(entry.getLastAccessed() > origAccessed);
        assertTrue(entry.getLastModified() > origModified);
        assertEquals(origCreated, entry.getCreated());
    }
    
    @Test
    public void testReadOk() throws Exception {
        System.out.println("read (OK)");

        final long origModified = entry.getLastModified();
        final long origAccessed = entry.getLastAccessed();
        final long origCreated = entry.getCreated();

        final ByteBuffer data = ByteBuffer.allocate((int) ff.getLength());
        ff.read(0, data);
        
        assertTrue(entry.getLastAccessed() > origAccessed);
        assertEquals(entry.getLastModified(), origModified);
        assertEquals(origCreated, entry.getCreated());
    }

    @Test(expected=FileSystemException.class)
    public void testReadTooLong() throws Exception {
        System.out.println("read (too long)");
        
        final ByteBuffer data = ByteBuffer.allocate((int) ff.getLength() + 1);
        ff.read(0, data);
    }
    
    @Test
    public void testWrite() throws Exception {
        System.out.println("write");
        
        final long origModified = entry.getLastModified();
        final long origAccessed = entry.getLastAccessed();
        final long origCreated = entry.getCreated();

        final ByteBuffer data = ByteBuffer.allocate(100000);
        ff.write(0, data);
        
        assertTrue(entry.getLastAccessed() > origAccessed);
        assertTrue(entry.getLastModified() > origModified);
        assertEquals(origCreated, entry.getCreated());

    }
    
}
