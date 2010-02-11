
package com.meetwise.fs.fat;

import com.meetwise.fs.FsDirectory;
import com.meetwise.fs.FsDirectoryEntry;
import com.meetwise.fs.FsFile;
import com.meetwise.fs.util.RamDisk;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.Iterator;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class ListAllTest {
    
    @Test
    public void testListComplex() throws Exception {
        System.out.println("testListComplex");

        final InputStream is = getClass().getResourceAsStream(
                "/complex.img.gz");

        final RamDisk rd = RamDisk.readGzipped(is);
        final FatFileSystem fs = new FatFileSystem(rd, true);
        listDirectories(fs.getRoot(), "");
    }
    
    private void listDirectories(FsDirectory dir, String ident) throws IOException {

        final Iterator<FsDirectoryEntry> i = dir.iterator();
        
        while (i.hasNext()) {
            final FsDirectoryEntry e = i.next();
            
            if (e.isDirectory()) {
                if (e.getName().equals(".") || e.getName().equals(".."))
                    continue;
                
                System.out.println(ident + "- " + e.getName());
                listDirectories(e.getDirectory(), ident + "   ");
            } else {
                checkFile(e, ident);
            }
        }
    }
    
    private void checkFile(FsDirectoryEntry fe, String ident) throws IOException {
        System.out.print(ident + " + " + fe.getName());
        final FsFile f = fe.getFile();
        
        System.out.println(" [size=" + f.getLength() + "]");

        final ByteBuffer bb = ByteBuffer.allocate((int) f.getLength());
        f.read(0, bb);
    }
    
    public static void main(String[] args) throws Exception {
        ListAllTest lat = new ListAllTest();
        
        lat.testListComplex();
    }
}
