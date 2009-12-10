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

import static org.junit.Assert.assertEquals;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import java.io.IOException;
import java.net.URL;
import java.util.Calendar;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import javax.jcr.PathNotFoundException;
import javax.jcr.Property;
import javax.jcr.PropertyIterator;
import javax.jcr.RepositoryException;
import javax.jcr.Session;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import org.jboss.dna.graph.SecurityContext;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.jcr.JcrConfiguration;
import org.jboss.dna.jcr.JcrEngine;
import org.jboss.dna.jcr.JcrTools;
import org.jboss.dna.jcr.SecurityContextCredentials;
import org.jboss.dna.repository.sequencer.SequencingService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class DdlSequencerIntegrationTest {
    private JcrEngine engine;
    private Session session;
    private URL cndUrl;
    private JcrTools tools;
    
    @Before
    public void beforeEach() throws Exception {
        // Configure the DNA configuration. This could be done by loading a configuration from a file, or by
        // using a (local or remote) configuration repository, or by setting up the configuration programmatically.
        // This test uses the programmatic approach...
        
        tools = new JcrTools();
        
        String repositoryName = "ddlRepository";
        String workspaceName = "default";
        String repositorySource = "ddlRepositorySource";
        
        cndUrl = this.getClass().getClassLoader().getResource("org/jboss/dna/test/integration/sequencer/ddl/StandardDdl.cnd");
        
        JcrConfiguration config = new JcrConfiguration();
        // Set up the in-memory source where we'll upload the content and where the sequenced output will be stored ...
        config.repositorySource(repositorySource)
              .usingClass(InMemoryRepositorySource.class)
              .setDescription("The repository for our content")
              .setProperty("defaultWorkspaceName", workspaceName);
        // Set up the JCR repository to use the source ...
        config.repository(repositoryName).addNodeTypes(cndUrl).setSource(repositorySource);
        // Set up the DDL sequencer ...
        config.sequencer("DDL Sequencer")
            .usingClass("org.jboss.dna.sequencer.ddl.DdlSequencer")
            .loadedFromClasspath()
            .setDescription("Sequences DDL files to extract individual statements and accompanying statement properties and values")
            .sequencingFrom("//(*.(ddl)[*])/jcr:content[@jcr:data]")
            .andOutputtingTo("/ddls/$1"); 
        config.save();
        this.engine = config.build();
        this.engine.start();

        this.session = this.engine.getRepository(repositoryName)
                                  .login(new SecurityContextCredentials(new MyCustomSecurityContext()), workspaceName);

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
    
    private void uploadFile(URL url) throws RepositoryException, IOException {
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
    
    protected void waitUntilSequencedNodesIs( int totalNumberOfNodesSequenced ) throws InterruptedException {
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

    @Test
    public void shouldSequenceCreateSchemaDdlFile() throws Exception {
        System.out.println("STARTED:  shouldSequenceCreateSchemaDdlFile(create_schema.ddl)");
        URL url = this.getClass().getClassLoader().getResource("org/jboss/dna/test/integration/sequencer/ddl/create_schema.ddl");
        uploadFile(url);
        
        waitUntilSequencedNodesIs(1);
        
        // Find the node ...
        Node root = session.getRootNode();

        if (root.hasNode("ddls") ) {
            if (root.hasNode("ddls")) {
                Node ddlsNode = root.getNode("ddls");
                System.out.println("   | NAME: " + ddlsNode.getName() + "  PATH: " + ddlsNode.getPath());
                for (NodeIterator iter = ddlsNode.getNodes(); iter.hasNext();) {
                    Node ddlNode = iter.nextNode();
                    
                    printNodeProperties(ddlNode);
                    
                    verifyNode(ddlNode, "hollywood", "ns001:startLineNumber");
                    verifyNode(ddlNode, "winners", "ns001:expression");
                    verifyNode(ddlNode, "title", "ns001:datatypeLength");
                }
            }
        }
        
        System.out.println("FINISHED:  shouldSequenceCreateSchemaDdlFile(create_schema.ddl)");
    }
    
    @Test
    public void shouldSequenceDerbyDdlFile() throws Exception {
        System.out.println("STARTED:  shouldSequenceDerbyDdlFile(derby_test_statements.ddl)");
        URL url = this.getClass().getClassLoader().getResource("org/jboss/dna/test/integration/sequencer/ddl/derby_test_statements.ddl");
        uploadFile(url);
        
        waitUntilSequencedNodesIs(1);
        
        // Find the node ...
        Node root = session.getRootNode();

        if (root.hasNode("ddls") ) {
            if (root.hasNode("ddls")) {
                Node ddlsNode = root.getNode("ddls");
                //System.out.println("   | NAME: " + ddlsNode.getName() + "  PATH: " + ddlsNode.getPath());
                for (NodeIterator iter = ddlsNode.getNodes(); iter.hasNext();) {
                    Node ddlNode = iter.nextNode();
                    
                    long numStatements = ddlNode.getNodes().nextNode().getNodes().getSize();
                    assertEquals(numStatements, 64);
                    
                    printNodeProperties(ddlNode);
                    
                    verifyNode(ddlNode, "HOTELAVAILABILITY", "ns001:startLineNumber");
                    verifyNode(ddlNode, "SAMP.DEPARTMENT", "ns001:expression");
                    verifyNode(ddlNode, "HOTEL_ID", "ns001:datatypeName");
                    verifyNode(ddlNode, "CITIES", "ns001:startLineNumber");
                }
            }
        }
        
        System.out.println("FINISHED:  shouldSequenceDerbyDdlFile(derby_test_statements.ddl)");
    }
    
    protected class MyCustomSecurityContext implements SecurityContext {
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
    
    private void printNodeProperties(Node node) throws Exception {
        printProperties(node);
        for (NodeIterator iter = node.getNodes(); iter.hasNext();) { 
            printNodeProperties(iter.nextNode());
        }
        
    }
    
    private void verifyNode(Node topNode, String name, String propName) throws Exception {
        Node node = findNode(topNode, name);
        
        if( node != null ) {
            assertThat( node.hasProperty(propName), is(true));
        } else {
            fail("NODE: " + name + " not found");
        }
        
    }
    
    private Node findNode(Node node, String name) throws Exception  {
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
    
    private void printProperties( Node node ) throws RepositoryException, PathNotFoundException, ValueFormatException {
        
        System.out.println("\n >>>  NODE PATH: " + node.getPath() );
        System.out.println("\n           NAME: " + node.getName() );

        // Create a Properties object containing the properties for this node; ignore any children ...
        //Properties props = new Properties();
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
}
