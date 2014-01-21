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
import org.modeshape.jcr.query.model.JoinType;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Property;
import org.modeshape.jcr.query.plan.PlanNode.Type;

/**
 * An {@link OptimizerRule optimizer rule} that converts {@link JoinType#RIGHT_OUTER right outer joins} into
 * {@link JoinType#LEFT_OUTER left outer joins}.
 */
@Immutable
public class RightOuterToLeftOuterJoins implements OptimizerRule {

    public static final RightOuterToLeftOuterJoins INSTANCE = new RightOuterToLeftOuterJoins();

    @Override
    public PlanNode execute( QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {
        // For each of the JOIN nodes ...
        for (PlanNode joinNode : plan.findAllAtOrBelow(Type.JOIN)) {
            if (JoinType.RIGHT_OUTER == joinNode.getProperty(Property.JOIN_TYPE, JoinType.class)) {
                // Swap the information ...
                PlanNode left = joinNode.getFirstChild();
                left.removeFromParent(); // right is now the first child ...
                left.setParent(joinNode);
                joinNode.setProperty(Property.JOIN_TYPE, JoinType.LEFT_OUTER);
                // None of the Constraints or JoinCondition need to be changed (they refer to named selectors) ...
            }
        }
        return plan;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
