package org.modeshape.graph.connector.path;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.Location;
import org.modeshape.graph.NodeConflictBehavior;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
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
import org.modeshape.graph.request.VerifyWorkspaceRequest;
import org.modeshape.graph.request.processor.RequestProcessor;

/**
 * The default implementation of the {@link RequestProcessor} for path repositories.
 */
public class PathRequestProcessor extends RequestProcessor {

    private final PathFactory pathFactory;
    private final PathRepository repository;
    private final boolean updatesAllowed;
    private final PathRepositoryTransaction txn;

    public PathRequestProcessor( ExecutionContext context,
                                 PathRepository repository,
                                 Observer observer,
                                 boolean updatesAllowed,
                                 PathRepositoryTransaction txn ) {
        super(repository.getSourceName(), context, observer);
        this.repository = repository;
        pathFactory = context.getValueFactories().getPathFactory();
        this.updatesAllowed = updatesAllowed;
        this.txn = txn;
    }

    protected boolean updatesAllowed( Request request ) {
        if (!updatesAllowed) {
            request.setError(new InvalidRequestException(GraphI18n.sourceIsReadOnly.text(getSourceName())));
        }
        return !request.hasError();
    }

    @Override
    public void process( VerifyWorkspaceRequest request ) {
        PathWorkspace original = getWorkspace(request, request.workspaceName());
        if (original != null) {
            Path path = getExecutionContext().getValueFactories().getPathFactory().createRootPath();
            request.setActualRootLocation(Location.create(path, repository.getRootNodeUuid()));
            request.setActualWorkspaceName(original.getName());
        }
    }

    @Override
    public void process( GetWorkspacesRequest request ) {
        Set<String> names = repository.getWorkspaceNames();
        request.setAvailableWorkspaceNames(new HashSet<String>(names));
        setCacheableInfo(request);
    }

    @Override
    public void process( CreateWorkspaceRequest request ) {
        // There's a separate flag to allow creating workspaces (which may not require modifying existing data)
        // if (!updatesAllowed(request)) return;

        if (!repository.isWritable()) {
            String msg = GraphI18n.sourceIsReadOnly.text(repository.getSourceName());
            request.setError(new InvalidRequestException(msg));
            return;
        }

        WritablePathRepository writableRepo = (WritablePathRepository)repository;

        PathWorkspace workspace = writableRepo.createWorkspace(getExecutionContext(),
                                                               request.desiredNameOfNewWorkspace(),
                                                               request.conflictBehavior());
        if (workspace == null) {
            String msg = GraphI18n.workspaceAlreadyExistsInRepository.text(request.desiredNameOfNewWorkspace(),
                                                                           repository.getSourceName());
            request.setError(new InvalidWorkspaceException(msg));
        } else {
            request.setActualRootLocation(Location.create(pathFactory.createRootPath(), repository.getRootNodeUuid()));
            request.setActualWorkspaceName(workspace.getName());
            recordChange(request);
        }
    }

    @Override
    public void process( CloneBranchRequest request ) {
        if (!updatesAllowed(request)) return;

        PathWorkspace workspace = getWorkspace(request, request.fromWorkspace());
        PathWorkspace intoWorkspace = getWorkspace(request, request.intoWorkspace());

        if (workspace == null || intoWorkspace == null) return;
        PathNode node = getTargetNode(workspace, request, request.from());
        if (node == null) return;

        if (!(intoWorkspace instanceof WritablePathWorkspace)) {
            I18n msg = GraphI18n.workspaceIsReadOnly;
            request.setError(new InvalidRequestException(msg.text(repository.getSourceName(), intoWorkspace.getName())));
            return;
        }

        WritablePathWorkspace newWorkspace = (WritablePathWorkspace)intoWorkspace;

        // Look up the new parent, which must exist ...
        Path newParentPath = request.into().getPath();
        PathNode newParent = newWorkspace.getNode(newParentPath);
        Set<Location> removedExistingNodes = new HashSet<Location>();
        Name desiredName = request.desiredName();
        PathNode newNode = newWorkspace.copyNode(getExecutionContext(), node, workspace, newParent, desiredName, true);

        Location oldLocation = Location.create(node.getPath(), node.getUuid());
        Location newLocation = Location.create(newNode.getPath(), newNode.getUuid());
        request.setActualLocations(oldLocation, newLocation);
        request.setRemovedNodes(Collections.unmodifiableSet(removedExistingNodes));
        recordChange(request);
    }

    @Override
    public void process( CloneWorkspaceRequest request ) {
        if (!updatesAllowed(request)) return;

        // Find the original workspace that we're cloning ...
        final ExecutionContext context = getExecutionContext();
        String targetWorkspaceName = request.desiredNameOfTargetWorkspace();
        String nameOfWorkspaceToBeCloned = request.nameOfWorkspaceToBeCloned();
        PathWorkspace original = repository.getWorkspace(nameOfWorkspaceToBeCloned);
        PathWorkspace target = repository.getWorkspace(targetWorkspaceName);

        if (!repository.isWritable()) {
            String msg = GraphI18n.sourceIsReadOnly.text(repository.getSourceName());
            request.setError(new InvalidRequestException(msg));
            return;
        }

        WritablePathRepository writableRepo = (WritablePathRepository)repository;

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
                    target = writableRepo.createWorkspace(context, targetWorkspaceName, request.targetConflictBehavior());
                    assert target != null;

                    request.setActualRootLocation(Location.create(pathFactory.createRootPath(), writableRepo.getRootNodeUuid()));
                    request.setActualWorkspaceName(target.getName());
                    return;
            }
        }
        assert original != null;
        target = writableRepo.createWorkspace(context,
                                              targetWorkspaceName,
                                              request.targetConflictBehavior(),
                                              nameOfWorkspaceToBeCloned);
        assert target != null;

        request.setActualRootLocation(Location.create(pathFactory.createRootPath(), writableRepo.getRootNodeUuid()));
        request.setActualWorkspaceName(target.getName());
        recordChange(request);
    }

    @Override
    public void process( DestroyWorkspaceRequest request ) {
        if (!updatesAllowed(request)) return;

        PathWorkspace workspace = repository.getWorkspace(request.workspaceName());
        if (workspace != null) {
            request.setActualRootLocation(Location.create(pathFactory.createRootPath(), repository.getRootNodeUuid()));
            recordChange(request);
        } else {
            String msg = GraphI18n.workspaceDoesNotExistInRepository.text(request.workspaceName(), repository.getSourceName());
            request.setError(new InvalidWorkspaceException(msg));
        }
    }

    @Override
    public void process( CopyBranchRequest request ) {
        if (!updatesAllowed(request)) return;

        PathWorkspace workspace = getWorkspace(request, request.fromWorkspace());
        PathWorkspace intoWorkspace = getWorkspace(request, request.intoWorkspace());
        if (workspace == null || intoWorkspace == null) return;
        PathNode node = getTargetNode(workspace, request, request.from());
        if (node == null) return;

        if (!(intoWorkspace instanceof WritablePathWorkspace)) {
            I18n msg = GraphI18n.workspaceIsReadOnly;
            request.setError(new InvalidRequestException(msg.text(repository.getSourceName(), intoWorkspace.getName())));
            return;
        }

        WritablePathWorkspace newWorkspace = (WritablePathWorkspace)intoWorkspace;

        // Look up the new parent, which must exist ...
        Path newParentPath = request.into().getPath();
        Name desiredName = request.desiredName();
        PathNode newParent = newWorkspace.getNode(newParentPath);
        PathNode newNode = newWorkspace.copyNode(getExecutionContext(), node, workspace, newParent, desiredName, true);
        Location oldLocation = Location.create(node.getPath(), node.getUuid());
        Location newLocation = Location.create(newNode.getPath(), newNode.getUuid());
        request.setActualLocations(oldLocation, newLocation);
        recordChange(request);
    }

    @Override
    public void process( CreateNodeRequest request ) {
        if (!updatesAllowed(request)) return;

        PathWorkspace workspace = getWorkspace(request, request.inWorkspace());
        if (workspace == null) return;
        Path parent = request.under().getPath();
        CheckArg.isNotNull(parent, "request.under().getPath()");
        PathNode node = null;
        // Look up the parent node, which must exist ...

        PathNode parentNode = workspace.getNode(parent);
        if (parentNode == null) {
            Path lowestExisting = workspace.getLowestExistingPath(parent);
            request.setError(new PathNotFoundException(request.under(), lowestExisting, GraphI18n.nodeDoesNotExist.text(parent)));
            return;
        }

        if (!(workspace instanceof WritablePathWorkspace)) {
            I18n msg = GraphI18n.workspaceIsReadOnly;
            request.setError(new InvalidRequestException(msg.text(repository.getSourceName(), workspace.getName())));
            return;
        }

        WritablePathWorkspace newWorkspace = (WritablePathWorkspace)workspace;

        // Make a list of the properties that we will store: all props except dna:uuid and jcr:uuid
        Map<Name, Property> propsToStore = new HashMap<Name, Property>(request.properties().size());
        for (Property property : request.properties()) {
            if (property.size() > 0) propsToStore.put(property.getName(), property);
        }

        NodeConflictBehavior conflictBehavior = request.conflictBehavior();
        switch (conflictBehavior) {
            case APPEND:
                node = newWorkspace.createNode(getExecutionContext(), parentNode, request.named(), propsToStore, conflictBehavior);
                break;
            case DO_NOT_REPLACE:
                for (Segment childSegment : parentNode.getChildSegments()) {
                    if (request.named().equals(childSegment.getName())) {
                        Path childPath = pathFactory.create(parent, childSegment);
                        node = newWorkspace.getNode(childPath);
                        break;
                    }
                }
                if (node == null) {
                    node = newWorkspace.createNode(getExecutionContext(),
                                                   parentNode,
                                                   request.named(),
                                                   propsToStore,
                                                   conflictBehavior);
                }
                break;
            case REPLACE:
                // See if the node already exists (this doesn't record an error on the request) ...
                node = workspace.getNode(pathFactory.create(parent, request.named()));
                if (node != null) {
                    newWorkspace.removeNode(getExecutionContext(), node.getPath());
                }
                node = newWorkspace.createNode(getExecutionContext(), parentNode, request.named(), propsToStore, conflictBehavior);
                break;
            case UPDATE:
                // See if the node already exists (this doesn't record an error on the request) ...
                node = newWorkspace.getNode(pathFactory.create(parent, request.named()));
                if (node == null) {
                    node = newWorkspace.createNode(getExecutionContext(),
                                                   parentNode,
                                                   request.named(),
                                                   propsToStore,
                                                   conflictBehavior);
                } // otherwise, we found it and we're setting any properties below
                break;
        }
        assert node != null;

        Location actualLocation = Location.create(node.getPath(), node.getUuid());
        request.setActualLocationOfNode(actualLocation);
        recordChange(request);

    }

    @Override
    public void process( DeleteBranchRequest request ) {
        if (!updatesAllowed(request)) return;

        PathWorkspace workspace = getWorkspace(request, request.inWorkspace());
        if (workspace == null) return;
        PathNode node = getTargetNode(workspace, request, request.at());
        if (node == null) return;

        if (!(workspace instanceof WritablePathWorkspace)) {
            I18n msg = GraphI18n.workspaceIsReadOnly;
            request.setError(new InvalidRequestException(msg.text(repository.getSourceName(), workspace.getName())));
            return;
        }

        WritablePathWorkspace newWorkspace = (WritablePathWorkspace)workspace;
        newWorkspace.removeNode(getExecutionContext(), node.getPath());

        request.setActualLocationOfNode(Location.create(node.getPath(), node.getUuid()));
        recordChange(request);
    }

    @Override
    public void process( MoveBranchRequest request ) {
        if (!updatesAllowed(request)) return;

        PathWorkspace workspace = getWorkspace(request, request.inWorkspace());
        if (workspace == null) return;

        PathNode beforeNode = request.before() != null ? getTargetNode(workspace, request, request.before()) : null;
        PathNode node = getTargetNode(workspace, request, request.from());
        if (node == null) return;
        if (request.hasError()) return; // if beforeNode could not be found
        // Look up the new parent, which must exist ...
        Path newParentPath;

        if (request.into() != null) {
            newParentPath = request.into().getPath();
        } else {
            // into or before cannot both be null
            assert beforeNode != null;
            newParentPath = beforeNode.getPath().getParent();
        }

        PathNode newParent = workspace.getNode(newParentPath);
        if (newParent == null) {
            Path lowestExisting = workspace.getLowestExistingPath(newParentPath);
            request.setError(new PathNotFoundException(request.into(), lowestExisting,
                                                       GraphI18n.nodeDoesNotExist.text(newParentPath)));
            return;
        }

        if (!(workspace instanceof WritablePathWorkspace)) {
            I18n msg = GraphI18n.workspaceIsReadOnly;
            request.setError(new InvalidRequestException(msg.text(repository.getSourceName(), workspace.getName())));
            return;
        }

        WritablePathWorkspace newWorkspace = (WritablePathWorkspace)workspace;

        node = newWorkspace.moveNode(getExecutionContext(), node, request.desiredName(), newWorkspace, newParent, beforeNode);
        assert node.getPath().getParent().equals(newParent.getPath());

        Location oldLocation = Location.create(request.from().getPath());
        Location newLocation = Location.create(node.getPath(), node.getUuid());
        request.setActualLocations(oldLocation, newLocation);
        recordChange(request);

    }

    @Override
    public void process( ReadNodeRequest request ) {
        PathWorkspace workspace = getWorkspace(request, request.inWorkspace());
        if (workspace == null) return;

        PathNode node = getTargetNode(workspace, request, request.at());
        if (node == null) {
            request.setError(new PathNotFoundException(request.at(), workspace.getLowestExistingPath(request.at().getPath())));
            return;
        }

        // Get the names of the children ...
        for (Path.Segment childSegment : node.getChildSegments()) {
            request.addChild(Location.create(pathFactory.create(node.getPath(), childSegment)));
        }

        // Get the properties of the node ...
        request.addProperties(node.getProperties().values());

        request.setActualLocationOfNode(Location.create(node.getPath(), node.getUuid()));
        setCacheableInfo(request);
    }

    @Override
    public void process( ReadAllChildrenRequest request ) {
        PathWorkspace workspace = getWorkspace(request, request.inWorkspace());
        if (workspace == null) return;

        PathNode node = getTargetNode(workspace, request, request.of());
        if (node == null) {
            request.setError(new PathNotFoundException(request.of(), workspace.getLowestExistingPath(request.of().getPath())));
            return;
        }

        List<Path.Segment> childSegments = node.getChildSegments();
        for (Path.Segment childSegment : childSegments) {
            request.addChild(Location.create(pathFactory.create(node.getPath(), childSegment)));
        }
        request.setActualLocationOfNode(Location.create(node.getPath(), node.getUuid()));
        setCacheableInfo(request);
    }

    @Override
    public void process( ReadAllPropertiesRequest request ) {
        PathWorkspace workspace = getWorkspace(request, request.inWorkspace());
        if (workspace == null) return;

        PathNode node = getTargetNode(workspace, request, request.at());
        if (node == null) {
            request.setError(new PathNotFoundException(request.at(), workspace.getLowestExistingPath(request.at().getPath())));
            return;
        }

        // Get the properties of the node ...
        request.addProperties(node.getProperties().values());
        request.setActualLocationOfNode(Location.create(node.getPath(), node.getUuid()));
        setCacheableInfo(request);
    }

    @Override
    public void process( AccessQueryRequest request ) {
        PathWorkspace workspace = getWorkspace(request, request.workspace());
        if (workspace == null) return;
        final ExecutionContext context = getExecutionContext();
        QueryResults results = workspace.query(context, request);
        if (results != null) {
            request.setResults(results.getTuples(), results.getStatistics());
        } else {
            super.processUnknownRequest(request);
        }
    }

    @Override
    public void process( FullTextSearchRequest request ) {
        PathWorkspace workspace = getWorkspace(request, request.workspace());
        if (workspace == null) return;
        final ExecutionContext context = getExecutionContext();
        QueryResults results = workspace.search(context, request.expression());
        if (results != null) {
            request.setResults(results.getColumns(), results.getTuples(), results.getStatistics());
        } else {
            super.processUnknownRequest(request);
        }
    }

    @Override
    public void process( UpdatePropertiesRequest request ) {
        if (!updatesAllowed(request)) return;

        PathWorkspace workspace = getWorkspace(request, request.inWorkspace());
        PathNode node = getTargetNode(workspace, request, request.on());
        if (node == null) return;

        if (!(workspace instanceof WritablePathWorkspace)) {
            I18n msg = GraphI18n.workspaceIsReadOnly;
            request.setError(new InvalidRequestException(msg.text(repository.getSourceName(), workspace.getName())));
            return;
        }

        WritablePathWorkspace newWorkspace = (WritablePathWorkspace)workspace;

        // Now set (or remove) the properties to the supplied node ...
        newWorkspace.setProperties(getExecutionContext(), node.getPath(), request.properties());

        request.setActualLocationOfNode(Location.create(node.getPath(), node.getUuid()));
        recordChange(request);
    }

    @Override
    public void process( LockBranchRequest request ) {
        PathWorkspace workspace = getWorkspace(request, request.inWorkspace());
        PathNode node = getTargetNode(workspace, request, request.at());
        if (node == null) return;

        workspace.lockNode(node, request.lockScope(), request.lockTimeoutInMillis());

        request.setActualLocation(Location.create(node.getPath(), node.getUuid()));
        recordChange(request);
    }

    @Override
    public void process( UnlockBranchRequest request ) {
        PathWorkspace workspace = getWorkspace(request, request.inWorkspace());
        PathNode node = getTargetNode(workspace, request, request.at());
        if (node == null) return;

        workspace.unlockNode(node);

        request.setActualLocation(Location.create(node.getPath(), node.getUuid()));
        recordChange(request);
    }

    protected PathWorkspace getWorkspace( Request request,
                                          String workspaceName ) {
        // Get the workspace for this request ...
        PathWorkspace workspace = repository.getWorkspace(workspaceName);
        if (workspace == null) {
            String msg = GraphI18n.workspaceDoesNotExistInRepository.text(workspaceName, repository.getSourceName());
            request.setError(new InvalidWorkspaceException(msg));
        }
        return workspace;
    }

    protected PathNode getTargetNode( PathWorkspace workspace,
                                      Request request,
                                      Location location ) {
        if (workspace == null) return null;
        PathNode node = null;

        if (location.getUuid() != null) {
            if (repository.getRootNodeUuid().equals(location.getUuid())) {
                PathFactory pathFactory = new ExecutionContext().getValueFactories().getPathFactory();
                return workspace.getNode(pathFactory.createRootPath());
            }
        }

        if (!location.hasPath()) {
            I18n msg = GraphI18n.pathConnectorRequestsMustHavePath;
            request.setError(new IllegalArgumentException(msg.text()));
            return null;
        }

        // Look up the node with the supplied path ...
        Path path = location.getPath();
        if (path != null) {
            node = workspace.getNode(path);
        }

        if (node == null && request != null) {
            if (path == null) {
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

    /**
     * Returns the transaction associated with this request processor. This transaction must eventually either be
     * {@link PathRepositoryTransaction#commit() committed} or {@link PathRepositoryTransaction#rollback() rolled back}.
     * 
     * @return the transaction associated with this request processor; never null
     * @see PathRepositoryTransaction#commit()
     * @see PathRepositoryTransaction#rollback()
     */
    public PathRepositoryTransaction getTransaction() {
        return this.txn;
    }
}
