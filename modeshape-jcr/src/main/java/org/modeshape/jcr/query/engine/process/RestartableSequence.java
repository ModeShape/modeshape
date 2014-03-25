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
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import org.mapdb.Serializer;
import org.modeshape.common.collection.CloseableSupplier;
import org.modeshape.common.collection.EmptyIterator;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.CachedNodeSupplier;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.query.BufferManager.QueueBuffer;
import org.modeshape.jcr.query.NodeSequence;
import org.modeshape.jcr.query.NodeSequence.Restartable;
import org.modeshape.jcr.query.engine.process.BufferedRows.BufferedRow;
import org.modeshape.jcr.query.engine.process.BufferedRows.BufferedRowFactory;

/**
 * A {@link NodeSequence} that captures the buffers as they are used (or all at once) so that the sequence can be restarted.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class RestartableSequence extends NodeSequence implements Restartable {

    protected final NodeSequence original;
    private final BufferedRowFactory<? extends BufferedRow> rowFactory;
    protected final Serializer<BufferedRow> rowSerializer;
    protected final Queue<Batch> inMemoryBatches;
    protected final QueueBufferSupplier offHeapBatchesSupplier;
    protected final String workspaceName;
    protected final AtomicLong remainingRowCount = new AtomicLong();
    private final int targetNumRowsInMemory;
    protected final int width;
    private BatchSequence batches;
    protected final AtomicLong batchSize = new AtomicLong();
    protected int actualNumRowsInMemory = 0;
    protected long totalSize = 0L;
    protected boolean loadedAll = false;
    protected boolean usedOffHeap = false;

    @SuppressWarnings( "unchecked" )
    public RestartableSequence( String workspaceName,
                                NodeSequence original,
                                final BufferManager bufferMgr,
                                CachedNodeSupplier nodeCache,
                                final int numRowsInMemory ) {
        this.original = original;
        this.workspaceName = workspaceName;
        this.width = original.width();
        assert !original.isEmpty();
        assert original.width() != 0;
        // Create the row factory ...
        this.rowFactory = BufferedRows.serializer(nodeCache, width);
        // Create the buffer into which we'll place the rows with null keys ...
        rowSerializer = (Serializer<BufferedRow>)BufferedRows.serializer(nodeCache, width);
        // Create the in-memory storage ...
        this.targetNumRowsInMemory = numRowsInMemory;
        inMemoryBatches = new LinkedList<>();
        // Create the supplier for the off-heap storage ...
        offHeapBatchesSupplier = new QueueBufferSupplier(bufferMgr);
        // Create the batch sequence that will copy and load each batch before returning it ...
        batches = new BatchSequence() {
            private final AtomicReference<Batch> copiedBatch = new AtomicReference<>();

            @Override
            public Batch nextBatch() {
                Batch batch = RestartableSequence.this.original.nextBatch();
                if (batch == null) {
                    // There are no more batches, so we have seen, copied, and loaded all of the batches ...
                    loadedAll = true;
                    return null;
                }
                // Otherwise, make a copy of it and load it into the storage ...
                boolean loadIntoMemory = inMemoryBatches != null && actualNumRowsInMemory < numRowsInMemory;
                totalSize += loadBatch(batch, loadIntoMemory, copiedBatch);
                if (batchSize.get() == 0L) batchSize.set(copiedBatch.get().rowCount());
                return copiedBatch.get();
            }
        };
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public long getRowCount() {
        if (batches == null) return 0L;// closed
        loadRemaining();
        return totalSize;
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public Batch nextBatch() {
        if (batches == null) return null; // closed
        return batches.nextBatch();
    }

    @Override
    public void restart() {
        loadRemaining();
        restartBatches();
    }

    protected void restartBatches() {
        if (batches == null) return; // closed
        remainingRowCount.set(totalSize);
        batches = new BatchSequence() {
            private Iterator<Batch> inMemory = inMemoryBatches.iterator();
            private Iterator<BufferedRow> persisted;

            @Override
            public Batch nextBatch() {
                if (inMemory.hasNext()) {
                    Batch result = inMemory.next();
                    if (result instanceof Restartable) {
                        ((Restartable)result).restart();
                    }
                    return result;
                }
                if (persisted == null) {
                    persisted = offHeapBatchesSupplier.iterator();
                }
                if (!persisted.hasNext()) return null;
                return batchFrom(persisted, batchSize.get());
            }
        };

    }

    @Override
    public void close() {
        RuntimeException error = null;
        try {
            original.close();
        } catch (RuntimeException e) {
            error = e;
        } finally {
            try {
                inMemoryBatches.clear();
                totalSize = 0L;
                remainingRowCount.set(0L);
                offHeapBatchesSupplier.close();
                loadedAll = true;
            } catch (RuntimeException e) {
                if (error == null) error = e;
            } finally {
                batches = null;
                if (error != null) throw error;
            }
        }
    }

    /**
     * Load all of the remaining rows from the supplied sequence into the buffer.
     */
    protected void loadRemaining() {
        if (!loadedAll) {
            // Put all of the batches from the sequence into the buffer
            assert targetNumRowsInMemory >= 0L;
            assert batchSize != null;
            Batch batch = original.nextBatch();
            boolean loadIntoMemory = inMemoryBatches != null && actualNumRowsInMemory < targetNumRowsInMemory;
            while (batch != null) {
                long rows = loadBatch(batch, loadIntoMemory, null);
                if (batchSize.get() == 0L) batchSize.set(rows);
                if (loadIntoMemory) {
                    assert inMemoryBatches != null;
                    if (actualNumRowsInMemory >= targetNumRowsInMemory) loadIntoMemory = false;
                }
                batch = original.nextBatch();
            }
            long numInMemory = inMemoryBatches != null ? actualNumRowsInMemory : 0L;
            totalSize = offHeapBatchesSupplier.size() + numInMemory;
            loadedAll = true;
            restartBatches();
        }
    }

    protected long loadBatch( Batch batch,
                              boolean loadIntoMemory,
                              AtomicReference<Batch> copyOutput ) {
        assert batch != null;
        if (batch.isEmpty()) {
            if (copyOutput != null) copyOutput.set(batch);
            return 0L;
        }
        // Put all of the batches from the sequence into the buffer
        if (loadIntoMemory) {
            Batch copy = NodeSequence.copy(batch);
            inMemoryBatches.add(copy);
            if (copyOutput != null) copyOutput.set(copy);
            long numRows = copy.rowCount();
            actualNumRowsInMemory += numRows;
            return numRows;
        }
        if (copyOutput != null) {
            batch = NodeSequence.copy(batch);
            copyOutput.set(batch);
        }
        long batchSize = 0L;
        QueueBuffer<BufferedRow> persistedBatches = offHeapBatchesSupplier.get();
        while (batch.hasNext()) {
            batch.nextRow();
            persistedBatches.append(createRow(batch));
            ++batchSize;
        }
        if (batch instanceof Restartable) {
            ((Restartable)batch).restart();
        }
        return batchSize;
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

            @Override
            public String toString() {
                return "(restartable-batch width=" + width + " rows=" + rowCount() + ")";
            }
        };
    }

    @Override
    public String toString() {
        return "(restartable total-size=" + totalSize + " " + original + " )";
    }

    protected static interface BatchSequence {
        Batch nextBatch();
    }

    protected class QueueBufferSupplier implements CloseableSupplier<QueueBuffer<BufferedRow>>, Iterable<BufferedRow> {
        private QueueBuffer<BufferedRow> buffer;
        private final BufferManager bufferMgr;

        protected QueueBufferSupplier( BufferManager bufferMgr ) {
            this.bufferMgr = bufferMgr;
        }

        @Override
        public Iterator<BufferedRow> iterator() {
            return buffer != null ? buffer.iterator() : new EmptyIterator<BufferedRow>();
        }

        protected long size() {
            return buffer != null ? buffer.size() : 0L;
        }

        @Override
        public QueueBuffer<BufferedRow> get() {
            if (buffer == null) {
                buffer = bufferMgr.createQueueBuffer(rowSerializer).useHeap(false).make();

            }
            return buffer;
        }

        @Override
        public void close() {
            if (buffer != null) {
                try {
                    buffer.close();
                } finally {
                    buffer = null;
                }
            }
        }

    }

}
