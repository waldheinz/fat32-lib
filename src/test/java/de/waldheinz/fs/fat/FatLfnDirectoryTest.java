/*
 * Copyright (C) 2009,2010 Matthias Treydte <mt@waldheinz.de>
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc.,
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */

package de.waldheinz.fs.fat;

import de.waldheinz.fs.BlockDevice;
import de.waldheinz.fs.FsDirectory;
import de.waldheinz.fs.FsDirectoryEntry;
import de.waldheinz.fs.FsFile;
import de.waldheinz.fs.fat.FatLfnDirectory.LfnEntry;
import de.waldheinz.fs.util.RamDisk;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import org.junit.Before;
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
        SuperFloppyFormatter.get(dev).format().close();
        
        this.bs = BootSector.read(dev);
        this.rootDirStore = Fat16RootDirectory.read(
                (Fat16BootSector) bs, false);
        this.fat = Fat.read(bs, 0);
        this.dir = new FatLfnDirectory(rootDirStore, fat);
    }

    @Test
    public void testRemoveDotEntries() throws IOException {
        System.out.println("removeDotEntries");

        final LfnEntry subEntry = dir.addDirectory("test");
        final FatLfnDirectory subDir =
                (FatLfnDirectory) subEntry.getDirectory();


        try {
            subDir.remove(".");
            fail("we just removed the \".\" entry");
        } catch (IllegalArgumentException ex) {
            /* fine */
        }
    }

    @Test
    public void testDotEntriesLfn() throws IOException {
        System.out.println("dotEntriesLfn");

        final LfnEntry subEntry = dir.addDirectory("test");
        final FatLfnDirectory subDir =
                (FatLfnDirectory) subEntry.getDirectory();
        
        LfnEntry entry = (LfnEntry) subDir.getEntry(".");
        System.out.println(entry);
        /* dot entries should not have a LFN */
        assertEquals(1, entry.compactForm().length);

        entry = (LfnEntry) subDir.getEntry("..");
        System.out.println(entry);
        /* dot entries should not have a LFN */
        assertEquals(1, entry.compactForm().length);

    }

    @Test(expected=IOException.class)
    public void testOnlyDotsDirectory() throws IOException {
        System.out.println("onlyDotsDirectory");

        dir.addDirectory("....");
    }
    
    @Test(expected=IOException.class)
    public void testOnlyDotsFile() throws IOException {
        System.out.println("onlyDotsFile");

        dir.addFile("....");
    }

    @Test
    public void testTrailingSpaces() throws IOException {
        System.out.println("trailingSpaces");
        
        dir.addDirectory("testDirectory ");
        assertNotNull(dir.getEntry("testDirectory"));

        dir.addDirectory("anotherDirectory");
        assertNotNull(dir.getEntry("anotherDirectory "));

        dir.addFile("testFile ");
        assertNotNull(dir.getEntry("testFile"));

        dir.addFile("anotherFile");
        assertNotNull(dir.getEntry("anotherFile "));
    }
    
    @Test
    public void testLeadingSpaces() throws IOException {
        System.out.println("leadingSpaces");

        dir.addDirectory(" testDirectory");
        assertNotNull(dir.getEntry("testDirectory"));

        dir.addDirectory("anotherDirectory");
        assertNotNull(dir.getEntry(" anotherDirectory"));

        dir.addFile(" testFile");
        assertNotNull(dir.getEntry("testFile"));

        dir.addFile("anotherFile");
        assertNotNull(dir.getEntry(" anotherFile"));
    }
    
    @Test
    public void testWriteSubdirFile() throws IOException {
        System.out.println("writeSubdirFile");

        System.out.println("cluster size: " + bs.getBytesPerCluster());
        final int freeBefore = fat.getFreeClusterCount();
        System.out.println("free clusters before: " +
                freeBefore);
        
        final LfnEntry subDir = dir.addDirectory("Directory");
        final FsDirectoryEntry fe = subDir.getDirectory().addFile("A File");
        final FsFile f = fe.getFile();
        f.write(0, ByteBuffer.allocate(516));

        System.out.println("free clusters after: " +
                fat.getFreeClusterCount());
        
        assertEquals(freeBefore - 3, fat.getFreeClusterCount());
    }
    
    @Test
    public void testRemoveFile() throws IOException {
        System.out.println("remove (file)");

        final int freeBefore = fat.getFreeClusterCount();
        final int entriesBefore = rootDirStore.getEntryCount();

        final String dirName = "testDirectory";
        final LfnEntry fileEntry = dir.addFile(dirName);
        fileEntry.getFile().setLength(100000);
        assertTrue(fat.getFreeClusterCount() < freeBefore);
        assertEquals(entriesBefore + 2, rootDirStore.getEntryCount());
        assertNotNull(dir.getEntry(dirName));
        
        dir.remove(dirName);
        assertEquals(freeBefore, fat.getFreeClusterCount());
        assertEquals(entriesBefore, rootDirStore.getEntryCount());
        assertNull(dir.getEntry(dirName));
    }

    @Test
    public void testRemoveDirectory() throws IOException {
        System.out.println("remove (directory)");

        final int freeBefore = fat.getFreeClusterCount();
        final int entriesBefore = rootDirStore.getEntryCount();
        
        final String dirName = "testDirectory";
        dir.addDirectory(dirName);
        assertEquals(freeBefore - 1, fat.getFreeClusterCount());
        assertEquals(entriesBefore + 2, rootDirStore.getEntryCount());
        assertNotNull(dir.getEntry(dirName));
        
        dir.remove(dirName);
        assertEquals(freeBefore, fat.getFreeClusterCount());
        assertEquals(entriesBefore, rootDirStore.getEntryCount());
        assertNull(dir.getEntry(dirName));
    }
    
    @Test
    public void testSubDirectoryTimeStamps() throws IOException {
        System.out.println("subDirectoryTimeStamps");
        
        final LfnEntry subDirEntry = dir.addDirectory("testDir");
        assertTrue(subDirEntry.isDirectory());
        
        final FsDirectory subDir = subDirEntry.getDirectory();
        final FsDirectoryEntry dot = subDir.getEntry(".");
        
        assertNotNull(dot);
        assertEquals(
                new Date(subDirEntry.getCreated()),
                new Date(dot.getCreated()));
        assertEquals(subDirEntry.getLastModified(), dot.getLastModified());
        assertEquals(subDirEntry.getLastAccessed(), dot.getLastAccessed());
    }

    @Test
    public void testAddTooManyDirectories() throws IOException {
        System.out.println("addTooManyDirectories");

        int count = 0;
        
        do {
            int freeBeforeAdd = fat.getFreeClusterCount();
            try {
                dir.addDirectory("this is test directory with index " + count);
            } catch (DirectoryFullException ex) {
                assertEquals(freeBeforeAdd, fat.getFreeClusterCount());
                return;
            }
        } while (true);
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
    public void testGetStorageDirectory() {
        System.out.println("getStorageDirectory");
        
        assertEquals(rootDirStore, dir.getStorageDirectory());
    }
    
    @Test
    public void testAddFile() throws Exception {
        System.out.println("addFile");
        
        assertNotNull(dir.addFile("A good file"));
    }
    
    @Test
    public void testAddDirectory() throws Exception {
        System.out.println("addDirectory");
        
        final String name = "A nice directory";
        final LfnEntry newDir = dir.addDirectory(name);
        assertNotNull(newDir);
        assertTrue(newDir == dir.getEntry(name));
        assertTrue(newDir.getDirectory() == dir.getEntry(name).getDirectory());
    }
    
    @Test
    public void testGetEntry() throws IOException {
        System.out.println("getEntry");
        
        final String NAME = "A fine File";
        final LfnEntry file = dir.addFile(NAME);
        assertEquals(file, dir.getEntry(NAME));
    }
    
    @Test
    public void testFlush() throws Exception {
        System.out.println("flush");
        
        dir.addFile("The perfect File");

        dir.flush();
    }
    
}
