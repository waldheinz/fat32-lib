
package de.waldheinz.fs.exfat;

import de.waldheinz.fs.AbstractFsObject;
import de.waldheinz.fs.BlockDevice;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class ExFatSuperBlock extends AbstractFsObject {

    /**
     * The size of the ExFAT super block in bytes.
     */
    private final static int SIZE = 512;

    private final static String OEM_NAME = "EXFAT   "; //NOI18N

    private long blockStart;
    private long blockCount;
    private long fatBlockStart;
    private long fatBlockCount;
    private long clusterBlockStart;
    private long clusterCount;
    private long rootDirCluster;
    private int volumeSerial;
    private byte fsVersionMinor;
    private byte fsVersionMajor;
    private short volumeState;
    private byte blockBits;
    private byte blocksPerClusterBits;

    public ExFatSuperBlock(boolean ro) {
        super(ro);
    }
    
    public static ExFatSuperBlock read(
            BlockDevice dev, boolean ro) throws IOException {
        
        final ByteBuffer b = ByteBuffer.allocate(SIZE);
        b.order(ByteOrder.LITTLE_ENDIAN);
        dev.read(0, b);
        
        /* check OEM name */

        final byte[] oemBytes = new byte[OEM_NAME.length()];
        b.position(0x03);
        b.get(oemBytes);
        final String oemString = new String(oemBytes);

        if (!OEM_NAME.equals(oemString)) {
            throw new IOException("OEM name mismatch");
        }

        /* check fat count */

        if ((b.get(0x6e) & 0xff) != 1) {
            throw new IOException("invalid FAT count");
        }

        /* check drive # */

        if ((b.get(0x6f) & 0xff) != 0x80) {
            throw new IOException("invalid drive number");
        }
        
        /* check boot signature */

        if ((b.get(510) & 0xff) != 0x55 ||
                (b.get(511) & 0xff) != 0xaa) throw new IOException(
                "missing boot sector signature");

        final ExFatSuperBlock result = new ExFatSuperBlock(ro);

        result.blockStart = b.getLong(0x40);
        result.blockCount = b.getLong(0x48);
        result.fatBlockStart = b.getInt(0x50);
        result.fatBlockCount = b.getInt(0x54);
        result.clusterBlockStart = b.getInt(0x58);
        result.clusterCount = b.getInt(0x5c);
        result.rootDirCluster = b.getInt(0x60);
        result.volumeSerial = b.getInt(0x64);
        result.fsVersionMinor = b.get(0x68);
        result.fsVersionMajor = b.get(0x69);
        result.volumeState = b.getShort(0x6a);
        result.blockBits = b.get(0x6c);
        result.blocksPerClusterBits = b.get(0x6d);

        return result;
    }

    public long getBlockStart() {
        return blockStart;
    }

    public long getBlockCount() {
        return blockCount;
    }

    public long getFatBlockStart() {
        return fatBlockStart;
    }

    public long getFatBlockCount() {
        return fatBlockCount;
    }

    public long getClusterBlockStart() {
        return clusterBlockStart;
    }
    
    public long getClusterCount() {
        return clusterCount;
    }

    public long getRootDirCluster() {
        return rootDirCluster;
    }

    public int getVolumeSerial() {
        return volumeSerial;
    }

    public byte getFsVersionMajor() {
        return fsVersionMajor;
    }

    public byte getFsVersionMinor() {
        return fsVersionMinor;
    }

    public short getVolumeState() {
        return volumeState;
    }

    public int getBlockSize() {
        return (1 << blockBits);
    }

    public int getBlocksPerCluster() {
        return (1 << blocksPerClusterBits);
    }

}
