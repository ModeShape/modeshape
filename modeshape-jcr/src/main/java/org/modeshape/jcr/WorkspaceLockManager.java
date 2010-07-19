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

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.i18n.I18n;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.connector.LockFailedException;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.DateTimeFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.PropertyFactory;
import org.modeshape.graph.property.ValueFactory;

/**
 * Manages the locks for a particular workspace in a repository. Locks are stored in a {@code Map<UUID, DnaLock>} while they exist
 * and are discarded after they are discarded (through the {@link Node#unlock()} method).
 */
@ThreadSafe
class WorkspaceLockManager {

    private final ExecutionContext context;
    private final Path locksPath;
    private final RepositoryLockManager repositoryLockManager;
    private final String workspaceName;
    private final ConcurrentMap<UUID, ModeShapeLock> workspaceLocksByNodeUuid;

    WorkspaceLockManager( ExecutionContext context,
                          RepositoryLockManager repositoryLockManager,
                          String workspaceName,
                          Path locksPath ) {
        this.context = context;
        this.repositoryLockManager = repositoryLockManager;
        this.workspaceName = workspaceName;
        this.locksPath = locksPath;

        this.workspaceLocksByNodeUuid = new ConcurrentHashMap<UUID, ModeShapeLock>();
    }

    /**
     * Creates a lock on the node with the given {@link Location}. This method creates a new lock, registers it in the list of
     * locks, immediately modifies the {@code jcr:lockOwner} and {@code jcr:lockIsDeep} properties on the node in the underlying
     * repository, and adds the lock to the system view.
     * <p>
     * The location given in {@code nodeLocation} must have a UUID.
     * </p>
     * 
     * @param session the session in which the node is being locked and that loaded the node
     * @param nodeLocation the location for the node; may not be null and must have a UUID
     * @param isDeep whether the node's descendants in the content graph should also be locked
     * @param isSessionScoped whether the lock should outlive the session in which it was created
     * @return an object representing the newly created lock
     * @throws RepositoryException if an error occurs updating the graph state
     */
    ModeShapeLock lock( JcrSession session,
                        Location nodeLocation,
                        boolean isDeep,
                        boolean isSessionScoped ) throws RepositoryException {
        assert nodeLocation != null;

        UUID lockUuid = UUID.randomUUID();
        UUID nodeUuid = uuidFor(session, nodeLocation);

        if (nodeUuid == null) {
            throw new RepositoryException(JcrI18n.uuidRequiredForLock.text(nodeLocation));
        }

        ExecutionContext sessionContext = session.getExecutionContext();
        String lockOwner = session.getUserID();

        DateTimeFactory dateFactory = sessionContext.getValueFactories().getDateFactory();
        DateTime expirationDate = dateFactory.create();
        expirationDate = expirationDate.plusMillis(JcrEngine.LOCK_EXTENSION_INTERVAL_IN_MILLIS);

        lockNodeInSystemGraph(sessionContext,
                              nodeUuid,
                              lockUuid,
                              workspaceName,
                              isSessionScoped,
                              isDeep,
                              session.sessionId(),
                              session.getUserID(),
                              expirationDate);

        try {
            lockNodeInRepository(session, nodeUuid, session.getUserID(), isDeep);
        } catch (LockFailedException lfe) {
            // Attempt to lock node at the repo level failed - cancel lock
            unlockNodeInSystemGraph(session.getExecutionContext(), nodeUuid);
            throw new RepositoryException(lfe);
        }

        ModeShapeLock lock = lockNodeInternally(lockOwner, lockUuid, nodeUuid, isDeep, isSessionScoped);

        session.cache().refreshProperties(Location.create(nodeUuid));

        return lock;
    }

    ModeShapeLock lockNodeInternally( String lockOwner,
                             UUID lockUuid,
                             UUID lockedNodeUuid,
                             boolean isDeep,
                             boolean isSessionScoped ) {
        ModeShapeLock lock = createLock(lockOwner, lockUuid, lockedNodeUuid, isDeep, isSessionScoped);

        workspaceLocksByNodeUuid.put(lockedNodeUuid, lock);

        return lock;
    }

    ModeShapeLock createLock( org.modeshape.graph.Node lockNode ) {
        return new ModeShapeLock(lockNode);
    }

    /* Factory method added to facilitate mocked testing */
    ModeShapeLock createLock( String lockOwner,
                              UUID lockUuid,
                              UUID nodeUuid,
                              boolean isDeep,
                              boolean isSessionScoped ) {
        return new ModeShapeLock(lockOwner, lockUuid, nodeUuid, isDeep, isSessionScoped);
    }

    /**
     * Marks the node as locked in the underlying repository with an immediate write (that is, the write is not part of the JCR
     * session scope and cannot be "rolled back" with a refresh at the {@link Session#refresh(boolean) session} or
     * {@link Item#refresh(boolean) item} level.
     * <p>
     * This method will also attempt to {@link Graph#lock(Location) lock the node in the underlying repository}. If the underlying
     * repository supports locks and {@link LockFailedException the lock attempt fails}, this method will cancel the lock attempt
     * by calling {@link #unlock(ExecutionContext, ModeShapeLock)} and will throw a {@code RepositoryException}.
     * </p>
     * <p>
     * This method does not modify the system graph. In other words, it will not create the record for the lock in the {@code
     * /jcr:system/dna:locks} subgraph.
     * </p>
     * 
     * @param session the session in which the node is being locked and that loaded the node
     * @param nodeUuid the UUID of the node to lock
     * @param lockOwner a string containing the lock owner's name
     * @param isDeep {@code true} if the lock should include all descendants of the locked node or {@code false} if the lock
     *        should only include the specified node and not its descendants. This value is redundant with the lockIsDeep
     *        parameter, but is included separately as a minor performance optimization
     * @throws LockFailedException if the repository supports locking nodes, but could not lock this particular node
     * @throws RepositoryException if the repository in which the node represented by {@code nodeUuid} supports locking but
     *         signals that the lock for the node cannot be acquired
     */
    void lockNodeInRepository( JcrSession session,
                               UUID nodeUuid,
                               String lockOwner,
                               boolean isDeep ) throws LockFailedException, RepositoryException {
        PropertyFactory propFactory = session.getExecutionContext().getPropertyFactory();
        Property lockOwnerProp = propFactory.create(JcrLexicon.LOCK_OWNER, lockOwner);
        Property lockIsDeepProp = propFactory.create(JcrLexicon.LOCK_IS_DEEP, isDeep);

        // Write them directly to the underlying graph
        Graph.Batch workspaceBatch = repositoryLockManager.createWorkspaceGraph(workspaceName, session.getExecutionContext()).batch();
        workspaceBatch.set(lockOwnerProp, lockIsDeepProp).on(nodeUuid);
        if (isDeep) {
            workspaceBatch.lock(nodeUuid).andItsDescendants().withDefaultTimeout();
        } else {
            workspaceBatch.lock(nodeUuid).only().withDefaultTimeout();
        }
            workspaceBatch.execute();

    }

    private final void lockNodeInSystemGraph( ExecutionContext sessionContext,
                                              UUID nodeUuid,
                                              UUID lockUuid,
                                              String workspaceName,
                                              boolean isSessionScoped,
                                              boolean lockIsDeep,
                                              String sessionId,
                                              String lockOwner,
                                              DateTime expirationDate ) {
        PathFactory pathFactory = sessionContext.getValueFactories().getPathFactory();
        PropertyFactory propFactory = sessionContext.getPropertyFactory();

        Graph graph = repositoryLockManager.createSystemGraph(sessionContext);
        graph.create(pathFactory.create(locksPath, pathFactory.createSegment(lockUuid.toString())),
                     propFactory.create(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.LOCK),
                     propFactory.create(ModeShapeLexicon.WORKSPACE, workspaceName),
                     propFactory.create(ModeShapeLexicon.LOCKED_UUID, nodeUuid.toString()),
                     propFactory.create(ModeShapeLexicon.IS_SESSION_SCOPED, isSessionScoped),
                     propFactory.create(ModeShapeLexicon.LOCKING_SESSION, sessionId),
                     propFactory.create(ModeShapeLexicon.EXPIRATION_DATE, expirationDate),
                     // This gets set after the lock succeeds and the lock token gets added to the session
                     propFactory.create(ModeShapeLexicon.IS_HELD_BY_SESSION, false),
                     propFactory.create(JcrLexicon.LOCK_OWNER, lockOwner),
                     propFactory.create(JcrLexicon.LOCK_IS_DEEP, lockIsDeep)).ifAbsent().and();

    }

    /**
     * Removes the provided lock, effectively unlocking the node to which the lock is associated.
     * 
     * @param sessionExecutionContext the execution context of the session in which the node is being unlocked
     * @param lock the lock to be removed
     */
    void unlock( ExecutionContext sessionExecutionContext,
                 ModeShapeLock lock ) {
        try {
            // Remove the lock node under the /jcr:system branch ...
            unlockNodeInSystemGraph(sessionExecutionContext, lock.getUuid());

            // Unlock the node in the repository graph ...
            unlockNodeInRepository(sessionExecutionContext, lock);

            unlockNodeInternally(lock.nodeUuid);
        } catch (PathNotFoundException pnfe) {
            /*
             * This can legitimately happen if there is a session-scoped lock on a node which is then deleted by the lock owner and the lock
             * owner logs out.  At that point, the lock owner still holds the token for the lock, so it will get cleaned up with a call to cleanLocks.
             */
            if (!lock.nodeUuid.equals(pnfe.getLocation().getUuid())) {
                // If the lock node under dna:locks is not found, this is an internal error
                throw new IllegalStateException(pnfe);
            }
            workspaceLocksByNodeUuid.remove(lock.nodeUuid);
        }
    }

    /**
     * Removes the workspace record of the lock in the underlying repository. This method clears the {@code jcr:lockOwner} and
     * {@code jcr:lockIsDeep} properties on the node and sends an {@link Graph#unlock(Location) unlock request} to the underlying
     * repository to clear any locks that it is holding on the node.
     * <p>
     * This method does not modify the system graph. In other words, it will not remove the record for the lock in the {@code
     * /jcr:system/dna:locks} subgraph.
     * </p>
     * 
     * @param sessionExecutionContext the execution context of the session in which the node is being unlocked
     * @param lock
     */
    void unlockNodeInRepository( ExecutionContext sessionExecutionContext,
                                 ModeShapeLock lock ) {
        Graph.Batch workspaceBatch = repositoryLockManager.createWorkspaceGraph(this.workspaceName, sessionExecutionContext).batch();

        workspaceBatch.remove(JcrLexicon.LOCK_OWNER, JcrLexicon.LOCK_IS_DEEP).on(lock.nodeUuid);
        workspaceBatch.unlock(lock.nodeUuid);

        workspaceBatch.execute();
    }

    void unlockNodeInSystemGraph( ExecutionContext sessionExecutionContext,
                                  UUID lockUuid ) {
        PathFactory pathFactory = sessionExecutionContext.getValueFactories().getPathFactory();
        Graph systemGraph = repositoryLockManager.createSystemGraph(sessionExecutionContext);
        systemGraph.delete(pathFactory.create(locksPath, pathFactory.createSegment(lockUuid.toString())));

    }

    boolean unlockNodeInternally( UUID lockedNodeUuid ) {
        return workspaceLocksByNodeUuid.remove(lockedNodeUuid) != null;
    }

    /**
     * Checks whether the given lock token is currently held by any session by querying the lock record in the underlying
     * repository.
     * 
     * @param session the session on behalf of which the lock query is being performed
     * @param lockToken the lock token to check; may not be null
     * @return true if a session currently holds the lock token, false otherwise
     * @throws LockException if the lock token doesn't exist
     */
    boolean isHeldBySession( JcrSession session,
                             String lockToken ) throws LockException {
        assert lockToken != null;

        ExecutionContext context = session.getExecutionContext();
        ValueFactory<Boolean> booleanFactory = context.getValueFactories().getBooleanFactory();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();

        try {
            org.modeshape.graph.Node lockNode = repositoryLockManager.createSystemGraph(context).getNodeAt(pathFactory.create(locksPath,
                                                                                                                              pathFactory.createSegment(lockToken)));

            return booleanFactory.create(lockNode.getProperty(ModeShapeLexicon.IS_HELD_BY_SESSION).getFirstValue());
        } catch (PathNotFoundException pnfe) {
            I18n msg = JcrI18n.invalidLockToken;
            throw new LockException(msg.text(lockToken));
        }

    }

    /**
     * Updates the underlying repository directly (i.e., outside the scope of the {@link Session}) to mark the token for the given
     * lock as being held (or not held) by some {@link Session}. Note that this method does not identify <i>which</i> (if any)
     * session holds the token for the lock, just that <i>some</i> session holds the token for the lock.
     * 
     * @param session the session on behalf of which the lock operation is being performed
     * @param lockToken the lock token for which the "held" status should be modified; may not be null
     * @param value the new value
     */
    void setHeldBySession( JcrSession session,
                           String lockToken,
                           boolean value ) {
        assert lockToken != null;

        ExecutionContext context = session.getExecutionContext();
        PropertyFactory propFactory = context.getPropertyFactory();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();

        repositoryLockManager.createSystemGraph(context).set(propFactory.create(ModeShapeLexicon.IS_HELD_BY_SESSION, value)).on(pathFactory.create(locksPath,
                                                                                                                                                   pathFactory.createSegment(lockToken)));
    }

    /**
     * Returns the lock that corresponds to the given lock token
     * 
     * @param lockToken the lock token
     * @return the corresponding lock, possibly null
     * @see Session#addLockToken(String)
     * @see Session#removeLockToken(String)
     */
    ModeShapeLock lockFor( String lockToken ) {
        for (ModeShapeLock lock : workspaceLocksByNodeUuid.values()) {
            if (lockToken.equals(lock.getLockToken())) {
                return lock;
            }
        }

        return null;
    }

    /**
     * Returns the lock that corresponds to the given UUID
     * 
     * @param session the session on behalf of which the lock operation is being performed
     * @param nodeLocation the node UUID
     * @return the corresponding lock, possibly null if there is no such lock
     */
    ModeShapeLock lockFor( JcrSession session,
                           Location nodeLocation ) {
        UUID nodeUuid = uuidFor(session, nodeLocation);
        return lockFor(nodeUuid);
    }

    /**
     * Returns the lock that corresponds to the given UUID
     * 
     * @param nodeUuid the node UUID
     * @return the corresponding lock, possibly null if there is no such lock
     */
    ModeShapeLock lockFor( UUID nodeUuid ) {
        if (nodeUuid == null) return null;
        return workspaceLocksByNodeUuid.get(nodeUuid);
    }

    /**
     * Returns the UUID that identifies the given location or {@code null} if the location does not have a UUID. The method
     * returns the {@link Location#getUuid() default UUID} if it exists. If it does not, the method returns the value of the
     * {@link JcrLexicon#UUID} property as a UUID. If the location does not contain that property, the method returns null.
     * 
     * @param session the session on behalf of which the lock operation is being performed
     * @param location the location for which the UUID should be returned
     * @return the UUID that identifies the given location or {@code null} if the location does not have a UUID.
     */
    UUID uuidFor( JcrSession session,
                  Location location ) {
        assert location != null;

        if (location.getUuid() != null) return location.getUuid();

        org.modeshape.graph.property.Property uuidProp = location.getIdProperty(JcrLexicon.UUID);
        if (uuidProp == null) return null;

        ExecutionContext context = session.getExecutionContext();
        return context.getValueFactories().getUuidFactory().create(uuidProp.getFirstValue());
    }

    /**
     * Unlocks all locks corresponding to the tokens in the {@code lockTokens} collection that are session scoped.
     * 
     * @param session the session on behalf of which the lock operation is being performed
     */
    void cleanLocks( JcrSession session ) {
        ExecutionContext context = session.getExecutionContext();
        Collection<String> lockTokens = session.lockManager().lockTokens();
        for (String lockToken : lockTokens) {
            ModeShapeLock lock = lockFor(lockToken);
            if (lock != null && lock.isSessionScoped()) {
                unlock(context, lock);
            }
        }
    }

    /**
     * Internal representation of a locked node. This class should only be created through calls to
     * {@link WorkspaceLockManager#lock(JcrSession, Location, boolean, boolean)}.
     */
    @ThreadSafe
    public class ModeShapeLock {
        final UUID nodeUuid;
        private final UUID lockUuid;
        private final String lockOwner;
        private final boolean deep;
        private final boolean sessionScoped;

        @SuppressWarnings( "synthetic-access" )
        ModeShapeLock( org.modeshape.graph.Node lockNode ) {
            ValueFactory<String> stringFactory = context.getValueFactories().getStringFactory();
            ValueFactory<UUID> uuidFactory = context.getValueFactories().getUuidFactory();
            ValueFactory<Boolean> booleanFactory = context.getValueFactories().getBooleanFactory();

            assert lockNode.getLocation().getPath() != null;

            String lockUuidAsString = lockNode.getLocation().getPath().getLastSegment().getName().getLocalName();
            Property lockOwnerProperty = lockNode.getProperty(JcrLexicon.LOCK_OWNER);
            Property nodeUuidProperty = lockNode.getProperty(ModeShapeLexicon.LOCKED_UUID);
            Property lockIsDeepProperty = lockNode.getProperty(JcrLexicon.LOCK_IS_DEEP);
            Property isSessionScopedProperty = lockNode.getProperty(ModeShapeLexicon.IS_SESSION_SCOPED);

            assert lockUuidAsString != null;
            assert lockOwnerProperty != null;
            assert nodeUuidProperty != null;
            assert lockIsDeepProperty != null;
            assert isSessionScopedProperty != null;

            this.lockOwner = stringFactory.create(lockOwnerProperty.getFirstValue());
            this.lockUuid = UUID.fromString(lockUuidAsString);
            this.nodeUuid = uuidFactory.create(nodeUuidProperty.getFirstValue());
            this.deep = booleanFactory.create(lockIsDeepProperty.getFirstValue());
            this.sessionScoped = booleanFactory.create(isSessionScopedProperty.getFirstValue());
        }

        ModeShapeLock( String lockOwner,
                       UUID lockUuid,
                       UUID nodeUuid,
                       boolean deep,
                       boolean sessionScoped ) {
            super();
            this.lockOwner = lockOwner;
            this.lockUuid = lockUuid;
            this.nodeUuid = nodeUuid;
            this.deep = deep;
            this.sessionScoped = sessionScoped;
        }

        @SuppressWarnings( "synthetic-access" )
        public boolean isLive() {
            return workspaceLocksByNodeUuid.containsKey(nodeUuid);
        }

        public UUID getUuid() {
            return lockUuid;
        }

        public boolean isDeep() {
            return deep;
        }

        public String getLockOwner() {
            return lockOwner;
        }

        public boolean isSessionScoped() {
            return sessionScoped;
        }

        public String getLockToken() {
            return lockUuid.toString();
        }

        @SuppressWarnings( "synthetic-access" )
        public Lock lockFor( SessionCache cache ) throws RepositoryException {
            final AbstractJcrNode node = cache.findJcrNode(Location.create(nodeUuid));
            final JcrSession session = cache.session();
            return new Lock() {
                public String getLockOwner() {
                    return lockOwner;
                }

                public String getLockToken() {
                    if (sessionScoped) return null;
                    String uuidString = lockUuid.toString();
                    return session.lockManager().lockTokens().contains(uuidString) ? uuidString : null;
                }

                public Node getNode() {
                    return node;
                }

                public boolean isDeep() {
                    return deep;
                }

                public boolean isLive() {
                    return workspaceLocksByNodeUuid.containsKey(nodeUuid);
                }

                public boolean isSessionScoped() {
                    return sessionScoped;
                }

                public void refresh() throws LockException {
                    String uuidString = lockUuid.toString();
                    if (!session.lockManager().lockTokens().contains(uuidString)) {
                        throw new LockException(JcrI18n.notLocked.text(node.location));
                    }
                }

                @Override
                public long getSecondsRemaining() {
                    return isLockOwningSession() ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                }

                @Override
                public boolean isLockOwningSession() {
                    String uuidString = lockUuid.toString();
                    return session.lockManager().lockTokens().contains(uuidString);
                }

            };
        }
    }
}
