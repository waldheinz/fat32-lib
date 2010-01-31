
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
    public final static String DEFAULT_OEM_NAME = "fat32lib"; //NOI18N

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

        final int clusterSize = spc * sectorSize;
        final FsInfoSector fsi;
        
        if (fatType == FatType.FAT32) {
            bs = new Fat32BootSector(device);

            final Fat32BootSector f32bs = (Fat32BootSector) bs;
            final long clust32 = (dataSectors * sectorSize + fatCount*8) /
                (clusterSize + fatCount*4);
            final long sectorsPerFat =
                    cdiv((clust32+2) * 4, device.getSectorSize());
                    
            f32bs.init();
            f32bs.setFsInfoSectorNr(1);
            f32bs.setSectorsPerFat(sectorsPerFat);
            final Random rnd = new Random(System.currentTimeMillis());
            f32bs.setFileSystemId(rnd.nextInt());
            f32bs.setNrFats(fatCount);
            f32bs.setVolumeLabel(label);
            
            /* create FS info sector */
            fsi = FsInfoSector.create(f32bs);
        } else {
            bs = new Fat16BootSector(device);
            final Fat16BootSector f16bs = (Fat16BootSector) bs;
            f16bs.init();
            
            final int rootDirEntries = calculateDefaultRootDirectorySize(
                    device.getSectorSize(), totalSectors);

            f16bs.setNrFats(fatCount);
            f16bs.setRootDirEntryCount(rootDirEntries);
            f16bs.setSectorsPerFat((Math.round(totalSectors / (spc *
                (sectorSize / fatType.getEntrySize()))) + 1));
            if (label != null) f16bs.setVolumeLabel(label);
            fsi = null;
        }
        
        bs.setNrReservedSectors(reservedSectors);
        bs.setMediumDescriptor(MEDIUM_DESCRIPTOR_HD);
        bs.setSectorsPerTrack(DEFAULT_SECTORS_PER_TRACK);
        bs.setNrHeads(DEFULT_HEADS);
        bs.setOemName(oemName);
        bs.setSectorsPerCluster(spc);
        bs.setBytesPerSector(device.getSectorSize());
        bs.setSectorCount(totalSectors);
        bs.write();
        
        if (fatType == FatType.FAT32) {
            Fat32BootSector f32bs = (Fat32BootSector) bs;
            /* possibly writes the boot sector copy */
            f32bs.writeCopy(device);
        }
        
        final Fat fat = Fat.create(bs, 0);
        
        final AbstractDirectory rootDirStore;
        if (fatType == FatType.FAT32) {
            final Fat32BootSector f32bs = (Fat32BootSector) bs;
            final ClusterChain rootDirChain = new ClusterChain(fat, false);
            rootDirStore = ClusterChainDirectory.createRoot(rootDirChain);
            f32bs.setRootDirFirstCluster(rootDirChain.getStartCluster());
            fsi.setFreeClusterCount(fat.getFreeClusterCount());
            fsi.setLastAllocatedCluster(fat.getLastAllocatedCluster());
            fsi.write();
        } else {
            rootDirStore = Fat16RootDirectory.create((Fat16BootSector) bs);
        }
        
        final FatLfnDirectory rootDir = new FatLfnDirectory(rootDirStore, fat);
        
        rootDir.flush();
        
        for (int i = 0; i < bs.getNrFats(); i++) {
            fat.writeCopy(FatUtils.getFatOffset(bs, i));
        }
        
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
    
}
