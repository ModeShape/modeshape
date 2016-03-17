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
package org.modeshape.jcr.cache.document;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.transaction.TransactionManager;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.jcr.RepositoryEnvironment;
import org.modeshape.jcr.TestingEnvironment;
import org.modeshape.jcr.TestingUtil;
import org.modeshape.jcr.api.txn.TransactionManagerLookup;
import org.modeshape.jcr.txn.DefaultTransactionManagerLookup;
import org.modeshape.jcr.txn.Transactions;
import org.modeshape.schematic.Schematic;
import org.modeshape.schematic.SchematicDb;
import org.modeshape.schematic.SchematicEntry;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.EditableArray;
import org.modeshape.schematic.document.EditableDocument;
import org.modeshape.schematic.internal.annotation.FixFor;
import org.modeshape.schematic.internal.document.BasicArray;

public class LocalDocumentStoreTest {
    
    private boolean print = false;
    private RepositoryEnvironment repoEnv;
    private LocalDocumentStore localStore;
    private SchematicDb db;

    @Before
    public void beforeTest() throws Exception {
        // create a default in-memory db....
        db = Schematic.getDb(new TestingEnvironment().defaultPersistenceConfiguration());
        db.start();
        TransactionManagerLookup txLookup = new DefaultTransactionManagerLookup();
        TransactionManager tm = txLookup.getTransactionManager();
        assertNotNull("Cannot find a transaction manager", tm);
        repoEnv = new TestRepositoryEnvironment(tm, db);
        localStore = new LocalDocumentStore(db, repoEnv);
    }

    @After
    public void afterTest() {
        try {
            db.stop();
        } finally {
            try {
                TestingUtil.killTransaction(transactions().getTransactionManager());
            } finally {
                repoEnv = null;
            }
        }
    }

    protected Transactions transactions() {
        return repoEnv.getTransactions();
    }

    protected void runInTransaction(Runnable operation) {
        localStore.runInTransaction(() -> {
            operation.run();
            return null;
        }, 0);
    }

    @Test
    public void shouldStoreDocumentWithUnusedKeyAndWithNullMetadata() {
        Document doc = Schematic.newDocument("k1", "value1", "k2", 2);
        String key = "can be anything";
        runInTransaction(() -> localStore.put(key, doc));
        SchematicEntry entry = localStore.get(key);
        assertThat("Should have found the entry", entry, is(notNullValue()));

        // Verify the content ...
        Document read = entry.content();
        assertThat(read, is(notNullValue()));
        assertThat(read.getString("k1"), is("value1"));
        assertThat(read.getInteger("k2"), is(2));
        assertThat(read.containsAll(doc), is(true));
        assertThat(read.equals(doc), is(true));

        // Verify the metadata ...
        Document readMetadata = entry.getMetadata();
        assertThat(readMetadata, is(notNullValue()));
        assertThat(readMetadata.getString("id"), is(key));
    }

    @Test
    public void shouldStoreDocumentWithUnusedKeyAndWithNonNullMetadata() {
        Document doc = Schematic.newDocument("k1", "value1", "k2", 2);
        String key = "can be anything";
        runInTransaction(() -> localStore.put(key, doc));

        // Read back from the database ...
        SchematicEntry entry = localStore.get(key);
        assertThat("Should have found the entry", entry, is(notNullValue()));

        // Verify the content ...
        Document read = entry.content();
        assertThat(read, is(notNullValue()));
        assertThat(read.getString("k1"), is("value1"));
        assertThat(read.getInteger("k2"), is(2));
        assertThat(read.containsAll(doc), is(true));
        assertThat(read.equals(doc), is(true));

        // Verify the metadata ...
        Document readMetadata = entry.getMetadata();
        assert readMetadata != null;
        assert readMetadata.getString("id").equals(key);
    }

    @Test
    public void shouldStoreDocumentAndFetchAndModifyAndRefetch() throws Exception {
        // Store the document ...
        Document doc = Schematic.newDocument("k1", "value1", "k2", 2);
        String key = "can be anything";
        runInTransaction(() -> localStore.put(key, doc));
        
        // Read back from the database ...
        SchematicEntry entry = localStore.get(key);
        assertThat("Should have found the entry", entry, is(notNullValue()));

        // Verify the content ...
        Document read = entry.content();
        assertThat(read, is(notNullValue()));
        assertThat(read.getString("k1"), is("value1"));
        assertThat(read.getInteger("k2"), is(2));
        assertThat(read.containsAll(doc), is(true));
        assertThat(read.equals(doc), is(true));

        // Modify using an editor ...
        runInTransaction(() -> {
            localStore.lockDocuments(key);
            EditableDocument editable = localStore.edit(key, true);
            editable.setBoolean("k3", true);
            editable.setNumber("k4", 3.5d);
        });

        // Now re-read ...
        SchematicEntry entry2 = localStore.get(key);
        Document read2 = entry2.content();
        assertThat(read2, is(notNullValue()));
        assertThat(read2.getString("k1"), is("value1"));
        assertThat(read2.getInteger("k2"), is(2));
        assertThat(read2.getBoolean("k3"), is(true));
        assertThat(read2.getDouble("k4") > 3.4d, is(true));
    }

    @Test
    public void shouldStoreDocumentAndFetchAndModifyAndRefetchUsingTransaction() throws Exception {
        // Store the document ...
        Document doc = Schematic.newDocument("k1", "value1", "k2", 2);
        String key = "can be anything";
        runInTransaction(() -> localStore.put(key, doc));
        
        // Read back from the database ...
        SchematicEntry entry = localStore.get(key);
        assertThat("Should have found the entry", entry, is(notNullValue()));

        // Verify the content ...
        Document read = entry.content();
        assertThat(read, is(notNullValue()));
        assertThat(read.getString("k1"), is("value1"));
        assertThat(read.getInteger("k2"), is(2));
        assertThat(read.containsAll(doc), is(true));
        assertThat(read.equals(doc), is(true));

        // Modify using an editor ...
        runInTransaction(() -> {
            localStore.lockDocuments(key);
            EditableDocument editable = localStore.edit(key, true);
            editable.setBoolean("k3", true);
            editable.setNumber("k4", 3.5d);
        });

        // Now re-read ...
        SchematicEntry entry2 = localStore.get(key);
        Document read2 = entry2.content();
        assertThat(read2, is(notNullValue()));
        assertThat(read2.getString("k1"), is("value1"));
        assertThat(read2.getInteger("k2"), is(2));
        assertThat(read2.getBoolean("k3"), is(true));
        assertThat(read2.getDouble("k4") > 3.4d, is(true));
    }

    @FixFor( "MODE-1734" )
    @Test
    public void shouldAllowMultipleConcurrentWritersToUpdateEntryInSerialFashion() throws Exception {
        Document doc = Schematic.newDocument("k1", "value1", "k2", 2);
        final String key = "can be anything";
        runInTransaction(() -> localStore.put(key, doc));
        SchematicEntry entry = localStore.get(key);
        assertThat("Should have found the entry", entry, is(notNullValue()));
        // Start two threads that each attempt to edit the document ...
        ExecutorService executors = Executors.newCachedThreadPool();
        final CountDownLatch latch = new CountDownLatch(1);
        Future<Void> f1 = executors.submit(() -> {
            latch.await(); // synchronize ...
            runInTransaction(() -> {
                print("Began txn1");
                while (!localStore.lockDocuments(key)) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.interrupted();
                        fail("Cannot acquire lock...");
                    }
                }
                EditableDocument editor = localStore.edit(key, true);
                editor.setNumber("k2", 3); // update an existing field
                print(editor);
                print("Committing txn1");
            });
            return null;
        });
        Future<Void> f2 = executors.submit(() -> {
            latch.await(); // synchronize ...
            runInTransaction(() -> {
                print("Began txn2");
                while (!localStore.lockDocuments(key)) {
                    try {
                        Thread.sleep(1);
                    } catch (InterruptedException e) {
                        Thread.interrupted();
                        fail("Cannot acquire lock...");
                    }
                }
                EditableDocument editor = localStore.edit(key, true);
                editor.setNumber("k3", 3); // add a new field
                print(editor);
                print("Committing txn2");
            });
            return null;
        });
        // print = true;
        // Start the threads ...
        latch.countDown();
        // Wait for the threads to die ...
        f1.get();
        f2.get();
        // System.out.println("Completed all threads");
        // Now re-read ...
        runInTransaction(() -> {
            Document read = localStore.get(key).content();
            assertThat(read, is(notNullValue()));
            assertThat(read.getString("k1"), is("value1"));
            assertThat(read.getInteger("k3"), is(3)); // Thread 2 is last, so this should definitely be there
            assertThat(read.getInteger("k2"), is(3)); // Thread 1 is first, but still shouldn't have been overwritten
        });
    }
    
    @Test
    public void multipleWritersShouldBeExclusivelyLockedOnTheSameKey() throws Exception {
        String rootKey = "3293af3317f1e7/";
        Document root = Schematic.newDocument("children", new BasicArray());
        runInTransaction(() -> localStore.put(rootKey, root));

        int threadCount = 150;
        int childrenForEachThread = 10;
        ForkJoinPool forkJoinPool = new ForkJoinPool(threadCount);
        forkJoinPool.submit(() -> IntStream.range(0, threadCount).parallel().forEach(i -> insertParentWithChildren(rootKey, 
                                                                                                                   childrenForEachThread)))
                    .get();

        Document rootDoc = localStore.get(rootKey).content();
        List<?> children = rootDoc.getArray("children");
        assertEquals("children corrupted", threadCount * childrenForEachThread, children.size());
        assertEquals(threadCount * childrenForEachThread + 1, localStore.keys().size());
    }
    
    private void insertParentWithChildren(String rootKey, int childrenForEachThread) {
        List<String> newKeys = IntStream.range(0, childrenForEachThread).mapToObj(
                nr -> UUID.randomUUID().toString()).collect(Collectors.toList());
        newKeys.add(rootKey);
        runInTransaction(() -> {
            if (localStore.lockDocuments(newKeys.toArray(new String[newKeys.size()]))) {
                newKeys.remove(rootKey);
                EditableDocument rootDoc = localStore.edit(rootKey, false);
                EditableArray children = rootDoc.getArray("children");
                newKeys.forEach(newKey -> {
                    EditableDocument newChild = Schematic.newDocument("name", Thread.currentThread().getName(),
                                                                      "key", newKey);
                    children.add(newChild);
                    localStore.put(newKey, newChild);
                });
            } else {
                fail("Should've obtained key by now");
            }
        });
    }

    protected void print( Object obj ) {
        if (print) {
            System.out.printf("%s - %s%n", Thread.currentThread().getName(), obj);
        }
    }

}
