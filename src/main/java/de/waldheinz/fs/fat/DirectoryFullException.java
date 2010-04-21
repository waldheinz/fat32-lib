
package de.waldheinz.fs.fat;

import java.io.IOException;

/**
 * Gets thrown when either
 * <ul>
 * <li>a {@link Fat16RootDirectory} becomes full or</li>
 * <li>a {@link ClusterChainDirectory} grows beyond it's
 *      {@link ClusterChainDirectory#MAX_SIZE maximum size}
 * </ul>
 * 
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public final class DirectoryFullException extends IOException {
    private final static long serialVersionUID = 2;
    private final int currentCapacity;
    private final int requestedCapacity;
    
    DirectoryFullException(int currentCapacity, int requestedCapacity) {
        this("directory is full", currentCapacity, requestedCapacity);
    }
    
    DirectoryFullException(String message,
            int currentCapacity, int requestedCapacity) {

        super(message);

        this.currentCapacity = currentCapacity;
        this.requestedCapacity = requestedCapacity;
    }
    
    /**
     * Returns the current capacity of the directory.
     *
     * @return the current capacity
     */
    public int getCurrentCapacity() {
        return currentCapacity;
    }

    /**
     * Returns the capacity the directory tried to grow, which did not succeed.
     *
     * @return the requested capacity
     */
    public int getRequestedCapacity() {
        return requestedCapacity;
    }
    
}
