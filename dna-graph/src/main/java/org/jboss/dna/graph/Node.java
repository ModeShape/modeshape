/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
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
package org.jboss.dna.graph;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.Property;

/**
 * A node in a {@link Graph graph}, with methods to access the properties and children.
 * 
 * @author Randall Hauch
 */
@Immutable
public interface Node extends Iterable<Location> {

    /**
     * Get the graph containing the node.
     * 
     * @return the graph
     */
    Graph getGraph();

    /**
     * Get the location of the node.
     * 
     * @return the node's location
     */
    Location getLocation();

    /**
     * Get the properties on the node.
     * 
     * @return the properties
     */
    Collection<Property> getProperties();

    /**
     * Get the property with the supplied name.
     * 
     * @param name the property name
     * @return the property, or null if there is no property by that name
     */
    Property getProperty( String name );

    /**
     * Get the property with the supplied name.
     * 
     * @param name the property name
     * @return the property, or null if there is no property by that name
     */
    Property getProperty( Name name );

    /**
     * Get the map of properties keyed by the property names.
     * 
     * @return the map of properties keyed by property name
     */
    Map<Name, Property> getPropertiesByName();

    /**
     * Get the children of the node.
     * 
     * @return the list of locations for each child
     */
    List<Location> getChildren();

    /**
     * Get the list of child {@link Path.Segment segments}.
     * 
     * @return the list containing a segment for each child
     */
    List<Path.Segment> getChildrenSegments();

    /**
     * Return whether this node has children.
     * 
     * @return true if the node has children, or false otherwise
     */
    boolean hasChildren();

}
