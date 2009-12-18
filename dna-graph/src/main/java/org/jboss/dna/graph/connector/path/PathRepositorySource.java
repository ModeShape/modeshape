package org.jboss.dna.graph.connector.path;

import org.jboss.dna.graph.cache.CachePolicy;
import org.jboss.dna.graph.connector.RepositoryContext;
import org.jboss.dna.graph.connector.RepositorySource;

/**
 * An extension of the {@link RepositorySource} class that provides a {@link CachePolicy cache policy} and a
 * {@link RepositoryContext repository context}.
 */
public interface PathRepositorySource extends RepositorySource {

    /**
     * Returns the {@link CachePolicy cache policy} for the repository source
     * 
     * @return the {@link CachePolicy cache policy} for the repository source
     */
    CachePolicy getDefaultCachePolicy();

    /**
     * Returns the {@link RepositoryContext repository context} for the repository source
     * 
     * @return the {@link RepositoryContext repository context} for the repository source
     */
    RepositoryContext getRepositoryContext();

}
