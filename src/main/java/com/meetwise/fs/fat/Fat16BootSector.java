
package com.meetwise.fs.fat;

import com.meetwise.fs.BlockDevice;

/**
 * The boot sector layout as used by the FAT12 / FAT16 variants.
 *
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
final class Fat16BootSector extends BootSector {

    /**
     * The default number of entries for the root directory.
     * 
     * @see #getRootDirEntryCount()
     * @see #setRootDirEntryCount(int) 
     */
    public static final int DEFAULT_ROOT_DIR_ENTRY_COUNT = 512;

    /**
     * The default volume label.
     */
    public static final String DEFAULT_VOLUME_LABEL = "NO NAME"; //NOI18N
    
    /**
     * The maximum number of clusters for a FAT12 file system. This is actually
     * the number of clusters where mkdosfs stop complaining about a FAT16
     * partition having not enough sectors, so it would be misinterpreted
     * as FAT12 without special handling.
     *
     * @see #getNrLogicalSectors()
     */
    public static final int MAX_FAT12_CLUSTERS = 4084;

    public static final int MAX_FAT16_CLUSTERS = 65524;

    /**
     * The offset to the sectors per FAT value.
     */
    public static final int SECTORS_PER_FAT_OFFSET = 0x16;

    /**
     * The offset to the root directory entry count value.
     *
     * @see #getRootDirEntryCount()
     * @see #setRootDirEntryCount(int) 
     */
    public static final int ROOT_DIR_ENTRIES_OFFSET = 0x11;

    /**
     * The offset to the first byte of the volume label.
     */
    public static final int VOLUME_LABEL_OFFSET = 0x2b;

    /**
     * The maximum length of the volume label.
     */
    public static final int MAX_VOLUME_LABEL_LENGTH = 11;

    /**
     * Creates a new {@code Fat16BootSector} for the specified device.
     *
     * @param device the {@code BlockDevice} holding the boot sector
     */
    public Fat16BootSector(BlockDevice device) {
        super(device);
    }
    
    /**
     * Returns the volume label that is stored in this boot sector.
     *
     * @return the volume label
     */
    public String getVolumeLabel() {
        final StringBuilder sb = new StringBuilder();

        for (int i=0; i < MAX_VOLUME_LABEL_LENGTH; i++) {
            final char c = (char) get8(VOLUME_LABEL_OFFSET + i);

            if (c != 0) {
                sb.append(c);
            } else {
                break;
            }
        }
        
        return sb.toString();
    }

    /**
     * Sets the volume label that is stored in this boot sector.
     *
     * @param label the new volume label
     * @throws IllegalArgumentException if the specified label is longer
     *      than {@link #MAX_VOLUME_LABEL_LENGTH}
     */
    public void setVolumeLabel(String label) throws IllegalArgumentException {
        if (label.length() > MAX_VOLUME_LABEL_LENGTH)
            throw new IllegalArgumentException("volume label too long");

        for (int i = 0; i < MAX_VOLUME_LABEL_LENGTH; i++) {
            set8(VOLUME_LABEL_OFFSET + i,
                    i < label.length() ? label.charAt(i) : 0);
        }
    }
    
    /**
     * Gets the number of sectors/fat for FAT 12/16.
     *
     * @return int
     */
    @Override
    public long getSectorsPerFat() {
        return get16(SECTORS_PER_FAT_OFFSET);
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
        
        set16(SECTORS_PER_FAT_OFFSET, (int)v);
    }

    @Override
    public FatType getFatType() {
        final long rootDirSectors = ((getRootDirEntryCount() * 32) +
                (getBytesPerSector() - 1)) / getBytesPerSector();
        final long dataSectors = getSectorCount() -
                (getNrReservedSectors() + (getNrFats() * getSectorsPerFat()) +
                rootDirSectors);
        final long clusterCount = dataSectors / getSectorsPerCluster();
        
        if (clusterCount > MAX_FAT16_CLUSTERS) throw new IllegalStateException(
                "too many clusters for FAT12/16: " + clusterCount);
        
        return clusterCount > MAX_FAT12_CLUSTERS ?
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
     * Gets the number of entries in the root directory.
     *
     * @return int the root directory entry count
     */
    @Override
    public int getRootDirEntryCount() {
        return get16(ROOT_DIR_ENTRIES_OFFSET);
    }
    
    /**
     * Sets the number of entries in the root directory
     *
     * @param v the new number of entries in the root directory
     * @throws IllegalArgumentException for negative values
     */
    public void setRootDirEntryCount(int v) throws IllegalArgumentException {
        if (v < 0) throw new IllegalArgumentException();
        if (v == getRootDirEntryCount()) return;
        
        set16(ROOT_DIR_ENTRIES_OFFSET, v);
    }
    
    @Override
    public void init() {
        super.init();
        
        setRootDirEntryCount(DEFAULT_ROOT_DIR_ENTRY_COUNT);
        setVolumeLabel(DEFAULT_VOLUME_LABEL);
    }
    
}
