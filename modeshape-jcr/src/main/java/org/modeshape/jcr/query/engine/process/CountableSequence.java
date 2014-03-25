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
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import org.mapdb.Serializer;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.CachedNodeSupplier;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.query.BufferManager.QueueBuffer;
import org.modeshape.jcr.query.NodeSequence;
import org.modeshape.jcr.query.engine.process.BufferedRows.BufferedRow;
import org.modeshape.jcr.query.engine.process.BufferedRows.BufferedRowFactory;

/**
 * A sequence that will return an accurate size for a given NodeSequence by buffering and counting the nodes and then accessing
 * the buffered sequence.
 * 
 * @see PartialMemoryCountableSequence
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class CountableSequence extends NodeSequence {

    private final NodeSequence original;
    private final BufferedRowFactory<? extends BufferedRow> rowFactory;
    private final QueueBuffer<BufferedRow> buffer;
    protected final AtomicLong remainingRowCount = new AtomicLong();
    protected final String workspaceName;
    protected final int width;
    private Iterator<BufferedRow> bufferedRows;
    private final AtomicLong batchSize = new AtomicLong();
    private long totalSize = -1L;

    @SuppressWarnings( "unchecked" )
    public CountableSequence( String workspaceName,
                              NodeSequence original,
                              BufferManager bufferMgr,
                              CachedNodeSupplier nodeCache,
                              boolean useHeap ) {
        this.original = original;
        this.workspaceName = workspaceName;
        this.width = original.width();
        assert !original.isEmpty();
        assert original.getRowCount() == -1;
        assert original.width() != 0;
        // Create the row factory ...
        this.rowFactory = BufferedRows.serializer(nodeCache, width);
        // Create the buffer into which we'll place the rows with null keys ...
        Serializer<BufferedRow> rowSerializer = (Serializer<BufferedRow>)BufferedRows.serializer(nodeCache, width);
        buffer = bufferMgr.createQueueBuffer(rowSerializer).useHeap(useHeap).make();
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public long getRowCount() {
        initialize();
        assert totalSize >= 0L;
        return totalSize;
    }

    @Override
    public final Batch nextBatch() {
        initialize();
        return doNextBatch();
    }

    protected Batch doNextBatch() {
        return batchFrom(bufferedRows, batchSize.get());
    }

    @Override
    public void close() {
        buffer.close();
    }

    public final void initialize() {
        if (bufferedRows == null) {
            doInitialize();
        }
    }

    protected void doInitialize() {
        // Load all the rows into the buffer ...
        totalSize = loadAll(original, buffer, batchSize);
        remainingRowCount.set(totalSize);
        original.close();
        bufferedRows = buffer.iterator();
    }

    /**
     * Load all of the rows from the supplied sequence into the buffer.
     * 
     * @param sequence the node sequence; may not be null
     * @param buffer the buffer into which all rows should be loaded; may not be null
     * @param batchSize the atomic that should be set with the size of the first batch
     * @return the total number of rows
     */
    protected long loadAll( NodeSequence sequence,
                            QueueBuffer<BufferedRow> buffer,
                            AtomicLong batchSize ) {
        return loadAll(sequence, buffer, batchSize, null, 0);
    }

    /**
     * Load all of the rows from the supplied sequence into the buffer.
     * 
     * @param sequence the node sequence; may not be null
     * @param buffer the buffer into which all rows should be loaded; may not be null
     * @param batchSize the atomic that should be set with the size of the first batch
     * @param inMemoryBatches the queue into which batches that are kept in-memory; may be null, in which case all batches are
     *        placed into the buffer
     * @param numRowsInMemory the approximate number of rows that should be kept in-memory; may be non-positive if all batches are
     *        to be placed into the buffer
     * @return the total number of rows
     */
    protected long loadAll( NodeSequence sequence,
                            QueueBuffer<BufferedRow> buffer,
                            AtomicLong batchSize,
                            Queue<Batch> inMemoryBatches,
                            int numRowsInMemory ) {
        // Put all of the batches from the sequence into the buffer
        Batch batch = sequence.nextBatch();
        boolean loadIntoMemory = numRowsInMemory > 0 && inMemoryBatches != null;
        long numInMemory = 0L;
        while (batch != null && batchSize.get() == 0L) {
            if (loadIntoMemory) {
                Batch copy = NodeSequence.batchWithCount(batch);
                inMemoryBatches.add(copy);
                batchSize.set(copy.rowCount());
                numInMemory += copy.rowCount();
                numRowsInMemory -= batchSize.get();
                if (numRowsInMemory <= 0) loadIntoMemory = false;
            } else {
                while (batch.hasNext()) {
                    batch.nextRow();
                    buffer.append(createRow(batch));
                    batchSize.incrementAndGet();
                }
            }
            batch = sequence.nextBatch();
        }
        while (batch != null) {
            if (loadIntoMemory) {
                Batch copy = NodeSequence.batchWithCount(batch);
                inMemoryBatches.add(copy);
                numInMemory += copy.rowCount();
                numRowsInMemory -= copy.rowCount();
                if (numRowsInMemory <= 0) loadIntoMemory = false;
            } else {
                while (batch.hasNext()) {
                    batch.nextRow();
                    buffer.append(createRow(batch));
                }
            }
            batch = sequence.nextBatch();
        }
        return buffer.size() + numInMemory;
    }

    protected BufferedRow createRow( Batch currentRow ) {
        return rowFactory.createRow(currentRow);
    }

    protected Batch batchFrom( final Iterator<BufferedRow> rows,
                               final long maxBatchSize ) {
        if (remainingRowCount.get() <= 0 || !rows.hasNext()) return null;
        if (maxBatchSize == 0) return NodeSequence.emptyBatch(workspaceName, this.width);
        final long rowsInBatch = Math.min(maxBatchSize, remainingRowCount.get());
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
                return false;
            }

            @Override
            public boolean hasNext() {
                return remainingRowCount.get() > 0 && rows.hasNext();
            }

            @Override
            public void nextRow() {
                current = rows.next();
                remainingRowCount.decrementAndGet();
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
        };
    }

}
