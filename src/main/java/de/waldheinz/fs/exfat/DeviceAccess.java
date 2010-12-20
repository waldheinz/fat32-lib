
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
        this.buffer.rewind();
        
        return getUint32(buffer);
    }

    public static int getUint8(ByteBuffer src) {
        return (src.get() & 0xff);
    }
    
    public static int getUint16(ByteBuffer src) {
        return (src.getShort() & 0xffff);
    }
    
    public static long getUint32(ByteBuffer src) {
        return (src.getInt() & 0xffffffff);
    }
    
    public static long getUint64(ByteBuffer src) throws IOException {
        final long result = src.getLong();
        
        if (result < 0) {
            throw new IOException("value too big");
        }
        
        return result;
    }
    
    public static char getChar(ByteBuffer src) {
        return (char) src.getShort();
    }
    
    public void read(ByteBuffer dest, long offset) throws IOException {
        dev.read(offset, dest);
    }
    
}
