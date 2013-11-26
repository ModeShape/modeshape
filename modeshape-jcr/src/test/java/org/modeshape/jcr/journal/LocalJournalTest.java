package org.modeshape.jcr.journal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.jcr.RepositoryException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.cache.change.Change;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.value.basic.JodaDateTime;

/**
 * Unit test for {@link LocalJournal}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class LocalJournalTest {

    protected LocalJournal journal;

    private long timestamp1;
    private long timestamp2;
    private long timestamp3;

    @Before
    public void before() throws Exception {
        FileUtil.delete("target/journal");
        this.journal = new LocalJournal("target/journal");
        journal.start();
        insertTestRecords();
    }

    @After
    public void after() throws RepositoryException {
        journal().shutdown();
    }

    protected void insertTestRecords() throws InterruptedException {
        //p1 has 4 changesets
        ChangeSet process1Changes1 = TestChangeSet.create("j1", 5);
        ChangeSet process1Changes2 = TestChangeSet.create("j1", 1);
        ChangeSet process1Changes3 = TestChangeSet.create("j1", 1);
        ChangeSet process1Changes4 = TestChangeSet.create("j1", 1);
        journal().notify(process1Changes1);
        journal().notify(process1Changes2);
        journal().notify(process1Changes3);
        journal().notify(process1Changes4);

        Thread.sleep(10);
        timestamp1 = System.currentTimeMillis();

        //p2 has 2 changesets
        ChangeSet process2Changes1 = TestChangeSet.create("j2", 1);
        ChangeSet process2Changes2 = TestChangeSet.create("j2", 1);
        journal().notify(process2Changes1);
        journal().notify(process2Changes2);
        Thread.sleep(10);
        timestamp2 = System.currentTimeMillis();

        //p3 has 2 changesets
        ChangeSet process3Changes1 = TestChangeSet.create("j3", 2);
        ChangeSet process3Changes2 = TestChangeSet.create("j3", 2);
        ChangeSet process3Changes3 = TestChangeSet.create("j3", 0);
        journal().notify(process3Changes1);
        journal().notify(process3Changes2);
        journal().notify(process3Changes3);

        Thread.sleep(10);
        timestamp3 = System.currentTimeMillis();
    }

    @Test
    public void shouldInsertRecords() throws InterruptedException {
        journal().addRecords(new JournalRecord(TestChangeSet.create("j4", 2)),
                             new JournalRecord(TestChangeSet.create("j4", 1)),
                             new JournalRecord(TestChangeSet.create("j4", 3)));
        Iterable<JournalRecord> records = journal().recordsFor("j4");
        int count = 0;
        Iterator<JournalRecord> iterator = records.iterator();
        while (iterator.hasNext()) {
            count++;
        }
        assertEquals(3, count);
    }

    @Test
    public void shouldReturnRecordsForProcess() throws InterruptedException {
        List<JournalRecord> records = new ArrayList<JournalRecord>();
        for (JournalRecord record : journal().recordsFor("j1")) {
            records.add(record);
            assertEquals("j1", record.getJournalId());
        }
        assertEquals(4, records.size());

        records.clear();
        for (JournalRecord record : journal().recordsFor("j2")) {
            records.add(record);
            assertEquals("j2", record.getJournalId());
        }
        assertEquals(2, records.size());

        records.clear();
        for (JournalRecord record : journal().recordsFor("j3")) {
            records.add(record);
            assertEquals("j3", record.getJournalId());
        }
        assertEquals(2, records.size());
    }

    @Test
    public void shouldReturnAllRecords() throws Exception {
        assertEquals(8, journal().allRecords(false).size());
    }

    @Test
    public void shouldSearchRecordsBasedOnTimestamp() throws Exception {
        //find records older than -1
        assertEquals(8, journal().recordsOlderThan(-1, true, false).size());
        //find records older than ts1, inclusive
        assertEquals(4, journal().recordsOlderThan(timestamp1, true, false).size());
        //find records older than ts2, exclusive
        assertEquals(2, journal().recordsOlderThan(timestamp2, true, false).size());
        //find records older than ts3, exclusive
        assertEquals(0, journal().recordsOlderThan(timestamp3, true, false).size());
        //find records older than max, exclusive
        assertEquals(0, journal().recordsOlderThan(Long.MAX_VALUE, true, false).size());
    }

    @Test
    public void shouldComputeJournalDeltas() throws Exception {
        List<String> journalIds = new ArrayList<String>();
        for (JournalRecord record : journal().recordsDelta("j2", false)) {
            journalIds.add(record.getJournalId());
        }
        assertEquals(Arrays.asList("j3", "j3"), journalIds);

        assertFalse(journal().recordsDelta("j3", false).iterator().hasNext());

        //add a new entry
        journal().notify(TestChangeSet.create("j1", 2));
        journalIds.clear();
        for (JournalRecord record : journal().recordsDelta("j3", false)) {
            journalIds.add(record.getJournalId());
        }
        assertEquals(Arrays.asList("j1"), journalIds);

        //a process that "hasn't been seen"
        assertEquals(9, journal().recordsDelta("j4", false).size());
    }

    @Test
    public void shouldRemoveOlderJournalEntries() throws Exception {
        File journalFolder = new File(localJournal().getJournalLocation());
        assertTrue(journalFolder.isDirectory() && journalFolder.canRead());
        assertEquals(1, journalFolder.list().length);

        //insert 200 entries - this should create multiple files
        int entriesCount = 200;
        for (int i = 0; i < entriesCount; i++) {
            journal().notify(TestChangeSet.create("j1", entriesCount));
        }
        //make sure we have at least 3 segments
        int filesCountFirstBatch = journalFolder.list().length;
        assertTrue(filesCountFirstBatch > 2);
        assertEquals(entriesCount + 8, journal().allRecords(false).size());

        //sleep
        Thread.sleep(100);
        long currentMillis = System.currentTimeMillis();

        //insert another batch of record which should produce additional segments
        for (int i = 0; i < entriesCount; i++) {
            journal().notify(TestChangeSet.create("j1", entriesCount));
        }

        int filesCountSecondBatch = journalFolder.list().length;
        assertTrue(filesCountFirstBatch < filesCountSecondBatch);
        assertEquals(entriesCount * 2 + 8, journal().allRecords(false).size());

        localJournal().removeRecordsOlderThan(currentMillis);
        //verify that there are fewer files
        assertTrue(filesCountSecondBatch > journalFolder.list().length);

        //now make sure we can still add data to the journal
        journal().notify(TestChangeSet.create("j4", 2));
        assertTrue(journal().recordsFor("j4").iterator().hasNext());
    }

    @Test
    public void shouldHaveSameJournalIdAfterRestart() throws Exception {
        ChangeJournal journal = journal();
        String journalId = journal.journalId();
        journal.shutdown();
        journal.start();
        assertEquals(journalId, journal.journalId());
    }

    protected ChangeJournal journal() {
        return journal;
    }

    protected LocalJournal localJournal() {
        return journal;
    }

    static class TestChangeSet implements ChangeSet {
        private List<Change> changes;
        private DateTime timestamp;
        private String journalId;

        private TestChangeSet( List<Change> changes,
                               String journalId ) {
            this.changes = changes;
            this.timestamp = new JodaDateTime();
            this.journalId = journalId;
        }

        @Override
        public int size() {
            return changes.size();
        }

        @Override
        public boolean isEmpty() {
            return changes.isEmpty();
        }

        @Override
        public String getUserId() {
            return "someUser";
        }

        @Override
        public Map<String, String> getUserData() {
            return null;
        }

        @Override
        public DateTime getTimestamp() {
            return timestamp;
        }

        @Override
        public String getProcessKey() {
            return null;
        }

        @Override
        public String getRepositoryKey() {
            return "someRepository";
        }

        @Override
        public String getWorkspaceName() {
            return "someWorkspace";
        }

        @Override
        public Set<NodeKey> changedNodes() {
            return null;
        }

        @Override
        public String getJournalId() {
            return journalId;
        }

        @Override
        public Iterator<Change> iterator() {
            return changes.iterator();
        }

        static ChangeSet create( String journalId,
                                 int changesCount ) {
            List<Change> changes = new ArrayList<Change>(changesCount);
            for (int i = 0; i < changesCount; i++) {
                changes.add(TestChange.newInstance());
            }
            return new TestChangeSet(changes, journalId);
        }
    }

    private static class TestChange extends Change {
        String nodeId = String.valueOf(System.currentTimeMillis() + System.nanoTime());
        String nodePath = String.valueOf(System.currentTimeMillis()) + "/" + String.valueOf(System.nanoTime());

        static TestChange newInstance() {
            return new TestChange();
        }
    }
}
