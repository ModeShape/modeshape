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
import java.security.AccessControlException;
import java.util.ArrayList;
import java.util.Arrays;
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
import javax.jcr.InvalidItemStateException;
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
import javax.jcr.version.OnParentVersionAction;
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import net.jcip.annotations.Immutable;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.text.Jsr283Encoder;
import org.modeshape.common.text.TextEncoder;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NamespaceRegistry;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.Reference;
import org.modeshape.graph.property.UuidFactory;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
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

    private static final TextEncoder NODE_ENCODER = new Jsr283Encoder();

    private static final NodeType[] EMPTY_NODE_TYPES = new NodeType[] {};
    private static final Object[] EMPTY_OBJECT_ARRAY = new Object[0];

    protected final NodeId nodeId;
    protected final Location location;

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

    final Name name() throws RepositoryException {
        return nodeInfo().getName();
    }

    final Path.Segment segment() throws RepositoryException {
        return nodeInfo().getSegment();
    }

    final Node<JcrNodePayload, JcrPropertyPayload> nodeInfo()
        throws ItemNotFoundException, AccessDeniedException, RepositoryException {
        return cache.findNode(nodeId, location.getPath());
    }

    final NodeEditor editorForParent() throws RepositoryException {
        try {
            Node<JcrNodePayload, JcrPropertyPayload> parent = nodeInfo().getParent();
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
        return new JcrValue(cache.factories(), cache, propertyType, value);
    }

    final JcrValue valueFrom( Calendar value ) {
        ValueFactories factories = cache.factories();
        DateTime dateTime = factories.getDateFactory().create(value);
        return new JcrValue(factories, cache, PropertyType.DATE, dateTime);
    }

    final JcrValue valueFrom( InputStream value ) {
        ValueFactories factories = cache.factories();
        Binary binary = factories.getBinaryFactory().create(value);
        return new JcrValue(factories, cache, PropertyType.DATE, binary);
    }

    final JcrValue valueFrom( javax.jcr.Node value ) throws UnsupportedRepositoryOperationException, RepositoryException {
        ValueFactories factories = cache.factories();
        String uuid = factories.getStringFactory().create(value.getUUID());
        return new JcrValue(factories, cache, PropertyType.REFERENCE, uuid);
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

    UUID uuid() throws RepositoryException {
        PropertyInfo<JcrPropertyPayload> uuidProp = nodeInfo().getProperty(JcrLexicon.UUID);
        if (uuidProp == null) {
            uuidProp = nodeInfo().getProperty(ModeShapeLexicon.UUID);
        }
        assert uuidProp != null;
        assert !uuidProp.getProperty().isEmpty();
        return context().getValueFactories().getUuidFactory().create(uuidProp.getProperty().getFirstValue());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getUUID()
     */
    public String getUUID() throws RepositoryException {
        // Return "jcr:uuid" only if node is referenceable
        if (!isReferenceable()) {
            throw new UnsupportedRepositoryOperationException(JcrI18n.nodeNotReferenceable.text());
        }

        return uuid().toString();
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
        return cache.isNodeType(nodeInfo(), nodeTypeName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getDefinition()
     */
    public NodeDefinition getDefinition() throws RepositoryException {
        NodeDefinitionId definitionId = nodeInfo().getPayload().getDefinitionId();
        return session().nodeTypeManager().getNodeDefinition(definitionId);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getPrimaryNodeType()
     */
    public JcrNodeType getPrimaryNodeType() throws RepositoryException {
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
        if (relativePath.indexOf('/') >= 0) {
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
        return nodeInfo().getProperty(name) != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getProperties()
     */
    public final PropertyIterator getProperties() throws RepositoryException {
        return new JcrPropertyIterator(cache.findJcrPropertiesFor(nodeId, location.getPath()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getProperties(java.lang.String)
     */
    public PropertyIterator getProperties( String namePattern ) throws RepositoryException {
        CheckArg.isNotNull(namePattern, "namePattern");
        namePattern = namePattern.trim();
        if (namePattern.length() == 0) return new JcrEmptyPropertyIterator();
        Collection<AbstractJcrProperty> properties = cache.findJcrPropertiesFor(nodeId, location.getPath());
        if ("*".equals(namePattern)) return new JcrPropertyIterator(properties);

        // Figure out the patterns for each of the different disjunctions in the supplied pattern ...
        List<Object> patterns = createPatternsFor(namePattern);

        // Go through the properties and remove any property that doesn't match a pattern ...
        boolean foundMatch = true;
        Collection<AbstractJcrProperty> matchingProperties = new LinkedList<AbstractJcrProperty>();
        Iterator<AbstractJcrProperty> iter = properties.iterator();
        while (iter.hasNext()) {
            AbstractJcrProperty property = iter.next();
            String propName = property.getName();
            assert foundMatch == true;
            for (Object patternOrMatch : patterns) {
                if (patternOrMatch instanceof Pattern) {
                    Pattern pattern = (Pattern)patternOrMatch;
                    if (pattern.matcher(propName).matches()) break;
                } else {
                    String match = (String)patternOrMatch;
                    if (propName.equals(match)) break;
                }
                // No pattern matched ...
                foundMatch = false;
            }
            if (foundMatch) {
                matchingProperties.add(property);
            } else {
                foundMatch = true; // for the next iteration ..
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
        QueryCommand query = builder.select("jcr:primaryType").fromAllNodesAs("allNodes").where().referenceValue("allNodes").isEqualTo(uuid).end().limit(maxNumberOfNodes).query();
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
        if (!this.isReferenceable()) {
            // This node is not referenceable, so it cannot have any references to it ...
            return new JcrEmptyPropertyIterator();
        }
        NodeIterator iter = referencingNodes(Integer.MAX_VALUE);
        if (!iter.hasNext()) {
            return new JcrEmptyPropertyIterator();
        }
        String uuid = getUUID();
        List<Property> references = new LinkedList<Property>();
        while (iter.hasNext()) {
            javax.jcr.Node node = iter.nextNode();
            // Go through the properties and look for reference properties that have a value of this node's UUID ...
            PropertyIterator propIter = node.getProperties();
            while (propIter.hasNext()) {
                Property prop = propIter.nextProperty();
                // Look at the definition's required type ...
                int propType = prop.getDefinition().getRequiredType();
                if (propType == PropertyType.REFERENCE || propType == PropertyType.UNDEFINED || propType == PropertyType.STRING) {
                    if (prop.getDefinition().isMultiple()) {
                        for (Value value : prop.getValues()) {
                            if (uuid.equals(value.getString())) {
                                references.add(prop);
                                break;
                            }
                        }
                    } else {
                        Value value = prop.getValue();
                        if (uuid.equals(value.getString())) {
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
    public final Property getProperty( String relativePath ) throws RepositoryException {
        CheckArg.isNotEmpty(relativePath, "relativePath");
        int indexOfFirstSlash = relativePath.indexOf('/');
        if (indexOfFirstSlash == 0) {
            // Not a relative path ...
            throw new IllegalArgumentException(JcrI18n.invalidPathParameter.text(relativePath, "relativePath"));
        }
        Name propertyName = null;
        if (indexOfFirstSlash != -1) {
            // We know it's a relative path with more than one segment ...
            Path path = pathFrom(relativePath).getNormalizedPath();
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
        if (relativePath.equals(".")) return true;
        if (relativePath.equals("..")) return isRoot() ? false : true;
        int indexOfFirstSlash = relativePath.indexOf('/');
        if (indexOfFirstSlash == 0) {
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
        return nodeInfo().getChildrenCount() > 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>relativePath</code> is empty or <code>null</code>.
     * @see javax.jcr.Node#getNode(java.lang.String)
     */
    public final AbstractJcrNode getNode( String relativePath ) throws RepositoryException {
        CheckArg.isNotEmpty(relativePath, "relativePath");
        if (relativePath.equals(".")) return this;
        if (relativePath.equals("..")) return this.getParent();
        int indexOfFirstSlash = relativePath.indexOf('/');
        if (indexOfFirstSlash == 0) {
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
                AbstractJcrItem item = cache.findJcrNode(nodeId, location.getPath(), path);
                if (item instanceof javax.jcr.Node) {
                    return (AbstractJcrNode)item;
                }
                I18n msg = JcrI18n.nodeNotFoundAtPathRelativeToReferenceNode;
                throw new PathNotFoundException(msg.text(relativePath, getPath(), cache.workspaceName()));
            }
            segment = path.getLastSegment();
        } else {
            segment = segmentFrom(relativePath);
        }
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
        namePattern = namePattern.trim();
        if (namePattern.length() == 0) return new JcrEmptyNodeIterator();
        if ("*".equals(namePattern)) return getNodes();
        List<Object> patterns = createPatternsFor(namePattern);

        // Implementing exact-matching only for now to prototype types as properties
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
        visitor.visit(this);
    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>ModeShape Implementation Notes</b>
     * </p>
     * <p>
     * ModeShape imposes the following additional restrictions on the addition of mixin types in addition to the restrictions
     * provided by the JCR 1.0 specification:
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

        NodeType primaryType = this.getPrimaryNodeType();
        NodeType[] mixinTypes = this.getMixinNodeTypes();

        if (!mixinCandidateType.isMixin()) {
            return false;
        }

        if (mixinCandidateType.conflictsWith(primaryType, mixinTypes)) {
            return false;
        }

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
     * The criteria noted in {@link #canAddMixin(String)} must be satisifed in addition to the criteria defined in the JCR 1.0
     * specification.
     * </p>
     * 
     * @see javax.jcr.Node#addMixin(java.lang.String)
     */
    public final void addMixin( String mixinName ) throws RepositoryException {
        CheckArg.isNotNull(mixinName, "mixinName");
        CheckArg.isNotZeroLength(mixinName, "mixinName");

        JcrNodeType mixinCandidateType = cache.nodeTypes().getNodeType(mixinName);

        // Check this separately since it throws a different type of exception
        if (this.isLocked() && !holdsLock()) {
            throw new LockException(JcrI18n.lockTokenNotHeld.text(this.location));
        }

        if (!isCheckedOut()) {
            throw new VersionException(JcrI18n.nodeIsCheckedIn.text(getPath()));
        }

        if (!canAddMixin(mixinName)) {
            throw new ConstraintViolationException(JcrI18n.cannotAddMixin.text(mixinName));
        }

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
    public final void removeMixin( String mixinName ) throws RepositoryException {

        if (this.isLocked() && !holdsLock()) {
            throw new LockException(JcrI18n.lockTokenNotHeld.text(this.location));
        }

        if (!isCheckedOut()) {
            throw new VersionException(JcrI18n.nodeIsCheckedIn.text(getPath()));
        }

        /*
         * This is a special workaround for o.a.j.test.api.version.VersionText.testRemoveMixin().
         * This test tries to remove the mix:versionable mixin from a node with the primary type
         * nt:version and no mixin types.  It expects a ConstraintViolationException (because nt:version nodes
         * are protected) instead of a NoSuchNodeTypeException (because the node doesn't have that mixin).
         * 
         * Interestingly, o.a.j.test.api.version.VersionHistoryTest.testRemoveMixin tries to remove
         * mix:versionable from a nt:versionHistory node, but accepts either a CVE or a NSNTE.
         */
        if (JcrMixLexicon.VERSIONABLE.getString(context().getNamespaceRegistry()).equals(mixinName)) {
            throw new ConstraintViolationException(JcrI18n.cannotRemoveMixVersionable.text(getPath()));
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

        if (isLocked() && !holdsLock()) {
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
        }

        // Create the child ...
        return editor.createChild(childName, desiredUuid, childPrimaryTypeName);
    }

    protected final Property removeExistingValuedProperty( String name ) throws ConstraintViolationException, RepositoryException {
        AbstractJcrProperty property = cache.findJcrProperty(nodeId, location.getPath(), nameFrom(name));
        if (property != null) {
            property.remove();
            return property;
        }
        // else the property doesn't exist ...
        throw new RepositoryException(JcrI18n.propertyNotFoundOnNode.text(name, getPath(), cache.workspaceName()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, boolean)
     */
    public final Property setProperty( String name,
                                       boolean value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        return editor().setProperty(nameFrom(name), valueFrom(PropertyType.BOOLEAN, value));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, java.util.Calendar)
     */
    public final Property setProperty( String name,
                                       Calendar value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {

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
        if (values == null) {
            // If there is an existing property, then remove it ...
            return removeExistingValuedProperty(name);
        }

        // Set the value, perhaps to an empty array ...
        return editor().setProperty(nameFrom(name), values, type);
    }

    /**
     * Throw an {@link UnsupportedRepositoryOperationException} if this node is not versionable (i.e.,
     * isNodeType(JcrMixLexicon.VERSIONABLE) == false).
     * 
     * @throws UnsupportedRepositoryOperationException if <code>!isNodeType({@link JcrMixLexicon#VERSIONABLE})</code>
     * @throws RepositoryException if an error occurs reading the node types for this node
     */
    private void checkVersionable() throws UnsupportedRepositoryOperationException, RepositoryException {
        if (!isNodeType(JcrMixLexicon.VERSIONABLE)) {
            throw new UnsupportedRepositoryOperationException(JcrI18n.requiresVersionable.text());
        }
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
            // TODO: add message
            throw new ConstraintViolationException(JcrI18n.cannotRemoveItemWithProtectedDefinition.text(getPath()));
        }

    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#isCheckedOut()
     */
    public final boolean isCheckedOut() throws RepositoryException {
        return editor().isCheckedOut();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#checkin()
     */
    public final Version checkin() throws UnsupportedRepositoryOperationException, RepositoryException {
        checkVersionable();

        if (isNew() || isModified()) {
            throw new InvalidItemStateException(JcrI18n.noPendingChangesAllowed.text());
        }

        // Check this separately since it throws a different type of exception
        if (this.isLocked() && !holdsLock()) {
            throw new LockException(JcrI18n.lockTokenNotHeld.text(this.location));
        }

        if (getProperty(JcrLexicon.MERGE_FAILED) != null) {
            throw new VersionException(JcrI18n.pendingMergeConflicts.text(getPath()));
        }

        Property isCheckedOut = getProperty(JcrLexicon.IS_CHECKED_OUT);

        if (!isCheckedOut.getBoolean()) {
            return getBaseVersion();
        }

        PathFactory pathFactory = context().getValueFactories().getPathFactory();
        Name primaryTypeName = getPrimaryTypeName();
        List<Name> mixinTypeNames = getMixinTypeNames();

        UUID jcrUuid = uuid();
        UUID versionUuid = UUID.randomUUID();

        Name nameSegment = context().getValueFactories().getNameFactory().create(jcrUuid.toString());
        Path historyPath = pathFactory.createAbsolutePath(JcrLexicon.SYSTEM, JcrLexicon.VERSION_STORAGE, nameSegment);

        Node<JcrNodePayload, JcrPropertyPayload> historyNode = cache.findNode(null, historyPath);

        Graph systemGraph = session().repository().createSystemGraph(context());
        Graph.Batch systemBatch = systemGraph.batch();
        DateTime now = context().getValueFactories().getDateFactory().create();

        Path versionPath = pathFactory.create(historyPath, nameFrom(NODE_ENCODER.encode(now.getString())));
        AbstractJcrProperty predecessorsProp = getProperty(JcrLexicon.PREDECESSORS);

        systemBatch.create(versionPath).with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.VERSION).and(JcrLexicon.CREATED, now).and(JcrLexicon.UUID,
                                                                                                                             versionUuid).and(predecessorsProp.property()).and();
        Path frozenVersionPath = pathFactory.create(versionPath, JcrLexicon.FROZEN_NODE);
        systemBatch.create(frozenVersionPath).with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FROZEN_NODE).and(JcrLexicon.FROZEN_UUID,
                                                                                                          jcrUuid).and(JcrLexicon.FROZEN_PRIMARY_TYPE,
                                                                                                                       primaryTypeName).and(JcrLexicon.FROZEN_MIXIN_TYPES,
                                                                                                                                            mixinTypeNames).and(versionedPropertiesFor(this)).and();

        int onParentVersion = getDefinition().getOnParentVersion();
        for (NodeIterator childNodes = this.getNodes(); childNodes.hasNext();) {
            AbstractJcrNode childNode = (AbstractJcrNode)childNodes.nextNode();
            versionNodeAt(childNode, frozenVersionPath, systemBatch, onParentVersion);
        }

        PropertyFactory propFactory = context().getPropertyFactory();
        UuidFactory uuidFactory = context().getValueFactories().getUuidFactory();

        for (Object ob : predecessorsProp.property()) {
            UUID predUuid = uuidFactory.create(ob);

            org.modeshape.graph.property.Property successorsProp = systemGraph.getNodeAt(predUuid).getProperty(JcrLexicon.SUCCESSORS);

            List<Object> newSuccessors = new LinkedList<Object>();
            if (successorsProp != null) {
                for (Object successor : successorsProp) {
                    newSuccessors.add(successor);
                }
            }

            newSuccessors.add(versionUuid);

            org.modeshape.graph.property.Property newSuccessorsProp = propFactory.create(JcrLexicon.SUCCESSORS,
                                                                                         newSuccessors.toArray());
            systemBatch.set(newSuccessorsProp).on(predUuid).and();
        }

        systemBatch.execute();
        cache.refresh(historyNode.getNodeId(), historyPath, false);

        AbstractJcrNode newVersion = cache.findJcrNode(Location.create(versionUuid));

        NodeEditor editor = editor();
        editor.setProperty(JcrLexicon.PREDECESSORS,
                           valuesFrom(PropertyType.REFERENCE, EMPTY_OBJECT_ARRAY),
                           PropertyType.REFERENCE,
                           false);
        editor.setProperty(JcrLexicon.BASE_VERSION, valueFrom(newVersion), false);
        editor.setProperty(JcrLexicon.IS_CHECKED_OUT, valueFrom(PropertyType.BOOLEAN, false), false);
        save();

        return new JcrVersionNode(newVersion);
    }

    private void versionNodeAt( AbstractJcrNode node,
                                Path verisonedParentPath,
                                Graph.Batch batch,
                                int onParentVersionAction ) throws RepositoryException {

        Path childPath = context().getValueFactories().getPathFactory().create(verisonedParentPath, node.path().getLastSegment());

        Name primaryTypeName = node.getPrimaryTypeName();
        List<Name> mixinTypeNames = node.getMixinTypeNames();
        UUID uuid = UUID.randomUUID();
        if (node.isReferenceable()) uuid = node.uuid();

        switch (onParentVersionAction) {
            case OnParentVersionAction.ABORT:
                throw new VersionException(JcrI18n.cannotCheckinNodeWithAbortChildNode.text(node.getName(),
                                                                                            node.getParent().getName()));
            case OnParentVersionAction.VERSION:
                if (node.isNodeType(JcrMixLexicon.VERSIONABLE)) {
                    JcrVersionHistoryNode history = node.getVersionHistory();
                    UUID historyUuid = history.uuid();
                    batch.create(childPath).with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.VERSIONED_CHILD).with(JcrLexicon.CHILD_VERSION_HISTORY,
                                                                                                             historyUuid).and();

                    break;
                }

                // Otherwise, treat it as a copy
            case OnParentVersionAction.COPY:
                batch.create(childPath).with(JcrLexicon.PRIMARY_TYPE, JcrNtLexicon.FROZEN_NODE).and(JcrLexicon.FROZEN_PRIMARY_TYPE,
                                                                                                    primaryTypeName).and(JcrLexicon.FROZEN_MIXIN_TYPES,
                                                                                                                         mixinTypeNames).and(JcrLexicon.FROZEN_UUID,
                                                                                                                                             uuid).and(versionedPropertiesFor(node)).and();
                break;
            case OnParentVersionAction.INITIALIZE:
            case OnParentVersionAction.COMPUTE:
            case OnParentVersionAction.IGNORE:
                // Do nothing for these. No built-in types require initialize or compute for child nodes.
                return;
            default:
                throw new IllegalStateException("Unexpected value: " + onParentVersionAction);
        }

        for (NodeIterator childNodes = node.getNodes(); childNodes.hasNext();) {
            AbstractJcrNode childNode = (AbstractJcrNode)childNodes.nextNode();
            versionNodeAt(childNode, childPath, batch, onParentVersionAction);
        }

    }

    // private Collection<>

    private Collection<org.modeshape.graph.property.Property> versionedPropertiesFor( AbstractJcrNode node )
        throws RepositoryException {

        Collection<org.modeshape.graph.property.Property> props = new LinkedList<org.modeshape.graph.property.Property>();
        PropertyFactory propFactory = context().getPropertyFactory();

        for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
            AbstractJcrProperty property = (AbstractJcrProperty)iter.nextProperty();

            org.modeshape.graph.property.Property prop = property.property();
            PropertyDefinitionId propDefnId = property.propertyInfo().getPayload().getPropertyDefinitionId();
            JcrPropertyDefinition propDefn = cache.nodeTypes().getPropertyDefinition(propDefnId);

            switch (propDefn.getOnParentVersion()) {
                case OnParentVersionAction.ABORT:
                    I18n msg = JcrI18n.cannotCheckinNodeWithAbortProperty;
                    throw new VersionException(msg.text(property.getName(), node.getName()));
                case OnParentVersionAction.COPY:
                case OnParentVersionAction.VERSION:
                    props.add(prop);
                    break;
                case OnParentVersionAction.INITIALIZE:
                    Object[] defaultValues = propDefn.getDefaultValues();
                    if (defaultValues != null && defaultValues.length > 0) {
                        props.add(propFactory.create(prop.getName(), defaultValues));
                    }
                    break;
                case OnParentVersionAction.COMPUTE:
                case OnParentVersionAction.IGNORE:
                    // Do nothing for these
            }
        }

        return props;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#checkout()
     */
    public final void checkout() throws UnsupportedRepositoryOperationException, LockException, RepositoryException {
        checkVersionable();

        // Check this separately since it throws a different type of exception
        if (this.isLocked() && !holdsLock()) {
            throw new LockException(JcrI18n.lockTokenNotHeld.text(this.location));
        }

        PropertyFactory propFactory = context().getPropertyFactory();

        PropertyInfo<JcrPropertyPayload> mvProp = this.nodeInfo().getProperty(ModeShapeIntLexicon.MULTI_VALUED_PROPERTIES);
        org.modeshape.graph.property.Property multiValuedProps = mvProp != null ? mvProp.getProperty() : null;

        if (multiValuedProps == null) {
            multiValuedProps = propFactory.create(ModeShapeIntLexicon.MULTI_VALUED_PROPERTIES, JcrLexicon.PREDECESSORS);
        } else if (!Arrays.<Object>asList(multiValuedProps.getValues()).contains(JcrLexicon.PREDECESSORS)) {
            List<Object> values = new LinkedList<Object>();

            for (Object value : multiValuedProps) {
                values.add(value);
            }

            values.add(JcrLexicon.PREDECESSORS);
            multiValuedProps = propFactory.create(ModeShapeIntLexicon.MULTI_VALUED_PROPERTIES, values);
        }

        ValueFactory<Reference> refFactory = context().getValueFactories().getReferenceFactory();
        Object[] oldPreds = EMPTY_OBJECT_ARRAY;

        AbstractJcrProperty oldPredsProperty = getBaseVersion().getProperty(JcrLexicon.PREDECESSORS);
        if (oldPredsProperty != null) {
            oldPreds = oldPredsProperty.property().getValuesAsArray();
        }

        Object[] newPreds = new Object[oldPreds.length + 1];
        System.arraycopy(oldPreds, 0, newPreds, 0, oldPreds.length);
        newPreds[oldPreds.length] = refFactory.create(getBaseVersion().uuid());

        org.modeshape.graph.property.Property isCheckedOut = propFactory.create(JcrLexicon.IS_CHECKED_OUT, true);
        org.modeshape.graph.property.Property predecessors = propFactory.create(JcrLexicon.PREDECESSORS, newPreds);

        Graph graph = session().workspace().graph();
        graph.set(isCheckedOut, predecessors, multiValuedProps).on(path()).and();

        refresh(true);

    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#merge(java.lang.String, boolean)
     */
    public final NodeIterator merge( String srcWorkspace,
                                     boolean bestEffort )
        throws UnsupportedRepositoryOperationException, ConstraintViolationException, RepositoryException {
        checkNotProtected();

        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#cancelMerge(javax.jcr.version.Version)
     */
    public final void cancelMerge( Version version ) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#doneMerge(javax.jcr.version.Version)
     */
    public final void doneMerge( Version version ) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getVersionHistory()
     */
    public final JcrVersionHistoryNode getVersionHistory() throws UnsupportedRepositoryOperationException, RepositoryException {
        checkVersionable();

        return new JcrVersionHistoryNode(session().getNodeByUUID(getProperty(JcrLexicon.VERSION_HISTORY).getString()));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getBaseVersion()
     */
    public final JcrVersionNode getBaseVersion() throws UnsupportedRepositoryOperationException, RepositoryException {
        checkVersionable();

        return new JcrVersionNode(session().getNodeByUUID(getProperty(JcrLexicon.BASE_VERSION).getString()));
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
        restore(version, ".", removeExisting);
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

        if (isLocked() && !holdsLock()) {
            throw new LockException(JcrI18n.lockTokenNotHeld.text(getPath()));
        }

        if (session().hasPendingChanges()) {
            throw new InvalidItemStateException(JcrI18n.noPendingChangesAllowed.text());
        }

        JcrVersionNode jcrVersion = (JcrVersionNode)version;
        JcrVersionHistoryNode versionHistory = getVersionHistory();
        if (!versionHistory.isSame(jcrVersion.getParent())) {
            throw new VersionException(JcrI18n.invalidVersion.text(version.getPath(), versionHistory.getPath()));
        }

        if (jcrVersion.isSame(versionHistory.getRootVersion())) {
            throw new VersionException(JcrI18n.cannotRestoreRootVersion.text(getPath()));
        }

        PathFactory pathFactory = context().getValueFactories().getPathFactory();
        Path relPathAsPath = pathFactory.create(relPath);
        if (relPathAsPath.isAbsolute()) throw new RepositoryException(JcrI18n.invalidRelativePath.text(relPath));

        Path actualPath = pathFactory.create(path(), relPathAsPath);

        // Ensure that the parent node exists - this will throw a PNFE if no node exists at that path
        AbstractJcrNode parentNode = cache.findJcrNode(null, actualPath.getParent());
        AbstractJcrNode existingNode = null;

        try {
            if (path().equals(actualPath)) {
                existingNode = this;
            } else {
                existingNode = cache.findJcrNode(null, actualPath);
                if (!versionHistory.isSame(existingNode.getVersionHistory())) {
                    throw new VersionException(JcrI18n.invalidVersion.text(version.getPath(),
                                                                           existingNode.getVersionHistory().getPath()));
                }
            }
        } catch (PathNotFoundException pnfe) {
            // This is allowable, but the node needs to be checked out
            if (!parentNode.isCheckedOut()) {
                String path = actualPath.getString(context().getNamespaceRegistry());
                throw new VersionException(JcrI18n.nodeIsCheckedIn.text(path));
            }
        }

        AbstractJcrNode frozenNode = jcrVersion.getNode(string(JcrLexicon.FROZEN_NODE));
        if (existingNode == null) {
            restoreFrom(frozenNode, parentNode.editor(), removeExisting);
        }

        NodeEditor editor = editor();
        editor.setProperty(JcrLexicon.IS_CHECKED_OUT, valueFrom(PropertyType.BOOLEAN, false), false);
        editor.setProperty(JcrLexicon.BASE_VERSION, valueFrom(jcrVersion), false);

        session().save();
    }

    private String string( Name name ) {
        return name.getString(context().getNamespaceRegistry());
    }

    private static final Set<Name> FROZEN_PROPERTY_NAMES = new HashSet<Name>(Arrays.asList(new Name[] {
        JcrLexicon.FROZEN_PRIMARY_TYPE, JcrLexicon.FROZEN_MIXIN_TYPES, JcrLexicon.FROZEN_UUID}));

    private void restoreFrom( AbstractJcrNode storedNode,
                              NodeEditor parentEditor,
                              boolean removeExisting ) throws RepositoryException {

        AbstractJcrProperty uuidProp = storedNode.getProperty(JcrLexicon.FROZEN_UUID);
        UUID uuid = uuidProp == null ? null : (UUID)uuidProp.property().getFirstValue();
        AbstractJcrProperty primaryTypeProp = storedNode.getProperty(JcrLexicon.FROZEN_PRIMARY_TYPE);
        Name primaryTypeName = (Name)primaryTypeProp.property().getFirstValue();
        AbstractJcrProperty mixinTypesProp = storedNode.getProperty(JcrLexicon.FROZEN_MIXIN_TYPES);
        Name[] mixinTypeNames = mixinTypesProp == null ? new Name[0] : (Name[])mixinTypesProp.property().getValuesAsArray();

        JcrNode child = parentEditor.createChild(storedNode.name(), uuid, primaryTypeName);
        NodeEditor childEditor = child.editor();

        for (int i = 0; i < mixinTypeNames.length; i++) {
            JcrNodeType mixinType = session().nodeTypeManager().getNodeType(mixinTypeNames[i]);
            childEditor.addMixin(mixinType);
        }

        for (PropertyInfo<JcrPropertyPayload> propInfo : storedNode.nodeInfo().getProperties()) {
            if (FROZEN_PROPERTY_NAMES.contains(propInfo.getName())) continue;

            AbstractJcrProperty jcrProperty = propInfo.getPayload().getJcrProperty();
            if (propInfo.isMultiValued()) {
                JcrValue[] values = (JcrValue[])jcrProperty.getValues();
                childEditor.setProperty(propInfo.getName(), values, jcrProperty.getType(), false);
            } else {
                JcrValue value = (JcrValue)jcrProperty.getValue();
                childEditor.setProperty(propInfo.getName(), value, false);
            }
        }

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
    public final boolean holdsLock() /*throws RepositoryException*/{
        WorkspaceLockManager.ModeShapeLock lock = session().workspace().lockManager().lockFor(session(), this.location);

        return lock != null && cache.session().lockTokens().contains(lock.getLockToken());
    }

    /**
     * {@inheritDoc}
     * 
     * @return <code>false</code>
     * @see javax.jcr.Node#isLocked()
     */
    public final boolean isLocked() throws LockException, RepositoryException {
        return lock() != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#lock(boolean, boolean)
     */
    public final Lock lock( boolean isDeep,
                            boolean isSessionScoped ) throws LockException, RepositoryException {
        if (!isLockable()) {
            throw new LockException(JcrI18n.nodeNotLockable.text(getPath()));
        }

        if (isLocked()) {
            throw new LockException(JcrI18n.alreadyLocked.text(this.location));
        }

        if (isDeep) {
            LinkedList<Node<JcrNodePayload, JcrPropertyPayload>> nodesToVisit = new LinkedList<Node<JcrNodePayload, JcrPropertyPayload>>();
            nodesToVisit.add(nodeInfo());

            while (!nodesToVisit.isEmpty()) {
                Node<JcrNodePayload, JcrPropertyPayload> node = nodesToVisit.remove(nodesToVisit.size() - 1);
                if (session().workspace().lockManager().lockFor(session(), node.getLocation()) != null) throw new LockException(
                                                                                                                                JcrI18n.parentAlreadyLocked.text(this.location,
                                                                                                                                                                 node.getLocation()));

                for (Node<JcrNodePayload, JcrPropertyPayload> child : node.getChildren()) {
                    nodesToVisit.add(child);
                }
            }
        }

        WorkspaceLockManager.ModeShapeLock lock = session().workspace().lockManager().lock(session(),
                                                                                           this.location,
                                                                                           isDeep,
                                                                                           isSessionScoped);

        cache.session().addLockToken(lock.getLockToken());
        return lock.lockFor(cache);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#unlock()
     */
    public final void unlock() throws LockException, RepositoryException {
        WorkspaceLockManager.ModeShapeLock lock = session().workspace().lockManager().lockFor(session(), this.location);

        if (lock == null) {
            throw new LockException(JcrI18n.notLocked.text(this.location));
        }

        if (!session().lockTokens().contains(lock.getLockToken())) {
            try {
                // See if the user has the permission to break someone else's lock
                session().checkPermission(cache.workspaceName(), null, ModeShapePermissions.UNLOCK_ANY);
            } catch (AccessControlException iae) {
                throw new LockException(JcrI18n.lockTokenNotHeld.text(this.location));
            }
        }

        session().workspace().lockManager().unlock(session().getExecutionContext(), lock);
        session().removeLockToken(lock.getLockToken());
    }

    private final WorkspaceLockManager.ModeShapeLock lock() throws RepositoryException {
        // This can only happen in mocked testing.
        if (session() == null || session().workspace() == null) return null;

        WorkspaceLockManager lockManager = session().workspace().lockManager();
        WorkspaceLockManager.ModeShapeLock lock = lockManager.lockFor(session(), this.location);
        if (lock != null) return lock;

        AbstractJcrNode parent = this;
        while (!parent.isRoot()) {
            parent = parent.getParent();

            WorkspaceLockManager.ModeShapeLock parentLock = lockManager.lockFor(session(), parent.location);
            if (parentLock != null && parentLock.isLive()) {
                return parentLock.isDeep() ? parentLock : null;
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getLock()
     */
    public final Lock getLock() throws LockException, RepositoryException {
        WorkspaceLockManager.ModeShapeLock lock = lock();

        if (lock == null) throw new LockException(JcrI18n.notLocked.text(this.location));
        return lock.lockFor(cache);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#isModified()
     */
    public final boolean isModified() {
        try {
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
        // This implementation is correct, except for not calling the SessionCache or graph layer to do the re-order
        if (!getPrimaryNodeType().hasOrderableChildNodes()) {
            throw new UnsupportedRepositoryOperationException(
                                                              JcrI18n.notOrderable.text(getPrimaryNodeType().getName(), getPath()));
        }

        PathFactory pathFactory = this.cache.pathFactory();
        Path srcPath = pathFactory.create(srcChildRelPath);
        if (srcPath.isAbsolute() || srcPath.size() != 1) {
            throw new ItemNotFoundException(JcrI18n.pathNotFound.text(srcPath.getString(cache.context().getNamespaceRegistry()),
                                                                      cache.session().workspace().getName()));
        }
        // getLastSegment should return the only segment, since we verified that size() == 1
        Path.Segment sourceSegment = srcPath.getLastSegment();
        try {
            nodeInfo().getChild(sourceSegment);
        } catch (org.modeshape.graph.property.PathNotFoundException e) {
            String workspaceName = this.cache.session().getWorkspace().getName();
            throw new ItemNotFoundException(JcrI18n.pathNotFound.text(srcPath, workspaceName));
        }

        Path.Segment destSegment = null;

        if (destChildRelPath != null) {
            Path destPath = pathFactory.create(destChildRelPath);
            if (destPath.isAbsolute() || destPath.size() != 1) {
                throw new ItemNotFoundException(
                                                JcrI18n.pathNotFound.text(destPath.getString(cache.context().getNamespaceRegistry()),
                                                                          cache.session().workspace().getName()));
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

    protected static List<Object> createPatternsFor( String namePattern ) throws RepositoryException {
        List<Object> patterns = new LinkedList<Object>();
        for (String stringPattern : namePattern.split("[|]")) {
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
                            String msg = JcrI18n.invalidNamePattern.text(c, namePattern);
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
        this.cache.refresh(this.nodeId, location.getPath(), keepChanges);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#save()
     */
    public void save() throws RepositoryException {
        session().checkReferentialIntegrityOfChanges();
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
        return HashCode.compute(cache, location);
    }
}
