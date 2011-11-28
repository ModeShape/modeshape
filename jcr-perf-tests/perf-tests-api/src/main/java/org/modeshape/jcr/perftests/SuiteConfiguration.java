/*
 * JBoss, Home of Professional Open Source
 * Copyright [2011], Red Hat, Inc., and individual contributors
 * by the @authors tag. See the copyright.txt in the distribution for a
 * full listing of individual contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

    private final Repository repository;
    private final Credentials credentials;
    private final int nodeCount;

    SuiteConfiguration( Repository repository, Credentials credentials, String configFile ) throws IOException {
        this.repository = repository;
        this.credentials = credentials;
        Properties properties  = loadPropertiesFile(configFile);
        String nodeCount = properties.getProperty("testsuite.config.nodeCount");
        if (nodeCount != null && !nodeCount.isEmpty()) {
            this.nodeCount = Integer.valueOf(nodeCount);
        }
        else {
            this.nodeCount = DEFAULT_NODE_COUNT;
        }
    }

    private Properties loadPropertiesFile( String configFile ) throws IOException {
        Properties suiteProperties = new Properties();
        suiteProperties.load(getClass().getClassLoader().getResourceAsStream(configFile));
        return suiteProperties;
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
