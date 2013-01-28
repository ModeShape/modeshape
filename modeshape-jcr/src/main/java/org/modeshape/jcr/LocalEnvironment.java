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

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.transaction.LockingMode;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.GenericTransactionManagerLookup;
import org.infinispan.transaction.lookup.TransactionManagerLookup;
import org.jgroups.Channel;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.DelegatingClassLoader;
import org.modeshape.common.util.StringURLClassLoader;
import org.modeshape.common.util.StringUtil;

/**
 * An {@link Environment} that can be used within a local (non-clustered) process.
 * <p>
 * To use a custom Environment instance, simply create a {@link RepositoryConfiguration} as usual but then call the
 * {@link RepositoryConfiguration#with(Environment)} with the Environment instance and then use the resulting
 * RepositoryConfiguration instance.
 * </p>
 * <p>
 * When a ModeShape {@link RepositoryConfiguration repository configuration} defines cache containers with configuration files on
 * the file system or the classpath, then a {@link LocalEnvironment} instance can be used as-is with no other configuration or
 * setup.
 * </p>
 * <p>
 * If applications wish to programmatically configure the Infinispan caches or cache containers, then those configurations can be
 * registered with a LocalEnvironment instance. Specifically, the {@link #addCacheContainer(String, CacheContainer)} and
 * {@link #addCacheContainerIfAbsent(String, CacheContainer)} methods register a programmatically created instance of a
 * {@link CacheContainer}. Alternatively, the {@link #defineCache(String, String, Configuration)} method can be used to register a
 * named cache with a programmatically created {@link Configuration Infinispan cache configuration}.
 * </p>
 */
public class LocalEnvironment implements Environment {

    public static final Class<? extends TransactionManagerLookup> DEFAULT_TRANSACTION_MANAGER_LOOKUP_CLASS = GenericTransactionManagerLookup.class;

    /**
     * The name for the default cache container that is used when {@link #getCacheContainer()} is called or if null is supplied as
     * the name in {@link #getCacheContainer(String)}.
     */
    public static final String DEFAULT_CONFIGURATION_NAME = "defaultCacheContainer";

    private final Class<? extends TransactionManagerLookup> transactionManagerLookupClass;
    private final ConcurrentMap<String, CacheContainer> containers = new ConcurrentHashMap<String, CacheContainer>();
    private volatile boolean shared = false;
    private final Logger logger = Logger.getLogger(getClass());

    public LocalEnvironment() {
        this.transactionManagerLookupClass = DEFAULT_TRANSACTION_MANAGER_LOOKUP_CLASS;
    }

    public LocalEnvironment( Class<? extends TransactionManagerLookup> transactionManagerLookupClass ) {
        if (transactionManagerLookupClass == null) transactionManagerLookupClass = DEFAULT_TRANSACTION_MANAGER_LOOKUP_CLASS;
        this.transactionManagerLookupClass = transactionManagerLookupClass;
    }

    /**
     * Get the default cache container.
     * 
     * @return the default cache container; never null
     * @throws IOException
     * @throws NamingException
     */
    public CacheContainer getCacheContainer() throws IOException, NamingException {
        return getCacheContainer(null);
    }

    @Override
    public synchronized CacheContainer getCacheContainer( String name ) throws IOException, NamingException {
        if (name == null) name = DEFAULT_CONFIGURATION_NAME;
        CacheContainer container = containers.get(name);
        if (container == null) {
            container = createContainer(name);
            containers.put(name, container);
        }
        return container;
    }

    @Override
    public synchronized Channel getChannel( String name ) {
        return null;
    }

    /**
     * Shutdown this environment, allowing it to reclaim any resources.
     * <p>
     * This method does nothing if the environment has been marked as {@link #isShared() shared}.
     * </p>
     */
    @Override
    public synchronized void shutdown() {
        if (!shared) doShutdown();
    }

    /**
     * Shutdown all containers and caches.
     */
    protected void doShutdown() {
        for (CacheContainer container : containers.values()) {
            shutdown(container);
        }
        containers.clear();
    }

    @Override
    public ClassLoader getClassLoader( ClassLoader fallbackLoader,
                                       String... classpathEntries ) {
        List<String> urls = new ArrayList<String>();
        if (classpathEntries != null) {
            for (String url : classpathEntries) {
                if (!StringUtil.isBlank(url)) {
                    urls.add(url);
                }
            }
        }
        List<ClassLoader> delegatesList = new ArrayList<ClassLoader>();
        if (!urls.isEmpty()) {
            StringURLClassLoader urlClassLoader = new StringURLClassLoader(urls);
            // only if any custom urls were parsed add this loader
            if (urlClassLoader.getURLs().length > 0) {
                delegatesList.add(urlClassLoader);
            }
        }

        ClassLoader currentLoader = getClass().getClassLoader();
        if (fallbackLoader != null && !fallbackLoader.equals(currentLoader)) {
            // if the parent of fallback is the same as the current loader, just use that
            if (fallbackLoader.getParent().equals(currentLoader)) {
                currentLoader = fallbackLoader;
            } else {
                delegatesList.add(fallbackLoader);
            }
        }

        return delegatesList.isEmpty() ? currentLoader : new DelegatingClassLoader(currentLoader, delegatesList);
    }

    protected void shutdown( CacheContainer container ) {
        container.stop();
    }

    protected Class<? extends TransactionManagerLookup> transactionManagerLookupClass() {
        return transactionManagerLookupClass;
    }

    protected TransactionManagerLookup transactionManagerLookupInstance() {
        try {
            return transactionManagerLookupClass().newInstance();
        } catch (Throwable t) {
            throw new RuntimeException(t);
        }
    }

    protected CacheContainer createContainer( String configFile ) throws IOException, NamingException {
        CacheContainer container = null;
        // First try finding the cache configuration ...
        if (configFile != null && !configFile.equals(DEFAULT_CONFIGURATION_NAME)) {
            configFile = configFile.trim();
            try {
                logger.debug("Starting cache manager using configuration at '{0}'", configFile);
                container = new DefaultCacheManager(configFile);
            } catch (FileNotFoundException e) {
                // Configuration file was not found, so try JNDI using configFileName as JNDI name...
                container = (CacheContainer)jndiContext().lookup(configFile);
            }
        }
        if (container == null) {
            // The default Infinispan configuration is in-memory, local and non-clustered.
            // But we need a transaction manager, so use the generic TM which is a good default ...
            Configuration config = createDefaultConfiguration();
            GlobalConfiguration global = createGlobalConfiguration();
            container = createContainer(global, config);
        }
        return container;
    }

    protected Configuration createDefaultConfiguration() {
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.transaction().transactionMode(TransactionMode.TRANSACTIONAL);
        configurationBuilder.transaction().transactionManagerLookup(transactionManagerLookupInstance());
        configurationBuilder.transaction().lockingMode(LockingMode.PESSIMISTIC);
        return configurationBuilder.build();
    }

    protected GlobalConfiguration createGlobalConfiguration() {
        GlobalConfigurationBuilder global = new GlobalConfigurationBuilder();
        // TODO author=Horia Chiorean date=7/26/12 description=MODE-1524 - Currently we don't use advanced externalizers
        // global = global.fluent().serialization().addAdvancedExternalizer(Schematic.externalizers()).build();
        return global.build();
    }

    protected CacheContainer createContainer( GlobalConfiguration globalConfiguration,
                                              Configuration configuration ) {
        logger.debug("Starting cache manager with global configuration \n{0}\nand default configuration:\n{1}",
                     globalConfiguration,
                     configuration);
        return new DefaultCacheManager(globalConfiguration, configuration);
    }

    protected Context jndiContext() throws NamingException {
        return new InitialContext();
    }

    /**
     * Add the supplied {@link CacheContainer} under the supplied name if and only if there is not already a cache container
     * registered at that name.
     * 
     * @param name the cache container name; may be null if the {@link #DEFAULT_CONFIGURATION_NAME default configuration name}
     *        should be used
     * @param cacheContainer the cache container; may not be null
     */
    public void addCacheContainerIfAbsent( String name,
                                           CacheContainer cacheContainer ) {
        CheckArg.isNotNull(cacheContainer, "cacheContainer");
        containers.putIfAbsent(name, cacheContainer);
    }

    /**
     * Add the supplied {@link CacheContainer} under the supplied name if and only if there is not already a cache container
     * registered at that name.
     * 
     * @param name the cache container name; may be null if the {@link #DEFAULT_CONFIGURATION_NAME default configuration name}
     *        should be used
     * @param cacheContainer the cache container; may not be null
     * @return the cache container that was previously registered in this environment by the supplied name, or null if there was
     *         no such previously-registered cache container
     */
    public CacheContainer addCacheContainer( String name,
                                             CacheContainer cacheContainer ) {
        CheckArg.isNotNull(cacheContainer, "cacheContainer");
        return containers.put(name, cacheContainer);
    }

    /**
     * Define within the default cache container an Infinispan cache with the given cache name and configuration. Note that the
     * cache container is created if required, but if it exists it must implement the {@link EmbeddedCacheManager} interface for
     * this method to succeed.
     * 
     * @param cacheName the name of the cache being defined; may not be null
     * @param configuration the cache configuration; may not be null
     * @return the clone of the supplied configuration that is used by the cache container; never null
     */
    public Configuration defineCache( String cacheName,
                                      Configuration configuration ) {
        CheckArg.isNotNull(cacheName, "cacheName");
        CheckArg.isNotNull(configuration, "configuration");
        return defineCache(null, cacheName, configuration);
    }

    /**
     * Define within the named cache container an Infinispan cache with the given cache name and configuration. Note that the
     * cache container is created if required, but if it exists it must implement the {@link EmbeddedCacheManager} interface for
     * this method to succeed.
     * 
     * @param cacheContainerName the name of the cache container; if null, the {@link #DEFAULT_CONFIGURATION_NAME default
     *        container name} is used
     * @param cacheName the name of the cache being defined; may not be null
     * @param configuration the cache configuration; may not be null
     * @return the clone of the supplied configuration that is used by the cache container; never null
     */
    public Configuration defineCache( String cacheContainerName,
                                      String cacheName,
                                      Configuration configuration ) {
        CheckArg.isNotNull(cacheName, "cacheName");
        CheckArg.isNotNull(configuration, "configuration");
        if (cacheContainerName == null) cacheContainerName = DEFAULT_CONFIGURATION_NAME;
        CacheContainer container = containers.get(cacheContainerName);
        if (container == null) {
            Configuration config = createDefaultConfiguration();
            GlobalConfiguration global = createGlobalConfiguration();
            CacheContainer newContainer = createContainer(global, config);
            container = containers.putIfAbsent(cacheContainerName, newContainer);
            if (container == null) container = newContainer;
        }
        return ((EmbeddedCacheManager)container).defineConfiguration(cacheName, configuration);
    }

    /**
     * Set whether this environment is shared amongst multiple repositories. Shared environments are not shutdown automatically,
     * and the application is expected to shutdown all containers and caches. By default, environments are not shared unless this
     * method is explicitly called with a parameter value of <code>true</code>.
     * 
     * @param shared true if this environment is shared, or false otherwise
     * @see #isShared()
     */
    public void setShared( boolean shared ) {
        this.shared = shared;
    }

    /**
     * Return whether this environment is shared amongst multiple repositories.
     * 
     * @return true if this environment is shared, or false otherwise
     * @see #setShared(boolean)
     */
    public boolean isShared() {
        return shared;
    }

}
