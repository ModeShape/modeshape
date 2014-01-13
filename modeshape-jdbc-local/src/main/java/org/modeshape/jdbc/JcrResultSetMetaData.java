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

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.nodetype.NodeType;
import javax.jcr.nodetype.PropertyDefinition;
import javax.jcr.query.QueryResult;
import org.modeshape.jdbc.JcrType.DefaultDataTypes;

/**
 * This driver's {@link ResultSetMetaData} implementation that obtains the metadata information from the JCR query result and
 * (where possible) the column's corresponding property definitions.
 */
public class JcrResultSetMetaData implements ResultSetMetaData {

    private final JcrConnection connection;
    private final QueryResult results;
    private int[] nullable;

    protected JcrResultSetMetaData( JcrConnection connection,
                                    QueryResult results ) {
        this.connection = connection;
        this.results = results;
    }

    /**
     * {@inheritDoc}
     * <p>
     * All columns come from the same repository (i.e., catalog).
     * </p>
     * 
     * @see java.sql.ResultSetMetaData#getCatalogName(int)
     */
    @Override
    public String getCatalogName( int column ) {
        return connection.info().getRepositoryName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#getColumnClassName(int)
     */
    @Override
    public String getColumnClassName( int column ) {
        return getJcrType(column).getRepresentationClass().getName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#getColumnCount()
     */
    @Override
    public int getColumnCount() throws SQLException {
        try {
            return results.getColumnNames().length;
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method returns the nominal display size based upon the column's type. Therefore, the value may not reflect the optimal
     * display size for any given <i>value</i>.
     * </p>
     * 
     * @see java.sql.ResultSetMetaData#getColumnDisplaySize(int)
     */
    @Override
    public int getColumnDisplaySize( int column ) {
        return getJcrType(column).getNominalDisplaySize();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#getColumnLabel(int)
     */
    @Override
    public String getColumnLabel( int column ) throws SQLException {
        try {
            return results.getColumnNames()[column - 1]; // column value is 1-based
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#getColumnName(int)
     */
    @Override
    public String getColumnName( int column ) throws SQLException {
        return getColumnLabel(column);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#getColumnType(int)
     */
    @Override
    public int getColumnType( int column ) {
        return getJcrType(column).getJdbcType();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#getColumnTypeName(int)
     */
    @Override
    public String getColumnTypeName( int column ) {
        return getJcrType(column).getJdbcTypeName();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method always returns the nominal display size for the type.
     * </p>
     * 
     * @see java.sql.ResultSetMetaData#getPrecision(int)
     */
    @Override
    public int getPrecision( int column ) {
        return getJcrType(column).getNominalDisplaySize();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method returns the number of digits behind the decimal point, which is assumed to be 3 if the type is
     * {@link PropertyType#DOUBLE} or 0 otherwise.
     * </p>
     * 
     * @see java.sql.ResultSetMetaData#getScale(int)
     */
    @Override
    public int getScale( int column ) {
        JcrType typeInfo = getJcrType(column);
        if (typeInfo.getJcrType() == PropertyType.DOUBLE) {
            return 3; // pulled from thin air
        }
        return 0;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method always returns the workspace name.
     * </p>
     * 
     * @see java.sql.ResultSetMetaData#getSchemaName(int)
     */
    @Override
    public String getSchemaName( int column ) {
        return connection.info().getWorkspaceName();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#getTableName(int)
     */
    @Override
    public String getTableName( int column ) throws SQLException {
        try {
            return results.getSelectorNames()[column - 1]; // column value is 1-based
        } catch (RepositoryException e) {
            throw new SQLException(e.getLocalizedMessage(), e);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method always returns false, since this JCR property types don't represent auto-incremented values.
     * </p>
     * 
     * @see java.sql.ResultSetMetaData#isAutoIncrement(int)
     */
    @Override
    public boolean isAutoIncrement( int column ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#isCaseSensitive(int)
     */
    @Override
    public boolean isCaseSensitive( int column ) {
        return getJcrType(column).isCaseSensitive();
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method always returns false, since no JCR property types (directly) represent currency.
     * </p>
     * 
     * @see java.sql.ResultSetMetaData#isCurrency(int)
     */
    @Override
    public boolean isCurrency( int column ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method always returns false, since this JDBC driver does not support writes.
     * </p>
     * 
     * @see java.sql.ResultSetMetaData#isDefinitelyWritable(int)
     */
    @Override
    public boolean isDefinitelyWritable( int column ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.ResultSetMetaData#isNullable(int)
     */
    @Override
    public int isNullable( int column ) throws SQLException {
        if (nullable == null) {
            int length = getColumnCount();
            nullable = new int[length];
            for (int i = 0; i != length; ++i) {
                nullable[i] = -1;
            }
        } else {
            int result = nullable[column - 1];
            if (result != -1) {
                // Already found this value, so return it ...
                return result;
            }
        }
        // Find the node type for the column (given that the column name is the property name and
        // the table name is the node type), and determine if the property definition is multi-valued or not mandatory.
        String nodeTypeName = getTableName(column);
        if (nodeTypeName.length() == 0) {
            // There is no table for the column, so therefore we don't know the node type ...
            return ResultSetMetaData.columnNullableUnknown;
        }
        String propertyName = getColumnName(column);
        boolean singleProp = false;
        boolean singleResidual = false;
        boolean multiResidual = false;
        NodeType type = connection.nodeType(nodeTypeName);
        for (PropertyDefinition defn : type.getPropertyDefinitions()) {
            if (defn.getName().equals(propertyName)) {
                if (defn.isMultiple() || defn.isMandatory()) {
                    // We know this IS nullable
                    return ResultSetMetaData.columnNullable;
                }
                // Otherwise this is a single-valued property that is mandatory,
                // but we can't return columnNotNullable because we may not have found the multi-valued property ...
                singleProp = true;
            } else if (defn.getName().equals("*")) { // Residual
                if (defn.isMultiple() || defn.isMandatory()) multiResidual = true;
                else singleResidual = true;
            }
        }
        int result = ResultSetMetaData.columnNullableUnknown;
        if (multiResidual) result = ResultSetMetaData.columnNullable;
        else if (singleProp || singleResidual) result = ResultSetMetaData.columnNoNulls;
        nullable[column - 1] = result;
        return result;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Even though the value may be writable in the JCR repository, this JDBC driver does not support writes. Therefore, this
     * method always returns true.
     * </p>
     * 
     * @see java.sql.ResultSetMetaData#isReadOnly(int)
     */
    @Override
    public boolean isReadOnly( int column ) {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * In JCR-SQL2, every property can be used in a WHERE clause. Therefore, this method always returns true.
     * </p>
     * 
     * @see java.sql.ResultSetMetaData#isSearchable(int)
     */
    @Override
    public boolean isSearchable( int column ) {
        return true;
    }

    /**
     * {@inheritDoc}
     * <p>
     * This method returns true if the column is a {@link PropertyType#DOUBLE}, {@link PropertyType#LONG} or
     * {@link PropertyType#DATE}.
     * </p>
     * 
     * @see java.sql.ResultSetMetaData#isSigned(int)
     */
    @Override
    public boolean isSigned( int column ) {
        return getJcrType(column).isSigned();
    }

    /**
     * {@inheritDoc}
     * <p>
     * Even though the value may be writable in the JCR repository, this JDBC driver does not support writes. Therefore, this
     * method always returns false.
     * </p>
     * 
     * @see java.sql.ResultSetMetaData#isWritable(int)
     */
    @Override
    public boolean isWritable( int column ) {
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Wrapper#isWrapperFor(java.lang.Class)
     */
    @Override
    public boolean isWrapperFor( Class<?> iface ) /*throws SQLException*/{
        return iface.isInstance(results);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.sql.Wrapper#unwrap(java.lang.Class)
     */
    @Override
    public <T> T unwrap( Class<T> iface ) throws SQLException {
        if (iface.isInstance(results)) {
            return iface.cast(results);
        }
        throw new SQLException(JdbcLocalI18n.classDoesNotImplementInterface.text(ResultSetMetaData.class.getSimpleName(),
                                                                                 iface.getName()));
    }

    private JcrType getJcrType( int column ) {
        JcrType typeInfo = null;
        if (results instanceof org.modeshape.jcr.api.query.QueryResult) {
            final String typeName = ((org.modeshape.jcr.api.query.QueryResult)results).getColumnTypes()[column - 1];
            typeInfo = JcrType.typeInfo(typeName);
        }
        /**
         * If no type is matched, then default to STRING
         */
        return (typeInfo != null ? typeInfo : JcrType.typeInfo(DefaultDataTypes.STRING));
    }

}
