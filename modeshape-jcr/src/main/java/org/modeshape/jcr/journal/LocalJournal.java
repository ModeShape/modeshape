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

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import javax.jcr.RepositoryException;
import org.infinispan.schematic.document.ThreadSafe;
import org.joda.time.DateTime;
import org.mapdb.Atomic;
import org.mapdb.BTreeKeySerializer;
import org.mapdb.BTreeMap;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.common.util.TimeBasedKeys;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.change.ChangeSet;

/**
 * An append only journal implementation which stores each {@link ChangeSet} (either local or remove) on the local FS.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@ThreadSafe
public class LocalJournal implements ChangeJournal {
    private static final Logger LOGGER = Logger.getLogger(LocalJournal.class);

    private static final int DEFAULT_MAX_TIME_TO_KEEP_FILES = -1;
    private static final String RECORDS_FIELD = "records";
    private static final String JOURNAL_ID_FIELD = "journalId";
    private static final TimeBasedKeys TIME_BASED_KEYS = TimeBasedKeys.create();

    /**
     * When searching records in the local journal, we want to use a small delta to compensate for the fact that there is slight
     * delay from the point in time when a change set is created (after session.save) to the point when the journal record is added
     * (notify async).
     */
    private static final long DEFAULT_LOCAL_SEARCH_DELTA = TimeUnit.SECONDS.toMillis(1);

    private final String journalLocation;
    private final boolean asyncWritesEnabled;
    private final long maxTimeToKeepEntriesMillis;

    private String journalId;
    private DB journalDB;
    /**
     * The records are a map of {@link org.modeshape.jcr.journal.JournalRecord} instances keyed by a time-based key.
     */
    private BTreeMap<Long, JournalRecord> records;
    private long searchTimeDelta;
    private volatile boolean stopped;

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
        this.searchTimeDelta = DEFAULT_LOCAL_SEARCH_DELTA;
    }

    protected LocalJournal( String journalLocation ) {
        this(journalLocation, false, DEFAULT_MAX_TIME_TO_KEEP_FILES);
    }

    @Override
    public boolean started() {
        return !stopped;
    }

    @SuppressWarnings( "rawtypes" )
    @Override
    public synchronized void start() throws RepositoryException {
        if (!stopped) {
            return;
        }
        try {
            File journalFileLocation = new File(journalLocation);
            if (!journalFileLocation.exists()) {
                boolean folderHierarchyCreated = journalFileLocation.mkdirs();
                assert folderHierarchyCreated;
            }

            /**
             * TODO author=Horia Chiorean date=1/14/14 description=The following should be enabled when append only files are available
            DBMaker dbMaker = DBMaker.newAppendFileDB(new File(journalFileLocation, RECORDS_FIELD))
                                     .compressionEnable()
                                     .checksumEnable()
                                     .closeOnJvmShutdown();
                                     
            */
            DBMaker dbMaker = DBMaker.newFileDB(new File(journalFileLocation, RECORDS_FIELD))
                                     .compressionEnable()
                                     .checksumEnable()
                                     .mmapFileEnableIfSupported()
                                     .closeOnJvmShutdown();
                                     
            if (asyncWritesEnabled) {
                dbMaker.asyncWriteEnable();
            }
            this.journalDB = dbMaker.make();
            this.records = this.journalDB.createTreeMap(RECORDS_FIELD)
                                         .keySerializer(BTreeKeySerializer.ZERO_OR_POSITIVE_LONG)
                                         .counterEnable()
                                         .makeOrGet();
            Atomic.String journalAtomic = this.journalDB.getAtomicString(JOURNAL_ID_FIELD);
            //only write the value the first time
            if (StringUtil.isBlank(journalAtomic.get())) {
                journalAtomic.set("Journal_" + UUID.randomUUID().toString());
            }
            this.journalId = journalAtomic.get();
            this.stopped = false;
        } catch (Exception e) {
            throw new RepositoryException(JcrI18n.cannotStartJournal.text(), e);
        } 
    }

    @Override
    public synchronized void shutdown() {
        if (this.stopped) {
            return;
        }
        
        try {
            this.journalDB.commit();
            this.journalDB.close();
        } catch (Exception e) {
            LOGGER.error(e, JcrI18n.cannotStopJournal);
        } finally {
            this.stopped = true;
        }
    }

    @Override
    public void notify( ChangeSet changeSet ) {
        //do not store records from jcr:system
        boolean systemWorkspaceChanges = RepositoryConfiguration.SYSTEM_WORKSPACE_NAME.equalsIgnoreCase(changeSet.getWorkspaceName());
        if (changeSet.isEmpty() || systemWorkspaceChanges) {
            return;
        }
        addRecords(new JournalRecord(changeSet));
    }

    @Override
    public synchronized void addRecords( JournalRecord... records ) {
        if (stopped) {
            return;
        }
        LOGGER.debug("Adding {0} records", records.length);
        for (JournalRecord record : records) {
            if (record.getTimeBasedKey() < 0) {
                //generate a unique timestamp only if there isn't one. In some scenarios (i.e. running in a cluster) we
                //always want to keep the original TS because otherwise it would be impossible to have a correct order
                //and therefore search
                long createTimeMillisUTC = TIME_BASED_KEYS.nextKey();
                record.withTimeBasedKey(createTimeMillisUTC);
            }
            this.records.put(record.getTimeBasedKey(), record);
        }
        this.journalDB.commit();
    }

    @Override
    public synchronized void removeOldRecords() {
        //perform cleanup
        removeRecordsOlderThan(System.currentTimeMillis() - this.maxTimeToKeepEntriesMillis);
    }

    protected void removeRecordsOlderThan( long millisInUtc ) {
        if (millisInUtc <= 0 || stopped) {
            return;
        }
        long searchBound = TIME_BASED_KEYS.getCounterEndingAt(millisInUtc);
        LOGGER.debug("Removing records older than " + searchBound);
        NavigableMap<Long, JournalRecord> toRemove = this.records.headMap(searchBound);
        toRemove.clear();
        journalDB.commit();
        journalDB.compact();
    }

    protected String getJournalLocation() {
        return journalLocation;
    }

    @Override
    public Records allRecords( boolean descendingOrder ) {
        return recordsFrom(records, descendingOrder);
    }

    @Override
    public JournalRecord lastRecord() {
        return this.records == null || this.records.isEmpty() ? null : this.records.lastEntry().getValue();
    }

    @Override
    public Records recordsNewerThan( DateTime changeSetTime,
                                     boolean inclusive,
                                     boolean descendingOrder ) {
        if (stopped) {
            return Records.EMPTY;
        }

        long changeSetMillisUTC = -1;
        long searchBound = -1;
        if (changeSetTime != null) {
            changeSetMillisUTC = changeSetTime.getMillis();
            //adjust the millis using a delta so that we are sure we catch everything in a cluster which may have differences in
            //clock time
            searchBound = TIME_BASED_KEYS.getCounterStartingAt(changeSetMillisUTC - searchTimeDelta);
        }

        NavigableMap<Long, JournalRecord> subMap = records.tailMap(searchBound, true);
        if (subMap.isEmpty()) {
            return Records.EMPTY;
        }
        //process each of the records from the result and look at the timestamp of the changeset, so that we're sure we only include
        //the correct ones (we used a delta to make sure we get everything)
        long startKeyInSubMap = -1;
        for (Iterator<Long> timeBasedKeysIterator = subMap.keySet().iterator(); timeBasedKeysIterator.hasNext();) {
            Long timeBasedKey = timeBasedKeysIterator.next();
            JournalRecord record = subMap.get(timeBasedKey);
            long recordChangeTimeMillisUTC = record.getChangeTimeMillis();
            if (((recordChangeTimeMillisUTC == changeSetMillisUTC) && inclusive)
                || recordChangeTimeMillisUTC > changeSetMillisUTC) {
                startKeyInSubMap = timeBasedKey;
                break;
            }
        }
        return startKeyInSubMap != -1 ? recordsFrom(subMap.tailMap(startKeyInSubMap, true), descendingOrder) : Records.EMPTY;
    }

    @Override
    public Iterator<NodeKey> changedNodesSince( final long timestamp ) {
        // we use a delta to make sure we get everything and we filter false positives later on
        long searchBound = TIME_BASED_KEYS.getCounterStartingAt(timestamp - searchTimeDelta);
        Collection<JournalRecord> journalRecords = records.tailMap(searchBound, true).values();
        if (journalRecords.isEmpty()) {
            return Collections.emptyListIterator();
        }
        final Iterator<JournalRecord> recordsIterator = journalRecords.iterator();  
        return new Iterator<NodeKey>() {
            private Iterator<NodeKey> currentBatchOfKeys = null;
            
            @Override
            public boolean hasNext() {
                nextBatchOfKeys();
                return currentBatchOfKeys != null && currentBatchOfKeys.hasNext();
            }

            @Override
            public NodeKey next() {
                nextBatchOfKeys();
                if (currentBatchOfKeys == null) {
                    throw new NoSuchElementException();
                }
                assert currentBatchOfKeys.hasNext();
                return currentBatchOfKeys.next();
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }
            
            private void nextBatchOfKeys() {
                if ((currentBatchOfKeys == null || !currentBatchOfKeys.hasNext()) && recordsIterator.hasNext()) {
                    while (recordsIterator.hasNext()) {
                        JournalRecord record = recordsIterator.next();
                        // we searched using a delta, so we must eliminate false positives
                        if (record.getChangeTimeMillis() >= timestamp) {
                            currentBatchOfKeys = record.getChangeSet().changedNodes().iterator();
                            break;
                        }
                    }
                }
                if (currentBatchOfKeys != null && !currentBatchOfKeys.hasNext()) {
                    currentBatchOfKeys = null;
                }
            }            
        };
    }

    @Override
    public String journalId() {
        return journalId;
    }

    protected LocalJournal withSearchTimeDelta( final long searchTimeDelta ) {
        this.searchTimeDelta = searchTimeDelta;
        return this;
    }

    private static Records recordsFrom( final NavigableMap<Long, JournalRecord> content, boolean descending ) {
        final Iterator<JournalRecord> iterator = descending ? content.descendingMap().values().iterator() : 
                                                              content.values().iterator();
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
                        return iterator.hasNext();
                    }

                    @Override
                    public JournalRecord next() {
                        return iterator.next();
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
