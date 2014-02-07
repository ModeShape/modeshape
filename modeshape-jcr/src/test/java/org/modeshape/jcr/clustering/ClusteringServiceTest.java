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
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.ClusteringHelper;

/**
 * Unit test for {@link ClusteringService}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ClusteringServiceTest {

    private ClusteringService clusteringService;

    @After
    public void after() throws Exception {
        if (clusteringService != null) {
            this.clusteringService.shutdown();
        }
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
    public void shouldAllowSettingClusterNameToNull() throws Exception {
        initClusterService(null);
    }

    @Test
    public void shouldAllowSettingClusterNameToBlankString() throws Exception {
        initClusterService("");
    }

    @Test
    public void shouldAllowSettingClusterNameToStringWithAlphaNumericCharacters() throws Exception {
        initClusterService("abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }

    @Test
    public void shouldAllowSettingClusterNameToStringWithAlphaNumericAndPunctuationCharacters() throws Exception {
        initClusterService("valid.cluster!name@#$%^&*()<>?,./:\"'[]\\{}|_+-=");
    }

    @Test
    public void shouldAllowSettingClusterNameToStringWithAlphaNumericAndWhitespaceCharacters() throws Exception {
        initClusterService("valid cluster name");
    }

    @Test
    public void shouldBroadcastMessagesBetweenServices() throws Exception {
        ClusteringService service1 = new ClusteringService().startStandalone("test-cluster1", "config/jgroups-test-config.xml");
        TestConsumer consumer1 = new TestConsumer("hello_1", "hello_2");
        service1.addConsumer(consumer1);

        ClusteringService service2 = new ClusteringService().startStandalone("test-cluster1", "config/jgroups-test-config.xml");
        TestConsumer consumer2 = new TestConsumer("hello_1", "hello_2");
        service2.addConsumer(consumer2);

        try {
            service1.sendMessage("hello_1");
            service2.sendMessage("hello_2");

            consumer1.assertAllPayloadsConsumed();
            consumer2.assertAllPayloadsConsumed();
        } finally {
            service1.shutdown();
            service2.shutdown();
        }
    }

    @Test
    public void shouldAllowGlobalLocking() throws Exception {
        BitSet bits = new BitSet();

        CountDownLatch consumer1Latch = new CountDownLatch(1);
        ClusteringService service1 = new ClusteringService().startStandalone("test-cluster1", "config/jgroups-test-config.xml");
        String consumer1Id = UUID.randomUUID().toString();
        LockConsumer consumer1 = new LockConsumer(consumer1Id, consumer1Latch, 0, bits, service1);
        service1.addConsumer(consumer1);

        CountDownLatch consumer2Latch = new CountDownLatch(1);
        ClusteringService service2 = new ClusteringService().startStandalone("test-cluster1", "config/jgroups-test-config.xml");
        String consumer2Id = UUID.randomUUID().toString();
        LockConsumer consumer2 = new LockConsumer(consumer2Id, consumer2Latch, 1, bits, service2);
        service2.addConsumer(consumer2);

        try {
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
        } finally {
            service1.shutdown();
            service2.shutdown();
        }
    }

    private void initClusterService( String name ) throws Exception {
        this.clusteringService = new ClusteringService().startStandalone(name, "config/jgroups-test-config.xml");
    }

    protected class TestConsumer extends MessageConsumer<String> {
        private List<String> payloads = new ArrayList<>();
        private CountDownLatch payloadsLatch;

        protected TestConsumer(String...expectedPayloads) {
            super(String.class);
            payloads = Arrays.asList(expectedPayloads);
            payloadsLatch = new CountDownLatch(expectedPayloads.length);
        }

        @Override
        public void consume( String payload ) {
            assertTrue(payloads.contains(payload));
            payloadsLatch.countDown();
        }

        protected void assertAllPayloadsConsumed() throws InterruptedException {
            payloadsLatch.await(1, TimeUnit.SECONDS);
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
            this.id  = id;
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
