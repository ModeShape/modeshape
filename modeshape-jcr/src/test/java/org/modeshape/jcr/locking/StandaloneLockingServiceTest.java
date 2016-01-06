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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.junit.Test;

/**
 * Unit test for {@link StandaloneLockingService}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class StandaloneLockingServiceTest {

    @Test
    public void shouldLockSingleLock() {
        LockingService service = newLockingService();
        String lockName = "test";
        assertTrue(service.tryLock(lockName));
        assertTrue(service.unlock(lockName).isEmpty());
    }

    @Test
    public void shouldLockMultipleLocks() {
        LockingService service = newLockingService();
        String[] lockNames = new String[] {"lock1", "lock2"};
        assertTrue(service.tryLock(lockNames));
        assertTrue(service.unlock(lockNames).isEmpty());
    }
    
    @Test
    public void shouldLockDifferentNamesFromDifferentThreads() throws Exception {
        LockingService service = newLockingService();
        CompletableFuture.runAsync(() -> assertTrue(service.tryLock("lock1"))).get();
        CompletableFuture.runAsync(() -> assertTrue(service.tryLock("lock2"))).get();
        assertEquals(service.unlock("lock1", "lock2").size(), 2);
    }

    @Test
    public void shouldFailIfLocksAlreadyHeld() {
        List<String> lockNames = Arrays.asList("lock1", "lock2");
        LockingService service = newLockingService();
        CompletableFuture<Void> op1 = CompletableFuture.runAsync(() -> assertTrue(service.tryLock(lockNames.toArray(
                new String[lockNames.size()]))));
        Collections.reverse(lockNames);
        op1.thenRunAsync(() -> assertFalse(service.tryLock(lockNames.toArray(new String[lockNames.size()]))))
           .thenRunAsync(() -> assertEquals(service.unlock(lockNames.toArray(new String[lockNames.size()])).size(),
                                            lockNames.size())); 
    }
    
    @Test
    public void shouldReleaseAllLocksWhenFailingToAcquireOne() throws Exception {
        LockingService service = newLockingService();
        CompletableFuture.runAsync(() -> assertTrue(service.tryLock("lock1", "lock2", "lock3"))).get();
        assertFalse(service.tryLock("lock4", "lock5", "lock2"));
        assertTrue(service.tryLock("lock4", "lock5"));
        assertTrue(service.unlock("lock4", "lock5").isEmpty());
    }
    
    @Test
    public void unlockShouldReleaseReentrantLocks() throws Exception {
        LockingService service = newLockingService();
        String[] locks = { "lock1", "lock2", "lock3" };
        CompletableFuture.runAsync(() -> {
            assertTrue(service.tryLock(locks));
            assertTrue(service.tryLock(locks));
            assertTrue(service.tryLock(locks));
            assertTrue(service.unlock(locks).isEmpty());
        }).get();
        assertTrue(service.tryLock(locks));
    }
    
    protected LockingService newLockingService() {
        LockingService lockingService = new StandaloneLockingService();
        lockingService.setLockTimeout(0);
        return lockingService;
    }
}
