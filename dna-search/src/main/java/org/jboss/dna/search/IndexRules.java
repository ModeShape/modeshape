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
package org.jboss.dna.search;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.NotThreadSafe;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.Field.Index;
import org.apache.lucene.document.Field.Store;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.graph.property.Name;

/**
 * The set of rules that dictate how properties should be indexed.
 */
@Immutable
public class IndexRules {

    public static final int INDEX = 2 << 0;
    public static final int ANALYZE = 2 << 1;
    public static final int STORE = 2 << 2;
    public static final int STORE_COMPRESSED = 2 << 3;
    public static final int ANALYZED_WITHOUT_NORMS = 2 << 4;
    public static final int FULL_TEXT = 2 << 5;
    public static final int TREAT_AS_DATE = 2 << 6;

    /**
     * A single rule that dictates how a single property should be indexed.
     * 
     * @see IndexRules#getRule(Name)
     */
    @Immutable
    public static interface Rule {
        /**
         * Return whether this property should be included in the indexes.
         * 
         * @return true if it is to be included, or false otherwise
         */
        boolean isIncluded();

        boolean isAnalyzed();

        boolean isAnalyzedWithoutNorms();

        boolean isStored();

        boolean isStoredCompressed();

        boolean isFullText();

        boolean isDate();

        int getMask();

        Field.Store getStoreOption();

        Field.Index getIndexOption();
    }

    public static final Rule SKIP = new SkipRule();

    @Immutable
    protected static class SkipRule implements Rule {
        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexRules.Rule#getMask()
         */
        public int getMask() {
            return 0;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexRules.Rule#isAnalyzed()
         */
        public boolean isAnalyzed() {
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexRules.Rule#isAnalyzedWithoutNorms()
         */
        public boolean isAnalyzedWithoutNorms() {
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexRules.Rule#isFullText()
         */
        public boolean isFullText() {
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexRules.Rule#isIncluded()
         */
        public boolean isIncluded() {
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexRules.Rule#isStored()
         */
        public boolean isStored() {
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexRules.Rule#isStoredCompressed()
         */
        public boolean isStoredCompressed() {
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexRules.Rule#isDate()
         */
        public boolean isDate() {
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexRules.Rule#getIndexOption()
         */
        public Index getIndexOption() {
            return Field.Index.NO;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexRules.Rule#getStoreOption()
         */
        public Store getStoreOption() {
            return Field.Store.NO;
        }
    }

    @Immutable
    public static final class GeneralRule implements Rule {
        private final int value;
        private final Field.Store store;
        private final Field.Index index;

        protected GeneralRule( int value ) {
            this.value = value;
            this.index = isAnalyzed() ? Field.Index.ANALYZED : Field.Index.NOT_ANALYZED;
            this.store = isStored() ? Field.Store.YES : Field.Store.NO;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexRules.Rule#getMask()
         */
        public int getMask() {
            return value;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexRules.Rule#isAnalyzed()
         */
        public boolean isAnalyzed() {
            return (value & ANALYZE) == ANALYZE;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexRules.Rule#isAnalyzedWithoutNorms()
         */
        public boolean isAnalyzedWithoutNorms() {
            return (value & ANALYZED_WITHOUT_NORMS) == ANALYZED_WITHOUT_NORMS;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexRules.Rule#isFullText()
         */
        public boolean isFullText() {
            return (value & FULL_TEXT) == FULL_TEXT;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexRules.Rule#isIncluded()
         */
        public boolean isIncluded() {
            return true;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexRules.Rule#isStored()
         */
        public boolean isStored() {
            return (value & STORE) == STORE;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexRules.Rule#isStoredCompressed()
         */
        public boolean isStoredCompressed() {
            return (value & STORE_COMPRESSED) == STORE_COMPRESSED;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexRules.Rule#isDate()
         */
        public boolean isDate() {
            return (value & TREAT_AS_DATE) == TREAT_AS_DATE;
        }

        protected Rule with( int options ) {
            return createRule(value | options);
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexRules.Rule#getIndexOption()
         */
        public Index getIndexOption() {
            return index;
        }

        /**
         * {@inheritDoc}
         * 
         * @see org.jboss.dna.search.IndexRules.Rule#getStoreOption()
         */
        public Store getStoreOption() {
            return store;
        }
    }

    private static final ConcurrentHashMap<Integer, Rule> CACHE = new ConcurrentHashMap<Integer, Rule>();

    protected static Rule createRule( int value ) {
        if (value <= 0) {
            return SKIP;
        }
        Integer key = new Integer(value);
        Rule rule = CACHE.get(key);
        if (rule == null) {
            Rule newRule = new GeneralRule(value);
            rule = CACHE.putIfAbsent(value, newRule);
            if (rule == null) rule = newRule;
        }
        return rule;
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
        return new Builder(new HashMap<Name, Rule>());
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
        return new Builder(initialRules.rulesByName).defaultTo(initialRules.defaultRule);
    }

    /**
     * A builder of immutable {@link IndexRules} objects.
     */
    @NotThreadSafe
    public static class Builder {
        private final Map<Name, Rule> rulesByName;
        private Rule defaultRule;

        Builder( Map<Name, Rule> rulesByName ) {
            assert rulesByName != null;
            this.rulesByName = rulesByName;
        }

        /**
         * Set the default rules.
         * 
         * @param rule the default rule to use
         * @return this builder for convenience and method chaining; never null
         * @throws IllegalArgumentException if the rule mask is negative
         */
        public Builder defaultTo( Rule rule ) {
            CheckArg.isNotNull(rule, "rule");
            defaultRule = rule;
            return this;
        }

        /**
         * Set the default rules.
         * 
         * @param ruleMask the bitmask of rule to use
         * @return this builder for convenience and method chaining; never null
         * @throws IllegalArgumentException if the rule mask is negative
         */
        public Builder defaultTo( int ruleMask ) {
            CheckArg.isNonNegative(ruleMask, "options");
            if (ruleMask == 0) {
                defaultRule = SKIP;
            } else {
                // Make sure the index flag is set ...
                ruleMask |= INDEX;
                defaultRule = createRule(ruleMask);
            }
            return this;
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
         * Set the properties with the supplied names to use the supplied rules.
         * 
         * @param ruleMask the bitmask of rules to use
         * @param namesToIndex the names of the properties that are to be skipped
         * @return this builder for convenience and method chaining; never null
         * @throws IllegalArgumentException if the rule mask is negative
         */
        public Builder set( int ruleMask,
                            Name... namesToIndex ) {
            CheckArg.isNonNegative(ruleMask, "options");
            if (namesToIndex != null) {
                if (ruleMask > 0) {
                    skip(namesToIndex);
                } else {
                    // Make sure the index flag is set ...
                    ruleMask |= INDEX;
                    Rule rule = createRule(ruleMask);
                    for (Name name : namesToIndex) {
                        rulesByName.put(name, rule);
                    }
                }
            }
            return this;
        }

        /**
         * Mark the properties with the supplied names to use the supplied rule mask. This does not remove any other rules for
         * these properties.
         * 
         * @param ruleMask the bitmask of rules to add
         * @param namesToIndex the names of the properties that are to be skipped
         * @return this builder for convenience and method chaining; never null
         * @throws IllegalArgumentException if the rule mask is negative
         */
        public Builder add( int ruleMask,
                            Name... namesToIndex ) {
            CheckArg.isNonNegative(ruleMask, "options");
            if (namesToIndex != null) {
                for (Name name : namesToIndex) {
                    add(name, ruleMask);
                }
            }
            return this;
        }

        /**
         * Mark the properties with the supplied names to be indexed. This does not remove any other rules for these properties.
         * 
         * @param namesToIndex the names of the properties that are to be indexed
         * @return this builder for convenience and method chaining; never null
         */
        public Builder index( Name... namesToIndex ) {
            if (namesToIndex != null) {
                for (Name name : namesToIndex) {
                    add(name, INDEX);
                }
            }
            return this;
        }

        /**
         * Mark the properties with the supplied names to be analyzed (and obviously indexed). This does not remove any other
         * rules for these properties.
         * 
         * @param namesToIndex the names of the properties that are to be analyzed
         * @return this builder for convenience and method chaining; never null
         */
        public Builder analyze( Name... namesToIndex ) {
            if (namesToIndex != null) {
                for (Name name : namesToIndex) {
                    add(name, ANALYZE | INDEX);
                }
            }
            return this;
        }

        /**
         * Mark the properties with the supplied names to be stored (and obviously indexed). This does not remove any other rules
         * for these properties.
         * 
         * @param namesToIndex the names of the properties that are to be stored
         * @return this builder for convenience and method chaining; never null
         */
        public Builder store( Name... namesToIndex ) {
            if (namesToIndex != null) {
                for (Name name : namesToIndex) {
                    add(name, STORE | INDEX);
                }
            }
            return this;
        }

        /**
         * Mark the properties with the supplied names to be included in full-text searches (and obviously indexed). This does not
         * remove any other rules for these properties.
         * 
         * @param namesToIndex the names of the properties that are to be included in full-text searches
         * @return this builder for convenience and method chaining; never null
         */
        public Builder fullText( Name... namesToIndex ) {
            if (namesToIndex != null) {
                for (Name name : namesToIndex) {
                    add(name, FULL_TEXT | INDEX);
                }
            }
            return this;
        }

        /**
         * Mark the properties with the supplied names to be treated as dates (and obviously indexed). This does not remove any
         * other rules for these properties.
         * 
         * @param namesToIndex the names of the properties that are to be included in full-text searches
         * @return this builder for convenience and method chaining; never null
         */
        public Builder treatAsDates( Name... namesToIndex ) {
            if (namesToIndex != null) {
                for (Name name : namesToIndex) {
                    add(name, TREAT_AS_DATE | INDEX);
                }
            }
            return this;
        }

        /**
         * Mark the properties with the supplied names to be indexed, analyzed and stored. This does not remove any other rules
         * for these properties.
         * 
         * @param namesToIndex the names of the properties that are to be indexed, analyzed and stored
         * @return this builder for convenience and method chaining; never null
         */
        public Builder analyzeAndStore( Name... namesToIndex ) {
            if (namesToIndex != null) {
                for (Name name : namesToIndex) {
                    add(name, INDEX | ANALYZE | STORE);
                }
            }
            return this;
        }

        /**
         * Mark the properties with the supplied names to be indexed, analyzed, stored and included in full-text searches. This
         * does not remove any other rules for these properties.
         * 
         * @param namesToIndex the names of the properties that are to be indexed, analyzed, stored and included in full-text
         *        searches
         * @return this builder for convenience and method chaining; never null
         */
        public Builder analyzeAndStoreAndFullText( Name... namesToIndex ) {
            if (namesToIndex != null) {
                for (Name name : namesToIndex) {
                    add(name, INDEX | ANALYZE | STORE | FULL_TEXT);
                }
            }
            return this;
        }

        /**
         * Mark the properties with the supplied names to be indexed, analyzed and included in full-text searches. This does not
         * remove any other rules for these properties.
         * 
         * @param namesToIndex the names of the properties that are to be indexed, analyzed and included in full-text searches
         * @return this builder for convenience and method chaining; never null
         */
        public Builder analyzeAndFullText( Name... namesToIndex ) {
            if (namesToIndex != null) {
                for (Name name : namesToIndex) {
                    add(name, INDEX | ANALYZE | FULL_TEXT);
                }
            }
            return this;
        }

        /**
         * Mark the properties with the supplied names to be indexed, stored and included in full-text searches. This does not
         * remove any other rules for these properties.
         * 
         * @param namesToIndex the names of the properties that are to be indexed, stored and included in full-text searches
         * @return this builder for convenience and method chaining; never null
         */
        public Builder storeAndFullText( Name... namesToIndex ) {
            if (namesToIndex != null) {
                for (Name name : namesToIndex) {
                    add(name, INDEX | STORE | FULL_TEXT);
                }
            }
            return this;
        }

        protected void add( Name name,
                            int option ) {
            Rule rule = rulesByName.get(name);
            if (rule != null) {
                option |= rule.getMask();
            }
            rulesByName.put(name, createRule(option));
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
