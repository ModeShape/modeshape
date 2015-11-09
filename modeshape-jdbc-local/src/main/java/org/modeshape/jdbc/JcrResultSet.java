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

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.math.BigDecimal;
import java.net.MalformedURLException;
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
import javax.jcr.Binary;
import javax.jcr.ItemNotFoundException;
import javax.jcr.PathNotFoundException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import javax.jcr.query.QueryResult;
import javax.jcr.query.Row;
import javax.jcr.query.RowIterator;
import org.modeshape.common.util.IoUtil;
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

        this.columnIndexesByName = Collections.unmodifiableMap(columnIndexesByName);

        try {
            this.rowIter = this.jcrResults.getRows();
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage(), e);
        }
    }

    protected JcrResultSet() {
        closed = true;
        columnIndexesByName = Collections.emptyMap();
    }

    @Override
    public boolean isClosed() {
        return closed || statement.isClosed();
    }

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
                case PropertyType.LONG:
                case PropertyType.DATE:
                case PropertyType.DECIMAL:
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
        if (isClosed()) throw new SQLException(JdbcLocalI18n.resultSetIsClosed.text());
    }

    protected final void noUpdates() throws SQLException {
        throw new SQLFeatureNotSupportedException(JdbcLocalI18n.updatesNotSupported.text());
    }

    protected final void forwardOnly() throws SQLException {
        throw new SQLException(JdbcLocalI18n.resultSetIsForwardOnly.text());
    }

    protected final void itemNotFoundUsingColunName( String columnName ) throws SQLException {
        throw new SQLException(JdbcLocalI18n.noSuchColumn.text(columnName));
    }

    @Override
    public ResultSetMetaData getMetaData() throws SQLException {
        notClosed();
        return metadata;
    }

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

    @Override
    public void cancelRowUpdates() throws SQLException {
        notClosed();
        noUpdates();
    }

    @Override
    public void clearWarnings() throws SQLException {
        notClosed();
    }

    @Override
    public void deleteRow() throws SQLException {
        notClosed();
        noUpdates();
    }

    @Override
    public int findColumn( String columnLabel ) throws SQLException {
        notClosed();
        final Integer result = columnLabel != null ? columnIndexesByName.get(columnLabel) : null;

        if (result == null) {
            this.itemNotFoundUsingColunName(columnLabel);
        }
        assert result != null;
        return result;
    }

    private String findColumn( int columnIndex ) throws SQLException {
        if (columnIndex > 0 && columnIndex < this.columnIDs.length) {
            return columnIDs[columnIndex];
        }

        throw new SQLException(JdbcLocalI18n.invalidColumnIndex.text(new Object[] {columnIndex, this.columnIDs.length}));
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

    @Override
    public int getRow() throws SQLException {
        notClosed();
        return (int)this.rowIter.getPosition();
    }

    @Override
    public RowId getRowId( final int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public RowId getRowId( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public String getCursorName() throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int getFetchSize() throws SQLException {
        notClosed();
        return statement.getFetchSize();
    }

    @Override
    public Array getArray( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Array getArray( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public InputStream getAsciiStream( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public InputStream getAsciiStream( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public BigDecimal getBigDecimal( int columnIndex ) throws SQLException {
        return getBigDecimal(findColumn(columnIndex));
    }

    @Override
    public BigDecimal getBigDecimal( String columnLabel ) throws SQLException {
        Object o = updateCurrentValueFromColumn(columnLabel, PropertyType.DECIMAL);
        if (o != null) {
            return (BigDecimal)o;
        }
        return null;
    }

    @Override
    @SuppressWarnings("deprecation")
    public BigDecimal getBigDecimal( int columnIndex,
                                     int scale ) throws SQLException {
        return getBigDecimal(columnIndex).setScale(scale);
    }

    @Override
    @SuppressWarnings("deprecation")
    public BigDecimal getBigDecimal( String columnLabel,
                                     int scale ) throws SQLException {
        return getBigDecimal(columnLabel).setScale(scale);
    }

    @Override
    public InputStream getBinaryStream( int columnIndex ) throws SQLException {
        return getBinaryStream(findColumn(columnIndex));
    }

    @Override
    public InputStream getBinaryStream( String columnLabel ) throws SQLException {
        Object o = updateCurrentValueFromColumn(columnLabel, PropertyType.BINARY);
        if (o != null) {
            try {
                return ((Binary) o).getStream();
            } catch (RepositoryException e) {
                throw new SQLException(e);
            }
        }
        return null;
    }

    @Override
    public Blob getBlob( int columnIndex ) throws SQLException {
        return getBlob(findColumn(columnIndex));
    }

    @Override
    public Blob getBlob( String columnLabel ) throws SQLException {
        Object o = updateCurrentValueFromColumn(columnLabel, PropertyType.BINARY);
        if (o != null) {
            return new JcrBlob((Binary)o);
        }
        return null;
    }

    @Override
    public boolean getBoolean( int columnIndex ) throws SQLException {
        return getBoolean(findColumn(columnIndex));
    }

    @Override
    public boolean getBoolean( String columnLabel ) throws SQLException {

        Object o = updateCurrentValueFromColumn(columnLabel, PropertyType.BOOLEAN);
        if (o != null) {
            return (Boolean)o;
        }
        return false;
    }

    @Override
    public byte getByte( int columnIndex ) throws SQLException {
        return getByte(findColumn(columnIndex));
    }

    @Override
    public byte getByte( String columnLabel ) throws SQLException {
        return Long.valueOf(getLong(columnLabel)).byteValue();
    }

    @Override
    public byte[] getBytes( int columnIndex ) throws SQLException {
        return getBytes(findColumn(columnIndex));
    }

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

    @Override
    public Reader getCharacterStream( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Reader getCharacterStream( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Clob getClob( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Clob getClob( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public int getConcurrency() throws SQLException {
        notClosed();
        return 0;
    }

    @Override
    public Date getDate( int columnIndex ) throws SQLException {
        return getDate(findColumn(columnIndex));
    }

    @Override
    public Date getDate( String columnLabel ) throws SQLException {
        Calendar calv = (Calendar)updateCurrentValueFromColumn(columnLabel, PropertyType.DATE);
        if (calv == null) return null;

        return TimestampWithTimezone.createDate(calv);
    }

    @Override
    public Date getDate( int columnIndex,
                         Calendar cal ) throws SQLException {
        return getDate(findColumn(columnIndex), cal);
    }

    @Override
    public Date getDate( String columnLabel,
                         Calendar cal ) throws SQLException {

        Calendar actual = (Calendar)updateCurrentValueFromColumn(columnLabel, PropertyType.DATE);

        if (actual == null) return null;

        return TimestampWithTimezone.createDate(actual, cal);

    }

    @Override
    public double getDouble( int columnIndex ) throws SQLException {
        return getDouble(findColumn(columnIndex));
    }

    @Override
    public double getDouble( String columnLabel ) throws SQLException {
        Object o = updateCurrentValueFromColumn(columnLabel, PropertyType.DOUBLE);
        if (o != null) {
            return (Double)o;
        }
        return 0;
    }

    @Override
    public float getFloat( int columnIndex ) throws SQLException {
        return getFloat(findColumn(columnIndex));
    }

    @Override
    public float getFloat( String columnLabel ) throws SQLException {
        return Double.valueOf(getDouble(columnLabel)).floatValue();
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

    @Override
    public int getInt( int columnIndex ) throws SQLException {
        return getInt(findColumn(columnIndex));
    }

    @Override
    public int getInt( String columnLabel ) throws SQLException {
        notClosed();
        return (int)getLong(columnLabel);
    }

    @Override
    public long getLong( int columnIndex ) throws SQLException {
        return getLong(findColumn(columnIndex));
    }

    @Override
    public long getLong( String columnLabel ) throws SQLException {
        Object o = updateCurrentValueFromColumn(columnLabel, PropertyType.LONG);
        if (o != null) {
            return (Long)o;
        }
        return 0L;
    }

    @Override
    public Reader getNCharacterStream( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Reader getNCharacterStream( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public NClob getNClob( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public NClob getNClob( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public String getNString( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public String getNString( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Object getObject( int columnIndex ) throws SQLException {
        return getObject(findColumn(columnIndex));
    }

    @Override
    public Object getObject( String columnLabel ) throws SQLException {
        return getColumnTranslatedToJDBC(columnLabel);
    }

    @Override
    public Object getObject( int columnIndex,
                             Map<String, Class<?>> map ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Object getObject( String columnLabel,
                             Map<String, Class<?>> map ) throws SQLException {
        throw new SQLFeatureNotSupportedException();

    }

    /**
     * This method always throws {@link SQLFeatureNotSupportedException}.
     * <p>
     * <em>Note:</em> This method is part of the JDBC API in JDK 1.7.
     * </p>
     * 
     * @param columnIndex
     * @param type
     * @param <T> the type class
     * @return the object
     * @throws SQLException
     */
    @Override
    public <T> T getObject( int columnIndex,
                            Class<T> type ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    /**
     * This method always throws {@link SQLFeatureNotSupportedException}.
     * <p>
     * <em>Note:</em> This method is part of the JDBC API in JDK 1.7.
     * </p>
     * 
     * @param columnLabel
     * @param type
     * @param <T> the type class
     * @return the object
     * @throws SQLException
     */
    @Override
    public <T> T getObject( String columnLabel,
                            Class<T> type ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Ref getRef( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public Ref getRef( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public SQLXML getSQLXML( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public SQLXML getSQLXML( String columnLabel ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    public short getShort( int columnIndex ) throws SQLException {
        return getShort(findColumn(columnIndex));
    }

    @Override
    public short getShort( String columnLabel ) throws SQLException {
        return Long.valueOf(getLong(columnLabel)).shortValue();
    }

    @Override
    public String getString( int columnIndex ) throws SQLException {
        return getString(findColumn(columnIndex));
    }

    @Override
    public String getString( String columnLabel ) throws SQLException {
        Object o = updateCurrentValueFromColumn(columnLabel, PropertyType.STRING);
        if (o != null) {
            return (String)o;
        }
        return null;
    }

    @Override
    public Time getTime( int columnIndex ) throws SQLException {
        return getTime(findColumn(columnIndex));
    }

    @Override
    public Time getTime( String columnLabel ) throws SQLException {
        Calendar calv = (Calendar)updateCurrentValueFromColumn(columnLabel, PropertyType.DATE);
        if (calv == null) return null;

        return TimestampWithTimezone.createTime(calv);
    }

    @Override
    public Time getTime( int columnIndex,
                         Calendar cal ) throws SQLException {
        return getTime(findColumn(columnIndex), cal);
    }

    @Override
    public Time getTime( String columnLabel,
                         Calendar cal ) throws SQLException {

        Calendar actual = (Calendar)updateCurrentValueFromColumn(columnLabel, PropertyType.DATE);

        if (actual == null) return null;

        // if cal is null, it will be supplied in TimestampWithTimezone
        return TimestampWithTimezone.createTime(actual, cal);

    }

    @Override
    public Timestamp getTimestamp( int columnIndex ) throws SQLException {
        return getTimestamp(findColumn(columnIndex));
    }

    @Override
    public Timestamp getTimestamp( String columnLabel ) throws SQLException {
        Calendar calv = (Calendar)updateCurrentValueFromColumn(columnLabel, PropertyType.DATE);
        if (calv == null) return null;
        return TimestampWithTimezone.createTimestamp(calv);

    }

    @Override
    public Timestamp getTimestamp( int columnIndex,
                                   Calendar cal ) throws SQLException {
        return getTimestamp(findColumn(columnIndex), cal);
    }

    @Override
    public Timestamp getTimestamp( String columnLabel,
                                   Calendar cal ) throws SQLException {

        Calendar actual = (Calendar)updateCurrentValueFromColumn(columnLabel, PropertyType.DATE);
        if (actual == null) return null;
        // if cal is null, it will be supplied in TimestampWithTimezone
        return TimestampWithTimezone.createTimestamp(actual, cal);
    }

    @Override
    public URL getURL( int columnIndex ) throws SQLException {
        return getURL(findColumn(columnIndex));
    }

    @Override
    public URL getURL( String columnLabel ) throws SQLException {
        try {
            return new URL(getString(columnLabel));
        } catch (MalformedURLException e) {
            throw new SQLException(e);
        }
    }

    @Override
    @SuppressWarnings("deprecation")
    public InputStream getUnicodeStream( int columnIndex ) throws SQLException {
        throw new SQLFeatureNotSupportedException();
    }

    @Override
    @SuppressWarnings("deprecation")
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

    private Object updateCurrentValueFromColumn( String columnName,
                                                 int asType ) throws SQLException {
        notClosed();
        isRowSet();

        this.currentValue = null;
        try {
            final Value jcrValue = row.getValue(columnName);
            this.currentValue = extractValue(jcrValue, asType);
        } catch (PathNotFoundException pnfe) {
            // do nothing
        } catch (ItemNotFoundException e) {
            itemNotFoundUsingColunName(columnName);
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage(), e);
        }
        return this.currentValue;
    }

    private Object extractValue( Value jcrValue,
                                 int asType ) throws SQLException {
        if (jcrValue == null) {
            return null;
        }

        try {
            switch (asType) {
                case PropertyType.STRING: {
                    return jcrValue.getString();
                }
                case PropertyType.BOOLEAN: {
                    return jcrValue.getBoolean();
                }
                case PropertyType.DATE: {
                    return jcrValue.getDate();
                }
                case PropertyType.DOUBLE: {
                    return jcrValue.getDouble();
                }
                case PropertyType.LONG: {
                    return jcrValue.getLong();
                }
                case PropertyType.DECIMAL:  {
                    return jcrValue.getDecimal();
                }
                case PropertyType.BINARY: {
                    if (jcrValue.getType() != PropertyType.BINARY) {
                        throw new SQLException(JdbcLocalI18n.cannotConvertJcrValue.text(PropertyType.nameFromValue(jcrValue.getType()),
                                                                                        PropertyType.TYPENAME_BINARY));
                    }
                    return jcrValue.getBinary();
                }
                default: {
                    return jcrValue.getString();
                }
            }
        } catch (ValueFormatException ve) {
            throw new SQLException(JdbcLocalI18n.cannotConvertJcrValue.text(PropertyType.nameFromValue(jcrValue.getType()),
                                                                            PropertyType.nameFromValue(asType)), ve);
        } catch (IllegalArgumentException ie) {
            throw new SQLException(JdbcLocalI18n.cannotConvertJcrValue.text(PropertyType.nameFromValue(jcrValue.getType()),
                                                                            PropertyType.nameFromValue(asType)), ie);
        } catch (RepositoryException e) {
            throw new SQLException(e);
        }
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

    @Override
    public SQLWarning getWarnings() /*throws SQLException*/{
        return null;
    }

    protected boolean hasNext() {
        return rowIter.hasNext();
    }

    @Override
    public void insertRow() throws SQLException {
        this.notClosed();
        this.noUpdates();
    }

    @Override
    public boolean isAfterLast() throws SQLException {
        this.notClosed();
        if (this.row == null && !this.rowIter.hasNext() && this.rowIter.getPosition() == this.rowIter.getSize()) {
            return true;
        }
        return false;

    }

    @Override
    public boolean isBeforeFirst() throws SQLException {
        this.notClosed();
        if (this.rowIter.getPosition() == 0) {
            return true;
        }
        return false;
    }

    @Override
    public boolean isFirst() throws SQLException {
        this.notClosed();
        if (this.rowIter.getPosition() == 1) {
            return true;
        }
        return false;
    }

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

        throw new SQLException(JdbcLocalI18n.currentRowNotSet.text());
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

    @Override
    public boolean wasNull() throws SQLException {
        notClosed();

        return currentValue == null;
    }

    @Override
    public boolean isWrapperFor( Class<?> iface ) {
        return iface.isInstance(this);
    }

    @Override
    public <T> T unwrap( Class<T> iface ) throws SQLException {
        if (iface.isInstance(this)) {
            return iface.cast(this);
        }

        throw new SQLException(JdbcLocalI18n.classDoesNotImplementInterface.text());
    }

}
