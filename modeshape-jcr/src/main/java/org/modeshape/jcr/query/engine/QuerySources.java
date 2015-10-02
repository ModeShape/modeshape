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
package org.modeshape.jcr.query.engine;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.JoinCondition;
import org.modeshape.jcr.JcrLexicon;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.ChildReference;
import org.modeshape.jcr.cache.ChildReferences;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.cache.document.NodeCacheIterator;
import org.modeshape.jcr.cache.document.NodeCacheIterator.NodeFilter;
import org.modeshape.jcr.query.NodeSequence;
import org.modeshape.jcr.query.NodeSequence.Batch;
import org.modeshape.jcr.spi.index.Index;
import org.modeshape.jcr.spi.index.IndexConstraints;
import org.modeshape.jcr.spi.index.ResultWriter;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Path.Segment;
import org.modeshape.jcr.value.ValueFactories;

/**
 * A factory for creating {@link NodeSequence} instances.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class QuerySources {

    protected final RepositoryCache repo;
    protected final String workspaceName;
    protected final String systemWorkspaceName;
    protected final boolean includeSystemContent;
    protected final NodeFilter queryableFilter;
    protected final NodeFilter queryableAndNonSystemFilter;
    protected final NodeTypes nodeTypes;

    /**
     * Construct a new instance.
     *
     * @param repository the repository cache; may not be null
     * @param nodeTypes the node types cache; may not be null
     * @param workspaceName the name of the main workspace to be queried; may not be null
     * @param includeSystemContent true if the system content is to be included in the query results, or false otherwise
     */
    public QuerySources( RepositoryCache repository,
                         final NodeTypes nodeTypes,
                         String workspaceName,
                         boolean includeSystemContent ) {
        assert repository != null;
        assert nodeTypes != null;
        assert workspaceName != null;
        this.repo = repository;
        this.nodeTypes = nodeTypes;
        this.workspaceName = workspaceName;
        this.includeSystemContent = includeSystemContent;
        this.systemWorkspaceName = includeSystemContent ? repo.getSystemWorkspaceName() : null;
        this.queryableFilter = new NodeFilter() {
            @Override
            public final boolean includeNode( CachedNode node,
                                              NodeCache cache ) {
                // Include only queryable nodes ...
                Name nodePrimaryType = node.getPrimaryType(cache);
                Set<Name> mixinTypes = node.getMixinTypes(cache);
                return !node.isExcludedFromSearch(cache) && nodeTypes.isQueryable(nodePrimaryType, mixinTypes);
            }

            @Override
            public String toString() {
                return "(queryable nodes)";
            }

            @Override
            public boolean continueProcessingChildren( CachedNode node, NodeCache cache ) {
                Name nodePrimaryType = node.getPrimaryType(cache);
                Set<Name> mixinTypes = node.getMixinTypes(cache);
                return !node.isExcludedFromSearch(cache) && 
                       !nodeTypes.isQueryable(nodePrimaryType, mixinTypes);
            }
        };
        if (!this.includeSystemContent) {
            final String systemWorkspaceKey = repo.getSystemWorkspaceKey();
            this.queryableAndNonSystemFilter = new NodeFilter() {
                @Override
                public final boolean includeNode( CachedNode node,
                                                  NodeCache cache ) {
                    // Include only queryable nodes that are NOT in the system workspace ...
                    Name nodePrimaryType = node.getPrimaryType(cache);
                    Set<Name> mixinTypes = node.getMixinTypes(cache);
                    return !node.isExcludedFromSearch(cache) && 
                           !node.getKey().getWorkspaceKey().equals(systemWorkspaceKey) && 
                           nodeTypes.isQueryable(nodePrimaryType, mixinTypes);
                }

                @Override
                public String toString() {
                    return "(queryable nodes not in workspace)";
                }

                @Override
                public boolean continueProcessingChildren( CachedNode node, NodeCache cache ) {
                    Name nodePrimaryType = node.getPrimaryType(cache);
                    Set<Name> mixinTypes = node.getMixinTypes(cache);
                    return  !node.isExcludedFromSearch(cache) &&
                            !node.getKey().getWorkspaceKey().equals(systemWorkspaceKey) && 
                            !nodeTypes.isQueryable(nodePrimaryType, mixinTypes);
                }
            };
        } else {
            this.queryableAndNonSystemFilter = queryableFilter;
        }
    }

    public boolean includeSystemContent() {
        return includeSystemContent;
    }

    public String getWorkspaceName() {
        return workspaceName;
    }

    /**
     * Obtain a {@link NodeSequence} that returns all (queryable) nodes in the workspace, where each node is assigned the given
     * score.
     *
     * @param score the score for each node
     * @param nodeCount the number of nodes (or an estimate) that will be returned
     * @return the sequence of nodes; never null
     */
    public NodeSequence allNodes( float score,
                                  long nodeCount ) {
        // Use a single batch for the workspace content ...
        NodeCacheIterator iter = nodes(workspaceName, null);
        assert iter != null;
        Batch mainBatch = NodeSequence.batchOfKeys(iter, nodeCount, score, workspaceName, repo);

        // Nothing else to use ...
        return NodeSequence.withBatch(mainBatch);
    }

    /**
     * Obtain a {@link NodeSequence} that returns the (queryable) node at the given path in the workspace, where the node is
     * assigned the given score.
     *
     * @param path the path of the node; may not be null
     * @param score the score for the node
     * @return the sequence of node(s); never null
     */
    public NodeSequence singleNode( Path path,
                                    float score ) {
        String workspaceName = getWorkspaceName(path);

        // Get the node filter to use ...
        NodeFilter nodeFilter = nodeFilterForWorkspace(workspaceName);
        if (nodeFilter != null) {

            // Find the node by path ...
            NodeCache cache = repo.getWorkspaceCache(workspaceName);
            CachedNode node = getNodeAtPath(path, cache);
            if (node != null && nodeFilter.includeNode(node, cache)) {
                return NodeSequence.withNodes(Collections.singleton(node), score, workspaceName);
            }
        }
        return NodeSequence.emptySequence(1);
    }

    /**
     * Obtain a {@link NodeSequence} that returns the (queryable) node with the given key in the workspace, where the node is
     * assigned the given score.
     *
     * @param workspaceName the name of the workspace; may not be null
     * @param identifier the {@link NodeKey#getIdentifier() identifier} of the node; may not be null
     * @param score the score for the node
     * @return the sequence of node(s); never null
     */
    public NodeSequence singleNode( String workspaceName,
                                    String identifier,
                                    float score ) {
        // Get the node filter to use ...
        NodeFilter nodeFilter = nodeFilterForWorkspace(workspaceName);
        if (nodeFilter != null) {

            // Find the node by path ...
            NodeCache cache = repo.getWorkspaceCache(workspaceName);
            if (NodeKey.isValidFormat(identifier)) {
                NodeKey key = new NodeKey(identifier);
                CachedNode node = cache.getNode(key);
                if (node != null && nodeFilter.includeNode(node, cache)) {
                    return NodeSequence.withNodes(Collections.singleton(node), score, workspaceName);
                }
            }
            // Try with the same source and workspace key as the root node ...
            NodeKey key = cache.getRootKey().withId(identifier);
            CachedNode node = cache.getNode(key);
            if (node != null && nodeFilter.includeNode(node, cache)) {
                return NodeSequence.withNodes(Collections.singleton(node), score, workspaceName);
            }
        }
        return NodeSequence.emptySequence(1);
    }

    protected String getIdentifier( CachedNode node,
                                    NodeKey workspaceRootKey ) {
        NodeKey key = node.getKey();
        if (!key.getSourceKey().equals(workspaceRootKey.getSourceKey())) {
            // return the whole thing ...
            return key.toString();
        }
        // return just the identifier part ...
        return key.getIdentifier();
    }

    /**
     * Obtain a {@link NodeSequence} that returns the (queryable) children of the node at the given path in the workspace, where
     * each child node is assigned the given score.
     *
     * @param parentPath the path of the parent node; may not be null
     * @param score the score for the nodes
     * @return the sequence of nodes; never null
     */
    public NodeSequence childNodes( Path parentPath,
                                    float score ) {
        String workspaceName = getWorkspaceName(parentPath);

        // Get the node filter to use ...
        NodeFilter nodeFilter = nodeFilterForWorkspace(workspaceName);
        if (nodeFilter != null) {
            // always append a shared nodes filter to the end of the workspace filter
            // JCR #14.16 -If a query matches a descendant node of a shared set, it appears in query results only once.
            NodeFilter compositeFilter = new CompositeNodeFilter(nodeFilter, sharedNodesFilter());

            // Find the node by path ...
            NodeCache cache = repo.getWorkspaceCache(workspaceName);
            CachedNode parentNode = getNodeAtPath(parentPath, cache);
            if (parentNode != null) {
                // Only add those children that are queryable ...
                ChildReferences childRefs = parentNode.getChildReferences(cache);
                List<CachedNode> results = new ArrayList<CachedNode>((int)childRefs.size());
                for (ChildReference childRef : childRefs) {
                    CachedNode child = cache.getNode(childRef);
                    if (compositeFilter.includeNode(child, cache)) {
                        results.add(child);
                    }
                }
                return NodeSequence.withNodes(results, score, workspaceName);
            }
        }
        return NodeSequence.emptySequence(1);
    }

    /**
     * Obtain a {@link NodeSequence} that returns the (queryable) descendants of the node at the given path in the workspace,
     * where each descendant node is assigned the given score.
     *
     * @param ancestorPath the path of the ancestor of all descendants; may not be null
     * @param score the score for the nodes
     * @return the sequence of nodes; never null
     */
    public NodeSequence descendantNodes( Path ancestorPath,
                                         float score ) {
        String workspaceName = getWorkspaceName(ancestorPath);

        // Get an iterator over all acceptable nodes in the workspace ...
        NodeCacheIterator iter = nodes(workspaceName, ancestorPath);
        if (iter != null) {
            if (iter.hasNext()) {
                // Skip the node at our path, which is to be excluded ...
                NodeKey key = iter.next();
                assert ancestorPath.equals(path(workspaceName, key)) : "First node in results does not match the expected path";
            }
            // Finally create and add a batch for this workspace ...
            return NodeSequence.withNodeKeys(iter, -1, score, workspaceName, repo);
        }
        return NodeSequence.emptySequence(1);
    }

    /**
     * Obtain a {@link NodeSequence} that uses the supplied index to find the node that satisfy the given constraints.
     *
     * @param index the index; may not be null
     * @param constraints the constraints that apply to the index; may not be null but can be empty
     * @param joinConditions the join constraints that apply to the index; may not be but can be empty
     * @param variables the immutable map of variable values keyed by their name; never null but possibly empty
     * @param parameters the provider-specific index parameters; may not be null, but may be empty
     * @param valueFactories the value factories; never null
     * @param batchSize the ideal number of nodes that are to be included in each batch; always positive     
     * @return the sequence of nodes; null if the index cannot be used (e.g., it might be rebuilding or in an inconsistent state)
     */
    public NodeSequence fromIndex( final Index index,
                                   final Collection<Constraint> constraints,
                                   final Collection<JoinCondition> joinConditions, 
                                   final Map<String, Object> variables,
                                   final Map<String, Object> parameters,
                                   final ValueFactories valueFactories,
                                   final int batchSize ) {
        if (!index.isEnabled()) {
            return null;
        }
        final IndexConstraints indexConstraints = new IndexConstraints() {

            @Override
            public boolean hasConstraints() {
                return !constraints.isEmpty();
            }

            @Override
            public Collection<Constraint> getConstraints() {
                return constraints;
            }

            @Override
            public Map<String, Object> getVariables() {
                return variables;
            }

            @Override
            public ValueFactories getValueFactories() {
                return valueFactories;
            }

            @Override
            public Map<String, Object> getParameters() {
                return parameters;
            }

            @Override
            public Collection<JoinCondition> getJoinConditions() {
                return joinConditions;
            }
        };
        // Return a node sequence that will lazily get the results from the index ...
        return new NodeSequence() {
            private Index.Results results;
            private BatchWriter writer;
            private boolean more = true;
            private long rowCount = 0L;

            @Override
            public int width() {
                return 1;
            }

            @Override
            public long getRowCount() {
                if (!more) return rowCount;
                return -1;
            }

            @Override
            public boolean isEmpty() {
                if (rowCount > 0) return false;
                if (!more) return true; // rowCount was not > 0, but there are no more
                if (results == null) {
                    // We haven't read anything yet, so return 'false' always (even if this is not the case)
                    // so we can delay the loading of the results until really needed ...
                    return false;
                }
                readBatch();
                return rowCount == 0;
            }

            @Override
            public Batch nextBatch() {
                if (writer == null) {
                    if (!more) return null;
                    readBatch();
                }
                if (writer != null) {
                    // Return a preloaded batch if there is one ...
                    Batch preloaded = writer.popPreloadedBatch();
                    if (preloaded != null) return preloaded;
                }
                try {
                    return writer.convertToBatch(!more);
                } finally {
                    writer = null;
                }
            }

            @Override
            public void close() {
                if (results != null) {
                    results.close();
                }
            }

            protected final void readBatch() {
                if (writer == null) {
                    writer = new BatchWriter(batchSize, workspaceName, repo);
                }
                more = writer.consumeOperation(getResults());
                rowCount += writer.rowCount();
            }

            @Override
            public String toString() {
                return "(from-index " + index.getName() + " with " + constraints + ")";
            }

            private Index.Results getResults() {
                if (results != null) return results;
                // Otherwise we have to initialize the results, so have the index do the filtering based upon the constraints ...
                results = index.filter(indexConstraints);
                return results;
            }
        };
    }

    protected static class BatchWriter implements ResultWriter {
        private List<NodeKey> keys;
        private List<Float> scores;
        private Float lastScore;
        private LinkedList<Batch> preloadedBatches;
        private final RepositoryCache repo;
        private final String workspaceName;
        private final int batchSize;

        protected BatchWriter( int batchSize,
                               String workspaceName,
                               RepositoryCache repository ) {
            this.batchSize = batchSize;
            this.repo = repository;
            this.workspaceName = workspaceName;
        }

        @Override
        public void add( NodeKey nodeKey,
                         float score ) {
            keys.add(nodeKey);
            if (lastScore == null || lastScore.floatValue() != score) {
                lastScore = Float.valueOf(score);
            }
            scores.add(lastScore);
        }

        @Override
        public void add( Iterable<NodeKey> nodeKeys,
                         float score ) {
            add(nodeKeys.iterator(), score);
        }

        @Override
        public void add( Iterator<NodeKey> nodeKeys,
                         float score ) {
            final Float s = Float.valueOf(score);
            while (nodeKeys.hasNext()) {
                keys.add(nodeKeys.next());
                scores.add(s);
            }
        }

        public Batch popPreloadedBatch() {
            if (preloadedBatches == null || preloadedBatches.isEmpty()) return null;
            return preloadedBatches.pop();
        }

        public long rowCount() {
            return keys == null ? 0 : keys.size();
        }

        protected boolean consumeOperation( Index.Results operation ) {
            if (keys != null && !keys.isEmpty()) {
                // We've already read some, but have to get ready to read more ...
                Batch batch = convertToBatch(false);
                if (batch.isEmpty()) return false;
                if (preloadedBatches == null) preloadedBatches = new LinkedList<>();
                preloadedBatches.add(batch);
            }
            keys = new ArrayList<NodeKey>(batchSize);
            scores = new ArrayList<Float>(batchSize);
            return operation.getNextBatch(this, batchSize);
        }

        protected Batch convertToBatch( boolean isLast ) {
            if (keys == null || keys.isEmpty()) {
                return isLast ? null : NodeSequence.emptyBatch(workspaceName, batchSize);
            }
            try {
                return NodeSequence.batchOfKeys(keys.iterator(), scores.iterator(), keys.size(), workspaceName, repo);
            } finally {
                keys = null;
                scores = null;
            }
        }
    }

    protected static class CompositeNodeFilter implements NodeFilter {
        private final List<NodeFilter> filters;

        protected CompositeNodeFilter( NodeFilter... filters ) {
            this.filters = Arrays.asList(filters);
        }

        @Override
        public boolean includeNode( CachedNode node,
                                    NodeCache cache ) {
            for (NodeFilter filter : filters) {
                if (!filter.includeNode(node, cache)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public boolean continueProcessingChildren( CachedNode node, NodeCache cache ) {
            for (NodeFilter filter : filters) {
                if (!filter.continueProcessingChildren(node, cache)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return filters.isEmpty() ? "" : filters.toString();
        }
    }

    /**
     * Creates a node filter which doesn't include any of the nodes from the shared set in the query result. This is per
     * JSR-283/#14.16: if a query matches a descendant node of a shared set, it appears in query results only once.
     *
     * @return a new {@link org.modeshape.jcr.cache.document.NodeCacheIterator.NodeFilter} instance
     */
    protected NodeFilter sharedNodesFilter() {
        return new NodeFilter() {
            private final Set<NodeKey> shareableNodeKeys = new HashSet<>();

            @Override
            public boolean includeNode( CachedNode node,
                                        NodeCache cache ) {
                if (nodeTypes.isShareable(node.getPrimaryType(cache), node.getMixinTypes(cache))) {
                    NodeKey key = node.getKey();
                    if (shareableNodeKeys.contains(key)) {
                        return false;
                    }
                    // we're seeing the original shareable node, so we need to process it
                    shareableNodeKeys.add(key);
                    return true;
                }
                return true;
            }

            @Override
            public boolean continueProcessingChildren( CachedNode node, NodeCache cache ) {
                return !shareableNodeKeys.contains(node.getKey());
            }

            @Override
            public String toString() {
                return "(excludes-sharables)";
            }
        };
    }

    /**
     * Return an iterator over all nodes at or below the specified path in the named workspace, using the supplied filter.
     *
     * @param workspaceName the name of the workspace
     * @param path the path of the root node of the subgraph, or null if all nodes in the workspace are to be included
     * @return the iterator, or null if this workspace will return no nodes
     */
    protected NodeCacheIterator nodes( String workspaceName,
                                       Path path ) {
        // Determine which filter we should use based upon the workspace name. For the system workspace,
        // all queryable nodes are included. For all other workspaces, all queryable nodes are included except
        // for those that are actually stored in the system workspace (e.g., the "/jcr:system" nodes).
        NodeFilter nodeFilterForWorkspace = nodeFilterForWorkspace(workspaceName);
        if (nodeFilterForWorkspace == null) return null;
        // always append a shared nodes filter to the end of the workspace filter,
        // JCR #14.16 -If a query matches a descendant node of a shared set, it appears in query results only once.
        NodeFilter compositeFilter = new CompositeNodeFilter(nodeFilterForWorkspace, sharedNodesFilter());

        // Then create an iterator over that workspace ...
        NodeCache cache = repo.getWorkspaceCache(workspaceName);
        NodeKey startingNode = null;
        if (path != null) {
            CachedNode node = getNodeAtPath(path, cache);
            if (node != null) startingNode = node.getKey();
        } else {
            startingNode = cache.getRootKey();
        }
        if (startingNode != null) {
            return new NodeCacheIterator(cache, startingNode, compositeFilter);
        }
        return null;
    }

    protected NodeFilter nodeFilterForWorkspace( String workspaceName ) {
        if (this.workspaceName.equals(workspaceName)) {
            // This is the normal workspace ...
            if (includeSystemContent) {
                // We are querying the workspace AND the system content. This is actually quite typical.

                // OPTIMIZATION: since the system workspace is already projected into the other workspace's
                // node cache, we can actually just use all the queryable nodes (including the "/jcr:system" nodes)
                // in the main workspace and completely ignore the system workspace. This is better because
                // we have to only work with one workspace (rather than two) AND the filter is simpler ...
                return this.queryableFilter;
            }

            // Otherwise, we are only querying the content in the main workspace but we need to exclude any nodes
            // that actually live in the system workspace ...
            return this.queryableAndNonSystemFilter;
        }

        // It must be the system workspace ...
        return includeSystemContent ? this.queryableFilter : null;
    }

    protected CachedNode getNodeAtPath( Path path,
                                        NodeCache cache ) {
        CachedNode node = cache.getNode(cache.getRootKey());
        for (Segment segment : path) {
            ChildReference childRef = node.getChildReferences(cache).getChild(segment);
            if (childRef == null) return null;
            node = cache.getNode(childRef);
        }
        return node;
    }

    protected String getWorkspaceName( Path path ) {
        if (includeSystemContent && path.size() > 1 && JcrLexicon.SYSTEM.equals(path.getSegment(0).getName())) {
            // the path is actually in the system area ...
            return systemWorkspaceName;
        }
        return workspaceName;
    }

    private Path path( String workspaceName,
                       NodeKey key ) {
        NodeCache cache = repo.getWorkspaceCache(workspaceName);
        return cache.getNode(key).getPath(cache);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("QuerySources(");
        sb.append("repo='").append(repo).append("'");
        sb.append(" workspace='").append(workspaceName).append("'");
        if (includeSystemContent) sb.append(" system='").append(systemWorkspaceName).append("'");
        sb.append(')');
        return sb.toString();
    }
}
