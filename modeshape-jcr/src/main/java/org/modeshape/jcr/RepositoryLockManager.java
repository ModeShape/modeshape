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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
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
import org.modeshape.jcr.value.DateTimeFactory;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.PropertyFactory;

@ThreadSafe
class RepositoryLockManager implements ChangeSetListener {

    private static final int KEY_OFFSET = "mode:lock-".length();

    private final JcrRepository.RunningState repository;
    private final String systemWorkspaceName;
    private final String processId;
    private final ConcurrentMap<NodeKey, ModeShapeLock> locksByNodeKey;
    private final Path locksPath;
    private final Logger logger;
    private final long lockExtensionIntervalMillis;
    private final long defaultLockAgeMillis;

    RepositoryLockManager( JcrRepository.RunningState repository, RepositoryConfiguration.GarbageCollection gcConfig ) {
        this.repository = repository;
        this.systemWorkspaceName = repository.repositoryCache().getSystemWorkspaceName();
        this.processId = repository.context().getProcessId();
        this.locksByNodeKey = new ConcurrentHashMap<NodeKey, ModeShapeLock>();
        PathFactory pathFactory = repository.context().getValueFactories().getPathFactory();
        this.locksPath = pathFactory.create(pathFactory.createRootPath(), JcrLexicon.SYSTEM, ModeShapeLexicon.LOCKS);
        this.logger = Logger.getLogger(getClass());
        long lockGCIntervalMillis = gcConfig.getIntervalInMillis();
        assert lockGCIntervalMillis > 0;
        
        /*
         * Each time the garbage collection process runs, session-scoped locks that are still used by active sessions will have their
         * expiry times extended by this amount of time. Each repository instance in the ModeShape cluster will run its own cleanup
         * process, which will extend the expiry times of its own locks. As soon as a repository is no longer running the cleanup
         * process, we know that there can be no active sessions.
         *
         * The default GC interval is expressed in hours, so we make the extension slightly larger to avoid any lock being expired
         * prematurely.
         */
        this.lockExtensionIntervalMillis = lockGCIntervalMillis + TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);
        
        /*
         * The amount of time that a lock may be active before considered expired. The sweep process will extend the locks for active
         * sessions, so only unused locks will have an unmodified expiry time.
         * 
         * The default GC interval is expressed in hours, so we make the default age slightly less to avoid stale lock.
         */
        this.defaultLockAgeMillis = lockGCIntervalMillis - TimeUnit.MILLISECONDS.convert(10, TimeUnit.MINUTES);
    }

    RepositoryLockManager with( JcrRepository.RunningState repository,
                                RepositoryConfiguration.GarbageCollection gcConfig ) {
        assert this.systemWorkspaceName == repository.repositoryCache().getSystemWorkspaceName();
        assert this.processId == repository.context().getProcessId();
        PathFactory pathFactory = repository.context().getValueFactories().getPathFactory();
        Path locksPath = pathFactory.create(pathFactory.createRootPath(), JcrLexicon.SYSTEM, ModeShapeLexicon.LOCKS);
        assert this.locksPath.equals(locksPath);
        return new RepositoryLockManager(repository, gcConfig);
    }

    /**
     * Refresh the locks from the stored representation.
     */
    protected void refreshFromSystem() {
        try {
            // Re-read and re-register all of the namespaces ...
            SessionCache systemCache = repository.createSystemSession(repository.context(), false);
            SystemContent system = new SystemContent(systemCache);
            CachedNode locks = system.locksNode();
            MutableCachedNode mutableLocks = null;
            for (ChildReference ref : locks.getChildReferences(systemCache)) {
                CachedNode node = systemCache.getNode(ref);
                if (node == null) {
                    if (mutableLocks == null) {
                        mutableLocks = system.mutableLocksNode();
                    }
                    NodeKey lockKey = ref.getKey();
                    logger.warn(JcrI18n.lockNotFound, lockKey);
                    mutableLocks.removeChild(systemCache, lockKey);
                    continue;
                }
                
                ModeShapeLock lock = new ModeShapeLock(node, systemCache);
                locksByNodeKey.put(lock.getLockedNodeKey(), lock);
            }
            if (mutableLocks != null) {
                system.save();
            }
        } catch (Throwable e) {
            logger.error(e, JcrI18n.errorRefreshingLocks, repository.name());
        }
    }

    /**
     * Clean up the locks within the repository's system content. Any locks held by active sessions are extended/renewed, while
     * those locks that are significantly expired are removed.
     *
     * @param activeSessionIds the IDs of the sessions that are still active in this repository
     * @throws TimeoutException if a timeout occurs attempting to lock nodes in ISPN
     */
    protected void cleanupLocks( Set<String> activeSessionIds ) throws TimeoutException {
        try {
            ExecutionContext context = repository.context();

            DateTimeFactory dates = context.getValueFactories().getDateFactory();
            DateTime now = dates.create();
            DateTime newExpiration = dates.create(now, this.lockExtensionIntervalMillis);

            PropertyFactory propertyFactory = context.getPropertyFactory();
            SessionCache systemSession = repository.createSystemSession(context, false);
            SystemContent systemContent = new SystemContent(systemSession);

            Map<String, List<NodeKey>> lockedNodesByWorkspaceName = new HashMap<>();
            // Iterate over the locks ...
            CachedNode locksNode = systemContent.locksNode();
            ChildReferences childReferences = locksNode.getChildReferences(systemSession);
            if (childReferences.isEmpty()) {
                // there are no locks, so nothing to do
                return;
            }
            for (ChildReference ref : childReferences) {
                NodeKey lockKey = ref.getKey();
                CachedNode lockNode = systemSession.getNode(lockKey);
                if (lockNode == null) {
                    //it may happen that another thread has performed a session.logout which means the lock might have been removed
                    continue;
                }
                ModeShapeLock lock = new ModeShapeLock(lockNode, systemSession);
                NodeKey lockedNodeKey = lock.getLockedNodeKey();
                if (lock.isSessionScoped() && activeSessionIds.contains(lock.getLockingSessionId())) {
                    //for active session locks belonging to the sessions of this process, we want to extend the expiration date
                    //so that other processes in a cluster can tell that this lock is still active
                    MutableCachedNode mutableLockNode = systemSession.mutable(lockKey);
                    Property prop = propertyFactory.create(ModeShapeLexicon.EXPIRATION_DATE, newExpiration);
                    mutableLockNode.setProperty(systemSession, prop);
                    //reflect the change in the expiry date in the internal map
                    this.locksByNodeKey.replace(lockedNodeKey, lock.withExpiryTime(newExpiration));

                    continue;
                }

                //if it's not an active session lock, we always check the expiry date
                DateTime expirationDate = firstDate(lockNode.getProperty(ModeShapeLexicon.EXPIRATION_DATE, systemSession));
                if (expirationDate.isBefore(now)) {
                    //remove the lock from the system area
                    systemContent.removeLock(lock);
                    //register the target node which needs cleaning
                    List<NodeKey> lockedNodes = lockedNodesByWorkspaceName.get(lock.getWorkspaceName());
                    if (lockedNodes == null) {
                        lockedNodes = new ArrayList<>();
                        lockedNodesByWorkspaceName.put(lock.getWorkspaceName(), lockedNodes);
                    }
                    lockedNodes.add(lockedNodeKey);
                }
            }
            //persist all the changes to the locks from the system area
            systemSession.save();

            if (!lockedNodesByWorkspaceName.isEmpty()) {
                //update each of nodes which has been unlocked
                for (String workspaceName : lockedNodesByWorkspaceName.keySet()) {
                    SessionCache internalSession = repository.repositoryCache().createSession(context, workspaceName, false);
                    for (NodeKey lockedNodeKey : lockedNodesByWorkspaceName.get(workspaceName)) {
                        //clear the internal cache
                        this.locksByNodeKey.remove(lockedNodeKey);

                        CachedNode lockedNode = internalSession.getWorkspace().getNode(lockedNodeKey);
                        if (lockedNode != null) {
                            MutableCachedNode mutableLockedNode = internalSession.mutable(lockedNodeKey);
                            mutableLockedNode.removeProperty(internalSession, JcrLexicon.LOCK_IS_DEEP);
                            mutableLockedNode.removeProperty(internalSession, JcrLexicon.LOCK_OWNER);
                            mutableLockedNode.unlock();
                        }
                    }
                    internalSession.save();
                }
            }
        } catch (TimeoutException te) {
            // there was a timeout in ISPN while locking on some nodes (most likely mode:locks) so we should re-throw this to callers
            // so they can react
            throw te;
        } catch (Throwable t) {
            logger.error(t, JcrI18n.errorCleaningUpLocks, repository.name());
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

        final DateTimeFactory dateFactory = context.getValueFactories().getDateFactory();
        long expirationTimeInMillis = this.defaultLockAgeMillis;
        if (timeoutHint > 0 && timeoutHint < Long.MAX_VALUE) {
            expirationTimeInMillis = TimeUnit.MILLISECONDS.convert(timeoutHint, TimeUnit.SECONDS);
        }
        DateTime expirationDate = dateFactory.create().plus(expirationTimeInMillis, TimeUnit.MILLISECONDS);

        // Create a new lock ...
        SessionCache systemSession = repository.createSystemSession(context, false);
        SystemContent system = new SystemContent(systemSession);
        NodeKey nodeKey = node.getKey();
        NodeKey lockKey = generateLockKey(system.locksKey(), nodeKey);
        String token = generateLockToken();
        ModeShapeLock lock = new ModeShapeLock(nodeKey, lockKey, session.workspaceName(), owner, token, isDeep, isSessionScoped,
                                               session.sessionId(), expirationDate);

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
            system.storeLock(lock);

            // Update the persistent node ...
            SessionCache lockingSession = session.spawnSessionCache(false);
            MutableCachedNode lockedNode = lockingSession.mutable(nodeKey);

            PropertyFactory propertyFactory = session.propertyFactory();
            lockedNode.setProperty(lockingSession, propertyFactory.create(JcrLexicon.LOCK_OWNER, owner));
            lockedNode.setProperty(lockingSession, propertyFactory.create(JcrLexicon.LOCK_IS_DEEP, isDeep));
            lockedNode.lock(isSessionScoped);

            // Now save both sessions. This will fail with a LockFailureException if the locking failed ...
            // save the system session first so that the system change is reflected first in the ws caches
            systemSession.save(lockingSession, null);
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
            NodeKey lockedNodeKey = lock.getLockedNodeKey();
            if (session.cache().getNode(lockedNodeKey) == null) {
                // the node on which the lock was placed, has been removed
                continue;
            }
            MutableCachedNode lockedNode = lockingSession.mutable(lockedNodeKey);
            lockedNode.removeProperty(lockingSession, JcrLexicon.LOCK_IS_DEEP);
            lockedNode.removeProperty(lockingSession, JcrLexicon.LOCK_OWNER);
            lockedNode.unlock();
        }

        // Now save the two sessions ...
        lockingSession.save(systemSession, null);
    }

    /**
     * Unlocks all locks corresponding to the tokens held by the supplied session.
     * 
     * @param session the session on behalf of which the lock operation is being performed
     * @return a {@link Set} of the lock tokens that have been cleaned (removed from the system area and the corresponding nodes
     * unlocked)
     * @throws RepositoryException if the session is not live
     */
    Set<String> cleanLocks( JcrSession session ) throws RepositoryException {
        Set<String> lockTokens = session.lockManager().lockTokens();
        List<ModeShapeLock> locks = null;
        for (ModeShapeLock lock : locksByNodeKey.values()) {
            if (lock.isSessionScoped() && lockTokens.contains(lock.getLockToken())) {
                if (locks == null) locks = new LinkedList<ModeShapeLock>();
                locks.add(lock);
            }
        }

        Set<String> cleanedTokens = null;
        if (locks != null) {
            cleanedTokens = new HashSet<>(locks.size());
            // clear the locks which have been unlocked
            unlock(session, locks);
            for (ModeShapeLock lock : locks) {
                locksByNodeKey.remove(lock.getLockedNodeKey());
                cleanedTokens.add(lock.getLockToken());
            }
        }
        return cleanedTokens != null ? cleanedTokens : Collections.<String>emptySet();
    }

    @Override
    public void notify( ChangeSet changeSet ) {
        if (!systemWorkspaceName.equals(changeSet.getWorkspaceName())) {
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

    protected final DateTime firstDate( Property property ) {
        if (property == null) return null;
        return repository.context().getValueFactories().getDateFactory().create(property.getFirstValue());
    }

    final ModeShapeLock findLockByToken( String token ) {
        assert token != null;
        for (ModeShapeLock lock : locksByNodeKey.values()) {
            if (token.equals(lock.getLockToken())) {
                return lock;
            }
        }
        return null;
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
        private final String lockingSessionId;
        private final DateTime expiryTime;

        protected ModeShapeLock( CachedNode lockNode,
                                 NodeCache cache ) {
            this.lockKey = lockNode.getKey();
            this.lockedNodeKey = lockedNodeKeyFromLockKey(lockKey);
            this.workspaceName = firstString(lockNode.getProperty(ModeShapeLexicon.WORKSPACE, cache));
            this.lockOwner = firstString(lockNode.getProperty(JcrLexicon.LOCK_OWNER, cache));
            this.deep = firstBoolean(lockNode.getProperty(JcrLexicon.LOCK_IS_DEEP, cache));
            this.sessionScoped = firstBoolean(lockNode.getProperty(ModeShapeLexicon.IS_SESSION_SCOPED, cache));
            this.lockToken = firstString(lockNode.getProperty(ModeShapeLexicon.LOCK_TOKEN, cache));
            this.lockingSessionId = firstString(lockNode.getProperty(ModeShapeLexicon.LOCKING_SESSION, cache));
            this.expiryTime = firstDate(lockNode.getProperty(ModeShapeLexicon.EXPIRATION_DATE, cache));
        }

        protected ModeShapeLock( NodeKey lockKey,
                                 Map<Name, Property> properties ) {
            this.lockKey = lockKey;
            this.lockedNodeKey = lockedNodeKeyFromLockKey(lockKey);
            this.workspaceName = firstString(properties.get(ModeShapeLexicon.WORKSPACE));
            this.lockOwner = firstString(properties.get(JcrLexicon.LOCK_OWNER));
            this.deep = firstBoolean(properties.get(JcrLexicon.LOCK_IS_DEEP));
            this.sessionScoped = firstBoolean(properties.get(ModeShapeLexicon.IS_SESSION_SCOPED));
            this.lockToken = firstString(properties.get(ModeShapeLexicon.LOCK_TOKEN));
            this.lockingSessionId = firstString(properties.get(ModeShapeLexicon.LOCKING_SESSION));
            this.expiryTime = firstDate(properties.get(ModeShapeLexicon.EXPIRATION_DATE));
        }

        protected ModeShapeLock( NodeKey lockedNodeKey,
                                 NodeKey lockKey,
                                 String workspace,
                                 String lockOwner,
                                 String lockToken,
                                 boolean deep,
                                 boolean sessionScoped,
                                 String lockingSessionId,
                                 DateTime expiryTime ) {
            this.lockedNodeKey = lockedNodeKey;
            this.lockKey = lockKey;
            this.lockOwner = lockOwner;
            this.workspaceName = workspace;
            this.deep = deep;
            this.sessionScoped = sessionScoped;
            this.lockToken = lockToken;
            this.lockingSessionId = lockingSessionId;
            this.expiryTime = expiryTime;
        }

        protected ModeShapeLock withExpiryTime(DateTime expiryTime) {
            return new ModeShapeLock(this.lockedNodeKey, this.lockKey, this.workspaceName, this.lockOwner, this.lockToken,
                                     this.deep, this.sessionScoped, this.lockingSessionId, expiryTime);
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

        public String getLockingSessionId() { return lockingSessionId; }

        public DateTime getExpiryTime() { return expiryTime; }

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
                    return isLockOwningSession() ? Long.MAX_VALUE : Long.MIN_VALUE;
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
