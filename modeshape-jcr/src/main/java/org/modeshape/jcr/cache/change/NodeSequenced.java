package org.modeshape.jcr.cache.change;

import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Path;

/**
 * Change which is triggered after the sequencing process of a node is finished.
 * 
 * @author Horia Chiorean
 */
public class NodeSequenced extends AbstractSequencingChange {

    private final NodeKey outputNodeKey;
    private final Path outputNodePath;

    public NodeSequenced( NodeKey sequencedNodeKey,
                          Path sequencedNodePath,
                          NodeKey outputNodeKey,
                          Path outputNodePath,
                          String outputPath,
                          String userId,
                          String selectedPath,
                          String sequencerName ) {
        super(sequencedNodeKey, sequencedNodePath, outputPath, userId, selectedPath, sequencerName);
        assert outputNodeKey != null;
        assert outputNodePath != null;
        this.outputNodeKey = outputNodeKey;
        this.outputNodePath = outputNodePath;
    }

    /**
     * Get the key of the top-level node that was output by the sequencer.
     * 
     * @return the output node key; never null
     */
    public NodeKey getOutputNodeKey() {
        return outputNodeKey;
    }

    /**
     * Get the path of the top-level node that was output by the sequencer.
     * 
     * @return the output node path; never null
     */
    public Path getOutputNodePath() {
        return outputNodePath;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Sequenced new node: ").append(outputNodeKey).append(" at path: ").append(outputNodePath);
        sb.append(" from the node: ").append(getKey()).append(" at path: ").append(getPath());
        return sb.toString();
    }
}
