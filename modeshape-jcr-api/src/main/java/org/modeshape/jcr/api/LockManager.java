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
package org.modeshape.jcr.api;

import javax.jcr.AccessDeniedException;
import javax.jcr.InvalidItemStateException;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;

/**
 * Placeholder for the JCR 2.0 LockManager interface
 */
public interface LockManager {

    /**
     * Adds the specified lock token to the current <code>Session</code>. Holding a lock token makes the current
     * <code>Session</code> the owner of the lock specified by that particular lock token.
     * 
     * @param lockToken a lock token (a string).
     * @throws LockException if the specified lock token is already held by another <code>Session</code> and the implementation
     *         does not support simultaneous ownership of open-scoped locks.
     * @throws RepositoryException if another error occurs.
     */
    public void addLockToken( String lockToken ) throws LockException, RepositoryException;

    /**
     * Returns the <code>Lock</code> object that applies to the node at the specified <code>absPath</code>. This may be either a
     * lock on that node itself or a deep lock on a node above that node.
     * <p>
     * 
     * @param absPath absolute path of node for which to obtain the lock
     * @return The applicable <code>Lock</code> object.
     * @throws LockException if no lock applies to this node.
     * @throws AccessDeniedException if the current session does not have sufficent access to get the lock.
     * @throws PathNotFoundException if no node is found at <code>absPath</code>
     * @throws RepositoryException if another error occurs.
     */
    public Lock getLock( String absPath ) throws PathNotFoundException, LockException, AccessDeniedException, RepositoryException;

    /**
     * Returns an array containing all lock tokens currently held by the current <code>Session</code>. Note that any such tokens
     * will represent open-scoped locks, since session-scoped locks do not have tokens.
     * 
     * @return an array of lock tokens (strings)
     * @throws RepositoryException if an error occurs.
     */
    public String[] getLockTokens() throws RepositoryException;

    /**
     * Returns <code>true</code> if the node at <code>absPath</code> holds a lock; otherwise returns <code>false</code>. To
     * <i>hold</i> a lock means that this node has actually had a lock placed on it specifically, as opposed to just having a lock
     * <i>apply</i> to it due to a deep lock held by a node above.
     * 
     * @param absPath absolute path of node
     * @return a <code>boolean</code>.
     * @throws PathNotFoundException if no node is found at <code>absPath</code>
     * @throws RepositoryException if an error occurs.
     */
    public boolean holdsLock( String absPath ) throws PathNotFoundException, RepositoryException;

    /**
     * <p>
     * Places a lock on the node at <code>absPath</code>. If successful, the node is said to <i>hold</i> the lock.
     * <p>
     * If <code>isDeep</code> is <code>true</code> then the lock applies to the specified node and all its descendant nodes; if
     * <code>false</code>, the lock applies only to the specified node. On a successful lock, the <code>jcr:lockIsDeep</code>
     * property of the locked node is set to this value.
     * <p>
     * If <code>isSessionScoped</code> is <code>true</code> then this lock will expire upon the expiration of the current session
     * (either through an automatic or explicit <code>Session.logout</code>); if false, this lock does not expire until it is
     * explicitly unlocked, it times out, or it is automatically unlocked due to a implementation-specific limitation.
     * <p>
     * The timeout parameter specifies the number of seconds until the lock times out (if it is not refreshed with
     * <code>Lock.refresh</code> in the meantime). An implementation may use this information as a hint or ignore it altogether.
     * Clients can discover the actual timeout by inspecting the returned <code>Lock</code> object.
     * <p>
     * The <code>ownerInfo</code> parameter can be used to pass a string holding owner information relevant to the client. An
     * implementation may either use or ignore this parameter. If it uses the parameter it must set the <code>jcr:lockOwner</code>
     * property of the locked node to this value and return this value on <code>Lock.getLockOwner</code>. If it ignores this
     * parameter the <code>jcr:lockOwner</code> property (and the value returned by <code>Lock.getLockOwner</code>) is set to
     * either the value returned by <code>Session.getUserID</code> of the owning session or an implementation-specific string
     * identifying the owner.
     * <p>
     * The method returns a <code>Lock</code> object representing the new lock. If the lock is open-scoped the returned lock will
     * include a lock token. The lock token is also automatically added to the set of lock tokens held by the current session.
     * <p>
     * The addition or change of the properties <code>jcr:lockIsDeep</code> and <code>jcr:lockOwner</code> are persisted
     * immediately; there is no need to call <code>save</code>.
     * <p>
     * It is possible to lock a node even if it is checked-in.
     * 
     * @param absPath absolute path of node to be locked
     * @param isDeep if <code>true</code> this lock will apply to this node and all its descendants; if <code>false</code>, it
     *        applies only to this node.
     * @param isSessionScoped if <code>true</code>, this lock expires with the current session; if <code>false</code> it expires
     *        when explicitly or automatically unlocked for some other reason.
     * @param timeoutHint desired lock timeout in seconds (servers are free to ignore this value); specify {@link Long#MAX_VALUE}
     *        for no timeout.
     * @param ownerInfo a string containing owner information supplied by the client; servers are free to ignore this value.
     * @return A <code>Lock</code> object containing a lock token.
     * @throws LockException if this node is not <code>mix:lockable</code> or this node is already locked or <code>isDeep</code>
     *         is <code>true</code> and a descendant node of this node already holds a lock.
     * @throws AccessDeniedException if this session does not have sufficent access to lock this node.
     * @throws InvalidItemStateException if this node has pending unsaved changes.
     * @throws PathNotFoundException if no node is found at <code>absPath</code>
     * @throws RepositoryException if another error occurs.
     */
    public Lock lock( String absPath,
                      boolean isDeep,
                      boolean isSessionScoped,
                      long timeoutHint,
                      String ownerInfo )
        throws LockException, PathNotFoundException, AccessDeniedException, InvalidItemStateException, RepositoryException;

    /**
     * Returns <code>true</code> if the node at <code>absPath</code> is locked either as a result of a lock held by that node or
     * by a deep lock on a node above that node; otherwise returns <code>false</code>.
     * 
     * @param absPath absolute path of node
     * @return a <code>boolean</code>.
     * @throws PathNotFoundException if no node is found at <code>absPath</code>
     * @throws RepositoryException if an error occurs.
     */
    public boolean isLocked( String absPath ) throws PathNotFoundException, RepositoryException;

    /**
     * Removes the specified lock token from this <code>Session</code>.
     * 
     * @param lockToken a lock token (a string)
     * @throws LockException if the current <code>Session</code> does not hold the specified lock token.
     * @throws RepositoryException if another error occurs.
     */
    public void removeLockToken( String lockToken ) throws LockException, RepositoryException;

    /**
     * Removes the lock on the node at <code>absPath</code>. Also removes the properties <code>jcr:lockOwner</code> and
     * <code>jcr:lockIsDeep</code> from that node. As well, the corresponding lock token is removed from the set of lock tokens
     * held by the current <code>Session</code>.
     * <p>
     * If the node does not currently hold a lock or holds a lock for which this <code>Session</code> is not the owner and is not
     * a "lock-superuser", then a <code>LockException</code> is thrown. Note that the system may give permission to a non-owning
     * session to unlock a lock. Typically, such "lock-superuser" capability is intended to facilitate administrational clean-up
     * of orphaned open-scoped locks.
     * <p>
     * Note that it is possible to unlock a node even if it is checked-in (the lock-related properties will be changed despite the
     * checked-in status).
     * <p>
     * If the current session does not have sufficient privileges to remove the lock, an <code>AccessDeniedException</code> is
     * thrown.
     * 
     * @param absPath absolute path of node to be unlocked
     * @throws LockException if this node does not currently hold a lock or holds a lock for which this Session does not have the
     *         correct lock token.
     * @throws AccessDeniedException if the current session does not have permission to unlock this node.
     * @throws InvalidItemStateException if this node has pending unsaved changes.
     * @throws PathNotFoundException if no node is found at <code>absPath</code>
     * @throws RepositoryException if another error occurs.
     */
    public void unlock( String absPath )
        throws PathNotFoundException, LockException, AccessDeniedException, InvalidItemStateException, RepositoryException;
}
