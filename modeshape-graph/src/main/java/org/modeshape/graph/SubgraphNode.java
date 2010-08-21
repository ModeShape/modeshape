/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in ModeShape is licensed
 * to you under the terms of the GNU Lesser General Public License as
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

import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;

/**
 * An extended {@link Node} that includes the ability to get nodes in the {@link Subgraph subgraph} relative to this node.
 */
public interface SubgraphNode extends Node {
    /**
     * Get the node at the supplied path that is relative to this node.
     * 
     * @param childName the name of the child node
     * @return the node, or null if the node is not {@link Subgraph#includes(Path) included} in these results
     */
    SubgraphNode getNode( String childName );

    /**
     * Get the node at the supplied path that is relative to this node.
     * 
     * @param childName the name of the child node
     * @return the node, or null if the node is not {@link Subgraph#includes(Path) included} in these results
     */
    SubgraphNode getNode( Name childName );

    /**
     * Get the node at the supplied path that is relative to this node.
     * 
     * @param childSegment the segment for the immediate child of this node
     * @return the node, or null if the node is not {@link Subgraph#includes(Location) included} in these results
     */
    SubgraphNode getNode( Path.Segment childSegment );

    /**
     * Get the node at the supplied path that is relative to this node.
     * 
     * @param relativePath the name that makes up a relative path to the node that is an immediate child of the
     *        {@link Subgraph#getRoot() root}
     * @return the node, or null if the node is not {@link Subgraph#includes(Path) included} in these results
     */
    SubgraphNode getNode( Path relativePath );
}
