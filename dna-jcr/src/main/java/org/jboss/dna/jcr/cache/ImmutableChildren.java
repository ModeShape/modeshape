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

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.collection.ReadOnlyIterator;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Path.Segment;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * An immutable implementation of {@link Children}.
 */
@Immutable
public class ImmutableChildren implements Children, InternalChildren {
    protected final UUID parentUuid;
    protected final Map<UUID, ChildNode> childrenByUuid;
    protected final ListMultimap<Name, ChildNode> childrenByName;

    public ImmutableChildren( UUID parentUuid,
                              Iterable<Location> children ) {
        this(parentUuid);
        for (Location childLocation : children) {
            UUID childUuid = childLocation.getUuid();
            Path.Segment segment = childLocation.getPath().getLastSegment();
            Name name = segment.getName();
            ChildNode child = new ChildNode(childUuid, segment);
            this.childrenByName.put(name, child);
            this.childrenByUuid.put(childUuid, child);
        }
    }

    public ImmutableChildren( UUID parentUuid ) {
        this.parentUuid = parentUuid;
        this.childrenByUuid = new HashMap<UUID, ChildNode>();
        this.childrenByName = new LinkedListMultimap<Name, ChildNode>();
    }

    protected ImmutableChildren( Children original ) {
        this.parentUuid = original.getParentUuid();
        this.childrenByUuid = new HashMap<UUID, ChildNode>();
        this.childrenByName = new LinkedListMultimap<Name, ChildNode>();
        Iterator<ChildNode> iter = original.iterator();
        while (iter.hasNext()) {
            ChildNode child = iter.next();
            this.childrenByName.put(child.getName(), child);
            this.childrenByUuid.put(child.getUuid(), child);
        }
    }

    protected ImmutableChildren( Children original,
                                 Name additionalChildName,
                                 UUID childUuid,
                                 PathFactory pathFactory ) {
        this(original);
        add(additionalChildName, childUuid, pathFactory);
    }

    /**
     * Utility method that adds a child with the supplied name. This method is not exposed publicly, ensuring that this class
     * remains publicly immutable. Subclasses that use this method (in places other than constructors) will no longer be
     * {@link Immutable immutable}.
     * 
     * @param additionalChildName
     * @param childUuid
     * @param pathFactory
     * @return the child node that was just added; never null
     */
    protected ChildNode add( Name additionalChildName,
                             UUID childUuid,
                             PathFactory pathFactory ) {
        ChildNode existing = this.childrenByUuid.get(childUuid);
        if (existing != null) return existing;

        List<ChildNode> childrenWithName = this.childrenByName.get(additionalChildName);
        Path.Segment segment = pathFactory.createSegment(additionalChildName, childrenWithName.size() + 1);
        ChildNode additionalChild = new ChildNode(childUuid, segment);
        this.childrenByName.put(additionalChildName, additionalChild);
        this.childrenByUuid.put(childUuid, additionalChild);
        return additionalChild;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.Children#size()
     */
    public int size() {
        return childrenByName.size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<ChildNode> iterator() {
        return new ReadOnlyIterator<ChildNode>(this.childrenByName.values().iterator());
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
        return this.childrenByUuid.get(uuid);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.Children#getChild(org.jboss.dna.graph.property.Path.Segment)
     */
    public ChildNode getChild( Segment segment ) {
        List<ChildNode> childrenWithName = this.childrenByName.get(segment.getName());
        int snsIndex = segment.getIndex();
        if (childrenWithName.size() < snsIndex) return null;
        return childrenWithName.get(snsIndex - 1);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.Children#getChildren(org.jboss.dna.graph.property.Name)
     */
    public Iterator<ChildNode> getChildren( Name name ) {
        return new ReadOnlyIterator<ChildNode>(this.childrenByName.get(name).iterator());
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
        // Create a mutable version ...
        ChangedChildren newChildren = new ChangedChildren(this);
        return newChildren.with(newChildName, newChildUuid, pathFactory);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.InternalChildren#without(java.util.UUID, org.jboss.dna.graph.property.PathFactory)
     */
    public ChangedChildren without( UUID childUuid,
                                    PathFactory pathFactory ) {
        if (this.childrenByUuid.containsKey(childUuid) && this.size() == 1) {
            return new ChangedChildren(this.parentUuid);
        }
        ChangedChildren newChildren = new ChangedChildren(this);
        return newChildren.without(childUuid, pathFactory);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (ChildNode child : childrenByName.values()) {
            if (!first) sb.append(", ");
            else first = false;
            sb.append(child.getName()).append('[').append(child.getSnsIndex()).append(']');
        }
        return sb.toString();
    }
}
