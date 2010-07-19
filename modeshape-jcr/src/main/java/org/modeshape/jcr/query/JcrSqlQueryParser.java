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
package org.modeshape.jcr.query;

import static org.modeshape.common.text.TokenStream.ANY_VALUE;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.text.Position;
import org.modeshape.common.text.TokenStream;
import org.modeshape.graph.GraphI18n;
import org.modeshape.graph.property.ValueFormatException;
import org.modeshape.graph.query.model.And;
import org.modeshape.graph.query.model.Comparison;
import org.modeshape.graph.query.model.Constraint;
import org.modeshape.graph.query.model.DynamicOperand;
import org.modeshape.graph.query.model.FullTextSearch;
import org.modeshape.graph.query.model.FullTextSearchScore;
import org.modeshape.graph.query.model.Join;
import org.modeshape.graph.query.model.JoinType;
import org.modeshape.graph.query.model.Literal;
import org.modeshape.graph.query.model.NamedSelector;
import org.modeshape.graph.query.model.NodePath;
import org.modeshape.graph.query.model.Not;
import org.modeshape.graph.query.model.Operator;
import org.modeshape.graph.query.model.Or;
import org.modeshape.graph.query.model.PropertyExistence;
import org.modeshape.graph.query.model.PropertyValue;
import org.modeshape.graph.query.model.Query;
import org.modeshape.graph.query.model.SameNodeJoinCondition;
import org.modeshape.graph.query.model.Selector;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.SetCriteria;
import org.modeshape.graph.query.model.Source;
import org.modeshape.graph.query.model.StaticOperand;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.model.Visitor;
import org.modeshape.graph.query.model.TypeSystem.TypeFactory;
import org.modeshape.graph.query.parse.FullTextSearchParser;
import org.modeshape.graph.query.parse.SqlQueryParser;
import org.modeshape.jcr.JcrI18n;

/**
 * Parser for JCR-SQL queries that produces {@link org.modeshape.graph.query.model abstract query model (AQM)} objects.
 * <p></p>
 * <h3>JCR-SQL grammar</h3>
 * <p>
 * This section defines the complete grammar for the JCR-SQL dialect supported by this parser, as defined by the
 * JCR 1.0.1 specification. This parser actually extends the {@link SqlQueryParser (extended) JCR-SQL2 parser},
 * and thus allows many of the JCR-SQL2 standard and extended features, although there are several key differences:
 * <ol>
 * <li>Names are not enclosed by square brackets.</li>
 * <li>Criteria on scores use <pre>jcr:score</pre> as a pseudo-column.</li>
 * <li>Criteria on path use <pre>jcr:path</pre> as a pseudo-column.</li>
 * <li>Joins are specified with comma-separated table names in the FROM clause and join criteria in the WHERE clause.</li>
 * </ol>
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
public class JcrSqlQueryParser extends SqlQueryParser {

    @SuppressWarnings( "deprecation" )
    public static final String LANGUAGE = javax.jcr.query.Query.SQL;

    /**
     * 
     */
    public JcrSqlQueryParser() {
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.QueryParser#getLanguage()
     */
    @Override
    public String getLanguage() {
        return LANGUAGE;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#parseQuery(org.modeshape.common.text.TokenStream,
     *      org.modeshape.graph.query.model.TypeSystem)
     */
    @Override
    protected Query parseQuery( TokenStream tokens,
                                TypeSystem typeSystem ) {
        Query query = super.parseQuery(tokens, typeSystem);
        // See if we have to rewrite the JCR-SQL-style join ...
        if (query.source() instanceof JoinableSources) {
            JoinableSources joinableSources = (JoinableSources)query.source();
            // Rewrite the joins ...
            Source newSource = rewrite(joinableSources);
            query = new Query(newSource, query.constraint(), query.orderings(), query.columns(), query.limits(),
                              query.isDistinct());
        }
        return query;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#parseFrom(org.modeshape.common.text.TokenStream,
     *      org.modeshape.graph.query.model.TypeSystem)
     */
    @Override
    protected Source parseFrom( TokenStream tokens,
                                TypeSystem typeSystem ) {
        Position firstSourcePosition = tokens.nextPosition();
        Source source = super.parseFrom(tokens, typeSystem);
        if (tokens.matches(',') && source instanceof NamedSelector) {
            NamedSelector selector = (NamedSelector)source;
            JoinableSources joinedSources = new JoinableSources(selector, firstSourcePosition);
            while (tokens.canConsume(',')) {
                // This is a JCR-SQL-style JOIN ...
                Position nextSourcePosition = tokens.nextPosition();
                NamedSelector nextSource = parseNamedSelector(tokens, typeSystem);
                joinedSources.add(nextSource, nextSourcePosition);
            }
            source = joinedSources;
        }
        return source;
    }

    /**
     * Parse a constraint clause. This method inherits all of the functionality from JCR-SQL2, except that JCR-SQL allows
     * constraints that use "<code>jcr:path</code>" and "<code>jcr:score</code>" pseudo-columns. In these special cases, the
     * resulting {@link Comparison comparison} will have a {@link NodePath} or {@link FullTextSearchScore} dynamic operand.
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#parseConstraint(org.modeshape.common.text.TokenStream,
     *      org.modeshape.graph.query.model.TypeSystem, org.modeshape.graph.query.model.Source)
     */
    @Override
    protected Constraint parseConstraint( TokenStream tokens,
                                          TypeSystem typeSystem,
                                          Source source ) {
        Constraint constraint = null;
        if (tokens.canConsume("JCR", ":", "PATH")) {
            // It is a property constraint on "jcr:path" ...
            SelectorName selector = getSelectorNameFor(source);
            PropertyValue value = new PropertyValue(selector, "jcr:path");
            Operator operator = parseComparisonOperator(tokens);
            StaticOperand right = parseStaticOperand(tokens, typeSystem);
            constraint = rewriteConstraint(new Comparison(value, operator, right));
        } else if (tokens.matches(ANY_VALUE, "IN")) {
            // This is a "... 'value' IN prop ..." pattern used in the JCR TCK tests but not in the JCR 1.0.1 specification
            // ...
            Literal value = parseLiteral(tokens, typeSystem);
            tokens.consume("IN");
            PropertyValue propertyValue = parsePropertyValue(tokens, typeSystem, source);
            constraint = new SetCriteria(propertyValue, value);
        } else if (source instanceof JoinableSources
                   && !(tokens.matches("(") || tokens.matches("NOT") || tokens.matches("CONTAINS", "(")
                        || tokens.matches("ISSAMENODE", "(") || tokens.matches("ISCHILDNODE", "(") || tokens.matches("ISDESCENDANTNODE",
                                                                                                                     "("))) {
            JoinableSources joinableSources = (JoinableSources)source;
            // See if this is a join condition ...
            if (tokens.matches(ANY_VALUE, ":", ANY_VALUE, ".", "JCR", ":", "PATH", "=")
                || tokens.matches(ANY_VALUE, ".", "JCR", ":", "PATH", "=")) {
                Position position = tokens.nextPosition();
                SelectorName selector1 = parseSelectorName(tokens, typeSystem);
                tokens.consume('.');
                parseName(tokens, typeSystem); // jcr:path
                tokens.consume('=');
                SelectorName selector2 = parseSelectorName(tokens, typeSystem);
                tokens.consume('.');
                parseName(tokens, typeSystem); // jcr:path
                joinableSources.add(new SameNodeJoinCondition(selector1, selector2), position);

                // AND has higher precedence than OR, so we need to evaluate it first ...
                while (tokens.canConsume("AND")) {
                    Constraint rhs = parseConstraint(tokens, typeSystem, source);
                    if (rhs != null) constraint = constraint != null ? new And(constraint, rhs) : rhs;
                }
                while (tokens.canConsume("OR")) {
                    Constraint rhs = parseConstraint(tokens, typeSystem, source);
                    if (rhs != null) constraint = constraint != null ? new And(constraint, rhs) : rhs;
                }
                return constraint;
            }
        }
        if (constraint != null) {
            // AND has higher precedence than OR, so we need to evaluate it first ...
            while (tokens.canConsume("AND")) {
                Constraint rhs = parseConstraint(tokens, typeSystem, source);
                if (rhs != null) constraint = new And(constraint, rhs);
            }
            while (tokens.canConsume("OR")) {
                Constraint rhs = parseConstraint(tokens, typeSystem, source);
                if (rhs != null) constraint = new Or(constraint, rhs);
            }
            return constraint;
        }

        constraint = super.parseConstraint(tokens, typeSystem, source);
        constraint = rewriteConstraint(constraint);
        return constraint;
    }

    @Override
    protected Constraint parsePropertyExistance( TokenStream tokens,
                                                 TypeSystem typeSystem,
                                                 Source source ) {
        if (tokens.matches(ANY_VALUE, "IS", "NOT", "NULL") || tokens.matches(ANY_VALUE, "IS", "NULL")
            || tokens.matches(ANY_VALUE, ".", ANY_VALUE, "IS", "NOT", "NULL")
            || tokens.matches(ANY_VALUE, ".", ANY_VALUE, ":", ANY_VALUE, "IS", "NOT", "NULL")
            || tokens.matches(ANY_VALUE, ".", ANY_VALUE, "IS", "NULL")
            || tokens.matches(ANY_VALUE, ".", ANY_VALUE, ":", ANY_VALUE, "IS", "NULL")
            || tokens.matches(ANY_VALUE, ":", ANY_VALUE, "IS", "NOT", "NULL")
            || tokens.matches(ANY_VALUE, ":", ANY_VALUE, ".", ANY_VALUE, "IS", "NOT", "NULL")
            || tokens.matches(ANY_VALUE, ":", ANY_VALUE, ".", ANY_VALUE, ":", ANY_VALUE, "IS", "NOT", "NULL")
            || tokens.matches(ANY_VALUE, ":", ANY_VALUE, ".", ANY_VALUE, "IS", "NULL")
            || tokens.matches(ANY_VALUE, ":", ANY_VALUE, ".", ANY_VALUE, ":", ANY_VALUE, "IS", "NULL")) {
            Position pos = tokens.nextPosition();
            String firstWord = parseName(tokens, typeSystem);
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
                propertyName = firstWord;
            }
            if (tokens.canConsume("IS", "NOT", "NULL")) {
                return new PropertyExistence(selectorName, propertyName);
            }
            tokens.consume("IS", "NULL");
            return new Not(new PropertyExistence(selectorName, propertyName));
        }
        return null;
    }

    protected SelectorName getSelectorNameFor( Source source ) {
        // Since JCR-SQL only allows ISSAMENODE join constraints, it doesn't matter which source we select ...
        if (source instanceof JoinableSources) {
            return ((JoinableSources)source).getSelectors().values().iterator().next().aliasOrName();
        }
        if (source instanceof Selector) {
            return ((Selector)source).aliasOrName();
        }
        assert false;
        return null;
    }

    protected Constraint rewriteConstraint( Constraint constraint ) {
        if (constraint instanceof Comparison) {
            Comparison comparison = (Comparison)constraint;
            DynamicOperand left = comparison.operand1();
            if (left instanceof PropertyValue) {
                PropertyValue propValue = (PropertyValue)left;
                if ("jcr:path".equals(propValue.propertyName())) {
                    // Rewrite this constraint as a PATH criteria ...
                    NodePath path = new NodePath(propValue.selectorName());
                    return new Comparison(path, comparison.operator(), comparison.operand2());
                }
                if ("jcr:score".equals(propValue.propertyName())) {
                    // Rewrite this constraint as a SCORE criteria ...
                    FullTextSearchScore score = new FullTextSearchScore(propValue.selectorName());
                    return new Comparison(score, comparison.operator(), comparison.operand2());
                }
            }
        } else if (constraint instanceof FullTextSearch) {
            FullTextSearch search = (FullTextSearch)constraint;
            if (".".equals(search.propertyName())) {
                // JCR-SQL's use of CONTAINS allows a '.' to be used to represent the search is to be
                // performed on all properties of the node(s). However, JCR-SQL2 and our AQM
                // expect a '*' to be used instead ...
                return new FullTextSearch(search.selectorName(), search.fullTextSearchExpression());
            }
        } else if (constraint instanceof And) {
            And and = (And)constraint;
            constraint = new And(rewriteConstraint(and.left()), rewriteConstraint(and.right()));
        } else if (constraint instanceof Or) {
            Or or = (Or)constraint;
            constraint = new Or(rewriteConstraint(or.left()), rewriteConstraint(or.right()));
        }
        return constraint;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Parsing behavior is overridden to that JCR-SQL style (unquoted prefixed) names are allowed. This method parses the selector
     * name, which may be of the form "<code>unprefixedName</code>" (consisting of a single token) or "<code>prefix:name</code>"
     * (consisting of three tokens).
     * </p>
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#parseName(org.modeshape.common.text.TokenStream,
     *      org.modeshape.graph.query.model.TypeSystem)
     */
    @Override
    protected String parseName( TokenStream tokens,
                                TypeSystem typeSystem ) {
        String token1 = tokens.consume();
        token1 = removeBracketsAndQuotes(token1);
        if (tokens.canConsume(':')) {
            String token2 = tokens.consume();
            token2 = removeBracketsAndQuotes(token2);
            return token1 + ':' + token2;
        }
        return token1;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#parseLiteralValue(org.modeshape.common.text.TokenStream,
     *      org.modeshape.graph.query.model.TypeSystem)
     */
    @Override
    protected Object parseLiteralValue( TokenStream tokens,
                                        TypeSystem typeSystem ) {
        if (tokens.canConsume("TIMESTAMP")) {
            Position pos = tokens.previousPosition();
            // This should be a timestamp represented as a single-quoted string ...
            String value = removeBracketsAndQuotes(tokens.consume());
            TypeFactory<?> dateTimeFactory = typeSystem.getDateTimeFactory();
            try {
                // Convert to a date and then back to a string to get canonical form ...
                Object dateTime = dateTimeFactory.create(value);
                return dateTime;
                // return dateTimeFactory.asString(dateTime);
            } catch (ValueFormatException e) {
                String msg = GraphI18n.expectingLiteralAndUnableToParseAsDate.text(value, pos.getLine(), pos.getColumn());
                throw new ParsingException(pos, msg);
            }
        }
        return super.parseLiteralValue(tokens, typeSystem);
    }

    /**
     * Remove any leading and trailing single-quotes.
     * 
     * @param text the input text; may not be null
     * @return the text without leading and trailing quotes, or <code>text</code> if there were no quotes
     */
    @Override
    protected String removeBracketsAndQuotes( String text ) {
        if (text.length() > 0) {
            char firstChar = text.charAt(0);
            switch (firstChar) {
                case '\'':
                    assert text.charAt(text.length() - 1) == firstChar;
                    return removeBracketsAndQuotes(text.substring(1, text.length() - 1));
            }
        }
        return text;
    }

    protected Source rewrite( JoinableSources joinableSources ) {
        // Find the order of the joins ...
        List<Join> joins = new LinkedList<Join>();
        for (SameNodeJoinCondition joinCondition : joinableSources.getJoinConditions()) {
            SelectorName selector1 = joinCondition.selector1Name();
            SelectorName selector2 = joinCondition.selector2Name();
            boolean found = false;
            ListIterator<Join> iter = joins.listIterator();
            while (iter.hasNext()) {
                Join next = iter.next();
                Join replacement = null;
                if (usesSelector(next, selector1)) {
                    Source right = joinableSources.getSelectors().get(selector2.name());
                    replacement = new Join(next, JoinType.INNER, right, joinCondition);
                } else if (usesSelector(next, selector2)) {
                    Source left = joinableSources.getSelectors().get(selector1.name());
                    replacement = new Join(left, JoinType.INNER, next, joinCondition);
                }
                if (replacement != null) {
                    iter.previous();
                    iter.remove();
                    joins.add(replacement);
                    found = true;
                    break;
                }
            }
            if (!found) {
                // Nothing matched, so add a new join ...
                Source left = joinableSources.getSelectors().get(selector1.name());
                Source right = joinableSources.getSelectors().get(selector2.name());
                if (left == null) {
                    Position pos = joinableSources.getJoinCriteriaPosition();
                    String msg = JcrI18n.selectorUsedInEquiJoinCriteriaDoesNotExistInQuery.text(selector1.name(),
                                                                                                pos.getLine(),
                                                                                                pos.getColumn());
                    throw new ParsingException(pos, msg);
                }
                if (right == null) {
                    Position pos = joinableSources.getJoinCriteriaPosition();
                    String msg = JcrI18n.selectorUsedInEquiJoinCriteriaDoesNotExistInQuery.text(selector2.name(),
                                                                                                pos.getLine(),
                                                                                                pos.getColumn());
                    throw new ParsingException(pos, msg);
                }
                joins.add(new Join(left, JoinType.INNER, right, joinCondition));
            }
        }
        if (joins.size() == 1) {
            return joins.get(0);
        }
        // Otherwise the join conditions were not sufficient
        return null;
    }

    protected boolean usesSelector( Join join,
                                    SelectorName selector ) {
        Source left = join.left();
        if (left instanceof Selector && selector.equals(((Selector)left).aliasOrName())) return true;
        if (left instanceof Join && usesSelector((Join)left, selector)) return true;
        Source right = join.right();
        if (right instanceof Selector && selector.equals(((Selector)right).aliasOrName())) return true;
        if (right instanceof Join && usesSelector((Join)right, selector)) return true;
        return false;
    }

    protected static class JoinableSources implements Source {
        private static final long serialVersionUID = 1L;
        private transient Map<String, Selector> selectors = new LinkedHashMap<String, Selector>();
        private transient List<SameNodeJoinCondition> joinConditions = new ArrayList<SameNodeJoinCondition>();
        private transient List<Position> selectorPositions = new ArrayList<Position>();
        private transient Position joinCriteriaPosition;

        protected JoinableSources( Selector firstSelector,
                                   Position position ) {
            add(firstSelector, position);
        }

        public void add( Selector selector,
                         Position position ) {
            selectors.put(selector.aliasOrName().name(), selector);
            selectorPositions.add(position);
        }

        public void add( SameNodeJoinCondition joinCondition,
                         Position position ) {
            joinConditions.add(joinCondition);
            joinCriteriaPosition = position;
        }

        public Iterable<String> selectorNames() {
            return selectors.keySet();
        }

        /**
         * @return joinConditions
         */
        public List<SameNodeJoinCondition> getJoinConditions() {
            return joinConditions;
        }

        /**
         * @return selectors
         */
        public Map<String, Selector> getSelectors() {
            return selectors;
        }

        /**
         * @return joinCriteriaPosition
         */
        public Position getJoinCriteriaPosition() {
            return joinCriteriaPosition;
        }

        public Position getPositionForSelector( String selector ) {
            int index = 0;
            for (Map.Entry<String, Selector> entry : selectors.entrySet()) {
                if (entry.getKey().equals(selector)) return selectorPositions.get(index);
                ++index;
            }
            return null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.graph.query.model.Visitable#accept(org.modeshape.graph.query.model.Visitor)
         */
        @Override
        public void accept( Visitor visitor ) {
        }

    }
}
