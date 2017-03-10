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

import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.collection.Collections;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.statistic.Stopwatch;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.Connectors;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.ModeShape;
import org.modeshape.jcr.ModeShapeLexicon;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.RepositoryEnvironment;
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
import org.modeshape.jcr.cache.document.LocalDocumentStore.DocumentOperationResults;
import org.modeshape.jcr.cache.document.ReadOnlySessionCache;
import org.modeshape.jcr.cache.document.TransactionalWorkspaceCaches;
import org.modeshape.jcr.cache.document.WorkspaceCache;
import org.modeshape.jcr.cache.document.WritableSessionCache;
import org.modeshape.jcr.federation.FederatedDocumentStore;
import org.modeshape.jcr.spi.federation.Connector;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.PropertyFactory;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.schematic.Schematic;
import org.modeshape.schematic.SchematicEntry;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.EditableArray;
import org.modeshape.schematic.document.EditableDocument;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;

/**
 *
 */
public class RepositoryCache {

    public static final String REPOSITORY_INFO_KEY = "repository:info";

    private static final Logger LOGGER = Logger.getLogger(RepositoryCache.class);

    private static final String SYSTEM_METADATA_IDENTIFIER = "jcr:system/mode:metadata";
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
    private final TransactionalWorkspaceCaches txWorkspaceCaches;
    private final String processKey;
    protected final Upgrades upgrades;
    private volatile boolean initializingRepository = false;
    private volatile boolean upgradingRepository = false;
    private int lastUpgradeId;
    private final int workspaceCacheSize;

    public RepositoryCache(ExecutionContext context,
                           DocumentStore documentStore,
                           RepositoryConfiguration configuration,
                           ContentInitializer initializer,
                           RepositoryEnvironment repositoryEnvironment,
                           ChangeBus changeBus,
                           Upgrades upgradeFunctions) {
        assert initializer != null;
        this.context = context;
        this.configuration = configuration;
        this.documentStore = documentStore;
        this.minimumStringLengthForBinaryStorage.set(configuration.getBinaryStorage().getMinimumStringSize());
        this.translator = new DocumentTranslator(this.context, this.documentStore, this.minimumStringLengthForBinaryStorage.get());
        this.repositoryEnvironment = repositoryEnvironment;
        this.txWorkspaceCaches = new TransactionalWorkspaceCaches(repositoryEnvironment.getTransactions());
        this.processKey = context.getProcessId();
        this.logger = Logger.getLogger(getClass());
        this.rootNodeId = RepositoryConfiguration.ROOT_NODE_ID;
        this.name = configuration.getName();
        this.workspaceCachesByName = new ConcurrentHashMap<>();
        this.workspaceNames = new CopyOnWriteArraySet<>(configuration.getAllWorkspaceNames());
        this.upgrades = upgradeFunctions;
        this.workspaceCacheSize = configuration.getWorkspaceCacheSize();
        CheckArg.isPositive(workspaceCacheSize, "workspaceCacheSize");
        
        SchematicEntry repositoryInfo = this.documentStore.localStore().get(REPOSITORY_INFO_KEY);
        boolean upgradeRequired = false;
        String databaseId = documentStore.localStore().databaseId();
        if (repositoryInfo == null) {
            // Create a UUID that we'll use as the string specifying who is doing the initialization ...
            String initializerId = UUID.randomUUID().toString();
    
            // Must be a new repository (or one created before 3.0.0.Final) ...
            this.repoKey = NodeKey.keyForSourceName(this.name);
            this.sourceKey = NodeKey.keyForSourceName(databaseId);
            DateTime now = context.getValueFactories().getDateFactory().create();
            // Store this info in the repository info document ...
            EditableDocument doc = Schematic.newDocument();
            doc.setString(REPOSITORY_NAME_FIELD_NAME, this.name);
            doc.setString(REPOSITORY_KEY_FIELD_NAME, this.repoKey);
            doc.setString(REPOSITORY_SOURCE_NAME_FIELD_NAME, databaseId);
            doc.setString(REPOSITORY_SOURCE_KEY_FIELD_NAME, this.sourceKey);
            Date creationDate = now.toDate();
            doc.setDate(REPOSITORY_CREATED_AT_FIELD_NAME, creationDate);
            doc.setString(REPOSITORY_INITIALIZER_FIELD_NAME, initializerId);
            doc.setString(REPOSITORY_CREATED_WITH_MODESHAPE_VERSION_FIELD_NAME, ModeShape.getVersion());
            doc.setNumber(REPOSITORY_UPGRADE_ID_FIELD_NAME, upgrades.getLatestAvailableUpgradeId());
    
            // loop until we know for sure someone is initializing a repository
            boolean initializerDecided = false;
            while (!initializerDecided) {
                LOGGER.debug("Initializer '{0}' is determining who should be initializing repository '{1}'", initializerId,
                             name);
                // run a transaction which writes the repository info to the DB. Note that it's possible in a cluster for someone
                // else to have successfully written something else first
                localStore().runInTransaction(
                        () -> this.documentStore.storeIfAbsent(REPOSITORY_INFO_KEY, doc), 0);
                // re-read the entry which was persisted (may be ours but may also belong to another process)
                // note that we're not relying on the outcome of the previous method intentionally to avoid any potential DB transaction
                // isolation issues, which could give us back our own entry even though someone else may've persisted something else
                SchematicEntry initializerInfo = this.documentStore.get(REPOSITORY_INFO_KEY);
                if (initializerInfo != null) {
                    Document content = initializerInfo.content();
                    initializingRepository = initializerId.equals(content.getString(REPOSITORY_INITIALIZER_FIELD_NAME)) &&
                                             creationDate.equals(content.getDate(REPOSITORY_CREATED_AT_FIELD_NAME));
                    if (initializingRepository || content.containsField(REPOSITORY_INITIALIZED_AT_FIELD_NAME)) {
                        // we're the ones initializing or someone else has already finished
                        initializerDecided = true;
                    } else {
                        // someone else is doing the initialization so wait until that completes or is rolled back
                        AtomicBoolean isRolledBack = new AtomicBoolean(false);
                        waitUntil(() -> {
                            LOGGER.debug("Repository '{0}' is being initialized by another process; waiting until that finished or is rolled back");
                            SchematicEntry persistedInitializerInfo = this.documentStore.get(REPOSITORY_INFO_KEY);
                            if (persistedInitializerInfo == null) {
                                isRolledBack.set(true);
                            }
                            return isRolledBack.get() || 
                                   persistedInitializerInfo.content().containsField(REPOSITORY_INITIALIZED_AT_FIELD_NAME);
                        }, 10, TimeUnit.MINUTES, JcrI18n.repositoryWasNeverInitializedAfterMinutes);
                        // it may have been rolled back, in which case we'll try ourselves again
                        initializerDecided = !isRolledBack.get();
                    }
                }
            }
            if (!initializingRepository) {
                // some other process (local or in a cluster) has gotten ahead of us...
                LOGGER.debug("Repository '{0}' has been initialized by another initializer: '{1}'", name, initializerId);
            } else {
                // We're doing the initialization ...
                LOGGER.debug("Initializer '{0}' is initializing repository '{1}'", initializerId, name);
            }
        } else {
            // Get the repository key and source key from the repository info document ...
            Document info = repositoryInfo.content();
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Repository '{0}' already initialized at '{1}'", name,
                             info.get(REPOSITORY_INITIALIZED_AT_FIELD_NAME));
            }
            String repoName = info.getString(REPOSITORY_NAME_FIELD_NAME, this.name);
            String sourceName = info.getString(REPOSITORY_SOURCE_NAME_FIELD_NAME, databaseId);
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
                this.upgradingRepository = localStore().runInTransaction(() -> {
                    LocalDocumentStore store = documentStore().localStore();
                    EditableDocument editor = store.edit(REPOSITORY_INFO_KEY, true);
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
            waitUntil(() -> {
                Document info = documentStore().localStore().get(REPOSITORY_INFO_KEY).content();
                int lastUpgradeId = info.getInteger(REPOSITORY_UPGRADE_ID_FIELD_NAME, 0);
                return !upgrades.isUpgradeRequired(lastUpgradeId);
            }, 10, TimeUnit.MINUTES, JcrI18n.repositoryWasNeverUpgradedAfterMinutes);
            LOGGER.debug("Content in existing repository '{0}' has been fully upgraded", name);
        } else if (!initializingRepository) {
            LOGGER.debug("Content in existing repository '{0}' does not need to be upgraded", name);
        }

        this.systemWorkspaceName = RepositoryConfiguration.SYSTEM_WORKSPACE_NAME;
        String systemWorkspaceKey = NodeKey.keyForWorkspaceName(systemWorkspaceName);
        this.systemMetadataKey = new NodeKey(this.sourceKey, systemWorkspaceKey, SYSTEM_METADATA_IDENTIFIER);
        
        // Initialize the workspaces ..
        Set<String> newWorkspaces = refreshRepositoryMetadata(false);

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
            initializer.initializeSystemArea(systemSession, root);
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
            // even if the system node is already there and the system area was not initialized, it can happen that certain parts
            // of the jcr:system area is still missing. For example restoring a 3.x repository into 4.x. Therefore this needs to 
            // be performed here, after we've loaded the system root nodes
            if (initializer.initializeIndexStorage(systemSession, systemSession.mutable(systemNode.getKey()))) {
                logger.debug("Initialized index storage area in the '{0}' workspace of the repository '{1}'", 
                             systemWorkspaceName, name);
                systemSession.save();
            }
        }
        this.systemKey = systemRef.getKey();

        // set the local source key in the document store
        this.documentStore.setLocalSourceKey(this.sourceKey);
        
        // and create any predefined workspaces (includes default)
        if (initializingRepository && !newWorkspaces.isEmpty()) {
            localStore().runInTransaction(() -> {
                newWorkspaces.forEach(this::createWorkspace);
                return null;
            }, 0, REPOSITORY_INFO_KEY);
        }
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
        SchematicEntry repositoryInfoEntry = this.documentStore.localStore().get(REPOSITORY_INFO_KEY);
        if (repositoryInfoEntry != null && initializingRepository) {
            localStore().runInTransaction(() -> {
                Document repoInfoDoc = repositoryInfoEntry.content();
                // we should only remove the repository info if it wasn't initialized successfully previously
                // in a cluster, it may happen that another node finished initialization while this node crashed (in which case we
                // should not remove the entry)
                if (!repoInfoDoc.containsField(REPOSITORY_INITIALIZED_AT_FIELD_NAME)) {
                    this.documentStore.localStore().remove(REPOSITORY_INFO_KEY);
                }
                return null;
            }, 0, REPOSITORY_INFO_KEY);
        }
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
    
    protected LocalDocumentStore localStore() {
        return documentStore.localStore();
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

    public void completeInitialization(Upgrades.Context upgradeContext) {
        if (initializingRepository || upgradingRepository) {
            localStore().runInTransaction(() -> {
                if (initializingRepository) {
                    writeInitializedAt();
                }
                if (upgradingRepository) {
                    doUpgrade(upgradeContext);
                }
                return null;
            }, 0 , REPOSITORY_INFO_KEY) ;
        }
    }
    
    private void writeInitializedAt() {
        LOGGER.debug("Marking repository '{0}' as fully initialized", name);
        LocalDocumentStore store = documentStore().localStore();
        EditableDocument repositoryInfo = store.edit(REPOSITORY_INFO_KEY, true);
        if (repositoryInfo.get(REPOSITORY_INITIALIZED_AT_FIELD_NAME) == null) {
            DateTime now = context().getValueFactories().getDateFactory().create();
            repositoryInfo.setDate(REPOSITORY_INITIALIZED_AT_FIELD_NAME, now.toDate());
        }
        LOGGER.debug("Repository '{0}' is fully initialized", name);
    }
    
    private void doUpgrade(final Upgrades.Context resources) {
        try {
            LOGGER.debug("Upgrading repository '{0}'", name);
            lastUpgradeId = upgrades.applyUpgradesSince(lastUpgradeId, resources);
            LOGGER.debug("Recording upgrade completion in repository '{0}'", name);
        
            LocalDocumentStore store = documentStore().localStore();
            EditableDocument editor = store.edit(REPOSITORY_INFO_KEY, true);
            DateTime now = context().getValueFactories().getDateFactory().create();
            editor.setDate(REPOSITORY_UPGRADED_AT_FIELD_NAME, now.toDate());
            editor.setNumber(REPOSITORY_UPGRADE_ID_FIELD_NAME, lastUpgradeId);
            editor.remove(REPOSITORY_UPGRADER_FIELD_NAME);
            LOGGER.debug("Repository '{0}' is fully upgraded", name);
        } catch (Throwable err) {
            // We do NOT want an error during upgrade to prevent the repository from coming online.
            // Therefore, we need to catch any exceptions here an log them, but continue ...
            logger.error(err, JcrI18n.failureDuringUpgradeOperation, getName(), err);
            resources.getProblems().addError(err, JcrI18n.failureDuringUpgradeOperation, getName(), err);
        }
    }
    
    public void startShutdown() {
        // Shutdown the in-memory caches used for the WorkspaceCache instances ...
        workspaceCachesByName.values().stream().forEach(WorkspaceCache::signalClosing);
    }

    public void completeShutdown() {
        // Shutdown the in-memory caches used for the WorkspaceCache instances ...
        workspaceCachesByName.values().stream().forEach(WorkspaceCache::signalClosed);
        workspaceCachesByName.clear();
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

    protected Set<String> refreshRepositoryMetadata( boolean update ) {
        final String systemMetadataKeyStr = this.systemMetadataKey.toString();
        final boolean accessControlEnabled = this.accessControlEnabled.get();
        SchematicEntry entry = documentStore.get(systemMetadataKeyStr);
        final Set<String> newWorkspaces = new HashSet<>(this.workspaceNames);

        if (!update && entry != null) {
            // We just need to read the metadata from the document, and we don't need a transaction for it ...
            Document doc = entry.content();
            Property accessProp = translator.getProperty(doc, name("accessControl"));
            boolean enabled = false;
            if (accessProp != null) {
                enabled = context.getValueFactories().getBooleanFactory().create(accessProp.getFirstValue());
            }
            this.accessControlEnabled.set(enabled);

            Property prop = translator.getProperty(doc, name("workspaces"));
            final Set<String> persistedWorkspaceNames = new HashSet<String>();
            ValueFactory<String> strings = context.getValueFactories().getStringFactory();
            for (Object value : prop) {
                String workspaceName = strings.create(value);
                persistedWorkspaceNames.add(workspaceName);
            }

            // detect if there are any new workspaces in the configuration which need persisting
            for (String configuredWorkspaceName : workspaceNames) {
                if (persistedWorkspaceNames.contains(configuredWorkspaceName)) {
                    newWorkspaces.remove(configuredWorkspaceName);
                }
            }
            this.workspaceNames.addAll(persistedWorkspaceNames);
            if (newWorkspaces.isEmpty()) {
                // only exit if there isn't a new workspace present. Otherwise, the config added a new workspace so we need
                // to make sure the meta-information is updated.
                return newWorkspaces;
            }
        }

        try {
            localStore().runInTransaction(() -> {
                // Re-read the entry within the transaction ...
                SchematicEntry systemEntry = documentStore().get(systemMetadataKeyStr);
                if (systemEntry == null) {
                    // We need to create a new entry ...
                    EditableDocument newDoc = Schematic.newDocument();
                    translator.setKey(newDoc, systemMetadataKey);
                    systemEntry = documentStore.storeIfAbsent(systemMetadataKeyStr, newDoc);
                    if (systemEntry == null) {
                        // Read-read the entry that we just put, so we can populate it with the same code that edits it ...
                        systemEntry = documentStore.get(systemMetadataKeyStr);
                    }
                }
                EditableDocument doc = documentStore().localStore().edit(systemMetadataKeyStr, true);
                PropertyFactory propFactory = context().getPropertyFactory();
                translator.setProperty(doc, propFactory.create(name("workspaces"), workspaceNames), null, null);
                translator.setProperty(doc, propFactory.create(name("accessControl"), accessControlEnabled), null, null);

                return null;
            }, 2, REPOSITORY_INFO_KEY);
            return newWorkspaces;
        } catch (RuntimeException re) {
            LOGGER.error(JcrI18n.errorUpdatingRepositoryMetadata, name, re.getMessage());
            throw re;
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
        final boolean isSystemWorkspace = this.systemWorkspaceName.equals(name);
        if (!this.workspaceNames.contains(name) && !isSystemWorkspace) {
            throw new WorkspaceNotFoundException(name);
        }
     
        // We know that this workspace is not the system workspace, so find it ...
        final WorkspaceCache systemWorkspaceCache = isSystemWorkspace ? null : workspaceCachesByName.get(systemWorkspaceName);

        // multiple threads (e.g. re-indexing threads) could be performing ws cache initialization, so we want this to be atomic
        return workspaceCachesByName.computeIfAbsent(name, wsName ->  initializeCacheForWorkspace(wsName, systemWorkspaceCache));
    }
    
    private WorkspaceCache initializeCacheForWorkspace(String name, WorkspaceCache systemWorkspaceCache) {
        String workspaceKey = NodeKey.keyForWorkspaceName(name);
        NodeKey rootKey = new NodeKey(sourceKey, workspaceKey, rootNodeId);

        return localStore().runInTransaction(() -> {
            ConcurrentMap<NodeKey, CachedNode> nodeCache = cacheForWorkspace().asMap();
            ExecutionContext context = context();
            logger.debug("Attempting to initialize a new ws cache for workspace '{0}' in repository '{1}' with root key '{2}'", name, 
                         getName(), rootKey);
            // Create the root document for this workspace ...
            EditableDocument rootDoc = Schematic.newDocument();
            DocumentTranslator trans = new DocumentTranslator(context, documentStore, Long.MAX_VALUE);
            trans.setProperty(rootDoc,
                              context.getPropertyFactory().create(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.ROOT),
                              null, null);
            String rootKeyString = rootKey.toString();
            trans.setProperty(rootDoc, context.getPropertyFactory().create(JcrLexicon.UUID, rootKeyString),
                              null, null);

            WorkspaceCache result = new WorkspaceCache(context, getKey(), name, systemWorkspaceCache,
                                                       documentStore, translator, rootKey, nodeCache,
                                                       changeBus, repositoryEnvironment());

            if (documentStore.storeIfAbsent(rootKeyString, rootDoc) == null) {
                // we are the first node to perform the initialization (in a cluster), so we need to link the system node
                
                // we'll be doing this using low-level document edits (as opposed to using sessions) in order to avoid
                // any pseudo-recursive calls which might occur when using sessions and calling session.save which, in turn,
                // can fire events that lead back to this place (initializing a ws cache) especially when indexing is configured
                if (!systemWorkspaceName.equals(name)) {
                    logger.debug("Creating '{0}' workspace in repository '{1}' with root '{2}'", name, getName(), rootKey);
                     
                    rootDoc = documentStore.edit(rootKeyString, false); // we just placed a document, but need to edit this in place
                    
                    EditableDocument systemChildRefDoc = translator.childReferenceDocument(RepositoryCache.this.systemKey, JcrLexicon.SYSTEM);
                    rootDoc.setArray(DocumentTranslator.CHILDREN, Schematic.newArray(systemChildRefDoc));
                    EditableDocument childInfo = rootDoc.getOrCreateDocument(DocumentTranslator.CHILDREN_INFO);
                    childInfo.setNumber(DocumentTranslator.COUNT, 1);
                    
                    EditableDocument systemDoc = documentStore.edit(RepositoryCache.this.systemKey.toString(), false);
                    assert systemDoc != null;
                    String parent = systemDoc.getString(DocumentTranslator.PARENT);
                    EditableArray parents = systemDoc.getOrCreateArray(DocumentTranslator.PARENT);
                    if (parent != null) {
                        parents.add(parent);
                    }
                    parents.add(rootKeyString);
              }
            } 
            return result;
        }, 2, REPOSITORY_INFO_KEY);
    }
    
    protected Cache<NodeKey, CachedNode> cacheForWorkspace() {
        // make sure eviction runs in the same thread
        return Caffeine.newBuilder().maximumSize(workspaceCacheSize).executor(Runnable::run).build();
    }

    public final DocumentTranslator getDocumentTranslator() {
        return this.translator;
    }

    void removeWorkspaceCaches( String name ) {
        assert name != null;
        assert !this.workspaceNames.contains(name);
        WorkspaceCache removed = this.workspaceCachesByName.remove(name);
        if (removed != null) {
            removed.signalDeleted();
            txWorkspaceCaches.rollbackActiveTransactionsForWorkspace(name);
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
            localStore().runInTransaction(() -> {
                // unlink the system node
                removeSession.mutable(removeSession.getRootKey()).removeChild(removeSession, getSystemKey());
                // remove the workspace and persist it
                RepositoryCache.this.workspaceNames.remove(name);
                refreshRepositoryMetadata(true);
                // persist the active changes in the session
                removeSession.save();

                return null;
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
        
        String sourceName = tokens[0];
        String workspaceName = tokens[1];
        
        this.workspaceNames.add(workspaceName);
        refreshRepositoryMetadata(true);

        ConcurrentMap<NodeKey, CachedNode> nodeCache = cacheForWorkspace().asMap();
        ExecutionContext context = context();
        
        //the name of the external connector is used for source name and workspace name
        
        String sourceKey = NodeKey.keyForSourceName(sourceName);
        String workspaceKey = NodeKey.keyForWorkspaceName(workspaceName);

        //ask external system to determine root identifier.
        Connector connector = connectors.getConnectorForSourceName(sourceName); 
        if (connector == null) {
            throw new IllegalArgumentException(JcrI18n.connectorNotFound.text(sourceName));
        }
        FederatedDocumentStore documentStore = new FederatedDocumentStore(connectors, this.documentStore().localStore());
        String rootId = connector.getRootDocumentId();

        // Compute the root key for this workspace ...
        NodeKey rootKey = new NodeKey(sourceKey, workspaceKey, rootId);

        // We know that this workspace is not the system workspace, so find it ...
        final WorkspaceCache systemWorkspaceCache = workspaceCachesByName.get(systemWorkspaceName);
        
        WorkspaceCache workspaceCache = new WorkspaceCache(context, getKey(), 
                workspaceName, systemWorkspaceCache, documentStore, translator, rootKey, nodeCache, changeBus, repositoryEnvironment());
        workspaceCachesByName.put(workspaceName, workspaceCache);

        return workspace(workspaceName);
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
        if (readOnly) {
            return new ReadOnlySessionCache(context, workspaceCache);
        }
        return new WritableSessionCache(context, workspaceCache, txWorkspaceCaches, repositoryEnvironment);
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
        
        DocumentOptimizer optimizer = new DocumentOptimizer(documentStore());
        try {
            DocumentOperationResults results = documentStore().localStore().performOnEachDocument((key, document) -> 
                optimizer.optimizeChildrenBlocks(new NodeKey(key), document, targetCountPerBlock, tolerance)
            );
            sw.stop();
            logger.info(JcrI18n.completeChildrenOptimization, getName(), sw.getTotalDuration().toSimpleString(), results);
            return results;
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
        /**
         * Initializes the system part of the repository.
         *
         * @param session a {@link SessionCache} instance, never {@code null}
         * @param parent a {@link MutableCachedNode} instance under which the initialization should be done, never {@code null}
         */
        public void initializeSystemArea(SessionCache session, MutableCachedNode parent);
        
        /**
         * Initializes the system part of the repository that deals with index storage.
         *
         * @param session a {@link SessionCache} instance, never {@code null}
         * @param systemNode a {@link MutableCachedNode} instance, never {@code null}
         * @return {@code true} if the initialization was performed, {@code false} otherwise.
         */
        public boolean initializeIndexStorage(SessionCache session, MutableCachedNode systemNode);
    }
}
