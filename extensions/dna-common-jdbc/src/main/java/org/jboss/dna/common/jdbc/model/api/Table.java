/*
 * JBoss DNA (http://www.jboss.org/dna)
 * See the COPYRIGHT.txt file distributed with this work for information
 * regarding copyright ownership.  Some portions may be licensed
 * to Red Hat, Inc. under one or more contributor license agreements.
 * See the AUTHORS.txt file in the distribution for a full listing of 
 * individual contributors. 
 *
 * JBoss DNA is free software. Unless otherwise indicated, all code in JBoss DNA
 * is licensed to you under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * JBoss DNA is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.common.jdbc.model.api;

import java.util.Set;

/**
 * Provides all core database table specific metadata.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public interface Table extends SchemaObject {

    /**
     * Returns type of table such as: "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
     * 
     * @return type of table.
     */
    TableType getTableType();

    /**
     * Sets type of table such as: "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
     * 
     * @param tableType the type of table.
     */
    void setTableType( TableType tableType );

    /**
     * Gets type catalog
     * 
     * @return types catalog (may be <code>null</code>)
     */
    Catalog getTypeCatalog();

    /**
     * Sets type catalog
     * 
     * @param typeCatalog the types catalog (may be <code>null</code>)
     */
    void setTypeCatalog( Catalog typeCatalog );

    /**
     * Gets type schema
     * 
     * @return types schema (may be <code>null</code>)
     */
    Schema getTypeSchema();

    /**
     * Sets type schema
     * 
     * @param typeSchema the types schema (may be <code>null</code>)
     */
    void setTypeSchema( Schema typeSchema );

    /**
     * Gets type name
     * 
     * @return types name (may be <code>null</code>)
     */
    String getTypeName();

    /**
     * Sets type name
     * 
     * @param typeName types name (may be <code>null</code>)
     */
    void setTypeName( String typeName );

    /**
     * Gets name of the designated "identifier" column of a typed table (may be <code>null</code>)
     * 
     * @return name of the designated "identifier" column of a typed table (may be <code>null</code>)
     */
    String getSelfReferencingColumnName();

    /**
     * Sets name of the designated "identifier" column of a typed table (may be <code>null</code>)
     * 
     * @param selfReferencingColumnName the name of the designated "identifier" column of a typed table (may be <code>null</code>)
     */
    void setSelfReferencingColumnName( String selfReferencingColumnName );

    /**
     * specifies how values in getSelfReferencingColumnName () are created. Values are "SYSTEM", "USER", "DERIVED". (may be
     * <code>null</code>)
     * 
     * @return how values in getSelfReferencingColumnName () are created.
     */
    String getReferenceGeneration();

    /**
     * specifies how values in getSelfReferencingColumnName () are created. Values are "SYSTEM", "USER", "DERIVED". (may be
     * <code>null</code>)
     * 
     * @param referenceGeneration how values in getSelfReferencingColumnName () are created.
     */
    void setReferenceGeneration( String referenceGeneration );

    /**
     * Gets a set of table columns
     * 
     * @return a set of table columns.
     */
    Set<TableColumn> getColumns();

    /**
     * Adds TableColumn
     * 
     * @param column the TableColumn
     */
    void addColumn( TableColumn column );

    /**
     * deletes TableColumn
     * 
     * @param column the TableColumn
     */
    void deleteColumn( TableColumn column );

    /**
     * Returns table column for specified column name or null
     * 
     * @param columnName the name of column
     * @return table column for specified column name or null.
     */
    TableColumn findColumnByName( String columnName );

    /**
     * Gets a table primary key
     * 
     * @return a table primary key.
     */
    PrimaryKey getPrimaryKey();

    /**
     * Sets a table primary key
     * 
     * @param primaryKey the table primary key.
     */
    void setPrimaryKey( PrimaryKey primaryKey );

    /**
     * Gets a set of table foreign key columns
     * 
     * @return a set of table foreign keys.
     */
    Set<ForeignKey> getForeignKeys();

    /**
     * adds ForeignKey
     * 
     * @param foreignKey the ForeignKey
     */
    void addForeignKey( ForeignKey foreignKey );

    /**
     * deletes ForeignKey
     * 
     * @param foreignKey the ForeignKey
     */
    void deleteForeignKey( ForeignKey foreignKey );

    /**
     * Returns table foreign key for specified name or null
     * 
     * @param fkName the name of foreign key
     * @return table foreign key for specified name or null.
     */
    ForeignKey findForeignKeyByName( String fkName );

    /**
     * Gets a set of table indexes
     * 
     * @return a set of table indexes.
     */
    Set<Index> getIndexes();

    /**
     * adds Index
     * 
     * @param index the Index
     */
    void addIndex( Index index );

    /**
     * deletes Index
     * 
     * @param index the Index
     */
    void deleteIndex( Index index );

    /**
     * Returns table index for specified name or null
     * 
     * @param indexName the name of index
     * @return table index for specified name or null.
     */
    Index findIndexByName( String indexName );

    /**
     * Gets a set of table version columns
     * 
     * @return a set of table version columns.
     */
    Set<TableColumn> getVersionColumns();

    /**
     * adds version column
     * 
     * @param tableColumn the TableColumn
     */
    void addVersionColumn( TableColumn tableColumn );

    /**
     * deletes version column
     * 
     * @param tableColumn the version column
     */
    void deleteVersionColumn( TableColumn tableColumn );

    /**
     * Returns table version column for specified name or null
     * 
     * @param columnName the name of Version Column
     * @return table Version Column for specified name or null.
     */
    TableColumn findVersionColumnByName( String columnName );

    /**
     * Gets table privileges.
     * 
     * @return set of table privileges
     */
    Set<Privilege> getPrivileges();

    /**
     * Adds table priviledge
     * 
     * @param privilege the table priviledge
     */
    void addPrivilege( Privilege privilege );

    /**
     * Deletes table priviledge
     * 
     * @param privilege the table priviledge
     */
    void deletePrivilege( Privilege privilege );

    /**
     * Searches priviledge by name
     * 
     * @param priviledgeName the priviledge name to search
     * @return priviledge if found, otherwise return null
     */
    Privilege findPriviledgeByName( String priviledgeName );

    /**
     * Retrieves a set of descriptions of a table's optimal set of columns that uniquely identifies a row in temporary scopes.
     * 
     * @return BestRowIdentifier set that uniquely identifies a row in scopes.
     */
    Set<BestRowIdentifier> getBestRowIdentifiers();

    /**
     * Adds BestRowIdentifier
     * 
     * @param bestRowIdentifier the BestRowIdentifier
     */
    void addBestRowIdentifier( BestRowIdentifier bestRowIdentifier );

    /**
     * deletes BestRowIdentifier
     * 
     * @param bestRowIdentifier the BestRowIdentifier
     */
    void deleteBestRowIdentifier( BestRowIdentifier bestRowIdentifier );

    /**
     * Searches the BestRowIdentifier by scope
     * 
     * @param scopeType the scope of best row identifier
     * @return BestRowIdentifier if any
     */
    BestRowIdentifier findBestRowIdentifierByScopeType( BestRowIdentifierScopeType scopeType );

    // ===============================================================
    // ------------------- JDBC 3.0 ---------------------------------
    // ===============================================================

    /**
     * Retrieves a description of the table hierarchies defined in a particular schema in this database. Only the immediate super
     * type/ sub type relationship is modeled.
     * 
     * @return super table for this table
     * @since 1.4 (JDBC 3.0)
     */
    Table getSuperTable();

    /**
     * Sets a description of the table hierarchies defined in a particular schema in this database. Only the immediate super type/
     * sub type relationship is modeled.
     * 
     * @param superTable the super table for this table
     * @since 1.4 (JDBC 3.0)
     */
    void setSuperTable( Table superTable );
}
