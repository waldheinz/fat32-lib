/*
 * $Id: AbstractDirectory.java 4975 2009-02-02 08:30:52Z lsantha $
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * 
 *
 * @author Ewout Prangsma &lt;epr at jnode.org&gt;
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
abstract class AbstractDirectory {

    /**
     * The maximum length of the volume label.
     *
     * @see #setLabel(java.lang.String) 
     */
    public static final int MAX_LABEL_LENGTH = 11;
    
    private final List<AbstractDirectoryEntry> entries;
    private final boolean readOnly;
    private final boolean isRoot;
    
    private boolean dirty;
    private int capacity;
    private String volumeLabel;

    protected AbstractDirectory(
            int capacity, boolean readOnly, boolean isRoot) {
        
        this.entries = new ArrayList<AbstractDirectoryEntry>();
        this.capacity = capacity;
        this.readOnly = readOnly;
        this.isRoot = isRoot;
    }
    
    protected abstract void read(ByteBuffer data) throws IOException;
    
    protected abstract void write(ByteBuffer data) throws IOException;

    protected abstract long getStorageCluster();

    /**
     *
     * @param entryCount
     * @throws IOException on write error
     * @throws DirectoryFullException if the FAT12/16 root directory is full
     * @see #sizeChanged(long)
     * @see #checkEntryCount(int) 
     */
    protected abstract void changeSize(int entryCount)
            throws DirectoryFullException, IOException;

    /**
     * Checks if the entry count passed to {@link #changeSize(int)} is at
     * least one, as we always have at least the {@link ShortName#DOT dot}
     * entry.
     *
     * @param entryCount the entry count to check for validity
     * @throws IllegalArgumentException if {@code entryCount <= 0}
     */
    protected final void checkEntryCount(int entryCount)
            throws IllegalArgumentException {
        
        if (entryCount < 0) throw new IllegalArgumentException(
                "invalid entry count of " + entryCount);
    }

    public void setEntries(List<AbstractDirectoryEntry> newEntries) {
        if (newEntries.size() > capacity)
            throw new IllegalArgumentException("too many entries");
        
        this.entries.clear();
        this.entries.addAll(newEntries);
    }
    
    /**
     * 
     * @param newSize the new storage space for the directory in bytes
     * @see #changeSize(int) 
     */
    protected final void sizeChanged(long newSize) throws IOException {
        final long newCount = newSize / AbstractDirectoryEntry.SIZE;
        if (newCount > Integer.MAX_VALUE)
            throw new IOException("directory too large");
        
        this.capacity = (int) newCount;
    }

    public final AbstractDirectoryEntry getEntry(int idx) {
        return this.entries.get(idx);
    }
    
    /**
     * Returns the current capacity of this {@code AbstractDirectory}.
     *
     * @return the number of entries this directory can hold in its current
     *      storage space
     * @see #changeSize(int)
     */
    public final int getCapacity() {
        return this.capacity;
    }

    /**
     * The number of entries that are currently stored in this
     * {@code AbstractDirectory}.
     *
     * @return the current number of directory entries
     */
    public final int getEntryCount() {
        return this.entries.size();
    }
    
    public boolean isReadOnly() {
        return readOnly;
    }

    public final boolean isRoot() {
        return this.isRoot;
    }
    
    /**
     * Gets the number of directory entries in this directory. This is the
     * number of "real" entries in this directory, possibly plus one if a
     * volume label is set.
     * 
     * @return the number of entries in this directory
     */
    public int getSize() {
        return entries.size() + ((this.volumeLabel != null) ? 1 : 0);
    }
    
    /**
     * Returns the dirty.
     * 
     * @return boolean
     */
    public boolean isDirty() {
        if (dirty)  return true;
        
        for (int i = 0; i < entries.size(); i++) {
            final AbstractDirectoryEntry entry = entries.get(i);
            if (entry != null && entry.isDirty()) return true;
        }
        
        return false;
    }

    /**
     * Mark this directory as dirty.
     */
    protected final void setDirty() {
        this.dirty = true;
    }

    /**
     * Mark this directory as not dirty.
     */
    private final void resetDirty() {
        this.dirty = false;
    }
    
    /**
     * Flush the contents of this directory to the persistent storage
     */
    public void flush() throws IOException {
        final ByteBuffer data = ByteBuffer.allocate(
                getCapacity() * AbstractDirectoryEntry.SIZE);

        final int volLabelOffset;

        if (this.volumeLabel != null) {
            volLabelOffset = 32;
            final AbstractDirectoryEntry labelEntry =
                    new AbstractDirectoryEntry(this);
            labelEntry.setFlags(AbstractDirectoryEntry.F_VOLUME_ID);
            
            for (int i=0; i < volumeLabel.length(); i++) {
                labelEntry.getData()[i] = volumeLabel.getBytes()[i];
            }
            
            labelEntry.write(data.array(), 0);
        } else {
            volLabelOffset = 0;
        }
        
        for (int i=0; i < entries.size(); i++) {
            final AbstractDirectoryEntry entry = entries.get(i);

            if (entry != null) {
                entry.write(data.array(),
                        i * AbstractDirectoryEntry.SIZE + volLabelOffset);
            }
        }
        
        write(data);
        resetDirty();
    }
    
    protected final void read() throws IOException {
        final ByteBuffer data = ByteBuffer.allocate(
                getCapacity() * AbstractDirectoryEntry.SIZE);
                
        read(data);
        
        final byte[] src = data.array();

        for (int i = 0; i < getCapacity(); i++) {
            final int offset = i * 32;
            if (src[offset] != 0) {
                final AbstractDirectoryEntry entry =
                        new AbstractDirectoryEntry(this, src, offset);

                if (entry.isVolumeLabel()) {
                    parseVolumeLabel(entry);
                } else {
                    entries.add(entry);
                }
            }
        }
    }
    
    public void addEntry(AbstractDirectoryEntry e) throws IOException {
        assert (e != null);
        
        if (getSize() == getCapacity()) {
            changeSize(capacity + 1);
        }

        entries.add(e);
    }
    
    public void addEntries(AbstractDirectoryEntry[] entries)
            throws IOException {
        
        if (getSize() + entries.length > getCapacity()) {
            changeSize(getSize() + entries.length);
        }

        this.entries.addAll(Arrays.asList(entries));
    }
    
    public void removeEntry(AbstractDirectoryEntry entry) throws IOException {
        assert (entry != null);
        
        this.entries.remove(entry);
        changeSize(getSize());
    }
    
    public String getLabel() {
        if (!isRoot()) throw new AssertionError("not root");
        
        return volumeLabel;
    }

    /**
     *
     * @param label
     * @throws IllegalArgumentException if the label is too long
     */
    public void setLabel(String label)
            throws IllegalArgumentException, IOException {

        if (!isRoot()) throw new AssertionError("not root");
        if (label.length() > MAX_LABEL_LENGTH) throw new
                IllegalArgumentException("label too long");

        if (this.volumeLabel != null) {
            if (label == null) {
                changeSize(getSize() - 1);
                this.volumeLabel = null;
            } else {
                ShortName.checkValidChars(label.toCharArray());
                this.volumeLabel = label;
            }
        } else {
            if (label != null) {
                changeSize(getSize() + 1);
                ShortName.checkValidChars(label.toCharArray());
                this.volumeLabel = label;
            }
        }

        this.dirty = true;
    }
    
    private void parseVolumeLabel(AbstractDirectoryEntry entry) {
        if (!entry.isVolumeLabel()) throw new IllegalArgumentException();

        final StringBuilder sb = new StringBuilder();

        for (int i=0; i < 11; i++) {
            final byte b = entry.getData()[i];
            
            if (b != 0) {
                sb.append((char) b);
            } else {
                break;
            }
        }
        
        this.volumeLabel = sb.toString();
    }
}
