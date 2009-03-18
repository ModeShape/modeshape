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
package org.jboss.dna.jcr;

import java.util.regex.Pattern;
import javax.jcr.Node;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.nodetype.PropertyDefinition;
import net.jcip.annotations.Immutable;
import org.jboss.dna.graph.ExecutionContext;
import org.jboss.dna.graph.property.Binary;
import org.jboss.dna.graph.property.DateTime;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.property.NameFactory;
import org.jboss.dna.graph.property.Path;
import org.jboss.dna.graph.property.PathFactory;
import org.jboss.dna.graph.property.ValueFactories;
import org.jboss.dna.graph.property.ValueFactory;
import org.jboss.dna.graph.property.ValueFormatException;
import org.jboss.dna.graph.property.basic.JodaDateTime;
/**
 * DNA implementation of the {@link PropertyDefinition} interface. This implementation is immutable and has all fields initialized
 * through its constructor.
 */
@Immutable
class JcrPropertyDefinition extends JcrItemDefinition implements PropertyDefinition {

    private final Value[] defaultValues;
    private final int requiredType;
    private final String[] valueConstraints;
    private final boolean multiple;
    private PropertyDefinitionId id;
    private ConstraintChecker checker = null;

    JcrPropertyDefinition( JcrSession session,
                           JcrNodeType declaringNodeType,
                           Name name,
                           int onParentVersion,
                           boolean autoCreated,
                           boolean mandatory,
                           boolean protectedItem,
                           Value[] defaultValues,
                           int requiredType,
                           String[] valueConstraints,
                           boolean multiple ) {
        super(session, declaringNodeType, name, onParentVersion, autoCreated, mandatory, protectedItem);
        this.defaultValues = defaultValues;
        this.requiredType = requiredType;
        this.valueConstraints = valueConstraints;
        this.multiple = multiple;
    }

    /**
     * Get the durable identifier for this property definition.
     * 
     * @return the property definition ID; never null
     */
    public PropertyDefinitionId getId() {
        if (id == null) {
            id = new PropertyDefinitionId(declaringNodeType.getInternalName(), name);
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
     * Creates a new <code>JcrPropertyDefinition</code> that is identical to the current object, but with the given
     * <code>declaringNodeType</code>. Provided to support immutable pattern for this class.
     * 
     * @param declaringNodeType the declaring node type for the new <code>JcrPropertyDefinition</code>
     * @return a new <code>JcrPropertyDefinition</code> that is identical to the current object, but with the given
     *         <code>declaringNodeType</code>.
     */
    JcrPropertyDefinition with( JcrNodeType declaringNodeType ) {
        return new JcrPropertyDefinition(this.session, declaringNodeType, this.name, this.getOnParentVersion(),
                                         this.isAutoCreated(), this.isMandatory(), this.isProtected(), this.getDefaultValues(),
                                         this.getRequiredType(), this.getValueConstraints(), this.isMultiple());
    }

    boolean satisfiesConstraints( Value value ) {
        assert value instanceof JcrValue;

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
            checker = createChecker(session, type, valueConstraints);
            this.checker = checker;
        }

        try {
            return checker.matches(value);
        } catch (ValueFormatException vfe) {
            // The value was so wonky that we couldn't even convert it to an appropriate type
            return false;
        }
    }

    /**
     * Returns a {@link ConstraintChecker} that will interpret the constraints described by <code>valueConstraints</code> using
     * the semantics defined in section 6.7.16 of the JCR 1.0 specification for the type indicated by <code>type</code> (where
     * <code>type</code> is a value from {@link PropertyType}) for the given <code>session</code>. The session is required to
     * handle the UUID lookups for reference constraints and provides the {@link ExecutionContext} that is used to provide
     * namespace mappings and value factories for the other constraint checkers.
     * 
     * @param session the current session
     * @param type the type of constraint checker that should be created (based on values from {@link PropertyType}).
     *        Type-specific semantics are defined in section 6.7.16 of the JCR 1.0 specification.
     * @param valueConstraints the constraints for the node as provided by {@link PropertyDefinition#getValueConstraints()}.
     * @return a constraint checker that matches the given parameters
     */
    private ConstraintChecker createChecker( JcrSession session,
                                                   int type,
                                                   String[] valueConstraints ) {
        ExecutionContext context = session.getExecutionContext();

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
                return new ReferenceConstraintChecker(valueConstraints, session);
            case PropertyType.STRING:
                return new StringConstraintChecker(valueConstraints, context);
            default:
                throw new IllegalStateException("Invalid property type: " + type);
        }
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
            return super.matches(new JcrValue(valueFactories, PropertyType.LONG, binary.getSize()));
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
    private static class ReferenceConstraintChecker implements ConstraintChecker {
        private final JcrSession session;

        private final Name[] constraints;

        protected ReferenceConstraintChecker( String[] valueConstraints,
                                              JcrSession session ) {
            this.session = session;

            NameFactory factory = session.getExecutionContext().getValueFactories().getNameFactory();

            constraints = new Name[valueConstraints.length];

            for (int i = 0; i < valueConstraints.length; i++) {
                constraints[i] = factory.create(valueConstraints[i]);
            }
        }

        public int getType() {
            return PropertyType.REFERENCE;
        }

        public boolean matches( Value value ) {
            assert value != null;

            Node node = null;
            try {
                node = session.getNodeByUUID(((JcrValue)value).value().toString());
            } catch (RepositoryException re) {
                return false;
            }

            for (int i = 0; i < constraints.length; i++) {
                try {
                    if (node.isNodeType(constraints[i].getString(session.getExecutionContext().getNamespaceRegistry()))) {
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
            assert value != null;

            Name name = valueFactory.create(((JcrValue)value).value());

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
            assert valueToMatch != null;

            PathFactory pathFactory = context.getValueFactories().getPathFactory();
            Path value = pathFactory.create(((JcrValue)valueToMatch).value());
            value = value.getNormalizedPath();

            for (int i = 0; i < constraints.length; i++) {
                boolean matchesDescendants = constraints[i].endsWith("*");
                Path constraintPath = pathFactory.create(matchesDescendants ? constraints[i].substring(0,
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
