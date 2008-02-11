/*
 *
 */
package org.jboss.dna.maven;

import javax.jcr.Repository;
import javax.jcr.Session;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.TransientRepository;
import org.jboss.dna.common.util.FileUtil;
import org.jboss.dna.common.util.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * @author Randall Hauch
 */
public abstract class JackrabbitRepositoryTest {

    public static final String TESTDATA_PATH = "./src/test/resources/";
    public static final String JACKRABBIT_DATA_PATH = "./target/testdata/jackrabbittest/";
    public static final String REPOSITORY_DIRECTORY_PATH = JACKRABBIT_DATA_PATH + "repository";
    public static final String REPOSITORY_CONFIG_PATH = TESTDATA_PATH + "jackrabbitInMemoryTestRepositoryConfig.xml";

    public static final String WORKSPACE_NAME = "default";

    protected static Repository repository;

    @BeforeClass
    public static void beforeAll() throws Exception {
        // Clean up the test data ...
        FileUtil.delete(JACKRABBIT_DATA_PATH);

        // Set up the transient repository ...
        repository = new TransientRepository(REPOSITORY_CONFIG_PATH, REPOSITORY_DIRECTORY_PATH);
    }

    @AfterClass
    public static void afterAll() throws Exception {
        try {
            JackrabbitRepository jackrabbit = (JackrabbitRepository)repository;
            jackrabbit.shutdown();
        } finally {
            // Clean up the test data ...
            FileUtil.delete(JACKRABBIT_DATA_PATH);
        }
    }

    /** Used to keep at least one session open during each test; when last session is closed, all data is cleaned up */
    private Session keepAliveSession;

    public void startRepository() throws Exception {
        keepAliveSession = repository.login();
    }

    @After
    public void shutdownRepository() throws Exception {
        if (keepAliveSession != null) {
            Logger.getLogger(this.getClass()).info("Shutting down repository");
            keepAliveSession.logout();
        }
    }
}
