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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ModeShapeLexicon;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.Location;
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
import org.modeshape.graph.request.Request;
import org.modeshape.graph.request.UnlockBranchRequest;
import org.modeshape.graph.request.UpdatePropertiesRequest;
import org.modeshape.graph.request.VerifyWorkspaceRequest;
import org.modeshape.graph.request.processor.RequestProcessor;

/**
 * The default implementation of the {@link RequestProcessor} for map repositories.
 */
public class MapRequestProcessor extends RequestProcessor {
    private final PathFactory pathFactory;
    private final PropertyFactory propertyFactory;
    private final MapRepository repository;
    private final boolean updatesAllowed;

    public MapRequestProcessor( ExecutionContext context,
                                MapRepository repository,
                                Observer observer,
                                boolean updatesAllowed ) {
        super(repository.getSourceName(), context, observer);
        this.repository = repository;
        pathFactory = context.getValueFactories().getPathFactory();
        propertyFactory = context.getPropertyFactory();
        this.updatesAllowed = updatesAllowed;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.request.processor.RequestProcessor#process(org.modeshape.graph.request.ReadAllChildrenRequest)
     */
    @Override
    public void process( ReadAllChildrenRequest request ) {
        MapWorkspace workspace = getWorkspace(request, request.inWorkspace());
        MapNode node = getTargetNode(workspace, request, request.of());
        if (node == null) {
            assert request.hasError();
            return;
        }

        Location actualLocation = getActualLocation(request.of(), node);
        assert actualLocation != null;
        Path path = actualLocation.getPath();
        // Get the names of the children ...
        List<MapNode> children = node.getChildren();
        for (MapNode child : children) {
            Segment childName = child.getName();
            Path childPath = pathFactory.create(path, childName);
            request.addChild(childPath, propertyFactory.create(ModeShapeLexicon.UUID, child.getUuid()));
        }
        request.setActualLocationOfNode(actualLocation);
        setCacheableInfo(request);
    }

    @Override
    public void process( LockBranchRequest request ) {
        MapWorkspace workspace = getWorkspace(request, request.inWorkspace());
        MapNode node = getTargetNode(workspace, request, request.at());
        if (node == null) return;

        workspace.lockNode(node, request.lockScope(), request.lockTimeoutInMillis());

        Location actualLocation = getActualLocation(request.at(), node);
        request.setActualLocation(actualLocation);
        recordChange(request);
    }

    @Override
    public void process( UnlockBranchRequest request ) {
        MapWorkspace workspace = getWorkspace(request, request.inWorkspace());
        MapNode node = getTargetNode(workspace, request, request.at());
        if (node == null) return;

        workspace.unlockNode(node);

        Location actualLocation = getActualLocation(request.at(), node);
        request.setActualLocation(actualLocation);
        recordChange(request);
    }

    @Override
    public void process( ReadAllPropertiesRequest request ) {
        MapWorkspace workspace = getWorkspace(request, request.inWorkspace());
        MapNode node = getTargetNode(workspace, request, request.at());
        if (node == null) {
            assert request.hasError();
            return;
        }

        // Get the properties of the node ...
        Location actualLocation = getActualLocation(request.at(), node);
        request.addProperty(propertyFactory.create(ModeShapeLexicon.UUID, node.getUuid()));
        for (Property property : node.getProperties().values()) {
            request.addProperty(property);
        }

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

        MapWorkspace workspace = getWorkspace(request, request.fromWorkspace());
        MapWorkspace newWorkspace = getWorkspace(request, request.intoWorkspace());
        if (workspace == null || newWorkspace == null) return;
        MapNode node = getTargetNode(workspace, request, request.from());
        if (node == null) return;

        // Look up the new parent, which must exist ...
        Path newParentPath = request.into().getPath();
        MapNode newParent = newWorkspace.getNode(newParentPath);
        Set<Location> removedExistingNodes = new HashSet<Location>();
        MapNode newNode = workspace.cloneNode(getExecutionContext(),
                                              node,
                                              newWorkspace,
                                              newParent,
                                              request.desiredName(),
                                              request.desiredSegment(),
                                              request.removeExisting(),
                                              removedExistingNodes);
        Path newPath = getExecutionContext().getValueFactories().getPathFactory().create(newParentPath, newNode.getName());
        Location oldLocation = getActualLocation(request.from(), node);
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

        MapWorkspace workspace = getWorkspace(request, request.fromWorkspace());
        MapWorkspace newWorkspace = getWorkspace(request, request.intoWorkspace());
        if (workspace == null || newWorkspace == null) return;
        MapNode node = getTargetNode(workspace, request, request.from());
        if (node == null) return;

        // Look up the new parent, which must exist ...
        Path newParentPath = request.into().getPath();
        Name desiredName = request.desiredName();
        MapNode newParent = newWorkspace.getNode(newParentPath);
        MapNode newNode = workspace.copyNode(getExecutionContext(), node, newWorkspace, newParent, desiredName, true);
        Path newPath = getExecutionContext().getValueFactories().getPathFactory().create(newParentPath, newNode.getName());
        Location oldLocation = getActualLocation(request.from(), node);
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

        MapWorkspace workspace = getWorkspace(request, request.inWorkspace());
        if (workspace == null) return;
        Path parent = request.under().getPath();
        CheckArg.isNotNull(parent, "request.under().getPath()");
        MapNode node = null;
        // Look up the parent node, which must exist ...

        MapNode parentNode = workspace.getNode(parent);
        if (parentNode == null) {
            Path lowestExisting = workspace.getLowestExistingPath(parent);
            request.setError(new PathNotFoundException(request.under(), lowestExisting, GraphI18n.nodeDoesNotExist.text(parent)));
            return;
        }

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

        switch (request.conflictBehavior()) {
            case APPEND:
                node = workspace.createNode(getExecutionContext(), parentNode, request.named(), uuid, propsToStore);
                break;
            case DO_NOT_REPLACE:
                for (MapNode child : parentNode.getChildren()) {
                    if (request.named().equals(child.getName().getName())) {
                        node = child;
                        break;
                    }
                }
                if (node == null) {
                    node = workspace.createNode(getExecutionContext(), parentNode, request.named(), uuid, propsToStore);
                }
                break;
            case REPLACE:
                // See if the node already exists (this doesn't record an error on the request) ...
                node = getTargetNode(workspace, null, Location.create(pathFactory.create(parent, request.named()), uuid));
                if (node != null) {
                    workspace.removeNode(getExecutionContext(), node);
                }
                node = workspace.createNode(getExecutionContext(), parentNode, request.named(), uuid, propsToStore);
                break;
            case UPDATE:
                // See if the node already exists (this doesn't record an error on the request) ...
                node = getTargetNode(workspace, null, Location.create(pathFactory.create(parent, request.named()), uuid));
                if (node == null) {
                    node = workspace.createNode(getExecutionContext(), parentNode, request.named(), uuid, propsToStore);
                } // otherwise, we found it and we're setting any properties below
                break;
        }
        assert node != null;
        Path path = getExecutionContext().getValueFactories().getPathFactory().create(parent, node.getName());

        Location actualLocation = getActualLocation(Location.create(path), node);
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

        MapWorkspace workspace = getWorkspace(request, request.inWorkspace());
        if (workspace == null) return;
        MapNode node = getTargetNode(workspace, request, request.at());
        if (node == null) return;
        workspace.removeNode(getExecutionContext(), node);
        Location actualLocation = getActualLocation(request.at(), node);
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

        MapWorkspace workspace = getWorkspace(request, request.inWorkspace());
        if (workspace == null) return;

        MapNode beforeNode = request.before() != null ? getTargetNode(workspace, request, request.before()) : null;
        MapNode node = getTargetNode(workspace, request, request.from());
        if (node == null) return;
        if (request.hasError()) return; // if beforeNode could not be found
        // Look up the new parent, which must exist ...
        Path newParentPath;

        if (request.into() != null) {
            newParentPath = request.into().getPath();
        } else {
            // into or before cannot both be null
            assert beforeNode != null;

            // Build the path from the before node to the root.
            LinkedList<Path.Segment> segments = new LinkedList<Path.Segment>();
            MapNode current = beforeNode.getParent();
            while (!current.equals(workspace.getRoot())) {
                segments.addFirst(current.getName());
                current = current.getParent();
            }
            newParentPath = getExecutionContext().getValueFactories().getPathFactory().createAbsolutePath(segments);
        }

        MapNode newParent = workspace.getNode(newParentPath);
        if (newParent == null) {
            Path lowestExisting = workspace.getLowestExistingPath(newParentPath);
            request.setError(new PathNotFoundException(request.into(), lowestExisting,
                                                       GraphI18n.nodeDoesNotExist.text(newParentPath)));
            return;
        }
        workspace.moveNode(getExecutionContext(), node, request.desiredName(), workspace, newParent, beforeNode);
        assert node.getParent().equals(newParent);
        Path newPath = getExecutionContext().getValueFactories().getPathFactory().create(newParentPath, node.getName());
        Location oldLocation = getActualLocation(request.from(), node);
        Location newLocation = Location.create(newPath, node.getUuid());
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

        MapWorkspace workspace = getWorkspace(request, request.inWorkspace());
        MapNode node = getTargetNode(workspace, request, request.on());
        if (node == null) return;
        // Now set (or remove) the properties to the supplied node ...
        for (Map.Entry<Name, Property> propertyEntry : request.properties().entrySet()) {
            Property property = propertyEntry.getValue();
            if (property == null) {
                node.removeProperty(propertyEntry.getKey());
                continue;
            }
            Name propName = property.getName();
            if (!propName.equals(ModeShapeLexicon.UUID)) {
                if (node.getProperties().get(propName) == null) {
                    // It is a new property ...
                    request.setNewProperty(propName);
                }
                node.setProperty(property);
            }
        }
        Location actualLocation = getActualLocation(request.on(), node);
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

        MapWorkspace workspace = repository.createWorkspace(getExecutionContext(),
                                                            request.desiredNameOfNewWorkspace(),
                                                            request.conflictBehavior());
        if (workspace == null) {
            String msg = GraphI18n.workspaceAlreadyExistsInRepository.text(request.desiredNameOfNewWorkspace(),
                                                                           repository.getSourceName());
            request.setError(new InvalidWorkspaceException(msg));
        } else {
            MapNode root = workspace.getRoot();
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

        MapWorkspace workspace = repository.getWorkspace(request.workspaceName());
        if (workspace != null) {
            MapNode root = workspace.getRoot();
            request.setActualRootLocation(Location.create(pathFactory.createRootPath(), root.getUuid()));
            recordChange(request);
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
        Set<String> names = repository.getWorkspaceNames();
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
        MapWorkspace original = getWorkspace(request, request.workspaceName());
        if (original != null) {
            Path path = getExecutionContext().getValueFactories().getPathFactory().createRootPath();
            request.setActualRootLocation(Location.create(path, original.getRoot().getUuid()));
            request.setActualWorkspaceName(original.getName());
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
        final ExecutionContext context = getExecutionContext();
        String targetWorkspaceName = request.desiredNameOfTargetWorkspace();
        String nameOfWorkspaceToBeCloned = request.nameOfWorkspaceToBeCloned();
        MapWorkspace original = repository.getWorkspace(nameOfWorkspaceToBeCloned);
        MapWorkspace target = repository.getWorkspace(targetWorkspaceName);

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
                    target = repository.createWorkspace(context, targetWorkspaceName, request.targetConflictBehavior());
                    assert target != null;

                    MapNode root = target.getRoot();
                    request.setActualRootLocation(Location.create(pathFactory.createRootPath(), root.getUuid()));
                    request.setActualWorkspaceName(target.getName());
                    return;
            }
        }
        assert original != null;
        target = repository.createWorkspace(context,
                                            targetWorkspaceName,
                                            request.targetConflictBehavior(),
                                            nameOfWorkspaceToBeCloned);
        assert target != null;
        MapNode root = target.getRoot();
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
        MapWorkspace workspace = getWorkspace(request, request.workspace());
        if (workspace == null) return;
        final ExecutionContext context = getExecutionContext();
        QueryResults results = workspace.query(context, request);
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
        MapWorkspace workspace = getWorkspace(request, request.workspace());
        if (workspace == null) return;
        final ExecutionContext context = getExecutionContext();
        QueryResults results = workspace.search(context, request.expression());
        if (results != null) {
            request.setResults(results.getColumns(), results.getTuples(), results.getStatistics());
        } else {
            super.processUnknownRequest(request);
        }
    }

    protected Location getActualLocation( Location location,
                                          MapNode node ) {
        Path path = location.getPath();
        if (path == null) {
            // Find the path on the node ...
            LinkedList<Path.Segment> segments = new LinkedList<Path.Segment>();
            MapNode n = node;
            while (n != null) {
                if (n.getParent() == null) break;
                segments.addFirst(n.getName());
                n = n.getParent();
            }
            path = pathFactory.createAbsolutePath(segments);
        }
        // If there is a UUID in the location, it should match the node's.
        assert location.getUuid() == null || location.getUuid().equals(node.getUuid());
        if (location.hasIdProperties()) {
            return location.with(path);
        }
        return Location.create(path, node.getUuid());
    }

    protected MapWorkspace getWorkspace( Request request,
                                         String workspaceName ) {
        // Get the workspace for this request ...
        MapWorkspace workspace = repository.getWorkspace(workspaceName);
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

    protected MapNode getTargetNode( MapWorkspace workspace,
                                     Request request,
                                     Location location ) {
        if (workspace == null) return null;
        // Check first for the UUID ...
        MapNode node = null;
        UUID uuid = location.getUuid();
        if (uuid != null) {
            node = workspace.getNode(uuid);
        }
        Path path = null;
        if (node == null) {
            // Look up the node with the supplied path ...
            path = location.getPath();
            if (path != null) {
                node = workspace.getNode(path);
            }
        }
        if (node == null && request != null) {
            if (path == null) {
                if (uuid == null) {
                    // Missing both path and UUID ...
                    I18n msg = GraphI18n.inMemoryConnectorRequestsMustHavePathOrUuid;
                    request.setError(new IllegalArgumentException(msg.text()));
                    return null;
                }
                // Missing path, and could not find by UUID ...
                request.setError(new PathNotFoundException(location, pathFactory.createRootPath(),
                                                           GraphI18n.nodeDoesNotExist.text(path)));
                return null;
            }
            // Could not find the node given the supplied path, so find the lowest path that does exist ...
            Path lowestExisting = workspace.getLowestExistingPath(path);
            request.setError(new PathNotFoundException(location, lowestExisting, GraphI18n.nodeDoesNotExist.text(path)));
        }
        return node;
    }
}
