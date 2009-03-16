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
package org.jboss.dna.jcr;

import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.graph.property.Name;

/**
 * An immutable identifier for a property, often used to reference information a property held within the {@link SessionCache}.
 */
@Immutable
public final class PropertyId {

    private final UUID nodeId;
    private final Name propertyName;
    private final int hc;

    /**
     * Create a new property identifier.
     * 
     * @param nodeId the UUID of the node that owns the property being reference; may not be null
     * @param propertyName the name of the property being referenced; may not be null
     */
    public PropertyId( UUID nodeId,
                       Name propertyName ) {
        assert nodeId != null;
        assert propertyName != null;
        this.nodeId = nodeId;
        this.propertyName = propertyName;
        this.hc = HashCode.compute(this.nodeId, this.propertyName);
    }

    /**
     * Get the UUID of the node that owns the property.
     * 
     * @return the node's UUID; never null
     */
    public UUID getNodeId() {
        return nodeId;
    }

    /**
     * Get the name of the property.
     * 
     * @return the property name; never null
     */
    public Name getPropertyName() {
        return propertyName;
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
        if (obj instanceof PropertyId) {
            PropertyId that = (PropertyId)obj;
            if (this.hc != that.hc) return false;
            if (!this.nodeId.equals(that.nodeId)) return false;
            return this.propertyName.equals(that.propertyName);
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
        return this.nodeId.toString() + '@' + this.propertyName.toString();
    }

}
