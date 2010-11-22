
package de.waldheinz.fs.exfat;

import de.waldheinz.fs.BlockDevice;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class DeviceAccess {
    
    private final BlockDevice dev;
    private final ByteBuffer buffer;
    
    public DeviceAccess(BlockDevice dev) {
        this.dev = dev;
        this.buffer = ByteBuffer.allocate(8);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
    }
    
    public long getUint32(long offset) throws IOException {
        this.buffer.rewind();
        this.buffer.limit(4);
        this.dev.read(offset, buffer);
        
        return (this.buffer.getInt(0) & 0xffffffff);
    }
    
}
