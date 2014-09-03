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
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.query.model.QueryCommand;
import org.modeshape.jcr.query.plan.PlanHints;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;

/**
 * The context in which queries are executed. This interface is implemented in the components that instantiate {@link JcrQuery}
 * instances and which can provide the required functionality needed by the JcrQuery and {@link JcrQueryResult} classes.
 */
public interface JcrQueryContext {

    /**
     * Method that verifies that this query context is still valid for use.
     * 
     * @throws RepositoryException if session has been closed and is no longer usable.
     */
    void checkValid() throws RepositoryException;

    /**
     * Get the execution context that should be used by the query.
     * 
     * @return the execution context; never null
     */
    ExecutionContext getExecutionContext();

    /**
     * Get the buffer manager.
     * 
     * @return the buffer manager; never null
     */
    BufferManager getBufferManager();

    /**
     * Get the name of the current workspace.
     * 
     * @return the workspace name; never null
     */
    String getWorkspaceName();

    /**
     * Store the query at the given location.
     * 
     * @param absolutePath the location at which the query should be stored; may not be null
     * @param nodeType the node type for the node; may not be null
     * @param language the query language; may not be null
     * @param statement the query statement; may not be null
     * @return the node at which the query was stored; never null
     * @throws RepositoryException if there was a problem storing the query
     */
    Node storeQuery( String absolutePath,
                     Name nodeType,
                     String language,
                     String statement ) throws RepositoryException;

    /**
     * Create a query object that can be {@link CancellableQuery#execute() executed} and optionally
     * {@link CancellableQuery#cancel() cancelled}.
     * 
     * @param query the abstract query command; may not be null
     * @param hints the hints
     * @param variables the map of variables and the corresonding values
     * @return the cancellable query
     * @throws RepositoryException if there is a problem accessing or using the repository
     */
    CancellableQuery createExecutableQuery( QueryCommand query,
                                            PlanHints hints,
                                            Map<String, Object> variables ) throws RepositoryException;

    /**
     * Obtain the JCR node given the supplied cached node.
     * 
     * @param node the cached node obtained from the query results
     * @return the JCR node instance that corresponds to the supplied cached node; may be null if the node does not exist or is
     *         not visible
     */
    Node getNode( CachedNode node );

    /**
     * Checks if there is a {@link org.modeshape.jcr.ModeShapePermissions#READ} permission for the given node in this context.
     * 
     * @param node a {@link org.modeshape.jcr.cache.CachedNode}, never {@code null}
     * @return {@code true} if the current context can read the given node, {@code false} otherwise
     */
    boolean canRead( CachedNode node );

    /**
     * Create a JCR {@link Value} instance given the supplied value and property type.
     * 
     * @param propertyType the JCR property type
     * @param value the value
     * @return the JCR value instance
     */
    Value createValue( int propertyType,
                       Object value );

    /**
     * Record the duration of a query.
     * 
     * @param duration the duration in nanoseconds; must be positive
     * @param unit the time unit for the duration; may not be null
     * @param query the query string; may not be null
     * @param language the query language; may not be null
     */
    void recordDuration( long duration,
                         TimeUnit unit,
                         String query,
                         String language );

    /**
     * Get the internal {@link Path} of the supplied cached node.
     * 
     * @param node the cached node; may not be null
     * @return the path; never null
     */
    Path getPath( CachedNode node );

    /**
     * Get the internal {@link Name} of the supplied cached node.
     * 
     * @param node the cached node; may not be null
     * @return the node name; never null
     */
    Name getName( CachedNode node );

    /**
     * Get the internal {@link Node#getIdentifier() public JCR identifier} of the supplied cached node.
     * 
     * @param node the cached node; may not be null
     * @return the JCR identifier; never null
     */
    String getIdentifier( CachedNode node );

    /**
     * Get the depth of the supplied cached node.
     * 
     * @param node the cached node; may not be null
     * @return the depth
     */
    long getDepth( CachedNode node );

    /**
     * Get the number of children of the supplied cached node.
     *
     * @param node the cached node; may not be null
     * @return the child count
     */
    long getChildCount( CachedNode node );

    /**
     * Get the UUID identifier of the supplied cached node. Note that the node must have the 'mix:referenceable' mixin for it to
     * have a UUID.
     * 
     * @param node the cached node; may not be null
     * @return the UUID, or null if the node is not referenceable
     */
    String getUuid( CachedNode node );
}
