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
package org.modeshape.graph.query.model;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import net.jcip.annotations.Immutable;
import org.modeshape.common.util.CheckArg;

/**
 * A constraint that evaluates to true when the defined operation evaluates to true.
 */
@Immutable
public class SetCriteria implements Constraint {
    private static final long serialVersionUID = 1L;

    private final DynamicOperand left;
    private final Collection<? extends StaticOperand> setOperands;

    public SetCriteria( DynamicOperand left,
                        Collection<? extends StaticOperand> setOperands ) {
        CheckArg.isNotNull(left, "left");
        CheckArg.isNotNull(setOperands, "setOperands");
        CheckArg.isNotEmpty(setOperands, "setOperands");
        this.left = left;
        this.setOperands = setOperands;
    }

    public SetCriteria( DynamicOperand left,
                        StaticOperand... setOperands ) {
        CheckArg.isNotNull(left, "left");
        CheckArg.isNotNull(setOperands, "setOperands");
        CheckArg.isNotEmpty(setOperands, "setOperands");
        this.left = left;
        this.setOperands = Collections.unmodifiableList(Arrays.asList(setOperands));
    }

    /**
     * Get the dynamic operand to which the set constraint applies
     * 
     * @return the dynamic operand; never null
     */
    public final DynamicOperand leftOperand() {
        return left;
    }

    /**
     * Get the collection of static operands defining the constrained values.
     * 
     * @return the right-hand-side static operands; never null and never empty
     */
    public final Collection<? extends StaticOperand> rightOperands() {
        return setOperands;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return left.hashCode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof SetCriteria) {
            SetCriteria that = (SetCriteria)obj;
            if (!this.left.equals(that.left)) return false;
            if (!this.setOperands.equals(that.setOperands)) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitable#accept(org.modeshape.graph.query.model.Visitor)
     */
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}
