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
package org.modeshape.graph.request;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.Location;
import org.modeshape.graph.NodeConflictBehavior;
import org.modeshape.graph.connector.UuidAlreadyExistsException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.request.CloneWorkspaceRequest.CloneConflictBehavior;
import org.modeshape.graph.request.CreateWorkspaceRequest.CreateConflictBehavior;

/**
 * A component that can be used to build up a list of requests. This implementation does perform some simple optimizations, such
 * as combining adjacent compatible requests.
 * <p>
 * This builder can be used to add multiple requests. When the enqueued requests are to be processed, calling {@link #pop()} will
 * remove and return the enqueued requests (as a {@link CompositeRequest} if there is more than one enqueued request).
 * </p>
 */
@NotThreadSafe
public class BatchRequestBuilder {

    private LinkedList<Request> requests;
    private NodeChange pendingRequest;

    public BatchRequestBuilder() {
        this.requests = new LinkedList<Request>();
    }

    public BatchRequestBuilder( LinkedList<Request> requests ) {
        this.requests = requests != null ? requests : new LinkedList<Request>();
    }

    /**
     * Determine whether this builder has built any requests.
     * 
     * @return true if there are requests (i.e., {@link #pop()} will return a non-null request), or false if there are no requests
     */
    public boolean hasRequests() {
        return pendingRequest != null || !requests.isEmpty();
    }

    /**
     * Finish any pending request
     */
    public void finishPendingRequest() {
        if (pendingRequest != null) {
            // There's a pending request, we need to build it ...
            add(pendingRequest.toRequest());
            pendingRequest = null;
        }
    }

    /**
     * Remove and return any requests that have been built by this builder since the last call to this method. This method will
     * return null if no requests have been built. If only one request was built, then it will be returned. If multiple requests
     * have been built, then this method will return a {@link CompositeRequest} containing them.
     * 
     * @return the request (or {@link CompositeRequest}) representing those requests that this builder has created since the last
     *         call to this method, or null if there are no requests to return
     */
    public Request pop() {
        int number = requests.size();
        if (pendingRequest != null) {
            // There's a pending request, we need to build it ...
            Request newRequest = pendingRequest.toRequest();
            if (number == 0) {
                // There's no other request ...
                return newRequest;
            }
            // We have at least one other request, so add the pending request ...
            addPending();
            ++number;
        } else {
            // There is no pending request ...
            if (number == 0) {
                // And no enqueued request ...
                return null;
            }
            if (number == 1) {
                // There's only one request, so return just the one ...
                Request result = requests.getFirst();
                requests.clear();
                return result;
            }
        }
        assert number >= 2;
        // Build a composite request (reusing the existing list), and then replace the list
        Request result = CompositeRequest.with(requests);
        requests = new LinkedList<Request>();
        return result;
    }

    protected final BatchRequestBuilder add( Request request ) {
        addPending();
        requests.add(request);
        return this;
    }

    protected final BatchRequestBuilder addPending() {
        if (pendingRequest != null) {
            requests.add(pendingRequest.toRequest());
            pendingRequest = null;
        }
        return this;
    }

    /**
     * Add a request to obtain the information about the available workspaces.
     * 
     * @return this builder for method chaining; never null
     */
    public BatchRequestBuilder getWorkspaces() {
        return add(new GetWorkspacesRequest());
    }

    /**
     * Add a request to verify the existance of the named workspace.
     * 
     * @param workspaceName the desired name of the workspace, or null if the source's default workspace should be used
     * @return this builder for method chaining; never null
     */
    public BatchRequestBuilder verifyWorkspace( String workspaceName ) {
        return add(new VerifyWorkspaceRequest(workspaceName));
    }

    /**
     * Add a request to create a new workspace, and specify the behavior should a workspace already exists with a name that
     * matches the desired name for the new workspace.
     * 
     * @param desiredNameOfNewWorkspace the desired name of the new workspace
     * @param createConflictBehavior the behavior if a workspace already exists with the same name, or null if the default
     *        behavior should be used
     * @return this builder for method chaining; never null
     */
    public BatchRequestBuilder createWorkspace( String desiredNameOfNewWorkspace,
                                                CreateConflictBehavior createConflictBehavior ) {
        return add(new CreateWorkspaceRequest(desiredNameOfNewWorkspace, createConflictBehavior));
    }

    /**
     * Add a request to clone an existing workspace to create a new workspace, and specify the behavior should a workspace already
     * exists with a name that matches the desired name for the new workspace.
     * 
     * @param nameOfWorkspaceToBeCloned the name of the existing workspace that is to be cloned
     * @param desiredNameOfTargetWorkspace the desired name of the target workspace
     * @param createConflictBehavior the behavior if a workspace already exists with the same name
     * @param cloneConflictBehavior the behavior if the workspace to be cloned does not exist
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if the either workspace name is null
     */
    public BatchRequestBuilder cloneWorkspace( String nameOfWorkspaceToBeCloned,
                                               String desiredNameOfTargetWorkspace,
                                               CreateConflictBehavior createConflictBehavior,
                                               CloneConflictBehavior cloneConflictBehavior ) {
        return add(new CloneWorkspaceRequest(nameOfWorkspaceToBeCloned, desiredNameOfTargetWorkspace, createConflictBehavior,
                                             cloneConflictBehavior));
    }

    /**
     * Add a request to destroy an existing workspace.
     * 
     * @param workspaceName the name of the workspace that is to be destroyed
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if the workspace name is null
     */
    public BatchRequestBuilder destroyWorkspace( String workspaceName ) {
        return add(new DestroyWorkspaceRequest(workspaceName));
    }

    /**
     * Add a request to verify the existance and location of a node at the supplied location.
     * 
     * @param at the location of the node to be verified
     * @param workspaceName the name of the workspace containing the node
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if the location or workspace name is null
     */
    public BatchRequestBuilder verifyNodeExists( Location at,
                                                 String workspaceName ) {
        return add(new VerifyNodeExistsRequest(at, workspaceName));
    }

    /**
     * Add a request to read the properties and number of children of a node at the supplied location.
     * 
     * @param at the location of the node to be read
     * @param workspaceName the name of the workspace containing the node
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if the location or workspace name is null
     */
    public BatchRequestBuilder readNode( Location at,
                                         String workspaceName ) {
        return add(new ReadNodeRequest(at, workspaceName));
    }

    /**
     * Add a request to read the children of a node at the supplied location in the designated workspace.
     * 
     * @param of the location of the node whose children are to be read
     * @param workspaceName the name of the workspace
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if the location or workspace name is null
     */
    public BatchRequestBuilder readAllChildren( Location of,
                                                String workspaceName ) {
        return add(new ReadAllChildrenRequest(of, workspaceName));
    }

    /**
     * Add a request to read the properties and number of children of a node at the supplied location.
     * 
     * @param of the location of the node whose children are to be read
     * @param workspaceName the name of the workspace
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if the location or workspace name is null
     */
    public BatchRequestBuilder readAllProperties( Location of,
                                                  String workspaceName ) {
        return add(new ReadAllPropertiesRequest(of, workspaceName));
    }

    /**
     * Add a request to read the properties and number of children of a node at the supplied location.
     * 
     * @param of the location of the node whose children are to be read
     * @param workspaceName the name of the workspace
     * @param propertyName the name of the property to read
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if the location or workspace name is null
     */
    public BatchRequestBuilder readProperty( Location of,
                                             String workspaceName,
                                             Name propertyName ) {
        return add(new ReadPropertyRequest(of, workspaceName, propertyName));
    }

    /**
     * Add a request to read the branch at the supplied location, to a maximum depth of 2.
     * 
     * @param at the location of the branch
     * @param workspaceName the name of the workspace containing the branch
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if the location or workspace name is null or if the maximum depth is not positive
     */
    public BatchRequestBuilder readBranch( Location at,
                                           String workspaceName ) {
        return add(new ReadBranchRequest(at, workspaceName));
    }

    /**
     * Add a request to read the branch (of given depth) at the supplied location.
     * 
     * @param at the location of the branch
     * @param workspaceName the name of the workspace containing the branch
     * @param maxDepth the maximum depth to read
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if the location or workspace name is null or if the maximum depth is not positive
     */
    public BatchRequestBuilder readBranch( Location at,
                                           String workspaceName,
                                           int maxDepth ) {
        return add(new ReadBranchRequest(at, workspaceName, maxDepth));
    }

    /**
     * Add a request to read a block of the children of a node at the supplied location. The block is defined by the starting
     * index of the first child and the number of children to include. Note that this index is <i>not</i> the
     * {@link Path.Segment#getIndex() same-name-sibiling index}, but rather is the index of the child as if the children were in
     * an array.
     * 
     * @param of the location of the node whose children are to be read
     * @param workspaceName the name of the workspace containing the parent
     * @param startingIndex the zero-based index of the first child to be included in the block
     * @param count the maximum number of children that should be included in the block
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if the location or workspace name is null, if <code>startingIndex</code> is negative, or
     *         if <code>count</count> is less than 1.
     */
    public BatchRequestBuilder readBlockOfChildren( Location of,
                                                    String workspaceName,
                                                    int startingIndex,
                                                    int count ) {
        return add(new ReadBlockOfChildrenRequest(of, workspaceName, startingIndex, count));
    }

    /**
     * Add a request to read those children of a node that are immediately after a supplied sibling node.
     * 
     * @param startingAfter the location of the previous sibling that was the last child of the previous block of children read
     * @param workspaceName the name of the workspace containing the node
     * @param count the maximum number of children that should be included in the block
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if the workspace name or <code>startingAfter</code> location is null, or if
     *         <code>count</count> is less than 1.
     */
    public BatchRequestBuilder readNextBlockOfChildren( Location startingAfter,
                                                        String workspaceName,
                                                        int count ) {
        return add(new ReadNextBlockOfChildrenRequest(startingAfter, workspaceName, count));
    }

    /**
     * Add a request to create a node with the given properties under the supplied location.
     * 
     * @param parentLocation the location of the existing parent node, under which the new child should be created
     * @param workspaceName the name of the workspace containing the parent
     * @param childName the name of the new child to create under the existing parent
     * @param properties the properties of the new node, which should include any {@link Location#getIdProperties() identification
     *        properties} for the new node
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if the location, workspace name, or child name is null
     */
    public BatchRequestBuilder createNode( Location parentLocation,
                                           String workspaceName,
                                           Name childName,
                                           Iterator<Property> properties ) {
        return add(new CreateNodeRequest(parentLocation, workspaceName, childName, CreateNodeRequest.DEFAULT_CONFLICT_BEHAVIOR,
                                         properties));
    }

    /**
     * Add a request to create a node with the given properties under the supplied location.
     * 
     * @param parentLocation the location of the existing parent node, under which the new child should be created
     * @param workspaceName the name of the workspace containing the parent
     * @param childName the name of the new child to create under the existing parent
     * @param properties the properties of the new node, which should include any {@link Location#getIdProperties() identification
     *        properties} for the new node
     * @param conflictBehavior the expected behavior if an equivalently-named child already exists under the <code>into</code>
     *        location
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if the location, workspace name, or child name is null
     */
    public BatchRequestBuilder createNode( Location parentLocation,
                                           String workspaceName,
                                           Name childName,
                                           Iterator<Property> properties,
                                           NodeConflictBehavior conflictBehavior ) {
        if (conflictBehavior == null) conflictBehavior = CreateNodeRequest.DEFAULT_CONFLICT_BEHAVIOR;
        return add(new CreateNodeRequest(parentLocation, workspaceName, childName, conflictBehavior, properties));
    }

    /**
     * Add a request to create a node with the given properties under the supplied location.
     * 
     * @param parentLocation the location of the existing parent node, under which the new child should be created
     * @param workspaceName the name of the workspace containing the parent
     * @param childName the name of the new child to create under the existing parent
     * @param properties the properties of the new node, which should include any {@link Location#getIdProperties() identification
     *        properties} for the new node
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if the location, workspace name, or child name is null
     */
    public BatchRequestBuilder createNode( Location parentLocation,
                                           String workspaceName,
                                           Name childName,
                                           Property[] properties ) {
        return add(new CreateNodeRequest(parentLocation, workspaceName, childName, CreateNodeRequest.DEFAULT_CONFLICT_BEHAVIOR,
                                         properties));
    }

    /**
     * Add a request to create a node with the given properties under the supplied location.
     * 
     * @param parentLocation the location of the existing parent node, under which the new child should be created
     * @param workspaceName the name of the workspace containing the parent
     * @param childName the name of the new child to create under the existing parent
     * @param properties the properties of the new node, which should include any {@link Location#getIdProperties() identification
     *        properties} for the new node
     * @param conflictBehavior the expected behavior if an equivalently-named child already exists under the <code>into</code>
     *        location
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if the location, workspace name, or child name is null
     */
    public BatchRequestBuilder createNode( Location parentLocation,
                                           String workspaceName,
                                           Name childName,
                                           Property[] properties,
                                           NodeConflictBehavior conflictBehavior ) {
        if (conflictBehavior == null) conflictBehavior = CreateNodeRequest.DEFAULT_CONFLICT_BEHAVIOR;
        return add(new CreateNodeRequest(parentLocation, workspaceName, childName, conflictBehavior, properties));
    }

    /**
     * Add a request to update the property on the node at the supplied location. This request will create the property if it does
     * not yet exist.
     * 
     * @param on the location of the node to be read
     * @param workspaceName the name of the workspace containing the node
     * @param property the new property on the node
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if the location or workspace name is null or if there are no properties to update
     */
    public BatchRequestBuilder setProperty( Location on,
                                            String workspaceName,
                                            Property property ) {
        // If there's a pending request ...
        if (pendingRequest != null) {
            // Compare the supplied location with that of the pending request
            if (pendingRequest.location.isSame(on)) {
                // They are the same location, so we can add the properties to the pending request ...
                pendingRequest.pendingProperties.put(property.getName(), property);
                return this;
            }
            // Not the exact same location, so push the existing pending request ...
            addPending();
        }

        // Record this operation as a pending change ...
        pendingRequest = new NodeChange(on, workspaceName);
        pendingRequest.pendingProperties.put(property.getName(), property);
        return this;
    }

    /**
     * Add a request to update the properties on the node at the supplied location.
     * 
     * @param on the location of the node to be read
     * @param workspaceName the name of the workspace containing the node
     * @param properties the new properties on the node
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if the location or workspace name is null or if there are no properties to update
     */
    public BatchRequestBuilder setProperties( Location on,
                                              String workspaceName,
                                              Property... properties ) {
        // If there's a pending request ...
        if (pendingRequest != null) {
            // Compare the supplied location with that of the pending request
            if (pendingRequest.location.isSame(on)) {
                // They are the same location, so we can add the properties to the pending request ...
                for (Property property : properties) {
                    pendingRequest.pendingProperties.put(property.getName(), property);
                }
                return this;
            }
            // Not the exact same location, so push the existing pending request ...
            addPending();
        }

        // Record this operation as a pending change ...
        pendingRequest = new NodeChange(on, workspaceName);
        for (Property property : properties) {
            if (property == null) continue;
            pendingRequest.pendingProperties.put(property.getName(), property);
        }
        return this;
    }

    /**
     * Add a request to remove the property with the supplied name from the given node. Supplying a name for a property that does
     * not exist will not cause an error.
     * 
     * @param on the location of the node to be read
     * @param workspaceName the name of the workspace containing the node
     * @param propertyName the name of the property that is to be removed
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if the location or workspace name is null or if there are no properties to remove
     */
    public BatchRequestBuilder removeProperty( Location on,
                                               String workspaceName,
                                               Name propertyName ) {
        // If there's a pending request ...
        if (pendingRequest != null) {
            // Compare the supplied location with that of the pending request
            if (pendingRequest.location.isSame(on)) {
                // They are the same location, so we can add the properties to the pending request ...
                pendingRequest.pendingProperties.put(propertyName, null);
                return this;
            }
            // Not the exact same location, so push the existing pending request ...
            addPending();
        }

        // Record this operation as a pending change ...
        pendingRequest = new NodeChange(on, workspaceName);
        pendingRequest.pendingProperties.put(propertyName, null);
        return this;
    }

    /**
     * Add a request to remove from the node the properties with the supplied names. Supplying a name for a property that does not
     * exist will not cause an error.
     * 
     * @param on the location of the node to be read
     * @param workspaceName the name of the workspace containing the node
     * @param propertyNames the names of the properties that are to be removed
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if the location or workspace name is null or if there are no properties to remove
     */
    public BatchRequestBuilder removeProperties( Location on,
                                                 String workspaceName,
                                                 Name... propertyNames ) {
        // If there's a pending request ...
        if (pendingRequest != null) {
            // Compare the supplied location with that of the pending request
            if (pendingRequest.location.isSame(on)) {
                // They are the same location, so we can add the properties to the pending request ...
                for (Name propertyName : propertyNames) {
                    pendingRequest.pendingProperties.put(propertyName, null);
                }
                return this;
            }
            // Not the exact same location, so push the existing pending request ...
            addPending();
        }

        // Record this operation as a pending change ...
        pendingRequest = new NodeChange(on, workspaceName);
        for (Name propertyName : propertyNames) {
            pendingRequest.pendingProperties.put(propertyName, null);
        }
        return this;
    }

    /**
     * Add a request to rename the node at the supplied location.
     * 
     * @param at the location of the node to be read
     * @param workspaceName the name of the workspace containing the node
     * @param newName the new name for the node
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if the location or workspace name is null
     */
    public BatchRequestBuilder renameNode( Location at,
                                           String workspaceName,
                                           Name newName ) {
        return add(new RenameNodeRequest(at, workspaceName, newName));
    }

    /**
     * Add a request to copy a branch to another.
     * 
     * @param from the location of the top node in the existing branch that is to be copied
     * @param fromWorkspace the name of the workspace where the <code>from</code> node exists
     * @param into the location of the existing node into which the copy should be placed
     * @param intoWorkspace the name of the workspace where the <code>into</code> node is to be copied
     * @param nameForCopy the desired name for the node that results from the copy, or null if the name of the original should be
     *        used
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if either of the locations or workspace names are null
     */
    public BatchRequestBuilder copyBranch( Location from,
                                           String fromWorkspace,
                                           Location into,
                                           String intoWorkspace,
                                           Name nameForCopy ) {
        return add(new CopyBranchRequest(from, fromWorkspace, into, intoWorkspace, nameForCopy,
                                         CopyBranchRequest.DEFAULT_NODE_CONFLICT_BEHAVIOR));
    }

    /**
     * Add a request to copy a branch to another.
     * 
     * @param from the location of the top node in the existing branch that is to be copied
     * @param fromWorkspace the name of the workspace where the <code>from</code> node exists
     * @param into the location of the existing node into which the copy should be placed
     * @param intoWorkspace the name of the workspace where the <code>into</code> node is to be copied
     * @param nameForCopy the desired name for the node that results from the copy, or null if the name of the original should be
     *        used
     * @param conflictBehavior the expected behavior if an equivalently-named child already exists at the <code>into</code>
     *        location, or null if the default node conflict behavior should be used
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if either of the locations or workspace names are null
     */
    public BatchRequestBuilder copyBranch( Location from,
                                           String fromWorkspace,
                                           Location into,
                                           String intoWorkspace,
                                           Name nameForCopy,
                                           NodeConflictBehavior conflictBehavior ) {
        if (conflictBehavior == null) conflictBehavior = CopyBranchRequest.DEFAULT_NODE_CONFLICT_BEHAVIOR;
        return add(new CopyBranchRequest(from, fromWorkspace, into, intoWorkspace, nameForCopy, conflictBehavior));
    }

    /**
     * Add a request to clone a branch to another.
     * 
     * @param from the location of the top node in the existing branch that is to be cloned
     * @param fromWorkspace the name of the workspace where the <code>from</code> node exists
     * @param into the location of the existing node into which the clone should be placed
     * @param intoWorkspace the name of the workspace where the <code>into</code> node is to be cloned
     * @param nameForClone the desired name for the node that results from the clone, or null if the name of the original should
     *        be used
     * @param exactSegmentForClone the exact {@link Path.Segment segment} at which the cloned tree should be rooted.
     * @param removeExisting whether any nodes in the intoWorkspace with the same UUIDs as a node in the source branch should be
     *        removed (if true) or a {@link UuidAlreadyExistsException} should be thrown.
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if any of the parameters are null except for {@code nameForClone} or {@code
     *         exactSegmentForClone}. Exactly one of {@code nameForClone} and {@code exactSegmentForClone} must be null.
     */
    public BatchRequestBuilder cloneBranch( Location from,
                                            String fromWorkspace,
                                            Location into,
                                            String intoWorkspace,
                                            Name nameForClone,
                                            Path.Segment exactSegmentForClone,
                                            boolean removeExisting ) {
        return add(new CloneBranchRequest(from, fromWorkspace, into, intoWorkspace, nameForClone, exactSegmentForClone,
                                          removeExisting));
    }

    /**
     * Create a request to move a branch from one location into another.
     * 
     * @param from the location of the top node in the existing branch that is to be moved
     * @param into the location of the existing node into which the branch should be moved
     * @param workspaceName the name of the workspace
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public BatchRequestBuilder moveBranch( Location from,
                                           Location into,
                                           String workspaceName ) {
        return add(new MoveBranchRequest(from, into, workspaceName, MoveBranchRequest.DEFAULT_CONFLICT_BEHAVIOR));
    }

    /**
     * Create a request to move a branch from one location into another.
     * 
     * @param from the location of the top node in the existing branch that is to be moved
     * @param into the location of the existing node into which the branch should be moved
     * @param workspaceName the name of the workspace
     * @param newNameForNode the new name for the node being moved, or null if the name of the original should be used
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public BatchRequestBuilder moveBranch( Location from,
                                           Location into,
                                           String workspaceName,
                                           Name newNameForNode ) {
        return add(new MoveBranchRequest(from, into, null, workspaceName, newNameForNode,
                                         MoveBranchRequest.DEFAULT_CONFLICT_BEHAVIOR));
    }

    /**
     * Create a request to move a branch from one location into another.
     * 
     * @param from the location of the top node in the existing branch that is to be moved
     * @param into the location of the existing node into which the branch should be moved
     * @param before the location of the node before which the branch should be moved; may be null
     * @param workspaceName the name of the workspace
     * @param newNameForNode the new name for the node being moved, or null if the name of the original should be used
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public BatchRequestBuilder moveBranch( Location from,
                                           Location into,
                                           Location before,
                                           String workspaceName,
                                           Name newNameForNode ) {
        return add(new MoveBranchRequest(from, into, before, workspaceName, newNameForNode,
                                         MoveBranchRequest.DEFAULT_CONFLICT_BEHAVIOR));
    }

    /**
     * Create a request to move a branch from one location into another.
     * 
     * @param from the location of the top node in the existing branch that is to be moved
     * @param into the location of the existing node into which the branch should be moved
     * @param workspaceName the name of the workspace
     * @param conflictBehavior the expected behavior if an equivalently-named child already exists at the <code>into</code>
     *        location
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public BatchRequestBuilder moveBranch( Location from,
                                           Location into,
                                           String workspaceName,
                                           NodeConflictBehavior conflictBehavior ) {
        if (conflictBehavior == null) conflictBehavior = MoveBranchRequest.DEFAULT_CONFLICT_BEHAVIOR;
        return add(new MoveBranchRequest(from, into, workspaceName, conflictBehavior));
    }

    /**
     * Add a request to delete a branch.
     * 
     * @param at the location of the top node in the existing branch that is to be deleted
     * @param workspaceName the name of the workspace containing the parent
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if the location or workspace name is null
     */
    public BatchRequestBuilder deleteBranch( Location at,
                                             String workspaceName ) {
        return add(new DeleteBranchRequest(at, workspaceName));
    }

    /**
     * Submit any request to this batch.
     * 
     * @param request the request to be batched; may not be null
     * @return this builder for method chaining; never null
     * @throws IllegalArgumentException if the request is null
     */
    public BatchRequestBuilder submit( Request request ) {
        CheckArg.isNotNull(request, "request");
        return add(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        for (Request request : requests) {
            sb.append(request.toString());
            sb.append("\n");
        }
        return sb.toString();
    }

    protected class NodeChange {
        protected final Location location;
        protected final String workspaceName;
        protected final Map<Name, Property> pendingProperties = new HashMap<Name, Property>();

        protected NodeChange( Location location,
                              String workspaceName ) {
            this.location = location;
            this.workspaceName = workspaceName;
        }

        protected Request toRequest() {
            if (pendingProperties.size() == 1) {
                Map.Entry<Name, Property> entry = pendingProperties.entrySet().iterator().next();
                Property property = entry.getValue();
                if (property == null) {
                    return new RemovePropertyRequest(location, workspaceName, entry.getKey());
                }
                return new SetPropertyRequest(location, workspaceName, property);
            }
            return new UpdatePropertiesRequest(location, workspaceName, pendingProperties);
        }
    }

}
