/*
 * $Id: AbstractFileSystem.java 4975 2009-02-02 08:30:52Z lsantha $
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
 
package com.meetwise.fs;

import java.io.IOException;
import java.util.HashMap;
import java.util.logging.Logger;

/**
 * Abstract class with common things in different FileSystem implementations
 * 
 * @author Fabien DUMINY
 */
public abstract class AbstractFileSystem implements FileSystem {

    private static final Logger log =
            Logger.getLogger(AbstractFileSystem.class.getName());

    private final boolean readOnly;
    private final BlockDevice api;
    private boolean closed;

    // cache of FSFile (key: FSDirectoryEntry)
    private HashMap<FSDirectoryEntry, FSFile> files =
            new HashMap<FSDirectoryEntry, FSFile>();

    // cache of FSDirectory (key: FSDirectoryEntry)
    private HashMap<FSDirectoryEntry, FSDirectory> directories =
            new HashMap<FSDirectoryEntry, FSDirectory>();

    /**
     * Construct an AbstractFileSystem in specified readOnly mode
     * 
     * @param api 
     * @param readOnly
     * @throws FileSystemException
     */
    public AbstractFileSystem(BlockDevice api, boolean readOnly)
            throws FileSystemException {
        
        this.api = api;
        this.closed = false;
        this.readOnly = readOnly;
    }
    
    /**
     * @see org.jnode.fs.FileSystem#close()
     */
    public void close() throws IOException {
        if (!isClosed()) {
            // if readOnly, nothing to do
            if (!isReadOnly()) {
                flush();
            }

            api.flush();
            files.clear();
            directories.clear();
            
            files = null;
            directories = null;
            closed = true;
        }
    }

    /**
     * Save the content that have been altered but not saved in the Device
     * 
     * @throws IOException
     */
    public void flush() throws IOException {
        flushFiles();
        flushDirectories();
    }

    /**
     * @return Returns the api.
     */
    public final BlockDevice getApi() {
        return api;
    }

    /**
     * @return Returns the FSApi.
     */
    public final BlockDevice getFSApi() {
        return api;
    }

    /**
     * @return if filesystem is closed.
     */
    public final boolean isClosed() {
        return closed;
    }

    /**
     * @return if filesystem is readOnly.
     */
    public final boolean isReadOnly() {
        return readOnly;
    }
    
    /**
     * Gets the file for the given entry.
     * 
     * @param entry
     * @return the FSFile object associated with entry
     * @throws IOException
     */
    public final synchronized FSFile getFile(FSDirectoryEntry entry) throws IOException {
        if (isClosed())
            throw new IOException("FileSystem is closed");

        FSFile file = files.get(entry);
        if (file == null) {
            file = createFile(entry);
            files.put(entry, file);
        }
        return file;
    }

    /**
     * Abstract method to create a new FSFile from the entry
     * 
     * @param entry
     * @return a new created FSFile
     * @throws IOException
     */
    protected abstract FSFile createFile(FSDirectoryEntry entry) throws IOException;

    /**
     * Flush all unsaved FSFile in our cache
     * 
     * @throws IOException
     */
    private final void flushFiles() throws IOException {
        log.info("flushing files ...");
        for (FSFile f : files.values()) {
            log.finer("flush: flushing file " + f);

            f.flush();
        }
    }

    /**
     * Gets the file for the given entry.
     * 
     * @param entry
     * @return the FSDirectory object associated with this entry
     * @throws IOException
     */
    public final synchronized FSDirectory getDirectory(FSDirectoryEntry entry) throws IOException {
        if (isClosed())
            throw new IOException("FileSystem is closed");

        FSDirectory dir = directories.get(entry);
        if (dir == null) {
            dir = createDirectory(entry);
            directories.put(entry, dir);
        }
        return dir;
    }

    /**
     * Abstract method to create a new directory from the given entry
     * 
     * @param entry
     * @return the new created FSDirectory
     * @throws IOException
     */
    protected abstract FSDirectory createDirectory(FSDirectoryEntry entry) throws IOException;

    /**
     * Flush all unsaved FSDirectory in our cache
     * 
     * @throws IOException
     */
    private final void flushDirectories() throws IOException {
        log.info("flushing directories ...");
        
        for (FSDirectory d : directories.values()) {
            log.finer("flush: flushing directory " + d);
            
            d.flush();
        }
    }
}
