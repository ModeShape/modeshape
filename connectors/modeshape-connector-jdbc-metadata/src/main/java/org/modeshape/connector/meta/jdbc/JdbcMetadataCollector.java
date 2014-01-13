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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.logging.Logger;
import org.modeshape.common.util.CheckArg;

/**
 * Default {@link MetadataCollector} implementation that uses the {@link java.sql.DatabaseMetaData built-in JDBC support} for collecting
 * database metadata.
 *
 * @see java.sql.DatabaseMetaData
 */
@Immutable
public class JdbcMetadataCollector implements MetadataCollector {
    private static final Logger LOG = Logger.getLogger(JdbcMetadataCollector.class);

    @Override
    public DBMetadata getDatabaseMetadata( Connection conn ) throws JdbcMetadataException {
        CheckArg.isNotNull(conn, "Database connection");
        try {
            DatabaseMetaData metaData = conn.getMetaData();
            return new DBMetadata(metaData.getDatabaseProductName(), metaData.getDatabaseProductVersion(),
                                  metaData.getDriverMajorVersion(), metaData.getDatabaseMinorVersion());
        } catch (SQLException e) {
            throw new JdbcMetadataException(e);
        }
    }

    @Override
    public List<String> getCatalogNames( Connection conn ) throws JdbcMetadataException {
        CheckArg.isNotNull(conn, "Database connection");

        List<String> catalogNames = new LinkedList<String>();

        ResultSet catalogs = null;
        try {
            DatabaseMetaData dmd = conn.getMetaData();
            catalogs = dmd.getCatalogs();
            while (catalogs.next()) {
                catalogNames.add(catalogs.getString("TABLE_CAT"));
            }
            return catalogNames;
        } catch (SQLException se) {
            throw new JdbcMetadataException(se);
        } finally {
            try {
                if (catalogs != null) {
                    catalogs.close();
                }
            } catch (Exception ignore) {
                LOG.debug(ignore, "Cannot close catalogs result set");
            }
        }
    }

    @Override
    public List<ColumnMetadata> getColumns( Connection conn,
                                            String catalogName,
                                            String schemaName,
                                            String tableName,
                                            String columnName ) throws JdbcMetadataException {
        CheckArg.isNotNull(tableName, "Table name");

        List<ColumnMetadata> columnData = new LinkedList<ColumnMetadata>();

        ResultSet columns = null;
        try {
            DatabaseMetaData dmd = conn.getMetaData();
            // Adjust default values of catalogName and schemaName to the "match tables with no catalog/schema" pattern
            if (catalogName == null) {
                catalogName = "";
            }
            if (schemaName == null) {
                schemaName = "";
            }
            columns = dmd.getColumns(catalogName, schemaName, tableName, columnName);
            // Get the list of names of the columns in the result set, which may or may not match what's in the spec
            Set<String> columnNames = columnsFor(columns);
            while (columns.next()) {
                ColumnMetadata column = new ColumnMetadata(columns.getString("COLUMN_NAME"),
                                                           columns.getInt("DATA_TYPE"),
                                                           columns.getString("TYPE_NAME"),
                                                           columns.getInt("COLUMN_SIZE"),
                                                           columns.getInt("DECIMAL_DIGITS"),
                                                           columns.getInt("NUM_PREC_RADIX"),
                                                           getNullableBoolean(columns, "NULLABLE"),
                                                           getStringIfPresent(columns, "REMARKS", columnNames),
                                                           getStringIfPresent(columns, "COLUMN_DEF", columnNames),
                                                           columns.getInt("CHAR_OCTET_LENGTH"),
                                                           columns.getInt("ORDINAL_POSITION"),
                                                           getStringIfPresent(columns, "SCOPE_CATLOG", columnNames),
                                                           getStringIfPresent(columns, "SCOPE_SCHEMA", columnNames),
                                                           getStringIfPresent(columns, "SCOPE_TABLE", columnNames),
                                                           getIntegerIfPresent(columns, "SOURCE_DATA_TYPE", columnNames));
                columnData.add(column);
            }

            return columnData;
        } catch (SQLException se) {
            throw new JdbcMetadataException(se);
        } finally {
            try {
                if (columns != null) {
                    columns.close();
                }
            } catch (Exception ignore) {
                LOG.debug(ignore, "Cannot close columns result set");
            }
        }
    }

    @Override
    public List<String> getSchemaNames( Connection conn,
                                        String catalogName ) throws JdbcMetadataException {
        List<String> schemaNames = new LinkedList<String>();

        ResultSet schemas = null;
        try {
            DatabaseMetaData dmd = conn.getMetaData();
            schemas = dmd.getSchemas();

            Set<String> columns = columnsFor(schemas);
            boolean hasCatalog = columns.contains(identifierFor(dmd, "TABLE_CATALOG"));

            while (schemas.next()) {

                /*
                 * PostgreSQL's JDBC3 driver doesn't include TABLE_CATALOG
                 */
                if (hasCatalog) {
                    String catalogNameForSchema = schemas.getString("TABLE_CATALOG");
                    String schemaName = schemas.getString("TABLE_SCHEM");
                    if ((catalogName == null && catalogNameForSchema == null)
                            || (catalogName != null && catalogName.equals(catalogNameForSchema))) {
                        schemaNames.add(schemaName);
                    }
                } else {
                    schemaNames.add(schemas.getString("TABLE_SCHEM"));
                }
            }

            return schemaNames;
        } catch (SQLException se) {
            throw new JdbcMetadataException(se);
        } finally {
            try {
                if (schemas != null) {
                    schemas.close();
                }
            } catch (Exception ignore) {
                LOG.debug(ignore, "Cannot close schemas result set");
            }
        }
    }

    @Override
    public List<TableMetadata> getTables( Connection conn,
                                          String catalogName,
                                          String schemaName,
                                          String tableName ) throws JdbcMetadataException {
        List<TableMetadata> tableData = new LinkedList<TableMetadata>();

        ResultSet tables = null;
        try {
            DatabaseMetaData dmd = conn.getMetaData();

            // Adjust default values of catalogName and schemaName to the "match tables with no catalog/schema" pattern
            if (catalogName == null) {
                catalogName = "";
            }
            if (schemaName == null) {
                schemaName = "";
            }

            tables = dmd.getTables(catalogName, schemaName, tableName, null);
            Set<String> columns = columnsFor(tables);
            while (tables.next()) {
                TableMetadata table = new TableMetadata(tables.getString("TABLE_NAME"),
                                                        tables.getString("TABLE_TYPE"),
                                                        tables.getString("REMARKS"),
                                                        getStringIfPresent(tables, "TYPE_CAT", columns),
                                                        getStringIfPresent(tables, "TYPE_SCHEM", columns),
                                                        getStringIfPresent(tables, "TYPE_NAME", columns),
                                                        getStringIfPresent(tables, "SELF_REFERENCING_COL_NAME", columns),
                                                        getStringIfPresent(tables, "REF_GENERATION", columns));
                tableData.add(table);
            }

            return tableData;
        } catch (SQLException se) {
            throw new JdbcMetadataException(se);
        } finally {
            try {
                if (tables != null) {
                    tables.close();
                }
            } catch (Exception ignore) {
                LOG.debug(ignore, "Cannot close tables result set");
            }
        }
    }

    @Override
    public List<ProcedureMetadata> getProcedures( Connection conn,
                                                  String catalogName,
                                                  String schemaName,
                                                  String procedureName ) throws JdbcMetadataException {
        List<ProcedureMetadata> procedureData = new LinkedList<ProcedureMetadata>();

        ResultSet procedures = null;
        try {
            DatabaseMetaData dmd = conn.getMetaData();

            // Adjust default values of catalogName and schemaName to the "match tables with no catalog/schema" pattern
            if (catalogName == null) {
                catalogName = "";
            }
            if (schemaName == null) {
                schemaName = "";
            }
            procedures = dmd.getProcedures(catalogName, schemaName, procedureName);
            while (procedures.next()) {
                ProcedureMetadata procedure = new ProcedureMetadata(procedures.getString("PROCEDURE_NAME"),
                                                                    procedures.getString("REMARKS"),
                                                                    procedures.getShort("PROCEDURE_TYPE"));
                procedureData.add(procedure);

            }

            return procedureData;
        } catch (SQLException se) {
            throw new JdbcMetadataException(se);
        } finally {
            try {
                if (procedures != null) {
                    procedures.close();
                }
            } catch (Exception ignore) {
            }
        }
    }

    @Override
    public List<ForeignKeyMetadata> getForeignKeys( Connection conn,
                                                    String catalogName,
                                                    String schemaName,
                                                    String tableName,
                                                    String fkColumnName ) throws JdbcMetadataException {
        CheckArg.isNotNull(tableName, "Table name");

        List<ForeignKeyMetadata> foreignKeyMetadata = new LinkedList<ForeignKeyMetadata>();

        ResultSet foreignKeys = null;
        try {
            DatabaseMetaData dmd = conn.getMetaData();
            // Adjust default values of catalogName and schemaName to the "match tables with no catalog/schema" pattern
            if (catalogName == null) {
                catalogName = "";
            }
            if (schemaName == null) {
                schemaName = "";
            }
            foreignKeys = dmd.getImportedKeys(catalogName, schemaName, tableName);
            Set<String> columnNames = columnsFor(foreignKeys);
            while (foreignKeys.next()) {
                String colName = foreignKeys.getString("FKCOLUMN_NAME");
                if (fkColumnName != null && !fkColumnName.equalsIgnoreCase(colName)) {
                    continue;
                }
                ForeignKeyMetadata foreignKey = new ForeignKeyMetadata(
                        getStringIfPresent(foreignKeys, "PKTABLE_CAT", columnNames),
                        getStringIfPresent(foreignKeys, "PKTABLE_SCHEM ", columnNames),
                        foreignKeys.getString("PKTABLE_NAME"),
                        foreignKeys.getString("PKCOLUMN_NAME"),
                        getStringIfPresent(foreignKeys, "FKTABLE_CAT", columnNames),
                        getStringIfPresent(foreignKeys, "FKTABLE_SCHEM", columnNames),
                        foreignKeys.getString("FKTABLE_NAME"),
                        colName,
                        foreignKeys.getShort("KEY_SEQ"),
                        foreignKeys.getShort("UPDATE_RULE"),
                        foreignKeys.getShort("DELETE_RULE"),
                        getStringIfPresent(foreignKeys, "FK_NAME", columnNames),
                        getStringIfPresent(foreignKeys, "PK_NAME", columnNames),
                        foreignKeys.getShort("DEFERRABILITY"));
                foreignKeyMetadata.add(foreignKey);
            }

            return foreignKeyMetadata;
        } catch (SQLException se) {
            throw new JdbcMetadataException(se);
        } finally {
            try {
                if (foreignKeys != null) {
                    foreignKeys.close();
                }
            } catch (Exception ignore) {
                LOG.debug(ignore, "Cannot close foreignKeys result set");
            }
        }
    }

    private Boolean getNullableBoolean( ResultSet rs,
                                        String columnName ) throws SQLException {
        Boolean b = rs.getBoolean(columnName);
        if (rs.wasNull()) {
            b = null;
        }
        return b;
    }

    private Set<String> columnsFor( ResultSet rs ) throws SQLException {
        ResultSetMetaData rmd = rs.getMetaData();
        int count = rmd.getColumnCount();

        Set<String> columns = new HashSet<String>(count);
        for (int i = 1; i <= count; i++) {
            columns.add(rmd.getColumnName(i));
        }
        return columns;
    }

    private String getStringIfPresent( ResultSet rs,
                                       String columnName,
                                       Set<String> resultSetColumns ) throws SQLException {
        if (!resultSetColumns.contains(columnName)) {
            return null;
        }

        return rs.getString(columnName);

    }

    private Integer getIntegerIfPresent( ResultSet rs,
                                         String columnName,
                                         Set<String> resultSetColumns ) throws SQLException {
        if (!resultSetColumns.contains(columnName)) {
            return null;
        }

        int i = rs.getInt(columnName);
        if (rs.wasNull()) {
            return null;
        }
        return i;

    }

    private String identifierFor( DatabaseMetaData dmd,
                                  String rawIdentifier ) throws SQLException {
        assert rawIdentifier != null;
        if (dmd.storesLowerCaseIdentifiers()) {
            return rawIdentifier.toLowerCase();
        }

        return rawIdentifier;
    }
}
