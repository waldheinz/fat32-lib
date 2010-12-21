
package de.waldheinz.fs.exfat;

import de.waldheinz.fs.AbstractFsObject;
import de.waldheinz.fs.FsDirectory;
import de.waldheinz.fs.FsDirectoryEntry;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class NodeDirectory extends AbstractFsObject implements FsDirectory {
    
    private final Node node;
    private final Map<String, NodeEntry> nameToNode;

    public NodeDirectory(Node node, boolean readOnly) throws IOException {
        super(readOnly);
        
        this.node = node;
        this.nameToNode = new HashMap<String, NodeEntry>();
        
        DirectoryParser.create(node).parse(new VisitorImpl());
    }
    
    @Override
    public Iterator<FsDirectoryEntry> iterator() {
        return Collections.<FsDirectoryEntry>unmodifiableCollection(
                nameToNode.values()).iterator();
    }
    
    @Override
    public FsDirectoryEntry getEntry(String name) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public FsDirectoryEntry addFile(String name) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public FsDirectoryEntry addDirectory(String name) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void remove(String name) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }
    
    @Override
    public void flush() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private class VisitorImpl implements DirectoryParser.Visitor {

        @Override
        public void foundLabel(String label) throws IOException {
            /* ignore */
        }

        @Override
        public void foundBitmap(
                long startCluster, long size) throws IOException {

            /* ignore */
        }

        @Override
        public void foundUpcaseTable(long checksum, long startCluster,
                long size) throws IOException {
            
            /* ignore */
        }
        
        @Override
        public void foundNode(Node node) throws IOException {
            nameToNode.put(node.getName(),
                    new NodeEntry(node, isReadOnly(), NodeDirectory.this));
        }
        
    }

}
