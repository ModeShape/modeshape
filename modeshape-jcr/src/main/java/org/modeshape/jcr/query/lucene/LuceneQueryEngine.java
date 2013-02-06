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
package org.modeshape.jcr.query.lucene;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import org.apache.lucene.search.Collector;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.MatchAllDocsQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.util.Version;
import org.hibernate.search.SearchFactory;
import org.hibernate.search.engine.spi.SearchFactoryImplementor;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.api.query.QueryCancelledException;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.query.CancellableQuery;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.QueryIndexing;
import org.modeshape.jcr.query.QueryResults;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.lucene.basic.BasicLuceneSchema;
import org.modeshape.jcr.query.lucene.basic.ExistsTupleCollector;
import org.modeshape.jcr.query.model.Constraint;
import org.modeshape.jcr.query.model.FullTextSearchScore;
import org.modeshape.jcr.query.model.QueryCommand;
import org.modeshape.jcr.query.optimize.Optimizer;
import org.modeshape.jcr.query.plan.PlanHints;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.Planner;
import org.modeshape.jcr.query.process.AbstractAccessComponent;
import org.modeshape.jcr.query.process.ProcessingComponent;
import org.modeshape.jcr.query.process.QueryEngine;
import org.modeshape.jcr.query.process.QueryProcessor;
import org.modeshape.jcr.query.process.SelectComponent;
import org.modeshape.jcr.query.validate.Schemata;

/**
 * A {@link QueryEngine} that uses Lucene for answering queries. Each repository uses a single {@link LuceneQueryEngine} instance,
 * and all sessions share the same engine. Therefore, this engine is threadsafe (and actually immutable), creating objects for
 * each {@link #execute(QueryContext, QueryCommand) submitted query}.
 */
public class LuceneQueryEngine extends QueryEngine {

    private static final Logger LOGGER = Logger.getLogger(LuceneQueryEngine.class);

    private final ExecutionContext repositoryContext;
    private final BasicLuceneSchema schema;

    // private final String repositoryName;

    /**
     * @param context the execution context for the repository
     * @param repositoryName the name of the repository
     * @param planner the planner that should be used
     * @param optimizer the optimizer that should be used
     * @param searchFactory the search factory for accessing the indexes
     * @param version the Lucene version used by the indexes
     * @param enableFullTextSearch true if full-text searching is enabled, or false otherwise
     */
    public LuceneQueryEngine( ExecutionContext context,
                              String repositoryName,
                              Planner planner,
                              Optimizer optimizer,
                              SearchFactoryImplementor searchFactory,
                              Version version,
                              boolean enableFullTextSearch ) {
        super(planner, optimizer, new LuceneQueryProcessor(repositoryName, searchFactory));
        this.repositoryContext = context;
        // this.repositoryName = repositoryName;
        this.schema = new BasicLuceneSchema(this.repositoryContext, searchFactory, version, enableFullTextSearch);
        ((LuceneQueryProcessor)this.processor).initialize(schema);
    }

    public void shutdown() {
        this.schema.shutdown();
    }

    /**
     * Get the interface for updating the indexes.
     * 
     * @return the indexing interface
     */
    public QueryIndexing getQueryIndexing() {
        return this.schema;
    }

    /**
     * Execute the supplied query against the named workspace, using the supplied hints, schemata and variables.
     * 
     * @param context the context in which the query is being executed; may not be null
     * @param repositoryCache the repository cache that should be used to load results; may not be null
     * @param workspaceNames the name of each workspace to be queried, or an empty set if all the workspaces should be queried;
     *        may not be null
     * @param overriddenNodeCachesByWorkspaceName the NodeCache instances that should be used to load results, which will be used
     *        instead of the RepositoryCache's NodeCache for a given workspace name; may be null or empty
     * @param query the query
     * @param schemata the schemata information that should be used for all processing of this query
     * @param hints the hints
     * @param variables the variables
     * @return the results
     * @throws InvalidQueryException if the
     */
    public CancellableQuery query( ExecutionContext context,
                                   RepositoryCache repositoryCache,
                                   Set<String> workspaceNames,
                                   Map<String, NodeCache> overriddenNodeCachesByWorkspaceName,
                                   final QueryCommand query,
                                   Schemata schemata,
                                   PlanHints hints,
                                   Map<String, Object> variables ) throws InvalidQueryException {
        final QueryContext queryContext = new QueryContext(context, repositoryCache, workspaceNames,
                                                           overriddenNodeCachesByWorkspaceName, schemata, hints, null, variables);
        return new CancellableQuery() {
            private final Lock lock = new ReentrantLock();
            private QueryResults results;

            @Override
            public QueryResults getResults() throws QueryCancelledException, RepositoryException {
                try {
                    lock.lock();
                    if (results == null) {
                        results = execute(queryContext, query); // this will block and will hold the lock until it is done ...
                    }
                    return results;
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public boolean cancel() {
                return queryContext.cancel();
            }
        };
    }

    /**
     * The {@link QueryProcessor} implementation used by this {@link LuceneQueryEngine}. It varies from the abstract class only by
     * creating a {@link LuceneAccessQuery} for each access query, and which is resposible for submitting the access query to
     * Lucene.
     */
    protected static class LuceneQueryProcessor extends QueryProcessor<LuceneProcessingContext> {
        private final SearchFactory searchFactory;
        private final String repositoryName;
        private LuceneSchema schema;

        protected LuceneQueryProcessor( String repositoryName,
                                        SearchFactory searchFactory ) {
            this.searchFactory = searchFactory;
            this.repositoryName = repositoryName;
        }

        protected void initialize( LuceneSchema schema ) {
            this.schema = schema;
            assert this.schema != null;
        }

        @Override
        protected boolean supportsPushDownExistConstraints() {
            return true;
        }

        @Override
        protected LuceneProcessingContext createProcessingContext( QueryContext queryContext ) {
            return new LuceneProcessingContext(queryContext, repositoryName, searchFactory, schema);
        }

        @Override
        protected void closeProcessingContext( LuceneProcessingContext processingContext ) {
            // This method will _ALWAYS_ be called by the superclass if createProcessingContext() is called
            assert processingContext != null;
            // Close the index readers ...
            processingContext.close();
        }

        @Override
        protected ProcessingComponent createAccessComponent( QueryCommand originalQuery,
                                                             QueryContext context,
                                                             PlanNode accessNode,
                                                             Columns resultColumns,
                                                             LuceneProcessingContext processingContext ) {
            assert this.schema != null;
            return new LuceneAccessQuery(schema, processingContext, context, resultColumns, accessNode);
        }
    }

    /**
     * The component that is created to represent a single access query and, when executed, transforms that access query into a
     * single Lucene query and issues it against Lucene.
     */
    protected static class LuceneAccessQuery extends AbstractAccessComponent {
        private final LuceneProcessingContext processingContext;
        private final LuceneSchema schema;

        public LuceneAccessQuery( LuceneSchema schema,
                                  LuceneProcessingContext processingContext,
                                  QueryContext context,
                                  Columns resultColumns,
                                  PlanNode accessNode ) {
            super(context, resultColumns, accessNode);
            this.schema = schema;
            this.processingContext = processingContext;
        }

        /**
         * {@inheritDoc}
         * <p>
         * Some kinds of constraints are not easily pushed down to Lucene as are of a Lucene Query, and instead are applied by
         * filtering the results. For example, a {@link FullTextSearchScore} applies to the score of the tuple, which cannot be
         * (easily?) applied as a {@link Query Lucene Query}.
         * </p>
         * <p>
         * Therefore, each of the AND-ed constraints of the query are evaluated separately. After all, each of the tuples returned
         * by the planned access query must satisfy all of the AND-ed constraints. Or, to put it another way, if a tuple does not
         * satisfy one of the AND-ed constraints, the tuple should not be included in the query results.
         * </p>
         * <p>
         * Logically, any AND-ed criteria that cannot be pushed down to Lucene can of course be applied as a filter on the
         * results. Thus, each AND-ed constraint is processed to first determine if it can be represented as a Lucene query; all
         * other AND-ed constraints must be handled as a results filter. Since most queries will likely use one or more simple
         * constraints AND-ed together, this approach will likely work very well.
         * </p>
         * <p>
         * The only hairy case is when any AND-ed constraint is actually an OR-ed combination of multiple constraints of which at
         * least one cannot be pushed down to Lucene. In this case, the entire AND-ed constraint must be treated as a results
         * filter (even if many of those constraints that make up the OR-ed constraint can be pushed down). Hopefully, this will
         * not be a common case in actual queries.
         * </p>
         * 
         * @see AbstractAccessComponent#execute()
         */
        @Override
        public List<Object[]> execute() {
            assert andedConstraints != null;
            assert limit != null;

            // Create the Lucene queries ...
            LuceneQuery queries = schema.createQuery(sourceName, andedConstraints, processingContext);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Executing the lucene query: {0}", queries.toString());
            }
            // Check whether the constraints were such that no results should be returned ...
            if (queries.matchesNone()) {
                // There are no results ...
                return Collections.emptyList();
            }

            // Otherwise, there are queries that should be executed ...
            Query pushDownQuery = queries.getPushDownQuery();
            if (pushDownQuery == null) {
                // There are no constraints that can be pushed down, so return _all_ the nodes ...
                pushDownQuery = new MatchAllDocsQuery();
            }

            // Get the results from Lucene ...
            QueryContext queryContext = getContext();
            List<Object[]> tuples = null;
            final Columns columns = getColumns();
            if (pushDownQuery instanceof MatchNoneQuery) {
                // There are no results ...
                tuples = Collections.emptyList();
            } else {
                try {
                    // Execute the query against the content indexes ...
                    String indexName = queries.getPushDownIndexName();
                    IndexSearcher searcher = processingContext.getSearcher(indexName);
                    Logger logger = Logger.getLogger(getClass());
                    if (logger.isTraceEnabled()) {
                        Set<String> workspaceNames = processingContext.getWorkspaceNames();
                        String repoName = processingContext.getRepositoryName();
                        logger.trace("query \"{0}\" workspace(s) in \"{1}\" repository: {2}",
                                     repoName,
                                     workspaceNames,
                                     pushDownQuery);
                    }
                    TupleCollector collector = schema.createTupleCollector(queryContext, columns);
                    if (getContext().getHints().isExistsQuery) {
                        // We only are looking for the existance of a tuple, so we want to stop as soon as we find one.
                        // So wrap the collector with an ExistsTupleCollector that will throw an exception as soon as one tuple
                        // is found.
                        collector = new ExistsTupleCollector(collector);
                        try {
                            searcher.search(pushDownQuery, collector);
                        } catch (ExistsTupleCollector.CompletedException e) {
                            // This only happens when the query has a row limit of 1, and we've found a tuple.
                            // So we should eat this exception and just continue ...
                        }
                    } else {
                        searcher.search(pushDownQuery, collector);
                    }
                    tuples = collector.getTuples();
                } catch (QueryCancelledIOException e) {
                    assert queryContext.isCancelled();
                    return Collections.emptyList();
                } catch (IOException e) {
                    throw new LuceneException(e);
                }
            }

            if (!tuples.isEmpty() && !queryContext.isCancelled()) {
                Constraint postProcessingConstraints = queries.getPostProcessingConstraints();
                if (postProcessingConstraints != null) {
                    // Create a delegate processing component that will return the tuples we've already found ...
                    final List<Object[]> allTuples = tuples;
                    ProcessingComponent tuplesProcessor = new ProcessingComponent(queryContext, columns) {
                        @Override
                        public List<Object[]> execute() {
                            return allTuples;
                        }
                    };
                    if (queryContext.isCancelled()) return Collections.emptyList();

                    // Create a processing component that will apply these constraints to the tuples we already found ...
                    SelectComponent selector = new SelectComponent(tuplesProcessor, postProcessingConstraints,
                                                                   queryContext.getVariables());
                    tuples = selector.execute();
                }

                // Limit the tuples ...
                if (!limit.isUnlimited()) {
                    int firstIndex = limit.getOffset();
                    int maxRows = Math.min(tuples.size(), limit.getRowLimit());
                    if (firstIndex > 0) {
                        // There is an offset and we're limiting the number of rows ...
                        if (firstIndex > tuples.size()) {
                            tuples.clear(); // not enough rows, so return no tuples
                        } else {
                            // Find the tuples for the range of rows we're interested in ...
                            tuples = tuples.subList(firstIndex, maxRows);
                        }
                    } else {
                        // We're limiting the number of rows but there is no offset ...
                        tuples = tuples.subList(0, maxRows);
                    }
                }
            }

            return tuples;
        }
    }

    public static abstract class TupleCollector extends Collector {

        /**
         * Get the tuples.
         * 
         * @return the tuples; never null
         */
        public abstract List<Object[]> getTuples();

        @Override
        public final void collect( int doc ) throws IOException {
            doCollect(doc);
        }

        public abstract float doCollect( int doc ) throws IOException;

    }
}
