/*
 *
 *  * ModeShape (http://www.modeshape.org)
 *  * See the COPYRIGHT.txt file distributed with this work for information
 *  * regarding copyright ownership.  Some portions may be licensed
 *  * to Red Hat, Inc. under one or more contributor license agreements.
 *  * See the AUTHORS.txt file in the distribution for a full listing of
 *  * individual contributors.
 *  *
 *  * ModeShape is free software. Unless otherwise indicated, all code in ModeShape
 *  * is licensed to you under the terms of the GNU Lesser General Public License as
 *  * published by the Free Software Foundation; either version 2.1 of
 *  * the License, or (at your option) any later version.
 *  *
 *  * ModeShape is distributed in the hope that it will be useful,
 *  * but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 *  * Lesser General Public License for more details.
 *  *
 *  * You should have received a copy of the GNU Lesser General Public
 *  * License along with this software; if not, write to the Free
 *  * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 *  * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 */
package org.modeshape.jcr.perftests;

import javax.jcr.Credentials;
import javax.jcr.Repository;
import java.io.IOException;
import java.util.Properties;

/**
 * Holder for the properties used to configure a test suite. Some of the configuration properties can be loaded from a config
 * file (e.g. testsuite.properties)
 *
 * @author Horia Chiorean
 */
public final class SuiteConfiguration {

    private static final int DEFAULT_NODE_COUNT = 10;

    private int nodeCount = DEFAULT_NODE_COUNT;
    private Repository repository;
    private Credentials credentials;

    SuiteConfiguration( Repository repository, Credentials credentials, String configFile ) throws IOException {
        this.repository = repository;
        this.credentials = credentials;
        loadPropertiesFromFile(configFile);
    }

    void loadPropertiesFromFile(String configFile) throws IOException {
        Properties suiteProperties = new Properties();
        suiteProperties.load(getClass().getClassLoader().getResourceAsStream(configFile));
        String nodeCount = suiteProperties.getProperty("testsuite.config.nodeCount");
        if (nodeCount != null && !nodeCount.isEmpty()) {
            this.nodeCount = Integer.valueOf(nodeCount);
        }
    }

    public int getNodeCount() {
        return nodeCount;
    }

    public Repository getRepository() {
        return repository;
    }

    public Credentials getCredentials() {
        return credentials;
    }
}
