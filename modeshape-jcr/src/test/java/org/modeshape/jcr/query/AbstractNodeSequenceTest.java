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
package org.modeshape.jcr.query;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.Before;
import org.modeshape.jcr.bus.RepositoryChangeBus;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.document.AbstractNodeCacheTest;
import org.modeshape.jcr.cache.document.DocumentStore;
import org.modeshape.jcr.cache.document.DocumentTranslator;
import org.modeshape.jcr.cache.document.LocalDocumentStore;
import org.modeshape.jcr.cache.document.WorkspaceCache;
import org.modeshape.jcr.query.NodeSequence.Batch;
import org.modeshape.jcr.query.NodeSequence.RowAccessor;
import org.modeshape.jcr.query.NodeSequence.RowFilter;
import org.modeshape.jcr.query.RowExtractors.ExtractFromRow;

/**
 * An abstract base class for test classes that operate against {@link NodeSequence}.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class AbstractNodeSequenceTest extends AbstractNodeCacheTest {

    private ExecutorService executor;
    private RepositoryChangeBus changeBus;

    @Override
    protected NodeCache createCache() {
        executor = Executors.newCachedThreadPool();
        changeBus = new RepositoryChangeBus("repo", executor);
        ConcurrentMap<NodeKey, CachedNode> nodeCache = new ConcurrentHashMap<NodeKey, CachedNode>();
        DocumentStore documentStore = new LocalDocumentStore(schematicDb);
        DocumentTranslator translator = new DocumentTranslator(context, documentStore, 100L);
        WorkspaceCache workspaceCache = new WorkspaceCache(context, "repo", "ws", null, documentStore, translator, ROOT_KEY_WS1,
                                                           nodeCache, changeBus, null);
        loadJsonDocuments(resource(resourceNameForWorkspaceContentDocument()));
        return workspaceCache;
    }

    @Override
    protected void shutdownCache( NodeCache cache ) {
        super.shutdownCache(cache);
        executor.shutdown();
    }

    @Override
    @Before
    public void beforeEach() {
        super.beforeEach();
    }

    protected String workspaceName() {
        return ((WorkspaceCache)cache).getWorkspaceName();
    }

    protected NodeSequence allNodes() {
        return allNodes(1.0f);
    }

    protected long countRows( NodeSequence sequence ) {
        try {
            long count = 0;
            Batch batch = null;
            while ((batch = sequence.nextBatch()) != null) {
                while (batch.hasNext()) {
                    batch.nextRow();
                    ++count;
                }
            }
            return count;
        } finally {
            sequence.close();
        }
    }

    protected void print( String msg,
                          NodeSequence sequence,
                          ExtractFromRow firstExtractor,
                          ExtractFromRow... extractors ) {
        if (!print()) return;
        System.out.println(msg);
        try {
            Batch batch = null;
            while ((batch = sequence.nextBatch()) != null) {
                while (batch.hasNext()) {
                    batch.nextRow();
                    System.out.print(" ");
                    System.out.print(firstExtractor.getValueInRow(batch));
                    for (ExtractFromRow extractor : extractors) {
                        System.out.print(" ");
                        System.out.print(extractor.getValueInRow(batch));
                    }
                    System.out.println();
                }
            }
        } finally {
            sequence.close();
        }
    }

    protected void printDetailed( String msg,
                                  NodeSequence sequence,
                                  ExtractFromRow... extractors ) {
        if (!print()) return;
        System.out.println(msg);
        try {
            Batch batch = null;
            while ((batch = sequence.nextBatch()) != null) {
                while (batch.hasNext()) {
                    batch.nextRow();
                    System.out.print(rowAsString(batch, " "));
                    for (ExtractFromRow extractor : extractors) {
                        System.out.print(" ");
                        System.out.print(extractor.getValueInRow(batch));
                    }
                    System.out.println();
                }
            }
        } finally {
            sequence.close();
        }
    }

    protected NodeSequence allNodes( float score ) {
        return NodeSequence.withNodeKeys(cache.getAllNodeKeys(), -1, score, workspaceName(), cache);
    }

    protected NodeSequence allNodes( float score,
                                     int sizeOfBatches ) {
        return multipleBatchesOf(cache.getAllNodeKeys(), score, sizeOfBatches);
    }

    protected NodeSequence multipleBatchesOf( Iterator<NodeKey> keys,
                                              float score,
                                              int sizeOfBatches ) {
        List<NodeKey> listOfKeys = new ArrayList<>();
        List<Batch> batches = new ArrayList<>();
        while (keys.hasNext()) {
            for (int i = 0; i != sizeOfBatches; ++i) {
                if (!keys.hasNext()) break;
                listOfKeys.add(keys.next());
            }
            batches.add(NodeSequence.batchOfKeys(listOfKeys.iterator(), listOfKeys.size(), score, workspaceName(), cache));
            listOfKeys = new ArrayList<>();
        }
        return NodeSequence.withBatches(batches, 1);
    }

    protected RowFilter rowFilterOfNodesWithKeysHavingWorkspaceKey( final int indexInRow,
                                                                    final String workspaceKey ) {
        return new RowFilter() {
            @Override
            public boolean isCurrentRowValid( Batch batch ) {
                CachedNode node = batch.getNode(indexInRow);
                if (node == null) return true;
                return node.getKey().getWorkspaceKey().equals(workspaceKey);
            }
        };
    }

    protected static interface Verifier {
        void verify( RowAccessor currentRow );

        void complete();
    }

    protected void assertRowsSatisfy( NodeSequence nodeSequence,
                                      Verifier verifier ) {
        // Iterate over the batches ...
        try {
            Batch batch = null;
            while ((batch = nodeSequence.nextBatch()) != null) {
                while (batch.hasNext()) {
                    batch.nextRow();
                    verifier.verify(batch);
                }
            }
            verifier.complete();
        } finally {
            nodeSequence.close();
        }
    }

    protected String rowAsString( RowAccessor row ) {
        return rowAsString(row, null);
    }

    protected String rowAsString( RowAccessor row,
                                  String prefix ) {
        StringBuilder sb = new StringBuilder();
        if (prefix != null) sb.append(prefix);
        sb.append('[');
        for (int i = 0; i != row.width(); ++i) {
            if (i != 0) sb.append(" | ");
            CachedNode node = row.getNode(i);
            sb.append(node != null ? node.getKey() : "null");
            sb.append('(');
            sb.append(row.getScore(i));
            sb.append(')');
        }
        sb.append(']');
        return sb.toString();
    }
}
