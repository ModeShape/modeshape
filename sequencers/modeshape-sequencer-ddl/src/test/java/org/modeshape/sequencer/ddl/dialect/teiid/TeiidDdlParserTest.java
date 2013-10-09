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
package org.modeshape.sequencer.ddl.dialect.teiid;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.modeshape.sequencer.ddl.DdlParserScorer;
import org.modeshape.sequencer.ddl.DdlParserTestHelper;
import org.modeshape.sequencer.ddl.node.AstNode;

/**
 * A test class for the {@link TeiidDdlParser}.
 */
public class TeiidDdlParserTest extends DdlParserTestHelper implements TeiidDdlConstants {

    public static final String DDL_FILE_PATH = "ddl/dialect/teiid/";
	
    @Before
    public void beforeEach() {
        this.parser = new TeiidDdlParser();
        setRootNode(this.parser.nodeFactory().node("DdlRootNode"));
        this.scorer = new DdlParserScorer();
    }

    /**
     * See Teiid TestDDLParser#testDuplicateFunctions()
     */
    @Test
    public void shouldParseDuplicateFunctions() {
        final String functionName = "SourceFunc";
        final String content = "CREATE FUNCTION " + functionName + "() RETURNS string; CREATE FUNCTION " + functionName
                               + "(param string) RETURNS string";
        assertScoreAndParse(content, null, 2);

        final List<AstNode> kids = getRootNode().getChildren();
        assertMixinType(kids.get(0), TeiidDdlLexicon.CreateProcedure.FUNCTION_STATEMENT);
        assertMixinType(kids.get(1), TeiidDdlLexicon.CreateProcedure.FUNCTION_STATEMENT);
    }

    /**
     * See Teiid TestDDLParser#testDuplicateFunctions1()
     */
    @Test
    public void shouldParseDuplicateFunctions1() {
        final String content = "CREATE FUNCTION SourceFunc() RETURNS string OPTIONS (UUID 'a'); CREATE FUNCTION SourceFunc1() RETURNS string OPTIONS (UUID 'a')";
        assertScoreAndParse(content, null, 2);

        {
            final List<AstNode> kids = getRootNode().childrenWithName("SourceFunc");
            assertThat(kids.size(), is(1));
            assertMixinType(kids.get(0), TeiidDdlLexicon.CreateProcedure.FUNCTION_STATEMENT);
        }

        {
            final List<AstNode> kids = getRootNode().childrenWithName("SourceFunc1");
            assertThat(kids.size(), is(1));
            assertMixinType(kids.get(0), TeiidDdlLexicon.CreateProcedure.FUNCTION_STATEMENT);
        }
    }

    /**
     * See Teiid TestDDLParser#testMultipleCommands()
     */
    @Test
    public void shouldParseMultipleCommands() {
        final String content = "CREATE VIEW V1 AS SELECT * FROM PM1.G1 "
                               + "CREATE PROCEDURE FOO(P1 integer) RETURNS (e1 integer, e2 varchar) AS SELECT * FROM PM1.G1;";
        assertScoreAndParse(content, null, 2);

        {
            final List<AstNode> kids = getRootNode().childrenWithName("V1");
            assertThat(kids.size(), is(1));
            assertMixinType(kids.get(0), TeiidDdlLexicon.CreateTable.VIEW_STATEMENT);
        }

        {
            final List<AstNode> kids = getRootNode().childrenWithName("FOO");
            assertThat(kids.size(), is(1));
            assertMixinType(kids.get(0), TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT);
        }
    }

    /**
     * See Teiid TestDDLParser#testMultipleCommands2()
     */
    @Test
    public void shouldParseMultipleCommands2() {
        final String content = "CREATE VIRTUAL PROCEDURE getTweets(query varchar) RETURNS (created_on varchar(25), "
                               + "from_user varchar(25), to_user varchar(25), "
                               + "profile_image_url varchar(25), source varchar(25), text varchar(140)) AS "
                               + "select tweet.* from "
                               + "(call twitter.invokeHTTP(action => 'GET', endpoint =>querystring('',query as \"q\"))) w, "
                               + "XMLTABLE('results' passing JSONTOXML('myxml', w.result) columns "
                               + "created_on string PATH 'created_at', " + "from_user string PATH 'from_user', "
                               + "to_user string PATH 'to_user', " + "profile_image_url string PATH 'profile_image_url', "
                               + "source string PATH 'source', " + "text string PATH 'text') tweet;"
                               + "CREATE VIEW Tweet AS select * FROM twitterview.getTweets;";
        assertScoreAndParse(content, null, 2);

        {
            final List<AstNode> kids = getRootNode().childrenWithName("getTweets");
            assertThat(kids.size(), is(1));
            assertMixinType(kids.get(0), TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT);
        }

        {
            final List<AstNode> kids = getRootNode().childrenWithName("Tweet");
            assertThat(kids.size(), is(1));
            assertMixinType(kids.get(0), TeiidDdlLexicon.CreateTable.VIEW_STATEMENT);
        }
    }

    /**
     * See Teiid TestDDLParser#testInsteadOfTrigger()
     */
    @Test
    public void shouldParseInsteadOfTrigger() {
        final String content = "CREATE VIEW G1( e1 integer, e2 varchar) AS select * from foo;"
                               + "CREATE TRIGGER ON G1 INSTEAD OF INSERT AS " + "FOR EACH ROW " + "BEGIN ATOMIC "
                               + "insert into g1 (e1, e2) values (1, 'trig');" + "END;"
                               + "CREATE View G2( e1 integer, e2 varchar) AS select * from foo;";
        assertScoreAndParse(content, null, 3);

        {
            final List<AstNode> kids = getRootNode().childrenWithName("G1");
            assertThat(kids.size(), is(2)); // view, trigger
            assertMixinType(kids.get(0), TeiidDdlLexicon.CreateTable.VIEW_STATEMENT);
            assertMixinType(kids.get(1), TeiidDdlLexicon.CreateTrigger.STATEMENT);
        }

        {
            final List<AstNode> kids = getRootNode().childrenWithName("G2");
            assertThat(kids.size(), is(1));
            assertMixinType(kids.get(0), TeiidDdlLexicon.CreateTable.VIEW_STATEMENT);
        }
    }

    /**
     * See Teiid TestDDLParser#testFK()
     */
    @Test
    public void shouldParseForeignKey() {
        final String content = "CREATE FOREIGN TABLE G1(g1e1 integer, g1e2 varchar, PRIMARY KEY(g1e1, g1e2));"
                               + "CREATE FOREIGN TABLE G2( g2e1 integer, g2e2 varchar, FOREIGN KEY (g2e1, g2e2) REFERENCES G1 (g1e1, g1e2))";
        assertScoreAndParse(content, null, 2);

        {
            final List<AstNode> kids = getRootNode().childrenWithName("G1");
            assertThat(kids.size(), is(1));
            assertMixinType(kids.get(0), TeiidDdlLexicon.CreateTable.TABLE_STATEMENT);
        }

        {
            final List<AstNode> kids = getRootNode().childrenWithName("G2");
            assertThat(kids.size(), is(1));
            assertMixinType(kids.get(0), TeiidDdlLexicon.CreateTable.TABLE_STATEMENT);
        }
    }

    /**
     * See Teiid TestDDLParser#testOptionalFK()
     */
    @Test
    public void shouldParseOptionalForeignKey() {
        final String content = "CREATE FOREIGN TABLE G1(g1e1 integer, g1e2 varchar, PRIMARY KEY(g1e1, g1e2));"
                               + "CREATE FOREIGN TABLE G2( g2e1 integer, g2e2 varchar, PRIMARY KEY(g2e1, g2e2), FOREIGN KEY (g2e1, g2e2) REFERENCES G1)";
        assertScoreAndParse(content, null, 2);

        {
            final List<AstNode> kids = getRootNode().childrenWithName("G1");
            assertThat(kids.size(), is(1));
            assertMixinType(kids.get(0), TeiidDdlLexicon.CreateTable.TABLE_STATEMENT);
        }

        {
            final List<AstNode> kids = getRootNode().childrenWithName("G2");
            assertThat(kids.size(), is(1));
            assertMixinType(kids.get(0), TeiidDdlLexicon.CreateTable.TABLE_STATEMENT);
        }
    }

    @Test
    public void shouldParseAccountsDdl() {
        final String content = "CREATE FOREIGN TABLE \"accounts.ACCOUNT\" ("
                               + "  ACCOUNT_ID long NOT NULL DEFAULT '0' OPTIONS (ANNOTATION '', NAMEINSOURCE '`ACCOUNT_ID`', NATIVE_TYPE 'INT'),"
                               + "  SSN string(10) OPTIONS (ANNOTATION '', NAMEINSOURCE '`SSN`', NATIVE_TYPE 'CHAR'),"
                               + "  STATUS string(10) OPTIONS (ANNOTATION '', NAMEINSOURCE '`STATUS`', NATIVE_TYPE 'CHAR'),"
                               + "  TYPE string(10) OPTIONS (ANNOTATION '', NAMEINSOURCE '`TYPE`', NATIVE_TYPE 'CHAR'),"
                               + "  DATEOPENED timestamp NOT NULL DEFAULT 'CURRENT_TIMESTAMP' OPTIONS (ANNOTATION '', NAMEINSOURCE '`DATEOPENED`', NATIVE_TYPE 'TIMESTAMP'),"
                               + "  DATECLOSED timestamp NOT NULL DEFAULT '0000-00-00 00:00:00' OPTIONS (ANNOTATION '', NAMEINSOURCE '`DATECLOSED`', NATIVE_TYPE 'TIMESTAMP')"
                               + "  )" + " OPTIONS (ANNOTATION '', NAMEINSOURCE '`accounts`.`ACCOUNT`', UPDATABLE TRUE);";
        assertScoreAndParse(content, null, 1);
        final AstNode tableNode = getRootNode().getChildren().get(0);
        assertMixinType(tableNode, TeiidDdlLexicon.CreateTable.TABLE_STATEMENT);
        assertProperty(tableNode, TeiidDdlLexicon.SchemaElement.TYPE, SchemaElementType.FOREIGN.toDdl());
    }

    @Test
    public void shouldParsePortfolioBR() {
        final String content = "CREATE VIRTUAL FUNCTION performRuleOnData(className string, returnMethodName string, returnIfNull string, VARIADIC z object )"
                               + "RETURNS string OPTIONS (JAVA_CLASS 'org.jboss.teiid.businessrules.udf.RulesUDF',  JAVA_METHOD 'performRuleOnData', VARARGS 'true');"
                               + "CREATE VIEW StockPrices ("
                               + "  symbol string,"
                               + "  price bigdecimal"
                               + "  )"
                               + "  AS "
                               + "    SELECT StockPrices.symbol, StockPrices.price"
                               + "      FROM (EXEC MarketData.getTextFiles('*.txt')) AS f,"
                               + "        TEXTTABLE(f.file COLUMNS symbol string, price bigdecimal HEADER) AS StockPrices;"
                               + "CREATE VIEW Stock ("
                               + "  symbol string,"
                               + "  price bigdecimal,"
                               + "  company_name   varchar(256)"
                               + "  )"
                               + "  AS"
                               + "    SELECT  S.symbol, S.price, A.COMPANY_NAME"
                               + "      FROM StockPrices AS S, Accounts.PRODUCT AS A"
                               + "      WHERE S.symbol = A.SYMBOL;"
                               + "CREATE VIRTUAL PROCEDURE StockValidation() RETURNS (companyname varchar(256), symbol string(10), price bigdecimal, message varchar(256) )"
                               + "  AS"
                               + "    BEGIN"
                               + "      DECLARE String VARIABLES.msg;"
                               + "      CREATE LOCAL TEMPORARY TABLE TEMP (companyname string, symbol string, price bigdecimal, message string);"
                               + "      LOOP ON (SELECT Stock.symbol, Stock.price, Stock.company_name FROM Stock) AS txncursor"
                               + "      BEGIN"
                               + "        VARIABLES.msg = Stocks.performRuleOnData('org.jboss.teiid.quickstart.data.MarketData', 'getInvalidMessage', 'noMsg', txncursor.company_name, txncursor.symbol, txncursor.price);"
                               + "        IF(VARIABLES.msg <> 'NoMsg')"
                               + "          BEGIN"
                               + "            INSERT INTO TEMP (TEMP.companyname, TEMP.symbol, TEMP.price, TEMP.message) VALUES (txncursor.COMPANY_NAME, txncursor.symbol, txncursor.price, VARIABLES.msg);"
                               + "          END"
                               + "      END"
                               + "      SELECT TEMP.companyname, TEMP.symbol, TEMP.price, TEMP.message FROM TEMP;"
                               + "    END"
                               + "";

        assertScoreAndParse(content, null, 4);

        {
            final List<AstNode> kids = getRootNode().childrenWithName("performRuleOnData");
            assertThat(kids.size(), is(1));
            assertMixinType(kids.get(0), TeiidDdlLexicon.CreateProcedure.FUNCTION_STATEMENT);
        }

        {
            final List<AstNode> kids = getRootNode().childrenWithName("StockPrices");
            assertThat(kids.size(), is(1));
            assertMixinType(kids.get(0), TeiidDdlLexicon.CreateTable.VIEW_STATEMENT);
        }

        {
            final List<AstNode> kids = getRootNode().childrenWithName("Stock");
            assertThat(kids.size(), is(1));
            assertMixinType(kids.get(0), TeiidDdlLexicon.CreateTable.VIEW_STATEMENT);
        }

        {
            final List<AstNode> kids = getRootNode().childrenWithName("StockValidation");
            assertThat(kids.size(), is(1));
            assertMixinType(kids.get(0), TeiidDdlLexicon.CreateProcedure.PROCEDURE_STATEMENT);
        }
    }
    
    @Test
	public void shouldParseAndResolveTableReference() {
		final String content = "CREATE FOREIGN TABLE G2(g2e1 integer, g2e2 varchar, PRIMARY KEY(g2e1, g2e2), FOREIGN KEY (g2e1, g2e2) REFERENCES G1)"
				+ "CREATE FOREIGN TABLE G1(g1e1 integer, g1e2 varchar, PRIMARY KEY(g1e1, g1e2));";

		assertScoreAndParse(content, null, 2);

		{
			final List<AstNode> kids = getRootNode().childrenWithName("G1");
			assertThat(kids.size(), is(1));
			assertMixinType(kids.get(0),
					TeiidDdlLexicon.CreateTable.TABLE_STATEMENT);
		}

		{
			final List<AstNode> kids = getRootNode().childrenWithName("G2");
			assertThat(kids.size(), is(1));
			assertMixinType(kids.get(0),
					TeiidDdlLexicon.CreateTable.TABLE_STATEMENT);
		}
	}
    
    @Test
    public void shouldParseTeiidStatements_1() {
    	// Parses a simplified DDL that contains 3 tables, 2 with Foreign keys.
    	// The Tables in the DDL are arranged, such that the first table has FK reference to 3rd table
    	// and results in an Unresolved table reference that should be handled by a postProcess() method
    	
        printTest("shouldParseTeiidStatements_1()");
        String content = getFileContent(DDL_FILE_PATH + "sap_short_test.ddl");
        assertScoreAndParse(content, "teiid_test_statements_1", 3);
        final AstNode tableNode = getRootNode().getChildren().get(0);
        if( tableNode != null) {
        	final List<AstNode> kids = getRootNode().childrenWithName("BookingCollection");
            assertThat(kids.size(), is(1));
            final List<AstNode> tableKids = kids.get(0).getChildren();
            assertThat(tableKids.size(), is(9));
            final List<AstNode> fkNodes = kids.get(0).childrenWithName("BookingFlight");
            assertThat(fkNodes.size(), is(1));
            
            final List<AstNode> fc_kids = getRootNode().childrenWithName("FlightCollection");
            assertThat(fc_kids.size(), is(1));
            final List<AstNode> fc_columns = fc_kids.get(0).childrenWithName("carrid");
            assertThat(fc_columns.size(), is(1));
            AstNode columnNode = fc_columns.get(0);
            
            @SuppressWarnings("unchecked")
			ArrayList<AstNode> props = ((ArrayList<AstNode>)fkNodes.get(0).getProperty(TeiidDdlLexicon.Constraint.TABLE_REFERENCE_REFERENCES));
            AstNode refColumnNode = props.get(0);
            assertThat(refColumnNode, is(columnNode));
        }
    }
    
    @Test
    public void shouldParseTeiidStatements_2() {
    	// Parses a full DDL file that contains multiple tables with multiple FK's
    	// The Tables in the DDL are arranged, such that the at least one table has FK reference to table defined later in the DDL
    	// and results in an Unresolved table reference that should be handled by a postProcess() method
    	
        printTest("shouldParseTeiidStatements_2()");
        String content = getFileContent(DDL_FILE_PATH + "sap-flight.ddl");
        assertScoreAndParse(content, "teiid_test_statements_2", 12);
        final AstNode tableNode = getRootNode().getChildren().get(0);
        
        if( tableNode != null) {
        	final List<AstNode> kids = getRootNode().childrenWithName("BookingCollection");
            assertThat(kids.size(), is(1));
            final List<AstNode> tableKids = kids.get(0).getChildren();
            assertThat(tableKids.size(), is(28));
            final List<AstNode> fkNodes = kids.get(0).childrenWithName("BookingFlight");
            assertThat(fkNodes.size(), is(1));
            
            final List<AstNode> fc_kids = getRootNode().childrenWithName("FlightCollection");
            assertThat(fc_kids.size(), is(1));
            final List<AstNode> fc_columns = fc_kids.get(0).childrenWithName("carrid");
            assertThat(fc_columns.size(), is(1));
            AstNode columnNode = fc_columns.get(0);
            
            @SuppressWarnings("unchecked")
			ArrayList<AstNode> props = ((ArrayList<AstNode>)fkNodes.get(0).getProperty(TeiidDdlLexicon.Constraint.TABLE_REFERENCE_REFERENCES));
            AstNode refColumnNode = null;
            for( AstNode nextColumnNode : props ) {
	            
	        	if( nextColumnNode.getName().equals("carrid")) {
	        		refColumnNode = nextColumnNode;
	        		break;
	        	}
        	}
            
            assertThat(refColumnNode, is(columnNode));
        }
    }

}
