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

import com.meetwise.fs.FileSystemException;
import java.io.IOException;
import com.meetwise.fs.FSFile;
import java.nio.ByteBuffer;

/**
 * A File instance is the in-memory representation of a single file (chain of
 * clusters).
 * 
 * @author Ewout Prangsma &lt; epr at jnode.org&gt;
 */
public final class FatFile extends ClusterChain implements FSFile {
    private long length;
    private FatDirectory dir;
    
    private boolean isDir;
    
    private final FatDirEntry myEntry;
    
    /**
     * Constructor used for the FAT32 root directory.
     *
     * @param fs
     * @param startCluster
     */
    FatFile(FatFileSystem fs, long startCluster) {
        super(fs, startCluster);
        
        this.myEntry = null;
        this.length = getLengthOnDisk();
        this.isDir = true;
    }

    FatFile(FatFileSystem fs, FatDirEntry myEntry,
            long startCluster, long length, boolean isDir) {

        super(fs, startCluster);
        
        this.myEntry = myEntry;
        this.length = length;
        this.isDir = isDir;
    }

    /**
     * Returns the length.
     * 
     * @return long
     */
    @Override
    public long getLength() {
        return length;
    }

    /**
     * Gets the directory contained in this file.
     * 
     * @return Directory
     * @throws IOException on read error
     */
    synchronized FatDirectory getDirectory() throws IOException {
        if (!isDir) throw new UnsupportedOperationException();
        
        if (dir == null) {
            dir = new FatLfnDirectory(getFileSystem(), this);
        }
        
        return dir;
    }
    
    /**
     * Flush any changes in this file to persistent storage
     * @throws IOException
     */
    @Override
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

    @Override
    public void setLength(long length) throws FileSystemException {
        if (this.length == length) return;

        final long clusterSize = getFileSystem().getClusterSize();
        final int nrClusters = (int) ((length + clusterSize - 1) / clusterSize);

        super.setChainLength(nrClusters);

        if (myEntry != null)
            this.myEntry.setStartCluster(super.getStartCluster());

        this.length = length;
        
        if (myEntry != null)
            this.myEntry.updateLength(length);
    }

    @Override
    public void read(long offset, ByteBuffer dest) throws FileSystemException {
        final int len = dest.remaining();
        final long max = isDir ? getLengthOnDisk() : getLength();

        if (offset + len > max)
            throw new FileSystemException(getFileSystem(),
                    "can not read beyond EOF"); //NOI18N

        readData(offset, dest);
    }
    
    @Override
    public void write(long offset, ByteBuffer srcBuf)
            throws FileSystemException {
            
        final long max = (isDir) ? getLengthOnDisk() : getLength();
        if (offset > max)
            throw new FileSystemException(this.getFileSystem(),
                    "can not write beyond EOF"); //NOI18N
                    
        int len = srcBuf.remaining();
        if (offset + len > max)
            setLength(offset + len);

        writeData(offset, srcBuf);
    }
    
}
