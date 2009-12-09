/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.search;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.observe.Observer;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.request.ChangeRequest;
import org.jboss.dna.graph.request.InvalidWorkspaceException;

/**
 * A component that acts as a search engine for the content within a single {@link RepositorySource}. This engine manages a set of
 * indexes and provides search functionality for each of the workspaces within the source, and provides various methods to
 * (re)index the content contained with source's workspaces and keep the indexes up-to-date via changes.
 * 
 * @param <WorkspaceType> the workspace type
 * @param <ProcessorType> the processor type
 */
@ThreadSafe
public abstract class SearchEngine<WorkspaceType extends SearchEngineWorkspace, ProcessorType extends SearchEngineProcessor<WorkspaceType>> {

    public static final boolean DEFAULT_VERIFY_WORKSPACE_IN_SOURCE = true;

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
    protected SearchEngine( String sourceName,
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
    protected SearchEngine( String sourceName,
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
     * Get the name of the source that can be searched with an engine that uses this provider.
     * 
     * @return the name of the source that is to be searchable; never null
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
     * Create the {@link SearchEngineProcessor} implementation that can be used to operate against the
     * {@link SearchEngineWorkspace} instances.
     * <p>
     * Note that the resulting processor must be {@link SearchEngineProcessor#close() closed} by the caller when completed.
     * </p>
     * 
     * @param context the context in which the processor is to be used; never null
     * @param observer the observer of any events created by the processor; may be null
     * @param readOnly true if the processor will only be reading or searching, or false if the processor will be used to update
     *        the workspaces
     * @return the processor; may not be null
     */
    public ProcessorType createProcessor( ExecutionContext context,
                                          Observer observer,
                                          boolean readOnly ) {
        return createProcessor(context, workspaces, observer, readOnly);
    }

    /**
     * Utility method to index all of the content at or below the supplied path in the named workspace within the
     * {@link #getSourceName() source}. If the starting point is the root node, then this method will drop the existing index(es)
     * and rebuild from the content in the workspace and source.
     * <p>
     * This method operates synchronously and returns when the requested indexing is completed.
     * </p>
     * 
     * @param context the context in which the operation is to be performed; may not be null
     * @param workspaceName the name of the workspace
     * @param startingPoint the location that represents the content to be indexed; must have a path
     * @param depthPerRead the depth of each subgraph read operation
     * @throws IllegalArgumentException if the workspace name or location are null
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws SearchEngineException if there is a problem updating the indexes
     * @throws InvalidWorkspaceException if the workspace does not exist
     */
    public void index( ExecutionContext context,
                       String workspaceName,
                       Location startingPoint,
                       int depthPerRead ) throws RepositorySourceException, SearchEngineException {
        CheckArg.isNotNull(workspaceName, "workspaceName");
        CheckArg.isNotNull(startingPoint, "startingPoint");
        assert startingPoint.hasPath();
        workspaces.getWorkspace(context, workspaceName, true);
        ProcessorType processor = createProcessor(context, workspaces, null, false);
        try {
            processor.crawl(workspaceName, startingPoint, depthPerRead);
        } finally {
            processor.close();
        }
    }

    /**
     * Index all of the content at or below the supplied path in the named workspace within the {@link #getSourceName() source}.
     * If the starting point is the root node, then this method will drop the existing index(es) and rebuild from the content in
     * the workspace and source.
     * <p>
     * This method operates synchronously and returns when the requested indexing is completed.
     * </p>
     * 
     * @param context the context in which the operation is to be performed; may not be null
     * @param workspaceName the name of the workspace
     * @param startingPoint the path that represents the content to be indexed
     * @param depthPerRead the depth of each subgraph read operation
     * @throws IllegalArgumentException if the workspace name or path are null
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws SearchEngineException if there is a problem updating the indexes
     * @throws InvalidWorkspaceException if the workspace does not exist
     */
    public void index( ExecutionContext context,
                       String workspaceName,
                       Path startingPoint,
                       int depthPerRead ) throws RepositorySourceException, SearchEngineException {
        CheckArg.isNotNull(workspaceName, "workspaceName");
        CheckArg.isNotNull(startingPoint, "startingPoint");
        index(context, workspaceName, Location.create(startingPoint), depthPerRead);
    }

    /**
     * Index all of the content in the named workspace within the {@link #getSourceName() source}. This method operates
     * synchronously and returns when the requested indexing is completed.
     * 
     * @param context the context in which the operation is to be performed; may not be null
     * @param workspaceName the name of the workspace
     * @param depthPerRead the depth of each subgraph read operation
     * @throws IllegalArgumentException if the workspace name is null
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws SearchEngineException if there is a problem updating the indexes
     * @throws InvalidWorkspaceException if the workspace does not exist
     */
    public void index( ExecutionContext context,
                       String workspaceName,
                       int depthPerRead ) throws RepositorySourceException, SearchEngineException {
        CheckArg.isNotNull(workspaceName, "workspaceName");
        Path rootPath = context.getValueFactories().getPathFactory().createRootPath();
        index(context, workspaceName, Location.create(rootPath), depthPerRead);
    }

    /**
     * Index (or re-index) all of the content in all of the workspaces within the source. This method operates synchronously and
     * returns when the requested indexing is completed.
     * 
     * @param context the context in which the operation is to be performed; may not be null
     * @param depthPerRead the depth of each subgraph read operation
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws SearchEngineException if there is a problem updating the indexes
     */
    public void index( ExecutionContext context,
                       int depthPerRead ) throws RepositorySourceException, SearchEngineException {
        Path rootPath = context.getValueFactories().getPathFactory().createRootPath();
        Location rootLocation = Location.create(rootPath);
        for (String workspaceName : graph(context).getWorkspaces()) {
            index(context, workspaceName, rootLocation, depthPerRead);
        }
    }

    /**
     * Update the indexes with the supplied set of changes to the content.
     * 
     * @param context the execution context for which this session is to be established; may not be null
     * @param changes the set of changes to the content
     * @return the actual changes that were made and which record any problems or errors; never null
     * @throws IllegalArgumentException if the path is null
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws SearchEngineException if there is a problem updating the indexes
     */
    public List<ChangeRequest> index( ExecutionContext context,
                                      final Iterable<ChangeRequest> changes ) throws SearchEngineException {
        List<ChangeRequest> requests = new LinkedList<ChangeRequest>();
        ProcessorType processor = createProcessor(context, workspaces, null, false);
        try {
            boolean submit = true;
            for (ChangeRequest request : changes) {
                ChangeRequest clone = request.clone();
                if (submit) {
                    processor.process(clone);
                    if (clone.hasError()) submit = false;
                }
                requests.add(clone);
            }
        } finally {
            processor.close();
        }
        return requests;
    }

    /**
     * Invoke the engine's garbage collection on all indexes used by all workspaces in the source. This method reclaims space and
     * optimizes the index. This should be done on a periodic basis after changes are made to the engine's indexes.
     * 
     * @param context the context in which the operation is to be performed; may not be null
     * @return true if an optimization was performed, or false if there was no need
     * @throws SearchEngineException if there is a problem during optimization
     */
    public boolean optimize( ExecutionContext context ) throws SearchEngineException {
        ProcessorType processor = createProcessor(context, workspaces, null, true);
        try {
            return processor.optimize();
        } finally {
            processor.close();
        }
    }

    /**
     * Invoke the engine's garbage collection for the indexes associated with the specified workspace. This method reclaims space
     * and optimizes the index. This should be done on a periodic basis after changes are made to the engine's indexes.
     * 
     * @param workspaceName the name of the workspace
     * @param context the context in which the operation is to be performed; may not be null
     * @return true if an optimization was performed, or false if there was no need
     * @throws IllegalArgumentException if the workspace name is null
     * @throws SearchEngineException if there is a problem during optimization
     * @throws InvalidWorkspaceException if the workspace does not exist
     */
    public boolean optimize( ExecutionContext context,
                             String workspaceName ) throws SearchEngineException {
        CheckArg.isNotNull(workspaceName, "workspaceName");
        ProcessorType processor = createProcessor(context, workspaces, null, true);
        try {
            return processor.optimize(workspaceName);
        } finally {
            processor.close();
        }
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
         * @see org.jboss.dna.graph.search.SearchEngine.Workspaces#getRepositoryConnectionFactory()
         */
        public RepositoryConnectionFactory getRepositoryConnectionFactory() {
            return connectionFactory;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.search.SearchEngine.Workspaces#getWorkspace(org.jboss.dna.graph.ExecutionContext,
         *      java.lang.String, boolean)
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
         * @see org.jboss.dna.graph.search.SearchEngine.Workspaces#getWorkspaces()
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
         * @see org.jboss.dna.graph.search.SearchEngine.Workspaces#removeWorkspace(java.lang.String)
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
         * @see org.jboss.dna.graph.search.SearchEngine.Workspaces#removeAllWorkspaces()
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
