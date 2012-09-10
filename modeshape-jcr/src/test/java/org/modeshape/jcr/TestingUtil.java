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

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.jcr.Repository;
import javax.transaction.TransactionManager;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.ModeShapeEngine.State;
import org.modeshape.jcr.value.binary.TransientBinaryStore;

/**
 * 
 */
public class TestingUtil {

    private static final Logger log = Logger.getLogger(TestingUtil.class);

    public static void killTransientBinaryStore() {
        File directory = TransientBinaryStore.get().getDirectory();
        FileUtil.delete(directory);
    }

    public static void killRepositories( Repository... repositories ) {
        killRepositories(Arrays.asList(repositories));
    }

    public static void killRepositories( Iterable<Repository> repositories ) {
        for (Repository repository : repositories) {
            if (repository instanceof JcrRepository) {
                killRepository((JcrRepository)repository);
            }
        }
    }

    public static void killRepositoryAndContainer( JcrRepository repository ) {
        Collection<CacheContainer> containers = killRepository(repository);
        // Now kill all the cache managers ...
        for (CacheContainer container : containers) {
            org.infinispan.test.TestingUtil.killCacheManagers(container);
        }
    }

    public static Collection<CacheContainer> killRepository( JcrRepository repository ) {
        if (repository == null) return Collections.emptySet();
        try {
            if (repository.getState() != State.RUNNING) return Collections.emptySet();
            // Rollback any open transactions ...
            TransactionManager txnMgr = repository.runningState().txnManager();
            if (txnMgr != null && txnMgr.getTransaction() != null) {
                try {
                    txnMgr.rollback();
                } catch (Throwable t) {
                   log.warn(t, JcrI18n.errorKillingRepository, repository.getName(), t.getMessage());
                }
            }

            // Then get the caches (which we'll kill after we shutdown the repository) ...
            Collection<Cache<?, ?>> caches = repository.caches();

            // First shut down the repository ...
            repository.shutdown().get();

            // Get the caches and kill them ...
            Set<CacheContainer> cacheContainers = new HashSet<CacheContainer>();
            for (Cache<?, ?> cache : caches) {
                if (cache != null) {
                    cacheContainers.add(cache.getCacheManager());
                    org.infinispan.test.TestingUtil.killCaches(cache);
                }
            }

            return cacheContainers;
        } catch (Throwable t) {
            log.error(t, JcrI18n.errorKillingRepository, repository.getName(), t.getMessage());
        }
        return Collections.emptySet();
    }

    public static void killEngine( ModeShapeEngine engine ) {
        if (engine == null) return;
        try {
            if (engine.getState() != State.RUNNING) return;

            // First shutdown and destroy the repositories ...
            Set<CacheContainer> cacheContainers = new HashSet<CacheContainer>();

            for (String key : engine.getRepositoryKeys()) {
                JcrRepository repository = engine.getRepository(key);
                cacheContainers.addAll(killRepository(repository));
            }
            // Then shutdown the engine ...
            engine.shutdown().get(20, TimeUnit.SECONDS);

            // Now kill all the cache managers ...
            for (CacheContainer container : cacheContainers) {
                org.infinispan.test.TestingUtil.killCacheManagers(container);
            }

        } catch (Throwable t) {
            log.error(t, JcrI18n.errorKillingEngine, t.getMessage());
        }
    }

    public static JcrRepository startRepositoryWithConfig(String configFile) throws Exception {
        URL configUrl = TestingUtil.class.getClassLoader().getResource(configFile);
        RepositoryConfiguration config = RepositoryConfiguration.read(configUrl);
        JcrRepository repository = null;
        repository = new JcrRepository(config);
        repository.start();
        return repository;
    }
}
