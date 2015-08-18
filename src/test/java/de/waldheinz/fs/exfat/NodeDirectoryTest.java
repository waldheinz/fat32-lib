
package de.waldheinz.fs.exfat;

import de.waldheinz.fs.FsDirectory;
import org.junit.Ignore;
import de.waldheinz.fs.util.RamDisk;
import java.io.IOException;
import java.io.InputStream;
import de.waldheinz.fs.FsDirectoryEntry;
import java.util.Iterator;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class NodeDirectoryTest {
    
    private RamDisk rd;
    private NodeDirectory dir;

    @Before
    public void setUp() throws IOException {
        final InputStream is =
                getClass().getResourceAsStream("exfat-test.dmg.gz");
        rd = RamDisk.readGzipped(is);
        is.close();
        dir = (NodeDirectory) ExFatFileSystem.read(rd, false).getRoot();
    }
    
    @Test
    public void testReadBig() throws IOException {
        System.out.println("read big dir");
        FsDirectoryEntry bde = dir.getEntry("Big Folder");
        FsDirectory bd = bde.getDirectory();
        
        assertNotNull(bd);
        
        for (int i=1; i <= 1024; i++) {
            FsDirectoryEntry e = bd.getEntry("file-" + i);
            
            assertNotNull(e);
            assertTrue(e.isFile());
            assertEquals(0, e.getFile().getLength());
        }
    }
    
    @Test
    public void testIterator() {
        System.out.println("iterator");

        Iterator<FsDirectoryEntry> it = dir.iterator();
        assertNotNull(it);

        while (it.hasNext()) {
            System.out.println(it.next());
        }
    }

    @Test
    public void testGetEntry() throws Exception {
        System.out.println("getEntry");
        
        final String name = "folder with image";
        
        final FsDirectoryEntry e = dir.getEntry(name);
        
        assertNotNull(e);
        assertEquals(name, e.getName());
        assertTrue(e.isDirectory());
        
        assertEquals(e, dir.getEntry(name.toUpperCase()));
    }
    
    @Test
    @Ignore
    public void testAddFile() throws Exception {
        System.out.println("addFile");
        String name = "";
        NodeDirectory instance = null;
        FsDirectoryEntry expResult = null;
        FsDirectoryEntry result = instance.addFile(name);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    @Test
    @Ignore
    public void testAddDirectory() throws Exception {
        System.out.println("addDirectory");
        String name = "";
        NodeDirectory instance = null;
        FsDirectoryEntry expResult = null;
        FsDirectoryEntry result = instance.addDirectory(name);
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    @Test
    @Ignore
    public void testRemove() throws Exception {
        System.out.println("remove");
        String name = "";
        NodeDirectory instance = null;
        instance.remove(name);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
    @Test
    public void testFlush() throws Exception {
        System.out.println("flush");
        
        dir.flush();
    }
    
}
