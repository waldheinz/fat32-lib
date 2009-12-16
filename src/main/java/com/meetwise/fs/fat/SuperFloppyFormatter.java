
package com.meetwise.fs.fat;

import com.meetwise.fs.BlockDevice;
import java.io.IOException;
import java.util.Random;

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
    public final static int DEFAULT_SECTORS_PER_TRACK = 32;

    /**
     * The default number of heads.
     */
    public final static int DEFULT_HEADS = 64;

    /**
     * The minimum number of clusters for FAT32.
     */
    public final static int FAT32_MIN_CLUSTERS = 65529;

    /**
     * The default OEM name for file systems created by this class.
     */
    public final static String DEFAULT_OEM_NAME = "fat32lib";

    public final static int DEFAULT_RESERVED_SECTORS = 32;

    private static final int MAX_DIRECTORY = 512;
    
    private final BlockDevice device;
    
    private String label;
    private String oemName;
    private FatType fatType;
    private int reservedSectors;
    private int fatCount;

    /**
     * Creates a new {@code SuperFloppyFormatter} for the specified
     * {@code BlockDevice}.
     *
     * @param device
     * @throws IOException on error accessing the specified {@code device}
     */
    public SuperFloppyFormatter(BlockDevice device) throws IOException {
        this.device = device;
        this.oemName = DEFAULT_OEM_NAME;
        this.reservedSectors = DEFAULT_RESERVED_SECTORS;
        this.fatCount = DEFAULT_FAT_COUNT;
        
        setFatType(defaultFatType());
    }

    /**
     * Returns the OEM name that will be written to the {@link BootSector}.
     *
     * @return the OEM name of the new file system
     */
    public String getOemName() {
        return oemName;
    }
    
    /**
     * Sets the OEM name of the boot sector.
     *
     * @param oemName the new OEM name
     * @see BootSector#setOemName(java.lang.String) 
     */
    public void setOemName(String oemName) {
        this.oemName = oemName;
    }

    /**
     * Sets the volume label of the file system to create.
     *
     * @param label the new file system label, may be {@code null}
     * @see FatFileSystem#setVolumeLabel(java.lang.String) 
     */
    public void setVolumeLabel(String label) {
        this.label = label;
    }

    /**
     * Returns the volume label that will be given to the new file system.
     *
     * @return the file system label, may be {@code null}
     * @see FatFileSystem#getVolumeLabel() 
     */
    public String getVolumeLabel() {
        return label;
    }
    
    /**
     * Writes the boot sector and file system to the device.
     *
     * @throws IOException on write error
     */
    public void format() throws IOException {

        final BootSector bs;

        final int sectorSize = device.getSectorSize();
        final int totalSectors = (int)(device.getSize() / sectorSize);
        final long dataSectors = totalSectors - reservedSectors;
        final long sizeInMB = (totalSectors * sectorSize) / (1024 * 1024);

        final int spc = fatType == FatType.FAT32 ?
            sectorsPerCluster32(sizeInMB) :
            sectorsPerCluster16(sizeInMB);

        if (fatType == FatType.FAT32) {
            bs = new Fat32BootSector(SF_BS);
            final Fat32BootSector f32bs = (Fat32BootSector) bs;
            final long clust32 = (dataSectors * sectorSize + fatCount*8) /
                (spc * sectorSize + fatCount*4);
            final long sectorsPerFat =
                    cdiv((clust32+2) * 4, device.getSectorSize());
                    
            f32bs.init();
            f32bs.setFsInfoSectorNr(1);
            f32bs.setSectorsPerFat(sectorsPerFat);
            final Random rnd = new Random(System.currentTimeMillis());
            f32bs.setFileSystemId(rnd.nextInt());
            f32bs.setVolumeLabel(label);

            /* make boot sector copy */
            
            /* create FS info sector */
            
            FsInfoSector fsi = new FsInfoSector(new byte[512]);
            fsi.init();
            fsi.write(device, f32bs.getFsInfoSectorNr() * BootSector.SIZE);
        } else {
            bs = new Fat16BootSector(SF_BS);
            final Fat16BootSector f16bs = (Fat16BootSector) bs;
            f16bs.init();
            
            final int rootDirEntries = calculateDefaultRootDirectorySize(
                    device.getSectorSize(), totalSectors);
            
            f16bs.setRootDirEntryCount(rootDirEntries);
            f16bs.setSectorsPerFat((Math.round(totalSectors / (spc *
                (device.getSectorSize() / fatType.getEntrySize()))) + 1));
        }
        
        bs.setNrReservedSectors(reservedSectors);
        bs.setNrFats(DEFAULT_FAT_COUNT);
        bs.setMediumDescriptor(MEDIUM_DESCRIPTOR_HD);
        bs.setSectorsPerTrack(DEFAULT_SECTORS_PER_TRACK);
        bs.setNrHeads(DEFULT_HEADS);
        bs.setOemName(oemName);
        bs.setSectorsPerCluster(spc);
        bs.setBytesPerSector(device.getSectorSize());
        bs.setSectorCount(totalSectors);
        bs.write(device);

        if (fatType == FatType.FAT32) {
            Fat32BootSector f32bs = (Fat32BootSector) bs;
            /* possibly writes the boot sector copy */
            f32bs.writeCopy(device);
        }

        final Fat fat = new Fat(bs);
                
        for (int i = 0; i < bs.getNrFats(); i++) {
            fat.write(device, FatUtils.getFatOffset(bs, i));
        }
        
        final FatLfnDirectory rootDir = new FatLfnDirectory(
                null, bs.getRootDirEntryCount());
                
        rootDir.write(device, FatUtils.getRootDirOffset(bs));
        
        if (label != null) {
            FatFileSystem fs = new FatFileSystem(device, false);
            fs.setVolumeLabel(label);
            fs.flush();
        }
        
        device.flush();
    }

    /**
     * Sets the type of FAT file system that will be created by this formatter.
     *
     * @param fatType the new FAT type
     */
    public void setFatType(FatType fatType) {
        /* parameter check */
        if (this.fatType == fatType) return;
        
        this.fatType = fatType;
    }

    /**
     * Returns the exact type of FAT the will be created by this formatter.
     *
     * @return the FAT type
     */
    public FatType getFatType() {
        return this.fatType;
    }

    private FatType defaultFatType() throws IOException {
        final long len = device.getSize();

        if (len < 1024 * 1024 * 1024) return FatType.FAT16;
        else return FatType.FAT32;
    }

    /**
     * Computes ceil(a/b).
     *
     * @param a parameter 1
     * @param b parameter 2
     * @return {@code ceil(a/b)}
     */
    private static long cdiv(long a, long b) {
        return (a + b - 1) / b;
    }
    
    private static int calculateDefaultRootDirectorySize(int bps, int nbTotalSectors) {
        int totalSize = bps * nbTotalSectors;
        // take a default 1/5 of the size for root max
        if (totalSize >= MAX_DIRECTORY * 5 * 32) { // ok take the max
            return MAX_DIRECTORY;
        } else {
            return totalSize / (5 * 32);
        }
    }

    /**
     * For FAT32, try to do the same as M$'s format command
     * (see http://www.win.tue.nl/~aeb/linux/fs/fat/fatgen103.pdf p. 20):
     * {@code
     * fs size <= 260M: 0.5k clusters
     * fs size <=   8G: 4k clusters
     * fs size <=  16G: 8k clusters
     * fs size >   16G: 16k clusters
     * }
     * 
     * @param size the FS size in MiB
     * @return the default sectors/cluster for FAT 32
     */
    private static int sectorsPerCluster32(long size) {
        return
            size > 16*1024 ? 32 :
            size >  8*1024 ? 16 :
            size >     260 ?  8 : 1;
    }

    private static int sectorsPerCluster16(long sizeInMB) {
        if (sizeInMB < 32) {
            return 1;
        } else if (sizeInMB < 64) {
            return 2;
        } else if (sizeInMB < 128) {
            return 4;
        } else if (sizeInMB < 256) {
            return 8;
        } else if (sizeInMB < 1024) {
            return 32;
        } else if (sizeInMB < 2048) {
            return 64;
        } else if (sizeInMB < 4096) {
            return 128;
        } else if (sizeInMB < 8192) {
            return 256;
        } else if (sizeInMB < 16384) {
            return 512;
        } else
            throw new IllegalArgumentException("disk too large for FAT16");
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
