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
package org.modeshape.jdbc.metadata;

import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import org.modeshape.jdbc.JcrType;
import org.modeshape.jdbc.JdbcLocalI18n;

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

    @Override
    public int getColumnCount() {
        return provider.getColumnCount();
    }

    @Override
    public boolean isAutoIncrement( int index ) {
        return provider.getBooleanValue(adjustColumn(index), ResultsMetadataConstants.AUTO_INCREMENTING);
    }

    @Override
    public boolean isCaseSensitive( int index ) {
        return provider.getBooleanValue(adjustColumn(index), ResultsMetadataConstants.CASE_SENSITIVE);
    }

    @Override
    public boolean isSearchable( int index ) {
        Integer searchable = (Integer)provider.getValue(adjustColumn(index), ResultsMetadataConstants.SEARCHABLE);
        return !(ResultsMetadataConstants.SEARCH_TYPES.UNSEARCHABLE.equals(searchable));
    }

    @Override
    public boolean isCurrency( int index ) {
        return provider.getBooleanValue(adjustColumn(index), ResultsMetadataConstants.CURRENCY);
    }

    @Override
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

    @Override
    public boolean isSigned( int index ) {
        return provider.getBooleanValue(adjustColumn(index), ResultsMetadataConstants.SIGNED);
    }

    @Override
    public int getColumnDisplaySize( int index ) {
        return provider.getIntValue(adjustColumn(index), ResultsMetadataConstants.DISPLAY_SIZE);
    }

    @Override
    public String getColumnLabel( int index ) {
        return provider.getStringValue(adjustColumn(index), ResultsMetadataConstants.COLUMN_LABEL);
    }

    @Override
    public String getColumnName( int index ) {
        return provider.getStringValue(adjustColumn(index), ResultsMetadataConstants.COLUMN);
    }

    @Override
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

    @Override
    public int getPrecision( int index ) {
        return provider.getIntValue(adjustColumn(index), ResultsMetadataConstants.PRECISION);
    }

    @Override
    public int getScale( int index ) {
        return provider.getIntValue(adjustColumn(index), ResultsMetadataConstants.SCALE);
    }

    @Override
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

    @Override
    public String getCatalogName( int index ) {
        return provider.getStringValue(adjustColumn(index), ResultsMetadataConstants.CATALOG);
    }

    @Override
    public int getColumnType( int index ) {
        String runtimeTypeName = provider.getStringValue(adjustColumn(index), ResultsMetadataConstants.DATA_TYPE);

        JcrType typeInfo = JcrType.typeInfo(runtimeTypeName);
        return typeInfo != null ? typeInfo.getJdbcType() : Types.VARCHAR;
    }

    @Override
    public String getColumnTypeName( int index ) {
        return provider.getStringValue(adjustColumn(index), ResultsMetadataConstants.DATA_TYPE);
    }

    @Override
    public boolean isReadOnly( int index ) {
        return !provider.getBooleanValue(adjustColumn(index), ResultsMetadataConstants.WRITABLE);
    }

    @Override
    public boolean isWritable( int index ) {
        return provider.getBooleanValue(adjustColumn(index), ResultsMetadataConstants.WRITABLE);
    }

    @Override
    public boolean isDefinitelyWritable( int index ) {
        return provider.getBooleanValue(adjustColumn(index), ResultsMetadataConstants.WRITABLE);
    }

    @Override
    public String getColumnClassName( int index ) {
        JcrType typeInfo = JcrType.typeInfo(getColumnTypeName(index));
        return typeInfo != null ? typeInfo.getRepresentationClass().getName() : String.class.getName();
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
