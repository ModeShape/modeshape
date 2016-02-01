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
package org.modeshape.sequencer.ddl;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_ALTER_TABLE_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_CREATE_SCHEMA_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_SCHEMA_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_PROBLEM;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_UNKNOWN_STATEMENT;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.text.ParsingException;
import org.modeshape.sequencer.ddl.DdlParsers.ParsingResult;
import org.modeshape.sequencer.ddl.dialect.derby.DerbyDdlParser;
import org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlParser;
import org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlParser;
import org.modeshape.sequencer.ddl.node.AstNode;
import org.modeshape.sequencer.ddl.node.AstNodeFactory;

/**
 *
 */
public class DdlParsersTest extends DdlParserTestHelper {
    private DdlParsers parsers;
    public static final String DDL_TEST_FILE_PATH = "ddl/";

    @Before
    public void beforeEach() {
        parsers = new DdlParsers();
    }

    @Test
    public void shouldParseUntypedDdlFileWithTypeInFilename() {
        printTest("shouldParseUntypedDdlFileWithTypeInFilename()");
        String content = "-- SAMPLE DDL FILE\n"
                         + "CREATE TABLE myTableName (PART_COLOR VARCHAR(255) NOT NULL, PART_ID INTEGER DEFAULT (100))\n"
                         + "DROP TABLE list_customers CASCADE";

        rootNode = parsers.parse(content, "someSQL92.ddl");

        assertEquals("SQL92", rootNode.getProperty(StandardDdlLexicon.PARSER_ID));
    }

    @Test
    public void shouldParseTypedDdlFileWith() {
        printTest("shouldParseTypedDdlFileWith()");
        String content = "-- SAMPLE SQL92 DDL FILE\n"
                         + "CREATE TABLE myTableName (PART_COLOR VARCHAR(255) NOT NULL, PART_ID INTEGER DEFAULT (100))\n"
                         + "DROP TABLE list_customers CASCADE";

        rootNode = parsers.parse(content, null);

        assertEquals("SQL92", rootNode.getProperty(StandardDdlLexicon.PARSER_ID));
    }

    @Test
    public void shouldParseUntypedDdlFile() {
        printTest("shouldParseUntypedDdlFile()");
        String content = "\n" + "-- SAMPLE DDL FILE\n"
                         + "CREATE TABLE myTableName (PART_COLOR VARCHAR(255) NOT NULL, PART_ID INTEGER DEFAULT (100))\n"
                         + "DROP TABLE list_customers CASCADE";

        rootNode = parsers.parse(content, null);

        assertEquals("SQL92", rootNode.getProperty(StandardDdlLexicon.PARSER_ID));
    }

    @Test
    public void shouldParseUntypedDerbyFile() {
        printTest("shouldParseUntypedDerbyFile()");
        String content = getFileContent(DDL_TEST_FILE_PATH + "dialect/derby/derby_test_statements.ddl");

        rootNode = parsers.parse(content, null);

        assertEquals("DERBY", rootNode.getProperty(StandardDdlLexicon.PARSER_ID));
    }

    @Test
    public void shouldParseTypedDerbyFile() {
        printTest("shouldParseTypedDerbyFile()");
        String content = getFileContent(DDL_TEST_FILE_PATH + "dialect/derby/derby_test_statements_typed.ddl");

        rootNode = parsers.parse(content, null);

        assertEquals("DERBY", rootNode.getProperty(StandardDdlLexicon.PARSER_ID));
    }

    @Test
    public void shouldParseUntypedOracleFile() {
        printTest("shouldParseUntypedOracleFile()");
        String content = getFileContent(DDL_TEST_FILE_PATH + "dialect/oracle/oracle_test_statements_3.ddl");

        rootNode = parsers.parse(content, null);

        assertEquals("ORACLE", rootNode.getProperty(StandardDdlLexicon.PARSER_ID));
    }

    @Test
    public void shouldParseUntypedPostgresFile() {
        printTest("shouldParseUntypedPostgresFile()");
        String content = getFileContent(DDL_TEST_FILE_PATH + "dialect/postgres/postgres_test_statements_1.ddl");

        rootNode = parsers.parse(content, null);

        assertThat("POSTGRES", is((String)rootNode.getProperty(StandardDdlLexicon.PARSER_ID)));
    }

    @Test
    public void shouldParseUnterminatedOracleFile() {
        printTest("shouldParseUnterminatedOracleFile()");
        String content = getFileContent(DDL_TEST_FILE_PATH + "GFM_Physical.ddl");

        rootNode = parsers.parse(content, null);
        assertEquals("ORACLE", rootNode.getProperty(StandardDdlLexicon.PARSER_ID));

        // printNodeChildren(rootNode);
        setPrintToConsole(true);

        final AstNodeFactory nodeFactory = new AstNodeFactory();
        List<AstNode> problems = nodeFactory.getChildrenForType(rootNode, TYPE_PROBLEM);
        for (AstNode problem : problems) {
            printTest(problem.toString());
        }

        assertThat(rootNode.getChildCount(), is(123));

        List<AstNode> schemaNodes = nodeFactory.getChildrenForType(rootNode, TYPE_CREATE_SCHEMA_STATEMENT);
        assertThat(schemaNodes.size(), is(1));
        assertThat(schemaNodes.get(0).getChildCount(), is(53));
        assertThat(schemaNodes.get(0).getName(), is("GLOBALFORCEMGMT"));
        List<AstNode> alterNodes = nodeFactory.getChildrenForType(rootNode, TYPE_ALTER_TABLE_STATEMENT);
        assertThat(alterNodes.size(), is(120));
        List<AstNode> dropSchemaNodes = nodeFactory.getChildrenForType(rootNode, TYPE_DROP_SCHEMA_STATEMENT);
        assertThat(dropSchemaNodes.size(), is(1));
        List<AstNode> unknownNodes = nodeFactory.getChildrenForType(rootNode, TYPE_UNKNOWN_STATEMENT);
        assertThat(unknownNodes.size(), is(1));
    }

    @Test( expected = ParsingException.class )
    public void shouldErrorWhenInvalidParserId() {
        final String content = getFileContent(DDL_TEST_FILE_PATH + "dialect/derby/derby_test_statements.ddl");
        this.parsers.parseUsing(content, "BOGUS");
    }

    @Test
    public void shouldParseHugeOracleFile() {
        printTest("shouldParseHugeOracleFile()");

        final String content = getFileContent(DDL_TEST_FILE_PATH + "dialect/oracle/huge.ddl");
        this.rootNode = this.parsers.parse(content, null);

        assertThat("ORACLE", is((String)this.rootNode.getProperty(StandardDdlLexicon.PARSER_ID)));
    }

    @Test
    public void shouldReturnBuiltInParsers() {
        printTest("shouldReturnBuiltInParsers()");

        assertThat(this.parsers.getParsers(),
                   hasItems(DdlParsers.BUILTIN_PARSERS.toArray(new DdlParser[DdlParsers.BUILTIN_PARSERS.size()])));
    }

    @Test
    public void shouldReturnParsersConstructedWith() {
        printTest("shouldReturnParsersConstructedWith()");

        final List<DdlParser> myParsers = new ArrayList<DdlParser>();
        myParsers.add(new DerbyDdlParser());

        final DdlParsers ddlParsers = new DdlParsers(myParsers);
        assertThat(ddlParsers.getParsers(), hasItems(myParsers.toArray(new DdlParser[myParsers.size()])));
    }
    
    @Test
    public void shouldReturnResultsInOrder() {
        printTest("shouldReturnResultsInOrder()");

        { // oracle
            final String ddl = getFileContent(DDL_TEST_FILE_PATH + "dialect/oracle/oracle_test_statements_3.ddl");
            final List<ParsingResult> results = this.parsers.parseUsingAll(ddl);

            assertThat(results.size(), is(DdlParsers.BUILTIN_PARSERS.size()));
            assertThat(results.get(0).getParserId(), is(OracleDdlParser.ID)); // first element should be Oracle result
        }

        { // derby
            final String ddl = getFileContent(DDL_TEST_FILE_PATH + "dialect/derby/derby_test_statements.ddl");
            final List<ParsingResult> results = this.parsers.parseUsingAll(ddl);

            assertThat(results.size(), is(DdlParsers.BUILTIN_PARSERS.size()));
            assertThat(results.get(0).getParserId(), is(DerbyDdlParser.ID)); // first element should be Teiid result
        }
    }

    @Test
    public void shouldReturnCorrectNumberOfResults() {
        printTest("shouldReturnCorrectNumberOfResults()");

        final List<DdlParser> myParsers = new ArrayList<DdlParser>(2);
        myParsers.add(new PostgresDdlParser());
        myParsers.add(new OracleDdlParser());
        final DdlParsers ddlParsers = new DdlParsers(myParsers);

        final String ddl = getFileContent(DDL_TEST_FILE_PATH + "dialect/oracle/oracle_test_statements_3.ddl");
        final List<ParsingResult> results = ddlParsers.parseUsing(ddl,
                                                                  myParsers.get(0).getId(),
                                                                  myParsers.get(1).getId(),
                                                                  (String[])null);
        assertThat(results.size(), is(myParsers.size()));
    }

    @Test( expected = ParsingException.class )
    public void shouldNotAllowInvalidParserId() {
        printTest("shouldNotAllowInvalidParserId()");

        final String ddl = getFileContent(DDL_TEST_FILE_PATH + "dialect/oracle/oracle_test_statements_3.ddl");
        this.parsers.parseUsing(ddl, this.parsers.getParsers().iterator().next().getId(), "bogusId", (String[])null);
    }

}
