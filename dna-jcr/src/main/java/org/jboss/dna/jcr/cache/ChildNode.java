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
 * 
 */
/**
 * The information about a child node. This is designed to be found in the {@link Children}, used quickly, and discarded. Clients
 * should not hold on to these objects, since any changes to the children involve discarding the old ChildNode objects and
 * replacing them with new instances.
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
