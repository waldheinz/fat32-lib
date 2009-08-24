
package com.meetwise.fs.fat;

import com.meetwise.fs.FileSystemFullException;

/**
 * Gets thrown when the root directory of a FAT12/FAT16 file system becomes
 * full and can not hold the additional entry. 
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public final class RootDirectoryFullException extends FileSystemFullException {
    private final static long serialVersionUID = 1;
    
    RootDirectoryFullException(FatFileSystem fs) {
        super(fs, "root directory is full");
    }
    
}
