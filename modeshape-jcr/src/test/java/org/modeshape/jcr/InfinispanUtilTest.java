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

import static org.junit.Assert.fail;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import org.infinispan.Cache;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.modeshape.common.util.FileUtil;
import org.modeshape.jcr.value.binary.infinispan.InfinispanTestUtil;

public class InfinispanUtilTest {

    private static DefaultCacheManager cacheManager;

    private static Configuration LOCAL;
    private static Configuration LOCAL_STORE_SHARED;
    private static Configuration DIST;
    private static Configuration DIST_STORE_SHARED;
    private static Configuration DIST_STORE_UNSHARED;

    @BeforeClass
    public static void beforeClass() throws Exception {
        cacheManager = InfinispanTestUtil.beforeClassStartup(true);

        ConfigurationBuilder configurationBuilder = new ConfigurationBuilder();
        configurationBuilder.clustering().cacheMode(CacheMode.LOCAL);
        LOCAL = configurationBuilder.build();
        configurationBuilder.clustering().cacheMode(CacheMode.DIST_SYNC);
        DIST = configurationBuilder.build();

        // store config
        File dir = new File(System.getProperty("java.io.tmpdir"), "InfinispanLocalBinaryStoreWithPersistenceTest");
        if (dir.exists()) FileUtil.delete(dir);
        dir.mkdirs();
        configurationBuilder.persistence().addSingleFileStore().shared(true).purgeOnStartup(true).location(dir.getAbsolutePath());
        configurationBuilder.clustering().cacheMode(CacheMode.LOCAL);
        LOCAL_STORE_SHARED = configurationBuilder.build();

        configurationBuilder.clustering().cacheMode(CacheMode.DIST_SYNC);
        DIST_STORE_SHARED = configurationBuilder.build();

        configurationBuilder.persistence().clearStores().addSingleFileStore().shared(false).purgeOnStartup(true).location(dir.getAbsolutePath());
        DIST_STORE_UNSHARED = configurationBuilder.build();

        // define caches
        cacheManager.defineConfiguration("LOCAL", LOCAL);
        cacheManager.defineConfiguration("LOCAL_STORE", LOCAL_STORE_SHARED);
        cacheManager.defineConfiguration("DIST", DIST);
        cacheManager.defineConfiguration("DIST_STORE", DIST_STORE_SHARED);
        cacheManager.defineConfiguration("DIST_STORE_UNSHARED", DIST_STORE_UNSHARED);
        cacheManager.start();
    }

    @AfterClass
    public static void afterClass() {
        InfinispanTestUtil.afterClassShutdown(cacheManager);
    }

    private void checkSequence( InfinispanUtil.Sequence<String> sequence,
                                String... expected ) throws Exception {
        int index = 0;
        List<String> keys = new ArrayList<String>(expected.length);
        while (true) {
            String key = sequence.next();
            if (key == null) {
                break;
            }
            keys.add(key);
        }

        if (keys.size() != expected.length) {
            fail("Sequence contains wrong elements. Expected: " + expected.length + " Contained: " + index);
        }
        for (String key : expected) {
            if (!keys.contains(key)) {
                fail("Missing key in sequence: " + key);
            }
        }
    }

    private void standardTest( String cacheName ) throws Exception {
        Cache<String, String> cache = cacheManager.getCache(cacheName);
        cache.put("Foo", "Bar");

        checkSequence(InfinispanUtil.getAllKeys(cache), "Foo");
    }

    @Test
    public void testLocal() throws Exception {
        standardTest("LOCAL");
    }

    @Test
    public void testLocalStore() throws Exception {
        standardTest("LOCAL_STORE");
    }

    @Test
    public void testDist() throws Exception {
        standardTest("DIST");
    }

    @Test
    public void testDistStore() throws Exception {
        standardTest("DIST_STORE");
    }

    @Test
    public void testDistStoreUnshared() throws Exception {
        standardTest("DIST_STORE_UNSHARED");
    }
}
