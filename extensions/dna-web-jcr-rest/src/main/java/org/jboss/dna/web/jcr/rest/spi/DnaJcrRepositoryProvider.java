package org.jboss.dna.web.jcr.rest.spi;

import java.util.HashSet;
import java.util.Set;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.jcr.JcrConfiguration;
import org.jboss.dna.jcr.JcrEngine;

public class DnaJcrRepositoryProvider implements RepositoryProvider {

    private JcrEngine jcrEngine;

    public DnaJcrRepositoryProvider() {
        jcrEngine = new JcrConfiguration().withConfigurationRepository()
            .usingClass(InMemoryRepositorySource.class.getName())
            .loadedFromClasspath()
            .describedAs("Configuration Repository")
            .with("name").setTo("configuration")
            .with("retryLimit")
            .setTo(5)
            .and()
            .addRepository("Source2")
            .usingClass(InMemoryRepositorySource.class.getName())
            .loadedFromClasspath()
            .describedAs("description")
            .with("name").setTo("JCR Repository")
            .and()
            .build();
        jcrEngine.start();
        
    }
    
    public Set<String> getJcrRepositoryNames() {
        return new HashSet<String>(jcrEngine.getJcrRepositoryNames());
    }

    public Repository getRepository( String repositoryName ) throws RepositoryException {
        return jcrEngine.getRepository(repositoryName);
    }

    public void shutdown() {
        jcrEngine.shutdown();
    }
}
