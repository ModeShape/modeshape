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

import java.util.ArrayList;
import java.util.List;
import junit.framework.Assert;
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

    @Override
    public void before() throws Exception {
        this.defaultJournal = startNewJournal("default-test-journal-process",
                                              "target/default_clustered_journal",
                                              "default-journal-cluster");
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

        // start a journal
        ClusteredJournal journal1 = startNewJournal("test-journal-node-1", "target/clustered_journal_1", "journal-cluster-1");
        // add 2 record to the 1st journal
        journal1.addRecords(new JournalRecord(TestChangeSet.create(journal1.journalId(), 1)));
        journal1.addRecords(new JournalRecord(TestChangeSet.create(journal1.journalId(), 1)));

        // start another journal
        ClusteredJournal journal2 = startNewJournal("test-journal-node-2", "target/clustered_journal_2", "journal-cluster-1");
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
        journal2.clusteringService().start();
        journal2.start();

        Assert.assertEquals(4, journal2.allRecords(false).size());
        journal2.shutdown();
        journal1.shutdown();
    }

    private ClusteredJournal startNewJournal( String processId,
                                              String fileLocation,
                                              String clusterName ) throws Exception {
        ClusteringService clusteringService = new ClusteringService(processId,
                                                                    ClusteringHelper.createClusteringConfiguration(clusterName));
        clusteringService.start();
        clusteringServices.add(clusteringService);

        FileUtil.delete(fileLocation);
        LocalJournal localJournal = new LocalJournal(fileLocation);
        ClusteredJournal clusteredJournal = new ClusteredJournal(localJournal, clusteringService);
        clusteredJournal.start();

        return clusteredJournal;
    }
}
