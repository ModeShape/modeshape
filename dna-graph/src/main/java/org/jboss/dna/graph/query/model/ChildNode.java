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
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.graph.property.Path;

/**
 * A constraint requiring that the selected node is a child of the node reachable by the supplied absolute path
 */
@Immutable
public class ChildNode extends Constraint {
    private final SelectorName selectorName;
    private final Path parentPath;
    private final int hc;

    /**
     * Create a constraint requiring that the node identified by the selector is a child of the node reachable by the supplied
     * absolute path.
     * 
     * @param selectorName the name of the selector
     * @param parentPath the absolute path to the parent
     */
    public ChildNode( SelectorName selectorName,
                      Path parentPath ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotNull(parentPath, "parentPath");
        this.selectorName = selectorName;
        this.parentPath = parentPath;
        this.hc = HashCode.compute(this.selectorName, this.parentPath);
    }

    /**
     * @return selectorName
     */
    public final SelectorName getSelectorName() {
        return selectorName;
    }

    /**
     * @return parentPath
     */
    public final Path getParentPath() {
        return parentPath;
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
        if (obj instanceof ChildNode) {
            ChildNode that = (ChildNode)obj;
            if (this.hc != that.hc) return false;
            if (!this.selectorName.equals(that.selectorName)) return false;
            if (!this.parentPath.equals(that.parentPath)) return false;
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
