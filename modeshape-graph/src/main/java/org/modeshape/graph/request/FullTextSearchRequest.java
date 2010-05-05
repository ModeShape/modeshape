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
package org.modeshape.graph.request;

import java.util.List;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.query.QueryResults.Columns;
import org.modeshape.graph.query.QueryResults.Statistics;

/**
 * A {@link Request} to perform a full-text search on a graph.
 */
public class FullTextSearchRequest extends SearchRequest {

    private static final long serialVersionUID = 1L;

    private final String expression;
    private final String workspaceName;
    private final int maxResults;
    private final int offset;

    /**
     * Create a new request to execute the supplied query against the name workspace.
     * 
     * @param fullTextSearch the full-text search to be performed; may not be null
     * @param workspace the name of the workspace to be queried
     * @param maxResults the maximum number of results that are to be returned; always positive
     * @param offset the number of initial results to skip, or 0 if the first results are to be returned
     * @throws IllegalArgumentException if the query or workspace name is null, if the maxResults is not positive, or if the
     *         offset is negative
     */
    public FullTextSearchRequest( String fullTextSearch,
                                  String workspace,
                                  int maxResults,
                                  int offset ) {
        CheckArg.isNotEmpty(fullTextSearch, "fullTextSearch");
        CheckArg.isNotNull(workspace, "workspace");
        CheckArg.isPositive(maxResults, "maxResults");
        CheckArg.isNonNegative(offset, "offset");
        this.expression = fullTextSearch;
        this.workspaceName = workspace;
        this.maxResults = maxResults;
        this.offset = offset;
    }

    /**
     * Get the full-text search expression that is to be executed.
     * 
     * @return the full-text search expression; never null and never empty
     */
    public String expression() {
        return expression;
    }

    /**
     * Get the name of the workspace in which the node exists.
     * 
     * @return the name of the workspace; never null
     */
    public String workspace() {
        return workspaceName;
    }

    /**
     * Get the maximum number of results that should be returned.
     * 
     * @return the maximum number of results that are to be returned; always positive
     */
    public int maxResults() {
        return maxResults;
    }

    /**
     * Get the number of initial search results that should be excluded from the {@link #getTuples() tuples} included on this
     * request.
     * 
     * @return the number of initial results to skip, or 0 if the first results are to be returned
     */
    public int offset() {
        return offset;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return expression.hashCode();
    }

    /**
     * Get the specification of the columns for the {@link #getTuples() results}.
     * 
     * @return the column specifications; never null
     */
    public Columns getResultColumns() {
        return super.columns();
    }

    /**
     * Set the results for this request.
     * 
     * @param resultColumns the definition of the result columns
     * @param tuples the result values
     * @param statistics the statistics, or null if there are none
     */
    public void setResults( Columns resultColumns,
                            List<Object[]> tuples,
                            Statistics statistics ) {
        super.doSetResults(resultColumns, tuples, statistics);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (this.getClass().isInstance(obj)) {
            FullTextSearchRequest that = (FullTextSearchRequest)obj;
            if (!this.expression().equals(that.expression())) return false;
            if (!this.workspace().equals(that.workspace())) return false;
            if (this.offset() != that.offset()) return false;
            if (this.maxResults() != that.maxResults()) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "search the \"" + workspaceName + "\" workspace with \"" + expression + "\"";
    }

    @Override
    public RequestType getType() {
        return RequestType.FULL_TEXT_SEARCH;
    }
}
