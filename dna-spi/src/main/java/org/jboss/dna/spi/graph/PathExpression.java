/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008, Red Hat Middleware LLC, and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors. 
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.dna.spi.graph;

import java.io.Serializable;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.spi.SpiI18n;

/**
 * An expression that defines an acceptable path using a regular-expression-like language. Path expressions can be used to
 * represent node paths or properties.
 * <p>
 * Here are some simple examples:
 * <ul>
 * <li><code>/a/b/c</code> - selects the node "c" that is a child of node "b" that is a child of node "a".</li>
 * <li><code>//a</code> - selects any node named "a" that is at any location (with any ancestors).</li>
 * <li><code></code> -</li>
 * <li><code></code> -</li>
 * </ul>
 * </p>
 * 
 * @author Randall Hauch
 */
@Immutable
public class PathExpression implements Serializable {

    /**
     * Initial version
     */
    private static final long serialVersionUID = 1L;

    /**
     * Compile the supplied expression and return the resulting path expression instance.
     * 
     * @param expression the expression
     * @return the path expression; never null
     * @throws IllegalArgumentException if the expression is null
     * @throws InvalidPathExpressionException if the expression is blank or is not a valid expression
     */
    public static final PathExpression compile( String expression ) throws InvalidPathExpressionException {
        return new PathExpression(expression);
    }

    private static final String SEQUENCE_PATTERN_STRING = "\\[(\\d+(?:,\\d+)*)\\]"; // \[(\d+(,\d+)*)\]
    private static final Pattern SEQUENCE_PATTERN = Pattern.compile(SEQUENCE_PATTERN_STRING);

    /**
     * Regular expression used to find unusable XPath predicates within an expression. This pattern results in unusable predicates
     * in group 1. Note that some predicates may be valid at the end but not valid elsewhere.
     * <p>
     * Currently, only index-like predicates (including sequences) are allowed everywhere. Predicates with paths and properties
     * are allowed only as the last predicate. Predicates with any operators are unused.
     * </p>
     * <p>
     * Nested predicates are not currently allowed.
     * </p>
     */
    // \[(?:(?:\d+(?:,\d+)*)|\*)\]|(?:\[[^\]\+\-\*=\!><'"\s]+\])$|(\[[^\]]+\])
    private static final String UNUSABLE_PREDICATE_PATTERN_STRING = "\\[(?:(?:\\d+(?:,\\d+)*)|\\*)\\]|(?:\\[[^\\]\\+\\-\\*=\\!><'\"\\s]+\\])$|(\\[[^\\]]+\\])";
    private static final Pattern UNUSABLE_PREDICATE_PATTERN = Pattern.compile(UNUSABLE_PREDICATE_PATTERN_STRING);

    /**
     * Regular expression used to find all XPath predicates except index and sequence patterns. This pattern results in the
     * predicates to be removed in group 1.
     */
    // \[(?:(?:\d+(?:,\d+)*)|\*)\]|(\[[^\]]+\])
    private static final String NON_INDEX_PREDICATE_PATTERN_STRING = "\\[(?:(?:\\d+(?:,\\d+)*)|\\*)\\]|(\\[[^\\]]+\\])";
    private static final Pattern NON_INDEX_PREDICATE_PATTERN = Pattern.compile(NON_INDEX_PREDICATE_PATTERN_STRING);

    private final String expression;
    private final Pattern matchPattern;
    private final Pattern selectPattern;

    /**
     * Create the supplied expression.
     * 
     * @param expression the expression
     * @throws IllegalArgumentException if the expression is null
     * @throws InvalidPathExpressionException if the expression is blank or is not a valid expression
     */
    public PathExpression( String expression ) throws InvalidPathExpressionException {
        ArgCheck.isNotNull(expression, "path expression");
        this.expression = expression.trim();
        if (this.expression.length() == 0) {
            throw new InvalidPathExpressionException(SpiI18n.pathExpressionMayNotBeBlank.text());
        }
        // Build the match pattern, which determines whether a path matches the condition ...
        String matchString = this.expression;
        try {
            matchString = removeUnusedPredicates(matchString);
            matchString = replaceXPathPatterns(matchString);
            this.matchPattern = Pattern.compile(matchString, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            String msg = SpiI18n.pathExpressionHasInvalidMatch.text(matchString, this.expression);
            throw new InvalidPathExpressionException(msg, e);
        }
        // Build the select pattern, which determines the path that will be selected ...
        String selectString = this.expression;
        try {
            selectString = removeAllPredicatesExceptIndexes(selectString);
            selectString = replaceXPathPatterns(selectString);
            selectString = "(" + selectString + ").*"; // group 1 will have selected path ...
            this.selectPattern = Pattern.compile(selectString, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            String msg = SpiI18n.pathExpressionHasInvalidSelect.text(selectString, this.expression);
            throw new InvalidPathExpressionException(msg, e);
        }
    }

    /**
     * @return expression
     */
    public String getExpression() {
        return expression;
    }

    /**
     * Replace certain XPath patterns that are not used or understood.
     * 
     * @param expression the input regular expressions string; may not be null
     * @return the regular expression with all unused XPath patterns removed; never null
     */
    protected String removeUnusedPredicates( String expression ) {
        assert expression != null;
        java.util.regex.Matcher matcher = UNUSABLE_PREDICATE_PATTERN.matcher(expression);
        StringBuffer sb = new StringBuffer();
        if (matcher.find()) {
            do {
                // Remove those predicates that show up in group 1 ...
                String predicateStr = matcher.group(0);
                String unusablePredicateStr = matcher.group(1);
                if (unusablePredicateStr != null) {
                    predicateStr = "";
                }
                matcher.appendReplacement(sb, predicateStr);
            } while (matcher.find());
            matcher.appendTail(sb);
            expression = sb.toString();
        }
        return expression;
    }

    /**
     * Remove all XPath predicates from the supplied regular expression string.
     * 
     * @param expression the input regular expressions string; may not be null
     * @return the regular expression with all XPath predicates removed; never null
     */
    protected String removeAllPredicatesExceptIndexes( String expression ) {
        assert expression != null;
        java.util.regex.Matcher matcher = NON_INDEX_PREDICATE_PATTERN.matcher(expression);
        StringBuffer sb = new StringBuffer();
        if (matcher.find()) {
            do {
                // Remove those predicates that show up in group 1 ...
                String predicateStr = matcher.group(0);
                String unusablePredicateStr = matcher.group(1);
                if (unusablePredicateStr != null) {
                    predicateStr = "";
                }
                matcher.appendReplacement(sb, predicateStr);
            } while (matcher.find());
            matcher.appendTail(sb);
            expression = sb.toString();
        }
        return expression;
    }

    /**
     * Replace certain XPath patterns, including some predicates, with substrings that are compatible with regular expressions.
     * 
     * @param expression the input regular expressions string; may not be null
     * @return the regular expression with XPath patterns replaced with regular expression fragments; never null
     */
    protected String replaceXPathPatterns( String expression ) {
        assert expression != null;
        // replace 2 or more sequential '|' characters in an OR expression
        expression = expression.replaceAll("[\\|]{2,}", "|");
        // if there is an empty expression in an OR expression, make the whole segment optional ...
        // (e.g., "/a/b/(c|)/d" => "a/b(/(c))?/d"
        expression = expression.replaceAll("/(\\([^|]+)(\\|){2,}([^)]+\\))", "(/$1$2$3)?");
        expression = expression.replaceAll("/\\(\\|+([^)]+)\\)", "(?:/($1))?");
        expression = expression.replaceAll("/\\((([^|]+)(\\|[^|]+)*)\\|+\\)", "(?:/($1))?");

        // // Allow any path (that doesn't contain an explicit counter) to contain a counter,
        // // done by replacing any '/' or '|' that isn't preceded by ']' or '*' or '/' or '(' with '(\[\d+\])?/'...
        // input = input.replaceAll("(?<=[^\\]\\*/(])([/|])", "(?:\\\\[\\\\d+\\\\])?$1");

        // Does the path contain any '[]' or '[*]' or '[0]' or '[n]' (where n is any positive integers)...
        // '[*]/' => '(\[\d+\])?/'
        expression = expression.replaceAll("\\[\\]", "(?:\\\\[\\\\d+\\\\])?"); // index is optional
        // '[]/' => '(\[\d+\])?/'
        expression = expression.replaceAll("\\[[*]\\]", "(?:\\\\[\\\\d+\\\\])?"); // index is optional
        // '[0]/' => '(\[0\])?/'
        expression = expression.replaceAll("\\[0\\]", "(?:\\\\[0\\\\])?"); // index is optional
        // '[n]/' => '\[n\]/'
        expression = expression.replaceAll("\\[([1-9]\\d*)\\]", "\\\\[$1\\\\]"); // index is required

        // Change any other end predicates to not be wrapped by braces but to begin with a slash ...
        // ...'[x]' => ...'/x'
        expression = expression.replaceAll("(?<!\\\\)\\[([^\\]]*)\\]$", "/$1");

        // Replace all '[n,m,o,p]' type sequences with '[(n|m|o|p)]'
        java.util.regex.Matcher matcher = SEQUENCE_PATTERN.matcher(expression);
        StringBuffer sb = new StringBuffer();
        boolean result = matcher.find();
        if (result) {
            do {
                String sequenceStr = matcher.group(1);
                boolean optional = false;
                if (sequenceStr.startsWith("0,")) {
                    sequenceStr = sequenceStr.replaceFirst("^0,", "");
                    optional = true;
                }
                if (sequenceStr.endsWith(",0")) {
                    sequenceStr = sequenceStr.replaceFirst(",0$", "");
                    optional = true;
                }
                if (sequenceStr.contains(",0,")) {
                    sequenceStr = sequenceStr.replaceAll(",0,", ",");
                    optional = true;
                }
                sequenceStr = sequenceStr.replaceAll(",", "|");
                String replacement = "\\\\[(?:" + sequenceStr + ")\\\\]";
                if (optional) {
                    replacement = "(?:" + replacement + ")?";
                }
                matcher.appendReplacement(sb, replacement);
                result = matcher.find();
            } while (result);
            matcher.appendTail(sb);
            expression = sb.toString();
        }

        // Order is important here
        expression = expression.replaceAll("[*]([^/(\\\\])", "[^/$1]*$1"); // '*' not followed by '/', '\\', or '('
        expression = expression.replaceAll("(?<!\\[\\^/\\])[*]", "[^/]*");
        expression = expression.replaceAll("[/]{2,}$", "(?:/[^/]*)*"); // ending '//'
        expression = expression.replaceAll("[/]{2,}", "(?:/[^/]*)*/"); // other '//'
        return expression;
    }

    /**
     * @return the expression
     */
    public String getSelectExpression() {
        return this.expression;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int hashCode() {
        return this.expression.hashCode();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean equals( Object obj ) {
        if (obj == this) return true;
        if (obj instanceof PathExpression) {
            PathExpression that = (PathExpression)obj;
            if (!this.expression.equalsIgnoreCase(that.expression)) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.expression;
    }

    /**
     * @param absolutePath
     * @return the matcher
     */
    public Matcher matcher( String absolutePath ) {
        // Determine if the input path match the select expression ...
        String originalAbsolutePath = absolutePath;
        // if (!absolutePath.endsWith("/")) absolutePath = absolutePath + "/";
        // Remove all trailing '/' ...
        absolutePath = absolutePath.replaceAll("/+$", "");

        // See if the supplied absolute path matches the pattern ...
        final java.util.regex.Matcher matcher = this.matchPattern.matcher(absolutePath);
        if (!matcher.matches()) {
            // No match, so return immediately ...
            return new Matcher(matcher, originalAbsolutePath, null);
        }

        // The absolute path does match the pattern, so use the select pattern and try to grab the selected path ...
        final java.util.regex.Matcher selectMatcher = this.selectPattern.matcher(absolutePath);
        if (!selectMatcher.matches()) {
            // Nothing can be selected, so return immediately ...
            return new Matcher(matcher, null, null);
        }
        // Grab the selected path ...
        String selectedPath = selectMatcher.group(1);

        // Remove the trailing '/@property' ...
        selectedPath = selectedPath.replaceAll("/@[^/\\[\\]]+$", "");

        return new Matcher(matcher, originalAbsolutePath, selectedPath);
    }

    @Immutable
    public static class Matcher {

        private final String inputPath;
        private final String selectedPath;
        private final java.util.regex.Matcher inputMatcher;
        private final int hc;

        protected Matcher( java.util.regex.Matcher inputMatcher,
                           String inputPath,
                           String selectedPath ) {
            this.inputMatcher = inputMatcher;
            this.inputPath = inputPath;
            this.selectedPath = selectedPath;
            this.hc = HashCode.compute(this.inputPath, this.selectedPath);
        }

        public boolean matches() {
            return this.selectedPath != null;
        }

        /**
         * @return inputPath
         */
        public String getInputPath() {
            return this.inputPath;
        }

        /**
         * @return selectPattern
         */
        public String getSelectedNodePath() {
            return this.selectedPath;
        }

        public int groupCount() {
            return this.inputMatcher.groupCount();
        }

        public String group( int groupNumber ) {
            return this.inputMatcher.group(groupNumber);
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public int hashCode() {
            return this.hc;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public boolean equals( Object obj ) {
            if (obj == this) return true;
            if (obj instanceof PathExpression.Matcher) {
                PathExpression.Matcher that = (PathExpression.Matcher)obj;
                if (!this.inputPath.equalsIgnoreCase(that.inputPath)) return false;
                if (!this.selectedPath.equalsIgnoreCase(that.selectedPath)) return false;
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return this.selectedPath;
        }
    }

    /**
     * Regular expression used to determine if the expression matches any single-level wildcard.
     */
    // /*(?:[*.](?:\[\*?\])?/*)*
    private static final String ANYTHING_PATTERN_STRING = "/*(?:[*.](?:\\[\\*?\\])?/*)*";
    private static final Pattern ANYTHING_PATTERN = Pattern.compile(ANYTHING_PATTERN_STRING);

    /**
     * Return whether this expression matches anything and therefore is not restrictive. These include expressions of any nodes ("
     * <code>/</code>"), any sequence of nodes ("<code>//</code>"), the self reference ("<code>.</code>"), or wildcard ("
     * <code>*</code>", "<code>*[]</code>" or "<code>*[*]</code>"). Combinations of these individual expressions are also
     * considered to match anything.
     * 
     * @return true if the expression matches anything, or false otherwise
     */
    public boolean matchesAnything() {
        return ANYTHING_PATTERN.matcher(expression).matches();
    }

    public static PathExpression all() {
        return ALL_PATHS_EXPRESSION;
    }

    private static final PathExpression ALL_PATHS_EXPRESSION = PathExpression.compile("//");

}
