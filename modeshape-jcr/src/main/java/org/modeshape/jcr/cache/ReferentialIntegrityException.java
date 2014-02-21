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
package org.modeshape.jcr.cache;

import java.util.Collections;
import java.util.Set;
import org.modeshape.jcr.JcrI18n;

/**
 * An exception signalling that a set of nodes could not be deleted because there are other nodes that contain JCR REFERENCE
 * properties pointing to these nodes, preventing their deletion.
 */
public class ReferentialIntegrityException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final Set<NodeKey> referrers;
    private final Set<NodeKey> removedNodes;

    /**
     * @param removedNodes the nodes that are being removed; may not be null or empty
     * @param referrers the set of node keys that refer to nodes being removed; may not be null or empty
     */
    public ReferentialIntegrityException( Set<NodeKey> removedNodes,
                                          Set<NodeKey> referrers ) {
        this.referrers = Collections.unmodifiableSet(referrers);
        this.removedNodes = Collections.unmodifiableSet(removedNodes);
        assert this.removedNodes != null;
        assert this.referrers != null;
        assert !this.referrers.isEmpty();
        assert !this.removedNodes.isEmpty();
    }

    /**
     * Get the set of node keys that contain REFERENCE properties to nodes being deleted, and therefore prevent the removal.
     * 
     * @return the immutable set of node keys to the referring nodes; never null and never empty
     */
    public Set<NodeKey> getReferrers() {
        return referrers;
    }

    /**
     * Get the set of keys to the nodes that were removed.
     * 
     * @return the immutable set of keys to the removed nodes; never null and never empty
     */
    public Set<NodeKey> getRemovedNodes() {
        return removedNodes;
    }

    @Override
    public String getMessage() {
        return JcrI18n.referentialIntegrityException.text(removedNodes, referrers);
    }
}
