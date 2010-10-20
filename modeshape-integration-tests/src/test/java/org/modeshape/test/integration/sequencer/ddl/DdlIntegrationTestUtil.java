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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
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
import org.modeshape.common.util.StringUtil;
import org.modeshape.graph.SecurityContext;
import org.modeshape.jcr.JcrEngine;
import org.modeshape.jcr.JcrTools;
import org.modeshape.repository.sequencer.SequencingService;

/**
 *
 */
public class DdlIntegrationTestUtil {
    public JcrEngine engine;
    public Session session;
    public JcrTools tools;
    public static final String ddlTestResourceRootFolder = "org/modeshape/test/integration/sequencer/ddl/";
    protected boolean print = false;

    protected URL getUrl( String urlStr ) {
        return this.getClass().getClassLoader().getResource(urlStr);
    }

    public void uploadFile( String folder,
                            String fileName,
                            String testMethod ) throws RepositoryException, IOException {
        // printStart(fileName, testMethod);

        URL url = getUrl(folder + fileName);
        uploadFile(url);
    }

    public void uploadFile( URL url ) throws RepositoryException, IOException {
        // Grab the last segment of the URL path, using it as the filename
        String filename = url.getPath().replaceAll("([^/]*/)*", "");
        String nodePath = "/a/b/" + filename;
        // String mimeType = "ddl";

        // Now use the JCR API to upload the file ...

        // Create the node at the supplied path ...
        // Node node = tools.findOrCreateNode(session.getRootNode(), nodePath, "nt:folder", "nt:file");

        // Upload the file to that node ...
        tools.uploadFile(session, nodePath, url);
        // Node contentNode = tools.findOrCreateChild(node, "jcr:content", "nt:resource");
        // contentNode.setProperty("jcr:mimeType", mimeType);
        // contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
        // Binary binary = session.getValueFactory().createBinary(url.openStream());
        // contentNode.setProperty("jcr:data", binary);

        // Save the session ...
        session.save();

    }

    /**
     * Get the sequencing statistics.
     * 
     * @return the statistics; never null
     */
    public SequencingService.Statistics getStatistics() {
        return this.engine.getSequencingService().getStatistics();
    }

    public void waitUntilSequencedNodesIs( int totalNumberOfNodesSequenced ) throws InterruptedException {
        // check 50 times, waiting 0.1 seconds between (for a total of 5 seconds max) ...
        waitUntilSequencedNodesIs(totalNumberOfNodesSequenced, 5);
    }

    public void waitUntilSequencedNodesIs( int totalNumberOfNodesSequenced,
                                           int numberOfSeconds ) throws InterruptedException {
        long numFound = 0;
        int actualMillis = 0;
        int numberOfMillis = numberOfSeconds * 1000;
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

    public class MyCustomSecurityContext implements SecurityContext {
        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.SecurityContext#getUserName()
         */
        public String getUserName() {
            return "Fred";
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.SecurityContext#hasRole(java.lang.String)
         */
        public boolean hasRole( String roleName ) {
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.SecurityContext#logout()
         */
        public void logout() {
            // do something
        }
    }

    public void verifyChildNode( Node parentNode,
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
            verifySingleValueProperty(childNode, propName, expectedValue);

        } else {
            fail("NODE: " + childNodeName + " not found");
        }

    }

    public void verifyNode( Node topNode,
                            String name,
                            String propName ) throws Exception {
        Node node = findNode(topNode, name);

        if (node != null) {
            assertThat(node.hasProperty(propName), is(true));
        } else {
            fail("NODE: " + name + " not found");
        }

    }

    public void verifySimpleStringProperty( Node node,
                                            String propName,
                                            String expectedValue ) throws Exception {
        assertThat(node.hasProperty(propName), is(true));
        verifySingleValueProperty(node, propName, expectedValue);
    }

    public void verifyNode( Node topNode,
                            String name,
                            String propName,
                            String expectedValue ) throws Exception {
        Node node = findNode(topNode, name);

        if (node != null) {
            assertThat(node.hasProperty(propName), is(true));
            verifySingleValueProperty(node, propName, expectedValue);

        } else {
            fail("NODE: " + name + " not found");
        }

    }

    public void verifyNode( Node topNode,
                            String name,
                            String propName,
                            int expectedValue ) throws Exception {
        Node node = findNode(topNode, name);

        if (node != null) {
            assertThat(node.hasProperty(propName), is(true));
            verifySingleValueProperty(node, propName, expectedValue);

        } else {
            fail("NODE: " + name + " not found");
        }

    }

    protected Value value( String value ) throws Exception {
        return session.getValueFactory().createValue(value);
    }

    public void verifySingleValueProperty( Node node,
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

    public void verifySingleValueProperty( Node node,
                                           String propNameStr,
                                           int expectedValue ) throws Exception {
        Property prop = node.getProperty(propNameStr);
        Value expValue = session.getValueFactory().createValue(expectedValue);
        Object actualValue = prop.getValue();
        assertThat(expValue, is(actualValue));

    }

    public void verifyMixin( Node topNode,
                             String nodeName,
                             String nodeType ) throws Exception {
        Node node = findNode(topNode, nodeName);

        if (node != null) {
            verifyMixin(node, nodeType);

        } else {
            fail("NODE: " + nodeName + " not found");
        }
    }

    public boolean hasMixin( Node node,
                             String nodeType ) throws Exception {
        for (NodeType mixin : node.getMixinNodeTypes()) {
            String mixinName = mixin.getName();
            if (mixinName.equals(nodeType)) {
                return true;
            }
        }
        return false;
    }

    public void verifyMixin( Node node,
                             String nodeType ) throws Exception {
        boolean foundMixin = hasMixin(node, nodeType);

        assertThat(foundMixin, is(true));
    }

    public void verifyNodeType( Node topNode,
                                String nodeName,
                                String nodeTypeName ) throws Exception {
        Node node = findNode(topNode, nodeName);

        if (node != null) {
            assertThat(node.isNodeType(nodeTypeName), is(true));
        } else {
            fail("NODE: " + nodeName + " not found");
        }

    }

    public void verifyNodeTypes( Node topNode,
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

    public Node findNode( Node node,
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

    public Node findNode( Node node,
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

    public Node assertNode( Node node,
                            String name,
                            String type ) throws Exception {
        Node existingNode = findNode(node, name, type);
        assertNotNull(node);

        return existingNode;
    }

    public void printPropertiesRecursive( Node node ) throws RepositoryException, PathNotFoundException, ValueFormatException {
        printProperties(node);

        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            printPropertiesRecursive(iter.nextNode());
        }

    }

    public void printChildProperties( Node node ) throws RepositoryException, PathNotFoundException, ValueFormatException {
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            printProperties(iter.nextNode());
        }

    }

    public void printProperties( Node node ) throws RepositoryException, PathNotFoundException, ValueFormatException {

        System.out.println("\n >>>  NODE PATH: " + node.getPath());
        System.out.println("           NAME: " + node.getName() + "\n");

        // Create a Properties object containing the properties for this node; ignore any children ...
        // Properties props = new PropMyCustomSecurityContexterties();
        for (PropertyIterator propertyIter = node.getProperties(); propertyIter.hasNext();) {
            Property property = propertyIter.nextProperty();
            String name = property.getName();
            String stringValue = null;
            if (property.getDefinition().isMultiple()) {
                StringBuilder sb = new StringBuilder();
                boolean first = true;
                for (Value value : property.getValues()) {
                    if (!first) {
                        sb.append(", ");
                        first = false;
                    }
                    sb.append(value.getString());
                }
                stringValue = sb.toString();
            } else {
                stringValue = property.getValue().getString();
            }
            System.out.println("   | PROP: " + name + "  VALUE: " + stringValue);
            // props.put(name, stringValue);
        }
    }

    public void printStart( String fileName,
                            String testMethod ) {
        System.out.println("STARTED:  " + testMethod + "(" + fileName + ")");
    }

    public void printEnd( String fileName,
                          String testMethod ) {
        System.out.println("ENDED:    " + testMethod + "(" + fileName + ")");
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

}
