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
import java.util.List;
import org.mapdb.Serializer;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.collection.MultiIterator;
import org.modeshape.common.collection.SequentialIterator;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.CachedNodeSupplier;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.query.BufferManager.DistinctBuffer;
import org.modeshape.jcr.query.NodeSequence;
import org.modeshape.jcr.query.RowExtractors.ExtractFromRow;
import org.modeshape.jcr.query.engine.process.BufferedRows.BufferedRow;
import org.modeshape.jcr.query.model.JoinType;
import org.modeshape.jcr.query.model.TypeSystem.TypeFactory;

/**
 * A {@link NodeSequence} implementation that performs an equijoin of two delegate sequences. The hash-join algorithm loads all
 * values on the right side into a buffer that hashes the right join condition value of each row. Then, it iterates through all
 * tuples on the left side and finds which of the values on the right have a matching join condition value.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 */
@NotThreadSafe
public class HashJoinSequence extends JoinSequence {

    protected final DistinctBuffer<Object> rightMatchedRowKeys;
    protected final DistinctBuffer<BufferedRow> rightRowsWithNullKey;
    protected final RangeProducer<Object> rangeProducer;

    @SuppressWarnings( "unchecked" )
    public HashJoinSequence( String workspaceName,
                             NodeSequence left,
                             NodeSequence right,
                             ExtractFromRow leftExtractor,
                             ExtractFromRow rightExtractor,
                             JoinType joinType,
                             BufferManager bufferMgr,
                             CachedNodeSupplier nodeCache,
                             RangeProducer<?> rangeProducer,
                             boolean pack,
                             boolean useHeap ) {
        super(workspaceName, left, right, leftExtractor, rightExtractor, joinType, bufferMgr, nodeCache, pack, useHeap, true);
        this.rangeProducer = (RangeProducer<Object>)rangeProducer;
        if (useNonMatchingRightRows()) {
            TypeFactory<?> keyType = rightExtractor.getType();
            Serializer<?> keySerializer = bufferMgr.serializerFor(keyType);
            rightMatchedRowKeys = (DistinctBuffer<Object>)bufferMgr.createDistinctBuffer(keySerializer).keepSize(true)
                                                                   .useHeap(useHeap).make();
            Serializer<BufferedRow> rowSerializer = (Serializer<BufferedRow>)BufferedRows.serializer(nodeCache, width);
            rightRowsWithNullKey = bufferMgr.createDistinctBuffer(rowSerializer).keepSize(true).useHeap(useHeap).make();
        } else {
            rightMatchedRowKeys = null;
            rightRowsWithNullKey = null;
        }
    }

    @Override
    protected BatchFactory initialize() {
        // Load all of the right sequence into the buffer ...
        int firstBatchSize = loadAll(delegate, extractor, rightRowsWithNullKey);
        if (firstBatchSize == 0) {
            // No rows were found on the right, so see if we need to return any nodes ...
            switch (joinType) {
                case CROSS:
                case RIGHT_OUTER:
                    // Nothing on the right, so return no rows ...
                    return new EmptyBatchFactory();
                case FULL_OUTER:
                case LEFT_OUTER:
                    // Nothing on the right but something on the left, so ...
                    return new LeftOnlyBatchFactory();
                case INNER:
                    // Nothing on the right, that means no left rows will match ...
                    return new EmptyBatchFactory();
            }
        }
        // Otherwise, there are rows on the left and the right ...
        switch (joinType) {
            case CROSS:
                // We use all of the rows on the right for every row on the left; logic is a little different ...
                return new HashCrossJoinBatchFactory();
            case RIGHT_OUTER:
            case FULL_OUTER:
            case LEFT_OUTER:
            case INNER:
            default:
                // We always try to match left and right rows, but possibly also include unmatched right rows ...
                return rangeProducer != null ? new HashJoinRangeBatchFactory() : new HashJoinBatchFactory();
        }
    }

    protected Iterator<BufferedRow> allRightRows() {
        if (rightRowsWithNullKey != null) {
            return SequentialIterator.create(rightRowsWithNullKey.iterator(), buffer.ascending());
        }
        return buffer.ascending();
    }

    @Override
    public void close() {
        try {
            super.close();
        } finally {
            try {
                if (rightMatchedRowKeys != null) {
                    rightMatchedRowKeys.close();
                }
            } finally {
                if (rightRowsWithNullKey != null) {
                    rightRowsWithNullKey.close();
                }
            }
        }
    }

    @Override
    public String toString() {
        return "(hash-join width=" + width() + " " + joinType + " left=" + left + ", right=" + delegate + ", on " + leftExtractor
               + "=" + extractor + " )";
    }

    protected class HashJoinBatchFactory implements BatchFactory {
        private Iterator<BufferedRow> rightRows;

        @Override
        public Batch nextBatch() {
            Batch leftBatch = findNextNonEmptyLeftBatch();
            if (leftBatch != null) {
                currentLeft = null; // reset ...
                return createBatch(leftBatch);
            }
            // Otherwise, we're done with the left side ...
            if (rightMatchedRowKeys == null) {
                // We never need to return any unused/unmatched rows from the right, so we're done ...
                return null;
            }
            if (rightRows == null) {
                // This is the first batch with the unused right rows, so get the iterator ...
                rightRows = allRightRows();
            }
            if (!rightRows.hasNext()) return null; // we're done!
            return new RightRowsBatch(rightRows, 100);
        }

        protected Batch createBatch( Batch leftBatch ) {
            return new HashJoinBatch(leftBatch);
        }
    }

    /**
     * A batch that contains rows that will have a left value and a right value.
     *
     * @author Randall Hauch (rhauch@redhat.com)
     */
    protected class HashJoinBatch implements Batch {
        private final Batch currentLeft;
        private Iterator<BufferedRow> rightMatchingRows;
        private BufferedRow currentRight;

        protected HashJoinBatch( Batch currentLeft ) {
            this.currentLeft = currentLeft;
            assert this.currentLeft != null;
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
            if (rightMatchingRows != null && rightMatchingRows.hasNext()) {
                // There are more rows on the right that match the current left row ...
                return true;
            }
            if (!currentLeft.hasNext()) {
                // No more left rows in this batch ...
                return false;
            }
            // Advanced to the next left row and find the matching rows on the right ...
            while (currentLeft.hasNext()) {
                currentLeft.nextRow();
                Object matchingValue = leftExtractor.getValueInRow(currentLeft);
                rightMatchingRows = getAllRightRowsFor(matchingValue);
                if (rightMatchingRows != null && rightMatchingRows.hasNext()) {
                    // Found a match which will be recorded when we go through the right matching rows...
                    return true;
                }
                // Did not find any matching rows on the right ...
                if (useAllLeftRowsWhenNoMatchingRightRows()) {
                    // We still have to include the left row ...
                    rightMatchingRows = null;
                    return true;
                }
                // Otherwise, we don't include the left row without matching rows on the right, so find the next left row ...
            }

            // No more left rows ...
            return false;
        }

        private Iterator<BufferedRow> getAllRightRowsFor( Object leftValue ) {
            if (leftValue instanceof Object[]) {
                // There are multiple left-hand values, so we have to look for each one ...
                List<Iterator<BufferedRow>> iterators = new LinkedList<>();
                for (Object left : (Object[])leftValue) {
                    Iterator<BufferedRow> matching = getRightRowsFor(left);
                    if (matching != null && matching.hasNext()) {
                        iterators.add(matching);
                    }
                }
                if (iterators.isEmpty()) return null;
                if (iterators.size() == 1) return iterators.get(0);
                return MultiIterator.fromIterators(iterators);
            }
            // This is just a single value or even null
            return getRightRowsFor(leftValue);
        }

        protected Iterator<BufferedRow> getRightRowsFor( Object leftValue ) {
            return buffer.getAll(leftValue);
        }

        protected void recordRightRowsMatched( Object rightKey ) {
            if (rightMatchedRowKeys != null) {
                // We only record the non-null values, since NULL never matches and they will always be unmatched ...
                // logger.trace("Join found matching rows on right with value {0}", matchingValue);
                rightMatchedRowKeys.addIfAbsent(rightKey);
            }
        }

        @Override
        public void nextRow() {
            // This current presumes that 'hasNext' was called and that either 'rightMatchingRows' is null (because there
            // was no match (e.g., left outer join) or that it is non-null and has at least one value ...
            if (rightMatchingRows != null) {
                currentRight = rightMatchingRows.next();
                // since there might be multiple rows on the right, we need to make sure we record each one
                recordRightRowsMatched(extractor.getValueInRow(currentRight));
            } else {
                currentRight = null;
            }
        }

        @Override
        public CachedNode getNode() {
            return currentLeft.getNode();
        }

        @Override
        public CachedNode getNode( int index ) {
            if (index < leftWidth) {
                return currentLeft.getNode(index);
            }
            if (currentRight == null) return null;
            return currentRight.getNode(index - leftWidth);
        }

        @Override
        public float getScore() {
            return currentLeft.getScore();
        }

        @Override
        public float getScore( int index ) {
            if (index < leftWidth) {
                return currentLeft.getScore(index);
            }
            if (currentRight == null) return 0.0f;
            return currentRight.getScore(index - leftWidth);
        }
    }

    protected class HashJoinRangeBatchFactory extends HashJoinBatchFactory {
        @Override
        protected Batch createBatch( Batch leftBatch ) {
            return new HashJoinRangeBatch(leftBatch);
        }
    }

    /**
     * A batch that contains rows that will have a left value and a right value.
     *
     * @author Randall Hauch (rhauch@redhat.com)
     */
    protected class HashJoinRangeBatch extends HashJoinBatch {
        protected HashJoinRangeBatch( Batch currentLeft ) {
            super(currentLeft);
            assert HashJoinSequence.this.rangeProducer != null;
        }

        @Override
        protected Iterator<BufferedRow> getRightRowsFor( Object leftValue ) {
            if (leftValue == null) {
                // Nothing on the right ever matches a NULL on the left ...
                return null;
            }
            Range<Object> range = HashJoinSequence.this.rangeProducer.getRange(leftValue);
            if (range == null) return null;
            return buffer.getAll(range.lowerBound(), range.isLowerBoundIncluded(), range.upperBound(),
                                 range.isUpperBoundIncluded());
        }
    }

    /**
     * A batch that contains rows that will have no left value and a right value. Every value on the left matches all values on
     * the right, since this is a cross-join.
     *
     * @author Randall Hauch (rhauch@redhat.com)
     */
    protected class HashCrossJoinBatch extends HashJoinBatch {

        protected HashCrossJoinBatch( Batch currentLeft ) {
            super(currentLeft);
        }

        @Override
        protected Iterator<BufferedRow> getRightRowsFor( Object leftValue ) {
            return allRightRows();
        }

        @Override
        protected void recordRightRowsMatched( Object rightKey ) {
            // do nothing
        }
    }

    protected class HashCrossJoinBatchFactory extends HashJoinBatchFactory {
        @Override
        protected Batch createBatch( Batch leftBatch ) {
            return new HashCrossJoinBatch(leftBatch);
        }
    }

    /**
     * A batch that contains rows that will have no left value and a right value.
     *
     * @author Randall Hauch (rhauch@redhat.com)
     */
    protected class RightRowsBatch implements Batch {
        private final Iterator<BufferedRow> rightRows;
        private final int maxSize;
        private BufferedRow currentRight;
        private int count = 0;

        protected RightRowsBatch( Iterator<BufferedRow> rightRows,
                                  int maxSize ) {
            this.rightRows = rightRows;
            this.maxSize = maxSize;
            assert this.rightRows != null;
            assert this.maxSize > 0;
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
            // Find the next buffered row that was not used ...
            while (rightRows.hasNext() && count < maxSize) {
                currentRight = rightRows.next();
                Object key = extractor.getValueInRow(currentRight);
                if (key == null || rightMatchedRowKeys.addIfAbsent(key)) {
                    logger.trace("Join found non-matched rows on right with value {0}", key);
                    ++count;
                    return true;
                }
                logger.trace("Join found matched rows on right with value {0}", key);
            }
            return false;
        }

        @Override
        public void nextRow() {
            // This currently presumes that 'hasNext' was called and that 'currentRight' has a value ...
        }

        @Override
        public CachedNode getNode() {
            // We're only returning right values, so there is never a value at index 0 ...
            return null;
        }

        @Override
        public CachedNode getNode( int index ) {
            if (currentRight != null && index >= leftWidth) {
                return currentRight.getNode(index - leftWidth);
            }
            return null;
        }

        @Override
        public float getScore() {
            // We're only returning right values, so there is never a value at index 0 ...
            return 0.0f;
        }

        @Override
        public float getScore( int index ) {
            if (currentRight != null && index >= leftWidth) {
                return currentRight.getScore(index - leftWidth);
            }
            return 0.0f;
        }
    }
}
