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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.jcr.ClusteringHelper;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.cache.change.ChangeSetListener;
import org.modeshape.jcr.clustering.ClusteringService;

/**
 * Unit test for {@link ClusteredChangeBus}
 * 
 * @author Horia Chiorean
 */
public class ClusteredChangeBusTest extends AbstractChangeBusTest {

    private static List<ClusteringService> clusteringServices;

    private ExecutorService executorService = Executors.newCachedThreadPool();
    private List<ChangeBus> buses = new ArrayList<>();
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        ClusteringHelper.bindJGroupsToLocalAddress();
        clusteringServices = IntStream.range(0, 3).mapToObj(i -> ClusteringService.startStandalone("clustered-change-bus-test",
                                                                                                   "config/cluster/jgroups-test-config.xml"))
                                      .collect(Collectors.toList());         
    }

    @AfterClass
    public static void afterClass() throws Exception {
        clusteringServices.forEach(ClusteringService::shutdown);
        ClusteringHelper.removeJGroupsBindings();
    }

    @Override
    protected ChangeBus createRepositoryChangeBus() throws Exception {
        return startNewBus(0);
    }

    @Override
    public void afterEach() {
        super.afterEach();
        try {
           buses.forEach(ChangeBus::shutdown);
        } finally {
            executorService.shutdownNow();
        }
    }
    
    @Test
    public void oneBusShouldNotifyRegisteredListeners() throws Exception {
        // Create three observers ...
        TestListener listener1 = new TestListener(1);
        
        startBusWithRegisteredListener(listener1);
       
        // Send a change from the first bus ...
        ChangeSet changeSet = new TestChangeSet("ws1");
        ChangeBus bus1 = buses.get(1);
        bus1.notify(changeSet);          
      
        listener1.assertExpectedEvents(changeSet);
    }
    
    @Test
    public void twoBusesShouldNotifyEachOther() throws Exception {
        // Create three observers ...
        TestListener listener1 = new TestListener(2);
        TestListener listener2 = new TestListener(2);
   
        startBusWithRegisteredListener(listener1, listener2);
    
        // Send changeSet to one of the buses ...
        ChangeSet changeSet1 = new TestChangeSet("bus1");
        buses.get(1).notify(changeSet1);
    
        ChangeSet changeSet2 = new TestChangeSet("bus2");
        buses.get(2).notify(changeSet2);    
        
        // Wait for the observers to be notified ...
        listener1.assertExpectedEvents(changeSet1, changeSet2);
        listener2.assertExpectedEvents(changeSet1, changeSet2);
    } 
    
    @Test
    public void shouldNotSendChangesIfBusIsShutdown() throws Exception {
        // Create three observers ...
        TestListener listener1 = new TestListener(1);
        TestListener listener2 = new TestListener(1);
        TestListener listener3 = new TestListener(1);
    
        startBusWithRegisteredListener(listener1, listener2, listener3);    
      
        // Send changeSet to one of the buses ...
        ChangeSet changeSet = new TestChangeSet("bus3");
        buses.get(3).notify(changeSet);
        
        listener1.assertExpectedEvents(changeSet);
        listener2.assertExpectedEvents(changeSet);
        listener3.assertExpectedEvents(changeSet);
    
        // shut down buses
        buses.get(3).shutdown();
        buses.get(2).shutdown();
        
        listener3.clear();
        listener2.clear();
        
        changeSet = new TestChangeSet("bus1");
        buses.get(1).notify(changeSet);
    
        listener2.assertNoEvents();
        listener3.assertNoEvents();
    }
    
    private ClusteredChangeBus startNewBus(int clusteringServiceIdx) throws Exception {
        ChangeBus internalBus = new RepositoryChangeBus("repo", executorService);
        ClusteredChangeBus bus = new ClusteredChangeBus(internalBus, clusteringServices.get(clusteringServiceIdx));
        bus.start();
        buses.add(bus);
        return bus;
    }
    
    private void startBusWithRegisteredListener(ChangeSetListener... listeners) throws Exception {
        for (int i = 0; i < listeners.length; i++) {
            ClusteredChangeBus bus = startNewBus(i);
            bus.register(listeners[i]);
        }
    }
}
