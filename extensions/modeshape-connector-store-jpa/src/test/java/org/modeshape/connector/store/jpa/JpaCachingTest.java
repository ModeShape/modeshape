package org.modeshape.connector.store.jpa;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.graph.Graph;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.test.AbstractConnectorTest;

public class JpaCachingTest extends AbstractConnectorTest {

    @Override
    protected RepositorySource setUpSource() throws Exception {
        // Set the connection properties using the environment defined in the POM files ...
        JpaSource source = TestEnvironment.configureJpaSource("Test Repository", this);
        source.setShowSql(true);
        source.setJpaPersistenceUnitName("modeshape-connector-jpa-ehcache");

        // source.setJpaCacheProviderClassName("org.hibernate.cache.HashtableCacheProvider");

        return source;
    }

    @Override
    protected void initializeContent( Graph graph ) throws Exception {
    }

    @Ignore
    @Test
    public void shouldAddAndReadNodeRepeatedly() throws Exception {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        System.setOut(new PrintStream(baos));

        graph.create("/cacheTest").with("jcr:primaryType").and();

        for (int i = 0; i < 3; i++) {
            graph.getNodeAt("/cacheTest");
        }

        System.err.println(baos.toByteArray().length);
    }
    
}
