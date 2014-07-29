package org.modeshape.jcr.federation;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.jcr.Connectors;
import org.modeshape.jcr.Connectors.PathMappings;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.bus.ChangeBus;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.change.RecordingChanges;
import org.modeshape.jcr.federation.spi.ConnectorChangeSet;
import org.modeshape.jcr.value.DateTimeFactory;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.WorkspaceAndPath;

@NotThreadSafe
public class ConnectorChangeSetImpl implements ConnectorChangeSet {

    private final Connectors connectors;
    private final String connectorSourceName;
    private final Connectors.PathMappings pathMappings;
    private final String processId;
    private final String repositoryKey;
    private final ChangeBus bus;
    private final Map<String, RecordingChanges> changesByWorkspace = new HashMap<String, RecordingChanges>();
    private final DateTimeFactory timeFactory;
    private final String sessionId;

    public ConnectorChangeSetImpl( Connectors connectors,
                                   PathMappings mappings,
                                   String sessionId,
                                   String processId,
                                   String repositoryKey,
                                   ChangeBus bus,
                                   DateTimeFactory timeFactory ) {
        this.connectors = connectors;
        this.connectorSourceName = mappings.getConnectorSourceName();
        this.timeFactory = timeFactory;
        this.pathMappings = mappings;
        this.processId = processId;
        this.repositoryKey = repositoryKey;
        this.bus = bus;
        this.sessionId = sessionId;
        assert this.connectors != null;
        assert this.connectorSourceName != null;
        assert this.pathMappings != null;
        assert this.processId != null;
        assert this.repositoryKey != null;
        assert this.bus != null;
        assert this.timeFactory != null;
    }

    protected final RecordingChanges changesFor( WorkspaceAndPath workspaceAndPath ) {
        return changesFor(workspaceAndPath.getWorkspaceName());
    }

    protected final RecordingChanges changesFor( String workspaceName ) {
        RecordingChanges changes = changesByWorkspace.get(workspaceName);
        if (changes == null) {
            changes = new RecordingChanges(sessionId, processId, repositoryKey, workspaceName);
            changesByWorkspace.put(workspaceName, changes);
        }
        return changes;
    }

    @Override
    public void nodeCreated( String docId,
                             String parentDocId,
                             String path,
                             Map<Name, Property> properties ) {
        NodeKey key = nodeKey(docId);
        NodeKey parentKey = nodeKey(parentDocId);
        Path externalPath = pathMappings.getPathFactory().create(path);
        // This external path in the connector may be projected into *multiple* nodes in the same or different workspaces ...
        for (WorkspaceAndPath wsAndPath : pathMappings.resolveExternalPathToInternal(externalPath)) {
            changesFor(wsAndPath).nodeCreated(key, parentKey, wsAndPath.getPath(), properties);
        }
    }

    @Override
    public void nodeRemoved( String docId,
                             String parentDocId,
                             String path ) {
        NodeKey key = nodeKey(docId);
        NodeKey parentKey = nodeKey(parentDocId);
        Path externalPath = pathMappings.getPathFactory().create(path);
        // This external path in the connector may be projected into *multiple* nodes in the same or different workspaces ...
        for (WorkspaceAndPath wsAndPath : pathMappings.resolveExternalPathToInternal(externalPath)) {
            changesFor(wsAndPath).nodeRemoved(key, parentKey, wsAndPath.getPath());
        }
        // Signal to the manager of the Connector instances that an external node was removed. If this external
        // node is used in a projection, that projection will be removed...
        connectors.externalNodeRemoved(docId);
    }

    @Override
    public void nodeMoved( String docId,
                           String newParentDocId,
                           String oldParentDocId,
                           String newPath,
                           String oldPath ) {
        NodeKey key = nodeKey(docId);
        NodeKey newParentKey = nodeKey(newParentDocId);
        NodeKey oldParentKey = nodeKey(oldParentDocId);
        Path newExternalPath = pathMappings.getPathFactory().create(newPath);
        Path oldExternalPath = pathMappings.getPathFactory().create(oldPath);
        Collection<WorkspaceAndPath> newWsAndPaths = pathMappings.resolveExternalPathToInternal(newExternalPath);
        Collection<WorkspaceAndPath> oldWsAndPaths = pathMappings.resolveExternalPathToInternal(oldExternalPath);

        // This method is unfortunately quite complicated because, while a single node can be moved within a connector's
        // single tree of content, different projections might mean the node's old location is in one projection while
        // the new location is in a different projection (especially considering that a projection projects a single
        // external node into a single internal node within a given workspace). Also, multiple projections can apply to a single
        // external node.
        //
        // Therefore, a single move within a connector's content tree might need to be mapped as a combination of
        // NODE_MOVED, NODE_CREATED, and NODE_REMOVED events. And because the general algorithm is a bit more complicated,
        // there are a few special cases where the logic (and overhead) can be much simpler. These special cases are
        // also quite common, so it's worth it to have the separate logic.

        int numNew = newWsAndPaths.size();
        int numOld = oldWsAndPaths.size();

        if (numNew == 0) {
            // The node was moved to a location that is not in a projection ...
            if (numOld == 0) {
                // this is an edge case, because the old location was not in a projection, either
                return;
            }
            // There are only old locations, so treat as NODE_REMOVED.
            for (WorkspaceAndPath wsAndOldPath : oldWsAndPaths) {
                changesFor(wsAndOldPath.getWorkspaceName()).nodeRemoved(key, oldParentKey, wsAndOldPath.getPath());
            }
            return;
        } else if (numOld == 0) {
            // There are just new nodes, so treat as NODE_CREATED.
            // Note that we do not know the properties ...
            Map<Name, Property> properties = Collections.emptyMap();
            for (WorkspaceAndPath wsAndNewPath : newWsAndPaths) {
                changesFor(wsAndNewPath.getWorkspaceName()).nodeCreated(key, newParentKey, wsAndNewPath.getPath(), properties);
            }
            return;
        }

        assert numNew >= 1;
        assert numOld >= 1;

        // Check for the most common case (just one new location and one old location) and use a more optimal algorithm ...
        if (numNew == 1 && numOld == 1) {
            WorkspaceAndPath newWsAndPath = newWsAndPaths.iterator().next();
            WorkspaceAndPath oldWsAndPath = newWsAndPaths.iterator().next();
            String newWorkspace = newWsAndPath.getWorkspaceName();
            String oldWorkspace = oldWsAndPath.getWorkspaceName();
            if (newWorkspace.equals(oldWorkspace)) {
                // The workspaces are the same, so this is the case of a simple move
                changesFor(newWorkspace).nodeMoved(key,
                                                   newParentKey,
                                                   oldParentKey,
                                                   newWsAndPath.getPath(),
                                                   oldWsAndPath.getPath());
                return;
            }
            // The workspace names don't match, so treat the old as a NODE_REMOVED ...
            changesFor(oldWsAndPath.getWorkspaceName()).nodeRemoved(key, oldParentKey, oldWsAndPath.getPath());
            // And the new as NODE_CREATED (in a separate workspace) ...
            // Note that we do not know the properties ...
            Map<Name, Property> properties = Collections.emptyMap();
            changesFor(newWsAndPath.getWorkspaceName()).nodeCreated(key, newParentKey, newWsAndPath.getPath(), properties);
            return;
        }

        assert numNew > 1 || numOld > 1;

        // Finally the general case. Here, we need to make sure that we don't lose any old locations that did not correspond
        // to at least new location. Since this is the last algorithm, we're actually going to remove all elements from
        // the 'oldWsAndPaths' collection as soon as we move them. (If multiple new locations map to a single old location,
        // then we'll have only one NODE_MOVED and one or more NODE_CREATED.)
        for (WorkspaceAndPath wsAndNewPath : newWsAndPaths) {
            // Look for the projections of the old external path in the same workspace ...
            boolean found = false;
            Iterator<WorkspaceAndPath> oldWsAndPathsIter = oldWsAndPaths.iterator();
            while (oldWsAndPathsIter.hasNext()) {
                WorkspaceAndPath wsAndOldPath = oldWsAndPathsIter.next();
                String newWorkspace = wsAndNewPath.getWorkspaceName();
                String oldWorkspace = wsAndOldPath.getWorkspaceName();
                if (newWorkspace.equals(oldWorkspace)) {
                    found = true;
                    changesFor(newWorkspace).nodeMoved(key,
                                                       newParentKey,
                                                       oldParentKey,
                                                       wsAndNewPath.getPath(),
                                                       wsAndOldPath.getPath());
                    oldWsAndPathsIter.remove(); // we don't want to deal with this WorkspaceAndPath as the 'from' of another move
                }
            }
            if (!found) {
                // The node appeared in one workspace, but it was moved from a node that projected into a different workspace,
                // so treat it as a NODE_CREATED in the new workspace.
                // Note that we do not know the properties ...
                Map<Name, Property> properties = Collections.emptyMap();
                changesFor(wsAndNewPath).nodeCreated(key, newParentKey, wsAndNewPath.getPath(), properties);
            }
        }

        // If there are any old paths left, we need to treat them as NODE_REMOVED ...
        for (WorkspaceAndPath oldWsAndPath : oldWsAndPaths) {
            changesFor(oldWsAndPath).nodeRemoved(key, oldParentKey, oldWsAndPath.getPath());
        }
    }

    @Override
    public void nodeReordered( String docId,
                               String parentDocId,
                               String newPath,
                               String oldNameSegment,
                               String reorderedBeforeNameSegment ) {
        NodeKey key = nodeKey(docId);
        NodeKey parentKey = nodeKey(parentDocId);
        PathFactory pathFactory = pathMappings.getPathFactory();
        Path newExternalPath = pathFactory.create(newPath);
        Path parentPath = newExternalPath.getParent();
        Path oldExternalPath = pathFactory.create(parentPath, pathFactory.createSegment(oldNameSegment));
        Path reorderedBeforePath = reorderedBeforeNameSegment == null ? null : pathFactory.create(parentPath,
                                                                                                  pathFactory.createSegment(reorderedBeforeNameSegment));
        // This external path in the connector may be projected into *multiple* nodes in the same or different workspaces ...
        for (WorkspaceAndPath wsAndPath : pathMappings.resolveExternalPathToInternal(newExternalPath)) {
            changesFor(wsAndPath).nodeReordered(key, parentKey, wsAndPath.getPath(), oldExternalPath, reorderedBeforePath);
        }
    }

    @Override
    public void propertyAdded( String docId,
                               String nodePath,
                               Property property ) {
        NodeKey key = nodeKey(docId);
        Path externalPath = pathMappings.getPathFactory().create(nodePath);
        // This external path in the connector may be projected into *multiple* nodes in the same or different workspaces ...
        for (WorkspaceAndPath wsAndPath : pathMappings.resolveExternalPathToInternal(externalPath)) {
            changesFor(wsAndPath).propertyAdded(key, wsAndPath.getPath(), property);
        }
    }

    @Override
    public void propertyRemoved( String docId,
                                 String nodePath,
                                 Property property ) {
        NodeKey key = nodeKey(docId);
        Path externalPath = pathMappings.getPathFactory().create(nodePath);
        // This external path in the connector may be projected into *multiple* nodes in the same or different workspaces ...
        for (WorkspaceAndPath wsAndPath : pathMappings.resolveExternalPathToInternal(externalPath)) {
            changesFor(wsAndPath).propertyRemoved(key, wsAndPath.getPath(), property);
        }
    }

    @Override
    public void propertyChanged( String docId,
                                 String nodePath,
                                 Property newProperty,
                                 Property oldProperty ) {
        NodeKey key = nodeKey(docId);
        Path externalPath = pathMappings.getPathFactory().create(nodePath);
        // This external path in the connector may be projected into *multiple* nodes in the same or different workspaces ...
        for (WorkspaceAndPath wsAndPath : pathMappings.resolveExternalPathToInternal(externalPath)) {
            changesFor(wsAndPath).propertyChanged(key, wsAndPath.getPath(), newProperty, oldProperty);
        }
    }

    @Override
    public void publish( Map<String, String> data ) {
        DateTime now = timeFactory.create();
        if (data == null) data = Collections.emptyMap();
        // Freeze and then notify the bus of each change set of a given workspace ...
        for (RecordingChanges changes : changesByWorkspace.values()) {
            changes.freeze(connectorSourceName, data, now);
            bus.notify(changes);
        }
        changesByWorkspace.clear();
    }

    private NodeKey nodeKey( String documentId ) {
        return FederatedDocumentStore.documentIdToNodeKey(connectorSourceName, documentId);
    }

    @Override
    public String toString() {
        return "Change set for connector '" + connectorSourceName + "': " + changesByWorkspace;
    }
}
