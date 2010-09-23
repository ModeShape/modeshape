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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.Location;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.query.QueryResults;
import org.modeshape.graph.request.AccessQueryRequest;
import org.modeshape.graph.request.CloneBranchRequest;
import org.modeshape.graph.request.CloneWorkspaceRequest;
import org.modeshape.graph.request.CopyBranchRequest;
import org.modeshape.graph.request.CreateNodeRequest;
import org.modeshape.graph.request.CreateWorkspaceRequest;
import org.modeshape.graph.request.DeleteBranchRequest;
import org.modeshape.graph.request.DestroyWorkspaceRequest;
import org.modeshape.graph.request.FullTextSearchRequest;
import org.modeshape.graph.request.GetWorkspacesRequest;
import org.modeshape.graph.request.InvalidRequestException;
import org.modeshape.graph.request.InvalidWorkspaceException;
import org.modeshape.graph.request.LockBranchRequest;
import org.modeshape.graph.request.MoveBranchRequest;
import org.modeshape.graph.request.ReadAllChildrenRequest;
import org.modeshape.graph.request.ReadAllPropertiesRequest;
import org.modeshape.graph.request.ReadNodeRequest;
import org.modeshape.graph.request.Request;
import org.modeshape.graph.request.UnlockBranchRequest;
import org.modeshape.graph.request.UpdatePropertiesRequest;
import org.modeshape.graph.request.VerifyNodeExistsRequest;
import org.modeshape.graph.request.VerifyWorkspaceRequest;
import org.modeshape.graph.request.processor.RequestProcessor;

/**
 * The default implementation of the {@link RequestProcessor} for map repositories.
 * 
 * @param <NodeType> the node type
 * @param <WorkspaceType> the workspace type
 */
public class Processor<NodeType extends Node, WorkspaceType extends Workspace> extends RequestProcessor {
    private final PathFactory pathFactory;
    private final PropertyFactory propertyFactory;
    private final Repository<NodeType, WorkspaceType> repository;
    private final boolean updatesAllowed;
    private final Transaction<NodeType, WorkspaceType> txn;

    public Processor( Transaction<NodeType, WorkspaceType> txn,
                      Repository<NodeType, WorkspaceType> repository,
                      Observer observer,
                      boolean updatesAllowed ) {
        super(repository.getSourceName(), txn.getContext(), observer);
        this.txn = txn;
        this.repository = repository;
        this.pathFactory = txn.getContext().getValueFactories().getPathFactory();
        this.propertyFactory = txn.getContext().getPropertyFactory();
        this.updatesAllowed = updatesAllowed;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadNodeRequest)
     */
    @Override
    public void process( ReadNodeRequest request ) {
        WorkspaceType workspace = getWorkspace(request, request.inWorkspace());
        NodeType node = getTargetNode(workspace, request, request.at());
        if (node == null) {
            assert request.hasError();
            return;
        }

        Location actualLocation = getActualLocation(workspace, request.at(), node);
        assert actualLocation != null;

        // Get the locations of the children ...
        List<Location> childLocations = txn.getChildrenLocations(workspace, node);
        request.addChildren(childLocations);

        // Get the properties of the node ...
        request.addProperty(propertyFactory.create(ModeShapeLexicon.UUID, node.getUuid()));
        request.addProperties(node.getProperties().values());

        request.setActualLocationOfNode(actualLocation);
        setCacheableInfo(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadAllChildrenRequest)
     */
    @Override
    public void process( ReadAllChildrenRequest request ) {
        WorkspaceType workspace = getWorkspace(request, request.inWorkspace());
        NodeType node = getTargetNode(workspace, request, request.of());
        if (node == null) {
            assert request.hasError();
            return;
        }

        Location actualLocation = getActualLocation(workspace, request.of(), node);
        assert actualLocation != null;
        Path path = actualLocation.getPath();
        // Get the names of the children ...
        List<NodeType> children = txn.getChildren(workspace, node);
        for (Node child : children) {
            Segment childName = child.getName();
            Path childPath = pathFactory.create(path, childName);
            Location childLocation = null;

            if (child.getUuid() != null) {
                childLocation = Location.create(childPath, child.getUuid());
            } else {
                childLocation = Location.create(childPath);
            }
            request.addChild(childLocation);
        }
        request.setActualLocationOfNode(actualLocation);
        setCacheableInfo(request);
    }

    @Override
    public void process( LockBranchRequest request ) {
        WorkspaceType workspace = getWorkspace(request, request.inWorkspace());
        NodeType node = getTargetNode(workspace, request, request.at());
        if (node == null) return;

        txn.lockNode(workspace, node, request.lockScope(), request.lockTimeoutInMillis());

        Location actualLocation = getActualLocation(workspace, request.at(), node);
        request.setActualLocation(actualLocation);
        recordChange(request);
    }

    @Override
    public void process( UnlockBranchRequest request ) {
        WorkspaceType workspace = getWorkspace(request, request.inWorkspace());
        NodeType node = getTargetNode(workspace, request, request.at());
        if (node == null) return;

        txn.unlockNode(workspace, node);

        Location actualLocation = getActualLocation(workspace, request.at(), node);
        request.setActualLocation(actualLocation);
        recordChange(request);
    }

    @Override
    public void process( ReadAllPropertiesRequest request ) {
        WorkspaceType workspace = getWorkspace(request, request.inWorkspace());
        NodeType node = getTargetNode(workspace, request, request.at());
        if (node == null) {
            assert request.hasError();
            return;
        }

        // Get the properties of the node ...
        Location actualLocation = getActualLocation(workspace, request.at(), node);
        request.addProperty(propertyFactory.create(ModeShapeLexicon.UUID, node.getUuid()));
        request.addProperties(node.getProperties().values());

        assert actualLocation != null;
        request.setActualLocationOfNode(actualLocation);
        setCacheableInfo(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CloneBranchRequest)
     */
    @Override
    public void process( CloneBranchRequest request ) {
        if (!updatesAllowed(request)) return;

        WorkspaceType workspace = getWorkspace(request, request.fromWorkspace());
        WorkspaceType newWorkspace = getWorkspace(request, request.intoWorkspace());
        if (workspace == null || newWorkspace == null) return;
        NodeType node = getTargetNode(workspace, request, request.from());
        if (node == null) return;

        // Look up the new parent, which must exist ...
        Path newParentPath = request.into().getPath();
        NodeType newParent = txn.getNode(newWorkspace, request.into());
        Set<Location> removedExistingNodes = new HashSet<Location>();
        NodeType newNode = txn.cloneNode(workspace,
                                         node,
                                         newWorkspace,
                                         newParent,
                                         request.desiredName(),
                                         request.desiredSegment(),
                                         request.removeExisting(),
                                         removedExistingNodes);
        Path newPath = getExecutionContext().getValueFactories().getPathFactory().create(newParentPath, newNode.getName());
        Location oldLocation = getActualLocation(workspace, request.from(), node);
        Location newLocation = Location.create(newPath, newNode.getUuid());
        request.setActualLocations(oldLocation, newLocation);
        request.setRemovedNodes(Collections.unmodifiableSet(removedExistingNodes));
        recordChange(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CopyBranchRequest)
     */
    @Override
    public void process( CopyBranchRequest request ) {
        if (!updatesAllowed(request)) return;

        WorkspaceType workspace = getWorkspace(request, request.fromWorkspace());
        WorkspaceType newWorkspace = getWorkspace(request, request.intoWorkspace());
        if (workspace == null || newWorkspace == null) return;
        NodeType node = getTargetNode(workspace, request, request.from());
        if (node == null) return;

        // Look up the new parent, which must exist ...
        Path newParentPath = request.into().getPath();
        Name desiredName = request.desiredName();
        NodeType newParent = getTargetNode(newWorkspace, request, request.into());
        if (newParent == null) return;
        NodeType newNode = txn.copyNode(workspace, node, newWorkspace, newParent, desiredName, true);
        Path newPath = getExecutionContext().getValueFactories().getPathFactory().create(newParentPath, newNode.getName());
        Location oldLocation = getActualLocation(workspace, request.from(), node);
        Location newLocation = Location.create(newPath, newNode.getUuid());
        request.setActualLocations(oldLocation, newLocation);
        recordChange(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CreateNodeRequest)
     */
    @Override
    public void process( CreateNodeRequest request ) {
        if (!updatesAllowed(request)) return;

        WorkspaceType workspace = getWorkspace(request, request.inWorkspace());
        if (workspace == null) return;

        // Look up the parent node, which must exist ...
        NodeType parentNode = txn.getNode(workspace, request.under());

        UUID uuid = null;
        // Make a list of the properties that we will store: all props except dna:uuid and jcr:uuid
        List<Property> propsToStore = new ArrayList<Property>(request.properties().size());
        for (Property property : request.properties()) {
            if (property.getName().equals(ModeShapeLexicon.UUID) || property.getName().equals(JcrLexicon.UUID)) {
                uuid = getExecutionContext().getValueFactories().getUuidFactory().create(property.getValues().next());
            } else {
                if (property.size() > 0) propsToStore.add(property);
            }
        }

        NodeType node = null;
        switch (request.conflictBehavior()) {
            case APPEND:
                node = txn.addChild(workspace, parentNode, request.named(), -1, uuid, propsToStore);
                break;
            case DO_NOT_REPLACE:
                node = txn.getFirstChild(workspace, parentNode, request.named());
                if (node == null) {
                    node = txn.addChild(workspace, parentNode, request.named(), -1, uuid, propsToStore);
                }
                break;
            case REPLACE:
                // See if the node already exists (this doesn't record an error on the request) ...
                node = txn.getFirstChild(workspace, parentNode, request.named());
                if (node != null) {
                    List<NodeType> children = txn.getChildren(workspace, node);
                    for (NodeType child : children) {
                        txn.removeNode(workspace, child);
                    }
                    txn.setProperties(workspace, node, propsToStore, null, true);
                    // txn.removeNode(workspace, node);
                    // node = txn.addChild(workspace, parentNode, request.named(), 1, uuid, propsToStore);
                } else {
                    node = txn.addChild(workspace, parentNode, request.named(), -1, uuid, propsToStore);
                }
                break;
            case UPDATE:
                // See if the node already exists (this doesn't record an error on the request) ...
                node = txn.getFirstChild(workspace, parentNode, request.named());
                if (node == null) {
                    node = txn.addChild(workspace, parentNode, request.named(), -1, uuid, propsToStore);
                } else {
                    // otherwise, we found it and add the properties (which we'll do later on)...
                }
                break;
        }
        assert node != null;
        Path path = txn.pathFor(workspace, node);
        Location actualLocation = getActualLocation(workspace, Location.create(path), node);
        request.setActualLocationOfNode(actualLocation);
        recordChange(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.DeleteBranchRequest)
     */
    @Override
    public void process( DeleteBranchRequest request ) {
        if (!updatesAllowed(request)) return;

        WorkspaceType workspace = getWorkspace(request, request.inWorkspace());
        if (workspace == null) return;
        NodeType node = getTargetNode(workspace, request, request.at());
        if (node == null) return;
        Location actualLocation = txn.removeNode(workspace, node);
        request.setActualLocationOfNode(actualLocation);
        recordChange(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.MoveBranchRequest)
     */
    @Override
    public void process( MoveBranchRequest request ) {
        if (!updatesAllowed(request)) return;

        WorkspaceType workspace = getWorkspace(request, request.inWorkspace());
        if (workspace == null) return;

        NodeType beforeNode = request.before() != null ? getTargetNode(workspace, request, request.before()) : null;
        NodeType node = getTargetNode(workspace, request, request.from());
        if (node == null) return;
        if (request.hasError()) return; // if beforeNode could not be found
        Location oldLocation = getActualLocation(workspace, request.from(), node);

        // Look up the new parent, which must exist ...

        NodeType newParent = null;
        Location newLocation = null;
        if (request.into() != null) {
            newParent = txn.getNode(workspace, request.into()); // this will fail if there is no node
            newLocation = txn.addChild(workspace, newParent, node, null, request.desiredName());
        } else {
            // into or before cannot both be null
            assert beforeNode != null;
            // The new parent is the parent of the before node ...
            newParent = txn.getParent(workspace, beforeNode);
            if (newParent == null) {
                // The before node must be the root ...
                request.setError(new PathNotFoundException(request.into(), pathFactory.createRootPath(),
                                                           GraphI18n.nodeDoesNotExist.text("parent of root")));
                return;
            }
            // Move the node into the parent of the beforeNode ...
            newLocation = txn.addChild(workspace, newParent, node, beforeNode, request.desiredName());
        }
        assert newParent != null;

        request.setActualLocations(oldLocation, newLocation);
        recordChange(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.UpdatePropertiesRequest)
     */
    @Override
    public void process( UpdatePropertiesRequest request ) {
        if (!updatesAllowed(request)) return;

        WorkspaceType workspace = getWorkspace(request, request.inWorkspace());
        NodeType node = getTargetNode(workspace, request, request.on());
        if (node == null) return;
        // Figure out which properties should be set ...
        List<Property> propertiesToSet = null;
        Set<Name> propertiesToRemove = null;
        for (Map.Entry<Name, Property> propertyEntry : request.properties().entrySet()) {
            Property property = propertyEntry.getValue();
            if (property == null) {
                if (propertiesToRemove == null) propertiesToRemove = new HashSet<Name>();
                propertiesToRemove.add(propertyEntry.getKey());
                continue;
            }
            Name propName = property.getName();
            if (!propName.equals(ModeShapeLexicon.UUID)) {
                if (node.getProperties().get(propName) == null) {
                    // It is a new property ...
                    request.setNewProperty(propName);
                }
                if (propertiesToSet == null) propertiesToSet = new LinkedList<Property>();
                propertiesToSet.add(property);
            }
        }

        txn.setProperties(workspace, node, propertiesToSet, propertiesToRemove, request.removeOtherProperties());
        Location actualLocation = getActualLocation(workspace, request.on(), node);
        request.setActualLocationOfNode(actualLocation);
        recordChange(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CreateWorkspaceRequest)
     */
    @Override
    public void process( CreateWorkspaceRequest request ) {
        if (!updatesAllowed(request)) return;

        String clonedWorkspaceName = null;
        WorkspaceType workspace = repository.createWorkspace(txn,
                                                             request.desiredNameOfNewWorkspace(),
                                                             request.conflictBehavior(),
                                                             clonedWorkspaceName);
        if (workspace == null) {
            String msg = GraphI18n.workspaceAlreadyExistsInRepository.text(request.desiredNameOfNewWorkspace(),
                                                                           repository.getSourceName());
            request.setError(new InvalidWorkspaceException(msg));
        } else {
            Node root = txn.getRootNode(workspace);
            request.setActualRootLocation(Location.create(pathFactory.createRootPath(), root.getUuid()));
            request.setActualWorkspaceName(workspace.getName());
            recordChange(request);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.DestroyWorkspaceRequest)
     */
    @Override
    public void process( DestroyWorkspaceRequest request ) {
        if (!updatesAllowed(request)) return;

        WorkspaceType workspace = repository.getWorkspace(txn, request.workspaceName());
        if (workspace != null) {
            Node root = txn.getRootNode(workspace);
            try {
                txn.destroyWorkspace(workspace);
                request.setActualRootLocation(Location.create(pathFactory.createRootPath(), root.getUuid()));
                recordChange(request);
            } catch (RuntimeException e) {
                request.setError(e);
            }
        } else {
            String msg = GraphI18n.workspaceDoesNotExistInRepository.text(request.workspaceName(), repository.getSourceName());
            request.setError(new InvalidWorkspaceException(msg));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.GetWorkspacesRequest)
     */
    @Override
    public void process( GetWorkspacesRequest request ) {
        // Always ask the transaction (in case a new workspace was added by another process) ...
        Set<String> names = txn.getWorkspaceNames();
        request.setAvailableWorkspaceNames(new HashSet<String>(names));
        setCacheableInfo(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.VerifyWorkspaceRequest)
     */
    @Override
    public void process( VerifyWorkspaceRequest request ) {
        WorkspaceType original = getWorkspace(request, request.workspaceName());
        if (original != null) {
            Path path = getExecutionContext().getValueFactories().getPathFactory().createRootPath();
            Node root = txn.getRootNode(original);
            request.setActualRootLocation(Location.create(path, root.getUuid()));
            request.setActualWorkspaceName(original.getName());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.VerifyNodeExistsRequest)
     */
    @Override
    public void process( VerifyNodeExistsRequest request ) {
        WorkspaceType original = getWorkspace(request, request.inWorkspace());
        if (original != null) {
            try {
                Location actualLoation = txn.verifyNodeExists(original, request.at());
                request.setActualLocationOfNode(actualLoation);
            } catch (RuntimeException e) {
                request.setError(e);
            }
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.CloneWorkspaceRequest)
     */
    @Override
    public void process( CloneWorkspaceRequest request ) {
        if (!updatesAllowed(request)) return;

        // Find the original workspace that we're cloning ...
        String targetWorkspaceName = request.desiredNameOfTargetWorkspace();
        String nameOfWorkspaceToBeCloned = request.nameOfWorkspaceToBeCloned();
        WorkspaceType original = repository.getWorkspace(txn, nameOfWorkspaceToBeCloned);
        WorkspaceType target = repository.getWorkspace(txn, targetWorkspaceName);

        if (target != null) {
            String msg = GraphI18n.workspaceAlreadyExistsInRepository.text(targetWorkspaceName, repository.getSourceName());
            request.setError(new InvalidWorkspaceException(msg));
            return;
        }

        if (original == null) {
            switch (request.cloneConflictBehavior()) {
                case DO_NOT_CLONE:
                    String msg = GraphI18n.workspaceDoesNotExistInRepository.text(nameOfWorkspaceToBeCloned,
                                                                                  repository.getSourceName());
                    request.setError(new InvalidWorkspaceException(msg));
                    return;
                case SKIP_CLONE:
                    nameOfWorkspaceToBeCloned = null;
                    break;
            }
        }
        assert original != null;
        target = repository.createWorkspace(txn, targetWorkspaceName, request.targetConflictBehavior(), nameOfWorkspaceToBeCloned);
        assert target != null;
        NodeType root = txn.getRootNode(target);
        request.setActualRootLocation(Location.create(pathFactory.createRootPath(), root.getUuid()));
        request.setActualWorkspaceName(target.getName());
        recordChange(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.AccessQueryRequest)
     */
    @Override
    public void process( AccessQueryRequest request ) {
        WorkspaceType workspace = getWorkspace(request, request.workspace());
        if (workspace == null) return;
        QueryResults results = txn.query(workspace, request);
        if (results != null) {
            request.setResults(results.getTuples(), results.getStatistics());
        } else {
            super.processUnknownRequest(request);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.FullTextSearchRequest)
     */
    @Override
    public void process( FullTextSearchRequest request ) {
        WorkspaceType workspace = getWorkspace(request, request.workspace());
        if (workspace == null) return;
        QueryResults results = txn.search(workspace, request);
        if (results != null) {
            request.setResults(results.getColumns(), results.getTuples(), results.getStatistics());
        } else {
            super.processUnknownRequest(request);
        }
    }

    protected Location getActualLocation( WorkspaceType workspace,
                                          Location location,
                                          NodeType node ) {
        Path path = txn.pathFor(workspace, node); // the location's path might not be right
        if (location.hasIdProperties()) {
            return location.with(path);
        }
        return Location.create(path, node.getUuid());
    }

    protected WorkspaceType getWorkspace( Request request,
                                          String workspaceName ) {
        // Get the workspace for this request ...
        WorkspaceType workspace = repository.getWorkspace(txn, workspaceName);
        if (workspace == null) {
            String msg = GraphI18n.workspaceDoesNotExistInRepository.text(workspaceName, repository.getSourceName());
            request.setError(new InvalidWorkspaceException(msg));
        }
        return workspace;
    }

    protected boolean updatesAllowed( Request request ) {
        if (!updatesAllowed) {
            request.setError(new InvalidRequestException(GraphI18n.sourceIsReadOnly.text(getSourceName())));
        }
        return !request.hasError();
    }

    protected NodeType getTargetNode( WorkspaceType workspace,
                                      Request request,
                                      Location location ) {
        if (workspace != null) {
            try {
                return txn.getNode(workspace, location);
            } catch (PathNotFoundException e) {
                request.setError(e);
            } catch (RuntimeException e) {
                request.setError(e);
            }
        }
        return null;
    }
}
