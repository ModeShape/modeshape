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

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.schematic.AbstractSchematicDBTest;
import org.modeshape.schematic.Schematic;
import org.modeshape.schematic.SchematicDb;
import org.modeshape.schematic.SchematicEntry;
import org.modeshape.schematic.internal.annotation.FixFor;

/**
 * Integration test for {@link RelationalDb}. The configuration used for this test is using system properties set by Maven
 * together with Docker.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class RelationalDbIT extends AbstractSchematicDBTest {

    @Override
    protected SchematicDb getDb() throws Exception {
        return Schematic.getDb(RelationalDbIT.class.getClassLoader().getResourceAsStream("db-config.json"));
    }
    
    @Before
    public void before() throws Exception {
        super.before();
        // run a query to validate that the table has been created and is empty.
        assertEquals(0, db.keys().size());
    }
    
    @After
    public void after() throws Exception {
        super.after();
        // run a query to check that the table has been removed
        try {
            db.keys();
            fail("The DB table should have been dropped...");
        } catch (RelationalProviderException e) {
            //expected
        }
    }
    
    @Test
    public void shouldLockEntriesExclusively() throws Exception {
        insertAndLock(100);
    }
    
    protected void insertAndLock(int entriesCount) throws Exception {
        List<String> ids = insertMultipleEntries(entriesCount, Executors.newSingleThreadExecutor()).get();
        // run and commit tx 1
        boolean result = simulateTransaction(() -> db.lockForWriting(ids));
        assertTrue("Locks should have been obtained", result);
        
        // run and commit tx 2
        result = simulateTransaction(() -> db.lockForWriting(ids));
        assertTrue("Locks should have been obtained", result);
    }
    
    @Test
    public void shouldReportNonExistentEntryAsLocked() throws Exception {
        simulateTransaction(() -> {
            Assert.assertTrue(db.lockForWriting("non_existant"));
            return null;
        });
    }
    
    @Test(expected = RelationalProviderException.class)
    public void shouldNotLockEntriesWithoutTransaction() throws Exception {
        String id = writeSingleEntry().id();
        db.lockForWriting(id);
    }
    
    @Test
    @Ignore("ignored by default because on most DBs running in Docker it takes a long time for the lock timeout message")
    public void concurrentThreadsShouldNotGetSameLock() throws Exception {
        String id = writeSingleEntry().id();
        CyclicBarrier barrier = new CyclicBarrier(2);
        ExecutorService executorService = Executors.newFixedThreadPool(2);
        Future<Boolean> t1 = executorService.submit(() -> {
            db.txStarted("1");
            boolean result = db.lockForWriting(id);
            barrier.await();
            db.txCommitted("1");
            return result;
        });
        
        Future<Boolean> t2 = executorService.submit(() -> {
            db.txStarted("2");
            boolean result = db.lockForWriting(id);
            barrier.await();
            db.txCommitted("2");
            return result;
        });
        
        boolean t1Success = t1.get();
        boolean t2Success = t2.get();
        assertTrue("Only one of the threads should have been able to lock" , (t1Success  && !t2Success) || (!t1Success && t2Success));
    }
    
    @Test
    @FixFor( "MODE-2629" )
    public void shouldReadWithDifferentBatches() throws Exception {         
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        int maxStatementParamCount = DefaultStatements.DEFAULT_MAX_STATEMENT_PARAM_COUNT;
        try {
            List<String> ids = insertMultipleEntries(2000, executorService).get(10, TimeUnit.SECONDS);
            loadAndAssertIds(ids, 0);
            loadAndAssertIds(ids, 1);
            loadAndAssertIds(ids, maxStatementParamCount / 2);
            loadAndAssertIds(ids, (maxStatementParamCount / 2) + 1);
            loadAndAssertIds(ids, (maxStatementParamCount /2) + (maxStatementParamCount / 4));
            loadAndAssertIds(ids, maxStatementParamCount);
        } finally {
            executorService.shutdownNow();
        }
    }
    
    private void loadAndAssertIds(List<String> insertedIds, int batchSize) {
        List<String> expectedIds = insertedIds.subList(0, batchSize);
        List<SchematicEntry> entries = db.load(expectedIds);
        List<String> actualIds = entries.stream().map(SchematicEntry::id).collect(Collectors.toList());
        Collections.sort(expectedIds);
        Collections.sort(actualIds);
        assertEquals("The same entries should have been read back", expectedIds, actualIds);
    }
    
    @Test
    @FixFor( "MODE-2629" )
    public void shouldLockWithDifferentBatches() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        int maxStatementParamCount = DefaultStatements.DEFAULT_MAX_STATEMENT_PARAM_COUNT;
        try {
            List<String> ids = insertMultipleEntries(2000, executorService).get(10, TimeUnit.SECONDS);
            lockIds(ids, 1);
            lockIds(ids, maxStatementParamCount / 2);
            lockIds(ids, (maxStatementParamCount / 2) + 1);
            lockIds(ids, (maxStatementParamCount /2) + (maxStatementParamCount / 4));
            lockIds(ids, maxStatementParamCount);
        } finally {
            executorService.shutdownNow();
        }    
    }
    
    private void lockIds(List<String> insertedIds, int batchSize) throws Exception {
        List<String> subList = insertedIds.subList(0, batchSize);
        simulateTransaction(() -> {
            assertTrue("Entries could not be locked", db.lockForWriting(subList));
            return null;
        });
    }
    
    @Test
    @FixFor( "MODE-2629" )
    public void shouldRemoveInBatches() throws Exception {
        ExecutorService executorService = Executors.newSingleThreadExecutor();
        int maxStatementParamCount = DefaultStatements.DEFAULT_MAX_STATEMENT_PARAM_COUNT;
        try {
            int count = 2000;
            List<String> ids = insertMultipleEntries(count, executorService).get(10, TimeUnit.SECONDS);
            removeBatch(ids, 0, 1);
            removeBatch(ids, 1, maxStatementParamCount / 2 + 1);
            int start =  maxStatementParamCount / 2 + 1;
            while (start < count) {
                int end = start + maxStatementParamCount > count ? count : start + maxStatementParamCount;
                removeBatch(ids, start, end);
                start = end;
            }
            assertTrue("Not all keys were deleted", db.keys().isEmpty());
        } finally {
            executorService.shutdownNow();
        }
    }
    
    private void removeBatch(List<String> ids, int start, int end) throws Exception {
        simulateTransaction(() -> {
            List<String> toRemove = ids.subList(start, end);
            toRemove.forEach(id -> db.remove(id));
            return null;
        });
    }
}
