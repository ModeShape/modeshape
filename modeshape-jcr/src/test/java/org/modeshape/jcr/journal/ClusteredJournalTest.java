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

import java.util.ArrayList;
import java.util.List;
import javax.jcr.RepositoryException;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.ClusteringHelper;
import org.modeshape.jcr.clustering.ClusteringService;
import junit.framework.Assert;

/**
 * Unit test for {@link ClusteredJournal}
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public class ClusteredJournalTest extends LocalJournalTest {

    private static final String PID1 = "test-journal-process1";
    private static final String PID2 = "test-journal-process2";

    private ClusteredJournal defaultJournal;
    private List<ClusteringService> clusteringServices = new ArrayList<ClusteringService>();

    @Override
    public void before() throws Exception {
        this.defaultJournal = startNewJournal("default-test-journal-process", "target/default_clustered_journal", "default-journal-cluster");
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
    public void after() throws RepositoryException {
        super.after();
        for (ClusteringService clusteringService : clusteringServices) {
            clusteringService.shutdown();
        }
    }

    @Test
    public void shouldReconcileDeltaInClusterWhenProcessesHaventSeenEachOther() throws Exception {
        //shut down the default journal
        after();

        //start a journal
        ClusteredJournal journal1 = startNewJournal(PID1, "target/clustered_journal_1", "journal-cluster-1");
        // add 1 record to the 1st journal
        journal1.addRecords(new JournalRecord(TestChangeSet.create(PID1, 1)));

        //start another journal
        ClusteredJournal journal2 = startNewJournal(PID2, "target/clustered_journal_2", "journal-cluster-1");
        // add 1 record to the 2nd journal
        journal2.addRecords(new JournalRecord(TestChangeSet.create(PID2, 1)));

        // shutdown the second journal
        journal2.clusteringService().shutdown();
        journal2.shutdown();

        // add 1 record to the 1st journal
        journal1.addRecords(new JournalRecord(TestChangeSet.create(PID1, 1)));

        // start the 2nd journal back and verify it received the delta from the 1st journal
        // in this case the delta should be everything the 1st process had because the 1st process
        // isn't clustered via the event bus with the 2nd process
        journal2.clusteringService().start();
        journal2.start();
        Thread.sleep(300);

        Assert.assertEquals(3, journal2.allRecords(false).size());
        journal2.shutdown();
        journal1.shutdown();
    }

    @Test
    public void shouldReconcileDeltaInClusterWhenProcessesHaveSeenEachOther() throws Exception {
        //shut down the default journal
        after();

        //start a journal
        ClusteredJournal journal1 = startNewJournal(PID1, "target/clustered_journal_1", "journal-cluster-1");
        // add 2 record to the 1st journal
        journal1.addRecords(new JournalRecord(TestChangeSet.create(PID1, 1)));
        journal1.addRecords(new JournalRecord(TestChangeSet.create(PID2, 1)));

        //start another journal
        ClusteredJournal journal2 = startNewJournal(PID2, "target/clustered_journal_2", "journal-cluster-1");
        // add 1 record to the 2nd journal
        journal2.addRecords(new JournalRecord(TestChangeSet.create(PID2, 1)));

        // shutdown the second journal
        journal2.clusteringService().shutdown();
        journal2.shutdown();

        // start the 2nd journal back and verify it received a 0 delta from the 1st journal
        // in this case the delta should be 0 because the 1st process should not have any "new" changes
        journal2.clusteringService().start();
        journal2.start();
        Thread.sleep(300);

        Assert.assertEquals(1, journal2.allRecords(false).size());
        journal2.shutdown();
        journal1.shutdown();
    }

    private ClusteredJournal startNewJournal( String processId, String fileLocation, String clusterName ) throws Exception {
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
