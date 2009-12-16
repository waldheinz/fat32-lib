
package com.meetwise.fs.fat;

/**
 * Contains the FAT32 specific parts of the boot sector.
 *
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
public final class Fat32BootSector extends BootSector {

    public Fat32BootSector(byte[] src) {
        super(src);
    }
    
    @Override
    public void init() {
        super.init();
        
        set16(0x32, 6); /* sector containing boot sector copy */

        /* FAT version */

        set8(0x52, 0x46); /* 'F' */
        set8(0x53, 0x41); /* 'A' */
        set8(0x54, 0x54); /* 'T' */
        set8(0x55, 0x33); /* '3' */
        set8(0x56, 0x32); /* '2' */
        set8(0x57, 0x20); /* ' ' */
        set8(0x58, 0x20); /* ' ' */
        set8(0x59, 0x20); /* ' ' */
    }

    /**
     * Sets the 11-byte volume label stored at offset 0x47.
     *
     * @param label the new volume label, may be {@code null}
     */
    public void setVolumeLabel(String label) {
        for (int i=0; i < 11; i++) {
            final byte c =
                    (label == null) ? 0 :
                    (label.length() > i) ? (byte) label.charAt(i) : 0x20;
            super.set8(0x47 + i, c);
        }
    }

    public int getFsInfoSectorNr() {
        return get16(0x30);
    }

    public void setFsInfoSectorNr(int offset) {
        if (getFsInfoSectorNr() == offset) return;
        super.set16(0x30, offset);
        super.dirty = true;
    }

    @Override
    public void setSectorsPerFat(long v) {
        if (getSectorsPerFat() == v) return;
        
        set32(0x24, v);
        super.dirty = true;
    }

    @Override
    public long getSectorsPerFat() {
        return get32(0x24);
    }

    @Override
    public FatType getFatType() {
        return FatType.FAT32;
    }

    @Override
    public void setSectorCount(long count) {
        super.setNrTotalSectors(count);
    }

    @Override
    public long getSectorCount() {
        return super.getNrTotalSectors();
    }

    /**
     * This is always 0 for FAT32.
     *
     * @return always 0
     */
    @Override
    public int getRootDirEntryCount() {
        return 0;
    }
    
    public void setFileSystemId(int id) {
        super.set32(0x43, id);
    }

    public int getFileSystemId() {
        return (int) super.get32(0x43);
    }

}
