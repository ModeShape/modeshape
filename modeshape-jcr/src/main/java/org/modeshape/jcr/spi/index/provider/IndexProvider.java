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
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.RepositoryException;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.JoinCondition;
import org.modeshape.common.annotation.GuardedBy;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.collection.DelegateIterable;
import org.modeshape.common.collection.Problems;
import org.modeshape.common.function.Function;
import org.modeshape.common.function.Predicate;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.Environment;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.ModeShapeLexicon;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.api.Logger;
import org.modeshape.jcr.api.index.IndexColumnDefinition;
import org.modeshape.jcr.api.index.IndexDefinition;
import org.modeshape.jcr.api.index.IndexDefinition.IndexKind;
import org.modeshape.jcr.bus.ChangeBus;
import org.modeshape.jcr.cache.CachedNode.Properties;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetAdapter.NodeTypePredicate;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.cache.change.Observable;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.engine.NoOpQueryIndexWriter;
import org.modeshape.jcr.spi.index.Index;
import org.modeshape.jcr.spi.index.IndexConstraints;
import org.modeshape.jcr.spi.index.IndexCostCalculator;
import org.modeshape.jcr.spi.index.IndexDefinitionChanges;
import org.modeshape.jcr.spi.index.IndexFeedback;
import org.modeshape.jcr.spi.index.IndexManager;
import org.modeshape.jcr.spi.index.IndexWriter;
import org.modeshape.jcr.spi.index.WorkspaceChanges;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PropertyType;
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
 * {@link #notify(IndexDefinitionChanges, ChangeBus, org.modeshape.jcr.NodeTypes.Supplier, Set, IndexFeedback)}. This method
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
 * {@link #notify(IndexDefinitionChanges, ChangeBus, org.modeshape.jcr.NodeTypes.Supplier, Set, IndexFeedback)} again, this time
 * with only the changes that were made to this provider's index definitions. If a new workspace is added or an existing workspace
 * is removed, ModeShape will called
 * {@link #notify(WorkspaceChanges, ChangeBus, org.modeshape.jcr.NodeTypes.Supplier, Set, IndexFeedback)} with the relevant
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
     * The default number of rows in a batch.
     */
    public static final int DEFAULT_BATCH_SIZE = 100;
    
    private final static IndexWriter EMPTY_WRITER = NoOpQueryIndexWriter.INSTANCE;

    /**
     * The logger instance, set via reflection
     */
    private Logger logger;

    /**
     * The name of this provider, set via reflection
     */
    private String name;

    /**
     * The execution context, set via reflection
     */
    private ExecutionContext context;

    /**
     * The running environment, set via reflection
     */
    private Environment environment;

    /**
     * The name of the repository that owns this provider, set via reflection
     */
    private String repositoryName;

    /**
     * A flag that tracks whether {@link #initialize()} has been called.
     */
    private boolean initialized = false;

    /**
     * The index planner.
     */
    private final IndexPlanner planner;

    /**
     * The {@link AtomicIndex} instances (one in each applicable workspace) all keyed by the index name. Each
     * {@link AtomicIndex} is a {@link ChangeSetListener} that is registered with the {@link Observable}, and which forwards
     * applicable {@link ChangeSet}s to the provider-supplied {@link ChangeSetListener listener}. It also maintains the
     * provider-supplied operations and a reference to the current {@link IndexDefinition}. These {@link AtomicIndex}es are
     * managed entirely by this class, and updated based upon the {@link #createIndex}, {@link #updateIndex}, and
     * {@link #removeIndex} methods of the provider.
     */
    private final Map<String, Map<String, AtomicIndex>> providedIndexesByWorkspaceNameByIndexName = new HashMap<>();

    private final Map<String, Map<String, AtomicIndex>> providedIndexesByIndexNameByWorkspaceName = new HashMap<>();

    /**
     * An IndexWriter that does the work for this provider. This is {@link #refreshDelegateIndexWriter(org.modeshape.jcr.NodeTypes.Supplier) updated} every time the
     * {@link #providedIndexesByWorkspaceNameByIndexName provided indexes} are modified, and it is called by the
     * publicly-accessible {@link #publicWriter}. Never null.
     */
    private volatile IndexWriter delegateWriter = EMPTY_WRITER;
    
    @SuppressWarnings( "synthetic-access" )
    private final IndexWriter publicWriter = new IndexWriter() {
        @Override
        public boolean canBeSkipped() {
            return delegateWriter.canBeSkipped();
        }

        @Override
        public void clearAllIndexes() {
            delegateWriter.clearAllIndexes();
        }

        @Override
        public void add( String workspace,
                         NodeKey key,
                         Path path,
                         Name primaryType,
                         Set<Name> mixinTypes,
                         Properties properties ) {
            delegateWriter.add(workspace, key, path, primaryType, mixinTypes, properties);
        }

        @Override
        public void remove( String workspace, NodeKey key ) {
            delegateWriter.remove(workspace, key);
        }
    };

    protected IndexProvider() {
        this.planner = new BasicPlanner();
    }

    protected IndexProvider( IndexPlanner planner ) {
        assert planner != null;
        this.planner = planner;
    }

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
     * Get the default number of rows in a batch.
     *
     * @return the number of rows, should never be < 0;
     */
    public int batchSize() {
        return DEFAULT_BATCH_SIZE;
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
     * Get the repository's running environment
     * 
     * @return a {@link Environment} instance, never null.
     */
    protected final Environment environment() {
        return environment;
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
     * Method called by the code calling {@link #initialize} (typically via reflection) to signal that the initialize method is
     * completed. See initialize() for details, and no this method is indeed used.
     */
    @SuppressWarnings( "unused" )
    private void postInitialize() {
        if (!initialized) {
            initialized = true;

            // ------------------------------------------------------------------------------------------------------------
            // Add any code here that needs to run after #initialize(...), which will be overwritten by subclasses
            // ------------------------------------------------------------------------------------------------------------
        }
    }

    /**
     * Method that should do the provider-specific initialization work. This is called by ModeShape once each time the repository
     * is started; this method should not be called by the provider itself.
     * <p>
     * By the time this method is called, ModeShape will hav already set the {@link #context}, {@link #logger}, {@link #name}, and
     * {@link #repositoryName} plus any fields that match configuration properties for the provider.
     * </p>
     * <p>
     * This is an excellent place for providers to validate the provider-specific fields set by ModeShape via reflection during
     * instantiation.
     * </p>
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

        delegateWriter = NoOpQueryIndexWriter.INSTANCE;
        try {
            // Shutdown each of the provided indexes ...
            for (Map<String, AtomicIndex> byWorkspaceName : providedIndexesByWorkspaceNameByIndexName.values()) {
                for (AtomicIndex provided : byWorkspaceName.values()) {
                    provided.shutdown(false);
                }
            }
        } finally {
            providedIndexesByWorkspaceNameByIndexName.clear();
            providedIndexesByIndexNameByWorkspaceName.clear();
            postShutdown();
        }
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
     * Notify the provider that the NodeTypes have changed. This method should only be called by ModeShape and never called by
     * other code.
     *
     * @param updatedNodeTypes the new node types; may not be null
     */
    public void notify( final NodeTypes updatedNodeTypes ) {
        CheckArg.isNotNull(updatedNodeTypes, "updatedNodeTypes");
        // For each of the provided indexes ...
        onEachIndex(new ProvidedIndexOperation() {
            @Override
            public void apply( String workspaceName,
                               AtomicIndex index ) {
                @SuppressWarnings( "synthetic-access" )
                NodeTypeMatcher matcher = nodeTypePredicate(updatedNodeTypes, index.indexDefinition());
                index.update(index.managed(), index.indexDefinition(), matcher);
            }
        });
    }

    /**
     * Validate the proposed index definition, and use the supplied problems to report any issues that will prevent this provider
     * from creating and using an index with the given definition.
     * <p>
     * This should only be implemented if the provider has any particular validation requirements (for example it doesn't support
     * multiple columns or certain index types). The repository will perform for all index definitions some basic validations
     * via {@link #validateDefaultColumnTypes(ExecutionContext, IndexDefinition, Problems)}
     * </p>
     *   
     * @param context the execution context in which to perform the validation; never null
     * @param defn the proposed index definition; never null
     * @param nodeTypesSupplier the supplier for the NodeTypes object that contains information about the currently-registered
     *        node types; never null
     * @param problems the problems that should be used to report any issues with the index definition; never null
     */
    public void validateProposedIndex( ExecutionContext context,
                                       IndexDefinition defn,
                                       NodeTypes.Supplier nodeTypesSupplier,
                                       Problems problems ) {
        //nothing by default
    }

    /**
     * Validates that if certain default columns are present in the index definition, they have a required type.
     *
     * @param context the execution context in which to perform the validation; never null
     * @param defn the proposed index definition; never null
     * @param problems the component to record any problems, errors, or warnings; may not be null
     */
    public void validateDefaultColumnTypes( ExecutionContext context,
                                            IndexDefinition defn,
                                            Problems problems ) {
        assert defn != null;
        for (int i = 0; i < defn.size(); i++) {
            validateDefaultColumnDefinitionType(context, defn, defn.getColumnDefinition(i), problems);
        }
    }

    protected void validateDefaultColumnDefinitionType( ExecutionContext context,
                                                        IndexDefinition defn, 
                                                        IndexColumnDefinition columnDefn,
                                                        Problems problems) {
        int columnType = columnDefn.getColumnType();
        PropertyType type = PropertyType.valueFor(columnType);
        switch (defn.getKind()) {
            case UNIQUE_VALUE:
            case ENUMERATED_VALUE:    
            case VALUE:
                if ((matches(context, columnDefn, JcrLexicon.PATH) && !isType(type, PropertyType.PATH))) {
                    problems.addError(JcrI18n.indexMustHaveOneColumnOfSpecificType, defn.getProviderName(),
                                      defn.getName(), columnDefn.getPropertyName(), type, PropertyType.PATH);
                }
                if ((matches(context, columnDefn, ModeShapeLexicon.LOCALNAME) && !isType(type, PropertyType.STRING))
                    || (matches(context, columnDefn, ModeShapeLexicon.ID) && !isType(type, PropertyType.STRING))) {
                    problems.addError(JcrI18n.indexMustHaveOneColumnOfSpecificType, defn.getProviderName(),
                                      defn.getName(), columnDefn.getPropertyName(), type, PropertyType.STRING);
                }
                if ((matches(context, columnDefn, ModeShapeLexicon.DEPTH) && !isType(type, PropertyType.LONG))) {
                    problems.addError(JcrI18n.indexMustHaveOneColumnOfSpecificType, defn.getProviderName(),
                                      defn.getName(), columnDefn.getPropertyName(), type, PropertyType.LONG);
                }
                if ((matches(context ,columnDefn, JcrLexicon.PRIMARY_TYPE) && !isType(type, PropertyType.NAME))
                    || (matches(context, columnDefn, JcrLexicon.MIXIN_TYPES) && !isType(type, PropertyType.NAME))
                    || (matches(context, columnDefn, JcrLexicon.NAME) && !isType(type, PropertyType.NAME))) {
                    problems.addError(JcrI18n.indexMustHaveOneColumnOfSpecificType, defn.getProviderName(),
                                      defn.getName(), columnDefn.getPropertyName(), type, PropertyType.NAME);
                }
                break;
            case NODE_TYPE:
                if (PropertyType.STRING != type) {
                    problems.addError(JcrI18n.indexMustHaveOneColumnOfSpecificType, defn.getProviderName(),
                                      defn.getName(), columnDefn.getPropertyName(), type, PropertyType.STRING);
                }
                break;
            case TEXT: {
                if (PropertyType.STRING != type && PropertyType.BINARY != type && PropertyType.PATH != type &&
                    PropertyType.NAME != type) {
                    String expectedTypeMsg = PropertyType.STRING + " or " + PropertyType.BINARY;
                    problems.addError(JcrI18n.indexMustHaveOneColumnOfSpecificType, defn.getProviderName(),
                                      defn.getName(), columnDefn.getPropertyName(), type, expectedTypeMsg);                     
                }
                break;
            }
                
        }
    }

    protected final boolean matches( ExecutionContext context,
                                     IndexColumnDefinition defn,
                                     Name name ) {
        return defn.getPropertyName().equals(name.getString(context.getNamespaceRegistry()));
    }

    protected final boolean matches( ExecutionContext context,
                                     String actual,
                                     Name name ) {
        return actual.equals(name.getString(context.getNamespaceRegistry()));
    }

    protected final boolean isType( PropertyType propType,
                                    PropertyType expected ) {
        return propType == expected;
    }

    /**
     * Get the writer that ModeShape can use to regenerate the indexes when a portion of the repository is to be re-indexed.
     *
     * @return the index writer; may be null if the indexes are updated outside of ModeShape
     */
    public final IndexWriter getIndexWriter() {
        return publicWriter;
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
        logger().trace("Looking for index '{0}' in '{1}' provider for query in workspace '{2}'", indexName, getName(),
                       workspaceName);
        Map<String, AtomicIndex> byWorkspaceNames = providedIndexesByWorkspaceNameByIndexName.get(indexName);
        return byWorkspaceNames == null ? null : byWorkspaceNames.get(workspaceName);
    }
    
    /**
     * Get the managed index with the given name and applicable for the given workspace.
     *
     * @param indexName the name of the index in this provider; never null
     * @param workspaceName the name of the workspace; never null
     * @return the managed index, or null if there is no such index
     */
    public final ManagedIndex getManagedIndex( String indexName,
                                               String workspaceName ) {
        logger().trace("Looking for managed index '{0}' in '{1}' provider in workspace '{2}'", indexName, getName(),
                       workspaceName);
        Map<String, AtomicIndex> byWorkspaceNames = providedIndexesByWorkspaceNameByIndexName.get(indexName);
        if (byWorkspaceNames == null) {
            return null;
        }
        AtomicIndex atomicIndex = byWorkspaceNames.get(workspaceName);
        return atomicIndex == null ? null : atomicIndex.managed();
    }

    /**
     * Return the latest time at which this provider successfully updated its indexes.  
     * 
     * @return a {@code Long} instance which either contains the latest update time or {@code null} which means that
     * this provider does not support incremental reindexing.
     *
     */
    public Long getLatestIndexUpdateTime() {
        // by default don't support incremental reindexing
        return null;
    }

    /**
     * Get this provider's {@link AtomicIndex} instances for the given workspace.
     *
     * @param workspaceName the name of the workspace; may not be null
     * @return the iterator over the provided indexes; never null but possibly empty
     */
    protected final Iterable<ManagedIndex> getIndexes( String workspaceName ) {
        final Map<String, AtomicIndex> byIndexName = providedIndexesByIndexNameByWorkspaceName.get(workspaceName);
        if (byIndexName == null) return Collections.emptySet();
        return DelegateIterable.around(byIndexName.values(), new Function<AtomicIndex, ManagedIndex>() {
            @Override
            public ManagedIndex apply( AtomicIndex input ) {
                return input.managed();
            }
        });
    }

    /**
     * Perform the specified operation on each of the managed indexes.
     *
     * @param op the operation; may not be null
     */
    protected final void onEachIndex( ManagedIndexOperation op ) {
        for (String workspaceName : workspaceNames()) {
            onEachIndexInWorkspace(workspaceName, op);
        }
    }

    /**
     * Perform the specified operation on each of the managed indexes.
     *
     * @param op the operation; may not be null
     */
    private final void onEachIndex( ProvidedIndexOperation op ) {
        for (String workspaceName : workspaceNames()) {
            Collection<AtomicIndex> indexes = providedIndexesFor(workspaceName);
            if (indexes != null) {
                for (AtomicIndex atomicIndex : indexes) {
                    assert atomicIndex.managed() != null;
                    assert atomicIndex.indexDefinition() != null;
                    op.apply(workspaceName, atomicIndex);
                }
            }
        }
    }

    private synchronized Set<String> workspaceNames() {
        return new HashSet<String>(providedIndexesByIndexNameByWorkspaceName.keySet());
    }

    private synchronized Collection<AtomicIndex> providedIndexesFor( String workspaceName ) {
        Map<String, AtomicIndex> byIndexName = providedIndexesByIndexNameByWorkspaceName.get(workspaceName);
        if (byIndexName != null) {
            return new ArrayList<>(byIndexName.values());
        }
        return null;
    }

    /**
     * Perform the specified operation on each of the managed indexes in the named workspace.
     *
     * @param workspaceName the name of the workspace; may not be null
     * @param op the operation; may not be null
     */
    public final void onEachIndexInWorkspace( String workspaceName,
                                              ManagedIndexOperation op ) {
        assert workspaceName != null;
        Collection<AtomicIndex> indexes = providedIndexesFor(workspaceName);
        if (indexes != null) {
            for (AtomicIndex atomicIndex : indexes) {
                assert atomicIndex.managed() != null;
                assert atomicIndex.indexDefinition() != null;
                op.apply(workspaceName, atomicIndex.managed(), atomicIndex.indexDefinition());
            }
        }
    }

    /**
     * An operation that performs on a managed index with the associated index definition.
     *
     * @author Randall Hauch (rhauch@redhat.com)
     */
    public static interface ManagedIndexOperation {
        /**
         * Apply the operation to a managed index
         *
         * @param workspaceName the name of the workspace in which the index exists; may not be null
         * @param index the managed index instance; may not be null
         * @param defn the definition for the index
         */
        void apply( String workspaceName,
                    ManagedIndex index,
                    IndexDefinition defn );
    }

    /**
     * An operation that performs on a provided index with the associated index definition.
     *
     * @author Randall Hauch (rhauch@redhat.com)
     */
    private static interface ProvidedIndexOperation {
        /**
         * Apply the operation to a provided index
         *
         * @param workspaceName the name of the workspace in which the index exists; may not be null
         * @param index the managed index instance; may not be null
         */
        void apply( String workspaceName,
                    AtomicIndex index );
    }

    /**
     * An IndexPlanner that calls {@link IndexProvider#planUseOfIndex} on each applicable managed index.
     *
     * @author Randall Hauch (rhauch@redhat.com)
     */
    private final class BasicPlanner extends IndexPlanner {
        protected BasicPlanner() {
        }

        @Override
        public void applyIndexes( final QueryContext context,
                                  final IndexCostCalculator calculator ) {
            final ManagedIndexOperation planningOp = new ManagedIndexOperation() {
                @Override
                public void apply( String workspaceName,
                                   ManagedIndex index,
                                   IndexDefinition defn ) {
                    boolean traceEnabled = logger().isTraceEnabled();
                    if (!defn.isEnabled()) {
                        if (traceEnabled) {
                            logger().trace("Skipping index '{0}' in '{1}' provider because it is not enabled",
                                           defn.getName(), getName());
                        }
                        return;
                    }
                    if (!defn.getWorkspaceMatchRule().usedInWorkspace(workspaceName)) {
                        if (traceEnabled) {
                            logger().trace("Skipping index '{0}' in '{1}' provider for query because it doest not match the '{2}' workspace",  
                                           defn.getName(), getName(), workspaceName);
                        }
                        return;
                    }

                    if (matchesType(defn, calculator, context)) {
                        if (traceEnabled) {
                            logger().trace("Considering index '{0}' in '{1}' provider for query in workspace '{2}'",
                                           defn.getName(),
                                           getName(), 
                                           workspaceName);
                        }
                        planUseOfIndex(context, calculator, workspaceName, index, defn);
                    } else if (traceEnabled) {
                        logger().trace("Skipping index '{0}' in '{1}' provider for query because the index definition node type '{2}' does not match the selected node types '{3}'",
                                       defn.getName(), getName(), defn.getNodeTypeName(), calculator.selectedNodeTypes());
                    }
                }
            };
            for (String workspaceName : context.getWorkspaceNames()) {
                onEachIndexInWorkspace(workspaceName, planningOp);
            }
        }
    }

    private boolean matchesType( IndexDefinition defn,
                                 IndexCostCalculator calculator,
                                 QueryContext context ) {
        final NodeTypes nodeTypes = context.getNodeTypes();
        final NameFactory nameFactory = context.getExecutionContext().getValueFactories().getNameFactory();
        // check of the node type of the index defn matches the request node type(s) from the query
        String indexedNodeType = defn.getNodeTypeName();
        Name indexedNodeTypeName = nameFactory.create(indexedNodeType);
        Set<String> selectedNodeTypes = calculator.selectedNodeTypes();
        assert !selectedNodeTypes.isEmpty();
        for (String nodeTypeOrAlias : selectedNodeTypes) {
            Name selectedNodeTypeName = nameFactory.create(nodeTypeOrAlias);
            if (nodeTypes.isTypeOrSubtype(selectedNodeTypeName, indexedNodeTypeName)) {
                return true;
            }
        }
        return false;
    }

    /**
     * The method that is called by the IndexProvider's {@link IndexProvider#getIndexPlanner default IndexPlanner} for each
     * managed index in the given workspace.
     * <p>
     * Subclasses can implement this method to determine and record whether the supplied index can be used by the query against
     * the named workspace, but usually the default implementation should be good enough. Subclasses should really only implement
     * the {@link #evaluateUsage(QueryContext, IndexCostCalculator, IndexDefinition)} method.
     * </p>
     * The {@link IndexUsage} class is useful to determine for a given index definition whether criteria is applicable.
     *
     * @param context the context of the original query; never null
     * @param calculator the calculator that should be used to record plan information for the index; never null
     * @param workspaceName the name of the workspace against which the query is operating; never null
     * @param index the managed index in the given workspace; never null
     * @param defn the definition for the index; never null
     */
    protected void planUseOfIndex( QueryContext context,
                                   IndexCostCalculator calculator,
                                   String workspaceName,
                                   ManagedIndex index,
                                   IndexDefinition defn ) {
        // Ask the provider for an index usage component
        IndexUsage planner = evaluateUsage(context, calculator, defn);
        if (planner == null) {
            throw new UnsupportedOperationException("Providers should either override this method or the #evaluateUsage method");
        }
        
        // Does this index apply to any of the ANDed constraints?
        int costEstimate = getCostEstimate();
        List<Constraint> applicableConstraints = new ArrayList<>();
        for (Constraint constraint : calculator.andedConstraints()) {
            if (planner.indexAppliesTo(constraint)) {
                logger().trace("Index '{0}' in '{1}' provider applies to query in workspace '{2}' with constraint: {3}",
                               defn.getName(), getName(), workspaceName, constraint);
                // The index does apply to this constraint ...
                applicableConstraints.add(constraint);
            }
        }
        if (!applicableConstraints.isEmpty()) {
            long cardinality = index.estimateCardinality(applicableConstraints, context.getVariables());
            long total = index.estimateTotalCount();
            Float selectivity = null;
            if (total > 0L) {
                double ratio = (double)cardinality / (double)total;
                selectivity = cardinality <= total ? new Float(ratio) : IndexCostCalculator.MAX_SELECTIVITY;
            }
            calculator.addIndex(defn.getName(), workspaceName, getName(), applicableConstraints, costEstimate, cardinality, 
                                selectivity);
        }

        Collection<JoinCondition> joinConditions = calculator.joinConditions();
        if (joinConditions.isEmpty()) {
            return;
        }
        List<JoinCondition> applicableJoins = new ArrayList<>();
        // Does this index apply to any of the join conditions ...
        for (JoinCondition joinCondition : joinConditions) {
            if (planner.indexAppliesTo(joinCondition)) {
                logger().trace("Index '{0}' in '{1}' provider applies to query in workspace '{2}' with constraint: {3}",
                               defn.getName(), getName(), workspaceName, joinCondition);
                applicableJoins.add(joinCondition);
            }
        }
        
        if (!applicableJoins.isEmpty()) {
            // The index does apply to this constraint, but the number of values corresponds to the total number of values
            // in the index (this is a JOIN CONDITON for which there is no literal values) ...
            long total = index.estimateTotalCount();
            calculator.addIndex(defn.getName(), workspaceName, getName(), applicableJoins, costEstimate, total);
        }
    }

    /**
     * Returns an object which is used during the planning phase to evaluate if a certain index should be used or not.
     *
     * @param context the context of the original query; never null
     * @param calculator the calculator that should be used to record plan information for the index; never null
     * @param defn the definition for the index; never null
     * @return an {@link IndexUsage} instance, or {@code null} which is the default and which indicates that a provider chooses
     * to override the {@link #planUseOfIndex(QueryContext, IndexCostCalculator, String, ManagedIndex, IndexDefinition)} method
     * 
     * @see #planUseOfIndex(QueryContext, IndexCostCalculator, String, ManagedIndex, IndexDefinition) 
     */
    protected IndexUsage evaluateUsage( QueryContext context,
                                        IndexCostCalculator calculator,
                                        IndexDefinition defn ) {
        // this returns null for backwards compatibility reasons (otherwise it should've been abstract)
        return null;
    }

    /**
     * Returns a positive number representing the cost estimate of using indexes from this provider. The cost estimate is a
     * relative measure of how fast/complex the indexes of a provider are which should also take into account whether a provider
     * is local to the repository or not. 
     * 
     * Remote providers should always return a higher cost than local providers.
     * 
     * @return a positive integer
     * @see org.modeshape.jcr.spi.index.IndexCostCalculator.Costs
     */
    protected int getCostEstimate() {
        return IndexCostCalculator.Costs.LOCAL;
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
    public final IndexPlanner getIndexPlanner() {
        return planner;
    }

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
                                           ChangeBus observable,
                                           NodeTypes.Supplier nodeTypesSupplier,
                                           final Set<String> workspaceNames,
                                           IndexFeedback feedback ) {
        for (IndexDefinition defn : changes.getIndexDefinitions()) {
            for (String workspaceName : changes.getAddedWorkspaces()) {
                if (defn.getWorkspaceMatchRule().usedInWorkspace(workspaceName)) {
                    // Add the index ...
                    try {
                        NodeTypeMatcher matcher = nodeTypePredicate(nodeTypesSupplier.getNodeTypes(), defn);
                        ManagedIndex managedIndex = createIndex(defn, workspaceName, nodeTypesSupplier, matcher, feedback);
                        AtomicIndex index = new AtomicIndex(defn, managedIndex, workspaceName, matcher);
                        addProvidedIndex(index);
                        registerIndex(index, observable);
                    } catch (RuntimeException e) {
                        String msg = "Error updating index '{0}' in workspace '{1}' with definition: {2}";
                        logger().error(e, msg, defn.getName(), workspaceName, defn);
                    }

                }
            }
        }
        // Remove the managed indexes for workspaces that no longer exist ...
        removeProvidedIndexes(observable, new Predicate<AtomicIndex>() {
            @Override
            public boolean test( AtomicIndex index ) {
                return !changes.getRemovedWorkspaces().contains(index.workspaceName());
            }
        });
        refreshDelegateIndexWriter(nodeTypesSupplier);
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
    public synchronized final void notify( final IndexDefinitionChanges changes,
                                           ChangeBus observable,
                                           NodeTypes.Supplier nodeTypesSupplier,
                                           final Set<String> workspaceNames,
                                           IndexFeedback feedback ) {
        for (IndexDefinition defn : changes.getUpdatedIndexDefinitions().values()) {
            Map<String, AtomicIndex> providedIndexesByWorkspaceName = providedIndexesByWorkspaceNameByIndexName.get(defn.getName());
            if (providedIndexesByWorkspaceName == null || providedIndexesByWorkspaceName.isEmpty()) {
                // There are no managed indexes for this index definition, so we know the index(es) will be new ...
                for (String workspaceName : workspaceNames) {
                    if (defn.getWorkspaceMatchRule().usedInWorkspace(workspaceName)) {
                        // Add the index ...
                        try {
                            NodeTypeMatcher matcher = nodeTypePredicate(nodeTypesSupplier.getNodeTypes(), defn);
                            ManagedIndex managedIndex = createIndex(defn, workspaceName, nodeTypesSupplier, matcher, feedback);
                            AtomicIndex index = new AtomicIndex(defn, managedIndex, workspaceName, matcher);
                            addProvidedIndex(index);
                            registerIndex(index, observable);
                        } catch (RuntimeException e) {
                            String msg = "Error updating index '{0}' in workspace '{1}' with definition: {2}";
                            logger().error(e, msg, defn.getName(), workspaceName, defn);
                        }
                    }
                }
            } else {
                // There is at least one existing managed index for this index definition, so figure out whether they should
                // be updated, created, or removed ...
                assert providedIndexesByWorkspaceName != null && !providedIndexesByWorkspaceName.isEmpty();
                for (String workspaceName : workspaceNames) {
                    AtomicIndex provided = providedIndexesByWorkspaceName.get(workspaceName);
                    if (provided != null) {
                        if (defn.getWorkspaceMatchRule().usedInWorkspace(workspaceName)) {
                            IndexDefinition oldDefn = provided.indexDefinition();
                            if (!isChanged(oldDefn, defn)) {
                                // Nothing about the index definition that we care about really changed, so don't do anything ...
                                logger().debug("Index provider '{0}' is not updating index in workspace '{1}' because there were no changes: {2}",
                                               getName(), workspaceName, defn);
                                continue;
                            }
                            
                            // The index is updated and still applies to this workspace, so update the operations ...
                            try {
                                NodeTypeMatcher matcher = nodeTypePredicate(nodeTypesSupplier.getNodeTypes(), defn);
                                ManagedIndex managedIndex = updateIndex(oldDefn, defn, provided.managed(),
                                                                        workspaceName, nodeTypesSupplier, matcher, feedback);
                                provided.update(managedIndex, defn, matcher);
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
                            NodeTypeMatcher matcher = nodeTypePredicate(nodeTypesSupplier.getNodeTypes(), defn);
                            ManagedIndex managedIndex = createIndex(defn, workspaceName, nodeTypesSupplier, matcher, feedback);
                            AtomicIndex index = new AtomicIndex(defn, managedIndex, workspaceName, matcher);
                            addProvidedIndex(index);
                            registerIndex(index, observable);
                        } catch (RuntimeException e) {
                            String msg = "Error adding index '{0}' in workspace '{1}' with definition: {2}";
                            logger().error(e, msg, defn.getName(), workspaceName, defn);
                        }
                    }
                }
                // Remove the managed indexes for workspaces that no longer exist ...
                removeProvidedIndexes(observable, new Predicate<AtomicIndex>() {
                    @Override
                    public boolean test( AtomicIndex index ) {
                        return !workspaceNames.contains(index.workspaceName());
                    }
                });
            }
        }
        // Remove all of the managed indexes for REMOVED index definitions ...
        removeProvidedIndexes(observable, new Predicate<AtomicIndex>() {
            @Override
            public boolean test( AtomicIndex index ) {
                return changes.getRemovedIndexDefinitions().contains(index.getName());
            }
        });
        refreshDelegateIndexWriter(nodeTypesSupplier);
    }

    private boolean isChanged( IndexDefinition defn1,
                               IndexDefinition defn2 ) {
        if (defn1.getKind() != defn2.getKind()) return true;
        if (defn1.size() != defn2.size()) return true;
        for (int i = 0; i != defn1.size(); ++i) {
            IndexColumnDefinition col1 = defn1.getColumnDefinition(i);
            IndexColumnDefinition col2 = defn2.getColumnDefinition(i);
            if (isChanged(col1, col2)) return true;
        }
        // We don't care about any properties ...
        return false;
    }

    private boolean isChanged( IndexColumnDefinition defn1,
                               IndexColumnDefinition defn2 ) {
        if (defn1.getColumnType() != defn2.getColumnType()) return true;
        if (!defn1.getPropertyName().equals(defn2.getPropertyName())) return true;
        return false;
    } 

    @GuardedBy( "this" )
    private void addProvidedIndex( AtomicIndex index ) {
        String indexName = index.getName();
        String workspaceName = index.workspaceName();
        Map<String, AtomicIndex> managedIndexesByWorkspaceName = providedIndexesByWorkspaceNameByIndexName.get(indexName);
        if (managedIndexesByWorkspaceName == null) {
            managedIndexesByWorkspaceName = new HashMap<>();
            providedIndexesByWorkspaceNameByIndexName.put(indexName, managedIndexesByWorkspaceName);
        }
        managedIndexesByWorkspaceName.put(workspaceName, index);

        // Add it to the reverse lookup ...
        Map<String, AtomicIndex> byName = providedIndexesByIndexNameByWorkspaceName.get(workspaceName);
        if (byName == null) {
            byName = new HashMap<>();
            providedIndexesByIndexNameByWorkspaceName.put(workspaceName, byName);
        }
        byName.put(indexName, index);
    }
    
    private void scanWorkspace( IndexFeedback feedback,
                                final IndexDefinition defn,
                                final String workspaceName,
                                final ManagedIndex managedIndex ) {
        feedback.scan(workspaceName, new IndexFeedback.IndexingCallback() {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public void beforeIndexing() {
                logger().debug(
                        "Disabling index '{0}' from provider '{1}' in workspace '{2}' while it is reindexed. It will not be used in queries until reindexing has completed",
                        defn.getName(), defn.getProviderName(), workspaceName);
                managedIndex.enable(false);
            }

            @SuppressWarnings( "synthetic-access" )
            @Override
            public void afterIndexing() {
                managedIndex.enable(true);
                logger().debug("Enabled index '{0}' from provider '{1}' in workspace '{2}' after reindexing has completed",
                               defn.getName(), defn.getProviderName(), workspaceName);
            }
        });
    }

    @GuardedBy( "this" )
    private void refreshDelegateIndexWriter(final NodeTypes.Supplier nodeTypesSupplier ) {
        // Go through the providers and assemble into a structure that a new IndexWriter can use ...
        final Map<String, Collection<IndexChangeAdapter>> adaptersByWorkspaceName = new HashMap<>();
        final Collection<ManagedIndex> managedIndexes = new ArrayList<>();
        for (Map<String, AtomicIndex> providedIndexesByWorkspaceName : providedIndexesByWorkspaceNameByIndexName.values()) {
            for (Map.Entry<String, AtomicIndex> entry : providedIndexesByWorkspaceName.entrySet()) {
                String workspaceName = entry.getKey();
                AtomicIndex index = entry.getValue();
                Collection<IndexChangeAdapter> adaptersForWorkspace = adaptersByWorkspaceName.get(workspaceName);
                if (adaptersForWorkspace == null) {
                    adaptersForWorkspace = new ArrayList<>();
                    adaptersByWorkspaceName.put(workspaceName, adaptersForWorkspace);
                }
                adaptersForWorkspace.add(index.managed().getIndexChangeAdapter());
                managedIndexes.add(index.managed());
            }
        }
        final boolean canBeSkipped = managedIndexes.isEmpty();

        // Create a delegate writer ...
        this.delegateWriter = new IndexWriter() {
            @Override
            public boolean canBeSkipped() {
                return canBeSkipped;
            }

            @Override
            public void clearAllIndexes() {
                for (ManagedIndex index : managedIndexes) {
                    index.clearAllData();
                }
            }

            @Override
            public void add( String workspace,
                             NodeKey key,
                             Path path,
                             Name primaryType,
                             Set<Name> mixinTypes,
                             Properties properties ) {
                Collection<IndexChangeAdapter> adapters = applicableAdapters(workspace);
                if (adapters != null) {
                    boolean queryable = nodeTypesSupplier.getNodeTypes().isQueryable(primaryType, mixinTypes);
                    // There are adapters for this workspace ...
                    for (IndexChangeAdapter adapter : adapters) {
                        if (adapter != null) {
                            adapter.reindex(workspace, key, path, primaryType, mixinTypes, properties, queryable);
                        }
                    }
                }
            }

            @Override
            public void remove( String workspace, NodeKey key ) {
                Collection<IndexChangeAdapter> adapters = applicableAdapters(workspace);
                if (adapters != null) {
                    // There are adapters for this workspace ...
                    for (IndexChangeAdapter adapter : adapters) {
                        if (adapter != null) {
                            adapter.clearDataFor(key);
                        }
                    }
                }
            }
            
            private Collection<IndexChangeAdapter> applicableAdapters( String workspace ) {
                Collection<IndexChangeAdapter> adapters = null;
                if (RepositoryConfiguration.SYSTEM_WORKSPACE_NAME.equals(workspace)) {
                    // the system workspace is linked to each WS, so all adapters have to process system data...
                    adapters = new ArrayList<>();
                    for (Collection<IndexChangeAdapter> adapterCollection : adaptersByWorkspaceName.values()) {
                        adapters.addAll(adapterCollection);
                    }
                } else {
                    adapters = adaptersByWorkspaceName.get(workspace);
                }
                return adapters;
            }
        };
    }

    /**
     * Method called when this provider needs to create a new index given the unique pair of workspace name and index definition.
     * An index definition can apply to multiple workspaces, and when it does this method will be called once for each applicable
     * workspace. 
     * <p>
     * Providers may either choose to implement this method and therefore have full control over the logic of creating managed indexes
     * or simply implement the {@link #getIndexBuilder(IndexDefinition, String, NodeTypes.Supplier, NodeTypePredicate)}
     * method which is the easier route and provides some built-in defaults.
     * </p>
     *
     * @param defn the definition of the index; never null
     * @param workspaceName the name of the actual workspace to which the new index applies; never null
     * @param nodeTypesSupplier the supplier for the current node types cache; never null
     * @param matcher the node type matcher used to determine which nodes should be included in the index, and which automatically
     *        updates when node types are changed in the repository; may not be null
     * @param feedback the feedback mechanism for this provider to signal to ModeShape that portions of the repository content
     *        must be scanned to build/populate the new index; never null
     * @return the implementation-specific {@link ManagedIndex} for the new index; may not be null
     */
    protected ManagedIndex createIndex( IndexDefinition defn,
                                        String workspaceName,
                                        NodeTypes.Supplier nodeTypesSupplier,
                                        NodeTypePredicate matcher,
                                        IndexFeedback feedback ) {
        ManagedIndexBuilder builder = getIndexBuilder(defn, workspaceName, nodeTypesSupplier, matcher);
        if (builder == null) {
            throw new UnsupportedOperationException("Index providers should either override this method or the #getIndexBuilder method");
        }
        logger().debug("Index provider '{0}' is creating index in workspace '{1}': {2}", getName(), workspaceName, defn);
        ManagedIndex index = builder.build();
        if (index.requiresReindexing()) {
            scanWorkspace(feedback, defn, workspaceName, index);
        }
        return index;
    }

    /**
     * Method called when this provider needs to update an existing index given the unique pair of workspace name and index
     * definition. An index definition can apply to multiple workspaces, and when it is changed this method will be called once
     * for each applicable workspace.
     *
     * <p>
     * Providers may either choose to implement this method and therefore have full control over the logic of updating managed indexes
     * or simply implement the {@link #getIndexBuilder(IndexDefinition, String, NodeTypes.Supplier, NodeTypePredicate)}
     * method which is the easier route and provides a built-in logic which will first destroy the existing (old) index and then
     * create and register a new one via the builder.
     * </p>
     * 
     * @param oldDefn the previous definition of the index; never null
     * @param updatedDefn the updated definition of the index; never null
     * @param existingIndex the existing index prior to this update, as returned from {@link #createIndex} or {@link #updateIndex}
     *        ; never null
     * @param workspaceName the name of the actual workspace to which the new index applies; never null
     * @param nodeTypesSupplier the supplier for the current node types cache; never null
     * @param matcher the node type matcher used to determine which nodes should be included in the index, and which automatically
     *        updates when node types are changed in the repository; may not be null
     * @param feedback the feedback mechanism for this provider to signal to ModeShape that portions of the repository content
     *        must be scanned to rebuild/repopulate the updated index; never null
     * @return the operations and provider-specific state for this index; never null
     */
    protected ManagedIndex updateIndex( IndexDefinition oldDefn,
                                        IndexDefinition updatedDefn,
                                        ManagedIndex existingIndex,
                                        String workspaceName,
                                        NodeTypes.Supplier nodeTypesSupplier,
                                        NodeTypePredicate matcher,
                                        IndexFeedback feedback ) {
        ManagedIndexBuilder builder = getIndexBuilder(updatedDefn, workspaceName, nodeTypesSupplier, matcher);
        if (builder == null) {
            throw new UnsupportedOperationException("Index providers should either override this method or the #getIndexBuilder method");
        }
        logger().debug("Index provider '{0}' is updating index in workspace '{1}': {2}", getName(), workspaceName, updatedDefn);
        existingIndex.shutdown(true);
        ManagedIndex index = builder.build();
        if (index.requiresReindexing()) {
            scanWorkspace(feedback, updatedDefn, workspaceName, index);
        }
        return index;    
    }

    /**
     * Method called when this provider needs to remove an existing index given the unique pair of workspace name and index
     * definition. An index definition can apply to multiple workspaces, and when it does this method will be called once for each
     * applicable workspace.   
     * <p>
     * The default implementation is to simply delegate the call to a specific index's 
     * {@link org.modeshape.jcr.spi.index.provider.ProvidedIndex#shutdown(boolean)} method, indicating that the index should be destroyed.    
     * </p>
     *
     * @param oldDefn the previous definition of the index; never null
     * @param existingIndex the existing index prior to this update, as returned from {@link #createIndex} or {@link #updateIndex}
     *        ; never null
     * @param workspaceName the name of the actual workspace to which the new index applies; never null
     */
    protected void removeIndex( IndexDefinition oldDefn,
                                ManagedIndex existingIndex,
                                String workspaceName ) {
        logger().debug("Index provider '{0}' is removing index in workspace '{1}': {2}", getName(), workspaceName, oldDefn);
        existingIndex.shutdown(true);
    }

    /**
     * Offers subclasses the possibility of providing a {@link ManagedIndexBuilder} instance which significantly simplifies the
     * task of creating or updating {@link ManagedIndex} instances for providers. 
     * <p>
     * If a provider chooses to override this method, it does not have to override 
     * the {@link #createIndex(IndexDefinition, String, NodeTypes.Supplier, NodeTypePredicate, IndexFeedback)} method or 
     * the {@link #updateIndex(IndexDefinition, IndexDefinition, ManagedIndex, String, NodeTypes.Supplier, NodeTypePredicate, IndexFeedback)}
     * method, as the builder instance will be used to create specific indexes and adapters, based on the index definition.
     * </p>
     * <p>
     * Providers can also choose to ignore this method, in which case they will have to override the 
     * {@link #createIndex(IndexDefinition, String, NodeTypes.Supplier, NodeTypePredicate, IndexFeedback)} and
     * {@link #updateIndex(IndexDefinition, IndexDefinition, ManagedIndex, String, NodeTypes.Supplier, NodeTypePredicate, IndexFeedback)} 
     * methods.
     * </p>
     *
     * @param defn the definition of the index; never null
     * @param workspaceName the name of the actual workspace to which the new index applies; never null
     * @param nodeTypesSupplier the supplier for the current node types cache; never null
     * @param matcher the node type matcher used to determine which nodes should be included in the index, and which automatically
     *        updates when node types are changed in the repository; may not be null
     * @return a {@link ManagedIndexBuilder} instance, or {@code null} (the default) which indicates that a provider chooses to
     * ovveride both the createIndex and updateIndex methods.
     */
    protected ManagedIndexBuilder getIndexBuilder( IndexDefinition defn, 
                                                   String workspaceName,
                                                   NodeTypes.Supplier nodeTypesSupplier, 
                                                   NodeTypePredicate matcher ) {
        // this returns null for backwards compatibility reasons (otherwise it should've been abstract)
        return null;
    }

    @GuardedBy( "this" )
    private void removeProvidedIndexes( ChangeBus observable,
                                        Predicate<AtomicIndex> predicate ) {
        Iterator<Map.Entry<String, Map<String, AtomicIndex>>> iter = providedIndexesByWorkspaceNameByIndexName.entrySet()
                                                                                                                .iterator();
        while (iter.hasNext()) {
            Map<String, AtomicIndex> byWorkspaceName = iter.next().getValue();
            if (byWorkspaceName.isEmpty()) continue;
            Iterator<Map.Entry<String, AtomicIndex>> providedIter = byWorkspaceName.entrySet().iterator();
            while (providedIter.hasNext()) {
                AtomicIndex index = providedIter.next().getValue();
                if (predicate.test(index)) {
                    removeProvidedIndex(index, observable);
                    iter.remove();
                    // Look for this provided index in the reverse lookup ...
                    Map<String, AtomicIndex> byIndexName = providedIndexesByIndexNameByWorkspaceName.get(index.workspaceName());
                    byIndexName.remove(index.getName());
                }
            }
        }
    }

    @GuardedBy( "this" )
    private void registerIndex( AtomicIndex index,
                                ChangeBus observable ) {
        // Register the index as a listener will work even when clustered.
        if (index.indexDefinition().isSynchronous()) {
            // The index should be updated synchronously in the same thread that submits the events to the bus (before the
            // 'notify' method returns), and the "in-thread" behavior is what does this ...
            observable.registerInThread(index);
        } else {
            // The index is to be updated asynchronously, so use a normal listener ...
            observable.register(index);
        }
    }

    @GuardedBy( "this" )
    private void removeProvidedIndex( AtomicIndex index,
                                      ChangeBus observable ) {
        try {
            observable.unregister(index);
            removeIndex(index.indexDefinition(), index.managed(), index.workspaceName());
        } catch (RuntimeException e) {
            String msg = "Error removing index '{0}' in workspace '{1}' with definition: {2}";
            logger().error(e, msg, index.getName(), index.workspaceName(), index.indexDefinition());
        }
    }

    private NodeTypeMatcher nodeTypePredicate( NodeTypes nodeTypes,
                                               IndexDefinition defn ) {
        // Get the indexed node type ...
        String indexedNodeType = defn.getNodeTypeName();
        Name indexedNodeTypeName = context().getValueFactories().getNameFactory().create(indexedNodeType);
        Set<Name> allNodeTypes = nodeTypes.getAllSubtypes(indexedNodeTypeName);
        assert allNodeTypes != null;
        return NodeTypeMatcher.create(allNodeTypes, nodeTypes);
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
    private final class AtomicIndex implements Index, ChangeSetListener {
        private final String workspaceName;
        private volatile ManagedIndex managedIndex;
        private volatile IndexDefinition defn;
        private final NodeTypeMatcher matcher;

        protected AtomicIndex( IndexDefinition defn,
                               ManagedIndex managedIndex,
                               String workspaceName,
                               NodeTypeMatcher matcher ) {
            this.defn = defn;
            this.managedIndex = managedIndex;
            this.workspaceName = workspaceName;
            this.matcher = matcher;
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
            return defn.getKind() == IndexKind.TEXT;
        }

        @Override
        public boolean isEnabled() {
            return managedIndex.isEnabled();
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
                                     IndexDefinition newDefinition,
                                     NodeTypeMatcher matcher ) {
            this.managedIndex = managedIndex;
            this.defn = newDefinition;
            this.matcher.use(matcher);
        }
    }
}
