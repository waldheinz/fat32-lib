
package com.meetwise.fs.fat;

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

    @Test
    public void testGenerateShortName() {
        System.out.println("generateShortName");

        final Set<ShortName> used = new HashSet<ShortName>();
        final ShortNameGenerator sng = new ShortNameGenerator(used);
        
        assertEquals(new ShortName("FOO.TXT"),
                sng.generateShortName("foo.txt"));
        assertEquals(new ShortName("TEST01~1.TXT"),
                sng.generateShortName("TEST 01.TXT"));
        assertEquals(new ShortName("TEXTFILE.TXT"),
                sng.generateShortName("TextFile.Txt"));
        assertEquals(new ShortName("TEXTFI~1.TXT"),
                sng.generateShortName("TextFile1.Mine.txt"));
        assertEquals(new ShortName("VER_12~1.TEX"),
                sng.generateShortName("ver +1.2.text"));
        assertEquals(new ShortName("MICROS~1.BAK"),
                sng.generateShortName("microsoft.bak"));
    }
    
    @Test
    public void testGenerateTildeSuffix() {
        System.out.println("generateTildeSuffix");
        
        final Set<ShortName> used = new HashSet<ShortName>();
        final ShortNameGenerator sng = new ShortNameGenerator(used);

        used.add(sng.generateShortName("foo.txt"));
        assertEquals(new ShortName("FOO~1.TXT"),
                sng.generateShortName("foo.txt"));
    }
}
