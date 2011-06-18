package org.modeshape.graph.connector.base.cache;

import java.io.Serializable;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.base.Node;

/**
 * Cache policy implementation for use with {@link NodeCache workspace caches}.
 * 
 * @param <KeyType> the type of the unique identifier for the nodes that are to be cached
 * @param <NodeType> the type of nodes that are to be cached
 */
public interface NodeCachePolicy<KeyType, NodeType extends Node> extends CachePolicy, Serializable {

    /**
     * Indicates whether the node should be cached .
     * 
     * @param node the node that may or may not be cached; may not be null
     * @return true if the node should be cached and false if it should not be cached
     */
    boolean shouldCache( NodeType node );

    /**
     * Return a new cache instance that {@link NodeCache#assignPolicy(NodeCachePolicy) uses this policy}.
     * 
     * @return the cache class
     * @param <CacheType> the type of cache being returned
     */
    <CacheType extends NodeCache<KeyType, NodeType>> CacheType newCache();

}
