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
package org.modeshape.graph.query.process;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.QueryResults;
import org.modeshape.graph.query.QueryResults.Columns;
import org.modeshape.graph.query.QueryResults.Statistics;
import org.modeshape.graph.query.model.ChildNodeJoinCondition;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.model.Constraint;
import org.modeshape.graph.query.model.EquiJoinCondition;
import org.modeshape.graph.query.model.JoinCondition;
import org.modeshape.graph.query.model.JoinType;
import org.modeshape.graph.query.model.Limit;
import org.modeshape.graph.query.model.Ordering;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.SameNodeJoinCondition;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.SetQuery.Operation;
import org.modeshape.graph.query.plan.JoinAlgorithm;
import org.modeshape.graph.query.plan.PlanNode;
import org.modeshape.graph.query.plan.PlanNode.Property;
import org.modeshape.graph.query.plan.PlanNode.Type;
import org.modeshape.graph.query.process.SelectComponent.Analyzer;

/**
 * An abstract {@link Processor} implementation that builds a tree of {@link ProcessingComponent} objects to perform the different
 * parts of the query processing logic. Subclasses are required to only implement one method: the
 * {@link #createAccessComponent(QueryCommand,QueryContext, PlanNode, Columns, Analyzer)} should create a ProcessorComponent
 * object that will perform the (low-level access) query described by the {@link PlanNode plan} given as a parameter.
 */
public abstract class QueryProcessor implements Processor {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.process.Processor#execute(org.modeshape.graph.query.QueryContext,
     *      org.modeshape.graph.query.model.QueryCommand, org.modeshape.graph.query.QueryResults.Statistics,
     *      org.modeshape.graph.query.plan.PlanNode)
     */
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

            // Go through the plan and create the corresponding ProcessingComponents ...
            Analyzer analyzer = createAnalyzer(context);
            ProcessingComponent component = createComponent(command, context, plan, columns, analyzer);
            long nanos2 = System.nanoTime();
            statistics = statistics.withResultsFormulationTime(nanos2 - nanos);
            nanos = nanos2;

            if (component != null) {
                // Now execute the component ...
                try {
                    columns = component.getColumns();
                    preExecute(context);
                    tuples = component.execute();
                } finally {
                    postExecute(context);
                }
            } else {
                // There must have been an error ...
                assert context.getProblems().hasErrors();
                tuples = Collections.emptyList();
            }

        } finally {
            statistics = statistics.withExecutionTime(System.nanoTime() - nanos);
        }
        assert tuples != null;
        final String planDesc = context.getHints().showPlan ? plan.getString() : null;
        return new org.modeshape.graph.query.process.QueryResults(columns, statistics, tuples, context.getProblems(), planDesc);
    }

    /**
     * A method that can be overridden when a hook is required immediately before the top-level {@link ProcessingComponent} is
     * executed. By default, this method does nothing.
     * 
     * @param context the context in which the query is being executed; may not be null
     */
    protected void preExecute( QueryContext context ) {
        // do nothing ...
    }

    /**
     * A method that can be overridden when a hook is required immediately after the top-level {@link ProcessingComponent} is
     * executed and all processing has been completed, even if there was an error. By default, this method does nothing.
     * 
     * @param context the context in which the query is being executed; may not be null
     */
    protected void postExecute( QueryContext context ) {
        // do nothing ...
    }

    /**
     * Create an {@link Analyzer} implementation that should be used by the non-access {@link ProcessingComponent}s that evaluate
     * criteria. By default, this method returns null, which means that any criteria evaluation will likely be pushed down under
     * an {@link Type#ACCESS ACCESS} node (and thus handled by an
     * {@link #createAccessComponent(QueryCommand,QueryContext, PlanNode, Columns, Analyzer) access component}.
     * <p>
     * However, for more simple access components that are not capable of handling joins and other non-trivial criteria, simply
     * return an Analyzer implementation that implements the methods using the source.
     * </p>
     * 
     * @param context the context in which query is being evaluated
     * @return the analyzer, or null if the ProcessingComponent objects that evaluate criteria should use a best-effort approach
     */
    protected Analyzer createAnalyzer( QueryContext context ) {
        return null;
    }

    /**
     * Create the {@link ProcessingComponent} that processes a single {@link Type#ACCESS} branch of a query plan.
     * 
     * @param originalQuery the original query that is being executed; never null
     * @param context the context in which query is being evaluated; never null
     * @param accessNode the node in the query plan that represents the {@link Type#ACCESS} plan; never null
     * @param resultColumns the columns that are to be returned; never null
     * @param analyzer the criteria analyzer; never null
     * @return the processing component; may not be null
     */
    protected abstract ProcessingComponent createAccessComponent( QueryCommand originalQuery,
                                                                  QueryContext context,
                                                                  PlanNode accessNode,
                                                                  Columns resultColumns,
                                                                  Analyzer analyzer );

    /**
     * Method that is called to build up the {@link ProcessingComponent} objects that correspond to the optimized query plan. This
     * method is called by {@link #execute(QueryContext, QueryCommand, Statistics, PlanNode)} for each of the various
     * {@link PlanNode} objects in the optimized query plan, and the method is actually recursive (since the optimized query plan
     * forms a tree). However, whenever this call structure reaches the {@link Type#ACCESS ACCESS} nodes in the query plan (which
     * each represents a separate atomic low-level query to the underlying system), the
     * {@link #createAccessComponent(QueryCommand,QueryContext, PlanNode, Columns, Analyzer)} method is called. Subclasses should
     * create an appropriate ProcessingComponent implementation that performs this atomic low-level query.
     * 
     * @param originalQuery the original query that is being executed; never null
     * @param context the context in which query is being evaluated
     * @param node the plan node for which the ProcessingComponent is to be created
     * @param columns the definition of the result columns for this portion of the query
     * @param analyzer the analyzer (returned from {@link #createAnalyzer(QueryContext)}) that should be used on the components
     *        that evaluate criteria; may be null if a best-effort should be made for the evaluation
     * @return the processing component for this plan node; or null if there was an error recorded in the
     *         {@link QueryContext#getProblems() problems}
     */
    protected ProcessingComponent createComponent( QueryCommand originalQuery,
                                                   QueryContext context,
                                                   PlanNode node,
                                                   Columns columns,
                                                   Analyzer analyzer ) {
        ProcessingComponent component = null;
        switch (node.getType()) {
            case ACCESS:
                // If the ACCESS node will not have results ...
                if (node.hasProperty(Property.ACCESS_NO_RESULTS)) {
                    component = new NoResultsComponent(context, columns);
                } else {
                    // Create the component to handle the ACCESS node ...
                    assert node.getChildCount() == 1;
                    component = createAccessComponent(originalQuery, context, node, columns, analyzer);
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
                                                                       analyzer);
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

                ProcessingComponent left = createComponent(originalQuery, context, leftPlan, leftColumns, analyzer);
                ProcessingComponent right = createComponent(originalQuery, context, rightPlan, rightColumns, analyzer);
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
                ProcessingComponent limitDelegate = createComponent(originalQuery,
                                                                    context,
                                                                    node.getFirstChild(),
                                                                    columns,
                                                                    analyzer);
                // Then create the limit component ...
                Integer rowLimit = node.getProperty(Property.LIMIT_COUNT, Integer.class);
                Integer offset = node.getProperty(Property.LIMIT_OFFSET, Integer.class);
                Limit limit = Limit.NONE;
                if (rowLimit != null) limit = limit.withRowLimit(rowLimit.intValue());
                if (offset != null) limit = limit.withOffset(offset.intValue());
                component = new LimitComponent(limitDelegate, limit);
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
                                                                      analyzer);
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
                                                                     analyzer);
                // Then create the select component ...
                Constraint constraint = node.getProperty(Property.SELECT_CRITERIA, Constraint.class);
                component = new SelectComponent(selectDelegate, constraint, context.getVariables(), analyzer);
                break;
            case SET_OPERATION:
                // Create the components under the SET_OPERATION ...
                List<ProcessingComponent> setDelegates = new LinkedList<ProcessingComponent>();
                for (PlanNode child : node) {
                    setDelegates.add(createComponent(originalQuery, context, child, columns, analyzer));
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
                                                                   analyzer);
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

                left = createComponent(originalQuery, context, leftPlan, leftColumns, analyzer);
                right = createComponent(originalQuery, context, rightPlan, rightColumns, analyzer);

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
