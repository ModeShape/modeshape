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
package org.modeshape.jcr.query.qom;

import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.StaticOperand;
import org.modeshape.graph.query.model.Between;

/**
 * Implementation of the 'between' constraint for the Graph API and that is an extension to JCR Query Object Model.
 */
public class JcrBetween extends Between implements JcrConstraint, org.modeshape.jcr.api.query.qom.Between {

    private static final long serialVersionUID = 1L;

    /**
     * Create a constraint that the values of the supplied dynamic operand are between the lower and upper bounds, specifying
     * whether the boundary values are to be included in the range.
     * 
     * @param operand the dynamic operand describing the values that are to be constrained
     * @param lowerBound the lower bound of the range
     * @param upperBound the upper bound of the range
     * @param includeLowerBound true if the lower boundary value is not be included
     * @param includeUpperBound true if the upper boundary value is not be included
     * @throws IllegalArgumentException if any of the arguments are null
     */
    public JcrBetween( JcrDynamicOperand operand,
                       JcrStaticOperand lowerBound,
                       JcrStaticOperand upperBound,
                       boolean includeLowerBound,
                       boolean includeUpperBound ) {
        super(operand, lowerBound, upperBound, includeLowerBound, includeUpperBound);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Between#operand()
     */
    @Override
    public JcrDynamicOperand operand() {
        return (JcrDynamicOperand)super.operand();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Between#lowerBound()
     */
    @Override
    public JcrStaticOperand lowerBound() {
        return (JcrStaticOperand)super.lowerBound();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Between#upperBound()
     */
    @Override
    public JcrStaticOperand upperBound() {
        return (JcrStaticOperand)super.upperBound();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.Between#getLowerBound()
     */
    @Override
    public StaticOperand getLowerBound() {
        return lowerBound();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.Between#getUpperBound()
     */
    @Override
    public StaticOperand getUpperBound() {
        return upperBound();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.Between#getOperand()
     */
    @Override
    public DynamicOperand getOperand() {
        return operand();
    }
}
