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
package org.modeshape.graph;

import java.util.List;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.request.Request;

/**
 * A set of nodes returned from a {@link Graph graph}, with methods to access the properties and children of the nodes in the
 * result. The {@link #iterator()} method can be used to iterate all over the nodes in the result.
 */
@Immutable
public interface Results extends Graph.BaseResults<Node> {

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

    /**
     * Get the requests that were executed as part of these results.
     * 
     * @return the requests; never null, but possibly empty if there were no results when execute was called
     */
    List<Request> getRequests();

}
