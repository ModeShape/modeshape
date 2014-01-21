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
import org.modeshape.jcr.query.plan.PlanNode.Type;

/**
 * An {@link OptimizerRule optimizer rule} that removes any ACCESS nodes that are known to never return any tuples because of
 * conflicting constraints.
 */
@Immutable
public class RemoveEmptyAccessNodes implements OptimizerRule {

    public static final RemoveEmptyAccessNodes INSTANCE = new RemoveEmptyAccessNodes();

    @Override
    public PlanNode execute( QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {
        // Find all access nodes ...
        for (PlanNode access : plan.findAllAtOrBelow(Type.ACCESS)) {
            if (access.getProperty(Property.ACCESS_NO_RESULTS, Boolean.class)) {
                // This node has conflicting constraints and will never return any results ...

                // TODO: implement this rule.

                // At least the QueryProcessor looks for this property and always creates a NoResultsComponent,
                // saving some work. But implementing this rule will make queries more efficient.
            }
        }

        return plan;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
