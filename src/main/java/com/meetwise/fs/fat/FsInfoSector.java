
package com.meetwise.fs.fat;

import com.meetwise.fs.Sector;
import com.meetwise.fs.BlockDevice;
import java.io.IOException;

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
    public FsInfoSector(BlockDevice device, long offset) throws IOException {
        super(device, offset, BootSector.SIZE);
    }
    
    public static int offset(Fat32BootSector bs) {
        return bs.getFsInfoSectorNr() * bs.getBytesPerSector();
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
        buffer.position(0x00);
        buffer.put((byte) 0x52);
        buffer.put((byte) 0x52);
        buffer.put((byte) 0x61);
        buffer.put((byte) 0x41);
        
        /* 480 reserved bytes */

        buffer.position(0x1e4);
        buffer.put((byte) 0x72);
        buffer.put((byte) 0x72);
        buffer.put((byte) 0x41);
        buffer.put((byte) 0x61);
        
        setNrFreeClusters(-1);
        setLastAllocatedCluster(2);

        buffer.position(0x1fe);
        buffer.put((byte) 0x55);
        buffer.put((byte) 0xaa);
        
        dirty = true;
    }
}
