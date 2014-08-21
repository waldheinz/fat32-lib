/*
 * Copyright (C) 2009-2013 Matthias Treydte <mt@waldheinz.de>
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

import static org.junit.Assert.*;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import org.junit.Before;
import org.junit.Test;
import de.waldheinz.fs.BlockDevice;
import de.waldheinz.fs.FsDirectory;
import de.waldheinz.fs.FsDirectoryEntry;
import de.waldheinz.fs.FsFile;
import de.waldheinz.fs.util.RamDisk;

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
        this.dir = new FatLfnDirectory(rootDirStore, fat, false);
    }
    
    @Test
    public void testRenameEntryClash() throws IOException {
        System.out.println("renameEntryClash");

        final String name = "An Entry for Testing";
        
        dir.addDirectory(name);
        final FatLfnDirectoryEntry f = dir.addFile("A Name to change");
        
        try {
            f.setName(name);
            fail("file should not have been renamed");
        } catch (IOException ex) {
            System.out.println("cought " + ex);
            assertNotNull("file was lost", dir.getEntry("A Name to change"));
        }
    }
    
    @Test
    public void testRenameEntry() throws IOException {
        System.out.println("renameEntry");
        
        FatLfnDirectoryEntry f = dir.addFile("oldFileName");
        f.setName("newFileName");
        
        assertNull("entry still in place",
                dir.getEntry("oldFileName"));
        assertNotNull("not in place with new name",
                dir.getEntry("newFileName"));
        assertEquals("newFileName", f.getName());
    }

    @Test
    public void testUsedNamesCleanupWithShortnames() throws IOException {
        System.out.println("testUsedNamesCleanupWithShortnames");
        
        FatLfnDirectoryEntry f = dir.addFile("test-with-shortname.txt");
        f.setName("newFileName");
        f.setName("test-with-shortname.txt");
        
        HashSet<String> usedNames = new HashSet<String> ();
        ShortNameGenerator shortNameGenerator = new ShortNameGenerator (usedNames);
        ShortName shortName = shortNameGenerator.generateShortName("test-with-shortname.txt");
        usedNames.add(shortName.asSimpleString().toLowerCase(Locale.ROOT));
        ShortName shortName2 = shortNameGenerator.generateShortName("test-with-shortname.txt");

        assertNotNull("entry has not correct shortname",
                dir.getEntry(shortName.asSimpleString()));
        assertNull("entry has not correct shortname",
                dir.getEntry(shortName2.asSimpleString()));

        FatLfnDirectoryEntry f2 = dir.addFile("test-with-shortname1.txt");
        assertEquals("entry has not correct shortname",f2,
                dir.getEntry(shortName2.asSimpleString()));
    }
    
    @Test
    public void testCaseSensitiveDirectory() throws IOException {
        System.out.println("caseSensitiveDirectory");

        dir.addDirectory("hateCamelCase.BAK");
        assertNotNull(dir.getEntry("hatecamelcase.bak"));
    }
    
    @Test
    public void testCaseSensitiveFile() throws IOException {
        System.out.println("caseSensitiveFile");

        dir.addFile("loveCamelCase");
        assertNotNull(dir.getEntry("LOVECAMELCASE"));
    }
    
    @Test(expected=IOException.class)
    public void testUniqueShortNameFileClash() throws IOException {
        System.out.println("uniqueShortNameFileClash");

        final FatLfnDirectoryEntry de = dir.addFile("a testing File");
        dir.addDirectory(de.realEntry.getShortName().asSimpleString());
    }
    
    @Test(expected=IOException.class)
    public void testUniqueShortNameDirClash() throws IOException {
        System.out.println("uniqueShortNameDirClash");

        final FatLfnDirectoryEntry de = dir.addDirectory("testDirectory");
        dir.addFile(de.realEntry.getShortName().asSimpleString());
    }
    
    @Test
    public void testReuseUniqueEntry() throws IOException {
        System.out.println("reuseUniqueEntry");

        dir.addFile("testFile");
        dir.remove("testFile");
        dir.addFile("testFile");
    }

    @Test(expected=IOException.class)
    public void testUniqueFileName() throws IOException {
        System.out.println("uniqueFileName");

        dir.addFile("testFile");
        dir.addFile("testFile");
    }
    
    @Test(expected=IOException.class)
    public void testUniqueDirName() throws IOException {
        System.out.println("uniqueDirName");

        dir.addDirectory("testDir");
        dir.addDirectory("testDir");
    }

    @Test
    public void testArchiveFlag() throws IOException {
        System.out.println("archiveFlag");

        final FatLfnDirectoryEntry f = dir.addDirectory("testDir");
        assertFalse(f.isArchiveFlag());

        f.setArchiveFlag(true);
        assertTrue(f.isArchiveFlag());
        assertTrue(f.isDirectory());

        f.setArchiveFlag(false);
        assertFalse(f.isArchiveFlag());
        assertTrue(f.isDirectory());
    }

    @Test
    public void testReadOnlyFlag() throws IOException {
        System.out.println("readOnlyFlag");

        final FatLfnDirectoryEntry f = dir.addDirectory("testDir");
        assertFalse(f.isReadOnlyFlag());

        f.setReadOnlyFlag(true);
        assertTrue(f.isReadOnlyFlag());
        assertTrue(f.isDirectory());

        f.setReadOnlyFlag(false);
        assertFalse(f.isReadOnlyFlag());
        assertTrue(f.isDirectory());
    }

    @Test
    public void testSystemFlag() throws IOException {
        System.out.println("systemFlag");

        final FatLfnDirectoryEntry f = dir.addDirectory("testDir");
        assertFalse(f.isSystemFlag());

        f.setSystemFlag(true);
        assertTrue(f.isSystemFlag());
        assertTrue(f.isDirectory());

        f.setSystemFlag(false);
        assertFalse(f.isSystemFlag());
        assertTrue(f.isDirectory());
    }
    
    @Test
    public void testHiddenFlag() throws IOException {
        System.out.println("hiddenFlag");
        
        final FatLfnDirectoryEntry f = dir.addFile("testFile");
        assertFalse(f.isHiddenFlag());

        f.setHiddenFlag(true);
        assertTrue(f.isHiddenFlag());
        assertTrue(f.isFile());

        f.setHiddenFlag(false);
        assertFalse(f.isHiddenFlag());
        assertTrue(f.isFile());
    }
    
    @Test
    public void testRemoveDotEntries() throws IOException {
        System.out.println("removeDotEntries");

        final FatLfnDirectoryEntry subEntry = dir.addDirectory("test");
        final FatLfnDirectory subDir =
                (FatLfnDirectory) subEntry.getDirectory();

        assertNotNull(subDir.getEntry("."));

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

        final FatLfnDirectoryEntry subEntry = dir.addDirectory("test");
        final FatLfnDirectory subDir =
                (FatLfnDirectory) subEntry.getDirectory();
        
        FatLfnDirectoryEntry entry = subDir.getEntry(".");
        assertNotNull(entry);
        
        System.out.println(entry);
        /* dot entries should not have a LFN */
        assertEquals(1, entry.compactForm().length);

        entry = subDir.getEntry("..");
        assertNotNull(entry);
        
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
        
        final FatLfnDirectoryEntry subDir = dir.addDirectory("Directory");
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
        final FatLfnDirectoryEntry fileEntry = dir.addFile(dirName);
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
        
        final FatLfnDirectoryEntry subDirEntry = dir.addDirectory("testDir");
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
            final int freeBeforeAdd = fat.getFreeClusterCount();
            
            try {
                dir.addDirectory("this is test directory with index " +
                        count++);
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
    public void testAddFile() throws Exception {
        System.out.println("addFile");

        final String name = "A good file";
        final FatLfnDirectoryEntry f = dir.addFile(name);
        
        assertNotNull(f);
        assertFalse(f.isHiddenFlag());
        assertEquals(name, f.getName());
    }
    
    @Test
    public void testAddDirectory() throws Exception {
        System.out.println("addDirectory");
        
        final String name = "A nice directory";
        final FatLfnDirectoryEntry newDir = dir.addDirectory(name);
        
        assertNotNull(newDir);
        assertFalse(newDir.isHiddenFlag());
        assertTrue(newDir == dir.getEntry(name));
        assertTrue(newDir.getDirectory() == dir.getEntry(name).getDirectory());
    }
    
    @Test
    public void testGetEntry() throws IOException {
        System.out.println("getEntry");
        
        final String NAME = "A fine File";
        final FatLfnDirectoryEntry file = dir.addFile(NAME);
        assertEquals(file, dir.getEntry(NAME));
    }
    
    @Test
    public void testFlush() throws Exception {
        System.out.println("flush");
        
        dir.addFile("The perfect File");

        dir.flush();
    }
    
}
