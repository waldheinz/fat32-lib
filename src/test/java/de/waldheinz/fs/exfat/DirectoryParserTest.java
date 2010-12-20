
package de.waldheinz.fs.exfat;

import de.waldheinz.fs.util.RamDisk;
import java.io.IOException;
import java.io.InputStream;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class DirectoryParserTest {

    private RamDisk bd;
    private ExFatSuperBlock sb;
    
    @Before
    public void setUp() throws IOException {
        final InputStream is =
                getClass().getResourceAsStream("exfat-test.dmg.gz");
        bd = RamDisk.readGzipped(is);
        sb = ExFatSuperBlock.read(bd, false);
        is.close();
    }
    
    @Test
    public void testCreate() throws IOException {
        System.out.println("create");
        
        Node node = Node.createRoot(sb);
        DirectoryParser result = DirectoryParser.create(node);

        assertNotNull(result);
    }

    @Test
    public void testParse() throws IOException {
        System.out.println("parse");

        Node node = Node.createRoot(sb);
        DirectoryParser result = DirectoryParser.create(node);

        result.parse(new DirectoryParser.Visitor() {
            
            private void print(String msg) {
                System.out.println("  * " + msg);
            }
            
            @Override
            public void foundLabel(String label) {
                print("label : " + label);
            }

            @Override
            public void foundBitmap(long startCluster, long size) {
                print("bitmap at " + startCluster + " of size " + size);
            }

            @Override
            public void foundUpcaseTable(
                    long checksum, long startCluster, long size) {

                print("upcase at " + startCluster + " of size " + size +
                        " (checksum " + Long.toHexString(checksum) + ")");
            }
            
        });
        
    }

}
