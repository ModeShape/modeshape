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
import java.util.NavigableMap;
import java.util.NoSuchElementException;
import java.util.Set;
import javax.jcr.query.qom.And;
import javax.jcr.query.qom.Constraint;
import javax.jcr.query.qom.Not;
import javax.jcr.query.qom.Or;
import javax.jcr.query.qom.PropertyExistence;
import javax.jcr.query.qom.StaticOperand;
import org.modeshape.common.logging.Logger;
import org.modeshape.jcr.api.query.qom.Between;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.api.query.qom.SetCriteria;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.index.local.IndexValues.Converter;
import org.modeshape.jcr.query.model.Comparison;
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

    /**
     * Create an {@link Results index operation} instance that will use the supplied {@link NavigableMap} (provided by an index)
     * and the {@link Converter} to return all of the {@link NodeKey}s that satisfy the given constraints.
     * 
     * @param keysByValue the index's map of values-to-NodeKey; may not be null
     * @param converter the converter; may not be null
     * @param constraints the constraints; may not be null but may be empty if there are no constraints
     * @return the index operation; never null
     */
    public static <T> Results createOperation( NavigableMap<T, String> keysByValue,
                                               Converter<T> converter,
                                               Collection<Constraint> constraints ) {
        OperationBuilder<T> builder = new BasicOperationBuilder<>(keysByValue, converter);
        for (Constraint constraint : constraints) {
            OperationBuilder<T> newBuilder = builder.apply(constraint, false);
            if (newBuilder != null) builder = newBuilder;
        }
        return builder.build();
    }

    /**
     * A builder of {@link Results} instances. Builders are used to apply multiple {@link Constraint}s to an index and to
     * ultimiately {@link #build() build} an {@link Results} instance that can be used to obtain the values that satisfy the
     * constraints.
     * 
     * @param <T> the type of index key
     * @author Randall Hauch (rhauch@redhat.com)
     */
    protected abstract static class OperationBuilder<T> {

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

        protected abstract Index.Results build();
    }

    /**
     * This builder operates against the supplied {@link NavigableMap} and for each constraint will find the
     * {@link NavigableMap#subMap(Object, boolean, Object, boolean) submap} that satisfies the constraints.
     * 
     * @param <T> the type of index key
     * @author Randall Hauch (rhauch@redhat.com)
     */
    protected static class BasicOperationBuilder<T> extends OperationBuilder<T> {
        protected final NavigableMap<T, String> keysByValue;
        protected final Converter<T> converter;

        protected BasicOperationBuilder( NavigableMap<T, String> keysByValue,
                                         Converter<T> converter ) {
            this.keysByValue = keysByValue;
            this.converter = converter;
        }

        protected OperationBuilder<T> create( NavigableMap<T, String> keysByValue ) {
            return new BasicOperationBuilder<>(keysByValue, converter);
        }

        @Override
        protected OperationBuilder<T> apply( Between between,
                                             boolean negated ) {
            T lower = converter.toLowerValue(between.getLowerBound());
            T upper = converter.toUpperValue(between.getUpperBound());
            boolean isLowerIncluded = between.isLowerBoundIncluded();
            boolean isUpperIncluded = between.isUpperBoundIncluded();
            if (negated) {
                OperationBuilder<T> lowerOp = create(keysByValue.headMap(lower, isLowerIncluded));
                OperationBuilder<T> upperOp = create(keysByValue.tailMap(upper, isUpperIncluded));
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
                    T lowerValue = converter.toLowerValue(operand);
                    T upperValue = converter.toUpperValue(operand);
                    return create(keysByValue.subMap(lowerValue, true, upperValue, true));
                case GREATER_THAN:
                    T value = converter.toUpperValue(operand);
                    return create(keysByValue.tailMap(value, false));
                case GREATER_THAN_OR_EQUAL_TO:
                    value = converter.toUpperValue(operand);
                    return create(keysByValue.tailMap(value, true));
                case LESS_THAN:
                    value = converter.toLowerValue(operand);
                    return create(keysByValue.headMap(value, false));
                case LESS_THAN_OR_EQUAL_TO:
                    value = converter.toLowerValue(operand);
                    return create(keysByValue.headMap(value, true));
                case NOT_EQUAL_TO:
                    OperationBuilder<T> lowerOp = create(keysByValue.headMap(converter.toLowerValue(operand), false));
                    OperationBuilder<T> upperOp = create(keysByValue.tailMap(converter.toUpperValue(operand), false));
                    return new DualOperationBuilder<>(lowerOp, upperOp);
                case LIKE:
                    // We can't handle LIKE with this kind of index. If we throw an exception, we'd have to remove any LIKE
                    // criteria from the list of criteria sent to this operation. Instead, we simply ignore it.
                    // Regardless, ModeShape should only pass the criteria that our IndexPlanner accepts for this part of the
                    // query, and our IndexPlanner won't accept LIKE criteria.
                    break;
            }
            return this;
        }

        @Override
        protected OperationBuilder<T> apply( SetCriteria setCriteria,
                                             boolean negated ) {
            return new SetOperationBuilder<>(keysByValue, converter, setCriteria, negated);
        }

        protected Iterator<String> keys() {
            return keysByValue.values().iterator();
        }

        @Override
        protected Results build() {
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
    }

    /**
     * This builder results in an operation that performs a positive or negative match against a set criteria containing known
     * values.
     * 
     * @param <T> the type of index key
     * @author Randall Hauch (rhauch@redhat.com)
     */
    protected static class SetOperationBuilder<T> extends BasicOperationBuilder<T> {
        private final SetCriteria criteria;
        private final boolean negated;

        protected SetOperationBuilder( NavigableMap<T, String> keysByValue,
                                       IndexValues.Converter<T> converter,
                                       SetCriteria criteria,
                                       boolean negated ) {
            super(keysByValue, converter);
            this.criteria = criteria;
            this.negated = negated;
        }

        @Override
        protected OperationBuilder<T> create( NavigableMap<T, String> keysByValue ) {
            return new SetOperationBuilder<>(keysByValue, converter, criteria, negated);
        }

        @Override
        protected OperationBuilder<T> apply( SetCriteria setCriteria,
                                             boolean negated ) {
            throw new UnsupportedOperationException("Can't evaluate two SetCriteria that are not ANDed or ORed together");
        }

        @Override
        protected Iterator<String> keys() {
            // Determine the set of keys that have a value in our set ...
            final Set<String> matchedKeys = new HashSet<>();
            for (StaticOperand valueOperand : criteria.getValues()) {
                // Find the range of all keys that have this value ...
                T lowValue = converter.toLowerValue(valueOperand);
                T highValue = converter.toUpperValue(valueOperand);
                NavigableMap<T, String> submap = null;
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
                if (submap.isEmpty()) continue; // no values for these keys
                matchedKeys.addAll(submap.values());
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
        protected Results build() {
            final Results first = left.build();
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
                        second = right.build();
                    }
                    return second.getNextBatch(writer, batchSize);
                }

                @Override
                public void close() {
                    // Nothing to do ...
                }
            };
        }
    }

    private Operations() {
    }

}
