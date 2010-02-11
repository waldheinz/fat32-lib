
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
     * The default OEM name for file systems created by this class.
     */
    public final static String DEFAULT_OEM_NAME = "fat32lib"; //NOI18N
    
    private static final int MAX_DIRECTORY = 512;
    
    private final BlockDevice device;
    
    private String label;
    private String oemName;
    private FatType fatType;
    private int sectorsPerCluster;
    private int reservedSectors;
    private int fatCount;

    /**
     * Creates a new {@code SuperFloppyFormatter} for the specified
     * {@code BlockDevice}.
     *
     * @param device
     * @throws IOException on error accessing the specified {@code device}
     * @deprecated use the {@link #get(com.meetwise.fs.BlockDevice)
     *      method instead
     */
    @Deprecated
    public SuperFloppyFormatter(BlockDevice device) throws IOException {
        this.device = device;
        this.oemName = DEFAULT_OEM_NAME;
        this.fatCount = DEFAULT_FAT_COUNT;
        setFatType(fatTypeFromDevice());
    }

    /**
     * Retruns a {@code SuperFloppyFormatter} instance suitable for formatting
     * the specified device.
     *
     * @param dev the device that should be formatted
     * @return the formatter for the device
     * @throws IOException on error creating the formatter
     */
    public static SuperFloppyFormatter get(BlockDevice dev) throws IOException {
        return new SuperFloppyFormatter(dev);
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
        final int sectorSize = device.getSectorSize();
        final int totalSectors = (int)(device.getSize() / sectorSize);
        
        final FsInfoSector fsi;
        final BootSector bs;
        if (fatType == FatType.FAT32) {
            bs = new Fat32BootSector(device);
            final Fat32BootSector f32bs = (Fat32BootSector) bs;
            
            f32bs.init();
            f32bs.setFsInfoSectorNr(1);
            f32bs.setSectorsPerFat(sectorsPerFat(0, totalSectors));
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
            
            final int rootDirEntries = rootDirectorySize(
                    device.getSectorSize(), totalSectors);

            f16bs.setNrFats(fatCount);
            f16bs.setRootDirEntryCount(rootDirEntries);
            f16bs.setSectorsPerFat(sectorsPerFat(rootDirEntries, totalSectors));
            if (label != null) f16bs.setVolumeLabel(label);
            fsi = null;
        }
        
        bs.setNrReservedSectors(reservedSectors);
        bs.setMediumDescriptor(MEDIUM_DESCRIPTOR_HD);
        bs.setSectorsPerTrack(DEFAULT_SECTORS_PER_TRACK);
        bs.setNrHeads(DEFULT_HEADS);
        bs.setOemName(oemName);
        bs.setSectorsPerCluster(sectorsPerCluster);
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
            fs.close();
        }
        
        device.flush();
    }

    private int sectorsPerFat(int rootDirEntries, int totalSectors)
            throws IOException {
        
        final int bps = device.getSectorSize();
        final int rootDirSectors =
                ((rootDirEntries * 32) + (bps - 1)) / bps;
        final long tmp1 =
                totalSectors - (this.reservedSectors + rootDirSectors);
        int tmp2 = (256 * this.sectorsPerCluster) + this.fatCount;

        if (fatType == FatType.FAT32)
            tmp2 /= 2;

        final int result = (int) ((tmp1 + (tmp2 - 1)) / tmp2);
        
        return result;
    }
    
    /**
     * Determines a usable FAT type from the {@link #device} by looking at the
     * {@link BlockDevice#getSize() device size} only. The value returned
     * matches what's recommended in the FAT specification by MS.
     *
     * @return the suggested FAT type
     * @throws IOException on error determining the device's size
     */
    private FatType fatTypeFromDevice() throws IOException {
        final long sizeInMb = device.getSize() / (1024 * 1024);
        if (sizeInMb < 4) return FatType.FAT12;
        else if (sizeInMb < 512) return FatType.FAT16;
        else return FatType.FAT32;
    }
    
    /**
     * Returns the exact type of FAT the will be created by this formatter.
     *
     * @return the FAT type
     */
    public FatType getFatType() {
        return this.fatType;
    }

    /**
     * Sets the type of FAT that will be created by this
     * {@code SuperFloppyFormatter}.
     *
     * @param fatType the desired {@code FatType}
     * @return this {@code SuperFloppyFormatter}
     * @throws IOException on error setting the {@code fatType}
     * @throws IllegalArgumentException if {@code fatType} does not support the
     *      size of the device
     */
    public SuperFloppyFormatter setFatType(FatType fatType)
            throws IOException, IllegalArgumentException {
        
        if (fatType == null) throw new NullPointerException();

        switch (fatType) {
            case FAT12: case FAT16:
                this.reservedSectors = 1;
                break;
                
            case FAT32:
                this.reservedSectors = 32;
        }
        
        this.sectorsPerCluster = defaultSectorsPerCluster(fatType);
        this.fatType = fatType;
        
        return this;
    }
    
    private static int rootDirectorySize(int bps, int nbTotalSectors) {
        final int totalSize = bps * nbTotalSectors;
        if (totalSize >= MAX_DIRECTORY * 5 * 32) {
            return MAX_DIRECTORY;
        } else {
            return totalSize / (5 * 32);
        }
    }
    
    private int sectorsPerCluster32() throws IOException {
        if (this.reservedSectors != 32) throw new IllegalStateException(
                "number of reserved sectors must be 32");
        
        if (this.fatCount != 2) throw new IllegalStateException(
                "number of FATs must be 2");

        final long sectors = device.getSize() / device.getSectorSize();

        if (sectors <= 66600) throw new IllegalArgumentException(
                "disk too small for FAT32");
                
        return
                sectors > 67108864 ? 64 :
                sectors > 33554432 ? 32 :
                sectors > 16777216 ? 16 :
                sectors >   532480 ?  8 : 1;
    }
    
    private int sectorsPerCluster16() throws IOException {
        if (this.reservedSectors != 1) throw new IllegalStateException(
                "number of reserved sectors must be 1");

        if (this.fatCount != 2) throw new IllegalStateException(
                "number of FATs must be 2");

        final long sectors = device.getSize() / device.getSectorSize();
        
        if (sectors <= 8400) throw new IllegalArgumentException(
                "disk too small for FAT16");

        if (sectors > 4194304) throw new IllegalArgumentException(
                "disk too large for FAT16");

        return
                sectors > 2097152 ? 64 :
                sectors > 1048576 ? 32 :
                sectors >  524288 ? 16 :
                sectors >  262144 ?  8 :
                sectors >   32680 ?  4 : 2;
    }
    
    private int defaultSectorsPerCluster(FatType fatType) throws IOException {
        switch (fatType) {
            case FAT12:
                return sectorsPerCluster12();

            case FAT16:
                return sectorsPerCluster16();

            case FAT32:
                return sectorsPerCluster32();
                
            default:
                throw new AssertionError();
        }
    }

    private int sectorsPerCluster12() throws IOException {
        int result = 1;
        
        final long sectors = device.getSize() / device.getSectorSize();

        while (sectors / result > Fat16BootSector.MAX_FAT12_CLUSTERS) {
            result *= 2;
            if (result * device.getSectorSize() > 4096) throw new
                    IllegalArgumentException("disk too large for FAT12");
        }
        
        return result;
    }
    
}
