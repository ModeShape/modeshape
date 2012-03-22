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

    private final NodeKey originalNodeKey;
    private final Path originalNodePath;

    public NodeSequenced( NodeKey sequencedNodeKey,
                          Path sequencedNodePath,
                          NodeKey originalNodeKey,
                          Path originalNodePath ) {
        super(sequencedNodeKey, sequencedNodePath);

        CheckArg.isNotNull(originalNodeKey, " original node key");
        CheckArg.isNotNull(originalNodePath, " original node path");
        this.originalNodeKey = originalNodeKey;
        this.originalNodePath = originalNodePath;
    }

    public NodeKey getOriginalNodeKey() {
        return originalNodeKey;
    }

    public Path getOriginalNodePath() {
        return originalNodePath;
    }

    public NodeKey getSequencedNodeKey() {
        return super.getKey();
    }

    public Path getSequencedNodePath() {
        return super.getPath();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Sequenced new node: ").append(getSequencedNodeKey()).append(" at path: ").append(getSequencedNodePath());
        sb.append(" from the node: ").append(getOriginalNodeKey()).append(" at path: ").append(originalNodePath);
        return sb.toString();
    }
}
