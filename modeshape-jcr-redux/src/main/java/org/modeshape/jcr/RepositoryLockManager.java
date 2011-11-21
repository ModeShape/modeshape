package org.modeshape.jcr;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.lock.Lock;
import javax.jcr.lock.LockException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.LockFailureException;
import org.modeshape.jcr.cache.MutableCachedNode;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.change.Change;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.cache.change.NodeAdded;
import org.modeshape.jcr.cache.change.NodeRemoved;
import org.modeshape.jcr.core.ExecutionContext;
import org.modeshape.jcr.value.DateTime;
import org.modeshape.jcr.value.DateTimeFactory;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.Property;

@ThreadSafe
class RepositoryLockManager implements ChangeSetListener {

    private static final int KEY_OFFSET = "mode:lock-".length();

    private final JcrRepository.RunningState repository;
    private final String systemWorkspaceKey;
    private final String processId;
    private final ConcurrentMap<NodeKey, ModeShapeLock> locksByNodeKey;
    private final Path locksPath;
    private final Logger logger;

    RepositoryLockManager( JcrRepository.RunningState repository ) {
        this.repository = repository;
        this.systemWorkspaceKey = repository.repositoryCache().getSystemKey().getWorkspaceKey();
        this.processId = repository.context().getProcessId();
        this.locksByNodeKey = new ConcurrentHashMap<NodeKey, ModeShapeLock>();
        PathFactory pathFactory = repository.context().getValueFactories().getPathFactory();
        this.locksPath = pathFactory.create(pathFactory.createRootPath(), JcrLexicon.SYSTEM, ModeShapeLexicon.LOCKS);
        this.logger = Logger.getLogger(getClass());
    }

    /**
     * Refresh the locks from the stored representation.
     */
    protected void refreshFromSystem() {
        try {
            // Re-read and re-register all of the namespaces ...
            SessionCache systemCache = repository.createSystemSession(repository.context(), true);
            SystemContent system = new SystemContent(systemCache);
            CachedNode locks = system.locksNode();
            for (ChildReference ref : locks.getChildReferences(systemCache)) {
                CachedNode node = systemCache.getNode(ref);
                ModeShapeLock lock = new ModeShapeLock(node, systemCache);
                locksByNodeKey.put(lock.getLockedNodeKey(), lock);
            }
        } catch (Throwable e) {
            logger.error(e, JcrI18n.errorRefreshingLocks, repository.name());
        }
    }

    protected final NodeKey lockedNodeKeyFromLockKey( NodeKey key ) {
        // The identifier of the lock key contains "mode:lock-" followed by the full key of the locked node ...
        String identifier = key.getIdentifier();
        return new NodeKey(identifier.substring(KEY_OFFSET));
    }

    final boolean isLocked( NodeKey lockedNodeKey ) {
        return locksByNodeKey.containsKey(lockedNodeKey);
    }

    final ModeShapeLock findLockFor( NodeKey nodeKey ) {
        return locksByNodeKey.get(nodeKey);
    }

    private final CachedNode findLockedNodeAtOrBelow( CachedNode node,
                                                      NodeCache cache ) {
        if (node.getChildReferences(cache).isEmpty()) {
            // It is a leaf node, so just check for a lock on the node ...
            if (isLocked(node.getKey())) return node;
        }
        // We assume that there are far fewer locks than there are descendants of the supplied path ...
        Path path = node.getPath(cache);
        for (ModeShapeLock lock : locksByNodeKey.values()) {
            CachedNode lockedNode = cache.getNode(lock.getLockedNodeKey());
            if (lockedNode == null) continue;
            Path lockedPath = lockedNode.getPath(cache);
            if (lockedPath.isAtOrBelow(path)) return lockedNode;
        }
        return null;
    }

    final Collection<ModeShapeLock> allLocks() {
        return locksByNodeKey.values();
    }

    protected final NodeKey generateLockKey( NodeKey prototype,
                                             NodeKey lockedNodeKey ) {
        return prototype.withId("mode:lock-" + lockedNodeKey.toString());
    }

    protected final String generateLockToken() {
        return UUID.randomUUID().toString();
    }

    /**
     * Creates a lock on the node with the given {@link NodeKey key}. This method creates a new lock, registers it in the list of
     * locks, immediately modifies the {@code jcr:lockOwner} and {@code jcr:lockIsDeep} properties on the persisted node in the
     * underlying repository, and adds the lock to the persistent system view.
     * <p>
     * This method <i>assumes</i> that the node is lockable and not already locked.
     * </p>
     * 
     * @param session the session in which the node is being locked and that loaded the node
     * @param node the node that is being locked; may not be null
     * @param isDeep whether the node's descendants in the content graph should also be locked
     * @param isSessionScoped whether the lock should outlive the session in which it was created
     * @param timeoutHint desired lock timeout in seconds (servers are free to ignore this value); specify {@link Long#MAX_VALUE}
     *        for no timeout.
     * @param ownerInfo a string containing owner information supplied by the client, and recorded on the lock; if null, then the
     *        session's user ID will be used
     * @return an object representing the newly created lock
     * @throws LockException if the lock could not be obtained
     * @throws RepositoryException if an error occurs updating the graph state
     */
    ModeShapeLock lock( JcrSession session,
                        CachedNode node,
                        boolean isDeep,
                        boolean isSessionScoped,
                        long timeoutHint,
                        String ownerInfo ) throws LockException, RepositoryException {
        assert session != null;
        assert node != null;

        final ExecutionContext context = session.context();
        final String owner = ownerInfo != null ? ownerInfo : session.getUserID();

        // Create a new lock ...
        SessionCache systemSession = repository.createSystemSession(context, false);
        SystemContent system = new SystemContent(systemSession);
        NodeKey nodeKey = node.getKey();
        NodeKey lockKey = generateLockKey(system.locksKey(), nodeKey);
        String token = generateLockToken();
        ModeShapeLock lock = new ModeShapeLock(nodeKey, lockKey, session.workspaceName(), owner, token, isDeep, isSessionScoped);

        if (isDeep) {
            NodeCache cache = session.cache();
            CachedNode locked = findLockedNodeAtOrBelow(node, cache);
            if (locked != null) {
                String nodePath = session.stringFactory().create(node.getPath(cache));
                String descendantPath = session.stringFactory().create(locked.getPath(cache));
                throw new LockException(JcrI18n.descendantAlreadyLocked.text(nodePath, descendantPath));
            }
        }

        ModeShapeLock existing = locksByNodeKey.putIfAbsent(nodeKey, lock);
        if (existing != null) {
            NodeCache cache = session.cache();
            CachedNode locked = cache.getNode(existing.getLockedNodeKey());
            String lockedPath = session.stringFactory().create(locked.getPath(cache));
            throw new LockException(JcrI18n.alreadyLocked.text(lockedPath));
        }

        try {
            // Store the lock within the system area ...
            final DateTimeFactory dateFactory = context.getValueFactories().getDateFactory();
            long expirationTimeInMillis = RepositoryConfiguration.LOCK_EXPIRY_AGE_IN_MILLIS;
            if (timeoutHint != Long.MAX_VALUE && timeoutHint > 1) {
                expirationTimeInMillis = TimeUnit.MILLISECONDS.convert(timeoutHint, TimeUnit.SECONDS);
            }
            DateTime expirationDate = dateFactory.create().plus(expirationTimeInMillis, TimeUnit.MILLISECONDS);
            system.storeLock(session, lock, expirationDate);

            // Udpdate the persistent node ...
            SessionCache lockingSession = session.spawnSessionCache(false);
            MutableCachedNode lockedNode = lockingSession.mutable(nodeKey);
            lockedNode.lock(isSessionScoped);

            // Now save both sessions. This will fail with a LockFailureException if the locking failed ...
            lockingSession.save(systemSession, null);
        } catch (LockFailureException e) {
            // Someone must have snuck in and locked the node, and we just didn't receive notification of it yet ...
            String location = nodeKey.toString();
            try {
                location = session.node(nodeKey, null).getPath();
            } catch (Throwable t) {
                // couldn't come up with the path, so just use the key
            }
            locksByNodeKey.remove(nodeKey);
            throw new LockException(JcrI18n.alreadyLocked.text(location));
        } catch (RuntimeException e) {
            locksByNodeKey.remove(nodeKey);
            throw new RepositoryException(e);
        }
        return lock;
    }

    /**
     * Updates the underlying repository directly (i.e., outside the scope of the {@link Session}) to mark the token for the given
     * lock as being held (or not held) by some {@link Session}. Note that this method does not identify <i>which</i> (if any)
     * session holds the token for the lock, just that <i>some</i> session holds the token for the lock.
     * 
     * @param session the session on behalf of which the lock operation is being performed
     * @param lockToken the lock token for which the "held" status should be modified; may not be null
     * @param value the new value
     * @return true if the lock "held" status was successfully changed to the desired value, or false otherwise
     * @throws LockException if there is no such lock with the supplied token
     */
    boolean setHeldBySession( JcrSession session,
                              String lockToken,
                              boolean value ) throws LockException {
        assert lockToken != null;

        // Create a system session to remove the locks ...
        final ExecutionContext context = session.context();
        SessionCache systemSession = repository.createSystemSession(context, false);
        SystemContent system = new SystemContent(systemSession);

        // Mark the session as held/unheld ...
        if (!system.changeLockHeldBySession(lockToken, value)) {
            return false;
        }

        // Now save the session ...
        system.save();
        return true;
    }

    String unlock( JcrSession session,
                   NodeKey lockedNodeKey ) throws LockException {
        ModeShapeLock existing = locksByNodeKey.remove(lockedNodeKey);
        if (existing == null) {
            NodeCache cache = session.cache();
            String location = session.stringFactory().create(cache.getNode(lockedNodeKey).getPath(cache));
            throw new LockException(JcrI18n.notLocked.text(location));
        }
        unlock(session, Collections.singleton(existing));
        return existing.getLockToken();
    }

    private void unlock( JcrSession session,
                         Iterable<ModeShapeLock> locks ) {
        if (locks == null) return;

        // Create a system session to remove the locks ...
        final ExecutionContext context = session.context();
        SessionCache systemSession = repository.createSystemSession(context, false);
        SystemContent system = new SystemContent(systemSession);

        // And create a separate session cache to change the locked nodes ...
        SessionCache lockingSession = session.spawnSessionCache(false);

        // Remove the locks ...
        for (ModeShapeLock lock : locks) {
            system.removeLock(lock);
            MutableCachedNode lockedNode = lockingSession.mutable(lock.getLockedNodeKey());
            lockedNode.unlock();
        }

        // Now save the two sessions ...
        lockingSession.save(systemSession, null);
    }

    /**
     * Unlocks all locks corresponding to the tokens held by the supplied session.
     * 
     * @param session the session on behalf of which the lock operation is being performed
     * @throws RepositoryException if the session is not live
     */
    void cleanLocks( JcrSession session ) throws RepositoryException {
        Set<String> lockTokens = session.lockManager().lockTokens();
        List<ModeShapeLock> locks = null;
        for (ModeShapeLock lock : locksByNodeKey.values()) {
            if (lock.isSessionScoped() && lockTokens.contains(lock.getLockToken())) {
                if (locks == null) locks = new LinkedList<ModeShapeLock>();
                locks.add(lock);
            }
        }
        if (locks != null) unlock(session, locks);
    }

    @Override
    public void notify( ChangeSet changeSet ) {
        if (!systemWorkspaceKey.equals(changeSet.getWorkspaceKey())) {
            // The change does not affect the 'system' workspace, so skip it ...
            return;
        }
        if (processId.equals(changeSet.getProcessKey())) {
            // We generated these changes, so skip them ...
            return;
        }

        try {
            // Now process the changes ...
            Set<NodeKey> locksToDelete = null;
            for (Change change : changeSet) {
                if (change instanceof NodeAdded) {
                    NodeAdded added = (NodeAdded)change;
                    Path addedPath = added.getPath();
                    if (locksPath.isAncestorOf(addedPath)) {
                        // Get the name of the node type ...
                        Map<Name, Property> props = added.getProperties();
                        NodeKey lockKey = added.getKey();
                        ModeShapeLock lock = new ModeShapeLock(lockKey, props);
                        locksByNodeKey.put(lock.getLockedNodeKey(), lock);
                    }
                } else if (change instanceof NodeRemoved) {
                    NodeRemoved removed = (NodeRemoved)change;
                    Path removedPath = removed.getPath();
                    if (locksPath.isAncestorOf(removedPath)) {
                        // This was a lock that was removed ...
                        if (locksToDelete == null) locksToDelete = new HashSet<NodeKey>();
                        // The key of the locked node is embedded in the lock key ...
                        NodeKey lockedNodeKey = lockedNodeKeyFromLockKey(removed.getKey());
                        locksByNodeKey.remove(lockedNodeKey);
                    }
                }
                // Lock nodes are never moved, and properties added or removed, and the only properties changed are those
                // related to lock expiration, which we don't care about.
            }
        } catch (Throwable e) {
            logger.error(e, JcrI18n.errorCleaningUpLocks, repository.name());
        }
    }

    protected final String firstString( Property property ) {
        if (property == null) return null;
        return repository.context().getValueFactories().getStringFactory().create(property.getFirstValue());
    }

    protected final boolean firstBoolean( Property property ) {
        if (property == null) return false;
        return repository.context().getValueFactories().getBooleanFactory().create(property.getFirstValue());
    }

    /**
     * Internal representation of a locked node.
     */
    @Immutable
    public class ModeShapeLock {
        private final NodeKey lockedNodeKey;
        private final NodeKey lockKey;
        private final String lockOwner;
        private final String workspaceName;
        private final String lockToken;
        private final boolean deep;
        private final boolean sessionScoped;

        protected ModeShapeLock( CachedNode lockNode,
                                 NodeCache cache ) {
            this.lockKey = lockNode.getKey();
            this.lockedNodeKey = lockedNodeKeyFromLockKey(lockKey);
            this.workspaceName = firstString(lockNode.getProperty(ModeShapeLexicon.WORKSPACE, cache));
            this.lockOwner = firstString(lockNode.getProperty(JcrLexicon.LOCK_OWNER, cache));
            this.deep = firstBoolean(lockNode.getProperty(JcrLexicon.IS_DEEP, cache));
            this.sessionScoped = firstBoolean(lockNode.getProperty(ModeShapeLexicon.IS_SESSION_SCOPED, cache));
            this.lockToken = firstString(lockNode.getProperty(ModeShapeLexicon.LOCK_TOKEN, cache));
        }

        protected ModeShapeLock( NodeKey lockKey,
                                 Map<Name, Property> properties ) {
            this.lockKey = lockKey;
            this.lockedNodeKey = lockedNodeKeyFromLockKey(lockKey);
            this.workspaceName = firstString(properties.get(ModeShapeLexicon.WORKSPACE));
            this.lockOwner = firstString(properties.get(JcrLexicon.LOCK_OWNER));
            this.deep = firstBoolean(properties.get(JcrLexicon.IS_DEEP));
            this.sessionScoped = firstBoolean(properties.get(ModeShapeLexicon.IS_SESSION_SCOPED));
            this.lockToken = firstString(properties.get(ModeShapeLexicon.LOCK_TOKEN));
        }

        protected ModeShapeLock( NodeKey lockedNodeKey,
                                 NodeKey lockKey,
                                 String workspace,
                                 String lockOwner,
                                 String lockToken,
                                 boolean deep,
                                 boolean sessionScoped ) {
            this.lockedNodeKey = lockedNodeKey;
            this.lockKey = lockKey;
            this.lockOwner = lockOwner;
            this.workspaceName = workspace;
            this.deep = deep;
            this.sessionScoped = sessionScoped;
            this.lockToken = lockToken;
        }

        public boolean isLive() {
            return isLocked(lockedNodeKey);
        }

        public NodeKey getLockKey() {
            return lockKey;
        }

        public NodeKey getLockedNodeKey() {
            return lockedNodeKey;
        }

        public String getWorkspaceName() {
            return workspaceName;
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
            return lockToken;
        }

        @SuppressWarnings( "synthetic-access" )
        public Lock lockFor( final JcrSession session ) throws RepositoryException {
            final AbstractJcrNode node = session.node(lockedNodeKey, null);
            final JcrLockManager lockManager = session.workspace().getLockManager();
            return new Lock() {
                @Override
                public String getLockOwner() {
                    return lockOwner;
                }

                @Override
                public String getLockToken() {
                    if (sessionScoped) return null;
                    String token = ModeShapeLock.this.getLockToken();
                    return lockManager.hasLockToken(token) ? token : null;
                }

                protected final String lockToken() {
                    return ModeShapeLock.this.getLockToken();
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
                public boolean isLive() {
                    return ModeShapeLock.this.isLive();
                }

                @Override
                public boolean isSessionScoped() {
                    return sessionScoped;
                }

                @Override
                public void refresh() throws LockException {
                    String token = lockToken();
                    if (!lockManager.hasLockToken(token)) {
                        String location = null;
                        try {
                            location = node.getPath();
                        } catch (RepositoryException e) {
                            location = lockedNodeKey.toString();
                        }
                        throw new LockException(JcrI18n.notLocked.text(location));
                    }
                }

                @Override
                public long getSecondsRemaining() {
                    return isLockOwningSession() ? Integer.MAX_VALUE : Integer.MIN_VALUE;
                }

                @Override
                public boolean isLockOwningSession() {
                    String token = lockToken();
                    return lockManager.hasLockToken(token);
                }
            };
        }

        @Override
        public String toString() {
            return "Lock " + lockKey + " for " + lockedNodeKey + " in '" + workspaceName + "' (" + (deep ? "deep," : "shallow;")
                   + (sessionScoped ? "session;" : "global;") + "owner='" + lockOwner + "';token='" + lockToken + "';";
        }
    }
}
