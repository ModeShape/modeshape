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
import static org.junit.Assert.assertNotNull;
import java.net.URL;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import org.jboss.dna.graph.connector.inmemory.InMemoryRepositorySource;
import org.jboss.dna.jcr.JcrConfiguration;
import org.jboss.dna.jcr.JcrTools;
import org.jboss.dna.jcr.SecurityContextCredentials;
import org.jboss.dna.sequencer.ddl.StandardDdlLexicon;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 *
 */
public class DdlSequencerIntegrationTest extends DdlIntegrationTestUtil {
//    private JcrEngine engine;
//    private Session session;
//    private JcrTools tools;
//    private static final String cndDdlFolder = "org/jboss/dna/test/integration/sequencer/ddl/";
    
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
            .addNodeTypes(getUrl(ddlTestResourceRootFolder + "StandardDdl.cnd"))
            .registerNamespace(StandardDdlLexicon.Namespace.PREFIX, StandardDdlLexicon.Namespace.URI)
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
    
    @Test
    public void shouldSequenceCreateSchemaDdlFile() throws Exception {
        System.out.println("STARTED:  shouldSequenceCreateSchemaDdlFile(create_schema.ddl)");
        URL url = getUrl(ddlTestResourceRootFolder + "create_schema.ddl");
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
    public void shouldSequenceStandardDdlFile() throws Exception {
        System.out.println("STARTED:  shouldSequenceStandardDdlFile(standard_test_statements.ddl)");
        URL url = getUrl(ddlTestResourceRootFolder + "standard_test_statements.ddl");
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
                    
                    long numStatements = ddlNode.getNodes().nextNode().getNodes().getSize();
                    assertEquals(numStatements, 7);
                }
            }
        }
        
        System.out.println("FINISHED:  shouldSequenceStandardDdlFile(standard_test_statements.ddl)");
    }
    
    @Test
    public void shouldSequenceStandardDdlGrantStatements() throws Exception {
        System.out.println("STARTED:  shouldSequenceStandardDdlGrantStatements(grant_test_statements.ddl)");
        URL url = getUrl(ddlTestResourceRootFolder + "grant_test_statements.ddl");
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
                    
                    long numStatements = ddlNode.getNodes().nextNode().getNodes().getSize();
                    assertEquals(numStatements, 4);
                    
                    //GRANT SELECT ON TABLE purchaseOrders TO maria,harry;
                    Node grantNode = findNode(ddlNode, "purchaseOrders", "ddl:grantOnTableStatement");
                    assertNotNull(grantNode);
                    Node granteeNode = findNode(grantNode, "maria", "ddl:grantee");
                    assertNotNull(granteeNode);
                    Node privNode = findNode(grantNode, "privilege", "ddl:grantPrivilege");
                    assertNotNull(privNode);
                    verifySingleValueProperty(privNode, "ddl:type", "SELECT");
                    
                    //GRANT UPDATE, USAGE ON TABLE purchaseOrders FROM anita,zhi;
                    grantNode = findNode(ddlNode, "billedOrders", "ddl:grantOnTableStatement");
                    assertNotNull(grantNode);
                    privNode = findNode(grantNode, "privilege", "ddl:grantPrivilege");
                    assertNotNull(privNode);
                    verifySingleValueProperty(privNode, "ddl:type", "UPDATE");
                    granteeNode = findNode(grantNode, "anita", "ddl:grantee");
                    assertNotNull(granteeNode);
                }
            }
        }
        
        System.out.println("FINISHED:  shouldSequenceStandardDdlGrantStatements(grant_test_statements.ddl)");
    }
    
    @Test
    public void shouldSequenceStandardDdlRevokeStatements() throws Exception {
        System.out.println("STARTED:  shouldSequenceStandardDdlRevokeStatements(revoke_test_statements.ddl)");
        URL url = getUrl(ddlTestResourceRootFolder + "revoke_test_statements.ddl");
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
                    
                    long numStatements = ddlNode.getNodes().nextNode().getNodes().getSize();
                    assertEquals(numStatements, 4);
                    
                    //REVOKE SELECT ON TABLE purchaseOrders FROM maria,harry;
                    Node revokeNode = findNode(ddlNode, "purchaseOrders", "ddl:revokeOnTableStatement");
                    assertNotNull(revokeNode);
                    Node granteeNode = findNode(revokeNode, "maria", "ddl:grantee");
                    assertNotNull(granteeNode);
                    Node privNode = findNode(revokeNode, "privilege", "ddl:grantPrivilege");
                    assertNotNull(privNode);
                    verifySingleValueProperty(privNode, "ddl:type", "SELECT");
                    
                    //REVOKE UPDATE, USAGE ON TABLE purchaseOrders FROM anita,zhi CASCADE;
                    revokeNode = findNode(ddlNode, "orderDetails", "ddl:revokeOnTableStatement");
                    assertNotNull(revokeNode);
                    privNode = findNode(revokeNode, "privilege", "ddl:grantPrivilege");
                    assertNotNull(privNode);
                    verifySingleValueProperty(privNode, "ddl:type", "UPDATE");
                    granteeNode = findNode(revokeNode, "anita", "ddl:grantee");
                    assertNotNull(granteeNode);
                }
            }
        }
        
        System.out.println("FINISHED:  shouldSequenceStandardDdlRevokeStatements(revoke_test_statements.ddl)");
    }

}
