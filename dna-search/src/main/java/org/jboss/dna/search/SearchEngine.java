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
package org.jboss.dna.search;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.apache.lucene.store.Directory;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.query.QueryResults;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.request.ChangeRequest;
import org.jboss.dna.graph.request.InvalidWorkspaceException;

/**
 * A component that acts as a search engine for the content within a single {@link RepositorySource}. This engine manages a set of
 * indexes and provides search functionality for each of the workspaces within the source, and provides various methods to
 * (re)index the content contained with source's workspaces and keep the indexes up-to-date via changes.
 */
@ThreadSafe
public class SearchEngine {

    private final ExecutionContext context;
    private final String sourceName;
    private final RepositoryConnectionFactory connectionFactory;
    private final DirectoryConfiguration directoryFactory;
    private final IndexingStrategy indexingStrategy;
    private final PathFactory pathFactory;
    @GuardedBy( "workspaceEngineLock" )
    private final Map<String, WorkspaceSearchEngine> workspaceEnginesByName;
    private final ReadWriteLock workspaceEngineLock = new ReentrantReadWriteLock();

    /**
     * Create a search engine instance given the supplied {@link ExecutionContext execution context}, name of the
     * {@link RepositorySource}, the {@link RepositoryConnectionFactory factory for RepositorySource connections}, and the
     * {@link DirectoryConfiguration directory factory} that defines where each workspace's indexes should be placed.
     * 
     * @param context the execution context in which all indexing operations should be performed
     * @param sourceName the name of the {@link RepositorySource}
     * @param connectionFactory the connection factory
     * @param directoryFactory the factory for Lucene {@link Directory directories}
     * @param indexingStrategy the indexing strategy that governs how properties are to be indexed; or null if the default
     *        strategy should be used
     * @throws IllegalArgumentException if any of the parameters (other than indexing strategy) are null
     */
    public SearchEngine( ExecutionContext context,
                         String sourceName,
                         RepositoryConnectionFactory connectionFactory,
                         DirectoryConfiguration directoryFactory,
                         IndexingStrategy indexingStrategy ) {
        CheckArg.isNotNull(context, "context");
        CheckArg.isNotNull(sourceName, "sourceName");
        CheckArg.isNotNull(connectionFactory, "connectionFactory");
        CheckArg.isNotNull(directoryFactory, "directoryFactory");
        this.sourceName = sourceName;
        this.connectionFactory = connectionFactory;
        this.directoryFactory = directoryFactory;
        this.context = context;
        this.pathFactory = context.getValueFactories().getPathFactory();
        this.workspaceEnginesByName = new HashMap<String, WorkspaceSearchEngine>();
        this.indexingStrategy = indexingStrategy != null ? indexingStrategy : new StoreLittleIndexingStrategy();
    }

    /**
     * Get the name of the RepositorySource that this engine is to use.
     * 
     * @return the source name; never null
     */
    public String getSourceName() {
        return sourceName;
    }

    /**
     * Get the context in which all indexing operations execute.
     * 
     * @return the execution context; never null
     */
    public ExecutionContext getContext() {
        return context;
    }

    /**
     * Utility to create a Graph for the source.
     * 
     * @return the graph instance; never null
     */
    final Graph graph() {
        return Graph.create(sourceName, connectionFactory, context);
    }

    /**
     * Get the search engine for the workspace with the supplied name.
     * 
     * @param workspaceName the name of the workspace
     * @return the workspace's search engine
     * @throws InvalidWorkspaceException if the workspace does not exist
     */
    protected WorkspaceSearchEngine getWorkspaceEngine( String workspaceName ) {
        WorkspaceSearchEngine engine = null;
        try {
            workspaceEngineLock.readLock().lock();
            engine = workspaceEnginesByName.get(workspaceName);
        } finally {
            workspaceEngineLock.readLock().unlock();
        }

        if (engine == null) {
            // Verify the workspace does exist ...
            if (!graph().getWorkspaces().contains(workspaceName)) {
                String msg = GraphI18n.workspaceDoesNotExistInRepository.text(workspaceName, getSourceName());
                throw new InvalidWorkspaceException(msg);
            }
            try {
                workspaceEngineLock.writeLock().lock();
                // Check whether another thread got in and created the engine while we waited ...
                engine = workspaceEnginesByName.get(workspaceName);
                if (engine == null) {
                    // Create the engine and register it ...
                    engine = new WorkspaceSearchEngine(context, directoryFactory, indexingStrategy, sourceName, workspaceName,
                                                       connectionFactory);
                    workspaceEnginesByName.put(workspaceName, engine);
                }
            } finally {
                workspaceEngineLock.writeLock().unlock();
            }
        }
        return engine;
    }

    /**
     * Index all of the content at or below the supplied path in the named workspace within the {@link #getSourceName() source}.
     * 
     * @param workspaceName the name of the workspace
     * @param startingPoint the path that represents the content to be indexed
     * @param depthPerRead the depth of each subgraph read operation
     * @throws IllegalArgumentException if the workspace name or path are null
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws SearchEngineException if there is a problem updating the indexes
     * @throws InvalidWorkspaceException if the workspace does not exist
     */
    public void indexContent( String workspaceName,
                              Path startingPoint,
                              int depthPerRead ) throws RepositorySourceException, SearchEngineException {
        CheckArg.isNotNull(workspaceName, "workspaceName");
        CheckArg.isNotNull(startingPoint, "startingPoint");
        getWorkspaceEngine(workspaceName).indexContent(startingPoint, depthPerRead);
    }

    /**
     * Index all of the content in the named workspace within the {@link #getSourceName() source}.
     * 
     * @param workspaceName the name of the workspace
     * @param depthPerRead the depth of each subgraph read operation
     * @throws IllegalArgumentException if the workspace name is null
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws SearchEngineException if there is a problem updating the indexes
     * @throws InvalidWorkspaceException if the workspace does not exist
     */
    public void indexContent( String workspaceName,
                              int depthPerRead ) throws RepositorySourceException, SearchEngineException {
        CheckArg.isNotNull(workspaceName, "workspaceName");
        indexContent(workspaceName, pathFactory.createRootPath(), depthPerRead);
    }

    /**
     * Index (or re-index) all of the content in all of the workspaces within the source.
     * 
     * @param depthPerRead the depth of each subgraph read operation
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws SearchEngineException if there is a problem updating the indexes
     */
    public void indexContent( int depthPerRead ) throws RepositorySourceException, SearchEngineException {
        Path rootPath = pathFactory.createRootPath();
        for (String workspaceName : graph().getWorkspaces()) {
            getWorkspaceEngine(workspaceName).indexContent(rootPath, depthPerRead);
        }
    }

    /**
     * Update the indexes with the supplied set of changes to the content.
     * 
     * @param changes the set of changes to the content
     * @throws IllegalArgumentException if the path is null
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws SearchEngineException if there is a problem updating the indexes
     */
    public void indexChanges( final Iterable<ChangeRequest> changes ) throws SearchEngineException {
        // First break up all the changes into different collections, one collection per workspace ...
        Map<String, Collection<ChangeRequest>> changesByWorkspace = new HashMap<String, Collection<ChangeRequest>>();
        for (ChangeRequest request : changes) {
            String workspaceName = request.changedWorkspace();
            Collection<ChangeRequest> changesForWorkspace = changesByWorkspace.get(workspaceName);
            if (changesForWorkspace == null) {
                changesForWorkspace = new LinkedList<ChangeRequest>();
                changesByWorkspace.put(workspaceName, changesForWorkspace);
            }
            changesForWorkspace.add(request);
        }
        // Now update the indexes for each workspace (serially). This minimizes the time that each workspace
        // locks its indexes for writing.
        for (Map.Entry<String, Collection<ChangeRequest>> entry : changesByWorkspace.entrySet()) {
            String workspaceName = entry.getKey();
            Collection<ChangeRequest> changesForWorkspace = entry.getValue();
            getWorkspaceEngine(workspaceName).indexChanges(changesForWorkspace);
        }
    }

    /**
     * Invoke the engine's garbage collection on all indexes used by all workspaces in the source. This method reclaims space and
     * optimizes the index. This should be done on a periodic basis after changes are made to the engine's indexes.
     * 
     * @throws SearchEngineException if there is a problem during optimization
     */
    public void optimize() throws SearchEngineException {
        for (String workspaceName : graph().getWorkspaces()) {
            getWorkspaceEngine(workspaceName).optimize();
        }
    }

    /**
     * Invoke the engine's garbage collection for the indexes associated with the specified workspace. This method reclaims space
     * and optimizes the index. This should be done on a periodic basis after changes are made to the engine's indexes.
     * 
     * @param workspaceName the name of the workspace
     * @throws IllegalArgumentException if the workspace name is null
     * @throws SearchEngineException if there is a problem during optimization
     * @throws InvalidWorkspaceException if the workspace does not exist
     */
    public void optimize( String workspaceName ) throws SearchEngineException {
        CheckArg.isNotNull(workspaceName, "workspaceName");
        getWorkspaceEngine(workspaceName).optimize();
    }

    /**
     * Perform a full-text search of the content in the named workspace, given the maximum number of results and the offset
     * defining the first result the caller is interested in.
     * 
     * @param workspaceName the name of the workspace
     * @param fullTextSearch the full-text search to be performed; may not be null
     * @param maxResults the maximum number of results that are to be returned; always positive
     * @param offset the number of initial results to skip, or 0 if the first results are to be returned
     * @return the activity that will perform the work
     * @throws IllegalArgumentException if the workspace name is null
     * @throws SearchEngineException if there is a problem during optimization
     * @throws InvalidWorkspaceException if the workspace does not exist
     */
    public List<Location> fullTextSearch( String workspaceName,
                                          String fullTextSearch,
                                          int maxResults,
                                          int offset ) {
        CheckArg.isNotNull(workspaceName, "workspaceName");
        return getWorkspaceEngine(workspaceName).fullTextSearch(fullTextSearch, maxResults, offset);
    }

    /**
     * Perform a query of the content in the named workspace, given the Abstract Query Model representation of the query.
     * 
     * @param workspaceName the name of the workspace
     * @param query the query that is to be executed, in the form of the Abstract Query Model
     * @return the query results; never null
     * @throws IllegalArgumentException if the context or query references are null
     */
    public QueryResults execute( String workspaceName,
                                 QueryCommand query ) {
        CheckArg.isNotNull(workspaceName, "workspaceName");
        return getWorkspaceEngine(workspaceName).execute(query);
    }

}
