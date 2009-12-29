
package com.meetwise.fs.fat;

import com.meetwise.fs.BlockDevice;
import com.meetwise.fs.FileSystemException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A chain of clusters as stored in a {@link Fat}.
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class ClusterChain {
    private final Fat fat;
    private final BlockDevice device;
    private final int clusterSize;
    private final long dataOffset;
    
    private long startCluster;

    public ClusterChain(Fat fat, int clusterSize,
            long dataOffset, long startCluster) {
        
        this.fat = fat;
        if (startCluster != 0)
            this.fat.testCluster(startCluster);
        this.device = fat.getDevice();
        this.dataOffset = dataOffset;
        this.startCluster = startCluster;
        this.clusterSize = clusterSize;
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
     * 
     * @param cluster
     * @param clusterOffset
     * @return long
     * @throws FileSystemException
     */
    private long getDevOffset(long cluster, int clusterOffset)
            throws FileSystemException {
        
        return dataOffset + clusterOffset +
                ((cluster - Fat.FIRST_CLUSTER) * clusterSize);
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
     * @throws IOException
     */
    public synchronized void setChainLength(int nrClusters) throws IOException {
        
        if (this.startCluster == 0) {
            final long[] chain;

            chain = fat.allocNew(nrClusters);

            this.startCluster = chain[0];
        } else {
            final long[] chain = fat.getChain(startCluster);

            if (nrClusters != chain.length) {
                if (nrClusters > chain.length) {
                    // Grow
                    int count = nrClusters - chain.length;

                    while (count > 0) {
                        fat.allocAppend(getStartCluster());
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
            throws IOException {

        int len = dest.remaining();
        final long[] chain = getFat().getChain(startCluster);
        final BlockDevice dev = getDevice();

        int chainIdx = (int) (offset / clusterSize);
        if (offset % clusterSize != 0) {
            int clusOfs = (int) (offset % clusterSize);
            int size = Math.min(len,
                    (int) (clusterSize - (offset % clusterSize) - 1));
            dest.limit(dest.position() + size);

            dev.read(getDevOffset(chain[chainIdx], clusOfs), dest);
            
            offset += size;
            len -= size;
            chainIdx++;
        }

        while (len > 0) {
            int size = Math.min(clusterSize, len);
            dest.limit(dest.position() + size);

            dev.read(getDevOffset(chain[chainIdx], 0), dest);

            len -= size;
            chainIdx++;
        }
    }
    
    public synchronized void writeData(long offset, ByteBuffer srcBuf)
            throws IOException {
        
        int len = srcBuf.remaining();
        
        final long[] chain = fat.getChain(getStartCluster());

        int chainIdx = (int) (offset / clusterSize);
        if (offset % clusterSize != 0) {
            int clusOfs = (int) (offset % clusterSize);
            int size = Math.min(len,
                    (int) (clusterSize - (offset % clusterSize) - 1));
            srcBuf.limit(srcBuf.position() + size);

            device.write(getDevOffset(chain[chainIdx], clusOfs), srcBuf);
            
            offset += size;
            len -= size;
            chainIdx++;
        }
        
        while (len > 0) {
            int size = Math.min(clusterSize, len);
            srcBuf.limit(srcBuf.position() + size);

            device.write(getDevOffset(chain[chainIdx], 0), srcBuf);

            len -= size;
            chainIdx++;
        }
        
    }
}
