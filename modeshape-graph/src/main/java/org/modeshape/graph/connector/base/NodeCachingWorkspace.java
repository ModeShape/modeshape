package org.modeshape.graph.connector.base;

import org.modeshape.graph.connector.base.cache.NodeCache;
import org.modeshape.graph.connector.base.cache.NodeCachePolicyChangedListener;

public interface NodeCachingWorkspace<KeyType, NodeType extends Node> extends NodeCachePolicyChangedListener<KeyType, NodeType> {
    
    NodeCache<KeyType, NodeType> getCache();
}
