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
import de.waldheinz.fs.ReadOnlyException;
import de.waldheinz.fs.util.RamDisk;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class VolumeLabelTest {

    private BlockDevice dev;
    private FatFileSystem fs;
    private Fat16BootSector bs;
    private AbstractDirectory dirStore;
    
    @Before
    public void setUp() throws IOException {
        this.dev = new RamDisk(8 * 1024 * 1024);
        
        this.fs = SuperFloppyFormatter.get(dev).format();
        this.dirStore = fs.getRootDirStore();
        this.bs = (Fat16BootSector) fs.getBootSector();
    }

    @Test
    public void testNothingSet() {
        System.out.println("nothingSet");
        
        assertEquals(Fat16BootSector.DEFAULT_VOLUME_LABEL, fs.getVolumeLabel());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSetLabelTooLong() throws IOException {
        System.out.println("setLabel (too long)");

        fs.setVolumeLabel("this is too long for sure");
    }
    
    @Test(expected=ReadOnlyException.class)
    public void testSetLabelReadOnly() throws IOException {
        System.out.println("setLabel (read only)");

        fs.close();
        fs = new FatFileSystem(dev, true);
        fs.setVolumeLabel("the label");
    }
    
    @Test
    public void testSetLabel() throws IOException {
        System.out.println("setLabel");

        final String label = "A Volume";
        fs.setVolumeLabel(label);

        assertEquals(label, fs.getVolumeLabel());
        assertEquals(label, bs.getVolumeLabel());
        assertEquals(label, dirStore.getLabel());

        fs.close();
        
        fs = new FatFileSystem(dev, true);
        dirStore = fs.getRootDirStore();
        bs = (Fat16BootSector) fs.getBootSector();
        
        assertEquals(label, fs.getVolumeLabel());
        assertEquals(label, bs.getVolumeLabel());
        assertEquals(label, dirStore.getLabel());
    }

    @Test(expected=IllegalArgumentException.class)
    public void testSetLabelInvalidChars() throws IOException {
        System.out.println("setLabel (invalid chars)");

        fs.setVolumeLabel(" invalid");
    }
}
