/*
 * $Id: FatFile.java 4975 2009-02-02 08:30:52Z lsantha $
 *
 * Copyright (C) 2003-2009 JNode.org
 *
 * This library is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as published
 * by the Free Software Foundation; either version 2.1 of the License, or
 * (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful, but 
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public 
 * License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this library; If not, write to the Free Software Foundation, Inc., 
 * 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
 */
 
package com.meetwise.fs.fat;

import com.meetwise.fs.BlockDevice;
import java.io.IOException;
import java.nio.ByteBuffer;
import com.meetwise.fs.FSFile;
import com.meetwise.fs.FileSystemException;
import com.meetwise.fs.ReadOnlyFileSystemException;

/**
 * A File instance is the in-memory representation of a single file (chain of
 * clusters).
 * 
 * @author Ewout Prangsma &lt; epr at jnode.org&gt;
 */
public final class FatFile extends FatObject implements FSFile {
    private long startCluster;
    private long length;
    private FatDirectory dir;
    
    private boolean isDir;
    
    private final int clusterSize;
    private final FatDirEntry myEntry;
    
    /**
     * Constructor used for the FAT32 root directory.
     *
     * @param fs
     * @param startCluster
     */
    public FatFile(FatFileSystem fs, long startCluster) {
        super(fs);

        this.myEntry = null;
        this.startCluster = startCluster;
        this.clusterSize = fs.getClusterSize();
        this.length = getLengthOnDisk();
        this.isDir = true;
    }

    public FatFile(FatFileSystem fs, FatDirEntry myEntry,
            long startCluster, long length, boolean isDir) {
        
        super(fs);
        
        this.startCluster = startCluster;
        this.myEntry = myEntry;
        this.length = length;
        this.clusterSize = fs.getClusterSize();
        this.isDir = isDir;
        
        if (length > getLengthOnDisk()) throw new AssertionError();
    }

    public synchronized void read(long fileOffset, ByteBuffer destBuf)
            throws FileSystemException {

        int len = destBuf.remaining();

        final long max = isDir ? getLengthOnDisk() : getLength();
        
        if (fileOffset + len > max)
            throw new FileSystemException(this.getFatFileSystem(),
                    "can not read beyond EOF"); //NOI18N

        final FatFileSystem fs = getFatFileSystem();
        final long[] chain = fs.getFat().getChain(startCluster);
        final BlockDevice api = fs.getApi();

        int chainIdx = (int) (fileOffset / clusterSize);
        if (fileOffset % clusterSize != 0) {
            int clusOfs = (int) (fileOffset % clusterSize);
            int size = Math.min(len,
                    (int) (clusterSize - (fileOffset % clusterSize) - 1));
            destBuf.limit(destBuf.position() + size);

            try {
                api.read(getDevOffset(chain[chainIdx], clusOfs), destBuf);
            } catch (IOException ex) {
                throw new FileSystemException(fs, ex);
            }
            
            fileOffset += size;
            len -= size;
            chainIdx++;
        }

        while (len > 0) {
            int size = Math.min(clusterSize, len);
            destBuf.limit(destBuf.position() + size);
            
            try {
                api.read(getDevOffset(chain[chainIdx], 0), destBuf);
            } catch (IOException ex) {
                throw new FileSystemException(fs, ex);
            }

            len -= size;
            chainIdx++;
        }
    }

    public synchronized void write(long fileOffset, ByteBuffer srcBuf)
            throws FileSystemException {
        
        if (getFileSystem().isReadOnly())
            throw new ReadOnlyFileSystemException(this.getFatFileSystem(),
                    "write in readonly filesystem"); //NOI18N

        final long max = (isDir) ? getLengthOnDisk() : getLength();
        if (fileOffset > max)
            throw new FileSystemException(this.getFatFileSystem(),
                    "can not write beyond EOF"); //NOI18N

        int len = srcBuf.remaining();
        if (fileOffset + len > max)
            setLength(fileOffset + len);

        final FatFileSystem fs = getFatFileSystem();
        final long[] chain = fs.getFat().getChain(getStartCluster());
        final BlockDevice api = fs.getApi();

        int chainIdx = (int) (fileOffset / clusterSize);
        if (fileOffset % clusterSize != 0) {
            int clusOfs = (int) (fileOffset % clusterSize);
            int size = Math.min(len,
                    (int) (clusterSize - (fileOffset % clusterSize) - 1));
            srcBuf.limit(srcBuf.position() + size);

            try {
                api.write(getDevOffset(chain[chainIdx], clusOfs), srcBuf);
            } catch (IOException ex) {
                throw new FileSystemException(fs, ex);
            }
            
            fileOffset += size;
            len -= size;
            chainIdx++;
        }
        
        while (len > 0) {
            int size = Math.min(clusterSize, len);
            srcBuf.limit(srcBuf.position() + size);

            try {
                api.write(getDevOffset(chain[chainIdx], 0), srcBuf);
            } catch (IOException ex) {
                throw new FileSystemException(fs, ex);
            }

            len -= size;
            chainIdx++;
        }
    }

    /**
     * Sets the length.
     * 
     * @param length The length to set
     */
    public synchronized void setLength(long length) throws FileSystemException {

        if (getFileSystem().isReadOnly()) throw new 
                ReadOnlyFileSystemException(this.getFatFileSystem(), "readonly filesystem"); //NOI18N

        if (this.length == length) return;
        
        final FatFileSystem fs = getFatFileSystem();
        final Fat fat = fs.getFat();
        final int nrClusters = (int) ((length + clusterSize - 1) / clusterSize);
        
        if (this.length == 0) {
            final long[] chain = fat.allocNew(nrClusters);
            this.startCluster = chain[0];
            if (myEntry != null)
                this.myEntry.setStartCluster((int) startCluster);
        } else {
            final long[] chain = fs.getFat().getChain(startCluster);

            if (nrClusters != chain.length) {
                if (nrClusters > chain.length) {
                    // Grow
                    int count = nrClusters - chain.length;
                    while (count > 0) {
                        fat.allocAppend(getStartCluster());
                        count--;
                    }
                } else {
                    // Shrink
                    fat.setEof(chain[nrClusters - 1]);
                    for (int i = nrClusters; i < chain.length; i++) {
                        fat.setFree(chain[i]);
                    }
                }
            }
        }
        
        this.length = length;
        if (myEntry != null)
            this.myEntry.updateLength(length);
    }

    /**
     * Returns the length.
     * 
     * @return long
     */
    public long getLength() {
        return length;
    }

    /**
     * Gets the size this file occupies on disk
     * 
     * @return long
     */
    public long getLengthOnDisk() {
        if (getStartCluster() == 0) return 0;
        
        final FatFileSystem fs = getFatFileSystem();
        final long[] chain = fs.getFat().getChain(getStartCluster());
        return ((long) chain.length) * fs.getClusterSize();
    }
    
    /**
     * Returns the startCluster.
     * 
     * @return long
     */
    public long getStartCluster() {
        return startCluster;
    }

    /**
     * Gets the directory contained in this file.
     * 
     * @return Directory
     * @throws IOException on read error
     */
    public synchronized FatDirectory getDirectory() throws IOException {
        if (!isDir) throw new UnsupportedOperationException();
        
        if (dir == null) {
            dir = new FatLfnDirectory(getFatFileSystem(), this);
        }
        
        return dir;
    }

    /**
     * Calculates the device offset (0-based) for the given cluster and offset
     * within the cluster.
     * @param cluster
     * @param clusterOffset
     * @return long
     * @throws FileSystemException
     */
    protected long getDevOffset(long cluster, int clusterOffset)
            throws FileSystemException {
        
        final FatFileSystem fs = getFatFileSystem();
        final long filesOffset;

        try {
            filesOffset = FatUtils.getFilesOffset(fs.getBootSector());
        } catch (IOException ex) {
            throw new FileSystemException(fs, ex);
        }
        return filesOffset + clusterOffset +
                ((cluster - FatUtils.FIRST_CLUSTER) * clusterSize);
    }

    /**
     * Flush any changes in this file to persistent storage
     * @throws IOException
     */
    public void flush() throws IOException {
        if (dir != null) {
            dir.flush();
        }
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " [length=" + getLength() +
                ", first cluster=" + getStartCluster() + "]";
    }
    
}
