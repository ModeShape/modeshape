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

package org.modeshape.common.collection.ring;

import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.locks.LockSupport;
import org.modeshape.common.collection.ring.GarbageCollectingConsumer.Collectable;
import org.modeshape.common.util.CheckArg;

/**
 * A single-threaded cursor for a ring buffer that ensures it does not pass the slowest {@link Pointer} that is consuming entries.
 * If the cursor needs to advance but cannot due to a slow {@link Pointer}, then the supplied {@link WaitStrategy strategy} will
 * be used. As new positions are {@link #publish(long) published}, the supplied {@link WaitStrategy} (used by consumers waiting
 * for this cursor to advance) will be {@link WaitStrategy#signalAllWhenBlocking() signalled}.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class SingleProducerCursor implements Cursor {

    private static final AtomicReferenceFieldUpdater<SingleProducerCursor, Pointer[]> STAY_BEHIND_UPDATER = AtomicReferenceFieldUpdater.newUpdater(SingleProducerCursor.class,
                                                                                                                                                   Pointer[].class,
                                                                                                                                                   "stayBehinds");

    private final int bufferSize;
    protected final Pointer current = new Pointer(Pointer.INITIAL_VALUE);
    protected final WaitStrategy waitStrategy;
    private long nextPosition = Pointer.INITIAL_VALUE;
    private long slowestConsumerPosition = Pointer.INITIAL_VALUE;
    protected volatile long finalPosition = Long.MAX_VALUE;
    protected volatile Pointer[] stayBehinds = new Pointer[0];

    public SingleProducerCursor( int bufferSize,
                                 WaitStrategy waitStrategy ) {
        CheckArg.isPositive(bufferSize, "cursor.getBufferSize()");
        CheckArg.isPowerOfTwo(bufferSize, "cursor.getBufferSize()");
        this.bufferSize = bufferSize;
        this.waitStrategy = waitStrategy;
    }

    @Override
    public long getCurrent() {
        return nextPosition;
    }

    @Override
    public int getBufferSize() {
        return bufferSize;
    }

    @Override
    public long claim() {
        return claimUpTo(1);
    }

    @Override
    public long claim( int number ) {
        return claimUpTo(number);
    }

    /**
     * Claim up to the supplied number of positions.
     * 
     * @param number the maximum number of positions to claim for writing; must be positive
     * @return the highest position that were claimed
     */
    protected long claimUpTo( int number ) {
        assert number > 0;
        long nextPosition = this.nextPosition;
        long maxPosition = nextPosition + number;
        long wrapPoint = maxPosition - bufferSize;
        long cachedSlowestConsumerPosition = this.slowestConsumerPosition;

        if (wrapPoint > cachedSlowestConsumerPosition || cachedSlowestConsumerPosition > nextPosition) {
            long minPosition;
            while (wrapPoint > (minPosition = positionOfSlowestPointer(nextPosition))) {
                // This takes on the order of tens of nanoseconds, so it's a useful activity to pause a bit.
                LockSupport.parkNanos(1L);
                waitStrategy.signalAllWhenBlocking();
            }
            this.slowestConsumerPosition = minPosition;
        }

        this.nextPosition = maxPosition;
        return maxPosition;

    }

    protected long positionOfSlowestPointer( long minimumPosition ) {
        return Pointers.getMinimum(stayBehinds, minimumPosition);
    }

    protected long positionOfSlowestConsumer() {
        return slowestConsumerPosition;
    }
    
    @Override
    public boolean publish( long position ) {
        if (finalPosition != Long.MAX_VALUE) return false;
        current.set(position);
        waitStrategy.signalAllWhenBlocking();
        return true;
    }

    @Override
    public long getHighestPublishedPosition( long lowerPosition,
                                             long upperPosition ) {
        // There is only one producer, so we know that all supplied entries are okay
        return upperPosition;
    }

    @Override
    public PointerBarrier newBarrier() {
        return new PointerBarrier() {
            private boolean closed = false;

            @Override
            public long waitFor( long position ) throws InterruptedException, TimeoutException {
                if (position > finalPosition) {
                    // The consumer is waiting for a position beyond the final position, meaning we're done ...
                    return -1;
                }
                long availableSequence = waitStrategy.waitFor(position, current, current, this);
                if (availableSequence < position) {
                    return availableSequence;
                }
                return getHighestPublishedPosition(position, availableSequence);
            }

            @Override
            public boolean isComplete() {
                return closed || SingleProducerCursor.this.isComplete();
            }

            @Override
            public void close() {
                this.closed = true;
            }
        };
    }

    @Override
    public void signalConsumers() {
        waitStrategy.signalAllWhenBlocking();
    }

    @Override
    public void complete() {
        finalPosition = current.get();
        waitStrategy.signalAllWhenBlocking();
    }

    @Override
    public boolean isComplete() {
        return finalPosition == current.get();
    }

    @Override
    public Pointer newPointer() {
        Pointer result = new Pointer(current.get());
        this.stayBehind(result);
        return result;
    }

    @Override
    public void stayBehind( Pointer... pointers ) {
        Pointers.add(this, STAY_BEHIND_UPDATER, this, pointers);
    }

    @Override
    public boolean ignore( Pointer pointer ) {
        return Pointers.remove(this, STAY_BEHIND_UPDATER, pointer);
    }

    @Override
    public GarbageCollectingConsumer createGarbageCollectingConsumer( Collectable collectable ) {
        return new GarbageCollectingConsumer(this, current, waitStrategy, collectable);
    }
}
