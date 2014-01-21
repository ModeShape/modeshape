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

/**
 * Interface that defines an {@link Optimizer} rule.
 */
@Immutable
public interface OptimizerRule {

    /**
     * Optimize the supplied plan using the supplied context, hints, and yet-to-be-run rules.
     * 
     * @param context the context in which the query is being optimized; never null
     * @param plan the plan to be optimized; never null
     * @param ruleStack the stack of rules that will be run after this rule; never null
     * @return the optimized plan; never null
     */
    PlanNode execute( QueryContext context,
                      PlanNode plan,
                      LinkedList<OptimizerRule> ruleStack );

}
