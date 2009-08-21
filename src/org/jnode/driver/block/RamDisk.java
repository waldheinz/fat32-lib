
package org.jnode.driver.block;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A {@link BlockDevice} that lives entirely in heap memory. This is basically
 * a RAM disk.
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public final class RamDisk implements BlockDevice {
    
    /**
     * The default sector size for {@code RamDisk}s.
     */
    public final static int DEFAULT_SECTOR_SIZE = 512;
    
    private final int sectorSize;
    private final ByteBuffer data;
    private final int size;

    /**
     * Creates a new instance of {@code RamDisk} of this specified
     * size and using the {@link #DEFAULT_SECTOR_SIZE}.
     *
     * @param size the size of the new block device
     */
    public RamDisk(int size) {
        this(size, DEFAULT_SECTOR_SIZE);
    }

    /**
     * Creates a new instance of {@code RamDisk} of this specified
     * size and sector size
     *
     * @param size the size of the new block device
     * @param sectorSize the sector size of the new block device
     */
    public RamDisk(int size, int sectorSize) {
        if (sectorSize < 1) throw new IllegalArgumentException(
                "invalid sector size"); //NOI18N
        
        this.sectorSize = sectorSize;
        this.size = size;
        this.data = ByteBuffer.allocate(size);
    }
    
    public long getLength() {
        return this.size;
    }

    public void read(long devOffset, ByteBuffer dest) throws IOException {
        if (devOffset > getLength()) throw new IllegalArgumentException();
        
        data.limit((int) (devOffset + dest.remaining()));
        data.position((int) devOffset);
        
        dest.put(data);
    }

    public void write(long devOffset, ByteBuffer src) throws IOException {
        if (devOffset > getLength()) throw new IllegalArgumentException(
                "offset=" + devOffset + ", length=" + getLength());

        data.limit((int) (devOffset + src.remaining()));
        data.position((int) devOffset);
        
        
        data.put(src);
    }
    
    public void flush() throws IOException {
        /* not needed */
    }
    
    public int getSectorSize() throws IOException {
        return this.sectorSize;
    }
}
