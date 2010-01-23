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

import com.meetwise.fs.FSDirectory;
import java.io.IOException;
import java.util.Iterator;
import com.meetwise.fs.FSDirectoryEntry;
import com.meetwise.fs.FileSystemException;

/**
 * @author Ewout Prangsma &lt; epr at jnode.org&gt;
 */
abstract class FatDirectory extends AbstractDirectory implements FSDirectory {

    private final boolean root;

    protected FatDirEntry labelEntry;

    /**
     * Constructor for Directory.
     * 
     * @param fs
     * @param chain
     * @throws FileSystemException 
     */
    public FatDirectory(ClusterChain chain, boolean isRoot)
            throws FileSystemException, IOException {
        
        super(chain);

        this.root = isRoot;
        if (isRoot)
            findLabelEntry();
    }

    // for root
    protected FatDirectory(Fat fat, boolean readOnly)
            throws FileSystemException, IOException {
        
        super(fat, readOnly);
        
        root = true;
        findLabelEntry();
    }

    String getLabel() {
        if (labelEntry != null) return labelEntry.getName();
        else return null;
    }
    
    private void findLabelEntry() {
        for (int i=0; i < entries.size(); i++) {
            if (entries.get(i) instanceof FatDirEntry) {
                FatDirEntry e = (FatDirEntry) entries.get(i);
                if (e.isLabel()) {
                    labelEntry = e;
                    entries.set(i, null);
                    break;
                }
            }
        }
    }
    
    void setLabel(String label) throws IOException {
        if (!root) {
            throw new IOException(
                    "volume name change on non-root directory"); //NOI18N
        }

        if (label != null) {
            Iterator<FSDirectoryEntry> i = iterator();
            FatDirEntry current;
            while (labelEntry == null && i.hasNext()) {
                current = (FatDirEntry) i.next();
                if (current.isLabel() &&
                        !(current.isHidden() && current.isReadonly() && current.isSystem())) {
                    labelEntry = current;
                }
            }

            if (labelEntry == null) {
                labelEntry = addFatFile(label);
                labelEntry.setLabel();
            }

            labelEntry.setName(label);

            if (label.length() > 8) {
                labelEntry.setExt(label.substring(8));
            } else {
                labelEntry.setExt("");
            }
        } else {
            labelEntry = null;
        }
    }
}
