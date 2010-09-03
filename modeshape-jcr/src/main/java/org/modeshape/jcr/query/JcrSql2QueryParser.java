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
package org.modeshape.jcr.query;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.List;
import javax.jcr.Binary;
import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFactory;
import javax.jcr.query.Query;
import org.modeshape.common.text.TokenStream;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.query.model.ArithmeticOperator;
import org.modeshape.graph.query.model.Between;
import org.modeshape.graph.query.model.Column;
import org.modeshape.graph.query.model.Constraint;
import org.modeshape.graph.query.model.DynamicOperand;
import org.modeshape.graph.query.model.JoinCondition;
import org.modeshape.graph.query.model.JoinType;
import org.modeshape.graph.query.model.Length;
import org.modeshape.graph.query.model.Limit;
import org.modeshape.graph.query.model.Operator;
import org.modeshape.graph.query.model.Order;
import org.modeshape.graph.query.model.Ordering;
import org.modeshape.graph.query.model.PropertyValue;
import org.modeshape.graph.query.model.QueryCommand;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.SetCriteria;
import org.modeshape.graph.query.model.SetQuery;
import org.modeshape.graph.query.model.Source;
import org.modeshape.graph.query.model.StaticOperand;
import org.modeshape.graph.query.model.Subquery;
import org.modeshape.graph.query.model.TypeSystem;
import org.modeshape.graph.query.model.FullTextSearch.Term;
import org.modeshape.graph.query.model.SetQuery.Operation;
import org.modeshape.graph.query.parse.SqlQueryParser;
import org.modeshape.jcr.query.qom.JcrAnd;
import org.modeshape.jcr.query.qom.JcrArithmeticOperand;
import org.modeshape.jcr.query.qom.JcrBetween;
import org.modeshape.jcr.query.qom.JcrBindVariableName;
import org.modeshape.jcr.query.qom.JcrChildNode;
import org.modeshape.jcr.query.qom.JcrChildNodeJoinCondition;
import org.modeshape.jcr.query.qom.JcrColumn;
import org.modeshape.jcr.query.qom.JcrComparison;
import org.modeshape.jcr.query.qom.JcrConstraint;
import org.modeshape.jcr.query.qom.JcrDescendantNode;
import org.modeshape.jcr.query.qom.JcrDescendantNodeJoinCondition;
import org.modeshape.jcr.query.qom.JcrDynamicOperand;
import org.modeshape.jcr.query.qom.JcrEquiJoinCondition;
import org.modeshape.jcr.query.qom.JcrFullTextSearch;
import org.modeshape.jcr.query.qom.JcrFullTextSearchScore;
import org.modeshape.jcr.query.qom.JcrJoin;
import org.modeshape.jcr.query.qom.JcrJoinCondition;
import org.modeshape.jcr.query.qom.JcrLength;
import org.modeshape.jcr.query.qom.JcrLimit;
import org.modeshape.jcr.query.qom.JcrLiteral;
import org.modeshape.jcr.query.qom.JcrLowerCase;
import org.modeshape.jcr.query.qom.JcrNamedSelector;
import org.modeshape.jcr.query.qom.JcrNodeDepth;
import org.modeshape.jcr.query.qom.JcrNodeLocalName;
import org.modeshape.jcr.query.qom.JcrNodeName;
import org.modeshape.jcr.query.qom.JcrNodePath;
import org.modeshape.jcr.query.qom.JcrNot;
import org.modeshape.jcr.query.qom.JcrOr;
import org.modeshape.jcr.query.qom.JcrOrdering;
import org.modeshape.jcr.query.qom.JcrPropertyExistence;
import org.modeshape.jcr.query.qom.JcrPropertyValue;
import org.modeshape.jcr.query.qom.JcrQueryCommand;
import org.modeshape.jcr.query.qom.JcrReferenceValue;
import org.modeshape.jcr.query.qom.JcrSameNode;
import org.modeshape.jcr.query.qom.JcrSameNodeJoinCondition;
import org.modeshape.jcr.query.qom.JcrSelectQuery;
import org.modeshape.jcr.query.qom.JcrSetCriteria;
import org.modeshape.jcr.query.qom.JcrSetQuery;
import org.modeshape.jcr.query.qom.JcrSource;
import org.modeshape.jcr.query.qom.JcrStaticOperand;
import org.modeshape.jcr.query.qom.JcrSubquery;
import org.modeshape.jcr.query.qom.JcrUpperCase;

/**
 * An specialization of the {@link SqlQueryParser} that uses a different language name that matches the JCR 2.0 specification.
 */
public class JcrSql2QueryParser extends SqlQueryParser {

    public static final String LANGUAGE = Query.JCR_SQL2;

    public JcrSql2QueryParser() {
        super();
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

    @Override
    protected JcrNamedSelector parseNamedSelector( TokenStream tokens,
                                                   TypeSystem typeSystem ) {
        return new JcrNamedSelector(super.parseNamedSelector(tokens, typeSystem));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#query(org.modeshape.graph.query.model.Source,
     *      org.modeshape.graph.query.model.Constraint, java.util.List, java.util.List, org.modeshape.graph.query.model.Limit,
     *      boolean)
     */
    @Override
    protected JcrSelectQuery query( Source source,
                                    Constraint constraint,
                                    List<? extends Ordering> orderings,
                                    List<? extends Column> columns,
                                    Limit limit,
                                    boolean distinct ) {
        return new JcrSelectQuery((JcrSource)source, (JcrConstraint)constraint, orderings(orderings), columns(columns),
                                  (JcrLimit)limit, distinct);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#setQuery(org.modeshape.graph.query.model.QueryCommand,
     *      org.modeshape.graph.query.model.SetQuery.Operation, org.modeshape.graph.query.model.QueryCommand, boolean)
     */
    @Override
    protected JcrSetQuery setQuery( QueryCommand leftQuery,
                                    Operation operation,
                                    QueryCommand rightQuery,
                                    boolean all ) {
        return new JcrSetQuery((JcrQueryCommand)leftQuery, operation, (JcrQueryCommand)rightQuery, all);
    }

    protected List<JcrColumn> columns( List<? extends Column> columns ) {
        List<JcrColumn> jcrColumns = new ArrayList<JcrColumn>();
        for (Column column : columns) {
            jcrColumns.add((JcrColumn)column);
        }
        return jcrColumns;
    }

    protected List<JcrOrdering> orderings( List<? extends Ordering> orderings ) {
        if (orderings == null) return null;
        List<JcrOrdering> jcrOrderings = new ArrayList<JcrOrdering>();
        for (Ordering ordering : orderings) {
            jcrOrderings.add((JcrOrdering)ordering);
        }
        return jcrOrderings;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#parseSetQuery(org.modeshape.common.text.TokenStream,
     *      org.modeshape.graph.query.model.QueryCommand, org.modeshape.graph.query.model.TypeSystem)
     */
    @Override
    protected SetQuery parseSetQuery( TokenStream tokens,
                                      QueryCommand leftHandSide,
                                      TypeSystem typeSystem ) {
        SetQuery query = super.parseSetQuery(tokens, leftHandSide, typeSystem);
        JcrQueryCommand left = (JcrQueryCommand)query.left();
        JcrQueryCommand right = (JcrQueryCommand)query.right();
        List<JcrOrdering> orderings = orderings(query.orderings());
        JcrLimit limit = (JcrLimit)query.limits();
        return new JcrSetQuery(left, query.operation(), right, query.isAll(), orderings, limit);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#length(org.modeshape.graph.query.model.PropertyValue)
     */
    @Override
    protected Length length( PropertyValue propertyValue ) {
        return new JcrLength((JcrPropertyValue)propertyValue);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#lowerCase(org.modeshape.graph.query.model.DynamicOperand)
     */
    @Override
    protected JcrLowerCase lowerCase( DynamicOperand operand ) {
        return new JcrLowerCase((JcrDynamicOperand)operand);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#upperCase(org.modeshape.graph.query.model.DynamicOperand)
     */
    @Override
    protected JcrUpperCase upperCase( DynamicOperand operand ) {
        return new JcrUpperCase((JcrDynamicOperand)operand);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#nodeName(org.modeshape.graph.query.model.SelectorName)
     */
    @Override
    protected JcrNodeName nodeName( SelectorName selector ) {
        return new JcrNodeName(selector);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#nodeLocalName(org.modeshape.graph.query.model.SelectorName)
     */
    @Override
    protected JcrNodeLocalName nodeLocalName( SelectorName selector ) {
        return new JcrNodeLocalName(selector);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#nodeDepth(org.modeshape.graph.query.model.SelectorName)
     */
    @Override
    protected JcrNodeDepth nodeDepth( SelectorName selector ) {
        return new JcrNodeDepth(selector);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#nodePath(org.modeshape.graph.query.model.SelectorName)
     */
    @Override
    protected JcrNodePath nodePath( SelectorName selector ) {
        return new JcrNodePath(selector);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#equiJoinCondition(org.modeshape.graph.query.model.SelectorName,
     *      java.lang.String, org.modeshape.graph.query.model.SelectorName, java.lang.String)
     */
    @Override
    protected JcrEquiJoinCondition equiJoinCondition( SelectorName selector1,
                                                      String property1,
                                                      SelectorName selector2,
                                                      String property2 ) {
        return new JcrEquiJoinCondition(selector1, property1, selector2, property2);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#descendantNodeJoinCondition(org.modeshape.graph.query.model.SelectorName,
     *      org.modeshape.graph.query.model.SelectorName)
     */
    @Override
    protected JcrDescendantNodeJoinCondition descendantNodeJoinCondition( SelectorName ancestor,
                                                                          SelectorName descendant ) {
        return new JcrDescendantNodeJoinCondition(ancestor, descendant);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#childNodeJoinCondition(org.modeshape.graph.query.model.SelectorName,
     *      org.modeshape.graph.query.model.SelectorName)
     */
    @Override
    protected JcrChildNodeJoinCondition childNodeJoinCondition( SelectorName parent,
                                                                SelectorName child ) {
        return new JcrChildNodeJoinCondition(parent, child);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#sameNodeJoinCondition(org.modeshape.graph.query.model.SelectorName,
     *      org.modeshape.graph.query.model.SelectorName)
     */
    @Override
    protected JcrSameNodeJoinCondition sameNodeJoinCondition( SelectorName selector1,
                                                              SelectorName selector2 ) {
        return new JcrSameNodeJoinCondition(selector1, selector2);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#sameNodeJoinCondition(org.modeshape.graph.query.model.SelectorName,
     *      org.modeshape.graph.query.model.SelectorName, java.lang.String)
     */
    @Override
    protected JcrSameNodeJoinCondition sameNodeJoinCondition( SelectorName selector1,
                                                              SelectorName selector2,
                                                              String path ) {
        return new JcrSameNodeJoinCondition(selector1, selector2, path);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#limit(int, int)
     */
    @Override
    protected JcrLimit limit( int rowCount,
                              int offset ) {
        return new JcrLimit(rowCount, offset);
    }

    @Override
    protected JcrColumn column( SelectorName selectorName,
                                String propertyName,
                                String columnName ) {
        return new JcrColumn(selectorName, propertyName, columnName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#join(org.modeshape.graph.query.model.Source,
     *      org.modeshape.graph.query.model.JoinType, org.modeshape.graph.query.model.Source,
     *      org.modeshape.graph.query.model.JoinCondition)
     */
    @Override
    protected JcrJoin join( Source left,
                            JoinType joinType,
                            Source right,
                            JoinCondition joinCondition ) {
        return new JcrJoin((JcrSource)left, joinType, (JcrSource)right, (JcrJoinCondition)joinCondition);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#not(org.modeshape.graph.query.model.Constraint)
     */
    @Override
    protected JcrNot not( Constraint constraint ) {
        return new JcrNot((JcrConstraint)constraint);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#and(org.modeshape.graph.query.model.Constraint,
     *      org.modeshape.graph.query.model.Constraint)
     */
    @Override
    protected JcrAnd and( Constraint constraint1,
                          Constraint constraint2 ) {
        return new JcrAnd((JcrConstraint)constraint1, (JcrConstraint)constraint2);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#or(org.modeshape.graph.query.model.Constraint,
     *      org.modeshape.graph.query.model.Constraint)
     */
    @Override
    protected JcrOr or( Constraint constraint1,
                        Constraint constraint2 ) {
        return new JcrOr((JcrConstraint)constraint1, (JcrConstraint)constraint2);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#fullTextSearch(org.modeshape.graph.query.model.SelectorName,
     *      java.lang.String, java.lang.String, org.modeshape.graph.query.model.FullTextSearch.Term)
     */
    @Override
    protected JcrFullTextSearch fullTextSearch( SelectorName name,
                                                String propertyName,
                                                String expression,
                                                Term term ) {
        return new JcrFullTextSearch(name, propertyName, expression, term);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#sameNode(org.modeshape.graph.query.model.SelectorName,
     *      java.lang.String)
     */
    @Override
    protected JcrSameNode sameNode( SelectorName name,
                                    String path ) {
        return new JcrSameNode(name, path);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#childNode(org.modeshape.graph.query.model.SelectorName,
     *      java.lang.String)
     */
    @Override
    protected JcrChildNode childNode( SelectorName name,
                                      String path ) {
        return new JcrChildNode(name, path);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#descendantNode(org.modeshape.graph.query.model.SelectorName,
     *      java.lang.String)
     */
    @Override
    protected JcrDescendantNode descendantNode( SelectorName name,
                                                String path ) {
        return new JcrDescendantNode(name, path);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#comparison(org.modeshape.graph.query.model.DynamicOperand,
     *      org.modeshape.graph.query.model.Operator, org.modeshape.graph.query.model.StaticOperand)
     */
    @Override
    protected JcrComparison comparison( DynamicOperand left,
                                        Operator operator,
                                        StaticOperand right ) {
        return new JcrComparison((JcrDynamicOperand)left, operator, (JcrStaticOperand)right);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#ordering(org.modeshape.graph.query.model.DynamicOperand,
     *      org.modeshape.graph.query.model.Order)
     */
    @Override
    protected JcrOrdering ordering( DynamicOperand operand,
                                    Order order ) {
        return new JcrOrdering((JcrDynamicOperand)operand, order);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#propertyExistence(org.modeshape.graph.query.model.SelectorName,
     *      java.lang.String)
     */
    @Override
    protected JcrPropertyExistence propertyExistence( SelectorName selector,
                                                      String propertyName ) {
        return new JcrPropertyExistence(selector, propertyName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#fullTextSearchScore(org.modeshape.graph.query.model.SelectorName)
     */
    @Override
    protected JcrFullTextSearchScore fullTextSearchScore( SelectorName selector ) {
        return new JcrFullTextSearchScore(selector);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#between(org.modeshape.graph.query.model.DynamicOperand,
     *      org.modeshape.graph.query.model.StaticOperand, org.modeshape.graph.query.model.StaticOperand, boolean, boolean)
     */
    @Override
    protected Between between( DynamicOperand operand,
                               StaticOperand lowerBound,
                               StaticOperand upperBound,
                               boolean lowerInclusive,
                               boolean upperInclusive ) {
        return new JcrBetween((JcrDynamicOperand)operand, (JcrStaticOperand)lowerBound, (JcrStaticOperand)upperBound,
                              lowerInclusive, upperInclusive);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#setCriteria(org.modeshape.graph.query.model.DynamicOperand,
     *      java.util.Collection)
     */
    @SuppressWarnings( "unchecked" )
    @Override
    protected SetCriteria setCriteria( DynamicOperand operand,
                                       Collection<? extends StaticOperand> values ) {
        return new JcrSetCriteria((JcrDynamicOperand)operand, (Collection<? extends JcrStaticOperand>)values);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#arithmeticOperand(org.modeshape.graph.query.model.DynamicOperand,
     *      org.modeshape.graph.query.model.ArithmeticOperator, org.modeshape.graph.query.model.DynamicOperand)
     */
    @Override
    protected JcrArithmeticOperand arithmeticOperand( DynamicOperand leftOperand,
                                                      ArithmeticOperator operator,
                                                      DynamicOperand rightOperand ) {
        return new JcrArithmeticOperand((JcrDynamicOperand)leftOperand, operator, (JcrDynamicOperand)rightOperand);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#propertyValue(org.modeshape.graph.query.model.SelectorName,
     *      java.lang.String)
     */
    @Override
    protected JcrPropertyValue propertyValue( SelectorName selector,
                                              String propertyName ) {
        return new JcrPropertyValue(selector, propertyName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#referenceValue(org.modeshape.graph.query.model.SelectorName)
     */
    @Override
    protected JcrReferenceValue referenceValue( SelectorName selector ) {
        return new JcrReferenceValue(selector, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#referenceValue(org.modeshape.graph.query.model.SelectorName,
     *      java.lang.String)
     */
    @Override
    protected JcrReferenceValue referenceValue( SelectorName selector,
                                                String propertyName ) {
        return new JcrReferenceValue(selector, propertyName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#bindVariableName(java.lang.String)
     */
    @Override
    protected JcrBindVariableName bindVariableName( String variableName ) {
        return new JcrBindVariableName(variableName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#subquery(org.modeshape.graph.query.model.QueryCommand)
     */
    @Override
    protected Subquery subquery( QueryCommand queryCommand ) {
        return new JcrSubquery((JcrQueryCommand)queryCommand);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.parse.SqlQueryParser#literal(TypeSystem, Object)
     */
    @Override
    protected JcrLiteral literal( TypeSystem typeSystem,
                                  Object value ) throws org.modeshape.graph.property.ValueFormatException {
        ValueFactory factory = ((JcrTypeSystem)typeSystem).getValueFactory();
        Value jcrValue = null;
        if (value instanceof String) {
            jcrValue = factory.createValue((String)value);
        } else if (value instanceof Boolean) {
            jcrValue = factory.createValue(((Boolean)value).booleanValue());
        } else if (value instanceof Binary) {
            jcrValue = factory.createValue((Binary)value);
        } else if (value instanceof DateTime) {
            jcrValue = factory.createValue(((DateTime)value).toCalendar());
        } else if (value instanceof Calendar) {
            jcrValue = factory.createValue((Calendar)value);
        } else if (value instanceof BigDecimal) {
            jcrValue = factory.createValue((BigDecimal)value);
        } else if (value instanceof Double) {
            jcrValue = factory.createValue((Double)value);
        } else if (value instanceof Long) {
            jcrValue = factory.createValue((Long)value);
        } else if (value instanceof InputStream) {
            try {
                Binary binary = factory.createBinary((InputStream)value);
                jcrValue = factory.createValue(binary);
            } catch (RepositoryException e) {
                throw new org.modeshape.graph.property.ValueFormatException(value,
                                                                            org.modeshape.graph.property.PropertyType.BINARY,
                                                                            e.getMessage());
            }
        } else if (value instanceof Node) {
            try {
                jcrValue = factory.createValue((Node)value);
            } catch (RepositoryException e) {
                throw new org.modeshape.graph.property.ValueFormatException(value,
                                                                            org.modeshape.graph.property.PropertyType.REFERENCE,
                                                                            e.getMessage());
            }
        } else {
            jcrValue = factory.createValue(value.toString());
        }
        return new JcrLiteral(jcrValue, value);
    }

}
