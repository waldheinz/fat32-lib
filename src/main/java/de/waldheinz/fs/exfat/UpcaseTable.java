
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
            
        final UpcaseTable result = new UpcaseTable(sb);
        
        return result;
    }
    
    private final ExFatSuperBlock sb;

    private UpcaseTable(ExFatSuperBlock sb) {
        this.sb = sb;
    }
    
}
