/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2008-2009, Red Hat Middleware LLC, and individual contributors
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
package org.jboss.dna.repository.sequencers;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;
import net.jcip.annotations.Immutable;
import org.jboss.dna.common.util.CheckArg;
import org.jboss.dna.common.util.HashCode;
import org.jboss.dna.graph.properties.PathExpression;
import org.jboss.dna.repository.RepositoryI18n;

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
 * 
 * @author Randall Hauch
 */
@Immutable
public class SequencerPathExpression implements Serializable {

    /**
     */
    private static final long serialVersionUID = 229464314137494765L;

    /**
     * The pattern used to break the initial input string into the two major parts, the selection and output expressions. Group 1
     * contains the selection expression, and group 2 contains the output expression.
     */
    private static final Pattern TWO_PART_PATTERN = Pattern.compile("((?:[^=]|=(?!>))+)(?:=>(.+))?");

    protected static final String DEFAULT_OUTPUT_EXPRESSION = ".";

    private static final String PARENT_PATTERN_STRING = "[^/]+/\\.\\./"; // [^/]+/\.\./
    private static final Pattern PARENT_PATTERN = Pattern.compile(PARENT_PATTERN_STRING);

    private static final String REPLACEMENT_VARIABLE_PATTERN_STRING = "(?<!\\\\)\\$(\\d+)"; // (?<!\\)\$(\d+)
    private static final Pattern REPLACEMENT_VARIABLE_PATTERN = Pattern.compile(REPLACEMENT_VARIABLE_PATTERN_STRING);

    /**
     * Compile the supplied expression and return the resulting SequencerPathExpression instance.
     * 
     * @param expression the expression
     * @return the path expression; never null
     * @throws IllegalArgumentException if the expression is null
     * @throws InvalidSequencerPathExpression if the expression is blank or is not a valid expression
     */
    public static final SequencerPathExpression compile( String expression ) throws InvalidSequencerPathExpression {
        CheckArg.isNotNull(expression, "sequencer path expression");
        expression = expression.trim();
        if (expression.length() == 0) {
            throw new InvalidSequencerPathExpression(RepositoryI18n.pathExpressionMayNotBeBlank.text());
        }
        java.util.regex.Matcher matcher = TWO_PART_PATTERN.matcher(expression);
        if (!matcher.matches()) {
            throw new InvalidSequencerPathExpression(RepositoryI18n.pathExpressionIsInvalid.text(expression));
        }
        String selectExpression = matcher.group(1);
        String outputExpression = matcher.group(2);
        return new SequencerPathExpression(PathExpression.compile(selectExpression), outputExpression);
    }

    private final PathExpression selectExpression;
    private final String outputExpression;
    private final int hc;

    protected SequencerPathExpression( PathExpression selectExpression,
                                       String outputExpression ) throws InvalidSequencerPathExpression {
        CheckArg.isNotNull(selectExpression, "select expression");
        this.selectExpression = selectExpression;
        this.outputExpression = outputExpression != null ? outputExpression.trim() : DEFAULT_OUTPUT_EXPRESSION;
        this.hc = HashCode.compute(this.selectExpression, this.outputExpression);
    }

    /**
     * @return selectExpression
     */
    public String getSelectExpression() {
        return this.selectExpression.getSelectExpression();
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
            if (!this.selectExpression.equals(that.selectExpression)) return false;
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
        PathExpression.Matcher inputMatcher = selectExpression.matcher(absolutePath);
        String outputPath = null;
        if (inputMatcher.matches()) {
            // Grab the named groups ...
            Map<Integer, String> replacements = new HashMap<Integer, String>();
            for (int i = 0, count = inputMatcher.groupCount(); i <= count; ++i) {
                replacements.put(i, inputMatcher.group(i));
            }

            // Grab the selected path ...
            String selectedPath = inputMatcher.getSelectedNodePath();

            // Find the output path using the groups from the match pattern ...
            outputPath = this.outputExpression;
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
        }

        return new Matcher(inputMatcher, outputPath);
    }

    @Immutable
    public static class Matcher {

        private final PathExpression.Matcher inputMatcher;
        private final String outputPath;
        private final int hc;

        protected Matcher( PathExpression.Matcher inputMatcher,
                           String outputPath ) {
            this.inputMatcher = inputMatcher;
            this.outputPath = outputPath;
            this.hc = HashCode.compute(super.hashCode(), this.outputPath);
        }

        public boolean matches() {
            return inputMatcher.matches() && this.outputPath != null;
        }

        /**
         * @return inputPath
         */
        public String getInputPath() {
            return inputMatcher.getInputPath();
        }

        /**
         * @return selectPattern
         */
        public String getSelectedPath() {
            return inputMatcher.getSelectedNodePath();
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
                if (!super.equals(that)) return false;
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
            return inputMatcher + " => " + this.outputPath;
        }
    }

}
