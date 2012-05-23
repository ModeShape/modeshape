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
package org.modeshape.jcr.store;

import org.infinispan.loaders.CacheLoaderConfig;
import org.infinispan.loaders.jdbc.stringbased.JdbcStringBasedCacheStoreConfig;
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
    protected CacheLoaderConfig getCacheLoaderConfiguration() {
        JdbcStringBasedCacheStoreConfig config = new JdbcStringBasedCacheStoreConfig();
        config.setConnectionFactoryClass("org.infinispan.loaders.jdbc.connectionfactory.PooledConnectionFactory");
        // config.setConnectionUrl("jdbc:h2:mem:string_based_db;DB_CLOSE_DELAY=-1");
        config.setConnectionUrl(dataSourceConfig.getUrl() + "/string_based_db;DB_CLOSE_DELAY=1");
        config.setIdColumnName("ID_COLUMN");
        config.setDataColumnName("DATA_COLUMN");
        config.setTimestampColumnName("TIMESTAMP_COLUMN");
        config.setStringsTableNamePrefix("ISPN_STRING_TABLE");
        config.setUserName(dataSourceConfig.getUsername());
        config.setDriverClass(dataSourceConfig.getDriverClassName());
        config.setIdColumnType("VARCHAR(255)");
        config.setDataColumnType("BINARY");
        config.setTimestampColumnType("BIGINT");
        config.setDropTableOnExit(true);
        config.setCreateTableOnStart(true);
        return config;
    }
}
