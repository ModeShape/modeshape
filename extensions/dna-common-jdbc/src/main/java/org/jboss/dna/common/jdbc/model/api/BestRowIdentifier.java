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

import java.util.Set;

/**
 * Provides table's best row identifies specific metadata.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public interface BestRowIdentifier extends CoreMetaData {

    /**
     * Return the scope of best row identifier
     * 
     * @return the scope of best row identifier
     */
    BestRowIdentifierScopeType getScopeType();

    /**
     * Sets the scope of best row identifier
     * 
     * @param scopeType the scope of best row identifier
     */
    void setScopeType( BestRowIdentifierScopeType scopeType );

    /**
     * Retrieves best row identifier columns
     * 
     * @return best row identifier columns
     */
    Set<Column> getColumns();

    /**
     * Adds column to the best row identifier
     * 
     * @param column the column that part of best row identifier
     */
    void addColumn( Column column );

    /**
     * Deletes column from the best row identifier
     * 
     * @param column the column that no longer part of best row identifier
     */
    void deleteColumn( Column column );

    /**
     * Searches column by name
     * 
     * @param columnName the column name to search
     * @return column if found, otherwise return null
     */
    Column findColumnByName( String columnName );
}
