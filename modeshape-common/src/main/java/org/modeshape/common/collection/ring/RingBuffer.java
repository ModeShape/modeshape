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

import java.util.NoSuchElementException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.modeshape.common.collection.ring.GarbageCollectingConsumer.Collectable;
import org.modeshape.common.util.CheckArg;

/**
 * A circular or "ring" buffer that allows entries supplied by a producer to be easily, quickly, and independently consumed by
 * multiple {@link Consumer consumers}. The design of this ring buffer attempts to eliminate or minimize contention between the
 * different consumers. The ring buffer can be completely lock-free, although by default the consumers of the ring buffer use a
 * {@link WaitStrategy} that blocks if they have processed all available entries and are waiting for more to be added. <h2>
 * Concepts</h2>
 * <p>
 * Conceptually, this buffer consists of a fixed-sized ring of elements; entries are added at the ring's "cursor" while multiple
 * consumers follow behind the cursor processing each of the entries as quickly as they can. Each consumer runs in its own thread,
 * and work toward the cursor at their own pace, independently of all other consumers. Most importantly, every consumer sees the
 * exact same order of entries.
 * </p>
 * <p>
 * When the ring buffer starts out, it is empty and the cursor is at the starting position. As entries are added, the cursor
 * travels around the ring, keeping track of its position and the position of all consumers. The cursor can never "lap" any of the
 * consumers, and this ensures that the consumers see a consistent and ordered set of entries. Typically, consumers are fast
 * enough that they trail relatively closely behind the cursor; plus, ring buffers are usually sized large enough so that the
 * cursor rarely (if ever) closes on the slowest consumer. (If this does happen, consider increasing the size of the buffer or
 * changing the consumers to process the entries more quickly, perhaps using a separate durable queue for those slow consumers.)
 * </p>
 * <p>
 * Consumers can be added after the ring buffer has entries, but such consumers will only see those entries that are added after
 * the consumer has been attached to the buffer.
 * </p>
 * <h2>Batching</h2>
 * <p>
 * Even though there is almost no locking within the ring buffer, the ring buffer uses another technique to make it as fast as
 * possible: batching. A producer can add multiple entries, called a "batch", at once. So rather than having to check for each
 * entry the the values that are shared among the different threads, adding entries via a batch means the shared data needs to be
 * checked only once per batch.
 * </p>
 * <p>
 * The consumer threads also process batches, although most of this is hidden within the runnable that calls the
 * {@link Consumer#consume(Object, long)} method. When ready to process an entry, this code asks for one entry and will get as
 * many entries that are available. All of the returned entries can then be processed without having to check any of the shared
 * data.
 * </p>
 * <h2>Shutdown</h2>
 * <p>
 * There are two ways to shut down the ring buffer to prevent adding new entries and to terminate all consumer threads:
 * <ol>
 * <li>{@link #shutdown(boolean)} is a graceful termination that immediately prevents adding new entries and that allows all
 * consumer threads to continue processing all previously-added entries. When each thread has consumed all entries, the consumer's
 * thread will terminate and the consumer "unregistered" from the ring buffer. The parameter to this method specifies whether the
 * method shoudl wait while all threads complete their processing.</li>
 * <li>{@link #shutdownNow()} immediately prevents adding new entries and immediately terminates all consumer threads, allowing
 * the consumer to continue processing any batch of entries that it is already working on. Thus each consumer may stop on a
 * different entry. Shutting down the ring buffer this way will stop the work almost immediately, but will leave unconsumed
 * entries in the buffer.</li>
 * </ol>
 * Once a ring buffer has been shutdown, it cannot be restarted.
 * </p>
 * 
 * @param <T> the type of entries stored in the buffer
 * @param <C> the type of consumer
 * @author Randall Hauch (rhauch@redhat.com)
 */
public final class RingBuffer<T, C> {

    private final int bufferSize;
    private final int mask;
    protected final Cursor cursor;
    private final Object[] buffer;
    private final Executor executor;
    protected final AtomicBoolean addEntries = new AtomicBoolean(true);
    protected final AtomicBoolean runConsumers = new AtomicBoolean(true);
    protected final ConsumerAdapter<T, C> consumerAdapter;
    private final ConcurrentMap<ConsumerRunner, ConsumerRunner> consumers = new ConcurrentHashMap<>();
    private final GarbageCollectingConsumer gcConsumer;

    RingBuffer( Cursor cursor,
                Executor executor,
                ConsumerAdapter<T, C> consumerAdapter,
                boolean gcEntries ) {
        this.cursor = cursor;
        this.bufferSize = cursor.getBufferSize();
        CheckArg.isPositive(bufferSize, "cursor.getBufferSize()");
        CheckArg.isPowerOfTwo(bufferSize, "cursor.getBufferSize()");
        this.mask = bufferSize - 1;
        this.buffer = new Object[bufferSize];
        this.executor = executor;
        this.consumerAdapter = consumerAdapter;
        if (gcEntries) {
            this.gcConsumer = this.cursor.createGarbageCollectingConsumer(new Collectable() {

                @Override
                public void collect( long position ) {
                    // System.out.println("----  CLEAR " + position);
                    clearEntry(position);
                }
            });
            this.executor.execute(gcConsumer);
        } else {
            this.gcConsumer = null;
        }
    }

    /**
     * Add to this buffer a single entry. This method blocks if there is no room in the ring buffer, providing back pressure on
     * the caller in such cases. Note that if this method blocks for any length of time, that means at least one consumer has yet
     * to process all of the entries that are currently in the ring buffer. In such cases, consider whether a larger ring buffer
     * is warranted.
     * 
     * @param entry the entry to be added; may not be null
     * @return true if the entry was added, or false if the buffer has been {@link #shutdown(boolean)}
     */
    public boolean add( T entry ) {
        assert entry != null;
        if (!addEntries.get()) return false;
        long position = cursor.claim(); // blocks
        int index = (int)(position & mask);
        buffer[index] = entry;
        return cursor.publish(position);
    }

    /**
     * Add to this buffer multiple entries. This method blocks until it is added.
     * 
     * @param entries the entries that are to be added; may not be null
     * @return true if all of the entries were added, or false if the buffer has been {@link #shutdown(boolean)} and none of the
     *         entries were added
     */
    public boolean add( T[] entries ) {
        assert entries != null;
        if (entries.length == 0 || !addEntries.get()) return false;
        long position = cursor.claim(entries.length); // blocks
        for (int i = 0; i != entries.length; ++i) {
            int index = (int)(position & mask);
            buffer[index] = entries[i];
        }
        return cursor.publish(position);
    }

    @SuppressWarnings( "unchecked" )
    protected T getEntry( long position ) {
        if (position < (cursor.getCurrent() - bufferSize)) {
            // The cursor has already overwritten the entry ...
            return null;
        }
        int index = (int)(position & mask);
        return (T)buffer[index];
    }

    protected void clearEntry( long position ) {
        if (position < (cursor.getCurrent() - bufferSize)) {
            // The cursor has already overwritten the entry ...
            return;
        }
        int index = (int)(position & mask);
        buffer[index] = null;
    }

    /**
     * Add the supplied consumer, and have it start processing entries in a separate thread.
     * <p>
     * Note that the thread will block when there are no more entries to be consumed. If the thread gets a timeout when waiting
     * for an entry, this method will retry the wait only one time before stopping.
     * </p>
     * <p>
     * The consumer is automatically removed from the ring buffer when it returns {@code false} from its
     * {@link Consumer#consume(Object, long)} method.
     * </p>
     * 
     * @param consumer the component that will process the entries; may not be null
     * @return true if the consumer was added, or false if the consumer was already registered with this buffer
     */
    public boolean addConsumer( final C consumer ) {
        return addConsumer(consumer, 1);
    }

    /**
     * Add the supplied consumer, and have it start processing entries in a separate thread.
     * <p>
     * The consumer is automatically removed from the ring buffer when it returns {@code false} from its
     * {@link Consumer#consume(Object, long)} method.
     * </p>
     * 
     * @param consumer the component that will process the entries; may not be null
     * @param timesToRetryUponTimeout the number of times that the thread should retry after timing out while waiting for the next
     *        entry; retries will not be attempted if the value is less than 1
     * @return true if the consumer was added, or false if the consumer was already registered with this buffer
     * @throws IllegalStateException if the ring buffer has already been {@link #shutdown(boolean)}
     */
    public boolean addConsumer( final C consumer,
                                final int timesToRetryUponTimeout ) {
        if (!addEntries.get()) {
            throw new IllegalStateException();
        }
        ConsumerRunner runner = new ConsumerRunner(consumer, timesToRetryUponTimeout);
        if (gcConsumer != null) gcConsumer.stayBehind(runner.getPointer());

        // Try to add the runner instance, with equality based upon consumer instance equality ...
        if (consumers.putIfAbsent(runner, runner) != null) return false;

        // It was added, so
        executor.execute(runner);
        return true;
    }

    /**
     * Remove the supplied consumer, and block until it stops running and is closed and removed from this buffer.
     * 
     * @param consumer the consumer component to be removed entry; retries will not be attempted if the value is less than 1
     * @return true if the consumer was removed, stopped, and closed, or false if the supplied consumer was not actually
     *         registered with this buffer (it may have completed)
     * @throws IllegalStateException if the ring buffer has already been {@link #shutdown(boolean)}
     */
    public boolean remove( C consumer ) {
        if (consumer != null) {
            // Iterate through the map to find the runner that owns this consumer ...
            ConsumerRunner match = null;
            for (ConsumerRunner runner : consumers.keySet()) {
                if (runner.getConsumer() == consumer) {
                    match = runner;
                    break;
                }
            }
            // Try to remove the matching runner (if we found one) from our list ...
            if (match != null) {
                // Tell the thread to stop and wait for it, after which it will have been removed from our map ...
                match.close();
                return true;
            }
        }
        // We either didn't find it, or we found it but something else remove it while we searched ...
        return false;
    }

    /**
     * Method called by the {@link ConsumerRunner#run()} method just before the method returns and the thread terminates. This
     * method invocation allows this buffer to clean up its reference to the runner.
     * 
     * @param runner the runner that has completed
     */
    protected void disconnect( ConsumerRunner runner ) {
        this.consumers.remove(runner);
        if (gcConsumer != null) gcConsumer.ignore(runner.getPointer());
    }

    /**
     * Shutdown this ring buffer by preventing any further entries, but allowing all existing entries to be processed by all
     * consumers.
     * 
     * @param block true if this method should block until all threads terminate after processing all remaining entries, or false
     *        if this method should return immediately (before all threads complete their processing)
     */
    public void shutdown( boolean block ) {
        // Prevent new entries from being added ...
        this.addEntries.set(false);

        // Mark the cursor as being finished; the threads will eventually catch up to this point and will each terminate ...
        this.cursor.complete();

        // Stop the garbage collection thread (if running) ...
        if (this.gcConsumer != null) this.gcConsumer.close();

        if (block) {
            // Go through all of the consumers and tell each to complete, waiting while they do ...
            removeAndStopAllListeners();
        }
    }

    /**
     * Shutdown this ring buffer by preventing any further entries from being added and stopping all consumers. This method blocks
     * until all threads are shutdown.
     */
    public void shutdownNow() {
        // Prevent new entries from being added ...
        this.addEntries.set(false);
        // Mark the cursor as being finished ...
        this.cursor.complete();

        // Stop the garbage collection thread (if running) ...
        if (this.gcConsumer != null) this.gcConsumer.close();

        // Tell all of the consumers to stop immediately ...
        this.runConsumers.set(false);

        // Go through all of the consumers and tell each to complete, waiting while they do ...
        removeAndStopAllListeners();
    }

    private void removeAndStopAllListeners() {
        while (!consumers.isEmpty()) {
            try {
                ConsumerRunner runner = consumers.keySet().iterator().next();
                runner.close();
            } catch (NoSuchElementException e) {
                // Must have been removed or finished while we were looking, so ignore it
            }
        }
        assert consumers.isEmpty();
    }

    public static interface ConsumerAdapter<EventType, ConsumerType> {
        boolean consume( ConsumerType consumer,
                         EventType event,
                         long position );

        void close( ConsumerType consumer );
    }

    protected class ConsumerRunner implements Runnable, AutoCloseable {
        private final C consumer;
        private final PointerBarrier barrier;
        private final Pointer pointer;
        private final int timesToRetryUponTimeout;
        private final AtomicBoolean runThread = new AtomicBoolean(true);
        private final CountDownLatch stopLatch = new CountDownLatch(1);

        protected ConsumerRunner( C consumer,
                                  final int timesToRetryUponTimeout ) {
            this.consumer = consumer;
            this.timesToRetryUponTimeout = timesToRetryUponTimeout;
            // Create a new barrier and a new pointer for consumer ...
            this.barrier = cursor.newBarrier();
            this.pointer = cursor.newPointer(); // the cursor will not wrap beyond this pointer
        }

        protected Pointer getPointer() {
            return pointer;
        }

        protected C getConsumer() {
            return consumer;
        }

        @Override
        public int hashCode() {
            return consumer.hashCode();
        }

        @Override
        public boolean equals( Object obj ) {
            if (this == obj) return true;
            if (obj instanceof RingBuffer.ConsumerRunner) {
                @SuppressWarnings( "unchecked" )
                ConsumerRunner that = (ConsumerRunner)obj;
                return this.consumer == that.consumer;
            }
            return false;
        }

        @Override
        public void close() {
            if (this.runThread.compareAndSet(true, false)) {
                try {
                    cursor.signalConsumers();
                    this.barrier.close();
                    this.stopLatch.await();
                } catch (InterruptedException e) {
                    // do nothing ...
                }
            }
        }

        @Override
        public void run() {
            try {
                int retry = timesToRetryUponTimeout;
                boolean consume = true;
                while (consume && runThread.get() && runConsumers.get()) {
                    T entry = null;
                    long next = pointer.get() + 1L;
                    try {
                        // Try to find the next position we can read to ...
                        long maxPosition = barrier.waitFor(next);
                        while (next <= maxPosition) {
                            entry = getEntry(next);
                            if (!runThread.get()) return;
                            if (!consumerAdapter.consume(consumer, entry, next)) {
                                // We're done, but tell the cursor to disregard our barrier ...
                                consume = false;
                                break;
                            }
                            next = pointer.incrementAndGet() + 1L;
                            retry = timesToRetryUponTimeout;
                        }
                        if (maxPosition < 0) {
                            // The buffer has been shutdown and there are no more positions, so we're done ...
                            return;
                        }
                    } catch (TimeoutException e) {
                        // It took too long to wait, but keep trying ...
                        --retry;
                        if (retry < 0) {
                            return;
                        }
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
                    try {
                        disconnect(this);
                    } finally {
                        try {
                            consumerAdapter.close(consumer);
                        } finally {
                            stopLatch.countDown();
                        }
                    }
                }
            }
        }

    }
}
