
package de.waldheinz.fs.exfat;

import de.waldheinz.fs.AbstractFsObject;
import de.waldheinz.fs.FsFile;
import de.waldheinz.fs.ReadOnlyException;
import java.io.IOException;
import java.nio.ByteBuffer;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class NodeFile extends AbstractFsObject implements FsFile {
    
    private final Node node;

    public NodeFile(Node node, boolean readOnly) {
        super(readOnly);
        
        this.node = node;
    }
    
    @Override
    public long getLength() {
        return this.node.getSize();
    }

    @Override
    public void setLength(long length) throws IOException {
        if (getLength() == length) return;
        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void read(long offset, ByteBuffer dest) throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void write(long offset, ByteBuffer src)
            throws ReadOnlyException, IOException {
        
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void flush() throws IOException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
