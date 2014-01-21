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
package org.modeshape.jcr.query.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.QueryResults;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.QueryResults.Statistics;
import org.modeshape.jcr.query.model.ChildNodeJoinCondition;
import org.modeshape.jcr.query.model.Column;
import org.modeshape.jcr.query.model.Constraint;
import org.modeshape.jcr.query.model.EquiJoinCondition;
import org.modeshape.jcr.query.model.JoinCondition;
import org.modeshape.jcr.query.model.JoinType;
import org.modeshape.jcr.query.model.Limit;
import org.modeshape.jcr.query.model.Ordering;
import org.modeshape.jcr.query.model.QueryCommand;
import org.modeshape.jcr.query.model.SameNodeJoinCondition;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.model.SetQuery.Operation;
import org.modeshape.jcr.query.plan.JoinAlgorithm;
import org.modeshape.jcr.query.plan.PlanHints;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Property;
import org.modeshape.jcr.query.plan.PlanNode.Type;

/**
 * An abstract {@link Processor} implementation that builds a tree of {@link ProcessingComponent} objects to perform the different
 * parts of the query processing logic. Subclasses are required to only implement one method: the
 * {@link #createAccessComponent(QueryCommand, QueryContext, PlanNode, Columns, Object)} should create a ProcessorComponent object
 * that will perform the (low-level access) query described by the {@link PlanNode plan} given as a parameter.
 * 
 * @param <ProcessingContextType> the processing context object type
 */
public abstract class QueryProcessor<ProcessingContextType> implements Processor {

    @Override
    public QueryResults execute( QueryContext context,
                                 QueryCommand command,
                                 Statistics statistics,
                                 PlanNode plan ) {
        long nanos = System.nanoTime();
        Columns columns = null;
        List<Object[]> tuples = null;
        try {
            // Find the topmost PROJECT node and build the Columns ...
            PlanNode project = plan.findAtOrBelow(Type.PROJECT);
            assert project != null;
            List<Column> projectedColumns = project.getPropertyAsList(Property.PROJECT_COLUMNS, Column.class);
            assert projectedColumns != null;
            assert !projectedColumns.isEmpty();
            List<String> columnTypes = project.getPropertyAsList(Property.PROJECT_COLUMN_TYPES, String.class);
            assert columnTypes != null;
            assert columnTypes.size() == projectedColumns.size();
            columns = new QueryResultColumns(projectedColumns, columnTypes, context.getHints().hasFullTextSearch);

            if (context.getHints().planOnly) {
                tuples = Collections.emptyList();
            } else {
                // Create the processing context ...
                final ProcessingContextType processingContext = createProcessingContext(context);
                try {

                    // Go through the plan and create the corresponding ProcessingComponents ...
                    ProcessingComponent component = createComponent(command, context, plan, columns, processingContext);
                    long nanos2 = System.nanoTime();
                    statistics = statistics.withResultsFormulationTime(Math.abs(nanos2 - nanos));
                    nanos = nanos2;

                    if (component != null) {
                        // Now execute the component ...
                        columns = component.getColumns();
                        tuples = component.execute();
                    } else {
                        // There must have been an error or was cancelled ...
                        assert context.getProblems().hasErrors() || context.isCancelled();
                        tuples = Collections.emptyList();
                    }
                } finally {
                    // Always close the processing context !!!
                    closeProcessingContext(processingContext);
                }
            }

        } finally {
            statistics = statistics.withExecutionTime(Math.abs(System.nanoTime() - nanos));
        }
        assert tuples != null;
        final String planDesc = context.getHints().showPlan ? plan.getString() : null;
        return new org.modeshape.jcr.query.process.QueryResults(columns, statistics, tuples, context.getProblems(), planDesc);
    }

    /**
     * A method that can be overridden by subclasses to create a single context object used for all the access queries for a
     * single query.
     * 
     * @param queryContext the context in which the query is being executed; never null
     * @return the processing context object; may be null
     */
    protected ProcessingContextType createProcessingContext( QueryContext queryContext ) {
        return null;
    }

    /**
     * A method that can be overridden to close the supplied processing context. This method will always be called after a call to
     * {@link #createProcessingContext(QueryContext)}
     * 
     * @param processingContext the processing context in which the query is being executed; null if
     *        {@link #createProcessingContext(QueryContext)} returned null
     */
    protected void closeProcessingContext( ProcessingContextType processingContext ) {
        // do nothing ...
    }

    protected abstract boolean supportsPushDownExistConstraints();

    /**
     * Create the {@link ProcessingComponent} that processes a single {@link Type#ACCESS} branch of a query plan.
     * 
     * @param originalQuery the original query that is being executed; never null
     * @param context the context in which query is being evaluated; never null
     * @param accessNode the node in the query plan that represents the {@link Type#ACCESS} plan; never null
     * @param resultColumns the columns that are to be returned; never null
     * @param processingContext the processing context in which the query is being executed; null if
     *        {@link #createProcessingContext(QueryContext)} returned null
     * @return the processing component; may not be null
     */
    protected abstract ProcessingComponent createAccessComponent( QueryCommand originalQuery,
                                                                  QueryContext context,
                                                                  PlanNode accessNode,
                                                                  Columns resultColumns,
                                                                  ProcessingContextType processingContext );

    /**
     * Method that is called to build up the {@link ProcessingComponent} objects that correspond to the optimized query plan. This
     * method is called by {@link #execute(QueryContext, QueryCommand, Statistics, PlanNode)} for each of the various
     * {@link PlanNode} objects in the optimized query plan, and the method is actually recursive (since the optimized query plan
     * forms a tree). However, whenever this call structure reaches the {@link Type#ACCESS ACCESS} nodes in the query plan (which
     * each represents a separate atomic low-level query to the underlying system), the
     * {@link #createAccessComponent(QueryCommand, QueryContext, PlanNode, Columns, Object)} method is called. Subclasses should
     * create an appropriate ProcessingComponent implementation that performs this atomic low-level query.
     * 
     * @param originalQuery the original query that is being executed; never null
     * @param context the context in which query is being evaluated
     * @param node the plan node for which the ProcessingComponent is to be created
     * @param columns the definition of the result columns for this portion of the query
     * @param processingContext the processing context in which the query is being executed; null if
     *        {@link #createProcessingContext(QueryContext)} returned null
     * @return the processing component for this plan node; or null if there was an error recorded in the
     *         {@link QueryContext#getProblems() problems}
     */
    protected ProcessingComponent createComponent( QueryCommand originalQuery,
                                                   QueryContext context,
                                                   PlanNode node,
                                                   Columns columns,
                                                   ProcessingContextType processingContext ) {
        ProcessingComponent component = null;
        switch (node.getType()) {
            case ACCESS:
                // If the ACCESS node will not have results ...
                if (node.hasProperty(Property.ACCESS_NO_RESULTS)) {
                    component = new NoResultsComponent(context, columns);
                } else {
                    // Create the component to handle the ACCESS node ...
                    assert node.getChildCount() == 1;
                    component = createAccessComponent(originalQuery, context, node, columns, processingContext);
                    // // Don't do anything special with an access node at the moment ...
                    // component = createComponent(context, node.getFirstChild(), columns, analyzer);
                }
                break;
            case DUP_REMOVE:
                // Create the component under the DUP_REMOVE ...
                assert node.getChildCount() == 1;
                ProcessingComponent distinctDelegate = createComponent(originalQuery,
                                                                       context,
                                                                       node.getFirstChild(),
                                                                       columns,
                                                                       processingContext);
                component = new DistinctComponent(distinctDelegate);
                break;
            case GROUP:
                throw new UnsupportedOperationException();
            case JOIN:
                // Create the components under the JOIN ...
                assert node.getChildCount() == 2;
                PlanNode leftPlan = node.getFirstChild();
                PlanNode rightPlan = node.getLastChild();

                // Define the columns for each side, taken from the supplied columns ...
                Columns leftColumns = createColumnsFor(leftPlan, columns);
                Columns rightColumns = createColumnsFor(rightPlan, columns);

                // Query context for the join (must remove isExists condition).
                QueryContext joinQueryContext = context;
                if (context.getHints().isExistsQuery) {
                    // must not push down a LIMIT 1 condition to joins.
                    PlanHints joinPlanHints = context.getHints().clone();
                    joinPlanHints.isExistsQuery = false;
                    joinQueryContext = context.with(joinPlanHints);
                }

                ProcessingComponent left = createComponent(originalQuery,
                                                           joinQueryContext,
                                                           leftPlan,
                                                           leftColumns,
                                                           processingContext);
                ProcessingComponent right = createComponent(originalQuery,
                                                            joinQueryContext,
                                                            rightPlan,
                                                            rightColumns,
                                                            processingContext);
                // Create the join component ...
                JoinAlgorithm algorithm = node.getProperty(Property.JOIN_ALGORITHM, JoinAlgorithm.class);
                JoinType joinType = node.getProperty(Property.JOIN_TYPE, JoinType.class);
                JoinCondition joinCondition = node.getProperty(Property.JOIN_CONDITION, JoinCondition.class);
                switch (algorithm) {
                    case MERGE:
                        if (joinCondition instanceof SameNodeJoinCondition) {
                            SameNodeJoinCondition condition = (SameNodeJoinCondition)joinCondition;
                            component = new MergeJoinComponent(context, left, right, condition, joinType);
                        } else if (joinCondition instanceof ChildNodeJoinCondition) {
                            ChildNodeJoinCondition condition = (ChildNodeJoinCondition)joinCondition;
                            component = new MergeJoinComponent(context, left, right, condition, joinType);
                        } else if (joinCondition instanceof EquiJoinCondition) {
                            EquiJoinCondition condition = (EquiJoinCondition)joinCondition;
                            component = new MergeJoinComponent(context, left, right, condition, joinType);
                        } else {
                            assert false : "Unable to use merge algorithm with descendant node join conditions";
                            throw new UnsupportedOperationException();
                        }
                        break;
                    case NESTED_LOOP:
                        component = new NestedLoopJoinComponent(context, left, right, joinCondition, joinType);
                        break;
                }
                // For each Constraint object applied to the JOIN, simply create a SelectComponent on top ...
                List<Constraint> constraints = node.getPropertyAsList(Property.JOIN_CONSTRAINTS, Constraint.class);
                if (constraints != null) {
                    for (Constraint constraint : constraints) {
                        component = new SelectComponent(component, constraint, context.getVariables());
                    }
                }
                break;
            case LIMIT:
                // Create the component under the LIMIT ...
                assert node.getChildCount() == 1;
                ProcessingComponent delegate = createComponent(originalQuery,
                                                               context,
                                                               node.getFirstChild(),
                                                               columns,
                                                               processingContext);
                // Then create the limit component ...
                Integer rowLimit = node.getProperty(Property.LIMIT_COUNT, Integer.class);
                Integer offset = node.getProperty(Property.LIMIT_OFFSET, Integer.class);
                Limit limit = Limit.NONE;
                if (rowLimit != null) limit = limit.withRowLimit(rowLimit.intValue());
                if (offset != null) limit = limit.withOffset(offset.intValue());
                // And wrap the delegate
                if (!limit.isUnlimited()) component = new LimitComponent(delegate, limit);
                break;
            case NULL:
                component = new NoResultsComponent(context, columns);
                break;
            case PROJECT:
                // Create the component under the PROJECT ...
                assert node.getChildCount() == 1;
                ProcessingComponent projectDelegate = createComponent(originalQuery,
                                                                      context,
                                                                      node.getFirstChild(),
                                                                      columns,
                                                                      processingContext);
                // Then create the project component ...
                List<Column> projectedColumns = node.getPropertyAsList(Property.PROJECT_COLUMNS, Column.class);
                component = new ProjectComponent(projectDelegate, projectedColumns);
                break;
            case SELECT:
                // Create the component under the SELECT ...
                assert node.getChildCount() == 1;
                ProcessingComponent selectDelegate = createComponent(originalQuery,
                                                                     context,
                                                                     node.getFirstChild(),
                                                                     columns,
                                                                     processingContext);
                // Then create the select component ...
                Constraint constraint = node.getProperty(Property.SELECT_CRITERIA, Constraint.class);
                component = new SelectComponent(selectDelegate, constraint, context.getVariables());
                break;
            case SET_OPERATION:
                // Create the components under the SET_OPERATION ...
                List<ProcessingComponent> setDelegates = new LinkedList<ProcessingComponent>();
                for (PlanNode child : node) {
                    setDelegates.add(createComponent(originalQuery, context, child, columns, processingContext));
                }
                // Then create the select component ...
                Operation operation = node.getProperty(Property.SET_OPERATION, Operation.class);
                boolean all = node.getProperty(Property.SET_USE_ALL, Boolean.class);
                boolean alreadySorted = false; // ????
                switch (operation) {
                    case EXCEPT:
                        component = new ExceptComponent(context, columns, setDelegates, alreadySorted, all);
                        break;
                    case INTERSECT:
                        component = new IntersectComponent(context, columns, setDelegates, alreadySorted, all);
                        break;
                    case UNION:
                        component = new UnionComponent(context, columns, setDelegates, alreadySorted, all);
                        break;
                }
                break;
            case SORT:
                // Create the component under the SORT ...
                assert node.getChildCount() == 1;
                ProcessingComponent sortDelegate = createComponent(originalQuery,
                                                                   context,
                                                                   node.getFirstChild(),
                                                                   columns,
                                                                   processingContext);
                // Then create the sort component ...
                List<Object> orderBys = node.getPropertyAsList(Property.SORT_ORDER_BY, Object.class);
                if (orderBys.isEmpty()) {
                    component = sortDelegate;
                } else {
                    if (orderBys.get(0) instanceof Ordering) {
                        List<Ordering> orderings = new ArrayList<Ordering>(orderBys.size());
                        for (Object orderBy : orderBys) {
                            orderings.add((Ordering)orderBy);
                        }
                        // Determine the alias-to-name mappings for the selectors in the orderings ...
                        Map<SelectorName, SelectorName> sourceNamesByAlias = new HashMap<SelectorName, SelectorName>();
                        for (PlanNode source : node.findAllAtOrBelow(Type.SOURCE)) {
                            SelectorName name = source.getProperty(Property.SOURCE_NAME, SelectorName.class);
                            SelectorName alias = source.getProperty(Property.SOURCE_ALIAS, SelectorName.class);
                            if (alias != null) sourceNamesByAlias.put(alias, name);
                        }
                        // Now create the sorting component ...
                        component = new SortValuesComponent(sortDelegate, orderings, sourceNamesByAlias);
                    } else {
                        // Order by the location(s) because it's before a merge-join ...
                        component = new SortLocationsComponent(sortDelegate);
                    }
                }
                break;
            case DEPENDENT_QUERY:
                // Create the components under the JOIN ...
                assert node.getChildCount() == 2;
                leftPlan = node.getFirstChild();
                rightPlan = node.getLastChild();

                // Define the columns for each side, taken from the supplied columns ...
                leftColumns = createColumnsFor(leftPlan, columns);
                rightColumns = createColumnsFor(rightPlan, columns);

                left = createComponent(originalQuery, context, leftPlan, leftColumns, processingContext);
                right = createComponent(originalQuery, context, rightPlan, rightColumns, processingContext);

                // Look for a variable name on the left and right plans ...
                String leftVariableName = leftPlan.getProperty(Property.VARIABLE_NAME, String.class);
                String rightVariableName = rightPlan.getProperty(Property.VARIABLE_NAME, String.class);
                component = new DependentQueryComponent(context, left, right, leftVariableName, rightVariableName);
                break;
            case SOURCE:
                assert false : "Source nodes should always be below ACCESS nodes by the time a plan is executed";
                throw new UnsupportedOperationException();
        }
        assert component != null;
        return component;
    }

    protected Columns createColumnsFor( PlanNode node,
                                        Columns projectedColumns ) {
        PlanNode project = node.findAtOrBelow(Type.PROJECT);
        assert project != null;
        List<Column> columns = project.getPropertyAsList(Property.PROJECT_COLUMNS, Column.class);
        List<String> columnTypes = project.getPropertyAsList(Property.PROJECT_COLUMN_TYPES, String.class);
        assert columns != null;
        assert !columns.isEmpty();
        assert columnTypes != null;
        assert columnTypes.size() == columns.size();
        return new QueryResultColumns(columns, columnTypes, projectedColumns.hasFullTextSearchScores());
    }
}
