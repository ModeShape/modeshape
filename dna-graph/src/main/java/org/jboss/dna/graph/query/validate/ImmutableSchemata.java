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
package org.jboss.dna.graph.query.validate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.jboss.dna.common.text.ParsingException;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.GraphI18n;
import org.jboss.dna.graph.property.PropertyType;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.model.QueryCommand;
import org.jboss.dna.graph.query.model.SelectorName;
import org.jboss.dna.graph.query.model.Visitors;
import org.jboss.dna.graph.query.parse.InvalidQueryException;
import org.jboss.dna.graph.query.parse.SqlQueryParser;
import org.jboss.dna.graph.query.plan.CanonicalPlanner;
import org.jboss.dna.graph.query.plan.PlanHints;
import org.jboss.dna.graph.query.plan.PlanNode;
import org.jboss.dna.graph.query.plan.PlanNode.Property;
import org.jboss.dna.graph.query.plan.PlanNode.Type;

/**
 * An immutable {@link Schemata} implementation.
 */
@Immutable
public class ImmutableSchemata implements Schemata {

    /**
     * Obtain a new instance for building Schemata objects.
     * 
     * @param context the execution context that can be used when building the schema
     * @return the new builder; never null
     * @throws IllegalArgumentException if the context is null
     */
    public static Builder createBuilder( ExecutionContext context ) {
        CheckArg.isNotNull(context, "context");
        return new Builder(context);
    }

    /**
     * A builder of immutable {@link Schemata} objects.
     */
    @NotThreadSafe
    public static class Builder {

        private final ExecutionContext context;
        private final Map<SelectorName, ImmutableTable> tables = new HashMap<SelectorName, ImmutableTable>();
        private final Map<SelectorName, QueryCommand> viewDefinitions = new HashMap<SelectorName, QueryCommand>();

        protected Builder( ExecutionContext context ) {
            this.context = context;
        }

        /**
         * Add a table with the supplied name and column names. Each column will be given a type of {@link PropertyType#STRING}.
         * The table will also overwrite any existing table definition with the same name.
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
            List<Column> columns = new ArrayList<Column>();
            int i = 0;
            for (String columnName : columnNames) {
                CheckArg.isNotEmpty(columnName, "columnName[" + (i++) + "]");
                columns.add(new ImmutableColumn(columnName, PropertyType.STRING));
            }
            ImmutableTable table = new ImmutableTable(new SelectorName(name), columns);
            tables.put(table.getName(), table);
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
                                 PropertyType[] types ) {
            CheckArg.isNotEmpty(name, "name");
            CheckArg.isNotEmpty(columnNames, "columnNames");
            CheckArg.isNotEmpty(types, "types");
            CheckArg.isEquals(columnNames.length, "columnNames.length", types.length, "types.length");
            List<Column> columns = new ArrayList<Column>();
            assert columnNames.length == types.length;
            for (int i = 0; i != columnNames.length; ++i) {
                String columnName = columnNames[i];
                CheckArg.isNotEmpty(columnName, "columnName[" + i + "]");
                columns.add(new ImmutableColumn(columnName, types[i]));
            }
            ImmutableTable table = new ImmutableTable(new SelectorName(name), columns);
            tables.put(table.getName(), table);
            return this;
        }

        /**
         * Add a table with the supplied name and single column name and type. The table will also overwrite any existing table
         * definition with the same name.
         * 
         * @param name the name of the new table
         * @param column1Name the name of the single column
         * @param column1Type the type for the single column
         * @return this builder, for convenience in method chaining; never null
         * @throws IllegalArgumentException if the table name is null or empty, the column name is null or empty, or if the type
         *         is null
         */
        public Builder addTable( String name,
                                 String column1Name,
                                 PropertyType column1Type ) {
            CheckArg.isNotEmpty(name, "name");
            CheckArg.isNotNull(column1Name, "column1Name");
            CheckArg.isNotNull(column1Type, "column1Type");
            List<Column> columns = new ArrayList<Column>();
            columns.add(new ImmutableColumn(column1Name, column1Type));
            ImmutableTable table = new ImmutableTable(new SelectorName(name), columns);
            tables.put(table.getName(), table);
            return this;
        }

        /**
         * Add a table with the supplied name and two column names and types. The table will also overwrite any existing table
         * definition with the same name.
         * 
         * @param name the name of the new table
         * @param column1Name the name of the first column
         * @param column1Type the type for the first column
         * @param column2Name the name of the second column
         * @param column2Type the type for the second column
         * @return this builder, for convenience in method chaining; never null
         * @throws IllegalArgumentException if the table name is null or empty, any column name is null or empty, or any of the
         *         types is null
         */
        public Builder addTable( String name,
                                 String column1Name,
                                 PropertyType column1Type,
                                 String column2Name,
                                 PropertyType column2Type ) {
            CheckArg.isNotEmpty(name, "name");
            CheckArg.isNotNull(column1Name, "column1Name");
            CheckArg.isNotNull(column1Type, "column1Type");
            CheckArg.isNotNull(column2Name, "column2Name");
            CheckArg.isNotNull(column2Type, "column2Type");
            List<Column> columns = new ArrayList<Column>();
            columns.add(new ImmutableColumn(column1Name, column1Type));
            columns.add(new ImmutableColumn(column2Name, column2Type));
            ImmutableTable table = new ImmutableTable(new SelectorName(name), columns);
            tables.put(table.getName(), table);
            return this;
        }

        /**
         * Add a table with the supplied name and three column names and types. The table will also overwrite any existing table
         * definition with the same name.
         * 
         * @param name the name of the new table
         * @param column1Name the name of the first column
         * @param column1Type the type for the first column
         * @param column2Name the name of the second column
         * @param column2Type the type for the second column
         * @param column3Name the name of the third column
         * @param column3Type the type for the third column
         * @return this builder, for convenience in method chaining; never null
         * @throws IllegalArgumentException if the table name is null or empty, any column name is null or empty, or any of the
         *         types is null
         */
        public Builder addTable( String name,
                                 String column1Name,
                                 PropertyType column1Type,
                                 String column2Name,
                                 PropertyType column2Type,
                                 String column3Name,
                                 PropertyType column3Type ) {
            CheckArg.isNotEmpty(name, "name");
            CheckArg.isNotNull(column1Name, "column1Name");
            CheckArg.isNotNull(column1Type, "column1Type");
            CheckArg.isNotNull(column2Name, "column2Name");
            CheckArg.isNotNull(column2Type, "column2Type");
            CheckArg.isNotNull(column3Name, "column3Name");
            CheckArg.isNotNull(column3Type, "column3Type");
            List<Column> columns = new ArrayList<Column>();
            columns.add(new ImmutableColumn(column1Name, column1Type));
            columns.add(new ImmutableColumn(column2Name, column2Type));
            columns.add(new ImmutableColumn(column3Name, column3Type));
            ImmutableTable table = new ImmutableTable(new SelectorName(name), columns);
            tables.put(table.getName(), table);
            return this;
        }

        /**
         * Add a table with the supplied name and four column names and types. The table will also overwrite any existing table
         * definition with the same name.
         * 
         * @param name the name of the new table
         * @param column1Name the name of the first column
         * @param column1Type the type for the first column
         * @param column2Name the name of the second column
         * @param column2Type the type for the second column
         * @param column3Name the name of the third column
         * @param column3Type the type for the third column
         * @param column4Name the name of the fourth column
         * @param column4Type the type for the fourth column
         * @return this builder, for convenience in method chaining; never null
         * @throws IllegalArgumentException if the table name is null or empty, any column name is null or empty, or any of the
         *         types is null
         */
        public Builder addTable( String name,
                                 String column1Name,
                                 PropertyType column1Type,
                                 String column2Name,
                                 PropertyType column2Type,
                                 String column3Name,
                                 PropertyType column3Type,
                                 String column4Name,
                                 PropertyType column4Type ) {
            CheckArg.isNotEmpty(name, "name");
            CheckArg.isNotNull(column1Name, "column1Name");
            CheckArg.isNotNull(column1Type, "column1Type");
            CheckArg.isNotNull(column2Name, "column2Name");
            CheckArg.isNotNull(column2Type, "column2Type");
            CheckArg.isNotNull(column3Name, "column3Name");
            CheckArg.isNotNull(column3Type, "column3Type");
            CheckArg.isNotNull(column4Name, "column4Name");
            CheckArg.isNotNull(column4Type, "column4Type");
            List<Column> columns = new ArrayList<Column>();
            columns.add(new ImmutableColumn(column1Name, column1Type));
            columns.add(new ImmutableColumn(column2Name, column2Type));
            columns.add(new ImmutableColumn(column3Name, column3Type));
            columns.add(new ImmutableColumn(column4Name, column4Type));
            ImmutableTable table = new ImmutableTable(new SelectorName(name), columns);
            tables.put(table.getName(), table);
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
            SqlQueryParser parser = new SqlQueryParser();
            QueryCommand command = parser.parseQuery(definition, context);
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
                                  PropertyType type ) {
            CheckArg.isNotEmpty(tableName, "tableName");
            CheckArg.isNotEmpty(columnName, "columnName");
            CheckArg.isNotNull(type, "type");
            SelectorName selector = new SelectorName(tableName);
            ImmutableTable existing = tables.get(selector);
            ImmutableTable table = null;
            if (existing == null) {
                List<Column> columns = new ArrayList<Column>();
                columns.add(new ImmutableColumn(columnName, type));
                table = new ImmutableTable(selector, columns);
            } else {
                table = existing.withColumn(columnName, type);
            }
            tables.put(table.getName(), table);
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
            ImmutableTable existing = tables.get(tableName);
            if (existing == null) {
                throw new IllegalArgumentException(GraphI18n.tableDoesNotExist.text(tableName));
            }
            Set<Column> keyColumns = new HashSet<Column>();
            for (String columnName : columnNames) {
                Column existingColumn = existing.getColumnsByName().get(columnName);
                if (existingColumn == null) {
                    String msg = GraphI18n.schemataKeyReferencesNonExistingColumn.text(tableName, columnName);
                    throw new IllegalArgumentException(msg);
                }
                keyColumns.add(existingColumn);
            }
            ImmutableTable table = existing.withKey(keyColumns);
            tables.put(table.getName(), table);
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
            ImmutableSchemata schemata = new ImmutableSchemata(new HashMap<SelectorName, Table>(tables));

            // Make a copy of the view definitions, and create the views ...
            Map<SelectorName, QueryCommand> definitions = new HashMap<SelectorName, QueryCommand>(viewDefinitions);
            ValueFactory<String> stringFactory = context.getValueFactories().getStringFactory();
            boolean added = false;
            do {
                added = false;
                Set<SelectorName> viewNames = new HashSet<SelectorName>(definitions.keySet());
                for (SelectorName name : viewNames) {
                    QueryCommand command = definitions.get(name);
                    // Create the canonical plan for the definition ...
                    QueryContext queryContext = new QueryContext(context, new PlanHints(), schemata);
                    CanonicalPlanner planner = new CanonicalPlanner();
                    PlanNode plan = planner.createPlan(queryContext, command);
                    if (queryContext.getProblems().hasErrors()) continue;

                    // Get the columns from the top-level PROJECT ...
                    PlanNode project = plan.findAtOrBelow(Type.PROJECT);
                    assert project != null;
                    List<org.jboss.dna.graph.query.model.Column> columns = project.getPropertyAsList(Property.PROJECT_COLUMNS,
                                                                                                     org.jboss.dna.graph.query.model.Column.class);
                    assert !columns.isEmpty();

                    // Go through all the columns and look up the types ...
                    List<Column> viewColumns = new ArrayList<Column>(columns.size());
                    for (org.jboss.dna.graph.query.model.Column column : columns) {
                        // Find the table that the column came from ...
                        Table source = schemata.getTable(column.getSelectorName());
                        if (source == null) break;
                        String viewColumnName = column.getColumnName();
                        String sourceColumnName = stringFactory.create(column.getPropertyName()); // getColumnName() returns alias
                        Column sourceColumn = source.getColumn(sourceColumnName);
                        if (sourceColumn == null) {
                            throw new InvalidQueryException(Visitors.readable(command),
                                                            "The view references a non-existant column '"
                                                            + column.getColumnName() + "' in '" + source.getName() + "'");
                        }
                        viewColumns.add(new ImmutableColumn(viewColumnName, sourceColumn.getPropertyType()));
                    }
                    if (viewColumns.size() != columns.size()) {
                        // We weren't able to resolve all of the columns,
                        // so maybe the columns were referencing yet-to-be-built views ...
                        continue;
                    }

                    // If we could resolve the definition ...
                    ImmutableView view = new ImmutableView(name, viewColumns, command);
                    definitions.remove(name);
                    schemata = schemata.with(view);
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

    private final Map<SelectorName, Table> tables;

    protected ImmutableSchemata( Map<SelectorName, Table> tables ) {
        this.tables = Collections.unmodifiableMap(tables);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.validate.Schemata#getTable(org.jboss.dna.graph.query.model.SelectorName)
     */
    public Table getTable( SelectorName name ) {
        return tables.get(name);
    }

    public ImmutableSchemata with( Table table ) {
        Map<SelectorName, Table> tables = new HashMap<SelectorName, Table>(this.tables);
        tables.put(table.getName(), table);
        return new ImmutableSchemata(tables);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
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

}
