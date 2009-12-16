
package com.meetwise.fs.fat;

/**
 * The boot sector layout as used by the FAT12 / FAT16 variants.
 *
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
public class Fat16BootSector extends BootSector {

    /**
     * The maximum number of sectors for a FAT12 file system. This is actually
     * the number of sectors where mkdosfs stop complaining about a FAT16
     * partition having not enough sectors, so it would be misinterpreted
     * as FAT12 without special handling.
     *
     * @see #getNrLogicalSectors()
     */
    public static final int MAX_FAT12_SECTORS = 8202;
    
    Fat16BootSector(byte[] bytes) {
        super(bytes);
    }
    
    /**
     * Gets the number of sectors/fat for FAT 12/16.
     *
     * @return int
     */
    @Override
    public long getSectorsPerFat() {
        return get16(0x16);
    }

    /**
     * Sets the number of sectors/fat
     *
     * @param v  the new number of sectors per fat
     */
    @Override
    public void setSectorsPerFat(long v) {
        if (v == getSectorsPerFat()) return;
        if (v > 0x7FFF) throw new IllegalArgumentException(
                "too many sectors for a FAT12/16");
        
        set16(0x16, (int)v);
    }

    @Override
    public FatType getFatType() {
        return getSectorCount() > MAX_FAT12_SECTORS ?
            FatType.FAT16 : FatType.FAT12;
    }
    
    @Override
    public void setSectorCount(long count) {
        if (count > 65535) {
            setNrLogicalSectors(0);
            setNrTotalSectors(count);
        } else {
            setNrLogicalSectors((int) count);
            setNrTotalSectors(count);
        }
    }
    
    @Override
    public long getSectorCount() {
        if (getNrLogicalSectors() == 0) return getNrTotalSectors();
        else return getNrLogicalSectors();
    }


    /**
     * Gets the number of entries in the root directory
     *
     * @return int
     */
    @Override
    public int getRootDirEntryCount() {
        return get16(0x11);
    }

    /**
     * Sets the number of entries in the root directory
     *
     * @param v the new number of entries in the root directory
     */
    public void setRootDirEntryCount(int v) {
        if (v == getRootDirEntryCount()) return;

        set16(0x11, v);
    }
}
