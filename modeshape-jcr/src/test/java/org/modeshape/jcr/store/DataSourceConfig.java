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

import java.io.IOException;
import java.util.Properties;
import org.modeshape.common.util.StringUtil;

/**
 * Test helper class which loads a property file and provides DB-specific properties, triggered by Maven profiles.
 *
 * @author Horia Chiorean
 */
public class DataSourceConfig {

    private final Properties dsProperties;

    public DataSourceConfig() {
        this("config/db/datasource.properties");
    }

    public DataSourceConfig( String configFilePath ) {
        this.dsProperties = loadPropertiesFile(configFilePath);
    }

    private Properties loadPropertiesFile( String configFilePath ) {
        try {
            Properties properties = new Properties();
            properties.load(getClass().getClassLoader().getResourceAsStream(configFilePath));
            return properties;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public String getDatabase() {
        return dsProperties.getProperty("database");
    }

    public String getUrl() {
        return dsProperties.getProperty("url");
    }

    public String getUsername() {
        return dsProperties.getProperty("username");
    }

    public String getPassword() {
        return dsProperties.getProperty("password");
    }

    public String getDriverClassName() {
        return dsProperties.getProperty("driverClassName");
    }

    public Integer getMaximumConnectionsInPool() {
        return asInteger("maximumConnectionsInPool");
    }

    public Integer getMinimumConnectionsInPool() {
        return asInteger("minimumConnectionsInPool");
    }

    public Integer getMaximumSizeOfStatementsCache() {
        return asInteger("maximumSizeOfStatementCache");
    }

    public Integer getMaximumConnectionIdleTimeInSeconds() {
        return asInteger("maximumConnectionIdleTimeInSeconds");
    }

    public Integer getNumberOfConnectionsToAcquireAsNeeded() {
        return asInteger("numberOfConnectionsToAcquireAsNeeded");
    }

    public Integer getRetryLimit() {
        return asInteger("retryLimit");
    }

    private Integer asInteger(String propertyName) {
        String value = dsProperties.getProperty(propertyName);
        if (!StringUtil.isBlank(value)) {
            return Integer.valueOf(value);
        }
        return null;
    }
}
