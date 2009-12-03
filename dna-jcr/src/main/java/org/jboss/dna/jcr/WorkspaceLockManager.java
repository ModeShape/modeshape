package org.jboss.dna.jcr;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.jcr.Item;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import net.jcip.annotations.ThreadSafe;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.connector.LockFailedException;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.PathNotFoundException;
import org.jboss.dna.graph.property.Property;
import org.jboss.dna.graph.property.PropertyFactory;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.jcr.SessionCache.NodeEditor;

/**
 * Manages the locks for a particular workspace in a repository. Locks are stored in a {@code Map<UUID, DnaLock>} while they exist
 * and are discarded after they are discarded (through the {@link Node#unlock()} method).
 */
@ThreadSafe
class WorkspaceLockManager {

    private final Path locksPath;
    private final JcrRepository repository;
    private final String workspaceName;
    private final ConcurrentMap<UUID, DnaLock> workspaceLocksByNodeUuid;

    WorkspaceLockManager( ExecutionContext context,
                          JcrRepository repository,
                          String workspaceName,
                          Path locksPath ) {
        this.repository = repository;
        this.workspaceName = workspaceName;
        this.locksPath = locksPath;

        this.workspaceLocksByNodeUuid = new ConcurrentHashMap<UUID, DnaLock>();

        Property locksPrimaryType = context.getPropertyFactory().create(JcrLexicon.PRIMARY_TYPE, DnaLexicon.LOCKS);
        repository.createSystemGraph(context).create(locksPath, locksPrimaryType).ifAbsent().and();
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
    DnaLock lock( JcrSession session,
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
        String lockOwner = sessionContext.getSecurityContext().getUserName();
        DnaLock lock = createLock(lockOwner, lockUuid, nodeUuid, isDeep, isSessionScoped);

        Graph.Batch batch = repository.createSystemGraph(sessionContext).batch();

        PropertyFactory propFactory = sessionContext.getPropertyFactory();
        PathFactory pathFactory = sessionContext.getValueFactories().getPathFactory();
        Property lockOwnerProp = propFactory.create(JcrLexicon.LOCK_OWNER, lockOwner);
        Property lockIsDeepProp = propFactory.create(JcrLexicon.LOCK_IS_DEEP, isDeep);

        batch.create(pathFactory.create(locksPath, pathFactory.createSegment(lockUuid.toString())),
                     propFactory.create(JcrLexicon.PRIMARY_TYPE, DnaLexicon.LOCK),
                     propFactory.create(DnaLexicon.WORKSPACE, workspaceName),
                     propFactory.create(DnaLexicon.LOCKED_UUID, nodeUuid.toString()),
                     propFactory.create(DnaLexicon.IS_SESSION_SCOPED, isSessionScoped),
                     // This gets set after the lock succeeds and the lock token gets added to the session
                     propFactory.create(DnaLexicon.IS_HELD_BY_SESSION, false),
                     lockOwnerProp,
                     lockIsDeepProp).ifAbsent().and();
        batch.execute();

        SessionCache cache = session.cache();
        AbstractJcrNode lockedNode = cache.findJcrNode(Location.create(nodeUuid));
        NodeEditor editor = cache.getEditorFor(lockedNode.nodeInfo());

        // Set the properties in the cache...
        editor.setProperty(JcrLexicon.LOCK_OWNER,
                           (JcrValue)cache.session().getValueFactory().createValue(lockOwner, PropertyType.STRING),
                           false);
        editor.setProperty(JcrLexicon.LOCK_IS_DEEP, (JcrValue)cache.session().getValueFactory().createValue(isDeep), false);

        lockNodeInRepository(session, nodeUuid, lockOwnerProp, lockIsDeepProp, lock, isDeep);
        workspaceLocksByNodeUuid.put(nodeUuid, lock);

        return lock;
    }

    /* Factory method added to facilitate mocked testing */
    DnaLock createLock( String lockOwner,
                        UUID lockUuid,
                        UUID nodeUuid,
                        boolean isDeep,
                        boolean isSessionScoped ) {
        return new DnaLock(lockOwner, lockUuid, nodeUuid, isDeep, isSessionScoped);
    }

    /**
     * Marks the node as locked in the underlying repository with an immediate write (that is, the write is not part of the JCR
     * session scope and cannot be "rolled back" with a refresh at the {@link Session#refresh(boolean) session} or
     * {@link Item#refresh(boolean) item} level.
     * <p>
     * This method will also attempt to {@link Graph#lock(Location) lock the node in the underlying repository}. If the underlying
     * repository supports locks and {@link LockFailedException the lock attempt fails}, this method will cancel the lock attempt
     * by calling {@link #unlock(JcrSession,DnaLock)} and will throw a {@code RepositoryException}.
     * </p>
     * <p>
     * This method does not modify the system graph. In other words, it will not create the record for the lock in the {@code
     * /jcr:system/dna:locks} subgraph.
     * </p>
     * 
     * @param session the session in which the node is being locked and that loaded the node
     * @param nodeUuid the UUID of the node to lock
     * @param lockOwnerProp an existing property with name {@link JcrLexicon#LOCK_OWNER} and the value being the name of the lock
     *        owner
     * @param lockIsDeepProp an existing property with name {@link JcrLexicon#LOCK_IS_DEEP} and the value being {@code true} if
     *        the lock should include all descendants of the locked node or {@code false} if the lock should only include the
     *        specified node and not its descendants
     * @param lock the internal lock representation
     * @param isDeep {@code true} if the lock should include all descendants of the locked node or {@code false} if the lock
     *        should only include the specified node and not its descendants. This value is redundant with the lockIsDeep
     *        parameter, but is included separately as a minor performance optimization
     * @throws RepositoryException if the repository in which the node represented by {@code nodeUuid} supports locking but
     *         signals that the lock for the node cannot be acquired
     */
    void lockNodeInRepository( JcrSession session,
                               UUID nodeUuid,
                               Property lockOwnerProp,
                               Property lockIsDeepProp,
                               DnaLock lock,
                               boolean isDeep ) throws RepositoryException {
        // Write them directly to the underlying graph
        Graph.Batch workspaceBatch = repository.createWorkspaceGraph(workspaceName, session.getExecutionContext()).batch();
        workspaceBatch.set(lockOwnerProp, lockIsDeepProp).on(nodeUuid);
        if (isDeep) {
            workspaceBatch.lock(nodeUuid).andItsDescendants().withDefaultTimeout();
        } else {
            workspaceBatch.lock(nodeUuid).only().withDefaultTimeout();
        }
        try {
            workspaceBatch.execute();
        } catch (LockFailedException lfe) {
            // Attempt to lock node at the repo level failed - cancel lock
            unlock(session, lock);
            throw new RepositoryException(lfe);
        }

    }

    /**
     * Removes the provided lock, effectively unlocking the node to which the lock is associated.
     * 
     * @param session the session in which the node is being unlocked
     * @param lock the lock to be removed
     */
    void unlock( JcrSession session,
                 DnaLock lock ) {
        try {
            ExecutionContext context = session.getExecutionContext();
            PathFactory pathFactory = context.getValueFactories().getPathFactory();

            // Remove the lock node under the /jcr:system branch ...
            Graph.Batch batch = repository.createSystemGraph(context).batch();
            batch.delete(pathFactory.create(locksPath, pathFactory.createSegment(lock.getUuid().toString())));
            batch.execute();

            // Unlock the node in the repository graph ...
            unlockNodeInRepository(session, lock);

            workspaceLocksByNodeUuid.remove(lock.nodeUuid);
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
     * @param session the session in which the node is being unlocked
     * @param lock
     */
    void unlockNodeInRepository( JcrSession session,
                                 DnaLock lock ) {
        Graph.Batch workspaceBatch = repository.createWorkspaceGraph(this.workspaceName, session.getExecutionContext()).batch();

        workspaceBatch.remove(JcrLexicon.LOCK_OWNER, JcrLexicon.LOCK_IS_DEEP).on(lock.nodeUuid);
        workspaceBatch.unlock(lock.nodeUuid);

        workspaceBatch.execute();
    }

    /**
     * Checks whether the given lock token is currently held by any session by querying the lock record in the underlying
     * repository.
     * 
     * @param session the session on behalf of which the lock query is being performed
     * @param lockToken the lock token to check; may not be null
     * @return true if a session currently holds the lock token, false otherwise
     */
    boolean isHeldBySession( JcrSession session,
                             String lockToken ) {
        assert lockToken != null;

        ExecutionContext context = session.getExecutionContext();
        ValueFactory<Boolean> booleanFactory = context.getValueFactories().getBooleanFactory();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();

        org.jboss.dna.graph.Node lockNode = repository.createSystemGraph(context)
                                                      .getNodeAt(pathFactory.create(locksPath,
                                                                                    pathFactory.createSegment(lockToken)));

        return booleanFactory.create(lockNode.getProperty(DnaLexicon.IS_HELD_BY_SESSION).getFirstValue());

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

        repository.createSystemGraph(context)
                  .set(propFactory.create(DnaLexicon.IS_HELD_BY_SESSION, value))
                  .on(pathFactory.create(locksPath, pathFactory.createSegment(lockToken)));
    }

    /**
     * Returns the lock that corresponds to the given lock token
     * 
     * @param lockToken the lock token
     * @return the corresponding lock, possibly null
     * @see Session#addLockToken(String)
     * @see Session#removeLockToken(String)
     */
    DnaLock lockFor( String lockToken ) {
        for (DnaLock lock : workspaceLocksByNodeUuid.values()) {
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
    DnaLock lockFor( JcrSession session,
                     Location nodeLocation ) {
        UUID nodeUuid = uuidFor(session, nodeLocation);
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

        org.jboss.dna.graph.property.Property uuidProp = location.getIdProperty(JcrLexicon.UUID);
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
        Collection<String> lockTokens = session.lockTokens();
        for (String lockToken : lockTokens) {
            DnaLock lock = lockFor(lockToken);
            if (lock != null && lock.isSessionScoped()) {
                unlock(session, lock);
            }
        }
    }

    /**
     * Internal representation of a locked node. This class should only be created through calls to
     * {@link WorkspaceLockManager#lock(JcrSession, Location, boolean, boolean)}.
     */
    @ThreadSafe
    public class DnaLock {
        final UUID nodeUuid;
        private final UUID lockUuid;
        private final String lockOwner;
        private final boolean deep;
        private final boolean sessionScoped;

        DnaLock( String lockOwner,
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
                    String uuidString = lockUuid.toString();
                    return session.lockTokens().contains(uuidString) ? uuidString : null;
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
                    if (getLockToken() == null) {
                        throw new LockException(JcrI18n.notLocked.text(node.location));
                    }
                }
            };
        }

    }
}
