
package de.waldheinz.fs.exfat;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class Cluster {
    
    /**
     * Marks a cluster containing a bad block.
     */
    private final static long BAD = 0xfffffff7;
    
    /**
     * Marks the final cluster of a file or directory.
     */
    private final static long END = 0xffffffff;
    
    public static boolean invalid(long cluster) {
        return ((cluster == END) || (cluster == BAD));
    }
    
    private Cluster() {
        /* utility class, no instances */
    }
    
}
