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

import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import org.infinispan.config.Configuration;
import org.infinispan.config.FluentConfiguration;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.schematic.Schematic;
import org.infinispan.transaction.lookup.GenericTransactionManagerLookup;
import org.infinispan.transaction.lookup.TransactionManagerLookup;
import org.jgroups.Channel;
import org.modeshape.common.util.DelegatingClassLoader;
import org.modeshape.common.util.StringURLClassLoader;
import org.modeshape.common.util.StringUtil;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 
 */
@SuppressWarnings( "deprecation" )
public class LocalEnvironment implements Environment {

    public static final Class<? extends TransactionManagerLookup> DEFAULT_TRANSACTION_MANAGER_LOOKUP_CLASS = GenericTransactionManagerLookup.class;

    private static final String DEFAULT_CONFIGURATION_NAME = "defaultCacheContainer";
    private final Class<? extends TransactionManagerLookup> transactionManagerLookupClass;
    private final Map<String, CacheContainer> containers = new HashMap<String, CacheContainer>();

    public LocalEnvironment() {
        this.transactionManagerLookupClass = DEFAULT_TRANSACTION_MANAGER_LOOKUP_CLASS;
    }

    public LocalEnvironment( Class<? extends TransactionManagerLookup> transactionManagerLookupClass ) {
        if (transactionManagerLookupClass == null) transactionManagerLookupClass = DEFAULT_TRANSACTION_MANAGER_LOOKUP_CLASS;
        this.transactionManagerLookupClass = transactionManagerLookupClass;
    }

    public synchronized CacheContainer getCacheContainer() throws IOException, NamingException {
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

    @Override
    public synchronized void shutdown() {
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
            //only if any custom urls were parsed add this loader
            if (urlClassLoader.getURLs().length > 0) {
                delegatesList.add(urlClassLoader);
            }
        }

        ClassLoader currentLoader = getClass().getClassLoader();
        if (fallbackLoader != null && !fallbackLoader.equals(currentLoader)) {
            //if the parent of fallback is the same as the current loader, just use that
            if (fallbackLoader.getParent().equals(currentLoader)) {
                currentLoader = fallbackLoader;
            }
            else {
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
                container = new DefaultCacheManager(configFile);
            } catch (FileNotFoundException e) {
                // Configuration file was not found, so try JNDI ...
                String jndiName = configFile;
                container = (CacheContainer)jndiContext().lookup(jndiName);
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
        FluentConfiguration configurator = new FluentConfiguration(new Configuration());
        configurator.transaction().transactionManagerLookupClass(transactionManagerLookupClass());
        return configurator.build();
    }

    protected GlobalConfiguration createGlobalConfiguration() {
        GlobalConfiguration global = new GlobalConfiguration();
        //TODO author=Horia Chiorean date=7/26/12 description=MODE-1524 - Currently we don't use advanced externalizers
        //global = global.fluent().serialization().addAdvancedExternalizer(Schematic.externalizers()).build();
        return global;
    }

    protected CacheContainer createContainer( GlobalConfiguration globalConfiguration,
                                              Configuration configuration ) {
        return new DefaultCacheManager(globalConfiguration, configuration);
    }

    protected Context jndiContext() throws NamingException {
        return new InitialContext();
    }

}
