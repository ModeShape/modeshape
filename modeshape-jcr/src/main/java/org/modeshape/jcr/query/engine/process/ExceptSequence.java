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

import org.modeshape.jcr.cache.CachedNodeSupplier;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.query.NodeSequence;
import org.modeshape.jcr.query.model.TypeSystem;

/**
 * A {@link AbstractNodeKeysSequence} implementation which performs an EXCEPT operation between
 * 2 other sequences, keeping only instances which don't have a corresponding value on the right.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ExceptSequence extends AbstractNodeKeysSequence {

    public ExceptSequence( String workspaceName,
                           NodeSequence leftSequence,
                           NodeSequence rightSequence,
                           TypeSystem types,
                           BufferManager bufferMgr,
                           CachedNodeSupplier nodeCache,
                           boolean pack,
                           boolean useHeap ) {
        super(workspaceName, leftSequence, rightSequence, types, bufferMgr, nodeCache, pack, useHeap);
    }

    @Override
    protected Batch batchWrapper( Batch leftBatch ) {
        return NodeSequence.batchFilteredWith(leftBatch, new RowFilter() {
            @Override
            public boolean isCurrentRowValid( Batch leftBatch ) {
                Object keyInLeft = keyInLeftRow(leftBatch);
                //the row is valid only if there are no matching right keys
                return matchingRightRows(keyInLeft) == null;
            }
        });
    }

    @Override
    public String toString() {
        return "(except width=" + width() + " left=" + leftSequence + ", right=" + delegate + ", on " + extractor + " )";
    }
}
