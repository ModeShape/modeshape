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
package org.modeshape.jdbc;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.URL;
import java.sql.Array;
import java.sql.Blob;
import java.sql.Clob;
import java.sql.Date;
import java.sql.NClob;
import java.sql.Ref;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.RowId;
import java.sql.SQLException;
import java.sql.SQLFeatureNotSupportedException;
import java.sql.SQLWarning;
import java.sql.SQLXML;
import java.sql.Statement;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import org.modeshape.jdbc.util.IoUtil;
import org.modeshape.jdbc.util.TimestampWithTimezone;

/**
 * 
 */
public class JcrResultSet implements ResultSet {

    private boolean closed;
    private JcrStatement statement;
    private QueryResult jcrResults;
    private ResultSetMetaData metadata;
    private RowIterator rowIter;
    private Row row;    
    
    // the object which was last read from Results
    private Object currentValue = null;

    private Map<String, Integer> columnIndexesByName;

    private String[] columnIDs = null;
    
    /** default Calendar instance for converting date/time/timestamp values */
    private Calendar defaultCalendar;


    protected JcrResultSet( JcrStatement statement,
                            QueryResult jcrResults,
                            ResultSetMetaData resultSetMetaData ) throws SQLException {
        this.statement = statement;
        this.jcrResults = jcrResults;
        assert this.statement != null;
        assert this.jcrResults != null;

        if (resultSetMetaData != null) {
            this.metadata = resultSetMetaData;
        } else {
            this.metadata = new JcrResultSetMetaData(this.statement.connection(), this.jcrResults);
        }
        int index = 1; // not zero-based
        int colCnt = this.metadata.getColumnCount();

        // add 1 because using 1 based location, not zero based, JDBC spec
        columnIDs = new String[colCnt + 1];
        columnIndexesByName = new HashMap<String, Integer>(colCnt);
        while (index <= colCnt) {
            String name = this.metadata.getColumnName(index);
            columnIndexesByName.put(name, index);
            columnIDs[index] = name;
            index++;
        }

        assert !columnIndexesByName.isEmpty();
        this.columnIndexesByName = Collections.unmodifiableMap(columnIndexesByName);

        try {
            this.rowIter = this.jcrResults.getRows();
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage(), e);
        }
    }  

    /**
     * no-arg CTOR is used to create an empty result set
     * 
     * @see JcrStatement#getGeneratedKeys()
     */
    protected JcrResultSet() {
        closed = true;
        columnIndexesByName = Collections.emptyMap();
    }
    
    
    Calendar getDefaultCalendar() {
        if (defaultCalendar == null) {
            defaultCalendar = TimestampWithTimezone.getCalendar(); 
        }
        return defaultCalendar;
    }
     
    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#isClosed()
     */
    @Override
    public boolean isClosed() {
        return closed || statement.isClosed();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#close()
     */
    @Override
    public void close() {
        if (!closed) {
            closed = true;
        	this.statement.close();
        }
    }

    byte[] convertToByteArray( final Value value ) throws SQLException {
        if (value == null) return null;
        InputStream is = null;
        boolean error = false;
        try {
            switch (value.getType()) {
                case PropertyType.STRING:
                case PropertyType.BOOLEAN:
                case PropertyType.DOUBLE:

                    String v = value.getString();
                    return (v != null ? v.getBytes() : null);

                case PropertyType.BINARY:
                    is = value.getBinary().getStream();
                    return IoUtil.readBytes(is);
                default:
                    return null;
            }

        } catch (IOException ioe) {
            error = true;
            throw new SQLException(ioe.getLocalizedMessage(), ioe);
        } catch (IllegalStateException ie) {
            error = true;
            throw new SQLException(ie.getLocalizedMessage(), ie);
        } catch (RepositoryException e) {
            error = true;
            throw new SQLException(e.getLocalizedMessage(), e);
        } finally {
            try {
                if (is != null) is.close();
            } catch (Exception e) {
                if (!error) throw new SQLException(e.getLocalizedMessage(), e);
            }
        }
    }

    protected final void notClosed() throws SQLException {
        if (isClosed()) throw new SQLException(JdbcI18n.resultSetIsClosed.text());
    }

    protected final void noUpdates() throws SQLException {
        throw new SQLFeatureNotSupportedException(JdbcI18n.updatesNotSupported.text());
    }

    protected final void forwardOnly() throws SQLException {
        throw new SQLException(JdbcI18n.resultSetIsForwardOnly.text());
    }

    protected final void itemNotFoundUsingColunName( String columnName ) throws SQLException {
        throw new SQLException(JdbcI18n.noSuchColumn.text(columnName));
    }

    protected final void itemNotFoundUsingColunIndex( int idx ) throws SQLException {
        throw new SQLException(JdbcI18n.noSuchColumn.text(String.valueOf(idx)));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getMetaData()
     */
    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        notClosed();
        return metadata;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getStatement()
     */
    @Override
    public Statement getStatement() throws SQLException {
        notClosed();
        return statement;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This driver only supports {@link ResultSet#TYPE_FORWARD_ONLY}.
     * </p>
     * 
     * @see java.sql.ResultSet#getType()
     */
    @Override
    public int getType() throws SQLException {
        notClosed();
        return ResultSet.TYPE_FORWARD_ONLY;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This driver only supports {@link ResultSet#FETCH_FORWARD}.
     * </p>
     * 
     * @see java.sql.ResultSet#getFetchDirection()
     */
    @Override
    public int getFetchDirection() throws SQLException {
        notClosed();
        return ResultSet.FETCH_FORWARD;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, will have no effect on the fetch direction because this driver only
     * supports {@link ResultSet#FETCH_FORWARD}.
     * </p>
     * 
     * @see java.sql.ResultSet#setFetchDirection(int)
     */
    @Override
    public void setFetchDirection( int direction ) throws SQLException {
    	throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLException} because this driver only supports
     * {@link ResultSet#FETCH_FORWARD}.
     * </p>
     * 
     * @see java.sql.ResultSet#absolute(int)
     */
    @Override
    public boolean absolute( int row ) throws SQLException {
        notClosed();
        forwardOnly();

        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLException} because this driver only supports
     * {@link ResultSet#FETCH_FORWARD}.
     * </p>
     * 
     * @see java.sql.ResultSet#afterLast()
     */
    @Override
    public void afterLast() throws SQLException {
        notClosed();
        forwardOnly();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLException} because this driver only supports
     * {@link ResultSet#FETCH_FORWARD}.
     * </p>
     * 
     * @see java.sql.ResultSet#beforeFirst()
     */
    @Override
    public void beforeFirst() throws SQLException {
        notClosed();
        forwardOnly();

    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#cancelRowUpdates()
     */
    @Override
    public void cancelRowUpdates() throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#clearWarnings()
     */
    @Override
    public void clearWarnings() throws SQLException {
        notClosed();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#deleteRow()
     */
    @Override
    public void deleteRow() throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#findColumn(java.lang.String)
     */
    @Override
    public int findColumn( String columnLabel ) throws SQLException {
        notClosed();
        final Integer result = columnLabel != null ? columnIndexesByName.get(columnLabel) : null;

        if (result == null) {
            this.itemNotFoundUsingColunName(columnLabel);
        }
        assert result != null;
        return result.intValue();
    }

    private String findColumn( int columnIndex ) throws SQLException {
        if (columnIndex > 0 && columnIndex < this.columnIDs.length) {
            return columnIDs[columnIndex];
        }

        throw new SQLException(JdbcI18n.invalidColumnIndex.text(new Object[] {columnIndex, this.columnIDs.length}));
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLException} because this driver only supports
     * {@link ResultSet#FETCH_FORWARD}.
     * </p>
     * 
     * @see java.sql.ResultSet#first()
     */
    @Override
    public boolean first() throws SQLException {
        notClosed();
        forwardOnly();

        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getRow()
     */
    @Override
    public int getRow() throws SQLException {
        notClosed();
        return (int)this.rowIter.getPosition();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method will always throw {@link SQLFeatureNotSupportedException}
     * </p>
     * 
     * @see java.sql.ResultSet#getRowId(int)
     */
    @Override
    public RowId getRowId( final int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getRowId(java.lang.String)
     */
    @Override
    public RowId getRowId( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getCursorName()
     */
    @Override
    public String getCursorName() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getFetchSize()
     */
    @Override
    public int getFetchSize() throws SQLException {
        notClosed();
        return statement.getFetchSize();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getArray(int)
     */
    @Override
    public Array getArray( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getArray(java.lang.String)
     */
    @Override
    public Array getArray( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getAsciiStream(int)
     */
    @Override
    public InputStream getAsciiStream( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getAsciiStream(java.lang.String)
     */
    @Override
    public InputStream getAsciiStream( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getBigDecimal(int)
     */
    @Override
    public BigDecimal getBigDecimal( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getBigDecimal(java.lang.String)
     */
    @Override
    public BigDecimal getBigDecimal( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getBigDecimal(int, int)
     */
    @Override
    public BigDecimal getBigDecimal( int columnIndex,
                                     int scale ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getBigDecimal(java.lang.String, int)
     */
    @Override
    public BigDecimal getBigDecimal( String columnLabel,
                                     int scale ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getBinaryStream(int)
     */
    @Override
    public InputStream getBinaryStream( int columnIndex ) throws SQLException {
        return getBinaryStream(findColumn(columnIndex));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getBinaryStream(java.lang.String)
     */
    @Override
    public InputStream getBinaryStream( String columnLabel ) throws SQLException {
    	Object o = getValueReturn(columnLabel, PropertyType.BINARY);
    	if (o != null) {
    		return (InputStream) o;
    	}
    	return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getBlob(int)
     */
    @Override
    public Blob getBlob( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getBlob(java.lang.String)
     */
    @Override
    public Blob getBlob( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getBoolean(int)
     */
    @Override
    public boolean getBoolean( int columnIndex ) throws SQLException {
        return getBoolean(findColumn(columnIndex));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getBoolean(java.lang.String)
     */
    @Override
    public boolean getBoolean( String columnLabel ) throws SQLException {
    	
    	Object o = getValueReturn(columnLabel, PropertyType.BOOLEAN);
    	if (o != null) {
    		return ((Boolean) o).booleanValue();
    	}
    	return Boolean.FALSE.booleanValue();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getByte(int)
     */
    @Override
    public byte getByte( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getByte(java.lang.String)
     */
    @Override
    public byte getByte( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getBytes(int)
     */
    @Override
    public byte[] getBytes( int columnIndex ) throws SQLException {
        return getBytes(findColumn(columnIndex));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getBytes(java.lang.String)
     */
    @Override
    public byte[] getBytes( String columnLabel ) throws SQLException {
        notClosed();
        isRowSet();

        this.currentValue = null;
        try {
            Value value = row.getValue(columnLabel);
            byte[] rtnbytes = convertToByteArray(value);
            this.currentValue = rtnbytes;
            return rtnbytes;
        } catch (PathNotFoundException pnfe) {
            // do nothing, return null
        } catch (ItemNotFoundException e) {
            itemNotFoundUsingColunName(columnLabel);
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage(), e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getCharacterStream(int)
     */
    @Override
    public Reader getCharacterStream( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getCharacterStream(java.lang.String)
     */
    @Override
    public Reader getCharacterStream( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getClob(int)
     */
    @Override
    public Clob getClob( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getClob(java.lang.String)
     */
    @Override
    public Clob getClob( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getConcurrency()
     */
    @Override
    public int getConcurrency() throws SQLException {
        notClosed();
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getDate(int)
     */
    @Override
    public Date getDate( int columnIndex ) throws SQLException {
        return getDate(findColumn(columnIndex));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getDate(java.lang.String)
     */
    @Override
    public Date getDate( String columnLabel ) throws SQLException {
    	Calendar calv = (Calendar)getValueReturn(columnLabel, PropertyType.DATE); 
    	if (calv == null) return null;
    	
    	return TimestampWithTimezone.createDate(calv);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getDate(int, java.util.Calendar)
     */
    @Override
    public Date getDate( int columnIndex,
                         Calendar cal ) throws SQLException {
    	 return getDate(findColumn(columnIndex), cal);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getDate(java.lang.String, java.util.Calendar)
     */
    @Override
    public Date getDate( String columnLabel,
                         Calendar cal ) throws SQLException {
    	
        Calendar actual = (Calendar)getValueReturn(columnLabel, PropertyType.DATE);

        if (actual == null) return null; 
              
    	return TimestampWithTimezone.createDate(actual, cal);
        
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getDouble(int)
     */
    @Override
    public double getDouble( int columnIndex ) throws SQLException {
        return getDouble(findColumn(columnIndex));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getDouble(java.lang.String)
     */
    @Override
    public double getDouble( String columnLabel ) throws SQLException {
    	Object o = getValueReturn(columnLabel, PropertyType.DOUBLE);
    	if (o != null) {
    		return ((Double) o).doubleValue();
    	}    	
    	return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getFloat(int)
     */
    @Override
    public float getFloat( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getFloat(java.lang.String)
     */
    @Override
    public float getFloat( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * <p>
     * According to 1.6 javadocs, holdability should be set to either {@link ResultSet#CLOSE_CURSORS_AT_COMMIT} or
     * {@link ResultSet#HOLD_CURSORS_OVER_COMMIT}. However, JDBC 4.0 spec says the default holdability is implementation defined.
     * Therefore, the default value will be 0.
     * 
     * @see java.sql.ResultSet#getHoldability()
     */
    @Override
    public int getHoldability() throws SQLException {
        notClosed();
        return 0;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getInt(int)
     */
    @Override
    public int getInt( int columnIndex ) throws SQLException {
        return getInt(findColumn(columnIndex));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getInt(java.lang.String)
     */
    @Override
    public int getInt( String columnLabel ) throws SQLException {
        notClosed();        
        return (int)getLong(columnLabel);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getLong(int)
     */
    @Override
    public long getLong( int columnIndex ) throws SQLException {
        return getLong(findColumn(columnIndex));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getLong(java.lang.String)
     */
    @Override
    public long getLong( String columnLabel ) throws SQLException {  
    	Object o = getValueReturn(columnLabel, PropertyType.LONG);
    	if (o != null) {
    		return ( (Long) o).longValue();
    	}
    	return 0L;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getNCharacterStream(int)
     */
    @Override
    public Reader getNCharacterStream( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getNCharacterStream(java.lang.String)
     */
    @Override
    public Reader getNCharacterStream( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getNClob(int)
     */
    @Override
    public NClob getNClob( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getNClob(java.lang.String)
     */
    @Override
    public NClob getNClob( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getNString(int)
     */
    @Override
    public String getNString( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getNString(java.lang.String)
     * @throws SQLFeatureNotSupportedException();
     */
    @Override
    public String getNString( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getObject(int)
     */
    @Override
    public Object getObject( int columnIndex ) throws SQLException {
        return getObject(findColumn(columnIndex));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getObject(java.lang.String)
     */
    @Override
    public Object getObject( String columnLabel ) throws SQLException {
        return getColumnTranslatedToJDBC(columnLabel);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getObject(int, java.util.Map)
     */
    @Override
    public Object getObject( int columnIndex,
                             Map<String, Class<?>> map ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getObject(java.lang.String, java.util.Map)
     */
    @Override
    public Object getObject( String columnLabel,
                             Map<String, Class<?>> map ) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getRef(int)
     */
    @Override
    public Ref getRef( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getRef(java.lang.String)
     */
    @Override
    public Ref getRef( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getSQLXML(int)
     */
    @Override
    public SQLXML getSQLXML( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getSQLXML(java.lang.String)
     */
    @Override
    public SQLXML getSQLXML( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getShort(int)
     */
    @Override
    public short getShort( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getShort(java.lang.String)
     */
    @Override
    public short getShort( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getString(int)
     */
    @Override
    public String getString( int columnIndex ) throws SQLException {
        return getString(findColumn(columnIndex));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getString(java.lang.String)
     */
    @Override
    public String getString( String columnLabel ) throws SQLException {
    	Object o = getValueReturn(columnLabel, PropertyType.STRING);
    	if (o != null) {
    		return (String) o;
    	}    
    	return null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getTime(int)
     */
    @Override
    public Time getTime( int columnIndex ) throws SQLException {
        return getTime(findColumn(columnIndex));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getTime(java.lang.String)
     */
    @Override
    public Time getTime( String columnLabel ) throws SQLException {
    	Calendar calv = (Calendar)getValueReturn(columnLabel, PropertyType.DATE);
    	if (calv == null) return null;  	
    	
    	return TimestampWithTimezone.createTime(calv);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getTime(int, java.util.Calendar)
     */
    @Override
    public Time getTime( int columnIndex,
                         Calendar cal ) throws SQLException {
        return getTime(findColumn(columnIndex), cal);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getTime(java.lang.String, java.util.Calendar)
     */
    @Override
    public Time getTime( String columnLabel,
                         Calendar cal ) throws SQLException {
    	
        Calendar actual = (Calendar)getValueReturn(columnLabel, PropertyType.DATE);

        if (actual == null) return null; 

        // if cal is null, it will be supplied in TimestampWithTimezone
        return TimestampWithTimezone.createTime(actual, cal);  
 
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getTimestamp(int)
     */
    @Override
    public Timestamp getTimestamp( int columnIndex ) throws SQLException {
        return getTimestamp(findColumn(columnIndex));
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getTimestamp(java.lang.String)
     */
    @Override
    public Timestamp getTimestamp( String columnLabel ) throws SQLException {
    	Calendar calv = (Calendar)getValueReturn(columnLabel, PropertyType.DATE);
    	if (calv == null) return null;
    	return TimestampWithTimezone.createTimestamp(calv);

    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getTimestamp(int, java.util.Calendar)
     */
    @Override
    public Timestamp getTimestamp( int columnIndex,
                                   Calendar cal ) throws SQLException {  	
    	 return getTimestamp(findColumn(columnIndex), cal);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getTimestamp(java.lang.String, java.util.Calendar)
     */
    @Override
    public Timestamp getTimestamp( String columnLabel,
                                   Calendar cal ) throws SQLException {
    	
        Calendar actual = (Calendar)getValueReturn(columnLabel, PropertyType.DATE);

        if (actual == null) return null;         
        
        // if cal is null, it will be supplied in TimestampWithTimezone
        return TimestampWithTimezone.createTimestamp(actual, cal);    	
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getURL(int)
     */
    @Override
    public URL getURL( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getURL(java.lang.String)
     */
    @Override
    public URL getURL( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getUnicodeStream(int)
     */
    @Override
    public InputStream getUnicodeStream( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getUnicodeStream(java.lang.String)
     */
    @Override
    public InputStream getUnicodeStream( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    public Value getValue( int columnIndex ) throws SQLException {
        return getValue(findColumn(columnIndex));

    }

    public Value getValue( String columnLabel ) throws SQLException {
        notClosed();
        isRowSet();

        try {
            return row.getValue(columnLabel);
        } catch (PathNotFoundException pnfe) {
            return null;
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage(), e);
        }

    }

    /**
     * This is called when the calling method controls what datatype to be returned. Another reason for centralizing this logic so
     * that the {@link #currentValue} can be maintained
     * 
     * @param columnName
     * @param type is the {@link PropertyType datatype} to be returned
     * @return Object
     * @throws SQLException
     */
    private Object getValueReturn( String columnName,
                                   int type ) throws SQLException {
        notClosed();
        isRowSet();

        this.currentValue = null;
        try {

            final Value value = row.getValue(columnName);
            this.currentValue = getValueObject(value, type);

        } catch (PathNotFoundException pnfe) {
        	// do nothing
        } catch (ItemNotFoundException e) {
            itemNotFoundUsingColunName(columnName);
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage(), e);
        }
        return this.currentValue;
    }

    /**
     * Helper method for returning an object from <code>value</code> based on <code>type</code>
     * 
     * @param value
     * @param type indicates {@link PropertyType} used to derive return object type
     * @return Object
     * @throws SQLException
     */
    private Object getValueObject( Value value,
                                   int type ) throws SQLException {
        if (value == null) return null;

        try {
            switch (type) {

                case PropertyType.STRING:
                    return value.getString();
                case PropertyType.BOOLEAN:
                    return value.getBoolean();
                case PropertyType.DATE:       
                	return value.getDate();
                case PropertyType.DOUBLE:
                    return value.getDouble();
                case PropertyType.LONG:
                    return value.getLong();
                case PropertyType.BINARY:
                    return value.getBinary().getStream();
            }

        } catch (ValueFormatException ve) {
            throw new SQLException(ve.getLocalizedMessage(), ve);
        } catch (IllegalStateException ie) {
            throw new SQLException(ie.getLocalizedMessage(), ie);
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage(), e);
        }
        return value.toString();
    }

    /**
     * This method transforms a {@link Value} into a JDBC type based on {@link JcrType} mappings
     * 
     * @param columnName
     * @return Object
     * @throws SQLException
     */
    private Object getColumnTranslatedToJDBC( String columnName ) throws SQLException {
        notClosed();
        isRowSet();

        Value value = null;
        this.currentValue = null;

        try {
            value = row.getValue(columnName);
        } catch (javax.jcr.PathNotFoundException pnf) {
            // do nothing
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage(), e);
        }

        if (value == null) return null;

        this.currentValue = JcrType.translateValueToJDBC(value);
        return this.currentValue;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#getWarnings()
     */
    @Override
    public SQLWarning getWarnings() /*throws SQLException*/{
        return null;
    }

    protected boolean hasNext() {
        return rowIter.hasNext();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#insertRow()
     */
    @Override
    public void insertRow() throws SQLException {
        this.notClosed();
        this.noUpdates();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#isAfterLast()
     */
    @Override
    public boolean isAfterLast() throws SQLException {
        this.notClosed();
        if (this.row == null && !this.rowIter.hasNext() && this.rowIter.getPosition() == this.rowIter.getSize()) {
            return true;
        }
        return false;

    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#isBeforeFirst()
     */
    @Override
    public boolean isBeforeFirst() throws SQLException {
        this.notClosed();
        if (this.rowIter.getPosition() == 0) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#isFirst()
     */
    @Override
    public boolean isFirst() throws SQLException {
        this.notClosed();
        if (this.rowIter.getPosition() == 1) {
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#isLast()
     */
    @Override
    public boolean isLast() throws SQLException {
        this.notClosed();

        if (this.row != null && !this.rowIter.hasNext() && this.rowIter.getPosition() == this.rowIter.getSize()) {
            return true;
        }
        return false;
    }

    protected final void isRowSet() throws SQLException {
        if (this.row != null) return;

        throw new SQLException(JdbcI18n.currentRowNotSet.text());
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLException} because this driver only supports
     * {@link ResultSet#FETCH_FORWARD}.
     * </p>
     * 
     * @see java.sql.ResultSet#last()
     */
    @Override
    public boolean last() throws SQLException {
        notClosed();
        forwardOnly();
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLException} because this driver only supports
     * {@link ResultSet#FETCH_FORWARD}.
     * </p>
     * 
     * @see java.sql.ResultSet#moveToCurrentRow()
     */
    @Override
    public void moveToCurrentRow() throws SQLException {
        notClosed();
        forwardOnly();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#moveToInsertRow()
     */
    @Override
    public void moveToInsertRow() throws SQLException {
        this.noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when cursor position is after the last row, will return <code>false</code>
     * </p>
     * 
     * @see java.sql.ResultSet#next()
     */
    @Override
    public boolean next() throws SQLException {
        notClosed();
        if (!this.hasNext()) {
            this.row = null;
            this.currentValue = null;
            return false;
        }

        this.row = rowIter.nextRow();
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLException} because this driver only supports
     * {@link ResultSet#FETCH_FORWARD}.
     * </p>
     * 
     * @see java.sql.ResultSet#previous()
     */
    @Override
    public boolean previous() throws SQLException {
        notClosed();
        this.forwardOnly();
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLException} because this driver only supports
     * {@link ResultSet#FETCH_FORWARD}.
     * </p>
     * 
     * @see java.sql.ResultSet#refreshRow()
     */
    @Override
    public void refreshRow() throws SQLException {
        notClosed();
        this.forwardOnly();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLException} because this driver only supports
     * {@link ResultSet#FETCH_FORWARD}.
     * </p>
     * 
     * @see java.sql.ResultSet#relative(int)
     */
    @Override
    public boolean relative( int rows ) throws SQLException {
    	throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method always returns false since this JDBC driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#rowDeleted()
     */
    @Override
    public boolean rowDeleted() throws SQLException {
    	throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method always returns false since this JDBC driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#rowInserted()
     */
    @Override
    public boolean rowInserted() throws SQLException {
    	throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method always returns false since this JDBC driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#rowUpdated()
     */
    @Override
    public boolean rowUpdated() throws SQLException {
    	throw new SQLFeatureNotSupportedException();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#setFetchSize(int)
     */
    @Override
    public void setFetchSize( int rows ) /*throws SQLException*/{
        // does nothing
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateArray(int, java.sql.Array)
     */
    @Override
    public void updateArray( int columnIndex,
                             Array x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateArray(java.lang.String, java.sql.Array)
     */
    @Override
    public void updateArray( String columnLabel,
                             Array x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateAsciiStream(int, java.io.InputStream)
     */
    @Override
    public void updateAsciiStream( int columnIndex,
                                   InputStream x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateAsciiStream(java.lang.String, java.io.InputStream)
     */
    @Override
    public void updateAsciiStream( String columnLabel,
                                   InputStream x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateAsciiStream(int, java.io.InputStream, int)
     */
    @Override
    public void updateAsciiStream( int columnIndex,
                                   InputStream x,
                                   int length ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateAsciiStream(java.lang.String, java.io.InputStream, int)
     */
    @Override
    public void updateAsciiStream( String columnLabel,
                                   InputStream x,
                                   int length ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateAsciiStream(int, java.io.InputStream, long)
     */
    @Override
    public void updateAsciiStream( int columnIndex,
                                   InputStream x,
                                   long length ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateAsciiStream(java.lang.String, java.io.InputStream, long)
     */
    @Override
    public void updateAsciiStream( String columnLabel,
                                   InputStream x,
                                   long length ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateBigDecimal(int, java.math.BigDecimal)
     */
    @Override
    public void updateBigDecimal( int columnIndex,
                                  BigDecimal x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateBigDecimal(java.lang.String, java.math.BigDecimal)
     */
    @Override
    public void updateBigDecimal( String columnLabel,
                                  BigDecimal x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateBinaryStream(int, java.io.InputStream)
     */
    @Override
    public void updateBinaryStream( int columnIndex,
                                    InputStream x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateBinaryStream(java.lang.String, java.io.InputStream)
     */
    @Override
    public void updateBinaryStream( String columnLabel,
                                    InputStream x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateBinaryStream(int, java.io.InputStream, int)
     */
    @Override
    public void updateBinaryStream( int columnIndex,
                                    InputStream x,
                                    int length ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateBinaryStream(java.lang.String, java.io.InputStream, int)
     */
    @Override
    public void updateBinaryStream( String columnLabel,
                                    InputStream x,
                                    int length ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateBinaryStream(int, java.io.InputStream, long)
     */
    @Override
    public void updateBinaryStream( int columnIndex,
                                    InputStream x,
                                    long length ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateBinaryStream(java.lang.String, java.io.InputStream, long)
     */
    @Override
    public void updateBinaryStream( String columnLabel,
                                    InputStream x,
                                    long length ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateBlob(int, java.sql.Blob)
     */
    @Override
    public void updateBlob( int columnIndex,
                            Blob x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateBlob(java.lang.String, java.sql.Blob)
     */
    @Override
    public void updateBlob( String columnLabel,
                            Blob x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateBlob(int, java.io.InputStream)
     */
    @Override
    public void updateBlob( int columnIndex,
                            InputStream inputStream ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateBlob(java.lang.String, java.io.InputStream)
     */
    @Override
    public void updateBlob( String columnLabel,
                            InputStream inputStream ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateBlob(int, java.io.InputStream, long)
     */
    @Override
    public void updateBlob( int columnIndex,
                            InputStream inputStream,
                            long length ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateBlob(java.lang.String, java.io.InputStream, long)
     */
    @Override
    public void updateBlob( String columnLabel,
                            InputStream inputStream,
                            long length ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateBoolean(int, boolean)
     */
    @Override
    public void updateBoolean( int columnIndex,
                               boolean x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateBoolean(java.lang.String, boolean)
     */
    @Override
    public void updateBoolean( String columnLabel,
                               boolean x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateByte(int, byte)
     */
    @Override
    public void updateByte( int columnIndex,
                            byte x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateByte(java.lang.String, byte)
     */
    @Override
    public void updateByte( String columnLabel,
                            byte x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateBytes(int, byte[])
     */
    @Override
    public void updateBytes( int columnIndex,
                             byte[] x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateBytes(java.lang.String, byte[])
     */
    @Override
    public void updateBytes( String columnLabel,
                             byte[] x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateCharacterStream(int, java.io.Reader)
     */
    @Override
    public void updateCharacterStream( int columnIndex,
                                       Reader x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateCharacterStream(java.lang.String, java.io.Reader)
     */
    @Override
    public void updateCharacterStream( String columnLabel,
                                       Reader reader ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateCharacterStream(int, java.io.Reader, int)
     */
    @Override
    public void updateCharacterStream( int columnIndex,
                                       Reader x,
                                       int length ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateCharacterStream(java.lang.String, java.io.Reader, int)
     */
    @Override
    public void updateCharacterStream( String columnLabel,
                                       Reader reader,
                                       int length ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateCharacterStream(int, java.io.Reader, long)
     */
    @Override
    public void updateCharacterStream( int columnIndex,
                                       Reader x,
                                       long length ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateCharacterStream(java.lang.String, java.io.Reader, long)
     */
    @Override
    public void updateCharacterStream( String columnLabel,
                                       Reader reader,
                                       long length ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateClob(int, java.sql.Clob)
     */
    @Override
    public void updateClob( int columnIndex,
                            Clob x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateClob(java.lang.String, java.sql.Clob)
     */
    @Override
    public void updateClob( String columnLabel,
                            Clob x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateClob(int, java.io.Reader)
     */
    @Override
    public void updateClob( int columnIndex,
                            Reader reader ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateClob(java.lang.String, java.io.Reader)
     */
    @Override
    public void updateClob( String columnLabel,
                            Reader reader ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateClob(int, java.io.Reader, long)
     */
    @Override
    public void updateClob( int columnIndex,
                            Reader reader,
                            long length ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateClob(java.lang.String, java.io.Reader, long)
     */
    @Override
    public void updateClob( String columnLabel,
                            Reader reader,
                            long length ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateDate(int, java.sql.Date)
     */
    @Override
    public void updateDate( int columnIndex,
                            Date x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateDate(java.lang.String, java.sql.Date)
     */
    @Override
    public void updateDate( String columnLabel,
                            Date x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateDouble(int, double)
     */
    @Override
    public void updateDouble( int columnIndex,
                              double x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateDouble(java.lang.String, double)
     */
    @Override
    public void updateDouble( String columnLabel,
                              double x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateFloat(int, float)
     */
    @Override
    public void updateFloat( int columnIndex,
                             float x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateFloat(java.lang.String, float)
     */
    @Override
    public void updateFloat( String columnLabel,
                             float x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateInt(int, int)
     */
    @Override
    public void updateInt( int columnIndex,
                           int x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateInt(java.lang.String, int)
     */
    @Override
    public void updateInt( String columnLabel,
                           int x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateLong(int, long)
     */
    @Override
    public void updateLong( int columnIndex,
                            long x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateLong(java.lang.String, long)
     */
    @Override
    public void updateLong( String columnLabel,
                            long x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateNCharacterStream(int, java.io.Reader)
     */
    @Override
    public void updateNCharacterStream( int columnIndex,
                                        Reader x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateNCharacterStream(java.lang.String, java.io.Reader)
     */
    @Override
    public void updateNCharacterStream( String columnLabel,
                                        Reader reader ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateNCharacterStream(int, java.io.Reader, long)
     */
    @Override
    public void updateNCharacterStream( int columnIndex,
                                        Reader x,
                                        long length ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateNCharacterStream(java.lang.String, java.io.Reader, long)
     */
    @Override
    public void updateNCharacterStream( String columnLabel,
                                        Reader reader,
                                        long length ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateNClob(int, java.sql.NClob)
     */
    @Override
    public void updateNClob( int columnIndex,
                             NClob clob ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateNClob(java.lang.String, java.sql.NClob)
     */
    @Override
    public void updateNClob( String columnLabel,
                             NClob clob ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateNClob(int, java.io.Reader)
     */
    @Override
    public void updateNClob( int columnIndex,
                             Reader reader ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateNClob(java.lang.String, java.io.Reader)
     */
    @Override
    public void updateNClob( String columnLabel,
                             Reader reader ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateNClob(int, java.io.Reader, long)
     */
    @Override
    public void updateNClob( int columnIndex,
                             Reader reader,
                             long length ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateNClob(java.lang.String, java.io.Reader, long)
     */
    @Override
    public void updateNClob( String columnLabel,
                             Reader reader,
                             long length ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateNString(int, java.lang.String)
     */
    @Override
    public void updateNString( int columnIndex,
                               String string ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateNString(java.lang.String, java.lang.String)
     */
    @Override
    public void updateNString( String columnLabel,
                               String string ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateNull(int)
     */
    @Override
    public void updateNull( int columnIndex ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateNull(java.lang.String)
     */
    @Override
    public void updateNull( String columnLabel ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateObject(int, java.lang.Object)
     */
    @Override
    public void updateObject( int columnIndex,
                              Object x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateObject(java.lang.String, java.lang.Object)
     */
    @Override
    public void updateObject( String columnLabel,
                              Object x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateObject(int, java.lang.Object, int)
     */
    @Override
    public void updateObject( int columnIndex,
                              Object x,
                              int scaleOrLength ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateObject(java.lang.String, java.lang.Object, int)
     */
    @Override
    public void updateObject( String columnLabel,
                              Object x,
                              int scaleOrLength ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateRef(int, java.sql.Ref)
     */
    @Override
    public void updateRef( int columnIndex,
                           Ref x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateRef(java.lang.String, java.sql.Ref)
     */
    @Override
    public void updateRef( String columnLabel,
                           Ref x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateRow()
     */
    @Override
    public void updateRow() throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateRowId(int, java.sql.RowId)
     */
    @Override
    public void updateRowId( int columnIndex,
                             RowId x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateRowId(java.lang.String, java.sql.RowId)
     */
    @Override
    public void updateRowId( String columnLabel,
                             RowId x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateSQLXML(int, java.sql.SQLXML)
     */
    @Override
    public void updateSQLXML( int columnIndex,
                              SQLXML xmlObject ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateSQLXML(java.lang.String, java.sql.SQLXML)
     */
    @Override
    public void updateSQLXML( String columnLabel,
                              SQLXML xmlObject ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateShort(int, short)
     */
    @Override
    public void updateShort( int columnIndex,
                             short x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateShort(java.lang.String, short)
     */
    @Override
    public void updateShort( String columnLabel,
                             short x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateString(int, java.lang.String)
     */
    @Override
    public void updateString( int columnIndex,
                              String x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateString(java.lang.String, java.lang.String)
     */
    @Override
    public void updateString( String columnLabel,
                              String x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateTime(int, java.sql.Time)
     */
    @Override
    public void updateTime( int columnIndex,
                            Time x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateTime(java.lang.String, java.sql.Time)
     */
    @Override
    public void updateTime( String columnLabel,
                            Time x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateTimestamp(int, java.sql.Timestamp)
     */
    @Override
    public void updateTimestamp( int columnIndex,
                                 Timestamp x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method, when called on an open result set, always throws {@link SQLFeatureNotSupportedException} since this JDBC
     * driver does not support any updates.
     * </p>
     * 
     * @see java.sql.ResultSet#updateTimestamp(java.lang.String, java.sql.Timestamp)
     */
    @Override
    public void updateTimestamp( String columnLabel,
                                 Timestamp x ) throws SQLException {
        notClosed();
        noUpdates();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSet#wasNull()
     */
    @Override
    public boolean wasNull() throws SQLException {
        notClosed();

        return currentValue == null;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
     */
    @Override
    public boolean isWrapperFor( Class<?> iface ) {
        return iface.isInstance(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Wrapper#unwrap(java.lang.Class)
     */
    @Override
    public <T> T unwrap( Class<T> iface ) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }

        throw new SQLException(JdbcI18n.classDoesNotImplementInterface.text());
    }

}
