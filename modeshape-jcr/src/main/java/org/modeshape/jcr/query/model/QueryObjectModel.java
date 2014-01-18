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

import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Source;
import org.modeshape.jcr.query.JcrQuery;
import org.modeshape.jcr.query.JcrQueryContext;
import org.modeshape.jcr.query.parse.QueryParser;
import org.modeshape.jcr.query.plan.PlanHints;
import org.modeshape.jcr.value.Path;

/**
 * Implementation of {@link QueryObjectModel} that represents a {@link Query select query}.
 */
public class QueryObjectModel extends JcrQuery implements javax.jcr.query.qom.QueryObjectModel {

    /**
     * Creates a new JCR {@link Query} by specifying the query statement itself, the language in which the query is stated, the
     * {@link QueryCommand} representation and, optionally, the node from which the query was loaded. The language must be a
     * string from among those returned by {@code QueryManager#getSupportedQueryLanguages()}.
     * 
     * @param context the context that was used to create this query and that will be used to execute this query; may not be null
     * @param statement the original statement as supplied by the client; may not be null
     * @param language the language obtained from the {@link QueryParser}; may not be null
     * @param query the parsed query representation; may not be null
     * @param hints any hints that are to be used; may be null if there are no hints
     * @param storedAtPath the path at which this query was stored, or null if this is not a stored query
     */
    public QueryObjectModel( JcrQueryContext context,
                             String statement,
                             String language,
                             SelectQuery query,
                             PlanHints hints,
                             Path storedAtPath ) {
        super(context, statement, language, query, hints, storedAtPath);

    }

    @Override
    protected SelectQuery query() {
        return (SelectQuery)super.query();
    }

    @Override
    public Source getSource() {
        return query().source();
    }

    @Override
    public Constraint getConstraint() {
        return query().constraint();
    }

    @Override
    public javax.jcr.query.qom.Column[] getColumns() {
        return query().getColumns();
    }

    @Override
    public javax.jcr.query.qom.Ordering[] getOrderings() {
        return query().getOrderings();
    }
}
