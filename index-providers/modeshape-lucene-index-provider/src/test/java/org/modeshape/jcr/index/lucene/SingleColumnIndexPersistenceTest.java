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
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
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
        return new SingleColumnIndex(name + "-single-valued", config, PropertiesTestUtil.ALLOWED_PROPERTIES, context);
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
    public void singleIndexCrudPerformance() {
        int nodeCount = 100000;
        int valuesPerProperty = 2;
        int batchSize = 1000;
        runPerfTestForIndex(nodeCount, valuesPerProperty, batchSize, this.index);
    }

    @Test
    @Ignore("perf test")
    public void multiIndexCrudPerformance() throws Exception {
        final int nodeCount = 100000;
        final int valuesPerProperty = 1;
        final int batchSize = 1000;
        int indexesCount = 20;
        ExecutorService executorService = Executors.newFixedThreadPool(indexesCount);
        List<Future<?>> result = new ArrayList<>(indexesCount);
        for (int i = 0; i < indexesCount; i++) {
            LuceneIndex index = createIndex("index" + i);
            IndexThread<Void> perfThread = new IndexThread<>(index, new IndexOperation<Void>() {
                @Override
                public Void execute( final LuceneIndex index ) throws Exception {
                    runPerfTestForIndex(nodeCount, valuesPerProperty, batchSize, index);
                    return null;
                }
            });
            result.add(executorService.submit(perfThread));
        }

        try {
            for (Future<?> future : result) {
                try {
                    future.get(3, TimeUnit.MINUTES);
                } catch (TimeoutException e) {
                    future.cancel(true);
                }
            }
        } finally {
            executorService.shutdownNow();
        }
    }

    private void runPerfTestForIndex( int nodeCount, int valuesPerProperty, int batchSize, LuceneIndex index ) {
        List<String> nodeKeys = new ArrayList<>();
        long start = System.nanoTime();
        // insert
        for (int i = 0; i < nodeCount; i++) {
            String nodeKey = UUID.randomUUID().toString();
            nodeKeys.add(nodeKey);
            addMultiplePropertiesToSameNode(index, nodeKey, valuesPerProperty, PropertyType.STRING);
            if (i > 0 && i % batchSize == 0) {
                index.commit();
                System.out.println("Inserted " + i + " nodes");
            }
        }
        index.commit();
        assertEquals(nodeCount, index.estimateTotalCount());
        long insertTime = TimeUnit.SECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        System.out.println("(" + index.getName() + ") Total time to insert " + nodeCount + " nodes: " + (insertTime / 60d) + " minutes");


        // update
        start = System.nanoTime();
        for (int i = 0; i < nodeCount; i++) {
            String nodeKey = nodeKeys.get(i);
            addMultiplePropertiesToSameNode(index, nodeKey, valuesPerProperty, PropertyType.STRING);
            if (i > 0 && i % batchSize == 0) {
                index.commit();
                System.out.println("Updated "  + i + " nodes");
            }
        }
        index.commit();
        assertEquals(nodeCount, index.estimateTotalCount());
        long updateTime = TimeUnit.SECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        System.out.println("(" + index.getName() + ") Total time to update " + nodeCount + " nodes: " + (updateTime / 60d) + " minutes");

        // delete
        start = System.nanoTime();
        for (int i = 0; i < nodeCount; i++) {
            String nodeKey = nodeKeys.get(i);
            index.remove(nodeKey);
            if (i > 0 && i % batchSize == 0) {
                index.commit();
                System.out.println("Removed "  + i + " nodes");
            }
        }
        index.commit();
        assertEquals(0, index.estimateTotalCount());
        long deleteTime = TimeUnit.SECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        System.out.println("(" + index.getName() + ") Total time to delete " + nodeCount + " nodes: " + (deleteTime / 60d) + " minutes");
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
