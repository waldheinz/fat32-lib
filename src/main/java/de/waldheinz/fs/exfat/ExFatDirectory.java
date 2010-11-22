
package de.waldheinz.fs.exfat;

import de.waldheinz.fs.AbstractFsObject;
import de.waldheinz.fs.FsDirectory;
import de.waldheinz.fs.FsDirectoryEntry;
import java.io.IOException;
import java.util.Iterator;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class ExFatDirectory extends AbstractFsObject implements FsDirectory {
    
    private final ExFatDirectoryEntry entry;

    public ExFatDirectory(ExFatDirectoryEntry entry) {
        super(entry.isReadOnly());
        
        this.entry = entry;
    }
    
    @Override
    public Iterator<FsDirectoryEntry> iterator() {
        throw new UnsupportedOperationException("Not supported yet.");
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
    
}
