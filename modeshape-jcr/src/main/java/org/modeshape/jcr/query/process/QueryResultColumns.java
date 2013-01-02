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
package org.modeshape.jcr.query.process;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.collection.ArrayListMultimap;
import org.modeshape.common.collection.Multimap;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.GraphI18n;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.QueryResults.Location;
import org.modeshape.jcr.query.model.Column;
import org.modeshape.jcr.query.model.Constraint;
import org.modeshape.jcr.query.model.FullTextSearch;
import org.modeshape.jcr.query.model.Visitors;

/**
 * Defines the columns associated with the results of a query. This definition allows the values to be accessed
 */
@Immutable
public class QueryResultColumns implements Columns {
    private static final long serialVersionUID = 1L;

    protected static final List<Column> NO_COLUMNS = Collections.<Column>emptyList();
    protected static final List<String> NO_TYPES = Collections.<String>emptyList();
    protected static final QueryResultColumns EMPTY = new QueryResultColumns(false, null, null);

    protected static final String DEFAULT_SELECTOR_NAME = "Results";

    /**
     * Get an empty results column definition.
     * 
     * @return the empty columns definition; never null
     */
    public static QueryResultColumns empty() {
        return EMPTY;
    }

    private final int tupleSize;
    private final int locationStartIndexInTuple;
    private final List<Column> columns;
    private final List<String> columnNames;
    private final List<String> columnTypes;
    private final List<String> selectorNames;
    private List<String> tupleValueNames;
    private final Map<String, String> selectorNameByColumnName;
    private final Map<String, Integer> columnIndexByColumnName;
    private final Map<String, Integer> locationIndexBySelectorName;
    private final Map<String, Integer> locationIndexByColumnName;
    private final Map<Integer, Integer> locationIndexByColumnIndex;
    private final Map<String, Map<String, ColumnInfo>> columnIndexByPropertyNameBySelectorName;
    private final Map<String, Integer> fullTextSearchScoreIndexBySelectorName;
    private final Map<String, String> propertyNameByColumnName;

    protected final static class ColumnInfo {
        protected final int columnIndex;
        protected final String type;

        protected ColumnInfo( int columnIndex,
                              String type ) {
            this.columnIndex = columnIndex;
            this.type = type;
        }
    }

    /**
     * Create a new definition for the query results given the supplied columns.
     * 
     * @param columns the columns that define the results; should never be modified directly
     * @param columnTypes the names of the types for each column in <code>columns</code>
     * @param includeFullTextSearchScores true if room should be made in the tuples for the full-text search scores for each
     *        {@link Location}, or false otherwise
     */
    public QueryResultColumns( List<? extends Column> columns,
                               List<String> columnTypes,
                               boolean includeFullTextSearchScores ) {
        this(includeFullTextSearchScores, columns, columnTypes);
        CheckArg.isNotEmpty(columns, "columns");
        CheckArg.isNotEmpty(columnTypes, "columnTypes");
    }

    /**
     * Create a new definition for the query results given the supplied columns.
     * 
     * @param includeFullTextSearchScores true if room should be made in the tuples for the full-text search scores for each
     *        {@link Location}, or false otherwise
     * @param columns the columns that define the results; should never be modified directly
     * @param columnTypes the names of the types for each column in <code>columns</code>
     */
    protected QueryResultColumns( boolean includeFullTextSearchScores,
                                  List<? extends Column> columns,
                                  List<String> columnTypes ) {
        this.columns = columns != null ? Collections.<Column>unmodifiableList(columns) : NO_COLUMNS;
        this.columnTypes = columnTypes != null ? Collections.<String>unmodifiableList(columnTypes) : NO_TYPES;
        this.columnIndexByColumnName = new HashMap<String, Integer>();
        Set<String> selectors = new HashSet<String>();
        final int columnCount = this.columns.size();
        this.locationIndexBySelectorName = new HashMap<String, Integer>();
        this.locationIndexByColumnIndex = new HashMap<Integer, Integer>();
        this.locationIndexByColumnName = new HashMap<String, Integer>();
        this.selectorNameByColumnName = new HashMap<String, String>();
        this.columnIndexByPropertyNameBySelectorName = new HashMap<String, Map<String, ColumnInfo>>();
        this.propertyNameByColumnName = new HashMap<String, String>();
        List<String> names = new ArrayList<String>(columnCount);
        List<String> selectorNames = new ArrayList<String>(columnCount);
        Set<Column> sameNameColumns = findColumnsWithSameNames(this.columns);

        // Find all the selector names ...
        Integer selectorIndex = new Integer(columnCount - 1);
        this.locationStartIndexInTuple = columnCount;
        for (int i = 0, max = this.columns.size(); i != max; ++i) {
            Column column = this.columns.get(i);
            assert column != null;
            String selectorName = column.selectorName().name();
            if (selectors.add(selectorName)) {
                selectorNames.add(selectorName);
                selectorIndex = new Integer(selectorIndex.intValue() + 1);
                locationIndexBySelectorName.put(selectorName, selectorIndex);
            }
        }

        // Now, find all of the column names ...
        selectorIndex = new Integer(columnCount - 1);
        Set<String> tempSelectors = new HashSet<String>();
        for (int i = 0, max = this.columns.size(); i != max; ++i) {
            Column column = this.columns.get(i);
            assert column != null;
            String selectorName = column.selectorName().name();
            if (tempSelectors.add(selectorName)) {
                selectorIndex = new Integer(selectorIndex.intValue() + 1);
            }
            String columnName = columnNameFor(column, names, sameNameColumns, selectors);
            assert columnName != null;
            propertyNameByColumnName.put(columnName, column.getPropertyName());
            selectorNameByColumnName.put(columnName, selectorName);
            columnIndexByColumnName.put(columnName, new Integer(i));
            locationIndexByColumnIndex.put(new Integer(i), selectorIndex);
            locationIndexByColumnName.put(columnName, selectorIndex);
            // Insert the entry by selector name and property name ...
            Map<String, ColumnInfo> byPropertyName = columnIndexByPropertyNameBySelectorName.get(selectorName);
            if (byPropertyName == null) {
                byPropertyName = new HashMap<String, ColumnInfo>();
                columnIndexByPropertyNameBySelectorName.put(selectorName, byPropertyName);
            }
            String columnType = this.columnTypes.get(i);
            byPropertyName.put(column.getPropertyName(), new ColumnInfo(i, columnType));
        }
        if (columns != null && selectorNames.isEmpty()) {
            String selectorName = DEFAULT_SELECTOR_NAME;
            selectorNames.add(selectorName);
            locationIndexBySelectorName.put(selectorName, 0);
        }
        this.selectorNames = Collections.unmodifiableList(selectorNames);
        this.columnNames = Collections.unmodifiableList(names);
        if (includeFullTextSearchScores) {
            this.fullTextSearchScoreIndexBySelectorName = new HashMap<String, Integer>();
            int numSelectors = selectorNames.size();
            for (String selectorName : selectorNames) {
                int index = locationIndexBySelectorName.get(selectorName).intValue() + numSelectors;
                fullTextSearchScoreIndexBySelectorName.put(selectorName, new Integer(index));
            }
            this.tupleSize = columnNames.size() + selectorNames.size() + selectorNames.size();
        } else {
            this.fullTextSearchScoreIndexBySelectorName = null;
            this.tupleSize = columnNames.size() + selectorNames.size();
        }
    }

    private QueryResultColumns( List<Column> columns,
                                QueryResultColumns wrappedAround ) {
        assert columns != null;
        this.columns = Collections.unmodifiableList(columns);
        this.columnIndexByColumnName = new HashMap<String, Integer>();
        this.locationIndexBySelectorName = new HashMap<String, Integer>();
        this.locationIndexByColumnIndex = new HashMap<Integer, Integer>();
        this.locationIndexByColumnName = new HashMap<String, Integer>();
        this.columnIndexByPropertyNameBySelectorName = new HashMap<String, Map<String, ColumnInfo>>();
        this.propertyNameByColumnName = new HashMap<String, String>();
        this.selectorNameByColumnName = new HashMap<String, String>();
        this.selectorNames = new ArrayList<String>(columns.size());
        List<String> types = new ArrayList<String>(columns.size());
        List<String> names = new ArrayList<String>(columns.size());
        Set<Column> sameNameColumns = findColumnsWithSameNames(this.columns);
        this.locationStartIndexInTuple = wrappedAround.getLocationStartIndexInTuple();

        // Find all the selector names ...
        for (int i = 0, max = this.columns.size(); i != max; ++i) {
            Column column = this.columns.get(i);
            assert column != null;
            String selectorName = column.selectorName().name();
            if (!selectorNames.contains(selectorName)) {
                selectorNames.add(selectorName);
            }
        }

        for (int i = 0, max = this.columns.size(); i != max; ++i) {
            Column column = this.columns.get(i);
            assert column != null;
            String selectorName = column.selectorName().name();
            String columnName = columnNameFor(column, names, sameNameColumns, selectorNames);
            assert columnName != null;
            propertyNameByColumnName.put(columnName, column.getPropertyName());
            selectorNameByColumnName.put(columnName, selectorName);
            Integer columnIndex = wrappedAround.columnIndexForName(columnName);
            if (columnIndex == null) {
                String columnNameWithoutSelector = column.getColumnName() != null ? column.getColumnName() : column.getPropertyName();
                if (columnNameWithoutSelector.startsWith(selectorName + ".")
                    && columnNameWithoutSelector.length() > (selectorName.length() + 1)) {
                    columnNameWithoutSelector = columnNameWithoutSelector.substring(selectorName.length() + 1);
                }
                columnIndex = wrappedAround.columnIndexForName(columnNameWithoutSelector);
                if (columnIndex == null) {
                    String columnNameWithSelector = column.selectorName() + "." + columnNameWithoutSelector;
                    columnIndex = wrappedAround.columnIndexForName(columnNameWithSelector);
                }
            }
            assert columnIndex != null;
            columnIndexByColumnName.put(columnName, columnIndex);
            String columnType = wrappedAround.getColumnTypes().get(columnIndex.intValue());
            types.add(columnType);
            Integer selectorIndex = new Integer(wrappedAround.getLocationIndex(selectorName));
            locationIndexBySelectorName.put(selectorName, selectorIndex);
            locationIndexByColumnIndex.put(columnIndex, selectorIndex);
            locationIndexByColumnName.put(columnName, selectorIndex);
            // Insert the entry by selector name and property name ...
            Map<String, ColumnInfo> byPropertyName = columnIndexByPropertyNameBySelectorName.get(selectorName);
            if (byPropertyName == null) {
                byPropertyName = new HashMap<String, ColumnInfo>();
                columnIndexByPropertyNameBySelectorName.put(selectorName, byPropertyName);
            }
            byPropertyName.put(column.getPropertyName(), new ColumnInfo(columnIndex, columnType));
        }
        if (selectorNames.isEmpty()) {
            String selectorName = DEFAULT_SELECTOR_NAME;
            selectorNames.add(selectorName);
            locationIndexBySelectorName.put(selectorName, 0);
        }
        this.columnNames = Collections.unmodifiableList(names);
        this.columnTypes = Collections.unmodifiableList(types);
        if (wrappedAround.fullTextSearchScoreIndexBySelectorName != null) {
            this.fullTextSearchScoreIndexBySelectorName = new HashMap<String, Integer>();
            for (String selectorName : selectorNames) {
                Integer selectorIndex = new Integer(wrappedAround.getFullTextSearchScoreIndexFor(selectorName));
                fullTextSearchScoreIndexBySelectorName.put(selectorName, selectorIndex);
            }
            this.tupleSize = columnNames.size() + selectorNames.size() + selectorNames.size();
        } else {
            this.fullTextSearchScoreIndexBySelectorName = null;
            this.tupleSize = columnNames.size() + selectorNames.size();
        }
    }

    public static boolean includeFullTextScores( Iterable<Constraint> constraints ) {
        for (Constraint constraint : constraints) {
            if (includeFullTextScores(constraint)) return true;
        }
        return false;
    }

    public static boolean includeFullTextScores( Constraint constraint ) {
        final AtomicBoolean includeFullTextScores = new AtomicBoolean(false);
        if (constraint != null) {
            Visitors.visitAll(constraint, new Visitors.AbstractVisitor() {
                @Override
                public void visit( FullTextSearch obj ) {
                    includeFullTextScores.set(true);
                }
            });
        }
        return includeFullTextScores.get();
    }

    @Override
    public int getLocationStartIndexInTuple() {
        return this.locationStartIndexInTuple;
    }

    @Override
    public Columns subSelect( List<Column> columns ) {
        return new QueryResultColumns(columns, this);
    }

    @Override
    public Columns subSelect( Column... columns ) {
        return new QueryResultColumns(Arrays.asList(columns), this);
    }

    @Override
    public Columns joinWith( Columns rightColumns ) {
        if (this == rightColumns) return this;
        List<Column> columns = new ArrayList<Column>(this.getColumnCount() + rightColumns.getColumnCount());
        columns.addAll(this.getColumns());
        columns.addAll(rightColumns.getColumns());
        List<String> types = new ArrayList<String>(this.getColumnCount() + rightColumns.getColumnCount());
        types.addAll(this.getColumnTypes());
        types.addAll(rightColumns.getColumnTypes());
        boolean includeFullTextScores = this.hasFullTextSearchScores() || rightColumns.hasFullTextSearchScores();
        return new QueryResultColumns(columns, types, includeFullTextScores);
    }

    @Override
    public List<? extends Column> getColumns() {
        return columns;
    }

    @Override
    @SuppressWarnings( "unchecked" )
    public Iterator<Column> iterator() {
        return (Iterator<Column>)getColumns().iterator();
    }

    @Override
    public List<String> getColumnNames() {
        return columnNames;
    }

    @Override
    public List<String> getColumnTypes() {
        return columnTypes;
    }

    @Override
    public int getColumnCount() {
        return columns.size();
    }

    @Override
    public int getLocationCount() {
        return selectorNames.size();
    }

    @Override
    public List<String> getSelectorNames() {
        return selectorNames;
    }

    @Override
    public int getTupleSize() {
        return tupleSize;
    }

    @Override
    public List<String> getTupleValueNames() {
        if (this.tupleValueNames == null) {
            // This is idempotent, so no need to lock ...
            List<String> results = new ArrayList<String>(getTupleSize());
            // Add the column names ...
            results.addAll(columnNames);
            // Add the location names ...
            for (String selectorName : selectorNames) {
                String name = "Location(" + selectorName + ")";
                results.add(name);
            }
            // Add the full-text search score names ...
            if (fullTextSearchScoreIndexBySelectorName != null) {
                for (String selectorName : selectorNames) {
                    String name = "Score(" + selectorName + ")";
                    results.add(name);
                }
            }
            this.tupleValueNames = results;
        }
        return this.tupleValueNames;
    }

    @Override
    public int getLocationIndexForColumn( int columnIndex ) {
        if (locationIndexByColumnIndex.isEmpty()) return 0;
        Integer result = locationIndexByColumnIndex.get(new Integer(columnIndex));
        if (result == null) {
            throw new IndexOutOfBoundsException(GraphI18n.columnDoesNotExistInQuery.text(columnIndex));
        }
        return result.intValue();
    }

    @Override
    public int getLocationIndexForColumn( String columnName ) {
        if (locationIndexByColumnName.isEmpty()) return 0;
        Integer result = locationIndexByColumnName.get(columnName);
        if (result == null) {
            throw new NoSuchElementException(GraphI18n.columnDoesNotExistInQuery.text(columnName));
        }
        return result.intValue();
    }

    @Override
    public int getLocationIndex( String selectorName ) {
        Integer result = locationIndexBySelectorName.get(selectorName);
        if (result == null) {
            throw new NoSuchElementException(GraphI18n.selectorDoesNotExistInQuery.text(selectorName));
        }
        return result.intValue();
    }

    @Override
    public boolean hasSelector( String selectorName ) {
        return locationIndexBySelectorName.containsKey(selectorName);
    }

    @Override
    public String getPropertyNameForColumn( int columnIndex ) {
        return columns.get(columnIndex).getPropertyName();
    }

    @Override
    public String getPropertyNameForColumnName( String columnName ) {
        String result = propertyNameByColumnName.get(columnName);
        return result != null ? result : columnName;
    }

    @Override
    public int getColumnIndexForName( String columnName ) {
        Integer result = columnIndexByColumnName.get(columnName);
        if (result == null) {
            throw new NoSuchElementException(GraphI18n.columnDoesNotExistInQuery.text(columnName));
        }
        return result.intValue();
    }

    protected Integer columnIndexForName( String columnName ) {
        return columnIndexByColumnName.get(columnName);
    }

    @Override
    public String getSelectorNameForColumnName( String columnName ) {
        return selectorNameByColumnName.get(columnName);
    }

    @Override
    public int getColumnIndexForProperty( String selectorName,
                                          String propertyName ) {
        Map<String, ColumnInfo> byPropertyName = columnIndexByPropertyNameBySelectorName.get(selectorName);
        if (byPropertyName == null) {
            throw new NoSuchElementException(GraphI18n.selectorDoesNotExistInQuery.text(selectorName));
        }
        ColumnInfo result = byPropertyName.get(propertyName);
        if (result == null) {
            throw new NoSuchElementException(GraphI18n.propertyOnSelectorIsNotUsedInQuery.text(propertyName, selectorName));
        }
        return result.columnIndex;
    }

    @Override
    public String getColumnTypeForProperty( String selectorName,
                                            String propertyName ) {
        Map<String, ColumnInfo> byPropertyName = columnIndexByPropertyNameBySelectorName.get(selectorName);
        if (byPropertyName == null) {
            throw new NoSuchElementException(GraphI18n.selectorDoesNotExistInQuery.text(selectorName));
        }
        ColumnInfo result = byPropertyName.get(propertyName);
        if (result == null) {
            throw new NoSuchElementException(GraphI18n.propertyOnSelectorIsNotUsedInQuery.text(propertyName, selectorName));
        }
        return result.type;
    }

    @Override
    public int getFullTextSearchScoreIndexFor( String selectorName ) {
        if (fullTextSearchScoreIndexBySelectorName == null) return -1;
        Integer result = fullTextSearchScoreIndexBySelectorName.get(selectorName);
        if (result == null) {
            throw new NoSuchElementException(GraphI18n.selectorDoesNotExistInQuery.text(selectorName));
        }
        return result.intValue();
    }

    @Override
    public boolean hasFullTextSearchScores() {
        return fullTextSearchScoreIndexBySelectorName != null;
    }

    @Override
    public boolean includes( Columns other ) {
        if (other == this) return true;
        if (other == null) return false;
        return this.getColumns().containsAll(other.getColumns());
    }

    @Override
    public boolean isUnionCompatible( Columns other ) {
        if (this == other) return true;
        if (other == null) return false;
        if (this.hasFullTextSearchScores() != other.hasFullTextSearchScores()) return false;
        if (this.getColumnCount() != other.getColumnCount()) return false;
        return this.getColumns().containsAll(other.getColumns()) && other.getColumns().containsAll(this.getColumns());
    }

    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof QueryResultColumns) {
            QueryResultColumns that = (QueryResultColumns)obj;
            return this.getColumns().equals(that.getColumns());
        }
        return false;
    }

    protected static String columnNameFor( Column column,
                                           List<String> columnNames,
                                           Set<Column> columnsWithDuplicateNames,
                                           Collection<String> selectorNames ) {
        String columnName = column.getColumnName() != null ? column.getColumnName() : column.getPropertyName();
        boolean qualified = columnName != null ? columnName.startsWith(column.getSelectorName() + ".") : false;
        boolean aliased = columnName != null ? !columnName.equals(column.getPropertyName()) : false;
        if (column.getPropertyName() == null || columnNames.contains(columnName) || columnsWithDuplicateNames.contains(column)) {
            // Per section 6.7.39 of the JSR-283 specification, if the property name for a column is not given
            // then the name for the column in the result set must be "selectorName.propertyName" ...
            columnName = column.selectorName() + "." + columnName;
        } else if (!qualified && !aliased && selectorNames.size() > 1) {
            // When there is more than one selector, all columns need to be named "selectorName.propertyName" ...
            columnName = column.selectorName() + "." + columnName;
        }
        columnNames.add(columnName);
        return columnName;
    }

    protected static Set<Column> findColumnsWithSameNames( List<Column> columns ) {
        Multimap<String, Column> columnNames = ArrayListMultimap.create();
        for (Column column : columns) {
            String columnName = column.getColumnName() != null ? column.getColumnName() : column.getPropertyName();
            columnNames.put(columnName, column);
        }
        Set<Column> results = new HashSet<Column>();
        for (Map.Entry<String, Collection<Column>> entry : columnNames.asMap().entrySet()) {
            if (entry.getValue().size() > 1) {
                results.addAll(entry.getValue());
            }
        }
        return results;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        boolean first = true;
        Iterator<String> nameIter = this.columnNames.iterator();
        for (Column column : getColumns()) {
            if (first) first = false;
            else sb.append(", ");
            sb.append(column);
            sb.append('(').append(getColumnIndexForName(nameIter.next())).append(')');
        }
        sb.append("] => Locations[");
        first = true;
        for (int i = 0, count = getColumnCount(); i != count; ++i) {
            if (first) first = false;
            else sb.append(", ");
            sb.append(getLocationIndexForColumn(i));
        }
        sb.append(']');
        return sb.toString();
    }
}
