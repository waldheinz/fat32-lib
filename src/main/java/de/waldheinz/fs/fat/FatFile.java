/*
 * Copyright (C) 2009,2010 Matthias Treydte <mt@waldheinz.de>
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
 
package de.waldheinz.fs.fat;

import de.waldheinz.fs.AbstractFsObject;
import java.io.IOException;
import de.waldheinz.fs.FsFile;
import de.waldheinz.fs.ReadOnlyException;
import java.io.EOFException;
import java.nio.ByteBuffer;

/**
 * A File instance is the in-memory representation of a single file (chain of
 * clusters).
 * 
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 * @since 0.6
 */
public final class FatFile extends AbstractFsObject implements FsFile {
    private final FatDirectoryEntry entry;
    private final ClusterChain chain;
    
    private FatFile(FatDirectoryEntry myEntry, ClusterChain chain) {
        super(myEntry.isReadOnly());
        
        this.entry = myEntry;
        this.chain = chain;
    }
    
    static FatFile get(Fat fat, FatDirectoryEntry entry)
            throws IOException {
        
        if (entry.isDirectory())
            throw new IllegalArgumentException(entry + " is a directory");
            
        final ClusterChain cc = new ClusterChain(
                fat, entry.getStartCluster(), entry.isReadonlyFlag());
                
        if (entry.getLength() > cc.getLengthOnDisk()) throw new IOException(
                "entry is larger than associated cluster chain");
                
        return new FatFile(entry, cc);
    }
    
    /**
     * Returns the length of this file. This is actually the length that
     * is stored in the {@link FatDirectoryEntry} that is associated with this file.
     * 
     * @return long the length that is recorded for this file
     */
    @Override
    public long getLength() {
        checkValid();
        
        return entry.getLength();
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " [length=" + getLength() +
                ", first cluster=" + chain.getStartCluster() + "]";
    }

    @Override
    public void setLength(long length) throws ReadOnlyException, IOException {
        checkWritable();
        
        if (getLength() == length) return;
        
        updateTimeStamps(true);
        chain.setSize(length);
        
        this.entry.setStartCluster(chain.getStartCluster());
        this.entry.setLength(length);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p><p>
     * Unless this file is read-ony, this method also updates the
     * "last accessed" field in the {@link FatDirectoryEntry} that is associated with
     * this file.
     * </p>
     * 
     * @param offset {@inheritDoc}
     * @param dest {@inheritDoc}
     * @throws FileSystemException {@inheritDoc}
     * @see FatDirectoryEntry#setLastAccessed(long)
     */
    @Override
    public void read(long offset, ByteBuffer dest) throws IOException {
        checkValid();
        
        final int len = dest.remaining();
        
        if (len == 0) return;
        
        if (offset + len > getLength()) {
            throw new EOFException();
        }

        if (!chain.isReadOnly()) {
            updateTimeStamps(false);
        }
        
        chain.readData(offset, dest);
    }

    /**
     * <p>
     * {@inheritDoc}
     * </p><p>
     * Unless this file is read-ony, this method also updates the
     * "last accessed" and "last modified" fields in the {@link FatDirectoryEntry}
     * that is associated with this file.
     * </p>
     *
     * @param offset {@inheritDoc}
     * @param srcBuf {@inheritDoc}
     * @throws FileSystemException {@inheritDoc}
     */
    @Override
    public void write(long offset, ByteBuffer srcBuf)
            throws ReadOnlyException, IOException {

        checkWritable();

        updateTimeStamps(true);
        
        final long lastByte = offset + srcBuf.remaining();

        if (lastByte > getLength()) {
            setLength(lastByte);
        }
        
        chain.writeData(offset, srcBuf);
    }
    
    private void updateTimeStamps(boolean write) {
        final long now = System.currentTimeMillis();
        entry.setLastAccessed(now);
        
        if (write) {
            entry.setLastModified(now);
        }
    }
    
    @Override
    public void flush() throws IOException {
        checkWritable();
        /* nothing else to do */
    }
    
    /**
     * Returns the {@code ClusterChain} that holds the contents of
     * this {@code FatFile}.
     *
     * @return the file's {@code ClusterChain}
     */
    ClusterChain getChain() {
        checkValid();
        
        return chain;
    }
    
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!(obj instanceof FatFile)) return false;
        
        final FatFile other = (FatFile) obj;
        
        if (this.entry != other.entry &&
                (this.entry == null || !this.entry.equals(other.entry))) {
            return false;
        }
        
        if (this.chain != other.chain &&
                (this.chain == null || !this.chain.equals(other.chain))) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + (this.entry != null ? this.entry.hashCode() : 0);
        hash = 97 * hash + (this.chain != null ? this.chain.hashCode() : 0);
        return hash;
    }
    
}
