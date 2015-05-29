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
 * Change which is triggered if the sequencing of a node fails
 * 
 * @author Horia Chiorean
 */
public class NodeSequencingFailure extends AbstractSequencingChange {

    private static final long serialVersionUID = 1L;

    /**
     * The cause of the sequencing failure
     */
    private Throwable cause;

    public NodeSequencingFailure( NodeKey sequencedNodeKey,
                                  Path sequencedNodePath,
                                  Name sequencedNodePrimaryType,
                                  Set<Name> sequencedNodeMixinTypes,
                                  String outputPath,
                                  String userId,
                                  String selectedPath,
                                  String sequencerName,
                                  Throwable cause ) {
        super(sequencedNodeKey, sequencedNodePath, sequencedNodePrimaryType, sequencedNodeMixinTypes, outputPath,
              userId, selectedPath, sequencerName);
        assert cause != null;
        this.cause = cause;
    }

    /**
     * Get the cause of the failure.
     * 
     * @return the exception; never null
     */
    public Throwable getCause() {
        return cause;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        sb.append("Failure while sequencing the node at:").append(getPath()).append(" with key:").append(getKey());
        sb.append(". Cause: ").append(cause.getMessage());
        return sb.toString();
    }
}
