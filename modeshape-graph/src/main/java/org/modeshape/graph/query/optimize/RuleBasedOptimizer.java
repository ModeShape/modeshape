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
import org.modeshape.common.collection.Problems;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.plan.PlanHints;
import org.modeshape.graph.query.plan.PlanNode;

/**
 * Optimizer implementation that optimizes a query using a stack of rules. Subclasses can override the
 * {@link #populateRuleStack(LinkedList, PlanHints)} method to define the stack of rules they'd like to use, including the use of
 * custom rules.
 */
@Immutable
public class RuleBasedOptimizer implements Optimizer {

    private static final Logger LOGGER = Logger.getLogger(RuleBasedOptimizer.class);

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.optimize.Optimizer#optimize(QueryContext, PlanNode)
     */
    public PlanNode optimize( QueryContext context,
                              PlanNode plan ) {
        LinkedList<OptimizerRule> rules = new LinkedList<OptimizerRule>();
        populateRuleStack(rules, context.getHints());

        Problems problems = context.getProblems();
        while (rules.peek() != null && !problems.hasErrors()) {
            OptimizerRule nextRule = rules.poll();
            LOGGER.debug("Running query optimizer rule {0}", nextRule);
            plan = nextRule.execute(context, plan, rules);
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
        if (hints.hasSubqueries) {
            ruleStack.addFirst(RaiseVariableName.INSTANCE);
        }
        ruleStack.addFirst(RewriteAsRangeCriteria.INSTANCE);
        if (hints.hasJoin) {
            ruleStack.addFirst(ChooseJoinAlgorithm.USE_ONLY_NESTED_JOIN_ALGORITHM);
            ruleStack.addFirst(RewriteIdentityJoins.INSTANCE);
        }
        ruleStack.addFirst(PushProjects.INSTANCE);
        ruleStack.addFirst(PushSelectCriteria.INSTANCE);
        ruleStack.addFirst(AddAccessNodes.INSTANCE);
        ruleStack.addFirst(RightOuterToLeftOuterJoins.INSTANCE);
        ruleStack.addFirst(CopyCriteria.INSTANCE);
        if (hints.hasView) {
            ruleStack.addFirst(ReplaceViews.INSTANCE);
        }
    }
}
