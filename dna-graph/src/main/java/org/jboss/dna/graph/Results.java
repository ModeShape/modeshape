/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.graph;

import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.properties.Path;

/**
 * A set of nodes returned from a {@link Graph graph}, with methods to access the properties and children of the nodes in the
 * result. The {@link #iterator()} method can be used to iterate all over the nodes in the result.
 * 
 * @author Randall Hauch
 */
@Immutable
public interface Results extends Iterable<Node> {

    /**
     * Get the graph containing the node.
     * 
     * @return the graph
     */
    Graph getGraph();

    /**
     * Get the node at the supplied location.
     * 
     * @param path the path of the node in these results
     * @return the node, or null if the node is not {@link #includes(Path) included} in these results
     */
    Node getNode( String path );

    /**
     * Get the node at the supplied location.
     * 
     * @param path the path of the node in these results
     * @return the node, or null if the node is not {@link #includes(Path) included} in these results
     */
    Node getNode( Path path );

    /**
     * Get the node at the supplied location.
     * 
     * @param location the location of the node
     * @return the node, or null if the node is not {@link #includes(Path) included} in these results
     */
    Node getNode( Location location );

    /**
     * Return whether these results include a node at the supplied location.
     * 
     * @param path the path of the node in these results
     * @return true if this subgraph includes the supplied location, or false otherwise
     */
    boolean includes( String path );

    /**
     * Return whether this subgraph has a node at the supplied location.
     * 
     * @param path the path of the node in these results
     * @return true if these results includes the supplied location, or false otherwise
     */
    boolean includes( Path path );

    /**
     * Return whether this subgraph has a node at the supplied location.
     * 
     * @param location the location of the node in these results
     * @return true if these results includes the supplied location, or false otherwise
     */
    boolean includes( Location location );

}
