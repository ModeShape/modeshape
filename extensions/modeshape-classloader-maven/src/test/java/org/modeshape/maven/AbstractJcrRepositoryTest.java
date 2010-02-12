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
package org.modeshape.maven;

import java.io.IOException;
import javax.jcr.Credentials;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.SimpleCredentials;
import org.jboss.security.config.IDTrustConfiguration;
import org.junit.After;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;

/**
 * An abstract base class for any unit test class that needs a JCR repository during one or more (but not necessarily all) of its
 * unit tests.
 * <p>
 * Typically, unit test classes need a repository for just some of it's unit tests (other unit tests don't need the repository
 * because they are just testing other functionality of the class under test), and this class was designed for this scenario. A
 * unit test method that needs a repository should call {@link #startRepository()}, and then may proceed to use the
 * {@link #getRepository() repository}. The test method need not call {@link #shutdownRepository()}, as it is called automatically
 * after every test (and does nothing if the repository was not started in the first place).
 * </p>
 * <p>
 * Some unit test methods may have a need to repeatedly start and stop the repository, and this can be done by calling
 * {@link #startRepository()} and {@link #shutdownRepository()} as many times as required. Note that shutting down the repository
 * will cause all transient state to be lost.
 * </p>
 */
public abstract class AbstractJcrRepositoryTest {

    public static final String TESTDATA_PATH = "./src/test/resources/";
    public static final String WORKSPACE_NAME = "default";
    public static final String USERNAME = "superuser";
    public static final String PASSWORD = "secret";

    private static JcrEngine engine;
    private static Repository repository;

    static {
        // Initialize IDTrust
        String configFile = "security/jaas.conf.xml";
        IDTrustConfiguration idtrustConfig = new IDTrustConfiguration();

        try {
            idtrustConfig.config(configFile);
        } catch (Exception ex) {
            throw new IllegalStateException(ex);
        }
    }

    protected Credentials simpleCredentials = new SimpleCredentials(USERNAME, PASSWORD.toCharArray());

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
            // Set up an in-memory ModeShape JCR engine with an in-memory repository ...
            JcrConfiguration config = new JcrConfiguration();

            String repositoryName = "Maven Repository";
            String workspaceName = "default";
            String repositorySource = "ddlRepositorySource";

            config = new JcrConfiguration();
            // Set up the in-memory source where we'll upload the content and where the sequenced output will be stored ...
            config.repositorySource(repositorySource)
                  .usingClass(InMemoryRepositorySource.class)
                  .setDescription("The repository for our content")
                  .setProperty("defaultWorkspaceName", workspaceName);
            // Set up the JCR repository to use the source ...
            config.repository(repositoryName).setSource(repositorySource);
            config.save();
            engine = config.build();
            engine.start();

            repository = engine.getRepository(repositoryName);
        }
    }

    /**
     * Shutdown the repository. This method is automatically called after every test, and does nothing if the repository has not
     * yet been started.
     */
    @After
    public synchronized void shutdownRepository() {
        if (repository != null) {
            if (engine != null) {
                try {
                    engine.shutdown();
                } finally {
                    engine = null;
                    repository = null;
                }
            }
        }
    }

    public boolean isRepositoryStarted() {
        return repository != null;
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
