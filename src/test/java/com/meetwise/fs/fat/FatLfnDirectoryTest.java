
package com.meetwise.fs.fat;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
public class FatLfnDirectoryTest {

    @Test
    public void testValidChar() {
        System.out.println("validChar");
        
        assertTrue(FatLfnDirectory.validChar('A'));
        assertTrue(FatLfnDirectory.validChar('Z'));
        assertTrue(FatLfnDirectory.validChar('0'));
        assertTrue(FatLfnDirectory.validChar('9'));

        assertFalse(FatLfnDirectory.validChar('.'));
        assertFalse(FatLfnDirectory.validChar('ö'));
    }

}
