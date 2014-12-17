package de.waldheinz.fs.fat;

import org.junit.Assert;
import org.junit.Test;

import java.io.InputStream;

import de.waldheinz.fs.util.RamDisk;

/**
 * @author Jan Seeger <jan@alphadev.net>
 */
public class FileSystemSizeTest {
    @Test
    public void testFat32Size() throws Exception {
        System.out.println("fat32Size");

        final InputStream is = getClass().getResourceAsStream(
                "fat32-test.img.gz");

        final RamDisk rd = RamDisk.readGzipped(is);
        final FatFileSystem fatFs = new FatFileSystem(rd, false);
        Assert.assertEquals(39366 * 1024, fatFs.getFreeSpace());
        Assert.assertEquals(39368 * 1024, fatFs.getUsableSpace());
        Assert.assertEquals(40000 * 1024, fatFs.getTotalSpace());
    }

    @Test
    public void testFat12Size() throws Exception {
        System.out.println("fat12Size");

        final InputStream is = getClass().getResourceAsStream(
                "fat12-test.img.gz");

        final RamDisk rd = RamDisk.readGzipped(is);
        final FatFileSystem fatFs = new FatFileSystem(rd, false);
        Assert.assertEquals(998 * 1024, fatFs.getFreeSpace());
        Assert.assertEquals(1004 * 1024, fatFs.getUsableSpace());
        Assert.assertEquals(-1, fatFs.getTotalSpace());
    }

    @Test
    public void testFat16Size() throws Exception {
        System.out.println("fat16Size");

        final InputStream is = getClass().getResourceAsStream(
                "fat16-test.img.gz");

        final RamDisk rd = RamDisk.readGzipped(is);
        final FatFileSystem fatFs = new FatFileSystem(rd, false);
        Assert.assertEquals(9956 * 1024, fatFs.getFreeSpace());
        Assert.assertEquals(9962 * 1024, fatFs.getUsableSpace());
        Assert.assertEquals(-1, fatFs.getTotalSpace());
    }
}
