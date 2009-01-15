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
 * Provides database table's foreing key specific metadata.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public interface ForeignKey extends SchemaObject {

    /**
     * Retrieves foreign key columns
     * 
     * @return foreign key columns
     */
    Set<ForeignKeyColumn> getColumns();

    /**
     * Adds ForeignKeyColumn
     * 
     * @param column the ForeignKeyColumn
     */
    void addColumn( ForeignKeyColumn column );

    /**
     * deletes ForeignKeyColumn
     * 
     * @param column the ForeignKeyColumn
     */
    void deleteColumn( ForeignKeyColumn column );

    /**
     * Returns table column for specified column name or null
     * 
     * @param columnName the name of column
     * @return table column for specified column name or null.
     */
    ForeignKeyColumn findColumnByName( String columnName );

    /**
     * Returns the scope table of a foreign key.
     * 
     * @return the scope table of a foreign key.
     */
    Table getSourceTable();

    /**
     * Sets the scope table of a foreign key.
     * 
     * @param sourceTable the scope table of a foreign key.
     */
    void setSourceTable( Table sourceTable );

    /**
     * Returns the PK of scope table.
     * 
     * @return the PK of scope table.
     */
    PrimaryKey getSourcePrimaryKey();

    /**
     * Sets the PK of scope table.
     * 
     * @param primaryKey the PK of scope table.
     */
    void setSourcePrimaryKey( PrimaryKey primaryKey );

    /**
     * What happens to a foreign key when the primary key is updated
     * 
     * @return what happens to a foreign key when the primary key is updated
     */
    KeyModifyRuleType getUpdateRule();

    /**
     * What happens to a foreign key when the primary key is updated
     * 
     * @param updateRule what happens to a foreign key when the primary key is updated
     */
    void setUpdateRule( KeyModifyRuleType updateRule );

    /**
     * What happens to a foreign key when the primary key is deleted
     * 
     * @return what happens to a foreign key when the primary key is deleted
     */
    KeyModifyRuleType getDeleteRule();

    /**
     * What happens to a foreign key when the primary key is deleted
     * 
     * @param deleteRule what happens to a foreign key when the primary key is deleted
     */
    void setDeleteRule( KeyModifyRuleType deleteRule );

    /**
     * Can the evaluation of foreign key constraints be deferred until commit
     * 
     * @return the evaluation of foreign key constraints be deferred until commit
     */
    KeyDeferrabilityType getDeferrability();

    /**
     * Can the evaluation of foreign key constraints be deferred until commit
     * 
     * @param deferrability the evaluation of foreign key constraints be deferred until commit
     */
    void setDeferrability( KeyDeferrabilityType deferrability );
}
