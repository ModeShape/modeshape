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
import java.util.Date;
import java.util.Iterator;
import java.util.Set;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.change.Change;
import org.modeshape.jcr.cache.change.ChangeSet;

/**
 * A record stored by the {@link LocalJournal}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class JournalRecord implements Serializable, Comparable<JournalRecord>, Iterable<Change> {

    private static final long serialVersionUID = 1L;

    /**
     * The time in UTC millis when this record was created.
     */
    private long createdTimeMillisUTC;

    /**
     * The content of the record.
     */
    private ChangeSet content;

    protected JournalRecord( ChangeSet content ) {
        this.content = content;
        this.createdTimeMillisUTC = -1;
    }

    protected JournalRecord( long createdTimeMillisUTC,
                             ChangeSet content ) {
        this.createdTimeMillisUTC = createdTimeMillisUTC;
        this.content = content;
    }

    /**
     * @return the time when this record was created
     */
    public long getCreatedTimeMillisUTC() {
        return createdTimeMillisUTC;
    }

    /**
     * @see org.modeshape.jcr.cache.change.ChangeSet#getProcessKey()
     */
    public String getProcessKey() {
        return content.getProcessKey();
    }

    /**
     * @see org.modeshape.jcr.cache.change.ChangeSet#getRepositoryKey()
     */
    public String getRepositoryKey() {
        return content.getRepositoryKey();
    }

    /**
     * @see org.modeshape.jcr.cache.change.ChangeSet#getUserId()
     */
    public String getUserId() {
        return content.getUserId();
    }

    /**
     * @see org.modeshape.jcr.cache.change.ChangeSet#getWorkspaceName()
     */
    public String getWorkspaceName() {
        return content.getWorkspaceName();
    }

    /**
     * Returns the time of the changeset, in UTC millis.
     * @return a timestamp
     * @see org.modeshape.jcr.cache.change.ChangeSet#getTimestamp()
     */
    public long getChangeTimeMillis() {
        return content.getTimestamp().getMillisecondsInUtc();
    }

    /**
     * @see org.modeshape.jcr.cache.change.ChangeSet#changedNodes()
     */
    public Set<NodeKey> changedNodes() {
        return content.changedNodes();
    }

    /**
     * @see org.modeshape.jcr.cache.change.ChangeSet#getJournalId()
     */
    public String getJournalId() {
        return content.getJournalId();
    }

    @Override
    public Iterator<Change> iterator() {
        return content.iterator();
    }

    @Override
    public boolean equals( Object o ) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        JournalRecord record = (JournalRecord)o;

        if (createdTimeMillisUTC != record.createdTimeMillisUTC) {
            return false;
        }

        if (content != null ? !content.getUUID().equals(record.content.getUUID()) : record.content != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = (int)(createdTimeMillisUTC ^ (createdTimeMillisUTC >>> 32));
        result = 31 * result + (content != null ? content.getUUID().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("JournalRecord {");
        sb.append("createdTime=").append(new Date(createdTimeMillisUTC).toString());
        sb.append(", journalId=").append(getJournalId());
        sb.append(", processKey=").append(getProcessKey());
        sb.append(", userId=").append(getUserId());
        sb.append(", repositoryKey=").append(getRepositoryKey());
        sb.append(", workspaceName=").append(getWorkspaceName());
        sb.append(", changeTime=").append(new Date(getChangeTimeMillis()).toString());
        sb.append('}');
        return sb.toString();
    }

    @Override
    public int compareTo( JournalRecord o ) {
        if (o == null) {
            return 1;
        }
        return Long.valueOf(createdTimeMillisUTC).compareTo(o.createdTimeMillisUTC);
    }

    protected JournalRecord withCreatedTimeMillisUTC( final long createdTimeMillisUTC ) {
        this.createdTimeMillisUTC = createdTimeMillisUTC;
        return this;
    }

    protected static JournalRecord searchBound(long createdTimeMillisUTC) {
        return new JournalRecord(createdTimeMillisUTC, null);
    }
}
