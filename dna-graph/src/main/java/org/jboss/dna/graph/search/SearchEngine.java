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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.GuardedBy;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Subgraph;
import org.jboss.dna.graph.SubgraphNode;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.QueryResults;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.validate.Schemata;
import org.jboss.dna.graph.request.ChangeRequest;
import org.jboss.dna.graph.request.InvalidWorkspaceException;
import org.jboss.dna.graph.search.SearchProvider.Session;

/**
 * A component that acts as a search engine for the content within a single {@link RepositorySource}. This engine manages a set of
 * indexes and provides search functionality for each of the workspaces within the source, and provides various methods to
 * (re)index the content contained with source's workspaces and keep the indexes up-to-date via changes.
 */
@ThreadSafe
public class SearchEngine {

    /**
     * The default maximum number of changes that can be made to an index before the indexes are automatically optimized is * * *
     * * {@value}
     */
    public static final int DEFAULT_MAX_CHANGES_BEFORE_AUTOMATIC_OPTIMIZATION = 0;

    protected final ExecutionContext context;
    private final String sourceName;
    private final RepositoryConnectionFactory connectionFactory;
    protected final SearchProvider indexLayout;
    private final int maxChangesBeforeAutomaticOptimization;
    @GuardedBy( "workspacesLock" )
    private final Map<String, Workspace> workspacesByName = new HashMap<String, Workspace>();
    private final ReadWriteLock workspacesLock = new ReentrantReadWriteLock();

    /**
     * Create a search engine instance given the supplied {@link ExecutionContext execution context}, name of the
     * {@link RepositorySource}, the {@link RepositoryConnectionFactory factory for RepositorySource connections}, and the
     * {@link SearchProvider search provider}.
     * 
     * @param context the execution context for indexing and optimization operations
     * @param sourceName the name of the {@link RepositorySource}
     * @param connectionFactory the connection factory
     * @param indexLayout the specification of the Lucene index layout
     * @param maxChangesBeforeAutomaticOptimization the number of changes that can be made to the index before the indexes are
     *        automatically optimized; may be 0 or a negative number if no automatic optimization should be done
     * @throws IllegalArgumentException if any of the parameters (other than indexing strategy) are null
     */
    public SearchEngine( ExecutionContext context,
                         String sourceName,
                         RepositoryConnectionFactory connectionFactory,
                         SearchProvider indexLayout,
                         int maxChangesBeforeAutomaticOptimization ) {
        CheckArg.isNotNull(context, "context");
        CheckArg.isNotNull(sourceName, "sourceName");
        CheckArg.isNotNull(connectionFactory, "connectionFactory");
        this.sourceName = sourceName;
        this.connectionFactory = connectionFactory;
        this.indexLayout = indexLayout;
        this.context = context;
        this.maxChangesBeforeAutomaticOptimization = maxChangesBeforeAutomaticOptimization < 0 ? 0 : maxChangesBeforeAutomaticOptimization;
    }

    /**
     * Create a search engine instance given the supplied {@link ExecutionContext execution context}, name of the
     * {@link RepositorySource}, the {@link RepositoryConnectionFactory factory for RepositorySource connections}, and the
     * {@link SearchProvider search provider} that defines where each workspace's indexes should be placed.
     * 
     * @param context the execution context for indexing and optimization operations
     * @param sourceName the name of the {@link RepositorySource}
     * @param connectionFactory the connection factory
     * @param indexLayout the specification of the Lucene index layout
     * @throws IllegalArgumentException if any of the parameters (other than indexing strategy) are null
     */
    public SearchEngine( ExecutionContext context,
                         String sourceName,
                         RepositoryConnectionFactory connectionFactory,
                         SearchProvider indexLayout ) {
        this(context, sourceName, connectionFactory, indexLayout, DEFAULT_MAX_CHANGES_BEFORE_AUTOMATIC_OPTIMIZATION);
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
     * @return maxChangesBeforeAutomaticOptimization
     */
    public int getMaxChangesBeforeAutomaticOptimization() {
        return maxChangesBeforeAutomaticOptimization;
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
     * Utility to obtain the root path.
     * 
     * @return the root path; never null
     */
    final Path rootPath() {
        return context.getValueFactories().getPathFactory().createRootPath();
    }

    /**
     * Utility to obtain a readable string representation of the supplied path.
     * 
     * @param path the path
     * @return the readable string representation; may be null if path is null
     */
    final String readable( Path path ) {
        return context.getValueFactories().getStringFactory().create(path);
    }

    /**
     * Index all of the content at or below the supplied path in the named workspace within the {@link #getSourceName() source}.
     * If the starting point is the root node, then this method will drop the existing index(es) and rebuild from the content in
     * the workspace and source.
     * <p>
     * This method operates synchronously and returns when the requested indexing is completed.
     * </p>
     * 
     * @param workspaceName the name of the workspace
     * @param startingPoint the location that represents the content to be indexed; must have a path
     * @param depthPerRead the depth of each subgraph read operation
     * @throws IllegalArgumentException if the workspace name or location are null
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws SearchEngineException if there is a problem updating the indexes
     * @throws InvalidWorkspaceException if the workspace does not exist
     */
    public void index( String workspaceName,
                       Location startingPoint,
                       int depthPerRead ) throws RepositorySourceException, SearchEngineException {
        CheckArg.isNotNull(workspaceName, "workspaceName");
        CheckArg.isNotNull(startingPoint, "startingPoint");
        assert startingPoint.hasPath();

        Workspace workspace = getWorkspace(workspaceName);
        if (startingPoint.getPath().isRoot()) {
            // More efficient to just start over with a new index ...
            workspace.execute(true, addContent(startingPoint, depthPerRead));
        } else {
            // Have to first remove the content below the starting point, then add it again ...
            workspace.execute(false, removeContent(startingPoint), addContent(startingPoint, depthPerRead));
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
     * @param workspaceName the name of the workspace
     * @param startingPoint the path that represents the content to be indexed
     * @param depthPerRead the depth of each subgraph read operation
     * @throws IllegalArgumentException if the workspace name or path are null
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws SearchEngineException if there is a problem updating the indexes
     * @throws InvalidWorkspaceException if the workspace does not exist
     */
    public void index( String workspaceName,
                       Path startingPoint,
                       int depthPerRead ) throws RepositorySourceException, SearchEngineException {
        CheckArg.isNotNull(workspaceName, "workspaceName");
        CheckArg.isNotNull(startingPoint, "startingPoint");
        index(workspaceName, Location.create(startingPoint), depthPerRead);
    }

    /**
     * Index all of the content in the named workspace within the {@link #getSourceName() source}. This method operates
     * synchronously and returns when the requested indexing is completed.
     * 
     * @param workspaceName the name of the workspace
     * @param depthPerRead the depth of each subgraph read operation
     * @throws IllegalArgumentException if the workspace name is null
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws SearchEngineException if there is a problem updating the indexes
     * @throws InvalidWorkspaceException if the workspace does not exist
     */
    public void index( String workspaceName,
                       int depthPerRead ) throws RepositorySourceException, SearchEngineException {
        CheckArg.isNotNull(workspaceName, "workspaceName");
        index(workspaceName, rootPath(), depthPerRead);
    }

    /**
     * Index (or re-index) all of the content in all of the workspaces within the source. This method operates synchronously and
     * returns when the requested indexing is completed.
     * 
     * @param depthPerRead the depth of each subgraph read operation
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws SearchEngineException if there is a problem updating the indexes
     */
    public void index( int depthPerRead ) throws RepositorySourceException, SearchEngineException {
        Path rootPath = rootPath();
        for (String workspaceName : graph().getWorkspaces()) {
            index(workspaceName, rootPath, depthPerRead);
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
    public void index( final Iterable<ChangeRequest> changes ) throws SearchEngineException {
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
            getWorkspace(workspaceName).execute(false, updateContent(changesForWorkspace));
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
            getWorkspace(workspaceName).execute(false, optimizeContent());
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
        getWorkspace(workspaceName).execute(false, optimizeContent());
    }

    /**
     * Perform a full-text search of the content in the named workspace, given the maximum number of results and the offset
     * defining the first result the caller is interested in.
     * 
     * @param context the execution context in which the search is to take place; may not be null
     * @param workspaceName the name of the workspace
     * @param fullTextSearch the full-text search to be performed; may not be null
     * @param maxResults the maximum number of results that are to be returned; always positive
     * @param offset the number of initial results to skip, or 0 if the first results are to be returned
     * @return the activity that will perform the work
     * @throws IllegalArgumentException if the execution context or workspace name are null
     * @throws SearchEngineException if there is a problem during optimization
     * @throws InvalidWorkspaceException if the workspace does not exist
     */
    public List<Location> fullTextSearch( ExecutionContext context,
                                          String workspaceName,
                                          String fullTextSearch,
                                          int maxResults,
                                          int offset ) {
        CheckArg.isNotNull(context, "context");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        Search searchActivity = searchContent(context, fullTextSearch, maxResults, offset);
        getWorkspace(workspaceName).execute(false, searchActivity);
        return searchActivity.getResults();
    }

    /**
     * Perform a query of the content in the named workspace, given the Abstract Query Model representation of the query.
     * 
     * @param context the execution context in which the search is to take place; may not be null
     * @param workspaceName the name of the workspace
     * @param query the query that is to be executed, in the form of the Abstract Query Model
     * @param schemata the definition of the tables and views that can be used in the query; may not be null
     * @return the query results; never null
     * @throws IllegalArgumentException if the context, query, or schemata references are null
     */
    public QueryResults query( ExecutionContext context,
                               String workspaceName,
                               QueryCommand query,
                               Schemata schemata ) {
        CheckArg.isNotNull(context, "context");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        CheckArg.isNotNull(query, "query");
        CheckArg.isNotNull(schemata, "schemata");
        QueryContext queryContext = new QueryContext(context, schemata);
        Query queryActivity = queryContent(queryContext, query);
        getWorkspace(workspaceName).execute(false, queryActivity);
        return queryActivity.getResults();
    }

    /**
     * Remove the supplied index from the search engine. This is typically done when the workspace has been deleted from the
     * source, or when
     * 
     * @param workspaceName the name of the workspace
     * @throws IllegalArgumentException if the workspace name is null
     * @throws SearchEngineException if there is a problem removing the workspace
     */
    public void removeWorkspace( String workspaceName ) throws SearchEngineException {
        CheckArg.isNotNull(workspaceName, "workspaceName");
        try {
            workspacesLock.writeLock().lock();
            // Check whether another thread got in and created the engine while we waited ...
            Workspace workspace = workspacesByName.remove(workspaceName);
            if (workspace != null) {
                indexLayout.destroyIndexes(context, getSourceName(), workspaceName);
            }
        } catch (IOException e) {
            String message = GraphI18n.errorWhileRemovingIndexesForWorkspace.text(sourceName, workspaceName, e.getMessage());
            throw new SearchEngineException(message, e);
        } finally {
            workspacesLock.writeLock().unlock();
        }
    }

    /**
     * Remove from the search engine all workspace-related indexes, thereby cleaning up any resources used by this search engine.
     * 
     * @throws SearchEngineException if there is a problem removing any of the workspace
     */
    public void removeWorkspaces() throws SearchEngineException {
        try {
            workspacesLock.writeLock().lock();
            for (String workspaceName : new HashSet<String>(workspacesByName.keySet())) {
                removeWorkspace(workspaceName);
            }
        } finally {
            workspacesLock.writeLock().unlock();
        }
    }

    /**
     * Get the search engine for the workspace with the supplied name.
     * 
     * @param workspaceName the name of the workspace
     * @return the workspace's search engine
     * @throws InvalidWorkspaceException if the workspace does not exist
     */
    protected Workspace getWorkspace( String workspaceName ) {
        Workspace workspace = null;
        try {
            workspacesLock.readLock().lock();
            workspace = workspacesByName.get(workspaceName);
        } finally {
            workspacesLock.readLock().unlock();
        }

        if (workspace == null) {
            // Verify the workspace does exist ...
            if (!graph().getWorkspaces().contains(workspaceName)) {
                String msg = GraphI18n.workspaceDoesNotExistInRepository.text(workspaceName, getSourceName());
                throw new InvalidWorkspaceException(msg);
            }
            try {
                workspacesLock.writeLock().lock();
                // Check whether another thread got in and created the engine while we waited ...
                workspace = workspacesByName.get(workspaceName);
                if (workspace == null) {
                    // Create the engine and register it ...
                    workspace = new Workspace(workspaceName);
                    workspacesByName.put(workspaceName, workspace);
                }
            } finally {
                workspacesLock.writeLock().unlock();
            }
        }
        return workspace;
    }

    protected class Workspace {
        private final String sourceName;
        private final String workspaceName;
        protected final AtomicInteger modifiedNodesSinceLastOptimize = new AtomicInteger(0);

        protected Workspace( String workspaceName ) {
            this.workspaceName = workspaceName;
            this.sourceName = getSourceName();
        }

        /**
         * Get the workspace name.
         * 
         * @return the workspace name; never null
         */
        public String getWorkspaceName() {
            return workspaceName;
        }

        /**
         * Execute the supplied activities against the indexes.
         * 
         * @param overwrite true if the existing indexes should be overwritten, or false if they should be used
         * @param activities the activities to execute
         * @throws SearchEngineException if there is a problem performing the activities
         */
        protected final void execute( boolean overwrite,
                                      Activity... activities ) throws SearchEngineException {
            // Determine if the activities are readonly ...
            boolean readOnly = true;
            for (Activity activity : activities) {
                if (!(activity instanceof ReadOnlyActivity)) {
                    readOnly = false;
                    break;
                }
            }

            // Create a session ...
            Session session = indexLayout.createSession(context, sourceName, workspaceName, overwrite, readOnly);
            assert session != null;

            // Execute the various activities ...
            Throwable error = null;
            try {
                int numChanges = 0;
                for (Activity activity : activities) {
                    try {
                        numChanges += activity.execute(session);
                    } catch (RuntimeException e) {
                        error = e;
                        throw e;
                    }
                }
                if (numChanges > 0) {
                    numChanges = this.modifiedNodesSinceLastOptimize.addAndGet(numChanges);
                    // Determine if there have been enough changes made to run the optimizer ...
                    int maxChanges = getMaxChangesBeforeAutomaticOptimization();
                    if (maxChanges > 0 && numChanges >= maxChanges) {
                        Activity optimizer = optimizeContent();
                        try {
                            optimizer.execute(session);
                        } catch (RuntimeException e) {
                            error = e;
                            throw e;
                        }
                    }
                }
            } finally {
                try {
                    if (error == null) {
                        session.commit();
                    } else {
                        session.rollback();
                    }
                } catch (RuntimeException e2) {
                    // We don't want to lose the existing error, if there is one ...
                    if (error == null) {
                        I18n msg = GraphI18n.errorWhileCommittingIndexChanges;
                        throw new SearchEngineException(msg.text(workspaceName, sourceName, e2.getMessage()), e2);
                    }
                }
            }
        }
    }

    /**
     * Create an activity that will optimize the indexes.
     * 
     * @return the activity that will perform the work
     */
    protected Activity optimizeContent() {
        return new Activity() {
            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.graph.search.SearchEngine.Activity#execute(org.jboss.dna.graph.search.SearchProvider.Session)
             */
            public int execute( Session session ) {
                session.optimize();
                return 0; // no lines changed
            }

            public String messageFor( Throwable error,
                                      String sourceName,
                                      String workspaceName ) {
                return GraphI18n.errorWhileOptimizingIndexes.text(sourceName, workspaceName, error.getMessage());
            }
        };
    }

    /**
     * Create an activity that will read from the source the content at the supplied location and add the content to the search
     * index.
     * 
     * @param location the location of the content to read; may not be null
     * @param depthPerRead the depth of each read operation; always positive
     * @return the activity that will perform the work
     */
    protected Activity addContent( final Location location,
                                   final int depthPerRead ) {
        return new Activity() {
            public int execute( Session session ) {

                // Create a queue that we'll use to walk the content ...
                LinkedList<Location> locationsToRead = new LinkedList<Location>();
                locationsToRead.add(location);
                int count = 0;

                // Now read and index the content ...
                Graph graph = graph();
                graph.useWorkspace(session.getWorkspaceName());
                while (!locationsToRead.isEmpty()) {
                    Location location = locationsToRead.poll();
                    if (location == null) continue;
                    Subgraph subgraph = graph.getSubgraphOfDepth(depthPerRead).at(location);
                    // Index all of the nodes within this subgraph ...
                    for (SubgraphNode node : subgraph) {
                        // Index the node ...
                        session.index(node);
                        ++count;

                        // Process the children ...
                        for (Location child : node.getChildren()) {
                            if (!subgraph.includes(child)) {
                                // Record this location as needing to be read ...
                                locationsToRead.add(child);
                            }
                        }
                    }
                }
                return count;
            }

            public String messageFor( Throwable error,
                                      String sourceName,
                                      String workspaceName ) {
                String path = readable(location.getPath());
                return GraphI18n.errorWhileIndexingContentAtPath.text(path, workspaceName, sourceName, error.getMessage());
            }
        };
    }

    /**
     * Create an activity that will remove from the indexes all documents that represent content at or below the specified
     * location.
     * 
     * @param location the location of the content to removed; may not be null
     * @return the activity that will perform the work
     */
    protected Activity removeContent( final Location location ) {
        return new Activity() {

            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.graph.search.SearchEngine.Activity#execute(org.jboss.dna.graph.search.SearchProvider.Session)
             */
            public int execute( Session session ) {
                // Delete the content at/below the path ...
                return session.deleteBelow(location.getPath());
            }

            public String messageFor( Throwable error,
                                      String sourceName,
                                      String workspaceName ) {
                String path = readable(location.getPath());
                return GraphI18n.errorWhileRemovingContentAtPath.text(path, workspaceName, sourceName, error.getMessage());
            }
        };
    }

    /**
     * Create an activity that will update the indexes with changes that were already made to the content.
     * 
     * @param changes the changes that have been made to the content; may not be null
     * @return the activity that will perform the work
     */
    protected Activity updateContent( final Iterable<ChangeRequest> changes ) {
        return new Activity() {

            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.graph.search.SearchEngine.Activity#execute(org.jboss.dna.graph.search.SearchProvider.Session)
             */
            public int execute( Session session ) {
                return session.apply(changes);
            }

            public String messageFor( Throwable error,
                                      String sourceName,
                                      String workspaceName ) {
                return GraphI18n.errorWhileUpdatingContent.text(workspaceName, sourceName, error.getMessage());
            }
        };
    }

    /**
     * Create an activity that will perform a full-text search given the supplied query.
     * 
     * @param context the context in which the search is to be performed; may not be null
     * @param fullTextSearch the full-text search to be performed; may not be null
     * @param maxResults the maximum number of results that are to be returned; always positive
     * @param offset the number of initial results to skip, or 0 if the first results are to be returned
     * @return the activity that will perform the work; never null
     */
    protected Search searchContent( final ExecutionContext context,
                                    final String fullTextSearch,
                                    final int maxResults,
                                    final int offset ) {
        final List<Location> results = new ArrayList<Location>(maxResults);
        return new Search() {
            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.graph.search.SearchEngine.Activity#execute(org.jboss.dna.graph.search.SearchProvider.Session)
             */
            public int execute( Session session ) {
                session.search(context, fullTextSearch, maxResults, offset, results);
                return 0;
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.graph.search.SearchEngine.Activity#messageFor(java.lang.Throwable, java.lang.String,
             *      java.lang.String)
             */
            public String messageFor( Throwable error,
                                      String sourceName,
                                      String workspaceName ) {
                return GraphI18n.errorWhilePerformingSearch.text(fullTextSearch, workspaceName, sourceName, error.getMessage());
            }

            public List<Location> getResults() {
                return results;
            }
        };
    }

    /**
     * Create an activity that will perform a query against the index.
     * 
     * @param context the context in which the search is to be performed; may not be null
     * @param query the query to be performed; may not be null
     * @return the activity that will perform the query; never null
     */
    protected Query queryContent( final QueryContext context,
                                  final QueryCommand query ) {
        return new Query() {
            private QueryResults results = null;

            public int execute( Session session ) throws SearchException {
                results = session.query(context, query);
                return 0;
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.jboss.dna.graph.search.SearchEngine.Activity#messageFor(java.lang.Throwable, java.lang.String,
             *      java.lang.String)
             */
            public String messageFor( Throwable error,
                                      String sourceName,
                                      String workspaceName ) {
                return GraphI18n.errorWhilePerformingQuery.text(query, workspaceName, sourceName, error.getMessage());
            }

            public QueryResults getResults() {
                return results;
            }
        };
    }

    /**
     * Interface for activities that will be executed against a workspace. These activities don't have to commit or roll back the
     * writer, nor do they have to translate the exceptions, since this is done by the
     * {@link Workspace#execute(boolean, Activity...)} method.
     */
    protected interface Activity {

        /**
         * Perform the activity by using the index writer.
         * 
         * @param indexSession the index session that should be used by the activity; never null
         * @return the number of changes that were made by this activity
         */
        int execute( Session indexSession );

        /**
         * Translate an exception obtained during {@link #execute(Session) execution} into a single message.
         * 
         * @param t the exception
         * @param sourceName the name of the source
         * @param workspaceName the name of the workspace
         * @return the error message
         */
        String messageFor( Throwable t,
                           String sourceName,
                           String workspaceName );
    }

    /**
     * A read-only activity.
     */
    protected interface ReadOnlyActivity extends Activity {
    }

    /**
     * A search activity.
     */
    protected interface Search extends ReadOnlyActivity {
        /**
         * Get the results of the search.
         * 
         * @return the list of {@link Location} objects for each node satisfying the results; never null
         */
        List<Location> getResults();
    }

    /**
     * A query activity.
     */
    protected interface Query extends ReadOnlyActivity {
        /**
         * Get the results of the query.
         * 
         * @return the results of a query; never null
         */
        QueryResults getResults();
    }

}
