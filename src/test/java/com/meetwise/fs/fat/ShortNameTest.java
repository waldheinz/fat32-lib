
package com.meetwise.fs.fat;

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

    @Test
    public void testGetValid() {
        System.out.println("getValid");
        
        assertNotNull(ShortName.get("TEST.TXT"));
        assertNotNull(ShortName.get("TEST"));
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
