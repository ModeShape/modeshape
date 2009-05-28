package org.jboss.dna.web.jcr.rest;

import java.util.Collection;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.servlet.ServletContext;
import org.jboss.dna.web.jcr.rest.spi.RepositoryProvider;

public class RepositoryFactory {

    public static final String PROVIDER_KEY = "org.jboss.dna.web.jcr.rest.REPOSITORY_PROVIDER";

    private static RepositoryProvider provider;

    private RepositoryFactory() {

    }

    static void initialize( ServletContext context ) {
        String className = context.getInitParameter(PROVIDER_KEY);

        try {
            Class<? extends RepositoryProvider> providerClass = Class.forName(className).asSubclass(RepositoryProvider.class);
            provider = providerClass.newInstance();
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    public static Repository getRepository( String repositoryName ) throws RepositoryException {
        return provider.getRepository(repositoryName);
    }

    public static Collection<String> getJcrRepositoryNames() {
        return provider.getJcrRepositoryNames();
    }

    static void shutdown() {
        provider.shutdown();
    }
}
