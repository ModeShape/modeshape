/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
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
package org.jboss.dna.jcr.cache;

import java.util.Iterator;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.collection.EmptyIterator;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Path.Segment;

/**
 * An immutable implementation of {@link Children}.
 */
@Immutable
public final class EmptyChildren implements Children, InternalChildren {

    static final Iterator<ChildNode> EMPTY_ITERATOR = new EmptyIterator<ChildNode>();

    private final UUID parentUuid;

    public EmptyChildren( UUID parentUuid ) {
        this.parentUuid = parentUuid;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.Children#size()
     */
    public int size() {
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<ChildNode> iterator() {
        return EMPTY_ITERATOR;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.Children#getParentUuid()
     */
    public UUID getParentUuid() {
        return parentUuid;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.Children#getChild(java.util.UUID)
     */
    public ChildNode getChild( UUID uuid ) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.Children#getChild(org.jboss.dna.graph.property.Path.Segment)
     */
    public ChildNode getChild( Segment segment ) {
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.Children#getChildren(org.jboss.dna.graph.property.Name)
     */
    public Iterator<ChildNode> getChildren( Name name ) {
        return EMPTY_ITERATOR;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.InternalChildren#with(org.jboss.dna.graph.property.Name, java.util.UUID,
     *      org.jboss.dna.graph.property.PathFactory)
     */
    public ChangedChildren with( Name newChildName,
                                 UUID newChildUuid,
                                 PathFactory pathFactory ) {
        ChangedChildren result = new ChangedChildren(this);
        result.add(newChildName, newChildUuid, pathFactory);
        return result;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.InternalChildren#without(java.util.UUID, org.jboss.dna.graph.property.PathFactory)
     */
    public ChangedChildren without( UUID childUuid,
                                    PathFactory pathFactory ) {
        return new ChangedChildren(this.parentUuid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return "";
    }
}
