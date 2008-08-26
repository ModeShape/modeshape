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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.Map.Entry;
import javax.jcr.Item;
import javax.jcr.ItemVisitor;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
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
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.spi.graph.Name;
import org.jboss.dna.spi.graph.Path.Segment;
import org.jboss.dna.spi.graph.impl.BasicPathSegment;

/**
 * @author jverhaeg
 */
@NotThreadSafe
abstract class AbstractJcrNode extends AbstractJcrItem implements Node {

    private final Session session;
    private Set<Property> properties;
    private Map<Name, Integer> childCountsByName;
    private UUID uuid;

    AbstractJcrNode( Session session ) {
        assert session != null;
        this.session = session;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#accept(javax.jcr.ItemVisitor)
     */
    public void accept( ItemVisitor visitor ) throws RepositoryException {
        visitor.visit(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#addMixin(java.lang.String)
     */
    public void addMixin( String mixinName ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#addNode(java.lang.String)
     */
    public Node addNode( String relPath ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#addNode(java.lang.String, java.lang.String)
     */
    public Node addNode( String relPath,
                         String primaryNodeTypeName ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#canAddMixin(java.lang.String)
     */
    public boolean canAddMixin( String mixinName ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#cancelMerge(javax.jcr.version.Version)
     */
    public void cancelMerge( Version version ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#checkin()
     */
    public Version checkin() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#checkout()
     */
    public void checkout() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#doneMerge(javax.jcr.version.Version)
     */
    public void doneMerge( Version version ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @throws IllegalArgumentException if <code>depth</code> is negative.
     * @see javax.jcr.Item#getAncestor(int)
     */
    public final Item getAncestor( int depth ) throws RepositoryException {
        ArgCheck.isNonNegative(depth, "depth");
        Node ancestor = this;
        while (--depth >= 0) {
            ancestor = ancestor.getParent();
        }
        return ancestor;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getBaseVersion()
     */
    public Version getBaseVersion() {
        throw new UnsupportedOperationException();
    }

    private Map<Name, Integer> getChildCountsByName() {
        if (childCountsByName == null) {
            childCountsByName = new HashMap<Name, Integer>();
        }
        return childCountsByName;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getCorrespondingNodePath(java.lang.String)
     */
    public String getCorrespondingNodePath( String workspaceName ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getDefinition()
     */
    public NodeDefinition getDefinition() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getIndex()
     */
    public int getIndex() {
        throw new UnsupportedOperationException();
    }

    final UUID getInternalUuid() {
        return uuid;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getLock()
     */
    public Lock getLock() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getMixinNodeTypes()
     */
    public NodeType[] getMixinNodeTypes() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getNode(java.lang.String)
     */
    public final Node getNode( String relativePath ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getNodes()
     */
    public NodeIterator getNodes() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getNodes(java.lang.String)
     */
    public NodeIterator getNodes( String namePattern ) {
        ArgCheck.isNotEmpty(namePattern, "namePattern");
        String[] disjuncts = namePattern.split("\\|");
        List<Segment> nodes = new ArrayList<Segment>();
        for (String disjunct : disjuncts) {
            String pattern = disjunct.trim();
            ArgCheck.isNotEmpty(pattern, "namePattern");
            String ndxPattern;
            int endNdx = pattern.length() - 1;
            if (pattern.charAt(endNdx) == ']') {
                int ndx = pattern.indexOf('[');
                ndxPattern = pattern.substring(ndx + 1, endNdx);
                pattern = pattern.substring(0, ndx);
            } else ndxPattern = null;
            for (Entry<Name, Integer> entry : getChildCountsByName().entrySet()) {
                if (entry.getKey().getString().matches(pattern)) {
                    if (ndxPattern != null && !entry.getValue().toString().matches(ndxPattern)) continue;
                    if (entry.getValue() > 1) nodes.add(new BasicPathSegment(entry.getKey(), entry.getValue()));
                    else nodes.add(new BasicPathSegment(entry.getKey()));
                }
            }
        }
        return new NodeIterator() {

            public Node nextNode() {
                return null;
            }

            public long getPosition() {
                return 0;
            }

            public long getSize() {
                return 0;
            }

            public void skip( long skipNum ) {
            }

            public boolean hasNext() {
                return false;
            }

            public Object next() {
                return null;
            }

            public void remove() {
            }
        };
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getPrimaryItem()
     */
    public Item getPrimaryItem() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
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
    public PropertyIterator getProperties() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getProperties(java.lang.String)
     */
    public PropertyIterator getProperties( String namePattern ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getProperty(java.lang.String)
     */
    public final Property getProperty( String relativePath ) throws RepositoryException {
        ArgCheck.isNotEmpty(relativePath, "relativePath");
        // TODO: Handle multi-segment paths
        for (Property property : properties) {
            if (relativePath.equals(property.getName())) return property;
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getReferences()
     */
    public PropertyIterator getReferences() {
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
        // Check if node is referenceable
        Property mixinsProp = getProperty("jcr:mixinTypes");
        if (mixinsProp != null) {
            for (Value value : mixinsProp.getValues()) {
                if ("mix:referenceable".equals(value.getString())) return getProperty("jcr:uuid").getString();
            }
        }
        throw new UnsupportedRepositoryOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getVersionHistory()
     */
    public VersionHistory getVersionHistory() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#hasNode(java.lang.String)
     */
    public boolean hasNode( String relPath ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#hasNodes()
     */
    public boolean hasNodes() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#hasProperties()
     */
    public boolean hasProperties() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#hasProperty(java.lang.String)
     */
    public boolean hasProperty( String relPath ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#holdsLock()
     */
    public boolean holdsLock() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#isCheckedOut()
     */
    public boolean isCheckedOut() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#isLocked()
     */
    public boolean isLocked() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#isNode()
     */
    public final boolean isNode() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#isNodeType(java.lang.String)
     */
    public boolean isNodeType( String nodeTypeName ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#isSame(javax.jcr.Item)
     */
    public boolean isSame( Item otherItem ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#lock(boolean, boolean)
     */
    public Lock lock( boolean isDeep,
                      boolean isSessionScoped ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#merge(java.lang.String, boolean)
     */
    public NodeIterator merge( String srcWorkspace,
                               boolean bestEffort ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#orderBefore(java.lang.String, java.lang.String)
     */
    public void orderBefore( String srcChildRelPath,
                             String destChildRelPath ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#removeMixin(java.lang.String)
     */
    public void removeMixin( String mixinName ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#restore(java.lang.String, boolean)
     */
    public void restore( String versionName,
                         boolean removeExisting ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#restore(javax.jcr.version.Version, boolean)
     */
    public void restore( Version version,
                         boolean removeExisting ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#restore(javax.jcr.version.Version, java.lang.String, boolean)
     */
    public void restore( Version version,
                         String relPath,
                         boolean removeExisting ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#restoreByLabel(java.lang.String, boolean)
     */
    public void restoreByLabel( String versionLabel,
                                boolean removeExisting ) {
        throw new UnsupportedOperationException();
    }

    final void setChildren( List<Segment> children ) {
        assert children != null;
        for (Segment seg : children) {
            Name name = seg.getName();
            Integer count = getChildCountsByName().get(name);
            if (count == null) getChildCountsByName().put(name, 1);
            else getChildCountsByName().put(name, count + 1);
        }
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
     * @see javax.jcr.Node#setProperty(java.lang.String, boolean)
     */
    public Property setProperty( String name,
                                 boolean value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, java.util.Calendar)
     */
    public Property setProperty( String name,
                                 Calendar value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, double)
     */
    public Property setProperty( String name,
                                 double value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, java.io.InputStream)
     */
    public Property setProperty( String name,
                                 InputStream value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, long)
     */
    public Property setProperty( String name,
                                 long value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Node)
     */
    public Property setProperty( String name,
                                 Node value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, java.lang.String)
     */
    public Property setProperty( String name,
                                 String value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, java.lang.String, int)
     */
    public Property setProperty( String name,
                                 String value,
                                 int type ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, java.lang.String[])
     */
    public Property setProperty( String name,
                                 String[] values ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, java.lang.String[], int)
     */
    public Property setProperty( String name,
                                 String[] values,
                                 int type ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Value)
     */
    public Property setProperty( String name,
                                 Value value ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Value, int)
     */
    public Property setProperty( String name,
                                 Value value,
                                 int type ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Value[])
     */
    public Property setProperty( String name,
                                 Value[] values ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#setProperty(java.lang.String, javax.jcr.Value[], int)
     */
    public Property setProperty( String name,
                                 Value[] values,
                                 int type ) {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#unlock()
     */
    public void unlock() {
        throw new UnsupportedOperationException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#update(java.lang.String)
     */
    public void update( String srcWorkspaceName ) {
        throw new UnsupportedOperationException();
    }
}
