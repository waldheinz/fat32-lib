
package com.meetwise.fs.fat;

import com.meetwise.fs.BlockDevice;
import com.meetwise.fs.BootSector;
import java.io.IOException;

/**
 *
 * @author Matthias Treydte &lt;matthias.treydte at meetwise.com&gt;
 */
public final class SuperFloppyFormatter {

    /**
     * The media descriptor used (hard disk).
     */
    public final static int MEDIUM_DESCRIPTOR_HD = 0xf8;

    /**
     * The default number of FATs.
     */
    public final static int DEFAULT_FAT_COUNT = 2;

    /**
     * The default number of sectors per track.
     */
    public final static int DEFAULT_SECTORS_PER_TRACK = 63;

    /**
     * The default number of heads.
     */
    public final static int DEFULT_HEADS = 255;

    /**
     * The minimum number of clusters for FAT32.
     */
    public final static int FAT32_MIN_CLUSTERS = 65529;

    private final BlockDevice device;
    private final BootSector bs;
    
    private String label;

    /**
     * Creates a new {@code SuperFloppyFormatter} for the specified
     * {@code BlockDevice}.
     *
     * @param device
     * @throws IOException on error accessing the specified {@code device}
     */
    public SuperFloppyFormatter(BlockDevice device) throws IOException {
        this.device = device;
        this.bs = new BootSector(SF_BS);

        bs.setNrFats(DEFAULT_FAT_COUNT);
        bs.setMediumDescriptor(MEDIUM_DESCRIPTOR_HD);
        bs.setSectorsPerTrack(DEFAULT_SECTORS_PER_TRACK);
        bs.setNrHeads(DEFULT_HEADS);

        setFatType(defaultFatType());
    }

    /**
     * Sets the label of the file system to create.
     *
     * @param label the new file system label, may be {@code null}
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Returns the label that will be given to the new file system.
     *
     * @return the file system label, may be {@code null}
     */
    public String getLabel() {
        return label;
    }
    
    /**
     * Writes the boot sector and file system to the device.
     *
     * @throws IOException on write error
     */
    public void format() throws IOException {
        bs.write(device);

        final Fat fat = new Fat(bs.getFatType(), bs.getMediumDescriptor(),
                bs.getSectorsPerFat(), bs.getBytesPerSector());
                
        for (int i = 0; i < bs.getNrFats(); i++) {
            fat.write(device, FatUtils.getFatOffset(bs, i));
        }

        final FatLfnDirectory rootDir = new FatLfnDirectory(
                null, bs.getNrRootDirEntries());


        rootDir.write(device, FatUtils.getRootDirOffset(bs));

        if (label != null) {
            FatFileSystem fs = new FatFileSystem(device, false);
            fs.setVolumeLabel(label);
            fs.flush();
        }
        
        device.flush();
    }
    
    private void setFatType(FatType fatType) {
        /* parameter check */

        
    }

    private FatType defaultFatType() throws IOException {
        final long len = device.getSize();

        if (len < 1024 * 1024 * 1024) return FatType.FAT16;
        else return FatType.FAT32;
    }

    /**
     * A boot sector prototype for super floppies.
     */
    private final static byte[] SF_BS = {
        (byte) 0xeb, (byte) 0x58, (byte) 0x90, (byte) 0x6d, (byte) 0x6b,
        (byte) 0x64, (byte) 0x6f, (byte) 0x73, (byte) 0x66, (byte) 0x73,
        (byte) 0x00, (byte) 0x00, (byte) 0x02, (byte) 0x01, (byte) 0x20,
        (byte) 0x00, (byte) 0x02, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0xf8, (byte) 0x00, (byte) 0x00, (byte) 0x20,
        (byte) 0x00, (byte) 0x40, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x80, (byte) 0x38, (byte) 0x01,
        (byte) 0x00, (byte) 0x68, (byte) 0x02, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x02,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x01, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x29, (byte) 0xcd, (byte) 0xa3, (byte) 0x9b,
        (byte) 0x5b, (byte) 0x66, (byte) 0x61, (byte) 0x74, (byte) 0x33,
        (byte) 0x32, (byte) 0x6c, (byte) 0x69, (byte) 0x62, (byte) 0x20,
        (byte) 0x20, (byte) 0x20, (byte) 0x46, (byte) 0x41, (byte) 0x54,
        (byte) 0x33, (byte) 0x32, (byte) 0x20, (byte) 0x20, (byte) 0x20,
        (byte) 0x0e, (byte) 0x1f, (byte) 0xbe, (byte) 0x77, (byte) 0x7c,
        (byte) 0xac, (byte) 0x22, (byte) 0xc0, (byte) 0x74, (byte) 0x0b,
        (byte) 0x56, (byte) 0xb4, (byte) 0x0e, (byte) 0xbb, (byte) 0x07,
        (byte) 0x00, (byte) 0xcd, (byte) 0x10, (byte) 0x5e, (byte) 0xeb,
        (byte) 0xf0, (byte) 0x32, (byte) 0xe4, (byte) 0xcd, (byte) 0x16,
        (byte) 0xcd, (byte) 0x19, (byte) 0xeb, (byte) 0xfe, (byte) 0x54,
        (byte) 0x68, (byte) 0x69, (byte) 0x73, (byte) 0x20, (byte) 0x69,
        (byte) 0x73, (byte) 0x20, (byte) 0x6e, (byte) 0x6f, (byte) 0x74,
        (byte) 0x20, (byte) 0x61, (byte) 0x20, (byte) 0x62, (byte) 0x6f,
        (byte) 0x6f, (byte) 0x74, (byte) 0x61, (byte) 0x62, (byte) 0x6c,
        (byte) 0x65, (byte) 0x20, (byte) 0x64, (byte) 0x69, (byte) 0x73,
        (byte) 0x6b, (byte) 0x2e, (byte) 0x20, (byte) 0x20, (byte) 0x50,
        (byte) 0x6c, (byte) 0x65, (byte) 0x61, (byte) 0x73, (byte) 0x65,
        (byte) 0x20, (byte) 0x69, (byte) 0x6e, (byte) 0x73, (byte) 0x65,
        (byte) 0x72, (byte) 0x74, (byte) 0x20, (byte) 0x61, (byte) 0x20,
        (byte) 0x62, (byte) 0x6f, (byte) 0x6f, (byte) 0x74, (byte) 0x61,
        (byte) 0x62, (byte) 0x6c, (byte) 0x65, (byte) 0x20, (byte) 0x66,
        (byte) 0x6c, (byte) 0x6f, (byte) 0x70, (byte) 0x70, (byte) 0x79,
        (byte) 0x20, (byte) 0x61, (byte) 0x6e, (byte) 0x64, (byte) 0x0d,
        (byte) 0x0a, (byte) 0x70, (byte) 0x72, (byte) 0x65, (byte) 0x73,
        (byte) 0x73, (byte) 0x20, (byte) 0x61, (byte) 0x6e, (byte) 0x79,
        (byte) 0x20, (byte) 0x6b, (byte) 0x65, (byte) 0x79, (byte) 0x20,
        (byte) 0x74, (byte) 0x6f, (byte) 0x20, (byte) 0x74, (byte) 0x72,
        (byte) 0x79, (byte) 0x20, (byte) 0x61, (byte) 0x67, (byte) 0x61,
        (byte) 0x69, (byte) 0x6e, (byte) 0x20, (byte) 0x2e, (byte) 0x2e,
        (byte) 0x2e, (byte) 0x20, (byte) 0x0d, (byte) 0x0a, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00, (byte) 0x00,
        (byte) 0x55, (byte) 0xaa
    };

}
