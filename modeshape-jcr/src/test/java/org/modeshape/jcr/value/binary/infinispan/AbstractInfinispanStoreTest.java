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

import org.infinispan.configuration.cache.Configuration;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.AfterClass;
import org.modeshape.jcr.value.binary.AbstractBinaryStoreTest;
import org.modeshape.jcr.value.binary.BinaryStore;

public abstract class AbstractInfinispanStoreTest extends AbstractBinaryStoreTest {

    protected static DefaultCacheManager cacheManager;
    private static final String BLOB = "blob";
    private static final String METADATA = "metadata";

    protected static InfinispanBinaryStore binaryStore;

    @AfterClass
    public static void afterClass() {
        // First stop the caches ...
        stopAdditionalCaches();
        InfinispanTestUtil.afterClassShutdown(cacheManager);
        // Then the store ...
        binaryStore.shutdown();
    }

    private static void stopAdditionalCaches() {
        cacheManager.getCache(METADATA).stop();
        cacheManager.removeCache(METADATA);
        cacheManager.getCache(BLOB).stop();
        cacheManager.removeCache(BLOB);
    }

    protected static void startBinaryStore( Configuration metadataConfiguration,
                                            Configuration blobConfiguration ) {
        cacheManager.defineConfiguration(METADATA, metadataConfiguration);
        cacheManager.startCache(METADATA);

        cacheManager.defineConfiguration(BLOB, blobConfiguration);
        cacheManager.startCache(BLOB);

        binaryStore = new InfinispanBinaryStore(cacheManager, true, METADATA, BLOB);
        binaryStore.setMimeTypeDetector(DEFAULT_DETECTOR);
        binaryStore.start();
    }

    @Override
    protected BinaryStore getBinaryStore() {
        return binaryStore;
    }

}
