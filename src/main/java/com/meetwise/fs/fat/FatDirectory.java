
package com.meetwise.fs.fat;

import java.io.IOException;

/**
 * 
 *
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class FatDirectory extends ClusterChainDirectory {

    private final FatDirEntry entry;

    private FatDirectory(ClusterChain chain, FatDirEntry entry) {
        super(chain, false);
        
        this.entry = entry;
    }
    
    public static FatDirectory read(FatDirEntry entry, Fat fat)
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

    public static FatDirectory create(AbstractDirectory parent, Fat fat)
            throws IOException {
        
        final AbstractDirectoryEntry entryData =
                new AbstractDirectoryEntry(parent);
        final FatDirEntry realEntry = new FatDirEntry(entryData);
        realEntry.getEntry().setFlags(AbstractDirectoryEntry.F_DIRECTORY);
        final ClusterChain chain = new ClusterChain(fat, false);
        chain.setChainLength(1);
        final FatDirectory fatDir = new FatDirectory(chain, realEntry);
        fatDir.flush();
        realEntry.setStartCluster(fatDir.getStorageCluster());
        return fatDir;
    }
    
    public FatDirEntry getEntry() {
        return entry;
    }
    
}
