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
        ChangeSet process1Changes1 = TestChangeSet.create("p1", 5);
        ChangeSet process1Changes2 = TestChangeSet.create("p1", 1);
        ChangeSet process1Changes3 = TestChangeSet.create("p1", 1);
        ChangeSet process1Changes4 = TestChangeSet.create("p1", 1);
        journal().notify(process1Changes1);
        journal().notify(process1Changes2);
        journal().notify(process1Changes3);
        journal().notify(process1Changes4);

        Thread.sleep(10);
        timestamp1 = System.currentTimeMillis();

        //p2 has 2 changesets
        ChangeSet process2Changes1 = TestChangeSet.create("p2", 1);
        ChangeSet process2Changes2 = TestChangeSet.create("p2", 1);
        journal().notify(process2Changes1);
        journal().notify(process2Changes2);
        Thread.sleep(10);
        timestamp2 = System.currentTimeMillis();

        //p3 has 2 changesets
        ChangeSet process3Changes1 = TestChangeSet.create("p3", 2);
        ChangeSet process3Changes2 = TestChangeSet.create("p3", 2);
        ChangeSet process3Changes3 = TestChangeSet.create("p3", 0);
        journal().notify(process3Changes1);
        journal().notify(process3Changes2);
        journal().notify(process3Changes3);

        Thread.sleep(10);
        timestamp3 = System.currentTimeMillis();
    }

    @Test
    public void shouldInsertRecords() throws InterruptedException {
        journal().addRecords(new JournalRecord(TestChangeSet.create("p4", 2)),
                             new JournalRecord(TestChangeSet.create("p4", 1)),
                             new JournalRecord(TestChangeSet.create("p4", 3)));
        Iterable<JournalRecord> records = journal().recordsFor("p4");
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
        for (JournalRecord record : journal().recordsFor("p1")) {
            records.add(record);
            assertEquals("p1", record.getProcessKey());
        }
        assertEquals(4, records.size());

        records.clear();
        for (JournalRecord record : journal().recordsFor("p2")) {
            records.add(record);
            assertEquals("p2", record.getProcessKey());
        }
        assertEquals(2, records.size());

        records.clear();
        for (JournalRecord record : journal().recordsFor("p3")) {
            records.add(record);
            assertEquals("p3", record.getProcessKey());
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
        List<String> processOwners = new ArrayList<String>();
        for (JournalRecord record : journal().recordsDelta("p2", false)) {
            processOwners.add(record.getProcessKey());
        }
        assertEquals(Arrays.asList("p3", "p3"), processOwners);

        assertFalse(journal().recordsDelta("p3", false).iterator().hasNext());

        //add a new entry
        journal().notify(TestChangeSet.create("p1", 2));
        processOwners.clear();
        for (JournalRecord record : journal().recordsDelta("p3", false)) {
            processOwners.add(record.getProcessKey());
        }
        assertEquals(Arrays.asList("p1"), processOwners);

        //a process that "hasn't been seen"
        assertEquals(9, journal().recordsDelta("p4", false).size());
    }

    @Test
    public void shouldRemoveOlderJournalEntries() throws Exception {
        File journalFolder = new File(localJournal().getJournalLocation());
        assertTrue(journalFolder.isDirectory() && journalFolder.canRead());
        assertEquals(1, journalFolder.list().length);

        //insert 200 entries - this should create multiple files
        int entriesCount = 200;
        for (int i = 0; i < entriesCount; i++) {
            journal().notify(TestChangeSet.create("p1", entriesCount));
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
            journal().notify(TestChangeSet.create("p1", entriesCount));
        }

        int filesCountSecondBatch = journalFolder.list().length;
        assertTrue(filesCountFirstBatch < filesCountSecondBatch);
        assertEquals(entriesCount * 2 + 8, journal().allRecords(false).size());

        localJournal().removeRecordsOlderThan(currentMillis);
        //verify that there are fewer files
        assertTrue(filesCountSecondBatch > journalFolder.list().length);

        //now make sure we can still add data to the journal
        journal().notify(TestChangeSet.create("p4", 2));
        assertTrue(journal().recordsFor("p4").iterator().hasNext());
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
        private String processKey;

        private TestChangeSet( List<Change> changes,
                               String processKey ) {
            this.changes = changes;
            this.timestamp = new JodaDateTime();
            this.processKey = processKey;
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
            return processKey;
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
        public Iterator<Change> iterator() {
            return changes.iterator();
        }

        static ChangeSet create( String processKey,
                                 int changesCount ) {
            List<Change> changes = new ArrayList<Change>(changesCount);
            for (int i = 0; i < changesCount; i++) {
                changes.add(TestChange.newInstance());
            }
            return new TestChangeSet(changes, processKey);
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
