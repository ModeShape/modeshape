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

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.jcr.RepositoryException;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.collection.ArrayListMultimap;
import org.modeshape.common.collection.Collections;
import org.modeshape.common.collection.Multimap;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.collection.SimpleProblems;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.Reflection;
import org.modeshape.jcr.RepositoryConfiguration.Component;
import org.modeshape.jcr.api.index.IndexColumnDefinition;
import org.modeshape.jcr.api.index.IndexColumnDefinitionTemplate;
import org.modeshape.jcr.api.index.IndexDefinition;
import org.modeshape.jcr.api.index.IndexDefinition.IndexKind;
import org.modeshape.jcr.api.index.IndexDefinitionTemplate;
import org.modeshape.jcr.api.index.IndexExistsException;
import org.modeshape.jcr.api.index.InvalidIndexDefinitionException;
import org.modeshape.jcr.api.index.NoSuchIndexException;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.WorkspaceNotFoundException;
import org.modeshape.jcr.cache.change.Change;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.NodeAdded;
import org.modeshape.jcr.cache.change.NodeRemoved;
import org.modeshape.jcr.cache.change.PropertyChanged;
import org.modeshape.jcr.cache.change.WorkspaceAdded;
import org.modeshape.jcr.cache.change.WorkspaceRemoved;
import org.modeshape.jcr.query.CompositeIndexWriter;
import org.modeshape.jcr.query.engine.ScanningQueryEngine;
import org.modeshape.jcr.spi.index.IndexDefinitionChanges;
import org.modeshape.jcr.spi.index.IndexFeedback;
import org.modeshape.jcr.spi.index.IndexFeedback.IndexingCallback;
import org.modeshape.jcr.spi.index.IndexManager;
import org.modeshape.jcr.spi.index.IndexWriter;
import org.modeshape.jcr.spi.index.WorkspaceChanges;
import org.modeshape.jcr.spi.index.provider.IndexProvider;
import org.modeshape.jcr.spi.index.provider.IndexProviderExistsException;
import org.modeshape.jcr.spi.index.provider.ManagedIndex;
import org.modeshape.jcr.spi.index.provider.NoSuchProviderException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.StringFactory;
import org.modeshape.jcr.value.ValueFactory;

/**
 * The {@link RepositoryIndexManager} is the maintainer of index definitions for the entire repository at run-time. The repository
 * index manager maintains an immutable view of all index definitions.
 */
@ThreadSafe
class RepositoryIndexManager implements IndexManager, NodeTypes.Listener {

    /**
     * Names of properties that are known to have non-unique values when used in a single-valued index.
     */
    private static final Set<Name> NON_UNIQUE_PROPERTY_NAMES = Collections.unmodifiableSet(JcrLexicon.PRIMARY_TYPE,
                                                                                           JcrLexicon.MIXIN_TYPES,
                                                                                           JcrLexicon.PATH,
                                                                                           ModeShapeLexicon.DEPTH,
                                                                                           ModeShapeLexicon.LOCALNAME);
    /**
     * Names of properties that are known to have non-enumerated values when used in a single-valued index.
     */
    private static final Set<Name> NON_ENUMERATED_PROPERTY_NAMES = Collections.unmodifiableSet(JcrLexicon.PRIMARY_TYPE,
                                                                                               JcrLexicon.MIXIN_TYPES,
                                                                                               JcrLexicon.PATH,
                                                                                               ModeShapeLexicon.DEPTH,
                                                                                               ModeShapeLexicon.LOCALNAME);

    private final JcrRepository.RunningState repository;
    private final RepositoryConfiguration config;
    private final ExecutionContext context;
    private final String systemWorkspaceName;
    private final Path indexesPath;
    private final Collection<Component> components;
    private final ConcurrentMap<String, IndexProvider> providers = new ConcurrentHashMap<>();
    private final AtomicBoolean initialized = new AtomicBoolean(false);
    private volatile IndexWriter indexWriter;

    private final Logger logger = Logger.getLogger(getClass());
    private volatile RepositoryIndexes indexes = RepositoryIndexes.NO_INDEXES;

    RepositoryIndexManager( JcrRepository.RunningState repository,
                            RepositoryConfiguration config ) {
        this.repository = repository;
        this.config = config;
        this.context = repository.context();
        this.systemWorkspaceName = this.repository.repositoryCache().getSystemWorkspaceName();

        PathFactory pathFactory = this.context.getValueFactories().getPathFactory();
        this.indexesPath = pathFactory.createAbsolutePath(JcrLexicon.SYSTEM, ModeShapeLexicon.INDEXES);

        // Set up the index providers ...
        this.components = config.getIndexProviders();
        for (Component component : components) {
            try {
                IndexProvider provider = component.createInstance(ScanningQueryEngine.class.getClassLoader());
                register(provider);
            } catch (Throwable t) {
                if (t.getCause() != null) {
                    t = t.getCause();
                }
                this.repository.error(t, JcrI18n.unableToInitializeIndexProvider, component, repository.name(), t.getMessage());
            }
        }
    }

    /**
     * Initialize this manager by calling {@link IndexProvider#initialize()} on each of the currently-registered providers.
     *
     * @return the information about the portions of the repository that need to be scanned to (re)build indexes; null if no
     *         scanning is required
     */
    protected synchronized ScanningTasks initialize() {
        if (initialized.get()) {
            // nothing to do ...
            return null;
        }

        // Initialize each of the providers, removing any that are not properly initialized ...
        for (Iterator<Map.Entry<String, IndexProvider>> providerIter = providers.entrySet().iterator(); providerIter.hasNext();) {
            IndexProvider provider = providerIter.next().getValue();
            try {
                doInitialize(provider);
            } catch (Throwable t) {
                if (t.getCause() != null) {
                    t = t.getCause();
                }
                repository.error(t, JcrI18n.unableToInitializeIndexProvider, provider.getName(), repository.name(),
                                 t.getMessage());
                providerIter.remove();
            }
        }
        // Re-read the index definitions in case there were disabled index definitions that used the now-available provider ...
        RepositoryIndexes indexes = readIndexDefinitions();

        // Notify the providers of all the index definitions (which we'll treat as "new" since we're just starting up) ...
        ScanningTasks feedback = new ScanningTasks();
        for (Iterator<Map.Entry<String, IndexProvider>> providerIter = providers.entrySet().iterator(); providerIter.hasNext();) {
            IndexProvider provider = providerIter.next().getValue();
            if (provider == null) continue;
            final String providerName = provider.getName();

            IndexChanges changes = new IndexChanges();
            for (IndexDefinition indexDefn : indexes.getIndexDefinitions().values()) {
                if (!providerName.equals(indexDefn.getProviderName())) continue;
                changes.change(indexDefn);
            }
            // Even if there are no definitions, we still want to notify each of the providers ...
            try {
                provider.notify(changes, repository.changeBus(), repository.nodeTypeManager(), repository.repositoryCache()
                                                                                                         .getWorkspaceNames(),
                                feedback.forProvider(providerName));
            } catch (RuntimeException e) {
                logger.error(e, JcrI18n.errorNotifyingProviderOfIndexChanges, providerName, repository.name(), e.getMessage());
            }
        }

        // Refresh the index writer ...
        refreshIndexWriter();
        initialized.set(true);
        return feedback;
    }

    @Override
    public void notify( NodeTypes updatedNodeTypes ) {
        // Notify all of the providers about the change in node types ...
        for (IndexProvider provider : providers.values()) {
            provider.notify(updatedNodeTypes);
        }
    }

    synchronized void importIndexDefinitions() throws RepositoryException {
        RepositoryConfiguration.Indexes indexes = config.getIndexes();
        if (indexes.isEmpty()) return;
        List<IndexDefinition> defns = new ArrayList<>();
        for (String indexName : indexes.getIndexNames()) {
            IndexDefinition defn = indexes.getIndex(indexName);
            if (defn != null) defns.add(defn);
        }
        if (!defns.isEmpty()) {
            IndexDefinition[] array = defns.toArray(new IndexDefinition[defns.size()]);
            registerIndexes(array, true);
            // Wait while the indexes get created ...
            try {
                Thread.sleep(500L + array.length * 50L);
            } catch (Exception e) {
                throw new SystemFailureException(e);
            }
            // We have to index the '/jcr:system' content, since it was created before these indexes were registered ...
            repository.queryManager().reindexSystemContent();
        }
    }

    protected void refreshIndexWriter() {
        indexWriter = CompositeIndexWriter.create(providers.values());
    }

    /**
     * Initialize the supplied provider.
     *
     * @param provider the provider; may not be null
     * @throws RepositoryException if there is a problem initializing the provider
     */
    protected void doInitialize( IndexProvider provider ) throws RepositoryException {

        // Set the execution context instance ...
        Reflection.setValue(provider, "context", repository.context());
        
        // Set the environment
        Reflection.setValue(provider, "environment", repository.environment());

        provider.initialize();

        // If successful, call the 'postInitialize' method reflectively (due to inability to call directly) ...
        Method postInitialize = Reflection.findMethod(IndexProvider.class, "postInitialize");
        Reflection.invokeAccessibly(provider, postInitialize, new Object[] {});

        if (logger.isDebugEnabled()) {
            logger.debug("Successfully initialized index provider '{0}' in repository '{1}'", provider.getName(),
                         repository.name());
        }
    }

    void shutdown() {
        for (IndexProvider provider : providers.values()) {
            try {
                provider.shutdown();
            } catch (RepositoryException e) {
                logger.error(e, JcrI18n.errorShuttingDownIndexProvider, repository.name(), provider.getName(), e.getMessage());
            }
        }

    }

    /**
     * Get the query index writer that will delegate to all registered providers.
     *
     * @return the query index writer instance; never null
     */
    IndexWriter getIndexWriter() {
        return indexWriter;
    }

    /**
     * Get the query index writer that will delegate to only those registered providers with the given names.
     *
     * @param providerNames the names of the providers that require indexing
     * @return a query index writer instance; never null
     */
    IndexWriter getIndexWriterForProviders( Set<String> providerNames ) {
        List<IndexProvider> reindexProviders = new LinkedList<>();
        for (IndexProvider provider : providers.values()) {
            if (providerNames.contains(provider.getName())) {
                reindexProviders.add(provider);
            }
        }
        return CompositeIndexWriter.create(reindexProviders);
    }

    @Override
    public synchronized void register( IndexProvider provider ) throws RepositoryException {
        if (providers.containsKey(provider.getName())) {
            throw new IndexProviderExistsException(JcrI18n.indexProviderAlreadyExists.text(provider.getName(), repository.name()));
        }

        // Set the repository name field ...
        Reflection.setValue(provider, "repositoryName", repository.name());

        // Set the logger instance
        Reflection.setValue(provider, "logger", ExtensionLogger.getLogger(provider.getClass()));

        if (initialized.get()) {
            // This manager is already initialized, so we have to initialize the new provider ...
            doInitialize(provider);
        }

        // Do this last so that it doesn't show up in the list of providers before it's properly initialized ...
        IndexProvider existing = providers.putIfAbsent(provider.getName(), provider);
        if (existing != null) {
            throw new IndexProviderExistsException(JcrI18n.indexProviderAlreadyExists.text(provider.getName(), repository.name()));
        }

        // Re-read the index definitions in case there were disabled index definitions that used the now-available provider ...
        readIndexDefinitions();

        // Refresh the index writer ...
        refreshIndexWriter();
    }

    @Override
    public void unregister( String providerName ) throws RepositoryException {
        IndexProvider provider = providers.remove(providerName);
        if (provider == null) {
            throw new NoSuchProviderException(JcrI18n.indexProviderDoesNotExist.text(providerName, repository.name()));
        }
        if (initialized.get()) {
            provider.shutdown();
        }

        // Re-read the index definitions in case there were disabled index definitions that used the now-available provider ...
        readIndexDefinitions();

        // Refresh the index writer ...
        refreshIndexWriter();
    }

    @Override
    public IndexStatus getIndexStatus( String providerName, String indexName, String workspaceName ) {
        CheckArg.isNotNull(providerName, "providerName");
        CheckArg.isNotNull(indexName, "indexName");
        CheckArg.isNotNull(workspaceName, "workspaceName");
        
        IndexProvider provider = getProvider(providerName);
        if (provider == null) {
            return IndexStatus.NON_EXISTENT;
        }
        ManagedIndex managedIndex = provider.getManagedIndex(indexName, workspaceName);
        return managedIndex != null ? managedIndex.getStatus() : IndexStatus.NON_EXISTENT;
    }

    @Override
    public List<ManagedIndex> getIndexes(String providerName, String workspaceName, final IndexStatus status) {
        CheckArg.isNotNull(providerName, "providerName");
        CheckArg.isNotNull(workspaceName, "workspaceName");

        final List<ManagedIndex> result = new ArrayList<>();
        IndexProvider provider = getProvider(providerName);
        
        if (provider == null) {
            return result;
        }
        provider.onEachIndexInWorkspace(workspaceName, new IndexProvider.ManagedIndexOperation() {
            @Override
            public void apply( String workspaceName, ManagedIndex index, IndexDefinition defn ) {
                if (index.getStatus().equals(status)) {
                    result.add(index);
                }
            }
        });
        return result;
    }

    @Override
    public List<String> getIndexNames( String providerName, String workspaceName, final IndexStatus status ) {
        CheckArg.isNotNull(providerName, "providerName");
        CheckArg.isNotNull(workspaceName, "workspaceName");

        final List<String> result = new ArrayList<>();
        IndexProvider provider = getProvider(providerName);

        if (provider == null) {
            return result;
        }
        provider.onEachIndexInWorkspace(workspaceName, new IndexProvider.ManagedIndexOperation() {
            @Override
            public void apply( String workspaceName, ManagedIndex index, IndexDefinition defn ) {
                if (index.getStatus().equals(status)) {
                    result.add(defn.getName());
                }
            }
        });
        return result;
    }

    @Override
    public Set<String> getProviderNames() {
        return Collections.unmodifiableSet(new HashSet<>(providers.keySet()));
    }

    protected Iterable<IndexProvider> getProviders() {
        return new ArrayList<>(providers.values());
    }

    @Override
    public IndexProvider getProvider( String name ) {
        return providers.get(name);
    }

    @Override
    public Map<String, IndexDefinition> getIndexDefinitions() {
        return indexes.getIndexDefinitions();
    }

    @Override
    public IndexColumnDefinitionTemplate createIndexColumnDefinitionTemplate() {
        return new RepositoryIndexColumnDefinitionTemplate();
    }

    @Override
    public IndexDefinitionTemplate createIndexDefinitionTemplate() {
        return new RepositoryIndexDefinitionTemplate();
    }

    @Override
    public void registerIndex( IndexDefinition indexDefinition,
                               boolean allowUpdate )
        throws InvalidIndexDefinitionException, IndexExistsException, RepositoryException {
        registerIndexes(new IndexDefinition[] {indexDefinition}, allowUpdate);
    }

    @Override
    public void registerIndexes( IndexDefinition[] indexDefinitions,
                                 boolean allowUpdate ) throws InvalidIndexDefinitionException, IndexExistsException {
        CheckArg.isNotNull(indexDefinitions, "indexDefinitions");

        // Before we do anything, validate each of the index definitions and throw an exception ...
        RepositoryNodeTypeManager nodeTypeManager = repository.nodeTypeManager();
        List<IndexDefinition> validated = new ArrayList<>(indexDefinitions.length);
        Problems problems = new SimpleProblems();
        for (IndexDefinition defn : indexDefinitions) {
            String name = defn.getName();
            String providerName = defn.getProviderName();

            if (name == null) {
                problems.addError(JcrI18n.indexMustHaveName, defn, repository.name());
                continue;
            }
            if (indexes.getIndexDefinitions().containsKey(name) && !allowUpdate) {
                // Throw this one immediately ...
                String msg = JcrI18n.indexAlreadyExists.text(defn.getName(), repository.name());
                throw new IndexExistsException(msg);
            }
            if (providerName == null) {
                problems.addError(JcrI18n.indexMustHaveProviderName, defn.getName(), repository.name());
                continue;
            }
            if (defn.hasSingleColumn()) {
                IndexColumnDefinition columnDefn = defn.getColumnDefinition(0);
                Name propName = context.getValueFactories().getNameFactory().create(columnDefn.getPropertyName());
                switch (defn.getKind()) {
                    case UNIQUE_VALUE:
                        if (NON_UNIQUE_PROPERTY_NAMES.contains(propName)) {
                            problems.addError(JcrI18n.unableToCreateUniqueIndexForColumn, defn.getName(),
                                              columnDefn.getPropertyName());
                        }
                        break;
                    case ENUMERATED_VALUE:
                        if (NON_ENUMERATED_PROPERTY_NAMES.contains(propName)) {
                            problems.addError(JcrI18n.unableToCreateEnumeratedIndexForColumn, defn.getName(),
                                              columnDefn.getPropertyName());
                        }
                        break;
                    case VALUE:
                    case NODE_TYPE:
                    case TEXT:
                        break;
                }
            } else {
                // Mulitple columns ...
                if (defn.getKind() == IndexKind.NODE_TYPE) {
                    // must be single-column indexes
                    problems.addError(JcrI18n.nodeTypeIndexMustHaveOneColumn, defn.getName());
                }
            }
            IndexProvider provider = providers.get(providerName);
            if (provider == null) {
                problems.addError(JcrI18n.indexProviderDoesNotExist, defn.getName(), repository.name());
            } else {
                // Perform some default validations that should be applied to all providers... 
                provider.validateDefaultColumnTypes(context, defn, problems);
                
                // Then have the provider perform any custom validations
                provider.validateProposedIndex(context, defn, nodeTypeManager, problems);

                // Create an instance of our own definition implementation ...
                defn = RepositoryIndexDefinition.createFrom(defn, true);

                validated.add(defn);
            }
        }
        if (problems.hasErrors()) {
            String msg = JcrI18n.invalidIndexDefinitions.text(repository.name(), problems);
            throw new InvalidIndexDefinitionException(new JcrProblems(problems), msg);
        }

        SessionCache systemCache = repository.createSystemSession(context, false);
        SystemContent system = new SystemContent(systemCache);
        for (IndexDefinition defn : validated) {
            String providerName = defn.getProviderName();

            // Determine if the index should be enabled ...
            defn = RepositoryIndexDefinition.createFrom(defn, providers.containsKey(providerName));

            // Write the definition to the system area ...
            system.store(defn, allowUpdate);
        }
        // Save the changes ...
        systemCache.save();

        // Refresh the immutable snapshot ...
        this.indexes = readIndexDefinitions();
    }

    @Override
    public void unregisterIndexes( String... indexNames ) throws NoSuchIndexException, RepositoryException {
        if (indexNames == null || indexNames.length == 0) return;

        // Remove the definition from the system area ...
        SessionCache systemCache = repository.createSystemSession(context, false);
        SystemContent system = new SystemContent(systemCache);
        for (String indexName : indexNames) {
            IndexDefinition defn = indexes.getIndexDefinitions().get(indexName);
            if (defn == null) {
                throw new NoSuchIndexException(JcrI18n.indexDoesNotExist.text(indexName, repository.name()));
            }
            system.remove(defn);
        }
        system.save();

        // Refresh the immutable snapshot ...
        this.indexes = readIndexDefinitions();
    }

    RepositoryIndexManager with( JcrRepository.RunningState repository ) {
        return new RepositoryIndexManager(repository, config);
    }

    protected final ValueFactory<String> strings() {
        return this.context.getValueFactories().getStringFactory();
    }

    /**
     * Get an immutable snapshot of the index definitions. This can be used by the query engine to determine which indexes might
     * be usable when querying a specific selector (node type).
     *
     * @return a snapshot of the index definitions at this moment; never null
     */
    public RepositoryIndexes getIndexes() {
        return indexes;
    }

    protected ScanningTasks notify( ChangeSet changeSet ) {
        if (changeSet.getWorkspaceName() == null) {
            // This is a change to the workspaces or repository metadata ...

            // Refresh the index definitions ...
            RepositoryIndexes indexes = readIndexDefinitions();
            ScanningTasks feedback = new ScanningTasks();
            if (!indexes.getIndexDefinitions().isEmpty()) {
                // Build up the names of the added and removed workspace names ...
                Set<String> addedWorkspaces = new HashSet<>();
                Set<String> removedWorkspaces = new HashSet<>();
                for (Change change : changeSet) {
                    if (change instanceof WorkspaceAdded) {
                        WorkspaceAdded added = (WorkspaceAdded)change;
                        addedWorkspaces.add(added.getWorkspaceName());
                    } else if (change instanceof WorkspaceRemoved) {
                        WorkspaceRemoved removed = (WorkspaceRemoved)change;
                        removedWorkspaces.add(removed.getWorkspaceName());
                    }
                }
                if (!addedWorkspaces.isEmpty() || !removedWorkspaces.isEmpty()) {
                    // Figure out which providers need to be called, and which definitions go with those providers ...
                    Map<String, List<IndexDefinition>> defnsByProvider = new HashMap<>();
                    for (IndexDefinition defn : indexes.getIndexDefinitions().values()) {
                        String providerName = defn.getProviderName();
                        List<IndexDefinition> defns = defnsByProvider.get(providerName);
                        if (defns == null) {
                            defns = new ArrayList<>();
                            defnsByProvider.put(providerName, defns);
                        }
                        defns.add(defn);
                    }
                    // Then for each provider ...
                    for (Map.Entry<String, List<IndexDefinition>> entry : defnsByProvider.entrySet()) {
                        String providerName = entry.getKey();
                        WorkspaceIndexChanges changes = new WorkspaceIndexChanges(entry.getValue(), addedWorkspaces,
                                                                                  removedWorkspaces);
                        IndexProvider provider = providers.get(providerName);
                        if (provider == null) continue;
                        provider.notify(changes, repository.changeBus(), repository.nodeTypeManager(),
                                        repository.repositoryCache().getWorkspaceNames(), feedback.forProvider(providerName));
                    }
                }
            }
            return feedback;
        }
        if (!systemWorkspaceName.equals(changeSet.getWorkspaceName())) {
            // The change does not affect the 'system' workspace, so skip it ...
            return null;
        }

        // It is simple to listen to all local and remote changes. Therefore, any changes made locally to the index definitions
        // will be propagated through the cached representation via this listener.
        AtomicReference<Map<Name, IndexChangeInfo>> changesByProviderName = new AtomicReference<>();
        for (Change change : changeSet) {
            if (change instanceof NodeAdded) {
                NodeAdded added = (NodeAdded)change;
                Path addedPath = added.getPath();
                if (indexesPath.isAncestorOf(addedPath)) {
                    // Get the name of the affected provider ...
                    Name providerName = addedPath.getSegment(2).getName();
                    if (addedPath.size() > 3) {
                        // Adding an index (or column definition), but all we care about is the name of the index
                        Name indexName = addedPath.getSegment(3).getName();
                        changeInfoForProvider(changesByProviderName, providerName).changed(indexName);
                    }
                }
            } else if (change instanceof NodeRemoved) {
                NodeRemoved removed = (NodeRemoved)change;
                Path removedPath = removed.getPath();
                if (indexesPath.isAncestorOf(removedPath)) {
                    // Get the name of the affected provider ...
                    Name providerName = removedPath.getSegment(2).getName();
                    if (removedPath.size() > 4) {
                        // It's a column definition being removed, so the index is changed ...
                        Name indexName = removedPath.getSegment(3).getName();
                        changeInfoForProvider(changesByProviderName, providerName).removed(indexName);
                    } else if (removedPath.size() > 3) {
                        // Removing an index (or column definition), but all we care about is the name of the index
                        Name indexName = removedPath.getSegment(3).getName();
                        changeInfoForProvider(changesByProviderName, providerName).removed(indexName);
                    } else if (removedPath.size() == 3) {
                        // The whole provider was removed ...
                        changeInfoForProvider(changesByProviderName, providerName).removedAll();
                    }
                }
            } else if (change instanceof PropertyChanged) {
                PropertyChanged propChanged = (PropertyChanged)change;
                Path changedPath = propChanged.getPathToNode();
                if (indexesPath.isAncestorOf(changedPath)) {
                    if (changedPath.size() > 3) {
                        // Adding an index (or column definition), but all we care about is the name of the index
                        Name providerName = changedPath.getSegment(2).getName();
                        Name indexName = changedPath.getSegment(3).getName();
                        changeInfoForProvider(changesByProviderName, providerName).changed(indexName);
                    }
                }
            } // we don't care about node moves (don't happen) or property added/removed (handled by node add/remove)
        }

        if (changesByProviderName.get() == null || changesByProviderName.get().isEmpty()) {
            // No changes to the indexes ...
            return null;
        }
        // Refresh the index definitions ...
        RepositoryIndexes indexes = readIndexDefinitions();

        // And notify the affected providers ...
        StringFactory strings = context.getValueFactories().getStringFactory();
        ScanningTasks feedback = new ScanningTasks();
        for (Map.Entry<Name, IndexChangeInfo> entry : changesByProviderName.get().entrySet()) {
            String providerName = strings.create(entry.getKey());
            IndexProvider provider = providers.get(providerName);
            if (provider == null) continue;

            IndexChanges changes = new IndexChanges();
            IndexChangeInfo info = entry.getValue();
            if (info.removedAll) {
                // Get all of the definitions for this provider ...
                for (IndexDefinition defn : indexes.getIndexDefinitions().values()) {
                    if (defn.getProviderName().equals(providerName)) changes.remove(defn.getName());
                }
            }
            // Others might have been added or changed after the existing ones were removed ...
            for (Name name : info.removedIndexes) {
                changes.remove(strings.create(name));
            }
            for (Name name : info.changedIndexes) {
                IndexDefinition defn = indexes.getIndexDefinitions().get(strings.create(name));
                if (defn != null) changes.change(defn);
            }
            // Notify the provider ...
            try {
                provider.notify(changes, repository.changeBus(), repository.nodeTypeManager(), repository.repositoryCache()
                                                                                                         .getWorkspaceNames(),
                                feedback.forProvider(providerName));
            } catch (RuntimeException e) {
                logger.error(e, JcrI18n.errorNotifyingProviderOfIndexChanges, providerName, repository.name(), e.getMessage());
            }
        }

        // Finally swap the snapshot of indexes ...
        this.indexes = indexes;
        return feedback;
    }
    
    protected boolean hasProviders() {
        return !providers.isEmpty();
    }

    protected static IndexChangeInfo changeInfoForProvider( AtomicReference<Map<Name, IndexChangeInfo>> changesByProviderName,
                                                            Name providerName ) {
        Map<Name, IndexChangeInfo> byProviderName = changesByProviderName.get();
        if (byProviderName == null) {
            byProviderName = new HashMap<>();
            changesByProviderName.set(byProviderName);
        }
        IndexChangeInfo info = byProviderName.get(providerName);
        if (info == null) {
            info = new IndexChangeInfo();
            byProviderName.put(providerName, info);
        }
        return info;
    }

    protected static final class IndexChangeInfo {
        protected final Set<Name> changedIndexes = new HashSet<>();
        protected final Set<Name> removedIndexes = new HashSet<>();
        protected boolean removedAll = false;

        public void changed( Name indexName ) {
            changedIndexes.add(indexName);
            removedIndexes.remove(indexName);
        }

        public void removed( Name indexName ) {
            removedIndexes.add(indexName);
            changedIndexes.remove(indexName);
        }

        public void removedAll() {
            removedAll = true;
            removedIndexes.clear();
            changedIndexes.clear();
        }
    }

    protected static final class IndexChanges implements IndexDefinitionChanges {
        private final Set<String> removedDefinitions = new HashSet<>();
        private final Map<String, IndexDefinition> changedDefinitions = new HashMap<>();

        protected void remove( String name ) {
            removedDefinitions.add(name);
        }

        protected void change( IndexDefinition indexDefn ) {
            this.changedDefinitions.put(indexDefn.getName(), indexDefn);
        }

        @Override
        public Set<String> getRemovedIndexDefinitions() {
            return removedDefinitions;
        }

        @Override
        public Map<String, IndexDefinition> getUpdatedIndexDefinitions() {
            return changedDefinitions;
        }
    }

    protected static final class WorkspaceIndexChanges implements WorkspaceChanges {
        private final List<IndexDefinition> definitions;
        private final Set<String> addedWorkspaceNames;
        private final Set<String> removedWorkspaceNames;

        protected WorkspaceIndexChanges( List<IndexDefinition> defns,
                                         Set<String> addedWorkspaces,
                                         Set<String> removedWorkspaces ) {
            this.definitions = defns;
            this.addedWorkspaceNames = addedWorkspaces;
            this.removedWorkspaceNames = removedWorkspaces;
        }

        @Override
        public Collection<IndexDefinition> getIndexDefinitions() {
            return definitions;
        }

        @Override
        public Set<String> getAddedWorkspaces() {
            return addedWorkspaceNames;
        }

        @Override
        public Set<String> getRemovedWorkspaces() {
            return removedWorkspaceNames;
        }
    }

    protected RepositoryIndexes readIndexDefinitions() {
        // There were at least some changes ...
        NodeTypes nodeTypes = repository.nodeTypeManager().getNodeTypes();
        try {
            // Read the affected index definitions ...
            SessionCache systemCache = repository.createSystemSession(context, false);
            SystemContent system = new SystemContent(systemCache);
            Collection<IndexDefinition> indexDefns = system.readAllIndexDefinitions(providers.keySet());
            this.indexes = new Indexes(context, indexDefns, nodeTypes);
            return this.indexes;
        } catch (WorkspaceNotFoundException e) {
            // This happens occasionally when shutting down ...
        } catch (Throwable e) {
            logger.error(e, JcrI18n.errorRefreshingIndexDefinitions, repository.name());
        }
        return indexes;
    }

    /**
     * An immutable view of the indexes defined for the repository.
     *
     * @author Randall Hauch (rhauch@redhat.com)
     */
    @Immutable
    public static final class Indexes extends RepositoryIndexes {
        private final Map<String, IndexDefinition> indexByName = new HashMap<>();
        private final Map<String, Map<String, Collection<IndexDefinition>>> indexesByProviderByNodeTypeName = new HashMap<>();

        protected Indexes( ExecutionContext context,
                           Collection<IndexDefinition> defns,
                           NodeTypes nodeTypes ) {
            // Identify the subtypes for each node type, and do this before we build any views ...
            if (!defns.isEmpty()) {
                Map<Name, Collection<String>> subtypesByName = new HashMap<>();
                for (JcrNodeType nodeType : nodeTypes.getAllNodeTypes()) {
                    // For each of the supertypes ...
                    for (JcrNodeType supertype : nodeType.getTypeAndSupertypes()) {
                        Collection<String> types = subtypesByName.get(supertype.getInternalName());
                        if (types == null) {
                            types = new LinkedList<>();
                            subtypesByName.put(supertype.getInternalName(), types);
                        }
                        types.add(nodeType.getName());
                    }
                }

                // Now process all of the indexes ...
                NameFactory names = context.getValueFactories().getNameFactory();
                Set<Name> nodeTypeNames = new HashSet<>();
                for (IndexDefinition defn : defns) {

                    // Determine all of the node types that are subtypes of any columns
                    nodeTypeNames.clear();
                    Name nodeTypeName = names.create(defn.getNodeTypeName());

                    if (!subtypesByName.containsKey(nodeTypeName)){
                        Logger.getLogger(getClass()).warn(JcrI18n.errorIndexing,"not creating index "+ defn.getName() +" because of unknown nodeType " + nodeTypeName.getString());
                        continue;
                    }

                    indexByName.put(defn.getName(), defn);

                    // Now find out all of the node types that are or subtype the named node types ...
                    for (String typeAndSubtype : subtypesByName.get(nodeTypeName)) {
                        Map<String, Collection<IndexDefinition>> byProvider = indexesByProviderByNodeTypeName.get(typeAndSubtype);
                        if (byProvider == null) {
                            byProvider = new HashMap<>();
                            indexesByProviderByNodeTypeName.put(typeAndSubtype, byProvider);
                        }
                        Collection<IndexDefinition> indexes = byProvider.get(defn.getProviderName());
                        if (indexes == null) {
                            indexes = new LinkedList<>();
                            byProvider.put(typeAndSubtype, indexes);
                        }
                        indexes.add(defn);
                    }
                }
            }
        }

        @Override
        public boolean hasIndexDefinitions() {
            return !indexByName.isEmpty();
        }

        @Override
        public Map<String, IndexDefinition> getIndexDefinitions() {
            return java.util.Collections.unmodifiableMap(indexByName);
        }

        @Override
        public Iterable<IndexDefinition> indexesFor( String nodeTypeName,
                                                     String providerName ) {
            Map<String, Collection<IndexDefinition>> defnsByProvider = indexesByProviderByNodeTypeName.get(nodeTypeName);
            if (defnsByProvider == null) return null;
            return defnsByProvider.get(providerName);
        }
    }

    static interface ScanOperation {
        public void scan( String workspace,
                          Path path );
    }

    /**
     * An immutable set of provider names and non-overlapping workspace-path pairs.
     *
     * @author Randall Hauch (rhauch@redhat.com)
     */
    @Immutable
    static class ScanningRequest {

        protected static final ScanningRequest EMPTY = new ScanningRequest();

        private final Set<String> providerNames;
        private final Multimap<String, PathToScan> pathsToScanByWorkspace;

        protected ScanningRequest() {
            this.providerNames = java.util.Collections.emptySet();
            this.pathsToScanByWorkspace = ArrayListMultimap.create();
        }

        protected ScanningRequest( Multimap<String, PathToScan> pathsToScanByWorkspace,
                                   Set<String> providerNames ) {
            assert pathsToScanByWorkspace != null;
            assert providerNames != null;
            this.providerNames = Collections.unmodifiableSet(providerNames);
            this.pathsToScanByWorkspace = pathsToScanByWorkspace;
        }

        /**
         * Determine if this has no providers or workspace-path pairs.
         *
         * @return true if this request is empty, or false otherwise
         */
        public boolean isEmpty() {
            return providerNames.isEmpty();
        }

        /**
         * Scan each required path in each of the workspaces.
         *
         * @param operation the scanning operation that is to be called for each workspace & path combination; may not be null
         */
        public void onEachPathInWorkspace( ScanOperation operation ) {
            for (Map.Entry<String, PathToScan> entry : pathsToScanByWorkspace.entries()) {
                String workspaceName = entry.getKey();
                PathToScan pathToScan = entry.getValue();
                try {
                    for (IndexingCallback callback : pathToScan) {
                        try {
                            callback.beforeIndexing();
                        } catch (RuntimeException e) {
                            Logger.getLogger(getClass()).error(e, JcrI18n.errorIndexing, pathToScan.path(), workspaceName,
                                                               e.getMessage());
                        }
                    }
                    operation.scan(workspaceName, pathToScan.path());
                } catch (RuntimeException e) {
                    Logger.getLogger(getClass())
                          .error(e, JcrI18n.errorIndexing, pathToScan.path(), workspaceName, e.getMessage());
                } finally {
                    for (IndexingCallback callback : pathToScan) {
                        try {
                            callback.afterIndexing();
                        } catch (RuntimeException e) {
                            Logger.getLogger(getClass()).error(e, JcrI18n.errorIndexing, pathToScan.path(), workspaceName,
                                                               e.getMessage());
                        }
                    }
                }
            }
        }

        /**
         * Get the set of provider names that are to be included in the scanning.
         *
         * @return the provider names; never null but possibly empty if {@link #isEmpty()} returns true
         */
        public Set<String> providerNames() {
            return providerNames;
        }
    }

    private static class PathToScan implements Iterable<IndexingCallback> {
        private final Path path;
        private final Set<IndexingCallback> callbacks = new CopyOnWriteArraySet<>();

        protected PathToScan( Path path,
                              IndexingCallback callback ) {
            this.path = path;
            if (callback != null) this.callbacks.add(callback);
        }

        public void addCallbacks( PathToScan other ) {
            callbacks.addAll(other.callbacks);
        }

        public Path path() {
            return path;
        }

        @Override
        public int hashCode() {
            return path.hashCode();
        }

        @Override
        public boolean equals( Object obj ) {
            return path.equals(obj);
        }

        @Override
        public Iterator<IndexingCallback> iterator() {
            return callbacks.iterator();
        }
    }

    /**
     * Threadsafe utility class for maintaining the list of providers and workspace-path pairs that need to be scanned. Instances
     * can be safely combined using {@link #add(ScanningTasks)}, and immutable snapshots of the information can be obtained via
     * {@link #drain()} (which atomically empties the providers and workspace-path pairs into the immutable
     * {@link ScanningRequest}).
     *
     * @author Randall Hauch (rhauch@redhat.com)
     */
    @ThreadSafe
    static class ScanningTasks {
        private final Set<String> providerNames = new HashSet<>();
        private Multimap<String, PathToScan> pathsByWorkspaceName = ArrayListMultimap.create();

        /**
         * Add all of the provider names and workspace-path pairs from the supplied scanning task.
         *
         * @param other the other scanning task; may be null
         * @return true if there is at least one workspace-path pair and provider, or false if there are none
         */
        public synchronized boolean add( ScanningTasks other ) {
            if (other != null) {
                this.providerNames.addAll(other.providerNames);
                for (Map.Entry<String, PathToScan> entry : other.pathsByWorkspaceName.entries()) {
                    add(entry.getKey(), entry.getValue());
                }
            }
            return !this.providerNames.isEmpty();
        }

        /**
         * Atomically drain all of the provider names and workspace-path pairs from this object and return them in an immutable
         * {@link ScanningRequest}.
         *
         * @return the immutable set of provider names and workspace-path pairs; never null
         */
        public synchronized ScanningRequest drain() {
            if (this.providerNames.isEmpty()) return ScanningRequest.EMPTY;

            Set<String> providerNames = new HashSet<>(this.providerNames);
            Multimap<String, PathToScan> pathsToScanByWorkspace = this.pathsByWorkspaceName;
            this.pathsByWorkspaceName = ArrayListMultimap.create();
            this.providerNames.clear();
            return new ScanningRequest(pathsToScanByWorkspace, providerNames);
        }

        protected synchronized void add( String providerName,
                                         String workspaceName,
                                         Path path,
                                         IndexingCallback callback ) {
            assert providerName != null;
            assert workspaceName != null;
            assert path != null;
            providerNames.add(providerName);
            add(workspaceName, path, callback);
        }

        private void add( String workspaceName,
                          PathToScan pathToScan ) {
            Collection<PathToScan> pathsToScan = pathsByWorkspaceName.get(workspaceName);
            if (pathsToScan.isEmpty()) {
                pathsToScan.add(pathToScan);
            } else {
                Iterator<PathToScan> iter = pathsToScan.iterator();
                boolean add = true;
                final Path path = pathToScan.path();
                while (iter.hasNext()) {
                    PathToScan existing = iter.next();
                    Path existingPath = existing.path();
                    if (path.isAtOrAbove(existingPath)) {
                        // Remove all of the existing paths that are at or above this path (we'll add it back in ...)
                        iter.remove();
                        // But add all of the callbacks ...
                        pathToScan.addCallbacks(existing);
                    } else if (path.isDescendantOf(existingPath)) {
                        // The new path is a descendant of an existing path, so we can stop now and do nothing ...
                        add = false;
                        // But add all of the callbacks ...
                        existing.addCallbacks(pathToScan);
                        break;
                    }
                }
                if (add) pathsByWorkspaceName.put(workspaceName, pathToScan);
            }
        }

        private void add( String workspaceName,
                          Path path,
                          IndexingCallback callback ) {
            add(workspaceName, new PathToScan(path, callback));
        }

        /**
         * Obtain an {@link IndexFeedback} instance that can be used to gather feedback from the named provider.
         *
         * @param providerName the name of the index provider; may not be null
         * @return the custom IndexFeedback instance; never null
         */
        protected IndexFeedback forProvider( final String providerName ) {
            assert providerName != null;
            return new IndexFeedback() {

                @Override
                public void scan( String workspaceName,
                                  IndexingCallback callback ) {
                    add(providerName, workspaceName, Path.ROOT_PATH, callback);
                }

                @Override
                public void scan( String workspaceName,
                                  IndexingCallback callback,
                                  Path path ) {
                    add(providerName, workspaceName, path, callback);
                }
            };
        }

    }
}
