package org.modeshape.graph.connector.base.cache;

import org.modeshape.common.annotation.Immutable;
import org.modeshape.graph.connector.base.Node;

/**
 * Event indicating that a repository's node cache policy has been changed.
 * 
 * @param <KeyType> the key for the cache entries, normally the natural unique identifier for the node
 * @param <NodeType> the node type that is being cached
 */
@Immutable
public class NodeCachePolicyChangedEvent<KeyType, NodeType extends Node> {

    private final NodeCachePolicy<KeyType, NodeType> oldPolicy;
    private final NodeCachePolicy<KeyType, NodeType> newPolicy;

    public NodeCachePolicyChangedEvent( NodeCachePolicy<KeyType, NodeType> oldPolicy,
                                        NodeCachePolicy<KeyType, NodeType> newPolicy ) {
        super();
        this.oldPolicy = oldPolicy;
        this.newPolicy = newPolicy;
    }

    public NodeCachePolicy<KeyType, NodeType> getOldPolicy() {
        return oldPolicy;
    }

    public NodeCachePolicy<KeyType, NodeType> getNewPolicy() {
        return newPolicy;
    }
}
