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
package org.modeshape.jcr;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.jcr.AccessDeniedException;
import javax.jcr.Binary;
import javax.jcr.InvalidItemStateException;
import javax.jcr.InvalidLifecycleTransitionException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.NoSuchWorkspaceException;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.nodetype.NoSuchNodeTypeException;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import net.jcip.annotations.Immutable;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.Reference;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.query.QueryBuilder;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.session.GraphSession.Node;
import org.modeshape.graph.session.GraphSession.NodeId;
import org.modeshape.graph.session.GraphSession.PropertyInfo;
import org.modeshape.jcr.SessionCache.JcrNodePayload;
import org.modeshape.jcr.SessionCache.JcrPropertyPayload;
import org.modeshape.jcr.SessionCache.NodeEditor;

/**
 * An abstract implementation of the JCR {@link javax.jcr.Node} interface. Instances of this class are created and managed by the
 * {@link SessionCache}. Each instance indirectly references the {@link javax.jcr.Node node information} also managed by the
 * SessionCache, and finds and operates against this information with each method call.
 */
@Immutable
abstract class AbstractJcrNode extends AbstractJcrItem implements javax.jcr.Node {

    private static final NodeType[] EMPTY_NODE_TYPES = new NodeType[] {};
    private static final Set<Name> INTERNAL_NODE_TYPE_NAMES = Collections.singleton(ModeShapeLexicon.SHARE);

    protected final NodeId nodeId;
    protected Location location;

    AbstractJcrNode( SessionCache cache,
                     NodeId nodeId,
                     Location location ) {
        super(cache);
        this.nodeId = nodeId;
        this.location = location;
    }

    abstract boolean isRoot();

    public abstract AbstractJcrNode getParent() throws ItemNotFoundException, RepositoryException;

    final NodeId internalId() {
        return nodeId;
    }

    Location location() {
        return location;
    }

    void setLocation( Location location ) {
        this.location = location;
    }

    Path.Segment segment() throws RepositoryException {
        return nodeInfo().getSegment();
    }

    JcrLockManager lockManager() {
        return session().lockManager();
    }

    Node<JcrNodePayload, JcrPropertyPayload> nodeInfo()
        throws InvalidItemStateException, AccessDeniedException, RepositoryException {
        try {
            // This method will find the node if the 'nodeId' is still valid and cached...
            return cache.findNode(nodeId, location.getPath());
        } catch (ItemNotFoundException infe) {
            // However, if this node has changed (e.g., moved), the path may not be sufficient to find the node,
            // so try to find by location ...
            try {
                Node<JcrNodePayload, JcrPropertyPayload> info = null;
                if (location.getUuid() != null) {
                    info = cache.findNodeWith(location.with((Path)null));
                } else {
                    info = cache.findNodeWith(location);
                }
                // Update this node's location if required ...
                Location newLocation = info.getLocation();
                if (!newLocation.equals(location)) {
                    location = newLocation;
                }
                return info;
            } catch (ItemNotFoundException infe2) {
                // Use the first exception ...
                throw new InvalidItemStateException(infe.getMessage());
            }
        }
    }

    Node<JcrNodePayload, JcrPropertyPayload> parentNodeInfo()
        throws InvalidItemStateException, AccessDeniedException, RepositoryException {
        return nodeInfo().getParent();
    }

    NodeEditor editorForParent() throws RepositoryException {
        try {
            Node<JcrNodePayload, JcrPropertyPayload> parent = parentNodeInfo();
            return cache.getEditorFor(parent);
        } catch (ItemNotFoundException err) {
            String msg = JcrI18n.nodeHasAlreadyBeenRemovedFromThisSession.text(nodeId, cache.workspaceName());
            throw new RepositoryException(msg);
        } catch (InvalidItemStateException err) {
            String msg = JcrI18n.nodeHasAlreadyBeenRemovedFromThisSession.text(nodeId, cache.workspaceName());
            throw new RepositoryException(msg);
        }
    }

    final NodeEditor editor() throws RepositoryException {
        try {
            return cache.getEditorFor(nodeId, location.getPath());
        } catch (ItemNotFoundException err) {
            String msg = JcrI18n.nodeHasAlreadyBeenRemovedFromThisSession.text(nodeId, cache.workspaceName());
            throw new RepositoryException(msg);
        } catch (InvalidItemStateException err) {
            String msg = JcrI18n.nodeHasAlreadyBeenRemovedFromThisSession.text(nodeId, cache.workspaceName());
            throw new RepositoryException(msg);
        }
    }

    final JcrValue valueFrom( int propertyType,
                              Object value ) {
        if (value instanceof JcrBinary) {
            value = ((JcrBinary)value).binary();
        }
        return new JcrValue(cache.factories(), cache, propertyType, value);
    }

    final JcrValue valueFrom( String value ) {
        return new JcrValue(cache.factories(), cache, PropertyType.STRING, value);
    }

    final JcrValue valueFrom( Calendar value ) {
        ValueFactories factories = cache.factories();
        DateTime dateTime = factories.getDateFactory().create(value);
        return new JcrValue(factories, cache, PropertyType.DATE, dateTime);
    }

    final JcrValue valueFrom( InputStream value ) {
        ValueFactories factories = cache.factories();
        org.modeshape.graph.property.Binary binary = factories.getBinaryFactory().create(value);
        return new JcrValue(factories, cache, PropertyType.BINARY, binary);
    }

    final JcrValue valueFrom( Binary value ) {
        ValueFactories factories = cache.factories();
        org.modeshape.graph.property.Binary binary = ((JcrBinary)value).binary();
        return new JcrValue(factories, cache, PropertyType.DATE, binary);
    }

    final JcrValue valueFrom( javax.jcr.Node value ) throws UnsupportedRepositoryOperationException, RepositoryException {
        ValueFactories factories = cache.factories();
        Reference ref = factories.getReferenceFactory().create(value.getIdentifier());
        return new JcrValue(factories, cache, PropertyType.REFERENCE, ref);
    }

    final JcrValue[] valuesFrom( int propertyType,
                                 Object[] values ) {
        /*
         * Null values in the array are "compacted" (read: ignored) as per section 7.1.6 in the JCR 1.0.1 specification. 
         */
        int len = values.length;
        ValueFactories factories = cache.factories();
        List<JcrValue> results = new ArrayList<JcrValue>(len);
        for (int i = 0; i != len; ++i) {
            if (values[i] != null) results.add(new JcrValue(factories, cache, propertyType, values[i]));
        }
        return results.toArray(new JcrValue[results.size()]);
    }

    @Override
    Path path() throws RepositoryException {
        // Don't use the path in the location, since it may no longer be valid
        return nodeInfo().getPath();
    }

    boolean isReferenceable() throws RepositoryException {
        return isNodeType(JcrMixLexicon.REFERENCEABLE);
    }

    boolean isLockable() throws RepositoryException {
        return isNodeType(JcrMixLexicon.LOCKABLE);
    }

    boolean isShareable() throws RepositoryException {
        return isNodeType(JcrMixLexicon.SHAREABLE);
    }

    boolean isShared() {
        return false;
    }

    /**
     * Get the UUID of this node, regardless of whether this node is referenceable.
     * 
     * @return the UUID of this node; never null
     * @throws RepositoryException if there is an error accessing the UUID of the node
     */
    UUID uuid() throws RepositoryException {
        UUID uuid = nodeInfo().getLocation().getUuid();
        if (uuid == null) {
            PropertyInfo<JcrPropertyPayload> uuidProp = nodeInfo().getProperty(JcrLexicon.UUID);
            if (uuidProp == null) {
                uuidProp = nodeInfo().getProperty(ModeShapeLexicon.UUID);
            }
            assert uuidProp != null;
            assert !uuidProp.getProperty().isEmpty();
            uuid = context().getValueFactories().getUuidFactory().create(uuidProp.getProperty().getFirstValue());
        }
        assert uuid != null;
        return uuid;
    }

    /**
     * Get the JCR 2.0-compatible identifier of this node, regardless of whether this node is referenceable.
     * 
     * @return the JCR 2.0 identifier of this node; never null
     * @throws RepositoryException if there is an error accessing the identifier of the node
     */
    String identifier() throws RepositoryException {
        String identifier = null;
        UUID uuid = nodeInfo().getLocation().getUuid();
        if (uuid == null) {
            PropertyInfo<JcrPropertyPayload> uuidProp = nodeInfo().getProperty(JcrLexicon.UUID);
            if (uuidProp == null) {
                uuidProp = nodeInfo().getProperty(ModeShapeLexicon.UUID);
            }
            if (uuidProp != null) {
                assert !uuidProp.getProperty().isEmpty();
                identifier = context().getValueFactories().getStringFactory().create(uuidProp.getProperty().getFirstValue());
            } else {
                // There is no UUID property, then we need to return an identifier, so use the path ...
                identifier = getPath();
            }
        } else {
            identifier = uuid.toString();
        }
        assert identifier != null;
        return identifier;
    }

    /**
     * Get the absolute and normalized identifier path for this node, regardless of whether this node is referenceable.
     * 
     * @return the node's identifier path; never null
     * @throws RepositoryException if there is an error accessing the identifier of this node
     */
    String identifierPath() throws RepositoryException {
        return "[" + identifier() + "]";
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getUUID()
     * @deprecated As of JCR 2.0, {@link #getIdentifier()} should be used instead.
     */
    @Deprecated
    public String getUUID() throws RepositoryException {
        // Return "jcr:uuid" only if node is referenceable
        if (!isReferenceable()) {
            throw new UnsupportedRepositoryOperationException(JcrI18n.nodeNotReferenceable.text());
        }
        return identifier();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getIdentifier()
     */
    @Override
    public String getIdentifier() throws RepositoryException {
        return identifier();
    }

    /**
     * {@inheritDoc}
     * 
     * @return <code>true</code>
     * @see javax.jcr.Item#isNode()
     */
    public final boolean isNode() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @return <code>false</code>
     * @see javax.jcr.Node#isNodeType(java.lang.String)
     */
    public boolean isNodeType( String nodeTypeName ) throws RepositoryException {
        return isNodeType(nameFrom(nodeTypeName));
    }

    /**
     * Determine whether this node's primary type or any of the mixins are or extend the node type with the supplied name. This
     * method is semantically equivalent to but slightly more efficient than the {@link #isNodeType(String) equivalent in the JCR
     * API}, especially when the node type name is already a {@link Name} object.
     * 
     * @param nodeTypeName the name of the node type
     * @return true if this node is of the node type given by the supplied name, or false otherwise
     * @throws RepositoryException if there is an exception
     */
    public final boolean isNodeType( Name nodeTypeName ) throws RepositoryException {
        checkSession();
        return cache.isNodeType(nodeInfo(), nodeTypeName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getDefinition()
     */
    public NodeDefinition getDefinition() throws RepositoryException {
        checkSession();
        NodeDefinitionId definitionId = nodeInfo().getPayload().getDefinitionId();
        return session().nodeTypeManager().getNodeDefinition(definitionId);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getPrimaryNodeType()
     */
    public JcrNodeType getPrimaryNodeType() throws RepositoryException {
        checkSession();
        return session().nodeTypeManager().getNodeType(getPrimaryTypeName());
    }

    Name getPrimaryTypeName() throws RepositoryException {
        return nodeInfo().getPayload().getPrimaryTypeName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getMixinNodeTypes()
     */
    public NodeType[] getMixinNodeTypes() throws RepositoryException {
        checkSession();
        NodeTypeManager nodeTypeManager = session().nodeTypeManager();
        Property mixinTypesProperty = getProperty(JcrLexicon.MIXIN_TYPES);
        if (mixinTypesProperty == null) return EMPTY_NODE_TYPES;
        List<NodeType> mixinNodeTypes = new LinkedList<NodeType>();
        for (Value value : mixinTypesProperty.getValues()) {
            String nodeTypeName = value.getString();
            NodeType nodeType = nodeTypeManager.getNodeType(nodeTypeName);
            if (nodeType != null) mixinNodeTypes.add(nodeType);
        }
        return mixinNodeTypes.toArray(new NodeType[mixinNodeTypes.size()]);
    }

    List<Name> getMixinTypeNames() throws RepositoryException {
        return nodeInfo().getPayload().getMixinTypeNames();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getPrimaryItem()
     */
    public final Item getPrimaryItem() throws RepositoryException {
        checkSession();
        // Get the primary item name from this node's type ...
        NodeType primaryType = getPrimaryNodeType();
        String primaryItemNameString = primaryType.getPrimaryItemName();
        if (primaryItemNameString == null) {
            I18n msg = JcrI18n.noPrimaryItemNameDefinedOnPrimaryType;
            throw new ItemNotFoundException(msg.text(primaryType.getName(), getPath(), cache.workspaceName()));
        }
        try {
            Path primaryItemPath = context().getValueFactories().getPathFactory().create(primaryItemNameString);
            if (primaryItemPath.size() != 1 || primaryItemPath.isAbsolute()) {
                I18n msg = JcrI18n.primaryItemNameForPrimaryTypeIsNotValid;
                throw new ItemNotFoundException(msg.text(primaryType.getName(),
                                                         primaryItemNameString,
                                                         getPath(),
                                                         cache.workspaceName()));
            }
            return cache.findJcrItem(nodeId, location.getPath(), primaryItemPath);
        } catch (ValueFormatException error) {
            I18n msg = JcrI18n.primaryItemNameForPrimaryTypeIsNotValid;
            throw new ItemNotFoundException(msg.text(primaryType.getName(),
                                                     primaryItemNameString,
                                                     getPath(),
                                                     cache.workspaceName()));
        } catch (PathNotFoundException error) {
            I18n msg = JcrI18n.primaryItemDoesNotExist;
            throw new ItemNotFoundException(msg.text(primaryType.getName(),
                                                     primaryItemNameString,
                                                     getPath(),
                                                     cache.workspaceName()));
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>otherItem</code> is <code>null</code>.
     * @see javax.jcr.Item#isSame(javax.jcr.Item)
     */
    @Override
    public boolean isSame( Item otherItem ) throws RepositoryException {
        CheckArg.isNotNull(otherItem, "otherItem");
        checkSession();
        if (super.isSame(otherItem) && otherItem instanceof javax.jcr.Node) {
            if (otherItem instanceof AbstractJcrNode) {
                AbstractJcrNode that = (AbstractJcrNode)otherItem;
                if (this.isReferenceable() && that.isReferenceable()) {
                    // Both are referenceable, so compare the UUIDs ...
                    return getUUID().equals(((AbstractJcrNode)otherItem).getUUID());
                }

                // One or both are not referenceable, so find the nearest ancestor that is referenceable.
                // The correspondence identifier (per Section 4.10.2 of JSR-170, version 1.0.1) for a
                // non-referenceable node is the pair of the UUID of the nearest referenceable ancestor and
                // the relative path from that referenceable ancestor. Per Section 6.2.8, two non-referenceable
                // nodes are the same if they have the same correspondence identifier.
                CorrespondenceId thisId = this.getCorrespondenceId();
                CorrespondenceId thatId = that.getCorrespondenceId();
                return thisId.equals(thatId);
            }
            // If not our implementation, let the other item figure out whether we are the same.
            return otherItem.isSame(this);
        }
        return false;
    }

    protected CorrespondenceId getCorrespondenceId() throws RepositoryException {
        if (this.isReferenceable()) return new CorrespondenceId(getUUID());
        assert !this.isRoot(); // the root must be referenceable

        // Find the nearest ancestor that is referenceable ...
        Path currentPath = path();
        AbstractJcrNode node = this.getParent();
        int beginIndex = currentPath.size() - 1;
        while (!node.isRoot() && !node.isReferenceable()) {
            node = node.getParent();
            --beginIndex;
        }
        // Get the relative path from the ancestor to this node ...
        Path relativePath = currentPath.relativeTo(node.path());
        assert !relativePath.isAbsolute();
        return new CorrespondenceId(node.getUUID(), relativePath);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#hasProperties()
     */
    public final boolean hasProperties() throws RepositoryException {
        checkSession();
        return nodeInfo().getPropertyCount() > 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>relativePath</code> is empty or <code>null</code>.
     * @see javax.jcr.Node#hasProperty(java.lang.String)
     */
    public final boolean hasProperty( String relativePath ) throws RepositoryException {
        CheckArg.isNotEmpty(relativePath, "relativePath");
        checkSession();
        if (relativePath.indexOf('/') >= 0 || relativePath.startsWith("[")) {
            try {
                getProperty(relativePath);
                return true;
            } catch (PathNotFoundException e) {
                return false;
            }
        }
        if (relativePath.equals(".")) return false;
        if (relativePath.equals("..")) return false;
        // Otherwise it should be a property on this node ...
        return nodeInfo().getProperty(nameFrom(relativePath)) != null;
    }

    public final boolean hasProperty( Name name ) throws RepositoryException {
        checkSession();
        return nodeInfo().getProperty(name) != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getProperties()
     */
    public PropertyIterator getProperties() throws RepositoryException {
        checkSession();
        return new JcrPropertyIterator(cache.findJcrPropertiesFor(nodeId, location.getPath()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getProperties(java.lang.String)
     */
    public PropertyIterator getProperties( String namePattern ) throws RepositoryException {
        CheckArg.isNotNull(namePattern, "namePattern");
        checkSession();
        namePattern = namePattern.trim();
        if (namePattern.length() == 0) return new JcrEmptyPropertyIterator();
        if ("*".equals(namePattern)) {
            Collection<AbstractJcrProperty> properties = cache.findJcrPropertiesFor(nodeId, location.getPath());
            return new JcrPropertyIterator(properties);
        }

        return getProperties(namePattern.split("[|]"));
    }

    public PropertyIterator getProperties( String[] nameGlobs ) throws RepositoryException {
        CheckArg.isNotNull(nameGlobs, "nameGlobs");
        if (nameGlobs.length == 0) return new JcrEmptyPropertyIterator();
        Collection<AbstractJcrProperty> properties = cache.findJcrPropertiesFor(nodeId, location.getPath());

        // Figure out the patterns for each of the different disjunctions in the supplied pattern ...
        List<Object> patterns = createPatternsFor(nameGlobs);

        boolean foundMatch = false;
        Collection<AbstractJcrProperty> matchingProperties = new LinkedList<AbstractJcrProperty>();
        Iterator<AbstractJcrProperty> iter = properties.iterator();
        while (iter.hasNext()) {
            AbstractJcrProperty property = iter.next();
            String propName = property.getName();
            assert foundMatch == false;
            for (Object patternOrMatch : patterns) {
                if (patternOrMatch instanceof Pattern) {
                    Pattern pattern = (Pattern)patternOrMatch;
                    if (pattern.matcher(propName).matches()) {
                        foundMatch = true;
                        break;
                    }
                } else {
                    String match = (String)patternOrMatch;
                    if (propName.equals(match)) {
                        foundMatch = true;
                        break;
                    }
                }
            }
            if (foundMatch) {
                matchingProperties.add(property);
                foundMatch = false; // for the next iteration ..
            }
        }
        return new JcrPropertyIterator(matchingProperties);
    }

    /**
     * Obtain an iterator over the nodes that reference this node.
     * 
     * @param maxNumberOfNodes the maximum number of nodes that should be returned, or {@link Integer#MAX_VALUE} (or a negative
     *        value) for all nodes
     * @return the iterator over the referencing nodes; never null
     * @throws RepositoryException if an error occurs while obtaining the information
     */
    protected final NodeIterator referencingNodes( int maxNumberOfNodes ) throws RepositoryException {
        if (!this.isReferenceable()) {
            return new JcrEmptyNodeIterator();
        }
        if (maxNumberOfNodes < 0) maxNumberOfNodes = Integer.MAX_VALUE;

        // Execute a query that will report all nodes referencing this node ...
        String uuid = getUUID();
        QueryBuilder builder = new QueryBuilder(context().getValueFactories().getTypeSystem());
        QueryCommand query = builder.select("jcr:primaryType")
                                    .fromAllNodesAs("allNodes")
                                    .where()
                                    .referenceValue("allNodes")
                                    .isEqualTo(uuid)
                                    .end()
                                    .limit(maxNumberOfNodes)
                                    .query();
        Query jcrQuery = session().workspace().queryManager().createQuery(query);
        QueryResult result = jcrQuery.execute();
        return result.getNodes();
    }

    /**
     * Determine whether there is at least one other node that has a reference to this node.
     * 
     * @return true if this node is referenced by at least one other node, or false if there are no references to this node
     * @throws RepositoryException if an error occurs while obtaining the information
     */
    protected final boolean hasIncomingReferences() throws RepositoryException {
        return referencingNodes(1).hasNext();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getReferences()
     */
    public final PropertyIterator getReferences() throws RepositoryException {
        return getReferences(null);
    }

    /**
     * This method returns all REFERENCE properties that refer to this node, have the specified name and that are accessible
     * through the current Session. If the name parameter is null then all referring REFERENCES are returned regardless of name.
     * <p>
     * Some implementations may only return properties that have been persisted. Some may return both properties that have been
     * persisted and those that have been dispatched but not persisted (for example, those saved within a transaction but not yet
     * committed) while others implementations may return these two categories of property as well as properties that are still
     * pending and not yet dispatched.
     * </p>
     * <p>
     * In implementations that support versioning, this method does not return properties that are part of the frozen state of a
     * version in version storage.
     * </p>
     * <p>
     * If this node has no referring REFERENCE properties with the specified name, an empty iterator is returned. This includes
     * the case where this node is not referenceable.
     * </p>
     * 
     * @param propertyName - name of referring REFERENCE properties to be returned; if null then all referring REFERENCEs are
     *        returned.
     * @return A PropertyIterator.
     * @throws RepositoryException if an error occurs
     * @see javax.jcr.Node#getReferences()
     */
    public final PropertyIterator getReferences( String propertyName ) throws RepositoryException {
        checkSession();
        return propertiesOnOtherNodesReferencingThis(propertyName, PropertyType.REFERENCE);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getWeakReferences()
     */
    @Override
    public PropertyIterator getWeakReferences() throws RepositoryException {
        return getWeakReferences(null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getWeakReferences(java.lang.String)
     */
    @Override
    public PropertyIterator getWeakReferences( String propertyName ) throws RepositoryException {
        checkSession();
        return propertiesOnOtherNodesReferencingThis(propertyName, PropertyType.WEAKREFERENCE);
    }

    /**
     * Find the properties on other nodes that are REFERENCE or WEAKREFERENCE properties (as dictated by the
     * <code>referenceType</code> parameter) to this node.
     * 
     * @param propertyName the name of the referring REFERENCE or WEAKREFERENCE properties on the other nodes, or null if all
     *        referring REFERENCE or WEAKREFERENCE properties should be returned
     * @param referenceType either {@link PropertyType#REFERENCE} or {@link PropertyType#WEAKREFERENCE};
     * @return the property iterator; never null by may be {@link JcrEmptyPropertyIterator empty} if there are no references or if
     *         this node is not referenceable
     * @throws RepositoryException if there is an error finding the referencing properties
     */
    protected PropertyIterator propertiesOnOtherNodesReferencingThis( String propertyName,
                                                                      int referenceType ) throws RepositoryException {
        if (!this.isReferenceable()) {
            // This node is not referenceable, so it cannot have any references to it ...
            return new JcrEmptyPropertyIterator();
        }
        NodeIterator iter = referencingNodes(Integer.MAX_VALUE);
        if (!iter.hasNext()) {
            return new JcrEmptyPropertyIterator();
        }
        // Use the identifier, not the UUID (since the getUUID() method just calls the getIdentifier() method) ...
        String id = getIdentifier();
        List<Property> references = new LinkedList<Property>();
        while (iter.hasNext()) {
            javax.jcr.Node node = iter.nextNode();

            // Go through the properties and look for reference properties that have a value of this node's UUID ...
            PropertyIterator propIter = node.getProperties();
            while (propIter.hasNext()) {
                Property prop = propIter.nextProperty();
                // Look at the definition's required type ...
                int propType = prop.getDefinition().getRequiredType();
                if (propType == referenceType || propType == PropertyType.UNDEFINED || propType == PropertyType.STRING) {
                    if (propertyName != null && !propertyName.equals(prop.getName())) continue;
                    if (prop.getDefinition().isMultiple()) {
                        for (Value value : prop.getValues()) {
                            if (id.equals(value.getString())) {
                                references.add(prop);
                                break;
                            }
                        }
                    } else {
                        Value value = prop.getValue();
                        if (id.equals(value.getString())) {
                            references.add(prop);
                        }
                    }
                }
            }
        }

        if (references.isEmpty()) return new JcrEmptyPropertyIterator();
        return new JcrPropertyIterator(references);
    }

    /**
     * A non-standard method to obtain a property given the {@link Name ModeShape Name} object. This method is faster
     * 
     * @param propertyName the property name
     * @return the JCR property with the supplied name, or null if the property doesn't exist
     * @throws RepositoryException if there is an error finding the property with the supplied name
     */
    public final AbstractJcrProperty getProperty( Name propertyName ) throws RepositoryException {
        AbstractJcrProperty property = cache.findJcrProperty(nodeId, location.getPath(), propertyName);
        // Must be referenceable in order to return this property ...
        if (property != null && JcrLexicon.UUID.equals(propertyName) && !isReferenceable()) return null;
        return property;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>relativePath</code> is empty or <code>null</code>.
     * @see javax.jcr.Node#getProperty(java.lang.String)
     */
    public Property getProperty( String relativePath ) throws RepositoryException {
        CheckArg.isNotEmpty(relativePath, "relativePath");
        checkSession();
        int indexOfFirstSlash = relativePath.indexOf('/');
        if (indexOfFirstSlash == 0 || relativePath.startsWith("[")) {
            // Not a relative path ...
            throw new IllegalArgumentException(JcrI18n.invalidPathParameter.text(relativePath, "relativePath"));
        }
        Name propertyName = null;
        if (indexOfFirstSlash != -1) {
            // We know it's a relative path with more than one segment ...
            Path path = pathFrom(relativePath).getNormalizedPath();
            assert !path.isIdentifier();
            if (path.size() > 1) {
                try {
                    AbstractJcrItem item = cache.findJcrItem(nodeId, location.getPath(), path);
                    if (item instanceof Property) {
                        return (Property)item;
                    }
                } catch (ItemNotFoundException e) {
                    I18n msg = JcrI18n.propertyNotFoundAtPathRelativeToReferenceNode;
                    throw new PathNotFoundException(msg.text(relativePath, getPath(), cache.workspaceName()));
                }
                I18n msg = JcrI18n.propertyNotFoundAtPathRelativeToReferenceNode;
                throw new PathNotFoundException(msg.text(relativePath, getPath(), cache.workspaceName()));
            }
            propertyName = path.getLastSegment().getName();
        } else {
            propertyName = nameFrom(relativePath);
        }
        // It's just a name, so look for it directly ...
        Property result = getProperty(propertyName);
        if (result != null) return result;
        I18n msg = JcrI18n.pathNotFoundRelativeTo;
        throw new PathNotFoundException(msg.text(relativePath, getPath(), cache.workspaceName()));
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>relativePath</code> is empty or <code>null</code>.
     * @see javax.jcr.Node#hasNode(java.lang.String)
     */
    public final boolean hasNode( String relativePath ) throws RepositoryException {
        CheckArg.isNotEmpty(relativePath, "relativePath");
        checkSession();
        if (relativePath.equals(".")) return true;
        if (relativePath.equals("..")) return isRoot() ? false : true;
        int indexOfFirstSlash = relativePath.indexOf('/');
        if (indexOfFirstSlash == 0 || relativePath.startsWith("[")) {
            // Not a relative path ...
            throw new IllegalArgumentException(JcrI18n.invalidPathParameter.text(relativePath, "relativePath"));
        }
        if (indexOfFirstSlash != -1) {
            Path path = pathFrom(relativePath).getNormalizedPath();
            try {
                AbstractJcrNode item = cache.findJcrNode(nodeId, location.getPath(), path);
                return item != null;
            } catch (PathNotFoundException e) {
                return false;
            }
        }
        // It's just a name, so look for a child ...
        try {
            Path.Segment segment = segmentFrom(relativePath);
            return nodeInfo().getChild(segment) != null;
        } catch (org.modeshape.graph.property.PathNotFoundException e) {
            return false;
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#hasNodes()
     */
    public final boolean hasNodes() throws RepositoryException {
        checkSession();
        return nodeInfo().getChildrenCount() > 0;
    }

    /**
     * A non-standard method to obtain a child node given the {@link Name ModeShape Name} object. This method is faster
     * 
     * @param childNodeName the child node name
     * @return the child node with the supplied name, or null if no child node exists with that name
     * @throws RepositoryException if there is an error finding the child node with the supplied name
     */
    public final AbstractJcrNode getNode( Name childNodeName ) throws RepositoryException {
        try {
            Path childPath = context().getValueFactories().getPathFactory().createRelativePath(childNodeName);
            return cache.findJcrNode(nodeId, location.getPath(), childPath);
        } catch (PathNotFoundException infe) {
            return null;
        } catch (ItemNotFoundException infe) {
            return null;
        }
    }

    /**
     * A non-standard method to obtain a child node given the {@link Path ModeShape relative path} object.
     * 
     * @param relativePath the relative path
     * @return the child node with the supplied name, or null if no child node exists with that name
     * @throws RepositoryException if there is an error finding the child node with the supplied name
     */
    public final AbstractJcrNode getNode( Path relativePath ) throws RepositoryException {
        return getNode(relativePath.getString(namespaces()));
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>relativePath</code> is empty or <code>null</code>.
     * @see javax.jcr.Node#getNode(java.lang.String)
     */
    public final AbstractJcrNode getNode( String relativePath ) throws RepositoryException {
        CheckArg.isNotEmpty(relativePath, "relativePath");
        checkSession();
        if (relativePath.equals(".")) return this;
        if (relativePath.equals("..")) return this.getParent();
        int indexOfFirstSlash = relativePath.indexOf('/');
        if (indexOfFirstSlash == 0 || relativePath.startsWith("[")) {
            // Not a relative path ...
            throw new IllegalArgumentException(JcrI18n.invalidPathParameter.text(relativePath, "relativePath"));
        }
        Path.Segment segment = null;
        if (indexOfFirstSlash != -1) {
            // We know it's a relative path with more than one segment ...
            Path path = pathFrom(relativePath).getNormalizedPath();
            if (path.size() == 1) {
                if (path.getLastSegment().isSelfReference()) return this;
                if (path.getLastSegment().isParentReference()) return this.getParent();
            }
            // We know it's a resolved relative path with more than one segment ...
            if (path.size() > 1) {
                return cache.findJcrNode(nodeId, location.getPath(), path);
            }
            segment = path.getLastSegment();
        } else {
            segment = segmentFrom(relativePath);
        }
        assert !segment.isIdentifier();
        // It's just a name, so look for a child ...
        try {
            return nodeInfo().getChild(segment).getPayload().getJcrNode();
        } catch (org.modeshape.graph.property.PathNotFoundException e) {
            String msg = JcrI18n.childNotFoundUnderNode.text(segment, getPath(), cache.workspaceName());
            throw new PathNotFoundException(msg);
        } catch (RepositorySourceException e) {
            throw new RepositoryException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getNodes()
     */
    public final NodeIterator getNodes() throws RepositoryException {
        checkSession();
        int childCount = nodeInfo().getChildrenCount();
        if (childCount == 0) {
            return new JcrEmptyNodeIterator();
        }
        List<AbstractJcrNode> matchingChildren = new LinkedList<AbstractJcrNode>();
        for (Node<JcrNodePayload, JcrPropertyPayload> child : nodeInfo().getChildren()) {
            matchingChildren.add(child.getPayload().getJcrNode());
        }
        return new JcrChildNodeIterator(matchingChildren, childCount);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getNodes(java.lang.String)
     */
    public NodeIterator getNodes( String namePattern ) throws RepositoryException {
        CheckArg.isNotNull(namePattern, "namePattern");
        checkSession();
        namePattern = namePattern.trim();
        if (namePattern.length() == 0) return new JcrEmptyNodeIterator();
        if ("*".equals(namePattern)) return getNodes();

        return getNodes(namePattern.split("[|]"));
    }

    public NodeIterator getNodes( String[] nameGlobs ) throws RepositoryException {
        CheckArg.isNotNull(nameGlobs, "nameGlobs");
        if (nameGlobs.length == 0) return new JcrEmptyNodeIterator();

        List<Object> patterns = createPatternsFor(nameGlobs);

        List<AbstractJcrNode> matchingChildren = new LinkedList<AbstractJcrNode>();
        NamespaceRegistry registry = namespaces();
        boolean foundMatch = false;
        for (Node<JcrNodePayload, JcrPropertyPayload> child : nodeInfo().getChildren()) {
            String childName = child.getName().getString(registry);
            for (Object patternOrMatch : patterns) {
                if (patternOrMatch instanceof Pattern) {
                    Pattern pattern = (Pattern)patternOrMatch;
                    if (pattern.matcher(childName).matches()) foundMatch = true;
                } else {
                    String match = (String)patternOrMatch;
                    if (childName.equals(match)) foundMatch = true;
                }
                if (foundMatch) {
                    foundMatch = false;
                    matchingChildren.add(child.getPayload().getJcrNode());
                    break;
                }
            }
        }
        return new JcrChildNodeIterator(matchingChildren, matchingChildren.size());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>visitor</code> is <code>null</code>.
     * @see javax.jcr.Item#accept(javax.jcr.ItemVisitor)
     */
    public final void accept( ItemVisitor visitor ) throws RepositoryException {
        CheckArg.isNotNull(visitor, "visitor");
        checkSession();
        visitor.visit(this);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>ModeShape Implementation Notes</b>
     * </p>
     * <p>
     * ModeShape imposes the following additional restrictions on the addition of mixin types in addition to the restrictions
     * provided by the JCR 1.0 and JCR 2.0 specifications:
     * <ol>
     * <li>No properties defined by the mixin type can have the same name as any property defined by the node's primary type or
     * any of its existing mixin types.</li>
     * <li>No child nodes defined by the mixin type can have the same name as any child node defined by the node's primary type or
     * any of its existing mixin types.</li>
     * <li>If the node has a current residual definition for child nodes and/or properties, all nodes and properties that share a
     * name with a child node definition or property definition from the new mixin type must be compatible with the definition
     * provided by the new mixin type.</li>
     * </ol>
     * </p>
     * 
     * @see javax.jcr.Node#canAddMixin(java.lang.String)
     */
    public final boolean canAddMixin( String mixinName ) throws NoSuchNodeTypeException, RepositoryException {
        CheckArg.isNotNull(mixinName, "mixinName");
        CheckArg.isNotZeroLength(mixinName, "mixinName");
        checkSession();

        session().checkPermission(path(), ModeShapePermissions.SET_PROPERTY);

        JcrNodeType mixinCandidateType = cache.nodeTypes().getNodeType(mixinName);

        if (this.isLocked()) {
            return false;
        }

        if (!isCheckedOut()) {
            return false;
        }

        if (this.getDefinition().isProtected()) {
            return false;
        }

        if (mixinCandidateType.isAbstract()) {
            return false;
        }

        if (!mixinCandidateType.isMixin()) {
            return false;
        }

        if (isNodeType(mixinCandidateType.getInternalName())) return true;

        // ------------------------------------------------------------------------------
        // Check for any existing properties based on residual definitions that conflict
        // ------------------------------------------------------------------------------
        for (JcrPropertyDefinition propertyDefinition : mixinCandidateType.propertyDefinitions()) {
            if (!hasProperty(propertyDefinition.getInternalName())) continue;
            AbstractJcrProperty existingProp = cache.findJcrProperty(nodeId,
                                                                     location.getPath(),
                                                                     propertyDefinition.getInternalName());
            if (existingProp != null) {
                if (propertyDefinition.isMultiple()) {
                    if (!propertyDefinition.canCastToTypeAndSatisfyConstraints(existingProp.getValues())) {
                        return false;
                    }
                } else {
                    if (!propertyDefinition.canCastToTypeAndSatisfyConstraints(existingProp.getValue())) {
                        return false;
                    }
                }
            }
        }

        // ------------------------------------------------------------------------------
        // Check for any existing child nodes based on residual definitions that conflict
        // ------------------------------------------------------------------------------
        Set<Name> mixinChildNodeNames = new HashSet<Name>();
        for (JcrNodeDefinition nodeDefinition : mixinCandidateType.childNodeDefinitions()) {
            mixinChildNodeNames.add(nodeDefinition.getInternalName());
        }

        for (Name nodeName : mixinChildNodeNames) {
            // Need to figure out if the child node requires an SNS definition
            int snsCount = nodeInfo().getChildrenCount(nodeName);
            for (Node<JcrNodePayload, JcrPropertyPayload> child : nodeInfo().getChildren(nodeName)) {
                JcrNodeDefinition match = this.cache.nodeTypes().findChildNodeDefinition(mixinCandidateType.getInternalName(),
                                                                                         Collections.<Name>emptyList(),
                                                                                         nodeName,
                                                                                         child.getPayload().getPrimaryTypeName(),
                                                                                         snsCount,
                                                                                         false);

                if (match == null) {
                    return false;
                }
            }

        }

        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>ModeShape Implementation Notes</b>
     * </p>
     * <p>
     * The criteria noted in {@link #canAddMixin(String)} must be satisifed in addition to the criteria defined in the JCR 1.0 and
     * JCR 2.0 specifications.
     * </p>
     * 
     * @see javax.jcr.Node#addMixin(java.lang.String)
     */
    public final void addMixin( String mixinName ) throws RepositoryException {
        CheckArg.isNotNull(mixinName, "mixinName");
        CheckArg.isNotZeroLength(mixinName, "mixinName");
        checkSession();

        JcrNodeType mixinCandidateType = cache.nodeTypes().getNodeType(mixinName);

        // Check this separately since it throws a different type of exception
        if (this.isLocked() && !getLock().isLockOwningSession()) {
            throw new LockException(JcrI18n.lockTokenNotHeld.text(this.location));
        }

        if (!isCheckedOut()) {
            throw new VersionException(JcrI18n.nodeIsCheckedIn.text(getPath()));
        }

        if (!canAddMixin(mixinName)) {
            throw new ConstraintViolationException(JcrI18n.cannotAddMixin.text(mixinName));
        }

        if (isNodeType(mixinName)) return;

        this.editor().addMixin(mixinCandidateType);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>ModeShape Implementation Notes</b>
     * </p>
     * <p>
     * ModeShape allows the removal of a mixin type if and only if all of the node's existing child nodes and properties would
     * still have a valid definition from the node's primary type or other mixin types. In practice, this means that either the
     * node must have a residual definition compatible with any of the remaining child nodes or properties that currently use a
     * definition from the to-be-removed mixin type or all of the child nodes and properties that use a definition from the
     * to-be-removed mixin type must be removed prior to calling this method.
     * </p>
     * *
     * 
     * @see javax.jcr.Node#removeMixin(java.lang.String)
     */
    public void removeMixin( String mixinName ) throws RepositoryException {
        checkSession();

        if (this.isLocked() && !getLock().isLockOwningSession()) {
            throw new LockException(JcrI18n.lockTokenNotHeld.text(this.location));
        }

        if (!isCheckedOut()) {
            throw new VersionException(JcrI18n.nodeIsCheckedIn.text(getPath()));
        }

        if (getDefinition().isProtected()) {
            throw new ConstraintViolationException(JcrI18n.cannotRemoveFromProtectedNode.text(getPath()));
        }

        Property existingMixinProperty = getProperty(JcrLexicon.MIXIN_TYPES);

        if (existingMixinProperty == null) {
            throw new NoSuchNodeTypeException(JcrI18n.invalidMixinTypeForNode.text(mixinName, getPath()));
        }

        Value[] existingMixinValues = existingMixinProperty.getValues();

        if (existingMixinValues.length == 0) {
            throw new NoSuchNodeTypeException(JcrI18n.invalidMixinTypeForNode.text(mixinName, getPath()));
        }

        // ------------------------------------------------------------------------------
        // Build the new list of mixin types
        // ------------------------------------------------------------------------------

        int newMixinValuesCount = existingMixinValues.length - 1;
        Value[] newMixinValues = new Value[newMixinValuesCount];
        List<Name> newMixinNames = new ArrayList<Name>(newMixinValuesCount);
        Name primaryTypeName = getPrimaryNodeType().getInternalName();

        int j = 0;
        for (int i = 0; i < existingMixinValues.length; i++) {
            if (!existingMixinValues[i].getString().equals(mixinName)) {
                if (j < newMixinValuesCount) {
                    newMixinValues[j++] = existingMixinValues[i];
                    newMixinNames.add(cache.nameFactory.create(existingMixinValues[i].getString()));
                } else {
                    throw new NoSuchNodeTypeException(JcrI18n.invalidMixinTypeForNode.text(mixinName, getPath()));
                }
            }
        }

        // ------------------------------------------------------------------------------
        // Check that any remaining properties that use the mixin type to be removed
        // match the residual definition for the node.
        // ------------------------------------------------------------------------------

        for (PropertyIterator iter = getProperties(); iter.hasNext();) {
            Property property = iter.nextProperty();
            if (mixinName.equals(property.getDefinition().getDeclaringNodeType().getName())) {
                JcrPropertyDefinition match;

                // Only the residual definition would work - if there were any other definition for this name,
                // the mixin type would not have been added due to the conflict
                if (property.getDefinition().isMultiple()) {
                    match = cache.nodeTypes().findPropertyDefinition(primaryTypeName,
                                                                     newMixinNames,
                                                                     JcrNodeType.RESIDUAL_NAME,
                                                                     property.getValues(),
                                                                     true);
                } else {
                    match = cache.nodeTypes().findPropertyDefinition(primaryTypeName,
                                                                     newMixinNames,
                                                                     JcrNodeType.RESIDUAL_NAME,
                                                                     property.getValue(),
                                                                     true,
                                                                     true);
                }

                if (match == null) {
                    throw new ConstraintViolationException(JcrI18n.noDefinition.text("property",
                                                                                     property.getName(),
                                                                                     getPath(),
                                                                                     primaryTypeName,
                                                                                     newMixinNames));
                }
            }
        }

        // ------------------------------------------------------------------------------
        // Check that any remaining child nodes that use the mixin type to be removed
        // match the residual definition for the node.
        // ------------------------------------------------------------------------------
        for (NodeIterator iter = getNodes(); iter.hasNext();) {
            AbstractJcrNode node = (AbstractJcrNode)iter.nextNode();
            Name childNodeName = cache.nameFactory.create(node.getName());
            int snsCount = node.nodeInfo().getChildrenCount(childNodeName);
            if (mixinName.equals(node.getDefinition().getDeclaringNodeType().getName())) {
                // Only the residual definition would work - if there were any other definition for this name,
                // the mixin type would not have been added due to the conflict
                JcrNodeDefinition match = cache.nodeTypes().findChildNodeDefinition(primaryTypeName,
                                                                                    newMixinNames,
                                                                                    JcrNodeType.RESIDUAL_NAME,
                                                                                    node.getPrimaryNodeType().getInternalName(),
                                                                                    snsCount,
                                                                                    true);

                if (match == null) {
                    throw new ConstraintViolationException(JcrI18n.noDefinition.text("child node",
                                                                                     node.getName(),
                                                                                     getPath(),
                                                                                     primaryTypeName,
                                                                                     newMixinNames));
                }
            }
        }

        editor().setProperty(JcrLexicon.MIXIN_TYPES, newMixinValues, PropertyType.NAME, false);
    }

    /**
     * Attempts to change the primary type of this node. Not yet supported, but some error checking is added.
     * 
     * @param nodeTypeName the name of the new primary type for this node
     * @throws NoSuchNodeTypeException if no node with the given name exists
     * @throws VersionException if this node is versionable and checked-in or is non-versionable but its nearest versionable
     *         ancestor is checked-in.
     * @throws ConstraintViolationException if existing child nodes or properties (or the lack of sufficient child nodes or
     *         properties) prevent this node from satisfying the definition of the new primary type
     * @throws LockException if a lock prevents the modification of the primary type
     * @throws RepositoryException if any other error occurs
     */
    public void setPrimaryType( String nodeTypeName )
        throws NoSuchNodeTypeException, VersionException, ConstraintViolationException, LockException, RepositoryException {
        checkSession();

        if (this.isLocked() && !getLock().isLockOwningSession()) {
            throw new LockException(JcrI18n.lockTokenNotHeld.text(this.location));
        }

        if (!isCheckedOut()) {
            throw new VersionException(JcrI18n.nodeIsCheckedIn.text(getPath()));
        }

        JcrNodeType nodeType = session().nodeTypeManager().getNodeType(nodeTypeName);

        if (nodeType.equals(getPrimaryNodeType())) return;

        if (nodeType.isMixin()) {
            throw new ConstraintViolationException(JcrI18n.cannotUseMixinTypeAsPrimaryType.text(nodeTypeName));
        }

        throw new ConstraintViolationException(JcrI18n.setPrimaryTypeNotSupported.text());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#addNode(java.lang.String)
     */
    public final javax.jcr.Node addNode( String relPath )
        throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException,
        RepositoryException {
        return addNode(relPath, null, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#addNode(java.lang.String, java.lang.String)
     */
    public final javax.jcr.Node addNode( String relPath,
                                         String primaryNodeTypeName )
        throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException,
        RepositoryException {
        return this.addNode(relPath, primaryNodeTypeName, null);
    }

    /**
     * Adds the a new node with the given primary type (if specified) at the given relative path with the given UUID (if
     * specified).
     * 
     * @param relPath the at which the new node should be created
     * @param primaryNodeTypeName the desired primary type for the new node; null value indicates that the default primary type
     *        from the appropriate definition for this node should be used
     * @param desiredUuid the UUID (for the jcr.uuid property) of this node; may be null
     * @return the newly created node
     * @throws ItemExistsException if an item at the specified path already exists and same-name siblings are not allowed.
     * @throws PathNotFoundException if the specified path implies intermediary nodes that do not exist.
     * @throws VersionException not thrown at this time, but included for compatibility with the specification
     * @throws ConstraintViolationException if the change would violate a node type or implementation-specific constraint.
     * @throws LockException not thrown at this time, but included for compatibility with the specification
     * @throws RepositoryException if another error occurs
     * @see #addNode(String, String)
     */
    final AbstractJcrNode addNode( String relPath,
                                   String primaryNodeTypeName,
                                   UUID desiredUuid )
        throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException,
        RepositoryException {
        checkSession();

        if (isLocked() && !getLock().isLockOwningSession()) {
            throw new LockException(JcrI18n.lockTokenNotHeld.text(this.location));
        }

        // Determine the path ...
        NodeEditor editor = null;
        Path path = null;
        try {
            path = cache.pathFactory().create(relPath);
        } catch (org.modeshape.graph.property.ValueFormatException e) {
            throw new RepositoryException(JcrI18n.invalidPathParameter.text(relPath, "relPath"));
        }
        if (path.size() == 0) {
            throw new RepositoryException(JcrI18n.invalidPathParameter.text(relPath, "relPath"));
        }
        if (path.isIdentifier()) {
            throw new RepositoryException(JcrI18n.invalidPathParameter.text(relPath, "relPath"));
        }
        if (path.getLastSegment().getIndex() > 1 || relPath.endsWith("]")) {
            throw new RepositoryException(JcrI18n.invalidPathParameter.text(relPath, "relPath"));
        }
        if (path.size() > 1) {
            // The only segment in the path is the child name ...
            Path parentPath = path.getParent();
            try {
                // Find the parent node ...
                Node<JcrNodePayload, JcrPropertyPayload> parentOfNewNode = cache.findNode(nodeId, location.getPath(), parentPath);
                editor = cache.getEditorFor(parentOfNewNode);
            } catch (RepositoryException e) {
                // We're going to throw an exception ... the question is which one ...
                try {
                    Node<JcrNodePayload, JcrPropertyPayload> grandparent;
                    if (parentPath.size() > 1) {
                        // Per the TCK, if relPath references a property, then we have to throw a ConstraintViolationException
                        // So, if we can't find the parent, try for the parent's parent and see if the last segment of the
                        // parent's
                        // path contains a property ...
                        Path grandparentPath = parentPath.getParent();
                        assert grandparentPath != null;

                        grandparent = cache.findNode(nodeId, location.getPath(), grandparentPath); // throws
                        // PathNotFoundException
                    } else {
                        grandparent = this.nodeInfo();
                    }

                    if (grandparent.getProperty(parentPath.getLastSegment().getName()) != null) {
                        // Need to throw a ConstraintViolationException since the request was to add a child to
                        // a property ...
                        throw new ConstraintViolationException(JcrI18n.invalidPathParameter.text(relPath, "relPath"));
                    }
                } catch (PathNotFoundException e2) {
                    // eat, since the original exception is what we want ...
                }

                // Otherwise, just throw the PathNotFoundException ...
                throw e;
            }
        } else {
            assert path.size() == 1;
            editor = editor();
        }
        Name childName = path.getLastSegment().getName();

        // Determine the name for the primary node type
        Name childPrimaryTypeName = null;
        if (primaryNodeTypeName != null) {
            try {
                childPrimaryTypeName = cache.nameFactory().create(primaryNodeTypeName);
            } catch (org.modeshape.graph.property.ValueFormatException e) {
                throw new RepositoryException(JcrI18n.invalidNodeTypeNameParameter.text(primaryNodeTypeName,
                                                                                        "primaryNodeTypeName"));
            }
            if (INTERNAL_NODE_TYPE_NAMES.contains(childPrimaryTypeName)) {
                String workspaceName = getSession().getWorkspace().getName();
                String childPath = cache.readable(path);
                throw new ConstraintViolationException(
                                                       JcrI18n.unableToCreateNodeWithInternalPrimaryType.text(primaryNodeTypeName,
                                                                                                              childPath,
                                                                                                              workspaceName));

            }
        }

        // Create the child ...
        return editor.createChild(childName, desiredUuid, childPrimaryTypeName);
    }

    /**
     * Performs a "best effort" check on whether a node can be added at the given relative path from this node with the given
     * primary node type (if one is specified).
     * <p>
     * Note that a result of {@code true} from this method does not guarantee that a call to {@code #addNode(String, String)} with
     * the same arguments will succeed, but a result of {@code false} guarantees that it would fail (assuming that the current
     * repository state does not change).
     * </p>
     * 
     * @param relPath the relative path at which the node would be added; may not be null
     * @param primaryNodeTypeName the primary type that would be used for the node; null indicates that a default primary type
     *        should be used if possible
     * @return false if the node could not be added for any reason; true if the node <i>might</i> be able to be added
     * @throws RepositoryException if an error occurs accessing the repository
     */
    final boolean canAddNode( String relPath,
                              String primaryNodeTypeName ) throws RepositoryException {
        CheckArg.isNotEmpty(relPath, relPath);
        checkSession();

        if (isLocked() && !getLock().isLockOwningSession()) {
            return false;
        }

        // Determine the path ...
        Path path = null;
        try {
            path = cache.pathFactory().create(relPath);
        } catch (org.modeshape.graph.property.ValueFormatException e) {
            return false;
        }
        if (path.size() == 0) {
            return false;
        }
        if (path.isIdentifier()) {
            return false;
        }
        if (path.getLastSegment().getIndex() > 1 || relPath.endsWith("]")) {
            return false;
        }
        if (path.size() > 1) {
            // The only segment in the path is the child name ...
            Path parentPath = path.getParent();
            try {
                // Find the parent node ...
                cache.findNode(nodeId, location.getPath(), parentPath);
            } catch (RepositoryException e) {
                return false;
            }
        }

        // Determine the name for the primary node type
        if (primaryNodeTypeName != null) {
            if (!session().nodeTypeManager().hasNodeType(primaryNodeTypeName)) return false;

            JcrNodeType nodeType = session().nodeTypeManager().getNodeType(primaryNodeTypeName);
            if (nodeType.isAbstract()) return false;
            if (nodeType.isMixin()) return false;
            if (INTERNAL_NODE_TYPE_NAMES.contains(nodeType.getInternalName())) return false;
        }

        return true;
    }

    protected final Property removeExistingValuedProperty( String name ) throws ConstraintViolationException, RepositoryException {
        AbstractJcrProperty property = cache.findJcrProperty(nodeId, location.getPath(), nameFrom(name));
        if (property != null) {
            property.remove();
            return property;
        }

        /*
         * Return without throwing an exception to match JR behavior.  This is also in conformance with the spec.
         * This is a fix for MODE-976.
         */
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, boolean)
     */
    public final Property setProperty( String name,
                                       boolean value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        return editor().setProperty(nameFrom(name), valueFrom(PropertyType.BOOLEAN, value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, Binary)
     */
    @Override
    public Property setProperty( String name,
                                 Binary value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return editor().setProperty(nameFrom(name), valueFrom(PropertyType.BINARY, value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, BigDecimal)
     */
    @Override
    public Property setProperty( String name,
                                 BigDecimal value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return editor().setProperty(nameFrom(name), valueFrom(PropertyType.DECIMAL, value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, java.util.Calendar)
     */
    public final Property setProperty( String name,
                                       Calendar value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();

        if (value == null) {
            return removeExistingValuedProperty(name);
        }

        return editor().setProperty(nameFrom(name), valueFrom(value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, double)
     */
    public final Property setProperty( String name,
                                       double value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        return editor().setProperty(nameFrom(name), valueFrom(PropertyType.DOUBLE, value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, java.io.InputStream)
     */
    public final Property setProperty( String name,
                                       InputStream value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        if (value == null) {
            return removeExistingValuedProperty(name);
        }

        return editor().setProperty(nameFrom(name), valueFrom(value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, long)
     */
    public final Property setProperty( String name,
                                       long value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        return editor().setProperty(nameFrom(name), valueFrom(PropertyType.LONG, value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Node)
     */
    public final Property setProperty( String name,
                                       javax.jcr.Node value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        if (value == null) {
            return removeExistingValuedProperty(name);
        }

        return editor().setProperty(nameFrom(name), valueFrom(value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, java.lang.String)
     */
    public final Property setProperty( String name,
                                       String value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        if (value == null) {
            return removeExistingValuedProperty(name);
        }

        return editor().setProperty(nameFrom(name), valueFrom(PropertyType.STRING, value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, java.lang.String, int)
     */
    public final Property setProperty( String name,
                                       String value,
                                       int type )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        if (value == null) {
            return removeExistingValuedProperty(name);
        }

        return editor().setProperty(nameFrom(name), valueFrom(type, value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, java.lang.String[])
     */
    public final Property setProperty( String name,
                                       String[] values )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        if (values == null) {
            return removeExistingValuedProperty(name);
        }

        return editor().setProperty(nameFrom(name), valuesFrom(PropertyType.STRING, values), PropertyType.UNDEFINED);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, java.lang.String[], int)
     */
    public final Property setProperty( String name,
                                       String[] values,
                                       int type )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        if (values == null) {
            return removeExistingValuedProperty(name);
        }

        return editor().setProperty(nameFrom(name), valuesFrom(type, values), PropertyType.UNDEFINED);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Value)
     */
    public final Property setProperty( String name,
                                       Value value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        if (value == null) {
            return removeExistingValuedProperty(name);
        }

        return editor().setProperty(nameFrom(name), (JcrValue)value);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Value, int)
     */
    public final Property setProperty( String name,
                                       Value value,
                                       int type )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        if (value == null) {
            return removeExistingValuedProperty(name);
        }

        return editor().setProperty(nameFrom(name), ((JcrValue)value).asType(type));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Value[])
     */
    public final Property setProperty( String name,
                                       Value[] values )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        if (values == null) {
            // If there is an existing property, then remove it ...
            return removeExistingValuedProperty(name);
        }

        return setProperty(name, values, PropertyType.UNDEFINED);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Value[], int)
     */
    public final Property setProperty( String name,
                                       Value[] values,
                                       int type )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        checkSession();
        if (values == null) {
            // If there is an existing property, then remove it ...
            return removeExistingValuedProperty(name);
        }

        // Set the value, perhaps to an empty array ...
        return editor().setProperty(nameFrom(name), values, type);
    }

    /**
     * Throw a {@link ConstraintViolationException} if this node is protected (based on the its node definition).
     * 
     * @throws ConstraintViolationException if this node's definition indicates that the node is protected
     * @throws RepositoryException if an error occurs retrieving the definition for this node
     */
    private void checkNotProtected() throws ConstraintViolationException, RepositoryException {
        JcrNodeDefinition nodeDefn = cache.nodeTypes().getNodeDefinition(nodeInfo().getPayload().getDefinitionId());
        if (nodeDefn.isProtected()) {
            throw new ConstraintViolationException(JcrI18n.cannotRemoveItemWithProtectedDefinition.text(getPath()));
        }
    }

    final JcrVersionManager versionManager() {
        return session().workspace().versionManager();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#isCheckedOut()
     */
    public final boolean isCheckedOut() throws RepositoryException {
        checkSession();
        return editor().isCheckedOut();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#checkin()
     */
    public final Version checkin() throws RepositoryException {
        return versionManager().checkin(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#checkout()
     */
    public final void checkout() throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        versionManager().checkout(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#merge(java.lang.String, boolean)
     */
    public final NodeIterator merge( String srcWorkspace,
                                     boolean bestEffort ) throws ConstraintViolationException, RepositoryException {
        CheckArg.isNotNull(srcWorkspace, "source workspace name");

        checkNotProtected();

        return versionManager().merge(this, srcWorkspace, bestEffort, false);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#cancelMerge(javax.jcr.version.Version)
     */
    public final void cancelMerge( Version version ) throws RepositoryException {
        versionManager().cancelMerge(this, version);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#doneMerge(javax.jcr.version.Version)
     */
    public final void doneMerge( Version version ) throws RepositoryException {
        versionManager().doneMerge(this, version);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getVersionHistory()
     */
    public final JcrVersionHistoryNode getVersionHistory() throws RepositoryException {
        return versionManager().getVersionHistory(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getBaseVersion()
     */
    public final JcrVersionNode getBaseVersion() throws RepositoryException {
        checkSession();

        // This can happen if the versionable type was added to the node, but it hasn't been saved yet
        if (!hasProperty(JcrLexicon.BASE_VERSION)) {
            throw new UnsupportedRepositoryOperationException(JcrI18n.requiresVersionable.text());
        }

        return (JcrVersionNode)session().getNodeByUUID(getProperty(JcrLexicon.BASE_VERSION).getString());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#restore(java.lang.String, boolean)
     */
    public final void restore( String versionName,
                               boolean removeExisting ) throws RepositoryException {
        restore(getVersionHistory().getVersion(versionName), removeExisting);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#restore(javax.jcr.version.Version, boolean)
     */
    public final void restore( Version version,
                               boolean removeExisting ) throws RepositoryException {
        try {
            checkNotProtected();
        } catch (ConstraintViolationException cve) {
            throw new UnsupportedRepositoryOperationException(cve);
        }
        versionManager().restore(path(), version, null, removeExisting);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#restore(javax.jcr.version.Version, java.lang.String, boolean)
     */
    public final void restore( Version version,
                               String relPath,
                               boolean removeExisting ) throws RepositoryException {
        checkNotProtected();

        PathFactory pathFactory = context().getValueFactories().getPathFactory();
        Path relPathAsPath = pathFactory.create(relPath);
        if (relPathAsPath.isAbsolute()) throw new RepositoryException(JcrI18n.invalidRelativePath.text(relPath));
        Path actualPath = pathFactory.create(path(), relPathAsPath).getCanonicalPath();

        versionManager().restore(actualPath, version, null, removeExisting);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#restoreByLabel(java.lang.String, boolean)
     */
    public final void restoreByLabel( String versionLabel,
                                      boolean removeExisting ) throws RepositoryException {
        restore(getVersionHistory().getVersionByLabel(versionLabel), removeExisting);
    }

    /**
     * {@inheritDoc}
     * 
     * @return <code>false</code>
     * @see javax.jcr.Node#holdsLock()
     */
    public final boolean holdsLock() throws RepositoryException {
        checkSession();
        return lockManager().holdsLock(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @return <code>false</code>
     * @see javax.jcr.Node#isLocked()
     */
    public final boolean isLocked() throws LockException, RepositoryException {
        return lockManager().isLocked(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#lock(boolean, boolean)
     */
    public final Lock lock( boolean isDeep,
                            boolean isSessionScoped ) throws LockException, RepositoryException {
        checkSession();
        return lockManager().lock(this, isDeep, isSessionScoped, -1L, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#unlock()
     */
    public final void unlock() throws LockException, RepositoryException {
        checkSession();
        lockManager().unlock(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getLock()
     */
    public final Lock getLock() throws LockException, RepositoryException {
        checkSession();
        return lockManager().getLock(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#isModified()
     */
    public final boolean isModified() {
        try {
            checkSession();
            Node<JcrNodePayload, JcrPropertyPayload> node = nodeInfo();
            // Considered modified if *not* new but changed
            return !node.isNew() && node.isChanged(true);
        } catch (RepositoryException re) {
            throw new IllegalStateException(re);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#isNew()
     */
    public final boolean isNew() {
        try {
            checkSession();
            return nodeInfo().isNew();
        } catch (RepositoryException re) {
            throw new IllegalStateException(re);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getCorrespondingNodePath(java.lang.String)
     */
    public final String getCorrespondingNodePath( String workspaceName )
        throws NoSuchWorkspaceException, ItemNotFoundException, RepositoryException {
        CheckArg.isNotNull(workspaceName, "workspace name");
        checkSession();
        NamespaceRegistry namespaces = this.context().getNamespaceRegistry();
        return correspondingNodePath(workspaceName).getString(namespaces);
    }

    protected final Path correspondingNodePath( String workspaceName )
        throws NoSuchWorkspaceException, ItemNotFoundException, RepositoryException {
        assert workspaceName != null;
        NamespaceRegistry namespaces = this.context().getNamespaceRegistry();

        // Find the closest ancestor (including this node) that is referenceable ...
        AbstractJcrNode referenceableRoot = this;
        while (!referenceableRoot.isNodeType(JcrMixLexicon.REFERENCEABLE.getString(namespaces))) {
            referenceableRoot = referenceableRoot.getParent();
        }

        // Find the relative path from the nearest referenceable node to this node (or null if this node is referenceable) ...
        Path relativePath = path().equals(referenceableRoot.path()) ? null : path().relativeTo(referenceableRoot.path());
        UUID uuid = UUID.fromString(referenceableRoot.getUUID());
        return this.cache.getPathForCorrespondingNode(workspaceName, uuid, relativePath);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#update(java.lang.String)
     */
    public final void update( String srcWorkspaceName ) throws NoSuchWorkspaceException, RepositoryException {
        CheckArg.isNotNull(srcWorkspaceName, "workspace name");
        checkSession();

        if (session().hasPendingChanges()) {
            throw new InvalidItemStateException(JcrI18n.noPendingChangesAllowed.text());
        }

        checkNotProtected();

        Path correspondingPath = null;
        try {
            correspondingPath = correspondingNodePath(srcWorkspaceName);
        } catch (ItemNotFoundException infe) {
            return;
        }

        // Need to force remove in case this node is not referenceable
        cache.graphSession().immediateClone(correspondingPath, srcWorkspaceName, path(), true, true);

        session().refresh(false);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedRepositoryOperationException always
     * @see javax.jcr.Node#orderBefore(java.lang.String, java.lang.String)
     */
    public final void orderBefore( String srcChildRelPath,
                                   String destChildRelPath ) throws UnsupportedRepositoryOperationException, RepositoryException {
        checkSession();
        // This implementation is correct, except for not calling the SessionCache or graph layer to do the re-order
        if (!getPrimaryNodeType().hasOrderableChildNodes()) {
            throw new UnsupportedRepositoryOperationException(
                                                              JcrI18n.notOrderable.text(getPrimaryNodeType().getName(), getPath()));
        }

        PathFactory pathFactory = this.cache.pathFactory();
        Path srcPath = pathFactory.create(srcChildRelPath);
        if (srcPath.isAbsolute()) {
            // Not a relative path ...
            throw new IllegalArgumentException(JcrI18n.invalidPathParameter.text(srcChildRelPath, "relativePath"));
        }
        if (srcPath.isAbsolute() || srcPath.size() != 1) {
            throw new ItemNotFoundException(JcrI18n.pathNotFound.text(srcPath.getString(namespaces()), cache.workspaceName()));
        }
        // getLastSegment should return the only segment, since we verified that size() == 1
        Path.Segment sourceSegment = srcPath.getLastSegment();
        try {
            nodeInfo().getChild(sourceSegment);
        } catch (org.modeshape.graph.property.PathNotFoundException e) {
            String workspaceName = this.cache.workspaceName();
            throw new ItemNotFoundException(JcrI18n.pathNotFound.text(srcPath, workspaceName));
        }

        Path.Segment destSegment = null;

        if (destChildRelPath != null) {
            Path destPath = pathFactory.create(destChildRelPath);
            if (destPath.isAbsolute()) {
                // Not a relative path ...
                throw new IllegalArgumentException(JcrI18n.invalidPathParameter.text(destChildRelPath, "relativePath"));
            }
            if (destPath.size() != 1) {
                throw new ItemNotFoundException(
                                                JcrI18n.pathNotFound.text(destPath.getString(namespaces()), cache.workspaceName()));
            }

            destSegment = destPath.getLastSegment();

            // getLastSegment should return the only segment, since we verified that size() == 1
            try {
                nodeInfo().getChild(destSegment);
            } catch (org.modeshape.graph.property.PathNotFoundException e) {
                String workspaceName = this.cache.session().getWorkspace().getName();
                throw new ItemNotFoundException(JcrI18n.pathNotFound.text(destPath, workspaceName));
            }
        }

        this.editor().orderChildBefore(sourceSegment, destSegment);
    }

    protected static List<Object> createPatternsFor( String[] namePatterns ) throws RepositoryException {
        List<Object> patterns = new LinkedList<Object>();
        for (String stringPattern : namePatterns) {
            stringPattern = stringPattern.trim();
            int length = stringPattern.length();
            if (length == 0) continue;
            if (stringPattern.indexOf("*") == -1) {
                // Doesn't use wildcard, so use String not Pattern
                patterns.add(stringPattern);
            } else {
                // We need to escape the regular expression characters ...
                StringBuilder sb = new StringBuilder(length);
                for (int i = 0; i != length; i++) {
                    char c = stringPattern.charAt(i);
                    switch (c) {
                        // Per the spec, the the following characters are not allowed in patterns:
                        case '/':
                        case '[':
                        case ']':
                        case '\'':
                        case '"':
                        case '|':
                        case '\t':
                        case '\n':
                        case '\r':
                            String msg = JcrI18n.invalidNamePattern.text(c, stringPattern);
                            throw new RepositoryException(msg);
                            // The following characters must be escaped when used in regular expressions ...
                        case '?':
                        case '(':
                        case ')':
                        case '$':
                        case '^':
                        case '.':
                        case '{':
                        case '}':
                        case '\\':
                            sb.append("\\");
                            sb.append(c);
                            break;
                        case '*':
                            // replace with the regular expression wildcard
                            sb.append(".*");
                            break;
                        default:
                            sb.append(c);
                            break;
                    }
                }
                String escapedString = sb.toString();
                Pattern pattern = Pattern.compile(escapedString);
                patterns.add(pattern);
            }
        }
        return patterns;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#refresh(boolean)
     */
    public void refresh( boolean keepChanges ) throws RepositoryException {
        checkSession();
        this.cache.refresh(this.nodeId, location.getPath(), keepChanges);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#save()
     */
    public void save() throws RepositoryException {
        checkSession();
        session().checkReferentialIntegrityOfChanges(this);
        cache.save(nodeId, location.getPath());
    }

    @Override
    public String toString() {

        try {
            PropertyIterator iter = this.getProperties();
            StringBuffer propertyBuff = new StringBuffer();
            while (iter.hasNext()) {
                AbstractJcrProperty prop = (AbstractJcrProperty)iter.nextProperty();
                propertyBuff.append(prop).append(", ");
            }
            return this.getPath() + " {" + propertyBuff.toString() + "}";
        } catch (RepositoryException re) {
            return re.getMessage();
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof AbstractJcrNode) {
            AbstractJcrNode that = (AbstractJcrNode)obj;
            if (this.cache != that.cache) return false;
            return this.location.equals(that.location);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return HashCode.compute(cache, location.getUuid());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#followLifecycleTransition(java.lang.String)
     */
    @SuppressWarnings( "unused" )
    @Override
    public void followLifecycleTransition( String transition )
        throws UnsupportedRepositoryOperationException, InvalidLifecycleTransitionException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getAllowedLifecycleTransistions()
     */
    @Override
    public String[] getAllowedLifecycleTransistions() throws UnsupportedRepositoryOperationException, RepositoryException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getSharedSet()
     */
    @Override
    public NodeIterator getSharedSet() throws RepositoryException {
        if (isShareable()) {
            // Find the nodes that make up this shared set ...
            return sharedSet();
        }
        // Otherwise, the shared set is just this node ...
        return new JcrSingleNodeIterator(this);
    }

    /**
     * Find all of the {@link javax.jcr.Node}s that make up the shared set.
     * 
     * @return the query result over the nodes in the node set; never null, but possibly empty if the node given by the identifier
     *         does not exist or is not a shareable node, or possibly of size 1 if the node given by the identifier does exist and
     *         is shareable but has no other nodes in the shared set
     * @throws RepositoryException if there is a problem executing the query or finding the shared set
     */
    NodeIterator sharedSet() throws RepositoryException {
        AbstractJcrNode original = this;
        String identifierOfSharedNode = getIdentifier();
        if (this instanceof JcrSharedNode) {
            original = ((JcrSharedNode)this).originalNode();
        }
        // Execute a query that will report all proxy nodes ...
        QueryBuilder builder = new QueryBuilder(context().getValueFactories().getTypeSystem());
        QueryCommand query = builder.select("jcr:primaryType")
                                    .from("mode:share")
                                    .where()
                                    .referenceValue("mode:share", "mode:sharedUuid")
                                    .isEqualTo(identifierOfSharedNode)
                                    .end()
                                    .query();
        Query jcrQuery = session().workspace().queryManager().createQuery(query);
        QueryResult result = jcrQuery.execute();
        // And combine the results ...
        return new JcrNodeIterator(original, result.getNodes());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#removeShare()
     */
    @Override
    public void removeShare() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (isShareable()) {
            // Get the nodes in the shared set ...
            NodeIterator sharedSetNodes = sharedSet();
            long sharedSetSize = sharedSetNodes.getSize(); // computed w/o respect for privileges
            if (sharedSetSize <= 1) {
                // There aren't any other nodes in the shared set, so simply remove this node ...
                doRemove();
                return;
            }
            // Find the second node in the shared set that is not this object ...
            AbstractJcrNode originalNode = (AbstractJcrNode)sharedSetNodes.nextNode();
            if (originalNode == this) {
                // We need to move this node into the first proxy ...
                JcrSharedNode firstProxy = (JcrSharedNode)sharedSetNodes.nextNode();
                assert !this.isRoot();
                assert !firstProxy.isRoot();
                boolean sameParent = firstProxy.getParent().equals(this.getParent());
                NodeEditor parentEditor = firstProxy.editorForParent();
                if (sameParent) {
                    // Move this node to just before the other shareable node ...
                    parentEditor.orderChildBefore(this.segment(), firstProxy.segment());
                    // And finally remove the first proxy ...
                    firstProxy.doRemove();
                } else {
                    // Find the node immediately following the proxy ...
                    Node<JcrNodePayload, JcrPropertyPayload> proxyNode = firstProxy.proxyInfo();
                    Node<JcrNodePayload, JcrPropertyPayload> nextChild = parentEditor.node().getChildAfter(proxyNode);
                    Name newName = proxyNode.getName();
                    // Remove the first proxy ...
                    firstProxy.doRemove();
                    // Move this node to the new parent ...
                    Node<JcrNodePayload, JcrPropertyPayload> newNode = parentEditor.moveToBeChild(this, newName);
                    if (nextChild != null) {
                        // And place this node where the first proxy was (just before the 'nextChild') ...
                        parentEditor.orderChildBefore(newNode.getSegment(), nextChild.getSegment());
                    }
                }
            } else {
                // We can just remove this proxy ...
                doRemove();
            }
            return;
        }
        // If we get to here, either there are no other nodes in the shared set or this node is a non-shareable node,
        // so simply remove this node (per section 14.2 of the JCR 2.0 specification) ...
        doRemove();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#removeSharedSet()
     */
    @Override
    public void removeSharedSet() throws VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (isShareable()) {
            // Remove all of the node is the shared set ...
            NodeIterator sharedSetNodes = sharedSet();
            while (sharedSetNodes.hasNext()) {
                AbstractJcrNode nodeInSharedSet = (AbstractJcrNode)sharedSetNodes.nextNode();
                nodeInSharedSet.doRemove();
            }
        } else {
            // Per section 14.2 of the JCR 2.0 specification:
            // "In cases where the shared set consists of a single node, or when these methods are
            // called on a non-shareable node, their behavior is identical to Node.remove()."
            doRemove();
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * According to Section 14.3 of the JCR 2.0 specification, an implementation may choose whether the {@link Item#remove()}
     * method (and the {@link Session#removeItem(String)} method) behaves as a {@link javax.jcr.Node#removeShare()} or
     * {@link javax.jcr.Node#removeSharedSet()}. {@link javax.jcr.Node#removeShare()} just removes this node from the shared set,
     * whereas {@link javax.jcr.Node#removeSharedSet()} removes all nodes in the shared set, including the original shared node.
     * </p>
     * <p>
     * ModeShape implements {@link Item#remove()} of a shared node as simply removing this node from the shared set. In other
     * words, this method is equivalent to calling {@link #removeShare()}.
     * </p>
     * 
     * @see javax.jcr.Item#remove()
     */
    @Override
    public void remove()
        throws VersionException, LockException, ConstraintViolationException, AccessDeniedException, RepositoryException {
        // Since this node might be shareable, we want to implement 'remove()' by calling 'removeShare()',
        // which will behave correctly even if it is not shareable ...
        removeShare();
    }

    /**
     * Perform a real remove of this node.
     * 
     * @throws VersionException
     * @throws LockException
     * @throws ConstraintViolationException
     * @throws AccessDeniedException
     * @throws RepositoryException
     */
    protected abstract void doRemove()
        throws VersionException, LockException, ConstraintViolationException, AccessDeniedException, RepositoryException;

}
