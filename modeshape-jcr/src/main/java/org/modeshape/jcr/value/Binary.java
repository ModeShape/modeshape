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
package org.modeshape.jcr.value;

import java.io.Serializable;
import org.modeshape.common.annotation.Immutable;

/**
 * Value holder for binary data. Binary instances are not mutable.
 */
@Immutable
public interface Binary extends Comparable<Binary>, Serializable, org.modeshape.jcr.api.Binary {

    /**
     * Get the length of this binary data.
     * <p>
     * Note that this method, unlike the standard {@link javax.jcr.Binary#getSize()} method, does not throw an exception.
     * </p>
     * 
     * @return the number of bytes in this binary data
     */
    @Override
    public long getSize();

    /**
     * Get the key for the binary value.
     * 
     * @return the key; never null
     */
    public BinaryKey getKey();
}
