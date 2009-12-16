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

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
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
import javax.jcr.nodetype.NodeType;
import org.jboss.dna.graph.SecurityContext;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.jcr.JcrConfiguration;
import org.jboss.dna.jcr.JcrEngine;
import org.jboss.dna.jcr.JcrTools;
import org.jboss.dna.jcr.SecurityContextCredentials;
import org.jboss.dna.repository.sequencer.SequencingService;
import org.jboss.dna.sequencer.ddl.StandardDdlLexicon;
import org.jboss.dna.sequencer.ddl.dialect.derby.DerbyDdlLexicon;
import org.jboss.dna.sequencer.ddl.dialect.oracle.OracleDdlLexicon;
import org.jboss.dna.sequencer.ddl.dialect.postgres.PostgresDdlLexicon;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class DdlSequencerIntegrationTest {
    private JcrEngine engine;
    private Session session;
    private JcrTools tools;
    private static final String cndDdlFolder = "org/jboss/dna/test/integration/sequencer/ddl/";
    
    @Before
    public void beforeEach() throws Exception {
        // Configure the DNA configuration. This could be done by loading a configuration from a file, or by
        // using a (local or remote) configuration repository, or by setting up the configuration programmatically.
        // This test uses the programmatic approach...
        
        tools = new JcrTools();
        
        String repositoryName = "ddlRepository";
        String workspaceName = "default";
        String repositorySource = "ddlRepositorySource";
        
        JcrConfiguration config = new JcrConfiguration();
        // Set up the in-memory source where we'll upload the content and where the sequenced output will be stored ...
        config.repositorySource(repositorySource)
              .usingClass(InMemoryRepositorySource.class)
              .setDescription("The repository for our content")
              .setProperty("defaultWorkspaceName", workspaceName);
        // Set up the JCR repository to use the source ...
        config.repository(repositoryName)
            .addNodeTypes(getUrl(cndDdlFolder + "StandardDdl.cnd"))
            .addNodeTypes(getUrl(cndDdlFolder + "DerbyDdl.cnd"))
            .addNodeTypes(getUrl(cndDdlFolder + "OracleDdl.cnd"))
            .addNodeTypes(getUrl(cndDdlFolder + "PostgresDdl.cnd"))
            .registerNamespace(StandardDdlLexicon.Namespace.PREFIX, StandardDdlLexicon.Namespace.URI)
            .registerNamespace(DerbyDdlLexicon.Namespace.PREFIX, DerbyDdlLexicon.Namespace.URI)
            .registerNamespace(OracleDdlLexicon.Namespace.PREFIX, OracleDdlLexicon.Namespace.URI)
            .registerNamespace(PostgresDdlLexicon.Namespace.PREFIX, PostgresDdlLexicon.Namespace.URI)
            .setSource(repositorySource);
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

    private URL getUrl(String urlStr) {
        return this.getClass().getClassLoader().getResource(urlStr);
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
        URL url = getUrl(cndDdlFolder + "create_schema.ddl");
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
                    
                    //printNodeProperties(ddlNode);
                    
                    verifyNode(ddlNode, "hollywood", "ddl:startLineNumber");
                    verifyNode(ddlNode, "winners", "ddl:expression");
                    verifyNode(ddlNode, "title", "ddl:datatypeLength");
                }
            }
        }
        
        System.out.println("FINISHED:  shouldSequenceCreateSchemaDdlFile(create_schema.ddl)");
    }
    
    @Test
    public void shouldSequenceDerbyDdlFile() throws Exception {
        System.out.println("STARTED:  shouldSequenceDerbyDdlFile(derby_test_statements.ddl)");
        URL url = getUrl(cndDdlFolder + "derby_test_statements.ddl");
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
                    
                    //printNodeProperties(ddlNode);
                    
                    verifyNode(ddlNode, "HOTELAVAILABILITY", "ddl:startLineNumber");
                    verifyNode(ddlNode, "SAMP.DEPARTMENT", "ddl:expression");
                    verifyNode(ddlNode, "HOTEL_ID", "ddl:datatypeName");
                    verifyNode(ddlNode, "CITIES", "ddl:startLineNumber");
                }
            }
        }
        
        System.out.println("FINISHED:  shouldSequenceDerbyDdlFile(derby_test_statements.ddl)");
    }
    
    @Test
    public void shouldSequenceOracleDdlFile() throws Exception {
        System.out.println("STARTED:  shouldSequenceOracleDdlFile(oracle_test_statements.ddl)");
        URL url = getUrl(cndDdlFolder + "oracle_test_statements.ddl");
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
                    assertEquals(numStatements, 50);
                    
                    //printNodeProperties(ddlNode);
                    
                    verifyNode(ddlNode, "address", "ddl:startLineNumber");
                    verifyNode(ddlNode, "cust_orders", "ddl:expression");
                    verifyMixin(ddlNode, "cust_orders", "oracleddl:createIndexStatement");
                    verifyNodeType(ddlNode, "cust_orders", "oracleddl:createIndexStatement");
                    verifyNodeType(ddlNode, "cust_orders", "ddl:creatable");
                    verifyNode(ddlNode, "cust_orders", "ddl:startCharIndex", 1698);
                    verifyNode(ddlNode, "customers_dim", "ddl:startColumnNumber");
                }
            }
        }
        
        System.out.println("FINISHED:  shouldSequenceOracleDdlFile(oracle_test_statements.ddl)");
    }
    
    @Test
    public void shouldSequencePostgresDdlFile() throws Exception {
        System.out.println("STARTED:  shouldSequencePostgresDdlFile(postgres_test_statements.ddl)");
        URL url = getUrl(cndDdlFolder + "postgres_test_statements.ddl");
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
                    assertEquals(numStatements, 101);
                    
                    //printNodeProperties(ddlNode);
                    
                    verifyNodeType(ddlNode, "increment", "postgresddl:createFunctionStatement");
                    verifyNode(ddlNode, "increment", "ddl:expression");
                    verifyNodeType(ddlNode, "increment", "ddl:creatable");
                    verifyNodeType(ddlNode, "increment", "postgresddl:functionOperand");
                    verifyNode(ddlNode, "increment", "ddl:startLineNumber", 214);
                    verifyNode(ddlNode, "increment", "ddl:startCharIndex", 7604);
                    
                    
                    //COMMENT ON FUNCTION my_function (timestamp) IS ’Returns Roman Numeral’;
                    verifyNodeType(ddlNode, "my_function", "postgresddl:commentOnStatement");
                    verifyNode(ddlNode, "my_function", "ddl:expression");
                    verifyNodeType(ddlNode, "my_function", "postgresddl:commentOperand");
                    verifyNode(ddlNode, "my_function", "ddl:startLineNumber", 44);
                    verifyNode(ddlNode, "my_function", "ddl:startCharIndex", 1573);
                    verifyNode(ddlNode, "my_function", "postgresddl:comment", "'Returns Roman Numeral'");
                }
            }
        }
        
        System.out.println("FINISHED:  shouldSequencePostgresDdlFile(postgres_test_statements.ddl)");
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
    
    public void printNodeProperties(Node node) throws Exception {
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
    
    private void verifyNode(Node topNode, String name, String propName, String expectedValue) throws Exception {
        Node node = findNode(topNode, name);
        
        if( node != null ) {
            assertThat( node.hasProperty(propName), is(true));
            verifySingleValueProperty(node, propName, expectedValue);
            
        } else {
            fail("NODE: " + name + " not found");
        }
        
    }
    
    private void verifyNode(Node topNode, String name, String propName, int expectedValue) throws Exception {
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
    
    private void verifySingleValueProperty(Node node, String propNameStr, String expectedValue) throws Exception {
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
    
   
    private void verifySingleValueProperty(Node node, String propNameStr, int expectedValue) throws Exception {
        Property prop = node.getProperty(propNameStr);
        Value expValue = session.getValueFactory().createValue(expectedValue);
        Object actualValue = prop.getValue();
        assertThat(expValue, is(actualValue));
        
    }
    
    private void verifyMixin(Node topNode, String nodeName, String nodeType) throws Exception {
        Node node = findNode(topNode, nodeName);
        
        if( node != null ) {
            verifyMixin(node, nodeType);
            
        } else {
            fail("NODE: " + nodeName + " not found");
        }
    }
    
    private void verifyMixin(Node node, String nodeType) throws Exception {
        boolean foundMixin = false;
        for( NodeType mixin : node.getMixinNodeTypes() ) {
            String mixinName = mixin.getName();
            if( mixinName.equals(nodeType) ) {
                foundMixin = true;
                break;
            }
        }
        
        assertThat(foundMixin, is(true));
    }
    
    private void verifyNodeType(Node topNode, String nodeName, String nodeTypeName) throws Exception {
        Node node = findNode(topNode, nodeName);
        
        if( node != null ) {
            assertThat(node.isNodeType(nodeTypeName), is(true));
        } else {
            fail("NODE: " + nodeName + " not found");
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
        System.out.println("           NAME: " + node.getName() + "\n" );

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
