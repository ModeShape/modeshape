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
package org.modeshape.test.performance;

import java.io.File;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.loaders.jdbm.configuration.JdbmCacheStoreConfigurationBuilder;
import org.modeshape.common.util.FileUtil;

public class JdbmCacheStorePerformanceTest extends InMemoryPerformanceTest {

    private final File dbDir = new File("target/database");

    @Override
    protected void cleanUpFileSystem() {
        FileUtil.delete(dbDir);
    }

    @Override
    public void applyLoaderConfiguration( ConfigurationBuilder configurationBuilder ) {
        JdbmCacheStoreConfigurationBuilder builder = new JdbmCacheStoreConfigurationBuilder(configurationBuilder.loaders());
        builder.location(dbDir.getAbsolutePath());
        builder.purgeOnStartup(true);
        configurationBuilder.loaders().addStore(builder);
    }
}
