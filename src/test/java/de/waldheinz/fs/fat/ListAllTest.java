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

import de.waldheinz.fs.FsDirectory;
import de.waldheinz.fs.FsDirectoryEntry;
import de.waldheinz.fs.FsFile;
import de.waldheinz.fs.util.RamDisk;
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
                System.out.println(ident + "- " + e.getName());

                if (e.getName().equals(".") || e.getName().equals(".."))
                    continue;
                
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
