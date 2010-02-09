 
package com.meetwise.fs;

/**
 * This exception is thrown when an attempt is made to write to a read-only
 * {@link BlockDevice}, {@link FileSystem} or other file system object. This is
 * an unchecked exception, as it should always be possible to query the object
 * about it's read-only state using it's {@code isReadOnly()} method.
 * 
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 * @see FileSystem#isReadOnly()
 * @see BlockDevice#isReadOnly() 
 */
public final class ReadOnlyException extends RuntimeException {

    private final static long serialVersionUID = 1;
    
    /**
     * Creates a new instance of {@code ReadOnlyException}.
     *
     */
    public ReadOnlyException() {
        super("read-only");
    }
}
