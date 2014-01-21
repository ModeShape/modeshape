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
package org.modeshape.jdbc;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.modeshape.jcr.JcrRepository.QueryLanguage;
import org.modeshape.jdbc.util.ResultsComparator;

/**
 * This class can be used to execute a query and compare the results to what's expected.
 */
public class ConnectionResultsComparator extends ResultsComparator {

    protected Connection internalConnection = null;
    protected ResultSet internalResultSet = null;
    protected Statement internalStatement = null;
    private SQLException internalException = null;
    protected int updateCount = -1;

    protected ConnectionResultsComparator() {

    }

    private ConnectionResultsComparator( Connection conn ) {
        this.internalConnection = conn;
    }

    public static void executeTest( Connection conn,
                                    String sql,
                                    String expected,
                                    int expectedRowCount,
                                    String jcrSQL ) throws SQLException {
        ConnectionResultsComparator util = new ConnectionResultsComparator(conn);
        try {
            util.execute(sql, jcrSQL);
            util.assertResultsSetEquals(util.internalResultSet, expected);

            util.assertRowCount(expectedRowCount);

        } finally {
            util.closeResultSet();
            util.closeStatement();
        }
    }

    public static void executeTest( Connection conn,
                                    String sql,
                                    int expectedRowCount,
                                    String jcrSQL ) throws SQLException {
        ConnectionResultsComparator util = new ConnectionResultsComparator(conn);
        try {
            util.execute(sql, jcrSQL);
            util.assertRowCount(expectedRowCount);
        } finally {
            util.closeResultSet();
            util.closeStatement();
        }
    }

    public static void executeTest( Connection conn,
                                    String sql,
                                    String[] expected,
                                    int expectedRowCount,
                                    String jcrSQL ) throws SQLException {
        AssertionError lastError = null;
        for (int i = 0; i != 10; ++i) {
            ConnectionResultsComparator util = new ConnectionResultsComparator(conn);
            try {
                util.execute(sql, jcrSQL);
                util.assertResultsSetEquals(util.internalResultSet, expected);

                util.assertRowCount(expectedRowCount);
                return;
            } catch (AssertionError e) {
                lastError = e;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException ie) {
                    fail("Interrupted");
                    return;
                }
            } finally {
                util.closeResultSet();
                util.closeStatement();
            }
        }
        if (lastError != null) throw lastError;
    }

    public static void executeTest( Connection conn,
                                    String sql,
                                    String expected,
                                    int expectedRowCount ) throws SQLException {
        executeTest(conn, sql, expected, expectedRowCount, QueryLanguage.JCR_SQL2);

    }

    public static void executeTest( Connection conn,
                                    String sql,
                                    String[] expected,
                                    int expectedRowCount ) throws SQLException {
        executeTest(conn, sql, expected, expectedRowCount, QueryLanguage.JCR_SQL2);

    }

    public static void executeTestAndPrint( Connection conn,
                                            String sql,
                                            String jcrSQL ) throws SQLException {
        ConnectionResultsComparator util = new ConnectionResultsComparator(conn);
        try {
            util.execute(sql, jcrSQL);
            util.printResults();
        } finally {
            util.closeResultSet();
            util.closeStatement();
        }
    }

    public static void executeTestAndPrint( Connection conn,
                                            String sql ) throws SQLException {
        executeTestAndPrint(conn, sql, QueryLanguage.JCR_SQL2);
    }

    private boolean execute( String sql,
                             String jcrSQL ) throws SQLException {
        return execute(sql, new Object[] {}, jcrSQL);
    }

    private boolean execute( String sql,
                             Object[] params,
                             String jcrSQL ) throws SQLException {
        this.updateCount = -1;

        assertNotNull(this.internalConnection);
        assertTrue(!this.internalConnection.isClosed());
        boolean result = false;

        this.internalStatement = createStatement();
        if (internalStatement instanceof JcrStatement) {
            ((JcrStatement)this.internalStatement).setJcrSqlLanguage(jcrSQL);
        }
        result = this.internalStatement.execute(sql);
        assertTrue(result);

        this.internalResultSet = this.internalStatement.getResultSet();

        assertThat(this.internalResultSet, is(notNullValue()));
        assertThat(this.internalResultSet.isBeforeFirst(), is(true));
        assertThat(this.internalResultSet.isClosed(), is(false));

        return result;

    }

    protected Statement createStatement() throws SQLException {
        return this.internalConnection.createStatement();
    }

    public boolean exceptionOccurred() {
        return this.internalException != null;
    }

    public boolean exceptionExpected() {
        return false;
    }

    public SQLException getLastException() {
        return this.internalException;
    }

    public void printResults() {
        assertNotNull(this.internalResultSet);
        printResults(this.internalResultSet, this.compareColumns);
    }

    public void assertUpdateCount( int expected ) {
        assertEquals(expected, updateCount);
    }

    public int getRowCount() {
        assertNotNull(this.internalResultSet);
        return super.getRowCount(this.internalResultSet);

    }

    public void closeStatement() {
        closeResultSet();

        if (this.internalStatement != null) {
            try {
                this.internalStatement.close();
            } catch (SQLException e) {

            } finally {
                this.internalStatement = null;
            }
        }
    }

    public void closeResultSet() {
        this.internalException = null;

        if (this.internalResultSet != null) {
            try {

                this.internalResultSet.close();
            } catch (SQLException e) {

            } finally {
                this.internalResultSet = null;
            }
        }
    }

    public void cancelQuery() throws SQLException {
        assertNotNull(this.internalConnection);
        assertTrue(!this.internalConnection.isClosed());
        assertNotNull(this.internalStatement);
        this.internalStatement.cancel();
    }

}
