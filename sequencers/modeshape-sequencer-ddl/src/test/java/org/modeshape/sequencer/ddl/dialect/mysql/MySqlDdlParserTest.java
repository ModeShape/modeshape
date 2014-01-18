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
package org.modeshape.sequencer.ddl.dialect.mysql;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_ALTER_TABLE_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_CREATE_VIEW_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_INSERT_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_PROBLEM;
import static org.modeshape.sequencer.ddl.dialect.mysql.MySqlDdlLexicon.TYPE_CREATE_INDEX_STATEMENT;
import java.util.List;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.sequencer.ddl.DdlParserScorer;
import org.modeshape.sequencer.ddl.DdlParserTestHelper;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 *
 */
public class MySqlDdlParserTest extends DdlParserTestHelper {

    public static final String DDL_FILE_PATH = "ddl/dialect/mysql/";

    @Before
    public void beforeEach() {
        parser = new MySqlDdlParser();
        setPrintToConsole(false);
        parser.setTestMode(isPrintToConsole());
        parser.setDoUseTerminator(true);
        rootNode = parser.nodeFactory().node("ddlRootNode");
        scorer = new DdlParserScorer();
    }

    @Test
    public void shouldParseCreateTable() {
        printTest("shouldParseCreateTable()");

        String content = "CREATE TABLE MY_TABLE_A (PARTID BLOB (255) NOT NULL DEFAULT (100), "
                         + " -- COLUMN 1 COMMENT with comma \nPARTCOLOR INTEGER NOT NULL) ON COMMIT DELETE ROWS;";
        assertScoreAndParse(content, null, 2);
    }

    @FixFor( "MODE-820" )
    @Test
    public void shouldParseCreateTableWithKilobyteInSize() {
        printTest("shouldParseCreateTableWithKilobyteInSize()");

        String content = "CREATE TABLE MY_TABLE_A (PARTID BLOB (2K) NOT NULL, "
                         + " -- COLUMN 1 COMMENT with comma \nPARTCOLOR CHAR(4M) NOT NULL) ON COMMIT DELETE ROWS;";

        assertScoreAndParse(content, null, 2);
    }

    @Test
    public void shouldParseCreateTableWithMySqlDataTypes() {
        printTest("shouldParseAlterTableAlterColumnDefaultRealNumber()");
        String content = "CREATE TABLE CS_EXT_FILES  (\n" + "     FILE_NAME        VARCHAR(255),\n"
                         + "     FILE_CONTENTS    LONGBLOB,\n" + "     CONFIG_CONTENTS    LONGTEXT);";

        assertScoreAndParse(content, null, 1);
        assertThat(rootNode.getChild(0).getChildCount(), is(3));
        assertThat(rootNode.getChild(0).getName(), is("CS_EXT_FILES"));
    }

    @Test
    public void shouldParseTestCreate() {
        printTest("shouldParseTestCreate()");
        String content = getFileContent(DDL_FILE_PATH + "mysql_test_create.ddl");

        assertScoreAndParse(content, "mysql_test_create.ddl", 145);

        List<AstNode> problems = parser.nodeFactory().getChildrenForType(rootNode, TYPE_PROBLEM);
        assertThat(problems.size(), is(0));
        List<AstNode> createTables = parser.nodeFactory().getChildrenForType(rootNode, TYPE_CREATE_TABLE_STATEMENT);
        assertThat(createTables.size(), is(57));
        List<AstNode> alterTables = parser.nodeFactory().getChildrenForType(rootNode, TYPE_ALTER_TABLE_STATEMENT);
        assertThat(alterTables.size(), is(31));
        List<AstNode> createViews = parser.nodeFactory().getChildrenForType(rootNode, TYPE_CREATE_VIEW_STATEMENT);
        assertThat(createViews.size(), is(3));
        List<AstNode> createIndexes = parser.nodeFactory().getChildrenForType(rootNode, TYPE_CREATE_INDEX_STATEMENT);
        assertThat(createIndexes.size(), is(53));
        List<AstNode> insertIntos = parser.nodeFactory().getChildrenForType(rootNode, TYPE_INSERT_STATEMENT);
        assertThat(insertIntos.size(), is(1));
    }

    @Ignore( "MySql support not fully implemented yet" )
    @Test
    public void shouldParseMySqlTestStatements() {
        printTest("shouldParseMySqlTestStatements()");
        String content = getFileContent(DDL_FILE_PATH + "mysql_test_statements.ddl");

        assertScoreAndParse(content, "mysql_test_statements.ddl", 106);

        printUnknownStatements(parser, rootNode);
        printProblems(parser, rootNode);

        List<AstNode> problems = parser.nodeFactory().getChildrenForType(rootNode, TYPE_PROBLEM);
        assertThat(problems.size(), is(0));
    }
}
