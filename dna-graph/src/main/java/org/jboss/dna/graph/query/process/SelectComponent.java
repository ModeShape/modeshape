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
package org.jboss.dna.graph.query.process;

import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.jboss.dna.graph.Location;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PropertyType;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.query.QueryContext;
import org.jboss.dna.graph.query.QueryResults.Columns;
import org.jboss.dna.graph.query.model.And;
import org.jboss.dna.graph.query.model.BindVariableName;
import org.jboss.dna.graph.query.model.ChildNode;
import org.jboss.dna.graph.query.model.Comparison;
import org.jboss.dna.graph.query.model.Constraint;
import org.jboss.dna.graph.query.model.DescendantNode;
import org.jboss.dna.graph.query.model.FullTextSearch;
import org.jboss.dna.graph.query.model.Literal;
import org.jboss.dna.graph.query.model.Not;
import org.jboss.dna.graph.query.model.Operator;
import org.jboss.dna.graph.query.model.Or;
import org.jboss.dna.graph.query.model.PropertyExistence;
import org.jboss.dna.graph.query.model.SameNode;
import org.jboss.dna.graph.query.model.StaticOperand;

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
     * <p>
     * For more control over the behavior, use the constructor that takes an {@link Analyzer} implementation (see
     * {@link #SelectComponent(ProcessingComponent, Constraint, Map, Analyzer)}).
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
        this(delegate, constraint, variables, null);
    }

    /**
     * Create a SELECT processing component that pass those tuples that satisfy the supplied constraint, using the supplied
     * {@link Analyzer} for the verification of the more complex/arduous constraints.
     * 
     * @param delegate the delegate processing component that this component should use to obtain the input tuples; may not be
     *        null
     * @param constraint the query constraint; may not be null
     * @param variables the map of variables keyed by their name (as used in {@link BindVariableName} constraints); may be null
     * @param analyzer the analyzer; may be null
     */
    public SelectComponent( ProcessingComponent delegate,
                            Constraint constraint,
                            Map<String, Object> variables,
                            Analyzer analyzer ) {
        super(delegate);
        this.constraint = constraint;
        this.variables = variables != null ? variables : Collections.<String, Object>emptyMap();
        this.checker = createChecker(delegate.getContext(), delegate.getColumns(), this.constraint, this.variables, analyzer);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.jboss.dna.graph.query.process.ProcessingComponent#execute()
     */
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
    protected static interface ConstraintChecker {
        /**
         * Return true if the tuple satisfies all of the constraints.
         * 
         * @param tuple the tuple; never null
         * @return true if the tuple satisifes the constraints, or false otherwise
         */
        boolean satisfiesConstraints( Object[] tuple );
    }

    /**
     * Inteface for criteria evaluation operations that cannot be defined efficiently, correctly, or completely using only the
     * tuple values.
     * 
     * @see SelectComponent#SelectComponent(ProcessingComponent, Constraint, Map, Analyzer)
     */
    public static interface Analyzer {
        /**
         * Determine whether the node specified by the location is the same node as that supplied by the path. This determines if
         * the nodes at the supplied location and path are the same node.
         * 
         * @param location the location of the node; never null
         * @param accessibleAtPath the path that the node can be accessed via
         * @return true if the node given by the {@link Location} is also accessible at the supplied path, or false otherwise
         */
        boolean isSameNode( Location location,
                            Path accessibleAtPath );

        /**
         * Determine whether the node at the supplied location has the named property.
         * 
         * @param location the location of the node; never null
         * @param propertyName the name of the property
         * @return true if the node at the supplied {@link Location} does contain the property, or false if it does not
         */
        boolean hasProperty( Location location,
                             Name propertyName );

        /**
         * Determine whether the node at the supplied location satisfies the supplied full-text query.
         * 
         * @param location the location of the node; never null
         * @param fullTextQuery the full-text search expression; never null
         * @return the full-text search score of the node, or 0.0d if the node does not satisfy the full-text query
         */
        double hasFullText( Location location,
                            String fullTextQuery );

        /**
         * Determine whether the named property of the node at the supplied location satisfies the supplied full-text query.
         * 
         * @param location the location of the node; never null
         * @param propertyName the name of the property; never null
         * @param fullTextQuery the full-text search expression; never null
         * @return the full-text search score of the node, or 0.0d if the node does not satisfy the full-text query
         */
        double hasFullText( Location location,
                            Name propertyName,
                            String fullTextQuery );
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
     * criteria}. For the most correct behavior, specify an {@link Analyzer} implementation.
     * 
     * @param context the context in which the query is being evaluated; may not be null
     * @param columns the definition of the result columns and the tuples; may not be null
     * @param constraint the criteria that this {@link SelectComponent} is to evaluate
     * @param variables the variables that are to be substituted for the various {@link BindVariableName} {@link StaticOperand
     *        operands}; may not be null
     * @param analyzer the analyzer that should be used to evalulate the operations that cannot be defined efficiently, correctly,
     *        or completely using only the tuple values; may be null if the tuple values should be used to perform the evaluation
     *        in perhaps an non-ideal manner
     * @return the constraint evaluator; never null
     */
    @SuppressWarnings( "unchecked" )
    protected ConstraintChecker createChecker( QueryContext context,
                                               Columns columns,
                                               Constraint constraint,
                                               Map<String, Object> variables,
                                               final Analyzer analyzer ) {
        if (constraint instanceof Or) {
            Or orConstraint = (Or)constraint;
            final ConstraintChecker left = createChecker(context, columns, orConstraint.getLeft(), variables, analyzer);
            final ConstraintChecker right = createChecker(context, columns, orConstraint.getRight(), variables, analyzer);
            return new ConstraintChecker() {
                public boolean satisfiesConstraints( Object[] tuple ) {
                    return left.satisfiesConstraints(tuple) || right.satisfiesConstraints(tuple);
                }
            };
        }
        if (constraint instanceof Not) {
            Not notConstraint = (Not)constraint;
            final ConstraintChecker original = createChecker(context, columns, notConstraint.getConstraint(), variables, analyzer);
            return new ConstraintChecker() {
                public boolean satisfiesConstraints( Object[] tuple ) {
                    return !original.satisfiesConstraints(tuple);
                }
            };
        }
        if (constraint instanceof And) {
            And andConstraint = (And)constraint;
            final ConstraintChecker left = createChecker(context, columns, andConstraint.getLeft(), variables, analyzer);
            final ConstraintChecker right = createChecker(context, columns, andConstraint.getRight(), variables, analyzer);
            return new ConstraintChecker() {
                public boolean satisfiesConstraints( Object[] tuple ) {
                    return left.satisfiesConstraints(tuple) && right.satisfiesConstraints(tuple);
                }
            };
        }
        if (constraint instanceof ChildNode) {
            ChildNode childConstraint = (ChildNode)constraint;
            final int locationIndex = columns.getLocationIndex(childConstraint.getSelectorName().getName());
            final Path parentPath = childConstraint.getParentPath();
            return new ConstraintChecker() {
                public boolean satisfiesConstraints( Object[] tuple ) {
                    Location location = (Location)tuple[locationIndex];
                    assert location.hasPath();
                    return location.getPath().getParent().equals(parentPath);
                }
            };
        }
        if (constraint instanceof DescendantNode) {
            DescendantNode descendantNode = (DescendantNode)constraint;
            final int locationIndex = columns.getLocationIndex(descendantNode.getSelectorName().getName());
            final Path ancestorPath = descendantNode.getAncestorPath();
            return new ConstraintChecker() {
                public boolean satisfiesConstraints( Object[] tuple ) {
                    Location location = (Location)tuple[locationIndex];
                    assert location.hasPath();
                    return location.getPath().isDecendantOf(ancestorPath);
                }
            };
        }
        if (constraint instanceof SameNode) {
            SameNode sameNode = (SameNode)constraint;
            final int locationIndex = columns.getLocationIndex(sameNode.getSelectorName().getName());
            final Path path = sameNode.getPath();
            if (analyzer != null) {
                return new ConstraintChecker() {
                    public boolean satisfiesConstraints( Object[] tuple ) {
                        Location location = (Location)tuple[locationIndex];
                        return analyzer.isSameNode(location, path);
                    }
                };
            }
            return new ConstraintChecker() {
                public boolean satisfiesConstraints( Object[] tuple ) {
                    Location location = (Location)tuple[locationIndex];
                    assert location.hasPath();
                    return location.getPath().isSameAs(path);
                }
            };
        }
        if (constraint instanceof PropertyExistence) {
            PropertyExistence propertyExistance = (PropertyExistence)constraint;
            String selectorName = propertyExistance.getSelectorName().getName();
            final Name propertyName = propertyExistance.getPropertyName();
            if (analyzer != null) {
                final int locationIndex = columns.getLocationIndex(selectorName);
                return new ConstraintChecker() {
                    public boolean satisfiesConstraints( Object[] tuple ) {
                        Location location = (Location)tuple[locationIndex];
                        return analyzer.hasProperty(location, propertyName);
                    }
                };
            }
            final int columnIndex = columns.getColumnIndexForProperty(selectorName, propertyName);
            return new ConstraintChecker() {
                public boolean satisfiesConstraints( Object[] tuple ) {
                    return tuple[columnIndex] != null;
                }
            };
        }
        if (constraint instanceof FullTextSearch) {
            if (analyzer != null) {
                FullTextSearch search = (FullTextSearch)constraint;
                String selectorName = search.getSelectorName().getName();
                final int locationIndex = columns.getLocationIndex(selectorName);
                final String expression = search.getFullTextSearchExpression();
                if (expression == null) {
                    return new ConstraintChecker() {
                        public boolean satisfiesConstraints( Object[] tuple ) {
                            return false;
                        }
                    };
                }
                final Name propertyName = search.getPropertyName(); // may be null
                final int scoreIndex = columns.getFullTextSearchScoreIndexFor(selectorName);
                assert scoreIndex >= 0 : "Columns do not have room for the search scores";
                if (propertyName != null) {
                    return new ConstraintChecker() {
                        public boolean satisfiesConstraints( Object[] tuple ) {
                            Location location = (Location)tuple[locationIndex];
                            if (location == null) return false;
                            double score = analyzer.hasFullText(location, propertyName, expression);
                            // put the score on the correct tuple value ...
                            Double existing = (Double)tuple[scoreIndex];
                            if (existing != null) {
                                score = Math.max(existing.doubleValue(), score);
                            }
                            tuple[scoreIndex] = new Double(score);
                            return true;
                        }
                    };
                }
                return new ConstraintChecker() {
                    public boolean satisfiesConstraints( Object[] tuple ) {
                        Location location = (Location)tuple[locationIndex];
                        if (location == null) return false;
                        double score = analyzer.hasFullText(location, expression);
                        // put the score on the correct tuple value ...
                        Double existing = (Double)tuple[scoreIndex];
                        if (existing != null) {
                            score = Math.max(existing.doubleValue(), score);
                        }
                        tuple[scoreIndex] = new Double(score);
                        return true;
                    }
                };
            }
            return new ConstraintChecker() {
                public boolean satisfiesConstraints( Object[] tuple ) {
                    return true;
                }
            };
        }
        if (constraint instanceof Comparison) {
            final ValueFactory<String> stringFactory = context.getExecutionContext().getValueFactories().getStringFactory();
            Comparison comparison = (Comparison)constraint;

            // Create the correct dynamic operation ...
            final DynamicOperation dynamicOperation = createDynamicOperation(context, columns, comparison.getOperand1());
            final PropertyType expectedType = dynamicOperation.getExpectedType();

            // Determine the literal value ...
            StaticOperand staticOperand = comparison.getOperand2();
            Object literalValue = null;
            if (staticOperand instanceof BindVariableName) {
                BindVariableName bindVariable = (BindVariableName)staticOperand;
                String variableName = bindVariable.getVariableName();
                literalValue = variables.get(variableName); // may be null
            } else {
                Literal literal = (Literal)staticOperand;
                literalValue = literal.getValue();
            }
            // Create the correct comparator ...
            final Comparator<Object> comparator = (Comparator<Object>)expectedType.getComparator();
            // Create the correct operation ...
            ValueFactory<?> literalFactory = context.getExecutionContext().getValueFactories().getValueFactory(expectedType);
            final Object rhs = literalFactory.create(literalValue);
            switch (comparison.getOperator()) {
                case EQUAL_TO:
                    return new ConstraintChecker() {
                        public boolean satisfiesConstraints( Object[] tuples ) {
                            return comparator.compare(dynamicOperation.evaluate(tuples), rhs) == 0;
                        }
                    };
                case GREATER_THAN:
                    return new ConstraintChecker() {
                        public boolean satisfiesConstraints( Object[] tuples ) {
                            return comparator.compare(dynamicOperation.evaluate(tuples), rhs) > 0;
                        }
                    };
                case GREATER_THAN_OR_EQUAL_TO:
                    return new ConstraintChecker() {
                        public boolean satisfiesConstraints( Object[] tuples ) {
                            return comparator.compare(dynamicOperation.evaluate(tuples), rhs) >= 0;
                        }
                    };
                case LESS_THAN:
                    return new ConstraintChecker() {
                        public boolean satisfiesConstraints( Object[] tuples ) {
                            return comparator.compare(dynamicOperation.evaluate(tuples), rhs) < 0;
                        }
                    };
                case LESS_THAN_OR_EQUAL_TO:
                    return new ConstraintChecker() {
                        public boolean satisfiesConstraints( Object[] tuples ) {
                            return comparator.compare(dynamicOperation.evaluate(tuples), rhs) <= 0;
                        }
                    };
                case NOT_EQUAL_TO:
                    return new ConstraintChecker() {
                        public boolean satisfiesConstraints( Object[] tuples ) {
                            return comparator.compare(dynamicOperation.evaluate(tuples), rhs) != 0;
                        }
                    };
                case LIKE:
                    // Convert the LIKE expression to a regular expression
                    final Pattern pattern = createRegexFromLikeExpression(stringFactory.create(rhs));
                    return new ConstraintChecker() {
                        public boolean satisfiesConstraints( Object[] tuples ) {
                            Object tupleValue = dynamicOperation.evaluate(tuples);
                            if (tupleValue == null) return false;
                            String value = stringFactory.create(tupleValue);
                            return pattern.matcher(value).matches();
                        }
                    };
            }
        }
        assert false;
        return null;
    }

    protected static Pattern createRegexFromLikeExpression( String likeExpression ) {
        return null;
    }
}
