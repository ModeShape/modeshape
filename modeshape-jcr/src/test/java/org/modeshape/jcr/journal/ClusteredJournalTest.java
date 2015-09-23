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

import static org.junit.Assert.assertTrue;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.ClusteringHelper;
import org.modeshape.jcr.clustering.ClusteringService;

/**
 * Unit test for {@link ClusteredJournal}
 * 
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ClusteredJournalTest extends LocalJournalTest {

    private ClusteredJournal defaultJournal;
    private List<ClusteringService> clusteringServices = new ArrayList<ClusteringService>();

    @BeforeClass
    public static void beforeClass() throws Exception {
        ClusteringHelper.bindJGroupsToLocalAddress();
    }

    @AfterClass
    public static void afterClass() throws Exception {
        ClusteringHelper.removeJGroupsBindings();
    }

    @Override
    public void before() throws Exception {
        this.defaultJournal = startNewJournal("target/default_clustered_journal", "default-journal-cluster");
        insertTestRecords();
    }

    @Override
    protected ChangeJournal journal() {
        return defaultJournal;
    }

    @Override
    protected LocalJournal localJournal() {
        return defaultJournal.localJournal();
    }

    @Override
    public void after() {
        super.after();
        for (ClusteringService clusteringService : clusteringServices) {
            clusteringService.shutdown();
        }
    }

    @Test
    public void shouldReconcileDeltaInCluster() throws Exception {
        // shut down the default journal
        after();
        ClusteredJournal journal1 = null; 
        ClusteredJournal journal2 = null; 
        ClusteredJournal journal3 = null; 
        ClusteredJournal journal4 = null; 

        try {
            // start a journal
            journal1 = startNewJournal("target/clustered_journal_1", "journal-cluster-1");
            assertTrue(journal1.started());
            // add 2 record to the 1st journal
            journal1.addRecords(new JournalRecord(TestChangeSet.create(journal1.journalId(), 1)));
            journal1.addRecords(new JournalRecord(TestChangeSet.create(journal1.journalId(), 1)));

            // start another journal
            journal2 = startNewJournal("target/clustered_journal_2", "journal-cluster-1");
            assertTrue(journal2.started());
            // check that the 2nd journal has received all the changes from the 1st (they haven't seen each other yet)
            Assert.assertEquals(2, journal2.allRecords(false).size());

            // add 1 record to the 2nd journal and also the 1st, to simulate a new distributed changeset
            JournalRecord recordJ2 = new JournalRecord(TestChangeSet.create(journal2.journalId(), 1));
            journal2.addRecords(recordJ2);
            Assert.assertEquals(3, journal2.allRecords(false).size());

            journal1.addRecords(recordJ2);
            Assert.assertEquals(3, journal1.allRecords(false).size());

            // shutdown the second journal
            journal2.clusteringService().shutdown();
            journal2.shutdown();

            // while the 2nd process is down, add a new record
            journal1.addRecords(new JournalRecord(TestChangeSet.create(journal1.journalId(), 1)));

            // start the 2nd journal back and verify it received a 1 delta from the 1st journal
            journal2.clusteringService().restart();
            journal2.start();

            Assert.assertEquals(4, journal2.allRecords(false).size());

            // start a fresh 3rd journal and check that it gets up-to-date 
            journal3 = startNewJournal("target/clustered_journal_3", "journal-cluster-1");
            assertTrue(journal3.started());
            Assert.assertEquals(4, journal3.allRecords(false).size());
            
            //shutdown 1 and 3 and start a 4th journal
            journal1.shutdown();
            journal4 = startNewJournal("target/clustered_journal_4", "journal-cluster-1");
            assertTrue(journal4.started());
            Assert.assertEquals(4, journal4.allRecords(false).size());
        } finally {
            shutdown(journal1, journal2, journal3, journal4);
        }
    }
    
    private void shutdown(ChangeJournal... journals) {
        for (ChangeJournal journal : journals) {
            try {
                if (journal != null && journal.started()) {
                    journal.shutdown();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private ClusteredJournal startNewJournal( String fileLocation,
                                              String clusterName ) throws Exception {
        ClusteringService clusteringService = ClusteringService.startStandalone(clusterName, "config/jgroups-test-config.xml");
        clusteringServices.add(clusteringService);

        FileUtil.delete(fileLocation);
        LocalJournal localJournal = new LocalJournal(fileLocation);
        ClusteredJournal clusteredJournal = new ClusteredJournal(localJournal, clusteringService);
        clusteredJournal.start();

        return clusteredJournal;
    }
}
