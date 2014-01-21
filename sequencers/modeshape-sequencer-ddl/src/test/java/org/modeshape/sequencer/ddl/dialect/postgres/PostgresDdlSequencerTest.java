/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.sequencer.ddl.dialect.postgres;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.modeshape.jcr.api.JcrConstants.NT_UNSTRUCTURED;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DATATYPE_NAME;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DDL_START_CHAR_INDEX;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DDL_START_LINE_NUMBER;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.NEW_NAME;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.PARSER_ID;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_UNKNOWN_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.COMMENT;
import static org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_TABLE_STATEMENT_POSTGRES;
import static org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_COMMENT_ON_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_FUNCTION_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_RULE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_SERVER_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_FUNCTION_PARAMETER;
import static org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_GRANT_ON_FUNCTION_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_RENAME_COLUMN;
import javax.jcr.Node;
import org.junit.Test;
import org.modeshape.sequencer.ddl.AbstractDdlSequencerTest;

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
        verifyProperty(node, DDL_START_CHAR_INDEX, 7616);

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
