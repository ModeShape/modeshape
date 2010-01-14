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

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;

public interface MapNode {

    /**
     * Returns the UUID for this node
     * 
     * @return the UUID for this node
     */
    public UUID getUuid();

    /**
     * Returns the name of this node along with its SNS index within its parent's children
     * 
     * @return the name of this node along with its SNS index within its parent's children
     */
    public Path.Segment getName();

    /**
     * @param name Sets name to the specified value.
     */
    public void setName( Path.Segment name );

    /**
     * Returns the set of child names for this node
     * 
     * @return the set of child names for this node
     */
    public Set<Name> getUniqueChildNames();

    /**
     * Returns the parent of this node or null if the node is the root node for its workspace.
     * 
     * @return the parent of this node; may be null if the node is the root node for its workspace
     */
    public MapNode getParent();

    /**
     * @param parent Sets parent to the specified value.
     */
    public void setParent( MapNode parent );

    /**
     * @return children
     */
    public List<MapNode> getChildren();

    /**
     * Removes all of the children for this node in a single operation.
     */
    public void clearChildren();

    /**
     * Adds the given child to the end of the list of children for this node
     * 
     * @param child the child to add to this node
     */
    public void addChild( MapNode child );

    /**
     * Inserts the specified child at the specified position in the list of children. Shifts the child currently at that position
     * (if any) and any subsequent children to the right (adds one to their indices).
     * 
     * @param index index at which the specified child is to be inserted
     * @param child the child to be inserted
     */
    public void addChild( int index,
                          MapNode child );

    /**
     * Removes the given child from the list of children
     * 
     * @param child the child to be removed
     * @return true if the child was one of this node's children (and was removed); false otherwise
     */
    public boolean removeChild( MapNode child );

    /**
     * Returns a map of property names to the property for the given name
     * 
     * @return a map of property names to the property for the given name
     */
    public Map<Name, Property> getProperties();

    /**
     * Sets the given properties in a single operation, overwriting any previous properties for the same name This bulk mutator
     * should be used when multiple properties are being set in order to allow underlying implementations to optimize their access
     * to their respective persistent storage mechanism.
     * 
     * @param properties the properties to set
     * @return this map node
     */
    public MapNode setProperties( Iterable<Property> properties );

    /**
     * Sets the property with the given name, overwriting any previous property for the given name
     * 
     * @param property the property to set
     * @return this map node
     */
    public MapNode setProperty( Property property );

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
                                Object... values );

    /**
     * Removes the property with the given name
     * 
     * @param propertyName the name of the property to remove
     * @return this map node
     */
    public MapNode removeProperty( Name propertyName );

    /**
     * Returns the named property
     * 
     * @param context the current execution context, used to get a {@link NameFactory name factory}
     * @param name the name of the property to return
     * @return the property for the given name
     */
    public Property getProperty( ExecutionContext context,
                                 String name );

    /**
     * Returns the named property
     * 
     * @param name the name of the property to return
     * @return the property for the given name
     */
    public Property getProperty( Name name );

}
