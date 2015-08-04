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

package org.modeshape.jcr.bus;

import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.modeshape.common.collection.ring.RingBuffer;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.RepositoryStatistics;
import org.modeshape.jcr.api.monitor.ValueMetric;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;

/**
 * Change bus implementation around a {@link org.modeshape.common.collection.ring.RingBuffer}
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 * @author Horia Chiorean (hchiorean@redhat.com)
 */
public final class RepositoryChangeBus implements ChangeBus {

    public static final int DEFAULT_RING_BUFFER_SIZE = 1 << 10; // 1024

    protected static final Logger LOGGER = Logger.getLogger(RepositoryChangeBus.class);
    
    private final AtomicBoolean shutdown = new AtomicBoolean(true);
    
    /**
     * We use a lock for {@link #register(ChangeSetListener)}, {@link #registerInThread(ChangeSetListener)},
     * {@link #unregister(ChangeSetListener)}, {@link #start()} and {@link #shutdown()} to ensure that a single listener is
     * properly and atomcially added to either one of the maps. However, the {@link #notify(ChangeSet)} method only needs a
     * consistent snapshot of each of the maps (not a consistent snapshot of <em>both</em>), which is why we're using
     * <em>concurrent</em> maps (even though we're using a lock for registration).
     */
    private final Lock registrationLock = new ReentrantLock();
    private final Set<ChangeSetListener> inThreadListeners = new CopyOnWriteArraySet<>();
    private final RingBuffer<ChangeSet, ChangeSetListener> ringBuffer;
    private final RepositoryStatistics statistics;

    /**
     * Creates a new change bus
     * 
     * @param repositoryName the repository name; may not be null
     * @param executor the {@link java.util.concurrent.ExecutorService} which will be used internally to submit workers to
     *        dispatching events to listeners.
     */
    public RepositoryChangeBus( String repositoryName,
                                ExecutorService executor ) {
        this(repositoryName, executor, null, DEFAULT_RING_BUFFER_SIZE);
    }

    /**
     * Creates a new change bus
     * 
     * @param repositoryName the repository name; may not be null
     * @param executor the {@link java.util.concurrent.ExecutorService} which will be used internally to submit workers to
     *        dispatching events to listeners.
     * @param statistics a {@link RepositoryStatistics} instance used to record various metrics; may be null 
     * @param bufferSize the total size of the ring buffer
     */
    public RepositoryChangeBus( String repositoryName,
                                ExecutorService executor,
                                RepositoryStatistics statistics,
                                int bufferSize) {
        this.ringBuffer = RepositoryRingBufferBuilder.withMultipleProducers(executor, 
                                                                            new ChangeSetListenerConsumerAdapter(),
                                                                            statistics)
                                                     .ofSize(bufferSize)
                                                     .named(repositoryName)
                                                     .garbageCollect(true)
                                                     .build();
        this.statistics = statistics;
    }

    @Override
    public boolean hasObservers() {
        if (shutdown.get()) return false;
        return !inThreadListeners.isEmpty() || ringBuffer.hasConsumers();
    }

    @Override
    public boolean register( ChangeSetListener observer ) {
        if (observer == null || shutdown.get()) return false;
        try {
            registrationLock.lock();
            boolean result = ringBuffer.addConsumer(observer);
            if (result && statistics != null) {
                statistics.increment(ValueMetric.LISTENER_COUNT);               
            }      
            return result;
        } finally {
            registrationLock.unlock();
        }
    }

    @Override
    public boolean registerInThread( ChangeSetListener observer ) {
        if (observer == null || shutdown.get()) return false;
        try {
            registrationLock.lock();
            boolean result = inThreadListeners.add(observer);
            if (result && statistics != null) {
                statistics.increment(ValueMetric.LISTENER_COUNT);
            }
            return result;
        } finally {
            registrationLock.unlock();
        }
    }

    @Override
    public boolean unregister( ChangeSetListener observer ) {
        if (observer == null || shutdown.get()) return false;
        try {
            registrationLock.lock();
            boolean result =  ringBuffer.remove(observer) || inThreadListeners.remove(observer);
            if (result && statistics != null) {
                statistics.decrement(ValueMetric.LISTENER_COUNT);                
            }
            return result;
        } finally {
            registrationLock.unlock();
        }
    }

    @Override
    public synchronized void start() throws Exception {
        shutdown.set(false);
    }

    @Override
    public synchronized void shutdown() {
        // This method is synchronized to make sure that 'start' and 'stop' are not called simultaneously ...
        if (shutdown.getAndSet(true)) {
            // It was already shutdown ...
            return;
        }

        try {
            registrationLock.lock();
            // Clear all of the in-thread listeners ...
            inThreadListeners.clear();
            // Shutdown the ring buffer waiting for running threads to complete
            ringBuffer.shutdown();
            // Clear the metric around the total number of listeners
            if (statistics != null) {
                statistics.set(ValueMetric.LISTENER_COUNT, 0);
            }
        } finally {
            registrationLock.unlock();
        }
    }

    @Override
    public void notify( ChangeSet changeSet ) {
        if (changeSet == null || !hasObservers()) return;
        if (shutdown.get()) {
            throw new IllegalStateException("Change bus has been already shut down, should not have any more observers");
        }

        // Add the change set into the buffer so it can be processed by the asynchronous listeners ...
        ringBuffer.add(changeSet);

        if (statistics != null) {
            // Increment the statistics
            statistics.increment(ValueMetric.EVENT_COUNT);
        }
        
        // And process all of the in-thread listeners ...
        for (ChangeSetListener listener : inThreadListeners) {
            try {
                listener.notify(changeSet);
            } catch (RuntimeException e) {
                if (shutdown.get()) {
                    // The repository has been shutdown, so we have to ignore these changes
                } else {
                    throw e;
                }
            }
        }
    }

    protected class ChangeSetListenerConsumerAdapter implements RingBuffer.ConsumerAdapter<ChangeSet, ChangeSetListener> {
        @Override
        public boolean consume( ChangeSetListener consumer,
                                ChangeSet event,
                                long position,
                                long maxPosition ) {
            consumer.notify(event);
            return true;
        }

        @Override
        public void close( ChangeSetListener consumer ) {
            // nothing to do here
        }

        @Override
        public void handleException( ChangeSetListener consumer,
                                     Throwable t,
                                     ChangeSet entry,
                                     long position,
                                     long maxPosition ) {
            LOGGER.error(t, BusI18n.errorProcessingEvent, entry.toString(), position);
        }
    }
}
