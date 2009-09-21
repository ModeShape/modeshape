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
package org.jboss.dna.graph.query.model;

import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.property.Name;

/**
 * A join condition that tests whether a property on a node is equal to a property on another node. A node-tuple satisfies the
 * constraint only if:
 * <ul>
 * <li>the {@code selector1Name} node has a property named {@code property1Name}, and</li>
 * <li>the {@code selector2Name} node has a property named {@code property2Name}, and</li>
 * <li>the value of property {@code property1Name} is equal to the value of property {@code property2Name}</li>
 * </ul>
 */
@Immutable
public class EquiJoinCondition extends JoinCondition {
    private final SelectorName selector1Name;
    private final Name property1Name;
    private final SelectorName selector2Name;
    private final Name property2Name;

    public EquiJoinCondition( SelectorName selector1Name,
                              Name property1Name,
                              SelectorName selector2Name,
                              Name property2Name ) {
        CheckArg.isNotNull(selector1Name, "selector1Name");
        CheckArg.isNotNull(property1Name, "property1Name");
        CheckArg.isNotNull(selector2Name, "selector2Name");
        CheckArg.isNotNull(property2Name, "property2Name");
        this.selector1Name = selector1Name;
        this.property1Name = property1Name;
        this.selector2Name = selector2Name;
        this.property2Name = property2Name;
    }

    public EquiJoinCondition( Column column1,
                              Column column2 ) {
        this(column1.getSelectorName(), column1.getPropertyName(), column2.getSelectorName(), column2.getPropertyName());
    }

    /**
     * @return selector1Name
     */
    public final SelectorName getSelector1Name() {
        return selector1Name;
    }

    /**
     * @return property1Name
     */
    public final Name getProperty1Name() {
        return property1Name;
    }

    /**
     * @return selector2Name
     */
    public final SelectorName getSelector2Name() {
        return selector2Name;
    }

    /**
     * @return property2Name
     */
    public final Name getProperty2Name() {
        return property2Name;
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
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof EquiJoinCondition) {
            EquiJoinCondition that = (EquiJoinCondition)obj;
            if (!this.selector1Name.equals(that.selector1Name)) return false;
            if (!this.selector2Name.equals(that.selector2Name)) return false;
            if (!this.property1Name.equals(that.property1Name)) return false;
            if (!this.property2Name.equals(that.property2Name)) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.model.Visitable#accept(org.jboss.dna.graph.query.model.Visitor)
     */
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }
}
