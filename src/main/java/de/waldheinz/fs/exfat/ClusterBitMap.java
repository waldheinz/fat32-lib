
package de.waldheinz.fs.exfat;

/**
 * The exFAT free space bitmap.
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class ClusterBitMap {

    /**
     * The first cluster of the {@code ClusterBitMap}.
     */
    private final long startCluster;

    /**
     * 
     */
    private final int size;

    public ClusterBitMap(long startCluster, int size) {
        this.startCluster = startCluster;
        this.size = size;
    }
    
}
