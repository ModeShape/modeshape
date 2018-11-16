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

import static org.modeshape.jcr.value.ValueComparators.STRING_COMPARATOR;

import java.util.Objects;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.function.IntPredicate;
import java.util.regex.Pattern;
import javax.jcr.query.qom.Comparison;
import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TermRangeQuery;
import org.apache.lucene.search.Weight;
import org.apache.lucene.search.WildcardQuery;
import org.modeshape.common.annotation.Immutable;
import org.modeshape.jcr.query.engine.QueryUtil;

/**
 * A Lucene {@link Query} implementation that is used to apply a {@link Comparison} constraint against a string field. This query
 * implementation works by using the weight and {@link Weight#scorer(LeafReaderContext)}  scorer} of the wrapped query
 * to score (and return) only those documents with string fields that satisfy the constraint.
 */
@Immutable
public class CompareStringQuery extends CompareQuery<String> {
    private enum FieldComparison {
        EQ(cmp -> cmp == 0), GT(cmp -> cmp > 0), GE(cmp -> cmp >= 0), LT(cmp -> cmp < 0), LE(cmp -> cmp <= 0);

        final IntPredicate testCmp;

        private FieldComparison(IntPredicate testCmp) {
            this.testCmp = testCmp;
        }

        boolean test(int cmp) {
            return testCmp.test(cmp);
        }

        Query createQueryForNodesWithField(String constraintValue, String fieldName, Function<String, String> caseOperation) {
            constraintValue = QueryUtil.unescape(constraintValue);
            if (caseOperation == null) {
                // no need to process the stored index values, so we can use a default Lucene query
                if (this == EQ) {
                    return new TermQuery(new Term(fieldName, constraintValue));
                }
                return TermRangeQuery.newStringRange(fieldName,
                                                     test(-1) ? null : constraintValue,
                                                     test(1) ? null : constraintValue,
                                                     test(0),
                                                     test(0));
            }
            final BiPredicate<String, String> evaluator;
            if (this == EQ) {
                evaluator = Objects::equals;
            } else {
                evaluator = ( s1, s2 ) -> test(STRING_COMPARATOR.compare(s1, s2));
            }
            return new CompareStringQuery(fieldName, constraintValue, evaluator, caseOperation);
        }
    }

    private static final String LUCENE_SPECIAL_CHARACTERS = "+-&|!(){}[]^\"~?*:\\";

    /**
     * Construct a {@link Query} implementation that scores nodes according to the supplied comparator.
     *
     * @param fieldName the name of the document field containing the value; may not be null
     * @param constraintValue the constraint value; may not be null
     * @param evaluator the {@link BiPredicate} implementation that returns whether the node path satisfies the
     * @param caseOperation a {@link Function} which can be applied to perform the comparison in a different case; may be null
     */
    protected CompareStringQuery(String fieldName,
                                 String constraintValue,
                                 BiPredicate<String, String> evaluator,
                                 Function<String, String> caseOperation) {
        super(fieldName, constraintValue, evaluator, caseOperation);
    }
    
    @Override
    protected String convertValue(String casedValue) {
        // return the value as-is (no conversion needed)
        return casedValue;
    }
    
    @Override
    public Query clone() {
        return new CompareStringQuery(field(), constraintValue, evaluator, caseOperation);
    }
    
    /**
     * Construct a {@link Query} implementation that scores documents with a string field value that is equal to the supplied
     * constraint value.
     *
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     *        evaluated; may be null which indicates that no case conversion should be done
     * @return the query; never null
     */
    public static Query createQueryForNodesWithFieldEqualTo(String constraintValue,
                                                            String fieldName,
                                                            Function<String, String> caseOperation) {
        return FieldComparison.EQ.createQueryForNodesWithField(constraintValue, fieldName, caseOperation);
    }
    
    /**
     * Construct a {@link Query} implementation that scores documents with a string field value that is greater than the supplied
     * constraint value.
     *
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     *        evaluated; may be null which indicates that no case conversion should be done
     * @return the query; never null
     */
    public static Query createQueryForNodesWithFieldGreaterThan(String constraintValue,
                                                                String fieldName,
                                                                Function<String, String> caseOperation) {
        return FieldComparison.GT.createQueryForNodesWithField(constraintValue, fieldName, caseOperation);
    }

    /**
     * Construct a {@link Query} implementation that scores documents with a string field value that is greater than or equal to
     * the supplied constraint value.
     *
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     *        evaluated; may be null which indicates that no case conversion should be done
     * @return the query; never null
     */
    public static Query createQueryForNodesWithFieldGreaterThanOrEqualTo(String constraintValue,
                                                                         String fieldName,
                                                                         Function<String, String> caseOperation) {
        return FieldComparison.GE.createQueryForNodesWithField(constraintValue, fieldName, caseOperation);
    }
    
    /**
     * Construct a {@link Query} implementation that scores documents with a string field value that is less than the supplied
     * constraint value.
     *
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     *        evaluated; may be null which indicates that no case conversion should be done
     * @return the query; never null
     */
    public static Query createQueryForNodesWithFieldLessThan(String constraintValue,
                                                             String fieldName,
                                                             Function<String, String> caseOperation) {
        return FieldComparison.LT.createQueryForNodesWithField(constraintValue, fieldName, caseOperation);
    }
    
    /**
     * Construct a {@link Query} implementation that scores documents with a string field value that is less than or equal to the
     * supplied constraint value.
     *
     * @param constraintValue the constraint value; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     *        evaluated; may be null which indicates that no case conversion should be done
     * @return the query; never null
     */
    public static Query createQueryForNodesWithFieldLessThanOrEqualTo(String constraintValue,
                                                                      String fieldName,
                                                                      Function<String, String> caseOperation) {
        return FieldComparison.LE.createQueryForNodesWithField(constraintValue, fieldName, caseOperation);
    }
    
    /**
     * Construct a {@link Query} implementation that scores documents with a string field value that is LIKE the supplied
     * constraint value, where the LIKE expression contains the SQL wildcard characters '%' and '_' or the regular expression
     * wildcard characters '*' and '?'.
     *
     * @param likeExpression the LIKE expression; may not be null
     * @param fieldName the name of the document field containing the value; may not be null
     * @param caseOperation the operation that should be performed on the indexed values before the constraint value is being
     *        evaluated; may be null which indicates that no case conversion should be done
     * @return the query; never null
     */
    protected static Query createQueryForNodesWithFieldLike(String likeExpression,
                                                            String fieldName,
                                                            Function<String, String> caseOperation) {
        assert likeExpression != null;
        assert likeExpression.length() > 0;
        
        if (!QueryUtil.hasWildcardCharacters(likeExpression)) {
            // This is not a like expression, so just do an equals ...
            return createQueryForNodesWithFieldEqualTo(likeExpression, fieldName, caseOperation);
        }
        if (caseOperation == null) {
            // We can just do a normal Wildcard query ...
            
            // '%' matches 0 or more characters
            // '_' matches any single character
            // '\x' matches 'x'
            // all other characters match themselves
            
            // Wildcard queries are a better match, but they can be slow and should not be used
            // if the first character of the expression is a '%' or '_' or '*' or '?' ...
            char firstChar = likeExpression.charAt(0);
            if (firstChar != '%' && firstChar != '_' && firstChar != '*' && firstChar != '?') {
                // Create a wildcard query ...
                return new WildcardQuery(new Term(fieldName, toWildcardExpression(likeExpression)));
            }
        }
        // Create a regex query...
        String regex = QueryUtil.toRegularExpression(likeExpression);
        Pattern pattern = Pattern.compile(regex, Pattern.UNICODE_CASE);
        return new RegexQuery(fieldName, pattern, caseOperation);
    }
    
    /**
     * Convert the JCR like expression to a Lucene wildcard expression. The JCR like expression uses '%' to match 0 or more
     * characters, '_' to match any single character, '\x' to match the 'x' character, and all other characters to match
     * themselves. Since ModeShape v5.5, this method additionally escapes Lucene special characters, with the exception,
     * for backwards compatibility, of the '*' and '?' wildcard characters themselves, which are supported alternatives
     * despite not being officially part of the JCR specification.
     *
     * @param likeExpression the like expression; may not be null
     * @return the expression that can be used with a WildcardQuery; never null
     */
    protected static String toWildcardExpression( String likeExpression ) {
        if (likeExpression.isEmpty()) {
            return likeExpression;
        }
        final int sz = likeExpression.length();
        final StringBuilder buf = new StringBuilder(sz);
        int pos = -1;
        while (++pos < sz) {
            final char c = likeExpression.charAt(pos);
            char out;
            switch (c) {
                case '%':
                case '*':
                    buf.append('*');
                    continue;
                case '_':
                case '?':
                    buf.append('?');
                    continue;
                case '\\':
                    if (++pos < sz) {
                        out = likeExpression.charAt(pos);
                        break;
                    }
                    // weird case with a trailing backslash, treat as "escaped nothing" i.e. skip it
                    continue;
                default:
                    out = c;
            }
            if (LUCENE_SPECIAL_CHARACTERS.indexOf(out) >= 0) {
                buf.append('\\');
            }
            buf.append(c);
        }
        return buf.toString();
    }
}
