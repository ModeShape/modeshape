/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.modeshape.jcr.clustering;

import static org.junit.Assert.assertArrayEquals;
import static org.modeshape.jcr.ClusteringHelper.startNewClusteringService;
import java.util.Set;
import java.util.TreeSet;
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

    @Test( expected = IllegalStateException.class )
    public void shouldNotAllowSettingClusterNameToNull() throws Exception {
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
        ClusteringService service1 = startNewClusteringService("test-cluster1");
        TestConsumer consumer1 = new TestConsumer();
        service1.addConsumer(consumer1);

        ClusteringService service2 = startNewClusteringService("test-cluster1");
        TestConsumer consumer2 = new TestConsumer();
        service2.addConsumer(consumer2);

        try {
            service1.sendMessage("hello_1");
            service2.sendMessage("hello_2");

            //we need a delay to make sure the messages arrive
            Thread.sleep(200);

            String[] expectedPayloads = new String[]{"hello_1", "hello_2"};
            assertArrayEquals(expectedPayloads, consumer1.getPayloads().toArray(new String[0]));
            assertArrayEquals(expectedPayloads, consumer2.getPayloads().toArray(new String[0]));
        } finally {
            service1.shutdown();
            service2.shutdown();
        }
    }

    private void initClusterService( String name ) throws Exception {
        this.clusteringService = startNewClusteringService(name);
    }

    private class TestConsumer extends MessageConsumer<String> {
        private Set<String> payloads = new TreeSet<String>();

        private TestConsumer() {
            super(String.class);
        }

        @Override
        public void consume( String payload ) {
            payloads.add(payload);
        }

        private Set<String> getPayloads() {
            return payloads;
        }
    }
}
