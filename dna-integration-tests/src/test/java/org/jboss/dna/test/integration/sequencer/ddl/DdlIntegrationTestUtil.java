/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors.
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 * 
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.test.integration.sequencer.ddl;

import static org.junit.Assert.assertNotNull;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.nodetype.NodeType;
import org.jboss.dna.graph.SecurityContext;
import org.jboss.dna.jcr.JcrEngine;
import org.jboss.dna.jcr.JcrTools;
import org.jboss.dna.repository.sequencer.SequencingService;

/**
 *
 */
public class DdlIntegrationTestUtil {
    public JcrEngine engine;
    public Session session;
    public JcrTools tools;
    public static final String ddlTestResourceRootFolder = "org/jboss/dna/test/integration/sequencer/ddl/";
    
    protected URL getUrl(String urlStr) {
        return this.getClass().getClassLoader().getResource(urlStr);
    }
    
    public void uploadFile(String folder, String fileName, String testMethod) throws RepositoryException, IOException {
        //printStart(fileName, testMethod);

        URL url = getUrl(folder + fileName);
        uploadFile(url);
    }
    
    public void uploadFile(URL url) throws RepositoryException, IOException {
        // Grab the last segment of the URL path, using it as the filename
        String filename = url.getPath().replaceAll("([^/]*/)*", "");
        String nodePath = "/a/b/" + filename;
        String mimeType = "ddl";

        // Now use the JCR API to upload the file ...


        // Create the node at the supplied path ...
        Node node = tools.findOrCreateNode(session.getRootNode(), nodePath, "nt:folder", "nt:file");

        // Upload the file to that node ...
        Node contentNode = tools.findOrCreateChild(node, "jcr:content", "nt:resource");
        contentNode.setProperty("jcr:mimeType", mimeType);
        contentNode.setProperty("jcr:lastModified", Calendar.getInstance());
        contentNode.setProperty("jcr:data", url.openStream());

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
        long numFound = 0;
        for (int i = 0; i != 50; i++) {
            numFound = getStatistics().getNumberOfNodesSequenced();
            if (numFound >= totalNumberOfNodesSequenced) {
                return;
            }
            Thread.sleep(1000);
        }
        fail("Expected to find " + totalNumberOfNodesSequenced + " nodes sequenced, but found " + numFound);
    }
    
    
    public class MyCustomSecurityContext implements SecurityContext {
        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.SecurityContext#getUserName()
         */
        public String getUserName() {
            return "Fred";
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.SecurityContext#hasRole(java.lang.String)
         */
        public boolean hasRole( String roleName ) {
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.graph.SecurityContext#logout()
         */
        public void logout() {
            // do something
        }
    }
    
    public void verifyChildNode(Node parentNode, String childNodeName, String propName, String expectedValue) throws Exception {
        // Find child node
        Node childNode = null;
        for (NodeIterator iter = parentNode.getNodes(); iter.hasNext();) {
            Node nextNode = iter.nextNode();
            if( nextNode.getName().equals(childNodeName)) {
                childNode = nextNode;
                break;
            }
        }
        if( childNode != null ) {
            assertThat( childNode.hasProperty(propName), is(true));
            verifySingleValueProperty(childNode, propName, expectedValue);
            
        } else {
            fail("NODE: " + childNodeName + " not found");
        }
        
    }
    
    public void verifyNode(Node topNode, String name, String propName) throws Exception {
        Node node = findNode(topNode, name);
        
        if( node != null ) {
            assertThat( node.hasProperty(propName), is(true));
        } else {
            fail("NODE: " + name + " not found");
        }
        
    }
    
    public void verifySimpleStringProperty(Node node, String propName, String expectedValue) throws Exception {
        assertThat( node.hasProperty(propName), is(true));
        verifySingleValueProperty(node, propName, expectedValue);
    }
    
    public void verifyNode(Node topNode, String name, String propName, String expectedValue) throws Exception {
        Node node = findNode(topNode, name);
        
        if( node != null ) {
            assertThat( node.hasProperty(propName), is(true));
            verifySingleValueProperty(node, propName, expectedValue);
            
        } else {
            fail("NODE: " + name + " not found");
        }
        
    }
    
    public void verifyNode(Node topNode, String name, String propName, int expectedValue) throws Exception {
        Node node = findNode(topNode, name);
        
        if( node != null ) {
            assertThat( node.hasProperty(propName), is(true));
            verifySingleValueProperty(node, propName, expectedValue);
            
        } else {
            fail("NODE: " + name + " not found");
        }
        
    }
    
    protected Value value( String value ) throws Exception {
        return session.getValueFactory().createValue(value);
    }
    
    public void verifySingleValueProperty(Node node, String propNameStr, String expectedValue) throws Exception {
        Value expValue = value(expectedValue);
        Property prop = node.getProperty(propNameStr);
        if( prop.getDefinition().isMultiple()) {
            boolean hasValue = false;
            
            Object[] values = prop.getValues();
            for( Object val : values) {
                if(val.equals(expValue)) {
                    hasValue = true;
                }
            }
            
            assertThat(hasValue, is(true));
        } else {
            Object actualValue = prop.getValue();
            assertThat(expValue, is(actualValue));
        }
        
    }
    
   
    public void verifySingleValueProperty(Node node, String propNameStr, int expectedValue) throws Exception {
        Property prop = node.getProperty(propNameStr);
        Value expValue = session.getValueFactory().createValue(expectedValue);
        Object actualValue = prop.getValue();
        assertThat(expValue, is(actualValue));
        
    }
    
    public void verifyMixin(Node topNode, String nodeName, String nodeType) throws Exception {
        Node node = findNode(topNode, nodeName);
        
        if( node != null ) {
            verifyMixin(node, nodeType);
            
        } else {
            fail("NODE: " + nodeName + " not found");
        }
    }
    
    public boolean hasMixin(Node node, String nodeType) throws Exception { 
        for( NodeType mixin : node.getMixinNodeTypes() ) {
            String mixinName = mixin.getName();
            if( mixinName.equals(nodeType) ) {
                return true;
            }
        }
        return false;
    }
    
    public void verifyMixin(Node node, String nodeType) throws Exception {
        boolean foundMixin = hasMixin(node, nodeType);

        
        assertThat(foundMixin, is(true));
    }
    
    public void verifyNodeType(Node topNode, String nodeName, String nodeTypeName) throws Exception {
        Node node = findNode(topNode, nodeName);
        
        if( node != null ) {
            assertThat(node.isNodeType(nodeTypeName), is(true));
        } else {
            fail("NODE: " + nodeName + " not found");
        }
        
    }
    
    public void verifyNodeTypes(Node topNode, String nodeName, String nodeTypeName, String...moreNodeTypeNames) throws Exception {
        Node node = findNode(topNode, nodeName);
        
        if( node != null ) {
            assertThat(node.isNodeType(nodeTypeName), is(true));
            for( String nextTypeName : moreNodeTypeNames ) {
                assertThat(node.isNodeType(nextTypeName), is(true));
            }
        } else {
            fail("NODE: " + nodeName + " not found");
        }
        
    }
    
    public Node findNode(Node node, String name) throws Exception  {
        if( node.getName().equals(name)) {
            return node;
        }
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            Node nextNode = iter.nextNode();
            if( nextNode.getName().equals(name)) {
                return nextNode;
            }
            Node someNode = findNode(nextNode, name);
            if( someNode != null ) {
                return someNode;
            }
        }
        
        return null;
    }
    
    public Node findNode(Node node, String name, String type) throws Exception  {
        if( node.getName().equals(name) && node.isNodeType(type)) { //(hasMixin(node, type) || node.isNodeType(type))) {
            return node;
        }
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) {
            Node nextNode = iter.nextNode();
            //String nextNodeName = nextNode.getName();
            //boolean isNodeType = nextNode.isNodeType(type);
            if( nextNode.getName().equals(name) && nextNode.isNodeType(type)) { //nextNodeName.equals(name) && isNodeType) { //(hasMixin(node, type) || node.isNodeType(type))) {
                return nextNode;
            }
            Node someNode = findNode(nextNode, name, type);
            if( someNode != null ) {
                return someNode;
            }
        }
        
        return null;
    }
    
    public Node assertNode(Node node, String name, String type) throws Exception {
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
        
        System.out.println("\n >>>  NODE PATH: " + node.getPath() );
        System.out.println("           NAME: " + node.getName() + "\n" );

        // Create a Properties object containing the properties for this node; ignore any children ...
        //Properties props = new PropMyCustomSecurityContexterties();
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
            //props.put(name, stringValue);
        }
    }
    
    public void printStart(String fileName, String testMethod) {
        System.out.println("STARTED:  " + testMethod + "(" + fileName +")");
    }
    
    public void printEnd(String fileName, String testMethod) {
        System.out.println("ENDED:    " + testMethod + "(" + fileName +")");
    }
}
