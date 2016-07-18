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
import java.util.List;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.schematic.AbstractSchematicDBTest;
import org.modeshape.schematic.Schematic;
import org.modeshape.schematic.SchematicDb;

/**
 * Unit test for {@link RelationalDb}. The configuration used for this test is filtered by Maven based on the active
 * DB profile.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class RelationalDbTest extends AbstractSchematicDBTest {

    @Override
    protected SchematicDb getDb() throws Exception {
        return Schematic.getDb(RelationalDbTest.class.getClassLoader().getResourceAsStream("db-config.json"));
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

    @Test
    public void shouldLockEntriesExclusivelyUsingBatches() throws Exception {
        insertAndLock(600);
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
}
