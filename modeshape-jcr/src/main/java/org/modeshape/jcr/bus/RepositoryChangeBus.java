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

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.modeshape.common.annotation.GuardedBy;
import org.modeshape.common.annotation.ThreadSafe;
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

    private final ExecutorService executor;
    private final ConcurrentHashMap<String, ConcurrentHashMap<ChangeSetListener, BlockingQueue<ChangeSet>>> workspaceListenerQueues;
    private final Set<Future<?>> workers;

    private final Set<ChangeSetListener> listeners;
    private final ReadWriteLock listenersLock = new ReentrantReadWriteLock(true);

    protected volatile boolean shutdown;

    private final String systemWorkspaceName;

    public RepositoryChangeBus( ExecutorService executor,
                                String systemWorkspaceName,
                                boolean separateThreadForSystemWorkspace ) {
        this.systemWorkspaceName = systemWorkspaceName;
        this.workers = new HashSet<Future<?>>();
        this.workspaceListenerQueues = new ConcurrentHashMap<String, ConcurrentHashMap<ChangeSetListener, BlockingQueue<ChangeSet>>>();
        this.executor = executor;
        this.listeners = Collections.synchronizedSet(new LinkedHashSet<ChangeSetListener>());
        this.shutdown = false;
    }

    RepositoryChangeBus( ExecutorService executor ) {
        this(executor, null, false);
    }

    @Override
    public void start() {
    }

    @Override
    public void shutdown() {
        shutdown = true;
        try {
            listenersLock.writeLock().lock();
            listeners.clear();
            workspaceListenerQueues.clear();
            stopWork();
        } finally {
            listenersLock.writeLock().unlock();
        }
    }

    private void stopWork() {
        executor.shutdown();
        for (Future<?> worker : workers) {
            if (!worker.isDone()) worker.cancel(true);
        }
        workers.clear();
    }

    @GuardedBy( "listenersLock" )
    @Override
    public boolean register( ChangeSetListener listener ) {
        if (listener == null) {
            return false;
        }
        try {
            listenersLock.writeLock().lock();
            return listeners.add(listener);
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
        try {
            listenersLock.writeLock().lock();
            return listeners.remove(listener);
        } finally {
            listenersLock.writeLock().unlock();
        }
    }

    @Override
    public void notify( ChangeSet changeSet ) {
        if (changeSet == null || !hasObservers()) {
            return;
        }

        if (shutdown) {
            throw new IllegalStateException("Change bus has been already shut down, should not be receiving events");
        }

        String workspaceName = changeSet.getWorkspaceName() != null ? changeSet.getWorkspaceName() : NULL_WORKSPACE_NAME;

        if (notifiedSystemWorkspaceListenersInline(changeSet, workspaceName)) {
            return;
        }

        ConcurrentHashMap<ChangeSetListener, BlockingQueue<ChangeSet>> listenersForWorkspace = workspaceListenerQueues.get(workspaceName);
        if (listenersForWorkspace == null) {
            listenersForWorkspace = new ConcurrentHashMap<ChangeSetListener, BlockingQueue<ChangeSet>>();
            ConcurrentHashMap<ChangeSetListener, BlockingQueue<ChangeSet>> existingMap = workspaceListenerQueues.putIfAbsent(workspaceName,
                                                                                                                             listenersForWorkspace);
            if (existingMap != null) {
                listenersForWorkspace = existingMap;
            }
        }

        try {
            listenersLock.readLock().lock();
            for (ChangeSetListener listener : listeners) {
                BlockingQueue<ChangeSet> listenerQueue = listenersForWorkspace.get(listener);
                if (listenerQueue == null) {
                    listenerQueue = new LinkedBlockingQueue<ChangeSet>();
                    listenerQueue.add(changeSet);
                    BlockingQueue<ChangeSet> existingQueue = listenersForWorkspace.putIfAbsent(listener, listenerQueue);
                    if (existingQueue != null) {
                        listenerQueue = existingQueue;
                    }
                    ChangeSetDispatcher dispatcher = new ChangeSetDispatcher(listener, listenerQueue);
                    workers.add(executor.submit(dispatcher));
                } else {
                    listenerQueue.add(changeSet);
                }
            }
        } finally {
            listenersLock.readLock().unlock();
        }
    }

    private boolean notifiedSystemWorkspaceListenersInline( ChangeSet changeSet,
                                                            String workspaceName ) {
        if (workspaceName.equalsIgnoreCase(systemWorkspaceName)) {
            listenersLock.readLock().lock();
            try {
                for (ChangeSetListener listener : listeners) {
                    listener.notify(changeSet);
                }
                return true;
            } finally {
                listenersLock.readLock().unlock();
            }
        }
        return false;
    }

    @Override
    public boolean hasObservers() {
        try {
            listenersLock.readLock().lock();
            return !listeners.isEmpty();
        } finally {
            listenersLock.readLock().unlock();
        }
    }

    private class ChangeSetDispatcher implements Callable<Void> {

        private static final int DEFAULT_POLL_TIMEOUT = 3;

        private ChangeSetListener listener;
        private BlockingQueue<ChangeSet> changeSetQueue;

        ChangeSetDispatcher( ChangeSetListener listener,
                             BlockingQueue<ChangeSet> changeSetQueue ) {
            this.listener = listener;
            this.changeSetQueue = changeSetQueue;
        }

        @Override
        public Void call() {
            while (!shutdown) {
                try {
                    ChangeSet changeSet = changeSetQueue.poll(DEFAULT_POLL_TIMEOUT, TimeUnit.SECONDS);
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

        private void shutdown() {
            while (!changeSetQueue.isEmpty()) {
                listener.notify(changeSetQueue.poll());
            }
            this.listener = null;
            this.changeSetQueue = null;
        }
    }
}
