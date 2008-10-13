/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.requests;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connectors.RepositoryConnection;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Property;

/**
 * Instruction to read the properties and children of the nodes in the branch at the supplied location. The children of the nodes
 * at the bottom of the branch are not read.
 * 
 * @author Randall Hauch
 */
@NotThreadSafe
public class ReadBranchRequest extends Request implements Iterable<Location> {

    public static final int DEFAULT_MAXIMUM_DEPTH = 2;

    private final Location at;
    private final int maxDepth;
    private final Map<Location, Map<Name, Property>> nodeProperties = new HashMap<Location, Map<Name, Property>>();
    private final Map<Location, List<Location>> children = new HashMap<Location, List<Location>>();

    /**
     * Create a request to read the branch at the supplied location, to a maximum depth of 2.
     * 
     * @param at the location of the branch
     * @throws IllegalArgumentException if the location is null
     */
    public ReadBranchRequest( Location at ) {
        CheckArg.isNotNull(at, "at");
        this.at = at;
        this.maxDepth = DEFAULT_MAXIMUM_DEPTH;
    }

    /**
     * Create a request to read the branch (of given depth) at the supplied location.
     * 
     * @param at the location of the branch
     * @param maxDepth the maximum depth to read
     * @throws IllegalArgumentException if the location is null or if the maximum depth is not positive
     */
    public ReadBranchRequest( Location at,
                              int maxDepth ) {
        CheckArg.isNotNull(at, "at");
        CheckArg.isPositive(maxDepth, "maxDepth");
        this.at = at;
        this.maxDepth = maxDepth;
    }

    /**
     * Get the location defining the top of the branch to be deleted
     * 
     * @return the location of the branch; never null
     */
    public Location at() {
        return at;
    }

    /**
     * Get the maximum depth of the branch that is to be read.
     * 
     * @return the maximum depth; always positive
     */
    public int maximumDepth() {
        return maxDepth;
    }

    /**
     * Return whether this branch contains the specified location.
     * 
     * @param location the location
     * @return true if this branch includes the location, or false otherwise
     */
    public boolean includes( Location location ) {
        return this.nodeProperties.containsKey(location);
    }

    /**
     * Add a node that was read from the {@link RepositoryConnection}. This method does not verify or check that the node is
     * indeed on the branch and that it is at a level prescribed by the request.
     * 
     * @param node the location of the node that appears on this branch.
     * @param properties the properties on the node
     * @throws IllegalArgumentException if the node is null
     */
    public void setProperties( Location node,
                               Property... properties ) {
        CheckArg.isNotNull(node, "node");
        Map<Name, Property> propertiesMap = nodeProperties.get(node);
        if (propertiesMap == null) {
            propertiesMap = new HashMap<Name, Property>();
            nodeProperties.put(node, propertiesMap);
        }
        for (Property property : properties) {
            propertiesMap.put(property.getName(), property);
        }
    }

    /**
     * Add a node that was read from the {@link RepositoryConnection}. This method does not verify or check that the node is
     * indeed on the branch and that it is at a level prescribed by the request.
     * 
     * @param node the location of the node that appears on this branch.
     * @param properties the properties on the node
     * @throws IllegalArgumentException if the node is null
     */
    public void setProperties( Location node,
                               Iterable<Property> properties ) {
        CheckArg.isNotNull(node, "node");
        Map<Name, Property> propertiesMap = nodeProperties.get(node);
        if (propertiesMap == null) {
            propertiesMap = new HashMap<Name, Property>();
            nodeProperties.put(node, propertiesMap);
        }
        for (Property property : properties) {
            propertiesMap.put(property.getName(), property);
        }
    }

    /**
     * Record the children for a parent node in the branch.
     * 
     * @param parent the location of the parent
     * @param children the location of each child, in the order they appear in the parent
     */
    public void setChildren( Location parent,
                             Location... children ) {
        CheckArg.isNotNull(parent, "parent");
        CheckArg.isNotNull(children, "children");
        this.children.put(parent, Arrays.asList(children));
    }

    /**
     * Record the children for a parent node in the branch.
     * 
     * @param parent the location of the parent
     * @param children the location of each child, in the order they appear in the parent
     */
    public void setChildren( Location parent,
                             List<Location> children ) {
        CheckArg.isNotNull(parent, "parent");
        CheckArg.isNotNull(children, "children");
        this.children.put(parent, children);
    }

    /**
     * Get the nodes that make up this branch. If this map is empty, the branch has not yet been read. The resulting map maintains
     * the order that the nodes were {@link #setProperties(Location, Property...) added}.
     * 
     * @return the branch information
     * @see #iterator()
     */
    public Map<Location, Map<Name, Property>> getPropertiesByNode() {
        return nodeProperties;
    }

    /**
     * Get the nodes that make up this branch. If this map is empty, the branch has not yet been read. The resulting map maintains
     * the order that the nodes were {@link #setProperties(Location, Property...) added}.
     * 
     * @param location the location of the node for which the properties are to be obtained
     * @return the properties for the location, as a map keyed by the property name
     * @see #iterator()
     */
    public Map<Name, Property> getPropertiesFor( Location location ) {
        return nodeProperties.get(location);
    }

    /**
     * Get the children of the node at the supplied location.
     * 
     * @param parent the location of the parent
     * @return the children, or null if there are no children (or if the parent has not been read)
     */
    public List<Location> getChildren( Location parent ) {
        CheckArg.isNotNull(parent, "parent");
        return this.children.get(parent);
    }

    /**
     * {@inheritDoc}
     * <p>
     * The resulting iterator accesses the {@link Location} objects in the branch, in pre-order traversal order.
     * </p>
     * 
     * @see java.lang.Iterable#iterator()
     */
    public Iterator<Location> iterator() {
        final LinkedList<Location> queue = new LinkedList<Location>();
        queue.addFirst(at());
        return new Iterator<Location>() {
            public boolean hasNext() {
                return queue.peek() != null;
            }

            public Location next() {
                // Add the children of the next node to the queue ...
                Location next = queue.poll();
                List<Location> children = getChildren(next);
                if (children != null && children.size() > 0) queue.addAll(0, children);
                return next;
            }

            public void remove() {
                throw new UnsupportedOperationException();
            }
        };
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (this.getClass().isInstance(obj)) {
            ReadBranchRequest that = (ReadBranchRequest)obj;
            if (!this.at().equals(that.at())) return false;
            if (this.maximumDepth() != that.maximumDepth()) return false;
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
        return "read branch " + at() + " to depth " + maximumDepth();
    }
}
