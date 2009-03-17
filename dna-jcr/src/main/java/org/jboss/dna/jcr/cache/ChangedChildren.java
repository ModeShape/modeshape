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
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.ImmutableChildren#without(org.jboss.dna.jcr.cache.ChildNode,
     *      org.jboss.dna.graph.property.PathFactory)
     */
    @Override
    public ChangedChildren without( ChildNode child,
                                    PathFactory pathFactory ) {
        // Make sure this object contains the child ...
        if (!childrenByUuid.containsKey(child.getUuid())) {
            return this;
        }
        // Remove the child fro this object, then adjust the remaining child node instances that follow it ...
        Name childName = child.getName();
        List<ChildNode> childrenWithSameName = childrenByName.get(childName);
        int snsIndex = child.getSnsIndex();
        if (snsIndex > childrenWithSameName.size()) {
            // The child node (with that SNS index) is no longer here) ...
            return this;
        }
        ListIterator<ChildNode> iter = childrenWithSameName.listIterator(--snsIndex);
        assert iter.hasNext();
        iter.next(); // start ...
        iter.remove(); // removes the item that was last returned from 'next()'
        while (iter.hasNext()) {
            ChildNode next = iter.next();
            ChildNode newNext = next.with(pathFactory.createSegment(childName, ++snsIndex));
            iter.set(newNext);
        }
        return this;
    }

}
