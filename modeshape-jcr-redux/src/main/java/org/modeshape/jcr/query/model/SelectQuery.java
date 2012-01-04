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

import java.util.List;

/**
 * An implementation of {@link SelectQuery}.
 */
public class SelectQuery extends Query implements org.modeshape.jcr.api.query.qom.SelectQuery {
    private static final long serialVersionUID = 1L;

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
    public SelectQuery( Source source,
                        Constraint constraint,
                        List<? extends Ordering> orderings,
                        List<? extends Column> columns,
                        Limit limit,
                        boolean isDistinct ) {
        super(source, constraint, orderings, columns, limit != null ? limit : Limit.NONE, isDistinct);
    }

    @Override
    public Constraint getConstraint() {
        return constraint();
    }

    @Override
    public Source getSource() {
        return source();
    }

    @Override
    public Query withLimit( int rowLimit ) {
        if (getLimits().getRowLimit() == rowLimit) return this; // nothing to change
        return new SelectQuery(source(), constraint(), orderings(), columns(), getLimits().withRowLimit(rowLimit), isDistinct());
    }

    @Override
    public Query withOffset( int offset ) {
        if (getLimits().getRowLimit() == offset) return this; // nothing to change
        return new SelectQuery(source(), constraint(), orderings(), columns(), getLimits().withOffset(offset), isDistinct());
    }

}
