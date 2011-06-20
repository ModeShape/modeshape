package org.modeshape.graph.connector.base;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.naming.BinaryRefAddr;
import javax.naming.Reference;
import org.modeshape.common.i18n.I18n;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.base.cache.NoCachePolicy;
import org.modeshape.graph.connector.base.cache.NodeCachePolicy;
import org.modeshape.graph.connector.base.cache.NodeCachePolicyChangedEvent;
import org.modeshape.graph.connector.base.cache.NodeCachePolicyChangedListener;

@SuppressWarnings( "serial" )
public abstract class AbstractNodeCachingRepositorySource<KeyType, NodeType extends Node> extends AbstractRepositorySource {

    protected static final String NODE_CACHE_POLICY = "nodeCachePolicy";

    /**
     * The initial cache policy. The default is to not cache.
     */
    public final NodeCachePolicy<KeyType, NodeType> DEFAULT_NODE_CACHE_POLICY = new NoCachePolicy<KeyType, NodeType>();

    private Collection<NodeCachePolicyChangedListener<KeyType, NodeType>> nodeCachePolicyChangedListeners = new CopyOnWriteArraySet<NodeCachePolicyChangedListener<KeyType, NodeType>>();
    private volatile NodeCachePolicy<KeyType, NodeType> nodeCachePolicy = DEFAULT_NODE_CACHE_POLICY;

    /**
     * Get the node cache policy for this source, or null if the global node cache policy should be used
     * 
     * @return the node cache policy; never null
     */
    public NodeCachePolicy<KeyType, NodeType> getNodeCachePolicy() {
        return nodeCachePolicy;
    }

    /**
     * @param nodeCachePolicy Sets defaultCachePolicy to the specified value.
     */
    public synchronized void setNodeCachePolicy( NodeCachePolicy<KeyType, NodeType> nodeCachePolicy ) {
        if (nodeCachePolicy == null) {
            nodeCachePolicy = DEFAULT_NODE_CACHE_POLICY;
        }

        if (this.nodeCachePolicy == nodeCachePolicy && this.nodeCachePolicy.equals(nodeCachePolicy)) return; // unchanged

        NodeCachePolicy<KeyType, NodeType> oldPolicy = this.nodeCachePolicy;
        this.nodeCachePolicy = nodeCachePolicy;
        cachePolicyChanged(oldPolicy, nodeCachePolicy);
    }

    private void cachePolicyChanged( NodeCachePolicy<KeyType, NodeType> oldPolicy,
                                     NodeCachePolicy<KeyType, NodeType> newPolicy ) {
        NodeCachePolicyChangedEvent<KeyType, NodeType> event = new NodeCachePolicyChangedEvent<KeyType, NodeType>(oldPolicy,
                                                                                                                  newPolicy);

        for (NodeCachePolicyChangedListener<KeyType, NodeType> listener : nodeCachePolicyChangedListeners) {
            listener.cachePolicyChanged(event);
        }
    }

    public void addNodeCachePolicyChangedListener( NodeCachePolicyChangedListener<KeyType, NodeType> listener ) {
        this.nodeCachePolicyChangedListeners.add(listener);
    }

    public void removeNodeCachePolicyChangedListener( NodeCachePolicyChangedListener<KeyType, NodeType> listener ) {
        this.nodeCachePolicyChangedListeners.add(listener);
    }

    protected void addNodeCachePolicyReference( Reference ref ) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        NodeCachePolicy<KeyType, NodeType> policy = getNodeCachePolicy();
        try {
            ObjectOutputStream oos = new ObjectOutputStream(baos);
            oos.writeObject(policy);
            ref.add(new BinaryRefAddr(NODE_CACHE_POLICY, baos.toByteArray()));
        } catch (IOException e) {
            I18n msg = GraphI18n.errorSerializingNodeCachePolicyInSource;
            throw new RepositorySourceException(getName(), msg.text(policy.getClass().getName(), getName()), e);
        }
    }

}
