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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.model.And;
import org.modeshape.jcr.query.model.Between;
import org.modeshape.jcr.query.model.Comparison;
import org.modeshape.jcr.query.model.Constraint;
import org.modeshape.jcr.query.model.DynamicOperand;
import org.modeshape.jcr.query.model.EquiJoinCondition;
import org.modeshape.jcr.query.model.FullTextSearchScore;
import org.modeshape.jcr.query.model.JoinCondition;
import org.modeshape.jcr.query.model.NodeDepth;
import org.modeshape.jcr.query.model.NodeId;
import org.modeshape.jcr.query.model.NodeLocalName;
import org.modeshape.jcr.query.model.NodeName;
import org.modeshape.jcr.query.model.NodePath;
import org.modeshape.jcr.query.model.Not;
import org.modeshape.jcr.query.model.Or;
import org.modeshape.jcr.query.model.PropertyExistence;
import org.modeshape.jcr.query.model.PropertyValue;
import org.modeshape.jcr.query.model.SameNodeJoinCondition;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.model.SetCriteria;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Property;
import org.modeshape.jcr.query.plan.PlanNode.Traversal;
import org.modeshape.jcr.query.plan.PlanNode.Type;

/**
 * Changes any criteria that writes criteria containing pseudo-columns into standard criteria.
 */
@Immutable
public class RewritePseudoColumns implements OptimizerRule {

    public static final RewritePseudoColumns INSTANCE = new RewritePseudoColumns();

    @Override
    public PlanNode execute( QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {

        // First go through all the JOIN conditions ...
        for (PlanNode join : plan.findAllAtOrBelow(Traversal.PRE_ORDER, Type.JOIN)) {
            JoinCondition condition = join.getProperty(Property.JOIN_CONDITION, JoinCondition.class);
            JoinCondition newCondition = rewrite(context, condition);
            if (newCondition != condition) {
                join.setProperty(Property.JOIN_CONDITION, newCondition);
            }
            List<Constraint> constraints = join.getPropertyAsList(Property.JOIN_CONSTRAINTS, Constraint.class);
            List<Constraint> newConstraints = rewrite(context, constraints);
            if (newConstraints != constraints) {
                join.setProperty(Property.JOIN_CONSTRAINTS, newConstraints);
            }
        }

        // Go through all the WHERE criteria ...
        for (PlanNode node : plan.findAllAtOrBelow(Traversal.PRE_ORDER, Type.SELECT)) {
            Constraint constraint = node.getProperty(Property.SELECT_CRITERIA, Constraint.class);
            Constraint newConstraint = rewrite(context, constraint);
            if (newConstraint != constraint) {
                node.setProperty(Property.SELECT_CRITERIA, newConstraint);
            }
        }
        return plan;
    }

    protected JoinCondition rewrite( QueryContext context,
                                     JoinCondition condition ) {
        if (condition instanceof EquiJoinCondition) {
            EquiJoinCondition equiJoin = (EquiJoinCondition)condition;
            if ("jcr:path".equals(equiJoin.getProperty1Name())) {
                SelectorName selector1 = equiJoin.selector1Name();
                SelectorName selector2 = equiJoin.selector2Name();
                if ("jcr:path".equals(equiJoin.getProperty2Name())) {
                    // This equijoin should be rewritten as a ISSAMENODE condition ...
                    return new SameNodeJoinCondition(selector1, selector2);
                }
                // Only one side uses "jcr:path", and we cannot handle this ...
                context.getProblems().addError(JcrI18n.equiJoinWithOneJcrPathPseudoColumnIsInvalid, selector1, selector2);
            } else if ("jcr:path".equals(equiJoin.getProperty2Name())) {
                SelectorName selector1 = equiJoin.selector1Name();
                SelectorName selector2 = equiJoin.selector2Name();
                // Only one side uses "jcr:path", and we cannot handle this ...
                context.getProblems().addError(JcrI18n.equiJoinWithOneJcrPathPseudoColumnIsInvalid, selector1, selector2);
            } else if ("mode:id".equals(equiJoin.getProperty1Name())) {
                SelectorName selector1 = equiJoin.selector1Name();
                SelectorName selector2 = equiJoin.selector2Name();
                if ("mode:id".equals(equiJoin.getProperty2Name())) {
                    // This equijoin should be rewritten as a ISSAMENODE condition ...
                    return new SameNodeJoinCondition(selector1, selector2);
                }
                // Only one side uses "jcr:path", and we cannot handle this ...
                context.getProblems().addError(JcrI18n.equiJoinWithOneNodeIdPseudoColumnIsInvalid, selector1, selector2);
            } else if ("mode:id".equals(equiJoin.getProperty2Name())) {
                SelectorName selector1 = equiJoin.selector1Name();
                SelectorName selector2 = equiJoin.selector2Name();
                // Only one side uses "jcr:path", and we cannot handle this ...
                context.getProblems().addError(JcrI18n.equiJoinWithOneNodeIdPseudoColumnIsInvalid, selector1, selector2);
            }
        }
        return condition;
    }

    protected List<Constraint> rewrite( QueryContext context,
                                        List<Constraint> constraints ) {
        if (constraints == null) return null;
        List<Constraint> rewritten = new ArrayList<>(constraints.size());
        for (Constraint constraint : constraints) {
            rewritten.add(rewrite(context, constraint));
        }
        return rewritten;
    }

    protected Constraint rewrite( QueryContext context,
                                  Constraint constraint ) {
        if (constraint instanceof Comparison) {
            Comparison comparison = (Comparison)constraint;
            DynamicOperand operand = comparison.getOperand1();
            DynamicOperand newOperand = rewrite(context, operand);
            if (newOperand != operand) {
                return new Comparison(newOperand, comparison.operator(), comparison.getOperand2());
            }
        } else if (constraint instanceof And) {
            And and = (And)constraint;
            Constraint left = and.left();
            Constraint right = and.right();
            Constraint newLeft = rewrite(context, left);
            Constraint newRight = rewrite(context, right);
            if (newLeft != left || newRight != right) {
                return new And(newLeft, newRight);
            }
        } else if (constraint instanceof Or) {
            Or or = (Or)constraint;
            Constraint left = or.left();
            Constraint right = or.right();
            Constraint newLeft = rewrite(context, left);
            Constraint newRight = rewrite(context, right);
            if (newLeft != left || newRight != right) {
                return new Or(newLeft, newRight);
            }
        } else if (constraint instanceof Not) {
            Not not = (Not)constraint;
            Constraint oldInner = not.getConstraint();
            Constraint newInner = rewrite(context, oldInner);
            if (oldInner != newInner) {
                return new Not(newInner);
            }
        } else if (constraint instanceof Between) {
            Between between = (Between)constraint;
            DynamicOperand operand = between.getOperand();
            DynamicOperand newOperand = rewrite(context, operand);
            if (newOperand != operand) {
                return new Between(newOperand, between.getLowerBound(), between.getUpperBound(), between.isLowerBoundIncluded(),
                                   between.isUpperBoundIncluded());
            }
        } else if (constraint instanceof SetCriteria) {
            SetCriteria set = (SetCriteria)constraint;
            DynamicOperand operand = set.leftOperand();
            DynamicOperand newOperand = rewrite(context, operand);
            if (newOperand != operand) {
                return new SetCriteria(newOperand, set.rightOperands());
            }
        } else if (constraint instanceof PropertyExistence) {
            PropertyExistence exist = (PropertyExistence)constraint;
            String property = exist.getPropertyName();
            if ("jcr:path".equals(property) || "jcr:name".equals(property) || "mode:localName".equals(property)
                || "jcr:localName".equals(property) || "mode:depth".equals(property) || "jcr:depth".equals(property)
                || "jcr:score".equals(property) || "mode:id".equals(property)) {
                // This constraint will always be true, so use this constraint that always is true ...
                return new PropertyExistence(exist.selectorName(), "jcr:primaryType");
            }
        }
        return constraint;
    }

    protected DynamicOperand rewrite( QueryContext context,
                                      DynamicOperand operand ) {
        if (operand instanceof PropertyValue) {
            PropertyValue propValue = (PropertyValue)operand;
            String property = propValue.getPropertyName();
            if ("jcr:path".equals(property)) {
                return new NodePath(propValue.selectorName());
            }
            if ("jcr:name".equals(property)) {
                return new NodeName(propValue.selectorName());
            }
            if ("mode:localName".equals(property) || "jcr:localName".equals(property)) {
                return new NodeLocalName(propValue.selectorName());
            }
            if ("mode:depth".equals(property) || "jcr:depth".equals(property)) {
                return new NodeDepth(propValue.selectorName());
            }
            if ("mode:id".equals(property)) {
                return new NodeId(propValue.selectorName());
            }
            if ("jcr:score".equals(property)) {
                return new FullTextSearchScore(propValue.selectorName());
            }
        }
        return operand;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
