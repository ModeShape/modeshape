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

import java.util.List;
import java.util.ListIterator;
import java.util.UUID;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;

/**
 * A {@link NotThreadSafe non-thread safe} implementation of {@link Children} that can be modified in place. This is typically
 * used to capture changes made within a session.
 */
@NotThreadSafe
public class ChangedChildren extends ImmutableChildren {

    public ChangedChildren( Children original ) {
        super(original);
    }

    /**
     * Creates an empty instance.
     * 
     * @param parentUuid the UUID of the parent node
     */
    protected ChangedChildren( UUID parentUuid ) {
        super(parentUuid);
    }

    protected ChangedChildren( ImmutableChildren original,
                                 Name additionalChildName,
                                 Path.Segment beforeChild,
                                 UUID childUuid,
                                 PathFactory pathFactory ) {
        super(original, additionalChildName, beforeChild, childUuid, pathFactory);
    }
    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.ImmutableChildren#with(org.jboss.dna.graph.property.Name, java.util.UUID,
     *      org.jboss.dna.graph.property.PathFactory)
     */
    @Override
    public ChangedChildren with( Name newChildName,
                                 UUID newChildUuid,
                                 PathFactory pathFactory ) {
        // Simply add the node to this object ...
        super.add(newChildName, newChildUuid, pathFactory);
        return this;
    }

    /**
     * Create another Children object that is equivalent to this node but with the supplied child added before the named node.
     * 
     * @param newChildName the name of the new child; may not be null
     * @param beforeChild the path segment of the child before which this node should be added; may not be null
     * @param newChildUuid the UUID of the new child; may not be null
     * @param pathFactory the factory that can be used to create Path and/or Path.Segment instances.
     * @return the new Children object; never null
     */
    public ChangedChildren with( Name newChildName,
                                 Path.Segment beforeChild,
                                 UUID newChildUuid,
                                 PathFactory pathFactory ) {
        return new ChangedChildren(this, newChildName, beforeChild, newChildUuid, pathFactory);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.ImmutableChildren#without(java.util.UUID, org.jboss.dna.graph.property.PathFactory)
     */
    @Override
    public ChangedChildren without( UUID childUuid,
                                    PathFactory pathFactory ) {
        // Remove the object that has the same UUID (regardless of the current SNS index) ...
        ChildNode toBeRemoved = childrenByUuid.get(childUuid);
        if (toBeRemoved == null) {
            return this;
        }
        // Remove the child from this object, then adjust the remaining child node instances that follow it ...
        Name childName = toBeRemoved.getName();
        List<ChildNode> childrenWithSameName = childrenByName.get(childName);
        int snsIndex = toBeRemoved.getSnsIndex();
        if (snsIndex > childrenWithSameName.size()) {
            // The child node (with that SNS index) is no longer here) ...
            return this;
        }
        ListIterator<ChildNode> iter = childrenWithSameName.listIterator(--snsIndex);
        assert iter.hasNext();
        ChildNode willBeRemoved = iter.next();
        assert willBeRemoved == toBeRemoved;
        childrenByUuid.remove(toBeRemoved.getUuid());
        iter.remove(); // removes the item that was last returned from 'next()'
        while (iter.hasNext()) {
            ChildNode next = iter.next();
            ChildNode newNext = next.with(pathFactory.createSegment(childName, ++snsIndex));
            childrenByUuid.put(newNext.getUuid(), newNext);
            iter.set(newNext);
        }
        return this;
    }

}
