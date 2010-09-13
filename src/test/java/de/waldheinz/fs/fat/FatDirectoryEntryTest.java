
package de.waldheinz.fs.fat;

import java.nio.ByteBuffer;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class FatDirectoryEntryTest {
    
    @Test
    public void testRead() {
        System.out.println("read (null)");
        
        final ByteBuffer buff = ByteBuffer.allocate(FatDirectoryEntry.SIZE);
        final FatDirectoryEntry result = FatDirectoryEntry.read(buff, false);
        
        assertNull(result);
    }
    
    @Test
    public void testVolumeLabel() {
        System.out.println("volumeLabel");
        
        FatDirectoryEntry vl = FatDirectoryEntry.createVolumeLabel("my name");
        
        assertTrue(vl.isVolumeLabel());
        assertEquals("my name", vl.getVolumeLabel());
        assertFalse(vl.isFile());
        assertFalse(vl.isDirectory());
        assertFalse(vl.isLfnEntry());
    }
    
    @Test
    public void testSystemFlag() {
        System.out.println("systemFlag");
        
        final FatDirectoryEntry e = FatDirectoryEntry.create(false);
        assertTrue(e.isFile());
        assertFalse(e.isSystemFlag());

        e.setSystemFlag(true);
        
        assertTrue(e.isFile());
        assertTrue(e.isSystemFlag());
    }
    
    @Test
    public void testHiddenFlag() {
        System.out.println("hiddenFlag");
        
        final FatDirectoryEntry e = FatDirectoryEntry.create(true);
        e.write(ByteBuffer.allocate(FatDirectoryEntry.SIZE));

        assertTrue(e.isDirectory());
        assertFalse(e.isHiddenFlag());
        assertFalse(e.isDirty());

        e.setHiddenFlag(true);

        assertTrue(e.isDirectory());
        assertTrue(e.isHiddenFlag());
        assertTrue(e.isDirty());
    }

    /**
     * Test of isLfnEntry method, of class FatDirectoryEntry.
     */
    @Test
    public void testIsLfnEntry() {
        System.out.println("isLfnEntry");
        
        FatDirectoryEntry instance = null;
        boolean expResult = false;
        boolean result = instance.isLfnEntry();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isDirty method, of class FatDirectoryEntry.
     */
    @Test
    public void testIsDirty() {
        System.out.println("isDirty");
        FatDirectoryEntry instance = null;
        boolean expResult = false;
        boolean result = instance.isDirty();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getFlags method, of class FatDirectoryEntry.
     */
    @Test
    public void testGetFlags() {
        System.out.println("getFlags");
        FatDirectoryEntry instance = null;
        int expResult = 0;
        int result = instance.getFlags();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setFlags method, of class FatDirectoryEntry.
     */
    @Test
    public void testSetFlags() {
        System.out.println("setFlags");
        int flags = 0;
        FatDirectoryEntry instance = null;
        instance.setFlags(flags);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isDirectory method, of class FatDirectoryEntry.
     */
    @Test
    public void testIsDirectory() {
        System.out.println("isDirectory");
        FatDirectoryEntry instance = null;
        boolean expResult = false;
        boolean result = instance.isDirectory();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
    @Test
    public void testCreateFile() {
        System.out.println("create (file)");
        
        final FatDirectoryEntry result = FatDirectoryEntry.create(false);
        
        assertTrue(result.isFile());
        assertFalse(result.isDirectory());
        assertFalse(result.isVolumeLabel());
        assertFalse(result.isLfnEntry());
    }

    @Test
    public void testCreateDirectory() {
        System.out.println("create (directory)");

        final FatDirectoryEntry result = FatDirectoryEntry.create(true);

        assertTrue(result.isDirectory());
        assertFalse(result.isFile());
        assertFalse(result.isVolumeLabel());
        assertFalse(result.isLfnEntry());
    }
    
    /**
     * Test of getCreated method, of class FatDirectoryEntry.
     */
    @Test
    public void testGetCreated() {
        System.out.println("getCreated");
        FatDirectoryEntry instance = null;
        long expResult = 0L;
        long result = instance.getCreated();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setCreated method, of class FatDirectoryEntry.
     */
    @Test
    public void testSetCreated() {
        System.out.println("setCreated");
        long created = 0L;
        FatDirectoryEntry instance = null;
        instance.setCreated(created);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getLastModified method, of class FatDirectoryEntry.
     */
    @Test
    public void testGetLastModified() {
        System.out.println("getLastModified");
        FatDirectoryEntry instance = null;
        long expResult = 0L;
        long result = instance.getLastModified();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setLastModified method, of class FatDirectoryEntry.
     */
    @Test
    public void testSetLastModified() {
        System.out.println("setLastModified");
        long lastModified = 0L;
        FatDirectoryEntry instance = null;
        instance.setLastModified(lastModified);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getLastAccessed method, of class FatDirectoryEntry.
     */
    @Test
    public void testGetLastAccessed() {
        System.out.println("getLastAccessed");
        FatDirectoryEntry instance = null;
        long expResult = 0L;
        long result = instance.getLastAccessed();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setLastAccessed method, of class FatDirectoryEntry.
     */
    @Test
    public void testSetLastAccessed() {
        System.out.println("setLastAccessed");
        long lastAccessed = 0L;
        FatDirectoryEntry instance = null;
        instance.setLastAccessed(lastAccessed);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isDeleted method, of class FatDirectoryEntry.
     */
    @Test
    public void testIsDeleted() {
        System.out.println("isDeleted");
        FatDirectoryEntry instance = null;
        boolean expResult = false;
        boolean result = instance.isDeleted();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
    @Test
    public void testLength() {
        System.out.println("length");
        
        final FatDirectoryEntry e = FatDirectoryEntry.create(false);
        assertEquals(0, e.getLength());
        
        e.setLength(100000);

        assertEquals(100000, e.getLength());
    }
    
    /**
     * Test of getShortName method, of class FatDirectoryEntry.
     */
    @Test
    public void testGetShortName() {
        System.out.println("getShortName");
        FatDirectoryEntry instance = null;
        ShortName expResult = null;
        ShortName result = instance.getShortName();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isFile method, of class FatDirectoryEntry.
     */
    @Test
    public void testIsFile() {
        System.out.println("isFile");
        FatDirectoryEntry instance = null;
        boolean expResult = false;
        boolean result = instance.isFile();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setShortName method, of class FatDirectoryEntry.
     */
    @Test
    public void testSetShortName() {
        System.out.println("setShortName");
        ShortName sn = null;
        FatDirectoryEntry instance = null;
        instance.setShortName(sn);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getStartCluster method, of class FatDirectoryEntry.
     */
    @Test
    public void testGetStartCluster() {
        System.out.println("getStartCluster");
        FatDirectoryEntry instance = null;
        long expResult = 0L;
        long result = instance.getStartCluster();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of setStartCluster method, of class FatDirectoryEntry.
     */
    @Test
    public void testSetStartCluster() {
        System.out.println("setStartCluster");
        long startCluster = 0L;
        FatDirectoryEntry instance = null;
        instance.setStartCluster(startCluster);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }
    
    /**
     * Test of write method, of class FatDirectoryEntry.
     */
    @Test
    public void testWrite() {
        System.out.println("write");
        ByteBuffer buff = null;
        FatDirectoryEntry instance = null;
        instance.write(buff);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of isReadonlyFlag method, of class FatDirectoryEntry.
     */
    @Test
    public void testIsReadonlyFlag() {
        System.out.println("isReadonlyFlag");
        FatDirectoryEntry instance = null;
        boolean expResult = false;
        boolean result = instance.isReadonlyFlag();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

    /**
     * Test of getLfnPart method, of class FatDirectoryEntry.
     */
    @Test
    public void testGetLfnPart() {
        System.out.println("getLfnPart");
        FatDirectoryEntry instance = null;
        String expResult = "";
        String result = instance.getLfnPart();
        assertEquals(expResult, result);
        // TODO review the generated test code and remove the default call to fail.
        fail("The test case is a prototype.");
    }

}
