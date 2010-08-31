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
package org.modeshape.jcr.query;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.query.QueryResults;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.parse.QueryParser;
import org.modeshape.graph.query.plan.PlanHints;
import org.modeshape.graph.query.validate.Schemata;
import org.modeshape.jcr.query.qom.JcrAbstractQuery;
import org.modeshape.jcr.query.qom.JcrLiteral;

/**
 * Implementation of {@link Query} that represents a {@link QueryCommand query command}.
 */
@NotThreadSafe
public class JcrQuery extends JcrAbstractQuery {

    private QueryCommand query;
    private final PlanHints hints;
    private final Map<String, Object> variables;

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
    public JcrQuery( JcrQueryContext context,
                     String statement,
                     String language,
                     QueryCommand query,
                     PlanHints hints,
                     Path storedAtPath ) {
        super(context, statement, language, storedAtPath);
        assert query != null;
        this.query = query;
        this.hints = hints;
        this.variables = new HashMap<String, Object>();
    }

    protected QueryCommand query() {
        return query;
    }

    /**
     * Get the underlying and immutable Abstract Query Model representation of this query.
     * 
     * @return the AQM representation; never null
     */
    public QueryCommand getAbstractQueryModel() {
        return query;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.Query#execute()
     */
    @SuppressWarnings( "deprecation" )
    public QueryResult execute() throws RepositoryException {
        context.isLive();
        // Submit immediately to the workspace graph ...
        Schemata schemata = context.getSchemata();
        QueryResults result = context.execute(query, hints, variables);
        checkForProblems(result.getProblems());
        if (Query.XPATH.equals(language)) {
            return new XPathQueryResult(context, statement, result, schemata);
        } else if (Query.SQL.equals(language)) {
            return new JcrSqlQueryResult(context, statement, result, schemata);
        }
        return new JcrQueryResult(context, statement, result, schemata);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return language + " -> " + statement + "\n" + StringUtil.createString(' ', Math.min(language.length() - 3, 0))
               + "AQM -> " + query;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.Query#bindValue(java.lang.String, javax.jcr.Value)
     */
    @Override
    public void bindValue( String varName,
                           Value value ) throws IllegalArgumentException, RepositoryException {
        CheckArg.isNotNull(varName, "varName");
        CheckArg.isNotNull(value, "value");
        variables.put(varName, JcrLiteral.rawValue(value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.Query#getBindVariableNames()
     */
    @Override
    public String[] getBindVariableNames() {
        Set<String> keys = variables.keySet();
        return keys.toArray(new String[keys.size()]);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.Query#setLimit(long)
     */
    @Override
    public void setLimit( long limit ) {
        if (limit > Integer.MAX_VALUE) limit = Integer.MAX_VALUE;
        query = query.withLimit((int)limit); // may not actually change if the limit matches the existing query
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.Query#setOffset(long)
     */
    @Override
    public void setOffset( long offset ) {
        if (offset > Integer.MAX_VALUE) offset = Integer.MAX_VALUE;
        query = query.withOffset((int)offset); // may not actually change if the offset matches the existing query
    }
}
