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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.jcip.annotations.ThreadSafe;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriter.MaxFieldLength;
import org.apache.lucene.queryParser.ParseException;
import org.apache.lucene.store.Directory;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Subgraph;
import org.jboss.dna.graph.SubgraphNode;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.query.QueryResults;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.request.ChangeRequest;

/**
 * A search engine dedicated to a single workspace.
 */
@ThreadSafe
public class WorkspaceSearchEngine {

    protected static final String PATHS_INDEX_NAME = "paths";
    protected static final String CONTENT_INDEX_NAME = "content";

    private final Directory pathsDirectory;
    private final Directory contentDirectory;
    private final ExecutionContext context;
    private final ExecutionContext encodedContext;
    private final String sourceName;
    private final String workspaceName;
    private final RepositoryConnectionFactory connectionFactory;
    private final IndexingStrategy indexingStrategy;
    protected final AtomicInteger modifiedNodesSinceLastOptimize = new AtomicInteger(0);

    /**
     * Create a search engine instance given the supplied {@link ExecutionContext execution context}, name of the
     * {@link RepositorySource}, the {@link RepositoryConnectionFactory factory for RepositorySource connections}, and the
     * {@link DirectoryConfiguration directory factory} that defines where each workspace's indexes should be placed.
     * 
     * @param context the execution context in which all indexing operations should be performed
     * @param directoryFactory the factory from which can be obtained the Lucene directory where the indexes should be persisted
     * @param indexingStrategy the indexing strategy that governs how properties are to be indexed; may not be null
     * @param sourceName the name of the {@link RepositorySource}
     * @param workspaceName the name of the workspace
     * @param connectionFactory the connection factory
     * @throws IllegalArgumentException if any of the parameters are null
     * @throws SearchEngineException if there is a problem initializing this engine
     */
    protected WorkspaceSearchEngine( ExecutionContext context,
                                     DirectoryConfiguration directoryFactory,
                                     IndexingStrategy indexingStrategy,
                                     String sourceName,
                                     String workspaceName,
                                     RepositoryConnectionFactory connectionFactory ) throws SearchEngineException {
        CheckArg.isNotNull(context, "context");
        CheckArg.isNotNull(directoryFactory, "directoryFactory");
        CheckArg.isNotNull(sourceName, "sourceName");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        CheckArg.isNotNull(connectionFactory, "connectionFactory");
        CheckArg.isNotNull(indexingStrategy, "indexingStrategy");
        this.sourceName = sourceName;
        this.workspaceName = workspaceName;
        this.connectionFactory = connectionFactory;
        this.context = context;
        this.indexingStrategy = indexingStrategy;
        this.encodedContext = context.with(new EncodingNamespaceRegistry(context.getNamespaceRegistry(),
                                                                         this.indexingStrategy.getNamespaceEncoder()));
        this.pathsDirectory = directoryFactory.getDirectory(workspaceName, PATHS_INDEX_NAME);
        this.contentDirectory = directoryFactory.getDirectory(workspaceName, CONTENT_INDEX_NAME);

        initialize();
    }

    protected void initialize() throws SearchEngineException {
        // Always create the index if not there ...
        try {
            Analyzer analyzer = this.indexingStrategy.createAnalyzer();
            ensureIndexesExist(this.pathsDirectory, analyzer);
            ensureIndexesExist(this.contentDirectory, analyzer);
        } catch (IOException e) {
            String msg = SearchI18n.errorWhileInitializingSearchEngine.text(workspaceName, sourceName, e.getMessage());
            throw new SearchEngineException(msg, e);
        }
    }

    private static void ensureIndexesExist( Directory directory,
                                            Analyzer analyzer ) throws IOException {
        IndexWriter writer = null;
        Throwable error = null;
        try {
            writer = new IndexWriter(directory, analyzer, false, MaxFieldLength.UNLIMITED);
        } catch (FileNotFoundException e) {
            // The index files don't yet exist, so we need to create them ...
            try {
                writer = new IndexWriter(directory, analyzer, true, MaxFieldLength.UNLIMITED);
            } catch (Throwable t) {
                error = t;
            }
        } catch (Throwable t) {
            error = t;
        } finally {
            if (writer != null) {
                // Either way, make sure we close the writer that we created ...
                try {
                    writer.close();
                } catch (IOException e) {
                    if (error == null) throw e;
                } catch (RuntimeException e) {
                    if (error == null) throw e;
                }
            }
        }
    }

    final Graph graph() {
        Graph graph = Graph.create(sourceName, connectionFactory, context);
        graph.useWorkspace(workspaceName);
        return graph;
    }

    final String workspaceName() {
        return workspaceName;
    }

    final String sourceName() {
        return sourceName;
    }

    final String readable( Path path ) {
        return context.getValueFactories().getStringFactory().create(path);
    }

    final IndexingStrategy strategy() {
        return indexingStrategy;
    }

    /**
     * Index all of the content at or below the supplied path.
     * 
     * @param startingPoint the path that represents the content to be indexed
     * @param depthPerBatch the depth of each subgraph read operation
     * @throws IllegalArgumentException if the path is null or the depth is not positive
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws SearchEngineException if there is a problem updating the indexes
     */
    public void indexContent( Path startingPoint,
                              int depthPerBatch ) throws RepositorySourceException, SearchEngineException {
        CheckArg.isNotNull(startingPoint, "startingPoint");
        indexContent(Location.create(startingPoint), depthPerBatch);
    }

    /**
     * Index all of the content at or below the supplied location.
     * 
     * @param startingPoint the location that represents the content to be indexed
     * @param depthPerRead the depth of each subgraph read operation
     * @throws IllegalArgumentException if the location is null or the depth is not positive
     * @throws RepositorySourceException if there is a problem accessing the content
     * @throws SearchEngineException if there is a problem updating the indexes
     */
    public void indexContent( Location startingPoint,
                              int depthPerRead ) throws RepositorySourceException, SearchEngineException {
        CheckArg.isNotNull(startingPoint, "startingPoint");
        CheckArg.isPositive(depthPerRead, "depthPerBatch");
        assert startingPoint.hasPath();

        if (startingPoint.getPath().isRoot()) {
            // More efficient to just start over with a new index ...
            execute(true, addContent(startingPoint, depthPerRead));
        } else {
            // Have to first remove the content below the starting point, then add it again ...
            execute(false, removeContent(startingPoint), addContent(startingPoint, depthPerRead));
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
        CheckArg.isNotNull(changes, "changes");
        execute(false, updateContent(changes));
    }

    /**
     * Invoke the engine's garbage collection on all indexes used by this workspace. This method reclaims space and optimizes the
     * index. This should be done on a periodic basis after changes are made to the engine's indexes.
     * 
     * @throws SearchEngineException if there is a problem during optimization
     */
    public void optimize() throws SearchEngineException {
        execute(false, optimizeContent());
    }

    /**
     * Create an activity that will perform a full-text search given the supplied query.
     * 
     * @param fullTextSearch the full-text search to be performed; may not be null
     * @param maxResults the maximum number of results that are to be returned; always positive
     * @param offset the number of initial results to skip, or 0 if the first results are to be returned
     * @return the activity that will perform the work
     */
    public List<Location> fullTextSearch( final String fullTextSearch,
                                          final int maxResults,
                                          final int offset ) {
        return execute(false, searchContent(fullTextSearch, maxResults, offset)).getResults();
    }

    /**
     * Create an activity that will perform a query of the content in this workspace, given the Abstract Query Model
     * representation of the query.
     * 
     * @param query the query that is to be executed, in the form of the Abstract Query Model
     * @return the query results; never null
     * @throws IllegalArgumentException if the context or query references are null
     */
    public QueryResults execute( QueryCommand query ) {
        return execute(false, queryContent(query)).getResults();
    }

    /**
     * Execute the supplied activities against the indexes.
     * 
     * @param <ActivityType> the type of activity
     * @param overwrite true if the existing indexes should be overwritten, or false if they should be used
     * @param activity the activity to execute
     * @return the same activity that was supplied as a parameter, returned as a convenience
     * @throws SearchEngineException if there is a problem performing the activities
     */
    protected final <ActivityType extends Activity> ActivityType execute( boolean overwrite,
                                                                          ActivityType activity ) throws SearchEngineException {
        execute(overwrite, new Activity[] {activity});
        return activity;
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
        Analyzer analyzer = this.indexingStrategy.createAnalyzer();
        IndexContext indexes = new IndexContext(encodedContext, pathsDirectory, contentDirectory, analyzer, overwrite);

        // Execute the various activities ...
        Throwable error = null;
        try {
            for (Activity activity : activities) {
                try {
                    activity.execute(indexes);
                } catch (IOException e) {
                    error = e;
                    throw new SearchEngineException(activity.messageFor(e), e);
                } catch (ParseException e) {
                    error = e;
                    throw new SearchEngineException(activity.messageFor(e), e);
                } catch (RuntimeException e) {
                    error = e;
                    throw e;
                }
            }
            if (indexes.hasWriters()) {
                // Determine if there have been enough changes made to run the optimizer ...
                int maxChanges = this.indexingStrategy.getChangeCountForAutomaticOptimization();
                if (maxChanges > 0 && this.modifiedNodesSinceLastOptimize.get() >= maxChanges) {
                    Activity optimizer = optimizeContent();
                    try {
                        optimizer.execute(indexes);
                    } catch (ParseException e) {
                        error = e;
                        throw new SearchEngineException(optimizer.messageFor(e), e);
                    } catch (IOException e) {
                        error = e;
                        throw new SearchEngineException(optimizer.messageFor(e), e);
                    } catch (RuntimeException e) {
                        error = e;
                        throw e;
                    }
                }
            }
        } finally {
            try {
                if (error == null) {
                    indexes.commit();
                } else {
                    indexes.rollback();
                }
            } catch (IOException e2) {
                // We don't want to lose the existing error, if there is one ...
                if (error == null) {
                    I18n msg = SearchI18n.errorWhileCommittingIndexChanges;
                    throw new SearchEngineException(msg.text(workspaceName(), sourceName(), e2.getMessage()), e2);
                }
            }
        }
    }

    /**
     * Interface for activities that will be executed against the set of indexes. These activities don't have to commit or roll
     * back the writer, nor do they have to translate the exceptions, since this is done by the
     * {@link WorkspaceSearchEngine#execute(boolean, Activity...)} method.
     */
    protected interface Activity {

        /**
         * Perform the activity by using the index writer.
         * 
         * @param indexes the set of indexes to use; never null
         * @throws IOException if there is an error using the writer
         * @throws ParseException if there is an error due to parsing
         */
        void execute( IndexContext indexes ) throws IOException, ParseException;

        /**
         * Translate an exception obtained during {@link #execute(IndexContext) execution} into a single message.
         * 
         * @param t the exception
         * @return the error message
         */
        String messageFor( Throwable t );
    }

    protected interface Search extends Activity {
        /**
         * Get the results of the search.
         * 
         * @return the list of {@link Location} objects for each node satisfying the results; never null
         */
        List<Location> getResults();
    }

    protected interface Query extends Activity {
        /**
         * Get the results of the query.
         * 
         * @return the results of a query; never null
         */
        QueryResults getResults();
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
            public void execute( IndexContext indexes ) throws IOException {

                // Create a queue that we'll use to walk the content ...
                LinkedList<Location> locationsToRead = new LinkedList<Location>();
                locationsToRead.add(location);
                int count = 0;

                // Now read and index the content ...
                Graph graph = graph();
                while (!locationsToRead.isEmpty()) {
                    Location location = locationsToRead.poll();
                    if (location == null) continue;
                    Subgraph subgraph = graph.getSubgraphOfDepth(depthPerRead).at(location);
                    // Index all of the nodes within this subgraph ...
                    for (SubgraphNode node : subgraph) {
                        // Index the node ...
                        strategy().index(node, indexes);
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
                modifiedNodesSinceLastOptimize.addAndGet(count);
            }

            public String messageFor( Throwable error ) {
                String path = readable(location.getPath());
                return SearchI18n.errorWhileIndexingContentAtPath.text(path, workspaceName(), sourceName(), error.getMessage());
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

            public void execute( IndexContext indexes ) throws IOException {
                // Delete the content at/below the path ...
                modifiedNodesSinceLastOptimize.addAndGet(strategy().deleteBelow(location.getPath(), indexes));
            }

            public String messageFor( Throwable error ) {
                String path = readable(location.getPath());
                return SearchI18n.errorWhileRemovingContentAtPath.text(path, workspaceName(), sourceName(), error.getMessage());
            }
        };
    }

    /**
     * Create an activity that will optimize the indexes.
     * 
     * @return the activity that will perform the work
     */
    protected Activity optimizeContent() {
        return new Activity() {
            public void execute( IndexContext indexes ) throws IOException {
                // Don't block ...
                indexes.getContentWriter().optimize();
                indexes.getPathsWriter().optimize();
            }

            public String messageFor( Throwable error ) {
                return SearchI18n.errorWhileOptimizingIndexes.text(workspaceName(), sourceName(), error.getMessage());
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

            public void execute( IndexContext indexes ) throws IOException {
                // Iterate over the changes ...
                modifiedNodesSinceLastOptimize.addAndGet(strategy().apply(changes, indexes));
            }

            public String messageFor( Throwable error ) {
                return SearchI18n.errorWhileUpdatingContent.text(workspaceName(), sourceName(), error.getMessage());
            }
        };
    }

    /**
     * Create an activity that will perform a full-text search given the supplied query.
     * 
     * @param fullTextSearch the full-text search to be performed; may not be null
     * @param maxResults the maximum number of results that are to be returned; always positive
     * @param offset the number of initial results to skip, or 0 if the first results are to be returned
     * @return the activity that will perform the work
     */
    protected Search searchContent( final String fullTextSearch,
                                    final int maxResults,
                                    final int offset ) {
        final List<Location> results = new ArrayList<Location>(maxResults);
        return new Search() {
            public void execute( IndexContext indexes ) throws IOException, ParseException {
                strategy().performQuery(fullTextSearch, maxResults, offset, indexes, results);
            }

            public String messageFor( Throwable error ) {
                return SearchI18n.errorWhilePerformingSearch.text(fullTextSearch,
                                                                  workspaceName(),
                                                                  sourceName(),
                                                                  error.getMessage());
            }

            public List<Location> getResults() {
                return results;
            }
        };
    }

    /**
     * Create an activity that will perform a query against the index.
     * 
     * @param query the query to be performed; may not be null
     * @return the activity that will perform the work
     */
    protected Query queryContent( final QueryCommand query ) {
        return new Query() {
            private QueryResults results = null;

            public void execute( IndexContext indexes ) throws IOException, ParseException {
                results = strategy().performQuery(query, indexes);
            }

            public String messageFor( Throwable error ) {
                return SearchI18n.errorWhilePerformingQuery.text(query, workspaceName(), sourceName(), error.getMessage());
            }

            public QueryResults getResults() {
                return results;
            }
        };
    }
}
