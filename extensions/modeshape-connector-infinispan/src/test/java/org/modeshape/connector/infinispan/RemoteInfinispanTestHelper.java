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
package org.modeshape.connector.infinispan;

import org.infinispan.config.Configuration;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.loaders.dummy.DummyInMemoryCacheStore;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.server.core.Main;
import org.infinispan.server.hotrod.HotRodServer;
import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.util.Properties;

/**
 * @author johnament
 */
public class RemoteInfinispanTestHelper {
    protected static final int PORT = 11311;
    protected static final int TIMEOUT = 0;
    private static HotRodServer server = null;
    private static int count = 0;

    public static synchronized HotRodServer createServer() throws IOException {
        count++;
        if (server == null) {
            DefaultCacheManager cacheManager = new DefaultCacheManager(new GlobalConfiguration());
            cacheManager.defineConfiguration("cars", createDummyCacheConfiguration("cars"));
            cacheManager.defineConfiguration("remowritable", createDummyCacheConfiguration("remowritable"));
            cacheManager.defineConfiguration("aircraft", createDummyCacheConfiguration("aircraft"));
            cacheManager.defineConfiguration("default", createDummyCacheConfiguration("default"));
            cacheManager.defineConfiguration("copyChildrenSource", createDummyCacheConfiguration("copyChildrenSource"));

            server = new HotRodServer();
            String hostAddress = hostAddress();
            String hostPort = Integer.toString(hostPort());
            String timeoutStr = Integer.toString(TIMEOUT);
            Properties props = new Properties();
            props.setProperty(Main.PROP_KEY_HOST(), hostAddress);
            props.setProperty(Main.PROP_KEY_PORT(), hostPort);
            props.setProperty(Main.PROP_KEY_IDLE_TIMEOUT(), timeoutStr);
            props.setProperty(Main.PROP_KEY_PROXY_HOST(), hostAddress);
            props.setProperty(Main.PROP_KEY_PROXY_PORT(), hostPort);

            server.start(props, cacheManager);
        }
        return server;
    }

    private static Configuration createDummyCacheConfiguration(String name) {
        Configuration dummyCacheConfiguration = new Configuration();
        DummyInMemoryCacheStore.Cfg dummyCacheCfg = new DummyInMemoryCacheStore.Cfg(name);
        dummyCacheCfg.setPurgeOnStartup(true);
        dummyCacheConfiguration.getCacheLoaderManagerConfig().addCacheLoaderConfig(dummyCacheCfg);
        return dummyCacheConfiguration;
    }

    public static int hostPort() {
        return PORT;
    }

    /**
     * Return the IP address of this host, in either IPv4 or IPv6 format.
     * 
     * @return the IP address as a string
     */
    public static String hostAddress() {
        try {
            InetAddress localHost = InetAddress.getLocalHost();
            if (localHost instanceof Inet4Address) {
                return "127.0.0.1";
            }
            return "::1";
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static synchronized void releaseServer() {
        --count;
        if (count <= 0) {
            try {
                // System.out.println("Stopping HotRot Server at " + hostAddress() + ":" + hostPort());
                server.stop();
            } finally {
                server = null;
            }
        }
    }
}
