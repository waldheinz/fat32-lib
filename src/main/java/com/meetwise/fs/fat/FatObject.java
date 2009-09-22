/*
 * $Id: FatObject.java 4975 2009-02-02 08:30:52Z lsantha $
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

import com.meetwise.fs.FSObject;
import com.meetwise.fs.FileSystem;

/**
 * @author Ewout Prangsma &lt; epr at jnode.org&gt;
 */
abstract class FatObject implements FSObject {

    /** 
     * The filesystem I'm a part of
     */
    private final FatFileSystem fs;

    /** 
     * Is this object still valid?
     */
    private boolean valid;
    
    public FatObject(FatFileSystem fs) {
        this.fs = fs;
        this.valid = true;
    }

    /** 
     * An object is not valid anymore if it has been removed from the filesystem.
     * All invocations on methods (exception this method) of invalid objects 
     * must throw an IOException.
     * 
     * @return if this object is still valid
     */
    public final boolean isValid() {
        return valid;
    }

    /**
     * Mark this object as invalid.
     */
    protected void invalidate() {
        valid = false;
    }

    /**
     * Gets the filesystem I'm a part of.
     *
     * @return the file system of this object
     */
    public final FileSystem getFileSystem() {
        return fs;
    }

    public final FatFileSystem getFatFileSystem() {
        return fs;
    }
}
