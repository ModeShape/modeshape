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
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import javax.jcr.Binary;
import javax.jcr.ItemExistsException;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.nodetype.NodeType;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;
import javax.jcr.query.QueryManager;
import javax.jcr.version.VersionHistory;
import javax.jcr.version.VersionManager;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.IoUtil;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.api.JcrTools;
import org.modeshape.jcr.api.observation.Event;

/**
 * Unit test for various clustered repository scenarios.
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ClusteredRepositoryTest {

    private static final Random RANDOM = new Random();

    private String node1Id = "cnode_" + UUID.randomUUID().toString();
    private String node2Id = "cnode_" + UUID.randomUUID().toString();
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        ClusteringHelper.bindJGroupsToLocalAddress();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        ClusteringHelper.removeJGroupsBindings();
    }
    
    @Before
    public void before() throws Exception {
        // c3p0 is async, so it might take a bit until we can do this....
        TestingUtil.waitUntilFolderCleanedUp("target/clustered");
    }

    @Test
    @FixFor( "MODE-2409" )
    public void shouldPropagateVersionableNodeInCluster() throws Exception {
        JcrRepository repository1 = TestingUtil.startClusteredRepositoryWithConfig("config/cluster/repo-config-clustered.json",
                                                                                   node1Id);
        JcrSession session1 = repository1.login();

        JcrRepository repository2 = TestingUtil.startClusteredRepositoryWithConfig("config/cluster/repo-config-clustered.json",
                                                                                   node2Id);
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
    @FixFor( "MODE-2617" )
    public void shouldCheckinNodesConcurrentlyInCluster() throws Exception {
        JcrRepository repository1 = TestingUtil.startRepositoryWithConfig("config/cluster/repo-config-clustered.json");
        JcrSession session = repository1.login();

        JcrRepository repository2 = TestingUtil.startRepositoryWithConfig("config/cluster/repo-config-clustered.json");
        repository2.login();
        int threadCount = 5;
        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        try {
            Node testRoot = session.getRootNode().addNode("testRoot");
            String testRootPath = testRoot.getPath();
            IntStream.range(0, threadCount).forEach(i -> {
                try {
                    Node parent = testRoot.addNode("parent-" + i);
                    parent.addMixin(NodeType.MIX_VERSIONABLE);
                } catch (RepositoryException e) {
                    throw new RuntimeException(e);
                }
            });
            session.save();
            session.logout();

            List<Callable<String>> tasks = IntStream.range(0, threadCount)
                                                    .mapToObj(i -> (Callable<String>) () -> {
                                                        JcrSession taskSession = repository1.login();
                                                        try {
                                                            VersionManager versionManager = taskSession.getWorkspace()
                                                                                                       .getVersionManager();
                                                            Node parent = taskSession.getNode(testRootPath + "/parent-" + i);
                                                            versionManager.checkout(parent.getPath());
                                                            Node child = parent.addNode("child-" + i);
                                                            child.addMixin(NodeType.MIX_VERSIONABLE);
                                                            child.getSession().save();
                                                            versionManager.checkout(child.getPath());
                                                            versionManager.checkin(child.getPath());
                                                            versionManager.checkin(parent.getPath());
                                                            return child.getPath();
                                                        } finally {
                                                            taskSession.logout();
                                                        }
                                                    })
                                                    .collect(Collectors.toList());

            List<String> expectedResults = IntStream.range(0, threadCount)
                                                    .mapToObj(i -> testRootPath + "/parent-" + i + "/child-" + i)
                                                    .collect(Collectors.toList());
            List<String> actualResults = executorService.invokeAll(tasks)
                                                        .stream()
                                                        .map(result -> {
                                                            try {
                                                                return result.get(5, TimeUnit.SECONDS);
                                                            } catch (Exception e) {
                                                                throw new RuntimeException(e);
                                                            }
                                                        })
                                                        .collect(Collectors.toList());
            Collections.sort(actualResults);
            assertEquals(expectedResults, actualResults);
        } finally {
            executorService.shutdown();
            TestingUtil.killRepositories(repository1, repository2);
        }
    }

    @Test
    @FixFor( {"MODE-1618", "MODE-2830", "MODE-1733", "MODE-1943", "MODE-2051", "MODE-2369"} )
    public void shouldPropagateNodeChangesInCluster() throws Exception {
        JcrRepository repository1 = TestingUtil.startClusteredRepositoryWithConfig("config/cluster/repo-config-clustered.json",
                                                                                   node1Id);
        JcrSession session1 = repository1.login();
        assertInitialContentPersisted(session1);

        JcrRepository repository2 = TestingUtil.startClusteredRepositoryWithConfig("config/cluster/repo-config-clustered.json",
                                                                                   node2Id);
        JcrSession session2 = repository2.login();
        assertInitialContentPersisted(session2);

        try {
            assertChangesVisibleViaListener(session1, session2);
            assertChangesArePropagatedInCluster(session1, session2, "node1");
            assertChangesArePropagatedInCluster(session2, session1, "node2");
        } finally {
            TestingUtil.killRepositories(repository1, repository2);
        }
    }
    
    @Test
    @FixFor( {"MODE-2077"} )
    public void shouldPropagateNodeChangesInClusterWithDBLocking() throws Exception {
        JcrRepository repository1 = TestingUtil.startClusteredRepositoryWithConfig("config/cluster/repo-config-clustered-db-locking.json",
                                                                                   node1Id);
        JcrSession session1 = repository1.login();
        assertInitialContentPersisted(session1);

        JcrRepository repository2 = TestingUtil.startClusteredRepositoryWithConfig("config/cluster/repo-config-clustered-db-locking.json",
                                                                                   node2Id);
        JcrSession session2 = repository2.login();
        assertInitialContentPersisted(session2);

        try {
            assertChangesVisibleViaListener(session1, session2);
            assertChangesArePropagatedInCluster(session1, session2, "node1");
            assertChangesArePropagatedInCluster(session2, session1, "node2");
        } finally {
            TestingUtil.killRepositories(repository1, repository2);
        }
    }

    private void assertChangesVisibleViaListener(JcrSession session1,
                                                JcrSession session2) throws RepositoryException, InterruptedException {
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
        Thread.sleep(500);
        try {
            session2.refresh(false);
            session2.getNode(testNodePath);
        } catch (PathNotFoundException e) {
            fail("Should have found the '/testNode' created in other repository in this repository: ");
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
        JcrRepository repository1 = null;
        JcrRepository repository2 = null;
        try {
            // Start the first process completely ...
            repository1 = TestingUtil.startClusteredRepositoryWithConfig(
                    "config/cluster/repo-config-clustered-journal-indexes.json", node1Id);
            
            // Start the second process completely ...
            repository2 = TestingUtil.startClusteredRepositoryWithConfig(
                    "config/cluster/repo-config-clustered-journal-indexes.json", node2Id);

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
    public void shouldReindexContentInClusterBasedOnTimestamp() throws Exception {
        JcrRepository repository1 = null;
        JcrRepository repository2 = null;
        try {
            repository1 = TestingUtil.startClusteredRepositoryWithConfig(
                    "config/cluster/repo-config-clustered-journal-indexes.json", node1Id);

            // Start the second process completely ...
            repository2 = TestingUtil.startClusteredRepositoryWithConfig(
                    "config/cluster/repo-config-clustered-journal-indexes.json", node2Id);
            RepositoryConfiguration repository2Config = repository2.getConfiguration();

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
            assertTrue("Second repository has not shutdown in the expected amount of time",
                       repository2.shutdown().get(5, TimeUnit.SECONDS));
            
            // add a new node in the first repo
            node = session1.getRootNode().addNode("repo1_node2");
            node.addMixin("mix:title");
            node.setProperty("jcr:title", "title2");
            session1.save();
            
            // start the 2nd repo back up - at the end of this the journals should be up-to-date
            repository2 = new JcrRepository(repository2Config);
            repository2.start();
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
        JcrRepository repository1 = null;
        JcrRepository repository2 = null;
        try {
            // Start the first process completely ...
            repository1 = TestingUtil.startClusteredRepositoryWithConfig(
                    "config/cluster/repo-config-clustered-journal-incremental-indexes.json",
                    node1Id);

            // Start the second process completely ...
            repository2 = TestingUtil.startClusteredRepositoryWithConfig(
                    "config/cluster/repo-config-clustered-journal-incremental-indexes.json", node2Id);
            RepositoryConfiguration repository2Config = repository2.getConfiguration();

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
            
            // start the 2nd repo back up - at the end of this the journals should be up-to-date
            repository2 = new JcrRepository(repository2Config);
            repository2.start();
            Thread.sleep(300);

            // run a query to check that the index are not yet up-to-date
            Session session2 = repository2.login();
            org.modeshape.jcr.api.Workspace workspace2 = (org.modeshape.jcr.api.Workspace)session2.getWorkspace();

            // run queries to check that reindexing has worked
            Query query = workspace2.getQueryManager().createQuery(
                    "select node.[jcr:path] from [mix:title] as node where node.[jcr:title] = 'title2'",
                    Query.JCR_SQL2);
            validateQuery().hasNodesAtPaths("/repo1_node2").useIndex("titleIndex").validate(query, query.execute());
            
            // shut the second repo down
            assertTrue("Second repository has not shutdown in the expected amount of time",
                       repository2.shutdown().get(3, TimeUnit.SECONDS));

            // remove a node from the first repo and change a value for the other node
            session1.getNode("/repo1_node2").remove();
            session1.getNode("/repo1_node1").setProperty("jcr:title", "title1_edited");
            session1.save();
            
            // start the 2nd repo back up - at the end of this the journals should be up-to-date
            repository2 = new JcrRepository(repository2Config);
            repository2.start();
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

    @Test
    @FixFor("MODE-2517")
    public void shouldPersistReindexedContentInCluster() throws Exception {
        JcrRepository repository1 = null;
        JcrRepository repository2 = null;
        try {
            // Start the first process completely ...
            repository1 = TestingUtil.startClusteredRepositoryWithConfig(
                    "config/cluster/repo-config-clustered-journal-indexes.json", node1Id);
            // Start the second process completely ...
            repository2 = TestingUtil.startClusteredRepositoryWithConfig(
                    "config/cluster/repo-config-clustered-journal-indexes.json", node2Id);
            RepositoryConfiguration repository2Config = repository2.getConfiguration();
            
            // make 1 change which should be propagated in the cluster
            Session session1 = repository1.login();
            Node node = session1.getRootNode().addNode("repo1_node1");
            node.addMixin("mix:title");
            node.setProperty("jcr:title", "title1");
            session1.save();
            Thread.sleep(300);

            // check the indexes have been updated on the 2nd node
            Session session2 = repository2.login();
            Query query = session2.getWorkspace().getQueryManager().createQuery(
                    "select node.[jcr:path] from [mix:title] as node where node.[jcr:title] = 'title1'",
                    Query.JCR_SQL2);
            validateQuery().hasNodesAtPaths("/repo1_node1").useIndex("titleIndex").validate(query, query.execute());
           
            // shut the second repo down
            TestingUtil.killRepository(repository2);
            
            // add a new node in the first repo
            node = session1.getRootNode().addNode("repo1_node2");
            node.addMixin("mix:title");
            node.setProperty("jcr:title", "title2");
            session1.save();

            // start the 2nd repo back up and force a reindexing
            repository2 = new JcrRepository(repository2Config);
            repository2.start();
            Thread.sleep(300);
            
            // run a query to check that the index are not yet up-to-date
            session2 = repository2.login();
            query = session2.getWorkspace().getQueryManager().createQuery(
                    "select node.[jcr:path] from [mix:title] as node where node.[jcr:title] = 'title2'",
                    Query.JCR_SQL2);
            validateQuery().rowCount(0).useIndex("titleIndex").validate(query, query.execute());
            
            ((org.modeshape.jcr.api.Workspace)session2.getWorkspace()).reindex("/");

            // run queries to check that reindexing has worked
            query = session2.getWorkspace().getQueryManager().createQuery(
                    "select node.[jcr:path] from [mix:title] as node where node.[jcr:title] = 'title2'",
                    Query.JCR_SQL2);
            validateQuery().hasNodesAtPaths("/repo1_node2").useIndex("titleIndex").validate(query, query.execute());

            // shut the second repo down
            TestingUtil.killRepository(repository2);
            
            // start the 2nd repo back up and check that reindexed data is still there
            repository2 = new JcrRepository(repository2Config);
            repository2.start();
            Thread.sleep(300);
            
            session2 = repository2.login();
            query = session2.getWorkspace().getQueryManager().createQuery(
                    "select node.[jcr:path] from [mix:title] as node where node.[jcr:title] = 'title2'",
                    Query.JCR_SQL2);
            validateQuery().hasNodesAtPaths("/repo1_node2").useIndex("titleIndex").validate(query, query.execute());
        } finally {
            TestingUtil.killRepositories(repository1, repository2);            
        }
    }

    @Test
    @FixFor({"MODE-1701", "MODE-2542"})
    public void shouldNotStartRepositoryWithInvalidJGroupsConfiguration() throws Exception {
        try {
            TestingUtil.startRepositoryWithConfig("config/cluster/repo-config-invalid-clustering.json");
            fail("Should reject invalid JGroups file...");
        } catch (RuntimeException e) {
            //expected
        }
    }

    @Test
    public void shouldLockNodesCorrectlyInCluster() throws Exception {
        JcrRepository repository1 = TestingUtil.startClusteredRepositoryWithConfig("config/cluster/repo-config-clustered.json",
                                                                                   node1Id);
        JcrSession session1 = repository1.login();

        JcrRepository repository2 = TestingUtil.startClusteredRepositoryWithConfig("config/cluster/repo-config-clustered.json",
                                                                                   node2Id);
        JcrSession session2 = repository2.login();

        session1.getRootNode().addNode("folder", "nt:folder");
        session1.save();
        
        assertNotNull(session2.getNode("/folder"));
        session1.logout();
        session2.logout();
        
        CyclicBarrier cyclicBarrier = new CyclicBarrier(3);
        // add a number of file names to a list
        List<String> fileNames = IntStream.range(1, 100).mapToObj(i -> "file" + i).collect(Collectors.toList());
        ExecutorService executors = Executors.newFixedThreadPool(2);
        try {
            // run a tasks on cluster node 1 which attempts to create each file from that list under the parent folder
            CompletableFuture<Boolean> taskOnClusterNode1 = CompletableFuture.supplyAsync(addFilesToFolder(repository1,
                                                                                                           cyclicBarrier,
                                                                                                           "node1",
                                                                                                           fileNames), executors);
            List<String> revertedList = new ArrayList<>(fileNames);
            Collections.reverse(revertedList);
            // run a tasks on cluster node 2 which attempts to create each file from that list in reverse under the parent folder
            CompletableFuture<Boolean> taskOnClusterNode2 = CompletableFuture.supplyAsync(addFilesToFolder(repository2,
                                                                                                           cyclicBarrier,
                                                                                                           "node2",
                                                                                                           revertedList), executors);
            cyclicBarrier.await(10, TimeUnit.SECONDS);
            boolean resultFromNode1 = taskOnClusterNode1.get(10, TimeUnit.SECONDS);
            boolean resultFromNode2 = taskOnClusterNode2.get(10, TimeUnit.SECONDS);
            // nt:folder does not allow SNS, so if locking working correctly only one of the 2 cluster nodes should've managed
            // to add all the files
            if (resultFromNode1 && resultFromNode2) {
                fail("Only one of the cluster nodes should've succeeded ");
            } 
            String expectedClusterNode = resultFromNode1 ? "node1" : "node2";
            JcrSession session = repository1.login();
            Node folder = session.getNode("/folder");
            for (NodeIterator nodeIterator = folder.getNodes(); nodeIterator.hasNext();) {
                Node file = nodeIterator.nextNode();
                InputStream is = file.getNode("jcr:content").getProperty("jcr:data").getBinary().getStream();
                String content = IoUtil.read(is);
                assertEquals(expectedClusterNode, content);
            }
            session.logout();
        } finally {
            TestingUtil.killRepositories(repository1, repository2);
            executors.shutdownNow();
        }
    }

    private Supplier<Boolean> addFilesToFolder(JcrRepository repository, CyclicBarrier cyclicBarrier, String clusterNodeId, 
                                               List<String> fileNames) throws RepositoryException {
        JcrTools tools = new JcrTools();
        JcrSession session = repository.login();
        return  () -> {
            try {
                fileNames.forEach(fileName -> {
                    try {
                        tools.uploadFile(session, "/folder/" + fileName, new ByteArrayInputStream(clusterNodeId.getBytes()));
                    } catch (RepositoryException | IOException e) {
                        throw new RuntimeException(e);
                    }
                });
                cyclicBarrier.await();
                session.save();
                return true;
            } catch (ItemExistsException ies) {
                return false;
            } catch (Exception e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            } finally {
                if (session != null) {
                    session.logout();
                }
            }
        };
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

        Thread.sleep(300);

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
        Thread.sleep(300);

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
        Thread.sleep(300);

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
            this.paths = new ArrayList<>();
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
