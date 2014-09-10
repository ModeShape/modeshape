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

import java.util.Map;
import javax.jcr.query.qom.BindVariableValue;
import javax.jcr.query.qom.StaticOperand;
import org.modeshape.jcr.api.index.IndexDefinition.IndexKind;
import org.modeshape.jcr.index.local.MapDB.UniqueKey;
import org.modeshape.jcr.query.model.Literal;
import org.modeshape.jcr.value.ValueFactory;

/**
 * Utilities for working with individual values.
 *
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class IndexValues {

    /**
     * Converts between {@link StaticOperand} and an index's key type.
     * <p>
     * Note that {@link IndexKind#VALUE} indexes may have multiple node keys for a given value, which means that any given value
     * might have a range of keys. Some MapDB-based indexes store such entries using an index key that consists of the actual
     * property value plus a counter. In this way, there are multiple index keys for a given {@link StaticOperand}, and the
     * {@link #toLowerValue} will return the lowest-possible index key given the {@link StaticOperand}, while the
     * {@link #toUpperValue} will return the highest-possible index key for the given {@link StaticOperand}
     * </p>
     * <p>
     * {@link IndexKind#UNIQUE_VALUE} indexes will use the actual property value as the index key, and thus the
     * {@link #toLowerValue} and {@link #toUpperValue} methods will both return the same index key for the same
     * {@link StaticOperand}.
     *
     * @param <T> the index's key type
     * @author Randall Hauch (rhauch@redhat.com)
     */
    public static interface Converter<T> {

        /**
         * Create the lowest possible index key for the supplied {@link StaticOperand}.
         *
         * @param operand the static operand; may be null
         * @param variables the bound variables; may not be null but may be empty
         * @return the lowest/smallest possible index key; or null if the operand is null
         */
        public T toLowerValue( StaticOperand operand,
                               Map<String, Object> variables );

        /**
         * Create the highest possible index key for the supplied {@link StaticOperand}.
         *
         * @param operand the static operand; may be null
         * @param variables the bound variables; may not be null but may be empty
         * @return the highest/largest possible index key; or null if the operand is null
         */
        T toUpperValue( StaticOperand operand,
                        Map<String, Object> variables );
    }

    public static <T> Converter<UniqueKey<T>> uniqueKeyConverter( Converter<T> converter ) {
        return new UniqueKeyConverter<T>(converter);
    }

    public static <T> Converter<T> converter( ValueFactory<T> factory ) {
        return new StandardConverter<T>(factory);
    }

    static final class UniqueKeyConverter<T> implements Converter<UniqueKey<T>> {
        private final Converter<T> valueConverter;

        protected UniqueKeyConverter( Converter<T> valueConverter ) {
            this.valueConverter = valueConverter;
        }

        @Override
        public UniqueKey<T> toLowerValue( StaticOperand operand,
                                          Map<String, Object> variables ) {
            T value = valueConverter.toLowerValue(operand, variables);
            return value != null ? new UniqueKey<T>(value, 0L) : null;
        }

        @Override
        public UniqueKey<T> toUpperValue( StaticOperand operand,
                                          Map<String, Object> variables ) {
            T value = valueConverter.toLowerValue(operand, variables);
            return value != null ? new UniqueKey<T>(value, Long.MAX_VALUE) : null;
        }
    }

    protected static class StandardConverter<T> implements Converter<T> {
        private final ValueFactory<T> factory;

        protected StandardConverter( ValueFactory<T> factory ) {
            this.factory = factory;
        }

        @Override
        public T toLowerValue( StaticOperand operand,
                               Map<String, Object> variables ) {
            if (operand instanceof Literal) {
                Literal literal = (Literal)operand;
                return factory.create(literal.value());
            }
            if (operand instanceof BindVariableValue) {
                BindVariableValue bind = (BindVariableValue)operand;
                return factory.create(variables.get(bind.getBindVariableName()));
            }
            throw new LocalIndexException("Unexpected static operand: " + operand);
        }

        @Override
        public T toUpperValue( StaticOperand operand,
                               Map<String, Object> variables ) {
            if (operand instanceof Literal) {
                Literal literal = (Literal)operand;
                return factory.create(literal.value());
            }
            if (operand instanceof BindVariableValue) {
                BindVariableValue bind = (BindVariableValue)operand;
                return factory.create(variables.get(bind.getBindVariableName()));
            }
            throw new LocalIndexException("Unexpected static operand: " + operand);
        }

    }

    private IndexValues() {
    }

}
