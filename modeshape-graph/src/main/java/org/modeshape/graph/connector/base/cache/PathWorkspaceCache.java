package org.modeshape.graph.connector.base.cache;

import org.modeshape.graph.connector.base.PathNode;

public interface PathWorkspaceCache<Path, N extends PathNode> extends WorkspaceCache<Path, N> {

    /**
     * Invalidates all nodes in the cache that have the given path or have a path that is an ancestor of the given path
     * 
     * @param path the root of the branch of nodes that are to be invalidated; may not be null
     */
    public void invalidate( Path path );

}
