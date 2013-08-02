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

package org.modeshape.jcr;

import org.jgroups.Global;
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
}
