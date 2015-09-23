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

import java.util.Collections;
import java.util.Iterator;
import org.joda.time.DateTime;
import org.modeshape.jcr.cache.NodeKey;
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
     * @throws Exception if anything fails during start
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
     * Returns the last record from the journal.
     * 
     * @return either a {@link org.modeshape.jcr.journal.JournalRecord} instance or {@code null} if the journal is empty.
     */
    public JournalRecord lastRecord();

    /**
     * Returns all records that have changesets which are newer than a given timestamp.
     * 
     * @param changeSetTime the {@link org.joda.time.DateTime} of the changes representing the lower bound; may be null indicating
     *        that *all the records* should be returned.
     * @param inclusive flag indicating whether the timestamp should be used inclusively or exclusively
     * @param descendingOrder flag indicating if the records should be returned in ascending order (oldest to newest) or
     *        descending order (newest to oldest)
     * @return a {@link Records} instance; never {@code null}
     */
    public Records recordsNewerThan( DateTime changeSetTime,
                                     boolean inclusive,
                                     boolean descendingOrder);

    /**
     * Returns the node keys which are part of change sets that are newer or equal to a given timestamp.
     * 
     * @param timestamp the timestamp of the changes representing the lower bound; 
     * @return an {@link Iterator} of {@link NodeKey}instances; never {@code null}, but may contain duplicate keys if the 
     * underl
     */
    public Iterator<NodeKey> changedNodesSince( long timestamp );

    /**
     * Adds one or more journal records to a journal.
     * 
     * @param records a {@link JournalRecord} array.
     */
    public void addRecords( JournalRecord... records );

    /**
     * Returns the id of this change journal.
     * 
     * @return a {@link String}, never {@code null}
     */
    public String journalId();

    /**
     * Checks if this journal is active (i.e. can accept requests) or not.
     *
     * @return {@code true} if the journal has started, {@code false otherwise}
     */
    public boolean started();

    /**
     * An {@link Iterable} extension which provides information about the number of entries the underlying collection holds.
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
