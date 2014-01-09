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

import java.util.Collections;
import java.util.Iterator;
import javax.jcr.RepositoryException;
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
     * Returns all records which are older than a given timestamp.
     *
     * @param localMillis a timestamp in local milliseconds.
     * @param inclusive flag indicating whether the timestamp should be used inclusively or exclusively
     * @param descendingOrder flag indicating if the records should be returned in ascending order (oldest to newest) or descending
     * order (newest to oldest)
     * @return a {@link Records} instance; never {@code null}
     */
    public Records recordsOlderThan( long localMillis,
                                     boolean inclusive,
                                     boolean descendingOrder );

    /**
     * Returns all the records from all the processes this journal has, since last seeing the process with the given journal id.
     *
     * @param journalId a {@link String} the id of a journal belonging to a process for which to compute the delta; must not be {@link null}
     * @param descendingOrder flag indicating if the records should be returned in ascending order (oldest to newest) or descending
     * order (newest to oldest)
     * @return a {@link Records} instance; never {@code null}. This will only contain records for other processes than {@code journalId}
     */
    public Records recordsDelta( String journalId,
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
        };

        /**
         * Returns the number of items this iterable has.
         *
         * @return an int
         */
        public int size();
    }
}
