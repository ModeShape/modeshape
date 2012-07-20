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

import org.infinispan.config.Configuration;
import org.infinispan.config.GlobalConfiguration;
import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.manager.CacheContainer;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
import org.infinispan.transaction.lookup.TransactionManagerLookup;

/**
 * An {@link Environment} implementation that can be used for testing.
 */
@SuppressWarnings( "deprecation" )
public class TestingEnvironment extends LocalEnvironment {

    private final CacheLoaderConfig cacheLoaderConfiguration;

    public TestingEnvironment() {
        this(null, DummyTransactionManagerLookup.class);
    }

    public TestingEnvironment(Class<? extends TransactionManagerLookup> transactionManagerLookup) {
        this(null, transactionManagerLookup);
    }

    public TestingEnvironment( CacheLoaderConfig cacheLoaderConfiguration ) {
        this(cacheLoaderConfiguration, DummyTransactionManagerLookup.class);
    }

    public TestingEnvironment(CacheLoaderConfig cacheLoaderConfiguration, Class<? extends TransactionManagerLookup> transactionManagerLookup) {
        super(transactionManagerLookup);
        this.cacheLoaderConfiguration = cacheLoaderConfiguration;
    }

    @Override
    protected void shutdown( CacheContainer container ) {
        TestingUtil.killCacheManagers(container);
    }

    @Override
    protected Configuration createDefaultConfiguration() {
        Configuration c = new Configuration();
        if (cacheLoaderConfiguration != null) {
            c = c.fluent().loaders().addCacheLoader(cacheLoaderConfiguration).build();
        }
        TransactionManagerLookup txnMgrLookup = transactionManagerLookupInstance();
        c = c.fluent().transaction().transactionManagerLookup(txnMgrLookup).build();
        return c;
    }

    @Override
    protected CacheContainer createContainer( GlobalConfiguration globalConfiguration,
                                              Configuration configuration ) {
        return TestCacheManagerFactory.createCacheManager(globalConfiguration, configuration);
    }
}
