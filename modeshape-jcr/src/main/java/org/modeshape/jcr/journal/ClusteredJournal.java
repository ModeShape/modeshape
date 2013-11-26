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

package org.modeshape.jcr.journal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.infinispan.schematic.document.ThreadSafe;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.clustering.ClusteringService;
import org.modeshape.jcr.clustering.MessageConsumer;

/**
 * A {@link ChangeJournal} implementation which runs in a cluster and which attempts to reconcile with other members of the cluster
 * on startup in order to retrieve missed/lost records.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@ThreadSafe
public class ClusteredJournal implements ChangeJournal {

    private final static Logger LOGGER  = Logger.getLogger(ClusteredJournal.class);

    private final LocalJournal localJournal;
    private final ClusteringService clusteringService;
    private final AtomicBoolean deltaReconciliationCompleted;
    private final AtomicInteger expectedNumberOfDeltaResponses;

    /**
     * Creates a new clustered journal
     *
     * @param localJournal the local {@link ChangeJournal} which will
     * @param clusteringService an {@link ClusteringService} instance.
     */
    public ClusteredJournal( LocalJournal localJournal,
                             ClusteringService clusteringService ) {
        this.localJournal = localJournal;
        this.clusteringService = clusteringService;
        this.deltaReconciliationCompleted = new AtomicBoolean(false);
        this.expectedNumberOfDeltaResponses = new AtomicInteger(0);
    }

    @Override
    public void notify( ChangeSet changeSet ) {
        localJournal.notify(changeSet);
    }

    @Override
    public void start() throws Exception {
        // make sure the clustering service is open
        if (!clusteringService.isOpen())  {
            throw new IllegalStateException("The clustering service has not been started");
        }

        localJournal.start();

        //make sure this process can always answer to delta requests
        clusteringService.addConsumer(new DeltaResponse(this));

        if (clusteringService.multipleMembersInCluster()) {
            deltaReconciliationCompleted.set(false);
            // we expect to receive delta responses equal to how many members the cluster has
            this.expectedNumberOfDeltaResponses.compareAndSet(0, clusteringService.membersInCluster() - 1);
            DeltaRequest request = new DeltaRequest(this);
            clusteringService.addConsumer(request);
            request.send();
        } else {
            this.deltaReconciliationCompleted.set(true);
        }
    }

    @Override
    public void shutdown() {
        localJournal.shutdown();
    }

    @Override
    public void removeOldRecords() {
        localJournal.removeOldRecords();
    }

    @Override
    public Records allRecords( boolean descendingOrder ) {
        return localJournal.allRecords(descendingOrder);
    }

    @Override
    public Iterable<JournalRecord> recordsFor( String journalId ) {
        return localJournal.recordsFor(journalId);
    }

    @Override
    public Records recordsOlderThan( long localMillis,
                                     boolean inclusive,
                                     boolean descendingOrder ) {
        return localJournal.recordsOlderThan(localMillis, inclusive, descendingOrder);
    }

    @Override
    public Records recordsDelta( String journalId,
                                 boolean descendingOrder ) {
        return localJournal.recordsDelta(journalId, descendingOrder);
    }

    @Override
    public void addRecords( JournalRecord... records ) {
        localJournal.addRecords(records);
    }

    @Override
    public boolean deltaReconciliationCompleted() {
       return deltaReconciliationCompleted.get();
    }

    @Override
    public String journalId() {
        return localJournal.journalId();
    }

    protected AtomicInteger expectedNumberOfDeltaResponses() {
        return this.expectedNumberOfDeltaResponses;
    }

    protected void markDeltaReconciliationAsCompleted() {
        this.deltaReconciliationCompleted.compareAndSet(false, true);
    }

    protected ClusteringService clusteringService() {
        return ClusteredJournal.this.clusteringService;
    }

    protected LocalJournal localJournal() {
        return localJournal;
    }

    protected String clusterName() {
        return clusteringService().processId();
    }

    private static class DeltaRequest implements Serializable, MessageConsumer<DeltaResponse>  {
        private static final int DELTA_REQUEST = 2;
        private static final long serialVersionUID = 1L;

        private final transient ClusteredJournal clusteredJournal;
        private final String journalId;

        private DeltaRequest(ClusteredJournal clusteredJournal) {
            this.clusteredJournal = clusteredJournal;
            this.journalId = clusteredJournal.journalId();
        }

        @Override
        public boolean interestedIn( int messageType ) {
            return messageType == DeltaResponse.DELTA_RESPONSE;
        }

        @Override
        public void consume( DeltaResponse payload ) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Journal {0} received delta response from journal {1} as part of cluster {2}. Delta size: {3} records.",
                             journalId,
                             payload.journalId,
                             clusteredJournal.clusterName(),
                             payload.journalRecords.size());
            }
            if (clusteredJournal.deltaReconciliationCompleted()) {
                LOGGER.debug("Journal {0} has already completed delta reconciliation, ignoring response from {1}", journalId, payload.journalId);
                //we've already processed the deltas (from another process), so we aren't interested anymore.
                return;
            }
            //we've received a response, so decrement the number of responses we're still expecting to get
            int remainingNumberOfExpectedResponses = clusteredJournal.expectedNumberOfDeltaResponses().decrementAndGet();
            List<JournalRecord> records = payload.journalRecords;
            if (!records.isEmpty()) {
                //the first non-empty delta will be considered successful and the journal entries added
                clusteredJournal.addRecords(records.toArray(new JournalRecord[0]));
                clusteredJournal.markDeltaReconciliationAsCompleted();
            } else if (remainingNumberOfExpectedResponses == 0) {
                clusteredJournal.markDeltaReconciliationAsCompleted();
            }
        }

        protected void send() {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Sending delta request from journal {0} as part of cluster {1}",
                             journalId,
                             clusteredJournal.clusterName());
            }
            clusteredJournal.clusteringService().sendMessage(this, DELTA_REQUEST);
        }
    }

    private static class DeltaResponse implements Serializable, MessageConsumer<DeltaRequest> {
        private static final long serialVersionUID = 1L;
        private static final int DELTA_RESPONSE = 3;

        private final transient ClusteredJournal clusteredJournal;

        private final String journalId;
        private List<JournalRecord> journalRecords;

        private DeltaResponse( ClusteredJournal clusteredJournal ) {
            this.clusteredJournal = clusteredJournal;
            this.journalId = clusteredJournal.journalId();
        }

        @Override
        public boolean interestedIn( int messageType ) {
            return messageType == DeltaRequest.DELTA_REQUEST;
        }

        @Override
        public void consume( DeltaRequest payload ) {
            if (payload.journalId.equals(journalId)) {
                LOGGER.debug("Journal {0} discarding delta request from itself", journalId);
                //we MUST discard own messages, because JGroups will broadcast these as well
                return;
            }
            Records delta = clusteredJournal.recordsDelta(payload.journalId, false);
            this.journalRecords = new ArrayList<JournalRecord>(delta.size());
            for (JournalRecord record : delta) {
                this.journalRecords.add(record);
            }
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Journal {0} sending delta response to journal {1} as part of cluster {2}. Delta size is {3}",
                             journalId,
                             payload.journalId,
                             clusteredJournal.clusterName(),
                             delta.size());
            }
            clusteredJournal.clusteringService().sendMessage(this, DELTA_RESPONSE);
        }
    }
}