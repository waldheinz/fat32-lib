
package com.meetwise.fs;

import java.io.IOException;

/**
 * This exception is thrown by a {@link BlockDevice} if there is an attempt
 * to write to a read-only device.
 *
 * @see BlockDevice#isReadOnly()
 * @see BlockDevice#write(long, java.nio.ByteBuffer)
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
public final class ReadOnlyDeviceException extends IOException {
    private final static long serialVersionUID = 1;
    
    private final BlockDevice device;

    /**
     * Creates a new instance of {@code ReadOnlyDeviceException} for the
     * specified {@code BlockDevice}.
     *
     * @param device the device that generated this exception
     */
    public ReadOnlyDeviceException(BlockDevice device) {
        super("this device is read-only");
        
        this.device = device;    
    }

    /**
     * Returns the {@code BlockDevice} this exception occured on.
     *
     * @return the {@code BlockDevice} that generated this exception
     */
    public BlockDevice getBlockDevice() {
        return device;
    }
    
}
