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

import de.waldheinz.fs.FsDirectoryEntry;
import de.waldheinz.fs.FsFile;
import de.waldheinz.fs.util.FileDisk;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.Assume;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class DosFsckTest {

    private final static String DOSFSCK_CMD = "/usr/sbin/dosfsck";
    private File file;
    private FileDisk dev;

    @Before
    public void setUp() throws Exception {
        Assume.assumeTrue(fsckAvailable());
        
        this.file = File.createTempFile("fat32-lib-test-", ".img");
        this.file.deleteOnExit();
    }
    
    @After
    public void tearDown() throws IOException {
        if (dev != null) {
            this.dev.close();
            this.dev = null;
        }
        
        if (this.file != null) {
            this.file.delete();
            this.file = null;
        }
    }
    
    @Test
    public void testVolumeLabel() throws Exception {
        System.out.println("volumeLabel");

        this.dev = FileDisk.create(file, 128 * 1024 * 1024);
        SuperFloppyFormatter.get(dev).setFatType(FatType.FAT32).
                setVolumeLabel("Cool Vol").format();

        runFsck();
    }

    @Test
    public void testFat32Write() throws Exception {
        System.out.println("fat32Write");

        this.dev = FileDisk.create(file, 128 * 1024 * 1024);
        SuperFloppyFormatter.get(dev).setFatType(FatType.FAT32).format();
        
        FatFileSystem fs = new FatFileSystem(dev, false);
        final FatLfnDirectory rootDir = (FatLfnDirectory) fs.getRoot();

        FatLfnDirectoryEntry entry = rootDir.addDirectory("Directory");
        
        for (int i = 0; i < 1; i++) {
            final FsDirectoryEntry e = entry.getDirectory().addFile(
                    "This is file number " + i);
            final FsFile fsFile = e.getFile();
            
            byte[] nullBytes = new byte[516];
            ByteBuffer buff = ByteBuffer.wrap(nullBytes);
            buff.rewind();
            fsFile.write(0, buff);
        }
        
        fs.flush();
        fs.close();
        runFsck();
    }

    @Test
    public void testCreateFat32() throws Exception {
        System.out.println("createFat32");

        this.dev = FileDisk.create(file, 128 * 1024 * 1024);
        SuperFloppyFormatter.get(dev).setFatType(FatType.FAT32).format();
        runFsck();
    }

    @Test
    public void testCreateFat16() throws Exception {
        System.out.println("createFat16");

        this.dev = FileDisk.create(file, 16 * 1024 * 1024);
        SuperFloppyFormatter.get(dev).setFatType(FatType.FAT16).format();
        runFsck();
    }

    @Test
    public void testCreateFat12() throws Exception {
        System.out.println("createFat12");

        this.dev = FileDisk.create(file, 2 * 1024 * 1024);
        SuperFloppyFormatter.get(dev).setFatType(FatType.FAT12).format();
        runFsck();
    }
    
    private boolean fsckAvailable() throws Exception {
        System.out.print("checking if dosfsck is available... ");
        
        final boolean result = new File(DOSFSCK_CMD).canExecute();
        
        if (result) {
            System.out.println("ok");
        } else {
            System.out.println("no");
        }
        
        return result;
    }
    
    private void runFsck() throws Exception {
        System.out.println("running fsck on " + file);
        
        final ProcessBuilder pb = new ProcessBuilder(
                DOSFSCK_CMD, "-v", "-n", file.toString());

        pb.redirectErrorStream(true);
        final Process proc = pb.start();

        while (true) {
            final int c = proc.getInputStream().read();
            if (c < 0) {
                break;
            }

            System.out.write(c);
        }

        assertEquals(0, proc.waitFor());
    }
    
    @Test
    public void testLargeFileSystem() throws Exception {
        System.out.println("large FAT32 fs");
        
        dev = FileDisk.create(file, 1024 * 1024 * 1024);
        FatFileSystem fs = SuperFloppyFormatter.get(dev)
                .setFatType(FatType.FAT32)
                .format();
        
        FatLfnDirectory root = fs.getRoot();
        
        for (int i=0; i < 63; i++) {
            final String fname = "file-" + i + ".test";
            System.out.println(fname);
            
            FatLfnDirectoryEntry fe = root.addFile(fname);
            final ByteBuffer bb = ByteBuffer.allocate(1024 * 1024 * 16);
            byte[] array = bb.array();
            
            Arrays.fill(array, (byte) i);
            fe.getFile().write(0, bb);
        }
        
        fs.close();
        
        fs = FatFileSystem.read(dev, true);
        root = fs.getRoot();
        
        for (int i=0; i < 30; i++) {
            FatLfnDirectoryEntry e = root.getEntry("file-" + i + ".test");
            assertTrue(e.isFile());
            FatFile ff = e.getFile();
            assertEquals(ff.getLength(), 16 * 1024 * 1024);
            ByteBuffer bb = ByteBuffer.allocate((int) ff.getLength());
            ff.read(0, bb);
            
            for (int j=0; j < bb.limit(); j++) {
                assertEquals(bb.get(j), i);
            }
        }
        
        runFsck();
    }
    
}
