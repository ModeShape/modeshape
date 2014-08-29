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
package org.modeshape.jcr.api.query;

import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.QueryResult;

/**
 * A specialization of the standard JCR {@link javax.jcr.query.Query} interface that adds the ModeShape-specific constant for the
 * {@link #FULL_TEXT_SEARCH full-text search} query language.
 */
public interface Query extends javax.jcr.query.Query {

    /**
     * A string constant representing the ModeShape full-text search query language.
     */
    public static final String FULL_TEXT_SEARCH = "search";

    /**
     * Specify whether the system content should be included in the query results.
     * 
     * @param includeSystemContent true if the system content should be included, or false if it should be excluded
     */
    public void includeSystemContent( boolean includeSystemContent );

    /**
     * Signal that the query, if currently {@link Query#execute() executing}, should be cancelled and stopped (with an exception).
     * This method does not block until the query is actually stopped.
     * 
     * @return true if the query was executing and will be cancelled, or false if the query was no longer running (because it had
     *         finished successfully or had already been cancelled) and could not be cancelled.
     */
    public boolean cancel();

    /**
     * Get the underlying and immutable Abstract Query Model representation of this query.
     * 
     * @return the string representation of this query's abstract query model; never null
     */
    public String getAbstractQueryModelRepresentation();

    /**
     * Generates a plan for the this query and returns a <code>{@link QueryResult}</code> object that contains no results (nodes
     * or rows) but does have a query plan.
     * <p>
     * If this <code>Query</code> contains a variable (see {@link javax.jcr.query.qom.BindVariableValue BindVariableValue}) which
     * has not been bound to a value (see {@link Query#bindValue}) then this method throws an <code>InvalidQueryException</code>.
     * 
     * @return a <code>QueryResult</code> object
     * @throws InvalidQueryException if the query contains an unbound variable.
     * @throws RepositoryException if another error occurs.
     * @see #execute()
     */
    public org.modeshape.jcr.api.query.QueryResult explain() throws InvalidQueryException, RepositoryException;

    @Override
    public org.modeshape.jcr.api.query.QueryResult execute() throws InvalidQueryException, RepositoryException;
}
