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

import java.io.IOException;

/**
 * 
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class FatDirectory extends ClusterChainDirectory {

    private final FatDirectoryEntry entry;

    private FatDirectory(ClusterChain chain, FatDirectoryEntry entry) {
        super(chain, false);
        
        this.entry = entry;
    }
    
    public static FatDirectory read(FatDirectoryEntry entry, Fat fat)
            throws IOException {
        
        if (!entry.getEntry().isDirectory()) throw
                new IllegalArgumentException(entry + " is no directory");
                
        final ClusterChain chain = new ClusterChain(
                fat, entry.getStartCluster(),
                entry.getEntry().isReadOnly());
        
        final FatDirectory result = new FatDirectory(chain, entry);
        result.read();
        return result;
    }


    public static FatDirectory createSub(
            AbstractDirectory parent, Fat fat) throws IOException {

        final ClusterChain chain = new ClusterChain(fat, false);
        chain.setChainLength(1);
        
        final AbstractDirectoryEntry entryData =
                new AbstractDirectoryEntry(parent);
        
        final FatDirectoryEntry realEntry = FatDirectoryEntry.create(entryData);
        realEntry.getEntry().setFlags(AbstractDirectoryEntry.F_DIRECTORY);
        realEntry.setStartCluster(chain.getStartCluster());
        
        final FatDirectory result = new FatDirectory(chain, realEntry);

        /* add "." entry */
        
        final AbstractDirectoryEntry dot = new AbstractDirectoryEntry(result);
        dot.setFlags(AbstractDirectoryEntry.F_DIRECTORY);
        final FatDirectoryEntry dotEntry = FatDirectoryEntry.create(dot);
        dotEntry.setName(ShortName.DOT);
        dotEntry.setStartCluster((int) result.getStorageCluster());
        copyDateTimeFields(realEntry, dotEntry);
        result.addEntry(dot);

        /* add ".." entry */

        final AbstractDirectoryEntry dotDot =
                new AbstractDirectoryEntry(result);
        dotDot.setFlags(AbstractDirectoryEntry.F_DIRECTORY);
        final FatDirectoryEntry dotDotEntry = FatDirectoryEntry.create(dotDot);
        dotDotEntry.setName(ShortName.DOT_DOT);
        dotDotEntry.setStartCluster((int) parent.getStorageCluster());
        copyDateTimeFields(realEntry, dotDotEntry);
        result.addEntry(dotDot);

        result.flush();

        return result;
    }
    
    public FatDirectoryEntry getEntry() {
        return entry;
    }
    
    private static void copyDateTimeFields(FatDirectoryEntry src, FatDirectoryEntry dst) {
        dst.setCreated(src.getCreated());
        dst.setLastAccessed(src.getLastAccessed());
        dst.setLastModified(src.getLastModified());
    }
}
