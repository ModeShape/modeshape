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
package org.modeshape.graph.request.processor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.Location;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.LockFailedException;
import org.modeshape.graph.observe.Changes;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.ReferentialIntegrityException;
import org.modeshape.graph.request.AccessQueryRequest;
import org.modeshape.graph.request.CacheableRequest;
import org.modeshape.graph.request.ChangeRequest;
import org.modeshape.graph.request.CloneBranchRequest;
import org.modeshape.graph.request.CloneWorkspaceRequest;
import org.modeshape.graph.request.CollectGarbageRequest;
import org.modeshape.graph.request.CompositeRequest;
import org.modeshape.graph.request.CopyBranchRequest;
import org.modeshape.graph.request.CreateNodeRequest;
import org.modeshape.graph.request.CreateWorkspaceRequest;
import org.modeshape.graph.request.DeleteBranchRequest;
import org.modeshape.graph.request.DeleteChildrenRequest;
import org.modeshape.graph.request.DestroyWorkspaceRequest;
import org.modeshape.graph.request.FullTextSearchRequest;
import org.modeshape.graph.request.GetWorkspacesRequest;
import org.modeshape.graph.request.InvalidRequestException;
import org.modeshape.graph.request.LockBranchRequest;
import org.modeshape.graph.request.MoveBranchRequest;
import org.modeshape.graph.request.ReadAllChildrenRequest;
import org.modeshape.graph.request.ReadAllPropertiesRequest;
import org.modeshape.graph.request.ReadBlockOfChildrenRequest;
import org.modeshape.graph.request.ReadBranchRequest;
import org.modeshape.graph.request.ReadNextBlockOfChildrenRequest;
import org.modeshape.graph.request.ReadNodeRequest;
import org.modeshape.graph.request.ReadPropertyRequest;
import org.modeshape.graph.request.RemovePropertyRequest;
import org.modeshape.graph.request.RenameNodeRequest;
import org.modeshape.graph.request.Request;
import org.modeshape.graph.request.SetPropertyRequest;
import org.modeshape.graph.request.UnlockBranchRequest;
import org.modeshape.graph.request.UnsupportedRequestException;
import org.modeshape.graph.request.UpdatePropertiesRequest;
import org.modeshape.graph.request.UpdateValuesRequest;
import org.modeshape.graph.request.VerifyNodeExistsRequest;
import org.modeshape.graph.request.VerifyWorkspaceRequest;

/**
 * A component that is used to process and execute {@link Request}s. This class is intended to be subclassed and methods
 * overwritten to define the behavior for executing the different kinds of requests. Abstract methods must be overridden, but
 * non-abstract methods all have meaningful default implementations.
 */
@NotThreadSafe
public abstract class RequestProcessor {

    private final ExecutionContext context;
    private final String sourceName;
    private final DateTime nowInUtc;
    private final CachePolicy defaultCachePolicy;
    private List<ChangeRequest> changes;
    private final Observer observer;

    protected RequestProcessor( String sourceName,
                                ExecutionContext context,
                                Observer observer ) {
        this(sourceName, context, observer, null, null);
    }

    protected RequestProcessor( String sourceName,
                                ExecutionContext context,
                                Observer observer,
                                DateTime now ) {
        this(sourceName, context, observer, now, null);
    }

    protected RequestProcessor( String sourceName,
                                ExecutionContext context,
                                Observer observer,
                                DateTime now,
                                CachePolicy defaultCachePolicy ) {
        CheckArg.isNotEmpty(sourceName, "sourceName");
        CheckArg.isNotNull(context, "context");
        this.context = context;
        this.sourceName = sourceName;
        this.nowInUtc = now != null ? now : context.getValueFactories().getDateFactory().createUtc();
        this.defaultCachePolicy = defaultCachePolicy;
        this.changes = observer != null ? new LinkedList<ChangeRequest>() : null;
        this.observer = observer;
    }

    /**
     * Record the supplied change request for publishing through the event mechanism.
     * 
     * @param request the completed change request; may not be null, and may not be cancelled or have an error
     */
    protected void recordChange( ChangeRequest request ) {
        assert request != null;
        assert !request.isCancelled();
        assert !request.hasError();
        if (changes != null) changes.add(request);
    }

    /**
     * Get the name of the source against which this processor is executing.
     * 
     * @return the repository source name; never null or empty
     */
    public final String getSourceName() {
        return sourceName;
    }

    /**
     * The execution context that this process is operating within.
     * 
     * @return the execution context; never null
     */
    public final ExecutionContext getExecutionContext() {
        return this.context;
    }

    /**
     * Get the 'current time' for this processor, which is usually a constant during its lifetime.
     * 
     * @return the current time in UTC; never null
     */
    public final DateTime getNowInUtc() {
        return this.nowInUtc;
    }

    /**
     * @return defaultCachePolicy
     */
    protected final CachePolicy getDefaultCachePolicy() {
        return defaultCachePolicy;
    }

    /**
     * Set the supplied request to have the default cache policy and the {@link #getNowInUtc() current time in UTC}.
     * 
     * @param request the cacheable request
     */
    protected void setCacheableInfo( CacheableRequest request ) {
        // Set it only if the request has no cache policy already ...
        if (request.getCachePolicy() == null && defaultCachePolicy != null) {
            request.setCachePolicy(defaultCachePolicy);
        }
        request.setTimeLoaded(nowInUtc);
    }

    /**
     * Set the supplied request to have the supplied cache policy and the {@link #getNowInUtc() current time in UTC}.
     * 
     * @param request the cacheable request
     * @param cachePolicy the cache policy for the request; may be null if there is to be no cache policy
     */
    protected void setCacheableInfo( CacheableRequest request,
                                     CachePolicy cachePolicy ) {
        if (cachePolicy == null) cachePolicy = defaultCachePolicy;
        if (cachePolicy != null) {
            if (request.getCachePolicy() != null) {
                // Set the supplied only if less than the current ...
                if (request.getCachePolicy().getTimeToLive() > cachePolicy.getTimeToLive()) {
                    request.setCachePolicy(cachePolicy);
                }
            } else {
                // There is no current policy, so set the supplied policy ...
                request.setCachePolicy(cachePolicy);
            }
        }
        request.setTimeLoaded(nowInUtc);
    }

    /**
     * Process a request by determining the type of request and delegating to the appropriate <code>process</code> method for that
     * type.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the general request
     */
    public void process( Request request ) {
        if (request == null) return;
        try {
            if (request.isCancelled()) return;

            switch (request.getType()) {
                case ACCESS_QUERY:
                    process((AccessQueryRequest)request);
                    break;
                case COMPOSITE:
                    process((CompositeRequest)request);
                    break;
                case CLONE_BRANCH:
                    process((CloneBranchRequest)request);
                    break;
                case CLONE_WORKSPACE:
                    process((CloneWorkspaceRequest)request);
                    break;
                case COLLECT_GARBAGE:
                    process((CollectGarbageRequest)request);
                    break;
                case COPY_BRANCH:
                    process((CopyBranchRequest)request);
                    break;
                case CREATE_NODE:
                    process((CreateNodeRequest)request);
                    break;
                case CREATE_WORKSPACE:
                    process((CreateWorkspaceRequest)request);
                    break;
                case DELETE_BRANCH:
                    process((DeleteBranchRequest)request);
                    break;
                case DELETE_CHILDREN:
                    process((DeleteChildrenRequest)request);
                    break;
                case DESTROY_WORKSPACE:
                    process((DestroyWorkspaceRequest)request);
                    break;
                case FULL_TEXT_SEARCH:
                    process((FullTextSearchRequest)request);
                    break;
                case GET_WORKSPACES:
                    process((GetWorkspacesRequest)request);
                    break;
                case LAST:
                    break;
                case LOCK_BRANCH:
                    process((LockBranchRequest)request);
                    break;
                case MOVE_BRANCH:
                    process((MoveBranchRequest)request);
                    break;
                case READ_ALL_CHILDREN:
                    process((ReadAllChildrenRequest)request);
                    break;
                case READ_ALL_PROPERTIES:
                    process((ReadAllPropertiesRequest)request);
                    break;
                case READ_BLOCK_OF_CHILDREN:
                    process((ReadBlockOfChildrenRequest)request);
                    break;
                case READ_BRANCH:
                    process((ReadBranchRequest)request);
                    break;
                case READ_NEXT_BLOCK_OF_CHILDREN:
                    process((ReadNextBlockOfChildrenRequest)request);
                    break;
                case READ_NODE:
                    process((ReadNodeRequest)request);
                    break;
                case READ_PROPERTY:
                    process((ReadPropertyRequest)request);
                    break;
                case REMOVE_PROPERTY:
                    process((RemovePropertyRequest)request);
                    break;
                case RENAME_NODE:
                    process((RenameNodeRequest)request);
                    break;
                case SET_PROPERTY:
                    process((SetPropertyRequest)request);
                    break;
                case UNLOCK_BRANCH:
                    process((UnlockBranchRequest)request);
                    break;
                case UPDATE_PROPERTIES:
                    process((UpdatePropertiesRequest)request);
                    break;
                case UPDATE_VALUES:
                    process((UpdateValuesRequest)request);
                    break;
                case VERIFY_NODE_EXISTS:
                    process((VerifyNodeExistsRequest)request);
                    break;
                case VERIFY_WORKSPACE:
                    process((VerifyWorkspaceRequest)request);
                    break;
                default:
                    processUnknownRequest(request);
            }
        } catch (RuntimeException e) {
            request.setError(e);
        } finally {
            completeRequest(request);
        }
    }

    protected void completeRequest( Request request ) {
        request.freeze();
    }

    /**
     * Process a request that is composed of multiple other (non-composite) requests. If any of the embedded requests
     * {@link Request#hasError() has an error} after it is processed, each of the embedded requests will be marked with the error
     * <i>and</i> the submitted composite request will be marked with an error. If one of the embedded requests attempts to
     * {@link Request#isReadOnly() make a change} and results in an error, then the processing of all remaining embedded requests
     * is aborted and the composite request will be marked with only this error from the change request.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the composite request
     */
    public void process( CompositeRequest request ) {
        if (request == null) return;
        boolean hasErrors = false;
        boolean readonly = request.isReadOnly();
        // Iterate over the requests in this composite, but only iterate once so that
        Iterator<Request> iter = request.iterator();
        while (iter.hasNext()) {
            Request embedded = iter.next();
            assert embedded != null;
            if (embedded.isCancelled()) return;
            try {
                process(embedded);
            } catch (RuntimeException e) {
                embedded.setError(e);
            }
            if (!hasErrors && embedded.hasError()) {
                hasErrors = true;
                if (!readonly && !embedded.isReadOnly()) {
                    // The request is trying to make changes, and this embedded was a change that resulted in an error.
                    // Therefore, the whole execution needs to stop and we should set this one error message
                    // on the composite request ...
                    assert embedded.getError() != null;
                    request.setError(embedded.getError());
                    // We need to freeze all the remaining (unprocessed) requests before returning ...
                    while (iter.hasNext()) {
                        embedded = iter.next();
                        // Cancel this request and then freeze ...
                        embedded.cancel();
                        embedded.freeze();
                    }
                    return;
                }
            }
        }
        if (hasErrors) {
            request.checkForErrors();
        }
    }

    /**
     * Method that is called by {@link #process(Request)} when the request was found to be of a request type that is not known by
     * this processor. By default this method sets an {@link UnsupportedRequestException unsupported request error} on the
     * request.
     * 
     * @param request the unknown request
     */
    protected void processUnknownRequest( Request request ) {
        request.setError(new InvalidRequestException(GraphI18n.unsupportedRequestType.text(request.getClass().getName(), request)));
    }

    /**
     * Process a request to verify a named workspace.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the request
     */
    public abstract void process( VerifyWorkspaceRequest request );

    /**
     * Process a request to get the information about the available workspaces.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the request
     */
    public abstract void process( GetWorkspacesRequest request );

    /**
     * Process a request to create a new workspace.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the request
     */
    public abstract void process( CreateWorkspaceRequest request );

    /**
     * Process a request to clone a branch into a new workspace.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the request
     */
    public abstract void process( CloneBranchRequest request );

    /**
     * Process a request to clone an existing workspace as a new workspace.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the request
     */
    public abstract void process( CloneWorkspaceRequest request );

    /**
     * Process a request to permanently destroy a workspace.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the request
     */
    public abstract void process( DestroyWorkspaceRequest request );

    /**
     * Process a request to copy a branch into another location.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the copy request
     */
    public abstract void process( CopyBranchRequest request );

    /**
     * Process a request to create a node at a specified location.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the create request
     */
    public abstract void process( CreateNodeRequest request );

    /**
     * Process a request to delete a branch at a specified location.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the delete request
     * @throws ReferentialIntegrityException if the delete could not be performed because some references to deleted nodes would
     *         have remained after the delete operation completed
     */
    public abstract void process( DeleteBranchRequest request );

    /**
     * Process a request to delete all of the child nodes under the supplied existing node.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the delete request
     * @throws ReferentialIntegrityException if the delete could not be performed because some references to deleted nodes would
     *         have remained after the delete operation completed
     */
    public void process( DeleteChildrenRequest request ) {
        if (request == null) return;
        if (request.isCancelled()) return;
        // First get all of the children under the node ...
        ReadAllChildrenRequest readChildren = new ReadAllChildrenRequest(request.at(), request.inWorkspace());
        process(readChildren);
        if (readChildren.hasError()) {
            request.setError(readChildren.getError());
            return;
        }
        if (readChildren.isCancelled()) return;

        // Issue a DeleteBranchRequest for each child ...
        for (Location child : readChildren) {
            if (request.isCancelled()) return;
            DeleteBranchRequest deleteChild = new DeleteBranchRequest(child, request.inWorkspace());
            process(deleteChild);
            request.addDeletedChild(child);
        }

        // Set the actual location of the parent node ...
        request.setActualLocationOfNode(readChildren.getActualLocationOfNode());
    }

    /**
     * Process a request to move a branch at a specified location into a different location.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the move request
     */
    public abstract void process( MoveBranchRequest request );

    /**
     * Process a request to read all of the children of a node.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the read request
     */
    public abstract void process( ReadAllChildrenRequest request );

    /**
     * Process a request to read a block of the children of a node. The block is defined by a
     * {@link ReadBlockOfChildrenRequest#startingAtIndex() starting index} and a {@link ReadBlockOfChildrenRequest#count() maximum
     * number of children to include in the block}.
     * <p>
     * This method does nothing if the request is null. The default implementation converts the command to a
     * {@link ReadAllChildrenRequest}, and then finds the children within the block. Obviously for large numbers of children, this
     * implementation may not be efficient and may need to be overridden.
     * </p>
     * 
     * @param request the read request
     */
    public void process( ReadBlockOfChildrenRequest request ) {
        if (request == null) return;
        // Convert the request to a ReadAllChildrenRequest and execute it ...
        ReadAllChildrenRequest readAll = new ReadAllChildrenRequest(request.of(), request.inWorkspace());
        process(readAll);
        if (readAll.hasError()) {
            request.setError(readAll.getError());
            return;
        }
        List<Location> allChildren = readAll.getChildren();

        // If there aren't enough children for the block's range ...
        if (allChildren.size() < request.startingAtIndex()) return;

        // Now, find the children in the block ...
        int endIndex = Math.min(request.endingBefore(), allChildren.size());
        for (int i = request.startingAtIndex(); i != endIndex; ++i) {
            request.addChild(allChildren.get(i));
        }
        // Set the actual location ...
        request.setActualLocationOfNode(readAll.getActualLocationOfNode());
        setCacheableInfo(request);
    }

    /**
     * Process a request to read the next block of the children of a node, starting after a previously-retrieved child.
     * <p>
     * This method does nothing if the request is null. The default implementation converts the command to a
     * {@link ReadAllChildrenRequest}, and then finds the children within the block. Obviously for large numbers of children, this
     * implementation may not be efficient and may need to be overridden.
     * </p>
     * 
     * @param request the read request
     */
    public void process( ReadNextBlockOfChildrenRequest request ) {
        if (request == null) return;

        // Get the parent path ...
        Location actualSiblingLocation = request.startingAfter();
        Path path = actualSiblingLocation.getPath();
        Path parentPath = null;
        if (path != null) parentPath = path.getParent();
        if (parentPath == null) {
            // Need to find the parent path, so get the actual location of the sibling ...
            VerifyNodeExistsRequest verifySibling = new VerifyNodeExistsRequest(request.startingAfter(), request.inWorkspace());
            process(verifySibling);
            actualSiblingLocation = verifySibling.getActualLocationOfNode();
            parentPath = actualSiblingLocation.getPath().getParent();
        }
        assert parentPath != null;

        // Convert the request to a ReadAllChildrenRequest and execute it ...
        ReadAllChildrenRequest readAll = new ReadAllChildrenRequest(Location.create(parentPath), request.inWorkspace());
        process(readAll);
        if (readAll.hasError()) {
            request.setError(readAll.getError());
            return;
        }
        List<Location> allChildren = readAll.getChildren();

        // Iterate through the children, looking for the 'startingAfter' child ...
        boolean found = false;
        int count = 0;
        for (Location child : allChildren) {
            if (count > request.count()) break;
            if (!found) {
                // Set to true if we find the child we're looking for ...
                found = child.isSame(request.startingAfter());
            } else {
                // Add the child to the block ...
                ++count;
                request.addChild(child);
            }
        }

        // Set the actual location ...
        request.setActualLocationOfStartingAfterNode(actualSiblingLocation);
        setCacheableInfo(request);
    }

    /**
     * Process a request to read a branch or subgraph that's below a node at a specified location.
     * <p>
     * This method does nothing if the request is null. The default implementation processes the branch by submitting the
     * equivalent requests to {@link ReadNodeRequest read the nodes} and the {@link ReadAllChildrenRequest children}. It starts by
     * doing this for the top-level node, then proceeds for each of the children of that node, and so forth.
     * </p>
     * 
     * @param request the request to read the branch
     */
    public void process( ReadBranchRequest request ) {
        if (request == null) return;
        // Create a queue for locations that need to be read ...
        Queue<LocationWithDepth> locationsToRead = new LinkedList<LocationWithDepth>();
        locationsToRead.add(new LocationWithDepth(request.at(), 1));
        int maxDepthPerRead = Math.min(request.maximumDepth(), absoluteMaximumDepthForBranchReads());

        // Now read the locations ...
        boolean first = true;
        while (locationsToRead.peek() != null) {
            if (request.isCancelled()) return;
            LocationWithDepth read = locationsToRead.poll();

            // Check the depth ...
            if (read.depth > maxDepthPerRead) break;

            // Read the properties ...
            ReadNodeRequest readNode = new ReadNodeRequest(read.location, request.inWorkspace());
            process(readNode);
            if (readNode.hasError()) {
                request.setError(readNode.getError());
                return;
            }

            Location actualLocation = readNode.getActualLocationOfNode();
            if (first) {
                // Set the actual location on the original request
                request.setActualLocationOfNode(actualLocation);
            }

            // Record in the request the children and properties that were read on this node ...
            request.setChildren(actualLocation, readNode.getChildren());
            request.setProperties(actualLocation, readNode.getProperties());

            if (includeChildrenInSubgraph(actualLocation, readNode.getPropertiesByName(), first)) {

                // Add each of the children to the list of locations that we need to read ...
                for (Location child : readNode.getChildren()) {
                    locationsToRead.add(new LocationWithDepth(child, read.depth + 1));
                }
            }

            if (first) first = false;
        }
        setCacheableInfo(request);
    }

    /**
     * This method is called from {@link #process(ReadBranchRequest)} when determining the maximum depth for the subgraph. By
     * default, this method returns {@link Integer#MAX_VALUE}, signaling that the ReadBranchRequest's
     * {@link ReadBranchRequest#maximumDepth() maximum depth} should be honored. However, subclasses can override this method to
     * return a constant value that will be used if less than the ReadBranchRequest's maximum depth.
     * 
     * @return the maximum read depth allowed by the processor; must be positive
     */
    protected int absoluteMaximumDepthForBranchReads() {
        return Integer.MAX_VALUE;
    }

    /**
     * This method is called from {@link #process(ReadBranchRequest)} when determining whether particular nodes should be included
     * in subgraph reads. For example, some processor implementations might want to always exclude nodes with certain names from
     * all subgraph reads. If this is the case, subclasses should override this method (which always returns true), and determine
     * whether the node at the supplied location should be included in the subgraph.
     * 
     * @param location the location of the parent node; never null
     * @param properties the properties of the parent node; never null
     * @param topOfSubgraph true if the parent node (identified by the location) is the root of the subgraph
     * @return true if the child nodes should be read and included in the subgraph, or false otherwise
     */
    protected boolean includeChildrenInSubgraph( Location location,
                                                 Map<Name, Property> properties,
                                                 boolean topOfSubgraph ) {
        return true;
    }

    /**
     * Process a request to read the properties of a node at the supplied location.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the read request
     */
    public abstract void process( ReadAllPropertiesRequest request );

    /**
     * Process a request to read the properties and children of a node at the supplied location.
     * <p>
     * This method does nothing if the request is null. Unless overridden, this method converts the single request into a
     * {@link ReadAllChildrenRequest} and a {@link ReadAllPropertiesRequest}.
     * </p>
     * 
     * @param request the read request
     */
    public void process( ReadNodeRequest request ) {
        if (request == null) return;
        // Read the properties ...
        ReadAllPropertiesRequest readProperties = new ReadAllPropertiesRequest(request.at(), request.inWorkspace());
        process(readProperties);
        if (readProperties.hasError()) {
            request.setError(readProperties.getError());
            return;
        }
        // Set the actual location ...
        request.setActualLocationOfNode(readProperties.getActualLocationOfNode());

        // Read the children ...
        ReadAllChildrenRequest readChildren = new ReadAllChildrenRequest(request.at(), request.inWorkspace());
        process(readChildren);
        if (readChildren.hasError()) {
            request.setError(readChildren.getError());
            return;
        }
        if (request.isCancelled()) return;
        // Now, copy all of the results into the submitted request ...
        for (Property property : readProperties) {
            request.addProperty(property);
        }
        for (Location child : readChildren) {
            request.addChild(child);
        }
        setCacheableInfo(request);
    }

    /**
     * Process a request to read a single property of a node at the supplied location.
     * <p>
     * This method does nothing if the request is null. Unless overridden, this method converts the request that
     * {@link ReadAllPropertiesRequest reads the node} and simply returns the one property.
     * </p>
     * 
     * @param request the read request
     */
    public void process( ReadPropertyRequest request ) {
        if (request == null) return;
        ReadAllPropertiesRequest readNode = new ReadAllPropertiesRequest(request.on(), request.inWorkspace());
        process(readNode);
        if (readNode.hasError()) {
            request.setError(readNode.getError());
            return;
        }
        Property property = readNode.getPropertiesByName().get(request.named());
        request.setProperty(property);
        // Set the actual location ...
        request.setActualLocationOfNode(readNode.getActualLocationOfNode());
        setCacheableInfo(request);
    }

    /**
     * Process a request to verify that a node exists at the supplied location.
     * <p>
     * This method does nothing if the request is null. Unless overridden, this method converts the request that
     * {@link ReadAllPropertiesRequest reads the node} and uses the result to determine if the node exists.
     * </p>
     * 
     * @param request the read request
     */
    public void process( VerifyNodeExistsRequest request ) {
        if (request == null) return;
        ReadAllPropertiesRequest readNode = new ReadAllPropertiesRequest(request.at(), request.inWorkspace());
        process(readNode);
        if (readNode.hasError()) {
            request.setError(readNode.getError());
            return;
        }
        // Set the actual location ...
        request.setActualLocationOfNode(readNode.getActualLocationOfNode());
        setCacheableInfo(request);
    }

    /**
     * Process a request to remove the specified property from a node.
     * <p>
     * This method does nothing if the request is null. Unless overridden, this method converts this request into a
     * {@link UpdatePropertiesRequest}.
     * </p>
     * 
     * @param request the request to remove the property
     */
    public void process( RemovePropertyRequest request ) {
        if (request == null) return;
        Map<Name, Property> properties = Collections.singletonMap(request.propertyName(), null);
        UpdatePropertiesRequest update = new UpdatePropertiesRequest(request.from(), request.inWorkspace(), properties);
        process(update);
        if (update.hasError()) {
            request.setError(update.getError());
        }
        // Set the actual location ...
        request.setActualLocationOfNode(update.getActualLocationOfNode());
    }

    /**
     * Process a request to set the specified property on a node.
     * <p>
     * This method does nothing if the request is null. Unless overridden, this method converts this request into a
     * {@link UpdatePropertiesRequest}.
     * </p>
     * 
     * @param request the request to set the property
     */
    public void process( SetPropertyRequest request ) {
        if (request == null) return;
        Property property = request.property();
        Map<Name, Property> properties = Collections.singletonMap(property.getName(), property);
        UpdatePropertiesRequest update = new UpdatePropertiesRequest(request.on(), request.inWorkspace(), properties);
        process(update);
        if (update.hasError()) {
            request.setError(update.getError());
        } else {
            // Set the actual location and created flags ...
            request.setActualLocationOfNode(update.getActualLocationOfNode());
            request.setNewProperty(update.isNewProperty(property.getName()));
        }
    }

    /**
     * Process a request to remove the specified properties from a node.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the remove request
     */
    public abstract void process( UpdatePropertiesRequest request );

    /**
     * Process a request to add and/or remove the specified values from a property on the given node.
     * <p>
     * This method does nothing if the request is null.
     * </p>
     * 
     * @param request the remove request
     */
    public void process( UpdateValuesRequest request ) {
        String workspaceName = request.inWorkspace();
        Location on = request.on();
        Name propertyName = request.property();

        // Read in the current values
        ReadPropertyRequest readProperty = new ReadPropertyRequest(on, workspaceName, propertyName);
        process(readProperty);

        if (readProperty.hasError()) {
            request.setError(readProperty.getError());
            return;
        }

        Property property = readProperty.getProperty();
        List<Object> actualRemovedValues = new ArrayList<Object>(request.removedValues().size());
        List<Object> newValues = property == null ? new LinkedList<Object>() : new LinkedList<Object>(
                                                                                                      Arrays.asList(property.getValuesAsArray()));
        // Calculate what the new values should be
        for (Object removedValue : request.removedValues()) {
            for (Iterator<Object> iter = newValues.iterator(); iter.hasNext();) {
                if (iter.next().equals(removedValue)) {
                    iter.remove();
                    actualRemovedValues.add(removedValue);
                    break;
                }
            }
        }

        newValues.addAll(request.addedValues());
        Property newProperty = getExecutionContext().getPropertyFactory().create(propertyName, newValues);

        // Update the current values
        SetPropertyRequest setProperty = new SetPropertyRequest(on, workspaceName, newProperty);
        process(setProperty);

        if (setProperty.hasError()) {
            request.setError(setProperty.getError());
        } else {
            // Set the actual location and property
            request.setActualLocation(setProperty.getActualLocationOfNode(), request.addedValues(), actualRemovedValues);
            request.setActualProperty(newProperty, setProperty.isNewProperty());
        }

    }

    /**
     * Process a request to rename a node specified location into a different location.
     * <p>
     * This method does nothing if the request is null. Unless overridden, this method converts the rename into a
     * {@link MoveBranchRequest move}. However, this only works if the <code>request</code> has a {@link Location#hasPath() path}
     * for its {@link RenameNodeRequest#at() location}. (If not, this method throws an {@link UnsupportedOperationException} and
     * must be overridden.)
     * </p>
     * 
     * @param request the rename request
     */
    public void process( RenameNodeRequest request ) {
        if (request == null) return;
        Location from = request.at();
        if (!from.hasPath()) {
            throw new UnsupportedOperationException();
        }
        Path newPath = getExecutionContext().getValueFactories().getPathFactory().create(from.getPath(), request.toName());
        Location to = Location.create(newPath);
        MoveBranchRequest move = new MoveBranchRequest(from, to, request.inWorkspace());
        process(move);
        // Set the actual locations ...
        request.setActualLocations(move.getActualLocationBefore(), move.getActualLocationAfter());
    }

    /**
     * Process a request to lock a node or branch within a workspace
     * <p>
     * The default implementation of this method does nothing, as most connectors will not support locking. Any implementation of
     * this method should do nothing if the request is null.
     * </p>
     * <p>
     * Implementations that do support locking should throw a {@link LockFailedException} if the request could not be fulfilled.
     * </p>
     * 
     * @param request the request
     */
    public void process( LockBranchRequest request ) {
        Location actualLocation = request.at();
        if (!actualLocation.hasPath()) {
            VerifyNodeExistsRequest nodeExists = new VerifyNodeExistsRequest(request.at(), request.inWorkspace());

            process(nodeExists);

            if (nodeExists.hasError()) {
                request.setError(nodeExists.getError());
                return;
            }

            actualLocation = nodeExists.getActualLocationOfNode();
        }

        request.setActualLocation(actualLocation);
    }

    /**
     * Process a request to unlock a node or branch within a workspace
     * <p>
     * The default implementation of this method does nothing, as most connectors will not support locking. Any implementation of
     * this method should do nothing if the request is null.
     * </p>
     * 
     * @param request the request
     */
    public void process( UnlockBranchRequest request ) {
        Location actualLocation = request.at();
        if (!actualLocation.hasPath()) {
            VerifyNodeExistsRequest nodeExists = new VerifyNodeExistsRequest(request.at(), request.inWorkspace());

            process(nodeExists);

            if (nodeExists.hasError()) {
                request.setError(nodeExists.getError());
                return;
            }

            actualLocation = nodeExists.getActualLocationOfNode();
        }

        request.setActualLocation(actualLocation);
    }

    /**
     * Process a request to query a workspace with an access query, which is is a low-level atomic query that is part of a larger,
     * planned query.
     * <p>
     * The default implementation of this method behaves as though the implementation does not support queries by setting an error
     * on the request
     * </p>
     * 
     * @param request the request
     */
    public void process( AccessQueryRequest request ) {
        processUnknownRequest(request);
    }

    /**
     * Process a request to search a workspace.
     * <p>
     * The default implementation of this method behaves as though the implementation does not support full-text searches by
     * setting an error on the request
     * </p>
     * 
     * @param request the request
     */
    public void process( FullTextSearchRequest request ) {
        processUnknownRequest(request);
    }

    /**
     * Process a request to collect garbage.
     * <p>
     * The default implementation of this method does nothing.
     * </p>
     * 
     * @param request the request
     */
    public void process( CollectGarbageRequest request ) {
        // do nothing by default
    }

    /**
     * Close this processor, allowing it to clean up any open resources.
     */
    public void close() {
        // do nothing by default
    }

    /**
     * Obtain the list of {@link ChangeRequest}s that were successfully processed by this processor.
     * <p>
     * Note that this list is modified during processing and thus should only be accessed by the caller when this processor has
     * been {@link #close() closed}.
     * </p>
     * <p>
     * Also, if this processor encounters errors while processing {@link Request#isReadOnly() change requests}, the processor does
     * not throw out any of the changes. Thus it is up to the caller to decide whether any of the changes are to be kept.
     * </p>
     * 
     * @return the list of successful changes; never null but possibly empty.
     */
    public List<ChangeRequest> getChanges() {
        return changes;
    }

    /**
     * Take any of the changes that have been accumulated by this processor and notify the observer. This should only be called
     * after {@link #close()} has been called.
     */
    public void notifyObserverOfChanges() {
        if (observer == null) {
            if (changes != null) changes.clear();
            return;
        }
        if (this.changes.isEmpty()) return;
        // then publish any changes ...
        String userName = context.getSecurityContext() != null ? context.getSecurityContext().getUserName() : null;
        if (userName == null) userName = "";
        String contextId = context.getId();
        String processId = context.getProcessId();
        Map<String, String> userData = context.getData();
        Changes changes = new Changes(processId, contextId, userName, getSourceName(), getNowInUtc(), this.changes, userData);
        observer.notify(changes);
        // Null the list, since this should have been closed
        this.changes = null;
    }

    /**
     * A class that represents a location at a known depth
     * 
     * @author Randall Hauch
     */
    @Immutable
    protected class LocationWithDepth {
        protected final Location location;
        protected final int depth;

        public LocationWithDepth( Location location,
                                  int depth ) {
            this.location = location;
            this.depth = depth;
        }

        public Location getLocation() {
            return location;
        }

        public int getDepth() {
            return depth;
        }

        @Override
        public int hashCode() {
            return location.hashCode();
        }

        @Override
        public String toString() {
            return location.toString() + " at depth " + depth;
        }
    }

}
