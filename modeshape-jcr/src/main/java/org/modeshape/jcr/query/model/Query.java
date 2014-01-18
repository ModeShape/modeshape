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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.common.util.ObjectUtil;

/**
 * 
 */
@Immutable
public class Query implements QueryCommand {
    private static final long serialVersionUID = 1L;

    public static final boolean IS_DISTINCT_DEFAULT = false;

    private final List<? extends Ordering> orderings;
    private final Limit limits;
    private final Source source;
    private final Constraint constraint;
    private final List<? extends Column> columns;
    private final boolean distinct;
    private final int hc;
    private transient javax.jcr.query.qom.Column[] columnArray;
    private transient javax.jcr.query.qom.Ordering[] orderingArray;

    /**
     * Create a new query that uses the supplied source.
     * 
     * @param source the source
     * @throws IllegalArgumentException if the source is null
     */
    public Query( Source source ) {
        super();
        CheckArg.isNotNull(source, "source");
        this.orderings = Collections.<Ordering>emptyList();
        this.limits = Limit.NONE;
        this.source = source;
        this.constraint = null;
        this.columns = Collections.<Column>emptyList();
        this.distinct = IS_DISTINCT_DEFAULT;
        this.hc = HashCode.compute(this.source, this.constraint, this.columns, this.distinct);
    }

    /**
     * Create a new query that uses the supplied source, constraint, orderings, columns and limits.
     * 
     * @param source the source
     * @param constraint the constraint (or composite constraint), or null or empty if there are no constraints
     * @param orderings the specifications of how the results are to be ordered, or null if the order is to be implementation
     *        determined
     * @param columns the columns to be included in the results, or null or empty if there are no explicit columns and the actual
     *        result columns are to be implementation determiend
     * @param limit the limit for the results, or null if all of the results are to be included
     * @param isDistinct true if duplicates are to be removed from the results
     * @throws IllegalArgumentException if the source is null
     */
    public Query( Source source,
                  Constraint constraint,
                  List<? extends Ordering> orderings,
                  List<? extends Column> columns,
                  Limit limit,
                  boolean isDistinct ) {
        CheckArg.isNotNull(source, "source");
        this.source = source;
        this.constraint = constraint;
        this.columns = columns != null ? columns : Collections.<Column>emptyList();
        this.distinct = isDistinct;
        this.orderings = orderings != null ? orderings : Collections.<Ordering>emptyList();
        this.limits = limit != null ? limit : Limit.NONE;
        this.hc = HashCode.compute(this.source, this.constraint, this.columns, this.distinct);
    }

    @Override
    public Limit getLimits() {
        return limits;
    }

    @Override
    public List<? extends Ordering> orderings() {
        return orderings;
    }

    /**
     * Get the source for the results.
     * 
     * @return the query source; never null
     */
    public Source source() {
        return source;
    }

    /**
     * Get the constraints, if there are any.
     * 
     * @return the constraint; may be null
     */
    public Constraint constraint() {
        return constraint;
    }

    @Override
    public List<? extends Column> columns() {
        return columns;
    }

    @Override
    public javax.jcr.query.qom.Ordering[] getOrderings() {
        if (orderingArray == null) {
            // this is idempotent ...
            orderingArray = orderings.toArray(new javax.jcr.query.qom.Ordering[orderings.size()]);
        }
        return orderingArray;
    }

    @Override
    public javax.jcr.query.qom.Column[] getColumns() {
        if (columnArray == null) {
            // this is idempotent ...
            columnArray = columns.toArray(new javax.jcr.query.qom.Column[columns.size()]);
        }
        return columnArray;
    }

    /**
     * Determine whether this query is to return only distinct values.
     * 
     * @return true if the query is to remove duplicate tuples, or false otherwise
     */
    public boolean isDistinct() {
        return distinct;
    }

    /**
     * Create a copy of this query, but one in which there are no duplicate rows in the results.
     * 
     * @return the copy of the query with no duplicate result rows; never null
     */
    public Query distinct() {
        return new Query(source, constraint, orderings(), columns, getLimits(), true);
    }

    /**
     * Create a copy of this query, but one in which there may be duplicate rows in the results.
     * 
     * @return the copy of the query with potentially duplicate result rows; never null
     */
    public Query noDistinct() {
        return new Query(source, constraint, orderings(), columns, getLimits(), false);
    }

    /**
     * Create a copy of this query, but one that uses the supplied constraint.
     * 
     * @param constraint the constraint that should be used; never null
     * @return the copy of the query that uses the supplied constraint; never null
     */
    public Query constrainedBy( Constraint constraint ) {
        return new Query(source, constraint, orderings(), columns, getLimits(), distinct);
    }

    /**
     * Create a copy of this query, but one whose results should be ordered by the supplied orderings.
     * 
     * @param orderings the result ordering specification that should be used; never null
     * @return the copy of the query that uses the supplied ordering; never null
     */
    public Query orderedBy( List<Ordering> orderings ) {
        return new Query(source, constraint, orderings, columns, getLimits(), distinct);
    }

    @Override
    public Query withLimit( int rowLimit ) {
        if (getLimits().getRowLimit() == rowLimit) return this; // nothing to change
        return new Query(source, constraint, orderings(), columns, getLimits().withRowLimit(rowLimit), distinct);
    }

    @Override
    public Query withOffset( int offset ) {
        if (getLimits().getOffset() == offset) return this; // nothing to change
        return new Query(source, constraint, orderings(), columns, getLimits().withOffset(offset), distinct);
    }

    /**
     * Create a copy of this query, but that returns results with the supplied columns.
     * 
     * @param columns the columns of the results; may not be null
     * @return the copy of the query returning the supplied result columns; never null
     */
    public Query returning( List<Column> columns ) {
        return new Query(source, constraint, orderings(), columns, getLimits(), distinct);
    }

    /**
     * Create a copy of this query, but that returns results that are ordered by the {@link #orderings() orderings} of this column
     * as well as those supplied.
     * 
     * @param orderings the additional orderings of the result rows; may no be null
     * @return the copy of the query returning the supplied result columns; never null
     */
    public Query adding( Ordering... orderings ) {
        List<Ordering> newOrderings = null;
        if (this.orderings() != null) {
            newOrderings = new ArrayList<Ordering>(orderings());
            for (Ordering ordering : orderings) {
                newOrderings.add(ordering);
            }
        } else {
            newOrderings = Arrays.asList(orderings);
        }
        return new Query(source, constraint, newOrderings, columns, getLimits(), distinct);
    }

    /**
     * Create a copy of this query, but that returns results that include the columns specified by this query as well as the
     * supplied columns.
     * 
     * @param columns the additional columns that should be included in the the results; may not be null
     * @return the copy of the query returning the supplied result columns; never null
     */
    public Query adding( Column... columns ) {
        List<Column> newColumns = null;
        if (this.columns != null) {
            newColumns = new ArrayList<Column>(this.columns);
            for (Column column : columns) {
                newColumns.add(column);
            }
        } else {
            newColumns = Arrays.asList(columns);
        }
        return new Query(source, constraint, orderings(), newColumns, getLimits(), distinct);
    }

    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    @Override
    public int hashCode() {
        return hc;
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Query) {
            Query that = (Query)obj;
            if (this.hc != that.hc) return false;
            if (this.distinct != that.distinct) return false;
            if (!this.source.equals(that.source)) return false;
            if (!ObjectUtil.isEqualWithNulls(this.getLimits(), that.getLimits())) return false;
            if (!ObjectUtil.isEqualWithNulls(this.constraint, that.constraint)) return false;
            if (!ObjectUtil.isEqualWithNulls(this.columns, that.columns)) return false;
            if (!ObjectUtil.isEqualWithNulls(this.orderings(), that.orderings())) return false;
            return true;
        }
        return false;
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}
