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
package org.modeshape.clustering;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.observe.Changes;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.request.ChangeRequest;
import org.modeshape.graph.request.CreateNodeRequest;

public class ClusteredObservationBusIntegerationTest {

    private ExecutionContext context = new ExecutionContext();

    @Test
    public void shouldProperlySendChangesThroughRealJGroupsCluster() throws Exception {

        // Create three observers ...
        CustomObserver observer1 = new CustomObserver();
        CustomObserver observer2 = new CustomObserver();
        CustomObserver observer3 = new CustomObserver();

        // Create three busses using a real JGroups cluster ...
        String name = "MyCluster";
        ClusteredObservationBus bus1 = startNewBus(name, observer1);
        try {
            // ------------------------------------
            // Send a change from the first bus ...
            // ------------------------------------

            // Set the observers to expect one event ...
            observer1.expectChanges(1);
            observer2.expectChanges(0); // shutdown
            observer3.expectChanges(0); // shutdown

            // Send changes to one of the busses ...
            Changes changes = changes();
            bus1.notify(changes);

            // Wait for the observers to be notified ...
            observer1.await();
            observer2.await();
            observer3.await();

            // Now verify that all of the observers received the notification ...
            assertThat(observer1.getObservedChanges().size(), is(1));
            assertThat(observer2.getObservedChanges().size(), is(0));
            assertThat(observer3.getObservedChanges().size(), is(0));
            assertThat(observer1.getObservedChanges().get(0), is(changes));

            // ------------------------------------
            // Create a second bus ...
            // ------------------------------------
            ClusteredObservationBus bus2 = startNewBus(name, observer2);
            try {
                // ------------------------------------
                // Send a change from the first bus ...
                // ------------------------------------

                // Set the observers to expect one event ...
                observer1.expectChanges(1);
                observer2.expectChanges(1);
                observer3.expectChanges(0); // shutdown

                // Send changes to one of the busses ...
                changes = changes();
                bus1.notify(changes);

                // Wait for the observers to be notified ...
                observer1.await();
                observer2.await();
                observer3.await();

                // Now verify that all of the observers received the notification ...
                assertThat(observer1.getObservedChanges().size(), is(1));
                assertThat(observer2.getObservedChanges().size(), is(1));
                assertThat(observer3.getObservedChanges().size(), is(0));
                assertThat(observer1.getObservedChanges().get(0), is(changes));
                assertThat(observer2.getObservedChanges().get(0), is(changes));

                // ------------------------------------
                // Send a change from the second bus ...
                // ------------------------------------

                // Set the observers to expect one event ...
                observer1.expectChanges(1);
                observer2.expectChanges(1);
                observer3.expectChanges(0); // shutdown

                // Send changes to one of the busses ...
                changes = changes();
                bus2.notify(changes);

                // Wait for the observers to be notified ...
                observer1.await();
                observer2.await();
                observer3.await();

                // Now verify that all of the observers received the notification ...
                assertThat(observer1.getObservedChanges().size(), is(1));
                assertThat(observer2.getObservedChanges().size(), is(1));
                assertThat(observer3.getObservedChanges().size(), is(0));
                assertThat(observer1.getObservedChanges().get(0), is(changes));
                assertThat(observer2.getObservedChanges().get(0), is(changes));

                // ------------------------------------
                // Create a second bus ...
                // ------------------------------------
                ClusteredObservationBus bus3 = startNewBus(name, observer3);
                try {

                    // ------------------------------------
                    // Send a change from the first bus ...
                    // ------------------------------------

                    // Set the observers to expect one event ...
                    observer1.expectChanges(1);
                    observer2.expectChanges(1);
                    observer3.expectChanges(1);

                    // Send changes to one of the busses ...
                    changes = changes();
                    bus1.notify(changes);

                    // Wait for the observers to be notified ...
                    observer1.await();
                    observer2.await();
                    observer3.await();

                    // Now verify that all of the observers received the notification ...
                    assertThat(observer1.getObservedChanges().size(), is(1));
                    assertThat(observer2.getObservedChanges().size(), is(1));
                    assertThat(observer3.getObservedChanges().size(), is(1));
                    assertThat(observer1.getObservedChanges().get(0), is(changes));
                    assertThat(observer2.getObservedChanges().get(0), is(changes));
                    assertThat(observer3.getObservedChanges().get(0), is(changes));

                    // -------------------------------------
                    // Send a change from the second bus ...
                    // -------------------------------------

                    // Set the observers to expect one event ...
                    observer1.expectChanges(1);
                    observer2.expectChanges(1);
                    observer3.expectChanges(1);

                    // Send changes to one of the busses ...
                    Changes changes2 = changes();
                    bus2.notify(changes2);

                    // Wait for the observers to be notified ...
                    observer1.await();
                    observer2.await();
                    observer3.await();

                    // Now verify that all of the observers received the notification ...
                    assertThat(observer1.getObservedChanges().size(), is(1));
                    assertThat(observer2.getObservedChanges().size(), is(1));
                    assertThat(observer3.getObservedChanges().size(), is(1));
                    assertThat(observer1.getObservedChanges().get(0), is(changes2));
                    assertThat(observer2.getObservedChanges().get(0), is(changes2));
                    assertThat(observer3.getObservedChanges().get(0), is(changes2));

                    // ------------------------------------
                    // Send a change from the third bus ...
                    // ------------------------------------

                    // Set the observers to expect one event ...
                    observer1.expectChanges(1);
                    observer2.expectChanges(1);
                    observer3.expectChanges(1);

                    // Send changes to one of the busses ...
                    Changes changes3 = changes();
                    bus3.notify(changes3);

                    // Wait for the observers to be notified ...
                    observer1.await();
                    observer2.await();
                    observer3.await();

                    // Now verify that all of the observers received the notification ...
                    assertThat(observer1.getObservedChanges().size(), is(1));
                    assertThat(observer2.getObservedChanges().size(), is(1));
                    assertThat(observer3.getObservedChanges().size(), is(1));
                    assertThat(observer1.getObservedChanges().get(0), is(changes3));
                    assertThat(observer2.getObservedChanges().get(0), is(changes3));
                    assertThat(observer3.getObservedChanges().get(0), is(changes3));

                    // ---------------------------------------
                    // Stop the busses! I want to get off! ...
                    // ---------------------------------------
                } finally {
                    bus3.shutdown();
                }
            } finally {
                // ------------------------------------
                // Send a change from the second bus ...
                // ------------------------------------

                // Set the observers to expect one event ...
                observer1.expectChanges(1);
                observer2.expectChanges(1);
                observer3.expectChanges(0); // shutdown

                // Send changes to one of the busses ...
                changes = changes();
                bus2.notify(changes);

                // Wait for the observers to be notified ...
                observer1.await();
                observer2.await();
                observer3.await();

                // Now verify that all of the observers received the notification ...
                assertThat(observer1.getObservedChanges().size(), is(1));
                assertThat(observer2.getObservedChanges().size(), is(1));
                assertThat(observer3.getObservedChanges().size(), is(0));
                assertThat(observer1.getObservedChanges().get(0), is(changes));
                assertThat(observer2.getObservedChanges().get(0), is(changes));

                bus2.shutdown();
            }
        } finally {
            // ------------------------------------
            // Send a change from the first bus ...
            // ------------------------------------

            // Set the observers to expect one event ...
            observer1.expectChanges(1);
            observer2.expectChanges(0); // shutdown
            observer3.expectChanges(0); // shutdown

            // Send changes to one of the busses ...
            Changes changes = changes();
            bus1.notify(changes);

            // Wait for the observers to be notified ...
            observer1.await();
            observer2.await();
            observer3.await();

            // Now verify that all of the observers received the notification ...
            assertThat(observer1.getObservedChanges().size(), is(1));
            assertThat(observer2.getObservedChanges().size(), is(0));
            assertThat(observer3.getObservedChanges().size(), is(0));
            assertThat(observer1.getObservedChanges().get(0), is(changes));

            bus1.shutdown();
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Utility methods
    // ----------------------------------------------------------------------------------------------------------------

    protected ClusteredObservationBus startNewBus( String name,
                                                   Observer localObserver ) {
        ClusteredObservationBus bus = new ClusteredObservationBus();
        bus.setClusterName(name);
        bus.start();
        bus.register(localObserver);
        return bus;
    }

    protected Changes changes() {
        DateTime now = context.getValueFactories().getDateFactory().create();
        Path path = context.getValueFactories().getPathFactory().create("/a");
        Name childName = context.getValueFactories().getNameFactory().create("b");
        Path childPath = context.getValueFactories().getPathFactory().create(path, childName);
        CreateNodeRequest request = new CreateNodeRequest(Location.create(path), "workspaceName", childName);
        request.setActualLocationOfNode(Location.create(childPath));
        List<ChangeRequest> requests = Collections.singletonList((ChangeRequest)request);
        return new Changes("processId", "contextId", "username", "sourceName", now, requests, null);
    }

    protected static class CustomObserver implements Observer {
        private final List<Changes> receivedChanges = new ArrayList<Changes>();
        private CountDownLatch latch;

        public void expectChanges( int expectedNumberOfChanges ) {
            latch = new CountDownLatch(expectedNumberOfChanges);
            receivedChanges.clear();
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.observe.Observer#notify(org.modeshape.graph.observe.Changes)
         */
        @Override
        public void notify( Changes changes ) {
            receivedChanges.add(changes);
            latch.countDown();
        }

        public void await() throws InterruptedException {
            latch.await(250, TimeUnit.MILLISECONDS);
        }

        public List<Changes> getObservedChanges() {
            return receivedChanges;
        }
    }
}
