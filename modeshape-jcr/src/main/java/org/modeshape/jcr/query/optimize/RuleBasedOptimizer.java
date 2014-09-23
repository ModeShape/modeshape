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
import org.modeshape.common.collection.Problems;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.plan.PlanHints;
import org.modeshape.jcr.query.plan.PlanNode;

/**
 * Optimizer implementation that optimizes a query using a stack of rules. Subclasses can override the
 * {@link #populateRuleStack(LinkedList, PlanHints)} method to define the stack of rules they'd like to use, including the use of
 * custom rules.
 */
@Immutable
public class RuleBasedOptimizer implements Optimizer {

    private static final Logger LOGGER = Logger.getLogger(RuleBasedOptimizer.class);

    @Override
    public PlanNode optimize( QueryContext context,
                              PlanNode plan ) {
        LinkedList<OptimizerRule> rules = new LinkedList<OptimizerRule>();
        populateRuleStack(rules, context.getHints());

        Problems problems = context.getProblems();
        while (rules.peek() != null && !problems.hasErrors()) {
            OptimizerRule nextRule = rules.poll();
            LOGGER.trace("Running query optimizer rule {0}", nextRule);
            plan = nextRule.execute(context, plan, rules);
            LOGGER.trace("Plan after running query optimizer rule {0}: \n{1}", nextRule, plan);
        }

        return plan;
    }

    /**
     * Method that is used to create the initial rule stack. This method can be overridden by subclasses
     * 
     * @param ruleStack the stack where the rules should be placed; never null
     * @param hints the plan hints
     */
    protected void populateRuleStack( LinkedList<OptimizerRule> ruleStack,
                                      PlanHints hints ) {
        ruleStack.addFirst(ReorderSortAndRemoveDuplicates.INSTANCE);
        ruleStack.addFirst(RewritePathAndNameCriteria.INSTANCE);
        if (hints.hasSubqueries) {
            ruleStack.addFirst(RaiseVariableName.INSTANCE);
        }
        ruleStack.addFirst(RewriteAsRangeCriteria.INSTANCE);
        if (hints.hasJoin) {
            ruleStack.addFirst(AddJoinConditionColumnsToSources.INSTANCE);
            ruleStack.addFirst(ChooseJoinAlgorithm.USE_ONLY_NESTED_JOIN_ALGORITHM);
            ruleStack.addFirst(RewriteIdentityJoins.INSTANCE);
        }
        ruleStack.addFirst(AddOrderingColumnsToSources.INSTANCE);
        ruleStack.addFirst(PushProjects.INSTANCE);
        ruleStack.addFirst(PushSelectCriteria.INSTANCE);
        ruleStack.addFirst(AddAccessNodes.INSTANCE);
        ruleStack.addFirst(JoinOrder.INSTANCE);
        ruleStack.addFirst(RightOuterToLeftOuterJoins.INSTANCE);
        ruleStack.addFirst(CopyCriteria.INSTANCE);
        if (hints.hasView) {
            ruleStack.addFirst(ReplaceViews.INSTANCE);
        }
        ruleStack.addFirst(RewritePseudoColumns.INSTANCE);
        // Add indexes determination last ...
        populateIndexingRules(ruleStack, hints);
        ruleStack.addLast(OrderIndexesByCost.INSTANCE);
    }

    /**
     * Method that is used to add the indexing rules to the rule stack. This method can be overridden by subclasses when custom
     * indexing rules are to be used. By default, this method simply adds the {@link AddIndexes} rule.
     * 
     * @param ruleStack the stack where the rules should be placed; never null
     * @param hints the plan hints
     */
    protected void populateIndexingRules( LinkedList<OptimizerRule> ruleStack,
                                          PlanHints hints ) {
        ruleStack.addLast(AddIndexes.implicitIndexes());
    }
}
