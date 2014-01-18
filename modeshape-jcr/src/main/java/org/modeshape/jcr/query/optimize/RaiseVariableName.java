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

import java.util.LinkedList;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Property;
import org.modeshape.jcr.query.plan.PlanNode.Traversal;
import org.modeshape.jcr.query.plan.PlanNode.Type;

/**
 * An {@link OptimizerRule optimizer rule} that moves up higher in the plan any {@link Property#VARIABLE_NAME variable name}
 * property to the node immediately under a {@link Type#DEPENDENT_QUERY dependent query} node.
 */
@Immutable
public class RaiseVariableName implements OptimizerRule {

    public static final RaiseVariableName INSTANCE = new RaiseVariableName();

    @Override
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

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
