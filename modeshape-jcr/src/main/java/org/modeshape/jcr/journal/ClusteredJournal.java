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

package org.modeshape.jcr.journal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.infinispan.schematic.document.ThreadSafe;
import org.joda.time.DateTime;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.cache.NodeKey;
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
public class ClusteredJournal extends MessageConsumer<ClusteredJournal.DeltaMessage> implements ChangeJournal {

    private final static Logger LOGGER  = Logger.getLogger(ClusteredJournal.class);
    private final static int MAX_MINUTES_TO_WAIT_FOR_RECONCILIATION = 2;

    private final LocalJournal localJournal;
    private final ClusteringService clusteringService;
    
    private CountDownLatch reconciliationLatch = null;

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

        //make sure this process can always process delta messages
        clusteringService.addConsumer(this);
        
        if (!clusteringService.multipleMembersInCluster()) {
            // this is the first node of the cluster, nothing to do
            return;
        }

        // we require just 1 response before unblocking for a couple of reasons:
        // a) partition tolerance is NOT SUPPORTED
        // b) each member of the cluster has a full view of all the changes throughout that cluster, thanks to remote events
        // we'll process all responses eventually, but we only block for the first one
        int numberOfRequiredResponses = 1;
        this.reconciliationLatch = new CountDownLatch(numberOfRequiredResponses);
        
        // send the request
        JournalRecord lastRecord = lastRecord();
        Long lastChangeSetTimeMillis = lastRecord != null ? lastRecord.getChangeTimeMillis() : null;
        DeltaMessage request = DeltaMessage.request(journalId(), lastChangeSetTimeMillis);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Sending delta request: {0}", request);
        }
        this.clusteringService.sendMessage(request);
        waitForReconciliationToComplete();
    }

    private void waitForReconciliationToComplete() throws InterruptedException {
        try {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{0} waiting until it receives {1} responses from cluster {2}", journalId(), 
                             reconciliationLatch.getCount(),
                             clusterName());
            }
            if (!reconciliationLatch.await(MAX_MINUTES_TO_WAIT_FOR_RECONCILIATION, TimeUnit.MINUTES)) {
                LOGGER.warn(JcrI18n.journalHasNotCompletedReconciliation,journalId(), clusterName(), MAX_MINUTES_TO_WAIT_FOR_RECONCILIATION);
            } else if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("{0} successfully completed reconciliation", journalId());
            }
        } catch (InterruptedException e) {
            LOGGER.warn(JcrI18n.journalHasNotCompletedReconciliation, journalId(), clusterName(),
                        MAX_MINUTES_TO_WAIT_FOR_RECONCILIATION);
            if (Thread.interrupted()) {
                throw e;
            }
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
    public JournalRecord lastRecord() {
        return localJournal.lastRecord();
    }

    @Override
    public Records recordsNewerThan( DateTime changeSetTime,
                                     boolean inclusive,
                                     boolean descendingOrder ) {
        return localJournal.recordsNewerThan(changeSetTime, inclusive, descendingOrder);
    }

    @Override
    public Iterator<NodeKey> changedNodesSince( long timestamp ) {
        return localJournal.changedNodesSince(timestamp);
    }

    @Override
    public void addRecords( JournalRecord... records ) {
        localJournal.addRecords(records);
    }

    @Override
    public String journalId() {
        return localJournal.journalId();
    }

    @Override
    public boolean started() {
        return localJournal.started() && reconciliationCompleted();
    }

    @Override
    public void consume( ClusteredJournal.DeltaMessage message ) {
        if (!localJournal.started()) {
            return;
        }
        if (message.isResponse()) {
            processDeltaResponse(message);
        } else {
            processDeltaRequest(message);
        }
    }
    
    protected boolean reconciliationCompleted() {
        return reconciliationLatch == null || reconciliationLatch.getCount() == 0;
    }

    private void processDeltaRequest(DeltaMessage request) {
        String requestorId = request.getRequestorId();
        String journalId = journalId();
        if (requestorId.equals(journalId)) {
            //we MUST discard own messages, because JGroups will broadcast these as well
            LOGGER.debug("{0} discarding delta request from itself", journalId);
            return;
        }

        if (!reconciliationCompleted()) {
            //if this clustered journal has not completed reconciling itself, it cannot send anything
            LOGGER.debug("{0} is still reconciling, cannot send delta to journal {1}", journalId, requestorId);
            return;
        }

        Long requestorLastChangeSetTime = request.getRequestorLastChangeSetTime();
        DateTime lastChangeSetTime = requestorLastChangeSetTime != null ? new DateTime(requestorLastChangeSetTime) : null;
        Records delta = recordsNewerThan(lastChangeSetTime, false, false);
        List<JournalRecord> deltaList = new ArrayList<>(delta.size());
        for (JournalRecord record : delta) {
            deltaList.add(record);
        }
        
        DeltaMessage response = DeltaMessage.response(request, journalId, deltaList);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Sending delta response {0} to journal {1}", response, requestorId);
        }
        clusteringService.sendMessage(response);
    }

    private void processDeltaResponse(DeltaMessage message) {
        String journalId = journalId();
        if (!journalId.equals(message.getRequestorId())) {
            // only process a response if the message is a response to our request (in a cluster everything will be broadcasted to everyone)
            return;
        }
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("{0} received delta response {1}", journalId, message);
        }
        List<JournalRecord> records = message.getRespondentRecords();
        if (!records.isEmpty()) {
            //make sure that a new timestamp is not generated for those records and whatever comes in the response is used.
            localJournal.addRecords(records.toArray(new JournalRecord[0]));
        }
        reconciliationLatch.countDown();     
    }

    protected ClusteringService clusteringService() {
        return ClusteredJournal.this.clusteringService;
    }

    protected LocalJournal localJournal() {
        return localJournal;
    }

    protected String clusterName() {
        return clusteringService().clusterName();
    }
    
    protected static class DeltaMessage implements Serializable {
        private static final long serialVersionUID = 1L;
        
        private final String requestorId;
        private final Long requestorLastChangeSetTime;
        private final String respondentId;
        private final List<JournalRecord> respondentRecords;

        private DeltaMessage( String requestorId,
                              Long requestorLastChangeSetTime,
                              String respondentId,
                              List<JournalRecord> respondentRecords ) {
            this.requestorId = requestorId;
            this.requestorLastChangeSetTime = requestorLastChangeSetTime;
            this.respondentId = respondentId;
            this.respondentRecords = respondentRecords;
        }
        
        protected boolean isResponse() {
            return this.respondentId != null;
        }

        protected String getRequestorId() {
            return requestorId;
        }

        protected Long getRequestorLastChangeSetTime() {
            return requestorLastChangeSetTime;
        }

        protected String getRespondentId() {
            return respondentId;
        }

        protected List<JournalRecord> getRespondentRecords() {
            return respondentRecords;
        }

        protected static DeltaMessage request(String requestorId, Long requestorLastChangeSetTime) {
            return new DeltaMessage(requestorId, requestorLastChangeSetTime, null, null);
        }

        protected static DeltaMessage response(DeltaMessage request, String repondentId, List<JournalRecord> respondentRecords) {
            return new DeltaMessage(request.requestorId, request.requestorLastChangeSetTime, repondentId, respondentRecords);
        }

        @Override
        public String toString() {
            StringBuilder sb = null;
            if (isResponse()) {
                sb = new StringBuilder("response[");
                sb.append("requestorId='").append(requestorId).append('\'');
                sb.append(", requestorLastChangeSetTime=").append(requestorLastChangeSetTime);
                sb.append(", repondentId='").append(respondentId).append('\'');
                sb.append(", respondentRecords=").append(respondentRecords);
            } else {
                sb = new StringBuilder("request[");
                sb.append("requestorId='").append(requestorId).append('\'');
                sb.append(", requestorLastChangeSetTime=").append(requestorLastChangeSetTime);                
            }
            sb.append(']');
            return sb.toString();
        }
    }
}