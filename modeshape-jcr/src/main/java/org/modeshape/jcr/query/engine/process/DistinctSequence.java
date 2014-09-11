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
package org.modeshape.jcr.query.engine.process;

import org.mapdb.Serializer;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.query.BufferManager.DistinctBuffer;
import org.modeshape.jcr.query.NodeSequence;
import org.modeshape.jcr.query.RowExtractors;
import org.modeshape.jcr.query.RowExtractors.ExtractFromRow;
import org.modeshape.jcr.query.Tuples;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.query.model.TypeSystem.TypeFactory;

/**
 * A {@link NodeSequence} that wraps another {@link NodeSequence} and returns only those rows that are seen for the first time.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class DistinctSequence extends DelegatingSequence {

    protected static final Logger LOGGER = Logger.getLogger(DistinctSequence.class);

    protected final ExtractFromRow keyExtractor;
    private final RowFilter filter;
    protected final boolean trace = LOGGER.isTraceEnabled();

    /**
     * Create a new distinct sequence given the type system and buffer manager.
     * 
     * @param delegate the node sequence that is to be wrapped; may not be null
     * @param types the system of type factories; may not be null
     * @param bufferMgr the buffer manager that should be used to create a temporary buffer into which records of the rows can be
     *        kept; may not be null
     * @param useHeap true if the temporary buffer should use the heap, or false if it should store data off-heap
     */
    @SuppressWarnings( "unchecked" )
    public DistinctSequence( NodeSequence delegate,
                             TypeSystem types,
                             BufferManager bufferMgr,
                             boolean useHeap ) {
        super(delegate);
        this.keyExtractor = RowExtractors.extractUniqueKey(delegate.width(), types);
        TypeFactory<?> keyType = types.getNodeKeyFactory();
        Serializer<?> keySerializer = bufferMgr.serializerFor(keyType);
        Serializer<?> serializer = Tuples.serializer(keySerializer, delegate.width());
        final DistinctBuffer<Object> rowsSeen = (DistinctBuffer<Object>)bufferMgr.createDistinctBuffer(serializer).keepSize(true)
                                                                                 .useHeap(useHeap).make();
        this.filter = new RowFilter() {
            @Override
            public boolean isCurrentRowValid( Batch batch ) {
                Object key = keyExtractor.getValueInRow(batch);
                if (!rowsSeen.addIfAbsent(key)) {
                    if (trace) LOGGER.trace("Distinct found existing key: {0}", key);
                    return false;
                }
                if (trace) LOGGER.trace("Distinct found new key: {0}", key);
                return true;
            }
        };
    }

    @Override
    public Batch nextBatch() {
        Batch batch = super.nextBatch();
        return NodeSequence.batchFilteredWith(batch, filter);
    }

    @Override
    public String toString() {
        return "(distinct on " + filter + " " + delegate + ")";
    }

}
