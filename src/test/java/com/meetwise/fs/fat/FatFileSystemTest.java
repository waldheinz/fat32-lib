
package com.meetwise.fs.fat;

import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import com.meetwise.fs.FSDirectory;
import com.meetwise.fs.FSDirectoryEntry;
import com.meetwise.fs.FSFile;
import com.meetwise.fs.util.RamDisk;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class FatFileSystemTest {
    
    @Test
    public void testFat12Read() throws Exception {
        System.out.println("testFat12Read");

        final InputStream is = getClass().getResourceAsStream(
                "/fat12-test.img.gz");

        final RamDisk rd = RamDisk.readGzipped(is);
        final FatFileSystem fatFs = new FatFileSystem(rd, false);
        assertEquals(2048, fatFs.getClusterSize());

        final BootSector bs = fatFs.getBootSector();
        assertEquals("mkdosfs", bs.getOemName());
        assertEquals(512, bs.getBytesPerSector());
        assertEquals(FatType.FAT12, bs.getFatType());
        assertEquals(4, bs.getSectorsPerCluster());
        assertEquals(1, bs.getNrReservedSectors());
        assertEquals(2, bs.getNrFats());
        assertEquals(512, bs.getRootDirEntryCount());
        assertEquals(2048, bs.getSectorCount());
        assertEquals(0xf8, bs.getMediumDescriptor());
        assertEquals(2, bs.getSectorsPerFat());
        assertEquals(32, bs.getSectorsPerTrack());
        assertEquals(64, bs.getNrHeads());
        assertEquals(0, bs.getNrHiddenSectors());
        assertEquals(512, FatUtils.getFatOffset(bs, 0));
        assertEquals(1536, FatUtils.getFatOffset(bs, 1));
        assertEquals(2560, FatUtils.getRootDirOffset(bs));

        final FSDirectory fatRootDir = fatFs.getRoot();

        Iterator<FSDirectoryEntry> i = fatRootDir.iterator();
        assertTrue (i.hasNext());

        while (i.hasNext()) {
            final FSDirectoryEntry e = i.next();
            System.out.println("     - " + e);
        }
    }

    /**
     * $ cat fat16-test.img.gz | gunzip | hexdump -C
     *
     * @throws Exception
     */
    @Test
    @Ignore
    public void testFat16Read() throws Exception {
        System.out.println("testFat16Read");

        final InputStream is = getClass().getResourceAsStream(
                "/fat16-test.img.gz");
        
        final RamDisk rd = RamDisk.readGzipped(is);
        final FatFileSystem fatFs = new FatFileSystem(rd, false);
        assertEquals(2048, fatFs.getClusterSize());
        
        final BootSector bs = fatFs.getBootSector();
        assertEquals("mkdosfs", bs.getOemName());
        assertEquals(512, bs.getBytesPerSector());
        assertEquals(FatType.FAT16, bs.getFatType());
        assertEquals(4, bs.getSectorsPerCluster());
        assertEquals(1, bs.getNrReservedSectors());
        assertEquals(2, bs.getNrFats());
        assertEquals(512, bs.getRootDirEntryCount());
        assertEquals(20000, bs.getSectorCount());
        assertEquals(0xf8, bs.getMediumDescriptor());
        assertEquals(20, bs.getSectorsPerFat());
        assertEquals(32, bs.getSectorsPerTrack());
        assertEquals(64, bs.getNrHeads());
        assertEquals(0, bs.getNrHiddenSectors());
        assertEquals(0x200, FatUtils.getFatOffset(bs, 0));
        assertEquals(0x2a00, FatUtils.getFatOffset(bs, 1));
        assertEquals(0x5200, FatUtils.getRootDirOffset(bs));
        
        final FSDirectory fatRootDir = fatFs.getRoot();
        
        FSDirectoryEntry entry = fatRootDir.getEntry("testFile");
        assertTrue(entry.isFile());
        assertFalse(entry.isDirectory());
        assertEquals(1250906972000l, entry.getCreated());
        assertEquals(1250906972000l, entry.getLastModified());
        assertEquals(1250899200000l, entry.getLastAccessed());
       
        FSFile file = entry.getFile();
        assertEquals(8, file.getLength());
        
        final FSDirectory rootDir = fatFs.getRoot();
        System.out.println("   rootDir = " + rootDir);

        Iterator<FSDirectoryEntry> i = rootDir.iterator();
        assertTrue (i.hasNext());
        
        while (i.hasNext()) {
            final FSDirectoryEntry e = i.next();
            System.out.println("     - " + e);
        }

        entry = rootDir.getEntry("TESTDIR");
        System.out.println("   testEnt = " + entry);
        assertTrue(entry.isDirectory());
        assertFalse(entry.isFile());

        final FSDirectory testDir = entry.getDirectory();
        System.out.println("   testDir = " + testDir);
        
        i = testDir.iterator();
        
        while (i.hasNext()) {
            final FSDirectoryEntry e = i.next();
            System.out.println("     - " + e);
        }
        
    }

    @Test
    public void testFat32Read() throws Exception {
        System.out.println("fat32Read");
        
        final InputStream is = getClass().getResourceAsStream(
                "/fat32-test.img.gz");

        final RamDisk rd = RamDisk.readGzipped(is);
        final FatFileSystem fatFs = new FatFileSystem(rd, false);
        assertEquals(512, fatFs.getClusterSize());

        final BootSector bs = fatFs.getBootSector();
        assertEquals(FatType.FAT32, bs.getFatType());
        assertEquals("mkdosfs", bs.getOemName());
        assertEquals(512, bs.getBytesPerSector());
        assertEquals(1, bs.getSectorsPerCluster());
        assertEquals(32, bs.getNrReservedSectors());
        assertEquals(2, bs.getNrFats());
        assertEquals(0, bs.getRootDirEntryCount());
        assertEquals(80000, bs.getSectorCount());
        assertEquals(0xf8, bs.getMediumDescriptor());
        assertEquals(616, bs.getSectorsPerFat());
        assertEquals(32, bs.getSectorsPerTrack());
        assertEquals(64, bs.getNrHeads());
        assertEquals(0, bs.getNrHiddenSectors());
        assertEquals(16384, FatUtils.getFatOffset(bs, 0));
        assertEquals(16384 + bs.getSectorsPerFat() * bs.getBytesPerSector(),
                FatUtils.getFatOffset(bs, 1));
        
        final FSDirectory rootDir = fatFs.getRoot();
        System.out.println("   rootDir = " + rootDir);
        
        Iterator<FSDirectoryEntry> i = rootDir.iterator();
        assertTrue(i.hasNext());
        
        while (i.hasNext()) {
            final FSDirectoryEntry e = i.next();
            System.out.println("     - " + e);
        }

        FSDirectoryEntry e = rootDir.getEntry("Langer Verzeichnisname");
        assertTrue(e.isDirectory());
        assertFalse(e.isFile());

        final FSDirectory dir = e.getDirectory();
        i = dir.iterator();
        assertTrue(i.hasNext());
        
        while (i.hasNext()) {
            e = i.next();
            System.out.println("     - " + e);
        }
    }
    
    @Test
    @Ignore
    public void testFat32Write() throws Exception {
        System.out.println("testFat32Write");

        final InputStream is = getClass().getResourceAsStream(
                "/fat32-test.img.gz");

        final RamDisk rd = RamDisk.readGzipped(is);
        FatFileSystem fatFs = new FatFileSystem(rd, false);
        FSDirectory rootDir = fatFs.getRoot();

        for (int i=0; i < 1024; i++) {
            final FSDirectoryEntry e = rootDir.addFile("f-" + i);
            assertTrue(e.isFile());
            assertFalse(e.isDirectory());
            final FSFile f = e.getFile();
            
            f.write(0, ByteBuffer.wrap(("this is file # " + i).getBytes()));
        }

        fatFs.close();

        fatFs = new FatFileSystem(rd, false);
        rootDir = fatFs.getRoot();
        
        for (int i=0; i < 1024; i++) {
            assertNotNull(rootDir.getEntry("f-" + i));
        }
    }

    public static void main(String[] args) throws Exception {
        FatFileSystemTest test = new FatFileSystemTest();
        test.testFat32Write();
    }
}