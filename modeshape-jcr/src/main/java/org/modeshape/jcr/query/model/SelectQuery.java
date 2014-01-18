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
