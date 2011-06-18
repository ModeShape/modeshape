package org.modeshape.graph.connector.base;

import org.modeshape.graph.connector.base.cache.NodeCachePolicy;

public interface NodeCachingRepositorySource<KeyType, NodeType extends Node> extends BaseRepositorySource {

    NodeCachePolicy<NodeType, NodeType> getNodeCachePolicy();

    void setNodeCachePolicy( NodeCachePolicy<NodeType, NodeType> defaultCachePolicy );

}
