
package de.waldheinz.fs.exfat;

import java.io.IOException;
import java.text.DateFormat;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class EntryTimesTest {
    
    private final static byte[] TEST_DATA = {
        (byte) 0x44, (byte) 0x62, (byte) 0x86, (byte) 0x3b, /* create */
        (byte) 0xf1, (byte) 0x62, (byte) 0xba, (byte) 0x3a, /* modify */
        (byte) 0x44, (byte) 0x62, (byte) 0x86, (byte) 0x3b, /* access */
        (byte) 0xa8, /* create cs */
        (byte) 0x00, /* modified cs */
        (byte) 0xec, (byte) 0xec, (byte) 0xec /* tz offset c/m/a */
    };
    
    @Before
    public void setUp() {
        
    }
    
    @Test
    public void testRead() throws IOException {
        System.out.println("read");
        
        final ByteBuffer src = ByteBuffer.wrap(TEST_DATA);
        src.order(ByteOrder.LITTLE_ENDIAN);
        
        final EntryTimes et = EntryTimes.read(src);
        
        assertNotNull(et);
        
        DateFormat df = DateFormat.getDateTimeInstance(
                DateFormat.MEDIUM, DateFormat.LONG, Locale.GERMAN);
        df.setTimeZone(TimeZone.getTimeZone("UTC"));
        
        System.out.println("created  : " + df.format(et.getCreated()));
        System.out.println("modified : " + df.format(et.getModified()));
        System.out.println("accessed : " + df.format(et.getAccessed()));
        
    }

}
