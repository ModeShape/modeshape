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
package org.modeshape.search.lucene.query;

import java.io.IOException;
import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.regex.Pattern;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.Searcher;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.regex.JavaUtilRegexCapabilities;
import org.apache.lucene.search.regex.RegexQuery;
import org.modeshape.graph.property.ValueComparators;
import org.modeshape.graph.property.ValueFactories;
import org.modeshape.graph.property.ValueFactory;
import org.modeshape.graph.query.model.Comparison;

/**
 * A Lucene {@link Query} implementation that is used to apply a {@link Comparison} constraint against a string field. This query
 * implementation works by using the {@link Query#weight(Searcher) weight} and
 * {@link Weight#scorer(IndexReader, boolean, boolean) scorer} of the wrapped query to score (and return) only those documents
 * with string fields that satisfy the constraint.
 */
public class CompareStringQuery extends CompareQuery<String> {

    private static final long serialVersionUID = 1L;
    protected static final Evaluator<String> EQUAL_TO = new Evaluator<String>() {
        private static final long serialVersionUID = 1L;

        public boolean satisfiesConstraint( String nodeValue,
                                            String constraintValue ) {
            return constraintValue.equals(nodeValue);
        }

        @Override
        public String toString() {
            return "=";
        }
    };
    protected static final Evaluator<String> IS_LESS_THAN = new Evaluator<String>() {
        private static final long serialVersionUID = 1L;

        public boolean satisfiesConstraint( String nodeValue,
                                            String constraintValue ) {
            return ValueComparators.STRING_COMPARATOR.compare(nodeValue, constraintValue) < 0;
        }

        @Override
        public String toString() {
            return "<";
        }
    };
    protected static final Evaluator<String> IS_LESS_THAN_OR_EQUAL_TO = new Evaluator<String>() {
        private static final long serialVersionUID = 1L;

        public boolean satisfiesConstraint( String nodeValue,
                                            String constraintValue ) {
            return ValueComparators.STRING_COMPARATOR.compare(nodeValue, constraintValue) <= 0;
        }

        @Override
        public String toString() {
            return "<=";
        }
    };
    protected static final Evaluator<String> IS_GREATER_THAN = new Evaluator<String>() {
        private static final long serialVersionUID = 1L;

        public boolean satisfiesConstraint( String nodeValue,
                                            String constraintValue ) {
            return ValueComparators.STRING_COMPARATOR.compare(nodeValue, constraintValue) > 0;
        }

        @Override
        public String toString() {
            return ">";
        }
    };
    protected static final Evaluator<String> IS_GREATER_THAN_OR_EQUAL_TO = new Evaluator<String>() {
        private static final long serialVersionUID = 1L;

        public boolean satisfiesConstraint( String nodeValue,
                                            String constraintValue ) {
            return ValueComparators.STRING_COMPARATOR.compare(nodeValue, constraintValue) >= 0;
        }

        @Override
        public String toString() {
            return ">=";
        }
    };

    /**
     * Construct a {@link Query} implementation that scores documents with a string field value that is equal to the supplied
     * constraint value.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseSensitive true if the comparison should be done in a case-sensitive manner, or false if it is to be
     *        case-insensitive
     * @return the query; never null
     */
    public static Query createQueryForNodesWithFieldEqualTo( String constraintValue,
                                                             String fieldName,
                                                             ValueFactories factories,
                                                             boolean caseSensitive ) {
        if (caseSensitive) {
            // We can just do a normal TermQuery ...
            return new TermQuery(new Term(fieldName, constraintValue));
        }
        return new CompareStringQuery(fieldName, constraintValue, factories.getStringFactory(), factories.getStringFactory(),
                                      EQUAL_TO, caseSensitive);
    }

    /**
     * Construct a {@link Query} implementation that scores documents with a string field value that is greater than the supplied
     * constraint value.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseSensitive true if the comparison should be done in a case-sensitive manner, or false if it is to be
     *        case-insensitive
     * @return the query; never null
     */
    public static CompareStringQuery createQueryForNodesWithFieldGreaterThan( String constraintValue,
                                                                              String fieldName,
                                                                              ValueFactories factories,
                                                                              boolean caseSensitive ) {
        return new CompareStringQuery(fieldName, constraintValue, factories.getStringFactory(), factories.getStringFactory(),
                                      IS_GREATER_THAN, caseSensitive);
    }

    /**
     * Construct a {@link Query} implementation that scores documents with a string field value that is greater than or equal to
     * the supplied constraint value.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseSensitive true if the comparison should be done in a case-sensitive manner, or false if it is to be
     *        case-insensitive
     * @return the query; never null
     */
    public static CompareStringQuery createQueryForNodesWithFieldGreaterThanOrEqualTo( String constraintValue,
                                                                                       String fieldName,
                                                                                       ValueFactories factories,
                                                                                       boolean caseSensitive ) {
        return new CompareStringQuery(fieldName, constraintValue, factories.getStringFactory(), factories.getStringFactory(),
                                      IS_GREATER_THAN_OR_EQUAL_TO, caseSensitive);
    }

    /**
     * Construct a {@link Query} implementation that scores documents with a string field value that is less than the supplied
     * constraint value.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseSensitive true if the comparison should be done in a case-sensitive manner, or false if it is to be
     *        case-insensitive
     * @return the query; never null
     */
    public static CompareStringQuery createQueryForNodesWithFieldLessThan( String constraintValue,
                                                                           String fieldName,
                                                                           ValueFactories factories,
                                                                           boolean caseSensitive ) {
        return new CompareStringQuery(fieldName, constraintValue, factories.getStringFactory(), factories.getStringFactory(),
                                      IS_LESS_THAN, caseSensitive);
    }

    /**
     * Construct a {@link Query} implementation that scores documents with a string field value that is less than or equal to the
     * supplied constraint value.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseSensitive true if the comparison should be done in a case-sensitive manner, or false if it is to be
     *        case-insensitive
     * @return the query; never null
     */
    public static CompareStringQuery createQueryForNodesWithFieldLessThanOrEqualTo( String constraintValue,
                                                                                    String fieldName,
                                                                                    ValueFactories factories,
                                                                                    boolean caseSensitive ) {
        return new CompareStringQuery(fieldName, constraintValue, factories.getStringFactory(), factories.getStringFactory(),
                                      IS_LESS_THAN_OR_EQUAL_TO, caseSensitive);
    }

    protected static boolean hasWildcardCharacters( String expression ) {
        CharacterIterator iter = new StringCharacterIterator(expression);
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
     * Construct a {@link Query} implementation that scores documents with a string field value that is LIKE the supplied
     * constraint value, where the LIKE expression contains the SQL wildcard characters '%' and '_' or the regular expression
     * wildcard characters '*' and '?'.
     * 
     * @param likeExpression the LIKE expression; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseSensitive true if the comparison should be done in a case-sensitive manner, or false if it is to be
     *        case-insensitive
     * @return the query; never null
     */
    public static Query createQueryForNodesWithFieldLike( String likeExpression,
                                                          String fieldName,
                                                          ValueFactories factories,
                                                          boolean caseSensitive ) {
        assert likeExpression != null;
        assert likeExpression.length() > 0;

        if (!hasWildcardCharacters(likeExpression)) {
            // This is not a like expression, so just do an equals ...
            return createQueryForNodesWithFieldEqualTo(likeExpression, fieldName, factories, caseSensitive);
        }
        if (caseSensitive) {
            // We can just do a normal Wildcard or RegEx query ...

            // '%' matches 0 or more characters
            // '_' matches any single character
            // '\x' matches 'x'
            // all other characters match themselves

            // Wildcard queries are a better match, but they can be slow and should not be used
            // if the first character of the expression is a '%' or '_' or '*' or '?' ...
            char firstChar = likeExpression.charAt(0);
            if (firstChar != '%' && firstChar != '_' && firstChar != '*' && firstChar != '?') {
                // Create a wildcard query ...
                String expression = toWildcardExpression(likeExpression);
                return new WildcardQuery(new Term(fieldName, expression));
            }
        }
        // Create a regex query (which will be done using the correct case) ...
        String regex = toRegularExpression(likeExpression);
        RegexQuery query = new RegexQuery(new Term(fieldName, regex));
        int flags = caseSensitive ? 0 : Pattern.CASE_INSENSITIVE;
        query.setRegexImplementation(new JavaUtilRegexCapabilities(flags));
        return query;
    }

    /**
     * Convert the JCR like expression to a Lucene wildcard expression. The JCR like expression uses '%' to match 0 or more
     * characters, '_' to match any single character, '\x' to match the 'x' character, and all other characters to match
     * themselves.
     * 
     * @param likeExpression the like expression; may not be null
     * @return the expression that can be used with a WildcardQuery; never null
     */
    protected static String toWildcardExpression( String likeExpression ) {
        return likeExpression.replace('%', '*').replace('_', '?').replaceAll("\\\\(.)", "$1");
    }

    /**
     * Convert the JCR like expression to a regular expression. The JCR like expression uses '%' to match 0 or more characters,
     * '_' to match any single character, '\x' to match the 'x' character, and all other characters to match themselves. Note that
     * if any regex metacharacters appear in the like expression, they will be escaped within the resulting regular expression.
     * 
     * @param likeExpression the like expression; may not be null
     * @return the expression that can be used with a WildcardQuery; never null
     */
    protected static String toRegularExpression( String likeExpression ) {
        // Replace all '\x' with 'x' ...
        String result = likeExpression.replaceAll("\\\\(.)", "$1");
        // Escape characters used as metacharacters in regular expressions, including
        // '[', '^', '\', '$', '.', '|', '+', '(', and ')'
        // But leave '?' and '*'
        result = result.replaceAll("([$.|+()\\[\\\\^\\\\\\\\])", "\\\\$1");
        // Replace '%'->'[.]*' and '_'->'[.]
        // (order of these calls is important!)
        result = result.replace("*", ".*").replace("?", ".");
        result = result.replace("%", ".*").replace("_", ".");
        return result;
    }

    private final boolean caseSensitive;

    /**
     * Construct a {@link Query} implementation that scores nodes according to the supplied comparator.
     * 
     * @param fieldName the name of the document field containing the value; may not be null
     * @param constraintValue the constraint value; may not be null
     * @param valueFactory the value factory that can be used during the scoring; may not be null
     * @param stringFactory the string factory that can be used during the scoring; may not be null
     * @param evaluator the {@link CompareQuery.Evaluator} implementation that returns whether the node path satisfies the
     *        constraint; may not be null
     * @param caseSensitive true if the comparison should be done in a case-sensitive manner, or false if it is to be
     *        case-insensitive
     */
    protected CompareStringQuery( String fieldName,
                                  String constraintValue,
                                  ValueFactory<String> valueFactory,
                                  ValueFactory<String> stringFactory,
                                  Evaluator<String> evaluator,
                                  boolean caseSensitive ) {
        super(fieldName, caseSensitive ? constraintValue : constraintValue.toLowerCase(), valueFactory, stringFactory, evaluator);
        this.caseSensitive = caseSensitive;
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.modeshape.search.lucene.query.CompareQuery#readFromDocument(org.apache.lucene.index.IndexReader, int)
     */
    @Override
    protected String readFromDocument( IndexReader reader,
                                       int docId ) throws IOException {
        String result = super.readFromDocument(reader, docId);
        if (result == null) return null;
        return caseSensitive ? result : result.toLowerCase();
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.lucene.search.Query#clone()
     */
    @Override
    public Object clone() {
        return new CompareStringQuery(fieldName, constraintValue, valueTypeFactory, stringFactory, evaluator, caseSensitive);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.apache.lucene.search.Query#toString(java.lang.String)
     */
    @Override
    public String toString( String field ) {
        return "(" + fieldName + evaluator.toString() + "'"
               + (stringFactory != null ? stringFactory.create(constraintValue) : constraintValue.toString()) + "')";
    }
}
