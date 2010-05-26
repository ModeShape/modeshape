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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.modeshape.jdbc.util.ResultSetReader;
import org.modeshape.jdbc.util.StringLineReader;



/** 
 * This class can be used as the base class to write Query tests for 
 * integration testing. Just like the scripted one this one should provide all
 * those required flexibility in testing.
 */
class DriverTestUtil {
	
    protected Connection internalConnection = null;
    protected JcrResultSet internalResultSet = null;
    protected Statement internalStatement = null;
    private SQLException internalException = null;
    protected int updateCount = -1;
    protected String DELIMITER = "    "; //$NON-NLS-1$ 
    private ResultSetReader reader = null;
       
    
    private DriverTestUtil(Connection conn) {
        this.internalConnection = conn;     
    }
        
    public static void executeTest(Connection conn, String sql, String expected) throws SQLException{
	DriverTestUtil util = new DriverTestUtil(conn);
	try {
    		util.execute(sql);
    		util.assertResultsSetEquals(expected);
    	
	} finally {
	    util.closeResultSet();
	    util.closeStatement();
	}
    }
	
    public static  void executeTest(Connection conn, String sql, String[] expected) throws SQLException{
	DriverTestUtil util = new DriverTestUtil(conn);
	try {
	    util.execute(sql);
	    util.assertResultsSetEquals(expected);   		
    	
	} finally {
	    util.closeResultSet();
	    util.closeStatement();
	}
    }	
    
    public static void executeTestAndPrint(Connection conn, String sql) throws SQLException{
	DriverTestUtil util = new DriverTestUtil(conn);
	try {
	    util.execute(sql);
	    util.printResults();
	} finally {
	    util.closeResultSet();
	    util.closeStatement();
	}
    }	
          
    private boolean execute(String sql) throws SQLException{
        return execute(sql, new Object[] {});
    }
    
    private boolean execute(String sql, Object[] params) throws SQLException{
	this.updateCount = -1;

	    assertNotNull(this.internalConnection);
	    assertTrue(!this.internalConnection.isClosed());
	    boolean result = false;

	    this.internalStatement = createStatement();
	    result = this.internalStatement.execute(sql);
	    assertTrue(result);
	    
	    this.internalResultSet = (JcrResultSet) this.internalStatement.getResultSet();
	    
	    assertThat(this.internalResultSet, is(notNullValue()));
	    assertThat(this.internalResultSet.isBeforeFirst(), is(true));
	    assertThat(this.internalResultSet.isClosed(), is(false));
	    	    
	    return result;

    }   
   
    protected Statement createStatement() throws SQLException{
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

    private void assertResultsSetEquals(String expected) {
    	assertResultsSetEquals(this.internalResultSet, expected);
    }
    
    private void assertResultsSetEquals(final ResultSet resultSet,final String expected) {
        assertNotNull(resultSet);
        reader = new ResultSetReader(resultSet, DELIMITER);
        assertReaderEquals(reader, new StringReader(expected));
    }

    private void assertResultsSetEquals(String[] expected) {
    	assertResultsSetEquals(this.internalResultSet, expected);
    }
    
    private void assertResultsSetEquals(ResultSet resultSet, String[] expected) {
        assertNotNull(resultSet);
        reader = new ResultSetReader(resultSet, DELIMITER);
        assertReaderEquals(reader, new StringArrayReader(expected));
    }
    
    private void assertReaderEquals(Reader expected, Reader reader) {
        BufferedReader  resultReader = new BufferedReader(expected);
        BufferedReader  expectedReader = new BufferedReader(reader);        
        try {
            compareResults(resultReader, expectedReader);
        } catch (Exception e) {
        	throw new RuntimeException(e);
        } finally {
            try {
                resultReader.close();
                expectedReader.close();
            } catch (IOException e) {
            	throw new RuntimeException(e);
            }             
        }
    }

   protected static String read(BufferedReader r, boolean casesensitive) throws IOException {
    	StringBuffer result = new StringBuffer();
    	String s = null;
    	try {
	    	while ((s = r.readLine()) != null) {
	    		result.append(  (casesensitive ? s.trim() : s.trim().toLowerCase()) );
	    		result.append("\n"); //$NON-NLS-1$
	    	}
    	} finally {
    		r.close();
    	}
    	return result.toString();
    }

    protected void compareResults(BufferedReader resultReader, BufferedReader expectedReader) throws IOException {
    	assertEquals(read(expectedReader, compareCaseSensitive()) , read(resultReader, compareCaseSensitive()));
    }
    
    protected boolean compareCaseSensitive() {
	return true;
    }
    
//    public void printResults() {
//        printResults(this.internalResultSet);
//    }

//    public void printResults(ResultSet results) {
//        printResults(results);
//    }

    public void printResults() {
        assertNotNull(this.internalResultSet);
        printResults(this.internalResultSet);
    }
   
    void printResults(ResultSet results) {
        if(results == null) {
            System.out.println("ResultSet is null"); //$NON-NLS-1$
            return;
        }        
        int row;
        try {
            row = -1;
            BufferedReader in = new BufferedReader(new ResultSetReader(results, DELIMITER));
            String line = in.readLine();
            String nextline = null;
            System.out.println("String[] expected = {");
            while(line != null) {
                row++;

                nextline = in.readLine();
                if (nextline != null) {
                    System.out.println("\"" + line + "\",");
                    line = nextline;
                } else {
                    System.out.println("\"" + line + "\"");
                    line = null;
                }
                               
            }
            System.out.println("};");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }        
    }
    
    public void assertUpdateCount(int expected) {
    	assertEquals(expected, updateCount);
    }

    public void assertRowCount(int expected) {
	if (reader != null) {
	    assertEquals(expected, this.reader.getRowCount());
	}
    }

    public int getRowCount() {
	
        assertNotNull(this.internalResultSet);
        // Count all
        try {
            int count = 0;
            while(this.internalResultSet.next()) {
                count++;
            }
            return count;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }    
    
    public void closeStatement() {
        closeResultSet();

        if (this.internalStatement != null){
            try {
                this.internalStatement.close();
            } catch(SQLException e) {
            	throw new RuntimeException(e);
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
    
    public void print(String msg) {
        System.out.println(msg);
    }
    
    public void print(Throwable e) {
        e.printStackTrace();
    }

   
}

/** 
 * Converts a String Array object into a Reader object.
 */
class StringArrayReader extends StringLineReader {
    String[] source = null;
    int index = 0;
    
    public StringArrayReader(String[] src) {
        this.source = src;
    }

    @SuppressWarnings("unused")
    @Override
    protected String nextLine() throws IOException {
        if (index < this.source.length) {
            return this.source[index++]+"\n"; //$NON-NLS-1$
        }
        return null;
    }
}