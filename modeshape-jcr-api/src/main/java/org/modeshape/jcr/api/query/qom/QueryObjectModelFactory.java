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
package org.modeshape.jcr.api.query.qom;

import javax.jcr.RepositoryException;
import javax.jcr.query.InvalidQueryException;
import javax.jcr.query.qom.Column;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.DynamicOperand;
import javax.jcr.query.qom.Ordering;
import javax.jcr.query.qom.QueryObjectModel;
import javax.jcr.query.qom.Source;
import javax.jcr.query.qom.StaticOperand;

/**
 * An extension to the standard JCR {@link QueryObjectModelFactory} interface, with methods to create additional components for
 * more powerful queries.
 */
public interface QueryObjectModelFactory extends javax.jcr.query.qom.QueryObjectModelFactory {

    /**
     * Creates a set query.
     * 
     * @param command set query; non-null
     * @return the executable query; non-null
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test and the parameters given fail that test. See the individual QOM factory methods for the validity criteria
     *         of each query element.
     * @throws RepositoryException if another error occurs.
     */
    public SetQueryObjectModel createQuery( SetQuery command ) throws InvalidQueryException, RepositoryException;

    /**
     * Creates a query for a select query command.
     * 
     * @param command select query; non-null
     * @return the executable query; non-null
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test and the parameters given fail that test. See the individual QOM factory methods for the validity criteria
     *         of each query element.
     * @throws RepositoryException if another error occurs.
     */
    public QueryObjectModel createQuery( SelectQuery command ) throws InvalidQueryException, RepositoryException;

    /**
     * Creates a query with one or more selectors.
     * 
     * @param source the node-tuple source; non-null
     * @param constraint the constraint, or null if none
     * @param orderings zero or more orderings; null is equivalent to a zero-length array
     * @param columns the columns; null is equivalent to a zero-length array
     * @param limit the limit; null is equivalent to having no limit
     * @param isDistinct true if the query should return distinct values; or false if no duplicate removal should be performed
     * @return the select query; non-null
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test and the parameters given fail that test. See the individual QOM factory methods for the validity criteria
     *         of each query element.
     * @throws RepositoryException if another error occurs.
     */
    public SelectQuery select( Source source,
                               Constraint constraint,
                               Ordering[] orderings,
                               Column[] columns,
                               Limit limit,
                               boolean isDistinct ) throws InvalidQueryException, RepositoryException;

    /**
     * Creates a query command that effectively appends the results of the right-hand query to those of the left-hand query.
     * 
     * @param left the query command that represents left-side of the set operation; non-null and must have columns that are
     *        equivalent and union-able to those of the right-side query
     * @param right the query command that represents right-side of the set operation; non-null and must have columns that are
     *        equivalent and union-able to those of the left-side query
     * @param orderings zero or more orderings; null is equivalent to a zero-length array
     * @param limit the limit; null is equivalent to having no limit
     * @param all true if duplicate rows in the left- and right-hand side results should be included, or false if duplicate rows
     *        should be eliminated
     * @return the select query; non-null
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test and the parameters given fail that test. See the individual QOM factory methods for the validity criteria
     *         of each query element.
     * @throws RepositoryException if another error occurs.
     */
    public SetQuery union( QueryCommand left,
                           QueryCommand right,
                           Ordering[] orderings,
                           Limit limit,
                           boolean all ) throws InvalidQueryException, RepositoryException;

    /**
     * Creates a query command that returns all rows that are both in the result of the left-hand query and in the result of the
     * right-hand query.
     * 
     * @param left the query command that represents left-side of the set operation; non-null and must have columns that are
     *        equivalent and union-able to those of the right-side query
     * @param right the query command that represents right-side of the set operation; non-null and must have columns that are
     *        equivalent and union-able to those of the left-side query
     * @param orderings zero or more orderings; null is equivalent to a zero-length array
     * @param limit the limit; null is equivalent to having no limit
     * @param all true if duplicate rows in the left- and right-hand side results should be included, or false if duplicate rows
     *        should be eliminated
     * @return the select query; non-null
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test and the parameters given fail that test. See the individual QOM factory methods for the validity criteria
     *         of each query element.
     * @throws RepositoryException if another error occurs.
     */
    public SetQuery intersect( QueryCommand left,
                               QueryCommand right,
                               Ordering[] orderings,
                               Limit limit,
                               boolean all ) throws InvalidQueryException, RepositoryException;

    /**
     * Creates a query command that returns all rows that are in the result of the left-hand query but not in the result of the
     * right-hand query.
     * 
     * @param left the query command that represents left-side of the set operation; non-null and must have columns that are
     *        equivalent and union-able to those of the right-side query
     * @param right the query command that represents right-side of the set operation; non-null and must have columns that are
     *        equivalent and union-able to those of the left-side query
     * @param orderings zero or more orderings; null is equivalent to a zero-length array
     * @param limit the limit; null is equivalent to having no limit
     * @param all true if duplicate rows in the left- and right-hand side results should be included, or false if duplicate rows
     *        should be eliminated
     * @return the select query; non-null
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test and the parameters given fail that test. See the individual QOM factory methods for the validity criteria
     *         of each query element.
     * @throws RepositoryException if another error occurs.
     */
    public SetQuery except( QueryCommand left,
                            QueryCommand right,
                            Ordering[] orderings,
                            Limit limit,
                            boolean all ) throws InvalidQueryException, RepositoryException;

    /**
     * Evaluates to a <code>LONG</code> value equal to the number of children for each of the node(s) in the specified selector.
     * <p>
     * The query is invalid if <code>selector</code> is not the name of a selector in the query.
     *
     * @param selectorName the selector name; non-null
     * @return the operand; non-null
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test (and not leave it until later, on {@link #createQuery}), and the parameters given fail that test
     * @throws RepositoryException if the operation otherwise fails
     */
    public ChildCount childCount( String selectorName ) throws InvalidQueryException, RepositoryException;

    /**
     * Creates a dynamic operand that casts another operand to a desired type.
     *
     * @param operand a {@link DynamicOperand} instance, may not be null.
     * @param desiredType a name of the type instance, may not be null.
     * @return a {@link Cast} operand instance
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implementation chooses to perform
     * that test (and not leave it until later, on {@link #createQuery}), and the parameters given fail that test
     * @throws RepositoryException if the operation otherwise fails
     */
    public Cast cast(DynamicOperand operand, String desiredType) throws InvalidQueryException, RepositoryException;

    /**
     * Evaluates to a <code>LONG</code> value equal to the depth of a node in the specified selector.
     * <p>
     * The query is invalid if <code>selector</code> is not the name of a selector in the query.
     * 
     * @param selectorName the selector name; non-null
     * @return the operand; non-null
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test (and not leave it until later, on {@link #createQuery}), and the parameters given fail that test
     * @throws RepositoryException if the operation otherwise fails
     */
    public NodeDepth nodeDepth( String selectorName ) throws InvalidQueryException, RepositoryException;

    /**
     * Evaluates to a <code>PATH</code> value equal to the prefix-qualified path of a node in the specified selector.
     * <p>
     * The query is invalid if <code>selector</code> is not the name of a selector in the query.
     * 
     * @param selectorName the selector name; non-null
     * @return the operand; non-null
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test (and not leave it until later, on {@link #createQuery}), and the parameters given fail that test
     * @throws RepositoryException if the operation otherwise fails
     */
    public NodePath nodePath( String selectorName ) throws InvalidQueryException, RepositoryException;

    /**
     * Evaluates to a limit on the maximum number of tuples in the results and the number of rows that are skipped before the
     * first tuple in the results.
     * 
     * @param rowLimit the maximum number of rows; must be a positive number, or {@link Integer#MAX_VALUE} if there is to be a
     *        non-zero offset but no limit
     * @param offset the number of rows to skip before beginning the results; must be 0 or a positive number
     * @return the operand; non-null
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test (and not leave it until later, on {@link #createQuery}), and the parameters given fail that test
     * @throws RepositoryException if the operation otherwise fails
     */
    public Limit limit( int rowLimit,
                        int offset ) throws InvalidQueryException, RepositoryException;

    /**
     * Tests that the value (or values) defined by the supplied dynamic operand are within a specified range. The range is
     * specified by a lower and upper bound, and whether each of the boundary values is included in the range.
     * 
     * @param operand the dynamic operand describing the values that are to be constrained
     * @param lowerBound the lower bound of the range
     * @param upperBound the upper bound of the range
     * @param includeLowerBound true if the lower boundary value is not be included
     * @param includeUpperBound true if the upper boundary value is not be included
     * @return the constraint; non-null
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test (and not leave it until later, on {@link #createQuery}), and the parameters given fail that test
     * @throws RepositoryException if the operation otherwise fails
     */
    public Between between( DynamicOperand operand,
                            StaticOperand lowerBound,
                            StaticOperand upperBound,
                            boolean includeLowerBound,
                            boolean includeUpperBound ) throws InvalidQueryException, RepositoryException;

    /**
     * Tests that the value (or values) defined by the supplied dynamic operand are found within the specified set of values.
     * 
     * @param operand the dynamic operand describing the values that are to be constrained
     * @param values the static operand values; may not be null or empty
     * @return the constraint; non-null
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test (and not leave it until later, on {@link #createQuery}), and the parameters given fail that test
     * @throws RepositoryException if the operation otherwise fails
     */
    public SetCriteria in( DynamicOperand operand,
                           StaticOperand... values ) throws InvalidQueryException, RepositoryException;

    /**
     * Creates a subquery that can be used as a {@link StaticOperand} in another query.
     * 
     * @param subqueryCommand the query command that is to be used as the subquery
     * @return the constraint; non-null
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test (and not leave it until later, on {@link #createQuery}), and the parameters given fail that test
     * @throws RepositoryException if the operation otherwise fails
     */
    public Subquery subquery( QueryCommand subqueryCommand ) throws InvalidQueryException, RepositoryException;

    /**
     * Create an arithmetic dynamic operand that adds the numeric value of the two supplied operand(s).
     * 
     * @param left the left-hand-side operand; not null
     * @param right the right-hand-side operand; not null
     * @return the dynamic operand; non-null
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test (and not leave it until later, on {@link #createQuery}), and the parameters given fail that test
     * @throws RepositoryException if the operation otherwise fails
     */
    public ArithmeticOperand add( DynamicOperand left,
                                  DynamicOperand right ) throws InvalidQueryException, RepositoryException;

    /**
     * Create an arithmetic dynamic operand that subtracts the numeric value of the second operand from the numeric value of the
     * first.
     * 
     * @param left the left-hand-side operand; not null
     * @param right the right-hand-side operand; not null
     * @return the dynamic operand; non-null
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test (and not leave it until later, on {@link #createQuery}), and the parameters given fail that test
     * @throws RepositoryException if the operation otherwise fails
     */
    public ArithmeticOperand subtract( DynamicOperand left,
                                       DynamicOperand right ) throws InvalidQueryException, RepositoryException;

    /**
     * Create an arithmetic dynamic operand that multplies the numeric value of the first operand by the numeric value of the
     * second.
     * 
     * @param left the left-hand-side operand; not null
     * @param right the right-hand-side operand; not null
     * @return the dynamic operand; non-null
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test (and not leave it until later, on {@link #createQuery}), and the parameters given fail that test
     * @throws RepositoryException if the operation otherwise fails
     */
    public ArithmeticOperand multiply( DynamicOperand left,
                                       DynamicOperand right ) throws InvalidQueryException, RepositoryException;

    /**
     * Create an arithmetic dynamic operand that divides the numeric value of the first operand by the numeric value of the
     * second.
     * 
     * @param left the left-hand-side operand; not null
     * @param right the right-hand-side operand; not null
     * @return the dynamic operand; non-null
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test (and not leave it until later, on {@link #createQuery}), and the parameters given fail that test
     * @throws RepositoryException if the operation otherwise fails
     */
    public ArithmeticOperand divide( DynamicOperand left,
                                     DynamicOperand right ) throws InvalidQueryException, RepositoryException;

    /**
     * Creates a dynamic operand that evaluates to the REFERENCE value of the any property on the specified selector.
     * <p>
     * The query is invalid if:
     * <ul>
     * <li><code>selector</code> is not the name of a selector in the query, or</li>
     * <li><code>property</code> is not a syntactically valid JCR name.</li>
     * </ul>
     * 
     * @param selectorName the selector name; non-null
     * @return the operand; non-null
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test (and not leave it until later, on {@link #createQuery}), and the parameters given fail that test
     * @throws RepositoryException if the operation otherwise fails
     */
    public ReferenceValue referenceValue( String selectorName ) throws InvalidQueryException, RepositoryException;

    /**
     * Creates a dynamic operand that evaluates to the REFERENCE value of the specified property on the specified selector.
     * <p>
     * The query is invalid if:
     * <ul>
     * <li><code>selector</code> is not the name of a selector in the query, or</li>
     * <li><code>property</code> is not a syntactically valid JCR name.</li>
     * </ul>
     * 
     * @param selectorName the selector name; non-null
     * @param propertyName the reference property name; non-null
     * @return the operand; non-null
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test (and not leave it until later, on {@link #createQuery}), and the parameters given fail that test
     * @throws RepositoryException if the operation otherwise fails
     */
    public ReferenceValue referenceValue( String selectorName,
                                          String propertyName ) throws InvalidQueryException, RepositoryException;

    /**
     * Filters node-tuples based on reverse like operation.
     * 
     * @param operand1 the first operand; non-null
     * @param operand2 the second operand; non-null
     * @return the constraint; non-null
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test (and not leave it until later, on {@link #createQuery}), and the parameters given fail that test
     * @throws RepositoryException if the operation otherwise fails
     */
    public Relike relike( javax.jcr.query.qom.StaticOperand operand1,
                          javax.jcr.query.qom.PropertyValue operand2 ) throws InvalidQueryException, RepositoryException;

    /**
     * Orders by the value of the specified operand, in ascending order. The query is invalid if <code>operand</code> does not
     * evaluate to a scalar value. Null values are returned last; use {@link #ascendingNullsFirst(DynamicOperand)} for other
     * behavior.
     * 
     * @param operand the operand by which to order; non-null
     * @return the ordering
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test (and not leave it until later, on {@link #createQuery}), and the parameters given fail that test
     * @throws RepositoryException if the operation otherwise fails
     */
    @Override
    public Ordering ascending( DynamicOperand operand ) throws InvalidQueryException, RepositoryException;

    /**
     * Orders by the value of the specified operand, in descending order. The query is invalid if <code>operand</code> does not
     * evaluate to a scalar value. Null values are returned first; use {@link #descendingNullsLast(DynamicOperand)} for other
     * behavior.
     * 
     * @param operand the operand by which to order; non-null
     * @return the ordering
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test (and not leave it until later, on {@link #createQuery}), and the parameters given fail that test
     * @throws RepositoryException if the operation otherwise fails
     */
    @Override
    public Ordering descending( DynamicOperand operand ) throws InvalidQueryException, RepositoryException;

    /**
     * Orders by the value of the specified operand, in ascending order. The query is invalid if <code>operand</code> does not
     * evaluate to a scalar value.
     * 
     * @param operand the operand by which to order; non-null
     * @return the ordering
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test (and not leave it until later, on {@link #createQuery}), and the parameters given fail that test
     * @throws RepositoryException if the operation otherwise fails
     */
    public Ordering ascendingNullsFirst( DynamicOperand operand ) throws InvalidQueryException, RepositoryException;

    /**
     * Orders by the value of the specified operand, in descending order. The query is invalid if <code>operand</code> does not
     * evaluate to a scalar value.
     * 
     * @param operand the operand by which to order; non-null
     * @return the ordering
     * @throws InvalidQueryException if a particular validity test is possible on this method, the implemention chooses to perform
     *         that test (and not leave it until later, on {@link #createQuery}), and the parameters given fail that test
     * @throws RepositoryException if the operation otherwise fails
     */
    public Ordering descendingNullsLast( DynamicOperand operand ) throws InvalidQueryException, RepositoryException;

}
