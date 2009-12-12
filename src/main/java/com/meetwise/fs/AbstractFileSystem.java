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
    private final BlockDevice blockDevice;
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
        
        this.blockDevice = api;
        this.closed = false;
        this.readOnly = readOnly;
    }
    
    @Override
    public void close() throws IOException {
        if (!isClosed()) {
            if (!isReadOnly()) {
                flush();
            }

            blockDevice.flush();
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
    @Override
    public void flush() throws IOException {
        flushFiles();
        flushDirectories();
    }

    /**
     * @return Returns the api.
     */
    public final BlockDevice getApi() {
        return blockDevice;
    }

    /**
     * @return Returns the FSApi.
     */
    public final BlockDevice getBlockDevice() {
        return blockDevice;
    }
    
    /**
     * @return if filesystem is closed.
     */
    @Override
    public final boolean isClosed() {
        return closed;
    }

    /**
     * @return if filesystem is readOnly.
     */
    @Override
    public final boolean isReadOnly() {
        return readOnly;
    }
    
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
