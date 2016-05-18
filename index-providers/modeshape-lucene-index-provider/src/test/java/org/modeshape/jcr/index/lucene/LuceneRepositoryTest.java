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
package org.modeshape.jcr.index.lucene;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.modeshape.jcr.ValidateQuery.validateQuery;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.query.Query;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.ClusteringHelper;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.TestingUtil;
import org.modeshape.jcr.api.Repository;

/**
 * Unit tests for generic operations involving a repository which has a {@link LuceneIndexProvider} configured. Tests which are 
 * not index/search specific should go here. All others should go in {@link LuceneIndexProviderTest}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class LuceneRepositoryTest {
    
    private Repository repository;

    @BeforeClass
    public static void beforeClass() throws Exception {
        ClusteringHelper.bindJGroupsToLocalAddress();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        ClusteringHelper.removeJGroupsBindings();
    }
    
    @After
    public void after() {
        if (repository != null) {
            TestingUtil.killRepositories(repository);
        }
    }
    
    @Test
    public void shouldAllowAdvancedLuceneConfiguration() throws Exception {
        repository = TestingUtil.startRepositoryWithConfig(
                "config/repo-config-persistent-lucene-provider-advanced-settings.json");
        
        //add a node in the default ws
        Session defaultSession = repository.login();
        Node node = defaultSession.getRootNode().addNode("node1");
        node.addMixin("mix:title");
        node.setProperty("jcr:title", "title1");
        defaultSession.save();

        //add a node in the other ws
        Session otherSession = repository.login("other");
        node = otherSession.getRootNode().addNode("node2");
        node.addMixin("mix:title");
        node.setProperty("jcr:title", "title2");
        otherSession.save();

        //test that each query is local to its own workspace....
        Query query = defaultSession.getWorkspace().getQueryManager().createQuery(
                "select node.[jcr:path] from [mix:title] as node where node.[jcr:title] LIKE 'title%'", Query.JCR_SQL2);
        validateQuery().hasNodesAtPaths("/node1").useIndex("titleIndex").validate(query, query.execute());

        query = otherSession.getWorkspace().getQueryManager().createQuery(
                "select node.[jcr:path] from [mix:title] as node where node.[jcr:title] LIKE 'title%'", Query.JCR_SQL2);
        validateQuery().hasNodesAtPaths("/node2").useIndex("titleIndex").validate(query, query.execute());
    }

    @Test
    @FixFor( "MODE-1903" )
    public void shouldReindexContentInClusterIncrementally() throws Exception {
        TestingUtil.waitUntilFolderCleanedUp("target/clustered");
        
        JcrRepository repository1 = null;
        JcrRepository repository2 = null;
        try {
            // Start the first process completely ...
            String clusterNode1 = UUID.randomUUID().toString();
            repository1 = TestingUtil.startClusteredRepositoryWithConfig(
                    "config/repo-config-clustered-incremental-indexes.json", clusterNode1);
            
            // Start the second process completely ...
            String clusterNode2 = UUID.randomUUID().toString();
            repository2 = TestingUtil.startClusteredRepositoryWithConfig(
                    "config/repo-config-clustered-incremental-indexes.json", clusterNode2);

            // make 1 change which should be propagated in the cluster
            Session session1 = repository1.login();
            Node node = session1.getRootNode().addNode("repo1_node1");
            node.addMixin("mix:title");
            node.setProperty("jcr:title", "title1");
            session1.save();
            Thread.sleep(300);

            TestingUtil.killRepository(repository2);

            // add a new node in the first repo
            node = session1.getRootNode().addNode("repo1_node2");
            node.addMixin("mix:title");
            node.setProperty("jcr:title", "title2");
            session1.save();

            // start the 2nd repo back up - at the end of this the journals should be up-to-date
            repository2 = TestingUtil.startClusteredRepositoryWithConfig(
                    "config/repo-config-clustered-incremental-indexes.json", clusterNode2);

            // run a query to check that the index are not yet up-to-date
            Session session2 = repository2.login();
            org.modeshape.jcr.api.Workspace workspace2 = (org.modeshape.jcr.api.Workspace)session2.getWorkspace();

            // run queries to check that reindexing has worked
            Query query = workspace2.getQueryManager().createQuery(
                    "select node.[jcr:path] from [mix:title] as node where node.[jcr:title] = 'title2'",
                    Query.JCR_SQL2);
            validateQuery().hasNodesAtPaths("/repo1_node2").useIndex("titleIndex").validate(query, query.execute());

            // shut the second repo down
            TestingUtil.killRepository(repository2);
            Thread.sleep(100);
            
            // remove a node from the first repo and change a value for the other node
            session1.getNode("/repo1_node2").remove();
            session1.getNode("/repo1_node1").setProperty("jcr:title", "title1_edited");
            session1.save();

            // start the 2nd repo back up - at the end of this the journals should be up-to-date and ISPN should've done the state
            // transfer
            repository2 = TestingUtil.startClusteredRepositoryWithConfig(
                    "config/repo-config-clustered-incremental-indexes.json", clusterNode2);

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
    @FixFor( "MODE-2586" )
    public void shouldReadIndexesFromLucene53() throws Exception {
        // unzip a folder with 53 indexes and data with 2 nodes: /node1 and /node2, both of which have mix:title and a title property
        FileUtil.unzip(LuceneRepositoryTest.class.getClassLoader().getResourceAsStream("lucene53_indexes.zip"), "target/indexes");
        FileUtil.unzip(LuceneRepositoryTest.class.getClassLoader().getResourceAsStream("lucene53_repo.zip"), "target/repo");
        
        // fire up the repository and check that the indexes and data are still being read
        repository = TestingUtil.startRepositoryWithConfig("config/repo-config-backward-compatibility-indexes.json");

        Session session = repository.login();
        Node node1 = session.getNode("/node1");
        assertTrue(node1.isNodeType("mix:title"));
        assertEquals("title1", node1.getProperty("jcr:title").getString());
        Node node2 = session.getNode("/node2");
        assertTrue(node2.isNodeType("mix:title"));
        assertEquals("title2", node2.getProperty("jcr:title").getString());        
        
        //test that each query is local to its own workspace....
        Query query = session.getWorkspace().getQueryManager().createQuery(
                "select node.[jcr:path] from [mix:title] as node where node.[jcr:title] LIKE 'title%'", Query.JCR_SQL2);
        validateQuery().hasNodesAtPaths("/node1", "/node2").useIndex("titleIndex").validate(query, query.execute());
    }
    
    @Test
    @FixFor( "MODE-2585" )
    @Ignore(" perf test")
    public void shouldQueryLargeNumberOfNodes() throws Exception {
        assertTrue(FileUtil.delete("target/repo"));
        assertTrue(FileUtil.delete("target/indexes"));
        repository = TestingUtil.startRepositoryWithConfig("config/repo-config-backward-compatibility-indexes.json");
        Session session = repository.login();
        int nodeCount = 10003;
        int batchSize = 1000;
        IntStream.range(0, nodeCount).forEach(i -> {
            int idx = i + 1;
            try {
                Node node = session.getRootNode().addNode("node" + idx);
                node.addMixin("mix:title");
                node.addMixin("mix:created");
                node.setProperty("jcr:title", "title" + idx);
                if (i > 0 && i % batchSize == 0) {
                    System.out.println("inserting batch [" + (i - batchSize) + "," + i + "]");
                    session.save();
                }
            } catch (RepositoryException e) {
                throw new RuntimeException(e);
            }
        });
        if (session.hasPendingChanges()) {
            System.out.printf("inserting last batch ");
            session.save();
        }
        
        Query query = session.getWorkspace().getQueryManager().createQuery(
                "select node.[jcr:path] from [mix:title] as node where node.[jcr:title] LIKE 'title%' ORDER BY [jcr:created] ASC LIMIT 3 OFFSET 1000", Query.JCR_SQL2);
        long start = System.nanoTime();
        validateQuery().rowCount(3).printDetail().useIndex("titleIndex").hasNodesAtPaths("/node1001", "/node1002", "/node1003").validate(query, query.execute());
        System.out.println("Time to run query with limit and offset: " + TimeUnit.MILLISECONDS.convert(System.nanoTime() - start, TimeUnit.NANOSECONDS)/1000d + " seconds");
    }
}
