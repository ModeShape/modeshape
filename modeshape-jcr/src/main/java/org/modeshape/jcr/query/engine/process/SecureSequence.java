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

import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.index.local.MapDB;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.query.JcrQueryContext;
import org.modeshape.jcr.query.NodeSequence;

/**
 * A {@link org.modeshape.jcr.query.NodeSequence} implementation which only returns nodes on which an existing query context
 * has {@link org.modeshape.jcr.ModeShapePermissions#READ} permissions. The key of
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class SecureSequence extends DelegatingSequence {

    private final JcrQueryContext context;
    private final BufferManager.DistinctBuffer<NodeKey> buffer;

    /**
     * Creates a new secure sequence over an existing sequence.
     *
     * @param delegate a {@link org.modeshape.jcr.query.NodeSequence} which is being wrapped; may not be null
     * @param context the {@link org.modeshape.jcr.query.JcrQueryContext} for which the permissions are checked.
     * @param useHeap a flag which indicates if the cached keys will be stored on or off heap
     */
    public SecureSequence( NodeSequence delegate,
                           JcrQueryContext context,
                           boolean useHeap) {
        super(delegate);
        this.context = context;
        this.buffer = context.getBufferManager().createDistinctBuffer(MapDB.NODE_KEY_SERIALIZER).useHeap(useHeap).keepSize(false).make();
    }

    @Override
    public Batch nextBatch() {
        Batch nextBatch = super.nextBatch();
        return NodeSequence.batchFilteredWith(nextBatch, new NodeSequence.RowFilter() {
            @Override
            public boolean isCurrentRowValid( Batch batch ) {
                CachedNode node = batch.getNode();
                NodeKey key = node.getKey();
                if (buffer.contains(key)) {
                    return true;
                } else if (context.canRead(node)) {
                    buffer.add(key);
                    return true;
                }
                return false;
            }
        });
    }

    @Override
    public long getRowCount() {
        //we do not know up front how many rows there will be
        return -1;
    }
}
