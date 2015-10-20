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
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.jcr.RepositoryException;
import org.modeshape.common.annotation.GuardedBy;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.ImmediateFuture;
import org.modeshape.jcr.JcrRepository.RunningState;
import org.modeshape.jcr.RepositoryIndexManager.ScanOperation;
import org.modeshape.jcr.RepositoryIndexManager.ScanningRequest;
import org.modeshape.jcr.RepositoryIndexManager.ScanningTasks;
import org.modeshape.jcr.api.index.IndexDefinition;
import org.modeshape.jcr.api.index.IndexManager;
import org.modeshape.jcr.api.query.QueryCancelledException;
import org.modeshape.jcr.api.query.qom.QueryCommand;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.PathCache;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.cache.document.WorkspaceCache;
import org.modeshape.jcr.journal.ChangeJournal;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.query.CancellableQuery;
import org.modeshape.jcr.query.CompositeIndexWriter;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.QueryEngine;
import org.modeshape.jcr.query.QueryEngineBuilder;
import org.modeshape.jcr.query.QueryResults;
import org.modeshape.jcr.query.engine.IndexQueryEngine;
import org.modeshape.jcr.query.engine.ScanningQueryEngine;
import org.modeshape.jcr.query.plan.PlanHints;
import org.modeshape.jcr.query.validate.Schemata;
import org.modeshape.jcr.spi.index.IndexWriter;
import org.modeshape.jcr.spi.index.provider.IndexProvider;
import org.modeshape.jcr.spi.index.provider.ManagedIndex;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;

/**
 * The query manager a the repository. Each instance lazily starts up the {@link QueryEngine}, which can be expensive.
 */
class RepositoryQueryManager implements ChangeSetListener {

    private final Logger logger = Logger.getLogger(getClass());
    private final Logger indexLogger = Logger.getLogger(getClass().getPackage().getName() + ".index");
    private final RunningState runningState;
    private final ExecutorService indexingExecutorService;
    private final RepositoryConfiguration repoConfig;
    private final RepositoryConfiguration.Reindexing reindexingCfg;
    private final RepositoryIndexManager indexManager;
    private final Lock engineInitLock = new ReentrantLock();
    @GuardedBy( "engineInitLock" )
    private volatile QueryEngine queryEngine;
    private volatile Future<Void> asyncReindexingResult;
    private volatile ScanningTasks toBeScanned = new ScanningTasks();
    private final AtomicBoolean started = new AtomicBoolean(false);

    RepositoryQueryManager( RunningState runningState,
                            ExecutorService indexingExecutorService,
                            RepositoryConfiguration config,
                            RepositoryConfiguration.Reindexing reindexingCfg) {
        this.runningState = runningState;
        this.indexingExecutorService = indexingExecutorService;
        this.repoConfig = config;
        this.reindexingCfg = reindexingCfg;
        this.indexManager = new RepositoryIndexManager(runningState, config);
    }

    synchronized void initialize() {
        this.toBeScanned.add(indexManager.initialize());
        started.set(true);
    }

    @Override
    public synchronized void notify( ChangeSet changeSet ) {
        if (started.get()) {
            boolean scanRequired = this.toBeScanned.add(this.indexManager.notify(changeSet));
            if (scanRequired) {
                // refresh the index writer
                this.indexManager.refreshIndexWriter();
                // It's initialized, so we have to call it ...
                reindexIfNeeded(reindexingCfg.isAsync(), false);
            }
        }
        // If not yet initialized, the "reindexIfNeeded" method will be called by the JcrRepository.
    }

    ChangeSetListener getListener() {
        return this;
    }

    void shutdown() {
        started.compareAndSet(true, false);
        indexingExecutorService.shutdown();
        if (queryEngine != null) {
            try {
                engineInitLock.lock();
                if (queryEngine != null) {
                    try {
                        queryEngine.shutdown();
                    } finally {
                        queryEngine = null;
                    }
                }
            } finally {
                engineInitLock.unlock();
            }
        }
        indexManager.shutdown();
    }

    void stopReindexing() {
        try {
            engineInitLock.lock();
            if (asyncReindexingResult != null) {
                try {
                    asyncReindexingResult.get(1, TimeUnit.MINUTES);
                } catch (java.util.concurrent.TimeoutException e) {
                    logger.debug("Re-indexing has not finished in time, attempting to cancel operation");
                    asyncReindexingResult.cancel(true);
                } catch (Exception e) {
                    logger.debug(e, "Unexpected exception while waiting for re-indexing to terminate");
                }
            }
        } finally {
            asyncReindexingResult = null;
            engineInitLock.unlock();
        }
    }

    public CancellableQuery query( ExecutionContext context,
                                   RepositoryCache repositoryCache,
                                   Set<String> workspaceNames,
                                   Map<String, NodeCache> overriddenNodeCachesByWorkspaceName,
                                   final QueryCommand query,
                                   Schemata schemata,
                                   RepositoryIndexes indexDefns,
                                   NodeTypes nodeTypes,
                                   PlanHints hints,
                                   Map<String, Object> variables ) {
        final QueryEngine queryEngine = queryEngine();
        final QueryContext queryContext = queryEngine.createQueryContext(context, repositoryCache, workspaceNames,
                                                                         overriddenNodeCachesByWorkspaceName, schemata,
                                                                         indexDefns, nodeTypes, new BufferManager(context),
                                                                         hints, variables);
        final org.modeshape.jcr.query.model.QueryCommand command = (org.modeshape.jcr.query.model.QueryCommand)query;
        return new CancellableQuery() {
            private final Lock lock = new ReentrantLock();
            private QueryResults results;

            @Override
            public QueryResults execute() throws QueryCancelledException, RepositoryException {
                try {
                    lock.lock();
                    if (results == null) {
                        // this will block and will hold the lock until it is done ...
                        results = queryEngine.execute(queryContext, command);
                    }
                    return results;
                } finally {
                    lock.unlock();
                }
            }

            @Override
            public boolean cancel() {
                return queryContext.cancel();
            }
        };
    }

    /**
     * Get the writer to the indexes. The resulting instance will only write to the index providers that were registered at the
     * time this method is called. Therefore, the writer should be used and discarded relatively quickly, since query index
     * providers may be {@link RepositoryIndexManager#register(org.modeshape.jcr.spi.index.provider.IndexProvider) added} or
     * {@link RepositoryIndexManager#unregister(String) removed} at any time.
     *
     * @return the index writer; never null
     */
    public IndexWriter getIndexWriter() {
        return indexManager.getIndexWriter();
    }

    protected RepositoryIndexManager getIndexManager() {
        return indexManager;
    }

    /**
     * Get an immutable snapshot of the index definitions. This can be used by the query engine to determine which indexes might
     * be usable when quering a specific selector (node type).
     *
     * @return a snapshot of the index definitions at this moment; never null
     */
    RepositoryIndexes getIndexes() {
        return indexManager.getIndexes();
    }

    /**
     * Obtain the query engine, which is created lazily and in a thread-safe manner.
     *
     * @return the query engine; never null
     */
    protected final QueryEngine queryEngine() {
        if (queryEngine == null) {
            try {
                engineInitLock.lock();
                if (queryEngine == null) {
                    QueryEngineBuilder builder = null;
                    if (!repoConfig.getIndexProviders().isEmpty()) {
                        // There is at least one index provider ...
                        builder = IndexQueryEngine.builder();
                        logger.debug("Queries with indexes are enabled for the '{0}' repository. Executing queries may require scanning the repository contents when the query cannot use the defined indexes.",
                                     repoConfig.getName());
                    } else {
                        // There are no indexes ...
                        builder = ScanningQueryEngine.builder();
                        logger.debug("Queries with no indexes are enabled for the '{0}' repository. Executing queries will always scan the repository contents.",
                                     repoConfig.getName());
                    }
                    queryEngine = builder.using(repoConfig, indexManager, runningState.context()).build();
                }
            } finally {
                engineInitLock.unlock();
            }
        }
        return queryEngine;
    }
    
    protected void reindex() {
        if (!indexManager.hasProviders()) {
            // there are no index providers, so nothing to reindex
            return;
        }
        
        boolean async = reindexingCfg.isAsync();
        RepositoryConfiguration.ReindexingMode mode = reindexingCfg.mode();
        switch (mode) {
            case INCREMENTAL: {
                final ChangeJournal journal = runningState.journal();
                if (journal == null) {
                    logger.warn(JcrI18n.warnIncrementalIndexingJournalNotEnabled, repoConfig.getName());
                    break;
                } else if (!journal.started()) {
                    logger.warn(JcrI18n.warnIncrementalIndexingJournalNotStarted, repoConfig.getName());
                    break;
                } else {
                    if (!async) {
                        reindexIncrementally(journal);
                    } else {
                        indexingExecutorService.submit(new Callable<Void>() {
                            @Override
                            public Void call() throws Exception {
                                reindexIncrementally(journal);
                                return null;
                            }
                        }); 
                    }
                    break;
                }
            }
            case IF_MISSING: {
                // when reindexing after startup, make sure the system area is included as well or indexes will lack this
                // information
                reindexIfNeeded(async, true);
                break;
            }
            default: {
                throw new IllegalArgumentException("Unknown indexing mode: " + mode.toString());
            }
        }
    }

    private void reindexIncrementally( ChangeJournal journal ) {
        // each provider may have a different timestamp when it last updated its indexes successfully. 
        // so for simplicity we'll use the lowest (earliest) timestamp of them all when reindexing
        long earliestTimestamp = Long.MAX_VALUE;
        List<IndexProvider> incrementalIndexingProviders = new ArrayList<>();
        for (IndexProvider provider : indexManager.getProviders()) {
            Long latestIndexUpdateTime = provider.getLatestIndexUpdateTime();
            if (latestIndexUpdateTime == null) {
                logger.warn(JcrI18n.warnIncrementalIndexingNotSupported, provider.getName());
                continue;
            }
            incrementalIndexingProviders.add(provider);
            earliestTimestamp = Math.min(earliestTimestamp, latestIndexUpdateTime);
        }
        if (incrementalIndexingProviders.isEmpty()) {
            // none of the providers support incremental reindexing so nothing to do...
            return;
        }
        assert earliestTimestamp != Long.MAX_VALUE;
        Iterator<NodeKey> changedNodes = journal.changedNodesSince(earliestTimestamp);
        IndexWriter writer = CompositeIndexWriter.create(incrementalIndexingProviders);
        RepositoryCache repositoryCache = runningState.repositoryCache();
        for (String workspaceName : repositoryCache.getWorkspaceNames()) {
            if (logger.isDebugEnabled()) {
                logger.debug("Performing incremental reindexing since '{0}' for repository '{1}' on workspace '{2}'",
                             new Date(earliestTimestamp), repositoryCache.getName(), workspaceName);
            }
            WorkspaceCache workspaceCache = repositoryCache.getWorkspaceCache(workspaceName);
            reindexSince(workspaceCache, writer, changedNodes);
        }
    }

    /**
     * Reindex the repository only if there is at least one provider that required scanning and reindexing.
     *
     * @param async whether the reindexing should be performed asynchronously or not
     * @param includeSystemContent whether the /jcr:system area should be indexed or not
     */
    protected void reindexIfNeeded( boolean async, final boolean includeSystemContent ) {
        final ScanningRequest request = toBeScanned.drain();
        if (!request.isEmpty()) {
            final IndexWriter writer = indexManager.getIndexWriterForProviders(request.providerNames());
            final RepositoryCache repoCache = runningState.repositoryCache();
            scan(async, writer, new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    // Scan each of the workspace-path pairs ...
                    ScanOperation op = new ScanOperation() {
                        @Override
                        public void scan( String workspaceName,
                                          Path path ) {
                            NodeCache workspaceCache = repoCache.getWorkspaceCache(workspaceName);
                            if (workspaceCache != null) {
                                // The workspace is still valid ...
                                CachedNode node = workspaceCache.getNode(workspaceCache.getRootKey());
                                if (!path.isRoot()) {
                                    for (Path.Segment segment : path) {
                                        ChildReference child = node.getChildReferences(workspaceCache).getChild(segment);
                                        if (child == null) {
                                            // The child no longer exists, so ignore this pair ...
                                            node = null;
                                            break;
                                        }
                                        node = workspaceCache.getNode(child);
                                        if (node == null) break;
                                    }
                                }
                                if (node != null) {
                                    if (logger.isDebugEnabled()) {
                                        logger.debug("Performing full reindexing for repository '{0}' and workspace '{1}'", 
                                                     repoCache.getName(),
                                                     workspaceName);
                                    }
                                    // If we find a node to start at, then scan the content ...
                                    // in certain cases (e.g. at startup) we have to index the system content (if it applies to
                                    // any of the indexes)
                                    boolean scanSystemContent = includeSystemContent ||
                                                                repoCache.getSystemWorkspaceName().equals(workspaceName);
                                    reindexContent(workspaceName, workspaceCache, node, Integer.MAX_VALUE, scanSystemContent,
                                                   writer);
                                }
                            }
                        }
                    };
                    request.onEachPathInWorkspace(op);
                    return null;
                }
            });
        }
    }

    /**
     * Clean all indexes and reindex all content.
     *
     * @param async true if the reindexing should be done in the background, or false if it should be done using this thread
     */
    protected void cleanAndReindex( boolean async ) {
        final IndexWriter writer = getIndexWriter();
        scan(async, getIndexWriter(), new Callable<Void>() {
            @SuppressWarnings( "synthetic-access" )
            @Override
            public Void call() throws Exception {
                writer.clearAllIndexes();
                reindexContent(true, writer);
                return null;
            }
        });
    }

    private void scan( boolean async,
                       final IndexWriter indexes,
                       Callable<Void> callable ) {
        if (!indexes.canBeSkipped()) {
            if (async) {
                asyncReindexingResult = indexingExecutorService.submit(callable);
            } else {
                try {
                    callable.call();
                } catch (RuntimeException e) {
                    throw e;
                } catch (Exception e) {
                    throw new RuntimeException();
                }
            }
        }
    }

    /**
     * Crawl and index all of the repository content.
     *
     * @param includeSystemContent true if the system content should also be indexed
     * @param indexes the index writer that should be use; may not be null
     */
    private void reindexContent( boolean includeSystemContent,
                                 IndexWriter indexes ) {
        if (indexes.canBeSkipped()) return;
        // The node type schemata changes every time a node type is (un)registered, so get the snapshot that we'll use throughout
        RepositoryCache repoCache = runningState.repositoryCache();

        logger.debug(JcrI18n.reindexAll.text(runningState.name()));

        if (includeSystemContent) {
            NodeCache systemWorkspaceCache = repoCache.getWorkspaceCache(repoCache.getSystemWorkspaceName());
            CachedNode rootNode = systemWorkspaceCache.getNode(repoCache.getSystemKey());
            // Index the system content ...
            logger.debug("Starting reindex of system content in '{0}' repository.", runningState.name());
            reindexSystemContent(rootNode, Integer.MAX_VALUE, indexes);
            logger.debug("Completed reindex of system content in '{0}' repository.", runningState.name());
        }

        // Index the non-system workspaces ...
        for (String workspaceName : repoCache.getWorkspaceNames()) {
            NodeCache workspaceCache = repoCache.getWorkspaceCache(workspaceName);
            CachedNode rootNode = workspaceCache.getNode(workspaceCache.getRootKey());
            logger.debug("Starting reindex of workspace '{0}' content in '{1}' repository.", runningState.name(), workspaceName);
            reindexContent(workspaceName, workspaceCache, rootNode, Integer.MAX_VALUE, false, indexes);
            logger.debug("Completed reindex of workspace '{0}' content in '{1}' repository.", runningState.name(), workspaceName);
        }
    }

    /**
     * Crawl and index the content in the named workspace.
     *
     * @param workspace the workspace
     * @throws IllegalArgumentException if the workspace is null
     */
    public void reindexContent( JcrWorkspace workspace ) {
        reindexContent(workspace, Path.ROOT_PATH, Integer.MAX_VALUE);
    }

    /**
     * Crawl and index the content starting at the supplied path in the named workspace, to the designated depth.
     *
     * @param workspace the workspace
     * @param path the path of the content to be indexed
     * @param depth the depth of the content to be indexed
     * @throws IllegalArgumentException if the workspace or path are null, or if the depth is less than 1
     */
    public void reindexContent( JcrWorkspace workspace,
                                Path path,
                                int depth ) {
        if (getIndexWriter().canBeSkipped()) {
            // There's no indexes that require updating ...
            return;
        }
        CheckArg.isPositive(depth, "depth");
        JcrSession session = workspace.getSession();
        NodeCache cache = session.cache().getWorkspace();
        String workspaceName = workspace.getName();

        // Look for the node ...
        CachedNode node = cache.getNode(cache.getRootKey());
        for (Segment segment : path) {
            // Look for the child by name ...
            ChildReference ref = node.getChildReferences(cache).getChild(segment);
            if (ref == null) return;
            node = cache.getNode(ref);
        }

        // If the node is in the system workspace ...
        String systemWorkspaceKey = runningState.repositoryCache().getSystemWorkspaceKey();
        if (node.getKey().getWorkspaceKey().equals(systemWorkspaceKey)) {
            reindexSystemContent(node, depth, getIndexWriter());
        } else {
            // It's just a regular node in the workspace ...
            reindexContent(workspaceName, cache, node, depth, path.isRoot(), getIndexWriter());
        }
    }
    
    protected void reindexSince( JcrWorkspace workspace,
                                 long timestamp ) {
        ChangeJournal journal = runningState.journal();
        assert journal != null;
        Iterator<NodeKey> changedNodes = journal.changedNodesSince(timestamp);
        if (!changedNodes.hasNext()) {
            // there are no nodes which have been changed since the given timestamp
            return;
        }
        reindexSince(workspace.getSession().cache().getWorkspace(), getIndexWriter(), changedNodes);
    }
    
    protected void reindexSince( final WorkspaceCache cache,
                                 final IndexWriter writer,
                                 final Iterator<NodeKey> changedNodes ) {
        if (writer.canBeSkipped()) {
            // There's no indexes that require updating ...
            return;
        }
        String workspaceName = cache.getWorkspaceName();
        String workspaceKey = NodeKey.keyForWorkspaceName(workspaceName);
      
        // take each of node keys that have been changed since the given timestamp and reindex each one if they belong to this WS
        while (changedNodes.hasNext()) {
            NodeKey nodeKey = changedNodes.next();
            if (!workspaceKey.equals(nodeKey.getWorkspaceKey())) {
                // this node does not belong to this WS cache, so ignore it...
                continue;
            }
            CachedNode node = cache.getNode(nodeKey);
            if (node != null) {
                // the node still exists in the repository so reindex based on the latest available data...
                reindexContent(workspaceName, cache, node, 1, true, writer);
            } else {
                // the node has been removed from the repository so clear the information from the indexes...
                writer.remove(workspaceName, nodeKey);
            }
        }   
    }

    protected Future<Boolean> reindexSinceAsync( final JcrWorkspace workspace,
                                                 final long timestamp ) {
        ChangeJournal journal = runningState.journal();
        assert journal != null;
        Iterator<NodeKey> changedNodes = journal.changedNodesSince(timestamp);
        if (!changedNodes.hasNext()) {
            // there are no nodes which have been changed since the given timestamp
            return new ImmediateFuture<>(Boolean.FALSE);
        }
        return reindexSinceAsync(workspace.getSession().cache().getWorkspace(), getIndexWriter(), changedNodes);
    }
    
    protected Future<Boolean> reindexSinceAsync( final WorkspaceCache cache,
                                                 final IndexWriter indexWriter,
                                                 final Iterator<NodeKey> changedNodes ) {
        return indexingExecutorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                reindexSince(cache, indexWriter, changedNodes);
                return Boolean.TRUE;
            }
        });    
    }

    protected void reindexContent( final String workspaceName,
                                   NodeCache cache,
                                   CachedNode node,
                                   int depth,
                                   boolean reindexSystemContent,
                                   final IndexWriter indexes ) {
        assert indexes != null;
        if (indexes.canBeSkipped()) return;
        if (node.isExcludedFromSearch(cache)) {
            return;
        }

        try {
            // change the status of the indexes to reindexing
            updateIndexesStatus(workspaceName, IndexManager.IndexStatus.ENABLED, IndexManager.IndexStatus.REINDEXING);
            
            // Get the path for the first node (we already have it, but we need to populate the cache) ...
            final PathCache paths = new PathCache(cache);
            Path nodePath = paths.getPath(node);

            // Index the first node ...
            if (indexLogger.isTraceEnabled()) {
                String path = runningState.context().getValueFactories().getStringFactory().create(nodePath);
                indexLogger.debug("Reindexing node '{0}' in workspace '{1}' of repository '{2}': {3}", path, workspaceName,
                                  runningState.name(), node);
            }
            indexes.add(workspaceName, node.getKey(), nodePath, node.getPrimaryType(cache), node.getMixinTypes(cache),
                        node.getPropertiesByName(cache));

            if (depth == 1) return;

            // Create a queue for processing the subgraph
            final Queue<NodeKey> queue = new LinkedList<NodeKey>();

            if (reindexSystemContent) {
                // We need to look for the system node, and index it differently ...
                ChildReferences childRefs = node.getChildReferences(cache);
                ChildReference systemRef = childRefs.getChild(JcrLexicon.SYSTEM);
                NodeKey systemKey = systemRef != null ? systemRef.getKey() : null;
                for (ChildReference childRef : node.getChildReferences(cache)) {
                    NodeKey childKey = childRef.getKey();
                    if (childKey.equals(systemKey)) {
                        // This is the "/jcr:system" node ...
                        node = cache.getNode(childKey);
                        reindexSystemContent(node, depth - 1, indexes);
                    } else {
                        queue.add(childKey);
                    }
                }
            } else {
                // Add all children to the queue ...
                for (ChildReference childRef : node.getChildReferences(cache)) {
                    NodeKey childKey = childRef.getKey();
                    // we should not reindex anything which is in the system area
                    if (!childKey.getWorkspaceKey().equals(runningState.systemWorkspaceKey())) {
                        queue.add(childKey);
                    }
                }
            }

            // Now, process the queue until empty ...
            while (true) {
                NodeKey key = queue.poll();
                if (key == null) break;
    
                // Look up the node and find the path ...
                node = cache.getNode(key);
                if (node == null || node.isExcludedFromSearch(cache)) {
                    continue;
                }
                nodePath = paths.getPath(node);
    
                // Index the node ...
                if (indexLogger.isTraceEnabled()) {
                    String path = runningState.context().getValueFactories().getStringFactory().create(nodePath);
                    indexLogger.debug("Reindexing node '{0}' in workspace '{1}' of repository '{2}': {3}", path, workspaceName,
                                      runningState.name(), node);
                }
                indexes.add(workspaceName, node.getKey(), nodePath, node.getPrimaryType(cache), node.getMixinTypes(cache),
                            node.getPropertiesByName(cache));
    
                // Check the depth ...
                if (nodePath.size() <= depth) {
                    // Add the children to the queue ...
                    for (ChildReference childRef : node.getChildReferences(cache)) {
                        queue.add(childRef.getKey());
                    }
                }
            }
        } finally {
            // set the index status back to enabled
            updateIndexesStatus(workspaceName, IndexManager.IndexStatus.REINDEXING, IndexManager.IndexStatus.ENABLED);
        }
    }
    
    protected void updateIndexesStatus( String workspaceName, final IndexManager.IndexStatus currentStatus, final IndexManager.IndexStatus newStatus ) {
        for (IndexProvider indexProvider : indexManager.getProviders()) {
            indexProvider.onEachIndexInWorkspace(workspaceName, new IndexProvider.ManagedIndexOperation() {
                @Override
                public void apply( String workspaceName, ManagedIndex index, IndexDefinition defn ) {
                    index.updateStatus(currentStatus, newStatus);
                }
            });
        }
    }

    protected void reindexSystemContent( CachedNode nodeInSystemBranch,
                                         int depth,
                                         IndexWriter indexes ) {
        RepositoryCache repoCache = runningState.repositoryCache();
        String workspaceName = repoCache.getSystemWorkspaceName();
        NodeCache systemWorkspaceCache = repoCache.getWorkspaceCache(workspaceName);
        reindexContent(workspaceName, systemWorkspaceCache, nodeInSystemBranch, depth, true, indexes);
    }

    protected void reindexSystemContent() {
        RepositoryCache repoCache = runningState.repositoryCache();
        String workspaceName = repoCache.getSystemWorkspaceName();
        NodeCache systemWorkspaceCache = repoCache.getWorkspaceCache(workspaceName);
        CachedNode systemNode = systemWorkspaceCache.getNode(repoCache.getSystemKey());
        reindexContent(workspaceName, systemWorkspaceCache, systemNode, Integer.MAX_VALUE, true, getIndexWriter());
    }

    /**
     * Asynchronously crawl and index the content in the named workspace.
     *
     * @param workspace the workspace
     * @return the future for the asynchronous operation; never null
     * @throws IllegalArgumentException if the workspace is null
     */
    public Future<Boolean> reindexContentAsync( final JcrWorkspace workspace ) {
        return indexingExecutorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                reindexContent(workspace);
                return Boolean.TRUE;
            }
        });
    }

    /**
     * Asynchronously crawl and index the content starting at the supplied path in the named workspace, to the designated depth.
     *
     * @param workspace the workspace
     * @param path the path of the content to be indexed
     * @param depth the depth of the content to be indexed
     * @return the future for the asynchronous operation; never null
     * @throws IllegalArgumentException if the workspace or path are null, or if the depth is less than 1
     */
    public Future<Boolean> reindexContentAsync( final JcrWorkspace workspace,
                                                final Path path,
                                                final int depth ) {
        return indexingExecutorService.submit(new Callable<Boolean>() {
            @Override
            public Boolean call() throws Exception {
                reindexContent(workspace, path, depth);
                return Boolean.TRUE;
            }
        });
    }

}
