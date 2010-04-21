
package de.waldheinz.fs;

import de.waldheinz.fs.fat.FatFileSystem;
import java.io.IOException;

/**
 * Factory for {@link FileSystem} instances.
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class FileSystemFactory {
    
    private FileSystemFactory() { }
    
    /**
     * <p>
     * Creates a new {@link FileSystem} for the specified {@code device}. When
     * using this method, care must be taken that there are no two file systems
     * existing on the same {@link BlockDevice}.
     * </p>
     *
     * @param device the device to create the file system for
     * @param readOnly if the file system should be openend read-only
     * @return a new {@code FileSystem} instance for the specified device
     * @throws UnknownFileSystemException if the file system type could
     *      not be determined
     * @throws IOException on read error
     */
    public static FileSystem create(BlockDevice device, boolean readOnly)
            throws UnknownFileSystemException, IOException {
            
        return new FatFileSystem(device, readOnly);
    }
}
