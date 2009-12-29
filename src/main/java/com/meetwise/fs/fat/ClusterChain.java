
package com.meetwise.fs.fat;

import com.meetwise.fs.BlockDevice;
import com.meetwise.fs.FileSystemException;
import com.meetwise.fs.FileSystemFullException;
import com.meetwise.fs.ReadOnlyFileSystemException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A chain of clusters as stored in a {@link Fat}.
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class ClusterChain extends FatObject {
    private final Fat fat;
    private final BlockDevice device;
    private long startCluster;
    private final int clusterSize;

    public ClusterChain(FatFileSystem fatFs, long startCluster) {
        super(fatFs);
        
        this.fat = fatFs.getFat();
        this.device = fatFs.getBlockDevice();
        this.startCluster = startCluster;
        this.clusterSize = fatFs.getClusterSize();
    }

    public Fat getFat() {
        return fat;
    }

    public BlockDevice getDevice() {
        return device;
    }

    /**
     * Returns the first cluster of this chain.
     *
     * @return the chain's first cluster, which may be 0 if this chain does
     *      not contain any clusters
     */
    public long getStartCluster() {
        return startCluster;
    }
    
    /**
     * Calculates the device offset (0-based) for the given cluster and offset
     * within the cluster.
     * @param cluster
     * @param clusterOffset
     * @return long
     * @throws FileSystemException
     */
    protected long getDevOffset(long cluster, int clusterOffset)
            throws FileSystemException {
        
        final long filesOffset = getFileSystem().getFilesOffset();
        
        return filesOffset + clusterOffset +
                ((cluster - FatUtils.FIRST_CLUSTER) * clusterSize);
    }

    /**
     * Returns the size this {@code ClusterChain} occupies on the device.
     *
     * @return the size this chain occupies on the device in bytes
     */
    public long getLengthOnDisk() {
        if (getStartCluster() == 0) return 0;
        
        final long[] chain = getFat().getChain(getStartCluster());
        return ((long) chain.length) * clusterSize;
    }


    /**
     * Sets the length.
     *
     * @param nrClusters the new number of clusters this chain should contain
     * @throws FileSystemException
     */
    public synchronized void setChainLength(int nrClusters) throws FileSystemException {

        if (getFileSystem().isReadOnly()) throw new
                ReadOnlyFileSystemException(this.getFileSystem());
        
        final FatFileSystem fs = getFileSystem();

        if (this.startCluster == 0) {
            final long[] chain;

            try {
                chain = fat.allocNew(nrClusters);
            } catch (IOException ex) {
                throw new FileSystemFullException(fs, ex);
            }

            this.startCluster = chain[0];
        } else {
            final long[] chain = fs.getFat().getChain(startCluster);

            if (nrClusters != chain.length) {
                if (nrClusters > chain.length) {
                    // Grow
                    int count = nrClusters - chain.length;
                    while (count > 0) {
                        try {
                            fat.allocAppend(getStartCluster());
                        } catch (IOException ex) {
                            throw new FileSystemFullException(fs, ex);
                        }
                        count--;
                    }
                } else {
                    // Shrink
                    fat.setEof(chain[nrClusters - 1]);
                    for (int i = nrClusters; i < chain.length; i++) {
                        fat.setFree(chain[i]);
                    }
                }
            }
        }
    }
    
    public synchronized void readData(long offset, ByteBuffer dest)
            throws FileSystemException {

        int len = dest.remaining();
        final long[] chain = getFat().getChain(startCluster);
        final BlockDevice dev = getDevice();

        int chainIdx = (int) (offset / clusterSize);
        if (offset % clusterSize != 0) {
            int clusOfs = (int) (offset % clusterSize);
            int size = Math.min(len,
                    (int) (clusterSize - (offset % clusterSize) - 1));
            dest.limit(dest.position() + size);

            try {
                dev.read(getDevOffset(chain[chainIdx], clusOfs), dest);
            } catch (IOException ex) {
                throw new FileSystemException(getFileSystem(), ex);
            }

            offset += size;
            len -= size;
            chainIdx++;
        }

        while (len > 0) {
            int size = Math.min(clusterSize, len);
            dest.limit(dest.position() + size);

            try {
                dev.read(getDevOffset(chain[chainIdx], 0), dest);
            } catch (IOException ex) {
                throw new FileSystemException(getFileSystem(), ex);
            }

            len -= size;
            chainIdx++;
        }
    }
    
    public synchronized void writeData(long offset, ByteBuffer srcBuf)
            throws FileSystemException {
            
        if (getFileSystem().isReadOnly())
            throw new ReadOnlyFileSystemException(this.getFileSystem(),
                    "write in readonly filesystem"); //NOI18N
                    
        int len = srcBuf.remaining();
        
        final FatFileSystem fs = getFileSystem();
        final long[] chain = fs.getFat().getChain(getStartCluster());
        final BlockDevice api = fs.getBlockDevice();

        int chainIdx = (int) (offset / clusterSize);
        if (offset % clusterSize != 0) {
            int clusOfs = (int) (offset % clusterSize);
            int size = Math.min(len,
                    (int) (clusterSize - (offset % clusterSize) - 1));
            srcBuf.limit(srcBuf.position() + size);

            try {
                api.write(getDevOffset(chain[chainIdx], clusOfs), srcBuf);
            } catch (IOException ex) {
                throw new FileSystemException(fs, ex);
            }

            offset += size;
            len -= size;
            chainIdx++;
        }
        
        while (len > 0) {
            int size = Math.min(clusterSize, len);
            srcBuf.limit(srcBuf.position() + size);

            try {
                api.write(getDevOffset(chain[chainIdx], 0), srcBuf);
            } catch (IOException ex) {
                throw new FileSystemException(fs, ex);
            }

            len -= size;
            chainIdx++;
        }

        
    }
}
