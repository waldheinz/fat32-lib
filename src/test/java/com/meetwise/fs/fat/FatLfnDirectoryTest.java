
package com.meetwise.fs.fat;

import com.meetwise.fs.BlockDevice;
import com.meetwise.fs.FileSystemException;
import com.meetwise.fs.fat.FatLfnDirectory.LfnEntry;
import com.meetwise.fs.util.RamDisk;
import java.io.IOException;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class FatLfnDirectoryTest {

    private BlockDevice dev;
    private BootSector bs;
    private Fat16RootDirectory rootDirStore;
    private Fat fat;
    private FatLfnDirectory dir;

    @Before
    public void setUp() throws IOException {
        this.dev = new RamDisk(1024 * 1024);
        SuperFloppyFormatter sff = new SuperFloppyFormatter(dev);
        sff.format();
        
        this.bs = BootSector.read(dev);
        this.rootDirStore = Fat16RootDirectory.read(
                (Fat16BootSector) bs, false);
        this.fat = Fat.read(bs, 0);
        this.dir = new FatLfnDirectory(rootDirStore, fat);
    }
    
    @Test
    public void testGeneratedEntries() throws IOException {
        System.out.println("generatedEntries");

        final int orig = rootDirStore.getEntryCount();
        System.out.println("orig=" + orig);
        dir.flush();
        assertEquals(orig, rootDirStore.getEntryCount());
        dir.addFile("hallo");
        dir.flush();
        assertTrue(orig < rootDirStore.getEntryCount());
    }

    @Test
    public void testGetFile() throws IOException {
        System.out.println("getFile");
        
        final LfnEntry fentry = dir.addFile(
                "I have problems making up file names");
        final FatFile file = dir.getFile(fentry.getRealEntry());
        
        assertEquals(fentry.getFile(), file);
    }
    
    @Test
    public void testGetStorageDirectory() {
        System.out.println("getStorageDirectory");
        
        assertEquals(rootDirStore, dir.getStorageDirectory());
    }
    
    @Test
    public void testIsDirty() throws FileSystemException {
        System.out.println("isDirty");
        
        assertFalse(dir.isDirty());
        dir.addFile("a file");
        assertTrue(dir.isDirty());
    }
    
    @Test
    @Ignore
    public void testGetLabel() {
        System.out.println("getLabel");
        
        fail("The test case is a prototype.");
    }
    
    @Test
    @Ignore
    public void testSetLabel() throws Exception {
        System.out.println("setLabel");
        
        dir.setLabel("a file system label");
    }
    
    @Test
    public void testAddFile() throws Exception {
        System.out.println("addFile");
        
        assertNotNull(dir.addFile("A good file"));
    }
    
    @Test
    public void testAddDirectory() throws Exception {
        System.out.println("addDirectory");

        assertNotNull(dir.addDirectory("A nice directory"));
    }
    
    @Test
    public void testGetEntry() throws FileSystemException {
        System.out.println("getEntry");
        
        final String NAME = "A fine File";
        final LfnEntry file = dir.addFile(NAME);
        assertEquals(file, dir.getEntry(NAME));
    }
    
    @Test
    public void testFlush() throws Exception {
        System.out.println("flush");
        
        dir.addFile("The perfect File");
        assertTrue(dir.isDirty());

        dir.flush();
        assertFalse(dir.isDirty());
    }
    
}
