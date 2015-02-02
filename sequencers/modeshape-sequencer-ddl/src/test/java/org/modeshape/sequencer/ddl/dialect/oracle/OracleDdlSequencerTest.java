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
package org.modeshape.sequencer.ddl.dialect.oracle;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.modeshape.jcr.api.JcrConstants.NT_UNSTRUCTURED;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.CHECK_SEARCH_CONDITION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.CONSTRAINT_TYPE;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DATATYPE_LENGTH;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DATATYPE_NAME;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DATATYPE_PRECISION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DATATYPE_SCALE;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.PARSER_ID;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_ADD_COLUMN_DEFINITION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_ADD_TABLE_CONSTRAINT_DEFINITION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_ALTER_TABLE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon.IN_OUT_NO_COPY;
import static org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon.TYPE_ALTER_USER_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon.TYPE_CREATE_DIRECTORY_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon.TYPE_CREATE_FUNCTION_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon.TYPE_CREATE_PROCEDURE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon.TYPE_FUNCTION_PARAMETER;
import javax.jcr.Node;
import javax.jcr.NodeIterator;
import org.junit.Test;
import org.modeshape.sequencer.ddl.AbstractDdlSequencerTest;
import org.modeshape.sequencer.ddl.StandardDdlLexicon;

/**
 * Unit test for the {@link org.modeshape.sequencer.ddl.DdlSequencer} when Oracle dialects are parsed.
 * 
 * @author Horia Chiorean
 */
public class OracleDdlSequencerTest extends AbstractDdlSequencerTest {

    @Test
    public void shouldSequenceOracleDdl() throws Exception {
        Node statementsNode = sequenceDdl("ddl/dialect/oracle/oracle_test_statements_2.ddl");
        assertThat(statementsNode.getNodes().getSize(), is(50l));
        verifyPrimaryType(statementsNode, NT_UNSTRUCTURED);
        verifyProperty(statementsNode, PARSER_ID, "ORACLE");

        Node createOrReplDirNode = statementsNode.getNode("CREATE OR REPLACE DIRECTORY");
        assertNotNull(createOrReplDirNode);
        verifyBaseProperties(createOrReplDirNode, NT_UNSTRUCTURED, "164", "1", "3937", 0);
        verifyMixinType(createOrReplDirNode, TYPE_CREATE_DIRECTORY_STATEMENT);

        Node countriesNode = statementsNode.getNode("countries");
        assertNotNull(countriesNode);
        verifyBaseProperties(countriesNode, NT_UNSTRUCTURED, "9", "1", "89", 3);
        verifyMixinType(countriesNode, TYPE_ALTER_TABLE_STATEMENT);

        Node duty_pct_node = countriesNode.getNode("duty_pct");
        assertNotNull(duty_pct_node);
        verifyPrimaryType(duty_pct_node, NT_UNSTRUCTURED);
        verifyProperty(duty_pct_node, DATATYPE_NAME, "NUMBER");
        verifyProperty(duty_pct_node, DATATYPE_PRECISION, "2");
        verifyProperty(duty_pct_node, DATATYPE_SCALE, "2");
        assertThat(verifyHasProperty(duty_pct_node, DATATYPE_LENGTH), is(false));
        verifyMixinType(duty_pct_node, TYPE_ADD_COLUMN_DEFINITION);

        Node check_1_node = countriesNode.getNode("CHECK_1");
        assertNotNull(check_1_node);
        verifyPrimaryType(check_1_node, NT_UNSTRUCTURED);
        verifyProperty(check_1_node, CONSTRAINT_TYPE, "CHECK");
        verifyMixinType(check_1_node, TYPE_ADD_TABLE_CONSTRAINT_DEFINITION);
        verifyProperty(check_1_node, CHECK_SEARCH_CONDITION, "( duty_pct < 10 . 5 )");

        Node visa_needed_node = countriesNode.getNode("visa_needed");
        assertNotNull(visa_needed_node);
        verifyPrimaryType(visa_needed_node, NT_UNSTRUCTURED);
        verifyProperty(visa_needed_node, DATATYPE_NAME, "VARCHAR2");
        verifyProperty(visa_needed_node, DATATYPE_LENGTH, "3");
        verifyMixinType(visa_needed_node, TYPE_ADD_COLUMN_DEFINITION);

        Node app_user1Node = statementsNode.getNode("app_user1");
        assertNotNull(app_user1Node);
        verifyBaseProperties(app_user1Node, NT_UNSTRUCTURED, "33", "1", "624", 0);
        verifyMixinType(app_user1Node, TYPE_ALTER_USER_STATEMENT);
    }

    @Test
    public void shouldSequenceOracleCreateProceduresAndFunctions() throws Exception {
        Node statementsNode = sequenceDdl("ddl/dialect/oracle/create_procedure_statements.ddl");
        assertEquals(4, statementsNode.getNodes().getSize());

        Node node = findNode(statementsNode, "remove_emp", TYPE_CREATE_PROCEDURE_STATEMENT);
        assertEquals(1, node.getNodes().getSize());
        Node paramNode = findNode(node, "employee_id", TYPE_FUNCTION_PARAMETER);
        verifyProperty(paramNode, DATATYPE_NAME, "NUMBER");

        node = findNode(statementsNode, "find_root", TYPE_CREATE_PROCEDURE_STATEMENT);
        assertEquals(1, node.getNodes().getSize());
        paramNode = findNode(node, "x", TYPE_FUNCTION_PARAMETER);
        verifyProperty(paramNode, DATATYPE_NAME, "REAL");
        verifyProperty(paramNode, IN_OUT_NO_COPY, "IN");

        node = findNode(statementsNode, "SecondMax", TYPE_CREATE_FUNCTION_STATEMENT);
        assertEquals(1, node.getNodes().getSize());
        paramNode = findNode(node, "input", TYPE_FUNCTION_PARAMETER);
        verifyProperty(paramNode, DATATYPE_NAME, "NUMBER");

        node = findNode(statementsNode, "text_length", TYPE_CREATE_FUNCTION_STATEMENT);
        assertEquals(1, node.getNodes().getSize());
        verifyProperty(node, DATATYPE_NAME, "NUMBER");
        paramNode = findNode(node, "a", TYPE_FUNCTION_PARAMETER);
        verifyProperty(paramNode, DATATYPE_NAME, "CLOB");
    }

    @Test
    public void shouldSequenceOracleIndexes() throws Exception {
        Node statementsNode = sequenceDdl("ddl/dialect/oracle/oracle_indexes.ddl");
        verifyPrimaryType(statementsNode, NT_UNSTRUCTURED);
        verifyProperty(statementsNode, PARSER_ID, "ORACLE");
        assertThat(statementsNode.getNodes().getSize(), is(10L));
    }

    @Test
    public void shouldParseUnterminatedStatements() throws Exception {
        Node statementsNode = sequenceDdl("ddl/dialect/oracle/unterminatedStatements.ddl");
        verifyPrimaryType(statementsNode, NT_UNSTRUCTURED);
        verifyProperty(statementsNode, PARSER_ID, "ORACLE");
        assertThat(statementsNode.getNodes().getSize(), is(3L));

        final NodeIterator itr = statementsNode.getNodes();

        while (itr.hasNext()) {
            verifyMixinType(itr.nextNode(), StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT);
        }
    }

    @Test
    public void shouldSequenceDbObjectNameWithValidSymbols() throws Exception {
        Node statementsNode = sequenceDdl("ddl/dialect/oracle/namesWithSymbols.ddl");
        verifyPrimaryType(statementsNode, NT_UNSTRUCTURED);
        verifyProperty(statementsNode, PARSER_ID, "ORACLE");
        assertThat(statementsNode.getNodes().getSize(), is(2L)); // 1 table, 1 index

        { // table node
            final Node tableNode = findNode(statementsNode, "EL$VIS", StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT);
            findNode(tableNode, "COL_A", StandardDdlLexicon.TYPE_COLUMN_DEFINITION);
            findNode(tableNode, "COL@B", StandardDdlLexicon.TYPE_COLUMN_DEFINITION);
            findNode(tableNode, "COL#C", StandardDdlLexicon.TYPE_COLUMN_DEFINITION);
        }

        findNode(statementsNode, "IDX$A", OracleDdlLexicon.TYPE_CREATE_TABLE_INDEX_STATEMENT);
    }

}
