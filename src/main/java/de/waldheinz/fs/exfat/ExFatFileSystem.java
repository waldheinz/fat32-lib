
package de.waldheinz.fs.exfat;

import de.waldheinz.fs.BlockDevice;
import java.io.IOException;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class ExFatFileSystem {
    
    public static ExFatFileSystem read(
            BlockDevice dev, boolean ro) throws IOException {
        
        final ExFatSuperBlock sb = ExFatSuperBlock.read(dev, ro);
        final Node rootNode = Node.createRoot(sb);
        
        final DirectoryParser rootDirRead = DirectoryParser.create(rootNode);
        
        rootDirRead.parse(new RootDirVisitor());

        final ExFatFileSystem result = new ExFatFileSystem(dev, sb, ro);
        
        return result;
    }
    
    private final BlockDevice dev;
    private final ExFatSuperBlock sb;
    private final boolean ro;

    private ExFatFileSystem(BlockDevice dev, ExFatSuperBlock sb, boolean ro) {
        this.dev = dev;
        this.sb = sb;
        this.ro = ro;
    }

    private static class RootDirVisitor implements DirectoryParser.Visitor {

        @Override
        public void foundLabel(String label) {
            throw new UnsupportedOperationException("Not supported yet.");
        }

        @Override
        public void foundBitmap(long startCluster, long size) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
        
    }

}
