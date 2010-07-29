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
package org.modeshape.graph.query.model;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Iterator;
import java.util.List;
import net.jcip.annotations.Immutable;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.common.util.ObjectUtil;
import org.modeshape.graph.query.parse.FullTextSearchParser;

/**
 * A constraint that evaluates to true only when a full-text search applied to the search scope results in positive findings. If a
 * property name is supplied, then the search is limited to the value(s) of the named property on the node(s) in the search scope.
 */
@Immutable
public class FullTextSearch implements Constraint {
    private static final long serialVersionUID = 1L;

    private final SelectorName selectorName;
    private final String propertyName;
    private final String fullTextSearchExpression;
    private Term term;
    private final int hc;

    /**
     * Create a constraint defining a full-text search against the property values on node within the search scope.
     * 
     * @param selectorName the name of the node selector defining the search scope
     * @param propertyName the name of the property to be searched; may be null if all property values are to be searched
     * @param fullTextSearchExpression the search expression
     * @param term the term representation, if it is known; may be null
     */
    public FullTextSearch( SelectorName selectorName,
                           String propertyName,
                           String fullTextSearchExpression,
                           Term term ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotEmpty(fullTextSearchExpression, "fullTextSearchExpression");
        this.selectorName = selectorName;
        this.propertyName = propertyName;
        this.term = term;
        this.fullTextSearchExpression = fullTextSearchExpression;
        this.hc = HashCode.compute(this.selectorName, this.propertyName, this.fullTextSearchExpression);
    }

    /**
     * Create a constraint defining a full-text search against the property values on node within the search scope.
     * 
     * @param selectorName the name of the node selector defining the search scope
     * @param propertyName the name of the property to be searched; may be null if all property values are to be searched
     * @param fullTextSearchExpression the search expression
     */
    public FullTextSearch( SelectorName selectorName,
                           String propertyName,
                           String fullTextSearchExpression ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotEmpty(fullTextSearchExpression, "fullTextSearchExpression");
        this.selectorName = selectorName;
        this.propertyName = propertyName;
        this.fullTextSearchExpression = fullTextSearchExpression;
        this.term = null;
        this.hc = HashCode.compute(this.selectorName, this.propertyName, this.fullTextSearchExpression);
    }

    /**
     * Create a constraint defining a full-text search against the node within the search scope.
     * 
     * @param selectorName the name of the node selector defining the search scope
     * @param fullTextSearchExpression the search expression
     */
    public FullTextSearch( SelectorName selectorName,
                           String fullTextSearchExpression ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotEmpty(fullTextSearchExpression, "fullTextSearchExpression");
        this.selectorName = selectorName;
        this.propertyName = null;
        this.term = null;
        this.fullTextSearchExpression = fullTextSearchExpression;
        this.hc = HashCode.compute(this.selectorName, this.propertyName, this.fullTextSearchExpression);
    }

    /**
     * Get the name of the selector that is to be searched
     * 
     * @return the selector name; never null
     */
    public final SelectorName selectorName() {
        return selectorName;
    }

    /**
     * Get the name of the property that is to be searched.
     * 
     * @return the property name; or null if the full-text search is to be performed across all searchable properties
     */
    public final String propertyName() {
        return propertyName;
    }

    /**
     * Get the full-text search expression, as a string.
     * 
     * @return the search expression; never null
     */
    public final String fullTextSearchExpression() {
        return fullTextSearchExpression;
    }

    /**
     * Get the formal {@link Term} representation of the expression.
     * 
     * @return the term representing this search; never null
     * @throws ParsingException if there is an error producing the term representation
     */
    public Term getTerm() {
        // Idempotent, so okay to not lock/synchronize ...
        if (term == null) {
            term = new FullTextSearchParser().parse(fullTextSearchExpression);
        }
        return term;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
        return hc;
    }

    /**
     * {@inheritDoc}
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof FullTextSearch) {
            FullTextSearch that = (FullTextSearch)obj;
            if (this.hc != that.hc) return false;
            if (!this.selectorName.equals(that.selectorName)) return false;
            if (!ObjectUtil.isEqualWithNulls(this.propertyName, that.propertyName)) return false;
            if (!this.fullTextSearchExpression.equals(that.fullTextSearchExpression)) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.graph.query.model.Visitable#accept(org.modeshape.graph.query.model.Visitor)
     */
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }

    /**
     * The general notion of a term that makes up a full-text search.
     */
    public static interface Term {
    }

    /**
     * A {@link Term} that represents a search term that requires another term to not appear.
     */
    public static class NegationTerm implements Term {
        private final Term negated;

        public NegationTerm( Term negatedTerm ) {
            assert negatedTerm != null;
            this.negated = negatedTerm;
        }

        /**
         * Get the term that is negated.
         * 
         * @return the negated term; never null
         */
        public Term getNegatedTerm() {
            return negated;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return negated.hashCode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof NegationTerm) {
                NegationTerm that = (NegationTerm)obj;
                return this.getNegatedTerm().equals(that.getNegatedTerm());
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return "-" + negated.toString();
        }
    }

    /**
     * A {@link Term} that represents a single search term. The term may be comprised of multiple words.
     */
    public static class SimpleTerm implements Term {
        private final String value;
        private final boolean quoted;

        /**
         * Create a simple term with the value and whether the term is excluded or included.
         * 
         * @param value the value that makes up the term
         */
        public SimpleTerm( String value ) {
            assert value != null;
            assert value.trim().length() > 0;
            this.value = value;
            this.quoted = this.value.indexOf(' ') != -1;
        }

        /**
         * Get the value of this term. Note that this is the actual value that is to be searched for, and will not include the
         * {@link #isQuotingRequired() quotes}.
         * 
         * @return the value; never null
         */
        public String getValue() {
            return value;
        }

        /**
         * Get the values of this term if the term is quoted.
         * 
         * @return the array of terms; never null
         */
        public String[] getValues() {
            return value.split("/w");
        }

        /**
         * Get whether this term needs to be quoted because it consists of multiple words.
         * 
         * @return true if the term needs to be quoted, or false otherwise
         */
        public boolean isQuotingRequired() {
            return quoted;
        }

        /**
         * Return whether this term contains any unescaped wildcard characters (e.g., one of '*', '?', '%', or '_').
         * 
         * @return true if this term contains unescaped wildcard characters, or false otherwise
         */
        public boolean containsWildcards() {
            if (this.value.length() == 0) return false;
            CharacterIterator iter = new StringCharacterIterator(this.value);
            boolean skipNext = false;
            for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
                if (skipNext) {
                    skipNext = false;
                    continue;
                }
                if (c == '*' || c == '?' || c == '%' || c == '_') return true;
                if (c == '\\') skipNext = true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return value.hashCode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof SimpleTerm) {
                SimpleTerm that = (SimpleTerm)obj;
                return this.getValue().equals(that.getValue());
            }
            return false;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return quoted ? "\"" + this.value + "\"" : this.value;
        }
    }

    /**
     * A list of {@link Term}s.
     */
    public static abstract class CompoundTerm implements Term, Iterable<Term> {
        private final List<Term> terms;

        /**
         * Create a compound term of the supplied terms.
         * 
         * @param terms the terms; may not be null or empty
         */
        protected CompoundTerm( List<Term> terms ) {
            this.terms = terms;
        }

        /**
         * Get the terms that make up this compound term.
         * 
         * @return the terms in the disjunction; never null and never empty
         */
        public List<Term> getTerms() {
            return terms;
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Iterable#iterator()
         */
        public Iterator<Term> iterator() {
            return terms.iterator();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#hashCode()
         */
        @Override
        public int hashCode() {
            return terms.hashCode();
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#equals(java.lang.Object)
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (this.getClass().isInstance(obj)) {
                CompoundTerm that = (CompoundTerm)obj;
                return this.getTerms().equals(that.getTerms());
            }
            return false;
        }

        protected String toString( String delimiter ) {
            if (terms.size() == 1) return terms.iterator().next().toString();
            StringBuilder sb = new StringBuilder();
            sb.append("( ");
            boolean first = true;
            for (Term term : terms) {
                if (first) first = false;
                else sb.append(' ').append(delimiter).append(' ');
                sb.append(term);
            }
            sb.append(" )");
            return sb.toString();
        }
    }

    /**
     * A set of {@link Term}s that are ORed together.
     */
    public static class Disjunction extends CompoundTerm {

        /**
         * Create a disjunction of the supplied terms.
         * 
         * @param terms the terms to be ORed together; may not be null or empty
         */
        public Disjunction( List<Term> terms ) {
            super(terms);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return toString("OR");
        }
    }

    /**
     * A set of {@link Term}s that are ANDed together.
     */
    public static class Conjunction extends CompoundTerm {

        /**
         * Create a conjunction of the supplied terms.
         * 
         * @param terms the terms to be ANDed together; may not be null or empty
         */
        public Conjunction( List<Term> terms ) {
            super(terms);
        }

        /**
         * {@inheritDoc}
         * 
         * @see java.lang.Object#toString()
         */
        @Override
        public String toString() {
            return toString("AND");
        }
    }

}
