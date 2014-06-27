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
import javax.jcr.query.qom.JoinCondition;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.model.ChildNodeJoinCondition;
import org.modeshape.jcr.query.model.DescendantNodeJoinCondition;
import org.modeshape.jcr.query.model.JoinType;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Property;
import org.modeshape.jcr.query.plan.PlanNode.Type;

/**
 * An optimization rule that ensure that the order of the left and right side of a JOIN match certain join criteria, including
 * {@link DescendantNodeJoinCondition} and {@link ChildNodeJoinCondition}.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class JoinOrder implements OptimizerRule {

    public static final JoinOrder INSTANCE = new JoinOrder();

    @Override
    public PlanNode execute( final QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {
        for (PlanNode join : plan.findAllAtOrBelow(Type.JOIN)) {
            boolean swapChildren = false;
            JoinCondition joinCondition = join.getProperty(Property.JOIN_CONDITION, JoinCondition.class);
            if (joinCondition instanceof DescendantNodeJoinCondition) {
                DescendantNodeJoinCondition condition = (DescendantNodeJoinCondition)joinCondition;
                SelectorName ancestorSelector = condition.ancestorSelectorName();
                // The ancestor needs to be on the left side of the join ...
                swapChildren = !join.getFirstChild().getSelectors().contains(ancestorSelector);
            } else if (joinCondition instanceof ChildNodeJoinCondition) {
                ChildNodeJoinCondition condition = (ChildNodeJoinCondition)joinCondition;
                SelectorName parentSelector = condition.parentSelectorName();
                // The ancestor needs to be on the left side of the join ...
                swapChildren = !join.getFirstChild().getSelectors().contains(parentSelector);
            }

            JoinType joinType = join.getProperty(Property.JOIN_TYPE, JoinType.class);
            if (swapChildren) {
                PlanNode first = join.getFirstChild();
                first.removeFromParent();
                join.addLastChild(first);
                if (joinType == JoinType.LEFT_OUTER){
                    //we've reversed an outer join, so we need to change the join type
                    join.setProperty(Property.JOIN_TYPE, JoinType.RIGHT_OUTER);
                } else if (joinType == JoinType.RIGHT_OUTER) {
                    //we've reversed an outer join, so we need to change the join type
                    join.setProperty(Property.JOIN_TYPE, JoinType.LEFT_OUTER);
                }
            }
        }
        return plan;
    }
}
