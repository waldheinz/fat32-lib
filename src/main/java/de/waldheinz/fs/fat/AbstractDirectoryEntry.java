/*
 * Copyright (C) 2003-2009 JNode.org
 *               2009,2010 Matthias Treydte <mt@waldheinz.de>
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
 
package de.waldheinz.fs.fat;

/**
 * 
 * @author gbin
 * @author Matthias Treydte &lt;waldheinz at gmail.com&gt;
 */
final class AbstractDirectoryEntry extends FatObject {
    
    
    public void write(byte[] dest, int offset) {
        System.arraycopy(rawData, 0, dest, offset, SIZE);
        this.dirty = false;
    }
    

    public void setReadonlyFlag() {
        setFlags(getFlags() | F_READONLY);
    }

    public void setHidden() {
        setFlags(getFlags() | F_HIDDEN);
    }

    
    public void setSystem() {
        setFlags(getFlags() | F_SYSTEM);
    }

    public void setDirectory() {
        setFlags(F_DIRECTORY);
    }

    public void setLabel() {
        setFlags(F_VOLUME_ID);
    }
    
    public boolean isArchive() {
        return ((getFlags() & F_ARCHIVE) != 0);
    }

    public void setArchive() {
        setFlags(getFlags() | F_ARCHIVE);
    }

}
