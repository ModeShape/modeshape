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

package org.modeshape.jcr;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.jcr.api.observation.Event;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Unit test for various clustered repository scenarios.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ClusteredRepositoryTest extends AbstractTransactionalTest {

    private JcrRepository repository1;
    private JcrSession session1;
    private JcrRepository repository2;
    private JcrSession session2;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ClusteringHelper.bindJGroupsToLocalAddress();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        ClusteringHelper.removeJGroupsBindings();
    }

    @Before
    public void setUp() throws Exception {
        repository1 = TestingUtil.startRepositoryWithConfig("config/clustered-repo-config.json");
        session1 = repository1.login();

        repository2 = TestingUtil.startRepositoryWithConfig("config/clustered-repo-config.json");
        session2 = repository2.login();
    }

    @After
    public void tearDown() throws Exception {
        TestingUtil.killRepositories(repository1, repository2);
    }

    @Test
    @FixFor( "MODE-1618" )
    public void shouldPropagateNodeChangesInCluster() throws Exception {
        int eventTypes = Event.NODE_ADDED | Event.PROPERTY_ADDED;
        ClusteringEventListener listener = new ClusteringEventListener(2);
        session2.getWorkspace().getObservationManager().addEventListener(listener, eventTypes, null, true, null, null, true);

        Node testNode = session1.getRootNode().addNode("testNode");
        String binary = "test string";
        testNode.setProperty("binaryProperty", session1.getValueFactory().createBinary(binary.getBytes()));
        session1.save();

        listener.waitForEvents();
        List<String> paths = listener.getPaths();
        assertEquals(3, paths.size());
        assertTrue(paths.contains("/testNode"));
        assertTrue(paths.contains("/testNode/binaryProperty"));
        assertTrue(paths.contains("/testNode/jcr:primaryType"));
    }

    private class ClusteringEventListener implements EventListener {
        private final List<String> paths;
        private final CountDownLatch eventsLatch;

        private ClusteringEventListener( int expectedEventsCount ) {
            this.paths = new ArrayList<String>();
            this.eventsLatch = new CountDownLatch(expectedEventsCount);
        }

        @Override
        public void onEvent( EventIterator events ) {
            while (events.hasNext()) {
                eventsLatch.countDown();
                Event event = (Event)events.nextEvent();
                try {
                    paths.add(event.getPath());
                } catch (RepositoryException e) {
                    throw new RuntimeException(e);
                }
            }
        }

        void waitForEvents() throws InterruptedException {
            assertTrue(eventsLatch.await(2, TimeUnit.SECONDS));
        }

        public List<String> getPaths() {
            return paths;
        }
    }
}
