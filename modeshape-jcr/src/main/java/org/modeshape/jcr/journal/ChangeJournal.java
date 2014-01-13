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

import java.util.Collections;
import java.util.Iterator;
import javax.jcr.RepositoryException;
import org.joda.time.DateTime;
import org.modeshape.jcr.cache.change.ChangeSetListener;

/**
 * An entity which records all changes which occur in a repository via {@code ChangeSet} instances.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public interface ChangeJournal extends ChangeSetListener {

    /**
     * Starts this journal instance.
     *
     * @throws RepositoryException if anything fails during start
     */
    public void start() throws Exception;

    /**
     * Stops the journal.
     */
    public void shutdown();

    /**
     * Removes older entries. Each journal is free to decide the semantics behind "old".
     */
    public void removeOldRecords();

    /**
     * Returns all the records this journal holds
     *
     * @param descendingOrder flag which indicates whether the entries
     * @return a {@link Records} instance
     */
    public Records allRecords( boolean descendingOrder );

    /**
     * Returns the records for a given journal.
     *
     * @param journalId a {@link String} the id of a journal; must not be {@link null}
     * @return an {@link Iterable<JournalRecord>}, never {@code null}
     */
    public Iterable<JournalRecord> recordsFor( String journalId );

    /**
     * Returns the last record from the journal.
     * @return either a {@link org.modeshape.jcr.journal.JournalRecord} instance or {@code null} if the journal is empty.
     */
    public JournalRecord lastRecord();

    /**
     * Returns all records that have changesets which are newer than a given timestamp.
     *
     *
     * @param time the {@link org.joda.time.DateTime} of the changes representing the lower bound.
     * @param inclusive flag indicating whether the timestamp should be used inclusively or exclusively
     * @param descendingOrder flag indicating if the records should be returned in ascending order (oldest to newest) or descending
     * order (newest to oldest)
     * @return a {@link Records} instance; never {@code null}
     */
    public Records recordsNewerThan( DateTime time,
                                     boolean inclusive,
                                     boolean descendingOrder );

    /**
     * Adds one or more journal records to a journal.
     *
     * @param records a {@link JournalRecord} array.
     */
    public void addRecords( JournalRecord... records );

    /**
     * Checks if the journal has finished reconciling its record with deltas received from journals belonging to other processes.
     *
     * @return {@code true} if the reconciliation has completed, {@code false} otherwise.
     */
    public boolean deltaReconciliationCompleted();

    /**
     * Returns the id of this change journal.
     *
     * @return a {@link String}, never {@code null}
     */
    public String journalId();

    /**
     * An {@link Iterable<JournalRecord> extension which provides information about the number of entries the underlying collection
     * holds.
     */
    public interface Records extends Iterable<JournalRecord> {
        public static final Records EMPTY = new Records() {
            @Override
            public int size() {
                return 0;
            }

            @Override
            public Iterator<JournalRecord> iterator() {
                return Collections.<JournalRecord>emptySet().iterator();
            }

            @Override
            public boolean isEmpty() {
                return true;
            }
        };

        /**
         * Returns the number of records.
         *
         * @return an int
         */
        public int size();

        /**
         * Returns true if the records set is empty.
         *
         * @return {@code true} if there aren't any records.
         */
        public boolean isEmpty();
    }
}
