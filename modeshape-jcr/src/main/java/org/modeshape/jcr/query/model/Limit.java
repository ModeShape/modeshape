/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
