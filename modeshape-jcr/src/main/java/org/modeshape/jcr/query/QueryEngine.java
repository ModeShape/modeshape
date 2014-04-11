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

import java.util.Map;
import java.util.Set;
import javax.jcr.RepositoryException;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.RepositoryIndexes;
import org.modeshape.jcr.api.query.QueryCancelledException;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.query.model.QueryCommand;
import org.modeshape.jcr.query.plan.PlanHints;
import org.modeshape.jcr.query.validate.Schemata;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public interface QueryEngine {

    /**
     * Execute the supplied query by planning, optimizing, and then processing it.
     * 
     * @param context the context in which the query should be executed
     * @param query the query that is to be executed
     * @return the query results; never null
     * @throws IllegalArgumentException if the context or query references are null
     * @throws QueryCancelledException if the query was cancelled
     * @throws RepositoryException if there was a problem executing the query
     */
    QueryResults execute( final QueryContext context,
                          QueryCommand query ) throws QueryCancelledException, RepositoryException;

    /**
     * Create a new context for query execution.
     * 
     * @param context the context in which the query is being executed; may not be null
     * @param repositoryCache the repository cache that should be used to load results; may be null if no results are to be loaded
     * @param workspaceNames the name of each workspace to be queried, or an empty set if all the workspaces should be queried;
     *        may not be null
     * @param overriddenNodeCachesByWorkspaceName the NodeCache instances that should be used to load results, which will be used
     *        instead of the RepositoryCache's NodeCache for a given workspace name; may be null or empty
     * @param schemata the schemata
     * @param indexDefns the definitions for the currently-defined indexes; never null
     * @param nodeTypes the snapshot of node types; may not be null
     * @param bufferManager the buffer manager; may not be null
     * @param hints the hints, or null if there are no hints
     * @param variables the mapping of variables and values, or null if there are no such variables
     * @return the context; never null
     * @throws IllegalArgumentException if the context, workspace name, or schemata are null
     */
    QueryContext createQueryContext( ExecutionContext context,
                                     RepositoryCache repositoryCache,
                                     Set<String> workspaceNames,
                                     Map<String, NodeCache> overriddenNodeCachesByWorkspaceName,
                                     Schemata schemata,
                                     RepositoryIndexes indexDefns,
                                     NodeTypes nodeTypes,
                                     BufferManager bufferManager,
                                     PlanHints hints,
                                     Map<String, Object> variables );

    /**
     * Signal that the engine is no longer needed and should clean up and/or close any resources.
     */
    void shutdown();

}
