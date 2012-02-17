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

import java.io.IOException;
import java.util.Properties;

/**
 * Test helper class which loads a property file and provides DB-specific properties, triggered by Maven profiles.
 *
 * @author Horia Chiorean
 */
public class DataSourceConfig {

    private final Properties dsProperties;

    public DataSourceConfig() {
        this("datasource.properties");
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
}
