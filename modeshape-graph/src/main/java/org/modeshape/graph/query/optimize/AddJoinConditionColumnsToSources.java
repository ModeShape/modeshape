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
package org.modeshape.graph.query.optimize;

import java.util.LinkedList;
import java.util.List;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.model.EquiJoinCondition;
import org.modeshape.graph.query.model.JoinCondition;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.plan.PlanNode;
import org.modeshape.graph.query.plan.PlanNode.Property;
import org.modeshape.graph.query.plan.PlanNode.Type;

/**
 * An {@link OptimizerRule} that adds any missing columns required by the join conditions to the appropriate join source.
 */
public class AddJoinConditionColumnsToSources implements OptimizerRule {

    public static final AddJoinConditionColumnsToSources INSTANCE = new AddJoinConditionColumnsToSources();

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.optimize.OptimizerRule#execute(org.modeshape.graph.query.QueryContext,
     *      org.modeshape.graph.query.plan.PlanNode, java.util.LinkedList)
     */
    public PlanNode execute( QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {
        // For each of the JOIN nodes ...
        for (PlanNode joinNode : plan.findAllAtOrBelow(Type.JOIN)) {
            JoinCondition condition = joinNode.getProperty(Property.JOIN_CONDITION, JoinCondition.class);
            if (condition instanceof EquiJoinCondition) {
                EquiJoinCondition equiJoinCondition = (EquiJoinCondition)condition;
                SelectorName selector1 = equiJoinCondition.selector1Name();
                Column joinColumn1 = columnFor(equiJoinCondition.selector1Name(), equiJoinCondition.property1Name());
                Column joinColumn2 = columnFor(equiJoinCondition.selector2Name(), equiJoinCondition.property2Name());

                // Figure out which side of the join condition goes with which side of the plan nodes ...
                PlanNode left = joinNode.getFirstChild();
                PlanNode right = joinNode.getLastChild();
                if (left.getSelectors().contains(selector1)) {
                    addEquiJoinColumn(context, left, joinColumn1);
                    addEquiJoinColumn(context, right, joinColumn2);
                } else {
                    addEquiJoinColumn(context, left, joinColumn2);
                    addEquiJoinColumn(context, right, joinColumn1);
                }
            }

        }
        return plan;
    }

    /**
     * Make sure that the supplied column is included in the {@link Property#PROJECT_COLUMNS projected columns} on the supplied
     * plan node or its children.
     * 
     * @param context the query context; may not be null
     * @param node the query plan node
     * @param joinColumn the column required by the join
     */
    protected void addEquiJoinColumn( QueryContext context,
                                      PlanNode node,
                                      Column joinColumn ) {
        if (node.getSelectors().contains(joinColumn.selectorName())) {
            // Get the existing projected columns ...
            List<Column> columns = node.getPropertyAsList(Property.PROJECT_COLUMNS, Column.class);
            List<String> types = node.getPropertyAsList(Property.PROJECT_COLUMN_TYPES, String.class);
            if (columns != null && addIfMissing(context, joinColumn, columns, types)) {
                node.setProperty(Property.PROJECT_COLUMNS, columns);
                node.setProperty(Property.PROJECT_COLUMN_TYPES, types);
            }
        }

        // Apply recursively ...
        for (PlanNode child : node) {
            addEquiJoinColumn(context, child, joinColumn);
        }
    }

    /**
     * Check the supplied list of columns for an existing column that matches the supplied {@link Column}, and if none is found
     * add the supplied Column to the list and add an appropriate type.
     * 
     * @param context the query context
     * @param column the column that will be added if not already in the list; may not be null
     * @param columns the list of columns; may not be null
     * @param columnTypes the list of column types; may not be null
     * @return true if the column was added (i.e., the lists were modified), or false if the lists were not modified
     */
    protected boolean addIfMissing( QueryContext context,
                                    Column column,
                                    List<Column> columns,
                                    List<String> columnTypes ) {
        for (Column c : columns) {
            if (c.propertyName().equals(column.propertyName()) && c.selectorName().equals(column.selectorName())) return false;
        }
        columns.add(column);
        columnTypes.add(context.getTypeSystem().getStringFactory().getTypeName());
        return true;
    }

    protected Column columnFor( SelectorName selector,
                                String property ) {
        return new Column(selector, property, property);
    }

}
