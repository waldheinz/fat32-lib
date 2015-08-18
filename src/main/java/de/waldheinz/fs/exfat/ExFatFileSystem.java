
package de.waldheinz.fs.exfat;

import de.waldheinz.fs.AbstractFileSystem;
import de.waldheinz.fs.BlockDevice;
import de.waldheinz.fs.FsDirectory;
import java.io.IOException;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public final class ExFatFileSystem extends AbstractFileSystem {
    
    public static ExFatFileSystem read(
            BlockDevice dev, boolean ro) throws IOException {
        
        final ExFatSuperBlock sb = ExFatSuperBlock.read(dev, ro);
        final Node rootNode = Node.createRoot(sb);
        final RootDirVisitor rootDirVis = new RootDirVisitor(sb);
        
        DirectoryParser.create(rootNode).parse(rootDirVis);
        
        if (rootDirVis.bitmap == null) {
            throw new IOException("cluster bitmap not found");
        }
        
        if (rootDirVis.upcase == null) {
            throw new IOException("upcase table not found");
        }
        
        final ExFatFileSystem result = new ExFatFileSystem(
                sb, rootNode, rootDirVis, ro);
        
        return result;
    }
    
    private final ExFatSuperBlock sb;
    private final Node rootNode;
    private final UpcaseTable upcase;
    private final ClusterBitMap bitmap;
    
    private ExFatFileSystem(ExFatSuperBlock sb, Node rootNode,
            RootDirVisitor vis, boolean ro) {
        
        super(ro);
        
        this.sb = sb;
        this.rootNode = rootNode;
        this.upcase = vis.upcase;
        this.bitmap = vis.bitmap;
    }
    
    @Override
    public FsDirectory getRoot() throws IOException {
        return new NodeDirectory(rootNode, upcase, isReadOnly());
    }
    
    @Override
    public long getTotalSpace() throws IOException {
        return -1;
    }

    @Override
    public long getFreeSpace() throws IOException {
        return -1;
    }

    @Override
    public long getUsableSpace() throws IOException {
        return -1;
    }

    @Override
    public void flush() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    private static class RootDirVisitor implements DirectoryParser.Visitor {

        private final ExFatSuperBlock sb;
        private ClusterBitMap bitmap;
        private UpcaseTable upcase;
        private String label;
        
        private RootDirVisitor(ExFatSuperBlock sb) {
            this.sb = sb;
        }

        @Override
        public void foundLabel(String label) {
            this.label = label;
        }
        
        @Override
        public void foundBitmap(
                long startCluster, long size) throws IOException {
            
            if (this.bitmap != null) {
                throw new IOException("already had a bitmap");
            }
            
            this.bitmap = ClusterBitMap.read(this.sb, startCluster, size);
        }
        
        @Override
        public void foundUpcaseTable(DirectoryParser parser, long startCluster, long size,
                long checksum) throws IOException {
            
            if (this.upcase != null) {
                throw new IOException("already had an upcase table");
            }
            
            this.upcase = UpcaseTable.read(
                    this.sb, startCluster, size, checksum);

            /* the parser may use this table for file names to come */
            parser.setUpcase(this.upcase);
        }

        @Override
        public void foundNode(Node node) {
            /* ignore */
        }
        
    }
    
}
