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
package org.jboss.dna.graph.query.plan;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.model.AllNodes;
import org.jboss.dna.graph.query.model.And;
import org.jboss.dna.graph.query.model.Column;
import org.jboss.dna.graph.query.model.Constraint;
import org.jboss.dna.graph.query.model.FullTextSearch;
import org.jboss.dna.graph.query.model.Join;
import org.jboss.dna.graph.query.model.JoinType;
import org.jboss.dna.graph.query.model.Limit;
import org.jboss.dna.graph.query.model.NamedSelector;
import org.jboss.dna.graph.query.model.Ordering;
import org.jboss.dna.graph.query.model.Query;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.model.Selector;
import org.jboss.dna.graph.query.model.SelectorName;
import org.jboss.dna.graph.query.model.SetQuery;
import org.jboss.dna.graph.query.model.Source;
import org.jboss.dna.graph.query.model.Visitors;
import org.jboss.dna.graph.query.plan.PlanNode.Property;
import org.jboss.dna.graph.query.plan.PlanNode.Type;
import org.jboss.dna.graph.query.validate.Schemata;
import org.jboss.dna.graph.query.validate.Schemata.Table;

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
 * This canonical plan, however, is optimized and rearranged so that it performs faster.
 * </p>
 */
public class CanonicalPlanner implements Planner {

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.plan.Planner#createPlan(org.jboss.dna.graph.query.QueryContext,
     *      org.jboss.dna.graph.query.model.QueryCommand)
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
        plan = createPlanNode(context, query.getSource(), usedSources);

        // Attach criteria (on top) ...
        plan = attachCriteria(context, plan, query.getConstraint());

        // Attach the project ...
        plan = attachProject(context, plan, query.getColumns(), usedSources);

        // Attach duplicate removal ...
        if (query.isDistinct()) {
            plan = attachDuplicateRemoval(context, plan);
        }

        // Process the orderings and limits ...
        plan = attachSorting(context, plan, query.getOrderings());
        plan = attachLimits(context, plan, query.getLimits());
        return plan;
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
        PlanNode left = createPlan(context, query.getLeft());
        PlanNode right = createPlan(context, query.getRight());

        // Wrap in a set operation node ...
        PlanNode plan = new PlanNode(Type.SET_OPERATION);
        plan.addChildren(left, right);
        plan.setProperty(Property.SET_OPERATION, query.getOperation());
        plan.setProperty(Property.SET_USE_ALL, query.isAll());

        // Process the orderings and limits ...
        plan = attachSorting(context, plan, query.getOrderings());
        plan = attachLimits(context, plan, query.getLimits());
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
                node.addSelector(selector.getAlias());
                node.setProperty(Property.SOURCE_ALIAS, selector.getAlias());
                node.setProperty(Property.SOURCE_NAME, selector.getName());
            } else {
                node.addSelector(selector.getName());
                node.setProperty(Property.SOURCE_NAME, selector.getName());
            }
            // Validate the source name and set the available columns ...
            Table table = context.getSchemata().getTable(selector.getName());
            if (table == null) {
                context.getProblems().addError(GraphI18n.tableDoesNotExist,
                                               selector.getName().getString(context.getExecutionContext()));
            } else {
                if (usedSelectors.put(selector.getAliasOrName(), table) != null) {
                    // There was already a table with this alias or name ...
                }
                node.setProperty(Property.SOURCE_COLUMNS, table.getColumns());
            }
            return node;
        }
        if (source instanceof Join) {
            Join join = (Join)source;
            // Set up new join node corresponding to this join predicate
            PlanNode node = new PlanNode(Type.JOIN);
            node.setProperty(Property.JOIN_TYPE, join.getType());
            node.setProperty(Property.JOIN_ALGORITHM, JoinAlgorithm.NESTED_LOOP);
            node.setProperty(Property.JOIN_CONDITION, join.getJoinCondition());

            context.getHints().hasJoin = true;
            if (join.getType() == JoinType.LEFT_OUTER) {
                context.getHints().hasOptionalJoin = true;
            }

            // Handle each child
            Source[] clauses = new Source[] {join.getLeft(), join.getRight()};
            for (int i = 0; i < 2; i++) {
                node.addLastChild(createPlanNode(context, clauses[i], usedSelectors));

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
     * @return the updated plan, or the existing plan if there were no constraints; never null
     */
    protected PlanNode attachCriteria( final QueryContext context,
                                       PlanNode plan,
                                       Constraint constraint ) {
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
            // Create the select node ...
            PlanNode criteriaNode = new PlanNode(Type.SELECT);
            criteriaNode.setProperty(Property.SELECT_CRITERIA, criteria);

            // Add selectors to the criteria node ...
            criteriaNode.addSelectors(Visitors.getSelectorsReferencedBy(criteria));

            // Is a full-text search of some kind included ...
            Visitors.visitAll(criteria, new Visitors.AbstractVisitor() {
                @Override
                public void visit( FullTextSearch obj ) {
                    context.getHints().hasFullTextSearch = true;
                }
            });

            plan = criteriaNode;
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
            separateAndConstraints(and.getLeft(), andableConstraints);
            separateAndConstraints(and.getRight(), andableConstraints);
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
                                      List<Ordering> orderings ) {
        if (orderings.isEmpty()) return plan;
        PlanNode sortNode = new PlanNode(Type.SORT);

        context.getHints().hasSort = true;
        sortNode.setProperty(Property.SORT_ORDER_BY, orderings);

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
        if (limit.getOffset() != 0) {
            limitNode.setProperty(Property.LIMIT_COUNT, limit.getOffset());
            attach = true;
        }
        if (!limit.isUnlimited()) {
            limitNode.setProperty(Property.LIMIT_OFFSET, limit.getRowLimit());
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
                                      List<Column> columns,
                                      Map<SelectorName, Table> selectors ) {
        if (columns == null) columns = Collections.emptyList();
        PlanNode projectNode = new PlanNode(Type.PROJECT);

        if (columns.isEmpty()) {
            columns = new LinkedList<Column>();
            // SELECT *, so find all of the columns that are available from all the sources ...
            NameFactory nameFactory = context.getExecutionContext().getValueFactories().getNameFactory();
            for (Table table : selectors.values()) {
                // Add the selector that is being used ...
                projectNode.addSelector(table.getName());
                // Compute the columns from this selector ...
                for (Schemata.Column column : table.getColumns()) {
                    String columnName = column.getName();
                    Name propertyName = nameFactory.create(columnName);
                    columns.add(new Column(table.getName(), propertyName, columnName));
                }
            }
        } else {
            // Add the selector used by each column ...
            for (Column column : columns) {
                SelectorName tableName = column.getSelectorName();
                // Add the selector that is being used ...
                projectNode.addSelector(tableName);

                // Verify that each column is available in the appropriate source ...
                Table table = selectors.get(tableName);
                if (table == null) {
                    context.getProblems().addError(GraphI18n.tableDoesNotExist,
                                                   tableName.getString(context.getExecutionContext()));
                } else {
                    // Make sure that the column is in the table ...
                    if (table.getColumn(column.getColumnName()) == null) {
                        context.getProblems().addError(GraphI18n.columnDoesNotExistOnTable,
                                                       column.getColumnName(),
                                                       tableName.getString(context.getExecutionContext()));
                    }
                }
            }
        }
        projectNode.setProperty(Property.PROJECT_COLUMNS, columns);
        projectNode.addLastChild(plan);
        return projectNode;
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

}
