/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.query.optimize;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.optimize.OptimizerRule;
import org.jboss.dna.graph.query.optimize.RuleBasedOptimizer;
import org.jboss.dna.graph.query.plan.PlanHints;
import org.jboss.dna.graph.query.plan.PlanNode;
import org.jboss.dna.graph.query.plan.PlanNode.Type;
import org.jboss.dna.graph.query.validate.Schemata;
import org.junit.Before;
import org.junit.Test;

/**
 * 
 */
public class RuleBasedOptimizerTest {

    private RuleBasedOptimizer optimizer;
    private List<OptimizerRule> rules;
    private List<Integer> ruleExecutionOrder;
    private QueryContext context;
    private PlanNode node;

    @Before
    public void beforeEach() {
        context = new QueryContext(new ExecutionContext(), new PlanHints(), mock(Schemata.class));
        node = new PlanNode(Type.ACCESS);

        ruleExecutionOrder = new ArrayList<Integer>();
        rules = new ArrayList<OptimizerRule>();

        // Add rules that, when executed, add their number to the 'ruleExecutionOrder' list ...
        for (int i = 0; i != 5; ++i) {
            final int ruleNumber = i;
            this.rules.add(new OptimizerRule() {
                @SuppressWarnings( "synthetic-access" )
                public PlanNode execute( QueryContext context,
                                         PlanNode plan,
                                         LinkedList<OptimizerRule> ruleStack ) {
                    ruleExecutionOrder.add(ruleNumber);
                    return plan;
                }
            });
        }

        // Create a rule-based optimizer that uses a stack of completely artificial mock rules ...
        this.optimizer = new RuleBasedOptimizer() {
            @SuppressWarnings( "synthetic-access" )
            @Override
            protected void populateRuleStack( LinkedList<OptimizerRule> ruleStack,
                                              PlanHints hints ) {
                ruleStack.addAll(rules);
            }
        };
    }

    @Test
    public void shouldExecuteEachRuleInSequence() {
        optimizer.optimize(context, node);
        for (int i = 0; i != rules.size(); ++i) {
            assertThat(ruleExecutionOrder.get(i), is(i));
        }
    }

    @Test
    public void shouldStopExecutingRulesIfThereIsAnErrorInTheProblems() {
        // Change of the rules to generate an error ...
        this.rules.set(3, new OptimizerRule() {
            public PlanNode execute( QueryContext context,
                                     PlanNode plan,
                                     LinkedList<OptimizerRule> ruleStack ) {
                context.getProblems().addError(GraphI18n.closedConnectionMayNotBeUsed);
                return plan;
            }
        });

        optimizer.optimize(context, node);
        assertThat(ruleExecutionOrder.get(0), is(0));
        assertThat(ruleExecutionOrder.get(1), is(1));
        assertThat(ruleExecutionOrder.get(2), is(2));
        assertThat(ruleExecutionOrder.size(), is(3));
    }
}
