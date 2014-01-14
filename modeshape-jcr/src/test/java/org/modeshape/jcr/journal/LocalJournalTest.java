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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
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

    private org.joda.time.DateTime timestamp1;
    private org.joda.time.DateTime timestamp2;
    private org.joda.time.DateTime timestamp3;

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
        //p1 has 4 changesets (sleep for 1 ms between them to make sure the timestamps are different
        ChangeSet process1Changes1 = TestChangeSet.create("j1", 5);
        journal().notify(process1Changes1);

        ChangeSet process1Changes2 = TestChangeSet.create("j1", 1);
        journal().notify(process1Changes2);

        ChangeSet process1Changes3 = TestChangeSet.create("j1", 1);
        journal().notify(process1Changes3);

        ChangeSet process1Changes4 = TestChangeSet.create("j1", 1);
        journal().notify(process1Changes4);
        timestamp1 = new org.joda.time.DateTime(process1Changes4.getTimestamp().getMilliseconds());

        //p2 has 2 changesets
        ChangeSet process2Changes1 = TestChangeSet.create("j2", 1);
        journal().notify(process2Changes1);

        ChangeSet process2Changes2 = TestChangeSet.create("j2", 1);
        journal().notify(process2Changes2);
        timestamp2 = new org.joda.time.DateTime(process2Changes2.getTimestamp().getMilliseconds());

        //p3 has 2 changesets
        ChangeSet process3Changes1 = TestChangeSet.create("j3", 2);
        journal().notify(process3Changes1);

        ChangeSet process3Changes2 = TestChangeSet.create("j3", 2);
        journal().notify(process3Changes2);
        timestamp3 = new org.joda.time.DateTime(process3Changes2.getTimestamp().getMilliseconds());

        ChangeSet process3Changes3 = TestChangeSet.create("j3", 0);
        journal().notify(process3Changes3);
    }

    @Test
    public void shouldReturnAllRecords() throws Exception {
        assertEquals(8, journal().allRecords(false).size());
    }

    @Test
    public void shouldAddRecords() throws InterruptedException {
        int initialRecordCount = journal().allRecords(false).size();
        journal().addRecords(
                new JournalRecord(TestChangeSet.create("j4", 2)),
                new JournalRecord(TestChangeSet.create("j4", 1)),
                new JournalRecord(TestChangeSet.create("j4", 3)));        
        assertEquals(initialRecordCount + 3, journal().allRecords(false).size());
    }

    @Test
    public void shouldReturnLastRecord() throws Exception {
        JournalRecord lastRecord = journal().lastRecord();
        assertNotNull(lastRecord);
        assertEquals("j3", lastRecord.getJournalId());
        assertEquals(timestamp3.getMillis(), lastRecord.getChangeTimeMillis());
    }

    @Test
    public void shouldSearchRecordsBasedOnTimestamp() throws Exception {
        //find records older than -1
        assertEquals(8, journal().recordsNewerThan(new org.joda.time.DateTime(-1), true, false).size());
        //find records older than ts1, inclusive
        assertEquals(5, journal().recordsNewerThan(timestamp1, true, false).size());
        //find records older than ts1, exclusive
        assertEquals(4, journal().recordsNewerThan(timestamp1, false, false).size());
        //find records older than ts2, exclusive
        assertEquals(2, journal().recordsNewerThan(timestamp2, false, false).size());
        //find records older than ts3, exclusive
        assertEquals(0, journal().recordsNewerThan(timestamp3, false, false).size());
        //find records older than ts3, inclusive
        assertEquals(1, journal().recordsNewerThan(timestamp3, true, false).size());
        //find records older than max, exclusive
        assertEquals(0, journal().recordsNewerThan(new org.joda.time.DateTime(Long.MAX_VALUE), true, false).size());
    }

    @Test
    public void shouldRemoveOlderJournalEntries() throws Exception {
        File journalFolder = new File(localJournal().getJournalLocation());
        assertTrue(journalFolder.isDirectory() && journalFolder.canRead());
        int initialEntriesCount = journal().allRecords(false).size();

        //insert some of entries - this should create multiple files
        int entriesCount = 10;
        for (int i = 0; i < entriesCount; i++) {
            journal().notify(TestChangeSet.create("j1", entriesCount));
        }
        //make sure we have at least 3 segments
        assertEquals(entriesCount + initialEntriesCount, journal().allRecords(false).size());

        Thread.sleep(1);
        long currentMillis = System.currentTimeMillis();
        Thread.sleep(1);

        //insert another batch of record which should produce additional segments
        for (int i = 0; i < entriesCount; i++) {
            journal().notify(TestChangeSet.create("j1", entriesCount));
        }

        int sizeAfterSecondBatch = journal().allRecords(false).size();
        assertEquals(entriesCount * 2 + initialEntriesCount, sizeAfterSecondBatch);

        //this should remove all the entries from the first batch + initial entries
        localJournal().removeRecordsOlderThan(currentMillis);
        assertEquals(entriesCount, journal().allRecords(false).size());

        //now make sure we can still add data to the journal
        journal().notify(TestChangeSet.create("j4", 2));
        assertEquals(entriesCount + 1, journal().allRecords(false).size());
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
        private final String uuid = UUID.randomUUID().toString();
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

        @Override
        public String getUUID() {
            return uuid;
        }

        static ChangeSet create( String journalId,
                                 int changesCount ) throws InterruptedException {
            List<Change> changes = new ArrayList<Change>(changesCount);
            for (int i = 0; i < changesCount; i++) {
                changes.add(new Change() {
                });
            }
            //sleep 1 second to make sure that successive calls won't have the same TS
            Thread.sleep(1);
            return new TestChangeSet(changes, journalId);
        }
    }
}
