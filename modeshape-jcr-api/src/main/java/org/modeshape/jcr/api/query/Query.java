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

}
