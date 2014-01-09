/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */

package org.modeshape.jcr.clustering;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
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
            service1.sendMessage("hello_1", 1);
            service2.sendMessage("hello_2", 1);

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

    private class TestConsumer implements MessageConsumer<String> {
        private Set<String> payloads = new TreeSet<String>();

        @Override
        public boolean interestedIn( int messageType ) {
            return true;
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
