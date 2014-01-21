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

package org.modeshape.jcr.bus;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.List;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.ClusteringHelper;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.clustering.ClusteringService;

/**
 * Unit test for {@link ClusteredRepositoryChangeBus}
 *
 * @author Horia Chiorean
 */
public class ClusteredRepositoryChangeBusTest extends RepositoryChangeBusTest {

    private ClusteredRepositoryChangeBus defaultBus;
    private List<ChangeBus> buses = new ArrayList<ChangeBus>();
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
    protected ChangeBus getChangeBus() throws Exception {
        if (defaultBus == null) {
            defaultBus = startNewBus();
        }
        return defaultBus;
    }

    @Override
    public void afterEach() {
        for (ChangeBus bus : buses) {
            bus.shutdown();
        }
        for (ClusteringService clusteringService : clusteringServices) {
            clusteringService.shutdown();
        }
        defaultBus = null;
    }

    @Test
    public void shouldSendChangeSetThroughCluster() throws Exception {
        // Create three observers ...
        TestListener listener1 = new TestListener();
        TestListener listener2 = new TestListener();
        TestListener listener3 = new TestListener();

        // Create three buses using a real JGroups cluster ...
        ClusteredRepositoryChangeBus bus1 = startNewBus();
        bus1.register(listener1);
        // ------------------------------------
        // Send a change from the first bus ...
        // ------------------------------------

        // Set the observers to expect one event ...
        listener1.expectChangeSet(1);
        listener2.expectChangeSet(0); // shutdown
        listener3.expectChangeSet(0); // shutdown

        // Send changeSet to one of the buses ...
        ChangeSet changeSet = new TestChangeSet("ws1");
        bus1.notify(changeSet);

        // Wait for the observers to be notified ...
        listener1.await();
        listener2.await();
        listener3.await();

        // Now verify that all of the observers received the notification ...
        assertThat(listener1.getObservedChangeSet().size(), is(1));
        assertThat(listener2.getObservedChangeSet().size(), is(0));
        assertThat(listener3.getObservedChangeSet().size(), is(0));
        assertThat(listener1.getObservedChangeSet().get(0), is(changeSet));

        // ------------------------------------
        // Create a second bus ...
        // ------------------------------------
        ClusteredRepositoryChangeBus bus2 = startNewBus();
        bus2.register(listener2);

        // ------------------------------------
        // Send a change from the first bus ...
        // ------------------------------------

        // Set the observers to expect one event ...
        listener1.expectChangeSet(1);
        listener2.expectChangeSet(1);
        listener3.expectChangeSet(0); // shutdown

        // Send changeSet to one of the buses ...
        changeSet = new TestChangeSet("ws1");
        bus1.notify(changeSet);

        // Wait for the observers to be notified ...
        listener1.await();
        listener2.await();
        listener3.await();

        // Now verify that all of the observers received the notification ...
        assertThat(listener1.getObservedChangeSet().size(), is(1));
        assertThat(listener2.getObservedChangeSet().size(), is(1));
        assertThat(listener3.getObservedChangeSet().size(), is(0));
        assertThat(listener1.getObservedChangeSet().get(0), is(changeSet));
        assertThat(listener2.getObservedChangeSet().get(0), is(changeSet));

        // ------------------------------------
        // Send a change from the second bus ...
        // ------------------------------------

        // Set the observers to expect one event ...
        listener1.expectChangeSet(1);
        listener2.expectChangeSet(1);
        listener3.expectChangeSet(0); // shutdown

        // Send changeSet to one of the buses ...
        changeSet = new TestChangeSet("ws2");
        bus2.notify(changeSet);

        // Wait for the observers to be notified ...
        listener1.await();
        listener2.await();
        listener3.await();

        // Now verify that all of the observers received the notification ...
        assertThat(listener1.getObservedChangeSet().size(), is(1));
        assertThat(listener2.getObservedChangeSet().size(), is(1));
        assertThat(listener3.getObservedChangeSet().size(), is(0));
        assertThat(listener1.getObservedChangeSet().get(0), is(changeSet));
        assertThat(listener2.getObservedChangeSet().get(0), is(changeSet));

        // ------------------------------------
        // Create a third bus ...
        // ------------------------------------
        ClusteredRepositoryChangeBus bus3 = startNewBus();
        bus3.register(listener3);
        // ------------------------------------
        // Send a change from the first bus ...
        // ------------------------------------

        // Set the observers to expect one event ...
        listener1.expectChangeSet(1);
        listener2.expectChangeSet(1);
        listener3.expectChangeSet(1);

        // Send changeSet to one of the buses ...
        changeSet = new TestChangeSet("ws1");
        bus1.notify(changeSet);

        // Wait for the observers to be notified ...
        listener1.await();
        listener2.await();
        listener3.await();

        // Now verify that all of the observers received the notification ...
        assertThat(listener1.getObservedChangeSet().size(), is(1));
        assertThat(listener2.getObservedChangeSet().size(), is(1));
        assertThat(listener3.getObservedChangeSet().size(), is(1));
        assertThat(listener1.getObservedChangeSet().get(0), is(changeSet));
        assertThat(listener2.getObservedChangeSet().get(0), is(changeSet));
        assertThat(listener3.getObservedChangeSet().get(0), is(changeSet));

        // -------------------------------------
        // Send a change from the second bus ...
        // -------------------------------------

        // Set the observers to expect one event ...
        listener1.expectChangeSet(1);
        listener2.expectChangeSet(1);
        listener3.expectChangeSet(1);

        // Send changeSet to one of the buses ...
        ChangeSet changeSet2 = new TestChangeSet("ws2");
        bus2.notify(changeSet2);

        // Wait for the observers to be notified ...
        listener1.await();
        listener2.await();
        listener3.await();

        // Now verify that all of the observers received the notification ...
        assertThat(listener1.getObservedChangeSet().size(), is(1));
        assertThat(listener2.getObservedChangeSet().size(), is(1));
        assertThat(listener3.getObservedChangeSet().size(), is(1));
        assertThat(listener1.getObservedChangeSet().get(0), is(changeSet2));
        assertThat(listener2.getObservedChangeSet().get(0), is(changeSet2));
        assertThat(listener3.getObservedChangeSet().get(0), is(changeSet2));

        // ------------------------------------
        // Send a change from the third bus ...
        // ------------------------------------

        // Set the observers to expect one event ...
        listener1.expectChangeSet(1);
        listener2.expectChangeSet(1);
        listener3.expectChangeSet(1);

        // Send changeSet to one of the buses ...
        ChangeSet changeSet3 = new TestChangeSet("ws3");
        bus3.notify(changeSet3);

        // Wait for the observers to be notified ...
        listener1.await();
        listener2.await();
        listener3.await();

        // Now verify that all of the observers received the notification ...
        assertThat(listener1.getObservedChangeSet().size(), is(1));
        assertThat(listener2.getObservedChangeSet().size(), is(1));
        assertThat(listener3.getObservedChangeSet().size(), is(1));
        assertThat(listener1.getObservedChangeSet().get(0), is(changeSet3));
        assertThat(listener2.getObservedChangeSet().get(0), is(changeSet3));
        assertThat(listener3.getObservedChangeSet().get(0), is(changeSet3));

        // ---------------------------------------
        // Stop the buses! I want to get off! ...
        // ---------------------------------------
        bus3.shutdown();
        // ------------------------------------
        // Send a change from the second bus ...
        // ------------------------------------

        // Set the observers to expect one event ...
        listener1.expectChangeSet(1);
        listener2.expectChangeSet(1);
        listener3.expectChangeSet(0); // shutdown

        // Send changeSet to one of the buses ...
        changeSet = new TestChangeSet("ws2");
        bus2.notify(changeSet);

        // Wait for the observers to be notified ...
        listener1.await();
        listener2.await();
        listener3.await();

        // Now verify that all of the observers received the notification ...
        assertThat(listener1.getObservedChangeSet().size(), is(1));
        assertThat(listener2.getObservedChangeSet().size(), is(1));
        assertThat(listener3.getObservedChangeSet().size(), is(0));
        assertThat(listener1.getObservedChangeSet().get(0), is(changeSet));
        assertThat(listener2.getObservedChangeSet().get(0), is(changeSet));

        bus2.shutdown();
        // ------------------------------------
        // Send a change from the first bus ...
        // ------------------------------------

        // Set the observers to expect one event ...
        listener1.expectChangeSet(1);
        listener2.expectChangeSet(0); // shutdown
        listener3.expectChangeSet(0); // shutdown

        // Send changeSet to one of the buses ...
        changeSet = new TestChangeSet("ws1");
        bus1.notify(changeSet);

        // Wait for the observers to be notified ...
        listener1.await();
        listener2.await();
        listener3.await();

        // Now verify that all of the observers received the notification ...
        assertThat(listener1.getObservedChangeSet().size(), is(1));
        assertThat(listener2.getObservedChangeSet().size(), is(0));
        assertThat(listener3.getObservedChangeSet().size(), is(0));
        assertThat(listener1.getObservedChangeSet().get(0), is(changeSet));
    }

    private ClusteredRepositoryChangeBus startNewBus() throws Exception {
        ClusteringService clusteringService = new ClusteringService("test-bus-process",
                                                                     ClusteringHelper.createClusteringConfiguration("testcluster-event-bus"));
        clusteringService.start();
        clusteringServices.add(clusteringService);

        ClusteredRepositoryChangeBus bus = new ClusteredRepositoryChangeBus(super.createRepositoryChangeBus(),
                                                                            clusteringService);
        bus.start();
        buses.add(bus);
        return bus;
    }
}
