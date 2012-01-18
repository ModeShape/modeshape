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
package org.modeshape.test.integration.sequencer.ddl;

import javax.jcr.*;
import javax.jcr.nodetype.NodeType;
import static org.hamcrest.core.Is.is;
import org.junit.After;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.BeforeClass;
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.jcr.JaasTestUtil;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrTools;
import org.modeshape.repository.sequencer.SequencingService;
import org.modeshape.sequencer.ddl.StandardDdlLexicon;
import org.modeshape.test.integration.sequencer.AbstractSequencerTest;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

/**
 * Base class for the {@link org.modeshape.sequencer.ddl.DdlSequencer} integration tests
 *
 * @author  ?
 * @author Horia Chiorean
 */
public abstract class AbstractDdlIntegrationTest {

    protected static final String DDL_TEST_RESOURCE_ROOT_FOLDER = "org/modeshape/test/integration/sequencer/ddl/";
    protected static final String DEFAULT_REPOSITORY_NAME = "ddlRepository";
    protected static final String DEFAULT_DDL_SEQUENCER = "DDL Sequencer";

    private static final String DEFAULT_WORKSPACE_NAME = "default";
    private static final String ROOT_PATH = "/a/b/";

    protected Session session;
    protected JcrEngine engine;
    protected JcrConfiguration config;
    protected boolean print = false;

    private JcrTools tools;

    @BeforeClass
    public static void beforeAll() {
        // Initialize the JAAS configuration to allow for an admin login later
        JaasTestUtil.initJaas("security/jaas.conf.xml");
    }

    @AfterClass
    public static void afterAll() {
        JaasTestUtil.releaseJaas();
    }

    @Before
    public void beforeEach() throws Exception {
        print = false;
        tools = new JcrTools();
        createDefaultConfig();
        startEngine();
    }

    private JcrConfiguration createDefaultConfig() {
        String repositorySource = "ddlRepositorySource";

        config = new JcrConfiguration();
        // Set up the in-memory source where we'll upload the content and where the sequenced output will be stored ...
        config.repositorySource(repositorySource)
                .usingClass(InMemoryRepositorySource.class)
                .setDescription("The repository for our content")
                .setProperty("defaultWorkspaceName", DEFAULT_WORKSPACE_NAME);
        // Set up the JCR repository to use the source ...
        config.repository(DEFAULT_REPOSITORY_NAME)
                .setSource(repositorySource)
                .addNodeTypes(getUrl("org/modeshape/sequencer/ddl/StandardDdl.cnd"))
                .registerNamespace(StandardDdlLexicon.Namespace.PREFIX, StandardDdlLexicon.Namespace.URI);

        // Set up the DDL sequencer ...
        config.sequencer(DEFAULT_DDL_SEQUENCER)
                .usingClass("org.modeshape.sequencer.ddl.DdlSequencer")
                .loadedFromClasspath()
                .setDescription("Sequences DDL files to extract individual statements and accompanying statement properties and values")
                .sequencingFrom("(//(*.(ddl)[*]))/jcr:content[@jcr:data]")
                .andOutputtingTo("/ddls/$1");
        addCustomConfiguration();
        config.save();
        return config;
    }

    protected void startEngine() throws RepositoryException {
        this.engine = config.build();
        this.engine.start();
        this.session = this.engine.getRepository(DEFAULT_REPOSITORY_NAME).login(
                new SimpleCredentials("superuser", "superuser".toCharArray()), DEFAULT_WORKSPACE_NAME);
    }

    @After
    public void afterEach() throws Exception {
        if (this.session != null) {
            this.session.logout();
        }
        if (this.engine != null) {
            this.engine.shutdown();
        }
    }

    protected void addCustomConfiguration() {
        //nothing by default
    }
    
    protected URL getUrl( String urlStr ) {
        return this.getClass().getClassLoader().getResource(urlStr);
    }

    protected void uploadFile( String folder, String fileName ) throws RepositoryException, IOException {
        URL url = getUrl(folder + fileName);
        uploadFile(url);
    }

    private void uploadFile( URL url ) throws RepositoryException, IOException {
        // Grab the last segment of the URL path, using it as the filename
        String filename = url.getPath().replaceAll("([^/]*/)*", "");
        String nodePath = ROOT_PATH + filename;
        tools.uploadFile(session, nodePath, url);
        session.save();
    }

    /**
     * Get the sequencing statistics.
     *
     * @return the statistics; never null
     */
    private SequencingService.Statistics getStatistics() {
        return this.engine.getSequencingService().getStatistics();
    }

    protected void waitUntilSequencedNodesIs( int totalNumberOfNodesSequenced ) throws InterruptedException {
        // check 50 times, waiting 0.1 seconds between (for a total of 5 seconds max) ...
        waitUntilSequencedNodesIs(totalNumberOfNodesSequenced, 5);
    }

    private void waitUntilSequencedNodesIs( int totalNumberOfNodesSequenced, int numberOfSeconds ) throws InterruptedException {
        long numFound = 0;
        int numberOfMillis = numberOfSeconds * 1000;
        int numberOfIterations = numberOfMillis / 100;
        for (int i = 0; i != numberOfIterations; i++) {
            numFound = getStatistics().getNumberOfNodesSequenced();
            if (numFound >= totalNumberOfNodesSequenced) {
                return;
            }
            Thread.sleep(100);
        }
        fail("Expected to find " + totalNumberOfNodesSequenced + " nodes sequenced, but found " + numFound);
    }

    protected void verifyChildNode( Node parentNode, String childNodeName, String propName, String expectedValue ) throws Exception {
        // Find child node
        Node childNode = null;
        for (NodeIterator iter = parentNode.getNodes(); iter.hasNext(); ) {
            Node nextNode = iter.nextNode();
            if (nextNode.getName().equals(childNodeName)) {
                childNode = nextNode;
                break;
            }
        }
        if (childNode != null) {
            assertThat(childNode.hasProperty(propName), is(true));
            verifySingleValueProperty(childNode, propName, expectedValue);

        } else {
            fail("NODE: " + childNodeName + " not found");
        }

    }

    protected void verifyNode( Node topNode, String name, String propName ) throws Exception {
        Node node = findNode(topNode, name);

        if (node != null) {
            assertThat(node.hasProperty(propName), is(true));
        } else {
            fail("NODE: " + name + " not found");
        }

    }

    protected void verifySimpleStringProperty( Node node, String propName, String expectedValue ) throws Exception {
        assertThat(node.hasProperty(propName), is(true));
        verifySingleValueProperty(node, propName, expectedValue);
    }

    protected void verifyNode( Node topNode, String name, String propName, String expectedValue ) throws Exception {
        Node node = findNode(topNode, name);

        if (node != null) {
            assertThat(node.hasProperty(propName), is(true));
            verifySingleValueProperty(node, propName, expectedValue);

        } else {
            fail("NODE: " + name + " not found");
        }

    }

    protected void verifyNode( Node topNode, String name, String propName, int expectedValue ) throws Exception {
        Node node = findNode(topNode, name);

        if (node != null) {
            assertThat(node.hasProperty(propName), is(true));
            verifySingleValueProperty(node, propName, expectedValue);

        } else {
            fail("NODE: " + name + " not found");
        }

    }

    private Value value( String value ) throws Exception {
        return session.getValueFactory().createValue(value);
    }

    protected void verifySingleValueProperty( Node node, String propNameStr, String expectedValue ) throws Exception {
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

    protected void verifySingleValueProperty( Node node, String propNameStr, int expectedValue ) throws Exception {
        Property prop = node.getProperty(propNameStr);
        Value expValue = session.getValueFactory().createValue(expectedValue);
        Object actualValue = prop.getValue();
        assertThat(expValue, is(actualValue));

    }

    protected void verifyMixin( Node topNode, String nodeName, String nodeType ) throws Exception {
        Node node = findNode(topNode, nodeName);

        if (node != null) {
            verifyMixin(node, nodeType);

        } else {
            fail("NODE: " + nodeName + " not found");
        }
    }

    protected boolean hasMixin( Node node, String nodeType ) throws Exception {
        for (NodeType mixin : node.getMixinNodeTypes()) {
            String mixinName = mixin.getName();
            if (mixinName.equals(nodeType)) {
                return true;
            }
        }
        return false;
    }

    protected void verifyMixin( Node node, String nodeType ) throws Exception {
        boolean foundMixin = hasMixin(node, nodeType);

        assertThat(foundMixin, is(true));
    }

    protected void verifyNodeType( Node topNode, String nodeName, String nodeTypeName ) throws Exception {
        Node node = findNode(topNode, nodeName);

        if (node != null) {
            assertThat(node.isNodeType(nodeTypeName), is(true));
        } else {
            fail("NODE: " + nodeName + " not found");
        }

    }

    protected void verifyNodeTypes( Node topNode, String nodeName, String nodeTypeName, String... moreNodeTypeNames ) throws Exception {
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

    protected Node findNode( Node node, String name ) throws Exception {
        if (node.getName().equals(name)) {
            return node;
        }
        for (NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
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

    protected Node findNode( Node node, String name, String type ) throws Exception {
        if (node.getName().equals(name) && node.isNodeType(type)) { // (hasMixin(node, type) || node.isNodeType(type))) {
            return node;
        }
        for (NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
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

    protected Node assertNode( Node node, String name, String type ) throws Exception {
        Node existingNode = findNode(node, name, type);
        assertNotNull(node);

        return existingNode;
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
    protected void printSubgraph( Node node, int maxDepth ) throws RepositoryException {
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
        if (!print) {
            return;
        }
        int currentDepth = node.getDepth() - depthOfSubgraph + 1;
        if (currentDepth > maxDepthOfSubgraph) {
            return;
        }
        if (lead == null) {
            lead = "";
        }
        String nodeLead = lead + StringUtil.createString(' ', (currentDepth - 1) * 2);

        StringBuilder sb = new StringBuilder();
        sb.append(nodeLead);
        if (node.getDepth() == 0) {
            sb.append("/");
        } else {
            sb.append(node.getName());
            if (node.getIndex() != 1) {
                sb.append('[').append(node.getIndex()).append(']');
            }
        }
        sb.append(" jcr:primaryType=" + node.getPrimaryNodeType().getName());
        boolean referenceable = false;
        if (node.getMixinNodeTypes().length != 0) {
            sb.append(" jcr:mixinTypes=[");
            boolean first = true;
            for (NodeType mixin : node.getMixinNodeTypes()) {
                if (first) {
                    first = false;
                } else {
                    sb.append(',');
                }
                sb.append(mixin.getName());
                if (mixin.getName().equals("mix:referenceable")) {
                    referenceable = true;
                }
            }
            sb.append(']');
        }
        if (referenceable) {
            sb.append(" jcr:uuid=" + node.getIdentifier());
        }
        System.out.println(sb);

        List<String> propertyNames = new LinkedList<String>();
        for (PropertyIterator iter = node.getProperties(); iter.hasNext(); ) {
            Property property = iter.nextProperty();
            String name = property.getName();
            if (name.equals("jcr:primaryType") || name.equals("jcr:mixinTypes") || name.equals("jcr:uuid")) {
                continue;
            }
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
                    if (first) {
                        first = false;
                    } else {
                        sb.append(',');
                    }
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
            for (NodeIterator iter = node.getNodes(); iter.hasNext(); ) {
                Node child = iter.nextNode();
                printSubgraph(child, lead, depthOfSubgraph, maxDepthOfSubgraph);
            }
        }
    }

    protected Node getStatementsContainer() throws RepositoryException {
        Node statementsContainer = session.getRootNode().getNode("ddls" + ROOT_PATH);
        AbstractSequencerTest.SequencedNodeValidator.validateSequencedNodeType(statementsContainer, session.getWorkspace().getNodeTypeManager());
        return statementsContainer;
    }
}
