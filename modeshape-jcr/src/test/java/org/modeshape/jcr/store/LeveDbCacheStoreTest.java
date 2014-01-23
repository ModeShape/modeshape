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
package org.modeshape.jcr.store;

import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.persistence.leveldb.configuration.LevelDBStoreConfiguration;
import org.infinispan.persistence.leveldb.configuration.LevelDBStoreConfigurationBuilder;
import org.modeshape.common.util.FileUtil;

public class LeveDbCacheStoreTest extends InMemoryTest {

    @Override
    protected void cleanUpFileSystem() {
        FileUtil.delete("target/leveldb");
        FileUtil.delete("target/leveldbdocuments");
    }

    @Override
    public void applyLoaderConfiguration( ConfigurationBuilder configurationBuilder ) {
        configurationBuilder.persistence()
                            .addStore(LevelDBStoreConfigurationBuilder.class)
                            .location("target/leveldb/content")
                            .expiredLocation("target/leveldb/expired")
                            .implementationType(LevelDBStoreConfiguration.ImplementationType.JAVA)
                            .purgeOnStartup(true);
    }
}
