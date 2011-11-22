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
