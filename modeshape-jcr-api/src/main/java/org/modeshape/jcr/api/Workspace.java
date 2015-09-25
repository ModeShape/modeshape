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
package org.modeshape.jcr.api;

import java.util.concurrent.Future;
import javax.jcr.AccessDeniedException;
import javax.jcr.RepositoryException;
import org.modeshape.jcr.api.federation.FederationManager;
import org.modeshape.jcr.api.index.IndexManager;
import org.modeshape.jcr.api.nodetype.NodeTypeManager;
import org.modeshape.jcr.api.query.QueryManager;

/**
 * An extension of JCR 2.0's {@link javax.jcr.Workspace} interface, with a few ModeShape-specific enhancements.
 */
public interface Workspace extends javax.jcr.Workspace {

    /**
     * {@inheritDoc}
     * <p>
     * This method returns the ModeShape-specific specialization of the standard {@link javax.jcr.nodetype.NodeTypeManager}
     * interface.
     * </p>
     */
    @Override
    NodeTypeManager getNodeTypeManager() throws RepositoryException;

    /**
     * {@inheritDoc}
     * <p>
     * This method returns the ModeShape-specific specialization of the standard {@link javax.jcr.query.QueryManager} interface.
     * </p>
     */
    @Override
    public QueryManager getQueryManager() throws RepositoryException;

    /**
     * Returns the <code>IndexManager</code> object that can be used to programmatically register and unregister index
     * definitions.
     *
     * @return the <code>IndexManager</code> object.
     * @throws RepositoryException if an error occurs.
     */
    public IndexManager getIndexManager() throws RepositoryException;

    /**
     * Return a {@link RepositoryManager} that can be used to administer the Repository instance through which this workspace's
     * session was acquired.
     *
     * @return the {@link RepositoryManager} instance.
     * @throws AccessDeniedException if the caller does not have authorization to obtain the manager.
     * @throws RepositoryException if another error occurred.
     * @since 3.0
     */
    RepositoryManager getRepositoryManager() throws AccessDeniedException, RepositoryException;

    /**
     * Crawl and re-index the content in this workspace. This method blocks until the indexing is completed.
     *
     * @throws AccessDeniedException if the session does not have the privileges to reindex the workspace
     * @throws RepositoryException if there is a problem with this session or workspace
     * @see #reindexAsync()
     * @see #reindexAsync(String)
     * @see #reindex(String)
     */
    void reindex() throws RepositoryException;

    /**
     * Crawl and index the content starting at the supplied path in this workspace, to the designated depth.
     *
     * @param path the path of the content to be indexed
     * @throws IllegalArgumentException if the workspace or path are null, or if the depth is less than 1
     * @throws AccessDeniedException if the session does not have the privileges to reindex this part of the workspace
     * @throws RepositoryException if there is a problem with this session or workspace
     * @see #reindex()
     * @see #reindexAsync()
     * @see #reindexAsync(String)
     */
    void reindex( String path ) throws RepositoryException;

    /**
     * Reindex only the nodes from this workspace that have changed since the given timestamp. This only works if the repository
     * has journaling enabled. This method blocks until the indexing is completed.
     * 
     * @param timestamp a {@code long} timestamp starting with which all changed nodes will be reindexed.
     * @throws RepositoryException if anything fails.
     * @since 4.5
     */
    void reindexSince( long timestamp ) throws RepositoryException;

    /**
     * Asynchronously crawl and re-index the content in this workspace.
     *
     * @return a future representing the asynchronous operation; never null
     * @throws AccessDeniedException if the session does not have the privileges to reindex the workspace
     * @throws RepositoryException if there is a problem with this session or workspace
     * @see #reindex()
     * @see #reindex(String)
     * @see #reindexAsync(String)
     */
    Future<Boolean> reindexAsync() throws RepositoryException;

    /**
     * Asynchronously crawl and index the content starting at the supplied path in this workspace, to the designated depth.
     *
     * @param path the path of the content to be indexed
     * @return a future representing the asynchronous operation; never null
     * @throws IllegalArgumentException if the workspace or path are null, or if the depth is less than 1
     * @throws AccessDeniedException if the session does not have the privileges to reindex this part of the workspace
     * @throws RepositoryException if there is a problem with this session or workspace
     * @see #reindex()
     * @see #reindex(String)
     * @see #reindexAsync()
     */
    Future<Boolean> reindexAsync( String path ) throws RepositoryException;

    /**
     * Asynchronously reindex only the nodes from this workspace that have changed since the given timestamp. 
     * This only works if the repository has journaling enabled.
     *
     * @param timestamp a {@code long} timestamp starting with which all changed nodes will be reindexed.
     * @return a future representing the asynchronous operation; never null
     * @throws RepositoryException if anything fails.
     * @see #reindexSince(long) 
     * @since 4.5
     */
    Future<Boolean> reindexSinceAsync( long timestamp ) throws RepositoryException;

    /**
     * Returns the federation manager instance which can be used to connect to external sources.
     * @return a {@link FederationManager} instance; never {@code null}
     * @throws RepositoryException if anything unexpected fails.
     * @see FederationManager
     */
    FederationManager getFederationManager() throws RepositoryException;
}
