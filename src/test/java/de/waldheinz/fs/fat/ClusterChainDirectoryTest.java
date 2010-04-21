
package de.waldheinz.fs.fat;

import de.waldheinz.fs.fat.ClusterChainDirectory;
import de.waldheinz.fs.fat.Fat;
import de.waldheinz.fs.fat.AbstractDirectoryEntry;
import de.waldheinz.fs.fat.FatDirEntry;
import de.waldheinz.fs.fat.DirectoryFullException;
import de.waldheinz.fs.fat.Fat32BootSector;
import de.waldheinz.fs.fat.ClusterChain;
import de.waldheinz.fs.util.RamDisk;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class ClusterChainDirectoryTest {

    private RamDisk rd;
    private Fat32BootSector bs;
    private Fat fat;
    private ClusterChain chain;
    private ClusterChainDirectory dir;

    @Before
    public void setUp() throws IOException {
        this.rd = new RamDisk(2048 * 2048);
        this.bs = new Fat32BootSector(rd);
        this.bs.init();
        this.fat = Fat.create(bs, 0);
        this.chain = new ClusterChain(fat, false);
        this.dir = ClusterChainDirectory.createRoot(chain);
    }

    @Test(expected=DirectoryFullException.class)
    public void testMaximumSize() throws IOException {
        System.out.println("maximumSize");
        
        while (true) {
            AbstractDirectoryEntry e = new AbstractDirectoryEntry(dir);
            FatDirEntry fe = FatDirEntry.create(e);
            dir.addEntry(e);
            
            assertTrue(
                    chain.getLengthOnDisk() <= ClusterChainDirectory.MAX_SIZE);
        }
    }

    @Test
    public void testGetStorageCluster() {
        System.out.println("getStorageCluster");

        assertEquals(0, dir.getStorageCluster());
    }

    @Test
    public void testCreate() {
        System.out.println("create");

        assertEquals(
                chain.getLengthOnDisk() / AbstractDirectoryEntry.SIZE,
                dir.getCapacity());
    }
    
}
