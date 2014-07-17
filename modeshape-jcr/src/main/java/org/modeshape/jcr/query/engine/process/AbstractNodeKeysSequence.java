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
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.jcr.cache.CachedNodeSupplier;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.query.NodeSequence;
import org.modeshape.jcr.query.RowExtractors;
import org.modeshape.jcr.query.model.TypeSystem;

/**
 * The base class of a {@link org.modeshape.jcr.query.NodeSequence} implementation which extracts node keys from 2 different
 * sequences and delegates to subclasses the behavior of deciding which batches to return based on which node keys are present/absent.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@NotThreadSafe
public abstract class AbstractNodeKeysSequence extends BufferingSequence {

    protected final NodeSequence leftSequence;
    protected final int totalWidth;

    private boolean initialized = false;

    protected AbstractNodeKeysSequence( String workspaceName,
                                        NodeSequence leftSequence,
                                        NodeSequence rightSequence,
                                        TypeSystem types,
                                        BufferManager bufferMgr,
                                        CachedNodeSupplier nodeCache,
                                        boolean pack,
                                        boolean useHeap ) {
        super(workspaceName, rightSequence, RowExtractors.extractUniqueKey(leftSequence.width(), types), bufferMgr, nodeCache,
              pack, useHeap, true);
        int leftWidth =  leftSequence.width();
        int rightWidth = rightSequence.width();
        if (leftWidth > 0 && rightWidth > 0 && leftWidth != rightWidth) {
            throw new IllegalArgumentException("The sequences must have the same width: " + leftSequence + " and " + rightSequence);
        }
        this.totalWidth = leftWidth;
        this.leftSequence = leftSequence;
    }

    @Override
    public Batch nextBatch() {
        if (!initialized) {
            //load all the keys from right sequence into the buffer
            loadAll(delegate, extractor, null);
            initialized = true;
        }
        Batch nextLeftBatch = leftSequence.nextBatch();
        if (nextLeftBatch == null) {
            //nothing more in the left, so we're done
            return null;
        }
        return batchWrapper(nextLeftBatch);
    }

    @Override
    public int width() {
        return totalWidth;
    }

    protected Object keyInLeftRow( Batch leftBatch ) {
        return extractor.getValueInRow(leftBatch);
    }

    protected Iterator<BufferedRows.BufferedRow> matchingRightRows(Object keyInLeft) {
        return buffer.getAll(keyInLeft);
    }

    protected abstract Batch batchWrapper( Batch leftBatch );
}
