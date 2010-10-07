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
package org.modeshape.jcr.query;

import java.util.LinkedList;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.query.QueryContext;
import org.modeshape.graph.query.model.And;
import org.modeshape.graph.query.model.Between;
import org.modeshape.graph.query.model.Comparison;
import org.modeshape.graph.query.model.Constraint;
import org.modeshape.graph.query.model.DynamicOperand;
import org.modeshape.graph.query.model.EquiJoinCondition;
import org.modeshape.graph.query.model.FullTextSearchScore;
import org.modeshape.graph.query.model.JoinCondition;
import org.modeshape.graph.query.model.NodeDepth;
import org.modeshape.graph.query.model.NodeLocalName;
import org.modeshape.graph.query.model.NodeName;
import org.modeshape.graph.query.model.NodePath;
import org.modeshape.graph.query.model.Not;
import org.modeshape.graph.query.model.Or;
import org.modeshape.graph.query.model.PropertyExistence;
import org.modeshape.graph.query.model.PropertyValue;
import org.modeshape.graph.query.model.SameNodeJoinCondition;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.SetCriteria;
import org.modeshape.graph.query.optimize.OptimizerRule;
import org.modeshape.graph.query.plan.PlanNode;
import org.modeshape.graph.query.plan.PlanNode.Property;
import org.modeshape.graph.query.plan.PlanNode.Traversal;
import org.modeshape.graph.query.plan.PlanNode.Type;
import org.modeshape.jcr.JcrI18n;

/**
 * An {@link OptimizerRule optimizer rule} that moves up higher in the plan any {@link Property#VARIABLE_NAME variable name}
 * property to the node immediately under a {@link Type#DEPENDENT_QUERY dependent query} node.
 */
@Immutable
public class RewritePseudoColumns implements OptimizerRule {

    public static final RewritePseudoColumns INSTANCE = new RewritePseudoColumns();

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.optimize.OptimizerRule#execute(org.modeshape.graph.query.QueryContext,
     *      org.modeshape.graph.query.plan.PlanNode, java.util.LinkedList)
     */
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
            Constraint constraint = join.getProperty(Property.JOIN_CONSTRAINTS, Constraint.class);
            Constraint newConstraint = rewrite(context, constraint);
            if (newConstraint != constraint) {
                join.setProperty(Property.JOIN_CONSTRAINTS, newConstraint);
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
            if ("jcr:path".equals(equiJoin.property1Name())) {
                SelectorName selector1 = equiJoin.selector1Name();
                SelectorName selector2 = equiJoin.selector2Name();
                if ("jcr:path".equals(equiJoin.property2Name())) {
                    // This equijoin should be rewritten as a ISSAMENODE condition ...
                    return new SameNodeJoinCondition(selector1, selector2);
                }
                // Only one side uses "jcr:path", and we cannot handle this ...
                context.getProblems().addError(JcrI18n.equiJoinWithOneJcrPathPseudoColumnIsInvalid, selector1, selector2);
            } else if ("jcr:path".equals(equiJoin.property2Name())) {
                SelectorName selector1 = equiJoin.selector1Name();
                SelectorName selector2 = equiJoin.selector2Name();
                // Only one side uses "jcr:path", and we cannot handle this ...
                context.getProblems().addError(JcrI18n.equiJoinWithOneJcrPathPseudoColumnIsInvalid, selector1, selector2);
            }
        }
        return condition;
    }

    protected Constraint rewrite( QueryContext context,
                                  Constraint constraint ) {
        if (constraint instanceof Comparison) {
            Comparison comparison = (Comparison)constraint;
            DynamicOperand operand = comparison.operand1();
            DynamicOperand newOperand = rewrite(context, operand);
            if (newOperand != operand) {
                return new Comparison(newOperand, comparison.operator(), comparison.operand2());
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
            Constraint oldInner = not.constraint();
            Constraint newInner = rewrite(context, oldInner);
            if (oldInner != newInner) {
                return new Not(newInner);
            }
        } else if (constraint instanceof Between) {
            Between between = (Between)constraint;
            DynamicOperand operand = between.operand();
            DynamicOperand newOperand = rewrite(context, operand);
            if (newOperand != operand) {
                return new Between(newOperand, between.lowerBound(), between.upperBound(), between.isLowerBoundIncluded(),
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
            String property = exist.propertyName();
            if ("jcr:path".equals(property) || "jcr:name".equals(property) || "mode:localName".equals(property)
                || "jcr:localName".equals(property) || "mode:depth".equals(property) || "jcr:depth".equals(property)
                || "jcr:score".equals(property)) {
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
            String property = propValue.propertyName();
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
            if ("jcr:score".equals(property)) {
                return new FullTextSearchScore(propValue.selectorName());
            }
        }
        return operand;
    }

}
