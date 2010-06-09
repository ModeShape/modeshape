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

import java.util.Set;
import net.jcip.annotations.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;

/**
 * A dynamic operand that evaluates to the value(s) of a property on a selector, used in a {@link Comparison} constraint.
 */
@Immutable
public class PropertyValue implements DynamicOperand {
    private static final long serialVersionUID = 1L;

    private final Set<SelectorName> selectorNames;
    private final String propertyName;
    private final int hc;

    /**
     * Create a dynamic operand that evaluates to the property values of the node identified by the selector.
     * 
     * @param selectorName the name of the selector
     * @param propertyName the name of the property
     * @throws IllegalArgumentException if the selector name or property name are null
     */
    public PropertyValue( SelectorName selectorName,
                          String propertyName ) {
        this.selectorNames = SelectorName.nameSetFrom(selectorName);
        CheckArg.isNotNull(propertyName, "propertyName");
        this.propertyName = propertyName;
        this.hc = HashCode.compute(selectorName, this.propertyName);
    }

    /**
     * Get the selector symbol upon which this operand applies.
     * 
     * @return the one selector names used by this operand; never null
     */
    public SelectorName selectorName() {
        return selectorNames().iterator().next();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.DynamicOperand#selectorNames()
     */
    public Set<SelectorName> selectorNames() {
        return selectorNames;
    }

    /**
     * Get the name of the property.
     * 
     * @return the property name; never null
     */
    public final String propertyName() {
        return propertyName;
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
        return hc;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof PropertyValue) {
            PropertyValue that = (PropertyValue)obj;
            if (this.hc != that.hc) return false;
            return this.selectorNames().equals(that.selectorNames()) && this.propertyName.equals(that.propertyName);
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
