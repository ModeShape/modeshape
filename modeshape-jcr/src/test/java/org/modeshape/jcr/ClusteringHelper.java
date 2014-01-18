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

package org.modeshape.jcr;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import org.jgroups.Global;
import org.modeshape.jcr.clustering.ClusteringService;
import org.modeshape.jcr.clustering.DefaultChannelProvider;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Utility class which should be used by tests running in JGroups clusters.
 *
 * @author Horia Chiorean (hchiorea@redhat.com)
 */
public final class ClusteringHelper {

    private ClusteringHelper() {
    }

    /**
     * Binds JGroups to 'localhost'.
     *
     * @throws UnknownHostException if 'localhost' cannot be determined on the running machine.
     */
    public static void bindJGroupsToLocalAddress() throws UnknownHostException {
        InetAddress localHost = getLocalHost();
        System.setProperty(Global.BIND_ADDR, localHost.getHostAddress());
        System.setProperty(Global.EXTERNAL_ADDR, localHost.getHostAddress());
    }

    public static InetAddress getLocalHost() throws UnknownHostException {
        boolean preferIpv6 = Boolean.getBoolean(Global.IPv6);

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

    /**
     * Removes any custom JGroups bindings.
     */
    public static void removeJGroupsBindings() {
        System.clearProperty(Global.BIND_ADDR);
        System.clearProperty(Global.EXTERNAL_ADDR);
    }

    /**
     * Create a mock clustering configuration document.
     * @param clusterName the name of the cluster
     * @return a {@link RepositoryConfiguration.Clustering}
     */
    public static RepositoryConfiguration.Clustering createClusteringConfiguration(String clusterName) {
        RepositoryConfiguration.Clustering repositoryConfiguration = mock(RepositoryConfiguration.Clustering.class);
        when(repositoryConfiguration.isEnabled()).thenReturn(true);
        when(repositoryConfiguration.getClusterName()).thenReturn(clusterName);
        when(repositoryConfiguration.getChannelProviderClassName()).thenReturn(DefaultChannelProvider.class.getName());
        when(repositoryConfiguration.getChannelConfiguration()).thenReturn("config/jgroups-test-config.xml");
        return repositoryConfiguration;
    }

    public static ClusteringService startNewClusteringService(String clusterName) throws Exception {
        ClusteringService result = new ClusteringService("test-cluster-service", ClusteringHelper.createClusteringConfiguration(clusterName));
        result.start();
        return result;
    }
}
