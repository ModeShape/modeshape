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
package org.modeshape.connector.meta.jdbc;

import java.sql.DatabaseMetaData;
import java.sql.Types;
import net.jcip.annotations.Immutable;

/**
 * Container for column-level metadata. The fields in this class roughly parallel the information returned from the
 * {@link DatabaseMetaData#getColumns(String, String, String, String)} method.
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

    public ColumnMetadata( String name,
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
     * @return the column name (COLUMN_NAME in the {@link DatabaseMetaData#getColumns(String, String, String, String)} result
     *         set).
     */
    public String getName() {
        return name;
    }

    /**
     * @return the JDBC data type (from {@link Types}) (DATA_TYPE in the
     *         {@link DatabaseMetaData#getColumns(String, String, String, String)} result set).
     */
    public int getJdbcDataType() {
        return jdbcDataType;
    }

    /**
     * @return the database-dependent type name (TYPENAME in the
     *         {@link DatabaseMetaData#getColumns(String, String, String, String)} result set).
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * @return the column size (length for character data types, precision for numeric data types) (COLUMN_SIZE in the
     *         {@link DatabaseMetaData#getColumns(String, String, String, String)} result set).
     */
    public int getColumnSize() {
        return columnSize;
    }

    /**
     * @return the number of fractional digits in the column (DECIMAL_DIGITS in the
     *         {@link DatabaseMetaData#getColumns(String, String, String, String)} result set).
     */
    public int getDecimalDigits() {
        return decimalDigits;
    }

    /**
     * @return the radix for the column (NUM_PREC_RADIX in the {@link DatabaseMetaData#getColumns(String, String, String, String)}
     *         result set).
     */
    public int getRadix() {
        return radix;
    }

    /**
     * @return whether the column allows NULLs or not (NULLABLE in the
     *         {@link DatabaseMetaData#getColumns(String, String, String, String)} result set); null indicates that it cannot be
     *         determined if the column allows NULLs.
     */
    public Boolean getNullable() {
        return nullable;
    }

    /**
     * @return the column description (REMARKS in the {@link DatabaseMetaData#getColumns(String, String, String, String)} result
     *         set).
     */
    public String getDescription() {
        return description;
    }

    /**
     * @return the default value for the column, if any (COLUMN_DEF in the
     *         {@link DatabaseMetaData#getColumns(String, String, String, String)} result set).
     */
    public String getDefaultValue() {
        return defaultValue;
    }

    /**
     * @return the number of bytes in the column (for char types) (CHAR_OCTECT_LENGTH in the
     *         {@link DatabaseMetaData#getColumns(String, String, String, String)} result set).
     */
    public int getLength() {
        return length;
    }

    /**
     * @return the 1-based index of the column within its table (ORDINAL_POSITION in the
     *         {@link DatabaseMetaData#getColumns(String, String, String, String)} result set).
     */
    public int getOrdinalPosition() {
        return ordinalPosition;
    }

    /**
     * @return for columns of type REF, the catalog name of the target table (SCOPE_CATALOG in the
     *         {@link DatabaseMetaData#getColumns(String, String, String, String)} result set).
     */
    public String getScopeCatalogName() {
        return scopeCatalogName;
    }

    /**
     * @return for columns of type REF, the schema name of the target table (SCOPE_SCHEMA in the
     *         {@link DatabaseMetaData#getColumns(String, String, String, String)} result set).
     */
    public String getScopeSchemaName() {
        return scopeSchemaName;
    }

    /**
     * @return for columns of type REF, the name of the target table (SCOPE_TABLE in the
     *         {@link DatabaseMetaData#getColumns(String, String, String, String)} result set).
     */
    public String getScopeTableName() {
        return scopeTableName;
    }

    /**
     * @return the source type of the referred type (from {@link Types}) for REF columns (SOURCE_DATA_TYPE in the
     *         {@link DatabaseMetaData#getColumns(String, String, String, String)} result set).
     */
    public Integer getSourceJdbcDataType() {
        return sourceJdbcDataType;
    }

}
