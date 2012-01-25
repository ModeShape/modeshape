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
package org.modeshape.jcr.query.optimize;

import java.util.LinkedList;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.model.And;
import org.modeshape.jcr.query.model.Between;
import org.modeshape.jcr.query.model.ChildNode;
import org.modeshape.jcr.query.model.Comparison;
import org.modeshape.jcr.query.model.Constraint;
import org.modeshape.jcr.query.model.DescendantNode;
import org.modeshape.jcr.query.model.DynamicOperand;
import org.modeshape.jcr.query.model.FullTextSearch;
import org.modeshape.jcr.query.model.NodeDepth;
import org.modeshape.jcr.query.model.NodeLocalName;
import org.modeshape.jcr.query.model.NodeName;
import org.modeshape.jcr.query.model.NodePath;
import org.modeshape.jcr.query.model.Not;
import org.modeshape.jcr.query.model.Or;
import org.modeshape.jcr.query.model.PropertyExistence;
import org.modeshape.jcr.query.model.SetCriteria;
import org.modeshape.jcr.query.model.Visitors;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Property;
import org.modeshape.jcr.query.plan.PlanNode.Type;

/**
 * An {@link OptimizerRule optimizer rule} that rewrites Constraint trees, moving path-, name-, or depth-oriented criteria to the
 * left-most parts of the constraint tree.
 */
@Immutable
public class RewritePathAndNameCriteria implements OptimizerRule {

    public static final RewritePathAndNameCriteria INSTANCE = new RewritePathAndNameCriteria();

    @Override
    public PlanNode execute( QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {
        // Remove the view from the selectors ...
        for (PlanNode node : plan.findAllAtOrBelow(Type.SELECT)) {
            Constraint constraint = node.getProperty(Property.SELECT_CRITERIA, Constraint.class);
            Constraint newConstraint = rewriteCriteria(context, constraint);
            if (constraint != newConstraint) {
                node.getSelectors().clear();
                node.addSelectors(Visitors.getSelectorsReferencedBy(newConstraint));
                node.setProperty(Property.SELECT_CRITERIA, newConstraint);
            }
        }
        return plan;
    }

    protected Constraint rewriteCriteria( QueryContext context,
                                          Constraint constraint ) {
        if (constraint instanceof And) {
            And and = (And)constraint;
            Constraint oldLeft = and.left();
            Constraint oldRight = and.right();
            Constraint newLeft = rewriteCriteria(context, oldLeft);
            Constraint newRight = rewriteCriteria(context, oldRight);
            if (newRight != oldRight) {
                // The right side is path-oriented ...
                if (newLeft != oldLeft) {
                    // The left is too, so create a new And constraint to signal that it's path oriented ...
                    return new And(newLeft, newRight);
                }
                // Only the right is, so swap the order ...
                return new And(newRight, newLeft);
            }
            // Neither are path-oriented ...
            return and;
        }
        if (constraint instanceof Or) {
            Or or = (Or)constraint;
            Constraint oldLeft = or.left();
            Constraint oldRight = or.right();
            Constraint newLeft = rewriteCriteria(context, oldLeft);
            Constraint newRight = rewriteCriteria(context, oldRight);
            if (newRight != oldRight) {
                // The right side is path-oriented ...
                if (newLeft != oldLeft) {
                    // The left is too, so create a new Or constraint to signal that it's path oriented ...
                    return new Or(newLeft, newRight);
                }
                // Only the right is, so swap the order ...
                return new Or(newRight, newLeft);
            }
            // Neither are path-oriented ...
            return or;
        }
        if (constraint instanceof Not) {
            Not not = (Not)constraint;
            Constraint oldWrapped = not.getConstraint();
            Constraint newWrapped = rewriteCriteria(context, oldWrapped);
            if (newWrapped != oldWrapped) {
                // The wrapped constraint is path-oriented, so create a new Not to signal it ...
                return new Not(newWrapped);
            }
            return not;
        }
        if (constraint instanceof ChildNode) {
            ChildNode childNode = (ChildNode)constraint;
            return new ChildNode(childNode.selectorName(), childNode.getParentPath());
        }
        if (constraint instanceof DescendantNode) {
            DescendantNode descNode = (DescendantNode)constraint;
            return new DescendantNode(descNode.selectorName(), descNode.getAncestorPath());
        }
        if (constraint instanceof PropertyExistence) {
            return constraint;
        }
        if (constraint instanceof FullTextSearch) {
            return constraint;
        }
        if (constraint instanceof Between) {
            Between between = (Between)constraint;
            DynamicOperand operand = between.getOperand();
            if (isPathOriented(operand)) {
                return new Between(operand, between.getLowerBound(), between.getUpperBound(), between.isLowerBoundIncluded(),
                                   between.isUpperBoundIncluded());
            }
            return between;
        }
        if (constraint instanceof Comparison) {
            Comparison comparison = (Comparison)constraint;
            DynamicOperand operand = comparison.getOperand1();
            if (isPathOriented(operand)) {
                return new Comparison(operand, comparison.operator(), comparison.getOperand2());
            }
            return comparison;
        }
        if (constraint instanceof SetCriteria) {
            SetCriteria criteria = (SetCriteria)constraint;
            DynamicOperand operand = criteria.getOperand();
            if (isPathOriented(operand)) {
                return new SetCriteria(operand, criteria.rightOperands());
            }
            return criteria;
        }
        return constraint;
    }

    protected boolean isPathOriented( DynamicOperand op ) {
        return op instanceof NodeDepth || op instanceof NodeLocalName || op instanceof NodeName || op instanceof NodePath;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
