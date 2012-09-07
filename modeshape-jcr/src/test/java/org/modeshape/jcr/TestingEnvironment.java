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

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.LegacyConfigurationAdaptor;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.global.LegacyGlobalConfigurationAdaptor;
import org.infinispan.manager.CacheContainer;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
import org.infinispan.transaction.lookup.TransactionManagerLookup;
import org.modeshape.jcr.store.InMemoryTest;

/**
 * An {@link Environment} implementation that can be used for testing.
 */
public class TestingEnvironment extends LocalEnvironment {

    private final InMemoryTest inMemoryTest;

    public TestingEnvironment() {
        this(null, DummyTransactionManagerLookup.class);
    }

    public TestingEnvironment(Class<? extends TransactionManagerLookup> transactionManagerLookup) {
        this(null, transactionManagerLookup);
    }

    public TestingEnvironment(InMemoryTest inMemoryTest) {
        this(inMemoryTest, DummyTransactionManagerLookup.class);
    }

    public TestingEnvironment(InMemoryTest inMemoryTest, Class<? extends TransactionManagerLookup> transactionManagerLookup) {
        super(transactionManagerLookup);
        this.inMemoryTest = inMemoryTest;
    }

    @Override
    protected void shutdown( CacheContainer container ) {
        TestingUtil.killCacheManagers(container);
    }

    @Override
    protected Configuration createDefaultConfiguration() {
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.transaction().transactionMode(TransactionMode.TRANSACTIONAL);
        configurationBuilder.transaction().transactionManagerLookup(transactionManagerLookupInstance());
        if(inMemoryTest != null){
            inMemoryTest.applyLoaderConfiguration(configurationBuilder);
        }
        return configurationBuilder.build();
    }

    @Override
    protected CacheContainer createContainer( GlobalConfiguration globalConfiguration,
                                              Configuration configuration ) {
        return TestCacheManagerFactory.createCacheManager(LegacyGlobalConfigurationAdaptor.adapt(globalConfiguration),
                LegacyConfigurationAdaptor.adapt(configuration));
    }
}
