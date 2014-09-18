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

package org.modeshape.jcr.index.local;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.jcr.query.qom.And;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Not;
import javax.jcr.query.qom.Or;
import javax.jcr.query.qom.PropertyExistence;
import javax.jcr.query.qom.StaticOperand;
import org.modeshape.common.collection.EmptyIterator;
import org.modeshape.common.collection.MultiIterator;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.api.query.qom.Between;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.api.query.qom.SetCriteria;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.index.local.IndexValues.Converter;
import org.modeshape.jcr.query.model.BindVariableName;
import org.modeshape.jcr.query.model.Comparison;
import org.modeshape.jcr.query.model.Literal;
import org.modeshape.jcr.spi.index.Index;
import org.modeshape.jcr.spi.index.ResultWriter;
import org.modeshape.jcr.spi.index.provider.Filter.Results;

/**
 * Utility for building {@link Results index Operation} instances that will use an index to return those {@link NodeKey}s that
 * satisfy a set of criteria.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 */
class Operations {

    protected static final Logger LOGGER = Logger.getLogger(Operations.class);

    protected static final Results EMPTY_RESULTS = new Results() {

        @Override
        public boolean getNextBatch( ResultWriter writer,
                                     int batchSize ) {
            return false;
        }

        @Override
        public void close() {
        }
    };

    protected static final FilterOperation EMPTY_FILTER_OPERATION = new FilterOperation() {
        @Override
        public Results getResults() {
            return EMPTY_RESULTS;
        }

        @Override
        public long estimateCount() {
            return 0;
        }
    };

    /**
     * Create an {@link Results index operation} instance that will use the supplied {@link NavigableMap} (provided by an index)
     * and the {@link Converter} to return all of the {@link NodeKey}s that satisfy the given constraints.
     *
     * @param keysByValue the index's map of values-to-NodeKey; may not be null
     * @param converter the converter; may not be null
     * @param constraints the constraints; may not be null but may be empty if there are no constraints
     * @param variables the bound variables for this query; may not be null but may be empty
     * @return the index operation; never null
     */
    public static <T> FilterOperation createFilter( NavigableMap<T, String> keysByValue,
                                                    Converter<T> converter,
                                                    Collection<Constraint> constraints,
                                                    Map<String, Object> variables ) {
        if (keysByValue.isEmpty()) return EMPTY_FILTER_OPERATION;
        NodeKeysAccessor<T, String> nodeKeysAccessor = new NodeKeysAccessor<T, String>() {
            @Override
            public Iterator<String> getNodeKeys( NavigableMap<T, String> keysByValue ) {
                return keysByValue.values().iterator();
            }

            @Override
            public void addAllTo( NavigableMap<T, String> keysByValue,
                                  Set<String> matchedKeys ) {
                matchedKeys.addAll(keysByValue.values());
            }
        };
        OperationBuilder<T> builder = new BasicOperationBuilder<>(keysByValue, converter, nodeKeysAccessor, variables);
        for (Constraint constraint : constraints) {
            OperationBuilder<T> newBuilder = builder.apply(constraint, false);
            if (newBuilder != null) builder = newBuilder;
        }
        return builder;
    }

    /**
     * Create an {@link Results index operation} instance that will use the supplied {@link NavigableMap} (provided by an
     * enumerated index) and the {@link Converter} to return all of the {@link NodeKey}s that satisfy the given constraints.
     *
     * @param keySetByEnumeratedValue the index's map of NodeKey sets; may not be null
     * @param converter the converter; may not be null
     * @param constraints the constraints; may not be null but may be empty if there are no constraints
     * @param variables the bound variables for this query; may not be null but may be empty
     * @return the index operation; never null
     */
    public static <T> FilterOperation createEnumeratedFilter( NavigableMap<T, Set<String>> keySetByEnumeratedValue,
                                                              Converter<T> converter,
                                                              Collection<Constraint> constraints,
                                                              Map<String, Object> variables ) {
        if (keySetByEnumeratedValue.isEmpty()) return EMPTY_FILTER_OPERATION;
        NodeKeysAccessor<T, Set<String>> nodeKeysAccessor = new NodeKeysAccessor<T, Set<String>>() {
            @Override
            public Iterator<String> getNodeKeys( NavigableMap<T, Set<String>> keysByValue ) {
                if (keysByValue.isEmpty()) return new EmptyIterator<String>();
                if (keysByValue.size() == 1) return keysByValue.values().iterator().next().iterator();
                return MultiIterator.fromIterables(keysByValue.values());
            }

            @Override
            public void addAllTo( NavigableMap<T, Set<String>> keysByValue,
                                  Set<String> matchedKeys ) {
                for (Map.Entry<T, Set<String>> entry : keysByValue.entrySet()) {
                    matchedKeys.addAll(entry.getValue());
                }
            }
        };
        OperationBuilder<T> builder = new BasicOperationBuilder<>(keySetByEnumeratedValue, converter, nodeKeysAccessor, variables);
        for (Constraint constraint : constraints) {
            OperationBuilder<T> newBuilder = builder.apply(constraint, false);
            if (newBuilder != null) builder = newBuilder;
        }
        return builder;
    }

    public static interface FilterOperation {
        Index.Results getResults();

        /**
         * Obtain an estimate of the number of results.
         *
         * @return the estimated number of results; either 0 or a positive number
         */
        long estimateCount();
    }

    /**
     * A builder of {@link Results} instances. Builders are used to apply multiple {@link Constraint}s to an index and to
     * ultimiately {@link #getResults() get} a {@link Results} instance that can be used to obtain the values that satisfy the
     * constraints.
     *
     * @param <T> the type of index key
     * @author Randall Hauch (rhauch@redhat.com)
     */
    protected abstract static class OperationBuilder<T> implements FilterOperation {

        protected OperationBuilder() {
        }

        /**
         * Return a new operation builder that will apply the given constraint to the index, possibly negating the constraint.
         *
         * @param constraint the constraint; may not be null
         * @param negated true if the constraint should be negated, or false if it should be applied as-is
         * @return the builder; never null
         */
        public OperationBuilder<T> apply( Constraint constraint,
                                          boolean negated ) {
            if (constraint instanceof Between) return apply((Between)constraint, negated);
            if (constraint instanceof Comparison) return apply((Comparison)constraint, negated);
            if (constraint instanceof And) {
                And and = (And)constraint;
                return apply(and.getConstraint1(), negated).apply(and.getConstraint2(), negated);
            }
            if (constraint instanceof Or) {
                Or or = (Or)constraint;
                OperationBuilder<T> left = apply(or.getConstraint1(), negated);
                OperationBuilder<T> right = apply(or.getConstraint2(), negated);
                return new DualOperationBuilder<>(left, right);
            }
            if (constraint instanceof Not) {
                Not not = (Not)constraint;
                return apply(not.getConstraint(), !negated);
            }
            if (constraint instanceof PropertyExistence) {
                // Presumably this index only contains values for this property ...
                return this;
            }
            if (constraint instanceof SetCriteria) {
                SetCriteria criteria = (SetCriteria)constraint;
                return apply(criteria, negated);
            }

            // We don't know how to handle any of the other kinds of constraints ...
            LOGGER.debug("Unable to process constraint, so ignoring: {0}", constraint);
            return this;
        }

        protected abstract OperationBuilder<T> apply( Between between,
                                                      boolean negated );

        protected abstract OperationBuilder<T> apply( Comparison comparison,
                                                      boolean negated );

        protected abstract OperationBuilder<T> apply( SetCriteria setCriteria,
                                                      boolean negated );
    }

    protected static interface NodeKeysAccessor<T, V> {
        public Iterator<String> getNodeKeys( NavigableMap<T, V> keysByValue );

        public void addAllTo( NavigableMap<T, V> keysByValue,
                              Set<String> matchedKeys );
    }

    /**
     * This builder operates against the supplied {@link NavigableMap} and for each constraint will find the
     * {@link NavigableMap#subMap(Object, boolean, Object, boolean) submap} that satisfies the constraints.
     *
     * @param <T> the type of index key
     * @param <V> the type of value to be iterated over
     * @author Randall Hauch (rhauch@redhat.com)
     */
    protected static class BasicOperationBuilder<T, V> extends OperationBuilder<T> {
        protected final NavigableMap<T, V> keysByValue;
        protected final Converter<T> converter;
        protected final NodeKeysAccessor<T, V> nodeKeysAccessor;
        protected final Map<String, Object> variables;

        protected BasicOperationBuilder( NavigableMap<T, V> keysByValue,
                                         Converter<T> converter,
                                         NodeKeysAccessor<T, V> nodeKeysAccessor,
                                         Map<String, Object> variables ) {
            this.keysByValue = keysByValue;
            this.converter = converter;
            this.nodeKeysAccessor = nodeKeysAccessor;
            this.variables = variables;
        }

        protected OperationBuilder<T> create( NavigableMap<T, V> keysByValue ) {
            return new BasicOperationBuilder<>(keysByValue, converter, nodeKeysAccessor, variables);
        }

        @Override
        protected OperationBuilder<T> apply( Between between,
                                             boolean negated ) {
            T lower = converter.toLowerValue(between.getLowerBound(), variables);
            T upper = converter.toUpperValue(between.getUpperBound(), variables);
            boolean isLowerIncluded = between.isLowerBoundIncluded();
            boolean isUpperIncluded = between.isUpperBoundIncluded();
            if (negated) {
                OperationBuilder<T> lowerOp = create(keysByValue.headMap(lower, !isLowerIncluded));
                OperationBuilder<T> upperOp = create(keysByValue.tailMap(upper, !isUpperIncluded));
                return new DualOperationBuilder<>(lowerOp, upperOp);
            }
            return create(keysByValue.subMap(lower, isLowerIncluded, upper, isUpperIncluded));
        }

        @Override
        protected OperationBuilder<T> apply( Comparison comparison,
                                             boolean negated ) {
            StaticOperand operand = comparison.getOperand2();
            Operator op = comparison.operator();
            if (negated) op = op.not();
            // Remember that T may be a composite value, and there may be multiple real values and keys for each composite ...
            switch (op) {
                case EQUAL_TO:
                    T lowerValue = converter.toLowerValue(operand, variables);
                    T upperValue = converter.toUpperValue(operand, variables);
                    return create(keysByValue.subMap(lowerValue, true, upperValue, true));
                case GREATER_THAN:
                    T value = converter.toUpperValue(operand, variables);
                    return create(keysByValue.tailMap(value, false));
                case GREATER_THAN_OR_EQUAL_TO:
                    value = converter.toLowerValue(operand, variables);
                    return create(keysByValue.tailMap(value, true));
                case LESS_THAN:
                    value = converter.toLowerValue(operand, variables);
                    return create(keysByValue.headMap(value, false));
                case LESS_THAN_OR_EQUAL_TO:
                    value = converter.toUpperValue(operand, variables);
                    return create(keysByValue.headMap(value, true));
                case NOT_EQUAL_TO:
                    OperationBuilder<T> lowerOp = create(keysByValue.headMap(converter.toLowerValue(operand, variables), false));
                    OperationBuilder<T> upperOp = create(keysByValue.tailMap(converter.toUpperValue(operand, variables), false));
                    return new DualOperationBuilder<>(lowerOp, upperOp);
                case LIKE:
                    // We can't handle LIKE with this kind of index, but we can return the complete list of node keys
                    // that have properties matching this index, and the LIKE can be done higher up. This might not very useful,
                    // but more than likely we're going to know the subset of nodes with this property better than any other
                    // index except somethiing that can actually handle LIKE. So we won't filter, and we'll just return this
                    // builder...
                    break;
            }
            return this;
        }

        @Override
        protected OperationBuilder<T> apply( SetCriteria setCriteria,
                                             boolean negated ) {
            return new SetOperationBuilder<>(keysByValue, converter, nodeKeysAccessor, variables, setCriteria, negated);
        }

        protected Iterator<String> keys() {
            return nodeKeysAccessor.getNodeKeys(keysByValue);
        }

        @Override
        public Results getResults() {
            final Iterator<String> filteredKeys = keys();
            final float score = 1.0f;
            return new Results() {
                @Override
                public boolean getNextBatch( ResultWriter writer,
                                             int batchSize ) {
                    int count = 0;
                    while (count < batchSize && filteredKeys.hasNext()) {
                        writer.add(new NodeKey(filteredKeys.next()), score);
                        ++count;
                    }
                    return filteredKeys.hasNext();
                }

                @Override
                public void close() {
                    // Nothing to do ...
                }
            };
        }

        @Override
        public long estimateCount() {
            return keysByValue.size();
        }
    }

    /**
     * This builder results in an operation that performs a positive or negative match against a set criteria containing known
     * values.
     *
     * @param <T> the type of index key
     * @param <V> the type of value to be iterated over
     * @author Randall Hauch (rhauch@redhat.com)
     */
    protected static class SetOperationBuilder<T, V> extends BasicOperationBuilder<T, V> {
        private final SetCriteria criteria;
        private final boolean negated;

        protected SetOperationBuilder( NavigableMap<T, V> keysByValue,
                                       IndexValues.Converter<T> converter,
                                       NodeKeysAccessor<T, V> nodeKeysAccessor,
                                       Map<String, Object> variables,
                                       SetCriteria criteria,
                                       boolean negated ) {
            super(keysByValue, converter, nodeKeysAccessor, variables);
            this.criteria = criteria;
            this.negated = negated;
        }

        @Override
        protected OperationBuilder<T> create( NavigableMap<T, V> keysByValue ) {
            return new SetOperationBuilder<>(keysByValue, converter, nodeKeysAccessor, variables, criteria, negated);
        }

        @Override
        protected OperationBuilder<T> apply( SetCriteria setCriteria,
                                             boolean negated ) {
            throw new UnsupportedOperationException("Can't evaluate two SetCriteria that are not ANDed or ORed together");
        }

        private void addValues( StaticOperand valueOperand,
                                Set<String> matchedKeys ) {
            if (valueOperand instanceof BindVariableName) {
                // We have to resolve the variable ...
                BindVariableName varName = (BindVariableName)valueOperand;
                String varNameStr = varName.getBindVariableName();
                Object varValue = this.variables.get(varNameStr);
                if (varValue instanceof Collection) {
                    Collection<?> collection = (Collection<?>)varValue;
                    for (Object value : collection) {
                        StaticOperand operand = new Literal(value);
                        addValues(operand, matchedKeys);
                    }
                } else {
                    StaticOperand operand = new Literal(varValue);
                    addValues(operand, matchedKeys);
                }
                // Not a value we know what to do with ...
                return;
            }

            T lowValue = converter.toLowerValue(valueOperand, variables);
            T highValue = converter.toUpperValue(valueOperand, variables);
            NavigableMap<T, V> submap = null;
            if (lowValue == null) {
                if (highValue == null) return;
                // High but not low ...
                submap = keysByValue.headMap(highValue, true);
            } else {
                if (highValue == null) {
                    // Low but not high ...
                    submap = keysByValue.tailMap(lowValue, true);
                } else {
                    // Both high and low ...
                    submap = keysByValue.subMap(lowValue, true, highValue, true);
                }
            }
            if (submap.isEmpty()) return; // no values for these keys
            nodeKeysAccessor.addAllTo(submap, matchedKeys);
        }

        @Override
        protected Iterator<String> keys() {
            // Determine the set of keys that have a value in our set ...
            final Set<String> matchedKeys = new HashSet<>();
            for (StaticOperand valueOperand : criteria.getValues()) {
                // Find the range of all keys that have this value ...
                addValues(valueOperand, matchedKeys);
            }

            if (!negated) {
                // We've already found all of the keys for the given value, so just return them ...
                return matchedKeys.iterator();
            }

            // Otherwise, we're supposed to find all of the keys that are NOT in the set ...
            final Iterator<String> allKeys = super.keys();
            if (matchedKeys.isEmpty()) {
                // Our key set is empty, so none are to be excluded...
                return allKeys;
            }

            // Otherwise, we'll return an iterator that just wraps the 'allKeys' iterator and skips any key in our set ...
            return new Iterator<String>() {
                private String next;

                @Override
                public boolean hasNext() {
                    return findNext();
                }

                @Override
                public String next() {
                    if (findNext()) {
                        assert next != null;
                        String result = next;
                        next = null;
                        return result;
                    }
                    throw new NoSuchElementException();
                }

                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }

                private boolean findNext() {
                    if (next != null) return true;
                    while (allKeys.hasNext()) {
                        String possible = allKeys.next();
                        if (matchedKeys.contains(possible)) continue; // it is to be excluded
                        next = possible;
                        return true;
                    }
                    return false;
                }
            };
        }

        @Override
        public long estimateCount() {
            long count = 0L;
            // Determine the set of keys that have a value in our set ...
            for (StaticOperand valueOperand : criteria.getValues()) {
                // Find the range of all keys that have this value ...
                T lowValue = converter.toLowerValue(valueOperand, variables);
                T highValue = converter.toUpperValue(valueOperand, variables);
                NavigableMap<T, V> submap = null;
                if (lowValue == null) {
                    if (highValue == null) continue;
                    // High but not low ...
                    submap = keysByValue.headMap(highValue, true);
                } else {
                    if (highValue == null) {
                        // Low but not high ...
                        submap = keysByValue.tailMap(lowValue, true);
                    } else {
                        // Both high and low ...
                        submap = keysByValue.subMap(lowValue, true, highValue, true);
                    }
                }
                count += submap.size();
            }

            if (!negated) {
                // We've already found all of the keys for the given value, so just return them ...
                count = keysByValue.size() - count;
            }
            return Math.max(count, 0L);
        }
    }

    /**
     * This builder delegates to both sides, and is used for {@link Or} constraints.
     *
     * @param <T> the type of index key
     * @author Randall Hauch (rhauch@redhat.com)
     */
    protected static class DualOperationBuilder<T> extends OperationBuilder<T> {
        protected final OperationBuilder<T> left;
        protected final OperationBuilder<T> right;

        protected DualOperationBuilder( OperationBuilder<T> left,
                                        OperationBuilder<T> right ) {
            this.left = left;
            this.right = right;
        }

        @Override
        protected OperationBuilder<T> apply( Between between,
                                             boolean negated ) {
            OperationBuilder<T> left = this.left.apply(between, negated);
            OperationBuilder<T> right = this.right.apply(between, negated);
            return new DualOperationBuilder<>(left, right);
        }

        @Override
        protected OperationBuilder<T> apply( Comparison comparison,
                                             boolean negated ) {
            OperationBuilder<T> left = this.left.apply(comparison, negated);
            OperationBuilder<T> right = this.right.apply(comparison, negated);
            return new DualOperationBuilder<>(left, right);
        }

        @Override
        protected OperationBuilder<T> apply( SetCriteria setCriteria,
                                             boolean negated ) {
            OperationBuilder<T> left = this.left.apply(setCriteria, negated);
            OperationBuilder<T> right = this.right.apply(setCriteria, negated);
            return new DualOperationBuilder<>(left, right);
        }

        @Override
        public Results getResults() {
            final Results first = left.getResults();
            return new Results() {
                Results second = null;

                @Override
                public boolean getNextBatch( ResultWriter writer,
                                             int batchSize ) {
                    if (first.getNextBatch(writer, batchSize)) {
                        return true;
                    }
                    // Otherwise, the first one is done so we have to get the second one ...
                    if (second == null) {
                        second = right.getResults();
                    }
                    return second.getNextBatch(writer, batchSize);
                }

                @Override
                public void close() {
                    // Nothing to do ...
                }
            };
        }

        @Override
        public long estimateCount() {
            return left.estimateCount() + right.estimateCount();
        }
    }

    private Operations() {
    }

}
