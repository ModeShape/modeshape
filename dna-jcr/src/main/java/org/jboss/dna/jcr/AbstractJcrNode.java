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
package org.jboss.dna.jcr;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.jcr.Item;
import javax.jcr.ItemNotFoundException;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.UnsupportedRepositoryOperationException;
import javax.jcr.Value;
import javax.jcr.lock.Lock;
import javax.jcr.nodetype.NodeDefinition;
import javax.jcr.nodetype.NodeType;
import javax.jcr.version.Version;
import javax.jcr.version.VersionHistory;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.properties.Name;
import org.jboss.dna.graph.properties.Path.Segment;

/**
 * @author jverhaeg
 */
@NotThreadSafe
abstract class AbstractJcrNode extends AbstractJcrItem implements Node {

    private final JcrSession session;
    Set<Property> properties;
    List<Name> children;
    List<Integer> childNameCounts;
    private UUID uuid;

    AbstractJcrNode( JcrSession session ) {
        assert session != null;
        this.session = session;
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
     * @see javax.jcr.Node#cancelMerge(javax.jcr.version.Version)
     */
    public final void cancelMerge( Version version ) {
        throw new UnsupportedOperationException();
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
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#doneMerge(javax.jcr.version.Version)
     */
    public final void doneMerge( Version version ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>depth</code> is negative.
     * @see javax.jcr.Item#getAncestor(int)
     */
    public final Item getAncestor( int depth ) throws RepositoryException {
        CheckArg.isNonNegative(depth, "depth");
        Node ancestor = this;
        while (--depth >= 0) {
            ancestor = ancestor.getParent();
        }
        return ancestor;
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
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#getCorrespondingNodePath(java.lang.String)
     */
    public final String getCorrespondingNodePath( String workspaceName ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#getDefinition()
     */
    public NodeDefinition getDefinition() {
        throw new UnsupportedOperationException();
    }

    final UUID getInternalUuid() {
        return uuid;
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
     * @see javax.jcr.Node#getMixinNodeTypes()
     */
    public NodeType[] getMixinNodeTypes() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>relativePath</code> is empty or <code>null</code>.
     * @see javax.jcr.Node#getNode(java.lang.String)
     */
    public final Node getNode( String relativePath ) throws RepositoryException {
        CheckArg.isNotEmpty(relativePath, "relativePath");
        Item item = getSession().getItem(getPath(getPath(), relativePath));
        if (item instanceof Node) {
            return (Node)item;
        }
        throw new PathNotFoundException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getNodes()
     */
    public final NodeIterator getNodes() {
        return new JcrNodeIterator(this, children, childNameCounts);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#getNodes(java.lang.String)
     */
    public NodeIterator getNodes( String namePattern ) {
        // TODO: Implement after changing impl to delegate to Graph API
        throw new UnsupportedOperationException();
        /*
        CheckArg.isNotEmpty(namePattern, "namePattern");
        String[] disjuncts = namePattern.split("\\|");
        List<Segment> nodes = new ArrayList<Segment>();
        for (String disjunct : disjuncts) {
            String pattern = disjunct.trim();
            CheckArg.isNotEmpty(pattern, "namePattern");
            String ndxPattern;
            int endNdx = pattern.length() - 1;
            if (pattern.charAt(endNdx) == ']') {
                int ndx = pattern.indexOf('[');
                ndxPattern = pattern.substring(ndx + 1, endNdx);
                pattern = pattern.substring(0, ndx);
            } else ndxPattern = null;
            for (Entry<Name, Integer> child : getChildCountsByName().entrySet()) {
                if (child.getKey().getLocalName().matches(pattern)) {
                    if (ndxPattern != null && !child.getValue().toString().matches(ndxPattern)) continue;
                    if (child.getValue() > 1) nodes.add(new BasicPathSegment(child.getKey(), child.getValue()));
                    else nodes.add(new BasicPathSegment(child.getKey()));
                }
            }
        }
        */
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getPrimaryItem()
     */
    public final Item getPrimaryItem() throws RepositoryException {
        // TODO: Check if declared in the node type first
        try {
            Property primaryItemProp = getProperty("jcr:primaryItemName");
            return session.getItem(getPath(getPath(), primaryItemProp.getString()));
        } catch (PathNotFoundException error) {
            throw new ItemNotFoundException(error);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#getPrimaryNodeType()
     */
    public NodeType getPrimaryNodeType() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getProperties()
     */
    public final PropertyIterator getProperties() {
        return new JcrPropertyIterator(properties);
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#getProperties(java.lang.String)
     */
    public PropertyIterator getProperties( String namePattern ) {
        // TODO: Implement after changing impl to delegate to Graph API
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>relativePath</code> is empty or <code>null</code>.
     * @see javax.jcr.Node#getProperty(java.lang.String)
     */
    public final Property getProperty( String relativePath ) throws RepositoryException {
        CheckArg.isNotEmpty(relativePath, "relativePath");
        if (relativePath.indexOf('/') >= 0) {
            Item item = session.getItem(getPath(getPath(), relativePath));
            if (item instanceof Property) {
                return (Property)item;
            }
            // The item must be a node.
            assert item instanceof Node;
            // Since session.getItem() gives precedence to nodes over properties, try explicitly looking for the property with the
            // same name as the found node using the returned node's parent.
            return ((Node)item).getParent().getProperty(item.getName());
        }
        assert properties != null;
        for (Property property : properties) {
            if (relativePath.equals(property.getName())) return property;
        }
        throw new PathNotFoundException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws UnsupportedOperationException always
     * @see javax.jcr.Node#getReferences()
     */
    public final PropertyIterator getReferences() {
        // TODO: Need to provide this at the DNA layer first (probably via a connector query)
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getSession()
     */
    public final Session getSession() {
        return session;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getUUID()
     */
    public final String getUUID() throws RepositoryException {
        // Return JCR UUID only if node is referenceable
        try {
            Property mixinsProp = getProperty("jcr:mixinTypes");
            if (mixinsProp != null) {
                for (Value value : mixinsProp.getValues()) {
                    if ("mix:referenceable".equals(value.getString())) return getProperty("jcr:uuid").getString();
                }
            }
        } catch (PathNotFoundException error) {
            throw new UnsupportedRepositoryOperationException(error);
        }
        throw new UnsupportedRepositoryOperationException();
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
     * @throws IllegalArgumentException if <code>relativePath</code> is empty or <code>null</code>.
     * @see javax.jcr.Node#hasNode(java.lang.String)
     */
    public final boolean hasNode( String relativePath ) throws RepositoryException {
        CheckArg.isNotEmpty(relativePath, "relativePath");
        if (relativePath.indexOf('/') >= 0) {
            return (getNode(relativePath) != null);
        }
        int ndxNdx = relativePath.indexOf('[');
        String name = (ndxNdx < 0 ? relativePath : relativePath.substring(0, ndxNdx));
        CheckArg.isNotEmpty(name, "relativePath");
        int childNdx = 0;
        if (children != null) {
            for (Name child : children) {
                if (name.equals(child.getString(session.getExecutionContext().getNamespaceRegistry()))) {
                    if (ndxNdx >= 0) {
                        return (Integer.parseInt(relativePath.substring(ndxNdx + 1, relativePath.length() - 1)) <= childNameCounts.get(childNdx));
                    }
                    return true;
                }
                childNdx++;
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#hasNodes()
     */
    public final boolean hasNodes() {
        return (children != null && !children.isEmpty());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#hasProperties()
     */
    public final boolean hasProperties() {
        assert properties != null;
        return !properties.isEmpty();
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
            return (getProperty(relativePath) != null);
        }
        assert properties != null;
        for (Property property : properties) {
            if (relativePath.equals(property.getName())) {
                return true;
            }
        }
        return false;
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
     * @see javax.jcr.Node#isCheckedOut()
     */
    public final boolean isCheckedOut() {
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
    public boolean isNodeType( String nodeTypeName ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>otherItem</code> is <code>null</code>.
     * @see javax.jcr.Item#isSame(javax.jcr.Item)
     */
    @Override
    public final boolean isSame( Item otherItem ) throws RepositoryException {
        CheckArg.isNotNull(otherItem, "otherItem");
        if (super.isSame(otherItem) && otherItem instanceof Node) {
            if (otherItem instanceof AbstractJcrNode) {
                return getInternalUuid().equals(((AbstractJcrNode)otherItem).getInternalUuid());
            }
            // If not our implementation, let the other item figure out whether we are the same.
            return otherItem.isSame(this);
        }
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
     * @throws UnsupportedRepositoryOperationException always
     * @see javax.jcr.Node#orderBefore(java.lang.String, java.lang.String)
     */
    public final void orderBefore( String srcChildRelPath,
                                   String destChildRelPath ) throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
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

    final void setChildren( List<Segment> children ) {
        assert children != null;
        if (this.children == null) {
            this.children = new ArrayList<Name>(children.size());
            childNameCounts = new ArrayList<Integer>(children.size());
        }
        for (Segment seg : children) {
            Name name = seg.getName();
            int ndx = this.children.indexOf(name);
            if (ndx >= 0) {
                childNameCounts.set(ndx, childNameCounts.get(ndx) + 1);
            } else {
                this.children.add(name);
                childNameCounts.add(1);
            }
        }
        assert this.children.size() == childNameCounts.size();
    }

    final void setInternalUuid( UUID uuid ) {
        assert uuid != null;
        this.uuid = uuid;
    }

    final void setProperties( Set<Property> properties ) {
        assert properties != null;
        this.properties = properties;
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
     * @throws UnsupportedRepositoryOperationException always
     * @see javax.jcr.Node#unlock()
     */
    public final void unlock() throws UnsupportedRepositoryOperationException {
        throw new UnsupportedRepositoryOperationException();
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
}
