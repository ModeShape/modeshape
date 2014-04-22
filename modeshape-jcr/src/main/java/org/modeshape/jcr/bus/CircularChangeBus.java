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

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;

/**
 * @author Randall Hauch (rhauch@redhat.com)
 */
public final class CircularChangeBus implements ChangeBus {

    protected static final Logger LOGGER = Logger.getLogger(CircularChangeBus.class);

    private final AtomicBoolean shutdown = new AtomicBoolean(true);
    private final ExecutorService executor;
    /**
     * We use a lock for {@link #register(ChangeSetListener)}, {@link #registerInThread(ChangeSetListener)},
     * {@link #unregister(ChangeSetListener)}, {@link #start()} and {@link #shutdown()} to ensure that a single listener is
     * properly and atomcially added to either one of the maps. However, the {@link #notify(ChangeSet)} method only needs a
     * consistent snapshot of each of the maps (not a consistent snapshot of <em>both</em>), which is why we're using
     * <em>concurrent</em> maps (even though we're using a lock for registration).
     */
    private final Lock registrationLock = new ReentrantLock();
    private final ConcurrentMap<ChangeSetListener, ChangeSetListener> inThreadListeners = new ConcurrentHashMap<>();
    private final ConcurrentMap<ChangeSetListener, ChangeSetDispatcher> dispatchers = new ConcurrentHashMap<>();

    private final ChangeSet[] ringBuffer;

    /**
     * Creates new change bus
     * 
     * @param executor the {@link java.util.concurrent.ExecutorService} which will be used internally to submit workers to
     *        dispatching events to listeners.
     * @param systemWorkspaceName the name of the system workspace, needed because internal (system) events are dispatched in the
     *        same thread; may no be null
     * @param bufferCapacity the maximum capacity of the buffer
     */
    public CircularChangeBus( ExecutorService executor,
                              String systemWorkspaceName,
                              int bufferCapacity ) {
        this.executor = executor;
    }

    @Override
    public boolean hasObservers() {
        if (shutdown.get()) return false;
        return !dispatchers.isEmpty() || !inThreadListeners.isEmpty();
    }

    @Override
    public boolean register( ChangeSetListener observer ) {
        if (observer == null || shutdown.get()) return false;
        try {
            registrationLock.lock();
            if (inThreadListeners.containsKey(observer) || dispatchers.containsKey(observer)) return false;
            // Create the dispatcher and start the thread ...
            ChangeSetDispatcher dispatcher = new ChangeSetDispatcher(observer, executor);
            return dispatchers.put(observer, dispatcher) == null;
        } finally {
            registrationLock.unlock();
        }
    }

    @Override
    public boolean registerInThread( ChangeSetListener observer ) {
        if (observer == null || shutdown.get()) return false;
        try {
            registrationLock.lock();
            if (inThreadListeners.containsKey(observer) || dispatchers.containsKey(observer)) return false;
            inThreadListeners.put(observer, observer);
            return true;
        } finally {
            registrationLock.unlock();
        }
    }

    @Override
    public boolean unregister( ChangeSetListener observer ) {
        if (observer == null || shutdown.get()) return false;
        try {
            registrationLock.lock();
            // Try removing the dispatcher form first ...
            ChangeSetDispatcher dispatcher = dispatchers.remove(observer);
            if (dispatcher != null) {
                dispatcher.shutdown();
                return true;
            }

            // It was not a dispatcher, so try the in-thread forwarder ...
            return inThreadListeners.remove(observer) != null;
        } finally {
            registrationLock.unlock();
        }
    }

    @Override
    public void start() throws Exception {
        try {
            registrationLock.lock();
            shutdown.set(false);
        } finally {
            registrationLock.unlock();
        }
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

            // It was not shutdown but now is, so we have to remove all of the processors ...
            for (ChangeSetDispatcher dispatcher : dispatchers.values()) {
                try {
                    dispatcher.shutdown();
                } catch (Throwable e) {
                    // LOGGER.error(JcrI18n.errorShuttingDownProcessor, null, null);
                }
            }
            this.dispatchers.clear();
        } finally {
            registrationLock.unlock();
        }
    }

    @Override
    public void notify( ChangeSet changeSet ) {
        if (changeSet == null || !hasObservers()) {
            return;
        }

        if (shutdown.get()) {
            throw new IllegalStateException("Change bus has been already shut down, should not have any more observers");
        }

        // Add it to the ring buffer for the dispatchers ...

        // And process all of the in-thread listeners ...
        if (!inThreadListeners.isEmpty()) {
            for (ChangeSetListener listener : inThreadListeners.keySet()) {
                listener.notify(changeSet);
            }
        }
    }

    private static class ChangeSetDispatcher implements Callable<Void> {

        private final Future<Void> future;
        protected ChangeSetListener listener;

        protected ChangeSetDispatcher( ChangeSetListener listener,
                                       ExecutorService executor ) {
            this.listener = listener;
            this.future = executor.submit(this);
        }

        @Override
        public Void call() throws Exception {
            return null;
        }

        public void shutdown() {
            this.future.cancel(true);
        }
    }
}
