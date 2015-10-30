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

import static org.modeshape.jcr.ValidateQuery.validateQuery;
import javax.jcr.Node;
import javax.jcr.Session;
import javax.jcr.query.Query;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.ClusteringHelper;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.TestingUtil;

/**
 * Unit tests for generic operations involving a repository which has a {@link LuceneIndexProvider} configured. Tests which are 
 * not index/search specific should go here. All others should go in {@link LuceneIndexProviderTest}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class LuceneRepositoryTest {

    @BeforeClass
    public static void beforeClass() throws Exception {
        ClusteringHelper.bindJGroupsToLocalAddress();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        ClusteringHelper.removeJGroupsBindings();
    }
    
    @Test
    public void shouldAllowAdvancedLuceneConfiguration() throws Exception {
        FileUtil.delete("target/persistent_repository");
        
        JcrRepository repository = TestingUtil.startRepositoryWithConfig(
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
        FileUtil.delete("target/clustered");
        
        JcrRepository repository1 = null;
        JcrRepository repository2 = null;
        try {
            // Start the first process completely ...
            repository1 = TestingUtil.startRepositoryWithConfig("config/cluster/clustered-repo-with-incremental-indexes-config-1.json");
            Thread.sleep(300);

            // Start the second process completely ...
            repository2 = TestingUtil.startRepositoryWithConfig("config/cluster/clustered-repo-with-incremental-indexes-config-2.json");
            Thread.sleep(300);

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

            // start the 2nd repo back up - at the end of this the journals should be up-to-date and ISPN should've done the state
            // transfer
            repository2 = TestingUtil.startRepositoryWithConfig(
                    "config/cluster/clustered-repo-with-incremental-indexes-config-2.json");
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
            TestingUtil.killRepository(repository1);

            // add a new node in the second repo
            node = session2.getRootNode().addNode("repo2_node1");
            node.addMixin("mix:title");
            node.setProperty("jcr:title", "title3");
            session2.save();

            // start the 1st repo back up - at the end of this the journals should be up-to-date and ISPN should've done the state
            // transfer
            repository1 = TestingUtil.startRepositoryWithConfig(
                    "config/cluster/clustered-repo-with-incremental-indexes-config-1.json");
            Thread.sleep(300);

            session1 = repository1.login();
            query = session1.getWorkspace().getQueryManager().createQuery(
                    "select node.[jcr:path] from [mix:title] as node where node.[jcr:title] = 'title3'",
                    Query.JCR_SQL2);
            validateQuery().hasNodesAtPaths("/repo2_node1").useIndex("titleIndex").validate(query, query.execute());

            // shut the second repo down
            TestingUtil.killRepository(repository2);

            // remove a node from the first repo and change a value for the other node
            session1.getNode("/repo1_node2").remove();
            session1.getNode("/repo1_node1").setProperty("jcr:title", "title1_edited");
            session1.save();

            // bring the 2nd repo back up
            // start the 2nd repo back up - at the end of this the journals should be up-to-date and ISPN should've done the state
            // transfer
            repository2 = TestingUtil.startRepositoryWithConfig(
                    "config/cluster/clustered-repo-with-incremental-indexes-config-2.json");
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
}
