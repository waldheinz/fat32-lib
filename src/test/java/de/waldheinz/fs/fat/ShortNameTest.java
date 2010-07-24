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

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class ShortNameTest {
    
    @Test
    public void testGetDots() {
        System.out.println("get (dot names)");
        
        assertEquals(ShortName.DOT, ShortName.get("."));
        assertEquals(ShortName.DOT_DOT, ShortName.get(".."));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testValidChars() {
        System.out.println("validChars");

        ShortName.get("\u003D");
    }
    
    @Test
    public void testGetValid() {
        System.out.println("getValid");

        ShortName name = ShortName.get("TEST.TXT");
        assertNotNull(name);

        name = ShortName.get("TEST");
        assertNotNull(name);
    }
    
    @Test(expected=IllegalArgumentException.class)
    public void testGetTooLong() {
        System.out.println("getTooLong");

        ShortName.get("THISISAVERYLONGSOHRTNAME");
    }

    @Test(expected=IllegalArgumentException.class)
    public void testGetLongExt() {
        System.out.println("getLongExt");

        ShortName.get("FILE.EXTENSION");
    }
}
