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

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.plan.PlanNode;
import org.modeshape.graph.query.plan.PlanUtil;
import org.modeshape.graph.query.plan.PlanNode.Property;
import org.modeshape.graph.query.plan.PlanNode.Type;

/**
 * This rule attempts to ensure the proper location of {@link Type#PROJECT} nodes. For example, every {@link Type#ACCESS} node
 * needs to have a PROJECT node below it and above any other nodes (e.g., {@link Type#SELECT} or {@link Type#SOURCE} nodes). This
 * rule ensures that the PROJECT exists, but it also attempts to reduce any unnecessary columns in existing PROJECT nodes.
 */
public class PushProjects implements OptimizerRule {

    public static final PushProjects INSTANCE = new PushProjects();

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.optimize.OptimizerRule#execute(org.modeshape.graph.query.QueryContext,
     *      org.modeshape.graph.query.plan.PlanNode, java.util.LinkedList)
     */
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

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
