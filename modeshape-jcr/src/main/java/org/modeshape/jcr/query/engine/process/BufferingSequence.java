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
import java.util.concurrent.atomic.AtomicLong;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.Serializer;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.CachedNodeSupplier;
import org.modeshape.jcr.index.local.MapDB.KeySerializerWithComparator;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.query.BufferManager.DistinctBuffer;
import org.modeshape.jcr.query.BufferManager.SortingBuffer;
import org.modeshape.jcr.query.NodeSequence;
import org.modeshape.jcr.query.RowExtractors.ExtractFromRow;
import org.modeshape.jcr.query.engine.process.BufferedRows.BufferedRow;
import org.modeshape.jcr.query.engine.process.BufferedRows.BufferedRowFactory;
import org.modeshape.jcr.query.model.TypeSystem.TypeFactory;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public abstract class BufferingSequence extends DelegatingSequence {

    protected static final Logger logger = Logger.getLogger(BufferingSequence.class);
    protected static final boolean trace = logger.isTraceEnabled();

    protected final SortingBuffer<Object, BufferedRow> buffer;
    protected final BufferedRowFactory<? extends BufferedRow> rowFactory;
    protected final ExtractFromRow extractor;
    protected final CachedNodeSupplier cache;
    protected final int width;
    protected final String workspaceName;
    protected final AtomicLong remainingRowCount = new AtomicLong();
    protected final AtomicLong rowsLeftInBatch = new AtomicLong();

    @SuppressWarnings( "unchecked" )
    protected BufferingSequence( String workspaceName,
                                 NodeSequence delegate,
                                 ExtractFromRow extractor,
                                 BufferManager bufferMgr,
                                 CachedNodeSupplier nodeCache,
                                 boolean pack,
                                 boolean useHeap,
                                 boolean allowDuplicates ) {
        super(delegate);
        assert extractor != null;
        this.workspaceName = workspaceName;
        this.width = delegate.width();
        this.cache = nodeCache;
        this.extractor = extractor;

        // Set up the row factory based upon the width of the delegate sequence...
        this.rowFactory = BufferedRows.serializer(nodeCache, width);

        // Set up the buffer ...
        SortingBuffer<Object, BufferedRow> buffer = null;
        TypeFactory<?> keyType = extractor.getType();
        if (allowDuplicates) {
            @SuppressWarnings( "rawtypes" )
            Serializer<? extends Comparable> keySerializer = (Serializer<? extends Comparable<?>>)bufferMgr.serializerFor(keyType);
            buffer = bufferMgr.createSortingWithDuplicatesBuffer(keySerializer, extractor.getType().getComparator(),
                                                                 (BufferedRowFactory<BufferedRow>)rowFactory).keepSize(true)
                              .useHeap(useHeap).make();
        } else {
            BTreeKeySerializer<Object> keySerializer = (BTreeKeySerializer<Object>)bufferMgr.bTreeKeySerializerFor(keyType, pack);
            if (keySerializer instanceof KeySerializerWithComparator) {
                keySerializer = ((KeySerializerWithComparator<Object>)keySerializer).withComparator(extractor.getType()
                                                                                                             .getComparator());
            }
            buffer = bufferMgr.createSortingBuffer(keySerializer, (BufferedRowFactory<BufferedRow>)rowFactory).keepSize(true)
                              .useHeap(useHeap).make();
        }
        this.buffer = buffer;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    protected long rowCount() {
        return buffer.size();
    }

    protected BufferedRow createRow( Batch currentRow ) {
        return rowFactory.createRow(currentRow);
    }

    /**
     * Load all of the rows from the supplied sequence into the buffer.
     * 
     * @param sequence the node sequence; may not be null
     * @param extractor the extractor for the sortable value; may not be null
     * @param rowsWithNullKey the buffer into which should be placed all rows for which the extracted key value is null; may be
     *        null if these are not to be kept
     * @return the size of the first batch, or 0 if there are no rows found
     */
    protected int loadAll( NodeSequence sequence,
                           ExtractFromRow extractor,
                           DistinctBuffer<BufferedRow> rowsWithNullKey ) {
        // Put all of the batches from the sequence into the buffer
        Batch batch = sequence.nextBatch();
        int batchSize = 0;
        Object value = null;
        while (batch != null && batchSize == 0) {
            while (batch.hasNext()) {
                batch.nextRow();
                value = extractor.getValueInRow(batch);
                if (value instanceof Object[]) {
                    // Put each of the values in the buffer ...
                    for (Object v : (Object[])value) {
                        buffer.put(v, createRow(batch));
                    }
                } else if (value != null) {
                    buffer.put(value, createRow(batch));
                } else if (rowsWithNullKey != null) {
                    rowsWithNullKey.addIfAbsent(createRow(batch));
                }
                ++batchSize;
            }
            batch = sequence.nextBatch();
        }
        while (batch != null) {
            while (batch.hasNext()) {
                batch.nextRow();
                value = extractor.getValueInRow(batch);
                if (value instanceof Object[]) {
                    // Put each of the values in the buffer ...
                    for (Object v : (Object[])value) {
                        buffer.put(v, createRow(batch));
                    }
                } else if (value != null) {
                    buffer.put(value, createRow(batch));
                } else if (rowsWithNullKey != null) {
                    rowsWithNullKey.addIfAbsent(createRow(batch));
                }
            }
            batch = sequence.nextBatch();
        }
        return batchSize;
    }

    protected Batch batchFrom( final Iterator<BufferedRow> rows,
                               final long maxBatchSize ) {
        if (rows == null || !rows.hasNext()) return null;
        if (maxBatchSize == 0 || remainingRowCount.get() <= 0) return NodeSequence.emptyBatch(workspaceName, this.width);
        final long rowsInBatch = Math.min(maxBatchSize, remainingRowCount.get());
        rowsLeftInBatch.set(rowsInBatch);
        return new Batch() {
            private BufferedRow current;

            @Override
            public int width() {
                return width;
            }

            @Override
            public long rowCount() {
                return rowsInBatch;
            }

            @Override
            public String getWorkspaceName() {
                return workspaceName;
            }

            @Override
            public boolean isEmpty() {
                return rowsInBatch <= 0;
            }

            @Override
            public boolean hasNext() {
                return rowsLeftInBatch.get() > 0 && rows.hasNext();
            }

            @Override
            public void nextRow() {
                current = rows.next();
                remainingRowCount.decrementAndGet();
                rowsLeftInBatch.decrementAndGet();
            }

            @Override
            public CachedNode getNode() {
                return current.getNode();
            }

            @Override
            public CachedNode getNode( int index ) {
                return current.getNode(index);
            }

            @Override
            public float getScore() {
                return current.getScore();
            }

            @Override
            public float getScore( int index ) {
                return current.getScore(index);
            }

            @Override
            public String toString() {
                return "(buffered-batch size=" + rowsInBatch + " )";
            }
        };
    }

    @Override
    public void close() {
        try {
            super.close();
        } finally {
            buffer.close();
        }
    }

}
