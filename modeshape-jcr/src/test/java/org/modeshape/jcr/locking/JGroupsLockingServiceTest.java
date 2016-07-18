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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.ClusteringHelper;
import org.modeshape.jcr.clustering.ClusteringService;

/**
 * Test which verifies the locking logic of the {@link org.modeshape.jcr.locking.LockingService} as per the contract
 * defined by {@link LockingService}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class JGroupsLockingServiceTest extends StandaloneLockingServiceTest {
    
    private static final int SERVICES_COUNT = 4;      
    
    private static List<ClusteringService> clusteringServices;
    
    private List<LockingService> lockingServices = new ArrayList<>();

    @BeforeClass
    public static void beforeClass() throws Exception {
        ClusteringHelper.bindJGroupsToLocalAddress();
        clusteringServices = IntStream.range(0, SERVICES_COUNT)
                                      .mapToObj(i -> ClusteringService.startStandalone("locking-cluster",
                                                                                       "config/cluster/jgroups-test-config.xml"))
                                      .collect(Collectors.toList());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        ClusteringHelper.removeJGroupsBindings();
        clusteringServices.forEach(ClusteringService::shutdown);
        clusteringServices.clear();
    }
    
    @After
    public void after() throws Exception {
        super.after();
        lockingServices.forEach(LockingService::shutdown);
        lockingServices.clear();
    }

    @Test
    public void shouldAcquireDisjunctLocksInCluster() throws Exception {
        LockingService service1 = newLockingService(0);
        LockingService service2 = newLockingService(1);
        assertTrue(service1.tryLock("lock1", "lock2"));
        assertTrue(service2.tryLock("lock3", "lock4"));         
    } 
    
    @Test
    public void shouldNotAcquireSameLockFromMultipleThreadOnTheSameClusterNode() throws Exception {
        LockingService service1 = newLockingService(0);
        assertTrue(service1.tryLock("lock1"));
        CompletableFuture.runAsync(() -> assertLock(service1, false, "lock1")).get();
        assertTrue(service1.unlock("lock1"));
        CompletableFuture.runAsync(() -> assertLock(service1, true, "lock1")).get();
    }
    
    @Test
    public void shouldPropagateLockInformationWhenChangingMembersInCluster() throws Exception {
        LockingService service1 = newLockingService(0);
        // lock while we're the only member
        assertTrue(service1.tryLock("lock1"));
        LockingService service2 = newLockingService(1);
        // check that a new member can't get the lock yet
        assertFalse(service2.tryLock("lock1"));
        // and neither another thread...
        CompletableFuture.runAsync(() -> assertLock(service1, false, "lock1")).get();
        
        // now unlock
        assertTrue(service1.unlock("lock1"));
        Thread.sleep(100);
        // check that the new member can get the lock
        assertTrue(service2.tryLock("lock1"));
        // shutdown the second member (which should release the lock)
        service2.shutdown();
        Thread.sleep(100);
        // and now check that another thread can get the lock
        CompletableFuture.runAsync(() -> assertLock(service1, true, "lock1")).get();
    }

    @Test
    public void shouldNotAcquireSameLockInClusterSimultaneously1() throws Exception {
        LockingService service1 = newLockingService(0);
        LockingService service2 = newLockingService(1);
        assertTrue(service1.tryLock("lock1", "lock2"));
        assertFalse(service2.tryLock("lock2", "lock3"));
        assertTrue(service1.unlock("lock2"));
        Thread.sleep(100);
        assertTrue(service2.tryLock("lock2", "lock3"));
        assertFalse(service1.tryLock("lock3"));
        assertTrue(service2.unlock("lock2", "lock3"));
    }

    @Test
    public void shouldNotAcquireSameLockInClusterSimultaneously2() throws Exception {
        LockingService service1 = newLockingService(0);
        LockingService service2 = newLockingService(1);
        LockingService service3 = newLockingService(2);
        LockingService service4 = newLockingService(3);
        assertTrue(service1.tryLock("lock1", "lock2"));
        assertFalse(service2.tryLock("lock2", "lock3"));
        assertFalse(service3.tryLock("lock4", "lock1"));
        assertTrue(service4.tryLock("lock5", "lock6"));
        assertFalse(service1.tryLock("lock1", "lock5"));
        assertFalse(service1.tryLock("lock6", "lock1"));

        assertTrue(service1.unlock("lock1", "lock2"));
        assertTrue(service4.unlock("lock5", "lock6"));
        Thread.sleep(100);

        assertTrue(service2.tryLock("lock2", "lock3"));
        assertTrue(service3.tryLock("lock4", "lock1"));
        
        assertFalse(service1.tryLock("lock1", "lock5"));
        assertFalse(service1.tryLock("lock6", "lock1"));
        
        // now shut down service 2 and make sure the rest of the locks are unchanged..
        service2.shutdown();
        Thread.sleep(100);
        
        assertFalse(service1.tryLock("lock1", "lock5"));
        assertFalse(service4.tryLock("lock4"));
    }

    protected JGroupsLockingService newLockingService(int clusteredServiceIdx) {
        ClusteringService service = clusteringServices.get(clusteredServiceIdx);  
        JGroupsLockingService lockingService = new JGroupsLockingService(service.getChannel(), 100);
        lockingServices.add(lockingService);        
        return lockingService;
    }

    @Override
    protected LockingService newLockingService() {
        return new JGroupsLockingService(clusteringServices.get(0).getChannel(), 100);
    }
}
