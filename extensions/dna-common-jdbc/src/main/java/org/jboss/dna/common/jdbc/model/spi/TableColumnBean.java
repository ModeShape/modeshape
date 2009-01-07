/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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

import org.jboss.dna.common.jdbc.model.api.ColumnPseudoType;
import org.jboss.dna.common.jdbc.model.api.TableColumn;
import org.jboss.dna.common.jdbc.model.api.Reference;

/**
 * Provides all database table column specific metadata.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class TableColumnBean extends ColumnBean implements TableColumn {
    private static final long serialVersionUID = -1719977563697808831L;
    private Boolean bestRowIdentifier;
    private ColumnPseudoType pseudoType;
    private Reference reference;
    private Boolean versionColumn;
    private Boolean primaryKeyColumn;
    private Boolean foreignKeyColumn;
    private Boolean indexColumn;

    /**
     * Default constructor
     */
    public TableColumnBean() {
    }

    /**
     * Is this column the part of Best Row Identifier in any scope?
     * 
     * @return true if this column is the part of Best Row Identifier in any scope, otherwise return false (even if unknown)
     */
    public Boolean isBestRowIdentifier() {
        return bestRowIdentifier;
    }

    /**
     * Is this column the part of Best Row Identifier in any scope?
     * 
     * @param bestRowIdentifier true if this column is the part of Best Row Identifier in any scope, otherwise return false (even
     *        if unknown)
     */
    public void setBestRowIdentifier( Boolean bestRowIdentifier ) {
        this.bestRowIdentifier = bestRowIdentifier;
    }

    /**
     * Return column's pseudo type
     * 
     * @return column's pseudo type
     */
    public ColumnPseudoType getPseudoType() {
        return pseudoType;
    }

    /**
     * Sets column's pseudo type
     * 
     * @param pseudoType the column's pseudo type
     */
    public void setPseudoType( ColumnPseudoType pseudoType ) {
        this.pseudoType = pseudoType;
    }

    /**
     * Returns column reference if datatype is REF
     * 
     * @return column reference if datatype is REF
     */
    public Reference getReference() {
        return reference;
    }

    /**
     * Sets column reference if datatype is REF
     * 
     * @param reference the column reference if datatype is REF
     */
    public void setReference( Reference reference ) {
        this.reference = reference;
    }

    /**
     * Retrieves true if column is automatically updated when any value in a row is updated. If it retrieves true then column can
     * be cast to VersionColumn.
     * 
     * @return true if column is automatically updated when any value in a row is updated, return false overwise.
     */
    public Boolean isVersionColumn() {
        return versionColumn;
    }

    /**
     * Sets true if column is automatically updated when any value in a row is updated. If it retrieves true then column can be
     * cast to VersionColumn.
     * 
     * @param versionColumn true if column is automatically updated when any value in a row is updated, return false overwise.
     */
    public void setVersionColumn( Boolean versionColumn ) {
        this.versionColumn = versionColumn;
    }

    /**
     * Retrieves true if column is part of primary key.
     * 
     * @return true if column is part of primary key, return false overwise.
     */
    public Boolean isPrimaryKeyColumn() {
        return primaryKeyColumn;
    }

    /**
     * Sets true if column is part of primary key.
     * 
     * @param primaryKeyColumn true if column is part of primary key, return false overwise.
     */
    public void setPrimaryKeyColumn( Boolean primaryKeyColumn ) {
        this.primaryKeyColumn = primaryKeyColumn;
    }

    /**
     * Retrieves true if column is part of foreign key.
     * 
     * @return true if column is part of foreign key, return false overwise.
     */
    public Boolean isForeignKeyColumn() {
        return foreignKeyColumn;
    }

    /**
     * Sets true if column is part of foreign key.
     * 
     * @param foreignKeyColumn true if column is part of foreign key, return false overwise.
     */
    public void setForeignKeyColumn( Boolean foreignKeyColumn ) {
        this.foreignKeyColumn = foreignKeyColumn;
    }

    /**
     * Retrieves true if column is part of any index.
     * 
     * @return true if column is part of any index, return false overwise.
     */
    public Boolean isIndexColumn() {
        return indexColumn;
    }

    /**
     * Sets true if column is part of any index.
     * 
     * @param indexColumn true if column is part of any index, return false overwise.
     */
    public void setIndexColumn( Boolean indexColumn ) {
        this.indexColumn = indexColumn;
    }
}
