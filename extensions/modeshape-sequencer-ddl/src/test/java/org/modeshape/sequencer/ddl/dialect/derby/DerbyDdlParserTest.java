package org.modeshape.sequencer.ddl.dialect.derby;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.modeshape.sequencer.ddl.StandardDdlLexicon.TYPE_PROBLEM;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.modeshape.sequencer.ddl.DdlParserScorer;
import org.modeshape.sequencer.ddl.DdlParserTestHelper;
import org.modeshape.sequencer.ddl.node.AstNode;

public class DerbyDdlParserTest extends DdlParserTestHelper {
    private DerbyDdlParser parser;
    private AstNode rootNode;
    private DdlParserScorer scorer;

    public static final String DDL_FILE_PATH = "src/test/resources/ddl/dialect/derby/";

    @Before
    public void beforeEach() {
        parser = new DerbyDdlParser();
        setPrintToConsole(false);
        parser.setTestMode(isPrintToConsole());
        parser.setDoUseTerminator(true);
        rootNode = parser.nodeFactory().node("ddlRootNode");
        scorer = new DdlParserScorer();
    }

    @Test
    public void shouldParseCreateFunctionWithDataTypeReturn() {
        // setPrintToConsole(true);
        // parser.setTestMode(isPrintToConsole());
        printTest("shouldParseCreateFunctionWithDataTypeReturn()");
        String content = "CREATE FUNCTION TO_DEGREES" + NEWLINE + "( RADIANS DOUBLE )" + NEWLINE + "RETURNS DOUBLE" + NEWLINE
                         + "PARAMETER STYLE JAVA" + NEWLINE + "NO SQL LANGUAGE JAVA" + NEWLINE
                         + "EXTERNAL NAME 'java.lang.Math.toDegrees';";

        parser.parse(content, null, rootNode, scorer);
        assertThat(scorer.getScore() > 0, is(true));
    }

    @Test
    public void shouldParseCreateFunctionWithTableTypeReturn() {
        // setPrintToConsole(true);
        // parser.setTestMode(isPrintToConsole());
        printTest("shouldParseCreateFunctionWithTableTypeReturn()");
        String content = "CREATE FUNCTION PROPERTY_FILE_READER" + NEWLINE + "( FILENAME VARCHAR( 32672 ), FILESIZE INTEGER )"
                         + NEWLINE + "RETURNS TABLE (KEY_COL VARCHAR( 10 ), VALUE_COL VARCHAR( 1000 ))" + NEWLINE
                         + "LANGUAGE JAVA" + NEWLINE + "PARAMETER STYLE DERBY_JDBC_RESULT_SET" + NEWLINE + "NO SQL" + NEWLINE
                         + "EXTERNAL NAME 'vtis.example.PropertyFileVTI.propertyFileVTI';";

        parser.parse(content, null, rootNode, scorer);
        assertThat(scorer.getScore() > 0, is(true));
    }

    @Test
    public void shouldParseDropSchemaRestrict() {
        printTest("shouldParseDropSchemaRestrict()");
        String content = "DROP SCHEMA SAMP RESTRICT;";

        parser.parse(content, null, rootNode, scorer);
        assertThat(scorer.getScore() > 0, is(true));
        assertThat(rootNode.getChildCount(), is(1));
    }

    @Test
    public void shouldParseCreateIndex() {
        printTest("shouldParseCreateIndex()");
        String content = "CREATE INDEX PAY_DESC ON SAMP.EMPLOYEE (SALARY DESC, UNIT);";

        parser.parse(content, null, rootNode, scorer);
        assertThat(scorer.getScore() > 0, is(true));
        assertThat(rootNode.getChildCount(), is(1));
    }

    @Test
    public void shouldParseLockTable() {
        printTest("shouldParseLockTable()");
        String content = "LOCK TABLE FlightAvailability IN EXCLUSIVE MODE;";

        parser.parse(content, null, rootNode, scorer);
        assertThat(scorer.getScore() > 0, is(true));
        assertThat(rootNode.getChildCount(), is(1));
    }

    @Test
    public void shouldParseDeclareGlobaTemporaryTable() {
        printTest("shouldParseDeclareGlobaTemporaryTable()");
        String content = "declare global temporary table SESSION.t1(c11 int) not logged;";

        parser.parse(content, null, rootNode, scorer);
        assertThat(scorer.getScore() > 0, is(true));
    }

    @Test
    public void shouldParseRenameTable() {
        printTest("shouldParseRenameTable()");
        String content = "RENAME TABLE SAMP.EMP_ACT TO EMPLOYEE_ACT;";

        parser.parse(content, null, rootNode, scorer);
        assertThat(scorer.getScore() > 0, is(true));
        assertThat(rootNode.getChildCount(), is(1));
    }

    @Test
    public void shouldParseCreateSynonym() {
        printTest("shouldParseCreateSynonym()");
        String content = "CREATE SYNONYM SAMP.T1 FOR SAMP.TABLEWITHLONGNAME;";

        parser.parse(content, null, rootNode, scorer);
        assertThat(scorer.getScore() > 0, is(true));
        assertThat(rootNode.getChildCount(), is(1));
    }

    @Test
    public void shouldParseCreateTrigger() {
        printTest("shouldParseCreateTrigger()");
        String content = "CREATE TRIGGER FLIGHTSDELETE3" + NEWLINE + "AFTER DELETE ON FLIGHTS" + NEWLINE
                         + "REFERENCING OLD AS OLD" + NEWLINE + "FOR EACH ROW" + NEWLINE
                         + "DELETE FROM FLIGHTAVAILABILITY WHERE FLIGHT_ID = OLD.FLIGHT_ID;";

        parser.parse(content, null, rootNode, scorer);
        assertThat(scorer.getScore() > 0, is(true));
        assertThat(rootNode.getChildCount(), is(1));
    }

    @Test
    public void shouldParseCreateTrigger_2() {
        printTest("shouldParseCreateTrigger_2()");
        String content = "CREATE TRIGGER t1 NO CASCADE BEFORE UPDATE ON x" + NEWLINE + "FOR EACH ROW MODE DB2SQL" + NEWLINE
                         + "values app.notifyEmail('Jerry', 'Table x is about to be updated');";

        parser.parse(content, null, rootNode, scorer);
        assertThat(scorer.getScore() > 0, is(true));
        assertThat(rootNode.getChildCount(), is(1));
    }

    @Test
    public void shouldParseGrantStatements() {
        printTest("shouldParseGrantStatements()");
        String content = "GRANT SELECT ON TABLE purchaseOrders TO maria,harry;" + NEWLINE
                         + "GRANT UPDATE, TRIGGER ON TABLE purchaseOrders TO anita,zhi;" + NEWLINE
                         + "GRANT SELECT ON TABLE orders.bills to PUBLIC;" + NEWLINE
                         + "GRANT EXECUTE ON PROCEDURE updatePurchases TO george;" + NEWLINE
                         + "GRANT purchases_reader_role TO george,maria;" + NEWLINE
                         + "GRANT SELECT ON TABLE purchaseOrders TO purchases_reader_role;";

        parser.parse(content, null, rootNode, scorer);
        assertThat(scorer.getScore() > 0, is(true));
        assertThat(rootNode.getChildCount(), is(6));
    }

    @Test
    public void shouldParseAlterTableAlterColumnDefaultRealNumber() {
        printTest("shouldParseAlterTableAlterColumnDefaultRealNumber()");
        String content = "ALTER TABLE Employees ALTER COLUMN Salary DEFAULT 1000.0;";

        parser.parse(content, null, rootNode, scorer);
        assertThat(scorer.getScore() > 0, is(true));
    }

    // DROP PROCEDURE some_procedure_name;
    @Test
    public void shouldParseDropProcedure() {
        printTest("shouldParseDropProcedure()");
        String content = "DROP PROCEDURE some_procedure_name";

        parser.parse(content, null, rootNode, scorer);
        assertThat(scorer.getScore() > 0, is(true));
    }

    @Test
    public void shouldParseDerbyStatements() {
        // setPrintToConsole(true);
        // parser.setTestMode(isPrintToConsole());
        printTest("shouldParseDerbyStatements()");
        String content = getFileContent(DDL_FILE_PATH + "derby_test_statements.ddl");

        parser.parse(content, null, rootNode, scorer);
        assertThat(scorer.getScore() > 0, is(true));

        List<AstNode> problems = parser.nodeFactory().getChildrenForType(rootNode, TYPE_PROBLEM);
        int nStatements = rootNode.getChildCount() - problems.size();
        assertEquals(64, nStatements);
    }
}
