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
package org.modeshape.jcr.value.binary.infinispan;

import java.net.InetAddress;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.jgroups.Global;
import org.modeshape.jcr.bus.ClusteredRepositoryChangeBusTest;

public class InfinispanTestUtil {

    /**
     * @param networked true if the Infinispan cache is clustered, or false otherwise
     * @return created and started CacheManager
     * @throws Exception if there is a problem starting the cache
     */
    public static DefaultCacheManager beforeClassStartup( boolean networked ) throws Exception {
        if (networked) {
            InetAddress localHost = ClusteredRepositoryChangeBusTest.getLocalHost();
            System.setProperty(Global.BIND_ADDR, localHost.getHostAddress());
            System.setProperty(Global.EXTERNAL_ADDR, localHost.getHostAddress());
        }
        GlobalConfigurationBuilder globalConfigurationBuilder = new GlobalConfigurationBuilder();
        if (networked) {
            globalConfigurationBuilder.transport().defaultTransport();
        } else {
            globalConfigurationBuilder.transport().transport(null);
        }
        return new DefaultCacheManager(globalConfigurationBuilder.build(), true);
    }

    public static void afterClassShutdown( DefaultCacheManager cacheManager ) {
        if (cacheManager != null) {
            if (cacheManager.getCacheManagerConfiguration().transport() != null) {
                System.clearProperty(Global.BIND_ADDR);
                System.clearProperty(Global.EXTERNAL_ADDR);
            }
            cacheManager.stop();
        }
    }
}
