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
package org.modeshape.connector.store.jpa;

import java.io.IOException;
import java.util.Properties;

public class TestEnvironment {

    public static JpaSource configureJpaSource( String sourceName,
                                                Object testCase ) {
        Properties properties = new Properties();
        ClassLoader loader = testCase instanceof Class<?> ? ((Class<?>)testCase).getClassLoader() : testCase.getClass()
                                                                                                            .getClassLoader();
        try {
            properties.load(loader.getResourceAsStream("database.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // Set the connection properties to be an in-memory HSQL database ...
        JpaSource source = new JpaSource();
        source.setName(sourceName);
        source.setDialect(properties.getProperty("jpaSource.dialect"));
        source.setDriverClassName(properties.getProperty("jpaSource.driverClassName"));
        source.setUsername(properties.getProperty("jpaSource.username"));
        source.setPassword(properties.getProperty("jpaSource.password"));
        source.setUrl(properties.getProperty("jpaSource.url"));

        String value = properties.getProperty("jpaSource.maximumConnectionsInPool");
        if (isValue(value)) source.setMaximumConnectionsInPool(Integer.parseInt(value));

        value = properties.getProperty("jpaSource.minimumConnectionsInPool");
        if (isValue(value)) source.setMinimumConnectionsInPool(Integer.parseInt(value));

        value = properties.getProperty("jpaSource.maximumSizeOfStatementCache");
        if (isValue(value)) source.setMaximumSizeOfStatementCache(Integer.parseInt(value));

        value = properties.getProperty("jpaSource.maximumConnectionIdleTimeInSeconds");
        if (isValue(value)) source.setMaximumConnectionIdleTimeInSeconds(Integer.parseInt(value));

        value = properties.getProperty("jpaSource.largeValueSizeInBytes");
        if (isValue(value)) source.setLargeValueSizeInBytes(Long.parseLong(value));

        value = properties.getProperty("jpaSource.autoGenerateSchema");
        if (isValue(value)) source.setAutoGenerateSchema(value);

        value = properties.getProperty("jpaSource.compressData");
        if (isValue(value)) source.setCompressData(Boolean.parseBoolean(value));

        value = properties.getProperty("jpaSource.cacheTimeToLiveInMilliseconds");
        if (isValue(value)) source.setCacheTimeToLiveInMilliseconds(Integer.parseInt(value));

        value = properties.getProperty("jpaSource.creatingWorkspacesAllowed");
        if (isValue(value)) source.setCreatingWorkspacesAllowed(Boolean.parseBoolean(value));

        value = properties.getProperty("jpaSource.defaultWorkspaceName");
        if (isValue(value)) source.setDefaultWorkspaceName(value);

        value = properties.getProperty("jpaSource.predefinedWorkspaceNames");
        if (isValue(value)) source.setPredefinedWorkspaceNames(splitValues(value));

        value = properties.getProperty("jpaSource.model");
        if (isValue(value)) source.setModel(value);

        value = properties.getProperty("jpaSource.numberOfConnectionsToAcquireAsNeeded");
        if (isValue(value)) source.setNumberOfConnectionsToAcquireAsNeeded(Integer.parseInt(value));

        value = properties.getProperty("jpaSource.referentialIntegrityEnforced");
        if (isValue(value)) source.setReferentialIntegrityEnforced(Boolean.parseBoolean(value));

        value = properties.getProperty("jpaSource.retryLimit");
        if (isValue(value)) source.setRetryLimit(Integer.parseInt(value));

        value = properties.getProperty("jpaSource.rootNodeUuid");
        if (isValue(value)) source.setRootNodeUuid(value);

        value = properties.getProperty("jpaSource.showSql");
        if (isValue(value)) source.setShowSql(Boolean.parseBoolean(value));

        return source;
    }

    protected static boolean isValue( String value ) {
        return value != null && value.trim().length() != 0;
    }

    protected static String[] splitValues( String value ) {
        String[] results = value.split(", ");
        for (int i = 0; i != results.length; ++i) {
            results[i] = results[i].trim();
            // Remove leading and trailing quotes, if there are any ...
            results[i] = results[i].replaceFirst("^['\"]+", "").replaceAll("['\"]+$", "").trim();
        }
        return results;
    }
}
