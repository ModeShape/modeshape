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
package org.modeshape.graph.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.request.InvalidWorkspaceException;

/**
 * A component that acts as a search engine for the content within a single {@link RepositorySource}. This engine manages a set of
 * indexes and provides search functionality for each of the workspaces within the source, and provides various methods to
 * (re)index the content contained with source's workspaces and keep the indexes up-to-date via changes.
 * 
 * @param <WorkspaceType> the workspace type
 * @param <ProcessorType> the processor type
 */
@ThreadSafe
public abstract class AbstractSearchEngine<WorkspaceType extends SearchEngineWorkspace, ProcessorType extends SearchEngineProcessor>
    implements SearchEngine {

    public static final boolean DEFAULT_VERIFY_WORKSPACE_IN_SOURCE = false;

    private final boolean verifyWorkspaceInSource;
    private final RepositoryConnectionFactory connectionFactory;
    private final String sourceName;
    private volatile Workspaces<WorkspaceType> workspaces;

    /**
     * Create a new provider instance that can be used to manage the indexes for the workspaces in a single source.
     * 
     * @param sourceName the name of the source that can be searched; never null
     * @param connectionFactory the connection factory; may be null if the engine can operate without connecting to the source
     */
    protected AbstractSearchEngine( String sourceName,
                                    RepositoryConnectionFactory connectionFactory ) {
        this(sourceName, connectionFactory, DEFAULT_VERIFY_WORKSPACE_IN_SOURCE);
    }

    /**
     * Create a new provider instance that can be used to manage the indexes for the workspaces in a single source.
     * 
     * @param sourceName the name of the source that can be searched; never null
     * @param connectionFactory the connection factory; may be null if the engine can operate without connecting to the source
     * @param verifyWorkspaceInSource true if the workspaces are to be verified by checking the original source
     * @throws IllegalArgumentException if any of the parameters are null
     */
    protected AbstractSearchEngine( String sourceName,
                                    RepositoryConnectionFactory connectionFactory,
                                    boolean verifyWorkspaceInSource ) {
        CheckArg.isNotNull(sourceName, "sourceName");
        CheckArg.isNotNull(connectionFactory, "connectionFactory");
        this.sourceName = sourceName;
        this.connectionFactory = connectionFactory;
        this.verifyWorkspaceInSource = verifyWorkspaceInSource;
        this.workspaces = new SearchWorkspaces(connectionFactory);
    }

    /**
     * @return connectionFactory
     */
    protected RepositoryConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.search.SearchEngine#getSourceName()
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Determine whether the workspaces should be verified with the original source before creating indexes for them.
     * 
     * @return true if verification should be performed, or false otherwise
     */
    public boolean isVerifyWorkspaceInSource() {
        return verifyWorkspaceInSource;
    }

    /**
     * Obtain a graph to the source for which this engine exists.
     * 
     * @param context the context in which the graph operations should be performed; never null
     * @return the graph; never null
     * @throws RepositorySourceException if a connection to the source cannot be established
     */
    protected Graph graph( ExecutionContext context ) {
        assert context != null;
        return Graph.create(sourceName, connectionFactory, context);
    }

    /**
     * Create the index(es) required for the named workspace.
     * 
     * @param context the context in which the operation is to be performed; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @return the workspace; never null
     * @throws SearchEngineException if there is a problem creating the workspace.
     */
    protected abstract WorkspaceType createWorkspace( ExecutionContext context,
                                                      String workspaceName ) throws SearchEngineException;

    /**
     * Create the {@link SearchEngineProcessor} implementation that can be used to operate against the
     * {@link SearchEngineWorkspace} instances.
     * <p>
     * Note that the resulting processor must be {@link SearchEngineProcessor#close() closed} by the caller when completed.
     * </p>
     * 
     * @param context the context in which the processor is to be used; never null
     * @param workspaces the set of existing search workspaces; never null
     * @param observer the observer of any events created by the processor; may be null
     * @param readOnly true if the processor will only be reading or searching, or false if the processor will be used to update
     *        the workspaces
     * @return the processor; may not be null
     */
    protected abstract ProcessorType createProcessor( ExecutionContext context,
                                                      Workspaces<WorkspaceType> workspaces,
                                                      Observer observer,
                                                      boolean readOnly );

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.search.SearchEngine#createProcessor(org.modeshape.graph.ExecutionContext,
     *      org.modeshape.graph.observe.Observer, boolean)
     */
    public SearchEngineProcessor createProcessor( ExecutionContext context,
                                                  Observer observer,
                                                  boolean readOnly ) {
        return createProcessor(context, workspaces, observer, readOnly);
    }

    public interface Workspaces<WorkspaceType extends SearchEngineWorkspace> {
        /**
         * Get the connection factory for repository sources.
         * 
         * @return the connection factory; never null
         */
        RepositoryConnectionFactory getRepositoryConnectionFactory();

        /**
         * Get the search engine for the workspace with the supplied name.
         * 
         * @param context the execution context; never null
         * @param workspaceName the name of the workspace; never null
         * @param createIfMissing true if the workspace should be created if missing, or false otherwise
         * @return the workspace's search engine
         * @throws InvalidWorkspaceException if the workspace does not exist
         */
        WorkspaceType getWorkspace( ExecutionContext context,
                                    String workspaceName,
                                    boolean createIfMissing );

        /**
         * Get the existing workspaces.
         * 
         * @return the workspaces
         */
        Collection<WorkspaceType> getWorkspaces();

        /**
         * Remove the supplied workspace from the search engine. This is typically done when the workspace is being deleted. Note
         * that the resulting Workspace needs to then be cleaned up by the caller.
         * 
         * @param workspaceName the name of the workspace
         * @return the workspace that was removed, or null if there was workspace with the supplied name
         */
        WorkspaceType removeWorkspace( String workspaceName );

        /**
         * Remove from the search engine all workspace-related indexes, thereby cleaning up any resources used by this search
         * engine.
         * 
         * @return the mutable map containing the {@link SearchEngineWorkspace} objects keyed by their name; never null but
         *         possibly empty
         */
        Map<String, WorkspaceType> removeAllWorkspaces();
    }

    protected class SearchWorkspaces implements Workspaces<WorkspaceType> {
        private final ReadWriteLock workspacesLock = new ReentrantReadWriteLock();
        @GuardedBy( "workspacesLock" )
        private final Map<String, WorkspaceType> workspacesByName = new HashMap<String, WorkspaceType>();
        private final RepositoryConnectionFactory connectionFactory;

        protected SearchWorkspaces( RepositoryConnectionFactory connectionFactory ) {
            this.connectionFactory = connectionFactory;
        }

        /**
         * {@inheritDoc}
         * 
         * @see AbstractSearchEngine.Workspaces#getRepositoryConnectionFactory()
         */
        public RepositoryConnectionFactory getRepositoryConnectionFactory() {
            return connectionFactory;
        }

        /**
         * {@inheritDoc}
         * 
         * @see AbstractSearchEngine.Workspaces#getWorkspace(org.modeshape.graph.ExecutionContext, java.lang.String, boolean)
         */
        public WorkspaceType getWorkspace( ExecutionContext context,
                                           String workspaceName,
                                           boolean createIfMissing ) {
            assert context != null;
            assert workspaceName != null;
            WorkspaceType workspace = null;
            try {
                workspacesLock.readLock().lock();
                workspace = workspacesByName.get(workspaceName);
            } finally {
                workspacesLock.readLock().unlock();
            }

            if (workspace == null) {
                // Verify the workspace does exist ...
                if (isVerifyWorkspaceInSource() && connectionFactory != null
                    && !graph(context).getWorkspaces().contains(workspaceName)) {
                    String msg = GraphI18n.workspaceDoesNotExistInRepository.text(workspaceName, getSourceName());
                    throw new InvalidWorkspaceException(msg);
                }
                try {
                    workspacesLock.writeLock().lock();
                    // Check whether another thread got in and created the engine while we waited ...
                    workspace = workspacesByName.get(workspaceName);
                    if (workspace == null) {
                        // Create the engine and register it ...
                        workspace = createWorkspace(context, workspaceName);
                        workspacesByName.put(workspaceName, workspace);
                    }
                } finally {
                    workspacesLock.writeLock().unlock();
                }
            }
            return workspace;
        }

        /**
         * {@inheritDoc}
         * 
         * @see AbstractSearchEngine.Workspaces#getWorkspaces()
         */
        public Collection<WorkspaceType> getWorkspaces() {
            try {
                workspacesLock.writeLock().lock();
                return new ArrayList<WorkspaceType>(workspacesByName.values());
            } finally {
                workspacesByName.clear();
                workspacesLock.writeLock().unlock();
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see AbstractSearchEngine.Workspaces#removeWorkspace(java.lang.String)
         */
        public WorkspaceType removeWorkspace( String workspaceName ) {
            CheckArg.isNotNull(workspaceName, "workspaceName");
            try {
                workspacesLock.writeLock().lock();
                // Check whether another thread got in and created the engine while we waited ...
                return workspacesByName.remove(workspaceName);
            } finally {
                workspacesLock.writeLock().unlock();
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see AbstractSearchEngine.Workspaces#removeAllWorkspaces()
         */
        public Map<String, WorkspaceType> removeAllWorkspaces() {
            try {
                workspacesLock.writeLock().lock();
                return new HashMap<String, WorkspaceType>(workspacesByName);
            } finally {
                workspacesByName.clear();
                workspacesLock.writeLock().unlock();
            }
        }
    }
}
