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
package org.modeshape.jcr;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.api.query.qom.Operator;
import org.modeshape.jcr.api.query.qom.QueryObjectModelConstants;
import org.modeshape.jcr.api.value.DateTime;
import org.modeshape.jcr.cache.NodeKey;
import org.modeshape.jcr.value.Name;
import org.modeshape.jcr.value.NameFactory;
import org.modeshape.jcr.value.NamespaceRegistry;
import org.modeshape.jcr.value.Path;
import org.modeshape.jcr.value.PathFactory;
import org.modeshape.jcr.value.Property;
import org.modeshape.jcr.value.StringFactory;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;
import org.modeshape.jcr.value.ValueFormatException;
import org.modeshape.jcr.value.basic.JodaDateTime;

/**
 * ModeShape implementation of the {@link PropertyDefinition} interface. This implementation is immutable and has all fields
 * initialized through its constructor.
 */
@Immutable
class JcrPropertyDefinition extends JcrItemDefinition implements PropertyDefinition {

    protected static final Map<String, Operator> OPERATORS_BY_JCR_NAME;

    static {
        Map<String, Operator> map = new HashMap<>();
        map.put(QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, Operator.EQUAL_TO);
        map.put(QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN, Operator.GREATER_THAN);
        map.put(QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO, Operator.GREATER_THAN_OR_EQUAL_TO);
        map.put(QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN, Operator.LESS_THAN);
        map.put(QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO, Operator.LESS_THAN_OR_EQUAL_TO);
        map.put(QueryObjectModelConstants.JCR_OPERATOR_LIKE, Operator.LIKE);
        map.put(QueryObjectModelConstants.JCR_OPERATOR_NOT_EQUAL_TO, Operator.NOT_EQUAL_TO);
        OPERATORS_BY_JCR_NAME = Collections.unmodifiableMap(map);
    }

    static Operator operatorFromSymbol( String jcrConstantValue ) {
        Operator op = OPERATORS_BY_JCR_NAME.get(jcrConstantValue);
        if (op == null) op = Operator.forSymbol(jcrConstantValue);
        assert op != null;
        return op;
    }

    private final Object[] rawDefaultValues;
    private final JcrValue[] defaultValues;
    private final int requiredType;
    private final String[] valueConstraints;
    private final boolean multiple;
    private final boolean fullTextSearchable;
    private final boolean queryOrderable;
    private final String[] queryOperators;
    private final NodeKey key;
    private final PropertyDefinitionId id;
    private ConstraintChecker checker = null;

    JcrPropertyDefinition( ExecutionContext context,
                           JcrNodeType declaringNodeType,
                           NodeKey prototypeKey,
                           Name name,
                           int onParentVersion,
                           boolean autoCreated,
                           boolean mandatory,
                           boolean protectedItem,
                           JcrValue[] defaultValues,
                           int requiredType,
                           String[] valueConstraints,
                           boolean multiple,
                           boolean fullTextSearchable,
                           boolean queryOrderable,
                           String[] queryOperators ) {
        super(context, declaringNodeType, name, onParentVersion, autoCreated, mandatory, protectedItem);
        this.defaultValues = defaultValues;
        this.requiredType = requiredType;
        this.valueConstraints = valueConstraints;
        this.multiple = multiple;
        this.fullTextSearchable = fullTextSearchable;
        this.queryOrderable = queryOrderable;
        this.queryOperators = queryOperators != null ? queryOperators : new String[] {
            QueryObjectModelConstants.JCR_OPERATOR_EQUAL_TO, QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN,
            QueryObjectModelConstants.JCR_OPERATOR_GREATER_THAN_OR_EQUAL_TO, QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN,
            QueryObjectModelConstants.JCR_OPERATOR_LESS_THAN_OR_EQUAL_TO, QueryObjectModelConstants.JCR_OPERATOR_LIKE,
            QueryObjectModelConstants.JCR_OPERATOR_NOT_EQUAL_TO};
        assert this.valueConstraints != null;
        this.id = this.declaringNodeType == null ? null : new PropertyDefinitionId(this.declaringNodeType.getInternalName(),
                                                                                   this.name, this.requiredType, this.multiple);
        this.key = this.id == null ? prototypeKey : prototypeKey.withId("/jcr:system/jcr:nodeTypes/" + this.id.getString());

        if (this.defaultValues != null) {
            this.rawDefaultValues = new Object[this.defaultValues.length];
            int i = 0;
            for (JcrValue defaultValue : this.defaultValues) {
                rawDefaultValues[i++] = defaultValue.value();
            }
        } else {
            this.rawDefaultValues = null;
        }
    }

    /**
     * Get the durable identifier for this property definition.
     * 
     * @return the property definition ID; never null
     */
    public PropertyDefinitionId getId() {
        return id;
    }

    @Override
    final NodeKey key() {
        return key;
    }

    @Override
    public JcrValue[] getDefaultValues() {
        return defaultValues;
    }

    /**
     * Get the default values array consisting of values that can be placed inside {@link Property} instances.
     * 
     * @return the default values, or null if there are none
     */
    Object[] getRawDefaultValues() {
        return rawDefaultValues;
    }

    /**
     * Return whether this definition has default values.
     * 
     * @return true if there default values, or false otherwise
     */
    public boolean hasDefaultValues() {
        return defaultValues != null;
    }

    @Override
    public int getRequiredType() {
        return requiredType;
    }

    @Override
    public String[] getValueConstraints() {
        return valueConstraints;
    }

    @Override
    public boolean isMultiple() {
        return multiple;
    }

    @Override
    public boolean isFullTextSearchable() {
        return fullTextSearchable;
    }

    @Override
    public boolean isQueryOrderable() {
        return queryOrderable;
    }

    @Override
    public String[] getAvailableQueryOperators() {
        return queryOperators;
    }

    /**
     * Creates a new <code>JcrPropertyDefinition</code> that is identical to the current object, but with the given
     * <code>declaringNodeType</code>. Provided to support immutable pattern for this class.
     * 
     * @param declaringNodeType the declaring node type for the new <code>JcrPropertyDefinition</code>
     * @return a new <code>JcrPropertyDefinition</code> that is identical to the current object, but with the given
     *         <code>declaringNodeType</code>.
     */
    JcrPropertyDefinition with( JcrNodeType declaringNodeType ) {
        return new JcrPropertyDefinition(this.context, declaringNodeType, key(), this.name, this.getOnParentVersion(),
                                         this.isAutoCreated(), this.isMandatory(), this.isProtected(), this.getDefaultValues(),
                                         this.getRequiredType(), this.getValueConstraints(), this.isMultiple(),
                                         this.isFullTextSearchable(), this.isQueryOrderable(), this.getAvailableQueryOperators());
    }

    /**
     * Creates a new <code>JcrPropertyDefinition</code> that is identical to the current object, but with the given
     * <code>context</code>. Provided to support immutable pattern for this class.
     * 
     * @param context the {@link ExecutionContext} for the new <code>JcrPropertyDefinition</code>
     * @return a new <code>JcrPropertyDefinition</code> that is identical to the current object, but with the given
     *         <code>context</code>.
     */
    JcrPropertyDefinition with( ExecutionContext context ) {
        return new JcrPropertyDefinition(context, this.declaringNodeType, key(), this.name, this.getOnParentVersion(),
                                         this.isAutoCreated(), this.isMandatory(), this.isProtected(), this.getDefaultValues(),
                                         this.getRequiredType(), this.getValueConstraints(), this.isMultiple(),
                                         this.isFullTextSearchable(), this.isQueryOrderable(), this.getAvailableQueryOperators());
    }

    @Override
    public String toString() {
        ValueFactory<String> strings = context.getValueFactories().getStringFactory();
        StringBuilder sb = new StringBuilder();
        PropertyDefinitionId id = getId();
        sb.append(strings.create(id.getNodeTypeName()));
        sb.append('/');
        sb.append(strings.create(id.getPropertyDefinitionName()));
        sb.append('/');
        sb.append(org.modeshape.jcr.api.PropertyType.nameFromValue(id.getPropertyType()));
        sb.append(id.allowsMultiple() ? '*' : '1');
        return sb.toString();
    }

    boolean satisfiesConstraints( Value value,
                                  JcrSession session ) {
        if (value == null) return false;
        if (valueConstraints == null || valueConstraints.length == 0) {
            return true;
        }

        // Neither the 1.0 or 2.0 specification formally prohibit constraints on properties with no required type.
        int type = requiredType == PropertyType.UNDEFINED ? value.getType() : requiredType;

        /*
         * Keep a method-local reference to the constraint checker in case another thread attempts to concurrently
         * check the constraints with a different required type.
         */
        ConstraintChecker checker = this.checker;

        if (checker == null || checker.getType() != type) {
            checker = createChecker(context, type, valueConstraints);
            this.checker = checker;
        }

        try {
            return checker.matches(value, session);
        } catch (ValueFormatException vfe) {
            // The value was so wonky that we couldn't even convert it to an appropriate type
            return false;
        }
    }

    boolean satisfiesConstraints( Value[] values,
                                  JcrSession session ) {
        if (valueConstraints == null || valueConstraints.length == 0) {
            if (requiredType != PropertyType.UNDEFINED) {
                for (Value value : values) {
                    if (value.getType() != requiredType) return false;
                }
            }
            return true;
        }
        if (values == null || values.length == 0) {
            // There are no values, so see if the definition allows multiple values ...
            return isMultiple();
        }

        // Neither the 1.0 or 2.0 specification formally prohibit constraints on properties with no required type.
        int type = requiredType == PropertyType.UNDEFINED ? values[0].getType() : requiredType;

        /*
         * Keep a method-local reference to the constraint checker in case another thread attempts to concurrently
         * check the constraints with a different required type.
         */
        ConstraintChecker checker = this.checker;

        if (checker == null || checker.getType() != type) {
            checker = createChecker(context, type, valueConstraints);
            this.checker = checker;
        }

        try {
            for (Value value : values) {
                if (requiredType != PropertyType.UNDEFINED && value.getType() != requiredType) return false;
                if (!checker.matches(value, session)) return false;
            }
            return true;
        } catch (ValueFormatException vfe) {
            // The value was so wonky that we couldn't even convert it to an appropriate type
            return false;
        }
    }

    /**
     * Return the minimum value allowed by the constraints, or null if no such minimum value is defined by the definition given
     * it's required type and constraints. A minimum value can only be found for numeric types, such as {@link PropertyType#DATE
     * DATE}, {@link PropertyType#LONG LONG}, {@link PropertyType#DOUBLE DOUBLE}, and {@link PropertyType#DECIMAL DECIMAL}; all
     * other types will return null.
     * 
     * @return the minimum value, or null if no minimum value could be identified
     */
    Object getMinimumValue() {
        if (requiredType == PropertyType.DATE || requiredType == PropertyType.DOUBLE || requiredType == PropertyType.LONG
            || requiredType == PropertyType.DECIMAL) {
            ConstraintChecker checker = this.checker;
            if (checker == null || checker.getType() != requiredType) {
                checker = createChecker(context, requiredType, valueConstraints);
                this.checker = checker;
            }
            assert checker instanceof RangeConstraintChecker;
            RangeConstraintChecker<?> rangeChecker = (RangeConstraintChecker<?>)checker;
            return rangeChecker.getMinimum(); // may still be null
        }
        return null;
    }

    /**
     * Return the maximum value allowed by the constraints, or null if no such maximum value is defined by the definition given
     * it's required type and constraints. A maximum value can only be found for numeric types, such as {@link PropertyType#DATE
     * DATE}, {@link PropertyType#LONG LONG}, {@link PropertyType#DOUBLE DOUBLE}, and {@link PropertyType#DECIMAL DECIMAL}; all
     * other types will return null.
     * 
     * @return the maximum value, or null if no maximum value could be identified
     */
    Object getMaximumValue() {
        if (requiredType == PropertyType.DATE || requiredType == PropertyType.DOUBLE || requiredType == PropertyType.LONG
            || requiredType == PropertyType.DECIMAL) {
            ConstraintChecker checker = this.checker;
            if (checker == null || checker.getType() != requiredType) {
                checker = createChecker(context, requiredType, valueConstraints);
                this.checker = checker;
            }
            assert checker instanceof RangeConstraintChecker;
            RangeConstraintChecker<?> rangeChecker = (RangeConstraintChecker<?>)checker;
            return rangeChecker.getMaximum(); // may still be null
        }
        return null;
    }

    /**
     * Returns <code>true</code> if <code>value</code> can be cast to <code>property.getRequiredType()</code> per the type
     * conversion rules in section 3.6.4 of the JCR 2.0 specification. If the property definition has a required type of
     * {@link PropertyType#UNDEFINED}, the cast will be considered to have succeeded.
     * 
     * @param value the value to be validated
     * @return <code>true</code> if the value can be cast to the required type for the property definition (if it exists).
     */
    boolean canCastToType( Value value ) {
        try {
            assert value instanceof JcrValue : "Illegal implementation of Value interface";
            ((JcrValue)value).asType(getRequiredType()); // throws ValueFormatException if there's a problem
            return true;
        } catch (javax.jcr.ValueFormatException vfe) {
            // Cast failed
            return false;
        }
    }

    /**
     * Returns <code>true</code> if <code>value</code> can be cast to <code>property.getRequiredType()</code> per the type
     * conversion rules in section 3.6.4 of the JCR 2.0 specification. If the property definition has a required type of
     * {@link PropertyType#UNDEFINED}, the cast will be considered to have succeeded.
     * 
     * @param values the values to be validated
     * @return <code>true</code> if the value can be cast to the required type for the property definition (if it exists).
     */
    boolean canCastToType( Value[] values ) {
        for (Value value : values) {
            if (!canCastToType(value)) return false;
        }
        return true;
    }

    /**
     * Returns <code>true</code> if <code>value</code> can be cast to <code>property.getRequiredType()</code> per the type
     * conversion rules in section 3.6.4 of the JCR 2.0 specification AND <code>value</code> satisfies the constraints (if any)
     * for the property definition. If the property definition has a required type of {@link PropertyType#UNDEFINED}, the cast
     * will be considered to have succeeded and the value constraints (if any) will be interpreted using the semantics for the
     * type specified in <code>value.getType()</code>.
     * 
     * @param value the value to be validated
     * @param session the session in which the constraints are to be checked; may not be null
     * @return <code>true</code> if the value can be cast to the required type for the property definition (if it exists) and
     *         satisfies the constraints for the property (if any exist).
     * @see PropertyDefinition#getValueConstraints()
     * @see #satisfiesConstraints(Value,JcrSession)
     */
    boolean canCastToTypeAndSatisfyConstraints( Value value,
                                                JcrSession session ) {
        try {
            assert value instanceof JcrValue : "Illegal implementation of Value interface";
            ((JcrValue)value).asType(getRequiredType()); // throws ValueFormatException if there's a problem
            return satisfiesConstraints(value, session);
        } catch (javax.jcr.ValueFormatException vfe) {
            // Cast failed
            return false;
        }
    }

    /**
     * Returns <code>true</code> if <code>value</code> can be cast to <code>property.getRequiredType()</code> per the type
     * conversion rules in section 3.6.4 of the JCR 2.0 specification AND <code>value</code> satisfies the constraints (if any)
     * for the property definition. If the property definition has a required type of {@link PropertyType#UNDEFINED}, the cast
     * will be considered to have succeeded and the value constraints (if any) will be interpreted using the semantics for the
     * type specified in <code>value.getType()</code>.
     * 
     * @param values the values to be validated
     * @param session the session in which the constraints are to be checked; may not be null
     * @return <code>true</code> if the value can be cast to the required type for the property definition (if it exists) and
     *         satisfies the constraints for the property (if any exist).
     * @see PropertyDefinition#getValueConstraints()
     * @see #satisfiesConstraints(Value,JcrSession)
     */
    boolean canCastToTypeAndSatisfyConstraints( Value[] values,
                                                JcrSession session ) {
        for (Value value : values) {
            if (!canCastToTypeAndSatisfyConstraints(value, session)) return false;
        }
        return true;
    }

    /**
     * Returns a {@link ConstraintChecker} that will interpret the constraints described by <code>valueConstraints</code> using
     * the semantics defined in section 3.6.4 of the JCR 2.0 specification for the type indicated by <code>type</code> (where
     * <code>type</code> is a value from {@link PropertyType}) for the given <code>context</code>. The {@link ExecutionContext} is
     * used to provide namespace mappings and value factories for the other constraint checkers.
     * 
     * @param context the execution context
     * @param type the type of constraint checker that should be created (based on values from {@link PropertyType}).
     *        Type-specific semantics are defined in section 3.7.3.6 of the JCR 2.0 specification.
     * @param valueConstraints the constraints for the node as provided by {@link PropertyDefinition#getValueConstraints()}.
     * @return a constraint checker that matches the given parameters
     */
    private ConstraintChecker createChecker( ExecutionContext context,
                                             int type,
                                             String[] valueConstraints ) {
        switch (type) {
            case PropertyType.BINARY:
                return new BinaryConstraintChecker(valueConstraints, context);
            case PropertyType.DATE:
                return new DateTimeConstraintChecker(valueConstraints, context);
            case PropertyType.DOUBLE:
                return new DoubleConstraintChecker(valueConstraints, context);
            case PropertyType.LONG:
                return new LongConstraintChecker(valueConstraints, context);
            case PropertyType.NAME:
                return new NameConstraintChecker(valueConstraints, context);
            case PropertyType.PATH:
                return new PathConstraintChecker(valueConstraints, context);
            case PropertyType.REFERENCE:
            case PropertyType.WEAKREFERENCE:
                return new ReferenceConstraintChecker(valueConstraints, context);
            case org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE:
                return new SimpleReferenceConstraintChecker(valueConstraints, context);
            case PropertyType.STRING:
                return new StringConstraintChecker(valueConstraints, context);
            case PropertyType.DECIMAL:
                return new DecimalConstraintChecker(valueConstraints, context);
            default:
                throw new IllegalStateException("Invalid property type: " + type);
        }
    }

    /**
     * Determine if the constraints on this definition are as-constrained or more-constrained than those on the supplied
     * definition.
     * 
     * @param other the property definition to compare; may not be null
     * @param context the execution context used to parse any values within the constraints
     * @return true if this property definition is as-constrained or more-constrained, or false otherwise
     */
    boolean isAsOrMoreConstrainedThan( PropertyDefinition other,
                                       ExecutionContext context ) {
        String[] otherConstraints = other.getValueConstraints();
        if (otherConstraints == null || otherConstraints.length == 0) {
            // The ancestor's definition is less constrained, so it's okay even if this definition has no constraints ...
            return true;
        }
        String[] constraints = this.getValueConstraints();
        if (constraints == null || constraints.length == 0) {
            // This definition has no constraints, while the ancestor does have them ...
            return false;
        }
        // There are constraints on both, so make sure they have the same types ...
        int type = this.getRequiredType();
        int otherType = other.getRequiredType();
        if (type == otherType && type != PropertyType.UNDEFINED) {
            ConstraintChecker thisChecker = createChecker(context, type, constraints);
            ConstraintChecker thatChecker = createChecker(context, otherType, otherConstraints);
            return thisChecker.isAsOrMoreConstrainedThan(thatChecker);
        }
        // We can only compare constraint literals, and we can only expect that every constraint literal in this
        // definition can be found in the other defintion (which can have more than this one) ...
        Set<String> thatLiterals = new HashSet<String>();
        for (String literal : otherConstraints) {
            thatLiterals.add(literal);
        }
        for (String literal : constraints) {
            if (!thatLiterals.contains(literal)) return false;
        }
        return true;
    }

    /**
     * Get a constraint checker that can be used to compare constraints.
     * 
     * @param context the execution context; may not be null
     * @return the constraint checker; never null
     */
    ConstraintChecker getConstraintChecker( ExecutionContext context ) {
        return createChecker(context, getRequiredType(), getValueConstraints());
    }

    @Override
    public int hashCode() {
        return getId().toString().hashCode();
    }

    @Override
    public boolean equals( Object obj ) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        JcrPropertyDefinition other = (JcrPropertyDefinition)obj;
        if (id == null) {
            if (other.id != null) return false;
        } else if (!id.equals(other.id)) return false;
        return true;
    }

    /**
     * Interface that encapsulates a reusable method that can test values to determine if they match a specific list of
     * constraints for the semantics associated with a single {@link PropertyType}.
     */
    public interface ConstraintChecker {

        /**
         * Returns the {@link PropertyType} (e.g., {@link PropertyType#LONG}) that defines the semantics used for interpretation
         * for the constraint values.
         * 
         * @return the {@link PropertyType} (e.g., {@link PropertyType#LONG}) that defines the semantics used for interpretation
         *         for the constraint values
         */
        public abstract int getType();

        /**
         * Returns <code>true</code> if and only if <code>value</code> satisfies the constraints used to create this constraint
         * checker.
         * 
         * @param value the value to test
         * @param session the session in which the constraints are to be checked; may not be nul
         * @return whether or not the value satisfies the constraints used to create this constraint checker
         * @see PropertyDefinition#getValueConstraints()
         * @see JcrPropertyDefinition#satisfiesConstraints(Value,JcrSession)
         */
        public abstract boolean matches( Value value,
                                         JcrSession session );

        public abstract boolean isAsOrMoreConstrainedThan( ConstraintChecker other );
    }

    private interface Range<T extends Comparable<T>> {
        boolean accepts( T value );

        T getMinimum();

        T getMaximum();

        boolean within( Range<T> other );

        boolean includesLowerValue();

        boolean includesUpperValue();
    }

    /**
     * Encapsulation of common parsing logic used for all ranged constraints. Binary, long, double, and date values all have their
     * constraints interpreted as a set of ranges that may include or exclude each end-point in the range.
     * 
     * @param <T> the specific type of the constraint (e.g., Binary, Long, Double, or DateTime).
     */
    private static abstract class RangeConstraintChecker<T extends Comparable<T>> implements ConstraintChecker {
        private final Range<T>[] constraints;
        private final ValueFactory<T> valueFactory;
        private T minimumValue;
        private T maximumValue;

        @SuppressWarnings( "unchecked" )
        protected RangeConstraintChecker( String[] valueConstraints,
                                          ExecutionContext context ) {
            constraints = new Range[valueConstraints.length];
            this.valueFactory = getValueFactory(context.getValueFactories());

            for (int i = 0; i < valueConstraints.length; i++) {
                constraints[i] = parseValueConstraint(valueConstraints[i]);
            }
        }

        protected abstract ValueFactory<T> getValueFactory( ValueFactories valueFactories );

        protected abstract T parseValue( String s );

        @Override
        public String toString() {
            return constraints.toString();
        }

        @SuppressWarnings( "unchecked" )
        protected T getMinimum() {
            if (minimumValue == null) {
                // This is idempotent, so okay to recreate ...
                Comparable<T> minimum = null;
                // Go through the value constraints and see which one is the minimum value ...
                for (Range<T> range : constraints) {
                    T rangeMin = range.getMinimum();
                    if (rangeMin == null) continue;
                    if (minimum == null) {
                        minimum = rangeMin;
                    } else {
                        minimum = minimum.compareTo(rangeMin) > 0 ? rangeMin : minimum;
                    }
                }
                minimumValue = (T)minimum;
            }
            return minimumValue;
        }

        @SuppressWarnings( "unchecked" )
        protected T getMaximum() {
            if (maximumValue == null) {
                // This is idempotent, so okay to recreate ...
                Comparable<T> maximum = null;
                // Go through the value constraints and see which one is the minimum value ...
                for (Range<T> range : constraints) {
                    T rangeMax = range.getMaximum();
                    if (rangeMax == null) continue;
                    if (maximum == null) {
                        maximum = rangeMax;
                    } else {
                        maximum = maximum.compareTo(rangeMax) > 0 ? rangeMax : maximum;
                    }
                }
                maximumValue = (T)maximum;
            }
            return maximumValue;
        }

        /**
         * Parses one constraint value into a {@link Range} that will accept only values which match the range described by the
         * value constraint.
         * 
         * @param valueConstraint the individual value constraint to be parsed into a {@link Range}.
         * @return a range that accepts values which match the given value constraint.
         */
        private Range<T> parseValueConstraint( final String valueConstraint ) {
            assert valueConstraint != null;

            final boolean includeLower = valueConstraint.charAt(0) == '[';
            final boolean includeUpper = valueConstraint.charAt(valueConstraint.length() - 1) == ']';

            int commaInd = valueConstraint.indexOf(',');
            String lval = commaInd > 1 ? valueConstraint.substring(1, commaInd) : null;
            String rval = commaInd < valueConstraint.length() - 2 ? valueConstraint.substring(commaInd + 1,
                                                                                              valueConstraint.length() - 1) : null;

            final T lower = lval == null ? null : parseValue(lval.trim());
            final T upper = rval == null ? null : parseValue(rval.trim());

            return new Range<T>() {
                @Override
                public boolean accepts( T value ) {
                    if (lower != null && (includeLower ? lower.compareTo(value) > 0 : lower.compareTo(value) >= 0)) {
                        return false;
                    }
                    if (upper != null && (includeUpper ? upper.compareTo(value) < 0 : upper.compareTo(value) <= 0)) {
                        return false;
                    }
                    return true;
                }

                @Override
                public String toString() {
                    return valueConstraint;
                }

                @Override
                public T getMaximum() {
                    return upper;
                }

                @Override
                public T getMinimum() {
                    return lower;
                }

                @Override
                public boolean includesLowerValue() {
                    return includeLower;
                }

                @Override
                public boolean includesUpperValue() {
                    return includeUpper;
                }

                @Override
                public boolean within( Range<T> other ) {
                    T otherMin = other.getMinimum();
                    if (lower == null) {
                        if (otherMin != null) return false;
                        // Neither has a lower value (i.e., both null) so okay
                    } else if (otherMin != null) {
                        // Both have a non-null lower value ...
                        if (includeLower == other.includesLowerValue() || other.includesLowerValue()) {
                            if (lower.compareTo(otherMin) < 0) return false;
                        } else {
                            assert includeLower && !other.includesLowerValue();
                            if (lower.compareTo(otherMin) <= 0) return false;
                        }
                    }

                    T otherMax = other.getMaximum();
                    if (upper == null) {
                        if (otherMax != null) return false;
                        // Neither has an upper value (i.e., both null) so okay
                    } else if (otherMax != null) {
                        // Both have a non-null upper value ...
                        if (includeUpper == other.includesUpperValue() || other.includesUpperValue()) {
                            if (upper.compareTo(otherMax) > 0) return false;
                        } else {
                            assert includeUpper && !other.includesUpperValue();
                            if (upper.compareTo(otherMax) >= 0) return false;
                        }
                    }
                    return true;
                }
            };
        }

        @Override
        public boolean matches( Value value,
                                JcrSession session ) {
            assert value != null;
            T convertedValue = valueFactory.create(((JcrValue)value).value());
            for (int i = 0; i < constraints.length; i++) {
                if (constraints[i].accepts(convertedValue)) {
                    return true;
                }
            }
            return false;
        }

        @SuppressWarnings( "unchecked" )
        @Override
        public boolean isAsOrMoreConstrainedThan( ConstraintChecker other ) {
            if (!other.getClass().equals(this.getClass())) return false;
            RangeConstraintChecker<T> that = (RangeConstraintChecker<T>)other;
            // Each of the ranges must be within one other range ...
            for (Range<T> thisRange : this.constraints) {
                boolean found = false;
                for (Range<T> thatRange : that.constraints) {
                    if (thisRange.within(thatRange)) {
                        found = true;
                        break;
                    }
                }
                if (!found) return false;
            }
            return true;
        }
    }

    @Immutable
    private static class BinaryConstraintChecker extends LongConstraintChecker {

        protected BinaryConstraintChecker( String[] valueConstraints,
                                           ExecutionContext context ) {
            super(valueConstraints, context);
        }

        @Override
        public int getType() {
            return PropertyType.BINARY;
        }

        @Override
        public boolean matches( Value value,
                                JcrSession session ) {
            try {
                JcrValue jcrValue = (JcrValue)value;
                long thatSize = value.getBinary().getSize();
                JcrValue sizeValue = new JcrValue(jcrValue.factories(), PropertyType.LONG, thatSize);
                return super.matches(sizeValue, session);
            } catch (RepositoryException e) {
                assert false : "Unexpected condition";
                return false;
            }
        }
    }

    @Immutable
    private static class LongConstraintChecker extends RangeConstraintChecker<Long> {

        protected LongConstraintChecker( String[] valueConstraints,
                                         ExecutionContext context ) {
            super(valueConstraints, context);
        }

        @Override
        public int getType() {
            return PropertyType.LONG;
        }

        @Override
        protected ValueFactory<Long> getValueFactory( ValueFactories valueFactories ) {
            return valueFactories.getLongFactory();
        }

        @Override
        protected Long parseValue( String s ) {
            return Long.parseLong(s);
        }
    }

    @Immutable
    private static class DateTimeConstraintChecker extends RangeConstraintChecker<DateTime> {

        protected DateTimeConstraintChecker( String[] valueConstraints,
                                             ExecutionContext context ) {
            super(valueConstraints, context);
        }

        @Override
        public int getType() {
            return PropertyType.DATE;
        }

        @Override
        protected ValueFactory<DateTime> getValueFactory( ValueFactories valueFactories ) {
            return valueFactories.getDateFactory();
        }

        @Override
        protected DateTime parseValue( String s ) {
            return new JodaDateTime(s.trim());
        }
    }

    @Immutable
    private static class DoubleConstraintChecker extends RangeConstraintChecker<Double> {

        protected DoubleConstraintChecker( String[] valueConstraints,
                                           ExecutionContext context ) {
            super(valueConstraints, context);
        }

        @Override
        public int getType() {
            return PropertyType.DOUBLE;
        }

        @Override
        protected ValueFactory<Double> getValueFactory( ValueFactories valueFactories ) {
            return valueFactories.getDoubleFactory();
        }

        @Override
        protected Double parseValue( String s ) {
            return Double.parseDouble(s);
        }
    }

    @Immutable
    private static class DecimalConstraintChecker extends RangeConstraintChecker<BigDecimal> {

        protected DecimalConstraintChecker( String[] valueConstraints,
                                            ExecutionContext context ) {
            super(valueConstraints, context);
        }

        @Override
        public int getType() {
            return PropertyType.DECIMAL;
        }

        @Override
        protected ValueFactory<BigDecimal> getValueFactory( ValueFactories valueFactories ) {
            return valueFactories.getDecimalFactory();
        }

        @Override
        protected BigDecimal parseValue( String s ) {
            return new BigDecimal(s);
        }
    }

    @Immutable
    private static class ReferenceConstraintChecker implements ConstraintChecker {
        private final Name[] constraints;
        ExecutionContext context;

        protected ReferenceConstraintChecker( String[] valueConstraints,
                                              ExecutionContext context ) {
            this.context = context;

            NameFactory factory = context.getValueFactories().getNameFactory();

            constraints = new Name[valueConstraints.length];

            for (int i = 0; i < valueConstraints.length; i++) {
                constraints[i] = factory.create(valueConstraints[i]);
            }
        }

        @Override
        public int getType() {
            return PropertyType.REFERENCE;
        }

        @Override
        public String toString() {
            return asString(constraints, context);
        }

        @Override
        public boolean matches( Value value,
                                JcrSession session ) {
            assert value instanceof JcrValue;

            if (session == null) {
                return false;
            }

            JcrValue jcrValue = (JcrValue)value;
            Node node = null;
            try {
                node = session.getNodeByIdentifier(jcrValue.getString());
            } catch (RepositoryException re) {
                return false;
            }

            NamespaceRegistry namespaces = session.namespaces();
            for (int i = 0; i < constraints.length; i++) {
                try {
                    if (node.isNodeType(constraints[i].getString(namespaces))) {
                        return true;
                    }
                } catch (RepositoryException re) {
                    throw new IllegalStateException(re);
                }
            }

            return false;
        }

        @Override
        public boolean isAsOrMoreConstrainedThan( ConstraintChecker other ) {
            if (!other.getClass().equals(this.getClass())) return false;
            ReferenceConstraintChecker that = (ReferenceConstraintChecker)other;
            // Compute the set of names from 'that' ...
            Set<Name> thatNames = new HashSet<Name>();
            for (Name name : that.constraints) {
                thatNames.add(name);
            }
            // Every name in this must be found in that (but 'that' can have more) ...
            for (Name name : this.constraints) {
                if (!thatNames.contains(name)) return false;
            }
            return true;
        }
    }

    private static class SimpleReferenceConstraintChecker extends ReferenceConstraintChecker {
        protected SimpleReferenceConstraintChecker( String[] valueConstraints,
                                                    ExecutionContext context ) {
            super(valueConstraints, context);
        }

        @Override
        public int getType() {
            return org.modeshape.jcr.api.PropertyType.SIMPLE_REFERENCE;
        }
    }

    @Immutable
    private static class NameConstraintChecker implements ConstraintChecker {
        private final Name[] constraints;
        private final ValueFactory<Name> valueFactory;
        private final ExecutionContext context;

        protected NameConstraintChecker( String[] valueConstraints,
                                         ExecutionContext context ) {
            this.context = context;
            this.valueFactory = context.getValueFactories().getNameFactory();

            constraints = new Name[valueConstraints.length];

            for (int i = 0; i < valueConstraints.length; i++) {
                constraints[i] = valueFactory.create(valueConstraints[i]);
            }
        }

        @Override
        public String toString() {
            return asString(constraints, context);
        }

        @Override
        public int getType() {
            return PropertyType.NAME;
        }

        @Override
        public boolean matches( Value value,
                                JcrSession session ) {
            assert value instanceof JcrValue;

            JcrValue jcrValue = (JcrValue)value;
            // Need to use the session execution context to handle the remaps
            Name name = valueFactory.create(jcrValue.value());

            for (int i = 0; i < constraints.length; i++) {
                if (constraints[i].equals(name)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean isAsOrMoreConstrainedThan( ConstraintChecker other ) {
            if (!other.getClass().equals(this.getClass())) return false;
            NameConstraintChecker that = (NameConstraintChecker)other;
            // Compute the set of names from 'that' ...
            Set<Name> thatNames = new HashSet<Name>();
            for (Name name : that.constraints) {
                thatNames.add(name);
            }
            // Every name in this must be found in that (but 'that' can have more) ...
            for (Name name : this.constraints) {
                if (!thatNames.contains(name)) return false;
            }
            return true;
        }
    }

    @Immutable
    private static class StringConstraintChecker implements ConstraintChecker {
        private final Set<String> expressions = new HashSet<String>();
        private final Pattern[] constraints;
        private ValueFactory<String> valueFactory;

        protected StringConstraintChecker( String[] valueConstraints,
                                           ExecutionContext context ) {
            constraints = new Pattern[valueConstraints.length];
            this.valueFactory = context.getValueFactories().getStringFactory();

            for (int i = 0; i < valueConstraints.length; i++) {
                String expr = valueConstraints[i];
                constraints[i] = Pattern.compile(expr);
                expressions.add(expr);
            }
        }

        @Override
        public int getType() {
            return PropertyType.STRING;
        }

        @Override
        public boolean matches( Value value,
                                JcrSession session ) {
            assert value != null;

            String convertedValue = valueFactory.create(((JcrValue)value).value());

            for (int i = 0; i < constraints.length; i++) {
                if (constraints[i].matcher(convertedValue).matches()) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean isAsOrMoreConstrainedThan( ConstraintChecker other ) {
            if (!other.getClass().equals(this.getClass())) return false;
            StringConstraintChecker that = (StringConstraintChecker)other;
            // Every regex in this must be found in that (but 'that' can have more) ...
            for (String expression : this.expressions) {
                if (!that.expressions.contains(expression)) return false;
            }
            return true;
        }
    }

    @Immutable
    private static class PathConstraintChecker implements ConstraintChecker {
        private final ExecutionContext context;
        private final String[] constraints;

        protected PathConstraintChecker( String[] valueConstraints,
                                         ExecutionContext context ) {
            this.constraints = valueConstraints;
            this.context = context;
        }

        @Override
        public int getType() {
            return PropertyType.PATH;
        }

        @Override
        public String toString() {
            return constraints.toString();
        }

        @Override
        public boolean matches( Value valueToMatch,
                                JcrSession session ) {
            assert valueToMatch instanceof JcrValue;

            if (session == null) {
                return false;
            }

            /*
             * Need two path factories here.  One uses the permanent namespace mappings to parse the constraints.
             * The other also looks at the transient mappings to parse the checked value
             */
            PathFactory repoPathFactory = context.getValueFactories().getPathFactory();
            PathFactory sessionPathFactory = session.pathFactory();
            Path value = sessionPathFactory.create(((JcrValue)valueToMatch).value());
            value = value.getNormalizedPath();

            for (int i = 0; i < constraints.length; i++) {
                boolean matchesDescendants = constraints[i].endsWith("/*");
                String pathStr = constraints[i];
                if (matchesDescendants) pathStr = pathStr.substring(0, pathStr.length() - 2);
                Path constraintPath = repoPathFactory.create(pathStr);
                if (matchesDescendants && value.isDescendantOf(constraintPath)) {
                    return true;
                }

                if (!matchesDescendants && value.equals(constraintPath)) {
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean isAsOrMoreConstrainedThan( ConstraintChecker other ) {
            if (!other.getClass().equals(this.getClass())) return false;
            PathConstraintChecker that = (PathConstraintChecker)other;
            // We only need the main path factory, since all paths are defined in node types ...
            PathFactory pathFactory = context.getValueFactories().getPathFactory();
            Set<Path> thatWildcardPaths = new HashSet<Path>();
            Set<Path> thatExactPaths = new HashSet<Path>();
            for (String constraint : that.constraints) {
                boolean matchesDescendants = constraint.endsWith("/*");
                if (matchesDescendants) {
                    String pathStr = constraint.substring(0, constraint.length() - 2);
                    Path path = pathFactory.create(pathStr);
                    thatWildcardPaths.add(path);
                } else {
                    Path path = pathFactory.create(constraint);
                    thatExactPaths.add(path);
                }
            }
            // Every path in this must be equal to or a descendant of a path in that ...
            for (String constraint : this.constraints) {
                Path path = pathFactory.create(constraint);
                boolean matched = false;
                // Check the exact match paths first ...
                if (thatExactPaths.contains(path)) {
                    matched = true;
                }
                if (!matched) {
                    // Now check the wildcard paths ...
                    for (Path thatPath : thatWildcardPaths) {
                        if (path.isAtOrBelow(thatPath)) {
                            matched = true;
                            break;
                        }
                    }
                    if (!matched) return false;
                }
            }
            return true;
        }
    }

    protected static String asString( Object[] values,
                                      ExecutionContext context ) {
        if (values.length == 0) return "[]";
        StringFactory strings = context.getValueFactories().getStringFactory();
        StringBuilder sb = new StringBuilder();
        sb.append('[');
        sb.append(strings.create(values[0]));
        for (int i = 1; i != values.length; ++i) {
            sb.append(',');
            sb.append(strings.create(values[0]));
        }
        return sb.toString();
    }

}
