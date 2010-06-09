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

import net.jcip.annotations.Immutable;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;

/**
 * A constraint that evaluates to true only when a named property exists on a node.
 */
@Immutable
public class PropertyExistence implements Constraint {
    private static final long serialVersionUID = 1L;

    private final SelectorName selectorName;
    private final String propertyName;
    private final int hc;

    /**
     * Create a constraint requiring that a property exist on a node.
     * 
     * @param selectorName the name of the node selector
     * @param propertyName the name of the property that must exist
     */
    public PropertyExistence( SelectorName selectorName,
                              String propertyName ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotNull(propertyName, "propertyName");
        this.selectorName = selectorName;
        this.propertyName = propertyName;
        this.hc = HashCode.compute(this.selectorName, this.propertyName);
    }

    /**
     * Get the name of the selector.
     * 
     * @return the selector name; never null
     */
    public final SelectorName selectorName() {
        return selectorName;
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
        if (obj instanceof PropertyExistence) {
            PropertyExistence that = (PropertyExistence)obj;
            if (this.hc != that.hc) return false;
            return this.selectorName.equals(that.selectorName) && this.propertyName.equals(that.propertyName);
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
