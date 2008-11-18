/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
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
import org.jboss.dna.common.jdbc.model.api.ForeignKey;
import org.jboss.dna.common.jdbc.model.api.ForeignKeyColumn;
import org.jboss.dna.common.jdbc.model.api.KeyDeferrabilityType;
import org.jboss.dna.common.jdbc.model.api.KeyModifyRuleType;
import org.jboss.dna.common.jdbc.model.api.PrimaryKey;
import org.jboss.dna.common.jdbc.model.api.Table;

/**
 * Provides database table's foreing key specific metadata.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class ForeignKeyBean extends SchemaObjectBean implements ForeignKey {
    private static final long serialVersionUID = 3481927472605189902L;
    private Set<ForeignKeyColumn> columns = new HashSet<ForeignKeyColumn>();
    private Table sourceTable;
    private PrimaryKey sourcePrimaryKey;
    private KeyModifyRuleType updateRule;
    private KeyModifyRuleType deleteRule;
    private KeyDeferrabilityType deferrability;

    /**
     * Default constructor
     */
    public ForeignKeyBean() {
    }

    /**
     * Retrieves foreign key columns
     * 
     * @return foreign key columns
     */
    public Set<ForeignKeyColumn> getColumns() {
        return columns;
    }

    /**
     * Adds ForeignKeyColumn
     * 
     * @param column the ForeignKeyColumn
     */
    public void addColumn( ForeignKeyColumn column ) {
        columns.add(column);
    }

    /**
     * Removes ForeignKeyColumn
     * 
     * @param column the ForeignKeyColumn
     */
    public void deleteColumn( ForeignKeyColumn column ) {
        columns.remove(column);
    }

    /**
     * Returns table column for specified column name or null
     * 
     * @param columnName the name of column
     * @return table column for specified column name or null.
     */
    public ForeignKeyColumn findColumnByName( String columnName ) {
        for (ForeignKeyColumn fkc : columns) {
            if (fkc.getName().equals(columnName)) {
                return fkc;
            }
        }
        // return nothing
        return null;
    }

    /**
     * Returns the scope table of a foreign key.
     * 
     * @return the scope table of a foreign key.
     */
    public Table getSourceTable() {
        return sourceTable;
    }

    /**
     * Sets the scope table of a foreign key.
     * 
     * @param sourceTable the scope table of a foreign key.
     */
    public void setSourceTable( Table sourceTable ) {
        this.sourceTable = sourceTable;
    }

    /**
     * Returns the PK of scope table.
     * 
     * @return the PK of scope table.
     */
    public PrimaryKey getSourcePrimaryKey() {
        return sourcePrimaryKey;
    }

    /**
     * Sets the PK of scope table.
     * 
     * @param primaryKey the PK of scope table.
     */
    public void setSourcePrimaryKey( PrimaryKey primaryKey ) {
        this.sourcePrimaryKey = primaryKey;
    }

    /**
     * What happens to a foreign key when the primary key is updated
     * 
     * @return what happens to a foreign key when the primary key is updated
     */
    public KeyModifyRuleType getUpdateRule() {
        return updateRule;
    }

    /**
     * What happens to a foreign key when the primary key is updated
     * 
     * @param updateRule what happens to a foreign key when the primary key is updated
     */
    public void setUpdateRule( KeyModifyRuleType updateRule ) {
        this.updateRule = updateRule;
    }

    /**
     * What happens to a foreign key when the primary key is deleted
     * 
     * @return what happens to a foreign key when the primary key is deleted
     */
    public KeyModifyRuleType getDeleteRule() {
        return deleteRule;
    }

    /**
     * What happens to a foreign key when the primary key is deleted
     * 
     * @param deleteRule what happens to a foreign key when the primary key is deleted
     */
    public void setDeleteRule( KeyModifyRuleType deleteRule ) {
        this.deleteRule = deleteRule;
    }

    /**
     * Can the evaluation of foreign key constraints be deferred until commit
     * 
     * @return the evaluation of foreign key constraints be deferred until commit
     */
    public KeyDeferrabilityType getDeferrability() {
        return deferrability;
    }

    /**
     * Can the evaluation of foreign key constraints be deferred until commit
     * 
     * @param deferrability the evaluation of foreign key constraints be deferred until commit
     */
    public void setDeferrability( KeyDeferrabilityType deferrability ) {
        this.deferrability = deferrability;
    }
}
