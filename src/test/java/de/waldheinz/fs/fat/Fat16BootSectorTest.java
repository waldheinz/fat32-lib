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
public class Fat16BootSectorTest {

    private BlockDevice dev;
    private Fat16BootSector bs;

    @Before
    public void setUp() {
        this.dev = new RamDisk(1024);
        this.bs = new Fat16BootSector(dev);
    }
    
    @Test
    public void testGetVolumeLabel() throws IOException {
        System.out.println("getVolumeLabel");

        bs.init();
        assertEquals(Fat16BootSector.DEFAULT_VOLUME_LABEL, bs.getVolumeLabel());
    }
    
    @Test
    public void testSetVolumeLabelValid() {
        System.out.println("setVolumeLabel (valid)");
        
        final String label = "01234567890";
        bs.setVolumeLabel(label);
        assertEquals(label, bs.getVolumeLabel());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSetVolumeLabelTooLong() {
        System.out.println("setVolumeLabel (too long)");

        bs.setVolumeLabel("012345678901");
    }
    
    @Test
    public void testInit() throws IOException {
        System.out.println("init");

        bs.init();
        
        assertEquals(
                Fat16BootSector.DEFAULT_VOLUME_LABEL,
                bs.getVolumeLabel());
        
        assertEquals(
                Fat16BootSector.DEFAULT_ROOT_DIR_ENTRY_COUNT,
                bs.getRootDirEntryCount());
    }

}
