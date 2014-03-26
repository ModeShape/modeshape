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
package org.modeshape.jcr.query.engine;

import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.RepositoryException;
import org.infinispan.commons.util.ReflectionUtil;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.ExtensionLogger;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.RepositoryConfiguration.Component;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.query.CompositeIndexWriter;
import org.modeshape.jcr.query.NodeSequence;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.QueryEngine;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.engine.IndexPlan.StandardIndexPlanner;
import org.modeshape.jcr.query.model.QueryCommand;
import org.modeshape.jcr.query.optimize.AddIndexes;
import org.modeshape.jcr.query.optimize.Optimizer;
import org.modeshape.jcr.query.optimize.OptimizerRule;
import org.modeshape.jcr.query.optimize.RuleBasedOptimizer;
import org.modeshape.jcr.query.plan.PlanHints;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.Planner;
import org.modeshape.jcr.query.validate.Schemata;
import org.modeshape.jcr.spi.query.QueryIndex;
import org.modeshape.jcr.spi.query.QueryIndexPlanner;
import org.modeshape.jcr.spi.query.QueryIndexProvider;
import org.modeshape.jcr.spi.query.QueryIndexWriter;

/**
 * A {@link QueryEngine} implementation that uses available indexes to more quickly produce query results.
 * <p>
 * This query engine is capable of producing results for a query by scanning all nodes in the workspace(s) and filtering out any
 * node that does not satisfy the criteria. This scanning is not very efficient and can result in slow queries, especially when
 * the repository is quite large or when the number of nodes that satisfies the query's criteria is a small fraction of all
 * possible nodes in the workspace(s).
 * </p>
 * <p>
 * This engine can use indexes for certain properties so that it can more quickly identify the nodes that have property values
 * satisfying the constraints of a query. It is possible to index all properties, but this requires significant space and
 * evaluating all constraints using indexes may actually not be the most efficient and fastest approach. This query engine
 * attempts to use a minimum number of indexes that will most quickly produce the set of nodes closest to the actual expected
 * results.
 * </p>
 * <p>
 * Indexes are access from the repository's {@link QueryIndexProvider} instances.
 */
public class IndexQueryEngine extends ScanningQueryEngine {

    /** We don't use the standard logging convention here; we want clients to easily configure logging for the indexes */
    protected static final Logger LOGGER = Logger.getLogger("org.modeshape.jcr.query");
    protected static final boolean TRACE = LOGGER.isTraceEnabled();
    protected static final boolean DEBUG = LOGGER.isDebugEnabled();

    public static class Builder extends ScanningQueryEngine.Builder {

        @Override
        public QueryEngine build() {
            // Instantiate the query index providers ...
            final Map<String, QueryIndexProvider> providersByName = new HashMap<String, QueryIndexProvider>();
            List<Component> components = config().getIndexes().getProviders();
            QueryIndexPlanner indexPlanner = StandardIndexPlanner.INSTANCE;
            for (Component component : components) {
                try {
                    QueryIndexProvider provider = component.createInstance(ScanningQueryEngine.class.getClassLoader());
                    // Set the repository name field ...
                    ReflectionUtil.setValue(provider, "repositoryName", repositoryName());

                    // Set the logger instance
                    ReflectionUtil.setValue(provider, "logger", ExtensionLogger.getLogger(provider.getClass()));

                    // Initialize it ...
                    provider.initialize();

                    // If successful, call the 'postInitialize' method reflectively (due to inability to call directly) ...
                    Method postInitialize = ReflectionUtil.findMethod(QueryIndexProvider.class, "postInitialize");
                    ReflectionUtil.invokeAccessibly(provider, postInitialize, new Object[] {});
                    if (DEBUG) {
                        LOGGER.debug("Successfully initialized index provider '{0}' in repository '{1}'", provider.getName(),
                                     repositoryName());
                    }
                    providersByName.put(provider.getName(), provider);

                    // Collect/combine the index planners ...
                    QueryIndexPlanner providerPlanner = provider.getIndexPlanner();
                    if (providerPlanner == null) {
                        throw new IllegalStateException(JcrI18n.indexProviderMissingPlanner.text(provider.getName(),
                                                                                                 repositoryName()));
                    }
                    indexPlanner = QueryIndexPlanner.both(indexPlanner, providerPlanner);
                } catch (Throwable t) {
                    if (t.getCause() != null) {
                        t = t.getCause();
                    }
                    LOGGER.error(t, JcrI18n.unableToInitializeIndexProvider, component, repositoryName(), t.getMessage());
                }
            }
            if (providersByName.isEmpty()) {
                // There are no indexes, so there's no reason to use this engine ...
                return super.build();
            }

            Optimizer optimizer = optimizer();
            if (optimizer == null) {
                // Create a single indexing rule that will use the index planner from all the providers ...
                final OptimizerRule indexingRule = AddIndexes.with(indexPlanner);
                // Create the optimizer that will add the providers' indexes using the same IndexingRule instance
                optimizer = new RuleBasedOptimizer() {
                    @Override
                    protected void populateIndexingRules( LinkedList<OptimizerRule> ruleStack,
                                                          PlanHints hints ) {
                        super.populateIndexingRules(ruleStack, hints);
                        ruleStack.addLast(indexingRule);
                    }
                };
            }
            return new IndexQueryEngine(context(), repositoryName(), planner(), optimizer, providersByName);
        }

        @Override
        protected Optimizer defaultOptimizer() {
            return null;
        }
    }

    /**
     * Obtain a builder that can be used to create new query engine instances.
     * 
     * @return the builder; never null
     */
    public static Builder builder() {
        return new Builder();
    }

    private final Map<String, QueryIndexProvider> indexProvidersByName;
    private final QueryIndexWriter indexWriter;

    protected IndexQueryEngine( ExecutionContext context,
                                String repositoryName,
                                Planner planner,
                                Optimizer optimizer,
                                Map<String, QueryIndexProvider> indexProvidersByName ) {
        super(context, repositoryName, planner, optimizer);
        this.indexProvidersByName = Collections.unmodifiableMap(indexProvidersByName);

        // And create a single composite writer ...
        if (this.indexProvidersByName.isEmpty()) {
            this.indexWriter = NoOpQueryIndexWriter.INSTANCE;
        } else if (this.indexProvidersByName.size() == 1) {
            this.indexWriter = this.indexProvidersByName.values().iterator().next().getQueryIndexWriter();
        } else {
            this.indexWriter = new CompositeIndexWriter(this.indexProvidersByName.values());
        }
    }

    @Override
    public void shutdown() {
        for (QueryIndexProvider provider : indexProvidersByName.values()) {
            try {
                provider.shutdown();
            } catch (RepositoryException e) {
                LOGGER.error(e, JcrI18n.errorShuttingDownIndexProvider, repositoryName, provider.getName(), e.getMessage());
            }
        }
    }

    @Override
    public QueryContext createQueryContext( ExecutionContext context,
                                            RepositoryCache repositoryCache,
                                            Set<String> workspaceNames,
                                            Map<String, NodeCache> overriddenNodeCachesByWorkspaceName,
                                            Schemata schemata,
                                            NodeTypes nodeTypes,
                                            BufferManager bufferManager,
                                            PlanHints hints,
                                            Map<String, Object> variables ) {
        return new IndexQueryContext(context, repositoryCache, workspaceNames, overriddenNodeCachesByWorkspaceName, schemata,
                                     nodeTypes, bufferManager, hints, null, variables, new HashMap<PlanNode, Columns>(),
                                     indexProvidersByName);
    }

    @Override
    public QueryIndexWriter getQueryIndexWriter() {
        return indexWriter;
    }

    @Override
    protected NodeSequence createNodeSequenceForSource( QueryCommand originalQuery,
                                                        QueryContext context,
                                                        PlanNode sourceNode,
                                                        IndexPlan indexPlan,
                                                        Columns columns,
                                                        QuerySources sources ) {
        NodeSequence sequence = super.createNodeSequenceForSource(originalQuery, context, sourceNode, indexPlan, columns, sources);
        if (sequence != null) return sequence;

        // Look up the index by name ...
        String providerName = indexPlan.getProviderName();
        QueryIndexProvider provider = indexProvidersByName.get(providerName);
        if (provider != null) {
            // Use the index to get a NodeSequence ...
            QueryIndex index = provider.getQueryIndex(indexPlan.getName());
            if (index != null) {
                return sources.fromIndex(index, indexPlan.getConstraints(), indexPlan.getParameters(), 100);
            }
        }

        return null;
    }

    @ThreadSafe
    static class IndexQueryContext extends ScanQueryContext {

        private final Map<String, QueryIndexProvider> providersByName;

        protected IndexQueryContext( ExecutionContext context,
                                     RepositoryCache repositoryCache,
                                     Set<String> workspaceNames,
                                     Map<String, NodeCache> overriddenNodeCachesByWorkspaceName,
                                     Schemata schemata,
                                     NodeTypes nodeTypes,
                                     BufferManager bufferManager,
                                     PlanHints hints,
                                     Problems problems,
                                     Map<String, Object> variables,
                                     Map<PlanNode, Columns> columnsByPlanNode,
                                     Map<String, QueryIndexProvider> indexProvidersByName ) {
            super(context, repositoryCache, workspaceNames, overriddenNodeCachesByWorkspaceName, schemata, nodeTypes,
                  bufferManager, hints, problems, variables, columnsByPlanNode);
            this.providersByName = indexProvidersByName;
        }

        /**
         * Get the query index providers.
         * 
         * @return the providers; never null but possibly empty
         */
        public Collection<QueryIndexProvider> getQueryIndexProviders() {
            return providersByName.values();
        }

        /**
         * Get a specific query index provider by name.
         * 
         * @param name the name of the query index provider; may not be null
         * @return the query index provider, or null if there is no provider with the given name
         */
        public QueryIndexProvider getQueryIndexProvider( String name ) {
            return providersByName.get(name);
        }

        @Override
        public IndexQueryContext with( Map<String, Object> variables ) {
            return new IndexQueryContext(context, repositoryCache, workspaceNames, overriddenNodeCachesByWorkspaceName, schemata,
                                         nodeTypes, bufferManager, hints, problems, variables, columnsByPlanNode, providersByName);
        }

        @Override
        public IndexQueryContext with( PlanHints hints ) {
            return new IndexQueryContext(context, repositoryCache, workspaceNames, overriddenNodeCachesByWorkspaceName, schemata,
                                         nodeTypes, bufferManager, hints, problems, variables, columnsByPlanNode, providersByName);
        }

        @Override
        public IndexQueryContext with( Problems problems ) {
            return new IndexQueryContext(context, repositoryCache, workspaceNames, overriddenNodeCachesByWorkspaceName, schemata,
                                         nodeTypes, bufferManager, hints, problems, variables, columnsByPlanNode, providersByName);
        }

        @Override
        public IndexQueryContext with( Schemata schemata ) {
            return new IndexQueryContext(context, repositoryCache, workspaceNames, overriddenNodeCachesByWorkspaceName, schemata,
                                         nodeTypes, bufferManager, hints, problems, variables, columnsByPlanNode, providersByName);
        }
    }

}
