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
package org.jboss.dna.graph.query.model;

import java.util.Iterator;
import java.util.List;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.text.ParsingException;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.ObjectUtil;
import org.jboss.dna.graph.property.Name;
import org.jboss.dna.graph.query.parse.FullTextSearchParser;

/**
 * A constraint that evaluates to true only when a full-text search applied to the search scope results in positive findings. If a
 * property name is supplied, then the search is limited to the value(s) of the named property on the node(s) in the search scope.
 */
@Immutable
public class FullTextSearch extends Constraint {
    private final SelectorName selectorName;
    private final Name propertyName;
    private final String fullTextSearchExpression;
    private Term term;

    /**
     * Create a constraint defining a full-text search against the property values on node within the search scope.
     * 
     * @param selectorName the name of the node selector defining the search scope
     * @param propertyName the name of the property to be searched; may be null if all property values are to be searched
     * @param fullTextSearchExpression the search expression
     * @param term the term representation, if it is known; may be null
     */
    public FullTextSearch( SelectorName selectorName,
                           Name propertyName,
                           String fullTextSearchExpression,
                           Term term ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotEmpty(fullTextSearchExpression, "fullTextSearchExpression");
        this.selectorName = selectorName;
        this.propertyName = propertyName;
        this.fullTextSearchExpression = fullTextSearchExpression;
    }

    /**
     * Create a constraint defining a full-text search against the property values on node within the search scope.
     * 
     * @param selectorName the name of the node selector defining the search scope
     * @param propertyName the name of the property to be searched; may be null if all property values are to be searched
     * @param fullTextSearchExpression the search expression
     */
    public FullTextSearch( SelectorName selectorName,
                           Name propertyName,
                           String fullTextSearchExpression ) {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotEmpty(fullTextSearchExpression, "fullTextSearchExpression");
        this.selectorName = selectorName;
        this.propertyName = propertyName;
        this.fullTextSearchExpression = fullTextSearchExpression;
        this.term = null;
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
        this.fullTextSearchExpression = fullTextSearchExpression;
    }

    /**
     * @return selectorName
     */
    public final SelectorName getSelectorName() {
        return selectorName;
    }

    /**
     * @return propertyName
     */
    public final Name getPropertyName() {
        return propertyName;
    }

    /**
     * @return fullTextSearchExpression
     */
    public final String getFullTextSearchExpression() {
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
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof FullTextSearch) {
            FullTextSearch that = (FullTextSearch)obj;
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
     * @see org.jboss.dna.graph.query.model.Visitable#accept(org.jboss.dna.graph.query.model.Visitor)
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
     * A {@link Term} that represents a single search term. The term may be comprised of multiple words.
     */
    public static class SimpleTerm implements Term {
        private final String value;
        private final boolean excluded;
        private final boolean quoted;

        /**
         * Create a simple term with the value and whether the term is excluded or included.
         * 
         * @param value the value that makes up the term
         * @param excluded true if the term should not appear, or false if the term is required
         */
        public SimpleTerm( String value,
                           boolean excluded ) {
            assert value != null;
            assert value.trim().length() > 0;
            this.value = value;
            this.excluded = excluded;
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
         * Get whether or not this term is expected to appear in the results.
         * 
         * @return true if the term is expected to not appear, or false if the term is expected to appear
         */
        public boolean isExcluded() {
            return excluded;
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
                if (this.isExcluded() != that.isExcluded()) return false;
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
            String value = quoted ? "\"" + this.value + "\"" : this.value;
            return excluded ? "-" + value : value;
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
     * A set of {@link Term}s that are ANDed together.
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
