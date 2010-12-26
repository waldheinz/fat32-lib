
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
final class NodeEntry extends AbstractFsObject implements FsDirectoryEntry {

    private final Node node;
    private final NodeDirectory parent;

    public NodeEntry(Node node, boolean readOnly, NodeDirectory parent) {
        super(readOnly);

        this.node = node;
        this.parent = parent;
    }
    
    @Override
    public String getName() {
        return node.getName();
    }

    @Override
    public FsDirectory getParent() {
        return this.parent;
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
        return (!this.node.isDirectory());
    }
    
    @Override
    public boolean isDirectory() {
        return this.node.isDirectory();
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
        if (!isFile()) {
            throw new UnsupportedOperationException("not a file");
        }

        return new NodeFile(this.node, isReadOnly());
    }
    
    @Override
    public FsDirectory getDirectory() throws IOException {
        if (!isDirectory()) {
            throw new UnsupportedOperationException("not a directory");
        }
        
        return new NodeDirectory(node, parent.upcase, isReadOnly());
    }
    
    @Override
    public boolean isDirty() {
        return false;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();

        sb.append(NodeEntry.class.getName());
        sb.append(" [node=");
        sb.append(this.node);
        sb.append(", parent=");
        sb.append(this.parent);
        sb.append("]");
        
        return sb.toString();
    }
    
}
