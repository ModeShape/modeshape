package org.jboss.dna.sequencer.ddl.dialect.derby;

import static org.hamcrest.core.Is.is;
import static org.jboss.dna.sequencer.ddl.StandardDdlLexicon.TYPE_PROBLEM;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.List;

import org.jboss.dna.sequencer.ddl.DdlParserTestHelper;
import org.jboss.dna.sequencer.ddl.StandardDdlParser;
import org.jboss.dna.sequencer.ddl.node.AstNode;
import org.junit.Before;
import org.junit.Test;

public class DerbyDdlParserTest extends DdlParserTestHelper{
	private StandardDdlParser parser;
	private AstNode rootNode;
	
	public static final String DDL_FILE_PATH = "src/test/resources/ddl/dialect/derby/";
	
	
	@Before
	public void beforeEach() {
		parser = new DerbyDdlParser();
		setPrintToConsole(false);
    	parser.setTestMode(isPrintToConsole());
    	parser.setDoUseTerminator(true);
    	rootNode = parser.nodeFactory().node("ddlRootNode");
	}
    
//	@Test
//	public void shouldParseDerbyDDL() {
//	  	String content = getFileContent(DDL_FILE_PATH + "derby_test_create.ddl");
//	  
//	  	List<Statement> stmts = parser.parse(content);
//	  	
//	  	assertEquals(744, stmts.size());
//
//	  	System.out.println("  END PARSING.  # Statements = " + stmts.size());
//	  
//	}
	
    @Test
    public void shouldParseDropSchemaRestrict() {
    	printTest("shouldParseDeclareGlobaTemporaryTable()");
    	String content = "DROP SCHEMA SAMP RESTRICT;";

    	boolean success = parser.parse(content, rootNode);
    	assertThat(true, is(success));
    }
    
	
    @Test
    public void shouldParseDeclareGlobaTemporaryTable() {
    	printTest("shouldParseDeclareGlobaTemporaryTable()");
    	String content = "declare global temporary table SESSION.t1(c11 int) not logged;";

    	boolean success = parser.parse(content, rootNode);
    	assertThat(true, is(success));
    }
	
    @Test
    public void shouldParseAlterTableAlterColumnDefaultRealNumber() {
    	printTest("shouldParseAlterTableAlterColumnDefaultRealNumber()");
    	String content = "ALTER TABLE Employees ALTER COLUMN Salary DEFAULT 1000.0;";

    	boolean success = parser.parse(content, rootNode);
    	assertThat(true, is(success));
    }
    
	
	
	@Test
	public void shouldParseDerbyStatements() {
		printTest("shouldParseDerbyStatements()");
	  	String content = getFileContent(DDL_FILE_PATH + "derby_test_statements.ddl");

    	boolean success = parser.parse(content, rootNode);
    	assertThat(true, is(success));
		
		List<AstNode> problems = parser.nodeFactory().getChildrenForType(rootNode, TYPE_PROBLEM);
		int nStatements = rootNode.getChildCount() - problems.size();
		assertEquals(64, nStatements);
		
	}
}