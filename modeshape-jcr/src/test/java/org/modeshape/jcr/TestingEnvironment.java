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

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.schematic.TestUtil;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
import org.infinispan.transaction.lookup.TransactionManagerLookup;

/**
 * An {@link Environment} implementation that can be used for testing.
 */
public class TestingEnvironment extends LocalEnvironment {

    private final CustomLoaderTest customLoaderTest;

    public TestingEnvironment() {
        this(null, DummyTransactionManagerLookup.class);
    }

    public TestingEnvironment( CustomLoaderTest customLoaderTest ) {
        this(customLoaderTest, DummyTransactionManagerLookup.class);
    }

    public TestingEnvironment( CustomLoaderTest customLoaderTest,
                               Class<? extends TransactionManagerLookup> transactionManagerLookup ) {
        super(transactionManagerLookup);
        this.customLoaderTest = customLoaderTest;
    }

    @Override
    protected void shutdown( CacheContainer container ) {
        TestUtil.killCacheContainers(container);
    }

    @Override
    protected Configuration createDefaultConfiguration() {
        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.transaction().transactionMode(TransactionMode.TRANSACTIONAL);
        configurationBuilder.transaction().transactionManagerLookup(transactionManagerLookupInstance());
        if (customLoaderTest != null) {
            customLoaderTest.applyLoaderConfiguration(configurationBuilder);
        }
        return configurationBuilder.build();
    }

    @Override
    protected CacheContainer createContainer( GlobalConfigurationBuilder globalConfigurationBuilder,
                                              ConfigurationBuilder configurationBuilder ) {
        configurationBuilder.jmxStatistics().disable();
        globalConfigurationBuilder.globalJmxStatistics().disable().allowDuplicateDomains(true);
        return new DefaultCacheManager(globalConfigurationBuilder.build(), configurationBuilder.build(), true);
    }
}
