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
package org.modeshape.jcr.query.model;

import java.util.ArrayList;
import java.util.List;
import javax.jcr.RepositoryException;
import javax.jcr.query.qom.BindVariableValue;
import org.modeshape.common.util.CheckArg;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.api.query.qom.QueryObjectModelConstants;
import org.modeshape.jcr.query.JcrQueryContext;
import org.modeshape.jcr.query.plan.PlanHints;

/**
 * An implementation of the JCR {@link QueryObjectModelFactory}. Note that this implementation constructs the query components but
 * does not validate any of the parameters or the resulting query definition. All validation is performed when the query is
 * {@link javax.jcr.query.Query#execute() executed}.
 */
public class QueryObjectModelFactory implements org.modeshape.jcr.api.query.qom.QueryObjectModelFactory {

    public static final String LANGUAGE = javax.jcr.query.Query.JCR_JQOM;
    private final JcrQueryContext context;

    public QueryObjectModelFactory( JcrQueryContext context ) {
        this.context = context;
    }

    protected SelectorName selectorName( String name ) {
        return new SelectorName(name);
    }

    @Override
    public QueryObjectModel createQuery( javax.jcr.query.qom.Source source,
                                         javax.jcr.query.qom.Constraint constraint,
                                         javax.jcr.query.qom.Ordering[] orderings,
                                         javax.jcr.query.qom.Column[] columns ) {

        SelectQuery query = select(source, constraint, orderings, columns, null, false);
        // Produce the statement, but use our ExecutionContext (primarily for namespaces and binary value usage) ...
        String statement = Visitors.readable(query, context.getExecutionContext());
        // Set up the hints ...
        PlanHints hints = new PlanHints();
        hints.showPlan = true;
        hints.hasFullTextSearch = true; // always include the score
        hints.qualifyExpandedColumnNames = true; // always qualify expanded names with the selector name in JCR-SQL2
        // We want to allow use of residual properties (not in the schemata) for criteria ...
        hints.validateColumnExistance = true;
        return new QueryObjectModel(context, statement, LANGUAGE, query, hints, null);
    }

    @Override
    public SetQueryObjectModel createQuery( org.modeshape.jcr.api.query.qom.SetQuery command ) {
        SetQuery setQuery = CheckArg.getInstanceOf(command, SetQuery.class, "command");
        String statement = setQuery.toString();
        // Set up the hints ...
        PlanHints hints = new PlanHints();
        hints.showPlan = true;
        hints.hasFullTextSearch = true; // always include the score
        hints.qualifyExpandedColumnNames = true; // always qualify expanded names with the selector name in JCR-SQL2
        // We want to allow use of residual properties (not in the schemata) for criteria ...
        hints.validateColumnExistance = true;
        return new SetQueryObjectModel(context, statement, LANGUAGE, setQuery, hints, null);
    }

    @Override
    public QueryObjectModel createQuery( org.modeshape.jcr.api.query.qom.SelectQuery command ) {
        SelectQuery selectQuery = CheckArg.getInstanceOf(command, SelectQuery.class, "command");
        String statement = selectQuery.toString();
        // Set up the hints ...
        PlanHints hints = new PlanHints();
        hints.showPlan = true;
        hints.hasFullTextSearch = true; // always include the score
        hints.qualifyExpandedColumnNames = true; // always qualify expanded names with the selector name in JCR-SQL2
        // We want to allow use of residual properties (not in the schemata) for criteria ...
        hints.validateColumnExistance = true;
        return new QueryObjectModel(context, statement, LANGUAGE, selectQuery, hints, null);
    }

    @Override
    public SelectQuery select( javax.jcr.query.qom.Source source,
                               javax.jcr.query.qom.Constraint constraint,
                               javax.jcr.query.qom.Ordering[] orderings,
                               javax.jcr.query.qom.Column[] columns,
                               org.modeshape.jcr.api.query.qom.Limit limit,
                               boolean distinct ) {
        Source jcrSource = CheckArg.getInstanceOf(source, Source.class, "source");
        Constraint jcrConstraint = null;
        if (constraint != null) {
            jcrConstraint = CheckArg.getInstanceOf(constraint, Constraint.class, "constraint");
        }
        List<Column> jcrColumns = null;
        if (columns != null) {
            jcrColumns = new ArrayList<Column>();
            for (int i = 0; i != columns.length; ++i) {
                jcrColumns.add(CheckArg.getInstanceOf(columns[i], Column.class, "column[" + i + "]"));
            }
        }
        List<Ordering> jcrOrderings = null;
        if (orderings != null) {
            jcrOrderings = new ArrayList<Ordering>();
            for (int i = 0; i != orderings.length; ++i) {
                jcrOrderings.add(CheckArg.getInstanceOf(orderings[i], Ordering.class, "orderings[" + i + "]"));
            }
        }
        Limit jcrLimit = limit == null ? Limit.NONE : new Limit(limit.getRowLimit(), limit.getOffset());
        return new SelectQuery(jcrSource, jcrConstraint, jcrOrderings, jcrColumns, jcrLimit, distinct);
    }

    @Override
    public org.modeshape.jcr.api.query.qom.SetQuery union( org.modeshape.jcr.api.query.qom.QueryCommand left,
                                                           org.modeshape.jcr.api.query.qom.QueryCommand right,
                                                           javax.jcr.query.qom.Ordering[] orderings,
                                                           org.modeshape.jcr.api.query.qom.Limit limit,
                                                           boolean all ) {
        return setQuery(left, SetQuery.Operation.UNION, right, orderings, limit, all);
    }

    @Override
    public SetQuery intersect( org.modeshape.jcr.api.query.qom.QueryCommand left,
                               org.modeshape.jcr.api.query.qom.QueryCommand right,
                               javax.jcr.query.qom.Ordering[] orderings,
                               org.modeshape.jcr.api.query.qom.Limit limit,
                               boolean all ) {
        return setQuery(left, SetQuery.Operation.INTERSECT, right, orderings, limit, all);
    }

    @Override
    public SetQuery except( org.modeshape.jcr.api.query.qom.QueryCommand left,
                            org.modeshape.jcr.api.query.qom.QueryCommand right,
                            javax.jcr.query.qom.Ordering[] orderings,
                            org.modeshape.jcr.api.query.qom.Limit limit,
                            boolean all ) {
        return setQuery(left, SetQuery.Operation.EXCEPT, right, orderings, limit, all);
    }

    public SetQuery setQuery( org.modeshape.jcr.api.query.qom.QueryCommand left,
                              SetQuery.Operation operation,
                              org.modeshape.jcr.api.query.qom.QueryCommand right,
                              javax.jcr.query.qom.Ordering[] orderings,
                              org.modeshape.jcr.api.query.qom.Limit limit,
                              boolean all ) {
        QueryCommand jcrLeft = CheckArg.getInstanceOf(left, QueryCommand.class, "left");
        QueryCommand jcrRight = CheckArg.getInstanceOf(left, QueryCommand.class, "left");
        List<Ordering> jcrOrderings = new ArrayList<Ordering>();
        for (int i = 0; i != orderings.length; ++i) {
            jcrOrderings.add(CheckArg.getInstanceOf(orderings[i], Ordering.class, "orderings[" + i + "]"));
        }
        Limit jcrLimit = limit == null ? Limit.NONE : new Limit(limit.getRowLimit(), limit.getOffset());
        return new SetQuery(jcrLeft, operation, jcrRight, all, jcrOrderings, jcrLimit);
    }

    @Override
    public NamedSelector selector( String nodeTypeName,
                                   String selectorName ) {
        CheckArg.isNotNull(nodeTypeName, "nodeTypeName");
        CheckArg.isNotNull(selectorName, "selectorName");
        return new NamedSelector(selectorName(nodeTypeName), selectorName(selectorName));
    }

    @Override
    public Column column( String selectorName,
                          String propertyName,
                          String columnName ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        if (propertyName == null) {
            return new Column(selectorName(selectorName));
        }
        CheckArg.isNotNull(columnName, "columnName");
        return new Column(selectorName(selectorName), propertyName, columnName);
    }

    @Override
    public Ordering ascending( javax.jcr.query.qom.DynamicOperand operand ) {
        DynamicOperand jcrOperand = CheckArg.getInstanceOf(operand, DynamicOperand.class, "operand");
        return new Ordering(jcrOperand, Order.ASCENDING);
    }

    @Override
    public Ordering descending( javax.jcr.query.qom.DynamicOperand operand ) {
        DynamicOperand jcrOperand = CheckArg.getInstanceOf(operand, DynamicOperand.class, "operand");
        return new Ordering(jcrOperand, Order.DESCENDING);
    }

    @Override
    public javax.jcr.query.qom.And and( javax.jcr.query.qom.Constraint constraint1,
                                        javax.jcr.query.qom.Constraint constraint2 ) {
        Constraint jcrConstraint1 = CheckArg.getInstanceOf(constraint1, Constraint.class, "constraint1");
        Constraint jcrConstraint2 = CheckArg.getInstanceOf(constraint2, Constraint.class, "constraint2");
        return new And(jcrConstraint1, jcrConstraint2);
    }

    @Override
    public BindVariableValue bindVariable( String bindVariableName ) {
        CheckArg.isNotNull(bindVariableName, "bindVariableName");
        return new BindVariableName(bindVariableName);
    }

    @Override
    public ChildNode childNode( String selectorName,
                                String path ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotNull(path, "path");
        return new ChildNode(selectorName(selectorName), path);
    }

    @Override
    public ChildNodeJoinCondition childNodeJoinCondition( String childSelectorName,
                                                          String parentSelectorName ) {
        CheckArg.isNotNull(childSelectorName, "childSelectorName");
        CheckArg.isNotNull(parentSelectorName, "parentSelectorName");
        return new ChildNodeJoinCondition(selectorName(parentSelectorName), selectorName(childSelectorName));
    }

    @Override
    public Comparison comparison( javax.jcr.query.qom.DynamicOperand operand1,
                                  String operator,
                                  javax.jcr.query.qom.StaticOperand operand2 ) {
        DynamicOperand jcrOperand1 = CheckArg.getInstanceOf(operand1, DynamicOperand.class, "operand1");
        CheckArg.isNotEmpty(operator, "operator");
        StaticOperand jcrOperand2 = CheckArg.getInstanceOf(operand2, StaticOperand.class, "operand2");
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
        return new Comparison(jcrOperand1, op, jcrOperand2);
    }

    @Override
    public DescendantNode descendantNode( String selectorName,
                                          String path ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotNull(path, "path");
        return new DescendantNode(selectorName(selectorName), path);
    }

    @Override
    public DescendantNodeJoinCondition descendantNodeJoinCondition( String descendantSelectorName,
                                                                    String ancestorSelectorName ) {
        CheckArg.isNotNull(descendantSelectorName, "descendantSelectorName");
        CheckArg.isNotNull(ancestorSelectorName, "ancestorSelectorName");
        return new DescendantNodeJoinCondition(selectorName(ancestorSelectorName), selectorName(descendantSelectorName));
    }

    @Override
    public EquiJoinCondition equiJoinCondition( String selector1Name,
                                                String property1Name,
                                                String selector2Name,
                                                String property2Name ) {
        CheckArg.isNotNull(selector1Name, "selector1Name");
        CheckArg.isNotNull(property1Name, "property1Name");
        CheckArg.isNotNull(selector2Name, "selector2Name");
        CheckArg.isNotNull(property2Name, "property2Name");
        return new EquiJoinCondition(selectorName(selector1Name), property1Name, selectorName(selector2Name), property2Name);
    }

    @Override
    public FullTextSearch fullTextSearch( String selectorName,
                                          String propertyName,
                                          javax.jcr.query.qom.StaticOperand fullTextSearchExpression ) throws RepositoryException {
        CheckArg.isNotNull(selectorName, "selectorName");
        StaticOperand expression = CheckArg.getInstanceOf(fullTextSearchExpression,
                                                          StaticOperand.class,
                                                          "fullTextSearchExpression");
        return new FullTextSearch(selectorName(selectorName), propertyName, expression, null);
    }

    @Override
    public FullTextSearchScore fullTextSearchScore( String selectorName ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        return new FullTextSearchScore(selectorName(selectorName));
    }

    @Override
    public Join join( javax.jcr.query.qom.Source left,
                      javax.jcr.query.qom.Source right,
                      String joinType,
                      javax.jcr.query.qom.JoinCondition joinCondition ) {
        Source leftSource = CheckArg.getInstanceOf(left, Source.class, "left");
        Source rightSource = CheckArg.getInstanceOf(right, Source.class, "right");
        JoinCondition jcrJoinCondition = CheckArg.getInstanceOf(joinCondition, JoinCondition.class, "joinCondition");
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
        return new Join(leftSource, type, rightSource, jcrJoinCondition);
    }

    @Override
    public Length length( javax.jcr.query.qom.PropertyValue propertyValue ) {
        PropertyValue jcrPropValue = CheckArg.getInstanceOf(propertyValue, PropertyValue.class, "propertyValue");
        return new Length(jcrPropValue);
    }

    @Override
    public LiteralValue literal( javax.jcr.Value literalValue ) throws RepositoryException {
        CheckArg.isNotNull(literalValue, "literalValue");
        return new LiteralValue(literalValue);
    }

    @Override
    public LowerCase lowerCase( javax.jcr.query.qom.DynamicOperand operand ) {
        DynamicOperand jcrOperand = CheckArg.getInstanceOf(operand, DynamicOperand.class, "operand");
        return new LowerCase(jcrOperand);
    }

    @Override
    public NodeLocalName nodeLocalName( String selectorName ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        return new NodeLocalName(selectorName(selectorName));
    }

    @Override
    public NodeName nodeName( String selectorName ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        return new NodeName(selectorName(selectorName));
    }

    @Override
    public Not not( javax.jcr.query.qom.Constraint constraint ) {
        Constraint jcrConstraint = CheckArg.getInstanceOf(constraint, Constraint.class, "constraint");
        return new Not(jcrConstraint);
    }

    @Override
    public Or or( javax.jcr.query.qom.Constraint constraint1,
                  javax.jcr.query.qom.Constraint constraint2 ) {
        Constraint jcrConstraint1 = CheckArg.getInstanceOf(constraint1, Constraint.class, "constraint1");
        Constraint jcrConstraint2 = CheckArg.getInstanceOf(constraint2, Constraint.class, "constraint2");
        return new Or(jcrConstraint1, jcrConstraint2);
    }

    @Override
    public PropertyExistence propertyExistence( String selectorName,
                                                String propertyName ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotNull(propertyName, "propertyName");
        return new PropertyExistence(selectorName(selectorName), propertyName);
    }

    @Override
    public PropertyValue propertyValue( String selectorName,
                                        String propertyName ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotNull(propertyName, "propertyName");
        return new PropertyValue(selectorName(selectorName), propertyName);
    }

    @Override
    public SameNode sameNode( String selectorName,
                              String path ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotNull(path, "path");
        return new SameNode(selectorName(selectorName), path);
    }

    @Override
    public SameNodeJoinCondition sameNodeJoinCondition( String selector1Name,
                                                        String selector2Name,
                                                        String selector2Path ) {
        CheckArg.isNotNull(selector1Name, "selector1Name");
        CheckArg.isNotNull(selector2Name, "selector2Name");
        return new SameNodeJoinCondition(selectorName(selector1Name), selectorName(selector2Name), selector2Path);
    }

    @Override
    public UpperCase upperCase( javax.jcr.query.qom.DynamicOperand operand ) {
        DynamicOperand jcrOperand = CheckArg.getInstanceOf(operand, DynamicOperand.class, "operand");
        return new UpperCase(jcrOperand);
    }

    @Override
    public NodePath nodePath( String selectorName ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        return new NodePath(selectorName(selectorName));
    }

    @Override
    public NodeDepth nodeDepth( String selectorName ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        return new NodeDepth(selectorName(selectorName));
    }

    @Override
    public Limit limit( int rowLimit,
                        int offset ) {
        CheckArg.isPositive(rowLimit, "rowLimit");
        CheckArg.isNonNegative(offset, "offset");
        return new Limit(rowLimit, offset);
    }

    @Override
    public Between between( javax.jcr.query.qom.DynamicOperand operand,
                            javax.jcr.query.qom.StaticOperand lowerBound,
                            javax.jcr.query.qom.StaticOperand upperBound,
                            boolean includeLowerBound,
                            boolean includeUpperBound ) {
        DynamicOperand jcrOperand = CheckArg.getInstanceOf(operand, DynamicOperand.class, "operand");
        StaticOperand lower = CheckArg.getInstanceOf(lowerBound, StaticOperand.class, "lowerBound");
        StaticOperand upper = CheckArg.getInstanceOf(upperBound, StaticOperand.class, "upperBound");
        return new Between(jcrOperand, lower, upper, includeLowerBound, includeUpperBound);
    }

    @Override
    public SetCriteria in( javax.jcr.query.qom.DynamicOperand operand,
                           javax.jcr.query.qom.StaticOperand... values ) {
        DynamicOperand jcrOperand = CheckArg.getInstanceOf(operand, DynamicOperand.class, "operand");
        List<StaticOperand> jcrValues = new ArrayList<StaticOperand>();
        for (javax.jcr.query.qom.StaticOperand value : values) {
            StaticOperand jcrValue = CheckArg.getInstanceOf(value, StaticOperand.class, "values");
            jcrValues.add(jcrValue);
        }
        return new SetCriteria(jcrOperand, jcrValues);
    }

    @Override
    public Subquery subquery( org.modeshape.jcr.api.query.qom.QueryCommand subqueryCommand ) {
        QueryCommand jcrCommand = CheckArg.getInstanceOf(subqueryCommand, QueryCommand.class, "subqueryCommand");
        return new Subquery(jcrCommand);
    }

    @Override
    public ArithmeticOperand add( javax.jcr.query.qom.DynamicOperand left,
                                  javax.jcr.query.qom.DynamicOperand right ) {
        return arithmeticOperand(left, ArithmeticOperator.ADD, right);
    }

    @Override
    public ArithmeticOperand subtract( javax.jcr.query.qom.DynamicOperand left,
                                       javax.jcr.query.qom.DynamicOperand right ) {
        return arithmeticOperand(left, ArithmeticOperator.SUBTRACT, right);
    }

    @Override
    public ArithmeticOperand multiply( javax.jcr.query.qom.DynamicOperand left,
                                       javax.jcr.query.qom.DynamicOperand right ) {
        return arithmeticOperand(left, ArithmeticOperator.MULTIPLY, right);
    }

    @Override
    public ArithmeticOperand divide( javax.jcr.query.qom.DynamicOperand left,
                                     javax.jcr.query.qom.DynamicOperand right ) {
        return arithmeticOperand(left, ArithmeticOperator.DIVIDE, right);
    }

    public ArithmeticOperand arithmeticOperand( javax.jcr.query.qom.DynamicOperand left,
                                                ArithmeticOperator operator,
                                                javax.jcr.query.qom.DynamicOperand right ) {
        DynamicOperand leftOperand = CheckArg.getInstanceOf(left, DynamicOperand.class, "left");
        DynamicOperand rightOperand = CheckArg.getInstanceOf(left, DynamicOperand.class, "left");
        return new ArithmeticOperand(leftOperand, operator, rightOperand);
    }

    @Override
    public ReferenceValue referenceValue( String selectorName ) {
        return new ReferenceValue(selectorName(selectorName), null);
    }

    @Override
    public ReferenceValue referenceValue( String selectorName,
                                          String propertyName ) {
        return new ReferenceValue(selectorName(selectorName), propertyName);
    }

}
