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
package org.modeshape.graph.connector.path;

import java.util.Map;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.NodeConflictBehavior;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.Path.Segment;

/**
 * Extension of {@link PathWorkspace} for repositories that support modification of nodes as well as access to the nodes.
 */
public interface WritablePathWorkspace extends PathWorkspace {

    /**
     * Create a node at the supplied path. The parent of the new node must already exist.
     * 
     * @param context the environment; may not be null
     * @param pathToNewNode the path to the new node; may not be null
     * @param properties the properties for the new node
     * @param conflictBehavior the expected behavior if an equivalently-named child already exists at the location
     * @return the new node (or root if the path specified the root)
     */
    PathNode createNode( ExecutionContext context,
                         String pathToNewNode,
                         Map<Name, Property> properties,
                         NodeConflictBehavior conflictBehavior );

    /**
     * Create a new node with the supplied name, as a child of the supplied parent.
     * 
     * @param context the execution context
     * @param parentNode the parent node; may not be null
     * @param name the name; may not be null
     * @param properties the properties for the new node
     * @param conflictBehavior the expected behavior if an equivalently-named child already exists at the location
     * @return the new node
     */
    PathNode createNode( ExecutionContext context,
                         PathNode parentNode,
                         Name name,
                         Map<Name, Property> properties,
                         NodeConflictBehavior conflictBehavior );

    /**
     * Move the supplied node to the new parent within this workspace. This method automatically removes the node from its
     * existing parent, and also correctly adjusts the {@link Segment#getIndex() index} to be correct in the new parent.
     * 
     * @param context
     * @param node the node to be moved; may not be the workspace root node
     * @param desiredNewName the new name for the node, if it is to be changed; may be null
     * @param originalWorkspace the workspace containing the node to be moved
     * @param newParent the new parent; may not be the workspace root node
     * @param beforeNode the node before which this new node should be placed
     * @return a new copy of {@code node} that reflects the new location
     */
    public PathNode moveNode( ExecutionContext context,
                              PathNode node,
                              Name desiredNewName,
                              WritablePathWorkspace originalWorkspace,
                              PathNode newParent,
                              PathNode beforeNode );

    /**
     * Copy the subgraph given by the original node and place the new copy under the supplied new parent. Note that internal
     * references between nodes within the original subgraph must be reflected as internal nodes within the new subgraph.
     * 
     * @param context the context; may not be null
     * @param original the node to be copied; may not be null
     * @param originalWorkspace the workspace containing the original parent node; may not be null
     * @param newParent the parent where the copy is to be placed; may not be null
     * @param desiredName the desired name for the node; if null, the name will be obtained from the original node
     * @param recursive true if the copy should be recursive
     * @return the new node, which is the top of the new subgraph
     */
    PathNode copyNode( ExecutionContext context,
                       PathNode original,
                       PathWorkspace originalWorkspace,
                       PathNode newParent,
                       Name desiredName,
                       boolean recursive );

    /**
     * Inserts the specified child at the specified position in the list of children. Shifts the child currently at that position
     * (if any) and any subsequent children to the right (adds one to their indices).
     * 
     * @param index index at which the specified child is to be inserted
     * @param child the child to be inserted
     */
    // public void addChild( int index,
    // PathNode child );
    /**
     * Removes the node at the given path
     * 
     * @param context the context; may not be null
     * @param nodePath the path of the node to be removed
     * @return true if the node existed (and was removed); false otherwise
     */
    public boolean removeNode( ExecutionContext context,
                               Path nodePath );

    /**
     * Sets the given properties in a single operation, overwriting any previous properties for the same name. This bulk mutator
     * should be used when multiple properties are being set in order to allow underlying implementations to optimize their access
     * to their respective persistent storage mechanism.
     * 
     * @param context the context; may not be null
     * @param nodePath the path to the node on which the properties should be set
     * @param properties the properties to set
     * @return this map node
     */
    public PathNode setProperties( ExecutionContext context,
                                   Path nodePath,
                                   Map<Name, Property> properties );

    /**
     * Removes the properties with the given names
     * 
     * @param context the context; may not be null
     * @param nodePath the path to the node from which the properties should be removed
     * @param propertyNames the name of the properties to remove
     * @return this map node
     */
    public PathNode removeProperties( ExecutionContext context,
                                      Path nodePath,
                                      Iterable<Name> propertyNames );

}
