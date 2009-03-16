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
package org.jboss.dna.jcr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.nodetype.PropertyDefinition;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.Results;
import org.jboss.dna.graph.connector.RepositorySourceException;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.property.ValueFormatException;
import com.google.common.base.ReferenceType;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.ReferenceMap;

/**
 * The class that manages the session's information that has been locally-cached after reading from the underlying {@link Graph
 * repository} or modified by the session but not yet saved or commited to the repository.
 * <p>
 * The cached information is broken into several different categories that are each described below.
 * </p>
 * <h3>JCR API objects</h3>
 * <p>
 * Clients using the DNA JCR implementation obtain a {@link JcrSession JCR Session} (which generally owns this cache instance) as
 * well as the JCR {@link JcrNode Node} and {@link AbstractJcrProperty Property} instances. This cache ensures that the same JCR
 * Node or Property objects are always returned for the same item in the repository, ensuring that the "==" operator always holds
 * true for the same item. However, as soon as all (client) references to these objects are garbage collected, this class is free
 * to also release those objects and, when needed, recreate new implementation objects.
 * </p>
 * <p>
 * This approach helps reduce memory utilization since any unused items are available for garbage collection, but it also
 * guarantees that once a client maintains a reference to an item, the same Java object will always be used for any references to
 * that item.
 * </p>
 * <h3>Cached nodes</h3>
 * <p>
 * The session cache is also responsible for maintaining a local cache of node information retrieved from the underlying
 * repository, reducing the need to request information any more than necessary. This information includes that obtained directly
 * from the repository store, including node properties, children, and references to the parent. It also includes computed
 * information, such as the NodeDefinition for a node, the name of the primary type and mixin types, and the original
 * {@link Location} of the node in the repository.
 * </p>
 * <h3>Transient changes</h3>
 * <p>
 * Any time content is changed in the session, those changes are held within the session until they are saved either by
 * {@link Session#save() saving the session} or {@link Item#save() saving an individual item} (which includes any content below
 * that item). This cache maintains all these transient changes, and when requested will send the change requests down the
 * repository. At any point, these transient changes may be rolled back (or "released"), again either for the
 * {@link Session#refresh(boolean) whole session} or for {@link Item#refresh(boolean) individual items}.
 * </p>
 */
@ThreadSafe
public class SessionCache {

    /**
     * Hidden flag that controls whether properties that appear on DNA nodes but not allowed by the node type or mixins should be
     * included anyway. This is currently {@value} .
     */
    private static final boolean INCLUDE_PROPERTIES_NOT_ALLOWED_BY_NODE_TYPE_OR_MIXINS = true;

    private final JcrSession session;
    private final String workspaceName;
    protected final ExecutionContext context;
    protected final PathFactory pathFactory;
    private final NameFactory nameFactory;
    private final ValueFactory<String> stringFactory;
    private final NamespaceRegistry namespaces;
    private final PropertyFactory propertyFactory;
    private final Graph store;
    private final Name defaultPrimaryTypeName;
    private final Property defaultPrimaryTypeProperty;
    private final Path rootPath;

    private UUID root;
    private final ReferenceMap<UUID, AbstractJcrNode> jcrNodes;
    private final ReferenceMap<PropertyId, AbstractJcrProperty> jcrProperties;

    private final HashMap<UUID, NodeInfo> cachedNodes;
    private final HashMap<UUID, NodeInfo> changedNodes;

    public SessionCache( JcrSession session,
                         String workspaceName,
                         ExecutionContext context,
                         JcrNodeTypeManager nodeTypes,
                         Graph store ) {
        assert session != null;
        assert workspaceName != null;
        assert context != null;
        assert store != null;
        this.session = session;
        this.workspaceName = workspaceName;
        this.store = store;
        this.context = context;
        this.pathFactory = context.getValueFactories().getPathFactory();
        this.nameFactory = context.getValueFactories().getNameFactory();
        this.stringFactory = context.getValueFactories().getStringFactory();
        this.namespaces = context.getNamespaceRegistry();
        this.propertyFactory = context.getPropertyFactory();
        this.defaultPrimaryTypeName = JcrNtLexicon.UNSTRUCTURED;
        this.defaultPrimaryTypeProperty = propertyFactory.create(JcrLexicon.PRIMARY_TYPE, this.defaultPrimaryTypeName);
        this.rootPath = pathFactory.createRootPath();

        this.jcrNodes = new ReferenceMap<UUID, AbstractJcrNode>(ReferenceType.STRONG, ReferenceType.SOFT);
        this.jcrProperties = new ReferenceMap<PropertyId, AbstractJcrProperty>(ReferenceType.STRONG, ReferenceType.SOFT);

        this.cachedNodes = new HashMap<UUID, NodeInfo>();
        this.changedNodes = new HashMap<UUID, NodeInfo>();
    }

    JcrSession session() {
        return session;
    }

    ExecutionContext context() {
        return context;
    }

    String workspaceName() {
        return workspaceName;
    }

    JcrNodeTypeManager nodeTypes() {
        return session.nodeTypeManager();
    }

    public JcrRootNode findJcrRootNode() throws RepositoryException {
        return (JcrRootNode)findJcrNode(findNodeInfoForRoot().getUuid());
    }

    /**
     * Find the JCR {@link JcrNode Node implementation} for the given UUID.
     * 
     * @param uuid the node's UUID
     * @return the existing node implementation
     * @throws ItemNotFoundException if a node with the supplied UUID could not be found
     * @throws RepositoryException if an error resulting in finding this node in the repository
     */
    public AbstractJcrNode findJcrNode( UUID uuid ) throws ItemNotFoundException, RepositoryException {
        AbstractJcrNode node = jcrNodes.get(uuid);
        if (node != null) return node;

        // An existing JCR Node object was not found, so we'll have to create it by finding the underlying
        // NodeInfo for the node (from the changed state or the cache) ...
        NodeInfo info = findNodeInfo(uuid);
        assert info != null;

        // Create the appropriate JCR Node object ...
        return createAndCacheJcrNodeFor(info);
    }

    /**
     * Find the JCR {@link AbstractJcrNode Node implementation} for the node given by the UUID of a reference node and a relative
     * path from the reference node. The relative path should already have been {@link Path#getNormalizedPath() normalized}.
     * 
     * @param uuidOfReferenceNode the UUID of the reference node; may be null if the root node is to be used as the reference
     * @param relativePath the relative (but normalized) path from the reference node, but which may be an absolute (and
     *        normalized) path only when the reference node is the root node; may not be null
     * @return the information for the referenced node; never null
     * @throws ItemNotFoundException if the reference node with the supplied UUID does not exist
     * @throws PathNotFoundException if the node given by the relative path does not exist
     * @throws RepositoryException if any other error occurs while reading information from the repository
     * @see #findNodeInfoForRoot()
     */
    public AbstractJcrNode findJcrNode( UUID uuidOfReferenceNode,
                                        Path relativePath ) throws PathNotFoundException, RepositoryException {
        // An existing JCR Node object was not found, so we'll have to create it by finding the underlying
        // NodeInfo for the node (from the changed state or the cache) ...
        NodeInfo info = findNodeInfo(uuidOfReferenceNode, relativePath);
        assert info != null;

        // Look for an existing node ...
        AbstractJcrNode node = jcrNodes.get(info.getUuid());
        if (node != null) return node;

        // Create the appropriate JCR Node object ...
        return createAndCacheJcrNodeFor(info);
    }

    public AbstractJcrProperty findJcrProperty( PropertyId propertyId ) throws PathNotFoundException, RepositoryException {
        AbstractJcrProperty property = jcrProperties.get(propertyId);
        if (property != null) return property;

        // An existing JCR Property object was not found, so we'll have to create it by finding the underlying
        // NodeInfo for the property's parent (from the changed state or the cache) ...
        PropertyInfo info = findPropertyInfo(propertyId); // throws PathNotFoundException if node not there
        if (info == null) return null; // no such property on this node

        // Now create the appropriate JCR Property object ...
        return createAndCacheJcrPropertyFor(info);
    }

    public Collection<AbstractJcrProperty> findJcrPropertiesFor( UUID nodeUuid )
        throws ItemNotFoundException, RepositoryException {
        NodeInfo info = findNodeInfo(nodeUuid);
        Map<Name, PropertyInfo> properties = info.getProperties();
        Collection<AbstractJcrProperty> result = new ArrayList<AbstractJcrProperty>(properties.size());
        for (PropertyInfo propertyInfo : properties.values()) {
            result.add(findJcrProperty(propertyInfo.getPropertyId()));
        }
        return result;
    }

    /**
     * Find the JCR {@link AbstractJcrItem Item implementation} for the node or property given by the UUID of a reference node and
     * a relative path from the reference node to the desired item. The relative path should already have been
     * {@link Path#getNormalizedPath() normalized}.
     * 
     * @param uuidOfReferenceNode the UUID of the reference node; may be null if the root node is to be used as the reference
     * @param relativePath the relative (but normalized) path from the reference node to the desired item, but which may be an
     *        absolute (and normalized) path only when the reference node is the root node; may not be null
     * @return the information for the referenced item; never null
     * @throws ItemNotFoundException if the reference node with the supplied UUID does not exist, or if an item given by the
     *         supplied relative path does not exist
     * @throws RepositoryException if any other error occurs while reading information from the repository
     * @see #findNodeInfoForRoot()
     */
    public AbstractJcrItem findJcrItem( UUID uuidOfReferenceNode,
                                        Path relativePath ) throws ItemNotFoundException, RepositoryException {
        // A pathological case is an empty relative path ...
        if (relativePath.size() == 0) {
            return findJcrNode(uuidOfReferenceNode);
        }
        if (relativePath.size() == 1) {
            Path.Segment segment = relativePath.getLastSegment();
            if (segment.isSelfReference()) return findJcrNode(uuidOfReferenceNode);
            if (segment.isParentReference()) {
                NodeInfo referencedNode = findNodeInfo(uuidOfReferenceNode);
                return findJcrNode(referencedNode.getParent());
            }
        }

        // Peek into the last segment of the path to see whether it uses a SNS index (and it's > 1) ...
        Path.Segment lastSegment = relativePath.getLastSegment();
        if (lastSegment.getIndex() > 1) {
            // Only nodes can have SNS index (but an index of 1 is the default)...
            return findJcrNode(uuidOfReferenceNode);
        }

        NodeInfo parent = null;
        if (relativePath.size() == 1) {
            // The referenced node must be the parent ...
            parent = findNodeInfo(uuidOfReferenceNode);
        } else {
            // We know that the parent of the referenced item should be a node (if the path is right) ...
            parent = findNodeInfo(uuidOfReferenceNode, relativePath.getParent());
        }

        // JSR-170 doesn't allow children and proeprties to have the same name, but this is relaxed in JSR-283.
        // But JSR-283 Section 3.3.4 states "The method Session.getItem will return the item at the specified path
        // if there is only one such item, if there is both a node and a property at the specified path, getItem
        // will return the node." Therefore, look for a child first ...
        ChildNode child = parent.getChildren().getChild(lastSegment);
        if (child != null) {
            return findJcrNode(child.getUuid());
        }

        // Otherwise it should be a property ...
        PropertyInfo propertyInfo = parent.getProperty(lastSegment.getName());
        if (propertyInfo != null) {
            return findJcrProperty(propertyInfo.getPropertyId());
        }

        // It was not found, so prepare a good exception message ...
        String msg = null;
        if (findNodeInfoForRoot().getUuid().equals(uuidOfReferenceNode)) {
            // The reference node was the root, so use this fact to convert the path to an absolute path in the message
            Path absolutePath = rootPath.resolve(relativePath);
            msg = JcrI18n.itemNotFoundAtPath.text(absolutePath, workspaceName);
        } else {
            // Find the path of the reference node ...
            Path referenceNodePath = getPathFor(uuidOfReferenceNode);
            msg = JcrI18n.itemNotFoundAtPathRelativeToReferenceNode.text(relativePath, referenceNodePath, workspaceName);
        }
        throw new ItemNotFoundException(msg);
    }

    /**
     * Utility method that creates and caches the appropriate kind of AbstractJcrNode implementation for node given by the
     * supplied information.
     * 
     * @param info the information for the node; may not be null
     * @return the <i>new</i> instance of the {@link Node}; never null
     */
    private AbstractJcrNode createAndCacheJcrNodeFor( NodeInfo info ) {
        UUID uuid = info.getUuid();
        Location location = info.getOriginalLocation();
        NodeDefinitionId nodeDefinitionId = info.getDefinitionId();
        JcrNodeDefinition definition = nodeTypes().getNodeDefinition(nodeDefinitionId);
        assert definition != null;

        if (root == null) {
            // Need to determine if this is the root node ...
            if (location.getPath().isRoot()) {
                // It is a root node ...
                JcrRootNode node = new JcrRootNode(this, uuid);
                jcrNodes.put(uuid, node);
                root = uuid;
                return node;
            }
        }

        // It is not a root node ...
        JcrNode node = new JcrNode(this, uuid);
        jcrNodes.put(uuid, node);
        return node;
    }

    /**
     * Utility method that creates and caches the appropriate kind of AbstractJcrProperty implementation for property given by the
     * supplied information.
     * 
     * @param info the information for the property; may not be null
     * @return the <i>new</i> instance of the {@link Property}; never null
     */
    private AbstractJcrProperty createAndCacheJcrPropertyFor( PropertyInfo info ) {
        boolean multiValued = info.getProperty().isMultiple();
        JcrPropertyDefinition definition = nodeTypes().getPropertyDefinition(info.getDefinitionId(), multiValued);
        assert definition != null;
        if (multiValued) {
            return new JcrMultiValueProperty(this, info.getPropertyId());
        }
        return new JcrSingleValueProperty(this, info.getPropertyId());
    }

    // public AbstractJcrProperty findJcrProperty( Path propertyPath ) throws PathNotFoundException, RepositoryException {
    // if (propertyPath.isRoot()) return null;
    // Path nodePath = propertyPath.getParent();
    // NodeInfo nodeInfo = findNodeInfo(nodePath);
    // Name propertyName = propertyPath.getLastSegment().getName();
    // PropertyInfo info = nodeInfo.getProperty(propertyName);
    //
    // // Look for an existing JCR Property object ...
    // PropertyId propertyId = info.getPropertyId();
    // AbstractJcrProperty property = jcrProperties.get(propertyId);
    // if (property != null) return property;
    //
    // // Now create the appropriate JCR Property object ...
    // return createAndCacheJcrPropertyFor(info);
    // }

    /**
     * Find the information for the node given by the UUID of the node. This is often the fastest way to find information,
     * especially after the information has been cached. Note, however, that this method first checks the cache, and if the
     * information is not in the cache, the information is read from the repository.
     * 
     * @param uuid the UUID for the node; may not be null
     * @return the information for the node with the supplied UUID, or null if the information is not in the cache
     * @throws ItemNotFoundException if there is no node with the supplied UUID
     * @throws RepositoryException if any other error occurs while reading information from the repository
     * @see #findNodeInfoInCache(UUID)
     * @see #findNodeInfo(UUID, Path)
     * @see #findNodeInfoForRoot()
     */
    NodeInfo findNodeInfo( UUID uuid ) throws ItemNotFoundException, RepositoryException {
        // See if we already have something in the changed nodes ...
        NodeInfo info = changedNodes.get(uuid);
        if (info == null) {
            // Or in the cache ...
            info = cachedNodes.get(uuid);
            if (info == null) {
                info = loadFromGraph(uuid, null);
            }
        }
        return info;
    }

    /**
     * Find the information for the node given by the UUID of the node. This is often the fastest way to find information,
     * especially after the information has been cached. Note, however, that this method only checks the cache.
     * 
     * @param uuid the UUID for the node; may not be null
     * @return the information for the node with the supplied UUID, or null if the information is not in the cache
     * @see #findNodeInfo(UUID)
     * @see #findNodeInfo(UUID, Path)
     * @see #findNodeInfoForRoot()
     */
    NodeInfo findNodeInfoInCache( UUID uuid ) {
        // See if we already have something in the changed nodes ...
        NodeInfo info = changedNodes.get(uuid);
        if (info == null) {
            // Or in the cache ...
            info = cachedNodes.get(uuid);
        }
        return info;
    }

    /**
     * Find the information for the root node. Generally, this returns information that's in the cache, except for the first time
     * the root node is needed.
     * 
     * @return the node information
     * @throws RepositoryException if there is an error while obtaining the information from the repository
     */
    NodeInfo findNodeInfoForRoot() throws RepositoryException {
        if (root == null) {
            // We haven't found the root yet ...
            NodeInfo info = loadFromGraph(this.rootPath, null);
            root = info.getUuid();
            this.jcrNodes.put(root, new JcrRootNode(this, root));
            return info;
        }
        return findNodeInfo(root);
    }

    /**
     * Find the information for the node given by the UUID of a reference node and a relative path from the reference node.
     * 
     * @param node the reference node; may be null if the root node is to be used as the reference
     * @param relativePath the relative path from the reference node, but which may be an absolute path only when the reference
     *        node is the root node; may not be null
     * @return the information for the referenced node; never null
     * @throws ItemNotFoundException if the reference node with the supplied UUID does not exist
     * @throws PathNotFoundException if the node given by the relative path does not exist
     * @throws RepositoryException if any other error occurs while reading information from the repository
     * @see #findNodeInfoForRoot()
     */
    NodeInfo findNodeInfo( UUID node,
                           Path relativePath ) throws ItemNotFoundException, PathNotFoundException, RepositoryException {
        // The relative path must be normalized ...
        assert relativePath.isNormalized();

        // Find the node from which we're starting ...
        NodeInfo fromInfo = null;
        if (node == null) {
            // This is only valid when the path is relative to the root (or it's an absolute path)
            fromInfo = findNodeInfoForRoot();
            node = fromInfo.getUuid();
        } else {
            fromInfo = findNodeInfo(node);
            assert relativePath.isAbsolute() ? node == root : true;
        }
        if (relativePath.isAbsolute()) {
            relativePath = relativePath.relativeTo(this.rootPath);
        }

        // If the relative path is of zero-length ...
        if (relativePath.size() == 0) {
            return fromInfo;
        }
        // Or it is of length 1 but it is a self reference ...
        if (relativePath.size() == 1 && relativePath.getLastSegment().isSelfReference()) {
            return fromInfo;
        }

        // TODO: This could be more efficient than always walking the path. For example, we could
        // maintain a cache of paths. Right now, we are walking as much of the path as we can,
        // but as soon as we reach the bottom-most cached/changed node, we need to read the rest
        // from the graph. We are figuring out all of the remaining nodes and read them from
        // the graph in one batch operation, so that part is pretty good.

        // Now, walk the path to find the nodes, being sure to look for changed information ...
        NodeInfo info = fromInfo;
        Iterator<Path.Segment> pathsIter = relativePath.iterator();
        while (pathsIter.hasNext()) {
            Path.Segment child = pathsIter.next();
            if (child.isParentReference()) {
                // Walk up ...
                UUID parentUuid = info.getParent();
                if (parentUuid == null) {
                    assert info.getUuid() == findNodeInfoForRoot().getUuid();
                    String msg = JcrI18n.errorWhileFindingNodeWithPath.text(relativePath, workspaceName);
                    throw new PathNotFoundException(msg);
                }
                info = findNodeInfo(parentUuid);
            } else {
                // Walk down ...
                // Note that once we start walking down, a normalized path should never have any more parent
                // or self references
                ChildNode childNodeInfo = info.getChildren().getChild(child);
                if (childNodeInfo == null) {
                    // The node (no longer?) exists, so compute the
                    Path fromPath = getPathFor(fromInfo);
                    String msg = JcrI18n.pathNotFoundRelativeTo.text(relativePath, fromPath, workspaceName);
                    throw new PathNotFoundException(msg);
                }
                // See if we already have something in the changed nodes ...
                UUID uuid = childNodeInfo.getUuid();
                NodeInfo childInfo = changedNodes.get(uuid);
                if (childInfo == null) {
                    // Or in the cache ...
                    childInfo = cachedNodes.get(uuid);
                    if (childInfo == null) {
                        // At this point, we've reached the bottom of the nodes that we have locally.
                        // Get the actual location of the last 'info', since all paths will be relative to it...
                        Location actualLocation = info.getOriginalLocation();
                        Path actualPath = actualLocation.getPath();
                        Path nextPath = pathFactory.create(actualPath, child);
                        if (pathsIter.hasNext()) {
                            // There are multiple remaining paths, so load them all in one batch operation,
                            // starting at the top-most path (the one we're currently at)...
                            List<Path> pathsInBatch = new LinkedList<Path>();
                            Results batchResults = null;
                            try {
                                Graph.Batch batch = store.batch();
                                batch.read(nextPath);
                                pathsInBatch.add(nextPath);
                                while (pathsIter.hasNext()) {
                                    child = pathsIter.next();
                                    nextPath = pathFactory.create(nextPath, child);
                                    batch.read(nextPath);
                                    pathsInBatch.add(nextPath);
                                }
                                batchResults = batch.execute();
                            } catch (org.jboss.dna.graph.property.PathNotFoundException e) {
                                Path fromPath = getPathFor(fromInfo);
                                throw new PathNotFoundException(JcrI18n.pathNotFoundRelativeTo.text(relativePath,
                                                                                                    fromPath,
                                                                                                    workspaceName));
                            } catch (RepositorySourceException e) {
                                throw new RepositoryException(JcrI18n.errorWhileFindingNodeWithUuid.text(uuid,
                                                                                                         workspaceName,
                                                                                                         e.getLocalizedMessage()));
                            }
                            // Now process all of the nodes that we loaded, again starting at the top and going down ...
                            for (Path batchPath : pathsInBatch) {
                                org.jboss.dna.graph.Node dnaNode = batchResults.getNode(batchPath);
                                childInfo = createNodeInfoFrom(dnaNode, info);
                                this.cachedNodes.put(childInfo.getUuid(), childInfo);
                                info = childInfo;
                            }
                        } else {
                            // This is the last path, so do it a little more efficiently than above ...
                            childInfo = loadFromGraph(nextPath, info);
                            info = childInfo;
                        }
                    } else {
                        info = childInfo;
                    }
                } else {
                    info = childInfo;
                }
            }
        }
        return info;
    }

    /**
     * Find the property information with the given identifier. If the property is not yet loaded into the cache, the node (and
     * its properties) will be read from the repository.
     * 
     * @param propertyId the identifier for the property; may not be null
     * @return the property information, or null if the node does not contain the specified property
     * @throws PathNotFoundException if the node containing this property does not exist
     * @throws RepositoryException if there is an error while obtaining the information
     */
    PropertyInfo findPropertyInfo( PropertyId propertyId ) throws PathNotFoundException, RepositoryException {
        NodeInfo info = findNodeInfo(propertyId.getNodeId());
        return info.getProperty(propertyId.getPropertyName());
    }

    Path getPathFor( UUID uuid ) throws ItemNotFoundException, RepositoryException {
        if (uuid == root) return rootPath;
        return getPathFor(findNodeInfo(uuid));
    }

    Path getPathFor( NodeInfo info ) throws ItemNotFoundException, RepositoryException {
        if (info != null && info.getUuid() == root) return rootPath;
        LinkedList<Path.Segment> segments = new LinkedList<Path.Segment>();
        while (info != null) {
            UUID parent = info.getParent();
            if (parent == null) break;
            NodeInfo parentInfo = findNodeInfo(parent);
            ChildNode child = parentInfo.getChildren().getChild(info.getUuid());
            if (child == null) break;
            Path.Segment segment = pathFactory.createSegment(child.getName(), child.getSnsIndex());

            segments.addFirst(segment);
            info = parentInfo;
        }
        return pathFactory.createAbsolutePath(segments);
    }

    Path getPathFor( PropertyInfo propertyInfo ) throws ItemNotFoundException, RepositoryException {
        Path nodePath = getPathFor(propertyInfo.getNodeUuid());
        return pathFactory.create(nodePath, propertyInfo.getPropertyName());
    }

    Path getPathFor( PropertyId propertyId ) throws ItemNotFoundException, RepositoryException {
        return getPathFor(findPropertyInfo(propertyId));
    }

    protected Name getNameOf( UUID nodeUuid ) throws ItemNotFoundException, RepositoryException {
        findNodeInfoForRoot();
        if (nodeUuid == root) return nameFactory.create("");
        // Get the parent ...
        NodeInfo info = findNodeInfo(nodeUuid);
        NodeInfo parent = findNodeInfo(info.getParent());
        ChildNode child = parent.getChildren().getChild(info.getUuid());
        return child.getName();
    }

    protected int getSnsIndexOf( UUID nodeUuid ) throws ItemNotFoundException, RepositoryException {
        findNodeInfoForRoot();
        if (nodeUuid == root) return 1;
        // Get the parent ...
        NodeInfo info = findNodeInfo(nodeUuid);
        NodeInfo parent = findNodeInfo(info.getParent());
        ChildNode child = parent.getChildren().getChild(info.getUuid());
        return child.getSnsIndex();
    }

    /**
     * Load from the underlying repository graph the information for the node with the supplied UUID. This method returns the
     * information for the requested node (after placing it in the cache), but this method may (at its discretion) also load and
     * cache information for other nodes.
     * <p>
     * Note that this method does not check the cache before loading from the repository graph.
     * </p>
     * 
     * @param uuid the UUID of the node; may not be null
     * @param parentInfo the parent information; may be null if not known
     * @return the information for the node
     * @throws ItemNotFoundException if the node does not exist in the repository
     * @throws RepositoryException if there was an error obtaining this information from the repository
     */
    protected NodeInfo loadFromGraph( UUID uuid,
                                      NodeInfo parentInfo ) throws ItemNotFoundException, RepositoryException {
        // Load the node information from the store ...
        try {
            org.jboss.dna.graph.Node node = store.getNodeAt(uuid);
            NodeInfo info = createNodeInfoFrom(node, parentInfo);
            this.cachedNodes.put(info.getUuid(), info);
            return info;
        } catch (org.jboss.dna.graph.property.PathNotFoundException e) {
            throw new ItemNotFoundException(JcrI18n.itemNotFoundWithUuid.text(uuid, workspaceName, e.getLocalizedMessage()));
        } catch (RepositorySourceException e) {
            throw new RepositoryException(
                                          JcrI18n.errorWhileFindingNodeWithUuid.text(uuid, workspaceName, e.getLocalizedMessage()));
        }
    }

    /**
     * Load from the underlying repository graph the information for the node with the supplied UUID. This method returns the
     * information for the requested node (after placing it in the cache), but this method may (at its discretion) also load and
     * cache information for other nodes.
     * <p>
     * Note that this method does not check the cache before loading from the repository graph.
     * </p>
     * 
     * @param path the path to the node; may not be null
     * @param parentInfo the parent information; may be null if not known
     * @return the information for the node
     * @throws PathNotFoundException if the node does not exist in the repository
     * @throws RepositoryException if there was an error obtaining this information from the repository
     */
    protected NodeInfo loadFromGraph( Path path,
                                      NodeInfo parentInfo ) throws PathNotFoundException, RepositoryException {
        // Load the node information from the store ...
        try {
            org.jboss.dna.graph.Node node = store.getNodeAt(path);
            NodeInfo info = createNodeInfoFrom(node, parentInfo);
            this.cachedNodes.put(info.getUuid(), info);
            return info;
        } catch (org.jboss.dna.graph.property.PathNotFoundException e) {
            throw new PathNotFoundException(JcrI18n.pathNotFound.text(path, workspaceName));
        } catch (RepositorySourceException e) {
            throw new RepositoryException(JcrI18n.errorWhileFindingNodeWithPath.text(path, workspaceName));
        }
    }

    /**
     * Create the {@link NodeInfo} object given the DNA graph node and the parent node information (if it is available).
     * 
     * @param graphNode the DNA graph node; may not be null
     * @param parentInfo the information for the parent node, or null if the supplied graph node represents the root node, or if
     *        the parent information is not known
     * @return the node information; never null
     * @throws RepositoryException if there is an error determining the child {@link NodeDefinition} for the supplied node,
     *         preventing the node information from being constructed
     */
    private NodeInfo createNodeInfoFrom( org.jboss.dna.graph.Node graphNode,
                                         NodeInfo parentInfo ) throws RepositoryException {
        // Now get the DNA node's UUID and find the DNA property containing the UUID ...
        Location location = graphNode.getLocation();
        ValueFactories factories = context.getValueFactories();
        UUID uuid = location.getUuid();
        org.jboss.dna.graph.property.Property uuidProperty = null;
        if (uuid != null) {
            // Check for an identification property ...
            uuidProperty = location.getIdProperty(JcrLexicon.UUID);
            if (uuidProperty == null) uuidProperty = location.getIdProperty(DnaLexicon.UUID);
        }
        if (uuidProperty == null) {
            uuidProperty = graphNode.getProperty(JcrLexicon.UUID);
            if (uuidProperty != null) {
                // Grab the first 'good' UUID value ...
                for (Object uuidValue : uuidProperty) {
                    try {
                        uuid = factories.getUuidFactory().create(uuidValue);
                        break;
                    } catch (ValueFormatException e) {
                        // Ignore; just continue with the next property value
                    }
                }
            }
            if (uuid == null) {
                // Look for the DNA UUID property ...
                org.jboss.dna.graph.property.Property dnaUuidProperty = graphNode.getProperty(DnaLexicon.UUID);
                if (dnaUuidProperty != null) {
                    // Grab the first 'good' UUID value ...
                    for (Object uuidValue : dnaUuidProperty) {
                        try {
                            uuid = factories.getUuidFactory().create(uuidValue);
                            break;
                        } catch (ValueFormatException e) {
                            // Ignore; just continue with the next property value
                        }
                    }
                }
            }
        }
        if (uuid == null) uuid = UUID.randomUUID();
        if (uuidProperty == null) uuidProperty = propertyFactory.create(JcrLexicon.UUID, uuid);

        // Either the UUID is not known, or there was no node. Either way, we have to create the node ...
        if (uuid == null) uuid = UUID.randomUUID();

        // Look for the primary type of the node ...
        Map<Name, Property> graphProperties = graphNode.getPropertiesByName();
        final boolean isRoot = location.getPath().isRoot();
        Name primaryTypeName = null;
        org.jboss.dna.graph.property.Property primaryTypeProperty = graphNode.getProperty(JcrLexicon.PRIMARY_TYPE);
        if (primaryTypeProperty != null && !primaryTypeProperty.isEmpty()) {
            try {
                primaryTypeName = factories.getNameFactory().create(primaryTypeProperty.getFirstValue());
            } catch (ValueFormatException e) {
                // use the default ...
            }
        }
        if (primaryTypeName == null) {
            // We have to have a primary type, so use the default ...
            if (isRoot) {
                primaryTypeName = DnaLexicon.ROOT;
                primaryTypeProperty = propertyFactory.create(JcrLexicon.PRIMARY_TYPE, primaryTypeName);
            } else {
                primaryTypeName = defaultPrimaryTypeName;
                primaryTypeProperty = defaultPrimaryTypeProperty;
            }
            // We have to add this property to the graph node...
            graphProperties = new HashMap<Name, Property>(graphProperties);
            graphProperties.put(primaryTypeProperty.getName(), primaryTypeProperty);
        }
        assert primaryTypeProperty != null;
        assert primaryTypeProperty.isEmpty() == false;

        // Look for a node definition stored on the node ...
        JcrNodeDefinition definition = null;
        org.jboss.dna.graph.property.Property nodeDefnProperty = graphProperties.get(DnaLexicon.NODE_DEFINITON);
        if (nodeDefnProperty != null && !nodeDefnProperty.isEmpty()) {
            String nodeDefinitionString = stringFactory.create(nodeDefnProperty.getFirstValue());
            NodeDefinitionId id = NodeDefinitionId.fromString(nodeDefinitionString, nameFactory);
            definition = nodeTypes().getNodeDefinition(id);
        }
        // Figure out the node definition for this node ...
        if (definition == null) {
            if (isRoot) {
                definition = nodeTypes().getRootNodeDefinition();
            } else {
                // We need the parent ...
                Path path = location.getPath();
                if (parentInfo == null) {
                    Path parentPath = path.getParent();
                    parentInfo = findNodeInfo(null, parentPath.getNormalizedPath());
                }
                Name childName = path.getLastSegment().getName();
                definition = findNodeDefinitionForChild(parentInfo, childName, primaryTypeName);
                if (definition == null) {
                    String msg = JcrI18n.nodeDefinitionCouldNotBeDeterminedForNode.text(path, workspaceName);
                    throw new RepositorySourceException(msg);
                }
            }
        }

        // Create the node information ...
        NodeInfo info = new NodeInfo(location, primaryTypeName, definition.getId());
        if (parentInfo != null) {
            info.setParent(parentInfo.getUuid());
        }

        // --------------------------------------------------
        // Set the node's children ...
        // --------------------------------------------------
        info.setChildren(graphNode.getChildren());

        // ------------------------------------------------------
        // Set the node's properties ...
        // ------------------------------------------------------
        // First get the property type for each property, based upon the primary type and mixins ...
        // The map with single-valued properties...
        Map<Name, PropertyDefinition> svPropertyDefinitionsByPropertyName = new HashMap<Name, PropertyDefinition>();
        // ... and the map with multi-valued properties
        Map<Name, PropertyDefinition> mvPropertyDefinitionsByPropertyName = new HashMap<Name, PropertyDefinition>();

        boolean referenceable = false;

        List<PropertyDefinition> anyPropertyDefinitions = new LinkedList<PropertyDefinition>();
        // Start with the primary type ...
        NodeType primaryType = nodeTypes().getNodeType(primaryTypeName);
        for (PropertyDefinition propertyDefn : primaryType.getPropertyDefinitions()) {
            String nameString = propertyDefn.getName();
            if ("*".equals(nameString)) {
                anyPropertyDefinitions.add(propertyDefn);
                continue;
            }
            Name name = nameFactory.create(nameString);

            if (propertyDefn.isMultiple()) {
                PropertyDefinition prev = mvPropertyDefinitionsByPropertyName.put(name, propertyDefn);
                if (prev != null) mvPropertyDefinitionsByPropertyName.put(name, prev); // put the first one back ...
            } else {
                PropertyDefinition prev = svPropertyDefinitionsByPropertyName.put(name, propertyDefn);
                if (prev != null) svPropertyDefinitionsByPropertyName.put(name, prev); // put the first one back ...
            }
        }
        // The process the mixin types ...
        org.jboss.dna.graph.property.Property mixinTypesProperty = graphProperties.get(JcrLexicon.MIXIN_TYPES);
        if (mixinTypesProperty != null && !mixinTypesProperty.isEmpty()) {
            for (Object mixinTypeValue : mixinTypesProperty) {
                Name mixinTypeName = nameFactory.create(mixinTypeValue);
                if (!referenceable && JcrMixLexicon.REFERENCEABLE.equals(mixinTypeName)) referenceable = true;
                String mixinTypeNameString = mixinTypeName.getString(namespaces);
                NodeType mixinType = nodeTypes().getNodeType(mixinTypeNameString);
                for (PropertyDefinition propertyDefn : mixinType.getPropertyDefinitions()) {
                    String nameString = propertyDefn.getName();
                    if ("*".equals(nameString)) {
                        anyPropertyDefinitions.add(propertyDefn);
                        continue;
                    }
                    Name name = nameFactory.create(nameString);
                    if (propertyDefn.isMultiple()) {
                        PropertyDefinition prev = mvPropertyDefinitionsByPropertyName.put(name, propertyDefn);
                        if (prev != null) mvPropertyDefinitionsByPropertyName.put(name, prev); // put the first one back ...
                    } else {
                        PropertyDefinition prev = svPropertyDefinitionsByPropertyName.put(name, propertyDefn);
                        if (prev != null) svPropertyDefinitionsByPropertyName.put(name, prev); // put the first one back ...
                    }
                }
            }
        }

        // Now create the JCR property object wrappers around the other properties ...
        for (org.jboss.dna.graph.property.Property dnaProp : graphProperties.values()) {
            Name name = dnaProp.getName();

            // Figure out the JCR property type for this property ...
            PropertyDefinition propertyDefinition;
            if (dnaProp.isMultiple()) {
                propertyDefinition = mvPropertyDefinitionsByPropertyName.get(name);
            } else {
                propertyDefinition = svPropertyDefinitionsByPropertyName.get(name);

                // If the property has only one value, dnaProp.isMultiple() will return false, but the
                // property may actually be a multi-valued property that happens to have one property set.
                if (propertyDefinition == null) {
                    propertyDefinition = mvPropertyDefinitionsByPropertyName.get(name);
                }
            }

            // If no property type was found for this property, see if there is a wildcard property ...
            if (propertyDefinition == null) {
                for (Iterator<PropertyDefinition> iter = anyPropertyDefinitions.iterator(); iter.hasNext();) {
                    PropertyDefinition nextDef = iter.next();

                    // Grab the first residual definition that matches on cardinality (single-valued vs. multi-valued)
                    if ((nextDef.isMultiple() && dnaProp.isMultiple()) || (!nextDef.isMultiple() && !dnaProp.isMultiple())) {
                        propertyDefinition = nextDef;
                        break;
                    }
                }
            }

            // If there still is no property type defined ...
            if (propertyDefinition == null) {
                assert anyPropertyDefinitions.isEmpty();
                if (INCLUDE_PROPERTIES_NOT_ALLOWED_BY_NODE_TYPE_OR_MIXINS) {
                    // We can use the "nt:unstructured" property definitions for any property ...
                    NodeType unstructured = nodeTypes().getNodeType(JcrNtLexicon.UNSTRUCTURED);
                    for (PropertyDefinition anyDefinition : unstructured.getDeclaredPropertyDefinitions()) {
                        if (anyDefinition.isMultiple()) {
                            propertyDefinition = anyDefinition;
                            break;
                        }
                    }
                }
            }
            if (propertyDefinition == null) {
                // We're supposed to skip this property (since we don't have a definition for it) ...
                continue;
            }

            // Figure out if this is a multi-valued property ...
            boolean isMultiple = propertyDefinition.isMultiple();
            if (!isMultiple && dnaProp.isEmpty()) {
                // Only multi-valued properties can have no values; so if not multi-valued, then skip ...
                continue;
            }

            // Figure out the property type ...
            int propertyType = propertyDefinition.getRequiredType();
            if (propertyType == PropertyType.UNDEFINED) {
                propertyType = JcrSession.jcrPropertyTypeFor(dnaProp);
            }

            // Record the property in the node information ...
            PropertyId propId = new PropertyId(uuid, name);
            info.setProperty(propId, (JcrPropertyDefinition)propertyDefinition, propertyType, dnaProp);
        }

        // Now add the "jcr:uuid" property if and only if referenceable ...
        if (referenceable) {
            // We know that this property is single-valued
            PropertyDefinition propertyDefinition = svPropertyDefinitionsByPropertyName.get(JcrLexicon.UUID);
            PropertyId propId = new PropertyId(uuid, JcrLexicon.UUID);
            info.setProperty(propId, (JcrPropertyDefinition)propertyDefinition, PropertyType.STRING, uuidProperty);
        } else {
            // Make sure there is NOT a "jcr:uuid" property ...
            info.removeProperty(JcrLexicon.UUID);
        }
        // Make sure the "dna:uuid" property did not get in there ...
        info.removeProperty(DnaLexicon.UUID);

        return info;
    }

    /**
     * Utility method to find the {@link NodeDefinition} for a child node with the supplied {@link Name name}, parent information,
     * and the primary node type of the child.
     * 
     * @param parentInfo the parent information; may not be null
     * @param childName the name of the child node (without any same-name-sibling index); may not be null
     * @param primaryTypeOfChild the name of the child's primary type
     * @return the node definition for this child, as best as can be determined, or null if the node definition could not be
     *         determined
     * @throws RepositoryException if the parent's pimary node type cannot be found in the {@link NodeTypeManager}
     */
    protected JcrNodeDefinition findNodeDefinitionForChild( NodeInfo parentInfo,
                                                            Name childName,
                                                            Name primaryTypeOfChild ) throws RepositoryException {
        // Get the primary type of the parent, and look at it's child definitions ...
        Name primaryTypeName = parentInfo.getPrimaryTypeName();
        JcrNodeType primaryType = nodeTypes().getNodeType(primaryTypeName);
        if (primaryType == null) {
            String msg = JcrI18n.missingNodeTypeForExistingNode.text(primaryTypeName, parentInfo.getUuid(), workspaceName);
            throw new RepositoryException(msg);
        }
        // TODO: should this also check the mixins?
        return primaryType.findBestNodeDefinitionForChild(childName, primaryTypeOfChild);
    }

    /**
     * The information that describes a node. This is the information that is kept in the {@link SessionCache#cachedNodes cache}
     * and in the record of {@link SessionCache#changedNodes changes} made by the session but not yet commited/saved.
     */
    @NotThreadSafe
    protected class NodeInfo {
        private final Location originalLocation;
        private final UUID uuid;
        private UUID parent;
        private final Name primaryTypeName;
        private final NodeDefinitionId definition;
        private final Children children;
        private final Map<Name, PropertyInfo> properties;

        protected NodeInfo( Location originalLocation,
                            Name primaryTypeName,
                            NodeDefinitionId definition ) {
            this.originalLocation = originalLocation;
            this.primaryTypeName = primaryTypeName;
            this.definition = definition;
            this.uuid = this.originalLocation.getUuid();
            this.children = new Children(this.uuid);
            this.properties = new HashMap<Name, PropertyInfo>();
            assert this.uuid != null;
            assert this.definition != null;
            assert this.primaryTypeName != null;
        }

        /**
         * @return location
         */
        public Location getOriginalLocation() {
            return originalLocation;
        }

        /**
         * @return uuid
         */
        public UUID getUuid() {
            return uuid;
        }

        /**
         * @return parent
         */
        public UUID getParent() {
            return parent;
        }

        /**
         * @param parent Sets parent to the specified value.
         */
        protected void setParent( UUID parent ) {
            this.parent = parent;
        }

        /**
         * @return primaryTypeName
         */
        public Name getPrimaryTypeName() {
            return primaryTypeName;
        }

        /**
         * @return definition
         */
        public NodeDefinitionId getDefinitionId() {
            return definition;
        }

        /**
         * Get the children for this node.
         * 
         * @return the children; never null but possibly empty
         */
        public Children getChildren() {
            return children;
        }

        /**
         * @param children Sets children to the specified value.
         * @return the children information; never null
         */
        public Children setChildren( List<Location> children ) {
            this.children.append(SessionCache.this, children, true);
            return this.children;
        }

        /**
         * Get the map of information for each property.
         * 
         * @return the information for each property; never null but possibly (though unlikely) empty
         */
        public Map<Name, PropertyInfo> getProperties() {
            return this.properties; // never null
        }

        public PropertyInfo getProperty( Name name ) {
            return this.properties.get(name);
        }

        public PropertyInfo setProperty( PropertyId id,
                                         JcrPropertyDefinition definition,
                                         int propertyType,
                                         Property dnaProperty ) {
            // Initialize the map if required (this never replaces it, though) ...
            PropertyInfo info = new PropertyInfo(id, definition.getId(), propertyType, dnaProperty, definition.isMultiple());
            return this.properties.put(id.getPropertyName(), info);
        }

        public PropertyInfo removeProperty( Name name ) {
            return this.properties.remove(name);
        }
    }

    /**
     * An immutable representation of the name and current value(s) for a property, along with the JCR metadata for the property,
     * including the {@link PropertyInfo#getDefinitionId() property definition} and {@link PropertyInfo#getPropertyType() property
     * type}.
     * <p>
     * This class is immutable, which means that clients should never hold onto an instance. Instead, clients can obtain an
     * instance by using a {@link PropertyId}, quickly use the information in the instance, and then immediately discard their
     * reference. This is because these instances are replaced and discarded whenever anything about the property changes.
     * </p>
     */
    @Immutable
    public static class PropertyInfo {
        private final PropertyId propertyId;
        private final PropertyDefinitionId definitionId;
        private final Property dnaProperty;
        private final int propertyType;
        private final boolean multiValued;

        protected PropertyInfo( PropertyId propertyId,
                                PropertyDefinitionId definitionId,
                                int propertyType,
                                Property dnaProperty,
                                boolean multiValued ) {
            this.propertyId = propertyId;
            this.definitionId = definitionId;
            this.propertyType = propertyType;
            this.dnaProperty = dnaProperty;
            this.multiValued = multiValued;
        }

        /**
         * Get the durable identifier for this property.
         * 
         * @return propertyId
         */
        public PropertyId getPropertyId() {
            return propertyId;
        }

        /**
         * Get the UUID of the node to which this property belongs.
         * 
         * @return the owner node's UUID; never null
         */
        public UUID getNodeUuid() {
            return propertyId.getNodeId();
        }

        /**
         * The identifier for the property definition.
         * 
         * @return the property definition ID; never null
         */
        public PropertyDefinitionId getDefinitionId() {
            return definitionId;
        }

        /**
         * Get the DNA Property, which contains the name and value(s)
         * 
         * @return the property; never null
         */
        public Property getProperty() {
            return dnaProperty;
        }

        /**
         * Get the property name.
         * 
         * @return the property name; never null
         */
        public Name getPropertyName() {
            return dnaProperty.getName();
        }

        /**
         * Get the JCR {@link PropertyType} for this property.
         * 
         * @return the property type
         */
        public int getPropertyType() {
            return propertyType;
        }

        /**
         * @return multiValued
         */
        public boolean isMultiValued() {
            return multiValued;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return propertyId.hashCode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof PropertyInfo) {
                return propertyId.equals(((PropertyInfo)obj).getPropertyId());
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(propertyId);
            sb.append(" defined by ").append(definitionId);
            sb.append(" of type ").append(PropertyType.nameFromValue(propertyType));
            if (dnaProperty.isSingle()) {
                sb.append(" with value ");
            } else {
                sb.append(" with values ");
            }
            sb.append(dnaProperty.getValuesAsArray());
            return sb.toString();
        }
    }

    /**
     * Class that maintains the ordered list of {@link ChildNode} instances. This class uses a {@link ListMultimap} to maintain
     * insertion order of the child nodes, and to allow fast access to the children with a specified name.
     */
    @ThreadSafe
    public final static class Children implements Iterable<ChildNode> {
        private final UUID parentUuid;
        private final Map<UUID, ChildNode> childrenByUuid;
        private final ListMultimap<Name, ChildNode> childrenByName;
        private final ReadWriteLock lock = new ReentrantReadWriteLock();

        Children( UUID parentUuid ) {
            this.parentUuid = parentUuid;
            this.childrenByUuid = new HashMap<UUID, ChildNode>();
            this.childrenByName = new LinkedListMultimap<Name, ChildNode>();
        }

        /**
         * Get the number of children.
         * 
         * @return the number of children
         */
        public int size() {
            try {
                lock.readLock().lock();
                return childrenByName.size();
            } finally {
                lock.readLock().unlock();
            }
        }

        /**
         * Get the children.
         * 
         * @return a copy of the list of children
         */
        public List<ChildNode> asList() {
            try {
                lock.readLock().lock();
                return new ArrayList<ChildNode>(childrenByName.values());
            } finally {
                lock.readLock().unlock();
            }
        }

        /**
         * Get the children.
         * 
         * @return a copy of the list of children
         */
        public Iterator<ChildNode> iterator() {
            return asList().iterator();
        }

        /**
         * The UUID of the parent node.
         * 
         * @return the parent node's UUID
         */
        public UUID getParentUuid() {
            return parentUuid;
        }

        /**
         * Get the child with the given UUID.
         * 
         * @param uuid the UUID of the child node
         * @return the child node, or null if there is no child with the supplied UUID
         */
        public ChildNode getChild( UUID uuid ) {
            try {
                lock.readLock().lock();
                return this.childrenByUuid.get(uuid);
            } finally {
                lock.readLock().unlock();
            }
        }

        /**
         * Get the child given the path segment.
         * 
         * @param segment the path segment for the child, which includes the {@link Path.Segment#getName() name} and
         *        {@link Path.Segment#getIndex() one-based same-name-sibling index}; may not be null
         * @return the information for the child node, or null if no such child existed
         */
        public ChildNode getChild( Path.Segment segment ) {
            try {
                lock.readLock().lock();
                List<ChildNode> childrenWithName = this.childrenByName.get(segment.getName());
                int snsIndex = segment.getIndex();
                if (childrenWithName.size() < snsIndex) return null;
                return childrenWithName.get(snsIndex - 1);
            } finally {
                lock.readLock().unlock();
            }
        }

        /**
         * Get the same-name-sibling children that all share the supplied name.
         * 
         * @param name the name for the children; may not be null
         * @return the children with the supplied name; never null
         */
        public List<ChildNode> getChildren( Name name ) {
            try {
                lock.readLock().lock();
                return new ArrayList<ChildNode>(this.childrenByName.get(name));
            } finally {
                lock.readLock().unlock();
            }
        }

        /**
         * Remove the child with the given path segment.
         * 
         * @param segment the path segment for the child, which includes the {@link Path.Segment#getName() name} and
         *        {@link Path.Segment#getIndex() one-based same-name-sibling index}; may not be null
         * @return the information for the child node that was removed, or null if no such child existed
         */
        public ChildNode remove( Path.Segment segment ) {
            try {
                lock.writeLock().lock();
                List<ChildNode> childrenWithName = this.childrenByName.get(segment.getName());
                int snsIndex = segment.getIndex();
                int numChildrenWithName = childrenWithName.size();
                if (numChildrenWithName < snsIndex) return null;
                ChildNode result = childrenWithName.remove(snsIndex);
                this.childrenByUuid.remove(result.getUuid());
                --numChildrenWithName;
                if (numChildrenWithName > snsIndex) {
                    // We need to reduce the SNS index of every child after the one that was just removed ...
                    ListIterator<ChildNode> siblingIter = childrenWithName.listIterator(snsIndex);
                    while (siblingIter.hasNext()) {
                        // Remove the next child and replace with one having the correct SNS index ...
                        ChildNode next = siblingIter.next();
                        siblingIter.remove();
                        siblingIter.set(next.withChangedSnsIndex(-1));
                    }
                }
                return result;
            } finally {
                lock.writeLock().unlock();
            }
        }

        /**
         * Add a new child to the end of the list of existing children a new child with the supplied name and UUID. The
         * same-name-sibling will be determined to be one more than the number of existing children with the same name.
         * 
         * @param cache the cache; may not be null
         * @param name the name for the child to be appended; may not be null
         * @param childUuid the UUID of the child; may not be null
         * @return the information for the newly-added child; never null
         */
        public ChildNode append( SessionCache cache,
                                 Name name,
                                 UUID childUuid ) {
            try {
                lock.writeLock().lock();
                List<ChildNode> childrenWithName = this.childrenByName.get(name);
                ChildNode child = new ChildNode(childUuid, name, childrenWithName.size() + 1);
                childrenWithName.add(child);
                this.childrenByUuid.put(childUuid, child);
                // Look for the child in the cache/changed nodes ...
                NodeInfo childInfo = cache.findNodeInfoInCache(childUuid);
                if (childInfo != null) childInfo.setParent(parentUuid);
                return child;
            } finally {
                lock.writeLock().unlock();
            }
        }

        /**
         * Append the children described by the supplied Location objects, optionally removing all existing children first.
         * 
         * @param cache the cache; may not be null
         * @param children a list containing a Location object for each child
         * @param removeExistingFirst true if the existing children should be removed before these children are added, or false if
         *        these children should be appended to the existing children
         */
        public void append( SessionCache cache,
                            List<Location> children,
                            boolean removeExistingFirst ) {
            try {
                lock.writeLock().lock();
                if (removeExistingFirst && !this.childrenByName.isEmpty()) {
                    for (ChildNode child : this.childrenByName.values()) {
                        // Look for the child in the cache/changed nodes ...
                        NodeInfo childInfo = cache.findNodeInfoInCache(child.getUuid());
                        if (childInfo != null) childInfo.setParent(null);
                        // TODO: These nodes are deleted and should be handled as deletes ...
                    }
                    this.childrenByUuid.clear();
                    this.childrenByName.clear();
                }
                for (Location childLocation : children) {
                    UUID childUuid = childLocation.getUuid();
                    Path.Segment segment = childLocation.getPath().getLastSegment();
                    Name name = segment.getName();
                    ChildNode child = new ChildNode(childUuid, name, segment.getIndex());
                    this.childrenByName.put(name, child);
                    this.childrenByUuid.put(childUuid, child);
                    // Look for the child in the cache/changed nodes ...
                    NodeInfo childInfo = cache.findNodeInfoInCache(childUuid);
                    if (childInfo != null) childInfo.setParent(parentUuid);
                }
            } finally {
                lock.writeLock().unlock();
            }
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            try {
                lock.readLock().lock();
                boolean first = true;
                for (ChildNode child : childrenByName.values()) {
                    if (!first) sb.append(", ");
                    else first = false;
                    sb.append(child.getName()).append('[').append(child.getSnsIndex()).append(']');
                }
            } finally {
                lock.readLock().unlock();
            }
            return sb.toString();
        }
    }

    /**
     * The information about a child node. This is designed to be found in the {@link Children}, used quickly, and discarded.
     * Clients should not hold on to these objects, since any changes to the children involve discarding the old ChildNode objects
     * and replacing them with new instances.
     */
    @Immutable
    public final static class ChildNode {
        private final UUID uuid;
        private final Name name;
        private final int snsIndex;

        protected ChildNode( UUID uuid,
                             Name name,
                             int snsIndex ) {
            this.uuid = uuid;
            this.name = name;
            this.snsIndex = snsIndex;
            assert this.snsIndex > 0;
        }

        /**
         * Get the UUID of the node.
         * 
         * @return the node's UUID; never null
         */
        public UUID getUuid() {
            return uuid;
        }

        /**
         * Get the name of the node.
         * 
         * @return the node's current name; never null
         */
        public Name getName() {
            return name;
        }

        /**
         * Get the same-name-sibling index of the node.
         * 
         * @return the node's SNS index; always positive
         */
        public int getSnsIndex() {
            return snsIndex;
        }

        /**
         * Return a new child node that has a changed SNS index.
         * 
         * @param delta the amount the change the SNS index, either positive to increase the value or negative to decrease the
         *        value
         * @return the copy of this, with a changed SNS index; never null
         */
        public ChildNode withChangedSnsIndex( int delta ) {
            return new ChildNode(uuid, name, snsIndex + delta);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return uuid.hashCode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof ChildNode) {
                return this.uuid.equals(((ChildNode)obj).uuid);
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return uuid.toString() + " ( " + name + "[" + snsIndex + "] )";
        }
    }

}
