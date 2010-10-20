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
package org.modeshape.test.integration.sequencer.ddl.dialect.derby;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.connector.inmemory.InMemoryRepositorySource;
import org.modeshape.jcr.JcrConfiguration;
import org.modeshape.jcr.JcrSecurityContextCredentials;
import org.modeshape.jcr.JcrTools;
import org.modeshape.sequencer.ddl.StandardDdlLexicon;
import org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon;
import org.modeshape.test.integration.sequencer.ddl.DdlIntegrationTestUtil;

/**
 * @author blafond
 */
public class DerbyDdlSequencerIntegrationTest extends DdlIntegrationTestUtil {
    private String resourceFolder = ddlTestResourceRootFolder + "/dialect/derby/";

    @Before
    public void beforeEach() throws Exception {
        // Configure the ModeShape configuration. This could be done by loading a configuration from a file, or by
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
        // Set up the JCR repository to use the source ..protected.
        config.repository(repositoryName)
              .addNodeTypes(getUrl("org/modeshape/sequencer/ddl/StandardDdl.cnd"))
              .addNodeTypes(getUrl(resourceFolder + "DerbyDdl.cnd"))
              .registerNamespace(StandardDdlLexicon.Namespace.PREFIX, StandardDdlLexicon.Namespace.URI)
              .registerNamespace(DerbyDdlLexicon.Namespace.PREFIX, DerbyDdlLexicon.Namespace.URI)
              .setSource(repositorySource);
        // Set up the DDL sequencer ...
        config.sequencer("DDL Sequencer")
              .usingClass("org.modeshape.sequencer.ddl.DdlSequencer")
              .loadedFromClasspath()
              .setDescription("Sequences DDL files to extract individual statements and accompanying statement properties and values")
              .sequencingFrom("(//(*.(ddl)[*]))/jcr:content[@jcr:data]")
              .andOutputtingTo("/ddls/$1");
        config.save();
        this.engine = config.build();
        this.engine.start();

        this.session = this.engine.getRepository(repositoryName)
                                  .login(new JcrSecurityContextCredentials(new MyCustomSecurityContext()), workspaceName);

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
    public void shouldSequenceDerbyDdlFile() throws Exception {

        uploadFile(resourceFolder, "derby_test_statements.ddl", "shouldSequenceDerbyDdlFile");

        waitUntilSequencedNodesIs(1);

        // Find the node ...
        Node root = session.getRootNode();

        if (root.hasNode("ddls")) {
            if (root.hasNode("ddls")) {
                Node ddlsNode = root.getNode("ddls/a/b");
                // System.out.println("   | NAME: " + ddlsNode.getName() + "  PATH: " + ddlsNode.getPath());
                for (NodeIterator iter = ddlsNode.getNodes(); iter.hasNext();) {
                    Node ddlNode = iter.nextNode();

                    long numStatements = ddlNode.getNodes().nextNode().getNodes().getSize();
                    assertEquals(numStatements, 64);

                    // printNodeProperties(ddlNode);

                    verifyNode(ddlNode, "HOTELAVAILABILITY", "ddl:startLineNumber");
                    verifyNode(ddlNode, "SAMP.DEPARTMENT", "ddl:expression");
                    verifyNode(ddlNode, "HOTEL_ID", "ddl:datatypeName");
                    verifyNode(ddlNode, "CITIES", "ddl:startLineNumber");

                    // Create Function
                    verifyNode(ddlNode, "PROPERTY_FILE_READER", "ddl:startLineNumber", 71);
                    verifyNodeTypes(ddlNode,
                                    "PROPERTY_FILE_READER",
                                    "derbyddl:createFunctionStatement",
                                    "ddl:creatable",
                                    "derbyddl:functionOperand");
                    verifyNode(ddlNode, "KEY_COL", "ddl:datatypeName", "VARCHAR");

                    Node functionNode = findNode(ddlNode, "TO_DEGREES");
                    assertNotNull(functionNode);
                    verifyChildNode(functionNode, "parameterStyle", "ddl:value", "PARAMETER STYLE JAVA");

                    // Create Index
                    // CREATE INDEX IXSALE ON SAMP.SALES (SALES);
                    Node indexNode = findNode(ddlNode, "IXSALE", "derbyddl:createIndexStatement");
                    assertNotNull(indexNode);
                    verifySimpleStringProperty(indexNode, "derbyddl:tableName", "SAMP.SALES");
                    Node colRefNode = findNode(indexNode, "SALES");
                    assertNotNull(colRefNode);
                    colRefNode = findNode(ddlNode, "SALES", "derbyddl:indexColumnReference");
                    assertNotNull(colRefNode);
                    verifyNodeTypes(colRefNode,
                                    "SALES",
                                    "derbyddl:indexColumnReference",
                                    "ddl:columnReference",
                                    "ddl:referenceOperand");

                    // declare global temporary table SESSION.t1(c11 int) not logged;
                    Node ttNode = findNode(ddlNode, "SESSION.t1", "derbyddl:declareGlobalTemporaryTableStatement");
                    assertNotNull(ttNode);
                    Node colNode = findNode(ttNode, "c11");
                    assertNotNull(colNode);
                    verifySimpleStringProperty(colNode, "ddl:datatypeName", "int");

                    // LOCK TABLE FlightAvailability IN EXCLUSIVE MODE;
                    Node lockNode = findNode(ddlNode, "FlightAvailability", "derbyddl:lockTableStatement");
                    assertNotNull(lockNode);
                    Node optionNode = findNode(lockNode, "lockMode");
                    assertNotNull(optionNode);
                    verifySimpleStringProperty(optionNode, "ddl:value", "EXCLUSIVE");

                    // RENAME TABLE SAMP.EMP_ACT TO EMPLOYEE_ACT
                    Node renameTableNode = findNode(ddlNode, "SAMP.EMP_ACT", "derbyddl:renameTableStatement");
                    assertNotNull(renameTableNode);
                    verifySimpleStringProperty(renameTableNode, "ddl:newName", "EMPLOYEE_ACT");

                    // CREATE SYNONYM SAMP.T1 FOR SAMP.TABLEWITHLONGNAME;
                    Node synonymNode = findNode(ddlNode, "SAMP.T1", "derbyddl:createSynonymStatement");
                    assertNotNull(synonymNode);
                    verifySimpleStringProperty(synonymNode, "derbyddl:tableName", "SAMP.TABLEWITHLONGNAME");

                    // CREATE TRIGGER FLIGHTSDELETE3
                    // AFTER DELETE ON FLIGHTS
                    // REFERENCING OLD AS OLD
                    // FOR EACH ROW
                    // DELETE FROM FLIGHTAVAILABILITY WHERE FLIGHT_ID = OLD.FLIGHT_ID;
                    Node triggerNode = findNode(ddlNode, "FLIGHTSDELETE3", "derbyddl:createTriggerStatement");
                    assertNotNull(triggerNode);
                    verifySimpleStringProperty(triggerNode, "derbyddl:tableName", "FLIGHTS");

                    // CREATE TRIGGER t1 NO CASCADE BEFORE UPDATE ON x
                    // FOR EACH ROW MODE DB2SQL
                    // values app.notifyEmail('Jerry', 'Table x is about to be updated');
                    triggerNode = findNode(ddlNode, "t1", "derbyddl:createTriggerStatement");
                    assertNotNull(triggerNode);
                    verifySimpleStringProperty(triggerNode, "derbyddl:tableName", "x");
                    optionNode = findNode(triggerNode, "forEach");
                    assertNotNull(optionNode);
                    verifySimpleStringProperty(optionNode, "ddl:value", "FOR EACH ROW");
                    optionNode = findNode(triggerNode, "eventType");
                    assertNotNull(optionNode);
                    verifySimpleStringProperty(optionNode, "ddl:value", "UPDATE");

                    // GRANT EXECUTE ON PROCEDURE p TO george;
                    Node grantNode = findNode(ddlNode, "p", "derbyddl:grantOnProcedureStatement");
                    assertNotNull(grantNode);

                    // GRANT purchases_reader_role TO george,maria;
                    grantNode = findNode(ddlNode, "grantRoles", "derbyddl:grantRolesStatement");
                    assertNotNull(grantNode);
                    Node roleNode = findNode(grantNode, "george", "ddl:grantee");
                    assertNotNull(roleNode);

                }
            }
        }
    }
}
