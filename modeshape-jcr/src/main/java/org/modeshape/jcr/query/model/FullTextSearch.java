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
package org.modeshape.jcr.query.model;

import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Calendar;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import javax.jcr.Binary;
import javax.jcr.PropertyType;
import javax.jcr.RepositoryException;
import javax.jcr.Value;
import javax.jcr.ValueFormatException;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.common.text.ParsingException;
import org.modeshape.common.util.CheckArg;
import org.modeshape.common.util.HashCode;
import org.modeshape.common.util.ObjectUtil;
import org.modeshape.jcr.query.engine.ScanningQueryEngine;
import org.modeshape.jcr.query.parse.FullTextSearchParser;

/**
 * A constraint that evaluates to true only when a full-text search applied to the search scope results in positive findings. If a
 * property name is supplied, then the search is limited to the value(s) of the named property on the node(s) in the search scope.
 */
@Immutable
public class FullTextSearch implements Constraint, javax.jcr.query.qom.FullTextSearch {
    private static final long serialVersionUID = 1L;

    protected static String toString( StaticOperand operand ) throws RepositoryException {
        if (operand instanceof javax.jcr.query.qom.Literal) {
            return ((javax.jcr.query.qom.Literal)operand).getLiteralValue().getString();
        }
        return operand.toString();
    }

    private final SelectorName selectorName;
    private final String propertyName;
    private final String fullTextSearchExpression;
    private Term term;
    private final int hc;
    private transient StaticOperand expression;

    /**
     * Create a constraint defining a full-text search against the property values on node within the search scope.
     * 
     * @param selectorName the name of the node selector defining the search scope
     * @param propertyName the name of the property to be searched; may be null if all property values are to be searched
     * @param fullTextSearchExpression the search expression
     * @param term the term representation, if it is known; may be null
     * @throws RepositoryException if there is an error converting the full text search expression to a string
     */
    public FullTextSearch( SelectorName selectorName,
                           String propertyName,
                           StaticOperand fullTextSearchExpression,
                           Term term ) throws RepositoryException {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotNull(fullTextSearchExpression, "fullTextSearchExpression");
        String expressionString = toString(fullTextSearchExpression);
        CheckArg.isNotEmpty(expressionString, "fullTextSearchExpression");
        this.selectorName = selectorName;
        this.propertyName = propertyName;
        this.term = term;
        this.fullTextSearchExpression = expressionString;
        this.hc = HashCode.compute(this.selectorName, this.propertyName, this.fullTextSearchExpression);
        this.expression = fullTextSearchExpression;
    }

    /**
     * Create a constraint defining a full-text search against the property values on node within the search scope.
     * 
     * @param selectorName the name of the node selector defining the search scope
     * @param propertyName the name of the property to be searched; may be null if all property values are to be searched
     * @param expressionString the string form of the full text search expression; may not be null or empty
     * @param fullTextSearchExpression the search expression
     */
    public FullTextSearch( SelectorName selectorName,
                           String propertyName,
                           String expressionString,
                           StaticOperand fullTextSearchExpression) {
        CheckArg.isNotNull(selectorName, "selectorName");
        CheckArg.isNotNull(fullTextSearchExpression, "fullTextSearchExpression");
        CheckArg.isNotEmpty(expressionString, "expressionString");
        this.selectorName = selectorName;
        this.propertyName = propertyName;
        this.fullTextSearchExpression = expressionString;
        this.hc = HashCode.compute(this.selectorName, this.propertyName, this.fullTextSearchExpression);
        this.expression = fullTextSearchExpression;
    }

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

    @Override
    public String getSelectorName() {
        return selectorName.getString();
    }

    @Override
    public final String getPropertyName() {
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

    @Override
    public StaticOperand getFullTextSearchExpression() {
        if (expression == null) {
            // This is idempotent, so we don't need to worry about concurrently setting the value ...
            this.expression = new Literal(new Value() {

                @Override
                public int getType() {
                    return PropertyType.STRING;
                }

                @Override
                public String getString() {
                    return fullTextSearchExpression();
                }

                @Override
                @SuppressWarnings("deprecation")
                public InputStream getStream() throws RepositoryException {
                    throw new ValueFormatException();
                }

                @Override
                public long getLong() throws ValueFormatException, RepositoryException {
                    throw new ValueFormatException();
                }

                @Override
                public double getDouble() throws ValueFormatException, RepositoryException {
                    throw new ValueFormatException();
                }

                @Override
                public BigDecimal getDecimal() throws ValueFormatException, RepositoryException {
                    throw new ValueFormatException();
                }

                @Override
                public Calendar getDate() throws ValueFormatException, RepositoryException {
                    throw new ValueFormatException();
                }

                @Override
                public boolean getBoolean() throws ValueFormatException, RepositoryException {
                    throw new ValueFormatException();
                }

                @Override
                public Binary getBinary() throws RepositoryException {
                    throw new ValueFormatException();
                }
            });
        }
        return expression;
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

    @Override
    public String toString() {
        return Visitors.readable(this);
    }

    @Override
    public int hashCode() {
        return hc;
    }

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

    public FullTextSearch withFullTextExpression( String expression ) {
        return new FullTextSearch(selectorName, propertyName, expression);
    }

    @Override
    public void accept( Visitor visitor ) {
        visitor.visit(this);
    }

    /**
     * The general notion of a term that makes up a full-text search.
     */
    public static interface Term {
        /**
         * Checks if the term matches (from a FTS perspective) the given value.
         *
         * @param value a non-null string
         * @return {@code true} if the term matches the value, {@code false} otherwise
         */
        public boolean matches(String value);
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

        @Override
        public boolean matches( String value ) {
            return !negated.matches(value);
        }

        @Override
        public int hashCode() {
            return negated.hashCode();
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof NegationTerm) {
                NegationTerm that = (NegationTerm)obj;
                return this.getNegatedTerm().equals(that.getNegatedTerm());
            }
            return false;
        }

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
        private final Pattern pattern;

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
            this.pattern = Pattern.compile(regexFromValue(), Pattern.CASE_INSENSITIVE | Pattern.DOTALL | Pattern.UNICODE_CASE);
        }

        private String regexFromValue() {
            String value = this.value;
            //parse a LIKE-style expression around the value which should ensure that any JCR wildcards are converted
            //to regex wildcards
            if (!value.startsWith("%") && !value.startsWith("*")) {
                value = "%" + value;
            }
            if (!value.endsWith("%") && !value.endsWith("*")) {
                value = value + "%";
            }
            return ScanningQueryEngine.toRegularExpression(value);
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

        @Override
        public boolean matches( String value ) {
            return pattern.matcher(value).matches();
        }

        @Override
        public int hashCode() {
            return value.hashCode();
        }

        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof SimpleTerm) {
                SimpleTerm that = (SimpleTerm)obj;
                return this.getValue().equals(that.getValue());
            }
            return false;
        }

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

        @Override
        public Iterator<Term> iterator() {
            return terms.iterator();
        }

        @Override
        public int hashCode() {
            return terms.hashCode();
        }

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

        @Override
        public boolean matches( String value ) {
            for (Term term : getTerms()) {
                if (term.matches(value)) {
                    return true;
                }
            }
            return false;
        }

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

        @Override
        public boolean matches( String value ) {
            for (Term term : getTerms()) {
                if (!term.matches(value)) {
                    return false;
                }
            }
            return true;
        }

        @Override
        public String toString() {
            return toString("AND");
        }
    }

}
