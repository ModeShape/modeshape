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
package org.modeshape.persistence.relational;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Test;
import org.modeshape.schematic.SchematicEntry;
import org.modeshape.schematic.document.Document;
import org.modeshape.schematic.internal.document.BasicDocument;

/**
 * Unit tests for the cases when multiple transactions concurrently write/read from a {@link org.modeshape.schematic.SchematicDb}
 * 
 * Note that this test *does not* test write contention on the same key, which ModeShape should make sure it never happens
 * due to exclusive locking.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class RelationalDbTransactionsTest extends AbstractRelationalDbTest {
    
    @Test
    public void shouldIsolateChangesWithinTransaction() throws Exception {
        SchematicEntry entry1 = SchematicEntry.create(UUID.randomUUID().toString(), defaultContent);
        SchematicEntry entry2 = SchematicEntry.create(UUID.randomUUID().toString(), defaultContent);
        CyclicBarrier syncBarrier = new CyclicBarrier(2);
        CompletableFuture<Void> thread1 = CompletableFuture.runAsync(() -> changeAndCommit(entry1,
                                                                                           entry2,
                                                                                           syncBarrier));
        CompletableFuture<Void> thread2 = CompletableFuture.runAsync(() -> changeAndCommit(entry2,
                                                                                           entry1,
                                                                                           syncBarrier));
        thread1.get(2, TimeUnit.SECONDS);
        thread2.get(2, TimeUnit.SECONDS);
       
        // both transactions should've removed the entries in the end
        assertFalse(db.containsKey(entry1.id()));
        assertFalse(db.containsKey(entry2.id()));
    }

    @Test
    public void shouldRollbackChangesWithinTransaction() throws Exception {
        SchematicEntry entry1 = SchematicEntry.create(UUID.randomUUID().toString(), defaultContent);
        SchematicEntry entry2 = SchematicEntry.create(UUID.randomUUID().toString(), defaultContent);
        CyclicBarrier syncBarrier = new CyclicBarrier(2);

        CompletableFuture<Void> thread1 = CompletableFuture.runAsync(() -> changeAndRollback(entry1, entry2, syncBarrier));
        CompletableFuture<Void> thread2 = CompletableFuture.runAsync(() -> changeAndRollback(entry2, entry1, syncBarrier));

        thread1.get(2, TimeUnit.SECONDS);
        thread2.get(2, TimeUnit.SECONDS);
        
        // both transactions should've rolledback their original changes
        assertEquals(entry1.content(), db.getEntry(entry1.id()).content());
        assertEquals(entry2.content(), db.getEntry(entry2.id()).content());
    }
    
    @Test
    public void shouldInsertAndUpdateEntriesConcurrentlyWithMultipleWriters() throws Exception {
        int threadsCount = 100;
        int entriesPerThread = 100;
        ExecutorService executors = Executors.newFixedThreadPool(threadsCount);
        print = false;
        if (print) {
            System.out.printf("Starting the run of " + threadsCount + " threads with " + entriesPerThread + " insertions per thread...");
        }
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
               .forEach(id -> assertTrue(db.containsKey(id)));
        long durationMillis = TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
        if (print) {
            System.out.printf("Total duration to insert " + threadsCount * entriesPerThread + " entries : " + durationMillis / 1000d + " seconds");
        }
    }

    private CompletableFuture<List<String>> insertMultipleEntries(int entriesPerThread, ExecutorService executors) {
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
            syncBarrier.reset();
            
            Document ourDocument = db.getEntry(ourEntry.id()).content();
            Document otherDocument = db.getEntry(otherEntry.id()).content();
            assertEquals(ourDocument, otherDocument);

            // start a new tx, make some changes and rollback
            txId = UUID.randomUUID().toString();
            db.txStarted(txId);
            db.put(ourEntry.id(), new BasicDocument());
            // rollback the tx
            db.txRolledback(txId);
            syncBarrier.await();
            syncBarrier.reset();
            
            // and check that the visible documents are unchanged
            assertEquals(ourDocument, db.getEntry(ourEntry.id()).content());
            assertEquals(otherDocument, db.getEntry(otherEntry.id()).content());
        } catch (Exception e) {
            throw new RuntimeException(e);
        } 
    }   

    private void changeAndCommit(SchematicEntry ourEntry, SchematicEntry otherEntry, CyclicBarrier syncBarrier) {
        try {
            String txId = UUID.randomUUID().toString();
            
            // start a tx and write the first entry
            db.txStarted(txId);
            db.put(ourEntry.id(), ourEntry.content());
            syncBarrier.await();
            syncBarrier.reset();         
           
            // now both transactions should've written something without committing so test changes are not visible
            assertTrue(db.containsKey(ourEntry.id()));
            assertFalse(db.containsKey(otherEntry.id()));
            
            // make some changes to ourEntry
            BasicDocument updatedDoc = new BasicDocument();
            db.put(ourEntry.id(), updatedDoc);
         
            // check that the changes are only visible to ourselves... 
            Document actualDocument = db.getEntry(ourEntry.id()).content();
            assertTrue(db.containsKey(ourEntry.id()));
            assertFalse(db.containsKey(otherEntry.id()));
            assertEquals(updatedDoc, actualDocument);

            // and wait for the other tx to make its own changes....
            syncBarrier.await();
            syncBarrier.reset();
            // now commit
            db.txCommitted(txId);
            syncBarrier.await();
            syncBarrier.reset();
            
            // check that outside changes are visible...
            assertTrue(db.containsKey(otherEntry.id()));
            Document otherDocument = db.getEntry(otherEntry.id()).content();
            assertEquals(updatedDoc, otherDocument);
           
            // start a new tx
            txId = UUID.randomUUID().toString();
            
            db.txStarted(txId);
            // remove entry entry
            db.remove(ourEntry.id());
            // and wait for the other tx to remove
            syncBarrier.await();
            syncBarrier.reset();
            // check that changes are not yet visible...            
            assertFalse(db.containsKey(ourEntry.id()));
            assertTrue(db.containsKey(otherEntry.id()));
            
            // commit the new tx
            db.txCommitted(txId);
            // and wait for the other tx to remove
            syncBarrier.await();
            syncBarrier.reset();

            // check that changes are not now visible...            
            assertFalse(db.containsKey(ourEntry.id()));
            assertFalse(db.containsKey(otherEntry.id()));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
