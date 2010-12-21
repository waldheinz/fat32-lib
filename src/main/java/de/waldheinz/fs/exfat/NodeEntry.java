
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
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isDirectory() {
        throw new UnsupportedOperationException("Not supported yet.");
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
