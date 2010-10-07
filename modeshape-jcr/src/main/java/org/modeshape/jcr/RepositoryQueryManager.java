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
package org.modeshape.jcr;

import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import org.apache.lucene.analysis.snowball.SnowballAnalyzer;
import org.apache.lucene.util.Version;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.text.UrlEncoder;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.observe.Changes;
import org.modeshape.graph.observe.Observable;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.QueryEngine;
import org.modeshape.graph.query.QueryResults;
import org.modeshape.graph.query.QueryResults.Columns;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.optimize.Optimizer;
import org.modeshape.graph.query.optimize.OptimizerRule;
import org.modeshape.graph.query.optimize.RuleBasedOptimizer;
import org.modeshape.graph.query.plan.CanonicalPlanner;
import org.modeshape.graph.query.plan.PlanHints;
import org.modeshape.graph.query.plan.PlanNode;
import org.modeshape.graph.query.plan.Planner;
import org.modeshape.graph.query.process.AbstractAccessComponent;
import org.modeshape.graph.query.process.ProcessingComponent;
import org.modeshape.graph.query.process.Processor;
import org.modeshape.graph.query.process.QueryProcessor;
import org.modeshape.graph.query.process.SelectComponent.Analyzer;
import org.modeshape.graph.query.validate.Schemata;
import org.modeshape.graph.request.AccessQueryRequest;
import org.modeshape.graph.request.FullTextSearchRequest;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.modeshape.graph.request.processor.RequestProcessor;
import org.modeshape.graph.search.SearchEngine;
import org.modeshape.graph.search.SearchEngineIndexer;
import org.modeshape.graph.search.SearchEngineProcessor;
import org.modeshape.jcr.query.RewritePseudoColumns;
import org.modeshape.search.lucene.IndexRules;
import org.modeshape.search.lucene.LuceneConfiguration;
import org.modeshape.search.lucene.LuceneConfigurations;
import org.modeshape.search.lucene.LuceneSearchEngine;

/**
 * 
 */
abstract class RepositoryQueryManager {

    protected final String sourceName;

    RepositoryQueryManager( String sourceName ) {
        this.sourceName = sourceName;
    }

    public abstract QueryResults query( String workspaceName,
                                        QueryCommand query,
                                        Schemata schemata,
                                        PlanHints hints,
                                        Map<String, Object> variables ) throws InvalidQueryException;

    public abstract QueryResults search( String workspaceName,
                                         String searchExpression,
                                         int maxRowCount,
                                         int offset ) throws InvalidQueryException;

    /**
     * Crawl and index the content in the named workspace.
     * 
     * @throws IllegalArgumentException if the workspace is null
     * @throws InvalidWorkspaceException if there is no workspace with the supplied name
     */
    public void reindexContent() {
        // do nothing by default
    }

    /**
     * Crawl and index the content in the named workspace.
     * 
     * @param workspace the workspace
     * @throws IllegalArgumentException if the workspace is null
     * @throws InvalidWorkspaceException if there is no workspace with the supplied name
     */
    public void reindexContent( JcrWorkspace workspace ) {
        // do nothing by default
    }

    /**
     * Crawl and index the content starting at the supplied path in the named workspace, to the designated depth.
     * 
     * @param workspace the workspace
     * @param path the path of the content to be indexed
     * @param depth the depth of the content to be indexed
     * @throws IllegalArgumentException if the workspace or path are null, or if the depth is less than 1
     * @throws InvalidWorkspaceException if there is no workspace with the supplied name
     */
    public void reindexContent( JcrWorkspace workspace,
                                String path,
                                int depth ) {
        // do nothing by default
    }

    static class PushDown extends RepositoryQueryManager {
        private final ExecutionContext context;
        private final RepositoryConnectionFactory connectionFactory;

        PushDown( String sourceName,
                  ExecutionContext context,
                  RepositoryConnectionFactory connectionFactory ) {
            super(sourceName);
            this.context = context;
            this.connectionFactory = connectionFactory;
        }

        private Graph workspaceGraph( String workspaceName ) {
            Graph graph = Graph.create(this.sourceName, connectionFactory, context);

            if (workspaceName != null) {
                graph.useWorkspace(workspaceName);
            }

            return graph;
        }

        @Override
        public QueryResults query( String workspaceName,
                                   QueryCommand query,
                                   Schemata schemata,
                                   PlanHints hints,
                                   Map<String, Object> variables ) {
            Graph.BuildQuery builder = workspaceGraph(workspaceName).query(query, schemata);
            if (variables != null) builder.using(variables);
            if (hints != null) builder.using(hints);
            return builder.execute();
        }

        @Override
        public QueryResults search( String workspaceName,
                                    String searchExpression,
                                    int maxRowCount,
                                    int offset ) {
            return workspaceGraph(workspaceName).search(searchExpression, maxRowCount, offset);
        }

    }

    static class Disabled extends RepositoryQueryManager {

        Disabled( String sourceName ) {
            super(sourceName);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.RepositoryQueryManager#query(java.lang.String, org.modeshape.graph.query.model.QueryCommand,
         *      org.modeshape.graph.query.validate.Schemata, org.modeshape.graph.query.plan.PlanHints, java.util.Map)
         */
        @Override
        public QueryResults query( String workspaceName,
                                   QueryCommand query,
                                   Schemata schemata,
                                   PlanHints hints,
                                   Map<String, Object> variables ) throws InvalidQueryException {
            throw new InvalidQueryException(JcrI18n.queryIsDisabledInRepository.text(this.sourceName));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.RepositoryQueryManager#search(java.lang.String, java.lang.String, int, int)
         */
        @Override
        public QueryResults search( String workspaceName,
                                    String searchExpression,
                                    int maxRowCount,
                                    int offset ) throws InvalidQueryException {
            throw new InvalidQueryException(JcrI18n.queryIsDisabledInRepository.text(this.sourceName));
        }
    }

    static class SelfContained extends RepositoryQueryManager {
        private final ExecutionContext context;
        private final String sourceName;
        private final LuceneConfiguration configuration;
        private final SearchEngine searchEngine;
        private final Observer searchObserver;
        private final ExecutorService service;
        private final QueryEngine queryEngine;
        private final RepositoryConnectionFactory connectionFactory;
        private final int maxDepthPerRead;

        SelfContained( ExecutionContext context,
                       String nameOfSourceToBeSearchable,
                       RepositoryConnectionFactory connectionFactory,
                       Observable observable,
                       RepositoryNodeTypeManager nodeTypeManager,
                       String indexDirectory,
                       boolean updateIndexesSynchronously,
                       int maxDepthPerRead ) throws RepositoryException {
            super(nameOfSourceToBeSearchable);

            this.context = context;
            this.sourceName = nameOfSourceToBeSearchable;
            this.connectionFactory = connectionFactory;
            this.maxDepthPerRead = maxDepthPerRead;
            // Define the configuration ...
            TextEncoder encoder = new UrlEncoder();
            if (indexDirectory != null) {
                File indexDir = new File(indexDirectory);
                if (indexDir.exists()) {
                    // The location does exist ...
                    if (!indexDir.isDirectory()) {
                        // The path is not a directory ...
                        I18n msg = JcrI18n.searchIndexDirectoryOptionSpecifiesFileNotDirectory;
                        throw new RepositoryException(msg.text(indexDirectory, sourceName));
                    }
                    if (!indexDir.canWrite()) {
                        // But we cannot write to it ...
                        I18n msg = JcrI18n.searchIndexDirectoryOptionSpecifiesDirectoryThatCannotBeWrittenTo;
                        throw new RepositoryException(msg.text(indexDirectory, sourceName));
                    }
                    if (!indexDir.canRead()) {
                        // But we cannot write to it ...
                        I18n msg = JcrI18n.searchIndexDirectoryOptionSpecifiesDirectoryThatCannotBeRead;
                        throw new RepositoryException(msg.text(indexDirectory, sourceName));
                    }
                    // The directory is usable
                } else {
                    // The location doesn't exist,so try to make it ...
                    if (!indexDir.mkdirs()) {
                        I18n msg = JcrI18n.searchIndexDirectoryOptionSpecifiesDirectoryThatCannotBeCreated;
                        throw new RepositoryException(msg.text(indexDirectory, sourceName));
                    }
                    // We successfully create the dirctory (or directories)
                }
                configuration = LuceneConfigurations.using(indexDir, encoder, encoder);
            } else {
                // Use in-memory as a fall-back ...
                configuration = LuceneConfigurations.inMemory();
            }
            assert configuration != null;

            // Set up the indexing rules ...
            IndexRules indexRules = nodeTypeManager.getRepositorySchemata().getIndexRules();

            // Set up the search engine ...
            org.apache.lucene.analysis.Analyzer analyzer = new SnowballAnalyzer(Version.LUCENE_30, "English");
            boolean verifyWorkspaces = false;
            searchEngine = new LuceneSearchEngine(nameOfSourceToBeSearchable, connectionFactory, verifyWorkspaces,
                                                  maxDepthPerRead, configuration, indexRules, analyzer);

            // Set up an original source observer to keep the index up to date ...
            if (updateIndexesSynchronously) {
                this.service = null;
                this.searchObserver = new Observer() {
                    @SuppressWarnings( "synthetic-access" )
                    public void notify( Changes changes ) {
                        if (changes.getSourceName().equals(sourceName)) {
                            process(changes);
                        }
                    }
                };
            } else {
                // It's asynchronous, so create a single-threaded executor and an observer that enqueues the results
                this.service = Executors.newCachedThreadPool();
                this.searchObserver = new Observer() {
                    @SuppressWarnings( "synthetic-access" )
                    public void notify( final Changes changes ) {
                        if (changes.getSourceName().equals(sourceName)) {
                            service.submit(new Runnable() {
                                public void run() {
                                    process(changes);
                                }
                            });
                        }
                    }
                };
            }
            observable.register(this.searchObserver);

            // Set up the query engine ...
            Planner planner = new CanonicalPlanner();

            // Create a custom optimizer that has our rules first ...
            Optimizer optimizer = new RuleBasedOptimizer() {
                /**
                 * {@inheritDoc}
                 * 
                 * @see org.modeshape.graph.query.optimize.RuleBasedOptimizer#populateRuleStack(java.util.LinkedList,
                 *      org.modeshape.graph.query.plan.PlanHints)
                 */
                @Override
                protected void populateRuleStack( LinkedList<OptimizerRule> ruleStack,
                                                  PlanHints hints ) {
                    super.populateRuleStack(ruleStack, hints);
                    ruleStack.addFirst(RewritePseudoColumns.INSTANCE);
                }
            };

            // Create a custom processor that knows how to submit the access query requests ...
            Processor processor = new QueryProcessor() {

                /**
                 * {@inheritDoc}
                 * 
                 * @see org.modeshape.graph.query.process.QueryProcessor#createAccessComponent(org.modeshape.graph.query.model.QueryCommand,
                 *      org.modeshape.graph.query.QueryContext, org.modeshape.graph.query.plan.PlanNode,
                 *      org.modeshape.graph.query.QueryResults.Columns,
                 *      org.modeshape.graph.query.process.SelectComponent.Analyzer)
                 */
                @Override
                protected ProcessingComponent createAccessComponent( QueryCommand originalQuery,
                                                                     QueryContext context,
                                                                     PlanNode accessNode,
                                                                     Columns resultColumns,
                                                                     Analyzer analyzer ) {
                    return new AccessQueryProcessor((GraphQueryContext)context, resultColumns, accessNode);
                }
            };
            this.queryEngine = new QueryEngine(planner, optimizer, processor);

            // Index any existing content ...
            reindexContent();
        }

        protected void process( Changes changes ) {
            try {
                searchEngine.index(context, changes.getChangeRequests());
            } catch (RuntimeException e) {
                Logger.getLogger(getClass()).error(e, JcrI18n.errorUpdatingQueryIndexes, e.getLocalizedMessage());
            }
        }

        @Override
        public QueryResults query( String workspaceName,
                                   QueryCommand query,
                                   Schemata schemata,
                                   PlanHints hints,
                                   Map<String, Object> variables ) {
            TypeSystem typeSystem = context.getValueFactories().getTypeSystem();
            SearchEngineProcessor processor = searchEngine.createProcessor(context, null, true);
            try {
                QueryContext context = new GraphQueryContext(schemata, typeSystem, hints, new SimpleProblems(), variables,
                                                             processor, workspaceName);
                return queryEngine.execute(context, query);
            } finally {
                processor.close();
            }
        }

        @Override
        public QueryResults search( String workspaceName,
                                    String searchExpression,
                                    int maxRowCount,
                                    int offset ) {
            SearchEngineProcessor processor = searchEngine.createProcessor(context, null, true);
            FullTextSearchRequest request = new FullTextSearchRequest(searchExpression, workspaceName, maxRowCount, offset);
            processor.process(request);
            return new org.modeshape.graph.query.process.QueryResults(request.getResultColumns(), request.getStatistics(),
                                                                      request.getTuples());
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.RepositoryQueryManager#reindexContent()
         */
        @Override
        public void reindexContent() {
            // Get the workspace names ...
            Set<String> workspaces = Graph.create(sourceName, connectionFactory, context).getWorkspaces();

            // Index the existing content (this obtains a connection and possibly locks the source) ...
            SearchEngineIndexer indexer = new SearchEngineIndexer(context, searchEngine, connectionFactory, maxDepthPerRead);
            try {
                for (String workspace : workspaces) {
                    indexer.index(workspace);
                }
            } finally {
                indexer.close();
            }

        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.RepositoryQueryManager#reindexContent(org.modeshape.jcr.JcrWorkspace)
         */
        @Override
        public void reindexContent( JcrWorkspace workspace ) {
            SearchEngineIndexer indexer = new SearchEngineIndexer(context, searchEngine, connectionFactory, maxDepthPerRead);
            try {
                indexer.index(workspace.getName());
            } finally {
                indexer.close();
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.jcr.RepositoryQueryManager#reindexContent(org.modeshape.jcr.JcrWorkspace, java.lang.String, int)
         */
        @Override
        public void reindexContent( JcrWorkspace workspace,
                                    String path,
                                    int depth ) {
            Path at = workspace.context().getValueFactories().getPathFactory().create(path);
            SearchEngineIndexer indexer = new SearchEngineIndexer(context, searchEngine, connectionFactory, maxDepthPerRead);
            try {
                indexer.index(workspace.getName(), at, depth);
            } finally {
                indexer.close();
            }
        }

        protected class GraphQueryContext extends QueryContext {
            private final RequestProcessor processor;
            private final String workspaceName;

            protected GraphQueryContext( Schemata schemata,
                                         TypeSystem typeSystem,
                                         PlanHints hints,
                                         Problems problems,
                                         Map<String, Object> variables,
                                         RequestProcessor processor,
                                         String workspaceName ) {
                super(schemata, typeSystem, hints, problems, variables);
                this.processor = processor;
                this.workspaceName = workspaceName;
            }

            /**
             * @return processor
             */
            public RequestProcessor getProcessor() {
                return processor;
            }

            /**
             * @return workspaceName
             */
            public String getWorkspaceName() {
                return workspaceName;
            }
        }

        protected static class AccessQueryProcessor extends AbstractAccessComponent {
            private final AccessQueryRequest accessRequest;

            protected AccessQueryProcessor( GraphQueryContext context,
                                            Columns columns,
                                            PlanNode accessNode ) {
                super(context, columns, accessNode);
                accessRequest = new AccessQueryRequest(context.getWorkspaceName(), sourceName, getColumns(), andedConstraints,
                                                       limit, context.getSchemata(), context.getVariables());
            }

            /**
             * Get the access query request.
             * 
             * @return the access query request; never null
             */
            public AccessQueryRequest getAccessRequest() {
                return accessRequest;
            }

            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.graph.query.process.ProcessingComponent#execute()
             */
            @Override
            public List<Object[]> execute() {
                GraphQueryContext context = (GraphQueryContext)getContext();
                context.getProcessor().process(accessRequest);
                if (accessRequest.getError() != null) {
                    I18n msg = GraphI18n.errorWhilePerformingQuery;
                    getContext().getProblems().addError(accessRequest.getError(),
                                                        msg,
                                                        accessNode.getString(),
                                                        accessRequest.workspace(),
                                                        sourceName,
                                                        accessRequest.getError().getLocalizedMessage());
                    return emptyTuples();
                }
                return accessRequest.getTuples();
            }

        }

    }
}
