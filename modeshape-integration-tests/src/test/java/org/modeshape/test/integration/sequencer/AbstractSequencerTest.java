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
package org.modeshape.test.integration.sequencer;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.query.Query;
import javax.jcr.query.QueryResult;
import org.junit.After;
import org.junit.Before;
import org.modeshape.common.util.StringUtil;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrRepository;
import org.modeshape.jcr.JcrTools;
import org.modeshape.repository.sequencer.SequencingService;

/**
 * 
 */
public abstract class AbstractSequencerTest {

    protected URL resourceUrl( String name ) {
        return getClass().getClassLoader().getResource(name);
    }

    protected JcrConfiguration configuration;
    protected JcrEngine engine;
    protected JcrRepository repository;
    protected Session session;
    protected JcrTools tools;
    protected boolean print;

    @Before
    public void beforeEach() throws Exception {
        print = false;
        configuration = new JcrConfiguration().loadFrom(resourceUrl(getResourcePathToConfigurationFile()));
        if (configuration.getProblems().hasErrors()) {
            System.err.println("Error reading in the configuration for " + getClass().getName());
        } else {
            engine = configuration.build();
            if (configuration.getProblems().hasErrors()) {
                System.err.println("Error starting engine for " + getClass().getName());
            } else {
                engine.start();
                repository = engine.getRepository("Content");
                session = repository.login();
            }
        }
        tools = new JcrTools();
    }

    @After
    public void afterEach() {
        configuration = null;

        try {
            if (session != null) {
                session.logout();
            }
        } finally {
            session = null;
            try {
                if (engine != null) {
                    engine.shutdown();
                }
            } finally {
                engine = null;
            }
        }
    }

    /**
     * Return the path to the configuration file that is available on the classpath of this class. Because this class' classloader
     * is used, the resulting path should not include a leading '/'.
     * 
     * @return the configuration file path; may not be null
     */
    protected abstract String getResourcePathToConfigurationFile();

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
        tools.uploadFile(session, nodePath, url);

        // Save the session ...
        session.save();

    }

    /**
     * Get the sequencing statistics.
     * 
     * @return the statistics; never null
     */
    protected SequencingService.Statistics getStatistics() {
        return this.engine.getSequencingService().getStatistics();
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
        NodeType nodeType = session.getWorkspace().getNodeTypeManager().getNodeType(name);
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
            NodeType nodeType = session.getWorkspace().getNodeTypeManager().getNodeType(nodeTypeName);
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
        return session.getNode(path);
    }

    protected Node assertNode( String path,
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
        return session.getValueFactory().createValue(value);
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
        Value expValue = session.getValueFactory().createValue(expectedValue);
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
        int currentDepth = node.getDepth() - depthOfSubgraph + 1;
        if (currentDepth > maxDepthOfSubgraph) return;
        if (lead == null) lead = "";
        String nodeLead = lead + StringUtil.createString(' ', (currentDepth - 1) * 2);

        StringBuilder sb = new StringBuilder();
        sb.append(nodeLead);
        if (node.getDepth() == 0) {
            sb.append("/");
        } else {
            sb.append(node.getName());
        }
        sb.append(" jcr:primaryType=" + node.getPrimaryNodeType().getName());
        boolean referenceable = false;
        if (node.getMixinNodeTypes().length != 0) {
            sb.append(" jcr:mixinTypes=[");
            boolean first = true;
            for (NodeType mixin : node.getMixinNodeTypes()) {
                if (first) first = false;
                else sb.append(',');
                sb.append(mixin.getName());
                if (mixin.getName().equals("mix:referenceable")) referenceable = true;
            }
            sb.append(']');
        }
        if (referenceable) {
            sb.append(" jcr:uuid=" + node.getIdentifier());
        }
        System.out.println(sb);

        List<String> propertyNames = new LinkedList<String>();
        for (PropertyIterator iter = node.getProperties(); iter.hasNext();) {
            Property property = iter.nextProperty();
            String name = property.getName();
            if (name.equals("jcr:primaryType") || name.equals("jcr:mixinTypes") || name.equals("jcr:uuid")) continue;
            propertyNames.add(property.getName());
        }
        Collections.sort(propertyNames);
        for (String propertyName : propertyNames) {
            Property property = node.getProperty(propertyName);
            sb = new StringBuilder();
            sb.append(nodeLead).append("  - ").append(propertyName).append('=');
            boolean binary = property.getType() == PropertyType.BINARY;
            if (property.isMultiple()) {
                sb.append('[');
                boolean first = true;
                for (Value value : property.getValues()) {
                    if (first) first = false;
                    else sb.append(',');
                    if (binary) {
                        sb.append(value.getBinary());
                    } else {
                        sb.append(value.getString());
                    }
                }
                sb.append(']');
            } else {
                Value value = property.getValue();
                if (binary) {
                    sb.append(value.getBinary());
                } else {
                    sb.append(value.getString());
                }
            }
            System.out.println(sb);
        }

        if (currentDepth < maxDepthOfSubgraph) {
            for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
                Node child = iter.nextNode();
                printSubgraph(child, lead, depthOfSubgraph, maxDepthOfSubgraph);
            }
        }
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
        return printQuery(jcrSql2, Query.JCR_SQL2, null, -1);
    }

    /**
     * Execute the supplied JCR-SQL2 query and, if printing is enabled, print out the results.
     * 
     * @param jcrSql2 the JCR-SQL2 query
     * @param expectedNumberOfResults the expected number of rows in the results, or -1 if this is not to be checked
     * @return the results
     * @throws RepositoryException
     */
    protected QueryResult printQuery( String jcrSql2,
                                      long expectedNumberOfResults ) throws RepositoryException {
        return printQuery(jcrSql2, Query.JCR_SQL2, null, expectedNumberOfResults);
    }

    /**
     * Execute the supplied JCR-SQL2 query and, if printing is enabled, print out the results.
     * 
     * @param jcrSql2 the JCR-SQL2 query
     * @param variables the variables for the query
     * @param expectedNumberOfResults the expected number of rows in the results, or -1 if this is not to be checked
     * @return the results
     * @throws RepositoryException
     */
    protected QueryResult printQuery( String jcrSql2,
                                      Map<String, String> variables,
                                      long expectedNumberOfResults ) throws RepositoryException {
        return printQuery(jcrSql2, Query.JCR_SQL2, variables, expectedNumberOfResults);
    }

    protected QueryResult printQuery( String queryExpression,
                                      String queryLanguage,
                                      Map<String, String> variables,
                                      long expectedNumberOfResults ) throws RepositoryException {
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

}
