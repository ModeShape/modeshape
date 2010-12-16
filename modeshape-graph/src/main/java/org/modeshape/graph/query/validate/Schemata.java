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
package org.modeshape.graph.query.validate;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.query.model.Operator;
import org.modeshape.graph.query.model.Ordering;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.SelectorName;

/**
 * The interface used to access the structure being queried and validate a query.
 */
@Immutable
public interface Schemata {

    /**
     * Get the information for the table or view with the supplied name within this schema.
     * <p>
     * The resulting definition is immutable.
     * </p>
     * 
     * @param name the table or view name; may not be null
     * @return the table or view information, or null if there is no such table
     */
    Table getTable( SelectorName name );

    /**
     * Information about a queryable table.
     */
    public interface Table {
        /**
         * Get the name for this table.
         * 
         * @return the table name; never null
         */
        SelectorName getName();

        /**
         * Get the information for a column with the supplied name within this table.
         * <p>
         * The resulting column definition is immutable.
         * </p>
         * 
         * @param name the column name; may not be null
         * @return the column information, or null if there is no such column
         */
        Column getColumn( String name );

        /**
         * Get the queryable columns in this table.
         * 
         * @return the immutable map of immutable column objects by their name; never null
         */
        Map<String, Column> getColumnsByName();

        /**
         * Get the queryable columns in this table.
         * 
         * @return the immutable, ordered list of immutable column objects; never null
         */
        List<Column> getColumns();

        /**
         * Get the queryable columns in this table that should be used in case of "SELECT *".
         * 
         * @return the immutable, ordered list of immutable column objects; never null
         */
        List<Column> getSelectAllColumns();

        /**
         * Get the queryable columns in this table that should be used in case of "SELECT *".
         * 
         * @return the immutable map of immutable column objects by their name; never null
         */
        Map<String, Column> getSelectAllColumnsByName();

        /**
         * Get the collection of keys for this table.
         * 
         * @return the immutable collection of immutable keys; never null, but possibly empty
         */
        Collection<Key> getKeys();

        /**
         * Determine whether this table has a {@link #getKeys() key} that contains exactly those columns listed.
         * 
         * @param columns the columns for the key
         * @return true if this table contains a key using exactly the supplied columns, or false otherwise
         */
        boolean hasKey( Column... columns );

        /**
         * Determine whether this table has a {@link #getKeys() key} that contains exactly those columns listed.
         * 
         * @param columns the columns for the key
         * @return true if this table contains a key using exactly the supplied columns, or false otherwise
         */
        boolean hasKey( Iterable<Column> columns );

        /**
         * Obtain this table's {@link #getKeys() key} that contains exactly those columns listed.
         * <p>
         * The resulting key definition is immutable.
         * </p>
         * 
         * @param columns the columns for the key
         * @return the key that uses exactly the supplied columns, or null if there is no such key
         */
        Key getKey( Column... columns );

        /**
         * Obtain this table's {@link #getKeys() key} that contains exactly those columns listed.
         * <p>
         * The resulting key definition is immutable.
         * </p>
         * 
         * @param columns the columns for the key
         * @return the key that uses exactly the supplied columns, or null if there is no such key
         */
        Key getKey( Iterable<Column> columns );

        /**
         * Determine whether this table allows has extra columns not included in the {@link #getColumns() column list}. This value
         * is used to determine whether columns in a SELECT clause should be validated against the list of known columns.
         * 
         * @return true if there are extra columns, or false if the {@link #getColumns() list of columns} is complete for this
         *         table.
         */
        boolean hasExtraColumns();
    }

    /**
     * Information about a view that is defined in terms of other views/tables.
     */
    public interface View extends Table {
        /**
         * Get the {@link QueryCommand query} that is the definition of the view.
         * 
         * @return the view definition; never null
         */
        QueryCommand getDefinition();
    }

    /**
     * Information about a queryable column.
     */
    public interface Column {
        /**
         * Get the name for this column.
         * 
         * @return the column name; never null
         */
        String getName();

        /**
         * Get the property type for this column.
         * 
         * @return the property type; never null
         */
        String getPropertyType();

        /**
         * Get whether the column can be used in a full-text search.
         * 
         * @return true if the column is full-text searchable, or false otherwise
         */
        boolean isFullTextSearchable();

        /**
         * Get the set of operators that can be used in a comparison involving this column.
         * 
         * @return the operators; never null but possibly empty
         */
        Set<Operator> getOperators();

        /**
         * Get whether this column can be used within an {@link Ordering ORDER BY clause}.
         * 
         * @return true if this column can be used in an order specification, or false otherwise
         */
        boolean isOrderable();
    }

    /**
     * Information about a key for a table.
     */
    public interface Key {
        /**
         * Get the columns that make up this key.
         * 
         * @return the key's columns; immutable and never null
         */
        Set<Column> getColumns();

        /**
         * Determine whether this key contains exactly those columns listed.
         * 
         * @param columns the columns for the key
         * @return true if this key contains exactly the supplied columns, or false otherwise
         */
        boolean hasColumns( Column... columns );

        /**
         * Determine whether this key contains exactly those columns listed.
         * 
         * @param columns the columns for the key
         * @return true if this key contains exactly the supplied columns, or false otherwise
         */
        boolean hasColumns( Iterable<Column> columns );
    }

}
