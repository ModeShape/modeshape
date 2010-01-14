package org.modeshape.graph.connector.path;

import java.util.UUID;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.path.cache.PathCachePolicy;

/**
 * An extension of the {@link RepositorySource} class that provides a {@link CachePolicy cache policy} and a
 * {@link RepositoryContext repository context}.
 */
public interface PathRepositorySource extends RepositorySource {

    /**
     * Get whether this source allows updates.
     * 
     * @return true if this source allows updates by clients, or false if no updates are allowed
     * @see #setUpdatesAllowed(boolean)
     */
    boolean areUpdatesAllowed();

    /**
     * Set whether this source allows updates to data within workspaces
     * 
     * @param updatesAllowed true if this source allows updates to data within workspaces clients, or false if updates are not
     *        allowed.
     * @see #areUpdatesAllowed()
     */
    void setUpdatesAllowed( boolean updatesAllowed );

    /**
     * Returns the {@link PathCachePolicy cache policy} for the repository source
     * 
     * @return the {@link PathCachePolicy cache policy} for the repository source
     */
    PathCachePolicy getCachePolicy();

    /**
     * Get the UUID that is used for the root node of each workspace
     * 
     * @return the UUID that is used for the root node of each workspace
     */
    UUID getRootNodeUuid();

    /**
     * Get the name of the default workspace.
     * 
     * @return the name of the workspace that should be used by default; never null
     */
    String getDefaultWorkspaceName();

    /**
     * Returns the {@link RepositoryContext repository context} for the repository source
     * 
     * @return the {@link RepositoryContext repository context} for the repository source
     */
    RepositoryContext getRepositoryContext();
}
