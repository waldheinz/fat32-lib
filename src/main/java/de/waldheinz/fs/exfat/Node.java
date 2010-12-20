
package de.waldheinz.fs.exfat;

import java.io.IOException;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class Node {

    private final static int ATTRIB_RO = 0x01;
    private final static int ATTRIB_HIDDEN = 0x02;
    private final static int ATTRIB_SYSTEM = 0x04;
    private final static int ATTRIB_VOLUME = 0x08;
    private final static int ATTRIB_DIR = 0x10;
    private final static int ATTRIB_ARCH = 0x20;
    
    public static Node createRoot(ExFatSuperBlock sb)
            throws IOException {

        final int flags = ATTRIB_DIR;
        final Node result = new Node(sb, sb.getRootDirCluster(), flags);
        
        result.clusterCount = result.rootDirSize();
        
        return result;
    }

    public static Node create(
            ExFatSuperBlock sb, long startCluster, int flags,
            String name) {
        
        final Node result = new Node(sb, startCluster, flags);
        
        result.name = name;

        return result;
    }
    
    private final ExFatSuperBlock sb;
    private final DeviceAccess da;
    private final long startCluster;
    
    private boolean isContiguous;
    private long clusterCount;
    private int flags;
    private String name;
    
    private Node(ExFatSuperBlock sb, long startCluster, int flags) {
        this.sb = sb;
        this.da = sb.getDeviceAccess();
        this.startCluster = startCluster;
        this.flags = flags;
    }

    public boolean isDirectory() {
        return ((this.flags & ATTRIB_DIR) != 0);
    }

    public ExFatSuperBlock getSuperBlock() {
        return sb;
    }
    
    public long getClusterCount() {
        return clusterCount;
    }

    public long getStartCluster() {
        return startCluster;
    }
    
    private long rootDirSize() throws IOException {
        long size = 0;
        long current = this.sb.getRootDirCluster();
        
        while (!Cluster.invalid(current)) {
            size++;
            current = nextCluster(current);
        }
        
        return size;
    }
    
    public long nextCluster(long cluster) throws IOException {
        Cluster.checkValid(cluster);
        
        if (this.isContiguous) {
            return cluster + 1;
        } else {
            final long fatOffset =
                    sb.blockToOffset(this.sb.getFatBlockStart()) +
                        cluster * Cluster.SIZE;
            
            return this.da.getUint32(fatOffset);
        }
    }

    @Override
    public String toString() {
        return Node.class.getSimpleName() +
                " [name=" + this.name + "]";
    }
    
}
