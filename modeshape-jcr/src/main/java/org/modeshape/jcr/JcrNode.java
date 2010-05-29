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

import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.graph.Location;
import org.modeshape.graph.session.GraphSession.NodeId;
import org.modeshape.jcr.api.Lock;

/**
 * A concrete {@link Node JCR Node} implementation.
 * 
 * @see JcrRootNode
 */
@NotThreadSafe
class JcrNode extends AbstractJcrNode {

    JcrNode( SessionCache cache,
             NodeId nodeId,
             Location location ) {
        super(cache, nodeId, location);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.AbstractJcrNode#isRoot()
     */
    @Override
    final boolean isRoot() {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Node#getIndex()
     */
    public int getIndex() throws RepositoryException {
        return segment().getIndex();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getName()
     */
    public String getName() throws RepositoryException {
        return name().getString(namespaces());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getParent()
     */
    @Override
    public AbstractJcrNode getParent() throws ItemNotFoundException, RepositoryException {
        checkSession();
        return nodeInfo().getParent().getPayload().getJcrNode();
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#getPath()
     */
    public String getPath() throws RepositoryException {
        // checkSession(); ideally we don't have to do this, because getting the path is a useful thing and is used in 'toString'
        return nodeInfo().getPath().getString(namespaces());
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.Item#remove()
     */
    public void remove() throws RepositoryException, LockException {
        Node parentNode = getParent();
        if (parentNode.isLocked()) {
            Lock parentLock = lockManager().getLock(this);
            if (parentLock != null && !parentLock.isLockOwningSession()) {
                throw new LockException(JcrI18n.lockTokenNotHeld.text(this.location));
            }
        }

        if (!parentNode.isCheckedOut()) {
            throw new VersionException(JcrI18n.nodeIsCheckedIn.text(parentNode.getPath()));
        }

        JcrNodeDefinition nodeDefn = cache.nodeTypes().getNodeDefinition(nodeInfo().getPayload().getDefinitionId());

        if (nodeDefn.isProtected()) {
            throw new ConstraintViolationException(JcrI18n.cannotRemoveItemWithProtectedDefinition.text(getPath()));
        }

        session().recordRemoval(location); // do this first before we destroy the node!
        editor().destroy();
    }
}
