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
import javax.jcr.version.VersionHistory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.FileUtil;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.observation.Event;

/**
 * Unit test for various clustered repository scenarios.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ClusteredRepositoryTest {

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
    @FixFor( "MODE-2409" )
    public void shouldPropagateVersionableNodeInCluster() throws Exception {
        JcrRepository repository1 = TestingUtil.startRepositoryWithConfig("config/clustered-repo-config.json");
        JcrSession session1 = repository1.login();

        JcrRepository repository2 = TestingUtil.startRepositoryWithConfig("config/clustered-repo-config.json");
        JcrSession session2 = repository2.login();

        try {
            int eventTypes = Event.NODE_ADDED | Event.PROPERTY_ADDED;
            ClusteringEventListener listener = new ClusteringEventListener(3);
            session2.getWorkspace().getObservationManager().addEventListener(listener, eventTypes, null, true, null, null, true);

            Node testNode = session1.getRootNode().addNode("testNode");
            testNode.addMixin("mix:versionable");
            String binary = "test string";
            testNode.setProperty("binaryProperty", session1.getValueFactory().createBinary(binary.getBytes()));
            session1.save();
           
            final String testNodePath = testNode.getPath();
            session1.getWorkspace().getVersionManager().checkin(testNodePath);

            listener.waitForEvents();
            List<String> paths = listener.getPaths();
            assertTrue(paths.contains("/testNode"));
            assertTrue(paths.contains("/testNode/binaryProperty"));
            assertTrue(paths.contains("/testNode/jcr:uuid"));
            assertTrue(paths.contains("/testNode/jcr:baseVersion"));
            assertTrue(paths.contains("/testNode/jcr:primaryType"));
            assertTrue(paths.contains("/testNode/jcr:predecessors"));
            assertTrue(paths.contains("/testNode/jcr:mixinTypes"));
            assertTrue(paths.contains("/testNode/jcr:versionHistory"));
            assertTrue(paths.contains("/testNode/jcr:isCheckedOut"));

            // check whether the node can be found in the second repository ...
            try {
                session2.refresh(false);
                session2.getNode(testNodePath);
                // check that there are 2 versions (base & 1.0)
                VersionHistory versionHistory = session2.getWorkspace().getVersionManager().getVersionHistory("/testNode");
                assertEquals(2, versionHistory.getAllVersions().getSize());
            } catch (PathNotFoundException e) {
                fail("Should have found the '/testNode' created in other repository in this repository: ");
            }
        } finally {
            TestingUtil.killRepositories(repository1, repository2);
        }
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

    /*
     * Each Infinispan configuration persists data in a separate location, and we use replication mode.
     */
    @Test
    @FixFor( {"MODE-1733", "MODE-1943", "MODE-2051", "MODE-2369"} )
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

    @Test
    @FixFor("MODE-1683")
    public void shouldClusterJournals() throws Exception {
        FileUtil.delete("target/clustered");
        JcrRepository repository1 = null;
        JcrRepository repository2 = null;
        try {
            // Start the first process completely ...
            repository1 = TestingUtil.startRepositoryWithConfig("config/clustered-repo-with-journaling-config-1.json");
            Thread.sleep(300);

            // Start the second process completely ...
            repository2 = TestingUtil.startRepositoryWithConfig("config/clustered-repo-with-journaling-config-2.json");
            Thread.sleep(300);

            assertEquals(repository1.runningState().journal().allRecords(false).size(),
                         repository2.runningState().journal().allRecords(false).size());

            // make 1 change which should be propagated in the cluster
            Session session1 = repository1.login();
            session1.getRootNode().addNode("node1");
            session1.save();
            Thread.sleep(300);

            assertEquals(repository1.runningState().journal().allRecords(false).size(),
                         repository2.runningState().journal().allRecords(false).size());

            //shut down the 2nd repo's journal
            repository2.runningState().journal().shutdown();

            // add another node to repo1 - this should be local to repo1 until repo2 comes up
            session1.getRootNode().addNode("node1");
            session1.save();
            session1.logout();
            Thread.sleep(300);

            // start the 2nd repo's journal back up and check that it received the additional node from the 1st node.
            repository2.runningState().journal().start();
            Thread.sleep(500);
            assertEquals(repository1.runningState().journal().allRecords(false).size(),
                         repository2.runningState().journal().allRecords(false).size());
        } finally {
            TestingUtil.killRepositories(repository1, repository2);
        }
    } 
    
    @Test
    @FixFor( "MODE-1903" )
    public void shouldReindexContentInClusterBasedOnTimestsamp() throws Exception {
        FileUtil.delete("target/clustered");
        JcrRepository repository1 = null;
        JcrRepository repository2 = null;
        try {
            // Start the first process completely ...
            repository1 = TestingUtil.startRepositoryWithConfig("config/clustered-repo-with-journaling-config-1.json");
            Thread.sleep(300);

            // Start the second process completely ...
            repository2 = TestingUtil.startRepositoryWithConfig("config/clustered-repo-with-journaling-config-2.json");
            Thread.sleep(300);

            // make 1 change which should be propagated in the cluster
            Session session1 = repository1.login();
            Node node = session1.getRootNode().addNode("repo1_node1");
            node.addMixin("mix:title");
            node.setProperty("jcr:title", "title1");
            session1.save();
            Thread.sleep(300);
            
            // remote events should've been sent out and processed through the cluster causing indexes to be updated on both cluster nodes 
            Session session2 = repository2.login();
            Query query = session2.getWorkspace().getQueryManager().createQuery("select node.[jcr:path] from [mix:title] as node where node.[jcr:title] = 'title1'",
                                                                                Query.JCR_SQL2);
            
            validateQuery().hasNodesAtPaths("/repo1_node1").useIndex("titleIndex").validate(query, query.execute());
            session2.logout();
            
            // shut the second repo down
            long priorToShutdown = System.currentTimeMillis();
            assertTrue("Second repository has not shutdown in the expected amount of time", repository2.shutdown().get(3,
                                                                                                                       TimeUnit.SECONDS));
            
            // add a new node in the first repo
            node = session1.getRootNode().addNode("repo1_node2");
            node.addMixin("mix:title");
            node.setProperty("jcr:title", "title2");
            session1.save();
            
            // start the 2nd repo back up - at the end of this the journals should be up-to-date and ISPN should've done the state
            // transfer
            repository2 = TestingUtil.startRepositoryWithConfig("config/clustered-repo-with-journaling-config-2.json");
            Thread.sleep(300);

            // run a query to check that the index are not yet up-to-date
            session2 = repository2.login();
            org.modeshape.jcr.api.Workspace workspace2 = (org.modeshape.jcr.api.Workspace)session2.getWorkspace();

            query = workspace2.getQueryManager().createQuery(
                    "select node.[jcr:path] from [mix:title] as node where node.[jcr:title] = 'title2'",
                    Query.JCR_SQL2);
            validateQuery().rowCount(0).useIndex("titleIndex").validate(query, query.execute());
            
            // reindex since before stopping the repository
            workspace2.reindexSince(priorToShutdown);
            
            // run a new query to check that we got the change
            query = workspace2.getQueryManager().createQuery(
                    "select node.[jcr:path] from [mix:title] as node where node.[jcr:title] = 'title2'",
                    Query.JCR_SQL2);

            validateQuery().hasNodesAtPaths("/repo1_node2").useIndex("titleIndex").validate(query, query.execute());
        } finally {
            TestingUtil.killRepositories(repository1, repository2); 
        }
    }  
    
    @Test
    @FixFor( "MODE-1903" )
    public void shouldReindexContentInClusterIncrementally() throws Exception {
        FileUtil.delete("target/clustered");
        JcrRepository repository1 = null;
        JcrRepository repository2 = null;
        try {
            // Start the first process completely ...
            repository1 = TestingUtil.startRepositoryWithConfig("config/clustered-repo-with-incremental-indexes-config-1.json");
            Thread.sleep(300);

            // Start the second process completely ...
            repository2 = TestingUtil.startRepositoryWithConfig("config/clustered-repo-with-incremental-indexes-config-2.json");
            Thread.sleep(300);

            // make 1 change which should be propagated in the cluster
            Session session1 = repository1.login();
            Node node = session1.getRootNode().addNode("repo1_node1");
            node.addMixin("mix:title");
            node.setProperty("jcr:title", "title1");
            session1.save();
            Thread.sleep(300);
            
            // shut the second repo down
            assertTrue("Second repository has not shutdown in the expected amount of time", 
                       repository2.shutdown().get(3, TimeUnit.SECONDS));
            
            // add a new node in the first repo
            node = session1.getRootNode().addNode("repo1_node2");
            node.addMixin("mix:title");
            node.setProperty("jcr:title", "title2");
            session1.save();
            
            // start the 2nd repo back up - at the end of this the journals should be up-to-date and ISPN should've done the state
            // transfer
            repository2 = TestingUtil.startRepositoryWithConfig("config/clustered-repo-with-incremental-indexes-config-2.json");
            Thread.sleep(300);

            // run a query to check that the index are not yet up-to-date
            Session session2 = repository2.login();
            org.modeshape.jcr.api.Workspace workspace2 = (org.modeshape.jcr.api.Workspace)session2.getWorkspace();

            // run queries to check that reindexing has worked
            Query query = workspace2.getQueryManager().createQuery(
                    "select node.[jcr:path] from [mix:title] as node where node.[jcr:title] = 'title2'",
                    Query.JCR_SQL2);
            validateQuery().hasNodesAtPaths("/repo1_node2").useIndex("titleIndex").validate(query, query.execute());

            // shut the first repo down
            assertTrue("First repository has not shutdown in the expected amount of time", repository1.shutdown().get(3, TimeUnit.SECONDS));
            
            // add a new node in the second repo
            node = session2.getRootNode().addNode("repo2_node1");
            node.addMixin("mix:title");
            node.setProperty("jcr:title", "title3");
            session2.save();

            // start the 1st repo back up - at the end of this the journals should be up-to-date and ISPN should've done the state
            // transfer
            repository1 = TestingUtil.startRepositoryWithConfig("config/clustered-repo-with-incremental-indexes-config-1.json");
            Thread.sleep(300);            

            session1 = repository1.login();
            query = session1.getWorkspace().getQueryManager().createQuery(
                    "select node.[jcr:path] from [mix:title] as node where node.[jcr:title] = 'title3'",
                    Query.JCR_SQL2);
            validateQuery().hasNodesAtPaths("/repo2_node1").useIndex("titleIndex").validate(query, query.execute());
            
            // shut the second repo down
            assertTrue("Second repository has not shutdown in the expected amount of time",
                       repository2.shutdown().get(3, TimeUnit.SECONDS));

            // remove a node from the first repo and change a value for the other node
            session1.getNode("/repo1_node2").remove();
            session1.getNode("/repo1_node1").setProperty("jcr:title", "title1_edited");
            session1.save();
            
            // bring the 2nd repo back up
            // start the 2nd repo back up - at the end of this the journals should be up-to-date and ISPN should've done the state
            // transfer
            repository2 = TestingUtil.startRepositoryWithConfig("config/clustered-repo-with-incremental-indexes-config-2.json");
            Thread.sleep(300);

            // run a query to check that the indexes are synced
            session2 = repository2.login();
            workspace2 = (org.modeshape.jcr.api.Workspace)session2.getWorkspace();

            query = workspace2.getQueryManager().createQuery(
                    "select node.[jcr:path] from [mix:title] as node where node.[jcr:title] = 'title2'",
                    Query.JCR_SQL2);
            validateQuery().rowCount(0).useIndex("titleIndex").validate(query, query.execute());

            query = workspace2.getQueryManager().createQuery(
                    "select node.[jcr:path] from [mix:title] as node where node.[jcr:title] = 'title1'",
                    Query.JCR_SQL2);
            validateQuery().rowCount(0).useIndex("titleIndex").validate(query, query.execute());

            query = workspace2.getQueryManager().createQuery(
                    "select node.[jcr:path] from [mix:title] as node where node.[jcr:title] = 'title1_edited'",
                    Query.JCR_SQL2);
            validateQuery().hasNodesAtPaths("/repo1_node1").useIndex("titleIndex").validate(query, query.execute());
            
        } finally {
            TestingUtil.killRepositories(repository1, repository2); 
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
        int minBinarySize = 4096;
        byte[] binaryData = new byte[minBinarySize + 2];
        RANDOM.nextBytes(binaryData);

        nodeProcess1 = process1Session.getNode(nodeAbsPath);
        //create a normal string property
        nodeProcess1.setProperty("testProp", "test value");
        //create a binary property
        nodeProcess1.setProperty("binaryProp", process1Session.getValueFactory().createBinary(new ByteArrayInputStream(binaryData)));
        //create a large string which should be stored as a binary
        String largeString = StringUtil.createString('a', minBinarySize + 2);
        nodeProcess1.setProperty("largeString", largeString);
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
        String process2LargeString = nodeProcess2.getProperty("largeString").getString();
        assertEquals(largeString, process2LargeString);

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

    protected ValidateQuery.ValidationBuilder validateQuery() {
        return ValidateQuery.validateQuery().printDetail(false);
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
