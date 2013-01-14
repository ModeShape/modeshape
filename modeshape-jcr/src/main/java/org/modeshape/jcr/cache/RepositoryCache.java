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
package org.modeshape.jcr.cache;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import javax.transaction.HeuristicMixedException;
import javax.transaction.HeuristicRollbackException;
import javax.transaction.NotSupportedException;
import javax.transaction.RollbackException;
import javax.transaction.SystemException;
import org.infinispan.Cache;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.collection.Collections;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.ModeShape;
import org.modeshape.jcr.ModeShapeLexicon;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.bus.ChangeBus;
import org.modeshape.jcr.cache.change.Change;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.cache.change.Observable;
import org.modeshape.jcr.cache.change.RecordingChanges;
import org.modeshape.jcr.cache.change.WorkspaceAdded;
import org.modeshape.jcr.cache.change.WorkspaceRemoved;
import org.modeshape.jcr.cache.document.DocumentStore;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.cache.document.LocalDocumentStore;
import org.modeshape.jcr.cache.document.ReadOnlySessionCache;
import org.modeshape.jcr.cache.document.WorkspaceCache;
import org.modeshape.jcr.cache.document.WritableSessionCache;
import org.modeshape.jcr.txn.Transactions;
import org.modeshape.jcr.txn.Transactions.Transaction;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.PropertyFactory;
import org.modeshape.jcr.value.ValueFactory;

/**
 *
 */
public class RepositoryCache implements Observable {

    private static final long MAX_NUMBER_OF_MINUTES_TO_WAIT_FOR_REPOSITORY_INITIALIZATION = 10;

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

    private final ExecutionContext context;
    private final RepositoryConfiguration configuration;
    private final DocumentStore documentStore;
    private final DocumentTranslator translator;
    private final ConcurrentHashMap<String, WorkspaceCache> workspaceCachesByName;
    private final AtomicLong minimumStringLengthForBinaryStorage = new AtomicLong();
    private final String name;
    private final String repoKey;
    private final String sourceKey;
    private final String rootNodeId;
    private final ChangeBus changeBus;
    private final NodeKey systemMetadataKey;
    private final NodeKey systemKey;
    private final Set<String> workspaceNames;
    private final String systemWorkspaceName;
    private final Logger logger;
    private final SessionEnvironment sessionContext;
    private final boolean createdSystemContent;
    private final CacheContainer workspaceCacheManager;
    private volatile boolean initializingRepository = false;

    public RepositoryCache( ExecutionContext context,
                            DocumentStore documentStore,
                            RepositoryConfiguration configuration,
                            ContentInitializer initializer,
                            SessionEnvironment sessionContext,
                            ChangeBus changeBus,
                            CacheContainer workspaceCacheContainer ) {
        this.context = context;
        this.configuration = configuration;
        this.documentStore = documentStore;
        this.minimumStringLengthForBinaryStorage.set(configuration.getBinaryStorage().getMinimumStringSize());
        this.translator = new DocumentTranslator(this.context, this.documentStore, this.minimumStringLengthForBinaryStorage.get());
        this.sessionContext = sessionContext;
        this.workspaceCacheManager = workspaceCacheContainer;
        this.logger = Logger.getLogger(getClass());
        this.rootNodeId = RepositoryConfiguration.ROOT_NODE_ID;
        this.name = configuration.getName();
        this.workspaceCachesByName = new ConcurrentHashMap<String, WorkspaceCache>();
        this.workspaceNames = new CopyOnWriteArraySet<String>(configuration.getAllWorkspaceNames());

        SchematicEntry repositoryInfo = this.documentStore.localStore().get(REPOSITORY_INFO_KEY);
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

            // Try to put it, but don't overwrite one that might have been stored since we checked ...
            this.documentStore.localStore().putIfAbsent(REPOSITORY_INFO_KEY, doc);
            repositoryInfo = this.documentStore.get(REPOSITORY_INFO_KEY);

            if (repositoryInfo.getContentAsDocument().getString(REPOSITORY_INITIALIZER_FIELD_NAME).equals(initializerId)) {
                // We're doing the initialization ...
                initializingRepository = true;
                LOGGER.debug("Initializing the '{0}' repository", name);
            }

        } else {
            // Get the repository key and source key from the repository info document ...
            Document info = repositoryInfo.getContentAsDocument();
            String repoName = info.getString(REPOSITORY_NAME_FIELD_NAME, this.name);
            String sourceName = info.getString(REPOSITORY_SOURCE_NAME_FIELD_NAME, configuration.getStoreName());
            this.repoKey = info.getString(REPOSITORY_KEY_FIELD_NAME, NodeKey.keyForSourceName(repoName));
            this.sourceKey = info.getString(REPOSITORY_SOURCE_KEY_FIELD_NAME, NodeKey.keyForSourceName(sourceName));
        }

        // If we're not doing the initialization of the repository, block for at most 5 minutes while another process does ...
        if (!initializingRepository) {
            final long numMinutesToWait = MAX_NUMBER_OF_MINUTES_TO_WAIT_FOR_REPOSITORY_INITIALIZATION;
            final long startTime = System.currentTimeMillis();
            final long quitTime = startTime + TimeUnit.MILLISECONDS.convert(numMinutesToWait, TimeUnit.MINUTES);
            boolean initialized = false;
            while (System.currentTimeMillis() < quitTime) {
                LOGGER.debug("Waiting for repository '{0}' to be fully initialized by another process in the cluster", name);
                Document info = repositoryInfo.getContentAsDocument();
                if (info.get(REPOSITORY_INITIALIZED_AT_FIELD_NAME) != null) {
                    initialized = true;
                    break;
                }
                // Otherwise, sleep for a short bit ...
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    Thread.interrupted();
                    break;
                }
            }

            if (!initialized) {
                LOGGER.error(JcrI18n.repositoryWasNeverInitializedAfterMinutes, name, numMinutesToWait);
                String msg = JcrI18n.repositoryWasNeverInitializedAfterMinutes.text(name, numMinutesToWait);
                throw new SystemFailureException(msg);
            }
            LOGGER.debug("Found repository '{0}' to be fully initialized", name);
        }

        this.systemWorkspaceName = RepositoryConfiguration.SYSTEM_WORKSPACE_NAME;
        String systemWorkspaceKey = NodeKey.keyForWorkspaceName(systemWorkspaceName);
        this.systemMetadataKey = new NodeKey(this.sourceKey, systemWorkspaceKey, SYSTEM_METADATA_IDENTIFIER);

        // Initialize the workspaces ..
        refreshWorkspaces(false);

        // this.eventBus = eventBus;
        this.changeBus = changeBus;
        // this.eventBus = new MultiplexingChangeSetListener();
        this.changeBus.register(new LocalChangeListener());

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
            this.createdSystemContent = true;
        } else {
            this.createdSystemContent = false;
            logger.debug("Found existing '{0}' workspace in repository '{1}'", systemWorkspaceName, name);
        }
        this.systemKey = systemRef.getKey();

        // set the local source key in the document store
        this.documentStore.setLocalSourceKey(this.sourceKey);
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    public final boolean isInitializingRepository() {
        return initializingRepository;
    }

    public void completeInitialization() {
        if (initializingRepository) {
            LOGGER.debug("Marking repository '{0}' as fully initialized", name);
            // Start a transaction ...
            Transactions txns = sessionContext.getTransactions();
            try {
                Transaction txn = txns.begin();
                try {
                    LocalDocumentStore store = this.documentStore.localStore();
                    store.prepareDocumentsForUpdate(Collections.unmodifiableSet(REPOSITORY_INFO_KEY));
                    SchematicEntry repositoryInfo = store.get(REPOSITORY_INFO_KEY);
                    EditableDocument editor = repositoryInfo.editDocumentContent();
                    if (editor.get(REPOSITORY_INITIALIZED_AT_FIELD_NAME) == null) {
                        DateTime now = context.getValueFactories().getDateFactory().create();
                        editor.setDate(REPOSITORY_INITIALIZED_AT_FIELD_NAME, now.toDate());
                    }
                } catch (RuntimeException e) {
                    txn.rollback();
                    throw e;
                }
                txn.commit();
                LOGGER.debug("Repository '{0}' is fully initialized", name);
            } catch (NotSupportedException err) {
                // No nested transactions are supported ...
                return;
            } catch (SecurityException err) {
                // No privilege to commit ...
                throw new SystemFailureException(err);
            } catch (IllegalStateException err) {
                // Not associated with a txn??
                throw new SystemFailureException(err);
            } catch (RollbackException err) {
                // Couldn't be committed, but the txn is already rolled back ...
                return;
            } catch (HeuristicMixedException err) {
            } catch (HeuristicRollbackException err) {
                // Rollback has occurred ...
                return;
            } catch (SystemException err) {
                // System failed unexpectedly ...
                throw new SystemFailureException(err);
            } finally {
                initializingRepository = false;
            }
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
            if (workspaceCacheManager instanceof EmbeddedCacheManager) {
                EmbeddedCacheManager embeddedCacheManager = (EmbeddedCacheManager)workspaceCacheManager;
                if (embeddedCacheManager.getStatus().equals(ComponentStatus.RUNNING)) {
                    ((EmbeddedCacheManager)workspaceCacheManager).removeCache(cacheNameForWorkspace(workspaceName));
                }
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

    @Override
    public boolean register( ChangeSetListener observer ) {
        return changeBus.register(observer);
    }

    @Override
    public boolean unregister( ChangeSetListener observer ) {
        return changeBus.unregister(observer);
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

    public boolean createdSystemContent() {
        return createdSystemContent;
    }

    protected void refreshWorkspaces( boolean update ) {
        // Read the node document ...
        DocumentTranslator translator = new DocumentTranslator(context, documentStore, minimumStringLengthForBinaryStorage.get());
        Set<String> workspaceNames = new HashSet<String>(this.workspaceNames);
        String systemMetadataKeyStr = this.systemMetadataKey.toString();
        SchematicEntry entry = documentStore.get(systemMetadataKeyStr);
        if (entry == null) {
            // it doesn't exist, so set it up ...
            PropertyFactory propFactory = context.getPropertyFactory();
            EditableDocument doc = Schematic.newDocument();
            translator.setKey(doc, systemMetadataKey);
            translator.setProperty(doc, propFactory.create(name("workspaces"), workspaceNames), null);
            entry = documentStore.localStore().putIfAbsent(systemMetadataKeyStr, doc);
            // we'll need to read the entry if one was inserted between 'containsKey' and 'putIfAbsent' ...
        }
        if (entry != null) {
            Document doc = entry.getContentAsDocument();
            Property prop = translator.getProperty(doc, name("workspaces"));
            if (prop != null && !prop.isEmpty() && !update) {
                workspaceNames.clear();
                ValueFactory<String> strings = context.getValueFactories().getStringFactory();
                for (Object value : prop) {
                    String workspaceName = strings.create(value);
                    workspaceNames.add(workspaceName);
                }
                this.workspaceNames.addAll(workspaceNames);
                this.workspaceNames.retainAll(workspaceNames);
            } else {
                // Set the property ...
                EditableDocument editable = entry.editDocumentContent();
                PropertyFactory propFactory = context.getPropertyFactory();
                translator.setProperty(editable, propFactory.create(name("workspaces"), workspaceNames), null);
                // we need to update local the cache immediately, so the changes are persisted
                documentStore.localStore().replace(systemMetadataKeyStr, editable);
            }
        }
    }

    /**
     * Executes the given operation only once, when the repository is created for the first time, using child node under
     * jcr:system as a global "lock". This should make the operation "cluster-friendly", where only the first node in the cluster
     * gets to execute the operation.
     * 
     * @param initOperation a {@code non-null} {@link Callable} instance
     * @throws Exception if anything unexpected occurs, clients are expected to handle this
     */
    public void runSystemOneTimeInitializationOperation( Callable<Void> initOperation ) throws Exception {
        SessionCache systemSession = createSession(context, systemWorkspaceName, false);
        MutableCachedNode systemNode = getSystemNode(systemSession);

        // look for the node which acts as a "global monitor"
        ChildReference repositoryReference = systemNode.getChildReferences(systemSession).getChild(ModeShapeLexicon.REPOSITORY);
        if (repositoryReference != null) {
            // should normally happen only in a clustered environment, when another node has committed/created the node
            CachedNode repositoryNode = systemSession.getNode(repositoryReference.getKey());
            Property statusProperty = repositoryNode.getProperty(ModeShapeLexicon.INITIALIZATION_STATE, systemSession);
            // if the node exists, the property should be there
            assert statusProperty != null;
            while (!ModeShapeLexicon.InitializationState.FINISHED.toString().equals(statusProperty.getFirstValue().toString())) {
                // some other node is executing the synchronized initialization so wait for it to finish
                LOGGER.debug("Waiting for another node to perform the initialization");
                Thread.sleep(500l);
                repositoryNode = systemSession.getNode(repositoryReference.getKey());
                statusProperty = repositoryNode.getProperty(ModeShapeLexicon.INITIALIZATION_STATE, systemSession);
            }
        } else {
            PropertyFactory propertyFactory = context.getPropertyFactory();

            // try to add the node and the status property to started
            // if another node is doing the same thing and none have committed yet, one of them will fail when the session.save
            // is committed, because there can be only one child of this type below jcr:system
            Property statusProperty = null;
            MutableCachedNode syncNode = null;
            try {
                Property primaryType = propertyFactory.create(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.REPOSITORY);
                statusProperty = propertyFactory.create(ModeShapeLexicon.INITIALIZATION_STATE,
                                                        ModeShapeLexicon.InitializationState.STARTED.toString());
                syncNode = systemNode.createChild(systemSession,
                                                  systemNode.getKey().withId("mode:synchronizedInitialization"),
                                                  ModeShapeLexicon.REPOSITORY,
                                                  primaryType,
                                                  statusProperty);
                systemSession.save();
            } catch (Exception e) {
                // an exception here should mean there's a DB conflict and another process has already managed to initialize
                LOGGER.warn(e, JcrI18n.errorDuringInitialInitialization);
                return;
            }

            try {
                // execute the operation
                // NOTE: this should always complete and ideally take a short amount of time. Otherwise, if this were to block all
                // the other nodes in a cluster would block
                initOperation.call();
            } finally {
                // set the status to finished so that other nodes aren't blocked. If something failed, it should be logged from
                // the outside
                statusProperty = propertyFactory.create(ModeShapeLexicon.INITIALIZATION_STATE,
                                                        ModeShapeLexicon.InitializationState.FINISHED.toString());
                NodeKey repositoryNodeKey = systemNode.getChildReferences(systemSession)
                                                      .getChild(ModeShapeLexicon.REPOSITORY)
                                                      .getKey();
                syncNode = systemSession.mutable(repositoryNodeKey);
                syncNode.setProperty(systemSession, statusProperty);
                systemSession.save();
            }
        }
    }

    /**
     * Runs a refresh operation, if and only if the one time initialization operation has finished successfully.
     * 
     * @param refreshOperation a {@code non-null} {@link Callable} instance
     * @throws Exception if anything unexpected occurs, clients are expected to handle this
     * @see RepositoryCache#runSystemOneTimeInitializationOperation(java.util.concurrent.Callable)
     */
    public void runSystemRefreshOperation( Callable<Void> refreshOperation ) throws Exception {
        SessionCache systemSession = createSession(context, systemWorkspaceName, false);
        MutableCachedNode systemNode = getSystemNode(systemSession);

        ChildReference repositoryReference = systemNode.getChildReferences(systemSession).getChild(ModeShapeLexicon.REPOSITORY);
        if (repositoryReference == null) {
            // refresh operations are only performed after the one time initialization has succeeded
            return;
        }
        CachedNode repositoryNode = systemSession.getNode(repositoryReference.getKey());
        Property statusProperty = repositoryNode.getProperty(ModeShapeLexicon.INITIALIZATION_STATE, systemSession);
        assert statusProperty != null;
        if (ModeShapeLexicon.InitializationState.FINISHED.name().equals(statusProperty.getString())) {
            refreshOperation.call();
        } else {
            LOGGER.debug("One time initialization not finished yet, ignoring refresh");
        }
    }

    private MutableCachedNode getSystemNode( SessionCache systemSession ) {
        NodeKey systemRootKey = systemSession.getRootKey();
        CachedNode systemRoot = systemSession.getNode(systemRootKey);
        ChildReference systemRef = systemRoot.getChildReferences(systemSession).getChild(JcrLexicon.SYSTEM);
        return systemSession.mutable(systemRef.getKey());
    }

    protected class LocalChangeListener implements ChangeSetListener {
        @Override
        public void notify( ChangeSet changeSet ) {

            if (changeSet == null || !getKey().equals(changeSet.getRepositoryKey())) {
                return;
            }
            String workspaceName = changeSet.getWorkspaceName();
            if (workspaceName != null) {
                for (WorkspaceCache cache : workspaces()) {
                    if (!cache.getWorkspaceName().equalsIgnoreCase(workspaceName)) {
                        // the workspace which triggered the event should've already processed the changeset, so we don't want to
                        // do it
                        cache.notify(changeSet);
                    }
                }
            } else {
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
                    }
                }
                if (changed) {
                    // The set of workspaces was changed, so refresh them now ...
                    refreshWorkspaces(false);

                    // And remove any already-cached workspaces. Note any open sessions to these workspaces will
                    for (String removedName : removedNames) {
                        removeWorkspace(removedName);
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

    WorkspaceCache workspace( String name ) {
        WorkspaceCache cache = workspaceCachesByName.get(name);
        if (cache == null) {
            if (!this.workspaceNames.contains(name) && !this.systemWorkspaceName.equals(name)) {
                throw new WorkspaceNotFoundException(name);
            }
            // Compute the root key for this workspace ...
            String workspaceKey = NodeKey.keyForWorkspaceName(name);
            NodeKey rootKey = new NodeKey(sourceKey, workspaceKey, rootNodeId);

            // Create the root document for this workspace ...
            EditableDocument rootDoc = Schematic.newDocument();
            DocumentTranslator trans = new DocumentTranslator(context, documentStore, Long.MAX_VALUE);
            trans.setProperty(rootDoc, context.getPropertyFactory().create(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.ROOT), null);
            trans.setProperty(rootDoc, context.getPropertyFactory().create(JcrLexicon.UUID, rootKey.toString()), null);

            documentStore.localStore().putIfAbsent(rootKey.toString(), rootDoc);

            // Create/get the Infinispan cache that we'll use within the WorkspaceCache, using the cache manager's
            // default configuration ...
            Cache<NodeKey, CachedNode> nodeCache = workspaceCacheManager.getCache(cacheNameForWorkspace(name));
            cache = new WorkspaceCache(context, getKey(), name, documentStore, translator, rootKey, nodeCache, changeBus);

            WorkspaceCache existing = workspaceCachesByName.putIfAbsent(name, cache);
            if (existing != null) {
                // Some other thread snuck in and created the cache for this workspace, so use it instead ...
                cache = existing;
            } else if (!this.systemWorkspaceName.equals(name)) {
                logger.debug("Creating '{0}' workspace in repository '{1}'", name, getName());
                // Link the system node to have this root as an additional parent ...
                SessionCache systemLinker = createSession(this.context, name, false);
                MutableCachedNode systemNode = systemLinker.mutable(systemLinker.getRootKey());
                systemNode.linkChild(systemLinker, this.systemKey, JcrLexicon.SYSTEM);
                systemLinker.save();
            }
        }
        return cache;
    }

    protected final String cacheNameForWorkspace( String workspaceName ) {
        return this.name + "/" + workspaceName;
    }

    public final DocumentTranslator getDocumentTranslator() {
        return this.translator;
    }

    void removeWorkspace( String name ) {
        assert name != null;
        assert !this.workspaceNames.contains(name);
        WorkspaceCache removed = this.workspaceCachesByName.remove(name);
        if (removed != null) {
            try {
                removed.signalDeleted();
            } finally {
                if (workspaceCacheManager instanceof EmbeddedCacheManager) {
                    ((EmbeddedCacheManager)workspaceCacheManager).removeCache(cacheNameForWorkspace(name));
                }
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
            refreshWorkspaces(true);

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
            RecordingChanges changes = new RecordingChanges(context.getId(), this.getKey());
            changes.workspaceAdded(name);
            changes.freeze(userId, userData, timestamp);
            this.changeBus.notify(changes);
        }
        return workspace(name);
    }

    /**
     * Permanently destroys the workspace with the supplied name, if the repository is appropriately configured. If no such
     * workspace exists in this repository, this method simply returns. Otherwise, this method attempts to destroy the named
     * workspace.
     * 
     * @param name the workspace name
     * @return true if the workspace with the supplied name existed and was destroyed, or false otherwise
     * @throws UnsupportedOperationException if this repository was not configured to allow
     *         {@link RepositoryConfiguration#isCreatingWorkspacesAllowed() creation (and destruction) of workspaces}.
     */
    public boolean destroyWorkspace( String name ) {
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
            // Otherwise, remove the workspace and persist it ...
            this.workspaceNames.remove(name);
            refreshWorkspaces(true);

            // And notify the others ...
            String userId = context.getSecurityContext().getUserName();
            Map<String, String> userData = context.getData();
            DateTime timestamp = context.getValueFactories().getDateFactory().create();
            RecordingChanges changes = new RecordingChanges(context.getId(), this.getKey());
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
     * Create a session for the workspace with the given name, using the supplied ExecutionContext for the session.
     * 
     * @param context the context for the new session; may not be null
     * @param workspaceName the name of the workspace; may not be null
     * @param readOnly true if the session is to be read-only
     * @return the new session that supports writes; never null
     * @throws WorkspaceNotFoundException if no such workspace exists
     */
    public SessionCache createSession( ExecutionContext context,
                                       String workspaceName,
                                       boolean readOnly ) {
        if (readOnly) {
            return new ReadOnlySessionCache(context, workspace(workspaceName), sessionContext);
        }
        return new WritableSessionCache(context, workspace(workspaceName), sessionContext);
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
