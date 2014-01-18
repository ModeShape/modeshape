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
package org.modeshape.jcr.query.optimize;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.model.Column;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Property;
import org.modeshape.jcr.query.plan.PlanNode.Type;
import org.modeshape.jcr.query.plan.PlanUtil;

/**
 * This rule attempts to ensure the proper location of {@link Type#PROJECT} nodes. For example, every {@link Type#ACCESS} node
 * needs to have a PROJECT node below it and above any other nodes (e.g., {@link Type#SELECT} or {@link Type#SOURCE} nodes). This
 * rule ensures that the PROJECT exists, but it also attempts to reduce any unnecessary columns in existing PROJECT nodes.
 */
public class PushProjects implements OptimizerRule {

    public static final PushProjects INSTANCE = new PushProjects();

    @Override
    public PlanNode execute( QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {

        // Find all of the ACCESS nodes to make sure there is a PROJECT ...
        for (PlanNode access : plan.findAllAtOrBelow(Type.ACCESS)) {
            // Find the first node that is below any LIMIT, SORT, or DUP_REMOVE ...
            PlanNode parentOfProject = access;
            while (parentOfProject.isOneOf(Type.LIMIT, Type.SORT, Type.DUP_REMOVE)) {
                parentOfProject = parentOfProject.getParent();
            }

            // Is there already a PROJECT here ?
            assert parentOfProject.getChildCount() == 1; // should only have one child ...
            PlanNode child = parentOfProject.getFirstChild(); // should only have one child ...
            if (child.is(Type.PROJECT)) {
                // Check to see if there is a PROJECT above the access node ...
                PlanNode accessParent = access.getParent();
                if (accessParent == null || accessParent.isNot(Type.PROJECT)) continue;
                // Otherwise, the parent is a PROJECT, but there is another PROJECT above the ACCESS node.
                // Remove the lower PROJECT so the next code block moves the top PROJECT down below the ACCESS node ...
                assert accessParent.is(Type.PROJECT);
                child.extractFromParent();
                child = parentOfProject.getFirstChild();
            }

            // If the parent of the ACCESS node is a PROJECT, then we can simply move it to here ...
            PlanNode accessParent = access.getParent();
            if (accessParent != null && accessParent.is(Type.PROJECT)) {
                PlanNode project = accessParent;
                if (project == plan) {
                    // The PROJECT node is the root, so the ACCESS node will be the root ...
                    plan = access;
                    access.removeFromParent();
                } else {
                    project.extractFromParent();
                }
                child.insertAsParent(project);
                if (plan == access) break;

                // We need to make sure we have all of the columns needed for any ancestor ...
                List<Column> requiredColumns = PlanUtil.findRequiredColumns(context, project);
                List<String> requiredTypes = PlanUtil.findRequiredColumnTypes(context, requiredColumns, child);
                project.setProperty(Property.PROJECT_COLUMNS, requiredColumns);
                project.setProperty(Property.PROJECT_COLUMN_TYPES, requiredTypes);
                project.addSelectors(getSelectorsFor(requiredColumns));
                continue;
            }

            // There is no PROJECT, so find the columns that are required by the plan above this point ...
            List<Column> requiredColumns = PlanUtil.findRequiredColumns(context, child);
            List<String> requiredTypes = PlanUtil.findRequiredColumnTypes(context, requiredColumns, child);

            // And insert the PROJECT ...
            PlanNode projectNode = new PlanNode(Type.PROJECT);
            projectNode.setProperty(Property.PROJECT_COLUMNS, requiredColumns);
            projectNode.setProperty(Property.PROJECT_COLUMN_TYPES, requiredTypes);
            projectNode.addSelectors(getSelectorsFor(requiredColumns));
            child.insertAsParent(projectNode);
        }
        return plan;
    }

    protected Set<SelectorName> getSelectorsFor( List<Column> columns ) {
        Set<SelectorName> selectors = new HashSet<SelectorName>();
        for (Column column : columns) {
            selectors.add(column.selectorName());
        }
        return selectors;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
