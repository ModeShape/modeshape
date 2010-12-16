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
package org.modeshape.jcr;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Pattern;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;
import net.jcip.annotations.Immutable;
import org.modeshape.graph.ExecutionContext;
import org.modeshape.graph.Location;
import org.modeshape.graph.ModeShapeIntLexicon;
import org.modeshape.graph.property.Binary;
import org.modeshape.graph.property.DateTime;
import org.modeshape.graph.property.Name;
import org.modeshape.graph.property.NameFactory;
import org.modeshape.graph.property.Path;
import org.modeshape.graph.property.PathFactory;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.property.ValueFormatException;
import org.modeshape.graph.property.basic.JodaDateTime;
import org.modeshape.graph.query.model.Operator;
import org.modeshape.jcr.api.query.qom.QueryObjectModelConstants;

/**
 * ModeShape implementation of the {@link PropertyDefinition} interface. This implementation is immutable and has all fields
 * initialized through its constructor.
 */
@Immutable
class JcrPropertyDefinition extends JcrItemDefinition implements PropertyDefinition {

    protected static final Map<String, Operator> OPERATORS_BY_JCR_NAME;

    static {
        Map<String, Operator> map = new HashMap<String, Operator>();
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

    private final Value[] defaultValues;
    private final int requiredType;
    private final String[] valueConstraints;
    private final boolean multiple;
    private final boolean fullTextSearchable;
    private final boolean isPrivate;
    private final boolean queryOrderable;
    private final String[] queryOperators;

    private PropertyDefinitionId id;
    private ConstraintChecker checker = null;

    JcrPropertyDefinition( ExecutionContext context,
                           JcrNodeType declaringNodeType,
                           Name name,
                           int onParentVersion,
                           boolean autoCreated,
                           boolean mandatory,
                           boolean protectedItem,
                           Value[] defaultValues,
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
        this.isPrivate = name != null && ModeShapeIntLexicon.Namespace.URI.equals(name.getNamespaceUri());
        assert this.valueConstraints != null;
    }

    /**
     * Get the durable identifier for this property definition.
     * 
     * @return the property definition ID; never null
     */
    public PropertyDefinitionId getId() {
        if (id == null) {
            // This is idempotent, so no need to lock
            id = new PropertyDefinitionId(this.declaringNodeType.getInternalName(), this.name, this.requiredType, this.multiple);
        }
        return id;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#getDefaultValues()
     */
    public Value[] getDefaultValues() {
        return defaultValues;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#getRequiredType()
     */
    public int getRequiredType() {
        return requiredType;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#getValueConstraints()
     */
    public String[] getValueConstraints() {
        return valueConstraints;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#isMultiple()
     */
    public boolean isMultiple() {
        return multiple;
    }

    /**
     * Return whether this property definition is considered private.
     * 
     * @return true if the definition is private, or false otherwise
     */
    public boolean isPrivate() {
        return isPrivate;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#isFullTextSearchable()
     */
    public boolean isFullTextSearchable() {
        return fullTextSearchable;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#isQueryOrderable()
     */
    public boolean isQueryOrderable() {
        return queryOrderable;
    }

    /**
     * Same as {@link #getAvailableQueryOperators()}.
     * 
     * @return the query operators
     * @deprecated Use the standard JCR {@link #getAvailableQueryOperators()} method instead
     */
    @Deprecated
    public String[] getQueryOperators() {
        return queryOperators;
    }

    /**
     * {@inheritDoc}
     * 
     * @see javax.jcr.nodetype.PropertyDefinition#getAvailableQueryOperators()
     */
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
        return new JcrPropertyDefinition(this.context, declaringNodeType, this.name, this.getOnParentVersion(),
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
        return new JcrPropertyDefinition(context, this.declaringNodeType, this.name, this.getOnParentVersion(),
                                         this.isAutoCreated(), this.isMandatory(), this.isProtected(), this.getDefaultValues(),
                                         this.getRequiredType(), this.getValueConstraints(), this.isMultiple(),
                                         this.isFullTextSearchable(), this.isQueryOrderable(), this.getAvailableQueryOperators());
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        ValueFactory<String> strings = context.getValueFactories().getStringFactory();
        StringBuilder sb = new StringBuilder();
        PropertyDefinitionId id = getId();
        sb.append(strings.create(id.getNodeTypeName()));
        sb.append('/');
        sb.append(strings.create(id.getPropertyDefinitionName()));
        sb.append('/');
        sb.append(PropertyType.nameFromValue(id.getPropertyType()));
        sb.append(id.allowsMultiple() ? '*' : '1');
        return sb.toString();
    }

    boolean satisfiesConstraints( Value value ) {
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
            return checker.matches(value);
        } catch (ValueFormatException vfe) {
            // The value was so wonky that we couldn't even convert it to an appropriate type
            return false;
        }
    }

    boolean satisfiesConstraints( Value[] values ) {
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
                if (!checker.matches(value)) return false;
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
            assert checker instanceof RangeConstraintChecker<?>;
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
            assert checker instanceof RangeConstraintChecker<?>;
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
     * @return <code>true</code> if the value can be cast to the required type for the property definition (if it exists) and
     *         satisfies the constraints for the property (if any exist).
     * @see PropertyDefinition#getValueConstraints()
     * @see #satisfiesConstraints(Value)
     */
    boolean canCastToTypeAndSatisfyConstraints( Value value ) {
        try {
            assert value instanceof JcrValue : "Illegal implementation of Value interface";
            ((JcrValue)value).asType(getRequiredType()); // throws ValueFormatException if there's a problem
            return satisfiesConstraints(value);
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
     * @return <code>true</code> if the value can be cast to the required type for the property definition (if it exists) and
     *         satisfies the constraints for the property (if any exist).
     * @see PropertyDefinition#getValueConstraints()
     * @see #satisfiesConstraints(Value)
     */
    boolean canCastToTypeAndSatisfyConstraints( Value[] values ) {
        for (Value value : values) {
            if (!canCastToTypeAndSatisfyConstraints(value)) return false;
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
            case PropertyType.STRING:
                return new StringConstraintChecker(valueConstraints, context);
            case PropertyType.DECIMAL:
                return new DecimalConstraintChecker(valueConstraints, context);
            default:
                throw new IllegalStateException("Invalid property type: " + type);
        }
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
         * @return whether or not the value satisfies the constraints used to create this constraint checker
         * @see PropertyDefinition#getValueConstraints()
         * @see JcrPropertyDefinition#satisfiesConstraints(Value)
         */
        public abstract boolean matches( Value value );
    }

    private interface Range<T> {
        boolean accepts( T value );

        Comparable<T> getMinimum();

        Comparable<T> getMaximum();
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

        protected abstract Comparable<T> parseValue( String s );

        @SuppressWarnings( "unchecked" )
        protected T getMinimum() {
            if (minimumValue == null) {
                // This is idempotent, so okay to recreate ...
                Comparable<T> minimum = null;
                // Go through the value constraints and see which one is the minimum value ...
                for (Range<T> range : constraints) {
                    T rangeMin = (T)range.getMinimum();
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
                    T rangeMax = (T)range.getMaximum();
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
        private Range<T> parseValueConstraint( String valueConstraint ) {
            assert valueConstraint != null;

            final boolean includeLower = valueConstraint.charAt(0) == '[';
            final boolean includeUpper = valueConstraint.charAt(valueConstraint.length() - 1) == ']';

            int commaInd = valueConstraint.indexOf(',');
            String lval = commaInd > 1 ? valueConstraint.substring(1, commaInd) : null;
            String rval = commaInd < valueConstraint.length() - 2 ? valueConstraint.substring(commaInd + 1,
                                                                                              valueConstraint.length() - 1) : null;

            final Comparable<T> lower = lval == null ? null : parseValue(lval.trim());
            final Comparable<T> upper = rval == null ? null : parseValue(rval.trim());

            return new Range<T>() {
                public boolean accepts( T value ) {
                    if (lower != null && (includeLower ? lower.compareTo(value) > 0 : lower.compareTo(value) >= 0)) {
                        return false;
                    }
                    if (upper != null && (includeUpper ? upper.compareTo(value) < 0 : upper.compareTo(value) <= 0)) {
                        return false;
                    }
                    return true;
                }

                /**
                 * {@inheritDoc}
                 * 
                 * @see org.modeshape.jcr.JcrPropertyDefinition.Range#getMaximum()
                 */
                public Comparable<T> getMaximum() {
                    return lower;
                }

                /**
                 * {@inheritDoc}
                 * 
                 * @see org.modeshape.jcr.JcrPropertyDefinition.Range#getMinimum()
                 */
                public Comparable<T> getMinimum() {
                    return upper;
                }
            };
        }

        /**
         * {@inheritDoc}
         * 
         * @see ConstraintChecker#matches(javax.jcr.Value)
         */
        public boolean matches( Value value ) {
            assert value != null;

            T convertedValue = valueFactory.create(((JcrValue)value).value());

            for (int i = 0; i < constraints.length; i++) {
                if (constraints[i].accepts(convertedValue)) {
                    return true;
                }
            }

            return false;
        }
    }

    @Immutable
    private static class BinaryConstraintChecker extends LongConstraintChecker {
        private final ValueFactories valueFactories;

        protected BinaryConstraintChecker( String[] valueConstraints,
                                           ExecutionContext context ) {
            super(valueConstraints, context);

            this.valueFactories = context.getValueFactories();
        }

        @Override
        public int getType() {
            return PropertyType.BINARY;
        }

        @Override
        public boolean matches( Value value ) {
            Binary binary = valueFactories.getBinaryFactory().create(((JcrValue)value).value());
            Value sizeValue = ((JcrValue)value).sessionCache().session().getValueFactory().createValue(binary.getSize());
            return super.matches(sizeValue);
        }
    }

    @Immutable
    private static class LongConstraintChecker extends RangeConstraintChecker<Long> {

        protected LongConstraintChecker( String[] valueConstraints,
                                         ExecutionContext context ) {
            super(valueConstraints, context);
        }

        public int getType() {
            return PropertyType.LONG;
        }

        @Override
        protected ValueFactory<Long> getValueFactory( ValueFactories valueFactories ) {
            return valueFactories.getLongFactory();
        }

        @Override
        protected Comparable<Long> parseValue( String s ) {
            return Long.parseLong(s);
        }
    }

    @Immutable
    private static class DateTimeConstraintChecker extends RangeConstraintChecker<DateTime> {

        protected DateTimeConstraintChecker( String[] valueConstraints,
                                             ExecutionContext context ) {
            super(valueConstraints, context);
        }

        public int getType() {
            return PropertyType.DATE;
        }

        @Override
        protected ValueFactory<DateTime> getValueFactory( ValueFactories valueFactories ) {
            return valueFactories.getDateFactory();
        }

        @Override
        protected Comparable<DateTime> parseValue( String s ) {
            return new JodaDateTime(s.trim());
        }
    }

    @Immutable
    private static class DoubleConstraintChecker extends RangeConstraintChecker<Double> {

        protected DoubleConstraintChecker( String[] valueConstraints,
                                           ExecutionContext context ) {
            super(valueConstraints, context);
        }

        public int getType() {
            return PropertyType.DOUBLE;
        }

        @Override
        protected ValueFactory<Double> getValueFactory( ValueFactories valueFactories ) {
            return valueFactories.getDoubleFactory();
        }

        @Override
        protected Comparable<Double> parseValue( String s ) {
            return Double.parseDouble(s);
        }
    }

    @Immutable
    private static class DecimalConstraintChecker extends RangeConstraintChecker<BigDecimal> {

        protected DecimalConstraintChecker( String[] valueConstraints,
                                            ExecutionContext context ) {
            super(valueConstraints, context);
        }

        public int getType() {
            return PropertyType.DECIMAL;
        }

        @Override
        protected ValueFactory<BigDecimal> getValueFactory( ValueFactories valueFactories ) {
            return valueFactories.getDecimalFactory();
        }

        @Override
        protected Comparable<BigDecimal> parseValue( String s ) {
            return new BigDecimal(s);
        }
    }

    @Immutable
    private static class ReferenceConstraintChecker implements ConstraintChecker {
        // private final ExecutionContext context;

        private final Name[] constraints;

        protected ReferenceConstraintChecker( String[] valueConstraints,
                                              ExecutionContext context ) {
            // this.context = context;

            NameFactory factory = context.getValueFactories().getNameFactory();

            constraints = new Name[valueConstraints.length];

            for (int i = 0; i < valueConstraints.length; i++) {
                constraints[i] = factory.create(valueConstraints[i]);
            }
        }

        public int getType() {
            return PropertyType.REFERENCE;
        }

        public boolean matches( Value value ) {
            assert value instanceof JcrValue;

            JcrValue jcrValue = (JcrValue)value;
            SessionCache cache = jcrValue.sessionCache();

            Node node = null;
            try {
                UUID uuid = cache.factories().getUuidFactory().create(jcrValue.value());
                node = cache.findJcrNode(Location.create(uuid));
            } catch (RepositoryException re) {
                return false;
            }

            for (int i = 0; i < constraints.length; i++) {
                try {
                    if (node.isNodeType(constraints[i].getString(cache.session().namespaces()))) {
                        return true;
                    }
                } catch (RepositoryException re) {
                    throw new IllegalStateException(re);
                }
            }

            return false;
        }
    }

    @Immutable
    private static class NameConstraintChecker implements ConstraintChecker {
        private final Name[] constraints;
        private final ValueFactory<Name> valueFactory;

        protected NameConstraintChecker( String[] valueConstraints,
                                         ExecutionContext context ) {
            this.valueFactory = context.getValueFactories().getNameFactory();

            constraints = new Name[valueConstraints.length];

            for (int i = 0; i < valueConstraints.length; i++) {
                constraints[i] = valueFactory.create(valueConstraints[i]);
            }
        }

        public int getType() {
            return PropertyType.NAME;
        }

        public boolean matches( Value value ) {
            assert value instanceof JcrValue;

            JcrValue jcrValue = (JcrValue)value;
            // Need to use the session execution context to handle the remaps
            Name name = jcrValue.sessionCache()
                                .session()
                                .getExecutionContext()
                                .getValueFactories()
                                .getNameFactory()
                                .create(jcrValue.value());

            for (int i = 0; i < constraints.length; i++) {
                if (constraints[i].equals(name)) {
                    return true;
                }
            }

            return false;
        }
    }

    @Immutable
    private static class StringConstraintChecker implements ConstraintChecker {
        private final Pattern[] constraints;
        private ValueFactory<String> valueFactory;

        protected StringConstraintChecker( String[] valueConstraints,
                                           ExecutionContext context ) {
            constraints = new Pattern[valueConstraints.length];
            this.valueFactory = context.getValueFactories().getStringFactory();

            for (int i = 0; i < valueConstraints.length; i++) {
                constraints[i] = Pattern.compile(valueConstraints[i]);
            }
        }

        public int getType() {
            return PropertyType.STRING;
        }

        public boolean matches( Value value ) {
            assert value != null;

            String convertedValue = valueFactory.create(((JcrValue)value).value());

            for (int i = 0; i < constraints.length; i++) {
                if (constraints[i].matcher(convertedValue).matches()) {
                    return true;
                }
            }

            return false;
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

        public int getType() {
            return PropertyType.PATH;
        }

        public boolean matches( Value valueToMatch ) {
            assert valueToMatch instanceof JcrValue;

            JcrValue jcrValue = (JcrValue)valueToMatch;

            /*
             * Need two path factories here.  One uses the permanent namespace mappings to parse the constraints.
             * The other also looks at the transient mappings to parse the checked value
             */
            PathFactory repoPathFactory = context.getValueFactories().getPathFactory();
            PathFactory sessionPathFactory = jcrValue.sessionCache().context().getValueFactories().getPathFactory();
            Path value = sessionPathFactory.create(((JcrValue)valueToMatch).value());
            value = value.getNormalizedPath();

            for (int i = 0; i < constraints.length; i++) {
                boolean matchesDescendants = constraints[i].endsWith("*");
                Path constraintPath = repoPathFactory.create(matchesDescendants ? constraints[i].substring(0,
                                                                                                           constraints[i].length() - 2) : constraints[i]);

                if (matchesDescendants && value.isDecendantOf(constraintPath)) {
                    return true;
                }

                if (!matchesDescendants && value.equals(constraintPath)) {
                    return true;
                }
            }

            return false;
        }
    }

}
