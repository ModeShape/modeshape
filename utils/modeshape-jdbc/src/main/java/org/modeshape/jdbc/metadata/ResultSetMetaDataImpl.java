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
package org.modeshape.jdbc.metadata;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import org.modeshape.jdbc.JcrType;
import org.modeshape.jdbc.JdbcI18n;

/**
 * 
 */
public class ResultSetMetaDataImpl implements ResultSetMetaData {

    private MetadataProvider provider;

    public ResultSetMetaDataImpl( MetadataProvider provider ) {
        this.provider = provider;
    }

    /**
     * Adjust from 1-based to internal 0-based representation
     * 
     * @param index External 1-based representation
     * @return Internal 0-based representation
     */
    private int adjustColumn( int index ) {
        return index - 1;
    }

    public int getColumnCount() {
        return provider.getColumnCount();
    }

    public boolean isAutoIncrement( int index ) {
        return provider.getBooleanValue(adjustColumn(index), ResultsMetadataConstants.AUTO_INCREMENTING);
    }

    public boolean isCaseSensitive( int index ) {
        return provider.getBooleanValue(adjustColumn(index), ResultsMetadataConstants.CASE_SENSITIVE);
    }

    public boolean isSearchable( int index ) {
        Integer searchable = (Integer)provider.getValue(adjustColumn(index), ResultsMetadataConstants.SEARCHABLE);
        return !(ResultsMetadataConstants.SEARCH_TYPES.UNSEARCHABLE.equals(searchable));
    }

    public boolean isCurrency( int index ) {
        return provider.getBooleanValue(adjustColumn(index), ResultsMetadataConstants.CURRENCY);
    }

    public int isNullable( int index ) {
        Object nullable = provider.getValue(adjustColumn(index), ResultsMetadataConstants.NULLABLE);
        if (nullable.equals(ResultsMetadataConstants.NULL_TYPES.NULLABLE)) {
            return columnNullable;
        } else if (nullable.equals(ResultsMetadataConstants.NULL_TYPES.NOT_NULL)) {
            return columnNoNulls;
        } else {
            return columnNullableUnknown;
        }
    }

    public boolean isSigned( int index ) {
        return provider.getBooleanValue(adjustColumn(index), ResultsMetadataConstants.SIGNED);
    }

    public int getColumnDisplaySize( int index ) {
        return provider.getIntValue(adjustColumn(index), ResultsMetadataConstants.DISPLAY_SIZE);
    }

    public String getColumnLabel( int index ) {
        return provider.getStringValue(adjustColumn(index), ResultsMetadataConstants.COLUMN_LABEL);
    }

    public String getColumnName( int index ) {
        return provider.getStringValue(adjustColumn(index), ResultsMetadataConstants.COLUMN);
    }

    public String getSchemaName( int index ) {
        String name = provider.getStringValue(adjustColumn(index), ResultsMetadataConstants.SCHEMA);
        if (name != null) {
            int dotIndex = name.indexOf('.');
            if (dotIndex != -1) {
                return name.substring(0, dotIndex);
            }
        }
        return null;
    }

    public int getPrecision( int index ) {
        return provider.getIntValue(adjustColumn(index), ResultsMetadataConstants.PRECISION);
    }

    public int getScale( int index ) {
        return provider.getIntValue(adjustColumn(index), ResultsMetadataConstants.SCALE);
    }

    public String getTableName( int index ) {
        String name = provider.getStringValue(adjustColumn(index), ResultsMetadataConstants.TABLE);
        if (name != null) {
            int dotIndex = name.indexOf('.');
            if (dotIndex != -1) {
                return name.substring(dotIndex + 1);
            }
        }
        return name;
    }

    public String getCatalogName( int index ) {
        return provider.getStringValue(adjustColumn(index), ResultsMetadataConstants.CATALOG);
    }

    public int getColumnType( int index ) {
        String runtimeTypeName = provider.getStringValue(adjustColumn(index), ResultsMetadataConstants.DATA_TYPE);

        JcrType typeInfo = JcrType.typeInfo(runtimeTypeName);
        return typeInfo != null ? typeInfo.getJdbcType() : Types.VARCHAR;
    }

    public String getColumnTypeName( int index ) {
        return provider.getStringValue(adjustColumn(index), ResultsMetadataConstants.DATA_TYPE);
    }

    public boolean isReadOnly( int index ) {
        return !provider.getBooleanValue(adjustColumn(index), ResultsMetadataConstants.WRITABLE);
    }

    public boolean isWritable( int index ) {
        return provider.getBooleanValue(adjustColumn(index), ResultsMetadataConstants.WRITABLE);
    }

    public boolean isDefinitelyWritable( int index ) {
        return provider.getBooleanValue(adjustColumn(index), ResultsMetadataConstants.WRITABLE);
    }

    public String getColumnClassName( int index ) {
        JcrType typeInfo = JcrType.typeInfo(getColumnTypeName(index));
        return typeInfo != null ? typeInfo.getRepresentationClass().getName() : String.class.getName();
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
