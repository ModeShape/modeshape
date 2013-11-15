package org.modeshape.jcr.journal;

import java.io.File;
import java.util.Collections;
import java.util.Iterator;
import java.util.NavigableSet;
import java.util.NoSuchElementException;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.TimeUnit;
import javax.jcr.RepositoryException;
import org.infinispan.schematic.document.ThreadSafe;
import org.mapdb.DB;
import org.mapdb.DBMaker;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.JcrI18n;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.value.basic.JodaDateTime;

/**
 * An append only journal implementation which stores each {@link ChangeSet} (either local or remove) on the local FS.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
@ThreadSafe
public class Journal implements ChangeSetListener {

    private static final int DEFAULT_MAX_TIME_TO_KEEP_FILES = -1;
    private static final Logger LOGGER = Logger.getLogger(Journal.class);
    private static final String FILE_NAME = "records";
    private static final String CONTENT_NAME = "records";
    private static final int MAX_RETRY_COUNT = 3;

    private final String journalLocation;
    private final boolean asyncWritesEnabled;
    private final long maxTimeToKeepEntriesMillis;

    private DB journalDB;
    private NavigableSet<JournalRecord> records;
    private volatile boolean stopped = false;

    /**
     * Creates a new journal instance, in stopped state.
     *
     * @param journalLocation the folder location on the FS where the entries should be saved. Must not be {@code null}
     * @param asyncWritesEnabled flag which indicates if disk write should be asynchronous or not.
     * @param maxDaysToKeepEntries the maximum number of days this journal should store entries on disk. A negative value or 0
     * means "store forever"
     */
    public Journal( String journalLocation,
                    boolean asyncWritesEnabled,
                    int maxDaysToKeepEntries ) {
        CheckArg.isNotNull(journalLocation, "journalLocation");

        this.journalLocation = journalLocation;
        this.asyncWritesEnabled = asyncWritesEnabled;
        this.maxTimeToKeepEntriesMillis = TimeUnit.DAYS.toMillis(maxDaysToKeepEntries);
        this.stopped = true;
    }

    /**
     * Creates a new journal which writes synchronously to disk and keeps files forever
     *
     * @see Journal#Journal(String, boolean, int)
     */
    protected Journal( String journalLocation ) {
        this(journalLocation, false, DEFAULT_MAX_TIME_TO_KEEP_FILES);
    }

    /**
     * Starts this journal instance.
     *
     * @return this {@link Journal}instance
     * @throws RepositoryException if anything fails during start
     */
    @SuppressWarnings( "rawtypes" )
    public synchronized Journal start() throws RepositoryException {
        try {
            File journalFileLocation = new File(journalLocation);
            if (!journalFileLocation.exists()) {
                journalFileLocation.mkdir();
            }
            DBMaker dbMaker = DBMaker.newAppendFileDB(new File(journalFileLocation, FILE_NAME))
                                     .compressionEnable()
                                     .checksumEnable()
                                     .closeOnJvmShutdown()
                                     .snapshotEnable()
                                     .randomAccessFileEnableIfNeeded();
            if (!asyncWritesEnabled) {
                dbMaker.asyncWriteDisable();
            }
            this.journalDB = dbMaker.make();
            this.records = this.journalDB.createTreeSet(CONTENT_NAME).keepCounter(true).makeOrGet();
            this.stopped = false;
            return this;
        } catch (Exception e) {
            throw new RepositoryException(JcrI18n.cannotStartJournal.text(), e);
        }
    }

    /**
     * Stops the journal.
     */
    public synchronized void shutdown() {
        this.stopped = true;
        try {
            this.journalDB.close();
        } catch (Exception e) {
            LOGGER.error(e, JcrI18n.cannotStopJournal);
        }
    }

    @Override
    public synchronized void notify( ChangeSet changeSet ) {
        if (stopped) {
            return;
        }
        if (changeSet.isEmpty()) {
            return;
        }
        boolean retry = true;
        int retryCount = 1;
        Exception lastException = null;

        while (retry && retryCount <= MAX_RETRY_COUNT) {
            try {
                long createTimeMillisUTC = uniqueCreatedTimeMillisUTC();
                JournalRecord record = new JournalRecord(createTimeMillisUTC, changeSet);
                this.records.add(record);
                this.journalDB.commit();
                retry = false;
            } catch (Exception e) {
                lastException = e;
                LOGGER.debug(e, "Error while committing changes to the journal. Retrying operation, pass " + retryCount);
                retryCount++;
            }
        }
        if (lastException != null) {
            throw new RuntimeException(lastException);
        }
    }

    /**
     * Removes entries older than {@link Journal#maxTimeToKeepEntriesMillis}.
     */
    public synchronized void removeOldRecords() {
        //perform cleanup
        removeRecordsOlderThan(System.currentTimeMillis() - this.maxTimeToKeepEntriesMillis);
    }

    protected void removeRecordsOlderThan( long timeMillis ) {
        if (timeMillis <= 0) {
            return;
        }

        long millisInUtc = new JodaDateTime(timeMillis).getMillisecondsInUtc();
        SortedSet<JournalRecord> toRemove = this.records.headSet(JournalRecord.searchBound(millisInUtc));
        toRemove.clear();
        journalDB.commit();
        journalDB.compact();
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

    /**
     * Returns all the records this journal holds
     *
     * @param descendingOrder flag which indicates whether the entries
     * @return a {@link Records} instance
     */
    public Records allRecords( boolean descendingOrder ) {
        return recordsFromSet(freeze(), descendingOrder);
    }

    /**
     * Returns the records for a given process.
     *
     * @param processKey a {@link String} the id of a process; must not be {@link null}
     * @return an {@link Iterable<JournalRecord>}, never {@code null}
     */
    public Iterable<JournalRecord> recordsFor( String processKey ) {
        if (stopped) {
            return Collections.emptySet();
        }
        CheckArg.isNotNull("processKey cannot be null", processKey);
        NavigableSet<JournalRecord> frozenSet = freeze();
        return filter(frozenSet.iterator(), processKey);
    }

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
                                     boolean descendingOrder ) {
        if (stopped) {
            return Records.EMPTY;
        }
        long millisUTC = new JodaDateTime(localMillis).getMillisecondsInUtc();
        JournalRecord bound = JournalRecord.searchBound(millisUTC);
        NavigableSet<JournalRecord> frozenSet = freeze();
        NavigableSet<JournalRecord> subset = frozenSet.tailSet(bound, inclusive);
        return recordsFromSet(subset, descendingOrder);
    }

    /**
     * Returns the all the records from all the processes this journal has, since last seeing the given process.
     *
     * @param processKey a {@link String} the id of a process; must not be {@link null}
     * @param descendingOrder flag indicating if the records should be returned in ascending order (oldest to newest) or descending
     * order (newest to oldest)
     * @return a {@link Records} instance; never {@code null}. This will only contain records for other processes than {@code processKey}
     */
    public Records recordsDelta( String processKey,
                                 boolean descendingOrder ) {
        if (stopped) {
            return Records.EMPTY;
        }
        CheckArg.isNotNull("processKey cannot be null", processKey);
        NavigableSet<JournalRecord> frozenSet = freeze();

        //step1: find the local time when this process was last seen (based on the last change we have from this process)
        long processLastSeenLocalTimeUtcMillis = -1;
        Iterator<JournalRecord> reverseIterator = frozenSet.descendingIterator();
        while (reverseIterator.hasNext()) {
            JournalRecord record = reverseIterator.next();
            if (record.getProcessKey().equals(processKey)) {
                processLastSeenLocalTimeUtcMillis = record.getCreatedTimeMillisUTC();
                break;
            }
        }
        if (processLastSeenLocalTimeUtcMillis == -1) {
            //we have never seen this process, so return our entire journal
            return recordsFromSet(frozenSet, descendingOrder);
        }

        //step2: find *all* the entries which are greater (in local time) than this last time
        JournalRecord bound = JournalRecord.searchBound(processLastSeenLocalTimeUtcMillis + 1);
        NavigableSet<JournalRecord> subset = (NavigableSet<JournalRecord>)frozenSet.tailSet(bound);
        return recordsFromSet(subset, descendingOrder);
    }

    private Iterable<JournalRecord> filter( final Iterator<JournalRecord> recordsIterator,
                                            final String processKey ) {
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
                    if (record.getProcessKey().equals(processKey)) {
                        return record;

                    }
                }
                return null;
            }
        };
    }

    private NavigableSet<JournalRecord> freeze() {
        return journalDB.snapshot().getTreeSet(CONTENT_NAME);
    }

    /**
     * An {@link Iterable<JournalRecord> extension which provides information about the number of entries the underlying collection
     * holds.
     */
    public interface Records extends Iterable<JournalRecord> {
        public static final Records EMPTY = recordsFromSet(new TreeSet<JournalRecord>(), false);

        /**
         * Returns the number of item this iterable has.
         *
         * @return an int
         */
        public int size();
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
