/*
 * $Id: FatBasicDirEntry.java 4975 2009-02-02 08:30:52Z lsantha $
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

/**
 * @author gbin
 */
class FatBasicDirEntry extends FatObject implements FatConstants {

    protected final byte[] rawData = new byte[32];

    FatBasicDirEntry(AbstractDirectory dir) {
        super(dir.getFileSystem());
    }

    FatBasicDirEntry(AbstractDirectory dir, byte[] src, int offset) {
        super(dir.getFileSystem());
        
        System.arraycopy(src, offset, rawData, 0, 32);
    }

    public void write(byte[] dest, int offset) {
        System.arraycopy(rawData, 0, dest, offset, 32);
    }
}
