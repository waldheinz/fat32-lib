
package de.waldheinz.fs.exfat;

import java.nio.ByteBuffer;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class EntryTimes {

    public static EntryTimes read(ByteBuffer src) {

        final int cTime = DeviceAccess.getUint16(src);
        final int cDate = DeviceAccess.getUint16(src);

        final int mTime = DeviceAccess.getUint16(src);
        final int mDate = DeviceAccess.getUint16(src);

        final int aTime = DeviceAccess.getUint16(src);
        final int aDate = DeviceAccess.getUint16(src);

        final int cTimeCs = DeviceAccess.getUint8(src);
        final int mTimeCs = DeviceAccess.getUint8(src);
        
        return new EntryTimes();
    }
    
}
