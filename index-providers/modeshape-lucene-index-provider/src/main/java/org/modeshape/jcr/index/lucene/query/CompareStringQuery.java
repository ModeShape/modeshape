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
package org.modeshape.jcr.index.lucene.query;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.regex.Pattern;
import javax.jcr.query.qom.Comparison;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.RegexpQuery;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.WildcardQuery;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.index.lucene.query.CaseOperations.CaseOperation;
import org.modeshape.jcr.value.ValueComparators;
import org.modeshape.jcr.value.ValueFactories;
import org.modeshape.jcr.value.ValueFactory;

/**
 * A Lucene {@link Query} implementation that is used to apply a {@link Comparison} constraint against a string field. This query
 * implementation works by using the weight and {@link Weight#scorer(LeafReaderContext)}  scorer} of the wrapped query
 * to score (and return) only those documents with string fields that satisfy the constraint.
 */
@Immutable
public class CompareStringQuery extends CompareQuery<String> {

    protected static final Evaluator<String> EQUAL_TO = new Evaluator<String>() {
        @Override
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
        @Override
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
        @Override
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
        @Override
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
        @Override
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
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     * evaluated; may not be null
     * @return the query; never null
     */
    public static Query createQueryForNodesWithFieldEqualTo( String constraintValue,
                                                             String fieldName,
                                                             ValueFactories factories,
                                                             CaseOperation caseOperation ) {      
        return new CompareStringQuery(fieldName, constraintValue, factories.getStringFactory(), factories.getStringFactory(),
                                      EQUAL_TO, caseOperation);
    }

    /**
     * Construct a {@link Query} implementation that scores documents with a string field value that is greater than the supplied
     * constraint value.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     *        evaluated; may not be null
     * @return the query; never null
     */
    public static CompareStringQuery createQueryForNodesWithFieldGreaterThan( String constraintValue,
                                                                              String fieldName,
                                                                              ValueFactories factories,
                                                                              CaseOperation caseOperation ) {
        return new CompareStringQuery(fieldName, constraintValue, factories.getStringFactory(), factories.getStringFactory(),
                                      IS_GREATER_THAN, caseOperation);
    }

    /**
     * Construct a {@link Query} implementation that scores documents with a string field value that is greater than or equal to
     * the supplied constraint value.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     *        evaluated; may not be null
     * @return the query; never null
     */
    public static CompareStringQuery createQueryForNodesWithFieldGreaterThanOrEqualTo( String constraintValue,
                                                                                       String fieldName,
                                                                                       ValueFactories factories,
                                                                                       CaseOperation caseOperation ) {
        return new CompareStringQuery(fieldName, constraintValue, factories.getStringFactory(), factories.getStringFactory(),
                                      IS_GREATER_THAN_OR_EQUAL_TO, caseOperation);
    }

    /**
     * Construct a {@link Query} implementation that scores documents with a string field value that is less than the supplied
     * constraint value.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     *        evaluated; may not be null
     * @return the query; never null
     */
    public static CompareStringQuery createQueryForNodesWithFieldLessThan( String constraintValue,
                                                                           String fieldName,
                                                                           ValueFactories factories,
                                                                           CaseOperation caseOperation ) {
        return new CompareStringQuery(fieldName, constraintValue, factories.getStringFactory(), factories.getStringFactory(),
                                      IS_LESS_THAN, caseOperation);
    }

    /**
     * Construct a {@link Query} implementation that scores documents with a string field value that is less than or equal to the
     * supplied constraint value.
     * 
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param factories the value factories that can be used during the scoring; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     *        evaluated; may not be null
     * @return the query; never null
     */
    public static CompareStringQuery createQueryForNodesWithFieldLessThanOrEqualTo( String constraintValue,
                                                                                    String fieldName,
                                                                                    ValueFactories factories,
                                                                                    CaseOperation caseOperation ) {
        return new CompareStringQuery(fieldName, constraintValue, factories.getStringFactory(), factories.getStringFactory(),
                                      IS_LESS_THAN_OR_EQUAL_TO, caseOperation);
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
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     *        evaluated; may not be null
     * @return the query; never null
     */
    public static Query createQueryForNodesWithFieldLike( String likeExpression,
                                                          String fieldName,
                                                          ValueFactories factories,
                                                          CaseOperation caseOperation ) {
        assert likeExpression != null;
        assert likeExpression.length() > 0;

        if (!hasWildcardCharacters(likeExpression)) {
            // This is not a like expression, so just do an equals ...
            return createQueryForNodesWithFieldEqualTo(likeExpression, fieldName, factories, caseOperation);
        }
        if (caseOperation == CaseOperations.AS_IS) {
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
        RegexpQuery query = new RegexpQuery(new Term(fieldName, regex));
        int flags = Pattern.UNICODE_CASE;
        if (caseOperation != CaseOperations.AS_IS) {
            // if we're searching either for the UPPERCASE or LOWERCASE of something, use Case Insensitive matching
            // even though it could produce false positive
            flags = flags | Pattern.CASE_INSENSITIVE;
        } 
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
    public static String toRegularExpression( String likeExpression ) {
        // Replace all '\x' with 'x' ...
        String result = likeExpression.replaceAll("\\\\(.)", "$1");
        // Escape characters used as metacharacters in regular expressions, including
        // '[', '^', '\', '$', '.', '|', '+', '&', '(', and ')'
        // But leave '?' and '*'
        result = result.replaceAll("([$.|+()&\\[\\\\^\\\\\\\\])", "\\\\$1");
        // Replace '%'->'[.]*' and '_'->'[.]
        // (order of these calls is important!)
        result = result.replace("*", ".*").replace("?", ".");
        result = result.replace("%", ".*").replace("_", ".");
        return result;
    }

    /**
     * Construct a {@link Query} implementation that scores nodes according to the supplied comparator.
     * 
     * @param fieldName the name of the document field containing the value; may not be null
     * @param constraintValue the constraint value; may not be null
     * @param valueFactory the value factory that can be used during the scoring; may not be null
     * @param stringFactory the string factory that can be used during the scoring; may not be null
     * @param evaluator the {@link CompareQuery.Evaluator} implementation that returns whether the node path satisfies the
     *        constraint; may not be null
     */
    protected CompareStringQuery( String fieldName,
                                  String constraintValue,
                                  ValueFactory<String> valueFactory,
                                  ValueFactory<String> stringFactory,
                                  Evaluator<String> evaluator,
                                  CaseOperation caseOperation ) {
        super(fieldName, constraintValue, valueFactory, stringFactory, evaluator, caseOperation);
    }

    @Override
    public Query clone() {
        return new CompareStringQuery(field(), constraintValue, valueTypeFactory, stringFactory, evaluator, caseOperation);
    }
}
