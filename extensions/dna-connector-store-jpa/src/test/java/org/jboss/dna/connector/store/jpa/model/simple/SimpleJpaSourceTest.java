package org.jboss.dna.connector.store.jpa.model.simple;

import java.util.concurrent.TimeUnit;
import org.jboss.dna.connector.store.jpa.JpaSource;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Subgraph;
import org.jboss.dna.graph.connector.RepositoryConnection;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositoryContext;
import org.jboss.dna.graph.observe.Observer;
import org.junit.Before;
import org.junit.Test;

public class SimpleJpaSourceTest {

    private JpaSource source;

    @Before
    public void beforeEach() {
        // Set the connection properties using the environment defined in the POM files ...
        source = new JpaSource();

        source.setModel(JpaSource.Models.SIMPLE.getName());
        source.setName("SimpleJpaSource");
        source.setDialect("org.hibernate.dialect.HSQLDialect");
        source.setDriverClassName("org.hsqldb.jdbcDriver");
        source.setUsername("sa");
        source.setPassword("");
        source.setUrl("jdbc:hsqldb:.");
        source.setShowSql(true);
        source.setAutoGenerateSchema("create");

        source.initialize(new RepositoryContext() {

            private final ExecutionContext context = new ExecutionContext();

            public Subgraph getConfiguration( int depth ) {
                return null;
            }

            public ExecutionContext getExecutionContext() {
                return context;
            }

            public Observer getObserver() {
                return null;
            }

            public RepositoryConnectionFactory getRepositoryConnectionFactory() {
                return null;
            }

        });
    }

    @Test
    public void shouldCreateLiveConnection() throws InterruptedException {
        RepositoryConnection connection = source.getConnection();
        connection.ping(1, TimeUnit.SECONDS);
        connection.close();
    }
}
