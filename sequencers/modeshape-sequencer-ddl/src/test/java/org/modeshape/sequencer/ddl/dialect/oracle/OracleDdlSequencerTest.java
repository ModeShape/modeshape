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
package org.modeshape.sequencer.ddl.dialect.oracle;

import javax.jcr.Node;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import static org.modeshape.jcr.api.JcrConstants.NT_UNSTRUCTURED;
import org.modeshape.sequencer.ddl.AbstractDdlSequencerTest;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.*;
import static org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon.*;

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
        verifyBaseProperties(createOrReplDirNode, NT_UNSTRUCTURED, "164", "1", "3886", 0);
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
        verifyMixinType(duty_pct_node, TYPE_COLUMN_DEFINITION);

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
        verifyMixinType(visa_needed_node, TYPE_COLUMN_DEFINITION);

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

}
