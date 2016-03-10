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
package org.modeshape.schematic;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertNull;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.document.EditableDocument;
import org.modeshape.schematic.document.Json;
import org.modeshape.schematic.document.ParsingException;
import org.modeshape.schematic.internal.document.BasicDocument;

/**
 * Base class for the different {@link SchematicDb} implementation.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public abstract class AbstractSchematicDBTest {

    protected static final Document DEFAULT_CONTENT ;
    
    private static final String VALUE_FIELD = "value";
    
    protected SchematicDb db;
    protected boolean print = false;

    static {
        try {
            DEFAULT_CONTENT = Json.read(AbstractSchematicDBTest.class.getClassLoader().getResourceAsStream("document.json"));
        } catch (ParsingException e) {
            throw new RuntimeException(e);
        }
    }
    
    protected abstract SchematicDb getDb() throws Exception ;

    @Before
    public void before() throws Exception {
        db = getDb();
        db.start();
    }

    @After
    public void after() throws Exception {
        db.stop();
    }

    @Test
    public void shouldGetAndPut() throws Exception {
        List<SchematicEntry> dbEntries = randomEntries(3);
        //simulate the start of a transaction
        db.txStarted("0");

        //write some entries without committing
        dbEntries.forEach(dbEntry -> db.put(dbEntry.id(), dbEntry.content()));
        Set<String> expectedIds = dbEntries.stream().map(SchematicEntry::id).collect(Collectors.toCollection(TreeSet::new));
        // check that the same connection is used and the entries are still there
        assertTrue(db.keys().containsAll(expectedIds));
        // simulate a commit for the write
        db.txCommitted("0");
        // check that the entries are still there
        assertTrue(db.keys().containsAll(expectedIds));
        // check that for each entry the content is correctly stored
        dbEntries.stream().forEach(entry -> assertEquals(entry.content(), db.getEntry(entry.id()).content()));

        // update one of the documents and check the update is correct
        SchematicEntry firstEntry = dbEntries.get(0);
        String idToUpdate = firstEntry.id();
        EditableDocument updatedDocument = firstEntry.content().edit(true);
        updatedDocument.setNumber(VALUE_FIELD, 2);

        //simulate a new transaction
        db.txStarted("1");
        db.get(idToUpdate);
        db.put(idToUpdate, updatedDocument);
        assertEquals(updatedDocument, db.getEntry(idToUpdate).content());
        db.txCommitted("1");
        assertEquals(updatedDocument, db.getEntry(idToUpdate).content());
    }

    @Test
    public void shouldReadSchematicEntry() throws Exception {
        SchematicEntry entry = writeSingleEntry();
        SchematicEntry schematicEntry = db.getEntry(entry.id());
        assertNotNull(schematicEntry);
        assertTrue(db.containsKey(entry.id()));
    }

    @Test
    public void shouldEditContentDirectly() throws Exception {
        // test the editing of content for an existing entry
        SchematicEntry entry = writeSingleEntry();
        EditableDocument editableDocument = simulateTransaction(() -> db.editContent(entry.id(), false));
        assertNotNull(editableDocument);
        assertEquals(entry.content(), editableDocument);
        simulateTransaction(() -> {
            EditableDocument document = db.editContent(entry.id(), false);
            document.setNumber(VALUE_FIELD, 2);
            return null;
        });
        Document doc = db.getEntry(entry.id()).content();
        assertEquals(2, (int) doc.getInteger(VALUE_FIELD));

        // test the editing of content for a new entry which should be create
        String newId = UUID.randomUUID().toString();
        EditableDocument newDocument = simulateTransaction(() -> db.editContent(newId, true));
        assertNotNull(newDocument);
        // the content in the DB should be an empty schematic entry...
        SchematicEntry schematicEntry = db.getEntry(newId);
        assertEquals(newId, schematicEntry.id());
        assertEquals(new BasicDocument(), schematicEntry.content());

        // test the editing of a non-existing id without creating a new entry for it
        newDocument = simulateTransaction(() -> db.editContent(UUID.randomUUID().toString(), false));
        assertNull(newDocument);
    }

    @Test
    public void shouldPutIfAbsent() throws Exception {
        SchematicEntry entry = writeSingleEntry();
        EditableDocument editableDocument = entry.content().edit(true);
        editableDocument.setNumber(VALUE_FIELD, 100);
        SchematicEntry updatedEntry = simulateTransaction(() -> db.putIfAbsent(entry.id(), entry.content()));
        assertNotNull(updatedEntry);
        assertEquals(1, (int) updatedEntry.content().getInteger(VALUE_FIELD));

        SchematicEntry newEntry = SchematicEntry.create(UUID.randomUUID().toString(), DEFAULT_CONTENT);
        assertNull(simulateTransaction(() -> db.putIfAbsent(newEntry.id(), newEntry.content())));
        updatedEntry = db.getEntry(newEntry.id());
        assertNotNull(updatedEntry);
    }

    @Test
    public void shouldPutSchematicEntry() throws Exception {
        SchematicEntry originalEntry = randomEntries(1).get(0);
        simulateTransaction(() -> {
            db.putEntry(originalEntry.source());
            return null;
        });

        SchematicEntry actualEntry = db.getEntry(originalEntry.id());
        assertNotNull(actualEntry);
        assertEquals(originalEntry.getMetadata(), actualEntry.getMetadata());
        assertEquals(originalEntry.content(), actualEntry.content());
        assertEquals(DEFAULT_CONTENT, actualEntry.content());
    }

    @Test
    public void shouldRemoveDocument() throws Exception {
        SchematicEntry entry = writeSingleEntry();
        simulateTransaction(() -> db.remove(entry.id()));
        assertFalse(db.containsKey(entry.id()));
    }

    @Test
    public void shouldRemoveAllDocuments() throws Exception {
        int count = 3;
        simulateTransaction(() -> {
            randomEntries(count).forEach(entry -> db.put(entry.id(), entry.content()));
            return null;
        });
        assertFalse(db.keys().isEmpty());
        simulateTransaction(() -> {
            db.removeAll();
            return null;
        });
        assertTrue(db.keys().isEmpty());
    }

    @Test
    public void shouldIsolateChangesWithinTransaction() throws Exception {
        SchematicEntry entry1 = SchematicEntry.create(UUID.randomUUID().toString(), DEFAULT_CONTENT);
        SchematicEntry entry2 = SchematicEntry.create(UUID.randomUUID().toString(), DEFAULT_CONTENT);
        CyclicBarrier syncBarrier = new CyclicBarrier(2);
        CompletableFuture<Void> thread1 = CompletableFuture.runAsync(() -> changeAndCommit(entry1, entry2, syncBarrier));
        CompletableFuture<Void> thread2 = CompletableFuture.runAsync(() -> changeAndCommit(entry2, entry1, syncBarrier));
        thread1.get(3, TimeUnit.SECONDS);
        thread2.get(3, TimeUnit.SECONDS);

        // both transactions should've removed the entries in the end
        Assert.assertFalse(db.containsKey(entry1.id()));
        Assert.assertFalse(db.containsKey(entry2.id()));
    }

    @Test
    public void shouldRollbackChangesWithinTransaction() throws Exception {
        SchematicEntry entry1 = SchematicEntry.create(UUID.randomUUID().toString(), DEFAULT_CONTENT);
        SchematicEntry entry2 = SchematicEntry.create(UUID.randomUUID().toString(), DEFAULT_CONTENT);
        CyclicBarrier syncBarrier = new CyclicBarrier(2);

        CompletableFuture<Void> thread1 = CompletableFuture.runAsync(() -> changeAndRollback(entry1, entry2, syncBarrier));
        CompletableFuture<Void> thread2 = CompletableFuture.runAsync(() -> changeAndRollback(entry2, entry1, syncBarrier));

        thread1.get(2, TimeUnit.SECONDS);
        thread2.get(2, TimeUnit.SECONDS);

        // both transactions should've rolledback their original changes
        Assert.assertEquals(entry1.content(), db.getEntry(entry1.id()).content());
        Assert.assertEquals(entry2.content(), db.getEntry(entry2.id()).content());
    }

    @Test
    public void shouldInsertAndUpdateEntriesConcurrentlyWithMultipleWriters() throws Exception {
        int threadsCount = 100;
        int entriesPerThread = 100;
        ExecutorService executors = Executors.newFixedThreadPool(threadsCount);
        print = false;
        print("Starting the run of " + threadsCount + " threads with " + entriesPerThread + " insertions per thread...");
        long startTime = System.nanoTime();
        List<Future<List<String>>> results = IntStream.range(0, threadsCount)
                                                      .mapToObj(value -> insertMultipleEntries(entriesPerThread, executors))
                                                      .collect(Collectors.toList());

        results.stream()
               .map(future -> {
                   try {
                       return future.get(2, TimeUnit.MINUTES);
                   } catch (Exception e) {
                       throw new RuntimeException(e);
                   }
               })
               .flatMap(List::stream)
               .forEach(id -> Assert.assertTrue(db.containsKey(id)));
        long durationMillis = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        if (print) {
            System.out.printf("Total duration to insert " + threadsCount * entriesPerThread + " entries : " + durationMillis / 1000d + " seconds");
        }
    }

    protected CompletableFuture<List<String>> insertMultipleEntries(int entriesPerThread, ExecutorService executors) {
        return CompletableFuture.supplyAsync(() -> {
            if (print) {
                System.out.println(Thread.currentThread().getName() + " inserting " + entriesPerThread + " entries...");
            }
            String txId = UUID.randomUUID().toString();
            db.txStarted(txId);
            List<String> ids = null;
            try {
                ids = randomEntries(entriesPerThread)
                        .stream()
                        .map(dbEntry -> {
                            db.put(dbEntry.id(), dbEntry.content());
                            return dbEntry.id();
                        })
                        .collect(Collectors.toList());
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            db.txCommitted(txId);
            return ids;
        }, executors);
    }

    private void changeAndRollback(SchematicEntry ourEntry, SchematicEntry otherEntry, CyclicBarrier syncBarrier) {
        try {
            String txId = UUID.randomUUID().toString();

            // start a tx and write the first entry
            db.txStarted(txId);
            db.put(ourEntry.id(), ourEntry.content());
            db.txCommitted(txId);
            syncBarrier.await();

            Document ourDocument = db.getEntry(ourEntry.id()).content();
            Document otherDocument = db.getEntry(otherEntry.id()).content();
            Assert.assertEquals(ourDocument, otherDocument);

            // start a new tx, make some changes and rollback
            txId = UUID.randomUUID().toString();
            db.txStarted(txId);
            db.put(ourEntry.id(), new BasicDocument());
            // rollback the tx
            db.txRolledback(txId);
            syncBarrier.await();

            // and check that the visible documents are unchanged
            Assert.assertEquals(ourDocument, db.getEntry(ourEntry.id()).content());
            Assert.assertEquals(otherDocument, db.getEntry(otherEntry.id()).content());
        } catch (RuntimeException re) {
            syncBarrier.reset();
            throw re;
        } catch (Throwable t) {
            t.printStackTrace();
            syncBarrier.reset();
            throw new RuntimeException(t);
        }
    }

    protected void changeAndCommit(SchematicEntry ourEntry, SchematicEntry otherEntry, CyclicBarrier syncBarrier) {
        try {
            String txId = UUID.randomUUID().toString();

            // start a tx and write the first entry
            db.txStarted(txId);
            db.put(ourEntry.id(), ourEntry.content());

            // now both transactions should've written something without committing so test changes are not visible
            Assert.assertTrue(db.containsKey(ourEntry.id()));
            Assert.assertFalse(db.containsKey(otherEntry.id()));

            // make some changes to ourEntry
            BasicDocument updatedDoc = new BasicDocument();
            db.put(ourEntry.id(), updatedDoc);

            // check that the changes are only visible to ourselves... 
            Document actualDocument = db.getEntry(ourEntry.id()).content();
            Assert.assertTrue(db.containsKey(ourEntry.id()));
            Assert.assertFalse(db.containsKey(otherEntry.id()));
            Assert.assertEquals(updatedDoc, actualDocument);
            syncBarrier.await();
            // and wait for the other tx to make its own changes....
            // now commit
            db.txCommitted(txId);
            syncBarrier.await();

            // check that outside changes are visible...
            Assert.assertTrue(db.containsKey(otherEntry.id()));
            Document otherDocument = db.getEntry(otherEntry.id()).content();
            Assert.assertEquals(updatedDoc, otherDocument);

            // start a new tx
            txId = UUID.randomUUID().toString();

            db.txStarted(txId);
            // remove entry entry
            db.remove(ourEntry.id());
            // and wait for the other tx to remove
            syncBarrier.await();

            // check that changes are not yet visible...            
            Assert.assertFalse(db.containsKey(ourEntry.id()));
            Assert.assertTrue(db.containsKey(otherEntry.id()));

            // and wait for the other tx to remove
            syncBarrier.await();

            // commit the new tx
            db.txCommitted(txId);
            // and wait for the other tx to remove
            syncBarrier.await();

            // check that changes are not now visible...            
            Assert.assertFalse(db.containsKey(ourEntry.id()));
            Assert.assertFalse(db.containsKey(otherEntry.id()));
        } catch (RuntimeException re) {
            syncBarrier.reset();
            throw re;
        } catch (Throwable t) {
            t.printStackTrace();
            syncBarrier.reset();
            throw new RuntimeException(t);
        }
    }

    protected  <T> T simulateTransaction(Callable<T> operation) throws Exception {
        db.txStarted("0");
        T result = operation.call();
        db.txCommitted("0");
        return result;
    }

    protected SchematicEntry writeSingleEntry() throws Exception {
        return simulateTransaction(() -> {
            SchematicEntry entry = SchematicEntry.create(UUID.randomUUID().toString(), DEFAULT_CONTENT);
            db.putEntry(entry.source());
            return entry;
        });
    }


    protected List<SchematicEntry> randomEntries(int sampleSize) throws Exception {
        return IntStream.range(0, sampleSize).mapToObj(i -> SchematicEntry.create(
                UUID.randomUUID().toString(), DEFAULT_CONTENT)).collect(Collectors.toList());
    }

    protected void print(String s) {
        if (print) {
            System.out.println(Thread.currentThread().getName() + ": " + s);
        }
    }
}
