package org.modeshape.jcr.cache.change;

import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Path;

/**
 * Change which is triggered after the sequencing process of a node is finished.
 *
 * @author Horia Chiorean
 */
public class NodeSequenced extends AbstractNodeChange {

    private final NodeKey sequencedNodeKey;
    private final Path sequencedNodePath;

    public NodeSequenced( NodeKey sequencedNodeKey,
                          Path sequencedNodePath,
                          NodeKey outputNodeKey,
                          Path outputNodePath ) {
        super(outputNodeKey, outputNodePath);

        CheckArg.isNotNull(outputNodeKey, " original node key");
        CheckArg.isNotNull(outputNodePath, " original node path");
        this.sequencedNodeKey = sequencedNodeKey;
        this.sequencedNodePath = sequencedNodePath;
    }

    public NodeKey getSequencedNodeKey() {
        return sequencedNodeKey;
    }

    public Path getSequencedNodePath() {
        return sequencedNodePath;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Sequenced new node: ").append(getSequencedNodeKey()).append(" at path: ").append(getSequencedNodePath());
        sb.append(" from the node: ").append(getSequencedNodeKey()).append(" at path: ").append(sequencedNodePath);
        return sb.toString();
    }
}
