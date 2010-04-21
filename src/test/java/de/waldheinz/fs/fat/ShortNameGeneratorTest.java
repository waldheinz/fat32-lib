
package de.waldheinz.fs.fat;

import de.waldheinz.fs.fat.ShortNameGenerator;
import de.waldheinz.fs.fat.ShortName;
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
                new ShortNameGenerator(new HashSet<ShortName>());
        sng.generateShortName("....");
    }

    @Test
    public void testGenerateShortName() {
        System.out.println("generateShortName");

        final Set<ShortName> used = new HashSet<ShortName>();
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
        
        final Set<ShortName> used = new HashSet<ShortName>();
        final ShortNameGenerator sng = new ShortNameGenerator(used);
        
        used.add(sng.generateShortName("foo.txt"));
        assertEquals(ShortName.get("FOO~1.TXT"),
                sng.generateShortName("foo.txt"));
    }
}
