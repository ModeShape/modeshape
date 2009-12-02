package org.jboss.dna.connector.store.jpa.model.simple;

import org.jboss.dna.connector.store.jpa.JpaSource;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.Graph;
import org.jboss.dna.graph.Subgraph;
import org.jboss.dna.graph.connector.RepositoryConnectionFactory;
import org.jboss.dna.graph.connector.RepositoryContext;
import org.jboss.dna.graph.connector.RepositorySource;
import org.jboss.dna.graph.connector.test.WritableConnectorTest;
import org.jboss.dna.graph.observe.Observer;

public class SimpleJpaConnectorWritableTest extends WritableConnectorTest {

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.test.AbstractConnectorTest#setUpSource()
     */
    @Override
    protected RepositorySource setUpSource() {
        // Set the connection properties using the environment defined in the POM files ...
        JpaSource source = new JpaSource();

        source.setModel(JpaSource.Models.SIMPLE.getName());
        source.setName("SimpleJpaSource");
        source.setDialect("org.hibernate.dialect.HSQLDialect");
        source.setDriverClassName("org.hsqldb.jdbcDriver");
        source.setUsername("sa");
        source.setPassword("");
        source.setUrl("jdbc:hsqldb:mem:test");
        source.setShowSql(false);
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

        return source;
    }

    @Override
    public void shouldCopyNodeWithChildren() {

    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.connector.test.AbstractConnectorTest#initializeContent(org.jboss.dna.graph.Graph)
     */
    @Override
    protected void initializeContent( Graph graph ) {
    }

}
