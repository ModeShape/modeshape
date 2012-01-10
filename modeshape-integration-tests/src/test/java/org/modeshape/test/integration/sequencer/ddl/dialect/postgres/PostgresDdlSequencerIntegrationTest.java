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
package org.modeshape.test.integration.sequencer.ddl.dialect.postgres;

import javax.jcr.Node;
import javax.jcr.NodeIterator;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;
import org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlLexicon;
import org.modeshape.test.integration.sequencer.ddl.AbstractDdlIntegrationTest;

/**
 * Integration test for the {@link org.modeshape.sequencer.ddl.DdlSequencer} using a PostgreSQL dialect.
 *
 * @author ?
 * @author Horia Chiorean
 */
public class PostgresDdlSequencerIntegrationTest extends AbstractDdlIntegrationTest {
    private static final String RESOURCE_FOLDER = DDL_TEST_RESOURCE_ROOT_FOLDER + "/dialect/postgres/";

    @Override
    protected void addCustomConfiguration() {
        config.repository(DEFAULT_REPOSITORY_NAME)
                .addNodeTypes(getUrl("org/modeshape/sequencer/ddl/dialect/postgres/PostgresDdl.cnd"))
                .registerNamespace(PostgresDdlLexicon.Namespace.PREFIX, PostgresDdlLexicon.Namespace.URI);
    }

    @Test
    public void shouldSequencePostgresDdlFile() throws Exception {

        uploadFile(RESOURCE_FOLDER, "postgres_test_statements.ddl");

        waitUntilSequencedNodesIs(1);

        // Find the node ...
        Node ddlsNode = getStatementsContainer();
        for (NodeIterator iter = ddlsNode.getNodes(); iter.hasNext(); ) {
            Node ddlNode = iter.nextNode();

            long numStatements = ddlNode.getNodes().nextNode().getNodes().getSize();
            assertEquals(numStatements, 106);

            // printNodeProperties(ddlNode);

            verifyNodeType(ddlNode, "increment", "postgresddl:createFunctionStatement");
            verifyNode(ddlNode, "increment", "ddl:expression");
            verifyNodeType(ddlNode, "increment", "ddl:creatable");
            verifyNodeType(ddlNode, "increment", "postgresddl:functionOperand");
            verifyNode(ddlNode, "increment", "ddl:startLineNumber", 214);
            verifyNode(ddlNode, "increment", "ddl:startCharIndex", 7604);

            // COMMENT ON FUNCTION my_function (timestamp) IS ’Returns Roman Numeral’;
            verifyNodeType(ddlNode, "my_function", "postgresddl:commentOnStatement");
            verifyNode(ddlNode, "my_function", "ddl:expression");
            verifyNodeType(ddlNode, "my_function", "postgresddl:commentOperand");
            verifyNode(ddlNode, "my_function", "ddl:startLineNumber", 44);
            verifyNode(ddlNode, "my_function", "ddl:startCharIndex", 1573);
            verifyNode(ddlNode, "my_function", "postgresddl:comment", "'Returns Roman Numeral'");

            // ALTER TABLE foreign_companies RENAME COLUMN address TO city;
            Node alterTableNode = findNode(ddlNode, "foreign_companies", "postgresddl:alterTableStatement");
            assertNotNull(alterTableNode);
            Node renameColNode = findNode(alterTableNode, "address", "postgresddl:renamedColumn");
            assertNotNull(renameColNode);
            verifySingleValueProperty(renameColNode, "ddl:newName", "city");

            // GRANT EXECUTE ON FUNCTION divideByTwo(numerator int, IN demoninator int) TO george;
            Node grantNode = findNode(ddlNode, "divideByTwo", "postgresddl:grantOnFunctionStatement");
            assertNotNull(grantNode);
            Node parameter_1 = findNode(grantNode, "numerator", "postgresddl:functionParameter");
            assertNotNull(parameter_1);
            verifySingleValueProperty(parameter_1, "ddl:datatypeName", "int");
        }
    }
}
