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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.modeshape.common.annotation.GuardedBy;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.HashCode;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;

/**
 * A standard {@link ChangeBus} implementation.
 * 
 * @author Horia Chiorean
 */
@ThreadSafe
public final class RepositoryChangeBus implements ChangeBus {

    protected static final Logger LOGGER = Logger.getLogger(RepositoryChangeBus.class);

    protected volatile boolean shutdown;

    private final ExecutorService executor;
    private final List<ChangeSetDispatcher> dispatchers;
    private final Map<Integer, Future<?>> workers;
    private final ReadWriteLock listenersLock;

    private final String systemWorkspaceName;

    /**
     * Creates new change bus
     * 
     * @param executor the {@link ExecutorService} which will be used internally to submit workers to dispatching events to
     *        listeners.
     * @param systemWorkspaceName the name of the system workspace, needed because internal (system) events are dispatched in the
     *        same thread; may no be null
     */
    public RepositoryChangeBus( ExecutorService executor,
                                String systemWorkspaceName ) {
        this.systemWorkspaceName = systemWorkspaceName;
        this.workers = new HashMap<>();
        this.dispatchers = new ArrayList<>();
        this.listenersLock = new ReentrantReadWriteLock(true);
        this.executor = executor;
        this.shutdown = false;
    }

    @Override
    public synchronized void start() {
    }

    @Override
    public synchronized void shutdown() {
        shutdown = true;
        dispatchers.clear();
        stopWork();
    }

    private void stopWork() {
        executor.shutdown();
        for (Future<?> worker : workers.values()) {
            if (!worker.isDone()) {
                worker.cancel(true);
            }
        }
        workers.clear();
    }

    @GuardedBy( "listenersLock" )
    @Override
    public boolean register( ChangeSetListener listener ) {
        return internalRegister(listener, false);
    }

    @Override
    @GuardedBy( "listenersLock" )
    public boolean registerInThread( ChangeSetListener listener ) {
        return internalRegister(listener, true);
    }

    private boolean internalRegister( ChangeSetListener listener, boolean inThread ) {
        if (listener == null) {
            return false;
        }
        int hashCode = HashCode.compute(listener);
        if (workers.containsKey(hashCode)) {
            return false;
        }
        try {
            listenersLock.writeLock().lock();

            if (!workers.containsKey(hashCode)) {
                ChangeSetDispatcher dispatcher = new ChangeSetDispatcher(listener, inThread);
                dispatchers.add(dispatcher);
                workers.put(hashCode, executor.submit(dispatcher));
                return true;
            }
            return false;
        } finally {
            listenersLock.writeLock().unlock();
        }
    }

    @GuardedBy( "listenersLock" )
    @Override
    public boolean unregister( ChangeSetListener listener ) {
        if (listener == null) {
            return false;
        }
        int hashCode = HashCode.compute(listener);
        if (!workers.containsKey(hashCode)) {
            return false;
        }
        try {
            listenersLock.writeLock().lock();
            if (!workers.containsKey(hashCode)) {
                return false;
            }
            for (Iterator<ChangeSetDispatcher> dispatcherIterator = dispatchers.iterator(); dispatcherIterator.hasNext();) {
                ChangeSetDispatcher dispatcher = dispatcherIterator.next();
                if (dispatcher.listenerHashCode() == hashCode) {
                    Future<?> work = workers.remove(hashCode);
                    // cancelling the work will call shutdown on the dispatcher
                    work.cancel(true);
                    dispatcherIterator.remove();
                    return true;
                }
            }
        } finally {
            listenersLock.writeLock().unlock();
        }
        return false;
    }

    @GuardedBy( "listenersLock" )
    @Override
    public void notify( ChangeSet changeSet ) {
        if (changeSet == null || !hasObservers()) {
            return;
        }

        if (shutdown) {
            throw new IllegalStateException("Change bus has been already shut down, should not have any more observers");
        }

        // changes in the system workspace are always submitted in the same thread because they need immediate processing
        boolean inThread = systemWorkspaceName.equalsIgnoreCase(changeSet.getWorkspaceName());
        submitChanges(changeSet, inThread);
    }

    private boolean submitChanges( ChangeSet changeSet,
                                   boolean inThread ) {
        try {
            listenersLock.readLock().lock();
            for (ChangeSetDispatcher dispatcher : dispatchers) {
                if (inThread || dispatcher.notifyInSameThread()) {
                    dispatcher.listener().notify(changeSet);
                } else {
                    dispatcher.submit(changeSet);
                }
            }
            return true;
        } finally {
            listenersLock.readLock().unlock();
        }
    }

    @GuardedBy( "listenersLock" )
    @Override
    public boolean hasObservers() {
        try {
            listenersLock.readLock().lock();
            return !dispatchers.isEmpty();
        } finally {
            listenersLock.readLock().unlock();
        }
    }

    private class ChangeSetDispatcher implements Callable<Void> {

        private final int listenerHashCode;
        private final boolean notifyInSameThread;
        private ChangeSetListener listener;
        private BlockingQueue<ChangeSet> queue;

        protected ChangeSetDispatcher( ChangeSetListener listener, boolean notifyInSameThread ) {
            this.listener = listener;
            this.listenerHashCode = HashCode.compute(listener);
            this.queue = new LinkedBlockingQueue<>();
            this.notifyInSameThread = notifyInSameThread;
        }

        @Override
        public Void call() {
            while (!shutdown) {
                try {
                    ChangeSet changeSet = queue.take();
                    if (changeSet != null) {
                        listener.notify(changeSet);
                    }
                } catch (InterruptedException e) {
                    Thread.interrupted();
                    break;
                }
            }
            shutdown();
            return null;
        }

        protected void submit( ChangeSet changeSet ) {
            if (!queue.offer(changeSet)) {
                LOGGER.debug("Cannot submit change set: {0} because the queue is full", changeSet);
            }
        }

        protected int listenerHashCode() {
            return listenerHashCode;
        }

        protected ChangeSetListener listener() {
            return listener;
        }

        protected boolean notifyInSameThread() {
            return notifyInSameThread;
        }

        private void shutdown() {
            while (!queue.isEmpty()) {
                listener.notify(queue.remove());
            }
            this.listener = null;
            this.queue = null;
        }
    }
}
