/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.common.jcr;

import java.io.IOException;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.SimpleCredentials;
import org.apache.jackrabbit.api.JackrabbitRepository;
import org.apache.jackrabbit.core.TransientRepository;
import org.jboss.dna.common.util.FileUtil;
import org.jboss.dna.common.util.Logger;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;

/**
 * An abstract base class for any unit test class that needs a JCR repository during one or more (but not necessarily all) of its
 * unit tests.
 * <p>
 * Typically, unit test classes need a repository for just some of it's unit tests (other unit tests don't need the repository
 * because they are just testing other functionality of the class under test), and this class was designed for this scenario. A
 * unit test method that needs a repository should call {@link #startRepository()}, and then may proceed to use the
 * {@link #getRepository() repository}. The test method need not call {@link #shutdownRepository()}, as it is called
 * automatically after every test (and does nothing if the repository was not started in the first place).
 * </p>
 * <p>
 * Some unit test methods may have a need to repeatedly start and stop the repository, and this can be done by calling
 * {@link #startRepository()} and {@link #shutdownRepository()} as many times as required.
 * </p>
 * <p>
 * Because the {@link TransientRepository transient repository} implementation used by this class automatically cleans itself up
 * whenever all {@link Session sessions} are closed, this class maintains an open session between the time the repository is
 * started and stopped. Therefore, unit tests can persist information in one session and see the information in other sessions.
 * </p>
 * 
 * @author Randall Hauch
 */
public abstract class AbstractJcrRepositoryTest {

    public static final String TESTDATA_PATH = "./src/test/resources/";
    public static final String JACKRABBIT_DATA_PATH = "./target/testdata/jackrabbittest/";
    public static final String REPOSITORY_DIRECTORY_PATH = JACKRABBIT_DATA_PATH + "repository";
    public static final String REPOSITORY_CONFIG_PATH = TESTDATA_PATH + "jackrabbitInMemoryTestRepositoryConfig.xml";

    public static final String WORKSPACE_NAME = "default";

    private static Repository repository;

    @BeforeClass
    public static void beforeAll() throws Exception {
        // Clean up the test data ...
        FileUtil.delete(JACKRABBIT_DATA_PATH);

        // Set up the transient repository (this shouldn't do anything yet)...
        repository = new TransientRepository(REPOSITORY_CONFIG_PATH, REPOSITORY_DIRECTORY_PATH);
    }

    @AfterClass
    public static void afterAll() {
        if (repository != null) {
            try {
                JackrabbitRepository jackrabbit = (JackrabbitRepository)repository;
                jackrabbit.shutdown();
            } finally {
                repository = null;
                // Clean up the test data ...
                FileUtil.delete(JACKRABBIT_DATA_PATH);
            }
        }
    }

    /** Used to keep at least one session open during each test; when last session is closed, all data is cleaned up */
    private Session keepAliveSession;

    protected Credentials simpleCredentials = new SimpleCredentials("jsmith", "secret".toCharArray());

    /**
     * Call this method during a test that needs the repository. It only needs to be called once during a test, although calling
     * it more than once does not hurt.
     * <p>
     * The repository can be started and {@link #shutdownRepository() shutdown} repeatedly during a single test.
     * </p>
     * 
     * @throws RepositoryException if there is a problem starting the repository
     * @throws IOException if there's a problem reading the repository configuration
     * @see #shutdownRepository()
     */
    public synchronized void startRepository() throws RepositoryException, IOException {
        if (repository == null) {
            // Clean up the test data ...
            FileUtil.delete(JACKRABBIT_DATA_PATH);

            // Set up the transient repository (this shouldn't do anything yet)...
            repository = new TransientRepository(REPOSITORY_CONFIG_PATH, REPOSITORY_DIRECTORY_PATH);

        }
        if (keepAliveSession == null) {
            keepAliveSession = repository.login();
        }
    }

    /**
     * Shutdown the repository. This method is automatically called after every test, and does nothing if the repository has not
     * yet been started.
     */
    @After
    public synchronized void shutdownRepository() {
        if (keepAliveSession != null) {
            try {
                Logger.getLogger(this.getClass()).debug("Shutting down repository");
                keepAliveSession.logout();
            } finally {
                keepAliveSession = null;
                if (repository != null) {
                    try {
                        JackrabbitRepository jackrabbit = (JackrabbitRepository)repository;
                        jackrabbit.shutdown();
                    } finally {
                        repository = null;
                        // Clean up the test data ...
                        FileUtil.delete(JACKRABBIT_DATA_PATH);
                    }
                }
            }
        }
    }

    public boolean isRepositoryStarted() {
        return this.keepAliveSession != null;
    }

    /**
     * Get the repository. This will start the repository if necessary.
     * 
     * @return repository
     * @throws RepositoryException if there is a problem obtaining the repository
     * @throws IOException if the repository has not yet been {@link #startRepository() started} and there's a problem reading the
     *         repository configuration
     */
    public Repository getRepository() throws RepositoryException, IOException {
        startRepository();
        return repository;
    }

    /**
     * Get credentials that can be used to log into the repository.
     * 
     * @return credentials
     */
    public Credentials getTestCredentials() {
        return this.simpleCredentials;
    }
}
