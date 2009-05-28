package org.jboss.dna.web.jcr.rest.spi;

import java.util.Set;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;

/**
 * Interface for any class that provides access to one or more local JCR repositories. Repository providers must provide a public,
 * no-argument constructor.
 */
public interface RepositoryProvider {

    /**
     * Returns a reference to the named repository
     * 
     * @param repositoryName the name of the repository to retrieve; may be null
     * @return the repository with the given name; may not be null
     * @throws RepositoryException if no repository with the given name exists or there is an error obtaining a reference to the
     *         named repository
     */
    Repository getRepository( String repositoryName ) throws RepositoryException;

    /**
     * Returns the available repository names
     * 
     * @return the available repository names; may not be null
     */
    Set<String> getJcrRepositoryNames();

    /**
     * Signals the repository provider that it should complete any pending transactions, shutdown, and release
     * any external resource held.
     */
    void shutdown();
}
