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
package org.modeshape.connector.meta.jdbc;

import org.modeshape.common.annotation.Immutable;

/**
 * Container for column-level metadata. The fields in this class roughly parallel the information returned from the
 * {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)} method.
 */
@Immutable
public class ColumnMetadata {

    private final String name;
    private final int jdbcDataType;
    private final String typeName;
    private final int columnSize;
    private final int decimalDigits;
    private final int radix;
    private final Boolean nullable;
    private final String description;
    private final String defaultValue;
    private final int length;
    private final int ordinalPosition;
    private final String scopeCatalogName;
    private final String scopeSchemaName;
    private final String scopeTableName;
    private final Integer sourceJdbcDataType;

    protected ColumnMetadata( String name,
                              int jdbcDataType,
                              String typeName,
                              int columnSize,
                              int decimalDigits,
                              int radix,
                              Boolean nullable,
                              String description,
                              String defaultValue,
                              int length,
                              int ordinalPosition,
                              String scopeCatalogName,
                              String scopeSchemaName,
                              String scopeTableName,
                              Integer sourceJdbcDataType ) {
        super();
        this.name = name;
        this.jdbcDataType = jdbcDataType;
        this.typeName = typeName;
        this.columnSize = columnSize;
        this.decimalDigits = decimalDigits;
        this.radix = radix;
        this.nullable = nullable;
        this.description = description;
        this.defaultValue = defaultValue;
        this.length = length;
        this.ordinalPosition = ordinalPosition;
        this.scopeCatalogName = scopeCatalogName;
        this.scopeSchemaName = scopeSchemaName;
        this.scopeTableName = scopeTableName;
        this.sourceJdbcDataType = sourceJdbcDataType;
    }

    /**
     * @return the column name (COLUMN_NAME in the {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)} result
     *         set).
     */
    public String getName() {
        return name;
    }

    /**
     * @return the JDBC data type (from {@link java.sql.Types}) (DATA_TYPE in the
     *         {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)} result set).
     */
    public int getJdbcDataType() {
        return jdbcDataType;
    }

    /**
     * @return the database-dependent type name (TYPENAME in the
     *         {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)} result set).
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * @return the column size (length for character data types, precision for numeric data types) (COLUMN_SIZE in the
     *         {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)} result set).
     */
    public int getColumnSize() {
        return columnSize;
    }

    /**
     * @return the number of fractional digits in the column (DECIMAL_DIGITS in the
     *         {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)} result set).
     */
    public int getDecimalDigits() {
        return decimalDigits;
    }

    /**
     * @return the radix for the column (NUM_PREC_RADIX in the {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)}
     *         result set).
     */
    public int getRadix() {
        return radix;
    }

    /**
     * @return whether the column allows NULLs or not (NULLABLE in the
     *         {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)} result set); null indicates that it cannot be
     *         determined if the column allows NULLs.
     */
    public Boolean getNullable() {
        return nullable;
    }

    /**
     * @return the column description (REMARKS in the {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)} result
     *         set).
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the default value for the column, if any (COLUMN_DEF in the
     *         {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)} result set).
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * @return the number of bytes in the column (for char types) (CHAR_OCTECT_LENGTH in the
     *         {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)} result set).
     */
    public int getLength() {
        return length;
    }

    /**
     * @return the 1-based index of the column within its table (ORDINAL_POSITION in the
     *         {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)} result set).
     */
    public int getOrdinalPosition() {
        return ordinalPosition;
    }

    /**
     * @return for columns of type REF, the catalog name of the target table (SCOPE_CATALOG in the
     *         {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)} result set).
     */
    public String getScopeCatalogName() {
        return scopeCatalogName;
    }

    /**
     * @return for columns of type REF, the schema name of the target table (SCOPE_SCHEMA in the
     *         {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)} result set).
     */
    public String getScopeSchemaName() {
        return scopeSchemaName;
    }

    /**
     * @return for columns of type REF, the name of the target table (SCOPE_TABLE in the
     *         {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)} result set).
     */
    public String getScopeTableName() {
        return scopeTableName;
    }

    /**
     * @return the source type of the referred type (from {@link java.sql.Types}) for REF columns (SOURCE_DATA_TYPE in the
     *         {@link java.sql.DatabaseMetaData#getColumns(String, String, String, String)} result set).
     */
    public Integer getSourceJdbcDataType() {
        return sourceJdbcDataType;
    }

}
