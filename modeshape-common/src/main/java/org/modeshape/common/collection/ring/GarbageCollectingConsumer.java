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

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class GarbageCollectingConsumer implements Runnable, AutoCloseable, DependentOnPointers {

    public static interface Collectable {
        void collect( long position );
    }

    private final Cursor cursor;
    private final PointerBarrier gcBarrier;
    private final Pointer pointer;
    protected final TrailingPointer trailingPointer;
    private final AtomicBoolean runThread = new AtomicBoolean(true);
    private final CountDownLatch stopLatch = new CountDownLatch(1);
    private final Collectable collectable;

    public GarbageCollectingConsumer( final Cursor cursor,
                                      Pointer cursorPointer,
                                      final WaitStrategy waitStrategy,
                                      Collectable collectable ) {
        this.cursor = cursor;
        this.pointer = new Pointer();
        this.trailingPointer = new TrailingPointer(cursorPointer);
        this.cursor.stayBehind(this.pointer);
        this.collectable = collectable;
        // Create the pointer barrier that will be called only when used ...
        this.gcBarrier = new PointerBarrier() {
            private volatile boolean closed = false;

            @Override
            public long waitFor( long position ) throws InterruptedException, TimeoutException {
                if (cursor.isComplete()) {
                    // The cursor is done, so return -1 ...
                    return -1;
                }

                // Use the gcPointer instead ...
                long availableSequence = waitStrategy.waitFor(position, trailingPointer, trailingPointer, this);
                if (availableSequence < position) {
                    return availableSequence;
                }
                return cursor.getHighestPublishedPosition(position, availableSequence);
            }

            @Override
            public boolean isComplete() {
                return closed || cursor.isComplete();
            }

            @Override
            public void close() {
                this.closed = true;
            }
        };
    }

    @Override
    public void run() {
        try {
            while (runThread.get()) {
                long next = pointer.get() + 1L;
                try {
                    // Try to find the next position we can read to ...
                    long maxPosition = gcBarrier.waitFor(next);
                    while (next <= maxPosition) {
                        collectable.collect(next);
                        if (!runThread.get()) return;
                        next = pointer.incrementAndGet() + 1L;
                    }
                    if (maxPosition < 0) {
                        // The buffer has been shutdown and there are no more positions, so we're done ...
                        return;
                    }
                } catch (TimeoutException e) {
                    // It took too long to wait, but just continue ...
                } catch (InterruptedException e) {
                    // The thread was interrupted ...
                    Thread.interrupted();
                    break;
                } catch (RuntimeException e) {
                    // Don't retry this entry, so just advance the pointer and continue ...
                    pointer.incrementAndGet();
                }
            }
        } finally {
            // When we're done, so tell the cursor to ignore our pointer ...
            try {
                cursor.ignore(pointer);
            } finally {
                stopLatch.countDown();
            }
        }
    }

    @Override
    public void close() {
        if (this.runThread.compareAndSet(true, false)) {
            try {
                this.stopLatch.await();
            } catch (InterruptedException e) {
                // do nothing ...
            }
        }
    }

    @Override
    public boolean ignore( Pointer pointer ) {
        return trailingPointer.ignore(pointer);
    }

    @Override
    public void stayBehind( Pointer... pointers ) {
        trailingPointer.stayBehind(pointers);
    }

}
