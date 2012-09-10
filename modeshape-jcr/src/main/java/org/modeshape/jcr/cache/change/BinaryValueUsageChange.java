/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr.cache.change;

import org.modeshape.jcr.value.BinaryKey;

/**
 * An event signalizing that the usages have changed for a binary value with a specific key.
 */
public abstract class BinaryValueUsageChange extends Change {

    private static final long serialVersionUID = 1L;

    private final BinaryKey key;

    protected BinaryValueUsageChange( BinaryKey key ) {
        this.key = key;
        assert this.key != null;
    }

    /**
     * Get the binary key.
     * 
     * @return the key; never null
     */
    public BinaryKey getKey() {
        return key;
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof BinaryValueUsageChange) {
            BinaryValueUsageChange that = (BinaryValueUsageChange)obj;
            return this.getKey().equals(that.getKey()) && this.getClass().equals(that.getClass());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }
}
