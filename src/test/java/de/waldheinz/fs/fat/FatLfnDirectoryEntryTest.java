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
import de.waldheinz.fs.util.RamDisk;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class FatLfnDirectoryEntryTest {

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
    public void testMoveTo() throws IOException {
        System.out.println("moveTo");
        
        final FatLfnDirectory target =
                dir.addDirectory("target").getDirectory();
        final FatLfnDirectoryEntry file = dir.addFile("a file");
        file.moveTo(target, "the same file");

        assertNull(dir.getEntry("a file"));
        assertNotNull(target.getEntry("the same file"));
    }
    
}
