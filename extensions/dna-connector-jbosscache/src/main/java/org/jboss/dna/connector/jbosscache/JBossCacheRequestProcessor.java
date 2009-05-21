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
package org.jboss.dna.connector.jbosscache;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.jboss.cache.Cache;
import org.jboss.cache.Fqn;
import org.jboss.cache.Node;
import org.jboss.dna.common.util.Logger;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.observe.Observer;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.graph.property.UuidFactory;
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
import org.jboss.dna.graph.request.Request;
import org.jboss.dna.graph.request.UpdatePropertiesRequest;
import org.jboss.dna.graph.request.VerifyWorkspaceRequest;
import org.jboss.dna.graph.request.processor.RequestProcessor;

/**
 * A {@link RequestProcessor} implementation that operates upon a {@link Cache JBoss Cache} instance for each workspace in the
 * {@link JBossCacheSource source}.
 * <p>
 * This processor only uses {@link Location} objects with {@link Location#getPath() paths}. Even though every node in the cache is
 * automatically assigned a UUID (and all operations properly handle UUIDs), these UUIDs are not included in the {@link Location}
 * objects because the processor is unable to search the cache to find nodes by UUID.
 * </p>
 */
public class JBossCacheRequestProcessor extends RequestProcessor {

    private final JBossCacheWorkspaces workspaces;
    private final boolean creatingWorkspacesAllowed;
    private final String defaultWorkspaceName;
    private final PathFactory pathFactory;
    private final PropertyFactory propertyFactory;
    private final UuidFactory uuidFactory;

    /**
     * @param sourceName the name of the source in which this processor is operating
     * @param context the execution context in which this processor operates
     * @param observer the observer to which events should be published; may be null if the events are not be published
     * @param workspaces the manager for the workspaces
     * @param defaultWorkspaceName the name of the default workspace; never null
     * @param creatingWorkspacesAllowed true if clients can create new workspaces, or false otherwise
     */
    JBossCacheRequestProcessor( String sourceName,
                                ExecutionContext context,
                                Observer observer,
                                JBossCacheWorkspaces workspaces,
                                String defaultWorkspaceName,
                                boolean creatingWorkspacesAllowed ) {
        super(sourceName, context, observer);
        assert workspaces != null;
        assert defaultWorkspaceName != null;
        this.workspaces = workspaces;
        this.creatingWorkspacesAllowed = creatingWorkspacesAllowed;
        this.defaultWorkspaceName = defaultWorkspaceName;
        this.pathFactory = context.getValueFactories().getPathFactory();
        this.propertyFactory = context.getPropertyFactory();
        this.uuidFactory = context.getValueFactories().getUuidFactory();
    }

    @Override
    public void process( ReadAllChildrenRequest request ) {
        // Look up the cache and the node ...
        Cache<Name, Object> cache = getCache(request, request.inWorkspace());
        if (cache == null) return;
        Path nodePath = request.of().getPath();
        Node<Name, Object> node = getNode(request, cache, nodePath);
        if (node == null) return;

        // Get the names of the children, using the child list ...
        Path.Segment[] childList = (Path.Segment[])node.get(JBossCacheLexicon.CHILD_PATH_SEGMENT_LIST);
        if (childList != null) {
            for (Path.Segment child : childList) {
                request.addChild(Location.create(pathFactory.create(nodePath, child)));
            }
        }
        request.setActualLocationOfNode(Location.create(nodePath));
        setCacheableInfo(request);
    }

    @Override
    public void process( ReadAllPropertiesRequest request ) {
        // Look up the cache and the node ...
        Cache<Name, Object> cache = getCache(request, request.inWorkspace());
        if (cache == null) return;
        Path nodePath = request.at().getPath();
        Node<Name, Object> node = getNode(request, cache, nodePath);
        if (node == null) return;

        // Get the properties on the node ...
        Map<Name, Object> dataMap = node.getData();
        for (Map.Entry<Name, Object> data : dataMap.entrySet()) {
            Name propertyName = data.getKey();
            // Don't allow the child list property to be accessed
            if (propertyName.equals(JBossCacheLexicon.CHILD_PATH_SEGMENT_LIST)) continue;
            Object values = data.getValue();
            Property property = propertyFactory.create(propertyName, values);
            request.addProperty(property);
        }
        request.setActualLocationOfNode(Location.create(nodePath));
        setCacheableInfo(request);
    }

    @Override
    public void process( CreateNodeRequest request ) {
        // Look up the cache and the node ...
        Cache<Name, Object> cache = getCache(request, request.inWorkspace());
        if (cache == null) return;
        Path parent = request.under().getPath();
        Node<Name, Object> parentNode = getNode(request, cache, parent);
        if (parentNode == null) return;

        // Update the children to account for same-name siblings.
        // This not only updates the FQN of the child nodes, but it also sets the property that stores the
        // the array of Path.Segment for the children (since the cache doesn't maintain order).
        Path.Segment newSegment = updateChildList(cache, parentNode, request.named(), getExecutionContext(), true);
        Node<Name, Object> node = parentNode.addChild(Fqn.fromElements(newSegment));
        assert checkChildren(parentNode);

        // Add the UUID property (if required), which may be overwritten by a supplied property ...
        node.put(DnaLexicon.UUID, uuidFactory.create());
        // Now add the properties to the supplied node ...
        for (Property property : request.properties()) {
            if (property.size() == 0) continue;
            Name propName = property.getName();
            Object value = null;
            if (property.size() == 1) {
                value = property.iterator().next();
            } else {
                value = property.getValuesAsArray();
            }
            node.put(propName, value);
        }
        Path nodePath = pathFactory.create(parent, newSegment);
        request.setActualLocationOfNode(Location.create(nodePath));
        recordChange(request);
    }

    @Override
    public void process( UpdatePropertiesRequest request ) {
        // Look up the cache and the node ...
        Cache<Name, Object> cache = getCache(request, request.inWorkspace());
        if (cache == null) return;
        Path nodePath = request.on().getPath();
        Node<Name, Object> node = getNode(request, cache, nodePath);
        if (node == null) return;

        // Now set (or remove) the properties to the supplied node ...
        for (Map.Entry<Name, Property> entry : request.properties().entrySet()) {
            Name propName = entry.getKey();
            // Don't allow the child list property to be removed or changed
            if (propName.equals(JBossCacheLexicon.CHILD_PATH_SEGMENT_LIST)) continue;

            Property property = entry.getValue();
            if (property == null) {
                node.remove(propName);
                continue;
            }
            Object value = null;
            if (property.isSingle()) {
                value = property.iterator().next();
            } else {
                value = property.getValuesAsArray();
            }
            node.put(propName, value);
        }
        request.setActualLocationOfNode(Location.create(nodePath));
        recordChange(request);
    }

    @Override
    public void process( CopyBranchRequest request ) {
        // Look up the caches ...
        Cache<Name, Object> fromCache = getCache(request, request.fromWorkspace());
        if (fromCache == null) return;
        Cache<Name, Object> intoCache = getCache(request, request.intoWorkspace());
        if (intoCache == null) return;

        // Look up the current node and the new parent (both of which must exist) ...
        Path nodePath = request.from().getPath();
        Node<Name, Object> node = getNode(request, fromCache, nodePath);
        if (node == null) return;
        Path newParentPath = request.into().getPath();
        Node<Name, Object> newParent = getNode(request, intoCache, newParentPath);
        if (newParent == null) return;

        boolean useSameUuids = fromCache != intoCache;
        UUID uuid = uuidFactory.create(node.get(DnaLexicon.UUID));
        UUID newNodeUuid = useSameUuids ? uuid : uuidFactory.create();

        // Copy the branch ...
        Name desiredName = request.desiredName();
        Path.Segment newSegment = copyNode(intoCache,
                                           node,
                                           newParent,
                                           desiredName,
                                           true,
                                           useSameUuids,
                                           newNodeUuid,
                                           null,
                                           getExecutionContext());

        Path newPath = pathFactory.create(newParentPath, newSegment);
        request.setActualLocations(Location.create(nodePath), Location.create(newPath));
        recordChange(request);
    }

    @Override
    public void process( DeleteBranchRequest request ) {
        // Look up the cache and the node ...
        Cache<Name, Object> cache = getCache(request, request.inWorkspace());
        if (cache == null) return;
        Path nodePath = request.at().getPath();
        Node<Name, Object> node = getNode(request, cache, nodePath);
        if (node == null) return;

        Path.Segment nameOfRemovedNode = nodePath.getLastSegment();
        Node<Name, Object> parent = node.getParent();
        if (cache.removeNode(node.getFqn())) {
            removeFromChildList(cache, parent, nameOfRemovedNode, getExecutionContext());
            request.setActualLocationOfNode(Location.create(nodePath));
            recordChange(request);
        } else {
            String msg = JBossCacheConnectorI18n.unableToDeleteBranch.text(getSourceName(), request.inWorkspace(), nodePath);
            request.setError(new RepositorySourceException(msg));
        }
    }

    @Override
    public void process( MoveBranchRequest request ) {
        // Look up the caches ...
        Cache<Name, Object> cache = getCache(request, request.inWorkspace());
        if (cache == null) return;

        // Look up the current node and the new parent (both of which must exist) ...
        Path nodePath = request.from().getPath();
        Node<Name, Object> node = getNode(request, cache, nodePath);
        if (node == null) return;
        Path newParentPath = request.into().getPath();
        Node<Name, Object> newParent = getNode(request, cache, newParentPath);
        if (newParent == null) return;

        // Copy the branch and use the same UUIDs ...
        Name desiredName = request.desiredName();
        Path.Segment newSegment = copyNode(cache, node, newParent, desiredName, true, true, null, null, getExecutionContext());

        // Now delete the old node ...
        Node<Name, Object> oldParent = node.getParent();
        boolean removed = oldParent.removeChild(node.getFqn().getLastElement());
        assert removed;
        Path.Segment nameOfRemovedNode = nodePath.getLastSegment();
        removeFromChildList(cache, oldParent, nameOfRemovedNode, getExecutionContext());

        Path newPath = pathFactory.create(newParentPath, newSegment);
        request.setActualLocations(Location.create(nodePath), Location.create(newPath));
        recordChange(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.VerifyWorkspaceRequest)
     */
    @Override
    public void process( VerifyWorkspaceRequest request ) {
        String workspaceName = request.workspaceName();
        if (workspaceName == null) workspaceName = defaultWorkspaceName;

        Cache<Name, Object> cache = workspaces.getWorkspace(workspaceName, false);
        if (cache == null) {
            String msg = JBossCacheConnectorI18n.workspaceDoesNotExist.text(getSourceName(), workspaceName);
            request.setError(new InvalidWorkspaceException(msg));
        } else {
            Fqn<?> rootName = Fqn.root();
            UUID uuid = uuidFactory.create(cache.get(rootName, DnaLexicon.UUID));
            if (uuid == null) {
                uuid = uuidFactory.create();
                cache.put(rootName, DnaLexicon.UUID, uuid);
            }
            request.setActualRootLocation(Location.create(pathFactory.createRootPath()));
            request.setActualWorkspaceName(workspaceName);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.GetWorkspacesRequest)
     */
    @Override
    public void process( GetWorkspacesRequest request ) {
        request.setAvailableWorkspaceNames(workspaces.getWorkspaceNames());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CreateWorkspaceRequest)
     */
    @Override
    public void process( CreateWorkspaceRequest request ) {
        String workspaceName = request.desiredNameOfNewWorkspace();
        if (!creatingWorkspacesAllowed) {
            String msg = JBossCacheConnectorI18n.unableToCreateWorkspaces.text(getSourceName(), workspaceName);
            request.setError(new InvalidRequestException(msg));
            return;
        }
        // Try to create the workspace ...
        Cache<Name, Object> cache = workspaces.getWorkspace(workspaceName, creatingWorkspacesAllowed);
        if (cache == null) {
            String msg = JBossCacheConnectorI18n.unableToCreateWorkspace.text(getSourceName(), workspaceName);
            request.setError(new InvalidWorkspaceException(msg));
            return;
        }
        // Make sure the root node has a UUID ...
        Fqn<?> rootName = Fqn.root();
        UUID uuid = uuidFactory.create(cache.get(rootName, DnaLexicon.UUID));
        if (uuid == null) {
            uuid = uuidFactory.create();
            cache.put(rootName, DnaLexicon.UUID, uuid);
        }
        request.setActualRootLocation(Location.create(pathFactory.createRootPath()));
        request.setActualWorkspaceName(workspaceName);
        recordChange(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.CloneWorkspaceRequest)
     */
    @Override
    public void process( CloneWorkspaceRequest request ) {
        String fromWorkspaceName = request.nameOfWorkspaceToBeCloned();
        String toWorkspaceName = request.desiredNameOfTargetWorkspace();
        if (!creatingWorkspacesAllowed) {
            String msg = JBossCacheConnectorI18n.unableToCloneWorkspaces.text(getSourceName(), fromWorkspaceName, toWorkspaceName);
            request.setError(new InvalidRequestException(msg));
            return;
        }
        // Make sure there is already a workspace that we're cloning ...
        Cache<Name, Object> fromCache = workspaces.getWorkspace(fromWorkspaceName, false);
        if (fromCache == null) {
            String msg = JBossCacheConnectorI18n.workspaceDoesNotExist.text(getSourceName(), fromWorkspaceName);
            request.setError(new InvalidWorkspaceException(msg));
            return;
        }

        // Try to create a new workspace with the target name ...
        Cache<Name, Object> intoCache = workspaces.createWorkspace(toWorkspaceName);
        if (intoCache == null) {
            // Couldn't create it because one already exists ...
            String msg = JBossCacheConnectorI18n.workspaceAlreadyExists.text(getSourceName(), toWorkspaceName);
            request.setError(new InvalidWorkspaceException(msg));
            return;
        }

        // And finally copy the contents ...
        Fqn<?> rootName = Fqn.root();
        Node<Name, Object> fromRoot = fromCache.getNode(rootName);
        Node<Name, Object> intoRoot = intoCache.getNode(rootName);
        intoRoot.clearData();
        intoRoot.putAll(fromRoot.getData());
        ExecutionContext context = getExecutionContext();

        // Loop over each child and copy it ...
        for (Node<Name, Object> child : fromRoot.getChildren()) {
            copyNode(intoCache, child, intoRoot, null, true, true, null, null, context);
        }

        // Copy the list of child segments in the root (this maintains the order of the children) ...
        Path.Segment[] childNames = (Path.Segment[])fromRoot.get(JBossCacheLexicon.CHILD_PATH_SEGMENT_LIST);
        intoRoot.put(JBossCacheLexicon.CHILD_PATH_SEGMENT_LIST, childNames);
        recordChange(request);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.request.processor.RequestProcessor#process(org.jboss.dna.graph.request.DestroyWorkspaceRequest)
     */
    @Override
    public void process( DestroyWorkspaceRequest request ) {
        Cache<Name, Object> fromCache = workspaces.getWorkspace(request.workspaceName(), false);
        if (fromCache == null) {
            String msg = JBossCacheConnectorI18n.workspaceDoesNotExist.text(getSourceName(), request.workspaceName());
            request.setError(new InvalidWorkspaceException(msg));
            return;
        }
        request.setActualRootLocation(Location.create(pathFactory.createRootPath()));
        recordChange(request);
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Utility methods
    // ----------------------------------------------------------------------------------------------------------------

    /**
     * Obtain the appropriate cache for the supplied workspace name, or set an error on the request if the workspace does not
     * exist (and could not or should not be created).
     * 
     * @param request the request
     * @param workspaceName the workspace name
     * @return the cache, or null if there is no such workspace
     */
    protected Cache<Name, Object> getCache( Request request,
                                            String workspaceName ) {
        if (workspaceName == null) workspaceName = defaultWorkspaceName;
        Cache<Name, Object> cache = workspaces.getWorkspace(workspaceName, creatingWorkspacesAllowed);
        if (cache == null) {
            String msg = JBossCacheConnectorI18n.workspaceDoesNotExist.text(getSourceName(), workspaceName);
            request.setError(new InvalidWorkspaceException(msg));
        }
        return cache;
    }

    protected Fqn<?> getFullyQualifiedName( Path path ) {
        assert path != null;
        return Fqn.fromList(path.getSegmentsList());
    }

    /**
     * Get a relative fully-qualified name that consists only of the supplied segment.
     * 
     * @param pathSegment the segment from which the fully qualified name is to be created
     * @return the relative fully-qualified name
     */
    protected Fqn<?> getFullyQualifiedName( Path.Segment pathSegment ) {
        assert pathSegment != null;
        return Fqn.fromElements(pathSegment);
    }

    @SuppressWarnings( "unchecked" )
    protected Path getPath( PathFactory factory,
                            Fqn<?> fqn ) {
        List<Path.Segment> segments = (List<Path.Segment>)fqn.peekElements();
        return factory.create(factory.createRootPath(), segments);
    }

    protected Node<Name, Object> getNode( Request request,
                                          Cache<Name, Object> cache,
                                          Path path ) {
        ExecutionContext context = getExecutionContext();
        if (path == null) {
            String msg = JBossCacheConnectorI18n.locationsMustHavePath.text(getSourceName(), request);
            request.setError(new InvalidRequestException(msg));
            return null;
        }
        // Look up the node with the supplied path ...
        Fqn<?> fqn = getFullyQualifiedName(path);
        Node<Name, Object> node = cache.getNode(fqn);
        if (node == null) {
            String nodePath = path.getString(context.getNamespaceRegistry());
            Path lowestExisting = null;
            while (fqn != null) {
                fqn = fqn.getParent();
                node = cache.getNode(fqn);
                if (node != null) {
                    lowestExisting = getPath(context.getValueFactories().getPathFactory(), fqn);
                    fqn = null;
                }
            }
            request.setError(new PathNotFoundException(Location.create(path), lowestExisting,
                                                       JBossCacheConnectorI18n.nodeDoesNotExist.text(nodePath)));
            node = null;
        }
        return node;

    }

    protected Path.Segment copyNode( Cache<Name, Object> newCache,
                                     Node<Name, Object> original,
                                     Node<Name, Object> newParent,
                                     Name desiredName,
                                     boolean recursive,
                                     boolean reuseOriginalUuids,
                                     UUID uuidForCopyOfOriginal,
                                     AtomicInteger count,
                                     ExecutionContext context ) {
        assert original != null;
        assert newParent != null;
        // Get or create the new node ...
        Path.Segment name = desiredName != null ? context.getValueFactories().getPathFactory().createSegment(desiredName) : (Path.Segment)original.getFqn()
                                                                                                                                                  .getLastElement();

        // Update the children to account for same-name siblings.
        // This not only updates the FQN of the child nodes, but it also sets the property that stores the
        // the array of Path.Segment for the children (since the cache doesn't maintain order).
        Path.Segment newSegment = updateChildList(newCache, newParent, name.getName(), context, true);
        Node<Name, Object> copy = newParent.addChild(getFullyQualifiedName(newSegment));
        assert checkChildren(newParent);
        // Copy the properties ...
        copy.clearData();
        copy.putAll(original.getData());
        copy.remove(JBossCacheLexicon.CHILD_PATH_SEGMENT_LIST); // will be reset later ...

        // Generate a new UUID for the new node, overwriting any existing value from the original ...
        if (reuseOriginalUuids) uuidForCopyOfOriginal = uuidFactory.create(original.get(DnaLexicon.UUID));
        if (uuidForCopyOfOriginal == null) uuidForCopyOfOriginal = uuidFactory.create();
        copy.put(DnaLexicon.UUID, uuidForCopyOfOriginal);

        if (count != null) count.incrementAndGet();
        if (recursive) {
            // Loop over each child and call this method ...
            for (Node<Name, Object> child : original.getChildren()) {
                copyNode(newCache, child, copy, null, true, reuseOriginalUuids, null, count, context);
            }
        }
        return newSegment;
    }

    /**
     * Update (or create) the array of {@link Path.Segment path segments} for the children of the supplied node. This array
     * maintains the ordered list of children (since the {@link Cache} does not maintain the order). Invoking this method will
     * change any existing children that a {@link Path.Segment#getName() name part} that matches the supplied
     * <code>changedName</code> to have the appropriate {@link Path.Segment#getIndex() same-name sibling index}.
     * 
     * @param cache the cache in which the parent exists ...
     * @param parent the parent node; may not be null
     * @param changedName the name that should be compared to the existing node siblings to determine whether the same-name
     *        sibling indexes should be updated; may not be null
     * @param context the execution context; may not be null
     * @param addChildWithName true if a new child with the supplied name is to be added to the children (but which does not yet
     *        exist in the node's children)
     * @return the path segment for the new child, or null if <code>addChildWithName</code> was false
     */
    protected Path.Segment updateChildList( Cache<Name, Object> cache,
                                            Node<Name, Object> parent,
                                            Name changedName,
                                            ExecutionContext context,
                                            boolean addChildWithName ) {
        assert parent != null;
        assert changedName != null;
        assert context != null;
        Set<Node<Name, Object>> children = parent.getChildren();
        if (children.isEmpty() && !addChildWithName) return null;

        // Go through the children, looking for any children with the same name as the 'changedName'
        List<ChildInfo> childrenWithChangedName = new LinkedList<ChildInfo>();
        Path.Segment[] childNames = (Path.Segment[])parent.get(JBossCacheLexicon.CHILD_PATH_SEGMENT_LIST);
        int index = 0;
        if (childNames != null) {
            for (Path.Segment childName : childNames) {
                if (childName.getName().equals(changedName)) {
                    ChildInfo info = new ChildInfo(childName, index);
                    childrenWithChangedName.add(info);
                }
                index++;
            }
        }
        if (addChildWithName) {
            // Make room for the new child at the end of the array ...
            if (childNames == null) {
                childNames = new Path.Segment[1];
            } else {
                int numExisting = childNames.length;
                Path.Segment[] newChildNames = new Path.Segment[numExisting + 1];
                System.arraycopy(childNames, 0, newChildNames, 0, numExisting);
                childNames = newChildNames;
            }

            // And add a child info for the new node ...
            ChildInfo info = new ChildInfo(null, index);
            childrenWithChangedName.add(info);
            Path.Segment newSegment = context.getValueFactories().getPathFactory().createSegment(changedName);
            childNames[index++] = newSegment;
        }
        assert childNames != null;

        // Now process the children with the same name, which may include a child info for the new node ...
        assert childrenWithChangedName.isEmpty() == false;
        if (childrenWithChangedName.size() == 1) {
            // The child should have no indexes ...
            ChildInfo child = childrenWithChangedName.get(0);
            if (child.segment != null && child.segment.hasIndex()) {
                // The existing child needs to have a new index ..
                Path.Segment newSegment = context.getValueFactories().getPathFactory().createSegment(changedName);
                // Replace the child with the correct FQN ...
                changeNodeName(cache, parent, child.segment, newSegment, context);
                // Change the segment in the child list ...
                childNames[child.childIndex] = newSegment;
            }
        } else {
            // There is more than one child with the same name ...
            int i = 0;
            for (ChildInfo child : childrenWithChangedName) {
                if (child.segment != null) {
                    // Determine the new name and index ...
                    Path.Segment newSegment = context.getValueFactories().getPathFactory().createSegment(changedName, i + 1);
                    // Replace the child with the correct FQN ...
                    changeNodeName(cache, parent, child.segment, newSegment, context);
                    // Change the segment in the child list ...
                    childNames[child.childIndex] = newSegment;
                } else {
                    // Determine the new name and index ...
                    Path.Segment newSegment = context.getValueFactories().getPathFactory().createSegment(changedName, i + 1);
                    childNames[child.childIndex] = newSegment;
                }
                ++i;
            }
        }

        // Record the list of children as a property on the parent ...
        // (Do this last, as it doesn't need to be done if there's an exception in the above logic)
        context.getLogger(getClass()).trace("Updating child list of {0} to: {1}", parent.getFqn(), Arrays.asList(childNames));
        parent.put(JBossCacheLexicon.CHILD_PATH_SEGMENT_LIST, childNames); // replaces any existing value

        if (addChildWithName) {
            // Return the segment for the new node ...
            return childNames[childNames.length - 1];
        }
        return null;
    }

    /**
     * Update the array of {@link Path.Segment path segments} for the children of the supplied node, based upon a node being
     * removed. This array maintains the ordered list of children (since the {@link Cache} does not maintain the order). Invoking
     * this method will change any existing children that a {@link Path.Segment#getName() name part} that matches the supplied
     * <code>changedName</code> to have the appropriate {@link Path.Segment#getIndex() same-name sibling index}.
     * 
     * @param cache the cache in which the parent exists ...
     * @param parent the parent node; may not be null
     * @param removedNode the segment of the node that was removed, which signals to look for node with the same name; may not be
     *        null
     * @param context the execution context; may not be null
     */
    protected void removeFromChildList( Cache<Name, Object> cache,
                                        Node<Name, Object> parent,
                                        Path.Segment removedNode,
                                        ExecutionContext context ) {
        assert parent != null;
        assert context != null;
        Set<Node<Name, Object>> children = parent.getChildren();
        if (children.isEmpty()) {
            parent.put(JBossCacheLexicon.CHILD_PATH_SEGMENT_LIST, null); // replaces any existing value
            return;
        }

        // Go through the children, looking for any children with the same name as the 'changedName'
        Path.Segment[] childNames = (Path.Segment[])parent.get(JBossCacheLexicon.CHILD_PATH_SEGMENT_LIST);
        assert childNames != null;
        int snsIndex = removedNode.getIndex();
        int index = 0;
        Path.Segment[] newChildNames = new Path.Segment[childNames.length - 1];
        for (Path.Segment childName : childNames) {
            if (!childName.getName().equals(removedNode.getName())) {
                newChildNames[index] = childName;
                index++;
            } else {
                // The name matches ...
                if (childName.getIndex() < snsIndex) {
                    // Just copy ...
                    newChildNames[index] = childName;
                    index++;
                } else if (childName.getIndex() == snsIndex) {
                    // don't copy ...
                } else {
                    // Append an updated segment ...
                    Path.Segment newSegment = context.getValueFactories()
                                                     .getPathFactory()
                                                     .createSegment(childName.getName(), childName.getIndex() - 1);
                    newChildNames[index] = newSegment;
                    // Replace the child with the correct FQN ...
                    changeNodeName(cache, parent, childName, newSegment, context);
                    index++;
                }
            }
        }
        parent.put(JBossCacheLexicon.CHILD_PATH_SEGMENT_LIST, newChildNames); // replaces any existing value
    }

    protected boolean checkChildren( Node<Name, Object> parent ) {
        Path.Segment[] childNamesProperty = (Path.Segment[])parent.get(JBossCacheLexicon.CHILD_PATH_SEGMENT_LIST);
        Set<Object> childNames = parent.getChildrenNames();
        boolean result = true;
        if (childNamesProperty.length != childNames.size()) result = false;
        for (int i = 0; i != childNamesProperty.length; ++i) {
            if (!childNames.contains(childNamesProperty[i])) result = false;
        }
        if (!result) {
            List<Path.Segment> names = new ArrayList<Path.Segment>();
            for (Object name : childNames) {
                names.add((Path.Segment)name);
            }
            Collections.sort(names);
            Logger.getLogger(getClass()).trace("Child list on {0} is: {1}", parent.getFqn(), childNamesProperty);
            Logger.getLogger(getClass()).trace("Children of {0} is: {1}", parent.getFqn(), names);
        }
        return result;
    }

    /**
     * Utility class used by the {@link #updateChildList(Cache, Node, Name, ExecutionContext, boolean)} method.
     * 
     * @author Randall Hauch
     */
    private static class ChildInfo {
        protected final Path.Segment segment;
        protected final int childIndex;

        protected ChildInfo( Path.Segment childSegment,
                             int childIndex ) {
            this.segment = childSegment;
            this.childIndex = childIndex;
        }

    }

    /**
     * Changes the name of the node in the cache (but does not update the list of child segments stored on the parent).
     * 
     * @param cache
     * @param parent
     * @param existing
     * @param newSegment
     * @param context
     */
    protected void changeNodeName( Cache<Name, Object> cache,
                                   Node<Name, Object> parent,
                                   Path.Segment existing,
                                   Path.Segment newSegment,
                                   ExecutionContext context ) {
        assert parent != null;
        assert existing != null;
        assert newSegment != null;
        assert context != null;

        if (existing.equals(newSegment)) return;
        context.getLogger(getClass()).trace("Renaming {0} to {1} under {2}", existing, newSegment, parent.getFqn());
        Node<Name, Object> existingChild = parent.getChild(existing);
        assert existingChild != null;

        // JBoss Cache can move a node from one node to another node, but the move doesn't change the name;
        // since you provide the FQN of the parent location, the name of the node cannot be changed.
        // Therefore, to compensate, we need to create a new child, copy all of the data, move all of the child
        // nodes of the old node, then remove the old node.

        // Create the new node ...
        Node<Name, Object> newChild = parent.addChild(Fqn.fromElements(newSegment));
        Fqn<?> newChildFqn = newChild.getFqn();

        // Copy the data ...
        newChild.putAll(existingChild.getData());

        // Move the children ...
        for (Node<Name, Object> grandChild : existingChild.getChildren()) {
            cache.move(grandChild.getFqn(), newChildFqn);
        }

        // Remove the existing ...
        parent.removeChild(existing);
    }
}
