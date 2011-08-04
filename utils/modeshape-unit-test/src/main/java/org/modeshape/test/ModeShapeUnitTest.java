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
package org.modeshape.test;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import javax.jcr.Credentials;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.Repository;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.observation.Event;
import javax.jcr.observation.EventIterator;
import javax.jcr.observation.EventListener;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.collection.Problems;
import org.modeshape.graph.property.Path;
import org.modeshape.jcr.CndNodeTypeReader;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrTools;
import org.modeshape.repository.sequencer.SequencingService;
import org.xml.sax.SAXException;

public abstract class ModeShapeUnitTest {

    protected static JcrConfiguration configuration;
    private static JcrEngine engine;
    private static final Set<Session> openSessions = new HashSet<Session>();
    private static Session session;
    private JcrTools tools;
    protected boolean print;
    protected boolean debug;
    protected static ClassLoader classLoader;

    @Before
    public void beforeEach() throws Exception {
        // Get the classloader for the test class ...
        classLoader = getClass().getClassLoader();

        openSessions.clear();
        tools = new JcrTools();
        print = false;
        debug = false;
    }

    @After
    public void afterEach() throws Exception {
        closeSessions();
    }

    protected JcrTools tools() {
        return tools;
    }

    /**
     * Define the path to the default configuration that will be used if no other configuration is specified.
     * 
     * @return the path to the default ModeShape configuration file, or null if there is no default configuration
     */
    protected String getPathToDefaultConfiguration() {
        return null;
    }

    /**
     * Explicitly start the JCR engine so that it uses the supplied configuration file. If an engine is already running, it will
     * first be shutdown.
     * 
     * @param pathToConfigurationFile the path to the ModeShape configuration file
     * @return the JCR engine instance that is ready to use
     * @throws IOException if the ModeShape configuration file could not be found or was invalid
     */
    protected JcrEngine startEngineUsing( String pathToConfigurationFile ) throws IOException {
        return startEngineUsing(pathToConfigurationFile, null);
    }

    /**
     * Explicitly start the JCR engine so that it uses the supplied configuration file. If an engine is already running, it will
     * first be shutdown.
     * <p>
     * This method can be called from the method annotated with {@link BeforeClass} annotation.
     * </p>
     * 
     * @param pathToConfigurationFile the path to the ModeShape configuration file
     * @param testClass the class under test, used to get the classloader if the configuration file is to be found on the
     *        classpath; may be null if the configuration file is on the file system in a location relative to the directory where
     *        the JVM was started or to the "src/test/resources" directory.
     * @return the JCR engine instance that is ready to use
     * @throws IOException if the ModeShape configuration file could not be found or was invalid
     */
    protected static JcrEngine startEngineUsing( String pathToConfigurationFile,
                                                 Class<?> testClass ) throws IOException {
        assertThat(pathToConfigurationFile, is(notNullValue()));
        stopEngine();
        try {
            try {
                configuration = new JcrConfiguration();
                configuration.loadFrom(pathToConfigurationFile);
            } catch (IOException e) {
                boolean read = false;
                if (classLoader == null && testClass != null) {
                    classLoader = testClass.getClassLoader();
                }
                if (classLoader != null) {
                    // Try loading it from the classpath ...
                    URL url = classLoader.getResource(pathToConfigurationFile);
                    if (url != null) {
                        configuration = new JcrConfiguration();
                        configuration.loadFrom(url);
                        read = true;
                    }
                }

                if (!read) {
                    // Re-create the configuration object (otherwise, it will think there are changes from the first load) ...
                    configuration = new JcrConfiguration();
                    // Try loading the configuration from within the src/test/resources folder ...
                    pathToConfigurationFile = pathToConfigurationFile.startsWith("/") ? pathToConfigurationFile : "/"
                                                                                                                  + pathToConfigurationFile;
                    pathToConfigurationFile = "src/test/resources" + pathToConfigurationFile;
                    configuration.loadFrom(pathToConfigurationFile);
                }
            }
        } catch (SAXException e) {
            throw new IOException(e);
        }
        return startEngine();
    }

    private static JcrEngine startEngine() {
        assertThat(configuration, is(notNullValue()));
        if (engine == null) {
            Problems problems = configuration.getProblems();
            if (!problems.isEmpty()) {
                System.out.println(problems);
                fail("Unable to start engine due to problems. See console for details.");
            }
            engine = configuration.build();
            engine.start();
        }
        return engine;
    }

    /**
     * If the JCR engine is running, explicitly close all open sessions and shut down the JCR engine. This method does nothing if
     * the engine is not running.
     */
    protected static void closeSessions() {
        if (openSessions != null) {
            try {
                for (Session session : openSessions) {
                    try {
                        if (session.isLive()) session.logout();
                    } catch (Throwable e) {
                        e.printStackTrace();
                    }
                }
            } finally {
                openSessions.clear();
            }
        }
        session = null;
    }

    /**
     * If the JCR engine is running, explicitly close all open sessions and shut down the JCR engine. This method does nothing if
     * the engine is not running.
     */
    protected static void stopEngine() {
        closeSessions();
        if (engine != null) {
            try {
                engine.shutdownAndAwaitTermination(3, TimeUnit.SECONDS);
            } catch (InterruptedException err) {
                err.printStackTrace();
            } finally {
                engine = null;
            }
        }
    }

    /**
     * Get the JCR engine. If the engine has not yet been started, it will be started using the
     * {@link #getPathToDefaultConfiguration() default configuration}.
     * 
     * @return the JCR Engine; never null
     * @throws IOException if the ModeShape configuration file could not be found or was invalid
     */
    protected JcrEngine engine() throws IOException {
        if (engine == null) {
            String pathToConfigFile = getPathToDefaultConfiguration();
            if (pathToConfigFile != null) {
                engine = startEngineUsing(pathToConfigFile);
            } else {
                Problems problems = configuration.getProblems();
                if (!problems.isEmpty()) {
                    System.out.println(problems);
                    fail("Unable to start engine due to problems. See console for details.");
                }
                engine = configuration.build();
                engine.start();
                return engine;
            }
        }
        return engine;
    }

    /**
     * Get the current session, or if there is none create a new session to the first JCR repository in the current configuration.
     * 
     * @return the session, which will be automatically closed when the test ends
     * @throws RepositoryException if there is a problem obtaining a session
     */
    protected Session session() throws RepositoryException {
        if (session == null || !session.isLive()) session = sessionTo(null, null, null);
        return session;
    }

    /**
     * Create a new session to the first JCR repository in the current configuration. This new session will be set as the
     * {@link #session() current session}.
     * 
     * @param repositoryName the name of the repository, or null if the first repository in the configuration should be used
     * @return the session, which will be automatically closed when the test ends
     * @throws RepositoryException if there is a problem obtaining a session
     */
    protected Session sessionTo( String repositoryName ) throws RepositoryException {
        return sessionTo(repositoryName, null, null);
    }

    /**
     * Create a new session to the first JCR repository in the current configuration. This new session will be set as the
     * {@link #session() current session}.
     * 
     * @param repositoryName the name of the repository, or null if the first repository in the configuration should be used
     * @param workspaceName the name of the workspace, or null if the default workspace in the repository should be used
     * @return the session, which will be automatically closed when the test ends
     * @throws RepositoryException if there is a problem obtaining a session
     */
    protected Session sessionTo( String repositoryName,
                                 String workspaceName ) throws RepositoryException {
        return sessionTo(repositoryName, workspaceName, null);
    }

    /**
     * Create a new session to the first JCR repository in the current configuration. This new session will be set as the
     * {@link #session() current session}.
     * 
     * @param repositoryName the name of the repository, or null if the first repository in the configuration should be used
     * @param workspaceName the name of the workspace, or null if the default workspace in the repository should be used
     * @param credentials the credentials to use, or null if no credentials should be used
     * @return the session, which will be automatically closed when the test ends
     * @throws RepositoryException if there is a problem obtaining a session
     */
    protected Session sessionTo( String repositoryName,
                                 String workspaceName,
                                 Credentials credentials ) throws RepositoryException {
        try {
            if (configuration == null) {
                startEngineUsing(getPathToDefaultConfiguration());
            }
            if (engine == null) {
                startEngine();
            }
            Repository repository = repository(repositoryName);
            session = credentials != null ? repository.login(credentials, workspaceName) : repository.login(workspaceName);
            openSessions.add(session);
            return session;
        } catch (IOException e) {
            throw new AssertionError(e);
        }
    }

    /**
     * Log out of the current session, if there is one.
     * 
     * @throws RepositoryException if there is a problem logging out of the current session
     */
    protected void logout() throws RepositoryException {
        if (session != null) {
            try {
                session.logout();
            } finally {
                session = null;
            }
        }
    }

    protected static Repository repository( String repositoryName ) throws RepositoryException {
        if (repositoryName == null) {
            if (configuration.repositoryNames().size() == 0) fail("No repository is configured");
            repositoryName = configuration.repositoryNames().iterator().next();
        }
        return engine.getRepository(repositoryName);
    }

    protected static Repository defaultRepository() throws RepositoryException {
        return repository(null);
    }

    protected void setSession( Session session ) {
        ModeShapeUnitTest.session = session;
    }

    protected Repository repository() throws RepositoryException, IOException {
        if (configuration.repositories().size() == 0) fail("No repository is configured");
        String repositoryName = configuration.repositories().iterator().next().getName();
        return engine().getRepository(repositoryName);
    }

    protected static String defaultRepositoryName() {
        if (configuration.repositories().size() == 0) fail("No repository is configured");
        return configuration.repositories().iterator().next().getName();
    }

    protected void importContent( String pathToResourceFile ) throws Exception {
        importContent(getClass(), pathToResourceFile);
    }

    protected void importContent( String pathToResourceFile,
                                  String repositoryName,
                                  String workspaceName ) throws Exception {
        importContent(getClass(), pathToResourceFile, repositoryName, workspaceName);
    }

    protected void importContent( String pathToResourceFile,
                                  String repositoryName,
                                  String workspaceName,
                                  String jcrPathToImportUnder ) throws Exception {
        importContent(getClass(), pathToResourceFile, repositoryName, workspaceName, jcrPathToImportUnder);
    }

    protected static void importContent( Class<?> testClass,
                                         String pathToResourceFile ) throws Exception {
        importContent(testClass, pathToResourceFile, defaultRepositoryName(), null, null);
    }

    protected static void importContent( Class<?> testClass,
                                         String pathToResourceFile,
                                         int importBehavior ) throws Exception {
        importContent(testClass, pathToResourceFile, defaultRepositoryName(), null, null, importBehavior);
    }

    protected static void importContent( Class<?> testClass,
                                         String pathToResourceFile,
                                         String repositoryName,
                                         String workspaceName ) throws Exception {
        importContent(testClass, pathToResourceFile, repositoryName, workspaceName, null);
    }

    protected static void importContent( Class<?> testClass,
                                         String pathToResourceFile,
                                         String repositoryName,
                                         String workspaceName,
                                         String jcrPathToImportUnder ) throws Exception {
        int behavior = ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW;
        importContent(testClass, pathToResourceFile, repositoryName, workspaceName, null, behavior);
    }

    protected static void importContent( Class<?> testClass,
                                         String pathToResourceFile,
                                         String repositoryName,
                                         String workspaceName,
                                         String jcrPathToImportUnder,
                                         int importBehavior ) throws Exception {

        // Use a session to load the contents ...
        try {
            InputStream stream = testClass.getClassLoader().getResourceAsStream(pathToResourceFile);
            if (stream == null) {
                String msg = "\"" + pathToResourceFile + "\" does not reference an existing file";
                System.err.println(msg);
                throw new IllegalArgumentException(msg);
            }
            assertNotNull(stream);
            if (jcrPathToImportUnder == null || jcrPathToImportUnder.trim().length() == 0) jcrPathToImportUnder = "/";

            Session session = repository(repositoryName).login(workspaceName);
            try {
                session.getWorkspace().importXML(jcrPathToImportUnder, stream, importBehavior);
            } finally {
                try {
                    session.save();
                } finally {
                    stream.close();
                    session.logout();
                }
            }
            session.save();
        } catch (RuntimeException t) {
            t.printStackTrace();
            throw t;
        } catch (Exception t) {
            t.printStackTrace();
            throw t;
        }
    }

    protected URL resourceUrl( String name ) {
        return getClass().getClassLoader().getResource(name);
    }

    protected Node uploadFile( String resourceFilePath,
                               String parentPath ) throws RepositoryException, IOException {
        return uploadFile(resourceUrl(resourceFilePath), parentPath);
    }

    protected Node uploadFile( String folder,
                               String fileName,
                               String parentPath ) throws RepositoryException, IOException {
        return uploadFile(resourceUrl(folder + fileName), parentPath);
    }

    protected Node uploadFile( URL url,
                               String parentPath ) throws RepositoryException, IOException {
        // Grab the last segment of the URL path, using it as the filename
        String filename = url.getPath().replaceAll("([^/]*/)*", "");
        if (!parentPath.startsWith("/")) parentPath = "/" + parentPath;
        if (!parentPath.endsWith("/")) parentPath = parentPath + "/";
        final String nodePath = parentPath + filename;

        // Wait a bit before uploading, to make sure everything is ready ...
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }

        if (debug) {
            System.out.println("---> Uploading '" + filename + "' into '" + nodePath + "'");
        }

        // Now use the JCR API to upload the file ...
        Session session = session();
        final CountDownLatch latch = new CountDownLatch(1);
        EventListener listener = new EventListener() {
            /**
             * {@inheritDoc}
             * 
             * @see javax.jcr.observation.EventListener#onEvent(javax.jcr.observation.EventIterator)
             */
            @Override
            public void onEvent( EventIterator events ) {
                while (events.hasNext()) {
                    try {
                        if (events.nextEvent().getPath().equals(nodePath)) {
                            latch.countDown();
                        }
                    } catch (Throwable e) {
                        latch.countDown();
                        fail(e.getMessage());
                    }
                }
            }
        };
        session.getWorkspace()
               .getObservationManager()
               .addEventListener(listener, Event.NODE_ADDED, parentPath, true, null, null, false);
        Node newFileNode = tools.uploadFile(session, nodePath, url);

        // Save the session ...
        session.save();

        // Now await for the event describing the newly-added file ...
        try {
            latch.await(2, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            fail(e.getMessage());
        }
        return newFileNode;
    }

    protected void uploadFiles( String destinationPath,
                                String... resourcePaths ) throws Exception {
        for (String resourcePath : resourcePaths) {
            uploadFile(resourcePath, destinationPath);
        }
    }

    protected void removeAllChildren( String absPath ) throws RepositoryException {
        try {
            Node node = session().getNode(absPath);
            tools.removeAllChildren(node);
        } catch (PathNotFoundException e) {
            // ignore
        }
    }

    /**
     * Get the sequencing statistics.
     * 
     * @return the statistics; never null
     */
    protected SequencingService.Statistics getStatistics() {
        return engine.getSequencingService().getStatistics();
    }

    /**
     * Block until the next sequencing operation finishes. If not enough sequenced nodes are produced within 5 seconds, this
     * method causes a unit test failure.
     * 
     * @throws InterruptedException if this thread is interrupted while waiting
     */
    protected void waitUntilSequencingFinishes() throws InterruptedException {
        // check 50 times, waiting 0.1 seconds between (for a total of 5 seconds max) ...
        waitUntilSequencedNodesIs(1, 5);
    }

    /**
     * Block until the total number of sequenced nodes is at last the value specified. If not enough sequenced nodes are produced
     * within 5 seconds, this method causes a unit test failure.
     * 
     * @param totalNumberOfNodesSequenced the minimum number of sequenced nodes
     * @throws InterruptedException if this thread is interrupted while waiting
     */
    protected void waitUntilSequencedNodesIs( int totalNumberOfNodesSequenced ) throws InterruptedException {
        // check 50 times, waiting 0.1 seconds between (for a total of 5 seconds max) ...
        waitUntilSequencedNodesIs(totalNumberOfNodesSequenced, 5);
    }

    protected Node waitUntilSequencedNodeIsAvailable( String path ) throws InterruptedException, RepositoryException {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i != 10 * 5; ++i) { // 10 seconds at the most
            try {
                return assertNode(path);
            } catch (PathNotFoundException t) {
                Thread.sleep(200); // wait a bit while the new content is indexed
            } catch (AssertionError t) {
                Thread.sleep(200); // wait a bit while the new content is indexed
            }
        }
        long endTime = System.currentTimeMillis();
        double seconds = (endTime - startTime) / 1000.0d;
        fail("Unable to find '" + path + "' even after waiting " + seconds + " seconds");
        return null;
    }

    protected Node waitUntilSequencedNodeIsAvailable( String path,
                                                      String primaryType,
                                                      String... mixinTypes ) throws InterruptedException, RepositoryException {
        long startTime = System.currentTimeMillis();
        for (int i = 0; i != 10 * 5; ++i) { // 10 seconds at the most
            try {
                return assertNode(path, primaryType, mixinTypes);
            } catch (PathNotFoundException t) {
                if (debug) {
                    System.out.println("---> Waiting for the sequenced node at " + path);
                }
                Thread.sleep(200); // wait a bit while the new content is indexed
            } catch (AssertionError t) {
                if (debug) {
                    System.out.println("---> Waiting for the sequenced node at " + path);
                }
                Thread.sleep(200); // wait a bit while the new content is indexed
            }
        }
        long endTime = System.currentTimeMillis();
        double seconds = (endTime - startTime) / 1000.0d;
        fail("Unable to find '" + path + "' even after waiting " + seconds + " seconds");
        return null;
    }

    /**
     * Block until the total number of sequenced nodes is at last the value specified. If not enough sequenced nodes are produced
     * within the allotted number of seconds, this method causes a unit test failure.
     * 
     * @param totalNumberOfNodesSequenced the minimum number of sequenced nodes
     * @param maxNumberOfSeconds the maximum number of seconds to block
     * @throws InterruptedException if this thread is interrupted while waiting
     */
    protected void waitUntilSequencedNodesIs( int totalNumberOfNodesSequenced,
                                              int maxNumberOfSeconds ) throws InterruptedException {
        long numFound = 0;
        int actualMillis = 0;
        int numberOfMillis = (int)TimeUnit.SECONDS.toMillis(maxNumberOfSeconds);
        int numberOfIterations = numberOfMillis / 100;
        for (int i = 0; i != numberOfIterations; i++) {
            numFound = getStatistics().getNumberOfNodesSequenced();
            if (numFound >= totalNumberOfNodesSequenced) {
                return;
            }
            Thread.sleep(100);
            actualMillis += 100;
        }
        Thread.sleep(100);
        fail("Expected to find " + totalNumberOfNodesSequenced + " nodes sequenced, but found " + numFound);
    }

    protected void registerNodeTypes( String pathToCndResourceFile ) {
        InputStream stream = getClass().getClassLoader().getResourceAsStream(pathToCndResourceFile);
        if (stream == null) {
            String msg = "\"" + pathToCndResourceFile + "\" does not reference an existing file";
            System.err.println(msg);
            throw new IllegalArgumentException(msg);
        }
        assertNotNull(stream);
        try {
            Session session = session();
            CndNodeTypeReader cndReader = new CndNodeTypeReader(session);
            cndReader.read(stream, pathToCndResourceFile);
            session.getWorkspace().getNodeTypeManager().registerNodeTypes(cndReader.getNodeTypeDefinitions(), true);
        } catch (RepositoryException re) {
            throw new IllegalStateException("Could not load node type definition files", re);
        } catch (IOException ioe) {
            throw new IllegalStateException("Could not access node type definition files", ioe);
        } finally {
            try {
                stream.close();
            } catch (IOException closer) {
            }
        }
    }

    protected void assertNodeType( String name,
                                   boolean isAbstract,
                                   boolean isMixin,
                                   boolean isQueryable,
                                   boolean hasOrderableChildNodes,
                                   String primaryItemName,
                                   int numberOfDeclaredChildNodeDefinitions,
                                   int numberOfDeclaredPropertyDefinitions,
                                   String... supertypes ) throws Exception {
        NodeType nodeType = session().getWorkspace().getNodeTypeManager().getNodeType(name);
        assertThat(nodeType, is(notNullValue()));
        assertThat("Unexpected abstract characteristic", nodeType.isAbstract(), is(isAbstract));
        assertThat("Unexpected mixin characteristic", nodeType.isMixin(), is(isMixin));
        assertThat("Unexpected queryable characteristic", nodeType.isQueryable(), is(isQueryable));
        assertThat("Unexpected orderable child nodes", nodeType.hasOrderableChildNodes(), is(hasOrderableChildNodes));
        assertThat("Unexpected primary item name", nodeType.getPrimaryItemName(), is(primaryItemName));
        assertThat("Unexpected number of declared supertypes", nodeType.getDeclaredSupertypes().length, is(supertypes.length));
        for (int i = 0; i != supertypes.length; ++i) {
            assertThat(nodeType.getDeclaredSupertypes()[i].getName(), is(supertypes[i]));
        }
        assertThat("Unexpected number of declared child node definitions",
                   nodeType.getDeclaredChildNodeDefinitions().length,
                   is(numberOfDeclaredChildNodeDefinitions));
        assertThat("Unexpected number of declared property definitions",
                   nodeType.getDeclaredPropertyDefinitions().length,
                   is(numberOfDeclaredPropertyDefinitions));
    }

    protected void assertNodeTypes( String... nodeTypeNames ) throws Exception {
        for (String nodeTypeName : nodeTypeNames) {
            NodeType nodeType = session().getWorkspace().getNodeTypeManager().getNodeType(nodeTypeName);
            assertThat(nodeType, is(notNullValue()));
        }
    }

    protected void assertChildNode( Node parentNode,
                                    String childNodeName,
                                    String propName,
                                    String expectedValue ) throws Exception {
        // Find child node
        Node childNode = null;
        for (NodeIterator iter = parentNode.getNodes(); iter.hasNext();) {
            Node nextNode = iter.nextNode();
            if (nextNode.getName().equals(childNodeName)) {
                childNode = nextNode;
                break;
            }
        }
        if (childNode != null) {
            assertThat(childNode.hasProperty(propName), is(true));
            assertSingleValueProperty(childNode, propName, expectedValue);

        } else {
            fail("NODE: " + childNodeName + " not found");
        }

    }

    protected void assertNodeIsSearchable( String path,
                                           String nodeType,
                                           String... otherTypes ) throws RepositoryException, InterruptedException {
        boolean p = print;
        try {
            print = false;
            for (int i = 0; i != 10 * 5; ++i) {
                try {
                    printQuery("SELECT * FROM [" + nodeType + "] WHERE PATH() = $path", 1, var("path", path));
                    if (otherTypes != null) {
                        for (String type : otherTypes) {
                            printQuery("SELECT * FROM [" + type + "] WHERE PATH() = $path", 1, var("path", path));
                        }
                    }
                    break;
                } catch (AssertionError e) {
                    if (debug) {
                        System.out.println("---> Waiting for a queryable node at " + path);
                    }
                    Thread.sleep(200); // wait a bit while the new content is indexed
                }
            }
        } finally {
            print = p;
        }
    }

    protected void assertNoNode( String path ) throws RepositoryException {
        try {
            Node node = assertNode(path);
            fail("Node at '" + node.getPath() + "' should not exist.");
        } catch (PathNotFoundException e) {
            // expected
        }
    }

    protected Node assertNode( String path ) throws RepositoryException {
        return session().getNode(path);
    }

    protected Node assertNode( String path,
                               String primaryType,
                               String... mixinTypes ) throws RepositoryException, InterruptedException {
        Node node = session().getNode(path);
        assertThat(node.getPrimaryNodeType().getName(), is(primaryType));
        primaryType = node.getPrimaryNodeType().getName();
        Set<String> expectedMixinTypes = new HashSet<String>(Arrays.asList(mixinTypes));
        Set<String> actualMixinTypes = new HashSet<String>();
        for (NodeType mixin : node.getMixinNodeTypes()) {
            actualMixinTypes.add(mixin.getName());
        }
        assertThat("Mixin types do not match", actualMixinTypes, is(expectedMixinTypes));
        assertNodeIsSearchable(path, primaryType, mixinTypes);
        return node;
    }

    protected Node assertNode( Node topNode,
                               String name ) throws Exception {
        Node node = findNode(topNode, name);
        if (node == null) {
            fail("NODE: " + name + " not found");
        }
        return node;
    }

    protected Node assertNode( Node topNode,
                               String name,
                               String propName ) throws Exception {
        Node node = findNode(topNode, name);

        if (node != null) {
            assertThat(node.hasProperty(propName), is(true));
        } else {
            fail("NODE: " + name + " not found");
        }
        return node;
    }

    protected void assertSimpleStringProperty( Node node,
                                               String propName,
                                               String expectedValue ) throws Exception {
        assertThat(node.hasProperty(propName), is(true));
        assertSingleValueProperty(node, propName, expectedValue);
    }

    protected void assertNode( Node topNode,
                               String name,
                               String propName,
                               String expectedValue ) throws Exception {
        Node node = findNode(topNode, name);

        if (node != null) {
            assertThat(node.hasProperty(propName), is(true));
            assertSingleValueProperty(node, propName, expectedValue);

        } else {
            fail("NODE: " + name + " not found");
        }

    }

    protected void assertNode( Node topNode,
                               String name,
                               String propName,
                               int expectedValue ) throws Exception {
        Node node = findNode(topNode, name);

        if (node != null) {
            assertThat(node.hasProperty(propName), is(true));
            assertSingleValueProperty(node, propName, expectedValue);

        } else {
            fail("NODE: " + name + " not found");
        }

    }

    protected Value value( String value ) throws Exception {
        return session().getValueFactory().createValue(value);
    }

    protected Path path( String path ) {
        return engine.getExecutionContext().getValueFactories().getPathFactory().create(path);
    }

    protected String string( Object obj ) {
        return engine.getExecutionContext().getValueFactories().getStringFactory().create(obj);
    }

    protected void assertSingleValueProperty( Node node,
                                              String propNameStr,
                                              String expectedValue ) throws Exception {
        if (node == null) {
            return;
        }
        Value expValue = value(expectedValue);
        Property prop = node.getProperty(propNameStr);
        if (prop.getDefinition().isMultiple()) {
            boolean hasValue = false;

            Object[] values = prop.getValues();
            for (Object val : values) {
                if (val.equals(expValue)) {
                    hasValue = true;
                }
            }

            assertThat(hasValue, is(true));
        } else {
            Object actualValue = prop.getValue();
            assertThat(expValue, is(actualValue));
        }

    }

    protected void assertSingleValueProperty( Node node,
                                              String propNameStr,
                                              int expectedValue ) throws Exception {
        Property prop = node.getProperty(propNameStr);
        Value expValue = session().getValueFactory().createValue(expectedValue);
        Object actualValue = prop.getValue();
        assertThat(expValue, is(actualValue));

    }

    protected void assertMixin( Node topNode,
                                String nodeName,
                                String nodeType ) throws Exception {
        Node node = findNode(topNode, nodeName);

        if (node != null) {
            assertMixin(node, nodeType);

        } else {
            fail("NODE: " + nodeName + " not found");
        }
    }

    protected boolean hasMixin( Node node,
                                String nodeType ) throws Exception {
        for (NodeType mixin : node.getMixinNodeTypes()) {
            String mixinName = mixin.getName();
            if (mixinName.equals(nodeType)) {
                return true;
            }
        }
        return false;
    }

    protected void assertMixin( Node node,
                                String nodeType ) throws Exception {
        boolean foundMixin = hasMixin(node, nodeType);

        assertThat(foundMixin, is(true));
    }

    protected void assertNodeType( Node topNode,
                                   String nodeName,
                                   String nodeTypeName ) throws Exception {
        Node node = findNode(topNode, nodeName);

        if (node != null) {
            assertThat(node.isNodeType(nodeTypeName), is(true));
        } else {
            fail("NODE: " + nodeName + " not found");
        }

    }

    protected void assertNodeTypes( Node topNode,
                                    String nodeName,
                                    String nodeTypeName,
                                    String... moreNodeTypeNames ) throws Exception {
        Node node = findNode(topNode, nodeName);

        if (node != null) {
            assertThat(node.isNodeType(nodeTypeName), is(true));
            for (String nextTypeName : moreNodeTypeNames) {
                assertThat(node.isNodeType(nextTypeName), is(true));
            }
        } else {
            fail("NODE: " + nodeName + " not found");
        }

    }

    protected Node findNode( Node node,
                             String name ) throws Exception {
        if (node.getName().equals(name)) {
            return node;
        }
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            Node nextNode = iter.nextNode();
            if (nextNode.getName().equals(name)) {
                return nextNode;
            }
            Node someNode = findNode(nextNode, name);
            if (someNode != null) {
                return someNode;
            }
        }

        return null;
    }

    protected Node findNode( Node node,
                             String name,
                             String type ) throws Exception {
        if (node.getName().equals(name) && node.isNodeType(type)) { // (hasMixin(node, type) || node.isNodeType(type))) {
            return node;
        }
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            Node nextNode = iter.nextNode();
            // String nextNodeName = nextNode.getName();
            // boolean isNodeType = nextNode.isNodeType(type);
            if (nextNode.getName().equals(name) && nextNode.isNodeType(type)) { // nextNodeName.equals(name) && isNodeType) {
                // //(hasMixin(node, type) ||
                // node.isNodeType(type))) {
                return nextNode;
            }
            Node someNode = findNode(nextNode, name, type);
            if (someNode != null) {
                return someNode;
            }
        }

        return null;
    }

    protected void printPropertiesRecursive( Node node ) throws RepositoryException, PathNotFoundException, ValueFormatException {
        printProperties(node);

        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            printPropertiesRecursive(iter.nextNode());
        }

    }

    protected void printChildProperties( Node node ) throws RepositoryException, PathNotFoundException, ValueFormatException {
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            printProperties(iter.nextNode());
        }

    }

    /**
     * Load the subgraph below this node, and print it to System.out if printing is enabled.
     * 
     * @param node the root of the subgraph
     * @throws RepositoryException
     */
    protected void printSubgraph( Node node ) throws RepositoryException {
        printSubgraph(node, Integer.MAX_VALUE);
    }

    /**
     * Load the subgraph below this node, and print it to System.out if printing is enabled.
     * 
     * @param node the root of the subgraph
     * @param maxDepth the maximum depth of the subgraph that should be printed
     * @throws RepositoryException
     */
    protected void printSubgraph( Node node,
                                  int maxDepth ) throws RepositoryException {
        printSubgraph(node, " ", node.getDepth(), maxDepth);
    }

    /**
     * Print this node and its properties to System.out if printing is enabled.
     * 
     * @param node the node to be printed
     * @throws RepositoryException
     */
    protected void printNode( Node node ) throws RepositoryException {
        printSubgraph(node, " ", node.getDepth(), 1);
    }

    /**
     * Load the subgraph below this node, and print it to System.out if printing is enabled.
     * 
     * @param node the root of the subgraph
     * @param lead the string that each line should begin with; may be null if there is no such string
     * @param depthOfSubgraph the depth of this subgraph's root node
     * @param maxDepthOfSubgraph the maximum depth of the subgraph that should be printed
     * @throws RepositoryException
     */
    private void printSubgraph( Node node,
                                String lead,
                                int depthOfSubgraph,
                                int maxDepthOfSubgraph ) throws RepositoryException {
        if (!print) return;
        tools.printSubgraph(node, lead, depthOfSubgraph, maxDepthOfSubgraph);
    }

    protected void printChildren( Node node ) throws RepositoryException {
        if (!print) return;
        System.out.println("Children of \"" + node.getPath() + "\"");
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            Node child = iter.nextNode();
            System.out.println(child.getPath());
        }
    }

    protected void printProperties( Node node ) throws RepositoryException, PathNotFoundException, ValueFormatException {
        if (!print) return;
        printSubgraph(node, " ", node.getDepth(), 1);
    }

    protected void printStart( String fileName,
                               String testMethod ) {
        if (!print) return;
        System.out.println("STARTED:  " + testMethod + "(" + fileName + ")");
    }

    protected void printEnd( String fileName,
                             String testMethod ) {
        if (!print) return;
        System.out.println("ENDED:    " + testMethod + "(" + fileName + ")");
    }

    /**
     * Execute the supplied JCR-SQL2 query and, if printing is enabled, print out the results.
     * 
     * @param jcrSql2 the JCR-SQL2 query
     * @return the results
     * @throws RepositoryException
     */
    protected QueryResult printQuery( String jcrSql2 ) throws RepositoryException {
        return printQuery(jcrSql2, Query.JCR_SQL2, -1, null);
    }

    /**
     * Execute the supplied JCR-SQL2 query and, if printing is enabled, print out the results.
     * 
     * @param jcrSql2 the JCR-SQL2 query
     * @param expectedNumberOfResults the expected number of rows in the results, or -1 if this is not to be checked
     * @param variables the variables for the query
     * @return the results
     * @throws RepositoryException
     */
    protected QueryResult printQuery( String jcrSql2,
                                      long expectedNumberOfResults,
                                      Variable... variables ) throws RepositoryException {
        Map<String, String> keyValuePairs = new HashMap<String, String>();
        for (Variable var : variables) {
            keyValuePairs.put(var.key, var.value);
        }
        return printQuery(jcrSql2, Query.JCR_SQL2, expectedNumberOfResults, keyValuePairs);
    }

    /**
     * Execute the supplied JCR-SQL2 query and, if printing is enabled, print out the results.
     * 
     * @param jcrSql2 the JCR-SQL2 query
     * @param expectedNumberOfResults the expected number of rows in the results, or -1 if this is not to be checked
     * @param variables the array of variable maps for the query; all maps will be combined into a single map
     * @return the results
     * @throws RepositoryException
     */
    protected QueryResult printQuery( String jcrSql2,
                                      long expectedNumberOfResults,
                                      Map<String, String> variables ) throws RepositoryException {
        return printQuery(jcrSql2, Query.JCR_SQL2, expectedNumberOfResults, variables);
    }

    protected QueryResult printQuery( String queryExpression,
                                      String queryLanguage,
                                      long expectedNumberOfResults,
                                      Map<String, String> variables ) throws RepositoryException {
        Session session = session();
        QueryResult results = null;
        for (int i = 0; i != 10; ++i) {
            Query query = session.getWorkspace().getQueryManager().createQuery(queryExpression, queryLanguage);
            if (variables != null && !variables.isEmpty()) {
                for (Map.Entry<String, String> entry : variables.entrySet()) {
                    String key = entry.getKey();
                    Value value = session.getValueFactory().createValue(entry.getValue());
                    query.bindValue(key, value);
                }
            }
            results = query.execute();
            if (results.getRows().getSize() == expectedNumberOfResults) {
                break;
            }
            // We got a different number of results. It could be that we caught the indexer before it was done indexing
            // the changes, so sleep for a bit and try again ...
            try {
                if (debug) {
                    System.out.println("---> Waiting for query: " + queryExpression
                                       + (variables != null ? " using " + variables : ""));
                }
                Thread.sleep(500);
            } catch (InterruptedException e) {
                fail(e.getMessage());
                return null;
            }
        }
        assertThat(results, is(notNullValue()));
        assert results != null;
        if (expectedNumberOfResults >= 0L) {
            assertThat("Expected different number of rows from '" + queryExpression + "'",
                       results.getRows().getSize(),
                       is(expectedNumberOfResults));
        }
        if (print) {
            System.out.println(queryExpression);
            System.out.println(results);
            System.out.println();
        }
        return results;
    }

    protected Variable var( String key,
                            String value ) {
        return new Variable(key, value);
    }

    protected Map<String, String> vars( String... keyValuePairs ) {
        assertThat(keyValuePairs.length % 2, is(0));
        Map<String, String> map = new HashMap<String, String>();
        for (int i = 0; i != keyValuePairs.length; ++i) {
            String key = keyValuePairs[i];
            String value = keyValuePairs[++i];
            map.put(key, value);
        }
        return map;
    }

    @Immutable
    protected static class Variable {
        protected final String key;
        protected final String value;

        protected Variable( String key,
                            String value ) {
            this.key = key;
            this.value = value;
        }
    }

    protected void repeatedlyWithSession( int times,
                                          Operation operation ) throws Exception {
        for (int i = 0; i != times; ++i) {
            double time = withSession(operation);
            print("Time to execute \"" + operation.getClass().getSimpleName() + "\": " + time + " ms");
        }
    }

    protected void browseTo( String path ) throws Exception {
        double time = 0.0d;
        for (Iterator<Path> iterator = path(path).pathsFromRoot(); iterator.hasNext();) {
            Path p = iterator.next();
            time += withSession(new BrowseContent(string(p)));
        }
        print("Time to browse down to \"" + path + "\": " + time + " ms");
    }

    protected void print( Object msg ) {
        if (print && msg != null) {
            System.out.println(msg.toString());
        }
    }

    protected double withSession( Operation operation ) throws Exception {
        return withSession(operation, true);
    }

    protected double withSession( Operation operation,
                                  boolean useSeparateSessions ) throws Exception {
        long startTime = System.nanoTime();
        Session oldSession = session();
        Session session = useSeparateSessions ? repository().login() : oldSession;
        try {
            operation.run(session);
        } finally {
            if (oldSession != null) setSession(oldSession);
            if (oldSession != session) session.logout();
        }
        return TimeUnit.MILLISECONDS.convert(System.nanoTime() - startTime, TimeUnit.NANOSECONDS);
    }

    protected interface Operation {
        public void run( Session session ) throws Exception;
    }

    protected abstract class BasicOperation implements Operation {
        protected Node assertNode( Session session,
                                   String path,
                                   String primaryType,
                                   String... mixinTypes ) throws RepositoryException {
            Node node = session.getNode(path);
            assertThat(node.getPrimaryNodeType().getName(), is(primaryType));
            Set<String> expectedMixinTypes = new HashSet<String>(Arrays.asList(mixinTypes));
            Set<String> actualMixinTypes = new HashSet<String>();
            for (NodeType mixin : node.getMixinNodeTypes()) {
                actualMixinTypes.add(mixin.getName());
            }
            assertThat("Mixin types do not match", actualMixinTypes, is(expectedMixinTypes));
            return node;
        }
    }

    protected class BrowseContent extends BasicOperation {
        private String path;

        public BrowseContent( String path ) {
            this.path = path;
        }

        @Override
        public void run( Session s ) throws RepositoryException {
            // Verify the file was imported ...
            Node node = s.getNode(path);
            assertThat(node, is(notNullValue()));
        }

    }

    protected class CountNodes extends BasicOperation {
        @Override
        public void run( Session s ) throws RepositoryException {
            // Count the nodes below the root, excluding the '/jcr:system' branch ...
            String queryStr = "SELECT [jcr:primaryType] FROM [nt:base]";
            Query query = s.getWorkspace().getQueryManager().createQuery(queryStr, Query.JCR_SQL2);
            long numNonSystemNodes = query.execute().getRows().getSize();
            print("  # nodes NOT in '/jcr:system' branch: " + numNonSystemNodes);
        }
    }

    protected class PrintNodes extends BasicOperation {
        @Override
        public void run( Session s ) throws RepositoryException {
            // Count the nodes below the root, excluding the '/jcr:system' branch ...
            String queryStr = "SELECT [jcr:path] FROM [nt:base] ORDER BY [jcr:path]";
            Query query = s.getWorkspace().getQueryManager().createQuery(queryStr, Query.JCR_SQL2);
            print(query.execute());
        }
    }

}
