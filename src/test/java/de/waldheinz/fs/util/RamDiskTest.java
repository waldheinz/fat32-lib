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

package de.waldheinz.fs.util;

import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class RamDiskTest {
    
    @Test
    public void testReadGzipped() throws IOException {
        System.out.println("testReadGzipped");
        
        final InputStream is = getClass().getResourceAsStream(
                "/de/waldheinz/fs/fat/fat16-test.img.gz");

        final RamDisk rd = RamDisk.readGzipped(is);
        assertEquals(512, rd.getSectorSize());
        assertEquals(10240000, rd.getSize());
    }

    @Test(expected=IllegalStateException.class)
    public void testClose() throws IOException {
        System.out.println("close");
        
        final RamDisk d = new RamDisk(4096);
        d.close();
        d.flush();
    }
    
}
