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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.query.model.Constraint;
import org.jboss.dna.graph.query.model.DynamicOperand;
import org.jboss.dna.graph.query.model.SetCriteria;
import org.jboss.dna.graph.query.model.StaticOperand;

/**
 * An {@link OptimizerRule optimizer rule} that merges SELECT nodes that have {@link SetCriteria} applied to the same column on
 * the same selector.
 */
@Immutable
public class MergeSetCriteria extends AbstractMergeSelectNodes<DynamicOperand, SetCriteria> {

    public static final MergeSetCriteria INSTANCE = new MergeSetCriteria();

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.optimize.AbstractMergeSelectNodes#getSimilarityKey(org.jboss.dna.graph.query.model.Constraint)
     */
    @Override
    protected DynamicOperand getSimilarityKey( Constraint constraint ) {
        if (constraint instanceof SetCriteria) {
            // Look for the list of plan nodes for this operand ...
            return ((SetCriteria)constraint).getLeftOperand();
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.optimize.AbstractMergeSelectNodes#merge(org.jboss.dna.graph.query.model.Constraint,
     *      org.jboss.dna.graph.query.model.Constraint)
     */
    @Override
    protected Constraint merge( SetCriteria firstConstraint,
                                SetCriteria secondConstraint ) {
        assert firstConstraint.getLeftOperand().equals(secondConstraint.getLeftOperand());
        Set<StaticOperand> allOperands = new HashSet<StaticOperand>();
        List<StaticOperand> orderedOperands = new ArrayList<StaticOperand>(firstConstraint.getRightOperands());
        allOperands.addAll(firstConstraint.getRightOperands());
        for (StaticOperand second : secondConstraint.getRightOperands()) {
            if (allOperands.add(second)) {
                orderedOperands.add(second);
            }
        }
        return new SetCriteria(firstConstraint.getLeftOperand(), orderedOperands);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getClass().getSimpleName();
    }
}
