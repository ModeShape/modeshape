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

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.model.Constraint;
import org.jboss.dna.graph.query.model.SetCriteria;
import org.jboss.dna.graph.query.plan.PlanNode;
import org.jboss.dna.graph.query.plan.PlanNode.Property;
import org.jboss.dna.graph.query.plan.PlanNode.Type;

/**
 * An {@link OptimizerRule optimizer rule} that merges SELECT nodes that have {@link SetCriteria} applied to the same column on
 * the same selector.
 * 
 * @param <SimilarityType> the type used to compare and identify similar nodes
 * @param <ConstraintType> the concrete type of Constraint objects that are being merged
 */
@Immutable
public abstract class AbstractMergeSelectNodes<SimilarityType, ConstraintType extends Constraint> implements OptimizerRule {

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.optimize.OptimizerRule#execute(org.jboss.dna.graph.query.QueryContext,
     *      org.jboss.dna.graph.query.plan.PlanNode, java.util.LinkedList)
     */
    @SuppressWarnings( "unchecked" )
    public PlanNode execute( QueryContext context,
                             PlanNode plan,
                             LinkedList<OptimizerRule> ruleStack ) {
        Map<SimilarityType, LinkedList<PlanNode>> mergeables = new HashMap<SimilarityType, LinkedList<PlanNode>>();
        for (PlanNode source : plan.findAllAtOrBelow(Type.SOURCE)) {
            // Walk up from the SOURCE and look for all SELECT nodes ...
            PlanNode node = source.getParent();
            while (node != null && node.is(Type.SELECT)) {
                Constraint constraint = node.getProperty(Property.SELECT_CRITERIA, Constraint.class);
                SimilarityType operand = getSimilarityKey(constraint);
                if (operand != null) {
                    LinkedList<PlanNode> nodes = mergeables.get(operand);
                    if (nodes == null) {
                        nodes = new LinkedList<PlanNode>();
                        mergeables.put(operand, nodes);
                    }
                    nodes.addFirst(node); // So that the list is in the downward order they appear in the plan
                }
                node = node.getParent();
            }

            // Merge all mergeable SELECT nodes ...
            for (LinkedList<PlanNode> mergeable : mergeables.values()) {
                if (mergeable.size() > 1) {
                    Iterator<PlanNode> iter = mergeable.iterator();
                    PlanNode firstSelect = iter.next();
                    Constraint constraint = firstSelect.getProperty(Property.SELECT_CRITERIA, Constraint.class);
                    while (iter.hasNext()) {
                        PlanNode nextSelect = iter.next();
                        Constraint nextConstraint = nextSelect.getProperty(Property.SELECT_CRITERIA, Constraint.class);
                        constraint = merge((ConstraintType)constraint, (ConstraintType)nextConstraint);
                        nextSelect.extractFromParent();
                    }
                    firstSelect.setProperty(Property.SELECT_CRITERIA, constraint);
                }
            }
            mergeables.clear();
        }
        return plan;
    }

    /**
     * Obtain the similarity key that will be used to determine whether two SELECT nodes should be merged.
     * 
     * @param constraint the constraint from the SELECT node
     * @return the similarity key, or null if the constraint should not be merged
     */
    protected abstract SimilarityType getSimilarityKey( Constraint constraint );

    /**
     * Merge the two constraints.
     * 
     * @param firstConstraint the first constraint
     * @param secondConstraint the second constraint
     * @return the merged constraint
     */
    protected abstract Constraint merge( ConstraintType firstConstraint,
                                         ConstraintType secondConstraint );
}
