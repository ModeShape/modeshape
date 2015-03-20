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
package org.infinispan.schematic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;
import javax.transaction.TransactionManager;
import org.infinispan.AdvancedCache;
import org.infinispan.Cache;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.CacheContainer;
import org.infinispan.manager.EmbeddedCacheManager;

public class TestUtil {

    public static boolean delete( File fileOrDirectory ) {
        if (fileOrDirectory == null) { return false; }
        if (!fileOrDirectory.exists()) { return false; }

        // The file/directory exists, so if a directory delete all of the contents ...
        if (fileOrDirectory.isDirectory()) {
            for (File childFile : fileOrDirectory.listFiles()) {
                delete(childFile); // recursive call (good enough for now until we need something better)
            }
            // Now an empty directory ...
        }
        // Whether this is a file or empty directory, just delete it ...
        return fileOrDirectory.delete();
    }

    public static InputStream resource( String resourcePath ) {
        InputStream stream = TestUtil.class.getClassLoader().getResourceAsStream(resourcePath);
        if (stream == null) {
            File file = new File(resourcePath);
            if (!file.exists()) {
                file = new File("src/test/resources" + resourcePath);
            }
            if (!file.exists()) {
                file = new File("src/test/resources/" + resourcePath);
            }
            if (file.exists()) {
                try {
                    stream = new FileInputStream(file);
                } catch (IOException e) {
                    throw new AssertionError("Failed to open stream to \"" + file.getAbsolutePath() + "\"");
                }
            }
        }
        assert stream != null : "Resource at \"" + resourcePath + "\" could not be found";
        return stream;
    }

    /**
     * Clears transaction with the current thread in the given transaction manager.
     *
     * @param txManager a TransactionManager to be cleared
     */
    public static void killTransaction( TransactionManager txManager ) {
        if (txManager != null) {
            try {
                txManager.rollback();
            } catch (Exception e) {
                // ignore
            }
        }
    }

    /**
     * Stops a collection of ISPN caches and stops any running transactions.
     *
     * @param caches a collection of caches
     */
    public static void killCaches( Iterable<Cache<?, ?>> caches ) {
        for (Cache<?, ?> c : caches) {
            killCache(c);
        }
    }

    /**
     * Stops an ISPN cache
     *
     * @param c the cache to stop
     */
    public static void killCache( Cache<?, ?> c ) {
        try {
            if (c != null && c.getStatus() == ComponentStatus.RUNNING) {
                AdvancedCache advancedCache = c.getAdvancedCache();
                if (advancedCache != null) {
                    TransactionManager tm = advancedCache.getTransactionManager();
                    if (tm != null) {
                        try {
                            tm.rollback();
                        } catch (Exception e) {
                            // don't care
                        }
                    }
                }
                c.stop();
            }
        } catch (Throwable t) {
            t.printStackTrace(System.err);
        }
    }

    /**
     * Stops an array of cache managers.
     *
     * @param containers an array of {@link EmbeddedCacheManager}
     */
    public static void killCacheContainers( CacheContainer... containers ) {
        for (CacheContainer container : containers) {
            if (!(container instanceof EmbeddedCacheManager) || ((EmbeddedCacheManager)container)
                                                                        .getStatus() != ComponentStatus.RUNNING) {
                continue;
            }
            EmbeddedCacheManager manager = (EmbeddedCacheManager)container;
            Set<Cache<?, ?>> caches = new HashSet<>();
            for (String cacheName : manager.getCacheNames()) {
                Cache<Object, Object> cache = manager.getCache(cacheName, false);
                AdvancedCache<Object, Object> advancedCache = cache.getAdvancedCache();
                caches.add(advancedCache);
            }
            killCaches(caches);
        }
        for (CacheContainer cm : containers) {
            try {
                if (cm != null) { cm.stop(); }
            } catch (Throwable e) {
                e.printStackTrace(System.err);
            }
        }
    }
}