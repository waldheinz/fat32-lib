
package de.waldheinz.fs.fat;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import java.io.IOException;
import org.junit.Before;
import org.junit.Test;
import de.waldheinz.fs.util.RamDisk;

public class MoveToReplaceTest {

    private static final String FILE_NAME = "file"; 
    private static final String DIRECTORY_NAME = "dir"; 

    private FatLfnDirectoryEntry file;
    private FatLfnDirectoryEntry dir;
    private FatLfnDirectory root;
    private FatFileSystem fs;
    
    @Before
    public void setUp() throws IOException {
        RamDisk dev = new RamDisk(1024 * 1024);
        fs = SuperFloppyFormatter.get(dev).format();

        root = fs.getRoot();
        file = root.addFile(FILE_NAME);
        dir = root.addDirectory(DIRECTORY_NAME);

    }
    
    @Test
    public void moveToAndReplaceTest() throws IOException {
        
        /* move file form root to dir */
        file.moveTo(dir.getDirectory(), DIRECTORY_NAME);

        /* check that the file has been moved and isn't in the original dir */
        
        assertEquals(file.getParent(), dir.getDirectory());
        assertNull( root.getEntry(FILE_NAME) );

        /* create a new file with a SAME filename in the original dir */
        
        root.addFile(FILE_NAME);
    }

}
