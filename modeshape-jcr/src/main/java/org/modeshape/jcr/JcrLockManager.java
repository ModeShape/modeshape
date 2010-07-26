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

import java.security.AccessControlException;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;
import java.util.UUID;
import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import javax.jcr.lock.LockManager;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.session.GraphSession.Node;
import org.modeshape.jcr.SessionCache.JcrNodePayload;
import org.modeshape.jcr.SessionCache.JcrPropertyPayload;
import org.modeshape.jcr.WorkspaceLockManager.ModeShapeLock;

/**
 * A per-session lock manager for a given workspace. This class encapsulates the session-specific locking logic and checks that do
 * not occur in @{link WorkspaceLockManager}.
 */
public class JcrLockManager implements LockManager {

    private final JcrSession session;
    private final WorkspaceLockManager lockManager;
    private final Set<String> lockTokens;

    JcrLockManager( JcrSession session,
                    WorkspaceLockManager lockManager ) {
        this.session = session;
        this.lockManager = lockManager;
        lockTokens = new HashSet<String>();
    }

    @Override
    public void addLockToken( String lockToken ) throws LockException {
        CheckArg.isNotNull(lockToken, "lock token");

        // Trivial case of giving a token back to ourself
        if (lockTokens.contains(lockToken)) {
            return;
        }

        if (lockManager.isHeldBySession(session, lockToken)) {
            throw new LockException(JcrI18n.lockTokenAlreadyHeld.text(lockToken));
        }

        lockManager.setHeldBySession(session, lockToken, true);
        lockTokens.add(lockToken);
    }

    @Override
    public Lock getLock( String absPath ) throws PathNotFoundException, LockException, AccessDeniedException, RepositoryException {
        AbstractJcrNode node = session.getNode(absPath);
        return getLock(node);
    }

    Lock getLock( AbstractJcrNode node ) throws PathNotFoundException, LockException, AccessDeniedException, RepositoryException {
        WorkspaceLockManager.ModeShapeLock lock = lockFor(node);
        if (lock != null) return lock.lockFor(node.cache);
        throw new LockException(JcrI18n.notLocked.text(node.location()));
    }

    @Override
    public String[] getLockTokens() {
        Set<String> publicTokens = new HashSet<String>(lockTokens);

        for (Iterator<String> iter = publicTokens.iterator(); iter.hasNext();) {
            String token = iter.next();
            WorkspaceLockManager.ModeShapeLock lock = lockManager.lockFor(token);
            if (lock.isSessionScoped()) iter.remove();
        }

        return publicTokens.toArray(new String[publicTokens.size()]);
    }

    Set<String> lockTokens() {
        return this.lockTokens;
    }

    @Override
    public boolean holdsLock( String absPath ) throws PathNotFoundException, RepositoryException {
        AbstractJcrNode node = session.getNode(absPath);
        return holdsLock(node);
    }

    boolean holdsLock( AbstractJcrNode node ) {
        WorkspaceLockManager.ModeShapeLock lock = lockManager.lockFor(session, node.location());

        return lock != null;

    }

    @Override
    public boolean isLocked( String absPath ) throws PathNotFoundException, RepositoryException {
        AbstractJcrNode node = session.getNode(absPath);
        return isLocked(node);
    }

    boolean isLocked( AbstractJcrNode node ) throws PathNotFoundException, RepositoryException {
        return lockFor(node) != null;
    }

    @Override
    public Lock lock( String absPath,
                      boolean isDeep,
                      boolean isSessionScoped,
                      long timeoutHint,
                      String ownerInfo )
        throws LockException, PathNotFoundException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        AbstractJcrNode node = session.getNode(absPath);
        return lock(node, isDeep, isSessionScoped, timeoutHint, ownerInfo);
    }

    Lock lock( AbstractJcrNode node,
               boolean isDeep,
               boolean isSessionScoped,
               long timeoutHint,
               String ownerInfo )
        throws LockException, PathNotFoundException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        if (!node.isLockable()) {
            throw new LockException(JcrI18n.nodeNotLockable.text(node.getPath()));
        }

        if (node.isLocked()) {
            throw new LockException(JcrI18n.alreadyLocked.text(node.location()));
        }

        if (node.isModified()) {
            throw new InvalidItemStateException();
        }

        if (isDeep) {
            LinkedList<Node<JcrNodePayload, JcrPropertyPayload>> nodesToVisit = new LinkedList<Node<JcrNodePayload, JcrPropertyPayload>>();
            nodesToVisit.add(node.nodeInfo());

            while (!nodesToVisit.isEmpty()) {
                Node<JcrNodePayload, JcrPropertyPayload> graphNode = nodesToVisit.remove(nodesToVisit.size() - 1);
                if (lockManager.lockFor(session, graphNode.getLocation()) != null) throw new LockException(
                                                                                                           JcrI18n.parentAlreadyLocked.text(node.location,
                                                                                                                                            graphNode.getLocation()));

                for (Node<JcrNodePayload, JcrPropertyPayload> child : graphNode.getChildren()) {
                    nodesToVisit.add(child);
                }
            }
        }

        WorkspaceLockManager.ModeShapeLock lock = lockManager.lock(session, node.location(), isDeep, isSessionScoped);

        addLockToken(lock.getLockToken());
        return lock.lockFor(session.cache());

    }

    @Override
    public void removeLockToken( String lockToken ) throws LockException {
        CheckArg.isNotNull(lockToken, "lockToken");
        // A LockException is thrown if the lock associated with the specified lock token is session-scoped.

        if (!lockTokens.contains(lockToken)) {
            throw new LockException(JcrI18n.invalidLockToken.text(lockToken));
        }

        /*
         * The JCR API library that we're using diverges from the spec in that it doesn't declare
         * this method to throw a LockException.  We'll throw a runtime exception for now.
         */

        ModeShapeLock lock = lockManager.lockFor(lockToken);
        if (lock == null) {
            // The lock is no longer valid
            lockTokens.remove(lockToken);
            return;
        }

        if (lock.isSessionScoped()) {
            throw new IllegalStateException(JcrI18n.cannotRemoveLockToken.text(lockToken));
        }

        lockManager.setHeldBySession(session, lockToken, false);
        lockTokens.remove(lockToken);
    }

    @Override
    public void unlock( String absPath )
        throws PathNotFoundException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        AbstractJcrNode node = session.getNode(absPath);
        unlock(node);
    }

    @SuppressWarnings( "unused" )
    void unlock( AbstractJcrNode node )
        throws PathNotFoundException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException {
        WorkspaceLockManager.ModeShapeLock lock = lockManager.lockFor(session, node.location());

        if (lock == null) {
            throw new LockException(JcrI18n.notLocked.text(node.location()));
        }

        if (lockTokens.contains(lock.getLockToken())) {
            lockManager.unlock(session.getExecutionContext(), lock);
            removeLockToken(lock.getLockToken());
        } else {
            try {
                // See if the user has the permission to break someone else's lock
                session.checkPermission(session.cache().workspaceName(), null, ModeShapePermissions.UNLOCK_ANY);

                // This user doesn't have the lock token, so don't try to remove it
                lockManager.unlock(session.getExecutionContext(), lock);
            } catch (AccessControlException iae) {
                throw new LockException(JcrI18n.lockTokenNotHeld.text(node.location()));
            }
        }

    }

    /**
     * 
     */
    final void cleanLocks() {
        lockManager.cleanLocks(session);
    }

    final WorkspaceLockManager.ModeShapeLock lockFor( AbstractJcrNode node ) throws RepositoryException {
        // This can only happen in mocked testing.
        if (session == null || session.workspace() == null) return null;

        WorkspaceLockManager.ModeShapeLock lock = lockManager.lockFor(session, node.location());
        if (lock != null) return lock;

        AbstractJcrNode parent = node;
        while (!parent.isRoot()) {
            parent = parent.getParent();

            WorkspaceLockManager.ModeShapeLock parentLock = lockManager.lockFor(session, parent.location());
            if (parentLock != null && parentLock.isLive()) {
                return parentLock.isDeep() ? parentLock : null;
            }
        }
        return null;
    }

    final WorkspaceLockManager.ModeShapeLock lockFor( UUID nodeUuid ) {
        // This can only happen in mocked testing.
        if (session == null || session.workspace() == null) return null;

        return lockManager.lockFor(nodeUuid);
    }

}
