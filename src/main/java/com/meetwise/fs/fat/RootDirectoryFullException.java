
package com.meetwise.fs.fat;

import java.io.IOException;

/**
 * Gets thrown when the root directory of a FAT12/FAT16 file system becomes
 * full and can not hold the additional entry. 
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public final class RootDirectoryFullException extends IOException {
    private final static long serialVersionUID = 2;
    
    RootDirectoryFullException() {
        super("root directory is full");
    }
}
