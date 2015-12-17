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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.BitSet;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.ClusteringHelper;
import org.modeshape.jcr.clustering.ClusteringService;
import org.modeshape.jcr.clustering.MessageConsumer;

/**
 * Test which verifies the locking logic of the {@link org.modeshape.jcr.clustering.ClusteringService} as per the contract
 * defined by {@link LockingService}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ClusteringServiceLockingTest extends StandaloneLockingServiceTest {
    
    private Stack<ClusteringService> testClusteringServices = new Stack<>();

    @BeforeClass
    public static void beforeClass() throws Exception {
        ClusteringHelper.bindJGroupsToLocalAddress();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        ClusteringHelper.removeJGroupsBindings();
    }
    
    @After
    public void after() throws Exception {
        testClusteringServices.forEach(ClusteringService::shutdown);
        testClusteringServices.clear();
    }
    
    @Test
    public void shouldAllowGlobalLocking() throws Exception {
        BitSet bits = new BitSet();

        CountDownLatch consumer1Latch = new CountDownLatch(1);
        ClusteringService service1 = newLockingService();
        String consumer1Id = UUID.randomUUID().toString();
        LockConsumer consumer1 = new LockConsumer(consumer1Id, consumer1Latch, 0, bits, service1);
        service1.addConsumer(consumer1);

        CountDownLatch consumer2Latch = new CountDownLatch(1);
        ClusteringService service2 = newLockingService();
        String consumer2Id = UUID.randomUUID().toString();
        LockConsumer consumer2 = new LockConsumer(consumer2Id, consumer2Latch, 1, bits, service2);
        service2.addConsumer(consumer2);

        //send a message from service2 to service1, this should make service1 acquire the lock
        service2.sendMessage(consumer2Id);
        //make sure the lock was acquired
        consumer1Latch.await(2, TimeUnit.SECONDS);
        //check that service1 made the change which means it has the lock
        assertTrue(bits.get(0));

        //send a message from service1 to service2, while service 1 should have the lock
        service1.sendMessage(consumer1Id);
        //make sure service2 tried to acquire the lock
        consumer2Latch.await(2, TimeUnit.SECONDS);
        //check that service2 didn't get to make the changes
        assertFalse(bits.get(1));
    }
    
    @Test
    public void shouldAcquireDisjunctLocksInCluster() throws Exception {
        ClusteringService service1 = newLockingService();
        ClusteringService service2 = newLockingService();
        assertTrue(service1.tryLock("lock1", "lock2"));
        assertTrue(service2.tryLock(10, TimeUnit.MILLISECONDS, "lock3", "lock4"));         
    }

    @Test
    public void shouldNotAcquireSameLockInClusterSimultaneously1() throws Exception {
        ClusteringService service1 = newLockingService();
        ClusteringService service2 = newLockingService();
        assertTrue(service1.tryLock("lock1", "lock2"));
        assertFalse(service2.tryLock(10, TimeUnit.MILLISECONDS, "lock2", "lock3"));
        assertTrue(service1.unlock("lock2"));
        Thread.sleep(10);
        assertTrue(service2.tryLock(10, TimeUnit.MILLISECONDS, "lock2", "lock3"));
        assertFalse(service1.unlock("lock2"));
        assertTrue(service2.unlock("lock2", "lock3"));
    }

    @Test
    public void shouldNotAcquireSameLockInClusterSimultaneously2() throws Exception {
        ClusteringService service1 = newLockingService();
        ClusteringService service2 = newLockingService();
        ClusteringService service3 = newLockingService();
        ClusteringService service4 = newLockingService();
        assertTrue(service1.tryLock("lock1", "lock2"));
        assertFalse(service2.tryLock(10, TimeUnit.MILLISECONDS, "lock2", "lock3"));
        assertFalse(service3.tryLock(10, TimeUnit.MILLISECONDS, "lock4", "lock1"));
        assertTrue(service4.tryLock(10, TimeUnit.MILLISECONDS, "lock5", "lock6"));
        assertFalse(service1.tryLock(10, TimeUnit.MILLISECONDS, "lock1", "lock5"));
        assertFalse(service1.tryLock(10, TimeUnit.MILLISECONDS, "lock6", "lock1"));

        assertTrue(service1.unlock("lock1", "lock2"));
        assertTrue(service4.unlock("lock5", "lock6"));
        
        assertTrue(service2.tryLock(10, TimeUnit.MILLISECONDS, "lock2", "lock3"));
        assertTrue(service3.tryLock(10, TimeUnit.MILLISECONDS, "lock4", "lock1"));
        assertFalse(service1.tryLock(10, TimeUnit.MILLISECONDS, "lock1", "lock5"));
        assertFalse(service1.tryLock(10, TimeUnit.MILLISECONDS, "lock6", "lock1"));
    }

    @Override
    protected ClusteringService newLockingService() {
        ClusteringService service = ClusteringService.startStandalone("locking-cluster", "config/cluster/jgroups-test-config.xml");
        testClusteringServices.push(service);
        return service;
    }

    protected class LockConsumer extends MessageConsumer<String> {
        private final String id;
        private final BitSet bits;
        private final CountDownLatch latch;
        private final int positionToFlip;
        private final ClusteringService clusteringService;

        protected LockConsumer( String id, CountDownLatch latch, int positionToFlip, BitSet bits,
                                ClusteringService clusteringService ) {
            super(String.class);
            this.id = id;
            this.bits = bits;
            this.clusteringService = clusteringService;
            this.positionToFlip = positionToFlip;
            this.latch = latch;
        }

        @Override
        public void consume( String payload ) {
            if (payload.equalsIgnoreCase(id)) {
                //ignore messages from self
                return;
            }
            if (clusteringService.tryLock(100, TimeUnit.MILLISECONDS, "testLock")) {
                bits.set(positionToFlip, true);
            }
            latch.countDown();
        }
    }
}
