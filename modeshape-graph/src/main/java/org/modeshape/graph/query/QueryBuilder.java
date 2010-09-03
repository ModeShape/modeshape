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
package org.modeshape.graph.query;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import net.jcip.annotations.NotThreadSafe;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PropertyType;
import org.modeshape.graph.query.model.AllNodes;
import org.modeshape.graph.query.model.And;
import org.modeshape.graph.query.model.ArithmeticOperand;
import org.modeshape.graph.query.model.ArithmeticOperator;
import org.modeshape.graph.query.model.Between;
import org.modeshape.graph.query.model.BindVariableName;
import org.modeshape.graph.query.model.ChildNode;
import org.modeshape.graph.query.model.ChildNodeJoinCondition;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.model.Comparison;
import org.modeshape.graph.query.model.Constraint;
import org.modeshape.graph.query.model.DescendantNode;
import org.modeshape.graph.query.model.DescendantNodeJoinCondition;
import org.modeshape.graph.query.model.DynamicOperand;
import org.modeshape.graph.query.model.EquiJoinCondition;
import org.modeshape.graph.query.model.FullTextSearch;
import org.modeshape.graph.query.model.FullTextSearchScore;
import org.modeshape.graph.query.model.Join;
import org.modeshape.graph.query.model.JoinCondition;
import org.modeshape.graph.query.model.JoinType;
import org.modeshape.graph.query.model.Length;
import org.modeshape.graph.query.model.Limit;
import org.modeshape.graph.query.model.Literal;
import org.modeshape.graph.query.model.LowerCase;
import org.modeshape.graph.query.model.NamedSelector;
import org.modeshape.graph.query.model.NodeDepth;
import org.modeshape.graph.query.model.NodeLocalName;
import org.modeshape.graph.query.model.NodeName;
import org.modeshape.graph.query.model.NodePath;
import org.modeshape.graph.query.model.Not;
import org.modeshape.graph.query.model.Operator;
import org.modeshape.graph.query.model.Or;
import org.modeshape.graph.query.model.Order;
import org.modeshape.graph.query.model.Ordering;
import org.modeshape.graph.query.model.PropertyExistence;
import org.modeshape.graph.query.model.PropertyValue;
import org.modeshape.graph.query.model.Query;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.ReferenceValue;
import org.modeshape.graph.query.model.SameNode;
import org.modeshape.graph.query.model.SameNodeJoinCondition;
import org.modeshape.graph.query.model.Selector;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.SetCriteria;
import org.modeshape.graph.query.model.SetQuery;
import org.modeshape.graph.query.model.Source;
import org.modeshape.graph.query.model.StaticOperand;
import org.modeshape.graph.query.model.Subquery;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.model.UpperCase;
import org.modeshape.graph.query.model.Visitors;
import org.modeshape.graph.query.model.SetQuery.Operation;

/**
 * A component that can be used to programmatically create {@link QueryCommand} objects. Simply call methods to build the selector
 * clause, from clause, join criteria, where criteria, limits, and ordering, and then {@link #query() obtain the query}. This
 * builder should be adequate for most queries; however, any query that cannot be expressed by this builder can always be
 * constructed by directly creating the Abstract Query Model classes.
 * <p>
 * This builder is stateful and therefore should only be used by one thread at a time. However, once a query has been built, the
 * builder can be {@link #clear() cleared} and used to create another query.
 * </p>
 * <p>
 * The order in which the methods are called are (for the most part) important. Simply call the methods in the same order that
 * would be most natural in a normal SQL query. For example, the following code creates a Query object that is equivalent to "
 * <code>SELECT * FROM table</code>":
 * 
 * <pre>
 * QueryCommand query = builder.selectStar().from(&quot;table&quot;).query();
 * </pre>
 * 
 * </p>
 * <p>
 * Here are a few other examples:
 * <table border="1" cellspacing="0" cellpadding="3" summary="">
 * <tr>
 * <th>SQL Statement</th>
 * <th>QueryBuilder code</th>
 * </tr>
 * <tr>
 * <td>
 * 
 * <pre>
 * SELECT * FROM table1
 *    INNER JOIN table2
 *            ON table2.c0 = table1.c0
 * </pre>
 * 
 * </td>
 * <td>
 * 
 * <pre>
 * query = builder.selectStar().from(&quot;table1&quot;).join(&quot;table2&quot;).on(&quot;table2.c0=table1.c0&quot;).query();
 * </pre>
 * 
 * </td>
 * </tr>
 * <tr>
 * <td>
 * 
 * <pre>
 * SELECT * FROM table1 AS t1
 *    INNER JOIN table2 AS t2
 *            ON t1.c0 = t2.c0
 * </pre>
 * 
 * </td>
 * <td>
 * 
 * <pre>
 * query = builder.selectStar().from(&quot;table1 AS t1&quot;).join(&quot;table2 AS t2&quot;).on(&quot;t1.c0=t2.c0&quot;).query();
 * </pre>
 * 
 * </td>
 * </tr>
 * <tr>
 * <td>
 * 
 * <pre>
 * SELECT * FROM table1 AS t1
 *    INNER JOIN table2 AS t2
 *            ON t1.c0 = t2.c0
 *    INNER JOIN table3 AS t3
 *            ON t1.c1 = t3.c1
 * </pre>
 * 
 * </td>
 * <td>
 * 
 * <pre>
 * query = builder.selectStar()
 *                .from(&quot;table1 AS t1&quot;)
 *                .innerJoin(&quot;table2 AS t2&quot;)
 *                .on(&quot;t1.c0=t2.c0&quot;)
 *                .innerJoin(&quot;table3 AS t3&quot;)
 *                .on(&quot;t1.c1=t3.c1&quot;)
 *                .query();
 * </pre>
 * 
 * </td>
 * </tr>
 * <tr>
 * <td>
 * 
 * <pre>
 * SELECT * FROM table1
 * UNION
 * SELECT * FROM table2
 * </pre>
 * 
 * </td>
 * <td>
 * 
 * <pre>
 * query = builder.selectStar().from(&quot;table1&quot;).union().selectStar().from(&quot;table2&quot;).query();
 * </pre>
 * 
 * </td>
 * </tr>
 * <tr>
 * <td>
 * 
 * <pre>
 * SELECT t1.c1,t1.c2,t2.c3 FROM table1 AS t1
 *    INNER JOIN table2 AS t2
 *            ON t1.c0 = t2.c0
 * UNION ALL
 * SELECT t3.c1,t3.c2,t4.c3 FROM table3 AS t3
 *    INNER JOIN table4 AS t4
 *            ON t3.c0 = t4.c0
 * </pre>
 * 
 * </td>
 * <td>
 * 
 * <pre>
 * query = builder.select(&quot;t1.c1&quot;,&quot;t1.c2&quot;,&quot;t2.c3&quot;,)
 *                .from(&quot;table1 AS t1&quot;)
 *                .innerJoin(&quot;table2 AS t2&quot;)
 *                .on(&quot;t1.c0=t2.c0&quot;)
 *                .union()
 *                .select(&quot;t3.c1&quot;,&quot;t3.c2&quot;,&quot;t4.c3&quot;,)
 *                .from(&quot;table3 AS t3&quot;)
 *                .innerJoin(&quot;table4 AS t4&quot;)
 *                .on(&quot;t3.c0=t4.c0&quot;)
 *                .query();
 * </pre>
 * 
 * </td>
 * </tr>
 * </table>
 * </pre>
 */
@NotThreadSafe
public class QueryBuilder {

    protected final TypeSystem typeSystem;
    protected Source source = new AllNodes();
    protected Constraint constraint;
    protected List<Column> columns = new LinkedList<Column>();
    protected List<Ordering> orderings = new LinkedList<Ordering>();
    protected Limit limit = Limit.NONE;
    protected boolean distinct;
    protected QueryCommand firstQuery;
    protected Operation firstQuerySetOperation;
    protected boolean firstQueryAll;

    /**
     * Create a new builder that uses the supplied execution context.
     * 
     * @param context the execution context
     * @throws IllegalArgumentException if the context is null
     */
    public QueryBuilder( TypeSystem context ) {
        CheckArg.isNotNull(context, "context");
        this.typeSystem = context;
    }

    /**
     * Clear this builder completely to start building a new query.
     * 
     * @return this builder object, for convenience in method chaining
     */
    public QueryBuilder clear() {
        return clear(true);
    }

    /**
     * Utility method that does all the work of the clear, but with a flag that defines whether to clear the first query. This
     * method is used by {@link #clear()} as well as the {@link #union() many} {@link #intersect() set} {@link #except()
     * operations}.
     * 
     * @param clearFirstQuery true if the first query should be cleared, or false if the first query should be retained
     * @return this builder object, for convenience in method chaining
     */
    protected QueryBuilder clear( boolean clearFirstQuery ) {
        source = new AllNodes();
        constraint = null;
        columns = new LinkedList<Column>();
        orderings = new LinkedList<Ordering>();
        limit = Limit.NONE;
        distinct = false;
        if (clearFirstQuery) {
            this.firstQuery = null;
            this.firstQuerySetOperation = null;
        }
        return this;
    }

    /**
     * Convenience method that creates a selector name object using the supplied string.
     * 
     * @param name the name of the selector; may not be null
     * @return the selector name; never null
     */
    protected SelectorName selector( String name ) {
        return new SelectorName(name.trim());
    }

    /**
     * Convenience method that creates a {@link NamedSelector} object given a string that contains the selector name and
     * optionally an alias. The format of the string parameter is <code>name [AS alias]</code>. Leading and trailing whitespace
     * are trimmed.
     * 
     * @param nameWithOptionalAlias the name and optional alias; may not be null
     * @return the named selector object; never null
     */
    protected NamedSelector namedSelector( String nameWithOptionalAlias ) {
        String[] parts = nameWithOptionalAlias.split("\\sAS\\s");
        if (parts.length == 2) {
            return new NamedSelector(selector(parts[0]), selector(parts[1]));
        }
        return new NamedSelector(selector(parts[0]));
    }

    /**
     * Create a {@link Column} given the supplied expression. The expression has the form "<code>[tableName.]columnName</code>",
     * where "<code>tableName</code>" must be a valid table name or alias. If the table name/alias is not specified, then there is
     * expected to be a single FROM clause with a single named selector.
     * 
     * @param nameExpression the expression specifying the columm name and (optionally) the table's name or alias; may not be null
     * @return the column; never null
     * @throws IllegalArgumentException if the table's name/alias is not specified, but the query has more than one named source
     */
    protected Column column( String nameExpression ) {
        String[] parts = nameExpression.split("(?<!\\\\)\\."); // a . not preceded by an escaping slash
        for (int i = 0; i != parts.length; ++i) {
            parts[i] = parts[i].trim();
        }
        SelectorName name = null;
        String propertyName = null;
        String columnName = null;
        if (parts.length == 2) {
            name = selector(parts[0]);
            propertyName = parts[1];
            columnName = parts[1];
        } else {
            if (source instanceof Selector) {
                Selector selector = (Selector)source;
                name = selector.hasAlias() ? selector.alias() : selector.name();
                propertyName = parts[0];
                columnName = parts[0];
            } else {
                throw new IllegalArgumentException(GraphI18n.columnMustBeScoped.text(parts[0]));
            }
        }
        return new Column(name, propertyName, columnName);
    }

    /**
     * Select all of the single-valued columns.
     * 
     * @return this builder object, for convenience in method chaining
     */
    public QueryBuilder selectStar() {
        columns.clear();
        return this;
    }

    /**
     * Add to the select clause the columns with the supplied names. Each column name has the form "
     * <code>[tableName.]columnName</code>", where " <code>tableName</code>" must be a valid table name or alias. If the table
     * name/alias is not specified, then there is expected to be a single FROM clause with a single named selector.
     * 
     * @param columnNames the column expressions; may not be null
     * @return this builder object, for convenience in method chaining
     * @throws IllegalArgumentException if the table's name/alias is not specified, but the query has more than one named source
     */
    public QueryBuilder select( String... columnNames ) {
        for (String expression : columnNames) {
            columns.add(column(expression));
        }
        return this;
    }

    /**
     * Select all of the distinct values from the single-valued columns.
     * 
     * @return this builder object, for convenience in method chaining
     */
    public QueryBuilder selectDistinctStar() {
        distinct = true;
        return selectStar();
    }

    /**
     * Select the distinct values from the columns with the supplied names. Each column name has the form "
     * <code>[tableName.]columnName</code>", where " <code>tableName</code>" must be a valid table name or alias. If the table
     * name/alias is not specified, then there is expected to be a single FROM clause with a single named selector.
     * 
     * @param columnNames the column expressions; may not be null
     * @return this builder object, for convenience in method chaining
     * @throws IllegalArgumentException if the table's name/alias is not specified, but the query has more than one named source
     */
    public QueryBuilder selectDistinct( String... columnNames ) {
        distinct = true;
        return select(columnNames);
    }

    /**
     * Specify that the query should select from the "__ALLNODES__" built-in table.
     * 
     * @return this builder object, for convenience in method chaining
     */
    public QueryBuilder fromAllNodes() {
        this.source = new AllNodes();
        return this;
    }

    /**
     * Specify that the query should select from the "__ALLNODES__" built-in table using the supplied alias.
     * 
     * @param alias the alias for the "__ALL_NODES" table; may not be null
     * @return this builder object, for convenience in method chaining
     */
    public QueryBuilder fromAllNodesAs( String alias ) {
        AllNodes allNodes = new AllNodes(selector(alias));
        SelectorName oldName = this.source instanceof Selector ? ((Selector)source).name() : null;
        // Go through the columns and change the selector name to use the new alias ...
        for (int i = 0; i != columns.size(); ++i) {
            Column old = columns.get(i);
            if (old.selectorName().equals(oldName)) {
                columns.set(i, new Column(allNodes.aliasOrName(), old.propertyName(), old.columnName()));
            }
        }
        this.source = allNodes;
        return this;
    }

    /**
     * Specify the name of the table from which tuples should be selected. The supplied string is of the form "
     * <code>tableName [AS alias]</code>".
     * 
     * @param tableNameWithOptionalAlias the name of the table, optionally including the alias
     * @return this builder object, for convenience in method chaining
     */
    public QueryBuilder from( String tableNameWithOptionalAlias ) {
        Selector selector = namedSelector(tableNameWithOptionalAlias);
        SelectorName oldName = this.source instanceof Selector ? ((Selector)source).name() : null;
        // Go through the columns and change the selector name to use the new alias ...
        for (int i = 0; i != columns.size(); ++i) {
            Column old = columns.get(i);
            if (old.selectorName().equals(oldName)) {
                columns.set(i, new Column(selector.aliasOrName(), old.propertyName(), old.columnName()));
            }
        }
        this.source = selector;
        return this;
    }

    /**
     * Begin the WHERE clause for this query by obtaining the constraint builder. When completed, be sure to call
     * {@link ConstraintBuilder#end() end()} on the resulting constraint builder, or else the constraint will not be applied to
     * the current query.
     * 
     * @return the constraint builder that can be used to specify the criteria; never null
     */
    public ConstraintBuilder where() {
        return new ConstraintBuilder(null);
    }

    /**
     * Perform an inner join between the already defined source with the supplied table. The supplied string is of the form "
     * <code>tableName [AS alias]</code>".
     * 
     * @param tableName the name of the table, optionally including the alias
     * @return the component that must be used to complete the join specification; never null
     */
    public JoinClause join( String tableName ) {
        return innerJoin(tableName);
    }

    /**
     * Perform an inner join between the already defined source with the supplied table. The supplied string is of the form "
     * <code>tableName [AS alias]</code>".
     * 
     * @param tableName the name of the table, optionally including the alias
     * @return the component that must be used to complete the join specification; never null
     */
    public JoinClause innerJoin( String tableName ) {
        // Expect there to be a source already ...
        return new JoinClause(namedSelector(tableName), JoinType.INNER);
    }

    /**
     * Perform a cross join between the already defined source with the supplied table. The supplied string is of the form "
     * <code>tableName [AS alias]</code>". Cross joins have a higher precedent than other join types, so if this is called after
     * another join was defined, the resulting cross join will be between the previous join's right-hand side and the supplied
     * table.
     * 
     * @param tableName the name of the table, optionally including the alias
     * @return the component that must be used to complete the join specification; never null
     */
    public JoinClause crossJoin( String tableName ) {
        // Expect there to be a source already ...
        return new JoinClause(namedSelector(tableName), JoinType.CROSS);
    }

    /**
     * Perform a full outer join between the already defined source with the supplied table. The supplied string is of the form "
     * <code>tableName [AS alias]</code>".
     * 
     * @param tableName the name of the table, optionally including the alias
     * @return the component that must be used to complete the join specification; never null
     */
    public JoinClause fullOuterJoin( String tableName ) {
        // Expect there to be a source already ...
        return new JoinClause(namedSelector(tableName), JoinType.FULL_OUTER);
    }

    /**
     * Perform a left outer join between the already defined source with the supplied table. The supplied string is of the form "
     * <code>tableName [AS alias]</code>".
     * 
     * @param tableName the name of the table, optionally including the alias
     * @return the component that must be used to complete the join specification; never null
     */
    public JoinClause leftOuterJoin( String tableName ) {
        // Expect there to be a source already ...
        return new JoinClause(namedSelector(tableName), JoinType.LEFT_OUTER);
    }

    /**
     * Perform a right outer join between the already defined source with the supplied table. The supplied string is of the form "
     * <code>tableName [AS alias]</code>".
     * 
     * @param tableName the name of the table, optionally including the alias
     * @return the component that must be used to complete the join specification; never null
     */
    public JoinClause rightOuterJoin( String tableName ) {
        // Expect there to be a source already ...
        return new JoinClause(namedSelector(tableName), JoinType.RIGHT_OUTER);
    }

    /**
     * Perform an inner join between the already defined source with the "__ALLNODES__" table using the supplied alias.
     * 
     * @param alias the alias for the "__ALL_NODES" table; may not be null
     * @return the component that must be used to complete the join specification; never null
     */
    public JoinClause joinAllNodesAs( String alias ) {
        return innerJoinAllNodesAs(alias);
    }

    /**
     * Perform an inner join between the already defined source with the "__ALL_NODES" table using the supplied alias.
     * 
     * @param alias the alias for the "__ALL_NODES" table; may not be null
     * @return the component that must be used to complete the join specification; never null
     */
    public JoinClause innerJoinAllNodesAs( String alias ) {
        // Expect there to be a source already ...
        return new JoinClause(namedSelector(AllNodes.ALL_NODES_NAME + " AS " + alias), JoinType.INNER);
    }

    /**
     * Perform a cross join between the already defined source with the "__ALL_NODES" table using the supplied alias. Cross joins
     * have a higher precedent than other join types, so if this is called after another join was defined, the resulting cross
     * join will be between the previous join's right-hand side and the supplied table.
     * 
     * @param alias the alias for the "__ALL_NODES" table; may not be null
     * @return the component that must be used to complete the join specification; never null
     */
    public JoinClause crossJoinAllNodesAs( String alias ) {
        // Expect there to be a source already ...
        return new JoinClause(namedSelector(AllNodes.ALL_NODES_NAME + " AS " + alias), JoinType.CROSS);
    }

    /**
     * Perform a full outer join between the already defined source with the "__ALL_NODES" table using the supplied alias.
     * 
     * @param alias the alias for the "__ALL_NODES" table; may not be null
     * @return the component that must be used to complete the join specification; never null
     */
    public JoinClause fullOuterJoinAllNodesAs( String alias ) {
        // Expect there to be a source already ...
        return new JoinClause(namedSelector(AllNodes.ALL_NODES_NAME + " AS " + alias), JoinType.FULL_OUTER);
    }

    /**
     * Perform a left outer join between the already defined source with the "__ALL_NODES" table using the supplied alias.
     * 
     * @param alias the alias for the "__ALL_NODES" table; may not be null
     * @return the component that must be used to complete the join specification; never null
     */
    public JoinClause leftOuterJoinAllNodesAs( String alias ) {
        // Expect there to be a source already ...
        return new JoinClause(namedSelector(AllNodes.ALL_NODES_NAME + " AS " + alias), JoinType.LEFT_OUTER);
    }

    /**
     * Perform a right outer join between the already defined source with the "__ALL_NODES" table using the supplied alias.
     * 
     * @param alias the alias for the "__ALL_NODES" table; may not be null
     * @return the component that must be used to complete the join specification; never null
     */
    public JoinClause rightOuterJoinAllNodesAs( String alias ) {
        // Expect there to be a source already ...
        return new JoinClause(namedSelector(AllNodes.ALL_NODES_NAME + " AS " + alias), JoinType.RIGHT_OUTER);
    }

    /**
     * Specify the maximum number of rows that are to be returned in the results. By default there is no limit.
     * 
     * @param rowLimit the maximum number of rows
     * @return this builder object, for convenience in method chaining
     * @throws IllegalArgumentException if the row limit is not a positive integer
     */
    public QueryBuilder limit( int rowLimit ) {
        this.limit.withRowLimit(rowLimit);
        return this;
    }

    /**
     * Specify the number of rows that results are to skip. The default offset is '0'.
     * 
     * @param offset the number of rows before the results are to begin
     * @return this builder object, for convenience in method chaining
     * @throws IllegalArgumentException if the row limit is a negative integer
     */
    public QueryBuilder offset( int offset ) {
        this.limit.withOffset(offset);
        return this;
    }

    /**
     * Perform a UNION between the query as defined prior to this method and the query that will be defined following this method.
     * 
     * @return this builder object, for convenience in method chaining
     */
    public QueryBuilder union() {
        this.firstQuery = query();
        this.firstQuerySetOperation = Operation.UNION;
        this.firstQueryAll = false;
        clear(false);
        return this;
    }

    /**
     * Perform a UNION ALL between the query as defined prior to this method and the query that will be defined following this
     * method.
     * 
     * @return this builder object, for convenience in method chaining
     */
    public QueryBuilder unionAll() {
        this.firstQuery = query();
        this.firstQuerySetOperation = Operation.UNION;
        this.firstQueryAll = true;
        clear(false);
        return this;
    }

    /**
     * Perform an INTERSECT between the query as defined prior to this method and the query that will be defined following this
     * method.
     * 
     * @return this builder object, for convenience in method chaining
     */
    public QueryBuilder intersect() {
        this.firstQuery = query();
        this.firstQuerySetOperation = Operation.INTERSECT;
        this.firstQueryAll = false;
        clear(false);
        return this;
    }

    /**
     * Perform an INTERSECT ALL between the query as defined prior to this method and the query that will be defined following
     * this method.
     * 
     * @return this builder object, for convenience in method chaining
     */
    public QueryBuilder intersectAll() {
        this.firstQuery = query();
        this.firstQuerySetOperation = Operation.INTERSECT;
        this.firstQueryAll = true;
        clear(false);
        return this;
    }

    /**
     * Perform an EXCEPT between the query as defined prior to this method and the query that will be defined following this
     * method.
     * 
     * @return this builder object, for convenience in method chaining
     */
    public QueryBuilder except() {
        this.firstQuery = query();
        this.firstQuerySetOperation = Operation.EXCEPT;
        this.firstQueryAll = false;
        clear(false);
        return this;
    }

    /**
     * Perform an EXCEPT ALL between the query as defined prior to this method and the query that will be defined following this
     * method.
     * 
     * @return this builder object, for convenience in method chaining
     */
    public QueryBuilder exceptAll() {
        this.firstQuery = query();
        this.firstQuerySetOperation = Operation.EXCEPT;
        this.firstQueryAll = true;
        clear(false);
        return this;
    }

    /**
     * Obtain a builder that will create the order-by clause (with one or more {@link Ordering} statements) for the query. This
     * method need be called only once to build the order-by clause, but can be called multiple times (it merely adds additional
     * {@link Ordering} statements).
     * 
     * @return the order-by builder; never null
     */
    public OrderByBuilder orderBy() {
        return new OrderByBuilder();
    }

    /**
     * Return a {@link QueryCommand} representing the currently-built query.
     * 
     * @return the resulting query command; never null
     * @see #clear()
     */
    public QueryCommand query() {
        QueryCommand result = new Query(source, constraint, orderings, columns, limit, distinct);
        if (this.firstQuery != null) {
            // EXCEPT has a higher precedence than INTERSECT or UNION, so if the first query is
            // an INTERSECT or UNION SetQuery, the result should be applied to the RHS of the previous set ...
            if (firstQuery instanceof SetQuery && firstQuerySetOperation == Operation.EXCEPT) {
                SetQuery setQuery = (SetQuery)firstQuery;
                QueryCommand left = setQuery.left();
                QueryCommand right = setQuery.right();
                SetQuery exceptQuery = new SetQuery(right, Operation.EXCEPT, result, firstQueryAll);
                result = new SetQuery(left, setQuery.operation(), exceptQuery, setQuery.isAll());
            } else {
                result = new SetQuery(this.firstQuery, this.firstQuerySetOperation, result, this.firstQueryAll);
            }
        }
        return result;
    }

    public interface OrderByOperandBuilder {
        /**
         * Adds to the order-by clause by using the length of the value for the given table and property.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @param property the name of the property; may not be null and must refer to a valid property name
         * @return the interface for completing the order-by specification; never null
         */
        public OrderByBuilder length( String table,
                                      String property );

        /**
         * Adds to the order-by clause by using the value for the given table and property.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @param property the name of the property; may not be null and must refer to a valid property name
         * @return the interface for completing the order-by specification; never null
         */
        public OrderByBuilder propertyValue( String table,
                                             String property );

        /**
         * Constrains the nodes in the the supplied table such that they must have a matching value for any of the node's
         * reference properties.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @return the interface for completing the order-by specification; never null
         */
        public OrderByBuilder referenceValue( String table );

        /**
         * Constrains the nodes in the the supplied table such that they must have a matching value for the named property.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @param property the name of the reference property; may be null if the constraint applies to all/any reference
         *        properties on the node
         * @return the interface for completing the order-by specification; never null
         */
        public OrderByBuilder referenceValue( String table,
                                              String property );

        /**
         * Adds to the order-by clause by using the full-text search score for the given table.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @return the interface for completing the order-by specification; never null
         */
        public OrderByBuilder fullTextSearchScore( String table );

        /**
         * Adds to the order-by clause by using the depth of the node given by the named table.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @return the interface for completing the order-by specification; never null
         */
        public OrderByBuilder depth( String table );

        /**
         * Adds to the order-by clause by using the path of the node given by the named table.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @return the interface for completing the order-by specification; never null
         */
        public OrderByBuilder path( String table );

        /**
         * Adds to the order-by clause by using the local name of the node given by the named table.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @return the interface for completing the order-by specification; never null
         */
        public OrderByBuilder nodeLocalName( String table );

        /**
         * Adds to the order-by clause by using the node name (including namespace) of the node given by the named table.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @return the interface for completing the order-by specification; never null
         */
        public OrderByBuilder nodeName( String table );

        /**
         * Adds to the order-by clause by using the uppercase form of the next operand.
         * 
         * @return the interface for completing the order-by specification; never null
         */
        public OrderByOperandBuilder upperCaseOf();

        /**
         * Adds to the order-by clause by using the lowercase form of the next operand.
         * 
         * @return the interface for completing the order-by specification; never null
         */
        public OrderByOperandBuilder lowerCaseOf();
    }

    /**
     * The component used to build the order-by clause. When the clause is completed, {@link #end()} should be called to return to
     * the {@link QueryBuilder} instance.
     */
    public class OrderByBuilder {

        protected OrderByBuilder() {
        }

        /**
         * Begin specifying an order-by specification using {@link Order#ASCENDING ascending order}.
         * 
         * @return the interface for specifying the operand that is to be ordered; never null
         */
        public OrderByOperandBuilder ascending() {
            return new SingleOrderByOperandBuilder(this, Order.ASCENDING);
        }

        /**
         * Begin specifying an order-by specification using {@link Order#DESCENDING descending order}.
         * 
         * @return the interface for specifying the operand that is to be ordered; never null
         */
        public OrderByOperandBuilder descending() {
            return new SingleOrderByOperandBuilder(this, Order.DESCENDING);
        }

        /**
         * An optional convenience method that returns this builder, but which makes the code using this builder more readable.
         * 
         * @return this builder; never null
         */
        public OrderByBuilder then() {
            return this;
        }

        /**
         * Complete the order-by clause and return the QueryBuilder instance.
         * 
         * @return the query builder instance; never null
         */
        public QueryBuilder end() {
            return QueryBuilder.this;
        }
    }

    protected class SingleOrderByOperandBuilder implements OrderByOperandBuilder {
        private final Order order;
        private final OrderByBuilder builder;

        protected SingleOrderByOperandBuilder( OrderByBuilder builder,
                                               Order order ) {
            this.order = order;
            this.builder = builder;
        }

        protected OrderByBuilder addOrdering( DynamicOperand operand ) {
            Ordering ordering = new Ordering(operand, order);
            QueryBuilder.this.orderings.add(ordering);
            return builder;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.OrderByOperandBuilder#propertyValue(java.lang.String, java.lang.String)
         */
        public OrderByBuilder propertyValue( String table,
                                             String property ) {
            return addOrdering(new PropertyValue(selector(table), property));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.OrderByOperandBuilder#referenceValue(java.lang.String)
         */
        public OrderByBuilder referenceValue( String table ) {
            return addOrdering(new ReferenceValue(selector(table)));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.OrderByOperandBuilder#referenceValue(java.lang.String, java.lang.String)
         */
        public OrderByBuilder referenceValue( String table,
                                              String property ) {
            return addOrdering(new ReferenceValue(selector(table), property));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.OrderByOperandBuilder#length(java.lang.String, java.lang.String)
         */
        public OrderByBuilder length( String table,
                                      String property ) {
            return addOrdering(new Length(new PropertyValue(selector(table), property)));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.OrderByOperandBuilder#fullTextSearchScore(java.lang.String)
         */
        public OrderByBuilder fullTextSearchScore( String table ) {
            return addOrdering(new FullTextSearchScore(selector(table)));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.OrderByOperandBuilder#depth(java.lang.String)
         */
        public OrderByBuilder depth( String table ) {
            return addOrdering(new NodeDepth(selector(table)));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.OrderByOperandBuilder#path(java.lang.String)
         */
        public OrderByBuilder path( String table ) {
            return addOrdering(new NodePath(selector(table)));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.OrderByOperandBuilder#nodeName(java.lang.String)
         */
        public OrderByBuilder nodeName( String table ) {
            return addOrdering(new NodeName(selector(table)));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.OrderByOperandBuilder#nodeLocalName(java.lang.String)
         */
        public OrderByBuilder nodeLocalName( String table ) {
            return addOrdering(new NodeLocalName(selector(table)));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.OrderByOperandBuilder#lowerCaseOf()
         */
        public OrderByOperandBuilder lowerCaseOf() {
            return new SingleOrderByOperandBuilder(builder, order) {
                /**
                 * {@inheritDoc}
                 * 
                 * @see org.modeshape.graph.query.QueryBuilder.SingleOrderByOperandBuilder#addOrdering(org.modeshape.graph.query.model.DynamicOperand)
                 */
                @Override
                protected OrderByBuilder addOrdering( DynamicOperand operand ) {
                    return super.addOrdering(new LowerCase(operand));
                }
            };
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.OrderByOperandBuilder#upperCaseOf()
         */
        public OrderByOperandBuilder upperCaseOf() {
            return new SingleOrderByOperandBuilder(builder, order) {
                /**
                 * {@inheritDoc}
                 * 
                 * @see org.modeshape.graph.query.QueryBuilder.SingleOrderByOperandBuilder#addOrdering(org.modeshape.graph.query.model.DynamicOperand)
                 */
                @Override
                protected OrderByBuilder addOrdering( DynamicOperand operand ) {
                    return super.addOrdering(new UpperCase(operand));
                }
            };
        }
    }

    /**
     * Class used to specify a join clause of a query.
     * 
     * @see QueryBuilder#join(String)
     * @see QueryBuilder#innerJoin(String)
     * @see QueryBuilder#leftOuterJoin(String)
     * @see QueryBuilder#rightOuterJoin(String)
     * @see QueryBuilder#fullOuterJoin(String)
     */
    public class JoinClause {
        private final NamedSelector rightSource;
        private final JoinType type;

        protected JoinClause( NamedSelector rightTable,
                              JoinType type ) {
            this.rightSource = rightTable;
            this.type = type;
        }

        /**
         * Walk the current source or the 'rightSource' to find the named selector with the supplied name or alias
         * 
         * @param tableName the table name
         * @return the selector name matching the supplied table name; never null
         * @throws IllegalArgumentException if the table name could not be resolved
         */
        protected SelectorName nameOf( String tableName ) {
            final SelectorName name = new SelectorName(tableName);
            // Look at the right source ...
            if (rightSource.aliasOrName().equals(name)) return name;
            // Look through the left source ...
            final AtomicBoolean notFound = new AtomicBoolean(true);
            Visitors.visitAll(source, new Visitors.AbstractVisitor() {
                @Override
                public void visit( AllNodes selector ) {
                    if (notFound.get() && selector.aliasOrName().equals(name)) notFound.set(false);
                }

                @Override
                public void visit( NamedSelector selector ) {
                    if (notFound.get() && selector.aliasOrName().equals(name)) notFound.set(false);
                }
            });
            if (notFound.get()) {
                throw new IllegalArgumentException("Expected \"" + tableName + "\" to be a valid table name or alias");
            }
            return name;
        }

        /**
         * Define the join as using an equi-join criteria by specifying the expression equating two columns. Each column reference
         * must be qualified with the appropriate table name or alias.
         * 
         * @param columnEqualExpression the equality expression between the two tables; may not be null
         * @return the query builder instance, for method chaining purposes
         * @throws IllegalArgumentException if the supplied expression is not an equality expression
         */
        public QueryBuilder on( String columnEqualExpression ) {
            String[] parts = columnEqualExpression.split("=");
            if (parts.length != 2) {
                throw new IllegalArgumentException("Expected equality expression for columns, but found \""
                                                   + columnEqualExpression + "\"");
            }
            return createJoin(new EquiJoinCondition(column(parts[0]), column(parts[1])));
        }

        /**
         * Define the join criteria to require the two tables represent the same node. The supplied tables must be a valid name or
         * alias.
         * 
         * @param table1 the name or alias of the first table
         * @param table2 the name or alias of the second table
         * @return the query builder instance, for method chaining purposes
         */
        public QueryBuilder onSameNode( String table1,
                                        String table2 ) {
            return createJoin(new SameNodeJoinCondition(nameOf(table1), nameOf(table2)));
        }

        /**
         * Define the join criteria to require the node in one table is a descendant of the node in another table. The supplied
         * tables must be a valid name or alias.
         * 
         * @param ancestorTable the name or alias of the table containing the ancestor node
         * @param descendantTable the name or alias of the table containing the descendant node
         * @return the query builder instance, for method chaining purposes
         */
        public QueryBuilder onDescendant( String ancestorTable,
                                          String descendantTable ) {
            return createJoin(new DescendantNodeJoinCondition(nameOf(ancestorTable), nameOf(descendantTable)));
        }

        /**
         * Define the join criteria to require the node in one table is a child of the node in another table. The supplied tables
         * must be a valid name or alias.
         * 
         * @param parentTable the name or alias of the table containing the parent node
         * @param childTable the name or alias of the table containing the child node
         * @return the query builder instance, for method chaining purposes
         */
        public QueryBuilder onChildNode( String parentTable,
                                         String childTable ) {
            return createJoin(new ChildNodeJoinCondition(nameOf(parentTable), nameOf(childTable)));
        }

        protected QueryBuilder createJoin( JoinCondition condition ) {
            // CROSS joins have a higher precedence, so we may need to adjust the existing left side in this case...
            if (type == JoinType.CROSS && source instanceof Join && ((Join)source).type() != JoinType.CROSS) {
                // A CROSS join follows a non-CROSS join, so the CROSS join becomes precendent ...
                Join left = (Join)source;
                Join cross = new Join(left.right(), type, rightSource, condition);
                source = new Join(left.left(), left.type(), cross, left.joinCondition());
            } else {
                // Otherwise, just create using usual precedence ...
                source = new Join(source, type, rightSource, condition);
            }
            return QueryBuilder.this;
        }
    }

    /**
     * Interface that defines a dynamic operand portion of a criteria.
     */
    public interface DynamicOperandBuilder {
        /**
         * Constrains the nodes in the the supplied table such that they must have a property value whose length matches the
         * criteria.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @param property the name of the property; may not be null and must refer to a valid property name
         * @return the interface for completing the value portion of the criteria specification; never null
         */
        public ComparisonBuilder length( String table,
                                         String property );

        /**
         * Constrains the nodes in the the supplied table such that they must have a matching value for the named property.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @param property the name of the property; may not be null and must refer to a valid property name
         * @return the interface for completing the value portion of the criteria specification; never null
         */
        public ComparisonBuilder propertyValue( String table,
                                                String property );

        /**
         * Constrains the nodes in the the supplied table such that they must have a matching value for any of the node's
         * reference properties.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @return the interface for completing the value portion of the criteria specification; never null
         */
        public ComparisonBuilder referenceValue( String table );

        /**
         * Constrains the nodes in the the supplied table such that they must have a matching value for the named property.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @param property the name of the reference property; may be null if the constraint applies to all/any reference
         *        properties on the node
         * @return the interface for completing the value portion of the criteria specification; never null
         */
        public ComparisonBuilder referenceValue( String table,
                                                 String property );

        /**
         * Constrains the nodes in the the supplied table such that they must have a matching value for any of the node's non-weak
         * reference properties.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @return the interface for completing the value portion of the criteria specification; never null
         */
        public ComparisonBuilder strongReferenceValue( String table );

        /**
         * Constrains the nodes in the the supplied table such that they must satisfy the supplied full-text search on the nodes'
         * property values.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @return the interface for completing the value portion of the criteria specification; never null
         */
        public ComparisonBuilder fullTextSearchScore( String table );

        /**
         * Constrains the nodes in the the supplied table based upon criteria on the node's depth.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @return the interface for completing the value portion of the criteria specification; never null
         */
        public ComparisonBuilder depth( String table );

        /**
         * Constrains the nodes in the the supplied table based upon criteria on the node's path.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @return the interface for completing the value portion of the criteria specification; never null
         */
        public ComparisonBuilder path( String table );

        /**
         * Constrains the nodes in the the supplied table based upon criteria on the node's local name.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @return the interface for completing the value portion of the criteria specification; never null
         */
        public ComparisonBuilder nodeLocalName( String table );

        /**
         * Constrains the nodes in the the supplied table based upon criteria on the node's name.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @return the interface for completing the value portion of the criteria specification; never null
         */
        public ComparisonBuilder nodeName( String table );

        /**
         * Begin a constraint against the uppercase form of a dynamic operand.
         * 
         * @return the interface for completing the criteria specification; never null
         */
        public DynamicOperandBuilder upperCaseOf();

        /**
         * Begin a constraint against the lowercase form of a dynamic operand.
         * 
         * @return the interface for completing the criteria specification; never null
         */
        public DynamicOperandBuilder lowerCaseOf();
    }

    public class ConstraintBuilder implements DynamicOperandBuilder {
        private final ConstraintBuilder parent;
        /** Used for the current operations */
        private Constraint constraint;
        /** Set when a logical criteria is started */
        private Constraint left;
        private boolean and;
        private boolean negateConstraint;
        private boolean implicitParentheses = true;

        protected ConstraintBuilder( ConstraintBuilder parent ) {
            this.parent = parent;
        }

        /**
         * Complete this constraint specification.
         * 
         * @return the query builder, for method chaining purposes
         */
        public QueryBuilder end() {
            buildLogicalConstraint();
            QueryBuilder.this.constraint = constraint;
            return QueryBuilder.this;
        }

        /**
         * Simulate the use of an open parenthesis in the constraint. The resulting builder should be used to define the
         * constraint within the parenthesis, and should always be terminated with a {@link #closeParen()}.
         * 
         * @return the constraint builder that should be used to define the portion of the constraint within the parenthesis;
         *         never null
         * @see #closeParen()
         */
        public ConstraintBuilder openParen() {
            return new ConstraintBuilder(this);
        }

        /**
         * Complete the specification of a constraint clause, and return the builder for the parent constraint clause.
         * 
         * @return the constraint builder that was used to create this parenthetical constraint clause builder; never null
         * @throws IllegalStateException if there was not an {@link #openParen() open parenthesis} to close
         */
        public ConstraintBuilder closeParen() {
            if (parent == null) {
                throw new IllegalStateException(GraphI18n.unexpectedClosingParenthesis.text());
            }
            buildLogicalConstraint();
            parent.implicitParentheses = false;
            return parent.setConstraint(constraint);
        }

        /**
         * Signal that the previous constraint clause be AND-ed together with another constraint clause that will be defined
         * immediately after this method call.
         * 
         * @return the constraint builder for the remaining constraint clause; never null
         */
        public ConstraintBuilder and() {
            buildLogicalConstraint();
            left = constraint;
            constraint = null;
            and = true;
            return this;
        }

        /**
         * Signal that the previous constraint clause be OR-ed together with another constraint clause that will be defined
         * immediately after this method call.
         * 
         * @return the constraint builder for the remaining constraint clause; never null
         */
        public ConstraintBuilder or() {
            buildLogicalConstraint();
            left = constraint;
            constraint = null;
            and = false;
            return this;
        }

        /**
         * Signal that the next constraint clause (defined immediately after this method) should be negated.
         * 
         * @return the constraint builder for the constraint clause that is to be negated; never null
         */
        public ConstraintBuilder not() {
            negateConstraint = true;
            return this;
        }

        protected ConstraintBuilder buildLogicalConstraint() {
            if (negateConstraint && constraint != null) {
                constraint = new Not(constraint);
                negateConstraint = false;
            }
            if (left != null && constraint != null) {
                if (and) {
                    // If the left constraint is an OR, we need to rearrange things since AND is higher precedence ...
                    if (left instanceof Or && implicitParentheses) {
                        Or previous = (Or)left;
                        constraint = new Or(previous.left(), new And(previous.right(), constraint));
                    } else {
                        constraint = new And(left, constraint);
                    }
                } else {
                    constraint = new Or(left, constraint);
                }
                left = null;
            }
            return this;
        }

        /**
         * Define a constraint clause that the node within the named table is the same node as that appearing at the supplied
         * path.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @param asNodeAtPath the path to the node
         * @return the constraint builder that was used to create this clause; never null
         */
        public ConstraintBuilder isSameNode( String table,
                                             String asNodeAtPath ) {
            return setConstraint(new SameNode(selector(table), asNodeAtPath));
        }

        /**
         * Define a constraint clause that the node within the named table is the child of the node at the supplied path.
         * 
         * @param childTable the name of the table; may not be null and must refer to a valid name or alias of a table appearing
         *        in the FROM clause
         * @param parentPath the path to the parent node
         * @return the constraint builder that was used to create this clause; never null
         */
        public ConstraintBuilder isChild( String childTable,
                                          String parentPath ) {
            return setConstraint(new ChildNode(selector(childTable), parentPath));
        }

        /**
         * Define a constraint clause that the node within the named table is a descendant of the node at the supplied path.
         * 
         * @param descendantTable the name of the table; may not be null and must refer to a valid name or alias of a table
         *        appearing in the FROM clause
         * @param ancestorPath the path to the ancestor node
         * @return the constraint builder that was used to create this clause; never null
         */
        public ConstraintBuilder isBelowPath( String descendantTable,
                                              String ancestorPath ) {
            return setConstraint(new DescendantNode(selector(descendantTable), ancestorPath));
        }

        /**
         * Define a constraint clause that the node within the named table has at least one value for the named property.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @param propertyName the name of the property
         * @return the constraint builder that was used to create this clause; never null
         */
        public ConstraintBuilder hasProperty( String table,
                                              String propertyName ) {
            return setConstraint(new PropertyExistence(selector(table), propertyName));
        }

        /**
         * Define a constraint clause that the node within the named table have at least one property that satisfies the full-text
         * search expression.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @param searchExpression the full-text search expression
         * @return the constraint builder that was used to create this clause; never null
         */
        public ConstraintBuilder search( String table,
                                         String searchExpression ) {
            return setConstraint(new FullTextSearch(selector(table), searchExpression));
        }

        /**
         * Define a constraint clause that the node within the named table have a value for the named property that satisfies the
         * full-text search expression.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @param propertyName the name of the property to be searched
         * @param searchExpression the full-text search expression
         * @return the constraint builder that was used to create this clause; never null
         */
        public ConstraintBuilder search( String table,
                                         String propertyName,
                                         String searchExpression ) {
            return setConstraint(new FullTextSearch(selector(table), propertyName, searchExpression));
        }

        protected ComparisonBuilder comparisonBuilder( DynamicOperand operand ) {
            return new ComparisonBuilder(this, operand);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.DynamicOperandBuilder#length(java.lang.String, java.lang.String)
         */
        public ComparisonBuilder length( String table,
                                         String property ) {
            return comparisonBuilder(new Length(new PropertyValue(selector(table), property)));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.DynamicOperandBuilder#propertyValue(String, String)
         */
        public ComparisonBuilder propertyValue( String table,
                                                String property ) {
            return comparisonBuilder(new PropertyValue(selector(table), property));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.DynamicOperandBuilder#strongReferenceValue(java.lang.String)
         */
        public ComparisonBuilder strongReferenceValue( String table ) {
            return comparisonBuilder(new ReferenceValue(selector(table), null, false));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.DynamicOperandBuilder#referenceValue(java.lang.String)
         */
        public ComparisonBuilder referenceValue( String table ) {
            return comparisonBuilder(new ReferenceValue(selector(table)));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.DynamicOperandBuilder#referenceValue(java.lang.String, java.lang.String)
         */
        public ComparisonBuilder referenceValue( String table,
                                                 String property ) {
            return comparisonBuilder(new ReferenceValue(selector(table), property));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.DynamicOperandBuilder#fullTextSearchScore(String)
         */
        public ComparisonBuilder fullTextSearchScore( String table ) {
            return comparisonBuilder(new FullTextSearchScore(selector(table)));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.DynamicOperandBuilder#depth(java.lang.String)
         */
        public ComparisonBuilder depth( String table ) {
            return comparisonBuilder(new NodeDepth(selector(table)));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.DynamicOperandBuilder#path(java.lang.String)
         */
        public ComparisonBuilder path( String table ) {
            return comparisonBuilder(new NodePath(selector(table)));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.DynamicOperandBuilder#nodeLocalName(String)
         */
        public ComparisonBuilder nodeLocalName( String table ) {
            return comparisonBuilder(new NodeLocalName(selector(table)));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.DynamicOperandBuilder#nodeName(String)
         */
        public ComparisonBuilder nodeName( String table ) {
            return comparisonBuilder(new NodeName(selector(table)));
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.DynamicOperandBuilder#upperCaseOf()
         */
        public DynamicOperandBuilder upperCaseOf() {
            return new UpperCaser(this);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.QueryBuilder.DynamicOperandBuilder#lowerCaseOf()
         */
        public DynamicOperandBuilder lowerCaseOf() {
            return new LowerCaser(this);
        }

        protected ConstraintBuilder setConstraint( Constraint constraint ) {
            if (this.constraint != null && this.left == null) {
                and();
            }
            this.constraint = constraint;
            return buildLogicalConstraint();
        }
    }

    /**
     * A specialized form of the {@link ConstraintBuilder} that always wraps the generated constraint in a {@link UpperCase}
     * instance.
     */
    protected class UpperCaser extends ConstraintBuilder {
        private final ConstraintBuilder delegate;

        protected UpperCaser( ConstraintBuilder delegate ) {
            super(null);
            this.delegate = delegate;
        }

        @Override
        protected ConstraintBuilder setConstraint( Constraint constraint ) {
            Comparison comparison = (Comparison)constraint;
            return delegate.setConstraint(new Comparison(new UpperCase(comparison.operand1()), comparison.operator(),
                                                         comparison.operand2()));
        }
    }

    /**
     * A specialized form of the {@link ConstraintBuilder} that always wraps the generated constraint in a {@link LowerCase}
     * instance.
     */
    protected class LowerCaser extends ConstraintBuilder {
        private final ConstraintBuilder delegate;

        protected LowerCaser( ConstraintBuilder delegate ) {
            super(null);
            this.delegate = delegate;
        }

        @Override
        protected ConstraintBuilder setConstraint( Constraint constraint ) {
            Comparison comparison = (Comparison)constraint;
            return delegate.setConstraint(new Comparison(new LowerCase(comparison.operand1()), comparison.operator(),
                                                         comparison.operand2()));
        }
    }

    public abstract class CastAs<ReturnType> {
        protected final Object value;

        protected CastAs( Object value ) {
            this.value = value;
        }

        /**
         * Define the right-hand side literal value cast as the specified type.
         * 
         * @param type the property type; may not be null
         * @return the constraint builder; never null
         */
        public abstract ReturnType as( String type );

        /**
         * Define the right-hand side literal value cast as a {@link PropertyType#STRING}.
         * 
         * @return the constraint builder; never null
         */
        public ReturnType asString() {
            return as(typeSystem.getStringFactory().getTypeName());
        }

        /**
         * Define the right-hand side literal value cast as a {@link PropertyType#BOOLEAN}.
         * 
         * @return the constraint builder; never null
         */
        public ReturnType asBoolean() {
            return as(typeSystem.getBooleanFactory().getTypeName());
        }

        /**
         * Define the right-hand side literal value cast as a {@link PropertyType#LONG}.
         * 
         * @return the constraint builder; never null
         */
        public ReturnType asLong() {
            return as(typeSystem.getLongFactory().getTypeName());
        }

        /**
         * Define the right-hand side literal value cast as a {@link PropertyType#DOUBLE}.
         * 
         * @return the constraint builder; never null
         */
        public ReturnType asDouble() {
            return as(typeSystem.getDoubleFactory().getTypeName());
        }

        /**
         * Define the right-hand side literal value cast as a {@link PropertyType#DATE}.
         * 
         * @return the constraint builder; never null
         */
        public ReturnType asDate() {
            return as(typeSystem.getDateTimeFactory().getTypeName());
        }

        /**
         * Define the right-hand side literal value cast as a {@link PropertyType#PATH}.
         * 
         * @return the constraint builder; never null
         */
        public ReturnType asPath() {
            return as(typeSystem.getPathFactory().getTypeName());
        }
    }

    public class CastAsRightHandSide extends CastAs<ConstraintBuilder> {
        private final RightHandSide rhs;

        protected CastAsRightHandSide( RightHandSide rhs,
                                       Object value ) {
            super(value);
            this.rhs = rhs;
        }

        /**
         * Define the right-hand side literal value cast as the specified type.
         * 
         * @param type the property type; may not be null
         * @return the constraint builder; never null
         */
        @Override
        public ConstraintBuilder as( String type ) {
            return rhs.comparisonBuilder.is(rhs.operator, typeSystem.getTypeFactory(type).create(value));
        }
    }

    public class CastAsUpperBoundary extends CastAs<ConstraintBuilder> {
        private final UpperBoundary upperBoundary;

        protected CastAsUpperBoundary( UpperBoundary upperBoundary,
                                       Object value ) {
            super(value);
            this.upperBoundary = upperBoundary;
        }

        /**
         * Define the right-hand side literal value cast as the specified type.
         * 
         * @param type the property type; may not be null
         * @return the constraint builder; never null
         */
        @Override
        public ConstraintBuilder as( String type ) {
            return upperBoundary.comparisonBuilder.isBetween(upperBoundary.lowerBound, typeSystem.getTypeFactory(type)
                                                                                                 .create(value));
        }
    }

    public class CastAsLowerBoundary extends CastAs<AndBuilder<UpperBoundary>> {
        private final ComparisonBuilder builder;

        protected CastAsLowerBoundary( ComparisonBuilder builder,
                                       Object value ) {
            super(value);
            this.builder = builder;
        }

        /**
         * Define the left-hand side literal value cast as the specified type.
         * 
         * @param type the property type; may not be null
         * @return the builder to complete the constraint; never null
         */
        @Override
        public AndBuilder<UpperBoundary> as( String type ) {
            Object literal = typeSystem.getTypeFactory(type).create(value);
            return new AndBuilder<UpperBoundary>(new UpperBoundary(builder, new Literal(literal)));
        }
    }

    public class RightHandSide {
        protected final Operator operator;
        protected final ComparisonBuilder comparisonBuilder;

        protected RightHandSide( ComparisonBuilder comparisonBuilder,
                                 Operator operator ) {
            this.operator = operator;
            this.comparisonBuilder = comparisonBuilder;
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( String literal ) {
            return comparisonBuilder.is(operator, literal);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( int literal ) {
            return comparisonBuilder.is(operator, literal);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( long literal ) {
            return comparisonBuilder.is(operator, literal);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( float literal ) {
            return comparisonBuilder.is(operator, literal);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( double literal ) {
            return comparisonBuilder.is(operator, literal);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( DateTime literal ) {
            return comparisonBuilder.is(operator, literal.toUtcTimeZone());
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( Path literal ) {
            return comparisonBuilder.is(operator, literal);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( Name literal ) {
            return comparisonBuilder.is(operator, literal);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( URI literal ) {
            return comparisonBuilder.is(operator, literal);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( UUID literal ) {
            return comparisonBuilder.is(operator, literal);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( Binary literal ) {
            return comparisonBuilder.is(operator, literal);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( BigDecimal literal ) {
            return comparisonBuilder.is(operator, literal);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( boolean literal ) {
            return comparisonBuilder.is(operator, literal);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param subquery the subquery
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( QueryCommand subquery ) {
            return comparisonBuilder.is(operator, subquery);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param variableName the name of the variable
         * @return the constraint builder; never null
         */
        public ConstraintBuilder variable( String variableName ) {
            return comparisonBuilder.is(operator, variableName);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<ConstraintBuilder> cast( int literal ) {
            return new CastAsRightHandSide(this, literal);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<ConstraintBuilder> cast( String literal ) {
            return new CastAsRightHandSide(this, literal);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<ConstraintBuilder> cast( boolean literal ) {
            return new CastAsRightHandSide(this, literal);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<ConstraintBuilder> cast( long literal ) {
            return new CastAsRightHandSide(this, literal);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<ConstraintBuilder> cast( double literal ) {
            return new CastAsRightHandSide(this, literal);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<ConstraintBuilder> cast( BigDecimal literal ) {
            return new CastAsRightHandSide(this, literal);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<ConstraintBuilder> cast( DateTime literal ) {
            return new CastAsRightHandSide(this, literal.toUtcTimeZone());
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<ConstraintBuilder> cast( Name literal ) {
            return new CastAsRightHandSide(this, literal);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<ConstraintBuilder> cast( Path literal ) {
            return new CastAsRightHandSide(this, literal);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<ConstraintBuilder> cast( UUID literal ) {
            return new CastAsRightHandSide(this, literal);
        }

        /**
         * Define the right-hand side of a comparison.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<ConstraintBuilder> cast( URI literal ) {
            return new CastAsRightHandSide(this, literal);
        }
    }

    public class UpperBoundary {
        protected final StaticOperand lowerBound;
        protected final ComparisonBuilder comparisonBuilder;

        protected UpperBoundary( ComparisonBuilder comparisonBuilder,
                                 StaticOperand lowerBound ) {
            this.lowerBound = lowerBound;
            this.comparisonBuilder = comparisonBuilder;
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( String literal ) {
            return comparisonBuilder.isBetween(lowerBound, literal);
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( int literal ) {
            return comparisonBuilder.isBetween(lowerBound, literal);
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( long literal ) {
            return comparisonBuilder.isBetween(lowerBound, literal);
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( float literal ) {
            return comparisonBuilder.isBetween(lowerBound, literal);
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( double literal ) {
            return comparisonBuilder.isBetween(lowerBound, literal);
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( DateTime literal ) {
            return comparisonBuilder.isBetween(lowerBound, literal);
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( Path literal ) {
            return comparisonBuilder.isBetween(lowerBound, literal);
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( Name literal ) {
            return comparisonBuilder.isBetween(lowerBound, literal);
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( URI literal ) {
            return comparisonBuilder.isBetween(lowerBound, literal);
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( UUID literal ) {
            return comparisonBuilder.isBetween(lowerBound, literal);
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( Binary literal ) {
            return comparisonBuilder.isBetween(lowerBound, literal);
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( BigDecimal literal ) {
            return comparisonBuilder.isBetween(lowerBound, literal);
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public ConstraintBuilder literal( boolean literal ) {
            return comparisonBuilder.isBetween(lowerBound, literal);
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param variableName the name of the variable
         * @return the constraint builder; never null
         */
        public ConstraintBuilder variable( String variableName ) {
            return comparisonBuilder.constraintBuilder.setConstraint(new Between(comparisonBuilder.left, lowerBound,
                                                                                 new BindVariableName(variableName)));
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param subquery the subquery
         * @return the constraint builder; never null
         */
        public ConstraintBuilder subquery( Subquery subquery ) {
            return comparisonBuilder.constraintBuilder.setConstraint(new Between(comparisonBuilder.left, lowerBound, subquery));
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param subquery the subquery
         * @return the constraint builder; never null
         */
        public ConstraintBuilder subquery( QueryCommand subquery ) {
            return subquery(new Subquery(subquery));
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<ConstraintBuilder> cast( int literal ) {
            return new CastAsUpperBoundary(this, literal);
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<ConstraintBuilder> cast( String literal ) {
            return new CastAsUpperBoundary(this, literal);
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<ConstraintBuilder> cast( boolean literal ) {
            return new CastAsUpperBoundary(this, literal);
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<ConstraintBuilder> cast( long literal ) {
            return new CastAsUpperBoundary(this, literal);
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<ConstraintBuilder> cast( double literal ) {
            return new CastAsUpperBoundary(this, literal);
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<ConstraintBuilder> cast( BigDecimal literal ) {
            return new CastAsUpperBoundary(this, literal);
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<ConstraintBuilder> cast( DateTime literal ) {
            return new CastAsUpperBoundary(this, literal.toUtcTimeZone());
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<ConstraintBuilder> cast( Name literal ) {
            return new CastAsUpperBoundary(this, literal);
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<ConstraintBuilder> cast( Path literal ) {
            return new CastAsUpperBoundary(this, literal);
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<ConstraintBuilder> cast( UUID literal ) {
            return new CastAsUpperBoundary(this, literal);
        }

        /**
         * Define the upper boundary value of a range.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<ConstraintBuilder> cast( URI literal ) {
            return new CastAsUpperBoundary(this, literal);
        }
    }

    public class LowerBoundary {
        protected final ComparisonBuilder comparisonBuilder;

        protected LowerBoundary( ComparisonBuilder comparisonBuilder ) {
            this.comparisonBuilder = comparisonBuilder;
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public AndBuilder<UpperBoundary> literal( String literal ) {
            return new AndBuilder<UpperBoundary>(new UpperBoundary(comparisonBuilder, new Literal(literal)));
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public AndBuilder<UpperBoundary> literal( int literal ) {
            return new AndBuilder<UpperBoundary>(new UpperBoundary(comparisonBuilder, new Literal(literal)));
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public AndBuilder<UpperBoundary> literal( long literal ) {
            return new AndBuilder<UpperBoundary>(new UpperBoundary(comparisonBuilder, new Literal(literal)));
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public AndBuilder<UpperBoundary> literal( float literal ) {
            return new AndBuilder<UpperBoundary>(new UpperBoundary(comparisonBuilder, new Literal(literal)));
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public AndBuilder<UpperBoundary> literal( double literal ) {
            return new AndBuilder<UpperBoundary>(new UpperBoundary(comparisonBuilder, new Literal(literal)));
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public AndBuilder<UpperBoundary> literal( DateTime literal ) {
            return new AndBuilder<UpperBoundary>(new UpperBoundary(comparisonBuilder, new Literal(literal)));
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public AndBuilder<UpperBoundary> literal( Path literal ) {
            return new AndBuilder<UpperBoundary>(new UpperBoundary(comparisonBuilder, new Literal(literal)));
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public AndBuilder<UpperBoundary> literal( Name literal ) {
            return new AndBuilder<UpperBoundary>(new UpperBoundary(comparisonBuilder, new Literal(literal)));
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public AndBuilder<UpperBoundary> literal( URI literal ) {
            return new AndBuilder<UpperBoundary>(new UpperBoundary(comparisonBuilder, new Literal(literal)));
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public AndBuilder<UpperBoundary> literal( UUID literal ) {
            return new AndBuilder<UpperBoundary>(new UpperBoundary(comparisonBuilder, new Literal(literal)));
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public AndBuilder<UpperBoundary> literal( Binary literal ) {
            return new AndBuilder<UpperBoundary>(new UpperBoundary(comparisonBuilder, new Literal(literal)));
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public AndBuilder<UpperBoundary> literal( BigDecimal literal ) {
            return new AndBuilder<UpperBoundary>(new UpperBoundary(comparisonBuilder, new Literal(literal)));
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value;
         * @return the constraint builder; never null
         */
        public AndBuilder<UpperBoundary> literal( boolean literal ) {
            return new AndBuilder<UpperBoundary>(new UpperBoundary(comparisonBuilder, new Literal(literal)));
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param variableName the name of the variable
         * @return the constraint builder; never null
         */
        public AndBuilder<UpperBoundary> variable( String variableName ) {
            return new AndBuilder<UpperBoundary>(new UpperBoundary(comparisonBuilder, new BindVariableName(variableName)));
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param subquery the subquery
         * @return the constraint builder; never null
         */
        public AndBuilder<UpperBoundary> subquery( Subquery subquery ) {
            return new AndBuilder<UpperBoundary>(new UpperBoundary(comparisonBuilder, subquery));
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param subquery the subquery
         * @return the constraint builder; never null
         */
        public AndBuilder<UpperBoundary> subquery( QueryCommand subquery ) {
            return subquery(new Subquery(subquery));
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<AndBuilder<UpperBoundary>> cast( int literal ) {
            return new CastAsLowerBoundary(comparisonBuilder, literal);
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<AndBuilder<UpperBoundary>> cast( String literal ) {
            return new CastAsLowerBoundary(comparisonBuilder, literal);
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<AndBuilder<UpperBoundary>> cast( boolean literal ) {
            return new CastAsLowerBoundary(comparisonBuilder, literal);
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<AndBuilder<UpperBoundary>> cast( long literal ) {
            return new CastAsLowerBoundary(comparisonBuilder, literal);
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<AndBuilder<UpperBoundary>> cast( double literal ) {
            return new CastAsLowerBoundary(comparisonBuilder, literal);
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<AndBuilder<UpperBoundary>> cast( BigDecimal literal ) {
            return new CastAsLowerBoundary(comparisonBuilder, literal);
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<AndBuilder<UpperBoundary>> cast( DateTime literal ) {
            return new CastAsLowerBoundary(comparisonBuilder, literal);
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<AndBuilder<UpperBoundary>> cast( Name literal ) {
            return new CastAsLowerBoundary(comparisonBuilder, literal);
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<AndBuilder<UpperBoundary>> cast( Path literal ) {
            return new CastAsLowerBoundary(comparisonBuilder, literal);
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<AndBuilder<UpperBoundary>> cast( UUID literal ) {
            return new CastAsLowerBoundary(comparisonBuilder, literal);
        }

        /**
         * Define the lower boundary value of a range.
         * 
         * @param literal the literal value that is to be cast
         * @return the constraint builder; never null
         */
        public CastAs<AndBuilder<UpperBoundary>> cast( URI literal ) {
            return new CastAsLowerBoundary(comparisonBuilder, literal);
        }
    }

    public class ArithmeticBuilder {
        protected final ArithmeticBuilder parent;
        protected final ArithmeticOperator operator;
        protected DynamicOperand left;
        protected final ComparisonBuilder comparisonBuilder;

        protected ArithmeticBuilder( ArithmeticOperator operator,
                                     ComparisonBuilder comparisonBuilder,
                                     DynamicOperand left,
                                     ArithmeticBuilder parent ) {
            this.operator = operator;
            this.left = left;
            this.comparisonBuilder = comparisonBuilder;
            this.parent = parent; // may be null
        }

        /**
         * Constrains the nodes in the the supplied table such that they must have a property value whose length matches the
         * criteria.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @param property the name of the property; may not be null and must refer to a valid property name
         * @return the interface for completing the value portion of the criteria specification; never null
         */
        public ComparisonBuilder length( String table,
                                         String property ) {
            return comparisonBuilder(new Length(new PropertyValue(selector(table), property)));
        }

        /**
         * Constrains the nodes in the the supplied table such that they must have a matching value for the named property.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @param property the name of the property; may not be null and must refer to a valid property name
         * @return the interface for completing the value portion of the criteria specification; never null
         */
        public ComparisonBuilder propertyValue( String table,
                                                String property ) {
            return comparisonBuilder(new PropertyValue(selector(table), property));
        }

        /**
         * Constrains the nodes in the the supplied table such that they must have a matching value for any of the node's
         * reference properties.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @return the interface for completing the value portion of the criteria specification; never null
         */
        public ComparisonBuilder referenceValue( String table ) {
            return comparisonBuilder(new ReferenceValue(selector(table)));
        }

        /**
         * Constrains the nodes in the the supplied table such that they must have a matching value for the named property.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @param property the name of the reference property; may be null if the constraint applies to all/any reference
         *        properties on the node
         * @return the interface for completing the value portion of the criteria specification; never null
         */
        public ComparisonBuilder referenceValue( String table,
                                                 String property ) {
            return comparisonBuilder(new ReferenceValue(selector(table), property));
        }

        /**
         * Constrains the nodes in the the supplied table such that they must satisfy the supplied full-text search on the nodes'
         * property values.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @return the interface for completing the value portion of the criteria specification; never null
         */
        public ComparisonBuilder fullTextSearchScore( String table ) {
            return comparisonBuilder(new FullTextSearchScore(selector(table)));
        }

        /**
         * Constrains the nodes in the the supplied table based upon criteria on the node's depth.
         * 
         * @param table the name of the table; may not be null and must refer to a valid name or alias of a table appearing in the
         *        FROM clause
         * @return the interface for completing the value portion of the criteria specification; never null
         */
        public ComparisonBuilder depth( String table ) {
            return comparisonBuilder(new NodeDepth(selector(table)));
        }

        // /**
        // * Simulate the use of an open parenthesis in the constraint. The resulting builder should be used to define the
        // * constraint within the parenthesis, and should always be terminated with a {@link #closeParen()}.
        // *
        // * @return the constraint builder that should be used to define the portion of the constraint within the parenthesis;
        // * never null
        // * @see #closeParen()
        // */
        // public ArithmeticBuilder openParen() {
        // return new ArithmeticBuilder(operator, comparisonBuilder, left, this);
        // }
        //
        // /**
        // * Complete the specification of a constraint clause, and return the builder for the parent constraint clause.
        // *
        // * @return the constraint builder that was used to create this parenthetical constraint clause builder; never null
        // * @throws IllegalStateException if there was not an {@link #openParen() open parenthesis} to close
        // */
        // public ComparisonBuilder closeParen() {
        // if (parent == null) {
        // throw new IllegalStateException(GraphI18n.unexpectedClosingParenthesis.text());
        // }
        // buildLogicalConstraint();
        // return parent.setLeft(left).comparisonBuilder;
        // }
        // protected ArithmeticBuilder setLeft( DynamicOperand left ) {
        // this.left = left;
        // return this;
        // }

        protected ComparisonBuilder comparisonBuilder( DynamicOperand right ) {
            DynamicOperand leftOperand = null;
            // If the left operand is an arithmetic operand, then we need to check the operator precedence ...
            if (left instanceof ArithmeticOperand) {
                ArithmeticOperand leftArith = (ArithmeticOperand)left;
                ArithmeticOperator operator = leftArith.operator();
                if (this.operator.precedes(operator)) {
                    // Need to do create an operand with leftArith.right and right
                    DynamicOperand inner = new ArithmeticOperand(leftArith.right(), this.operator, right);
                    leftOperand = new ArithmeticOperand(leftArith.left(), operator, inner);
                } else {
                    // the left preceds this, so we can add the new operand on top ...
                    leftOperand = new ArithmeticOperand(leftArith, operator, right);
                }
            } else {
                // The left isn't an arith ...
                leftOperand = new ArithmeticOperand(left, operator, right);
            }
            return new ComparisonBuilder(comparisonBuilder.constraintBuilder, leftOperand);
        }
    }

    /**
     * An interface used to set the right-hand side of a constraint.
     */
    public class ComparisonBuilder {
        protected final DynamicOperand left;
        protected final ConstraintBuilder constraintBuilder;

        protected ComparisonBuilder( ConstraintBuilder constraintBuilder,
                                     DynamicOperand left ) {
            this.left = left;
            this.constraintBuilder = constraintBuilder;
        }

        public ConstraintBuilder isInSubquery( QueryCommand subquery ) {
            CheckArg.isNotNull(subquery, "subquery");
            return this.constraintBuilder.setConstraint(new SetCriteria(left, new Subquery(subquery)));
        }

        public ConstraintBuilder isInSubquery( Subquery subquery ) {
            CheckArg.isNotNull(subquery, "subquery");
            return this.constraintBuilder.setConstraint(new SetCriteria(left, subquery));
        }

        public ConstraintBuilder isIn( Object... literals ) {
            CheckArg.isNotNull(literals, "literals");
            Collection<StaticOperand> right = new ArrayList<StaticOperand>();
            for (Object literal : literals) {
                right.add(literal instanceof Literal ? (Literal)literal : new Literal(literal));
            }
            return this.constraintBuilder.setConstraint(new SetCriteria(left, right));
        }

        public ConstraintBuilder isIn( Iterable<Object> literals ) {
            CheckArg.isNotNull(literals, "literals");
            Collection<StaticOperand> right = new ArrayList<StaticOperand>();
            for (Object literal : literals) {
                right.add(literal instanceof Literal ? (Literal)literal : new Literal(literal));
            }
            return this.constraintBuilder.setConstraint(new SetCriteria(left, right));
        }

        /**
         * Create a comparison object based upon the addition of the previously-constructed {@link DynamicOperand} and the next
         * DynamicOperand to be created with the supplied builder.
         * 
         * @return the builder that should be used to create the right-hand-side of the operation; never null
         */
        public ArithmeticBuilder plus() {
            return new ArithmeticBuilder(ArithmeticOperator.ADD, this, left, null);
        }

        /**
         * Create a comparison object based upon the subtraction of the next {@link DynamicOperand} (created using the builder
         * returned from this method) from the the previously-constructed DynamicOperand to be created with the supplied builder.
         * 
         * @return the builder that should be used to create the right-hand-side of the operation; never null
         */
        public ArithmeticBuilder minus() {
            return new ArithmeticBuilder(ArithmeticOperator.SUBTRACT, this, left, null);
        }

        /**
         * Define the operator that will be used in the comparison, returning an interface that can be used to define the
         * right-hand-side of the comparison.
         * 
         * @param operator the operator; may not be null
         * @return the interface used to define the right-hand-side of the comparison
         */
        public RightHandSide is( Operator operator ) {
            CheckArg.isNotNull(operator, "operator");
            return new RightHandSide(this, operator);
        }

        /**
         * Use the 'equal to' operator in the comparison, returning an interface that can be used to define the right-hand-side of
         * the comparison.
         * 
         * @return the interface used to define the right-hand-side of the comparison
         */
        public RightHandSide isEqualTo() {
            return is(Operator.EQUAL_TO);
        }

        /**
         * Use the 'equal to' operator in the comparison, returning an interface that can be used to define the right-hand-side of
         * the comparison.
         * 
         * @return the interface used to define the right-hand-side of the comparison
         */
        public RightHandSide isNotEqualTo() {
            return is(Operator.NOT_EQUAL_TO);
        }

        /**
         * Use the 'equal to' operator in the comparison, returning an interface that can be used to define the right-hand-side of
         * the comparison.
         * 
         * @return the interface used to define the right-hand-side of the comparison
         */
        public RightHandSide isGreaterThan() {
            return is(Operator.GREATER_THAN);
        }

        /**
         * Use the 'equal to' operator in the comparison, returning an interface that can be used to define the right-hand-side of
         * the comparison.
         * 
         * @return the interface used to define the right-hand-side of the comparison
         */
        public RightHandSide isGreaterThanOrEqualTo() {
            return is(Operator.GREATER_THAN_OR_EQUAL_TO);
        }

        /**
         * Use the 'equal to' operator in the comparison, returning an interface that can be used to define the right-hand-side of
         * the comparison.
         * 
         * @return the interface used to define the right-hand-side of the comparison
         */
        public RightHandSide isLessThan() {
            return is(Operator.LESS_THAN);
        }

        /**
         * Use the 'equal to' operator in the comparison, returning an interface that can be used to define the right-hand-side of
         * the comparison.
         * 
         * @return the interface used to define the right-hand-side of the comparison
         */
        public RightHandSide isLessThanOrEqualTo() {
            return is(Operator.LESS_THAN_OR_EQUAL_TO);
        }

        /**
         * Use the 'equal to' operator in the comparison, returning an interface that can be used to define the right-hand-side of
         * the comparison.
         * 
         * @return the interface used to define the right-hand-side of the comparison
         */
        public RightHandSide isLike() {
            return is(Operator.LIKE);
        }

        /**
         * Define the right-hand-side of the constraint using the supplied operator.
         * 
         * @param operator the operator; may not be null
         * @param variableName the name of the variable
         * @return the builder used to create the constraint clause, ready to be used to create other constraints clauses or
         *         complete already-started clauses; never null
         */
        public ConstraintBuilder isVariable( Operator operator,
                                             String variableName ) {
            CheckArg.isNotNull(operator, "operator");
            return this.constraintBuilder.setConstraint(new Comparison(left, operator, new BindVariableName(variableName)));
        }

        /**
         * Define the right-hand-side of the constraint using the supplied operator.
         * 
         * @param operator the operator; may not be null
         * @param subquery the subquery
         * @return the builder used to create the constraint clause, ready to be used to create other constraints clauses or
         *         complete already-started clauses; never null
         */
        public ConstraintBuilder is( Operator operator,
                                     QueryCommand subquery ) {
            assert operator != null;
            return is(operator, subquery);
        }

        /**
         * Define the right-hand-side of the constraint using the supplied operator.
         * 
         * @param operator the operator; may not be null
         * @param subquery the subquery
         * @return the builder used to create the constraint clause, ready to be used to create other constraints clauses or
         *         complete already-started clauses; never null
         */
        public ConstraintBuilder is( Operator operator,
                                     Subquery subquery ) {
            assert operator != null;
            return is(operator, subquery);
        }

        /**
         * Define the right-hand-side of the constraint using the supplied operator.
         * 
         * @param operator the operator; may not be null
         * @param literalOrSubquery the literal value or subquery
         * @return the builder used to create the constraint clause, ready to be used to create other constraints clauses or
         *         complete already-started clauses; never null
         */
        public ConstraintBuilder is( Operator operator,
                                     Object literalOrSubquery ) {
            assert operator != null;
            return this.constraintBuilder.setConstraint(new Comparison(left, operator, adapt(literalOrSubquery)));
        }

        protected StaticOperand adapt( Object literalOrSubquery ) {
            if (literalOrSubquery instanceof QueryCommand) {
                // Wrap the query in a subquery ...
                return new Subquery((QueryCommand)literalOrSubquery);
            }
            if (literalOrSubquery instanceof Subquery) {
                return (Subquery)literalOrSubquery;
            }
            if (literalOrSubquery instanceof Literal) {
                return (Literal)literalOrSubquery;
            }
            return new Literal(literalOrSubquery);
        }

        /**
         * Define the right-hand-side of the constraint using the supplied operator.
         * 
         * @param lowerBoundLiteral the literal value that represents the lower bound of the range (inclusive); may be a subquery
         * @param upperBoundLiteral the literal value that represents the upper bound of the range (inclusive); may be a subquery
         * @return the builder used to create the constraint clause, ready to be used to create other constraints clauses or
         *         complete already-started clauses; never null
         */
        public ConstraintBuilder isBetween( Object lowerBoundLiteral,
                                            Object upperBoundLiteral ) {
            assert lowerBoundLiteral != null;
            assert upperBoundLiteral != null;
            return this.constraintBuilder.setConstraint(new Between(left, adapt(lowerBoundLiteral), adapt(upperBoundLiteral)));
        }

        /**
         * Define the right-hand-side of the constraint to be equivalent to the value of the supplied variable.
         * 
         * @param variableName the name of the variable
         * @return the builder used to create the constraint clause, ready to be used to create other constraints clauses or
         *         complete already-started clauses; never null
         */
        public ConstraintBuilder isEqualToVariable( String variableName ) {
            return isVariable(Operator.EQUAL_TO, variableName);
        }

        /**
         * Define the right-hand-side of the constraint to be greater than the value of the supplied variable.
         * 
         * @param variableName the name of the variable
         * @return the builder used to create the constraint clause, ready to be used to create other constraints clauses or
         *         complete already-started clauses; never null
         */
        public ConstraintBuilder isGreaterThanVariable( String variableName ) {
            return isVariable(Operator.GREATER_THAN, variableName);
        }

        /**
         * Define the right-hand-side of the constraint to be greater than or equal to the value of the supplied variable.
         * 
         * @param variableName the name of the variable
         * @return the builder used to create the constraint clause, ready to be used to create other constraints clauses or
         *         complete already-started clauses; never null
         */
        public ConstraintBuilder isGreaterThanOrEqualToVariable( String variableName ) {
            return isVariable(Operator.GREATER_THAN_OR_EQUAL_TO, variableName);
        }

        /**
         * Define the right-hand-side of the constraint to be less than the value of the supplied variable.
         * 
         * @param variableName the name of the variable
         * @return the builder used to create the constraint clause, ready to be used to create other constraints clauses or
         *         complete already-started clauses; never null
         */
        public ConstraintBuilder isLessThanVariable( String variableName ) {
            return isVariable(Operator.LESS_THAN, variableName);
        }

        /**
         * Define the right-hand-side of the constraint to be less than or equal to the value of the supplied variable.
         * 
         * @param variableName the name of the variable
         * @return the builder used to create the constraint clause, ready to be used to create other constraints clauses or
         *         complete already-started clauses; never null
         */
        public ConstraintBuilder isLessThanOrEqualToVariable( String variableName ) {
            return isVariable(Operator.LESS_THAN_OR_EQUAL_TO, variableName);
        }

        /**
         * Define the right-hand-side of the constraint to be LIKE the value of the supplied variable.
         * 
         * @param variableName the name of the variable
         * @return the builder used to create the constraint clause, ready to be used to create other constraints clauses or
         *         complete already-started clauses; never null
         */
        public ConstraintBuilder isLikeVariable( String variableName ) {
            return isVariable(Operator.LIKE, variableName);
        }

        /**
         * Define the right-hand-side of the constraint to be not equal to the value of the supplied variable.
         * 
         * @param variableName the name of the variable
         * @return the builder used to create the constraint clause, ready to be used to create other constraints clauses or
         *         complete already-started clauses; never null
         */
        public ConstraintBuilder isNotEqualToVariable( String variableName ) {
            return isVariable(Operator.NOT_EQUAL_TO, variableName);
        }

        /**
         * Define the right-hand-side of the constraint to be equivalent to the supplied literal value.
         * 
         * @param literalOrSubquery the literal value or a subquery
         * @return the builder used to create the constraint clause, ready to be used to create other constraints clauses or
         *         complete already-started clauses; never null
         */
        public ConstraintBuilder isEqualTo( Object literalOrSubquery ) {
            return is(Operator.EQUAL_TO, literalOrSubquery);
        }

        /**
         * Define the right-hand-side of the constraint to be greater than the supplied literal value.
         * 
         * @param literalOrSubquery the literal value or a subquery
         * @return the builder used to create the constraint clause, ready to be used to create other constraints clauses or
         *         complete already-started clauses; never null
         */
        public ConstraintBuilder isGreaterThan( Object literalOrSubquery ) {
            return is(Operator.GREATER_THAN, literalOrSubquery);
        }

        /**
         * Define the right-hand-side of the constraint to be greater than or equal to the supplied literal value.
         * 
         * @param literalOrSubquery the literal value or a subquery
         * @return the builder used to create the constraint clause, ready to be used to create other constraints clauses or
         *         complete already-started clauses; never null
         */
        public ConstraintBuilder isGreaterThanOrEqualTo( Object literalOrSubquery ) {
            return is(Operator.GREATER_THAN_OR_EQUAL_TO, literalOrSubquery);
        }

        /**
         * Define the right-hand-side of the constraint to be less than the supplied literal value.
         * 
         * @param literalOrSubquery the literal value or a subquery
         * @return the builder used to create the constraint clause, ready to be used to create other constraints clauses or
         *         complete already-started clauses; never null
         */
        public ConstraintBuilder isLessThan( Object literalOrSubquery ) {
            return is(Operator.LESS_THAN, literalOrSubquery);
        }

        /**
         * Define the right-hand-side of the constraint to be less than or equal to the supplied literal value.
         * 
         * @param literalOrSubquery the literal value or a subquery
         * @return the builder used to create the constraint clause, ready to be used to create other constraints clauses or
         *         complete already-started clauses; never null
         */
        public ConstraintBuilder isLessThanOrEqualTo( Object literalOrSubquery ) {
            return is(Operator.LESS_THAN_OR_EQUAL_TO, literalOrSubquery);
        }

        /**
         * Define the right-hand-side of the constraint to be LIKE the supplied literal value.
         * 
         * @param literalOrSubquery the literal value or a subquery
         * @return the builder used to create the constraint clause, ready to be used to create other constraints clauses or
         *         complete already-started clauses; never null
         */
        public ConstraintBuilder isLike( Object literalOrSubquery ) {
            return is(Operator.LIKE, literalOrSubquery);
        }

        /**
         * Define the right-hand-side of the constraint to be not equal to the supplied literal value.
         * 
         * @param literalOrSubquery the literal value or a subquery
         * @return the builder used to create the constraint clause, ready to be used to create other constraints clauses or
         *         complete already-started clauses; never null
         */
        public ConstraintBuilder isNotEqualTo( Object literalOrSubquery ) {
            return is(Operator.NOT_EQUAL_TO, literalOrSubquery);
        }

        /**
         * Define the constraint as a range between a lower boundary and an upper boundary.
         * 
         * @return the interface used to specify the lower boundary boundary, the upper boundary, and which will return the
         *         builder interface; never null
         */
        public LowerBoundary isBetween() {
            return new LowerBoundary(this);
        }
    }

    public class AndBuilder<T> {
        private final T object;

        protected AndBuilder( T object ) {
            assert object != null;
            this.object = object;
        }

        /**
         * Return the component
         * 
         * @return the component; never null
         */
        public T and() {
            return this.object;
        }
    }
}
