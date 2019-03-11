/*
 * ModeShape (http://www.modeshape.org)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.modeshape.jcr.query.validate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.annotation.NotThreadSafe;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.ExecutionContext;
import org.modeshape.jcr.GraphI18n;
import org.modeshape.jcr.NodeTypes;
import org.modeshape.jcr.RepositoryIndexes;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.query.BufferManager;
import org.modeshape.jcr.query.QueryContext;
import org.modeshape.jcr.query.model.QueryCommand;
import org.modeshape.jcr.query.model.SelectorName;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.query.model.Visitors;
import org.modeshape.jcr.query.parse.BasicSqlQueryParser;
import org.modeshape.jcr.query.parse.InvalidQueryException;
import org.modeshape.jcr.query.plan.CanonicalPlanner;
import org.modeshape.jcr.query.plan.PlanHints;
import org.modeshape.jcr.query.plan.PlanNode;
import org.modeshape.jcr.query.plan.PlanNode.Property;
import org.modeshape.jcr.value.PropertyType;

/**
 * An immutable {@link Schemata} implementation.
 */
@Immutable
public class ImmutableSchemata implements Schemata {

    /**
     * Obtain a new instance for building Schemata objects.
     * 
     * @param context the execution context that this schemata should use
     * @param nodeTypes the node types that this schemata should use
     * @return the new builder; never null
     * @throws IllegalArgumentException if the context is null
     */
    public static Builder createBuilder( ExecutionContext context,
                                         NodeTypes nodeTypes ) {
        CheckArg.isNotNull(context, "context");
        CheckArg.isNotNull(nodeTypes, "nodeTypes");
        return new Builder(context, nodeTypes);
    }

    /**
     * A builder of immutable {@link Schemata} objects.
     */
    @NotThreadSafe
    public static class Builder {

        private final ExecutionContext context;
        private final NodeTypes nodeTypes;
        private final TypeSystem typeSystem;
        private final Map<String, MutableTable> tables = new HashMap<>();
        private final Map<SelectorName, QueryCommand> viewDefinitions = new HashMap<>();
        private final Set<SelectorName> tablesOrViewsWithExtraColumns = new HashSet<>();
        private final Map<String, Map<String, Boolean>> orderableColumnsByTableName = new HashMap<>();
        private final Map<String, Map<String, Set<Operator>>> operatorsForColumnsByTableName = new HashMap<>();

        protected Builder( ExecutionContext context,
                           NodeTypes nodeTypes ) {
            this.context = context;
            this.nodeTypes = nodeTypes;
            this.typeSystem = context.getValueFactories().getTypeSystem();
        }

        /**
         * Add a table with the supplied name and column names. Each column will be given a default type. The table will also
         * overwrite any existing table definition with the same name.
         * 
         * @param name the name of the new table
         * @param columnNames the names of the columns.
         * @return this builder, for convenience in method chaining; never null
         * @throws IllegalArgumentException if the table name is null or empty, any column name is null or empty, or if no column
         *         names are given
         */
        public Builder addTable( String name,
                                 String... columnNames ) {
            CheckArg.isNotEmpty(name, "name");
            CheckArg.isNotEmpty(columnNames, "columnNames");
            List<Column> columns = new ArrayList<>();
            int i = 0;
            for (String columnName : columnNames) {
                CheckArg.isNotEmpty(columnName, "columnName[" + (i++) + "]");
                columns.add(new ImmutableColumn(columnName, typeSystem.getDefaultType()));
            }
            MutableTable table = new MutableTable(name, columns, false);
            tables.put(name, table);
            return this;
        }

        /**
         * Add a table with the supplied name and column names and types. The table will also overwrite any existing table
         * definition with the same name.
         * 
         * @param name the name of the new table
         * @param columnNames the names of the columns
         * @param types the types for the columns
         * @return this builder, for convenience in method chaining; never null
         * @throws IllegalArgumentException if the table name is null or empty, any column name is null or empty, if no column
         *         names are given, or if the number of types does not match the number of columns
         */
        public Builder addTable( String name,
                                 String[] columnNames,
                                 String[] types ) {
            CheckArg.isNotEmpty(name, "name");
            CheckArg.isNotEmpty(columnNames, "columnNames");
            CheckArg.isNotEmpty(types, "types");
            CheckArg.isEquals(columnNames.length, "columnNames.length", types.length, "types.length");
            List<Column> columns = new ArrayList<>();
            assert columnNames.length == types.length;
            for (int i = 0; i != columnNames.length; ++i) {
                String columnName = columnNames[i];
                CheckArg.isNotEmpty(columnName, "columnName[" + i + "]");
                columns.add(new ImmutableColumn(columnName, types[i]));
            }
            MutableTable table = new MutableTable(name, columns, false);
            tables.put(name, table);
            return this;
        }

        /**
         * Add a view with the supplied name and SQL string definition. The column names and types will be inferred from the
         * source table(s) and views(s) used in the definition.
         * 
         * @param name the name of the new view
         * @param definition the SQL definition of the view
         * @return this builder, for convenience in method chaining; never null
         * @throws IllegalArgumentException if the view name is null or empty or the definition is null
         * @throws ParsingException if the supplied definition is cannot be parsed as a SQL query
         */
        public Builder addView( String name,
                                String definition ) {
            CheckArg.isNotEmpty(name, "name");
            CheckArg.isNotEmpty(definition, "definition");
            BasicSqlQueryParser parser = new BasicSqlQueryParser();
            QueryCommand command = parser.parseQuery(definition, typeSystem);
            this.viewDefinitions.put(new SelectorName(name), command);
            return this;
        }

        /**
         * Add a view with the supplied name and definition. The column names and types will be inferred from the source table(s)
         * used in the definition.
         * 
         * @param name the name of the new view
         * @param definition the definition of the view
         * @return this builder, for convenience in method chaining; never null
         * @throws IllegalArgumentException if the view name is null or empty or the definition is null
         */
        public Builder addView( String name,
                                QueryCommand definition ) {
            CheckArg.isNotEmpty(name, "name");
            CheckArg.isNotNull(definition, "definition");
            this.viewDefinitions.put(new SelectorName(name), definition);
            return this;
        }

        /**
         * Add a column with the supplied name and type to the named table. Any existing column with that name will be replaced
         * with the new column. If the table does not yet exist, it will be added.
         * 
         * @param tableName the name of the new table
         * @param columnName the names of the column
         * @param type the type for the column
         * @return this builder, for convenience in method chaining; never null
         * @throws IllegalArgumentException if the table name is null or empty, any column name is null or empty, if no column
         *         names are given, or if the number of types does not match the number of columns
         */
        public Builder addColumn( String tableName,
                                  String columnName,
                                  String type ) {
            CheckArg.isNotEmpty(tableName, "tableName");
            CheckArg.isNotEmpty(columnName, "columnName");
            CheckArg.isNotNull(type, "type");
            return addColumn(tableName, columnName, type, ImmutableColumn.DEFAULT_REQUIRED_TYPE,
                             ImmutableColumn.DEFAULT_FULL_TEXT_SEARCHABLE, ImmutableColumn.DEFAULT_ORDERABLE, null, null,
                             ImmutableColumn.ALL_OPERATORS);
        }

        /**
         * Add a column with the supplied name and type to the named table. Any existing column with that name will be replaced
         * with the new column. If the table does not yet exist, it will be added.
         * 
         * @param tableName the name of the new table
         * @param columnName the names of the column
         * @param type the type for the column
         * @param requiredType the requiredType for the column; never null
         * @param fullTextSearchable true if the column should be full-text searchable, or false if not
         * @param orderable true if the column can be used in order clauses, or false if not
         * @param minimum the minimum value for the column; may be null
         * @param maximum the maximum value for the column; may be null
         * @param operations the set operations that can be applied to this column within comparisons; may be empty or null if all
         *        operations apply
         * @return this builder, for convenience in method chaining; never null
         * @throws IllegalArgumentException if the table name is null or empty, the column name is null or empty, or if the
         *         property type is null
         */
        public Builder addColumn( String tableName,
                                  String columnName,
                                  String type,
                                  PropertyType requiredType,
                                  boolean fullTextSearchable,
                                  boolean orderable,
                                  Object minimum,
                                  Object maximum,
                                  Set<Operator> operations ) {
            CheckArg.isNotEmpty(tableName, "tableName");
            CheckArg.isNotEmpty(columnName, "columnName");
            CheckArg.isNotNull(type, "type");
            MutableTable existing = tables.get(tableName);
            Column column = new ImmutableColumn(columnName, type, requiredType, fullTextSearchable, orderable, minimum, maximum,
                                                operations);
            if (existing == null) {
                List<Column> columns = new ArrayList<>();
                columns.add(column);
                existing = new MutableTable(tableName, columns, false);
                tables.put(tableName, existing);
            } else {
                existing.addColumn(column);
            }
            return this;
        }

        /**
         * Make sure the column on the named table is searchable.
         * 
         * @param tableName the name of the new table
         * @param columnName the names of the column
         * @return this builder, for convenience in method chaining; never null
         * @throws IllegalArgumentException if the table name is null or empty or if the column name is null or empty
         */
        public Builder makeSearchable( String tableName,
                                       String columnName ) {
            CheckArg.isNotEmpty(tableName, "tableName");
            CheckArg.isNotEmpty(columnName, "columnName");
            MutableTable existing = tables.get(tableName);
            if (existing == null) {
                List<Column> columns = new ArrayList<>();
                columns.add(new ImmutableColumn(columnName, typeSystem.getDefaultType(), ImmutableColumn.DEFAULT_REQUIRED_TYPE,
                                                true));
                existing = new MutableTable(tableName, columns, false);
                tables.put(tableName, existing);
            } else {
                Column column = existing.getColumn(columnName);
                if (column != null && !column.isFullTextSearchable()) {
                    boolean orderable = column.isOrderable();
                    Set<Operator> operators = column.getOperators();
                    column = new ImmutableColumn(columnName, column.getPropertyTypeName(), column.getRequiredType(), true,
                                                 orderable, column.getMinimum(), column.getMaximum(), operators);
                }
                existing.addColumn(column);
            }
            return this;
        }

        /**
         * Record whether the column on the named table should be orderable.
         * 
         * @param tableName the name of the new table
         * @param columnName the names of the column
         * @param orderable true if the column should be orderable, or false otherwise
         * @return this builder, for convenience in method chaining; never null
         */
        public Builder markOrderable( String tableName,
                                      String columnName,
                                      boolean orderable ) {
            CheckArg.isNotEmpty(tableName, "tableName");
            Map<String, Boolean> byColumnNames = orderableColumnsByTableName.get(tableName);
            if (byColumnNames == null) {
                byColumnNames = new HashMap<>();
                orderableColumnsByTableName.put(tableName, byColumnNames);
            }
            byColumnNames.put(columnName, orderable);
            return this;
        }

        protected boolean orderable( SelectorName tableName,
                                     String columnName,
                                     boolean defaultValue ) {
            Map<String, Boolean> byColumnNames = orderableColumnsByTableName.get(tableName.getString());
            if (byColumnNames != null) {
                Boolean value = byColumnNames.get(columnName);
                if (value != null) return value.booleanValue();
            }
            return defaultValue;
        }

        /**
         * Record the operators that are allowed for the named column on the named table.
         * 
         * @param tableName the name of the new table
         * @param columnName the names of the column
         * @param operators the set of operators, or null or empty if the default operators should be used
         * @return this builder, for convenience in method chaining; never null
         */
        public Builder markOperators( String tableName,
                                      String columnName,
                                      Set<Operator> operators ) {
            CheckArg.isNotEmpty(tableName, "tableName");
            boolean useDefaults = operators == null || operators.isEmpty();
            Map<String, Set<Operator>> byColumnNames = operatorsForColumnsByTableName.get(tableName);
            if (byColumnNames == null) {
                if (useDefaults) return this;
                byColumnNames = new HashMap<>();
                operatorsForColumnsByTableName.put(tableName, byColumnNames);
            }
            if (useDefaults) {
                byColumnNames.remove(columnName);
                if (byColumnNames.isEmpty()) {
                    // Nothing more for any of the columns, so remove the table from the map ...
                    operatorsForColumnsByTableName.remove(tableName);
                }
            } else {
                Set<Operator> opSet = EnumSet.copyOf(operators);
                byColumnNames.put(columnName, opSet);
            }
            return this;
        }

        protected Set<Operator> operators( SelectorName tableName,
                                           String columnName,
                                           Set<Operator> defaultOperators ) {
            Map<String, Set<Operator>> byColumnNames = operatorsForColumnsByTableName.get(tableName.getString());
            if (byColumnNames != null) {
                Set<Operator> ops = byColumnNames.get(columnName);
                if (ops != null) return ops;
            }
            return defaultOperators;

        }

        /**
         * Make sure the column on the named table has extra columns that can be used without validation error.
         * 
         * @param tableName the name of the table
         * @return this builder, for convenience in method chaining; never null
         * @throws IllegalArgumentException if the table name is null or empty, or the table does not exist
         */
        public Builder markExtraColumns( String tableName ) {
            CheckArg.isNotEmpty(tableName, "tableName");
            tablesOrViewsWithExtraColumns.add(new SelectorName(tableName));
            return this;
        }

        /**
         * Specify that the named column in the given table should be excluded from the selected columns when "SELECT *" is used.
         * 
         * @param tableName the name of the new table
         * @param columnName the names of the column
         * @return this builder, for convenience in method chaining; never null
         * @throws IllegalArgumentException if the table name is null or empty or if the column name is null or empty
         */
        public Builder excludeFromSelectStar( String tableName,
                                              String columnName ) {
            CheckArg.isNotEmpty(tableName, "tableName");
            CheckArg.isNotEmpty(columnName, "columnName");
            MutableTable existing = tables.get(tableName);
            if (existing == null) {
                List<Column> columns = new ArrayList<>();
                columns.add(new ImmutableColumn(columnName, typeSystem.getDefaultType()));
                existing = new MutableTable(tableName, columns, false);
                tables.put(tableName, existing);
            }
            existing.excludeFromSelectStar(columnName);
            return this;
        }

        /**
         * Add to the specified table a key that references the existing named columns.
         * 
         * @param tableName the name of the new table
         * @param columnNames the names of the (existing) columns that make up the key
         * @return this builder, for convenience in method chaining; never null
         * @throws IllegalArgumentException if the table name is null or empty, the array of column names is null or empty, or if
         *         the column names do not reference existing columns in the table
         */
        public Builder addKey( String tableName,
                               String... columnNames ) {
            CheckArg.isNotEmpty(tableName, "tableName");
            CheckArg.isNotEmpty(columnNames, "columnNames");
            MutableTable existing = tables.get(tableName);
            if (existing == null) {
                throw new IllegalArgumentException(GraphI18n.tableDoesNotExist.text(tableName));
            }
            Set<Column> keyColumns = new HashSet<>();
            for (String columnName : columnNames) {
                Column existingColumn = existing.getColumn(columnName);
                if (existingColumn == null) {
                    String msg = GraphI18n.schemataKeyReferencesNonExistingColumn.text(tableName, columnName);
                    throw new IllegalArgumentException(msg);
                }
                keyColumns.add(existingColumn);
            }
            existing.addKey(keyColumns);
            return this;
        }

        /**
         * Build the {@link Schemata} instance, using the current state of the builder. This method creates a snapshot of the
         * tables (with their columns) as they exist at the moment this method is called.
         * 
         * @return the new Schemata; never null
         * @throws InvalidQueryException if any of the view definitions is invalid and cannot be resolved
         */
        public Schemata build() {
            Map<SelectorName, Table> tablesByName = new HashMap<>();
            // Add all the tables ...
            for (MutableTable mutableTable : tables.values()) {
                if (tablesOrViewsWithExtraColumns.contains(mutableTable.getName())) {
                    mutableTable.setExtraColumns(true);
                }
                Table table = mutableTable.asImmutable();
                tablesByName.put(table.getName(), table);
            }
            ImmutableSchemata schemata = new ImmutableSchemata(context, tablesByName);

            // Make a copy of the view definitions, and create the views ...
            Map<SelectorName, QueryCommand> definitions = new HashMap<>(viewDefinitions);
            boolean added = false;
            BufferManager bufferManager = new BufferManager(context);
            do {
                added = false;
                Set<SelectorName> viewNames = new HashSet<>(definitions.keySet());
                for (SelectorName name : viewNames) {
                    QueryCommand command = definitions.get(name);
                    // Create the canonical plan for the definition ...
                    PlanHints hints = new PlanHints();
                    hints.validateColumnExistance = false;
                    // Create a query context that queries all workspaces (we won't actually query using it) ...
                    Set<String> allWorkspaces = Collections.emptySet();
                    RepositoryIndexes indexDefns = RepositoryIndexes.NO_INDEXES;
                    QueryContext queryContext = new QueryContext(context, null, allWorkspaces, schemata, indexDefns, nodeTypes,
                                                                 bufferManager, hints, null);
                    CanonicalPlanner planner = new CanonicalPlanner();
                    PlanNode plan = planner.createPlan(queryContext, command);
                    if (queryContext.getProblems().hasErrors()) {
                        continue;
                    }

                    // Get the columns from the top-level PROJECT ...
                    PlanNode project = plan.findAtOrBelow(PlanNode.Type.PROJECT);
                    assert project != null;
                    List<org.modeshape.jcr.query.model.Column> columns = project.getPropertyAsList(Property.PROJECT_COLUMNS,
                                                                                                   org.modeshape.jcr.query.model.Column.class);
                    assert !columns.isEmpty();

                    // Go through all the columns and look up the types ...
                    Map<SelectorName, SelectorName> tableNameByAlias = null;
                    List<Column> viewColumns = new ArrayList<>(columns.size());
                    List<Column> viewColumnsInSelectStar = new ArrayList<>(columns.size());
                    Set<String> columnNames = new HashSet<>();
                    for (org.modeshape.jcr.query.model.Column column : columns) {
                        // Find the table that the column came from ...
                        Table source = schemata.getTable(column.selectorName());
                        if (source == null) {
                            // The column may be referring to the alias of the table ...
                            if (tableNameByAlias == null) {
                                tableNameByAlias = Visitors.getSelectorNamesByAlias(command);
                            }
                            SelectorName tableName = tableNameByAlias.get(column.selectorName());
                            if (tableName != null) source = schemata.getTable(tableName);
                            if (source == null) {
                                continue;
                            }
                        }
                        String viewColumnName = column.getColumnName();
                        if (viewColumnName.equals(column.getSelectorName() + "." + column.getPropertyName())) {
                            viewColumnName = column.getPropertyName();
                        }
                        if (columnNames.contains(viewColumnName)) continue;
                        String sourceColumnName = column.getPropertyName(); // getColumnName() returns alias
                        Column sourceColumn = source.getColumn(sourceColumnName);
                        if (sourceColumn == null) {
                            sourceColumn = source.getColumn(column.getColumnName());
                            if (sourceColumn == null) {
                                throw new InvalidQueryException(Visitors.readable(command),
                                                                "The view references a non-existant column '"
                                                                + column.getColumnName() + "' in '" + source.getName() + "'");
                            }
                        }
                        Set<Operator> operators = operators(name, viewColumnName, sourceColumn.getOperators());
                        boolean orderable = orderable(name, viewColumnName, sourceColumn.isOrderable());
                        Column newColumn = new ImmutableColumn(viewColumnName, sourceColumn.getPropertyTypeName(),
                                                               sourceColumn.getRequiredType(),
                                                               sourceColumn.isFullTextSearchable(), orderable,
                                                               sourceColumn.getMinimum(), sourceColumn.getMaximum(), operators);
                        viewColumns.add(newColumn);
                        if (source.getSelectAllColumnsByName().containsKey(sourceColumnName)) {
                            viewColumnsInSelectStar.add(newColumn);
                        }
                        columnNames.add(newColumn.getName());
                    }
                    // if (viewColumns.size() != columns.size()) {
                    // // We weren't able to resolve all of the columns,
                    // // so maybe the columns were referencing yet-to-be-built views ...
                    // continue;
                    // }

                    // If we could resolve the definition ...
                    Map<String, Column> viewColumnsByName = new HashMap<>();
                    Map<String, Column> viewSelectStarColumnsByName = new HashMap<>();
                    for (Column column : viewColumns) {
                        viewColumnsByName.put(column.getName(), column);
                    }
                    for (Column column : viewColumnsInSelectStar) {
                        viewSelectStarColumnsByName.put(column.getName(), column);
                    }
                    Set<Key> keys = Collections.emptySet();
                    boolean hasExtraColumns = tablesOrViewsWithExtraColumns.contains(name);
                    ImmutableView view = new ImmutableView(name, viewColumnsByName, viewColumns, hasExtraColumns, command, keys,
                                                           viewSelectStarColumnsByName, viewColumnsInSelectStar);
                    definitions.remove(name);

                    tablesByName.put(view.getName(), view);
                    schemata = new ImmutableSchemata(context, tablesByName);
                    added = true;
                }
            } while (added && !definitions.isEmpty());

            if (!definitions.isEmpty()) {
                QueryCommand command = definitions.values().iterator().next();
                throw new InvalidQueryException(Visitors.readable(command), "The view definition cannot be resolved: "
                                                                            + Visitors.readable(command));
            }

            return schemata;
        }
    }

    private final ExecutionContext context;
    private final Map<SelectorName, Table> tables;

    protected ImmutableSchemata( ExecutionContext context, Map<SelectorName, Table> tables ) {
        this.context = context;
        this.tables = Collections.unmodifiableMap(tables);
    }

    @Override
    public Table getTable( SelectorName name ) {
        return tables.get(name.qualifiedForm(context.getValueFactories().getNameFactory()));
    }

    public ImmutableSchemata with( Table table ) {
        return new ImmutableSchemata(context, Collections.singletonMap(table.getName(), table));
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        boolean first = true;
        for (Table table : tables.values()) {
            if (first) first = false;
            else sb.append('\n');
            sb.append(table);
        }
        return sb.toString();
    }

    protected static class MutableTable {
        private final SelectorName name;
        private final Map<String, Column> columnsByName = new HashMap<>();
        private final List<Column> columns = new LinkedList<>();
        private final Set<Key> keys = new HashSet<>();
        private boolean extraColumns = false;
        private final Set<String> columnNamesNotInSelectStar = new HashSet<>();

        protected MutableTable( String name,
                                List<Column> columns,
                                boolean extraColumns ) {
            this.name = new SelectorName(name);
            this.columns.addAll(columns);
            for (Column column : columns) {
                Column existing = this.columnsByName.put(column.getName(), column);
                assert existing == null;
            }
        }

        public SelectorName getName() {
            return name;
        }

        protected void addColumn( Column column ) {
            Column existing = this.columnsByName.put(column.getName(), column);
            if (existing != null) {
                this.columns.remove(existing);
            }
            this.columns.add(column);
        }

        protected Column getColumn( String name ) {
            return columnsByName.get(name);
        }

        protected Set<String> getColumnNamesInSelectStar() {
            return columnNamesNotInSelectStar;
        }

        protected boolean addKey( Collection<Column> keyColumns ) {
            return keys.add(new ImmutableKey(keyColumns));
        }

        protected void setExtraColumns( boolean extraColumns ) {
            this.extraColumns = extraColumns;
        }

        protected void excludeFromSelectStar( String columnName ) {
            columnNamesNotInSelectStar.add(columnName);
        }

        protected Table asImmutable() {
            Map<String, Column> columnsByName = Collections.unmodifiableMap(this.columnsByName);
            List<Column> columns = Collections.unmodifiableList(this.columns);
            Set<Key> keys = Collections.unmodifiableSet(this.keys);
            List<Column> columnsInSelectStar = new ArrayList<>();
            Map<String, Column> columnsInSelectStarByName = new HashMap<>();
            for (Column column : columns) {
                if (!columnNamesNotInSelectStar.contains(column.getName())) {
                    columnsInSelectStar.add(column);
                    columnsInSelectStarByName.put(column.getName(), column);
                }
            }
            columnsInSelectStar = Collections.unmodifiableList(columnsInSelectStar);
            columnsInSelectStarByName = Collections.unmodifiableMap(columnsInSelectStarByName);
            return new ImmutableTable(name, columnsByName, columns, keys, extraColumns, columnsInSelectStarByName,
                                      columnsInSelectStar);
        }
    }

}
