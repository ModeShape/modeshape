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
 * Lesser General Public License for more details
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org
 */
package org.modeshape.sequencer.ddl.dialect.postgres;

import javax.jcr.Node;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import static org.modeshape.jcr.api.JcrConstants.NT_UNSTRUCTURED;
import org.modeshape.sequencer.ddl.AbstractDdlSequencerTest;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.*;
import static org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.*;

/**
 * Unit test for the {@link org.modeshape.sequencer.ddl.DdlSequencer} when Postgres dialects are parsed.
 *
 * @author Horia Chiorean
 */
public class PostgresDdlSequencerTest extends AbstractDdlSequencerTest {

    @Test
    public void shouldSequencePostgresDdlFile() throws Exception {
        Node statementsNode = sequenceDdl("ddl/dialect/postgres/postgres_test_statements.ddl");
        assertThat(statementsNode.getNodes().getSize(), is(106l));

        Node node = findNode(statementsNode, "increment", TYPE_CREATE_FUNCTION_STATEMENT);
        verifyProperty(node, DDL_START_LINE_NUMBER, 214);
        verifyProperty(node, DDL_START_CHAR_INDEX, 7604);

        // COMMENT ON FUNCTION my_function (timestamp) IS ’Returns Roman Numeral’;
        node = findNode(statementsNode, "my_function", TYPE_COMMENT_ON_STATEMENT);
        verifyProperty(node, DDL_START_LINE_NUMBER, 44);
        verifyProperty(node, DDL_START_CHAR_INDEX, 1573);
        verifyProperty(node, COMMENT, "'Returns Roman Numeral'");

        // ALTER TABLE foreign_companies RENAME COLUMN address TO city;
        Node alterTableNode = findNode(statementsNode, "foreign_companies", TYPE_ALTER_TABLE_STATEMENT_POSTGRES);
        Node renameColNode = findNode(alterTableNode, "address", TYPE_RENAME_COLUMN);
        verifyProperty(renameColNode, NEW_NAME, "city");

        // GRANT EXECUTE ON FUNCTION divideByTwo(numerator int, IN demoninator int) TO george;
        Node grantNode = findNode(statementsNode, "divideByTwo", TYPE_GRANT_ON_FUNCTION_STATEMENT);
        Node parameter_1 = findNode(grantNode, "numerator", TYPE_FUNCTION_PARAMETER);
        assertNotNull(parameter_1);
        verifyProperty(parameter_1, DATATYPE_NAME, "int");
    }
    
    @Test
    public void shouldSequenceStatementsWithDoubleQuotes() throws Exception {    
        Node statementsNode = sequenceDdl("ddl/d_quoted_statements.ddl");
        assertThat(statementsNode.getNodes().getSize(), is(3l));

        verifyPrimaryType(statementsNode, NT_UNSTRUCTURED);
        verifyProperty(statementsNode, PARSER_ID, "POSTGRES");

        Node firstNode = statementsNode.getNode("unknownStatement");
        assertNotNull(firstNode);
        verifyBaseProperties(firstNode, NT_UNSTRUCTURED, "1", "1", "0", 0);
        verifyMixinType(firstNode, TYPE_UNKNOWN_STATEMENT);

        Node serverNode = statementsNode.getNode("CREATE SERVER");
        assertNotNull(serverNode);
        verifyBaseProperties(serverNode, NT_UNSTRUCTURED, "5", "1", "93", 0);
        verifyMixinType(serverNode, TYPE_CREATE_SERVER_STATEMENT);

        Node ruleNode = statementsNode.getNode("_RETURN");
        assertNotNull(ruleNode);
        verifyBaseProperties(ruleNode, NT_UNSTRUCTURED, "7", "1", "144", 0);
        verifyMixinType(ruleNode, TYPE_CREATE_RULE_STATEMENT);
    }
}
