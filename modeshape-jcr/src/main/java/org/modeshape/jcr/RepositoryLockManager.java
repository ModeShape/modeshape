package org.modeshape.jcr;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import net.jcip.annotations.ThreadSafe;
import org.modeshape.common.util.Logger;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Location;
import org.modeshape.graph.Node;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.observe.Changes;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.DateTimeFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.PathNotFoundException;
import org.modeshape.graph.property.Property;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.Path.Segment;
import org.modeshape.graph.request.ChangeRequest;
import org.modeshape.graph.request.CreateNodeRequest;

@ThreadSafe
class RepositoryLockManager implements JcrSystemObserver {

    private static final Logger LOGGER = Logger.getLogger(RepositoryLockManager.class);

    private final JcrRepository repository;
    private final ConcurrentMap<String, WorkspaceLockManager> lockManagers;
    private final Path locksPath;

    RepositoryLockManager( JcrRepository repository ) {
        this.repository = repository;
        ExecutionContext executionContext = repository.getExecutionContext();
        PathFactory pathFactory = executionContext.getValueFactories().getPathFactory();

        this.lockManagers = new ConcurrentHashMap<String, WorkspaceLockManager>();
        this.locksPath = pathFactory.create(pathFactory.createRootPath(), JcrLexicon.SYSTEM, ModeShapeLexicon.LOCKS);

        if (locksPath != null) {

            Property locksPrimaryType = executionContext.getPropertyFactory().create(JcrLexicon.PRIMARY_TYPE,
                                                                                     ModeShapeLexicon.LOCKS);

            repository.createSystemGraph(executionContext).create(locksPath, locksPrimaryType).ifAbsent().and();
        }

    }

    /**
     * Returns the lock manager for the named workspace (if one already exists) or creates a new lock manager and returns it. This
     * method is thread-safe.
     * 
     * @param workspaceName the name of the workspace for which the lock manager should be returned
     * @return the lock manager for the workspace; never null
     */
    WorkspaceLockManager getLockManager( String workspaceName ) {
        WorkspaceLockManager lockManager = lockManagers.get(workspaceName);
        if (lockManager != null) return lockManager;

        ExecutionContext executionContext = repository.getExecutionContext();
        lockManager = new WorkspaceLockManager(executionContext, this, workspaceName, locksPath);
        WorkspaceLockManager newLockManager = lockManagers.putIfAbsent(workspaceName, lockManager);

        if (newLockManager != null) return newLockManager;
        return lockManager;
    }

    JcrGraph createSystemGraph( ExecutionContext context ) {
        return repository.createSystemGraph(context);
    }

    JcrGraph createWorkspaceGraph( String workspaceName,
                                   ExecutionContext workspaceContext ) {
        return repository.createWorkspaceGraph(workspaceName, workspaceContext);
    }

    /**
     * Iterates through the list of session-scoped locks in this repository, deleting any session-scoped locks that were created
     * by a session that is no longer active.
     */
    void cleanUpLocks() {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(JcrI18n.cleaningUpLocks.text());
        }

        Set<JcrSession> activeSessions = repository.activeSessions();
        Set<String> activeSessionIds = new HashSet<String>(activeSessions.size());

        for (JcrSession activeSession : activeSessions) {
            activeSessionIds.add(activeSession.sessionId());
        }

        ExecutionContext executionContext = repository.getExecutionContext();
        Graph systemGraph = createSystemGraph(executionContext);
        PathFactory pathFactory = executionContext.getValueFactories().getPathFactory();
        ValueFactory<Boolean> booleanFactory = executionContext.getValueFactories().getBooleanFactory();
        ValueFactory<String> stringFactory = executionContext.getValueFactories().getStringFactory();

        DateTimeFactory dateFactory = executionContext.getValueFactories().getDateFactory();
        DateTime now = dateFactory.create();
        DateTime newExpirationDate = now.plusMillis(JcrEngine.LOCK_EXTENSION_INTERVAL_IN_MILLIS);

        Path locksPath = pathFactory.createAbsolutePath(JcrLexicon.SYSTEM, ModeShapeLexicon.LOCKS);

        Subgraph locksGraph = null;
        try {
            locksGraph = systemGraph.getSubgraphOfDepth(2).at(locksPath);
        } catch (PathNotFoundException pnfe) {
            // It's possible for this to run before the dna:locks child node gets added to the /jcr:system node.
            return;
        }

        for (Location lockLocation : locksGraph.getRoot().getChildren()) {
            Node lockNode = locksGraph.getNode(lockLocation);

            Boolean isSessionScoped = booleanFactory.create(lockNode.getProperty(ModeShapeLexicon.IS_SESSION_SCOPED).getFirstValue());

            if (!isSessionScoped) continue;
            String lockingSession = stringFactory.create(lockNode.getProperty(ModeShapeLexicon.LOCKING_SESSION).getFirstValue());

            // Extend locks held by active sessions
            if (activeSessionIds.contains(lockingSession)) {
                systemGraph.set(ModeShapeLexicon.EXPIRATION_DATE).on(lockLocation).to(newExpirationDate);
            } else {
                DateTime expirationDate = dateFactory.create(lockNode.getProperty(ModeShapeLexicon.EXPIRATION_DATE).getFirstValue());
                // Destroy expired locks (if it was still held by an active session, it would have been extended by now)
                if (expirationDate.isBefore(now)) {
                    String workspaceName = stringFactory.create(lockNode.getProperty(ModeShapeLexicon.WORKSPACE).getFirstValue());
                    WorkspaceLockManager lockManager = lockManagers.get(workspaceName);
                    lockManager.unlock(executionContext, lockManager.createLock(lockNode));
                }
            }
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace(JcrI18n.cleanedUpLocks.text());
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.JcrSystemObserver#getObservedPath()
     */
    @Override
    public Path getObservedPath() {
        return locksPath;
    }

    @Override
    public void notify( Changes changes ) {
        for (ChangeRequest change : changes.getChangeRequests()) {
            assert change.changedLocation().hasPath();

            Path changedPath = change.changedLocation().getPath();
            if (changedPath.equals(locksPath)) {
                // nothing to do with the "/jcr:system/mode:locks" node ...
                continue;
            }
            assert locksPath.isAncestorOf(changedPath);

            Segment rawUuid = changedPath.getLastSegment();
            UUID lockedNodeUuid = UUID.fromString(string(rawUuid));
            assert lockedNodeUuid != null;

            switch (change.getType()) {
                case CREATE_NODE:
                    CreateNodeRequest create = (CreateNodeRequest)change;

                    Property workspaceNameProp = null;
                    Property lockOwnerProp = null;
                    Property lockUuidProp = null;
                    Property isDeepProp = null;
                    Property isSessionScopedProp = null;

                    for (Property prop : create.properties()) {
                        if (JcrLexicon.LOCK_OWNER.equals(prop.getName())) {
                            lockOwnerProp = prop;
                        } else if (JcrLexicon.LOCK_IS_DEEP.equals(prop.getName())) {
                            isDeepProp = prop;
                        } else if (ModeShapeLexicon.IS_HELD_BY_SESSION.equals(prop.getName())) {
                            isSessionScopedProp = prop;
                        } else if (JcrLexicon.UUID.equals(prop.getName())) {
                            isSessionScopedProp = prop;
                        } else if (ModeShapeLexicon.WORKSPACE_NAME.equals(prop.getName())) {
                            workspaceNameProp = prop;
                        }
                    }

                    String lockOwner = firstString(lockOwnerProp);
                    String workspaceName = firstString(workspaceNameProp);
                    UUID lockUuid = firstUuid(lockUuidProp);
                    boolean isDeep = firstBoolean(isDeepProp);
                    boolean isSessionScoped = firstBoolean(isSessionScopedProp);

                    WorkspaceLockManager workspaceManager = getLockManager(workspaceName);
                    assert workspaceManager != null;

                    workspaceManager.lockNodeInternally(lockOwner, lockUuid, lockedNodeUuid, isDeep, isSessionScoped);

                    break;
                case DELETE_BRANCH:

                    
                    boolean success = false;
                    for (WorkspaceLockManager workspaceLockManager : lockManagers.values()) {
                        if (workspaceLockManager.lockFor(lockedNodeUuid) != null) {
                            success |= workspaceLockManager.unlockNodeInternally(lockedNodeUuid);
                            break;
                        }
                    }

                    assert success : "No internal lock existed for node " + lockedNodeUuid.toString();

                    break;
                default:
                    assert false : "Unexpected change request: " + change;
            }

        }
    }

    private final String string( Segment rawString ) {
        ExecutionContext context = repository.getExecutionContext();
        return context.getValueFactories().getStringFactory().create(rawString);
    }

    private final String firstString( Property property ) {
        if (property == null) return null;
        Object firstValue = property.getFirstValue();

        ExecutionContext context = repository.getExecutionContext();
        return context.getValueFactories().getStringFactory().create(firstValue);
    }

    private final UUID firstUuid( Property property ) {
        if (property == null) return null;
        Object firstValue = property.getFirstValue();

        ExecutionContext context = repository.getExecutionContext();
        return context.getValueFactories().getUuidFactory().create(firstValue);
    }

    private final boolean firstBoolean( Property property ) {
        if (property == null) return false;
        Object firstValue = property.getFirstValue();

        ExecutionContext context = repository.getExecutionContext();
        return context.getValueFactories().getBooleanFactory().create(firstValue);
    }
}
