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
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicLong;
import javax.transaction.TransactionManager;
import org.infinispan.schematic.Schematic;
import org.infinispan.schematic.SchematicDb;
import org.infinispan.schematic.SchematicEntry;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableDocument;
import org.modeshape.common.collection.Collections;
import org.modeshape.common.util.Logger;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.ModeShapeLexicon;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.change.Change;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.cache.change.MultiplexingChangeSetListener;
import org.modeshape.jcr.cache.change.Observable;
import org.modeshape.jcr.cache.change.RecordingChanges;
import org.modeshape.jcr.cache.change.WorkspaceAdded;
import org.modeshape.jcr.cache.change.WorkspaceRemoved;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.cache.document.ReadOnlySessionCache;
import org.modeshape.jcr.cache.document.WorkspaceCache;
import org.modeshape.jcr.cache.document.WritableSessionCache;
import org.modeshape.jcr.core.ExecutionContext;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.PropertyFactory;
import org.modeshape.jcr.value.ValueFactory;

/**
 * 
 */
public class RepositoryCache implements Observable {

    private static final String SYSTEM_METADATA_IDENTIFIER = "jcr:system/mode:metadata";

    private final ExecutionContext context;
    private final RepositoryConfiguration configuration;
    private final SchematicDb database;
    private final ConcurrentHashMap<String, WorkspaceCache> workspaceCachesByName;
    private final AtomicLong minimumBinarySizeInBytes = new AtomicLong();
    private final String name;
    private final String repoKey;
    private final String sourceKey;
    private final String rootNodeId;
    private final TransactionManager txnMgr;
    private final MultiplexingChangeSetListener changeListener;
    private final NodeKey systemMetadataKey;
    private final NodeKey systemKey;
    private final Set<String> workspaceNames;
    private final String systemWorkspaceName;
    private final Logger logger;
    private final SessionCacheMonitor sessionCacheMonitor;

    public RepositoryCache( ExecutionContext context,
                            SchematicDb database,
                            RepositoryConfiguration configuration,
                            TransactionManager transactionManager,
                            ContentInitializer initializer,
                            SessionCacheMonitor monitor ) {
        this.context = context;
        this.configuration = configuration;
        this.database = database;
        this.minimumBinarySizeInBytes.set(configuration.getBinaryStorage().getMinimumBinarySizeInBytes());
        this.name = configuration.getName();
        this.repoKey = NodeKey.keyForSourceName(this.name);
        this.sourceKey = NodeKey.keyForSourceName(configuration.getStoreName());
        this.rootNodeId = RepositoryConfiguration.ROOT_NODE_ID;
        this.txnMgr = transactionManager;
        this.logger = Logger.getLogger(getClass());
        this.workspaceCachesByName = new ConcurrentHashMap<String, WorkspaceCache>();
        this.sessionCacheMonitor = monitor;

        // Initialize the workspaces ..
        this.systemWorkspaceName = RepositoryConfiguration.SYSTEM_WORKSPACE_NAME;
        String systemWorkspaceKey = NodeKey.keyForWorkspaceName(systemWorkspaceName);
        this.systemMetadataKey = new NodeKey(this.sourceKey, systemWorkspaceKey, SYSTEM_METADATA_IDENTIFIER);
        this.workspaceNames = new CopyOnWriteArraySet<String>(configuration.getAllWorkspaceNames());
        refreshWorkspaces(false);

        // TODO: Events : Change this to a real bus; in lieu of that, we'll process the requests locally ...
        this.changeListener = new MultiplexingChangeSetListener();
        this.changeListener.register(new LocalChangeListener());

        // Make sure the system workspace is configured to have a 'jcr:system' node ...
        SessionCache system = createSession(context, systemWorkspaceName, false);
        NodeKey systemRootKey = system.getRootKey();
        CachedNode systemRoot = system.getNode(systemRootKey);
        ChildReference systemRef = systemRoot.getChildReferences(system).getChild(JcrLexicon.SYSTEM);
        CachedNode systemNode = systemRef != null ? system.getNode(systemRef) : null;
        if (systemRef == null || systemNode == null) {
            logger.debug("Initializing the '{0}' workspace in repository '{1}'", systemWorkspaceName, name);
            // We have to create the initial "/jcr:system" content ...
            MutableCachedNode root = system.mutable(systemRootKey);
            if (initializer == null) initializer = NO_OP_INITIALIZER;
            initializer.initialize(system, root);
            system.save();
            // Look the node up again ...
            systemRoot = system.getNode(systemRootKey);
            systemRef = systemRoot.getChildReferences(system).getChild(JcrLexicon.SYSTEM);
        } else {
            logger.debug("Found existing '{0}' workspace in repository '{1}'", systemWorkspaceName, name);
        }
        this.systemKey = systemRef.getKey();
    }

    protected Name name( String name ) {
        return context.getValueFactories().getNameFactory().create(name);
    }

    @Override
    public boolean register( ChangeSetListener observer ) {
        return changeListener.register(observer);
    }

    @Override
    public boolean unregister( ChangeSetListener observer ) {
        return changeListener.unregister(observer);
    }

    public void setLargeValueSizeInBytes( long sizeInBytes ) {
        assert sizeInBytes > -1;
        minimumBinarySizeInBytes.set(sizeInBytes);
        for (WorkspaceCache workspaceCache : workspaceCachesByName.values()) {
            assert workspaceCache != null;
            workspaceCache.setMinimumBinarySizeInBytes(minimumBinarySizeInBytes.get());
        }
    }

    public long largeValueSizeInBytes() {
        return minimumBinarySizeInBytes.get();
    }

    protected void refreshWorkspaces( boolean update ) {
        // Read the node document ...
        DocumentTranslator translator = new DocumentTranslator(context, database, minimumBinarySizeInBytes.get());
        Set<String> workspaceNames = new HashSet<String>(this.workspaceNames);
        String systemMetadataKeyStr = this.systemMetadataKey.toString();
        SchematicEntry entry = database.get(systemMetadataKeyStr);
        if (entry == null) {
            // it doesn't exist, so set it up ...
            PropertyFactory propFactory = context.getPropertyFactory();
            EditableDocument doc = Schematic.newDocument();
            translator.setKey(doc, systemMetadataKey);
            translator.setProperty(doc, propFactory.create(name("workspaces"), workspaceNames), null);
            entry = database.putIfAbsent(systemMetadataKeyStr, doc, null);
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
            }
        }
    }

    protected class LocalChangeListener implements ChangeSetListener {
        @Override
        public void notify( ChangeSet changeSet ) {

            if (changeSet == null || !getKey().equals(changeSet.getRepositoryKey())) return;
            String workspaceName = changeSet.getWorkspaceName();
            if (workspaceName != null) {
                for (WorkspaceCache cache : workspaces()) {
                    cache.notify(changeSet);
                }
            } else {
                // Look for changes to the workspaces ...
                Set<String> removedNames = new HashSet<String>();
                boolean changed = false;
                for (Change change : changeSet) {
                    if (change instanceof WorkspaceAdded) {
                        String addedName = ((WorkspaceAdded)change).getWorkspaceName();
                        if (!getWorkspaceNames().contains(addedName)) changed = true;
                    } else if (change instanceof WorkspaceRemoved) {
                        String removedName = ((WorkspaceRemoved)change).getWorkspaceName();
                        removedNames.add(removedName);
                        if (getWorkspaceNames().contains(removedName)) changed = true;
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
            DocumentTranslator trans = new DocumentTranslator(context, database, Long.MAX_VALUE);
            trans.setProperty(rootDoc, context.getPropertyFactory().create(JcrLexicon.PRIMARY_TYPE, ModeShapeLexicon.ROOT), null);
            trans.setProperty(rootDoc, context.getPropertyFactory().create(JcrLexicon.UUID, rootKey.toString()), null);

            database.putIfAbsent(rootKey.toString(), rootDoc, null);
            cache = new WorkspaceCache(context, getKey(), name, database, minimumBinarySizeInBytes.get(), rootKey, changeListener);
            WorkspaceCache existing = workspaceCachesByName.putIfAbsent(name, cache);
            if (existing != null) {
                // Some other thread snuck in and created the cache for this workspace, so use it instead ...
                cache = existing;
            } else if (!this.systemWorkspaceName.equals(name)) {
                logger.debug("Initializing '{0}' workspace in repository '{1}'", name, getName());
                // Link the system node to have this root as an additional parent ...
                SessionCache systemLinker = createSession(this.context, name, false);
                MutableCachedNode systemNode = systemLinker.mutable(systemLinker.getRootKey());
                systemNode.linkChild(systemLinker, this.systemKey, JcrLexicon.SYSTEM);
                systemLinker.save();
            }
        }
        return cache;
    }

    void removeWorkspace( String name ) {
        assert name != null;
        assert !this.workspaceNames.contains(name);
        WorkspaceCache removed = this.workspaceCachesByName.remove(name);
        if (removed != null) removed.signalDeleted();
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
            this.changeListener.notify(changes);
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
            this.changeListener.notify(changes);
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
    public NodeCache getWorkspaceCache( String workspaceName ) {
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
            return new ReadOnlySessionCache(context, workspace(workspaceName), sessionCacheMonitor);
        }
        return new WritableSessionCache(context, workspace(workspaceName), txnMgr, sessionCacheMonitor);
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
