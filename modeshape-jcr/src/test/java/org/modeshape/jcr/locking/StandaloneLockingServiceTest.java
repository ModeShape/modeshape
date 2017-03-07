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
package org.modeshape.jcr.locking;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.Before;
import org.junit.Test;

/**
 * Unit test for {@link StandaloneLockingService}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class StandaloneLockingServiceTest {
    
    private LockingService service;
    private ExecutorService executors;
    
    @Before
    public void before() {
        service = newLockingService();
        executors = Executors.newFixedThreadPool(3);
    }
    
    public void after() throws Exception {
        executors.shutdownNow();
        service.shutdown();
    }

    @Test
    public void shouldLockMultipleLocks() throws Exception {
        String[] lockNames = new String[] { "lock1", "lock2" };
        assertTrue(service.tryLock(lockNames));
        assertTrue(service.unlock(lockNames));
    }
   
    @Test
    public void shouldLockDifferentNamesFromDifferentThreads() throws Exception {
        CompletableFuture.runAsync(() -> {
            assertLock(service, true, "lock1");
            assertTrue(service.unlock("lock1"));
        }).thenRunAsync(() -> {
            assertLock(service, true, "lock2");
            assertTrue(service.unlock("lock2"));
        }, executors).get();
    } 
  
    @Test
    public void shuttingDownShouldReleaseLocks() throws Exception {
        assertTrue(service.tryLock("lock1"));
        assertTrue(service.tryLock("lock2"));
        assertTrue(service.shutdown());
        assertFalse(service.shutdown());
    }

    @Test
    public void shouldFailIfLocksAlreadyHeld() throws Exception {
        assertLock(service, true, "lock1", "lock2");
        CompletableFuture.runAsync(() -> assertLock(service, false, "lock2", "lock1"), executors)
                         .thenRunAsync(() -> assertLock(service, false, "lock1", "lock2"), executors)                 
                         .get();
        assertTrue(service.unlock("lock1", "lock2"));
    }

    @Test
    public void shouldReleaseAllLocksWhenFailingToAcquireOne() throws Exception {
        CompletableFuture.runAsync(() -> assertLock(service, true, "lock1", "lock2", "lock3"))
                         .thenRunAsync(() -> assertLock(service, false, "lock4", "lock5", "lock2"), executors)
                         .thenRun(() -> {
                             assertLock(service, true, "lock4", "lock5");
                             assertTrue(service.unlock("lock4", "lock5"));
                         }).get();
    }

    @Test
    public void unlockShouldReleaseReentrantLocks() throws Exception {
        String[] locks = { "lock1", "lock2", "lock3" };
        CompletableFuture.runAsync(() -> {
            assertLock(service, true, locks);
            assertLock(service, true, locks);
            assertLock(service, true, locks);
            assertTrue(service.unlock(locks));
        }).get();
        assertTrue(service.tryLock(locks));
        assertTrue(service.unlock(locks));
    }

    @Test
    public void locksShouldBeExclusiveAcrossMultipleThreads() throws Exception {
        String commonLock = "3293af3317f1e7/";
        int threadCount = 100;
        int uniqueLocksPerThread = 3;
        List<String> results = new ArrayList<>();
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        IntStream.range(0, threadCount)
                 .forEach(i -> executorService.submit(() -> {
                     lockAndUnlock(service, commonLock, uniqueLocksPerThread, results);
                     return null;
                 }));
        executorService.shutdown();
        if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
            fail("Could obtain locks in an orderly amount of time..");
            executorService.shutdownNow();
        } else {
            assertEquals("exclusive access was not ensured", threadCount * uniqueLocksPerThread,
                         results.size());
        }
    }

    private void lockAndUnlock(LockingService service, String commonLock, int uniqueLocksCount, List<String> accumulator) {
        List<String> uniqueLocks =
                IntStream.range(0, uniqueLocksCount).mapToObj(i -> UUID.randomUUID().toString()).collect(Collectors.toList());
        uniqueLocks.add(commonLock);
        String[] locks = uniqueLocks.toArray(new String[uniqueLocks.size()]);
        try {
            if (service.tryLock(15, TimeUnit.SECONDS, locks)) {
                uniqueLocks.remove(commonLock);
                accumulator.addAll(uniqueLocks);
                service.unlock(locks);
            } else {
                fail("locks should've been obtained by now...");
            }
        } catch (InterruptedException e) {
            fail("interrupted...");
        }
    }

    protected LockingService newLockingService() {
        return new StandaloneLockingService();
    }
    
    protected void assertLock(LockingService service, boolean successful, String...names) {
        try {
            boolean result = service.tryLock(names);
            assertEquals("lock operation failed", successful, result);
        } catch (InterruptedException e) {
            Thread.interrupted();
            fail("interrupted...");
        }
    }
}
