
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
        final FatDirectoryEntry result = FatDirectoryEntry.read(FatType.FAT16, buff, false);
        
        assertNull(result);
    }
    
    @Test
    public void testVolumeLabel() {
        System.out.println("volumeLabel");
        
        FatDirectoryEntry vl = FatDirectoryEntry.createVolumeLabel(FatType.FAT16, "my name");
        
        assertTrue(vl.isVolumeLabel());
        assertEquals("my name", vl.getVolumeLabel());
        assertFalse(vl.isFile());
        assertFalse(vl.isDirectory());
        assertFalse(vl.isLfnEntry());
    }
    
    @Test
    public void testSystemFlag() {
        System.out.println("systemFlag");
        
        final FatDirectoryEntry e = FatDirectoryEntry.create(FatType.FAT16, false);
        assertTrue(e.isFile());
        assertFalse(e.isSystemFlag());

        e.setSystemFlag(true);
        
        assertTrue(e.isFile());
        assertTrue(e.isSystemFlag());
        assertTrue(e.isDirty());
    }
    
    @Test
    public void testHiddenFlag() {
        System.out.println("hiddenFlag");
        
        final FatDirectoryEntry e = FatDirectoryEntry.create(FatType.FAT16, true);
        e.write(ByteBuffer.allocate(FatDirectoryEntry.SIZE));

        assertTrue(e.isDirectory());
        assertFalse(e.isHiddenFlag());
        assertFalse(e.isDirty());

        e.setHiddenFlag(true);

        assertTrue(e.isDirectory());
        assertTrue(e.isHiddenFlag());
        assertTrue(e.isDirty());
    }
    
    @Test
    public void testCreateFile() {
        System.out.println("create (file)");
        
        final FatDirectoryEntry result = FatDirectoryEntry.create(FatType.FAT16, false);
        
        assertTrue(result.isFile());
        assertFalse(result.isDirectory());
        assertFalse(result.isVolumeLabel());
        assertFalse(result.isLfnEntry());
    }

    @Test
    public void testCreateDirectory() {
        System.out.println("create (directory)");

        final FatDirectoryEntry result = FatDirectoryEntry.create(FatType.FAT16, true);

        assertTrue(result.isDirectory());
        assertFalse(result.isFile());
        assertFalse(result.isVolumeLabel());
        assertFalse(result.isLfnEntry());
    }
    
    @Test
    public void testIsDeleted() {
        System.out.println("isDeleted");
        
        FatDirectoryEntry e = FatDirectoryEntry.create(FatType.FAT16, false);
        assertFalse (e.isDeleted());

        final ByteBuffer bb = ByteBuffer.allocate(512);
        bb.put(0, (byte) 0xe5);

        e = FatDirectoryEntry.read(FatType.FAT16, bb, false);
        assertTrue(e.isDeleted());
    }
    
    @Test
    public void testLength() {
        System.out.println("length");
        
        final FatDirectoryEntry e = FatDirectoryEntry.create(FatType.FAT16, false);
        assertEquals(0, e.getLength());
        
        e.setLength(100000);

        assertEquals(100000, e.getLength());
    }
    
    @Test
    public void testReadonlyFlag() {
        System.out.println("readonlyFlag");
        
        final FatDirectoryEntry e = FatDirectoryEntry.create(FatType.FAT16, true);
        e.write(ByteBuffer.allocate(FatDirectoryEntry.SIZE));
        
        assertTrue(e.isDirectory());
        assertFalse(e.isReadonlyFlag());
        assertFalse(e.isHiddenFlag());
        assertFalse(e.isDirty());

        e.setReadonlyFlag(true);

        assertTrue(e.isDirectory());
        assertTrue(e.isReadonlyFlag());
        assertFalse(e.isHiddenFlag());
        assertTrue(e.isDirty());
    }
    
    @Test
    public void testLowercaseBasename() {
        System.out.println("lowercaseBasename");

        final FatDirectoryEntry e = FatDirectoryEntry.create(FatType.FAT16, false);
        assertFalse (e.isBasenameLowercaseFlag());
        assertFalse (e.isExtensionLowercaseFlag());

        e.setBasenameLowercaseFlag(true);

        assertTrue (e.isBasenameLowercaseFlag());
        assertFalse (e.isExtensionLowercaseFlag());
    }

    @Test
    public void testLowercaseExtension() {
        System.out.println("lowercaseExtension");

        final FatDirectoryEntry e = FatDirectoryEntry.create(FatType.FAT16, false);
        assertFalse (e.isBasenameLowercaseFlag());
        assertFalse (e.isExtensionLowercaseFlag());

        e.setExtensionLowercaseFlag(true);

        assertFalse (e.isBasenameLowercaseFlag());
        assertTrue (e.isExtensionLowercaseFlag());
    }
}
