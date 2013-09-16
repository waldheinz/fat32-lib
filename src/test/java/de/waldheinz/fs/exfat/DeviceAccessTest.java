package de.waldheinz.fs.exfat;

import java.nio.ByteBuffer;
import org.junit.Test;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class DeviceAccessTest {
    
    @Test
    public void testGetUint32() {
        System.out.println("getUint32");
        
        final ByteBuffer bb = ByteBuffer.allocate(4);
        bb.putInt(-1);
        bb.rewind();
        
        final long unsigned = DeviceAccess.getUint32(bb);
        
        System.out.println("read " + unsigned);
        
        assertThat(unsigned, greaterThanOrEqualTo(0l));
    }
    
}
