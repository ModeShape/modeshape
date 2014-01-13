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

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.jcr.RepositoryException;
import org.infinispan.schematic.document.ThreadSafe;
import org.joda.time.DateTime;
import org.mapdb.Atomic;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.cache.change.ChangeSet;

/**
 * An append only journal implementation which stores each {@link ChangeSet} (either local or remove) on the local FS.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@ThreadSafe
public class LocalJournal implements ChangeJournal {
    private static final Logger LOGGER = Logger.getLogger(LocalJournal.class);

    private static final ReadWriteLock RW_LOCK = new ReentrantReadWriteLock(true);
    private static final int DEFAULT_MAX_TIME_TO_KEEP_FILES = -1;
    private static final String RECORDS_FIELD = "records";
    private static final String JOURNAL_ID_FIELD = "journalId";

    private final String journalLocation;
    private final boolean asyncWritesEnabled;
    private final long maxTimeToKeepEntriesMillis;

    private String journalId;
    private DB journalDB;
    private NavigableSet<JournalRecord> records;
    private long searchTimeDelta;
    private volatile boolean stopped = false;

    /**
     * Creates a new journal instance, in stopped state.
     *
     * @param journalLocation the folder location on the FS where the entries should be saved. Must not be {@code null}
     * @param asyncWritesEnabled flag which indicates if disk write should be asynchronous or not.
     * @param maxDaysToKeepEntries the maximum number of days this journal should store entries on disk. A negative value or 0
     */
    public LocalJournal( String journalLocation,
                         boolean asyncWritesEnabled,
                         int maxDaysToKeepEntries ) {
        CheckArg.isNotNull(journalLocation, "journalLocation");

        this.journalLocation = journalLocation;
        this.asyncWritesEnabled = asyncWritesEnabled;
        this.maxTimeToKeepEntriesMillis = TimeUnit.DAYS.toMillis(maxDaysToKeepEntries);
        this.stopped = true;
        this.searchTimeDelta = TimeUnit.SECONDS.toMillis(1);
    }

    protected LocalJournal( String journalLocation ) {
        this(journalLocation, false, DEFAULT_MAX_TIME_TO_KEEP_FILES);
    }

    @SuppressWarnings( "rawtypes" )
    @Override
    public void start() throws RepositoryException {
        if (!stopped) {
            return;
        }
        RW_LOCK.writeLock().lock();
        try {
            File journalFileLocation = new File(journalLocation);
            if (!journalFileLocation.exists()) {
                assert journalFileLocation.mkdirs();
            }
//            DBMaker dbMaker = DBMaker.newAppendFileDB(new File(journalFileLocation, RECORDS_FIELD))
//                                     .compressionEnable()
//                                     .checksumEnable()
//                                     .closeOnJvmShutdown()
//                                     .snapshotEnable();
            DBMaker dbMaker = DBMaker.newFileDB(new File(journalFileLocation, RECORDS_FIELD))
                                     .compressionEnable()
                                     .checksumEnable()
                                     .closeOnJvmShutdown()
                                     .mmapFileEnableIfSupported()
                                     .snapshotEnable();
            if (asyncWritesEnabled) {
                dbMaker.asyncWriteEnable();
            }
            this.journalDB = dbMaker.make();
            this.records = this.journalDB.createTreeSet(RECORDS_FIELD).counterEnable().makeOrGet();
            Atomic.String journalAtomic = this.journalDB.getAtomicString(JOURNAL_ID_FIELD);
            //only write the value the first time
            if (StringUtil.isBlank(journalAtomic.get())) {
                journalAtomic.set("journal_" + UUID.randomUUID().toString());
            }
            this.journalId = journalAtomic.get();
            this.stopped = false;
        } catch (Exception e) {
            throw new RepositoryException(JcrI18n.cannotStartJournal.text(), e);
        }  finally {
            RW_LOCK.writeLock().unlock();
        }
    }

    @Override
    public void shutdown() {
        if (this.stopped) {
            return;
        }
        RW_LOCK.writeLock().lock();
        this.stopped = true;
        try {
            this.journalDB.commit();
            this.journalDB.close();
        } catch (Exception e) {
            LOGGER.error(e, JcrI18n.cannotStopJournal);
        } finally {
            RW_LOCK.writeLock().unlock();
        }
    }

    @Override
    public void notify( ChangeSet changeSet ) {
        //do not store records from jcr:system from other journals because that would wreak havoc for delta calculation.
        //If stored, these changes would be received in a cluster when a foreign node comes up.
        boolean systemWorkspaceChanges = changeSet.getWorkspaceName().equalsIgnoreCase(RepositoryConfiguration.SYSTEM_WORKSPACE_NAME);
        if (changeSet.isEmpty() || systemWorkspaceChanges) {
            return;
        }
        addRecords(new JournalRecord(changeSet));
    }

    @Override
    public void addRecords( JournalRecord... records ) {
        if (stopped) {
            return;
        }
        RW_LOCK.writeLock().lock();
        try {
            LOGGER.debug("Adding {0} records", records.length);
            for (JournalRecord record : records) {
                if (record.getCreatedTimeMillisUTC() < 0) {
                    //generate a unique timestamp only if there isn't one. In some scenarios (i.e. running in a cluster) we
                    //always want to keep the original TS
                    long createTimeMillisUTC = uniqueCreatedTimeMillisUTC();
                    record.withCreatedTimeMillisUTC(createTimeMillisUTC);
                }
                this.records.add(record);
            }
            this.journalDB.commit();
        } finally {
            RW_LOCK.writeLock().unlock();
        }
    }

    @Override
    public void removeOldRecords() {
        //perform cleanup
        removeRecordsOlderThan(System.currentTimeMillis() - this.maxTimeToKeepEntriesMillis);
    }

    @Override
    public boolean deltaReconciliationCompleted() {
        return true;
    }

    protected synchronized void removeRecordsOlderThan( long millisInUtc ) {
        RW_LOCK.writeLock().lock();
        try {
            if (millisInUtc <= 0 || stopped) {
                return;
            }
            LOGGER.debug("Removing records older than " + millisInUtc);
            SortedSet<JournalRecord> toRemove = this.records.headSet(JournalRecord.searchBound(millisInUtc));
            toRemove.clear();
            journalDB.commit();
            journalDB.compact();
        } finally {
            RW_LOCK.writeLock().unlock();
        }
    }

    protected String getJournalLocation() {
        return journalLocation;
    }

    private long uniqueCreatedTimeMillisUTC() {
        long createTimeMillisUTC = System.currentTimeMillis();
        JournalRecord recordSameTimestamp = JournalRecord.searchBound(createTimeMillisUTC);
        while (records.contains(recordSameTimestamp)) {
            createTimeMillisUTC = System.currentTimeMillis();
            recordSameTimestamp.withCreatedTimeMillisUTC(createTimeMillisUTC);
        }
        return createTimeMillisUTC;
    }

    @Override
    public Records allRecords( boolean descendingOrder ) {
        return recordsFromSet(records, descendingOrder);
    }

    @Override
    public Iterable<JournalRecord> recordsFor( String journalId ) {
        if (stopped) {
            return Collections.emptySet();
        }
        CheckArg.isNotNull("journalId cannot be null", journalId);
        return filter(records.iterator(), journalId);
    }

    @Override
    public JournalRecord lastRecord() {
        return this.records == null || this.records.isEmpty() ? null : this.records.last();
    }

    @Override
    public Records recordsNewerThan( DateTime time,
                                     boolean inclusive,
                                     boolean descendingOrder ) {
        if (stopped) {
            return Records.EMPTY;
        }
        long millisUTC = time != null ? time.getMillis() : -1;
        //adjust the millis using a delta so that we are sure we catch everything because we only search using the "created time"
        //not the "change set" time

        JournalRecord bound = JournalRecord.searchBound(millisUTC - searchTimeDelta);
        NavigableSet<JournalRecord> subset = records.tailSet(bound, true);

        //process each of the records from the result and look at the timestamp of the changeset, so that we're sure we only include
        //the correct ones (we used a delta to make sure we get everything)
        JournalRecord startRecord = null;
        for (JournalRecord record : subset) {
            long journalChangeTimeMillisUTC = record.getChangeTimeMillis();
            if (((journalChangeTimeMillisUTC == millisUTC) && inclusive)
                || journalChangeTimeMillisUTC > millisUTC) {
                startRecord = record;
                break;
            }
        }
        return startRecord != null ? recordsFromSet(subset.tailSet(startRecord, true), descendingOrder) : Records.EMPTY;
    }

    @Override
    public String journalId() {
        return journalId;
    }

    protected LocalJournal withSearchTimeDelta( final long searchTimeDelta ) {
        this.searchTimeDelta = searchTimeDelta;
        return this;
    }

    private Iterable<JournalRecord> filter( final Iterator<JournalRecord> recordsIterator,
                                            final String journalId ) {
        return new Iterable<JournalRecord>() {
            private JournalRecord currentRecord = null;

            public Iterator<JournalRecord> iterator() {
                return new Iterator<JournalRecord>() {
                    @Override
                    public boolean hasNext() {
                        currentRecord = advance();
                        return currentRecord != null;
                    }

                    @Override
                    public JournalRecord next() {
                        if (currentRecord == null) {
                            currentRecord = advance();
                            if (currentRecord == null) {
                                throw new NoSuchElementException();
                            }
                            return currentRecord;
                        } else {
                            JournalRecord next = currentRecord;
                            currentRecord = null;
                            return next;
                        }
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("Never remove from this iterator");
                    }
                };
            }

            private JournalRecord advance() {
                while (recordsIterator.hasNext()) {
                    JournalRecord record = recordsIterator.next();
                    if (record.getJournalId().equals(journalId)) {
                        return record;

                    }
                }
                return null;
            }
        };
    }

    private static Records recordsFromSet( final NavigableSet<JournalRecord> content,
                                           boolean descending ) {
        final Iterator<JournalRecord> originalIterator = !descending ? content.iterator() : content.descendingIterator();
        return new Records() {
            @Override
            public int size() {
                return content.size();
            }

            @Override
            public Iterator<JournalRecord> iterator() {
                return new Iterator<JournalRecord>() {
                    @Override
                    public boolean hasNext() {
                        return originalIterator.hasNext();
                    }

                    @Override
                    public JournalRecord next() {
                        return originalIterator.next();
                    }

                    @Override
                    public void remove() {
                        throw new UnsupportedOperationException("This iterator is read-only");
                    }
                };
            }

            @Override
            public boolean isEmpty() {
                return size() == 0;
            }
        };
    }
}
