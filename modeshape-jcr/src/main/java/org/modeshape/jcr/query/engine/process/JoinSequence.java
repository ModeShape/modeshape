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

import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.CachedNodeSupplier;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.query.NodeSequence;
import org.modeshape.jcr.query.RowExtractors.ExtractFromRow;
import org.modeshape.jcr.query.model.JoinType;

/**
 * A {@link NodeSequence} implementation that performs a join of two delegate sequences.
 * 
 * @see HashJoinSequence
 * @author Randall Hauch (rhauch@redhat.com)
 */
@NotThreadSafe
public abstract class JoinSequence extends BufferingSequence {

    public static final class Range<K> {
        private final K lower;
        private final K upper;
        private final boolean lowerIncluded;
        private final boolean upperIncluded;

        public Range( K lower,
                      boolean lowerIncluded,
                      K upper,
                      boolean upperIncluded ) {
            this.lower = lower;
            this.upper = upper;
            this.lowerIncluded = lowerIncluded;
            this.upperIncluded = upperIncluded;
        }

        /**
         * Get the lower bound, if there is one.
         * 
         * @return the lower bound, or null if there is none
         */
        public K lowerBound() {
            return lower;
        }

        /**
         * Get the upper bound, if there is one.
         * 
         * @return the upper bound, or null if there is none
         */
        public K upperBound() {
            return upper;
        }

        public boolean isUpperBoundIncluded() {
            return upperIncluded;
        }

        public boolean isLowerBoundIncluded() {
            return lowerIncluded;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append(lowerIncluded ? '[' : '(');
            sb.append(lower);
            sb.append(',');
            sb.append(upper);
            sb.append(upperIncluded ? ']' : ')');
            return sb.toString();
        }
    }

    public static interface RangeProducer<K> {
        /**
         * Determine the range of keys given the input key.
         * 
         * @param input the input key
         * @return the range of output keys; may be null if no range is valid
         */
        Range<K> getRange( K input );
    }

    protected final NodeSequence left;
    protected final ExtractFromRow leftExtractor;
    protected final int leftWidth;
    protected final int totalWidth;
    protected final JoinType joinType;
    protected int batchSize = 100;
    protected Batch currentLeft;
    private BatchFactory batchFactory;

    protected JoinSequence( String workspaceName,
                            NodeSequence left,
                            NodeSequence right,
                            ExtractFromRow leftExtractor,
                            ExtractFromRow rightExtractor,
                            JoinType joinType,
                            BufferManager bufferMgr,
                            CachedNodeSupplier nodeCache,
                            boolean pack,
                            boolean useHeap,
                            boolean allowDuplicates ) {
        super(workspaceName, right, rightExtractor, bufferMgr, nodeCache, pack, useHeap, allowDuplicates);
        this.left = left;
        this.leftExtractor = leftExtractor;
        this.leftWidth = left.width();
        this.joinType = joinType;
        this.totalWidth = left.width() + right.width();
    }

    @Override
    public int width() {
        return totalWidth;
    }

    @Override
    public boolean isEmpty() {
        if (left.isEmpty()) {
            if (useNonMatchingRightRows() || useAllRightRowsWhenNoLeftRows()) {
                // Even if the left is empty, we still need to use the right rows ...
                return delegate.isEmpty();
            }
            // Left is empty, but we always have to have a left and don't care about the right ...
            return true;
        } else if (delegate.isEmpty()) {
            if (useAllLeftRowsWhenNoMatchingRightRows()) {
                // There are no right rows, but we still need to use all the rows on the left ...
                assert !left.isEmpty();
                return false;
            }
        }
        return false;
    }

    @Override
    public Batch nextBatch() {
        // Find the next non-null and non-empty left batch ...
        findNextNonEmptyLeftBatch();

        if (batchFactory == null) {
            // This is the first time through...
            if (currentLeft == null) {
                // And there are no rows on the left ...
                if (useAllRightRowsWhenNoLeftRows()) {
                    // But we have to return all the right rows ...
                    batchFactory = new RightOnlyBatchFactory();
                } else {
                    // We always return no rows, even if there are some on the right ...
                    batchFactory = new EmptyBatchFactory();
                }
            } else {
                // Otherwise, initialize the batch factory ...
                batchFactory = initialize();
            }
        }

        // Now return a new Batch instance that performs the join ...
        return batchFactory.nextBatch();
    }

    protected Batch findNextNonEmptyLeftBatch() {
        while (currentLeft == null) {
            currentLeft = left.nextBatch();
            if (currentLeft == null) return null; // no more left
            if (currentLeft.isEmpty() || !currentLeft.hasNext()) {
                // Skip any empty batches ...
                currentLeft = null;
            }
        }
        return currentLeft;
    }

    /**
     * Called once when the implementation is to create a {@link BatchFactory} for all batches returned by this join sequence.
     * This is called only when we know that there are at least some rows on the left side.
     * 
     * @return the batch factory that should be used to obtain the batches; never null
     */
    protected abstract BatchFactory initialize();

    /**
     * Determine whether the algorithm that encounters no left rows should process the right rows. This method only returns true
     * when the {@link #joinType join type} is a {@link JoinType#RIGHT_OUTER right outer join}.
     * <p>
     * The {@link JoinType#CROSS cross join} (or Cartesian Product) is not included because the number of rows returned will be
     * <code>N<sub>left</sub> x N<sub>right</sub></code>, where <code>N<sub>left</sub></code> is the number of rows on the
     * left-hand side, and <code>N<sub>right</sub></code> is the number of rows on the right-hand side. Therefore, if either is
     * empty, the result is empty.
     * </p>
     * 
     * @return true if the right rows should be returned when there are no left rows, or false otherwise
     */
    protected boolean useAllRightRowsWhenNoLeftRows() {
        return joinType == JoinType.RIGHT_OUTER;
    }

    /**
     * Determine whether the algorithm that encounters a left row with no matching right rows should still include such left rows
     * in the result. This method only returns true when the {@link #joinType join type} is a {@link JoinType#FULL_OUTER full
     * outer join} or {@link JoinType#LEFT_OUTER left outer join}.
     * 
     * @return true if the left rows should be returned even when there are no matching rows on the right, or false otherwise
     */
    protected boolean useAllLeftRowsWhenNoMatchingRightRows() {
        return joinType == JoinType.FULL_OUTER || joinType == JoinType.LEFT_OUTER;
    }

    /**
     * Determine whether the algorithm should return rows from the right-side of the join that were no matched to left rows. This
     * method only returns true when the {@link #joinType join type} is a {@link JoinType#FULL_OUTER full outer join} or
     * {@link JoinType#RIGHT_OUTER right outer join}.
     * 
     * @return true if the join algorithm should return rows on the right that did not match rows on the left, or false otherwise
     */
    protected boolean useNonMatchingRightRows() {
        return joinType == JoinType.FULL_OUTER || joinType == JoinType.RIGHT_OUTER;
    }

    @Override
    public void close() {
        try {
            super.close();
        } finally {
            left.close();
        }
    }

    /**
     * A factory for batches.
     * 
     * @author Randall Hauch (rhauch@redhat.com)
     */
    protected static interface BatchFactory {
        /**
         * Get the next batch.
         * 
         * @return the next batch, or null if there are no more batches
         */
        Batch nextBatch();
    }

    /**
     * A {@link BatchFactory} that always returns null.
     * 
     * @author Randall Hauch (rhauch@redhat.com)
     */
    protected final class EmptyBatchFactory implements BatchFactory {
        @Override
        public Batch nextBatch() {
            return null;
        }
    }

    /**
     * A {@link BatchFactory} for batches that return only the right-hand rows. This is useful only when there are no left-hand
     * rows and the join must still return all right-hand rows.
     * 
     * @author Randall Hauch (rhauch@redhat.com)
     */
    protected final class RightOnlyBatchFactory implements BatchFactory {
        @Override
        public Batch nextBatch() {
            Batch right = delegate.nextBatch();
            return right == null ? null : new RightOnlyBatch(right);
        }
    }

    protected final class RightOnlyBatch implements Batch {
        private final Batch right;

        protected RightOnlyBatch( Batch right ) {
            this.right = right;
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
            return right.isEmpty();
        }

        @Override
        public long rowCount() {
            return right.rowCount();
        }

        @Override
        public boolean hasNext() {
            return right.hasNext();
        }

        @Override
        public void nextRow() {
            right.nextRow();
        }

        @Override
        public CachedNode getNode() {
            // the left will always have at least one node, and we never have a left row ...
            return null;
        }

        @Override
        public CachedNode getNode( int index ) {
            if (index < leftWidth) return null;
            return right.getNode(index - leftWidth);
        }

        @Override
        public float getScore() {
            // the left will always have at least one score, and we never have a left row ...
            return 0.0f;
        }

        @Override
        public float getScore( int index ) {
            if (index < leftWidth) return 0.0f;
            return right.getScore(index - leftWidth);
        }
    }

    /**
     * A {@link BatchFactory} for batches that return only the left-hand rows. This is useful only when there are no right-hand
     * rows and the join must still return all left-hand rows.
     * 
     * @author Randall Hauch (rhauch@redhat.com)
     */
    protected final class LeftOnlyBatchFactory implements BatchFactory {
        @Override
        public Batch nextBatch() {
            Batch leftBatch = findNextNonEmptyLeftBatch();
            if (leftBatch == null) return null;
            currentLeft = null; // reset
            return new LeftOnlyBatch(leftBatch);
        }
    }

    protected final class LeftOnlyBatch implements Batch {
        private final Batch left;

        protected LeftOnlyBatch( Batch left ) {
            this.left = left;
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
            return left.isEmpty();
        }

        @Override
        public long rowCount() {
            return left.rowCount();
        }

        @Override
        public boolean hasNext() {
            return left.hasNext();
        }

        @Override
        public void nextRow() {
            left.nextRow();
        }

        @Override
        public CachedNode getNode() {
            return left.getNode();
        }

        @Override
        public CachedNode getNode( int index ) {
            if (index < leftWidth) return left.getNode(index);
            return null;
        }

        @Override
        public float getScore() {
            return left.getScore();
        }

        @Override
        public float getScore( int index ) {
            if (index < leftWidth) return left.getScore(index);
            return 0.0f;
        }
    }
}
