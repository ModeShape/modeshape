/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.connector.inmemory;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.CommonI18n;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.DnaLexicon;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Path;
import org.jboss.dna.graph.properties.PathFactory;
import org.jboss.dna.graph.properties.PathNotFoundException;
import org.jboss.dna.graph.properties.Property;
import org.jboss.dna.graph.properties.PropertyFactory;
import org.jboss.dna.graph.properties.Path.Segment;
import org.jboss.dna.graph.properties.basic.BasicPath;
import org.jboss.dna.graph.requests.CopyBranchRequest;
import org.jboss.dna.graph.requests.CreateNodeRequest;
import org.jboss.dna.graph.requests.DeleteBranchRequest;
import org.jboss.dna.graph.requests.MoveBranchRequest;
import org.jboss.dna.graph.requests.ReadAllChildrenRequest;
import org.jboss.dna.graph.requests.ReadAllPropertiesRequest;
import org.jboss.dna.graph.requests.Request;
import org.jboss.dna.graph.requests.UpdatePropertiesRequest;
import org.jboss.dna.graph.requests.processor.RequestProcessor;

/**
 * @author Randall Hauch
 */
@NotThreadSafe
public class InMemoryRepository {

    protected final ReadWriteLock lock = new ReentrantReadWriteLock();
    private final String name;
    private final UUID rootNodeUuid;
    private final Map<UUID, Node> nodesByUuid = new HashMap<UUID, Node>();

    public InMemoryRepository( String name,
                               UUID rootNodeUUID ) {
        CheckArg.isNotNull(rootNodeUUID, "rootNodeUUID");
        CheckArg.isNotEmpty(name, "name");
        this.name = name;
        this.rootNodeUuid = rootNodeUUID;
        // Create the root node ...
        Node root = new Node(rootNodeUUID);
        nodesByUuid.put(root.getUuid(), root);
    }

    /**
     * @return lock
     */
    public ReadWriteLock getLock() {
        return lock;
    }

    /**
     * @return name
     */
    public String getName() {
        return name;
    }

    public Node getRoot() {
        return nodesByUuid.get(this.rootNodeUuid);
    }

    public Node getNode( UUID uuid ) {
        assert uuid != null;
        return nodesByUuid.get(uuid);
    }

    protected Map<UUID, Node> getNodesByUuid() {
        return nodesByUuid;
    }

    public Node getNode( ExecutionContext context,
                         String path ) {
        assert context != null;
        assert path != null;
        return getNode(context.getValueFactories().getPathFactory().create(path));
    }

    /**
     * Find a node with the given path.
     * 
     * @param path the path to the node; may not be null
     * @return the node with the path, or null if the node does not exist
     */
    public Node getNode( Path path ) {
        assert path != null;
        Node node = getRoot();
        for (Path.Segment segment : path) {
            Node desiredChild = null;
            for (Node child : node.getChildren()) {
                if (child == null) continue;
                Path.Segment childName = child.getName();
                if (childName == null) continue;
                if (childName.equals(segment)) {
                    desiredChild = child;
                    break;
                }
            }
            if (desiredChild != null) {
                node = desiredChild;
            } else {
                return null;
            }
        }
        return node;
    }

    /**
     * Find the lowest existing node along the path.
     * 
     * @param path the path to the node; may not be null
     * @return the lowest existing node along the path, or the root node if no node exists on the path
     */
    public Path getLowestExistingPath( Path path ) {
        assert path != null;
        Node node = getRoot();
        int segmentNumber = 0;
        for (Path.Segment segment : path) {
            Node desiredChild = null;
            for (Node child : node.getChildren()) {
                if (child == null) continue;
                Path.Segment childName = child.getName();
                if (childName == null) continue;
                if (childName.equals(segment)) {
                    desiredChild = child;
                    break;
                }
            }
            if (desiredChild != null) {
                node = desiredChild;
            } else {
                return path.subpath(0, segmentNumber);
            }
            ++segmentNumber;
        }
        return BasicPath.ROOT;
    }

    protected UUID generateUuid() {
        return UUID.randomUUID();
    }

    public void removeNode( ExecutionContext context,
                            Node node ) {
        assert context != null;
        assert node != null;
        assert getRoot().equals(node) != true;
        Node parent = node.getParent();
        assert parent != null;
        parent.getChildren().remove(node);
        correctSameNameSiblingIndexes(context, parent, node.getName().getName());
        removeUuidReference(node);
    }

    protected void removeUuidReference( Node node ) {
        nodesByUuid.remove(node.getUuid());
        for (Node child : node.getChildren()) {
            removeUuidReference(child);
        }
    }

    /**
     * Create a node at the supplied path. The parent of the new node must already exist.
     * 
     * @param context the environment; may not be null
     * @param pathToNewNode the path to the new node; may not be null
     * @return the new node (or root if the path specified the root)
     */
    public Node createNode( ExecutionContext context,
                            String pathToNewNode ) {
        assert context != null;
        assert pathToNewNode != null;
        Path path = context.getValueFactories().getPathFactory().create(pathToNewNode);
        if (path.isRoot()) return getRoot();
        Path parentPath = path.getParent();
        Node parentNode = getNode(parentPath);
        Name name = path.getLastSegment().getName();
        return createNode(context, parentNode, name, null);
    }

    /**
     * Create a new node with the supplied name, as a child of the supplied parent.
     * 
     * @param context the execution context
     * @param parentNode the parent node; may not be null
     * @param name the name; may not be null
     * @param uuid the UUID of the node, or null if the UUID is to be generated
     * @return the new node
     */
    public Node createNode( ExecutionContext context,
                            Node parentNode,
                            Name name,
                            UUID uuid ) {
        assert context != null;
        assert name != null;
        if (parentNode == null) parentNode = getRoot();
        if (uuid == null) uuid = generateUuid();
        Node node = new Node(uuid);
        nodesByUuid.put(node.getUuid(), node);
        node.setParent(parentNode);
        Path.Segment newName = context.getValueFactories().getPathFactory().createSegment(name);
        node.setName(newName);
        parentNode.getChildren().add(node);
        correctSameNameSiblingIndexes(context, parentNode, name);
        return node;
    }

    protected void correctSameNameSiblingIndexes( ExecutionContext context,
                                                  Node parentNode,
                                                  Name name ) {
        if (parentNode == null) return;
        // Look for the highest existing index ...
        List<Node> childrenWithSameNames = new LinkedList<Node>();
        for (Node child : parentNode.getChildren()) {
            if (child.getName().getName().equals(name)) childrenWithSameNames.add(child);
        }
        if (childrenWithSameNames.size() == 0) return;
        if (childrenWithSameNames.size() == 1) {
            Node childWithSameName = childrenWithSameNames.get(0);
            Path.Segment newName = context.getValueFactories().getPathFactory().createSegment(name, Path.NO_INDEX);
            childWithSameName.setName(newName);
            return;
        }
        int index = 1;
        for (Node childWithSameName : childrenWithSameNames) {
            Path.Segment segment = childWithSameName.getName();
            if (segment.getIndex() != index) {
                Path.Segment newName = context.getValueFactories().getPathFactory().createSegment(name, index);
                childWithSameName.setName(newName);
            }
            ++index;
        }
    }

    /**
     * Move the supplied node to the new parent. This method automatically removes the node from its existing parent, and also
     * correctly adjusts the {@link Path.Segment#getIndex() index} to be correct in the new parent.
     * 
     * @param context
     * @param node the node to be moved; may not be the {@link #getRoot() root}
     * @param newParent the new parent; may not be the {@link #getRoot() root}
     */
    public void moveNode( ExecutionContext context,
                          Node node,
                          Node newParent ) {
        assert context != null;
        assert newParent != null;
        assert node != null;
        assert getRoot().equals(newParent) != true;
        assert getRoot().equals(node) != true;
        Node oldParent = node.getParent();
        if (oldParent != null) {
            if (oldParent.equals(newParent)) return;
            boolean removed = oldParent.getChildren().remove(node);
            assert removed == true;
            node.setParent(null);
            correctSameNameSiblingIndexes(context, oldParent, node.getName().getName());
        }
        node.setParent(newParent);
        newParent.getChildren().add(node);
        correctSameNameSiblingIndexes(context, newParent, node.getName().getName());
    }

    public Node copyNode( ExecutionContext context,
                          Node original,
                          Node newParent,
                          boolean recursive ) {
        assert context != null;
        assert original != null;
        assert newParent != null;
        // Get or create the new node ...
        Node copy = createNode(context, newParent, original.getName().getName(), null);

        // Copy the properties ...
        copy.getProperties().clear();
        copy.getProperties().putAll(original.getProperties());
        if (recursive) {
            // Loop over each child and call this method ...
            for (Node child : original.getChildren()) {
                copyNode(context, child, copy, true);
            }
        }
        return copy;
    }

    /**
     * Get a request processor given the supplied environment and source name.
     * 
     * @param context the environment in which the commands are to be executed
     * @param sourceName the name of the repository source
     * @return the request processor; never null
     */
    /*package*/RequestProcessor getRequestProcessor( ExecutionContext context,
                                                      String sourceName ) {
        return new Processor(context, sourceName);
    }

    protected class Processor extends RequestProcessor {
        private final PathFactory pathFactory;
        private final PropertyFactory propertyFactory;

        protected Processor( ExecutionContext context,
                             String sourceName ) {
            super(sourceName, context);
            pathFactory = context.getValueFactories().getPathFactory();
            propertyFactory = context.getPropertyFactory();
        }

        @Override
        public void process( ReadAllChildrenRequest request ) {
            Node node = getTargetNode(request, request.of());
            if (node == null) return;
            Path path = request.of().getPath();
            // Get the names of the children ...
            List<Node> children = node.getChildren();
            for (Node child : children) {
                Segment childName = child.getName();
                Path childPath = pathFactory.create(path, childName);
                request.addChild(childPath, propertyFactory.create(DnaLexicon.UUID, child.getUuid()));
            }
            request.setActualLocationOfNode(new Location(path, node.getUuid()));
        }

        @Override
        public void process( ReadAllPropertiesRequest request ) {
            Node node = getTargetNode(request, request.at());
            if (node == null) return;
            // Get the properties of the node ...
            request.addProperty(propertyFactory.create(DnaLexicon.UUID, node.getUuid()));
            for (Property property : node.getProperties().values()) {
                request.addProperty(property);
            }
            request.setActualLocationOfNode(new Location(request.at().getPath(), node.getUuid()));
        }

        @Override
        public void process( CopyBranchRequest request ) {
            Node node = getTargetNode(request, request.from());
            if (node == null) return;
            // Look up the new parent, which must exist ...
            Path newPath = request.into().getPath();
            Path newParentPath = newPath.getParent();
            Node newParent = getNode(newParentPath);
            Node newNode = copyNode(getExecutionContext(), node, newParent, true);
            newPath = getExecutionContext().getValueFactories().getPathFactory().create(newParentPath, newNode.getName());
            Location oldLocation = new Location(request.from().getPath(), node.getUuid());
            Location newLocation = new Location(newPath, newNode.getUuid());
            request.setActualLocations(oldLocation, newLocation);
        }

        @Override
        public void process( CreateNodeRequest request ) {
            Path parent = request.under().getPath();
            CheckArg.isNotNull(parent, "request.under().getPath()");
            Node node = null;
            // Look up the parent node, which must exist ...
            Node parentNode = getNode(parent);
            if (parentNode == null) {
                Path lowestExisting = getLowestExistingPath(parent);
                throw new PathNotFoundException(request.under(), lowestExisting,
                                                InMemoryConnectorI18n.nodeDoesNotExist.text(parent));
            }
            UUID uuid = null;
            for (Property property : request.properties()) {
                if (property.getName().equals(DnaLexicon.UUID)) {
                    uuid = getExecutionContext().getValueFactories().getUuidFactory().create(property.getValues().next());
                    break;
                }
            }
            node = createNode(getExecutionContext(), parentNode, request.named(), uuid);
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
            request.setActualLocationOfNode(new Location(path, node.getUuid()));
        }

        @Override
        public void process( DeleteBranchRequest request ) {
            Node node = getTargetNode(request, request.at());
            if (node == null) return;
            removeNode(getExecutionContext(), node);
            request.setActualLocationOfNode(new Location(request.at().getPath(), node.getUuid()));
        }

        @Override
        public void process( MoveBranchRequest request ) {
            Node node = getTargetNode(request, request.from());
            if (node == null) return;
            // Look up the new parent, which must exist ...
            Path newPath = request.into().getPath();
            Path newParentPath = newPath.getParent();
            Node newParent = getNode(newParentPath);
            node.setParent(newParent);
            newPath = getExecutionContext().getValueFactories().getPathFactory().create(newParentPath, node.getName());
            Location oldLocation = new Location(request.from().getPath(), node.getUuid());
            Location newLocation = new Location(newPath, node.getUuid());
            request.setActualLocations(oldLocation, newLocation);
        }

        @Override
        public void process( UpdatePropertiesRequest request ) {
            Node node = getTargetNode(request, request.on());
            if (node == null) return;
            // Now set (or remove) the properties to the supplied node ...
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
            request.setActualLocationOfNode(new Location(request.on().getPath(), node.getUuid()));
        }

        protected Node getTargetNode( Request request,
                                      Location location ) {
            Path path = location.getPath();
            if (path == null) {
                request.setError(new IllegalArgumentException(CommonI18n.argumentMayNotBeNull.text("location.getPath()")));
                return null;
            }
            // Look up the node with the supplied path ...
            Node node = InMemoryRepository.this.getNode(path);
            if (node == null) {
                Path lowestExisting = getLowestExistingPath(path);
                request.setError(new PathNotFoundException(location, lowestExisting,
                                                           InMemoryConnectorI18n.nodeDoesNotExist.text(path)));
                return null;
            }
            return node;
        }
    }
}
