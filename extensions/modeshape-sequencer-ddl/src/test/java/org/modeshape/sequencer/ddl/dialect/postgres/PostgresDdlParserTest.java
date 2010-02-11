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
package org.modeshape.sequencer.ddl.dialect.postgres;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_CREATE_SCHEMA_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_CREATE_TABLE_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_DROP_COLUMN_DEFINITION;
import static org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_FOREIGN_DATA_WRAPPER_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_ALTER_TABLE_STATEMENT_POSTGRES;
import static org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_COMMENT_ON_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_RULE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_CREATE_SEQUENCE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_GRANT_ON_FUNCTION_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_LISTEN_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.postgres.PostgresDdlLexicon.TYPE_RENAME_COLUMN;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.sequencer.ddl.DdlConstants;
import org.modeshape.sequencer.ddl.DdlParserScorer;
import org.modeshape.sequencer.ddl.DdlParserTestHelper;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 *
 */
public class PostgresDdlParserTest extends DdlParserTestHelper {
    private static final String SPACE = DdlConstants.SPACE;

    public static final String DDL_FILE_PATH = "src/test/resources/ddl/dialect/postgres/";

    @Before
    public void beforeEach() {
        parser = new PostgresDdlParser();
        setPrintToConsole(false);
        parser.setTestMode(isPrintToConsole());
        parser.setDoUseTerminator(true);
        rootNode = parser.nodeFactory().node("ddlRootNode");
        scorer = new DdlParserScorer();
    }

    @Test
    public void shouldParseAlterTableMultipleAddColumns() {
        printTest("shouldParseAlterTableMultipleAddColumns()");
        String content = "ALTER TABLE distributors \n" + "        ADD COLUMN nick_name varchar(30), \n"
                         + "        ADD COLUMN address varchar(30);";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_TABLE_STATEMENT_POSTGRES));

        assertEquals(2, childNode.getChildCount());

    }

    @Test
    public void shouldParseAlterTableMultipleMixedActions() {
        printTest("shouldParseAlterTableMultipleAddColumns()");
        String content = "ALTER TABLE distributors \n" + "        ADD COLUMN nick_name varchar(30), \n"
                         + "        ALTER COLUMN address TYPE varchar(255), \n" + "        RENAME COLUMN address TO city, \n"
                         + "        DROP COLUMN address;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_TABLE_STATEMENT_POSTGRES));

        assertEquals(4, childNode.getChildCount());
        assertTrue(hasMixinType(childNode.getChild(2).getProperty(JcrLexicon.MIXIN_TYPES), TYPE_RENAME_COLUMN));
        assertTrue(hasMixinType(childNode.getChild(3).getProperty(JcrLexicon.MIXIN_TYPES), TYPE_DROP_COLUMN_DEFINITION));

    }

    @Test
    public void shouldParseAlterTableMultipleAlterColumns() {
        printTest("shouldParseAlterTableMultipleAlterColumns()");
        String content = "ALTER TABLE distributors ALTER COLUMN address TYPE varchar(80), ALTER COLUMN name TYPE varchar(100);";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_TABLE_STATEMENT_POSTGRES));

    }

    @Test
    public void shouldParseAlterTableMultipeAlterColumns_2() {
        printTest("shouldParseAlterTableMultipeAlterColumns_2()");
        String content = "ALTER TABLE foo" + SPACE + "ALTER COLUMN foo_timestamp DROP DEFAULT," + SPACE
                         + "ALTER COLUMN foo_timestamp TYPE timestamp with time zone" + SPACE
                         + "USING timestamp with time zone ’epoch’ + foo_timestamp *GO interval ’1 second’," + SPACE
                         + "ALTER COLUMN foo_timestamp SET DEFAULT now();";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_TABLE_STATEMENT_POSTGRES));
    }

    @Test
    public void shouldParseAlterTableMultipeColumns_3() {
        printTest("shouldParseAlterTableMultipeColumns_3()");
        String content = "ALTER TABLE foo" + SPACE + "ALTER COLUMN foo_timestamp SET DATA TYPE timestamp with time zone" + SPACE
                         + "USING timestamp with time zone ’epoch’ + foo_timestamp * interval ’1 second’;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_TABLE_STATEMENT_POSTGRES));
    }

    @Test
    public void shouldParseAlterTableMultipeColumns_4() {
        printTest("shouldParseAlterTableMultipeColumns_4()");
        String content = "ALTER TABLE distributors ALTER COLUMN street DROP NOT NULL;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_TABLE_STATEMENT_POSTGRES));
    }

    @Test
    public void shouldParseAlterTableMultipeColumns_5() {
        printTest("shouldParseAlterTableMultipeColumns_5()");
        String content = "ALTER TABLE distributors ADD CONSTRAINT zipchk CHECK (char_length(zipcode) = 5);";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_TABLE_STATEMENT_POSTGRES));
    }

    @Test
    public void shouldParseAlterTableMultipeColumns_6() {
        printTest("shouldParseAlterTableMultipeColumns_6()");
        String content = "ALTER TABLE distributors SET TABLESPACE fasttablespace;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_TABLE_STATEMENT_POSTGRES));
    }

    @Test
    public void shouldParseCreateSchema() {
        printTest("shouldParseCreateSchema()");
        String content = "CREATE SCHEMA hollywood" + SPACE + "CREATE TABLE films (title text, release date, awards text[])"
                         + SPACE + "CREATE VIEW winners AS SELECT title, release FROM films WHERE awards IS NOT NULL;";
        assertScoreAndParse(content, null, 1); // schema
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_SCHEMA_STATEMENT));
    }

    @Test
    public void shouldParseCreateSequence() {
        printTest("shouldParseCreateSequence()");
        String content = "CREATE SEQUENCE serial START 101;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_SEQUENCE_STATEMENT));
    }

    // CREATE TABLE films (
    // code char(5) CONSTRAINT firstkey PRIMARY KEY,
    // title varchar(40) NOT NULL,
    // did integer NOT NULL,
    // date_prod date,
    // kind varchar(10),
    // len interval hour to minute
    // );

    @Test
    public void shouldParseCreateTable_1() {
        printTest("shouldParseCreateTable_1()");
        String content = "CREATE TABLE films (" + SPACE + "code        char(5) CONSTRAINT firstkey PRIMARY KEY," + SPACE
                         + "title       varchar(40) NOT NULL," + SPACE + "did         integer NOT NULL," + SPACE
                         + "date_prod   date," + SPACE + "kind        varchar(10)," + SPACE
                         + "len         interval hour to minute" + SPACE + ");";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_TABLE_STATEMENT));
    }

    // CREATE TABLE distributors (
    // did integer PRIMARY KEY DEFAULT nextval(’serial’),
    // name varchar(40) NOT NULL CHECK (name <> ”)
    //		     
    // );

    @Test
    public void shouldParseCreateTable_2() {
        printTest("shouldParseCreateTable_2()");
        String content = "CREATE TABLE distributors (" + SPACE + "did    integer PRIMARY KEY DEFAULT nextval(’serial’)," + SPACE
                         + "name      varchar(40) NOT NULL CHECK (name <> ”)" + SPACE + ");";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_TABLE_STATEMENT));
    }

    // CREATE TABLE distributors (
    // name varchar(40) DEFAULT ’Luso Films’,
    // did integer DEFAULT nextval(’distributors_serial’),
    // modtime timestamp DEFAULT current_timestamp
    // );

    @Ignore
    @Test
    public void shouldParseCreateTable_3() {
        printTest("shouldParseCreateTable_3()");
        String content = "CREATE TABLE distributors (" + SPACE + "name      varchar(40) DEFAULT 'xxxx yyyy'," + SPACE
                         + "name      varchar(40) DEFAULT ’Luso Films’," + SPACE
                         + "did       integer DEFAULT nextval(’distributors_serial’)," + SPACE
                         + "modtime   timestamp DEFAULT current_timestamp" + SPACE + ");";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_TABLE_STATEMENT));
    }

    // CREATE TABLE films_recent AS
    // SELECT * FROM films WHERE date_prod >= ’2002-01-01’;

    @Test
    public void shouldParseCreateTable_4() {
        printTest("shouldParseCreateTable_4()");
        String content = "CREATE TABLE films_recent AS" + SPACE + "SELECT * FROM films WHERE date_prod >= ’2002-01-01’;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_TABLE_STATEMENT));
    }

    // LISTEN virtual;

    @Test
    public void shouldParseListen() {
        printTest("shouldParseListen()");
        String content = "LISTEN virtual;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_LISTEN_STATEMENT));
    }

    // CREATE TEMP TABLE films_recent WITH (OIDS) ON COMMIT DROP AS
    // EXECUTE recentfilms(’2002-01-01’);

    @Test
    public void shouldParseCreateTempTable() {
        printTest("shouldParseCreateTempTable()");
        String content = "CREATE TEMP TABLE films_recent WITH (OIDS) ON COMMIT DROP AS EXECUTE recentfilms(’2002-01-01’);";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_TABLE_STATEMENT));

    }

    // CREATE RULE notify_me AS ON UPDATE TO mytable DO ALSO NOTIFY mytable;
    @Test
    public void shouldParseCreateRule() {
        printTest("shouldParseCreateRule()");
        String content = "CREATE RULE notify_me AS ON UPDATE TO mytable DO ALSO NOTIFY mytable;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_RULE_STATEMENT));

    }

    @Test
    public void shouldParseAlterForeignDataWrapper() {
        printTest("");
        String content = "ALTER FOREIGN DATA WRAPPER dbi OPTIONS (ADD foo ’1’, DROP ’bar’);";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_FOREIGN_DATA_WRAPPER_STATEMENT));
    }

    @Test
    public void shouldParseCommentOn() {
        printTest("shouldParseCommentOn()");
        String content = "COMMENT ON TABLE mytable IS ’This is my table.’;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_COMMENT_ON_STATEMENT));
    }

    @Test
    public void shouldParseCreateFunctionWithMultipleSemicolons() {
        printTest("shouldParseCreateFunctionWithMultipleSemicolons()");
        String content = "CREATE OR REPLACE FUNCTION increment(i integer) RETURNS integer AS $$ BEGIN RETURN i + 1; END;" + SPACE
                         + "CREATE TABLE tblName_A (col_1 varchar(255));";
        assertScoreAndParse(content, null, 2);
    }

    @Test
    public void shouldParseLockTable() {
        printTest("shouldParseLockTable()");
        String content = "LOCK TABLE films IN SHARE MODE;";
        assertScoreAndParse(content, null, 1);
    }

    @Test
    public void shouldParsePrepareStatement() {
        printTest("shouldParsePrepareStatement()");
        String content = "PREPARE fooplan (int, text, bool, numeric) AS INSERT INTO foo VALUES($1, $2, $3, $4);";
        assertScoreAndParse(content, null, 1);
    }

    @Test
    public void shouldParseDropDomain() {
        printTest("shouldParseDropDomain()");
        String content = "DROP DOMAIN IF EXISTS domain_name CASCADE;";
        assertScoreAndParse(content, null, 1);
    }

    @Test
    public void shouldParseDropTableMultiple() {
        printTest("shouldParseDropDomain()");
        String content = "DROP TABLE films, distributors;";
        assertScoreAndParse(content, null, 2);
    }

    @Test
    public void shouldParseGrantOnTable() {
        printTest("shouldParseGrantOnTable()");
        String content = "GRANT UPDATE, TRIGGER ON TABLE t TO anita,zhi;";
        assertScoreAndParse(content, null, 1);
    }

    @Test
    public void shouldParseGrantOnMultipleTables() {
        printTest("shouldParseGrantOnMultipleTables()");
        String content = "GRANT UPDATE, TRIGGER ON TABLE t1, t2, t3 TO anita,zhi;";
        assertScoreAndParse(content, null, 3);
    }

    @Test
    public void shouldParseGrantExecuteOnFunction() {
        printTest("shouldParseGrantExecuteOnFunction()");
        String content = "GRANT EXECUTE ON FUNCTION divideByTwo(numerator int, IN demoninator int) TO george;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_GRANT_ON_FUNCTION_STATEMENT));
    }

    @Test
    public void shouldParseGrantExecuteAndUpdateOnMultipleFunctions() {
        printTest("shouldParseGrantExecuteOnMultipleFunctions()");
        String content = "GRANT EXECUTE, UPDATE ON FUNCTION cos(), sin(b double precision) TO peter;";
        assertScoreAndParse(content, null, 2);
        AstNode childNode = rootNode.getChild(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_GRANT_ON_FUNCTION_STATEMENT));
        assertEquals(3, childNode.getChildCount());
        childNode = rootNode.getChild(1);
        assertEquals(4, childNode.getChildCount());
    }

    @Test
    public void shouldParsePostgresStatements_1() {
        printTest("shouldParsePostgresStatements_1()");
        String content = getFileContent(DDL_FILE_PATH + "postgres_test_statements_1.ddl");
        assertScoreAndParse(content, "postgres_test_statements_1.ddl", 82);
    }

    @Ignore
    @Test
    public void shouldParsePostgresStatements_2() {
        printTest("shouldParsePostgresStatements_2()");
        String content = getFileContent(DDL_FILE_PATH + "postgres_test_statements_2.ddl");
        assertScoreAndParse(content, "postgres_test_statements_2.ddl", 101);
    }

    @Test
    public void shouldParsePostgresStatements_3() {
        printTest("shouldParsePostgresStatements_3()");
        String content = getFileContent(DDL_FILE_PATH + "postgres_test_statements_3.ddl");
        assertScoreAndParse(content, "postgres_test_statements_3.ddl", 143);
    }

    @Test
    public void shouldParsePostgresStatements_4() {
        printTest("shouldParsePostgresStatements_4()");
        String content = getFileContent(DDL_FILE_PATH + "postgres_test_statements_4.ddl");
        assertScoreAndParse(content, "postgres_test_statements_4.ddl", 34);
    }
}
