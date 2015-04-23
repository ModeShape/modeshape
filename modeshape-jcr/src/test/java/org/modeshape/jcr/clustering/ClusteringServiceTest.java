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

package org.modeshape.jcr.clustering;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.Stack;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.ClusteringHelper;

/**
 * Unit test for {@link ClusteringService}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ClusteringServiceTest {

    private Stack<ClusteringService> testClusteringServices = new Stack<>();

    @After
    public void after() throws Exception {
        while (!testClusteringServices.isEmpty()) {
            testClusteringServices.pop().shutdown();
        }
        testClusteringServices.clear();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        ClusteringHelper.bindJGroupsToLocalAddress();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        ClusteringHelper.removeJGroupsBindings();
    }

    @Test
    public void shouldBroadcastMessagesBetweenServices() throws Exception {
        ClusteringService service1 = startStandalone("test-cluster1");
        TestConsumer consumer1 = new TestConsumer("hello_1", "hello_2");
        service1.addConsumer(consumer1);

        ClusteringService service2 = startStandalone("test-cluster1");
        TestConsumer consumer2 = new TestConsumer("hello_1", "hello_2");
        service2.addConsumer(consumer2);

        service1.sendMessage("hello_1");
        service2.sendMessage("hello_2");

        consumer1.assertAllPayloadsConsumed();
        consumer2.assertAllPayloadsConsumed();
    }

    @Test
    public void shouldAllowGlobalLocking() throws Exception {
        BitSet bits = new BitSet();

        CountDownLatch consumer1Latch = new CountDownLatch(1);
        ClusteringService service1 = startStandalone("test-cluster1");
        String consumer1Id = UUID.randomUUID().toString();
        LockConsumer consumer1 = new LockConsumer(consumer1Id, consumer1Latch, 0, bits, service1);
        service1.addConsumer(consumer1);

        CountDownLatch consumer2Latch = new CountDownLatch(1);
        ClusteringService service2 = startStandalone("test-cluster1");
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
    @FixFor( "MODE-2226" )
    public void shouldAllowMultipleForksOffTheSameChannel() throws Exception {
        ClusteringService main11 = startStandalone("test-cluster1");
        ClusteringService main12 = startStandalone("test-cluster2");

        //fork11 communicates via the same fork stack to fork21
        ClusteringService fork11 = startForked(main11);
        TestConsumer consumer11 = new TestConsumer("11", "21");
        fork11.addConsumer(consumer11);

        //fork12 communicates via the same fork stack to fork22
        ClusteringService fork12 = startForked(main12);
        TestConsumer consumer12 = new TestConsumer("12", "22");
        fork12.addConsumer(consumer12);

        ClusteringService main21 = startStandalone("test-cluster1");
        ClusteringService main22 = startStandalone("test-cluster2");

        //fork21 communicates via the same fork stack to fork11
        ClusteringService fork21 = startForked(main21);
        TestConsumer consumer21 = new TestConsumer("11", "21");
        fork21.addConsumer(consumer21);

        //fork22 communicates via the same fork stack to fork12
        ClusteringService fork22 = startForked(main22);
        TestConsumer consumer22 = new TestConsumer("12", "22");
        fork22.addConsumer(consumer22);

        fork11.sendMessage("11");
        fork21.sendMessage("21");
        fork12.sendMessage("12");
        fork22.sendMessage("22");

        consumer11.assertAllPayloadsConsumed();
        consumer12.assertAllPayloadsConsumed();
        consumer21.assertAllPayloadsConsumed();
        consumer22.assertAllPayloadsConsumed();
    }

    private ClusteringService startStandalone( String clusterName ) {
        ClusteringService service = ClusteringService.startStandalone(clusterName, "config/jgroups-test-config.xml");
        testClusteringServices.push(service);
        return service;
    }

    private ClusteringService startForked( ClusteringService mainService ) {
        ClusteringService service = ClusteringService.startForked(mainService.getChannel());
        testClusteringServices.push(service);
        return service;
    }

    protected class TestConsumer extends MessageConsumer<String> {
        private List<String> payloads = new ArrayList<>();
        private CountDownLatch payloadsLatch;

        protected TestConsumer( String... expectedPayloads ) {
            super(String.class);
            payloads = Arrays.asList(expectedPayloads);
            payloadsLatch = new CountDownLatch(expectedPayloads.length);
        }

        @Override
        public void consume( String payload ) {
            assertTrue(payload + " not expected", payloads.contains(payload));
            payloadsLatch.countDown();
        }

        protected void assertAllPayloadsConsumed() throws InterruptedException {
            assertTrue("Not all payloads received", payloadsLatch.await(1, TimeUnit.SECONDS));
        }
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
            if (clusteringService.tryLock(100, TimeUnit.MILLISECONDS)) {
                bits.set(positionToFlip, true);
            }
            latch.countDown();
        }
    }
}
