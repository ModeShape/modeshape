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
import java.util.List;
import java.util.Map;
import org.modeshape.graph.Location;
import org.modeshape.graph.NodeConflictBehavior;
import org.modeshape.graph.connector.UuidAlreadyExistsException;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.request.CloneWorkspaceRequest.CloneConflictBehavior;
import org.modeshape.graph.request.CreateWorkspaceRequest.CreateConflictBehavior;
import org.modeshape.graph.request.LockBranchRequest.LockScope;

/**
 * A component that can be used to build requests while allowing different strategies for how requests are handled. Subclasses can
 * simply override the {@link #process(Request)} method to define what happens with each request.
 */
public abstract class RequestBuilder {

    /**
     * Create a new builder.
     */
    protected RequestBuilder() {
    }

    protected abstract <T extends Request> T process( T request );

    /**
     * Add a request to obtain the information about the available workspaces.
     * 
     * @return the request; never null
     */
    public GetWorkspacesRequest getWorkspaces() {
        GetWorkspacesRequest request = new GetWorkspacesRequest();
        process(request);
        return request;
    }

    /**
     * Add a request to verify the existance of the named workspace.
     * 
     * @param workspaceName the desired name of the workspace, or null if the source's default workspace should be used
     * @return the request; never null
     */
    public VerifyWorkspaceRequest verifyWorkspace( String workspaceName ) {
        return process(new VerifyWorkspaceRequest(workspaceName));
    }

    /**
     * Add a request to create a new workspace, and specify the behavior should a workspace already exists with a name that
     * matches the desired name for the new workspace.
     * 
     * @param desiredNameOfNewWorkspace the desired name of the new workspace
     * @param createConflictBehavior the behavior if a workspace already exists with the same name, or null if the default
     *        behavior should be used
     * @return the request; never null
     */
    public CreateWorkspaceRequest createWorkspace( String desiredNameOfNewWorkspace,
                                                   CreateConflictBehavior createConflictBehavior ) {
        return process(new CreateWorkspaceRequest(desiredNameOfNewWorkspace, createConflictBehavior));
    }

    /**
     * Add a request to clone an existing workspace to create a new workspace, and specify the behavior should a workspace already
     * exists with a name that matches the desired name for the new workspace.
     * 
     * @param nameOfWorkspaceToBeCloned the name of the existing workspace that is to be cloned
     * @param desiredNameOfTargetWorkspace the desired name of the target workspace
     * @param createConflictBehavior the behavior if a workspace already exists with the same name
     * @param cloneConflictBehavior the behavior if the workspace to be cloned does not exist
     * @return the request; never null
     * @throws IllegalArgumentException if the either workspace name is null
     */
    public CloneWorkspaceRequest cloneWorkspace( String nameOfWorkspaceToBeCloned,
                                                 String desiredNameOfTargetWorkspace,
                                                 CreateConflictBehavior createConflictBehavior,
                                                 CloneConflictBehavior cloneConflictBehavior ) {
        return process(new CloneWorkspaceRequest(nameOfWorkspaceToBeCloned, desiredNameOfTargetWorkspace, createConflictBehavior,
                                                 cloneConflictBehavior));
    }

    /**
     * Add a request to destroy an existing workspace.
     * 
     * @param workspaceName the name of the workspace that is to be destroyed
     * @return the request; never null
     * @throws IllegalArgumentException if the workspace name is null
     */
    public DestroyWorkspaceRequest destroyWorkspace( String workspaceName ) {
        return process(new DestroyWorkspaceRequest(workspaceName));
    }

    /**
     * Add a request to verify the existance and location of a node at the supplied location.
     * 
     * @param at the location of the node to be verified
     * @param workspaceName the name of the workspace containing the node
     * @return the request; never null
     * @throws IllegalArgumentException if the location or workspace name is null
     */
    public VerifyNodeExistsRequest verifyNodeExists( Location at,
                                                     String workspaceName ) {
        return process(new VerifyNodeExistsRequest(at, workspaceName));
    }

    /**
     * Add a request to read the properties and number of children of a node at the supplied location.
     * 
     * @param at the location of the node to be read
     * @param workspaceName the name of the workspace containing the node
     * @return the request; never null
     * @throws IllegalArgumentException if the location or workspace name is null
     */
    public ReadNodeRequest readNode( Location at,
                                     String workspaceName ) {
        return process(new ReadNodeRequest(at, workspaceName));
    }

    /**
     * Add a request to read the children of a node at the supplied location in the designated workspace.
     * 
     * @param of the location of the node whose children are to be read
     * @param workspaceName the name of the workspace
     * @return the request; never null
     * @throws IllegalArgumentException if the location or workspace name is null
     */
    public ReadAllChildrenRequest readAllChildren( Location of,
                                                   String workspaceName ) {
        return process(new ReadAllChildrenRequest(of, workspaceName));
    }

    /**
     * Add a request to read the properties and number of children of a node at the supplied location.
     * 
     * @param of the location of the node whose children are to be read
     * @param workspaceName the name of the workspace
     * @return the request; never null
     * @throws IllegalArgumentException if the location or workspace name is null
     */
    public ReadAllPropertiesRequest readAllProperties( Location of,
                                                       String workspaceName ) {
        return process(new ReadAllPropertiesRequest(of, workspaceName));
    }

    /**
     * Add a request to read the properties and number of children of a node at the supplied location.
     * 
     * @param of the location of the node whose children are to be read
     * @param workspaceName the name of the workspace
     * @param propertyName the name of the property to read
     * @return the request; never null
     * @throws IllegalArgumentException if the location or workspace name is null
     */
    public ReadPropertyRequest readProperty( Location of,
                                             String workspaceName,
                                             Name propertyName ) {
        return process(new ReadPropertyRequest(of, workspaceName, propertyName));
    }

    /**
     * Add a request to read the branch at the supplied location, to a maximum depth of 2.
     * 
     * @param at the location of the branch
     * @param workspaceName the name of the workspace containing the branch
     * @return the request; never null
     * @throws IllegalArgumentException if the location or workspace name is null or if the maximum depth is not positive
     */
    public ReadBranchRequest readBranch( Location at,
                                         String workspaceName ) {
        return process(new ReadBranchRequest(at, workspaceName));
    }

    /**
     * Add a request to read the branch (of given depth) at the supplied location.
     * 
     * @param at the location of the branch
     * @param workspaceName the name of the workspace containing the branch
     * @param maxDepth the maximum depth to read
     * @return the request; never null
     * @throws IllegalArgumentException if the location or workspace name is null or if the maximum depth is not positive
     */
    public ReadBranchRequest readBranch( Location at,
                                         String workspaceName,
                                         int maxDepth ) {
        return process(new ReadBranchRequest(at, workspaceName, maxDepth));
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
     * @return the request; never null
     * @throws IllegalArgumentException if the location or workspace name is null, if <code>startingIndex</code> is negative, or
     *         if <code>count</count> is less than 1.
     */
    public ReadBlockOfChildrenRequest readBlockOfChildren( Location of,
                                                           String workspaceName,
                                                           int startingIndex,
                                                           int count ) {
        return process(new ReadBlockOfChildrenRequest(of, workspaceName, startingIndex, count));
    }

    /**
     * Add a request to read those children of a node that are immediately after a supplied sibling node.
     * 
     * @param startingAfter the location of the previous sibling that was the last child of the previous block of children read
     * @param workspaceName the name of the workspace containing the node
     * @param count the maximum number of children that should be included in the block
     * @return the request; never null
     * @throws IllegalArgumentException if the workspace name or <code>startingAfter</code> location is null, or if
     *         <code>count</count> is less than 1.
     */
    public ReadNextBlockOfChildrenRequest readNextBlockOfChildren( Location startingAfter,
                                                                   String workspaceName,
                                                                   int count ) {
        return process(new ReadNextBlockOfChildrenRequest(startingAfter, workspaceName, count));
    }

    /**
     * Add a request to create a node with the given properties under the supplied location.
     * 
     * @param parentLocation the location of the existing parent node, under which the new child should be created
     * @param workspaceName the name of the workspace containing the parent
     * @param childName the name of the new child to create under the existing parent
     * @param properties the properties of the new node, which should include any {@link Location#getIdProperties() identification
     *        properties} for the new node
     * @return the request; never null
     * @throws IllegalArgumentException if the location, workspace name, or child name is null
     */
    public CreateNodeRequest createNode( Location parentLocation,
                                         String workspaceName,
                                         Name childName,
                                         Iterator<Property> properties ) {
        return process(new CreateNodeRequest(parentLocation, workspaceName, childName,
                                             CreateNodeRequest.DEFAULT_CONFLICT_BEHAVIOR, properties));
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
     * @return the request; never null
     * @throws IllegalArgumentException if the location, workspace name, or child name is null
     */
    public CreateNodeRequest createNode( Location parentLocation,
                                         String workspaceName,
                                         Name childName,
                                         Iterator<Property> properties,
                                         NodeConflictBehavior conflictBehavior ) {
        if (conflictBehavior == null) conflictBehavior = CreateNodeRequest.DEFAULT_CONFLICT_BEHAVIOR;
        return process(new CreateNodeRequest(parentLocation, workspaceName, childName, conflictBehavior, properties));
    }

    /**
     * Add a request to create a node with the given properties under the supplied location.
     * 
     * @param parentLocation the location of the existing parent node, under which the new child should be created
     * @param workspaceName the name of the workspace containing the parent
     * @param childName the name of the new child to create under the existing parent
     * @param properties the properties of the new node, which should include any {@link Location#getIdProperties() identification
     *        properties} for the new node
     * @return the request; never null
     * @throws IllegalArgumentException if the location, workspace name, or child name is null
     */
    public CreateNodeRequest createNode( Location parentLocation,
                                         String workspaceName,
                                         Name childName,
                                         Property[] properties ) {
        return process(new CreateNodeRequest(parentLocation, workspaceName, childName,
                                             CreateNodeRequest.DEFAULT_CONFLICT_BEHAVIOR, properties));
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
     * @return the request; never null
     * @throws IllegalArgumentException if the location, workspace name, or child name is null
     */
    public CreateNodeRequest createNode( Location parentLocation,
                                         String workspaceName,
                                         Name childName,
                                         Property[] properties,
                                         NodeConflictBehavior conflictBehavior ) {
        if (conflictBehavior == null) conflictBehavior = CreateNodeRequest.DEFAULT_CONFLICT_BEHAVIOR;
        return process(new CreateNodeRequest(parentLocation, workspaceName, childName, conflictBehavior, properties));
    }

    /**
     * Add a request to update the property on the node at the supplied location. This request will create the property if it does
     * not yet exist.
     * 
     * @param on the location of the node to be read
     * @param workspaceName the name of the workspace containing the node
     * @param property the new property on the node
     * @return the request; never null
     * @throws IllegalArgumentException if the location or workspace name is null or if there are no properties to update
     */
    public SetPropertyRequest setProperty( Location on,
                                           String workspaceName,
                                           Property property ) {
        return process(new SetPropertyRequest(on, workspaceName, property));
    }

    /**
     * Add a request to update the properties on the node at the supplied location.
     * 
     * @param on the location of the node to be read
     * @param workspaceName the name of the workspace containing the node
     * @param properties the new properties on the node
     * @return the {@link SetPropertyRequest} or {@link UpdatePropertiesRequest} request, depending upon the number of properties
     *         being set; never null
     * @throws IllegalArgumentException if the location or workspace name is null or if there are no properties to update
     */
    public Request setProperties( Location on,
                                  String workspaceName,
                                  Property... properties ) {
        if (properties.length == 1) {
            return process(new SetPropertyRequest(on, workspaceName, properties[0]));
        }
        Map<Name, Property> propertyMap = new HashMap<Name, Property>();
        for (Property property : properties) {
            propertyMap.put(property.getName(), property);
        }
        return process(new UpdatePropertiesRequest(on, workspaceName, propertyMap));
    }

    /**
     * Add a request to remove the property with the supplied name from the given node. Supplying a name for a property that does
     * not exist will not cause an error.
     * 
     * @param on the location of the node to be read
     * @param workspaceName the name of the workspace containing the node
     * @param propertyName the name of the property that is to be removed
     * @return the request; never null
     * @throws IllegalArgumentException if the location or workspace name is null or if there are no properties to remove
     */
    public RemovePropertyRequest removeProperty( Location on,
                                                 String workspaceName,
                                                 Name propertyName ) {
        return process(new RemovePropertyRequest(on, workspaceName, propertyName));
    }

    /**
     * Add a request to remove from the node the properties with the supplied names. Supplying a name for a property that does not
     * exist will not cause an error.
     * 
     * @param on the location of the node to be read
     * @param workspaceName the name of the workspace containing the node
     * @param propertyNames the names of the properties that are to be removed
     * @return the {@link RemovePropertyRequest} or {@link UpdatePropertiesRequest} request, depending upon the number of
     *         properties being removed; never null
     * @throws IllegalArgumentException if the location or workspace name is null or if there are no properties to remove
     */
    public Request removeProperties( Location on,
                                     String workspaceName,
                                     Name... propertyNames ) {
        if (propertyNames.length == 1) {
            return process(new RemovePropertyRequest(on, workspaceName, propertyNames[0]));
        }
        Map<Name, Property> properties = new HashMap<Name, Property>();
        for (Name propertyName : propertyNames) {
            properties.put(propertyName, null);
        }
        return process(new UpdatePropertiesRequest(on, workspaceName, properties));
    }

    /**
     * Add a request to rename the node at the supplied location.
     * 
     * @param at the location of the node to be read
     * @param workspaceName the name of the workspace containing the node
     * @param newName the new name for the node
     * @return the request; never null
     * @throws IllegalArgumentException if the location or workspace name is null
     */
    public RenameNodeRequest renameNode( Location at,
                                         String workspaceName,
                                         Name newName ) {
        return process(new RenameNodeRequest(at, workspaceName, newName));
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
     *        location, or null if the default conflict behavior should be used
     * @return the request; never null
     * @throws IllegalArgumentException if either of the locations or workspace names are null
     */
    public CopyBranchRequest copyBranch( Location from,
                                         String fromWorkspace,
                                         Location into,
                                         String intoWorkspace,
                                         Name nameForCopy,
                                         NodeConflictBehavior conflictBehavior ) {
        if (conflictBehavior == null) conflictBehavior = CopyBranchRequest.DEFAULT_NODE_CONFLICT_BEHAVIOR;
        return process(new CopyBranchRequest(from, fromWorkspace, into, intoWorkspace, nameForCopy, conflictBehavior));
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
     * @return the request; never null
     * @throws IllegalArgumentException if any of the parameters are null except for {@code nameForClone} or {@code
     *         exactSegmentForClone}. Exactly one of {@code nameForClone} and {@code exactSegmentForClone} must be null.
     */
    public CloneBranchRequest cloneBranch( Location from,
                                           String fromWorkspace,
                                           Location into,
                                           String intoWorkspace,
                                           Name nameForClone,
                                           Path.Segment exactSegmentForClone,
                                           boolean removeExisting ) {
        return process(new CloneBranchRequest(from, fromWorkspace, into, intoWorkspace, nameForClone, exactSegmentForClone,
                                              removeExisting));
    }

    /**
     * Create a request to move a branch from one location into another.
     * 
     * @param from the location of the top node in the existing branch that is to be moved
     * @param into the location of the existing node into which the branch should be moved
     * @param workspaceName the name of the workspace
     * @return the request; never null
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public MoveBranchRequest moveBranch( Location from,
                                         Location into,
                                         String workspaceName ) {
        return process(new MoveBranchRequest(from, into, workspaceName, MoveBranchRequest.DEFAULT_CONFLICT_BEHAVIOR));
    }

    /**
     * Create a request to move a branch from one location into another.
     * 
     * @param from the location of the top node in the existing branch that is to be moved
     * @param into the location of the existing node into which the branch should be moved
     * @param workspaceName the name of the workspace
     * @param newNameForNode the new name for the node being moved, or null if the name of the original should be used
     * @return the request; never null
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public MoveBranchRequest moveBranch( Location from,
                                         Location into,
                                         String workspaceName,
                                         Name newNameForNode ) {
        return process(new MoveBranchRequest(from, into, null, workspaceName, newNameForNode,
                                             MoveBranchRequest.DEFAULT_CONFLICT_BEHAVIOR));
    }

    /**
     * Create a request to move a branch from one location into another before the given child node of the new location.
     * 
     * @param from the location of the top node in the existing branch that is to be moved
     * @param into the location of the existing node into which the branch should be moved
     * @param before the location of the node before which the branch should be moved; may be null
     * @param workspaceName the name of the workspace
     * @param newNameForNode the new name for the node being moved, or null if the name of the original should be used
     * @return the request; never null
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public MoveBranchRequest moveBranch( Location from,
                                         Location into,
                                         Location before,
                                         String workspaceName,
                                         Name newNameForNode ) {
        return process(new MoveBranchRequest(from, into, before, workspaceName, newNameForNode,
                                             MoveBranchRequest.DEFAULT_CONFLICT_BEHAVIOR));
    }

    /**
     * Create a request to move a branch from one location into another.
     * 
     * @param from the location of the top node in the existing branch that is to be moved
     * @param into the location of the existing node into which the branch should be moved
     * @param workspaceName the name of the workspace
     * @param newNameForNode the new name for the node being moved, or null if the name of the original should be used
     * @param conflictBehavior the expected behavior if an equivalently-named child already exists at the <code>into</code>
     *        location
     * @return the request; never null
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public MoveBranchRequest moveBranch( Location from,
                                         Location into,
                                         String workspaceName,
                                         Name newNameForNode,
                                         NodeConflictBehavior conflictBehavior ) {
        if (conflictBehavior == null) conflictBehavior = MoveBranchRequest.DEFAULT_CONFLICT_BEHAVIOR;
        return process(new MoveBranchRequest(from, into, null, workspaceName, newNameForNode, conflictBehavior));
    }

    /**
     * Create a request to move a branch from one location into another.
     * 
     * @param from the location of the top node in the existing branch that is to be moved
     * @param into the location of the existing node into which the branch should be moved
     * @param workspaceName the name of the workspace
     * @param conflictBehavior the expected behavior if an equivalently-named child already exists at the <code>into</code>
     *        location
     * @return the request; never null
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public MoveBranchRequest moveBranch( Location from,
                                         Location into,
                                         String workspaceName,
                                         NodeConflictBehavior conflictBehavior ) {
        if (conflictBehavior == null) conflictBehavior = MoveBranchRequest.DEFAULT_CONFLICT_BEHAVIOR;
        return process(new MoveBranchRequest(from, into, workspaceName, conflictBehavior));
    }

    /**
     * Add a request to delete a branch.
     * 
     * @param at the location of the top node in the existing branch that is to be deleted
     * @param workspaceName the name of the workspace containing the parent
     * @return the request; never null
     * @throws IllegalArgumentException if the location or workspace name is null
     */
    public DeleteBranchRequest deleteBranch( Location at,
                                             String workspaceName ) {
        return process(new DeleteBranchRequest(at, workspaceName));
    }

    /**
     * Add a request to add values to a property on an existing node
     * 
     * @param workspaceName the name of the workspace containing the node; may not be null
     * @param on the location of the node; may not be null
     * @param property the name of the property; may not be null
     * @param values the new values to add; may not be null
     * @return the request; never null
     */
    public UpdateValuesRequest addValues( String workspaceName,
                                          Location on,
                                          Name property,
                                          List<Object> values ) {
        UpdateValuesRequest request = new UpdateValuesRequest(workspaceName, on, property, values, null);
        process(request);
        return request;
    }

    /**
     * Add a request to remove values from a property on an existing node
     * 
     * @param workspaceName the name of the workspace containing the node; may not be null
     * @param on the location of the node; may not be null
     * @param property the name of the property; may not be null
     * @param values the new values to remove; may not be null
     * @return the request; never null
     */
    public UpdateValuesRequest removeValues( String workspaceName,
                                             Location on,
                                             Name property,
                                             List<Object> values ) {
        UpdateValuesRequest request = new UpdateValuesRequest(workspaceName, on, property, null, values);
        process(request);
        return request;
    }

    /**
     * Create a request to lock a branch or node
     * 
     * @param workspaceName the name of the workspace containing the node; may not be null
     * @param target the location of the top node in the existing branch that is to be locked
     * @param lockScope the {@link LockBranchRequest#lockScope()} scope of the lock
     * @param lockTimeoutInMillis the number of milliseconds that the lock should last before the lock times out; zero (0)
     *        indicates that the connector default should be used
     * @return the request; never null
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public LockBranchRequest lockBranch( String workspaceName,
                                         Location target,
                                         LockScope lockScope,
                                         long lockTimeoutInMillis ) {
        return process(new LockBranchRequest(target, workspaceName, lockScope, lockTimeoutInMillis));
    }

    /**
     * Create a request to unlock a branch or node
     * <p>
     * The lock on the node should be removed. If the lock was deep (i.e., locked the entire branch under the node, then all of
     * the descendants of the node affected by the lock should also be unlocked after this request is processed.
     * </p>
     * 
     * @param workspaceName the name of the workspace containing the node; may not be null
     * @param target the location of the top node in the existing branch that is to be unlocked
     * @return the request; never null
     * @throws IllegalArgumentException if any of the parameters are null
     */
    public UnlockBranchRequest unlockBranch( String workspaceName,
                                             Location target ) {
        return process(new UnlockBranchRequest(target, workspaceName));
    }

    /**
     * Create a request to perform a full-text search of the workspace.
     * 
     * @param workspaceName the name of the workspace containing the node
     * @param fullTextSearchExpression the full-text search expression
     * @param maxResults the maximum number of results that are to be returned; always positive
     * @param offset the number of initial results to skip, or 0 if the first results are to be returned
     * @return the request; never null
     * @throws IllegalArgumentException if any of the parameters are null or if the expression is empty
     */
    public FullTextSearchRequest search( String workspaceName,
                                         String fullTextSearchExpression,
                                         int maxResults,
                                         int offset ) {
        return process(new FullTextSearchRequest(fullTextSearchExpression, workspaceName, maxResults, offset));
    }
}
