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

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.query.QueryResults.Columns;
import org.modeshape.jcr.query.QueryResults.Location;
import org.modeshape.jcr.query.model.And;
import org.modeshape.jcr.query.model.BindVariableName;
import org.modeshape.jcr.query.model.ChildNode;
import org.modeshape.jcr.query.model.Comparison;
import org.modeshape.jcr.query.model.Constraint;
import org.modeshape.jcr.query.model.DescendantNode;
import org.modeshape.jcr.query.model.FullTextSearch;
import org.modeshape.jcr.query.model.Literal;
import org.modeshape.jcr.query.model.Not;
import org.modeshape.jcr.query.model.Or;
import org.modeshape.jcr.query.model.PropertyExistence;
import org.modeshape.jcr.query.model.SameNode;
import org.modeshape.jcr.query.model.SetCriteria;
import org.modeshape.jcr.query.model.StaticOperand;
import org.modeshape.jcr.query.model.TypeSystem;
import org.modeshape.jcr.query.model.TypeSystem.TypeFactory;
import org.modeshape.jcr.query.validate.Schemata;
import org.modeshape.jcr.value.Path;

/**
 */
public class SelectComponent extends DelegatingComponent {

    private final Constraint constraint;
    private final ConstraintChecker checker;
    private final Map<String, Object> variables;

    /**
     * Create a SELECT processing component that pass those tuples that satisfy the supplied constraint. Certain constraints
     * (including {@link FullTextSearch}, {@link SameNode} and {@link PropertyExistence}) are evaluated in a fairly limited
     * fashion, essentially operating upon the tuple values themselves.
     * <p>
     * For example, the {@link SameNode} constraint is satisfied when the selected node has the same path as the constraint's
     * {@link SameNode#getPath() path}. And the {@link PropertyExistence} constraint is satisfied when the
     * {@link PropertyExistence#getPropertyName() property} is represented in the tuple with a non-null value. Similarly,
     * {@link FullTextSearch} always evaluates to true. Obviously these implementations will likely not be sufficient for many
     * purposes. But in cases where these particular constraints are handled in other ways (and thus not expected to be seen by
     * this processor), this form may be sufficient.
     * </p>
     * 
     * @param delegate the delegate processing component that this component should use to obtain the input tuples; may not be
     *        null
     * @param constraint the query constraint; may not be null
     * @param variables the map of variables keyed by their name (as used in {@link BindVariableName} constraints); may be null
     */
    public SelectComponent( ProcessingComponent delegate,
                            Constraint constraint,
                            Map<String, Object> variables ) {
        super(delegate);
        this.constraint = constraint;
        this.variables = variables != null ? variables : Collections.<String, Object>emptyMap();
        TypeSystem types = delegate.getContext().getTypeSystem();
        Schemata schemata = delegate.getContext().getSchemata();
        this.checker = createChecker(types, schemata, delegate.getColumns(), this.constraint, this.variables);
    }

    @Override
    public List<Object[]> execute() {
        List<Object[]> tuples = delegate().execute();
        if (!tuples.isEmpty()) {
            // Iterate through the tuples, removing any that do not satisfy the constraint ...
            Iterator<Object[]> iter = tuples.iterator();
            while (iter.hasNext()) {
                if (!checker.satisfiesConstraints(iter.next())) {
                    iter.remove();
                }
            }
        }
        return tuples;
    }

    /**
     * Interface used to determine whether a tuple satisfies all of the constraints applied to the SELECT node.
     */
    public static interface ConstraintChecker {
        /**
         * Return true if the tuple satisfies all of the constraints.
         * 
         * @param tuple the tuple; never null
         * @return true if the tuple satisifes the constraints, or false otherwise
         */
        boolean satisfiesConstraints( Object[] tuple );
    }

    /**
     * Interface defining the {@link Comparison} functionality for a specific {@link Operator}.
     */
    protected static interface CompareOperation {
        /**
         * Perform the comparison operation.
         * 
         * @param tupleValue the value in the tuple
         * @param criteriaValue the right-hand-side of the comparison
         * @return true if the comparison criteria is satisfied, or false otherwise
         */
        boolean evaluate( Object tupleValue,
                          Object criteriaValue );
    }

    /**
     * Create the constraint evaluator that is used by the {@link SelectComponent} to evaluate the supplied {@link Constraint
     * criteria}.
     * 
     * @param types the type system; may not be null
     * @param schemata the schemata; may not be null
     * @param columns the definition of the result columns and the tuples; may not be null
     * @param constraint the criteria that this {@link SelectComponent} is to evaluate
     * @param variables the variables that are to be substituted for the various {@link BindVariableName} {@link StaticOperand
     *        operands}; may not be null
     * @return the constraint evaluator; never null
     */
    protected ConstraintChecker createChecker( final TypeSystem types,
                                               Schemata schemata,
                                               Columns columns,
                                               Constraint constraint,
                                               Map<String, Object> variables ) {
        if (constraint instanceof Or) {
            Or orConstraint = (Or)constraint;
            final ConstraintChecker left = createChecker(types, schemata, columns, orConstraint.left(), variables);
            final ConstraintChecker right = createChecker(types, schemata, columns, orConstraint.right(), variables);
            return new ConstraintChecker() {
                @Override
                public boolean satisfiesConstraints( Object[] tuple ) {
                    return left.satisfiesConstraints(tuple) || right.satisfiesConstraints(tuple);
                }
            };
        }
        if (constraint instanceof Not) {
            Not notConstraint = (Not)constraint;
            final ConstraintChecker original = createChecker(types, schemata, columns, notConstraint.getConstraint(), variables);
            return new ConstraintChecker() {
                @Override
                public boolean satisfiesConstraints( Object[] tuple ) {
                    return !original.satisfiesConstraints(tuple);
                }
            };
        }
        if (constraint instanceof And) {
            And andConstraint = (And)constraint;
            final ConstraintChecker left = createChecker(types, schemata, columns, andConstraint.left(), variables);
            final ConstraintChecker right = createChecker(types, schemata, columns, andConstraint.right(), variables);
            return new ConstraintChecker() {
                @Override
                public boolean satisfiesConstraints( Object[] tuple ) {
                    return left.satisfiesConstraints(tuple) && right.satisfiesConstraints(tuple);
                }
            };
        }
        if (constraint instanceof ChildNode) {
            ChildNode childConstraint = (ChildNode)constraint;
            final int locationIndex = columns.getLocationIndex(childConstraint.selectorName().name());
            final Path parentPath = (Path)types.getPathFactory().create(childConstraint.getParentPath());
            return new ConstraintChecker() {
                @Override
                public boolean satisfiesConstraints( Object[] tuple ) {
                    Location location = (Location)tuple[locationIndex];
                    return location.getPath().getParent().equals(parentPath);
                }
            };
        }
        if (constraint instanceof DescendantNode) {
            DescendantNode descendantNode = (DescendantNode)constraint;
            final int locationIndex = columns.getLocationIndex(descendantNode.selectorName().name());
            final Path ancestorPath = (Path)types.getPathFactory().create(descendantNode.getAncestorPath());
            return new ConstraintChecker() {
                @Override
                public boolean satisfiesConstraints( Object[] tuple ) {
                    Location location = (Location)tuple[locationIndex];
                    return location.getPath().isDescendantOf(ancestorPath);
                }
            };
        }
        if (constraint instanceof SameNode) {
            SameNode sameNode = (SameNode)constraint;
            final int locationIndex = columns.getLocationIndex(sameNode.selectorName().name());
            final String path = sameNode.getPath();
            return new ConstraintChecker() {
                @Override
                public boolean satisfiesConstraints( Object[] tuple ) {
                    Location location = (Location)tuple[locationIndex];
                    return location.toString().equals(path);
                }
            };
        }
        if (constraint instanceof PropertyExistence) {
            PropertyExistence propertyExistance = (PropertyExistence)constraint;
            String selectorName = propertyExistance.selectorName().name();
            final String propertyName = propertyExistance.getPropertyName();
            final int columnIndex = columns.getColumnIndexForProperty(selectorName, propertyName);
            return new ConstraintChecker() {
                @Override
                public boolean satisfiesConstraints( Object[] tuple ) {
                    return tuple[columnIndex] != null;
                }
            };
        }
        if (constraint instanceof FullTextSearch) {
            return new ConstraintChecker() {
                @Override
                public boolean satisfiesConstraints( Object[] tuple ) {
                    return true;
                }
            };
        }
        if (constraint instanceof Comparison) {
            Comparison comparison = (Comparison)constraint;

            // Create the correct dynamic operation ...
            DynamicOperation dynamicOperation = createDynamicOperation(types, schemata, columns, comparison.getOperand1());
            Operator operator = comparison.operator();
            StaticOperand staticOperand = comparison.getOperand2();
            return createChecker(types, schemata, columns, dynamicOperation, operator, staticOperand);
        }
        if (constraint instanceof SetCriteria) {
            SetCriteria setCriteria = (SetCriteria)constraint;
            DynamicOperation dynamicOperation = createDynamicOperation(types, schemata, columns, setCriteria.leftOperand());
            Operator operator = Operator.EQUAL_TO;
            final List<ConstraintChecker> checkers = new LinkedList<ConstraintChecker>();
            for (StaticOperand setValue : setCriteria.rightOperands()) {
                ConstraintChecker rightChecker = createChecker(types, schemata, columns, dynamicOperation, operator, setValue);
                assert rightChecker != null;
                checkers.add(rightChecker);
            }
            if (checkers.isEmpty()) {
                // Nothing will satisfy these constraints ...
                return new ConstraintChecker() {
                    @Override
                    public boolean satisfiesConstraints( Object[] tuple ) {
                        return false;
                    }
                };
            }
            return new ConstraintChecker() {
                @Override
                public boolean satisfiesConstraints( Object[] tuple ) {
                    for (ConstraintChecker checker : checkers) {
                        if (checker.satisfiesConstraints(tuple)) return true;
                    }
                    return false;
                }
            };
        }
        assert false;
        return null;
    }

    @SuppressWarnings( "unchecked" )
    protected ConstraintChecker createChecker( final TypeSystem types,
                                               Schemata schemata,
                                               Columns columns,
                                               final DynamicOperation dynamicOperation,
                                               Operator operator,
                                               StaticOperand staticOperand ) {
        final String expectedType = dynamicOperation.getExpectedType();

        // Determine the literal value ...
        Object literalValue = null;
        if (staticOperand instanceof BindVariableName) {
            BindVariableName bindVariable = (BindVariableName)staticOperand;
            String variableName = bindVariable.getBindVariableName();
            literalValue = variables.get(variableName); // may be null
        } else {
            Literal literal = (Literal)staticOperand;
            literalValue = literal.value();
        }
        // Create the correct comparator ...
        final TypeFactory<?> typeFactory = types.getTypeFactory(expectedType);
        assert typeFactory != null;
        final Comparator<Object> comparator = (Comparator<Object>)typeFactory.getComparator();
        assert comparator != null;
        // Create the correct operation ...
        final TypeFactory<?> literalFactory = types.getTypeFactory(expectedType);
        final Object rhs = literalFactory.create(literalValue);
        switch (operator) {
            case EQUAL_TO:
                return new ConstraintChecker() {
                    @Override
                    public boolean satisfiesConstraints( Object[] tuples ) {
                        return comparator.compare(dynamicOperation.evaluate(tuples), rhs) == 0;
                    }
                };
            case GREATER_THAN:
                return new ConstraintChecker() {
                    @Override
                    public boolean satisfiesConstraints( Object[] tuples ) {
                        return comparator.compare(dynamicOperation.evaluate(tuples), rhs) > 0;
                    }
                };
            case GREATER_THAN_OR_EQUAL_TO:
                return new ConstraintChecker() {
                    @Override
                    public boolean satisfiesConstraints( Object[] tuples ) {
                        return comparator.compare(dynamicOperation.evaluate(tuples), rhs) >= 0;
                    }
                };
            case LESS_THAN:
                return new ConstraintChecker() {
                    @Override
                    public boolean satisfiesConstraints( Object[] tuples ) {
                        return comparator.compare(dynamicOperation.evaluate(tuples), rhs) < 0;
                    }
                };
            case LESS_THAN_OR_EQUAL_TO:
                return new ConstraintChecker() {
                    @Override
                    public boolean satisfiesConstraints( Object[] tuples ) {
                        return comparator.compare(dynamicOperation.evaluate(tuples), rhs) <= 0;
                    }
                };
            case NOT_EQUAL_TO:
                return new ConstraintChecker() {
                    @Override
                    public boolean satisfiesConstraints( Object[] tuples ) {
                        return comparator.compare(dynamicOperation.evaluate(tuples), rhs) != 0;
                    }
                };
            case LIKE:
                // Convert the LIKE expression to a regular expression
                final Pattern pattern = createRegexFromLikeExpression(types.asString(rhs));
                return new ConstraintChecker() {
                    @Override
                    public boolean satisfiesConstraints( Object[] tuples ) {
                        Object tupleValue = dynamicOperation.evaluate(tuples);
                        if (tupleValue == null) return false;
                        String value = types.asString(tupleValue);
                        return pattern.matcher(value).matches();
                    }
                };
        }
        assert false;
        return null;
    }

    protected static Pattern createRegexFromLikeExpression( String likeExpression ) {
        return null;
    }
}
