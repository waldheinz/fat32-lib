
package org.jnode.fs.fat;

import org.jnode.util.LittleEndian;

/**
 * 
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class Sector {

    protected final byte[] data;
    protected boolean dirty;

    public Sector(int size) {
        this (new byte[size]);
    }

    private Sector(byte[] data) {
        this.data = data;
        this.dirty = false;
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
