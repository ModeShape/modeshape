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

import java.util.Set;
import java.util.UUID;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.jcr.NodeDefinitionId;

/**
 * The information that describes a node. This is the information that is kept in the cache.
 */
public interface NodeInfo {

    /**
     * @return location
     */
    public Location getOriginalLocation();

    /**
     * @return uuid
     */
    public UUID getUuid();

    /**
     * @return parent
     */
    public UUID getParent();

    /**
     * @return primaryTypeName
     */
    public Name getPrimaryTypeName();

    /**
     * @return definition
     */
    public NodeDefinitionId getDefinitionId();

    /**
     * Get the children for this node.
     * 
     * @return the immutable children; never null but possibly empty
     */
    public Children getChildren();

    /**
     * Return true of this node has at least one property.
     * 
     * @return true if there is at least one property, or false if there are none
     */
    public boolean hasProperties();

    /**
     * Get the names of the properties that are owned by this node.
     * 
     * @return the unmodifiable set of property names
     */
    public Set<Name> getPropertyNames();

    /**
     * Get this node's property that has the supplied name.
     * 
     * @param name the property name; may not be null
     * @return the property information, or null if this node has no property with the supplied name
     */
    public PropertyInfo getProperty( Name name );
}
