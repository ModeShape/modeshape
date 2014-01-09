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

import org.modeshape.common.annotation.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.clustering.ClusteringService;
import org.modeshape.jcr.clustering.MessageConsumer;

/**
 * Implementation of a {@link ChangeBus} which can run in a cluster, via {@link ClusteringService}. This bus wraps around another bus, to which it
 * delegates all "local" processing of events.
 * <p>
 * It is important that the order of the {@link org.modeshape.jcr.cache.change.ChangeSet} instances are maintained across the cluster, and JGroups will do
 * this for us as long as we push all local changes into the channel and receive all local/remote changes from the channel.
 * </p>
 *
 * @author Horia Chiorean
 */
@ThreadSafe
public final class ClusteredRepositoryChangeBus implements ChangeBus, MessageConsumer<ChangeSet> {

    /**
     * The type of a message containing a change set.
     */
    public static final int CHANGESET_MESSAGE = 1;

    private static final Logger LOGGER = Logger.getLogger(ClusteredRepositoryChangeBus.class);

    /**
     * The wrapped standalone bus to which standard bus operations are delegated
     */
    private final ChangeBus delegate;

    /**
     * The {@link ClusteringService} which handles communication inside the cluster
     */
    private final ClusteringService clusteringService;

    /**
     * Creates a new clustered repository bus
     *
     * @param delegate the local bus to which changes will be delegated
     * @param clusteringService the object which will handle sending/receiving information in the cluster.
     */
    public ClusteredRepositoryChangeBus( ChangeBus delegate,
                                         ClusteringService clusteringService) {
        CheckArg.isNotNull(delegate, "delegate");
        CheckArg.isNotNull(clusteringService, "clusteringService");
        this.delegate = delegate;
        this.clusteringService = clusteringService;
    }

    @Override
    public boolean interestedIn( int messageType ) {
        return messageType == CHANGESET_MESSAGE;
    }

    @Override
    public void consume( ChangeSet changes ) {
        if (hasObservers()) {
            delegate.notify(changes);
            logReceivedOperation(changes);
        }
    }

    @Override
    public synchronized void start() throws Exception {
        // make sure the clustering service is open
        if (!clusteringService.isOpen())  {
            throw new IllegalStateException("The clustering service has not been started");
        }

        // start the delegate
        delegate.start();

        //register with the clustering service
        clusteringService.addConsumer(this);
    }

    @Override
    public boolean hasObservers() {
        return delegate.hasObservers();
    }

    @Override
    public synchronized void shutdown() {
        delegate.shutdown();
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
        logSendOperation(changeSet);
        clusteringService.sendMessage(changeSet, CHANGESET_MESSAGE);
    }

    protected final void logSendOperation( ChangeSet changeSet ) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Process {0} sending to cluster '{1}' {2} changes on workspace {3} made by {4} from process '{5}' at {6}",
                         clusteringService.processId(),
                         clusteringService.clusterName(),
                         changeSet.size(),
                         changeSet.getWorkspaceName(),
                         changeSet.getUserData(),
                         changeSet.getProcessKey(),
                         changeSet.getTimestamp());
        }
    }

    protected final void logReceivedOperation( ChangeSet changeSet ) {
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Process {0} received on cluster '{1}' {2} changes on workspace {3} made by {4} from process '{5}' at {6}",
                         clusteringService.processId(),
                         clusteringService.clusterName(),
                         changeSet.size(),
                         changeSet.getWorkspaceName(),
                         changeSet.getUserId(),
                         changeSet.getProcessKey(),
                         changeSet.getTimestamp());

        }
    }

    @Override
    public boolean register( ChangeSetListener observer ) {
        return delegate.register(observer);
    }

    @Override
    public boolean unregister( ChangeSetListener observer ) {
        return delegate.unregister(observer);
    }
}
