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
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.query.qom.Comparison;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.JoinCondition;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.StaticOperand;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.api.query.qom.SetCriteria;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.engine.IndexPlan;
import org.modeshape.jcr.query.engine.IndexPlanners;
import org.modeshape.jcr.query.model.Literal;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Operation;
import org.modeshape.jcr.query.plan.PlanNode.Property;
import org.modeshape.jcr.query.plan.PlanNode.Type;
import org.modeshape.jcr.spi.index.IndexCostCalculator;
import org.modeshape.jcr.spi.index.provider.IndexPlanner;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.StringFactory;

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
            // There were some constraints, so prepare to collect indexes ...
            assert source.getSelectors().size() == 1;
            final SelectorName selectorName = source.getSelectors().iterator().next();
            // Look for any SELECT nodes above this but below an ACCESS node, because all of the SELECT define
            // criteria that are all ANDed together ...
            final List<Constraint> constraints = new LinkedList<>();
            final List<JoinCondition> joinConditions = new LinkedList<>();
            final Set<String> nodeTypeNames = new HashSet<>();
            final NodeTypes nodeTypes = context.getNodeTypes();
            final NameFactory nameFactory = context.getExecutionContext().getValueFactories().getNameFactory();
            final StringFactory strings = context.getExecutionContext().getValueFactories().getStringFactory();

            source.applyToAncestors(new Operation() {
                @Override
                public void apply( PlanNode node ) {
                    if (node.getType() == Type.SELECT) {
                        Constraint constraint = node.getProperty(Property.SELECT_CRITERIA, Constraint.class);
                        if (constraint != null) {
                            constraints.add(constraint);
                            // While we're at it, look for the constraint on the primary type. This tells us which node type
                            // we're working with ...
                            if (nodeTypeNames.isEmpty()) {
                                if (constraint instanceof Comparison) {
                                    Comparison comparison = (Comparison)constraint;
                                    if (isPrimaryTypeConstraint(comparison.getOperand1())) {
                                        collectNodeType(comparison.getOperand2());
                                    }
                                } else if (constraint instanceof SetCriteria) {
                                    SetCriteria criteria = (SetCriteria)constraint;
                                    if (isPrimaryTypeConstraint(criteria.getOperand())) {
                                        // Look for literal values and collect them
                                        for (StaticOperand operand : criteria.getValues()) {
                                            collectNodeType(operand);
                                        }
                                    }
                                }
                            }
                        }
                    } else if (node.getType() == Type.JOIN) {
                        // Also look at the join conditions ...
                        JoinCondition joinCondition = node.getProperty(Property.JOIN_CONDITION, JoinCondition.class);
                        if (joinCondition != null) {
                            joinConditions.add(joinCondition);
                        }
                        // Look for additional constraints pushed down to the join ...
                        List<Constraint> joinConstraints = node.getPropertyAsList(Property.JOIN_CONSTRAINTS, Constraint.class);
                        if (joinConstraints != null) {
                            for (Constraint joinConstraint : joinConstraints) {
                                constraints.add(joinConstraint);
                            }
                        }
                    }
                }

                private boolean isPrimaryTypeConstraint( DynamicOperand operand ) {
                    if (operand instanceof PropertyValue) {
                        PropertyValue propValue = (PropertyValue)operand;
                        if (propValue.getSelectorName().equals(selectorName.getString())) {
                            // The property value matches our selector name, so this might be a constraint on the
                            // primary type or mixin types. If so, then the selector name is an alias ...
                            String propName = propValue.getPropertyName();
                            return "jcr:primaryType".equals(propName) || "jcr:mixinTypes".equals(propName);
                        }
                    }
                    return false;
                }

                private void collectNodeType( StaticOperand operand ) {
                    if (operand instanceof Literal) {
                        // Get the literal value, which should be a node type ...
                        Literal literal = (Literal)operand;
                        String nodeType = strings.create(literal.value());
                        Name nodeTypeName = nameFactory.create(nodeType);
                        boolean shouldAddNodeType = true;
                        // we should only keep the type if it's a super-type because the ReplaceViews rule could have added all
                        // subtypes in the types constraints as well and index matching should be done based on strict type matching
                        for (Iterator<String> nodeTypesIterator = nodeTypeNames.iterator(); nodeTypesIterator.hasNext();) {
                            String collectedType = nodeTypesIterator.next();
                            Name collectedNodeTypeName = nameFactory.create(collectedType);
                            // check if we haven't already added a super type, in which case we won't be adding the sub type
                            if (nodeTypes.isTypeOrSubtype(nodeTypeName, collectedNodeTypeName)) {
                                shouldAddNodeType = false;
                                break;
                            } else if (nodeTypes.isTypeOrSubtype(collectedNodeTypeName, nodeTypeName)) {
                                // we've already collected a subtype of the type so we'll remove it from the list because
                                // we should only be keeping super-types
                                nodeTypesIterator.remove();
                            }
                        }
                        if (shouldAddNodeType) {
                            nodeTypeNames.add(nodeType);
                        }
                    }
                }
            });
            if (!constraints.isEmpty() || !joinConditions.isEmpty()) {
                // Get the selector name. The plan's selectors will contain an alias if one is used, so we have to find
                // the 'selector.[jcr:primaryType] = '<nodeType>' constraint
                // Add the alias ...
                nodeTypeNames.add(selectorName.getString());
                final List<IndexPlan> indexPlans = new LinkedList<>();
                IndexCostCalculator calculator = new IndexCostCalculator() {
                    @Override
                    public Set<String> selectedNodeTypes() {
                        return nodeTypeNames;
                    }

                    @Override
                    public Collection<Constraint> andedConstraints() {
                        return constraints;
                    }

                    @Override
                    public Collection<JoinCondition> joinConditions() {
                        return joinConditions;
                    }

                    @Override
                    public Map<String, Object> getVariables() {
                        return context.getVariables();
                    }

                    @Override
                    public void addIndex( String name,
                                          String workspaceName,
                                          String providerName,
                                          Collection<JoinCondition> joinConditions,
                                          int costEstimate,
                                          long cardinalityEstimate ) {
                        IndexPlan indexPlan = new IndexPlan(name, workspaceName, providerName, null, joinConditions,
                                                            costEstimate, cardinalityEstimate, 1.0f, null);
                        indexPlans.add(indexPlan);
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
                        IndexPlan indexPlan = new IndexPlan(name, workspaceName, providerName, constraints, null, costEstimate,
                                                            cardinalityEstimate, selectivityEstimate, parameters);
                        indexPlans.add(indexPlan);
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
                if (!indexPlans.isEmpty()) {
                    // Sort the index plans, so the best one is first ...
                    Collections.sort(indexPlans);
                    // Add an index node for each index ...
                    for (IndexPlan indexPlan : indexPlans) {
                        // Add a plan node for this index ...
                        PlanNode indexNode = new PlanNode(Type.INDEX, source.getSelectors());
                        indexNode.setProperty(Property.INDEX_SPECIFICATION, indexPlan);
                        // and add it under the SOURCE node ...
                        source.addLastChild(indexNode);
                    }
                }
            }
        }
        return plan;
    }
}
