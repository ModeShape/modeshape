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
package org.modeshape.jcr.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.Binary;
import org.modeshape.jcr.api.query.qom.Limit;
import org.modeshape.jcr.cache.CachedNode;
import org.modeshape.jcr.cache.NodeCache;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.RepositoryCache;
import org.modeshape.jcr.query.model.DynamicOperand;
import org.modeshape.jcr.query.model.NullOrder;
import org.modeshape.jcr.query.model.Order;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.query.model.TypeSystem.TypeFactory;
import org.modeshape.jcr.value.BinaryValue;
import org.modeshape.jcr.value.InvalidPathException;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.Reference;
import org.modeshape.jcr.value.binary.BinaryStore;
import org.modeshape.jcr.value.binary.BinaryStoreException;

/**
 * A sequence of nodes that is accessed by batches and that is accessible only once.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
@NotThreadSafe
public abstract class NodeSequence {

    protected static final Logger LOGGER = Logger.getLogger(NodeSequence.class);

    /**
     * Get the number of nodes in each row.
     * 
     * @return the width of each row; always positive
     */
    public abstract int width();

    /**
     * Get the number of rows in this sequence. If the actual number of rows is not known, this method returns -1.
     * 
     * @return the number of rows, or -1 if the number of rows is not known or cannot be found with significant overhead
     */
    public abstract long getRowCount();

    /**
     * Determine whether this results is known to be empty.
     * 
     * @return the true if there are no results, or false if there is at least some or if the number of rows is not known
     */
    public abstract boolean isEmpty();

    /**
     * Get the next batch of {@link NodeKey} instances.
     * 
     * @return the next batch, or null if there are no more batches.
     */
    public abstract Batch nextBatch();

    /**
     * Signal that this node sequence is no longer needed.
     */
    public abstract void close();

    /**
     * An interface that abstracts accessing the node(s) and score(s) in a row.
     * 
     * @author Randall Hauch (rhauch@redhat.com)
     */
    @NotThreadSafe
    public interface RowAccessor {
        /**
         * Get the number of nodes in each row.
         * 
         * @return the width of each row; always positive
         */
        int width();

        /**
         * Get the first node in the current row. This is a convenience method that is equivalent to <code>getNode(0)</code>.
         * 
         * @return the first node in the current row; may be null if there is no node at the first position in this row
         * @throws NoSuchElementException if there is no current row
         */
        CachedNode getNode();

        /**
         * Get the node at the specified index in the current row.
         * 
         * @param index the 0-based index for the node in the row; must be less than {@link #width()}.
         * @return the specified node in the current row; may be null if there is no node at the valid index in this row
         * @throws NoSuchElementException if there is no current row
         * @throws IndexOutOfBoundsException if the index is not greater than 0 and less than {@link #width()}
         */
        CachedNode getNode( int index );

        /**
         * Get the score for the first node in the current row. This is a convenience method that is equivalent to
         * <code>getScore(0)</code>.
         * 
         * @return the score for the first node in the current row; always 0.0f if there is no corresponding node at the valid
         *         index in this row
         * @throws NoSuchElementException if there is no current row
         */
        float getScore();

        /**
         * Get the score for the specified node in the current row.
         * 
         * @param index the 0-based index for the node in the row; must be less than {@link #width()}.
         * @return the score for the specified node in the current row; always 0.0f if there is no corresponding node at the valid
         *         index in this row
         * @throws NoSuchElementException if there is no current row
         * @throws IndexOutOfBoundsException if the index is not greater than 0 and less than {@link #width()}
         */
        float getScore( int index );

    }

    /**
     * A batch of rows containing nodes and scores.
     * 
     * @see NodeSequence#nextBatch()
     */
    @NotThreadSafe
    public interface Batch extends RowAccessor {

        /**
         * Get the name of the workspace in which exists all nodes in this batch.
         * 
         * @return the workspace name; never null
         */
        String getWorkspaceName();

        /**
         * Get the number of rows in the batch, if that information is available.
         * 
         * @return the number of rows; may be equal to -1 if the number of rows is not known without significant overhead
         */
        long rowCount();

        /**
         * Determine whether this batch is empty.
         * 
         * @return the true if there are no results, or false if there is at least some
         */
        boolean isEmpty();

        /**
         * Determine if there are more rows in this batch.
         * 
         * @return true if there are more rows, or false otherwise
         */
        boolean hasNext();

        /**
         * Move to the next row in this batch.
         * 
         * @throws NoSuchElementException if there is no current row
         */
        void nextRow();
    }

    public static interface Restartable {
        void restart();
    }

    /**
     * Get an empty node sequence.
     * 
     * @param width the width of the batches; must be positive
     * @return the empty node sequence; never null
     */
    public static NodeSequence emptySequence( final int width ) {
        assert width >= 0;
        return new NodeSequence() {
            @Override
            public int width() {
                return width;
            }

            @Override
            public Batch nextBatch() {
                return null;
            }

            @Override
            public long getRowCount() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return true;
            }

            @Override
            public void close() {
            }

            @Override
            public String toString() {
                return "(empty-sequence width=" + width() + ")";
            }
        };
    }

    /**
     * Get a batch of nodes that is empty.
     * 
     * @param workspaceName the name of the workspace
     * @param width the width of the batch; must be positive
     * @return the empty node batch; never null
     */
    public static Batch emptyBatch( final String workspaceName,
                                    final int width ) {
        assert width > 0;
        return new Batch() {
            @Override
            public boolean hasNext() {
                return false;
            }

            @Override
            public String getWorkspaceName() {
                return workspaceName;
            }

            @Override
            public int width() {
                return width;
            }

            @Override
            public long rowCount() {
                return 0;
            }

            @Override
            public boolean isEmpty() {
                return true;
            }

            @Override
            public void nextRow() {
                throw new NoSuchElementException();
            }

            @Override
            public CachedNode getNode() {
                throw new NoSuchElementException();
            }

            @Override
            public CachedNode getNode( int index ) {
                throw new NoSuchElementException();
            }

            @Override
            public float getScore() {
                throw new NoSuchElementException();
            }

            @Override
            public float getScore( int index ) {
                throw new NoSuchElementException();
            }

            @Override
            public String toString() {
                return "(empty-batch width=" + width() + ")";
            }
        };
    }

    /**
     * Create a sequence of nodes that returns the supplied single batch of nodes.
     * 
     * @param sequence the node keys to be returned; if null, an {@link #emptySequence empty instance} is returned
     * @return the sequence of nodes; never null
     */
    public static NodeSequence withBatch( final Batch sequence ) {
        if (sequence == null) return emptySequence(1);
        return new NodeSequence() {
            private boolean done = false;

            @Override
            public int width() {
                return sequence.width();
            }

            @Override
            public long getRowCount() {
                return sequence.rowCount();
            }

            @Override
            public boolean isEmpty() {
                return sequence.isEmpty();
            }

            @Override
            public Batch nextBatch() {
                if (done) return null;
                done = true;
                return sequence;
            }

            @Override
            public void close() {
            }

            @Override
            public String toString() {
                return "(sequence-with-batch " + sequence + " )";
            }
        };
    }

    /**
     * Create a sequence of nodes that iterates over the supplied batches of nodes. Note that the supplied iterator is accessed
     * lazily as the resulting sequence instance is {@link #nextBatch() used}.
     * 
     * @param batches the iterable container containing the node batches to be returned; if null, an {@link #emptySequence empty
     *        instance} is returned
     * @param width the width of the batch; must be positive
     * @return the sequence of nodes; never null
     */
    public static NodeSequence withBatches( final Collection<Batch> batches,
                                            final int width ) {
        if (batches == null || batches.isEmpty()) return emptySequence(width);
        if (batches.size() == 1) {
            Batch batch = batches.iterator().next();
            assert width == batch.width();
            return withBatch(batch);
        }
        // Tally the size of each batch ...
        long rowCount = 0L;
        for (Batch batch : batches) {
            long count = batch.rowCount();
            rowCount = count < 0L ? -1L : rowCount + count;
        }
        return withBatches(batches.iterator(), width, rowCount);
    }

    /**
     * Create a sequence of nodes that iterates over the supplied batches of nodes. Note that the supplied iterator is accessed
     * lazily as the resulting sequence instance is {@link #nextBatch() used}.
     * 
     * @param batches the iterator over the nodes to be returned; if null, an {@link #emptySequence empty instance} is returned
     * @param width the width of the batch; must be positive, and must be the width of all the batches
     * @param rowCount the number of rows in the batches; must be -1 if not known, 0 if known to be empty, or a positive number if
     *        the number of rows is known
     * @return the sequence of nodes; never null
     * @see #withBatches(Collection, int)
     */
    public static NodeSequence withBatches( final Iterator<Batch> batches,
                                            final int width,
                                            final long rowCount ) {
        assert rowCount >= -1;
        if (batches == null) return emptySequence(width);
        return new NodeSequence() {
            @Override
            public int width() {
                return width;
            }

            @Override
            public long getRowCount() {
                return rowCount;
            }

            @Override
            public boolean isEmpty() {
                return rowCount == 0;
            }

            @Override
            public Batch nextBatch() {
                return batches.hasNext() ? batches.next() : null;
            }

            @Override
            public void close() {
            }

            @Override
            public String toString() {
                return "(sequence width=" + width() + " (iterator<batch>) )";
            }
        };
    }

    /**
     * Create a sequence of nodes that iterates over the supplied nodes. Note that the supplied iterator is accessed lazily as the
     * resulting sequence's {@link #nextBatch() first batch} is {@link Batch#nextRow() used}.
     * 
     * @param nodes the iterator over the nodes to be returned; if null, an {@link #emptySequence empty instance} is returned
     * @param score the score to return for all of the nodes
     * @param workspaceName the name of the workspace in which all of the nodes exist
     * @return the sequence of nodes; never null
     */
    public static NodeSequence withNodes( final Collection<CachedNode> nodes,
                                          final float score,
                                          final String workspaceName ) {
        if (nodes == null || nodes.isEmpty()) return emptySequence(1);
        return withNodes(nodes.iterator(), nodes.size(), score, workspaceName);
    }

    /**
     * Create a sequence of nodes that iterates over the supplied nodes. Note that the supplied iterator is accessed lazily as the
     * resulting sequence's {@link #nextBatch() first batch} is {@link Batch#nextRow() used}.
     * 
     * @param nodes the iterator over the node keys to be returned; if null, an {@link #emptySequence empty instance} is returned
     * @param nodeCount the number of nodes in the iterator; must be -1 if not known, 0 if known to be empty, or a positive number
     *        if the number of nodes is known
     * @param score the score to return for all of the nodes
     * @param workspaceName the name of the workspace in which all of the nodes exist
     * @return the sequence of nodes; never null
     */
    public static NodeSequence withNodes( final Iterator<CachedNode> nodes,
                                          final long nodeCount,
                                          final float score,
                                          final String workspaceName ) {
        assert nodeCount >= -1;
        if (nodes == null) return emptySequence(1);
        return withBatch(batchOf(nodes, nodeCount, score, workspaceName));
    }

    public static NodeSequence withNode( final CachedNode node,
                                         final int width,
                                         final float score,
                                         final String workspaceName ) {
        return new NodeSequence() {
            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public long getRowCount() {
                return 1;
            }

            @Override
            public int width() {
                return width;
            }

            @Override
            public Batch nextBatch() {
                return new Batch() {
                    private boolean done = false;

                    @Override
                    public String getWorkspaceName() {
                        return workspaceName;
                    }

                    @Override
                    public float getScore() {
                        return score;
                    }

                    @Override
                    public float getScore( int index ) {
                        if (index == 0) return score;
                        throw new IndexOutOfBoundsException();
                    }

                    @Override
                    public CachedNode getNode( int index ) {
                        if (index == 0) return getNode();
                        throw new IndexOutOfBoundsException();
                    }

                    @Override
                    public CachedNode getNode() {
                        return done ? null : node;
                    }

                    @Override
                    public boolean hasNext() {
                        return !done;
                    }

                    @Override
                    public void nextRow() {
                        done = true;
                    }

                    @Override
                    public boolean isEmpty() {
                        return false;
                    }

                    @Override
                    public long rowCount() {
                        return 1;
                    }

                    @Override
                    public int width() {
                        return width;
                    }
                };
            }

            @Override
            public void close() {
            }
        };
    }

    /**
     * Create a sequence of nodes that iterates over the supplied node keys. Note that the supplied iterator is accessed lazily as
     * the resulting sequence's {@link #nextBatch() first batch} is {@link Batch#nextRow() used}.
     * 
     * @param keys the iterator over the keys of the nodes to be returned; if null, an {@link #emptySequence empty instance} is
     *        returned
     * @param score the score to return for all of the nodes
     * @param workspaceName the name of the workspace in which all of the nodes exist
     * @param repository the repository cache used to access the workspaces and cached nodes; may be null only if the key sequence
     *        is null or empty
     * @return the sequence of nodes; never null
     */
    public static NodeSequence withNodeKeys( final Collection<NodeKey> keys,
                                             final float score,
                                             final String workspaceName,
                                             final RepositoryCache repository ) {
        if (keys == null || keys.isEmpty()) return emptySequence(1);
        return withNodeKeys(keys.iterator(), keys.size(), score, workspaceName, repository);
    }

    /**
     * Create a sequence of nodes that iterates over the supplied node keys. Note that the supplied iterator is accessed lazily as
     * the resulting sequence's {@link #nextBatch() first batch} is {@link Batch#nextRow() used}.
     * 
     * @param keys the iterator over the keys of the nodes to be returned; if null, an {@link #emptySequence empty instance} is
     *        returned
     * @param score the score to return for all of the nodes
     * @param workspaceName the name of the workspace in which all of the nodes exist
     * @param cache the cache used to access the cached nodes; may be null only if the key sequence is null or empty
     * @return the sequence of nodes; never null
     */
    public static NodeSequence withNodeKeys( final Collection<NodeKey> keys,
                                             final float score,
                                             final String workspaceName,
                                             final NodeCache cache ) {
        if (keys == null || keys.isEmpty()) return emptySequence(1);
        return withNodeKeys(keys.iterator(), keys.size(), score, workspaceName, cache);
    }

    /**
     * Create a sequence of nodes that iterates over the supplied node keys. Note that the supplied iterator is accessed lazily as
     * the resulting sequence's {@link #nextBatch() first batch} is {@link Batch#nextRow() used}.
     * 
     * @param keys the iterator over the keys of the node keys to be returned; if null, an {@link #emptySequence empty instance}
     *        is returned
     * @param keyCount the number of node keys in the iterator; must be -1 if not known, 0 if known to be empty, or a positive
     *        number if the number of node keys is known
     * @param score the score to return for all of the nodes
     * @param workspaceName the name of the workspace in which all of the nodes exist
     * @param repository the repository cache used to access the workspaces and cached nodes; may be null only if the key sequence
     *        is null or empty
     * @return the sequence of nodes; never null
     */
    public static NodeSequence withNodeKeys( final Iterator<NodeKey> keys,
                                             final long keyCount,
                                             final float score,
                                             final String workspaceName,
                                             final RepositoryCache repository ) {
        assert keyCount >= -1;
        if (keys == null) return emptySequence(1);
        return withBatch(batchOfKeys(keys, keyCount, score, workspaceName, repository));
    }

    /**
     * Create a sequence of nodes that iterates over the supplied node keys. Note that the supplied iterator is accessed lazily as
     * the resulting sequence's {@link #nextBatch() first batch} is {@link Batch#nextRow() used}.
     * 
     * @param keys the iterator over the keys of the node keys to be returned; if null, an {@link #emptySequence empty instance}
     *        is returned
     * @param keyCount the number of node keys in the iterator; must be -1 if not known, 0 if known to be empty, or a positive
     *        number if the number of node keys is known
     * @param score the score to return for all of the nodes
     * @param workspaceName the name of the workspace in which all of the nodes exist
     * @param cache the node cache used to access the cached nodes; may be null only if the key sequence is null or empty
     * @return the sequence of nodes; never null
     */
    public static NodeSequence withNodeKeys( final Iterator<NodeKey> keys,
                                             final long keyCount,
                                             final float score,
                                             final String workspaceName,
                                             final NodeCache cache ) {
        assert keyCount >= -1;
        if (keys == null) return emptySequence(1);
        return withBatch(batchOfKeys(keys, keyCount, score, workspaceName, cache));
    }

    /**
     * Create a sequence of nodes that skips a specified number of nodes before returning any nodes and that limits the number of
     * nodes returned.
     * 
     * @param sequence the original sequence that is to be limited; may be null
     * @param limitAndOffset the specification of the offset and limit; if null this method simply returns <code>sequence</code>
     * @return the limitd sequence of nodes; never null
     */
    public static NodeSequence limit( NodeSequence sequence,
                                      Limit limitAndOffset ) {
        if (sequence == null) return emptySequence(0);
        if (limitAndOffset != null && !limitAndOffset.isUnlimited()) {
            final int limit = limitAndOffset.getRowLimit();
            // Perform the skip first ...
            if (limitAndOffset.isOffset()) {
                sequence = skip(sequence, limitAndOffset.getOffset());
            }
            // And then the offset ...
            if (limit != Integer.MAX_VALUE) {
                sequence = limit(sequence, limit);
            }
        }
        return sequence;
    }

    /**
     * Create a sequence of nodes that returns at most the supplied number of rows.
     * 
     * @param sequence the original sequence that is to be limited; may be null
     * @param maxRows the maximum number of rows that are to be returned by the sequence; should be positive or this method simply
     *        returns <code>sequence</code>
     * @return the sequence of 'maxRows' nodes; never null
     */
    public static NodeSequence limit( final NodeSequence sequence,
                                      final long maxRows ) {
        if (sequence == null) return emptySequence(0);
        if (maxRows <= 0) return emptySequence(sequence.width());
        if (sequence.isEmpty()) return sequence;
        return new NodeSequence() {
            protected long rowsRemaining = maxRows;

            @Override
            public long getRowCount() {
                long count = sequence.getRowCount();
                if (count < 0L) return -1;
                return count < maxRows ? count : maxRows;
            }

            @Override
            public int width() {
                return sequence.width();
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public Batch nextBatch() {
                if (rowsRemaining <= 0) return null;
                final Batch next = sequence.nextBatch();
                if (next == null) return null;
                long size = next.rowCount();
                boolean sizeKnown = false;
                if (size >= 0) {
                    // The size is known ...
                    sizeKnown = true;
                    if (size <= rowsRemaining) {
                        // The whole batch can be returned ...
                        rowsRemaining -= size;
                        return next;
                    }
                    // Only the first part of this batch is okay ...
                    assert size > rowsRemaining;
                    // just continue ...
                }
                // The size is not known or larger than rowsRemaining, so we return a batch that exposes only the number we need
                long limit = rowsRemaining;
                rowsRemaining = 0L;
                return new LimitBatch(next, limit, sizeKnown);
            }

            @Override
            public void close() {
                sequence.close();
            }

            @Override
            public String toString() {
                return "(limit " + maxRows + " " + sequence + " )";
            }
        };
    }

    /**
     * Create a sequence of nodes that skips a specified number of rows before returning any rows.
     * 
     * @param sequence the original sequence that is to be limited; may be null
     * @param skip the number of initial rows that should be skipped; should be positive or this method simply returns
     *        <code>sequence</code>
     * @return the sequence of nodes after the first <code>skip</code> nodes are skipped; never null
     */
    public static NodeSequence skip( final NodeSequence sequence,
                                     final int skip ) {
        if (sequence == null) return emptySequence(0);
        if (skip <= 0 || sequence.isEmpty()) return sequence;
        return new NodeSequence() {
            private int rowsToSkip = skip;

            @Override
            public long getRowCount() {
                long count = sequence.getRowCount();
                if (count < skip) return -1;
                return count == skip ? 0 : count - skip;
            }

            @Override
            public int width() {
                return sequence.width();
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public Batch nextBatch() {
                Batch next = sequence.nextBatch();
                while (next != null) {
                    if (rowsToSkip <= 0) return next;
                    long size = next.rowCount();
                    if (size >= 0) {
                        // The size of this batch is known ...
                        if (size == 0) {
                            // but it is empty, so just skip this batch altogether ...
                            next = sequence.nextBatch();
                            continue;
                        }
                        if (size <= rowsToSkip) {
                            // The entire batch is smaller than the number of rows we're skipping, so skip the whole batch ...
                            rowsToSkip -= size;
                            next = sequence.nextBatch();
                            continue;
                        }
                        // Otherwise, we have to skip the first `rowsToSkip` rows in the batch ...
                        for (int i = 0; i != rowsToSkip; ++i) {
                            if (!next.hasNext()) return null;
                            next.nextRow();
                            --size;
                        }
                        rowsToSkip = 0;
                        return new AlternateSizeBatch(next, size);
                    }
                    // Otherwise the size of the batch is not known, so we need to skip the rows individually ...
                    while (rowsToSkip > 0 && next.hasNext()) {
                        next.nextRow();
                        --rowsToSkip;
                    }
                    if (next.hasNext()) return next;
                    // Otherwise, we've used up all of this batch so just continue to the next ...
                    next = sequence.nextBatch();
                }
                return next;
            }

            @Override
            public void close() {
                sequence.close();
            }

            @Override
            public String toString() {
                return "(skip " + skip + " " + sequence + " )";
            }
        };
    }

    protected static class LimitBatch implements Batch {
        private final Batch original;
        private final boolean sizeKnown;
        private final long rowCount;
        private long rowsUsed = 0L;

        protected LimitBatch( Batch original,
                              long rowCount,
                              boolean sizeKnown ) {
            this.original = original;
            this.sizeKnown = sizeKnown;
            this.rowCount = rowCount;
        }

        @Override
        public long rowCount() {
            return sizeKnown ? rowCount : -1L;
        }

        @Override
        public String getWorkspaceName() {
            return original.getWorkspaceName();
        }

        @Override
        public boolean isEmpty() {
            return sizeKnown && rowCount == 0;
        }

        @Override
        public int width() {
            return original.width();
        }

        @Override
        public boolean hasNext() {
            return rowsUsed < rowCount && original.hasNext();
        }

        @Override
        public void nextRow() {
            if (++rowsUsed > rowCount) throw new NoSuchElementException();
            original.nextRow();
        }

        @Override
        public CachedNode getNode() {
            return original.getNode();
        }

        @Override
        public CachedNode getNode( int index ) {
            return original.getNode(index);
        }

        @Override
        public float getScore() {
            return original.getScore();
        }

        @Override
        public float getScore( int index ) {
            return original.getScore(index);
        }

        @Override
        public String toString() {
            return "(limit-batch size=" + rowCount + " " + original + " )";
        }
    }

    protected static class AlternateSizeBatch implements Batch {
        private final Batch original;
        private final long newSize;

        protected AlternateSizeBatch( Batch original,
                                      long newSize ) {
            this.original = original;
            this.newSize = newSize;
        }

        @Override
        public long rowCount() {
            return newSize;
        }

        @Override
        public String getWorkspaceName() {
            return original.getWorkspaceName();
        }

        @Override
        public boolean isEmpty() {
            return newSize == 0;
        }

        @Override
        public int width() {
            return original.width();
        }

        @Override
        public boolean hasNext() {
            return original.hasNext();
        }

        @Override
        public void nextRow() {
            original.nextRow();
        }

        @Override
        public CachedNode getNode() {
            return original.getNode();
        }

        @Override
        public CachedNode getNode( int index ) {
            return original.getNode(index);
        }

        @Override
        public float getScore() {
            return original.getScore();
        }

        @Override
        public float getScore( int index ) {
            return original.getScore(index);
        }

        @Override
        public String toString() {
            return "(batch size=" + newSize + " " + original + " )";
        }
    }

    /**
     * Create a sequence of nodes that all satisfy the supplied filter.
     * 
     * @param sequence the original sequence that is to be limited; may be null
     * @param filter the filter to apply to the nodes; if null this method simply returns <code>sequence</code>
     * @return the sequence of filtered nodes; never null
     */
    public static NodeSequence filter( final NodeSequence sequence,
                                       final RowFilter filter ) {
        if (sequence == null) return emptySequence(0);
        if (filter == null || sequence.isEmpty()) return sequence;
        return new NodeSequence() {

            @Override
            public long getRowCount() {
                // we don't know how the filter affects the row count ...
                return -1;
            }

            @Override
            public int width() {
                return sequence.width();
            }

            @Override
            public boolean isEmpty() {
                // not known to be empty, so always return false ...
                return false;
            }

            @Override
            public Batch nextBatch() {
                Batch next = sequence.nextBatch();
                return batchFilteredWith(next, filter);
            }

            @Override
            public void close() {
                sequence.close();
            }

            @Override
            public String toString() {
                return "(filtered " + filter + " " + sequence + ")";
            }
        };
    }

    /**
     * Create a sequence of nodes that contains the nodes from the first sequence followed by the second sequence.
     * 
     * @param first the first sequence; may be null
     * @param second the second sequence; may be null
     * @return the new combined sequence; never null
     * @throws IllegalArgumentException if the sequences have different widths
     */
    public static NodeSequence append( final NodeSequence first,
                                       final NodeSequence second ) {
        if (first == null) {
            return second != null ? second : emptySequence(0);
        }
        if (second == null) return first;
        int firstWidth = first.width();
        final int secondWidth = second.width();
        if (firstWidth > 0 && secondWidth > 0 && firstWidth != secondWidth) {
            throw new IllegalArgumentException("The sequences must have the same width: " + first + " and " + second);
        }
        if (first.isEmpty()) return second;
        if (second.isEmpty()) return first;
        long firstCount = first.getRowCount();
        long secondCount = second.getRowCount();
        final long count = firstCount < 0 ? -1 : (secondCount < 0 ? -1 : firstCount + secondCount);
        return new NodeSequence() {
            private NodeSequence current = first;

            @Override
            public int width() {
                return secondWidth;
            }

            @Override
            public long getRowCount() {
                return count;
            }

            @Override
            public boolean isEmpty() {
                return false;
            }

            @Override
            public Batch nextBatch() {
                Batch batch = current.nextBatch();
                while (batch == null && current == first) {
                    current = second;
                    batch = current.nextBatch();
                }
                return batch;
            }

            @Override
            public void close() {
                try {
                    first.close();
                } finally {
                    second.close();
                }
            }

            @Override
            public String toString() {
                return "(append width=" + width() + " " + first + "," + second + " )";
            }
        };
    }

    /**
     * Create a sequence of nodes that merges the two supplied sequences.
     * 
     * @param first the first sequence; may be null
     * @param second the second sequence; may be null
     * @param totalWidth the total width of the sequences; should be equal to <code>first.getWidth() + second.getWidth()</code>
     * @return the new merged sequence; never null
     */
    public static NodeSequence merging( final NodeSequence first,
                                        final NodeSequence second,
                                        final int totalWidth ) {
        if (first == null) {
            if (second == null) return emptySequence(totalWidth);
            final int firstWidth = totalWidth - second.width();
            return new NodeSequence() {
                @Override
                public int width() {
                    return totalWidth;
                }

                @Override
                public long getRowCount() {
                    return second.getRowCount();
                }

                @Override
                public boolean isEmpty() {
                    return second.isEmpty();
                }

                @Override
                public Batch nextBatch() {
                    return batchOf(null, second.nextBatch(), firstWidth, second.width());
                }

                @Override
                public void close() {
                    second.close();
                }

                @Override
                public String toString() {
                    return "(merge width=" + width() + " (null-sequence)," + second + " )";
                }
            };
        }
        if (second == null) {
            final int secondWidth = totalWidth - first.width();
            return new NodeSequence() {
                @Override
                public int width() {
                    return totalWidth;
                }

                @Override
                public long getRowCount() {
                    return first.getRowCount();
                }

                @Override
                public boolean isEmpty() {
                    return first.isEmpty();
                }

                @Override
                public Batch nextBatch() {
                    return batchOf(first.nextBatch(), null, first.width(), secondWidth);
                }

                @Override
                public void close() {
                    first.close();
                }

                @Override
                public String toString() {
                    return "(merge width=" + width() + " " + first + ",(null-sequence) )";
                }
            };
        }
        final int firstWidth = first.width();
        final int secondWidth = second.width();
        final long rowCount = Math.max(-1, first.getRowCount() + second.getRowCount());
        return new NodeSequence() {
            @Override
            public int width() {
                return totalWidth;
            }

            @Override
            public long getRowCount() {
                return rowCount;
            }

            @Override
            public boolean isEmpty() {
                return rowCount == 0;
            }

            @Override
            public Batch nextBatch() {
                Batch nextA = first.nextBatch();
                Batch nextB = second.nextBatch();
                if (nextA == null) {
                    if (nextB == null) return null;
                    return batchOf(null, nextB, firstWidth, secondWidth);
                }
                return batchOf(nextA, nextB, firstWidth, secondWidth);
            }

            @Override
            public void close() {
                // always do both ...
                try {
                    first.close();
                } finally {
                    second.close();
                }
            }

            @Override
            public String toString() {
                return "(merge width=" + width() + " " + first + "," + second + " )";
            }
        };
    }

    /**
     * Create a batch of nodes around the supplied iterable container. Note that the supplied iterator is accessed lazily only
     * when the batch is {@link Batch#nextRow() used}.
     * 
     * @param nodes the collection of nodes to be returned; if null, an {@link #emptySequence empty instance} is returned
     * @param score the score to return for all of the nodes
     * @param workspaceName the name of the workspace in which all of the nodes exist
     * @return the batch of nodes; never null
     */
    public static Batch batchOf( final Collection<CachedNode> nodes,
                                 final float score,
                                 final String workspaceName ) {
        if (nodes == null) return emptyBatch(workspaceName, 1);
        return batchOf(nodes.iterator(), nodes.size(), score, workspaceName);
    }

    /**
     * Create a batch of nodes around the supplied iterator. Note that the supplied iterator is accessed lazily only when the
     * batch is {@link Batch#nextRow() used}.
     * 
     * @param nodes the iterator over the nodes to be returned; if null, an {@link #emptySequence empty instance} is returned
     * @param nodeCount the number of nodes in the iterator; must be -1 if not known, 0 if known to be empty, or a positive number
     *        if the number of nodes is known
     * @param score the score to return for all of the nodes
     * @param workspaceName the name of the workspace in which all of the nodes exist
     * @return the batch of nodes; never null
     */
    public static Batch batchOf( final Iterator<CachedNode> nodes,
                                 final long nodeCount,
                                 final float score,
                                 final String workspaceName ) {
        assert nodeCount >= -1;
        if (nodes == null) return emptyBatch(workspaceName, 1);
        return new Batch() {
            private CachedNode current;

            @Override
            public int width() {
                return 1;
            }

            @Override
            public long rowCount() {
                return nodeCount;
            }

            @Override
            public boolean isEmpty() {
                return nodeCount == 0;
            }

            @Override
            public String getWorkspaceName() {
                return workspaceName;
            }

            @Override
            public boolean hasNext() {
                return nodes.hasNext();
            }

            @Override
            public void nextRow() {
                current = nodes.next();
            }

            @Override
            public CachedNode getNode() {
                return current;
            }

            @Override
            public CachedNode getNode( int index ) {
                if (index != 0) throw new IndexOutOfBoundsException();
                return current;
            }

            @Override
            public float getScore() {
                return score;
            }

            @Override
            public float getScore( int index ) {
                if (index != 0) throw new IndexOutOfBoundsException();
                return score;
            }

            @Override
            public String toString() {
                return "(batch node-count=" + rowCount() + " score=" + getScore() + " )";
            }
        };
    }

    /**
     * Create a batch of nodes around the supplied iterable container. Note that the supplied iterator is accessed lazily only
     * when the batch is {@link Batch#nextRow() used}.
     * 
     * @param keys the iterator over the keys of the nodes to be returned; if null, an {@link #emptySequence empty instance} is
     *        returned
     * @param score the score to return for all of the nodes
     * @param workspaceName the name of the workspace in which all of the nodes exist
     * @param repository the repository cache used to access the workspaces and cached nodes; may be null only if the key sequence
     *        is null or empty
     * @return the batch of nodes; never null
     */
    public static Batch batchOfKeys( final Collection<NodeKey> keys,
                                     final float score,
                                     final String workspaceName,
                                     final RepositoryCache repository ) {
        if (keys == null) return emptyBatch(workspaceName, 1);
        return batchOfKeys(keys.iterator(), keys.size(), score, workspaceName, repository);
    }

    /**
     * Create a batch of nodes around the supplied iterator. Note that the supplied iterator is accessed lazily only when the
     * batch is {@link Batch#nextRow() used}.
     * 
     * @param keys the iterator over the keys of the nodes to be returned; if null, an {@link #emptySequence empty instance} is
     *        returned
     * @param nodeCount the number of nodes in the iterator; must be -1 if not known, 0 if known to be empty, or a positive number
     *        if the number of nodes is known
     * @param score the score to return for all of the nodes
     * @param workspaceName the name of the workspace in which all of the nodes exist
     * @param cache the workspace cache used to access the cached nodes; may be null only if the key sequence is null or empty
     * @return the batch of nodes; never null
     */
    public static Batch batchOfKeys( final Iterator<NodeKey> keys,
                                     final long nodeCount,
                                     final float score,
                                     final String workspaceName,
                                     final NodeCache cache ) {
        assert nodeCount >= -1;
        if (keys == null) return emptyBatch(workspaceName, 1);
        return new Batch() {
            private CachedNode current;

            @Override
            public int width() {
                return 1;
            }

            @Override
            public long rowCount() {
                return nodeCount;
            }

            @Override
            public boolean isEmpty() {
                return nodeCount == 0;
            }

            @Override
            public String getWorkspaceName() {
                return workspaceName;
            }

            @Override
            public boolean hasNext() {
                return keys.hasNext();
            }

            @Override
            public void nextRow() {
                NodeKey key = keys.next();
                current = cache.getNode(key);
            }

            @Override
            public CachedNode getNode() {
                return current;
            }

            @Override
            public CachedNode getNode( int index ) {
                if (index != 0) {
                    throw new IndexOutOfBoundsException();
                }
                return current;
            }

            @Override
            public float getScore() {
                return score;
            }

            @Override
            public float getScore( int index ) {
                if (index != 0) throw new IndexOutOfBoundsException();
                return score;
            }

            @Override
            public String toString() {
                return "(batch key-count=" + rowCount() + " score=" + getScore() + " " + keys + ")";
            }
        };
    }

    /**
     * Create a batch of nodes around the supplied iterator. Note that the supplied iterator is accessed lazily only when the
     * batch is {@link Batch#nextRow() used}.
     * 
     * @param keys the iterator over the keys of the nodes to be returned; if null, an {@link #emptySequence empty instance} is
     *        returned
     * @param nodeCount the number of nodes in the iterator; must be -1 if not known, 0 if known to be empty, or a positive number
     *        if the number of nodes is known
     * @param score the score to return for all of the nodes
     * @param workspaceName the name of the workspace in which all of the nodes exist
     * @param repository the repository cache used to access the workspaces and cached nodes; may be null only if the key sequence
     *        is null or empty
     * @return the batch of nodes; never null
     */
    public static Batch batchOfKeys( final Iterator<NodeKey> keys,
                                     final long nodeCount,
                                     final float score,
                                     final String workspaceName,
                                     final RepositoryCache repository ) {
        assert nodeCount >= -1;
        if (keys == null) return emptyBatch(workspaceName, 1);
        final NodeCache cache = repository.getWorkspaceCache(workspaceName);
        return batchOfKeys(keys, nodeCount, score, workspaceName, cache);
    }

    /**
     * Create a batch of nodes around the supplied iterator and the scores iterator. Note that the supplied iterators are accessed
     * lazily only when the batch is {@link Batch#nextRow() used}.
     * 
     * @param keys the iterator over the keys of the nodes to be returned; if null, an {@link #emptySequence empty instance} is
     *        returned
     * @param scores the iterator over the scores of the nodes; must return the same number of values as nodes returned by the
     *        <code>keys</code> iterator
     * @param nodeCount the number of nodes in the iterator; must be -1 if not known, 0 if known to be empty, or a positive number
     *        if the number of nodes is known
     * @param workspaceName the name of the workspace in which all of the nodes exist
     * @param repository the repository cache used to access the workspaces and cached nodes; may be null only if the key sequence
     *        is null or empty
     * @return the batch of nodes; never null
     */
    public static Batch batchOfKeys( final Iterator<NodeKey> keys,
                                     final Iterator<Float> scores,
                                     final long nodeCount,
                                     final String workspaceName,
                                     final RepositoryCache repository ) {
        assert nodeCount >= -1;
        if (keys == null) return emptyBatch(workspaceName, 1);
        final NodeCache cache = repository.getWorkspaceCache(workspaceName);
        return new Batch() {
            private CachedNode current;
            private float score;

            @Override
            public int width() {
                return 1;
            }

            @Override
            public long rowCount() {
                return nodeCount;
            }

            @Override
            public boolean isEmpty() {
                return nodeCount == 0;
            }

            @Override
            public String getWorkspaceName() {
                return workspaceName;
            }

            @Override
            public boolean hasNext() {
                return keys.hasNext();
            }

            @Override
            public void nextRow() {
                NodeKey key = keys.next();
                current = cache.getNode(key);
                Float score = scores.next();
                this.score = score != null ? score.floatValue() : 1.0f;
            }

            @Override
            public CachedNode getNode() {
                return current;
            }

            @Override
            public CachedNode getNode( int index ) {
                if (index != 0) throw new IndexOutOfBoundsException();
                return current;
            }

            @Override
            public float getScore() {
                return score;
            }

            @Override
            public float getScore( int index ) {
                if (index != 0) throw new IndexOutOfBoundsException();
                return score;
            }

            @Override
            public String toString() {
                return "(batch key-count=" + rowCount() + " score=" + getScore() + " )";
            }
        };
    }

    protected static Batch batchOf( final Batch first,
                                    final Batch second,
                                    final int firstWidth,
                                    final int secondWidth ) {
        if (first == null && second == null) return null;
        assert first != null || second != null;
        @SuppressWarnings( "null" )
        final String workspaceName = first != null ? first.getWorkspaceName() : second.getWorkspaceName();
        assert first == null || workspaceName.equals(first.getWorkspaceName());
        assert second == null || workspaceName.equals(second.getWorkspaceName());
        assert first == null || first.width() == firstWidth;
        assert second == null || second.width() == secondWidth;

        // Calculate the maximum row count, but handle nulls and when row counts are -1 if unknown ...
        long rowCount = 0L;
        if (first == null) {
            assert second != null; // both are not null per first line
            rowCount = second.rowCount();
        } else if (second == null) {
            rowCount = first.rowCount();
        } else {
            // both are not null ...
            long firstSize = first.rowCount();
            long secondSize = second.rowCount();
            if (firstSize == -1 || secondSize == -1) rowCount = -1;
            else rowCount = Math.max(firstSize, secondSize);
        }
        final long totalRowCount = rowCount;

        return new Batch() {
            private Batch batch1 = first;
            private Batch batch2 = second;
            private final int totalWidth = firstWidth + secondWidth;

            @Override
            public final int width() {
                return totalWidth;
            }

            @Override
            public long rowCount() {
                return totalRowCount;
            }

            @Override
            public boolean isEmpty() {
                return totalRowCount == 0;
            }

            @Override
            public String getWorkspaceName() {
                return workspaceName;
            }

            @Override
            public boolean hasNext() {
                return hasNext1() || hasNext2();
            }

            private boolean hasNext1() {
                if (batch1 == null) return false;
                if (batch1.hasNext()) return true;
                batch1 = null;
                return false;
            }

            private boolean hasNext2() {
                if (batch2 == null) return false;
                if (batch2.hasNext()) return true;
                batch2 = null;
                return false;
            }

            @Override
            public void nextRow() {
                if (hasNext1()) {
                    batch1.nextRow();
                    if (hasNext2()) batch2.nextRow();
                } else {
                    assert batch1 == null;
                    if (hasNext2()) batch2.nextRow();
                    else throw new NoSuchElementException();
                }
            }

            @Override
            public CachedNode getNode() {
                return batch1 != null ? batch1.getNode() : null;
            }

            @Override
            public CachedNode getNode( int index ) {
                if (index > 0 && index < firstWidth) {
                    return batch1 != null ? batch1.getNode(index) : null;
                } else if (index >= firstWidth && index < totalWidth) {
                    return batch2 != null ? batch2.getNode(index - firstWidth) : null;
                }
                throw new IndexOutOfBoundsException();
            }

            @Override
            public float getScore() {
                return batch1 != null ? batch1.getScore() : 0.0f;
            }

            @Override
            public float getScore( int index ) {
                if (index > 0 && index < firstWidth) {
                    return batch1 != null ? batch1.getScore(index) : 0.0f;
                } else if (index >= firstWidth && index < totalWidth) {
                    return batch2 != null ? batch2.getScore(index - firstWidth) : 0.0f;
                }
                throw new IndexOutOfBoundsException();
            }
        };
    }

    /**
     * An operation that extracts a single value from the current row in a given {@link Batch}. Note that a single instance will
     * be called many times (once for each row in the batches of a {@link NodeSequence}).
     * 
     * @author Randall Hauch (rhauch@redhat.com)
     */
    public static interface ExtractFromRow {

        /**
         * Get the type of value that this extrator will return from {@link #getValueInRow}.
         * 
         * @return the type; never null
         */
        TypeFactory<?> getType();

        /**
         * Evaluate the {@link DynamicOperand} against the current row in the supplied batch. Note that implementations will need
         * to know whether to get the {@link Batch#getNode() only node in the row} or how to get a {@link Batch#getNode(int)} for
         * a specific selector.
         * 
         * @param row the row accessor through which the current row values can be obtained; never null
         * @return the dynamic operands value for the current row; may be null
         */
        Object getValueInRow( RowAccessor row );
    }

    public static ExtractFromRow extractFullText( final int indexInRow,
                                                  final NodeCache cache,
                                                  TypeSystem types,
                                                  final BinaryStore binaries ) {
        final TypeFactory<String> type = types.getStringFactory();
        final boolean trace = LOGGER.isTraceEnabled();
        return new ExtractFromRow() {
            @Override
            public TypeFactory<String> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                CachedNode node = row.getNode(indexInRow);
                if (node == null) return null;
                StringBuilder fullTextString = new StringBuilder();
                Name name = node.getName(cache);
                fullTextString.append(name.getLocalName());
                Iterator<Property> iter = node.getProperties(cache);
                while (iter.hasNext()) {
                    extractFullTextFrom(iter.next(), type, fullTextString, binaries, node, cache);
                }
                if (trace) LOGGER.trace("Extracting full-text from {0}: {1}", node.getPath(cache), fullTextString);
                // There should always be some content, since every node except the root has a name and even the
                // root node has some properties that will be converted to full-text ...
                return fullTextString.toString();
            }

            @Override
            public String toString() {
                return "(extract-full-text)";
            }
        };
    }

    public static ExtractFromRow extractFullText( final int indexInRow,
                                                  final NodeCache cache,
                                                  final Name propertyName,
                                                  TypeSystem types,
                                                  final BinaryStore binaries ) {
        final TypeFactory<String> type = types.getStringFactory();
        return new ExtractFromRow() {
            @Override
            public TypeFactory<String> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                CachedNode node = row.getNode(indexInRow);
                if (node == null) return null;
                Property prop = node.getProperty(propertyName, cache);
                if (prop == null || prop.isEmpty()) return null;
                StringBuilder fullTextString = new StringBuilder();
                extractFullTextFrom(prop, type, fullTextString, binaries, node, cache);
                return fullTextString.toString();
            }

            @Override
            public String toString() {
                return "(extract-full-text property=" + type.create(propertyName) + ")";
            }
        };
    }

    protected static void extractFullTextFrom( Property property,
                                               TypeFactory<String> type,
                                               StringBuilder fullTextString,
                                               BinaryStore binaries,
                                               CachedNode node,
                                               NodeCache cache ) {
        for (Object value : property) {
            extractFullTextFrom(value, type, binaries, fullTextString);
        }
    }

    public static void extractFullTextFrom( Object propertyValue,
                                            TypeFactory<String> type,
                                            BinaryStore binaries,
                                            StringBuilder fullTextString ) {
        if (propertyValue instanceof Binary && binaries != null) {
            // Try extracting the text from the binary value ...
            try {
                propertyValue = binaries.getText((BinaryValue)propertyValue);
            } catch (BinaryStoreException e) {
                LOGGER.debug("Error getting full text from binary {0}", propertyValue);
            }
        }
        if (propertyValue != null) {
            // Convert all other types to a string ...
            fullTextString.append(' ').append(type.create(propertyValue));
        }
    }

    /**
     * Create an extractor that extracts the {@link NodeKey} from the node at the given position in the row.
     * 
     * @param indexInRow the index of the node in the rows; must be valid
     * @param cache the cache containing the nodes; may not be null
     * @param types the type system; may not be null
     * @return the node key extractor; never null
     */
    public static ExtractFromRow extractNodeKey( final int indexInRow,
                                                 final NodeCache cache,
                                                 TypeSystem types ) {
        final TypeFactory<String> type = types.getStringFactory();
        final boolean trace = LOGGER.isTraceEnabled();
        return new ExtractFromRow() {
            @Override
            public TypeFactory<String> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                CachedNode node = row.getNode(indexInRow);
                if (node == null) return null;
                if (trace) LOGGER.trace("Extracting node key from {0}: {1}", node.getPath(cache), node.getKey());
                return node.getKey().toString();
            }

            @Override
            public String toString() {
                return "(extract-node-key)";
            }
        };
    }

    /**
     * Create an extractor that extracts the parent's {@link NodeKey} from the node at the given position in the row.
     * 
     * @param indexInRow the index of the node in the rows; must be valid
     * @param cache the cache containing the nodes; may not be null
     * @param types the type system; may not be null
     * @return the node key extractor; never null
     */
    public static ExtractFromRow extractParentNodeKey( final int indexInRow,
                                                       final NodeCache cache,
                                                       TypeSystem types ) {
        final TypeFactory<String> type = types.getStringFactory();
        final boolean trace = LOGGER.isTraceEnabled();
        return new ExtractFromRow() {
            @Override
            public TypeFactory<String> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                CachedNode node = row.getNode(indexInRow);
                if (node == null) return null;
                NodeKey parentKey = node.getParentKey(cache);
                if (trace) LOGGER.trace("Extracting parent key from {0}: {1}", node.getPath(cache), parentKey);
                return parentKey != null ? parentKey.toString() : null;
            }

            @Override
            public String toString() {
                return "(extract-parent-key)";
            }
        };
    }

    /**
     * Create an extractor that extracts the path from the node at the given position in the row.
     * 
     * @param indexInRow the index of the node in the rows; must be valid
     * @param cache the cache containing the nodes; may not be null
     * @param types the type system; may not be null
     * @return the path extractor; never null
     */
    public static ExtractFromRow extractPath( final int indexInRow,
                                              final NodeCache cache,
                                              TypeSystem types ) {
        final TypeFactory<Path> type = types.getPathFactory();
        final boolean trace = LOGGER.isTraceEnabled();
        return new ExtractFromRow() {
            @Override
            public TypeFactory<Path> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                CachedNode node = row.getNode(indexInRow);
                if (node == null) return null;
                Path path = node.getPath(cache);
                if (trace) LOGGER.trace("Extracting path from {0}", path);
                return path;
            }

            @Override
            public String toString() {
                return "(extract-path)";
            }
        };
    }

    /**
     * Create an extractor that extracts the parent path from the node at the given position in the row.
     * 
     * @param indexInRow the index of the node in the rows; must be valid
     * @param cache the cache containing the nodes; may not be null
     * @param types the type system; may not be null
     * @return the path extractor; never null
     */
    public static ExtractFromRow extractParentPath( final int indexInRow,
                                                    final NodeCache cache,
                                                    TypeSystem types ) {
        final TypeFactory<Path> type = types.getPathFactory();
        final boolean trace = LOGGER.isTraceEnabled();
        return new ExtractFromRow() {
            @Override
            public TypeFactory<Path> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                CachedNode node = row.getNode(indexInRow);
                if (node == null) return null;
                NodeKey parentKey = node.getParentKey(cache);
                if (parentKey == null) return null;
                CachedNode parent = cache.getNode(parentKey);
                if (parent == null) return null;
                Path parentPath = parent.getPath(cache);
                if (trace) LOGGER.trace("Extracting parent path from {0}: {1}", node.getPath(cache), parentPath);
                return parentPath;
            }

            @Override
            public String toString() {
                return "(extract-parent-path)";
            }
        };
    }

    /**
     * Create an extractor that extracts the path from the node at the given position in the row and applies the relative path.
     * 
     * @param indexInRow the index of the node in the rows; must be valid
     * @param relativePath the relative path that should be applied to the nodes' path; may not be null and must be a valid
     *        relative path
     * @param cache the cache containing the nodes; may not be null
     * @param types the type system; may not be null
     * @return the path extractor; never null
     */
    public static ExtractFromRow extractRelativePath( final int indexInRow,
                                                      final Path relativePath,
                                                      final NodeCache cache,
                                                      TypeSystem types ) {
        CheckArg.isNotNull(relativePath, "relativePath");
        final TypeFactory<Path> type = types.getPathFactory();
        final boolean trace = LOGGER.isTraceEnabled();
        return new ExtractFromRow() {
            @Override
            public TypeFactory<Path> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                CachedNode node = row.getNode(indexInRow);
                if (node == null) return null;
                Path nodePath = node.getPath(cache);
                try {
                    Path path = nodePath.resolve(relativePath);
                    if (trace) LOGGER.trace("Extracting relative path {2} from {0}: {2}", node.getPath(cache), relativePath, path);
                    return path;
                } catch (InvalidPathException e) {
                    return null;
                }
            }

            @Override
            public String toString() {
                return "(extract-relative-path)";
            }
        };
    }

    /**
     * Create an extractor that extracts the name from the node at the given position in the row.
     * 
     * @param indexInRow the index of the node in the rows; must be valid
     * @param cache the cache containing the nodes; may not be null
     * @param types the type system; may not be null
     * @return the path extractor; never null
     */
    public static ExtractFromRow extractName( final int indexInRow,
                                              final NodeCache cache,
                                              TypeSystem types ) {
        final TypeFactory<Name> type = types.getNameFactory();
        final boolean trace = LOGGER.isTraceEnabled();
        return new ExtractFromRow() {
            @Override
            public TypeFactory<Name> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                CachedNode node = row.getNode(indexInRow);
                if (node == null) return null;
                Name name = node.getName(cache);
                if (trace) LOGGER.trace("Extracting name from {0}: {1}", node.getPath(cache), name);
                return name;
            }

            @Override
            public String toString() {
                return "(extract-name)";
            }
        };
    }

    /**
     * Create an extractor that extracts the name from the node at the given position in the row.
     * 
     * @param indexInRow the index of the node in the rows; must be valid
     * @param cache the cache containing the nodes; may not be null
     * @param types the type system; may not be null
     * @return the path extractor; never null
     */
    public static ExtractFromRow extractLocalName( final int indexInRow,
                                                   final NodeCache cache,
                                                   TypeSystem types ) {
        final TypeFactory<String> type = types.getStringFactory();
        final boolean trace = LOGGER.isTraceEnabled();
        return new ExtractFromRow() {
            @Override
            public TypeFactory<String> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                CachedNode node = row.getNode(indexInRow);
                String name = node.getName(cache).getLocalName();
                if (trace) LOGGER.trace("Extracting name from {0}: {1}", node.getPath(cache), name);
                return name;
            }

            @Override
            public String toString() {
                return "(extract-local-name)";
            }
        };
    }

    /**
     * Create an extractor that extracts an object that uniquely identifies the row. The object will be either a {@link NodeKey}
     * (if rowWidth is 1), or a {@link Tuples tuple} containing each of the {@link NodeKey}s of the nodes in the row. Note that
     * any of the node keys can be null, but this extractor will never return a null object.
     * 
     * @param rowWidth the number of nodes in each row; must be positive
     * @param types the types system; may not be null
     * @return the extractor; never null
     */
    public static ExtractFromRow extractUniqueKey( final int rowWidth,
                                                   TypeSystem types ) {
        final TypeFactory<Reference> keyType = types.getReferenceFactory();
        assert rowWidth > 0;
        if (rowWidth == 1) {
            return new ExtractFromRow() {
                @Override
                public TypeFactory<Reference> getType() {
                    return keyType;
                }

                @Override
                public Object getValueInRow( RowAccessor row ) {
                    return keyFor(row.getNode());
                }

                @Override
                public String toString() {
                    return "(extract-key-for-order-1-tuples)";
                }
            };
        }
        if (rowWidth == 2) {
            final TypeFactory<?> tupleType = Tuples.typeFactory(keyType, keyType);
            return new ExtractFromRow() {
                @Override
                public TypeFactory<?> getType() {
                    return tupleType;
                }

                @Override
                public Object getValueInRow( RowAccessor row ) {
                    return Tuples.tuple(keyFor(row.getNode(0)), keyFor(row.getNode(1)));
                }

                @Override
                public String toString() {
                    return "(extract-key-for-order-2-tuples)";
                }
            };
        }
        if (rowWidth == 3) {
            final TypeFactory<?> tupleType = Tuples.typeFactory(keyType, keyType, keyType);
            return new ExtractFromRow() {
                @Override
                public TypeFactory<?> getType() {
                    return tupleType;
                }

                @Override
                public Object getValueInRow( RowAccessor row ) {
                    return Tuples.tuple(keyFor(row.getNode(0)), keyFor(row.getNode(1)), keyFor(row.getNode(2)));
                }

                @Override
                public String toString() {
                    return "(extract-key-for-order-3-tuples)";
                }
            };
        }
        if (rowWidth == 4) {
            final TypeFactory<?> tupleType = Tuples.typeFactory(keyType, keyType, keyType, keyType);
            return new ExtractFromRow() {
                @Override
                public TypeFactory<?> getType() {
                    return tupleType;
                }

                @Override
                public Object getValueInRow( RowAccessor row ) {
                    return Tuples.tuple(keyFor(row.getNode(0)), keyFor(row.getNode(1)), keyFor(row.getNode(2)),
                                        keyFor(row.getNode(3)));
                }

                @Override
                public String toString() {
                    return "(extract-key-for-order-4-tuples)";
                }
            };
        }
        final TypeFactory<?> tupleType = Tuples.typeFactory(keyType, rowWidth);
        return new ExtractFromRow() {
            @Override
            public TypeFactory<?> getType() {
                return tupleType;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                Object[] keys = new Object[rowWidth];
                for (int i = 0; i != rowWidth; ++i) {
                    keys[i] = keyFor(row.getNode(i));
                }
                return Tuples.tuple(keys);
            }

            @Override
            public String toString() {
                return "(extract-key-for-order-N-tuples)";
            }
        };
    }

    /**
     * Create an extractor that extracts the property value from the node at the given position in the row.
     * 
     * @param propertyName the name of the property; may not be null
     * @param indexInRow the index of the node in the rows; must be valid
     * @param cache the cache containing the nodes; may not be null
     * @param desiredType the desired type, which should be converted from the actual value; may not be null
     * @return the path extractor; never null
     */
    public static ExtractFromRow extractPropertyValue( final Name propertyName,
                                                       final int indexInRow,
                                                       final NodeCache cache,
                                                       final TypeFactory<?> desiredType ) {
        return new ExtractFromRow() {
            @Override
            public Object getValueInRow( RowAccessor row ) {
                CachedNode node = row.getNode(indexInRow);
                if (node == null) return null;
                org.modeshape.jcr.value.Property prop = node.getProperty(propertyName, cache);
                return prop == null ? null : desiredType.create(prop.getFirstValue());
            }

            @Override
            public TypeFactory<?> getType() {
                return desiredType;
            }
        };
    }

    protected static final NodeKey keyFor( CachedNode node ) {
        return node != null ? node.getKey() : null;
    }

    /**
     * Obtain an extractor of a tuple containing each of the values from the supplied extractors.
     * 
     * @param first the first extractor; may not be null
     * @param second the second extractor; may not be null
     * @return the tuple extractor
     */
    public static ExtractFromRow extractorWith( final ExtractFromRow first,
                                                final ExtractFromRow second ) {
        final TypeFactory<?> type = Tuples.typeFactory(first.getType(), second.getType());
        return new ExtractFromRow() {
            @Override
            public TypeFactory<?> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                return Tuples.tuple(first.getValueInRow(row), second.getValueInRow(row));
            }
        };
    }

    /**
     * Obtain an extractor of a tuple containing each of the values from the supplied extractors.
     * 
     * @param first the first extractor; may not be null
     * @param second the second extractor; may not be null
     * @param third the third extractor; may not be null
     * @return the tuple extractor
     */
    public static ExtractFromRow extractorWith( final ExtractFromRow first,
                                                final ExtractFromRow second,
                                                final ExtractFromRow third ) {
        final TypeFactory<?> type = Tuples.typeFactory(first.getType(), second.getType(), third.getType());
        return new ExtractFromRow() {
            @Override
            public TypeFactory<?> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                return Tuples.tuple(first.getValueInRow(row), second.getValueInRow(row), third.getValueInRow(row));
            }
        };
    }

    /**
     * Obtain an extractor of a tuple containing each of the values from the supplied extractors.
     * 
     * @param first the first extractor; may not be null
     * @param second the second extractor; may not be null
     * @param third the third extractor; may not be null
     * @param fourth the fourth extractor; may not be null
     * @return the tuple extractor
     */
    public static ExtractFromRow extractorWith( final ExtractFromRow first,
                                                final ExtractFromRow second,
                                                final ExtractFromRow third,
                                                final ExtractFromRow fourth ) {
        final TypeFactory<?> type = Tuples.typeFactory(first.getType(), second.getType(), third.getType(), fourth.getType());
        return new ExtractFromRow() {
            @Override
            public TypeFactory<?> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                return Tuples.tuple(first.getValueInRow(row), second.getValueInRow(row), third.getValueInRow(row),
                                    fourth.getValueInRow(row));
            }
        };
    }

    /**
     * Obtain an extractor of a tuple containing each of the values from the supplied extractors.
     * 
     * @param extractors the extractors; may not be null or empty
     * @return the tuple extractor
     */
    public static ExtractFromRow extractorWith( final Collection<ExtractFromRow> extractors ) {
        final int len = extractors.size();
        assert len > 0;
        if (len == 1) {
            return extractors.iterator().next();
        }
        if (len == 2) {
            Iterator<ExtractFromRow> iter = extractors.iterator();
            ExtractFromRow first = iter.next();
            ExtractFromRow second = iter.next();
            return extractorWith(first, second);
        }
        if (len == 3) {
            Iterator<ExtractFromRow> iter = extractors.iterator();
            ExtractFromRow first = iter.next();
            ExtractFromRow second = iter.next();
            ExtractFromRow third = iter.next();
            return extractorWith(first, second, third);
        }
        if (len == 4) {
            Iterator<ExtractFromRow> iter = extractors.iterator();
            ExtractFromRow first = iter.next();
            ExtractFromRow second = iter.next();
            ExtractFromRow third = iter.next();
            ExtractFromRow fourth = iter.next();
            return extractorWith(first, second, third, fourth);
        }
        Collection<TypeFactory<?>> types = new ArrayList<TypeFactory<?>>();
        final ExtractFromRow[] extracts = new ExtractFromRow[len];
        int i = 0;
        for (ExtractFromRow extractor : extractors) {
            extracts[i++] = extractor;
            types.add(extractor.getType());
        }
        final TypeFactory<?> type = Tuples.typeFactory(types);
        return new ExtractFromRow() {
            @Override
            public TypeFactory<?> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                Object[] values = new Object[len];
                for (int i = 0; i != len; ++i) {
                    values[i] = extracts[i].getValueInRow(row);
                }
                return Tuples.tuple(values);
            }
        };
    }

    /**
     * Create an extractor that has a {@link ExtractFromRow#getType() type factory} with a {@link TypeFactory#getComparator()
     * comparator} that sorts according to the specified order and null-order behavior.
     * 
     * @param extractor the original extractor; may not be null
     * @param order the specification of whether the comparator should order ascending or descending; may not be null
     * @param nullOrder the specification of whether null values should appear first or last; may not be null
     * @return the extractor that is identical except that it has an inverted comparator; never null
     */
    public static ExtractFromRow extractorWith( final ExtractFromRow extractor,
                                                final Order order,
                                                final NullOrder nullOrder ) {
        final TypeFactory<?> type = TypeSystem.with(extractor.getType(), order, nullOrder);
        return new ExtractFromRow() {
            @Override
            public TypeFactory<?> getType() {
                return type;
            }

            @Override
            public Object getValueInRow( RowAccessor row ) {
                return extractor.getValueInRow(row);
            }

            @Override
            public String toString() {
                return extractor.toString() + " " + order + " " + nullOrder;
            }
        };
    }

    /**
     * A filter of rows.
     * 
     * @author Randall Hauch (rhauch@redhat.com)
     */
    public static interface RowFilter {
        /**
         * Determine if the current row in the given batch satisfies the filter and should be included in the results.
         * <p>
         * Implementations should <i>never</i> call {@link Batch#hasNext()} or {@link Batch#nextRow()}.
         * </p>
         * 
         * @param batch the batch that is at a valid row
         * @return true if the current row is acceptable, or false if it should be excluded
         */
        boolean isCurrentRowValid( Batch batch );
    }

    public static final RowFilter NO_PASS_ROW_FILTER = new RowFilter() {
        @Override
        public boolean isCurrentRowValid( Batch batch ) {
            return false;
        }

        @Override
        public String toString() {
            return "(no-pass-filter)";
        }
    };

    public static final RowFilter PASS_ROW_FILTER = new RowFilter() {
        @Override
        public boolean isCurrentRowValid( Batch batch ) {
            return true;
        }

        @Override
        public String toString() {
            return "(pass-filter)";
        }
    };

    public static RowFilter requireBoth( final RowFilter first,
                                         final RowFilter second ) {
        if (first == null) return second == null ? NO_PASS_ROW_FILTER : second;
        if (second == null) return first;
        return new RowFilter() {
            @Override
            public boolean isCurrentRowValid( Batch batch ) {
                return first.isCurrentRowValid(batch) && second.isCurrentRowValid(batch);
            }
        };
    }

    public static RowFilter requireEither( final RowFilter first,
                                           final RowFilter second ) {
        if (first == null) return second == null ? NO_PASS_ROW_FILTER : second;
        if (second == null) return first;
        return new RowFilter() {
            @Override
            public boolean isCurrentRowValid( Batch batch ) {
                return first.isCurrentRowValid(batch) || second.isCurrentRowValid(batch);
            }
        };
    }

    /**
     * Create a batch that applies the given filter to the supplied batch. Only rows that satisfy the filter will be exposed by
     * this batch. Note that this batch maintains nor caches any rows, and walks the supplied batch as needed (rather than up
     * front). Therefore, the {@link #getRowCount()} is almost always -1 (except when the supplied batch is null or empty, or if
     * the filter is null.
     * 
     * @param batch the batch to be filtered; if null an empty batch is returned
     * @param filter the filter to be applied to the rows in the batch; if null an empty batch is returned
     * @return the filtered batch, or an empty batch if the supplied batch is null or empty, or if the supplied filter is null
     */
    public static Batch batchFilteredWith( final Batch batch,
                                           final RowFilter filter ) {
        if (batch == null || batch.isEmpty() || batch.rowCount() == 0 || filter == null || batch.width() < 1) return batch;
        return new Batch() {
            private boolean atNext = false;

            @Override
            public int width() {
                return batch.width();
            }

            @Override
            public boolean isEmpty() {
                return batch.isEmpty();
            }

            @Override
            public String getWorkspaceName() {
                return batch.getWorkspaceName();
            }

            @Override
            public long rowCount() {
                return -1L;
            }

            @Override
            public CachedNode getNode() {
                return batch.getNode();
            }

            @Override
            public CachedNode getNode( int index ) {
                return batch.getNode(index);
            }

            @Override
            public float getScore() {
                return batch.getScore();
            }

            @Override
            public float getScore( int index ) {
                return batch.getScore(index);
            }

            @Override
            public boolean hasNext() {
                return findNext();
            }

            @Override
            public void nextRow() {
                if (findNext()) {
                    atNext = false;
                    return;
                }
                throw new NoSuchElementException();
            }

            private boolean findNext() {
                if (!atNext) {
                    while (batch.hasNext()) {
                        batch.nextRow();
                        // See if the batch's current row satisfies the filter ...
                        if (filter.isCurrentRowValid(batch)) {
                            atNext = true;
                            break;
                        }
                    }
                }
                return atNext;
            }

            @Override
            public String toString() {
                return "(filtered-batch " + filter + " on " + batch + ")";
            }
        };
    }

    /**
     * Create a batch that always has a {@link Batch#rowCount()}, even if that means returning a new Batch that buffers the
     * original's rows it into memory.
     * 
     * @param batch the original batch; may be null
     * @return the batch that has a true {@link Batch#rowCount()}, or the original batch if null or empty or if the original has a
     *         non-negative row count
     */
    public static Batch batchWithCount( Batch batch ) {
        if (batch == null || batch.isEmpty() || batch.rowCount() >= 0 || batch.width() < 1) return batch;
        // Otherwise, the batch doesn't know it's row count, so we have to copy it ...
        return batch.width() == 1 ? new SingleWidthBatch(batch) : new MultiWidthBatch(batch);
    }

    /**
     * Create a copy of a batch that always has a {@link Batch#rowCount()}.
     * 
     * @param batch the original batch; may be null
     * @return the batch that has a true {@link Batch#rowCount()}, or the original batch if null or empty or if the original has a
     *         non-negative row count
     */
    public static Batch copy( Batch batch ) {
        if (batch == null) return batch;
        if (batch.isEmpty() || batch.width() < 1) return emptyBatch(batch.getWorkspaceName(), 1);
        // Otherwise, create a copy ...
        return batch.width() == 1 ? new SingleWidthBatch(batch) : new MultiWidthBatch(batch);
    }

    protected static class SingleWidthBatch implements Batch, Restartable {
        private final Batch original;
        protected final List<CachedNode> nodes = new ArrayList<>();
        protected final List<Float> scores = new ArrayList<>();
        protected int rowNumber = -1;
        protected final long rowCount;

        protected SingleWidthBatch( Batch batch ) {
            this.original = batch;
            long count = 0L;
            while (original.hasNext()) {
                original.nextRow();
                addRow(original);
                ++count;
            }
            rowCount = count;
        }

        @Override
        public void restart() {
            rowNumber = -1;
        }

        protected void addRow( Batch batch ) {
            nodes.add(original.getNode());
            scores.add(original.getScore());
        }

        @Override
        public String getWorkspaceName() {
            return original.getWorkspaceName();
        }

        @Override
        public int width() {
            return original.width();
        }

        @Override
        public boolean isEmpty() {
            return nodes.isEmpty();
        }

        @Override
        public long rowCount() {
            return rowCount;
        }

        @Override
        public boolean hasNext() {
            return (rowNumber + 1) < rowCount;
        }

        @Override
        public void nextRow() {
            ++rowNumber;
        }

        @Override
        public CachedNode getNode() {
            try {
                return nodes.get(nodeIndex(rowNumber, 0));
            } catch (IndexOutOfBoundsException e) {
                throw new NoSuchElementException();
            }
        }

        @Override
        public CachedNode getNode( int index ) {
            if (index < 0 || index >= width()) throw new NoSuchElementException();
            return nodes.get(nodeIndex(rowNumber, index));
        }

        @Override
        public float getScore() {
            return scores.get(nodeIndex(rowNumber, 0));
        }

        @Override
        public float getScore( int index ) {
            if (index < 0 || index >= width()) throw new NoSuchElementException();
            return scores.get(nodeIndex(rowNumber, index));
        }

        protected int nodeIndex( int rowNumber,
                                 int positionInRow ) {
            return rowNumber + positionInRow;
        }

        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i != rowCount; ++i) {
                sb.append('[');
                for (int j = 0; j != width(); ++j) {
                    if (j != 0) sb.append(",");
                    CachedNode node = nodes.get(nodeIndex(i, j));
                    if (node != null) {
                        sb.append(node.getKey());
                    } else {
                        sb.append("null");
                    }
                }
                sb.append(']').append("\n");
            }
            return sb.toString();
        }
    }

    protected static final class MultiWidthBatch extends SingleWidthBatch {
        protected MultiWidthBatch( Batch batch ) {
            super(batch);
        }

        @Override
        protected void addRow( Batch batch ) {
            for (int i = 0; i != width(); ++i) {
                nodes.add(batch.getNode(i));
                scores.add(batch.getScore(i));
            }
        }

        @Override
        public long rowCount() {
            return nodes.size() / width();
        }

        @Override
        protected int nodeIndex( int rowNumber,
                                 int positionInRow ) {
            return rowNumber * width() + positionInRow;
        }
    }
}
