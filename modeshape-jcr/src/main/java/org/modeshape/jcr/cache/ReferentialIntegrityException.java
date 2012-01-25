/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.modeshape.jcr.cache;

import java.util.Collections;
import java.util.Set;

/**
 * An exception signalling that a set of nodes could not be deleted because there are other nodes that contain JCR REFERENCE
 * properties pointing to these ndoes, preventing their deletion.
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
}
