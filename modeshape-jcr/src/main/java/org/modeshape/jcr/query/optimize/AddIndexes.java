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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import javax.jcr.query.qom.Constraint;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.engine.IndexPlan;
import org.modeshape.jcr.query.engine.IndexPlanners;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Operation;
import org.modeshape.jcr.query.plan.PlanNode.Property;
import org.modeshape.jcr.query.plan.PlanNode.Type;
import org.modeshape.jcr.spi.index.IndexCostCalculator;
import org.modeshape.jcr.spi.index.provider.IndexPlanner;

/**
 * A rule that adds indexes below {@link Type#SOURCE} nodes. The rule uses an {@link IndexPlanner} that will actually look at the
 * AND-ed constraints for the source (that is, the constraints in the {@link Type#SELECT} above the {@link Type#SOURCE} node but
 * below an {@link Type#ACCESS} node) and produce 0 or more indexes. These indexes are then added as {@link Type#INDEX} nodes
 * below the {@link Type#SOURCE} node.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 */
@Immutable
public class AddIndexes implements OptimizerRule {

    private static final AddIndexes IMPLICIT_INDEXES = new AddIndexes(null);

    /**
     * The instance of the rule that uses the implicit indexes, like those for finding nodes (or children or descendants) based
     * upon a path.
     *
     * @return the rule; never null
     */
    public static AddIndexes implicitIndexes() {
        return IMPLICIT_INDEXES;
    }

    /**
     * The instance of the rule that uses the supplied index planner.
     *
     * @param planners the index planners; should not be null
     * @return the new rule; never null
     */
    public static AddIndexes with( IndexPlanners planners ) {
        return new AddIndexes(planners);
    }

    private final IndexPlanners planners;

    protected AddIndexes( IndexPlanners planner ) {
        this.planners = planner != null ? planner : IndexPlanners.implicit();
    }

    @Override
    public PlanNode execute( final QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {
        for (final PlanNode source : plan.findAllAtOrBelow(Type.SOURCE)) {
            // Look for any SELECT nodes above this but below an ACCESS node, because all of the SELECT define
            // criteria that are all ANDed together ...
            final AtomicReference<List<Constraint>> constraints = new AtomicReference<>();
            source.applyToAncestorsUpTo(Type.ACCESS, new Operation() {
                @Override
                public void apply( PlanNode node ) {
                    if (node.getType() == Type.SELECT) {
                        Constraint constraint = node.getProperty(Property.SELECT_CRITERIA, Constraint.class);
                        if (constraint != null) {
                            if (constraints.get() == null) constraints.set(new LinkedList<Constraint>());
                            constraints.get().add(constraint);
                        }
                    }
                }
            });
            if (constraints.get() != null) {
                // There were some constraints, so prepare to collect indexes ...
                assert source.getSelectors().size() == 1;
                final SelectorName selectorName = source.getSelectors().iterator().next();
                IndexCostCalculator calculator = new IndexCostCalculator() {
                    @Override
                    public String selectedNodeType() {
                        return selectorName.getString();
                    }

                    @Override
                    public Collection<Constraint> andedConstraints() {
                        return constraints.get();
                    }

                    @Override
                    public Map<String, Object> getVariables() {
                        return context.getVariables();
                    }

                    @Override
                    public void addIndex( String name,
                                          String workspaceName,
                                          String providerName,
                                          Collection<Constraint> constraints,
                                          int costEstimate,
                                          long cardinalityEstimate,
                                          Float selectivityEstimate,
                                          Map<String, Object> parameters ) {
                        // Add a plan node for this index ...
                        PlanNode indexNode = new PlanNode(Type.INDEX, source.getSelectors());
                        IndexPlan indexPlan = new IndexPlan(name, workspaceName, providerName, constraints, costEstimate,
                                                            cardinalityEstimate, selectivityEstimate, parameters);
                        indexNode.setProperty(Property.INDEX_SPECIFICATION, indexPlan);
                        // and add it under the SOURCE node ...
                        source.addLastChild(indexNode);
                    }

                    @Override
                    public void addIndex( String name,
                                          String workspaceName,
                                          String providerName,
                                          Collection<Constraint> constraints,
                                          int costEstimate,
                                          long cardinalityEstimate,
                                          Float selectivityEstimate ) {
                        addIndex(name, workspaceName, providerName, constraints, costEstimate, cardinalityEstimate,
                                 selectivityEstimate, null);
                    }

                    @Override
                    public void addIndex( String name,
                                          String workspaceName,
                                          String providerName,
                                          Collection<Constraint> constraints,
                                          int costEstimate,
                                          long cardinalityEstimate,
                                          Float selectivityEstimate,
                                          String parameterName,
                                          Object parameterValue ) {
                        Map<String, Object> params = Collections.singletonMap(parameterName, parameterValue);
                        addIndex(name, workspaceName, providerName, constraints, costEstimate, cardinalityEstimate,
                                 selectivityEstimate, params);
                    }

                    @Override
                    public void addIndex( String name,
                                          String workspaceName,
                                          String providerName,
                                          Collection<Constraint> constraints,
                                          int costEstimate,
                                          long cardinalityEstimate,
                                          Float selectivityEstimate,
                                          String parameterName1,
                                          Object parameterValue1,
                                          String parameterName2,
                                          Object parameterValue2 ) {
                        Map<String, Object> params = new HashMap<>();
                        params.put(parameterName1, parameterValue1);
                        params.put(parameterName2, parameterValue2);
                        addIndex(name, workspaceName, providerName, constraints, costEstimate, cardinalityEstimate,
                                 selectivityEstimate, params);
                    }
                };
                // And collect the indexes from the index planner ...
                planners.applyIndexes(context, calculator);
            }
        }
        return plan;
    }
}
