/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import org.modeshape.jcr.JcrSharedNodeCache.SharedSet;
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
        return session().node(parentKey, null);
    }

    @Override
    public String getPath() throws RepositoryException {
        // checkSession(); ideally we don't have to do this, because getting the path is a useful thing and is used in 'toString'
        return path().getString(namespaces());
    }

    @Override
    boolean isShared() {
        // Sometimes, a shareable and shared node is represented by a JcrNode rather than a JcrSharedNode. Therefore,
        // the share-related logic needs to be done here ...
        try {
            return isShareable() && sharedSet().getSize() > 1;
        } catch (RepositoryException e) {
            // Shouldn't really happen, but just in case ...
            throw new RuntimeException(e);
        }
    }

    @Override
    protected void doRemove()
        throws VersionException, LockException, ConstraintViolationException, AccessDeniedException, RepositoryException {
        // Sometimes, a shareable and shared node is represented by a JcrNode rather than a JcrSharedNode. Therefore,
        // the share-related logic needs to be done here ...

        SessionCache cache = sessionCache();
        NodeKey key = key();
        MutableCachedNode parent = mutableParent();
        if (!isShareable()) {
            // It's not shareable, so we will always destroy the node immediately ...
            parent.removeChild(cache, key);
            cache.destroy(key);
            return;
        }

        // It is shareable, so we need to check how many shares there are before we remove this node from its parent ...
        JcrSharedNodeCache shareableNodeCache = session().shareableNodeCache();
        SharedSet sharedSet = shareableNodeCache.getSharedSet(this);
        if (sharedSet.getSize() <= 1) {
            // There are no shares, so destroy the node and the shared set ...
            parent.removeChild(cache, key);
            cache.destroy(key);
            shareableNodeCache.destroyed(key);
            return;
        }

        // The node being removed is shared to at least two places, so we should remove it from the primary parent,
        // NOT destroy the node, and adjust the SharedSet ...
        parent.removeChild(cache, key);
        shareableNodeCache.removed(this);
    }
}
