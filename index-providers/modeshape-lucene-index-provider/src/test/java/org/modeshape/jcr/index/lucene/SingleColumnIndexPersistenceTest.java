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
package org.modeshape.jcr.index.lucene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.jcr.value.PropertyType;

/**
 * Tests CRUD operations on the {@link SingleColumnIndex} index.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class SingleColumnIndexPersistenceTest extends AbstractIndexPersistenceTest {

    @Override
    protected LuceneIndex createIndex( String name ) {
        return new SingleColumnIndex(name + "-single-valued", "default", config, PropertiesTestUtil.ALLOWED_PROPERTIES, context);
    }

    @Test
    public void shouldClearAllData() throws Exception {
        assertEquals(0, index.estimateTotalCount());
        IndexedProperty property = newProperty(PropertyType.STRING);
        index.add(UUID.randomUUID().toString(), property.getName(), property.getValue());
        index.commit();
        assertEquals(1, index.estimateTotalCount());
        index.clearAllData();
        assertEquals(0, index.estimateTotalCount());
    }

    @Test
    public void shouldAddNodesWithSingleValues() throws Exception {
        assertEquals(0, index.estimateTotalCount());
        assertTrue(index.requiresReindexing());

        List<PropertyType> types = new ArrayList<>(Arrays.asList(PropertyType.values()));
        types.remove(PropertyType.OBJECT);
        for (PropertyType type : types) {
            String nodeKey = UUID.randomUUID().toString();
            IndexedProperty property = newProperty(type);
            index.add(nodeKey, property.getName(), property.getValue());
        }
        index.commit();
        assertEquals(types.size(), index.estimateTotalCount());
        assertFalse(index.requiresReindexing());
    }

    @Test
    public void shouldAddNodesWithMultipleValues() throws Exception {
        PropertyType[] types = new PropertyType[] {PropertyType.BOOLEAN, PropertyType.LONG, PropertyType.PATH};
        addMultipleNodes(index, 3, types);
        assertEquals(types.length, index.estimateTotalCount());
    }

    @Test
    public void shouldUpdateValueForSameNodes() throws Exception {
        String nodeKey1 = UUID.randomUUID().toString();
        addMultiplePropertiesToSameNode(index, nodeKey1, 1, PropertyType.STRING);
        addMultiplePropertiesToSameNode(index, nodeKey1, 1, PropertyType.STRING);
        index.commit();

        String nodeKey2 = UUID.randomUUID().toString();
        addMultiplePropertiesToSameNode(index, nodeKey2, 1, PropertyType.STRING);
        addMultiplePropertiesToSameNode(index, nodeKey2, 1, PropertyType.STRING);
        index.commit();

        assertEquals(2, index.estimateTotalCount());
    }

    @Test
    public void shouldRemoveSingleValue() throws Exception {
        String nodeKey = UUID.randomUUID().toString();
        String propertyName = addMultiplePropertiesToSameNode(index, nodeKey, 1, PropertyType.STRING);
        index.commit();

        index.remove(nodeKey, propertyName);
        index.commit();
        assertEquals(0, index.estimateTotalCount());
    }

    @Test
    public void shouldRemoveAllValues() throws Exception {
        String nodeKey = UUID.randomUUID().toString();
        addMultiplePropertiesToSameNode(index, nodeKey, 2, PropertyType.LONG);
        index.commit();

        index.remove(nodeKey);
        index.commit();
        assertEquals(0, index.estimateTotalCount());
    }

    @Test
    public void shouldUpdateValuesBetweenRestarts() throws Exception {
        String nodeKey = UUID.randomUUID().toString();
        // a new node to the index
        addMultiplePropertiesToSameNode(index, nodeKey, 2, PropertyType.LONG);
        index.commit();

        // restart the index without clearing the data
        index.shutdown(false);
        index = defaultIndex();

        // and check that data is still there
        assertFalse(index.requiresReindexing());
        assertEquals(1, index.estimateTotalCount());
        addMultiplePropertiesToSameNode(index, nodeKey, 1, PropertyType.STRING);
        index.commit();
        assertEquals(1, index.estimateTotalCount());

        //add a new document
        nodeKey = UUID.randomUUID().toString();
        addMultiplePropertiesToSameNode(index, nodeKey, 2, PropertyType.DECIMAL);
        index.commit();

        // restart the index without clearing the data
        index.shutdown(false);
        index = defaultIndex();

        // and check the second update
        assertFalse(index.requiresReindexing());
        assertEquals(2, index.estimateTotalCount());
        // remove a node
        index.remove(nodeKey);
        index.commit();

        // restart the index without clearing the data
        index.shutdown(false);
        index = defaultIndex();
        // and check the removal
        assertFalse(index.requiresReindexing());
        assertEquals(1, index.estimateTotalCount());
        addMultiplePropertiesToSameNode(index, nodeKey, 2, PropertyType.DECIMAL);
        index.commit();
        assertEquals(2, index.estimateTotalCount());
    }

    @Test
    @Ignore("perf test")
    public void singleThreadIndexCrudPerformance() throws Exception {
        int nodeCount = 100000;
        int valuesPerProperty = 2;
        int batchSize = 1000;
        List<String> nodeKeys = insertNodes(nodeCount, valuesPerProperty, batchSize, index);
        assertEquals(nodeCount, index.estimateTotalCount());
        
        updateNodes(valuesPerProperty, batchSize, index, nodeKeys, null);
        assertEquals(nodeCount, index.estimateTotalCount());
        
        removeNodes(batchSize, index, nodeKeys, null);
        assertEquals(0, index.estimateTotalCount());
    }

    @Test
    @Ignore("perf test")
    public void multiThreadIndexCrudPerformance() throws Exception {
        final int nodeCount = 100000;
        final int valuesPerProperty = 1;
        final int batchSize = 1000;
        final int threadCount = 4;
        final int nodesPerThread = nodeCount / threadCount;

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        final List<String> nodeKeys = new ArrayList<>();
        final List<Future<List<String>>> futures = new ArrayList<>();
        //insert
        for (int i = 0; i < threadCount; i++) {
            futures.add(executorService.submit(new Callable<List<String>>() {
                @Override
                public List<String> call() throws Exception {
                    return insertNodes(nodesPerThread, valuesPerProperty, batchSize, index);
                }
            }));
        }
        for (Future<List<String>> future : futures) {
            nodeKeys.addAll(future.get());
        }
        assertEquals(nodeCount, index.estimateTotalCount());
        assertEquals(nodeCount, nodeKeys.size());

        final CyclicBarrier barrier = new CyclicBarrier(threadCount + 1);
        //update
        for (int i = 0; i < threadCount; i++) {
            final int startIdx = i * nodesPerThread;
            final int endIdx = Math.min(nodeKeys.size(), startIdx + nodesPerThread);
            executorService.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    updateNodes(valuesPerProperty, batchSize, index, nodeKeys.subList(startIdx, endIdx), barrier);
                    return null;
                }
            });
        }
        barrier.await();
        assertEquals(nodeCount, index.estimateTotalCount());
        
        //remove
        barrier.reset();
        for (int i = 0; i < threadCount; i++) {
            final int startIdx = i * nodesPerThread;
            final int endIdx = Math.min(nodeKeys.size(), startIdx + nodesPerThread);
            executorService.submit(new Callable<Void>() {
                @Override
                public Void call() throws Exception {
                    removeNodes(batchSize, index, nodeKeys.subList(startIdx, endIdx), barrier);
                    return null;
                }
            });
        }
        barrier.await();
        assertEquals(0, index.estimateTotalCount());
    }

    private void removeNodes( int batchSize, LuceneIndex index, List<String> nodeKeys, CyclicBarrier barrier )
            throws Exception{
        long start;
        start = System.nanoTime();
        for (int i = 0; i < nodeKeys.size(); i++) {
            String nodeKey = nodeKeys.get(i);
            index.remove(nodeKey);
            if (i > 0 && i % batchSize == 0) {
                index.commit();
                System.out.println(Thread.currentThread().getName() + " removed "  + i + " nodes");
            }
        }
        index.commit();
        long deleteTime = TimeUnit.SECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        System.out.println(Thread.currentThread().getName() + ": (" + index.getName() + ") Total time to delete " + nodeKeys.size() + " nodes: " + (deleteTime / 60d) + " minutes");
        if (barrier != null) {
            barrier.await();
        }
    }

    private void updateNodes(int valuesPerProperty, int batchSize, LuceneIndex index, List<String> nodeKeys,
                              CyclicBarrier barrier) throws Exception {
        long start;
        start = System.nanoTime();
        for (int i = 0; i < nodeKeys.size(); i++) {
            String nodeKey = nodeKeys.get(i);
            addMultiplePropertiesToSameNode(index, nodeKey, valuesPerProperty, PropertyType.STRING);
            if (i > 0 && i % batchSize == 0) {
                index.commit();
                System.out.println(Thread.currentThread().getName() + " updated "  + i + " nodes");
            }
        }
        index.commit();
   
        long updateTime = TimeUnit.SECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        System.out.println(Thread.currentThread().getName() + ": (" + index.getName() + ") Total time to update " + nodeKeys.size() + " nodes: " + (updateTime / 60d) + " minutes");
        if (barrier != null) {
            barrier.await();
        }
    }

    private List<String> insertNodes( int nodeCount, int valuesPerProperty, int batchSize, LuceneIndex index ) 
            throws Exception {
        List<String> nodeKeys = new ArrayList<>();
        long start = System.nanoTime();
        // insert
        for (int i = 0; i < nodeCount; i++) {
            String nodeKey = UUID.randomUUID().toString();
            nodeKeys.add(nodeKey);
            addMultiplePropertiesToSameNode(index, nodeKey, valuesPerProperty, PropertyType.STRING);
            if (i > 0 && i % batchSize == 0) {
                index.commit();
                System.out.println(Thread.currentThread().getName() + " inserted " + i + " nodes");
            }
        }
        index.commit();
        long insertTime = TimeUnit.SECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        System.out.println(Thread.currentThread().getName() + ": (" + index.getName() + ") Total time to insert " + nodeCount + " nodes: " + (insertTime / 60d) + " minutes");
        return nodeKeys;
    }

    private void addMultipleNodes( LuceneIndex index, int propertiesCount, PropertyType[] types ) {
        for (PropertyType type : types) {
            String nodeKey = UUID.randomUUID().toString();
            addMultiplePropertiesToSameNode(index, nodeKey, propertiesCount, type);
        }
        index.commit();
    }

    private static interface IndexOperation<T> {
        T execute(final LuceneIndex index) throws Exception;
    }

    private static class IndexThread<T> implements Callable<T> {
        private final LuceneIndex index;
        private final IndexOperation<T> operation;

        protected IndexThread( LuceneIndex index, IndexOperation<T> operation) {
            this.index = index;
            this.operation = operation;
        }

        @Override
        public T call() throws Exception {
            try {
                return operation.execute(index);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } finally {
                index.shutdown(true);
            }
        }
    }
}
