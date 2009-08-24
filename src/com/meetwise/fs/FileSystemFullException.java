/*
 * $Id: FileSystemFullException.java 4975 2009-02-02 08:30:52Z lsantha $
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

/**
 * Gets thrown when a file system can not store the additional data that
 * is tried to be written.
 * 
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
public class FileSystemFullException extends FileSystemException {
    private final static long serialVersionUID = 1;
    
    /**
     * Creates a new instance of {@code FileSystemFullException}.
     *
     * @param fs the {@link FileSystem} on which this exception was generated
     * @param message a message describing futher details, may be {@code null}
     */
    public FileSystemFullException(FileSystem fs, String message) {
        super(fs, message);
    }
}
