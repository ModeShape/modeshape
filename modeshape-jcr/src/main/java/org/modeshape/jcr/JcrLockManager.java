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

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
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
    private final Set<String> lockTokens = Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    JcrLockManager( JcrSession session,
                    RepositoryLockManager lockManager ) {
        this.session = session;
        this.lockManager = lockManager;
    }

    boolean hasLockToken( String token ) {
        return !StringUtil.isBlank(token) && lockTokens.contains(token);
    }

    /**
     * Unlocks all locks corresponding to the tokens held by the supplied session.
     * 
     * @throws RepositoryException if the session is not live
     */
    final void cleanLocks() throws RepositoryException {
        // clean the session-scoped locks: unlock all the nodes and remove the lock tokens from the system area
        Set<String> cleanedTokens = lockManager.cleanLocks(session);
        if (lockTokens.isEmpty()) {
            return;
        }
        // clean all open-scoped locks created via this session
        for (String lockToken : lockTokens) {
            if (!cleanedTokens.contains(lockToken)) {
                ModeShapeLock lock = lockManager.findLockByToken(lockToken);
                if (lock == null) {
                    // there is no existing lock for this token (it must've been removed from somewhere else)
                    continue;
                }
                // this is an open-scoped lock created via this session for which we'll have to update the 'held' flag
                if (!lockManager.setHeldBySession(session, lockToken, false)) {
                    // Generally not expected, because if the lock exists we can always change the lock-held value to false
                    // even when it is already false ...
                    throw new LockException(JcrI18n.invalidLockToken.text(lockToken));
                }
            }
        }
        // always clean our internal map of tokens
        lockTokens.clear();
    }

    @Override
    public void addLockToken( String lockToken ) throws LockException {
        CheckArg.isNotNull(lockToken, "lockToken");

        if (lockTokens.contains(lockToken)) {
            // We already hold the token ...
            return;
        }

        // Change the lock to be held by a session ...
        try {
            if (!lockManager.setHeldBySession(session, lockToken, true)) {
                throw new LockException(JcrI18n.lockTokenAlreadyHeld.text(lockToken));
            }
            lockTokens.add(lockToken);
        } catch (LockException e) {
            lockTokens.remove(lockToken);
            throw e;
        }
    }

    @Override
    public void removeLockToken( String lockToken ) throws LockException {
        CheckArg.isNotNull(lockToken, "lockToken");

        // Trivial case of giving a token back to ourself
        if (!lockTokens.contains(lockToken)) {
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
        return Collections.unmodifiableSet(lockTokens);
    }

    @Override
    public String[] getLockTokens() {
        Set<String> tokens =  new HashSet<String>();
        for (String token : lockTokens) {
            ModeShapeLock lock = lockManager.findLockByToken(token);
            if (lock != null && !lock.isSessionScoped()) {
                tokens.add(token);
            }
        }
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

    public Lock getLockIfExists( AbstractJcrNode node ) throws AccessDeniedException, RepositoryException {
        ModeShapeLock lock = getLowestLockAlongPath(node);
        return lock == null ? null : lock.lockFor(session);
    }

    public ModeShapeLock getLowestLockAlongPath( final AbstractJcrNode node )
        throws PathNotFoundException, AccessDeniedException, RepositoryException {
        session.checkLive();
        if (!this.session.repository().lockingUsed()) {
            //if locking hasn't been used at all, there's nothing to check
            return null;
        }
        SessionCache sessionCache = session.cache();
        NodeCache cache = sessionCache;
        NodeKey nodeKey = node.key();
        NodeKey key = nodeKey;
        while (key != null) {
            ModeShapeLock lock = lockManager.findLockFor(key);
            if (lock != null && (lock.isDeep() || nodeKey.equals(lock.getLockedNodeKey()))) {
                // There is a lock that applies to 'node', either because the lock is actually on 'node' or because
                // an ancestor node is locked with a deep lock...
                return lock;
            }
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

    /**
     * Determine if this lock manager holds a lock on the supplied node.
     * 
     * @param node the node; may not be null
     * @return true if this lock maanger does hold a lock on the node, or false otherwise
     */
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

    /**
     * Attempt to obtain a lock on the supplied node.
     * 
     * @param node the node; may not be null
     * @param isDeep true if the lock should be a deep lock
     * @param isSessionScoped true if the lock should be scoped to the session
     * @param timeoutHint desired lock timeout in seconds (servers are free to ignore this value); specify {@link Long#MAX_VALUE}
     *        for no timeout.
     * @param ownerInfo a string containing owner information supplied by the client, and recorded on the lock; if null, then the
     *        session's user ID will be used
     * @return the lock; never null
     * @throws AccessDeniedException if the caller does not have privilege to lock the node
     * @throws InvalidItemStateException if the node has been modified and cannot be locked
     * @throws LockException if the lock could not be obtained
     * @throws RepositoryException if an error occurs updating the graph state
     */
    public Lock lock( AbstractJcrNode node,
                      boolean isDeep,
                      boolean isSessionScoped,
                      long timeoutHint,
                      String ownerInfo )
        throws LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        if (!node.isLockable()) {
            throw new LockException(JcrI18n.nodeNotLockable.text(node.location()));
        }
        if (node.isLocked()) {
            throw new LockException(JcrI18n.alreadyLocked.text(node.location()));            
        }
        if (node.isNew()|| node.isModified()) {
            throw new InvalidItemStateException(JcrI18n.changedNodeCannotBeLocked.text(node.location()));
        }

        // Try to obtain the lock ...
        ModeShapeLock lock = lockManager.lock(session, node.node(), isDeep, isSessionScoped, timeoutHint, ownerInfo);
        String token = lock.getLockToken();
        lockTokens.add(token);
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
        if (lock != null && !lockTokens.contains(lock.getLockToken())) {
            // Someone else holds the lock, so see if the user has the permission to break someone else's lock ...
            try {
                session.checkPermission(session.workspaceName(), node.path(), ModeShapePermissions.UNLOCK_ANY);
            } catch (AccessDeniedException e) {
                //expected by the TCK
                throw new LockException(e);
            }
        }

        // Remove the lock ...
        String lockToken = lockManager.unlock(session, node.key());
        lockTokens.remove(lockToken);
    }

}
