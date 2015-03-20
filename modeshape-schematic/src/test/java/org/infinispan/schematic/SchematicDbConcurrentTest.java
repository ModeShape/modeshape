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
package org.infinispan.schematic;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.infinispan.AdvancedCache;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Test class which runs various concurrent tests against {@link org.infinispan.schematic.SchematicDb} and Infinispan.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@SuppressWarnings( "synthetic-access" )
@Ignore("This shouldn't be normally run and is only present to validate MODE-2280")
public class SchematicDbConcurrentTest extends AbstractSchematicDbTest {

    private AdvancedCache<Object, Object> rawCache;
    private static final AtomicInteger THREAD_IDX = new AtomicInteger(0);

    @Override
    public void beforeTest() {
        try {
            TestUtil.delete(new File("target/concurrent_load"));

            cm = new DefaultCacheManager(getClass().getClassLoader().getResourceAsStream(
                    "infinispan/concurrent-load-infinispan-cache.xml"));
            tm = cm.getCache().getAdvancedCache().getTransactionManager();
            // Now create the SchematicDb ...
            db = Schematic.get(cm, "documents");
            rawCache = cm.getCache("raw", true).getAdvancedCache();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void afterTest() {
        THREAD_IDX.set(0);
        super.afterTest();
    }

    @Test
    @FixFor( "MODE-2280" )
    public void rawCacheShouldHandleOneWriterAndMultipleReadersRepeatedly() throws Exception {
        afterTest();
        int repeatCount = 100;

        for (int i = 0; i < repeatCount; ++i) {
            print ("Run #" + (i + 1));
            beforeTest();
            rawCacheShouldHandleOneWriterAndMultipleReaders();
            afterTest();
        }
    }

    @Test
    @FixFor( "MODE-2280" )
    public void rawCacheShouldHandleOneWriterAndMultipleReaders() throws Exception {
        int totalNumberOfChildrenDocuments = 500;
        int saveBatchSize = 20;
        int modifierThreadsCount = 500;

        ExecutorService executorService = Executors.newCachedThreadPool(new ThreadFactory() {
            @Override
            public Thread newThread( Runnable r ) {
                return new Thread(r, "Thread_" + THREAD_IDX.incrementAndGet());
            }
        });

        int threadIdx = 1;
        long start = System.nanoTime();
        try {
            storeList(Collections.<String>emptyList());

            List<Future<ReaderResult>> threadResults = new ArrayList<Future<ReaderResult>>();
            Set<String> listElements = new LinkedHashSet<>();
            for (int i = 0; i != totalNumberOfChildrenDocuments; ++i) {
                listElements.add(UUID.randomUUID().toString());
                if (i >= saveBatchSize && i % saveBatchSize == 0) {
                    print("Saving  batch " + i);
                    storeList(listElements);
                    print("...saved; at " + System.currentTimeMillis());
                    print("...firing threads " + threadIdx + " through " + (threadIdx + modifierThreadsCount));
                    threadIdx += modifierThreadsCount + 1;
                    // fire up the threads that will read each of the newly added children
                    for (int j = 0; j < modifierThreadsCount; j++) {
                        threadResults.add(executorService.submit(new ListReader(
                                new LinkedHashSet<String>(listElements)
                        )));
                    }
                    listElements.clear();
                }
            }
            if (!listElements.isEmpty()) {
                print("Saving final batch");
                storeList(listElements);
                print("...saved; at " + System.currentTimeMillis());

                threadResults.add(executorService.submit(new ListReader(
                        new LinkedHashSet<String>(listElements)
                )));
            }
            print("Total time to insert records=" + (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) / 1000d)
                  + " seconds with batch size=" + saveBatchSize);

            print("Waiting for " + threadResults.size() + " threads to complete");
            for (Future<ReaderResult> future : threadResults) {
                ReaderResult result = future.get(1, TimeUnit.MINUTES);
                result.validate();
            }
        } catch (java.util.concurrent.TimeoutException te) {
            fail("Task never finished completion");
        } finally {
            executorService.shutdown();
            assertTrue(executorService.awaitTermination(1, TimeUnit.MINUTES));
        }
    }

    @SuppressWarnings( "unchecked" )
    private void storeList( Iterable<String> ids ) throws Exception {
        tm.begin();
        List<String> oldList = (List<String>)rawCache.get("list");

        List<String> newList = new ArrayList<>();
        if (oldList != null) {
            newList.addAll(oldList);
        }
        for (String childId : ids) {
            newList.add(childId);
            // do a put so that we eventually trigger the eviction thread
            rawCache.put(childId, childId);
        }
        rawCache.put("list", newList);
        tm.commit();
        print("wrote list reference: " + System.identityHashCode(rawCache.get("list")));
    }

    @Test
    @FixFor( "MODE-2280" )
    public void shouldHandleMultipleReadersRepeatedly() throws Exception {
        afterTest();
        int repeatCount = 100;

        for (int i = 0; i < repeatCount; ++i) {
            print ("Run #" + (i + 1));
            beforeTest();
            shouldHandleOneWriterAndMultipleReaders();
            afterTest();
        }
    }

    @Test
    @FixFor( "MODE-2280" )
    public void shouldHandleOneWriterAndMultipleReaders() throws Exception {
        int totalNumberOfChildrenDocuments = 500;
        int saveBatchSize = 20;
        int modifierThreadsCount = 500;

        ExecutorService executorService = Executors.newFixedThreadPool(modifierThreadsCount);

        long start = System.nanoTime();
        try {
            String parentId = UUID.randomUUID().toString();
            Document parent = newDocument(parentId, "parent");
            persistDocument(parent);

            List<Future<ReaderResult>> threadResults = new ArrayList<Future<ReaderResult>>();
            Set<Document> childrenPerBatch = new LinkedHashSet<Document>();
            Set<String> idsPerBatch = new LinkedHashSet<>();
            for (int i = 0; i != totalNumberOfChildrenDocuments; ++i) {
                String childKey = UUID.randomUUID().toString();
                childrenPerBatch.add(newDocument(childKey, "child_" + i));
                idsPerBatch.add(childKey);
                if (i >= saveBatchSize && i % saveBatchSize == 0) {
                    print("Saving  batch " + i);
                    addChildren(parentId, childrenPerBatch);
                    print("...saved; at " + System.currentTimeMillis());
                    // fire up the threads that will read each of the newly added children
                    for (int j = 0; j < modifierThreadsCount; j++) {
                        threadResults.add(executorService.submit(new ChildrenReader(
                                new LinkedHashSet<String>(idsPerBatch),
                                parentId
                        )));
                    }
                    idsPerBatch.clear();
                    childrenPerBatch.clear();
                }
            }
            if (!childrenPerBatch.isEmpty()) {
                print("Saving final batch");
                addChildren(parentId, childrenPerBatch);
                print("...saved; at " + System.currentTimeMillis());

                threadResults.add(executorService.submit(new ChildrenReader(
                        new LinkedHashSet<String>(idsPerBatch),
                        parentId
                )));
            }
            print("Total time to insert records=" + (TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start) / 1000d)
                  + " seconds with batch size=" + saveBatchSize);

            print("Waiting for " + threadResults.size() + " threads to complete");
            for (Future<ReaderResult> future : threadResults) {
                ReaderResult result = future.get(1, TimeUnit.MINUTES);
                result.validate();
            }
        } catch (java.util.concurrent.TimeoutException te) {
            fail("Task never finished completion");
        } finally {
            executorService.shutdown();
            assertTrue(executorService.awaitTermination(1, TimeUnit.MINUTES));
        }
    }

    private void addChildren( String parentKey, Iterable<Document> children ) throws Exception {
        tm.begin();
        EditableDocument parent = db.editContent(parentKey, false);
        EditableArray childrenArray = parent.getOrCreateArray("children");
        for (Document child : children) {
            String childName = child.getDocument(SchematicEntry.FieldName.CONTENT).getString("name");
            String childId = child.getDocument(SchematicEntry.FieldName.METADATA).getString(SchematicEntry.FieldName.ID);
            childrenArray.add(Schematic.newDocument("childId", childId, "childName", childName));

            db.put(child);
        }
        tm.commit();
    }

    private static void print( String message ) {
        System.out.println(Thread.currentThread().getName() + " " + message);
    }

    private Document newDocument( String id, String name ) {
        return Schematic.newDocument(
                SchematicEntry.FieldName.METADATA, Schematic.newDocument(SchematicEntry.FieldName.ID, id),
                SchematicEntry.FieldName.CONTENT, Schematic.newDocument("name", name, "children", Schematic.newArray()))
                        .unwrap();
    }

    private void persistDocument( Document document ) throws Exception {
        tm.begin();
        db.put(document);
        tm.commit();
    }

    private final class ListReader implements Callable<ReaderResult> {
        private final Set<String> listElements;

        private ListReader( Set<String> listElements ) {
            this.listElements = listElements;
        }

        @Override
        @SuppressWarnings( "unchecked" )
        public ReaderResult call() throws Exception {
            List<String> storedList = (List<String>)rawCache.get("list");
            for (String listElement : listElements) {
                if (!storedList.contains(listElement)) {
                    print("read list reference: " + System.identityHashCode(storedList));
                    return new ReaderResult("Element " + listElement + " not found in the list");
                }
            }
            return ReaderResult.EMPTY;
        }
    }

    private final class ChildrenReader implements Callable<ReaderResult> {

        private final Set<String> childrenIds;
        private final String parentId;

        private ChildrenReader( Set<String> childrenIds, String parentId ) {
            this.childrenIds = childrenIds;
            this.parentId = parentId;
        }

        @Override
        public ReaderResult call() throws Exception {
            if (!db.containsKey(parentId)) {
                return new ReaderResult("Parent " + parentId + " not found in DB");
            }
            Document parentDocument = db.get(parentId).getContent();
            List<?> children = parentDocument.getArray("children");
            Set<String> storedChildrenIds = new LinkedHashSet<>();
            for (Object child : children) {
                Document childDocument = (Document)child;
                storedChildrenIds.add(childDocument.getString("childId"));
            }
            for (String childId : childrenIds) {
                if (!db.containsKey(childId)) {
                    return new ReaderResult("Child " + childId + " not found in DB");
                }
                Document childDocument = db.get(childId).getContent();
                String name = childDocument.getString("name");
                if (!name.startsWith("child")) {
                    return new ReaderResult("Invalid child name: " + name);
                }
                if (!storedChildrenIds.contains(childId)) {
                    return new ReaderResult("Child " + childId + " not found in the parent's children array");
                }
            }
            return ReaderResult.EMPTY;
        }
    }

    private static final class ReaderResult {
        private static final ReaderResult EMPTY = new ReaderResult(null);
        private final String errorMessage;

        private ReaderResult( String errorMessage ) {
            this.errorMessage = errorMessage;
            if (errorMessage != null) {
                print(errorMessage);
            }
        }

        private void validate() {
            if (errorMessage == null) {
                return;
            }
            throw new RuntimeException(errorMessage);
        }
    }
}
