package org.jboss.dna.jcr;

import java.util.Collection;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
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

    private final ExecutionContext context;
    private final Path locksPath;
    private final JcrRepository repository;
    private final String workspaceName;
    private final ConcurrentMap<UUID, DnaLock> workspaceLocksByNodeUuid;

    WorkspaceLockManager( ExecutionContext context,
                          JcrRepository repository,
                          String workspaceName,
                          Path locksPath ) {
        this.context = context;
        this.repository = repository;
        this.workspaceName = workspaceName;
        this.locksPath = locksPath;

        this.workspaceLocksByNodeUuid = new ConcurrentHashMap<UUID, DnaLock>();

        Property locksPrimaryType = context.getPropertyFactory().create(JcrLexicon.PRIMARY_TYPE, DnaLexicon.LOCKS);
        repository.createSystemGraph().create(locksPath, locksPrimaryType).ifAbsent().and();
    }

    /**
     * Creates a lock on the node with the given {@link Location}. This method creates a new lock, registers it in the list of
     * locks, immediately modifies the {@code jcr:lockOwner} and {@code jcr:lockIsDeep} properties on the node in the underlying
     * repository, and adds the lock to the system view.
     * <p>
     * The location given in {@code nodeLocation} must have a UUID.
     * </p>
     * 
     * @param cache the session cache from which the node was loaded
     * @param nodeLocation the location for the node; may not be null and must have a UUID
     * @param lockOwner the owner of the new lock
     * @param isDeep whether the node's descendants in the content graph should also be locked
     * @param isSessionScoped whether the lock should outlive the session in which it was created
     * @return an object representing the newly created lock
     * @throws RepositoryException if an error occurs updating the graph state
     */
    DnaLock lock( SessionCache cache,
                  Location nodeLocation,
                  String lockOwner,
                  boolean isDeep,
                  boolean isSessionScoped ) throws RepositoryException {
        assert nodeLocation != null;
        
        UUID lockUuid = UUID.randomUUID();
        UUID nodeUuid = uuidFor(nodeLocation);

        if (nodeUuid == null) {
            throw new RepositoryException(JcrI18n.uuidRequiredForLock.text(nodeLocation));
        }
        
        DnaLock lock = new DnaLock(lockOwner, lockUuid, nodeUuid, isDeep, isSessionScoped);

        Graph.Batch batch = repository.createSystemGraph().batch();

        PropertyFactory propFactory = context.getPropertyFactory();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();
        Property lockOwnerProp = propFactory.create(JcrLexicon.LOCK_OWNER, lockOwner);
        Property lockIsDeepProp = propFactory.create(JcrLexicon.LOCK_IS_DEEP, isDeep);

        batch.create(pathFactory.create(locksPath, pathFactory.createSegment(lockUuid.toString())),
                     propFactory.create(JcrLexicon.PRIMARY_TYPE, DnaLexicon.LOCK),
                     propFactory.create(JcrLexicon.UUID, nodeUuid.toString()),
                     propFactory.create(DnaLexicon.WORKSPACE, workspaceName),
                     propFactory.create(DnaLexicon.LOCKED_NODE, nodeUuid.toString()),
                     propFactory.create(DnaLexicon.IS_SESSION_SCOPED, isSessionScoped),
                  // This gets set after the lock succeeds and the lock token gets added to the session
                     propFactory.create(DnaLexicon.IS_HELD_BY_SESSION, false),  
                     lockOwnerProp,
                     lockIsDeepProp).ifAbsent().and();
        batch.execute();

        AbstractJcrNode lockedNode = cache.findJcrNode(Location.create(nodeUuid));
        NodeEditor editor = cache.getEditorFor(lockedNode.nodeInfo());

        // Set the properties in the cache...
        editor.setProperty(JcrLexicon.LOCK_OWNER,
                           (JcrValue)cache.session().getValueFactory().createValue(lockOwner, PropertyType.STRING),
                           false);
        editor.setProperty(JcrLexicon.LOCK_IS_DEEP, (JcrValue)cache.session().getValueFactory().createValue(isDeep), false);

        // Write them directly to the underlying graph
        repository.createWorkspaceGraph(workspaceName).set(lockOwnerProp, lockIsDeepProp).on(nodeUuid);

        workspaceLocksByNodeUuid.put(nodeUuid, lock);

        return lock;
    }

    /**
     * Removes the provided lock, effectively unlocking the node to which the lock is associated.
     * 
     * @param lock the lock to be removed
     */
    void unlock( DnaLock lock ) {
        try {
            PathFactory pathFactory = context.getValueFactories().getPathFactory();

            Graph.Batch batch = repository.createSystemGraph().batch();

            batch.delete(pathFactory.create(locksPath, pathFactory.createSegment(lock.lockUuid.toString())));
            batch.remove(JcrLexicon.LOCK_OWNER, JcrLexicon.LOCK_IS_DEEP).on(lock.nodeUuid);
            batch.execute();
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

    boolean isHeldBySession( String lockToken ) {
        ValueFactory<Boolean> booleanFactory = context.getValueFactories().getBooleanFactory();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();

        org.jboss.dna.graph.Node lockNode = repository.createSystemGraph().getNodeAt(pathFactory.create(locksPath,
                                                                                        pathFactory.createSegment(lockToken)));

        return booleanFactory.create(lockNode.getProperty(DnaLexicon.IS_HELD_BY_SESSION).getFirstValue());
        
    }
    
    void setHeldBySession( String lockToken,
                           boolean value ) {
        PropertyFactory propFactory = context.getPropertyFactory();
        PathFactory pathFactory = context.getValueFactories().getPathFactory();

        repository.createSystemGraph().set(propFactory.create(DnaLexicon.IS_HELD_BY_SESSION, value)).on(pathFactory.create(locksPath,
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
     * @param nodeUuid the node UUID
     * @return the corresponding lock, possibly null
     */
    DnaLock lockFor( Location nodeLocation ) throws RepositoryException {
        UUID nodeUuid = uuidFor(nodeLocation);
        if (nodeUuid == null) return null;
        return workspaceLocksByNodeUuid.get(nodeUuid);
    }
    

    UUID uuidFor( Location location ) throws RepositoryException {
        if (location.getUuid() != null) return location.getUuid();

        org.jboss.dna.graph.property.Property uuidProp = location.getIdProperty(JcrLexicon.UUID);
        if (uuidProp == null) return null;

        return context.getValueFactories().getUuidFactory().create(uuidProp.getFirstValue());
    }

    /**
     * Unlocks all locks corresponding to the tokens in the {@code lockTokens} collection that are session scoped.
     * 
     * @param lockTokens the collection of lock tokens
     */
    void cleanLocks( Collection<String> lockTokens ) {
        for (String lockToken : lockTokens) {
            DnaLock lock = lockFor(lockToken);
            if (lock != null && lock.isSessionScoped()) {
                unlock(lock);
            }
        }
    }

    /**
     * Internal representation of a locked node. This class should only be created through calls to
     * {@link WorkspaceLockManager#lock(SessionCache, Location, String, boolean, boolean)}.
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

        public boolean isLive() {
            return workspaceLocksByNodeUuid.containsKey(nodeUuid);
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

        public Lock lockFor( SessionCache cache ) throws RepositoryException {
            final AbstractJcrNode node = cache.findJcrNode(Location.create(nodeUuid));
            final JcrSession session = cache.session();
            return new Lock() {
                @Override
                public String getLockOwner() {
                    return lockOwner;
                }

                @Override
                public String getLockToken() {
                    String uuidString = lockUuid.toString();
                    return session.lockTokens().contains(uuidString) ? uuidString : null;
                }

                @Override
                public Node getNode() {
                    return node;
                }

                @Override
                public boolean isDeep() {
                    return deep;
                }

                @Override
                public boolean isLive() throws RepositoryException {
                    return workspaceLocksByNodeUuid.containsKey(nodeUuid);
                }

                @Override
                public boolean isSessionScoped() {
                    return sessionScoped;
                }

                @Override
                public void refresh() throws LockException, RepositoryException {
                    if (getLockToken() == null) {
                        throw new LockException(JcrI18n.notLocked.text(node.location));
                    }
                }
            };
        }

    }
}
