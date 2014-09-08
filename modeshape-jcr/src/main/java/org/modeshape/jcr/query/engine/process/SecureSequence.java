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
import org.modeshape.jcr.query.JcrQueryContext;
import org.modeshape.jcr.query.NodeSequence;

/**
 * A {@link org.modeshape.jcr.query.NodeSequence} implementation which only returns nodes on which an existing query context
 * has {@link org.modeshape.jcr.ModeShapePermissions#READ} permissions.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class SecureSequence extends DelegatingSequence {

    protected final JcrQueryContext context;

    /**
     * Creates a new secure sequence over an existing sequence.
     *
     * @param delegate a {@link org.modeshape.jcr.query.NodeSequence} which is being wrapped; may not be null
     * @param context the {@link org.modeshape.jcr.query.JcrQueryContext} for which the permissions are checked.
     */
    public SecureSequence( NodeSequence delegate,
                           JcrQueryContext context ) {
        super(delegate);
        this.context = context;
    }

    @Override
    public Batch nextBatch() {
        Batch nextBatch = super.nextBatch();
        return NodeSequence.batchFilteredWith(nextBatch, new NodeSequence.RowFilter() {
            @Override
            public boolean isCurrentRowValid( Batch batch ) {
                CachedNode node = batch.getNode();
                return context.canRead(node);
            }
        });
    }

    @Override
    public long getRowCount() {
        //we do not know up front how many rows there will be
        return -1;
    }

    @Override
    public String toString() {
        return "(secure " + super.delegate + ")";
    }
}
