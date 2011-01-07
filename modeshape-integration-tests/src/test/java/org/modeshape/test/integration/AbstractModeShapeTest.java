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
package org.modeshape.test.integration;

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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.jcr.ImportUUIDBehavior;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import net.jcip.annotations.Immutable;
import org.junit.After;
import org.junit.Before;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrTools;
import org.modeshape.repository.sequencer.SequencingService;

/**
 * 
 */
public abstract class AbstractModeShapeTest {

    protected static JcrConfiguration configuration;
    protected static JcrEngine engine;
    protected static JcrRepository repository;
    protected Session session;
    protected JcrTools tools;
    protected boolean print;

    @Before
    public void beforeEach() throws Exception {
        print = false;
        tools = new JcrTools();
    }

    @After
    public void afterEach() throws Exception {
    }

    protected void setSession( Session session ) {
        this.session = session;
    }

    protected Session session() {
        return session;
    }

    protected static void startEngine( Class<?> testClass,
                                       String resourcePathToConfigurationFile,
                                       String repositoryName ) throws Exception {
        CheckArg.isNotNull(testClass, "testClass");
        CheckArg.isNotNull(resourcePathToConfigurationFile, "resourcePathToConfigurationFile");
        URL configFile = testClass.getClassLoader().getResource(resourcePathToConfigurationFile);
        if (configFile == null) {
            String msg = "\"" + resourcePathToConfigurationFile + "\" does not reference an existing file";
            System.err.println(msg);
            throw new IllegalArgumentException(msg);
        }
        configuration = new JcrConfiguration().loadFrom(configFile);
        if (configuration.getProblems().hasErrors()) {
            System.err.println("Error reading in the configuration for " + testClass.getName());
        } else {
            engine = configuration.build();
            if (configuration.getProblems().hasErrors()) {
                System.err.println("Error starting engine for " + testClass.getName());
            } else {
                engine.start();
                try {
                    repository = engine.getRepository(repositoryName);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                    throw e;
                }
            }
        }
    }

    protected static void stopEngine() throws Exception {
        configuration = null;
        try {
            if (engine != null) {
                engine.shutdown();
            }
        } finally {
            engine = null;
        }
    }

    protected static void importContent( Class<?> testClass,
                                         String pathToResourceFile ) throws Exception {
        importContent(testClass, pathToResourceFile, null);
    }

    protected static void importContent( Class<?> testClass,
                                         String pathToResourceFile,
                                         String jcrPathToImportUnder ) throws Exception {
        // Use a session to load the contents ...
        Session session = repository.login();
        try {
            InputStream stream = testClass.getClassLoader().getResourceAsStream(pathToResourceFile);
            if (stream == null) {
                String msg = "\"" + pathToResourceFile + "\" does not reference an existing file";
                System.err.println(msg);
                throw new IllegalArgumentException(msg);
            }
            assertNotNull(stream);
            if (jcrPathToImportUnder == null || jcrPathToImportUnder.trim().length() == 0) jcrPathToImportUnder = "/";

            try {
                session.getWorkspace().importXML(jcrPathToImportUnder, stream, ImportUUIDBehavior.IMPORT_UUID_CREATE_NEW);
            } finally {
                stream.close();
            }
            session.save();
        } catch (RuntimeException t) {
            t.printStackTrace();
            throw t;
        } catch (Exception t) {
            t.printStackTrace();
            throw t;
        } finally {
            session.logout();
        }

    }

    protected URL resourceUrl( String name ) {
        return getClass().getClassLoader().getResource(name);
    }

    protected void uploadFile( String resourceFilePath,
                               String parentPath ) throws RepositoryException, IOException {
        uploadFile(resourceUrl(resourceFilePath), parentPath);
    }

    protected void uploadFile( String folder,
                               String fileName,
                               String parentPath ) throws RepositoryException, IOException {
        uploadFile(resourceUrl(folder + fileName), parentPath);
    }

    protected void uploadFile( URL url,
                               String parentPath ) throws RepositoryException, IOException {
        // Grab the last segment of the URL path, using it as the filename
        String filename = url.getPath().replaceAll("([^/]*/)*", "");
        if (!parentPath.startsWith("/")) parentPath = "/" + parentPath;
        if (!parentPath.endsWith("/")) parentPath = parentPath + "/";
        String nodePath = parentPath + filename;

        // Now use the JCR API to upload the file ...
        tools.uploadFile(session(), nodePath, url);

        // Save the session ...
        session().save();

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
        assertThat(nodeType.isAbstract(), is(isAbstract));
        assertThat(nodeType.isMixin(), is(isMixin));
        assertThat(nodeType.isQueryable(), is(isQueryable));
        assertThat(nodeType.hasOrderableChildNodes(), is(hasOrderableChildNodes));
        assertThat(nodeType.getPrimaryItemName(), is(primaryItemName));
        for (int i = 0; i != supertypes.length; ++i) {
            assertThat(nodeType.getDeclaredSupertypes()[i].getName(), is(supertypes[i]));
        }
        assertThat(nodeType.getDeclaredSupertypes().length, is(supertypes.length));
        assertThat(nodeType.getDeclaredChildNodeDefinitions().length, is(numberOfDeclaredChildNodeDefinitions));
        assertThat(nodeType.getDeclaredPropertyDefinitions().length, is(numberOfDeclaredPropertyDefinitions));
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

    protected Node assertNode( String path ) throws RepositoryException {
        return session().getNode(path);
    }

    protected Node assertNode( String path,
                               String primaryType,
                               String... mixinTypes ) throws RepositoryException {
        Node node = session().getNode(path);
        assertThat(node.getPrimaryNodeType().getName(), is(primaryType));
        Set<String> expectedMixinTypes = new HashSet<String>(Arrays.asList(mixinTypes));
        Set<String> actualMixinTypes = new HashSet<String>();
        for (NodeType mixin : node.getMixinNodeTypes()) {
            actualMixinTypes.add(mixin.getName());
        }
        assertThat("Mixin types do not match", actualMixinTypes, is(expectedMixinTypes));
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
        Query query = session.getWorkspace().getQueryManager().createQuery(queryExpression, queryLanguage);
        if (variables != null && !variables.isEmpty()) {
            for (Map.Entry<String, String> entry : variables.entrySet()) {
                String key = entry.getKey();
                Value value = session.getValueFactory().createValue(entry.getValue());
                query.bindValue(key, value);
            }
        }
        QueryResult results = query.execute();
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
}
