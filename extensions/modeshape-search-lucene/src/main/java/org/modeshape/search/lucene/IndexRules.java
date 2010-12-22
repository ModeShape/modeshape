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
package org.modeshape.search.lucene;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.modeshape.common.util.CheckArg;
import org.modeshape.graph.property.Name;

/**
 * The set of rules that dictate how properties should be indexed.
 */
@Immutable
public class IndexRules {

    public static enum FieldType {
        STRING,
        DOUBLE,
        FLOAT,
        INT,
        BOOLEAN,
        LONG,
        DATE,
        BINARY,
        REFERENCE,
        WEAK_REFERENCE,
        DECIMAL;
    }

    /**
     * A single rule that dictates how a single property should be indexed.
     * 
     * @see IndexRules#getRule(Name)
     */
    @Immutable
    public static interface Rule {

        boolean isSkipped();

        boolean canBeReference();

        boolean isFullTextSearchable();

        FieldType getType();

        Field.Store getStoreOption();

        Field.Index getIndexOption();
    }

    @Immutable
    public static interface NumericRule<T> extends Rule {
        T getMinimum();

        T getMaximum();
    }

    public static final Rule SKIP = new SkipRule();

    @Immutable
    protected static class SkipRule implements Rule {

        /**
         * {@inheritDoc}
         * 
         * @see Rule#getType()
         */
        public FieldType getType() {
            return FieldType.STRING;
        }

        /**
         * {@inheritDoc}
         * 
         * @see Rule#isSkipped()
         */
        public boolean isSkipped() {
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.search.lucene.IndexRules.Rule#isFullTextSearchable()
         */
        @Override
        public boolean isFullTextSearchable() {
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.search.lucene.IndexRules.Rule#canBeReference()
         */
        @Override
        public boolean canBeReference() {
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see Rule#getIndexOption()
         */
        public Index getIndexOption() {
            return Field.Index.NO;
        }

        /**
         * {@inheritDoc}
         * 
         * @see Rule#getStoreOption()
         */
        public Store getStoreOption() {
            return Field.Store.NO;
        }
    }

    @Immutable
    protected static class TypedRule implements Rule {
        protected final boolean canBeReference;
        protected final boolean fullTextSearchable;
        protected final FieldType type;
        protected final Field.Store store;
        protected final Field.Index index;

        protected TypedRule( FieldType type,
                             Field.Store store,
                             Field.Index index,
                             boolean canBeReference,
                             boolean fullTextSearchable ) {
            this.type = type;
            this.index = index;
            this.store = store;
            this.canBeReference = canBeReference;
            this.fullTextSearchable = fullTextSearchable;
            assert this.type != null;
            assert this.index != null;
            assert this.store != null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see IndexRules.Rule#getType()
         */
        public FieldType getType() {
            return type;
        }

        /**
         * {@inheritDoc}
         * 
         * @see IndexRules.Rule#isSkipped()
         */
        public boolean isSkipped() {
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.search.lucene.IndexRules.Rule#isFullTextSearchable()
         */
        @Override
        public boolean isFullTextSearchable() {
            return fullTextSearchable;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.modeshape.search.lucene.IndexRules.Rule#canBeReference()
         */
        @Override
        public boolean canBeReference() {
            return canBeReference;
        }

        /**
         * {@inheritDoc}
         * 
         * @see IndexRules.Rule#getIndexOption()
         */
        public Index getIndexOption() {
            return index;
        }

        /**
         * {@inheritDoc}
         * 
         * @see IndexRules.Rule#getStoreOption()
         */
        public Store getStoreOption() {
            return store;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return type.name() + " rule (" + store + "," + index + ")";
        }
    }

    @Immutable
    protected static class NumericTypedRule<T> extends TypedRule implements NumericRule<T> {
        protected final T minValue;
        protected final T maxValue;

        protected NumericTypedRule( FieldType type,
                                    Field.Store store,
                                    Field.Index index,
                                    T minValue,
                                    T maxValue ) {
            super(type, store, index, false, false);
            this.minValue = minValue;
            this.maxValue = maxValue;
            assert this.minValue != null;
            assert this.maxValue != null;
        }

        /**
         * {@inheritDoc}
         * 
         * @see IndexRules.NumericRule#getMaximum()
         */
        public T getMaximum() {
            return maxValue;
        }

        /**
         * {@inheritDoc}
         * 
         * @see IndexRules.NumericRule#getMinimum()
         */
        public T getMinimum() {
            return minValue;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return super.toString() + " with range [" + minValue + "," + maxValue + "]";
        }
    }

    private final Map<Name, Rule> rulesByName;
    private final Rule defaultRule;

    protected IndexRules( Map<Name, Rule> rulesByName,
                          Rule defaultRule ) {
        this.rulesByName = rulesByName;
        this.defaultRule = defaultRule != null ? defaultRule : SKIP;
        assert this.defaultRule != null;
    }

    /**
     * Get the rule associated with the given property name.
     * 
     * @param name the property name, or null if the default rule is to be returned
     * @return the rule; never null
     */
    public Rule getRule( Name name ) {
        Rule result = rulesByName.get(name);
        return result != null ? result : this.defaultRule;
    }

    /**
     * Return a new builder that can be used to create {@link IndexRules} objects.
     * 
     * @return a builder; never null
     */
    public static Builder createBuilder() {
        return new Builder(new HashMap<Name, Rule>(), null);
    }

    /**
     * Return a new builder that can be used to create {@link IndexRules} objects.
     * 
     * @param initialRules the rules that the builder should start with
     * @return a builder; never null
     * @throws IllegalArgumentException if the initial rules reference is null
     */
    public static Builder createBuilder( IndexRules initialRules ) {
        CheckArg.isNotNull(initialRules, "initialRules");
        return new Builder(new HashMap<Name, Rule>(initialRules.rulesByName), initialRules.defaultRule);
    }

    /**
     * A builder of immutable {@link IndexRules} objects.
     */
    @NotThreadSafe
    public static class Builder {
        private final Map<Name, Rule> rulesByName;
        private Rule defaultRule;

        Builder( Map<Name, Rule> rulesByName,
                 Rule defaultRule ) {
            assert rulesByName != null;
            this.rulesByName = rulesByName;
            this.defaultRule = defaultRule;
        }

        /**
         * Mark the properties with the supplied names to be skipped from indexing.
         * 
         * @param namesToIndex the names of the properties that are to be skipped
         * @return this builder for convenience and method chaining; never null
         */
        public Builder skip( Name... namesToIndex ) {
            if (namesToIndex != null) {
                for (Name name : namesToIndex) {
                    rulesByName.put(name, SKIP);
                }
            }
            return this;
        }

        /**
         * Define a string-based field as the default.
         * 
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @param canBeReference true if this field can contain references; or false if it cannot
         * @param fullTextSearchable true if this field is full-text searchable, or false otherwise
         * @return this builder for convenience and method chaining; never null
         */
        public Builder defaultTo( Field.Store store,
                                  Field.Index index,
                                  boolean canBeReference,
                                  boolean fullTextSearchable ) {
            if (store == null) store = Field.Store.YES;
            if (index == null) index = Field.Index.NOT_ANALYZED;
            defaultRule = new TypedRule(FieldType.STRING, store, index, canBeReference, fullTextSearchable);
            return this;
        }

        /**
         * Define a string-based field in the indexes. This method will overwrite any existing definition in this builder.
         * 
         * @param name the name of the field
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @param canBeReference true if this field can contain references; or false if it cannot
         * @param fullTextSearchable true if this field is full-text searchable, or false otherwise
         * @return this builder for convenience and method chaining; never null
         */
        public Builder stringField( Name name,
                                    Field.Store store,
                                    Field.Index index,
                                    boolean canBeReference,
                                    boolean fullTextSearchable ) {
            if (store == null) store = Field.Store.YES;
            if (index == null) index = Field.Index.NOT_ANALYZED;
            Rule rule = new TypedRule(FieldType.STRING, store, index, canBeReference, fullTextSearchable);
            rulesByName.put(name, rule);
            return this;
        }

        /**
         * Define a binary-based field in the indexes. This method will overwrite any existing definition in this builder.
         * 
         * @param name the name of the field
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @param fullTextSearchable true if this field is full-text searchable, or false otherwise
         * @return this builder for convenience and method chaining; never null
         */
        public Builder binaryField( Name name,
                                    Field.Store store,
                                    Field.Index index,
                                    boolean fullTextSearchable ) {
            if (store == null) store = Field.Store.YES;
            if (index == null) index = Field.Index.NOT_ANALYZED;
            Rule rule = new TypedRule(FieldType.BINARY, store, index, false, fullTextSearchable);
            rulesByName.put(name, rule);
            return this;
        }

        /**
         * Define a path-based field in the indexes. This method will overwrite any existing definition in this builder.
         * 
         * @param name the name of the field
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @return this builder for convenience and method chaining; never null
         */
        public Builder pathField( Name name,
                                  Field.Store store,
                                  Field.Index index ) {
            if (store == null) store = Field.Store.YES;
            if (index == null) index = Field.Index.NOT_ANALYZED;
            Rule rule = new TypedRule(FieldType.STRING, store, index, false, false);
            rulesByName.put(name, rule);
            return this;
        }

        /**
         * Define a reference-based field in the indexes. This method will overwrite any existing definition in this builder.
         * 
         * @param name the name of the field
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @return this builder for convenience and method chaining; never null
         */
        public Builder referenceField( Name name,
                                       Field.Store store,
                                       Field.Index index ) {
            if (store == null) store = Field.Store.YES;
            if (index == null) index = Field.Index.NOT_ANALYZED;
            Rule rule = new TypedRule(FieldType.REFERENCE, store, index, true, false);
            rulesByName.put(name, rule);
            return this;
        }

        /**
         * Define a weak-reference-based field in the indexes. This method will overwrite any existing definition in this builder.
         * 
         * @param name the name of the field
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @param fullTextSearchable true if this field is full-text searchable, or false otherwise
         * @return this builder for convenience and method chaining; never null
         */
        public Builder weakReferenceField( Name name,
                                           Field.Store store,
                                           Field.Index index,
                                           boolean fullTextSearchable ) {
            if (store == null) store = Field.Store.YES;
            if (index == null) index = Field.Index.NOT_ANALYZED;
            Rule rule = new TypedRule(FieldType.WEAK_REFERENCE, store, index, false, fullTextSearchable);
            rulesByName.put(name, rule);
            return this;
        }

        protected <T> Builder numericField( Name name,
                                            FieldType type,
                                            Field.Store store,
                                            Field.Index index,
                                            T minValue,
                                            T maxValue ) {
            if (store == null) store = Field.Store.YES;
            if (index == null) index = Field.Index.NOT_ANALYZED;
            Rule rule = new NumericTypedRule<T>(type, store, index, minValue, maxValue);
            rulesByName.put(name, rule);
            return this;
        }

        /**
         * Define a boolean-based field in the indexes. This method will overwrite any existing definition in this builder.
         * 
         * @param name the name of the field
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @return this builder for convenience and method chaining; never null
         */
        public Builder booleanField( Name name,
                                     Field.Store store,
                                     Field.Index index ) {
            return numericField(name, FieldType.BOOLEAN, store, index, Boolean.FALSE, Boolean.TRUE);
        }

        /**
         * Define a integer-based field in the indexes. This method will overwrite any existing definition in this builder.
         * 
         * @param name the name of the field
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @param minValue the minimum value for this field, or null if there is no minimum value
         * @param maxValue the maximum value for this field, or null if there is no maximum value
         * @return this builder for convenience and method chaining; never null
         */
        public Builder integerField( Name name,
                                     Field.Store store,
                                     Field.Index index,
                                     Integer minValue,
                                     Integer maxValue ) {
            if (minValue == null) minValue = Integer.MIN_VALUE;
            if (maxValue == null) maxValue = Integer.MAX_VALUE;
            return numericField(name, FieldType.INT, store, index, minValue, maxValue);
        }

        /**
         * Define a long-based field in the indexes. This method will overwrite any existing definition in this builder.
         * 
         * @param name the name of the field
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @param minValue the minimum value for this field, or null if there is no minimum value
         * @param maxValue the maximum value for this field, or null if there is no maximum value
         * @return this builder for convenience and method chaining; never null
         */
        public Builder longField( Name name,
                                  Field.Store store,
                                  Field.Index index,
                                  Long minValue,
                                  Long maxValue ) {
            if (minValue == null) minValue = Long.MIN_VALUE;
            if (maxValue == null) maxValue = Long.MAX_VALUE;
            return numericField(name, FieldType.LONG, store, index, minValue, maxValue);
        }

        /**
         * Define a decimal-based field in the indexes. This method will overwrite any existing definition in this builder.
         * <p>
         * Decimal fields can contain an exceedingly large range of values, and because Lucene is not capable of performing range
         * queries using BigDecimal values, decimal fields are stored as lexicographically-sortable strings.
         * 
         * @param name the name of the field
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @param minValue the minimum value for this field, or null if there is no minimum value
         * @param maxValue the maximum value for this field, or null if there is no maximum value
         * @return this builder for convenience and method chaining; never null
         */
        public Builder decimalField( Name name,
                                     Field.Store store,
                                     Field.Index index,
                                     BigDecimal minValue,
                                     BigDecimal maxValue ) {
            if (store == null) store = Field.Store.YES;
            if (index == null) index = Field.Index.NOT_ANALYZED;
            Rule rule = new TypedRule(FieldType.STRING, store, index, false, false);
            rulesByName.put(name, rule);
            return this;
        }

        /**
         * Define a date-based field in the indexes. This method will overwrite any existing definition in this builder.
         * 
         * @param name the name of the field
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @param minValue the minimum value for this field, or null if there is no minimum value
         * @param maxValue the maximum value for this field, or null if there is no maximum value
         * @return this builder for convenience and method chaining; never null
         */
        public Builder dateField( Name name,
                                  Field.Store store,
                                  Field.Index index,
                                  Long minValue,
                                  Long maxValue ) {
            if (minValue == null) minValue = 0L;
            if (maxValue == null) maxValue = Long.MAX_VALUE;
            return numericField(name, FieldType.DATE, store, index, minValue, maxValue);
        }

        /**
         * Define a float-based field in the indexes. This method will overwrite any existing definition in this builder.
         * 
         * @param name the name of the field
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @param minValue the minimum value for this field, or null if there is no minimum value
         * @param maxValue the maximum value for this field, or null if there is no maximum value
         * @return this builder for convenience and method chaining; never null
         */
        public Builder floatField( Name name,
                                   Field.Store store,
                                   Field.Index index,
                                   Float minValue,
                                   Float maxValue ) {
            if (minValue == null) minValue = Float.MIN_VALUE;
            if (maxValue == null) maxValue = Float.MAX_VALUE;
            return numericField(name, FieldType.FLOAT, store, index, minValue, maxValue);
        }

        /**
         * Define a double-based field in the indexes. This method will overwrite any existing definition in this builder.
         * 
         * @param name the name of the field
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @param minValue the minimum value for this field, or null if there is no minimum value
         * @param maxValue the maximum value for this field, or null if there is no maximum value
         * @return this builder for convenience and method chaining; never null
         */
        public Builder doubleField( Name name,
                                    Field.Store store,
                                    Field.Index index,
                                    Double minValue,
                                    Double maxValue ) {
            if (minValue == null) minValue = Double.MIN_VALUE;
            if (maxValue == null) maxValue = Double.MAX_VALUE;
            return numericField(name, FieldType.DOUBLE, store, index, minValue, maxValue);
        }

        /**
         * Define a integer-based field in the indexes. This method will overwrite any existing definition in this builder.
         * 
         * @param name the name of the field
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @param minValue the minimum value for this field, or null if there is no minimum value
         * @return this builder for convenience and method chaining; never null
         */
        public Builder integerField( Name name,
                                     Field.Store store,
                                     Field.Index index,
                                     Integer minValue ) {
            return integerField(name, store, index, minValue, null);
        }

        /**
         * Define a long-based field in the indexes. This method will overwrite any existing definition in this builder.
         * 
         * @param name the name of the field
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @param minValue the minimum value for this field, or null if there is no minimum value
         * @return this builder for convenience and method chaining; never null
         */
        public Builder longField( Name name,
                                  Field.Store store,
                                  Field.Index index,
                                  Long minValue ) {
            return longField(name, store, index, minValue, null);
        }

        /**
         * Define a date-based field in the indexes. This method will overwrite any existing definition in this builder.
         * 
         * @param name the name of the field
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @param minValue the minimum value for this field, or null if there is no minimum value
         * @return this builder for convenience and method chaining; never null
         */
        public Builder dateField( Name name,
                                  Field.Store store,
                                  Field.Index index,
                                  Long minValue ) {
            return dateField(name, store, index, minValue, null);
        }

        /**
         * Define a float-based field in the indexes. This method will overwrite any existing definition in this builder.
         * 
         * @param name the name of the field
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @param minValue the minimum value for this field, or null if there is no minimum value
         * @return this builder for convenience and method chaining; never null
         */
        public Builder floatField( Name name,
                                   Field.Store store,
                                   Field.Index index,
                                   Float minValue ) {
            return floatField(name, store, index, minValue, null);
        }

        /**
         * Define a double-based field in the indexes. This method will overwrite any existing definition in this builder.
         * 
         * @param name the name of the field
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @param minValue the minimum value for this field, or null if there is no minimum value
         * @return this builder for convenience and method chaining; never null
         */
        public Builder doubleField( Name name,
                                    Field.Store store,
                                    Field.Index index,
                                    Double minValue ) {
            return doubleField(name, store, index, minValue, null);
        }

        /**
         * Define a integer-based field in the indexes. This method will overwrite any existing definition in this builder.
         * 
         * @param name the name of the field
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @return this builder for convenience and method chaining; never null
         */
        public Builder integerField( Name name,
                                     Field.Store store,
                                     Field.Index index ) {
            return integerField(name, store, index, null, null);
        }

        /**
         * Define a long-based field in the indexes. This method will overwrite any existing definition in this builder.
         * 
         * @param name the name of the field
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @return this builder for convenience and method chaining; never null
         */
        public Builder longField( Name name,
                                  Field.Store store,
                                  Field.Index index ) {
            return longField(name, store, index, null, null);
        }

        /**
         * Define a date-based field in the indexes. This method will overwrite any existing definition in this builder.
         * 
         * @param name the name of the field
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @return this builder for convenience and method chaining; never null
         */
        public Builder dateField( Name name,
                                  Field.Store store,
                                  Field.Index index ) {
            return dateField(name, store, index, null, null);
        }

        /**
         * Define a float-based field in the indexes. This method will overwrite any existing definition in this builder.
         * 
         * @param name the name of the field
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @return this builder for convenience and method chaining; never null
         */
        public Builder floatField( Name name,
                                   Field.Store store,
                                   Field.Index index ) {
            return floatField(name, store, index, null, null);
        }

        /**
         * Define a double-based field in the indexes. This method will overwrite any existing definition in this builder.
         * 
         * @param name the name of the field
         * @param store the storage setting, or null if the field should be {@link Store#YES stored}
         * @param index the index setting, or null if the field should be indexed but {@link Index#NOT_ANALYZED not analyzed}
         * @return this builder for convenience and method chaining; never null
         */
        public Builder doubleField( Name name,
                                    Field.Store store,
                                    Field.Index index ) {
            return doubleField(name, store, index, null, null);
        }

        /**
         * Build the indexing rules.
         * 
         * @return the immutable indexing rules.
         */
        public IndexRules build() {
            return new IndexRules(Collections.unmodifiableMap(new HashMap<Name, Rule>(rulesByName)), defaultRule);
        }
    }
}
