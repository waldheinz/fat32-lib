
package com.meetwise.fs.fat;

import com.meetwise.fs.util.FileDisk;
import java.io.File;
import java.io.IOException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class DosFsckTest {

    private final static String DOSFSCK_CMD = "/sbin/dosfsck";

    private File file;
    private FileDisk dev;

    @Before
    public void setUp() throws IOException {
        this.file = File.createTempFile("fat32-lib-test", ".img");
        this.dev = FileDisk.create(file, 100 * 1024);
    }

    @After
    public void tearDown() throws IOException {
        this.dev.close();
        this.file.delete();
    }

    @Test
    public void testCreateFat32() throws Exception {
        System.out.println("createFat32");

        SuperFloppyFormatter f = new SuperFloppyFormatter(dev);
        f.setFatType(FatType.FAT32);
        f.format();
        
        runFsck();
    }
    
    @Test
    public void testCreateFat16() throws Exception {
        System.out.println("createFat16");

        SuperFloppyFormatter f = new SuperFloppyFormatter(dev);
        f.setFatType(FatType.FAT16);
        f.format();
        
        runFsck();
    }
    
    @Test
    public void testCreateFat12() throws Exception {
        System.out.println("createFat12");

        SuperFloppyFormatter f = new SuperFloppyFormatter(dev);
        f.setFatType(FatType.FAT12);
        f.format();

        runFsck();
    }

    private void runFsck() throws Exception {
        final ProcessBuilder pb = new ProcessBuilder(
                DOSFSCK_CMD, "-n", file.toString());

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
}
