
package de.waldheinz.fs;

import java.io.IOException;

/**
 * Indicates that it was not possible to determine the file system being
 * used on a block device.
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public final class UnknownFileSystemException extends IOException {
    private final static long serialVersionUID = 1;
    
    private final BlockDevice device;
    
    /**
     * Creates a new instance of {@code UnknownFileSystemException}.
     * 
     * @param device the {@code BlockDevice} whose file system could not
     *      be determined
     */
    public UnknownFileSystemException(BlockDevice device) {
        super("can not determin file system type"); //NOI18N
        this.device = device;
    }

    /**
     * Returns the {@code BlockDevice} whose file system could not be
     * determined.
     *
     * @return the {@code BlockDevice} with an unknown file system
     */
    public BlockDevice getDevice() {
        return this.device;
    }
}
