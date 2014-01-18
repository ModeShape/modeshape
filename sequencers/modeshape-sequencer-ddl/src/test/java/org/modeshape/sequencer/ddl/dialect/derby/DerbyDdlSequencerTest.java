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
package org.modeshape.sequencer.ddl.dialect.derby;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.modeshape.jcr.api.JcrConstants.NT_UNSTRUCTURED;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.CONSTRAINT_TYPE;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DATATYPE_NAME;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DDL_START_LINE_NUMBER;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.GRANTEE;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.NEW_NAME;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.PARSER_ID;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_ADD_TABLE_CONSTRAINT_DEFINITION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_ALTER_TABLE_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_COLUMN_REFERENCE;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_CREATE_SCHEMA_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.VALUE;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.PARAMETER_STYLE;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TABLE_NAME;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_CREATE_FUNCTION_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_CREATE_INDEX_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_CREATE_SYNONYM_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_CREATE_TRIGGER_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_DECLARE_GLOBAL_TEMPORARY_TABLE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_DROP_PROCEDURE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_GRANT_ON_PROCEDURE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_GRANT_ROLES_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_INDEX_COLUMN_REFERENCE;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_LOCK_TABLE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlLexicon.TYPE_RENAME_TABLE_STATEMENT;
import javax.jcr.Node;
import org.junit.Test;
import org.modeshape.sequencer.ddl.AbstractDdlSequencerTest;

/**
 * Unit test for the {@link org.modeshape.sequencer.ddl.DdlSequencer} when Derby specific files are used
 * 
 * @author Horia Chiorean
 */
public class DerbyDdlSequencerTest extends AbstractDdlSequencerTest {

    @Test
    public void shouldSequenceDerbyDdl() throws Exception {
        Node statementsNode = sequenceDdl("ddl/dialect/derby/derby_test_statements.ddl");
        assertThat(statementsNode.getNodes().getSize(), is(64l));
        verifyPrimaryType(statementsNode, NT_UNSTRUCTURED);
        verifyProperty(statementsNode, PARSER_ID, "DERBY");

        Node indexNode = statementsNode.getNode("IXSALE");
        assertNotNull(indexNode);
        verifyBaseProperties(indexNode, NT_UNSTRUCTURED, "87", "1", "2931", 1);
        verifyMixinType(indexNode, TYPE_CREATE_INDEX_STATEMENT);
        findNode(indexNode, "SALES", TYPE_INDEX_COLUMN_REFERENCE);

        Node schemaNode = statementsNode.getNode("FLIGHTS");
        assertNotNull(schemaNode);
        verifyBaseProperties(schemaNode, NT_UNSTRUCTURED, "98", "1", "3263", 0);
        verifyMixinType(schemaNode, TYPE_CREATE_SCHEMA_STATEMENT);
        verifyExpression(schemaNode, "CREATE SCHEMA FLIGHTS AUTHORIZATION anita;");

        Node unknownNode_1 = statementsNode.getNode("some_procedure_name");
        assertNotNull(unknownNode_1);
        verifyBaseProperties(unknownNode_1, NT_UNSTRUCTURED, "172", "1", "5513", 0);
        verifyMixinType(unknownNode_1, TYPE_DROP_PROCEDURE_STATEMENT);
        verifyExpression(unknownNode_1, "DROP PROCEDURE some_procedure_name;");
        Node alterTableNode = statementsNode.getNode("SAMP.DEPARTMENT");
        assertNotNull(alterTableNode);
        verifyBaseProperties(alterTableNode, NT_UNSTRUCTURED, "16", "1", "478", 1);
        verifyMixinType(alterTableNode, TYPE_ALTER_TABLE_STATEMENT);

        Node uniqueNode = alterTableNode.getNode("NEW_UNIQUE");
        assertNotNull(uniqueNode);
        verifyPrimaryType(uniqueNode, NT_UNSTRUCTURED);
        verifyProperty(uniqueNode, CONSTRAINT_TYPE, "UNIQUE");
        verifyMixinType(uniqueNode, TYPE_ADD_TABLE_CONSTRAINT_DEFINITION);

        // One column reference
        assertThat(uniqueNode.getNodes().getSize(), is(1l));
        Node colRefNode = uniqueNode.getNode("DEPTNO");
        assertNotNull(colRefNode);
        verifyPrimaryType(colRefNode, NT_UNSTRUCTURED);
        verifyMixinType(colRefNode, TYPE_COLUMN_REFERENCE);

        // Create Function
        Node functionNode = findNode(statementsNode, "PROPERTY_FILE_READER", TYPE_CREATE_FUNCTION_STATEMENT);
        verifyProperty(functionNode, DDL_START_LINE_NUMBER, 71);
        functionNode = findNode(statementsNode, "TO_DEGREES", TYPE_CREATE_FUNCTION_STATEMENT);
        verifyProperty(functionNode, PARAMETER_STYLE, "PARAMETER STYLE JAVA");

        // declare global temporary table SESSION.t1(c11 int) not logged;
        Node ttNode = findNode(statementsNode, "SESSION.t1", TYPE_DECLARE_GLOBAL_TEMPORARY_TABLE_STATEMENT);
        Node colNode = findNode(ttNode, "c11");
        verifyProperty(colNode, DATATYPE_NAME, "int");

        // LOCK TABLE FlightAvailability IN EXCLUSIVE MODE;
        Node lockNode = findNode(statementsNode, "FlightAvailability", TYPE_LOCK_TABLE_STATEMENT);
        Node optionNode = findNode(lockNode, "lockMode");
        verifyProperty(optionNode, VALUE, "EXCLUSIVE");

        // RENAME TABLE SAMP.EMP_ACT TO EMPLOYEE_ACT
        Node renameTableNode = findNode(statementsNode, "SAMP.EMP_ACT[2]", TYPE_RENAME_TABLE_STATEMENT);
        verifyProperty(renameTableNode, NEW_NAME, "EMPLOYEE_ACT");

        // CREATE SYNONYM SAMP.T1 FOR SAMP.TABLEWITHLONGNAME;
        Node synonymNode = findNode(statementsNode, "SAMP.T1", TYPE_CREATE_SYNONYM_STATEMENT);
        verifyProperty(synonymNode, TABLE_NAME, "SAMP.TABLEWITHLONGNAME");

        // CREATE TRIGGER FLIGHTSDELETE3
        // AFTER DELETE ON FLIGHTS
        // REFERENCING OLD AS OLD
        // FOR EACH ROW
        // DELETE FROM FLIGHTAVAILABILITY WHERE FLIGHT_ID = OLD.FLIGHT_ID;
        Node triggerNode = findNode(statementsNode, "FLIGHTSDELETE3", TYPE_CREATE_TRIGGER_STATEMENT);
        verifyProperty(triggerNode, TABLE_NAME, "FLIGHTS");

        // CREATE TRIGGER t1 NO CASCADE BEFORE UPDATE ON x
        // FOR EACH ROW MODE DB2SQL
        // values app.notifyEmail('Jerry', 'Table x is about to be updated');
        triggerNode = findNode(statementsNode, "t1", TYPE_CREATE_TRIGGER_STATEMENT);
        verifyProperty(triggerNode, TABLE_NAME, "x");
        optionNode = findNode(triggerNode, "forEach");
        verifyProperty(optionNode, VALUE, "FOR EACH ROW");
        optionNode = findNode(triggerNode, "eventType");
        verifyProperty(optionNode, VALUE, "UPDATE");

        // GRANT EXECUTE ON PROCEDURE p TO george;
        findNode(statementsNode, "p", TYPE_GRANT_ON_PROCEDURE_STATEMENT);

        // GRANT purchases_reader_role TO george,maria;
        Node grantNode = findNode(statementsNode, "grantRoles", TYPE_GRANT_ROLES_STATEMENT);
        /*Node roleNode =*/findNode(grantNode, "george", GRANTEE);
    }
}
