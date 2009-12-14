
package com.meetwise.fs.fat;

import com.meetwise.fs.FSDirectory;
import com.meetwise.fs.FSDirectoryEntry;
import com.meetwise.fs.util.RamDisk;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
public class ConformanceTest {
    
    private FSDirectory root;
    private FatFileSystem fs;

    public static void main(String[] args) throws Exception {
        final ConformanceTest ct = new ConformanceTest();
        ct.setUp();
        ct.testAcceptedFileNames();
    }
    
    @Before
    public void setUp() throws Exception {
        RamDisk d = new RamDisk(512 * 1024);
        final FatFormatter ff = FatFormatter.superFloppyFormatter(d);
        ff.format(d, null);
        fs = new FatFileSystem(d, false);
        root = fs.getRoot();
    }


    @Test
    public void testAcceptedFileNames() throws Exception {
        System.out.println("acceptedFileNames");
        
        root.addFile("jdom-1.0.jar");
        final FSDirectoryEntry dir = root.addDirectory("testDir.test");
        dir.getDirectory().addFile("this.should.work.toooo");
    }

    @Test
    public void testMaxRootEntries() throws Exception {
        System.out.println("testMaxRootEntries");
        
        /* divide by 2 because we use LFNs which take entries, too */
        final int max = fs.getBootSector().getNrRootDirEntries() / 2;

        for (int i=0; i < max; i++) {
            root.addFile("f-" + i);
        }

        try {
            root.addFile("fails");
            fail("added too many files to root directory");
        } catch (RootDirectoryFullException ex) {
            /* fine */
        }
    }
    
}
