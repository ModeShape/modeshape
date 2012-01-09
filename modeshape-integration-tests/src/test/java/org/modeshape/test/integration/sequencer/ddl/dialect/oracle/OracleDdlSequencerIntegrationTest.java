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

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import static org.junit.Assert.assertEquals;
import org.junit.Test;
import org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon;
import org.modeshape.test.integration.sequencer.ddl.AbstractDdlIntegrationTest;

/**
 * Integration test for the {@link org.modeshape.sequencer.ddl.DdlSequencer} using an Oracle dialect.
 *
 * @author ?
 * @author Horia Chiorean
 */
public class OracleDdlSequencerIntegrationTest extends AbstractDdlIntegrationTest {
    private static final String RESOURCE_FOLDER = DDL_TEST_RESOURCE_ROOT_FOLDER + "/dialect/oracle/";

    @Override
    protected void addCustomConfiguration() {
        config.repository(DEFAULT_REPOSITORY_NAME)
                .addNodeTypes(getUrl(RESOURCE_FOLDER + "OracleDdl.cnd"))
                .registerNamespace(OracleDdlLexicon.Namespace.PREFIX, OracleDdlLexicon.Namespace.URI);
    }

    @Test
    public void shouldSequenceOracleDdlFile() throws Exception {

        uploadFile(RESOURCE_FOLDER, "oracle_test_statements.ddl");

        waitUntilSequencedNodesIs(1);

        Node ddlsNode = getStatementsContainer();

        for (NodeIterator iter = ddlsNode.getNodes(); iter.hasNext(); ) {
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

    @Test
    public void shouldSequenceOracleCreateProceduresAndFunctions() throws Exception {

        uploadFile(RESOURCE_FOLDER, "create_procedure_statements.ddl");

        waitUntilSequencedNodesIs(1);

        // Find the node ...
        Node ddlsNode = getStatementsContainer();
        for (NodeIterator iter = ddlsNode.getNodes(); iter.hasNext(); ) {
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
