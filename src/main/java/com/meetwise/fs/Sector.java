
package com.meetwise.fs;

import com.meetwise.fs.util.LittleEndian;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * 
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class Sector {

    protected final byte[] data;
    protected boolean dirty;
    
    protected Sector(byte[] data) {
        this.data = data;
        this.dirty = false;
    }

    /**
     * Reads the specified number of bytes starting at {@code offset} from
     * the device and returns it as an array of bytes.
     *
     * @param dev the device to read from
     * @param offset the offset where to start reading
     * @param size the number of bytes to read
     * @return the bytes that were read
     * @throws IOException on read error
     */
    public static byte[] readBytes(BlockDevice dev,
            long offset, int size) throws IOException {
        
        final byte[] result = new byte[size];
        dev.read(offset, ByteBuffer.wrap(result));
        return result;
    }

    protected int get16(int offset) {
        return LittleEndian.getUInt16(data, offset);
    }

    protected long get32(int offset) {
        return LittleEndian.getUInt32(data, offset);
    }
    
    protected int get8(int offset) {
        return LittleEndian.getUInt8(data, offset);
    }

    protected void set16(int offset, int value) {
        LittleEndian.setInt16(data, offset, value);
        dirty = true;
    }

    protected void set32(int offset, long value) {
        LittleEndian.setInt32(data, offset, (int) value);
        dirty = true;
    }

    protected void set8(int offset, int value) {
        LittleEndian.setInt8(data, offset, value);
        dirty = true;
    }

}
