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
package org.modeshape.jcr.spi.index.provider;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import javax.jcr.RepositoryException;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.function.Predicate;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.api.Logger;
import org.modeshape.jcr.api.index.IndexDefinition;
import org.modeshape.jcr.api.index.IndexDefinition.IndexKind;
import org.modeshape.jcr.cache.CachedNode.Properties;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.cache.change.Observable;
import org.modeshape.jcr.spi.index.Index;
import org.modeshape.jcr.spi.index.IndexConstraints;
import org.modeshape.jcr.spi.index.IndexDefinitionChanges;
import org.modeshape.jcr.spi.index.IndexFeedback;
import org.modeshape.jcr.spi.index.IndexManager;
import org.modeshape.jcr.spi.index.IndexWriter;
import org.modeshape.jcr.spi.index.WorkspaceChanges;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.ValueFactories;

/**
 * A component that provides access to and manages a set of {@link Index indexes} that are described/defined with
 * {@link IndexDefinition} objects via the session's {@link IndexManager}. This provider uses each {@link IndexDefinition} to
 * define and manage the {@link Index}es for each workspace identified by the definition. Much of this management is the same for
 * all providers, so this implementation provides much of that functionality. What each custom provider must do, then, is respond
 * to the {@link #createIndex}, {@link #updateIndex}, {@link #removeIndex} methods and return an appropriate {@link ManagedIndex}
 * object that behaves as described by the method parameters. While the {@link Index} represents the functionality needed by the
 * query engine, the {@link ManagedIndex} is the lowest-level and simplest interface to encapsulate the behavior of a single index
 * managed by this provider. for each Only basic functionality is provided, and subclasses specialize the behavior by overriding
 * several abstract methods.
 * <p>
 * Upon startup, the repository examines its configuration and sets up each of the index providers using this sequence:
 * <ol>
 * <li>Instantiation of a providers via no-arg constructor</li>
 * <li>Reflectively set each of the member fields of the provider instance, based upon the provider configuration's properties</li>
 * <li>Call the {@link #initialize()} method. This method is final and delegates to {@link #doInitialize()}, which can be
 * specialized for any implementation-specific startup behavior.</li>
 * <li>Notify this provider of all of its {@link IndexDefinition}s, whether they are defined in the configuration or persisted in
 * the repository's system area, by calling
 * {@link #notify(IndexDefinitionChanges, Observable, org.modeshape.jcr.NodeTypes.Supplier, Set, IndexFeedback)}. This method
 * processes all of the definition changes, and based upon the available workspaces, calls the {@link #createIndex},
 * {@link #updateIndex}, and {@link #removeIndex} methods for each index/workspace combination. Each of these methods can request
 * via the {@link IndexFeedback} method parameter that all or parts of the repository content be scanned and re-indexed. (The
 * repository does the scanning efficiently: if multiple providers request that the entire repository be scanned, the repository
 * will index the content only once.) Also, each {@link ManagedIndex} resulting from these methods will be given its own even
 * listener, and this is how the indexes are notified of changes in content (see below).
 * <li>
 * <li>If some content must be scanned, the repository will call {@link #getIndexWriter()} and use the {@link IndexWriter} to
 * notify the index(es) of the content.</li>
 * </ol>
 * After the repository is running, calls to {@link IndexManager} will alter the {@link IndexDefinition}s index definitions. As
 * these changes are persisted, the repository will call the
 * {@link #notify(IndexDefinitionChanges, Observable, org.modeshape.jcr.NodeTypes.Supplier, Set, IndexFeedback)} again, this time
 * with only the changes that were made to this provider's index definitions. If a new workspace is added or an existing workspace
 * is removed, ModeShape will called
 * {@link #notify(WorkspaceChanges, Observable, org.modeshape.jcr.NodeTypes.Supplier, Set, IndexFeedback)} with the relevant
 * information to allow the provider to respond.</li>
 * </p>
 * <p>
 * As sessions persist changes to content (via javax.jcr.Session#save() or by committing user transactions), the repository's
 * event bus will notify each {@link ManagedIndex} (via its {@link ManagedIndex#getIndexChangeAdapter() writing adapter}) of these
 * changes. This writing adapter is also used during manual re-indexing of content.
 * </p>
 * <p>
 * When queries are submitted, ModeShape will call {@link #getIndexPlanner()} to obtain the planner that should determine for each
 * portion of the query which indexes (if any) apply to that part of the query. When a particular index is to be used within a
 * query, ModeShape will then call {@link #getIndex(String,String)} and use it to answer the portion of the query.
 * </p>
 * <h2>Custom providers</h2>
 * <p>
 * Implementing custom providers is fairly straightforward: simply extend this class, provide implementations for the abstract
 * methods, and optionally override the default implementations for the other non-final methods.
 * </p>
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public abstract class IndexProvider {

    /**
     * The logger instance, set via reflection
     */
    private Logger logger;

    /**
     * The name of this provider, set via reflection
     */
    private String name;

    /**
     * The execution context, seet via reflection
     */
    private ExecutionContext context;

    /**
     * The name of the repository that owns this provider, set via reflection
     */
    private String repositoryName;

    /**
     * A flag that tracks whether {@link #initialize()} has been called.
     */
    private boolean initialized = false;

    /**
     * The {@link ProvidedIndex} instances (one in each applicable workspace) all keyed by the index name. Each
     * {@link ProvidedIndex} is a {@link ChangeSetListener} that is registered with the {@link Observable}, and which forwards
     * applicable {@link ChangeSet}s to the provider-supplied {@link ChangeSetListener listener}. It also maintains the
     * provider-supplied operations and a reference to the current {@link IndexDefinition}. These {@link ProvidedIndex}es are
     * managed entirely by this class, and updated based upon the {@link #createIndex}, {@link #updateIndex}, and
     * {@link #removeIndex} methods of the provider.
     */
    private final Map<String, Map<String, ProvidedIndex>> providedIndexesByWorkspaceNameByIndexName = new HashMap<>();

    protected final Logger logger() {
        return logger;
    }

    /**
     * Get the name for this provider.
     * 
     * @return the name; never null
     */
    public final String getName() {
        return name;
    }

    /**
     * Get the name of the repository.
     * 
     * @return the repository name; never null
     */
    public final String getRepositoryName() {
        return repositoryName;
    }

    /**
     * Get the context in which this provider executes. This is set prior to {@link #initialize() initialization} by ModeShape,
     * and never changed.
     * 
     * @return the execution context; never null
     */
    protected final ExecutionContext context() {
        return context;
    }

    /**
     * Initialize the provider. This is called automatically by ModeShape once for each provider instance, and should not be
     * called by the provider itself.
     * 
     * @throws RepositoryException if there is a problem initializing the provider
     */
    public synchronized final void initialize() throws RepositoryException {
        if (!initialized) {
            try {
                doInitialize();
                initialized = true;
            } catch (RuntimeException e) {
                throw new RepositoryException(e);
            }
        }
    }

    /**
     * Method that should do the provider-specific initialization work. This is called by ModeShape once for each provider
     * instance, and should not be called by the provider itself.
     * 
     * @throws RepositoryException if there is a problem initializing the provider
     */
    protected abstract void doInitialize() throws RepositoryException;

    /**
     * Signal this provider that it is no longer needed and can release any resources that are being held.
     * 
     * @throws RepositoryException if there is a problem shutting down the provider
     */
    public synchronized final void shutdown() throws RepositoryException {
        preShutdown();

        // Shutdown each of the provided indexes ...
        for (Map<String, ProvidedIndex> byWorkspaceName : providedIndexesByWorkspaceNameByIndexName.values()) {
            for (ProvidedIndex provided : byWorkspaceName.values()) {
                provided.shutdown(false);
            }
        }
        providedIndexesByWorkspaceNameByIndexName.clear();

        postShutdown();
    }

    /**
     * Method called immediately when #shutdown() is invoked, before any other operations are performed and before the managed
     * indexes are each shutdown.
     * 
     * @throws RepositoryException if there is a problem shutting down the provider
     */
    protected void preShutdown() throws RepositoryException {
        // Do nothing by default
    }

    /**
     * Method called during #shutdown() after each of the managed indexes have been shutdown.
     * 
     * @throws RepositoryException if there is a problem shutting down the provider
     */
    protected void postShutdown() throws RepositoryException {
        // Do nothing by default
    }

    /**
     * Get the writer that ModeShape can use to regenerate the indexes when a portion of the repository is to be re-indexed.
     * 
     * @return the index writer; may be null if the indexes are updated outside of ModeShape
     */
    public final IndexWriter getIndexWriter() {
        // Make a copy of the providers ...
        final Collection<ProvidedIndex> indexes = new ArrayList<>(providedIndexesByWorkspaceNameByIndexName.size());
        for (Map<String, ProvidedIndex> byWorkspaceNames : providedIndexesByWorkspaceNameByIndexName.values()) {
            for (ProvidedIndex index : byWorkspaceNames.values()) {
                indexes.add(index);
            }
        }
        return new IndexWriter() {
            @Override
            public boolean canBeSkipped() {
                return indexes.isEmpty();
            }

            @Override
            public void clearAllIndexes() {
                for (ProvidedIndex index : indexes) {
                    index.managed().removeAll();
                }
            }

            @Override
            public void add( String workspace,
                             NodeKey key,
                             Path path,
                             Name primaryType,
                             Set<Name> mixinTypes,
                             Properties properties ) {
                for (ProvidedIndex index : indexes) {
                    if (index.workspaceName().equals(workspace)) {
                        index.managed().getIndexChangeAdapter()
                             .index(workspace, key, path, primaryType, mixinTypes, properties, true);
                    }
                }
            }
        };
    }

    /**
     * Get the queryable index with the given name and applicable for the given workspace.
     * 
     * @param indexName the name of the index in this provider; never null
     * @param workspaceName the name of the workspace; never null
     * @return the queryable index, or null if there is no such index
     */
    public final Index getIndex( String indexName,
                                 String workspaceName ) {
        Map<String, ProvidedIndex> byWorkspaceNames = providedIndexesByWorkspaceNameByIndexName.get(indexName);
        return byWorkspaceNames == null ? null : byWorkspaceNames.get(workspaceName);
    }

    /**
     * Get the planner that, during the query planning/optimization phase, evaluates for a single source the AND-ed query
     * constraints and defines indexes that may be used.
     * <p>
     * This method is typically called only once after the provider has been {@link #initialize() initialized}.
     * </p>
     * 
     * @return the index planner; may not be null
     */
    public abstract IndexPlanner getIndexPlanner();

    /**
     * Get the namespace registry for the provider's {@link #context() execution context}, and which can be used to convert
     * qualified names (e.g., "{@code jcr:description}") to unqualified names (or vice versa).
     * 
     * @return the namespace registry; never null
     */
    protected final NamespaceRegistry namespaces() {
        return context.getNamespaceRegistry();
    }

    /**
     * Get the container for the type-specific factories useful to convert values that will be written to the index.
     * 
     * @return the container of type-specific value factories; never null
     */
    protected final ValueFactories valueFactories() {
        return context.getValueFactories();
    }

    protected final NameFactory names() {
        return valueFactories().getNameFactory();
    }

    /**
     * Signal that some workspaces were added or removed. This method is also called upon startup of this repository instance so
     * that the provider understands the index definitions that are available. The provider should adapt to these changes as best
     * as possible.
     * <p>
     * This method examines the supplied {@link WorkspaceChanges workspace additions and removals}, and updates the internal state
     * of this provider. It will then call the {@link #createIndex}, {@link #updateIndex}, or {@link #removeIndex} methods for
     * each of the affected indexes.
     * </p>
     * 
     * @param changes the changes in the workspaces; never null
     * @param observable the Observable object with which an index can register a listener for changes; this is the only mechanism
     *        by which the indexes can be updated
     * @param nodeTypesSupplier the supplier from which can be very efficiently obtained the latest {@link NodeTypes snapshot of
     *        node types} useful in determining if changes on a node are to be included in an index of a particular
     *        {@link IndexDefinition#getNodeTypeName() index node type}.
     * @param workspaceNames the names of all workspaces in this repository; never null and likely never null
     * @param feedback the feedback mechanism for this provider to signal to ModeShape that one or more indexes need to be
     *        entirely or partially rebuilt via scanning; never null
     */
    public synchronized final void notify( final WorkspaceChanges changes,
                                           Observable observable,
                                           NodeTypes.Supplier nodeTypesSupplier,
                                           final Set<String> workspaceNames,
                                           IndexFeedback feedback ) {
        for (IndexDefinition defn : changes.getIndexDefinitions()) {
            for (String workspaceName : changes.getAddedWorkspaces()) {
                if (defn.getWorkspaceMatchRule().usedInWorkspace(workspaceName)) {
                    // Add the index ...
                    try {
                        ManagedIndex managedIndex = createIndex(defn, workspaceName, nodeTypesSupplier, feedback);
                        Map<String, ProvidedIndex> managedIndexesByWorkspaceName = providedIndexesByWorkspaceNameByIndexName.get(defn.getName());
                        if (managedIndexesByWorkspaceName == null) {
                            managedIndexesByWorkspaceName = new HashMap<>();
                            providedIndexesByWorkspaceNameByIndexName.put(defn.getName(), managedIndexesByWorkspaceName);
                        }
                        ProvidedIndex index = new ProvidedIndex(defn, managedIndex, workspaceName);
                        managedIndexesByWorkspaceName.put(workspaceName, index);
                        observable.register(index);
                    } catch (RuntimeException e) {
                        String msg = "Error updating index '{0}' in workspace '{1}' with definition: {2}";
                        logger().error(e, msg, defn.getName(), workspaceName, defn);
                    }

                }
            }
            // Remove the managed indexes for workspaces that no longer exist ...
            Map<String, ProvidedIndex> managedIndexesByWorkspaceName = providedIndexesByWorkspaceNameByIndexName.get(defn.getName());
            if (managedIndexesByWorkspaceName.isEmpty()) continue;
            removeProvidedIndexes(managedIndexesByWorkspaceName, observable, new Predicate<ProvidedIndex>() {
                @Override
                public boolean test( ProvidedIndex index ) {
                    return !changes.getRemovedWorkspaces().contains(index.workspaceName());
                }
            });
        }
    }

    /**
     * Signal that some of the definitions of indexes owned by this provider were changed. This method is also called upon startup
     * of this repository instance so that the provider understands the index definitions that are available. The provider should
     * adapt to these changes as best as possible.
     * <p>
     * This method examines the supplied {@link IndexDefinitionChanges changes}, current workspaces, and maintains the internal
     * state of this provider. It will then call the {@link #createIndex}, {@link #updateIndex}, or {@link #removeIndex} methods
     * for each of the affected indexes.
     * </p>
     * 
     * @param changes the changes in the definitions; never null
     * @param observable the Observable object with which an index can register a listener for changes; this is the only mechanism
     *        by which the indexes can be updated
     * @param nodeTypesSupplier the supplier from which can be very efficiently obtained the latest {@link NodeTypes snapshot of
     *        node types} useful in determining if changes on a node are to be included in an index of a particular
     *        {@link IndexDefinition#getNodeTypeName() index node type}.
     * @param workspaceNames the names of all workspaces in this repository; never null and likely never null
     * @param feedback the feedback mechanism for this provider to signal to ModeShape that one or more indexes need to be
     *        entirely or partially rebuilt via scanning; never null
     */
    public synchronized final void notify( IndexDefinitionChanges changes,
                                           Observable observable,
                                           NodeTypes.Supplier nodeTypesSupplier,
                                           final Set<String> workspaceNames,
                                           IndexFeedback feedback ) {
        for (IndexDefinition defn : changes.getUpdatedIndexDefinitions().values()) {
            Map<String, ProvidedIndex> providedIndexesByWorkspaceName = providedIndexesByWorkspaceNameByIndexName.get(defn.getName());
            if (providedIndexesByWorkspaceName == null || providedIndexesByWorkspaceName.isEmpty()) {
                // There are no managed indexes for this index definition, so we know the index(es) will be new ...
                providedIndexesByWorkspaceName = new HashMap<>();
                for (String workspaceName : workspaceNames) {
                    if (defn.getWorkspaceMatchRule().usedInWorkspace(workspaceName)) {
                        // Add the index ...
                        try {
                            ManagedIndex managedIndex = createIndex(defn, workspaceName, nodeTypesSupplier, feedback);
                            ProvidedIndex index = new ProvidedIndex(defn, managedIndex, workspaceName);
                            providedIndexesByWorkspaceName.put(workspaceName, index);
                            observable.register(index);
                        } catch (RuntimeException e) {
                            String msg = "Error updating index '{0}' in workspace '{1}' with definition: {2}";
                            logger().error(e, msg, defn.getName(), workspaceName, defn);
                        }
                    }
                }
                if (!providedIndexesByWorkspaceName.isEmpty()) {
                    // At least one managed index applied to one of the workspaces ...
                    Object existing = providedIndexesByWorkspaceNameByIndexName.put(defn.getName(),
                                                                                    providedIndexesByWorkspaceName);
                    assert existing == null;
                }
            } else {
                // There is at least one existing managed index for this index definition, so figure out whether they should
                // be updated, created, or removed ...
                assert providedIndexesByWorkspaceName != null && !providedIndexesByWorkspaceName.isEmpty();
                for (String workspaceName : workspaceNames) {
                    ProvidedIndex provided = providedIndexesByWorkspaceName.get(workspaceName);
                    if (provided != null) {
                        if (defn.getWorkspaceMatchRule().usedInWorkspace(workspaceName)) {
                            // The index is updated and still applies to this workspace, so update the operations ...
                            try {
                                ManagedIndex managedIndex = updateIndex(provided.indexDefinition(), defn, provided.managed(),
                                                                        workspaceName, nodeTypesSupplier, feedback);
                                provided.update(managedIndex, defn);
                            } catch (RuntimeException e) {
                                String msg = "Error updating index '{0}' in workspace '{1}' with definition: {2}";
                                logger().error(e, msg, defn.getName(), workspaceName, defn);
                            }
                        } else {
                            // The existing managed index no longer applies to this workspace ...
                            removeProvidedIndex(provided, observable);
                        }
                    } else {
                        // There is no managed index yet, so add one ...
                        try {
                            ManagedIndex managedIndex = createIndex(defn, workspaceName, nodeTypesSupplier, feedback);
                            ProvidedIndex index = new ProvidedIndex(defn, managedIndex, workspaceName);
                            providedIndexesByWorkspaceName.put(workspaceName, index);
                            observable.register(index);
                        } catch (RuntimeException e) {
                            String msg = "Error adding index '{0}' in workspace '{1}' with definition: {2}";
                            logger().error(e, msg, defn.getName(), workspaceName, defn);
                        }
                    }
                }
                // Remove the managed indexes for workspaces that no longer exist ...
                removeProvidedIndexes(providedIndexesByWorkspaceName, observable, new Predicate<ProvidedIndex>() {
                    @Override
                    public boolean test( ProvidedIndex index ) {
                        return !workspaceNames.contains(index.workspaceName());
                    }
                });
            }
        }
        // Remove all of the managed indexes for REMOVED index definitions ...
        Predicate<ProvidedIndex> predicate = Predicate.always();
        for (String indexName : changes.getRemovedIndexDefinitions()) {
            removeProvidedIndexes(providedIndexesByWorkspaceNameByIndexName.get(indexName), observable, predicate);
        }
    }

    /**
     * Method called when this provider needs to create a new index given the unique pair of workspace name and index definition.
     * An index definition can apply to multiple workspaces, and when it does this method will be called once for each applicable
     * workspace.
     * 
     * @param defn the definition of the index; never null
     * @param workspaceName the name of the actual workspace to which the new index applies; never null
     * @param nodeTypesSupplier the supplier for the current node types cache; never null
     * @param feedback the feedback mechanism for this provider to signal to ModeShape that portions of the repository content
     *        must be scanned to build/populate the new index; never null
     * @return the implementation-specific {@link ManagedIndex} for the new index; may not be null
     */
    protected abstract ManagedIndex createIndex( IndexDefinition defn,
                                                 String workspaceName,
                                                 NodeTypes.Supplier nodeTypesSupplier,
                                                 IndexFeedback feedback );

    /**
     * Method called when this provider needs to update an existing index given the unique pair of workspace name and index
     * definition. An index definition can apply to multiple workspaces, and when it is changed this method will be called once
     * for each applicable workspace.
     * 
     * @param oldDefn the previous definition of the index; never null
     * @param updatedDefn the updated definition of the index; never null
     * @param existingIndex the existing index prior to this update, as returned from {@link #createIndex} or {@link #updateIndex}
     *        ; never null
     * @param workspaceName the name of the actual workspace to which the new index applies; never null
     * @param nodeTypesSupplier the supplier for the current node types cache; never null
     * @param feedback the feedback mechanism for this provider to signal to ModeShape that portions of the repository content
     *        must be scanned to rebuild/repopulate the updated index; never null
     * @return the operations and provider-specific state for this index; never null
     */
    protected abstract ManagedIndex updateIndex( IndexDefinition oldDefn,
                                                 IndexDefinition updatedDefn,
                                                 ManagedIndex existingIndex,
                                                 String workspaceName,
                                                 NodeTypes.Supplier nodeTypesSupplier,
                                                 IndexFeedback feedback );

    /**
     * Method called when this provider needs to remove an existing index given the unique pair of workspace name and index
     * definition. An index definition can apply to multiple workspaces, and when it does this method will be called once for each
     * applicable workspace.
     * 
     * @param oldDefn the previous definition of the index; never null
     * @param existingIndex the existing index prior to this update, as returned from {@link #createIndex} or {@link #updateIndex}
     *        ; never null
     * @param workspaceName the name of the actual workspace to which the new index applies; never null
     */
    protected abstract void removeIndex( IndexDefinition oldDefn,
                                         ManagedIndex existingIndex,
                                         String workspaceName );

    private void removeProvidedIndexes( Map<String, ProvidedIndex> managedIndexesByWorkspaceName,
                                        Observable observable,
                                        Predicate<ProvidedIndex> predicate ) {
        Iterator<Map.Entry<String, ProvidedIndex>> iter = managedIndexesByWorkspaceName.entrySet().iterator();
        while (iter.hasNext()) {
            ProvidedIndex index = iter.next().getValue();
            if (predicate.test(index)) {
                removeProvidedIndex(index, observable);
                iter.remove();
            }
        }

    }

    private void removeProvidedIndex( ProvidedIndex index,
                                      Observable observable ) {
        try {
            observable.unregister(index);
            removeIndex(index.indexDefinition(), index.managed(), index.workspaceName());
        } catch (RuntimeException e) {
            String msg = "Error removing index '{0}' in workspace '{1}' with definition: {2}";
            logger().error(e, msg, index.getName(), index.workspaceName(), index.indexDefinition());
        }
    }

    /**
     * This class is used within IndexProvider to keep a thread-safe object for each index. Even when the IndexDefinition for that
     * index is changed, the same instance will always associated with that definition/workspace pair. This is actually the
     * {@link Index} implementation exposed by the {@link IndexProvider#getIndex(String, String)} method, though it largely
     * delegates to the most current {@link ManagedIndex} instance created by the provider.
     * 
     * @author Randall Hauch (rhauch@redhat.com)
     */
    @ThreadSafe
    private class ProvidedIndex implements Index, ChangeSetListener {
        private final String workspaceName;
        private volatile ManagedIndex managedIndex;
        private volatile IndexDefinition defn;

        protected ProvidedIndex( IndexDefinition defn,
                                 ManagedIndex managedIndex,
                                 String workspaceName ) {
            this.defn = defn;
            this.managedIndex = managedIndex;
            this.workspaceName = workspaceName;
        }

        protected final IndexDefinition indexDefinition() {
            return defn;
        }

        protected final String workspaceName() {
            return workspaceName;
        }

        @Override
        public final String getProviderName() {
            return IndexProvider.this.getName();
        }

        @Override
        public final String getName() {
            return defn.getName();
        }

        @Override
        public boolean supportsFullTextConstraints() {
            return defn.getKind() == IndexKind.FULLTEXTSEARCH;
        }

        @Override
        public final Results filter( IndexConstraints constraints ) {
            return managedIndex.filter(constraints);
        }

        @Override
        public final void notify( ChangeSet changeSet ) {
            if (changeSet.getWorkspaceName() != null) {
                // This is a change in the content of a workspace ...
                managedIndex.getIndexChangeAdapter().notify(changeSet);
            }
        }

        protected ManagedIndex managed() {
            return managedIndex;
        }

        public void shutdown( boolean destroyed ) {
            managedIndex.shutdown(destroyed);
        }

        protected final void update( ManagedIndex managedIndex,
                                     IndexDefinition newDefinition ) {
            this.managedIndex = managedIndex;
            this.defn = defn;
        }
    }
}
