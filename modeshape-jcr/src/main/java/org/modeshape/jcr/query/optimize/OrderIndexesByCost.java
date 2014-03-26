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

import java.util.Comparator;
import java.util.LinkedList;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.engine.IndexPlan;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Property;
import org.modeshape.jcr.query.plan.PlanNode.Type;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class OrderIndexesByCost implements OptimizerRule {

    public static final OrderIndexesByCost INSTANCE = new OrderIndexesByCost();

    @Override
    public PlanNode execute( final QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {
        for (PlanNode source : plan.findAllAtOrBelow(Type.SOURCE)) {
            source.orderChildren(Type.INDEX, new Comparator<PlanNode>() {
                @Override
                public int compare( PlanNode o1,
                                    PlanNode o2 ) {
                    IndexPlan index1 = o1.getProperty(Property.INDEX_SPECIFICATION, IndexPlan.class);
                    IndexPlan index2 = o2.getProperty(Property.INDEX_SPECIFICATION, IndexPlan.class);
                    assert index1 != null;
                    assert index2 != null;
                    return index1.compareTo(index2);
                }
            });
        }
        return plan;
    }
}
