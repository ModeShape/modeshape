/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.cache.change;

import java.util.Set;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;

/**
 * Change which is triggered after the sequencing process of a node is finished.
 * 
 * @author Horia Chiorean
 */
public class NodeSequenced extends AbstractSequencingChange {

    private static final long serialVersionUID = 1L;

    private final NodeKey outputNodeKey;
    private final Path outputNodePath;

    public NodeSequenced( NodeKey sequencedNodeKey,
                          Path sequencedNodePath,
                          Name sequencedNodePrimaryType,
                          Set<Name> sequencedNodeMixinTypes,
                          NodeKey outputNodeKey,
                          Path outputNodePath,
                          String outputPath,
                          String userId,
                          String selectedPath,
                          String sequencerName ) {
        super(sequencedNodeKey, sequencedNodePath, sequencedNodePrimaryType, sequencedNodeMixinTypes, outputPath,
              userId, selectedPath, sequencerName);
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
