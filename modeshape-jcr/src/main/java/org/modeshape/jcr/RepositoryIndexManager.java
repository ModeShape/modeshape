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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.jcr.RepositoryException;
import org.infinispan.commons.util.ReflectionUtil;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.collection.Collections;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.RepositoryConfiguration.Component;
import org.modeshape.jcr.cache.SessionCache;
import org.modeshape.jcr.cache.change.Change;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.cache.change.NodeAdded;
import org.modeshape.jcr.cache.change.NodeRemoved;
import org.modeshape.jcr.cache.change.PropertyChanged;
import org.modeshape.jcr.query.CompositeIndexWriter;
import org.modeshape.jcr.query.engine.ScanningQueryEngine;
import org.modeshape.jcr.spi.index.IndexColumnDefinitionTemplate;
import org.modeshape.jcr.spi.index.IndexDefinition;
import org.modeshape.jcr.spi.index.IndexDefinitionChanges;
import org.modeshape.jcr.spi.index.IndexDefinitionTemplate;
import org.modeshape.jcr.spi.index.IndexExistsException;
import org.modeshape.jcr.spi.index.IndexManager;
import org.modeshape.jcr.spi.index.InvalidIndexDefinitionException;
import org.modeshape.jcr.spi.index.NoSuchIndexException;
import org.modeshape.jcr.spi.index.provider.IndexProvider;
import org.modeshape.jcr.spi.index.provider.IndexProviderExistsException;
import org.modeshape.jcr.spi.index.provider.IndexWriter;
import org.modeshape.jcr.spi.index.provider.NoSuchProviderException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.StringFactory;
import org.modeshape.jcr.value.ValueFactory;

/**
 * The {@link RepositoryIndexManager} is the maintainer of index definitions for the entire repository at run-time. The repository
 * index manager maintains an immutable view of all index definitions.
 */
@ThreadSafe
class RepositoryIndexManager implements ChangeSetListener, IndexManager {

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
                this.repository.error(t, JcrI18n.unableToInitializeSequencer, component, repository.name(), t.getMessage());
            }
        }
    }

    /**
     * Initialize this manager by calling {@link IndexProvider#initialize()} on each of the currently-registered providers.
     */
    protected synchronized void initialize() {
        if (initialized.get()) {
            // nothing to do ...
            return;
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
        refreshIndexWriter();
        initialized.set(true);
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
        provider.initialize();

        // If successful, call the 'postInitialize' method reflectively (due to inability to call directly) ...
        Method postInitialize = ReflectionUtil.findMethod(IndexProvider.class, "postInitialize");
        ReflectionUtil.invokeAccessibly(provider, postInitialize, new Object[] {});

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
     * Get the query index writer that will delegate to only those registered providers that also need to be
     * {@link IndexProvider#isReindexingRequired() reindexed}.
     * 
     * @return a query index writer instance; never null
     */
    IndexWriter getIndexWriterForProvidersNeedingReindexing() {
        List<IndexProvider> reindexProviders = new LinkedList<>();
        for (IndexProvider provider : providers.values()) {
            if (provider.isReindexingRequired()) {
                reindexProviders.add(provider);
            }
        }
        return CompositeIndexWriter.create(reindexProviders);
    }

    @Override
    public synchronized void register( IndexProvider provider ) throws RepositoryException {
        if (providers.containsKey(provider.getName())) {
            throw new IndexProviderExistsException("Index provider already exists with name '{0}'");
        }

        // Set the repository name field ...
        ReflectionUtil.setValue(provider, "repositoryName", repository.name());

        // Set the logger instance
        ReflectionUtil.setValue(provider, "logger", ExtensionLogger.getLogger(provider.getClass()));

        if (initialized.get()) {
            // This manager is already initialized, so we have to initialize the new provider ...
            doInitialize(provider);
        }

        // Do this last so that it doesn't show up in the list of providers before it's properly initialized ...
        IndexProvider existing = providers.putIfAbsent(provider.getName(), provider);
        if (existing != null) {
            throw new IndexProviderExistsException("Index provider already exists with name '{0}'");
        }
        refreshIndexWriter();
    }

    @Override
    public void unregister( String providerName ) throws RepositoryException {
        IndexProvider provider = providers.remove(providerName);
        if (provider == null) {
            throw new NoSuchProviderException("There is no index provider with the name '{0}'");
        }
        if (initialized.get()) {
            provider.shutdown();
        }
        refreshIndexWriter();
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
                                 boolean allowUpdate )
        throws InvalidIndexDefinitionException, IndexExistsException, RepositoryException {
        CheckArg.isNotNull(indexDefinitions, "indexDefinitions");
        SessionCache systemCache = repository.createSystemSession(context, false);
        SystemContent system = new SystemContent(systemCache);
        for (IndexDefinition defn : indexDefinitions) {
            String name = defn.getName();
            String providerName = defn.getProviderName();
            if (indexes.getIndexDefinitions().containsKey(name)) throw new IndexExistsException("The index '{0}' already exists");
            if (name == null) throw new InvalidIndexDefinitionException("The index name may not be null");
            if (providerName == null) throw new InvalidIndexDefinitionException("The index provider name may not be null");

            // Determine if the index should be enabled ...
            defn = RepositoryIndexDefinition.createFrom(defn, providers.containsKey(providerName));

            // Write the definition to the system area ...
            system.store(defn, allowUpdate);
        }

        // Refresh the immutable snapshot ...
        this.indexes = readIndexDefinitions();
    }

    @Override
    public void unregisterIndex( String indexName ) throws NoSuchIndexException, RepositoryException {
        IndexDefinition defn = indexes.getIndexDefinitions().get(indexName);
        if (defn == null) {
            throw new NoSuchIndexException("The index '{0}' does not exist");
        }

        // Remove the definition from the system area ...
        SessionCache systemCache = repository.createSystemSession(context, false);
        SystemContent system = new SystemContent(systemCache);
        system.remove(defn);

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
     * be usable when quering a specific selector (node type).
     * 
     * @return a snapshot of the index definitions at this moment; never null
     */
    public RepositoryIndexes getIndexes() {
        return indexes;
    }

    @Override
    public void notify( ChangeSet changeSet ) {
        if (!systemWorkspaceName.equals(changeSet.getWorkspaceName())) {
            // The change does not affect the 'system' workspace, so skip it ...
            return;
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
                        changeInfoForProvider(changesByProviderName, providerName).changed(indexName);
                    } else if (removedPath.size() > 3) {
                        // Adding an index (or column definition), but all we care about is the name of the index
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
            return;
        }
        // Refresh the index definitions ...
        RepositoryIndexes indexes = readIndexDefinitions();

        // And notify the affected providers ...
        StringFactory strings = context.getValueFactories().getStringFactory();
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
                provider.notify(changes);
            } catch (RuntimeException e) {
                logger.error(e, JcrI18n.errorRefreshingIndexDefinitions, repository.name());
            }
        }

        // Finally swap the snapshot of indexes ...
        this.indexes = indexes;
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

    protected RepositoryIndexes readIndexDefinitions() {
        // There were at least some changes ...
        NodeTypes nodeTypes = repository.nodeTypeManager().getNodeTypes();
        try {
            // Read the affected index definitions ...
            SessionCache systemCache = repository.createSystemSession(context, false);
            SystemContent system = new SystemContent(systemCache);
            Collection<IndexDefinition> indexDefns = system.readAllIndexDefinitions();
            return new Indexes(indexDefns, nodeTypes);
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

        protected Indexes( Collection<IndexDefinition> defns,
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
                Set<Name> nodeTypeNames = new HashSet<>();
                for (IndexDefinition defn : defns) {
                    indexByName.put(defn.getName(), defn);
                    // Determine all of the node types that are subtypes of any columns
                    nodeTypeNames.clear();
                    Name nodeTypeName = defn.getNodeTypeName();
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
}
