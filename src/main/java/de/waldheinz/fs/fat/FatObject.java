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

import de.waldheinz.fs.FsObject;
import de.waldheinz.fs.ReadOnlyException;

/**
 * TODO: move this to fs package as AbstractFsObject
 *
 * @author Ewout Prangsma &lt; epr at jnode.org&gt;
 * @since 0.6
 */
public class FatObject implements FsObject {
    
    /** 
     * Is this object still valid?
     */
    private boolean valid;
    private boolean readOnly;
    
    FatObject(boolean readOnly) {
        this.valid = true;
        this.readOnly = readOnly;
    }

    /** 
     * An object is not valid anymore if it has been removed from the filesystem.
     * All invocations on methods (exception this method) of invalid objects 
     * must throw an IOException.
     * 
     * @return if this object is still valid
     */
    @Override
    public final boolean isValid() {
        return this.valid;
    }

    /**
     * Mark this object as invalid.
     */
    protected final void invalidate() {
        this.valid = false;
    }

    /**
     * @since 0.6
     */
    protected final void checkValid() {
        if (!isValid()) throw new IllegalStateException(
                this + " is not valid any more");
    }

    /**
     * @since 0.6
     */
    protected final void checkWritable() {
        checkValid();

        if (isReadOnly()) {
            throw new ReadOnlyException();
        }
    }
    
    @Override
    public final boolean isReadOnly() {
        return this.readOnly;
    }
    
}
