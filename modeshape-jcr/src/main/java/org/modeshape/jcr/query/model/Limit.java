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
package org.modeshape.jcr.query.model;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;

/**
 * A specification of the limit placed on a query, detailing the a maximum number of result rows and an offset for the first row
 * in the results.
 */
@Immutable
public class Limit implements LanguageObject, org.modeshape.jcr.api.query.qom.Limit {
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

    @Override
    public final int getOffset() {
        return offset;
    }

    @Override
    public final int getRowLimit() {
        return rowLimit;
    }

    @Override
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

    public final boolean isLimitedToSingleRowWithNoOffset() {
        return rowLimit == 1 && offset == 0;
    }

    @Override
    public final boolean isOffset() {
        return offset > 0;
    }

    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    @Override
    public int hashCode() {
        return rowLimit;
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Limit) {
            Limit that = (Limit)obj;
            return this.offset == that.offset && this.rowLimit == that.rowLimit;
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }

    /**
     * Create a new Limit object that contains the same offset as this object but a new maximum row count.
     * 
     * @param rowLimit the maximum row count for the new Limit object
     * @return the new Limit object
     */
    public Limit withRowLimit( int rowLimit ) {
        return new Limit(rowLimit, offset);
    }

    /**
     * Create a new Limit object that contains the same maximum row count as this object but a new offset.
     * 
     * @param offset the offset for the new Limit object
     * @return the new Limit object
     */
    public Limit withOffset( int offset ) {
        return new Limit(rowLimit, offset);
    }

}
