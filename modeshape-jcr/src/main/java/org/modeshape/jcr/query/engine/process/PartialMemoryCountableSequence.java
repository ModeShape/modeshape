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

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.atomic.AtomicLong;
import org.modeshape.jcr.cache.CachedNodeSupplier;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.query.BufferManager.QueueBuffer;
import org.modeshape.jcr.query.NodeSequence;
import org.modeshape.jcr.query.engine.process.BufferedRows.BufferedRow;

/**
 * A sequence that will return an accurate size for a given NodeSequence by buffering and counting the nodes and then accessing
 * the buffered sequence. Some number of batches will be stored in-memory, while the rest are stored off-heap.
 * 
 * @see CountableSequence
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class PartialMemoryCountableSequence extends CountableSequence {

    private final Queue<Batch> inMemoryBatches;
    private final int approxRowsInMemory;

    public PartialMemoryCountableSequence( String workspaceName,
                                           NodeSequence original,
                                           BufferManager bufferMgr,
                                           CachedNodeSupplier nodeCache,
                                           int numRowsInMemory ) {
        super(workspaceName, original, bufferMgr, nodeCache, true);
        inMemoryBatches = new LinkedList<>();
        approxRowsInMemory = numRowsInMemory;
    }

    @Override
    protected Batch doNextBatch() {
        if (!inMemoryBatches.isEmpty()) {
            return inMemoryBatches.poll();
        }
        return super.doNextBatch();
    }

    @Override
    protected long loadAll( NodeSequence sequence,
                            QueueBuffer<BufferedRow> buffer,
                            AtomicLong batchSize ) {
        return loadAll(sequence, buffer, batchSize, inMemoryBatches, approxRowsInMemory);
    }
}
