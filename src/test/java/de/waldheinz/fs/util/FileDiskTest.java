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

package de.waldheinz.fs.util;

import de.waldheinz.fs.ReadOnlyException;
import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.Random;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
public class FileDiskTest {

    private final static int SIZE = 1024 * 1024;
    private FileDisk fd;
    private File f;
    
    @Before
    public void setUp() throws Exception {
        f = File.createTempFile("fileDiskTest", ".tmp");
        f.deleteOnExit();
        fd = FileDisk.create(f, SIZE);
    }

    @After
    public void tearDown() throws IOException {
        fd.close();
        f.delete();
    }
    
    @Test(expected=ReadOnlyException.class)
    public void testIsReadOnly() throws IOException {
        System.out.println("isReadOnly");

        assertFalse(fd.isReadOnly());

        fd.close();

        fd = new FileDisk(f, true);

        assertTrue(fd.isReadOnly());

        fd.write(0, ByteBuffer.allocate(1000));
    }

    @Test(expected=IllegalStateException.class)
    public void testClose() throws IOException {
        System.out.println("close");
        
        fd.close();
        fd.getSize();
    }

    @Test(expected=IOException.class)
    public void testReadPastEnd() throws IOException {
        System.out.println("readPastEnd");

        fd.read(SIZE - 999, ByteBuffer.allocate(1000));
    }

    @Test(expected=IOException.class)
    public void testWritePastEnd() throws IOException {
        System.out.println("writePastEnd");
        
        fd.write(SIZE - 999, ByteBuffer.allocate(1000));
    }

    @Test
    public void testPersistence() throws IOException {
        System.out.println("persistence");

        /* write random data */

        final ByteBuffer reference = ByteBuffer.allocate(SIZE);
        final Random rnd = new Random(System.currentTimeMillis());
        rnd.nextBytes(reference.array());
        fd.write(0, reference);
        fd.close();

        /* check if we can read it back in */
        
        fd = new FileDisk(f, true);
        final ByteBuffer read = ByteBuffer.allocate(SIZE);
        fd.read(0, read);

        reference.rewind();
        read.rewind();
        
        assertEquals(reference, read);
    }
    
}
