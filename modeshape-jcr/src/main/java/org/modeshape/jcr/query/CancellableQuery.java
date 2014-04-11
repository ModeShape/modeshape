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
    QueryResults execute() throws QueryCancelledException, RepositoryException;

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
