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
package org.modeshape.jcr.api.query.qom;

/**
 * Representation of a limit on the number of tuple results.
 */
public interface Limit {

    /**
     * Get the number of rows skipped before the results begin.
     * 
     * @return the offset; always 0 or a positive number
     */
    public int getOffset();

    /**
     * Get the maximum number of rows that are to be returned.
     * 
     * @return the maximum number of rows; always positive, or equal to {@link Integer#MAX_VALUE} if there is no limit
     */
    public int getRowLimit();

    /**
     * Determine whether this limit clause is necessary.
     * 
     * @return true if the number of rows is not limited and there is no offset, or false otherwise
     */
    public boolean isUnlimited();

    /**
     * Determine whether this limit clause defines an offset.
     * 
     * @return true if there is an offset, or false if there is no offset
     */
    public boolean isOffset();
}
