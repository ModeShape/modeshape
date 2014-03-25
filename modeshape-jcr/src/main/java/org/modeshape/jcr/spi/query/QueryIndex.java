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
package org.modeshape.jcr.spi.query;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import javax.jcr.query.qom.Constraint;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.jcr.cache.NodeKey;

/**
 * A provider-specific index used by the query system to quickly provide the set of {@link NodeKey}s that satisfy a particular
 * portion of a query.
 * <p>
 * Each time ModeShape uses this index, it calls the {@link #filter(Filter)} method to obtain an {@link Operation} instance that
 * ModeShape will then use to {@link Operation#getNextBatch(ResultWriter, int) access} the batches of node keys that satisfy the
 * given {@link Filter}. Note that once an {@link Operation} is obtained, it may not be called if the query is cancelled before
 * this index is needed.
 * </p>
 * 
 * @see QueryIndexProvider#getQueryIndex(String)
 * @author Randall Hauch (rhauch@redhat.com)
 */
public interface QueryIndex {

    /**
     * Get the name of the {@link QueryIndexProvider} that owns this index.
     * 
     * @return the provider's {@link QueryIndexProvider#getName() name}; never null
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
     * Return a {@link Operation} instance that ModeShape can when it actually wants the results. Note that this method should
     * return quickly, since ideally no work is really done other than instantiating and populating the {@link Operation}
     * instance. Instead, all work should be performed when the resulting Operation's
     * {@link Operation#getNextBatch(ResultWriter, int)} method is called.
     * 
     * @param filter the filter to be applied by this index; never null
     * @return the batched node keys; never null
     */
    Operation filter( Filter filter );

    /**
     * A potentially executable stateful operation that returns the set of nodes that satisfies the constraints supplied when the
     * operation is {@link QueryIndex#filter(Filter) created}.
     * <p>
     * {@link QueryIndex} implementations should create their own implementations of this interface, and ModeShape will then
     * periodically call {@link #getNextBatch(ResultWriter, int)} as (additional) results are needed to answer the query.
     * Generally, each {@link Operation} instance will have enough state to execute the filtering steps.
     * </p>
     * <p>
     * ModeShape may call this method zero or more times, based upon whether results are actually needed and in specific batch
     * sizes. And every time this method is called, implementations should read the next batch of nodes that satisfies the
     * criteria, where the number of nodes in the batch should be roughly equal to <code>batchSize</code>. If the index
     * implementation is interacting with a remote system, then each method invocation may correspond to a single remote request.
     * </p>
     * 
     * @author Randall Hauch (rhauch@redhat.com)
     */
    interface Operation extends AutoCloseable {
        /**
         * Obtain the next batch of results for the query.
         * 
         * @param writer the writer that this method should use to register results in the batch; never null
         * @param batchSize the ideal number of node keys that are to be included in this batch; always positive
         * @return true if there are additional results after this batch is completed, or false if this batch contains the final
         *         results of the query
         */
        boolean getNextBatch( ResultWriter writer,
                              int batchSize );

        /**
         * Close any and all resources for the operation. This will always be called by ModeShape when the operation is no longer
         * needed, even if {@link #getNextBatch(ResultWriter, int)} was never called.
         */
        @Override
        void close();
    }

    /**
     * A writer passed by ModeShape to a {@link Operation} instance when the query engine needs additional results for the query.
     * <p>
     * Instances of this type are created by ModeShape and passed into the {@link Operation#getNextBatch(ResultWriter, int)}
     * method. Thus, providers do not need to implement this interface (except maybe for testing purposes).
     * </p>
     * 
     * @author Randall Hauch (rhauch@redhat.com)
     */
    @NotThreadSafe
    interface ResultWriter {
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

    /**
     * The filter containing a set of constraints.
     * <p>
     * Instances of this type are created by ModeShape and passed into the {@link QueryIndex#filter(Filter)} method. Thus,
     * providers do not need to implement this interface (except maybe for testing purposes).
     * </p>
     * 
     * @see QueryIndex#filter(Filter)
     * @author Randall Hauch (rhauch@redhat.com)
     */
    @Immutable
    interface Filter {
        /**
         * Return whether this filter contains constraints. This is identical to calling "<code>!getConstraints().isEmpty()</code>
         * ".
         * 
         * @return true if there is at least one constraint, or false if there are none
         */
        boolean hasConstraints();

        /**
         * Get the constraints for this filter.
         * 
         * @return the constraints; never null but maybe empty
         */
        Collection<Constraint> getConstraints();

        /**
         * Get the parameters for this filter operation, as determined during
         * {@link QueryIndexPlanner#applyIndexes(org.modeshape.jcr.query.QueryContext, org.modeshape.jcr.query.model.SelectorName, java.util.List, org.modeshape.jcr.spi.query.IndexCollector)}
         * 
         * @return the parameters; never null but may be empty
         */
        Map<String, Object> getParameters();
    }
}
