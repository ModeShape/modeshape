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
import org.modeshape.jcr.query.plan.CanonicalPlanner;
import org.modeshape.jcr.query.plan.PlanHints;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Type;

/**
 * An {@link OptimizerRule optimizer rule} that inserts an ACCESS above each SOURCE leaf node in a query plan. This rule is often
 * the first rule to run against a {@link CanonicalPlanner canonical plan} (see
 * {@link RuleBasedOptimizer#populateRuleStack(LinkedList, PlanHints)}.
 * <p>
 * Before:
 * 
 * <pre>
 *        ...
 *         |
 *       SOURCE
 * </pre>
 * 
 * After:
 * 
 * <pre>
 *        ...
 *         |
 *       ACCESS
 *         |
 *       SOURCE
 * </pre>
 * 
 * </p>
 */
@Immutable
public class AddAccessNodes implements OptimizerRule {

    public static final AddAccessNodes INSTANCE = new AddAccessNodes();

    @Override
    public PlanNode execute( QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {
        // On each of the source nodes ...
        for (PlanNode source : plan.findAllAtOrBelow(Type.SOURCE)) {
            // The source node may have children if it is a view ...
            if (source.getChildCount() != 0) continue;

            // Create the ACCESS node, set the selectors, and insert above the source node ...
            PlanNode access = new PlanNode(Type.ACCESS);
            access.addSelectors(source.getSelectors());
            source.insertAsParent(access);
        }
        return plan;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
