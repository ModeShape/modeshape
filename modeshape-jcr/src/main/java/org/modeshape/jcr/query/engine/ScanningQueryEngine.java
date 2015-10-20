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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.regex.Pattern;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.qom.BindVariableValue;
import javax.jcr.query.qom.StaticOperand;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.collection.ArrayListMultimap;
import org.modeshape.common.collection.Multimap;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.GraphI18n;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.RepositoryIndexes;
import org.modeshape.jcr.api.query.QueryCancelledException;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.CachedNodeSupplier;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.PropertyTypeUtil;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.query.NodeSequence;
import org.modeshape.jcr.query.NodeSequence.Batch;
import org.modeshape.jcr.query.NodeSequence.RowAccessor;
import org.modeshape.jcr.query.NodeSequence.RowFilter;
import org.modeshape.jcr.query.PseudoColumns;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.QueryEngine;
import org.modeshape.jcr.query.QueryEngineBuilder;
import org.modeshape.jcr.query.QueryResults;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.QueryResults.Statistics;
import org.modeshape.jcr.query.RowExtractors;
import org.modeshape.jcr.query.RowExtractors.ExtractFromRow;
import org.modeshape.jcr.query.engine.process.DependentQuery;
import org.modeshape.jcr.query.engine.process.DistinctSequence;
import org.modeshape.jcr.query.engine.process.ExceptSequence;
import org.modeshape.jcr.query.engine.process.HashJoinSequence;
import org.modeshape.jcr.query.engine.process.IntersectSequence;
import org.modeshape.jcr.query.engine.process.JoinSequence.Range;
import org.modeshape.jcr.query.engine.process.JoinSequence.RangeProducer;
import org.modeshape.jcr.query.engine.process.SortingSequence;
import org.modeshape.jcr.query.model.And;
import org.modeshape.jcr.query.model.ArithmeticOperand;
import org.modeshape.jcr.query.model.Between;
import org.modeshape.jcr.query.model.BindVariableName;
import org.modeshape.jcr.query.model.Cast;
import org.modeshape.jcr.query.model.ChildCount;
import org.modeshape.jcr.query.model.ChildNode;
import org.modeshape.jcr.query.model.ChildNodeJoinCondition;
import org.modeshape.jcr.query.model.Column;
import org.modeshape.jcr.query.model.Comparison;
import org.modeshape.jcr.query.model.Constraint;
import org.modeshape.jcr.query.model.DescendantNode;
import org.modeshape.jcr.query.model.DescendantNodeJoinCondition;
import org.modeshape.jcr.query.model.DynamicOperand;
import org.modeshape.jcr.query.model.EquiJoinCondition;
import org.modeshape.jcr.query.model.FullTextSearch;
import org.modeshape.jcr.query.model.FullTextSearchScore;
import org.modeshape.jcr.query.model.JoinCondition;
import org.modeshape.jcr.query.model.JoinType;
import org.modeshape.jcr.query.model.Length;
import org.modeshape.jcr.query.model.Limit;
import org.modeshape.jcr.query.model.Literal;
import org.modeshape.jcr.query.model.LiteralValue;
import org.modeshape.jcr.query.model.LowerCase;
import org.modeshape.jcr.query.model.NodeDepth;
import org.modeshape.jcr.query.model.NodeId;
import org.modeshape.jcr.query.model.NodeLocalName;
import org.modeshape.jcr.query.model.NodeName;
import org.modeshape.jcr.query.model.NodePath;
import org.modeshape.jcr.query.model.Not;
import org.modeshape.jcr.query.model.NullOrder;
import org.modeshape.jcr.query.model.Or;
import org.modeshape.jcr.query.model.Ordering;
import org.modeshape.jcr.query.model.PropertyExistence;
import org.modeshape.jcr.query.model.PropertyValue;
import org.modeshape.jcr.query.model.QueryCommand;
import org.modeshape.jcr.query.model.ReferenceValue;
import org.modeshape.jcr.query.model.Relike;
import org.modeshape.jcr.query.model.SameNode;
import org.modeshape.jcr.query.model.SameNodeJoinCondition;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.model.SetCriteria;
import org.modeshape.jcr.query.model.SetQuery.Operation;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.query.model.TypeSystem.TypeFactory;
import org.modeshape.jcr.query.model.UpperCase;
import org.modeshape.jcr.query.model.Visitors;
import org.modeshape.jcr.query.optimize.Optimizer;
import org.modeshape.jcr.query.optimize.RuleBasedOptimizer;
import org.modeshape.jcr.query.plan.JoinAlgorithm;
import org.modeshape.jcr.query.plan.PlanHints;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Property;
import org.modeshape.jcr.query.plan.PlanNode.Traversal;
import org.modeshape.jcr.query.plan.PlanNode.Type;
import org.modeshape.jcr.query.plan.Planner;
import org.modeshape.jcr.query.validate.Schemata;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.PropertyType;
import org.modeshape.jcr.value.StringFactory;
import org.modeshape.jcr.value.binary.BinaryStore;

/**
 * A {@link QueryEngine} implementation that always scans all nodes in the workspace(s) and filtering out any node that does not
 * satisfy the criteria. This scanning is not very efficient and can result in slow queries, especially when the repository is
 * quite large or when the number of nodes that satisfies the query's criteria is a small fraction of all possible nodes in the
 * workspace(s).
 * <p>
 * However, this fully-functional QueryEngine implementation is designed to be subclassed when the nodes for a particular source
 * (and optionally criteria) can be found more quickly. In such cases, the subclass should override the
 * {@link #createNodeSequenceForSource(QueryCommand, QueryContext, PlanNode, Columns, QuerySources)} method or the
 * {@link #createNodeSequenceForSource(QueryCommand, QueryContext, PlanNode, IndexPlan, Columns, QuerySources)} method and return
 * a NodeSequence that contains only the applicable nodes.
 * </p>
 */
@Immutable
public class ScanningQueryEngine implements org.modeshape.jcr.query.QueryEngine {

    /** We don't use the standard logging convention here; we want clients to easily configure logging for the indexes */
    protected static final Logger LOGGER = Logger.getLogger("org.modeshape.jcr.query");

    public static class Builder extends QueryEngineBuilder {

        @Override
        public QueryEngine build() {
            return new ScanningQueryEngine(context(), repositoryName(), planner(), optimizer());
        }

        @Override
        protected Optimizer defaultOptimizer() {
            return new RuleBasedOptimizer();
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    protected final String repositoryName;
    protected final Planner planner;
    protected final Optimizer optimizer;

    public ScanningQueryEngine( ExecutionContext context,
                                String repositoryName,
                                Planner planner,
                                Optimizer optimizer ) {
        assert planner != null;
        assert optimizer != null;
        this.repositoryName = repositoryName;
        this.planner = planner;
        this.optimizer = optimizer;
    }

    /**
     * Execute the supplied query by planning, optimizing, and then processing it.
     * 
     * @param queryContext the context in which the query should be executed; same instance as returned by
     *        {@link #createQueryContext}
     * @param query the query that is to be executed
     * @return the query results; never null
     * @throws IllegalArgumentException if the context or query references are null
     * @throws QueryCancelledException if the query was cancelled
     * @throws RepositoryException if there was a problem executing the query
     */
    @Override
    public QueryResults execute( final QueryContext queryContext,
                                 final QueryCommand query ) throws QueryCancelledException, RepositoryException {
        CheckArg.isNotNull(queryContext, "queryContext");
        CheckArg.isNotNull(query, "query");
        final ScanQueryContext context = (ScanQueryContext)queryContext;

        checkCancelled(context);

        // Validate that all of the referenced variables have been provided ...
        Visitors.visitAll(query, new Visitors.AbstractVisitor() {
            @Override
            public void visit( BindVariableName obj ) {
                if (!context.getVariables().keySet().contains(obj.getBindVariableName())) {
                    context.getProblems().addError(GraphI18n.missingVariableValue, obj.getBindVariableName());
                }
            }
        });

        boolean trace = LOGGER.isTraceEnabled();
        if (trace) {
            LOGGER.trace("Beginning to process query {3} against workspace(s) {0} in '{1}' repository: {2}",
                         context.getWorkspaceNames(), repositoryName, query, context.id());
        }

        // Create the canonical plan ...
        long start = System.nanoTime();
        PlanNode plan = planner.createPlan(context, query);
        long duration = Math.abs(System.nanoTime() - start);
        Statistics stats = new Statistics(duration);
        final String workspaceName = context.getWorkspaceNames().iterator().next();

        if (trace) {
            LOGGER.trace("Computed canonical query plan for query {0}: {1}", context.id(), plan);
        }

        checkCancelled(context);
        Columns resultColumns = null;
        if (!context.getProblems().hasErrors()) {
            // Optimize the plan ...
            start = System.nanoTime();
            PlanNode optimizedPlan = optimizer.optimize(context, plan);
            duration = Math.abs(System.nanoTime() - start);
            stats = stats.withOptimizationTime(duration);

            if (trace) {
                LOGGER.trace("Computed optimized query plan for query {0}:\n{1}", context.id(), optimizedPlan);
            }

            // Find the query result columns ...
            start = System.nanoTime();

            // Determine the Columns object for specific nodes in the plan, and store them in the context ...
            optimizedPlan.apply(Traversal.POST_ORDER, new PlanNode.Operation() {

                @Override
                public void apply( PlanNode node ) {
                    Columns columns = null;
                    switch (node.getType()) {
                        case PROJECT:
                        case SOURCE:
                            columns = determineProjectedColumns(node, context);
                            assert columns != null;
                            break;
                        case JOIN:
                            Columns leftColumns = context.columnsFor(node.getFirstChild());
                            Columns rightColumns = context.columnsFor(node.getLastChild());
                            columns = leftColumns.with(rightColumns);
                            assert columns != null;
                            break;
                        case DEPENDENT_QUERY:
                            columns = context.columnsFor(node.getLastChild());
                            assert columns != null;
                            break;
                        case SET_OPERATION:
                            leftColumns = context.columnsFor(node.getFirstChild());
                            rightColumns = context.columnsFor(node.getLastChild());
                            if (checkUnionCompatible(leftColumns, rightColumns, context, query)) {
                                columns = leftColumns;
                                assert columns != null;
                            }
                            break;
                        case INDEX:
                            // do nothing with indexes ...
                            break;
                        default:
                            assert node.getChildCount() == 1;
                            columns = context.columnsFor(node.getFirstChild());
                            assert columns != null;
                            break;
                    }
                    if (columns != null) {
                        context.addColumnsFor(node, columns);
                    }
                }
            });
            Problems problems = context.getProblems();
            if (problems.hasErrors()) {
                throw new RepositoryException(JcrI18n.problemsWithQuery.text(query, problems.toString()));
            } else if (LOGGER.isDebugEnabled() && problems.hasWarnings()) {
                LOGGER.debug("There are several warnings with this query: {0}\n{1}", query, problems.toString());
            }

            resultColumns = context.columnsFor(optimizedPlan);
            assert resultColumns != null;
            duration = Math.abs(System.nanoTime() - start);
            stats = stats.withResultsFormulationTime(duration);

            if (trace) {
                LOGGER.trace("Computed output columns for query {0}: {1}", context.id(), resultColumns);
            }

            if (!context.getProblems().hasErrors()) {
                checkCancelled(context);
                // Execute the plan ...
                try {
                    start = System.nanoTime();
                    if (trace) {
                        LOGGER.trace("Start executing query {0}", context.id());
                    }
                    QueryResults results = executeOptimizedQuery(context, query, stats, optimizedPlan);
                    if (trace) {
                        LOGGER.trace("Stopped executing query {0}: {1}", context.id(), stats);
                    }
                    return results;
                } finally {
                    duration = Math.abs(System.nanoTime() - start);
                    stats = stats.withExecutionTime(duration);
                }
            }
        }
        // Check whether the query was cancelled during execution ...
        checkCancelled(context);

        if (resultColumns == null) resultColumns = ResultColumns.EMPTY;

        // There were problems somewhere ...
        int width = resultColumns.getColumns().size();
        CachedNodeSupplier cachedNodes = context.getNodeCache(workspaceName);
        return new Results(resultColumns, stats, NodeSequence.emptySequence(width), cachedNodes, context.getProblems(), null);
    }

    /**
     * Determine whether this column and the other are <i>union-compatible</i> (that is, having the same columns).
     *
     * @param results1 the first result set; may not be null
     * @param results2 the second result set; may not be null
     * @param context the query execution context; may not be null
     * @param query the query being executed; may not be null
     * @return true if the supplied columns definition are union-compatible, or false if they are not
     */
    protected boolean checkUnionCompatible( Columns results1,
                                            Columns results2,
                                            ScanQueryContext context,
                                            QueryCommand query ) {
        if (results1 == results2) return true;
        if (results1 == null || results2 == null) return false;
        if (results1.hasFullTextSearchScores() != results2.hasFullTextSearchScores()) {
            // The query is not compatible
            context.getProblems().addError(JcrI18n.setQueryContainsResultSetsWithDifferentFullTextSearch);
            return false;
        }
        if (results1.getColumns().size() != results2.getColumns().size()) {
            // The query is not compatible
            context.getProblems().addError(JcrI18n.setQueryContainsResultSetsWithDifferentNumberOfColumns,
                                           results1.getColumns().size(), results2.getColumns().size());
            return false;
        }
        // Go through the columns and make sure that the property names and types match ...
        // (we can't just check column names, since the column names may include the selector if more than one selector)
        int numColumns = results1.getColumns().size();
        boolean noProblems = true;
        for (int i = 0; i != numColumns; ++i) {
            Column thisColumn = results1.getColumns().get(i);
            Column thatColumn = results2.getColumns().get(i);
            if (!thisColumn.getPropertyName().equalsIgnoreCase(thatColumn.getPropertyName())) return false;
            String thisType = results1.getColumnTypeForProperty(thisColumn.getSelectorName(), thisColumn.getPropertyName());
            String thatType = results2.getColumnTypeForProperty(thatColumn.getSelectorName(), thatColumn.getPropertyName());
            if (!thisType.equalsIgnoreCase(thatType)) {
                // The query is not compatible
                context.getProblems().addError(JcrI18n.setQueryContainsResultSetsWithDifferentColumns, thisColumn, thatColumn);
                noProblems = false;
            }
        }
        return noProblems;
    }

    /**
     * Compute the columns that are defined in the supplied {@link PlanNode plan node}. If the supplied plan node is not a
     * {@link Type#PROJECT project node}, the method finds the first PROJECT node below the given node.
     * 
     * @param optimizedPlan the optimized plan node in a query plan; may not be null
     * @param context the query context; may not be null
     * @return the representation of the projected columns; never null
     */
    protected Columns determineProjectedColumns( PlanNode optimizedPlan,
                                                 final ScanQueryContext context ) {
        final PlanHints hints = context.getHints();

        // Look for which columns to include in the results; this will be defined by the highest PROJECT node ...
        PlanNode project = optimizedPlan;
        if (project.getType() != Type.PROJECT) {
            project = optimizedPlan.findAtOrBelow(Traversal.LEVEL_ORDER, Type.PROJECT);
        }
        if (project != null) {
            List<Column> columns = project.getPropertyAsList(Property.PROJECT_COLUMNS, Column.class);
            List<String> columnTypes = project.getPropertyAsList(Property.PROJECT_COLUMN_TYPES, String.class);
            // Determine whether to include the full-text search scores in the results ...
            boolean includeFullTextSearchScores = hints.hasFullTextSearch;
            if (!includeFullTextSearchScores) {
                for (PlanNode select : optimizedPlan.findAllAtOrBelow(Type.SELECT)) {
                    Constraint constraint = select.getProperty(Property.SELECT_CRITERIA, Constraint.class);
                    if (QueryUtil.includeFullTextScores(constraint)) {
                        includeFullTextSearchScores = true;
                        break;
                    }
                }
            }
            // The projected columns may not include all of the selectors from the child of the PROJECT node.
            // So, we need to figure out the selector indexes based upon the ResultColumn for the child ...
            Columns childColumns = context.columnsFor(project.getFirstChild());
            return new ResultColumns(columns, columnTypes, includeFullTextSearchScores, childColumns);
        }
        // Look for a SOURCE ...
        if (optimizedPlan.getType() == Type.SOURCE) {
            PlanNode source = optimizedPlan;
            List<Schemata.Column> schemataColumns = source.getPropertyAsList(Property.SOURCE_COLUMNS, Schemata.Column.class);
            List<Column> columns = new ArrayList<>(schemataColumns.size());
            List<String> columnTypes = new ArrayList<>(schemataColumns.size());
            SelectorName selector = source.getSelectors().iterator().next();
            for (Schemata.Column schemataColumn : schemataColumns) {
                Column column = new Column(selector, schemataColumn.getName(), schemataColumn.getName());
                columns.add(column);
                columnTypes.add(schemataColumn.getPropertyTypeName());
            }
            return new ResultColumns(columns, columnTypes, hints.hasFullTextSearch, null);
        }
        return ResultColumns.EMPTY;
    }

    private void checkCancelled( QueryContext context ) throws QueryCancelledException {
        if (context.isCancelled()) {
            throw new QueryCancelledException();
        }
    }

    @Override
    public void shutdown() {
        // nothing to do
    }

    @Override
    public QueryContext createQueryContext( ExecutionContext context,
                                            RepositoryCache repositoryCache,
                                            Set<String> workspaceNames,
                                            Map<String, NodeCache> overriddenNodeCachesByWorkspaceName,
                                            Schemata schemata,
                                            RepositoryIndexes indexDefns,
                                            NodeTypes nodeTypes,
                                            BufferManager bufferManager,
                                            PlanHints hints,
                                            Map<String, Object> variables ) {
        return new ScanQueryContext(context, repositoryCache, workspaceNames, overriddenNodeCachesByWorkspaceName, schemata,
                                    indexDefns, nodeTypes, bufferManager, hints, null, variables,
                                    new HashMap<PlanNode, Columns>());
    }

    /**
     * Execute the optimized query defined by the supplied {@link PlanNode plan node}.
     * 
     * @param context the context in which the query is to be executed; may not be null
     * @param command the original query; may not be null
     * @param statistics the statistics for the current query execution
     * @param plan the optimized plan for the query; may not be null
     * @return the query results; never null but possibly empty
     */
    protected QueryResults executeOptimizedQuery( final ScanQueryContext context,
                                                  QueryCommand command,
                                                  Statistics statistics,
                                                  PlanNode plan ) {
        long nanos = System.nanoTime();
        Columns columns = null;
        NodeSequence rows = null;
        final String workspaceName = context.getWorkspaceNames().iterator().next();
        try {

            // Find the topmost PROJECT node and build the Columns ...
            PlanNode project = plan.findAtOrBelow(Type.PROJECT);
            assert project != null;
            columns = context.columnsFor(plan);
            assert columns != null;

            boolean trace = LOGGER.isTraceEnabled();
            if (context.getHints().planOnly) {
                if (trace) {
                    LOGGER.trace("Request for only query plan when executing query {0}", context.id());
                }
                rows = NodeSequence.emptySequence(columns.getColumns().size());
            } else {
                boolean includeSystemContent = context.getHints().includeSystemContent;
                final QuerySources sources = new QuerySources(context.getRepositoryCache(), context.getNodeTypes(),
                                                              workspaceName, includeSystemContent);
                rows = createNodeSequence(command, context, plan, columns, sources);
                long nanos2 = System.nanoTime();
                statistics = statistics.withResultsFormulationTime(Math.abs(nanos2 - nanos));
                nanos = nanos2;
                if (rows == null) {
                    // There must have been an error or was cancelled ...
                    assert context.getProblems().hasErrors() || context.isCancelled();
                    rows = NodeSequence.emptySequence(columns.getColumns().size());
                }
                if (trace) {
                    LOGGER.trace("The execution function for {0}: {1}", context.id(), rows);
                }
            }
        } finally {
            statistics = statistics.withExecutionTime(Math.abs(System.nanoTime() - nanos));
        }
        final String planDesc = context.getHints().showPlan ? plan.getString() : null;
        CachedNodeSupplier cachedNodes = context.getNodeCache(workspaceName);
        return new Results(columns, statistics, rows, cachedNodes, context.getProblems(), planDesc);
    }

    /**
     * Create a node sequence containing the results of the original query as defined by the supplied plan.
     * 
     * @param originalQuery the original query command; may not be null
     * @param context the context in which the query is to be executed; may not be null
     * @param plan the optimized plan for the query; may not be null
     * @param columns the result column definition; may not be null
     * @param sources the query sources for the repository; may not be null
     * @return the sequence of results; null only if the type of plan is not understood
     */
    protected NodeSequence createNodeSequence( QueryCommand originalQuery,
                                               ScanQueryContext context,
                                               PlanNode plan,
                                               Columns columns,
                                               QuerySources sources ) {
        NodeSequence rows = null;
        final String workspaceName = sources.getWorkspaceName();
        final NodeCache cache = context.getNodeCache(workspaceName);
        final TypeSystem types = context.getTypeSystem();
        final BufferManager bufferManager = context.getBufferManager();

        switch (plan.getType()) {
            case ACCESS:
                // If the ACCESS node is known to never have results ...
                if (plan.hasProperty(Property.ACCESS_NO_RESULTS)) {
                    rows = NodeSequence.emptySequence(columns.getColumns().size());
                } else {
                    // Create the sequence for the plan node under the the ACCESS node ...
                    assert plan.getChildCount() == 1;
                    rows = createNodeSequence(originalQuery, context, plan.getFirstChild(), columns, sources);
                }
                break;
            case DEPENDENT_QUERY:
                assert plan.getChildCount() == 2;
                // Create the independent query from the left ...
                PlanNode indepPlan = plan.getFirstChild();
                Columns indepColumns = context.columnsFor(indepPlan);
                String variableName = indepPlan.getProperty(Property.VARIABLE_NAME, String.class);
                NodeSequence independent = createNodeSequence(originalQuery, context, indepPlan, indepColumns, sources);

                // Create an extractor to get the value specified in the columns ...
                Column column = indepColumns.getColumns().get(0);
                boolean allowMultiValued = false;
                String typeName = indepColumns.getColumnTypeForProperty(column.getSelectorName(), column.getPropertyName());
                TypeFactory<?> type = context.getTypeSystem().getTypeFactory(typeName);
                ExtractFromRow indepExtractor = createExtractFromRow(column.getSelectorName(), column.getPropertyName(), context,
                                                                     indepColumns, sources, type, allowMultiValued);
                // Create the sequence for the dependent query ...
                PlanNode depPlan = plan.getLastChild();
                Columns depColumns = context.columnsFor(depPlan);
                NodeSequence dependent = createNodeSequence(originalQuery, context, depPlan, depColumns, sources);

                // now create the dependent query ...
                rows = new DependentQuery(independent, indepExtractor, type, dependent, variableName, context.getVariables());
                break;
            case DUP_REMOVE:
                assert plan.getChildCount() == 1;
                if (plan.getFirstChild().getType() == Type.SORT) {
                    // There is a SORT below this DUP_REMOVE, and we can do that in one fell swoop with the sort ...
                    rows = createNodeSequence(originalQuery, context, plan.getFirstChild(), columns, sources);
                } else {
                    // Create the sequence for the plan node under the DUP_REMOVE ...
                    rows = createNodeSequence(originalQuery, context, plan.getFirstChild(), columns, sources);
                    if (!rows.isEmpty() && !(rows instanceof DistinctSequence)) {
                        // Wrap that with a sequence that removes duplicates ...
                        boolean useHeap = false;
                        rows = new DistinctSequence(rows, context.getTypeSystem(), context.getBufferManager(), useHeap);
                    }
                }
                break;
            case GROUP:
                throw new UnsupportedOperationException();
            case JOIN:
                // Create the components under the JOIN ...
                assert plan.getChildCount() == 2;
                PlanNode leftPlan = plan.getFirstChild();
                PlanNode rightPlan = plan.getLastChild();

                // Define the columns for each side, taken from the supplied columns ...
                Columns leftColumns = context.columnsFor(leftPlan);
                Columns rightColumns = context.columnsFor(rightPlan);

                // Query context for the join (must remove isExists condition).
                ScanQueryContext joinQueryContext = context;
                if (context.getHints().isExistsQuery) {
                    // must not push down a LIMIT 1 condition to joins.
                    PlanHints joinPlanHints = context.getHints().clone();
                    joinPlanHints.isExistsQuery = false;
                    joinQueryContext = context.with(joinPlanHints);
                }

                NodeSequence left = createNodeSequence(originalQuery, joinQueryContext, leftPlan, leftColumns, sources);
                NodeSequence right = createNodeSequence(originalQuery, joinQueryContext, rightPlan, rightColumns, sources);

                // Figure out the join algorithm ...
                JoinAlgorithm algorithm = plan.getProperty(Property.JOIN_ALGORITHM, JoinAlgorithm.class);
                JoinType joinType = plan.getProperty(Property.JOIN_TYPE, JoinType.class);
                JoinCondition joinCondition = plan.getProperty(Property.JOIN_CONDITION, JoinCondition.class);
                boolean pack = false;
                boolean useHeap = false;
                if (0 >= right.getRowCount() && right.getRowCount() < 100) useHeap = true;
                ExtractFromRow leftExtractor = null;
                ExtractFromRow rightExtractor = null;
                RangeProducer<?> rangeProducer = null;
                switch (algorithm) {
                    case NESTED_LOOP:
                        // rows = new NestedLoopJoinComponent(context, left, right, joinCondition, joinType);
                        // break;
                    case MERGE:
                        if (joinCondition instanceof SameNodeJoinCondition) {
                            SameNodeJoinCondition condition = (SameNodeJoinCondition)joinCondition;
                            // check if the JOIN was not reversed by an optimization
                            boolean joinReversed = !leftColumns.getSelectorNames().contains(condition.getSelector1Name());
                            int leftIndex;
                            int rightIndex;
                            if (joinReversed) {
                                // figure out the row indexes for the different selectors ...
                                leftIndex = leftColumns.getSelectorIndex(condition.getSelector2Name());
                                rightIndex = rightColumns.getSelectorIndex(condition.getSelector1Name());
                            } else {
                                leftIndex = leftColumns.getSelectorIndex(condition.getSelector1Name());
                                rightIndex = rightColumns.getSelectorIndex(condition.getSelector2Name());
                            }
                            String relativePath = condition.getSelector2Path();
                            if (relativePath != null) {
                                // Get extractors that will get the path of the nodes ...
                                PathFactory pathFactory = context.getExecutionContext().getValueFactories().getPathFactory();
                                Path relPath = pathFactory.create(relativePath);
                                if (joinReversed) {
                                    leftExtractor = RowExtractors.extractRelativePath(leftIndex, relPath, cache, types);
                                    rightExtractor = RowExtractors.extractPath(rightIndex, cache, types);
                                } else {
                                    leftExtractor = RowExtractors.extractPath(leftIndex, cache, types);
                                    rightExtractor = RowExtractors.extractRelativePath(rightIndex, relPath, cache, types);
                                }
                            } else {
                                // The nodes must be the same node ...
                                leftExtractor = RowExtractors.extractNodeKey(leftIndex, cache, types);
                                rightExtractor = RowExtractors.extractNodeKey(rightIndex, cache, types);
                            }
                        } else if (joinCondition instanceof ChildNodeJoinCondition) {
                            ChildNodeJoinCondition condition = (ChildNodeJoinCondition)joinCondition;

                            // check if the JOIN was not reversed by an optimization
                            boolean joinReversed = !leftColumns.getSelectorNames().contains(condition.getParentSelectorName());

                            if (joinReversed) {
                                int leftIndex = leftColumns.getSelectorIndex(condition.getChildSelectorName());
                                int rightIndex = rightColumns.getSelectorIndex(condition.getParentSelectorName());

                                leftExtractor = RowExtractors.extractParentNodeKey(leftIndex, cache, types);
                                rightExtractor = RowExtractors.extractNodeKey(rightIndex, cache, types);
                            } else {
                                int leftIndex = leftColumns.getSelectorIndex(condition.getParentSelectorName());
                                int rightIndex = rightColumns.getSelectorIndex(condition.getChildSelectorName());

                                leftExtractor = RowExtractors.extractNodeKey(leftIndex, cache, types);
                                rightExtractor = RowExtractors.extractParentNodeKey(rightIndex, cache, types);
                            }
                        } else if (joinCondition instanceof EquiJoinCondition) {
                            EquiJoinCondition condition = (EquiJoinCondition)joinCondition;
                            // check if the JOIN was not reversed by an optimization
                            boolean joinReversed = !leftColumns.getSelectorNames().contains(condition.getSelector1Name());

                            String sel1 = condition.getSelector1Name();
                            String sel2 = condition.getSelector2Name();
                            String prop1 = condition.getProperty1Name();
                            String prop2 = condition.getProperty2Name();
                            if (joinReversed) {
                                leftExtractor = createExtractFromRow(sel2, prop2, joinQueryContext, leftColumns, sources, null,
                                                                     true);
                                rightExtractor = createExtractFromRow(sel1, prop1, joinQueryContext, rightColumns, sources, null,
                                                                      true);
                            } else {
                                leftExtractor = createExtractFromRow(sel1, prop1, joinQueryContext, leftColumns, sources, null,
                                                                     true);
                                rightExtractor = createExtractFromRow(sel2, prop2, joinQueryContext, rightColumns, sources, null,
                                                                      true);
                            }

                        } else if (joinCondition instanceof DescendantNodeJoinCondition) {
                            DescendantNodeJoinCondition condition = (DescendantNodeJoinCondition)joinCondition;
                            // For this to work, we want the ancestors to be on the left, so that the descendants can quickly
                            // be found given a path of each ancestor ...
                            assert leftColumns.getSelectorNames().contains(condition.getAncestorSelectorName());
                            String ancestorSelector = condition.getAncestorSelectorName();
                            String descendantSelector = condition.getDescendantSelectorName();
                            int ancestorSelectorIndex = leftColumns.getSelectorIndex(ancestorSelector);
                            int descendantSelectorIndex = rightColumns.getSelectorIndex(descendantSelector);
                            leftExtractor = RowExtractors.extractPath(ancestorSelectorIndex, cache, types);
                            rightExtractor = RowExtractors.extractPath(descendantSelectorIndex, cache, types);
                            // This is the only time we need a RangeProducer ...
                            final PathFactory paths = context.getExecutionContext().getValueFactories().getPathFactory();
                            rangeProducer = new RangeProducer<Path>() {
                                @Override
                                public Range<Path> getRange( Path leftPath ) {
                                    if (leftPath.isRoot()) {
                                        // All paths are descendants of the root
                                        return new Range<>(leftPath, false, null, true);
                                    }
                                    // Given the path of the node on the left side of the join, find the range of all paths
                                    // that might be considered descendants of the left path....
                                    boolean includeLower = false; // we don't want to include the left node; only descendants
                                    // The upper bound path is the same as the left path, just with an incremented SNS ...
                                    Path.Segment lastSegment = leftPath.getLastSegment();
                                    Path.Segment upperSegment = paths.createSegment(lastSegment.getName(),
                                                                                    lastSegment.getIndex() + 1);
                                    Path upperBoundPath = paths.create(leftPath.getParent(), upperSegment);
                                    return new Range<>(leftPath, includeLower, upperBoundPath, false);
                                }
                            };
                        } else {
                            assert false : "Unable to use merge algorithm with join conditions: " + joinCondition;
                            throw new UnsupportedOperationException();
                        }
                        break;
                }

                // Perform conversion if required ...
                assert leftExtractor != null;
                assert rightExtractor != null;
                TypeFactory<?> leftType = leftExtractor.getType();
                TypeFactory<?> rightType = rightExtractor.getType();
                if (!leftType.equals(rightType)) {
                    // wrap the right extractor with a converting extractor ...
                    final TypeFactory<?> commonType = context.getTypeSystem().getCompatibleType(leftType, rightType);
                    if (!leftType.equals(commonType)) leftExtractor = RowExtractors.convert(leftExtractor, commonType);
                    if (!rightType.equals(commonType)) rightExtractor = RowExtractors.convert(rightExtractor, commonType);
                }

                rows = new HashJoinSequence(workspaceName, left, right, leftExtractor, rightExtractor, joinType,
                                            context.getBufferManager(), cache, rangeProducer, pack, useHeap);
                // For each Constraint object applied to the JOIN, simply create a SelectComponent on top ...
                RowFilter filter = null;
                List<Constraint> constraints = plan.getPropertyAsList(Property.JOIN_CONSTRAINTS, Constraint.class);
                if (constraints != null) {
                    for (Constraint constraint : constraints) {
                        RowFilter constraintFilter = createRowFilter(constraint, context, columns, sources);
                        filter = NodeSequence.requireBoth(filter, constraintFilter);
                    }
                }
                rows = NodeSequence.filter(rows, filter); // even if filter is null
                break;
            case LIMIT:
                // Create the sequence for the plan node under the LIMIT ...
                assert plan.getChildCount() == 1;
                rows = createNodeSequence(originalQuery, context, plan.getFirstChild(), columns, sources);
                // Calculate the limit ...
                Integer rowLimit = plan.getProperty(Property.LIMIT_COUNT, Integer.class);
                Integer offset = plan.getProperty(Property.LIMIT_OFFSET, Integer.class);
                Limit limit = Limit.NONE;
                if (rowLimit != null) limit = limit.withRowLimit(rowLimit.intValue());
                if (offset != null) limit = limit.withOffset(offset.intValue());
                // Then create the limited sequence ...
                if (!limit.isUnlimited()) {
                    rows = NodeSequence.limit(rows, limit);
                }
                break;
            case NULL:
                // No results ...
                rows = NodeSequence.emptySequence(columns.getColumns().size());
                break;
            case PROJECT:
                // Nothing to do, since the projected columns will be accessed as needed when the results are processed. Instead,
                // just process the PROJECT node's only child ...
                PlanNode child = plan.getFirstChild();
                columns = context.columnsFor(child);
                rows = createNodeSequence(originalQuery, context, child, columns, sources);
                break;
            case SELECT:
                // Create the sequence for the plan node under the SELECT ...
                assert plan.getChildCount() == 1;
                rows = createNodeSequence(originalQuery, context, plan.getFirstChild(), columns, sources);
                Constraint constraint = plan.getProperty(Property.SELECT_CRITERIA, Constraint.class);
                filter = createRowFilter(constraint, context, columns, sources);
                rows = NodeSequence.filter(rows, filter);
                break;
            case SET_OPERATION:
                Operation operation = plan.getProperty(Property.SET_OPERATION, Operation.class);
                boolean all = plan.getProperty(Property.SET_USE_ALL, Boolean.class);
                PlanNode firstPlan = plan.getFirstChild();
                PlanNode secondPlan = plan.getLastChild();
                Columns firstColumns = context.columnsFor(firstPlan);
                Columns secondColumns = context.columnsFor(secondPlan);
                NodeSequence first = createNodeSequence(originalQuery, context, firstPlan, firstColumns, sources);
                NodeSequence second = createNodeSequence(originalQuery, context, secondPlan, secondColumns, sources);
                useHeap = 0 >= second.getRowCount() && second.getRowCount() < 100;
                if (first.width() != second.width()) {
                    // A set operation requires that the 'first' and 'second' sequences have the same width, but this is
                    // not necessarily the case (e.g., when one side involves a JOIN but the other does not). The columns
                    // will dictate which subset of selector indexes in the sequences should be used.
                    first = NodeSequence.slice(first, firstColumns);
                    second = NodeSequence.slice(second, secondColumns);
                    assert first.width() == second.width();
                }
                pack = false;
                switch (operation) {
                    case UNION: {
                        // If one of them is empty, return the other ...
                        if (first.isEmpty()) return second;
                        if (second.isEmpty()) return first;
                        // This is really just a sequence with the two parts ...
                        rows = NodeSequence.append(first, second);
                        break;
                    }
                    case INTERSECT: {
                        // If one of them is empty, there are no results ...
                        if (first.isEmpty()) return first;
                        if (second.isEmpty()) return second;
                        rows = new IntersectSequence(workspaceName, first, second, types, bufferManager, cache, pack, useHeap);
                        break;
                    }
                    case EXCEPT: {
                        // If the second is empty, there's nothing to exclude ...
                        if (second.isEmpty()) return first;
                        rows = new ExceptSequence(workspaceName, first, second, types, bufferManager, cache, pack, useHeap);
                        break;
                    }
                }
                if (!all) {
                    useHeap = false;
                    rows = new DistinctSequence(rows, context.getTypeSystem(), context.getBufferManager(), useHeap);
                }
                break;
            case SORT:
                assert plan.getChildCount() == 1;
                PlanNode delegate = plan.getFirstChild();
                boolean allowDuplicates = true;
                if (delegate.getType() == Type.DUP_REMOVE) {
                    // This SORT already removes duplicates, so we can skip the first child ...
                    delegate = delegate.getFirstChild();
                    allowDuplicates = false;
                }
                PlanNode parent = plan.getParent();
                if (parent != null && parent.getType() == Type.DUP_REMOVE) {
                    // The parent is a DUP_REMOVE (shouldn't really happen in an optimized plan), we should disallow duplicates
                    // ...
                    allowDuplicates = false;
                }
                // Create the sequence for the delegate plan node ...
                rows = createNodeSequence(originalQuery, context, delegate, columns, sources);
                if (!rows.isEmpty()) {
                    // Prepare to wrap this delegate sequence based upon the SORT_ORDER_BY ...
                    List<Object> orderBys = plan.getPropertyAsList(Property.SORT_ORDER_BY, Object.class);
                    if (!orderBys.isEmpty()) {
                        // Create an extractor from the orderings that we'll use for the sorting ...
                        ExtractFromRow sortExtractor = null;
                        pack = false;
                        useHeap = false;
                        NullOrder nullOrder = null;
                        if (orderBys.get(0) instanceof Ordering) {
                            List<Ordering> orderings = new ArrayList<Ordering>(orderBys.size());
                            for (Object orderBy : orderBys) {
                                orderings.add((Ordering)orderBy);
                            }
                            // Determine the alias-to-name mappings for the selectors in the orderings ...
                            Map<SelectorName, SelectorName> sourceNamesByAlias = new HashMap<SelectorName, SelectorName>();
                            for (PlanNode source : plan.findAllAtOrBelow(Type.SOURCE)) {
                                SelectorName name = source.getProperty(Property.SOURCE_NAME, SelectorName.class);
                                SelectorName alias = source.getProperty(Property.SOURCE_ALIAS, SelectorName.class);
                                if (alias != null) sourceNamesByAlias.put(alias, name);
                            }
                            // If there are multiple orderings, then we'll never have nulls. But if there is just one ordering,
                            // we have to handle nulls ...
                            if (orderings.size() == 1) {
                                nullOrder = orderings.get(0).nullOrder();
                            }
                            // Now create the single sorting extractor ...
                            sortExtractor = createSortingExtractor(orderings, sourceNamesByAlias, context, columns, sources);
                        } else {
                            // Order by the location(s) because it's before a merge-join ...
                            final TypeFactory<?> keyType = context.getTypeSystem().getReferenceFactory();
                            List<ExtractFromRow> extractors = new ArrayList<>();
                            for (Object ordering : orderBys) {
                                SelectorName selectorName = (SelectorName)ordering;
                                final int index = columns.getSelectorIndex(selectorName.name());
                                extractors.add(new ExtractFromRow() {
                                    @Override
                                    public TypeFactory<?> getType() {
                                        return keyType;
                                    }

                                    @Override
                                    public Object getValueInRow( RowAccessor row ) {
                                        CachedNode node = row.getNode(index);
                                        return node != null ? node.getKey() : null;
                                    }
                                });
                            }
                            // This is jsut for a merge join, so use standard null ordering ...
                            nullOrder = NullOrder.NULLS_LAST;
                            // Now create the single sorting extractor ...
                            sortExtractor = RowExtractors.extractorWith(extractors);
                        }

                        // Now create the sorting sequence ...
                        if (sortExtractor != null) {
                            rows = new SortingSequence(workspaceName, rows, sortExtractor, bufferManager, cache, pack, useHeap,
                                                       allowDuplicates, nullOrder);
                        }
                    }
                }
                break;
            case SOURCE:
                // Otherwise, just grab all of the nodes ...
                rows = createNodeSequenceForSource(originalQuery, context, plan, columns, sources);
                break;
            default:
                break;
        }
        return rows;
    }

    /**
     * Create a node sequence for the given source.
     * 
     * @param originalQuery the original query command; may not be null
     * @param context the context in which the query is to be executed; may not be null
     * @param sourceNode the {@link Type#SOURCE} plan node for one part of a query; may not be null
     * @param columns the result column definition; may not be null
     * @param sources the query sources for the repository; may not be null
     * @return the sequence of results; null only if the type of plan is not understood
     */
    protected NodeSequence createNodeSequenceForSource( QueryCommand originalQuery,
                                                        QueryContext context,
                                                        PlanNode sourceNode,
                                                        Columns columns,
                                                        QuerySources sources ) {
        // The indexes should already be in the correct order, from lowest cost to highest cost ...
        for (PlanNode indexNode : sourceNode.getChildren()) {
            if (indexNode.getType() != Type.INDEX) continue;
            IndexPlan index = indexNode.getProperty(Property.INDEX_SPECIFICATION, IndexPlan.class);
            NodeSequence sequence = createNodeSequenceForSource(originalQuery, context, sourceNode, index, columns, sources);
            if (sequence != null) {
                // Mark the index as being used ...
                indexNode.setProperty(Property.INDEX_USED, Boolean.TRUE);
                return sequence;
            }
            // Otherwise, keep looking for an index ...
            LOGGER.debug("Skipping disabled index '{0}' from provider '{1}' in workspace(s) {2} for query: {3}", index.getName(),
                         index.getProviderName(), context.getWorkspaceNames(), originalQuery);
        }

        // Grab all of the nodes ...
        return sources.allNodes(1.0f, -1);
    }

    /**
     * Create a node sequence for the given index
     * 
     * @param originalQuery the original query command; may not be null
     * @param context the context in which the query is to be executed; may not be null
     * @param sourceNode the {@link Type#SOURCE} plan node for one part of a query; may not be null
     * @param index the {@link IndexPlan} specification; may not be null
     * @param columns the result column definition; may not be null
     * @param sources the query sources for the repository; may not be null
     * @return the sequence of results; null only if the type of index is not understood
     */
    protected NodeSequence createNodeSequenceForSource( QueryCommand originalQuery,
                                                        QueryContext context,
                                                        PlanNode sourceNode,
                                                        IndexPlan index,
                                                        Columns columns,
                                                        QuerySources sources ) {
        if (index.getProviderName() == null) {
            String name = index.getName();
            String pathStr = (String)index.getParameters().get(IndexPlanners.PATH_PARAMETER);
            if (pathStr != null) {
                if (IndexPlanners.NODE_BY_PATH_INDEX_NAME.equals(name)) {
                    PathFactory paths = context.getExecutionContext().getValueFactories().getPathFactory();
                    Path path = paths.create(pathStr);
                    return sources.singleNode(path, 1.0f);
                }
                if (IndexPlanners.CHILDREN_BY_PATH_INDEX_NAME.equals(name)) {
                    PathFactory paths = context.getExecutionContext().getValueFactories().getPathFactory();
                    Path path = paths.create(pathStr);
                    return sources.childNodes(path, 1.0f);
                }
                if (IndexPlanners.DESCENDANTS_BY_PATH_INDEX_NAME.equals(name)) {
                    PathFactory paths = context.getExecutionContext().getValueFactories().getPathFactory();
                    Path path = paths.create(pathStr);
                    return sources.descendantNodes(path, 1.0f);
                }
            }
            String idStr = (String)index.getParameters().get(IndexPlanners.ID_PARAMETER);
            if (idStr != null) {
                if (IndexPlanners.NODE_BY_ID_INDEX_NAME.equals(name)) {
                    StringFactory string = context.getExecutionContext().getValueFactories().getStringFactory();
                    String id = string.create(idStr);
                    final String workspaceName = context.getWorkspaceNames().iterator().next();
                    return sources.singleNode(workspaceName, id, 1.0f);
                }
            }
        }
        return null;
    }

    /**
     * Create an {@link ExtractFromRow} instance that produces for given row a single object that can be used to sort all rows in
     * the specified order.
     * 
     * @param orderings the specification of the sort order; may not be null or empty
     * @param sourceNamesByAlias the map of selector names keyed by their aliases; may not be null but may be empty
     * @param context the context in which the query is to be executed; may not be null
     * @param columns the result column definition; may not be null
     * @param sources the query sources for the repository; may not be null
     * @return the extractor; never null
     */
    protected ExtractFromRow createSortingExtractor( List<Ordering> orderings,
                                                     Map<SelectorName, SelectorName> sourceNamesByAlias,
                                                     QueryContext context,
                                                     Columns columns,
                                                     QuerySources sources ) {
        if (orderings.size() == 1) {
            return createSortingExtractor(orderings.get(0), sourceNamesByAlias, context, columns, sources);
        }
        if (orderings.size() == 2) {
            ExtractFromRow first = createSortingExtractor(orderings.get(0), sourceNamesByAlias, context, columns, sources);
            ExtractFromRow second = createSortingExtractor(orderings.get(1), sourceNamesByAlias, context, columns, sources);
            return RowExtractors.extractorWith(first, second);
        }
        if (orderings.size() == 3) {
            ExtractFromRow first = createSortingExtractor(orderings.get(0), sourceNamesByAlias, context, columns, sources);
            ExtractFromRow second = createSortingExtractor(orderings.get(1), sourceNamesByAlias, context, columns, sources);
            ExtractFromRow third = createSortingExtractor(orderings.get(2), sourceNamesByAlias, context, columns, sources);
            return RowExtractors.extractorWith(first, second, third);
        }
        if (orderings.size() == 4) {
            ExtractFromRow first = createSortingExtractor(orderings.get(0), sourceNamesByAlias, context, columns, sources);
            ExtractFromRow second = createSortingExtractor(orderings.get(1), sourceNamesByAlias, context, columns, sources);
            ExtractFromRow third = createSortingExtractor(orderings.get(2), sourceNamesByAlias, context, columns, sources);
            ExtractFromRow fourth = createSortingExtractor(orderings.get(3), sourceNamesByAlias, context, columns, sources);
            return RowExtractors.extractorWith(first, second, third, fourth);
        }
        List<ExtractFromRow> extractors = new ArrayList<>(orderings.size());
        for (Ordering ordering : orderings) {
            extractors.add(createSortingExtractor(ordering, sourceNamesByAlias, context, columns, sources));
        }
        return RowExtractors.extractorWith(extractors);
    }

    /**
     * Create an {@link ExtractFromRow} instance that produces for given row a single object that can be used to sort all rows in
     * the specified order.
     * 
     * @param ordering the specification of the sort order; may not be null or empty
     * @param sourceNamesByAlias the map of selector names keyed by their aliases; may not be null but may be empty
     * @param context the context in which the query is to be executed; may not be null
     * @param columns the result column definition; may not be null
     * @param sources the query sources for the repository; may not be null
     * @return the extractor; never null
     */
    protected ExtractFromRow createSortingExtractor( Ordering ordering,
                                                     Map<SelectorName, SelectorName> sourceNamesByAlias,
                                                     QueryContext context,
                                                     Columns columns,
                                                     QuerySources sources ) {
        DynamicOperand operand = ordering.getOperand();
        TypeFactory<?> defaultType = context.getTypeSystem().getStringFactory();// only when ordered column is residual or not
                                                                                // defined
        ExtractFromRow extractor = createExtractFromRow(operand, context, columns, sources, defaultType, false, false);
        return RowExtractors.extractorWith(extractor, ordering.order(), ordering.nullOrder());
    }

    /**
     * Create a {@link RowFilter} implementation given the supplied constraints. The resulting filter can be applied to a
     * NodeSequence by using {@link NodeSequence#filter(NodeSequence, RowFilter)}.
     * 
     * @param constraint the constraints to be applied by the filter; may not be null;
     * @param context the context in which the query is to be executed; may not be null
     * @param columns the result column definition; may not be null
     * @param sources the query sources for the repository; may not be null
     * @return the row filter that implements the constraints; never null
     */
    protected RowFilter createRowFilter( final Constraint constraint,
                                         final QueryContext context,
                                         Columns columns,
                                         QuerySources sources ) {
        assert constraint != null;
        assert context != null;
        assert columns != null;
        assert sources != null;
        if (constraint instanceof Or) {
            Or orConstraint = (Or)constraint;
            final RowFilter left = createRowFilter(orConstraint.left(), context, columns, sources);
            final RowFilter right = createRowFilter(orConstraint.right(), context, columns, sources);
            return new RowFilter() {
                @Override
                public boolean isCurrentRowValid( Batch batch ) {
                    return left.isCurrentRowValid(batch) || right.isCurrentRowValid(batch);
                }

                @Override
                public String toString() {
                    return "(or " + left + "," + right + " )";
                }
            };
        }
        if (constraint instanceof Not) {
            Not notConstraint = (Not)constraint;
            final RowFilter not = createRowFilter(notConstraint.getConstraint(), context, columns, sources);
            return new RowFilter() {
                @Override
                public boolean isCurrentRowValid( Batch batch ) {
                    return !not.isCurrentRowValid(batch);
                }

                @Override
                public String toString() {
                    return "(not " + not + " )";
                }
            };
        }
        if (constraint instanceof And) {
            And andConstraint = (And)constraint;
            final RowFilter left = createRowFilter(andConstraint.left(), context, columns, sources);
            final RowFilter right = createRowFilter(andConstraint.right(), context, columns, sources);
            return new RowFilter() {
                @Override
                public boolean isCurrentRowValid( Batch batch ) {
                    return left.isCurrentRowValid(batch) && right.isCurrentRowValid(batch);
                }

                @Override
                public String toString() {
                    return "(and " + left + "," + right + " )";
                }
            };
        }
        if (constraint instanceof ChildNode) {
            ChildNode childConstraint = (ChildNode)constraint;
            PathFactory paths = context.getExecutionContext().getValueFactories().getPathFactory();
            final Path parentPath = paths.create(childConstraint.getParentPath());
            final NodeCache cache = context.getNodeCache(sources.getWorkspaceName());
            final CachedNode parent = sources.getNodeAtPath(parentPath, cache);
            if (parent == null) {
                return NodeSequence.NO_PASS_ROW_FILTER;
            }
            final NodeKey parentKey = parent.getKey();
            final boolean isParentRoot = parentPath.isRoot();
            final String selectorName = childConstraint.getSelectorName();
            final int index = columns.getSelectorIndex(selectorName);
            return new RowFilter() {
                @Override
                public boolean isCurrentRowValid( Batch batch ) {
                    CachedNode node = batch.getNode(index);
                    if (node == null) return false;
                    if (isParentRoot) return true;
                    if (parentKey.equals(node.getParentKey(cache))) return true;
                    // Don't have to check the additional parents since we only find shared nodes in the original location ...
                    return false;
                }

                @Override
                public String toString() {
                    return "(filter " + Visitors.readable(constraint) + ")";
                }
            };
        }
        if (constraint instanceof DescendantNode) {
            DescendantNode descendantNode = (DescendantNode)constraint;
            PathFactory paths = context.getExecutionContext().getValueFactories().getPathFactory();
            final Path ancestorPath = paths.create(descendantNode.getAncestorPath());
            final NodeCache cache = context.getNodeCache(sources.getWorkspaceName());
            final CachedNode ancestor = sources.getNodeAtPath(ancestorPath, cache);
            if (ancestor == null) {
                return NodeSequence.NO_PASS_ROW_FILTER;
            }
            final NodeKey ancestorKey = ancestor.getKey();
            final String selectorName = descendantNode.getSelectorName();
            final int index = columns.getSelectorIndex(selectorName);
            return new RowFilter() {
                @Override
                public boolean isCurrentRowValid( Batch batch ) {
                    CachedNode node = batch.getNode(index);
                    while (node != null) {
                        NodeKey parentKey = node.getParentKey(cache);
                        if (parentKey == null) return false;
                        if (ancestorKey.equals(parentKey)) return true;
                        // Don't have to check the additional parents since we only find shared nodes in the original location ...
                        node = cache.getNode(parentKey);
                    }
                    return false;
                }

                @Override
                public String toString() {
                    return "(filter " + Visitors.readable(constraint) + ")";
                }
            };
        }
        if (constraint instanceof SameNode) {
            SameNode sameNode = (SameNode)constraint;
            PathFactory paths = context.getExecutionContext().getValueFactories().getPathFactory();
            final Path path = paths.create(sameNode.getPath());
            final NodeCache cache = context.getNodeCache(sources.getWorkspaceName());
            final CachedNode node = sources.getNodeAtPath(path, cache);
            if (node == null) {
                return NodeSequence.NO_PASS_ROW_FILTER;
            }
            final NodeKey nodeKey = node.getKey();
            final String selectorName = sameNode.getSelectorName();
            final int index = columns.getSelectorIndex(selectorName);
            return new RowFilter() {
                @Override
                public boolean isCurrentRowValid( Batch batch ) {
                    CachedNode node = batch.getNode(index);
                    return node != null && nodeKey.equals(node.getKey());
                }

                @Override
                public String toString() {
                    return "(filter " + Visitors.readable(constraint) + ")";
                }
            };
        }
        if (constraint instanceof PropertyExistence) {
            PropertyExistence propertyExistance = (PropertyExistence)constraint;
            NameFactory names = context.getExecutionContext().getValueFactories().getNameFactory();
            final Name propertyName = names.create(propertyExistance.getPropertyName());
            final String selectorName = propertyExistance.selectorName().name();
            final int index = columns.getSelectorIndex(selectorName);
            final NodeCache cache = context.getNodeCache(sources.getWorkspaceName());
            assert index >= 0;
            return new RowFilter() {
                @Override
                public boolean isCurrentRowValid( Batch batch ) {
                    CachedNode node = batch.getNode(index);
                    return node != null && node.hasProperty(propertyName, cache);
                }

                @Override
                public String toString() {
                    return "(filter " + Visitors.readable(constraint) + ")";
                }
            };
        }
        if (constraint instanceof Between) {
            Between between = (Between)constraint;
            final StaticOperand lower = between.getLowerBound();
            final StaticOperand upper = between.getUpperBound();
            final boolean includeLower = between.isLowerBoundIncluded();
            final boolean includeUpper = between.isUpperBoundIncluded();
            DynamicOperand dynamicOperand = between.getOperand();
            final TypeFactory<?> defaultType = determineType(dynamicOperand, context, columns);
            final ExtractFromRow operation = createExtractFromRow(dynamicOperand, context, columns, sources, defaultType, true,
                                                                  false);

            // Determine the literal value in the static operand ...
            return new RowFilterSupplier() {
                @Override
                protected RowFilter createFilter() {
                    // Evaluate the operand, which may have variables ...
                    final Object lowerLiteralValue = literalValue(lower, context, defaultType);
                    final Object upperLiteralValue = literalValue(upper, context, defaultType);
                    // Create the correct operation ...
                    final TypeFactory<?> expectedType = operation.getType();
                    final Object lowerValue = expectedType.create(lowerLiteralValue);
                    final Object upperValue = expectedType.create(upperLiteralValue);
                    @SuppressWarnings( "unchecked" )
                    final Comparator<Object> comparator = (Comparator<Object>)expectedType.getComparator();
                    if (includeLower) {
                        if (includeUpper) {
                            return new DynamicOperandFilter(operation) {
                                @Override
                                protected boolean evaluate( Object leftHandValue ) {
                                    if (leftHandValue == null) return false; // null values never match
                                    return comparator.compare(leftHandValue, lowerValue) >= 0
                                           && comparator.compare(leftHandValue, upperValue) <= 0;
                                }

                                @Override
                                public String toString() {
                                    return "(filter " + Visitors.readable(constraint) + ")";
                                }
                            };
                        }
                        // Don't include upper ...
                        return new DynamicOperandFilter(operation) {
                            @Override
                            protected boolean evaluate( Object leftHandValue ) {
                                if (leftHandValue == null) return false; // null values never match
                                return comparator.compare(leftHandValue, lowerValue) >= 0
                                       && comparator.compare(leftHandValue, upperValue) < 0;
                            }

                            @Override
                            public String toString() {
                                return "(filter " + Visitors.readable(constraint) + ")";
                            }
                        };
                    }
                    assert !includeLower;
                    // Don't include lower
                    if (includeUpper) {
                        return new DynamicOperandFilter(operation) {
                            @Override
                            protected boolean evaluate( Object leftHandValue ) {
                                if (leftHandValue == null) return false; // null values never match
                                return comparator.compare(leftHandValue, lowerValue) > 0
                                       && comparator.compare(leftHandValue, upperValue) <= 0;
                            }

                            @Override
                            public String toString() {
                                return "(filter " + Visitors.readable(constraint) + ")";
                            }
                        };
                    }
                    // Don't include upper or lower ...
                    return new DynamicOperandFilter(operation) {
                        @Override
                        protected boolean evaluate( Object leftHandValue ) {
                            if (leftHandValue == null) return false; // null values never match
                            return comparator.compare(leftHandValue, lowerValue) > 0
                                   && comparator.compare(leftHandValue, upperValue) < 0;
                        }

                        @Override
                        public String toString() {
                            return "(filter " + Visitors.readable(constraint) + ")";
                        }
                    };
                }
            };
        }
        if (constraint instanceof Comparison) {
            Comparison comparison = (Comparison)constraint;

            // Create the correct dynamic operation ...
            final DynamicOperand dynamicOperand = comparison.getOperand1();
            final Operator operator = comparison.operator();
            final StaticOperand staticOperand = comparison.getOperand2();
            final TypeFactory<?> actualType = determineType(dynamicOperand, context, columns);
            TypeFactory<?> expectedType = null;
            ExtractFromRow op = null;
            if (operator == Operator.LIKE) {
                expectedType = context.getTypeSystem().getStringFactory();
                op = createExtractFromRow(dynamicOperand, context, columns, sources, expectedType, true, true);
                if (op.getType() != expectedType) {
                    // Need to convert the extracted value(s) to strings because this is a LIKE operation ...
                    op = RowExtractors.convert(op, expectedType);
                }
            } else {
                expectedType = actualType;
                op = createExtractFromRow(dynamicOperand, context, columns, sources, expectedType, true, false);
            }
            final TypeFactory<?> defaultType = expectedType;
            final ExtractFromRow operation = op;
            // Determine the literal value in the static operand ...
            return new RowFilterSupplier() {
                @Override
                protected RowFilter createFilter() {
                    // Evaluate the operand, which may have variables ...
                    final Object literalValue = literalValue(staticOperand, context, defaultType);
                    // Create the correct operation ...
                    final TypeFactory<?> expectedType = operation.getType();
                    final Object rhs = expectedType.create(literalValue);
                    @SuppressWarnings( "unchecked" )
                    final Comparator<Object> comparator = (Comparator<Object>)expectedType.getComparator();
                    switch (operator) {
                        case EQUAL_TO:
                            return new DynamicOperandFilter(operation) {
                                @Override
                                protected boolean evaluate( Object leftHandValue ) {
                                    if (leftHandValue == null) return false; // null values never match
                                    return comparator.compare(leftHandValue, rhs) == 0;
                                }

                                @Override
                                public String toString() {
                                    return "(filter " + Visitors.readable(constraint) + ")";
                                }
                            };
                        case NOT_EQUAL_TO:
                            return new DynamicOperandFilter(operation) {
                                @Override
                                protected boolean evaluate( Object leftHandValue ) {
                                    if (leftHandValue == null) return false; // null values never match
                                    return comparator.compare(leftHandValue, rhs) != 0;
                                }

                                @Override
                                public String toString() {
                                    return "(filter " + Visitors.readable(constraint) + ")";
                                }
                            };
                        case GREATER_THAN:
                            return new DynamicOperandFilter(operation) {
                                @Override
                                protected boolean evaluate( Object leftHandValue ) {
                                    if (leftHandValue == null) return false; // null values never match
                                    return comparator.compare(leftHandValue, rhs) > 0;
                                }

                                @Override
                                public String toString() {
                                    return "(filter " + Visitors.readable(constraint) + ")";
                                }
                            };
                        case GREATER_THAN_OR_EQUAL_TO:
                            return new DynamicOperandFilter(operation) {
                                @Override
                                protected boolean evaluate( Object leftHandValue ) {
                                    if (leftHandValue == null) return false; // null values never match
                                    return comparator.compare(leftHandValue, rhs) >= 0;
                                }

                                @Override
                                public String toString() {
                                    return "(filter " + Visitors.readable(constraint) + ")";
                                }
                            };
                        case LESS_THAN:
                            return new DynamicOperandFilter(operation) {
                                @Override
                                protected boolean evaluate( Object leftHandValue ) {
                                    if (leftHandValue == null) return false; // null values never match
                                    return comparator.compare(leftHandValue, rhs) < 0;
                                }

                                @Override
                                public String toString() {
                                    return "(filter " + Visitors.readable(constraint) + ")";
                                }
                            };
                        case LESS_THAN_OR_EQUAL_TO:
                            return new DynamicOperandFilter(operation) {
                                @Override
                                protected boolean evaluate( Object leftHandValue ) {
                                    if (leftHandValue == null) return false; // null values never match
                                    return comparator.compare(leftHandValue, rhs) <= 0;
                                }

                                @Override
                                public String toString() {
                                    return "(filter " + Visitors.readable(constraint) + ")";
                                }
                            };
                        case LIKE:
                            // Convert the LIKE expression to a regular expression
                            final TypeSystem types = context.getTypeSystem();
                            String expression = types.asString(rhs).trim();
                            if ("%".equals(expression)) {
                                // We'll accept any non-null value ...
                                return new DynamicOperandFilter(operation) {
                                    @Override
                                    protected boolean evaluate( Object leftHandValue ) {
                                        return leftHandValue != null;
                                    }

                                    @Override
                                    public String toString() {
                                        return "(filter " + Visitors.readable(constraint) + ")";
                                    }
                                };
                            }
                            if (Path.class.isAssignableFrom(actualType.getType())) {
                                // This LIKE is dealing with paths and SNS wildcards, so we have to extract path values that
                                // have SNS indexes in all segments ...
                                final PathFactory paths = context.getExecutionContext().getValueFactories().getPathFactory();
                                expression = QueryUtil.addSnsIndexesToLikeExpression(expression);
                                String regex = QueryUtil.toRegularExpression(expression);
                                final Pattern pattern = Pattern.compile(regex);
                                return new DynamicOperandFilter(operation) {
                                    @Override
                                    protected boolean evaluate( Object leftHandValue ) {
                                        if (leftHandValue == null) return false; // null values never match
                                        // Get the value as a path and construct a string representation with SNS indexes
                                        // in the correct spot ...
                                        Path path = paths.create(leftHandValue);
                                        String strValue = null;
                                        if (path.isRoot()) {
                                            strValue = "/";
                                        } else {
                                            StringBuilder sb = new StringBuilder();
                                            for (Path.Segment segment : path) {
                                                sb.append('/').append(types.asString(segment.getName()));
                                                sb.append('[').append(segment.getIndex()).append(']');
                                            }
                                            strValue = sb.toString();
                                        }
                                        return pattern.matcher(strValue).matches();
                                    }

                                    @Override
                                    public String toString() {
                                        return "(filter " + Visitors.readable(constraint) + ")";
                                    }
                                };
                            }
                            String regex = QueryUtil.toRegularExpression(expression);
                            final Pattern pattern = Pattern.compile(regex);
                            return new DynamicOperandFilter(operation) {
                                @Override
                                protected boolean evaluate( Object leftHandValue ) {
                                    if (leftHandValue == null) return false; // null values never match
                                    String value = types.asString(leftHandValue);
                                    return pattern.matcher(value).matches();
                                }

                                @Override
                                public String toString() {
                                    return "(filter " + Visitors.readable(constraint) + ")";
                                }
                            };
                    }
                    assert false : "Should not get here";
                    return null;
                }
            };
        }
        if (constraint instanceof SetCriteria) {
            final SetCriteria setCriteria = (SetCriteria)constraint;
            DynamicOperand operand = setCriteria.getOperand();
            final TypeFactory<?> defaultType = determineType(operand, context, columns);
            // If the set criteria contains a bind variable, then the operand filter should lazily evaluate the bind variable ...
            final ExtractFromRow operation = createExtractFromRow(operand, context, columns, sources, defaultType, true, false);
            final boolean trace = LOGGER.isTraceEnabled() && !defaultType.getTypeName().equals("NAME");
            final PathFactory pathFactory = context.getExecutionContext().getValueFactories().getPathFactory();
            return new RowFilterSupplier() {

                @Override
                protected RowFilter createFilter() {
                    final Set<?> values = ScanningQueryEngine.literalValues(setCriteria, context, defaultType);
                    return new DynamicOperandFilter(operation) {
                        @Override
                        protected boolean evaluate( Object leftHandValue ) {
                            if (Path.class.isAssignableFrom(defaultType.getType())) {
                                leftHandValue = leftHandValue instanceof Object[] ?
                                                pathFactory.create((Object[])leftHandValue) : 
                                                pathFactory.create(leftHandValue);
                            }
                            if (leftHandValue instanceof Object[]) {
                                for (Object leftValue : (Object[])leftHandValue) {
                                    if (values.contains(leftValue)) {
                                        if (trace) LOGGER.trace("Found '{0}' in values: {1}", leftHandValue, values);
                                        return true;
                                    }
                                }
                                if (trace) LOGGER.trace("Failed to find '{0}' in values: {1}", leftHandValue, values);
                                return false;
                            }

                            if (values.contains(leftHandValue)) {
                                if (trace) {
                                    LOGGER.trace("Found '{0}' in values: {1}", leftHandValue, values);
                                }
                                return true;
                            }
                            if (trace) {
                                LOGGER.trace("Failed to find '{0}' in values: {1}", leftHandValue, values);
                            }
                            return false;
                        }

                        @Override
                        public String toString() {
                            return "(filter " + Visitors.readable(constraint) + ")";
                        }
                    };
                }
            };
        }
        if (constraint instanceof FullTextSearch) {
            final TypeFactory<String> strings = context.getTypeSystem().getStringFactory();
            final StaticOperand ftsExpression = ((FullTextSearch)constraint).getFullTextSearchExpression();
            final FullTextSearch fts;
            if (ftsExpression instanceof BindVariableName) {
                Object searchExpression = literalValue(ftsExpression, context, strings);
                if (searchExpression != null) {
                    fts = ((FullTextSearch)constraint).withFullTextExpression(searchExpression.toString());
                } else {
                    fts = (FullTextSearch)constraint;
                }
            } else {
                fts = (FullTextSearch)constraint;
            }

            final NodeCache cache = context.getNodeCache(sources.getWorkspaceName());
            final BinaryStore binaries = context.getExecutionContext().getBinaryStore();
            String selectorName = fts.getSelectorName();
            String propertyName = fts.getPropertyName();
            final int index = columns.getSelectorIndex(selectorName);
            ExtractFromRow fullTextExtractor = null;
            if (propertyName != null) {
                // This is to search just the designated property of the node (name, all property values) ...
                final ExtractFromRow propertyValueExtractor = createExtractFromRow(selectorName, propertyName, context, columns,
                                                                                   sources, strings, true);
                fullTextExtractor = new ExtractFromRow() {
                    @Override
                    public TypeFactory<?> getType() {
                        return strings;
                    }

                    @Override
                    public Object getValueInRow( RowAccessor row ) {
                        Object result = propertyValueExtractor.getValueInRow(row);
                        if (result == null) return null;
                        StringBuilder fullTextString = new StringBuilder();
                        RowExtractors.extractFullTextFrom(result, strings, binaries, fullTextString);
                        return fullTextString.toString();
                    }
                };
            } else {
                // This is to search all aspects of the node (name, all property values) ...
                fullTextExtractor = RowExtractors.extractFullText(index, cache, context.getTypeSystem(), binaries);
            }
            // Return a filter that processes all of the text ...
            final ExtractFromRow extractor = fullTextExtractor;
            return new DynamicOperandFilter(extractor) {
                @Override
                protected boolean evaluate( Object leftHandValue ) {
                    /**
                     * The term will match the extracted value "as-is" via regex, without any stemming or punctuation removal.
                     * This means that the matching is done in a much more strict way than what Lucene did in 3.x If we were to
                     * implement stemming or hyphen removal, we would need to do it *both* in the row extractor
                     * (RowExtractors.extractFullText) and in the term where the regex is built
                     */
                    return fts.getTerm().matches(leftHandValue.toString());
                }
            };
        }
        if (constraint instanceof Relike) {
            Relike relike = (Relike)constraint;
            StaticOperand staticOperand = relike.getOperand1();
            Object literalValue = literalValue(staticOperand, context, context.getTypeSystem().getStringFactory());
            if (literalValue == null) {
                return NodeSequence.NO_PASS_ROW_FILTER;
            }
            final String literalStr = literalValue.toString();
            PropertyValue propertyValue = relike.getOperand2();
            NameFactory names = context.getExecutionContext().getValueFactories().getNameFactory();
            final Name propertyName = names.create(propertyValue.getPropertyName());
            final String selectorName = propertyValue.getSelectorName();
            final int index = columns.getSelectorIndex(selectorName);
            final NodeCache cache = context.getNodeCache(sources.getWorkspaceName());
            return new RowFilter() {
                @Override
                public boolean isCurrentRowValid( Batch batch ) {
                    CachedNode node = batch.getNode(index);
                    if (node == null) return false;
                    org.modeshape.jcr.value.Property property = node.getProperty(propertyName, cache);
                    if (property == null) return false;
                    for (Object value : property) {
                        if (value == null) continue;
                        // The property value should be a LIKE expression ...
                        String regex = toRegularExpression(value.toString());
                        final Pattern pattern = Pattern.compile(regex);
                        if (pattern.matcher(literalStr).matches()) return true;
                    }
                    return false;
                }
            };
        }
        assert false;
        return NodeSequence.PASS_ROW_FILTER;
    }

    protected TypeFactory<?> determineType( DynamicOperand operand,
                                            QueryContext context,
                                            Columns columns ) {
        TypeSystem types = context.getTypeSystem();
        if (operand instanceof PropertyValue) {
            PropertyValue propValue = (PropertyValue)operand;
            String typeName = columns.getColumnTypeForProperty(propValue.getSelectorName(), propValue.getPropertyName());
            return typeName != null ? types.getTypeFactory(typeName) : types.getStringFactory();
        }
        // The types used here must match those in NodeTypeSchemata's constructor
        if (operand instanceof NodeName) {
            return types.getNameFactory();
        }
        if (operand instanceof NodePath) {
            return types.getPathFactory();
        }
        if (operand instanceof Length) {
            return types.getLongFactory();
        }
        if (operand instanceof ChildCount) {
            return types.getLongFactory();
        }
        if (operand instanceof NodeDepth) {
            return types.getLongFactory();
        }
        if (operand instanceof FullTextSearchScore) {
            return types.getDoubleFactory();
        }
        if (operand instanceof LowerCase) {
            return types.getStringFactory();
        }
        if (operand instanceof UpperCase) {
            return types.getStringFactory();
        }
        if (operand instanceof NodeLocalName) {
            return types.getStringFactory();
        }
        if (operand instanceof ReferenceValue) {
            return types.getStringFactory();
        }
        if (operand instanceof ArithmeticOperand) {
            ArithmeticOperand arith = (ArithmeticOperand)operand;
            TypeFactory<?> leftType = determineType(arith.getLeft(), context, columns);
            TypeFactory<?> rightType = determineType(arith.getRight(), context, columns);
            return types.getCompatibleType(leftType, rightType);
        }
        if (operand instanceof Cast) {
            return types.getTypeFactory(((Cast)operand).getDesiredTypeName());
        }
        return types.getStringFactory();
    }

    /**
     * Create a {@link ExtractFromRow} implementation that performs the supplied {@link DynamicOperand} against a current row in
     * the current batch.
     * 
     * @param operand the dynamic operand
     * @param context the context in which the query is to be executed; may not be null
     * @param columns the result column definition; may not be null
     * @param sources the query sources for the repository; may not be null
     * @param defaultType the type that should be used by default, or null if an exception should be thrown when the type for the
     *        property name could not be determined
     * @param allowMultiValued true if the extractor called upon a particular node and multi-valued property return an Object[]
     *        that contains all the resulting values of the property, or false if only the first value should be returned
     * @param isLike true if the result will be used in a LIKE operation, or false otherwise; this may affect the number of
     *        results that will be returned
     * @return the dynamic operation implementation; never null
     */
    protected ExtractFromRow createExtractFromRow( DynamicOperand operand,
                                                   QueryContext context,
                                                   Columns columns,
                                                   final QuerySources sources,
                                                   TypeFactory<?> defaultType,
                                                   boolean allowMultiValued,
                                                   boolean isLike ) {
        assert operand != null;
        assert context != null;
        assert columns != null;
        assert sources != null;
        if (operand instanceof PropertyValue) {
            PropertyValue propValue = (PropertyValue)operand;
            String propertyName = propValue.getPropertyName();
            String selectorName = propValue.selectorName().name();
            return createExtractFromRow(selectorName, propertyName, context, columns, sources, defaultType, allowMultiValued);
        }
        if (operand instanceof ReferenceValue) {
            ReferenceValue refValue = (ReferenceValue)operand;
            String propertyName = refValue.getPropertyName();
            String selectorName = refValue.selectorName().name();
            if (propertyName == null) {
                return createExtractReferencesFromRow(selectorName, context, columns, sources, defaultType);
            }
            return createExtractFromRow(selectorName, propertyName, context, columns, sources, defaultType, allowMultiValued);
        }
        if (operand instanceof Length) {
            Length length = (Length)operand;
            final PropertyValue value = length.getPropertyValue();
            String propertyName = value.getPropertyName();
            String selectorName = value.selectorName().name();
            final ExtractFromRow getPropValue = createExtractFromRow(selectorName, propertyName, context, columns, sources,
                                                                     defaultType, allowMultiValued);
            final TypeFactory<?> longType = context.getTypeSystem().getLongFactory();
            return new ExtractFromRow() {
                @Override
                public Object getValueInRow( RowAccessor row ) {
                    Object typedValue = getPropValue.getValueInRow(row);
                    return getPropValue.getType().length(typedValue);
                }

                @Override
                public TypeFactory<?> getType() {
                    return longType;
                }

                @Override
                public String toString() {
                    return "(length " + getPropValue + ")";
                }
            };
        }
        final TypeFactory<String> stringFactory = context.getTypeSystem().getStringFactory();
        if (operand instanceof LowerCase) {
            LowerCase lowerCase = (LowerCase)operand;
            final ExtractFromRow delegate = createExtractFromRow(lowerCase.getOperand(), context, columns, sources, defaultType,
                                                                 allowMultiValued, false);
            return new ExtractFromRow() {
                @Override
                public TypeFactory<?> getType() {
                    return stringFactory;
                }

                @Override
                public Object getValueInRow( RowAccessor row ) {
                    Object valueInRow = delegate.getValueInRow(row);
                    if (valueInRow == null) {
                        return null;
                    }
                    if (valueInRow instanceof Object[]) {
                        // multi valued prop
                        Object[] values = (Object[])valueInRow;
                        Object[] lowerCasedValues = new Object[values.length];
                        for (int i = 0; i < values.length; i++) {  
                            String valueString = stringFactory.create(values[i]);
                            lowerCasedValues[i] = valueString.toLowerCase();
                        }
                        return lowerCasedValues;
                    } else {
                        // single valued prop
                        return stringFactory.create(valueInRow).toLowerCase();
                    }
                }

                @Override
                public String toString() {
                    return "(lowercase " + delegate + ")";
                }
            };
        }
        if (operand instanceof UpperCase) {
            final UpperCase upperCase = (UpperCase)operand;
            final ExtractFromRow delegate = createExtractFromRow(upperCase.getOperand(), context, columns, sources, defaultType,
                                                                 allowMultiValued, false);
            return new ExtractFromRow() {
                @Override
                public TypeFactory<?> getType() {
                    return stringFactory;
                }

                @Override
                public Object getValueInRow( RowAccessor row ) {
                    Object valueInRow = delegate.getValueInRow(row);
                    if (valueInRow == null) {
                        return null;
                    }
                    if (valueInRow instanceof Object[]) {
                        // multi valued prop
                        Object[] values = (Object[])valueInRow;
                        Object[] upperCasedValues = new Object[values.length];
                        for (int i = 0; i < values.length; i++) {
                            String valueString = stringFactory.create(values[i]);
                            upperCasedValues[i] = valueString.toUpperCase();
                        }
                        return upperCasedValues;
                    } else {
                        // single valued prop
                        return stringFactory.create(valueInRow).toUpperCase();
                    }
                }

                @Override
                public String toString() {
                    return "(uppercase " + delegate + ")";
                }
            };
        }
        if (operand instanceof Cast) {
            final Cast cast = (Cast)operand;
            final ExtractFromRow delegate = createExtractFromRow(cast.getOperand(), context, columns, sources, defaultType,
                                                                 allowMultiValued, false);
            final String desiredTypeName = cast.getDesiredTypeName();
            final TypeFactory<?> typeFactory = context.getTypeSystem().getTypeFactory(desiredTypeName);
            final Class<?> desiredType = typeFactory.getType();
            return new ExtractFromRow() {
                @Override
                public TypeFactory<?> getType() {
                    return typeFactory;
                }

                @Override
                public Object getValueInRow( RowAccessor row ) {
                    Object valueInRow = delegate.getValueInRow(row);
                    if (valueInRow == null) {
                        return null;
                    }
                    if (valueInRow instanceof Object[]) {
                        // multi valued prop
                        Object[] values = (Object[])valueInRow;
                        Object[] convertedValues = new Object[values.length];
                        for (int i = 0; i < values.length; i++) {
                            Object originalValue = values[i];
                            if (desiredType.isAssignableFrom(originalValue.getClass())) {
                                // short circuit if someone is trying to do a cast to same type as the original
                                return values;
                            }
                            Object convertedValue = typeFactory.create(originalValue);
                            convertedValues[i] = convertedValue;
                        }
                        return convertedValues;
                    } else {
                        // single valued prop
                        if (desiredType.isAssignableFrom(valueInRow.getClass()))  {
                            // short circuit if someone is trying to do a cast to same type as the original
                            return valueInRow;
                        }
                        return typeFactory.create(valueInRow);
                    }
                }

                @Override
                public String toString() {
                    return "(cast " + delegate + ")";
                }
            };
        }
        if (operand instanceof NodeDepth) {
            final NodeDepth nodeDepth = (NodeDepth)operand;
            final int indexInRow = columns.getSelectorIndex(nodeDepth.getSelectorName());
            final NodeCache cache = context.getNodeCache(sources.getWorkspaceName());
            final TypeFactory<?> longType = context.getTypeSystem().getLongFactory();
            return new ExtractFromRow() {
                @Override
                public TypeFactory<?> getType() {
                    return longType; // depth is always a long type
                }

                @Override
                public Object getValueInRow( RowAccessor row ) {
                    CachedNode node = row.getNode(indexInRow);
                    if (node == null) return null;
                    return new Long(node.getDepth(cache));
                }

                @Override
                public String toString() {
                    return "(nodeDepth " + nodeDepth.getSelectorName() + ")";
                }
            };
        }
        if (operand instanceof ChildCount) {
            final ChildCount childCount = (ChildCount)operand;
            final int indexInRow = columns.getSelectorIndex(childCount.getSelectorName());
            final NodeCache cache = context.getNodeCache(sources.getWorkspaceName());
            final TypeFactory<?> longType = context.getTypeSystem().getLongFactory();
            return new ExtractFromRow() {
                @Override
                public TypeFactory<?> getType() {
                    return longType; // count is always a long type
                }

                @Override
                public Object getValueInRow( RowAccessor row ) {
                    CachedNode node = row.getNode(indexInRow);
                    if (node == null) return null;
                    return new Long(node.getChildReferences(cache).size());
                }

                @Override
                public String toString() {
                    return "(childCount " + childCount.getSelectorName() + ")";
                }
            };
        }
        if (operand instanceof NodeId) {
            final NodeId nodeId = (NodeId)operand;
            final int indexInRow = columns.getSelectorIndex(nodeId.getSelectorName());
            final NodeCache cache = context.getNodeCache(sources.getWorkspaceName());
            final NodeKey root = cache.getRootKey();
            final TypeFactory<?> stringType = context.getTypeSystem().getStringFactory();
            return new ExtractFromRow() {
                @Override
                public TypeFactory<?> getType() {
                    return stringType; // ID is always a string type
                }

                @Override
                public Object getValueInRow( RowAccessor row ) {
                    CachedNode node = row.getNode(indexInRow);
                    if (node == null) return null;
                    return sources.getIdentifier(node, root);
                }

                @Override
                public String toString() {
                    return "(nodeId " + nodeId.getSelectorName() + ")";
                }
            };
        }
        if (operand instanceof NodePath) {
            final NodePath nodePath = (NodePath)operand;
            final int indexInRow = columns.getSelectorIndex(nodePath.getSelectorName());
            final NodeCache cache = context.getNodeCache(sources.getWorkspaceName());
            if (isLike) {
                return new ExtractFromRow() {
                    @Override
                    public TypeFactory<?> getType() {
                        return stringFactory;
                    }

                    @Override
                    public Object getValueInRow( RowAccessor row ) {
                        CachedNode node = row.getNode(indexInRow);
                        if (node == null) return null;
                        Path path = node.getPath(cache);
                        if (path.isRoot()) {
                            return stringFactory.create(path);
                        }
                        // And the path that always has the SNS index ...
                        StringBuilder sb = new StringBuilder();
                        for (Path.Segment segment : path) {
                            // Add the segment WITH the index ...
                            sb.append("/");
                            sb.append(stringFactory.create(segment.getName()));
                            sb.append('[').append(segment.getIndex()).append(']');
                        }
                        return sb.toString();
                    }

                    @Override
                    public String toString() {
                        return "(nodePath " + nodePath.getSelectorName() + ")";
                    }
                };
            }
            // Otherwise, just return the single path ...
            return new ExtractFromRow() {
                @Override
                public TypeFactory<?> getType() {
                    return stringFactory;
                }

                @Override
                public Object getValueInRow( RowAccessor row ) {
                    CachedNode node = row.getNode(indexInRow);
                    if (node == null) return null;
                    Path path = node.getPath(cache);
                    return stringFactory.create(path);
                }

                @Override
                public String toString() {
                    return "(nodePath " + nodePath.getSelectorName() + ")";
                }
            };
        }
        if (operand instanceof NodeName) {
            final NodeName nodeName = (NodeName)operand;
            final int indexInRow = columns.getSelectorIndex(nodeName.getSelectorName());
            final NodeCache cache = context.getNodeCache(sources.getWorkspaceName());
            return new ExtractFromRow() {
                @Override
                public TypeFactory<?> getType() {
                    return stringFactory;
                }

                @Override
                public Object getValueInRow( RowAccessor row ) {
                    CachedNode node = row.getNode(indexInRow);
                    if (node == null) return null;
                    Name name = node.getName(cache);
                    return stringFactory.create(name);
                }

                @Override
                public String toString() {
                    return "(nodeName " + nodeName.getSelectorName() + ")";
                }
            };
        }
        if (operand instanceof NodeLocalName) {
            final NodeLocalName nodeName = (NodeLocalName)operand;
            final int indexInRow = columns.getSelectorIndex(nodeName.getSelectorName());
            final NodeCache cache = context.getNodeCache(sources.getWorkspaceName());
            return new ExtractFromRow() {
                @Override
                public TypeFactory<?> getType() {
                    return stringFactory;
                }

                @Override
                public Object getValueInRow( RowAccessor row ) {
                    CachedNode node = row.getNode(indexInRow);
                    if (node == null) return null;
                    Name name = node.getName(cache);
                    return name.getLocalName(); // works even for root
                }

                @Override
                public String toString() {
                    return "(localName " + nodeName.getSelectorName() + ")";
                }
            };
        }
        if (operand instanceof FullTextSearchScore) {
            final FullTextSearchScore fts = (FullTextSearchScore)operand;
            final int indexInRow = columns.getSelectorIndex(fts.getSelectorName());
            final TypeFactory<Double> doubleType = context.getTypeSystem().getDoubleFactory();
            return new ExtractFromRow() {
                @Override
                public TypeFactory<?> getType() {
                    return doubleType;
                }

                @Override
                public Object getValueInRow( RowAccessor row ) {
                    return new Double(row.getScore(indexInRow)); // must convert the float to a double value
                }

                @Override
                public String toString() {
                    return "(fullTextScore " + fts.getSelectorName() + ")";
                }
            };
        }
        if (operand instanceof ArithmeticOperand) {
            // This works on single-valued properties only ...
            ArithmeticOperand arith = (ArithmeticOperand)operand;
            final ExtractFromRow leftOp = createExtractFromRow(arith.getLeft(), context, columns, sources, defaultType, false,
                                                               false);
            final ExtractFromRow rightOp = createExtractFromRow(arith.getRight(), context, columns, sources, defaultType, false,
                                                                false);
            // compute the expected (common) type ...
            TypeFactory<?> leftType = leftOp.getType();
            TypeFactory<?> rightType = rightOp.getType();
            final TypeSystem typeSystem = context.getTypeSystem();
            final String commonType = typeSystem.getCompatibleType(leftType.getTypeName(), rightType.getTypeName());
            if (typeSystem.getDoubleFactory().getTypeName().equals(commonType)) {
                final TypeFactory<Double> commonTypeFactory = typeSystem.getDoubleFactory();
                switch (arith.operator()) {
                    case ADD:
                        return new ExtractFromRow() {
                            @Override
                            public TypeFactory<?> getType() {
                                return commonTypeFactory;
                            }

                            @Override
                            public Object getValueInRow( RowAccessor row ) {
                                Double right = commonTypeFactory.create(rightOp.getValueInRow(row));
                                Double left = commonTypeFactory.create(leftOp.getValueInRow(row));
                                if (right == null) return left;
                                if (left == null) return right;
                                return left.doubleValue() / right.doubleValue();
                            }

                            @Override
                            public String toString() {
                                return "(double + " + leftOp + "," + rightOp + ")";
                            }
                        };
                    case SUBTRACT:
                        return new ExtractFromRow() {
                            @Override
                            public TypeFactory<?> getType() {
                                return commonTypeFactory;
                            }

                            @Override
                            public Object getValueInRow( RowAccessor row ) {
                                Double right = commonTypeFactory.create(rightOp.getValueInRow(row));
                                Double left = commonTypeFactory.create(leftOp.getValueInRow(row));
                                if (right == null) return left;
                                if (left == null) left = 0.0d;
                                return left.doubleValue() * right.doubleValue();
                            }

                            @Override
                            public String toString() {
                                return "(double - " + leftOp + "," + rightOp + ")";
                            }
                        };
                    case MULTIPLY:
                        return new ExtractFromRow() {
                            @Override
                            public TypeFactory<?> getType() {
                                return commonTypeFactory;
                            }

                            @Override
                            public Object getValueInRow( RowAccessor row ) {
                                Double right = commonTypeFactory.create(rightOp.getValueInRow(row));
                                Double left = commonTypeFactory.create(leftOp.getValueInRow(row));
                                if (right == null || left == null) return null;
                                return left.doubleValue() * right.doubleValue();
                            }

                            @Override
                            public String toString() {
                                return "(double x " + leftOp + "," + rightOp + ")";
                            }
                        };
                    case DIVIDE:
                        return new ExtractFromRow() {
                            @Override
                            public TypeFactory<?> getType() {
                                return commonTypeFactory;
                            }

                            @Override
                            public Object getValueInRow( RowAccessor row ) {
                                Double right = commonTypeFactory.create(rightOp.getValueInRow(row));
                                Double left = commonTypeFactory.create(leftOp.getValueInRow(row));
                                if (right == null || left == null) return null;
                                return left.doubleValue() / right.doubleValue();
                            }

                            @Override
                            public String toString() {
                                return "(double / " + leftOp + "," + rightOp + ")";
                            }
                        };
                }
            } else if (typeSystem.getLongFactory().getTypeName().equals(commonType)) {
                final TypeFactory<Long> commonTypeFactory = typeSystem.getLongFactory();
                switch (arith.operator()) {
                    case ADD:
                        return new ExtractFromRow() {
                            @Override
                            public TypeFactory<?> getType() {
                                return commonTypeFactory;
                            }

                            @Override
                            public Object getValueInRow( RowAccessor row ) {
                                Long right = commonTypeFactory.create(rightOp.getValueInRow(row));
                                Long left = commonTypeFactory.create(leftOp.getValueInRow(row));
                                if (right == null) return left;
                                if (left == null) return right;
                                return left.longValue() / right.longValue();
                            }

                            @Override
                            public String toString() {
                                return "(long + " + leftOp + "," + rightOp + ")";
                            }
                        };
                    case SUBTRACT:
                        return new ExtractFromRow() {
                            @Override
                            public TypeFactory<?> getType() {
                                return commonTypeFactory;
                            }

                            @Override
                            public Object getValueInRow( RowAccessor row ) {
                                Long right = commonTypeFactory.create(rightOp.getValueInRow(row));
                                Long left = commonTypeFactory.create(leftOp.getValueInRow(row));
                                if (right == null) return left;
                                if (left == null) left = 0L;
                                return left.longValue() * right.longValue();
                            }

                            @Override
                            public String toString() {
                                return "(long - " + leftOp + "," + rightOp + ")";
                            }
                        };
                    case MULTIPLY:
                        return new ExtractFromRow() {
                            @Override
                            public TypeFactory<?> getType() {
                                return commonTypeFactory;
                            }

                            @Override
                            public Object getValueInRow( RowAccessor row ) {
                                Long right = commonTypeFactory.create(rightOp.getValueInRow(row));
                                Long left = commonTypeFactory.create(leftOp.getValueInRow(row));
                                if (right == null || left == null) return null;
                                return left.longValue() * right.longValue();
                            }

                            @Override
                            public String toString() {
                                return "(long x " + leftOp + "," + rightOp + ")";
                            }
                        };
                    case DIVIDE:
                        return new ExtractFromRow() {
                            @Override
                            public TypeFactory<?> getType() {
                                return commonTypeFactory;
                            }

                            @Override
                            public Object getValueInRow( RowAccessor row ) {
                                Long right = commonTypeFactory.create(rightOp.getValueInRow(row));
                                Long left = commonTypeFactory.create(leftOp.getValueInRow(row));
                                if (right == null || left == null) return null;
                                return left.longValue() / right.longValue();
                            }

                            @Override
                            public String toString() {
                                return "(long / " + leftOp + "," + rightOp + ")";
                            }
                        };
                }
            }
        }
        assert false;
        return null;
    }

    protected static abstract class PropertyValueExtractor implements ExtractFromRow {
        private final TypeFactory<?> typeFactory;
        private final String selectorName;
        private final String propertyName;

        protected PropertyValueExtractor( String selectorName,
                                          String propertyName,
                                          TypeFactory<?> typeFactory ) {
            this.selectorName = selectorName;
            this.propertyName = propertyName;
            this.typeFactory = typeFactory;
        }

        @Override
        public TypeFactory<?> getType() {
            return typeFactory;
        }

        @Override
        public String toString() {
            return "(" + Visitors.readable(new PropertyValue(new SelectorName(selectorName), propertyName)) + ")";
        }
    }

    /**
     * Create a {@link ExtractFromRow} implementation that accesses the value(s) in the property identified by the supplied
     * selector and property names.
     * 
     * @param selectorName the name of the selector containing the node(s) to be accessed; may not be null
     * @param propertyName the name of the property on the node(s) to be accessed; may not be null
     * @param context the context in which the query is to be executed; may not be null
     * @param columns the result column definition; may not be null
     * @param sources the query sources for the repository; may not be null
     * @param defaultType the type that should be used by default, or null if an exception should be thrown when the type for the
     *        property name could not be determined
     * @param allowMultiValued true if the extractor called upon a particular node and multi-valued property return an Object[]
     *        that contains all the resulting values of the property, or false if only the first value should be returned
     * @return the dynamic operation implementation; never null
     */
    protected ExtractFromRow createExtractFromRow( final String selectorName,
                                                   final String propertyName,
                                                   QueryContext context,
                                                   Columns columns,
                                                   QuerySources sources,
                                                   TypeFactory<?> defaultType,
                                                   boolean allowMultiValued ) {
        final Name propName = context.getExecutionContext().getValueFactories().getNameFactory().create(propertyName);
        final NodeCache cache = context.getNodeCache(sources.getWorkspaceName());
        // Find the expected property type of the value ...
        assert columns != null;
        final int indexInRow = columns.getSelectorIndex(selectorName);
        if (PseudoColumns.contains(propName, true)) {
            if (PseudoColumns.isScore(propName)) {
                // This is a special case of obtaining the score from the row ...
                final TypeFactory<?> typeFactory = context.getTypeSystem().getDoubleFactory();
                return new PropertyValueExtractor(selectorName, propertyName, typeFactory) {

                    @Override
                    public Object getValueInRow( RowAccessor row ) {
                        // We have to convert this to a double, since 'float' is not a valid JCR property type ...
                        return new Double(row.getScore(indexInRow));
                    }
                };
            }
            if (PseudoColumns.isPath(propName)) {
                // This is a special case of obtaining the score from the row ...
                final TypeFactory<?> typeFactory = context.getTypeSystem().getPathFactory();
                return new PropertyValueExtractor(selectorName, propertyName, typeFactory) {

                    @Override
                    public Object getValueInRow( RowAccessor row ) {
                        CachedNode node = row.getNode(indexInRow);
                        if (node == null) return null;
                        return node.getPath(cache);
                    }
                };
            }
            if (PseudoColumns.isDepth(propName)) {
                // This is a special case of obtaining the score from the row ...
                final TypeFactory<?> typeFactory = context.getTypeSystem().getLongFactory();
                return new PropertyValueExtractor(selectorName, propertyName, typeFactory) {

                    @Override
                    public Object getValueInRow( RowAccessor row ) {
                        CachedNode node = row.getNode(indexInRow);
                        if (node == null) return null;
                        return node.getDepth(cache);
                    }
                };
            }
            if (PseudoColumns.isName(propName)) {
                // This is a special case of obtaining the score from the row ...
                final TypeFactory<?> typeFactory = context.getTypeSystem().getNameFactory();
                return new PropertyValueExtractor(selectorName, propertyName, typeFactory) {

                    @Override
                    public Object getValueInRow( RowAccessor row ) {
                        CachedNode node = row.getNode(indexInRow);
                        if (node == null) return null;
                        return node.getName(cache);
                    }
                };
            }
            if (PseudoColumns.isLocalName(propName)) {
                // This is a special case of obtaining the score from the row ...
                final TypeFactory<?> typeFactory = context.getTypeSystem().getStringFactory();
                return new PropertyValueExtractor(selectorName, propertyName, typeFactory) {

                    @Override
                    public Object getValueInRow( RowAccessor row ) {
                        CachedNode node = row.getNode(indexInRow);
                        if (node == null) return null;
                        return node.getName(cache).getLocalName();
                    }
                };
            }
            if (PseudoColumns.isId(propName)) {
                // This is a special case of obtaining the identifier from the row ...
                final TypeFactory<?> typeFactory = context.getTypeSystem().getStringFactory();
                return new PropertyValueExtractor(selectorName, propertyName, typeFactory) {

                    @Override
                    public Object getValueInRow( RowAccessor row ) {
                        CachedNode node = row.getNode(indexInRow);
                        if (node == null) return null;
                        return node.getKey().toString();
                    }
                };
            }
            if (PseudoColumns.isUuid(propName)) {
                // This is a special case of obtaining the "jcr:uuid" value from the row, except that the
                // CachedNode instances don't know about this property ...
                final TypeFactory<?> typeFactory = context.getTypeSystem().getStringFactory();
                final NodeTypes nodeTypes = context.getNodeTypes();
                return new PropertyValueExtractor(selectorName, propertyName, typeFactory) {

                    @Override
                    public Object getValueInRow( RowAccessor row ) {
                        CachedNode node = row.getNode(indexInRow);
                        if (node == null) return null;
                        if (nodeTypes.isReferenceable(node.getPrimaryType(cache), node.getMixinTypes(cache))) {
                            // The node is a 'mix:referenceable' node, so return the UUID ...
                            NodeKey key = node.getKey();
                            return key.getIdentifier();
                        }
                        return null;
                    }
                };
            }
        }
        String expectedType = null;
        try {
            expectedType = columns.getColumnTypeForProperty(selectorName, propertyName);
        } catch (NoSuchElementException e) {
            if (defaultType == null) throw e;
        }
        final TypeFactory<?> typeFactory = expectedType != null ? context.getTypeSystem().getTypeFactory(expectedType) : defaultType;
        if (allowMultiValued) {
            return new ExtractFromRow() {
                @Override
                public Object getValueInRow( RowAccessor row ) {
                    CachedNode node = row.getNode(indexInRow);
                    if (node == null) return null;
                    org.modeshape.jcr.value.Property prop = node.getProperty(propName, cache);
                    if (prop == null || prop.isEmpty()) return null;
                    if (prop.isSingle()) {
                        return typeFactory.create(prop.getFirstValue());
                    }
                    assert prop.isMultiple();
                    Object[] result = new Object[prop.size()];
                    int i = -1;
                    for (Object value : prop) {
                        result[++i] = typeFactory.create(value);
                    }
                    return result;
                }

                @Override
                public TypeFactory<?> getType() {
                    return typeFactory;
                }

                @Override
                public String toString() {
                    return "(" + selectorName + "." + propertyName + ")";
                }
            };
        }
        return new ExtractFromRow() {
            @Override
            public Object getValueInRow( RowAccessor row ) {
                CachedNode node = row.getNode(indexInRow);
                if (node == null) return null;
                org.modeshape.jcr.value.Property prop = node.getProperty(propName, cache);
                if (prop == null || prop.isEmpty()) return null;
                return typeFactory.create(prop.getFirstValue());
            }

            @Override
            public TypeFactory<?> getType() {
                return typeFactory;
            }

            @Override
            public String toString() {
                return "(" + selectorName + "." + propertyName + ")";
            }
        };
    }

    /**
     * Create a {@link ExtractFromRow} implementation that accesses the REFERENCE value(s) in the properties of the node
     * identified by the supplied selector names.
     * 
     * @param selectorName the name of the selector containing the node(s) to be accessed; may not be null
     * @param context the context in which the query is to be executed; may not be null
     * @param columns the result column definition; may not be null
     * @param sources the query sources for the repository; may not be null
     * @param defaultType the type that should be used by default, or null if an exception should be thrown when the type for the
     *        property name could not be determined
     * @return the dynamic operation implementation; never null
     */
    protected ExtractFromRow createExtractReferencesFromRow( final String selectorName,
                                                             QueryContext context,
                                                             Columns columns,
                                                             QuerySources sources,
                                                             TypeFactory<?> defaultType ) {
        final NodeCache cache = context.getNodeCache(sources.getWorkspaceName());
        // Find the expected property type of the value ...
        assert columns != null;
        final int indexInRow = columns.getSelectorIndex(selectorName);
        final TypeFactory<?> typeFactory = context.getTypeSystem().getStringFactory();
        final boolean trace = LOGGER.isTraceEnabled();
        return new ExtractFromRow() {
            @Override
            public Object getValueInRow( RowAccessor row ) {
                CachedNode node = row.getNode(indexInRow);
                if (node == null) return null;
                List<Object> values = null;
                for (Iterator<org.modeshape.jcr.value.Property> iter = node.getProperties(cache); iter.hasNext();) {
                    org.modeshape.jcr.value.Property prop = iter.next();
                    if (prop == null || prop.isEmpty()) continue;
                    if (prop.isReference() || prop.isSimpleReference()) {
                        if (prop.isSingle()) {
                            Object value = prop.getFirstValue();
                            if (value != null) {
                                if (values == null) values = new LinkedList<>();
                                values.add(typeFactory.create(value));
                            }
                        } else {
                            assert prop.isMultiple();
                            for (Object value : prop) {
                                if (value == null) continue;
                                if (values == null) values = new LinkedList<>();
                                values.add(typeFactory.create(value));
                            }
                        }
                    }
                }
                if (values == null || values.isEmpty()) return null;
                if (trace) {
                    LOGGER.trace("Found references in '{0}': {1}", node.getPath(cache), values);
                }
                return values.toArray();
            }

            @Override
            public TypeFactory<?> getType() {
                return typeFactory;
            }

            @Override
            public String toString() {
                return "(references " + selectorName + ")";
            }
        };
    }

    /**
     * Interface for evaluating a {@link DynamicOperand} against the current row in a {@link Batch} and returning the
     * corresponding value.
     */
    protected static abstract class DynamicOperandFilter implements RowFilter {
        private final ExtractFromRow extractor;

        protected DynamicOperandFilter( ExtractFromRow extractor ) {
            this.extractor = extractor;
        }

        @Override
        public boolean isCurrentRowValid( Batch batch ) {
            Object lhs = extractor.getValueInRow(batch);
            if (lhs == null) return false; // NULL never matches any value, even NULL
            if (lhs instanceof Object[]) {
                // The value is an array, meaning the dynamic operand was a multi-valued property ...
                for (Object lhsValue : (Object[])lhs) {
                    if (evaluate(lhsValue)) return true;
                }
                return false;
            }
            return evaluate(lhs);
        }

        /**
         * Determine whether the left hand side of the expression (as computed by the dynamic operation) will result in this row
         * being included in the results. Note that implementations will compare this left hand side for the row with a fixed
         * right hand side value.
         * 
         * @param leftHandValue the left hand side of the current row; may be null
         * @return true if the left hand value satisfies this filter, or false otherwise
         */
        protected abstract boolean evaluate( Object leftHandValue );
    }

    /**
     * A {@link RowFilter} implementation that lazily initializes the real RowFilter implementation the first time it's needed and
     * thereafter will simply delegate to the implementation.
     * 
     * @author Randall Hauch (rhauch@redhat.com)
     */
    protected static abstract class RowFilterSupplier implements RowFilter {
        private RowFilter delegate;

        @Override
        public final boolean isCurrentRowValid( Batch batch ) {
            return delegate().isCurrentRowValid(batch);
        }

        protected final RowFilter delegate() {
            if (delegate == null) {
                delegate = createFilter();
            }
            return delegate;
        }

        /**
         * Instantiate the RowFilter that will be used. This method may be called more than once only if
         * {@link #isCurrentRowValid(Batch)} is called from multiple threads.
         * 
         * @return the row filter; may not be null
         */
        protected abstract RowFilter createFilter();

        @Override
        public String toString() {
            return delegate().toString();
        }
    }

    /**
     * Get the literal value that is defined in the supplied {@link StaticOperand}. If the supplied static operand is a
     * {@link BindVariableValue}, the the variable value is obtained from the {@link QueryContext#getVariables() variables} in the
     * {@link QueryContext}. Otherwise, this method simply casts the {@link StaticOperand} to a {@link Literal} value.
     * 
     * @param staticOperand the static operand; may be null
     * @param context the query context; may not be null
     * @param type the type factory for the expected type; may be null if the value should be used as-is
     * @return the literal value; may be null
     */
    protected static Object literalValue( StaticOperand staticOperand,
                                          QueryContext context,
                                          TypeFactory<?> type ) {
        // Determine the literal value ...
        Object literalValue = null;
        if (staticOperand instanceof BindVariableName) {
            BindVariableName bindVariable = (BindVariableName)staticOperand;
            String variableName = bindVariable.getBindVariableName();
            literalValue = context.getVariables().get(variableName); // may be null
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Using variable '{0}' value: {1}", variableName, literalValue);
            }
            if (literalValue instanceof Collection || literalValue instanceof Object[]) return literalValue;
            return type.create(literalValue); // without converting!
        }
        if (staticOperand instanceof LiteralValue) {
            LiteralValue literal = (LiteralValue)staticOperand;
            Value value = literal.getLiteralValue();
            if (value != null) {
                // Use the proper type factory to ensure the value is the correct type ...
                PropertyType propType = PropertyTypeUtil.modePropertyTypeFor(value.getType());
                try {
                    literalValue = context.getExecutionContext().getValueFactories().getValueFactory(propType)
                                          .create(value.getString());
                } catch (RepositoryException e) {
                    // Really shouldn't happen, but just in case ...
                    throw new SystemFailureException(e);
                }
            }
        } else if (staticOperand instanceof Literal) {
            Literal literal = (Literal)staticOperand;
            literalValue = literal.value();
        }
        return type != null ? type.create(literalValue) : null;
    }

    protected static Set<?> literalValues( SetCriteria setCriteria,
                                           QueryContext context,
                                           TypeFactory<?> type ) {
        Set<Object> values = new HashSet<>();
        for (Object value : setCriteria.getValues()) {
            if (value instanceof StaticOperand) {
                Object literal = literalValue((StaticOperand)value, context, type);
                if (literal instanceof Collection) {
                    for (Object v : (Collection<?>)literal) {
                        if (v != null) values.add(type.create(v));
                    }
                    values.addAll((Collection<?>)literal);
                } else if (literal instanceof Object[]) {
                    for (Object v : (Object[])literal) {
                        if (v != null) values.add(type.create(v));
                    }
                } else if (literal != null) {
                    values.add(type.create(literal));
                }
            }
        }
        return values;
    }

    /**
     * Convert the JCR like expression to a Lucene wildcard expression. The JCR like expression uses '%' to match 0 or more
     * characters, '_' to match any single character, '\x' to match the 'x' character, and all other characters to match
     * themselves.
     * 
     * @param likeExpression the like expression; may not be null
     * @return the expression that can be used with a WildcardQuery; never null
     */
    protected static String toWildcardExpression( String likeExpression ) {
        return likeExpression.replace('%', '*').replace('_', '?').replaceAll("\\\\(.)", "$1");
    }

    /**
     * Convert the JCR like expression to a regular expression. The JCR like expression uses '%' to match 0 or more characters,
     * '_' to match any single character, '\x' to match the 'x' character, and all other characters to match themselves. Note that
     * if any regex metacharacters appear in the like expression, they will be escaped within the resulting regular expression.
     * 
     * @param likeExpression the like expression; may not be null
     * @return the expression that can be used with a WildcardQuery; never null
     */
    public static String toRegularExpression( String likeExpression ) {
        // Replace all '\x' with 'x' ...
        String result = likeExpression.replaceAll("\\\\(.)", "$1");
        // Escape characters used as metacharacters in regular expressions, including
        // '[', '^', '\', '$', '.', '|', '+', '(', and ')'
        // But leave '?' and '*'
        result = result.replaceAll("([$.|+()\\[\\\\^\\\\\\\\])", "\\\\$1");
        // Replace '%'->'[.]*' and '_'->'[.]
        // (order of these calls is important!)
        result = result.replace("*", ".*").replace("?", ".");
        result = result.replace("%", ".*").replace("_", ".");
        return result;
    }

    // protected RowFilter createRowFilter( DynamicOperand dynamicOperand, Operator op, StaticOperand Constraint constraint,
    // QueryContext context, Columns columns, QuerySources sources ) {
    // }

    public static final class ResultColumns implements org.modeshape.jcr.query.QueryResults.Columns {

        private static final long serialVersionUID = 1L;

        protected static final List<Column> NO_COLUMNS = Collections.<Column>emptyList();
        protected static final List<String> NO_TYPES = Collections.<String>emptyList();
        protected static final String DEFAULT_SELECTOR_NAME = "Results";
        protected static final ResultColumns EMPTY = new ResultColumns(null, null, false, null);

        private final List<Column> columns;
        private final List<String> columnTypes;
        private final List<String> columnNames;
        private final List<String> selectorNames;
        private final Map<String, Integer> selectorIndexBySelectorName;
        private final Map<String, String> selectorNameByColumnName;
        private final Map<String, String> propertyNameByColumnName;
        private final Map<String, Map<String, ColumnInfo>> columnIndexByPropertyNameBySelectorName;
        private final boolean includeFullTextSearchScores;

        protected final static class ColumnInfo {
            protected final int columnIndex;
            protected final String type;

            protected ColumnInfo( int columnIndex,
                                  String type ) {
                this.columnIndex = columnIndex;
                this.type = type;
            }

            @Override
            public String toString() {
                return "" + columnIndex + "(" + type + ")";
            }
        }

        /**
         * Create a new definition for the query results given the supplied columns.
         * 
         * @param columns the columns that define the results; should never be modified directly
         * @param columnTypes the names of the types for each column in <code>columns</code>
         * @param includeFullTextSearchScores true if the results should include full text search scores, or false otherwise
         * @param precedingColumns the columns for the preceding plan node and which contain correct selector indexes; may be null
         */
        public ResultColumns( List<Column> columns,
                              List<String> columnTypes,
                              boolean includeFullTextSearchScores,
                              Columns precedingColumns ) {
            this.includeFullTextSearchScores = includeFullTextSearchScores;
            this.columns = columns != null ? Collections.<Column>unmodifiableList(columns) : NO_COLUMNS;
            this.columnTypes = columnTypes != null ? Collections.<String>unmodifiableList(columnTypes) : NO_TYPES;
            this.selectorIndexBySelectorName = new HashMap<String, Integer>();
            this.columnIndexByPropertyNameBySelectorName = new HashMap<String, Map<String, ColumnInfo>>();
            this.selectorNameByColumnName = new HashMap<String, String>();
            this.propertyNameByColumnName = new HashMap<String, String>();

            Set<String> selectors = new HashSet<String>();
            final int columnCount = this.columns.size();
            List<String> names = new ArrayList<String>(columnCount);
            List<String> selectorNames = new ArrayList<String>(columnCount);
            Set<Column> sameNameColumns = findColumnsWithSameNames(this.columns);

            // Find all the selector names ...
            int selectorIndex = 0;
            for (int i = 0, max = this.columns.size(); i != max; ++i) {
                Column column = this.columns.get(i);
                assert column != null;
                String selectorName = column.selectorName().name();
                if (selectors.add(selectorName)) {
                    selectorNames.add(selectorName);
                    int index = selectorIndex;
                    if (precedingColumns != null) {
                        index = precedingColumns.getSelectorIndex(selectorName);
                        if (index < 0) index = selectorIndex;
                    }
                    selectorIndexBySelectorName.put(selectorName, index);
                    ++selectorIndex;
                }
            }

            // Now, find all of the column names ...
            for (int i = 0, max = this.columns.size(); i != max; ++i) {
                Column column = this.columns.get(i);
                assert column != null;
                String selectorName = column.selectorName().name();
                String columnName = columnNameFor(column, names, sameNameColumns, selectors);
                assert columnName != null;
                propertyNameByColumnName.put(columnName, column.getPropertyName());
                selectorNameByColumnName.put(columnName, selectorName);
                // Insert the entry by selector name and property name ...
                Map<String, ColumnInfo> byPropertyName = columnIndexByPropertyNameBySelectorName.get(selectorName);
                if (byPropertyName == null) {
                    byPropertyName = new HashMap<String, ColumnInfo>();
                    columnIndexByPropertyNameBySelectorName.put(selectorName, byPropertyName);
                }
                String columnType = this.columnTypes.get(i);
                byPropertyName.put(column.getPropertyName(), new ColumnInfo(i, columnType));
            }
            if (columns != null && selectorNames.isEmpty()) {
                String selectorName = DEFAULT_SELECTOR_NAME;
                selectorNames.add(selectorName);
                selectorIndexBySelectorName.put(selectorName, 0);
            }
            this.selectorNames = Collections.unmodifiableList(selectorNames);
            this.columnNames = Collections.unmodifiableList(names);
        }

        protected static Set<Column> findColumnsWithSameNames( List<Column> columns ) {
            Multimap<String, Column> columnNames = ArrayListMultimap.create();
            for (Column column : columns) {
                String columnName = column.getColumnName() != null ? column.getColumnName() : column.getPropertyName();
                columnNames.put(columnName, column);
            }
            Set<Column> results = new HashSet<Column>();
            for (Map.Entry<String, Collection<Column>> entry : columnNames.asMap().entrySet()) {
                if (entry.getValue().size() > 1) {
                    results.addAll(entry.getValue());
                }
            }
            return results;
        }

        protected static String columnNameFor( Column column,
                                               List<String> columnNames,
                                               Set<Column> columnsWithDuplicateNames,
                                               Collection<String> selectorNames ) {
            String columnName = column.getColumnName() != null ? column.getColumnName() : column.getPropertyName();
            boolean qualified = columnName != null ? columnName.startsWith(column.getSelectorName() + ".") : false;
            boolean aliased = columnName != null ? !columnName.equals(column.getPropertyName()) : false;
            if (column.getPropertyName() == null || columnNames.contains(columnName)
                || columnsWithDuplicateNames.contains(column)) {
                // Per section 6.7.39 of the JSR-283 specification, if the property name for a column is not given
                // then the name for the column in the result set must be "selectorName.propertyName" ...
                columnName = column.selectorName() + "." + columnName;
            } else if (!qualified && !aliased && selectorNames.size() > 1) {
                // When there is more than one selector, all columns need to be named "selectorName.propertyName" ...
                columnName = column.selectorName() + "." + columnName;
            }
            columnNames.add(columnName);
            return columnName;
        }

        @Override
        public Iterator<Column> iterator() {
            return columns.iterator();
        }

        @Override
        public List<? extends Column> getColumns() {
            return columns;
        }

        @Override
        public List<String> getColumnNames() {
            return columnNames;
        }

        @Override
        public List<String> getColumnTypes() {
            return columnTypes;
        }

        @Override
        public String getColumnTypeForProperty( String selectorName,
                                                String propertyName ) {
            Map<String, ColumnInfo> byPropertyName = columnIndexByPropertyNameBySelectorName.get(selectorName);
            if (byPropertyName == null) {
                throw new NoSuchElementException(GraphI18n.selectorDoesNotExistInQuery.text(selectorName));
            }
            ColumnInfo result = byPropertyName.get(propertyName);
            return result != null ? result.type : null;
        }

        @Override
        public int getSelectorIndex( String selectorName ) {
            if (!selectorNames.contains(selectorName)) return -1;
            return selectorIndexBySelectorName.get(selectorName);
        }

        @Override
        public List<String> getSelectorNames() {
            return selectorNames;
        }

        @Override
        public String getPropertyNameForColumnName( String columnName ) {
            String result = propertyNameByColumnName.get(columnName);
            return result != null ? result : columnName;
        }

        @Override
        public String getSelectorNameForColumnName( String columnName ) {
            return selectorNameByColumnName.get(columnName);
        }

        @Override
        public boolean hasFullTextSearchScores() {
            return includeFullTextSearchScores;
        }

        @Override
        public int hashCode() {
            return getColumns().hashCode();
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof ResultColumns) {
                ResultColumns that = (ResultColumns)obj;
                return this.getColumns().equals(that.getColumns());
            }
            return false;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append("{ ").append("\n");
            if (this.columnNames != null) {
                boolean first = true;
                for (int i = 0; i != columns.size(); ++i) {
                    Column column = columns.get(i);
                    String type = columnTypes.get(i);
                    if (first) first = false;
                    else sb.append(", ").append("\n");
                    sb.append(Visitors.readable(column)).append("{").append(type.toUpperCase()).append("}");
                }
            }
            sb.append("\n").append(" }");
            return sb.toString();
        }

        @Override
        public Columns with( Columns other ) {
            final int maxNumberOfColumns = this.getColumns().size() + other.getColumns().size();
            List<Column> allColumns = new ArrayList<>(maxNumberOfColumns);
            List<String> allTypes = new ArrayList<>(maxNumberOfColumns);
            Set<Column> seen = new HashSet<>();
            allColumns.addAll(this.columns);
            allTypes.addAll(this.columnTypes);
            Iterator<String> types = other.getColumnTypes().iterator();
            for (Column column : other) {
                String type = types.next();
                if (seen.add(column)) {
                    allColumns.add(column);
                    allTypes.add(type);
                }
            }
            boolean fts = this.hasFullTextSearchScores() || other.hasFullTextSearchScores();
            return new ResultColumns(allColumns, allTypes, fts, this);
        }
    }

    @ThreadSafe
    static class ScanQueryContext extends QueryContext {

        protected final Map<PlanNode, Columns> columnsByPlanNode;

        protected ScanQueryContext( ExecutionContext context,
                                    RepositoryCache repositoryCache,
                                    Set<String> workspaceNames,
                                    Map<String, NodeCache> overriddenNodeCachesByWorkspaceName,
                                    Schemata schemata,
                                    RepositoryIndexes indexDefns,
                                    NodeTypes nodeTypes,
                                    BufferManager bufferManager,
                                    PlanHints hints,
                                    Problems problems,
                                    Map<String, Object> variables,
                                    Map<PlanNode, Columns> columnsByPlanNode ) {
            super(context, repositoryCache, workspaceNames, overriddenNodeCachesByWorkspaceName, schemata, indexDefns, nodeTypes,
                  bufferManager, hints, problems, variables);
            this.columnsByPlanNode = columnsByPlanNode;
        }

        /**
         * Add a {@link Columns} object for the given plan node.
         * 
         * @param node plan node; may not be null
         * @param columns the columns to be assocated with this plan node; may not be null
         */
        public void addColumnsFor( PlanNode node,
                                   Columns columns ) {
            columnsByPlanNode.put(node, columns);
        }

        /**
         * Get the {@link Columns} object for the given plan node.
         * 
         * @param node plan node; may not be null
         * @return the columns to be assocated with this plan node; may not be null
         */
        public Columns columnsFor( PlanNode node ) {
            return columnsByPlanNode.get(node);
        }

        @Override
        public ScanQueryContext with( Map<String, Object> variables ) {
            return new ScanQueryContext(context, repositoryCache, workspaceNames, overriddenNodeCachesByWorkspaceName, schemata,
                                        indexDefns, nodeTypes, bufferManager, hints, problems, variables, columnsByPlanNode);
        }

        @Override
        public ScanQueryContext with( PlanHints hints ) {
            return new ScanQueryContext(context, repositoryCache, workspaceNames, overriddenNodeCachesByWorkspaceName, schemata,
                                        indexDefns, nodeTypes, bufferManager, hints, problems, variables, columnsByPlanNode);
        }

        @Override
        public ScanQueryContext with( Problems problems ) {
            return new ScanQueryContext(context, repositoryCache, workspaceNames, overriddenNodeCachesByWorkspaceName, schemata,
                                        indexDefns, nodeTypes, bufferManager, hints, problems, variables, columnsByPlanNode);
        }

        @Override
        public ScanQueryContext with( Schemata schemata ) {
            return new ScanQueryContext(context, repositoryCache, workspaceNames, overriddenNodeCachesByWorkspaceName, schemata,
                                        indexDefns, nodeTypes, bufferManager, hints, problems, variables, columnsByPlanNode);
        }
    }
}
