package org.modeshape.connector.infinispan;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.util.Set;
import java.util.UUID;
import org.apache.log4j.Logger;
import org.infinispan.Cache;
import org.infinispan.api.BasicCache;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Node;
import org.modeshape.graph.Workspace;
import org.modeshape.graph.connector.MockRepositoryContext;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.property.PathNotFoundException;

/*
 * Quick test that two clustered InfinispanSources can share data.
 * 
 * This test is currently ignored. See MODE-764 for details.
 */
public class InfinispanClusterTest {

    private static final String CONFIG_FILE = "./src/test/resources/infinispan_clustered_config.xml";

    private final ExecutionContext context = new ExecutionContext();

    @Test
    public void shouldWorkAndDoes() throws Exception {
        EmbeddedCacheManager manager1 = new DefaultCacheManager(CONFIG_FILE);
        Cache<String, String> cache1 = manager1.getCache("cache");

        EmbeddedCacheManager manager2 = new DefaultCacheManager(CONFIG_FILE);
        Cache<String, String> cache2 = manager2.getCache("cache");

        cache1.put("key1", "value1");
        cache1.put("key2", "value2");

        // Make sure the keys are accessible from cache2 ...
        String v1 = cache2.get("key1");
        String v2 = cache2.get("key2");
        assertThat(v1, is("value1"));
        assertThat(v2, is("value2"));

        Logger.getLogger(getClass()).info("*** Getting ready to create 'new-cache' programmatically");

        // now create a new cache, but have to do this on both managers! See ISPN-658 ...
        manager1.defineConfiguration("new-cache", manager1.getDefaultCacheConfiguration());
        Cache<String, String> cache1b = manager1.getCache("new-cache");
        manager2.defineConfiguration("new-cache", manager2.getDefaultCacheConfiguration());
        Cache<String, String> cache2b = manager2.getCache("new-cache");

        Logger.getLogger(getClass()).info("*** Getting ready to insert entry into 'new-cache'");
        cache1b.put("key1b", "value1b");

        Logger.getLogger(getClass()).info("*** Getting ready to find 'new-cache' from second manager");

        String v1b = cache2b.get("key1b");
        assertThat(v1b, is("value1b"));

        manager1.stop();
        manager2.stop();
    }

    @Ignore
    @Test
    public void shouldWorkButDoesNot() throws Exception {
        EmbeddedCacheManager manager1 = new DefaultCacheManager(CONFIG_FILE);
        Cache<String, String> cache1 = manager1.getCache("cache");

        EmbeddedCacheManager manager2 = new DefaultCacheManager(CONFIG_FILE);
        Cache<String, String> cache2 = manager2.getCache("cache");

        cache1.put("key1", "value1");
        cache1.put("key2", "value2");

        // Make sure the keys are accessible from cache2 ...
        String v1 = cache2.get("key1");
        String v2 = cache2.get("key2");
        assertThat(v1, is("value1"));
        assertThat(v2, is("value2"));

        Logger.getLogger(getClass()).info("*** Getting ready to create 'new-cache' programmatically");

        // now create a new cache ...
        manager1.defineConfiguration("new-cache", manager1.getDefaultCacheConfiguration());
        Cache<String, String> cache1b = manager1.getCache("new-cache");

        // The following does not work because of ISPN-658 ...
        // The previous two lines need to also be done for 'manager2' BEFORE using 'new-cache' on 'manager1'!!!
        Logger.getLogger(getClass()).info("*** Getting ready to insert entry into 'new-cache'");
        cache1b.put("key1b", "value1b");

        Logger.getLogger(getClass()).info("*** Getting ready to find 'new-cache' from second manager");
        manager2.defineConfiguration("new-cache", manager2.getDefaultCacheConfiguration());
        Cache<String, String> cache2b = manager2.getCache("new-cache");

        String v1b = cache2b.get("key1b");
        assertThat(v1b, is("value1b"));

        manager1.stop();
        manager2.stop();
    }

    @Test
    public void shouldDistributeGraphNodesUsingPredefinedWorkspaces() {
        InfinispanSource source1 = new InfinispanSource();
        source1.setName("source1");
        source1.setCacheConfigurationName(CONFIG_FILE);
        source1.initialize(repositoryContextFor(source1));

        InfinispanSource source2 = new InfinispanSource();
        source2.setName("source2");
        source2.setCacheConfigurationName(CONFIG_FILE);
        source2.initialize(repositoryContextFor(source2));

        Graph graph1 = Graph.create(source1, context);
        Graph graph2 = Graph.create(source2, context);

        assertThat(source1.getRootNodeUuid(), is(source2.getRootNodeUuid()));

        Node root1 = graph1.getNodeAt("/");
        Node root2 = graph2.getNodeAt("/");

        assertThat(root1.getLocation(), is(root2.getLocation()));

        graph1.create("/foo").and();

        BasicCache<UUID, InfinispanNode> cache1 = source1.cacheContainer().getCache("");
        BasicCache<UUID, InfinispanNode> cache2 = source2.cacheContainer().getCache("");
        assertThat(cache1.size(), is(2));
        assertThat(cache2.size(), is(2));
        // assertThat(cache2.containsKey(this))

        graph2.getNodeAt("/foo");

        graph2.delete("/foo");
        try {
            Thread.sleep(5000);
        } catch (Exception e) {
        }
        try {
            graph1.getNodeAt("/foo");
            fail("/foo was deleted by the other source and should no longer exist");
        } catch (PathNotFoundException expected) {

        }
    }

    @Ignore
    @Test
    public void shouldDistributeGraphNodesUsingDynamicallyCreatedWorkspaces() {
        InfinispanSource source1 = new InfinispanSource();
        source1.setName("source1");
        source1.setCacheConfigurationName(CONFIG_FILE);
        source1.initialize(repositoryContextFor(source1));

        InfinispanSource source2 = new InfinispanSource();
        source2.setName("source2");
        source2.setCacheConfigurationName(CONFIG_FILE);
        source2.initialize(repositoryContextFor(source2));

        Graph graph1 = Graph.create(source1, context);
        Graph graph2 = Graph.create(source2, context);

        assertThat(source1.getRootNodeUuid(), is(source2.getRootNodeUuid()));

        Node root1 = graph1.getNodeAt("/");
        Node root2 = graph2.getNodeAt("/");

        assertThat(root1.getLocation(), is(root2.getLocation()));

        graph1.create("/foo").and();

        BasicCache<UUID, InfinispanNode> cache1 = source1.cacheContainer().getCache("");
        BasicCache<UUID, InfinispanNode> cache2 = source2.cacheContainer().getCache("");
        assertThat(cache1.size(), is(2));
        assertThat(cache2.size(), is(2));
        // assertThat(cache2.containsKey(this))

        graph2.getNodeAt("/foo");

        graph2.delete("/foo");
        try {
            Thread.sleep(5000);
        } catch (Exception e) {
        }
        try {
            graph1.getNodeAt("/foo");
            fail("/foo was deleted by the other source and should no longer exist");
        } catch (PathNotFoundException expected) {

        }

        // The following does not work because of ISPN-658; see MODE-1183 for details

        // Create a workspace in one source, and have it be seen in the other ...
        Workspace nfws1 = graph1.createWorkspace().named("new-fangled-workspace");
        Set<String> workspaces1 = graph1.getWorkspaces();
        Set<String> workspaces2 = graph2.getWorkspaces();
        assertThat(workspaces1, is(workspaces2));
        Workspace nfws2 = graph2.useWorkspace(nfws1.getName());
        assertThat(nfws1.getRoot(), is(nfws2.getRoot()));
    }

    private final RepositoryContext repositoryContextFor( final RepositorySource source ) {
        return new MockRepositoryContext(context, source);
    }
}
