
package org.jnode.fs.fat;

import java.io.IOException;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class RootDirectoryFullException extends IOException {
    private final static long serialVersionUID = 1;

    public RootDirectoryFullException() {
        super("root directory is full");
    }
    
}
