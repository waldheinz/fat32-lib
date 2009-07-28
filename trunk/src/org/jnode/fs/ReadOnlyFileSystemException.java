/*
 * $Id: ReadOnlyFileSystemException.java 4975 2009-02-02 08:30:52Z lsantha $
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
 * @author Fabien DUMINY
 * 
 * New exception allowing to handle cases where a FileSystem is mounted readOnly
 */
public class ReadOnlyFileSystemException extends IOException {

    private final static long serialVersionUID = 1;

    public ReadOnlyFileSystemException() {
        super("read-only file system");
    }
    /**
     * @param message
     * @param cause
     */
    public ReadOnlyFileSystemException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }

    /**
     * @param cause
     */
    public ReadOnlyFileSystemException(Throwable cause) {
        super();
        initCause(cause);
    }

    /**
     * @param message
     */
    public ReadOnlyFileSystemException(String message) {
        super(message);
    }
}
