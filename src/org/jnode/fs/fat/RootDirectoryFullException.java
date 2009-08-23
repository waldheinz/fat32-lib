
package org.jnode.fs.fat;

import java.io.IOException;
import org.jnode.fs.FileSystem;
import org.jnode.fs.FileSystemException;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class RootDirectoryFullException extends FileSystemException {
    private final static long serialVersionUID = 1;

    public RootDirectoryFullException(FileSystem fs) {
        super(fs, "root directory is full");
    }
    
}
