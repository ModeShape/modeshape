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
package org.modeshape.jcr.query.validate;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.collection.Collections;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.query.validate.Schemata.Column;
import org.modeshape.jcr.value.PropertyType;

@Immutable
public class ImmutableColumn implements Column {

    public static final Set<Operator> ALL_OPERATORS = Collections.unmodifiableSet(EnumSet.allOf(Operator.class));
    public static final Set<Operator> NO_OPERATORS = Collections.unmodifiableSet(EnumSet.noneOf(Operator.class));

    public static final PropertyType DEFAULT_REQUIRED_TYPE = PropertyType.STRING;
    public static final boolean DEFAULT_FULL_TEXT_SEARCHABLE = false;
    public static final boolean DEFAULT_ORDERABLE = true;

    private final boolean fullTextSearchable;
    private final boolean orderable;
    private final boolean comparable;
    private final String name;
    private final String typeName;
    private final PropertyType requiredType;
    private final Set<Operator> operators;
    private final Object minimumValue;
    private final Object maximumValue;

    protected ImmutableColumn( String name,
                               String type,
                               PropertyType requiredType ) {
        this(name, type, requiredType, DEFAULT_FULL_TEXT_SEARCHABLE, DEFAULT_ORDERABLE, null, null, ALL_OPERATORS);
    }

    protected ImmutableColumn( String name,
                               String type ) {
        this(name, type, DEFAULT_REQUIRED_TYPE, DEFAULT_FULL_TEXT_SEARCHABLE, DEFAULT_ORDERABLE, null, null, ALL_OPERATORS);
    }

    protected ImmutableColumn( String name,
                               String type,
                               PropertyType requiredType,
                               boolean fullTextSearchable ) {
        this(name, type, requiredType, fullTextSearchable, DEFAULT_ORDERABLE, null, null, ALL_OPERATORS);
    }

    protected ImmutableColumn( String name,
                               String type,
                               PropertyType requiredType,
                               boolean fullTextSearchable,
                               boolean orderable,
                               boolean canContainReferences,
                               Object minimum,
                               Object maximum,
                               Operator... operators ) {
        this(name, type, requiredType, fullTextSearchable, orderable, minimum, maximum,
             operators != null && operators.length != 0 ? EnumSet.copyOf(Arrays.asList(operators)) : null);
    }

    protected ImmutableColumn( String name,
                               String type,
                               PropertyType requiredType,
                               boolean fullTextSearchable,
                               boolean orderable,
                               Object minimum,
                               Object maximum,
                               Set<Operator> operators ) {
        this.name = name;
        this.typeName = type;
        this.requiredType = requiredType;
        this.fullTextSearchable = fullTextSearchable;
        this.orderable = orderable;
        this.operators = operators == null || operators.isEmpty() ? ALL_OPERATORS : Collections.unmodifiableSet(EnumSet.copyOf(operators));
        this.minimumValue = minimum;
        this.maximumValue = maximum;
        if (this.operators.isEmpty()) {
            this.comparable = false;
        } else {
            boolean comparable = this.requiredType == PropertyType.STRING;
            if (!comparable) {
                // See if the operators expect comparable ...
                for (Operator operator : this.operators) {
                    if (operator == Operator.GREATER_THAN || operator == Operator.GREATER_THAN_OR_EQUAL_TO
                        || operator == Operator.LESS_THAN || operator == Operator.LESS_THAN_OR_EQUAL_TO) {
                        comparable = true;
                        break;
                    }
                }
            }
            this.comparable = comparable;
        }
        assert this.name != null;
        assert this.typeName != null;
        assert this.operators != null;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getPropertyTypeName() {
        return typeName;
    }

    @Override
    public boolean isFullTextSearchable() {
        return fullTextSearchable;
    }

    @Override
    public boolean isOrderable() {
        return orderable;
    }

    @Override
    public boolean isComparable() {
        return comparable;
    }

    @Override
    public Object getMinimum() {
        return minimumValue;
    }

    @Override
    public Object getMaximum() {
        return maximumValue;
    }

    @Override
    public PropertyType getRequiredType() {
        return requiredType;
    }

    @Override
    public Set<Operator> getOperators() {
        return operators;
    }

    @Override
    public String toString() {
        return this.name + "(" + typeName + ")";
    }
}
