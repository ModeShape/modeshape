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
package org.jboss.dna.services.sequencers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.ArgCheck;
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.services.ServicesI18n;

/**
 * An expression that defines a selection of some change in the repository that signals a sequencing operation should be run, and
 * the location where the sequencing output should be placed. Sequencer path expressions are used within the
 * {@link SequencerConfig sequencer configurations} and used to determine whether information in the repository needs to be
 * sequenced.
 * <p>
 * A simple example is the following:
 * 
 * <pre>
 *     /a/b/c@title =&gt; /d/e/f
 * </pre>
 * 
 * which means that a sequencer (that uses this expression in its configuration) should be run any time there is a new or modified
 * <code>title</code> property on the <code>/a/b/c</code> node, and that the output of the sequencing should be placed at
 * <code>/d/e/f</code>.
 * </p>
 * @author Randall Hauch
 */
@Immutable
public class SequencerPathExpression implements Serializable {

    /**
     * The pattern used to break the initial input string into the two major parts, the selection and output expressions. Group 1
     * contains the selection expression, and group 2 contains the output expression.
     */
    private static final Pattern TWO_PART_PATTERN = Pattern.compile("((?:[^=]|=(?!>))+)(?:=>(.+))?");

    protected static final String DEFAULT_OUTPUT_EXPRESSION = ".";

    private static final String REPLACEMENT_VARIABLE_PATTERN_STRING = "(?<!\\\\)\\$(\\d+)"; // (?<!\\)\$(\d+)
    private static final Pattern REPLACEMENT_VARIABLE_PATTERN = Pattern.compile(REPLACEMENT_VARIABLE_PATTERN_STRING);

    private static final String PARENT_PATTERN_STRING = "[^/]+/\\.\\./"; // [^/]+/\.\./
    private static final Pattern PARENT_PATTERN = Pattern.compile(PARENT_PATTERN_STRING);

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

    /**
     * Compile the supplied expression and return the resulting SequencerPathExpression2 instance.
     * @param expression the expression
     * @return the path expression; never null
     * @throws IllegalArgumentException if the expression is null
     * @throws InvalidSequencerPathExpression if the expression is blank or is not a valid expression
     */
    public static final SequencerPathExpression compile( String expression ) throws InvalidSequencerPathExpression {
        ArgCheck.isNotNull(expression, "sequencer path expression");
        expression = expression.trim();
        if (expression.length() == 0) {
            throw new InvalidSequencerPathExpression(ServicesI18n.pathExpressionMayNotBeBlank.text());
        }
        java.util.regex.Matcher matcher = TWO_PART_PATTERN.matcher(expression);
        if (!matcher.matches()) {
            throw new InvalidSequencerPathExpression(ServicesI18n.pathExpressionIsInvalid.text(expression));
        }
        String selectExpression = matcher.group(1);
        String outputExpression = matcher.group(2);
        return new SequencerPathExpression(selectExpression, outputExpression);
    }

    private final String selectExpression;
    private final String outputExpression;
    private final Pattern matchPattern;
    private final Pattern selectPattern;
    private final int hc;

    protected SequencerPathExpression( String selectExpression, String outputExpression ) throws InvalidSequencerPathExpression {
        ArgCheck.isNotNull(selectExpression, "select expression");
        this.selectExpression = selectExpression.trim();
        this.outputExpression = outputExpression != null ? outputExpression.trim() : DEFAULT_OUTPUT_EXPRESSION;
        this.hc = HashCode.compute(this.selectExpression, this.outputExpression);

        // Build the match pattern, which determines whether a path matches the condition ...
        String matchString = this.selectExpression;
        try {
            matchString = removeUnusedPredicates(matchString);
            matchString = replaceXPathPatterns(matchString);
            this.matchPattern = Pattern.compile(matchString, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            String msg = ServicesI18n.pathExpressionHasInvalidMatch.text(matchString, this.selectExpression, this.outputExpression);
            throw new InvalidSequencerPathExpression(msg, e);
        }
        // Build the select pattern, which determines the path that will be selected ...
        String selectString = this.selectExpression.trim();
        try {
            selectString = removeAllPredicatesExceptIndexes(selectString);
            selectString = replaceXPathPatterns(selectString);
            selectString = "(" + selectString + ").*"; // group 1 will have selected path ...
            this.selectPattern = Pattern.compile(selectString, Pattern.CASE_INSENSITIVE);
        } catch (PatternSyntaxException e) {
            String msg = ServicesI18n.pathExpressionHasInvalidSelect.text(selectString, this.selectExpression, this.outputExpression);
            throw new InvalidSequencerPathExpression(msg, e);
        }
    }

    /**
     * Replace certain XPath patterns that are not used or understood.
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
        expression = expression.replaceAll("/\\(\\|+([^)]+)\\)", "(/($1))?");
        expression = expression.replaceAll("/\\((([^|]+)(\\|[^|]+)*)\\|+\\)", "(/($1))?");

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
        expression = expression.replaceAll("[*]", "[^/]*");
        expression = expression.replaceAll("[/]{2,}", "(/[^/]*)*/");
        return expression;
    }

    /**
     * @return selectExpression
     */
    public String getSelectExpression() {
        return this.selectExpression;
    }

    /**
     * @return outputExpression
     */
    public String getOutputExpression() {
        return this.outputExpression;
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
        if (obj instanceof SequencerPathExpression) {
            SequencerPathExpression that = (SequencerPathExpression)obj;
            if (!this.selectExpression.equalsIgnoreCase(that.selectExpression)) return false;
            if (!this.outputExpression.equalsIgnoreCase(that.outputExpression)) return false;
            return true;
        }
        return false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return this.selectExpression + "=>" + this.outputExpression;
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
            return new Matcher(originalAbsolutePath, null, null);
        }
        Map<Integer, String> replacements = new HashMap<Integer, String>();
        for (int i = 0, count = matcher.groupCount(); i <= count; ++i) {
            replacements.put(i, matcher.group(i));
        }

        // The absolute path does match the pattern, so use the select pattern and try to grab the selected path ...
        final java.util.regex.Matcher selectMatcher = this.selectPattern.matcher(absolutePath);
        if (!selectMatcher.matches()) {
            // Nothing can be selected, so return immediately ...
            return new Matcher(originalAbsolutePath, null, null);
        }
        // Grab the selected path ...
        String selectedPath = selectMatcher.group(1);

        // Remove the trailing '/@property' ...
        selectedPath = selectedPath.replaceAll("/@[^/\\[\\]]+$", "");

        // Find the output path using the groups from the match pattern ...
        String outputPath = this.outputExpression;
        if (!DEFAULT_OUTPUT_EXPRESSION.equals(outputPath)) {
            java.util.regex.Matcher replacementMatcher = REPLACEMENT_VARIABLE_PATTERN.matcher(outputPath);
            StringBuffer sb = new StringBuffer();
            if (replacementMatcher.find()) {
                do {
                    String variable = replacementMatcher.group(1);
                    String replacement = replacements.get(Integer.valueOf(variable));
                    if (replacement == null) replacement = replacementMatcher.group(0);
                    replacementMatcher.appendReplacement(sb, replacement);
                } while (replacementMatcher.find());
                replacementMatcher.appendTail(sb);
                outputPath = sb.toString();
            }
            // Make sure there is a trailing '/' ...
            if (!outputPath.endsWith("/")) outputPath = outputPath + "/";

            // Replace all references to "/./" with "/" ...
            outputPath = outputPath.replaceAll("/\\./", "/");

            // Remove any path segment followed by a parent reference ...
            java.util.regex.Matcher parentMatcher = PARENT_PATTERN.matcher(outputPath);
            while (parentMatcher.find()) {
                outputPath = parentMatcher.replaceAll("");
                // Make sure there is a trailing '/' ...
                if (!outputPath.endsWith("/")) outputPath = outputPath + "/";
                parentMatcher = PARENT_PATTERN.matcher(outputPath);
            }

            // Remove all multiple occurrences of '/' ...
            outputPath = outputPath.replaceAll("/{2,}", "/");

            // Remove the trailing '/@property' ...
            outputPath = outputPath.replaceAll("/@[^/\\[\\]]+$", "");

            // Remove a trailing '/' ...
            outputPath = outputPath.replaceAll("/$", "");

            // If the output path is blank, then use the default output expression ...
            if (outputPath.length() == 0) outputPath = DEFAULT_OUTPUT_EXPRESSION;

        }
        if (DEFAULT_OUTPUT_EXPRESSION.equals(outputPath)) {
            // The output path is the default expression, so use the selected path ...
            outputPath = selectedPath;
        }

        return new Matcher(originalAbsolutePath, selectedPath, outputPath);
    }

    @Immutable
    public static class Matcher {

        private final String inputPath;
        private final String selectedPath;
        private final String outputPath;
        private final int hc;

        protected Matcher( String inputPath, String selectedPath, String outputPath ) {
            this.inputPath = inputPath;
            this.selectedPath = selectedPath;
            this.outputPath = outputPath;
            this.hc = HashCode.compute(this.inputPath, this.selectedPath, this.outputPath);
        }

        public boolean matches() {
            return this.selectedPath != null && this.outputPath != null;
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
        public String getSelectedPath() {
            return this.selectedPath;
        }

        /**
         * @return outputPath
         */
        public String getOutputPath() {
            return this.outputPath;
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
            if (obj instanceof SequencerPathExpression.Matcher) {
                SequencerPathExpression.Matcher that = (SequencerPathExpression.Matcher)obj;
                if (!this.inputPath.equalsIgnoreCase(that.inputPath)) return false;
                if (!this.selectedPath.equalsIgnoreCase(that.selectedPath)) return false;
                if (!this.outputPath.equalsIgnoreCase(that.outputPath)) return false;
                return true;
            }
            return false;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return this.selectedPath + " => " + this.outputPath;
        }
    }

}
