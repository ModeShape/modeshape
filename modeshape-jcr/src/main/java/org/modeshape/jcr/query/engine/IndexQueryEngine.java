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

import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.query.NodeSequence;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.QueryEngine;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.model.QueryCommand;
import org.modeshape.jcr.query.optimize.AddIndexes;
import org.modeshape.jcr.query.optimize.Optimizer;
import org.modeshape.jcr.query.optimize.OptimizerRule;
import org.modeshape.jcr.query.optimize.RuleBasedOptimizer;
import org.modeshape.jcr.query.plan.PlanHints;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.Planner;
import org.modeshape.jcr.spi.index.Index;
import org.modeshape.jcr.spi.index.IndexCostCalculator;
import org.modeshape.jcr.spi.index.IndexManager;
import org.modeshape.jcr.spi.index.provider.IndexPlanner;
import org.modeshape.jcr.spi.index.provider.IndexProvider;

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
 * Indexes are access from the repository's {@link IndexProvider} instances.
 */
public class IndexQueryEngine extends ScanningQueryEngine {

    /** We don't use the standard logging convention here; we want clients to easily configure logging for the indexes */
    protected static final Logger LOGGER = Logger.getLogger("org.modeshape.jcr.query");
    protected static final boolean TRACE = LOGGER.isTraceEnabled();
    protected static final boolean DEBUG = LOGGER.isDebugEnabled();

    public static class Builder extends ScanningQueryEngine.Builder {

        @Override
        public QueryEngine build() {
            // Determine the names of the query index providers ...
            Set<String> providerNames = indexManager().getProviderNames();
            if (providerNames.isEmpty()) {
                // There are no providers and thus we're not using any explicit indexes. There's no reason to use this engine ...
                return super.build();
            }

            // Get the planner for each provider ...
            Map<String, IndexPlanner> plannersByProviderName = new HashMap<>();
            for (String providerName : indexManager().getProviderNames()) {
                IndexProvider provider = indexManager().getProvider(providerName);
                if (provider != null) {
                    IndexPlanner planner = provider.getIndexPlanner();
                    if (planner == null) {
                        throw new IllegalStateException(JcrI18n.indexProviderMissingPlanner.text(providerName, repositoryName()));
                    }
                    plannersByProviderName.put(providerName, planner);
                }
            }

            // Create the optimizer for the query engine ...
            IndexPlanners indexPlanners = IndexPlanners.withProviders(plannersByProviderName);
            Optimizer optimizer = optimizer();
            if (optimizer == null) {
                // Create a single indexing rule that will use the index planner from all the providers ...
                final OptimizerRule indexingRule = AddIndexes.with(indexPlanners);
                // Create the optimizer that will add the providers' indexes using the same IndexingRule instance
                optimizer = new RuleBasedOptimizer() {
                    @Override
                    protected void populateIndexingRules( LinkedList<OptimizerRule> ruleStack,
                                                          PlanHints hints ) {
                        ruleStack.addLast(indexingRule);
                    }
                };
            }
            // Finally create the query engine ...
            return new IndexQueryEngine(context(), repositoryName(), planner(), optimizer, indexManager());
        }

        @Override
        protected final Optimizer defaultOptimizer() {
            return null;
        }
    }

    /**
     * A {@link IndexPlanner} implementation that passes through only those indexes that are owned by the named provider.
     *
     * @author Randall Hauch (rhauch@redhat.com)
     */
    protected static class ProviderIndexPlanner extends IndexPlanner {
        protected final String providerName;
        private final IndexPlanner providerPlanner;

        protected ProviderIndexPlanner( String providerName,
                                        IndexPlanner providerPlanner ) {
            this.providerName = providerName;
            this.providerPlanner = providerPlanner;
        }

        @Override
        public void applyIndexes( QueryContext context,
                                  IndexCostCalculator calculator ) {
            if (!context.getIndexDefinitions().hasIndexDefinitions()) return;
            providerPlanner.applyIndexes(context, calculator);
        }

        @Override
        public String toString() {
            return providerPlanner.toString();
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

    private final IndexManager indexManager;

    protected IndexQueryEngine( ExecutionContext context,
                                String repositoryName,
                                Planner planner,
                                Optimizer optimizer,
                                IndexManager indexManager ) {
        super(context, repositoryName, planner, optimizer);
        this.indexManager = indexManager;
    }

    @Override
    protected NodeSequence createNodeSequenceForSource( QueryCommand originalQuery,
                                                        QueryContext context,
                                                        PlanNode sourceNode,
                                                        IndexPlan indexPlan,
                                                        Columns columns,
                                                        QuerySources sources ) {
        // First let the supertype try to determine a node sequence. This will find the native indexes ...
        NodeSequence sequence = super.createNodeSequenceForSource(originalQuery, context, sourceNode, indexPlan, columns, sources);
        if (sequence != null) return sequence;

        // Look up the index by name ...
        String providerName = indexPlan.getProviderName();
        IndexProvider provider = indexManager.getProvider(providerName);
        if (provider != null) {
            // Use the index to get a NodeSequence ...
            Index index = provider.getIndex(indexPlan.getName(), indexPlan.getWorkspaceName());
            if (index != null) {
                return sources.fromIndex(index, indexPlan.getConstraints(), indexPlan.getJoinConditions(),
                                         context.getVariables(), indexPlan.getParameters(),
                                         context.getExecutionContext().getValueFactories(), provider.batchSize());
            }
        }
        return null;
    }
}
