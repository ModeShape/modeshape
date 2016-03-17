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

import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
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

    private static List<ClusteringService> cluster;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ClusteringHelper.bindJGroupsToLocalAddress();
        cluster = IntStream.range(0, 4)
                           .mapToObj(i -> {
                               String clusterName = "test-cluster" + (i % 2 == 0 ? "1" : "2");
                               return ClusteringService.startStandalone(clusterName, "config/cluster/jgroups-test-config.xml");
                           })
                           .collect(Collectors.toList());
    }

    @AfterClass
    public static void afterClass() throws Exception {
        cluster.forEach(ClusteringService::shutdown);
        cluster.clear();
        ClusteringHelper.removeJGroupsBindings();
    }

    @Test
    public void shouldBroadcastMessagesBetweenServices() throws Exception {
        ClusteringService service1 = cluster.get(0);
        TestConsumer consumer1 = new TestConsumer("hello_1", "hello_2");
        service1.addConsumer(consumer1);

        ClusteringService service2 = cluster.get(2);
        TestConsumer consumer2 = new TestConsumer("hello_1", "hello_2");
        service2.addConsumer(consumer2);

        service1.sendMessage("hello_1");
        service2.sendMessage("hello_2");

        consumer1.assertAllPayloadsConsumed();
        consumer2.assertAllPayloadsConsumed();
    }

    @Test
    @FixFor( "MODE-2226" )
    public void shouldAllowMultipleForksOffTheSameChannel() throws Exception {
        ClusteringService main11 = cluster.get(0); //cluster1
        ClusteringService main12 = cluster.get(1); //cluster2

        //fork11 communicates via the same fork stack to fork21
        ClusteringService fork11 = startForked(main11);
        TestConsumer consumer11 = new TestConsumer("11", "21");
        fork11.addConsumer(consumer11);

        //fork12 communicates via the same fork stack to fork22
        ClusteringService fork12 = startForked(main12);
        TestConsumer consumer12 = new TestConsumer("12", "22");
        fork12.addConsumer(consumer12);

        ClusteringService main21 = cluster.get(2); //cluster1
        ClusteringService main22 = cluster.get(3); //cluster2

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

    private ClusteringService startForked(ClusteringService mainService) {
        ClusteringService service = ClusteringService.startForked(mainService.getChannel());
        cluster.add(service);
        return service;
    }

    protected class TestConsumer extends MessageConsumer<String> {
        private List<String> payloads = new ArrayList<>();
        private CountDownLatch payloadsLatch;

        protected TestConsumer(String... expectedPayloads) {
            super(String.class);
            payloads = Arrays.asList(expectedPayloads);
            payloadsLatch = new CountDownLatch(expectedPayloads.length);
        }

        @Override
        public void consume(String payload) {
            assertTrue(payload + " not expected", payloads.contains(payload));
            payloadsLatch.countDown();
        }

        protected void assertAllPayloadsConsumed() throws InterruptedException {
            assertTrue("Not all payloads received", payloadsLatch.await(1, TimeUnit.SECONDS));
        }
    }
}
