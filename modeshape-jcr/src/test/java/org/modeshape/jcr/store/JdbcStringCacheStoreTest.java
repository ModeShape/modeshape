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
import org.infinispan.persistence.jdbc.configuration.JdbcStringBasedStoreConfigurationBuilder;
import org.junit.Ignore;

/**
 * Currently ignored because of the time required to run it.
 */
@Ignore
public class JdbcStringCacheStoreTest extends InMemoryTest {

    private final DataSourceConfig dataSourceConfig = new DataSourceConfig();

    @Override
    protected void cleanUpFileSystem() throws Exception {
        // The database has a 1 second close delay, so sleep for a bit more than 1 second to allow it to close ...
        Thread.sleep(1200);
    }

    @Override
    public void applyLoaderConfiguration( ConfigurationBuilder configurationBuilder ) {
        JdbcStringBasedStoreConfigurationBuilder builder = new JdbcStringBasedStoreConfigurationBuilder(configurationBuilder.persistence());
        builder.purgeOnStartup(true);
        builder.table()
               .createOnStart(true)
               .dropOnExit(true)
               .idColumnName("ID_COLUMN")
               .idColumnType("VARCHAR(255)")
               .timestampColumnName("TIMESTAMP_COLUMN")
               .timestampColumnType("BIGINT")
               .dataColumnName("DATA_COLUMN")
               .dataColumnType("BINARY")
               .connectionPool()
               .connectionUrl(dataSourceConfig.getUrl() + "/string_based_db;DB_CLOSE_DELAY=1")
               .driverClass(dataSourceConfig.getDriverClassName())
               .username(dataSourceConfig.getUsername());
        configurationBuilder.persistence().addStore(builder);
    }

}
