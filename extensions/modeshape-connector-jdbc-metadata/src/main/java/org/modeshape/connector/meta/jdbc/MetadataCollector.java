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

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.util.List;

/**
 * The {@code MetadataCollector} provides hooks for DBMS-specific implementations of metadata retrieval methods. These methods
 * largely duplicate what is provided in {@link DatabaseMetaData the JDBC metadata interface}, but allow a pluggable way to work
 * around JDBC driver-specific shortcomings.
 * <p>
 * For convenience, a {@link JdbcMetadataCollector} default implementation} of this interface is provided that is based on
 * {@link DatabaseMetaData}.
 * </p>
 * 
 * @see JdbcMetadataCollector
 */
public interface MetadataCollector {

    /**
     * Return the list of catalog names that currently exist in the database to which {@code connection} is connected. The names
     * must be sorted in a manner that is stable between successive calls to this method.
     * 
     * @param conn the connection to the database; must not be non-null and must be open. This connection should not be closed by
     *        this method.
     * @return An ordered list of the catalogs in the database, or an empty list if no catalogs exist; may not be null
     * @throws JdbcMetadataException if the catalog names cannot be retrieved
     * @see DatabaseMetaData#getCatalogs()
     */
    List<String> getCatalogNames( Connection conn ) throws JdbcMetadataException;

    /**
     * Return the list of schema names that currently exist in the database to which {@code connection} is connected within the
     * named catalog. If {@code catalogName} is null, then all schema names should be returned regardless of the catalog with
     * which they are associated. The schema names must be sorted in a manner that is stable between successive calls to this
     * method.
     * 
     * @param conn the connection to the database; must not be non-null and must be open. This connection should not be closed by
     *        this method.
     * @param catalogName the name of the catalog to which returned schemas must belong, or null if all schemas are to be returned
     * @return An ordered list of the schemas in the database, or an empty list if no schemas exist; may not be null
     * @throws JdbcMetadataException if the schema names cannot be retrieved
     * @see DatabaseMetaData#getSchemas()
     */
    List<String> getSchemaNames( Connection conn,
                                 String catalogName ) throws JdbcMetadataException;

    /**
     * Return the list of tables that currently exist in the database to which {@code connection} is connected within the named
     * catalog (if {@code catalogName} is non-null) and named schema (if {@code schemaName} is non-null). If {@code tableName} is
     * null, then all tables which conform to the catalog and schema restriction noted previously should be returned. If {@code
     * tableName} is non-null, then only the table(s) that exactly match that name should be returned. The table metadata must be
     * sorted in a manner that is stable between successive calls to this method.
     * 
     * @param conn the connection to the database; must not be non-null and must be open. This connection should not be closed by
     *        this method.
     * @param catalogName the name of the catalog to which returned tables must belong, or null if tables are to be returned
     *        without regard to their catalog
     * @param schemaName the name of the schema to which returned tables must belong, or null if tables are to be returned without
     *        regard to their schema
     * @param tableName the name of the table to be returned, or null if all tables within the given catalog and schema are to be
     *        returned
     * @return An ordered list of the tables in the database that match the given constraints, or an empty list if no tables exist
     *         that match the given constraints; may not be null
     * @throws JdbcMetadataException if the table metadata cannot be retrieved
     * @see DatabaseMetaData#getTables(String, String, String, String[])
     */
    List<TableMetadata> getTables( Connection conn,
                                   String catalogName,
                                   String schemaName,
                                   String tableName ) throws JdbcMetadataException;

    /**
     * Return the list of columns that currently exist in the database to which {@code connection} is connected within the named
     * catalog (if {@code catalogName} is non-null), named schema (if {@code schemaName} is non-null), and named table. If {@code
     * columnName} is null, then all columns which conform to the catalog, schema, and table restrictions noted previously should
     * be returned. If {@code columnName} is non-null, then only the column that exactly matches that name should be returned. The
     * column metadata must be sorted in a manner that is stable between successive calls to this method.
     * 
     * @param conn the connection to the database; must not be non-null and must be open. This connection should not be closed by
     *        this method.
     * @param catalogName the name of the catalog to which returned columns must belong, or null if columns are to be returned
     *        without regard to their catalog
     * @param schemaName the name of the schema to which returned columns must belong, or null if columns are to be returned
     *        without regard to their schema
     * @param tableName the name of the table to which returned columns must belong; may not be null
     * @param columnName the name of the column to be returned, or null if all columns within the given catalog, schema, and table
     *        are to be returned
     * @return An ordered list of the columns in the database that match the given constraints, or an empty list if no columns
     *         exist that match the given constraints; may not be null
     * @throws JdbcMetadataException if the column metadata cannot be retrieved
     * @see DatabaseMetaData#getColumns(String, String, String, String)
     */
    List<ColumnMetadata> getColumns( Connection conn,
                                     String catalogName,
                                     String schemaName,
                                     String tableName,
                                     String columnName ) throws JdbcMetadataException;

    /**
     * Return the list of procedures that currently exist in the database to which {@code connection} is connected within the
     * named catalog (if {@code catalogName} is non-null) and named schema (if {@code schemaName} is non-null). If {@code
     * procedureName} is null, then all procedures which conform to the catalog and schema restriction noted previously should be
     * returned. If {@code procedureName} is non-null, then only the procedure(s) that exactly match that name should be returned.
     * The procedure metadata must be sorted in a manner that is stable between successive calls to this method.
     * 
     * @param conn the connection to the database; must not be non-null and must be open. This connection should not be closed by
     *        this method.
     * @param catalogName the name of the catalog to which returned procedures must belong, or null if procedures are to be
     *        returned without regard to their catalog
     * @param schemaName the name of the schema to which returned procedures must belong, or null if procedures are to be returned
     *        without regard to their schema
     * @param procedureName the name of the procedure(s) to be returned, or null if all procedures within the given catalog and
     *        schema are to be returned
     * @return An ordered list of the procedures in the database that match the given constraints, or an empty list if no
     *         procedures exist that match the given constraints; may not be null
     * @throws JdbcMetadataException if the procedure metadata cannot be retrieved
     */
    List<ProcedureMetadata> getProcedures( Connection conn,
                                           String catalogName,
                                           String schemaName,
                                           String procedureName ) throws JdbcMetadataException;

}
