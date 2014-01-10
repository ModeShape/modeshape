/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.modeshape.jcr.journal;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.infinispan.schematic.document.ThreadSafe;
import org.modeshape.common.SystemFailureException;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.JcrI18n;
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
public class ClusteredJournal extends MessageConsumer<DeltaMessage> implements ChangeJournal {

    private final static Logger LOGGER  = Logger.getLogger(ClusteredJournal.class);
    private final static long MAX_TIME_TO_WAIT_FOR_RECONCILIATION = TimeUnit.MINUTES.toMillis(5);

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
        super(DeltaMessage.class);

        CheckArg.isNotNull(localJournal, "localJournal");
        CheckArg.isNotNull(clusteringService, "clusteringService");

        this.clusteringService = clusteringService;
        this.localJournal = localJournal.withSearchTimeDelta(clusteringService.getMaxAllowedClockDelayMillis());
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
        long lastChangeSetTime = -1;
        //get all the existing records in reverse order
        Records allLocalRecords = localJournal.allRecords(true);
        if (allLocalRecords.size() > 0) {
            lastChangeSetTime = allLocalRecords.iterator().next().getChangeTimeMillisUTC();
        }

        //make sure this process can always process delta messages
        clusteringService.addConsumer(this);

        if (clusteringService.multipleMembersInCluster()) {
            deltaReconciliationCompleted.set(false);
            // we expect to receive delta responses equal to how many members the cluster has
            this.expectedNumberOfDeltaResponses.compareAndSet(0, clusteringService.membersInCluster() - 1);
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Sending delta request from journal {0} as part of cluster {1}", journalId(), clusterName());
            }
            clusteringService.sendMessage(DeltaMessage.request(journalId(), lastChangeSetTime));
            waitForReconciliationToComplete();
        } else {
            this.deltaReconciliationCompleted.set(true);
        }
    }

    private void waitForReconciliationToComplete() {
        long startedAt = System.currentTimeMillis();
        //wait a predefined amount of time for delta reconciliation to complete
        while (!deltaReconciliationCompleted() && (System.currentTimeMillis() - startedAt) <= MAX_TIME_TO_WAIT_FOR_RECONCILIATION) {
            // Otherwise, sleep for a short bit ...
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.interrupted();
                break;
            }
        }

        if (!deltaReconciliationCompleted()) {
            throw new SystemFailureException(JcrI18n.journalHasNotCompletedReconciliation.text(journalId(), clusterName(),
                                                                                               MAX_TIME_TO_WAIT_FOR_RECONCILIATION));
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
    public Records recordsNewerThan( long changeSetTimeMillis,
                                     boolean inclusive,
                                     boolean descendingOrder ) {
        return localJournal.recordsNewerThan(changeSetTimeMillis, inclusive, descendingOrder);
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

    @Override
    public void consume( DeltaMessage message ) {
        if (message instanceof DeltaMessage.DeltaRequest)  {
            processDeltaRequest((DeltaMessage.DeltaRequest)message);
            return;
        }
        if (message instanceof DeltaMessage.DeltaResponse) {
            processDeltaResponse((DeltaMessage.DeltaResponse)message);
            return;
        }
        if (message instanceof DeltaMessage.DeltaStillReconciling) {
            processStillReconciling();
        }
    }

    private void processStillReconciling() {
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Journal {0} says it's still reconciling");
        }
        //we've received a response, but the node from which we've received the response is still reconciling
        if (expectedNumberOfDeltaResponses().decrementAndGet() == 0) {
            markDeltaReconciliationAsCompleted();
        }
    }

    private void processDeltaRequest(DeltaMessage.DeltaRequest request) {
        if (request.getJournalId().equals(journalId())) {
            //we MUST discard own messages, because JGroups will broadcast these as well
            LOGGER.debug("Journal {0} discarding delta request from itself", journalId());
            return;
        }

        if (!deltaReconciliationCompleted()) {
            //if this clustered journal has not completed reconciling itself, it cannot send anything
            LOGGER.debug("Journal {0} is still reconciling, cannot send delta to journal {1}", journalId(), request.getJournalId());
            clusteringService.sendMessage(DeltaMessage.stillReconciling(journalId()));
            return;
        }

        Records delta = recordsNewerThan(request.getLastChangeSetTimeMillis(), false, false);
        List<JournalRecord> deltaList = new ArrayList<JournalRecord>(delta.size());
        for (JournalRecord record : delta) {
            deltaList.add(record);
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Journal {0} sending delta response to journal {1} as part of cluster {2}. Delta size is {3}",
                         journalId(),
                         request.getJournalId(),
                         clusterName(),
                         delta.size());
        }
        clusteringService.sendMessage(DeltaMessage.response(journalId(), deltaList));
    }

    private void processDeltaResponse(DeltaMessage.DeltaResponse response) {
        List<JournalRecord> records = response.getRecords();
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Journal {0} received delta response from journal {1} as part of cluster {2}. Delta size: {3} records.",
                         journalId(),
                         response.getJournalId(),
                         clusterName(),
                         records.size());
        }
        //we've received a response, so decrement the number of responses we're still expecting to get
        int remainingNumberOfExpectedResponses = expectedNumberOfDeltaResponses().decrementAndGet();
        if (remainingNumberOfExpectedResponses == 0) {
            markDeltaReconciliationAsCompleted();
        }

        if (!records.isEmpty()) {
            //make sure that a new timestamp is not generated for those records and whatever comes in the response is used.
            localJournal.addRecords(records.toArray(new JournalRecord[0]));
        }
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
}