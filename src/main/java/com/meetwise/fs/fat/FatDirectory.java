/*
 * $Id: FatDirectory.java 4975 2009-02-02 08:30:52Z lsantha $
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

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * A directory that is stored in a cluster chain.
 *
 * @author Ewout Prangsma &lt; epr at jnode.org&gt;
 */
final class FatDirectory extends AbstractDirectory {
    private final ClusterChain chain;
    
    private FatDirectory(ClusterChain chain, boolean readOnly, boolean isRoot) {
        super(chain.getFat(),
                (int)(chain.getLengthOnDisk() / AbstractDirectoryEntry.SIZE),
                readOnly, isRoot);
        
        this.chain = chain;   
    }

    public static FatDirectory read(ClusterChain chain,
            boolean readOnly, boolean root) throws IOException {
        
        final FatDirectory result = new FatDirectory(chain, readOnly, root);
        result.read();
        return result;
    }
    
    public static FatDirectory create(ClusterChain cc,
            long parentCluster, boolean root) throws IOException {

        cc.setChainLength(1);
        final FatDirectory result = new FatDirectory(cc, false, root);
        result.initialize(cc.getStartCluster(), parentCluster);
        result.flush();
        return result;
    }
    
    @Override
    protected void read(ByteBuffer data) throws IOException {
        this.chain.readData(0, data);
    }

    @Override
    protected void write(ByteBuffer data) throws IOException {
        final long trueSize = chain.setSize(data.capacity());
        chain.writeData(0, data);
        
        if (trueSize > data.capacity()) {
            final int rest = (int) (trueSize - data.capacity());
            final ByteBuffer fill = ByteBuffer.allocate(rest);
            chain.writeData(data.capacity(), fill);
        }
    }

    /**
     * 
     * @return
     */
    @Override
    protected long getStorageCluster() {
        return isRoot() ? 0 : chain.getStartCluster();
    }
    
    @Override
    protected boolean canChangeSize(int entryCount) {
        /* TODO: check this */
        return true;
    }
}
