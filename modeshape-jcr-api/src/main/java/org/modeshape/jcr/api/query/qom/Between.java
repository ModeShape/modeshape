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
package org.modeshape.jcr.api.query.qom;

import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.StaticOperand;

/**
 * A constraint that evaluates to true when the value defined by the dynamic operand evaluates to be within the specified range.
 */
public interface Between extends Constraint {

    /**
     * Get the dynamic operand specification.
     * 
     * @return the dynamic operand; never null
     */
    public DynamicOperand getOperand();

    /**
     * Get the lower bound operand.
     * 
     * @return the lower bound; never null
     */
    public StaticOperand getLowerBound();

    /**
     * Get the upper bound operand.
     * 
     * @return the upper bound; never null
     */
    public StaticOperand getUpperBound();

    /**
     * Return whether the lower bound is to be included in the results.
     * 
     * @return true if the {@link #getLowerBound() lower bound} is to be included, or false otherwise
     */
    public boolean isLowerBoundIncluded();

    /**
     * Return whether the upper bound is to be included in the results.
     * 
     * @return true if the {@link #getUpperBound() upper bound} is to be included, or false otherwise
     */
    public boolean isUpperBoundIncluded();
}
