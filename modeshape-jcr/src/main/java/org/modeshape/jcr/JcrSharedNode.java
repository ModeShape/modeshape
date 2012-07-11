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

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.ItemNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.JcrSharedNodeCache.SharedSet;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;

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
@ThreadSafe
final class JcrSharedNode extends JcrNode {

    private final NodeKey parentKey;
    private final SharedSet sharedSet;

    protected JcrSharedNode( SharedSet sharedSet,
                             NodeKey parentKey ) {
        super(sharedSet.session(), sharedSet.key());
        this.parentKey = parentKey;
        this.sharedSet = sharedSet;
        assert this.parentKey != null;
        assert this.sharedSet != null;
    }

    @Override
    boolean isShared() {
        return true;
    }

    @Override
    protected NodeKey parentKey() {
        return parentKey;
    }

    @Override
    protected void doRemove()
        throws VersionException, LockException, ConstraintViolationException, AccessDeniedException, RepositoryException {
        // Remove this from the shared set ...
        sharedSet.remove(this);

        // Then remove the child from the parent but do not destroy the node ...
        SessionCache cache = sessionCache();
        NodeKey key = key();
        MutableCachedNode parent = mutableParent();
        parent.removeChild(cache, key);
    }

    @Override
    public AbstractJcrNode getParent() throws ItemNotFoundException, RepositoryException {
        checkSession();
        return parent();
    }

    protected AbstractJcrNode parent() throws ItemNotFoundException {
        return session().node(parentKey, null);
    }

    @Override
    SharedSet sharedSet() {
        return sharedSet;
    }

    @Override
    Path path() throws ItemNotFoundException, InvalidItemStateException {
        AbstractJcrNode parent = parent();
        CachedNode node = parent.node();
        SessionCache cache = session.cache();
        ChildReference childRef = node.getChildReferences(cache).getChild(sharedSet.key());
        Path parentPath = parent.path();
        return session().pathFactory().create(parentPath, childRef.getSegment());
    }

    @Override
    protected Name name() throws RepositoryException {
        return segment().getName();
    }

    @Override
    protected Segment segment() throws RepositoryException {
        AbstractJcrNode parent = parent();
        CachedNode node = parent.node();
        SessionCache cache = session.cache();
        ChildReference childRef = node.getChildReferences(cache).getChild(sharedSet.key());
        return childRef.getSegment();
    }
}
