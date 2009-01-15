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
package org.jboss.dna.common.jdbc.model.spi;

import java.util.Set;
import java.util.HashSet;
import org.jboss.dna.common.jdbc.model.api.BestRowIdentifier;
import org.jboss.dna.common.jdbc.model.api.BestRowIdentifierScopeType;
import org.jboss.dna.common.jdbc.model.api.Catalog;
import org.jboss.dna.common.jdbc.model.api.ForeignKey;
import org.jboss.dna.common.jdbc.model.api.Index;
import org.jboss.dna.common.jdbc.model.api.PrimaryKey;
import org.jboss.dna.common.jdbc.model.api.Privilege;
import org.jboss.dna.common.jdbc.model.api.Schema;
import org.jboss.dna.common.jdbc.model.api.Table;
import org.jboss.dna.common.jdbc.model.api.TableColumn;
import org.jboss.dna.common.jdbc.model.api.TableType;

/**
 * Provides all core database table specific metadata.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class TableBean extends SchemaObjectBean implements Table {
    private static final long serialVersionUID = -1315274844163173964L;
    private Set<TableColumn> columns = new HashSet<TableColumn>();
    private Set<ForeignKey> foreignKeys = new HashSet<ForeignKey>();
    private Set<Index> indexes = new HashSet<Index>();
    private Set<TableColumn> versionColumns = new HashSet<TableColumn>();
    private Set<Privilege> privileges = new HashSet<Privilege>();
    private Set<BestRowIdentifier> bestRowIdentifiers = new HashSet<BestRowIdentifier>();
    private TableType tableType;
    private Catalog typeCatalog;
    private Schema typeSchema;
    private String typeName;
    private String selfReferencingColumnName;
    private String referenceGeneration;
    private PrimaryKey primaryKey;
    private Table superTable;

    /**
     * Default constructor
     */
    public TableBean() {
    }

    /**
     * Returns type of table such as: "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
     * 
     * @return type of table.
     */
    public TableType getTableType() {
        return tableType;
    }

    /**
     * Sets type of table such as: "TABLE", "VIEW", "SYSTEM TABLE", "GLOBAL TEMPORARY", "LOCAL TEMPORARY", "ALIAS", "SYNONYM".
     * 
     * @param tableType the type of table.
     */
    public void setTableType( TableType tableType ) {
        this.tableType = tableType;
    }

    /**
     * Gets type catalog
     * 
     * @return types catalog (may be <code>null</code>)
     */
    public Catalog getTypeCatalog() {
        return typeCatalog;
    }

    /**
     * Sets type catalog
     * 
     * @param typeCatalog the types catalog (may be <code>null</code>)
     */
    public void setTypeCatalog( Catalog typeCatalog ) {
        this.typeCatalog = typeCatalog;
    }

    /**
     * Gets type schema
     * 
     * @return types schema (may be <code>null</code>)
     */
    public Schema getTypeSchema() {
        return typeSchema;
    }

    /**
     * Sets type schema
     * 
     * @param typeSchema the types schema (may be <code>null</code>)
     */
    public void setTypeSchema( Schema typeSchema ) {
        this.typeSchema = typeSchema;
    }

    /**
     * Gets type name
     * 
     * @return types name (may be <code>null</code>)
     */
    public String getTypeName() {
        return typeName;
    }

    /**
     * Sets type name
     * 
     * @param typeName types name (may be <code>null</code>)
     */
    public void setTypeName( String typeName ) {
        this.typeName = typeName;
    }

    /**
     * Gets name of the designated "identifier" column of a typed table (may be <code>null</code>)
     * 
     * @return name of the designated "identifier" column of a typed table (may be <code>null</code>)
     */
    public String getSelfReferencingColumnName() {
        return selfReferencingColumnName;
    }

    /**
     * Sets name of the designated "identifier" column of a typed table (may be <code>null</code>)
     * 
     * @param selfReferencingColumnName the name of the designated "identifier" column of a typed table (may be <code>null</code>)
     */
    public void setSelfReferencingColumnName( String selfReferencingColumnName ) {
        this.selfReferencingColumnName = selfReferencingColumnName;
    }

    /**
     * specifies how values in getSelfReferencingColumnName () are created. Values are "SYSTEM", "USER", "DERIVED". (may be
     * <code>null</code>)
     * 
     * @return how values in getSelfReferencingColumnName () are created.
     */
    public String getReferenceGeneration() {
        return referenceGeneration;
    }

    /**
     * specifies how values in getSelfReferencingColumnName () are created. Values are "SYSTEM", "USER", "DERIVED". (may be
     * <code>null</code>)
     * 
     * @param referenceGeneration how values in getSelfReferencingColumnName () are created.
     */
    public void setReferenceGeneration( String referenceGeneration ) {
        this.referenceGeneration = referenceGeneration;
    }

    /**
     * Gets a set of table columns
     * 
     * @return a set of table columns.
     */
    public Set<TableColumn> getColumns() {
        return columns;
    }

    /**
     * Adds TableColumn
     * 
     * @param column the TableColumn
     */
    public void addColumn( TableColumn column ) {
        columns.add(column);
    }

    /**
     * deletes TableColumn
     * 
     * @param column the TableColumn
     */
    public void deleteColumn( TableColumn column ) {
        columns.remove(column);
    }

    /**
     * Returns table column for specified column name or null
     * 
     * @param columnName the name of column
     * @return table column for specified column name or null.
     */
    public TableColumn findColumnByName( String columnName ) {
        for (TableColumn c : columns) {
            if (c.getName().equals(columnName)) {
                return c;
            }
        }
        // return nothing
        return null;
    }

    /**
     * Gets a table primary key
     * 
     * @return a table primary key.
     */
    public PrimaryKey getPrimaryKey() {
        return primaryKey;
    }

    /**
     * Sets a table primary key
     * 
     * @param primaryKey the table primary key.
     */
    public void setPrimaryKey( PrimaryKey primaryKey ) {
        this.primaryKey = primaryKey;
    }

    /**
     * Gets a set of table foreign key columns
     * 
     * @return a set of table foreign keys.
     */
    public Set<ForeignKey> getForeignKeys() {
        return foreignKeys;
    }

    /**
     * adds ForeignKey
     * 
     * @param foreignKey the ForeignKey
     */
    public void addForeignKey( ForeignKey foreignKey ) {
        foreignKeys.add(foreignKey);
    }

    /**
     * deletes ForeignKey
     * 
     * @param foreignKey the ForeignKey
     */
    public void deleteForeignKey( ForeignKey foreignKey ) {
        foreignKeys.remove(foreignKey);
    }

    /**
     * Returns table foreign key for specified name or null
     * 
     * @param fkName the name of foreign key
     * @return table foreign key for specified name or null.
     */
    public ForeignKey findForeignKeyByName( String fkName ) {
        for (ForeignKey fk : foreignKeys) {
            if (fk.getName().equals(fkName)) {
                return fk;
            }
        }
        // return nothing
        return null;
    }

    /**
     * Gets a set of table indexes
     * 
     * @return a set of table indexes.
     */
    public Set<Index> getIndexes() {
        return indexes;
    }

    /**
     * adds Index
     * 
     * @param index the Index
     */
    public void addIndex( Index index ) {
        indexes.add(index);
    }

    /**
     * deletes Index
     * 
     * @param index the Index
     */
    public void deleteIndex( Index index ) {
        indexes.remove(index);
    }

    /**
     * Returns table index for specified name or null
     * 
     * @param indexName the name of index
     * @return table index for specified name or null.
     */
    public Index findIndexByName( String indexName ) {
        for (Index i : indexes) {
            if (i.getName().equals(indexName)) {
                return i;
            }
        }
        // return nothing
        return null;
    }

    /**
     * Gets a set of table version columns
     * 
     * @return a set of table version columns.
     */
    public Set<TableColumn> getVersionColumns() {
        return versionColumns;
    }

    /**
     * adds version column
     * 
     * @param tableColumn the TableColumn
     */
    public void addVersionColumn( TableColumn tableColumn ) {
        versionColumns.add(tableColumn);
    }

    /**
     * deletes version column
     * 
     * @param tableColumn the version column
     */
    public void deleteVersionColumn( TableColumn tableColumn ) {
        versionColumns.remove(tableColumn);
    }

    /**
     * Returns table version column for specified name or null
     * 
     * @param columnName the name of Version Column
     * @return table Version Column for specified name or null.
     */
    public TableColumn findVersionColumnByName( String columnName ) {
        for (TableColumn c : versionColumns) {
            if (c.getName().equals(columnName)) {
                return c;
            }
        }
        // return nothing
        return null;
    }

    /**
     * Gets table privileges.
     * 
     * @return set of table privileges
     */
    public Set<Privilege> getPrivileges() {
        return privileges;
    }

    /**
     * Adds table priviledge
     * 
     * @param privilege the table priviledge
     */
    public void addPrivilege( Privilege privilege ) {
        privileges.add(privilege);
    }

    /**
     * Deletes table priviledge
     * 
     * @param privilege the table priviledge
     */
    public void deletePrivilege( Privilege privilege ) {
        privileges.remove(privilege);
    }

    /**
     * Searches priviledge by name
     * 
     * @param priviledgeName the priviledge name to search
     * @return priviledge if found, otherwise return null
     */
    public Privilege findPriviledgeByName( String priviledgeName ) {
        for (Privilege p : privileges) {
            if (p.getName().equals(priviledgeName)) {
                return p;
            }
        }
        // return nothing
        return null;
    }

    /**
     * Retrieves a set of descriptions of a table's optimal set of columns that uniquely identifies a row in temporary scopes.
     * 
     * @return BestRowIdentifier set that uniquely identifies a row in scopes.
     */
    public Set<BestRowIdentifier> getBestRowIdentifiers() {
        return bestRowIdentifiers;
    }

    /**
     * Adds BestRowIdentifier
     * 
     * @param bestRowIdentifier the BestRowIdentifier
     */
    public void addBestRowIdentifier( BestRowIdentifier bestRowIdentifier ) {
        bestRowIdentifiers.add(bestRowIdentifier);
    }

    /**
     * deletes BestRowIdentifier
     * 
     * @param bestRowIdentifier the BestRowIdentifier
     */
    public void deleteBestRowIdentifier( BestRowIdentifier bestRowIdentifier ) {
        bestRowIdentifiers.remove(bestRowIdentifier);
    }

    /**
     * Searches the BestRowIdentifier by scope
     * 
     * @param scopeType the scope of best row identifier
     * @return BestRowIdentifier if any
     */
    public BestRowIdentifier findBestRowIdentifierByScopeType( BestRowIdentifierScopeType scopeType ) {
        for (BestRowIdentifier bri : bestRowIdentifiers) {
            if (bri.getScopeType().equals(scopeType)) {
                return bri;
            }
        }
        // return nothing
        return null;
    }

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
    public Table getSuperTable() {
        return superTable;
    }

    /**
     * Sets a description of the table hierarchies defined in a particular schema in this database. Only the immediate super type/
     * sub type relationship is modeled.
     * 
     * @param superTable the super table for this table
     * @since 1.4 (JDBC 3.0)
     */
    public void setSuperTable( Table superTable ) {
        this.superTable = superTable;
    }
}
