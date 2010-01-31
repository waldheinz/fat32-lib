
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


    public static FatDirectory createSub(
            AbstractDirectory parent, Fat fat) throws IOException {

        final ClusterChain chain = new ClusterChain(fat, false);
        chain.setChainLength(1);
        
        final AbstractDirectoryEntry entryData =
                new AbstractDirectoryEntry(parent);
        
        final FatDirEntry realEntry = new FatDirEntry(entryData);
        realEntry.getEntry().setFlags(AbstractDirectoryEntry.F_DIRECTORY);
        realEntry.setStartCluster(chain.getStartCluster());
        
        final FatDirectory result = new FatDirectory(chain, realEntry);
        
        final AbstractDirectoryEntry dot = new AbstractDirectoryEntry(result);
        dot.setFlags(AbstractDirectoryEntry.F_DIRECTORY);
        final FatDirEntry dotEntry = new FatDirEntry(dot);
        dotEntry.setName(ShortName.DOT);
        dotEntry.setStartCluster((int) result.getStorageCluster());
        copyDateTimeFields(realEntry, dotEntry);
        result.addEntry(dot);

        final AbstractDirectoryEntry dotDot =
                new AbstractDirectoryEntry(result);
        dotDot.setFlags(AbstractDirectoryEntry.F_DIRECTORY);
        final FatDirEntry dotDotEntry = new FatDirEntry(dotDot);
        dotDotEntry.setName(ShortName.DOT_DOT);
        dotDotEntry.setStartCluster((int) parent.getStorageCluster());
        copyDateTimeFields(realEntry, dotDotEntry);
        result.addEntry(dotDot);

        result.flush();

        return result;
    }
    
    public FatDirEntry getEntry() {
        return entry;
    }
    
    private static void copyDateTimeFields(FatDirEntry src, FatDirEntry dst) {
        dst.setCreated(src.getCreated());
        dst.setLastAccessed(src.getLastAccessed());
        dst.setLastModified(src.getLastModified());
    }
}
