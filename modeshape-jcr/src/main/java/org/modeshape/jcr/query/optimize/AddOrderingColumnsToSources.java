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
package org.modeshape.jcr.query.optimize;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.model.Column;
import org.modeshape.jcr.query.model.Ordering;
import org.modeshape.jcr.query.model.PropertyValue;
import org.modeshape.jcr.query.model.ReferenceValue;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.model.Visitor;
import org.modeshape.jcr.query.model.Visitors;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Property;
import org.modeshape.jcr.query.plan.PlanNode.Type;

/**
 * An {@link OptimizerRule} that adds any missing columns required by the ordering specifications to the SORT node's PROJECT, and
 * to the appropriate access nodes.
 */
public class AddOrderingColumnsToSources implements OptimizerRule {

    public static final AddOrderingColumnsToSources INSTANCE = new AddOrderingColumnsToSources();

    @Override
    public PlanNode execute( QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {
        final boolean includeSourceName = context.getHints().qualifyExpandedColumnNames;

        // For each of the SORT nodes ...
        for (PlanNode sortNode : plan.findAllAtOrBelow(Type.SORT)) {

            // Get the list of property and reference expressions that are used in the Ordering instances, using a visitor. Other
            // kinds of
            // dynamic operands in Ordering instances will not need to change the columns
            final Set<Column> sortColumns = new HashSet<Column>();
            Visitor columnVisitor = new Visitors.AbstractVisitor() {
                @Override
                public void visit( PropertyValue prop ) {
                    sortColumns.add(columnFor(prop.selectorName(), prop.getPropertyName(), includeSourceName));
                }

                @Override
                public void visit( ReferenceValue ref ) {
                    sortColumns.add(columnFor(ref.selectorName(), ref.getPropertyName(), includeSourceName));
                }
            };
            List<Object> orderBys = sortNode.getPropertyAsList(Property.SORT_ORDER_BY, Object.class);
            if (orderBys != null && !orderBys.isEmpty()) {
                for (Object ordering : orderBys) {
                    if (ordering instanceof Ordering) {
                        Visitors.visitAll((Ordering)ordering, columnVisitor);
                    }
                }
            }

            // Add each of the sort columns to the appropriate PROJECT nodes in the subtrees of this plan ...
            Set<Column> columnsOnlyForSort = new HashSet<Column>();
            for (Column sortColumn : sortColumns) {
                if (addSortColumn(context, sortNode, sortColumn)) columnsOnlyForSort.add(sortColumn);
            }

            // If any columns were added only for the sort, we need to insert a PROJECT without the sort-only column(s)...
            if (!columnsOnlyForSort.isEmpty()) {
                // Find the existing project below the sort ...
                PlanNode existingProject = sortNode.findAtOrBelow(Type.PROJECT);
                List<Column> columns = existingProject.getPropertyAsList(Property.PROJECT_COLUMNS, Column.class);
                List<String> types = existingProject.getPropertyAsList(Property.PROJECT_COLUMN_TYPES, String.class);
                columns = new ArrayList<Column>(columns);
                types = new ArrayList<String>(types);
                for (Column sortOnlyColumn : columnsOnlyForSort) {
                    int index = columns.indexOf(sortOnlyColumn);
                    if (index >= 0) {
                        columns.remove(index);
                        types.remove(index);
                    }
                }

                // Determine the minimum selectors ...
                Set<SelectorName> selectors = new HashSet<SelectorName>();
                for (Column column : columns) {
                    selectors.add(column.selectorName());
                }

                // Now create a new PROJECT that wraps the SORT to remove the column(s) needed by the SORT ...
                PlanNode newProject = new PlanNode(Type.PROJECT);
                newProject.addSelectors(selectors);
                newProject.setProperty(Property.PROJECT_COLUMNS, columns);
                newProject.setProperty(Property.PROJECT_COLUMN_TYPES, types);

                // And insert the new PROJECT node ...
                sortNode.insertAsParent(newProject);
                if (plan == sortNode) plan = newProject;
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
     * @param sortColumn the column required by the sort
     * @return true if the sort column was added, or false if it was already in the {@link Property#PROJECT_COLUMNS projected
     *         columns}
     */
    protected boolean addSortColumn( QueryContext context,
                                     PlanNode node,
                                     Column sortColumn ) {
        boolean added = false;
        if (node.getSelectors().contains(sortColumn.selectorName())) {
            // Get the existing projected columns ...
            List<Column> columns = node.getPropertyAsList(Property.PROJECT_COLUMNS, Column.class);
            List<String> types = node.getPropertyAsList(Property.PROJECT_COLUMN_TYPES, String.class);
            if (columns != null && addIfMissing(context, sortColumn, columns, types)) {
                node.setProperty(Property.PROJECT_COLUMNS, columns);
                node.setProperty(Property.PROJECT_COLUMN_TYPES, types);
                added = true;
            }
        }

        // Apply recursively ...
        for (PlanNode child : node) {
            addSortColumn(context, child, sortColumn);
        }

        if (node.is(Type.SORT)) {
            // Get the child node, which should be a PROJECT ...
            PlanNode child = node.findAtOrBelow(Type.PROJECT);
            if (child != null && !child.getSelectors().contains(sortColumn.selectorName())) {
                // Make sure the PROJECT includes the missing columns, even when the selector is not included ...
                List<Column> columns = child.getPropertyAsList(Property.PROJECT_COLUMNS, Column.class);
                List<String> types = child.getPropertyAsList(Property.PROJECT_COLUMN_TYPES, String.class);
                if (columns != null && addIfMissing(context, sortColumn, columns, types)) {
                    child.setProperty(Property.PROJECT_COLUMNS, columns);
                    child.setProperty(Property.PROJECT_COLUMN_TYPES, types);
                    added = true;
                }
            }
        }
        return added;
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
            if (!c.selectorName().equals(column.selectorName())) continue;
            String cName = c.getPropertyName();
            if (cName.equals(column.getPropertyName()) || cName.equals(column.getColumnName())) return false;
            cName = c.getColumnName();
            if (cName.equals(column.getPropertyName()) || cName.equals(column.getColumnName())) return false;
        }
        columns.add(column);
        columnTypes.add(context.getTypeSystem().getDefaultType());
        return true;
    }

    protected Column columnFor( SelectorName selector,
                                String property,
                                boolean includeSourceName ) {
        String columnName = includeSourceName ? selector.getString() + "." + property : property;
        return new Column(selector, property, columnName);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
