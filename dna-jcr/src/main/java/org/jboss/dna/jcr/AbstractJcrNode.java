/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
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

import java.io.InputStream;
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
import javax.jcr.InvalidItemStateException;
import javax.jcr.Item;
import javax.jcr.ItemExistsException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
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
import javax.jcr.version.Version;
import javax.jcr.version.VersionException;
import javax.jcr.version.VersionHistory;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.jcr.SessionCache.NodeEditor;
import org.jboss.dna.jcr.cache.ChildNode;
import org.jboss.dna.jcr.cache.Children;
import org.jboss.dna.jcr.cache.NodeInfo;
import org.jboss.dna.jcr.cache.PropertyInfo;

/**
 * An abstract implementation of the JCR {@link Node} interface. Instances of this class are created and managed by the
 * {@link SessionCache}. Each instance references the {@link NodeInfo node information} also managed by the SessionCache, and
 * finds and operates against this information with each method call.
 */
@Immutable
abstract class AbstractJcrNode extends AbstractJcrItem implements Node {

    private static final NodeType[] EMPTY_NODE_TYPES = new NodeType[] {};

    protected final UUID nodeUuid;

    AbstractJcrNode( SessionCache cache,
                     UUID nodeUuid ) {
        super(cache);
        this.nodeUuid = nodeUuid;
    }

    abstract boolean isRoot();

    final UUID internalUuid() {
        return nodeUuid;
    }

    final NodeInfo nodeInfo() throws ItemNotFoundException, RepositoryException {
        return cache.findNodeInfo(nodeUuid);
    }

    final NodeEditor editorForParent() throws RepositoryException {
        try {
            return cache.getEditorFor(nodeInfo().getParent());
        } catch (ItemNotFoundException err) {
            String msg = JcrI18n.nodeHasAlreadyBeenRemovedFromThisSession.text(nodeUuid, cache.workspaceName());
            throw new RepositoryException(msg);
        } catch (InvalidItemStateException err) {
            String msg = JcrI18n.nodeHasAlreadyBeenRemovedFromThisSession.text(nodeUuid, cache.workspaceName());
            throw new RepositoryException(msg);
        }
    }

    final NodeEditor editor() throws RepositoryException {
        try {
            return cache.getEditorFor(nodeUuid);
        } catch (ItemNotFoundException err) {
            String msg = JcrI18n.nodeHasAlreadyBeenRemovedFromThisSession.text(nodeUuid, cache.workspaceName());
            throw new RepositoryException(msg);
        } catch (InvalidItemStateException err) {
            String msg = JcrI18n.nodeHasAlreadyBeenRemovedFromThisSession.text(nodeUuid, cache.workspaceName());
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

    final JcrValue valueFrom( Node value ) throws UnsupportedRepositoryOperationException, RepositoryException {
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
        return cache.getPathFor(nodeInfo());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getUUID()
     */
    public final String getUUID() throws RepositoryException {
        // Return "jcr:uuid" only if node is referenceable
        String referenceableTypeName = JcrMixLexicon.REFERENCEABLE.getString(namespaces());
        if (!isNodeType(referenceableTypeName)) {
            throw new UnsupportedRepositoryOperationException();
        }
        return nodeUuid.toString();
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
        NodeType nodeType = getPrimaryNodeType();

        if (nodeType.isNodeType(nodeTypeName)) {
            return true;
        }

        NodeType[] mixinNodeTypes = getMixinNodeTypes();
        for (int i = 0; i < mixinNodeTypes.length; i++) {
            if (mixinNodeTypes[i].isNodeType(nodeTypeName)) {
                return true;
            }
        }

        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getDefinition()
     */
    public NodeDefinition getDefinition() throws RepositoryException {
        NodeDefinitionId definitionId = nodeInfo().getDefinitionId();
        return session().nodeTypeManager().getNodeDefinition(definitionId);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#getPrimaryNodeType()
     */
    public JcrNodeType getPrimaryNodeType() throws RepositoryException {
        Name primaryTypeName = nodeInfo().getPrimaryTypeName();
        return session().nodeTypeManager().getNodeType(primaryTypeName);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
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
            return cache.findJcrItem(nodeUuid, primaryItemPath);
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
        if (super.isSame(otherItem) && otherItem instanceof Node) {
            if (otherItem instanceof AbstractJcrNode) {
                return internalUuid().equals(((AbstractJcrNode)otherItem).internalUuid());
            }
            // If not our implementation, let the other item figure out whether we are the same.
            return otherItem.isSame(this);
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#hasProperties()
     */
    public final boolean hasProperties() throws RepositoryException {
        return nodeInfo().hasProperties();
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
        return cache.findPropertyInfo(new PropertyId(nodeUuid, nameFrom(relativePath))) != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getProperties()
     */
    public final PropertyIterator getProperties() throws RepositoryException {
        return new JcrPropertyIterator(cache.findJcrPropertiesFor(nodeUuid));
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
        Collection<AbstractJcrProperty> properties = cache.findJcrPropertiesFor(nodeUuid);
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
                foundMatch = true; // for the next iteration ..
            }
        }
        return new JcrPropertyIterator(matchingProperties);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#getReferences()
     */
    public final PropertyIterator getReferences() throws RepositoryException {
        // Iterate through the properties to see which ones have a REFERENCE type ...
        Collection<AbstractJcrProperty> properties = cache.findJcrPropertiesFor(nodeUuid);
        Collection<AbstractJcrProperty> references = new LinkedList<AbstractJcrProperty>();
        Iterator<AbstractJcrProperty> iter = properties.iterator();
        while (iter.hasNext()) {
            AbstractJcrProperty property = iter.next();
            if (property.getType() == PropertyType.REFERENCE) references.add(property);
        }
        if (references.isEmpty()) return new JcrEmptyPropertyIterator();
        return new JcrPropertyIterator(references);
    }

    /**
     * A non-standard method to obtain a property given the {@link Name DNA Name} object. This method is faster
     * 
     * @param propertyName the property name
     * @return the JCR property with the supplied name, or null if the property doesn't exist
     * @throws RepositoryException if there is an error finding the property with the supplied name
     */
    public final Property getProperty( Name propertyName ) throws RepositoryException {
        return cache.findJcrProperty(new PropertyId(nodeUuid, propertyName));
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
        if (indexOfFirstSlash != -1) {
            // We know it's a relative path with more than one segment ...
            Path path = pathFrom(relativePath).getNormalizedPath();
            AbstractJcrItem item = cache.findJcrItem(nodeUuid, path);
            if (item instanceof Property) {
                return (Property)item;
            }
            I18n msg = JcrI18n.propertyNotFoundAtPathRelativeToReferenceNode;
            throw new PathNotFoundException(msg.text(relativePath, getPath(), cache.workspaceName()));
        }
        // It's just a name, so look for it directly ...
        Property property = getProperty(nameFrom(relativePath));
        if (property != null) return property;
        throw new PathNotFoundException();
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
                AbstractJcrNode item = cache.findJcrNode(nodeUuid, path);
                return item != null;
            } catch (PathNotFoundException e) {
                return false;
            }
        }
        // It's just a name, so look for a child ...
        Path.Segment segment = segmentFrom(relativePath);
        ChildNode child = nodeInfo().getChildren().getChild(segment);
        return child != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#hasNodes()
     */
    public final boolean hasNodes() throws RepositoryException {
        return nodeInfo().getChildren().size() > 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>relativePath</code> is empty or <code>null</code>.
     * @see javax.jcr.Node#getNode(java.lang.String)
     */
    public final Node getNode( String relativePath ) throws RepositoryException {
        CheckArg.isNotEmpty(relativePath, "relativePath");
        if (relativePath.equals(".")) return this;
        if (relativePath.equals("..")) return this.getParent();
        int indexOfFirstSlash = relativePath.indexOf('/');
        if (indexOfFirstSlash == 0) {
            // Not a relative path ...
            throw new IllegalArgumentException(JcrI18n.invalidPathParameter.text(relativePath, "relativePath"));
        }
        if (indexOfFirstSlash != -1) {
            // We know it's a relative path with more than one segment ...
            Path path = pathFrom(relativePath).getNormalizedPath();
            AbstractJcrItem item = cache.findJcrItem(nodeUuid, path);
            if (item instanceof Node) {
                return (Node)item;
            }
            I18n msg = JcrI18n.nodeNotFoundAtPathRelativeToReferenceNode;
            throw new PathNotFoundException(msg.text(relativePath, getPath(), cache.workspaceName()));
        }
        // It's just a name, so look for a child ...
        Path.Segment segment = segmentFrom(relativePath);
        ChildNode child = nodeInfo().getChildren().getChild(segment);
        if (child != null) {
            return cache.findJcrNode(child.getUuid());
        }
        String msg = JcrI18n.childNotFoundUnderNode.text(segment, getPath(), cache.workspaceName());
        throw new PathNotFoundException(msg);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getNodes()
     */
    public final NodeIterator getNodes() throws RepositoryException {
        Children children = nodeInfo().getChildren();
        if (children.size() == 0) {
            return new JcrEmptyNodeIterator();
        }
        return new JcrChildNodeIterator(cache, children, children.size());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#getNodes(java.lang.String)
     */
    public NodeIterator getNodes( String namePattern ) throws RepositoryException {
        CheckArg.isNotNull(namePattern, "namePattern");
        namePattern = namePattern.trim();
        if (namePattern.length() == 0) return new JcrEmptyNodeIterator();
        if ("*".equals(namePattern)) return getNodes();
        List<Object> patterns = createPatternsFor(namePattern);

        // Implementing exact-matching only for now to prototype types as properties
        Children children = nodeInfo().getChildren();
        List<ChildNode> matchingChildren = new LinkedList<ChildNode>();
        NamespaceRegistry registry = namespaces();
        boolean foundMatch = false;
        for (ChildNode child : children) {
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
                    matchingChildren.add(child);
                    break;
                }
            }
        }
        return new JcrChildNodeIterator(cache, matchingChildren, matchingChildren.size());
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
     * <b>DNA Implementation Notes</b>
     * </p>
     * <p>
     * DNA imposes the following additional restrictions on the addition of mixin types in addition to the restrictions provided
     * by the JCR 1.0 specification:
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

        JcrNodeType mixinCandidateType = cache.nodeTypes().getNodeType(mixinName);

        if (this.isLocked()) {
            return false;
        }

        if (this.getDefinition().isProtected()) {
            return false;
        }

        // TODO: Check access control when that support is added
        // TODO: Throw VersionException if this node is versionable and checked in or unversionable and the nearest versionable
        // ancestor is checked in

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
            AbstractJcrProperty existingProp = cache.findJcrProperty(new PropertyId(nodeUuid,
                                                                                    propertyDefinition.getInternalName()));
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
            int snsCount = nodeInfo().getChildren().getCountOfSameNameSiblingsWithName(nodeName);

            for (Iterator<ChildNode> iter = nodeInfo().getChildren().getChildren(nodeName); iter.hasNext();) {
                AbstractJcrNode childNode = cache.findJcrNode(iter.next().getUuid());
                JcrNodeDefinition match = this.cache.nodeTypes().findChildNodeDefinition(mixinCandidateType.getInternalName(),
                                                                                         Collections.<Name>emptyList(),
                                                                                         nodeName,
                                                                                         childNode.getPrimaryNodeType()
                                                                                                  .getInternalName(),
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
     * <b>DNA Implementation Notes</b>
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
        if (this.isLocked()) {
            throw new LockException();
        }

        if (!canAddMixin(mixinName)) {
            throw new ConstraintViolationException();
        }

        Property existingMixinProperty = getProperty(JcrLexicon.MIXIN_TYPES);
        Value[] existingMixinValues;
        if (existingMixinProperty != null) {
            existingMixinValues = existingMixinProperty.getValues();
        } else {
            existingMixinValues = new Value[0];
        }

        Value[] newMixinValues = new Value[existingMixinValues.length + 1];
        System.arraycopy(existingMixinValues, 0, newMixinValues, 0, existingMixinValues.length);
        newMixinValues[newMixinValues.length - 1] = valueFrom(PropertyType.NAME, mixinCandidateType.getInternalName());

        cache.findJcrProperty(editor().setProperty(JcrLexicon.MIXIN_TYPES, newMixinValues, false));

        // ------------------------------------------------------------------------------
        // Create any auto-created properties/nodes from new type
        // ------------------------------------------------------------------------------

        for (JcrPropertyDefinition propertyDefinition : mixinCandidateType.propertyDefinitions()) {
            if (propertyDefinition.isAutoCreated() && !propertyDefinition.isProtected()) {
                if (null == cache.findJcrProperty(new PropertyId(nodeUuid, propertyDefinition.getInternalName()))) {
                    assert propertyDefinition.getDefaultValues() != null;
                    if (propertyDefinition.isMultiple()) {
                        setProperty(propertyDefinition.getName(), propertyDefinition.getDefaultValues());
                    } else {
                        assert propertyDefinition.getDefaultValues().length == 1;
                        setProperty(propertyDefinition.getName(), propertyDefinition.getDefaultValues()[0]);
                    }
                }
            }
        }

        for (JcrNodeDefinition nodeDefinition : mixinCandidateType.childNodeDefinitions()) {
            if (nodeDefinition.isAutoCreated() && !nodeDefinition.isProtected()) {
                Name nodeName = nodeDefinition.getInternalName();
                if (!nodeInfo().getChildren().getChildren(nodeName).hasNext()) {
                    assert nodeDefinition.getDefaultPrimaryType() != null;
                    editor().createChild(nodeName,
                                         (UUID)null,
                                         ((JcrNodeType)nodeDefinition.getDefaultPrimaryType()).getInternalName());
                }
            }
        }

    }

    /**
     * {@inheritDoc}
     * <p>
     * <b>DNA Implementation Notes</b>
     * </p>
     * <p>
     * DNA allows the removal of a mixin type if and only if all of the node's existing child nodes and properties would still
     * have a valid definition from the node's primary type or other mixin types. In practice, this means that either the node
     * must have a residual definition compatible with any of the remaining child nodes or properties that currently use a
     * definition from the to-be-removed mixin type or all of the child nodes and properties that use a definition from the
     * to-be-removed mixin type must be removed prior to calling this method.
     * </p>
     * *
     * 
     * @see javax.jcr.Node#removeMixin(java.lang.String)
     */
    public final void removeMixin( String mixinName ) throws RepositoryException {

        if (this.isLocked()) {
            throw new LockException();
        }

        // TODO: Check access control when that support is added
        // TODO: Throw VersionException if this node is versionable and checked in or unversionable and the nearest versionable
        // ancestor is checked in

        Property existingMixinProperty = getProperty(JcrLexicon.MIXIN_TYPES);

        if (existingMixinProperty == null) {
            throw new NoSuchNodeTypeException();
        }

        Value[] existingMixinValues = existingMixinProperty.getValues();

        if (existingMixinValues.length == 0) {
            throw new NoSuchNodeTypeException();
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
                    throw new NoSuchNodeTypeException();
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
                    throw new ConstraintViolationException();
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
            int snsCount = node.nodeInfo().getChildren().getCountOfSameNameSiblingsWithName(childNodeName);
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
                    throw new ConstraintViolationException();
                }
            }
        }

        cache.findJcrProperty(editor().setProperty(JcrLexicon.MIXIN_TYPES, newMixinValues, false));

    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#addNode(java.lang.String)
     */
    public final Node addNode( String relPath )
        throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException,
        RepositoryException {
        return addNode(relPath, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#addNode(java.lang.String, java.lang.String)
     */
    public final Node addNode( String relPath,
                               String primaryNodeTypeName )
        throws ItemExistsException, PathNotFoundException, VersionException, ConstraintViolationException, LockException,
        RepositoryException {
        // Determine the path ...
        NodeEditor editor = null;
        Path path = null;
        try {
            path = cache.pathFactory().create(relPath);
        } catch (org.jboss.dna.graph.property.ValueFormatException e) {
            throw new RepositoryException(JcrI18n.invalidPathParameter.text(relPath, "relPath"));
        }
        if (path.size() == 0) {
            throw new RepositoryException(JcrI18n.invalidPathParameter.text(relPath, "relPath"));
        }
        if (path.getLastSegment().getIndex() > 1 || relPath.endsWith("]")) {
            throw new RepositoryException(JcrI18n.invalidPathParameter.text(relPath, "relPath"));
        }
        if (path.size() != 1) {
            // The only segment in the path is the child name ...
            NodeInfo parentInfo = null;
            Path parentPath = path.getParent();
            try {
                parentInfo = cache.findNodeInfo(nodeUuid, parentPath); // throws PathNotFoundException
                editor = cache.getEditorFor(parentInfo.getUuid());
            } catch (PathNotFoundException e) {
                // We're going to throw an exception ... the question is which one ...
                if (parentPath.size() > 1) {
                    // Per the TCK, if relPath references a property, then we have to throw a ConstraintViolationException
                    // So, if we can't find the parent, try for the parent's parent and see if the last segment of the parent's
                    // path contains a property ...
                    Path grandparentPath = parentPath.getParent();
                    assert grandparentPath != null;
                    try {
                        NodeInfo grandparentInfo = cache.findNodeInfo(nodeUuid, grandparentPath); // throws PathNotFoundException
                        if (grandparentInfo.getProperty(parentPath.getLastSegment().getName()) != null) {
                            // Need to throw a ConstraintViolationException since the request was to add a child to
                            // a property ...
                            throw new ConstraintViolationException(JcrI18n.invalidPathParameter.text(relPath, "relPath"));
                        }
                    } catch (PathNotFoundException e2) {
                        // eat, since the original exception is what we want ...
                    }
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
            } catch (org.jboss.dna.graph.property.ValueFormatException e) {
                throw new RepositoryException(JcrI18n.invalidNodeTypeNameParameter.text(primaryNodeTypeName,
                                                                                        "primaryNodeTypeName"));
            }
        }

        // Create the child ...
        ChildNode child = editor.createChild(childName, null, childPrimaryTypeName);
        return cache.findJcrNode(child.getUuid());
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#update(java.lang.String)
     */
    public final void update( String srcWorkspaceName ) {
        throw new UnsupportedOperationException();
    }

    /**
     * Checks whether there is an existing property with this name that does not match the given cardinality. If such a property
     * exists, a {@code javax.jcr.ValueFormatException} is thrown, as per section 7.1.5 of the JCR 1.0.1 specification.
     * 
     * @param propertyName the name of the property
     * @param isMultiple whether the property must have multiple values
     * @throws javax.jcr.ValueFormatException if the property exists but has the opposite cardinality
     * @throws RepositoryException if any other error occurs
     */
    private void checkCardinalityOfExistingProperty( Name propertyName,
                                                     boolean isMultiple )
        throws javax.jcr.ValueFormatException, RepositoryException {
        // Check for existing single-valued property - can't set multiple values on single-valued property
        PropertyInfo propInfo = this.nodeInfo().getProperty(propertyName);
        if (propInfo != null && propInfo.isMultiValued() != isMultiple) {
            if (isMultiple) {
                I18n msg = JcrI18n.unableToSetSingleValuedPropertyUsingMultipleValues;
                throw new ValueFormatException(msg.text(getPath(),
                                                        propertyName.getString(cache.namespaces),
                                                        cache.workspaceName()));
            }
            I18n msg = JcrI18n.unableToSetMultiValuedPropertyUsingSingleValue;
            throw new ValueFormatException(msg.text(getPath(), propertyName, cache.workspaceName()));
        }

    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, boolean)
     */
    public final Property setProperty( String name,
                                       boolean value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        Name propertyName = nameFrom(name);
        checkCardinalityOfExistingProperty(propertyName, false);
        return cache.findJcrProperty(editor().setProperty(propertyName, valueFrom(PropertyType.BOOLEAN, value)));

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
            // If there is an existing property, then remove it ...
            return removeExistingValuedProperty(name);
        }

        Name propertyName = nameFrom(name);
        checkCardinalityOfExistingProperty(propertyName, false);
        return cache.findJcrProperty(editor().setProperty(propertyName, valueFrom(value)));

    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, double)
     */
    public final Property setProperty( String name,
                                       double value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        Name propertyName = nameFrom(name);
        checkCardinalityOfExistingProperty(propertyName, false);
        return cache.findJcrProperty(editor().setProperty(propertyName, valueFrom(PropertyType.DOUBLE, value)));

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
            // If there is an existing property, then remove it ...
            return removeExistingValuedProperty(name);
        }
        Name propertyName = nameFrom(name);
        checkCardinalityOfExistingProperty(propertyName, false);
        return cache.findJcrProperty(editor().setProperty(propertyName, valueFrom(value)));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, long)
     */
    public final Property setProperty( String name,
                                       long value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        Name propertyName = nameFrom(name);
        checkCardinalityOfExistingProperty(propertyName, false);
        return cache.findJcrProperty(editor().setProperty(propertyName, valueFrom(PropertyType.LONG, value)));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Node)
     */
    public final Property setProperty( String name,
                                       Node value )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (value == null) {
            // If there is an existing property, then remove it ...
            return removeExistingValuedProperty(name);
        }

        Name propertyName = nameFrom(name);
        checkCardinalityOfExistingProperty(propertyName, false);
        return cache.findJcrProperty(editor().setProperty(propertyName, valueFrom(value)));
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
            // If there is an existing property, then remove it ...
            return removeExistingValuedProperty(name);
        }
        Name propertyName = nameFrom(name);
        checkCardinalityOfExistingProperty(propertyName, false);
        return cache.findJcrProperty(editor().setProperty(propertyName, valueFrom(PropertyType.STRING, value)));
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
            // If there is an existing property, then remove it ...
            return removeExistingValuedProperty(name);
        }
        Name propertyName = nameFrom(name);
        checkCardinalityOfExistingProperty(propertyName, false);
        return cache.findJcrProperty(editor().setProperty(propertyName, valueFrom(type, value)));
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
            // If there is an existing property, then remove it ...
            return removeExistingValuedProperty(name);
        }
        Name propertyName = nameFrom(name);
        checkCardinalityOfExistingProperty(propertyName, true);
        return cache.findJcrProperty(editor().setProperty(propertyName, valuesFrom(PropertyType.STRING, values)));
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
            // If there is an existing property, then remove it ...
            return removeExistingValuedProperty(name);
        }
        Name propertyName = nameFrom(name);
        checkCardinalityOfExistingProperty(propertyName, true);
        return cache.findJcrProperty(editor().setProperty(propertyName, valuesFrom(type, values)));
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
            // If there is an existing property, then remove it ...
            return removeExistingValuedProperty(name);
        }
        Name propertyName = nameFrom(name);
        checkCardinalityOfExistingProperty(propertyName, false);
        return cache.findJcrProperty(editor().setProperty(propertyName, (JcrValue)value));
    }

    protected final Property removeExistingValuedProperty( String name )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        PropertyId id = new PropertyId(nodeUuid, nameFrom(name));
        AbstractJcrProperty property = cache.findJcrProperty(id);
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
     * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Value, int)
     */
    public final Property setProperty( String name,
                                       Value value,
                                       int type )
        throws ValueFormatException, VersionException, LockException, ConstraintViolationException, RepositoryException {
        if (value == null) {
            // If there is an existing property, then remove it ...
            return removeExistingValuedProperty(name);
        }
        Name propertyName = nameFrom(name);
        checkCardinalityOfExistingProperty(propertyName, false);
        return cache.findJcrProperty(editor().setProperty(propertyName, ((JcrValue)value).asType(type)));
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
        int len = values.length;
        Value[] newValues = null;
        if (len == 0) {
            newValues = JcrMultiValueProperty.EMPTY_VALUES;
        } else {
            List<Value> valuesWithDesiredType = new ArrayList<Value>(len);
            int expectedType = -1;
            for (int i = 0; i != len; ++i) {
                Value value = values[i];
                if (value == null) continue;
                if (expectedType == -1) {
                    expectedType = value.getType();
                } else if (value.getType() != expectedType) {
                    // Make sure the type of each value is the same, as per Javadoc in section 7.1.5 of the JCR 1.0.1 spec
                    StringBuilder sb = new StringBuilder();
                    sb.append('[');
                    for (int j = 0; j != values.length; ++j) {
                        if (j != 0) sb.append(",");
                        sb.append(values[j].toString());
                    }
                    sb.append(']');
                    String propType = PropertyType.nameFromValue(expectedType);
                    I18n msg = JcrI18n.allPropertyValuesMustHaveSameType;
                    throw new ValueFormatException(msg.text(name, values, propType, getPath(), cache.workspaceName()));
                }
                valuesWithDesiredType.add(value);
            }
            if (valuesWithDesiredType.isEmpty()) {
                newValues = JcrMultiValueProperty.EMPTY_VALUES;
            } else {
                newValues = valuesWithDesiredType.toArray(new Value[valuesWithDesiredType.size()]);
            }
        }

        Name propertyName = nameFrom(name);
        checkCardinalityOfExistingProperty(propertyName, true);
        // Set the value, perhaps to an empty array ...
        return cache.findJcrProperty(editor().setProperty(propertyName, newValues));
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

        int len = values.length;
        Value[] newValues = null;
        if (len == 0) {
            newValues = JcrMultiValueProperty.EMPTY_VALUES;
        } else {
            List<Value> valuesWithDesiredType = new ArrayList<Value>(len);
            int expectedType = -1;
            for (int i = 0; i != len; ++i) {
                Value value = values[i];

                if (value == null) continue;
                if (expectedType == -1) {
                    expectedType = value.getType();
                } else if (value.getType() != expectedType) {
                    // Make sure the type of each value is the same, as per Javadoc in section 7.1.5 of the JCR 1.0.1 spec
                    StringBuilder sb = new StringBuilder();
                    sb.append('[');
                    for (int j = 0; j != values.length; ++j) {
                        if (j != 0) sb.append(",");
                        sb.append(values[j].toString());
                    }
                    sb.append(']');
                    String propType = PropertyType.nameFromValue(expectedType);
                    I18n msg = JcrI18n.allPropertyValuesMustHaveSameType;
                    throw new ValueFormatException(msg.text(name, values, propType, getPath(), cache.workspaceName()));
                }
                if (value.getType() != type) {
                    value = ((JcrValue)value).asType(type);
                }
                valuesWithDesiredType.add(value);
            }
            if (valuesWithDesiredType.isEmpty()) {
                newValues = JcrMultiValueProperty.EMPTY_VALUES;
            } else {
                newValues = valuesWithDesiredType.toArray(new Value[valuesWithDesiredType.size()]);
            }
        }

        Name propertyName = nameFrom(name);
        checkCardinalityOfExistingProperty(propertyName, true);
        // Set the value, perhaps to an empty array ...
        return cache.findJcrProperty(editor().setProperty(propertyName, newValues));
    }

    /**
     * {@inheritDoc}
     * 
     * @return <code>false</code>
     * @see javax.jcr.Node#isCheckedOut()
     */
    public final boolean isCheckedOut() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#checkin()
     */
    public final Version checkin() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#checkout()
     */
    public final void checkout() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @return <code>false</code>
     * @see javax.jcr.Node#holdsLock()
     */
    public final boolean holdsLock() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @return <code>false</code>
     * @see javax.jcr.Node#isLocked()
     */
    public final boolean isLocked() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedRepositoryOperationException always
     * @see javax.jcr.Node#lock(boolean, boolean)
     */
    public final Lock lock( boolean isDeep,
                            boolean isSessionScoped ) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedRepositoryOperationException always
     * @see javax.jcr.Node#unlock()
     */
    public final void unlock() throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedRepositoryOperationException always
     * @see javax.jcr.Node#getLock()
     */
    public final Lock getLock() throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#merge(java.lang.String, boolean)
     */
    public final NodeIterator merge( String srcWorkspace,
                                     boolean bestEffort ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#cancelMerge(javax.jcr.version.Version)
     */
    public final void cancelMerge( Version version ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#doneMerge(javax.jcr.version.Version)
     */
    public final void doneMerge( Version version ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#getCorrespondingNodePath(java.lang.String)
     */
    public final String getCorrespondingNodePath( String workspaceName ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedRepositoryOperationException always
     * @see javax.jcr.Node#getVersionHistory()
     */
    public final VersionHistory getVersionHistory() throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedRepositoryOperationException always
     * @see javax.jcr.Node#getBaseVersion()
     */
    public final Version getBaseVersion() throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedRepositoryOperationException always
     * @see javax.jcr.Node#restore(java.lang.String, boolean)
     */
    public final void restore( String versionName,
                               boolean removeExisting ) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedRepositoryOperationException always
     * @see javax.jcr.Node#restore(javax.jcr.version.Version, boolean)
     */
    public final void restore( Version version,
                               boolean removeExisting ) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedRepositoryOperationException always
     * @see javax.jcr.Node#restore(javax.jcr.version.Version, java.lang.String, boolean)
     */
    public final void restore( Version version,
                               String relPath,
                               boolean removeExisting ) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedRepositoryOperationException always
     * @see javax.jcr.Node#restoreByLabel(java.lang.String, boolean)
     */
    public final void restoreByLabel( String versionLabel,
                                      boolean removeExisting ) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedRepositoryOperationException always
     * @see javax.jcr.Node#orderBefore(java.lang.String, java.lang.String)
     */
    public final void orderBefore( String srcChildRelPath,
                                   String destChildRelPath ) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
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
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Item#refresh(boolean)
     */
    public void refresh( boolean keepChanges ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#save()
     */
    public void save() throws RepositoryException {
        cache.save(nodeUuid);
    }
}
