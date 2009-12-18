/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.connector.meta.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;
import javax.sql.DataSource;
import org.jboss.dna.common.util.IoUtil;
import org.jboss.dna.connector.meta.jdbc.JdbcMetadataSource;

public class TestEnvironment {

    public static JdbcMetadataSource configureJdbcMetadataSource( String sourceName,
                                                                  Object testCase ) {
        Properties properties = new Properties();
        ClassLoader loader = testCase instanceof Class<?> ? ((Class<?>)testCase).getClassLoader() : testCase.getClass().getClassLoader();
        try {
            properties.load(loader.getResourceAsStream("database.properties"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        JdbcMetadataSource source = new JdbcMetadataSource();
        source.setName(sourceName);
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

        value = properties.getProperty("jpaSource.cacheTimeToLiveInMilliseconds");
        if (isValue(value)) source.setCacheTimeToLiveInMilliseconds(Integer.parseInt(value));

        value = properties.getProperty("jpaSource.defaultWorkspaceName");
        if (isValue(value)) source.setDefaultWorkspaceName(value);

        value = properties.getProperty("jpaSource.numberOfConnectionsToAcquireAsNeeded");
        if (isValue(value)) source.setNumberOfConnectionsToAcquireAsNeeded(Integer.parseInt(value));

        value = properties.getProperty("jpaSource.retryLimit");
        if (isValue(value)) source.setRetryLimit(Integer.parseInt(value));

        value = properties.getProperty("jpaSource.rootNodeUuid");
        if (isValue(value)) source.setRootNodeUuid(value);

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

    public static void executeDdl( DataSource dataSource,
                                   String pathToDdl ) throws Exception {
        Connection conn = null;
        Statement stmt = null;
        InputStream is = null;


        try {
            conn = dataSource.getConnection();
            is = TestEnvironment.class.getResourceAsStream(pathToDdl);
            String ddl = IoUtil.read(is);
            
            stmt = conn.createStatement();
            stmt.execute(ddl);

        } finally {
            if (stmt != null) try {
                stmt.close();
            } catch (Exception ignore) {
            }
            if (conn != null) try {
                conn.close();
            } catch (Exception ignore) {
            }
            if (is != null) try {
                is.close();
            } catch (Exception ignore) {
            }
        }
    }
}
