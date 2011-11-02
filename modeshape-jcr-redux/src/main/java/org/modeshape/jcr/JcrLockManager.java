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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.RepositoryLockManager.ModeShapeLock;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.LockFailureException;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionCache;

/**
 * A {@link LockManager} implementation owned and used by a {@link JcrSession session}. This object is relatively lightweight, and
 * maintains the set of lock tokens owned by this session plus references to the session and the repository-wide lock manager.
 */
@ThreadSafe
class JcrLockManager implements LockManager {

    private final JcrSession session;
    private final RepositoryLockManager lockManager;
    private final ConcurrentMap<String, Object> lockTokens = new ConcurrentHashMap<String, Object>();

    JcrLockManager( JcrSession session,
                    RepositoryLockManager lockManager ) {
        this.session = session;
        this.lockManager = lockManager;
    }

    boolean hasLockToken( String token ) {
        return lockTokens.containsKey(token);
    }

    @Override
    public void addLockToken( String lockToken ) throws LockException {
        CheckArg.isNotNull(lockToken, "lockToken");

        // Trivial case of giving a token back to ourself
        if (lockTokens.putIfAbsent(lockToken, null) != null) {
            // We already hold the token ...
            return;
        }

        // Change the lock to be held by a session ...
        try {
            if (!lockManager.setHeldBySession(session, lockToken, true)) {
                throw new LockException(JcrI18n.lockTokenAlreadyHeld.text(lockToken));
            }
        } catch (LockException e) {
            lockTokens.remove(lockToken);
            throw e;
        }
    }

    @Override
    public void removeLockToken( String lockToken ) throws LockException {
        CheckArg.isNotNull(lockToken, "lockToken");

        // Trivial case of giving a token back to ourself
        if (lockTokens.containsKey(lockToken)) {
            // We don't already hold the token ...
            throw new LockException(JcrI18n.invalidLockToken.text(lockToken));
        }

        // Change the lock to be no longer held by a session ...
        try {
            if (!lockManager.setHeldBySession(session, lockToken, false)) {
                // Generally not expected, because if the lock exists we can always change the lock-held value to false
                // even when it is already false ...
                throw new LockException(JcrI18n.invalidLockToken.text(lockToken));
            }

            // Now remove the tokens from our session ...
            lockTokens.remove(lockToken);
        } catch (LockFailureException e) {
            lockTokens.remove(lockToken);
            throw new LockException(JcrI18n.invalidLockToken.text(lockToken));
        }
    }

    Set<String> lockTokens() {
        return Collections.unmodifiableSet(lockTokens.keySet());
    }

    @Override
    public String[] getLockTokens() {
        // Make a copy (which can be done atomically) since getting the size and iterating over the (possibly-changing) values
        // cannot be done atomically...
        Set<String> tokens = new HashSet<String>(lockTokens.keySet());
        return tokens.toArray(new String[tokens.size()]);
    }

    @Override
    public boolean isLocked( String absPath ) throws PathNotFoundException, RepositoryException {
        return getLowestLockAlongPath(session.node(session.absolutePathFor(absPath))) != null;
    }

    public boolean isLocked( AbstractJcrNode node ) throws PathNotFoundException, RepositoryException {
        return getLowestLockAlongPath(node) != null;
    }

    @Override
    public Lock getLock( String absPath ) throws PathNotFoundException, LockException, AccessDeniedException, RepositoryException {
        ModeShapeLock lock = getLowestLockAlongPath(session.node(session.absolutePathFor(absPath)));
        if (lock != null) return lock.lockFor(session);
        throw new LockException(JcrI18n.notLocked.text(absPath));
    }

    public Lock getLock( AbstractJcrNode node ) throws LockException, AccessDeniedException, RepositoryException {
        ModeShapeLock lock = getLowestLockAlongPath(node);
        if (lock != null) return lock.lockFor(session);
        throw new LockException(JcrI18n.notLocked.text(node.getPath()));
    }

    public ModeShapeLock getLowestLockAlongPath( final AbstractJcrNode node )
        throws PathNotFoundException, AccessDeniedException, RepositoryException {
        session.checkLive();

        SessionCache sessionCache = session.cache();
        NodeCache cache = sessionCache;
        NodeKey key = node.key();
        while (key != null) {
            ModeShapeLock lock = lockManager.findLockFor(key);
            if (lock != null) return lock;
            // Otherwise, get the parent, but use the cache directly ...
            CachedNode cachedNode = cache.getNode(key);
            if (cachedNode == null) {
                // The node has been removed, so get the node from the workspace cache ...
                if (sessionCache == cache) {
                    cache = sessionCache.getWorkspace();
                    cachedNode = cache.getNode(key);
                }
                if (cachedNode == null) break;
            }
            key = cachedNode.getParentKey(cache);
        }
        return null;
    }

    @Override
    public boolean holdsLock( String absPath ) throws PathNotFoundException, RepositoryException {
        AbstractJcrNode node = session.node(session.absolutePathFor(absPath));
        return lockManager.findLockFor(node.key()) != null;
    }

    public boolean holdsLock( AbstractJcrNode node ) {
        return lockManager.findLockFor(node.key()) != null;
    }

    @Override
    public Lock lock( String absPath,
                      boolean isDeep,
                      boolean isSessionScoped,
                      long timeoutHint,
                      String ownerInfo )
        throws LockException, PathNotFoundException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        AbstractJcrNode node = session.node(session.absolutePathFor(absPath));
        return lock(node, isDeep, isSessionScoped, timeoutHint, ownerInfo);
    }

    public Lock lock( AbstractJcrNode node,
                      boolean isDeep,
                      boolean isSessionScoped,
                      long timeoutHint,
                      String ownerInfo )
        throws LockException, PathNotFoundException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        if (!node.isLockable()) {
            throw new LockException(JcrI18n.nodeNotLockable.text(node.getPath()));
        }

        if (node.isModified()) {
            throw new InvalidItemStateException(JcrI18n.changedNodeCannotBeLocked.text(node.getPath()));
        }

        // Try to obtain the lock ...
        ModeShapeLock lock = lockManager.lock(session, node.node(), isDeep, isSessionScoped, timeoutHint, ownerInfo);
        lockTokens.put(lock.getLockToken(), null);
        return lock.lockFor(session);
    }

    @Override
    public void unlock( String absPath )
        throws PathNotFoundException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        AbstractJcrNode node = session.node(session.absolutePathFor(absPath));
        unlock(node);
    }

    public void unlock( AbstractJcrNode node )
        throws PathNotFoundException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {

        if (node.isModified()) {
            throw new InvalidItemStateException(JcrI18n.changedNodeCannotBeUnlocked.text(node.getPath()));
        }

        ModeShapeLock lock = lockManager.findLockFor(node.key());
        if (lock != null && !lockTokens.containsKey(lock.getLockToken())) {
            // Someone else holds the lock, so see if the user has the permission to break someone else's lock ...
            session.checkPermission(session.workspaceName(), node.path(), ModeShapePermissions.UNLOCK_ANY);
        }

        // Remove the lock ...
        String lockToken = lockManager.unlock(session, node.key());
        lockTokens.remove(lockToken);
    }

}
