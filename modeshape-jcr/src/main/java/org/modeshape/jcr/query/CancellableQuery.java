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
import org.modeshape.jcr.api.query.QueryCancelledException;

/**
 * A simple interface that allows tracking an executing query that can be cancelled.
 */
public interface CancellableQuery {

    /**
     * Execute the query and get the results. Note that this method can be called by multiple threads, and all will block until
     * the query execution has completed. Subsequent calls to this method will return the same results.
     * 
     * @return the query results.
     * @throws QueryCancelledException if the query was cancelled
     * @throws RepositoryException if there was a problem executing the query
     */
    QueryResults getResults() throws QueryCancelledException, RepositoryException;

    /**
     * Cancel the query if it is currently running. Note that this method does not block until the query is cancelled; it merely
     * marks the query as being cancelled, and the query will terminate at its next available opportunity. Also, subsequent calls
     * to this method have no affect.
     * 
     * @return true if the query was executing and will be cancelled, or false if the query was no longer running (because it had
     *         finished successfully or had already been cancelled) and could not be cancelled.
     */
    boolean cancel();
}
