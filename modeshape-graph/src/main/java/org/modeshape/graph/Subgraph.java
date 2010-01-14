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

import net.jcip.annotations.Immutable;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;

/**
 * A subgraph returned by the {@link Graph}, containing the nodes in the subgraph as well as the properties and children for each
 * of those nodes. The {@link #iterator()} method may be used to walk the nodes in the subgraph in a pre-order traversal.
 * <p>
 * Since this subgraph has a single {@link #getLocation() node that is the top of the subgraph}, the methods that take a String
 * path or {@link Path path object} will accept absolute or relative paths.
 * </p>
 * <p>
 * This subgraph will not contain any {@link #iterator() nodes} that exist below the {@link #getMaximumDepth() maximum depth}.
 * Also, all nodes included in the subgraph have all their properties and children. However, nodes that are at the maximum depth
 * of the subgraph will contain the locations for child nodes that are below the maximum depth and therefore not included in this
 * subgraph.
 * </p>
 */
@Immutable
public interface Subgraph extends Graph.BaseResults<SubgraphNode> {

    /**
     * Get the location of the subgraph, which is the location of the node at the top of the subgraph.
     * 
     * @return the location of the top node in the subgraph; never null
     */
    Location getLocation();

    /**
     * Get the maximum depth requested for this subgraph. The actual subgraph may not be as deep, but will never be deeper than
     * this value.
     * 
     * @return the maximum depth requested; always positive
     */
    int getMaximumDepth();

    /**
     * Get the node at the supplied location.
     * 
     * @param relativePath the name that makes up a relative path to the node that is an immediate child of the {@link #getRoot()
     *        root}
     * @return the node, or null if the node is not {@link #includes(Path) included} in these results
     */
    SubgraphNode getNode( Name relativePath );

    /**
     * Get the node that is at the {@link #getLocation() root} of the subgraph.
     * 
     * @return the root node in the subgraph
     */
    SubgraphNode getRoot();

}
