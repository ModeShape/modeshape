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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.LinkedBlockingQueue;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.HashCode;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.change.Change;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.value.BinaryKey;

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
    private final Set<ChangeSetDispatcher> dispatchers;
    private final ConcurrentHashMap<Integer, Future<?>> workers;

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
        this.workers = new ConcurrentHashMap<Integer, Future<?>>();
        this.dispatchers = Collections.newSetFromMap(new ConcurrentHashMap<ChangeSetDispatcher, Boolean>());
        this.executor = executor;
        this.shutdown = false;
    }

    @Override
    public synchronized void start() {
    }

    @Override
    public synchronized void shutdown() {
        // add a stop marker to signal that all the internal workers should stop waiting on the queue
        for (ChangeSetDispatcher dispatcher : dispatchers) {
            dispatcher.submit(FinalChangeSet.INSTANCE);
        }
        shutdown = true;
        for (Future<?> worker : workers.values()) {
            // cancel any remaining active work (best effort)
            worker.cancel(true);
        }
        executor.shutdownNow();
        workers.clear();
        dispatchers.clear();
    }

    @Override
    public boolean register( ChangeSetListener listener ) {
        if (listener == null || shutdown) {
            return false;
        }
        int hashCode = HashCode.compute(listener);
        if (!workers.containsKey(hashCode)) {
            ChangeSetDispatcher dispatcher = new ChangeSetDispatcher(listener);
            dispatchers.add(dispatcher);
            return workers.putIfAbsent(hashCode, executor.submit(dispatcher)) == null;
        }
        return false;
    }

    @Override
    public boolean unregister( ChangeSetListener listener ) {
        if (listener == null) {
            return false;
        }
        int hashCode = HashCode.compute(listener);
        if (workers.containsKey(hashCode)) {
            for (Iterator<ChangeSetDispatcher> dispatcherIterator = dispatchers.iterator(); dispatcherIterator.hasNext();) {
                ChangeSetDispatcher dispatcher = dispatcherIterator.next();
                if (dispatcher.listenerHashCode() == hashCode) {
                    // add the stop marker to the queue
                    dispatcher.submit(FinalChangeSet.INSTANCE);
                    Future<?> work = workers.remove(hashCode);
                    if (work != null) {
                        // cancel the work (best effort)
                        work.cancel(true);
                    }
                    dispatcherIterator.remove();
                    return true;
                }
            }
        }
        return false;
    }

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
        for (ChangeSetDispatcher dispatcher : dispatchers) {
            if (inThread) {
                dispatcher.listener().notify(changeSet);
            } else {
                dispatcher.submit(changeSet);
            }
        }
        return true;
    }

    @Override
    public boolean hasObservers() {
        return !dispatchers.isEmpty();
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
            boolean interrupted = false;
            try {
                while (!shutdown) {
                    ChangeSet changeSet = queue.take();
                    if (changeSet == FinalChangeSet.INSTANCE) {
                        // stop marker and stop processing immediately
                        shutdown(true);
                        return null;
                    } else if (changeSet != null) {
                        listener.notify(changeSet);
                    }
                }
            } catch (InterruptedException e) {
                interrupted = true;
                Thread.interrupted();
            }
            shutdown(interrupted);
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

        private void shutdown( boolean immediately ) {
            if (immediately) {
                this.queue.clear();
            } else {
                while (!queue.isEmpty()) {
                    ChangeSet changeSet = queue.remove();
                    if (changeSet != FinalChangeSet.INSTANCE) {
                        listener.notify(changeSet);
                    }
                }
            }
            this.listener = null;
            this.queue = null;
        }
    }

    private static class FinalChangeSet implements ChangeSet {
        private static final long serialVersionUID = 1L;
        private static final FinalChangeSet INSTANCE = new FinalChangeSet();

        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public String getUserId() {
            return null;
        }

        @Override
        public Map<String, String> getUserData() {
            return null;
        }

        @Override
        public DateTime getTimestamp() {
            return null;
        }

        @Override
        public String getProcessKey() {
            return null;
        }

        @Override
        public String getRepositoryKey() {
            return null;
        }

        @Override
        public String getWorkspaceName() {
            return null;
        }

        @Override
        public Set<NodeKey> changedNodes() {
            return null;
        }

        @Override
        public Set<BinaryKey> unusedBinaries() {
            return null;
        }

        @Override
        public Set<BinaryKey> usedBinaries() {
            return null;
        }

        @Override
        public boolean hasBinaryChanges() {
            return false;
        }

        @Override
        public String getSessionId() {
            return null;
        }

        @Override
        public Iterator<Change> iterator() {
            return null;
        }

        @Override
        public String toString() {
            return "RepositoryChangeBus#STOP_MARKER";
        }
    }
}
