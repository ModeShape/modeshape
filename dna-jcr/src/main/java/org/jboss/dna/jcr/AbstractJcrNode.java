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
import java.util.Calendar;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.jcr.Item;
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
import javax.jcr.lock.Lock;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.NodeTypeManager;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.i18n.I18n;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NamespaceRegistry;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.ValueFormatException;
import org.jboss.dna.jcr.SessionCache.ChildNode;
import org.jboss.dna.jcr.SessionCache.Children;
import org.jboss.dna.jcr.SessionCache.NodeInfo;

/**
 * An abstract implementation of the JCR {@link Node} interface. Instances of this class are created and managed by the
 * {@link SessionCache}. Each instance references the {@link SessionCache.NodeInfo node information} also managed by the
 * SessionCache, and finds and operates against this information with each method call.
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
        Property mixinsProp = getProperty(JcrLexicon.MIXIN_TYPES);
        if (mixinsProp != null) {
            String referenceableMixinName = JcrMixLexicon.REFERENCEABLE.getString(namespaces());
            for (Value value : mixinsProp.getValues()) {
                if (referenceableMixinName.equals(value.getString())) return nodeUuid.toString();
            }
        }
        throw new UnsupportedRepositoryOperationException();
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
    public NodeType getPrimaryNodeType() throws RepositoryException {
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
        NodeTypeManager nodeTypeManager = session().getWorkspace().getNodeTypeManager();
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
     * @return <code>false</code>
     * @see javax.jcr.Node#canAddMixin(java.lang.String)
     */
    public final boolean canAddMixin( String mixinName ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#removeMixin(java.lang.String)
     */
    public final void removeMixin( String mixinName ) {
        throw new UnsupportedOperationException();
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
        return nodeInfo().getProperties().size() > 0;
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
            throw new IllegalArgumentException(JcrI18n.invalidPathParameter.text(relativePath));
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
            throw new IllegalArgumentException(JcrI18n.invalidPathParameter.text(relativePath));
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
            throw new IllegalArgumentException(JcrI18n.invalidPathParameter.text(relativePath));
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
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#addMixin(java.lang.String)
     */
    public final void addMixin( String mixinName ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#addNode(java.lang.String)
     */
    public final Node addNode( String relPath ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#addNode(java.lang.String, java.lang.String)
     */
    public final Node addNode( String relPath,
                               String primaryNodeTypeName ) {
        throw new UnsupportedOperationException();
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
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#setProperty(java.lang.String, boolean)
     */
    public final Property setProperty( String name,
                                       boolean value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#setProperty(java.lang.String, java.util.Calendar)
     */
    public final Property setProperty( String name,
                                       Calendar value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#setProperty(java.lang.String, double)
     */
    public final Property setProperty( String name,
                                       double value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#setProperty(java.lang.String, java.io.InputStream)
     */
    public final Property setProperty( String name,
                                       InputStream value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#setProperty(java.lang.String, long)
     */
    public final Property setProperty( String name,
                                       long value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Node)
     */
    public final Property setProperty( String name,
                                       Node value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#setProperty(java.lang.String, java.lang.String)
     */
    public final Property setProperty( String name,
                                       String value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#setProperty(java.lang.String, java.lang.String, int)
     */
    public final Property setProperty( String name,
                                       String value,
                                       int type ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#setProperty(java.lang.String, java.lang.String[])
     */
    public final Property setProperty( String name,
                                       String[] values ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#setProperty(java.lang.String, java.lang.String[], int)
     */
    public final Property setProperty( String name,
                                       String[] values,
                                       int type ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Value)
     */
    public final Property setProperty( String name,
                                       Value value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Value, int)
     */
    public final Property setProperty( String name,
                                       Value value,
                                       int type ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Value[])
     */
    public final Property setProperty( String name,
                                       Value[] values ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Value[], int)
     */
    public final Property setProperty( String name,
                                       Value[] values,
                                       int type ) {
        throw new UnsupportedOperationException();
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
}
