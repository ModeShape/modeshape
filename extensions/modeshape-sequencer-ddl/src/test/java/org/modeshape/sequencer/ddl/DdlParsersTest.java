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
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.graph.ExecutionContext;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.*;
import org.modeshape.sequencer.ddl.node.AstNode;
import org.modeshape.sequencer.ddl.node.AstNodeFactory;
import java.util.List;

/**
 *
 */
public class DdlParsersTest extends DdlParserTestHelper {
    private DdlParsers parsers;
    public static final String DDL_TEST_FILE_PATH = "ddl/";

    private ExecutionContext context;

    private AstNodeFactory nodeFactory;

    private AstNode rootNode;

    @Before
    public void beforeEach() {
        parsers = new DdlParsers();
        // ddlTest = this.getClass().getClassLoader().getResource("createTablesTest.ddl");
        context = new ExecutionContext();
        rootNode = new AstNode(context.getValueFactories().getNameFactory().create("root_node"));
        nodeFactory = new AstNodeFactory(context);
    }

    @Test
    public void shouldParseUntypedDdlFileWithTypeInFilename() {
        printTest("shouldParseTypedDdlFile()");
        String content = "-- SAMPLE DDL FILE\n"
                         + "CREATE TABLE myTableName (PART_COLOR VARCHAR(255) NOT NULL, PART_ID INTEGER DEFAULT (100))\n"
                         + "DROP TABLE list_customers CASCADE";

        rootNode = parsers.parse(content, "someSQL92.ddl");

        assertEquals("SQL92", rootNode.getProperty(StandardDdlLexicon.PARSER_ID).getFirstValue());
    }

    @Test
    public void shouldParseTypedDdlFileWith() {
        printTest("shouldParseTypedDdlFile()");
        String content = "-- SAMPLE SQL92 DDL FILE\n"
                         + "CREATE TABLE myTableName (PART_COLOR VARCHAR(255) NOT NULL, PART_ID INTEGER DEFAULT (100))\n"
                         + "DROP TABLE list_customers CASCADE";

        rootNode = parsers.parse(content, null);

        assertEquals("SQL92", rootNode.getProperty(StandardDdlLexicon.PARSER_ID).getFirstValue());
    }

    @Test
    public void shouldParseUntypedDdlFile() {
        printTest("shouldParseUntypedDdlFile()");
        String content = "\n" + "-- SAMPLE DDL FILE\n"
                         + "CREATE TABLE myTableName (PART_COLOR VARCHAR(255) NOT NULL, PART_ID INTEGER DEFAULT (100))\n"
                         + "DROP TABLE list_customers CASCADE";

        rootNode = parsers.parse(content, null);

        assertEquals("SQL92", rootNode.getProperty(StandardDdlLexicon.PARSER_ID).getFirstValue());
    }

    @Test
    public void shouldParseUntypedDerbyFile() {
        printTest("shouldParseUntypedDerbyFile()");
        String content = getFileContent(DDL_TEST_FILE_PATH + "dialect/derby/derby_test_statements.ddl");

        rootNode = parsers.parse(content, null);

        assertEquals("DERBY", rootNode.getProperty(StandardDdlLexicon.PARSER_ID).getFirstValue());
    }

    @Test
    public void shouldParseTypedDerbyFile() {
        printTest("shouldParseTypedDerbyFile()");
        String content = getFileContent(DDL_TEST_FILE_PATH + "dialect/derby/derby_test_statements_typed.ddl");

        rootNode = parsers.parse(content, null);

        assertEquals("DERBY", rootNode.getProperty(StandardDdlLexicon.PARSER_ID).getFirstValue());
    }

    @Test
    public void shouldParseUntypedOracleFile() {
        printTest("shouldParseUntypedOracleFile()");
        String content = getFileContent(DDL_TEST_FILE_PATH + "dialect/oracle/oracle_test_statements_3.ddl");

        rootNode = parsers.parse(content, null);

        assertEquals("ORACLE", rootNode.getProperty(StandardDdlLexicon.PARSER_ID).getFirstValue());
    }

    @Test
    public void shouldParseUntypedPostgresFile() {
        printTest("shouldParseUntypedPostgresFile()");
        String content = getFileContent(DDL_TEST_FILE_PATH + "dialect/postgres/postgres_test_statements_1.ddl");

        rootNode = parsers.parse(content, null);

        assertThat("POSTGRES", is((String)rootNode.getProperty(StandardDdlLexicon.PARSER_ID).getFirstValue()));
    }

    @Test
    public void shouldParseUnterminatedOracleFile() {
        printTest("shouldParseUnterminatedOracleFile()");
        String content = getFileContent(DDL_TEST_FILE_PATH + "GFM_Physical.ddl");

        rootNode = parsers.parse(content, null);
        assertEquals("ORACLE", rootNode.getProperty(StandardDdlLexicon.PARSER_ID).getFirstValue());

        // printNodeChildren(rootNode);
        setPrintToConsole(true);
        List<AstNode> problems = nodeFactory.getChildrenForType(rootNode, TYPE_PROBLEM);
        for (AstNode problem : problems) {
            printTest(problem.toString());
        }

        assertThat(rootNode.getChildCount(), is(123));

        List<AstNode> schemaNodes = nodeFactory.getChildrenForType(rootNode, TYPE_CREATE_SCHEMA_STATEMENT);
        assertThat(schemaNodes.size(), is(1));
        assertThat(schemaNodes.get(0).getChildCount(), is(53));
        assertThat(schemaNodes.get(0).getName().getString(), is("GLOBALFORCEMGMT"));
        List<AstNode> alterNodes = nodeFactory.getChildrenForType(rootNode, TYPE_ALTER_TABLE_STATEMENT);
        assertThat(alterNodes.size(), is(120));
        List<AstNode> dropSchemaNodes = nodeFactory.getChildrenForType(rootNode, TYPE_DROP_SCHEMA_STATEMENT);
        assertThat(dropSchemaNodes.size(), is(1));
        List<AstNode> unknownNodes = nodeFactory.getChildrenForType(rootNode, TYPE_UNKNOWN_STATEMENT);
        assertThat(unknownNodes.size(), is(1));

    }
}
