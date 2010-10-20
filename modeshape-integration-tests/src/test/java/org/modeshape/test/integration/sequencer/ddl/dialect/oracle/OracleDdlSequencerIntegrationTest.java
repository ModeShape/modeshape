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
package org.modeshape.test.integration.sequencer.ddl.dialect.oracle;

import static org.junit.Assert.assertEquals;
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
import org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon;
import org.modeshape.test.integration.sequencer.ddl.DdlIntegrationTestUtil;

/**
 *
 */
public class OracleDdlSequencerIntegrationTest extends DdlIntegrationTestUtil {
    private String resourceFolder = ddlTestResourceRootFolder + "/dialect/oracle/";

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
        // Set up the JCR repository to use the source ...
        config.repository(repositoryName)
              .addNodeTypes(getUrl("org/modeshape/sequencer/ddl/StandardDdl.cnd"))
              .addNodeTypes(getUrl(resourceFolder + "OracleDdl.cnd"))
              .registerNamespace(StandardDdlLexicon.Namespace.PREFIX, StandardDdlLexicon.Namespace.URI)
              .registerNamespace(OracleDdlLexicon.Namespace.PREFIX, OracleDdlLexicon.Namespace.URI)
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
    public void shouldSequenceOracleDdlFile() throws Exception {

        uploadFile(resourceFolder, "oracle_test_statements.ddl", "shouldSequenceOracleDdlFile");

        waitUntilSequencedNodesIs(1);

        // Find the node ...
        Node root = session.getRootNode();

        if (root.hasNode("ddls")) {
            Node ddlsNode = root.getNode("ddls/a/b");
            // System.out.println("   | NAME: " + ddlsNode.getName() + "  PATH: " + ddlsNode.getPath());
            for (NodeIterator iter = ddlsNode.getNodes(); iter.hasNext();) {
                Node ddlNode = iter.nextNode();

                long numStatements = ddlNode.getNodes().nextNode().getNodes().getSize();
                assertEquals(numStatements, 50);

                // printNodeProperties(ddlNode);

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

    @Test
    public void shouldSequenceOracleCreateProceduresAndFunctions() throws Exception {

        uploadFile(resourceFolder, "create_procedure_statements.ddl", "shouldSequenceOracleCreateProceduresAndFunctions");

        waitUntilSequencedNodesIs(1);

        // Find the node ...
        Node root = session.getRootNode();

        if (root.hasNode("ddls")) {
            Node ddlsNode = root.getNode("ddls/a/b");
            // System.out.println("   | NAME: " + ddlsNode.getName() + "  PATH: " + ddlsNode.getPath());
            for (NodeIterator iter = ddlsNode.getNodes(); iter.hasNext();) {
                Node ddlNode = iter.nextNode();

                long numStatements = ddlNode.getNodes().nextNode().getNodes().getSize();
                assertEquals(4, numStatements);

                // printNodeProperties(ddlNode);
                // remove_emp <ns001:startLineNumber = 1, jcr:primaryType = nt:unstructured, ns001:startColumnNumber = 1,
                // jcr:mixinTypes = ns002:createProcedureStatement, ns001:expression =
                // CREATE PROCEDURE remove_emp (employee_id NUMBER) AS tot_emps NUMBER;
                // BEGIN
                // DELETE FROM employees
                // WHERE employees.employee_id = remove_emp.employee_id;
                // tot_emps := tot_emps - 1;
                // END;
                // /, ns001:startCharIndex = 0>
                // employee_id <ns001:datatypeName = NUMBER, jcr:primaryType = nt:unstructured, ns001:datatypePrecision = 0,
                // jcr:mixinTypes = ns002:functionParameter, ns001:datatypeScale = 0>

                Node node = assertNode(ddlNode, "remove_emp", "oracleddl:createProcedureStatement");
                assertEquals(1, node.getNodes().getSize());
                Node paramNode = assertNode(node, "employee_id", "oracleddl:functionParameter");
                verifySimpleStringProperty(paramNode, "ddl:datatypeName", "NUMBER");

                // find_root <ns001:startLineNumber = 9, jcr:primaryType = nt:unstructured, ns001:startColumnNumber = 1,
                // jcr:mixinTypes = ns002:createProcedureStatement, ns001:expression =
                // CREATE PROCEDURE find_root ( x IN REAL )
                // IS LANGUAGE C
                // NAME c_find_root
                // LIBRARY c_utils
                // PARAMETERS ( x BY REFERENCE );, ns001:startCharIndex = 211>
                // x <ns001:datatypeName = REAL, jcr:primaryType = nt:unstructured, ns002:inOutNoCopy = IN, jcr:mixinTypes =
                // ns002:functionParameter>
                node = assertNode(ddlNode, "find_root", "oracleddl:createProcedureStatement");
                assertEquals(1, node.getNodes().getSize());
                paramNode = assertNode(node, "x", "oracleddl:functionParameter");
                verifySimpleStringProperty(paramNode, "ddl:datatypeName", "REAL");
                verifySimpleStringProperty(paramNode, "oracleddl:inOutNoCopy", "IN");

                // CREATE FUNCTION SecondMax (input NUMBER) RETURN NUMBER
                // PARALLEL_ENABLE AGGREGATE USING SecondMaxImpl;
                node = assertNode(ddlNode, "SecondMax", "oracleddl:createFunctionStatement");
                assertEquals(1, node.getNodes().getSize());
                paramNode = assertNode(node, "input", "oracleddl:functionParameter");
                verifySimpleStringProperty(paramNode, "ddl:datatypeName", "NUMBER");

                // CREATE OR REPLACE FUNCTION text_length(a CLOB)
                // RETURN NUMBER DETERMINISTIC IS
                // BEGIN
                // RETURN DBMS_LOB.GETLENGTH(a);
                // END;
                // /
                node = assertNode(ddlNode, "text_length", "oracleddl:createFunctionStatement");
                assertEquals(1, node.getNodes().getSize());
                verifySimpleStringProperty(node, "ddl:datatypeName", "NUMBER");
                paramNode = assertNode(node, "a", "oracleddl:functionParameter");
                verifySimpleStringProperty(paramNode, "ddl:datatypeName", "CLOB");
            }
        }
    }
}
