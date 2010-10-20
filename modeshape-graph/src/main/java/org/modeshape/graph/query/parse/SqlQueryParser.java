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
package org.modeshape.graph.query.parse;

import static org.modeshape.common.text.TokenStream.ANY_VALUE;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.modeshape.common.CommonI18n;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.Position;
import org.modeshape.common.text.TokenStream;
import org.modeshape.common.text.TokenStream.CharacterStream;
import org.modeshape.common.text.TokenStream.Tokenizer;
import org.modeshape.common.text.TokenStream.Tokens;
import org.modeshape.common.xml.XmlCharacters;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.property.ValueFormatException;
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
import org.modeshape.graph.query.model.FullTextSearch.Term;
import org.modeshape.graph.query.model.SetQuery.Operation;
import org.modeshape.graph.query.model.TypeSystem.TypeFactory;

/**
 * A {@link QueryParser} implementation that parses a subset of SQL select and set queries.
 * <p>
 * This grammar is equivalent to the SQL grammar as defined by the JCR 2.0 specification, with some useful additions:
 * <ul>
 * <li>"<code>... (UNION|INTERSECT|EXCEPT) [ALL] ...</code>" to combine and merge results from multiple queries</li>
 * <li>"<code>SELECT DISTINCT ...</code>" to remove duplicates</li>
 * <li>"<code>LIMIT count [OFFSET number]</code>" clauses to control the number of results returned as well as the number of rows
 * that should be skipped</li>
 * <li>Support for additional join types, including "<code>FULL OUTER JOIN</code>" and "<code>CROSS JOIN</code>"</li>
 * <li>Additional dynamic operands "<code>DEPTH([&lt;selectorName>])</code>" and "<code>PATH([&lt;selectorName>])</code>" that
 * enables placing constraints on the node depth and path, respectively, and which can be used in a manner similar to "
 * <code>NAME([&lt;selectorName>])</code>" and "<code>LOCALNAME([&lt;selectorName>])</code>. Note in each of these cases, the
 * selector name is optional if there is only one selector in the query.</li>
 * <li>Additional dynamic operand "<code>REFERENCE([&lt;selectorName>.]&lt;propertyName>])</code>" that
 * enables placing constraints on one or all reference properties, and which can be used in a manner similar to "
 * <code>PropertyValue([&lt;selectorName>.]&lt;propertyName>)</code>". Note in each of these cases, the
 * selector name is optional if there is only one selector in the query, and that the property name can be excluded
 * if the constraint should apply to all reference properties.</li>
 * <li>Support for the IN clause and NOT IN clause to more easily supply a list of valid discrete static operands: "
 * <code>&lt;dynamicOperand> [NOT] IN (&lt;staticOperand> {, &lt;staticOperand>})</code>"</li>
 * <li>Support for the BETWEEN clause: "<code>&lt;dynamicOperand> [NOT] BETWEEN &lt;lowerBoundStaticOperand> [EXCLUSIVE] AND
 * &lt;upperBoundStaticOperand> [EXCLUSIVE]</code>"</i>
 * <li>Support for arithmetic operations ('+', '-', '*', '/') between dynamic operands used in <code>WHERE</code> criteria and <code>ORDER BY</code>
 * clauses: "<code>WHERE &lt;dynamicOperand> + &lt;dynamicOperand> ...</code>" or "<code>ORDER BY (&lt;dynamicOperand> + &lt;dynamicOperand>) [ASC]</code>".
 * Note that standard operator precedence is used, but grouping by (potentially nested) parentheses is also supported.</i>
 * </ul>
 * </p>
 * <h3>SQL grammar</h3>
 * <p>
 * This section defines the complete grammar for the SQL dialect supported by this parser.
 * </p>
 * <h4>Queries</h4>
 * 
 * <pre>
 * QueryCommand ::= Query | SetQuery
 * 
 * SetQuery ::= Query ('UNION'|'INTERSECT'|'EXCEPT') [ALL] Query
 *                  { ('UNION'|'INTERSECT'|'EXCEPT') [ALL] Query }
 * 
 * Query ::= 'SELECT' ['DISINCT'] columns
 *           'FROM' Source
 *           ['WHERE' Constraint]
 *           ['ORDER BY' orderings]
 *           [Limit]
 * </pre>
 * 
 * <h4>Sources</h4>
 * 
 * <pre>
 * Source ::= Selector | Join
 * 
 * Selector ::= nodeTypeName ['AS' selectorName]
 * 
 * nodeTypeName ::= Name
 * </pre>
 * 
 * <h4>Joins</h4>
 * 
 * <pre>
 * Join ::= left [JoinType] 'JOIN' right 'ON' JoinCondition
 *          // If JoinType is omitted INNER is assumed.
 *          
 * left ::= Source
 * right ::= Source
 * 
 * JoinType ::= Inner | LeftOuter | RightOuter | FullOuter | Cross
 * 
 * Inner ::= 'INNER' ['JOIN']
 * 
 * LeftOuter ::= 'LEFT JOIN' | 'OUTER JOIN' | 'LEFT OUTER JOIN'
 * 
 * RightOuter ::= 'RIGHT OUTER' ['JOIN']
 * 
 * RightOuter ::= 'FULL OUTER' ['JOIN']
 * 
 * RightOuter ::= 'CROSS' ['JOIN']
 * 
 * JoinCondition ::= EquiJoinCondition | SameNodeJoinCondition | ChildNodeJoinCondition | DescendantNodeJoinCondition
 * </pre>
 * 
 * <h5>Equi-join conditions</h5>
 * 
 * <pre>
 * EquiJoinCondition ::= selector1Name'.'property1Name '=' selector2Name'.'property2Name
 * 
 * selector1Name ::= selectorName
 * selector2Name ::= selectorName
 * property1Name ::= propertyName
 * property2Name ::= propertyName
 * </pre>
 * 
 * <h5>Same-node join condition</h5>
 * 
 * <pre>
 * SameNodeJoinCondition ::= 'ISSAMENODE(' selector1Name ',' selector2Name [',' selector2Path] ')'
 * 
 * selector2Path ::= Path
 * </pre>
 * 
 * <h5>Child-node join condition</h5>
 * 
 * <pre>
 * ChildNodeJoinCondition ::= 'ISCHILDNODE(' childSelectorName ',' parentSelectorName ')'
 * 
 * childSelectorName ::= selectorName
 * parentSelectorName ::= selectorName
 * </pre>
 * 
 * <h5>Descendant-node join condition</h5>
 * 
 * <pre>
 * DescendantNodeJoinCondition ::= 'ISDESCENDANTNODE(' descendantSelectorName ',' ancestorSelectorName ')'
 * descendantSelectorName ::= selectorName
 * ancestorSelectorName ::= selectorName
 * </pre>
 * 
 * <h4>Constraints</h4>
 * 
 * <pre>
 * Constraint ::= ConstraintItem | '(' ConstraintItem ')'
 * 
 * ConstraintItem ::= And | Or | Not | Comparison | Between | PropertyExistence | SetConstraint | FullTextSearch | 
 *                    SameNode | ChildNode | DescendantNode
 * </pre>
 * 
 * <h5>And constraint</h5>
 * 
 * <pre>
 * And ::= constraint1 'AND' constraint2
 * 
 * constraint1 ::= Constraint
 * constraint2 ::= Constraint
 * </pre>
 * 
 * <h5>Or constraint</h5>
 * 
 * <pre>
 * Or ::= constraint1 'OR' constraint2
 * </pre>
 * 
 * <h5>Not constraint</h5>
 * 
 * <pre>
 * Not ::= 'NOT' Constraint
 * </pre>
 * 
 * <h5>Comparison constraint</h5>
 * 
 * <pre>
 * Comparison ::= DynamicOperand Operator StaticOperand
 * 
 * Operator ::= '=' | '!=' | '<' | '<=' | '>' | '>=' | 'LIKE'
 * </pre>
 * 
 * <h5>Between constraint</h5>
 * 
 * <pre>
 * Between ::= DynamicOperand ['NOT'] 'BETWEEN' lowerBound ['EXCLUSIVE'] 'AND' upperBound ['EXCLUSIVE']
 * 
 * lowerBound ::= StaticOperand
 * upperBound ::= StaticOperand
 * </pre>
 * 
 * <h5>Property existence constraint</h5>
 * 
 * <pre>
 * PropertyExistence ::= selectorName'.'propertyName 'IS' ['NOT'] 'NULL' | 
 *                       propertyName 'IS' ['NOT'] 'NULL' &#47;* If only one selector exists in this query *&#47;
 * 
 * </pre>
 * 
 * <h5>Set constraint</h5>
 * 
 * <pre>
 * SetConstraint ::= selectorName'.'propertyName ['NOT'] 'IN' | 
 *                       propertyName ['NOT'] 'IN' &#47;* If only one selector exists in this query *&#47;
 *                       '(' firstStaticOperand {',' additionalStaticOperand } ')'
 * firstStaticOperand ::= StaticOperand
 * additionalStaticOperand ::= StaticOperand
 * </pre>
 * 
 * <h5>Full-text search constraint</h5>
 * 
 * <pre>
 * FullTextSearch ::= 'CONTAINS(' ([selectorName'.']propertyName | selectorName'.*') 
 *                            ',' ''' fullTextSearchExpression''' ')'
 *                    &#47;* If only one selector exists in this query, explicit specification of the selectorName
 *                       preceding the propertyName is optional *&#47;
 * fullTextSearchExpression ::= &#47;* a full-text search expression, see {@link FullTextSearchParser} *&#47;
 * </pre>
 * 
 * <h5>Same-node constraint</h5>
 * 
 * <pre>
 * SameNode ::= 'ISSAMENODE(' [selectorName ','] Path ')' 
 *                    &#47;* If only one selector exists in this query, explicit specification of the selectorName
 *                       preceding the propertyName is optional *&#47;
 * </pre>
 * 
 * <h5>Child-node constraint</h5>
 * 
 * <pre>
 * ChildNode ::= 'ISCHILDNODE(' [selectorName ','] Path ')' 
 *                    &#47;* If only one selector exists in this query, explicit specification of the selectorName
 *                       preceding the propertyName is optional *&#47;
 * </pre>
 * 
 * <h5>Descendant-node constraint</h5>
 * 
 * <pre>
 * DescendantNode ::= 'ISDESCENDANTNODE(' [selectorName ','] Path ')' 
 *                    /* If only one selector exists in this query, explicit specification of the selectorName
 *                       preceding the propertyName is optional *&#47;
 * </pre>
 * 
 * <h5>Paths and names</h5>
 * 
 * <pre>
 * 
 * Name ::= '[' quotedName ']' | '[' simpleName ']' | simpleName
 * 
 * quotedName ::= /* A JCR Name (see the JCR specification) *&#47;
 * simpleName ::= /* A JCR Name that contains only SQL-legal characters (namely letters, digits, and underscore) *&#47;
 *
 * Path ::= '[' quotedPath ']' | '[' simplePath ']' | simplePath
 *
 * quotedPath ::= /* A JCR Path that contains non-SQL-legal characters *&#47;
 * simplePath ::= /* A JCR Path (rather Name) that contains only SQL-legal characters (namely letters, digits, and underscore) *&#47;
 * </pre>
 * 
 * <h4>Static operands</h4>
 * 
 * <pre>
 * StaticOperand ::= Literal | BindVariableValue
 * </pre>
 * 
 * <h5>Literal</h5>
 * 
 * <pre>
 * Literal ::= CastLiteral | UncastLiteral
 * 
 * CastLiteral ::= 'CAST(' UncastLiteral ' AS ' PropertyType ')'
 * 
 * PropertyType ::= 'STRING' | 'BINARY' | 'DATE' | 'LONG' | 'DOUBLE' | 'DECIMAL' | 'BOOLEAN' | 'NAME' | 'PATH' | 
 *                  'REFERENCE' | 'WEAKREFERENCE' | 'URI'
 *                  
 * UncastLiteral ::= UnquotedLiteral | ''' UnquotedLiteral ''' | '"' UnquotedLiteral '"'
 * 
 * UnquotedLiteral ::= /* String form of a JCR Value, as defined in the JCR specification *&#47;
 * </pre>
 * 
 * <h5>Bind variables</h5>
 * 
 * <pre>
 * BindVariableValue ::= '$'bindVariableName
 * 
 * bindVariableName ::= /* A string that conforms to the JCR Name syntax, though the prefix does not need to be
 *                         a registered namespace prefix. *&#47;
 * </pre>
 * 
 * <h4>Dynamic operands</h4>
 * 
 * <pre>
 * DynamicOperand ::= PropertyValue | ReferenceValue | Length | NodeName | NodeLocalName | NodePath | NodeDepth | 
 *                    FullTextSearchScore | LowerCase | UpperCase | Arithmetic |
 *                    '(' DynamicOperand ')'
 * </pre>
 * <h5>Property value</h5>
 * <pre>
 * PropertyValue ::= [selectorName'.'] propertyName
 *                    /* If only one selector exists in this query, explicit specification of the selectorName
 *                       preceding the propertyName is optional *&#47;
 * </pre>
 * <h5>Reference value</h5>
 * <pre>
 * ReferenceValue ::= 'REFERENCE(' selectorName '.' propertyName ')' |
 *                    'REFERENCE(' selectorName ')' |
 *                    'REFERENCE()' |
 *                    /* If only one selector exists in this query, explicit specification of the selectorName
 *                       preceding the propertyName is optional. Also, the property name may be excluded 
 *                       if the constraint should apply to any reference property. *&#47;
 * </pre>
 * <h5>Property length</h5>
 * <pre>
 * Length ::= 'LENGTH(' PropertyValue ')'
 * </pre>
 * <h5>Node name</h5>
 * <pre>
 * NodeName ::= 'NAME(' [selectorName] ')'
 *                    /* If only one selector exists in this query, explicit specification of the selectorName
 *                       is optional *&#47;
 * </pre>
 * <h5>Node local name</h5>
 * <pre>
 * NodeLocalName ::= 'LOCALNAME(' [selectorName] ')'
 *                    /* If only one selector exists in this query, explicit specification of the selectorName
 *                       is optional *&#47;
 * </pre>
 * <h5>Node path</h5>
 * <pre>
 * NodePath ::= 'PATH(' [selectorName] ')'
 *                    /* If only one selector exists in this query, explicit specification of the selectorName
 *                       is optional *&#47;
 * </pre>
 * <h5>Node depth</h5>
 * <pre>
 * NodeDepth ::= 'DEPTH(' [selectorName] ')'
 *                    /* If only one selector exists in this query, explicit specification of the selectorName
 *                       is optional *&#47;
 * </pre>
 * <h5>Full-text search score</h5>
 * <pre>
 * FullTextSearchScore ::= 'SCORE(' [selectorName] ')'
 *                    /* If only one selector exists in this query, explicit specification of the selectorName
 *                       is optional *&#47;
 * </pre>
 * <h5>Lowercase</h5>
 * <pre>
 * LowerCase ::= 'LOWER(' DynamicOperand ')'
 * </pre>
 * <h5>Uppercase</h5>
 * <pre>
 * UpperCase ::= 'UPPER(' DynamicOperand ')'
 * </pre>
 * <h5>Arithmetic</h5>
 * <pre>
 * Arithmetic ::= DynamicOperand ('+'|'-'|'*'|'/') DynamicOperand
 * </pre>
 * 
 * <h4>Ordering</h4>
 * 
 * <pre>
 * orderings ::= Ordering {',' Ordering}
 * 
 * Ordering ::= DynamicOperand [Order]
 * 
 * Order ::= 'ASC' | 'DESC'
 * </pre>
 * 
 * <h4>Columns</h4>
 * 
 * <pre>
 * columns ::= (Column ',' {Column}) | '*'
 * 
 * Column ::= ([selectorName'.']propertyName ['AS' columnName]) | (selectorName'.*')
 *                    /* If only one selector exists in this query, explicit specification of the selectorName
 *                       preceding the propertyName is optional *&#47;
 * selectorName ::= Name
 * propertyName ::= Name
 * columnName ::= Name
 * </pre>
 * 
 * <h4>Limit</h4>
 * 
 * <pre>
 * Limit ::= 'LIMIT' count [ 'OFFSET' offset ]
 * count ::= /* Positive integer value *&#47;
 * offset ::= /* Non-negative integer value *&#47;
 * </pre>
 */
public class SqlQueryParser implements QueryParser {

    public static final String LANGUAGE = "SQL";

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.QueryParser#getLanguage()
     */
    public String getLanguage() {
        return LANGUAGE;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return getLanguage();
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof QueryParser) {
            QueryParser that = (QueryParser)obj;
            return this.getLanguage().equals(that.getLanguage());
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.QueryParser#parseQuery(String, TypeSystem)
     */
    public QueryCommand parseQuery( String query,
                                    TypeSystem typeSystem ) {
        Tokenizer tokenizer = new SqlTokenizer(false);
        TokenStream tokens = new TokenStream(query, tokenizer, false);
        tokens.start();
        return parseQueryCommand(tokens, typeSystem);
    }

    protected QueryCommand parseQueryCommand( TokenStream tokens,
                                              TypeSystem typeSystem ) {
        QueryCommand command = null;
        if (tokens.matches("SELECT")) {
            command = parseQuery(tokens, typeSystem);
            while (tokens.hasNext()) {
                if (tokens.matchesAnyOf("UNION", "INTERSECT", "EXCEPT")) {
                    command = parseSetQuery(tokens, command, typeSystem);
                } else if (tokens.matches(')')) {
                    // There's more in this token stream, but we'll stop reading ...
                    break;
                } else {
                    Position pos = tokens.previousPosition();
                    String msg = GraphI18n.unexpectedToken.text(tokens.consume(), pos.getLine(), pos.getColumn());
                    throw new ParsingException(pos, msg);
                }
            }
        } else {
            // We expected SELECT ...
            Position pos = tokens.nextPosition();
            String msg = GraphI18n.unexpectedToken.text(tokens.consume(), pos.getLine(), pos.getColumn());
            throw new ParsingException(pos, msg);
        }
        return command;
    }

    protected Query parseQuery( TokenStream tokens,
                                TypeSystem typeSystem ) {
        AtomicBoolean isDistinct = new AtomicBoolean(false);
        List<ColumnExpression> columnExpressions = parseSelect(tokens, isDistinct, typeSystem);
        Source source = parseFrom(tokens, typeSystem);
        Constraint constraint = parseWhere(tokens, typeSystem, source);
        // Parse the order by and limit (can be in any order) ...
        List<? extends Ordering> orderings = parseOrderBy(tokens, typeSystem, source);
        Limit limit = parseLimit(tokens);
        if (orderings == null) parseOrderBy(tokens, typeSystem, source);

        // Convert the column expressions to columns ...
        List<Column> columns = new ArrayList<Column>(columnExpressions.size());
        for (ColumnExpression expression : columnExpressions) {
            SelectorName selectorName = expression.getSelectorName();
            String propertyName = expression.getPropertyName();
            if (selectorName == null) {
                if (source instanceof Selector) {
                    selectorName = ((Selector)source).aliasOrName();
                } else {
                    Position pos = expression.getPosition();
                    String msg = GraphI18n.mustBeScopedAtLineAndColumn.text(expression, pos.getLine(), pos.getColumn());
                    throw new ParsingException(pos, msg);
                }
            }
            columns.add(column(selectorName, propertyName, expression.getColumnName()));
        }
        // Now create the query ...
        return query(source, constraint, orderings, columns, limit, isDistinct.get());
    }

    protected SetQuery parseSetQuery( TokenStream tokens,
                                      QueryCommand leftHandSide,
                                      TypeSystem typeSystem ) {
        Operation operation = null;
        if (tokens.canConsume("UNION")) {
            operation = Operation.UNION;
        } else if (tokens.canConsume("INTERSECT")) {
            operation = Operation.INTERSECT;
        } else {
            tokens.consume("EXCEPT");
            operation = Operation.EXCEPT;
        }
        boolean all = tokens.canConsume("ALL");
        // Parse the next select
        QueryCommand rightQuery = parseQuery(tokens, typeSystem);
        return setQuery(leftHandSide, operation, rightQuery, all);
    }

    protected List<ColumnExpression> parseSelect( TokenStream tokens,
                                                  AtomicBoolean isDistinct,
                                                  TypeSystem typeSystem ) {
        tokens.consume("SELECT");
        if (tokens.canConsume("DISTINCT")) isDistinct.set(true);
        if (tokens.canConsume('*')) {
            return Collections.emptyList();
        }
        List<ColumnExpression> columns = new ArrayList<ColumnExpression>();
        do {
            Position position = tokens.nextPosition();
            String propertyName = parseName(tokens, typeSystem);
            SelectorName selectorName = null;
            if (tokens.canConsume('.')) {
                // We actually read the selector name, so now read the property name ...
                selectorName = new SelectorName(propertyName);
                propertyName = parseName(tokens, typeSystem);
            }
            String alias = propertyName;
            if (tokens.canConsume("AS")) alias = parseName(tokens, typeSystem);
            columns.add(new ColumnExpression(selectorName, propertyName, alias, position));
        } while (tokens.canConsume(','));
        return columns;
    }

    protected Source parseFrom( TokenStream tokens,
                                TypeSystem typeSystem ) {
        Source source = null;
        tokens.consume("FROM");
        source = parseNamedSelector(tokens, typeSystem);
        while (tokens.hasNext()) {
            JoinType joinType = null;
            if (tokens.canConsume("JOIN") || tokens.canConsume("INNER", "JOIN")) {
                joinType = JoinType.INNER;
            } else if (tokens.canConsume("OUTER", "JOIN") || tokens.canConsume("LEFT", "JOIN")
                       || tokens.canConsume("LEFT", "OUTER", "JOIN")) {
                joinType = JoinType.LEFT_OUTER;
            } else if (tokens.canConsume("RIGHT", "OUTER", "JOIN") || tokens.canConsume("RIGHT", "OUTER")) {
                joinType = JoinType.RIGHT_OUTER;
            } else if (tokens.canConsume("FULL", "OUTER", "JOIN") || tokens.canConsume("FULL", "OUTER")) {
                joinType = JoinType.FULL_OUTER;
            } else if (tokens.canConsume("CROSS", "JOIN") || tokens.canConsume("CROSS")) {
                joinType = JoinType.CROSS;
            }
            if (joinType == null) break;
            // Read the name of the selector on the right side of the join ...
            NamedSelector right = parseNamedSelector(tokens, typeSystem);
            // Read the join condition ...
            JoinCondition joinCondition = parseJoinCondition(tokens, typeSystem);
            // Create the join ...
            source = join(source, joinType, right, joinCondition);
        }
        return source;
    }

    protected JoinCondition parseJoinCondition( TokenStream tokens,
                                                TypeSystem typeSystem ) {
        tokens.consume("ON");
        if (tokens.canConsume("ISSAMENODE", "(")) {
            SelectorName selector1Name = parseSelectorName(tokens, typeSystem);
            tokens.consume(',');
            SelectorName selector2Name = parseSelectorName(tokens, typeSystem);
            if (tokens.canConsume('.')) {
                String path = parsePath(tokens, typeSystem);
                tokens.consume(')');
                return sameNodeJoinCondition(selector1Name, selector2Name, path);
            }
            tokens.consume(')');
            return sameNodeJoinCondition(selector1Name, selector2Name);
        }
        if (tokens.canConsume("ISCHILDNODE", "(")) {
            SelectorName child = parseSelectorName(tokens, typeSystem);
            tokens.consume(',');
            SelectorName parent = parseSelectorName(tokens, typeSystem);
            tokens.consume(')');
            return childNodeJoinCondition(parent, child);
        }
        if (tokens.canConsume("ISDESCENDANTNODE", "(")) {
            SelectorName descendant = parseSelectorName(tokens, typeSystem);
            tokens.consume(',');
            SelectorName ancestor = parseSelectorName(tokens, typeSystem);
            tokens.consume(')');
            return descendantNodeJoinCondition(ancestor, descendant);
        }
        SelectorName selector1 = parseSelectorName(tokens, typeSystem);
        tokens.consume('.');
        String property1 = parseName(tokens, typeSystem);
        tokens.consume('=');
        SelectorName selector2 = parseSelectorName(tokens, typeSystem);
        tokens.consume('.');
        String property2 = parseName(tokens, typeSystem);
        return equiJoinCondition(selector1, property1, selector2, property2);
    }

    protected Constraint parseWhere( TokenStream tokens,
                                     TypeSystem typeSystem,
                                     Source source ) {
        if (tokens.canConsume("WHERE")) {
            return parseConstraint(tokens, typeSystem, source);
        }
        return null;
    }

    protected Constraint parseConstraint( TokenStream tokens,
                                          TypeSystem typeSystem,
                                          Source source ) {
        Constraint constraint = null;
        Position pos = tokens.nextPosition();
        if (tokens.canConsume("(")) {
            constraint = parseConstraint(tokens, typeSystem, source);
            tokens.consume(")");
        } else if (tokens.canConsume("NOT")) {
            tokens.canConsume('(');
            constraint = not(parseConstraint(tokens, typeSystem, source));
            tokens.canConsume(')');
        } else if (tokens.canConsume("CONTAINS", "(")) {
            // Either 'selectorName.propertyName', or 'selectorName.*' or 'propertyName' ...
            String first = tokens.consume();
            SelectorName selectorName = null;
            String propertyName = null;
            if (tokens.canConsume(".", "*")) {
                selectorName = new SelectorName(removeBracketsAndQuotes(first));
            } else if (tokens.canConsume('.')) {
                selectorName = new SelectorName(removeBracketsAndQuotes(first));
                propertyName = parseName(tokens, typeSystem);
            } else {
                if (!(source instanceof Selector)) {
                    String msg = GraphI18n.functionIsAmbiguous.text("CONTAINS()", pos.getLine(), pos.getColumn());
                    throw new ParsingException(pos, msg);
                }
                selectorName = ((Selector)source).name();
                propertyName = removeBracketsAndQuotes(first);
            }
            tokens.consume(',');

            // Followed by the full text search expression ...
            String expression = removeBracketsAndQuotes(tokens.consume(), false); // don't remove nested quotes
            Term term = parseFullTextSearchExpression(expression, tokens.previousPosition());
            tokens.consume(")");
            constraint = fullTextSearch(selectorName, propertyName, expression, term);
        } else if (tokens.canConsume("ISSAMENODE", "(")) {
            SelectorName selectorName = null;
            if (tokens.matches(ANY_VALUE, ")")) {
                if (!(source instanceof Selector)) {
                    String msg = GraphI18n.functionIsAmbiguous.text("ISSAMENODE()", pos.getLine(), pos.getColumn());
                    throw new ParsingException(pos, msg);
                }
                selectorName = ((Selector)source).name();
            } else {
                selectorName = parseSelectorName(tokens, typeSystem);
                tokens.consume(',');
            }
            String path = parsePath(tokens, typeSystem);
            tokens.consume(')');
            constraint = sameNode(selectorName, path);
        } else if (tokens.canConsume("ISCHILDNODE", "(")) {
            SelectorName selectorName = null;
            if (tokens.matches(ANY_VALUE, ")")) {
                if (!(source instanceof Selector)) {
                    String msg = GraphI18n.functionIsAmbiguous.text("ISCHILDNODE()", pos.getLine(), pos.getColumn());
                    throw new ParsingException(pos, msg);
                }
                selectorName = ((Selector)source).name();
            } else {
                selectorName = parseSelectorName(tokens, typeSystem);
                tokens.consume(',');
            }
            String path = parsePath(tokens, typeSystem);
            tokens.consume(')');
            constraint = childNode(selectorName, path);
        } else if (tokens.canConsume("ISDESCENDANTNODE", "(")) {
            SelectorName selectorName = null;
            if (tokens.matches(ANY_VALUE, ")")) {
                if (!(source instanceof Selector)) {
                    String msg = GraphI18n.functionIsAmbiguous.text("ISDESCENDANTNODE()", pos.getLine(), pos.getColumn());
                    throw new ParsingException(pos, msg);
                }
                selectorName = ((Selector)source).name();
            } else {
                selectorName = parseSelectorName(tokens, typeSystem);
                tokens.consume(',');
            }
            String path = parsePath(tokens, typeSystem);
            tokens.consume(')');
            constraint = descendantNode(selectorName, path);
        } else {
            // First try a property existance ...
            Position pos2 = tokens.nextPosition();
            constraint = parsePropertyExistance(tokens, typeSystem, source);
            if (constraint == null) {
                // Try to parse as a dynamic operand ...
                DynamicOperand left = parseDynamicOperand(tokens, typeSystem, source);
                if (left != null) {
                    if (tokens.matches('(') && left instanceof PropertyValue) {
                        // This was probably a bad function that we parsed as the start of a dynamic operation ...
                        String name = ((PropertyValue)left).propertyName(); // this may be the function name
                        String msg = GraphI18n.expectingConstraintCondition.text(name, pos2.getLine(), pos2.getColumn());
                        throw new ParsingException(pos, msg);
                    }
                    if (tokens.matches("IN", "(") || tokens.matches("NOT", "IN", "(")) {
                        boolean not = tokens.canConsume("NOT");
                        Collection<StaticOperand> staticOperands = parseInClause(tokens, typeSystem);
                        constraint = setCriteria(left, staticOperands);
                        if (not) constraint = not(constraint);
                    } else if (tokens.matches("BETWEEN") || tokens.matches("NOT", "BETWEEN")) {
                        boolean not = tokens.canConsume("NOT");
                        tokens.consume("BETWEEN");
                        StaticOperand lowerBound = parseStaticOperand(tokens, typeSystem);
                        boolean lowerInclusive = !tokens.canConsume("EXCLUSIVE");
                        tokens.consume("AND");
                        StaticOperand upperBound = parseStaticOperand(tokens, typeSystem);
                        boolean upperInclusive = !tokens.canConsume("EXCLUSIVE");
                        constraint = between(left, lowerBound, upperBound, lowerInclusive, upperInclusive);
                        if (not) constraint = not(constraint);
                    } else {
                        Operator operator = parseComparisonOperator(tokens);
                        StaticOperand right = parseStaticOperand(tokens, typeSystem);
                        constraint = comparison(left, operator, right);
                    }
                }
                // else continue ...
            }
        }
        if (constraint == null) {
            String msg = GraphI18n.expectingConstraintCondition.text(tokens.consume(), pos.getLine(), pos.getColumn());
            throw new ParsingException(pos, msg);
        }
        // AND has higher precedence than OR, so we need to evaluate it first ...
        while (tokens.canConsume("AND")) {
            Constraint rhs = parseConstraint(tokens, typeSystem, source);
            if (rhs != null) constraint = and(constraint, rhs);
        }
        while (tokens.canConsume("OR")) {
            Constraint rhs = parseConstraint(tokens, typeSystem, source);
            if (rhs != null) constraint = or(constraint, rhs);
        }
        return constraint;
    }

    protected List<StaticOperand> parseInClause( TokenStream tokens,
                                                 TypeSystem typeSystem ) {
        List<StaticOperand> result = new ArrayList<StaticOperand>();
        tokens.consume("IN");
        tokens.consume("(");
        if (!tokens.canConsume(")")) {
            // Not empty, so read the static operands ...
            do {
                result.add(parseStaticOperand(tokens, typeSystem));
            } while (tokens.canConsume(','));
            tokens.consume(")");
        }
        return result;
    }

    protected Term parseFullTextSearchExpression( String expression,
                                                  Position startOfExpression ) {
        try {
            return new FullTextSearchParser().parse(expression);
        } catch (ParsingException e) {
            // Convert the position in the exception into a position in the query.
            Position queryPos = startOfExpression.add(e.getPosition());
            throw new ParsingException(queryPos, e.getMessage());
        }
    }

    protected Operator parseComparisonOperator( TokenStream tokens ) {
        if (tokens.canConsume("=")) return Operator.EQUAL_TO;
        if (tokens.canConsume("LIKE")) return Operator.LIKE;
        if (tokens.canConsume("!", "=")) return Operator.NOT_EQUAL_TO;
        if (tokens.canConsume("<", ">")) return Operator.NOT_EQUAL_TO;
        if (tokens.canConsume("<", "=")) return Operator.LESS_THAN_OR_EQUAL_TO;
        if (tokens.canConsume(">", "=")) return Operator.GREATER_THAN_OR_EQUAL_TO;
        if (tokens.canConsume("<")) return Operator.LESS_THAN;
        if (tokens.canConsume(">")) return Operator.GREATER_THAN;
        Position pos = tokens.nextPosition();
        String msg = GraphI18n.expectingComparisonOperator.text(tokens.consume(), pos.getLine(), pos.getColumn());
        throw new ParsingException(pos, msg);
    }

    protected List<Ordering> parseOrderBy( TokenStream tokens,
                                           TypeSystem typeSystem,
                                           Source source ) {
        if (tokens.canConsume("ORDER", "BY")) {
            List<Ordering> orderings = new ArrayList<Ordering>();
            do {
                orderings.add(parseOrdering(tokens, typeSystem, source));
            } while (tokens.canConsume(','));
            return orderings;
        }
        return null;
    }

    protected Ordering parseOrdering( TokenStream tokens,
                                      TypeSystem typeSystem,
                                      Source source ) {
        DynamicOperand operand = parseDynamicOperand(tokens, typeSystem, source);
        Order order = Order.ASCENDING;
        if (tokens.canConsume("DESC")) order = Order.DESCENDING;
        if (tokens.canConsume("ASC")) order = Order.ASCENDING;
        return ordering(operand, order);
    }

    protected Constraint parsePropertyExistance( TokenStream tokens,
                                                 TypeSystem typeSystem,
                                                 Source source ) {
        if (tokens.matches(ANY_VALUE, ".", ANY_VALUE, "IS", "NOT", "NULL")
            || tokens.matches(ANY_VALUE, ".", ANY_VALUE, "IS", "NULL") || tokens.matches(ANY_VALUE, "IS", "NOT", "NULL")
            || tokens.matches(ANY_VALUE, "IS", "NULL")) {
            Position pos = tokens.nextPosition();
            String firstWord = tokens.consume();
            SelectorName selectorName = null;
            String propertyName = null;
            if (tokens.canConsume('.')) {
                // We actually read the selector name, so now read the property name ...
                selectorName = new SelectorName(firstWord);
                propertyName = parseName(tokens, typeSystem);
            } else {
                // Otherwise the source should be a single named selector
                if (!(source instanceof Selector)) {
                    String msg = GraphI18n.mustBeScopedAtLineAndColumn.text(firstWord, pos.getLine(), pos.getColumn());
                    throw new ParsingException(pos, msg);
                }
                selectorName = ((Selector)source).name();
                propertyName = parseName(firstWord, typeSystem);
            }
            if (tokens.canConsume("IS", "NOT", "NULL")) {
                return propertyExistence(selectorName, propertyName);
            }
            tokens.consume("IS", "NULL");
            return not(propertyExistence(selectorName, propertyName));
        }
        return null;
    }

    protected StaticOperand parseStaticOperand( TokenStream tokens,
                                                TypeSystem typeSystem ) {
        if (tokens.canConsume('$')) {
            // The variable name must conform to a valid prefix, which is defined as a valid NCName ...
            String value = tokens.consume();
            if (!XmlCharacters.isValidNcName(value)) {
                Position pos = tokens.previousPosition();
                String msg = GraphI18n.bindVariableMustConformToNcName.text(value, pos.getLine(), pos.getColumn());
                throw new ParsingException(pos, msg);
            }
            return bindVariableName(value);
        }
        if (tokens.canConsume('(')) {
            // Sometimes the subqueries are wrapped with parentheses ...
            StaticOperand result = parseStaticOperand(tokens, typeSystem);
            tokens.consume(')');
            return result;
        }
        if (tokens.matches("SELECT")) {
            // This is a subquery. This object is stateless, so we can reuse this object ...
            QueryCommand subqueryExpression = parseQueryCommand(tokens, typeSystem);
            return subquery(subqueryExpression);
        }
        return parseLiteral(tokens, typeSystem);
    }

    protected Subquery subquery( QueryCommand queryCommand ) {
        return new Subquery(queryCommand);
    }

    protected Literal parseLiteral( TokenStream tokens,
                                    TypeSystem typeSystem ) {
        if (tokens.canConsume("CAST", "(")) {
            // Get the value that is to be cast ...
            Position pos = tokens.nextPosition();
            Object value = parseLiteralValue(tokens, typeSystem);
            // Figure out the type we're supposed to cast to ...
            tokens.consume("AS");
            String typeName = tokens.consume();
            TypeFactory<?> typeFactory = typeSystem.getTypeFactory(typeName);
            if (typeFactory == null) {
                Position typePos = tokens.previousPosition();
                String msg = GraphI18n.invalidPropertyType.text(tokens.consume(), typePos.getLine(), typePos.getColumn());
                throw new ParsingException(typePos, msg);
            }
            // Convert the supplied value to the desired value ...
            tokens.consume(')');
            try {
                Object literal = typeFactory.create(value);
                return literal(typeSystem, literal);
            } catch (ValueFormatException e) {
                String msg = GraphI18n.valueCannotBeCastToSpecifiedType.text(value,
                                                                             pos.getLine(),
                                                                             pos.getColumn(),
                                                                             typeFactory.getTypeName(),
                                                                             e.getMessage());
                throw new ParsingException(pos, msg);
            }
        }
        // Just create a literal out of the supplied value ...
        return literal(typeSystem, parseLiteralValue(tokens, typeSystem));
    }

    protected Object parseLiteralValue( TokenStream tokens,
                                        TypeSystem typeSystem ) {
        if (tokens.matches(SqlTokenizer.QUOTED_STRING)) {
            return removeBracketsAndQuotes(tokens.consume());
        }
        TypeFactory<Boolean> booleanFactory = typeSystem.getBooleanFactory();
        if (booleanFactory != null) {
            if (tokens.canConsume("TRUE")) return booleanFactory.asString(Boolean.TRUE);
            if (tokens.canConsume("FALSE")) return booleanFactory.asString(Boolean.FALSE);
        }

        // Otherwise it is an unquoted literal value ...
        Position pos = tokens.nextPosition();
        String sign = "";
        if (tokens.canConsume('-')) sign = "-";
        else if (tokens.canConsume('+')) sign = "";

        // Try to parse this value as a number ...
        String integral = tokens.consume();
        TypeFactory<Double> doubleFactory = typeSystem.getDoubleFactory();
        if (doubleFactory != null) {
            String decimal = null;
            if (tokens.canConsume('.')) {
                decimal = tokens.consume();
                String value = sign + integral + "." + decimal;
                if (decimal.endsWith("e") && (tokens.matches('+') || tokens.matches('-'))) {
                    // There's more to the number ...
                    value = value + tokens.consume() + tokens.consume(); // +/-EXP
                }
                try {
                    // Convert to a double and then back to a string to get canonical form ...
                    return doubleFactory.asString(doubleFactory.create(value));
                } catch (ValueFormatException e) {
                    String msg = GraphI18n.expectingLiteralAndUnableToParseAsDouble.text(value, pos.getLine(), pos.getColumn());
                    throw new ParsingException(pos, msg);
                }
            }
        }
        TypeFactory<?> dateTimeFactory = typeSystem.getDateTimeFactory();
        if (dateTimeFactory != null) {
            if (tokens.canConsume('-')) {
                // Looks like a date (see Section 3.6.4.3 of the JCR 2.0 specification) ...
                // sYYYY-MM-DDThh:mm:ss.sssTZD
                String year = integral;
                String month = tokens.consume();
                tokens.consume('-');
                String dateAndHour = tokens.consume();
                tokens.consume(':');
                String minutes = tokens.consume();
                tokens.consume(':');
                String seconds = tokens.consume();
                tokens.consume('.');
                String subSeconds = tokens.consume(); // should contain 'T' separator and possibly the TZ name and (if no +/-)
                // hours
                String tzSign = "+";
                String tzHours = "00";
                String tzMinutes = "00";
                String tzDelim = ":";
                if (tokens.canConsume('+')) {
                    // the fractionalSeconds did NOT contain the tzHours ...
                    tzHours = tokens.consume();
                    if (tokens.canConsume(':')) tzMinutes = tokens.consume();
                } else if (tokens.canConsume('-')) {
                    // the fractionalSeconds did NOT contain the tzHours ...
                    tzSign = "-";
                    tzHours = tokens.consume();
                    if (tokens.canConsume(':')) tzMinutes = tokens.consume();
                } else if (tokens.canConsume(':')) {
                    // fractionalSeconds DID contain the TZ hours (without + or -)
                    tzHours = tzSign = "";
                    if (tokens.canConsume(':')) tzMinutes = tokens.consume();
                } else if (subSeconds.endsWith("Z")) {
                    tzSign = tzMinutes = tzDelim = tzHours = "";
                } else if (subSeconds.endsWith("UTC")) {
                    subSeconds = subSeconds.length() > 3 ? subSeconds.substring(0, subSeconds.length() - 3) : subSeconds;
                }
                String value = sign + year + "-" + month + "-" + dateAndHour + ":" + minutes + ":" + seconds + "." + subSeconds
                               + tzSign + tzHours + tzDelim + tzMinutes;
                try {
                    // Convert to a date and then back to a string to get canonical form ...
                    Object dateTime = dateTimeFactory.create(value);
                    return dateTimeFactory.asString(dateTime);
                } catch (ValueFormatException e) {
                    String msg = GraphI18n.expectingLiteralAndUnableToParseAsDate.text(value, pos.getLine(), pos.getColumn());
                    throw new ParsingException(pos, msg);
                }
            }
        }
        TypeFactory<Long> longFactory = typeSystem.getLongFactory();
        // try to parse an a long ...
        String value = sign + integral;
        try {
            // Convert to a long and then back to a string to get canonical form ...
            return longFactory.asString(longFactory.create(value));
        } catch (ValueFormatException e) {
            String msg = GraphI18n.expectingLiteralAndUnableToParseAsLong.text(value, pos.getLine(), pos.getColumn());
            throw new ParsingException(pos, msg);
        }
    }

    protected DynamicOperand parseDynamicOperand( TokenStream tokens,
                                                  TypeSystem typeSystem,
                                                  Source source ) {
        DynamicOperand result = null;
        Position pos = tokens.nextPosition();
        if (tokens.canConsume('(')) {
            result = parseDynamicOperand(tokens, typeSystem, source);
            tokens.consume(")");
        } else if (tokens.canConsume("LENGTH", "(")) {
            result = length(parsePropertyValue(tokens, typeSystem, source));
            tokens.consume(")");
        } else if (tokens.canConsume("LOWER", "(")) {
            result = lowerCase(parseDynamicOperand(tokens, typeSystem, source));
            tokens.consume(")");
        } else if (tokens.canConsume("UPPER", "(")) {
            result = upperCase(parseDynamicOperand(tokens, typeSystem, source));
            tokens.consume(")");
        } else if (tokens.canConsume("NAME", "(")) {
            if (tokens.canConsume(")")) {
                if (source instanceof Selector) {
                    return nodeName(((Selector)source).name());
                }
                String msg = GraphI18n.functionIsAmbiguous.text("NAME()", pos.getLine(), pos.getColumn());
                throw new ParsingException(pos, msg);
            }
            result = nodeName(parseSelectorName(tokens, typeSystem));
            tokens.consume(")");
        } else if (tokens.canConsume("LOCALNAME", "(")) {
            if (tokens.canConsume(")")) {
                if (source instanceof Selector) {
                    return nodeLocalName(((Selector)source).name());
                }
                String msg = GraphI18n.functionIsAmbiguous.text("LOCALNAME()", pos.getLine(), pos.getColumn());
                throw new ParsingException(pos, msg);
            }
            result = nodeLocalName(parseSelectorName(tokens, typeSystem));
            tokens.consume(")");
        } else if (tokens.canConsume("SCORE", "(")) {
            if (tokens.canConsume(")")) {
                if (source instanceof Selector) {
                    return fullTextSearchScore(((Selector)source).name());
                }
                String msg = GraphI18n.functionIsAmbiguous.text("SCORE()", pos.getLine(), pos.getColumn());
                throw new ParsingException(pos, msg);
            }
            result = fullTextSearchScore(parseSelectorName(tokens, typeSystem));
            tokens.consume(")");
        } else if (tokens.canConsume("DEPTH", "(")) {
            if (tokens.canConsume(")")) {
                if (source instanceof Selector) {
                    return nodeDepth(((Selector)source).name());
                }
                String msg = GraphI18n.functionIsAmbiguous.text("DEPTH()", pos.getLine(), pos.getColumn());
                throw new ParsingException(pos, msg);
            }
            result = nodeDepth(parseSelectorName(tokens, typeSystem));
            tokens.consume(")");
        } else if (tokens.canConsume("PATH", "(")) {
            if (tokens.canConsume(")")) {
                if (source instanceof Selector) {
                    return nodePath(((Selector)source).name());
                }
                String msg = GraphI18n.functionIsAmbiguous.text("PATH()", pos.getLine(), pos.getColumn());
                throw new ParsingException(pos, msg);
            }
            result = nodePath(parseSelectorName(tokens, typeSystem));
            tokens.consume(")");
        } else if (tokens.canConsume("REFERENCE", "(")) {
            result = parseReferenceValue(tokens, typeSystem, source);
        } else {
            result = parsePropertyValue(tokens, typeSystem, source);
        }

        // Is this operand followed by an arithmetic operation ...
        ArithmeticOperator arithmeticOperator = null;
        if (tokens.canConsume('+')) {
            arithmeticOperator = ArithmeticOperator.ADD;
        } else if (tokens.canConsume('-')) {
            arithmeticOperator = ArithmeticOperator.SUBTRACT;
        } else if (tokens.canConsume('*')) {
            arithmeticOperator = ArithmeticOperator.MULTIPLY;
        } else if (tokens.canConsume('/')) {
            arithmeticOperator = ArithmeticOperator.DIVIDE;
        }
        if (arithmeticOperator != null) {
            if (tokens.matches('(')) {
                // Don't use precendence, but instead use the next DynamicOperand as the RHS ...
                DynamicOperand right = parseDynamicOperand(tokens, typeSystem, source);
                result = arithmeticOperand(result, arithmeticOperator, right);
            } else {
                // There is no parenthesis, so use operator precedence ...
                DynamicOperand right = parseDynamicOperand(tokens, typeSystem, source);
                if (right instanceof ArithmeticOperand) {
                    // But the RHS is an arithmetic operand, so we need to use operator precedence ...
                    ArithmeticOperand arithRhs = (ArithmeticOperand)right;
                    ArithmeticOperator rhsOperator = arithRhs.operator();
                    if (arithmeticOperator.precedes(rhsOperator)) {
                        // This operand's operator does take precedence, so this must be computed before working with the RHS ...
                        DynamicOperand newRhs = arithRhs.right();
                        DynamicOperand newLhs = new ArithmeticOperand(result, arithmeticOperator, arithRhs.left());
                        result = arithmeticOperand(newLhs, rhsOperator, newRhs);
                    } else {
                        result = arithmeticOperand(result, arithmeticOperator, right);
                    }
                } else {
                    // The RHS is just another DynamicOperand ...
                    result = arithmeticOperand(result, arithmeticOperator, right);
                }
            }
        }
        return result;
    }

    protected PropertyValue parsePropertyValue( TokenStream tokens,
                                                TypeSystem typeSystem,
                                                Source source ) {
        Position pos = tokens.nextPosition();
        String firstWord = parseName(tokens, typeSystem);
        SelectorName selectorName = null;
        if (tokens.canConsume('.')) {
            // We actually read the selector name, so now read the property name ...
            selectorName = new SelectorName(firstWord);
            String propertyName = parseName(tokens, typeSystem);
            return propertyValue(selectorName, propertyName);
        }
        // Otherwise the source should be a single named selector
        if (source instanceof Selector) {
            selectorName = ((Selector)source).aliasOrName();
            return propertyValue(selectorName, firstWord);
        }
        String msg = GraphI18n.mustBeScopedAtLineAndColumn.text(firstWord, pos.getLine(), pos.getColumn());
        throw new ParsingException(pos, msg);
    }

    protected ReferenceValue parseReferenceValue( TokenStream tokens,
                                                  TypeSystem typeSystem,
                                                  Source source ) {
        Position pos = tokens.nextPosition();
        SelectorName selectorName = null;
        if (tokens.canConsume(')')) {
            // There should be a single source ...
            if (source instanceof Selector) {
                selectorName = ((Selector)source).aliasOrName();
                return referenceValue(selectorName);
            }
            String msg = GraphI18n.functionIsAmbiguous.text("REFERENCE()", pos.getLine(), pos.getColumn());
            throw new ParsingException(pos, msg);
        }
        // Otherwise, there is at least one word inside the parentheses ...
        String firstWord = parseName(tokens, typeSystem);
        if (tokens.canConsume('.')) {
            // We actually read the selector name, so now read the property name ...
            selectorName = new SelectorName(firstWord);
            String propertyName = parseName(tokens, typeSystem);
            return referenceValue(selectorName, propertyName);
        }
        tokens.consume(")");
        // The name may be a selector name, or it may be a property name on the default selector.
        // If there is just a single selector ...
        if (source instanceof Selector) {
            Selector selector = (Selector)source;
            // and the selector name matches ...
            selectorName = new SelectorName(firstWord);
            if (selectorName.equals(selector.name()) || (selector.hasAlias() && selectorName.equals(selector.alias()))) {
                // This is a reference value with just the selector name ...
                return referenceValue(selectorName);
            }
            // Otherwise, the reference value is just the property name ...
            return referenceValue(selector.aliasOrName(), firstWord);
        }
        // Otherwise, the first word is the name of a selector ...
        selectorName = new SelectorName(firstWord);
        return referenceValue(selectorName);
    }

    protected Limit parseLimit( TokenStream tokens ) {
        if (tokens.canConsume("LIMIT")) {
            int first = tokens.consumeInteger();
            if (tokens.canConsume(',')) {
                // This is of the 'from,to' style ...
                int to = tokens.consumeInteger();
                int offset = to - first;
                if (offset < 0) {
                    Position pos = tokens.previousPosition();
                    String msg = GraphI18n.secondValueInLimitRangeCannotBeLessThanFirst.text(first,
                                                                                             to,
                                                                                             pos.getLine(),
                                                                                             pos.getColumn());
                    throw new ParsingException(pos, msg);
                }
                return limit(offset, first);
            }
            if (tokens.canConsume("OFFSET")) {
                int offset = tokens.consumeInteger();
                return limit(first, offset);
            }
            // No offset
            return limit(first, 0);
        }
        return null;
    }

    /**
     * Remove all leading and trailing single-quotes, double-quotes, or square brackets from the supplied text. If multiple,
     * properly-paired quotes or brackets are found, they will all be removed.
     * 
     * @param text the input text; may not be null
     * @return the text without leading and trailing brackets and quotes, or <code>text</code> if there were no square brackets or
     *         quotes
     */
    protected String removeBracketsAndQuotes( String text ) {
        return removeBracketsAndQuotes(text, true);
    }

    /**
     * Remove any leading and trailing single-quotes, double-quotes, or square brackets from the supplied text.
     * 
     * @param text the input text; may not be null
     * @param recursive true if more than one pair of quotes, double-quotes, or square brackets should be removed, or false if
     *        just the first pair should be removed
     * @return the text without leading and trailing brackets and quotes, or <code>text</code> if there were no square brackets or
     *         quotes
     */
    protected String removeBracketsAndQuotes( String text,
                                              boolean recursive ) {
        if (text.length() > 0) {
            char firstChar = text.charAt(0);
            switch (firstChar) {
                case '\'':
                case '"':
                    assert text.charAt(text.length() - 1) == firstChar;
                    String removed = text.substring(1, text.length() - 1);
                    return recursive ? removeBracketsAndQuotes(removed, recursive) : removed;
                case '[':
                    assert text.charAt(text.length() - 1) == ']';
                    removed = text.substring(1, text.length() - 1);
                    return recursive ? removeBracketsAndQuotes(removed, recursive) : removed;
            }
        }
        return text;
    }

    protected NamedSelector parseNamedSelector( TokenStream tokens,
                                                TypeSystem typeSystem ) {
        SelectorName name = parseSelectorName(tokens, typeSystem);
        SelectorName alias = null;
        if (tokens.canConsume("AS")) alias = parseSelectorName(tokens, typeSystem);
        return new NamedSelector(name, alias);
    }

    protected SelectorName parseSelectorName( TokenStream tokens,
                                              TypeSystem typeSystem ) {
        return new SelectorName(parseName(tokens, typeSystem));
    }

    protected String parsePath( TokenStream tokens,
                                TypeSystem typeSystem ) {
        return removeBracketsAndQuotes(tokens.consume());
    }

    protected String parseName( TokenStream tokens,
                                TypeSystem typeSystem ) {
        return removeBracketsAndQuotes(tokens.consume());
    }

    protected String parseName( String token,
                                TypeSystem typeSystem ) {
        return removeBracketsAndQuotes(token);
    }

    protected Query query( Source source,
                           Constraint constraint,
                           List<? extends Ordering> orderings,
                           List<? extends Column> columns,
                           Limit limit,
                           boolean distinct ) {
        return new Query(source, constraint, orderings, columns, limit, distinct);
    }

    protected SetQuery setQuery( QueryCommand leftQuery,
                                 Operation operation,
                                 QueryCommand rightQuery,
                                 boolean all ) {
        return new SetQuery(leftQuery, operation, rightQuery, all);
    }

    protected Length length( PropertyValue propertyValue ) {
        return new Length(propertyValue);
    }

    protected LowerCase lowerCase( DynamicOperand operand ) {
        return new LowerCase(operand);
    }

    protected UpperCase upperCase( DynamicOperand operand ) {
        return new UpperCase(operand);
    }

    protected NodeName nodeName( SelectorName selector ) {
        return new NodeName(selector);
    }

    protected NodeLocalName nodeLocalName( SelectorName selector ) {
        return new NodeLocalName(selector);
    }

    protected NodeDepth nodeDepth( SelectorName selector ) {
        return new NodeDepth(selector);
    }

    protected NodePath nodePath( SelectorName selector ) {
        return new NodePath(selector);
    }

    protected EquiJoinCondition equiJoinCondition( SelectorName selector1,
                                                   String property1,
                                                   SelectorName selector2,
                                                   String property2 ) {
        return new EquiJoinCondition(selector1, property1, selector2, property2);
    }

    protected DescendantNodeJoinCondition descendantNodeJoinCondition( SelectorName ancestor,
                                                                       SelectorName descendant ) {
        return new DescendantNodeJoinCondition(ancestor, descendant);
    }

    protected ChildNodeJoinCondition childNodeJoinCondition( SelectorName parent,
                                                             SelectorName child ) {
        return new ChildNodeJoinCondition(parent, child);
    }

    protected SameNodeJoinCondition sameNodeJoinCondition( SelectorName selector1,
                                                           SelectorName selector2 ) {
        return new SameNodeJoinCondition(selector1, selector2);
    }

    protected SameNodeJoinCondition sameNodeJoinCondition( SelectorName selector1,
                                                           SelectorName selector2,
                                                           String path ) {
        return new SameNodeJoinCondition(selector1, selector2, path);
    }

    protected Limit limit( int rowCount,
                           int offset ) {
        return new Limit(rowCount, offset);
    }

    protected Column column( SelectorName selectorName,
                             String propertyName,
                             String columnName ) {
        return new Column(selectorName, propertyName, columnName);
    }

    protected Join join( Source left,
                         JoinType joinType,
                         Source right,
                         JoinCondition joinCondition ) {
        return new Join(left, joinType, right, joinCondition);
    }

    protected Not not( Constraint constraint ) {
        return new Not(constraint);
    }

    protected And and( Constraint constraint1,
                       Constraint constraint2 ) {
        return new And(constraint1, constraint2);
    }

    protected Or or( Constraint constraint1,
                     Constraint constraint2 ) {
        return new Or(constraint1, constraint2);
    }

    protected Between between( DynamicOperand operand,
                               StaticOperand lowerBound,
                               StaticOperand upperBound,
                               boolean lowerInclusive,
                               boolean upperInclusive ) {
        return new Between(operand, lowerBound, upperBound, lowerInclusive, upperInclusive);
    }

    protected SetCriteria setCriteria( DynamicOperand operand,
                                       Collection<? extends StaticOperand> values ) {
        return new SetCriteria(operand, values);
    }

    protected FullTextSearch fullTextSearch( SelectorName name,
                                             String propertyName,
                                             String expression,
                                             Term term ) {
        return new FullTextSearch(name, propertyName, expression, term);
    }

    protected SameNode sameNode( SelectorName name,
                                 String path ) {
        return new SameNode(name, path);
    }

    protected ChildNode childNode( SelectorName name,
                                   String path ) {
        return new ChildNode(name, path);
    }

    protected DescendantNode descendantNode( SelectorName name,
                                             String path ) {
        return new DescendantNode(name, path);
    }

    protected Comparison comparison( DynamicOperand left,
                                     Operator operator,
                                     StaticOperand right ) {
        return new Comparison(left, operator, right);
    }

    protected Ordering ordering( DynamicOperand operand,
                                 Order order ) {
        return new Ordering(operand, order);
    }

    protected PropertyExistence propertyExistence( SelectorName selector,
                                                   String propertyName ) {
        return new PropertyExistence(selector, propertyName);
    }

    protected FullTextSearchScore fullTextSearchScore( SelectorName selector ) {
        return new FullTextSearchScore(selector);
    }

    protected ArithmeticOperand arithmeticOperand( DynamicOperand leftOperand,
                                                   ArithmeticOperator operator,
                                                   DynamicOperand rightOperand ) {
        return new ArithmeticOperand(leftOperand, operator, rightOperand);
    }

    protected PropertyValue propertyValue( SelectorName selector,
                                           String propertyName ) {
        return new PropertyValue(selector, propertyName);
    }

    protected ReferenceValue referenceValue( SelectorName selector ) {
        return new ReferenceValue(selector);
    }

    protected ReferenceValue referenceValue( SelectorName selector,
                                             String propertyName ) {
        return new ReferenceValue(selector, propertyName);
    }

    protected BindVariableName bindVariableName( String variableName ) {
        return new BindVariableName(variableName);
    }

    protected Literal literal( TypeSystem typeSystem,
                               Object value ) throws ValueFormatException {
        return new Literal(value);
    }

    /**
     * A {@link TokenStream.Tokenizer} implementation that parses words, quoted phrases, comments, and symbols. Words are
     * delimited by whitespace and consist only of alpha-number characters plus the underscore character. Quoted phrases are
     * delimited by single-quote and double-quote characters (which may be escaped within the quote). Comments are the characters
     * starting with '/*' and ending with '&#42;/', or starting with '--' and ending with the next line terminator (or the end of
     * the content).
     */
    public static class SqlTokenizer implements TokenStream.Tokenizer {
        /**
         * The token type for tokens that represent an unquoted string containing a character sequence made up of non-whitespace
         * and non-symbol characters.
         */
        public static final int WORD = 1;
        /**
         * The token type for tokens that consist of an individual "symbol" character. The set of characters includes:
         * <code>[]<>=-+(),</code>
         */
        public static final int SYMBOL = 2;
        /**
         * The token type for tokens that consist of other characters.
         */
        public static final int OTHER = 3;
        /**
         * The token type for tokens that consist of all the characters within single-quotes, double-quotes, or square brackets.
         */
        public static final int QUOTED_STRING = 4;
        /**
         * The token type for tokens that consist of all the characters between "/*" and "&#42;/" or between "--" and the next
         * line terminator (e.g., '\n', '\r' or "\r\n")
         */
        public static final int COMMENT = 6;

        private final boolean useComments;

        public SqlTokenizer( boolean useComments ) {
            this.useComments = useComments;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.common.text.TokenStream.Tokenizer#tokenize(CharacterStream, Tokens)
         */
        public void tokenize( CharacterStream input,
                              Tokens tokens ) throws ParsingException {
            while (input.hasNext()) {
                char c = input.next();
                switch (c) {
                    case ' ':
                    case '\t':
                    case '\n':
                    case '\r':
                        // Just skip these whitespace characters ...
                        break;
                    case '(':
                    case ')':
                    case '{':
                    case '}':
                    case '*':
                    case '.':
                    case ',':
                    case ';':
                    case '+':
                    case '%':
                    case '?':
                    case '$':
                    case ']':
                    case '!':
                    case '<':
                    case '>':
                    case '|':
                    case '=':
                    case ':':
                        tokens.addToken(input.position(input.index()), input.index(), input.index() + 1, SYMBOL);
                        break;
                    case '\'':
                    case '[':
                    case '\"':
                        int startIndex = input.index();
                        char closingChar = c == '[' ? ']' : c;
                        Position pos = input.position(startIndex);
                        boolean foundClosingQuote = false;
                        while (input.hasNext()) {
                            c = input.next();
                            if (c == '\\' && input.isNext(closingChar)) {
                                c = input.next(); // consume the closingChar since it is escaped
                            } else if (c == closingChar) {
                                foundClosingQuote = true;
                                break;
                            }
                        }
                        if (!foundClosingQuote) {
                            String msg = CommonI18n.noMatchingDoubleQuoteFound.text(pos.getLine(), pos.getColumn());
                            if (closingChar == '\'') {
                                msg = CommonI18n.noMatchingSingleQuoteFound.text(pos.getLine(), pos.getColumn());
                            } else if (closingChar == ']') {
                                msg = GraphI18n.noMatchingBracketFound.text(pos.getLine(), pos.getColumn());
                            }
                            throw new ParsingException(pos, msg);
                        }
                        int endIndex = input.index() + 1; // beyond last character read
                        tokens.addToken(pos, startIndex, endIndex, QUOTED_STRING);
                        break;
                    case '-':
                        startIndex = input.index();
                        pos = input.position(input.index());
                        if (input.isNext('-')) {
                            // End-of-line comment ...
                            boolean foundLineTerminator = false;
                            while (input.hasNext()) {
                                c = input.next();
                                if (c == '\n' || c == '\r') {
                                    foundLineTerminator = true;
                                    break;
                                }
                            }
                            endIndex = input.index(); // the token won't include the '\n' or '\r' character(s)
                            if (!foundLineTerminator) ++endIndex; // must point beyond last char
                            if (c == '\r' && input.isNext('\n')) input.next();
                            if (useComments) {
                                tokens.addToken(pos, startIndex, endIndex, COMMENT);
                            }
                        } else {
                            tokens.addToken(input.position(input.index()), input.index(), input.index() + 1, SYMBOL);
                            break;
                        }
                        break;
                    case '/':
                        startIndex = input.index();
                        pos = input.position(input.index());
                        if (input.isNext('*')) {
                            // Multi-line comment ...
                            while (input.hasNext() && !input.isNext('*', '/')) {
                                c = input.next();
                            }
                            if (input.hasNext()) input.next(); // consume the '*'
                            if (input.hasNext()) input.next(); // consume the '/'
                            if (useComments) {
                                endIndex = input.index() + 1; // the token will include the quote characters
                                tokens.addToken(pos, startIndex, endIndex, COMMENT);
                            }
                        } else {
                            tokens.addToken(input.position(input.index()), input.index(), input.index() + 1, SYMBOL);
                            break;
                        }
                        break;
                    default:
                        startIndex = input.index();
                        pos = input.position(input.index());
                        // Read as long as there is a valid XML character ...
                        int tokenType = (Character.isLetterOrDigit(c) || c == '_') ? WORD : OTHER;
                        while (input.isNextLetterOrDigit() || input.isNext('_')) {
                            c = input.next();
                        }
                        endIndex = input.index() + 1; // beyond last character that was included
                        tokens.addToken(pos, startIndex, endIndex, tokenType);
                }
            }
        }
    }
}
