
package com.meetwise.fs.fat;

import com.meetwise.fs.Sector;
import com.meetwise.fs.BlockDevice;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The FAT32 File System Information Sector.
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 * @see http://en.wikipedia.org/wiki/File_Allocation_Table#FS_Information_Sector
 */
class FsInfoSector extends Sector {

    public static final int FREE_CLUSTERS_OFFSET = 0x1e8;

    public static final int LAST_ALLOCATED_OFFSET = 0x1ec;
    
    /**
     *
     * @param bs
     * @param device
     * @throws IOException on read error
     */
    public FsInfoSector(byte[] bytes) throws IOException {
        super(bytes);
    }

    public static int offset(Fat32BootSector bs) {
        return bs.getFsInfoSectorNr() * bs.getBytesPerSector();
    }
    
    boolean isDirty() {
        return dirty;
    }
    
    void write(BlockDevice device, long offset) throws IOException {
        device.write(offset, ByteBuffer.wrap(data));
    }
    
    public void setNrFreeClusters(long value) {
        super.set32(FREE_CLUSTERS_OFFSET, value);
    }

    public long getNrFreeClusters() {
        return super.get32(FREE_CLUSTERS_OFFSET);
    }

    public void setLastAllocatedCluster(long value) {
        super.set32(LAST_ALLOCATED_OFFSET, value);
    }

    public long getLastAllocatedCluster() {
        return super.get32(LAST_ALLOCATED_OFFSET);
    }

    public void init() {
        data[0x000] = 0x52; // R
        data[0x001] = 0x52; // R
        data[0x002] = 0x61; // a
        data[0x003] = 0x41; // A

        /* 480 reserved bytes */

        data[0x1e4] = 0x72; // r
        data[0x1e5] = 0x72; // r
        data[0x1e6] = 0x41; // A
        data[0x1e7] = 0x61; // a

        setNrFreeClusters(-1);
        setLastAllocatedCluster(2);
        
        data[0x1fe] = 0x55;
        data[0x1ff] = (byte) 0xaa;

        dirty = true;
    }
}
