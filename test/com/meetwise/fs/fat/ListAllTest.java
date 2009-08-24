
package com.meetwise.fs.fat;

import com.meetwise.fs.FSDirectory;
import com.meetwise.fs.FSEntry;
import com.meetwise.fs.RamDisk;
import java.io.IOException;
import java.io.InputStream;
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
        final InputStream is = getClass().getResourceAsStream(
                "/data/complex.img.gz");

        final RamDisk rd = RamDisk.readGzipped(is);
        final FatFileSystem fs = new FatFileSystem(rd, true);
        listDirectories(fs.getRootDir(), "");
    }
    
    private void listDirectories(FSDirectory dir, String ident) throws IOException {

        final Iterator<FSEntry> i = dir.iterator();
        
        while (i.hasNext()) {
            final FSEntry e = i.next();
            
            if (e.isDirectory()) {
                System.out.println(ident + "- " + e.getName());
                listDirectories(e.getDirectory(), ident + "   ");
            } else {
                System.out.println(ident + "+ " + e.getName());
            }
        }
    }
}
