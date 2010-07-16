package org.modeshape.sequencer.ddl.dialect.oracle;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_ALTER_TABLE_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_GRANT_STATEMENT;
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
import org.junit.Before;
import org.junit.Test;
import org.modeshape.common.FixFor;
import org.modeshape.graph.JcrLexicon;
import org.modeshape.sequencer.ddl.DdlConstants;
import org.modeshape.sequencer.ddl.DdlParserScorer;
import org.modeshape.sequencer.ddl.DdlParserTestHelper;
import org.modeshape.sequencer.ddl.node.AstNode;

public class OracleDdlParserTest extends DdlParserTestHelper {

    public static final String DDL_FILE_PATH = "src/test/resources/ddl/dialect/oracle/";

    @Before
    public void beforeEach() {
        parser = new OracleDdlParser();
        setPrintToConsole(false);
        parser.setTestMode(isPrintToConsole());
        parser.setDoUseTerminator(true);
        rootNode = parser.nodeFactory().node("ddlRootNode");
        scorer = new DdlParserScorer();
    }

    // @Test
    // public void shouldParseOracleDDL() {
    // String content = getFileContent(DDL_FILE_PATH + "oracle_test_create.ddl");
    //	  
    // List<Statement> stmts = parser.parse(content);
    //	  	
    // System.out.println("  END PARSING.  # Statements = " + stmts.size());
    //	  
    // }

    @Test
    public void shouldParseCreateOrReplaceTrigger() {
        printTest("shouldParseCreateOrReplaceTrigger()");
        String content = "CREATE OR REPLACE TRIGGER drop_trigger" + DdlConstants.SPACE + "BEFORE DROP ON hr.SCHEMA"
                         + DdlConstants.SPACE + "BEGIN" + DdlConstants.SPACE
                         + "RAISE_APPLICATION_ERROR ( num => -20000,msg => 'Cannot drop object');" + DdlConstants.SPACE + "END;"
                         + DdlConstants.SPACE + "/";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_TRIGGER_STATEMENT));

    }

    @Test
    public void shouldParseAnalyze() {
        printTest("shouldParseAnalyze()");
        String content = "ANALYZE TABLE customers VALIDATE STRUCTURE ONLINE;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ANALYZE_STATEMENT));
    }

    @Test
    public void shouldParseRollbackToSavepoint() {
        printTest("shouldParseRollbackToSavepoint()");
        String content = "ROLLBACK TO SAVEPOINT banda_sal;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ROLLBACK_STATEMENT));
    }

    @Test
    public void shouldParseAlterTableAddREF() {
        printTest("shouldParseAlterTableAddREF()");
        String content = "ALTER TABLE staff ADD (REF(dept) WITH ROWID);";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_TABLE_STATEMENT));
    }

    @Test
    public void shouldParseAlterTableADDWithNESTED_TABLE() {
        // This is a one-off case where there is a custom datatype (i.e. skill_table_type)
        printTest("shouldParseAlterTableADDWithNESTED_TABLE()");
        String content = "ALTER TABLE employees ADD (skills skill_table_type) NESTED TABLE skills STORE AS nested_skill_table;";
        assertScoreAndParse(content, null, 2); // ALTER TABLE + 1 PROBLEM
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_TABLE_STATEMENT));
    }

    @Test
    public void shouldParseAlterIndexRename() {
        printTest("shouldParseAlterIndexRename()");
        String content = "ALTER INDEX upper_ix RENAME TO upper_name_ix;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_INDEX_STATEMENT));
    }

    @Test
    public void shouldParseAlterIndexMODIFY() {
        printTest("shouldParseAlterIndexMODIFY()");
        String content = "ALTER INDEX cost_ix MODIFY PARTITION p3 STORAGE(MAXEXTENTS 30) LOGGING;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_INDEX_STATEMENT));
    }

    @Test
    public void shouldParseAlterIndexDROP() {
        printTest("shouldParseAlterIndexDROP()");
        String content = "ALTER INDEX cost_ix DROP PARTITION p1;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_INDEX_STATEMENT));
    }

    @Test
    public void shouldParseAlterIndexTypeADD() {
        printTest("shouldParseAlterIndexTypeADD()");
        String content = "ALTER INDEXTYPE position_indextype ADD lob_contains(CLOB, CLOB);";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_INDEXTYPE_STATEMENT));
    }

    @Test
    public void shouldParseTEMP_TEST() {
        printTest("shouldParseTEMP_TEST()");
        String content = "COMMENT ON COLUMN employees.job_id IS 'abbreviated job title';";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_COMMENT_ON_STATEMENT));
    }

    // GRANT ALL ON bonuses TO hr WITH GRANT OPTION;
    @Test
    public void shouldParseGrantAllOn() {
        printTest("shouldParseGrant()");
        String content = "GRANT ALL ON bonuses TO hr WITH GRANT OPTION;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_GRANT_STATEMENT));
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
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_TABLE_STATEMENT));
    }

    @Test
    public void shouldParseAlterTableWithAddColumns() {
        printTest("shouldParseAlterTableWithModifyClause()");

        String content = "ALTER TABLE countries \n" + "     ADD (duty_pct     NUMBER(2,2)  CHECK (duty_pct < 10.5),\n"
                         + "     visa_needed  VARCHAR2(3));";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_TABLE_STATEMENT));
        assertEquals(3, childNode.getChildCount()); // 2 columns + CHECK constraint
    }

    @Test
    public void shouldParseJava() {
        printTest("shouldParseJava()");

        String content = "CREATE JAVA SOURCE NAMED \"Hello\" AS public class Hello { public static String hello() {return \"Hello World\";   } };";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_JAVA_STATEMENT));
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
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_TRIGGER_STATEMENT));

    }

    @Test
    public void shouldParseGrantReadOnDirectory() {
        printTest("shouldParseGrantReadOnDirectory()");

        String content = "GRANT READ ON DIRECTORY bfile_dir TO hr \n" + "     WITH GRANT OPTION;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_GRANT_STATEMENT));
    }

    @Test
    public void shouldParseCreateFunction_1() {
        printTest("shouldParseCreateFunction_1()");
        String content = "CREATE OR REPLACE FUNCTION text_length(a CLOB)"
                         + " RETURN NUMBER DETERMINISTIC IS BEGIN RETURN DBMS_LOB.GETLENGTH(a); END; /";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_FUNCTION_STATEMENT));
    }

    @Test
    public void shouldParseCreateProcedure_1() {
        printTest("shouldParseCreateProcedure_1()");
        String content = "CREATE PROCEDURE remove_emp (employee_id NUMBER) AS tot_emps NUMBER;" + NEWLINE + "BEGIN" + NEWLINE
                         + "   DELETE FROM employees" + NEWLINE + "   WHERE employees.employee_id = remove_emp.employee_id;"
                         + NEWLINE + "tot_emps := tot_emps - 1;" + NEWLINE + "END;" + NEWLINE + "/";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_PROCEDURE_STATEMENT));
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
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_PROCEDURE_STATEMENT));
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
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_MATERIALIZED_VIEW_STATEMENT));
    }

    @Test
    public void shouldParseCreateMaterializedViewLog() {
        printTest("shouldParseCreateMaterializedViewLog()");
        String content = "CREATE MATERIALIZED VIEW LOG ON products" + NEWLINE + "WITH ROWID, SEQUENCE (prod_id)" + NEWLINE
                         + "INCLUDING NEW VALUES;";
        assertScoreAndParse(content, null, 1);
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_MATERIALIZED_VIEW_LOG_STATEMENT));
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
}
