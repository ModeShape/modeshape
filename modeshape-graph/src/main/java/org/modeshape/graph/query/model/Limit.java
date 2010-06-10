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
package org.modeshape.graph.query.model;

import net.jcip.annotations.Immutable;
import org.modeshape.common.util.CheckArg;

/**
 * 
 */
@Immutable
public class Limit implements LanguageObject {
    private static final long serialVersionUID = 1L;

    public static final Limit NONE = new Limit(Integer.MAX_VALUE, 0);

    private final int offset;
    private final int rowLimit;

    /**
     * Create a limit on the number of rows.
     * 
     * @param rowLimit the maximum number of rows
     * @throws IllegalArgumentException if the row limit is negative
     */
    public Limit( int rowLimit ) {
        CheckArg.isNonNegative(rowLimit, "rowLimit");
        this.rowLimit = rowLimit;
        this.offset = 0;
    }

    /**
     * Create a limit on the number of rows and the number of initial rows to skip.
     * 
     * @param rowLimit the maximum number of rows
     * @param offset the number of rows to skip before beginning the results
     * @throws IllegalArgumentException if the row limit is negative, or if the offset is negative
     */
    public Limit( int rowLimit,
                  int offset ) {
        CheckArg.isNonNegative(rowLimit, "rowLimit");
        CheckArg.isNonNegative(offset, "offset");
        this.rowLimit = rowLimit;
        this.offset = offset;
    }

    /**
     * Get the number of rows skipped before the results begin.
     * 
     * @return the offset; always 0 or a positive number
     */
    public final int offset() {
        return offset;
    }

    /**
     * Get the maximum number of rows that are to be returned.
     * 
     * @return the maximum number of rows; never negative, or equal to {@link Integer#MAX_VALUE} if there is no limit
     */
    public final int rowLimit() {
        return rowLimit;
    }

    /**
     * Determine whether this limit clause is necessary.
     * 
     * @return true if the number of rows is not limited and there is no offset, or false otherwise
     */
    public final boolean isUnlimited() {
        return rowLimit == Integer.MAX_VALUE && offset == 0;
    }

    /**
     * Determine whether this limit clause defines a maximum limit
     * 
     * @return true if the number of rows are limited, or false if there is no limit to the number of rows
     */
    public final boolean hasRowLimited() {
        return rowLimit != Integer.MAX_VALUE;
    }

    /**
     * Determine whether this limit clause defines an offset.
     * 
     * @return true if there is an offset, or false if there is no offset
     */
    public final boolean isOffset() {
        return offset > 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return rowLimit;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Limit) {
            Limit that = (Limit)obj;
            return this.offset == that.offset && this.rowLimit == that.rowLimit;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitable#accept(org.modeshape.graph.query.model.Visitor)
     */
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }

    public Limit withRowLimit( int rowLimit ) {
        return new Limit(rowLimit, offset);
    }

    public Limit withOffset( int offset ) {
        return new Limit(rowLimit, offset);
    }

}
