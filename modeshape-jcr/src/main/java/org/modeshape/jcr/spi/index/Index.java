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

import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.spi.index.provider.Filter;
import org.modeshape.jcr.spi.index.provider.IndexProvider;

/**
 * An index used by the query system to quickly provide the set of {@link NodeKey}s that satisfy a particular portion of a query.
 * <p>
 * Each time ModeShape uses this index, it calls the {@link #filter(IndexConstraints)} method with a set of
 * {@link IndexConstraints} to obtain an {@link org.modeshape.jcr.spi.index.provider.Filter.Results} instance that ModeShape will
 * then use to {@link org.modeshape.jcr.spi.index.provider.Filter.Results#getNextBatch(ResultWriter, int) access} the batches of
 * node keys that satisfy the constraints. Note that once an {@link org.modeshape.jcr.spi.index.provider.Filter.Results} is
 * obtained, it may be called zero or more times but will always be closed.
 * </p>
 * 
 * @see IndexProvider#getIndex(String,String)
 * @author Randall Hauch (rhauch@redhat.com)
 */
public interface Index extends Filter {

    /**
     * Get the name of the {@link IndexProvider} that owns this index.
     * 
     * @return the provider's {@link IndexProvider#getName() name}; never null
     */
    String getProviderName();

    /**
     * Get the name of this index.
     * 
     * @return the index name; never null
     */
    String getName();

    /**
     * Return whether this index can use full-text search constraints. Such indexes may be used in some queries even if they are
     * not the lowest-cost option.
     * 
     * @return true if this index can use full-text search constraints, or false otherwise.
     */
    boolean supportsFullTextConstraints();

    /**
     * Determine if this index is enabled for use.
     *
     * @return true if enabled, or false otherwise
     */
    boolean isEnabled();
}
