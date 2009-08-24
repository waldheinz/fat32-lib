
package com.meetwise.fs;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.zip.GZIPInputStream;

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
     * 
     *
     * @param in
     * @return
     * @throws IOException on read error
     */
    public static RamDisk readGzipped(InputStream in) throws IOException {
        final GZIPInputStream zis = new GZIPInputStream(in);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        
        final byte[] buffer = new byte[4096];
        
        int read = zis.read(buffer);
        int total = 0;
        
        while (read >= 0) {
            total += read;
            bos.write(buffer, 0, read);
            read = zis.read(buffer);
        }

        if (total < DEFAULT_SECTOR_SIZE) throw new IOException(
                "read only " + total + " bytes"); //NOI18N
                
        final ByteBuffer bb = ByteBuffer.wrap(bos.toByteArray(), 0, total);
        return new RamDisk(bb, DEFAULT_SECTOR_SIZE);
    }
    
    private RamDisk(ByteBuffer buffer, int sectorSize) {
        this.size = buffer.limit();
        this.sectorSize = sectorSize;
        this.data = buffer;
    }

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
    
    public long getSize() {
        return this.size;
    }

    public void read(long devOffset, ByteBuffer dest) throws IOException {
        if (devOffset > getSize()) throw new IllegalArgumentException();
        
        data.limit((int) (devOffset + dest.remaining()));
        data.position((int) devOffset);
        
        dest.put(data);
    }

    public void write(long devOffset, ByteBuffer src) throws IOException {
        if (devOffset > getSize()) throw new IllegalArgumentException(
                "offset=" + devOffset + ", length=" + getSize());

        data.limit((int) (devOffset + src.remaining()));
        data.position((int) devOffset);
        
        
        data.put(src);
    }
    
    public void flush() throws IOException {
        /* not needed */
    }
    
    public int getSectorSize() {
        return this.sectorSize;
    }
}
