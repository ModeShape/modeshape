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
package org.jboss.dna.jcr;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import org.jboss.dna.common.collection.Problems;
import org.jboss.dna.common.collection.SimpleProblems;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.text.TextEncoder;
import org.jboss.dna.common.text.UrlEncoder;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.observe.Changes;
import org.jboss.dna.graph.observe.Observable;
import org.jboss.dna.graph.observe.Observer;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.QueryEngine;
import org.jboss.dna.graph.query.QueryResults;
import org.jboss.dna.graph.query.QueryResults.Columns;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.model.TypeSystem;
import org.jboss.dna.graph.query.optimize.Optimizer;
import org.jboss.dna.graph.query.optimize.RuleBasedOptimizer;
import org.jboss.dna.graph.query.plan.CanonicalPlanner;
import org.jboss.dna.graph.query.plan.PlanHints;
import org.jboss.dna.graph.query.plan.PlanNode;
import org.jboss.dna.graph.query.plan.Planner;
import org.jboss.dna.graph.query.process.AbstractAccessComponent;
import org.jboss.dna.graph.query.process.ProcessingComponent;
import org.jboss.dna.graph.query.process.Processor;
import org.jboss.dna.graph.query.process.QueryProcessor;
import org.jboss.dna.graph.query.process.SelectComponent.Analyzer;
import org.jboss.dna.graph.query.validate.Schemata;
import org.jboss.dna.graph.request.AccessQueryRequest;
import org.jboss.dna.graph.request.InvalidWorkspaceException;
import org.jboss.dna.graph.request.processor.RequestProcessor;
import org.jboss.dna.graph.search.SearchEngine;
import org.jboss.dna.graph.search.SearchEngineIndexer;
import org.jboss.dna.graph.search.SearchEngineProcessor;
import org.jboss.dna.search.lucene.IndexRules;
import org.jboss.dna.search.lucene.LuceneConfiguration;
import org.jboss.dna.search.lucene.LuceneConfigurations;
import org.jboss.dna.search.lucene.LuceneSearchEngine;

/**
 * 
 */
class RepositoryQueryManager {

    RepositoryQueryManager() {
    }

    @SuppressWarnings( "unused" )
    public QueryResults query( JcrWorkspace workspace,
                               QueryCommand query,
                               Schemata schemata,
                               PlanHints hints,
                               Map<String, Object> variables ) throws InvalidQueryException {
        Graph.BuildQuery builder = workspace.graph().query(query, schemata);
        if (variables != null) builder.using(variables);
        if (hints != null) builder.using(hints);
        return builder.execute();
    }

    @SuppressWarnings( "unused" )
    public QueryResults search( JcrWorkspace workspace,
                                String searchExpression,
                                int maxRowCount,
                                int offset ) throws InvalidQueryException {
        return workspace.graph().search(searchExpression, maxRowCount, offset);
    }

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

    static class Disabled extends RepositoryQueryManager {

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.jcr.RepositoryQueryManager#query(org.jboss.dna.jcr.JcrWorkspace,
         *      org.jboss.dna.graph.query.model.QueryCommand, org.jboss.dna.graph.query.validate.Schemata,
         *      org.jboss.dna.graph.query.plan.PlanHints, java.util.Map)
         */
        @Override
        public QueryResults query( JcrWorkspace workspace,
                                   QueryCommand query,
                                   Schemata schemata,
                                   PlanHints hints,
                                   Map<String, Object> variables ) throws InvalidQueryException {
            throw new InvalidQueryException(JcrI18n.queryIsDisabledInRepository.text(workspace.getSourceName()));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.jcr.RepositoryQueryManager#search(org.jboss.dna.jcr.JcrWorkspace, java.lang.String, int, int)
         */
        @Override
        public QueryResults search( JcrWorkspace workspace,
                                    String searchExpression,
                                    int maxRowCount,
                                    int offset ) throws InvalidQueryException {
            throw new InvalidQueryException(JcrI18n.queryIsDisabledInRepository.text(workspace.getSourceName()));
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

        SelfContained( ExecutionContext context,
                       String nameOfSourceToBeSearchable,
                       RepositoryConnectionFactory connectionFactory,
                       Observable observable,
                       String indexDirectory,
                       boolean updateIndexesSynchronously ) throws RepositoryException {
            this.context = context;
            this.sourceName = nameOfSourceToBeSearchable;
            this.connectionFactory = connectionFactory;
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
            IndexRules indexRules = null;

            // Set up the search engine ...
            org.apache.lucene.analysis.Analyzer analyzer = null;
            boolean verifyWorkspaces = false;
            searchEngine = new LuceneSearchEngine(nameOfSourceToBeSearchable, connectionFactory, verifyWorkspaces, configuration,
                                                  indexRules, analyzer);

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
            Optimizer optimizer = new RuleBasedOptimizer();
            Processor processor = new QueryProcessor() {

                /**
                 * {@inheritDoc}
                 * 
                 * @see org.jboss.dna.graph.query.process.QueryProcessor#createAccessComponent(org.jboss.dna.graph.query.model.QueryCommand,
                 *      org.jboss.dna.graph.query.QueryContext, org.jboss.dna.graph.query.plan.PlanNode,
                 *      org.jboss.dna.graph.query.QueryResults.Columns,
                 *      org.jboss.dna.graph.query.process.SelectComponent.Analyzer)
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
        public QueryResults query( JcrWorkspace workspace,
                                   QueryCommand query,
                                   Schemata schemata,
                                   PlanHints hints,
                                   Map<String, Object> variables ) {
            TypeSystem typeSystem = workspace.context().getValueFactories().getTypeSystem();
            SearchEngineProcessor processor = searchEngine.createProcessor(context, null, true);
            try {
                QueryContext context = new GraphQueryContext(schemata, typeSystem, hints, new SimpleProblems(), variables,
                                                             processor, workspace.getName());
                return queryEngine.execute(context, query);
            } finally {
                processor.close();
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.jcr.RepositoryQueryManager#reindexContent()
         */
        @Override
        public void reindexContent() {
            // Get the workspace names ...
            Set<String> workspaces = Graph.create(sourceName, connectionFactory, context).getWorkspaces();

            // Index the existing content (this obtains a connection and possibly locks the source) ...
            SearchEngineIndexer indexer = new SearchEngineIndexer(context, searchEngine, connectionFactory);
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
         * @see org.jboss.dna.jcr.RepositoryQueryManager#reindexContent(org.jboss.dna.jcr.JcrWorkspace)
         */
        @Override
        public void reindexContent( JcrWorkspace workspace ) {
            SearchEngineIndexer indexer = new SearchEngineIndexer(context, searchEngine, connectionFactory);
            try {
                indexer.index(workspace.getName());
            } finally {
                indexer.close();
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.jcr.RepositoryQueryManager#reindexContent(org.jboss.dna.jcr.JcrWorkspace, java.lang.String, int)
         */
        @Override
        public void reindexContent( JcrWorkspace workspace,
                                    String path,
                                    int depth ) {
            Path at = workspace.context().getValueFactories().getPathFactory().create(path);
            SearchEngineIndexer indexer = new SearchEngineIndexer(context, searchEngine, connectionFactory);
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
                context.getProcessor().process(accessRequest);
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
             * @see org.jboss.dna.graph.query.process.ProcessingComponent#execute()
             */
            @Override
            public List<Object[]> execute() {
                if (accessRequest.getError() != null) {
                    I18n msg = GraphI18n.errorWhilePerformingQuery;
                    getContext().getProblems().addError(accessRequest.getError(),
                                                        msg,
                                                        accessNode.getString(),
                                                        accessRequest.workspace(),
                                                        sourceName);
                    return emptyTuples();
                }
                return accessRequest.getTuples();
            }

        }

    }
}
