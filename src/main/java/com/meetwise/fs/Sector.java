
package com.meetwise.fs;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * 
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class Sector {

    private final BlockDevice device;
    private final long offset;

    /**
     * The buffer holding the contents of this {@code Sector}.
     */
    protected final ByteBuffer buffer;

    protected boolean dirty;
    
    protected Sector(BlockDevice device, long offset, int size) throws IOException {
        this.offset = offset;
        this.device = device;
        this.buffer = ByteBuffer.allocate(size);
        this.buffer.order(ByteOrder.LITTLE_ENDIAN);
        device.read(offset, buffer);
        this.dirty = false;
    }

    public boolean isDirty() {
        return this.dirty;
    }

    public void write() throws IOException {
        buffer.rewind();
        buffer.limit(buffer.capacity());
        device.write(offset, buffer);
        this.dirty = false;
    }

    protected int get16(int offset) {
        return buffer.getShort(offset) & 0xffff;
    }

    protected long get32(int offset) {
        return buffer.getInt(offset) & 0xffffffff;
    }
    
    protected int get8(int offset) {
        return buffer.get(offset) & 0xff;
    }
    
    protected void set16(int offset, int value) {
        buffer.putShort(offset, (short) (value & 0xffff));
        dirty = true;
    }

    protected void set32(int offset, long value) {
        buffer.putInt(offset, (int) (value & 0xffffffff));
        dirty = true;
    }

    protected void set8(int offset, int value) {
        buffer.put(offset, (byte) (value & 0xff));
        dirty = true;
    }
    
}
