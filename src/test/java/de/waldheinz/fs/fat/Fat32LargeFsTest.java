/*
 * Copyright (C) 2013 Matthias Treydte <waldheinz@gmail.com>.
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 * MA 02110-1301  USA
 */
package de.waldheinz.fs.fat;

import de.waldheinz.fs.util.FileDisk;
import java.io.File;
import java.io.IOException;
import org.junit.Test;

/**
 *
 * @author Matthias Treydte &lt;waldheinz@gmail.com&gt;
 */
public class Fat32LargeFsTest {
    
    @Test
    public void testLargeFileSystem() throws Exception {
        File f = File.createTempFile("fat32-lib-test-", ".img");
        f.deleteOnExit();
        System.out.println("test file is " + f.getAbsolutePath());
        
        FileDisk fd = FileDisk.create(f, 512 * 1024 * 1024);
        FatFileSystem fs = SuperFloppyFormatter.get(fd)
                .setFatType(FatType.FAT32)
                .format();
        
        fs.flush();
        fs.close();
    }
    
}
