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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.jcip.annotations.Immutable;
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

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.QueryCommand#limits()
     */
    public Limit limits() {
        return limits;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.QueryCommand#orderings()
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.QueryCommand#columns()
     */
    public List<? extends Column> columns() {
        return columns;
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
        return new Query(source, constraint, orderings(), columns, limits(), true);
    }

    /**
     * Create a copy of this query, but one in which there may be duplicate rows in the results.
     * 
     * @return the copy of the query with potentially duplicate result rows; never null
     */
    public Query noDistinct() {
        return new Query(source, constraint, orderings(), columns, limits(), false);
    }

    /**
     * Create a copy of this query, but one that uses the supplied constraint.
     * 
     * @param constraint the constraint that should be used; never null
     * @return the copy of the query that uses the supplied constraint; never null
     */
    public Query constrainedBy( Constraint constraint ) {
        return new Query(source, constraint, orderings(), columns, limits(), distinct);
    }

    /**
     * Create a copy of this query, but one whose results should be ordered by the supplied orderings.
     * 
     * @param orderings the result ordering specification that should be used; never null
     * @return the copy of the query that uses the supplied ordering; never null
     */
    public Query orderedBy( List<Ordering> orderings ) {
        return new Query(source, constraint, orderings, columns, limits(), distinct);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.QueryCommand#withLimit(int)
     */
    public Query withLimit( int rowLimit ) {
        if (limits().rowLimit() == rowLimit) return this; // nothing to change
        return new Query(source, constraint, orderings(), columns, limits().withRowLimit(rowLimit), distinct);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.QueryCommand#withOffset(int)
     */
    public Query withOffset( int offset ) {
        if (limits().offset() == offset) return this; // nothing to change
        return new Query(source, constraint, orderings(), columns, limits().withOffset(offset), distinct);
    }

    /**
     * Create a copy of this query, but that returns results with the supplied columns.
     * 
     * @param columns the columns of the results; may not be null
     * @return the copy of the query returning the supplied result columns; never null
     */
    public Query returning( List<Column> columns ) {
        return new Query(source, constraint, orderings(), columns, limits(), distinct);
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
        return new Query(source, constraint, newOrderings, columns, limits(), distinct);
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
        return new Query(source, constraint, orderings(), newColumns, limits(), distinct);
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
        return hc;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof Query) {
            Query that = (Query)obj;
            if (this.hc != that.hc) return false;
            if (this.distinct != that.distinct) return false;
            if (!this.source.equals(that.source)) return false;
            if (!ObjectUtil.isEqualWithNulls(this.limits(), that.limits())) return false;
            if (!ObjectUtil.isEqualWithNulls(this.constraint, that.constraint)) return false;
            if (!ObjectUtil.isEqualWithNulls(this.columns, that.columns)) return false;
            if (!ObjectUtil.isEqualWithNulls(this.orderings(), that.orderings())) return false;
            return true;
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
}
