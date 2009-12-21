
package com.meetwise.fs.fat;

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
}
