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
package org.modeshape.jcr;

import javax.jcr.Session;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Workspace;
import javax.naming.NamingException;
import junit.framework.AssertionFailedError;
import org.infinispan.schematic.document.Changes;
import org.infinispan.schematic.document.Document;
import org.infinispan.schematic.document.EditableArray;
import org.infinispan.schematic.document.EditableDocument;
import org.infinispan.schematic.document.Editor;
import org.infinispan.schematic.document.Json;
import org.junit.After;
import org.junit.Before;
import org.modeshape.jcr.api.JcrTools;

/**
 * A base class for tests that require a new JcrSession and JcrRepository for each test method.
 */
public abstract class SingleUseAbstractTest extends AbstractJcrRepositoryTest {

    protected static final String REPO_NAME = "testRepo";

    /**
     * Flag that will signal to {@link #beforeEach()} whether to automatically start the repository using the
     * {@link #createRepositoryConfiguration(String, Environment) default configuration}.
     * <p>
     * There are two ways to run tests with this class:
     * <ol>
     * <li>All tests runs against a fresh repository created from the same configuration. In this case, the
     * {@link #startRepositoryAutomatically} variable should be set to true, and the
     * {@link #createRepositoryConfiguration(String, Environment)} should be overridden if a non-default configuration is to be
     * used for all the tests.</li>
     * <li>Each test requires a fresh repository with a different configuration. In this case, the
     * {@link #startRepositoryAutomatically} variable should be set to <code>false</code>, and each test should then call one of
     * the {@link #startRepositoryWithConfiguration(RepositoryConfiguration)} methods before using the repository.</li>
     * </ol>
     */
    protected static boolean startRepositoryAutomatically = true;

    protected Environment environment = new TestingEnvironment();
    protected RepositoryConfiguration config;
    protected JcrRepository repository;
    protected JcrSession session;
    protected JcrTools tools;

    protected void startRepository() throws Exception {
        config = createRepositoryConfiguration(REPO_NAME, environment);
        repository = new JcrRepository(config);
        repository.start();
        session = repository.login();
    }

    protected void stopRepository() throws Exception {
        try {
            TestingUtil.killRepositories(repository);
        } finally {
            repository = null;
            config = null;
            environment.shutdown();
        }
    }

    @Override
    @Before
    public void beforeEach() throws Exception {
        super.beforeEach();
        if (startRepositoryAutomatically) startRepository();
        tools = new JcrTools();
    }

    @After
    public void afterEach() throws Exception {
        stopRepository();
    }

    @Override
    protected JcrSession session() {
        return session;
    }

    protected Session jcrSession() {
        return session;
    }

    @Override
    protected JcrRepository repository() {
        return repository;
    }

    /**
     * Subclasses can override this method to define the RepositoryConfiguration that will be used for the given repository name
     * and cache container. By default, this method simply returns an empty configuration:
     * 
     * <pre>
     * return new RepositoryConfiguration(repositoryName, cacheContainer);
     * </pre>
     * 
     * @param repositoryName the name of the repository to create; never null
     * @param environment the environment that the resulting configuration should use; may be null
     * @return the repository configuration
     * @throws Exception if there is a problem creating the configuration
     */
    protected RepositoryConfiguration createRepositoryConfiguration( String repositoryName,
                                                                     Environment environment ) throws Exception {
        return new RepositoryConfiguration(repositoryName, environment);
    }

    /**
     * Subclasses can call this method at the beginning of each test to shutdown any currently-running repository and to start up
     * a new repository with the given JSON configuration content.
     * 
     * @param configContent the JSON string containing the configuration for the repository (note that single quotes can be used
     *        in place of double quote, making it easier for to specify a JSON content as a Java string)
     * @throws Exception if there was a problem starting the repository
     * @see #startRepositoryWithConfiguration(Document)
     * @see #startRepositoryWithConfiguration(InputStream)
     * @see #startRepositoryWithConfiguration(RepositoryConfiguration)
     * @see #startRepositoryAutomatically
     */
    protected void startRepositoryWithConfiguration( String configContent ) throws Exception {
        Document doc = Json.read(configContent);
        startRepositoryWithConfiguration(doc);
    }

    /**
     * Subclasses can call this method at the beginning of each test to shutdown any currently-running repository and to start up
     * a new repository with the given JSON configuration document.
     * 
     * @param doc the JSON document containing the configuration for the repository
     * @throws Exception if there was a problem starting the repository
     * @see #startRepositoryWithConfiguration(String)
     * @see #startRepositoryWithConfiguration(InputStream)
     * @see #startRepositoryWithConfiguration(RepositoryConfiguration)
     * @see #startRepositoryAutomatically
     */
    protected void startRepositoryWithConfiguration( Document doc ) throws Exception {
        RepositoryConfiguration config = new RepositoryConfiguration(doc, REPO_NAME, environment);
        startRepositoryWithConfiguration(config);
    }

    /**
     * Subclasses can call this method at the beginning of each test to shutdown any currently-running repository and to start up
     * a new repository with the given JSON configuration content.
     * 
     * @param configInputStream the input stream containing the JSON content defining the configuration for the repository
     * @throws Exception if there was a problem starting the repository
     * @see #startRepositoryWithConfiguration(String)
     * @see #startRepositoryWithConfiguration(Document)
     * @see #startRepositoryWithConfiguration(RepositoryConfiguration)
     * @see #startRepositoryAutomatically
     */
    protected void startRepositoryWithConfiguration( InputStream configInputStream ) throws Exception {
        RepositoryConfiguration config = RepositoryConfiguration.read(configInputStream, REPO_NAME).with(environment);
        startRepositoryWithConfiguration(config);
    }

    /**
     * Subclasses can call this method at the beginning of each test to shutdown any currently-running repository and to start up
     * a new repository with the given repository configuration
     * 
     * @param configuration the repository configuration object; may not be null can be used in place of double quote, making it
     *        easier for to specify a JSON content as a Java string)
     * @throws Exception if there was a problem starting the repository
     * @see #startRepositoryWithConfiguration(String)
     * @see #startRepositoryWithConfiguration(Document)
     * @see #startRepositoryWithConfiguration(InputStream)
     * @see #startRepositoryAutomatically
     */
    protected void startRepositoryWithConfiguration( RepositoryConfiguration configuration ) throws Exception {
        config = configuration;
        if (repository != null) {
            try {
                repository.shutdown().get(10, TimeUnit.SECONDS);
            } finally {
                repository = null;
            }
        }
        repository = new JcrRepository(config);
        repository.start();
        session = repository.login();
    }

    /**
     * Make sure that a workspace with the supplied name exists.
     * 
     * @param workspaceName the name of the workspace; may not be null
     */
    protected void predefineWorkspace( String workspaceName ) {
        assertThat(workspaceName, is(notNullValue()));
        // Edit the configuration ...
        Editor editor = config.edit();
        EditableDocument workspaces = editor.getOrCreateDocument("workspaces");
        EditableArray predefined = workspaces.getOrCreateArray("predefined");
        predefined.addStringIfAbsent(workspaceName);

        // And apply the changes ...
        Changes changes = editor.getChanges();
        if (changes.isEmpty()) return;
        try {
            repository.apply(changes);
        } catch (Exception e) {
            throw new AssertionFailedError("Unexpected error while predefining the \"" + workspaceName + "\" workspace:"
                                           + e.getMessage());
        } 
    }

    /**
     * Utility method to get the resource on the classpath given by the supplied name
     * 
     * @param name the name (or path) of the classpath resource
     * @return the input stream to the content; may be null if the resource does not exist
     */
    protected InputStream resourceStream( String name ) {
        return getClass().getClassLoader().getResourceAsStream(name);
    }

    /**
     * Register the node types in the CND file at the given location on the classpath.
     * 
     * @param resourceName the name of the CND file on the classpath
     * @throws RepositoryException if there is a problem registering the node types
     * @throws IOException if the CND file could not be read
     */
    protected void registerNodeTypes( String resourceName ) throws RepositoryException, IOException {
        InputStream stream = resourceStream(resourceName);
        assertThat(stream, is(notNullValue()));
        Workspace workspace = session().getWorkspace();
        org.modeshape.jcr.api.nodetype.NodeTypeManager ntMgr = (org.modeshape.jcr.api.nodetype.NodeTypeManager)workspace.getNodeTypeManager();
        ntMgr.registerNodeTypes(stream, true);
    }

    /**
     * Import under the supplied parent node the repository content in the XML file at the given location on the classpath.
     * 
     * @param parent the node under which the content should be imported; may not be null
     * @param resourceName the name of the XML file on the classpath
     * @param uuidBehavior the UUID behavior; see {@link ImportUUIDBehavior} for values
     * @throws RepositoryException if there is a problem importing the content
     * @throws IOException if the XML file could not be read
     */
    protected void importContent( Node parent,
                                  String resourceName,
                                  int uuidBehavior ) throws RepositoryException, IOException {
        InputStream stream = resourceStream(resourceName);
        assertThat(stream, is(notNullValue()));
        parent.getSession().getWorkspace().importXML(parent.getPath(), stream, uuidBehavior);
    }

    /**
     * Import under the supplied parent node the repository content in the XML file at the given location on the classpath.
     * 
     * @param parentPath the path to the node under which the content should be imported; may not be null
     * @param resourceName the name of the XML file on the classpath
     * @param uuidBehavior the UUID behavior; see {@link ImportUUIDBehavior} for values
     * @throws RepositoryException if there is a problem importing the content
     * @throws IOException if the XML file could not be read
     */
    protected void importContent( String parentPath,
                                  String resourceName,
                                  int uuidBehavior ) throws RepositoryException, IOException {
        InputStream stream = resourceStream(resourceName);
        assertThat(stream, is(notNullValue()));
        session().getWorkspace().importXML(parentPath, stream, uuidBehavior);
    }
}
