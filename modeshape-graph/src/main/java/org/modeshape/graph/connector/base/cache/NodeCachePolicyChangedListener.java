package org.modeshape.graph.connector.base.cache;

import org.modeshape.graph.connector.base.Node;

/**
 * Listener for {@link NodeCachePolicyChangedEvent NodeCachePolicyChangedEvents}.
 * 
 * @param <KeyType> the key for the cache entries, normally the natural unique identifier for the node
 * @param <NodeType> the node type that is being cached
 */
public interface NodeCachePolicyChangedListener<KeyType, NodeType extends Node> {

    /**
     * Handler for {@link NodeCachePolicyChangedEvent NodeCachePolicyChangedEvents}. This handler is invoked after the policy has
     * been changed. This method has no mechanism to veto or reject the policy change.
     * 
     * @param event the event containing the change information; may not be null
     */
    void cachePolicyChanged( NodeCachePolicyChangedEvent<KeyType, NodeType> event );
}
