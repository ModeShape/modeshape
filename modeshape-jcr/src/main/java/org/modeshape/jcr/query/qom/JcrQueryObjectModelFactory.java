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
package org.modeshape.jcr.query.qom;

import java.util.ArrayList;
import java.util.List;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.query.Query;
import javax.jcr.query.qom.And;
import javax.jcr.query.qom.BindVariableValue;
import javax.jcr.query.qom.ChildNode;
import javax.jcr.query.qom.ChildNodeJoinCondition;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Comparison;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.DescendantNode;
import javax.jcr.query.qom.DescendantNodeJoinCondition;
import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.EquiJoinCondition;
import javax.jcr.query.qom.FullTextSearch;
import javax.jcr.query.qom.FullTextSearchScore;
import javax.jcr.query.qom.Join;
import javax.jcr.query.qom.JoinCondition;
import javax.jcr.query.qom.Length;
import javax.jcr.query.qom.Literal;
import javax.jcr.query.qom.LowerCase;
import javax.jcr.query.qom.NodeLocalName;
import javax.jcr.query.qom.NodeName;
import javax.jcr.query.qom.Not;
import javax.jcr.query.qom.Or;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.PropertyExistence;
import javax.jcr.query.qom.PropertyValue;
import javax.jcr.query.qom.QueryObjectModelFactory;
import javax.jcr.query.qom.SameNode;
import javax.jcr.query.qom.SameNodeJoinCondition;
import javax.jcr.query.qom.Source;
import javax.jcr.query.qom.StaticOperand;
import javax.jcr.query.qom.UpperCase;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.query.model.ArithmeticOperator;
import org.modeshape.graph.query.model.JoinType;
import org.modeshape.graph.query.model.Operator;
import org.modeshape.graph.query.model.Order;
import org.modeshape.graph.query.model.SelectorName;
import org.modeshape.graph.query.model.SetQuery.Operation;
import org.modeshape.graph.query.plan.PlanHints;
import org.modeshape.jcr.api.query.qom.ArithmeticOperand;
import org.modeshape.jcr.api.query.qom.Between;
import org.modeshape.jcr.api.query.qom.Limit;
import org.modeshape.jcr.api.query.qom.NodeDepth;
import org.modeshape.jcr.api.query.qom.NodePath;
import org.modeshape.jcr.api.query.qom.QueryCommand;
import org.modeshape.jcr.api.query.qom.QueryObjectModelConstants;
import org.modeshape.jcr.api.query.qom.SetCriteria;
import org.modeshape.jcr.api.query.qom.SetQuery;
import org.modeshape.jcr.api.query.qom.Subquery;
import org.modeshape.jcr.query.JcrQueryContext;

/**
 * An implementation of the JCR {@link QueryObjectModelFactory}. Note that this implementation constructs the query components but
 * does not validate any of the parameters or the resulting query definition. All validation is performed when the query is
 * {@link Query#execute() executed}.
 */
public class JcrQueryObjectModelFactory
    implements QueryObjectModelFactory, org.modeshape.jcr.api.query.qom.QueryObjectModelFactory {

    public static final String LANGUAGE = Query.JCR_JQOM;
    private final JcrQueryContext context;

    public JcrQueryObjectModelFactory( JcrQueryContext context ) {
        this.context = context;
    }

    protected SelectorName selectorName( String name ) {
        return new SelectorName(name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#createQuery(javax.jcr.query.qom.Source, javax.jcr.query.qom.Constraint,
     *      javax.jcr.query.qom.Ordering[], javax.jcr.query.qom.Column[])
     */
    @Override
    public JcrQueryObjectModel createQuery( Source source,
                                            Constraint constraint,
                                            Ordering[] orderings,
                                            Column[] columns ) {

        JcrSelectQuery query = select(source, constraint, orderings, columns, null, false);
        String statement = query.toString();
        // Set up the hints ...
        PlanHints hints = new PlanHints();
        hints.showPlan = true;
        // We want to allow use of residual properties (not in the schemata) for criteria ...
        hints.validateColumnExistance = true;
        return new JcrQueryObjectModel(context, statement, LANGUAGE, query, hints, null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.QueryObjectModelFactory#select(javax.jcr.query.qom.Source,
     *      javax.jcr.query.qom.Constraint, javax.jcr.query.qom.Ordering[], javax.jcr.query.qom.Column[],
     *      org.modeshape.jcr.api.query.qom.Limit, boolean)
     */
    @Override
    public JcrSelectQuery select( Source source,
                                  Constraint constraint,
                                  Ordering[] orderings,
                                  Column[] columns,
                                  Limit limit,
                                  boolean distinct ) {
        JcrSource jcrSource = CheckArg.getInstanceOf(source, JcrSource.class, "source");
        JcrConstraint jcrConstraint = null;
        if (constraint != null) {
            jcrConstraint = CheckArg.getInstanceOf(constraint, JcrConstraint.class, "constraint");
        }
        List<JcrColumn> jcrColumns = null;
        if (columns != null) {
            jcrColumns = new ArrayList<JcrColumn>();
            for (int i = 0; i != columns.length; ++i) {
                jcrColumns.add(CheckArg.getInstanceOf(columns[i], JcrColumn.class, "column[" + i + "]"));
            }
        }
        List<JcrOrdering> jcrOrderings = null;
        if (orderings != null) {
            jcrOrderings = new ArrayList<JcrOrdering>();
            for (int i = 0; i != orderings.length; ++i) {
                jcrOrderings.add(CheckArg.getInstanceOf(orderings[i], JcrOrdering.class, "orderings[" + i + "]"));
            }
        }
        JcrLimit jcrLimit = limit == null ? JcrLimit.NONE : new JcrLimit(limit.getRowLimit(), limit.getOffset());
        return new JcrSelectQuery(jcrSource, jcrConstraint, jcrOrderings, jcrColumns, jcrLimit, distinct);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.QueryObjectModelFactory#union(QueryCommand, QueryCommand, Ordering[], Limit, boolean)
     */
    @Override
    public SetQuery union( QueryCommand left,
                           QueryCommand right,
                           Ordering[] orderings,
                           Limit limit,
                           boolean all ) {
        return setQuery(left, Operation.UNION, right, orderings, limit, all);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.QueryObjectModelFactory#intersect(org.modeshape.jcr.api.query.qom.QueryCommand,
     *      org.modeshape.jcr.api.query.qom.QueryCommand, javax.jcr.query.qom.Ordering[], org.modeshape.jcr.api.query.qom.Limit,
     *      boolean)
     */
    @Override
    public SetQuery intersect( QueryCommand left,
                               QueryCommand right,
                               Ordering[] orderings,
                               Limit limit,
                               boolean all ) {
        return setQuery(left, Operation.INTERSECT, right, orderings, limit, all);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.QueryObjectModelFactory#except(org.modeshape.jcr.api.query.qom.QueryCommand,
     *      org.modeshape.jcr.api.query.qom.QueryCommand, javax.jcr.query.qom.Ordering[], org.modeshape.jcr.api.query.qom.Limit,
     *      boolean)
     */
    @Override
    public SetQuery except( QueryCommand left,
                            QueryCommand right,
                            Ordering[] orderings,
                            Limit limit,
                            boolean all ) {
        return setQuery(left, Operation.EXCEPT, right, orderings, limit, all);
    }

    public JcrSetQuery setQuery( QueryCommand left,
                                 Operation operation,
                                 QueryCommand right,
                                 Ordering[] orderings,
                                 Limit limit,
                                 boolean all ) {
        JcrQueryCommand jcrLeft = CheckArg.getInstanceOf(left, JcrQueryCommand.class, "left");
        JcrQueryCommand jcrRight = CheckArg.getInstanceOf(left, JcrQueryCommand.class, "left");
        List<JcrOrdering> jcrOrderings = new ArrayList<JcrOrdering>();
        for (int i = 0; i != orderings.length; ++i) {
            jcrOrderings.add(CheckArg.getInstanceOf(orderings[i], JcrOrdering.class, "orderings[" + i + "]"));
        }
        JcrLimit jcrLimit = limit == null ? JcrLimit.NONE : new JcrLimit(limit.getRowLimit(), limit.getOffset());
        return new JcrSetQuery(jcrLeft, operation, jcrRight, all, jcrOrderings, jcrLimit);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#selector(java.lang.String, java.lang.String)
     */
    @Override
    public JcrNamedSelector selector( String nodeTypeName,
                                      String selectorName ) {
        CheckArg.isNotNull(nodeTypeName, "nodeTypeName");
        CheckArg.isNotNull(selectorName, "selectorName");
        return new JcrNamedSelector(selectorName(nodeTypeName), selectorName(selectorName));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#column(java.lang.String, java.lang.String, java.lang.String)
     */
    @Override
    public JcrColumn column( String selectorName,
                             String propertyName,
                             String columnName ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        if (propertyName == null) {
            return new JcrColumn(selectorName(selectorName));
        }
        CheckArg.isNotNull(columnName, "columnName");
        return new JcrColumn(selectorName(selectorName), selectorName, columnName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#ascending(javax.jcr.query.qom.DynamicOperand)
     */
    @Override
    public JcrOrdering ascending( DynamicOperand operand ) {
        JcrDynamicOperand jcrOperand = CheckArg.getInstanceOf(operand, JcrDynamicOperand.class, "operand");
        return new JcrOrdering(jcrOperand, Order.ASCENDING);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#descending(javax.jcr.query.qom.DynamicOperand)
     */
    @Override
    public JcrOrdering descending( DynamicOperand operand ) {
        JcrDynamicOperand jcrOperand = CheckArg.getInstanceOf(operand, JcrDynamicOperand.class, "operand");
        return new JcrOrdering(jcrOperand, Order.DESCENDING);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#and(javax.jcr.query.qom.Constraint, javax.jcr.query.qom.Constraint)
     */
    @Override
    public And and( Constraint constraint1,
                    Constraint constraint2 ) {
        JcrConstraint jcrConstraint1 = CheckArg.getInstanceOf(constraint1, JcrConstraint.class, "constraint1");
        JcrConstraint jcrConstraint2 = CheckArg.getInstanceOf(constraint2, JcrConstraint.class, "constraint2");
        return new JcrAnd(jcrConstraint1, jcrConstraint2);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#bindVariable(java.lang.String)
     */
    @Override
    public BindVariableValue bindVariable( String bindVariableName ) {
        CheckArg.isNotNull(bindVariableName, "bindVariableName");
        return new JcrBindVariableName(bindVariableName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#childNode(java.lang.String, java.lang.String)
     */
    @Override
    public ChildNode childNode( String selectorName,
                                String path ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotNull(path, "path");
        return new JcrChildNode(selectorName(selectorName), path);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#childNodeJoinCondition(java.lang.String, java.lang.String)
     */
    @Override
    public ChildNodeJoinCondition childNodeJoinCondition( String childSelectorName,
                                                          String parentSelectorName ) {
        CheckArg.isNotNull(childSelectorName, "childSelectorName");
        CheckArg.isNotNull(parentSelectorName, "parentSelectorName");
        return new JcrChildNodeJoinCondition(selectorName(parentSelectorName), selectorName(childSelectorName));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#comparison(javax.jcr.query.qom.DynamicOperand, java.lang.String,
     *      javax.jcr.query.qom.StaticOperand)
     */
    @Override
    public Comparison comparison( DynamicOperand operand1,
                                  String operator,
                                  StaticOperand operand2 ) {
        JcrDynamicOperand jcrOperand1 = CheckArg.getInstanceOf(operand1, JcrDynamicOperand.class, "operand1");
        CheckArg.isNotEmpty(operator, "operator");
        JcrStaticOperand jcrOperand2 = CheckArg.getInstanceOf(operand2, JcrStaticOperand.class, "operand2");
        operator = operator.trim();
        Operator op = null;
        if (QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO.equals(operator)) op = Operator.EQUAL_TO;
        else if (QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN.equals(operator)) op = Operator.GREATER_THAN;
        else if (QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO.equals(operator)) op = Operator.GREATER_THAN_OR_EQUAL_TO;
        else if (QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN.equals(operator)) op = Operator.LESS_THAN;
        else if (QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO.equals(operator)) op = Operator.LESS_THAN_OR_EQUAL_TO;
        else if (QueryObjectModelConstants.JCR_OPERATOR_LIKE.equals(operator)) op = Operator.LIKE;
        else if (QueryObjectModelConstants.JCR_OPERATOR_NOT_EQUAL_TO.equals(operator)) op = Operator.NOT_EQUAL_TO;
        if (op == null) {
            throw new IllegalArgumentException("Unknown or unsupported comparison operator: " + operator);
        }
        return new JcrComparison(jcrOperand1, op, jcrOperand2);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#descendantNode(java.lang.String, java.lang.String)
     */
    @Override
    public DescendantNode descendantNode( String selectorName,
                                          String path ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotNull(path, "path");
        return new JcrDescendantNode(selectorName(selectorName), path);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#descendantNodeJoinCondition(java.lang.String, java.lang.String)
     */
    @Override
    public DescendantNodeJoinCondition descendantNodeJoinCondition( String descendantSelectorName,
                                                                    String ancestorSelectorName ) {
        CheckArg.isNotNull(descendantSelectorName, "descendantSelectorName");
        CheckArg.isNotNull(ancestorSelectorName, "ancestorSelectorName");
        return new JcrDescendantNodeJoinCondition(selectorName(ancestorSelectorName), selectorName(descendantSelectorName));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#equiJoinCondition(java.lang.String, java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    @Override
    public EquiJoinCondition equiJoinCondition( String selector1Name,
                                                String property1Name,
                                                String selector2Name,
                                                String property2Name ) {
        CheckArg.isNotNull(selector1Name, "selector1Name");
        CheckArg.isNotNull(property1Name, "property1Name");
        CheckArg.isNotNull(selector2Name, "selector2Name");
        CheckArg.isNotNull(selector2Name, "selector2Name");
        return new JcrEquiJoinCondition(selectorName(selector1Name), property1Name, selectorName(selector2Name), selector2Name);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#fullTextSearch(java.lang.String, java.lang.String,
     *      javax.jcr.query.qom.StaticOperand)
     */
    @Override
    public FullTextSearch fullTextSearch( String selectorName,
                                          String propertyName,
                                          StaticOperand fullTextSearchExpression ) throws RepositoryException {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotNull(fullTextSearchExpression, "fullTextSearchExpression");
        return new JcrFullTextSearch(selectorName(selectorName), propertyName, fullTextSearchExpression);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#fullTextSearchScore(java.lang.String)
     */
    @Override
    public FullTextSearchScore fullTextSearchScore( String selectorName ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        return new JcrFullTextSearchScore(selectorName(selectorName));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#join(javax.jcr.query.qom.Source, javax.jcr.query.qom.Source,
     *      java.lang.String, javax.jcr.query.qom.JoinCondition)
     */
    @Override
    public Join join( Source left,
                      Source right,
                      String joinType,
                      JoinCondition joinCondition ) {
        JcrSource leftSource = CheckArg.getInstanceOf(left, JcrSource.class, "left");
        JcrSource rightSource = CheckArg.getInstanceOf(right, JcrSource.class, "right");
        JcrJoinCondition jcrJoinCondition = CheckArg.getInstanceOf(joinCondition, JcrJoinCondition.class, "joinCondition");
        CheckArg.isNotEmpty(joinType, "joinType");
        joinType = joinType.trim();
        JoinType type = null;
        if (QueryObjectModelConstants.JCR_JOIN_TYPE_CROSS.equals(joinType)) type = JoinType.CROSS;
        else if (QueryObjectModelConstants.JCR_JOIN_TYPE_INNER.equals(joinType)) type = JoinType.INNER;
        else if (QueryObjectModelConstants.JCR_JOIN_TYPE_FULL_OUTER.equals(joinType)) type = JoinType.FULL_OUTER;
        else if (QueryObjectModelConstants.JCR_JOIN_TYPE_LEFT_OUTER.equals(joinType)) type = JoinType.LEFT_OUTER;
        else if (QueryObjectModelConstants.JCR_JOIN_TYPE_RIGHT_OUTER.equals(joinType)) type = JoinType.RIGHT_OUTER;
        if (type == null) {
            throw new IllegalArgumentException("Unknown or unsupported join type: " + joinType);
        }
        return new JcrJoin(leftSource, type, rightSource, jcrJoinCondition);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#length(javax.jcr.query.qom.PropertyValue)
     */
    @Override
    public Length length( PropertyValue propertyValue ) {
        JcrPropertyValue jcrPropValue = CheckArg.getInstanceOf(propertyValue, JcrPropertyValue.class, "propertyValue");
        return new JcrLength(jcrPropValue);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#literal(javax.jcr.Value)
     */
    @Override
    public Literal literal( Value literalValue ) throws RepositoryException {
        CheckArg.isNotNull(literalValue, "literalValue");
        return new JcrLiteral(literalValue);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#lowerCase(javax.jcr.query.qom.DynamicOperand)
     */
    @Override
    public LowerCase lowerCase( DynamicOperand operand ) {
        JcrDynamicOperand jcrOperand = CheckArg.getInstanceOf(operand, JcrDynamicOperand.class, "operand");
        return new JcrLowerCase(jcrOperand);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#nodeLocalName(java.lang.String)
     */
    @Override
    public NodeLocalName nodeLocalName( String selectorName ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        return new JcrNodeLocalName(selectorName(selectorName));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#nodeName(java.lang.String)
     */
    @Override
    public NodeName nodeName( String selectorName ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        return new JcrNodeName(selectorName(selectorName));
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#not(javax.jcr.query.qom.Constraint)
     */
    @Override
    public Not not( Constraint constraint ) {
        JcrConstraint jcrConstraint = CheckArg.getInstanceOf(constraint, JcrConstraint.class, "constraint");
        return new JcrNot(jcrConstraint);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#or(javax.jcr.query.qom.Constraint, javax.jcr.query.qom.Constraint)
     */
    @Override
    public Or or( Constraint constraint1,
                  Constraint constraint2 ) {
        JcrConstraint jcrConstraint1 = CheckArg.getInstanceOf(constraint1, JcrConstraint.class, "constraint1");
        JcrConstraint jcrConstraint2 = CheckArg.getInstanceOf(constraint2, JcrConstraint.class, "constraint2");
        return new JcrOr(jcrConstraint1, jcrConstraint2);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#propertyExistence(java.lang.String, java.lang.String)
     */
    @Override
    public PropertyExistence propertyExistence( String selectorName,
                                                String propertyName ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotNull(propertyName, "propertyName");
        return new JcrPropertyExistence(selectorName(selectorName), propertyName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#propertyValue(java.lang.String, java.lang.String)
     */
    @Override
    public PropertyValue propertyValue( String selectorName,
                                        String propertyName ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotNull(propertyName, "propertyName");
        return new JcrPropertyValue(selectorName(selectorName), propertyName);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#sameNode(java.lang.String, java.lang.String)
     */
    @Override
    public SameNode sameNode( String selectorName,
                              String path ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotNull(path, "path");
        return new JcrSameNode(selectorName(selectorName), path);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#sameNodeJoinCondition(java.lang.String, java.lang.String,
     *      java.lang.String)
     */
    @Override
    public SameNodeJoinCondition sameNodeJoinCondition( String selector1Name,
                                                        String selector2Name,
                                                        String selector2Path ) {
        CheckArg.isNotNull(selector1Name, "selector1Name");
        CheckArg.isNotNull(selector1Name, "selector1Name");
        return new JcrSameNodeJoinCondition(selectorName(selector1Name), selectorName(selector1Name), selector2Path);
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.query.qom.QueryObjectModelFactory#upperCase(javax.jcr.query.qom.DynamicOperand)
     */
    @Override
    public UpperCase upperCase( DynamicOperand operand ) {
        JcrDynamicOperand jcrOperand = CheckArg.getInstanceOf(operand, JcrDynamicOperand.class, "operand");
        return new JcrUpperCase(jcrOperand);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.QueryObjectModelFactory#nodePath(java.lang.String)
     */
    @Override
    public NodePath nodePath( String selectorName ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        return new JcrNodePath(selectorName(selectorName));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.QueryObjectModelFactory#nodeDepth(java.lang.String)
     */
    @Override
    public NodeDepth nodeDepth( String selectorName ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        return new JcrNodeDepth(selectorName(selectorName));
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.QueryObjectModelFactory#limit(int, int)
     */
    @Override
    public Limit limit( int rowLimit,
                        int offset ) {
        CheckArg.isPositive(rowLimit, "rowLimit");
        CheckArg.isNonNegative(offset, "offset");
        return new JcrLimit(rowLimit, offset);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.QueryObjectModelFactory#between(javax.jcr.query.qom.DynamicOperand,
     *      javax.jcr.query.qom.StaticOperand, javax.jcr.query.qom.StaticOperand, boolean, boolean)
     */
    @Override
    public Between between( DynamicOperand operand,
                            StaticOperand lowerBound,
                            StaticOperand upperBound,
                            boolean includeLowerBound,
                            boolean includeUpperBound ) {
        JcrDynamicOperand jcrOperand = CheckArg.getInstanceOf(operand, JcrDynamicOperand.class, "operand");
        JcrStaticOperand lower = CheckArg.getInstanceOf(lowerBound, JcrStaticOperand.class, "lowerBound");
        JcrStaticOperand upper = CheckArg.getInstanceOf(upperBound, JcrStaticOperand.class, "upperBound");
        return new JcrBetween(jcrOperand, lower, upper, includeLowerBound, includeUpperBound);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.QueryObjectModelFactory#in(javax.jcr.query.qom.DynamicOperand,
     *      javax.jcr.query.qom.StaticOperand[])
     */
    @Override
    public SetCriteria in( DynamicOperand operand,
                           StaticOperand... values ) {
        JcrDynamicOperand jcrOperand = CheckArg.getInstanceOf(operand, JcrDynamicOperand.class, "operand");
        List<JcrStaticOperand> jcrValues = new ArrayList<JcrStaticOperand>();
        for (StaticOperand value : values) {
            JcrStaticOperand jcrValue = CheckArg.getInstanceOf(value, JcrStaticOperand.class, "values");
            jcrValues.add(jcrValue);
        }
        return new JcrSetCriteria(jcrOperand, jcrValues);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.QueryObjectModelFactory#subquery(org.modeshape.jcr.api.query.qom.QueryCommand)
     */
    @Override
    public Subquery subquery( QueryCommand subqueryCommand ) {
        JcrQueryCommand jcrCommand = CheckArg.getInstanceOf(subqueryCommand, JcrQueryCommand.class, "subqueryCommand");
        return new JcrSubquery(jcrCommand);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.QueryObjectModelFactory#add(javax.jcr.query.qom.DynamicOperand,
     *      javax.jcr.query.qom.DynamicOperand)
     */
    @Override
    public ArithmeticOperand add( DynamicOperand left,
                                  DynamicOperand right ) {
        return arithmeticOperand(left, ArithmeticOperator.ADD, right);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.QueryObjectModelFactory#subtract(javax.jcr.query.qom.DynamicOperand,
     *      javax.jcr.query.qom.DynamicOperand)
     */
    @Override
    public ArithmeticOperand subtract( DynamicOperand left,
                                       DynamicOperand right ) {
        return arithmeticOperand(left, ArithmeticOperator.SUBTRACT, right);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.QueryObjectModelFactory#multiply(javax.jcr.query.qom.DynamicOperand,
     *      javax.jcr.query.qom.DynamicOperand)
     */
    @Override
    public ArithmeticOperand multiply( DynamicOperand left,
                                       DynamicOperand right ) {
        return arithmeticOperand(left, ArithmeticOperator.MULTIPLY, right);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.QueryObjectModelFactory#divide(javax.jcr.query.qom.DynamicOperand,
     *      javax.jcr.query.qom.DynamicOperand)
     */
    @Override
    public ArithmeticOperand divide( DynamicOperand left,
                                     DynamicOperand right ) {
        return arithmeticOperand(left, ArithmeticOperator.DIVIDE, right);
    }

    public ArithmeticOperand arithmeticOperand( DynamicOperand left,
                                                ArithmeticOperator operator,
                                                DynamicOperand right ) {
        JcrDynamicOperand leftOperand = CheckArg.getInstanceOf(left, JcrDynamicOperand.class, "left");
        JcrDynamicOperand rightOperand = CheckArg.getInstanceOf(left, JcrDynamicOperand.class, "left");
        return new JcrArithmeticOperand(leftOperand, operator, rightOperand);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.QueryObjectModelFactory#referenceValue(java.lang.String)
     */
    @Override
    public JcrReferenceValue referenceValue( String selectorName ) {
        return new JcrReferenceValue(selectorName(selectorName), null);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.jcr.api.query.qom.QueryObjectModelFactory#referenceValue(java.lang.String, java.lang.String)
     */
    @Override
    public JcrReferenceValue referenceValue( String selectorName,
                                             String propertyName ) {
        return new JcrReferenceValue(selectorName(selectorName), propertyName);
    }

}
