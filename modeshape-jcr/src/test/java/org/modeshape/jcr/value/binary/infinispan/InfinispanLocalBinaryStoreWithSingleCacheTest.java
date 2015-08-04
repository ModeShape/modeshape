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
package org.modeshape.jcr.value.binary.infinispan;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;

public class InfinispanLocalBinaryStoreWithSingleCacheTest extends AbstractInfinispanStoreTest {

    private static final String CACHE_NAME = "singleCache";

    @AfterClass
    public static void afterClass() {
        cacheManager.getCache(CACHE_NAME).stop();
        cacheManager.removeCache(CACHE_NAME);
        InfinispanTestUtil.afterClassShutdown(cacheManager);
        binaryStore.shutdown();
    }

    protected static void startBinaryStore( Configuration metadataConfiguration,
                                            Configuration blobConfiguration ) {
        cacheManager.defineConfiguration(CACHE_NAME, metadataConfiguration);
        cacheManager.startCache(CACHE_NAME);

        binaryStore = new InfinispanBinaryStore(cacheManager, true, CACHE_NAME, CACHE_NAME);         
        binaryStore.setMimeTypeDetector(DEFAULT_DETECTOR);
        binaryStore.start();
    }

    @BeforeClass
    public static void beforeClass() throws Exception {
        cacheManager = InfinispanTestUtil.beforeClassStartup(false);

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.clustering().cacheMode(CacheMode.LOCAL);
        Configuration blobConfiguration = configurationBuilder.build();

        Configuration metadataConfiguration = configurationBuilder.build();

        startBinaryStore(metadataConfiguration, blobConfiguration);
    }
}
