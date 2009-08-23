/*
 * $Id: FileSystemException.java 4975 2009-02-02 08:30:52Z lsantha $
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
 
package org.jnode.fs;

import java.io.IOException;

/**
 * 
 * @author Ewout Prangsma &lt; epr at jnode.org&gt;
 * @author Matthias Treydte
 */
public class FileSystemException extends IOException {
    private final static long serialVersionUID = 1;

    private final FileSystem fs;

    /**
     * 
     * @param fs the file system on which this exception occured
     */
    public FileSystemException(FileSystem fs) {
        
        this.fs = fs;
    }

    /**
     * @param fs the file system on which this exception occured
     * @param message
     * @param cause
     */
    public FileSystemException(FileSystem fs, String message, Throwable cause) {
        super(message, cause);
        
        this.fs = fs;
    }

    /**
     * @param fs 
     * @param cause
     */
    public FileSystemException(FileSystem fs, Throwable cause) {
        super(cause);
        
        this.fs = fs;
    }

    /**
     * 
     *
     * @param fs the file system on which this exception occured
     * @param s
     */
    public FileSystemException(FileSystem fs, String s) {
        super(s);
        
        this.fs = fs;
    }

    public FileSystem getFileSystem() {
        return this.fs;
    }
}
