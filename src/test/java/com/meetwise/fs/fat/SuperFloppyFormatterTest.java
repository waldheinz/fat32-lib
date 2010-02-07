
package com.meetwise.fs.fat;

import com.meetwise.fs.BlockDevice;
import com.meetwise.fs.FileSystem;
import com.meetwise.fs.FileSystemFactory;
import com.meetwise.fs.util.FileDisk;
import com.meetwise.fs.util.RamDisk;
import java.io.File;
import java.io.IOException;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
public class SuperFloppyFormatterTest {

    public static void main(String[] args) throws IOException {
        final File file = new File("/tmp/testdisk.img");
        FileDisk d = FileDisk.create(file, 40960000);
        SuperFloppyFormatter f = new SuperFloppyFormatter(d);
        f.setFatType(FatType.FAT32);
//        f.setVolumeLabel("testdisk");
        f.format();
        d.close();
        
        d = new FileDisk(new File("/tmp/fat32-test.img"), false);
        final FileSystem fs = FileSystemFactory.create(d, false);
        fs.getRoot().addFile("this is another looooong name");
        fs.close();
        d.close();
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testFat12FormatInvalid() throws IOException {
        System.out.println("fat12Format (invalid)");

        BlockDevice dev = new RamDisk(16800000);
        SuperFloppyFormatter f = new SuperFloppyFormatter(dev);
        f.setFatType(FatType.FAT12);
        f.format();
    }
    
    @Test
    public void testFat12FormatValid() throws IOException {
        System.out.println("fat12Format (valid)");

        BlockDevice dev = new RamDisk(16700000);
        SuperFloppyFormatter f = new SuperFloppyFormatter(dev);
        f.setFatType(FatType.FAT12);
        f.format();
        
        FatFileSystem fs = new FatFileSystem(dev, false);
        final BootSector bs = fs.getBootSector();
        System.out.println("sectors=" + bs.getNrTotalSectors());
        System.out.println("sectors per cluster=" + bs.getSectorsPerCluster());
        System.out.println("clusters=" +
                bs.getNrTotalSectors() / bs.getSectorsPerCluster());
        assertEquals(FatType.FAT12, fs.getFatType());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testFat16FormatTooSmall() throws IOException {
        System.out.println("fat16Format (too small)");

        BlockDevice dev = new RamDisk(4 * 1024 * 1024);
        SuperFloppyFormatter f = new SuperFloppyFormatter(dev);
        f.setFatType(FatType.FAT16);
    }

    @Test
    public void testFat16FormatValid() throws IOException {
        System.out.println("fat16Format (valid)");
        
        BlockDevice dev = new RamDisk(16 * 1024 * 1024);
        SuperFloppyFormatter f = new SuperFloppyFormatter(dev);
        f.setFatType(FatType.FAT16);
    }

    @Test(expected=IllegalArgumentException.class) @Ignore
    public void testFat16FormatTooBig() throws IOException {
        System.out.println("fat16Format (too big)");
        final File file = File.createTempFile("fat32-test", ".img");
        
        try {
            BlockDevice dev = FileDisk.create(file, 3 * 1024 * 1024 * 1024);
            SuperFloppyFormatter f = new SuperFloppyFormatter(dev);
            f.setFatType(FatType.FAT16);
        } finally {
            file.delete();
        }
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testFat32FormatInvalid() throws IOException {
        System.out.println("fat32Format (invalid)");

        BlockDevice dev = new RamDisk(16 * 1024 * 1024);
        SuperFloppyFormatter f = new SuperFloppyFormatter(dev);
        f.setFatType(FatType.FAT32);
    }
    
    @Test
    public void testFat32FormatValid() throws IOException {
        System.out.println("fat32Format (valid)");

        BlockDevice dev = new RamDisk(50 * 1024 * 1024);
        SuperFloppyFormatter f = new SuperFloppyFormatter(dev);
        f.setFatType(FatType.FAT32);
        f.format();
        
        FatFileSystem fs = new FatFileSystem(dev, false);
        assertEquals(FatType.FAT32, fs.getFatType());
        fs.getRoot().addFile("this is another looooong name");
        fs.getRoot().addDirectory("this is a sub-directory!!!");
        fs.close();
    }
    
    @Test
    public void testAutoFormat() throws IOException {
        System.out.println("autoFormat");
        
        RamDisk rd = new RamDisk(1024 * 1024);
        SuperFloppyFormatter f = new SuperFloppyFormatter(rd);
        f.format();

        FatFileSystem fs = new FatFileSystem(rd, false);
        assertNotNull(fs.getRoot());
    }
}
