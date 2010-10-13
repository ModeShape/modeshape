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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.naming.BinaryRefAddr;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.Name;
import javax.naming.NamingException;
import javax.naming.RefAddr;
import javax.naming.Reference;
import javax.naming.Referenceable;
import javax.naming.StringRefAddr;
import javax.naming.spi.ObjectFactory;
import net.jcip.annotations.ThreadSafe;
import org.infinispan.Cache;
import org.infinispan.client.hotrod.RemoteCacheManager;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.modeshape.common.annotation.Category;
import org.modeshape.common.annotation.Description;
import org.modeshape.common.annotation.Label;
import org.modeshape.common.i18n.I18n;
import org.modeshape.common.util.HashCode;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.cache.CachePolicy;
import org.modeshape.graph.connector.RepositoryConnection;
import org.modeshape.graph.connector.RepositoryContext;
import org.modeshape.graph.connector.RepositorySource;
import org.modeshape.graph.connector.RepositorySourceCapabilities;
import org.modeshape.graph.connector.RepositorySourceException;
import org.modeshape.graph.connector.base.BaseRepositorySource;
import org.modeshape.graph.connector.base.Connection;
import org.modeshape.graph.observe.Observer;

/**
 * A repository source that uses an Infinispan instance to manage the content. This source is capable of using an existing
 * {@link CacheContainer} or creating a new cache container. This process is controlled entirely by the JavaBean properties of the
 * InfinispanSource instance.
 * <p>
 * This source first attempts to find an existing cache manager found in {@link #getCacheContainerJndiName() JNDI} (or the
 * {@link DefaultCacheManager} if no such manager is available) and the {@link #getCacheConfigurationName() cache configuration
 * name} if supplied or the default configuration if not set.
 * </p>
 * <p>
 * Like other {@link RepositorySource} classes, instances of JBossCacheSource can be placed into JNDI and do support the creation
 * of {@link Referenceable JNDI referenceable} objects and resolution of references into JBossCacheSource.
 * </p>
 */
@ThreadSafe
public class RemoteInfinispanSource extends BaseInfinispanSource implements BaseRepositorySource, ObjectFactory {
    private static final long serialVersionUID = 1L;

    protected static final String CACHE_FACTORY_JNDI_NAME = "remoteInfinispanServerList";

    @Description( i18n = InfinispanConnectorI18n.class, value = "remoteInfinispanServerListPropertyDescription" )
    @Label( i18n = InfinispanConnectorI18n.class, value = "remoteInfinispanServerListPropertyLabel" )
    @Category( i18n = InfinispanConnectorI18n.class, value = "remoteInfinispanServerListPropertyCategory" )
    private volatile String remoteInfinispanServerList;

    /**
     * Get the name in JNDI of a {@link cacheContainer} instance that should be used to create the cache for this source.
     * <p>
     * This source first attempts to find a cache instance using the {@link cacheContainer} found in
     * {@link #getCacheContainerJndiName() JNDI} (or the {@link DefaultCacheManager} if no such manager is available) and the
     * {@link #getCacheConfigurationName() cache configuration name} if supplied or the default configuration if not set.
     * </p>
     *
     * @return the JNDI name of the {@link cacheContainer} instance that should be used, or null if the {@link DefaultCacheManager}
     *         should be used if a cache is to be created
     * @see #setcacheContainerJndiName(String)
     * @see #getCacheConfigurationName()
     */
    public String getRemoteInfinispanServerList() {
        return remoteInfinispanServerList;
    }

    /**
     *
     * @param remoteInfinispanServerList the server list in appropriate server:port;server2:port2 format.
     */
    public synchronized void setRemoteInfinispanServerList( String remoteInfinispanServerList ) {
        if (this.remoteInfinispanServerList == remoteInfinispanServerList || this.remoteInfinispanServerList != null
            && this.remoteInfinispanServerList.equals(remoteInfinispanServerList)) return; // unchanged
        this.remoteInfinispanServerList = remoteInfinispanServerList;
    }

    @Override
    protected CacheContainer createCacheContainer() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getObjectInstance(Object obj, Name name, Context nameCtx, Hashtable<?, ?> environment) throws Exception {
        throw new UnsupportedOperationException("Not supported yet.");
    }

}
