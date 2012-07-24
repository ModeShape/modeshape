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

package org.modeshape.jcr.bus;

import static org.hamcrest.core.Is.is;
import org.jgroups.Global;
import org.junit.AfterClass;
import static org.junit.Assert.assertThat;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.modeshape.jcr.RepositoryConfiguration;
import org.modeshape.jcr.cache.change.ChangeSet;
import org.modeshape.jcr.clustering.DefaultChannelProvider;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for {@link ClusteredRepositoryChangeBus}
 *
 * @author Horia Chiorean
 */
public class ClusteredRepositoryChangeBusTest extends RepositoryChangeBusTest {

    private static final String CLUSTER_NAME = "testcluster-event-bus";

    private ClusteredRepositoryChangeBus defaultBus;

    private List<ChangeBus> buses;
    
    @BeforeClass
    public static void beforeClass() throws Exception {
        InetAddress localHost = getLocalHost();
        System.setProperty(Global.BIND_ADDR, localHost.getHostAddress());
        System.setProperty(Global.EXTERNAL_ADDR, localHost.getHostAddress());
    }

    private static InetAddress getLocalHost() throws UnknownHostException {
        String ipv6Prop = System.getProperty(Global.IPv6);
        boolean preferIpv6 = ipv6Prop != null && Boolean.TRUE.toString().equalsIgnoreCase(ipv6Prop);

        InetAddress localHost = null;
        InetAddress[] localHostAddresses = InetAddress.getAllByName("localhost");
        for (InetAddress localAddress : localHostAddresses) {
            if (preferIpv6 && localAddress instanceof Inet6Address) {
                localHost = localAddress;
                break;
            } else if (!preferIpv6 && !(localAddress instanceof Inet6Address)) {
                localHost = localAddress;
                break;
            }
        }
        assert localHost != null;
        return localHost;
    }

    @AfterClass
    public static void afterClass() throws Exception {
        System.clearProperty(Global.BIND_ADDR);
        System.clearProperty(Global.EXTERNAL_ADDR);
    }
    
    @Override
    public void beforeEach() {
        buses = new ArrayList<ChangeBus>();
    }

    @Override
    protected ChangeBus getChangeBus() {
        if (defaultBus == null) {
            defaultBus = startNewBus(CLUSTER_NAME);
        }
        return defaultBus;
    }

    @Override
    public void afterEach() {
        for (ChangeBus bus : buses) {
            bus.shutdown();
        }
        defaultBus = null;
    }

    @Test( expected = IllegalStateException.class )
    public void shouldNotAllowSettingClusterNameToNull() {
        startNewBus(null);
    }

    @Test
    public void shouldAllowSettingClusterNameToBlankString() {
        startNewBus("");
    }

    @Test
    public void shouldAllowSettingClusterNameToStringWithAlphaNumericCharacters() {
        startNewBus("abcdefghijklmnopqrstuvwxyz0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ");
    }

    @Test
    public void shouldAllowSettingClusterNameToStringWithAlphaNumericAndPunctuationCharacters() {
        startNewBus("valid.cluster!name@#$%^&*()<>?,./:\"'[]\\{}|_+-=");
    }

    @Test
    public void shouldAllowSettingClusterNameToStringWithAlphaNumericAndWhitespaceCharacters() {
        startNewBus("valid cluster name");
    }

    @Test
    public void shouldSendChangeSetThroughRealJGroupsCluster() throws Exception {
        // Create three observers ...
        TestListener listener1 = new TestListener();
        TestListener listener2 = new TestListener();
        TestListener listener3 = new TestListener();

        // Create three buses using a real JGroups cluster ...
        ClusteredRepositoryChangeBus bus1 = startNewBus(CLUSTER_NAME);
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
        ClusteredRepositoryChangeBus bus2 = startNewBus(CLUSTER_NAME);
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
        ClusteredRepositoryChangeBus bus3 = startNewBus(CLUSTER_NAME);
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

    private ClusteredRepositoryChangeBus startNewBus( String name) {
        ClusteredRepositoryChangeBus bus = new ClusteredRepositoryChangeBus(createClusteringConfiguration(name), super.createRepositoryChangeBus());
        bus.start();
        buses.add(bus);
        return bus;
    }
    
    private RepositoryConfiguration.Clustering createClusteringConfiguration(String clusterName) {
        RepositoryConfiguration.Clustering repositoryConfiguration = mock(RepositoryConfiguration.Clustering.class);
        when(repositoryConfiguration.isEnabled()).thenReturn(true);
        when(repositoryConfiguration.getClusterName()).thenReturn(clusterName);
        when(repositoryConfiguration.getChannelProviderClassName()).thenReturn(DefaultChannelProvider.class.getName());
        return repositoryConfiguration;
    }
}
