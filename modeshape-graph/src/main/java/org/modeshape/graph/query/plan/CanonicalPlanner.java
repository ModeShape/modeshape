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
package org.modeshape.graph.query.plan;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.model.AllNodes;
import org.modeshape.graph.query.model.And;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.model.Constraint;
import org.modeshape.graph.query.model.FullTextSearch;
import org.modeshape.graph.query.model.Join;
import org.modeshape.graph.query.model.JoinType;
import org.modeshape.graph.query.model.Limit;
import org.modeshape.graph.query.model.NamedSelector;
import org.modeshape.graph.query.model.Ordering;
import org.modeshape.graph.query.model.Query;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.Selector;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.SetQuery;
import org.modeshape.graph.query.model.Source;
import org.modeshape.graph.query.model.Subquery;
import org.modeshape.graph.query.model.Visitable;
import org.modeshape.graph.query.model.Visitors;
import org.modeshape.graph.query.model.Visitors.WalkAllVisitor;
import org.modeshape.graph.query.plan.PlanNode.Property;
import org.modeshape.graph.query.plan.PlanNode.Type;
import org.modeshape.graph.query.validate.Schemata;
import org.modeshape.graph.query.validate.Validator;
import org.modeshape.graph.query.validate.Schemata.Table;
import org.modeshape.graph.query.validate.Schemata.View;

/**
 * The planner that produces a canonical query plan given a {@link QueryCommand query command}.
 * <p>
 * A canonical plan always has the same structure:
 * 
 * <pre>
 *       LIMIT       if row limit or offset are used
 *         |
 *      SORTING      if 'ORDER BY' is used
 *         |
 *     DUP_REMOVE    if 'SELECT DISTINCT' is used
 *         |
 *      PROJECT      with the list of columns being SELECTed
 *         |
 *       GROUP       if 'GROUP BY' is used
 *         |
 *      SELECT1
 *         |         One or more SELECT plan nodes that each have
 *      SELECT2      a single non-join constraint that are then all AND-ed
 *         |         together (see {@link #separateAndConstraints(Constraint, List)})
 *      SELECTn
 *         |
 *    SOURCE or JOIN     A single SOURCE or JOIN node, depending upon the query
 *              /  \
 *             /    \
 *           SOJ    SOJ    A SOURCE or JOIN node for the left and right side of the JOIN
 * </pre>
 * <p>
 * There leaves of the tree are always SOURCE nodes, so <i>conceptually</i> data always flows through this plan from the bottom
 * SOURCE nodes, is adjusted/filtered as it trickles up through the plan, and is then ready to be used by the caller as it emerges
 * from the top node of the plan.
 * </p>
 * <p>
 * This canonical plan, however, is later optimized and rearranged so that it performs faster.
 * </p>
 */
public class CanonicalPlanner implements Planner {

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.plan.Planner#createPlan(org.modeshape.graph.query.QueryContext,
     *      org.modeshape.graph.query.model.QueryCommand)
     */
    public PlanNode createPlan( QueryContext context,
                                QueryCommand query ) {
        PlanNode plan = null;
        if (query instanceof Query) {
            plan = createCanonicalPlan(context, (Query)query);
        } else {
            plan = createCanonicalPlan(context, (SetQuery)query);
        }
        return plan;
    }

    /**
     * Create a canonical query plan for the given query.
     * 
     * @param context the context in which the query is being planned
     * @param query the query to be planned
     * @return the root node of the plan tree representing the canonical plan
     */
    protected PlanNode createCanonicalPlan( QueryContext context,
                                            Query query ) {
        PlanNode plan = null;

        // Process the source of the query ...
        Map<SelectorName, Table> usedSources = new HashMap<SelectorName, Table>();
        plan = createPlanNode(context, query.source(), usedSources);

        // Attach criteria (on top) ...
        Map<String, Subquery> subqueriesByVariableName = new HashMap<String, Subquery>();
        plan = attachCriteria(context, plan, query.constraint(), subqueriesByVariableName);

        // Attach groupbys (on top) ...
        // plan = attachGrouping(context,plan,query.getGroupBy());

        // Attach the project ...
        plan = attachProject(context, plan, query.columns(), usedSources);

        // Attach duplicate removal ...
        if (query.isDistinct()) {
            plan = attachDuplicateRemoval(context, plan);
        }

        // Process the orderings and limits ...
        plan = attachSorting(context, plan, query.orderings());
        plan = attachLimits(context, plan, query.limits());

        // Now add in the subqueries as dependent joins, in reverse order ...
        plan = attachSubqueries(context, plan, subqueriesByVariableName);

        // Validate that all the parts of the query are resolvable ...
        validate(context, query, usedSources);

        // Now we need to validate all of the subqueries ...
        for (Subquery subquery : Visitors.subqueries(query, false)) {
            // Just do it by creating a plan, even though we aren't doing anything with these plans ...
            createPlan(context, subquery.query());
        }

        return plan;
    }

    /**
     * Validate the supplied query.
     * 
     * @param context the context in which the query is being planned
     * @param query the set query to be planned
     * @param usedSelectors the map of {@link SelectorName}s (aliases or names) used in the query.
     */
    protected void validate( QueryContext context,
                             QueryCommand query,
                             Map<SelectorName, Table> usedSelectors ) {
        // // Resolve everything ...
        // Visitors.visitAll(query, new Validator(context, usedSelectors));
        // Resolve everything (except subqueries) ...
        Validator validator = new Validator(context, usedSelectors);
        query.accept(new WalkAllVisitor(validator) {
            @Override
            protected void enqueue( Visitable objectToBeVisited ) {
                if (objectToBeVisited instanceof Subquery) return;
                super.enqueue(objectToBeVisited);
            }
        });
    }

    /**
     * Create a canonical query plan for the given set query.
     * 
     * @param context the context in which the query is being planned
     * @param query the set query to be planned
     * @return the root node of the plan tree representing the canonical plan
     */
    protected PlanNode createCanonicalPlan( QueryContext context,
                                            SetQuery query ) {
        // Process the left and right parts of the query ...
        PlanNode left = createPlan(context, query.left());
        PlanNode right = createPlan(context, query.right());

        // Wrap in a set operation node ...
        PlanNode plan = new PlanNode(Type.SET_OPERATION);
        plan.addChildren(left, right);
        plan.setProperty(Property.SET_OPERATION, query.operation());
        plan.setProperty(Property.SET_USE_ALL, query.isAll());

        // Process the orderings and limits ...
        plan = attachSorting(context, plan, query.orderings());
        plan = attachLimits(context, plan, query.limits());
        return plan;
    }

    /**
     * Create a JOIN or SOURCE node that contain the source information.
     * 
     * @param context the execution context
     * @param source the source to be processed; may not be null
     * @param usedSelectors the map of {@link SelectorName}s (aliases or names) used in the query.
     * @return the new plan; never null
     */
    protected PlanNode createPlanNode( QueryContext context,
                                       Source source,
                                       Map<SelectorName, Table> usedSelectors ) {
        if (source instanceof Selector) {
            // No join required ...
            assert source instanceof AllNodes || source instanceof NamedSelector;
            Selector selector = (Selector)source;
            PlanNode node = new PlanNode(Type.SOURCE);
            if (selector.hasAlias()) {
                node.addSelector(selector.alias());
                node.setProperty(Property.SOURCE_ALIAS, selector.alias());
                node.setProperty(Property.SOURCE_NAME, selector.name());
            } else {
                node.addSelector(selector.name());
                node.setProperty(Property.SOURCE_NAME, selector.name());
            }
            // Validate the source name and set the available columns ...
            Table table = context.getSchemata().getTable(selector.name());
            if (table != null) {
                if (table instanceof View) context.getHints().hasView = true;
                if (usedSelectors.put(selector.aliasOrName(), table) != null) {
                    // There was already a table with this alias or name ...
                }
                node.setProperty(Property.SOURCE_COLUMNS, table.getColumns());
            } else {
                context.getProblems().addError(GraphI18n.tableDoesNotExist, selector.name());
            }
            return node;
        }
        if (source instanceof Join) {
            Join join = (Join)source;
            // Set up new join node corresponding to this join predicate
            PlanNode node = new PlanNode(Type.JOIN);
            node.setProperty(Property.JOIN_TYPE, join.type());
            node.setProperty(Property.JOIN_ALGORITHM, JoinAlgorithm.NESTED_LOOP);
            node.setProperty(Property.JOIN_CONDITION, join.joinCondition());

            context.getHints().hasJoin = true;
            if (join.type() == JoinType.LEFT_OUTER) {
                context.getHints().hasOptionalJoin = true;
            }

            // Handle each child
            Source[] clauses = new Source[] {join.left(), join.right()};
            for (int i = 0; i < 2; i++) {
                PlanNode sourceNode = createPlanNode(context, clauses[i], usedSelectors);
                node.addLastChild(sourceNode);

                // Add selectors to the joinNode
                for (PlanNode child : node.getChildren()) {
                    node.addSelectors(child.getSelectors());
                }
            }
            return node;
        }
        // should not get here; if we do, somebody added a new type of source
        assert false;
        return null;
    }

    /**
     * Attach all criteria above the join nodes. The optimizer will push these criteria down to the appropriate source.
     * 
     * @param context the context in which the query is being planned
     * @param plan the existing plan, which joins all source groups
     * @param constraint the criteria or constraint from the query
     * @param subqueriesByVariableName the subqueries by variable name
     * @return the updated plan, or the existing plan if there were no constraints; never null
     */
    protected PlanNode attachCriteria( final QueryContext context,
                                       PlanNode plan,
                                       Constraint constraint,
                                       Map<String, Subquery> subqueriesByVariableName ) {
        if (constraint == null) return plan;
        context.getHints().hasCriteria = true;

        // Extract the list of Constraint objects that all must be satisfied ...
        LinkedList<Constraint> andableConstraints = new LinkedList<Constraint>();
        separateAndConstraints(constraint, andableConstraints);
        assert !andableConstraints.isEmpty();

        // For each of these constraints, create a criteria (SELECT) node above the supplied (JOIN or SOURCE) node.
        // Do this in reverse order so that the top-most SELECT node corresponds to the first constraint.
        while (!andableConstraints.isEmpty()) {
            Constraint criteria = andableConstraints.removeLast();

            // Replace any subqueries with bind variables ...
            criteria = PlanUtil.replaceSubqueriesWithBindVariables(context, criteria, subqueriesByVariableName);

            // Create the select node ...
            PlanNode criteriaNode = new PlanNode(Type.SELECT);
            criteriaNode.setProperty(Property.SELECT_CRITERIA, criteria);

            // Add selectors to the criteria node ...
            criteriaNode.addSelectors(Visitors.getSelectorsReferencedBy(criteria));

            // Is there at least one full-text search or subquery ...
            Visitors.visitAll(criteria, new Visitors.AbstractVisitor() {
                @Override
                public void visit( FullTextSearch obj ) {
                    context.getHints().hasFullTextSearch = true;
                }
            });

            criteriaNode.addFirstChild(plan);
            plan = criteriaNode;
        }

        if (!subqueriesByVariableName.isEmpty()) {
            context.getHints().hasSubqueries = true;

        }
        return plan;
    }

    /**
     * Walk the supplied constraint to extract a list of the constraints that can be AND-ed together. For example, given the
     * constraint tree ((C1 AND C2) AND (C3 OR C4)), this method would result in a list of three separate criteria: [C1,C2,(C3 OR
     * C4)]. The resulting <code>andConstraints</code> list will contain Constraint objects that all must be true.
     * 
     * @param constraint the input constraint
     * @param andableConstraints the collection into which all non-{@link And AND} constraints should be placed
     */
    protected void separateAndConstraints( Constraint constraint,
                                           List<Constraint> andableConstraints ) {
        if (constraint == null) return;
        assert andableConstraints != null;
        if (constraint instanceof And) {
            And and = (And)constraint;
            separateAndConstraints(and.left(), andableConstraints);
            separateAndConstraints(and.right(), andableConstraints);
        } else {
            andableConstraints.add(constraint);
        }
    }

    /**
     * Attach SORT node at top of tree. The SORT may be pushed down to a source (or sources) if possible by the optimizer.
     * 
     * @param context the context in which the query is being planned
     * @param plan the existing plan
     * @param orderings list of orderings from the query
     * @return the updated plan, or the existing plan if there were no orderings; never null
     */
    protected PlanNode attachSorting( QueryContext context,
                                      PlanNode plan,
                                      List<? extends Ordering> orderings ) {
        if (orderings.isEmpty()) return plan;
        PlanNode sortNode = new PlanNode(Type.SORT);

        context.getHints().hasSort = true;
        sortNode.setProperty(Property.SORT_ORDER_BY, orderings);
        for (Ordering ordering : orderings) {
            sortNode.addSelectors(Visitors.getSelectorsReferencedBy(ordering));
        }

        sortNode.addLastChild(plan);
        return sortNode;
    }

    /**
     * Attach a LIMIT node at the top of the plan tree.
     * 
     * @param context the context in which the query is being planned
     * @param plan the existing plan
     * @param limit the limit definition; may be null
     * @return the updated plan, or the existing plan if there were no limits
     */
    protected PlanNode attachLimits( QueryContext context,
                                     PlanNode plan,
                                     Limit limit ) {
        if (limit.isUnlimited()) return plan;
        context.getHints().hasLimit = true;
        PlanNode limitNode = new PlanNode(Type.LIMIT);

        boolean attach = false;
        if (limit.offset() != 0) {
            limitNode.setProperty(Property.LIMIT_OFFSET, limit.offset());
            attach = true;
        }
        if (!limit.isUnlimited()) {
            limitNode.setProperty(Property.LIMIT_COUNT, limit.rowLimit());
            attach = true;
        }
        if (attach) {
            limitNode.addLastChild(plan);
            plan = limitNode;
        }
        return plan;
    }

    /**
     * Attach a PROJECT node at the top of the plan tree.
     * 
     * @param context the context in which the query is being planned
     * @param plan the existing plan
     * @param columns the columns being projected; may be null
     * @param selectors the selectors keyed by their alias or name
     * @return the updated plan
     */
    protected PlanNode attachProject( QueryContext context,
                                      PlanNode plan,
                                      List<? extends Column> columns,
                                      Map<SelectorName, Table> selectors ) {
        PlanNode projectNode = new PlanNode(Type.PROJECT);

        List<Column> newColumns = new LinkedList<Column>();
        List<String> newTypes = new ArrayList<String>();
        if (columns == null || columns.isEmpty()) {
            // SELECT *, so find all of the columns that are available from all the sources ...
            for (Map.Entry<SelectorName, Table> entry : selectors.entrySet()) {
                SelectorName tableName = entry.getKey();
                Table table = entry.getValue();
                // Add the selector that is being used ...
                projectNode.addSelector(tableName);
                // Compute the columns from this selector ...
                allColumnsFor(table, tableName, newColumns, newTypes);
            }
        } else {
            // Add the selector used by each column ...
            for (Column column : columns) {
                SelectorName tableName = column.selectorName();
                // Add the selector that is being used ...
                projectNode.addSelector(tableName);

                // Verify that each column is available in the appropriate source ...
                Table table = selectors.get(tableName);
                if (table == null) {
                    context.getProblems().addError(GraphI18n.tableDoesNotExist, tableName);
                } else {
                    // Make sure that the column is in the table ...
                    String columnName = column.propertyName();
                    if ("*".equals(columnName)) {
                        // This is a 'SELECT *' on this source, but this source is one of multiple sources ...
                        allColumnsFor(table, tableName, newColumns, newTypes);
                    } else {
                        // This is a particular column, so add it ...
                        if (!newColumns.contains(column)) {
                            newColumns.add(column);
                            org.modeshape.graph.query.validate.Schemata.Column schemaColumn = table.getColumn(columnName);
                            if (schemaColumn != null) {
                                newTypes.add(schemaColumn.getPropertyType());
                            } else {
                                newTypes.add(context.getTypeSystem().getStringFactory().getTypeName());
                            }
                        }
                    }
                    boolean validateColumnExistance = context.getHints().validateColumnExistance && !table.hasExtraColumns();
                    if (table.getColumn(columnName) == null && validateColumnExistance && !"*".equals(columnName)) {
                        context.getProblems().addError(GraphI18n.columnDoesNotExistOnTable, columnName, tableName);
                    }
                }
            }
        }
        projectNode.setProperty(Property.PROJECT_COLUMNS, newColumns);
        projectNode.setProperty(Property.PROJECT_COLUMN_TYPES, newTypes);
        projectNode.addLastChild(plan);
        return projectNode;
    }

    protected void allColumnsFor( Table table,
                                  SelectorName tableName,
                                  List<Column> columns,
                                  List<String> columnTypes ) {
        // Compute the columns from this selector ...
        for (Schemata.Column column : table.getSelectAllColumns()) {
            String columnName = column.getName();
            String propertyName = columnName;
            Column newColumn = new Column(tableName, propertyName, columnName);
            if (!columns.contains(column)) {
                columns.add(newColumn);
                columnTypes.add(column.getPropertyType());
            }
        }
    }

    /**
     * Attach DUP_REMOVE node at top of tree. The DUP_REMOVE may be pushed down to a source (or sources) if possible by the
     * optimizer.
     * 
     * @param context the context in which the query is being planned
     * @param plan the existing plan
     * @return the updated plan
     */
    protected PlanNode attachDuplicateRemoval( QueryContext context,
                                               PlanNode plan ) {
        PlanNode dupNode = new PlanNode(Type.DUP_REMOVE);
        plan.setParent(dupNode);
        return dupNode;
    }

    /**
     * Attach plan nodes for each subquery, resulting with the first subquery at the top of the plan tree.
     * 
     * @param context the context in which the query is being planned
     * @param plan the existing plan
     * @param subqueriesByVariableName the queries by the variable name used in substitution
     * @return the updated plan, or the existing plan if there were no limits
     */
    protected PlanNode attachSubqueries( QueryContext context,
                                         PlanNode plan,
                                         Map<String, Subquery> subqueriesByVariableName ) {
        // Order the variable names in reverse order ...
        List<String> varNames = new ArrayList<String>(subqueriesByVariableName.keySet());
        Collections.sort(varNames);
        Collections.reverse(varNames);

        for (String varName : varNames) {
            Subquery subquery = subqueriesByVariableName.get(varName);
            // Plan out the subquery ...
            PlanNode subqueryNode = createPlan(context, subquery.query());
            setSubqueryVariableName(subqueryNode, varName);

            // Create a DEPENDENT_QUERY node, with the subquery on the LHS (so it is executed first) ...
            PlanNode depQuery = new PlanNode(Type.DEPENDENT_QUERY);
            depQuery.addChildren(subqueryNode, plan);
            depQuery.addSelectors(subqueryNode.getSelectors());
            depQuery.addSelectors(plan.getSelectors());
            plan = depQuery;
        }
        return plan;
    }

    protected void setSubqueryVariableName( PlanNode subqueryPlan,
                                            String varName ) {
        if (subqueryPlan.getType() != Type.DEPENDENT_QUERY) {
            subqueryPlan.setProperty(Property.VARIABLE_NAME, varName);
            return;
        }
        // Otherwise, this is a dependent query, and our subquery should be on the right (last child) ...
        setSubqueryVariableName(subqueryPlan.getLastChild(), varName);
    }
}
