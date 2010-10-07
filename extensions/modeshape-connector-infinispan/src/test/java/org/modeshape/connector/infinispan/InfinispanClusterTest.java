package org.modeshape.connector.infinispan;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.util.UUID;
import org.infinispan.Cache;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Graph;
import org.modeshape.graph.Node;
import org.modeshape.graph.Subgraph;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryConnectionFactory;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.observe.Observer;
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
    public void shouldDistributeGraphNodes() {
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

        Cache<UUID, InfinispanNode> cache1 = source1.cacheContainer().getCache("");
        Cache<UUID, InfinispanNode> cache2 = source2.cacheContainer().getCache("");
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

    private final RepositoryContext repositoryContextFor( final RepositorySource source ) {
        return new RepositoryContext() {

            @Override
            public Subgraph getConfiguration( int depth ) {
                return null;
            }

            @SuppressWarnings( "synthetic-access" )
            @Override
            public ExecutionContext getExecutionContext() {
                return context;
            }

            @Override
            public Observer getObserver() {
                return null;
            }

            @Override
            public RepositoryConnectionFactory getRepositoryConnectionFactory() {
                return new RepositoryConnectionFactory() {

                    @Override
                    public RepositoryConnection createConnection( String sourceName ) throws RepositorySourceException {
                        return source.getConnection();
                    }

                };
            }

        };
    }
}
