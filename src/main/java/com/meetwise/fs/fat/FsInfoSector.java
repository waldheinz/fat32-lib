
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
final class FsInfoSector extends Sector {

    /**
     * The offset to the free cluster count value in the FS info sector.
     */
    public static final int FREE_CLUSTERS_OFFSET = 0x1e8;
    
    public static final int LAST_ALLOCATED_OFFSET = 0x1ec;
    
    private FsInfoSector(BlockDevice device, long offset) {
        super(device, offset, BootSector.SIZE);
    }
    
    public static FsInfoSector read(Fat32BootSector bs) throws IOException {
        final FsInfoSector result =
                new FsInfoSector(bs.getDevice(), offset(bs));
        
        result.read();
        /* TODO: check if this really is a FS info sector */
        return result;
    }

    public static FsInfoSector create(Fat32BootSector bs) throws IOException {
        final FsInfoSector result =
                new FsInfoSector(bs.getDevice(), offset(bs));
        
        result.init();
        result.write();
        return result;
    }

    private static int offset(Fat32BootSector bs) {
        return bs.getFsInfoSectorNr() * bs.getBytesPerSector();
    }

    /**
     * Sets the number of free clusters on the file system stored at
     * {@link #FREE_CLUSTERS_OFFSET}.
     *
     * @param value the new free cluster count
     * @see Fat#getFreeClusterCount()
     */
    public void setFreeClusterCount(long value) {
        if (getFreeClusterCount() == value) return;
        
        set32(FREE_CLUSTERS_OFFSET, value);
    }
    
    /**
     * Returns the number of free clusters on the file system as sepcified by
     * the 32-bit value at {@link #FREE_CLUSTERS_OFFSET}.
     *
     * @return the number of free clusters
     * @see Fat#getFreeClusterCount() 
     */
    public long getFreeClusterCount() {
        return get32(FREE_CLUSTERS_OFFSET);
    }

    public void setLastAllocatedCluster(long value) {
        if (getLastAllocatedCluster() == value) return;
        
        super.set32(LAST_ALLOCATED_OFFSET, value);
    }

    public long getLastAllocatedCluster() {
        return super.get32(LAST_ALLOCATED_OFFSET);
    }

    private void init() {
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
        
        setFreeClusterCount(-1);
        setLastAllocatedCluster(2);

        buffer.position(0x1fe);
        buffer.put((byte) 0x55);
        buffer.put((byte) 0xaa);
        
        markDirty();
    }
}
