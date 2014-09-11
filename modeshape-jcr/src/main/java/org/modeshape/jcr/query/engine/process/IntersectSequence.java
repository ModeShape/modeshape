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
import java.util.NoSuchElementException;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.CachedNodeSupplier;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.query.NodeSequence;
import org.modeshape.jcr.query.model.TypeSystem;

/**
 * A {@link AbstractNodeKeysSequence} implementation which performs an INTERSECT operation between 2 other sequences,
 * keeping only instances which have the same unique value in both sequences.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class IntersectSequence extends AbstractNodeKeysSequence {

    public IntersectSequence( String workspaceName,
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
        return new IntersectBatch(leftBatch);
    }

    /**
     * A batch implementation which wraps the left batch, but return rows from the matching keys from the right. The reason
     * is that there may be duplicate keys on the right which should really be returned in case of an INTERSECT ALL and
     * simply looking & filtering the left batch is not enough.
     */
    protected class IntersectBatch implements Batch {
        private final Batch leftBatch;

        private Iterator<BufferedRows.BufferedRow> matchingRightRows;
        private BufferedRows.BufferedRow currentRight;

        protected IntersectBatch( Batch leftBatch ) {
            this.leftBatch = leftBatch;
        }

        @Override
        public int width() {
            return totalWidth;
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
        public long rowCount() {
            return -1; // don't really know how many ...
        }

        @Override
        public boolean hasNext() {
            if (matchingRightRows != null && matchingRightRows.hasNext()) {
                return true;
            }

            while (leftBatch.hasNext()) {
                leftBatch.nextRow();
                Object keyInLeftRow = keyInLeftRow(leftBatch);
                matchingRightRows = matchingRightRows(keyInLeftRow);
                if (matchingRightRows != null) {
                    //we found keys on the right matching the current key from the left (there may be multiple keys with the same value)
                    break;
                }
            }

            return matchingRightRows != null && matchingRightRows.hasNext();
        }

        @Override
        public void nextRow() {
            currentRight = matchingRightRows.next();
        }

        @Override
        public CachedNode getNode() {
            if (currentRight == null) {
                throw new NoSuchElementException();
            }
            return currentRight.getNode();
        }

        @Override
        public CachedNode getNode( int index ) {
            if (currentRight == null) {
                throw new NoSuchElementException();
            }
            return currentRight.getNode(index);
        }

        @Override
        public float getScore() {
            if (currentRight == null) {
                throw new NoSuchElementException();
            }
            return currentRight.getScore();
        }

        @Override
        public float getScore( int index ) {
            if (currentRight == null) {
                throw new NoSuchElementException();
            }
            return currentRight.getScore(index);
        }
    }

    @Override
    public String toString() {
        return "(intersect width=" + width() + " left=" + leftSequence + ", right=" + delegate + ", on " + extractor + " )";
    }
}
