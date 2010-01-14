/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.graph.connector.map;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;

public class DefaultMapNode implements MapNode {

    private final UUID uuid;
    private MapNode parent;
    private Path.Segment name;
    private final Map<Name, Property> properties = new HashMap<Name, Property>();
    private final LinkedList<MapNode> children = new LinkedList<MapNode>();
    final Set<Name> existingNames = new HashSet<Name>();

    public DefaultMapNode( UUID uuid ) {
        assert uuid != null;
        this.uuid = uuid;
    }

    /* (non-Javadoc)
     * @see org.modeshape.graph.connector.map.MapNode#getUuid()
     */
    public UUID getUuid() {
        return uuid;
    }

    /* (non-Javadoc)
     * @see org.modeshape.graph.connector.map.MapNode#getName()
     */
    public Path.Segment getName() {
        return name;
    }

    /**
     * @param name Sets name to the specified value.
     */
    public void setName( Path.Segment name ) {
        this.name = name;
    }

    public Set<Name> getUniqueChildNames() {
        return existingNames;
    }

    /* (non-Javadoc)
     * @see org.modeshape.graph.connector.map.MapNode#getParent()
     */
    public MapNode getParent() {
        return parent;
    }

    /**
     * @param parent Sets parent to the specified value.
     */
    public void setParent( MapNode parent ) {
        this.parent = parent;
    }

    /**
     * @return children
     */
    public LinkedList<MapNode> getChildren() {
        return children;
    }

    /**
     * @return properties
     */
    public Map<Name, Property> getProperties() {
        return properties;
    }

    public void addChild( int index,
                          MapNode child ) {
        children.add(index, child);
    }

    public void addChild( MapNode child ) {
        children.add(child);

    }

    public void clearChildren() {
        children.clear();
    }

    public boolean removeChild( MapNode child ) {
        return children.remove(child);
    }

    public MapNode removeProperty( Name propertyName ) {
        properties.remove(propertyName);
        return this;
    }

    public MapNode setProperties( Iterable<Property> properties ) {
        for (Property property : properties) {
            this.properties.put(property.getName(), property);
        }
        return this;
    }

    /**
     * Sets the property with the given name, overwriting any previous property for the given name
     * 
     * @param property the property to set
     * @return this map node
     */
    public MapNode setProperty( Property property ) {
        if (property != null) {
            this.properties.put(property.getName(), property);
        }
        return this;
    }

    /**
     * Sets the property with the given name, overwriting any previous property for the given name
     * 
     * @param context the current execution context, used to get a {@link NameFactory name factory} and {@link PropertyFactory
     *        property factory}.
     * @param name the name of the property
     * @param values the values for the property
     * @return this map node
     */
    public MapNode setProperty( ExecutionContext context,
                                String name,
                                Object... values ) {
        PropertyFactory propertyFactory = context.getPropertyFactory();
        Name propertyName = context.getValueFactories().getNameFactory().create(name);
        return setProperty(propertyFactory.create(propertyName, values));
    }

    /**
     * Returns the named property
     * 
     * @param context the current execution context, used to get a {@link NameFactory name factory}
     * @param name the name of the property to return
     * @return the property for the given name
     */
    public Property getProperty( ExecutionContext context,
                                 String name ) {
        Name propertyName = context.getValueFactories().getNameFactory().create(name);
        return getProperty(propertyName);
    }

    /**
     * Returns the named property
     * 
     * @param name the name of the property to return
     * @return the property for the given name
     */
    public Property getProperty( Name name ) {
        return this.properties.get(name);
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
        if (obj instanceof MapNode) {
            MapNode that = (MapNode)obj;
            if (!this.getUuid().equals(that.getUuid())) return false;
            return true;
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
        StringBuilder sb = new StringBuilder();
        if (this.name == null) {
            sb.append("");
        } else {
            sb.append(this.name);
        }
        sb.append(" (").append(uuid).append(")");
        return sb.toString();
    }
}
