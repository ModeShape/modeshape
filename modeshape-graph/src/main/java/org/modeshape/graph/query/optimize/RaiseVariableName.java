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
import net.jcip.annotations.Immutable;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.plan.PlanNode;
import org.modeshape.graph.query.plan.PlanNode.Property;
import org.modeshape.graph.query.plan.PlanNode.Traversal;
import org.modeshape.graph.query.plan.PlanNode.Type;

/**
 * An {@link OptimizerRule optimizer rule} that moves up higher in the plan any {@link Property#VARIABLE_NAME variable name}
 * property to the node immediately under a {@link Type#DEPENDENT_QUERY dependent query} node.
 */
@Immutable
public class RaiseVariableName implements OptimizerRule {

    public static final RaiseVariableName INSTANCE = new RaiseVariableName();

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.optimize.OptimizerRule#execute(org.modeshape.graph.query.QueryContext,
     *      org.modeshape.graph.query.plan.PlanNode, java.util.LinkedList)
     */
    public PlanNode execute( QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {
        for (PlanNode depQuery : plan.findAllAtOrBelow(Traversal.PRE_ORDER, Type.DEPENDENT_QUERY)) {
            // Check the left ...
            PlanNode left = depQuery.getFirstChild();
            raiseVariableName(left);

            // Check the right ...
            PlanNode right = depQuery.getLastChild();
            raiseVariableName(right);
        }
        return plan;
    }

    protected void raiseVariableName( PlanNode node ) {
        if (node.getType() != Type.DEPENDENT_QUERY) {
            String variableName = removeVariableName(node);
            if (variableName != null) {
                node.setProperty(Property.VARIABLE_NAME, variableName);
            }
        }
    }

    protected String removeVariableName( PlanNode node ) {
        if (node == null) return null;
        String variableName = node.getProperty(Property.VARIABLE_NAME, String.class);
        if (variableName != null) {
            node.removeProperty(Property.VARIABLE_NAME);
            return variableName;
        }
        // Look for it in the left side ...
        return removeVariableName(node.getFirstChild());
    }
}
