
package com.meetwise.fs.fat;

import com.meetwise.fs.util.FileDisk;
import com.meetwise.fs.util.RamDisk;
import java.io.File;
import java.io.IOException;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
public class SuperFloppyFormatterTest {

    public static void main(String[] args) throws IOException {
        final FileDisk d = FileDisk.create(new File("/tmp/testdisk.img"), 40960000);
        SuperFloppyFormatter f = new SuperFloppyFormatter(d);
        f.setFatType(FatType.FAT32);
        f.setVolumeLabel("testdisk");
        f.format();
        d.close();
    }

    @Test
    public void testFat32Format() throws IOException {
        System.out.println("fat32Format");

        RamDisk rd = new RamDisk(50 * 1024 * 1024);
        SuperFloppyFormatter f = new SuperFloppyFormatter(rd);
        f.setFatType(FatType.FAT32);
        f.setVolumeLabel("test");
        f.format();

        FatFileSystem fs = new FatFileSystem(rd, false);
        assertEquals(FatType.FAT32, fs.getFatType());
        assertEquals("test", fs.getVolumeLabel());
    }

    @Test
    public void testAutoFormat() throws IOException {
        System.out.println("autoFormat");
        
        RamDisk rd = new RamDisk(1024 * 1024);
        SuperFloppyFormatter f = new SuperFloppyFormatter(rd);
        f.format();
    }
}
