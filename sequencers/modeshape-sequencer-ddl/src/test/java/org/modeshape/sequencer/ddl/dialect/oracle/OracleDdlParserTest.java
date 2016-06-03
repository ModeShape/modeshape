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

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_ALTER_TABLE_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_GRANT_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_PROBLEM;
import static org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon.TYPE_ALTER_INDEXTYPE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon.TYPE_ALTER_INDEX_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon.TYPE_ANALYZE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon.TYPE_COMMENT_ON_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon.TYPE_CREATE_FUNCTION_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon.TYPE_CREATE_JAVA_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon.TYPE_CREATE_MATERIALIZED_VIEW_LOG_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon.TYPE_CREATE_MATERIALIZED_VIEW_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon.TYPE_CREATE_PROCEDURE_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon.TYPE_CREATE_TRIGGER_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon.TYPE_ROLLBACK_STATEMENT;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.sequencer.ddl.DdlConstants;
import org.modeshape.sequencer.ddl.DdlParserScorer;
import org.modeshape.sequencer.ddl.DdlParserTestHelper;
import org.modeshape.sequencer.ddl.StandardDdlLexicon;
import org.modeshape.sequencer.ddl.node.AstNode;

public class OracleDdlParserTest extends DdlParserTestHelper {

    public static final String DDL_FILE_PATH = "ddl/dialect/oracle/";

    @Before
    public void beforeEach() {
        parser = new OracleDdlParser();
        setPrintToConsole(false);
        parser.setTestMode(isPrintToConsole());
        parser.setDoUseTerminator(true);
        rootNode = parser.nodeFactory().node("ddlRootNode");
        scorer = new DdlParserScorer();
    }

    @Test
    public void shouldParseCreateOrReplaceTrigger() {
        printTest("shouldParseCreateOrReplaceTrigger()");
        String content = "CREATE OR REPLACE TRIGGER drop_trigger" + DdlConstants.SPACE + "BEFORE DROP ON hr.SCHEMA"
                         + DdlConstants.SPACE + "BEGIN" + DdlConstants.SPACE
                         + "RAISE_APPLICATION_ERROR ( num => -20000,msg => 'Cannot drop object');" + DdlConstants.SPACE + "END;"
                         + DdlConstants.SPACE + "/";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_CREATE_TRIGGER_STATEMENT));

    }

    @Test
    public void shouldParseAnalyze() {
        printTest("shouldParseAnalyze()");
        String content = "ANALYZE TABLE customers VALIDATE STRUCTURE ONLINE;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_ANALYZE_STATEMENT));
    }

    @Test
    public void shouldParseRollbackToSavepoint() {
        printTest("shouldParseRollbackToSavepoint()");
        String content = "ROLLBACK TO SAVEPOINT banda_sal;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_ROLLBACK_STATEMENT));
    }

    @Test
    public void shouldParseAlterTableAddREF() {
        printTest("shouldParseAlterTableAddREF()");
        String content = "ALTER TABLE staff ADD (REF(dept) WITH ROWID);";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_ALTER_TABLE_STATEMENT));
    }

    @Test
    public void shouldParseAlterTableADDWithNESTED_TABLE() {
        // This is a one-off case where there is a custom datatype (i.e. skill_table_type)
        printTest("shouldParseAlterTableADDWithNESTED_TABLE()");
        String content = "ALTER TABLE employees ADD (skills VARCHAR(10)) NESTED TABLE skills STORE AS nested_skill_table;";
        assertScoreAndParse(content, null, 1); // ALTER TABLE + NO MORE PROBLEM
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_ALTER_TABLE_STATEMENT));
    }

    @Test
    @FixFor( "MODE-2350" )
    public void shouldParseAlterTableWithAddAndModify() {
        printTest("shouldParseAlterTableMultipleOps()");
        String content = "ALTER TABLE employees ADD (add_field NUMBER) MODIFY (mod_field VARCHAR2(10));";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_ALTER_TABLE_STATEMENT));
        assertTrue(childNode.getChildCount() == 2);
    }

    @Test
    @FixFor( "MODE-2350" )
    public void shouldParseAlterTableWithAddMultipleColumns() {
        printTest("shouldParseAlterTableMultipleOps()");
        String content = "ALTER TABLE employees ADD (field1 NUMBER, field2 varchar2(75), field3 varchar2(75));";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_ALTER_TABLE_STATEMENT));
        assertTrue(childNode.getChildCount() == 3);
    }

    @Test
    @FixFor( "MODE-2350" )
    public void shouldParseAlterTableWithModifyMultipleColumns() {
        printTest("shouldParseAlterTableMultipleOps()");
        String content = "ALTER TABLE employees MODIFY (field1 NUMBER NOT NULL, field2 varchar2(75), field3 varchar2(75));";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_ALTER_TABLE_STATEMENT));
        assertTrue(childNode.getChildCount() == 3);
    } 
    
    @Test
    @FixFor( "MODE-2350" )
    public void shouldParseAlterTableWitAddAndModifyMultipleColumns() {
        printTest("shouldParseAlterTableMultipleOps()");
        String content = "ALTER TABLE employees ADD (field1 NUMBER, field2 varchar2(75), field3 varchar2(75)) " +
                         "MODIFY (field1 NUMBER NOT NULL, field2 varchar2(75), field3 varchar2(75));";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_ALTER_TABLE_STATEMENT));
        assertTrue(childNode.getChildCount() == 6);
    }
    
    @Test
    @FixFor( "MODE-2350" )
    public void shouldRejectAlterTableIncorrectAdd() {
        printTest("shouldParseAlterTableMultipleOps()");
        String content = "ALTER TABLE employees ADD (add_field NUMBER MODIFY (mod_field VARCHAR2(10));";
        assertScoreAndParse(content, null, 2);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_ALTER_TABLE_STATEMENT));
        assertTrue(childNode.getChildCount() == 2);
        assertTrue(hasMixinType(childNode.getChild(1), TYPE_PROBLEM));
    }  
    
    @Test
    @FixFor( "MODE-2350" )
    public void shouldRejectAlterTableIncorrectAddModify() {
        printTest("shouldParseAlterTableMultipleOps()");
        String content = "ALTER TABLE employees ADD MODIFY (mod_field VARCHAR2(10));";
        assertScoreAndParse(content, null, 2);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_ALTER_TABLE_STATEMENT));
        assertTrue(childNode.getChildCount() == 2);
        assertTrue(hasMixinType(childNode.getChild(1), TYPE_PROBLEM));
    }

    @Test
    @FixFor( "MODE-2439" )
    public void shouldParseAlterTableWithAddAndModifyNB() {
        printTest("shouldParseAlterTableMultipleOps()");
        String content = "ALTER TABLE employees ADD add_field NUMBER MODIFY mod_field VARCHAR2(10) ADD (add_field2 NUMBER) MODIFY (mod_field2 VARCHAR2(10));";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_ALTER_TABLE_STATEMENT));
        assertTrue(childNode.getChildCount() == 4);
    }

    @Test
    @FixFor( "MODE-2439" )
    public void shouldParseAlterTableModifyNoDatatype() {
        printTest("shouldParseAlterTableMultipleOps()");
        String content = "ALTER TABLE employees MODIFY mod_field DEFAULT NULL MODIFY (mod_field2 DEFAULT NULL);";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_ALTER_TABLE_STATEMENT));
        assertTrue(childNode.getChildCount() == 2);
    }

    @Test
    public void shouldParseAlterTableDropConstraint() {
        printTest("shouldParseAlterTableMultipleOps()");
        String content = "ALTER TABLE employees DROP CONSTRAINT fk_something";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_ALTER_TABLE_STATEMENT));
        assertTrue(childNode.getChildCount() == 1);
    }

    @Test
    public void shouldParseAlterIndexRename() {
        printTest("shouldParseAlterIndexRename()");
        String content = "ALTER INDEX upper_ix RENAME TO upper_name_ix;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_ALTER_INDEX_STATEMENT));
    }

    @Test
    public void shouldParseAlterIndexMODIFY() {
        printTest("shouldParseAlterIndexMODIFY()");
        String content = "ALTER INDEX cost_ix MODIFY PARTITION p3 STORAGE(MAXEXTENTS 30) LOGGING;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_ALTER_INDEX_STATEMENT));
    }

    @Test
    public void shouldParseAlterIndexDROP() {
        printTest("shouldParseAlterIndexDROP()");
        String content = "ALTER INDEX cost_ix DROP PARTITION p1;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_ALTER_INDEX_STATEMENT));
    }

    @Test
    public void shouldParseAlterIndexTypeADD() {
        printTest("shouldParseAlterIndexTypeADD()");
        String content = "ALTER INDEXTYPE position_indextype ADD lob_contains(CLOB, CLOB);";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_ALTER_INDEXTYPE_STATEMENT));
    }

    @Test
    public void shouldParseTEMP_TEST() {
        printTest("shouldParseTEMP_TEST()");
        String content = "COMMENT ON COLUMN employees.job_id IS 'abbreviated job title';";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_COMMENT_ON_STATEMENT));
    }

    // GRANT ALL ON bonuses TO hr WITH GRANT OPTION;
    @Test
    public void shouldParseGrantAllOn() {
        printTest("shouldParseGrant()");
        String content = "GRANT ALL ON bonuses TO hr WITH GRANT OPTION;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_GRANT_STATEMENT));
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
    public void shouldParseAlterTableWithModifyClause() {
        printTest("shouldParseAlterTableWithModifyClause()");

        String content = "ALTER TABLE employees MODIFY LOB (resume) (CACHE);";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_ALTER_TABLE_STATEMENT));
    }

    @Test
    public void shouldParseAlterTableWithAddColumns() {
        printTest("shouldParseAlterTableWithModifyClause()");

        String content = "ALTER TABLE countries \n" + "     ADD (duty_pct     NUMBER(2,2)  CHECK (duty_pct < 10.5),\n"
                         + "     visa_needed  VARCHAR2(3));";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_ALTER_TABLE_STATEMENT));
        assertEquals(3, childNode.getChildCount()); // 2 columns + CHECK constraint
    }

    @Test
    public void shouldParseJava() {
        printTest("shouldParseJava()");

        String content = "CREATE JAVA SOURCE NAMED \"Hello\" AS public class Hello { public static String hello() {return \"Hello World\";   } };";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_CREATE_JAVA_STATEMENT));
    }

    @Test
    public void shouldParseCreateOrReplaceTriggerWithEmbeddedStatements() {
        printTest("shouldParseCreateOrReplaceTriggerWithEmbeddedStatements()");

        String content = "CREATE OR REPLACE TRIGGER order_info_insert" + " INSTEAD OF INSERT ON order_info" + " DECLARE"
                         + "   duplicate_info EXCEPTION;" + "   PRAGMA EXCEPTION_INIT (duplicate_info, -00001);" + " BEGIN"
                         + "   INSERT INTO customers" + "     (customer_id, cust_last_name, cust_first_name)" + "   VALUES ("
                         + "   :new.customer_id, " + "   :new.cust_last_name," + "   :new.cust_first_name);"
                         + " INSERT INTO orders (order_id, order_date, customer_id)" + " VALUES (" + "   :new.order_id,"
                         + "   :new.order_date," + "   :new.customer_id);" + " EXCEPTION" + "   WHEN duplicate_info THEN"
                         + "    RAISE_APPLICATION_ERROR (" + "       num=> -20107,"
                         + "       msg=> 'Duplicate customer or order ID');" + " END order_info_insert;" + " /";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_CREATE_TRIGGER_STATEMENT));

    }

    @Test
    public void shouldParseGrantReadOnDirectory() {
        printTest("shouldParseGrantReadOnDirectory()");

        String content = "GRANT READ ON DIRECTORY bfile_dir TO hr \n" + "     WITH GRANT OPTION;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_GRANT_STATEMENT));
    }

    @Test
    public void shouldParseCreateFunction_1() {
        printTest("shouldParseCreateFunction_1()");
        String content = "CREATE OR REPLACE FUNCTION text_length(a CLOB)"
                         + " RETURN NUMBER DETERMINISTIC IS BEGIN RETURN DBMS_LOB.GETLENGTH(a); END; /";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_CREATE_FUNCTION_STATEMENT));
    }

    @Test
    public void shouldParseCreateProcedure_1() {
        printTest("shouldParseCreateProcedure_1()");
        String content = "CREATE PROCEDURE remove_emp (employee_id NUMBER) AS tot_emps NUMBER;" + NEWLINE + "BEGIN" + NEWLINE
                         + "   DELETE FROM employees" + NEWLINE + "   WHERE employees.employee_id = remove_emp.employee_id;"
                         + NEWLINE + "tot_emps := tot_emps - 1;" + NEWLINE + "END;" + NEWLINE + "/";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_CREATE_PROCEDURE_STATEMENT));
    }

    @Test
    public void shouldParseCreateProcedure_2() {
        printTest("shouldParseCreateProcedure_2()");
        String content = "CREATE OR REPLACE PROCEDURE add_emp (employee_id NUMBER, employee_age NUMBER) AS tot_emps NUMBER;"
                         + NEWLINE + "BEGIN" + NEWLINE + "   INSERT INTO employees" + NEWLINE
                         + "   WHERE employees.employee_id = remove_emp.employee_id;" + NEWLINE + "tot_emps := tot_emps + 1;"
                         + NEWLINE + "END;" + NEWLINE + "/";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_CREATE_PROCEDURE_STATEMENT));
        assertEquals(2, childNode.getChildCount());
    }

    @Test
    public void shouldParseOracleProceduresAndFunctions() {
        printTest("shouldParseOracleProceduresAndFunctions()");
        String content = getFileContent(DDL_FILE_PATH + "create_procedure_statements.ddl");
        assertScoreAndParse(content, "create_procedure_statements.ddl", 4);
    }

    @Test
    public void shouldParseCreateMaterializedView() {
        printTest("shouldParseCreateMaterializedView()");
        String content = " CREATE MATERIALIZED VIEW sales_mv" + NEWLINE + "BUILD IMMEDIATE" + NEWLINE + "REFRESH FAST ON COMMIT"
                         + NEWLINE + "AS SELECT t.calendar_year, p.prod_id, " + NEWLINE + "   SUM(s.amount_sold) AS sum_sales"
                         + NEWLINE + "   FROM times t, products p, sales s" + NEWLINE
                         + "   WHERE t.time_id = s.time_id AND p.prod_id = s.prod_id" + NEWLINE
                         + "   GROUP BY t.calendar_year, p.prod_id;" + NEWLINE;
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_CREATE_MATERIALIZED_VIEW_STATEMENT));
    }

    @Test
    public void shouldParseCreateMaterializedViewLog() {
        printTest("shouldParseCreateMaterializedViewLog()");
        String content = "CREATE MATERIALIZED VIEW LOG ON products" + NEWLINE + "WITH ROWID, SEQUENCE (prod_id)" + NEWLINE
                         + "INCLUDING NEW VALUES;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode, TYPE_CREATE_MATERIALIZED_VIEW_LOG_STATEMENT));
    }

    @Test
    public void shouldParseOracleStatements_1() {
        printTest("shouldParseOracleStatements_1()");
        String content = getFileContent(DDL_FILE_PATH + "oracle_test_statements_1.ddl");
        assertScoreAndParse(content, "oracle_test_statements_1", 50);
    }

    @Test
    public void shouldParseOracleStatements_2() {
        printTest("shouldParseOracleStatements_2()");
        String content = getFileContent(DDL_FILE_PATH + "oracle_test_statements_2.ddl");
        assertScoreAndParse(content, "oracle_test_statements_2", 50);
    }

    @Test
    public void shouldParseOracleStatements_3() {
        printTest("shouldParseOracleStatements_3()");
        String content = getFileContent(DDL_FILE_PATH + "oracle_test_statements_3.ddl");
        assertScoreAndParse(content, "oracle_test_statements_3", 50);
    }

    @Test
    public void shouldParseOracleStatements_4() {
        printTest("shouldParseOracleStatements_4()");
        String content = getFileContent(DDL_FILE_PATH + "oracle_test_statements_4.ddl");
        assertScoreAndParse(content, "oracle_test_statements_4", 48);
    }

    @FixFor( "MODE-1326" )
    @Test
    public void shouldSequenceCreateIndexStatements() throws Exception {
        String content = getFileContent(DDL_FILE_PATH + "mode_1326.ddl");
        assertScoreAndParse(content, "mode_1326", 477);
    }

    @Test
    public void shouldParseCreateTableIndexStatementOrderedWithVariations() {
        final String content = "CREATE TABLE BB_TEST_GROUP\n" //$NON-NLS-1$
                               + "(\n" //$NON-NLS-1$
                               + "BB_TEST_GROUP_ID NUMBER DEFAULT 0 NOT NULL,\n" //$NON-NLS-1$
                               + "TEST_GROUP_DISPLAY CHAR(15 BYTE) DEFAULT ' ' NOT NULL,\n" //$NON-NLS-1$
                               + "TEST_GROUP_DESCRIPTION  VARCHAR2(50 BYTE),\n" //$NON-NLS-1$
                               + "ACTIVE_IND NUMBER DEFAULT 0 NOT NULL,\n" //$NON-NLS-1$
                               + "ACTIVE_STATUS_CD NUMBER DEFAULT 0 NOT NULL,\n" //$NON-NLS-1$
                               // this includes PL/SQL and does not parse        + "ACTIVE_STATUS_DT_TM DATE DEFAULT TO_DATE ( '01/01/190000:00:00' , 'MM/DD/YYYYHH24:MI:SS' ) NOT NULL,\n" //$NON-NLS-1$
                               + "ACTIVE_STATUS_PRSNL_ID NUMBER DEFAULT 0 NOT NULL,\n" //$NON-NLS-1$
                               + "UPDT_CNT NUMBER DEFAULT 0 NOT NULL,\n" //$NON-NLS-1$
                               + "UPDT_DT_TM DATE DEFAULT SYSDATE NOT NULL,\n" //$NON-NLS-1$
                               + "UPDT_ID NUMBER DEFAULT 0 NOT NULL,\n" //$NON-NLS-1$
                               + "UPDT_TASK NUMBER DEFAULT 0 NOT NULL,\n" //$NON-NLS-1$
                               + "UPDT_APPLCTX NUMBER DEFAULT 0 NOT NULL\n" //$NON-NLS-1$
                               + ")\n" //$NON-NLS-1$
                               + "LOGGING \n" //$NON-NLS-1$
                               + "NOCOMPRESS \n" //$NON-NLS-1$
                               + "NOCACHE\n" //$NON-NLS-1$
                               + "NOPARALLEL\n" //$NON-NLS-1$
                               + "MONITORING;\n" //$NON-NLS-1$
                               + "" //$NON-NLS-1$
                               + "CREATE UNIQUE INDEX XAK1BB_TEST_GROUP ON BB_TEST_GROUP\n" //$NON-NLS-1$
                               + "(TEST_GROUP_DISPLAY ASC, UPDT_CNT DESC)\n" //$NON-NLS-1$
                               + "LOGGING\n" //$NON-NLS-1$
                               + "COMPRESS 4\n" //$NON-NLS-1$
                               + "NOPARALLEL\n" //$NON-NLS-1$
                               + "UNUSABLE;"; //$NON-NLS-1$
        this.parser.parse(content, this.rootNode, null);
        assertThat(this.rootNode.getChildCount(), is(2)); // table & index

        final List<AstNode> nodes = this.rootNode.childrenWithName("XAK1BB_TEST_GROUP");
        assertThat(nodes.size(), is(1));

        final AstNode indexNode = nodes.get(0);
        assertMixinType(indexNode, OracleDdlLexicon.TYPE_CREATE_TABLE_INDEX_STATEMENT);
        assertProperty(indexNode, OracleDdlLexicon.INDEX_TYPE, OracleDdlConstants.IndexTypes.TABLE);
        assertProperty(indexNode, OracleDdlLexicon.TABLE_NAME, "BB_TEST_GROUP");
        assertProperty(indexNode, OracleDdlLexicon.UNIQUE_INDEX, true);
        assertProperty(indexNode, OracleDdlLexicon.BITMAP_INDEX, false);
        assertProperty(indexNode, OracleDdlLexicon.UNUSABLE_INDEX, true);
        assertThat(indexNode.getProperty(OracleDdlLexicon.TABLE_ALIAS), is(nullValue()));
        assertThat(indexNode.getProperty(OracleDdlLexicon.OTHER_INDEX_REFS), is(nullValue()));
        assertThat(indexNode.getProperty(OracleDdlLexicon.WHERE_CLAUSE), is(nullValue()));

        // index attribute multi-value property
        @SuppressWarnings( "unchecked" )
        final List<String> indexAtributes = (List<String>)indexNode.getProperty(OracleDdlLexicon.INDEX_ATTRIBUTES);
        assertThat(indexAtributes.size(), is(3));
        assertThat(indexAtributes, hasItems("LOGGING", "COMPRESS 4", "NOPARALLEL"));

        // column references
        assertThat(indexNode.getChildCount(), is(2));

        { // TEST_GROUP_DISPLAY column
            final AstNode colRefNode = indexNode.getChild(0);
            assertThat(colRefNode.getName(), is("TEST_GROUP_DISPLAY"));
            assertProperty(colRefNode, OracleDdlLexicon.INDEX_ORDER, "ASC");
            assertMixinType(colRefNode, StandardDdlLexicon.TYPE_COLUMN_REFERENCE);
        }

        { // UPDT_CNT column
            final AstNode colRefNode = indexNode.getChild(1);
            assertThat(colRefNode.getName(), is("UPDT_CNT"));
            assertProperty(colRefNode, OracleDdlLexicon.INDEX_ORDER, "DESC");
            assertMixinType(colRefNode, StandardDdlLexicon.TYPE_COLUMN_REFERENCE);
        }
    }

    @Test
    public void shouldParseCreateTableIndexStatement() throws Exception {
        final String content = "CREATE TABLE CUST_MPAGE(\n" //$NON-NLS-1$
                               + "MPAGE_ID INTEGER,\n" //$NON-NLS-1$
                               + "NAME VARCHAR2(50 BYTE),\n" //$NON-NLS-1$
                               + "DESCRIPTION VARCHAR2(200 BYTE)\n" //$NON-NLS-1$
                               + ")\n" //$NON-NLS-1$
                               + "LOGGING\n" //$NON-NLS-1$
                               + "NOCOMPRESS\n" //$NON-NLS-1$
                               + "NOCACHE\n" //$NON-NLS-1$
                               + "NOPARALLEL\n" //$NON-NLS-1$
                               + "MONITORING;\n" //$NON-NLS-1$
                               + "" //$NON-NLS-1$
                               + "CREATE INDEX CUST_MPAGE_PK ON CUST_MPAGE (MPAGE_ID) LOGGING NOPARALLEL;";
        this.parser.parse(content, this.rootNode, null);
        assertThat(this.rootNode.getChildCount(), is(2)); // table & index

        final List<AstNode> nodes = this.rootNode.childrenWithName("CUST_MPAGE_PK");
        assertThat(nodes.size(), is(1));

        final AstNode indexNode = nodes.get(0);
        assertMixinType(indexNode, OracleDdlLexicon.TYPE_CREATE_TABLE_INDEX_STATEMENT);
        assertProperty(indexNode, OracleDdlLexicon.INDEX_TYPE, OracleDdlConstants.IndexTypes.TABLE);
        assertProperty(indexNode, OracleDdlLexicon.TABLE_NAME, "CUST_MPAGE");
        assertProperty(indexNode, OracleDdlLexicon.UNIQUE_INDEX, false);
        assertProperty(indexNode, OracleDdlLexicon.BITMAP_INDEX, false);
        assertProperty(indexNode, OracleDdlLexicon.UNUSABLE_INDEX, false);
        assertThat(indexNode.getProperty(OracleDdlLexicon.TABLE_ALIAS), is(nullValue()));
        assertThat(indexNode.getProperty(OracleDdlLexicon.OTHER_INDEX_REFS), is(nullValue()));
        assertThat(indexNode.getProperty(OracleDdlLexicon.WHERE_CLAUSE), is(nullValue()));

        // index attribute multi-value property
        @SuppressWarnings( "unchecked" )
        final List<String> indexAtributes = (List<String>)indexNode.getProperty(OracleDdlLexicon.INDEX_ATTRIBUTES);
        assertThat(indexAtributes.size(), is(2));
        assertThat(indexAtributes, hasItems("LOGGING", "NOPARALLEL"));

        // column reference
        assertThat(indexNode.getChildCount(), is(1));
        assertThat(indexNode.getFirstChild().getName(), is("MPAGE_ID"));
        assertMixinType(indexNode.getFirstChild(), StandardDdlLexicon.TYPE_COLUMN_REFERENCE);
    }

    @Test
    public void shouleParseCreateTableIndexWithFunctions() {
        final String content = "CREATE TABLE Weatherdata_tab(\n" //$NON-NLS-1$
                               + "Maxtemp INTEGER,\n" //$NON-NLS-1$
                               + "Mintemp INTEGER\n" //$NON-NLS-1$
                               + ");\n" //$NON-NLS-1$
                               + "" //$NON-NLS-1$
                               + "CREATE BITMAP INDEX Compare_index\n" //$NON-NLS-1$
                               + "ON Weatherdata_tab ((Maxtemp - Mintemp) DESC, Maxtemp);";
        this.parser.parse(content, this.rootNode, null);
        assertThat(this.rootNode.getChildCount(), is(2)); // table & index

        final List<AstNode> nodes = this.rootNode.childrenWithName("Compare_index");
        assertThat(nodes.size(), is(1));

        final AstNode indexNode = nodes.get(0);
        assertMixinType(indexNode, OracleDdlLexicon.TYPE_CREATE_TABLE_INDEX_STATEMENT);
        assertProperty(indexNode, OracleDdlLexicon.INDEX_TYPE, OracleDdlConstants.IndexTypes.TABLE);
        assertProperty(indexNode, OracleDdlLexicon.TABLE_NAME, "Weatherdata_tab");
        assertProperty(indexNode, OracleDdlLexicon.UNIQUE_INDEX, false);
        assertProperty(indexNode, OracleDdlLexicon.BITMAP_INDEX, true);
        assertProperty(indexNode, OracleDdlLexicon.UNUSABLE_INDEX, false);
        assertThat(indexNode.getProperty(OracleDdlLexicon.TABLE_ALIAS), is(nullValue()));
        assertThat(indexNode.getProperty(OracleDdlLexicon.WHERE_CLAUSE), is(nullValue()));

        // functions
        @SuppressWarnings( "unchecked" )
        final List<String> functionRefs = (List<String>)indexNode.getProperty(OracleDdlLexicon.OTHER_INDEX_REFS);
        assertThat(functionRefs.size(), is(1));
        assertThat(functionRefs, hasItems("(Maxtemp-Mintemp) DESC")); // parsing takes out internal spaces in the function

        // column reference
        assertThat(indexNode.getChildCount(), is(1));
        assertThat(indexNode.getFirstChild().getName(), is("Maxtemp"));
        assertMixinType(indexNode.getFirstChild(), StandardDdlLexicon.TYPE_COLUMN_REFERENCE);
    }

    @Test
    public void shouldParseCreateClusterIndexStatement() throws Exception {
        final String content = "CREATE INDEX idx_personnel ON CLUSTER personnel;"; //$NON-NLS-1$
        this.parser.parse(content, this.rootNode, null);
        assertThat(this.rootNode.getChildCount(), is(1)); // index

        final List<AstNode> nodes = this.rootNode.childrenWithName("idx_personnel");
        assertThat(nodes.size(), is(1)); // table & index

        final AstNode indexNode = nodes.get(0);
        assertMixinType(indexNode, OracleDdlLexicon.TYPE_CREATE_CLUSTER_INDEX_STATEMENT);
        assertProperty(indexNode, OracleDdlLexicon.INDEX_TYPE, OracleDdlConstants.IndexTypes.CLUSTER);
        assertProperty(indexNode, OracleDdlLexicon.CLUSTER_NAME, "personnel");
        assertThat(indexNode.getProperty(OracleDdlLexicon.TABLE_NAME), is(nullValue()));
        assertProperty(indexNode, OracleDdlLexicon.UNIQUE_INDEX, false);
        assertProperty(indexNode, OracleDdlLexicon.BITMAP_INDEX, false);
        assertProperty(indexNode, OracleDdlLexicon.UNUSABLE_INDEX, false);
        assertThat(indexNode.getProperty(OracleDdlLexicon.TABLE_ALIAS), is(nullValue()));
        assertThat(indexNode.getProperty(OracleDdlLexicon.OTHER_INDEX_REFS), is(nullValue()));
        assertThat(indexNode.getProperty(OracleDdlLexicon.INDEX_ATTRIBUTES), is(nullValue()));
        assertThat(indexNode.getProperty(OracleDdlLexicon.WHERE_CLAUSE), is(nullValue()));
    }

    @Test
    public void shouldParseCreateBitmapJoinIndexStatement() throws Exception {
        final String content = "CREATE TABLE sales(\n" //$NON-NLS-1$
                               + "cust_id INTEGER,\n" //$NON-NLS-1$
                               + "cust_name VARCHAR2(50 BYTE)\n" //$NON-NLS-1$
                               + ");\n" //$NON-NLS-1$
                               + "" //$NON-NLS-1$
                               + "CREATE TABLE customers(\n" //$NON-NLS-1$
                               + "cust_id INTEGER,\n" //$NON-NLS-1$
                               + "cust_name VARCHAR2(50 BYTE)\n" //$NON-NLS-1$
                               + ");\n" //$NON-NLS-1$
                               + "" //$NON-NLS-1$
                               + "CREATE BITMAP INDEX sales_cust_gender_bjix\n" //$NON-NLS-1$
                               + "ON sales(customers.cust_gender)\n" //$NON-NLS-1$
                               + "FROM sales, customers\n" //$NON-NLS-1$
                               + "WHERE sales.cust_id = customers.cust_id\n" //$NON-NLS-1$
                               + "LOCAL;"; //$NON-NLS-1$
        this.parser.parse(content, this.rootNode, null);
        assertThat(this.rootNode.getChildCount(), is(3)); // 2 tables & index

        final List<AstNode> nodes = this.rootNode.childrenWithName("sales_cust_gender_bjix");
        assertThat(nodes.size(), is(1));

        final AstNode indexNode = nodes.get(0);
        assertMixinType(indexNode, OracleDdlLexicon.TYPE_CREATE_BITMAP_JOIN_INDEX_STATEMENT);
        assertProperty(indexNode, OracleDdlLexicon.INDEX_TYPE, OracleDdlConstants.IndexTypes.BITMAP_JOIN);
        assertProperty(indexNode, OracleDdlLexicon.TABLE_NAME, "sales");
        assertProperty(indexNode, OracleDdlLexicon.UNIQUE_INDEX, false);
        assertProperty(indexNode, OracleDdlLexicon.BITMAP_INDEX, true);
        assertProperty(indexNode, OracleDdlLexicon.UNUSABLE_INDEX, false);
        assertProperty(indexNode, OracleDdlLexicon.WHERE_CLAUSE, "sales.cust_id = customers.cust_id LOCAL"); // index attributes
                                                                                                             // included
        assertThat(indexNode.getProperty(OracleDdlLexicon.INDEX_ATTRIBUTES), is(nullValue()));
        assertThat(indexNode.getProperty(OracleDdlLexicon.TABLE_ALIAS), is(nullValue()));
        assertThat(indexNode.getChildCount(), is(3)); // 1 column references, 2 table references

        { // 1 column reference
            final List<AstNode> colRefs = indexNode.getChildren(StandardDdlLexicon.TYPE_COLUMN_REFERENCE);
            assertThat(colRefs.size(), is(1));

            final AstNode colRefNode = colRefs.get(0);
            assertThat(colRefNode.getName(), is("customers.cust_gender"));
        }

        { // 2 table references
            final List<AstNode> tableRefs = indexNode.getChildren(StandardDdlLexicon.TYPE_TABLE_REFERENCE);
            assertThat(tableRefs.size(), is(2));

            { // sales table
                final AstNode tableRefNode = tableRefs.get(0);
                assertThat(tableRefNode.getName(), is("sales"));
            }

            { // customers table
                final AstNode tableRefNode = tableRefs.get(1);
                assertThat(tableRefNode.getName(), is("customers"));
            }
        }
    }

    @Test
    public void shouldParseDbObjectNameWithValidSymbols() {
        final String content = "CREATE TABLE EL$VIS (\n" //$NON-NLS-1$
                               + "COL_A VARCHAR2(20) NOT NULL,\n" //$NON-NLS-1$
                               + "COL@B VARCHAR2(10) NOT NULL,\n" //$NON-NLS-1$
                               + "COL#C NUMBER(10),\n" //$NON-NLS-1$
                               + "COL$D NUMBER(10));"; //$NON-NLS-1$
        this.parser.parse(content, this.rootNode, null);
        assertThat(this.rootNode.getChildCount(), is(1));

        final AstNode tableNode = this.rootNode.getChildren().get(0);
        assertThat(tableNode.getName(), is("EL$VIS"));
        assertThat(tableNode.getChildCount(), is(4)); // 4 columns

        assertThat(tableNode.childrenWithName("COL_A").size(), is(1));
        assertThat(tableNode.childrenWithName("COL@B").size(), is(1));
        assertThat(tableNode.childrenWithName("COL#C").size(), is(1));
        assertThat(tableNode.childrenWithName("COL$D").size(), is(1));
    }

    @Test
    @FixFor( "MODE-2604" )
    public void shouldParseCreateTableStatementsWithCheckConstraints() throws Exception {
        String content = getFileContent("ddl/create_table_check_constraint.ddl");
        assertScoreAndParse(content, "create_table_check_constraint.ddl", 2);
        List<AstNode> constraintNodes = parser.nodeFactory().getChildrenForType(rootNode, StandardDdlLexicon.TYPE_TABLE_CONSTRAINT);
        assertEquals(2, constraintNodes.size());
    }

    @Test
    @FixFor( "MODE-2604" )
    public void shouldParseAlterTableStatementsWithCheckConstraints() throws Exception {
        String content = getFileContent("ddl/alter_table_check_constraint.ddl");
        assertScoreAndParse(content, "alter_table_check_constraint.ddl", 2);
        List<AstNode> constraintNodes = parser.nodeFactory().getChildrenForType(rootNode, StandardDdlLexicon.TYPE_ADD_TABLE_CONSTRAINT_DEFINITION);
        assertEquals(2, constraintNodes.size());
    }
}
