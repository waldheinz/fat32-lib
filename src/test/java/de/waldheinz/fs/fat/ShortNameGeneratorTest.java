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

import java.util.HashSet;
import java.util.Set;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
public class ShortNameGeneratorTest {
    
    @Test
    public void testValidChar() {
        System.out.println("validChar");

        assertTrue(ShortNameGenerator.validChar('A'));
        assertTrue(ShortNameGenerator.validChar('Z'));
        assertTrue(ShortNameGenerator.validChar('0'));
        assertTrue(ShortNameGenerator.validChar('9'));

        assertFalse(ShortNameGenerator.validChar('.'));
        assertFalse(ShortNameGenerator.validChar('ö'));
    }

    @Test(expected=IllegalArgumentException.class)
    public void testOnlyDots() {
        System.out.println("onlyDots");
        
        final ShortNameGenerator sng =
                new ShortNameGenerator(new HashSet<String>());
        sng.generateShortName("....");
    }

    @Test
    public void testGenerateShortName() {
        System.out.println("generateShortName");

        final Set<String> used = new HashSet<String>();
        final ShortNameGenerator sng = new ShortNameGenerator(used);
        
        assertEquals(ShortName.get("FOO.TXT"),
                sng.generateShortName("foo.txt"));
        assertEquals(ShortName.get("TEST01~1.TXT"),
                sng.generateShortName("TEST 01.TXT"));
        assertEquals(ShortName.get("TEXTFILE.TXT"),
                sng.generateShortName("TextFile.Txt"));
        assertEquals(ShortName.get("TEXTFI~1.TXT"),
                sng.generateShortName("TextFile1.Mine.txt"));
        assertEquals(ShortName.get("VER_12~1.TEX"),
                sng.generateShortName("ver +1.2.text"));
        assertEquals(ShortName.get("MICROS~1.BAK"),
                sng.generateShortName("microsoft.bak"));
        assertEquals(ShortName.get("ANNOY"),
                sng.generateShortName(".annoy"));
    }
    
    @Test
    public void testGenerateTildeSuffix() {
        System.out.println("generateTildeSuffix");
        
        final Set<String> used = new HashSet<String>();
        final ShortNameGenerator sng = new ShortNameGenerator(used);
        
        used.add(sng.generateShortName("foo.txt").asSimpleString().toLowerCase());
        assertEquals(ShortName.get("FOO~1.TXT"),
                sng.generateShortName("foo.txt"));
    }
}
