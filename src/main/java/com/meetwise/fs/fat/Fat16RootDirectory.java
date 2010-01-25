
package com.meetwise.fs.fat;

import com.meetwise.fs.BlockDevice;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * The root directory of a FAT12/16 partition.
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class Fat16RootDirectory extends AbstractDirectory {
    private final BlockDevice device;
    private final long deviceOffset;

    private Fat16RootDirectory(Fat fat, boolean readOnly) {
        super(fat, fat.getBootSector().getRootDirEntryCount(), readOnly, true);

        this.deviceOffset = FatUtils.getRootDirOffset(fat.getBootSector());
        this.device = fat.getDevice();
    }

    public static Fat16RootDirectory read(Fat fat, boolean readOnly) throws IOException {
        final Fat16RootDirectory result = new Fat16RootDirectory(fat, readOnly);
        result.read();
        return result;
    }

    public static Fat16RootDirectory create(Fat fat) throws IOException {
        final Fat16RootDirectory result = new Fat16RootDirectory(fat, false);
        result.flush();
        return result;
    }

    @Override
    protected void read(ByteBuffer data) throws IOException {
        this.device.read(deviceOffset, data);
    }

    @Override
    protected void write(ByteBuffer data) throws IOException {
        this.device.write(deviceOffset, data);
    }

    /**
     * By convention always returns 0, as the FAT12/16 root directory is not
     * stored in a cluster chain.
     *
     * @return always 0
     */
    @Override
    protected long getStorageCluster() {
        return 0;
    }

    @Override
    protected boolean canChangeSize(int entryCount) {
        return false;
    }
    
}
