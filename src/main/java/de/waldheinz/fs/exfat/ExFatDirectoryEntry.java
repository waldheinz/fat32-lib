
package de.waldheinz.fs.exfat;

import de.waldheinz.fs.AbstractFsObject;
import de.waldheinz.fs.FsDirectory;
import de.waldheinz.fs.FsDirectoryEntry;
import de.waldheinz.fs.FsFile;
import java.io.IOException;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class ExFatDirectoryEntry
    extends AbstractFsObject
    implements FsDirectoryEntry {
    
    public static ExFatDirectoryEntry createRoot(ExFatSuperBlock sb)
            throws IOException {
                
        final ExFatDirectoryEntry result =
                new ExFatDirectoryEntry(sb.isReadOnly(), true, sb);
        
        result.clusterCount = result.rootDirSize();
        
        return result;
    }
    
    private final boolean isDirectory;
    private final ExFatSuperBlock sb;
    private final DeviceAccess da;
    private boolean isContiguous;
    private long clusterCount;
    
    public ExFatDirectoryEntry(boolean readOnly, boolean isDirectory,
            ExFatSuperBlock sb) {
                
        super(readOnly);
        
        this.isDirectory = isDirectory;
        this.sb = sb;
        this.da = sb.getDeviceAccess();
    }

    public long getClusterCount() {
        return clusterCount;
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
    
    private long blockToOffset(long block) {
        return this.sb.getBlockSize() * block;
    }
    
    public long nextCluster(long cluster) throws IOException {
        Cluster.checkValid(cluster);
        
        if (this.isContiguous) {
            return cluster + 1;
        } else {
            final long fatOffset = blockToOffset(this.sb.getFatBlockStart()) +
                    cluster * Cluster.SIZE;
            
            return this.da.getUint32(fatOffset);
        }
    }

    @Override
    public String getName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FsDirectory getParent() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getLastModified() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getCreated() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public long getLastAccessed() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isFile() {
        return !this.isDirectory;
    }

    @Override
    public boolean isDirectory() {
        return this.isDirectory;
    }

    @Override
    public void setName(String newName) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setLastModified(long lastModified) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FsFile getFile() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public FsDirectory getDirectory() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public boolean isDirty() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
