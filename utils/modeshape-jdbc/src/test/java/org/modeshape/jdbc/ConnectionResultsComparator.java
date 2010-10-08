/*
 * JBoss, Home of Professional Open Source.
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 * 
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA
 * 02110-1301 USA.
 */

package org.modeshape.jdbc;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
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
    protected JcrStatement internalStatement = null;
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
                                    String[] expected,
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

        this.internalStatement = (JcrStatement)createStatement();
        this.internalStatement.setJcrSqlLanguage(jcrSQL);
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
