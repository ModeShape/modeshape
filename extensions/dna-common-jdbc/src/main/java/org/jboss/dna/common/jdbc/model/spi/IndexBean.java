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
import org.jboss.dna.common.jdbc.model.api.Index;
import org.jboss.dna.common.jdbc.model.api.IndexColumn;
import org.jboss.dna.common.jdbc.model.api.IndexType;

/**
 * Provides all database table's index specific metadata.
 * 
 * @author <a href="mailto:litsenko_sergey@yahoo.com">Sergiy Litsenko</a>
 */
public class IndexBean extends SchemaObjectBean implements Index {
    private static final long serialVersionUID = -1217601426100735909L;
    private Set<IndexColumn> columns = new HashSet<IndexColumn>();
    private Boolean unique;
    private IndexType indexType;
    private Integer cardinality;
    private Integer pages;
    private String filterCondition;

    /**
     * Default constructor
     */
    public IndexBean() {
    }

    /**
     * Retrieves index columns
     * 
     * @return index columns
     */
    public Set<IndexColumn> getColumns() {
        return columns;
    }

    /**
     * Adds IndexColumn
     * 
     * @param indexColumn the IndexColumn
     */
    public void addColumn( IndexColumn indexColumn ) {
        columns.add(indexColumn);
    }

    /**
     * delete IndexColumn
     * 
     * @param indexColumn the IndexColumn
     */
    public void deleteColumn( IndexColumn indexColumn ) {
        columns.remove(indexColumn);
    }

    /**
     * Returns index column for specified column name or null
     * 
     * @param columnName the name of column
     * @return index column for specified column name or null.
     */
    public IndexColumn findColumnByName( String columnName ) {
        for (IndexColumn ic : columns) {
            if (ic.getName().equals(columnName)) {
                return ic;
            }
        }
        // return nothing
        return null;
    }

    /**
     * Can index values be non-unique. false when TYPE is tableIndexStatistic.
     * 
     * @return true if index values can be non-unique.
     */
    public Boolean isUnique() {
        return unique;
    }

    /**
     * Can index values be non-unique. false when TYPE is tableIndexStatistic.
     * 
     * @param unique true if index values can be non-unique.
     */
    public void setUnique( Boolean unique ) {
        this.unique = unique;
    }

    /**
     * Gets index type
     * 
     * @return index type
     */
    public IndexType getIndexType() {
        return indexType;
    }

    /**
     * Sets index type
     * 
     * @param indexType index type
     */
    public void setIndexType( IndexType indexType ) {
        this.indexType = indexType;
    }

    /**
     * When TYPE is tableIndexStatistic, then this is the number of rows in the table; otherwise, it is the number of unique
     * values in the index.
     * 
     * @return the number of rows in the table if index type is STATISTICS; otherwise, the number of unique values in the index.
     */
    public Integer getCardinality() {
        return cardinality;
    }

    /**
     * When TYPE is tableIndexStatistic, then this is the number of rows in the table; otherwise, it is the number of unique
     * values in the index.
     * 
     * @param cardinality the number of rows in the table if index type is STATISTICS; otherwise, the number of unique values in
     *        the index.
     */
    public void setCardinality( Integer cardinality ) {
        this.cardinality = cardinality;
    }

    /**
     * When TYPE is tableIndexStatisic then this is the number of pages used for the table, otherwise it is the number of pages
     * used for the current index.
     * 
     * @return the number of pages used for the table if index type is STATISTICS; otherwise the number of pages used for the
     *         current index.
     */
    public Integer getPages() {
        return pages;
    }

    /**
     * When TYPE is tableIndexStatisic then this is the number of pages used for the table, otherwise it is the number of pages
     * used for the current index.
     * 
     * @param pages the number of pages used for the table if index type is STATISTICS; otherwise the number of pages used for the
     *        current index.
     */
    public void setPages( Integer pages ) {
        this.pages = pages;
    }

    /**
     * Returns the filter condition, if any. (may be null)
     * 
     * @return the filter condition, if any. (may be null)
     */
    public String getFilterCondition() {
        return filterCondition;
    }

    /**
     * Sets the filter condition, if any. (may be null)
     * 
     * @param filterCondition the filter condition, if any. (may be null)
     */
    public void setFilterCondition( String filterCondition ) {
        this.filterCondition = filterCondition;
    }
}
