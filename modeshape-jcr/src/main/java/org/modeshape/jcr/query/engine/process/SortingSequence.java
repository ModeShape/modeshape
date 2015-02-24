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

import java.util.Iterator;
import org.mapdb.Serializer;
import org.modeshape.common.collection.SequentialIterator;
import org.modeshape.jcr.cache.CachedNodeSupplier;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.query.BufferManager.DistinctBuffer;
import org.modeshape.jcr.query.NodeSequence;
import org.modeshape.jcr.query.RowExtractors.ExtractFromRow;
import org.modeshape.jcr.query.engine.process.BufferedRows.BufferedRow;
import org.modeshape.jcr.query.model.NullOrder;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class SortingSequence extends BufferingSequence {

    private final DistinctBuffer<BufferedRow> rowsWithNullKey;
    private final NullOrder nullOrder;
    private Iterator<BufferedRow> bufferedRows;
    private int batchSize = 0;

    @SuppressWarnings( {"unchecked"} )
    public SortingSequence( String workspaceName,
                            NodeSequence delegate,
                            ExtractFromRow extractor,
                            BufferManager bufferMgr,
                            CachedNodeSupplier nodeCache,
                            boolean pack,
                            boolean useHeap,
                            boolean allowDuplicates,
                            NullOrder nullOrder ) {
        super(workspaceName, delegate, extractor, bufferMgr, nodeCache, pack, useHeap, allowDuplicates);
        this.nullOrder = nullOrder;
        // Create the buffer into which we'll place the rows with null keys ...
        Serializer<BufferedRow> rowSerializer = (Serializer<BufferedRow>)BufferedRows.serializer(nodeCache, width);
        rowsWithNullKey = bufferMgr.createDistinctBuffer(rowSerializer).keepSize(true).useHeap(useHeap).make();
    }

    @Override
    public long getRowCount() {
        if (bufferedRows == null) {
            bufferedRows = initialize();
        }
        return super.rowCount() + rowsWithNullKey.size();
    }

    @Override
    public Batch nextBatch() {
        if (bufferedRows == null) {
            bufferedRows = initialize();
        }
        remainingRowCount.addAndGet(-rowsLeftInBatch.get());
        if (remainingRowCount.get() == 0L) return null;
        return batchFrom(bufferedRows, batchSize);
    }

    /**
     * Initialize this node sequence only the first time that {@link #nextBatch()} is called.
     * 
     * @return the iterator over the buffered rows in this sequence; may be null if this sequence is empty
     */
    protected Iterator<BufferedRow> initialize() {
        // Load everthing into the buffer ...
        batchSize = loadAll(delegate, extractor, rowsWithNullKey);
        remainingRowCount.set(buffer.size() + rowsWithNullKey.size());
        // We always return the buffered rows in ascending order of the extracted key ...
        if (rowsWithNullKey.isEmpty()) {
            return buffer.ascending();
        }
        // Return the rows with NULL first ...
        assert nullOrder != null;
        switch (nullOrder) {
            case NULLS_FIRST:
                return SequentialIterator.create(rowsWithNullKey.iterator(), buffer.ascending());
            case NULLS_LAST:
                return SequentialIterator.create(buffer.ascending(), rowsWithNullKey.iterator());
        }
        assert false;
        return null;
    }

    @Override
    public void close() {
        try {
            super.close();
        } finally {
            rowsWithNullKey.close();
        }
    }

    @Override
    public String toString() {
        return "(sorting-sequence width=" + width() + " order=" + extractor + " " + delegate + ")";
    }
}
