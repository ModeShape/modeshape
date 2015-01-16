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

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.clustering.ClusteringService;
import org.modeshape.jcr.clustering.MessageConsumer;

/**
 * Implementation of a {@link ChangeBus} which can run in a cluster, via {@link ClusteringService}. This bus wraps around another
 * bus, to which it delegates all "local" processing of events.
 * <p>
 * It is important that the order of the {@link org.modeshape.jcr.cache.change.ChangeSet} instances are maintained across the
 * cluster, and JGroups will do this for us as long as we push all local changes into the channel and receive all local/remote
 * changes from the channel.
 * </p>
 * 
 * @author Horia Chiorean
 */
@ThreadSafe
public final class ClusteredChangeBus extends MessageConsumer<ChangeSet> implements ChangeBus {

    private static final Logger LOGGER = Logger.getLogger(ClusteredChangeBus.class);

    /**
     * The wrapped standalone bus to which standard bus operations are delegated
     */
    private final ChangeBus delegate;

    /**
     * The {@link ClusteringService} which handles communication inside the cluster
     */
    private final ClusteringService clusteringService;

    /**
     * A map which contains latches used for the thread which places a message on the bus to wait until it gets that message back from JGroups
     */
    private final Map<String, CountDownLatch> loopbackLatches = new HashMap<>();

    /**
     * Creates a new clustered repository bus
     * 
     * @param delegate the local bus to which changes will be delegated
     * @param clusteringService the object which will handle sending/receiving information in the cluster.
     */
    public ClusteredChangeBus( ChangeBus delegate,
                               ClusteringService clusteringService ) {
        super(ChangeSet.class);

        CheckArg.isNotNull(delegate, "delegate");
        CheckArg.isNotNull(clusteringService, "clusteringService");
        this.delegate = delegate;
        this.clusteringService = clusteringService;
    }

    @Override
    public void consume( ChangeSet changes ) {
        if (hasObservers()) {
            try {
                delegate.notify(changes);
                logReceivedOperation(changes);
            } finally {
                // if we're waiting for a loopback message to arrive back, always make sure we notify the waiting thread
                if (!loopbackLatches.isEmpty()) {
                    String uuid = changes.getUUID();
                    CountDownLatch latch = loopbackLatches.remove(uuid);
                    if (latch != null) {
                        latch.countDown();   
                        if (LOGGER.isTraceEnabled()) {
                            LOGGER.trace("Loopback changeset {0} received back on {1}", uuid, clusteringService.toString());
                        }
                    }
                }
            }
        }
    }

    @Override
    public synchronized void start() throws Exception {
        // make sure the clustering service is open
        if (!clusteringService.isOpen()) {
            throw new IllegalStateException("The clustering service has not been started");
        }

        // start the delegate
        delegate.start();

        // register with the clustering service
        clusteringService.addConsumer(this);
    }

    @Override
    public boolean hasObservers() {
        return delegate.hasObservers();
    }

    @Override
    public synchronized void shutdown() {
        delegate.shutdown();
        loopbackLatches.clear();
    }

    @Override
    public void notify( ChangeSet changeSet ) {
        if (changeSet == null) {
            return; // do nothing
        }
        if (!clusteringService.multipleMembersInCluster()) {
            // We are in clustered mode, but there is only one participant in the cluster (us).
            // So short-circuit the cluster and just notify the local observers ...
            consume(changeSet);
            return;
        }

        // There are multiple participants in the cluster, so send all changes out to JGroups,
        // letting JGroups do the ordering of messages...
        // note that JGroups will dispatch our own changeset *in a separate thread* (see below)
        logSendOperation(changeSet);
        clusteringService.sendMessage(changeSet);
        
        // Since JGroups will always dispatch incoming message off a separate thread, we need to wait here until JGroups
        // has dispatched our own message back. This is because we have the semantics of in-thread listeners which must
        // work correctly. See MODE-2409 for details.
        CountDownLatch waitForLoopback = new CountDownLatch(1);
        String uuid = changeSet.getUUID();
        loopbackLatches.put(uuid, waitForLoopback);
        try {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Waiting for loopback changeset {0} to be received back on {1}", uuid, clusteringService.toString());
            }
            if (!waitForLoopback.await(15, TimeUnit.SECONDS)) {
                loopbackLatches.remove(uuid);
                LOGGER.error(BusI18n.loopbackMessageNotReceived, uuid, clusteringService.toString());
            }
        } catch (InterruptedException e) {
            loopbackLatches.remove(uuid);
            Thread.interrupted();
            LOGGER.error(e, BusI18n.loopbackMessageNotReceived, uuid, clusteringService.toString());
        }
    }

    protected final void logSendOperation( ChangeSet changeSet ) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Sending to cluster '{0}' {1} changes on workspace {2} made by {3} from process '{4}' at {5}",
                         clusteringService.toString(),
                         changeSet.size(),
                         changeSet.getWorkspaceName(),
                         changeSet.getUserData(),
                         changeSet.getProcessKey(),
                         changeSet.getTimestamp());
        }
    }

    protected final void logReceivedOperation( ChangeSet changeSet ) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Received from cluster '{0}' {1} changes on workspace {2} made by {3} from process '{4}' at {5}",
                         clusteringService.toString(),
                         changeSet.size(),
                         changeSet.getWorkspaceName(),
                         changeSet.getUserId(),
                         changeSet.getProcessKey(),
                         changeSet.getTimestamp());

        }
    }

    @Override
    public boolean register( ChangeSetListener listener ) {
        return delegate.register(listener);
    }

    @Override
    public boolean registerInThread( ChangeSetListener listener ) {
        return delegate.registerInThread(listener);
    }

    @Override
    public boolean unregister( ChangeSetListener listener ) {
        return delegate.unregister(listener);
    }
}
