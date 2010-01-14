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

import java.util.Set;
import java.util.UUID;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.LockFailedException;
import org.modeshape.graph.connector.UuidAlreadyExistsException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.query.QueryResults;
import org.modeshape.graph.request.AccessQueryRequest;
import org.modeshape.graph.request.LockBranchRequest.LockScope;

/**
 * The {@code MapWorkspace} defines the required methods for workspaces in a {@link MapRepository map repository}. By default, a
 * map repository supports multiple workspaces, each equating logically to a map of UUIDs to {@link MapNode map nodes}.
 * <p>
 * As each map node (except the root-node for the workspace) has a non-null parent and a set of children, the {@link MapNode map
 * nodes} naturally construct a tree within the workspace.
 * </p>
 */
public interface MapWorkspace {

    /**
     * Returns the name of the workspace. There can only be one workspace with a given name per repository.
     * 
     * @return the name of the workspace
     */
    String getName();

    /**
     * Returns the root node in the workspace. This returns a {@link MapNode map node} where {@code node.getParent() == null} and
     * {@code node.getUuid() == repository.getRootNodeUuid()}.
     * 
     * @return the root node in the workspace
     */
    MapNode getRoot();

    /**
     * Returns the node with the given UUID, if one exists or {@code null} if no {@MapNode node} with the given UUID
     * exists in the workspace.
     * <p>
     * That is, {@code node == null || node.getUuid().equals(uuid)} for the returned node.
     * </p>
     * 
     * @param uuid the UUID of the node to be retrieved; may not be null
     * @return the node with the given UUID, if one exists or {@code null} if no {@MapNode node} with the given UUID
     *         exists in the workspace.
     */
    MapNode getNode( UUID uuid );

    /**
     * Returns the node at the given path, if one exists of {@code null} if no {@MapNode node} exists at the given path.
     * 
     * @param path the path of the node to retrieve; may not be null
     * @return the node at the given path, if one exists of {@code null} if no {@MapNode node} exists at the given path.
     */
    MapNode getNode( Path path );

    /**
     * Removes the given node. This method will return false if the given node does not exist in this workspace.
     * 
     * @param context the current execution context; may not be null
     * @param node the node to be removed; may not be null
     * @return whether a node was removed as a result of this operation
     */
    boolean removeNode( ExecutionContext context,
                        MapNode node );

    /**
     * Create a node at the supplied path. The parent of the new node must already exist.
     * 
     * @param context the environment; may not be null
     * @param pathToNewNode the path to the new node; may not be null
     * @param properties the properties for the new node
     * @return the new node (or root if the path specified the root)
     */
    MapNode createNode( ExecutionContext context,
                        String pathToNewNode,
                        Iterable<Property> properties );

    /**
     * Create a new node with the supplied name, as a child of the supplied parent.
     * 
     * @param context the execution context
     * @param parentNode the parent node; may not be null
     * @param name the name; may not be null
     * @param uuid the UUID of the node, or null if the UUID is to be generated
     * @param properties the properties for the new node
     * @return the new node
     */
    MapNode createNode( ExecutionContext context,
                        MapNode parentNode,
                        Name name,
                        UUID uuid,
                        Iterable<Property> properties );

    /**
     * Move the supplied node to the new parent. This method automatically removes the node from its existing parent, and also
     * correctly adjusts the {@link Path.Segment#getIndex() index} to be correct in the new parent.
     * 
     * @param context
     * @param node the node to be moved; may not be the {@link MapWorkspace#getRoot() root}
     * @param desiredNewName the new name for the node, if it is to be changed; may be null
     * @param newWorkspace the workspace containing the new parent node
     * @param newParent the new parent; may not be the {@link MapWorkspace#getRoot() root}
     * @param beforeNode the node before which this new node should be placed
     */
    void moveNode( ExecutionContext context,
                   MapNode node,
                   Name desiredNewName,
                   MapWorkspace newWorkspace,
                   MapNode newParent,
                   MapNode beforeNode );

    /**
     * This should copy the subgraph given by the original node and place the new copy under the supplied new parent. Note that
     * internal references between nodes within the original subgraph must be reflected as internal nodes within the new subgraph.
     * 
     * @param context the context; may not be null
     * @param original the node to be copied; may not be null
     * @param newWorkspace the workspace containing the new parent node; may not be null
     * @param newParent the parent where the copy is to be placed; may not be null
     * @param desiredName the desired name for the node; if null, the name will be obtained from the original node
     * @param recursive true if the copy should be recursive
     * @return the new node, which is the top of the new subgraph
     */
    MapNode copyNode( ExecutionContext context,
                      MapNode original,
                      MapWorkspace newWorkspace,
                      MapNode newParent,
                      Name desiredName,
                      boolean recursive );

    /**
     * This should clone the subgraph given by the original node and place the cloned copy under the supplied new parent. Note
     * that internal references between nodes within the original subgraph must be reflected as internal nodes within the new
     * subgraph.
     * 
     * @param context the context; may not be null
     * @param original the node to be cloned; may not be null
     * @param newWorkspace the workspace containing the new parent node; may not be null
     * @param newParent the parent where the clone is to be placed; may not be null
     * @param desiredName the desired name for the node; if null, the name will be calculated from {@code desiredSegment}; Exactly
     *        one of {@code desiredSegment} and {@code desiredName} must be non-null
     * @param desiredSegment the exact segment at which the clone should be rooted; if null, the name will be inferred from
     *        {@code desiredName}; Exactly one of {@code desiredSegment} and {@code desiredName} must be non-null
     * @param removeExisting true if existing nodes in the new workspace with the same UUIDs as nodes in the branch rooted at
     *        {@code original} should be removed; if false, a UuidAlreadyExistsException will be thrown if a UUID conflict is
     *        detected
     * @param removedExistingNodes the set into which should be placed all of the existing nodes that were removed as a result of
     *        this clone operation, or null if these nodes need not be collected
     * @return the new node, which is the top of the new subgraph
     * @throws UuidAlreadyExistsException if {@code removeExisting} is true and and a UUID in the source tree already exists in
     *         the new workspace
     */
    MapNode cloneNode( ExecutionContext context,
                       MapNode original,
                       MapWorkspace newWorkspace,
                       MapNode newParent,
                       Name desiredName,
                       Path.Segment desiredSegment,
                       boolean removeExisting,
                       Set<Location> removedExistingNodes ) throws UuidAlreadyExistsException;

    /**
     * Attempts to lock the given node with the given timeout. If the lock attempt fails, a {@link LockFailedException} will be
     * thrown.
     * 
     * @param node the node to be locked; may not be null
     * @param lockScope the scope of the lock (i.e., whether descendants of {@code node} should be included in the lock
     * @param lockTimeoutInMillis the maximum lifetime of the lock in milliseconds; zero (0) indicates that the connector default
     *        should be used
     * @throws LockFailedException if the implementing connector supports locking but the lock could not be acquired.
     */
    void lockNode( MapNode node,
                   LockScope lockScope,
                   long lockTimeoutInMillis ) throws LockFailedException;

    /**
     * Attempts to unlock the given node.
     * 
     * @param node the node to be unlocked; may not be null
     */
    void unlockNode( MapNode node );

    /**
     * Find the lowest existing node along the path.
     * 
     * @param path the path to the node; may not be null
     * @return the lowest existing node along the path, or the root node if no node exists on the path
     */
    Path getLowestExistingPath( Path path );

    /**
     * Returns the path for the given node with this workspace if one exists, or a {@code null} if no node exists at the given
     * path.
     * 
     * @param pathFactory the path factory to use to construct the path; may not be null
     * @param node the node for which the path should be retrieved; may not be null
     * @return the path for the given node with this workspace if one exists or null if the node does not exist in this workspace
     */
    Path pathFor( PathFactory pathFactory,
                  MapNode node );

    /**
     * Perform a query of this workspace.
     * 
     * @param context the context in which the query is to be executed; may not be null
     * @param accessQuery the access query; may not be null
     * @return the query results, or null if the query is not supported
     */
    QueryResults query( ExecutionContext context,
                        AccessQueryRequest accessQuery );

    /**
     * Perform a full-text search of this workspace.
     * 
     * @param context the context in which the query is to be executed; may not be null
     * @param fullTextSearchExpression the full-text search expression; may not be null
     * @return the query results, or null if the query is not supported
     */
    QueryResults search( ExecutionContext context,
                         String fullTextSearchExpression );
}
