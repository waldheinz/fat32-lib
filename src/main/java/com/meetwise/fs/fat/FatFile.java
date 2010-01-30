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
 * @author Ewout Prangsma &lt;epr at jnode.org&gt;
 */
final class FatFile extends ClusterChain implements FSFile {

    private final FatDirEntry myEntry;
    
    FatFile(Fat fat, FatDirEntry myEntry, boolean readOnly) {
        super(fat, myEntry.getStartCluster(), readOnly);

        if (myEntry.getEntry().isDirectory())
            throw new IllegalArgumentException(myEntry + " is a directory");

        this.myEntry = myEntry;
    }

    /**
     * Returns the length.
     * 
     * @return long
     */
    @Override
    public long getLength() {
        return myEntry.getLength();
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " [length=" + getLength() +
                ", first cluster=" + getStartCluster() + "]";
    }

    @Override
    public void setLength(long length) throws FileSystemException {
        if (getLength() == length) return;
        
        try {
            super.setSize(length);
        } catch (IOException ex) {
            throw new FileSystemException(null, ex);
        }

        this.myEntry.setStartCluster(super.getStartCluster());    
        this.myEntry.setLength(length);
    }

    @Override
    public void read(long offset, ByteBuffer dest) throws FileSystemException {
        final int len = dest.remaining();

        if (offset + len > getLength())
            throw new FileSystemException(null,
                    "can not read beyond EOF"); //NOI18N

        try {
            readData(offset, dest);
        } catch (IOException ex) {
            throw new FileSystemException(null, ex);
        }
    }
    
    @Override
    public void write(long offset, ByteBuffer srcBuf)
            throws FileSystemException {
            
        int len = srcBuf.remaining();
        if (offset + len > getLength())
            setLength(offset + len);
        try {
            writeData(offset, srcBuf);
        } catch (IOException ex) {
            throw new FileSystemException(null, ex);
        }
    }

    @Override
    public void flush() throws IOException {
        /* nothing to do */
    }
    
}
