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
import java.util.List;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.QueryResults.Location;
import org.modeshape.jcr.query.model.Column;
import org.modeshape.jcr.query.model.Constraint;
import org.modeshape.jcr.query.model.Limit;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Property;
import org.modeshape.jcr.query.plan.PlanNode.Type;
import org.modeshape.jcr.query.validate.Schemata;

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
        assert this.sourceName != null;

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
        assert this.projectedColumns != null;

        // Add the criteria ...
        List<Constraint> andedConstraints = null;
        for (PlanNode select : accessNode.findAllAtOrBelow(Type.SELECT)) {
            Constraint selectConstraint = select.getProperty(Property.SELECT_CRITERIA, Constraint.class);
            if (andedConstraints == null) andedConstraints = new ArrayList<Constraint>();
            andedConstraints.add(selectConstraint);
        }
        this.andedConstraints = andedConstraints != null ? andedConstraints : Collections.<Constraint>emptyList();
        assert this.andedConstraints != null;

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
        assert this.limit != null;
    }

}
