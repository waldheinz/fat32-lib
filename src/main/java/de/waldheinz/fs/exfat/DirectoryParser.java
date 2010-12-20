
package de.waldheinz.fs.exfat;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class DirectoryParser {

    private final static int ENAME_MAX_LEN = 15;
    private final static int VALID     = 0x80;
    private final static int CONTINUED = 0x40;
    private final static int EOD = (0x00);
    private final static int BITMAP = (0x01 | VALID);
    private final static int UPCASE = (0x02 | VALID);
    private final static int LABEL = (0x03 | VALID);
    private final static int FILE = (0x05 | VALID);
    private final static int FILE_INFO = (0x00 | VALID | CONTINUED);
    private final static int FILE_NAME = (0x01 | VALID | CONTINUED);
    
    public static DirectoryParser create(Node node) throws IOException {
        assert (node.isDirectory()) : "not a directory";

        final ByteBuffer chunk = ByteBuffer.allocate(
                node.getSuperBlock().getBytesPerCluster());
        chunk.order(ByteOrder.LITTLE_ENDIAN);
        
        final DirectoryParser result = new DirectoryParser(
                chunk, node.getSuperBlock(), node.getStartCluster());
                
        result.init();
        
        return result;
    }
    
    private final ExFatSuperBlock sb;
    private final ByteBuffer chunk;
    private long cluster;

    private DirectoryParser(
            ByteBuffer chunk, ExFatSuperBlock sb, long cluster) {

        this.chunk = chunk;
        this.sb = sb;
        this.cluster = cluster;
    }

    private void init() throws IOException {
        this.sb.readCluster(chunk, cluster);
        chunk.rewind();
    }
    
    public void parse(Visitor v) throws IOException {
        final int entryType = chunk.get() & 0xff;
        
        switch (entryType) {
            case LABEL:
                parseLabel(v);
                break;
                
            default:
                throw new IOException("unknown entry type " + entryType);
        }
    }

    private void parseLabel(Visitor v) throws IOException {
        final int length = DeviceAccess.getUint8(chunk);
        
        if (length > ENAME_MAX_LEN) {
            throw new IOException(length + " is too long");
        }

        final StringBuilder labelBuilder = new StringBuilder(length);
        
        for (int i=0; i < length; i++) {
            labelBuilder.append(DeviceAccess.getChar(chunk));
        }
        
        v.foundLabel(labelBuilder.toString());
    }
    
    interface Visitor {
        public void foundLabel(String label);
    }

}
