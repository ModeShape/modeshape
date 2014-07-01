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

package org.modeshape.jcr.spi.index;

import java.util.Iterator;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.jcr.cache.NodeKey;

/**
 * A writer passed by ModeShape to a {@link org.modeshape.jcr.spi.index.provider.Filter.Results} instance when the query engine
 * needs additional results for the query.
 * <p>
 * Instances of this type are created by ModeShape and passed into the
 * {@link org.modeshape.jcr.spi.index.provider.Filter.Results#getNextBatch(ResultWriter, int)} method. Thus, providers do not need
 * to implement this interface (except maybe for testing purposes).
 * </p>
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
@NotThreadSafe
public interface ResultWriter {
    /**
     * Add to the current batch a single node key with a score.
     * 
     * @param nodeKey the node key; may not be null
     * @param score the score; must be positive
     */
    void add( NodeKey nodeKey,
              float score );

    /**
     * Add to the current batch a series of node keys with the same score for each node key.
     * 
     * @param nodeKeys the node keys; may not be null
     * @param score the score; must be positive
     */
    void add( Iterable<NodeKey> nodeKeys,
              float score );

    /**
     * Add to the current batch a series of node keys with the same score for each node key.
     * 
     * @param nodeKeys the node keys; may not be null
     * @param score the score; must be positive
     */
    void add( Iterator<NodeKey> nodeKeys,
              float score );
}
