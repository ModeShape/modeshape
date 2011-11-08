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

import java.util.concurrent.TimeUnit;
import org.infinispan.config.Configuration;
import org.infinispan.manager.CacheContainer;
import org.infinispan.test.TestingUtil;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.transaction.lookup.DummyTransactionManagerLookup;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * A base class for tests that require a single shared JcrSession and JcrRepository for all test methods.
 */
public abstract class MultiUseAbstractTest {

    private static final String REPO_NAME = "testRepo";

    private static CacheContainer cm;
    private static RepositoryConfiguration config;
    protected static JcrRepository repository;
    protected static JcrSession session;

    @BeforeClass
    public static void beforeAll() throws Exception {
        Configuration c = new Configuration();
        c = c.fluent().transaction().transactionManagerLookup(new DummyTransactionManagerLookup()).build();
        cm = TestCacheManagerFactory.createCacheManager(c);

        // Configuration c = new Configuration();
        // cm = TestCacheManagerFactory.createCacheManager(c, true);
        config = new RepositoryConfiguration(REPO_NAME, cm);
        repository = new JcrRepository(config);
        repository.start();
        session = repository.login();
    }

    @AfterClass
    public static void afterAll() throws Exception {
        try {
            repository.shutdown().get(3L, TimeUnit.SECONDS);
        } finally {
            repository = null;
            config = null;
            try {
                TestingUtil.killCacheManagers(cm);
            } finally {
                cm = null;
            }
        }
    }

    public JcrRepository repository() {
        return repository;
    }

    public JcrSession session() {
        return session;
    }
}
