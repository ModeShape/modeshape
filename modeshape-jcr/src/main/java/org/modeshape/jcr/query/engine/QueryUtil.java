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

    public static boolean includeFullTextScores( Iterable<Constraint> constraints ) {
        for (Constraint constraint : constraints) {
            if (includeFullTextScores(constraint)) return true;
        }
        return false;
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

}
