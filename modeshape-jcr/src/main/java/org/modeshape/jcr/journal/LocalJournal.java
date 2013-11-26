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
import org.mapdb.Atomic;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.value.basic.JodaDateTime;

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
            DBMaker dbMaker = DBMaker.newAppendFileDB(new File(journalFileLocation, RECORDS_FIELD))
                                     .compressionEnable()
                                     .checksumEnable()
                                     .closeOnJvmShutdown()
                                     .snapshotEnable()
                                     .randomAccessFileEnableIfNeeded();
            if (!asyncWritesEnabled) {
                dbMaker.asyncWriteDisable();
            }
            this.journalDB = dbMaker.make();
            this.records = this.journalDB.createTreeSet(RECORDS_FIELD).keepCounter(true).makeOrGet();
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
                long createTimeMillisUTC = uniqueCreatedTimeMillisUTC();
                record.withCreatedTimeMillisUTC(createTimeMillisUTC);
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

    protected synchronized void removeRecordsOlderThan( long timeMillis ) {
        RW_LOCK.writeLock().lock();
        try {
            if (timeMillis <= 0 || stopped) {
                return;
            }
            long millisInUtc = new JodaDateTime(timeMillis).getMillisecondsInUtc();
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
        long createTimeMillisUTC = new JodaDateTime().getMillisecondsInUtc();
        JournalRecord recordSameTimestamp = JournalRecord.searchBound(createTimeMillisUTC);
        while (records.contains(recordSameTimestamp)) {
            createTimeMillisUTC = new JodaDateTime().getMillisecondsInUtc();
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
    public Records recordsOlderThan( long localMillis,
                                     boolean inclusive,
                                     boolean descendingOrder ) {
        if (stopped) {
            return Records.EMPTY;
        }
        long millisUTC = new JodaDateTime(localMillis).getMillisecondsInUtc();
        JournalRecord bound = JournalRecord.searchBound(millisUTC);
        NavigableSet<JournalRecord> subset = records.tailSet(bound, inclusive);
        return recordsFromSet(subset, descendingOrder);
    }

    @Override
    public Records recordsDelta( String journalId,
                                 boolean descendingOrder ) {
        CheckArg.isNotNull("journalId cannot be null", journalId);

        LOGGER.debug("Journal {0} computing records delta for remote journal {1}", this.journalId, journalId);
        if (stopped) {
            return Records.EMPTY;
        }

        //step1: find the local time when this process was last seen (based on the last change we have from this process)
        long processLastSeenLocalTimeUtcMillis = -1;
        Iterator<JournalRecord> reverseIterator = records.descendingIterator();
        while (reverseIterator.hasNext()) {
            JournalRecord record = reverseIterator.next();
            if (record.getJournalId().equals(journalId)) {
                processLastSeenLocalTimeUtcMillis = record.getCreatedTimeMillisUTC();
                break;
            }
        }
        if (processLastSeenLocalTimeUtcMillis == -1) {
            //we have never seen this process, so return everything
            return allRecords(descendingOrder);
        }

        //step2: find *all* the entries which are greater (in local time) than this last time
        JournalRecord bound = JournalRecord.searchBound(processLastSeenLocalTimeUtcMillis + 1);
        NavigableSet<JournalRecord> subset = (NavigableSet<JournalRecord>)records.tailSet(bound);
        return recordsFromSet(subset, descendingOrder);
    }

    @Override
    public String journalId() {
        return journalId;
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
        };
    }
}
