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

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import org.infinispan.schematic.internal.HashCode;
import org.modeshape.common.annotation.GuardedBy;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;

/**
 * A {@link ChangeBus} implementation based on the LMAX disruptor via a {@link org.modeshape.jcr.bus.ChangeSetDispatcher} instance.
 *
 * @author Horia Chiorean
 * @see <a href="https://github.com/LMAX-Exchange/disruptor/wiki/Introduction">LMAX Disruptor</a>
 */
@NotThreadSafe
public final class DisruptorRepositoryChangeBus implements ChangeBus {

    protected static final Logger LOGGER = Logger.getLogger(DisruptorRepositoryChangeBus.class);
    protected static final int DEFAULT_BUFFER_SIZE = 1 << 10; //1024

    protected volatile boolean shutdown;

    private final String systemWorkspaceName;
    private final ChangeSetDispatcher dispatcher;
    private final Set<Integer> activeListenerHashes;

    /**
     * Creates new change bus
     *
     * @param executor the {@link java.util.concurrent.ExecutorService} which will be used internally to submit workers to dispatching events to
     * listeners.
     * @param systemWorkspaceName the name of the system workspace, needed because internal (system) events are dispatched in the
     * same thread; may no be null
     */
    public DisruptorRepositoryChangeBus( ExecutorService executor,
                                         String systemWorkspaceName ) {
        this.systemWorkspaceName = systemWorkspaceName;
        this.shutdown = false;
        this.dispatcher = new ChangeSetDispatcher(executor, DEFAULT_BUFFER_SIZE);
        this.activeListenerHashes = new HashSet<>();
    }

    @Override
    public synchronized void start() {
    }

    @Override
    public synchronized void shutdown() {
        shutdown = true;
        dispatcher.stop();
        activeListenerHashes.clear();
    }

    @Override
    public boolean register( ChangeSetListener listener ) {
        return internalRegister(listener, false);
    }

    @Override
    public boolean registerInThread( ChangeSetListener listener ) {
        return internalRegister(listener, true);
    }

    private boolean internalRegister( ChangeSetListener listener, boolean inThread ) {
        validateNotStopped();

        if (listener == null) {
            return false;
        }

        Integer hashCode = HashCode.compute(listener);
        if  (activeListenerHashes.contains(hashCode)) {
            return false;
        }

        if (inThread) {
            dispatcher.addSyncListener(listener);
        } else {
            dispatcher.addAsyncListener(listener);
        }
        activeListenerHashes.add(hashCode);
        return true;
    }

    @Override
    public boolean unregister( ChangeSetListener listener ) {
        validateNotStopped();

        if (listener == null) {
            return false;
        }

        Integer hashCode = HashCode.compute(listener);
        if  (!activeListenerHashes.remove(hashCode)) {
            return false;
        }

        dispatcher.removeListener(listener);
        return true;
    }

    @GuardedBy( "listenersLock" )
    @Override
    public void notify( ChangeSet changeSet ) {
        if (changeSet == null || !hasObservers()) {
            return;
        }

        if (systemWorkspaceName.equalsIgnoreCase(changeSet.getWorkspaceName())) {
            // changes in the system workspace are always submitted in the same thread because they need immediate processing
            dispatcher.dispatchSync(changeSet);
        } else {
            dispatcher.dispatchAsync(changeSet);
        }
    }

    private void validateNotStopped() {
        if (shutdown) {
            throw new IllegalStateException("Change bus has been already shut down, should not have any more observers");
        }
    }

    @Override
    public boolean hasObservers() {
        return !activeListenerHashes.isEmpty();
    }
}
