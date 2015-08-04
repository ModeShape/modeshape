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
package org.modeshape.jcr.cache;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import javax.transaction.NotSupportedException;
import javax.transaction.SystemException;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.util.concurrent.TimeoutException;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.collection.Collections;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.jcr.ConfigurationException;
import org.modeshape.jcr.Connectors;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.ModeShape;
import org.modeshape.jcr.ModeShapeLexicon;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.Upgrades;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.bus.ChangeBus;
import org.modeshape.jcr.cache.change.Change;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.cache.change.RecordingChanges;
import org.modeshape.jcr.cache.change.RepositoryMetadataChanged;
import org.modeshape.jcr.cache.change.WorkspaceAdded;
import org.modeshape.jcr.cache.change.WorkspaceRemoved;
import org.modeshape.jcr.cache.document.DocumentOptimizer;
import org.modeshape.jcr.cache.document.DocumentStore;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.cache.document.LocalDocumentStore;
import org.modeshape.jcr.cache.document.LocalDocumentStore.DocumentOperation;
import org.modeshape.jcr.cache.document.LocalDocumentStore.DocumentOperationResults;
import org.modeshape.jcr.cache.document.ReadOnlySessionCache;
import org.modeshape.jcr.cache.document.WorkspaceCache;
import org.modeshape.jcr.cache.document.WritableSessionCache;
import org.modeshape.jcr.clustering.ClusteringService;
import org.modeshape.jcr.federation.ExternalDocumentStore;
import org.modeshape.jcr.txn.Transactions;
import org.modeshape.jcr.txn.Transactions.Transaction;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.PropertyFactory;
import org.modeshape.jcr.value.ValueFactory;

/**
 *
 */
public class RepositoryCache {

    private static final Logger LOGGER = Logger.getLogger(RepositoryCache.class);

    private static final String SYSTEM_METADATA_IDENTIFIER = "jcr:system/mode:metadata";
    private static final String REPOSITORY_INFO_KEY = "repository:info";
    private static final String REPOSITORY_NAME_FIELD_NAME = "repositoryName";
    private static final String REPOSITORY_KEY_FIELD_NAME = "repositoryKey";
    private static final String REPOSITORY_SOURCE_NAME_FIELD_NAME = "sourceName";
    private static final String REPOSITORY_SOURCE_KEY_FIELD_NAME = "sourceKey";
    private static final String REPOSITORY_CREATED_AT_FIELD_NAME = "createdAt";
    private static final String REPOSITORY_INITIALIZED_AT_FIELD_NAME = "intializedAt";
    private static final String REPOSITORY_INITIALIZER_FIELD_NAME = "intializer";
    private static final String REPOSITORY_CREATED_WITH_MODESHAPE_VERSION_FIELD_NAME = "createdWithModeShapeVersion";
    private static final String REPOSITORY_UPGRADE_ID_FIELD_NAME = "lastUpgradeId";
    private static final String REPOSITORY_UPGRADED_AT_FIELD_NAME = "lastUpgradedAt";
    private static final String REPOSITORY_UPGRADER_FIELD_NAME = "upgrader";

    private final ExecutionContext context;
    private final RepositoryConfiguration configuration;
    private final DocumentStore documentStore;
    private final DocumentTranslator translator;
    private final ConcurrentHashMap<String, WorkspaceCache> workspaceCachesByName;
    private final AtomicLong minimumStringLengthForBinaryStorage = new AtomicLong();
    private final AtomicBoolean accessControlEnabled = new AtomicBoolean(false);
    private final String name;
    private final String repoKey;
    private final String sourceKey;
    private final String rootNodeId;
    protected final ChangeBus changeBus;
    protected final NodeKey systemMetadataKey;
    private final NodeKey systemKey;
    protected final Set<String> workspaceNames;
    protected final String systemWorkspaceName;
    protected final Logger logger;
    private final RepositoryEnvironment repositoryEnvironment;
    private final String processKey;
    private final EmbeddedCacheManager workspaceCacheManager;
    protected final Upgrades upgrades;
    private volatile boolean initializingRepository = false;
    private volatile boolean upgradingRepository = false;
    private int lastUpgradeId;
    private final ClusteringService clusteringService;
    private volatile boolean isHoldingClusterLock = false;
    private final RepositoryFeaturesDetector repositoryFeaturesDetector;

    public RepositoryCache( ExecutionContext context,
                            DocumentStore documentStore,
                            ClusteringService clusteringService,
                            RepositoryConfiguration configuration,
                            ContentInitializer initializer,
                            RepositoryEnvironment repositoryEnvironment,
                            ChangeBus changeBus,
                            CacheContainer workspaceCacheContainer,
                            Upgrades upgradeFunctions ) {
        this.context = context;
        this.configuration = configuration;
        this.documentStore = documentStore;
        this.clusteringService = clusteringService;
        this.minimumStringLengthForBinaryStorage.set(configuration.getBinaryStorage().getMinimumStringSize());
        this.translator = new DocumentTranslator(this.context, this.documentStore, this.minimumStringLengthForBinaryStorage.get());
        this.repositoryEnvironment = repositoryEnvironment;
        this.processKey = context.getProcessId();
        if (! (workspaceCacheContainer instanceof EmbeddedCacheManager)) {
            throw new ConfigurationException(JcrI18n.workspaceCacheShouldBeEmbedded.text());
        }
        this.workspaceCacheManager = (EmbeddedCacheManager)workspaceCacheContainer;
        this.logger = Logger.getLogger(getClass());
        this.rootNodeId = RepositoryConfiguration.ROOT_NODE_ID;
        this.name = configuration.getName();
        this.workspaceCachesByName = new ConcurrentHashMap<>();
        this.workspaceNames = new CopyOnWriteArraySet<>(configuration.getAllWorkspaceNames());
        this.upgrades = upgradeFunctions;

        // if we're running in a cluster, try to acquire a global cluster lock to perform initialization
        if (clusteringService != null) {
            int minutesToWait = 10;
            LOGGER.debug("Waiting at most for {0} minutes while verifying the status of the '{1}' repository", minutesToWait,
                         name);
            if (!clusteringService.tryLock(minutesToWait, TimeUnit.MINUTES)) {
                throw new SystemFailureException(JcrI18n.repositoryWasNeverInitializedAfterMinutes.text(name, minutesToWait));
            }
            LOGGER.debug("Repository '{0}' acquired clustered-wide lock for performing initialization or verifying status", name);
            // at this point we should have a global cluster-wide lock
            isHoldingClusterLock = true;
        }

        SchematicEntry repositoryInfo = this.documentStore.localStore().get(REPOSITORY_INFO_KEY);
        boolean upgradeRequired = false;
        if (repositoryInfo == null) {
            // Create a UUID that we'll use as the string specifying who is doing the initialization ...
            String initializerId = UUID.randomUUID().toString();

            // Must be a new repository (or one created before 3.0.0.Final) ...
            this.repoKey = NodeKey.keyForSourceName(this.name);
            this.sourceKey = NodeKey.keyForSourceName(configuration.getStoreName());
            DateTime now = context.getValueFactories().getDateFactory().create();
            // Store this info in the repository info document ...
            EditableDocument doc = Schematic.newDocument();
            doc.setString(REPOSITORY_NAME_FIELD_NAME, this.name);
            doc.setString(REPOSITORY_KEY_FIELD_NAME, this.repoKey);
            doc.setString(REPOSITORY_SOURCE_NAME_FIELD_NAME, configuration.getStoreName());
            doc.setString(REPOSITORY_SOURCE_KEY_FIELD_NAME, this.sourceKey);
            doc.setDate(REPOSITORY_CREATED_AT_FIELD_NAME, now.toDate());
            doc.setString(REPOSITORY_INITIALIZER_FIELD_NAME, initializerId);
            doc.setString(REPOSITORY_CREATED_WITH_MODESHAPE_VERSION_FIELD_NAME, ModeShape.getVersion());
            doc.setNumber(REPOSITORY_UPGRADE_ID_FIELD_NAME, upgrades.getLatestAvailableUpgradeId());

            // store the repository info
            if (this.documentStore.localStore().putIfAbsent(REPOSITORY_INFO_KEY, doc) != null) {
                // if clustered, we should be holding a cluster-wide lock, so if some other process managed to write under this
                // key,
                // smth is seriously wrong. If not clustered, only 1 thread will always perform repository initialization.
                // in either case, this should not happen
                throw new SystemFailureException(JcrI18n.repositoryWasInitializedByOtherProcess.text(name));
            }
            repositoryInfo = this.documentStore.get(REPOSITORY_INFO_KEY);
            // We're doing the initialization ...
            initializingRepository = true;
            LOGGER.debug("Initializing the '{0}' repository", name);
        } else {
            // Get the repository key and source key from the repository info document ...
            Document info = repositoryInfo.getContent();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Repository '{0}' already initialized at '{1}'", name,
                             info.get(REPOSITORY_INITIALIZED_AT_FIELD_NAME));
            }
            String repoName = info.getString(REPOSITORY_NAME_FIELD_NAME, this.name);
            String sourceName = info.getString(REPOSITORY_SOURCE_NAME_FIELD_NAME, configuration.getStoreName());
            this.repoKey = info.getString(REPOSITORY_KEY_FIELD_NAME, NodeKey.keyForSourceName(repoName));
            this.sourceKey = info.getString(REPOSITORY_SOURCE_KEY_FIELD_NAME, NodeKey.keyForSourceName(sourceName));

            // See if this existing repository needs to be upgraded ...
            lastUpgradeId = info.getInteger(REPOSITORY_UPGRADE_ID_FIELD_NAME, 0);
            upgradeRequired = upgrades.isUpgradeRequired(lastUpgradeId);
            if (upgradeRequired && info.getString(REPOSITORY_UPGRADER_FIELD_NAME) == null) {
                int nextId = upgrades.getLatestAvailableUpgradeId();
                LOGGER.debug("The content in repository '{0}' needs to be upgraded (steps {1}->{2})", name, lastUpgradeId, nextId);

                // The repository does need to be upgraded and nobody is yet doing it. Note that we only want one process in the
                // cluster to do this, so we need to update the document store in an atomic fashion. So, first attempt to
                // lock the document ...
                this.upgradingRepository = runInTransaction(new Callable<Boolean>() {
                    @Override
                    public Boolean call() throws Exception {
                        LocalDocumentStore store = documentStore().localStore();
                        store.lockDocuments(Collections.unmodifiableSet(REPOSITORY_INFO_KEY));
                        EditableDocument editor = store.edit(REPOSITORY_INFO_KEY, true, false);
                        if (editor.get(REPOSITORY_UPGRADER_FIELD_NAME) == null) {
                            // Make sure that some other process didn't sneak in and already upgrade ...
                            int lastUpgradeId = editor.getInteger(REPOSITORY_UPGRADE_ID_FIELD_NAME, 0);
                            if (upgrades.isUpgradeRequired(lastUpgradeId)) {
                                // An upgrade is still required, and we get to do it ...
                                final String upgraderId = UUID.randomUUID().toString();
                                editor.setString(REPOSITORY_UPGRADER_FIELD_NAME, upgraderId);
                                return true;
                            }
                        }
                        // Another process is upgrading (or has already upgraded) the repository ...
                        return false;
                    }
                }, 1, REPOSITORY_INFO_KEY);
                if (this.upgradingRepository) {
                    LOGGER.debug("This process will upgrade the content in repository '{0}'", name);
                } else {
                    LOGGER.debug("The content in repository '{0}' does not need to be upgraded", name);
                }
            }
        }

        // If we're not doing the upgrade, then block for at most 10 minutes while another process does ...
        if (upgradeRequired && !upgradingRepository) {
            LOGGER.debug("Waiting at most for 10 minutes for another process in the cluster to upgrade the content in existing repository '{0}'",
                         name);
            waitUntil(new Callable<Boolean>() {
                @Override
                public Boolean call() {
                    Document info = documentStore().localStore().get(REPOSITORY_INFO_KEY).getContent();
                    int lastUpgradeId = info.getInteger(REPOSITORY_UPGRADE_ID_FIELD_NAME, 0);
                    return !upgrades.isUpgradeRequired(lastUpgradeId);
                }
            }, 10, TimeUnit.MINUTES, JcrI18n.repositoryWasNeverUpgradedAfterMinutes);
            LOGGER.debug("Content in existing repository '{0}' has been fully upgraded", name);
        } else if (!initializingRepository) {
            LOGGER.debug("Content in existing repository '{0}' does not need to be upgraded", name);
        }

        this.systemWorkspaceName = RepositoryConfiguration.SYSTEM_WORKSPACE_NAME;
        String systemWorkspaceKey = NodeKey.keyForWorkspaceName(systemWorkspaceName);
        this.systemMetadataKey = new NodeKey(this.sourceKey, systemWorkspaceKey, SYSTEM_METADATA_IDENTIFIER);

        // Initialize the workspaces ..
        refreshRepositoryMetadata(false);

        this.changeBus = changeBus;
        this.changeBus.registerInThread(new ChangesToWorkspacesListener());

        // Make sure the system workspace is configured to have a 'jcr:system' node ...
        SessionCache systemSession = createSession(context, systemWorkspaceName, false);
        NodeKey systemRootKey = systemSession.getRootKey();
        CachedNode systemRoot = systemSession.getNode(systemRootKey);
        logger.debug("System root: {0}", systemRoot);
        ChildReference systemRef = systemRoot.getChildReferences(systemSession).getChild(JcrLexicon.SYSTEM);
        logger.debug("jcr:system child reference: {0}", systemRef);
        CachedNode systemNode = systemRef != null ? systemSession.getNode(systemRef) : null;
        logger.debug("System node: {0}", systemNode);
        if (systemRef == null || systemNode == null) {
            logger.debug("Creating the '{0}' workspace in repository '{1}'", systemWorkspaceName, name);
            // We have to create the initial "/jcr:system" content ...
            MutableCachedNode root = systemSession.mutable(systemRootKey);
            if (initializer == null) {
                initializer = NO_OP_INITIALIZER;
            }
            initializer.initialize(systemSession, root);
            systemSession.save();
            // Now we need to forcibly refresh the system workspace cache ...
            refreshWorkspace(systemWorkspaceName);

            systemSession = createSession(context, systemWorkspaceName, false);
            systemRoot = systemSession.getNode(systemRootKey);
            systemRef = systemRoot.getChildReferences(systemSession).getChild(JcrLexicon.SYSTEM);
            if (systemRef == null) {
                throw new SystemFailureException(JcrI18n.unableToInitializeSystemWorkspace.text(name));
            }
        } else {
            logger.debug("Found existing '{0}' workspace in repository '{1}'", systemWorkspaceName, name);
        }
        this.systemKey = systemRef.getKey();

        // set the local source key in the document store
        this.documentStore.setLocalSourceKey(this.sourceKey);

        // init the features detector (must be done only after the system area has been fully initialized)
        this.repositoryFeaturesDetector = new RepositoryFeaturesDetector(systemSession, systemRef.getKey());
    }

    protected boolean waitUntil( Callable<Boolean> condition,
                                 long time,
                                 TimeUnit unit,
                                 I18n failureMsg ) {
        final long startTime = System.currentTimeMillis();
        final long quitTime = startTime + TimeUnit.MILLISECONDS.convert(time, unit);
        Exception lastError = null;
        while (System.currentTimeMillis() < quitTime) {
            try {
                lastError = null;
                if (condition.call()) return true;
            } catch (Exception e) {
                // Capture the exception ...
                lastError = e;
            }
            // Otherwise, sleep for a short bit ...
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.interrupted();
                break;
            }
        }

        // We waited to max amount of time, but still didn't see the successful condition, so abort by throwing an exception.
        // The process can be restarted at any time, and hopefully will eventually see the upgrade.
        LOGGER.error(lastError, failureMsg, name, time);
        String msg = failureMsg.text(name, time);
        throw new SystemFailureException(msg);
    }

    /**
     * Removes the repository info document, in case the repository has not yet been initialized (as indicated by the presence of
     * the #REPOSITORY_INITIALIZED_AT_FIELD_NAME field). This should only be used during repository startup, in case an unexpected
     * error occurs.
     */
    public final void rollbackRepositoryInfo() {
        try {
            SchematicEntry repositoryInfoEntry = this.documentStore.localStore().get(REPOSITORY_INFO_KEY);
            if (repositoryInfoEntry != null) {
                Document repoInfoDoc = repositoryInfoEntry.getContent();
                // we should only remove the repository info if it wasn't initialized successfully previously
                // in a cluster, it may happen that another node finished initialization while this node crashed (in which case we
                // should not remove the entry)
                if (!repoInfoDoc.containsField(REPOSITORY_INITIALIZED_AT_FIELD_NAME)) {
                    this.documentStore.localStore().remove(REPOSITORY_INFO_KEY);
                }
            }
        } finally {
            // if we have a global cluster-wide lock, make sure its released
            if (isHoldingClusterLock) {
                clusteringService.unlock();
                LOGGER.debug("Repository '{0}' released clustered-wide lock after failing to start up ", name);
            }
        }
    }

    public final boolean versioningUsed() {
        return repositoryFeaturesDetector.versioningUsed();
    }

    public final boolean lockingUsed() {
        return repositoryFeaturesDetector.lockingUsed();
    }

    public final ChangeBus changeBus() {
        return changeBus;
    }

    protected final RepositoryEnvironment repositoryEnvironment() {
        return repositoryEnvironment;
    }

    protected final String processKey() {
        return processKey;
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    public final boolean isInitializingRepository() {
        return initializingRepository;
    }

    public final boolean isAccessControlEnabled() {
        return accessControlEnabled.get();
    }

    public final void setAccessControlEnabled( boolean enabled ) {
        if (this.accessControlEnabled.compareAndSet(!enabled, enabled)) {
            refreshRepositoryMetadata(true);

            // And notify the others ...
            String userId = context.getSecurityContext().getUserName();
            Map<String, String> userData = context.getData();
            DateTime timestamp = context.getValueFactories().getDateFactory().create();
            RecordingChanges changes = new RecordingChanges(context.getId(), context.getProcessId(), this.getKey(), null,
                                                            repositoryEnvironment.journalId());
            changes.repositoryMetadataChanged();
            changes.freeze(userId, userData, timestamp);
            this.changeBus.notify(changes);
        }
    }

    protected final DocumentStore documentStore() {
        return this.documentStore;
    }

    protected final ExecutionContext context() {
        return this.context;
    }

    public RepositoryCache completeInitialization() {
        try {
            if (initializingRepository) {
                LOGGER.debug("Marking repository '{0}' as fully initialized", name);
                runInTransaction(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        LocalDocumentStore store = documentStore().localStore();
                        store.lockDocuments(Collections.unmodifiableSet(REPOSITORY_INFO_KEY));
                        EditableDocument repositoryInfo = store.edit(REPOSITORY_INFO_KEY, true, false);
                        if (repositoryInfo.get(REPOSITORY_INITIALIZED_AT_FIELD_NAME) == null) {
                            DateTime now = context().getValueFactories().getDateFactory().create();
                            repositoryInfo.setDate(REPOSITORY_INITIALIZED_AT_FIELD_NAME, now.toDate());
                        }
                        return null;
                    }
                }, 1, REPOSITORY_INFO_KEY);
                LOGGER.debug("Repository '{0}' is fully initialized", name);
            }
            return this;
        } finally {
            // if we have a global cluster-wide lock, make sure its released
            if (isHoldingClusterLock) {
                clusteringService.unlock();
                LOGGER.debug("Repository '{0}' released clustered-wide lock after successful startup", name);
            }
        }
    }

    public RepositoryCache completeUpgrade( final Upgrades.Context resources ) {
        if (upgradingRepository) {
            try {
                runInTransaction(new Callable<Void>() {
                    @SuppressWarnings( "synthetic-access" )
                    @Override
                    public Void call() throws Exception {
                        LOGGER.debug("Upgrading repository '{0}'", name);
                        lastUpgradeId = upgrades.applyUpgradesSince(lastUpgradeId, resources);
                        LOGGER.debug("Recording upgrade completion in repository '{0}'", name);

                        LocalDocumentStore store = documentStore().localStore();
                        EditableDocument editor = store.edit(REPOSITORY_INFO_KEY, true, false);
                        DateTime now = context().getValueFactories().getDateFactory().create();
                        editor.setDate(REPOSITORY_UPGRADED_AT_FIELD_NAME, now.toDate());
                        editor.setNumber(REPOSITORY_UPGRADE_ID_FIELD_NAME, lastUpgradeId);
                        editor.remove(REPOSITORY_UPGRADER_FIELD_NAME);
                        return null;
                    }
                }, 1, REPOSITORY_INFO_KEY);
                LOGGER.debug("Repository '{0}' is fully upgraded", name);
            } catch (Throwable err) {
                // We do NOT want an error during upgrade to prevent the repository from coming online.
                // Therefore, we need to catch any exceptions here an log them, but continue ...
                logger.error(err, JcrI18n.failureDuringUpgradeOperation, getName(), err);
                resources.getProblems().addError(err, JcrI18n.failureDuringUpgradeOperation, getName(), err);
            }
        }
        return this;
    }

    private <V> V runInTransaction( Callable<V> operation, int retryCountOnLockTimeout, String... keysToLock ) {
        // Start a transaction ...
        Transactions txns = repositoryEnvironment.getTransactions();
        try {
            Transaction txn = txns.begin();
            if (keysToLock.length > 0) {
                List<String> keysList = Arrays.asList(keysToLock);
                boolean locksAquired = false;
                while (!locksAquired && retryCountOnLockTimeout > 0) {
                    locksAquired = documentStore().localStore().lockDocuments(keysList);
                    --retryCountOnLockTimeout;
                }
                if (!locksAquired) {
                    txn.rollback();
                    throw new org.modeshape.jcr.TimeoutException(
                            "Cannot aquire lock on " + keysToLock + " after " + retryCountOnLockTimeout + " attempts");
                }
            }
            try {
                V result = operation.call();
                txn.commit();
                return result;
            } catch (Exception e) {
                // always rollback
                txn.rollback();
                // throw as is (see below)
                throw e;
            }
        } catch (IllegalStateException | SystemException err) {
            throw new SystemFailureException(err);
        } catch (NotSupportedException e) {
            logger.debug(e, "nested transactions not supported");
            return null;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void startShutdown() {
        // Shutdown the in-memory caches used for the WorkspaceCache instances ...
        for (Map.Entry<String, WorkspaceCache> entry : workspaceCachesByName.entrySet()) {
            // Tell the workspace cache intance that it's closed ...
            entry.getValue().signalClosing();
        }
    }

    public void completeShutdown() {
        // Shutdown the in-memory caches used for the WorkspaceCache instances ...
        for (Map.Entry<String, WorkspaceCache> entry : workspaceCachesByName.entrySet()) {
            String workspaceName = entry.getKey();
            // Tell the workspace cache intance that it's closed ...
            entry.getValue().signalClosed();
            // Remove the infinispan cache from the manager ...
            if (workspaceCacheManager.getStatus().equals(ComponentStatus.RUNNING)) {
                String cacheName = cacheNameForWorkspace(workspaceName);
                workspaceCacheManager.removeCache(cacheName);
            }
        }
    }

    /**
     * Get the identifier of the repository's metadata document.
     *
     * @return the cache key for the repository's metadata document; never null
     */
    public NodeKey getRepositoryMetadataDocumentKey() {
        return systemMetadataKey;
    }

    public void setLargeStringLength( long sizeInBytes ) {
        assert sizeInBytes > -1;
        minimumStringLengthForBinaryStorage.set(sizeInBytes);
        for (WorkspaceCache workspaceCache : workspaceCachesByName.values()) {
            assert workspaceCache != null;
            workspaceCache.setMinimumStringLengthForBinaryStorage(minimumStringLengthForBinaryStorage.get());
        }
    }

    public long largeValueSizeInBytes() {
        return minimumStringLengthForBinaryStorage.get();
    }

    protected void refreshRepositoryMetadata( boolean update ) {
        // Read the node document ...
        final DocumentTranslator translator = new DocumentTranslator(context, documentStore,
                                                                     minimumStringLengthForBinaryStorage.get());
        final String systemMetadataKeyStr = this.systemMetadataKey.toString();
        final boolean accessControlEnabled = this.accessControlEnabled.get();
        SchematicEntry entry = documentStore.get(systemMetadataKeyStr);

        if (!update && entry != null) {
            // We just need to read the metadata from the document, and we don't need a transaction for it ...
            Document doc = entry.getContent();
            Property accessProp = translator.getProperty(doc, name("accessControl"));
            boolean enabled = false;
            if (accessProp != null) {
                enabled = context.getValueFactories().getBooleanFactory().create(accessProp.getFirstValue());
            }
            this.accessControlEnabled.set(enabled);

            Property prop = translator.getProperty(doc, name("workspaces"));
            final Set<String> persistedWorkspaceNames = new HashSet<String>();
            ValueFactory<String> strings = context.getValueFactories().getStringFactory();
            boolean workspaceNotYetPersisted = false;
            for (Object value : prop) {
                String workspaceName = strings.create(value);
                persistedWorkspaceNames.add(workspaceName);
            }

            // detect if there are any new workspaces in the configuration which need persisting
            for (String configuredWorkspaceName : workspaceNames) {
                if (!persistedWorkspaceNames.contains(configuredWorkspaceName)) {
                    workspaceNotYetPersisted = true;
                    break;
                }
            }
            this.workspaceNames.addAll(persistedWorkspaceNames);
            if (!workspaceNotYetPersisted) {
                // only exit if there isn't a new workspace present. Otherwise, the config added a new workspace so we need
                // to make sure the meta-information is updated.
                return;
            }
        }

        try {
            runInTransaction(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    // Re-read the entry within the transaction ...
                    SchematicEntry entry = documentStore().get(systemMetadataKeyStr);
                    if (entry == null) {
                        // We need to create a new entry ...
                        EditableDocument newDoc = Schematic.newDocument();
                        translator.setKey(newDoc, systemMetadataKey);
                        entry = documentStore().localStore().putIfAbsent(systemMetadataKeyStr, newDoc);
                        if (entry == null) {
                            // Read-read the entry that we just put, so we can populate it with the same code that edits it ...
                            entry = documentStore().localStore().get(systemMetadataKeyStr);
                        }
                    }
                    EditableDocument doc = documentStore().localStore().edit(systemMetadataKeyStr, true, false);
                    PropertyFactory propFactory = context().getPropertyFactory();
                    translator.setProperty(doc, propFactory.create(name("workspaces"), workspaceNames), null, null);
                    translator.setProperty(doc, propFactory.create(name("accessControl"), accessControlEnabled), null, null);

                    return null;
                }
            }, 2, systemMetadataKeyStr);
        } catch (RuntimeException re) {
            LOGGER.error(JcrI18n.errorUpdatingRepositoryMetadata, name, re.getMessage());
            throw re;
        }
    }

    /**
     * Executes the given operation only once, when the repository is created for the first time, using child node under
     * jcr:system as a global "lock". In a cluster, this should only be run by the node which performs the initialization.
     *
     * @param initOperation a {@code non-null} {@link Callable} instance
     * @throws Exception if anything unexpected occurs, clients are expected to handle this
     */
    public void runOneTimeSystemInitializationOperation( Callable<Void> initOperation ) throws Exception {
        if (!isInitializingRepository()) {
            // we should only perform this operation if this is the node (in a cluster) that's initializing the repository
            return;
        }

        SessionCache systemSession = createSession(context, systemWorkspaceName, false);
        MutableCachedNode systemNode = getSystemNode(systemSession);

        // look for the node which acts as a "global monitor"
        ChildReference repositoryReference = systemNode.getChildReferences(systemSession).getChild(ModeShapeLexicon.REPOSITORY);
        if (repositoryReference != null) {
            // the presence of the repository node indicates that the operation has been run on this repository
            return;
        }

        initOperation.call();
        Property primaryType = context.getPropertyFactory().create(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.REPOSITORY);
        systemNode.createChild(systemSession, systemNode.getKey().withId("mode:repository"), ModeShapeLexicon.REPOSITORY,
                               primaryType);
        systemSession.save();
    }

    private MutableCachedNode getSystemNode( SessionCache systemSession ) {
        NodeKey systemRootKey = systemSession.getRootKey();
        CachedNode systemRoot = systemSession.getNode(systemRootKey);
        ChildReference systemRef = systemRoot.getChildReferences(systemSession).getChild(JcrLexicon.SYSTEM);
        return systemSession.mutable(systemRef.getKey());
    }

    protected class RepositoryFeaturesDetector implements ChangeSetListener {
        private final AtomicBoolean versioningUsed;
        private final NodeKey versionStorageKey;
        private final AtomicBoolean lockingUsed;
        private final NodeKey locksContainerKey;

        protected RepositoryFeaturesDetector( SessionCache systemSession,
                                              NodeKey systemNodeKey ) {
            CachedNode systemNode = systemSession.getNode(systemNodeKey);

            this.versionStorageKey = systemNode.getChildReferences(systemSession).getChild(JcrLexicon.VERSION_STORAGE).getKey();
            CachedNode versionStorage = systemSession.getNode(versionStorageKey);
            boolean versioningUsed = !versionStorage.getChildReferences(systemSession).isEmpty();
            this.versioningUsed = new AtomicBoolean(versioningUsed);

            this.locksContainerKey = systemNode.getChildReferences(systemSession).getChild(ModeShapeLexicon.LOCKS).getKey();
            CachedNode locksContainer = systemSession.getNode(locksContainerKey);
            boolean lockingUsed = !locksContainer.getChildReferences(systemSession).isEmpty();
            this.lockingUsed = new AtomicBoolean(lockingUsed);

            if (!versioningUsed || !lockingUsed) {
                // register with the change bus to be able to detect the changes in the usage of the various features
                RepositoryCache.this.changeBus.registerInThread(this);
            }
        }

        protected boolean lockingUsed() {
            return lockingUsed.get();
        }

        protected boolean versioningUsed() {
            return versioningUsed.get();
        }

        @Override
        public void notify( ChangeSet changeSet ) {
            if (!systemWorkspaceName.equals(changeSet.getWorkspaceName())) {
                return;
            }
            Set<NodeKey> nodeKeys = changeSet.changedNodes();
            if (!versioningUsed() && nodeKeys.contains(this.versionStorageKey)) {
                this.versioningUsed.set(true);
            }
            if (!lockingUsed() && nodeKeys.contains(this.locksContainerKey)) {
                this.lockingUsed.set(true);
            }
            if (versioningUsed() && lockingUsed()) {
                RepositoryCache.this.changeBus.unregister(this);
            }
        }
    }

    protected class ChangesToWorkspacesListener implements ChangeSetListener {
        @Override
        public void notify( ChangeSet changeSet ) {

            if (changeSet == null || !getKey().equals(changeSet.getRepositoryKey())) {
                return;
            }
            // Look at local and remote change sets ...
            if (changeSet.getWorkspaceName() == null) {
                // Look for changes to the workspaces ...
                Set<String> removedNames = new HashSet<String>();
                boolean changed = false;
                for (Change change : changeSet) {
                    if (change instanceof WorkspaceAdded) {
                        String addedName = ((WorkspaceAdded)change).getWorkspaceName();
                        if (!getWorkspaceNames().contains(addedName)) {
                            changed = true;
                        }
                    } else if (change instanceof WorkspaceRemoved) {
                        String removedName = ((WorkspaceRemoved)change).getWorkspaceName();
                        removedNames.add(removedName);
                        if (getWorkspaceNames().contains(removedName)) {
                            changed = true;
                        }
                    } else if (change instanceof RepositoryMetadataChanged) {
                        changed = true;
                    }
                }
                if (changed) {
                    // The set of workspaces (or repository metadata) was changed, so refresh them now ...
                    refreshRepositoryMetadata(false);

                    // And remove any already-cached workspaces. Note any open sessions to these workspaces will
                    for (String removedName : removedNames) {
                        removeWorkspaceCaches(removedName);
                    }
                }
            }
        }
    }

    /**
     * Get the key for this repository.
     *
     * @return the repository's key; never null
     */
    public final String getKey() {
        return this.repoKey;
    }

    public final NodeKey getSystemKey() {
        return this.systemKey;
    }

    public final String getSystemWorkspaceKey() {
        return NodeKey.keyForWorkspaceName(getSystemWorkspaceName());
    }

    public final String getSystemWorkspaceName() {
        return this.systemWorkspaceName;
    }

    /**
     * Get the name for this repository.
     *
     * @return the repository's name; never null
     */
    public final String getName() {
        return this.name;
    }

    /**
     * Get the names of all available workspaces in this repository. Not all workspaces may be loaded. This set does not contain
     * the system workspace name, as that workspace is not accessible/visible to JCR clients.
     *
     * @return the names of all available workspaces; never null and immutable
     */
    public final Set<String> getWorkspaceNames() {
        return Collections.unmodifiableSet(this.workspaceNames);
    }

    WorkspaceCache workspace( final String name ) {
        WorkspaceCache workspaceCache = workspaceCachesByName.get(name);

        if (workspaceCache != null) {
            return workspaceCache;
        }

        final boolean isSystemWorkspace = this.systemWorkspaceName.equals(name);
        if (!this.workspaceNames.contains(name) && !isSystemWorkspace) {
            throw new WorkspaceNotFoundException(name);
        }

        // We know that this workspace is not the system workspace, so find it ...
        final WorkspaceCache systemWorkspaceCache = isSystemWorkspace ? null : workspaceCachesByName.get(systemWorkspaceName);

        // when multiple threads (e.g. re-indexing threads) are performing ws cache initialization, we want this to be atomic
        synchronized (this) {
            // after we have the lock, check if maybe another thread has already finished
            if (!workspaceCachesByName.containsKey(name)) {
                WorkspaceCache initializedWsCache = runInTransaction(new Callable<WorkspaceCache>() {
                    @SuppressWarnings( "synthetic-access" )
                    @Override
                    public WorkspaceCache call() throws Exception {
                        // Create/get the Infinispan workspaceCache that we'll use within the WorkspaceCache, using the
                        // workspaceCache manager's
                        // default configuration ...
                        Cache<NodeKey, CachedNode> nodeCache = cacheForWorkspace(name);
                        ExecutionContext context = context();

                        // Compute the root key for this workspace ...
                        String workspaceKey = NodeKey.keyForWorkspaceName(name);
                        NodeKey rootKey = new NodeKey(sourceKey, workspaceKey, rootNodeId);

                        // Create the root document for this workspace ...
                        EditableDocument rootDoc = Schematic.newDocument();
                        DocumentTranslator trans = new DocumentTranslator(context, documentStore, Long.MAX_VALUE);
                        trans.setProperty(rootDoc,
                                          context.getPropertyFactory().create(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.ROOT),
                                          null, null);
                        trans.setProperty(rootDoc, context.getPropertyFactory().create(JcrLexicon.UUID, rootKey.toString()),
                                          null, null);

                        WorkspaceCache workspaceCache = new WorkspaceCache(context, getKey(), name, systemWorkspaceCache,
                                                                           documentStore, translator, rootKey, nodeCache,
                                                                           changeBus, repositoryEnvironment());

                        if (documentStore.localStore().putIfAbsent(rootKey.toString(), rootDoc) == null) {
                            // we are the first node to perform the initialization, so we need to link the system node
                            if (!RepositoryCache.this.systemWorkspaceName.equals(name)) {
                                logger.debug("Creating '{0}' workspace in repository '{1}'", name, getName());
                                SessionCache workspaceSession = new WritableSessionCache(context, workspaceCache,
                                                                                         repositoryEnvironment);
                                MutableCachedNode workspaceRootNode = workspaceSession.mutable(workspaceSession.getRootKey());
                                workspaceRootNode.linkChild(workspaceSession, RepositoryCache.this.systemKey, JcrLexicon.SYSTEM);

                                // this will be enrolled in the active transaction
                                workspaceSession.save();
                            }
                        }
                        return workspaceCache;
                    }
                }, 0);
                workspaceCachesByName.put(name, initializedWsCache);
            }
        }
        return workspaceCachesByName.get(name);
    }

    protected Cache<NodeKey, CachedNode> cacheForWorkspace( String name ) {
        String cacheName = cacheNameForWorkspace(name);
        if (LOGGER.isDebugEnabled()) {
            Set<String> cacheNames = workspaceCacheManager.getCacheNames();
            if (!cacheNames.contains(cacheName)) {
                // there is no explicit ws cache defined for this cache 
                LOGGER.debug(
                        "There is no explicit cache named '{0}' defined in the workspace cache container, using a default cache",
                        cacheName);
            }
        }
        Cache<NodeKey, CachedNode> cache = workspaceCacheManager.getCache(cacheName);
        Configuration cacheConfiguration = cache.getCacheConfiguration();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("The '{0}' workspace cache is using the cache configuration: '{1}'", cacheName,
                         cacheConfiguration);
        }
        validateWorkspaceCacheConfig(cacheName, cacheConfiguration);
        return cache;
    }

    private void validateWorkspaceCacheConfig( String cacheName, Configuration cacheConfiguration ) {
        if (cacheConfiguration.transaction() != null &&
            cacheConfiguration.transaction().transactionMode() != TransactionMode.NON_TRANSACTIONAL) {
            throw new ConfigurationException(JcrI18n.workspaceCacheShouldNotBeTransactional.text(cacheName));
        }
        if (cacheConfiguration.eviction() == null || cacheConfiguration.eviction().strategy() == EvictionStrategy.NONE) {
            throw new ConfigurationException(JcrI18n.workspaceCacheShouldUseEviction.text(cacheName));
        }
        if (cacheConfiguration.persistence() != null && cacheConfiguration.persistence().usingStores()) {
            throw new ConfigurationException(JcrI18n.workspaceCacheShouldNotUseLoaders.text(cacheName));
        }
    }
    protected final String cacheNameForWorkspace( String workspaceName ) {
        return this.name + "/" + workspaceName;
    }

    public final DocumentTranslator getDocumentTranslator() {
        return this.translator;
    }

    void removeWorkspaceCaches( String name ) {
        assert name != null;
        assert !this.workspaceNames.contains(name);
        WorkspaceCache removed = this.workspaceCachesByName.remove(name);
        if (removed != null) {
            try {
                removed.signalDeleted();
                repositoryEnvironment.getTransactionalWorkspaceCacheFactory().remove(name);
            } finally {
                workspaceCacheManager.removeCache(cacheNameForWorkspace(name));
            }
        }
    }

    /**
     * Drops any existing cache for the named workspace, so that the next time it's needed it will be reloaded from the persistent
     * storage.
     * <p>
     * This method does nothing if a cache for the workspace with the supplied name hasn't yet been created.
     * </p>
     *
     * @param name the name of the workspace
     */
    void refreshWorkspace( String name ) {
        assert name != null;
        this.workspaceCachesByName.remove(name);
    }

    Iterable<WorkspaceCache> workspaces() {
        return workspaceCachesByName.values();
    }

    /**
     * Create a new workspace in this repository, if the repository is appropriately configured. If the repository already
     * contains a workspace with the supplied name, then this method simply returns that workspace. Otherwise, this method
     * attempts to create the named workspace and will return a cache for this newly-created workspace.
     *
     * @param name the workspace name
     * @return the workspace cache for the new (or existing) workspace; never null
     * @throws UnsupportedOperationException if this repository was not configured to allow
     *         {@link RepositoryConfiguration#isCreatingWorkspacesAllowed() creation of workspaces}.
     */
    public WorkspaceCache createWorkspace( String name ) {
        if (!workspaceNames.contains(name)) {
            if (!configuration.isCreatingWorkspacesAllowed()) {
                throw new UnsupportedOperationException(JcrI18n.creatingWorkspacesIsNotAllowedInRepository.text(getName()));
            }
            // Otherwise, create the workspace and persist it ...
            this.workspaceNames.add(name);
            refreshRepositoryMetadata(true);

            // Now make sure that the "/jcr:system" node is a child of the root node ...
            SessionCache session = createSession(context, name, false);
            MutableCachedNode root = session.mutable(session.getRootKey());
            ChildReference ref = root.getChildReferences(session).getChild(JcrLexicon.SYSTEM);
            if (ref == null) {
                root.linkChild(session, systemKey, JcrLexicon.SYSTEM);
                session.save();
            }

            // And notify the others ...
            String userId = context.getSecurityContext().getUserName();
            Map<String, String> userData = context.getData();
            DateTime timestamp = context.getValueFactories().getDateFactory().create();
            RecordingChanges changes = new RecordingChanges(context.getId(), context.getProcessId(), this.getKey(), null,
                                                            repositoryEnvironment.journalId());
            changes.workspaceAdded(name);
            changes.freeze(userId, userData, timestamp);
            this.changeBus.notify(changes);
        }
        return workspace(name);
    }

    /**
     * Permanently destroys the workspace with the supplied name, if the repository is appropriately configured, also unlinking
     * the jcr:system node from the root node . If no such workspace exists in this repository, this method simply returns.
     * Otherwise, this method attempts to destroy the named workspace.
     *
     * @param name the workspace name
     * @param removeSession an outside session which will be used to unlink the jcr:system node and which is needed to guarantee
     *        atomicity.
     * @return true if the workspace with the supplied name existed and was destroyed, or false otherwise
     * @throws UnsupportedOperationException if this repository was not configured to allow
     *         {@link RepositoryConfiguration#isCreatingWorkspacesAllowed() creation (and destruction) of workspaces}.
     */
    public boolean destroyWorkspace( final String name,
                                     final WritableSessionCache removeSession ) {
        if (workspaceNames.contains(name)) {
            if (configuration.getPredefinedWorkspaceNames().contains(name)) {
                throw new UnsupportedOperationException(JcrI18n.unableToDestroyPredefinedWorkspaceInRepository.text(name,
                                                                                                                    getName()));
            }
            if (configuration.getDefaultWorkspaceName().equals(name)) {
                throw new UnsupportedOperationException(JcrI18n.unableToDestroyDefaultWorkspaceInRepository.text(name, getName()));
            }
            if (this.systemWorkspaceName.equals(name)) {
                throw new UnsupportedOperationException(JcrI18n.unableToDestroySystemWorkspaceInRepository.text(name, getName()));
            }
            if (!configuration.isCreatingWorkspacesAllowed()) {
                throw new UnsupportedOperationException(JcrI18n.creatingWorkspacesIsNotAllowedInRepository.text(getName()));
            }
            // persist *all* the changes in one unit, because in case of failure we need to remain in consistent state
            runInTransaction(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    // unlink the system node
                    removeSession.mutable(removeSession.getRootKey()).removeChild(removeSession, getSystemKey());
                    // remove the workspace and persist it
                    RepositoryCache.this.workspaceNames.remove(name);
                    refreshRepositoryMetadata(true);
                    // persist the active changes in the session
                    removeSession.save();

                    return null;
                }
            }, 0);

            // And notify the others - this notification will clear & close the WS cache via the local listener
            String userId = context.getSecurityContext().getUserName();
            Map<String, String> userData = context.getData();
            DateTime timestamp = context.getValueFactories().getDateFactory().create();
            RecordingChanges changes = new RecordingChanges(context.getId(), context.getProcessId(), this.getKey(), null,
                                                            repositoryEnvironment.journalId());
            changes.workspaceRemoved(name);
            changes.freeze(userId, userData, timestamp);
            this.changeBus.notify(changes);
            return true;
        }
        return false;
    }

    /**
     * Get the NodeCache for the workspace with the given name. This session can be used (by multiple concurrent threads) to read,
     * create, change, or delete content.
     * <p>
     * The session maintains a transient set of changes to the workspace content, and these changes are always visible. But
     * additionally any changes to the workspace content saved by other sessions will immediately be visible to this session.
     * Notice that at times the changes persisted by other sessions may cause some of this session's transient state to become
     * invalid. (For example, this session's newly-created child of some node, A, may become invalid or inaccessible if some other
     * session saved a deletion of node A.)
     *
     * @param workspaceName the name of the workspace; may not be null
     * @return the node cache; never null
     * @throws WorkspaceNotFoundException if no such workspace exists
     */
    public WorkspaceCache getWorkspaceCache( String workspaceName ) {
        return workspace(workspaceName);
    }

    /**
     * Creates a new workspace in the repository coupled with external document
     * store.
     *
     * @param name the name of the repository
     * @param connectors connectors to the external systems.
     * @return workspace cache for the new workspace.
     */
    public WorkspaceCache createExternalWorkspace(String name, Connectors connectors) {
        String[] tokens = name.split(":");
        
        String conName = tokens[0];
        String wsName = tokens[1];
        
        this.workspaceNames.add(wsName);
        refreshRepositoryMetadata(true);

        Cache<NodeKey, CachedNode> nodeCache = cacheForWorkspace(name);
        ExecutionContext context = context();
        
        //the name of the external connector is used for source name and workspace name
        
        String sourceKey = NodeKey.keyForSourceName(conName);
        String workspaceKey = NodeKey.keyForWorkspaceName(conName);

        //ask external system to determine root identifier.
        ExternalDocumentStore documentStore = new ExternalDocumentStore(connectors);
        String rootId = documentStore.getRootId(sourceKey);

        // Compute the root key for this workspace ...
        NodeKey rootKey = new NodeKey(sourceKey, workspaceKey, rootId);

        // We know that this workspace is not the system workspace, so find it ...
        final WorkspaceCache systemWorkspaceCache = workspaceCachesByName.get(systemWorkspaceName);
        
        WorkspaceCache workspaceCache = new WorkspaceCache(context, getKey(), 
                wsName, systemWorkspaceCache, documentStore, translator, rootKey, nodeCache, changeBus, repositoryEnvironment());
        workspaceCachesByName.put(wsName, workspaceCache);

        return workspace(wsName);
    }
    
    /**
     * Create a session for the workspace with the given name, using the supplied ExecutionContext for the session.
     *
     * @param context the context for the new session; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param readOnly true if the session is to be read-only
     * @return the new session that supports writes; never null
     * @throws WorkspaceNotFoundException if no such workspace exists
     */
    public SessionCache createSession(ExecutionContext context,
            String workspaceName,
            boolean readOnly) {
        WorkspaceCache workspaceCache = workspace(workspaceName);
        if (readOnly || workspaceCache.isExternal()) {
            return new ReadOnlySessionCache(context, workspaceCache, repositoryEnvironment);
        }
        return new WritableSessionCache(context, workspaceCache, repositoryEnvironment);
    }

    /**
     * Optimize the children in the supplied node document
     * <p>
     * Note that this method changes the underlying db as well as the given document, so *it must* be called either from a
     * transactional context or it must be followed by a session.save call, otherwise there might be inconsistencies between what
     * a session sees as "persisted" state and the reality.
     * </p>
     *
     * @param targetCountPerBlock the target number of children per block
     * @param tolerance the allowed tolerance between the target and actual number of children per block
     * @return the results of the optimization; never null
     */
    public DocumentOperationResults optimizeChildren( final int targetCountPerBlock,
                                                      final int tolerance ) {
        Stopwatch sw = new Stopwatch();
        logger.info(JcrI18n.beginChildrenOptimization, getName());
        sw.start();

        try {
            DocumentOperationResults results = documentStore().localStore().performOnEachDocument(new DocumentOperation() {
                private static final long serialVersionUID = 1L;

                private DocumentOptimizer optimizer;

                @Override
                public void setEnvironment( Cache<String, SchematicEntry> cache ) {
                    super.setEnvironment(cache);
                    this.optimizer = new DocumentOptimizer(cache);
                }

                @Override
                public boolean execute( String key,
                                        EditableDocument document ) {
                    return this.optimizer.optimizeChildrenBlocks(new NodeKey(key), document, targetCountPerBlock, tolerance);
                }
            });
            sw.stop();
            logger.info(JcrI18n.completeChildrenOptimization, getName(), sw.getTotalDuration().toSimpleString(), results);
            return results;
        } catch (TimeoutException te) {
            // we were unable to obtain some locks in ISPN while trying to do optimize so we'll just log this since the operation
            // will be retried later and the transaction should've been rolled back
            if (logger.isDebugEnabled()) {
                logger.debug(te, "Unable to obtain ISPN locks while trying to perform document optimization");
            }
        } catch (Throwable e) {
            logger.info(JcrI18n.errorDuringChildrenOptimization, getName(), sw.getTotalDuration().toSimpleString(), e);
        }
        return null;
    }

    @Override
    public String toString() {
        return name;
    }

    public static interface ContentInitializer {
        public void initialize( SessionCache session,
                                MutableCachedNode parent );
    }

    public static final ContentInitializer NO_OP_INITIALIZER = new ContentInitializer() {
        @Override
        public void initialize( SessionCache session,
                                MutableCachedNode parent ) {
        }
    };
}
