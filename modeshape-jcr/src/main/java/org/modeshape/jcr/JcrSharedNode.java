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

import java.util.ArrayList;
import java.util.Collection;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.ConstraintViolationException;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.Location;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.session.GraphSession.Node;
import org.modeshape.jcr.SessionCache.JcrNodePayload;
import org.modeshape.jcr.SessionCache.JcrPropertyPayload;

/**
 * A concrete {@link javax.jcr.Node JCR Node} implementation that is used for all nodes that are part of a shared set but not the
 * original node that was shared. In essense, all instances of JcrShareableNode are proxies that have their own name and location,
 * but that delegate nearly all other operations to the referenced node.
 * <p>
 * Instances of this class are created for each node in a workspace that have a primary type of "{@link ModeShapeLexicon#SHARE
 * mode:share}". These nodes each have a single "{@link ModeShapeLexicon#SHARED_UUID mode:sharedUuid}" REFERENCE property that
 * points to the original shareable node. Thus, with the help of this class, JCR clients do not ever see this "mode:share" node,
 * but instead see the original sharable node.
 * </p>
 * 
 * @see JcrRootNode
 * @see JcrNode
 */
@NotThreadSafe
class JcrSharedNode extends JcrNode {

    /** The UUID of the "mode:share" proxy node, hidden from the user. */
    private AbstractJcrNode original;
    private AbstractJcrNode proxy;

    JcrSharedNode( AbstractJcrNode proxy,
                   AbstractJcrNode original ) {
        // Set the super's nodeId and location to be that of the original, not the proxy. We'll override
        // all the methods that need the proxy information ....
        super(proxy.cache, original.nodeId, original.location);
        this.proxy = proxy;
        this.original = original;
        assert proxy.cache == original.cache : "Only able to share nodes within the same cache";
        assert !proxy.isRoot() : "The root node can never be a shared node";
        assert !original.isRoot() : "The root node can never be shareable";
    }

    /**
     * Get the node that represents the proxy, and is a true representation of the underlying node with a primary type of
     * {@link ModeShapeLexicon#SHARE mode:share} and lone {@link ModeShapeLexicon#SHARED_UUID mode:sharedUuid} property.
     * 
     * @return the proxy node
     */
    AbstractJcrNode proxyNode() {
        return proxy;
    }

    /**
     * Get the node that represents the original node that is being shared by this proxy.
     * 
     * @return the original node
     */
    AbstractJcrNode originalNode() {
        return original;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.AbstractJcrNode#isShareable()
     */
    @Override
    boolean isShareable() {
        return original != null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.AbstractJcrNode#isShared()
     */
    @Override
    boolean isShared() {
        return true;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.AbstractJcrNode#setLocation(org.modeshape.graph.Location)
     */
    @Override
    void setLocation( Location location ) {
        proxyNode().setLocation(location);
    }

    @Override
    protected Location locationToDestroy() {
        return proxyNode().location();
    }

    @Override
    protected void doDestroy() throws AccessDeniedException, RepositoryException {
        proxyNode().editor().destroy();
    }

    @Override
    Node<JcrNodePayload, JcrPropertyPayload> nodeInfo()
        throws InvalidItemStateException, AccessDeniedException, RepositoryException {
        return original.nodeInfo();
    }

    Node<JcrNodePayload, JcrPropertyPayload> proxyInfo()
        throws InvalidItemStateException, AccessDeniedException, RepositoryException {
        return proxy.nodeInfo();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The parent of this shared node is the parent of the proxy, not of the original shareable node.
     * </p>
     * 
     * @see org.modeshape.jcr.AbstractJcrNode#parentNodeInfo()
     */
    @Override
    Node<JcrNodePayload, JcrPropertyPayload> parentNodeInfo()
        throws InvalidItemStateException, AccessDeniedException, RepositoryException {
        return proxyInfo().getParent();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The path of this shared node is the path of the proxy, not of the original shareable node.
     * </p>
     * 
     * @see org.modeshape.jcr.AbstractJcrNode#path()
     */
    @Override
    Path path() throws RepositoryException {
        return proxyInfo().getPath();
    }

    /**
     * {@inheritDoc}
     * <p>
     * The segment of this shared node is the segment of the proxy, not of the original shareable node.
     * </p>
     * 
     * @see org.modeshape.jcr.AbstractJcrNode#path()
     */
    @Override
    Path.Segment segment() throws RepositoryException {
        return proxyInfo().getSegment();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.AbstractJcrNode#getCorrespondenceId()
     */
    @Override
    protected CorrespondenceId getCorrespondenceId() throws RepositoryException {
        return original.getCorrespondenceId();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.AbstractJcrNode#removeMixin(java.lang.String)
     */
    @Override
    public void removeMixin( String mixinName ) throws RepositoryException {
        if (cache.stringFactory().create(JcrMixLexicon.SHAREABLE).equals(mixinName)) {
            // Per section 14.15 of the JCR 2.0 specification, we can do a few things.
            // We could remove this shared node via removeShare(),
            // or do something else to the content to adjust to the missing mixin name,
            // or we could throw an exception ...
            throw new ConstraintViolationException();
        }
        super.removeMixin(mixinName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.AbstractJcrNode#getProperty(java.lang.String)
     */
    @Override
    public Property getProperty( String relativePath ) throws RepositoryException {
        return adapt(super.getProperty(relativePath));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.AbstractJcrNode#getProperties()
     */
    @Override
    public PropertyIterator getProperties() throws RepositoryException {
        return adapt(super.getProperties());
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.AbstractJcrNode#getProperties(java.lang.String)
     */
    @Override
    public PropertyIterator getProperties( String namePattern ) throws RepositoryException {
        return adapt(super.getProperties(namePattern));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.AbstractJcrNode#getProperties(java.lang.String[])
     */
    @Override
    public PropertyIterator getProperties( String[] nameGlobs ) throws RepositoryException {
        return adapt(super.getProperties(nameGlobs));
    }

    /**
     * Adapt the property objects so that their owner is this node, not the original's.
     * 
     * @param property the property from the original
     * @return the adapted property
     */
    protected Property adapt( Property property ) {
        if (property instanceof JcrSingleValueProperty) {
            JcrSingleValueProperty original = (JcrSingleValueProperty)property;
            return new JcrSingleValueProperty(cache, this, original.name());
        }
        if (property instanceof JcrMultiValueProperty) {
            JcrMultiValueProperty original = (JcrMultiValueProperty)property;
            return new JcrMultiValueProperty(cache, this, original.name());
        }
        return property;
    }

    protected PropertyIterator adapt( PropertyIterator propertyIter ) {
        Collection<Property> props = new ArrayList<Property>((int)propertyIter.getSize());
        while (propertyIter.hasNext()) {
            props.add(adapt(propertyIter.nextProperty()));
        }
        return new JcrPropertyIterator(props);
    }
}
