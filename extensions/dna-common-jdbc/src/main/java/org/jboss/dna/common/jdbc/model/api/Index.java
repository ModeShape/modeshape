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
package org.jboss.dna.common.jdbc.model.api;

import java.util.Set;

/**
 * Provides all database table's index specific metadata.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public interface Index extends SchemaObject {

    /**
     * Retrieves index columns
     * 
     * @return index columns
     */
    Set<IndexColumn> getColumns();

    /**
     * Adds IndexColumn
     * 
     * @param indexColumn the IndexColumn
     */
    void addColumn( IndexColumn indexColumn );

    /**
     * delete IndexColumn
     * 
     * @param indexColumn the IndexColumn
     */
    void deleteColumn( IndexColumn indexColumn );

    /**
     * Returns index column for specified column name or null
     * 
     * @param columnName the name of column
     * @return index column for specified column name or null.
     */
    IndexColumn findColumnByName( String columnName );

    /**
     * Can index values be non-unique. false when TYPE is tableIndexStatistic.
     * 
     * @return true if index values can be non-unique.
     */
    Boolean isUnique();

    /**
     * Can index values be non-unique. false when TYPE is tableIndexStatistic.
     * 
     * @param unique true if index values can be non-unique.
     */
    void setUnique( Boolean unique );

    /**
     * Gets index type
     * 
     * @return index type
     */
    IndexType getIndexType();

    /**
     * Sets index type
     * 
     * @param indexType index type
     */
    void setIndexType( IndexType indexType );

    /**
     * When TYPE is tableIndexStatistic, then this is the number of rows in the table; otherwise, it is the number of unique
     * values in the index.
     * 
     * @return the number of rows in the table if index type is STATISTICS; otherwise, the number of unique values in the index.
     */
    Integer getCardinality();

    /**
     * When TYPE is tableIndexStatistic, then this is the number of rows in the table; otherwise, it is the number of unique
     * values in the index.
     * 
     * @param cardinality the number of rows in the table if index type is STATISTICS; otherwise, the number of unique values in
     *        the index.
     */
    void setCardinality( Integer cardinality );

    /**
     * When TYPE is tableIndexStatisic then this is the number of pages used for the table, otherwise it is the number of pages
     * used for the current index.
     * 
     * @return the number of pages used for the table if index type is STATISTICS; otherwise the number of pages used for the
     *         current index.
     */
    Integer getPages();

    /**
     * When TYPE is tableIndexStatisic then this is the number of pages used for the table, otherwise it is the number of pages
     * used for the current index.
     * 
     * @param pages the number of pages used for the table if index type is STATISTICS; otherwise the number of pages used for the
     *        current index.
     */
    void setPages( Integer pages );

    /**
     * Returns the filter condition, if any. (may be null)
     * 
     * @return the filter condition, if any. (may be null)
     */
    String getFilterCondition();

    /**
     * Sets the filter condition, if any. (may be null)
     * 
     * @param filterCondition the filter condition, if any. (may be null)
     */
    void setFilterCondition( String filterCondition );
}
