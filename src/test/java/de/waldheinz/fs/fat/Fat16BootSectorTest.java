
package de.waldheinz.fs.fat;

import de.waldheinz.fs.fat.Fat16BootSector;
import de.waldheinz.fs.BlockDevice;
import de.waldheinz.fs.util.RamDisk;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class Fat16BootSectorTest {

    private BlockDevice dev;
    private Fat16BootSector bs;

    @Before
    public void setUp() {
        this.dev = new RamDisk(1024);
        this.bs = new Fat16BootSector(dev);
    }
    
    @Test
    public void testGetVolumeLabel() {
        System.out.println("getVolumeLabel");

        bs.init();
        assertEquals(Fat16BootSector.DEFAULT_VOLUME_LABEL, bs.getVolumeLabel());
    }
    
    @Test
    public void testSetVolumeLabelValid() {
        System.out.println("setVolumeLabel (valid)");
        
        final String label = "01234567890";
        bs.setVolumeLabel(label);
        assertEquals(label, bs.getVolumeLabel());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSetVolumeLabelTooLong() {
        System.out.println("setVolumeLabel (too long)");

        bs.setVolumeLabel("012345678901");
    }
    
    @Test
    public void testInit() {
        System.out.println("init");

        bs.init();
        
        assertEquals(
                Fat16BootSector.DEFAULT_VOLUME_LABEL,
                bs.getVolumeLabel());
        
        assertEquals(
                Fat16BootSector.DEFAULT_ROOT_DIR_ENTRY_COUNT,
                bs.getRootDirEntryCount());
    }

}
