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
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.jcr.sequencer.AbstractSequencerTest;
import org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlParser;
import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class StandardDdlSequencerTest extends AbstractSequencerTest {

    private void verifyProperty( Node node, String propNameStr, String expectedValue ) throws RepositoryException {
        Property property = node.getProperty(propNameStr);
        Value value = property.isMultiple() ? property.getValues()[0] : property.getValue();
        assertEquals(expectedValue, value.getString());
    }

    private boolean verifyHasProperty( Node node, String propNameStr ) throws RepositoryException {
        return node.hasProperty(propNameStr);
    }

    private void verifyPrimaryType( Node node, String expectedValue ) throws RepositoryException {
        verifyProperty(node, "jcr:primaryType", expectedValue);
    }

    private void verifyMixinType( Node node, String expectedValue ) throws RepositoryException {
        verifyProperty(node, "jcr:mixinTypes", expectedValue);
    }

    private void verifyExpression( Node node, String expectedValue ) throws RepositoryException {
        verifyProperty(node, "ddl:expression", expectedValue);
    }

    private void verifyBaseProperties( Node node, String primaryType, String lineNum, String colNum, String charIndex, long numChildren ) throws RepositoryException {
        verifyPrimaryType(node, primaryType);
        verifyProperty(node, "ddl:startLineNumber", lineNum);
        verifyProperty(node, "ddl:startColumnNumber", colNum);
        verifyProperty(node, "ddl:startCharIndex", charIndex);
        assertThat(node.getNodes().getSize(), is(numChildren));
    }

    @Test
    public void shouldSequenceCreateSchema() throws Exception {
        // CREATE SCHEMA hollywood
        // CREATE TABLE films (title varchar(255), release date, producerName varchar(255))
        // CREATE VIEW winners AS
        // SELECT title, release FROM films WHERE producerName IS NOT NULL;

        // Subgraph
        // <name = "/" uuid = "0a3501e8-a6ec-4a0e-89b7-9cf64476b18b">
        // <name = "statements" primaryType = "nt:unstructured" uuid = "56f305d6-aa76-4b1a-8e06-bd0c2981c2ff" parserId =
        // "POSTGRES">
        // <name = "hollywood" startLineNumber = "1" primaryType = "nt:unstructured" uuid = "88d2901d-eeaa-4bd6-80c4-243ea56f7c20"
        // startColumnNumber = "1" mixinTypes = "ddl:createSchemaStatement" expression = "CREATE SCHEMA hollywood" startCharIndex
        // = "0">
        // <name = "films" startLineNumber = "2" primaryType = "nt:unstructured" uuid = "8ff9e0a1-ad32-48b0-8a76-011fd6a35142"
        // startColumnNumber = "5" mixinTypes = "ddl:createTableStatement" expression =
        // "CREATE TABLE films (title varchar(255), release date, producerName varchar(255))" startCharIndex = "28">
        // <name = "title" datatypeName = "VARCHAR" datatypeLength = "255" primaryType = "nt:unstructured" uuid =
        // "c1a65f89-2ed4-4a39-858d-b4fd06c60879" mixinTypes = "ddl:columnDefinition">
        // <name = "release" datatypeName = "DATE" primaryType = "nt:unstructured" uuid = "67b8ee6d-341e-4e7e-bb79-bdec0caa6865"
        // mixinTypes = "ddl:columnDefinition">
        // <name = "producerName" datatypeName = "VARCHAR" datatypeLength = "255" primaryType = "nt:unstructured" uuid =
        // "83deb1a7-8ccb-479c-ab38-acc64be99251" mixinTypes = "ddl:columnDefinition">
        // <name = "winners" startLineNumber = "3" primaryType = "nt:unstructured" uuid = "c8610a1c-be27-4b16-9b0b-fc631dbfd00b"
        // startColumnNumber = "5" mixinTypes = "ddl:createViewStatement" expression =
        // "CREATE VIEW winners AS SELECT title, release FROM films WHERE producerName IS NOT NULL;" queryExpression =
        // " SELECT title, release FROM films WHERE producerName IS NOT NULL" startCharIndex = "113">

        Node statementsNode = sequenceDdl("ddl/create_schema.ddl");
        assertThat(statementsNode.getNodes().getSize(), is(1l));

        verifyPrimaryType(statementsNode, "nt:unstructured");
        verifyProperty(statementsNode, "ddl:parserId", "POSTGRES");

        Node schemaNode = statementsNode.getNode("hollywood");
        assertNotNull(schemaNode);
        verifyBaseProperties(schemaNode, "nt:unstructured", "1", "1", "0", 2);
        verifyMixinType(schemaNode, "ddl:createSchemaStatement");
        verifyExpression(schemaNode, "CREATE SCHEMA hollywood");

        Node filmsNode = schemaNode.getNode("films");
        assertNotNull(filmsNode);
        verifyBaseProperties(filmsNode, "nt:unstructured", "2", "5", "28", 3);
        verifyMixinType(filmsNode, "ddl:createTableStatement");
        verifyExpression(filmsNode, "CREATE TABLE films (title varchar(255), release date, producerName varchar(255))");

        Node winnersNode = schemaNode.getNode("winners");
        assertNotNull(winnersNode);
        verifyBaseProperties(winnersNode, "nt:unstructured", "3", "5", "113", 0);
        verifyMixinType(winnersNode, "ddl:createViewStatement");
        verifyExpression(winnersNode,
                         "CREATE VIEW winners AS SELECT title, release FROM films WHERE producerName IS NOT NULL;");

        // Check Column Properties
        Node titleNode = filmsNode.getNode("title");
        assertNotNull(titleNode);
        verifyPrimaryType(titleNode, "nt:unstructured");
        verifyProperty(titleNode, "ddl:datatypeName", "VARCHAR");
        verifyProperty(titleNode, "ddl:datatypeLength", "255");
        verifyMixinType(titleNode, "ddl:columnDefinition");

        Node releaseNode = filmsNode.getNode("release");
        assertNotNull(releaseNode);
        verifyPrimaryType(releaseNode, "nt:unstructured");
        verifyProperty(releaseNode, "ddl:datatypeName", "DATE");
        verifyHasProperty(releaseNode, "ddl:datatypeLength");
        verifyMixinType(titleNode, "ddl:columnDefinition");

        // Map<Property> props = output.getProperties(nodePath)
    }

    @Test
    public void shouldSequenceCreateTable() throws Exception {
        // CREATE TABLE IDTABLE
        // (
        // IDCONTEXT VARCHAR(20) NOT NULL PRIMARY KEY,
        // NEXTID NUMERIC
        // );

        // Subgraph
        // <name = "/" uuid = "8325c96e-e81c-4f16-9c08-33b408e4f7cf">
        // <name = "statements" primaryType = "nt:unstructured" uuid = "ac307790-7378-44fe-8a06-e4292574e2f1" parserId = "SQL92">
        // <name = "IDTABLE" startLineNumber = "1" primaryType = "nt:unstructured" uuid = "cb0e1c03-0815-4b3b-92a0-b95a5980be53"
        // startColumnNumber = "1" mixinTypes = "ddl:createTableStatement" expression = "CREATE TABLE IDTABLE
        // (
        // IDCONTEXT VARCHAR(20) NOT NULL PRIMARY KEY,
        // NEXTID NUMERIC
        // );" startCharIndex = "0">
        // <name = "IDCONTEXT" datatypeName = "VARCHAR" datatypeLength = "20" primaryType = "nt:unstructured" uuid =
        // "ab269004-852a-4731-9886-9ec8dff230b9" nullable = "NOT NULL" mixinTypes = "ddl:columnDefinition">
        // <name = "PK_1" primaryType = "nt:unstructured" uuid = "7395e24e-db3d-412d-91fe-d479258b8641" mixinTypes =
        // "ddl:tableConstraint" constraintType = "2">
        // <name = "IDCONTEXT" primaryType = "nt:unstructured" uuid = "43f052c7-fbf9-4243-a995-86092c367177" mixinTypes =
        // "ddl:columnReference">
        // <name = "NEXTID" datatypeName = "NUMERIC" primaryType = "nt:unstructured" uuid = "91dc78bc-0c43-4f80-9579-4010e31a7ae3"
        // datatypePrecision = "0" mixinTypes = "ddl:columnDefinition" datatypeScale = "0">

        String targetExpression = "CREATE TABLE IDTABLE\n" + "(\n" + "  IDCONTEXT  VARCHAR(20) NOT NULL PRIMARY KEY,\n"
                + "  NEXTID     NUMERIC\n" + ");";

        Node statementsNode = sequenceDdl("ddl/create_table.ddl");
        assertThat(statementsNode.getNodes().getSize(), is(1l));
        verifyPrimaryType(statementsNode, "nt:unstructured");
        verifyProperty(statementsNode, "ddl:parserId", "SQL92");

        Node tableNode = statementsNode.getNode("IDTABLE");
        assertNotNull(tableNode);
        verifyBaseProperties(tableNode, "nt:unstructured", "1", "1", "0", 3);
        verifyMixinType(tableNode, "ddl:createTableStatement");
        verifyExpression(tableNode, targetExpression);

        // Check Column Properties
        Node idcontextNode = tableNode.getNode("IDCONTEXT");
        assertNotNull(idcontextNode);
        verifyPrimaryType(idcontextNode, "nt:unstructured");
        verifyProperty(idcontextNode, "ddl:datatypeName", "VARCHAR");
        verifyProperty(idcontextNode, "ddl:datatypeLength", "20");
        verifyMixinType(idcontextNode, "ddl:columnDefinition");

        Node nextidNode = tableNode.getNode("NEXTID");
        assertNotNull(nextidNode);
        verifyPrimaryType(nextidNode, "nt:unstructured");
        verifyProperty(nextidNode, "ddl:datatypeName", "NUMERIC");
        verifyProperty(nextidNode, "ddl:datatypePrecision", "0");
        verifyProperty(nextidNode, "ddl:datatypeScale", "0");
        verifyHasProperty(nextidNode, "ddl:datatypeLength");
        verifyMixinType(nextidNode, "ddl:columnDefinition");

        Node pk_1_Node = tableNode.getNode("PK_1");
        assertNotNull(pk_1_Node);
        verifyPrimaryType(pk_1_Node, "nt:unstructured");
        verifyProperty(pk_1_Node, "ddl:constraintType", "PRIMARY KEY");
        verifyMixinType(pk_1_Node, "ddl:tableConstraint");

        // One column reference
        assertThat(pk_1_Node.getNodes().getSize(), is(1l));
        Node idcontectRefNode = pk_1_Node.getNode("IDCONTEXT");
        assertNotNull(idcontectRefNode);
        verifyPrimaryType(idcontectRefNode, "nt:unstructured");
        verifyMixinType(idcontectRefNode, "ddl:columnReference");
    }

    @Test
    public void shouldSequenceStatementsWithDoubleQuotes() throws Exception {
        // ALTER JAVA CLASS "Agent"
        // RESOLVER (("/home/java.101/bin/*" pm)(* public))
        // RESOLVE;
        //
        // CREATE SERVER foo FOREIGN DATA WRAPPER "default";
        //
        // CREATE RULE "_RETURN" AS
        // ON SELECT TO t1
        // DO INSTEAD
        // SELECT * FROM t2;

        // Subgraph
        // <name = "/" uuid = "233d07e5-8431-4844-856b-ac80d0129c01">
        // <name = "statements" primaryType = "nt:unstructured" uuid = "dbd23d78-8005-4b7b-aa61-eac98726b8d4" parserId =
        // "POSTGRES">
        // <name = "unknownStatement" startLineNumber = "1" primaryType = "nt:unstructured" uuid =
        // "b3fb1e1f-284b-46bd-9a06-86d1b85dd5b3" startColumnNumber = "1" mixinTypes = "ddl:unknownStatement" expression =
        // "ALTER JAVA CLASS "Agent"
        // RESOLVER (("/home/java.101/bin/*" pm)(* public))
        // RESOLVE;" startCharIndex = "0">
        // <name = "CREATE SERVER" startLineNumber = "5" primaryType = "nt:unstructured" uuid =
        // "10217bab-877f-4b45-b169-2f81bb31f620" startColumnNumber = "1" mixinTypes = "postgresddl:createServerStatement"
        // expression = "CREATE SERVER foo FOREIGN DATA WRAPPER "default";" startCharIndex = "93">
        // <name = "_RETURN" startLineNumber = "7" primaryType = "nt:unstructured" uuid = "2b655039-f239-4930-8c1b-a3b9a8e7c949"
        // startColumnNumber = "1" mixinTypes = "postgresddl:createRuleStatement" expression = "CREATE RULE "_RETURN" AS
        // ON SELECT TO t1
        // DO INSTEAD
        // SELECT * FROM t2;" startCharIndex = "144">
        Node statementsNode = sequenceDdl("ddl/d_quoted_statements.ddl");
        assertThat(statementsNode.getNodes().getSize(), is(3l));

        verifyPrimaryType(statementsNode, "nt:unstructured");
        verifyProperty(statementsNode, "ddl:parserId", "POSTGRES");

        Node firstNode = statementsNode.getNode("unknownStatement");
        assertNotNull(firstNode);
        verifyBaseProperties(firstNode, "nt:unstructured", "1", "1", "0", 0);
        verifyMixinType(firstNode, "ddl:unknownStatement");

        Node serverNode = statementsNode.getNode("CREATE SERVER");
        assertNotNull(serverNode);
        verifyBaseProperties(serverNode, "nt:unstructured", "5", "1", "93", 0);
        verifyMixinType(serverNode, "postgresddl:createServerStatement");

        Node ruleNode = statementsNode.getNode("_RETURN");
        assertNotNull(ruleNode);
        verifyBaseProperties(ruleNode, "nt:unstructured", "7", "1", "144", 0);
        verifyMixinType(ruleNode, "postgresddl:createRuleStatement");
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
        // <name = "RT_MDLS" startLineNumber = "80" primaryType = "nt:unstructured" uuid = "a11e48d5-0908-467b-ba79-a497df87a576"
        // startColumnNumber = "1" mixinTypes = "ddl:createTableStatement" expression = "CREATE TABLE RT_MDLS...." startCharIndex
        // = "2258">
        // <name = "MDL_UID" datatypeName = "NUMERIC" primaryType = "nt:unstructured" uuid =
        // "85b9c2b3-cadc-425c-a736-656be1475780" datatypePrecision = "20" nullable = "NOT NULL" mixinTypes =
        // "ddl:columnDefinition" datatypeScale = "0">
        // <name = "MDL_UUID" datatypeName = "VARCHAR" datatypeLength = "64" primaryType = "nt:unstructured" uuid =
        // "88766ab0-32e6-44f6-9244-78d48b2242d4" nullable = "NOT NULL" mixinTypes = "ddl:columnDefinition">
        // <name = "MDL_NM" datatypeName = "VARCHAR" datatypeLength = "255" primaryType = "nt:unstructured" uuid =
        // "cb29fa8e-03f0-4e82-8040-5d853aa8a1f9" nullable = "NOT NULL" mixinTypes = "ddl:columnDefinition">
        // <name = "MDL_VERSION" datatypeName = "VARCHAR" datatypeLength = "50" primaryType = "nt:unstructured" uuid =
        // "169a4e2f-79a4-47a4-8e23-4fa1ea09c876" mixinTypes = "ddl:columnDefinition">
        // <name = "DESCRIPTION" datatypeName = "VARCHAR" datatypeLength = "255" primaryType = "nt:unstructured" uuid =
        // "00682ead-5bb0-4c0d-b243-6edbf56dcb4a" mixinTypes = "ddl:columnDefinition">
        // <name = "MDL_URI" datatypeName = "VARCHAR" datatypeLength = "255" primaryType = "nt:unstructured" uuid =
        // "851f94e1-5d43-4a58-be6c-720a6c3bec7f" mixinTypes = "ddl:columnDefinition">
        // <name = "MDL_TYPE" datatypeName = "NUMERIC" primaryType = "nt:unstructured" uuid =
        // "a7b74a6f-872d-4e8b-a2da-4ebf6ef38ed4" datatypePrecision = "3" mixinTypes = "ddl:columnDefinition" datatypeScale = "0">
        // <name = "IS_PHYSICAL" datatypeName = "CHAR" datatypeLength = "1" primaryType = "nt:unstructured" uuid =
        // "b96aeaaa-4bd8-4154-9daf-50aac2fd9529" nullable = "NOT NULL" mixinTypes = "ddl:columnDefinition">
        // <name = "MULTI_SOURCED" datatypeName = "CHAR" defaultValue = "'0'" datatypeLength = "1" defaultOption = "0" primaryType
        // = "nt:unstructured" uuid = "75c48a37-f65c-4854-bca4-8d504cf8b658" mixinTypes = "ddl:columnDefinition">
        // <name = "VISIBILITY" datatypeName = "NUMERIC" primaryType = "nt:unstructured" uuid =
        // "345ac63b-a343-408d-9262-6e009ad3e0c2" datatypePrecision = "3" mixinTypes = "ddl:columnDefinition" datatypeScale = "0">

        Node statementsNode = sequenceDdl("ddl/createTables.ddl");

        assertThat(statementsNode.getNodes().getSize(), is(20l));
        verifyPrimaryType(statementsNode, "nt:unstructured");
        verifyProperty(statementsNode, "ddl:parserId", "SQL92");

        Node tableNode = statementsNode.getNode("RT_MDLS");
        assertNotNull(tableNode);
        verifyBaseProperties(tableNode, "nt:unstructured", "80", "1", "2258", 10);
        verifyMixinType(tableNode, "ddl:createTableStatement");

        // Check Column Properties
        Node node_1 = tableNode.getNode("MDL_UUID");
        assertNotNull(node_1);
        verifyPrimaryType(node_1, "nt:unstructured");
        verifyProperty(node_1, "ddl:datatypeName", "VARCHAR");
        verifyProperty(node_1, "ddl:datatypeLength", "64");
        verifyMixinType(node_1, "ddl:columnDefinition");

        Node node_2 = tableNode.getNode("MDL_TYPE");
        assertNotNull(node_2);
        verifyPrimaryType(node_2, "nt:unstructured");
        verifyProperty(node_2, "ddl:datatypeName", "NUMERIC");
        verifyProperty(node_2, "ddl:datatypePrecision", "3");
        verifyProperty(node_2, "ddl:datatypeScale", "0");
        verifyHasProperty(node_2, "ddl:datatypeLength");
        verifyMixinType(node_2, "ddl:columnDefinition");
    }

    @Ignore
    @Test
    public void shouldSequenceDerbyDdl() throws Exception {
        Node statementsNode = sequenceDdl("ddl/dialect/derby/derby_test_statements.ddl");
        assertThat(statementsNode.getNodes().getSize(), is(64l));
        verifyPrimaryType(statementsNode, "nt:unstructured");
        verifyProperty(statementsNode, "ddl:parserId", "DERBY");

        // CREATE INDEX IXSALE ON SAMP.SALES (SALES);
        // <name = "IXSALE" startLineNumber = "87" primaryType = "nt:unstructured" uuid = "85740506-ac6e-46f3-8118-be0c9eb1bc57"
        // startColumnNumber = "1" mixinTypes = "derbyddl:createIndexStatement" tableName = "SAMP.SALES" expression =
        // "CREATE INDEX IXSALE ON SAMP.SALES (SALES);" unique = "false" startCharIndex = "2886">

        Node indexNode = statementsNode.getNode("IXSALE");
        assertNotNull(indexNode);
        verifyBaseProperties(indexNode, "nt:unstructured", "87", "1", "2886", 0);
        verifyMixinType(indexNode, "derbyddl:createIndexStatement");

        // CREATE SCHEMA FLIGHTS AUTHORIZATION anita;
        // <name = "FLIGHTS" startLineNumber = "98" primaryType = "nt:unstructured" uuid = "e1c8227d-c663-4d4b-8506-b1e41aa1f7f6"
        // startColumnNumber = "1" mixinTypes = "ddl:createSchemaStatement" expression =
        // "CREATE SCHEMA FLIGHTS AUTHORIZATION anita;" startCharIndex = "3218">
        Node schemaNode = statementsNode.getNode("FLIGHTS");
        assertNotNull(schemaNode);
        verifyBaseProperties(schemaNode, "nt:unstructured", "98", "1", "3218", 0);
        verifyMixinType(schemaNode, "ddl:createSchemaStatement");
        verifyExpression(schemaNode, "CREATE SCHEMA FLIGHTS AUTHORIZATION anita;");

        // DROP PROCEDURE some_procedure_name;
        // <name = "unknownStatement[3]" startLineNumber = "172" primaryType = "nt:unstructured" uuid =
        // "1f57c0ec-4361-4f64-963d-dcb919c27656" startColumnNumber = "1" mixinTypes = "ddl:unknownStatement" expression =
        // "DROP PROCEDURE some_procedure_name;" startCharIndex = "5438">
        Node unknownNode_1 = statementsNode.getNode("some_procedure_name");
        assertNotNull(unknownNode_1);
        verifyBaseProperties(unknownNode_1, "nt:unstructured", "172", "1", "5438", 0);
        verifyMixinType(unknownNode_1, "derbyddl:dropProcedureStatement");
        verifyExpression(unknownNode_1, "DROP PROCEDURE some_procedure_name;");

        // ALTER TABLE SAMP.DEPARTMENT
        // ADD CONSTRAINT NEW_UNIQUE UNIQUE (DEPTNO);

        // <name = "SAMP.DEPARTMENT" startLineNumber = "16" primaryType = "nt:unstructured" uuid =
        // "b3c54a3c-4779-48a0-a2f3-80e5079bb62c" startColumnNumber = "1" mixinTypes = "ddl:alterTableStatement" expression =
        // "ALTER TABLE SAMP.DEPARTMENT
        // ADD CONSTRAINT NEW_UNIQUE UNIQUE (DEPTNO);
        //
        // -- add a new foreign key constraint to the
        // -- Cities table. Each row in Cities is checked
        // -- to make sure it satisfied the constraints.
        // -- if any rows don't satisfy the constraint, the
        // -- constraint is not added" startCharIndex = "478">
        // <name = "NEW_UNIQUE" primaryType = "nt:unstructured" uuid = "c678bb9d-a0ac-4f69-b8a0-ec890557bc20" mixinTypes =
        // "ddl:addTableConstraintDefinition" constraintType = "0">
        // <name = "DEPTNO" primaryType = "nt:unstructured" uuid = "0f019782-e72d-485a-8f8c-e62954fd6ae6" mixinTypes =
        // "ddl:columnReference">
        Node alterTableNode = statementsNode.getNode("SAMP.DEPARTMENT");
        assertNotNull(alterTableNode);
        verifyBaseProperties(alterTableNode, "nt:unstructured", "16", "1", "478", 1);
        verifyMixinType(alterTableNode, "ddl:alterTableStatement");

        Node uniqueNode = alterTableNode.getNode("NEW_UNIQUE");
        assertNotNull(uniqueNode);
        verifyPrimaryType(uniqueNode, "nt:unstructured");
        verifyProperty(uniqueNode, "ddl:constraintType", "0");
        verifyMixinType(uniqueNode, "ddl:addTableConstraintDefinition");

        // One column reference
        assertThat(uniqueNode.getNodes().getSize(), is(1l));
        Node colRefNode = uniqueNode.getNode("DEPTNO");
        assertNotNull(colRefNode);
        verifyPrimaryType(colRefNode, "nt:unstructured");
        verifyMixinType(colRefNode, "ddl:columnReference");
    }

    @Test
    public void shouldSequenceOracleDdl() throws Exception {
        Node statementsNode = sequenceDdl("ddl/dialect/oracle/oracle_test_statements_2.ddl");
        assertThat(statementsNode.getNodes().getSize(), is(50l));
        verifyPrimaryType(statementsNode, "nt:unstructured");
        verifyProperty(statementsNode, "ddl:parserId", "ORACLE");

        // <name = "CREATE OR REPLACE DIRECTORY" startLineNumber = "164" primaryType = "nt:unstructured" uuid =
        // "c45eb2bb-1b85-469d-9dfc-0012fdfd8ac4" startColumnNumber = "1" mixinTypes = "oracleddl:createDirectoryStatement"
        // expression = "CREATE OR REPLACE DIRECTORY bfile_dir AS '/private1/LOB/files';" startCharIndex = "3887">
        Node createOrReplDirNode = statementsNode.getNode("CREATE OR REPLACE DIRECTORY");
        assertNotNull(createOrReplDirNode);
        verifyBaseProperties(createOrReplDirNode, "nt:unstructured", "164", "1", "3886", 0);
        verifyMixinType(createOrReplDirNode, "oracleddl:createDirectoryStatement");

        // <name = "countries" startLineNumber = "9" primaryType = "nt:unstructured" uuid = "70f45acc-57b0-41c9-b166-bcba4f8c75b8"
        // startColumnNumber = "1" mixinTypes = "ddl:alterTableStatement" expression = "ALTER TABLE countries
        // ADD (duty_pct NUMBER(2,2) CHECK (duty_pct < 10.5),
        // visa_needed VARCHAR2(3));" startCharIndex = "89">
        // <name = "duty_pct" datatypeName = "NUMBER" primaryType = "nt:unstructured" uuid =
        // "20079cae-5de1-425c-925e-df230410ea69" datatypePrecision = "2" mixinTypes = "ddl:columnDefinition" datatypeScale = "2">
        // <name = "CHECK_1" primaryType = "nt:unstructured" name = "CHECK_1" uuid = "210039d7-ebe7-47a8-94be-c3adb70b2885"
        // mixinTypes = "ddl:addTableConstraintDefinition" constraintType = "3" searchCondition = "( duty_pct < 10 . 5 )">
        // <name = "visa_needed" datatypeName = "VARCHAR2" datatypeLength = "3" primaryType = "nt:unstructured" uuid =
        // "b7b6bf1d-6a2b-411a-aa63-974d13ba20e8" mixinTypes = "ddl:columnDefinition">
        Node countriesNode = statementsNode.getNode("countries");
        assertNotNull(countriesNode);
        verifyBaseProperties(countriesNode, "nt:unstructured", "9", "1", "89", 3);
        verifyMixinType(countriesNode, "ddl:alterTableStatement");

        Node duty_pct_node = countriesNode.getNode("duty_pct");
        assertNotNull(duty_pct_node);
        verifyPrimaryType(duty_pct_node, "nt:unstructured");
        verifyProperty(duty_pct_node, "ddl:datatypeName", "NUMBER");
        verifyProperty(duty_pct_node, "ddl:datatypePrecision", "2");
        verifyProperty(duty_pct_node, "ddl:datatypeScale", "2");
        assertThat(verifyHasProperty(duty_pct_node, "ddl:datatypeLength"), is(false));
        verifyMixinType(duty_pct_node, "ddl:columnDefinition");

        Node check_1_node = countriesNode.getNode("CHECK_1");
        assertNotNull(check_1_node);
        verifyPrimaryType(check_1_node, "nt:unstructured");
        verifyProperty(check_1_node, "ddl:constraintType", "CHECK");
        verifyMixinType(check_1_node, "ddl:addTableConstraintDefinition");
        verifyProperty(check_1_node, "ddl:searchCondition", "( duty_pct < 10 . 5 )");

        Node visa_needed_node = countriesNode.getNode("visa_needed");
        assertNotNull(visa_needed_node);
        verifyPrimaryType(visa_needed_node, "nt:unstructured");
        verifyProperty(visa_needed_node, "ddl:datatypeName", "VARCHAR2");
        verifyProperty(visa_needed_node, "ddl:datatypeLength", "3");
        verifyMixinType(visa_needed_node, "ddl:columnDefinition");

        // <name = "app_user1" startLineNumber = "33" primaryType = "nt:unstructured" uuid =
        // "8c660ae8-2078-4263-a0e7-8f517cefd3a0" startColumnNumber = "1" mixinTypes = "oracleddl:alterUserStatement" expression =
        // "ALTER USER app_user1
        // GRANT CONNECT THROUGH sh
        // WITH ROLE warehouse_user;" startCharIndex = "624">

        Node app_user1Node = statementsNode.getNode("app_user1");
        assertNotNull(app_user1Node);
        verifyBaseProperties(app_user1Node, "nt:unstructured", "33", "1", "624", 0);
        verifyMixinType(app_user1Node, "oracleddl:alterUserStatement");
    }

    private Node sequenceDdl( String ddlFile ) throws Exception {
        String fileName = ddlFile.substring(ddlFile.lastIndexOf("/") + 1);
        createNodeWithContentFromFile(fileName, ddlFile);
        Node sequencedNode = getSequencedNode(rootNode, "ddl/" + fileName);

        assertNotNull(sequencedNode);
        assertThat(sequencedNode.getNodes().getSize(), is(1l));

        Node statementsNode = sequencedNode.getNode("ddl:statements");
        assertNotNull(statementsNode);
        return statementsNode;
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
