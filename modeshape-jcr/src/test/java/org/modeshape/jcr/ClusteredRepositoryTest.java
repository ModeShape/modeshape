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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.jcr.api.observation.Event;

/**
 * Unit test for various clustered repository scenarios.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ClusteredRepositoryTest extends AbstractTransactionalTest {

    private static final Random RANDOM = new Random();

    @BeforeClass
    public static void beforeClass() throws Exception {
        ClusteringHelper.bindJGroupsToLocalAddress();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        ClusteringHelper.removeJGroupsBindings();
    }

    @Test
    @FixFor( {"MODE-1618", "MODE-2830"} )
    public void shouldPropagateNodeChangesInCluster() throws Exception {
        JcrRepository repository1 = TestingUtil.startRepositoryWithConfig("config/clustered-repo-config.json");
        JcrSession session1 = repository1.login();

        JcrRepository repository2 = TestingUtil.startRepositoryWithConfig("config/clustered-repo-config.json");
        JcrSession session2 = repository2.login();

        try {
            int eventTypes = Event.NODE_ADDED | Event.PROPERTY_ADDED;
            ClusteringEventListener listener = new ClusteringEventListener(3);
            session2.getWorkspace().getObservationManager().addEventListener(listener, eventTypes, null, true, null, null, true);

            Node testNode = session1.getRootNode().addNode("testNode");
            String binary = "test string";
            testNode.setProperty("binaryProperty", session1.getValueFactory().createBinary(binary.getBytes()));
            session1.save();
            final String testNodePath = testNode.getPath();

            listener.waitForEvents();
            List<String> paths = listener.getPaths();
            assertEquals(3, paths.size());
            assertTrue(paths.contains("/testNode"));
            assertTrue(paths.contains("/testNode/binaryProperty"));
            assertTrue(paths.contains("/testNode/jcr:primaryType"));

            // check whether the node can be found in the second repository ...
            try {
                session2.refresh(false);
                session2.getNode(testNodePath);
            } catch (PathNotFoundException e) {
                fail("Should have found the '/testNode' created in other repository in this repository: ");
            }
        } finally {
            TestingUtil.killRepositories(repository1, repository2);
        }
    }

    @Test
    @FixFor( "MODE-1701" )
    public void shouldStartRepositoryWithJGroupsXMLConfigurationFile() throws Exception {
        JcrRepository repository = null;
        try {
            repository = TestingUtil.startRepositoryWithConfig("config/clustered-repo-config-jgroups-file.json");
            assertEquals(ModeShapeEngine.State.RUNNING, repository.getState());
        } finally {
            TestingUtil.killRepository(repository);
        }
    }

    @Test( expected = RepositoryException.class )
    @FixFor( "MODE-1701" )
    public void shouldNotStartRepositoryWithInvalidJGroupsConfiguration() throws Exception {
        TestingUtil.startRepositoryWithConfig("config/clustered-repo-config-invalid-jgroups-file.json");
    }

    /*
     * Each Infinispan configuration persists data in a separate location, and we use replication mode.
     */
    @Test
    @FixFor( {"MODE-1733", "MODE-1943", "MODE-2051"} )
    public void shouldClusterWithReplicatedCachePersistedToSeparateAreasForEachProcess() throws Exception {
        FileUtil.delete("target/clustered");
        JcrRepository repository1 = null;
        JcrRepository repository2 = null;
        try {
            // Start the first process completely ...
            repository1 = TestingUtil.startRepositoryWithConfig("config/repo-config-clustered-persistent-1.json");
            Session session1 = repository1.login();
            assertInitialContentPersisted(session1);

            // Start the second process completely ...
            repository2 = TestingUtil.startRepositoryWithConfig("config/repo-config-clustered-persistent-2.json");
            Session session2 = repository2.login();
            assertInitialContentPersisted(session2);

            // in this setup, index changes are local but we are in clustered mode, so changes should also be indexed
            assertChangesArePropagatedInCluster(session1, session2, "node1");
            assertChangesArePropagatedInCluster(session2, session1, "node2");

            session1.logout();
            session2.logout();
        } finally {
            Logger.getLogger(getClass())
                  .debug("Killing repositories in shouldStartClusterWithReplicatedCachePersistedToSeparateAreasForEachProcess");
            TestingUtil.killRepositories(repository1, repository2);
            FileUtil.delete("target/clustered");
        }

    }

    private void assertInitialContentPersisted( Session session ) throws RepositoryException {
        assertThat(session.getRootNode(), is(notNullValue()));
        assertThat(session.getNode("/Cars"), is(notNullValue()));
        assertThat(session.getNode("/Cars/Hybrid"), is(notNullValue()));
        assertThat(session.getNode("/Cars/Hybrid/Toyota Prius"), is(notNullValue()));
        assertThat(session.getWorkspace().getNodeTypeManager().getNodeType("car:Car"), is(notNullValue()));
        assertThat(session.getWorkspace().getNodeTypeManager().getNodeType("air:Aircraft"), is(notNullValue()));
    }

    /**
     * Each Infinispan configuration persists data in a separate location, we use replication mode and the indexes are clustered
     * via JGroups.
     * 
     * @throws Exception
     */
    @Test
    @FixFor( "MODE-1897" )
    public void shouldStartClusterWithReplicatedCachePersistedToSeparateAreasForEachProcessAndClusteringJGroupsIndexing()
        throws Exception {
        FileUtil.delete("target/clustered");
        JcrRepository repository1 = null;
        JcrRepository repository2 = null;
        try {
            // Start the master process completely ...
            repository1 = TestingUtil.startRepositoryWithConfig("config/repo-config-clustered-indexes-1.json");
            Session session1 = repository1.login();

            // Start the slave process completely ...
            repository2 = TestingUtil.startRepositoryWithConfig("config/repo-config-clustered-indexes-2.json");
            Session session2 = repository2.login();

            // in this setup, index changes clustered, so they *should be* be sent across to other nodes
            assertChangesArePropagatedInCluster(session1, session2, "node1");

            session1.logout();
            session2.logout();
        } finally {
            Logger.getLogger(getClass())
                  .debug("Killing repositories in shouldStartClusterWithReplicatedCachePersistedToSeparateAreasForEachProcessAndClusteringJGroupsIndexing");
            TestingUtil.killRepositories(repository1, repository2);
            FileUtil.delete("target/clustered");
        }

    }

    /*
     * Each Infinispan configuration persists data to the SAME location, including indexes. This is NOT a valid option because the
     * indexes get corrupted, so we will ignore this
     */
    @Ignore
    @Test
    @FixFor( "MODE-1733" )
    public void shouldStartClusterWithReplicatedCachePersistedToSameAreaForBothProcesses() throws Exception {
        FileUtil.delete("target/clustered");
        JcrRepository repository1 = null;
        JcrRepository repository2 = null;
        try {
            // Start the first process completely ...
            repository1 = TestingUtil.startRepositoryWithConfig("config/repo-config-clustered-persistent-1.json");
            Session session1 = repository1.login();
            assertThat(session1.getRootNode(), is(notNullValue()));

            // Start the second process completely ...
            repository2 = TestingUtil.startRepositoryWithConfig("config/repo-config-clustered-persistent-1.json");
            Session session2 = repository2.login();
            assertThat(session2.getRootNode(), is(notNullValue()));

            session1.logout();
            session2.logout();
        } finally {
            TestingUtil.killRepositories(repository1, repository2);
            FileUtil.delete("target/clustered");
        }
    }

    private void assertChangesArePropagatedInCluster( Session process1Session,
                                                      Session process2Session,
                                                      String nodeName )
        throws Exception {
        String nodeAbsPath = "/" + nodeName;
        String pathQuery = "select * from [nt:unstructured] as n where n.[jcr:path]='" + nodeAbsPath + "'";

        // Add a jcr node in the 1st process and check it can be queried
        Node nodeProcess1 = process1Session.getRootNode().addNode(nodeName);
        process1Session.save();
        queryAndExpectResults(process1Session, pathQuery, 1);

        // wait a bit for state transfer to complete
        Thread.sleep(1500);

        // check that the custom jcr node created on the other process, was sent to this one
        assertNotNull(process2Session.getNode(nodeAbsPath));
        queryAndExpectResults(process2Session, pathQuery, 1);

        // set a property of that node and check it's send through the cluster
        byte[] binaryData = new byte[4096 * 2];
        RANDOM.nextBytes(binaryData);

        nodeProcess1 = process1Session.getNode(nodeAbsPath);
        //create a normal string property
        nodeProcess1.setProperty("testProp", "test value");
        //create a binary property
        nodeProcess1.setProperty("binaryProp", process1Session.getValueFactory().createBinary(new ByteArrayInputStream(binaryData)));
        process1Session.save();
        String propertyQuery = "select * from [nt:unstructured] as n where n.[testProp]='test value'";
        queryAndExpectResults(process1Session, propertyQuery, 1);

        // wait a bit for state transfer to complete
        Thread.sleep(1500);
        // check the property change was made in the indexes on the second node
        queryAndExpectResults(process2Session, propertyQuery, 1);

        //check the properties were sent across the cluster
        Node nodeProcess2 = process2Session.getNode(nodeAbsPath);
        assertEquals("test value", nodeProcess2.getProperty("testProp").getString());
        Binary binary = nodeProcess2.getProperty("binaryProp").getBinary();
        byte[] process2Data = IoUtil.readBytes(binary.getStream());
        assertArrayEquals("Binary data not propagated in cluster", binaryData, process2Data);

        // Remove the node in the first process and check it's removed from the indexes across the cluster
        nodeProcess1 = process1Session.getNode(nodeAbsPath);
        nodeProcess1.remove();
        process1Session.save();
        queryAndExpectResults(process1Session, pathQuery, 0);
        // wait a bit for state transfer to complete
        Thread.sleep(1500);
        // check the node was removed from the indexes in the second cluster node
        queryAndExpectResults(process2Session, pathQuery, 0);
        try {
            process2Session.getNode(nodeAbsPath);
            fail(nodeAbsPath + " not removed from other node in the cluster");
        } catch (PathNotFoundException e) {
            //expected
        }
    }

    private void queryAndExpectResults(Session session, String queryString, int howMany) throws RepositoryException{
        QueryManager queryManager = session.getWorkspace().getQueryManager();
        Query query = queryManager.createQuery(queryString, Query.JCR_SQL2);
        NodeIterator nodes = query.execute().getNodes();
        assertEquals(howMany, nodes.getSize());
    }

    protected class ClusteringEventListener implements EventListener {
        private final List<String> paths;
        private final CountDownLatch eventsLatch;

        protected ClusteringEventListener( int expectedEventsCount ) {
            this.paths = new ArrayList<String>();
            this.eventsLatch = new CountDownLatch(expectedEventsCount);
        }

        @Override
        public void onEvent( EventIterator events ) {
            while (events.hasNext()) {
                Event event = (Event)events.nextEvent();
                try {
                    paths.add(event.getPath());
                } catch (RepositoryException e) {
                    throw new RuntimeException(e);
                }
                eventsLatch.countDown();
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
