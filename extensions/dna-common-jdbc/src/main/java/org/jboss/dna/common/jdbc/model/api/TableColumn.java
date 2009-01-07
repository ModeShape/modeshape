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
package org.jboss.dna.common.jdbc.model.api;

/**
 * Provides all database table column specific metadata.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public interface TableColumn extends Column {

    /**
     * Is this column the part of Best Row Identifier in any scope?
     * 
     * @return true if this column is the part of Best Row Identifier in any scope, otherwise return false (even if unknown)
     */
    Boolean isBestRowIdentifier();

    /**
     * Is this column the part of Best Row Identifier in any scope?
     * 
     * @param bestRowIdentifier true if this column is the part of Best Row Identifier in any scope, otherwise return false (even
     *        if unknown)
     */
    void setBestRowIdentifier( Boolean bestRowIdentifier );

    /**
     * Return column's pseudo type
     * 
     * @return column's pseudo type
     */
    ColumnPseudoType getPseudoType();

    /**
     * Sets column's pseudo type
     * 
     * @param pseudoType the column's pseudo type
     */
    void setPseudoType( ColumnPseudoType pseudoType );

    /**
     * Returns column reference if datatype is REF
     * 
     * @return column reference if datatype is REF
     */
    Reference getReference();

    /**
     * Sets column reference if datatype is REF
     * 
     * @param reference the column reference if datatype is REF
     */
    void setReference( Reference reference );

    /**
     * Retrieves true if column is automatically updated when any value in a row is updated. If it retrieves true then column can
     * be cast to VersionColumn.
     * 
     * @return true if column is automatically updated when any value in a row is updated, return false overwise.
     */
    Boolean isVersionColumn();

    /**
     * Sets true if column is automatically updated when any value in a row is updated. If it retrieves true then column can be
     * cast to VersionColumn.
     * 
     * @param versionColumn true if column is automatically updated when any value in a row is updated, return false overwise.
     */
    void setVersionColumn( Boolean versionColumn );

    /**
     * Retrieves true if column is part of primary key.
     * 
     * @return true if column is part of primary key, return false overwise.
     */
    Boolean isPrimaryKeyColumn();

    /**
     * Sets true if column is part of primary key.
     * 
     * @param primaryKeyColumn true if column is part of primary key, return false overwise.
     */
    void setPrimaryKeyColumn( Boolean primaryKeyColumn );

    /**
     * Retrieves true if column is part of foreign key.
     * 
     * @return true if column is part of foreign key, return false overwise.
     */
    Boolean isForeignKeyColumn();

    /**
     * Sets true if column is part of foreign key.
     * 
     * @param foreignKeyColumn true if column is part of foreign key, return false overwise.
     */
    void setForeignKeyColumn( Boolean foreignKeyColumn );

    /**
     * Retrieves true if column is part of any index.
     * 
     * @return true if column is part of any index, return false overwise.
     */
    Boolean isIndexColumn();

    /**
     * Sets true if column is part of any index.
     * 
     * @param indexColumn true if column is part of any index, return false overwise.
     */
    void setIndexColumn( Boolean indexColumn );
}
