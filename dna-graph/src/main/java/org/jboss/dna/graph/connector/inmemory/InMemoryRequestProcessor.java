/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * Unless otherwise indicated, all code in JBoss DNA is licensed
 * to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.graph.connector.inmemory;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.JcrLexicon;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.RepositoryContext;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.graph.property.Path.Segment;
import org.jboss.dna.graph.request.CloneWorkspaceRequest;
import org.jboss.dna.graph.request.CopyBranchRequest;
import org.jboss.dna.graph.request.CreateNodeRequest;
import org.jboss.dna.graph.request.CreateWorkspaceRequest;
import org.jboss.dna.graph.request.DeleteBranchRequest;
import org.jboss.dna.graph.request.DestroyWorkspaceRequest;
import org.jboss.dna.graph.request.GetWorkspacesRequest;
import org.jboss.dna.graph.request.InvalidWorkspaceException;
import org.jboss.dna.graph.request.MoveBranchRequest;
import org.jboss.dna.graph.request.ReadAllChildrenRequest;
import org.jboss.dna.graph.request.ReadAllPropertiesRequest;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.UpdatePropertiesRequest;
import org.jboss.dna.graph.request.VerifyWorkspaceRequest;
import org.jboss.dna.graph.request.processor.RequestProcessor;

/**
 * A {@link RequestProcessor} implementation that operates on an {@link InMemoryRepository} and its
 * {@link InMemoryRepository.Workspace workspaces}.
 */
public class InMemoryRequestProcessor extends RequestProcessor {
    private final PathFactory pathFactory;
    private final PropertyFactory propertyFactory;
    private final InMemoryRepository repository;

    protected InMemoryRequestProcessor( ExecutionContext context,
                                        InMemoryRepository repository,
                                        RepositoryContext repositoryContext ) {
        super(repository.getSourceName(), context, repositoryContext != null ? repositoryContext.getObserver() : null);
        this.repository = repository;
        pathFactory = context.getValueFactories().getPathFactory();
        propertyFactory = context.getPropertyFactory();
    }

    @Override
    public void process( ReadAllChildrenRequest request ) {
        InMemoryRepository.Workspace workspace = getWorkspace(request, request.inWorkspace());
        InMemoryNode node = getTargetNode(workspace, request, request.of());
        if (node == null) return;
        Location actualLocation = getActualLocation(request.of().getPath(), node);
        Path path = actualLocation.getPath();
        // Get the names of the children ...
        List<InMemoryNode> children = node.getChildren();
        for (InMemoryNode child : children) {
            Segment childName = child.getName();
            Path childPath = pathFactory.create(path, childName);
            request.addChild(childPath, propertyFactory.create(DnaLexicon.UUID, child.getUuid()));
        }
        request.setActualLocationOfNode(actualLocation);
        setCacheableInfo(request);
    }

    @Override
    public void process( ReadAllPropertiesRequest request ) {
        InMemoryRepository.Workspace workspace = getWorkspace(request, request.inWorkspace());
        InMemoryNode node = getTargetNode(workspace, request, request.at());
        if (node == null) return;
        // Get the properties of the node ...
        Location actualLocation = getActualLocation(request.at().getPath(), node);
        request.addProperty(propertyFactory.create(DnaLexicon.UUID, node.getUuid()));
        for (Property property : node.getProperties().values()) {
            request.addProperty(property);
        }
        request.setActualLocationOfNode(actualLocation);
        setCacheableInfo(request);
    }

    @Override
    public void process( CopyBranchRequest request ) {
        InMemoryRepository.Workspace workspace = getWorkspace(request, request.fromWorkspace());
        InMemoryRepository.Workspace newWorkspace = getWorkspace(request, request.intoWorkspace());
        if (workspace == null || newWorkspace == null) return;
        InMemoryNode node = getTargetNode(workspace, request, request.from());
        if (node == null) return;
        // Look up the new parent, which must exist ...
        Path newParentPath = request.into().getPath();
        Name desiredName = request.desiredName();
        InMemoryNode newParent = newWorkspace.getNode(newParentPath);
        Map<UUID, UUID> copyMap = workspace.equals(newWorkspace) ? new HashMap<UUID, UUID>() : null;
        InMemoryNode newNode = workspace.copyNode(getExecutionContext(),
                                                  node,
                                                  newWorkspace,
                                                  newParent,
                                                  desiredName,
                                                  true,
                                                  copyMap);
        Path newPath = getExecutionContext().getValueFactories().getPathFactory().create(newParentPath, newNode.getName());
        Location oldLocation = getActualLocation(request.from().getPath(), node);
        Location newLocation = Location.create(newPath, newNode.getUuid());
        request.setActualLocations(oldLocation, newLocation);
        recordChange(request);
    }

    @Override
    public void process( CreateNodeRequest request ) {
        InMemoryRepository.Workspace workspace = getWorkspace(request, request.inWorkspace());
        if (workspace == null) return;
        Path parent = request.under().getPath();
        CheckArg.isNotNull(parent, "request.under().getPath()");
        InMemoryNode node = null;
        // Look up the parent node, which must exist ...
        // System.err.println(request.toString());
        InMemoryNode parentNode = workspace.getNode(parent);
        if (parentNode == null) {
            Path lowestExisting = workspace.getLowestExistingPath(parent);
            request.setError(new PathNotFoundException(request.under(), lowestExisting,
                                                       GraphI18n.inMemoryNodeDoesNotExist.text(parent)));
            return;
        }
        UUID uuid = null;
        for (Property property : request.properties()) {
            if (property.getName().equals(DnaLexicon.UUID) || property.getName().equals(JcrLexicon.UUID)) {
                uuid = getExecutionContext().getValueFactories().getUuidFactory().create(property.getValues().next());
                break;
            }
        }
        switch (request.conflictBehavior()) {
            case APPEND:
            case DO_NOT_REPLACE:
                node = workspace.createNode(getExecutionContext(), parentNode, request.named(), uuid);
                break;
            case REPLACE:
                // See if the node already exists (this doesn't record an error on the request) ...
                node = getTargetNode(workspace, null, Location.create(pathFactory.create(parent, request.named()), uuid));
                if (node != null) {
                    workspace.removeNode(getExecutionContext(), node);
                }
                node = workspace.createNode(getExecutionContext(), parentNode, request.named(), uuid);
                break;
            case UPDATE:
                // See if the node already exists (this doesn't record an error on the request) ...
                node = getTargetNode(workspace, null, Location.create(pathFactory.create(parent, request.named()), uuid));
                if (node == null) {
                    node = workspace.createNode(getExecutionContext(), parentNode, request.named(), uuid);
                } // otherwise, we found it and we're setting any properties below
                break;
        }
        assert node != null;
        Path path = getExecutionContext().getValueFactories().getPathFactory().create(parent, node.getName());
        // Now add the properties to the supplied node ...
        for (Property property : request.properties()) {
            Name propName = property.getName();
            if (property.size() == 0) {
                node.getProperties().remove(propName);
                continue;
            }
            if (!propName.equals(DnaLexicon.UUID)) {
                node.getProperties().put(propName, property);
            }
        }
        Location actualLocation = getActualLocation(path, node);
        request.setActualLocationOfNode(actualLocation);
        recordChange(request);
    }

    @Override
    public void process( DeleteBranchRequest request ) {
        InMemoryRepository.Workspace workspace = getWorkspace(request, request.inWorkspace());
        if (workspace == null) return;
        InMemoryNode node = getTargetNode(workspace, request, request.at());
        if (node == null) return;
        workspace.removeNode(getExecutionContext(), node);
        Location actualLocation = getActualLocation(request.at().getPath(), node);
        request.setActualLocationOfNode(actualLocation);
        recordChange(request);
    }

    @Override
    public void process( MoveBranchRequest request ) {
        InMemoryRepository.Workspace workspace = getWorkspace(request, request.inWorkspace());
        if (workspace == null) return;

        InMemoryNode beforeNode = request.before() != null ? getTargetNode(workspace, request, request.before()) : null;
        InMemoryNode node = getTargetNode(workspace, request, request.from());
        if (node == null) return;
        // Look up the new parent, which must exist ...
        Path newParentPath;
        
        if (request.into() != null) {
            newParentPath = request.into().getPath();
        }
        else {
            // into or before cannot both be null
            assert beforeNode != null;
            
            // Build the path from the before node to the root.
            LinkedList<Path.Segment> segments = new LinkedList<Path.Segment>();
            InMemoryNode current = beforeNode.getParent();
            while (current != workspace.getRoot()) {
                segments.addFirst(current.getName());
                current = current.getParent();
            } 
            newParentPath = getExecutionContext().getValueFactories().getPathFactory().createAbsolutePath(segments);
        }
        
        InMemoryNode newParent = workspace.getNode(newParentPath);
        workspace.moveNode(getExecutionContext(), node, request.desiredName(), workspace, newParent, beforeNode);
        assert node.getParent() == newParent;
        Path newPath = getExecutionContext().getValueFactories().getPathFactory().create(newParentPath, node.getName());
        Location oldLocation = getActualLocation(request.from().getPath(), node);
        Location newLocation = Location.create(newPath, node.getUuid());
        request.setActualLocations(oldLocation, newLocation);
        recordChange(request);
    }

    @Override
    public void process( UpdatePropertiesRequest request ) {
        InMemoryRepository.Workspace workspace = getWorkspace(request, request.inWorkspace());
        InMemoryNode node = getTargetNode(workspace, request, request.on());
        if (node == null) return;
        // Now set (or remove) the properties to the supplied node ...
        for (Map.Entry<Name, Property> propertyEntry : request.properties().entrySet()) {
            Property property = propertyEntry.getValue();
            if (property == null) {
                node.getProperties().remove(propertyEntry.getKey());
                continue;
            }
            Name propName = property.getName();
            if (!propName.equals(DnaLexicon.UUID)) {
                node.getProperties().put(propName, property);
            }
        }
        Location actualLocation = getActualLocation(request.on().getPath(), node);
        request.setActualLocationOfNode(actualLocation);
        recordChange(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CreateWorkspaceRequest)
     */
    @Override
    public void process( CreateWorkspaceRequest request ) {
        InMemoryRepository.Workspace workspace = repository.createWorkspace(getExecutionContext(),
                                                                            request.desiredNameOfNewWorkspace(),
                                                                            request.conflictBehavior());
        if (workspace == null) {
            String msg = GraphI18n.workspaceAlreadyExistsInRepository.text(request.desiredNameOfNewWorkspace(),
                                                                           repository.getSourceName());
            request.setError(new InvalidWorkspaceException(msg));
        } else {
            InMemoryNode root = workspace.getRoot();
            request.setActualRootLocation(Location.create(pathFactory.createRootPath(), root.getUuid()));
            request.setActualWorkspaceName(workspace.getName());
            recordChange(request);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.DestroyWorkspaceRequest)
     */
    @Override
    public void process( DestroyWorkspaceRequest request ) {
        InMemoryRepository.Workspace workspace = repository.getWorkspace(getExecutionContext(), request.workspaceName());
        if (workspace != null) {
            InMemoryNode root = workspace.getRoot();
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.GetWorkspacesRequest)
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
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.VerifyWorkspaceRequest)
     */
    @Override
    public void process( VerifyWorkspaceRequest request ) {
        InMemoryRepository.Workspace original = getWorkspace(request, request.workspaceName());
        if (original != null) {
            Path path = getExecutionContext().getValueFactories().getPathFactory().createRootPath();
            request.setActualRootLocation(Location.create(path, original.getRoot().getUuid()));
            request.setActualWorkspaceName(original.getName());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CloneWorkspaceRequest)
     */
    @Override
    public void process( CloneWorkspaceRequest request ) {
        // Find the original workspace that we're cloning ...
        final ExecutionContext context = getExecutionContext();
        String targetWorkspaceName = request.desiredNameOfTargetWorkspace();
        String nameOfWorkspaceToBeCloned = request.nameOfWorkspaceToBeCloned();
        InMemoryRepository.Workspace original = repository.getWorkspace(context, nameOfWorkspaceToBeCloned);
        InMemoryRepository.Workspace target = repository.getWorkspace(context, targetWorkspaceName);
        if (original == null) {
            switch (request.cloneConflictBehavior()) {
                case DO_NOT_CLONE:
                    String msg = GraphI18n.workspaceDoesNotExistInRepository.text(nameOfWorkspaceToBeCloned,
                                                                                  repository.getSourceName());
                    request.setError(new InvalidWorkspaceException(msg));
                    return;
                case SKIP_CLONE:
                    target = repository.createWorkspace(context, targetWorkspaceName, request.targetConflictBehavior());
                    if (target == null) {
                        msg = GraphI18n.workspaceAlreadyExistsInRepository.text(targetWorkspaceName, repository.getSourceName());
                        request.setError(new InvalidWorkspaceException(msg));
                    } else {
                        InMemoryNode root = target.getRoot();
                        request.setActualRootLocation(Location.create(pathFactory.createRootPath(), root.getUuid()));
                        request.setActualWorkspaceName(target.getName());
                    }
                    return;
            }
        }
        assert original != null;
        target = repository.createWorkspace(context,
                                            targetWorkspaceName,
                                            request.targetConflictBehavior(),
                                            nameOfWorkspaceToBeCloned);
        if (target == null) {
            // Since the original was there, the only reason the target wasn't created was because the workspace already existed
            // ...
            String msg = GraphI18n.workspaceAlreadyExistsInRepository.text(targetWorkspaceName, repository.getSourceName());
            request.setError(new InvalidWorkspaceException(msg));
        } else {
            InMemoryNode root = target.getRoot();
            request.setActualRootLocation(Location.create(pathFactory.createRootPath(), root.getUuid()));
            request.setActualWorkspaceName(target.getName());
            recordChange(request);
        }
    }

    protected Location getActualLocation( Path path,
                                          InMemoryNode node ) {
        if (path == null) {
            // Find the path on the node ...
            LinkedList<Path.Segment> segments = new LinkedList<Path.Segment>();
            InMemoryNode n = node;
            while (n != null) {
                if (n.getParent() == null) break;
                segments.addFirst(n.getName());
                n = n.getParent();
            }
            path = pathFactory.createAbsolutePath(segments);
        }
        return Location.create(path, node.getUuid());
    }

    protected InMemoryRepository.Workspace getWorkspace( Request request,
                                                         String workspaceName ) {
        // Get the workspace for this request ...
        InMemoryRepository.Workspace workspace = repository.getWorkspace(getExecutionContext(), workspaceName);
        if (workspace == null) {
            String msg = GraphI18n.workspaceDoesNotExistInRepository.text(workspaceName, repository.getSourceName());
            request.setError(new InvalidWorkspaceException(msg));
        }
        return workspace;
    }

    protected InMemoryNode getTargetNode( InMemoryRepository.Workspace workspace,
                                          Request request,
                                          Location location ) {
        if (workspace == null) return null;
        // Check first for the UUID ...
        InMemoryNode node = null;
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
                                                           GraphI18n.inMemoryNodeDoesNotExist.text(path)));
                return null;
            }
            // Could not find the node given the supplied path, so find the lowest path that does exist ...
            Path lowestExisting = workspace.getLowestExistingPath(path);
            request.setError(new PathNotFoundException(location, lowestExisting, GraphI18n.inMemoryNodeDoesNotExist.text(path)));
        }
        return node;
    }
}
