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
package org.modeshape.sequencer.ddl;

import javax.jcr.Node;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.junit.Test;
import static org.modeshape.jcr.api.JcrConstants.*;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.*;
import org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlParser;
import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for the behaviour of the ddl sequencer, when standard ddl files are sequenced
 *
 * @author Horia Chiorean
 */
public class StandardDdlSequencerTest extends AbstractDdlSequencerTest {

    @Test
    public void shouldSequenceCreateSchema() throws Exception {
        // CREATE SCHEMA hollywood
        // CREATE TABLE films (title varchar(255), release date, producerName varchar(255))
        // CREATE VIEW winners AS
        // SELECT title, release FROM films WHERE producerName IS NOT NULL;
        Node statementsNode = sequenceDdl("ddl/create_schema.ddl");
        assertThat(statementsNode.getNodes().getSize(), is(1l));

        verifyPrimaryType(statementsNode, NT_UNSTRUCTURED);
        verifyProperty(statementsNode, PARSER_ID, "POSTGRES");

        Node schemaNode = statementsNode.getNode("hollywood");
        assertNotNull(schemaNode);
        verifyBaseProperties(schemaNode, NT_UNSTRUCTURED, "1", "1", "0", 2);
        verifyMixinType(schemaNode, TYPE_CREATE_SCHEMA_STATEMENT);
        verifyExpression(schemaNode, "CREATE SCHEMA hollywood");

        Node filmsNode = schemaNode.getNode("films");
        assertNotNull(filmsNode);
        verifyBaseProperties(filmsNode, NT_UNSTRUCTURED, "2", "5", "28", 3);
        verifyMixinType(filmsNode, TYPE_CREATE_TABLE_STATEMENT);
        verifyExpression(filmsNode, "CREATE TABLE films (title varchar(255), release date, producerName varchar(255))");

        Node winnersNode = schemaNode.getNode("winners");
        assertNotNull(winnersNode);
        verifyBaseProperties(winnersNode, NT_UNSTRUCTURED, "3", "5", "113", 0);
        verifyMixinType(winnersNode, TYPE_CREATE_VIEW_STATEMENT);
        verifyExpression(winnersNode,
                         "CREATE VIEW winners AS SELECT title, release FROM films WHERE producerName IS NOT NULL;");

        // Check Column Properties
        Node titleNode = filmsNode.getNode("title");
        assertNotNull(titleNode);
        verifyPrimaryType(titleNode, NT_UNSTRUCTURED);
        verifyProperty(titleNode, DATATYPE_NAME, "VARCHAR");
        verifyProperty(titleNode, DATATYPE_LENGTH, "255");
        verifyMixinType(titleNode, TYPE_COLUMN_DEFINITION);

        Node releaseNode = filmsNode.getNode("release");
        assertNotNull(releaseNode);
        verifyPrimaryType(releaseNode, NT_UNSTRUCTURED);
        verifyProperty(releaseNode, DATATYPE_NAME, "DATE");
        verifyHasProperty(releaseNode, DATATYPE_LENGTH);
        verifyMixinType(titleNode, TYPE_COLUMN_DEFINITION);
    }

    @Test
    public void shouldSequenceCreateTable() throws Exception {
        // CREATE TABLE IDTABLE
        // (
        // IDCONTEXT VARCHAR(20) NOT NULL PRIMARY KEY,
        // NEXTID NUMERIC
        // );
        String targetExpression = "CREATE TABLE IDTABLE\n" + "(\n" + "  IDCONTEXT  VARCHAR(20) NOT NULL PRIMARY KEY,\n"
                + "  NEXTID     NUMERIC\n" + ");";

        Node statementsNode = sequenceDdl("ddl/create_table.ddl");
        assertThat(statementsNode.getNodes().getSize(), is(1l));
        verifyPrimaryType(statementsNode, NT_UNSTRUCTURED);
        verifyProperty(statementsNode, PARSER_ID, "SQL92");

        Node tableNode = statementsNode.getNode("IDTABLE");
        assertNotNull(tableNode);
        verifyBaseProperties(tableNode, NT_UNSTRUCTURED, "1", "1", "0", 3);
        verifyMixinType(tableNode, TYPE_CREATE_TABLE_STATEMENT);
        verifyExpression(tableNode, targetExpression);

        // Check Column Properties
        Node idcontextNode = tableNode.getNode("IDCONTEXT");
        assertNotNull(idcontextNode);
        verifyPrimaryType(idcontextNode, NT_UNSTRUCTURED);
        verifyProperty(idcontextNode, DATATYPE_NAME, "VARCHAR");
        verifyProperty(idcontextNode, DATATYPE_LENGTH, "20");
        verifyMixinType(idcontextNode, TYPE_COLUMN_DEFINITION);

        Node nextidNode = tableNode.getNode("NEXTID");
        assertNotNull(nextidNode);
        verifyPrimaryType(nextidNode, NT_UNSTRUCTURED);
        verifyProperty(nextidNode, DATATYPE_NAME, "NUMERIC");
        verifyProperty(nextidNode, DATATYPE_PRECISION, "0");
        verifyProperty(nextidNode, DATATYPE_SCALE, "0");
        verifyHasProperty(nextidNode, DATATYPE_LENGTH);
        verifyMixinType(nextidNode, TYPE_COLUMN_DEFINITION);

        Node pk_1_Node = tableNode.getNode("PK_1");
        assertNotNull(pk_1_Node);
        verifyPrimaryType(pk_1_Node, NT_UNSTRUCTURED);
        verifyProperty(pk_1_Node, CONSTRAINT_TYPE, "PRIMARY KEY");
        verifyMixinType(pk_1_Node, TYPE_TABLE_CONSTRAINT);

        // One column reference
        assertThat(pk_1_Node.getNodes().getSize(), is(1l));
        Node idcontectRefNode = pk_1_Node.getNode("IDCONTEXT");
        assertNotNull(idcontectRefNode);
        verifyPrimaryType(idcontectRefNode, NT_UNSTRUCTURED);
        verifyMixinType(idcontectRefNode, TYPE_COLUMN_REFERENCE);
    }


    @Test
    public void shouldGenerateNodeTypesForCreateTables() throws Exception {
        // Check one table
        // CREATE TABLE RT_MDLS
        // (
        // MDL_UID NUMERIC(20) NOT NULL,
        // MDL_UUID VARCHAR(64) NOT NULL,
        // MDL_NM VARCHAR(255) NOT NULL,
        // MDL_VERSION VARCHAR(50),
        // DESCRIPTION VARCHAR(255),
        // MDL_URI VARCHAR(255),
        // MDL_TYPE NUMERIC(3),
        // IS_PHYSICAL CHAR(1) NOT NULL,
        // MULTI_SOURCED CHAR(1) DEFAULT '0',
        // VISIBILITY NUMERIC(3)
        //
        // );
        Node statementsNode = sequenceDdl("ddl/createTables.ddl");

        assertThat(statementsNode.getNodes().getSize(), is(20l));
        verifyPrimaryType(statementsNode, NT_UNSTRUCTURED);
        verifyProperty(statementsNode, PARSER_ID, "SQL92");

        Node tableNode = statementsNode.getNode("RT_MDLS");
        assertNotNull(tableNode);
        verifyBaseProperties(tableNode, NT_UNSTRUCTURED, "81", "1", "2212", 10);
        verifyMixinType(tableNode, TYPE_CREATE_TABLE_STATEMENT);

        // Check Column Properties
        Node node_1 = tableNode.getNode("MDL_UUID");
        assertNotNull(node_1);
        verifyPrimaryType(node_1, NT_UNSTRUCTURED);
        verifyProperty(node_1, DATATYPE_NAME, "VARCHAR");
        verifyProperty(node_1, DATATYPE_LENGTH, "64");
        verifyMixinType(node_1, TYPE_COLUMN_DEFINITION);

        Node node_2 = tableNode.getNode("MDL_TYPE");
        assertNotNull(node_2);
        verifyPrimaryType(node_2, NT_UNSTRUCTURED);
        verifyProperty(node_2, DATATYPE_NAME, "NUMERIC");
        verifyProperty(node_2, DATATYPE_PRECISION, "3");
        verifyProperty(node_2, DATATYPE_SCALE, "0");
        verifyHasProperty(node_2, DATATYPE_LENGTH);
        verifyMixinType(node_2, TYPE_COLUMN_DEFINITION);
    }

    @Test
    public void shouldSequenceStandardDdlFile() throws Exception {
        Node statementsNode = sequenceDdl("ddl/standard_test_statements.ddl");
        assertEquals(44l, statementsNode.getNodes().getSize());

        Node stmtNode = findNode(statementsNode, "assertNotNull", TYPE_CREATE_ASSERTION_STATEMENT);
        Node constraintAttribute = findNode(stmtNode, "CONSTRAINT_ATTRIBUTE", TYPE_CONSTRAINT_ATTRIBUTE);
        verifyProperty(constraintAttribute, PROPERTY_VALUE, "NOT DEFERRABLE");

        stmtNode = findNode(statementsNode, "assertIsZero", TYPE_CREATE_ASSERTION_STATEMENT);
        constraintAttribute = findNode(stmtNode, "CONSTRAINT_ATTRIBUTE", TYPE_CONSTRAINT_ATTRIBUTE);
        verifyProperty(constraintAttribute, PROPERTY_VALUE, "INITIALLY DEFERRED");

        Node tableNode = findNode(statementsNode, "employee", TYPE_CREATE_TABLE_STATEMENT);
        Node columnNode = findNode(tableNode, "empname", TYPE_COLUMN_DEFINITION);
        verifyProperty(columnNode, DATATYPE_NAME, "CHAR");
        verifyProperty(columnNode, DATATYPE_LENGTH, 10);

        Node constraintNode = findNode(tableNode, "emp_fk1", TYPE_TABLE_CONSTRAINT);
        constraintAttribute = findNode(constraintNode, "CONSTRAINT_ATTRIBUTE", TYPE_CONSTRAINT_ATTRIBUTE);
        verifyProperty(constraintAttribute, PROPERTY_VALUE, "INITIALLY IMMEDIATE");
        findNode(constraintNode, "deptno[1]", TYPE_COLUMN_REFERENCE);
        findNode(constraintNode, "dept", TYPE_TABLE_REFERENCE);
        findNode(constraintNode, "deptno[2]", TYPE_FK_COLUMN_REFERENCE);

        Node viewNode = findNode(statementsNode, "view_1", TYPE_CREATE_VIEW_STATEMENT);
        findNode(viewNode, "col1", TYPE_COLUMN_REFERENCE);

        tableNode = findNode(statementsNode, "table_5", TYPE_CREATE_TABLE_STATEMENT);
        assertEquals(18, tableNode.getNodes().getSize());
    }

    @Test
    public void shouldSequenceStandardDdlGrantStatements() throws Exception {
        Node statementsNode = sequenceDdl("ddl/grant_test_statements.ddl");
        assertEquals(4, statementsNode.getNodes().getSize());

        // GRANT SELECT ON TABLE purchaseOrders TO maria,harry;
        Node grantNode = findNode(statementsNode, "purchaseOrders", TYPE_GRANT_ON_TABLE_STATEMENT);
        findNode(grantNode, "maria", GRANTEE);
        Node privNode = findNode(grantNode, "privilege", GRANT_PRIVILEGE);
        verifyProperty(privNode, TYPE, "SELECT");

        // GRANT UPDATE, USAGE ON TABLE purchaseOrders FROM anita,zhi;
        grantNode = findNode(statementsNode, "billedOrders", TYPE_GRANT_ON_TABLE_STATEMENT);
        privNode = findNode(grantNode, "privilege", GRANT_PRIVILEGE);
        verifyProperty(privNode, TYPE, "UPDATE");
        findNode(grantNode, "anita", GRANTEE);
    }

    @Test
    public void shouldSequenceStandardDdlRevokeStatements() throws Exception {
        Node statementsNode = sequenceDdl("ddl/revoke_test_statements.ddl");
        assertEquals(4, statementsNode.getNodes().getSize());

        // REVOKE SELECT ON TABLE purchaseOrders FROM maria,harry;
        Node revokeNode = findNode(statementsNode, "purchaseOrders", TYPE_REVOKE_ON_TABLE_STATEMENT);
        findNode(revokeNode, "maria", GRANTEE);
        Node privNode = findNode(revokeNode, "privilege", GRANT_PRIVILEGE);
        verifyProperty(privNode, TYPE, "SELECT");

        // REVOKE UPDATE, USAGE ON TABLE purchaseOrders FROM anita,zhi CASCADE;
        revokeNode = findNode(statementsNode, "orderDetails", TYPE_REVOKE_ON_TABLE_STATEMENT);
        privNode = findNode(revokeNode, "privilege", GRANT_PRIVILEGE);
        verifyProperty(privNode, TYPE, "UPDATE");
        findNode(revokeNode, "anita", GRANTEE);
    }

    protected String[] builtInGrammars() {
        List<String> builtInParserNames = new ArrayList<String>();
        for (DdlParser parser : DdlParsers.BUILTIN_PARSERS) {
            builtInParserNames.add(parser.getId().toLowerCase());
        }
        return builtInParserNames.toArray(new String[builtInParserNames.size()]);
    }

    @Test
    public void shouldHaveDefaultListOfGrammars() {
        DdlSequencer sequencer = new DdlSequencer();
        String[] grammars = sequencer.getGrammars();
        assertThat(grammars, is(builtInGrammars()));
    }

    @Test
    public void shouldCreateListOfDdlParserInstancesForDefaultListOfGrammars() {
        DdlSequencer sequencer = new DdlSequencer();
        List<DdlParser> parsers = sequencer.getParserList();
        assertThat(parsers, is(DdlParsers.BUILTIN_PARSERS));
    }

    @Test
    public void shouldAllowSettingGrammarsWithEmptyArray() {
        DdlSequencer sequencer = new DdlSequencer();
        sequencer.setGrammars(new String[] {});
        assertThat(sequencer.getGrammars(), is(builtInGrammars()));
    }

    @Test
    public void shouldAllowSettingGrammarsWithNullArray() {
        DdlSequencer sequencer = new DdlSequencer();
        sequencer.setGrammars(null);
        assertThat(sequencer.getGrammars(), is(builtInGrammars()));
    }

    @Test
    public void shouldAllowSettingGrammarsToNonEmptyArrayOfValidBuiltInGrammars() {
        DdlSequencer sequencer = new DdlSequencer();
        String[] grammars = new String[] {new OracleDdlParser().getId(), new StandardDdlParser().getId()};
        sequencer.setGrammars(grammars);
        assertThat(sequencer.getGrammars(), is(grammars));
    }

    @Test
    public void shouldAllowSettingGrammarsToNonEmptyArrayOfValidAndNonExistantBuiltInGrammars() {
        DdlSequencer sequencer = new DdlSequencer();
        String[] grammars = new String[] {new OracleDdlParser().getId(), new StandardDdlParser().getId(), "argle"};
        sequencer.setGrammars(grammars);
        assertThat(sequencer.getGrammars(), is(grammars));
    }

    @Test
    public void shouldCreateDdlParserInstancesForAllValidBuiltInGrammars() {
        DdlSequencer sequencer = new DdlSequencer();
        String[] grammars = new String[] {new OracleDdlParser().getId(), new StandardDdlParser().getId(), "argle"};
        sequencer.setGrammars(grammars);
        assertThat(sequencer.getGrammars(), is(grammars));
        List<DdlParser> parsers = sequencer.getParserList();
        assertThat(parsers.get(0), is((DdlParser)new OracleDdlParser()));
        assertThat(parsers.get(1), is((DdlParser)new StandardDdlParser()));
        assertThat(parsers.size(), is(2));
    }

    @Test
    public void shouldCreateDdlParserInstancesForAllValidBuiltInGrammarsAndInstantiableParser() {
        DdlSequencer sequencer = new DdlSequencer();
        String[] grammars = new String[] {new OracleDdlParser().getId(), new StandardDdlParser().getId(),
                ArgleDdlParser.class.getName()};
        sequencer.setGrammars(grammars);
        assertThat(sequencer.getGrammars(), is(grammars));
        List<DdlParser> parsers = sequencer.getParserList();
        assertThat(parsers.get(0), is((DdlParser)new OracleDdlParser()));
        assertThat(parsers.get(1), is((DdlParser)new StandardDdlParser()));
        assertThat(parsers.get(2), is((DdlParser)new ArgleDdlParser()));
        assertThat(parsers.size(), is(3));
    }

    protected static class ArgleDdlParser extends StandardDdlParser {
        @Override
        public String getId() {
            return "ARGLE";
        }
    }
}
