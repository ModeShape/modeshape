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

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.jcr.NodeDefinitionId;

/**
 * The information that describes a node. This is the information that is kept in the cache.
 */
@Immutable
public class ImmutableNodeInfo implements NodeInfo {
    private final Location originalLocation;
    private final UUID uuid;
    private final UUID parent;
    private final Name primaryTypeName;
    private final NodeDefinitionId definition;
    private final Children children;
    private final Map<Name, PropertyInfo> properties;
    private final Set<Name> mixinTypeNames;

    /**
     * Create an immutable NodeInfo instance.
     * 
     * @param originalLocation the original location
     * @param primaryTypeName the name of the node's primary type
     * @param mixinTypeNames the names of the mixin types for this node, or null if there are none
     * @param definition the definition used when creating the node
     * @param parent the parent
     * @param children the immutable children; may be null if there are no children
     * @param properties the unmodifiable map of properties; may be null if there are no properties
     */
    public ImmutableNodeInfo( Location originalLocation,
                              Name primaryTypeName,
                              Set<Name> mixinTypeNames,
                              NodeDefinitionId definition,
                              UUID parent,
                              Children children,
                              Map<Name, PropertyInfo> properties ) {
        this.originalLocation = originalLocation;
        this.primaryTypeName = primaryTypeName;
        this.definition = definition;
        this.parent = parent;
        this.uuid = this.originalLocation.getUuid();
        this.children = children != null ? children : new EmptyChildren(this.uuid);
        if (properties == null) properties = Collections.emptyMap();
        this.properties = properties;
        if (mixinTypeNames == null) mixinTypeNames = Collections.emptySet();
        this.mixinTypeNames = mixinTypeNames;
        assert this.uuid != null;
        assert this.definition != null;
        assert this.primaryTypeName != null;
        assert this.children != null;
        assert this.mixinTypeNames != null;
        assert this.properties != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.NodeInfo#getOriginalLocation()
     */
    public Location getOriginalLocation() {
        return originalLocation;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.NodeInfo#getUuid()
     */
    public UUID getUuid() {
        return uuid;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.NodeInfo#getParent()
     */
    public UUID getParent() {
        return parent;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.NodeInfo#getPrimaryTypeName()
     */
    public Name getPrimaryTypeName() {
        return primaryTypeName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.NodeInfo#getMixinTypeNames()
     */
    public Set<Name> getMixinTypeNames() {
        return mixinTypeNames;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.NodeInfo#getDefinitionId()
     */
    public NodeDefinitionId getDefinitionId() {
        return definition;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.NodeInfo#getChildren()
     */
    public Children getChildren() {
        return children;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.NodeInfo#hasProperties()
     */
    public boolean hasProperties() {
        return this.properties.size() != 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.NodeInfo#getPropertyCount()
     */
    public int getPropertyCount() {
        return this.properties.size();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.NodeInfo#getPropertyNames()
     */
    public Set<Name> getPropertyNames() {
        return this.properties.keySet();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.jcr.cache.NodeInfo#getProperty(org.jboss.dna.graph.property.Name)
     */
    public PropertyInfo getProperty( Name name ) {
        return this.properties.get(name);
    }
}
