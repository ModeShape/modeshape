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
import javax.jcr.ItemNotFoundException;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.lock.LockException;
import javax.jcr.nodetype.ConstraintViolationException;
import javax.jcr.version.VersionException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionCache;

/**
 * A concrete {@link Node JCR Node} implementation.
 * 
 * @see JcrRootNode
 */
@ThreadSafe
class JcrNode extends AbstractJcrNode {

    JcrNode( JcrSession session,
             NodeKey nodeKey ) {
        super(session, nodeKey);
    }

    @Override
    final boolean isRoot() {
        return false;
    }

    @Override
    Type type() {
        return Type.NODE;
    }

    @Override
    public int getIndex() throws RepositoryException {
        return segment().getIndex();
    }

    @Override
    public String getName() throws RepositoryException {
        return segment().getName().getString(namespaces());
    }

    @Override
    public AbstractJcrNode getParent() throws ItemNotFoundException, RepositoryException {
        checkSession();
        NodeKey parentKey = node().getParentKey(sessionCache());
        if (parentKey == null) {
            int x = 0;
        }
        return session().node(parentKey, null);
    }

    @Override
    public String getPath() throws RepositoryException {
        // checkSession(); ideally we don't have to do this, because getting the path is a useful thing and is used in 'toString'
        return path().getString(namespaces());
    }

    @Override
    protected void doRemove()
        throws VersionException, LockException, ConstraintViolationException, AccessDeniedException, RepositoryException {

        SessionCache cache = sessionCache();
        NodeKey key = key();
        MutableCachedNode parent = mutableParent();
        parent.removeChild(cache, key);
        cache.destroy(key);
    }
}
