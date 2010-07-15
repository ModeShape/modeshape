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
import static org.hamcrest.core.IsNot.not;
import static org.hamcrest.core.IsNull.nullValue;
import static org.hamcrest.core.IsSame.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.jgroups.ChannelListener;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.observe.Changes;
import org.modeshape.graph.observe.Observer;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.request.ChangeRequest;
import org.modeshape.graph.request.CreateNodeRequest;

public class ClusteredObservationBusTest {

    private ClusteredObservationBus bus;
    private ExecutionContext context = new ExecutionContext();
    protected JChannel mockChannel;

    @Before
    public void beforeEach() {
        mockChannel = Mockito.mock(JChannel.class);
        // Create a clustered bus that does NOT use a real JChannel ...
        bus = newBus(mockChannel);
    }

    @Test
    public void shouldSerializeAndDeserializeChanges() throws Exception {
        Changes changes = changes();
        byte[] data = ClusteredObservationBus.serialize(changes);
        Changes deserialized = ClusteredObservationBus.deserialize(data);
        // Should be equal ...
        assertThat(changes, is(deserialized));
        // but not == ...
        assertThat(changes, is(not(sameInstance(deserialized))));
    }

    @Test( expected = IllegalArgumentException.class )
    public void shouldNotAllowSettingClusterNameToNull() {
        bus.setClusterName(null);
    }

    @Test
    public void shouldAllowSettingClusterNameToBlankString() {
        setAndGetClusterName("");
    }

    @Test
    public void shouldAllowSettingClusterNameToStringWithAlphaNumericCharacters() {
        setAndGetClusterName("abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }

    @Test
    public void shouldAllowSettingClusterNameToStringWithAlphaNumericAndPunctuationCharacters() {
        setAndGetClusterName("valid.cluster!name@#$%^&*()<>?,./:\"'[]\\{}|_+-=");
    }

    @Test
    public void shouldAllowSettingClusterNameToStringWithAlphaNumericAndWhitespaceCharacters() {
        setAndGetClusterName("valid cluster name");
    }

    @Test
    public void shouldAllowSettingConfigurationToNull() {
        setAndGetConfiguration(null);
    }

    @Test
    public void shouldAllowSettingConfigurationToBlankString() {
        setAndGetConfiguration(null);
    }

    @Test( expected = IllegalStateException.class )
    public void shouldNotAllowStartingWithoutSettingClusterName() {
        assertThat(bus.getClusterName(), is(nullValue()));
        assertThat(bus.isStarted(), is(false));
        bus.start();
        assertThat(bus.isStarted(), is(true));
        bus.shutdown();
        assertThat(bus.isStarted(), is(false));
    }

    @Test
    public void shouldAllowStartingWithoutSettingConfiguration() {
        bus.setClusterName("clusterName");
        assertThat(bus.isStarted(), is(false));
        bus.start();
        assertThat(bus.isStarted(), is(true));
        bus.shutdown();
        assertThat(bus.isStarted(), is(false));
    }

    @Test
    public void shouldAllowShuttingDownWithoutHavingStarted() {
        assertThat(bus.isStarted(), is(false));
        bus.shutdown();
        assertThat(bus.isStarted(), is(false));
    }

    @Test( expected = IllegalStateException.class )
    public void shouldNotAllowSettingConfigurationAfterBusHasBeenStartedButBeforeBusHasBeenShutdown() {
        bus.setClusterName("clusterName");
        bus.setConfiguration("old configuration");
        assertThat(bus.isStarted(), is(false));
        bus.start();
        assertThat(bus.isStarted(), is(true));
        bus.setConfiguration("new configuration"); // !! should fail !!
    }

    @Test
    public void shouldAllowSettingConfigurationAfterBusHasBeenStartedAndShutdown() {
        bus.setClusterName("clusterName");
        bus.setConfiguration("old configuration");
        assertThat(bus.isStarted(), is(false));
        bus.start();
        assertThat(bus.isStarted(), is(true));
        bus.shutdown();
        assertThat(bus.isStarted(), is(false));
        bus.setConfiguration("new configuration");
    }

    @Test
    public void shouldAllowNotifyToBeCalledBeforeStartButShouldDoNothing() throws Exception {
        bus.setClusterName("clusterName");
        bus.notify(changes());
        ArgumentCaptor<Message> argument = ArgumentCaptor.forClass(Message.class);
        verify(mockChannel, never()).send(argument.capture());
    }

    @Test
    public void shouldAllowNotifyToBeCalledAfterStartWithMultipleMembersAndShouldSendMessageToJGroups() throws Exception {
        bus.setClusterName("clusterName");
        bus.start();
        verify(mockChannel, times(1)).addChannelListener(ArgumentCaptor.forClass(ChannelListener.class).capture());
        verify(mockChannel, times(1)).connect("clusterName");
        verify(mockChannel, times(1)).setReceiver(ArgumentCaptor.forClass(Receiver.class).capture());

        // When connected, JGroups will call back to the listener and the bus will record it as open.
        // But we have to do this manually because we've stubbed out JGroups ...
        bus.isOpen.set(true);

        // JGroups also normally calls Receiver.viewAccepted(...), and the bus' receiver sets whether there are
        // multiple members in the cluster. We need to set this manually because we've stubbed out JGroups ...
        bus.multipleAddressesInCluster.set(true);

        // Now call the notify method ...
        bus.notify(changes());
        verify(mockChannel, times(1)).send(ArgumentCaptor.forClass(Message.class).capture());
        verifyNoMoreInteractions(mockChannel);
    }

    @Test
    public void shouldAllowNotifyToBeCalledAfterStartWithOneMemberAndShouldSendMessageToLocalObserversBuNotJGroups()
        throws Exception {
        bus.setClusterName("clusterName");
        bus.start();
        verify(mockChannel, times(1)).addChannelListener(ArgumentCaptor.forClass(ChannelListener.class).capture());
        verify(mockChannel, times(1)).connect("clusterName");
        verify(mockChannel, times(1)).setReceiver(ArgumentCaptor.forClass(Receiver.class).capture());

        // When connected, JGroups will call back to the listener and the bus will record it as open.
        // But we have to do this manually because we've stubbed out JGroups ...
        bus.isOpen.set(true);

        // JGroups also normally calls Receiver.viewAccepted(...), and the bus' receiver sets whether there are
        // multiple members in the cluster. We need to set this manually because we've stubbed out JGroups ...
        bus.multipleAddressesInCluster.set(false);

        // Add a local listener ...
        Observer observer = mock(Observer.class);
        bus.register(observer);

        // Now call the notify method ...
        Changes changes = changes();
        bus.notify(changes);

        verify(mockChannel, never()).send(ArgumentCaptor.forClass(Message.class).capture());
        verifyNoMoreInteractions(mockChannel);
        verify(observer, times(1)).notify(changes);
    }

    @Test
    public void shouldProperlySendChangesThroughRealJGroupsCluster() throws Exception {

        // Create three observers ...
        CountDownLatch latch = new CountDownLatch(3);
        CustomObserver observer1 = new CustomObserver(latch);
        CustomObserver observer2 = new CustomObserver(latch);
        CustomObserver observer3 = new CustomObserver(latch);

        // Create three busses using a real JGroups cluster ...
        String name = "MyCluster";
        ClusteredObservationBus bus1 = startNewBus(name, observer1);
        try {
            ClusteredObservationBus bus2 = startNewBus(name, observer2);
            try {
                ClusteredObservationBus bus3 = startNewBus(name, observer3);
                try {

                    // Send changes to one of the busses ...
                    Changes changes = changes();
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

                    // Stop the busses ...
                } finally {
                    bus3.shutdown();
                }
            } finally {
                bus2.shutdown();
            }
        } finally {
            bus1.shutdown();
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Utility methods
    // ----------------------------------------------------------------------------------------------------------------

    protected void setAndGetClusterName( String name ) {
        bus.setClusterName(name);
        String nameAfter = bus.getClusterName();
        assertThat(nameAfter, is(name));
    }

    protected void setAndGetConfiguration( String config ) {
        bus.setConfiguration(config);
        String configAfter = bus.getConfiguration();
        assertThat(configAfter, is(config));
    }

    protected ClusteredObservationBus startNewBus( String name,
                                                   Observer localObserver ) {
        ClusteredObservationBus bus = newBus(null);
        bus.setClusterName(name);
        bus.start();
        bus.register(localObserver);
        return bus;
    }

    protected ClusteredObservationBus newBus( final JChannel channel ) {
        return channel == null ? new ClusteredObservationBus() : new ClusteredObservationBus() {
            /**
             * {@inheritDoc}
             * 
             * @see org.modeshape.clustering.ClusteredObservationBus#newChannel(java.lang.String)
             */
            @Override
            protected JChannel newChannel( String configuration ) {
                return channel;
            }
        };

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
        private final CountDownLatch latch;

        protected CustomObserver( CountDownLatch latch ) {
            this.latch = latch;
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
