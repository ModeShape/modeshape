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

import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;

/**
 * The representation of a child node. This is an immutable representation of a child node within the collection of its siblings
 * as the collection appeared at some point in time. This should be used as a guide to determine how long to hold onto references.
 * <p>
 * For example, adding and removing children may affect the {@link #getSnsIndex() same-name-sibling index} of the children, so
 * these kinds of operations will result in the replacement of old ChildObject instances. Therefore, clients should generally find
 * the ChildNode instances in a {@link Children} container, use the ChildNode objects quickly, then discard their references.
 * </p>
 * <p>
 * There may be times when a client does wish to keep a representation of a ChildNode as it appeared at some moment in time, and
 * so it may want to hold onto references to ChildNode objects for longer durations. This is fine, as long as it is understood
 * that at some point the referenced ChildNode may no longer represent the current state.
 * </p>
 */
@Immutable
public final class ChildNode {

    private final UUID uuid;
    private final Path.Segment segment;

    public ChildNode( UUID uuid,
                      Path.Segment segment ) {
        assert uuid != null;
        assert segment != null;
        this.uuid = uuid;
        this.segment = segment;
    }

    /**
     * Get the UUID of the node.
     * 
     * @return the node's UUID; never null
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * Get the path segment for this node.
     * 
     * @return the path segment; never null
     */
    public Path.Segment getSegment() {
        return segment;
    }

    /**
     * Get the name of the node.
     * 
     * @return the node's current name; never null
     */
    public Name getName() {
        return segment.getName();
    }

    /**
     * Get the same-name-sibling index of the node.
     * 
     * @return the node's SNS index; always positive
     */
    public int getSnsIndex() {
        return segment.getIndex();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return uuid.hashCode();
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
            return this.uuid.equals(((ChildNode)obj).uuid);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return uuid.toString() + " ( " + getSegment() + " )";
    }

    /**
     * Obtain a new instance that uses the same {@link #getUuid() UUID} but the supplied path segment.
     * 
     * @param newSegment the new segment; may not be null
     * @return the new instance; never null
     */
    public ChildNode with( Path.Segment newSegment ) {
        return new ChildNode(uuid, newSegment);
    }

}
