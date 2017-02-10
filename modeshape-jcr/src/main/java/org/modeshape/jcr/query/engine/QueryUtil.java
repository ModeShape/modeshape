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
package org.modeshape.jcr.query.engine;

import java.text.CharacterIterator;
import java.text.StringCharacterIterator;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import org.modeshape.jcr.query.model.Constraint;
import org.modeshape.jcr.query.model.FullTextSearch;
import org.modeshape.jcr.query.model.Visitors;

/**
 * Utility methods for query processing.
 * 
 * @author Randall Hauch (rhauch@redhat.com)
 */
public class QueryUtil {
    
    /**
     * Checks if the given expression has any wildcard characters
     * @param expression a {@code String} value, never {@code null}
     * @return true if the expression has wildcard characters, false otherwise
     */
    public static boolean hasWildcardCharacters( String expression ) {
        Objects.requireNonNull(expression);
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
        // '[', '^', '\', '$', '.', '|', '+', '(', and ')'
        // But leave '?' and '*'
        result = result.replaceAll("([$.|+()\\[\\\\^\\\\\\\\])", "\\\\$1");
        // Replace '%'->'[.]*' and '_'->'[.]
        // (order of these calls is important!)
        result = result.replace("*", ".*").replace("?", ".");
        result = result.replace("%", ".*").replace("_", ".");
        // Replace all wildcards between square bracket literals with digit wildcards ...
        result = result.replace("\\[.*]", "\\[\\d+]");
        return result;
    }

    public static boolean includeFullTextScores( Constraint constraint ) {
        final AtomicBoolean includeFullTextScores = new AtomicBoolean(false);
        if (constraint != null) {
            Visitors.visitAll(constraint, new Visitors.AbstractVisitor() {
                @Override
                public void visit( FullTextSearch obj ) {
                    includeFullTextScores.set(true);
                }
            });
        }
        return includeFullTextScores.get();
    }

    private QueryUtil() {
    }

    /**
     * Process the supplied LIKE expression for an absolute path and return a copy that has a SNS index (wildcard or literal) for
     * all literal segments in the expression. For example, this method will convert:
     *
     * <pre>
     *  /alpha/beta[%]
     * </pre>
     *
     * into
     *
     * <pre>
     *  /alpha[1]/beta[%]
     * </pre>
     *
     * and
     *
     * <pre>
     *  /alpha/%/beta[%]
     * </pre>
     *
     * into
     *
     * <pre>
     *  /alpha[1]/%/beta[%]
     * </pre>
     *
     * @param pathLikeExpression the LIKE expression for a path; may not be null
     * @return the updated like expression with SNS indexes in all literal segments
     */
    public static String addSnsIndexesToLikeExpression( String pathLikeExpression ) {
        if ("%".equals(pathLikeExpression)) return pathLikeExpression;

        boolean altered = false;
        StringBuilder sb = new StringBuilder();
        for (String segment : pathLikeExpression.split("/")) {
            if (segment.length() == 0) {
                // This segment is empty ...
                continue;
            }
            sb.append('/').append(segment);
            if (segment.endsWith("%") || segment.endsWith("]")) {
                // This segment already ends with a wildcard or a SNS index, so we're done ...
                continue;
            }
            // Otherwise, we have to add the SNS index ...
            sb.append("[1]");
            altered = true;
        }
        return altered ? sb.toString() : pathLikeExpression;
    }

}
