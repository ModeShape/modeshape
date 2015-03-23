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

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.jcr.Repository;
import org.infinispan.Cache;
import org.infinispan.manager.CacheContainer;
import org.infinispan.schematic.TestUtil;
import org.modeshape.common.SystemFailureException;
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
        boolean killedAtLeastOne = false;
        for (Repository repository : repositories) {
            if (repository instanceof JcrRepository) {
                if (killedAtLeastOne) {
                    // We need to wait for a few seconds before we shut down the other repositories, since they're
                    // probably clustered, and the second one needs to be updated after the first one shuts down ...
                    try {
                        Thread.sleep(1000L);
                    } catch (Exception e) {
                        throw new SystemFailureException(e);
                    }
                }
                killRepository((JcrRepository)repository);
                killedAtLeastOne = true;
            }
        }
    }

    public static void killRepositoryAndContainer( JcrRepository repository ) {
        Collection<CacheContainer> containers = killRepository(repository);
        // Now kill all the cache managers ...
        for (CacheContainer container : containers) {
            TestUtil.killCacheContainers(container);
        }
    }

    public static Collection<CacheContainer> killRepository( JcrRepository repository ) {
        if (repository == null) return Collections.emptySet();
        try {
            if (repository.getState() != State.RUNNING) return Collections.emptySet();
            // Rollback any open transactions ...
            TestUtil.killTransaction(repository.runningState().txnManager());

            // Then get the caches (which we'll kill after we shutdown the repository) ...
            Collection<Cache<?, ?>> caches = repository.caches();

            // First shut down the repository ...
            repository.shutdown().get();

            // Get the caches and kill them ...
            Set<CacheContainer> cacheContainers = new HashSet<CacheContainer>();
            for (Cache<?, ?> cache : caches) {
                if (cache != null) {
                    cacheContainers.add(cache.getCacheManager());
                    TestUtil.killCache(cache);
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
                TestUtil.killCacheContainers(container);
            }

        } catch (Throwable t) {
            log.error(t, JcrI18n.errorKillingEngine, t.getMessage());
        }
    }

    public static JcrRepository startRepositoryWithConfig( String configFile ) throws Exception {
        URL configUrl = TestingUtil.class.getClassLoader().getResource(configFile);
        RepositoryConfiguration config = RepositoryConfiguration.read(configUrl);
        JcrRepository repository = null;
        repository = new JcrRepository(config);
        repository.start();
        return repository;
    }
}
