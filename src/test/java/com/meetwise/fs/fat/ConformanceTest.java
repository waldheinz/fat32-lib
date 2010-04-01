
package com.meetwise.fs.fat;

import com.meetwise.fs.FileSystemFactory;
import com.meetwise.fs.FsDirectory;
import com.meetwise.fs.FsDirectoryEntry;
import com.meetwise.fs.util.RamDisk;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
public class ConformanceTest {

    private RamDisk d;
    private FsDirectory root;
    private FatFileSystem fs;
    
    @Before
    public void setUp() throws Exception {
        d = new RamDisk(2 * 1024 * 1024);
        fs = SuperFloppyFormatter.get(d).format();
        root = fs.getRoot();
    }
    
    @Test(expected=IOException.class)
    public void testWrongSectorsPerCluster() throws Exception {
        System.out.println("wrongSectorsPerCluster");
        
        root = null;
        fs.close();
        BootSector bs = BootSector.read(d);
        
        assertEquals(2, bs.getSectorsPerCluster());
        
        bs.setSectorsPerCluster(1);
        bs.write();
        FileSystemFactory.create(d, true);
        
        fail("file system should not have been created");
    }
    
    @Test
    public void testAcceptedFileNames() throws Exception {
        System.out.println("acceptedFileNames");
        
        root.addFile("jdom-1.0.jar");
        final FsDirectoryEntry dir = root.addDirectory("testDir.test");
        dir.getDirectory().addFile("this.should.work.toooo");
        fs.flush();
    }

    @Test
    public void testMaxRootEntries() throws Exception {
        System.out.println("testMaxRootEntries");
        
        /* divide by 2 because we use LFNs which take entries, too */
        final int max = fs.getBootSector().getRootDirEntryCount() / 2;

        System.out.println("max=" + fs.getBootSector().getRootDirEntryCount());

        assertEquals(FatType.FAT12, fs.getFatType());

        int i=0;
        for (; i < max; i++) {
            root.addFile("f-" + i);
        }
        
        try {
            root.addFile("fails");
            fail("added too many files to root directory: " + ++i);
        } catch (DirectoryFullException ex) {
            /* fine */
        }
    }
    
}
