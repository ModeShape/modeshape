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
public final class JournalRecord implements Serializable, Iterable<Change> {

    private static final long serialVersionUID = 1L;

    /**
     * The time based key for this journal record.
     */
    private long timeBasedKey;

    /**
     * The content of the record.
     */
    private final ChangeSet content;

    protected JournalRecord( ChangeSet content ) {
        this.content = content;
        this.timeBasedKey = -1;
    }

    protected JournalRecord withTimeBasedKey( final long timeBasedKey ) {
        this.timeBasedKey = timeBasedKey;
        return this;
    }

    /**
     * @return the time when this record was created
     */
    public long getTimeBasedKey() {
        return timeBasedKey;
    }

    /**
     * Get the key of the process in which the changes were made.
     * 
     * @return the process key; never null
     * @see org.modeshape.jcr.cache.change.ChangeSet#getProcessKey()
     */
    public String getProcessKey() {
        return content.getProcessKey();
    }

    /**
     * Get the key of the repository in which the changes were made.
     * 
     * @return the repository key; never null
     * @see org.modeshape.jcr.cache.change.ChangeSet#getRepositoryKey()
     */
    public String getRepositoryKey() {
        return content.getRepositoryKey();
    }

    /**
     * Returns the ID (username) of the user which performed the changes.
     * 
     * @return a {@link String} representing the username; may be {@code null} in the case of changes performed "by the system.
     * @see org.modeshape.jcr.cache.change.ChangeSet#getUserId()
     */
    public String getUserId() {
        return content.getUserId();
    }

    /**
     * Get the name of the workspace in which the changes were made.
     * 
     * @return the workspace name; may be null only when workspaces are added or removed
     * @see org.modeshape.jcr.cache.change.ChangeSet#getWorkspaceName()
     */
    public String getWorkspaceName() {
        return content.getWorkspaceName();
    }

    /**
     * Returns the time of the changeset, in UTC millis.
     * 
     * @return a timestamp
     * @see org.modeshape.jcr.cache.change.ChangeSet#getTimestamp()
     */
    public long getChangeTimeMillis() {
        return content.getTimestamp().getMillisecondsInUtc();
    }

    /**
     * Returns the set of keys for the nodes which has been changed.
     * 
     * @return the set of changed node keys; never {@code null}
     * @see org.modeshape.jcr.cache.change.ChangeSet#changedNodes()
     */
    public Set<NodeKey> changedNodes() {
        return content.changedNodes();
    }

    /**
     * Returns the identifier of the local {@link org.modeshape.jcr.journal.ChangeJournal} instance.
     * 
     * @return either a non-null {@link String} if journaling is enabled, or {@code null} if journaling isn't enabled.
     * @see org.modeshape.jcr.cache.change.ChangeSet#getJournalId()
     */
    public String getJournalId() {
        return content.getJournalId();
    }

    /**
     * Returns the underlying change set.
     *
     * @return {@link org.modeshape.jcr.cache.change.ChangeSet} instance; never null
     */
    public ChangeSet getChangeSet() {
        return this.content;
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

        return content.getUUID().equals(record.content.getUUID());

    }

    @Override
    public int hashCode() {
        return content.getUUID().hashCode();
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("JournalRecord {");
        sb.append("timeBasedKey=").append(new Date(timeBasedKey).toString());
        sb.append(", journalId=").append(getJournalId());
        sb.append(", processKey=").append(getProcessKey());
        sb.append(", userId=").append(getUserId());
        sb.append(", repositoryKey=").append(getRepositoryKey());
        sb.append(", workspaceName=").append(getWorkspaceName());
        sb.append(", content=").append(content.toString());
        sb.append('}');
        return sb.toString();
    }
}
