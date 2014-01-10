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

import java.io.Serializable;
import java.util.List;

/**
 * Class which contains the messages send in a cluster between members of the cluster when attempting delta reconciliation.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public abstract class DeltaMessage implements Serializable {

    private static final long serialVersionUID = 1L;

    private final String journalId;

    protected DeltaMessage( String journalId ) {
        this.journalId = journalId;
    }

    protected String getJournalId() {
        return journalId;
    }

    static DeltaRequest request(String journalId, long lastChangeSetTimeMillis) {
        return new DeltaRequest(journalId, lastChangeSetTimeMillis);
    }

    static DeltaStillReconciling stillReconciling(String journalId) {
        return new DeltaStillReconciling(journalId);
    }

    static DeltaResponse response(String journalId, List<JournalRecord> records) {
        return new DeltaResponse(journalId, records);
    }

    protected static class DeltaRequest extends DeltaMessage {
        private static final long serialVersionUID = 1L;

        private final long lastChangeSetTimeMillis;

        public DeltaRequest( String journalId,
                             long lastChangeSetTimeMillis ) {
            super(journalId);
            this.lastChangeSetTimeMillis = lastChangeSetTimeMillis;
        }

        protected long getLastChangeSetTimeMillis() {
            return lastChangeSetTimeMillis;
        }
    }

    protected static class DeltaResponse extends DeltaMessage {
        private static final long serialVersionUID = 1L;

        private final List<JournalRecord> records;

        protected DeltaResponse( String journalId,
                              List<JournalRecord> records ) {
            super(journalId);
            this.records = records;
        }

        protected List<JournalRecord> getRecords() {
            return records;
        }
    }

    protected static class DeltaStillReconciling extends DeltaMessage {
        private static final long serialVersionUID = 1L;

        protected DeltaStillReconciling( String journalId ) {
            super(journalId);
        }
    }
}
