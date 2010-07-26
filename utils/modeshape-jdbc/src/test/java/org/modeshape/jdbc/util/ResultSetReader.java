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

package org.modeshape.jdbc.util;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.SQLXML;
import java.sql.Types;

import javax.jcr.PropertyType;
import javax.jcr.Value;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.hamcrest.core.IsInstanceOf;
import org.modeshape.jdbc.JcrResultSet;
import org.modeshape.jdbc.JcrType;


/** 
 * This object wraps/extends a SQL ResultSet object as Reader object. Once the
 * ResultSet can read as reader then it can be persisted, printed or compared 
 * easily without lot of code hassle of walking it every time.
 * 
 * <p>PS: remember this is a Reader not InputStream, so all the fields read
 * going to be converted to strings before they returned.
 * 
 */
public class ResultSetReader extends StringLineReader {
	private static final String DEFAULT_DELIM =  "    "; //$NON-NLS-1$
	
    JcrResultSet source = null;
    ResultSetMetaData metadata = null;
    
    // Number of columns in the result set
    int columnCount = 0;

    // delimiter between the fields while reading each row 
    String delimiter = null;
 
    boolean firstTime = true;
    int[] columnTypes = null;
    
    private int rowCount=0;
    
    private boolean compareColumns = true;
    
    
    public ResultSetReader(JcrResultSet in, boolean compareCols) {
    	this(in, DEFAULT_DELIM, compareCols);       
    }
    
    public ResultSetReader(ResultSet in, String delimiter, boolean compareCols) {
        this.source = (JcrResultSet)in;        
        this.delimiter = delimiter;
        this.compareColumns = compareCols;
    }
    
    /** 
     * @see java.io.Reader#close()
     */
    @Override
    public void close() throws IOException {
        source.close();
	super.close();
    }

    /**
     * Get the next line of results from the ResultSet. The first line will be the 
     * metadata of the resultset and then followed by the result rows. Each row will be 
     * returned as one line.  
     * @return next result line from result set.
     * @throws IOException 
     */
    @Override
    protected String nextLine() throws IOException, SQLException{        
 //       try {
            if (firstTime) {
                firstTime = false;
                this.metadata = source.getMetaData();
                columnCount = metadata.getColumnCount();
                columnTypes  = new int[columnCount];
                for (int i = 0; i < columnCount; i++) {
                    columnTypes[i] = metadata.getColumnType(i+1);
                }
                return resultSetMetaDataToString(metadata, delimiter);
            }
            
            // if you get here then we are ready to read the results.
            if (source.next()) {
            	rowCount++;
            	
                final StringBuffer sb = new StringBuffer();
                // Walk through column values in this row
                for (int col = 1; col <= columnCount; col++) {
                	// this does not work when database metadata is being queried
                	if (compareColumns) compareColumn(col);
                     
                    final Object anObj = source.getObject(col);
                    if (columnTypes[col-1] == Types.CLOB) {
                        sb.append(anObj != null ? anObj : "null"); //$NON-NLS-1$
                    }
                    else if (columnTypes[col-1] == Types.BLOB) {
                        sb.append(anObj != null ? "BLOB" : "null"); //$NON-NLS-1$ //$NON-NLS-2$
                    }
                    else if (columnTypes[col-1] == Types.SQLXML) {
                    	final SQLXML xml = (SQLXML)anObj;
                    	sb.append(anObj != null ? prettyPrint(xml) : "null"); //$NON-NLS-1$
                    }
                    else {
                        sb.append(anObj != null ? anObj : "null"); //$NON-NLS-1$
                    }
                    if (col != columnCount) {
                        sb.append(delimiter); 
                    }                    
                }
                sb.append("\n"); //$NON-NLS-1$
                return sb.toString();
            }
       
        return null;
    }
    
    private void compareColumn(int col) throws SQLException {

	    Object objIdx = source.getObject(col);

	    String colName = metadata.getColumnName(col);
	    Object objName = source.getObject(colName);
	    
	    assertThat(objIdx, is(objName));
	    
	    if (objIdx == null) return;
	    	       
	    Value v = source.getValue(col);
	    
	    JcrType jcrType = JcrType.builtInTypeMap().get(PropertyType.nameFromValue(v.getType()));
	    
	    assertThat(objIdx, IsInstanceOf.instanceOf(jcrType.getRepresentationClass()));
		  
    }
    
    public int getRowCount() {
		return rowCount;
	}
    
    /**
     * Get the first line from the result set. This is the resultset metadata line where
     * we gather the column names and their types. 
     * @param metadata 
     * @param delimiter 
     * @return String
     * @throws SQLException
     */
    public static String resultSetMetaDataToString(ResultSetMetaData metadata, String delimiter) throws SQLException{
        StringBuffer sb = new StringBuffer();
        int columnCount = metadata.getColumnCount();
        for (int col = 1; col <= columnCount; col++) {
            sb.append(metadata.getColumnName(col))
                .append("[")          //$NON-NLS-1$
                .append(metadata.getColumnTypeName(col))
                .append("]");       //$NON-NLS-1$
            if (col != columnCount) {
                sb.append(delimiter);
            }
        }
        sb.append("\n"); //$NON-NLS-1$
        return sb.toString();        
    }
    
	public static String prettyPrint(SQLXML xml) throws SQLException {
		try {
			TransformerFactory transFactory = TransformerFactory.newInstance();
			transFactory.setAttribute("indent-number", new Integer(2)); //$NON-NLS-1$
			
			Transformer tf = transFactory.newTransformer();
			tf.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes"); //$NON-NLS-1$
			tf.setOutputProperty(OutputKeys.ENCODING, "UTF-8");//$NON-NLS-1$
			tf.setOutputProperty(OutputKeys.INDENT, "yes");//$NON-NLS-1$
			tf.setOutputProperty(OutputKeys.METHOD, "xml");//$NON-NLS-1$
			tf.setOutputProperty(OutputKeys.STANDALONE, "yes");//$NON-NLS-1$
			tf.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "4"); //$NON-NLS-1$ //$NON-NLS-2$
			
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			StreamResult xmlOut = new StreamResult(new BufferedOutputStream(out));
			tf.transform(xml.getSource(StreamSource.class), xmlOut);
			
			return out.toString();
		} catch (Exception e) {
			return xml.getString();
		}
	}   
}
