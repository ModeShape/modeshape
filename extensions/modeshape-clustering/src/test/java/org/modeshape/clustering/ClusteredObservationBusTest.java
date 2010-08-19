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
import static org.mockito.Mockito.stub;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import java.util.Collections;
import java.util.List;
import java.util.Vector;
import org.jgroups.Address;
import org.jgroups.ChannelListener;
import org.jgroups.JChannel;
import org.jgroups.Message;
import org.jgroups.Receiver;
import org.jgroups.View;
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
    protected View mockView;
    protected Vector<Address> channelMembers;

    @Before
    public void beforeEach() {
        // Now create a mock JChannel and our bus ...
        final JChannel mockChannel = Mockito.mock(JChannel.class);
        this.mockChannel = mockChannel;
        this.bus = new ClusteredObservationBus() {
            @Override
            protected JChannel newChannel( String configuration ) {
                return mockChannel;
            }
        };
        // Create a mocked view; the only thing ClusteredObservationBus uses it for is to
        // call 'channel.getView().getMembers().size()', so mock these objects so that the
        // size is 'numberOfMembers'
        channelMembers = new Vector<Address>();
        mockView = Mockito.mock(View.class);
        stub(mockChannel.getView()).toReturn(mockView);
        stub(mockView.getMembers()).toReturn(channelMembers);
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

        // Get the bus' receiver so we can set the view ...
        ArgumentCaptor<Receiver> receiverArgument = ArgumentCaptor.forClass(Receiver.class);
        verify(mockChannel, times(1)).setReceiver(receiverArgument.capture());

        // Pretend that the channel has two members ...
        setChannelMemberCount(2);
        receiverArgument.getValue().viewAccepted(mockView);

        // When connected, JGroups will call back to the listener and the bus will record it as open.
        // But we have to do this manually because we've stubbed out JGroups ...
        bus.isOpen.set(true);

        // Now call the notify method ...
        bus.notify(changes());
        verify(mockChannel, times(1)).send(ArgumentCaptor.forClass(Message.class).capture());
        verifyNoMoreInteractions(mockChannel);
    }

    @Test
    public void shouldAllowNotifyToBeCalledAfterStartWithOneMemberAndShouldSendMessageToLocalObserversBuNotJGroups()
        throws Exception {
        // Pretend that the channel has two members ...
        setChannelMemberCount(1);

        bus.setClusterName("clusterName");
        bus.start();
        verify(mockChannel, times(1)).addChannelListener(ArgumentCaptor.forClass(ChannelListener.class).capture());
        verify(mockChannel, times(1)).connect("clusterName");

        // Get the bus' receiver so we can set the view ...
        ArgumentCaptor<Receiver> receiverArgument = ArgumentCaptor.forClass(Receiver.class);
        verify(mockChannel, times(1)).setReceiver(receiverArgument.capture());

        // Pretend that the channel has only one member ...
        setChannelMemberCount(1);
        receiverArgument.getValue().viewAccepted(mockView);

        // When connected, JGroups will call back to the listener and the bus will record it as open.
        // But we have to do this manually because we've stubbed out JGroups ...
        bus.isOpen.set(true);

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

    // ----------------------------------------------------------------------------------------------------------------
    // Utility methods
    // ----------------------------------------------------------------------------------------------------------------

    protected void setChannelMemberCount( int count ) {
        while (channelMembers.size() > count) {
            channelMembers.remove(channelMembers.size() - 1);
        }
        while (channelMembers.size() < count) {
            channelMembers.add(Mockito.mock(Address.class));
        }
    }

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
}
