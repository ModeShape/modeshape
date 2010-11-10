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

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.CONSTRAINT_ATTRIBUTE_TYPE;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.CONSTRAINT_TYPE;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DATATYPE_LENGTH;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DATATYPE_NAME;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DATATYPE_PRECISION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DATATYPE_SCALE;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DEFAULT_OPTION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DEFAULT_VALUE;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.DROP_BEHAVIOR;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.NULLABLE;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.PROPERTY_VALUE;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_ADD_TABLE_CONSTRAINT_DEFINITION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_ALTER_TABLE_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_COLUMN_DEFINITION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_COLUMN_REFERENCE;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_CREATE_SCHEMA_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_CREATE_VIEW_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_ASSERTION_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_CHARACTER_SET_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_COLLATION_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_DOMAIN_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_SCHEMA_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_TABLE_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_TRANSLATION_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_VIEW_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_STATEMENT_OPTION;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_TABLE_CONSTRAINT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.VALUE;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.graph.property.Property;
import org.modeshape.sequencer.ddl.node.AstNode;

public class StandardDdlParserTest extends DdlParserTestHelper {

    public static final String DDL_FILE_PATH = "src/test/resources/ddl/";
    public static final String MM_DDL_FILE_PATH = "src/test/resources/mmddl/";

    @Before
    public void beforeEach() {
        parser = new StandardDdlParser();
        setPrintToConsole(false);
        parser.setTestMode(isPrintToConsole());
        rootNode = parser.nodeFactory().node("ddlRootNode");
        parser.setRootNode(rootNode);
        scorer = new DdlParserScorer();
    }

    @After
    public void afterEach() {
        rootNode.removeAllChildren();
        rootNode = null;
        parser = null;
    }

    private DdlTokenStream getTokens( String content ) {
        DdlTokenStream tokens = new DdlTokenStream(content, DdlTokenStream.ddlTokenizer(true), false);

        tokens.start();

        return tokens;
    }

    private void printNodeChildren( AstNode node ) {
        if (isPrintToConsole()) {
            int count = 1;
            for (AstNode childNode : node.getChildren()) {
                System.out.println("NODE(" + (count++) + ")  NAME = " + childNode.getName().getString());
            }
        }
    }

    @Test
    public void shouldParseName() {
        printTest("shouldParseName()");

        String content = "simpleName";
        String targetName = "simpleName";
        DdlTokenStream tokens = getTokens(content);
        String result = parser.parseName(tokens);
        assertEquals(targetName, result);

        content = "schema_name.table_name";
        targetName = "schema_name.table_name";
        tokens = getTokens(content);
        result = parser.parseName(tokens);
        assertEquals(targetName, result);

        content = "[schema_name].[table_name]";
        targetName = "schema_name.table_name";
        tokens = getTokens(content);
        result = parser.parseName(tokens);
        assertEquals(targetName, result);

        content = "[schema_name].[\"COLUMN\"]";
        targetName = "schema_name.COLUMN";
        tokens = getTokens(content);
        result = parser.parseName(tokens);
        assertEquals(targetName, result);

        content = "\"_RETURN\"";
        targetName = "_RETURN";
        tokens = getTokens(content);
        result = parser.parseName(tokens);
        assertEquals(targetName, result);
    }

    @Test
    public void isTableConstraint() {
        printTest("isTableConstraint()");

        String content = "PRIMARY KEY";
        DdlTokenStream tokens = getTokens(content);
        boolean result = parser.isTableConstraint(tokens);
        assertTrue(result);

        content = "FOREIGN KEY";
        tokens = getTokens(content);
        result = parser.isTableConstraint(tokens);
        assertTrue(result);

        content = "UNIQUE";
        tokens = getTokens(content);
        result = parser.isTableConstraint(tokens);
        assertTrue(result);

        content = "CONSTRAINT constraint_name PRIMARY KEY";
        tokens = getTokens(content);
        result = parser.isTableConstraint(tokens);
        assertTrue(result);

        content = "CONSTRAINT constraint_name FOREIGN KEY";
        tokens = getTokens(content);
        result = parser.isTableConstraint(tokens);
        assertTrue(result);

        content = "CONSTRAINT constraint_name UNIQUE";
        tokens = getTokens(content);
        result = parser.isTableConstraint(tokens);
        assertTrue(result);

        content = "XXXXX";
        tokens = getTokens(content);
        result = parser.isTableConstraint(tokens);
        assertFalse(result);

        content = "CONSTRAINT constraint_name XXXXX";
        tokens = getTokens(content);
        result = parser.isTableConstraint(tokens);
        assertFalse(result);
    }

    @Test
    public void shouldParseCreateStatement() {
        printTest("shouldParseCreateStatement()");

        String content = "CREATE TABLE EQPT_TYPE ( CAT_CODE CHAR(7) NOT NULL);";

        Object result = parser.score(content, null, scorer);
        parser.parse(content, rootNode, result);
    }

    @Test
    public void shouldParseCreateBogusStatement() {
        printTest("shouldParseCreateBogusStatement()");

        String content = "CREATE TABLE myTable (PART_COLOR varchar(255) NOT NULL DEFAULT BLUE" + ", PRIMARY KEY (PART_COLOR)"
                         + ", PART_ID int NOT NULL DEFAULT (1000));" + "CREATE BOGUS more JUNK after this JUNK;"
                         + "CREATE LOCAL TEMPORARY TABLE yourTable (PART_COLOR varchar(255) NOT NULL DEFAULT BLUE);";

        parser.setDoUseTerminator(true);
        Object result = parser.score(content, null, scorer);
        parser.parse(content, rootNode, result);

        printNodeChildren(rootNode);

        assertEquals(0, parser.getProblems().size());

        // assertTrue(parser.getProblems().get(0).getMessage().startsWith("Tokens were found at line"));

        assertEquals(3, rootNode.getChildCount()); // TWO STATEMENTS + UNKNOWN statement
        AstNode tableNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(tableNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_TABLE_STATEMENT));
        assertEquals("myTable", tableNode.getName().getString());
        assertEquals(3, tableNode.getChildCount());
    }

    @Test
    public void shouldParseCreateStatementWithUnusedTokens() {
        printTest("shouldParseCreateStatementWithUnusedTokens()");

        String content = "CREATE TABLE myTable (PART_COLOR varchar(255) NOT NULL DEFAULT BLUE BOGUS TOKENS"
                         + ", PRIMARY KEY (PART_COLOR)" + ", PART_ID int NOT NULL DEFAULT (1000));";

        parser.setDoUseTerminator(true);
        Object result = parser.score(content, null, scorer);
        parser.parse(content, rootNode, result);

        printNodeChildren(rootNode);

        assertEquals(2, rootNode.getChildCount()); // ONE TABLE + ONE PROBLEM
        AstNode tableNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(tableNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_TABLE_STATEMENT));
        assertEquals("myTable", tableNode.getName().getString());
        assertEquals(4, tableNode.getChildCount()); // 2 Columns, 1 Constraint + 1 Problem

        assertEquals(1, parser.getProblems().size());

        assertTrue(parser.getProblems().get(0).getMessage().startsWith("The following unused tokens were found"));
    }

    @Test
    public void shouldParseAlterTableDefinitionAddConstraintFK() {
        printTest("shouldParseAlterTableDefinition_1()");

        String content = "ALTER TABLE table_name_14 ADD CONSTRAINT fk_name FOREIGN KEY (ref_col_name) REFERENCES ref_table_name(ref_table_column_name);";

        Object result = parser.score(content, null, scorer);
        parser.parse(content, rootNode, result);

        assertEquals(1, rootNode.getChildCount());
        AstNode alterTableNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(alterTableNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_TABLE_STATEMENT));
        assertEquals("table_name_14", alterTableNode.getName().getString());
        assertEquals(1, alterTableNode.getChildCount());
        AstNode constraintNode = alterTableNode.getChild(0); // / FOREIGN KEY
        assertEquals("fk_name", constraintNode.getName().getString());
        assertEquals(DdlConstants.FOREIGN_KEY, constraintNode.getProperty(CONSTRAINT_TYPE).getFirstValue());
        assertTrue(hasMixinType(constraintNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ADD_TABLE_CONSTRAINT_DEFINITION));

        // Expect 3 references: COLUMN REFERENCE for this table, External Table reference and column reference in external table
        assertEquals(3, constraintNode.getChildCount());
    }

    @Test
    public void shouldParseCreateSchemaWithNestedTable() {
        printTest("shouldParseSchemaDefinitionStatements()");

        String content = "CREATE SCHEMA hollywood \n" + SPACE
                         + "     CREATE TABLE films (title varchar(255), release date, producerName varchar(255))\n" + SPACE
                         + "     GRANT SELECT ON TABLE films TO producer_role " + SPACE + "     CREATE VIEW winners AS\n" + SPACE
                         + "             SELECT title, release FROM films WHERE producerName IS NOT NULL;";

        Object result = parser.score(content, null, scorer);
        parser.parse(content, rootNode, result);

        assertThat(rootNode.getChildCount(), is(1));

        List<AstNode> schemaNodes = parser.nodeFactory().getChildrenForType(rootNode, TYPE_CREATE_SCHEMA_STATEMENT);
        assertThat(schemaNodes.size(), is(1));
        assertThat(schemaNodes.get(0).getChildCount(), is(3));
        assertThat(schemaNodes.get(0).getName().getString(), is("hollywood"));
        List<AstNode> tableNodes = parser.nodeFactory().getChildrenForType(schemaNodes.get(0), TYPE_CREATE_TABLE_STATEMENT);
        assertThat(tableNodes.size(), is(1));
        assertThat(tableNodes.get(0).getName().getString(), is("films"));
        List<AstNode> viewNodes = parser.nodeFactory().getChildrenForType(schemaNodes.get(0), TYPE_CREATE_VIEW_STATEMENT);
        assertThat(viewNodes.size(), is(1));
        assertThat(viewNodes.get(0).getName().getString(), is("winners"));
        List<AstNode> columnNodes = parser.nodeFactory().getChildrenForType(tableNodes.get(0), TYPE_COLUMN_DEFINITION);
        assertThat(columnNodes.size(), is(3));

    }

    @Test
    public void shouldParseSchemaDefinitionStatements() {
        printTest("shouldParseSchemaDefinitionStatements()");

        String content = getFileContent(DDL_FILE_PATH + "schema_definition.ddl");
        Object result = parser.score(content, "schema_definition.ddl", scorer);
        parser.parse(content, rootNode, result);

        assertEquals(13, rootNode.getChildCount());

        List<AstNode> schemaNodes = parser.nodeFactory().getChildrenForType(rootNode, TYPE_CREATE_SCHEMA_STATEMENT);

        assertEquals(5, schemaNodes.size());

    }

    @Test
    public void shouldParseSchemaManipulationStatements() {
        printTest("shouldParseSchemaManipulationStatements()");

        String content = getFileContent(DDL_FILE_PATH + "schema_manipulation.ddl");
        Object result = parser.score(content, "schema_manipulation.ddl", scorer);
        parser.parse(content, rootNode, result);

        assertEquals(31, rootNode.getChildCount());

        List<AstNode> theNodes = parser.nodeFactory().getChildrenForType(rootNode, TYPE_ALTER_TABLE_STATEMENT);

        assertEquals(14, theNodes.size());

        theNodes = parser.nodeFactory().getChildrenForType(rootNode, TYPE_DROP_TABLE_STATEMENT);

        assertEquals(2, theNodes.size());
    }

    @Test
    public void shouldParseTableDefinitionStatements() {
        printTest("shouldParseTableDefinitionStatements()");

        String content = getFileContent(DDL_FILE_PATH + "table_definition.ddl");
        Object result = parser.score(content, "table_definition.ddl", scorer);
        parser.parse(content, rootNode, result);
    }

    @Test
    public void shoudParseUntilTerminated() {
        printTest("shoudParseUntilTerminated()");
        String prefix = " ( COL_SUM, COL_DIFF ) AS SELECT COMM + BONUS, COMM - BONUS FROM SAMP.EMPLOYEE";
        String content = prefix + "; CREATE TABLE someName";
        DdlTokenStream tokens = getTokens(content);

        String result = parser.parseUntilTerminator(tokens);
        printTest(result);
        assertEquals(prefix, result);
    }

    @Test
    public void shouldParseColumnDefinition() {
        printTest("shouldParseColumnDefinition()");

        // CASE 1
        String content = "PARTID VARCHAR (255) NOT NULL DEFAULT '12345'";

        DdlTokenStream tokens = getTokens(content);

        AstNode tableNode = parser.nodeFactory().node("PARTID", rootNode, TYPE_CREATE_TABLE_STATEMENT, rootNode);
        parser.setRootNode(rootNode);
        parser.parseColumnDefinition(tokens, tableNode, false);

        assertEquals(1, tableNode.getChildCount());
        AstNode column = tableNode.getChildren().get(0);
        assertEquals("VARCHAR", column.getProperty(DATATYPE_NAME).getFirstValue());
        assertEquals(255L, column.getProperty(DATATYPE_LENGTH).getFirstValue());
        assertTrue(column.getProperty(DATATYPE_PRECISION) == null);
        assertEquals("NOT NULL", column.getProperty(NULLABLE).getFirstValue());
        assertEquals(DEFAULT_ID_LITERAL, column.getProperty(DEFAULT_OPTION).getFirstValue());
        assertEquals("'12345'", column.getProperty(DEFAULT_VALUE).getFirstValue());

        tableNode.removeAllChildren();

        // CASE 2
        content = "PARTID CHARACTER (255) NULL DEFAULT (12345)";

        tokens = getTokens(content);

        parser.parseColumnDefinition(tokens, tableNode, false);

        assertEquals(1, tableNode.getChildCount());
        column = tableNode.getChildren().get(0);
        assertEquals("CHARACTER", column.getProperty(DATATYPE_NAME).getFirstValue());
        assertEquals(255L, column.getProperty(DATATYPE_LENGTH).getFirstValue());
        assertTrue(column.getProperty(DATATYPE_PRECISION) == null);
        assertEquals("NULL", column.getProperty(NULLABLE).getFirstValue());
        assertEquals(DEFAULT_ID_LITERAL, column.getProperty(DEFAULT_OPTION).getFirstValue());
        assertEquals("12345", column.getProperty(DEFAULT_VALUE).getFirstValue());

        tableNode.removeAllChildren();

        // CASE 3
        content = "PARTID DECIMAL (6, 2) DEFAULT (6.213)";

        tokens = getTokens(content);

        parser.parseColumnDefinition(tokens, tableNode, false);

        assertEquals(1, tableNode.getChildCount());
        column = tableNode.getChildren().get(0);
        assertEquals("DECIMAL", column.getProperty(DATATYPE_NAME).getFirstValue());
        assertEquals(6, column.getProperty(DATATYPE_PRECISION).getFirstValue());
        assertEquals(2, column.getProperty(DATATYPE_SCALE).getFirstValue());
        assertTrue(column.getProperty(DATATYPE_LENGTH) == null);
        assertEquals(DEFAULT_ID_LITERAL, column.getProperty(DEFAULT_OPTION).getFirstValue());
        assertEquals("6.213", column.getProperty(DEFAULT_VALUE).getFirstValue());
    }

    @FixFor( "MODE-820" )
    @Test
    public void shouldParseColumnDefinitionWithKilobyteInSize() {
        printTest("shouldParseColumnDefinitionWithKilobyteInSize()");

        String content = "PARTID CHAR(2K) NOT NULL";

        DdlTokenStream tokens = getTokens(content);

        AstNode tableNode = parser.nodeFactory().node("PARTID", rootNode, TYPE_CREATE_TABLE_STATEMENT, rootNode);
        parser.setRootNode(rootNode);
        parser.parseColumnDefinition(tokens, tableNode, false);

        assertEquals(1, tableNode.getChildCount());
        AstNode column = tableNode.getChildren().get(0);
        assertEquals("CHAR", column.getProperty(DATATYPE_NAME).getFirstValue());
        assertEquals(2048L, column.getProperty(DATATYPE_LENGTH).getFirstValue());
        assertTrue(column.getProperty(DATATYPE_PRECISION) == null);
        assertEquals("NOT NULL", column.getProperty(NULLABLE).getFirstValue());

        tableNode.removeAllChildren();
    }

    @FixFor( "MODE-820" )
    @Test
    public void shouldParseColumnDefinitionWithMegabyteInSize() {
        printTest("shouldParseColumnDefinitionWithKilobyteInSize()");

        String content = "PARTID CHAR(4M) NOT NULL";

        DdlTokenStream tokens = getTokens(content);

        AstNode tableNode = parser.nodeFactory().node("PARTID", rootNode, TYPE_CREATE_TABLE_STATEMENT, rootNode);
        parser.setRootNode(rootNode);
        parser.parseColumnDefinition(tokens, tableNode, false);

        assertEquals(1, tableNode.getChildCount());
        AstNode column = tableNode.getChildren().get(0);
        assertEquals("CHAR", column.getProperty(DATATYPE_NAME).getFirstValue());
        assertEquals(4194304L, column.getProperty(DATATYPE_LENGTH).getFirstValue());
        assertTrue(column.getProperty(DATATYPE_PRECISION) == null);
        assertEquals("NOT NULL", column.getProperty(NULLABLE).getFirstValue());

        tableNode.removeAllChildren();
    }

    @FixFor( "MODE-820" )
    @Test
    public void shouldParseColumnDefinitionWithGigabyteInSize() {
        printTest("shouldParseColumnDefinitionWithKilobyteInSize()");

        String content = "PARTID CHAR(5G) NOT NULL";

        DdlTokenStream tokens = getTokens(content);

        AstNode tableNode = parser.nodeFactory().node("PARTID", rootNode, TYPE_CREATE_TABLE_STATEMENT, rootNode);
        parser.setRootNode(rootNode);
        parser.parseColumnDefinition(tokens, tableNode, false);

        assertEquals(1, tableNode.getChildCount());
        AstNode column = tableNode.getChildren().get(0);
        assertEquals("CHAR", column.getProperty(DATATYPE_NAME).getFirstValue());
        assertEquals(5368709120L, column.getProperty(DATATYPE_LENGTH).getFirstValue());
        assertTrue(column.getProperty(DATATYPE_PRECISION) == null);
        assertEquals("NOT NULL", column.getProperty(NULLABLE).getFirstValue());

        tableNode.removeAllChildren();
    }

    @Test
    public void shouldGetTableElementString() {
        printTest("shouldGetTableElementString()");

        String content = "(PARTID VARCHAR (255) NOT NULL DEFAULT (100),  -- COLUMN 1 COMMENT with comma \nPARTCOLOR INTEGER NOT NULL)";

        DdlTokenStream tokens = getTokens(content);

        String result = parser.getTableElementsString(tokens, false);

        printResult("   STRING = " + result);

    }

    @Test
    public void shouldParseCreateTable() {
        printTest("shouldParseCreateTable()");

        String content = "CREATE TABLE MY_TABLE_A (PARTID VARCHAR (255) NOT NULL DEFAULT (100), "
                         + " -- COLUMN 1 COMMENT with comma \nPARTCOLOR INTEGER NOT NULL) ON COMMIT DELETE ROWS;";

        DdlTokenStream tokens = getTokens(content);

        parser.setRootNode(rootNode);

        AstNode result = parser.parseCreateTableStatement(tokens, rootNode);

        assertEquals(1, rootNode.getChildCount());
        AstNode tableNode = rootNode.getChildren().get(0);
        assertThat(result, is(tableNode));
        assertEquals("MY_TABLE_A", tableNode.getName().getString());
        assertEquals(3, tableNode.getChildCount()); // 2 COLUMNS + 1 Table Option
        AstNode column = tableNode.getChildren().get(0);
        assertEquals("PARTID", column.getName().getString());
        assertEquals("VARCHAR", column.getProperty(DATATYPE_NAME).getFirstValue());
        assertEquals(255L, column.getProperty(DATATYPE_LENGTH).getFirstValue());
        assertTrue(column.getProperty(DATATYPE_PRECISION) == null);

        List<AstNode> tableOptions = parser.nodeFactory().getChildrenForType(tableNode, TYPE_STATEMENT_OPTION);
        assertEquals(1, tableOptions.size());
        assertEquals("ON COMMIT DELETE ROWS", tableOptions.get(0).getProperty(VALUE).getFirstValue());

    }

    @FixFor( "MODE-820" )
    @Test
    public void shouldParseCreateTableWithKilobyteInSize() {
        printTest("shouldParseCreateTableWithKilobyteInSize()");

        String content = "CREATE TABLE MY_TABLE_A (PARTID CHAR (2K) NOT NULL, "
                         + " -- COLUMN 1 COMMENT with comma \nPARTCOLOR CHAR(4M) NOT NULL) ON COMMIT DELETE ROWS;";

        DdlTokenStream tokens = getTokens(content);

        parser.setRootNode(rootNode);

        AstNode result = parser.parseCreateTableStatement(tokens, rootNode);

        assertEquals(1, rootNode.getChildCount());
        AstNode tableNode = rootNode.getChildren().get(0);
        assertThat(result, is(tableNode));
        assertEquals("MY_TABLE_A", tableNode.getName().getString());
        assertEquals(3, tableNode.getChildCount()); // 2 COLUMNS + 1 Table Option
        AstNode column1 = tableNode.getChildren().get(0);
        assertEquals("PARTID", column1.getName().getString());
        assertEquals("CHAR", column1.getProperty(DATATYPE_NAME).getFirstValue());
        assertEquals(2048L, column1.getProperty(DATATYPE_LENGTH).getFirstValue());
        assertTrue(column1.getProperty(DATATYPE_PRECISION) == null);
        AstNode column2 = tableNode.getChildren().get(1);
        assertEquals("PARTCOLOR", column2.getName().getString());
        assertEquals("CHAR", column2.getProperty(DATATYPE_NAME).getFirstValue());
        assertEquals(4194304L, column2.getProperty(DATATYPE_LENGTH).getFirstValue());
        assertTrue(column2.getProperty(DATATYPE_PRECISION) == null);

        List<AstNode> tableOptions = parser.nodeFactory().getChildrenForType(tableNode, TYPE_STATEMENT_OPTION);
        assertEquals(1, tableOptions.size());
        assertEquals("ON COMMIT DELETE ROWS", tableOptions.get(0).getProperty(VALUE).getFirstValue());
    }

    @Test
    public void shouldParseNextTableOption() {
        printTest("shouldParseNextTableOption()");

        // CASE 1 ON COMMIT PRESERVE ROWS
        String content = "ON COMMIT PRESERVE ROWS";

        DdlTokenStream tokens = getTokens(content);

        AstNode tableNode = parser.nodeFactory().node("PARTS_COLOR", rootNode, TYPE_CREATE_TABLE_STATEMENT);

        parser.parseNextCreateTableOption(tokens, tableNode);

        List<AstNode> options = parser.nodeFactory().getChildrenForType(tableNode, TYPE_STATEMENT_OPTION);
        assertEquals(1, options.size());

        assertEquals(content, options.get(0).getProperty(VALUE).getFirstValue());

        tableNode.removeAllChildren();

        // CASE 2 ON COMMIT DELETE ROWS
        content = "ON COMMIT DELETE ROWS";
        tokens = getTokens(content);

        parser.parseNextCreateTableOption(tokens, tableNode);

        options = parser.nodeFactory().getChildrenForType(tableNode, TYPE_STATEMENT_OPTION);
        assertEquals(1, options.size());

        assertEquals(content, options.get(0).getProperty(VALUE).getFirstValue());

        tableNode.removeAllChildren();

        // CASE 3 ON COMMIT DROP
        content = "ON COMMIT DROP";
        tokens = getTokens(content);

        parser.parseNextCreateTableOption(tokens, tableNode);

        options = parser.nodeFactory().getChildrenForType(tableNode, TYPE_STATEMENT_OPTION);
        assertEquals(1, options.size());

        assertEquals(content, options.get(0).getProperty(VALUE).getFirstValue());

        tableNode.removeAllChildren();
    }

    @Test
    public void shouldAreNextTokensCreateTableOptions() {
        printTest("shouldAreNextTokensCreateTableOptions()");

        // CASE 1 ON COMMIT PRESERVE ROWS
        String content = "ON COMMIT PRESERVE ROWS";

        DdlTokenStream tokens = getTokens(content);

        assertTrue(parser.areNextTokensCreateTableOptions(tokens));

        content = "OFF COMMIT PRESERVE ROWS";

        tokens = getTokens(content);

        assertFalse(parser.areNextTokensCreateTableOptions(tokens));
    }

    @Test
    public void shouldParseConstraintAttributes() {
        printTest("shouldParseConstraintAttributes()");
        // <constraint attributes> ::=
        // <constraint check time> [ [ NOT ] DEFERRABLE ]
        // | [ NOT ] DEFERRABLE [ <constraint check time> ]
        //
        // <constraint check time> ::=
        // INITIALLY DEFERRED
        // | INITIALLY IMMEDIATE

        // CASE 1: INITIALLY DEFERRED
        String content = "INITIALLY DEFERRED";

        DdlTokenStream tokens = getTokens(content);

        AstNode constraintNode = parser.nodeFactory().node("FK_1", rootNode, TYPE_TABLE_CONSTRAINT);

        parser.parseConstraintAttributes(tokens, constraintNode);

        assertEquals(1, constraintNode.getChildCount()); // ONE CHILD

        List<AstNode> attributes = parser.nodeFactory().getChildrenForType(constraintNode, CONSTRAINT_ATTRIBUTE_TYPE);
        assertEquals(1, attributes.size());
        assertEquals(content, attributes.get(0).getProperty(PROPERTY_VALUE).getFirstValue());

        constraintNode.removeAllChildren();

        // CASE 2: INITIALLY IMMEDIATE DEFERRABLE
        content = "INITIALLY IMMEDIATE DEFERRABLE";
        tokens = getTokens(content);

        parser.parseConstraintAttributes(tokens, constraintNode);

        assertEquals(2, constraintNode.getChildCount()); // TWO CHILDREN

        attributes = parser.nodeFactory().getChildrenForType(constraintNode, CONSTRAINT_ATTRIBUTE_TYPE);
        assertEquals(2, attributes.size());
        assertEquals("INITIALLY IMMEDIATE", attributes.get(0).getProperty(PROPERTY_VALUE).getFirstValue());
        assertEquals("DEFERRABLE", attributes.get(1).getProperty(PROPERTY_VALUE).getFirstValue());

        constraintNode.removeAllChildren();

        // CASE 3: NOT DEFERRABLE INITIALLY IMMEDIATE
        content = "NOT DEFERRABLE INITIALLY IMMEDIATE";
        tokens = getTokens(content);

        parser.parseConstraintAttributes(tokens, constraintNode);

        assertEquals(2, constraintNode.getChildCount()); // 2 Children

        attributes = parser.nodeFactory().getChildrenForType(constraintNode, CONSTRAINT_ATTRIBUTE_TYPE);
        assertEquals(2, attributes.size());
        assertEquals("NOT DEFERRABLE", attributes.get(0).getProperty(PROPERTY_VALUE).getFirstValue());
        assertEquals("INITIALLY IMMEDIATE", attributes.get(1).getProperty(PROPERTY_VALUE).getFirstValue());

        constraintNode.removeAllChildren();

    }

    @Test
    public void shouldParseColumnsAndConstraints() {
        printTest("shouldParseColumnsAndConstraints()");

        String content = "(PART_COLOR varchar(255) NOT NULL DEFAULT BLUE" + ", PRIMARY KEY (PART_COLOR)"
                         + ", PART_ID int NOT NULL DEFAULT (1000)"
                         + ", FOREIGN KEY FK_SUPPLIER REFERENCES SUPPLIERS (SUPPLIER_ID, SUPPLIER_NAME) );";

        DdlTokenStream tokens = getTokens(content);

        AstNode tableNode = parser.nodeFactory().node("PART_COLOR", rootNode, TYPE_CREATE_TABLE_STATEMENT);

        parser.parseColumnsAndConstraints(tokens, tableNode);

        assertEquals(4, tableNode.getChildCount());

        AstNode column = parser.nodeFactory().getChildforNameAndType(tableNode, "PART_COLOR", TYPE_COLUMN_DEFINITION);
        assertNotNull(column);
        assertEquals("VARCHAR", column.getProperty(DATATYPE_NAME).getFirstValue());
        assertEquals(255L, column.getProperty(DATATYPE_LENGTH).getFirstValue());
        assertTrue(column.getProperty(DATATYPE_PRECISION) == null);
        assertTrue(column.getProperty(DATATYPE_SCALE) == null);
        assertEquals("NOT NULL", column.getProperty(NULLABLE).getFirstValue());
        assertEquals(DEFAULT_ID_LITERAL, column.getProperty(DEFAULT_OPTION).getFirstValue());
        assertEquals("BLUE", column.getProperty(DEFAULT_VALUE).getFirstValue());

        AstNode foreignKeyNode = parser.nodeFactory().getChildforNameAndType(tableNode, "FK_SUPPLIER", TYPE_TABLE_CONSTRAINT);

        assertEquals(DdlConstants.FOREIGN_KEY, foreignKeyNode.getProperty(CONSTRAINT_TYPE).getFirstValue());
        assertEquals(3, foreignKeyNode.getChildCount()); // 2 COLUMN REFERENCES + 1 TABLE REFERENCE
    }

    @Test
    public void shouldParseCreateLocalTemporaryTable() {
        printTest("shouldParseCreateLocalTemporaryTable()");

        String tableName = "MY_TABLE_A";
        String content = "CREATE LOCAL TEMPORARY TABLE MY_TABLE_A (PARTID VARCHAR (255) NOT NULL DEFAULT (100),  -- COLUMN 1 COMMENT with comma \nPARTCOLOR INTEGER NOT NULL);";

        DdlTokenStream tokens = getTokens(content);

        AstNode result = parser.parseCreateTableStatement(tokens, rootNode);

        assertEquals(1, rootNode.getChildCount());
        AstNode tableNode = rootNode.getChildren().get(0);
        assertThat(result, is(tableNode));
        assertEquals(tableName, tableNode.getName().getString());
        assertEquals(2, tableNode.getChildCount());
        AstNode column = tableNode.getChildren().get(0);
        assertEquals("VARCHAR", column.getProperty(DATATYPE_NAME).getFirstValue());
        assertEquals(255L, column.getProperty(DATATYPE_LENGTH).getFirstValue());
        assertTrue(column.getProperty(DATATYPE_PRECISION) == null);
        assertEquals("NOT NULL", column.getProperty(NULLABLE).getFirstValue());
        assertEquals(DEFAULT_ID_LITERAL, column.getProperty(DEFAULT_OPTION).getFirstValue());
        assertEquals("100", column.getProperty(DEFAULT_VALUE).getFirstValue());
    }

    @Test
    public void shouldParseCreateTableWithConstraint() {
        printTest("shouldParseCreateTableWithConstraint()");

        String tableName = "MY_TABLE_B";
        String content = "CREATE TABLE MY_TABLE_B (PARTID VARCHAR (255), PRIMARY KEY (C1, C2), \nPARTCOLOR INTEGER NOT NULL, CONSTRAINT PK_A PRIMARY KEY (FILE_UID) );";

        DdlTokenStream tokens = getTokens(content);

        AstNode result = parser.parseCreateTableStatement(tokens, rootNode);

        assertEquals(1, rootNode.getChildCount());
        AstNode tableNode = rootNode.getChildren().get(0);
        assertThat(result, is(tableNode));
        assertEquals(tableName, tableNode.getName().getString());
        assertEquals(4, tableNode.getChildCount()); // 2 COLUMNs + 2 PRIMARY KEY CONSTRAINTS (BOGUS)
        AstNode column1 = tableNode.getChildren().get(0);
        assertEquals("VARCHAR", column1.getProperty(DATATYPE_NAME).getFirstValue());
        assertEquals(255L, column1.getProperty(DATATYPE_LENGTH).getFirstValue());
        assertTrue(column1.getProperty(DATATYPE_PRECISION) == null);
    }

    @Test
    public void shouldParseCreateTableWithInlineConstraint() {
        printTest("shouldParseCreateTableWithInlineConstraint()");

        String tableName = "table_name_22_B";
        String content = "CREATE TABLE table_name_22_B ( column_name_1 VARCHAR(255) CONSTRAINT pk_name UNIQUE INITIALLY IMMEDIATE);";

        DdlTokenStream tokens = getTokens(content);

        AstNode result = parser.parseCreateTableStatement(tokens, rootNode);

        assertEquals(1, rootNode.getChildCount()); // COLUMN + PRIMARY KEY CONSTRAINT
        AstNode tableNode = rootNode.getChildren().get(0);
        assertThat(result, is(tableNode));
        assertEquals(tableName, tableNode.getName().getString());
        assertEquals(2, tableNode.getChildCount()); // 1 COLUMN & 1 CONSTRAINT
        AstNode column = tableNode.getChildren().get(0);
        assertEquals("VARCHAR", column.getProperty(DATATYPE_NAME).getFirstValue());
        assertEquals(255L, column.getProperty(DATATYPE_LENGTH).getFirstValue());
        assertTrue(column.getProperty(DATATYPE_PRECISION) == null);
    }

    @Test
    public void shouldParseCreateTableWithInlineUniqueConstraint() {
        printTest("shouldParseCreateTableWithInlineUniqueConstraint()");

        String tableName = "table_name_22_B";
        String content = "CREATE TABLE table_name_22_B ( REALMUID     NUMERIC(10) NOT NULL CONSTRAINT PK_AUTHREALMS UNIQUE);";

        DdlTokenStream tokens = getTokens(content);

        AstNode result = parser.parseCreateTableStatement(tokens, rootNode);

        assertEquals(1, rootNode.getChildCount()); // COLUMN + PRIMARY KEY CONSTRAINT
        AstNode tableNode = rootNode.getChildren().get(0);
        assertThat(result, is(tableNode));
        assertEquals(tableName, tableNode.getName().getString());
        assertEquals(2, tableNode.getChildCount()); // 1 COLUMN & 1 CONSTRAINT
        AstNode column = tableNode.getChildren().get(0);
        assertEquals("NUMERIC", column.getProperty(DATATYPE_NAME).getFirstValue());
        assertEquals(10, column.getProperty(DATATYPE_PRECISION).getFirstValue());
        assertTrue(column.getProperty(DATATYPE_LENGTH) == null);

        AstNode prim_key = tableNode.getChildren().get(1);
        AstNode columnRef = prim_key.getChildren().get(0);
        assertEquals("REALMUID", columnRef.getName().getString());
    }

    @Test
    public void shouldParseCreateTableWithPrimaryKeyColumnConstraint() {
        printTest("shouldWork()");
        String PK_COL = "customerfamilyhistoryid";
        String content = "CREATE TABLE CustomerFamilyHistory (";
        content += PK_COL
                   + " numeric(10) CONSTRAINT customerfamilyhistoryid_pk PRIMARY KEY, firstname varchar(50) NOT NULL, lastname varchar(50) NOT NULL, age numeric(3), sibling varchar(20), customerid numeric(7) NOT NULL);";
        DdlTokenStream tokens = getTokens(content);
        AstNode result = parser.parseCreateTableStatement(tokens, rootNode);
        assertThat(result, is(notNullValue()));

        // test to make sure there is a table node
        assertEquals(1, rootNode.getChildCount()); // TABLE
        AstNode tableNode = rootNode.getChild(0);

        // test to make sure all columns and primary key constraint are children of the table node
        assertEquals(7, tableNode.getChildCount()); // 6 Columns + 1 Constraint

        // find constraint
        boolean foundConstraint = false;
        for (AstNode kid : tableNode.getChildren()) {
            Property value = kid.getProperty(CONSTRAINT_TYPE);

            if (value != null) {
                assertFalse(foundConstraint); // make sure no other constraint found
                foundConstraint = true;

                assertEquals(value.getFirstValue(), PRIMARY_KEY); // test for primary key

                // make sure a child node pointing to the column is found
                assertEquals(1, kid.getChildCount());
                AstNode child = kid.getChild(0);
                assertTrue(hasMixinType(child.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_COLUMN_REFERENCE));
                assertEquals(PK_COL, child.getName().getString());
            }
        }
    }

    @Test
    public void shouldParseCreateTableWithInlineUniqueConstraintWithColumns() {
        printTest("shouldParseCreateTableWithInlineUniqueConstraint()");

        String tableName = "table_name_24_B";
        String content = "CREATE TABLE table_name_24_B ( REALMUID     NUMERIC(10) NOT NULL CONSTRAINT PK_AUTHREALMS UNIQUE (columnA, columnB));";

        DdlTokenStream tokens = getTokens(content);

        AstNode result = parser.parseCreateTableStatement(tokens, rootNode);

        assertEquals(1, rootNode.getChildCount()); // COLUMN + PRIMARY KEY CONSTRAINT
        AstNode tableNode = rootNode.getChildren().get(0);
        assertThat(result, is(tableNode));
        assertEquals(tableName, tableNode.getName().getString());
        assertEquals(2, tableNode.getChildCount()); // 1 COLUMN & 1 CONSTRAINT
        AstNode column = tableNode.getChildren().get(0);
        assertEquals("NUMERIC", column.getProperty(DATATYPE_NAME).getFirstValue());
        assertEquals(10, column.getProperty(DATATYPE_PRECISION).getFirstValue());
        assertTrue(column.getProperty(DATATYPE_LENGTH) == null);

        AstNode prim_key = tableNode.getChildren().get(1);
        assertEquals(2, prim_key.getChildCount()); // 2 column references
        AstNode columnRef = prim_key.getChildren().get(0);
        assertEquals("columnA", columnRef.getName().getString());
    }

    @FixFor( "MODE-817" )
    @Test
    public void shouldParseAlterTables() {
        printTest("shouldParseAlterTables()");

        String content = getFileContent(DDL_FILE_PATH + "alterTables.ddl");
        assertScoreAndParse(content, "alterTables.ddl", 31);

        List<AstNode> theNodes = parser.nodeFactory().getChildrenForType(rootNode, TYPE_CREATE_TABLE_STATEMENT);
        assertThat(theNodes.size(), is(0));
        theNodes = parser.nodeFactory().getChildrenForType(rootNode, TYPE_ALTER_TABLE_STATEMENT);
        assertThat(theNodes.size(), is(13));
    }

    @Test
    public void shouldParseCreateTables() {
        printTest("shouldParseCreateTables()");

        String content = getFileContent(DDL_FILE_PATH + "createTables.ddl");
        assertScoreAndParse(content, "createTables.ddl", 20);

        List<AstNode> theNodes = parser.nodeFactory().getChildrenForType(rootNode, TYPE_CREATE_TABLE_STATEMENT);
        assertThat(theNodes.size(), is(16));
        theNodes = parser.nodeFactory().getChildrenForType(rootNode, TYPE_ALTER_TABLE_STATEMENT);
        assertThat(theNodes.size(), is(2));
    }

    @Test
    public void shouldParseDropStatements() {
        printTest("shouldParseDropStatements()");

        String content = getFileContent(DDL_FILE_PATH + "drop_statements.ddl");
        assertScoreAndParse(content, "drop_statements.ddl", 12);

        List<AstNode> theNodes = parser.nodeFactory().getChildrenForType(rootNode, TYPE_DROP_TABLE_STATEMENT);
        assertThat(theNodes.size(), is(2));
        assertEquals("list_customers", theNodes.get(0).getName().getString());
        String prop = (String)theNodes.get(1).getProperty(DROP_BEHAVIOR).getFirstValue();
        assertEquals("RESTRICT", prop);

        theNodes = parser.nodeFactory().getChildrenForType(rootNode, TYPE_DROP_VIEW_STATEMENT);
        assertThat(theNodes.size(), is(2));
        assertEquals("list_customers", theNodes.get(0).getName().getString());
        prop = (String)theNodes.get(1).getProperty(DROP_BEHAVIOR).getFirstValue();
        assertEquals("RESTRICT", prop);

        theNodes = parser.nodeFactory().getChildrenForType(rootNode, TYPE_DROP_SCHEMA_STATEMENT);
        assertThat(theNodes.size(), is(2));
        assertEquals("list_customers", theNodes.get(0).getName().getString());
        prop = (String)theNodes.get(0).getProperty(DROP_BEHAVIOR).getFirstValue();
        assertEquals("CASCADE", prop);

        theNodes = parser.nodeFactory().getChildrenForType(rootNode, TYPE_DROP_DOMAIN_STATEMENT);
        assertThat(theNodes.size(), is(2));
        assertEquals("list_customers", theNodes.get(0).getName().getString());
        prop = (String)theNodes.get(0).getProperty(DROP_BEHAVIOR).getFirstValue();
        assertEquals("CASCADE", prop);

        theNodes = parser.nodeFactory().getChildrenForType(rootNode, TYPE_DROP_TRANSLATION_STATEMENT);
        assertThat(theNodes.size(), is(1));
        assertEquals("translation_name", theNodes.get(0).getName().getString());

        theNodes = parser.nodeFactory().getChildrenForType(rootNode, TYPE_DROP_COLLATION_STATEMENT);
        assertThat(theNodes.size(), is(1));
        assertEquals("collation_name", theNodes.get(0).getName().getString());

        theNodes = parser.nodeFactory().getChildrenForType(rootNode, TYPE_DROP_CHARACTER_SET_STATEMENT);
        assertThat(theNodes.size(), is(1));
        assertEquals("character_set_name", theNodes.get(0).getName().getString());

        theNodes = parser.nodeFactory().getChildrenForType(rootNode, TYPE_DROP_ASSERTION_STATEMENT);
        assertThat(theNodes.size(), is(1));
        assertEquals("assertion_name", theNodes.get(0).getName().getString());
    }

    @Test
    public void shouldParseCreateViewNoViewColumnsNoOptions() {
        printTest("shouldParseCreateViewNoViewColumnsNoOptions()");

        // NOTE this test really contains 2 statements.

        // The basic parser will result in 3 nodes, "CREATE VIEW", "GRANT" and "TERMINATOR"

        String content = "CREATE VIEW new_product_view" + " AS SELECT color, quantity FROM new_product WHERE color = 'RED'"
                         + " GRANT select ON new_product_view TO hr;";

        assertScoreAndParse(content, null, 2);

        AstNode viewNode = rootNode.getChildren().get(0);
        assertEquals("new_product_view", viewNode.getName().getString());
        assertEquals(0, viewNode.getChildCount());
    }

    @Test
    public void shouldParseCreateViewWithViewColumnsNoOptions() {
        printTest("shouldParseCreateViewWithViewColumnsNoOptions()");
        String content = "CREATE VIEW CORPDATA.EMP_YEARSOFSERVICE (LASTNAME, YEARSOFSERVICE)"
                         + " AS SELECT LASTNAME, YEAR (CURRENT DATE - HIREDATE) FROM CORPDATA.EMPLOYEE;";

        assertScoreAndParse(content, null, 1);

        AstNode viewNode = rootNode.getChildren().get(0);
        assertEquals("CORPDATA.EMP_YEARSOFSERVICE", viewNode.getName().getString());
        assertEquals(2, viewNode.getChildCount()); // TWO COLUMN REFERENCES
        AstNode columnRef = viewNode.getChildren().get(0);
        assertEquals("LASTNAME", columnRef.getName().getString());
    }

    @Test
    public void shouldParseMixedTerminatedStatements() {
        printTest("");

        String content = getFileContent(DDL_FILE_PATH + "standardDdlTest.ddl");
        assertScoreAndParse(content, "standardDdlTest.ddl", 11);

        // List<AstNode> theNodes = parser.nodeFactory().getChildrenForType(rootNode, TYPE_MISSING_TERMINATOR);
        // assertThat(theNodes.size(), is(3));
        // assertEquals(DdlConstants.MISSING_TERMINATOR_NODE_LITERAL, theNodes.get(0).getName().getString());
    }

    @Test
    public void shouldParseUnterminatedStatementsFile() {
        printTest("shouldParseUnterminatedOracleFile()");
        String content = getFileContent(DDL_FILE_PATH + "GFM_Physical_Partial.ddl");

        // parser.setTestMode(true);

        assertScoreAndParse(content, "GFM_Physical_Partial.ddl", 5);

        List<AstNode> schemaNodes = parser.nodeFactory().getChildrenForType(rootNode, TYPE_CREATE_SCHEMA_STATEMENT);
        assertThat(schemaNodes.size(), is(1));
        assertThat(schemaNodes.get(0).getChildCount(), is(2));
        assertThat(schemaNodes.get(0).getName().getString(), is("GLOBALFORCEMGMT"));

    }

    @Test
    public void shouldParseStatementsWithDoubleQuotes() {
        printTest("shouldParseUnterminatedOracleFile()");
        String content = "ALTER JAVA CLASS \"Agent\"" + "RESOLVER ((\"/home/java.101/bin/*\" pm)(* public)) RESOLVE;"
                         + "CREATE SERVER foo FOREIGN DATA WRAPPER \"default\";"
                         + "CREATE RULE \"_RETURN\" AS ON SELECT TO t1 DO INSTEAD SELECT * FROM t2;";

        // parser.setTestMode(true);
        assertScoreAndParse(content, null, 3);
    }

    @Test
    public void shouldParseGrantStatements() {
        printTest("shouldParseGrantStatements()");
        String content = "GRANT SELECT ON TABLE purchaseOrders TO maria,harry;" + NEWLINE
                         + "GRANT UPDATE, USAGE ON TABLE purchaseOrders TO anita,zhi;" + NEWLINE
                         + "GRANT SELECT ON TABLE orders.bills to PUBLIC;" + NEWLINE
                         + "GRANT INSERT(a, b, c) ON TABLE purchaseOrders TO purchases_reader_role;";
        assertScoreAndParse(content, null, 4);
    }

    @Test
    public void shouldParseRevokeStatements() {
        printTest("shouldParseRevokeStatements()");
        String content = "REVOKE SELECT ON TABLE purchaseOrders FROM maria,harry;" + NEWLINE
                         + "REVOKE UPDATE, USAGE ON TABLE purchaseOrders FROM anita,zhi CASCADE;" + NEWLINE
                         + "REVOKE SELECT ON TABLE orders.bills FROM PUBLIC RESTRICT;" + NEWLINE
                         + "REVOKE INSERT(a, b, c) ON TABLE purchaseOrders FROM purchases_reader_role;";
        assertScoreAndParse(content, null, 4);
    }

    @Test
    public void shouldParseSchemaWithTableAndView() {
        printTest("shouldParseSchemaWithTableAndView()");
        String content = "create schema schema_1 authorization ADM default character set UNICODE" + NEWLINE
                         + "     create table table_1 (col1 varchar(20) not null, col2 nchar default current_user)" + NEWLINE
                         + "     create view view_1 (col1, col2) as select*from a with check option" + NEWLINE + ";";
        assertScoreAndParse(content, null, 1);
    }
}
