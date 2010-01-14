package org.modeshape.sequencer.ddl.dialect.oracle;

import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_ALTER_TABLE_STATEMENT;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_GRANT_STATEMENT;
import static org.modeshape.sequencer.ddl.dialect.oracle.OracleDdlLexicon.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.modeshape.graph.JcrLexicon;
import org.modeshape.sequencer.ddl.DdlConstants;
import org.modeshape.sequencer.ddl.DdlParserTestHelper;
import org.modeshape.sequencer.ddl.StandardDdlParser;
import org.modeshape.sequencer.ddl.node.AstNode;
import org.junit.Before;
import org.junit.Test;

public class OracleDdlParserTest extends DdlParserTestHelper {
	private StandardDdlParser parser;
	private AstNode rootNode;
	
	public static final String DDL_FILE_PATH = "src/test/resources/ddl/dialect/oracle/";
	
    @Before
    public void beforeEach() {
    	parser = new OracleDdlParser();
    	setPrintToConsole(false);
    	parser.setTestMode(isPrintToConsole());
    	parser.setDoUseTerminator(true);
    	rootNode = parser.nodeFactory().node("ddlRootNode");
    }
    
//	@Test
//	public void shouldParseOracleDDL() {
//	  	String content = getFileContent(DDL_FILE_PATH + "oracle_test_create.ddl");
//	  
//	  	List<Statement> stmts = parser.parse(content);
//	  	
//	  	System.out.println("  END PARSING.  # Statements = " + stmts.size());
//	  
//	}
    
    @Test
    public void shouldParseCreateOrReplaceTrigger() {
    	printTest("shouldParseCreateOrReplaceTrigger()");
    	String content = "CREATE OR REPLACE TRIGGER drop_trigger" + DdlConstants.SPACE + 
    	   "BEFORE DROP ON hr.SCHEMA" + DdlConstants.SPACE + 
    	   "BEGIN" + DdlConstants.SPACE + 
    	   		"RAISE_APPLICATION_ERROR ( num => -20000,msg => 'Cannot drop object');" + DdlConstants.SPACE + 
    	   "END;" + DdlConstants.SPACE +  "/";
    	
    	boolean success = parser.parse(content, rootNode);
    	
    	assertEquals(true, success);
    	assertEquals(1, rootNode.getChildCount());
		AstNode childNode = rootNode.getChildren().get(0);
		assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_TRIGGER_STATEMENT));
    	
    }
    
    @Test 
    public void shouldParseAnalyze() {
    	printTest("shouldParseAnalyze()");
    	String content = "ANALYZE TABLE customers VALIDATE STRUCTURE ONLINE;";

    	boolean success = parser.parse(content, rootNode);
    	
    	assertEquals(true, success);
    	assertEquals(1, rootNode.getChildCount());
		AstNode childNode = rootNode.getChildren().get(0);
		assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ANALYZE_STATEMENT));
    }
    
    @Test
    public void shouldParseRollbackToSavepoint() {
    	printTest("shouldParseRollbackToSavepoint()");
    	String content = "ROLLBACK TO SAVEPOINT banda_sal;";

    	boolean success = parser.parse(content, rootNode);
    	
    	assertEquals(true, success);
    	assertEquals(1, rootNode.getChildCount());
		AstNode childNode = rootNode.getChildren().get(0);
		assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ROLLBACK_STATEMENT));
    }
    
    @Test
    public void shouldParseAlterTableAddREF() {
    	printTest("shouldParseAlterTableAddREF()");
    	String content = "ALTER TABLE staff ADD (REF(dept) WITH ROWID);";

    	boolean success = parser.parse(content, rootNode);

    	assertEquals(true, success);
    	assertEquals(1, rootNode.getChildCount());
		AstNode childNode = rootNode.getChildren().get(0);
		assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_TABLE_STATEMENT));
    }
	
    @Test
    public void shouldParseAlterTableADDWithNESTED_TABLE() {
    	// This is a one-off case where there is a custom datatype (i.e. skill_table_type)
    	printTest("shouldParseAlterTableADDWithNESTED_TABLE()");
    	String content = "ALTER TABLE employees ADD (skills skill_table_type) NESTED TABLE skills STORE AS nested_skill_table;";

    	boolean success = parser.parse(content, rootNode);
    	
    	assertEquals(false, success); // DO NOT HAVE CUSTOM DATATYPE wired to isColumnDefintion() method.
    	assertEquals(2, rootNode.getChildCount());  // ALTER TABLE + 1 PROBLEM
		AstNode childNode = rootNode.getChildren().get(0);
		assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_TABLE_STATEMENT));
    }
    
    @Test
    public void shouldParseAlterIndexRename() {
    	printTest("shouldParseAlterIndexRename()");
    	String content = "ALTER INDEX upper_ix RENAME TO upper_name_ix;";

    	boolean success = parser.parse(content, rootNode);
    	
    	assertEquals(true, success);
    	assertEquals(1, rootNode.getChildCount());
		AstNode childNode = rootNode.getChildren().get(0);
		assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_INDEX_STATEMENT));
    }
    
    @Test
    public void shouldParseAlterIndexMODIFY() {
    	printTest("shouldParseAlterIndexMODIFY()");
    	String content = "ALTER INDEX cost_ix MODIFY PARTITION p3 STORAGE(MAXEXTENTS 30) LOGGING;";

    	boolean success = parser.parse(content, rootNode);
    	
    	assertEquals(true, success);
    	assertEquals(1, rootNode.getChildCount());
		AstNode childNode = rootNode.getChildren().get(0);
		assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_INDEX_STATEMENT));
    }
    
    @Test
    public void shouldParseAlterIndexDROP() {
    	printTest("shouldParseAlterIndexDROP()");
    	String content = "ALTER INDEX cost_ix DROP PARTITION p1;";

    	boolean success = parser.parse(content, rootNode);
    	
    	assertEquals(true, success);
    	assertEquals(1, rootNode.getChildCount());
		AstNode childNode = rootNode.getChildren().get(0);
		assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_INDEX_STATEMENT));
    }
    
    @Test
    public void shouldParseAlterIndexTypeADD() {
    	printTest("shouldParseAlterIndexTypeADD()");
    	String content = "ALTER INDEXTYPE position_indextype ADD lob_contains(CLOB, CLOB);";

    	boolean success = parser.parse(content, rootNode);
    	
    	assertEquals(true, success);
    	assertEquals(1, rootNode.getChildCount());
		AstNode childNode = rootNode.getChildren().get(0);
		assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_INDEXTYPE_STATEMENT));
    }
    
    @Test
    public void shouldParseTEMP_TEST() {
    	printTest("shouldParseTEMP_TEST()");
        String content = "COMMENT ON COLUMN employees.job_id IS 'abbreviated job title';";

    	boolean success = parser.parse(content, rootNode);
    	
    	assertEquals(true, success);
    	assertEquals(1, rootNode.getChildCount());
		AstNode childNode = rootNode.getChildren().get(0);
		assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_COMMENT_ON_STATEMENT));
    }
    // GRANT ALL ON bonuses TO hr WITH GRANT OPTION;
    @Test
    public void shouldParseGrantAllOn() {
    	printTest("shouldParseGrant()");
        String content = "GRANT ALL ON bonuses TO hr WITH GRANT OPTION;";

    	boolean success = parser.parse(content, rootNode);
    	
    	assertEquals(true, success);
    	assertEquals(1, rootNode.getChildCount());
		AstNode childNode = rootNode.getChildren().get(0);
		assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_GRANT_STATEMENT));
    }
    
    @Test
    public void shouldParseAlterTableWithModifyClause() {
    	printTest("shouldParseAlterTableWithModifyClause()");
    	
    	String content = "ALTER TABLE employees MODIFY LOB (resume) (CACHE);";

    	boolean success = parser.parse(content, rootNode);
    	
    	assertEquals(true, success);
    	assertEquals(1, rootNode.getChildCount());
		AstNode childNode = rootNode.getChildren().get(0);
		assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_TABLE_STATEMENT));
    }
    
    @Test
    public void shouldParseAlterTableWithAddColumns() {
    	printTest("shouldParseAlterTableWithModifyClause()");
    	
    	String content = "ALTER TABLE countries \n"
    				   + "     ADD (duty_pct     NUMBER(2,2)  CHECK (duty_pct < 10.5),\n"
    				   + "     visa_needed  VARCHAR2(3));"; 

    	boolean success = parser.parse(content, rootNode);
    	
    	assertEquals(true, success);
    	assertEquals(1, rootNode.getChildCount());
		AstNode childNode = rootNode.getChildren().get(0);
		assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_ALTER_TABLE_STATEMENT));
		assertEquals(3, childNode.getChildCount()); // 2 columns + CHECK constraint
    }


    
    @Test
    public void shouldParseJava() {
    	printTest("shouldParseJava()");
    	
    	String content = "CREATE JAVA SOURCE NAMED \"Hello\" AS public class Hello { public static String hello() {return \"Hello World\";   } };";

    	boolean success = parser.parse(content, rootNode);
    	
    	assertEquals(true, success);
    	assertEquals(1, rootNode.getChildCount());
		AstNode childNode = rootNode.getChildren().get(0);
		assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_JAVA_STATEMENT));
    }
    
    @Test
    public void shouldParseCreateOrReplaceTriggerWithEmbeddedStatements() {
    	printTest("shouldParseCreateOrReplaceTriggerWithEmbeddedStatements()");
    	
    	String content = "CREATE OR REPLACE TRIGGER order_info_insert"
			    + " INSTEAD OF INSERT ON order_info"
				+ " DECLARE"
				+ "   duplicate_info EXCEPTION;"
				+ "   PRAGMA EXCEPTION_INIT (duplicate_info, -00001);"
				+ " BEGIN"
				+ "   INSERT INTO customers"
				+ "     (customer_id, cust_last_name, cust_first_name)"
				+ "   VALUES ("
				+ "   :new.customer_id, "
				+ "   :new.cust_last_name,"
				+ "   :new.cust_first_name);"
				+ " INSERT INTO orders (order_id, order_date, customer_id)"
				+ " VALUES ("
				+ "   :new.order_id,"
				+ "   :new.order_date,"
				+ "   :new.customer_id);"
				+ " EXCEPTION"
				+ "   WHEN duplicate_info THEN"
				+ "    RAISE_APPLICATION_ERROR ("
				+ "       num=> -20107,"
				+ "       msg=> 'Duplicate customer or order ID');"
				+ " END order_info_insert;"
				+ " /";
    	
    	boolean success = parser.parse(content, rootNode);
    	
    	assertEquals(true, success);
    	assertEquals(1, rootNode.getChildCount());
		AstNode childNode = rootNode.getChildren().get(0);
		assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_TRIGGER_STATEMENT));
    	
    }
    
    @Test
    public void shouldParseGrantReadOnDirectory() {
    	printTest("shouldParseGrantReadOnDirectory()");
    	
    	String content = "GRANT READ ON DIRECTORY bfile_dir TO hr \n"
    		+ "     WITH GRANT OPTION;";

    	boolean success = parser.parse(content, rootNode);
    	
    	assertEquals(true, success);
    	assertEquals(1, rootNode.getChildCount());
		AstNode childNode = rootNode.getChildren().get(0);
		assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_GRANT_STATEMENT));
    }
    
    @Test
    public void shouldParseCreateFunction_1() {
    	printTest("shouldParseCreateFunction_1()");
    	String content = "CREATE OR REPLACE FUNCTION text_length(a CLOB)"
    		+ " RETURN NUMBER DETERMINISTIC IS BEGIN RETURN DBMS_LOB.GETLENGTH(a); END; /";
    	
    	boolean success = parser.parse(content, rootNode);
    	
    	assertEquals(true, success);
    	assertEquals(1, rootNode.getChildCount());
		AstNode childNode = rootNode.getChildren().get(0);
		assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_FUNCTION_STATEMENT));
    }
    
    @Test
    public void shouldParseCreateProcedure_1() {
        printTest("shouldParseCreateProcedure_1()");
        String content = "CREATE PROCEDURE remove_emp (employee_id NUMBER) AS tot_emps NUMBER;" + NEWLINE
                               + "BEGIN" + NEWLINE
                               + "   DELETE FROM employees" + NEWLINE
                               + "   WHERE employees.employee_id = remove_emp.employee_id;" + NEWLINE
                               + "tot_emps := tot_emps - 1;" + NEWLINE
                               + "END;" + NEWLINE
                               + "/";
        
        boolean success = parser.parse(content, rootNode);
        
        assertEquals(true, success);
        assertEquals(1, rootNode.getChildCount());
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_PROCEDURE_STATEMENT));
    }
    
    @Test
    public void shouldParseCreateProcedure_2() {
        printTest("shouldParseCreateProcedure_2()");
        String content = "CREATE OR REPLACE PROCEDURE add_emp (employee_id NUMBER, employee_age NUMBER) AS tot_emps NUMBER;" + NEWLINE
                               + "BEGIN" + NEWLINE
                               + "   INSERT INTO employees" + NEWLINE
                               + "   WHERE employees.employee_id = remove_emp.employee_id;" + NEWLINE
                               + "tot_emps := tot_emps + 1;" + NEWLINE
                               + "END;" + NEWLINE
                               + "/";
        
        boolean success = parser.parse(content, rootNode);
        
        assertEquals(true, success);
        assertEquals(1, rootNode.getChildCount());
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_PROCEDURE_STATEMENT));
        assertEquals(2, childNode.getChildCount());
    }
    
    @Test
    public void shouldParseOracleProceduresAndFunctions() {
        printTest("shouldParseOracleProceduresAndFunctions()");
        String content = getFileContent(DDL_FILE_PATH + "create_procedure_statements.ddl");

        boolean success = parser.parse(content, rootNode);
        
        assertEquals(true, success);
        assertEquals(4, rootNode.getChildCount());
    }
    
    @Test
    public void shouldParseCreateMaterializedView() {
        printTest("shouldParseCreateMaterializedView()");
        String content = " CREATE MATERIALIZED VIEW sales_mv" + NEWLINE
                                + "BUILD IMMEDIATE" + NEWLINE
                                + "REFRESH FAST ON COMMIT" + NEWLINE
                                + "AS SELECT t.calendar_year, p.prod_id, " + NEWLINE
                                + "   SUM(s.amount_sold) AS sum_sales" + NEWLINE
                                + "   FROM times t, products p, sales s" + NEWLINE
                                + "   WHERE t.time_id = s.time_id AND p.prod_id = s.prod_id" + NEWLINE
                                + "   GROUP BY t.calendar_year, p.prod_id;" + NEWLINE;
        
        boolean success = parser.parse(content, rootNode);
        
        assertEquals(true, success);
        assertEquals(1, rootNode.getChildCount());
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_MATERIALIZED_VIEW_STATEMENT));
    }
    
    @Test
    public void shouldParseCreateMaterializedViewLog() {
        printTest("shouldParseCreateMaterializedViewLog()");
        String content = "CREATE MATERIALIZED VIEW LOG ON products" + NEWLINE
                                + "WITH ROWID, SEQUENCE (prod_id)" + NEWLINE
                                + "INCLUDING NEW VALUES;";
        
        boolean success = parser.parse(content, rootNode);
        
        assertEquals(true, success);
        assertEquals(1, rootNode.getChildCount());
        AstNode childNode = rootNode.getChildren().get(0);
        assertTrue(hasMixinType(childNode.getProperty(JcrLexicon.MIXIN_TYPES), TYPE_CREATE_MATERIALIZED_VIEW_LOG_STATEMENT));
    }
    
	@Test
	public void shouldParseOracleStatements_1() {
		printTest("shouldParseOracleStatements_1()");
	  	String content = getFileContent(DDL_FILE_PATH + "oracle_test_statements_1.ddl");

	  	boolean success = parser.parse(content, rootNode);
	  	
	  	assertEquals(true, success);
	  	assertEquals(50, rootNode.getChildCount());
	}
	
	@Test
	public void shouldParseOracleStatements_2() {
		printTest("shouldParseOracleStatements_2()");
	  	String content = getFileContent(DDL_FILE_PATH + "oracle_test_statements_2.ddl");

	  	boolean success = parser.parse(content, rootNode);
	  	
	  	assertEquals(true, success);
	  	assertEquals(50, rootNode.getChildCount());
	}
	
	@Test
	public void shouldParseOracleStatements_3() {
		printTest("shouldParseOracleStatements_3()");
	  	String content = getFileContent(DDL_FILE_PATH + "oracle_test_statements_3.ddl");

	  	boolean success = parser.parse(content, rootNode);
	  	
	  	assertEquals(true, success);
	  	assertEquals(50, rootNode.getChildCount());
	}
	
	@Test
	public void shouldParseOracleStatements_4() {
		printTest("shouldParseOracleStatements_4()");
	  	String content = getFileContent(DDL_FILE_PATH + "oracle_test_statements_4.ddl");

	  	boolean success = parser.parse(content, rootNode);
	  	
	  	assertEquals(true, success);
	  	assertEquals(48, rootNode.getChildCount());
	}

}
