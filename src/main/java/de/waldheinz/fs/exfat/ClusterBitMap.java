
package de.waldheinz.fs.exfat;

import java.io.IOException;

/**
 * The exFAT free space bitmap.
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class ClusterBitMap {
    
    public static ClusterBitMap read(ExFatSuperBlock sb,
            long startCluster, long size) throws IOException {

        Cluster.checkValid(startCluster);
        
        final ClusterBitMap result = new ClusterBitMap(sb, startCluster, size);

        if (size < ((result.clusterCount + 7) / 8)) {
            throw new IOException("cluster bitmap too small");
        }
        
        return result;
    }
    
    /**
     * The super block of the file system holding this {@code ClusterBitMap}.
     */
    private final ExFatSuperBlock sb;

    /**
     * The first cluster of the {@code ClusterBitMap}.
     */
    private final long startCluster;

    /**
     * The size in bytes.
     */
    private final long size;

    private final long clusterCount;
    
    private ClusterBitMap(ExFatSuperBlock sb, long startCluster, long size) {
        this.sb = sb;
        this.startCluster = startCluster;
        this.size = size;
        this.clusterCount = sb.getClusterCount() - Cluster.FIRST_DATA_CLUSTER;
    }
    
}
