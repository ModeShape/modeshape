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

import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.modeshape.jcr.ClusteringHelper;

public class InfinispanTestUtil {

    /**
     * @param networked true if the Infinispan cache is clustered, or false otherwise
     * @return created and started CacheManager
     * @throws Exception if there is a problem starting the cache
     */
    public static DefaultCacheManager beforeClassStartup( boolean networked ) throws Exception {
        if (networked) {
            ClusteringHelper.bindJGroupsToLocalAddress();
        }
        GlobalConfigurationBuilder globalConfigurationBuilder = new GlobalConfigurationBuilder();
        globalConfigurationBuilder.globalJmxStatistics().disable().allowDuplicateDomains(true);
        if (networked) {
            globalConfigurationBuilder.transport().defaultTransport().addProperty("configurationFile", "config/jgroups-test-config.xml");
        } else {
            globalConfigurationBuilder.transport().transport(null);
        }
        return new DefaultCacheManager(globalConfigurationBuilder.build(), true);
    }

    public static void afterClassShutdown( DefaultCacheManager cacheManager ) {
        if (cacheManager != null) {
            if (cacheManager.getCacheManagerConfiguration().transport() != null) {
                ClusteringHelper.removeJGroupsBindings();
            }
            cacheManager.stop();
        }
    }
}
