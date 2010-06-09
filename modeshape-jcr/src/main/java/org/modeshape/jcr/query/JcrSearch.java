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

import javax.jcr.RepositoryException;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.query.QueryResults;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.parse.QueryParser;
import org.modeshape.graph.query.validate.Schemata;
import org.modeshape.jcr.query.qom.JcrAbstractQuery;

/**
 * A {@link Query} implementation that represents a search.
 */
public class JcrSearch extends JcrAbstractQuery {

    public static final int MAXIMUM_RESULTS_FOR_FULL_TEXT_SEARCH_QUERIES = Integer.MAX_VALUE;

    /**
     * Creates a new JCR {@link Query} by specifying the query statement itself, the language in which the query is stated, the
     * {@link QueryCommand} representation and, optionally, the node from which the query was loaded. The language must be a
     * string from among those returned by {@code QueryManager#getSupportedQueryLanguages()}.
     * 
     * @param context the context that was used to create this query and that will be used to execute this query; may not be null
     * @param statement the original statement as supplied by the client; may not be null
     * @param language the language obtained from the {@link QueryParser}; may not be null
     * @param storedAtPath the path at which this query was stored, or null if this is not a stored query
     */
    public JcrSearch( JcrQueryContext context,
                      String statement,
                      String language,
                      Path storedAtPath ) {
        super(context, statement, language, storedAtPath);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.Query#execute()
     */
    public QueryResult execute() throws RepositoryException {
        // Submit immediately to the workspace graph ...
        Schemata schemata = context.getSchemata();
        QueryResults result = context.search(statement, MAXIMUM_RESULTS_FOR_FULL_TEXT_SEARCH_QUERIES, 0);
        checkForProblems(result.getProblems());
        return new JcrQueryResult(context, statement, result, schemata);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return language + " -> " + statement;
    }

    @Override
    public void bindValue( String varName,
                           Value value ) throws IllegalArgumentException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    @Override
    public String[] getBindVariableNames() throws RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    @Override
    public void setLimit( long limit ) {
        throw new IllegalStateException();
    }

    @Override
    public void setOffset( long offset ) {
        throw new IllegalStateException();
    }
}
