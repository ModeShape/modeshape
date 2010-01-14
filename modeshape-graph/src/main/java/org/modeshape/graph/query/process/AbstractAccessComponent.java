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
import java.util.List;
import org.modeshape.graph.Location;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.QueryResults.Columns;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.model.Constraint;
import org.modeshape.graph.query.model.Limit;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.plan.PlanNode;
import org.modeshape.graph.query.plan.PlanNode.Property;
import org.modeshape.graph.query.plan.PlanNode.Type;
import org.modeshape.graph.query.validate.Schemata;

/**
 * A reusable base class for {@link ProcessingComponent} implementations that does everything except obtain the correct
 * {@link Location} objects for the query results.
 */
public abstract class AbstractAccessComponent extends ProcessingComponent {

    protected final PlanNode accessNode;
    protected final SelectorName sourceName;
    protected final List<Column> projectedColumns;
    protected final List<Constraint> andedConstraints;
    protected final Limit limit;

    protected AbstractAccessComponent( QueryContext context,
                                       Columns columns,
                                       PlanNode accessNode ) {
        super(context, columns);
        this.accessNode = accessNode;

        // Find the table name; should be
        PlanNode source = accessNode.findAtOrBelow(Type.SOURCE);
        if (source != null) {
            this.sourceName = source.getProperty(Property.SOURCE_NAME, SelectorName.class);
            // if (!AllNodes.ALL_NODES_NAME.equals(this.sourceName)) {
            // throw new IllegalArgumentException();
            // }
        } else {
            throw new IllegalArgumentException();
        }

        // Find the project ...
        PlanNode project = accessNode.findAtOrBelow(Type.PROJECT);
        if (project != null) {
            List<Column> projectedColumns = project.getPropertyAsList(Property.PROJECT_COLUMNS, Column.class);
            if (projectedColumns != null) {
                this.projectedColumns = projectedColumns;
            } else {
                // Get the columns from the source columns ...
                List<Schemata.Column> schemataColumns = source.getPropertyAsList(Property.SOURCE_COLUMNS, Schemata.Column.class);
                this.projectedColumns = new ArrayList<Column>(schemataColumns.size());
                for (Schemata.Column schemataColumn : schemataColumns) {
                    String columnName = schemataColumn.getName();
                    // PropertyType type = schemataColumn.getPropertyType();
                    String propertyName = columnName;
                    Column column = new Column(sourceName, propertyName, columnName);
                    this.projectedColumns.add(column);
                }
            }
        } else {
            throw new IllegalArgumentException();
        }

        // Add the criteria ...
        List<Constraint> andedConstraints = null;
        for (PlanNode select : accessNode.findAllAtOrBelow(Type.SELECT)) {
            Constraint selectConstraint = select.getProperty(Property.SELECT_CRITERIA, Constraint.class);
            if (andedConstraints == null) andedConstraints = new ArrayList<Constraint>();
            andedConstraints.add(selectConstraint);
        }
        this.andedConstraints = andedConstraints != null ? andedConstraints : Collections.<Constraint>emptyList();

        // Find the limit ...
        Limit limit = Limit.NONE;
        PlanNode limitNode = accessNode.findAtOrBelow(Type.LIMIT);
        if (limitNode != null) {
            Integer count = limitNode.getProperty(Property.LIMIT_COUNT, Integer.class);
            if (count != null) limit = limit.withRowLimit(count.intValue());
            Integer offset = limitNode.getProperty(Property.LIMIT_OFFSET, Integer.class);
            if (offset != null) limit = limit.withOffset(offset.intValue());
        }
        this.limit = limit;
    }

}
