package org.jboss.dna.graph.connector.path;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.observe.Observer;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.request.CloneBranchRequest;
import org.jboss.dna.graph.request.CloneWorkspaceRequest;
import org.jboss.dna.graph.request.CopyBranchRequest;
import org.jboss.dna.graph.request.CreateNodeRequest;
import org.jboss.dna.graph.request.CreateWorkspaceRequest;
import org.jboss.dna.graph.request.DeleteBranchRequest;
import org.jboss.dna.graph.request.DestroyWorkspaceRequest;
import org.jboss.dna.graph.request.GetWorkspacesRequest;
import org.jboss.dna.graph.request.InvalidRequestException;
import org.jboss.dna.graph.request.InvalidWorkspaceException;
import org.jboss.dna.graph.request.MoveBranchRequest;
import org.jboss.dna.graph.request.ReadAllChildrenRequest;
import org.jboss.dna.graph.request.ReadAllPropertiesRequest;
import org.jboss.dna.graph.request.ReadNodeRequest;
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.UpdatePropertiesRequest;
import org.jboss.dna.graph.request.VerifyWorkspaceRequest;
import org.jboss.dna.graph.request.processor.RequestProcessor;

/**
 * The default implementation of the {@link RequestProcessor} for path repositories.
 */
public class PathRequestProcessor extends RequestProcessor {

    private final PathFactory pathFactory;
    private final PathRepository repository;

    public PathRequestProcessor( ExecutionContext context,
                                 PathRepository repository,
                                 Observer observer ) {
        super(repository.getSourceName(), context, observer);
        this.repository = repository;
        pathFactory = context.getValueFactories().getPathFactory();
    }

    private void updatesNotSupported( Request request ) {
        request.setError(new InvalidRequestException(GraphI18n.unsupportedRequestType.text(request.getClass().getName(), request)));
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
        updatesNotSupported(request);
    }

    @Override
    public void process( CloneBranchRequest request ) {
        updatesNotSupported(request);
    }

    @Override
    public void process( CloneWorkspaceRequest request ) {
        updatesNotSupported(request);
    }

    @Override
    public void process( DestroyWorkspaceRequest request ) {
        updatesNotSupported(request);
    }

    @Override
    public void process( CopyBranchRequest request ) {
        updatesNotSupported(request);
    }

    @Override
    public void process( CreateNodeRequest request ) {
        updatesNotSupported(request);
    }

    @Override
    public void process( DeleteBranchRequest request ) {
        updatesNotSupported(request);
    }

    @Override
    public void process( MoveBranchRequest request ) {
        updatesNotSupported(request);
    }

    @Override
    public void process( ReadNodeRequest request ) {
        PathWorkspace workspace = getWorkspace(request, request.inWorkspace());
        PathNode node = getTargetNode(workspace, request, request.at());
        if (node == null) return;

        // Get the names of the children ...
        for (Path.Segment childSegment : node.getChildSegments()) {
            request.addChild(Location.create(pathFactory.create(node.getPath(), childSegment)));
        }

        // Get the properties of the node ...
        request.addProperties(node.getProperties().values());

        request.setActualLocationOfNode(Location.create(node.getPath()));
        setCacheableInfo(request);
    }

    @Override
    public void process( ReadAllChildrenRequest request ) {
        PathWorkspace workspace = getWorkspace(request, request.inWorkspace());
        PathNode node = getTargetNode(workspace, request, request.of());
        if (node == null) return;

        List<Path.Segment> childSegments = node.getChildSegments();
        for (Path.Segment childSegment : childSegments) {
            request.addChild(Location.create(pathFactory.create(node.getPath(), childSegment)));
        }
        request.setActualLocationOfNode(Location.create(node.getPath()));
        setCacheableInfo(request);
    }

    @Override
    public void process( ReadAllPropertiesRequest request ) {
        PathWorkspace workspace = getWorkspace(request, request.inWorkspace());
        PathNode node = getTargetNode(workspace, request, request.at());
        if (node == null) return;

        // Get the properties of the node ...
        request.addProperties(node.getProperties().values());
        request.setActualLocationOfNode(Location.create(node.getPath()));
        setCacheableInfo(request);
    }

    @Override
    public void process( UpdatePropertiesRequest request ) {
        updatesNotSupported(request);
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

}
