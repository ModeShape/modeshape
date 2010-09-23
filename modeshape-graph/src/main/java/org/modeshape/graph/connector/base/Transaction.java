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
package org.modeshape.graph.connector.base;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.LockFailedException;
import org.modeshape.graph.connector.UuidAlreadyExistsException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.query.QueryResults;
import org.modeshape.graph.request.AccessQueryRequest;
import org.modeshape.graph.request.FullTextSearchRequest;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.modeshape.graph.request.LockBranchRequest.LockScope;

/**
 * A transaction in which all read and write operations against a repository are performed. The actual transaction instance is
 * obtained by calling {@link Repository#startTransaction(ExecutionContext,boolean)}.
 * <p>
 * Note that implementations are not required to be thread-safe, since they (and their corresponding {@link Connection}) are
 * expected to be used by a single thread.
 * </p>
 * 
 * @param <NodeType> the type of node
 * @param <WorkspaceType> the type of workspace
 */
@NotThreadSafe
public interface Transaction<NodeType extends Node, WorkspaceType extends Workspace> {

    /**
     * Get the context in which this operator executes.
     * 
     * @return the execution context; never null
     */
    ExecutionContext getContext();

    /**
     * Get the names of the existing workspaces.
     * 
     * @return the immutable set of workspace names; never null
     */
    Set<String> getWorkspaceNames();

    /**
     * Creates a new workspace with the given name containing only a root node. If the workspace already exists, it is left
     * untouched and returned.
     * 
     * @param name the name of the workspace; may not be null
     * @param originalToClone the workspace that should be cloned, or null if the new workspace is to only contain a root node
     * @return the newly created workspace; may not be null
     * @throws InvalidWorkspaceException if the workspace could not be created
     */
    WorkspaceType getWorkspace( String name,
                                WorkspaceType originalToClone ) throws InvalidWorkspaceException;

    /**
     * Destroy the workspace with the supplied name.
     * 
     * @param workspace the workspace that is to be destroyed; may not be null
     * @return true if the workspace was destroyed, or false if the workspace did not exist
     * @throws InvalidWorkspaceException if the workspace could not be destroyed
     */
    boolean destroyWorkspace( WorkspaceType workspace ) throws InvalidWorkspaceException;

    /**
     * Get the root node of the repository workspace.
     * 
     * @param workspace the workspace; may not be null
     * @return the root node; never null
     */
    NodeType getRootNode( WorkspaceType workspace );

    /**
     * Verify that the supplied node exists.
     * 
     * @param workspace the workspace; may not be null
     * @param location of the node; may not be null
     * @return the actual location of the node; never null
     * @throws PathNotFoundException if the node at the given location does not exist
     */
    Location verifyNodeExists( WorkspaceType workspace,
                               Location location );

    /**
     * Find the node with the supplied unique identifier.
     * 
     * @param workspace the workspace; may not be null
     * @param location of the node; may not be null
     * @return the node, or null if there is no node with the supplied identifier
     * @throws PathNotFoundException if the node at the given location could not be found
     */
    NodeType getNode( WorkspaceType workspace,
                      Location location );

    /**
     * Returns the path for the given node with this workspace if one exists, or a {@code null} if no node exists at the given
     * path.
     * 
     * @param workspace the workspace; may not be null
     * @param node the node for which the path should be retrieved; may not be null
     * @return the path for the given node with this workspace if one exists or null if the node does not exist in this workspace
     */
    Path pathFor( WorkspaceType workspace,
                  NodeType node );

    /**
     * Returns the parent of the supplied node. This method returns null if the supplied node is the root node.
     * 
     * @param workspace the workspace; may not be null
     * @param node the child node; may not be null
     * @return the parent of this node; or null if the node is the root node for its workspace
     */
    NodeType getParent( WorkspaceType workspace,
                        NodeType node );

    /**
     * Find in the supplied parent node the child with the supplied name and same-name-sibling index. This method returns null if
     * the parent has no such child.
     * 
     * @param workspace the workspace; may not be null
     * @param parent the parent node; may not be null
     * @param childSegment the segment of the child; may not be null
     * @return the child of this node; or null if no such child exists
     */
    NodeType getChild( WorkspaceType workspace,
                       NodeType parent,
                       Path.Segment childSegment );

    /**
     * Find in the supplied parent node the first child with the supplied name. This method returns null if the parent has no such
     * child.
     * 
     * @param workspace the workspace; may not be null
     * @param parent the parent node; may not be null
     * @param childName the name of the child; may not be null
     * @return the child of this node; or null if no such child exists
     */
    NodeType getFirstChild( WorkspaceType workspace,
                            NodeType parent,
                            Name childName );

    /**
     * Get the children for the supplied node.
     * 
     * @param workspace the workspace; may not be null
     * @param node the node whose children are to be returned; may not be null
     * @return the children, never null but possibly empty
     */
    List<NodeType> getChildren( WorkspaceType workspace,
                                NodeType node );

    /**
     * Get the locations for all children of the supplied node.
     * 
     * @param workspace the workspace; may not be null
     * @param node the node whose children are to be returned; may not be null
     * @return the locations of all children, never null but possibly empty
     */
    List<Location> getChildrenLocations( WorkspaceType workspace,
                                         NodeType node );

    /**
     * Removes all of the children for this node in a single operation.
     * 
     * @param workspace the workspace; may not be null
     * @param node the node whose children are to be removed; may not be null
     */
    void removeAllChildren( WorkspaceType workspace,
                            NodeType node );

    /**
     * Creates a new child node under the supplied parent, where the new child will have the specified name, properties, and
     * (optionally) UUID. The child will be appended to the list of children, and will be given the appropriate same-name-sibling
     * index.
     * 
     * @param workspace the workspace; may not be null
     * @param parent the parent node; may not be null
     * @param name the name; may not be null
     * @param index index at which the specified child is to be inserted, or -1 if the child is to be appended
     * @param uuid the UUID of the node, or null if the UUID is to be generated
     * @param properties the properties for the new node; may be null if there are no other properties
     * @return the representation of the new node
     */
    NodeType addChild( WorkspaceType workspace,
                       NodeType parent,
                       Name name,
                       int index,
                       UUID uuid,
                       Iterable<Property> properties );

    /**
     * Inserts the specified child at the specified position in the list of children. Shifts the child currently at that position
     * (if any) and any subsequent children to the right (adds one to their indices). The child is automatically removed from its
     * existing parent (if it has one), though this method can be used to reorder a child within the same parent.
     * <p>
     * This method can also be used to rename an existing child by 'moving' the child node to the existing parent and a new
     * desired name. However, if no 'beforeOtherChild' is supplied, then the node being renamed will also be moved to the end of
     * the children.
     * </p>
     * 
     * @param workspace the workspace; may not be null
     * @param parent the parent node; may not be null
     * @param newChild the node that is to be added as a child of the parent; may not be null
     * @param beforeOtherChild the existing child before which the child is to be added; may be null if the child is to be added
     *        at the end
     * @param desiredName the desired name for the node; may be null if the new child node's name is to be kept
     * @return the actual location of the node, or null if the node didn't exist
     */
    Location addChild( WorkspaceType workspace,
                       NodeType parent,
                       NodeType newChild,
                       NodeType beforeOtherChild,
                       Name desiredName );

    /**
     * Removes the given node from the repository.
     * 
     * @param workspace the workspace; may not be null
     * @param node the node to be removed; may not be null
     * @return the actual location of the node, or null if the node didn't exist
     */
    Location removeNode( WorkspaceType workspace,
                         NodeType node );

    /**
     * Sets the given properties in a single operation, overwriting any previous properties for the same name. This bulk mutator
     * should be used when multiple properties are being set in order to allow underlying implementations to optimize their access
     * to their respective persistent storage mechanism. The implementation should <i>not</i> change the identification
     * properties.
     * 
     * @param workspace the workspace; may not be null
     * @param node the node; may not be null
     * @param propertiesToSet the properties to set; may be null or empty if there are no properties being added or set
     * @param propertiesToRemove the names of the properties that are to be removed; may be null or empty if no properties are
     *        being removed
     * @param removeAllExisting true if all existing, non-identification properties should be removed, or false otherwise
     * @return this map node
     */
    NodeType setProperties( WorkspaceType workspace,
                            NodeType node,
                            Iterable<Property> propertiesToSet,
                            Iterable<Name> propertiesToRemove,
                            boolean removeAllExisting );

    /**
     * Removes the property with the given name
     * 
     * @param workspace the workspace; may not be null
     * @param node the node; may not be null
     * @param propertyName the name of the property to remove; may not be null
     * @return this map node
     */
    NodeType removeProperty( WorkspaceType workspace,
                             NodeType node,
                             Name propertyName );

    /**
     * This should clone the subgraph given by the original node and place the cloned copy under the supplied new parent. Note
     * that internal references between nodes within the original subgraph must be reflected as internal nodes within the new
     * subgraph.
     * 
     * @param originalWorkspace the workspace containing the original node that is being cloned; may not be null
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
    NodeType cloneNode( WorkspaceType originalWorkspace,
                        NodeType original,
                        WorkspaceType newWorkspace,
                        NodeType newParent,
                        Name desiredName,
                        Path.Segment desiredSegment,
                        boolean removeExisting,
                        Set<Location> removedExistingNodes ) throws UuidAlreadyExistsException;

    /**
     * This should copy the subgraph given by the original node and place the new copy under the supplied new parent. Note that
     * internal references between nodes within the original subgraph must be reflected as internal nodes within the new subgraph.
     * 
     * @param originalWorkspace the workspace containing the original node that is being cloned; may not be null
     * @param original the node to be copied; may not be null
     * @param newWorkspace the workspace containing the new parent node; may not be null
     * @param newParent the parent where the copy is to be placed; may not be null
     * @param desiredName the desired name for the node; if null, the name will be obtained from the original node
     * @param recursive true if the copy should be recursive
     * @return the new node, which is the top of the new subgraph
     */
    NodeType copyNode( WorkspaceType originalWorkspace,
                       NodeType original,
                       WorkspaceType newWorkspace,
                       NodeType newParent,
                       Name desiredName,
                       boolean recursive );

    /**
     * Perform a query of this workspace.
     * 
     * @param workspace the workspace to be searched; may not be null
     * @param accessQuery the access query; may not be null
     * @return the query results, or null if the query is not supported
     */
    QueryResults query( WorkspaceType workspace,
                        AccessQueryRequest accessQuery );

    /**
     * Perform a full-text search of this workspace.
     * 
     * @param workspace the workspace to be searched; may not be null
     * @param search the full-text search; may not be null
     * @return the query results, or null if the query is not supported
     */
    QueryResults search( WorkspaceType workspace,
                         FullTextSearchRequest search );

    /**
     * Attempts to lock the given node with the given timeout. If the lock attempt fails, a {@link LockFailedException} will be
     * thrown.
     * 
     * @param workspace the workspace; may not be null
     * @param node the node to be locked; may not be null
     * @param lockScope the scope of the lock (i.e., whether descendants of {@code node} should be included in the lock
     * @param lockTimeoutInMillis the maximum lifetime of the lock in milliseconds; zero (0) indicates that the connector default
     *        should be used
     * @throws LockFailedException if the implementing connector supports locking but the lock could not be acquired.
     */
    void lockNode( WorkspaceType workspace,
                   NodeType node,
                   LockScope lockScope,
                   long lockTimeoutInMillis ) throws LockFailedException;

    /**
     * Attempts to unlock the given node.
     * 
     * @param workspace the workspace; may not be null
     * @param node the node to be unlocked; may not be null
     */
    void unlockNode( WorkspaceType workspace,
                     NodeType node );

    /**
     * Commit any changes that have been made to the repository. This method may throw runtime exceptions if there are failures
     * committing the changes, but the transaction is still expected to be closed.
     * 
     * @see #rollback()
     */
    void commit();

    /**
     * Rollback any changes that have been made to this repository. This method may throw runtime exceptions if there are failures
     * rolling back the changes, but the transaction is still expected to be closed.
     * 
     * @see #commit()
     */
    void rollback();

}
