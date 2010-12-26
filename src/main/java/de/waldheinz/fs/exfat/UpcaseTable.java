
package de.waldheinz.fs.exfat;

import java.io.IOException;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class UpcaseTable {

    public static UpcaseTable read(ExFatSuperBlock sb,
            long startCluster, long size, long checksum) throws IOException {
                
        Cluster.checkValid(startCluster);
        
        if ((size == 0) || (size > (0xffff * 2)) || (size % 2) != 0) {
            throw new IOException("bad upcase table size " + size);
        }
            
        final UpcaseTable result = new UpcaseTable(sb,
                sb.clusterToOffset(startCluster), size);
        
        return result;
    }
    
    private final ExFatSuperBlock sb;
    private final long size;
    private final long chars;
    private final DeviceAccess da;
    private final long offset;

    private UpcaseTable(ExFatSuperBlock sb, long offset, long size) {
        this.sb = sb;
        this.da = sb.getDeviceAccess();
        this.size = size;
        this.chars = size / 2;
        this.offset = offset;
    }
    
    public char toUpperCase(char c) throws IOException {
        if (c > this.chars) {
            return c;
        } else {
            return da.getChar(offset + (c * 2));
        }
    }
    
    public String toUpperCase(String s) throws IOException {
        final StringBuilder result = new StringBuilder(s.length());

        for (char c : s.toCharArray()) {
            result.append(toUpperCase(c));
        }

        return result.toString();
    }
    
    public long getCharCount() {
        return this.chars;
    }
    
}
