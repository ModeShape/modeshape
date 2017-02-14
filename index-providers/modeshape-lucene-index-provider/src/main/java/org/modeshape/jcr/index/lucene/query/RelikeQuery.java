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
import org.apache.lucene.search.Query;
import org.modeshape.common.annotation.Immutable;

/**
 * A Lucene {@link Query} implementation that matches some value by document property as like pattern.
 * 
 * @since 4.5
 */
@Immutable
public class RelikeQuery extends ConstantScoreWeightQuery {

    private final String relikeValue;
    
    protected RelikeQuery( String field, String relikeValue ) {
        super(field);
        this.relikeValue = relikeValue;
    }

    @Override
    protected boolean accepts(String value) {
        return value != null && like(relikeValue, value);
    }
    
    public String toString( String field ) {
        final StringBuilder sb = new StringBuilder();
        return sb.append(field).append(" RELIKE ").append(relikeValue).toString();
    }

    @Override
    public Query clone() {
        return new RelikeQuery(field(), relikeValue);
    }
    
    private static boolean like( String value, String pattern ) {
        CompareType cmpType = getCompareType(pattern);
        switch (cmpType) {
            case EQ: {
                return value.equals(pattern);
            }
            case ENDS_WITH: {
                return value.endsWith(pattern.substring(1));
            }
            case STARTS_WITH: {
                return value.startsWith(pattern.substring(0, pattern.length() - 1));
            }
            case REGEXP:
            default: {
                Pattern p = Pattern.compile(toRegularExpression(pattern));
                return p.matcher(value).matches();
            }
        }
    }

    private enum CompareType {
        EQ,
        STARTS_WITH,
        ENDS_WITH,
        REGEXP;
    }
    
    /**
     * Determine the compare type given the expression. This method returns:
     * <ul>
     * <li>CompareType.EQ if expression does not contains '_' or '%';</li>
     * <li>CompareType.ENDS_WITH if expression has only one '%' at start and does not contains '_';</li>
     * <li>CompareType.STARTS_WITH if expression has only one '%' at end and does not contains '_';</li>
     * <li>CompareType.REGEXP otherwise</li>
     * </ul>
     * 
     * @param expression the expression for which the {@link CompareType} is to be found
     * @return the compare type; never null;
     */
    private static CompareType getCompareType( String expression ) {
        CompareType result = CompareType.EQ;

        CharacterIterator iter = new StringCharacterIterator(expression);
        final int fistIndex = 0;
        final int lastIndex = expression.length() - 1;
        boolean skipNext = false;
        for (char c = iter.first(); c != CharacterIterator.DONE; c = iter.next()) {
            if (skipNext) {
                skipNext = false;
                continue;
            }
            if (c == '_') {
                return CompareType.REGEXP;
            }
            if (c == '%') {
                if (result != CompareType.EQ) {
                    // pattern like '%abcdfe%' -> only regexp can handle this;
                    return CompareType.REGEXP;
                }

                int index = iter.getIndex();
                if (index == fistIndex) {
                    result = CompareType.ENDS_WITH;
                } else if (index == lastIndex) {
                    result = CompareType.STARTS_WITH;
                } else {
                    return CompareType.REGEXP;
                }
            }

            if (c == '\\') skipNext = true;
        }

        return result;
    }

    private static String toRegularExpression( String likeExpression ) {
        // Replace all '\x' with 'x' ...
        String result = likeExpression.replaceAll("\\\\(.)", "$1");
        // Escape characters used as metacharacters in regular expressions, including
        // '[', '^', '\', '$', '.', '|', '+', '*', '?', '(', and ')'
        result = result.replaceAll("([$.|+()\\*\\?\\[\\\\^\\\\\\\\])", "\\\\$1");
        // Replace '%'->'[.]*' and '_'->'[.]
        result = result.replace("%", ".*").replace("_", ".");
        return result;
    }
}
