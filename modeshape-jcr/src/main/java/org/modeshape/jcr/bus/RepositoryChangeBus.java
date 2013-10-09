/*
 * ModeShape (http://www.modeshape.org)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of
 * individual contributors.
 *
 * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * ModeShape is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
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

    private static final String NULL_WORKSPACE_NAME = "null_workspace_name";
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
        this.workers = new HashMap<Integer, Future<?>>();
        this.dispatchers = new ArrayList<ChangeSetDispatcher>();
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
                ChangeSetDispatcher dispatcher = new ChangeSetDispatcher(listener);
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

        String workspaceName = changeSet.getWorkspaceName() != null ? changeSet.getWorkspaceName() : NULL_WORKSPACE_NAME;
        if (workspaceName.equalsIgnoreCase(systemWorkspaceName)) {
            // changes in the system workspace are always submitted in the same thread because they need immediate processing
            submitChanges(changeSet, true);
        } else {
            submitChanges(changeSet, false);
        }
    }

    private boolean submitChanges( ChangeSet changeSet,
                                   boolean inThread ) {
        try {
            listenersLock.readLock().lock();
            for (ChangeSetDispatcher dispatcher : dispatchers) {
                if (inThread) {
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
        private ChangeSetListener listener;
        private BlockingQueue<ChangeSet> queue;

        protected ChangeSetDispatcher( ChangeSetListener listener ) {
            this.listener = listener;
            this.listenerHashCode = HashCode.compute(listener);
            this.queue = new LinkedBlockingQueue<ChangeSet>();
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

        private void shutdown() {
            while (!queue.isEmpty()) {
                listener.notify(queue.remove());
            }
            this.listener = null;
            this.queue = null;
        }
    }
}
